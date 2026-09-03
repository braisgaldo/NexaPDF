package es.ghatostudio.nexapdf.pdf

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import es.ghatostudio.nexapdf.domain.pdf.FormatoDocumento
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import es.ghatostudio.nexapdf.pdf.ofimatica.PaqueteOoxml
import es.ghatostudio.nexapdf.pdf.ofimatica.XmlPlano
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Conversion entre PDF y los formatos de ofimatica, probada en el dispositivo.
 *
 * Cada prueba fabrica su propio .docx, .xlsx o .pptx escribiendo el ZIP y el XML
 * a mano, que es exactamente lo que son esos formatos. Asi lo que se prueba es
 * un paquete OOXML real y la prueba no depende de tener ficheros preparados en
 * el telefono.
 *
 * Lo que se comprueba es que el **contenido** sobrevive al viaje: los textos,
 * las celdas y el numero de paginas. La fidelidad de maquetacion no se prueba
 * porque no se promete: ver la documentacion de ConversorDocumentos.
 */
@RunWith(AndroidJUnit4::class)
class ConversorDocumentosAndroidTest {

    private lateinit var trabajo: File
    private lateinit var conversor: ConversorDocumentosAndroid

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        trabajo = File(contexto.cacheDir, "pruebas-conversor").apply {
            deleteRecursively()
            mkdirs()
        }
        conversor = ConversorDocumentosAndroid(MotorPdfAndroid(trabajo.absolutePath))
    }

    private fun <T> exito(resultado: ResultadoPdf<T>): T {
        assertTrue("Se esperaba exito y llego $resultado", resultado is ResultadoPdf.Exito)
        return (resultado as ResultadoPdf.Exito).valor
    }

    private fun textoDelPdf(fichero: File): String =
        PDDocument.load(fichero).use { PDFTextStripper().getText(it) }

    private fun paginasDe(fichero: File): Int =
        PDDocument.load(fichero).use { it.numberOfPages }

    // --- Formatos ------------------------------------------------------------

    @Test
    fun reconoce_el_formato_por_la_extension() {
        assertEquals(FormatoDocumento.PDF, FormatoDocumento.desdeNombre("informe.pdf"))
        assertEquals(FormatoDocumento.DOCX, FormatoDocumento.desdeNombre("Informe final.docx"))
        assertEquals(FormatoDocumento.XLSX, FormatoDocumento.desdeNombre("cuentas.XLSX"))
        assertEquals(FormatoDocumento.PPTX, FormatoDocumento.desdeNombre("charla.pptx"))
        assertEquals(FormatoDocumento.IMAGEN, FormatoDocumento.desdeNombre("foto.JPEG"))
        assertEquals(FormatoDocumento.IMAGEN, FormatoDocumento.desdeNombre("captura.png"))
        assertEquals(null, FormatoDocumento.desdeNombre("notas.txt"))
        assertEquals(null, FormatoDocumento.desdeNombre("sin_extension"))
    }

    // --- Ofimatica a PDF -----------------------------------------------------

    @Test
    fun un_docx_se_convierte_conservando_su_texto() = runBlocking {
        val docx = crearDocx(
            "entrada.docx",
            listOf(
                "Informe de prueba" to true,
                "Primera linea del cuerpo con acentos: cafe, nino, accion." to false,
                "Segunda linea, mas larga, para que el ajuste de linea tenga que " +
                    "repartirla en varias filas dentro del ancho de la pagina." to false,
            ),
        )
        val salida = File(trabajo, "desde-docx.pdf")

        exito(conversor.aPdf(docx.absolutePath, salida.absolutePath))

        val texto = textoDelPdf(salida)
        assertTrue("Falta el titulo", texto.contains("Informe de prueba"))
        assertTrue("Falta el cuerpo", texto.contains("cafe, nino, accion"))
        assertTrue("Falta la linea larga", texto.contains("repartirla"))
        assertTrue(paginasDe(salida) >= 1)
    }

    @Test
    fun un_xlsx_se_convierte_en_una_tabla() = runBlocking {
        val filas = listOf(
            listOf("Concepto", "Unidades", "Total"),
            listOf("Teclado", "2", "179,80"),
            listOf("Monitor", "1", "249,00"),
        )
        val xlsx = crearXlsx("entrada.xlsx", filas)
        val salida = File(trabajo, "desde-xlsx.pdf")

        exito(conversor.aPdf(xlsx.absolutePath, salida.absolutePath))

        val texto = textoDelPdf(salida)
        filas.flatten().forEach { celda ->
            assertTrue("Falta la celda \"$celda\"", texto.contains(celda))
        }
    }

    @Test
    fun un_pptx_da_una_pagina_por_diapositiva() = runBlocking {
        val diapositivas = listOf(
            "Primera diapositiva" to "Contenido de la primera",
            "Segunda diapositiva" to "Contenido de la segunda",
            "Tercera diapositiva" to "Contenido de la tercera",
        )
        val pptx = crearPptx("entrada.pptx", diapositivas)
        val salida = File(trabajo, "desde-pptx.pdf")

        exito(conversor.aPdf(pptx.absolutePath, salida.absolutePath))

        assertEquals(3, paginasDe(salida))
        val texto = textoDelPdf(salida)
        assertTrue(texto.contains("Primera diapositiva"))
        assertTrue(texto.contains("Contenido de la tercera"))
    }

    @Test
    fun una_imagen_se_convierte_en_un_pdf_de_una_pagina() = runBlocking {
        val imagen = File(trabajo, "foto.png")
        val mapa = android.graphics.Bitmap.createBitmap(
            600, 400, android.graphics.Bitmap.Config.ARGB_8888,
        ).apply { eraseColor(android.graphics.Color.rgb(40, 90, 180)) }
        imagen.outputStream().use {
            mapa.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        mapa.recycle()

        val salida = File(trabajo, "desde-imagen.pdf")
        exito(conversor.aPdf(imagen.absolutePath, salida.absolutePath))

        assertEquals(1, paginasDe(salida))
        PDDocument.load(salida).use { documento ->
            val caja = documento.getPage(0).mediaBox
            // Con "ajustar a la imagen" la pagina toma la proporcion de la foto.
            assertEquals(600f / 400f, caja.width / caja.height, 0.03f)
        }
    }

    @Test
    fun un_pdf_pasado_a_pdf_se_devuelve_tal_cual() = runBlocking {
        val pdf = crearPdfSencillo("ya-es-pdf.pdf", "Contenido")
        val resultado = exito(conversor.aPdf(pdf.absolutePath, File(trabajo, "otro.pdf").absolutePath))
        assertEquals(pdf.absolutePath, resultado)
    }

    @Test
    fun un_docx_corrupto_falla_sin_romper_nada() = runBlocking {
        val falso = File(trabajo, "roto.docx").apply { writeText("esto no es un ZIP") }
        val resultado = conversor.aPdf(falso.absolutePath, File(trabajo, "roto.pdf").absolutePath)
        assertTrue(resultado is ResultadoPdf.Fallo)
    }

    // --- PDF a ofimatica -----------------------------------------------------

    @Test
    fun un_pdf_se_exporta_a_docx_con_su_texto() = runBlocking {
        val pdf = crearPdfSencillo("origen.pdf", "Texto que debe llegar al documento")
        val salida = File(trabajo, "salida.docx")

        exito(conversor.desdePdf(pdf.absolutePath, FormatoDocumento.DOCX, salida.absolutePath))

        val documento = PaqueteOoxml.leerTexto(salida, "word/document.xml")
        assertNotNull("El .docx no tiene word/document.xml", documento)
        val texto = XmlPlano.textoDe(documento!!, "w:t")
        assertTrue("Falta el texto original", texto.contains("Texto que debe llegar"))
        // Un paquete valido necesita su tabla de tipos y sus relaciones.
        assertNotNull(PaqueteOoxml.leerTexto(salida, "[Content_Types].xml"))
        assertNotNull(PaqueteOoxml.leerTexto(salida, "_rels/.rels"))
    }

    @Test
    fun un_pdf_se_exporta_a_xlsx_con_sus_celdas() = runBlocking {
        val pdf = crearPdfSencillo("tabla.pdf", "Concepto  Unidades  Total")
        val salida = File(trabajo, "salida.xlsx")

        exito(conversor.desdePdf(pdf.absolutePath, FormatoDocumento.XLSX, salida.absolutePath))

        val cadenas = PaqueteOoxml.leerTexto(salida, "xl/sharedStrings.xml")
        assertNotNull("El .xlsx no tiene sharedStrings.xml", cadenas)
        assertTrue(cadenas!!.contains("Concepto"))
        assertNotNull(PaqueteOoxml.leerTexto(salida, "xl/worksheets/sheet1.xml"))
        assertNotNull(PaqueteOoxml.leerTexto(salida, "xl/workbook.xml"))
    }

    @Test
    fun un_pdf_se_exporta_a_pptx_con_una_diapositiva_por_pagina() = runBlocking {
        val pdf = crearPdfDeVariasPaginas("presentar.pdf", 3)
        val salida = File(trabajo, "salida.pptx")

        exito(conversor.desdePdf(pdf.absolutePath, FormatoDocumento.PPTX, salida.absolutePath))

        val diapositivas = PaqueteOoxml.listar(salida, "ppt/slides/slide")
            .filter { it.endsWith(".xml") }
        assertEquals(3, diapositivas.size)

        // Cada diapositiva lleva su imagen y su relacion apuntando a ella.
        (1..3).forEach { numero ->
            assertNotNull(
                "Falta la imagen de la diapositiva $numero",
                PaqueteOoxml.leerBytes(salida, "ppt/media/image$numero.png"),
            )
            val relaciones = PaqueteOoxml.leerTexto(salida, "ppt/slides/_rels/slide$numero.xml.rels")
            assertTrue(relaciones!!.contains("image$numero.png"))
        }
        // PowerPoint exige patron, diseno y tema aunque no se usen.
        assertNotNull(PaqueteOoxml.leerTexto(salida, "ppt/slideMasters/slideMaster1.xml"))
        assertNotNull(PaqueteOoxml.leerTexto(salida, "ppt/slideLayouts/slideLayout1.xml"))
        assertNotNull(PaqueteOoxml.leerTexto(salida, "ppt/theme/theme1.xml"))
    }

    @Test
    fun el_viaje_de_ida_y_vuelta_conserva_el_texto() = runBlocking {
        // docx -> pdf -> docx. El formato se pierde, el texto no.
        val original = crearDocx(
            "ida.docx",
            listOf("Titulo del informe" to true, "Cuerpo con datos importantes." to false),
        )
        val intermedio = File(trabajo, "intermedio.pdf")
        val vuelta = File(trabajo, "vuelta.docx")

        exito(conversor.aPdf(original.absolutePath, intermedio.absolutePath))
        exito(conversor.desdePdf(intermedio.absolutePath, FormatoDocumento.DOCX, vuelta.absolutePath))

        val texto = XmlPlano.textoDe(
            PaqueteOoxml.leerTexto(vuelta, "word/document.xml")!!,
            "w:t",
        )
        assertTrue("Se perdio el titulo", texto.contains("Titulo del informe"))
        assertTrue("Se perdio el cuerpo", texto.contains("datos importantes"))
    }

    @Test
    fun los_paquetes_generados_son_zip_validos() = runBlocking {
        val pdf = crearPdfDeVariasPaginas("valido.pdf", 2)

        listOf(
            FormatoDocumento.DOCX to "v.docx",
            FormatoDocumento.XLSX to "v.xlsx",
            FormatoDocumento.PPTX to "v.pptx",
        ).forEach { (formato, nombre) ->
            val salida = File(trabajo, nombre)
            exito(conversor.desdePdf(pdf.absolutePath, formato, salida.absolutePath))

            // Que se pueda abrir como ZIP y tenga la tabla de tipos es lo minimo
            // que exige cualquier programa de ofimatica para no darlo por roto.
            ZipFile(salida).use { zip ->
                assertNotNull(
                    "$nombre no tiene [Content_Types].xml",
                    zip.getEntry("[Content_Types].xml"),
                )
                assertTrue("$nombre esta vacio", zip.size() > 1)
            }
        }
    }

    // --- Construccion de ficheros de prueba ----------------------------------

    private fun crearPdfSencillo(nombre: String, texto: String): File =
        crearPdfConLineas(nombre, listOf(texto))

    private fun crearPdfDeVariasPaginas(nombre: String, paginas: Int): File {
        val fichero = File(trabajo, nombre)
        PDDocument().use { documento ->
            repeat(paginas) { indice ->
                val pagina = com.tom_roush.pdfbox.pdmodel.PDPage(
                    com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4,
                )
                documento.addPage(pagina)
                com.tom_roush.pdfbox.pdmodel.PDPageContentStream(documento, pagina).use { flujo ->
                    flujo.beginText()
                    flujo.setFont(
                        com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA,
                        24f,
                    )
                    flujo.newLineAtOffset(72f, 700f)
                    flujo.showText("Pagina ${indice + 1}")
                    flujo.endText()
                }
            }
            documento.save(fichero)
        }
        return fichero
    }

    private fun crearPdfConLineas(nombre: String, lineas: List<String>): File {
        val fichero = File(trabajo, nombre)
        PDDocument().use { documento ->
            val pagina = com.tom_roush.pdfbox.pdmodel.PDPage(
                com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4,
            )
            documento.addPage(pagina)
            com.tom_roush.pdfbox.pdmodel.PDPageContentStream(documento, pagina).use { flujo ->
                var y = 720f
                lineas.forEach { linea ->
                    flujo.beginText()
                    flujo.setFont(
                        com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA,
                        16f,
                    )
                    flujo.newLineAtOffset(60f, y)
                    flujo.showText(linea)
                    flujo.endText()
                    y -= 28f
                }
            }
            documento.save(fichero)
        }
        return fichero
    }

    private fun escribirZip(nombre: String, partes: Map<String, String>): File {
        val fichero = File(trabajo, nombre)
        ZipOutputStream(fichero.outputStream().buffered()).use { zip ->
            partes.forEach { (ruta, contenido) ->
                zip.putNextEntry(ZipEntry(ruta))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return fichero
    }

    private fun crearDocx(nombre: String, parrafos: List<Pair<String, Boolean>>): File {
        val cuerpo = parrafos.joinToString("") { (texto, negrita) ->
            val formato = "<w:rPr>" + (if (negrita) "<w:b/>" else "") + "<w:sz w:val=\"24\"/></w:rPr>"
            "<w:p><w:r>$formato<w:t xml:space=\"preserve\">$texto</w:t></w:r></w:p>"
        }
        return escribirZip(
            nombre,
            mapOf(
                "[Content_Types].xml" to
                    "$CABECERA<Types xmlns=\"$NS_TIPOS\">" +
                    "<Default Extension=\"rels\" ContentType=\"$TIPO_RELACIONES\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "</Types>",
                "_rels/.rels" to relacionesRaiz("word/document.xml"),
                "word/document.xml" to
                    "$CABECERA<w:document xmlns:w=\"$NS_WORD\"><w:body>$cuerpo</w:body></w:document>",
            ),
        )
    }

    private fun crearXlsx(nombre: String, filas: List<List<String>>): File {
        val textos = filas.flatten().filter { it.isNotBlank() }.distinct()
        val indices = textos.withIndex().associate { (indice, texto) -> texto to indice }

        val filasXml = filas.mapIndexed { numeroFila, fila ->
            val celdas = fila.mapIndexed { numeroColumna, celda ->
                if (celda.isBlank()) {
                    ""
                } else {
                    val letra = 'A' + numeroColumna
                    "<c r=\"$letra${numeroFila + 1}\" t=\"s\"><v>${indices[celda]}</v></c>"
                }
            }.joinToString("")
            "<row r=\"${numeroFila + 1}\">$celdas</row>"
        }.joinToString("")

        return escribirZip(
            nombre,
            mapOf(
                "[Content_Types].xml" to
                    "$CABECERA<Types xmlns=\"$NS_TIPOS\">" +
                    "<Default Extension=\"rels\" ContentType=\"$TIPO_RELACIONES\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "</Types>",
                "_rels/.rels" to relacionesRaiz("xl/workbook.xml"),
                "xl/workbook.xml" to
                    "$CABECERA<workbook xmlns=\"$NS_EXCEL\"><sheets>" +
                    "<sheet name=\"Hoja\" sheetId=\"1\"/></sheets></workbook>",
                "xl/sharedStrings.xml" to
                    "$CABECERA<sst xmlns=\"$NS_EXCEL\" count=\"${textos.size}\">" +
                    textos.joinToString("") { "<si><t>$it</t></si>" } + "</sst>",
                "xl/worksheets/sheet1.xml" to
                    "$CABECERA<worksheet xmlns=\"$NS_EXCEL\"><sheetData>$filasXml</sheetData></worksheet>",
            ),
        )
    }

    private fun crearPptx(nombre: String, diapositivas: List<Pair<String, String>>): File {
        val partes = mutableMapOf<String, String>()

        diapositivas.forEachIndexed { indice, (titulo, cuerpo) ->
            partes["ppt/slides/slide${indice + 1}.xml"] =
                "$CABECERA<p:sld xmlns:a=\"$NS_DIBUJO\" xmlns:r=\"$NS_REL\" xmlns:p=\"$NS_PPT\">" +
                    "<p:cSld><p:spTree>" +
                    formaConTexto(2, titulo, 4000, 609600) +
                    formaConTexto(3, cuerpo, 2000, 2133600) +
                    "</p:spTree></p:cSld></p:sld>"
        }

        partes["ppt/presentation.xml"] =
            "$CABECERA<p:presentation xmlns:a=\"$NS_DIBUJO\" xmlns:r=\"$NS_REL\" " +
                "xmlns:p=\"$NS_PPT\"><p:sldSz cx=\"9144000\" cy=\"6858000\"/></p:presentation>"
        partes["_rels/.rels"] = relacionesRaiz("ppt/presentation.xml")
        partes["[Content_Types].xml"] =
            "$CABECERA<Types xmlns=\"$NS_TIPOS\">" +
                "<Default Extension=\"rels\" ContentType=\"$TIPO_RELACIONES\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/></Types>"

        return escribirZip(nombre, partes)
    }

    private fun formaConTexto(id: Int, texto: String, tamano: Int, y: Int): String =
        "<p:sp><p:nvSpPr><p:cNvPr id=\"$id\" name=\"F$id\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>" +
            "<p:spPr><a:xfrm><a:off x=\"685800\" y=\"$y\"/>" +
            "<a:ext cx=\"7772400\" cy=\"1143000\"/></a:xfrm></p:spPr>" +
            "<p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r>" +
            "<a:rPr lang=\"es-ES\" sz=\"$tamano\"/><a:t>$texto</a:t></a:r></a:p></p:txBody></p:sp>"

    private fun relacionesRaiz(destino: String): String =
        "$CABECERA<Relationships xmlns=\"$NS_PAQUETE_REL\">" +
            "<Relationship Id=\"rId1\" Type=\"$NS_REL/officeDocument\" Target=\"$destino\"/>" +
            "</Relationships>"

    companion object {
        private const val CABECERA =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        private const val NS_TIPOS =
            "http://schemas.openxmlformats.org/package/2006/content-types"
        private const val TIPO_RELACIONES =
            "application/vnd.openxmlformats-package.relationships+xml"
        private const val NS_PAQUETE_REL =
            "http://schemas.openxmlformats.org/package/2006/relationships"
        private const val NS_REL =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        private const val NS_WORD =
            "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
        private const val NS_EXCEL =
            "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        private const val NS_PPT =
            "http://schemas.openxmlformats.org/presentationml/2006/main"
        private const val NS_DIBUJO =
            "http://schemas.openxmlformats.org/drawingml/2006/main"

        @JvmStatic
        @BeforeClass
        fun cargarPdfBox() {
            PDFBoxResourceLoader.init(
                ApplicationProvider.getApplicationContext<android.content.Context>(),
            )
        }
    }
}
