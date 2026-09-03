package es.ghatostudio.nexapdf.pdf.ofimatica

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Exporta un PDF a los formatos de ofimatica.
 *
 * Un PDF no guarda parrafos, ni celdas, ni diapositivas: guarda letras colocadas
 * en coordenadas sobre una pagina. Todo lo que se recupera aqui se deduce de
 * esas posiciones, y cada formato lo aprovecha de una manera:
 *
 *  - **DOCX**: el texto, respetando lineas y saltos de pagina. Es la conversion
 *    mas fiel, porque un documento de texto es justo lo que un PDF de texto
 *    contiene.
 *  - **XLSX**: las lineas se agrupan en filas por su altura y en columnas por su
 *    posicion horizontal. Funciona bien con tablas alineadas y regular con
 *    tablas irregulares; no hay forma de acertar siempre sin adivinar.
 *  - **PPTX**: una diapositiva por pagina, con la pagina como imagen. Es la que
 *    conserva el aspecto exacto, a cambio de que el texto deja de ser editable.
 */
class PdfAOfimatica {

    private companion object {
        /** Columnas minimas para considerar que una fila es de tabla. */
        const val MINIMO_COLUMNAS = 2

        /** Filas seguidas con la misma forma para llamarlo tabla. */
        const val MINIMO_FILAS_TABLA = 3

        /** Ancho util de un A4 con los margenes de esta clase, en vigesimas. */
        const val ANCHO_UTIL_TWIPS = 9638

        /** Puntos de separacion vertical a partir de los cuales hay otra fila. */
        const val TOLERANCIA_FILA = 6f

        /** Puntos de separacion horizontal que se consideran otra columna. */
        const val SEPARACION_COLUMNA = 18f

        /** Resolucion a la que se rasteriza cada pagina para el .pptx. */
        const val ANCHO_DIAPOSITIVA_PX = 1600
    }

    // --- DOCX ----------------------------------------------------------------

    fun aDocx(origen: File, destino: File, titulo: String) {
        val paginas = filasPorPagina(origen)

        val cuerpo = buildString {
            paginas.forEachIndexed { indice, filas ->
                if (indice > 0) {
                    // Salto de pagina explicito entre paginas del PDF original.
                    append(
                        "<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>",
                    )
                }
                if (filas.isEmpty()) {
                    append("<w:p/>")
                    return@forEachIndexed
                }
                agruparEnBloques(filas).forEach { bloque ->
                    when (bloque) {
                        is Bloque.Parrafos -> bloque.lineas.forEach { linea ->
                            append("<w:p><w:r><w:t xml:space=\"preserve\">")
                            append(PaqueteOoxml.escapar(linea))
                            append("</w:t></w:r></w:p>")
                        }

                        is Bloque.Tabla -> append(tablaWord(bloque.filas))
                    }
                }
            }
        }

        val documento = """
            ${PaqueteOoxml.CABECERA_XML}
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>$cuerpo
                <w:sectPr>
                  <w:pgSz w:w="11906" w:h="16838"/>
                  <w:pgMar w:top="1134" w:right="1134" w:bottom="1134" w:left="1134"/>
                </w:sectPr>
              </w:body>
            </w:document>
        """.trimIndent()

        PaqueteOoxml.escribir(
            destino,
            listOf(
                PaqueteOoxml.Entrada("[Content_Types].xml", tiposDeContenido(TIPOS_DOCX)),
                PaqueteOoxml.Entrada("_rels/.rels", relacionesRaiz("word/document.xml", TIPO_DOCUMENTO_WORD)),
                PaqueteOoxml.Entrada("docProps/core.xml", propiedades(titulo)),
                PaqueteOoxml.Entrada("word/document.xml", documento),
            ),
        )
    }

    // --- XLSX ----------------------------------------------------------------

