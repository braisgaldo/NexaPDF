package es.ghatostudio.nexapdf.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ghatostudio.nexapdf.di.ContenedorApp
import es.ghatostudio.nexapdf.domain.model.Ajustes
import es.ghatostudio.nexapdf.domain.model.ModoGuardado
import es.ghatostudio.nexapdf.ui.i18n.Idioma
import es.ghatostudio.nexapdf.ui.navegacion.Destino
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import es.ghatostudio.nexapdf.domain.model.TareaConResultado
import kotlinx.coroutines.Job

/**
 * Estado que vive mientras vive la aplicacion: ajustes, pila de navegacion,
 * avisos y la logica de cuando ofrecer la donacion.
 *
 * Lo que es propio de un documento concreto (paginas cargadas, borrador de
 * edicion) no esta aqui: eso se crea y se destruye con su pantalla.
 */
class EstadoApp(private val contenedor: ContenedorApp) : ViewModel() {

    val ajustes: StateFlow<Ajustes> = contenedor.ajustes.ajustes
        .stateIn(viewModelScope, SharingStarted.Eagerly, Ajustes())

    // --- Navegacion ----------------------------------------------------------

    private val pila = mutableStateListOf<Destino>(Destino.Inicio)

    val destinoActual: Destino get() = pila.last()
    val puedeVolver: Boolean get() = pila.size > 1

    fun ir(destino: Destino) {
        pila.add(destino)
    }

    /** Sustituye el destino actual, para no acumular pasos intermedios. */
    fun reemplazar(destino: Destino) {
        if (pila.size > 1) pila.removeAt(pila.lastIndex)
        pila.add(destino)
    }

    fun volver(): Boolean {
        if (pila.size <= 1) return false
        pila.removeAt(pila.lastIndex)
        return true
    }

    fun volverAInicio() {
        while (pila.size > 1) pila.removeAt(pila.lastIndex)
    }

    // --- Avisos --------------------------------------------------------------

    /** Mensaje pendiente de mostrar en el snackbar, con accion opcional. */
    data class Aviso(
        val texto: String,
        val accion: String? = null,
        val alPulsar: (() -> Unit)? = null,
    )

    var aviso by mutableStateOf<Aviso?>(null)
        private set

    fun avisar(texto: String, accion: String? = null, alPulsar: (() -> Unit)? = null) {
        aviso = Aviso(texto, accion, alPulsar)
    }

    fun avisoMostrado() {
        aviso = null
    }

    // --- Trabajo en curso ----------------------------------------------------

    /** Texto de la operacion en marcha, o `null` si no hay ninguna. */
    var trabajando by mutableStateOf<String?>(null)
        private set

    /** Cuantas partes de cuantas van hechas, si la tarea lo sabe. */
    var progreso by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    private var trabajoEnCurso: Job? = null

    /** Si la tarea en curso se puede cortar por la mitad. */
    val sePuedeCancelar: Boolean get() = trabajoEnCurso != null

    fun empezarTrabajo(descripcion: String, trabajo: Job? = null) {
        trabajando = descripcion
        progreso = null
        trabajoEnCurso = trabajo
    }

    /**
     * Apunta por donde va la tarea.
     *
     * Un velo con un texto fijo no distingue entre "tarda" y "se ha
     * colgado", y en un documento de ciento veinte paginas la diferencia
     * importa.
     */
    fun fijarProgreso(hechas: Int, total: Int) {
        progreso = if (total > 1) hechas to total else null
    }

    fun cancelarTrabajo() {
        trabajoEnCurso?.cancel()
        terminarTrabajo()
    }

    fun terminarTrabajo() {
        trabajando = null
        progreso = null
        trabajoEnCurso = null
    }

    // --- Donacion ------------------------------------------------------------

    var mostrandoDonacion by mutableStateOf(false)
        private set

    /**
     * El aviso esta decidido pero aun no se ensena.
     *
     * No se puede mostrar nada mientras la app esta en segundo plano, asi que
     * al cerrar la sesion solo se anota que toca, y se ensena cuando el usuario
     * vuelve y esta en la pantalla de inicio. Esperar a inicio es lo que
     * garantiza que no aparezca encima de una tarea a medias.
     */
    var donacionPendiente by mutableStateOf(false)
        private set

