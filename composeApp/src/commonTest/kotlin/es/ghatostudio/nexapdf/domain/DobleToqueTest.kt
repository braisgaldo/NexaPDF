package es.ghatostudio.nexapdf.domain

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import es.ghatostudio.nexapdf.ui.componentes.EstadoEncuadre
import es.ghatostudio.nexapdf.ui.componentes.EstadoEncuadre.Companion.ESCALA_DOBLE_TOQUE
import es.ghatostudio.nexapdf.ui.componentes.PosicionEncuadre
import es.ghatostudio.nexapdf.ui.componentes.anclarColumna
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Las cuentas del doble toque, sin pantalla de por medio.
 *
 * Lo que se le pide al gesto es una sola cosa: **lo que estaba bajo el dedo
 * sigue bajo el dedo**. Por eso casi todas las pruebas tocan lejos del centro.
 * En el centro exacto cualquier calculo acierta, porque ampliar respecto al
 * centro de la vista —que es lo que hace el pellizco— deja el centro donde
 * estaba; una prueba que tocara ahi pasaria igual con el codigo equivocado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DobleToqueTest {

    private fun estado(ancho: Int, alto: Int) = EstadoEncuadre().apply {
        tamano = IntSize(ancho, alto)
    }

    /** Donde acaba en pantalla lo que estaba en [punto] con la pagina entera. */
    private fun dondeQueda(estado: EstadoEncuadre, destino: PosicionEncuadre, punto: Offset): Offset {
        val centro = Offset(estado.tamano.width / 2f, estado.tamano.height / 2f)
        return centro + (punto - centro) * destino.escala + destino.desplazamiento
    }

    /**
     * Reloj de fotogramas para las pruebas: uno cada 16 ms de tiempo virtual,
     * que es lo que da una pantalla de 60 Hz.
     */
    private class RelojDePrueba : MonotonicFrameClock {
        private var ahora = 0L
        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            delay(16)
            ahora += 16_000_000L
            return onFrame(ahora)
        }
    }

    @Test
    fun `lo que estaba bajo el dedo sigue bajo el dedo`() {
        // Incluidas las cuatro esquinas: el tope del desplazamiento es justo el
        // que deja una esquina en su sitio, asi que ahi se ve si sobra o falta.
        val puntos = listOf(
            Offset(300f, 700f),
            Offset(900f, 1600f),
            Offset(120f, 1850f),
            Offset(0f, 0f),
            Offset(1080f, 0f),
            Offset(0f, 2000f),
            Offset(1080f, 2000f),
        )
        for (punto in puntos) {
            val vista = estado(1080, 2000)
            val donde = dondeQueda(vista, vista.destinoDobleToque(punto), punto)
            assertTrue(
                abs(donde.x - punto.x) < 1f && abs(donde.y - punto.y) < 1f,
                "el toque en $punto acabo en $donde",
            )
        }
    }

    @Test
    fun `acerca a la escala del doble toque`() {
        val vista = estado(1080, 2000)
        assertEquals(ESCALA_DOBLE_TOQUE, vista.destinoDobleToque(Offset(200f, 300f)).escala)
        assertEquals(ESCALA_DOBLE_TOQUE, vista.escalaTrasDobleToque)
    }

    @Test
    fun `con la pagina ampliada vuelve a verla entera`() {
        // Da igual como se ampliara: pellizcando, buscando o con otro doble toque.
        val pellizcada = estado(1080, 2000).apply { aplicar(3.2f, Offset(150f, -400f)) }
        val buscada = estado(1080, 2000).apply {
            enfocar(Offset(0.2f, 0.7f), 0.1f, 0.02f, 0.707f)
        }
        for (vista in listOf(pellizcada, buscada)) {
            val destino = vista.destinoDobleToque(Offset(800f, 300f))
            assertEquals(1f, destino.escala)
            assertEquals(Offset.Zero, destino.desplazamiento)
            assertEquals(1f, vista.escalaTrasDobleToque)
        }
    }

    @Test
    fun `la animacion acaba exacta, en los dos sentidos`() = runTest {
        val vista = estado(1080, 2000)
        val punto = Offset(250f, 1500f)
        val esperado = vista.destinoDobleToque(punto)

        withContext(RelojDePrueba()) { vista.alternarConDobleToque(punto) }
        assertEquals(esperado.escala, vista.escala)
        assertEquals(esperado.desplazamiento, vista.desplazamiento)

        // Y al volver, 1 exacto: con un 1,0000001 interpolado la pagina seguiria
        // contando como ampliada y el pager no dejaria pasar de hoja.
        withContext(RelojDePrueba()) { vista.alternarConDobleToque(punto) }
        assertEquals(1f, vista.escala)
        assertEquals(Offset.Zero, vista.desplazamiento)
        assertFalse(vista.ampliada)
    }

    @Test
    fun `la animacion pasa por el medio y no salta`() = runTest {
        val vista = estado(1080, 2000)
        val reloj = RelojDePrueba()
        val animacion = launch(reloj) { vista.alternarConDobleToque(Offset(300f, 400f)) }

        advanceTimeBy(100)
        assertTrue(
            vista.escala > 1.05f && vista.escala < ESCALA_DOBLE_TOQUE - 0.05f,
            "a mitad de animacion la escala era ${vista.escala}",
        )
        animacion.join()
        assertEquals(ESCALA_DOBLE_TOQUE, vista.escala)
    }

    @Test
    fun `pellizcar a mitad de animacion la para`() = runTest {
        // Si la animacion siguiera, cada fotograma pisaria lo que hace el dedo.
        val vista = estado(1080, 2000)
        val reloj = RelojDePrueba()
        launch(reloj) { vista.alternarConDobleToque(Offset(300f, 400f)) }

        advanceTimeBy(60)
        vista.aplicar(1.1f, Offset(10f, 0f))
        val trasElDedo = vista.escala
        advanceUntilIdle()

        assertEquals(trasElDedo, vista.escala, "la animacion siguio despues del pellizco")
    }

    @Test
    fun `un segundo doble toque a mitad de animacion manda sobre el primero`() = runTest {
        val vista = estado(1080, 2000)
        val reloj = RelojDePrueba()
        launch(reloj) { vista.alternarConDobleToque(Offset(300f, 400f)) }
        advanceTimeBy(100)
        // A medio camino ya cuenta como ampliada, asi que el segundo aleja.
        launch(reloj) { vista.alternarConDobleToque(Offset(300f, 400f)) }
        advanceUntilIdle()

        assertEquals(1f, vista.escala)
        assertEquals(Offset.Zero, vista.desplazamiento)
    }

    // --- Lectura continua ----------------------------------------------------

    private class Pagina(
        override val index: Int,
        override val offset: Int,
        override val size: Int,
    ) : LazyListItemInfo {
        override val key: Any get() = index
    }

    /**
     * Paginas como las pone la lista: proporcion fija, el ancho de la columna
     * menos los margenes, y separadas.
     */
    private fun paginas(
        anchoVista: Float,
        margen: Float,
        escala: Float,
        primera: Int,
        desplazamiento: Int,
        separacion: Int = 25,
        proporcion: Float = 0.707f,
    ): List<Pagina> {
        val alto = ((anchoVista * escala - 2 * margen) / proporcion).toInt()
        return (0 until 4).map { i ->
            Pagina(primera + i, -desplazamiento + i * (alto + separacion), alto)
        }
    }

    @Test
    fun `en continuo el punto tocado sigue bajo el dedo`() {
        val ancho = 1000f
        val margen = 30f
        val inicioVista = -20 // el margen de arriba de la lista
        val antes = paginas(ancho, margen, escala = 1f, primera = 3, desplazamiento = 200)

        for (punto in listOf(Offset(700f, 900f), Offset(80f, 300f), Offset(950f, 1700f))) {
            val anclaje = anclarColumna(
                punto = punto,
                escalaAntes = 1f,
                escalaDespues = ESCALA_DOBLE_TOQUE,
                anchoVista = ancho,
                margen = margen,
                lateral = 0,
                inicioVista = inicioVista,
                paginas = antes,
            )

            // Que parte de que pagina habia bajo el dedo.
            val y = punto.y + inicioVista
            val tocada = antes.first { y >= it.offset && y < it.offset + it.size }
            assertEquals(tocada.index, anclaje.pagina)
            val fx = (punto.x - margen) / (ancho - 2 * margen)
            val fy = (y - tocada.offset) / tocada.size

            // Donde queda esa misma parte con la columna ancha y las barras
            // movidas segun el anclaje.
            val anchoPagina = ancho * ESCALA_DOBLE_TOQUE - 2 * margen
            val altoPagina = tocada.size * anchoPagina / (ancho - 2 * margen)
            val x = margen + fx * anchoPagina - anclaje.lateral
            val arriba = -anclaje.desplazamientoPagina
            val yFinal = arriba + fy * altoPagina - inicioVista
            assertTrue(
                abs(x - punto.x) <= 1f && abs(yFinal - punto.y) <= 1f,
                "el toque en $punto acabo en ($x, $yFinal)",
            )
        }
    }

    @Test
    fun `en continuo la pagina tocada puede empezar por debajo del borde`() {
        // Tocar la parte de arriba de una pagina que empieza a media pantalla:
        // ampliada, su borde superior tiene que quedar por debajo del borde de
        // la lista, y eso es un desplazamiento negativo.
        val antes = paginas(1000f, 30f, escala = 1f, primera = 3, desplazamiento = 200)
        val segunda = antes[1]
        val punto = Offset(500f, segunda.offset + 60f)

        val anclaje = anclarColumna(
            punto, 1f, ESCALA_DOBLE_TOQUE, 1000f, 30f, 0, 0, antes,
        )

        assertEquals(segunda.index, anclaje.pagina)
        assertTrue(anclaje.desplazamientoPagina < 0, "fue ${anclaje.desplazamientoPagina}")
    }

    @Test
    fun `en continuo alejar vuelve al borde izquierdo`() {
        val antes = paginas(1000f, 30f, escala = ESCALA_DOBLE_TOQUE, primera = 0, desplazamiento = 900)
        val anclaje = anclarColumna(
            punto = Offset(600f, 800f),
            escalaAntes = ESCALA_DOBLE_TOQUE,
            escalaDespues = 1f,
            anchoVista = 1000f,
            margen = 30f,
            lateral = 1200,
            inicioVista = 0,
            paginas = antes,
        )
        assertEquals(0, anclaje.lateral)
    }

    @Test
    fun `en continuo el desplazamiento lateral no se sale de la columna`() {
        // De punta a punta, margenes incluidos: tocar en el margen no puede
        // pedir un desplazamiento que deje hueco vacio a un lado.
        val maximo = (1000f * ESCALA_DOBLE_TOQUE - 1000f).toInt()
        for (x in 0..1000 step 25) {
            val anclaje = anclarColumna(
                punto = Offset(x.toFloat(), 500f),
                escalaAntes = 1f,
                escalaDespues = ESCALA_DOBLE_TOQUE,
                anchoVista = 1000f,
                margen = 30f,
                lateral = 0,
                inicioVista = 0,
                paginas = paginas(1000f, 30f, 1f, 0, 0),
            )
            assertTrue(anclaje.lateral in 0..maximo, "tocando en x=$x pidio ${anclaje.lateral}")
        }
    }

    @Test
    fun `en continuo un toque entre dos paginas se queda con la mas cercana`() {
        val antes = paginas(1000f, 30f, escala = 1f, primera = 0, desplazamiento = 0, separacion = 40)
        val hueco = antes[0].offset + antes[0].size + 10f
        val anclaje = anclarColumna(
            Offset(500f, hueco), 1f, ESCALA_DOBLE_TOQUE, 1000f, 30f, 0, 0, antes,
        )
        assertEquals(0, anclaje.pagina)
    }

    @Test
    fun `en continuo sin paginas a la vista solo se mueve de lado`() {
        val anclaje = anclarColumna(
            Offset(500f, 500f), 1f, ESCALA_DOBLE_TOQUE, 1000f, 30f, 0, 0, emptyList(),
        )
        assertNull(anclaje.pagina)
    }
}
