package es.ghatostudio.nexapdf.domain

import es.ghatostudio.nexapdf.domain.escaner.ApiladoDeFotogramas
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * El apilado de rafaga: varias fotos de la misma hoja fundidas en una limpia.
 *
 * Lo facil aqui es enganarse. Promediar siempre baja el ruido, asi que una
 * prueba que solo mire el ruido pasa incluso con el alineado roto —y con el
 * alineado roto la pagina sale borrosa, que es peor que no hacer nada. Por eso
 * cada prueba de ruido va acompanada de una de nitidez: **las dos a la vez, o no
 * vale**.
 */
class ApiladoDeFotogramasTest {

    private val ancho = 240
    private val alto = 180

    /** Una pagina con texto: franjas verticales, que es lo que un borde tiene. */
    private fun pagina(): IntArray = IntArray(ancho * alto) { indice ->
        val x = indice % ancho
        val y = indice / ancho
        when {
            y < 20 || y > alto - 20 -> 240
            (x / 12) % 2 == 0 -> 40
            else -> 235
        }
    }

    /** Desplaza una imagen con interpolacion, como la mano al temblar. */
    private fun mover(origen: IntArray, dx: Float, dy: Float): IntArray {
        val salida = IntArray(ancho * alto)
        for (y in 0 until alto) {
            for (x in 0 until ancho) {
                val fx = (x - dx).coerceIn(0f, (ancho - 1).toFloat())
                val fy = (y - dy).coerceIn(0f, (alto - 1).toFloat())
                val x0 = fx.toInt()
                val y0 = fy.toInt()
                val x1 = minOf(x0 + 1, ancho - 1)
                val y1 = minOf(y0 + 1, alto - 1)
                val px = fx - x0
                val py = fy - y0
                val arriba = origen[y0 * ancho + x0] * (1 - px) + origen[y0 * ancho + x1] * px
                val abajo = origen[y1 * ancho + x0] * (1 - px) + origen[y1 * ancho + x1] * px
                salida[y * ancho + x] = (arriba + (abajo - arriba) * py).roundToInt()
            }
        }
        return salida
    }

    /** Grano reproducible: cada fotograma con su semilla. */
    private fun conRuido(origen: IntArray, semilla: Int, amplitud: Int = 10): IntArray {
        var estado = semilla * 7919 + 13
        return IntArray(origen.size) { indice ->
            estado = estado * 1103515245 + 12345
            val ruido = ((estado ushr 16) % (2 * amplitud + 1)) - amplitud
            (origen[indice] + ruido).coerceIn(0, 255)
        }
    }

    /** Desviacion del papel respecto a su valor real: cuanto grano queda. */
    private fun granoDelPapel(datos: IntArray, limpia: IntArray): Double {
        var suma = 0.0
        var cuenta = 0
        for (y in 30 until alto - 30) {
            for (x in 0 until ancho) {
                val indice = y * ancho + x
                if (limpia[indice] < 200) continue // solo papel, no tinta
                val diferencia = (datos[indice] - limpia[indice]).toDouble()
                suma += diferencia * diferencia
                cuenta++
            }
        }
        return if (cuenta == 0) 0.0 else kotlin.math.sqrt(suma / cuenta)
    }

    /** El mayor salto entre columnas contiguas: cuanto filo conserva. */
    private fun filo(datos: IntArray): Int {
        val fila = alto / 2
        var mayor = 0
        for (x in 40 until ancho - 40) {
            val salto = abs(datos[fila * ancho + x + 1] - datos[fila * ancho + x])
            if (salto > mayor) mayor = salto
        }
        return mayor
    }

    @Test
    fun `cuatro fotos movidas dejan menos grano y el mismo filo`() {
        // La prueba que justifica que esto exista. Cuatro disparos con la mano
        // temblando, cada uno con su grano. Tiene que salir mas limpia **y**
        // igual de definida: si solo se mira lo primero, un alineado roto
        // tambien pasaria, y dejaria la pagina borrosa.
        val limpia = pagina()
        val fotogramas = listOf(
            conRuido(limpia, 1),
            conRuido(mover(limpia, 2.4f, -1.7f), 2),
            conRuido(mover(limpia, -3.1f, 2.2f), 3),
            conRuido(mover(limpia, 1.3f, 3.4f), 4),
        )

        val apilada = ApiladoDeFotogramas.apilar(fotogramas, ancho, alto)

        val granoAntes = granoDelPapel(fotogramas.first(), limpia)
        val granoDespues = granoDelPapel(apilada, limpia)
        assertTrue(
            granoDespues < granoAntes * 0.75,
            "el grano paso de $granoAntes a $granoDespues: apilar no esta limpiando",
        )

        val filoAntes = filo(fotogramas.first())
        val filoDespues = filo(apilada)
        assertTrue(
            filoDespues > filoAntes * 0.85,
            "el filo paso de $filoAntes a $filoDespues: el alineado esta emborronando",
        )
    }

