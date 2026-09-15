package es.ghatostudio.nexapdf.domain

import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.escaner.DeteccionDocumento
import es.ghatostudio.nexapdf.domain.escaner.DetectorDocumento
import es.ghatostudio.nexapdf.domain.escaner.SuavizadorDeteccion
import es.ghatostudio.nexapdf.domain.escaner.escenaDistinta
import es.ghatostudio.nexapdf.domain.model.Punto
import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cuanto se mueve el contorno cuando el papel **no** se mueve.
 *
 * Es la prueba que faltaba. Las otras comprueban que la deteccion acierta en un
 * fotograma; esta comprueba que acierta *igual* en el siguiente, que es lo que
 * se ve en pantalla: un contorno que baila entre dos posiciones se lee como que
 * la aplicacion no sabe lo que hace, aunque cada fotograma por separado este
 * bien detectado.
 *
 * La escena es la misma en todos los fotogramas salvo por el ruido del sensor,
 * que es justo lo que hay en una camara real apuntando quieta a una mesa.
 */
class EstabilidadDeteccionTest {

    private val ancho = 640
    private val alto = 480

    @Test
    fun `el contorno no baila entre fotogramas de la misma escena`() {
        val papel = Cuadrilatero(
            supIzq = Punto(0.22f, 0.14f),
            supDer = Punto(0.79f, 0.17f),
            infDer = Punto(0.83f, 0.86f),
            infIzq = Punto(0.17f, 0.84f),
        )
        val detector = DetectorDocumento()
        val suavizador = SuavizadorDeteccion()

        val salidas = (0 until FOTOGRAMAS).map { indice ->
            val bruta = detector.detectar(escena(papel, semilla = indice), ancho, alto)
            suavizador.siguiente(bruta)
        }

        // Se descartan los primeros: el suavizado necesita un par de fotogramas
        // para engancharse, igual que en pantalla.
        val estables = salidas.drop(6).mapNotNull { it.cuadro }
        assertTrue(
            estables.size >= FOTOGRAMAS - 8,
            "la deteccion se perdio en ${FOTOGRAMAS - 6 - estables.size} fotogramas",
        )

        val temblor = temblorMaximo(estables)
        assertTrue(
            temblor <= TEMBLOR_ACEPTABLE,
            "el contorno se mueve $temblor de la pantalla entre fotogramas seguidos; " +
                "el limite es $TEMBLOR_ACEPTABLE",
        )
    }

    @Test
    fun `las esquinas no se intercambian entre fotogramas`() {
        // Un papel girado casi 45 grados es donde el orden de las esquinas se
        // decide por poco. Si baila, el suavizado mezcla la esquina de arriba
        // con la de la izquierda y el contorno da un latigazo.
        val papel = Cuadrilatero(
            supIzq = Punto(0.44f, 0.10f),
            supDer = Punto(0.88f, 0.47f),
            infDer = Punto(0.55f, 0.89f),
            infIzq = Punto(0.11f, 0.52f),
        )
        val detector = DetectorDocumento()
        val suavizador = SuavizadorDeteccion()

        val salidas = (0 until FOTOGRAMAS).map { indice ->
            suavizador.siguiente(detector.detectar(escena(papel, semilla = indice), ancho, alto))
        }
        val estables = salidas.drop(6).mapNotNull { it.cuadro }
        assertTrue(estables.size >= FOTOGRAMAS - 10, "se perdio el papel girado")

        assertTrue(
            temblorMaximo(estables) <= TEMBLOR_ACEPTABLE_GIRADO,
            "las esquinas del papel girado saltan ${temblorMaximo(estables)}",
        )
    }

    @Test
    fun `la confianza se mantiene alta en una escena quieta`() {
        val papel = Cuadrilatero(
            supIzq = Punto(0.20f, 0.15f),
            supDer = Punto(0.80f, 0.15f),
            infDer = Punto(0.80f, 0.85f),
            infIzq = Punto(0.20f, 0.85f),
        )
        val detector = DetectorDocumento()
        val suavizador = SuavizadorDeteccion()

        val confianzas = (0 until FOTOGRAMAS).map { indice ->
            suavizador.siguiente(detector.detectar(escena(papel, semilla = indice), ancho, alto))
                .confianza
        }.drop(6)

        val minima = confianzas.min()
        assertTrue(
            minima >= DeteccionDocumento.UMBRAL_AUTOMATICO,
            "la confianza baja hasta $minima y la captura automatica nunca dispararia",
        )
    }

    @Test
    fun `la deteccion de un fotograma cabe en el tiempo de un fotograma`() {
        val papel = Cuadrilatero(
            supIzq = Punto(0.20f, 0.15f),
            supDer = Punto(0.80f, 0.15f),
            infDer = Punto(0.80f, 0.85f),
            infIzq = Punto(0.20f, 0.85f),
        )
        val detector = DetectorDocumento()
        val plano = escena(papel, semilla = 1)

        repeat(5) { detector.detectar(plano, ancho, alto) }

        val inicio = kotlin.time.TimeSource.Monotonic.markNow()
        repeat(MEDIDAS) { detector.detectar(plano, ancho, alto) }
        val porFotograma = inicio.elapsedNow() / MEDIDAS

        // El margen es amplio a proposito: esto corre en el escritorio de
        // integracion continua, no en el telefono. Sirve para cazar un cambio
        // que multiplique el coste por diez, no para medir el movil.
        assertTrue(
            porFotograma.inWholeMilliseconds < 60,
            "cada fotograma cuesta $porFotograma, demasiado para una vista previa",
        )
    }

