package es.ghatostudio.nexapdf.domain

import es.ghatostudio.nexapdf.domain.escaner.MARGEN_TARJETA_PT
import es.ghatostudio.nexapdf.domain.escaner.PUNTOS_POR_MM
import es.ghatostudio.nexapdf.domain.escaner.SEPARACION_TARJETAS_PT
import es.ghatostudio.nexapdf.domain.escaner.TipoTarjeta
import es.ghatostudio.nexapdf.domain.escaner.huecoDeTarjeta
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El modo tarjeta, que se resume en una promesa: si lo imprimes, sale del
 * tamano del carne.
 *
 * Todo lo demas —el recorte, el enderezado, la mejora— es lo mismo que para una
 * hoja. Lo unico propio de este modo es la geometria de aqui, asi que es lo
 * unico que hay que probar, y conviene probarlo en milimetros de verdad: un
 * fallo aqui no rompe nada visible en pantalla y solo se descubre con una regla
 * encima de un papel ya impreso.
 */
class TarjetaTest {

    // A4 en puntos PDF.
    private val anchoA4 = 595.276f
    private val altoA4 = 841.89f

    private fun mm(puntos: Float) = puntos / PUNTOS_POR_MM

    @Test
    fun `un dni sale con las medidas de un dni`() {
        // ID-1: 85,60 x 53,98 mm. Es la medida de la norma ISO 7810, la que
        // cumplen el DNI, el carne de conducir y cualquier tarjeta de credito.
        val hueco = huecoDeTarjeta(
            tipo = TipoTarjeta.ID1,
            anchoImagenPx = 1600,
            altoImagenPx = 1000,
            anchoPaginaPt = anchoA4,
            altoPaginaPt = altoA4,
        )

        assertTrue(
            abs(mm(hueco.anchoPt) - 85.60f) < 0.1f,
            "el ancho salio de ${mm(hueco.anchoPt)} mm y tenia que ser 85,60",
        )
        assertTrue(
            abs(mm(hueco.altoPt) - 53.98f) < 0.1f,
            "el alto salio de ${mm(hueco.altoPt)} mm y tenia que ser 53,98",
        )
    }

    @Test
    fun `el anverso y el reverso caben en la misma hoja`() {
        // Es la razon de ser del modo: las dos caras juntas en un folio, para
        // entregarlo en una ventanilla. Si solo cupiera una, no habria modo.
        val hueco = huecoDeTarjeta(
            tipo = TipoTarjeta.ID1,
            anchoImagenPx = 1600,
            altoImagenPx = 1000,
            anchoPaginaPt = anchoA4,
            altoPaginaPt = altoA4,
        )

        assertTrue(hueco.porPagina >= 2, "solo caben ${hueco.porPagina} caras por hoja")

        // Y lo que se dice que cabe tiene que caber de verdad, con su aire y sus
        // margenes: esta es la cuenta que decide si la ultima sale cortada.
        val ocupado = hueco.porPagina * hueco.altoPt +
            (hueco.porPagina - 1) * SEPARACION_TARJETAS_PT
        assertTrue(
            ocupado <= altoA4 - 2 * MARGEN_TARJETA_PT + 0.01f,
            "dice que caben ${hueco.porPagina} pero ocupan $ocupado pt y solo hay " +
                "${altoA4 - 2 * MARGEN_TARJETA_PT}",
        )
    }

    @Test
    fun `un pasaporte tambien cabe por duplicado`() {
        val hueco = huecoDeTarjeta(
            tipo = TipoTarjeta.ID3,
            anchoImagenPx = 1500,
            altoImagenPx = 1056,
            anchoPaginaPt = anchoA4,
            altoPaginaPt = altoA4,
        )

        assertTrue(abs(mm(hueco.anchoPt) - 125f) < 0.1f, "ancho ${mm(hueco.anchoPt)} mm")
        assertTrue(hueco.porPagina >= 2, "solo caben ${hueco.porPagina} paginas de pasaporte")
    }

