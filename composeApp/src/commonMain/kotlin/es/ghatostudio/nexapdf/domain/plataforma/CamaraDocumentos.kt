package es.ghatostudio.nexapdf.domain.plataforma

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.ghatostudio.nexapdf.domain.escaner.DeteccionDocumento

/**
 * La camara en vivo dentro de la aplicacion.
 *
 * Es la unica parte de NexaPDF que necesita un permiso del sistema, y esta
 * detras de una interfaz por el mismo motivo que todo lo demas: `commonMain` no
 * sabe que existe CameraX. En escritorio esta dependencia es `null` y el
 * escaner ofrece solo la galeria.
 *
 * **Por que camara propia y no la del telefono.** Hasta aqui NexaPDF delegaba
 * toda captura en la app de camara del sistema, y asi no declaraba ningun
 * permiso. Eso sigue valiendo para "Imagenes a PDF". Pero un escaner de
 * documentos necesita ver los fotogramas *antes* de que se tome la foto: es lo
 * que permite ensenar el contorno del papel y el porcentaje de deteccion
 * mientras se apunta, y disparar solo cuando el encuadre esta bien. La camara
 * del sistema devuelve una foto ya hecha y no deja mirar por el visor. El
 * porque y lo que cuesta estan en docs/adr/0005-camara-escaner.md.
 */
interface CamaraDocumentos {

    /** El dispositivo tiene camara trasera utilizable. */
    val disponible: Boolean

    /** El permiso de camara esta concedido ahora mismo. */
    fun hayPermiso(): Boolean

    /**
     * Pide el permiso y espera la respuesta.
     *
     * Devuelve si quedo concedido. No se pide al arrancar la aplicacion sino al
     * entrar en el escaner: un permiso que se pide cuando se entiende para que
     * es se concede, y uno que salta nada mas abrir la app se deniega.
     */
    suspend fun pedirPermiso(): Boolean

    /**
     * El visor con la deteccion en marcha.
     *
     * @param linterna enciende el flash en modo antorcha.
     * @param fotosPorDisparo cuantas fotos seguidas hace cada disparo. Una
     *   desactiva la rafaga; varias se funden despues en una imagen con menos
     *   grano.
     * @param analizando cuando es `false` se deja de analizar (por ejemplo
     *   mientras se guarda una foto), pero la vista previa sigue viva: apagarla
     *   entera provocaria un parpadeo negro entre hoja y hoja.
     * @param alMedirProporcion ancho dividido por alto de lo que la camara
     *   ensena. La pantalla encuadra el visor con esa proporcion exacta para que
     *   el contorno detectado, que viene en coordenadas de 0 a 1 del fotograma,
     *   caiga justo encima del papel que se ve. Sin esto, la vista previa
     *   recortaria por los lados y el contorno saldria desplazado.
     * @param alDetectar se llama en cada fotograma analizado, en el hilo
     *   principal, con el papel ya suavizado entre fotogramas.
     * @param alEstarLista entrega el disparador en cuanto la camara arranca, y
     *   `null` si deja de estar disponible.
     */
    @Composable
    fun Visor(
        modifier: Modifier,
        linterna: Boolean,
        analizando: Boolean,
        fotosPorDisparo: Int,
        alMedirProporcion: (Float) -> Unit,
        alDetectar: (DeteccionDocumento) -> Unit,
        alEstarLista: (Disparador?) -> Unit,
    )
}

/**
 * Hace la foto y devuelve las rutas de los ficheros.
 *
 * Devuelve una **lista** y no una ruta porque el escaner dispara una rafaga
 * corta: varias fotos seguidas de la misma hoja, que despues se funden en una
 * mas limpia. La primera es la de referencia y la que manda en el encuadre; las
 * demas solo aportan para quitar grano. Con la rafaga desactivada la lista trae
 * una sola foto, y todo lo de detras funciona igual.
 *
 * Vacia si no se pudo hacer ninguna.
 */
fun interface Disparador {
    suspend fun disparar(): List<String>
}
