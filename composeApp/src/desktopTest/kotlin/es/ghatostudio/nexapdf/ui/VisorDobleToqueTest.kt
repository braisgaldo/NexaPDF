package es.ghatostudio.nexapdf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import es.ghatostudio.nexapdf.data.RepositorioAjustes
import es.ghatostudio.nexapdf.di.ContenedorApp
import es.ghatostudio.nexapdf.di.LocalContenedor
import es.ghatostudio.nexapdf.domain.model.DireccionLectura
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.ed_pagina_siguiente
import es.ghatostudio.nexapdf.resources.visor_pagina_numero
import es.ghatostudio.nexapdf.ui.pantallas.AccionesVisor
import es.ghatostudio.nexapdf.ui.pantallas.PantallaVisor
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import java.lang.reflect.Proxy
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El doble toque dentro del visor de verdad, con sus dos formas de leer.
 *
 * Las pruebas del componente no bastan, y por un fallo concreto: en la lectura
 * continua el visor reiniciaba el encuadre cada vez que cambiaba de tamano, y
 * ensanchar la columna **es** cambiarla de tamano. El componente ampliaba bien
 * y el visor lo deshacia un fotograma despues. Solo se ve montando el visor.
 */
@OptIn(ExperimentalTestApi::class)
class VisorDobleToqueTest {

    private val total = 6

    /**
     * Un doble de una interfaz que solo sabe lo que [responder] le diga, y que
     * falla con el nombre del metodo en todo lo demas: si el visor empezara a
     * usar algo que esta prueba no preve, se sabria en lugar de pasar en verde.
     */
    private inline fun <reified T : Any> doble(noinline responder: (String) -> Any? = { null }): T {
        val nombre = T::class.simpleName
        return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { yo, metodo, args ->
            when (metodo.name) {
                "toString" -> "doble de $nombre"
                "hashCode" -> System.identityHashCode(yo)
                "equals" -> yo === args?.firstOrNull()
                else -> responder(metodo.name)
                    ?: throw UnsupportedOperationException("$nombre.${metodo.name}")
            }
        } as T
    }

    private fun contenedor(): ContenedorApp {
        val hoja = ImageBitmap(707, 1000)
        return ContenedorApp(
            motorPdf = doble { if (it == "renderizarPagina") ResultadoPdf.Exito(hoja) else null },
            conversor = doble(),
            servicios = doble(),
            ficheros = doble(),
            selector = doble(),
            ajustes = RepositorioAjustes(
                doble<DataStore<Preferences>> { if (it == "getData") emptyFlow<Preferences>() else null },
            ),
            escaner = doble(),
        )
    }

    private val pagina = mutableIntStateOf(0)

    private fun ComposeUiTest.visor(lectura: DireccionLectura) {
        val dependencias = contenedor()
        setContent {
            CompositionLocalProvider(LocalContenedor provides dependencias) {
                MaterialTheme {
                    Box(Modifier.size(360.dp, 720.dp).testTag("visor")) {
                        PantallaVisor(
                            ruta = "prueba.pdf",
                            nombreDocumento = "prueba.pdf",
                            paginaActual = pagina.intValue,
                            totalPaginas = total,
                            proporcion = 0.707f,
                            anchoRender = 707,
                            contrasena = null,
                            lectura = lectura,
                            secciones = emptyList(),
                            firmas = emptyList(),
                            formatearFecha = { "" },
                            snackbar = remember { SnackbarHostState() },
                            acciones = AccionesVisor(
                                alBuscar = { emptyList() },
                                alIrAPagina = { if (it in 0 until total) pagina.intValue = it },
                                alCompartir = {},
                            ),
                            alVolver = {},
                        )
                    }
                }
            }
        }
        esperar()
    }

    private fun ComposeUiTest.esperar() {
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
    }

    private fun ComposeUiTest.hoja(numero: Int): SemanticsNodeInteraction {
        val texto = runBlocking { getString(Res.string.visor_pagina_numero, numero) }
        return onNodeWithContentDescription(texto)
    }

