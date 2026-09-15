package es.ghatostudio.nexapdf.domain

import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.escaner.DeteccionDocumento
import es.ghatostudio.nexapdf.domain.escaner.DetectorDocumento
import es.ghatostudio.nexapdf.domain.escaner.SuavizadorDeteccion
import es.ghatostudio.nexapdf.domain.model.Punto
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas del detector de bordes.
 *
 * Se prueba con escenas sinteticas y no con fotos: una foto de verdad mezcla
 * veinte cosas a la vez y cuando la prueba falla no dice cual. Aqui cada caso
 * aisla una: un folio recto, uno en perspectiva, uno girado, una mesa vacia y
 * una escena con lineas de sobra donde el detector tiene que elegir.
 */
class DetectorDocumentoTest {

    private val ancho = 480
    private val alto = 640

    @Test
    fun `un folio recto se detecta donde esta`() {
        val papel = rectangulo(0.15f, 0.12f, 0.85f, 0.88f)
        val deteccion = DetectorDocumento().detectar(escena(papel), ancho, alto)

        val cuadro = assertNotNull(deteccion.cuadro, "no se detecto ningun papel")
        assertCerca(papel, cuadro, tolerancia = 0.04f)
        assertTrue(
            deteccion.confianza > 0.75f,
            "la confianza deberia ser alta en un folio limpio: ${deteccion.confianza}",
        )
    }

    @Test
    fun `un folio en perspectiva se detecta con sus cuatro esquinas`() {
        // Lo que sale al fotografiar un papel desde arriba y de lado: el borde
        // de abajo, mas cerca de la camara, mas ancho que el de arriba.
        val papel = Cuadrilatero(
            supIzq = Punto(0.26f, 0.16f),
            supDer = Punto(0.74f, 0.18f),
            infDer = Punto(0.88f, 0.83f),
            infIzq = Punto(0.13f, 0.81f),
        )
        val deteccion = DetectorDocumento().detectar(escena(papel), ancho, alto)

        val cuadro = assertNotNull(deteccion.cuadro, "no se detecto el papel inclinado")
        assertCerca(papel, cuadro, tolerancia = 0.06f)
        assertTrue(deteccion.hayPapel, "confianza insuficiente: ${deteccion.confianza}")
    }

    @Test
    fun `un folio girado conserva el orden de las esquinas`() {
        val papel = Cuadrilatero(
            supIzq = Punto(0.30f, 0.14f),
            supDer = Punto(0.86f, 0.32f),
            infDer = Punto(0.70f, 0.86f),
            infIzq = Punto(0.14f, 0.68f),
        )
        val deteccion = DetectorDocumento().detectar(escena(papel), ancho, alto)

        val cuadro = assertNotNull(deteccion.cuadro, "no se detecto el papel girado")
        // El contrato del orden: la esquina "superior izquierda" tiene que ser
        // la de suma de coordenadas mas pequena. Si esto se rompe, el
        // enderezado sale del reves y la pagina aparece boca abajo.
        val porSuma = cuadro.esquinas.minByOrNull { it.x + it.y }
        assertEquals(cuadro.supIzq, porSuma)
        assertTrue(cuadro.esConvexo)
    }

    @Test
    fun `una superficie vacia no inventa un documento`() {
        val vacia = ByteArray(ancho * alto) { 128.toByte() }
        assertNull(DetectorDocumento().detectar(vacia, ancho, alto).cuadro)
    }

    @Test
    fun `el ruido suelto no se toma por un documento`() {
        // Un patron de puntos sueltos, sin ninguna recta larga: el gradiente
        // dispara pero no hay bordes que seguir.
        val ruido = ByteArray(ancho * alto) { 60 }
        var semilla = 12345
        for (indice in ruido.indices) {
            semilla = semilla * 1103515245 + 12345
            if ((semilla ushr 16) % 7 == 0) ruido[indice] = 220.toByte()
        }
        val deteccion = DetectorDocumento().detectar(ruido, ancho, alto)
        assertFalse(
            deteccion.hayPapel,
            "el ruido no deberia pasar el umbral: ${deteccion.confianza}",
        )
    }

    @Test
    fun `el salto de fila de la camara se respeta`() {
        // La camara alinea las filas: hay bytes de relleno al final de cada una.
        // Si se ignoran, la imagen sale inclinada y no se detecta nada.
        val papel = rectangulo(0.18f, 0.15f, 0.82f, 0.85f)
        val relleno = 64
        val salto = ancho + relleno
        val conRelleno = ByteArray(salto * alto) { 0 }
        val plano = escena(papel)
        for (y in 0 until alto) {
            plano.copyInto(
                destination = conRelleno,
                destinationOffset = y * salto,
                startIndex = y * ancho,
                endIndex = (y + 1) * ancho,
            )
        }

        val deteccion = DetectorDocumento().detectar(conRelleno, ancho, alto, salto)
        val cuadro = assertNotNull(deteccion.cuadro, "el salto de fila rompio la deteccion")
        assertCerca(papel, cuadro, tolerancia = 0.05f)
    }

    @Test
    fun `la misma instancia sirve para varios fotogramas`() {
        // Los buffers se reaprovechan entre llamadas; si el acumulador no se
        // limpiara, el segundo fotograma arrastraria los votos del primero.
        val detector = DetectorDocumento()
        val primero = rectangulo(0.10f, 0.10f, 0.60f, 0.60f)
        val segundo = rectangulo(0.35f, 0.30f, 0.90f, 0.88f)

        detector.detectar(escena(primero), ancho, alto)
        val deteccion = detector.detectar(escena(segundo), ancho, alto)

        val cuadro = assertNotNull(deteccion.cuadro)
        assertCerca(segundo, cuadro, tolerancia = 0.05f)
    }

    @Test
    fun `el suavizado olvida el papel despues de varios fotogramas sin el`() {
        val suavizador = SuavizadorDeteccion()
        val papel = rectangulo(0.2f, 0.2f, 0.8f, 0.8f)
        suavizador.siguiente(DeteccionDocumento(papel, 0.9f))

        repeat(3) {
            assertNotNull(
                suavizador.siguiente(DeteccionDocumento.NADA).cuadro,
                "un fotograma malo no deberia borrar el encuadre",
            )
        }
        assertNull(
            suavizador.siguiente(DeteccionDocumento.NADA).cuadro,
            "tras varios fotogramas sin papel hay que soltar el encuadre",
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

    /** Un papel claro sobre una mesa oscura, que es el caso que hay que resolver. */
    private fun escena(papel: Cuadrilatero, fondo: Int = 45, hoja: Int = 215): ByteArray {
        val plano = ByteArray(ancho * alto) { fondo.toByte() }
        for (y in 0 until alto) {
            val ny = (y + 0.5f) / alto
            for (x in 0 until ancho) {
                val nx = (x + 0.5f) / ancho
                if (dentro(papel, nx, ny)) plano[y * ancho + x] = hoja.toByte()
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

    private fun assertCerca(esperado: Cuadrilatero, obtenido: Cuadrilatero, tolerancia: Float) {
        esperado.esquinas.zip(obtenido.esquinas).forEachIndexed { indice, (a, b) ->
            assertTrue(
                abs(a.x - b.x) <= tolerancia && abs(a.y - b.y) <= tolerancia,
                "la esquina $indice esta en (${b.x}, ${b.y}) y se esperaba (${a.x}, ${a.y})",
            )
        }
    }
}
