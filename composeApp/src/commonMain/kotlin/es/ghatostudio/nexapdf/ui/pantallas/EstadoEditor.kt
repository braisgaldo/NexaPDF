package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import es.ghatostudio.nexapdf.domain.model.Edicion
import es.ghatostudio.nexapdf.domain.model.EdicionPagina
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import es.ghatostudio.nexapdf.domain.model.TipoFigura

/** Herramienta activa del editor. */
enum class HerramientaEditor {
    MOVER,
    DIBUJAR,
    RESALTAR,
    BORRAR,
    FIGURA,
    TEXTO,
    IMAGEN,
    FIRMA,
    FILTRO,
}

/**
 * Estado de edicion de una pagina.
 *
 * Las ediciones se guardan como una lista de datos y no se aplican al PDF hasta
 * que el usuario pulsa guardar. Eso hace que dibujar sea instantaneo por pesado
 * que sea el documento, y que deshacer sea quitar el ultimo elemento de una
 * lista en lugar de rehacer el fichero.
 */
class EstadoEditor(indicePagina: Int) {

    var herramienta by mutableStateOf(HerramientaEditor.DIBUJAR)
    var color by mutableStateOf(COLORES.first())
    var grosor by mutableStateOf(0.006f)
    var opacidad by mutableStateOf(1f)
    var tipoFigura by mutableStateOf(TipoFigura.RECTANGULO)
    var conRelleno by mutableStateOf(false)
    var filtro by mutableStateOf(FiltroPagina.NINGUNO)
    var intensidadFiltro by mutableStateOf(0.5f)

    val indice = indicePagina

    private val ediciones = mutableStateListOf<Edicion>()
    private val deshechas = mutableStateListOf<Edicion>()

    /** Trazo que se esta dibujando ahora mismo, aun sin soltar el dedo. */
    var trazoEnCurso by mutableStateOf<List<Punto>>(emptyList())
        private set

    /** Figura que se esta arrastrando ahora mismo. */
    var figuraEnCurso by mutableStateOf<Pair<Punto, Punto>?>(null)
        private set

    /** Id del objeto seleccionado, o `null` si no hay ninguno. */
    var seleccionada by mutableStateOf<String?>(null)
        private set

    val listaEdiciones: List<Edicion> get() = ediciones

    /** El objeto seleccionado, si sigue existiendo. */
    val objetoSeleccionado: Edicion.Colocada?
        get() = seleccionada?.let { id ->
            ediciones.firstOrNull { it.id == id } as? Edicion.Colocada
        }

    /**
     * Selecciona el objeto que hay bajo el punto, o deselecciona si no hay
     * ninguno. Se recorre de arriba abajo porque lo ultimo colocado es lo que
     * se ve encima y lo que el usuario cree estar tocando.
     */
    fun seleccionarEn(punto: Punto): Boolean {
        val encontrado = ediciones
            .asReversed()
            .filterIsInstance<Edicion.Colocada>()
            .firstOrNull { it.marco.contieneConMargen(punto, MARGEN_SELECCION) }
        seleccionada = encontrado?.id
        return encontrado != null
    }

    fun deseleccionar() {
        seleccionada = null
    }

    /** Desplaza el objeto seleccionado, sin dejar que se salga de la pagina. */
    fun moverSeleccion(dx: Float, dy: Float) {
        transformarSeleccion { objeto ->
            val m = objeto.marco.normalizado()
            val despX = dx.coerceIn(-m.izquierda, 1f - m.derecha)
            val despY = dy.coerceIn(-m.arriba, 1f - m.abajo)
            conMarco(
                objeto,
                Rectangulo(
                    izquierda = m.izquierda + despX,
                    arriba = m.arriba + despY,
                    derecha = m.derecha + despX,
                    abajo = m.abajo + despY,
                ),
            )
        }
    }

    /**
     * Escala el objeto desde su centro.
     *
     * Desde el centro y no desde una esquina porque con el dedo encima de la
     * esquina no se ve lo que se esta haciendo; creciendo hacia los dos lados
     * el objeto no se escapa de debajo del dedo.
     */
    fun escalarSeleccion(factor: Float) {
        transformarSeleccion { objeto ->
            val m = objeto.marco.normalizado()
            val cx = (m.izquierda + m.derecha) / 2f
            val cy = (m.arriba + m.abajo) / 2f
            val semiAncho = ((m.derecha - m.izquierda) / 2f * factor).coerceIn(0.01f, 0.5f)
            val semiAlto = ((m.abajo - m.arriba) / 2f * factor).coerceIn(0.005f, 0.5f)
            val nuevo = Rectangulo(cx - semiAncho, cy - semiAlto, cx + semiAncho, cy + semiAlto)
            when (objeto) {
                // El texto escala tambien su cuerpo de letra: un marco mas
                // grande con la misma fuente solo anade aire alrededor.
                is Edicion.Texto -> objeto.copy(
                    marco = nuevo,
                    tamano = (objeto.tamano * factor).coerceIn(0.008f, 0.25f),
                )

                else -> conMarco(objeto, nuevo)
            }
        }
    }