    // --- Rearme de la captura automatica -------------------------------------

    @Test
    fun `la misma hoja quieta no cuenta como escena nueva`() {
        // El fallo que arregla esto: la captura automatica sacaba dos fotos de
        // la misma pagina, porque un segundo despues del disparo el papel
        // seguia encuadrado y quieto.
        val tomada = rectangulo(0.20f, 0.15f, 0.80f, 0.85f)
        assertFalse(
            escenaDistinta(tomada, tomada),
            "la hoja no se ha movido y se esta dando por nueva",
        )

        // Un temblor de mano de medio punto porcentual tampoco.
        val temblando = rectangulo(0.205f, 0.152f, 0.803f, 0.848f)
        assertFalse(
            escenaDistinta(tomada, temblando),
            "un temblor de mano no es una hoja nueva",
        )
    }

    @Test
    fun `quitar el papel libera la captura`() {
        val tomada = rectangulo(0.20f, 0.15f, 0.80f, 0.85f)
        assertTrue(
            escenaDistinta(tomada, null),
            "sin papel delante hay que poder volver a disparar",
        )
    }

    @Test
    fun `poner otra hoja en otro sitio libera la captura`() {
        val tomada = rectangulo(0.20f, 0.15f, 0.80f, 0.85f)
        val otra = rectangulo(0.32f, 0.26f, 0.92f, 0.94f)
        assertTrue(
            escenaDistinta(tomada, otra),
            "una hoja puesta en otro sitio es una escena nueva",
        )
    }

    // --- Utilidades ----------------------------------------------------------

    private fun rectangulo(
        izquierda: Float,
        arriba: Float,
        derecha: Float,
        abajo: Float,
    ) = Cuadrilatero(
        supIzq = Punto(izquierda, arriba),
        supDer = Punto(derecha, arriba),
        infDer = Punto(derecha, abajo),
        infIzq = Punto(izquierda, abajo),
    )

    /** Lo que mas se mueve una esquina de un fotograma al siguiente, en fraccion de pantalla. */
    private fun temblorMaximo(cuadros: List<Cuadrilatero>): Float {
        var peor = 0f
        for (indice in 1 until cuadros.size) {
            val anterior = cuadros[indice - 1].esquinas
            val actual = cuadros[indice].esquinas
            for (esquina in 0 until 4) {
                peor = max(peor, abs(anterior[esquina].x - actual[esquina].x))
                peor = max(peor, abs(anterior[esquina].y - actual[esquina].y))
            }
        }
        return peor
    }

    /** La misma escena en todos los fotogramas salvo por el ruido del sensor. */
    private fun escena(papel: Cuadrilatero, semilla: Int): ByteArray {
        val plano = ByteArray(ancho * alto)
        var estado = semilla * 2654435761L + 1013904223L
        for (y in 0 until alto) {
            val ny = (y + 0.5f) / alto
            for (x in 0 until ancho) {
                val nx = (x + 0.5f) / ancho
                estado = estado * 6364136223846793005L + 1442695040888963407L
                val ruido = ((estado ushr 33) % 13).toInt() - 6
                // Un degradado de luz encima del fondo, como en cualquier foto.
                val fondo = 48 + (26 * nx).toInt()
                val hoja = 208 - (22 * ny).toInt()
                val valor = (if (dentro(papel, nx, ny)) hoja else fondo) + ruido
                plano[y * ancho + x] = valor.coerceIn(0, 255).toByte()
            }
        }
        return plano
    }

    private fun dentro(cuadro: Cuadrilatero, x: Float, y: Float): Boolean {
        val puntos = cuadro.esquinas
        var signo = 0
        for (indice in puntos.indices) {
            val a = puntos[indice]
            val b = puntos[(indice + 1) % 4]
            val cruz = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x)
            val actual = if (cruz > 0f) 1 else if (cruz < 0f) -1 else 0
            if (actual == 0) continue
            if (signo == 0) signo = actual else if (signo != actual) return false
        }
        return true
    }

    private companion object {
        const val FOTOGRAMAS = 30
        const val MEDIDAS = 20

        /**
         * Medio punto porcentual de la pantalla entre fotogramas.
         *
         * En un movil de 1080 px son cinco pixeles: por debajo de eso el ojo no
         * lo lee como movimiento sino como una linea quieta.
         */
        const val TEMBLOR_ACEPTABLE = 0.005f

        /** Con el papel muy girado se admite algo mas, pero no un salto. */
        const val TEMBLOR_ACEPTABLE_GIRADO = 0.010f
    }
}
