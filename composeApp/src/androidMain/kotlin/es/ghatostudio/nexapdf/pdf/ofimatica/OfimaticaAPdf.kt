package es.ghatostudio.nexapdf.pdf.ofimatica

import android.graphics.BitmapFactory
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import es.ghatostudio.nexapdf.pdf.FuentesPdf
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Convierte documentos de ofimatica a PDF.
 *
 * Traduce el **contenido**, no la maquetacion: parrafos con sus negritas y
 * cursivas, tablas con sus celdas, diapositivas con sus textos e imagenes. No
 * reproduce el diseno exacto de Word, Excel o PowerPoint, porque eso exige un
 * motor de composicion tipografica completo, y el unico que existe para estos
 * formatos pesa mas que cualquier app de movil. La interfaz avisa de ello antes
 * de convertir.
 */
class OfimaticaAPdf {

    private companion object {
        val TAMANO_PAGINA: PDRectangle = PDRectangle.A4
        val TAMANO_DIAPOSITIVA = PDRectangle(720f, 540f) // 10 x 7,5 pulgadas
        const val MARGEN = 56f
        const val INTERLINEADO = 1.35f

        /** Unidades de un punto tipografico en EMU, la unidad de OOXML. */
        const val EMU_POR_PUNTO = 12700f
    }

    // --- DOCX ----------------------------------------------------------------

    /** Un parrafo con el formato que se puede reproducir de verdad. */
    private data class Parrafo(
        val texto: String,
        val negrita: Boolean,
        val cursiva: Boolean,
        val tamanoPt: Float,
        val saltoDePagina: Boolean,
    )

    fun docxAPdf(origen: File, destino: File) {
        val xml = PaqueteOoxml.leerTexto(origen, "word/document.xml")
            ?: error("El .docx no contiene word/document.xml")

        val parrafos = XmlPlano.bloques(xml, "w:p").map { bloqueParrafo ->
            // Un parrafo son varias "runs"; cada una puede tener su formato.
            val runs = XmlPlano.bloques(bloqueParrafo, "w:r")
            val texto = runs.joinToString("") { XmlPlano.textoDe(it, "w:t") }
            val primeraRun = runs.firstOrNull().orEmpty()
            val propiedades = XmlPlano.bloques(primeraRun, "w:rPr").firstOrNull().orEmpty()

            Parrafo(
                texto = texto,
                negrita = XmlPlano.contiene(propiedades, "w:b"),
                cursiva = XmlPlano.contiene(propiedades, "w:i"),
                // w:sz va en medios puntos, que es como lo guarda Word.
                tamanoPt = XmlPlano.atributosDe(propiedades, "w:sz").firstOrNull()
                    ?.let { XmlPlano.atributo(it, "w:val")?.toFloatOrNull() }
                    ?.div(2f)
                    ?: 11f,
                saltoDePagina = bloqueParrafo.contains("w:type=\"page\""),
            )
        }

        escribirDocumentoDeTexto(destino, parrafos)
    }

    private fun escribirDocumentoDeTexto(destino: File, parrafos: List<Parrafo>) {
        PDDocument().use { documento ->
            val fuentes = FuentesPdf(documento)
            var pagina = nuevaPagina(documento, TAMANO_PAGINA)
            var flujo = PDPageContentStream(documento, pagina)
            var y = TAMANO_PAGINA.height - MARGEN
            val anchoUtil = TAMANO_PAGINA.width - MARGEN * 2

            fun saltarPagina() {
                flujo.close()
                pagina = nuevaPagina(documento, TAMANO_PAGINA)
                flujo = PDPageContentStream(documento, pagina)
                y = TAMANO_PAGINA.height - MARGEN
            }

            parrafos.forEach { parrafo ->
                if (parrafo.saltoDePagina && y < TAMANO_PAGINA.height - MARGEN) saltarPagina()

                if (parrafo.texto.isBlank()) {
                    y -= parrafo.tamanoPt * INTERLINEADO
                    return@forEach
                }

                val fuente = fuenteQueSirva(fuentes, parrafo.texto, parrafo.negrita, parrafo.cursiva)
                val lineas = repartirEnLineas(parrafo.texto, fuente, parrafo.tamanoPt, anchoUtil)
                val alturaLinea = parrafo.tamanoPt * INTERLINEADO

                lineas.forEach { linea ->
                    if (y - alturaLinea < MARGEN) saltarPagina()
                    flujo.beginText()
                    flujo.setFont(fuente, parrafo.tamanoPt)
                    flujo.newLineAtOffset(MARGEN, y - parrafo.tamanoPt)
                    runCatching { flujo.showText(linea) }
                    flujo.endText()
                    y -= alturaLinea
                }
                // Un poco de aire entre parrafos, como en cualquier documento.
                y -= parrafo.tamanoPt * 0.4f
            }

            flujo.close()
            if (documento.numberOfPages == 0) nuevaPagina(documento, TAMANO_PAGINA)
            documento.save(destino)
        }
    }

