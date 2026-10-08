package es.ghatostudio.nexapdf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.ui.componentes.ColumnaAmpliable
import es.ghatostudio.nexapdf.ui.componentes.EstadoEncuadre
import es.ghatostudio.nexapdf.ui.componentes.EstadoEncuadre.Companion.ESCALA_DOBLE_TOQUE
import es.ghatostudio.nexapdf.ui.componentes.encuadreConPaso
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El doble toque con toques de verdad.
 *
 * Las cuentas se prueban aparte (`DobleToqueTest`); aqui lo que se prueba es
 * el gesto: toques, temblores, arrastres y pellizcos inyectados por la misma
 * tuberia de entrada que usa la aplicacion, con sus tiempos y su holgura. Es
 * donde estaba el riesgo. El detector de serie de Compose habria pasado las
 * pruebas de cuentas y fallado al alejar con el dedo temblando un pixel.
 */
@OptIn(ExperimentalTestApi::class)
class GestosEncuadreTest {

    private fun ComposeUiTest.paginaSuelta(estado: EstadoEncuadre): SemanticsNodeInteraction {
        setContent {
            Box(Modifier.size(300.dp, 500.dp).testTag("pagina").encuadreConPaso(estado))
        }
        return onNodeWithTag("pagina")
    }

    /** Deja pasar el plazo del doble toque y lo que quede de animacion. */
    private fun ComposeUiTest.esperarAQueSeAsiente() {
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun EstadoEncuadre.dondeQueda(punto: Offset): Offset {
        val centro = Offset(tamano.width / 2f, tamano.height / 2f)
        return centro + (punto - centro) * escala + desplazamiento
    }

    @Test
    fun `dos toques acercan al punto tocado y otros dos devuelven la pagina entera`() =
        runComposeUiTest {
            val estado = EstadoEncuadre()
            val pagina = paginaSuelta(estado)
            var punto = Offset.Zero

            pagina.performTouchInput {
                punto = percentOffset(0.25f, 0.3f)
                doubleClick(punto)
            }
            esperarAQueSeAsiente()

            assertEquals(ESCALA_DOBLE_TOQUE, estado.escala)
            val donde = estado.dondeQueda(punto)
            assertTrue(
                abs(donde.x - punto.x) < 1f && abs(donde.y - punto.y) < 1f,
                "lo tocado en $punto acabo en $donde",
            )

            pagina.performTouchInput { doubleClick(percentOffset(0.7f, 0.8f)) }
            esperarAQueSeAsiente()

            assertEquals(1f, estado.escala)
            assertEquals(Offset.Zero, estado.desplazamiento)
        }

    @Test
    fun `alejar funciona aunque el dedo tiemble`() = runComposeUiTest {
        // La prueba que justifica no usar detectTapGestures. Con la pagina
        // ampliada, el arrastre consume cualquier movimiento, tambien el de un
        // par de pixeles que tiene todo toque real; ese detector lo daba por
        // cancelado y alejar fallaba a ratos.
        val estado = EstadoEncuadre()
        val pagina = paginaSuelta(estado)
        pagina.performTouchInput { doubleClick(center) }
        esperarAQueSeAsiente()
        assertTrue(estado.ampliada)

        pagina.performTouchInput {
            down(center)
            moveBy(Offset(3f, 2f))
            up()
            advanceEventTime(150)
            down(center + Offset(2f, -1f))
            moveBy(Offset(-2f, 3f))
            up()
        }
        esperarAQueSeAsiente()

        assertEquals(1f, estado.escala, "con un temblor de tres pixeles no alejo")
    }

    @Test
    fun `un toque solo no hace nada`() = runComposeUiTest {
        val estado = EstadoEncuadre()
        paginaSuelta(estado).performTouchInput { click(percentOffset(0.3f, 0.3f)) }
        esperarAQueSeAsiente()
        assertEquals(1f, estado.escala)
    }

    @Test
    fun `dos toques en sitios distintos no son un doble toque`() = runComposeUiTest {
        val estado = EstadoEncuadre()
        paginaSuelta(estado).performTouchInput {
            click(percentOffset(0.1f, 0.1f))
            advanceEventTime(120)
            click(percentOffset(0.9f, 0.9f))
        }
        esperarAQueSeAsiente()
        assertEquals(1f, estado.escala)
    }

    @Test
    fun `dos toques demasiado separados en el tiempo no son un doble toque`() =
        runComposeUiTest {
            val estado = EstadoEncuadre()
            paginaSuelta(estado).performTouchInput {
                click(center)
                advanceEventTime(800)
                click(center)
            }
            esperarAQueSeAsiente()
            assertEquals(1f, estado.escala)
        }

    @Test
    fun `una pulsacion larga y un toque no son un doble toque`() = runComposeUiTest {
        val estado = EstadoEncuadre()
        paginaSuelta(estado).performTouchInput {
            longClick(center)
            advanceEventTime(100)
            click(center)
        }
        esperarAQueSeAsiente()
        assertEquals(1f, estado.escala)
    }

    @Test
    fun `el pellizco sigue funcionando y no se confunde con toques`() = runComposeUiTest {
        val estado = EstadoEncuadre()
        paginaSuelta(estado).performTouchInput {
            pinch(
                start0 = center + Offset(-20f, 0f),
                end0 = center + Offset(-80f, 0f),
                start1 = center + Offset(20f, 0f),
                end1 = center + Offset(80f, 0f),
            )
        }
        esperarAQueSeAsiente()
        assertTrue(estado.escala > 2f, "el pellizco dejo la escala en ${estado.escala}")
        assertTrue(estado.escala != ESCALA_DOBLE_TOQUE, "el pellizco se tomo por un doble toque")
    }

    @Test
    fun `en el pager deslizar pasa de hoja y dos toques acercan`() = runComposeUiTest {
        // La convivencia que importa en el visor. El detector nuevo no consume
        // nada hasta estar seguro, asi que deslizar tiene que seguir pasando de
        // hoja; y con la pagina ampliada, deslizar mueve la pagina y no pasa.
        val estado = EstadoEncuadre()
        val hojas = PagerState { 5 }
        setContent {
            HorizontalPager(
                state = hojas,
                userScrollEnabled = !estado.ampliada,
                modifier = Modifier.size(300.dp, 500.dp).testTag("pager"),
            ) { indice ->
                Box(Modifier.fillMaxSize().testTag("hoja$indice").encuadreConPaso(estado))
            }
        }

        onNodeWithTag("pager").performTouchInput { swipeLeft() }
        esperarAQueSeAsiente()
        assertEquals(1, hojas.currentPage)
        assertEquals(1f, estado.escala, "deslizar se tomo por un toque")

        onNodeWithTag("hoja1").performTouchInput { doubleClick(percentOffset(0.3f, 0.6f)) }
        esperarAQueSeAsiente()
        assertEquals(ESCALA_DOBLE_TOQUE, estado.escala)

        val antes = estado.desplazamiento
        onNodeWithTag("pager").performTouchInput { swipeLeft() }
        esperarAQueSeAsiente()
        assertEquals(1, hojas.currentPage, "con la pagina ampliada, deslizar paso de hoja")
        assertTrue(estado.desplazamiento != antes, "con la pagina ampliada, deslizar no la movio")

        onNodeWithTag("hoja1").performTouchInput { doubleClick(center) }
        esperarAQueSeAsiente()
        assertEquals(1f, estado.escala)

        onNodeWithTag("pager").performTouchInput { swipeLeft() }
        esperarAQueSeAsiente()
        assertEquals(2, hojas.currentPage, "despues de alejar no se pudo pasar de hoja")
    }

    // --- Lectura continua ----------------------------------------------------

    private fun ComposeUiTest.columna(estado: EstadoEncuadre, lista: LazyListState) {
        setContent {
            ColumnaAmpliable(
                encuadre = estado,
                lista = lista,
                totalPaginas = 8,
                modifier = Modifier.size(360.dp, 640.dp).testTag("columna"),
            ) { indice ->
                Box(Modifier.fillMaxWidth().aspectRatio(0.707f).testTag("hoja$indice"))
            }
        }
    }

    /** Rectangulo de una hoja en la pantalla, sin recortar por los bordes. */
    private fun ComposeUiTest.rectangulo(indice: Int): Pair<Offset, Offset>? = runCatching {
        val nodo = onNodeWithTag("hoja$indice").fetchSemanticsNode()
        nodo.positionInRoot to Offset(nodo.size.width.toFloat(), nodo.size.height.toFloat())
    }.getOrNull()

    /**
     * Toca dos veces en una fraccion de la columna y comprueba que lo que
     * habia debajo sigue debajo, a lo ancho y a lo alto.
     */
    private fun ComposeUiTest.dobleToqueAnclado(
        estado: EstadoEncuadre,
        fraccion: Offset,
        escalaEsperada: Float,
    ) {
        val nodo = onNodeWithTag("columna").fetchSemanticsNode()
        val enPantalla = nodo.positionInRoot +
            Offset(nodo.size.width * fraccion.x, nodo.size.height * fraccion.y)

        val (indice, relativo) = (0 until 8).firstNotNullOf { i ->
            val (esquina, tamano) = rectangulo(i) ?: return@firstNotNullOf null
            val dentro = enPantalla - esquina
            if (dentro.y in 0f..tamano.y) i to Offset(dentro.x / tamano.x, dentro.y / tamano.y) else null
        }

        onNodeWithTag("columna").performTouchInput { doubleClick(percentOffset(fraccion.x, fraccion.y)) }
        esperarAQueSeAsiente()

        val (esquina, tamano) = rectangulo(indice) ?: error("la hoja $indice ya no se ve")
        val ahora = esquina + Offset(relativo.x * tamano.x, relativo.y * tamano.y)
        // Al volver al ancho normal la hoja entera cabe y ya no hay desplazamiento
        // lateral que valga: lo tocado vuelve a su sitio a lo ancho, y lo que se
        // conserva es la altura.
        val aLoAncho = escalaEsperada <= 1f || abs(ahora.x - enPantalla.x) <= 2f
        assertTrue(
            aLoAncho && abs(ahora.y - enPantalla.y) <= 2f,
            "lo tocado de la hoja $indice estaba en $enPantalla y acabo en $ahora",
        )
        assertEquals(escalaEsperada, estado.escala)
    }

    @Test
    fun `en continuo dos toques ensanchan sin perder el sitio y otros dos lo devuelven`() =
        runComposeUiTest {
            val estado = EstadoEncuadre()
            columna(estado, LazyListState())
            val anchoNormal = rectangulo(0)!!.second.x

            dobleToqueAnclado(estado, Offset(0.7f, 0.6f), ESCALA_DOBLE_TOQUE)
            val anchoAmpliado = rectangulo(0)?.second?.x ?: rectangulo(1)!!.second.x
            assertTrue(anchoAmpliado > anchoNormal * 2.4f, "la hoja paso de $anchoNormal a $anchoAmpliado")

            dobleToqueAnclado(estado, Offset(0.3f, 0.4f), 1f)
        }

    @Test
    fun `en continuo se puede tocar una hoja que empieza a media pantalla`() =
        runComposeUiTest {
            // La hoja de abajo empieza por debajo del borde de la lista; para
            // dejar su parte de arriba bajo el dedo hay que pedir a la lista
            // un desplazamiento negativo, y eso es lo que se comprueba.
            val estado = EstadoEncuadre()
            columna(estado, LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 250))

            val (esquina, _) = rectangulo(1)!!
            val nodo = onNodeWithTag("columna").fetchSemanticsNode()
            val fraccionY = (esquina.y + 30f - nodo.positionInRoot.y) / nodo.size.height
            dobleToqueAnclado(estado, Offset(0.5f, fraccionY), ESCALA_DOBLE_TOQUE)
        }

    @Test
    fun `en continuo desplazarse no deshace la ampliacion`() = runComposeUiTest {
        val estado = EstadoEncuadre()
        columna(estado, LazyListState())
        dobleToqueAnclado(estado, Offset(0.5f, 0.5f), ESCALA_DOBLE_TOQUE)

        onNodeWithTag("columna").performTouchInput { swipeUp() }
        esperarAQueSeAsiente()
        assertEquals(ESCALA_DOBLE_TOQUE, estado.escala)
    }
}
