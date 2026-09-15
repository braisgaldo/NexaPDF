package es.ghatostudio.nexapdf.domain.escaner

import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Los cuatro vertices del papel dentro de la foto, en coordenadas normalizadas
 * (0..1) con el origen arriba a la izquierda.
 *
 * Se guarda un cuadrilatero y no un rectangulo porque una foto de un papel
 * hecha a mano nunca sale de frente: los lados convergen. Enderezarla es
 * justamente lo que separa "una foto de un folio" de "un documento escaneado",
 * y para eso hacen falta las cuatro esquinas por separado.
 *
 * El orden es fijo y recorre el papel en sentido horario empezando arriba a la
 * izquierda. Todo lo que consume un cuadrilatero cuenta con ese orden: si se
 * pierde, la correccion de perspectiva sale del reves.
 */
data class Cuadrilatero(
    val supIzq: Punto,
    val supDer: Punto,
    val infDer: Punto,
    val infIzq: Punto,
) {
    val esquinas: List<Punto> get() = listOf(supIzq, supDer, infDer, infIzq)

    /** Area por la formula del cordon de zapato, en fraccion de la imagen. */
    val area: Float
        get() {
            val puntos = esquinas
            var suma = 0f
            for (indice in puntos.indices) {
                val actual = puntos[indice]
                val siguiente = puntos[(indice + 1) % puntos.size]
                suma += actual.x * siguiente.y - siguiente.x * actual.y
            }
            return abs(suma) / 2f
        }

    /**
     * Todos los vertices giran en el mismo sentido.
     *
     * Un cuadrilatero cruzado (en forma de lazo) tiene area y cuatro esquinas
     * igual que uno bueno, pero al enderezarlo produce una imagen doblada sobre
     * si misma. Se descarta antes de llegar ahi.
     */
    val esConvexo: Boolean
        get() {
            val puntos = esquinas
            var positivos = 0
            var negativos = 0
            for (indice in puntos.indices) {
                val a = puntos[indice]
                val b = puntos[(indice + 1) % 4]
                val c = puntos[(indice + 2) % 4]
                val cruz = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
                if (cruz > 0f) positivos++ else if (cruz < 0f) negativos++
            }
            return positivos == 0 || negativos == 0
        }

    /** Caja que lo contiene, para encuadres rapidos. */
    fun envolvente(): Rectangulo = Rectangulo(
        izquierda = esquinas.minOf { it.x },
        arriba = esquinas.minOf { it.y },
        derecha = esquinas.maxOf { it.x },
        abajo = esquinas.maxOf { it.y },
    )

    /** Mueve una esquina y devuelve el cuadrilatero nuevo, acotado a la imagen. */
    fun conEsquina(indice: Int, destino: Punto): Cuadrilatero {
        val acotado = Punto(destino.x.coerceIn(0f, 1f), destino.y.coerceIn(0f, 1f))
        return when (indice) {
            0 -> copy(supIzq = acotado)
            1 -> copy(supDer = acotado)
            2 -> copy(infDer = acotado)
            else -> copy(infIzq = acotado)
        }
    }

    /**
     * Proporcion ancho/alto que tendra la pagina una vez enderezada.
     *
     * Se toma el lado mas largo de cada par opuesto: al enderezar, el borde que
     * quedo mas cerca de la camara es el que conserva el detalle, y encoger
     * hasta el mas corto tiraria pixeles que si estaban.
     */
    fun proporcionEnderezada(anchoImagen: Int, altoImagen: Int): Float {
        val (ancho, alto) = ladosEnPixeles(anchoImagen, altoImagen)
        return if (alto <= 0f) 1f else ancho / alto
    }

    /**
     * Cuanto mide la pagina enderezada, en pixeles de la imagen de origen.
     *
     * Se toma el lado mas largo de cada par opuesto, no la caja que envuelve al
     * cuadrilatero. Con la caja, una hoja girada sobre la mesa daba una medida
     * mucho mayor que el papel —la diagonal en vez del lado— y el enderezado
     * ampliaba la imagen por encima de los pixeles que de verdad habia, que es
     * una forma cara de emborronar el texto.
     */
    fun ladosEnPixeles(anchoImagen: Int, altoImagen: Int): Pair<Float, Float> {
        fun largo(a: Punto, b: Punto): Float {
            val dx = (a.x - b.x) * anchoImagen
            val dy = (a.y - b.y) * altoImagen
            return sqrt(dx * dx + dy * dy)
        }
        return max(largo(supIzq, supDer), largo(infIzq, infDer)) to
            max(largo(supIzq, infIzq), largo(supDer, infDer))
    }

    companion object {
        /** La imagen entera: lo que se usa cuando no se detecta nada. */
        val COMPLETO = Cuadrilatero(
            supIzq = Punto(0f, 0f),
            supDer = Punto(1f, 0f),
            infDer = Punto(1f, 1f),
            infIzq = Punto(0f, 1f),
        )

        /**
         * Ordena cuatro puntos sueltos en el recorrido horario esperado.
         *
         * Son dos pasos y hacen falta los dos:
         *
         *  1. **Recorrido.** Se ordenan por el angulo respecto al centro, que es
         *     lo unico que da un recorrido sin cruces sea cual sea el giro del
         *     papel. Clasificar por "la de suma menor es la de arriba a la
         *     izquierda" funciona con un folio derecho y se rompe con uno girado
         *     cuarenta grados, donde dos esquinas contiguas empatan.
         *  2. **Punto de partida.** Del recorrido ya ordenado se empieza por la
         *     esquina mas cercana al origen. Asi dos detecciones seguidas del
         *     mismo papel nombran igual a cada esquina, que es lo que evita que
         *     el suavizado mezcle la de arriba con la de la izquierda y de un
         *     latigazo en pantalla.
         */
        fun ordenar(puntos: List<Punto>): Cuadrilatero? {
            if (puntos.size != 4) return null
            val centroX = puntos.sumOf { it.x.toDouble() }.toFloat() / 4f
            val centroY = puntos.sumOf { it.y.toDouble() }.toFloat() / 4f

            // Con la Y hacia abajo, ordenar por angulo creciente recorre el
            // cuadrilatero en el sentido de las agujas del reloj tal y como se
            // ve en pantalla.
            val recorrido = puntos.sortedBy {
                atan2(it.y - centroY, it.x - centroX)
            }
            if (recorrido.distinct().size != 4) return null

            val primera = recorrido.indices.minBy {
                val punto = recorrido[it]
                punto.x + punto.y
            }
            val girado = List(4) { recorrido[(primera + it) % 4] }
            return Cuadrilatero(girado[0], girado[1], girado[2], girado[3])
        }
    }
}

