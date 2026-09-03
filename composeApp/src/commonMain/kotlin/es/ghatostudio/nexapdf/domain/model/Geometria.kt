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

    companion object {
        val COMPLETO = Rectangulo(0f, 0f, 1f, 1f)

        fun desdeEsquinas(a: Punto, b: Punto) =
            Rectangulo(a.x, a.y, b.x, b.y).normalizado()
    }
}
