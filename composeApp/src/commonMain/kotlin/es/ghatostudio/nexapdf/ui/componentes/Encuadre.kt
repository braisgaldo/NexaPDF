package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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

    /**
     * Quien ha movido el encuadre por ultimo.
     *
     * Cada cambio pide turno. Una animacion de doble toque se apunta el suyo al
     * empezar y se retira en cuanto otro lo pide. Sin esto, pellizcar mientras
     * la pagina aun se esta acercando era pelearse con ella: cada fotograma de
     * la animacion pisaba lo que acababa de hacer el dedo.
     */
    private var turno = 0

    /** A que escala lleva un doble toque desde la actual: es un interruptor. */
    val escalaTrasDobleToque: Float get() = if (ampliada) 1f else ESCALA_DOBLE_TOQUE

    fun aplicar(cambioEscala: Float, arrastre: Offset) {
        turno++
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
        turno++
        escala = 1f
        desplazamiento = Offset.Zero
    }

    /**
     * Fija la escala sin animar y sin desplazamiento.
     *
     * Es para la lectura continua, donde la escala no transforma la pagina
     * sino que ensancha la columna, y lo que hay que mover para no perder el
     * sitio son las barras de desplazamiento, no este encuadre.
     */
    fun ponerEscala(nueva: Float) {
        turno++
        escala = nueva.coerceIn(MINIMO, MAXIMO)
        desplazamiento = Offset.Zero
    }

    /**
     * Donde acaba el encuadre tras un doble toque en [punto].
     *
     * Con la pagina entera, acerca a [ESCALA_DOBLE_TOQUE] **dejando bajo el
     * dedo lo que estaba bajo el dedo**. Es lo que hace cualquier visor y lo
     * que se espera: quien toca dos veces sobre un parrafo quiere leer ese
     * parrafo. Acercar respecto al centro, que es lo que hace el pellizco de
     * aqui, mandaria fuera de la pantalla justo lo que se ha tocado en cuanto
     * no estuviera en medio.
     *
     * Con la pagina ya ampliada, vuelve a verla entera. No hay un segundo
     * nivel de acercamiento: dos toques para entrar y dos para salir es un
     * gesto que se aprende a la primera, y para afinar mas ya esta el pellizco.
     *
     * Se cumple hasta en las esquinas, y no por casualidad: el tope del
     * arrastre deja justo el margen que necesita una esquina para quedarse
     * donde esta, asi que acotar no aparta nunca lo tocado del dedo.
     */
    fun destinoDobleToque(punto: Offset): PosicionEncuadre {
        if (ampliada) return PosicionEncuadre(1f, Offset.Zero)
        val nueva = ESCALA_DOBLE_TOQUE
        if (tamano == IntSize.Zero) return PosicionEncuadre(nueva, Offset.Zero)

        // Lo que hay bajo el dedo, en coordenadas de la pagina sin ampliar, y el
        // desplazamiento que lo vuelve a poner en el mismo sitio con la escala
        // nueva. Es despejar `punto = centro + (enPagina - centro) * escala + d`.
        val centro = Offset(tamano.width / 2f, tamano.height / 2f)
        val enPagina = aPagina(punto)
        val propuesto = punto - centro - (enPagina - centro) * nueva
        return PosicionEncuadre(nueva, acotar(propuesto, nueva))
    }

    /** Doble toque: acerca o aleja con una animacion corta. */
    suspend fun alternarConDobleToque(punto: Offset) {
        animarHasta(destinoDobleToque(punto))
    }

    /**
     * Lleva el encuadre a [destino] en [DURACION_ANIMACION_MS].
     *
     * Saltar de golpe al 250 % desorienta: no se ve de donde sale lo que queda
     * en pantalla. Animado se entiende que se ha acercado a lo que se toco.
     *
     * Se interpola a mano, fotograma a fotograma, para poder retirarse en
     * cuanto el usuario toma el mando con un pellizco o un arrastre (ver
     * [turno]). Lo interrumpido se queda donde estaba, que es donde el dedo lo
     * ha dejado.
     */
    suspend fun animarHasta(destino: PosicionEncuadre) {
        val mio = ++turno
        val escalaInicial = escala
        val desplazamientoInicial = desplazamiento
        val duracion = DURACION_ANIMACION_MS * 1_000_000f
        var inicio = -1L
        while (true) {
            val progreso = withFrameNanos { ahora ->
                if (inicio < 0) inicio = ahora
                ((ahora - inicio) / duracion).coerceIn(0f, 1f)
            }
            if (turno != mio) return
            if (progreso >= 1f) {
                // El valor exacto y no el interpolado: volver a 1 tiene que
                // dar 1, o `ampliada` podria seguir diciendo que si.
                escala = destino.escala
                desplazamiento = destino.desplazamiento
                return
            }
            val tramo = FastOutSlowInEasing.transform(progreso)
            escala = escalaInicial + (destino.escala - escalaInicial) * tramo
            desplazamiento = desplazamientoInicial +
                (destino.desplazamiento - desplazamientoInicial) * tramo
        }
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
        turno++
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

    companion object {
        /**
         * A cuanto acerca el doble toque.
         *
         * En un A4 que ocupa el ancho de un movil, la letra de cuerpo diez se
         * queda en milimetro y medio; al 250 % pasa a leerse sin forzar la
         * vista y todavia cabe una linea entera de una columna de texto.
         */
        const val ESCALA_DOBLE_TOQUE = 2.5f

        /** Lo bastante corta para no hacer esperar, y lo bastante para seguirla. */
        const val DURACION_ANIMACION_MS = 220

        private const val MINIMO = 1f
        private const val MAXIMO = 6f

        /** Que parte de la vista debe ocupar la zona enfocada. */
        private const val FRACCION_OBJETIVO = 0.5f

        /**
         * Limites del acercamiento automatico.
         *
         * El minimo esta para que saltar a un resultado se note aunque la
         * palabra sea larga; el maximo, para que una coincidencia de dos letras
         * no deje la pantalla llena de un trozo de letra sin contexto alrededor.
         */
        private const val ACERCAMIENTO_MINIMO = 1.6f
        private const val ACERCAMIENTO_MAXIMO = 4f

        /** Suelo para no dividir por cero con una zona degenerada. */
        private const val MINIMO_PIXELES = 1f
    }
}