    // --- XLSX ----------------------------------------------------------------

    fun xlsxAPdf(origen: File, destino: File) {
        val compartidas = cadenasCompartidas(origen)
        val hojas = PaqueteOoxml.listar(origen, "xl/worksheets/sheet")
            .filter { it.endsWith(".xml") }
        if (hojas.isEmpty()) error("El .xlsx no contiene ninguna hoja")

        val tablas = hojas.mapNotNull { ruta ->
            PaqueteOoxml.leerTexto(origen, ruta)?.let { leerHoja(it, compartidas) }
        }.filter { it.isNotEmpty() }

        escribirTablas(destino, tablas)
    }

    /** Excel guarda los textos repetidos en una tabla aparte para no duplicarlos. */
    private fun cadenasCompartidas(origen: File): List<String> {
        val xml = PaqueteOoxml.leerTexto(origen, "xl/sharedStrings.xml") ?: return emptyList()
        return XmlPlano.bloques(xml, "si").map { XmlPlano.textoDe(it, "t") }
    }

    private fun leerHoja(xml: String, compartidas: List<String>): List<List<String>> =
        XmlPlano.bloques(xml, "row").map { fila ->
            XmlPlano.bloquesConAtributos(fila, "c").map { (atributos, contenido) ->
                val valor = XmlPlano.bloques(contenido, "v").firstOrNull().orEmpty()
                when (XmlPlano.atributo(atributos, "t")) {
                    // "s" = indice en la tabla de cadenas compartidas.
                    "s" -> valor.toIntOrNull()?.let { compartidas.getOrNull(it) }.orEmpty()
                    // "inlineStr" = el texto va dentro de la propia celda.
                    "inlineStr" -> XmlPlano.textoDe(contenido, "t")
                    else -> PaqueteOoxml.desescapar(valor)
                }
            }
        }.filter { fila -> fila.any { it.isNotBlank() } }

    private fun escribirTablas(destino: File, tablas: List<List<List<String>>>) {
        PDDocument().use { documento ->
            val fuentes = FuentesPdf(documento)
            // Apaisado: una hoja de calculo tiene mas ancho que alto.
            val caja = PDRectangle(TAMANO_PAGINA.height, TAMANO_PAGINA.width)

            tablas.forEach { filas ->
                val columnas = filas.maxOf { it.size }
                val anchoUtil = caja.width - MARGEN * 2
                val anchoColumna = anchoUtil / max(1, columnas)
                val tamanoTexto = min(10f, max(6f, anchoColumna / 7f))
                val altoFila = tamanoTexto * 2f

                var pagina = nuevaPagina(documento, caja)
                var flujo = PDPageContentStream(documento, pagina)
                var y = caja.height - MARGEN

                filas.forEachIndexed { indiceFila, fila ->
                    if (y - altoFila < MARGEN) {
                        flujo.close()
                        pagina = nuevaPagina(documento, caja)
                        flujo = PDPageContentStream(documento, pagina)
                        y = caja.height - MARGEN
                    }

                    // La primera fila se marca como cabecera: en la practica casi
                    // siempre lo es, y ayuda a leer la tabla.
                    val esCabecera = indiceFila == 0
                    if (esCabecera) {
                        flujo.setNonStrokingColor(0.90f, 0.92f, 0.96f)
                        flujo.addRect(MARGEN, y - altoFila, anchoUtil, altoFila)
                        flujo.fill()
                        flujo.setNonStrokingColor(0f, 0f, 0f)
                    }

                    flujo.setStrokingColor(0.75f, 0.75f, 0.78f)
                    flujo.setLineWidth(0.4f)
                    flujo.addRect(MARGEN, y - altoFila, anchoUtil, altoFila)
                    flujo.stroke()

                    fila.forEachIndexed { indiceColumna, celda ->
                        if (celda.isBlank()) return@forEachIndexed
                        val x = MARGEN + indiceColumna * anchoColumna
                        flujo.moveTo(x, y - altoFila)
                        flujo.lineTo(x, y)
                        flujo.stroke()

                        val fuente = fuenteQueSirva(fuentes, celda, esCabecera, cursiva = false)
                        val recortado = recortarAlAncho(
                            celda, fuente, tamanoTexto, anchoColumna - 6f,
                        )
                        flujo.beginText()
                        flujo.setFont(fuente, tamanoTexto)
                        flujo.newLineAtOffset(x + 3f, y - altoFila + tamanoTexto * 0.6f)
                        runCatching { flujo.showText(recortado) }
                        flujo.endText()
                    }
                    y -= altoFila
                }
                flujo.close()
            }

            if (documento.numberOfPages == 0) nuevaPagina(documento, caja)
            documento.save(destino)
        }
    }