    fun aXlsx(origen: File, destino: File, titulo: String) {
        val filas = filasDetectadas(origen)

        // Excel guarda los textos en una tabla compartida y las celdas apuntan a
        // ella por indice. Con documentos que repiten cabeceras esto ahorra
        // bastante espacio, y es como lo escribe el propio Excel.
        val compartidas = LinkedHashMap<String, Int>()
        filas.flatten().forEach { celda ->
            if (celda.isNotBlank()) compartidas.getOrPut(celda) { compartidas.size }
        }

        val filasXml = buildString {
            filas.forEachIndexed { indiceFila, fila ->
                append("<row r=\"${indiceFila + 1}\">")
                fila.forEachIndexed { indiceColumna, celda ->
                    if (celda.isBlank()) return@forEachIndexed
                    val referencia = "${letraDeColumna(indiceColumna)}${indiceFila + 1}"
                    append("<c r=\"$referencia\" t=\"s\"><v>${compartidas[celda]}</v></c>")
                }
                append("</row>")
            }
        }

        val hoja = """
            ${PaqueteOoxml.CABECERA_XML}
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>$filasXml</sheetData>
            </worksheet>
        """.trimIndent()

        val cadenas = buildString {
            append(PaqueteOoxml.CABECERA_XML)
            append(
                "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                    "count=\"${compartidas.size}\" uniqueCount=\"${compartidas.size}\">",
            )
            compartidas.keys.forEach { texto ->
                append("<si><t xml:space=\"preserve\">${PaqueteOoxml.escapar(texto)}</t></si>")
            }
            append("</sst>")
        }

        val libro = """
            ${PaqueteOoxml.CABECERA_XML}
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets><sheet name="${PaqueteOoxml.escapar(titulo.take(28))}" sheetId="1" r:id="rId1"/></sheets>
            </workbook>
        """.trimIndent()

        val relacionesLibro = """
            ${PaqueteOoxml.CABECERA_XML}
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="$TIPO_HOJA" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Type="$TIPO_CADENAS" Target="sharedStrings.xml"/>
            </Relationships>
        """.trimIndent()

        PaqueteOoxml.escribir(
            destino,
            listOf(
                PaqueteOoxml.Entrada("[Content_Types].xml", tiposDeContenido(TIPOS_XLSX)),
                PaqueteOoxml.Entrada("_rels/.rels", relacionesRaiz("xl/workbook.xml", TIPO_DOCUMENTO_EXCEL)),
                PaqueteOoxml.Entrada("docProps/core.xml", propiedades(titulo)),
                PaqueteOoxml.Entrada("xl/workbook.xml", libro),
                PaqueteOoxml.Entrada("xl/_rels/workbook.xml.rels", relacionesLibro),
                PaqueteOoxml.Entrada("xl/sharedStrings.xml", cadenas),
                PaqueteOoxml.Entrada("xl/worksheets/sheet1.xml", hoja),
            ),
        )
    }

    private fun letraDeColumna(indice: Int): String {
        var restante = indice
        val letras = StringBuilder()
        do {
            letras.insert(0, ('A' + restante % 26))
            restante = restante / 26 - 1
        } while (restante >= 0)
        return letras.toString()
    }

    // --- PPTX ----------------------------------------------------------------

