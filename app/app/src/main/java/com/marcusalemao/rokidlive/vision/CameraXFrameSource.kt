package com.marcusalemao.rokidlive.vision

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Fonte de frames via CameraX — fallback de DESENVOLVIMENTO (roda no celular).
 * Nos Rokid Glasses a captura real deve vir do CXR SDK: cria CxrFrameSource
 * implementando FrameSource e troca aqui em VisionService (uma linha).
 */
class CameraXFrameSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : FrameSource {

    private val executor = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null

    @SuppressLint("UnsafeOptInUsageError")
    override fun start(onFrame: (frame: Bitmap, jpegBase64: String) -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            provider = future.get().also { cam ->
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    val bmp = proxy.toBitmap640x480()
                    proxy.close()
                    if (bmp != null) {
                        val b64 = bmp.jpegBase64(60)
                        if (b64 != null) onFrame(bmp, b64)
                    }
                }

                cam.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    analysis
                )
            }
        }, executor)
    }

    override fun stop() {
        provider?.unbindAll()
        executor.shutdown()
    }
}

// ---- Extensões auxiliares (YUV → Bitmap → JPEG base64) ----

private fun ImageProxy.toBitmap640x480(): Bitmap? = runCatching {
    val bmp = toBitmap() // ImageProxy.toBitmap() lida com YUV/RGB internamente
    Bitmap.createScaledBitmap(bmp, 640, 480, true)
}.getOrNull()

internal fun Bitmap.jpegBase64(quality: Int): String? = runCatching {
    val bos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, bos)
    Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

/** Thumbnail 160x120 do mesmo frame — vira snapshot_base64 do registro de memória. */
internal fun Bitmap.snapshotBase64(): String? = runCatching {
    val small = Bitmap.createScaledBitmap(this, 160, 120, true)
    val bos = ByteArrayOutputStream()
    small.compress(Bitmap.CompressFormat.JPEG, 50, bos)
    Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
}.getOrNull()
