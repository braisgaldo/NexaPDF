package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Reordenar arrastrando, para listas y rejillas perezosas.
 *
 * El gesto empieza con una pulsacion larga, que es lo que espera cualquiera en
 * Android y ademas evita que un desplazamiento normal mueva cosas sin querer.
 *
 * La logica es la misma para las dos: se sigue la posicion del dedo dentro del
 * area visible, se mira que elemento hay debajo y, si no es el que se arrastra,
 * se intercambian. Seguir el dedo en vez de acumular desplazamientos evita el
 * salto que aparece cuando el elemento cambia de sitio a mitad del gesto.
 *
 * El arrastre no sustituye a los botones de subir y bajar: un gesto de arrastre
 * no existe para quien navega con TalkBack, asi que las listas mantienen sus
 * botones y esto es solo un atajo para quien usa el dedo.
 */
class EstadoReordenable internal constructor(
    private val posicionesVisibles: () -> List<ElementoVisible>,
    private val alMover: (desde: Int, hasta: Int) -> Unit,
) {
    /** Un elemento colocado en pantalla: su indice y su rectangulo. */
    internal data class ElementoVisible(
        val indice: Int,
        val izquierda: Int,
        val arriba: Int,
        val derecha: Int,
        val abajo: Int,
    ) {
        fun contiene(punto: Offset): Boolean =
            punto.x >= izquierda && punto.x < derecha && punto.y >= arriba && punto.y < abajo

        val centro: Offset
            get() = Offset((izquierda + derecha) / 2f, (arriba + abajo) / 2f)
    }

    /** Indice del elemento que se esta arrastrando, o `null`. */
    var indiceArrastrado by mutableStateOf<Int?>(null)
        private set

    private var posicionDedo by mutableStateOf(Offset.Zero)

    /** Desplazamiento que hay que aplicar al elemento arrastrado para seguir al dedo. */
    fun desplazamientoDe(indice: Int): Offset {
        if (indice != indiceArrastrado) return Offset.Zero
        val elemento = posicionesVisibles().firstOrNull { it.indice == indice } ?: return Offset.Zero
        return posicionDedo - elemento.centro
    }

    internal fun empezar(punto: Offset) {
        posicionDedo = punto
        indiceArrastrado = posicionesVisibles().firstOrNull { it.contiene(punto) }?.indice
    }

    internal fun arrastrar(delta: Offset) {
        if (indiceArrastrado == null) return
        posicionDedo += delta

        val actual = indiceArrastrado ?: return
        val destino = posicionesVisibles().firstOrNull { it.contiene(posicionDedo) } ?: return
        if (destino.indice != actual) {
            alMover(actual, destino.indice)
            indiceArrastrado = destino.indice
        }
    }

    internal fun terminar() {
        indiceArrastrado = null
        posicionDedo = Offset.Zero
    }
}

/** Modificador que instala el gesto de arrastre sobre el contenedor. */
fun Modifier.reordenable(estado: EstadoReordenable): Modifier = pointerInput(estado) {
    detectDragGesturesAfterLongPress(
        onDragStart = { estado.empezar(it) },
        onDrag = { cambio, desplazamiento ->
            cambio.consume()
            estado.arrastrar(desplazamiento)
        },
        onDragEnd = { estado.terminar() },
        onDragCancel = { estado.terminar() },
    )
}

@Composable
fun rememberReordenable(
    estadoRejilla: LazyGridState,
    alMover: (Int, Int) -> Unit,
): EstadoReordenable = remember(estadoRejilla) {
    EstadoReordenable(
        posicionesVisibles = {
            estadoRejilla.layoutInfo.visibleItemsInfo.map { item ->
                EstadoReordenable.ElementoVisible(
                    indice = item.index,
                    izquierda = item.offset.x,
                    arriba = item.offset.y,
                    derecha = item.offset.x + item.size.width,
                    abajo = item.offset.y + item.size.height,
                )
            }
        },
        alMover = alMover,
    )
}

@Composable
fun rememberReordenable(
    estadoLista: LazyListState,
    alMover: (Int, Int) -> Unit,
): EstadoReordenable = remember(estadoLista) {
    EstadoReordenable(
        posicionesVisibles = {
            estadoLista.layoutInfo.visibleItemsInfo.map { item ->
                EstadoReordenable.ElementoVisible(
                    indice = item.index,
                    // Una lista vertical ocupa todo el ancho: para el gesto solo
                    // cuenta la coordenada vertical.
                    izquierda = Int.MIN_VALUE / 2,
                    arriba = item.offset,
                    derecha = Int.MAX_VALUE / 2,
                    abajo = item.offset + item.size,
                )
            }
        },
        alMover = alMover,
    )
}
