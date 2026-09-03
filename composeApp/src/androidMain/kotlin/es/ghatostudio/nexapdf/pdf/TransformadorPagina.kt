package es.ghatostudio.nexapdf.pdf

import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.util.Matrix
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.Rectangulo

/**
 * Puente entre las coordenadas del editor y las del PDF.
 *
 * El editor trabaja en coordenadas normalizadas (0..1, origen arriba a la
 * izquierda) sobre la pagina *tal como se ve*. Un PDF, en cambio, mide en puntos
 * desde abajo a la izquierda y ademas puede llevar una rotacion `/Rotate` que el
 * lector aplica al mostrarlo pero que no afecta al contenido guardado.
 *
 * Si se ignora esa rotacion, todo lo que el usuario dibuje en una pagina girada
 * aparece movido y torcido. En vez de convertir punto a punto, esta clase da la
 * matriz que lleva del "espacio visible" al espacio real de la pagina: se aplica
 * una vez al abrir el flujo de contenido y a partir de ahi se puede dibujar
 * texto, imagenes y trazos usando directamente las coordenadas que ve el usuario.
 */
class TransformadorPagina(pagina: PDPage) {

    private val caja = pagina.cropBox
    private val anchoPagina = caja.width
    private val altoPagina = caja.height
    private val rotacion = ((pagina.rotation % 360) + 360) % 360

    /** Ancho del area visible, en puntos. */
    val anchoVisible: Float = if (rotacion % 180 == 0) anchoPagina else altoPagina

    /** Alto del area visible, en puntos. */
    val altoVisible: Float = if (rotacion % 180 == 0) altoPagina else anchoPagina

    /**
     * Matriz que convierte el espacio visible en espacio de pagina.
     *
     * Se deduce de a donde va cada esquina al girar la pagina; los terminos de
     * desplazamiento incluyen el origen del cropBox, que no siempre es (0,0).
     */
    val matriz: Matrix = when (rotacion) {
        90 -> Matrix(0f, 1f, -1f, 0f, anchoPagina + caja.lowerLeftX, caja.lowerLeftY)
        180 -> Matrix(-1f, 0f, 0f, -1f, anchoPagina + caja.lowerLeftX, altoPagina + caja.lowerLeftY)
        270 -> Matrix(0f, -1f, 1f, 0f, caja.lowerLeftX, altoPagina + caja.lowerLeftY)
        else -> Matrix(1f, 0f, 0f, 1f, caja.lowerLeftX, caja.lowerLeftY)
    }

    /** Punto normalizado a coordenadas del espacio visible, en puntos. */
    fun aVisible(punto: Punto): Pair<Float, Float> =
        punto.x * anchoVisible to (1f - punto.y) * altoVisible

    /** Coordenada X normalizada a puntos del espacio visible. */
    fun x(nx: Float): Float = nx * anchoVisible

    /** Coordenada Y normalizada (origen arriba) a puntos del espacio visible. */
    fun y(ny: Float): Float = (1f - ny) * altoVisible

    /** Longitud relativa al ancho de pagina, en puntos. */
    fun ancho(fraccion: Float): Float = fraccion * anchoVisible

    /** Longitud relativa al alto de pagina, en puntos. */
    fun alto(fraccion: Float): Float = fraccion * altoVisible

    /**
     * Rectangulo normalizado a (x, y, ancho, alto) del espacio visible, con la
     * Y ya apuntando hacia arriba como espera el PDF.
     */
    fun aRectanguloVisible(rectangulo: Rectangulo): RectanguloPt {
        val r = rectangulo.normalizado()
        val izquierda = x(r.izquierda)
        val derecha = x(r.derecha)
        val arriba = y(r.arriba)
        val abajo = y(r.abajo)
        return RectanguloPt(
            x = izquierda,
            y = abajo,
            ancho = (derecha - izquierda).coerceAtLeast(0.1f),
            alto = (arriba - abajo).coerceAtLeast(0.1f),
        )
    }
}

/** Rectangulo en puntos con origen abajo a la izquierda. */
data class RectanguloPt(val x: Float, val y: Float, val ancho: Float, val alto: Float)
