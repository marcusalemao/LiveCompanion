package com.marcusalemao.rokidlive.vision

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import android.util.Log
import android.util.Range
import android.util.Size
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CAPTURA REAL NOS ROKID GLASSES.
 *
 * Porta fiel do GlassesCameraManager (app RokidGlasses v16, validado em produção
 * nos óculos em 12-13/09/2026 — extraído por descompilação do APK). Descoberta
 * chave: a câmera dos óculos é acessível via CAMERA2 API DIRETO — não precisa
 * de AAR proprietário pra capturar (o CXR service manager é caminho alternativo).
 *
 * Estratégia validada no v16, portada aqui:
 *  - findBestCamera: preferência BACK → FRONT → EXTERNAL (nos óculos, a câmera
 *    pode se reportar como qualquer uma; v16 logava e escolhia a melhor)
 *  - Tamanho JPEG mais próximo de 1280x720 (fallback 1280x720)
 *  - openCamera com retry (5 tentativas, 2s entre elas, timeout 5s)
 *  - Exposure compensation +2EV (limites do hardware)
 *  - Captura JPEG qualidade 85, thread própria "CameraBackground"
 *
 * Adaptado pra streaming: loop de captura still a 1 FPS (o pipeline só consome
 * 640x480 — a redução acontece aqui mesmo, antes de codificar base64).
 */
class CxrFrameSource(private val context: Context) : FrameSource {

    companion object {
        private const val TAG = "CxrFrameSource"
        private const val TARGET_AREA = 1280 * 720          // área alvo de captura
        private const val JPEG_QUALITY = 85                 // mesmo do v16
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 2000L
        private const val FRAME_INTERVAL_MS = 1000L         // 1 FPS
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)

    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    override fun start(onFrame: (frame: Bitmap, jpegBase64: String) -> Unit) {
        if (!running.compareAndSet(false, true)) return
        scope.launch {
            val ok = initialize()
            if (!ok) {
                Log.e(TAG, "falha ao inicializar câmera dos óculos")
                running.set(false)
                return@launch
            }
            // Loop de streaming 1 FPS: captura still contínua (mesma técnica
            // de foto do v16, que funcionou nos óculos — agora em loop)
            while (running.get()) {
                val bytes = captureOnce()
                if (bytes != null) {
                    val bmp = decode640x480(bytes)
                    if (bmp != null) {
                        val b64 = Base64.encodeToString(scaleJpeg(bmp, 640, 480, 60), Base64.NO_WRAP)
                        onFrame(bmp, b64)
                    }
                }
                delay(FRAME_INTERVAL_MS)
            }
        }
    }

    override fun stop() {
        running.set(false)
        scope.launch { release() }
    }

    // ---------- inicialização (porta do v16) ----------

