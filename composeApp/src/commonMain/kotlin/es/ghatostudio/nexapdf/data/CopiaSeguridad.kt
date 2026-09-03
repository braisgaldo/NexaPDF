package es.ghatostudio.nexapdf.data

import es.ghatostudio.nexapdf.domain.model.Ajustes
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.Serializable

/**
 * Formato del fichero .nexaPDF.bak.
 *
 * Es JSON en claro, no un ZIP ni una base de datos, porque lo unico que NexaPDF
 * guarda entre sesiones son las preferencias y las firmas: unos pocos kilobytes.
 * En claro tiene dos ventajas concretas: el usuario puede abrirlo y ver que no
 * hay nada raro dentro, y una copia corrupta se puede reparar a mano.
 *
 * Los documentos PDF no van en la copia. Son ficheros del usuario que ya estan
 * en su telefono; duplicarlos aqui multiplicaria el tamano de la copia por mil
 * sin darle nada que no tuviera.
 */
@Serializable
data class CopiaSeguridad(
    val formato: String = FORMATO,
    val versionEsquema: Int = VERSION_ESQUEMA,
    val versionApp: String,
    val creadaEnEpochMillis: Long,
    val creadaEnLegible: String,
    val ajustes: Ajustes,
) {
    companion object {
        const val FORMATO = "es.ghatostudio.nexapdf.copia"
        const val VERSION_ESQUEMA = 1
        const val EXTENSION = ".nexaPDF.bak"
        const val TIPO_MIME = "application/octet-stream"
    }
}

/** Por que no se ha podido leer una copia. */
enum class ErrorCopia {
    NO_ES_JSON,
    FORMATO_DESCONOCIDO,
    ESQUEMA_MAS_NUEVO,
    CONTENIDO_INCOMPLETO,
}

sealed interface ResultadoCopia {
    data class Exito(val copia: CopiaSeguridad) : ResultadoCopia
    data class Fallo(val error: ErrorCopia, val detalle: String? = null) : ResultadoCopia
}

/**
 * Lee y escribe copias de seguridad.
 *
 * La lectura es deliberadamente estricta con la cabecera y tolerante con el
 * resto: si el fichero no es una copia de NexaPDF hay que decirlo cuanto antes,
 * pero una copia de una version anterior a la que le falten campos nuevos debe
 * poder restaurarse igualmente.
 */
class CodecCopiaSeguridad(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {

    fun escribir(ajustes: Ajustes, versionApp: String, ahora: Long, fechaLegible: String): String {
        val copia = CopiaSeguridad(
            versionApp = versionApp,
            creadaEnEpochMillis = ahora,
            creadaEnLegible = fechaLegible,
            ajustes = ajustes,
        )
        return json.encodeToString(copia)
    }

    fun leer(contenido: String): ResultadoCopia {
        val raiz = try {
            json.parseToJsonElement(contenido).jsonObject
        } catch (e: SerializationException) {
            return ResultadoCopia.Fallo(ErrorCopia.NO_ES_JSON, e.message)
        } catch (e: IllegalArgumentException) {
            return ResultadoCopia.Fallo(ErrorCopia.NO_ES_JSON, e.message)
        }

        val formato = raiz["formato"]?.jsonPrimitive?.contentOrNulo()
        if (formato != CopiaSeguridad.FORMATO) {
            return ResultadoCopia.Fallo(ErrorCopia.FORMATO_DESCONOCIDO, formato)
        }

        val esquema = runCatching { raiz["versionEsquema"]?.jsonPrimitive?.int }.getOrNull()
            ?: return ResultadoCopia.Fallo(ErrorCopia.CONTENIDO_INCOMPLETO, "versionEsquema")

        if (esquema > CopiaSeguridad.VERSION_ESQUEMA) {
            return ResultadoCopia.Fallo(ErrorCopia.ESQUEMA_MAS_NUEVO, esquema.toString())
        }

        val migrado = migrar(raiz, esquema)

        return try {
            ResultadoCopia.Exito(json.decodeFromJsonElement(CopiaSeguridad.serializer(), migrado))
        } catch (e: SerializationException) {
            ResultadoCopia.Fallo(ErrorCopia.CONTENIDO_INCOMPLETO, e.message)
        }
    }

    /**
     * Lleva una copia de un esquema antiguo al actual.
     *
     * Ahora mismo solo existe el esquema 1, asi que no hay nada que migrar. La
     * funcion existe ya, con su prueba, para que anadir el esquema 2 sea escribir
     * un `when` mas y no rehacer la lectura entera.
     */
    private fun migrar(raiz: JsonObject, desdeEsquema: Int): JsonObject {
        var actual = raiz
        var version = desdeEsquema
        while (version < CopiaSeguridad.VERSION_ESQUEMA) {
            actual = when (version) {
                else -> actual
            }
            version++
        }
        return if (version == desdeEsquema) {
            actual
        } else {
            buildJsonObject {
                actual.forEach { (clave, valor) -> put(clave, valor) }
                put("versionEsquema", JsonPrimitive(CopiaSeguridad.VERSION_ESQUEMA))
            }
        }
    }

    private fun JsonPrimitive.contentOrNulo(): String? = if (isString) content else null
}
