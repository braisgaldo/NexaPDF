package es.ghatostudio.nexapdf.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import es.ghatostudio.nexapdf.domain.model.Ajustes
import es.ghatostudio.nexapdf.domain.model.EstadoDonacion
import es.ghatostudio.nexapdf.domain.model.FirmaGuardada
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import es.ghatostudio.nexapdf.domain.model.TareaConResultado

/**
 * Preferencias de la aplicacion sobre DataStore.
 *
 * Las claves son planas salvo las firmas guardadas, que se serializan a JSON:
 * son pocas y pequenas, y meterlas en una base de datos aparte solo para eso
 * complicaria la copia de seguridad sin ganar nada.
 */
class RepositorioAjustes(
    private val almacen: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {

    private object Claves {
        val familiaTema = stringPreferencesKey("familia_tema")
        val modoTema = stringPreferencesKey("modo_tema")
        val idioma = stringPreferencesKey("idioma")
        val calidadVista = stringPreferencesKey("calidad_vista")
        val confirmarDestructivas = booleanPreferencesKey("confirmar_destructivas")
        val guardarEnDescargas = booleanPreferencesKey("guardar_en_descargas")
        val estadoDonacion = stringPreferencesKey("estado_donacion")
        val usosReales = intPreferencesKey("usos_reales")
        val usosAlAplazar = intPreferencesKey("usos_al_aplazar")
        val aplazadaEn = longPreferencesKey("aplazada_en")
        val sesionConUsoReal = booleanPreferencesKey("sesion_con_uso_real")
        val tourVisto = booleanPreferencesKey("tour_visto")
        val modoGuardado = stringPreferencesKey("modo_guardado")
        val direccionLectura = stringPreferencesKey("direccion_lectura")
        val aperturaEditar = stringPreferencesKey("apertura_editar")
        val aperturaUnir = stringPreferencesKey("apertura_unir")
        val aperturaFirmar = stringPreferencesKey("apertura_firmar")
        val aperturaConvertir = stringPreferencesKey("apertura_convertir")
        val aperturaImagenes = stringPreferencesKey("apertura_imagenes")
        val aperturaCifrar = stringPreferencesKey("apertura_cifrar")
        val carpetaDestino = stringPreferencesKey("carpeta_destino")
        val preguntarCompartir = booleanPreferencesKey("preguntar_compartir")
        val resumenSeparar = booleanPreferencesKey("resumen_separar")
        val pedirManuscrita = booleanPreferencesKey("pedir_manuscrita")
        val firmas = stringPreferencesKey("firmas_guardadas")
        val nombreFirmas = stringPreferencesKey("nombre_para_firmas")
    }

    val ajustes: Flow<Ajustes> = almacen.data.map { preferencias ->
        val porDefecto = Ajustes()
        Ajustes(
            familiaTema = preferencias[Claves.familiaTema] ?: porDefecto.familiaTema,
            modoTema = preferencias[Claves.modoTema] ?: porDefecto.modoTema,
            idioma = preferencias[Claves.idioma],
            calidadVista = preferencias[Claves.calidadVista] ?: porDefecto.calidadVista,
            confirmarAccionesDestructivas = preferencias[Claves.confirmarDestructivas]
                ?: porDefecto.confirmarAccionesDestructivas,
            guardarEnDescargasAlTerminar = preferencias[Claves.guardarEnDescargas]
                ?: porDefecto.guardarEnDescargasAlTerminar,
            tourVisto = preferencias[Claves.tourVisto] ?: porDefecto.tourVisto,
            modoGuardado = preferencias[Claves.modoGuardado] ?: porDefecto.modoGuardado,
            direccionLectura = preferencias[Claves.direccionLectura]
                ?: porDefecto.direccionLectura,
            aperturaEditar = preferencias[Claves.aperturaEditar] ?: porDefecto.aperturaEditar,
            aperturaUnir = preferencias[Claves.aperturaUnir] ?: porDefecto.aperturaUnir,
            aperturaFirmar = preferencias[Claves.aperturaFirmar]
                ?: porDefecto.aperturaFirmar,
            aperturaConvertir = preferencias[Claves.aperturaConvertir]
                ?: porDefecto.aperturaConvertir,
            aperturaImagenes = preferencias[Claves.aperturaImagenes]
                ?: porDefecto.aperturaImagenes,
            aperturaCifrar = preferencias[Claves.aperturaCifrar]
                ?: porDefecto.aperturaCifrar,
            carpetaDestino = preferencias[Claves.carpetaDestino],
            preguntarCompartir = preferencias[Claves.preguntarCompartir]
                ?: porDefecto.preguntarCompartir,
            resumenAlSepararEnPartes = preferencias[Claves.resumenSeparar]
                ?: porDefecto.resumenAlSepararEnPartes,
            pedirFirmaManuscrita = preferencias[Claves.pedirManuscrita]
                ?: porDefecto.pedirFirmaManuscrita,
            estadoDonacion = preferencias[Claves.estadoDonacion] ?: porDefecto.estadoDonacion,
            usosReales = preferencias[Claves.usosReales] ?: 0,
            usosAlAplazar = preferencias[Claves.usosAlAplazar] ?: 0,
            aplazadaEnEpochMillis = preferencias[Claves.aplazadaEn] ?: 0L,
            sesionConUsoReal = preferencias[Claves.sesionConUsoReal] ?: false,
            firmasGuardadas = leerFirmas(preferencias[Claves.firmas]),
            nombreParaFirmas = preferencias[Claves.nombreFirmas] ?: "",
        )
    }

    suspend fun actual(): Ajustes = ajustes.first()

    private fun leerFirmas(bruto: String?): List<FirmaGuardada> =
        if (bruto.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<FirmaGuardada>>(bruto) }.getOrDefault(emptyList())
        }

    suspend fun fijarFamiliaTema(clave: String) = editar { it[Claves.familiaTema] = clave }

    suspend fun fijarModoTema(clave: String) = editar { it[Claves.modoTema] = clave }

    suspend fun fijarIdioma(etiqueta: String?) = editar {
        if (etiqueta == null) it.remove(Claves.idioma) else it[Claves.idioma] = etiqueta
    }

    suspend fun fijarCalidadVista(clave: String) = editar { it[Claves.calidadVista] = clave }

    suspend fun fijarConfirmarDestructivas(valor: Boolean) =
        editar { it[Claves.confirmarDestructivas] = valor }

    suspend fun fijarGuardarEnDescargas(valor: Boolean) =
        editar { it[Claves.guardarEnDescargas] = valor }

    suspend fun fijarNombreParaFirmas(nombre: String) =
        editar { it[Claves.nombreFirmas] = nombre }

    suspend fun guardarFirmas(firmas: List<FirmaGuardada>) =
        editar { it[Claves.firmas] = json.encodeToString(firmas) }

    /**
     * Marca que en esta sesion el usuario ha hecho algo real con la app.
     *
     * "Real" significa que se ha producido un documento: unir, separar, crear
     * desde imagenes, guardar una edicion o firmar. Abrir un PDF y cerrarlo no
     * cuenta, porque no ha aportado nada y pedir dinero por eso seria pedirlo
     * por nada.
     */
    suspend fun registrarUsoReal() = editar { preferencias ->
        if (preferencias[Claves.sesionConUsoReal] != true) {
            preferencias[Claves.sesionConUsoReal] = true
            preferencias[Claves.usosReales] = (preferencias[Claves.usosReales] ?: 0) + 1
        }
    }

    suspend fun reiniciarSesion() = editar { it[Claves.sesionConUsoReal] = false }

    suspend fun marcarTourVisto() = editar { it[Claves.tourVisto] = true }

    suspend fun fijarModoGuardado(clave: String) = editar { it[Claves.modoGuardado] = clave }

    suspend fun fijarDireccionLectura(clave: String) =
        editar { it[Claves.direccionLectura] = clave }

    suspend fun fijarApertura(tarea: TareaConResultado, clave: String) = editar {
        it[claveDe(tarea)] = clave
    }

    private fun claveDe(tarea: TareaConResultado) = when (tarea) {
        TareaConResultado.EDITAR -> Claves.aperturaEditar
        TareaConResultado.UNIR -> Claves.aperturaUnir
        TareaConResultado.FIRMAR -> Claves.aperturaFirmar
        TareaConResultado.CONVERTIR -> Claves.aperturaConvertir
        TareaConResultado.IMAGENES -> Claves.aperturaImagenes
        TareaConResultado.CIFRAR -> Claves.aperturaCifrar
    }

    suspend fun fijarPreguntarCompartir(valor: Boolean) =
        editar { it[Claves.preguntarCompartir] = valor }

    suspend fun fijarResumenAlSeparar(valor: Boolean) =
        editar { it[Claves.resumenSeparar] = valor }

    suspend fun fijarPedirFirmaManuscrita(valor: Boolean) =
        editar { it[Claves.pedirManuscrita] = valor }

    suspend fun fijarCarpetaDestino(uri: String?) = editar {
        if (uri == null) it.remove(Claves.carpetaDestino) else it[Claves.carpetaDestino] = uri
    }

    suspend fun aplazarDonacion(ahora: Long) = editar { preferencias ->
        preferencias[Claves.estadoDonacion] = EstadoDonacion.APLAZADA.name
        preferencias[Claves.aplazadaEn] = ahora
        preferencias[Claves.usosAlAplazar] = preferencias[Claves.usosReales] ?: 0
        preferencias[Claves.sesionConUsoReal] = false
    }

    suspend fun silenciarDonacion() = editar { preferencias ->
        preferencias[Claves.estadoDonacion] = EstadoDonacion.SILENCIADA.name
        preferencias[Claves.sesionConUsoReal] = false
    }

    /** Sustituye los ajustes completos. Lo usa la importacion de copias. */
    suspend fun reemplazar(nuevos: Ajustes) = editar { preferencias ->
        preferencias[Claves.familiaTema] = nuevos.familiaTema
        preferencias[Claves.modoTema] = nuevos.modoTema
        if (nuevos.idioma == null) preferencias.remove(Claves.idioma)
        else preferencias[Claves.idioma] = nuevos.idioma
        preferencias[Claves.calidadVista] = nuevos.calidadVista
        preferencias[Claves.confirmarDestructivas] = nuevos.confirmarAccionesDestructivas
        preferencias[Claves.guardarEnDescargas] = nuevos.guardarEnDescargasAlTerminar
        preferencias[Claves.tourVisto] = nuevos.tourVisto
        preferencias[Claves.modoGuardado] = nuevos.modoGuardado
        preferencias[Claves.direccionLectura] = nuevos.direccionLectura
        preferencias[Claves.aperturaEditar] = nuevos.aperturaEditar
        preferencias[Claves.aperturaUnir] = nuevos.aperturaUnir
        preferencias[Claves.aperturaFirmar] = nuevos.aperturaFirmar
        preferencias[Claves.aperturaConvertir] = nuevos.aperturaConvertir
        preferencias[Claves.aperturaImagenes] = nuevos.aperturaImagenes
        preferencias[Claves.aperturaCifrar] = nuevos.aperturaCifrar
        preferencias[Claves.preguntarCompartir] = nuevos.preguntarCompartir
        preferencias[Claves.resumenSeparar] = nuevos.resumenAlSepararEnPartes
        preferencias[Claves.pedirManuscrita] = nuevos.pedirFirmaManuscrita
        if (nuevos.carpetaDestino == null) preferencias.remove(Claves.carpetaDestino)
        else preferencias[Claves.carpetaDestino] = nuevos.carpetaDestino
        preferencias[Claves.estadoDonacion] = nuevos.estadoDonacion
        preferencias[Claves.usosReales] = nuevos.usosReales
        preferencias[Claves.usosAlAplazar] = nuevos.usosAlAplazar
        preferencias[Claves.aplazadaEn] = nuevos.aplazadaEnEpochMillis
        preferencias[Claves.firmas] = json.encodeToString(nuevos.firmasGuardadas)
        preferencias[Claves.nombreFirmas] = nuevos.nombreParaFirmas
    }

    private suspend fun editar(bloque: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        almacen.edit(bloque)
    }
}