    fun aPptx(origen: File, destino: File, titulo: String) {
        val imagenes = rasterizarPaginas(origen)
        if (imagenes.isEmpty()) error("El PDF no tiene paginas que exportar")

        val entradas = mutableListOf<PaqueteOoxml.Entrada>()
        val idsDiapositiva = imagenes.indices.map { "rId${it + 2}" }

        entradas += PaqueteOoxml.Entrada("[Content_Types].xml", tiposDeContenidoPptx(imagenes.size))
        entradas += PaqueteOoxml.Entrada(
            "_rels/.rels",
            relacionesRaiz("ppt/presentation.xml", TIPO_DOCUMENTO_POWERPOINT),
        )
        entradas += PaqueteOoxml.Entrada("docProps/core.xml", propiedades(titulo))

        val listaDiapositivas = imagenes.indices.joinToString("") { indice ->
            "<p:sldId id=\"${256 + indice}\" r:id=\"${idsDiapositiva[indice]}\"/>"
        }
        entradas += PaqueteOoxml.Entrada(
            "ppt/presentation.xml",
            """
            ${PaqueteOoxml.CABECERA_XML}
            <p:presentation xmlns:a="$NS_DRAWING" xmlns:r="$NS_RELACIONES" xmlns:p="$NS_PRESENTACION">
              <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
              <p:sldIdLst>$listaDiapositivas</p:sldIdLst>
              <p:sldSz cx="9144000" cy="6858000"/>
              <p:notesSz cx="6858000" cy="9144000"/>
            </p:presentation>
            """.trimIndent(),
        )

        val relacionesPresentacion = buildString {
            append(PaqueteOoxml.CABECERA_XML)
            append("<Relationships xmlns=\"$NS_PAQUETE_RELACIONES\">")
            append("<Relationship Id=\"rId1\" Type=\"$TIPO_PATRON\" Target=\"slideMasters/slideMaster1.xml\"/>")
            imagenes.indices.forEach { indice ->
                append(
                    "<Relationship Id=\"${idsDiapositiva[indice]}\" Type=\"$TIPO_DIAPOSITIVA\" " +
                        "Target=\"slides/slide${indice + 1}.xml\"/>",
                )
            }
            append("</Relationships>")
        }
        entradas += PaqueteOoxml.Entrada("ppt/_rels/presentation.xml.rels", relacionesPresentacion)

        entradas += PaqueteOoxml.Entrada("ppt/slideMasters/slideMaster1.xml", patronVacio())
        entradas += PaqueteOoxml.Entrada(
            "ppt/slideMasters/_rels/slideMaster1.xml.rels",
            """
            ${PaqueteOoxml.CABECERA_XML}
            <Relationships xmlns="$NS_PAQUETE_RELACIONES">
              <Relationship Id="rId1" Type="$TIPO_DISENO" Target="../slideLayouts/slideLayout1.xml"/>
              <Relationship Id="rId2" Type="$TIPO_TEMA" Target="../theme/theme1.xml"/>
            </Relationships>
            """.trimIndent(),
        )
        entradas += PaqueteOoxml.Entrada("ppt/slideLayouts/slideLayout1.xml", disenoVacio())
        entradas += PaqueteOoxml.Entrada(
            "ppt/slideLayouts/_rels/slideLayout1.xml.rels",
            """
            ${PaqueteOoxml.CABECERA_XML}
            <Relationships xmlns="$NS_PAQUETE_RELACIONES">
              <Relationship Id="rId1" Type="$TIPO_PATRON" Target="../slideMasters/slideMaster1.xml"/>
            </Relationships>
            """.trimIndent(),
        )
        entradas += PaqueteOoxml.Entrada("ppt/theme/theme1.xml", temaMinimo())

        imagenes.forEachIndexed { indice, png ->
            val numero = indice + 1
            entradas += PaqueteOoxml.Entrada("ppt/media/image$numero.png", png)
            entradas += PaqueteOoxml.Entrada("ppt/slides/slide$numero.xml", diapositivaConImagen(numero))
            entradas += PaqueteOoxml.Entrada(
                "ppt/slides/_rels/slide$numero.xml.rels",
                """
                ${PaqueteOoxml.CABECERA_XML}
                <Relationships xmlns="$NS_PAQUETE_RELACIONES">
                  <Relationship Id="rId1" Type="$TIPO_DISENO" Target="../slideLayouts/slideLayout1.xml"/>
                  <Relationship Id="rId2" Type="$TIPO_IMAGEN" Target="../media/image$numero.png"/>
                </Relationships>
                """.trimIndent(),
            )
        }

        PaqueteOoxml.escribir(destino, entradas)
    }

    /** Diapositiva con la pagina del PDF ocupandola entera. */
    private fun diapositivaConImagen(numero: Int): String = """
        ${PaqueteOoxml.CABECERA_XML}
        <p:sld xmlns:a="$NS_DRAWING" xmlns:r="$NS_RELACIONES" xmlns:p="$NS_PRESENTACION">
          <p:cSld><p:spTree>
            <p:nvGrpSpPr>
              <p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/>
            </p:nvGrpSpPr>
            <p:grpSpPr><a:xfrm>
              <a:off x="0" y="0"/><a:ext cx="9144000" cy="6858000"/>
              <a:chOff x="0" y="0"/><a:chExt cx="9144000" cy="6858000"/>
            </a:xfrm></p:grpSpPr>
            <p:pic>
              <p:nvPicPr>
                <p:cNvPr id="${numero + 1}" name="Pagina $numero"/>
                <p:cNvPicPr/><p:nvPr/>
              </p:nvPicPr>
              <p:blipFill><a:blip r:embed="rId2"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>
              <p:spPr>
                <a:xfrm><a:off x="0" y="0"/><a:ext cx="9144000" cy="6858000"/></a:xfrm>
                <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
              </p:spPr>
            </p:pic>
          </p:spTree></p:cSld>
          <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
        </p:sld>
    """.trimIndent()