/** Una escala y un desplazamiento: adonde va el encuadre. */
data class PosicionEncuadre(val escala: Float, val desplazamiento: Offset)

@Composable
fun rememberEncuadre(): EstadoEncuadre = remember { EstadoEncuadre() }

/**
 * Espera dos toques seguidos y llama a [alDobleToque] con el punto del segundo.
 *
 * No se usa `detectTapGestures` por una razon concreta. Ese detector da el
 * toque por perdido en cuanto otro consume un movimiento, y con la pagina
 * ampliada el arrastre de [encuadreConPaso] consume **cualquier** movimiento,
 * tambien el temblor de medio pixel que tiene todo dedo al tocar. El resultado
 * era que acercar funcionaba y alejar fallaba a ratos, justo cuando ya no se
 * ve la pagina entera y mas falta hace.
 *
 * Aqui lo que decide si fue un toque es lo que de verdad lo define: un solo
 * dedo, que no se ha movido mas que la holgura del sistema y que se ha
 * levantado antes de que cuente como pulsacion larga. Quien consuma que es
 * cosa suya. Y no se consume nada salvo el segundo levantamiento: pasar de
 * pagina, arrastrar y pellizcar siguen llegando a quien los atiende.
 *
 * Los dos toques tienen que caer cerca el uno del otro. Dos toques rapidos en
 * puntos distintos de la pantalla son dos toques, no uno doble.
 */
suspend fun PointerInputScope.detectarDobleToque(alDobleToque: suspend (Offset) -> Unit) {
    val separacionMaxima = SEPARACION_ENTRE_TOQUES.toPx()
    coroutineScope {
        awaitEachGesture {
            val primero = awaitFirstDown(requireUnconsumed = false)
            val primerToque = esperarToque(primero) ?: return@awaitEachGesture

            // El mismo margen de tiempo que usa el sistema, con su minimo: dos
            // eventos casi simultaneos son un rebote del panel, no dos toques.
            val segundo = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                val minimo = primerToque.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
                var abajo: PointerInputChange
                do {
                    abajo = awaitFirstDown(requireUnconsumed = false)
                } while (abajo.uptimeMillis < minimo)
                abajo
            } ?: return@awaitEachGesture
            if ((segundo.position - primerToque.position).getDistance() > separacionMaxima) {
                return@awaitEachGesture
            }

            val segundoToque = esperarToque(segundo) ?: return@awaitEachGesture
            segundoToque.consume()
            // Fuera del gesto: la animacion dura varios fotogramas y el
            // detector tiene que quedar libre para el siguiente toque.
            launch { alDobleToque(segundo.position) }
        }
    }
}

/**
 * Sigue al dedo de [abajo] hasta que se levanta, si eso fue un toque.
 *
 * Devuelve nulo si se convierte en otra cosa: un arrastre, un pellizco o una
 * pulsacion larga.
 */
