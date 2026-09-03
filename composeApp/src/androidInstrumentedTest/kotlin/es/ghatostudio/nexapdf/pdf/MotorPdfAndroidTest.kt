package es.ghatostudio.nexapdf.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import es.ghatostudio.nexapdf.domain.model.AlineacionTexto
import es.ghatostudio.nexapdf.domain.model.BorradorEdicion
import es.ghatostudio.nexapdf.domain.model.DisposicionImagenes
import es.ghatostudio.nexapdf.domain.model.Edicion
import es.ghatostudio.nexapdf.domain.model.EdicionPagina
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.domain.model.Orientacion
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.RangoPaginas
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import es.ghatostudio.nexapdf.domain.model.TamanoPagina
import es.ghatostudio.nexapdf.domain.model.TipoFigura
import es.ghatostudio.nexapdf.domain.pdf.EntradaUnion
import es.ghatostudio.nexapdf.domain.pdf.ErrorPdf
import es.ghatostudio.nexapdf.domain.pdf.OrigenCertificado
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import kotlinx.coroutines.runBlocking
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date

/**
 * Pruebas del motor de PDF sobre el dispositivo real.
 *
 * Corren en el telefono y no en la JVM del ordenador a proposito: casi todo lo
 * que hace este motor depende de APIs de Android que no existen fuera del
 * dispositivo (PdfRenderer, Bitmap, el proveedor de seguridad del sistema).
 * Una prueba que se ejecutase en el escritorio no probaria el mismo codigo.
 *
 * Cada prueba se fabrica sus propios documentos, asi que no hace falta preparar
 * nada en el telefono ni depender de ficheros que puedan no estar.
 */
@RunWith(AndroidJUnit4::class)
class MotorPdfAndroidTest {

