package es.ghatostudio.nexapdf.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Punto en coordenadas normalizadas de pagina: 0..1 sobre el ancho y el alto,
 * con el origen arriba a la izquierda.
 *
 * Trabajar normalizado evita arrastrar densidades de pantalla y factores de zoom
 * por toda la aplicacion: el lienzo dibuja multiplicando por su tamano y el motor
 * de PDF multiplica por el tamano de la pagina en puntos, invirtiendo la Y porque
 * PDF cuenta desde abajo.
 */
@Serializable
data class Punto(val x: Float, val y: Float) {
    operator fun plus(otro: Punto) = Punto(x + otro.x, y + otro.y)
    operator fun minus(otro: Punto) = Punto(x - otro.x, y - otro.y)
}

/** Rectangulo normalizado, tambien con origen arriba a la izquierda. */
@Serializable
data class Rectangulo(
    val izquierda: Float,
    val arriba: Float,
    val derecha: Float,
    val abajo: Float,
) {
    val ancho: Float get() = abs(derecha - izquierda)
    val alto: Float get() = abs(abajo - arriba)
    val centro: Punto get() = Punto((izquierda + derecha) / 2f, (arriba + abajo) / 2f)

    /** Devuelve el rectangulo con las esquinas ordenadas y recortado a la pagina. */
    fun normalizado(): Rectangulo = Rectangulo(
        izquierda = min(izquierda, derecha).coerceIn(0f, 1f),
        arriba = min(arriba, abajo).coerceIn(0f, 1f),
        derecha = max(izquierda, derecha).coerceIn(0f, 1f),
        abajo = max(arriba, abajo).coerceIn(0f, 1f),
    )

    fun desplazado(dx: Float, dy: Float): Rectangulo =
        Rectangulo(izquierda + dx, arriba + dy, derecha + dx, abajo + dy)

    fun contiene(punto: Punto): Boolean {
        val n = normalizado()
        return punto.x >= n.izquierda && punto.x <= n.derecha &&
            punto.y >= n.arriba && punto.y <= n.abajo
    }

    /**
     * Como [contiene], pero ensanchando el rectangulo [margen] por cada lado.
     *
     * Existe porque una linea de texto de un PDF mide dos centesimas del alto
     * de la pagina: en un movil son treinta pixeles, menos de la mitad de la
     * yema de un dedo. Exigiendo que el toque cayera dentro de la caja exacta
     * de las letras, tocar una linea para sustituirla no funcionaba casi nunca
     * y la accion se iba a "anadir texto nuevo" sin decir por que.
     */
    fun contieneConMargen(punto: Punto, margen: Float): Boolean {
        val n = normalizado()
        return punto.x >= n.izquierda - margen && punto.x <= n.derecha + margen &&
            punto.y >= n.arriba - margen && punto.y <= n.abajo + margen
    }

    /** Distancia del punto al centro, para desempatar entre varios candidatos. */
    fun distanciaAlCentro(punto: Punto): Float {
        val n = normalizado()
        val cx = (n.izquierda + n.derecha) / 2f
        val cy = (n.arriba + n.abajo) / 2f
        val dx = punto.x - cx
        val dy = punto.y - cy
        return dx * dx + dy * dy
    }

    companion object {
        val COMPLETO = Rectangulo(0f, 0f, 1f, 1f)

        fun desdeEsquinas(a: Punto, b: Punto) =
            Rectangulo(a.x, a.y, b.x, b.y).normalizado()
    }
}
