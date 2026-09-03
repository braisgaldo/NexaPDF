package es.ghatostudio.nexapdf.domain

import es.ghatostudio.nexapdf.domain.model.Ajustes
import es.ghatostudio.nexapdf.domain.model.EstadoDonacion
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Reglas de cuando puede aparecer el aviso de donacion.
 *
 * Es la parte de la app donde un fallo se nota mas: un aviso que sale dos veces,
 * o que sale sin que el usuario haya hecho nada, resulta molesto de inmediato.
 * De ahi que estas reglas esten aisladas en una funcion pura y cubiertas caso
 * por caso.
 */
class ReglasDonacionTest {

    private val ahora = 1_700_000_000_000L
    private val dia = Ajustes.MILIS_POR_DIA

    @Test
    fun no_aparece_si_la_sesion_no_ha_producido_nada() {
        val ajustes = Ajustes(sesionConUsoReal = false)
        assertFalse(ajustes.tocaMostrarDonacion(ahora))
    }

    @Test
    fun aparece_al_cerrar_la_primera_sesion_con_uso_real() {
        val ajustes = Ajustes(sesionConUsoReal = true, usosReales = 1)
        assertTrue(ajustes.tocaMostrarDonacion(ahora))
    }

    @Test
    fun una_vez_silenciada_no_vuelve_nunca() {
        val ajustes = Ajustes(
            estadoDonacion = EstadoDonacion.SILENCIADA.name,
            sesionConUsoReal = true,
            usosReales = 500,
            aplazadaEnEpochMillis = 0L,
        )
        assertFalse(ajustes.tocaMostrarDonacion(ahora + 3650 * dia))
    }

    @Test
    fun aplazada_no_vuelve_antes_de_treinta_dias_aunque_sobren_usos() {
        val ajustes = Ajustes(
            estadoDonacion = EstadoDonacion.APLAZADA.name,
            sesionConUsoReal = true,
            usosAlAplazar = 1,
            usosReales = 40,
            aplazadaEnEpochMillis = ahora,
        )
        assertFalse(ajustes.tocaMostrarDonacion(ahora + 29 * dia))
    }

    @Test
    fun aplazada_no_vuelve_sin_diez_usos_aunque_pase_un_ano() {
        val ajustes = Ajustes(
            estadoDonacion = EstadoDonacion.APLAZADA.name,
            sesionConUsoReal = true,
            usosAlAplazar = 5,
            usosReales = 12,
            aplazadaEnEpochMillis = ahora,
        )
        // Solo han pasado siete usos desde que se aplazo; hacen falta diez.
        assertFalse(ajustes.tocaMostrarDonacion(ahora + 365 * dia))
    }

    @Test
    fun aplazada_vuelve_una_segunda_vez_con_treinta_dias_y_diez_usos() {
        val ajustes = Ajustes(
            estadoDonacion = EstadoDonacion.APLAZADA.name,
            sesionConUsoReal = true,
            usosAlAplazar = 5,
            usosReales = 15,
            aplazadaEnEpochMillis = ahora,
        )
        assertTrue(ajustes.tocaMostrarDonacion(ahora + 30 * dia))
    }

    @Test
    fun aplazada_tampoco_vuelve_si_la_sesion_no_ha_producido_nada() {
        val ajustes = Ajustes(
            estadoDonacion = EstadoDonacion.APLAZADA.name,
            sesionConUsoReal = false,
            usosAlAplazar = 0,
            usosReales = 99,
            aplazadaEnEpochMillis = ahora,
        )
        assertFalse(ajustes.tocaMostrarDonacion(ahora + 100 * dia))
    }

    @Test
    fun el_estado_por_defecto_es_sin_mostrar() {
        assertTrue(Ajustes().donacion == EstadoDonacion.SIN_MOSTRAR)
    }

    @Test
    fun un_estado_desconocido_no_hace_que_reaparezca() {
        // Si una copia de seguridad trajera un valor que esta version no
        // conoce, lo prudente es tratarlo como el estado inicial y no como
        // "vuelve a mostrarlo": el usuario ya decidio algo.
        val ajustes = Ajustes(estadoDonacion = "UN_ESTADO_FUTURO", sesionConUsoReal = false)
        assertFalse(ajustes.tocaMostrarDonacion(ahora))
    }
}