    private lateinit var trabajo: File
    private lateinit var motor: MotorPdfAndroid

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        trabajo = File(contexto.cacheDir, "pruebas-pdf").apply {
            deleteRecursively()
            mkdirs()
        }
        motor = MotorPdfAndroid(contexto, trabajo.absolutePath)
    }

    // --- Utilidades ----------------------------------------------------------

    /** Crea un PDF con [paginas] paginas, cada una con su numero escrito. */
    private fun crearPdf(nombre: String, paginas: Int, texto: String = "Pagina"): File {
        val fichero = File(trabajo, nombre)
        PDDocument().use { documento ->
            repeat(paginas) { indice ->
                val pagina = PDPage(PDRectangle.A4)
                documento.addPage(pagina)
                PDPageContentStream(documento, pagina).use { flujo ->
                    flujo.beginText()
                    flujo.setFont(PDType1Font.HELVETICA_BOLD, 36f)
                    flujo.newLineAtOffset(72f, 700f)
                    flujo.showText("$texto ${indice + 1}")
                    flujo.endText()
                }
            }
            documento.save(fichero)
        }
        return fichero
    }

    private fun crearImagen(nombre: String, ancho: Int, alto: Int, color: Int): File {
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        Canvas(mapa).apply {
            drawColor(color)
            drawCircle(
                ancho / 2f,
                alto / 2f,
                minOf(ancho, alto) / 3f,
                Paint().apply { this.color = Color.WHITE },
            )
        }
        val fichero = File(trabajo, nombre)
        fichero.outputStream().use { mapa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        mapa.recycle()
        return fichero
    }

    private fun <T> exito(resultado: ResultadoPdf<T>): T {
        assertTrue(
            "Se esperaba exito y llego $resultado",
            resultado is ResultadoPdf.Exito,
        )
        return (resultado as ResultadoPdf.Exito).valor
    }

    private fun paginasDe(fichero: File): Int =
        PDDocument.load(fichero).use { it.numberOfPages }

    private fun textoDe(fichero: File): String =
        PDDocument.load(fichero).use { com.tom_roush.pdfbox.text.PDFTextStripper().getText(it) }

    // --- Apertura y lectura --------------------------------------------------

    @Test
    fun abre_un_documento_y_lee_sus_paginas() = runBlocking {
        val fichero = crearPdf("origen.pdf", 3)

        val documento = exito(motor.abrir(fichero.absolutePath))
        assertEquals(3, documento.numeroPaginas)
        assertTrue(documento.tamanoBytes > 0)
        assertTrue(!documento.cifrado)

        val paginas = exito(motor.paginas(fichero.absolutePath))
        assertEquals(3, paginas.size)
        assertEquals(0, paginas[0].indice)
        // A4 en puntos, con la tolerancia del redondeo de PDFBox.
        assertEquals(595f, paginas[0].anchoPt, 1f)
        assertEquals(842f, paginas[0].altoPt, 1f)
    }

    @Test
    fun un_fichero_que_no_es_pdf_da_error_legible() = runBlocking {
        val basura = File(trabajo, "basura.pdf").apply { writeText("esto no es un PDF") }

        val resultado = motor.abrir(basura.absolutePath)
        assertTrue(resultado is ResultadoPdf.Fallo)
        assertEquals(ErrorPdf.FICHERO_INVALIDO, (resultado as ResultadoPdf.Fallo).causa)
    }

    @Test
    fun renderiza_una_pagina_al_ancho_pedido() = runBlocking {
        val fichero = crearPdf("render.pdf", 1)

        val imagen = exito(motor.renderizarPagina(fichero.absolutePath, 0, anchoPx = 600))
        assertEquals(600, imagen.width)
        // A4 es mas alto que ancho: la proporcion debe conservarse.
        assertTrue("El alto deberia ser mayor que el ancho", imagen.height > imagen.width)

        motor.cerrar(fichero.absolutePath)
    }

    // --- Unir, extraer, separar, reorganizar ---------------------------------

    @Test
    fun une_documentos_conservando_el_orden() = runBlocking {
        val primero = crearPdf("a.pdf", 2, "A")
        val segundo = crearPdf("b.pdf", 3, "B")
        val salida = File(trabajo, "unido.pdf")

        exito(
            motor.unir(
                listOf(EntradaUnion(primero.absolutePath), EntradaUnion(segundo.absolutePath)),
                salida.absolutePath,
            ),
        )

        assertEquals(5, paginasDe(salida))
        val texto = textoDe(salida)
        assertTrue("Falta el contenido del primero", texto.contains("A 1"))
        assertTrue("Falta el contenido del segundo", texto.contains("B 3"))
        assertTrue(
            "El primero debe ir antes que el segundo",
            texto.indexOf("A 1") < texto.indexOf("B 1"),
        )
    }

    @Test
    fun une_solo_las_paginas_pedidas_de_cada_documento() = runBlocking {
        val primero = crearPdf("c.pdf", 4, "C")
        val segundo = crearPdf("d.pdf", 4, "D")
        val salida = File(trabajo, "unido-parcial.pdf")

        exito(
            motor.unir(
                listOf(
                    EntradaUnion(primero.absolutePath, paginas = listOf(0, 2)),
                    EntradaUnion(segundo.absolutePath, paginas = listOf(3)),
                ),
                salida.absolutePath,
            ),
        )

        assertEquals(3, paginasDe(salida))
        val texto = textoDe(salida)
        assertTrue(texto.contains("C 1"))
        assertTrue(texto.contains("C 3"))
        assertTrue(texto.contains("D 4"))
        assertTrue("No deberia estar la pagina 2 de C", !texto.contains("C 2"))
    }

    @Test
    fun extrae_las_paginas_indicadas() = runBlocking {
        val fichero = crearPdf("extraer.pdf", 6, "P")
        val salida = File(trabajo, "extraido.pdf")

        exito(motor.extraerPaginas(fichero.absolutePath, listOf(1, 3, 5), salida.absolutePath))

        assertEquals(3, paginasDe(salida))
        val texto = textoDe(salida)
        assertTrue(texto.contains("P 2"))
        assertTrue(texto.contains("P 4"))
        assertTrue(texto.contains("P 6"))
        assertTrue(!texto.contains("P 1"))
    }

    @Test
    fun separa_el_documento_en_un_fichero_por_pagina() = runBlocking {
        val fichero = crearPdf("separar.pdf", 4, "S")
        val destino = File(trabajo, "partes").apply { mkdirs() }

        val generados = exito(
            motor.separar(
                ruta = fichero.absolutePath,
                rangos = (0..3).map { RangoPaginas(it, it) },
                directorioSalida = destino.absolutePath,
                nombreBase = "parte",
            ),
        )

        assertEquals(4, generados.size)
        generados.forEach { ruta -> assertEquals(1, paginasDe(File(ruta))) }
        assertTrue(textoDe(File(generados[2])).contains("S 3"))
    }

    @Test
    fun separa_por_rangos_de_varias_paginas() = runBlocking {
        val fichero = crearPdf("rangos.pdf", 6, "R")
        val destino = File(trabajo, "rangos").apply { mkdirs() }

        val generados = exito(
            motor.separar(
                ruta = fichero.absolutePath,
                rangos = listOf(RangoPaginas(0, 2), RangoPaginas(3, 5)),
                directorioSalida = destino.absolutePath,
                nombreBase = "bloque",
            ),
        )

        assertEquals(2, generados.size)
        assertEquals(3, paginasDe(File(generados[0])))
        assertEquals(3, paginasDe(File(generados[1])))
    }

    @Test
    fun reorganiza_y_gira_paginas() = runBlocking {
        val fichero = crearPdf("reorganizar.pdf", 3, "Q")
        val salida = File(trabajo, "reorganizado.pdf")

        exito(
            motor.reorganizar(
                ruta = fichero.absolutePath,
                ordenPaginas = listOf(2, 0, 1),
                rotaciones = mapOf(0 to 90),
                rutaSalida = salida.absolutePath,
            ),
        )

        assertEquals(3, paginasDe(salida))
        PDDocument.load(salida).use { documento ->
            // La pagina que estaba en la posicion 0 va ahora la segunda y es la
            // unica girada.
            assertEquals(0, documento.getPage(0).rotation)
            assertEquals(90, documento.getPage(1).rotation)
        }
    }

    // --- Imagenes a PDF ------------------------------------------------------

    @Test
    fun crea_un_pdf_con_una_imagen_por_pagina() = runBlocking {
        val imagenes = listOf(
            crearImagen("i1.png", 800, 600, Color.RED),
            crearImagen("i2.png", 600, 800, Color.BLUE),
            crearImagen("i3.png", 700, 700, Color.GREEN),
        ).map { it.absolutePath }
        val salida = File(trabajo, "imagenes.pdf")

        exito(
            motor.imagenesAPdf(
                imagenes = imagenes,
                disposicion = DisposicionImagenes.UNA_POR_PAGINA,
                tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
                orientacion = Orientacion.AUTOMATICA,
                margenPt = 0f,
                espaciadoPt = 0f,
                rutaSalida = salida.absolutePath,
            ),
        )

        assertEquals(3, paginasDe(salida))
        PDDocument.load(salida).use { documento ->
            // Con "ajustar a la imagen", la pagina toma la proporcion de la foto.
            val primera = documento.getPage(0).mediaBox
            assertEquals(800f / 600f, primera.width / primera.height, 0.02f)
            val segunda = documento.getPage(1).mediaBox
            assertEquals(600f / 800f, segunda.width / segunda.height, 0.02f)
        }
    }

    @Test
    fun agrupa_varias_imagenes_en_la_misma_pagina() = runBlocking {
        val imagenes = (1..7).map {
            crearImagen("g$it.png", 400, 400, Color.rgb(it * 30, 100, 200)).absolutePath
        }
        val salida = File(trabajo, "cuadricula.pdf")

        exito(
            motor.imagenesAPdf(
                imagenes = imagenes,
                disposicion = DisposicionImagenes.CUATRO_POR_PAGINA,
                tamano = TamanoPagina.A4,
                orientacion = Orientacion.VERTICAL,
                margenPt = 24f,
                espaciadoPt = 12f,
                rutaSalida = salida.absolutePath,
            ),
        )

        // Siete imagenes de cuatro en cuatro son dos paginas.
        assertEquals(2, paginasDe(salida))
        PDDocument.load(salida).use { documento ->
            val caja = documento.getPage(0).mediaBox
            assertEquals(595f, caja.width, 1f)
            assertEquals(842f, caja.height, 1f)
        }
    }

    // --- Ediciones -----------------------------------------------------------

    @Test
    fun aplica_trazos_figuras_texto_y_tapados() = runBlocking {
        val fichero = crearPdf("editar.pdf", 2, "E")
        val salida = File(trabajo, "editado.pdf")

        val borrador = BorradorEdicion(
            rutaDocumento = fichero.absolutePath,
            paginas = mapOf(
                0 to EdicionPagina(
                    indice = 0,
                    ediciones = listOf(
                        Edicion.Trazo(
                            id = "t1",
                            puntos = (0..20).map { Punto(0.1f + it * 0.03f, 0.5f) },
                            colorArgb = 0xFFD32F2FL,
                            grosor = 0.006f,
                        ),
                        Edicion.Trazo(
                            id = "t2",
                            puntos = (0..10).map { Punto(0.2f, 0.2f + it * 0.02f) },
                            colorArgb = 0xFFF9A825L,
                            grosor = 0.02f,
                            opacidad = 0.35f,
                            resaltador = true,
                        ),
                        Edicion.Figura(
                            id = "f1",
                            tipo = TipoFigura.RECTANGULO,
                            marco = Rectangulo(0.1f, 0.1f, 0.4f, 0.25f),
                            colorTrazoArgb = 0xFF1976D2L,
                            colorRellenoArgb = null,
                            grosor = 0.004f,
                        ),
                        Edicion.Figura(
                            id = "f2",
                            tipo = TipoFigura.ELIPSE,
                            marco = Rectangulo(0.5f, 0.1f, 0.8f, 0.3f),
                            colorTrazoArgb = 0xFF388E3CL,
                            colorRellenoArgb = 0xFFDDF3DDL,
                            grosor = 0.004f,
                        ),
                        Edicion.Figura(
                            id = "f3",
                            tipo = TipoFigura.FLECHA,
                            marco = Rectangulo(0.2f, 0.6f, 0.7f, 0.75f),
                            colorTrazoArgb = 0xFF7B1FA2L,
                            colorRellenoArgb = null,
                            grosor = 0.005f,
                        ),
                        Edicion.Tapado(
                            id = "x1",
                            marco = Rectangulo(0.1f, 0.8f, 0.6f, 0.86f),
                            colorArgb = 0xFFFFFFFFL,
                        ),
                        Edicion.Texto(
                            id = "tx1",
                            contenido = "Texto anadido por NexaPDF",
                            marco = Rectangulo(0.1f, 0.8f, 0.9f, 0.86f),
                            colorArgb = 0xFF1A1A1AL,
                            tamano = 0.02f,
                            alineacion = AlineacionTexto.INICIO,
                        ),
                    ),
                ),
            ),
        )

        exito(motor.aplicarEdiciones(borrador, salida.absolutePath))

        assertEquals(2, paginasDe(salida))
        val texto = textoDe(salida)
        assertTrue("El texto original debe seguir", texto.contains("E 1"))
        assertTrue(
            "El texto anadido debe ser texto real y extraible",
            texto.contains("Texto anadido por NexaPDF"),
        )
    }

    @Test
    fun el_texto_en_alfabetos_no_latinos_no_rompe_el_documento() = runBlocking {
        val fichero = crearPdf("idiomas.pdf", 1)
        val salida = File(trabajo, "idiomas-editado.pdf")

        // Griego y cirilico deberian ir con fuente incrustada; el arabe necesita
        // conformado y cae al camino rasterizado. Ninguno de los dos debe fallar.
        val textos = listOf("Ελληνικά", "Русский", "العربية", "日本語")
        val borrador = BorradorEdicion(
            rutaDocumento = fichero.absolutePath,
            paginas = mapOf(
                0 to EdicionPagina(
                    indice = 0,
                    ediciones = textos.mapIndexed { indice, contenido ->
                        Edicion.Texto(
                            id = "idioma$indice",
                            contenido = contenido,
                            marco = Rectangulo(0.1f, 0.1f + indice * 0.15f, 0.9f, 0.2f + indice * 0.15f),
                            colorArgb = 0xFF1A1A1AL,
                            tamano = 0.03f,
                        )
                    },
                ),
            ),
        )

        exito(motor.aplicarEdiciones(borrador, salida.absolutePath))
        assertEquals(1, paginasDe(salida))
        assertTrue("El documento resultante deberia pesar algo", salida.length() > 1000)
    }

    @Test
    fun un_filtro_rasteriza_la_pagina_y_quita_su_texto() = runBlocking {
        val fichero = crearPdf("filtro.pdf", 2, "F")
        val salida = File(trabajo, "filtrado.pdf")

        val borrador = BorradorEdicion(
            rutaDocumento = fichero.absolutePath,
            paginas = mapOf(
                0 to EdicionPagina(
                    indice = 0,
                    filtro = FiltroPagina.BLANCO_Y_NEGRO,
                    intensidadFiltro = 0.6f,
                ),
            ),
        )

        exito(motor.aplicarEdiciones(borrador, salida.absolutePath))

        val texto = textoDe(salida)
        // La pagina filtrada pierde el texto seleccionable, que es el efecto
        // documentado; la que no se toco lo conserva.
        assertTrue("La pagina filtrada no deberia tener texto", !texto.contains("F 1"))
        assertTrue("La pagina sin filtrar debe conservarlo", texto.contains("F 2"))
    }

    @Test
    fun la_firma_manuscrita_se_estampa_en_la_pagina() = runBlocking {
        val fichero = crearPdf("firmar.pdf", 1)
        val salida = File(trabajo, "firmado-mano.pdf")

        val borrador = BorradorEdicion(
            rutaDocumento = fichero.absolutePath,
            paginas = mapOf(
                0 to EdicionPagina(
                    indice = 0,
                    ediciones = listOf(
                        Edicion.Firma(
                            id = "firma",
                            trazos = listOf(
                                (0..15).map { Punto(it / 15f, 0.5f + 0.3f * kotlin.math.sin(it * 0.6f)) },
                            ),
                            marco = Rectangulo(0.5f, 0.75f, 0.95f, 0.9f),
                            colorArgb = 0xFF1A1A1AL,
                            grosor = 0.004f,
                        ),
                    ),
                ),
            ),
        )

        exito(motor.aplicarEdiciones(borrador, salida.absolutePath))
        assertTrue(salida.length() > fichero.length())
    }

    @Test
    fun localiza_los_bloques_de_texto_de_una_pagina() = runBlocking {
        val fichero = crearPdf("bloques.pdf", 1, "Bloque")

        val bloques = exito(motor.bloquesDeTexto(fichero.absolutePath, 0))

        assertTrue("Deberia encontrar al menos un bloque", bloques.isNotEmpty())
        val bloque = bloques.first { it.texto.contains("Bloque") }
        // Las coordenadas llegan normalizadas: dentro de la pagina y con area.
        assertTrue(bloque.marco.izquierda in 0f..1f)
        assertTrue(bloque.marco.arriba in 0f..1f)
        assertTrue(bloque.marco.ancho > 0f)
        assertTrue(bloque.marco.alto > 0f)
    }

    // --- Firma con certificado -----------------------------------------------

    @Test
    fun firma_con_un_certificado_y_la_firma_queda_en_el_documento() = runBlocking {
        val fichero = crearPdf("para-firmar.pdf", 2)
        val salida = File(trabajo, "firmado-certificado.pdf")
        val certificado = generarPkcs12("Brais Galdo", CONTRASENA_CERTIFICADO)

        exito(
            motor.firmarConCertificado(
                ruta = fichero.absolutePath,
                origen = OrigenCertificado.Fichero(certificado, CONTRASENA_CERTIFICADO),
                apariencia = null,
                motivo = "Conformidad",
                lugar = "A Coruna",
                rutaSalida = salida.absolutePath,
            ),
        )

        val firmas = exito(motor.firmasExistentes(salida.absolutePath))
        assertEquals(1, firmas.size)
        assertEquals("Brais Galdo", firmas.first().nombre)
        assertEquals("Conformidad", firmas.first().motivo)
        assertEquals("A Coruna", firmas.first().lugar)
        assertNotNull(firmas.first().fechaEpochMillis)
        assertTrue(
            "La firma deberia cubrir todo el documento",
            firmas.first().cubreTodoElDocumento,
        )

        // El guardado incremental conserva el contenido anterior intacto.
        assertEquals(2, paginasDe(salida))
        assertTrue(textoDe(salida).contains("Pagina 1"))
    }

    @Test
    fun una_contrasena_de_certificado_equivocada_da_error_de_certificado() = runBlocking {
        val fichero = crearPdf("para-firmar-2.pdf", 1)
        val salida = File(trabajo, "no-firmado.pdf")
        val certificado = generarPkcs12("Prueba", CONTRASENA_CERTIFICADO)

        val resultado = motor.firmarConCertificado(
            ruta = fichero.absolutePath,
            origen = OrigenCertificado.Fichero(certificado, "contrasena-equivocada"),
            apariencia = null,
            motivo = null,
            lugar = null,
            rutaSalida = salida.absolutePath,
        )

        assertTrue(resultado is ResultadoPdf.Fallo)
        assertEquals(ErrorPdf.CERTIFICADO_INVALIDO, (resultado as ResultadoPdf.Fallo).causa)
    }

    @Test
    fun un_documento_sin_firmar_no_declara_firmas() = runBlocking {
        val fichero = crearPdf("sin-firma.pdf", 1)
        assertTrue(exito(motor.firmasExistentes(fichero.absolutePath)).isEmpty())
    }

    /** Genera un PKCS#12 autofirmado para poder probar la firma sin depender de nada. */
    private fun generarPkcs12(nombre: String, contrasena: String): ByteArray {
        val proveedor = BouncyCastleProvider()
        val claves = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

        val ahora = System.currentTimeMillis()
        val sujeto = X500Name("CN=$nombre, O=NexaPDF, C=ES")
        val certificado: X509Certificate = JcaX509CertificateConverter()
            .setProvider(proveedor)
            .getCertificate(
                JcaX509v3CertificateBuilder(
                    sujeto,
                    BigInteger.valueOf(ahora),
                    Date(ahora - 86_400_000L),
                    Date(ahora + 365L * 86_400_000L),
                    sujeto,
                    claves.public,
                ).build(
                    JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider(proveedor)
                        .build(claves.private),
                ),
            )

        val almacen = KeyStore.getInstance("PKCS12", proveedor).apply {
            load(null, null)
            setKeyEntry("nexapdf", claves.private, contrasena.toCharArray(), arrayOf(certificado))
        }
        return java.io.ByteArrayOutputStream()
            .also { almacen.store(it, contrasena.toCharArray()) }
            .toByteArray()
    }

    companion object {
        private const val CONTRASENA_CERTIFICADO = "prueba1234"

        @JvmStatic
        @BeforeClass
        fun cargarPdfBox() {
            PDFBoxResourceLoader.init(
                ApplicationProvider.getApplicationContext<android.content.Context>(),
            )
        }
    }
}