    // --- PPTX ----------------------------------------------------------------

    fun pptxAPdf(origen: File, destino: File) {
        val diapositivas = PaqueteOoxml.listar(origen, "ppt/slides/slide")
            .filter { it.endsWith(".xml") }
        if (diapositivas.isEmpty()) error("El .pptx no contiene diapositivas")

        PDDocument().use { documento ->
            val fuentes = FuentesPdf(documento)

            diapositivas.forEach { rutaDiapositiva ->
                val xml = PaqueteOoxml.leerTexto(origen, rutaDiapositiva) ?: return@forEach
                val pagina = nuevaPagina(documento, TAMANO_DIAPOSITIVA)

                PDPageContentStream(documento, pagina).use { flujo ->
                    dibujarImagenesDeDiapositiva(origen, rutaDiapositiva, xml, documento, flujo)
                    dibujarTextosDeDiapositiva(xml, fuentes, flujo)
                }
            }

            if (documento.numberOfPages == 0) nuevaPagina(documento, TAMANO_DIAPOSITIVA)
            documento.save(destino)
        }
    }

    private fun dibujarTextosDeDiapositiva(
        xml: String,
        fuentes: FuentesPdf,
        flujo: PDPageContentStream,
    ) {
        // Cada `p:sp` es una forma con su cuadro de texto y su posicion.
        XmlPlano.bloques(xml, "p:sp").forEach { forma ->
            val marco = marcoDeForma(forma) ?: MarcoPt(
                MARGEN,
                TAMANO_DIAPOSITIVA.height - MARGEN - 120f,
                TAMANO_DIAPOSITIVA.width - MARGEN * 2,
                120f,
            )

            var y = marco.arriba
            XmlPlano.bloques(forma, "a:p").forEach { parrafo ->
                val texto = XmlPlano.textoDe(parrafo, "a:t")
                if (texto.isBlank()) {
                    y -= 14f
                    return@forEach
                }

                val propiedades = XmlPlano.atributosDe(parrafo, "a:rPr").firstOrNull().orEmpty()
                val tamano = XmlPlano.atributo(propiedades, "sz")?.toFloatOrNull()
                    ?.div(100f) // sz va en centesimas de punto
                    ?: 18f
                val negrita = XmlPlano.atributo(propiedades, "b") == "1"

                val fuente = fuenteQueSirva(fuentes, texto, negrita, cursiva = false)
                repartirEnLineas(texto, fuente, tamano, marco.ancho).forEach { linea ->
                    if (y < 12f) return@forEach
                    flujo.beginText()
                    flujo.setFont(fuente, tamano)
                    flujo.newLineAtOffset(marco.izquierda, y - tamano)
                    runCatching { flujo.showText(linea) }
                    flujo.endText()
                    y -= tamano * INTERLINEADO
                }
            }
        }
    }

    private fun dibujarImagenesDeDiapositiva(
        origen: File,
        rutaDiapositiva: String,
        xml: String,
        documento: PDDocument,
        flujo: PDPageContentStream,
    ) {
        val nombre = rutaDiapositiva.substringAfterLast('/')
        val relaciones = PaqueteOoxml.leerTexto(origen, "ppt/slides/_rels/$nombre.rels")
            ?: return

        // r:embed apunta a un identificador de relacion, y el fichero .rels dice
        // que imagen del paquete es cada identificador.
        val destinoPorId = Regex("Id=\"([^\"]+)\"[^>]*Target=\"([^\"]+)\"")
            .findAll(relaciones)
            .associate { it.groupValues[1] to it.groupValues[2] }

        XmlPlano.bloques(xml, "p:pic").forEach { imagenXml ->
            val id = Regex("r:embed=\"([^\"]+)\"").find(imagenXml)?.groupValues?.get(1)
                ?: return@forEach
            val objetivo = destinoPorId[id] ?: return@forEach
            val rutaImagen = "ppt/" + objetivo.removePrefix("../")
            val bytes = PaqueteOoxml.leerBytes(origen, rutaImagen) ?: return@forEach
            val mapa = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@forEach

            val marco = marcoDeForma(imagenXml) ?: MarcoPt(
                MARGEN,
                TAMANO_DIAPOSITIVA.height - MARGEN,
                TAMANO_DIAPOSITIVA.width - MARGEN * 2,
                TAMANO_DIAPOSITIVA.height - MARGEN * 2,
            )

            runCatching {
                val objeto = LosslessFactory.createFromImage(documento, mapa)
                flujo.drawImage(
                    objeto,
                    marco.izquierda,
                    marco.arriba - marco.alto,
                    marco.ancho,
                    marco.alto,
                )
            }
            mapa.recycle()
        }
    }

