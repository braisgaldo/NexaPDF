package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
     * Amplia y centra la vista sobre una zona concreta de la pagina.
     *
     * Existe por la busqueda. Saltar a la pagina donde esta la palabra y dejarla
     * ahi, entera y a tamano de pantalla, no es encontrar nada: en un A4 visto
     * en un movil la palabra resaltada mide dos milimetros y hay que buscarla
     * con el dedo. Lo que uno espera al pulsar un resultado es verlo, y para eso
     * hay que acercarse a el.
     *
     * **La pagina no llena la vista**, y ese detalle es justo el que hay que
     * tener en cuenta. Se dibuja encajada y centrada, asi que entre el borde de
     * la pantalla y el borde del papel queda un margen que depende de lo
     * apaisada que sea cada una. Tomando las coordenadas de la palabra como si
     * fueran de la pantalla, el centrado sale desviado **justo ese margen**: con
     * una pagina casi tan ancha como la vista casi se acierta, y con una
     * estrecha la palabra se va fuera. Por eso aqui se rehace el mismo encaje
     * que hace la capa de resaltados.
     *
     * @param centroRelativo centro de la zona, de 0 a 1 sobre la **pagina**.
     * @param anchoRelativo y [altoRelativo] tamano de la zona en esas mismas
     *   unidades.
     * @param proporcion ancho dividido por alto de la pagina.
     */
    fun enfocar(
        centroRelativo: Offset,
        anchoRelativo: Float,
        altoRelativo: Float,
        proporcion: Float,
    ) {
        if (tamano == IntSize.Zero || proporcion <= 0f) return

        val anchoVista = tamano.width.toFloat()
        val altoVista = tamano.height.toFloat()
        if (anchoVista <= 0f || altoVista <= 0f) return

        // El mismo encaje que `ContentScale.Fit`: la pagina entra entera y sobra
        // sitio por dos lados.
        val anchoPagina: Float
        val altoPagina: Float
        if (anchoVista / altoVista > proporcion) {
            altoPagina = altoVista
            anchoPagina = altoVista * proporcion
        } else {
            anchoPagina = anchoVista
            altoPagina = anchoVista / proporcion
        }
        val margenX = (anchoVista - anchoPagina) / 2f
        val margenY = (altoVista - altoPagina) / 2f

        // El acercamiento se calcula en pixeles de pantalla y no en fracciones
        // de pagina, que es otra cosa que el margen desvirtuaba: una zona que
        // ocupa el 10 % de una pagina estrecha no ocupa el 10 % de la vista.
        val anchoZona = (anchoRelativo * anchoPagina).coerceAtLeast(MINIMO_PIXELES)
        val altoZona = (altoRelativo * altoPagina).coerceAtLeast(MINIMO_PIXELES)
        val porAncho = FRACCION_OBJETIVO * anchoVista / anchoZona
        val porAlto = FRACCION_OBJETIVO * altoVista / altoZona
        escala = minOf(porAncho, porAlto).coerceIn(ACERCAMIENTO_MINIMO, ACERCAMIENTO_MAXIMO)

        // El encuadre escala respecto al centro de la vista, asi que para traer
        // un punto al centro hay que desplazarlo justo lo contrario de lo que la
        // escala lo aleja.
        val centro = Offset(anchoVista / 2f, altoVista / 2f)
        val punto = Offset(
            margenX + centroRelativo.x * anchoPagina,
            margenY + centroRelativo.y * altoPagina,
        )
        desplazamiento = acotarAlPapel(
            propuesto = -(punto - centro) * escala,
            escalaActual = escala,
            margenX = margenX,
            margenY = margenY,
            anchoPagina = anchoPagina,
            altoPagina = altoPagina,
        )
    }

    /**
     * Tope de desplazamiento medido contra el papel, no contra la pantalla.
     *
     * [acotar] impide sacar de la vista algo del tamano de la vista, y para
     * arrastrar con el dedo esta bien. Para el enfoque no vale, y se veia: una
     * palabra del margen izquierdo de la hoja necesita un desplazamiento mayor
     * del que ese tope permite, asi que se quedaba a un tercio de pantalla del
     * centro. Y como los margenes son justo donde empieza y acaba **cada linea**
     * de texto, le pasaba a muchisimas palabras.
     *
     * El limite de aqui es otro: el borde del papel no puede pasar del centro de
     * la vista. Con esa regla cualquier punto que este dentro de la hoja se
     * puede centrar exactamente —el caso extremo es centrar el propio borde— y
     * se sigue sin poder empujar la pagina hasta perderla de vista.
     */
    private fun acotarAlPapel(
        propuesto: Offset,
        escalaActual: Float,
        margenX: Float,
        margenY: Float,
        anchoPagina: Float,
        altoPagina: Float,
    ): Offset {
        val centroX = tamano.width / 2f
        val centroY = tamano.height / 2f
        val maximoX = (centroX - margenX) * escalaActual
        val minimoX = -(margenX + anchoPagina - centroX) * escalaActual
        val maximoY = (centroY - margenY) * escalaActual
        val minimoY = -(margenY + altoPagina - centroY) * escalaActual
        return Offset(
            propuesto.x.coerceIn(minOf(minimoX, maximoX), maxOf(minimoX, maximoX)),
            propuesto.y.coerceIn(minOf(minimoY, maximoY), maxOf(minimoY, maximoY)),
        )
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

        /** Que parte de la vista debe ocupar la zona enfocada. */
        const val FRACCION_OBJETIVO = 0.5f

        /**
         * Limites del acercamiento automatico.
         *
         * El minimo esta para que saltar a un resultado se note aunque la
         * palabra sea larga; el maximo, para que una coincidencia de dos letras
         * no deje la pantalla llena de un trozo de letra sin contexto alrededor.
         */
        const val ACERCAMIENTO_MINIMO = 1.6f
        const val ACERCAMIENTO_MAXIMO = 4f

        /** Suelo para no dividir por cero con una zona degenerada. */
        const val MINIMO_PIXELES = 1f
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

/**
 * Encuadre que convive con el paso de pagina.
 *
 * [encuadre] usa `detectTransformGestures`, que consume el arrastre en cuanto
 * pasa el umbral de toque aunque no llegue a aplicar nada. Con la pagina en un
 * pager eso se traducia en que deslizar de lado no pasaba de hoja: el gesto se
 * lo quedaba el zoom y no llegaba a nadie.
 *
 * Aqui el reparto es explicito. Dos dedos siempre son ampliar y mover. Un dedo
 * solo mueve la pagina cuando ya esta ampliada, que es cuando hay algo fuera de
 * la vista que ver; si no lo esta, el evento se deja pasar y lo recoge quien
 * corresponda para pasar de pagina.
 */
fun Modifier.encuadreConPaso(estado: EstadoEncuadre): Modifier =
    this
        .onSizeChanged { estado.tamano = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val evento = awaitPointerEvent()
                    val dedos = evento.changes.count { it.pressed }
                    val zoom = evento.calculateZoom()
                    val arrastre = evento.calculatePan()
                    val manda = dedos >= 2 || estado.ampliada
                    if (manda && (zoom != 1f || arrastre != Offset.Zero)) {
                        estado.aplicar(zoom, arrastre)
                        evento.changes.forEach { it.consume() }
                    }
                } while (evento.changes.any { it.pressed })
            }
        }

/**
 * Igual que [encuadre], pero solo con dos dedos.
 *
 * En el editor un dedo esta dibujando. Si el gesto de mover la pagina
 * respondiera tambien a un dedo, cada trazo movaria el documento en lugar de
 * pintar; con dos, ampliar y desplazarse conviven con el pincel sin estorbarse.
 */
fun Modifier.encuadreDosDedos(estado: EstadoEncuadre): Modifier =
    this
        .onSizeChanged { estado.tamano = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val evento = awaitPointerEvent()
                    val dedos = evento.changes.count { it.pressed }
                    if (dedos >= 2) {
                        val zoom = evento.calculateZoom()
                        val arrastre = evento.calculatePan()
                        if (zoom != 1f || arrastre != Offset.Zero) {
                            estado.aplicar(zoom, arrastre)
                            evento.changes.forEach { it.consume() }
                        }
                    }
                } while (evento.changes.any { it.pressed })
            }
        }
