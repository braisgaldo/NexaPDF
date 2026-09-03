package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Zoom y desplazamiento de una pagina.
 *
 * Se guarda aparte del contenido porque lo usan dos pantallas distintas, el
 * visor y el editor, y porque el editor necesita convertir las coordenadas del
 * dedo a coordenadas de pagina teniendo en cuenta el encuadre: sin eso, con la
 * pagina ampliada se dibujaria donde no toca.
 */
@Stable
class EstadoEncuadre {
    var escala by mutableFloatStateOf(1f)
        private set

    var desplazamiento by mutableStateOf(Offset.Zero)
        private set

    var tamano by mutableStateOf(IntSize.Zero)
        internal set

    val ampliada: Boolean get() = escala > 1.01f

    fun aplicar(cambioEscala: Float, arrastre: Offset) {
        val nueva = (escala * cambioEscala).coerceIn(MINIMO, MAXIMO)
        // Al volver al 100 % se recentra: quedarse con un desplazamiento
        // residual hace que la pagina aparezca torcida sin motivo aparente.
        if (nueva <= 1.001f) {
            escala = 1f
            desplazamiento = Offset.Zero
            return
        }
        escala = nueva
        desplazamiento = acotar(desplazamiento + arrastre, nueva)
    }

    fun reiniciar() {
        escala = 1f
        desplazamiento = Offset.Zero
    }

    /**
     * Pasa un punto de la pantalla a coordenadas de la pagina sin ampliar.
     *
     * Es la operacion inversa del encuadre: deshace el desplazamiento y la
     * escala, en ese orden.
     */
    fun aPagina(punto: Offset): Offset {
        if (tamano == IntSize.Zero) return punto
        val centro = Offset(tamano.width / 2f, tamano.height / 2f)
        return (punto - centro - desplazamiento) / escala + centro
    }

    /** No deja arrastrar la pagina fuera de la vista. */
    private fun acotar(propuesto: Offset, escalaActual: Float): Offset {
        if (tamano == IntSize.Zero) return propuesto
        val margenX = (tamano.width * (escalaActual - 1f)) / 2f
        val margenY = (tamano.height * (escalaActual - 1f)) / 2f
        return Offset(
            propuesto.x.coerceIn(-margenX, margenX),
            propuesto.y.coerceIn(-margenY, margenY),
        )
    }

    private companion object {
        const val MINIMO = 1f
        const val MAXIMO = 6f
    }
}

@Composable
fun rememberEncuadre(): EstadoEncuadre = remember { EstadoEncuadre() }

/**
 * Pellizco para ampliar y arrastre para moverse.
 *
 * Solo consume el arrastre cuando la pagina esta ampliada: con la pagina
 * entera a la vista, arrastrar debe seguir sirviendo para dibujar o para pasar
 * de pagina, no para mover algo que ya se ve completo.
 */
fun Modifier.encuadre(estado: EstadoEncuadre): Modifier =
    this
        .onSizeChanged { estado.tamano = it }
        .pointerInput(Unit) {
            detectTransformGestures(panZoomLock = true) { _, arrastre, zoom, _ ->
                if (zoom != 1f || estado.ampliada) {
                    estado.aplicar(zoom, arrastre)
                }
            }
        }
