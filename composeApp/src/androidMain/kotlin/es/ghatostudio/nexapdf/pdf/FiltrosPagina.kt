package es.ghatostudio.nexapdf.pdf

import android.graphics.Bitmap
import android.graphics.Color
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import kotlin.math.roundToInt

/**
 * Mejoras globales de pagina.
 *
 * Se trabaja sobre el array de pixeles en una sola pasada en lugar de usar
 * ColorMatrix y un Canvas: los filtros de documento (umbral adaptativo,
 * aclarado de fondo) no son transformaciones lineales de color y no se pueden
 * expresar como matriz, y hacerlo todo igual evita tener dos caminos distintos.
 */
object FiltrosPagina {

    fun aplicar(origen: Bitmap, filtro: FiltroPagina, intensidad: Float): Bitmap {
        if (filtro == FiltroPagina.NINGUNO) return origen

        val ancho = origen.width
        val alto = origen.height
        val pixeles = IntArray(ancho * alto)
        origen.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)

        val fuerza = intensidad.coerceIn(0f, 1f)

        when (filtro) {
            FiltroPagina.ESCALA_DE_GRISES -> transformar(pixeles) { r, g, b ->
                val gris = luminancia(r, g, b)
                Triple(gris, gris, gris)
            }

            FiltroPagina.BLANCO_Y_NEGRO -> {
                // Umbral relativo a la luminancia media de la pagina: un escaneo
                // con sombra lateral no tiene un umbral fijo que le sirva.
                val umbral = umbralAutomatico(pixeles) * (0.75f + 0.5f * (1f - fuerza))
                transformar(pixeles) { r, g, b ->
                    val valor = if (luminancia(r, g, b) >= umbral) 255 else 0
                    Triple(valor, valor, valor)
                }
            }

            FiltroPagina.DOCUMENTO_NITIDO -> {
                // Lleva a blanco puro todo lo que ya casi lo era y oscurece la
                // tinta: es lo que se espera de una foto de un papel.
                val corteFondo = 168 + (60 * fuerza).roundToInt()
                val corteTinta = 60 + (40 * (1f - fuerza)).roundToInt()
                transformar(pixeles) { r, g, b ->
                    val gris = luminancia(r, g, b)
                    val valor = when {
                        gris >= corteFondo -> 255
                        gris <= corteTinta -> 0
                        else -> ((gris - corteTinta) * 255f / (corteFondo - corteTinta)).roundToInt()
                    }
                    Triple(valor, valor, valor)
                }
            }

            FiltroPagina.ALTO_CONTRASTE -> {
                val factor = 1f + 1.6f * fuerza
                transformar(pixeles) { r, g, b ->
                    Triple(contraste(r, factor), contraste(g, factor), contraste(b, factor))
                }
            }

            FiltroPagina.ACLARAR -> {
                val suma = (70 * fuerza).roundToInt()
                transformar(pixeles) { r, g, b ->
                    Triple(
                        (r + suma).coerceAtMost(255),
                        (g + suma).coerceAtMost(255),
                        (b + suma).coerceAtMost(255),
                    )
                }
            }

            FiltroPagina.INVERTIR -> transformar(pixeles) { r, g, b ->
                Triple(255 - r, 255 - g, 255 - b)
            }

            FiltroPagina.NINGUNO -> Unit
        }

        return Bitmap.createBitmap(pixeles, ancho, alto, Bitmap.Config.ARGB_8888)
    }

    private inline fun transformar(
        pixeles: IntArray,
        transformacion: (Int, Int, Int) -> Triple<Int, Int, Int>,
    ) {
        for (indice in pixeles.indices) {
            val pixel = pixeles[indice]
            val (r, g, b) = transformacion(
                Color.red(pixel),
                Color.green(pixel),
                Color.blue(pixel),
            )
            pixeles[indice] = Color.argb(Color.alpha(pixel), r, g, b)
        }
    }

    private fun luminancia(r: Int, g: Int, b: Int): Int =
        ((r * 299 + g * 587 + b * 114) / 1000).coerceIn(0, 255)

    private fun contraste(canal: Int, factor: Float): Int =
        (((canal - 128) * factor) + 128).roundToInt().coerceIn(0, 255)

    /**
     * Umbral por el metodo de Otsu: busca el corte que mejor separa los dos
     * grupos de luminancia de la imagen. Es el que usan los escaneres y funciona
     * sin ajustes en fotos con iluminacion desigual.
     */
    private fun umbralAutomatico(pixeles: IntArray): Int {
        val histograma = IntArray(256)
        for (pixel in pixeles) {
            histograma[luminancia(Color.red(pixel), Color.green(pixel), Color.blue(pixel))]++
        }

        val total = pixeles.size
        var sumaTotal = 0L
        for (valor in 0..255) sumaTotal += valor.toLong() * histograma[valor]

        var sumaFondo = 0L
        var pesoFondo = 0
        var mejorVarianza = 0.0
        var mejorUmbral = 128

        for (valor in 0..255) {
            pesoFondo += histograma[valor]
            if (pesoFondo == 0) continue
            val pesoFrente = total - pesoFondo
            if (pesoFrente == 0) break

            sumaFondo += valor.toLong() * histograma[valor]
            val mediaFondo = sumaFondo.toDouble() / pesoFondo
            val mediaFrente = (sumaTotal - sumaFondo).toDouble() / pesoFrente
            val varianza = pesoFondo.toDouble() * pesoFrente * (mediaFondo - mediaFrente) *
                (mediaFondo - mediaFrente)

            if (varianza > mejorVarianza) {
                mejorVarianza = varianza
                mejorUmbral = valor
            }
        }
        return mejorUmbral
    }
}
