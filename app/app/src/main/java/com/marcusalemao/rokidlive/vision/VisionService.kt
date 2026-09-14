package com.marcusalemao.rokidlive.vision

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.marcusalemao.rokidlive.api.ApiClient
import com.marcusalemao.rokidlive.hud.HudActivity
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Pipeline de visão — o coração do Rokid Vision Assistant.
 *
 * Loop a 1 FPS (respeitando o gate de 4s do backend por análise Gemini):
 *  1. Frame 640x480 da fonte (CXR nos óculos / CameraX em dev)
 *  2. Frame-Differencing local — cena estática não sai do aparelho
 *  3. POST rvFrameIngest com frame + snapshot + GPS
 *  4. hud_alert da resposta vai direto pro HUD (broadcast pra HudActivity)
 *
 * Service foreground (tipo camera) — sobrevive com o app em background,
 * exigência pra streaming contínuo nos óculos.
 */
class VisionService : Service() {

    companion object {
        const val ACTION_START = "rokidlive.action.START"
        const val ACTION_STOP = "rokidlive.action.STOP"
        const val HUD_ALERT_BROADCAST = "rokidlive.HUD_ALERT"

        private const val FRAME_INTERVAL_MS = 1000L   // 1 FPS
        private const val CHANNEL_ID = "rokidlive_vision"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var source: FrameSource? = null
    private var lastPixels: IntArray? = null
    private var lastSentAt = 0L
    private var lastAlertAt = 0L

    private var lat: Double? = null
    private var lng: Double? = null
    private var locationListener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            else -> startForegroundInternal()
        }
        return START_STICKY
    }

    private fun startForegroundInternal() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Visão RokidLive", NotificationManager.IMPORTANCE_LOW)
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Beto Vision")
            .setContentText("Streaming de visão ativo — 1 FPS")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
        } else {
            startForeground(1, notif)
        }

        startLocationWatch()
        startFrameLoop()
    }

    private fun startLocationWatch() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { l: Location ->
            lat = l.latitude; lng = l.longitude
        }
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000L, 5f, locationListener!!)
        } catch (_: SecurityException) { /* permissão ainda não concedida */ }
    }

    private fun startFrameLoop() {
        if (source != null) return // já rodando

        // NOS ÓCULOS: CxrFrameSource = Camera2 direto na câmera dos óculos
        // (porta do GlassesCameraManager v16 — validado em produção, sem AAR).
        // CameraXFrameSource só para desenvolvimento no celular.
        val onGlasses = android.os.Build.MODEL.lowercase().contains("rokid")
        source = (if (onGlasses) CxrFrameSource(this) else CameraXFrameSource(this, this)).also { src ->
            src.start { frame: Bitmap, jpegBase64: String ->
                scope.launch {
                    handleFrame(frame, jpegBase64)
                }
            }
        }
    }

    private suspend fun handleFrame(frame: Bitmap, jpegBase64: String) {
        val now = System.currentTimeMillis()

        // 1. Frame-differencing local (porta do client HTML validado)
        val w = frame.width; val h = frame.height
        val pixels = IntArray(w * h)
        frame.getPixels(pixels, 0, w, 0, 0, w, h)
        val prev = lastPixels
        if (prev != null && !FrameDiff.hasSceneChanged(pixels, prev)) {
            return // cena estática — não gasta rede nem budget
        }
        lastPixels = pixels

        // 2. Gate de 4s do backend (MIN_ANALYSIS_INTERVAL_MS) — evita 429 silencioso
        if (now - lastSentAt < 4000L) return
        lastSentAt = now

        // 3. Ingest (frame + snapshot + GPS)
        val snapshot = frame.snapshotBase64()
        val resp = ApiClient.ingestFrame(jpegBase64, snapshot, lat, lng)

        // 4. hud_alert instantânea da resposta do pipeline → HUD
        val alert = resp?.optString("hud_alert")
        if (!alert.isNullOrBlank() && now - lastAlertAt > 4000L) {
            lastAlertAt = now
            broadcastAlert(alert)
        }
    }

    private fun broadcastAlert(text: String) {
        val i = Intent(HUD_ALERT_BROADCAST)
        i.putExtra("text", text)
        i.setPackage(packageName)
        sendBroadcast(i)
    }

    override fun onDestroy() {
        source?.stop()
        source = null
        locationListener?.let {
            (getSystemService(Context.LOCATION_SERVICE) as LocationManager)
                .removeUpdates(it)
        }
        scope.cancel()
        super.onDestroy()
    }
}
