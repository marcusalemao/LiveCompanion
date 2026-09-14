package com.marcusalemao.rokidlive.vision

import android.content.Context
import android.graphics.Bitmap

/**
 * CAPTURA REAL DOS ROKID GLASSES — via CXR SDK (a mesma do RokidPhone).
 *
 * Passos pra ativar (quando o AAR estiver plugado em app/libs/):
 *  1. build.gradle.kts: implementation(files("libs/cxr-sdk.aar"))
 *  2. Implementar start(): inicializar CXR camera, pedir frames 640x480,
 *     converter pra Bitmap e chamar onFrame(bitmap, jpegBase64)
 *  3. VisionService.startFrameLoop(): trocar CameraXFrameSource por CxrFrameSource
 *
 * O resto do app (diff, ingest, HUD, KeyEvents) NÃO MUDA — a abstração
 * FrameSource isola o SDK proprietário numa única classe.
 */
class CxrFrameSource(private val context: Context) : FrameSource {

    override fun start(onFrame: (frame: Bitmap, jpegBase64: String) -> Unit) {
        // TODO: CXR camera API — ver RokidPhone/CameraModule.kt pra referência
        //  cxrCamera.setFrameCallback { frameData ->
        //      val bmp = frameData.toBitmap(640, 480)
        //      onFrame(bmp, bmp.jpegBase64(60))
        //  }
        throw NotImplementedError("Plugar o CXR SDK (AAR em app/libs) pra captura nos óculos")
    }

    override fun stop() {
        // TODO: liberar câmera CXR
    }
}
