package es.ghatostudio.nexapdf.domain.model

import es.ghatostudio.nexapdf.ui.theme.ThemeFamily
import es.ghatostudio.nexapdf.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

/** Calidad con la que se rasterizan las paginas en pantalla. */
enum class CalidadVista(val escala: Float) {
    /** Rapida: util en moviles modestos o documentos de muchas paginas. */
    RAPIDA(1.0f),
    EQUILIBRADA(1.5f),
    NITIDA(2.0f),
}

/**
 * Estado del aviso de donacion.
 *
 * El recorrido es de una sola direccion: nunca mostrada -> aplazada -> callada.
 * No hay forma de volver atras salvo reinstalando, y la preferencia viaja en la
 * copia de seguridad justamente para que reinstalar tampoco la reinicie.
 */
enum class EstadoDonacion {
    SIN_MOSTRAR,
    APLAZADA,
    SILENCIADA,
}

@Serializable
data class Ajustes(
    val familiaTema: String = ThemeFamily.INDIGO.name,
    val modoTema: String = ThemeMode.SISTEMA.name,
    /** Etiqueta BCP-47, o `null` para seguir el idioma del sistema. */
    val idioma: String? = null,
    val calidadVista: String = CalidadVista.EQUILIBRADA.name,
    val confirmarAccionesDestructivas: Boolean = true,
    val guardarEnDescargasAlTerminar: Boolean = true,

    /**
     * Cuando sacar los resultados de la carpeta privada de la aplicacion.
     *
     * Una tarea encadena varios ficheros: unes, reordenas, editas, firmas. Con
     * PASO_A_PASO queda cada resultado intermedio, que es util para volver
     * atras y ver como iba; con SOLO_AL_FINAL solo sale lo que se guarda o se
     * comparte a proposito, y la carpeta no se llena de versiones.
     */
    val modoGuardado: String = ModoGuardado.PASO_A_PASO.name,

    /** Como se pasa de pagina en el visor. Ver [DireccionLectura]. */
    val direccionLectura: String = DireccionLectura.LATERAL.name,

    /**
     * Carpeta elegida por el usuario, como URI de arbol del sistema.
     *
     * `null` significa la carpeta de la aplicacion (Descargas/NexaPDF), que es
     * donde va todo si no se toca nada.
     */
    val carpetaDestino: String? = null,

    /** Preguntar si se quiere compartir cada documento recien creado. */
    val preguntarCompartir: Boolean = false,

    /**
     * Ensenar el resumen antes de crear los ficheros al dividir en partes.
     *
     * Viene puesto porque dividir es la operacion que mas ficheros produce
     * de una vez y la que mas cuesta deshacer: mirar la lista antes sale
     * mas barato que borrar doce ficheros mal cortados. Quien parte
     * documentos a diario lo puede quitar.
     */
    val resumenAlSepararEnPartes: Boolean = true,

    /**
     * Ofrecer dibujar la rubrica antes de firmar con certificado.
     *
     * Quien firma con certificado a diario no dibuja nunca su rubrica, y
     * el paso se convierte en un toque de mas cada vez. Viene puesto porque
     * la firma a mano es lo que la mayoria entiende por firmar.
     */
    val pedirFirmaManuscrita: Boolean = true,

    /**
     * Que hacer con lo recien creado, tarea por tarea.
     *
     * Una sola opcion para todas no vale: al editar una pagina se quiere
     * ver como ha quedado, y al convertir veinte ficheros a PDF lo ultimo
     * que se quiere es que se abra cada uno. Se guarda un valor por tarea
     * y no un mapa porque son cinco fijas y las claves sueltas son mas
     * faciles de leer en el fichero de preferencias.
     */
    val aperturaEditar: String = AperturaAlTerminar.ABRIR.name,
    val aperturaUnir: String = AperturaAlTerminar.ABRIR.name,
    val aperturaFirmar: String = AperturaAlTerminar.ABRIR.name,
    val aperturaConvertir: String = AperturaAlTerminar.ABRIR.name,
    val aperturaImagenes: String = AperturaAlTerminar.ABRIR.name,

    // --- Aviso de donacion ---
    val estadoDonacion: String = EstadoDonacion.SIN_MOSTRAR.name,
    /** Sesiones en las que el usuario ha completado alguna operacion real. */
    val usosReales: Int = 0,
    val usosAlAplazar: Int = 0,
    val aplazadaEnEpochMillis: Long = 0L,
    /** La sesion actual ya ha producido un documento. */
    val sesionConUsoReal: Boolean = false,

    /** El tour guiado ya se vio; no se vuelve a ensenar solo. */
    val tourVisto: Boolean = false,

    // --- Datos del usuario ---
    val firmasGuardadas: List<FirmaGuardada> = emptyList(),
    val nombreParaFirmas: String = "",
) {
    val familia: ThemeFamily get() = ThemeFamily.desdeClave(familiaTema)
    val modo: ThemeMode get() = ThemeMode.desdeClave(modoTema)
    /** Que hacer al terminar [tarea]. */
    fun apertura(tarea: TareaConResultado): AperturaAlTerminar {
        val clave = when (tarea) {
            TareaConResultado.EDITAR -> aperturaEditar
            TareaConResultado.UNIR -> aperturaUnir
            TareaConResultado.FIRMAR -> aperturaFirmar
            TareaConResultado.CONVERTIR -> aperturaConvertir
            TareaConResultado.IMAGENES -> aperturaImagenes
        }
        return AperturaAlTerminar.entries.firstOrNull { it.name == clave }
            ?: AperturaAlTerminar.ABRIR
    }

    val lectura: DireccionLectura
        get() = DireccionLectura.entries.firstOrNull { it.name == direccionLectura }
            ?: DireccionLectura.LATERAL

    val guardado: ModoGuardado
        get() = ModoGuardado.entries.firstOrNull { it.name == modoGuardado }
            ?: ModoGuardado.PASO_A_PASO

    val calidad: CalidadVista
        get() = CalidadVista.entries.firstOrNull { it.name == calidadVista } ?: CalidadVista.EQUILIBRADA
    val donacion: EstadoDonacion
        get() = EstadoDonacion.entries.firstOrNull { it.name == estadoDonacion } ?: EstadoDonacion.SIN_MOSTRAR

    /**
     * Decide si toca ensenar el aviso de donacion al cerrar la sesion.
     *
     * Reglas del punto 4.4.3: una sola vez al cerrar la primera sesion con uso
     * real; si se aplaza, una segunda y ultima vez pasados 30 dias y 10 usos.
     */
    fun tocaMostrarDonacion(ahoraEpochMillis: Long): Boolean = when (donacion) {
        EstadoDonacion.SILENCIADA -> false
        EstadoDonacion.SIN_MOSTRAR -> sesionConUsoReal
        EstadoDonacion.APLAZADA -> {
            val dias = (ahoraEpochMillis - aplazadaEnEpochMillis) / MILIS_POR_DIA
            sesionConUsoReal && dias >= DIAS_DE_ESPERA && (usosReales - usosAlAplazar) >= USOS_DE_ESPERA
        }
    }

    companion object {
        const val DIAS_DE_ESPERA = 30L
        const val USOS_DE_ESPERA = 10
        const val MILIS_POR_DIA = 24L * 60L * 60L * 1000L
    }
}