    /**
     * Ancho con el que se ve la hoja, transformaciones incluidas. En la lectura
     * lateral el zoom es una capa grafica y no cambia el tamano de la caja;
     * esto mide lo que se ve, no lo que mide la caja.
     */
    private fun SemanticsNodeInteraction.anchoVisto(): Float {
        val coordenadas = fetchSemanticsNode().layoutInfo.coordinates
        val izquierda = coordenadas.localToRoot(Offset.Zero)
        val derecha = coordenadas.localToRoot(Offset(coordenadas.size.width.toFloat(), 0f))
        return derecha.x - izquierda.x
    }

    @Test
    fun `lectura lateral - dos toques acercan, otros dos alejan, y se sigue pasando de hoja`() =
        runComposeUiTest {
            visor(DireccionLectura.LATERAL)
            val normal = hoja(1).anchoVisto()

            hoja(1).performTouchInput { doubleClick(percentOffset(0.3f, 0.4f)) }
            esperar()
            val ampliada = hoja(1).anchoVisto()
            assertTrue(abs(ampliada / normal - 2.5f) < 0.05f, "la hoja paso de $normal a $ampliada")

            // Ampliada, deslizar mueve la hoja y no pasa de pagina.
            hoja(1).performTouchInput { swipeLeft() }
            esperar()
            assertEquals(0, pagina.intValue)

            hoja(1).performTouchInput { doubleClick(center) }
            esperar()
            assertTrue(abs(hoja(1).anchoVisto() - normal) < 1f, "no volvio a verse entera")

            hoja(1).performTouchInput { swipeLeft() }
            esperar()
            assertEquals(1, pagina.intValue)

            // Y de vuelta a la primera, que es por la que se abrio el documento:
            // la barra de abajo se quedaba en la 2 porque se comparaba con el
            // numero de pagina del momento de abrir.
            hoja(2).performTouchInput { swipeRight() }
            esperar()
            assertEquals(0, pagina.intValue)
        }

    @Test
    fun `lectura continua - la columna ensanchada no se encoge sola`() = runComposeUiTest {
        visor(DireccionLectura.VERTICAL)
        val normal = hoja(1).anchoVisto()

        hoja(1).performTouchInput { doubleClick(percentOffset(0.4f, 0.3f)) }
        esperar()
        val ampliada = hoja(1).anchoVisto()
        assertTrue(ampliada > normal * 2.4f, "la hoja paso de $normal a $ampliada")

        // Unos cuantos fotogramas despues sigue ancha. Antes de arreglarlo, el
        // visor la devolvia a su ancho en cuanto la media cambiaba.
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertTrue(abs(hoja(1).anchoVisto() - ampliada) < 1f, "la columna volvio sola a su ancho")

        // Leyendo de seguido se pasa a la hoja siguiente sin perder el ancho.
        // Se desliza dentro del documento: ni la barra de arriba ni la de abajo.
        val antes = pagina.intValue
        repeat(3) {
            onNodeWithTag("visor").performTouchInput {
                swipeUp(startY = height * 0.75f, endY = height * 0.2f)
            }
            esperar()
        }
        assertTrue(pagina.intValue > antes, "no se llego a cambiar de hoja (sigue en ${pagina.intValue})")
        val visible = hoja(pagina.intValue + 1).anchoVisto()
        assertTrue(abs(visible - ampliada) < 1f, "al pasar de hoja el ancho paso a $visible")

        onNodeWithTag("visor").performTouchInput { doubleClick(percentOffset(0.5f, 0.5f)) }
        esperar()
        assertTrue(abs(hoja(pagina.intValue + 1).anchoVisto() - normal) < 1f, "no volvio al ancho normal")
    }

    @Test
    fun `lectura continua - saltar con la barra vuelve al ancho normal`() = runComposeUiTest {
        // Un salto es otra cosa que leer de seguido: se llega a una hoja
        // cualquiera, y llegar con la columna ancha la dejaria a medio ver.
        visor(DireccionLectura.VERTICAL)
        val normal = hoja(1).anchoVisto()
        hoja(1).performTouchInput { doubleClick(center) }
        esperar()

        val siguiente = runBlocking { getString(Res.string.ed_pagina_siguiente) }
        onNodeWithContentDescription(siguiente).performClick()
        esperar()

        assertEquals(1, pagina.intValue)
        assertTrue(abs(hoja(2).anchoVisto() - normal) < 1f, "el salto conservo el ancho")
    }
}
