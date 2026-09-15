package es.ghatostudio.nexapdf.pdf

import android.graphics.Bitmap
import android.graphics.Color
import es.ghatostudio.nexapdf.domain.escaner.AplanadoDeLuz
import es.ghatostudio.nexapdf.domain.escaner.Deconvolucion
import es.ghatostudio.nexapdf.domain.escaner.TonoDeDocumento
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
                // Se aplana la luz antes de decidir el umbral. El metodo de Otsu
                // busca el mejor corte **para toda la pagina**, y con una sombra
                // lateral no existe ninguno que valga: el que salva la zona
                // iluminada llena la sombra de negro. Sobre la pagina ya plana
                // si lo hay.
                val gris = IntArray(pixeles.size)
                for (indice in pixeles.indices) {
                    val pixel = pixeles[indice]
                    gris[indice] = luminancia(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                }
                AplanadoDeLuz.aplanarEnSitio(gris, ancho, alto)
                val umbral = umbralDeOtsu(gris) * (0.75f + 0.5f * (1f - fuerza))
                for (indice in pixeles.indices) {
                    val valor = if (gris[indice] >= umbral) 255 else 0
                    pixeles[indice] = Color.argb(Color.alpha(pixeles[indice]), valor, valor, valor)
                }
            }

            FiltroPagina.DOCUMENTO_NITIDO -> {
                // Tres pasos, en este orden, y cada uno arregla algo distinto:
                //
                //  1. **Aplanar la luz.** Un telefono no ilumina el papel por
                //     igual: la propia mano hace sombra. Con un corte global, la
                //     mitad iluminada salia blanca y la sombreada gris sucia.
                //  2. **Deshacer el desenfoque.** La foto de un papel nunca
                //     sale perfectamente definida, y enderezar la perspectiva
                //     ablanda mas. Aqui no se disimula exagerando los bordes:
                //     se deshace, partiendo de un modelo de como se emborrono.
                //  3. **Cortar.** Solo ahora tiene sentido un umbral fijo, que
                //     es lo unico que habia antes: con la pagina ya plana, el
                //     mismo corte vale en todas partes.
                // Los tres pasos van sobre el mismo array. Encadenando las
                // versiones que devuelven array nuevo habia cinco paginas
                // enteras vivas a la vez: a resolucion nativa son mas de
                // doscientos megabytes solo en enteros, y la aplicacion no pide
                // `largeHeap`.
                val gris = IntArray(pixeles.size)
                for (indice in pixeles.indices) {
                    val pixel = pixeles[indice]
                    gris[indice] = luminancia(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                }

                AplanadoDeLuz.aplanarEnSitio(gris, ancho, alto)
                Deconvolucion.aplicarEnSitio(
                    luminancia = gris,
                    ancho = ancho,
                    alto = alto,
                    iteraciones = VUELTAS_BASE + (fuerza * VUELTAS_EXTRA).roundToInt(),
                    haciaElPapel = ENFOQUE_HACIA_EL_PAPEL,
                    estimacion = FloatArray(pixeles.size),
                    apoyo = FloatArray(pixeles.size),
                )
                TonoDeDocumento.aplicarEnSitio(gris, fuerza)

                for (indice in pixeles.indices) {
                    val valor = gris[indice]
                    pixeles[indice] = Color.argb(Color.alpha(pixeles[indice]), valor, valor, valor)
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

    /**
     * Vueltas de deconvolucion con la intensidad al minimo.
     *
     * Cuatro ya deja los trazos macizos. Por debajo la pagina vuelve a verse
     * moteada, que es el defecto que este paso viene a arreglar.
     */
    private const val VUELTAS_BASE = 4

    /**
     * Lo que anade el deslizador por encima del minimo.
     *
     * El deslizador no cambia una cantidad sino cuantas veces se corrige, que es
     * lo unico que la deconvolucion tiene de graduable. Diez vueltas es el
     * techo util: mas alla el papel coge grano y las letras ya no ganan.
     */
    private const val VUELTAS_EXTRA = 6

    /**
     * Cuanto se aclara el lado claro del borde.
     *
     * Fijo, y a proposito fuera del deslizador: no es un ajuste de gusto, es el
     * freno. Es lo unico que separa "las letras se ven macizas" de "la pagina
     * tiene halos y el lapiz ha desaparecido".
     */
    private const val ENFOQUE_HACIA_EL_PAPEL = 0.40f

    private fun contraste(canal: Int, factor: Float): Int =
        (((canal - 128) * factor) + 128).roundToInt().coerceIn(0, 255)

    /**
     * Umbral por el metodo de Otsu: busca el corte que mejor separa los dos
     * grupos de luminancia de la imagen. Es el que usan los escaneres y funciona
     * sin ajustes en fotos con iluminacion desigual.
     */
    private fun umbralDeOtsu(gris: IntArray): Int {
        val histograma = IntArray(256)
        for (valor in gris) histograma[valor.coerceIn(0, 255)]++

        val total = gris.size
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
