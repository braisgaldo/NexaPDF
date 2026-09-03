package es.ghatostudio.nexapdf.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Una anotacion colocada sobre una pagina.
 *
 * Todas las ediciones son datos puros y serializables: el lienzo las pinta en
 * tiempo real sin tocar el PDF, y solo al guardar el motor las convierte en
 * contenido real del documento. Asi editar es instantaneo por caro que sea el
 * PDF de origen, y siempre se puede deshacer.
 */
@Serializable
sealed interface Edicion {
    val id: String

    /**
     * Edicion que ocupa un marco y se puede seleccionar para moverla,
     * escalarla o girarla, como un objeto de cualquier editor.
     *
     * No todas lo son: un trazo a mano alzada son puntos sueltos, y un tapado
     * es la mitad de una sustitucion de texto, no algo que se coloque aparte.
     */
    sealed interface Colocada : Edicion {
        val marco: Rectangulo

        /** Giro en grados alrededor del centro del marco. */
        val rotacion: Float
    }

    /** Trazo a mano alzada (dedo o lapiz). */
    @Serializable
    @SerialName("trazo")
    data class Trazo(
        override val id: String,
        val puntos: List<Punto>,
        val colorArgb: Long,
        /** Grosor como fraccion del ancho de pagina, para que el zoom no lo altere. */
        val grosor: Float,
        val opacidad: Float = 1f,
        /** Un marcador fluorescente se multiplica sobre el contenido en vez de taparlo. */
        val resaltador: Boolean = false,
    ) : Edicion

    /** Figura geometrica. */
    @Serializable
    @SerialName("figura")
    data class Figura(
        override val id: String,
        val tipo: TipoFigura,
        val marco: Rectangulo,
        val colorTrazoArgb: Long,
        val colorRellenoArgb: Long? = null,
        val grosor: Float,
        val opacidad: Float = 1f,
    ) : Edicion

    /** Caja de texto anadida o texto de sustitucion. */
    @Serializable
    @SerialName("texto")
    data class Texto(
        override val id: String,
        val contenido: String,
        override val marco: Rectangulo,
        val colorArgb: Long,
        /** Tamano de fuente como fraccion del alto de pagina. */
        val tamano: Float,
        val negrita: Boolean = false,
        val cursiva: Boolean = false,
        val alineacion: AlineacionTexto = AlineacionTexto.INICIO,
        override val rotacion: Float = 0f,
    ) : Colocada

    /** Imagen incrustada. */
    @Serializable
    @SerialName("imagen")
    data class Imagen(
        override val id: String,
        val rutaImagen: String,
        override val marco: Rectangulo,
        val opacidad: Float = 1f,
        override val rotacion: Float = 0f,
    ) : Colocada

    /**
     * Rectangulo opaco que tapa contenido. Se usa para sustituir texto: se cubre
     * el original y encima se coloca un [Texto] nuevo.
     */
    @Serializable
    @SerialName("tapado")
    data class Tapado(
        override val id: String,
        val marco: Rectangulo,
        val colorArgb: Long,
    ) : Edicion

    /** Firma manuscrita colocada como sello. */
    @Serializable
    @SerialName("firma")
    data class Firma(
        override val id: String,
        val trazos: List<List<Punto>>,
        override val marco: Rectangulo,
        val colorArgb: Long,
        val grosor: Float,
        override val rotacion: Float = 0f,
    ) : Colocada
}

enum class TipoFigura {
    RECTANGULO,
    ELIPSE,
    LINEA,
    FLECHA,
}

enum class AlineacionTexto { INICIO, CENTRO, FIN }

/**
 * Mejora global aplicada a la pagina completa.
 *
 * Los filtros son el unico caso en el que la pagina se rasteriza: no hay forma
 * de aplicar un ajuste de contraste al contenido vectorial de un PDF sin
 * convertirlo antes en imagen. Solo se aplican a las paginas que el usuario
 * marca, y la interfaz avisa de que esas paginas dejaran de tener texto
 * seleccionable.
 */
enum class FiltroPagina {
    NINGUNO,
    ESCALA_DE_GRISES,
    BLANCO_Y_NEGRO,
    DOCUMENTO_NITIDO,
    ALTO_CONTRASTE,
    ACLARAR,
    INVERTIR,
}

/** Estado completo de edicion de una pagina. */
@Serializable
data class EdicionPagina(
    val indice: Int,
    val ediciones: List<Edicion> = emptyList(),
    val filtro: FiltroPagina = FiltroPagina.NINGUNO,
    val intensidadFiltro: Float = 0.5f,
) {
    val tieneCambios: Boolean
        get() = ediciones.isNotEmpty() || filtro != FiltroPagina.NINGUNO
}

/** Conjunto de ediciones pendientes de aplicar sobre un documento. */
@Serializable
data class BorradorEdicion(
    val rutaDocumento: String,
    val paginas: Map<Int, EdicionPagina> = emptyMap(),
) {
    val tieneCambios: Boolean get() = paginas.values.any { it.tieneCambios }

    fun pagina(indice: Int): EdicionPagina = paginas[indice] ?: EdicionPagina(indice)

    fun conPagina(pagina: EdicionPagina): BorradorEdicion =
        copy(paginas = paginas + (pagina.indice to pagina))
}