/**
 * Lo que el detector ve en un fotograma.
 *
 * La confianza no es un adorno: es lo que se ensena en pantalla mientras se
 * apunta, y lo que decide si el disparo automatico se atreve o no. Va de 0 a 1
 * y mide que parte del contorno del papel se distingue de verdad, no el parecido
 * con un rectangulo ideal.
 */
data class DeteccionDocumento(
    val cuadro: Cuadrilatero?,
    val confianza: Float,
    /**
     * El contorno lleva varios fotogramas sin moverse.
     *
     * Es lo que de verdad decide si la captura automatica dispara. Con la
     * confianza sola no bastaba: un encuadre puede estar al noventa por ciento y
     * seguir bailando porque la mano no se ha parado, y la foto salia movida.
     * Quieto y bien encuadrado son dos cosas distintas y hay que exigir las dos.
     */
    val estable: Boolean = false,
) {
    val hayPapel: Boolean get() = cuadro != null && confianza >= UMBRAL_VISIBLE

    /** Porcentaje entero, que es como se ensena. */
    val porcentaje: Int get() = (confianza.coerceIn(0f, 1f) * 100f).toInt()

    companion object {
        val NADA = DeteccionDocumento(null, 0f)

        /** Por debajo de esto no se dibuja el contorno: seria ruido en pantalla. */
        const val UMBRAL_VISIBLE = 0.35f

        /**
         * A partir de aqui el disparo automatico se considera seguro.
         *
         * No es 1,0 a proposito: un papel blanco sobre una mesa clara nunca
         * llega al contorno perfecto, y exigirlo dejaria la captura automatica
         * sin disparar jamas.
         */
        const val UMBRAL_AUTOMATICO = 0.80f
    }
}

/**
 * Una hoja ya capturada, tal y como el usuario la puede retocar antes de
 * convertirla en PDF.
 *
 * Se guarda la ruta de la foto original y aparte el recorte y el filtro, en vez
 * de reescribir la imagen en cada cambio: asi mover una esquina o cambiar de
 * filtro es instantaneo y siempre se trabaja sobre los pixeles originales, sin
 * ir perdiendo calidad a cada retoque.
 */
