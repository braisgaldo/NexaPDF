package es.ghatostudio.nexapdf.data

import es.ghatostudio.nexapdf.domain.model.Ajustes
import es.ghatostudio.nexapdf.domain.model.FirmaGuardada
import es.ghatostudio.nexapdf.domain.model.Punto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Pruebas del formato .nexaPDF.bak.
 *
 * Una copia de seguridad que no se puede restaurar es peor que no tenerla, asi
 * que se comprueba el viaje de ida y vuelta completo y, sobre todo, que cada
 * forma de fichero invalido produzca el error concreto que la interfaz sabe
 * explicar, en vez de un fallo generico.
 */
class CodecCopiaSeguridadTest {

    private val codec = CodecCopiaSeguridad()

    private val ajustesDeEjemplo = Ajustes(
        familiaTema = "BOSQUE",
        modoTema = "OSCURO",
        idioma = "gl",
        calidadVista = "NITIDA",
        confirmarAccionesDestructivas = false,
        guardarEnDescargasAlTerminar = false,
        estadoDonacion = "APLAZADA",
        usosReales = 7,
        usosAlAplazar = 3,
        aplazadaEnEpochMillis = 1_700_000_000_000L,
        nombreParaFirmas = "Brais Galdo",
        firmasGuardadas = listOf(
            FirmaGuardada(
                id = "f1",
                nombre = "Habitual",
                trazos = listOf(listOf(Punto(0f, 0f), Punto(0.5f, 0.4f), Punto(1f, 0.1f))),
                colorArgb = 0xFF1A1A1AL,
                grosor = 0.004f,
                creadaEn = 1_700_000_000_000L,
            ),
        ),
    )

    @Test
    fun exportar_e_importar_devuelve_exactamente_los_mismos_ajustes() {
        val contenido = codec.escribir(
            ajustes = ajustesDeEjemplo,
            versionApp = "1.0.0",
            ahora = 1_700_000_000_000L,
            fechaLegible = "14 de noviembre de 2023",
        )

        val leida = codec.leer(contenido)
        assertIs<ResultadoCopia.Exito>(leida)
        assertEquals(ajustesDeEjemplo, leida.copia.ajustes)
        assertEquals("1.0.0", leida.copia.versionApp)
        assertEquals(CopiaSeguridad.VERSION_ESQUEMA, leida.copia.versionEsquema)
        assertEquals(CopiaSeguridad.FORMATO, leida.copia.formato)
    }

    @Test
    fun las_firmas_guardadas_sobreviven_al_viaje() {
        val contenido = codec.escribir(ajustesDeEjemplo, "1.0.0", 0L, "hoy")
        val leida = codec.leer(contenido)
        assertIs<ResultadoCopia.Exito>(leida)

        val firma = leida.copia.ajustes.firmasGuardadas.single()
        assertEquals("Habitual", firma.nombre)
        assertEquals(3, firma.trazos.single().size)
        assertEquals(Punto(0.5f, 0.4f), firma.trazos.single()[1])
    }

    @Test
    fun la_preferencia_de_donacion_viaja_en_la_copia() {
        // Es el motivo por el que la copia existe tal cual: reinstalar la app no
        // debe volver a mostrar un aviso que el usuario ya silencio.
        val silenciada = ajustesDeEjemplo.copy(estadoDonacion = "SILENCIADA")
        val leida = codec.leer(codec.escribir(silenciada, "1.0.0", 0L, "hoy"))
        assertIs<ResultadoCopia.Exito>(leida)
        assertEquals("SILENCIADA", leida.copia.ajustes.estadoDonacion)
    }

    @Test
    fun un_fichero_que_no_es_json_se_rechaza_como_tal() {
        val leida = codec.leer("esto no es JSON, es una foto renombrada")
        assertIs<ResultadoCopia.Fallo>(leida)
        assertEquals(ErrorCopia.NO_ES_JSON, leida.error)
    }

    @Test
    fun un_json_de_otra_aplicacion_se_rechaza_por_formato() {
        val leida = codec.leer("""{"formato":"otra.app","versionEsquema":1}""")
        assertIs<ResultadoCopia.Fallo>(leida)
        assertEquals(ErrorCopia.FORMATO_DESCONOCIDO, leida.error)
    }

    @Test
    fun una_copia_de_una_version_futura_se_rechaza_sin_estropear_nada() {
        val futura = """
            {
              "formato": "${CopiaSeguridad.FORMATO}",
              "versionEsquema": ${CopiaSeguridad.VERSION_ESQUEMA + 1},
              "versionApp": "9.9.9",
              "creadaEnEpochMillis": 0,
              "creadaEnLegible": "manana",
              "ajustes": {}
            }
        """.trimIndent()

        val leida = codec.leer(futura)
        assertIs<ResultadoCopia.Fallo>(leida)
        assertEquals(ErrorCopia.ESQUEMA_MAS_NUEVO, leida.error)
    }

    @Test
    fun una_copia_a_la_que_le_falta_la_version_de_esquema_se_rechaza() {
        val leida = codec.leer("""{"formato":"${CopiaSeguridad.FORMATO}"}""")
        assertIs<ResultadoCopia.Fallo>(leida)
        assertEquals(ErrorCopia.CONTENIDO_INCOMPLETO, leida.error)
    }

    @Test
    fun una_copia_antigua_a_la_que_le_falten_campos_nuevos_se_restaura_igual() {
        // Tolerante en la lectura del contenido y estricta en la cabecera: una
        // copia hecha antes de anadir una preferencia debe seguir sirviendo, y
        // los campos que falten toman su valor por defecto.
        val minima = """
            {
              "formato": "${CopiaSeguridad.FORMATO}",
              "versionEsquema": 1,
              "versionApp": "1.0.0",
              "creadaEnEpochMillis": 0,
              "creadaEnLegible": "antes",
              "ajustes": { "familiaTema": "OCASO" }
            }
        """.trimIndent()

        val leida = codec.leer(minima)
        assertIs<ResultadoCopia.Exito>(leida)
        assertEquals("OCASO", leida.copia.ajustes.familiaTema)
        assertEquals(Ajustes().modoTema, leida.copia.ajustes.modoTema)
        assertTrue(leida.copia.ajustes.firmasGuardadas.isEmpty())
    }

    @Test
    fun el_contenido_escrito_es_legible_para_una_persona() {
        // Que se pueda abrir con un editor y entender es parte del formato: es
        // lo que permite comprobar que dentro no hay nada raro.
        val contenido = codec.escribir(ajustesDeEjemplo, "1.0.0", 0L, "hoy")
        assertTrue(contenido.contains("\n"), "Deberia ir formateado en varias lineas")
        assertTrue(contenido.contains(CopiaSeguridad.FORMATO))
        assertTrue(contenido.contains("familiaTema"))
    }
}