    /**
     * Posicion y tamano de una forma, leidos de su `a:xfrm`.
     *
     * `a:off` y `a:ext` se escriben siempre cerradas sobre si mismas, de ahi que
     * se lean con `atributosDe` y no buscando un bloque con contenido.
     */
    private fun marcoDeForma(forma: String): MarcoPt? {
        val transformacion = XmlPlano.bloques(forma, "a:xfrm").firstOrNull() ?: return null
        val desplazamiento = XmlPlano.atributosDe(transformacion, "a:off").firstOrNull().orEmpty()
        val extension = XmlPlano.atributosDe(transformacion, "a:ext").firstOrNull().orEmpty()

        val x = XmlPlano.atributo(desplazamiento, "x")?.toFloatOrNull()
        val y = XmlPlano.atributo(desplazamiento, "y")?.toFloatOrNull()
        val ancho = XmlPlano.atributo(extension, "cx")?.toFloatOrNull()
        val alto = XmlPlano.atributo(extension, "cy")?.toFloatOrNull()
        if (x == null || y == null || ancho == null || alto == null) return null

        // OOXML mide en EMU y desde arriba; el PDF, en puntos y desde abajo.
        return MarcoPt(
            izquierda = x / EMU_POR_PUNTO,
            arriba = TAMANO_DIAPOSITIVA.height - y / EMU_POR_PUNTO,
            ancho = ancho / EMU_POR_PUNTO,
            alto = alto / EMU_POR_PUNTO,
        )
    }

    private data class MarcoPt(
        val izquierda: Float,
        val arriba: Float,
        val ancho: Float,
        val alto: Float,
    )

    // --- Utilidades comunes --------------------------------------------------

    private fun nuevaPagina(documento: PDDocument, caja: PDRectangle): PDPage =
        PDPage(caja).also { documento.addPage(it) }

    /**
     * Elige una fuente capaz de escribir el texto.
     *
     * Si ninguna lo cubre (arabe, escrituras que necesitan conformado) se cae a
     * la estandar: el texto saldra imperfecto, pero el documento se genera. Es
     * mejor que abortar la conversion entera por una linea.
     */
    private fun fuenteQueSirva(
        fuentes: FuentesPdf,
        texto: String,
        negrita: Boolean,
        cursiva: Boolean,
    ): PDFont = when (val eleccion = fuentes.elegir(texto, negrita, cursiva)) {
        is FuentesPdf.Eleccion.Vectorial -> eleccion.fuente
        FuentesPdf.Eleccion.Rasterizar ->
            (fuentes.elegir("", negrita, cursiva) as FuentesPdf.Eleccion.Vectorial).fuente
    }

    private fun anchoDe(fuente: PDFont, texto: String, tamano: Float): Float =
        runCatching { fuente.getStringWidth(texto) / 1000f * tamano }
            .getOrElse { texto.length * tamano * 0.5f }

    private fun repartirEnLineas(
        texto: String,
        fuente: PDFont,
        tamano: Float,
        anchoDisponible: Float,
    ): List<String> {
        val resultado = mutableListOf<String>()
        texto.split('\n').forEach { parrafo ->
            var linea = StringBuilder()
            parrafo.split(' ').forEach { palabra ->
                val candidata = if (linea.isEmpty()) palabra else "$linea $palabra"
                if (anchoDe(fuente, candidata, tamano) <= anchoDisponible || linea.isEmpty()) {
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

    private fun recortarAlAncho(
        texto: String,
        fuente: PDFont,
        tamano: Float,
        anchoDisponible: Float,
    ): String {
        if (anchoDe(fuente, texto, tamano) <= anchoDisponible) return texto
        var recortado = texto
        while (recortado.length > 1 &&
            anchoDe(fuente, "$recortado…", tamano) > anchoDisponible
        ) {
            recortado = recortado.dropLast(1)
        }
        return "$recortado…"
    }
}