/** Firma manuscrita que el usuario ha guardado para reutilizarla. */
@Serializable
data class FirmaGuardada(
    val id: String,
    val nombre: String,
    val trazos: List<List<Punto>>,
    val colorArgb: Long,
    val grosor: Float,
    val creadaEn: Long,
)

/** Cuando salen los ficheros de la carpeta privada de la aplicacion. */
/**
 * Como se recorre un documento en el visor.
 *
 * No hay una respuesta buena para todo el mundo: para revisar un contrato
 * pagina a pagina va mejor pasar de lado, y para leer de corrido va mejor
 * el desplazamiento continuo, que es como se lee cualquier cosa en un
 * telefono. Por eso se elige y no se decide por el usuario.
 */
/**
 * Que pasa cuando termina de crearse un documento.
 *
 * Abrirlo siempre estorba a quien encadena tareas y no quiere ver cada
 * paso; no abrirlo nunca deja con la duda de si ha salido bien. Como no hay
 * una respuesta buena para los dos, se elige.
 */
/**
 * Tareas que dejan un documento nuevo y que por tanto pueden abrirlo.
 *
 * Convertir esta solo en el sentido que acaba en PDF: para un .docx o un
 * .xlsx la aplicacion no tiene visor y no habria nada que abrir.
 */
enum class TareaConResultado {
    EDITAR,
    UNIR,
    FIRMAR,
    CONVERTIR,
    IMAGENES,
}

enum class AperturaAlTerminar {
    /** Se abre en cuanto esta listo. */
    ABRIR,

    /** Se pregunta antes de abrirlo. */
    PREGUNTAR,

    /** No se abre: se avisa y se vuelve al inicio. */
    NO_ABRIR,
}

enum class DireccionLectura {
    /** Una pagina cada vez, deslizando de lado. */
    LATERAL,

    /** Todas las paginas seguidas, desplazandose hacia abajo. */
    VERTICAL,
}

enum class ModoGuardado {
    /** Cada resultado intermedio se guarda segun se produce. */
    PASO_A_PASO,

    /** Solo sale lo que el usuario guarda o comparte a proposito. */
    SOLO_AL_FINAL,
}