    @Test
    fun `con un solo fotograma no cambia nada`() {
        val limpia = pagina()
        val apilada = ApiladoDeFotogramas.apilar(listOf(limpia), ancho, alto)
        assertTrue(apilada.contentEquals(limpia), "con una sola foto no hay nada que fundir")
    }

    @Test
    fun `un dedo que entra en el encuadre no deja fantasma`() {
        // El rechazo por parecido, que es la red de seguridad de todo esto. Una
        // foto de las cuatro tiene una franja oscura que las otras no —una
        // sombra, un dedo, el movil tapando la luz—. Promediandola sin mas
        // quedaria un fantasma gris a la vista en la pagina final.
        val limpia = pagina()
        val estropeada = limpia.copyOf()
        for (y in 60 until 100) {
            for (x in 20 until 120) estropeada[y * ancho + x] = 10
        }

        val apilada = ApiladoDeFotogramas.apilar(
            listOf(limpia, limpia.copyOf(), estropeada, limpia.copyOf()),
            ancho,
            alto,
        )

        // En la zona del intruso, la pagina tiene que seguir siendo la de
        // referencia y no una mezcla.
        var peor = 0
        for (y in 60 until 100) {
            for (x in 20 until 120) {
                val indice = y * ancho + x
                peor = max(peor, abs(apilada[indice] - limpia[indice]))
            }
        }
        assertTrue(peor <= 4, "el intruso dejo un fantasma de $peor niveles")
    }

    @Test
    fun `una foto de otra escena se descarta entera`() {
        // Si el disparo pilla otra cosa —la mesa, el techo— no hay
        // desplazamiento que la haga encajar. Tiene que quedarse fuera en lugar
        // de arrastrar la media.
        val limpia = pagina()
        val otraEscena = IntArray(ancho * alto) { (it * 37) % 256 }

        val apilada = ApiladoDeFotogramas.apilar(listOf(limpia, otraEscena), ancho, alto)

        var peor = 0
        for (indice in limpia.indices) peor = max(peor, abs(apilada[indice] - limpia[indice]))
        assertTrue(peor <= 4, "la foto ajena movio la pagina $peor niveles")
    }

    @Test
    fun `el alineado acierta por debajo del pixel`() {
        // Se comprueba de la unica forma que de verdad prueba el subpixel: se
        // apilan dos copias desplazadas medio pixel de una imagen **sin ruido**.
        // Si el alineado fuese a pixeles enteros, la media de las dos saldria
        // mas blanda que el original. Alineando bien, sale igual.
        val limpia = pagina()
        val movida = mover(limpia, 0.5f, 0.5f)

        val apilada = ApiladoDeFotogramas.apilar(listOf(limpia, movida), ancho, alto)

        val filoOriginal = filo(limpia)
        val filoApilado = filo(apilada)
        assertTrue(
            filoApilado > filoOriginal * 0.9,
            "el filo era $filoOriginal y quedo en $filoApilado: se perdio en el subpixel",
        )
    }

    @Test
    fun `apilar una pagina entera cuesta lo que puede costar`() {
        // Esto corre una vez por hoja, encima del revelado, que ya cuesta casi
        // tres segundos en el telefono. Si el apilado costara otro tanto por
        // fotograma, la rafaga saldria mas cara de lo que vale.
        val anchoPagina = 2480
        val altoPagina = 3500
        val limpia = IntArray(anchoPagina * altoPagina) { indice ->
            val y = indice / anchoPagina
            if (y % 40 < 6) 45 else 235
        }
        val fotogramas = listOf(
            conRuido(limpia, 1),
            conRuido(limpia, 2),
            conRuido(limpia, 3),
        )

        val inicio = kotlin.time.TimeSource.Monotonic.markNow()
        ApiladoDeFotogramas.apilar(fotogramas, anchoPagina, altoPagina)
        val tardado = inicio.elapsedNow()

        assertTrue(
            tardado.inWholeMilliseconds < 8000,
            "apilar tres fotogramas costo $tardado",
        )
    }

    private fun max(a: Int, b: Int) = if (a > b) a else b
}