data class HojaEscaneada(
    val id: String,
    val rutaOriginal: String,
    /**
     * Las demas fotos de la rafaga, si las hubo.
     *
     * Son disparos de la misma hoja hechos seguidos, que se funden con la
     * primera para quitar grano antes de mejorar la pagina. Van aparte de
     * [rutaOriginal] a proposito: la de referencia es **esa** y solo esa, es la
     * que fija el encuadre, la que se ensena al revisar y la unica que sigue
     * valiendo si las demas se descartan por no encajar.
     */
    val rutasRafaga: List<String> = emptyList(),
    val cuadro: Cuadrilatero,
    val filtro: FiltroPagina = FiltroPagina.DOCUMENTO_NITIDO,
    val intensidadFiltro: Float = 0.5f,
    /** Giro en pasos de 90 grados aplicado despues de enderezar. */
    val giroGrados: Int = 0,
    /** Confianza con la que se detecto el recorte, para poder avisar. */
    val confianza: Float = 0f,
) {
    val recortada: Boolean get() = cuadro != Cuadrilatero.COMPLETO

    fun girada(): HojaEscaneada = copy(giroGrados = (giroGrados + 90) % 360)
}

/** Una palabra reconocida por el OCR, situada sobre la imagen. */
data class PalabraOcr(
    val texto: String,
    /** Caja de la palabra en coordenadas normalizadas de la imagen. */
    val marco: Rectangulo,
)

/** Texto de una pagina, listo para incrustarse como capa invisible. */
data class TextoPagina(val palabras: List<PalabraOcr>) {
    val vacia: Boolean get() = palabras.isEmpty()

    companion object {
        val SIN_TEXTO = TextoPagina(emptyList())
    }
}

/**
 * Una pagina lista para entrar en el PDF: la imagen ya enderezada y mejorada, y
 * el texto que se leyo sobre ella.
 *
 * La imagen y el texto viajan juntos porque las coordenadas del texto son las de
 * *esa* imagen. Reconocer el texto sobre la foto original y pegarlo sobre la
 * imagen recortada dejaria las palabras desplazadas: al buscar, el visor
 * senalaria un sitio y la palabra estaria en otro.
 */
data class PaginaEscaneada(
    val rutaImagen: String,
    val texto: TextoPagina = TextoPagina.SIN_TEXTO,
)

/**
 * Si lo que se ve ahora es otra escena distinta de la hoja ya fotografiada.
 *
 * Es lo que libera la captura automatica despues de un disparo. Sin esta
 * condicion, un segundo despues de la foto el papel sigue ahi, sigue bien
 * encuadrado y sigue quieto, asi que se cumplian otra vez todas las condiciones
 * y salian **dos fotos de la misma pagina**. Una pausa mas larga no lo arregla,
 * solo lo retrasa: lo que hay que exigir es que la escena cambie.
 *
 * Vale con que el papel desaparezca del encuadre, que es lo que pasa al
 * levantar el telefono para cambiar de hoja, o con que el contorno se haya
 * movido lo suficiente, que es lo que pasa al poner otra hoja encima.
 */
fun escenaDistinta(
    tomada: Cuadrilatero,
    ahora: Cuadrilatero?,
    umbral: Float = MOVIMIENTO_PARA_OTRA_HOJA,
): Boolean {
    if (ahora == null) return true
    val unas = tomada.esquinas
    val otras = ahora.esquinas
    var suma = 0f
    for (indice in 0 until 4) {
        val dx = unas[indice].x - otras[indice].x
        val dy = unas[indice].y - otras[indice].y
        suma += sqrt(dx * dx + dy * dy)
    }
    return suma / 4f > umbral
}

/**
 * Cuanto tiene que moverse el contorno para contar como otra hoja.
 *
 * Un ocho por ciento de la pantalla de media por esquina. Por debajo es la misma
 * hoja y solo se ha movido la mano; por encima es que se ha cambiado el papel.
 */
const val MOVIMIENTO_PARA_OTRA_HOJA = 0.08f

/** Lado mas corto de un cuadrilatero, en pixeles. */
internal fun ladoMinimo(cuadro: Cuadrilatero, ancho: Int, alto: Int): Float {
    val puntos = cuadro.esquinas
    var minimo = Float.MAX_VALUE
    for (indice in puntos.indices) {
        val a = puntos[indice]
        val b = puntos[(indice + 1) % 4]
        val dx = (a.x - b.x) * ancho
        val dy = (a.y - b.y) * alto
        minimo = min(minimo, sqrt(dx * dx + dy * dy))
    }
    return minimo
}
