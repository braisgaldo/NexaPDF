package es.ghatostudio.nexapdf.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.graphics.blend.BlendMode
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import es.ghatostudio.nexapdf.domain.model.AlineacionTexto
import es.ghatostudio.nexapdf.domain.model.Edicion
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.TipoFigura
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory

/**
 * Convierte las anotaciones del editor en contenido real del PDF.
 *
 * Todo se dibuja dentro del sistema de coordenadas visible que instala
 * [TransformadorPagina], de modo que aqui no hay que pensar en rotaciones ni en
 * que el eje Y del PDF va al reves.
 */
class PintorEdiciones(
    private val documento: PDDocument,
    private val fuentes: FuentesPdf,
) {

    fun pintar(
        flujo: PDPageContentStream,
        transformador: TransformadorPagina,
        ediciones: List<Edicion>,
    ) {
        ediciones.forEach { edicion ->
            flujo.saveGraphicsState()
            runCatching {
                // El giro se aplica al sistema de coordenadas y no a cada punto:
                // se lleva el origen al centro del objeto, se gira, y se vuelve.
                // Asi el resto del pintado sigue trabajando en coordenadas de
                // pagina sin enterarse de que hay una rotacion en medio.
                val colocada = edicion as? Edicion.Colocada
                if (colocada != null && colocada.rotacion != 0f) {
                    val marco = colocada.marco.normalizado()
                    val cx = transformador.x((marco.izquierda + marco.derecha) / 2f)
                    val cy = transformador.y((marco.arriba + marco.abajo) / 2f)
                    // En pantalla el eje Y baja y en el PDF sube, asi que el
                    // mismo giro tiene el signo contrario aqui.
                    val radianes = Math.toRadians(-colocada.rotacion.toDouble())
                    flujo.transform(Matrix.getTranslateInstance(cx, cy))
                    flujo.transform(Matrix.getRotateInstance(radianes, 0f, 0f))
                    flujo.transform(Matrix.getTranslateInstance(-cx, -cy))
                }

                when (edicion) {
                    is Edicion.Trazo -> pintarTrazo(flujo, transformador, edicion)
                    is Edicion.Figura -> pintarFigura(flujo, transformador, edicion)
                    is Edicion.Texto -> pintarTexto(flujo, transformador, edicion)
                    is Edicion.Imagen -> pintarImagen(flujo, transformador, edicion)
                    is Edicion.Tapado -> pintarTapado(flujo, transformador, edicion)
                    is Edicion.Firma -> pintarFirma(flujo, transformador, edicion)
                }
            }
            flujo.restoreGraphicsState()
        }
    }

    // --- Trazos --------------------------------------------------------------

    private fun pintarTrazo(
        flujo: PDPageContentStream,
        t: TransformadorPagina,
        trazo: Edicion.Trazo,
    ) {
        if (trazo.puntos.size < 2) {
            // Un toque suelto sigue siendo un punto de tinta visible.
            trazo.puntos.firstOrNull()?.let { punto ->
                aplicarTransparencia(flujo, trazo.opacidad, trazo.resaltador)
                colorRelleno(flujo, trazo.colorArgb)
                val radio = t.ancho(trazo.grosor) / 2f
                circulo(flujo, t.x(punto.x), t.y(punto.y), radio)
                flujo.fill()
            }
            return
        }

        aplicarTransparencia(flujo, trazo.opacidad, trazo.resaltador)
        colorTrazo(flujo, trazo.colorArgb)
        flujo.setLineWidth(t.ancho(trazo.grosor))
        flujo.setLineCapStyle(1)
        flujo.setLineJoinStyle(1)

        val suavizado = suavizar(trazo.puntos)
        flujo.moveTo(t.x(suavizado.first().x), t.y(suavizado.first().y))
        // Curvas de Catmull-Rom convertidas a Bezier: el trazo del dedo llega
        // como una poligonal y sin suavizar se ven los vertices.
        for (indice in 0 until suavizado.size - 1) {
            val p0 = suavizado[max(indice - 1, 0)]
            val p1 = suavizado[indice]
            val p2 = suavizado[indice + 1]
            val p3 = suavizado[minOf(indice + 2, suavizado.size - 1)]

            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f

            flujo.curveTo(
                t.x(c1x), t.y(c1y),
                t.x(c2x), t.y(c2y),
                t.x(p2.x), t.y(p2.y),
            )
        }
        flujo.stroke()
    }

    /** Quita puntos casi repetidos, que solo engordan el fichero. */
    private fun suavizar(puntos: List<Punto>): List<Punto> {
        if (puntos.size <= 2) return puntos
        val resultado = mutableListOf(puntos.first())
        val minimo = 0.0015f
        puntos.drop(1).forEach { punto ->
            val ultimo = resultado.last()
            if (kotlin.math.abs(punto.x - ultimo.x) > minimo ||
                kotlin.math.abs(punto.y - ultimo.y) > minimo
            ) {
                resultado += punto
            }
        }
        if (resultado.last() != puntos.last()) resultado += puntos.last()
        return resultado
    }

    // --- Figuras -------------------------------------------------------------

    private fun pintarFigura(
        flujo: PDPageContentStream,
        t: TransformadorPagina,
        figura: Edicion.Figura,
    ) {
        aplicarTransparencia(flujo, figura.opacidad, resaltador = false)
        val marco = t.aRectanguloVisible(figura.marco)
        val grosor = t.ancho(figura.grosor)
        flujo.setLineWidth(grosor)
        flujo.setLineCapStyle(1)
        flujo.setLineJoinStyle(1)
        colorTrazo(flujo, figura.colorTrazoArgb)
        figura.colorRellenoArgb?.let { colorRelleno(flujo, it) }

        when (figura.tipo) {
            TipoFigura.RECTANGULO -> {
                flujo.addRect(marco.x, marco.y, marco.ancho, marco.alto)
                terminar(flujo, figura.colorRellenoArgb != null)
            }

            TipoFigura.ELIPSE -> {
                elipse(flujo, marco)
                terminar(flujo, figura.colorRellenoArgb != null)
            }

            TipoFigura.LINEA -> {
                val r = figura.marco
                flujo.moveTo(t.x(r.izquierda), t.y(r.arriba))
                flujo.lineTo(t.x(r.derecha), t.y(r.abajo))
                flujo.stroke()
            }

            TipoFigura.FLECHA -> {
                val r = figura.marco
                val x1 = t.x(r.izquierda)
                val y1 = t.y(r.arriba)
                val x2 = t.x(r.derecha)
                val y2 = t.y(r.abajo)
                flujo.moveTo(x1, y1)
                flujo.lineTo(x2, y2)
                flujo.stroke()

                val angulo = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
                val largo = max(grosor * 4f, 8f)
                val apertura = Math.toRadians(28.0)
                flujo.moveTo(x2, y2)
                flujo.lineTo(
                    (x2 - largo * cos(angulo - apertura)).toFloat(),
                    (y2 - largo * sin(angulo - apertura)).toFloat(),
                )
                flujo.moveTo(x2, y2)
                flujo.lineTo(
                    (x2 - largo * cos(angulo + apertura)).toFloat(),
                    (y2 - largo * sin(angulo + apertura)).toFloat(),
                )
                flujo.stroke()
            }
        }
    }

    private fun terminar(flujo: PDPageContentStream, conRelleno: Boolean) {
        if (conRelleno) flujo.fillAndStroke() else flujo.stroke()
    }

    /** Elipse con cuatro curvas de Bezier; la constante es la habitual. */
    private fun elipse(flujo: PDPageContentStream, marco: RectanguloPt) {
        val k = 0.5522847f
        val rx = marco.ancho / 2f
        val ry = marco.alto / 2f
        val cx = marco.x + rx
        val cy = marco.y + ry

        flujo.moveTo(cx - rx, cy)
        flujo.curveTo(cx - rx, cy + ry * k, cx - rx * k, cy + ry, cx, cy + ry)
        flujo.curveTo(cx + rx * k, cy + ry, cx + rx, cy + ry * k, cx + rx, cy)
        flujo.curveTo(cx + rx, cy - ry * k, cx + rx * k, cy - ry, cx, cy - ry)
        flujo.curveTo(cx - rx * k, cy - ry, cx - rx, cy - ry * k, cx - rx, cy)
        flujo.closePath()
    }

    private fun circulo(flujo: PDPageContentStream, cx: Float, cy: Float, radio: Float) {
        elipse(flujo, RectanguloPt(cx - radio, cy - radio, radio * 2, radio * 2))
    }

    // --- Texto ---------------------------------------------------------------

    private fun pintarTexto(
        flujo: PDPageContentStream,
        t: TransformadorPagina,
        texto: Edicion.Texto,
    ) {
        if (texto.contenido.isBlank()) return
        val marco = t.aRectanguloVisible(texto.marco)

        // El fondo va antes que las letras, evidentemente.
        texto.fondoArgb?.let { fondo ->
            colorRelleno(flujo, fondo)
            flujo.addRect(marco.x, marco.y, marco.ancho, marco.alto)
            flujo.fill()
        }

        val tamanoPt = (texto.tamano * t.altoVisible).coerceIn(4f, 400f)

        when (val eleccion = fuentes.elegir(texto.contenido, texto.negrita, texto.cursiva)) {
            is FuentesPdf.Eleccion.Vectorial ->
                escribirVectorial(flujo, texto, marco, tamanoPt, eleccion.fuente)

            FuentesPdf.Eleccion.Rasterizar ->
                escribirRasterizado(flujo, texto, marco, tamanoPt)
        }
    }

    private fun escribirVectorial(
        flujo: PDPageContentStream,
        texto: Edicion.Texto,
        marco: RectanguloPt,
        tamanoPt: Float,
        fuente: PDFont,
    ) {
        val lineas = ajustarLineas(texto.contenido, fuente, tamanoPt, marco.ancho)
        val interlineado = tamanoPt * 1.25f
        colorRelleno(flujo, texto.colorArgb)

        flujo.beginText()
        flujo.setFont(fuente, tamanoPt)
        // El origen del texto es su linea base, no la parte alta del rectangulo.
        var y = marco.y + marco.alto - tamanoPt * 0.92f
        var xAnterior = 0f
        var yAnterior = 0f
        var primera = true

        lineas.forEach { linea ->
            val anchoLinea = anchoSeguro(fuente, linea, tamanoPt)
            val x = when (texto.alineacion) {
                AlineacionTexto.INICIO -> marco.x
                AlineacionTexto.CENTRO -> marco.x + (marco.ancho - anchoLinea) / 2f
                AlineacionTexto.FIN -> marco.x + marco.ancho - anchoLinea
            }
            if (primera) {
                flujo.newLineAtOffset(x, y)
                primera = false
            } else {
                flujo.newLineAtOffset(x - xAnterior, y - yAnterior)
            }
            xAnterior = x
            yAnterior = y
            runCatching { flujo.showText(linea) }
            y -= interlineado
        }
        flujo.endText()
    }

    private fun anchoSeguro(fuente: PDFont, texto: String, tamano: Float): Float =
        runCatching { fuente.getStringWidth(texto) / 1000f * tamano }.getOrDefault(0f)

    private fun ajustarLineas(
        contenido: String,
        fuente: PDFont,
        tamano: Float,
        anchoDisponible: Float,
    ): List<String> {
        val resultado = mutableListOf<String>()
        contenido.split('\n').forEach { parrafo ->
            if (parrafo.isEmpty()) {
                resultado += ""
                return@forEach
            }
            var linea = StringBuilder()
            parrafo.split(' ').forEach { palabra ->
                val candidata = if (linea.isEmpty()) palabra else "$linea $palabra"
                if (anchoSeguro(fuente, candidata, tamano) <= anchoDisponible || linea.isEmpty()) {
                    linea = StringBuilder(candidata)
                } else {
                    resultado += linea.toString()
                    linea = StringBuilder(palabra)
                }
            }
            resultado += linea.toString()
        }
        return resultado
    }

    /**
     * Dibuja el texto como imagen usando el motor de Android.
     *
     * Es la salida para el arabe y cualquier otra escritura que necesite
     * conformado: StaticLayout aplica las formas contextuales y el orden
     * bidireccional correctos. A cambio, ese texto no queda seleccionable, y la
     * interfaz lo advierte antes de guardar.
     */
    private fun escribirRasterizado(
        flujo: PDPageContentStream,
        texto: Edicion.Texto,
        marco: RectanguloPt,
        tamanoPt: Float,
    ) {
        val escala = 4f // 288 ppp: suficiente para que no se vea pixelado al ampliar
        val anchoPx = (marco.ancho * escala).toInt().coerceIn(1, 6000)
        val altoPx = (marco.alto * escala).toInt().coerceIn(1, 6000)

        val pintura = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = texto.colorArgb.toInt()
            textSize = tamanoPt * escala
            typeface = Typeface.create(
                Typeface.DEFAULT,
                when {
                    texto.negrita && texto.cursiva -> Typeface.BOLD_ITALIC
                    texto.negrita -> Typeface.BOLD
                    texto.cursiva -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                },
            )
        }

        val alineacion = when (texto.alineacion) {
            AlineacionTexto.INICIO -> Layout.Alignment.ALIGN_NORMAL
            AlineacionTexto.CENTRO -> Layout.Alignment.ALIGN_CENTER
            AlineacionTexto.FIN -> Layout.Alignment.ALIGN_OPPOSITE
        }

        val disposicion = StaticLayout.Builder
            .obtain(texto.contenido, 0, texto.contenido.length, pintura, anchoPx)
            .setAlignment(alineacion)
            .setIncludePad(false)
            .build()

        val lienzo = Bitmap.createBitmap(anchoPx, altoPx, Bitmap.Config.ARGB_8888)
        Canvas(lienzo).also { disposicion.draw(it) }

        val imagen = LosslessFactory.createFromImage(documento, lienzo)
        flujo.drawImage(imagen, marco.x, marco.y, marco.ancho, marco.alto)
        lienzo.recycle()
    }

    // --- Imagenes y tapados --------------------------------------------------

    private fun pintarImagen(
        flujo: PDPageContentStream,
        t: TransformadorPagina,
        imagen: Edicion.Imagen,
    ) {
        val fichero = File(imagen.rutaImagen)
        if (!fichero.exists()) return
        val original = BitmapFactory.decodeFile(fichero.absolutePath) ?: return

        aplicarTransparencia(flujo, imagen.opacidad, resaltador = false)
        val marco = t.aRectanguloVisible(imagen.marco)

        // La foto se reduce a los pixeles que caben en su hueco a 300 ppp. Una
        // foto de movil son cuatro mil pixeles de ancho; metida entera en un
        // recuadro de cinco centimetros no se ve mejor y multiplica por veinte
        // el tamano del fichero.
        val anchoUtil = (marco.ancho * PUNTOS_POR_PULGADA_IMAGEN / 72f).toInt().coerceAtLeast(1)
        val altoUtil = (marco.alto * PUNTOS_POR_PULGADA_IMAGEN / 72f).toInt().coerceAtLeast(1)
        val escala = minOf(
            1f,
            anchoUtil.toFloat() / original.width,
            altoUtil.toFloat() / original.height,
        )
        val mapa = if (escala >= 1f) {
            original
        } else {
            Bitmap.createScaledBitmap(
                original,
                (original.width * escala).toInt().coerceAtLeast(1),
                (original.height * escala).toInt().coerceAtLeast(1),
                true,
            )
        }

        // JPEG para fotos y sin perdidas solo cuando hay transparencia que
        // conservar: guardar una fotografia sin perdidas engorda el PDF sin que
        // nadie note la diferencia.
        val objeto: PDImageXObject = if (mapa.hasAlpha()) {
            LosslessFactory.createFromImage(documento, mapa)
        } else {
            JPEGFactory.createFromImage(documento, mapa, CALIDAD_JPEG_IMAGEN)
        }
        flujo.drawImage(objeto, marco.x, marco.y, marco.ancho, marco.alto)
        if (mapa !== original) mapa.recycle()
        original.recycle()
    }

    private fun pintarTapado(
        flujo: PDPageContentStream,
        t: TransformadorPagina,
        tapado: Edicion.Tapado,
    ) {
        val marco = t.aRectanguloVisible(tapado.marco)
        colorRelleno(flujo, tapado.colorArgb)
        flujo.addRect(marco.x, marco.y, marco.ancho, marco.alto)
        flujo.fill()
    }

    private fun pintarFirma(
        flujo: PDPageContentStream,
        t: TransformadorPagina,
        firma: Edicion.Firma,
    ) {
        // Los trazos de una firma vienen normalizados respecto a su propia caja,
        // asi que primero se llevan al marco donde el usuario la ha soltado.
        val marco = firma.marco.normalizado()
        colorTrazo(flujo, firma.colorArgb)
        flujo.setLineWidth(t.ancho(firma.grosor))
        flujo.setLineCapStyle(1)
        flujo.setLineJoinStyle(1)

        firma.trazos.filter { it.size >= 2 }.forEach { trazo ->
            val puntos = trazo.map { punto ->
                Punto(
                    x = marco.izquierda + punto.x * marco.ancho,
                    y = marco.arriba + punto.y * marco.alto,
                )
            }
            flujo.moveTo(t.x(puntos.first().x), t.y(puntos.first().y))
            puntos.drop(1).forEach { flujo.lineTo(t.x(it.x), t.y(it.y)) }
            flujo.stroke()
        }
    }

    // --- Utilidades de color y transparencia ---------------------------------

    private fun aplicarTransparencia(
        flujo: PDPageContentStream,
        opacidad: Float,
        resaltador: Boolean,
    ) {
        if (opacidad >= 1f && !resaltador) return
        val estado = PDExtendedGraphicsState().apply {
            strokingAlphaConstant = opacidad
            nonStrokingAlphaConstant = opacidad
            // Un marcador fluorescente deja ver lo que hay debajo; si se pintase
            // en modo normal taparia el texto que pretende resaltar.
            if (resaltador) blendMode = BlendMode.MULTIPLY
        }
        flujo.setGraphicsStateParameters(estado)
    }

    private fun colorTrazo(flujo: PDPageContentStream, argb: Long) {
        val (r, g, b) = componentes(argb)
        flujo.setStrokingColor(r, g, b)
    }

    private fun colorRelleno(flujo: PDPageContentStream, argb: Long) {
        val (r, g, b) = componentes(argb)
        flujo.setNonStrokingColor(r, g, b)
    }

    private fun componentes(argb: Long): Triple<Float, Float, Float> = Triple(
        ((argb shr 16) and 0xFF).toFloat() / 255f,
        ((argb shr 8) and 0xFF).toFloat() / 255f,
        (argb and 0xFF).toFloat() / 255f,
    )
}


/** Resolucion a la que se guarda una imagen insertada: imprime bien y no infla el PDF. */
private const val PUNTOS_POR_PULGADA_IMAGEN = 300f

/** Calidad del JPEG de una imagen insertada. */
private const val CALIDAD_JPEG_IMAGEN = 0.85f