    private fun rasterizarPaginas(origen: File): List<ByteArray> = runCatching {
        ParcelFileDescriptor.open(origen, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderizador ->
                (0 until renderizador.pageCount).map { indice ->
                    renderizador.openPage(indice).use { pagina ->
                        val alto = (ANCHO_DIAPOSITIVA_PX.toFloat() * pagina.height / pagina.width)
                            .roundToInt()
                        val mapa = Bitmap.createBitmap(
                            ANCHO_DIAPOSITIVA_PX,
                            alto,
                            Bitmap.Config.ARGB_8888,
                        )
                        mapa.eraseColor(Color.WHITE)
                        pagina.render(mapa, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        ByteArrayOutputStream().use { salida ->
                            mapa.compress(Bitmap.CompressFormat.PNG, 100, salida)
                            mapa.recycle()
                            salida.toByteArray()
                        }
                    }
                }
            }
        }
    }.getOrDefault(emptyList())

    // --- Extraccion de texto -------------------------------------------------

    /** Lineas de texto de cada pagina, en orden de lectura. */
    private fun textoPorPagina(origen: File): List<List<String>> =
        PDDocument.load(origen).use { documento ->
            (1..documento.numberOfPages).map { numero ->
                val extractor = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = numero
                    endPage = numero
                }
                extractor.getText(documento)
                    .split('\n')
                    .map { it.trimEnd() }
                    .dropLastWhile { it.isBlank() }
            }
        }

    /** Una linea de texto con la posicion de cada uno de sus fragmentos. */
    private class Fragmento(val x: Float, val fin: Float, val texto: String)

    private class LineaConPosicion(val y: Float) {
        val fragmentos = mutableListOf<Fragmento>()
    }

    /**
     * Reconstruye filas y columnas a partir de las posiciones del texto.
     *
     * Se agrupan los fragmentos por altura para formar filas, y dentro de cada
     * fila se abre una columna nueva cuando el hueco horizontal supera el umbral.
     * Es la forma habitual de recuperar una tabla de un PDF, y acierta con las
     * tablas alineadas; con las que no lo estan, el resultado es aproximado.
     */
    private fun filasDetectadas(origen: File): List<List<String>> =
        filasPorPagina(origen).flatten()

    /**
     * Como [filasDetectadas], pero sin mezclar paginas.
     *
     * Word necesita el corte: una tabla que termina al pie de una pagina y otra
     * distinta que empieza en la siguiente tienen a veces el mismo numero de
     * columnas, y unidas saldrian como una sola tabla que no existe.
     */
    private fun filasPorPagina(origen: File): List<List<List<String>>> {
        val paginas = mutableListOf<List<List<String>>>()

        PDDocument.load(origen).use { documento ->
            (1..documento.numberOfPages).forEach { numero ->
                val filas = mutableListOf<List<String>>()
                val lineas = mutableListOf<LineaConPosicion>()

                val extractor = object : PDFTextStripper() {
                    override fun writeString(
                        texto: String,
                        posiciones: MutableList<TextPosition>,
                    ) {
                        if (texto.isBlank() || posiciones.isEmpty()) return
                        val y = posiciones.first().yDirAdj
                        val linea = lineas.firstOrNull { abs(it.y - y) < TOLERANCIA_FILA }
                            ?: LineaConPosicion(y).also { lineas += it }
                        // Se guarda tambien donde acaba: con el ancho real de
                        // los glifos, el hueco entre columnas se mide bien y
                        // deja de partir palabras largas en dos celdas.
                        val ultima = posiciones.last()
                        linea.fragmentos += Fragmento(
                            x = posiciones.first().xDirAdj,
                            fin = ultima.xDirAdj + ultima.widthDirAdj,
                            texto = texto.trim(),
                        )
                    }
                }.apply {
                    sortByPosition = true
                    startPage = numero
                    endPage = numero
                }
                extractor.getText(documento)

                lineas.sortedBy { it.y }.forEach { linea ->
                    val ordenados = linea.fragmentos.sortedBy { it.x }
                    val celdas = mutableListOf<String>()
                    var acumulado = StringBuilder()
                    var finAnterior = Float.NaN

                    ordenados.forEach { fragmento ->
                        val hayHueco = !finAnterior.isNaN() &&
                            fragmento.x - finAnterior > SEPARACION_COLUMNA
                        if (hayHueco && acumulado.isNotEmpty()) {
                            celdas += acumulado.toString().trim()
                            acumulado = StringBuilder()
                        }
                        if (acumulado.isNotEmpty()) acumulado.append(' ')
                        acumulado.append(fragmento.texto)
                        finAnterior = fragmento.fin
                    }
                    if (acumulado.isNotEmpty()) celdas += acumulado.toString().trim()
                    if (celdas.any { it.isNotBlank() }) filas += celdas
                }
                paginas += filas
            }
        }
        return paginas
    }

    /** Lo que sale de una pagina: parrafos sueltos o una tabla. */
    private sealed interface Bloque {
        class Parrafos(val lineas: List<String>) : Bloque
        class Tabla(val filas: List<List<String>>) : Bloque
    }

    /**
     * Separa las filas de una pagina en parrafos y tablas.
     *
     * Una tabla se reconoce por lo mismo que la reconoce el ojo: varias filas
     * seguidas partidas en el mismo numero de columnas. Con dos filas no basta
     * (dos lineas con un hueco en medio son un texto con sangria, no una
     * tabla), asi que se exige un minimo de tres.
     *
     * No se intenta adivinar mas: sin bordes ni etiquetas de estructura, un PDF
     * no dice donde empieza una tabla, y pasarse de listo convierte parrafos
     * normales en tablas de una columna.
     */
    private fun agruparEnBloques(filas: List<List<String>>): List<Bloque> {
        val bloques = mutableListOf<Bloque>()
        val sueltas = mutableListOf<String>()
        var indice = 0

        fun volcarSueltas() {
            if (sueltas.isNotEmpty()) {
                bloques += Bloque.Parrafos(sueltas.toList())
                sueltas.clear()
            }
        }

        while (indice < filas.size) {
            val columnas = filas[indice].size
            if (columnas < MINIMO_COLUMNAS) {
                sueltas += filas[indice].joinToString(" ").trim()
                indice++
                continue
            }

            var fin = indice
            while (fin < filas.size && filas[fin].size == columnas) fin++
            val cuantas = fin - indice

            if (cuantas >= MINIMO_FILAS_TABLA) {
                volcarSueltas()
                bloques += Bloque.Tabla(filas.subList(indice, fin).toList())
            } else {
                // Pocas filas para ser tabla: se escriben como texto normal.
                (indice until fin).forEach { sueltas += filas[it].joinToString(" ").trim() }
            }
            indice = fin
        }
        volcarSueltas()
        return bloques
    }

    /**
     * Una tabla de Word con bordes finos y ancho repartido.
     *
     * El ancho va en vigesimas de punto sobre el area util de un A4 con los
     * margenes que pone esta misma clase: 11906 - 1134 - 1134 = 9638.
     */
    private fun tablaWord(filas: List<List<String>>): String {
        val columnas = filas.first().size
        val ancho = ANCHO_UTIL_TWIPS / columnas

        return buildString {
            append("<w:tbl><w:tblPr>")
            append("<w:tblW w:w=\"$ANCHO_UTIL_TWIPS\" w:type=\"dxa\"/>")
            append("<w:tblBorders>")
            listOf("top", "left", "bottom", "right", "insideH", "insideV").forEach { lado ->
                append("<w:$lado w:val=\"single\" w:sz=\"4\" w:color=\"BFBFBF\"/>")
            }
            append("</w:tblBorders></w:tblPr><w:tblGrid>")
            repeat(columnas) { append("<w:gridCol w:w=\"$ancho\"/>") }
            append("</w:tblGrid>")

            filas.forEachIndexed { numero, fila ->
                append("<w:tr>")
                fila.forEach { celda ->
                    append("<w:tc><w:tcPr><w:tcW w:w=\"$ancho\" w:type=\"dxa\"/></w:tcPr>")
                    append("<w:p><w:r>")
                    // La primera fila se pone en negrita: en una tabla sacada
                    // de un PDF casi siempre es la cabecera.
                    if (numero == 0) append("<w:rPr><w:b/></w:rPr>")
                    append("<w:t xml:space=\"preserve\">")
                    append(PaqueteOoxml.escapar(celda))
                    append("</w:t></w:r></w:p></w:tc>")
                }
                append("</w:tr>")
            }
            append("</w:tbl>")
            // Word necesita un parrafo detras de una tabla; sin el, dos tablas
            // seguidas se funden en una.
            append("<w:p/>")
        }
    }

    // --- Partes fijas de los paquetes ----------------------------------------

    private fun tiposDeContenido(especificos: String): String = """
        ${PaqueteOoxml.CABECERA_XML}
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Default Extension="png" ContentType="image/png"/>
          <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
          $especificos
        </Types>
    """.trimIndent()

    private fun tiposDeContenidoPptx(diapositivas: Int): String {
        val partes = buildString {
            append(
                "<Override PartName=\"/ppt/presentation.xml\" ContentType=\"$TIPO_CONTENIDO_PRESENTACION\"/>",
            )
            append(
                "<Override PartName=\"/ppt/slideMasters/slideMaster1.xml\" " +
                    "ContentType=\"$TIPO_CONTENIDO_PATRON\"/>",
            )
            append(
                "<Override PartName=\"/ppt/slideLayouts/slideLayout1.xml\" " +
                    "ContentType=\"$TIPO_CONTENIDO_DISENO\"/>",
            )
            append(
                "<Override PartName=\"/ppt/theme/theme1.xml\" ContentType=\"$TIPO_CONTENIDO_TEMA\"/>",
            )
            repeat(diapositivas) { indice ->
                append(
                    "<Override PartName=\"/ppt/slides/slide${indice + 1}.xml\" " +
                        "ContentType=\"$TIPO_CONTENIDO_DIAPOSITIVA\"/>",
                )
            }
        }
        return tiposDeContenido(partes)
    }

    private fun relacionesRaiz(destino: String, tipo: String): String = """
        ${PaqueteOoxml.CABECERA_XML}
        <Relationships xmlns="$NS_PAQUETE_RELACIONES">
          <Relationship Id="rId1" Type="$tipo" Target="$destino"/>
          <Relationship Id="rId2" Type="$TIPO_PROPIEDADES" Target="docProps/core.xml"/>
        </Relationships>
    """.trimIndent()

    private fun propiedades(titulo: String): String = """
        ${PaqueteOoxml.CABECERA_XML}
        <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                           xmlns:dc="http://purl.org/dc/elements/1.1/">
          <dc:title>${PaqueteOoxml.escapar(titulo)}</dc:title>
          <dc:creator>NexaPDF</dc:creator>
          <cp:lastModifiedBy>NexaPDF</cp:lastModifiedBy>
        </cp:coreProperties>
    """.trimIndent()

    /**
     * Patron y diseno minimos.
     *
     * PowerPoint exige que existan aunque no se usen: sin ellos considera el
     * fichero corrupto y se niega a abrirlo.
     */
    private fun patronVacio(): String = """
        ${PaqueteOoxml.CABECERA_XML}
        <p:sldMaster xmlns:a="$NS_DRAWING" xmlns:r="$NS_RELACIONES" xmlns:p="$NS_PRESENTACION">
          <p:cSld><p:spTree>
            <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
            <p:grpSpPr/>
          </p:spTree></p:cSld>
          <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2"
                    accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6"
                    hlink="hlink" folHlink="folHlink"/>
          <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
        </p:sldMaster>
    """.trimIndent()

    private fun disenoVacio(): String = """
        ${PaqueteOoxml.CABECERA_XML}
        <p:sldLayout xmlns:a="$NS_DRAWING" xmlns:r="$NS_RELACIONES" xmlns:p="$NS_PRESENTACION"
                     type="blank" preserve="1">
          <p:cSld name="Blanco"><p:spTree>
            <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
            <p:grpSpPr/>
          </p:spTree></p:cSld>
          <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
        </p:sldLayout>
    """.trimIndent()

    private fun temaMinimo(): String = """
        ${PaqueteOoxml.CABECERA_XML}
        <a:theme xmlns:a="$NS_DRAWING" name="NexaPDF">
          <a:themeElements>
            <a:clrScheme name="NexaPDF">
              <a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1>
              <a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1>
              <a:dk2><a:srgbClr val="1B1B1F"/></a:dk2>
              <a:lt2><a:srgbClr val="F9F9FF"/></a:lt2>
              <a:accent1><a:srgbClr val="4A5A9F"/></a:accent1>
              <a:accent2><a:srgbClr val="5A5C77"/></a:accent2>
              <a:accent3><a:srgbClr val="7E506D"/></a:accent3>
              <a:accent4><a:srgbClr val="00696D"/></a:accent4>
              <a:accent5><a:srgbClr val="8B5000"/></a:accent5>
              <a:accent6><a:srgbClr val="B12938"/></a:accent6>
              <a:hlink><a:srgbClr val="0066CC"/></a:hlink>
              <a:folHlink><a:srgbClr val="7B1FA2"/></a:folHlink>
            </a:clrScheme>
            <a:fontScheme name="NexaPDF">
              <a:majorFont><a:latin typeface="Arial"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont>
              <a:minorFont><a:latin typeface="Arial"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont>
            </a:fontScheme>
            <a:fmtScheme name="NexaPDF">
              <a:fillStyleLst>
                <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
              </a:fillStyleLst>
              <a:lnStyleLst>
                <a:ln><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln>
                <a:ln><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln>
                <a:ln><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln>
              </a:lnStyleLst>
              <a:effectStyleLst>
                <a:effectStyle><a:effectLst/></a:effectStyle>
                <a:effectStyle><a:effectLst/></a:effectStyle>
                <a:effectStyle><a:effectLst/></a:effectStyle>
              </a:effectStyleLst>
              <a:bgFillStyleLst>
                <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
                <a:solidFill><a:schemeClr val="phClr"/></a:solidFill>
              </a:bgFillStyleLst>
            </a:fmtScheme>
          </a:themeElements>
        </a:theme>
    """.trimIndent()
}

