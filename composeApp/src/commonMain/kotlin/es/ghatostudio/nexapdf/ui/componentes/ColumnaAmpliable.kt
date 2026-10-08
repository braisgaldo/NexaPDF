package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

/**
 * Lectura continua: todas las paginas seguidas, en una columna que se ensancha.
 *
 * El zoom aqui no mueve la pagina, la ensancha, y el ancho de mas se recorre de
 * lado: es como se lee un documento largo cuando la letra es pequena. Dos dedos
 * ensanchan a voluntad; un doble toque ensancha a [EstadoEncuadre.ESCALA_DOBLE_TOQUE]
 * dejando bajo el dedo lo que estaba bajo el dedo, y otro doble toque devuelve
 * el ancho normal.
 *
 * @param pagina lo que se dibuja en cada pagina. Tiene que ocupar el ancho que
 *   recibe con una proporcion fija, que es lo que permite a [anclarColumna]
 *   saber de antemano cuanto va a medir.
 */
@Composable
fun ColumnaAmpliable(
    encuadre: EstadoEncuadre,
    lista: LazyListState,
    totalPaginas: Int,
    modifier: Modifier = Modifier,
    pagina: @Composable (Int) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val anchoBase = maxWidth
        val densidad = LocalDensity.current
        val medidas by rememberUpdatedState(
            with(densidad) { anchoBase.toPx() to MARGEN_LATERAL.toPx() },
        )

        // El desplazamiento lateral se sustituye entero en cada doble toque, en
        // lugar de moverse con `scrollTo`. Moverlo no sirve: el estado recorta
        // contra el ancho que tenia la columna **antes** de ensancharla, que con
        // la pagina entera es cero, asi que solo podria corregirse un fotograma
        // despues y se veria un salto hacia la izquierda. Uno nuevo nace sin
        // tope hasta que se mide, y se mide ya con el ancho nuevo.
        var lateral by remember { mutableStateOf(ScrollState(0)) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Antes del desplazamiento lateral, para recibir el toque en
                // coordenadas de la pantalla y no del documento ensanchado.
                .pointerInput(encuadre, lista) {
                    detectarDobleToque { punto ->
                        val (anchoVista, margen) = medidas
                        val despues = encuadre.escalaTrasDobleToque
                        val disposicion = lista.layoutInfo
                        val anclaje = anclarColumna(
                            punto = punto,
                            escalaAntes = encuadre.escala,
                            escalaDespues = despues,
                            anchoVista = anchoVista,
                            margen = margen,
                            lateral = lateral.value,
                            inicioVista = disposicion.viewportStartOffset,
                            paginas = disposicion.visibleItemsInfo,
                        )
                        // Las tres cosas antes de la siguiente medida, para que
                        // la columna aparezca ya ancha y en su sitio a la vez.
                        encuadre.ponerEscala(despues)
                        lateral = ScrollState(anclaje.lateral)
                        anclaje.pagina?.let {
                            lista.requestScrollToItem(it, anclaje.desplazamientoPagina)
                        }
                    }
                }
                .horizontalScroll(lateral)
                .encuadreDosDedos(encuadre),
        ) {
            LazyColumn(
                state = lista,
                modifier = Modifier.width(anchoBase * encuadre.escala),
                contentPadding = PaddingValues(horizontal = MARGEN_LATERAL, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(totalPaginas.coerceAtLeast(1)) { indice -> pagina(indice) }
            }
        }
    }
}

private val MARGEN_LATERAL = 12.dp
