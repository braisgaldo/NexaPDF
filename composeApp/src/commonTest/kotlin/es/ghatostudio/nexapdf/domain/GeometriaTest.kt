package es.ghatostudio.nexapdf.domain

import es.ghatostudio.nexapdf.domain.model.DisposicionImagenes
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.RangoPaginas
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeometriaTest {

    @Test
    fun un_rectangulo_dibujado_hacia_atras_se_normaliza() {
        // Arrastrar de abajo a la derecha hacia arriba a la izquierda es tan
        // valido como al reves, y debe dar la misma figura.
        val alReves = Rectangulo(0.8f, 0.9f, 0.2f, 0.1f).normalizado()
        assertEquals(0.2f, alReves.izquierda)
        assertEquals(0.1f, alReves.arriba)
        assertEquals(0.8f, alReves.derecha)
        assertEquals(0.9f, alReves.abajo)
    }

    @Test
    fun normalizar_recorta_lo_que_se_sale_de_la_pagina() {
        val desbordado = Rectangulo(-0.5f, -0.2f, 1.7f, 1.4f).normalizado()
        assertEquals(0f, desbordado.izquierda)
        assertEquals(0f, desbordado.arriba)
        assertEquals(1f, desbordado.derecha)
        assertEquals(1f, desbordado.abajo)
    }

    @Test
    fun contiene_acierta_dentro_y_fuera() {
        val marco = Rectangulo(0.2f, 0.2f, 0.6f, 0.5f)
        assertTrue(marco.contiene(Punto(0.4f, 0.3f)))
        assertTrue(marco.contiene(Punto(0.2f, 0.2f)))
        assertFalse(marco.contiene(Punto(0.7f, 0.3f)))
        assertFalse(marco.contiene(Punto(0.4f, 0.6f)))
    }

    @Test
    fun un_rango_de_paginas_funciona_en_los_dos_sentidos() {
        assertEquals(listOf(2, 3, 4), RangoPaginas(2, 4).paginas)
        assertEquals(listOf(2, 3, 4), RangoPaginas(4, 2).paginas)
        assertEquals(1, RangoPaginas(5, 5).cuantas)
    }

    @Test
    fun la_proporcion_de_una_pagina_tiene_en_cuenta_su_rotacion() {
        val vertical = PaginaPdf(indice = 0, anchoPt = 595f, altoPt = 842f, rotacion = 0)
        val girada = PaginaPdf(indice = 0, anchoPt = 595f, altoPt = 842f, rotacion = 90)

        assertTrue(vertical.proporcion < 1f, "Sin girar es mas alta que ancha")
        assertTrue(girada.proporcion > 1f, "Girada 90 grados se ve apaisada")
        assertEquals(1f, vertical.proporcion * girada.proporcion, 0.0001f)
    }

    @Test
    fun la_proporcion_a_ciento_ochenta_grados_es_la_misma_que_sin_girar() {
        val sinGirar = PaginaPdf(0, 595f, 842f, rotacion = 0)
        val delReves = PaginaPdf(0, 595f, 842f, rotacion = 180)
        assertEquals(sinGirar.proporcion, delReves.proporcion, 0.0001f)
    }

    @Test
    fun cada_disposicion_declara_filas_y_columnas_coherentes() {
        DisposicionImagenes.entries.forEach { disposicion ->
            assertEquals(
                disposicion.porPagina,
                disposicion.filas * disposicion.columnas,
                "En $disposicion las filas por las columnas no dan las imagenes por pagina",
            )
        }
    }

    @Test
    fun desplazar_no_cambia_el_tamano() {
        val original = Rectangulo(0.1f, 0.2f, 0.5f, 0.6f)
        val movido = original.desplazado(0.2f, -0.1f)
        assertEquals(original.ancho, movido.ancho, 0.0001f)
        assertEquals(original.alto, movido.alto, 0.0001f)
        assertEquals(0.3f, movido.izquierda, 0.0001f)
    }
}
