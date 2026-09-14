package com.marcusalemao.rokidlive.vision

/**
 * Frame-Differencing: porta direta do algoritmo do client HTML validado.
 * Descarta cena estática ANTES de gastar rede e budget de Gemini.
 *
 * Amostra 1 a cada 16 pixels (passo 64 no array RGBA) e compara
 * a variação média de canal contra o limiar.
 */
object FrameDiff {

    const val DIFF_THRESHOLD = 15.0

    /**
     * @param current  pixels RGBA do frame atual (ex.: ImageProxy → bitmap → getPixels)
     * @param previous pixels RGBA do frame anterior
     * @return true se a cena mudou o suficiente pra valer o envio
     */
    fun hasSceneChanged(current: IntArray, previous: IntArray, threshold: Double = DIFF_THRESHOLD): Boolean {
        if (current.size != previous.size) return true
        var totalDiff = 0.0
        var sampled = 0
        val step = 4 * 16
        var i = 0
        while (i < current.size) {
            val c = current[i]; val p = previous[i]
            val dr = Math.abs(((c shr 16) and 0xFF) - ((p shr 16) and 0xFF))
            val dg = Math.abs(((c shr 8) and 0xFF) - ((p shr 8) and 0xFF))
            val db = Math.abs((c and 0xFF) - (p and 0xFF))
            totalDiff += (dr + dg + db) / 3.0
            sampled++
            i += step
        }
        if (sampled == 0) return true
        return (totalDiff / sampled) > threshold
    }
}