    private suspend fun initialize(): Boolean = withTimeoutOrNull(5000L + RETRY_DELAY_MS * MAX_RETRIES) {
        startBackgroundThread()
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val cameraId = findBestCamera() ?: run {
            Log.e(TAG, "nenhuma câmera encontrada")
            return@withTimeoutOrNull false
        }
        Log.i(TAG, "câmera selecionada: $cameraId")

        var opened = false
        repeat(MAX_RETRIES) { attempt ->
            opened = openCameraWithRetry(cameraId)
            if (opened) return@repeat
            Log.w(TAG, "tentativa ${attempt + 1}/$MAX_RETRIES falhou, aguardando ${RETRY_DELAY_MS}ms")
            delay(RETRY_DELAY_MS)
        }
        if (!opened) return@withTimeoutOrNull false

        val reader = imageReader ?: return@withTimeoutOrNull false
        val device = cameraDevice ?: return@withTimeoutOrNull false

        val sessionReady = suspendCancellableCoroutine<CameraCaptureSession> { cont ->
            try {
                device.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) { cont.resumeWith(kotlin.Result.success(session)) }
                    override fun onConfigureFailed(session: CameraCaptureSession) { cont.resumeWith(kotlin.Result.failure(IllegalStateException("configure failed"))) }
                }, backgroundHandler)
            } catch (e: Exception) { cont.resumeWith(kotlin.Result.failure(e)) }
        }
        captureSession = sessionReady
        true
    } ?: false

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /** Porta do findBestCamera (v16): BACK → FRONT → EXTERNAL. */
    private fun findBestCamera(): String? {
        val cm = cameraManager ?: return null
        return runCatching {
            var back: String? = null
            var front: String? = null
            var external: String? = null
            for (id in cm.cameraIdList) {
                Log.d(TAG, "câmera disponível: $id")
                val ch = cm.getCameraCharacteristics(id)
                when (ch.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_BACK -> back = id
                    CameraCharacteristics.LENS_FACING_FRONT -> front = id
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> external = id
                }
            }
            back ?: external ?: front ?: cm.cameraIdList.firstOrNull()
        }.getOrNull()
    }

    /** Porta do chooseCaptureSize (v16): JPEG mais próximo de 1280x720. */
    private fun chooseCaptureSize(cameraId: String): Size {
        val cm = cameraManager ?: return Size(1280, 720)
        return runCatching {
            val ch = cm.getCameraCharacteristics(cameraId)
            val map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG)
            var best = Size(1280, 720)
            var bestDiff = Int.MAX_VALUE
            sizes?.forEach { s ->
                val diff = kotlin.math.abs(s.width * s.height - TARGET_AREA)
                if (diff < bestDiff) { bestDiff = diff; best = s }
            }
            Log.i(TAG, "tamanho de captura: ${best.width}x${best.height}")
            best
        }.getOrDefault(Size(1280, 720))
    }

    private suspend fun openCameraWithRetry(cameraId: String): Boolean = withTimeoutOrNull(5000L) {
        val cm = cameraManager ?: return@withTimeoutOrNull false
        val size = chooseCaptureSize(cameraId)

        imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 2)

        suspendCancellableCoroutine<CameraDevice> { cont ->
            try {
                cm.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(device: CameraDevice) { cont.resumeWith(kotlin.Result.success(device)) }
                    override fun onDisconnected(device: CameraDevice) { device.close(); cont.resumeWith(kotlin.Result.failure(IllegalStateException("disconnected"))) }
                    override fun onError(device: CameraDevice, error: Int) { device.close(); cont.resumeWith(kotlin.Result.failure(IllegalStateException("erro $error"))) }
                }, backgroundHandler)
            } catch (e: Exception) { cont.resumeWith(kotlin.Result.failure(e)) }
        }.let { device ->
            cameraDevice = device
            true
        }
    } ?: false

    // ---------- captura (porta do captureImage do v16) ----------

    private suspend fun captureOnce(): ByteArray? {
        val device = cameraDevice ?: return null
        val session = captureSession ?: return null
        val reader = imageReader ?: return null

        return withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine<ByteArray> { cont ->
                try {
                    reader.acquireLatestImage()?.close() // limpa buffer velho
                    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                        addTarget(reader.surface)
                        // Exposure compensation +2EV (porta do v16 — imagem mais clara)
                        runCatching {
                            val ch = cameraManager!!.getCameraCharacteristics(device.id)
                            val step = ch.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                            val range = ch.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                            if (step != null && range != null) {
                                val target = (2.0f / step.toFloat()).toInt().coerceIn(range.lower, range.upper)
                                set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, target)
                            }
                        }
                    }
                    reader.setOnImageAvailableListener({ r ->
                        val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                        val bytes = runCatching {
                            val buffer = image.planes[0].buffer
                            val b = ByteArray(buffer.remaining())
                            buffer.get(b)
                            b
                        }.getOrNull()
                        image.close()
                        r.setOnImageAvailableListener(null, null)
                        cont.resumeWith(kotlin.Result.success(bytes ?: ByteArray(0)))
                    }, backgroundHandler)
                    session.capture(request.build(), null, backgroundHandler)
                } catch (e: Exception) {
                    cont.resumeWith(kotlin.Result.failure(e))
                }
            }
        }
    }

    // ---------- util ----------

    private fun decode640x480(jpeg: ByteArray): Bitmap? = runCatching {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }  // 1280x720 → 640x360-ish
        val raw = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, opts) ?: return null
        Bitmap.createScaledBitmap(raw, 640, 480, true)
    }.getOrNull()

    private fun scaleJpeg(bmp: Bitmap, w: Int, h: Int, quality: Int): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)
        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, bos)
        return bos.toByteArray()
    }

    @SuppressLint("MissingPermission")
    private suspend fun release() {
        runCatching { captureSession?.close() }
        captureSession = null
        runCatching { cameraDevice?.close() }
        cameraDevice = null
        runCatching { imageReader?.close() }
        imageReader = null
        backgroundThread?.quitSafely()
        runCatching { backgroundThread?.join() }
        backgroundThread = null
        backgroundHandler = null
    }
}