    /**
     * Ensena el aviso si sigue tocando.
     *
     * Lo llama la pantalla despues de comprobar que la app lleva un rato quieta
     * en Inicio. Antes se ensenaba directamente al volver a primer plano, y eso
     * lo hacia saltar al regresar del selector de ficheros del sistema: la app
     * pasa por Inicio un instante, con el documento ya elegido y a punto de
     * abrirse, y el aviso caia justo encima de una tarea empezada.
     */
    fun mostrarDonacionSiProcede() {
        if (donacionPendiente && destinoActual == Destino.Inicio) {
            donacionPendiente = false
            mostrandoDonacion = true
        }
    }

    /**
     * Registra que el usuario ha producido un documento.
     *
     * Es la definicion de "uso real" del punto 4.4.3: unir, separar, crear
     * desde imagenes, guardar una edicion o firmar. Abrir un PDF y cerrarlo no
     * cuenta.
     */
    fun registrarUsoReal() {
        viewModelScope.launch { contenedor.ajustes.registrarUsoReal() }
    }

    /**
     * Se llama cuando la app pasa a segundo plano.
     *
     * Es el unico momento en que puede aparecer el aviso de donacion: nunca al
     * arrancar y nunca encima de una tarea a medias.
     */
    fun alCerrarSesion() {
        viewModelScope.launch {
            val actuales = contenedor.ajustes.actual()
            if (actuales.tocaMostrarDonacion(contenedor.servicios.ahora()) &&
                contenedor.servicios.donacionesDisponibles
            ) {
                donacionPendiente = true
            }
        }
    }

    /** Entrada manual desde Ajustes: siempre disponible, sin condiciones. */
    fun abrirDonacion() {
        mostrandoDonacion = true
    }

    fun cerrarDonacion() {
        mostrandoDonacion = false
    }

    fun aplazarDonacion() {
        mostrandoDonacion = false
        viewModelScope.launch {
            contenedor.ajustes.aplazarDonacion(contenedor.servicios.ahora())
        }
    }

    fun silenciarDonacion() {
        mostrandoDonacion = false
        viewModelScope.launch { contenedor.ajustes.silenciarDonacion() }
    }

    // --- Preferencias --------------------------------------------------------

    fun fijarFamiliaTema(clave: String) =
        viewModelScope.launch { contenedor.ajustes.fijarFamiliaTema(clave) }

    fun fijarModoTema(clave: String) =
        viewModelScope.launch { contenedor.ajustes.fijarModoTema(clave) }

    fun fijarIdioma(idioma: Idioma?) {
        viewModelScope.launch {
            contenedor.ajustes.fijarIdioma(idioma?.etiqueta)
            contenedor.servicios.aplicarIdioma(idioma?.etiqueta)
        }
    }

    fun fijarCalidadVista(clave: String) =
        viewModelScope.launch { contenedor.ajustes.fijarCalidadVista(clave) }

    fun fijarConfirmarDestructivas(valor: Boolean) =
        viewModelScope.launch { contenedor.ajustes.fijarConfirmarDestructivas(valor) }

    fun fijarGuardarEnDescargas(valor: Boolean) =
        viewModelScope.launch { contenedor.ajustes.fijarGuardarEnDescargas(valor) }

    fun fijarModoGuardado(modo: ModoGuardado) =
        viewModelScope.launch { contenedor.ajustes.fijarModoGuardado(modo.name) }

    fun fijarPreguntarCompartir(valor: Boolean) =
        viewModelScope.launch { contenedor.ajustes.fijarPreguntarCompartir(valor) }

    fun fijarResumenAlSeparar(valor: Boolean) =
        viewModelScope.launch { contenedor.ajustes.fijarResumenAlSeparar(valor) }

    fun fijarPedirFirmaManuscrita(valor: Boolean) =
        viewModelScope.launch { contenedor.ajustes.fijarPedirFirmaManuscrita(valor) }

    fun fijarDireccionLectura(clave: String) =
        viewModelScope.launch { contenedor.ajustes.fijarDireccionLectura(clave) }

    fun fijarApertura(tarea: TareaConResultado, clave: String) =
        viewModelScope.launch { contenedor.ajustes.fijarApertura(tarea, clave) }

    fun fijarCarpetaDestino(uri: String?) =
        viewModelScope.launch { contenedor.ajustes.fijarCarpetaDestino(uri) }

    fun fijarNombreParaFirmas(nombre: String) =
        viewModelScope.launch { contenedor.ajustes.fijarNombreParaFirmas(nombre) }
}