    @Test
    fun `una tarjeta fotografiada de pie se dibuja de pie`() {
        // Un DNI sujetado en vertical. Si el hueco no girase con la imagen, la
        // tarjeta se encajaria dentro de un rectangulo tumbado y saldria
        // diminuta en medio de la hoja.
        val hueco = huecoDeTarjeta(
            tipo = TipoTarjeta.ID1,
            anchoImagenPx = 1000,
            altoImagenPx = 1600,
            anchoPaginaPt = anchoA4,
            altoPaginaPt = altoA4,
        )

        assertTrue(
            hueco.altoPt > hueco.anchoPt,
            "la imagen venia de pie y el hueco salio tumbado: ${hueco.anchoPt} x ${hueco.altoPt}",
        )
        assertTrue(abs(mm(hueco.altoPt) - 85.60f) < 0.1f, "el lado largo mide ${mm(hueco.altoPt)} mm")
    }

    @Test
    fun `automatico distingue una tarjeta de un pasaporte`() {
        // ID-1 tiene proporcion 1,586 y el pasaporte 1,42. Son las dos formas
        // que la gente escanea y se separan bien.
        assertEquals(
            TipoTarjeta.ID1,
            TipoTarjeta.paraProporcion(1586, 1000),
            "una forma de tarjeta de credito deberia salir ID-1",
        )
        assertEquals(
            TipoTarjeta.ID3,
            TipoTarjeta.paraProporcion(1420, 1000),
            "una forma de pasaporte deberia salir ID-3",
        )
        // Y da igual como se sujetara: se compara lado largo contra lado corto.
        assertEquals(
            TipoTarjeta.ID1,
            TipoTarjeta.paraProporcion(1000, 1586),
            "girada tendria que dar lo mismo",
        )
    }

    @Test
    fun `automatico sobre una imagen sin medidas no revienta`() {
        assertEquals(TipoTarjeta.ID1, TipoTarjeta.paraProporcion(0, 0))

        val hueco = huecoDeTarjeta(
            tipo = TipoTarjeta.AUTOMATICO,
            anchoImagenPx = 1600,
            altoImagenPx = 1000,
            anchoPaginaPt = anchoA4,
            altoPaginaPt = altoA4,
        )
        assertTrue(hueco.anchoPt > 0f && hueco.altoPt > 0f, "el hueco salio vacio")
    }

    @Test
    fun `en una pagina diminuta la tarjeta se reduce en vez de salirse`() {
        // Una tarjeta mas grande que la hoja no se puede imprimir a tamano real,
        // y salirse de la pagina significa imprimirla cortada. Se prefiere
        // pequena y entera.
        val hueco = huecoDeTarjeta(
            tipo = TipoTarjeta.ID3,
            anchoImagenPx = 1500,
            altoImagenPx = 1056,
            anchoPaginaPt = 200f,
            altoPaginaPt = 300f,
        )

        assertTrue(
            hueco.anchoPt <= 200f - 2 * MARGEN_TARJETA_PT + 0.01f,
            "la tarjeta mide ${hueco.anchoPt} pt de ancho y la hoja util son " +
                "${200f - 2 * MARGEN_TARJETA_PT}",
        )
        assertTrue(hueco.porPagina >= 1, "tiene que caber al menos una")
    }

    @Test
    fun `nunca se amplia una tarjeta para llenar la hoja`() {
        // Estirar un DNI hasta ocupar el A4 seria justo lo contrario de lo que
        // este modo promete, por mucho que sobre sitio.
        val hueco = huecoDeTarjeta(
            tipo = TipoTarjeta.ID1,
            anchoImagenPx = 1600,
            altoImagenPx = 1000,
            anchoPaginaPt = 2000f,
            altoPaginaPt = 3000f,
        )
        assertTrue(
            abs(mm(hueco.anchoPt) - 85.60f) < 0.1f,
            "con una hoja enorme la tarjeta crecio a ${mm(hueco.anchoPt)} mm",
        )
    }
}
