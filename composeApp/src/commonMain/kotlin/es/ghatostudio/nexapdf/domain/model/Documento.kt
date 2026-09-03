package es.ghatostudio.nexapdf.domain.model

import kotlinx.serialization.Serializable

/** Un PDF ya copiado al espacio de trabajo interno de la app. */
@Serializable
data class DocumentoPdf(
    val id: String,
    val ruta: String,
    val nombre: String,
    val numeroPaginas: Int,
    val tamanoBytes: Long,
    val cifrado: Boolean = false,
    val importadoEn: Long = 0L,
)

/** Geometria de una pagina, en puntos PDF (1 pt = 1/72 pulgada). */
@Serializable
data class PaginaPdf(
    val indice: Int,
    val anchoPt: Float,
    val altoPt: Float,
    val rotacion: Int = 0,
) {
    /** Relacion ancho/alto ya teniendo en cuenta la rotacion de la pagina. */
    val proporcion: Float
        get() = if (rotacion % 180 == 0) anchoPt / altoPt else altoPt / anchoPt
}

/** Una imagen copiada al espacio de trabajo. */
@Serializable
data class ImagenOrigen(
    val ruta: String,
    val nombre: String,
    val anchoPx: Int,
    val altoPx: Int,
)

/** Rango de paginas inclusivo, base 0. */
@Serializable
data class RangoPaginas(val desde: Int, val hasta: Int) {
    val paginas: List<Int> get() = (minOf(desde, hasta)..maxOf(desde, hasta)).toList()
    val cuantas: Int get() = paginas.size
}

/** Tamanos de pagina disponibles al crear un PDF a partir de imagenes. */
enum class TamanoPagina(val anchoPt: Float, val altoPt: Float) {
    /** Se adapta a la proporcion de cada imagen; no recorta ni deja margenes. */
    AJUSTAR_A_IMAGEN(0f, 0f),
    A4(595.28f, 841.89f),
    CARTA(612f, 792f),
    A5(419.53f, 595.28f),
    A3(841.89f, 1190.55f),
}

/** Como se colocan las imagenes dentro del PDF resultante. */
enum class DisposicionImagenes(val porPagina: Int) {
    UNA_POR_PAGINA(1),
    DOS_POR_PAGINA(2),
    CUATRO_POR_PAGINA(4),
    SEIS_POR_PAGINA(6),
    ;

    val filas: Int
        get() = when (this) {
            UNA_POR_PAGINA -> 1
            DOS_POR_PAGINA -> 2
            CUATRO_POR_PAGINA -> 2
            SEIS_POR_PAGINA -> 3
        }

    val columnas: Int
        get() = when (this) {
            UNA_POR_PAGINA -> 1
            DOS_POR_PAGINA -> 1
            CUATRO_POR_PAGINA -> 2
            SEIS_POR_PAGINA -> 2
        }
}

/** Orientacion forzada de la pagina generada. */
enum class Orientacion { VERTICAL, HORIZONTAL, AUTOMATICA }

/** Bloque de texto localizado dentro de una pagina, para poder editarlo. */
data class BloqueTexto(
    val id: String,
    val texto: String,
    val marco: Rectangulo,
    /** Tamano de fuente estimado, en puntos PDF. */
    val tamanoFuentePt: Float,
)
