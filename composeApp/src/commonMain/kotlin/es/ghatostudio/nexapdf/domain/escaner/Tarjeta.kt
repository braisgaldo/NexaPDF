package es.ghatostudio.nexapdf.domain.escaner

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Formatos normalizados de tarjeta y documento de identidad.
 *
 * Las medidas son las de la norma ISO/IEC 7810, que es la que cumplen el DNI, el
 * permiso de conducir, las tarjetas de credito, las de transporte y los
 * pasaportes de todo el mundo. No son aproximaciones: una tarjeta de credito
 * mide 85,60 x 53,98 mm en Tokio y en Vigo.
 *
 * **Por que esto importa.** Escanear una tarjeta no es escanear una hoja
 * pequena. Lo que se espera de la copia de un DNI es poder imprimirla y que
 * salga **del tamano del DNI**, porque para eso sirve: para entregarla en una
 * ventanilla junto a un impreso. Una foto recortada y estirada a A4 no vale para
 * eso aunque se vea muy bien. Por eso lo que define este modo no es el recorte
 * —de eso ya se encarga el detector, que no distingue una tarjeta de un folio—
 * sino el **tamano fisico** al que la tarjeta se dibuja en la pagina.
 */
enum class TipoTarjeta(val anchoMm: Float, val altoMm: Float) {

    /**
     * Se decide por la forma de lo capturado.
     *
     * Funciona, pero tiene un limite que conviene decir: ID-2 e ID-3 tienen
     * practicamente la misma proporcion (1,419 y 1,421), asi que **por la forma
     * no se pueden distinguir**. Automatico acierta al separar una tarjeta de
     * credito de un pasaporte, que es la diferencia que de verdad se nota al
     * imprimir; entre los dos formatos grandes elige el de pasaporte, que es el
     * que la gente escanea. Para lo demas estan las opciones explicitas.
     */
    AUTOMATICO(0f, 0f),

    /** ID-1: DNI, permiso de conducir, tarjeta de credito, abono de transporte. */
    ID1(85.60f, 53.98f),

    /** ID-2: algunos documentos de identidad antiguos y visados. */
    ID2(105f, 74f),

    /** ID-3: la pagina de datos de un pasaporte. */
    ID3(125f, 88f),
    ;

    /** Proporcion lado largo / lado corto. Cero para [AUTOMATICO]. */
    val proporcion: Float get() = if (altoMm <= 0f) 0f else anchoMm / altoMm

    companion object {

        /** Los formatos con medidas de verdad, sin [AUTOMATICO]. */
        val CONCRETOS: List<TipoTarjeta> get() = listOf(ID1, ID2, ID3)

        /**
         * El formato que mejor encaja con una imagen de estas proporciones.
         *
         * Se compara con el lado largo dividido por el corto, asi que da igual
         * que la tarjeta se fotografiara tumbada o de pie.
         */
        fun paraProporcion(anchoPx: Int, altoPx: Int): TipoTarjeta {
            if (anchoPx <= 0 || altoPx <= 0) return ID1
            val medida = max(anchoPx, altoPx).toFloat() / min(anchoPx, altoPx).toFloat()
            // ID2 se deja fuera de la eleccion automatica: comparte proporcion
            // con ID3 y elegir entre los dos por la forma seria echarlo a
            // suertes. Entre ellos gana el pasaporte, que es lo que se escanea.
            return listOf(ID1, ID3).minBy { abs(it.proporcion - medida) }
        }
    }
}

/**
 * Como se coloca una tarjeta en la pagina, ya en puntos PDF.
 *
 * @param anchoPt y [altoPt] el tamano **fisico** de la tarjeta en la pagina.
 * @param porPagina cuantas caben en una pagina, apiladas.
 */
data class HuecoDeTarjeta(
    val anchoPt: Float,
    val altoPt: Float,
    val porPagina: Int,
)

/**
 * Calcula el hueco de una tarjeta dentro de una pagina.
 *
 * La tarjeta se orienta como venga la imagen: un DNI fotografiado de pie se
 * dibuja de pie, con las mismas medidas cambiadas de sitio. Y si aun asi no cabe
 * a lo ancho —un pasaporte en una pagina estrecha— se reduce lo justo para que
 * quepa, porque una tarjeta que se sale de la pagina se imprime cortada, que es
 * peor que imprimirla algo pequena.
 */
fun huecoDeTarjeta(
    tipo: TipoTarjeta,
    anchoImagenPx: Int,
    altoImagenPx: Int,
    anchoPaginaPt: Float,
    altoPaginaPt: Float,
    margenPt: Float = MARGEN_TARJETA_PT,
    separacionPt: Float = SEPARACION_TARJETAS_PT,
): HuecoDeTarjeta {
    val formato = if (tipo == TipoTarjeta.AUTOMATICO) {
        TipoTarjeta.paraProporcion(anchoImagenPx, altoImagenPx)
    } else {
        tipo
    }

    val dePie = altoImagenPx > anchoImagenPx
    var ancho = (if (dePie) formato.altoMm else formato.anchoMm) * PUNTOS_POR_MM
    var alto = (if (dePie) formato.anchoMm else formato.altoMm) * PUNTOS_POR_MM

    val utilAncho = (anchoPaginaPt - 2 * margenPt).coerceAtLeast(1f)
    val utilAlto = (altoPaginaPt - 2 * margenPt).coerceAtLeast(1f)
    // Nunca se amplia: si cabe, va a su tamano de verdad y punto.
    val ajuste = min(1f, min(utilAncho / ancho, utilAlto / alto))
    ancho *= ajuste
    alto *= ajuste

    val caben = ((utilAlto + separacionPt) / (alto + separacionPt)).toInt().coerceAtLeast(1)
    return HuecoDeTarjeta(ancho, alto, caben)
}

/** Un milimetro en puntos PDF: 72 por pulgada, 25,4 milimetros por pulgada. */
const val PUNTOS_POR_MM = 72f / 25.4f

/** Margen alrededor del bloque de tarjetas. */
const val MARGEN_TARJETA_PT = 42f

/** Aire entre una tarjeta y la siguiente, para poder recortarlas. */
const val SEPARACION_TARJETAS_PT = 24f