private suspend fun AwaitPointerEventScope.esperarToque(abajo: PointerInputChange): PointerInputChange? {
    val holgura = viewConfiguration.touchSlop
    while (true) {
        val evento = awaitPointerEvent()
        if (evento.changes.count { it.pressed } > 1) return null
        val dedo = evento.changes.firstOrNull { it.id == abajo.id } ?: return null
        if ((dedo.position - abajo.position).getDistance() > holgura) return null
        if (!dedo.pressed) {
            val duracion = dedo.uptimeMillis - abajo.uptimeMillis
            return if (duracion < viewConfiguration.longPressTimeoutMillis) dedo else null
        }
    }
}

/** Distancia maxima entre los dos toques; la misma que usa Android. */
private val SEPARACION_ENTRE_TOQUES = 100.dp

/**
 * Doble toque en la lectura continua, que no se parece al de la lateral.
 *
 * Ahi la escala no transforma la pagina: ensancha la columna, y lo que hay
 * que mover para no perder el sitio son las dos barras de desplazamiento. Esta
 * funcion dice adonde, con la regla de siempre: lo que estaba bajo el dedo
 * sigue bajo el dedo.
 *
 * Se calcula **antes** de medir la columna nueva, a proposito. Esperar a que
 * se mida y corregir despues deja un fotograma con la columna ya ancha y las
 * barras sin mover, y se ve como un parpadeo hacia la esquina. Se puede
 * calcular de antemano porque cada pagina es una caja de proporcion fija que
 * ocupa el ancho de la columna menos sus margenes: su alto nuevo sale de
 * multiplicar el de ahora por lo que crece ese ancho.
 *
 * @param punto donde fue el toque, relativo a la vista y no al contenido.
 * @param anchoVista ancho de la vista sin ampliar.
 * @param margen margen lateral de la columna, a cada lado.
 * @param lateral desplazamiento horizontal actual.
 * @param inicioVista `viewportStartOffset` de la lista: negativo cuando la
 *   lista tiene margen arriba, porque las paginas cuentan desde el.
 * @param paginas las paginas visibles ahora mismo.
 */
fun anclarColumna(
    punto: Offset,
    escalaAntes: Float,
    escalaDespues: Float,
    anchoVista: Float,
    margen: Float,
    lateral: Int,
    inicioVista: Int,
    paginas: List<LazyListItemInfo>,
): AnclajeColumna {
    val anchoPaginaAntes = (anchoVista * escalaAntes - 2 * margen).coerceAtLeast(1f)
    val anchoPaginaDespues = (anchoVista * escalaDespues - 2 * margen).coerceAtLeast(1f)
    val crece = anchoPaginaDespues / anchoPaginaAntes

    val fraccionX = ((lateral + punto.x - margen) / anchoPaginaAntes).coerceIn(0f, 1f)
    val maximoLateral = (anchoVista * escalaDespues - anchoVista).coerceAtLeast(0f)
    val lateralNuevo = (margen + fraccionX * anchoPaginaDespues - punto.x)
        .coerceIn(0f, maximoLateral)
        .roundToInt()

    // En el hueco entre dos paginas no hay ninguna bajo el dedo; vale la mas
    // cercana, que es la que se estaba mirando.
    val y = punto.y + inicioVista
    val pagina = paginas.firstOrNull { y >= it.offset && y < it.offset + it.size }
        ?: paginas.minByOrNull { abs(it.offset + it.size / 2f - y) }
        ?: return AnclajeColumna(lateralNuevo, null, 0)
    val fraccionY = ((y - pagina.offset) / pagina.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    // Cuanto tiene que quedar por encima del borde superior de la lista para
    // que el punto tocado siga a la misma altura. Puede ser negativo: la pagina
    // empieza mas abajo del borde, y la lista lo resuelve subiendo a la de antes.
    val desplazamientoPagina = (fraccionY * pagina.size * crece - y).roundToInt()
    return AnclajeColumna(lateralNuevo, pagina.index, desplazamientoPagina)
}

/** Adonde llevar las dos barras de la lectura continua tras un doble toque. */
data class AnclajeColumna(
    val lateral: Int,
    /** Pagina que hay que llevar arriba, o nulo si no habia ninguna a la vista. */
    val pagina: Int?,
    /** Cuanto de esa pagina queda por encima del borde, como en `scrollToItem`. */
    val desplazamientoPagina: Int,
)

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
 *
 * Y dos toques seguidos acercan al punto tocado, o devuelven la pagina entera
 * si ya estaba ampliada. El pellizco pide las dos manos con el movil en una;
 * el doble toque se hace con el pulgar.
 */
fun Modifier.encuadreConPaso(estado: EstadoEncuadre): Modifier =
    this
        .onSizeChanged { estado.tamano = it }
        .pointerInput(Unit) { detectarDobleToque(estado::alternarConDobleToque) }
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