    fun rotarSeleccion(grados: Float) {
        transformarSeleccion { objeto ->
            conRotacion(objeto, (objeto.rotacion + grados).mod(360f))
        }
    }

    /** Quita el objeto seleccionado. */
    fun borrarSeleccion() {
        val id = seleccionada ?: return
        val indiceObjeto = ediciones.indexOfFirst { it.id == id }
        if (indiceObjeto >= 0) deshechas.add(ediciones.removeAt(indiceObjeto))
        seleccionada = null
    }

    private inline fun transformarSeleccion(cambio: (Edicion.Colocada) -> Edicion) {
        val id = seleccionada ?: return
        val indiceObjeto = ediciones.indexOfFirst { it.id == id }
        if (indiceObjeto < 0) return
        val objeto = ediciones[indiceObjeto] as? Edicion.Colocada ?: return
        ediciones[indiceObjeto] = cambio(objeto)
    }

    private fun conMarco(objeto: Edicion.Colocada, marco: Rectangulo): Edicion = when (objeto) {
        is Edicion.Texto -> objeto.copy(marco = marco)
        is Edicion.Imagen -> objeto.copy(marco = marco)
        is Edicion.Firma -> objeto.copy(marco = marco)
    }

    private fun conRotacion(objeto: Edicion.Colocada, rotacion: Float): Edicion = when (objeto) {
        is Edicion.Texto -> objeto.copy(rotacion = rotacion)
        is Edicion.Imagen -> objeto.copy(rotacion = rotacion)
        is Edicion.Firma -> objeto.copy(rotacion = rotacion)
    }
    val puedeDeshacer: Boolean get() = ediciones.isNotEmpty()
    val puedeRehacer: Boolean get() = deshechas.isNotEmpty()
    val hayCambios: Boolean get() = ediciones.isNotEmpty() || filtro != FiltroPagina.NINGUNO

    private var contador = 0

    private fun siguienteId(): String = "ed-${indice}-${contador++}"

    // --- Trazos --------------------------------------------------------------

    fun empezarTrazo(punto: Punto) {
        trazoEnCurso = listOf(punto)
    }

    fun continuarTrazo(punto: Punto) {
        trazoEnCurso = trazoEnCurso + punto
    }

    fun terminarTrazo() {
        val puntos = trazoEnCurso
        trazoEnCurso = emptyList()
        if (puntos.isEmpty()) return

        val resaltador = herramienta == HerramientaEditor.RESALTAR
        anadir(
            Edicion.Trazo(
                id = siguienteId(),
                puntos = puntos,
                colorArgb = color,
                // El marcador se pinta mucho mas grueso y translucido: si no,
                // no parece un marcador sino un boligrafo de color.
                grosor = if (resaltador) grosor * 4f else grosor,
                opacidad = if (resaltador) 0.35f else opacidad,
                resaltador = resaltador,
            ),
        )
    }

    // --- Figuras -------------------------------------------------------------

    fun empezarFigura(punto: Punto) {
        figuraEnCurso = punto to punto
    }

    fun continuarFigura(punto: Punto) {
        figuraEnCurso = figuraEnCurso?.copy(second = punto)
    }

    fun terminarFigura() {
        val (inicio, fin) = figuraEnCurso ?: return
        figuraEnCurso = null

        val marco = if (tipoFigura == TipoFigura.LINEA || tipoFigura == TipoFigura.FLECHA) {
            // Una linea necesita conservar la direccion del arrastre; si se
            // normalizara, todas las flechas apuntarian al mismo sitio.
            Rectangulo(inicio.x, inicio.y, fin.x, fin.y)
        } else {
            Rectangulo.desdeEsquinas(inicio, fin)
        }
        if (marco.ancho < 0.005f && marco.alto < 0.005f) return

        anadir(
            Edicion.Figura(
                id = siguienteId(),
                tipo = tipoFigura,
                marco = marco,
                colorTrazoArgb = color,
                colorRellenoArgb = if (conRelleno) color else null,
                grosor = grosor,
                opacidad = opacidad,
            ),
        )
    }

