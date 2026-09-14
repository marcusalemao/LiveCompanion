package com.marcusalemao.rokidlive.vision

import android.graphics.Bitmap

/**
 * Abstração da fonte de frames — o coração da troca CameraX ↔ CXR.
 *
 * - CameraXFrameSource: fallback que roda em QUALQUER Android (celular),
 *   usado pra desenvolver o pipeline sem os óculos na mão.
 * - CxrFrameSource: captura pela câmera real dos Rokid Glasses via CXR SDK
 *   (mesma API que o RokidPhone usa pra foto). Pluga o AAR em app/libs e
 *   implementa aqui — o resto do app não muda NADA.
 */
interface FrameSource {
    /** Liga a câmera e começa a produzir frames 640x480 JPEG. callback em thread própria. */
    fun start(onFrame: (frame: Bitmap, jpegBase64: String) -> Unit)

    fun stop()
}