// --- Espacios de nombres y tipos de contenido de OOXML ------------------------

private const val NS_DRAWING = "http://schemas.openxmlformats.org/drawingml/2006/main"
private const val NS_RELACIONES = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
private const val NS_PRESENTACION = "http://schemas.openxmlformats.org/presentationml/2006/main"
private const val NS_PAQUETE_RELACIONES =
    "http://schemas.openxmlformats.org/package/2006/relationships"

private const val TIPO_DOCUMENTO_WORD = "$NS_RELACIONES/officeDocument"
private const val TIPO_DOCUMENTO_EXCEL = "$NS_RELACIONES/officeDocument"
private const val TIPO_DOCUMENTO_POWERPOINT = "$NS_RELACIONES/officeDocument"
private const val TIPO_PROPIEDADES =
    "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"
private const val TIPO_HOJA = "$NS_RELACIONES/worksheet"
private const val TIPO_CADENAS = "$NS_RELACIONES/sharedStrings"
private const val TIPO_DIAPOSITIVA = "$NS_RELACIONES/slide"
private const val TIPO_PATRON = "$NS_RELACIONES/slideMaster"
private const val TIPO_DISENO = "$NS_RELACIONES/slideLayout"
private const val TIPO_TEMA = "$NS_RELACIONES/theme"
private const val TIPO_IMAGEN = "$NS_RELACIONES/image"

private const val TIPOS_DOCX =
    "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
private const val TIPOS_XLSX =
    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
        "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
        "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>"

private const val TIPO_CONTENIDO_PRESENTACION =
    "application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"
private const val TIPO_CONTENIDO_DIAPOSITIVA =
    "application/vnd.openxmlformats-officedocument.presentationml.slide+xml"
private const val TIPO_CONTENIDO_PATRON =
    "application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"
private const val TIPO_CONTENIDO_DISENO =
    "application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"
private const val TIPO_CONTENIDO_TEMA =
    "application/vnd.openxmlformats-officedocument.theme+xml"