    // --- Otras ediciones -----------------------------------------------------

    fun anadirTexto(
        contenido: String,
        marco: Rectangulo,
        tamano: Float,
        taparDebajo: Boolean,
        fondoArgb: Long? = null,
    ) {
        if (contenido.isBlank()) return
        if (taparDebajo) {
            anadir(
                Edicion.Tapado(
                    id = siguienteId(),
                    // Se tapa un poco mas de lo que ocupa la linea, porque los
                    // acentos y las colas de las letras sobresalen del marco.
                    marco = marco.copy(
                        arriba = (marco.arriba - 0.004f).coerceAtLeast(0f),
                        abajo = (marco.abajo + 0.004f).coerceAtMost(1f),
                    ),
                    colorArgb = BLANCO,
                ),
            )
        }
        anadir(
            Edicion.Texto(
                id = siguienteId(),
                contenido = contenido,
                marco = marco,
                colorArgb = color,
                tamano = tamano,
                fondoArgb = fondoArgb,
            ),
        )
    }

    fun anadirImagen(ruta: String, marco: Rectangulo) {
        val nueva = Edicion.Imagen(id = siguienteId(), rutaImagen = ruta, marco = marco)
        anadir(nueva)
        seleccionada = nueva.id
    }

    fun anadirFirma(trazos: List<List<Punto>>, marco: Rectangulo) {
        if (trazos.isEmpty()) return
        anadir(
            Edicion.Firma(
                id = siguienteId(),
                trazos = trazos,
                marco = marco,
                colorArgb = NEGRO,
                grosor = 0.004f,
            ),
        )
    }

    /** Quita la ultima edicion que toque el punto: es el borrador. */
    fun borrarEn(punto: Punto) {
        val indiceTocado = ediciones.indexOfLast { edicion ->
            when (edicion) {
                is Edicion.Trazo -> edicion.puntos.any { cerca(it, punto, edicion.grosor + 0.02f) }
                is Edicion.Figura -> edicion.marco.normalizado().contiene(punto)
                is Edicion.Texto -> edicion.marco.normalizado().contiene(punto)
                is Edicion.Imagen -> edicion.marco.normalizado().contiene(punto)
                is Edicion.Tapado -> edicion.marco.normalizado().contiene(punto)
                is Edicion.Firma -> edicion.marco.normalizado().contiene(punto)
            }
        }
        if (indiceTocado >= 0) {
            deshechas.add(ediciones.removeAt(indiceTocado))
        }
    }

    private fun cerca(a: Punto, b: Punto, margen: Float): Boolean =
        kotlin.math.abs(a.x - b.x) < margen && kotlin.math.abs(a.y - b.y) < margen

    private fun anadir(edicion: Edicion) {
        ediciones.add(edicion)
        // Una accion nueva invalida el camino de rehacer, como en cualquier
        // editor: rehacer sobre una historia distinta no significaria nada.
        deshechas.clear()
    }

    fun deshacer() {
        if (ediciones.isEmpty()) return
        deshechas.add(ediciones.removeAt(ediciones.lastIndex))
    }

    fun rehacer() {
        if (deshechas.isEmpty()) return
        ediciones.add(deshechas.removeAt(deshechas.lastIndex))
    }

    fun aEdicionPagina(): EdicionPagina = EdicionPagina(
        indice = indice,
        ediciones = ediciones.toList(),
        filtro = filtro,
        intensidadFiltro = intensidadFiltro,
    )

    companion object {
        const val NEGRO = 0xFF1A1A1AL
        const val BLANCO = 0xFFFFFFFFL

        /**
         * Paleta del editor.
         *
         * Son colores de contenido, no de interfaz: van a quedar dentro del PDF
         * y deben verse igual en cualquier lector, asi que no salen de los
         * tokens del tema. Se eligen contrastados sobre papel blanco.
         */
        val COLORES = listOf(
            NEGRO,
            0xFFD32F2FL, // rojo
            0xFF1976D2L, // azul
            0xFF388E3CL, // verde
            0xFFF9A825L, // amarillo
            0xFF7B1FA2L, // morado
            0xFFEF6C00L, // naranja
            BLANCO,
        )
    }
}

/**
 * Holgura al tocar para seleccionar un objeto, en fraccion de pagina.
 *
 * Mas generosa que la de las lineas de texto del documento: aqui el objeto lo
 * ha colocado el propio usuario y espera poder cogerlo sin apuntar.
 */
private const val MARGEN_SELECCION = 0.02f
