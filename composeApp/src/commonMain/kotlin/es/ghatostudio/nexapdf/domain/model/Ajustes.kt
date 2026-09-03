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
