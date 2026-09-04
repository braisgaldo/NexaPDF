package es.ghatostudio.nexapdf.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import es.ghatostudio.nexapdf.domain.model.BloqueTexto
import es.ghatostudio.nexapdf.domain.model.BorradorEdicion
import es.ghatostudio.nexapdf.domain.model.DisposicionImagenes
import es.ghatostudio.nexapdf.domain.model.DocumentoPdf
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.domain.model.Orientacion
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import es.ghatostudio.nexapdf.domain.model.RangoPaginas
import es.ghatostudio.nexapdf.domain.model.TamanoPagina
import es.ghatostudio.nexapdf.domain.pdf.AparienciaFirma
import es.ghatostudio.nexapdf.domain.pdf.Coincidencia
import es.ghatostudio.nexapdf.domain.pdf.EntradaUnion
import es.ghatostudio.nexapdf.domain.pdf.ErrorPdf
import es.ghatostudio.nexapdf.domain.pdf.FirmaExistente
import es.ghatostudio.nexapdf.domain.pdf.MotorPdf
import es.ghatostudio.nexapdf.domain.pdf.OrigenCertificado
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import es.ghatostudio.nexapdf.domain.pdf.Seccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import es.ghatostudio.nexapdf.domain.pdf.PermisosPdf
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.util.Selector
import org.bouncycastle.util.Store

/**
 * Motor de PDF para Android.
 *
 * Reparte el trabajo entre dos bibliotecas segun para que sirve cada una:
 *
 *  - **`android.graphics.pdf.PdfRenderer`** para mostrar paginas. Es el
 *    renderizador del sistema, esta acelerado por hardware y tarda milisegundos,
 *    que es lo que hace falta para pasar paginas sin esperas.
 *  - **PDFBox** para modificar documentos, que es lo que PdfRenderer no sabe
 *    hacer: unir, separar, anotar, incrustar imagenes y firmar.
 *
 * Los documentos cifrados se descifran una sola vez al abrirlos y se trabaja
 * sobre la copia descifrada, porque PdfRenderer no admite contrasenas.
 */
class MotorPdfAndroid(
    private val contexto: Context,
    private val directorioTrabajo: String,
) : MotorPdf {

    /**
     * Un cerrojo por documento, no uno para todo.
     *
     * `PdfRenderer` no admite dos paginas abiertas a la vez del mismo
     * documento, pero documentos distintos no se estorban. Con un cerrojo
     * unico, las miniaturas de una rejilla se rasterizaban en fila de una
     * en una aunque el telefono tuviera ocho nucleos parados.
     */
    private val cerrojoDeMapas = Mutex()
    private val cerrojos = HashMap<String, Mutex>()
    private val renderizadores = LinkedHashMap<String, RenderizadorAbierto>()
    private val cacheMiniaturas = CacheMiniaturas()
    private val firmador = FirmadorPdf()

    private suspend fun cerrojoDe(ruta: String): Mutex =
        cerrojoDeMapas.withLock { cerrojos.getOrPut(ruta) { Mutex() } }

    private class RenderizadorAbierto(
        val descriptor: ParcelFileDescriptor,
        val renderizador: PdfRenderer,
    ) {
        fun cerrar() {
            runCatching { renderizador.close() }
            runCatching { descriptor.close() }
        }
    }

    // --- Apertura y metadatos ------------------------------------------------

    override suspend fun abrir(ruta: String, contrasena: String?): ResultadoPdf<DocumentoPdf> =
        withContext(Dispatchers.IO) {
            val fichero = File(ruta)
            if (!fichero.exists()) {
                return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, ruta)
            }

            try {
                PDDocument.load(fichero, contrasena ?: "").use { documento ->
                    val estabaCifrado = documento.isEncrypted

                    val rutaFinal = if (estabaCifrado) {
                        // Se guarda una copia sin cifrar para poder usar el
                        // renderizador del sistema en el resto de la sesion.
                        documento.setAllSecurityToBeRemoved(true)
                        // La copia conserva el nombre del original y se mete en
                        // una carpeta propia. Antes se llamaba "algo.abierto.pdf"
                        // y ese ".abierto" acababa en la barra del visor y en el
                        // nombre de lo que se guardara despues. La carpeta lleva
                        // la huella de la ruta para que dos documentos que se
                        // llamen igual no se pisen.
                        val huella = fichero.absolutePath.hashCode().toUInt().toString(16)
                        val carpeta = File(directorioTrabajo, "descifrados/$huella")
                        carpeta.mkdirs()
                        val descifrado = File(carpeta, fichero.name)
                        documento.save(descifrado)
                        descifrado.absolutePath
                    } else {
                        ruta
                    }

                    ResultadoPdf.Exito(
                        DocumentoPdf(
                            id = rutaFinal,
                            ruta = rutaFinal,
                            nombre = fichero.name,
                            numeroPaginas = documento.numberOfPages,
                            tamanoBytes = File(rutaFinal).length(),
                            cifrado = estabaCifrado,
                            importadoEn = fichero.lastModified(),
                        ),
                    )
                }
            } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
                ResultadoPdf.Fallo(ErrorPdf.NECESITA_CONTRASENA, e.message)
            } catch (e: OutOfMemoryError) {
                ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
            } catch (e: Exception) {
                ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, e.message)
            }
        }

    override suspend fun paginas(ruta: String, contrasena: String?): ResultadoPdf<List<PaginaPdf>> =
        withContext(Dispatchers.IO) {
            conDocumento(ruta, contrasena) { documento ->
                documento.pages.mapIndexed { indice, pagina ->
                    val caja = pagina.cropBox
                    PaginaPdf(
                        indice = indice,
                        anchoPt = caja.width,
                        altoPt = caja.height,
                        rotacion = ((pagina.rotation % 360) + 360) % 360,
                    )
                }
            }
        }

    // --- Renderizado ---------------------------------------------------------

    override suspend fun renderizarPagina(
        ruta: String,
        indice: Int,
        anchoPx: Int,
        contrasena: String?,
        miniatura: Boolean,
    ): ResultadoPdf<ImageBitmap> = withContext(Dispatchers.IO) {
        val ancho = anchoPx.coerceIn(48, MAXIMO_ANCHO_PX)
        if (miniatura) {
            cacheMiniaturas.buscar(ruta, indice, ancho)?.let {
                return@withContext ResultadoPdf.Exito(it)
            }
        }

        cerrojoDe(ruta).withLock {
            // Otra corrutina ha podido dejarla puesta mientras se esperaba.
            if (miniatura) {
                cacheMiniaturas.buscar(ruta, indice, ancho)?.let {
                    return@withLock ResultadoPdf.Exito(it)
                }
            }
            try {
                val abierto = obtenerRenderizador(ruta)
                    ?: return@withLock ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, ruta)

                if (indice !in 0 until abierto.renderizador.pageCount) {
                    return@withLock ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, "pagina $indice")
                }

                abierto.renderizador.openPage(indice).use { pagina ->
                    val alto = (ancho.toFloat() * pagina.height / pagina.width)
                        .roundToInt()
                        .coerceIn(48, MAXIMO_ANCHO_PX * 4)

                    // Un pico de memoria mientras se parte un documento largo
                    // hacia fallar la miniatura, y la rejilla se quedaba llena
                    // de marcas de error para siempre porque nadie reintentaba.
                    // Soltar lo cacheado y volver a intentarlo convierte un
                    // apuro pasajero en un fotograma mas lento.
                    val lienzo = try {
                        Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                    } catch (sinSitio: OutOfMemoryError) {
                        cacheMiniaturas.vaciar()
                        @Suppress("UNUSED_EXPRESSION") sinSitio
                        Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                    }
                    // PdfRenderer no pinta el fondo: sin esto, una pagina con
                    // zonas transparentes saldria en negro sobre tema oscuro.
                    lienzo.eraseColor(Color.WHITE)
                    pagina.render(lienzo, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    // `PdfRenderer` solo sabe pintar en ARGB_8888: pedirle
                    // RGB_565 responde "Unsupported pixel format". Asi que se
                    // pinta como quiere y la miniatura se queda con la copia de
                    // 16 bits, que es la que se guarda y ocupa la mitad. Sobre
                    // papel no se distingue; la pagina grande sigue entera,
                    // porque ahi se amplia y el degradado de un sello si canta.
                    val mapa = if (miniatura) {
                        lienzo.copy(Bitmap.Config.RGB_565, false)?.also { lienzo.recycle() } ?: lienzo
                    } else {
                        lienzo
                    }
                    val imagen = mapa.asImageBitmap()
                    if (miniatura) cacheMiniaturas.guardar(ruta, indice, ancho, mapa, imagen)
                    ResultadoPdf.Exito(imagen)
                }
            } catch (e: OutOfMemoryError) {
                cacheMiniaturas.vaciar()
                ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
            } catch (e: Exception) {
                ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, e.message)
            }
        }
    }

    /**
     * El renderizador del documento, abriendolo si hace falta.
     *
     * El mapa se toca siempre bajo [cerrojoDeMapas]. Al pasar a un cerrojo por
     * documento, varias corrutinas empezaron a entrar aqui a la vez: el mapa se
     * corrompia y, peor, el desalojo podia cerrar el renderizador que otra
     * estaba usando, que es como se quedaron todas las miniaturas en blanco.
     * Nunca se desaloja el documento que se acaba de pedir.
     */
    private suspend fun obtenerRenderizador(ruta: String): RenderizadorAbierto? =
        cerrojoDeMapas.withLock {
            renderizadores[ruta]?.let { return@withLock it }

            val fichero = File(ruta)
            if (!fichero.exists()) return@withLock null

            val abierto = runCatching {
                val descriptor =
                    ParcelFileDescriptor.open(fichero, ParcelFileDescriptor.MODE_READ_ONLY)
                RenderizadorAbierto(descriptor, PdfRenderer(descriptor))
            }.getOrNull() ?: return@withLock null

            // Cache pequena: cada documento abierto retiene un descriptor.
            while (renderizadores.size >= MAXIMO_DOCUMENTOS_ABIERTOS) {
                val masAntiguo = renderizadores.keys.firstOrNull { it != ruta } ?: break
                renderizadores.remove(masAntiguo)?.cerrar()
            }
            renderizadores[ruta] = abierto
            abierto
        }

    override suspend fun renderizarImagen(
        ruta: String,
        anchoPx: Int,
    ): ResultadoPdf<ImageBitmap> = withContext(Dispatchers.IO) {
        val mapa = cargarImagen(ruta, anchoPx.coerceIn(64, MAXIMO_LADO_IMAGEN))
            ?: return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, ruta)
        ResultadoPdf.Exito(mapa.asImageBitmap())
    }

    override suspend fun cerrar(ruta: String) {
        cacheMiniaturas.olvidar(ruta)
        // Primero se espera a que nadie este renderizando ese documento, y solo
        // entonces se toca el mapa.
        cerrojoDe(ruta).withLock {
            cerrojoDeMapas.withLock { renderizadores.remove(ruta)?.cerrar() }
        }
    }

    // --- Unir, separar, reorganizar ------------------------------------------

    override suspend fun unir(
        entradas: List<EntradaUnion>,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        if (entradas.isEmpty()) {
            return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, "sin entradas")
        }

        try {
            val seleccionaPaginas = entradas.any { it.paginas != null }

            if (!seleccionaPaginas) {
                // Camino rapido: PDFMergerUtility trabaja en flujo y no carga
                // los documentos completos en memoria.
                val union = PDFMergerUtility().apply {
                    destinationFileName = rutaSalida
                }
                entradas.forEach { union.addSource(File(it.ruta)) }
                // Hasta 20 MB en memoria y el resto en fichero temporal: unir
                // varios PDF grandes en un movil no cabe entero en el heap.
                union.mergeDocuments(MemoryUsageSetting.setupMixed(20L * 1024 * 1024))
                return@withContext ResultadoPdf.Exito(rutaSalida)
            }

            // Los origenes se mantienen abiertos hasta despues de guardar. Una
            // PDPage anadida a otro documento sigue apuntando a los flujos del
            // suyo, asi que cerrarlo antes del save deja el resultado a medias
            // con un "COSStream has been closed".
            val abiertos = mutableListOf<PDDocument>()
            try {
                PDDocument().use { destino ->
                    entradas.forEach { entrada ->
                        val origen = PDDocument.load(File(entrada.ruta), entrada.contrasena ?: "")
                        abiertos += origen
                        val indices = entrada.paginas ?: (0 until origen.numberOfPages).toList()
                        indices.filter { it in 0 until origen.numberOfPages }.forEach { indice ->
                            destino.addPage(origen.getPage(indice))
                        }
                    }
                    if (destino.numberOfPages == 0) {
                        return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, "sin paginas")
                    }
                    destino.save(File(rutaSalida))
                }
            } finally {
                abiertos.forEach { runCatching { it.close() } }
            }
            ResultadoPdf.Exito(rutaSalida)
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
        }
    }

    override suspend fun extraerPaginas(
        ruta: String,
        paginas: List<Int>,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        if (paginas.isEmpty()) {
            return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, "sin paginas")
        }
        copiarPaginas(rutaSalida) { destino ->
            PDDocument.load(File(ruta)).also { origen ->
                paginas.filter { it in 0 until origen.numberOfPages }.forEach { indice ->
                    destino.addPage(origen.getPage(indice))
                }
                if (destino.numberOfPages == 0) error("ninguna pagina valida")
            }
        }
    }

    override suspend fun separar(
        ruta: String,
        rangos: List<RangoPaginas>,
        directorioSalida: String,
        nombreBase: String,
        alAvanzar: ((Int, Int) -> Unit)?,
    ): ResultadoPdf<List<String>> = withContext(Dispatchers.IO) {
        if (rangos.isEmpty()) {
            return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, "sin rangos")
        }

        try {
            File(directorioSalida).mkdirs()
            val generados = mutableListOf<String>()

            PDDocument.load(File(ruta)).use { origen ->
                rangos.forEachIndexed { posicion, rango ->
                    val indices = rango.paginas.filter { it in 0 until origen.numberOfPages }
                    if (indices.isEmpty()) return@forEachIndexed

                    val sufijo = if (indices.size == 1) {
                        "${indices.first() + 1}"
                    } else {
                        "${indices.first() + 1}-${indices.last() + 1}"
                    }
                    val salida = File(directorioSalida, "$nombreBase $sufijo.pdf")

                    PDDocument().use { parte ->
                        indices.forEach { parte.addPage(origen.getPage(it)) }
                        parte.save(salida)
                    }
                    generados += salida.absolutePath
                    alAvanzar?.invoke(generados.size, rangos.size)
                    currentCoroutineContext().ensureActive()
                    // El indice de posicion se conserva por si en el futuro hace
                    // falta numerar los cortes en vez de sus paginas.
                    @Suppress("UNUSED_EXPRESSION") posicion
                }
            }

            if (generados.isEmpty()) {
                ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, "ningun rango valido")
            } else {
                ResultadoPdf.Exito(generados)
            }
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
        }
    }

    override suspend fun reorganizar(
        ruta: String,
        ordenPaginas: List<Int>,
        rotaciones: Map<Int, Int>,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        copiarPaginas(rutaSalida) { destino ->
            PDDocument.load(File(ruta)).also { origen ->
                ordenPaginas.filter { it in 0 until origen.numberOfPages }.forEach { indice ->
                    val pagina = origen.getPage(indice)
                    rotaciones[indice]?.let { giro ->
                        pagina.rotation = ((pagina.rotation + giro) % 360 + 360) % 360
                    }
                    destino.addPage(pagina)
                }
                if (destino.numberOfPages == 0) error("documento vacio")
            }
        }
    }

    // --- Imagenes a PDF ------------------------------------------------------

    override suspend fun imagenesAPdf(
        imagenes: List<String>,
        disposicion: DisposicionImagenes,
        tamano: TamanoPagina,
        orientacion: Orientacion,
        margenPt: Float,
        espaciadoPt: Float,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        if (imagenes.isEmpty()) {
            return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, "sin imagenes")
        }

        escribirDocumento(rutaSalida) { documento ->
            imagenes.chunked(disposicion.porPagina).forEach { grupo ->
                val mapas = grupo.mapNotNull { cargarImagen(it) }
                if (mapas.isEmpty()) return@forEach

                val cajaPagina = calcularCajaPagina(tamano, orientacion, disposicion, mapas.first())
                val pagina = PDPage(cajaPagina)
                documento.addPage(pagina)

                PDPageContentStream(documento, pagina).use { flujo ->
                    val celdas = calcularCeldas(cajaPagina, disposicion, margenPt, espaciadoPt)
                    mapas.forEachIndexed { indice, mapa ->
                        val celda = celdas.getOrNull(indice) ?: return@forEachIndexed
                        val destino = encajar(mapa.width, mapa.height, celda)
                        val objeto = incrustar(documento, mapa)
                        flujo.drawImage(objeto, destino.x, destino.y, destino.ancho, destino.alto)
                    }
                }
                mapas.forEach { it.recycle() }
            }
            if (documento.numberOfPages == 0) error("ninguna imagen legible")
        }
    }

    /**
     * Carga la imagen submuestreada y ya girada segun su EXIF.
     *
     * Sin lo primero, una rafaga de fotos de 50 megapixeles agota la memoria del
     * telefono; sin lo segundo, las fotos hechas en vertical salen tumbadas,
     * porque la camara guarda el sensor en horizontal y anota el giro aparte.
     */
    private fun cargarImagen(ruta: String, ladoMaximo: Int = MAXIMO_LADO_IMAGEN): Bitmap? {
        val fichero = File(ruta)
        if (!fichero.exists()) return null

        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichero.absolutePath, medidas)
        if (medidas.outWidth <= 0) return null

        var muestreo = 1
        while (
            (medidas.outWidth / muestreo) > ladoMaximo ||
            (medidas.outHeight / muestreo) > ladoMaximo
        ) {
            muestreo *= 2
        }

        val mapa = BitmapFactory.decodeFile(
            fichero.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = muestreo },
        ) ?: return null

        val giro = runCatching {
            when (
                ExifInterface(fichero.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        if (giro == 0f) return mapa

        val matriz = android.graphics.Matrix().apply { postRotate(giro) }
        val girado = Bitmap.createBitmap(mapa, 0, 0, mapa.width, mapa.height, matriz, true)
        if (girado != mapa) mapa.recycle()
        return girado
    }

    /**
     * Incrusta la imagen eligiendo compresion.
     *
     * Una foto se guarda como JPEG, que es como ya venia y ocupa una decima
     * parte; un grafico o una captura se guarda sin perdida, porque el JPEG le
     * ensuciaria los bordes del texto. El criterio es el numero de colores
     * distintos, que separa bien fotografia de grafico.
     */
    private fun incrustar(documento: PDDocument, mapa: Bitmap) =
        if (pareceFotografia(mapa)) {
            JPEGFactory.createFromImage(documento, mapa, 0.85f)
        } else {
            LosslessFactory.createFromImage(documento, mapa)
        }

    private fun pareceFotografia(mapa: Bitmap): Boolean {
        val paso = max(1, min(mapa.width, mapa.height) / 32)
        val colores = HashSet<Int>()
        var x = 0
        while (x < mapa.width && colores.size <= UMBRAL_COLORES) {
            var y = 0
            while (y < mapa.height && colores.size <= UMBRAL_COLORES) {
                colores += mapa.getPixel(x, y)
                y += paso
            }
            x += paso
        }
        return colores.size > UMBRAL_COLORES
    }

    private fun calcularCajaPagina(
        tamano: TamanoPagina,
        orientacion: Orientacion,
        disposicion: DisposicionImagenes,
        primera: Bitmap,
    ): PDRectangle {
        if (tamano == TamanoPagina.AJUSTAR_A_IMAGEN && disposicion == DisposicionImagenes.UNA_POR_PAGINA) {
            // La pagina toma la proporcion de la imagen: ni margenes blancos ni
            // recortes. Se fija el lado mayor a 842 pt (el alto de un A4).
            val escala = 842f / max(primera.width, primera.height)
            return PDRectangle(primera.width * escala, primera.height * escala)
        }

        val base = if (tamano == TamanoPagina.AJUSTAR_A_IMAGEN) TamanoPagina.A4 else tamano
        val apaisada = when (orientacion) {
            Orientacion.HORIZONTAL -> true
            Orientacion.VERTICAL -> false
            Orientacion.AUTOMATICA -> primera.width > primera.height &&
                disposicion == DisposicionImagenes.UNA_POR_PAGINA
        }
        return if (apaisada) {
            PDRectangle(base.altoPt, base.anchoPt)
        } else {
            PDRectangle(base.anchoPt, base.altoPt)
        }
    }

    private fun calcularCeldas(
        caja: PDRectangle,
        disposicion: DisposicionImagenes,
        margen: Float,
        espaciado: Float,
    ): List<RectanguloPt> {
        val filas = disposicion.filas
        val columnas = disposicion.columnas
        val anchoUtil = caja.width - margen * 2 - espaciado * (columnas - 1)
        val altoUtil = caja.height - margen * 2 - espaciado * (filas - 1)
        val anchoCelda = anchoUtil / columnas
        val altoCelda = altoUtil / filas

        return buildList {
            for (fila in 0 until filas) {
                for (columna in 0 until columnas) {
                    add(
                        RectanguloPt(
                            x = margen + columna * (anchoCelda + espaciado),
                            // Se rellena de arriba abajo, que es el orden en que
                            // el usuario ve las miniaturas.
                            y = caja.height - margen - (fila + 1) * altoCelda - fila * espaciado,
                            ancho = anchoCelda,
                            alto = altoCelda,
                        ),
                    )
                }
            }
        }
    }

    /** Encaja la imagen dentro de la celda sin deformarla ni recortarla. */
    private fun encajar(anchoImagen: Int, altoImagen: Int, celda: RectanguloPt): RectanguloPt {
        val escala = min(celda.ancho / anchoImagen, celda.alto / altoImagen)
        val ancho = anchoImagen * escala
        val alto = altoImagen * escala
        return RectanguloPt(
            x = celda.x + (celda.ancho - ancho) / 2f,
            y = celda.y + (celda.alto - alto) / 2f,
            ancho = ancho,
            alto = alto,
        )
    }

    // --- Ediciones -----------------------------------------------------------

    override suspend fun aplicarEdiciones(
        borrador: BorradorEdicion,
        rutaSalida: String,
        contrasena: String?,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(File(borrador.rutaDocumento), contrasena ?: "").use { documento ->
                val fuentes = FuentesPdf(documento)
                val pintor = PintorEdiciones(documento, fuentes)

                borrador.paginas.values.filter { it.tieneCambios }.forEach { edicionPagina ->
                    val indice = edicionPagina.indice
                    if (indice !in 0 until documento.numberOfPages) return@forEach
                    val pagina = documento.getPage(indice)
                    val transformador = TransformadorPagina(pagina)

                    if (edicionPagina.filtro != FiltroPagina.NINGUNO) {
                        aplanarConFiltro(
                            documento = documento,
                            pagina = pagina,
                            transformador = transformador,
                            rutaOrigen = borrador.rutaDocumento,
                            indice = indice,
                            edicionPagina = edicionPagina,
                        )
                    }

                    // El recorte va despues de pintar: si se hiciera antes,
                    // el transformador estaria trabajando con una caja que ya
                    // no es la que se vio al editar.
                    if (edicionPagina.ediciones.isNotEmpty()) {
                        PDPageContentStream(
                            documento,
                            pagina,
                            PDPageContentStream.AppendMode.APPEND,
                            true,
                            true,
                        ).use { flujo ->
                            flujo.saveGraphicsState()
                            flujo.transform(transformador.matriz)
                            pintor.pintar(flujo, transformador, edicionPagina.ediciones)
                            flujo.restoreGraphicsState()
                        }
                    }
                    edicionPagina.recorte?.let { marco ->
                        val caja = transformador.aRectanguloVisible(marco)
                        pagina.cropBox = PDRectangle(caja.x, caja.y, caja.ancho, caja.alto)
                    }
                }

                documento.save(File(rutaSalida))
            }
            ResultadoPdf.Exito(rutaSalida)
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
        }
    }

    /**
     * Sustituye el contenido de la pagina por su version filtrada.
     *
     * Un filtro de imagen no se puede aplicar al contenido vectorial de un PDF:
     * hay que rasterizar. Se hace solo en las paginas que el usuario marca, y la
     * pantalla avisa de que esas paginas pierden el texto seleccionable.
     */
    private fun aplanarConFiltro(
        documento: PDDocument,
        pagina: PDPage,
        transformador: TransformadorPagina,
        rutaOrigen: String,
        indice: Int,
        edicionPagina: es.ghatostudio.nexapdf.domain.model.EdicionPagina,
    ) {
        val anchoPx = (transformador.anchoVisible * DPI_FILTRO / 72f).roundToInt()
            .coerceIn(200, MAXIMO_ANCHO_PX)
        val original = rasterizarConSistema(rutaOrigen, indice, anchoPx) ?: return
        val filtrada = FiltrosPagina.aplicar(
            original,
            edicionPagina.filtro,
            edicionPagina.intensidadFiltro,
        )

        val objeto = JPEGFactory.createFromImage(documento, filtrada, 0.9f)
        PDPageContentStream(
            documento,
            pagina,
            PDPageContentStream.AppendMode.OVERWRITE,
            true,
            true,
        ).use { flujo ->
            flujo.saveGraphicsState()
            flujo.transform(transformador.matriz)
            flujo.drawImage(objeto, 0f, 0f, transformador.anchoVisible, transformador.altoVisible)
            flujo.restoreGraphicsState()
        }

        if (filtrada != original) filtrada.recycle()
        original.recycle()
    }

    private fun rasterizarConSistema(ruta: String, indice: Int, anchoPx: Int): Bitmap? = runCatching {
        ParcelFileDescriptor.open(File(ruta), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderizador ->
                renderizador.openPage(indice).use { pagina ->
                    val alto = (anchoPx.toFloat() * pagina.height / pagina.width).roundToInt()
                    val mapa = Bitmap.createBitmap(anchoPx, alto, Bitmap.Config.ARGB_8888)
                    mapa.eraseColor(Color.WHITE)
                    pagina.render(mapa, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    mapa
                }
            }
        }
    }.getOrNull()

    // --- Texto ---------------------------------------------------------------

    override suspend fun bloquesDeTexto(
        ruta: String,
        indice: Int,
        contrasena: String?,
    ): ResultadoPdf<List<BloqueTexto>> = withContext(Dispatchers.IO) {
        conDocumento(ruta, contrasena) { documento ->
            if (indice !in 0 until documento.numberOfPages) return@conDocumento emptyList()
            val pagina = documento.getPage(indice)
            val transformador = TransformadorPagina(pagina)
            val extractor = ExtractorBloques(transformador)
            extractor.setStartPage(indice + 1)
            extractor.setEndPage(indice + 1)
            extractor.getText(documento)
            extractor.bloques
        }
    }

    /**
     * Recoge lineas de texto con su posicion en la pagina.
     *
     * PDFTextStripper entrega el texto ya ordenado junto con la posicion de cada
     * caracter; agrupando por linea se obtiene un rectangulo sobre el que el
     * usuario puede tocar para sustituir ese texto.
     */
    private class ExtractorBloques(
        private val transformador: TransformadorPagina,
    ) : PDFTextStripper() {

        val bloques = mutableListOf<BloqueTexto>()
        private var contador = 0

        init {
            sortByPosition = true
        }

        override fun writeString(texto: String, posiciones: MutableList<TextPosition>) {
            super.writeString(texto, posiciones)
            if (texto.isBlank() || posiciones.isEmpty()) return

            var izquierda = Float.MAX_VALUE
            var derecha = Float.MIN_VALUE
            var arriba = Float.MAX_VALUE
            var abajo = Float.MIN_VALUE
            var alturaMaxima = 0f

            posiciones.forEach { posicion ->
                izquierda = min(izquierda, posicion.xDirAdj)
                derecha = max(derecha, posicion.xDirAdj + posicion.widthDirAdj)
                arriba = min(arriba, posicion.yDirAdj - posicion.heightDir)
                abajo = max(abajo, posicion.yDirAdj)
                alturaMaxima = max(alturaMaxima, posicion.heightDir)
            }

            // PDFTextStripper ya devuelve coordenadas en el espacio visible con
            // el origen arriba, que es justo el sistema del editor.
            val ancho = transformador.anchoVisible
            val alto = transformador.altoVisible
            if (ancho <= 0f || alto <= 0f) return

            bloques += BloqueTexto(
                id = "bloque-${contador++}",
                texto = texto.trim(),
                marco = Rectangulo(
                    izquierda = (izquierda / ancho).coerceIn(0f, 1f),
                    arriba = (arriba / alto).coerceIn(0f, 1f),
                    derecha = (derecha / ancho).coerceIn(0f, 1f),
                    abajo = (abajo / alto).coerceIn(0f, 1f),
                ),
                tamanoFuentePt = alturaMaxima,
            )
        }
    }

    // --- Busqueda e indice ---------------------------------------------------

    override suspend fun buscarTexto(
        ruta: String,
        consulta: String,
        contrasena: String?,
    ): ResultadoPdf<List<Coincidencia>> = withContext(Dispatchers.IO) {
        val aguja = consulta.trim()
        if (aguja.isEmpty()) return@withContext ResultadoPdf.Exito(emptyList())

        conDocumento(ruta, contrasena) { documento ->
            val encontradas = mutableListOf<Coincidencia>()

            for (indice in 0 until documento.numberOfPages) {
                // PDFTextStripper no mira si la corrutina sigue viva, asi
                // que se comprueba aqui: sin esto, una busqueda cancelada
                // en un documento de quinientas paginas seguia ocupando el
                // hilo hasta el final aunque ya no la quisiera nadie.
                currentCoroutineContext().ensureActive()
                if (encontradas.size >= MAXIMO_COINCIDENCIAS) break
                val pagina = documento.getPage(indice)
                val transformador = TransformadorPagina(pagina)
                val buscador = BuscadorPosicional(aguja, indice, transformador, encontradas)
                buscador.setStartPage(indice + 1)
                buscador.setEndPage(indice + 1)
                runCatching { buscador.getText(documento) }
            }
            encontradas
        }
    }

    /**
     * Encuentra el texto y ademas dice donde esta.
     *
     * PDFTextStripper entrega cada linea junto con la posicion de cada
     * caracter. Buscando dentro de la linea y quedandose con las posiciones que
     * abarca la coincidencia sale el rectangulo exacto, que es lo que hace
     * falta para resaltarla sobre la pagina.
     */
    private class BuscadorPosicional(
        private val aguja: String,
        private val indicePagina: Int,
        private val transformador: TransformadorPagina,
        private val destino: MutableList<Coincidencia>,
    ) : PDFTextStripper() {

        init {
            sortByPosition = true
        }

        override fun writeString(texto: String, posiciones: MutableList<TextPosition>) {
            if (texto.isBlank() || posiciones.isEmpty()) return

            var desde = 0
            while (destino.size < MAXIMO_COINCIDENCIAS) {
                val encontrado = texto.indexOf(aguja, desde, ignoreCase = true)
                if (encontrado < 0) break

                val primera = encontrado.coerceIn(0, posiciones.lastIndex)
                val ultima = (encontrado + aguja.length - 1).coerceIn(0, posiciones.lastIndex)
                val abarcadas = posiciones.subList(primera, ultima + 1)

                var izquierda = Float.MAX_VALUE
                var derecha = -Float.MAX_VALUE
                var arriba = Float.MAX_VALUE
                var abajo = -Float.MAX_VALUE
                abarcadas.forEach { posicion ->
                    izquierda = min(izquierda, posicion.xDirAdj)
                    derecha = max(derecha, posicion.xDirAdj + posicion.widthDirAdj)
                    arriba = min(arriba, posicion.yDirAdj - posicion.heightDir)
                    abajo = max(abajo, posicion.yDirAdj)
                }

                val ancho = transformador.anchoVisible
                val alto = transformador.altoVisible
                if (ancho > 0f && alto > 0f) {
                    destino += Coincidencia(
                        pagina = indicePagina,
                        fragmento = fragmento(texto, encontrado),
                        marco = Rectangulo(
                            izquierda = (izquierda / ancho).coerceIn(0f, 1f),
                            arriba = (arriba / alto).coerceIn(0f, 1f),
                            derecha = (derecha / ancho).coerceIn(0f, 1f),
                            abajo = (abajo / alto).coerceIn(0f, 1f),
                        ),
                    )
                }
                desde = encontrado + aguja.length
            }
        }

        private fun fragmento(texto: String, posicion: Int): String {
            val inicio = (posicion - CONTEXTO).coerceAtLeast(0)
            val fin = (posicion + aguja.length + CONTEXTO).coerceAtMost(texto.length)
            val trozo = texto.substring(inicio, fin).replace(Regex("\\s+"), " ").trim()
            val prefijo = if (inicio > 0) "\u2026" else ""
            val sufijo = if (fin < texto.length) "\u2026" else ""
            return prefijo + trozo + sufijo
        }

        private companion object {
            const val MAXIMO_COINCIDENCIAS = 200
            const val CONTEXTO = 40
        }
    }

    override suspend fun cifrar(
        ruta: String,
        contrasenaApertura: String,
        contrasenaPermisos: String,
        permisos: PermisosPdf,
        rutaSalida: String,
        contrasenaActual: String?,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        if (contrasenaApertura.isBlank()) {
            return@withContext ResultadoPdf.Fallo(ErrorPdf.OPERACION_NO_PERMITIDA, "sin contrasena")
        }
        try {
            PDDocument.load(File(ruta), contrasenaActual ?: "").use { documento ->
                // Si venia cifrado se le quita lo anterior antes de poner lo
                // nuevo: encadenar politicas sobre un documento ya protegido
                // deja un fichero que unos lectores abren y otros no.
                if (documento.isEncrypted) documento.setAllSecurityToBeRemoved(true)

                val acceso = AccessPermission().apply {
                    setCanPrint(permisos.permitirImprimir)
                    setCanPrintFaithful(permisos.permitirImprimir)
                    setCanExtractContent(permisos.permitirCopiar)
                    setCanExtractForAccessibility(true)
                    setCanModify(permisos.permitirModificar)
                    setCanModifyAnnotations(permisos.permitirAnotar)
                    setCanFillInForm(permisos.permitirAnotar)
                    setCanAssembleDocument(permisos.permitirModificar)
                }
                val duena = contrasenaPermisos.ifBlank { contrasenaApertura }
                val politica = StandardProtectionPolicy(duena, contrasenaApertura, acceso).apply {
                    // 256 bits es lo que usa AES-256 en PDF 2.0 y lo que
                    // entiende cualquier lector actual. Con 128 el fichero se
                    // abre en lectores muy viejos, pero la proteccion vale la
                    // mitad y no compensa.
                    encryptionKeyLength = 256
                }
                documento.protect(politica)
                documento.save(File(rutaSalida))
            }
            ResultadoPdf.Exito(rutaSalida)
        } catch (e: InvalidPasswordException) {
            ResultadoPdf.Fallo(ErrorPdf.NECESITA_CONTRASENA, e.message)
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
        }
    }

    override suspend fun descifrar(
        ruta: String,
        contrasena: String,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(File(ruta), contrasena).use { documento ->
                documento.setAllSecurityToBeRemoved(true)
                documento.save(File(rutaSalida))
            }
            ResultadoPdf.Exito(rutaSalida)
        } catch (e: InvalidPasswordException) {
            ResultadoPdf.Fallo(ErrorPdf.NECESITA_CONTRASENA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
        }
    }

    override suspend fun esquema(
        ruta: String,
        contrasena: String?,
    ): ResultadoPdf<List<Seccion>> = withContext(Dispatchers.IO) {
        conDocumento(ruta, contrasena) { documento ->
            val raiz = documento.documentCatalog?.documentOutline
                ?: return@conDocumento emptyList()
            val secciones = mutableListOf<Seccion>()
            recorrerEsquema(documento, raiz.children(), 0, secciones)
            secciones
        }
    }

    /** Recorre el arbol de marcadores en profundidad, respetando el orden. */
    private fun recorrerEsquema(
        documento: PDDocument,
        nodos: Iterable<PDOutlineItem>,
        nivel: Int,
        destino: MutableList<Seccion>,
    ) {
        if (nivel > MAXIMO_NIVEL_ESQUEMA) return
        nodos.forEach { nodo ->
            if (destino.size >= MAXIMO_SECCIONES) return
            val titulo = nodo.title?.trim().orEmpty()
            val pagina = runCatching {
                nodo.findDestinationPage(documento)?.let { documento.pages.indexOf(it) }
            }.getOrNull() ?: -1
            if (titulo.isNotEmpty() && pagina >= 0) {
                destino += Seccion(titulo = titulo, pagina = pagina, nivel = nivel)
            }
            recorrerEsquema(documento, nodo.children(), nivel + 1, destino)
        }
    }

    // --- Firma ---------------------------------------------------------------

    override suspend fun firmarConCertificado(
        ruta: String,
        origen: OrigenCertificado,
        apariencia: AparienciaFirma?,
        motivo: String?,
        lugar: String?,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        val credenciales = when (origen) {
            is OrigenCertificado.Fichero ->
                firmador.abrirCertificado(origen.contenido, origen.contrasena)

            is OrigenCertificado.AlmacenDelSistema ->
                firmador.credencialesDelSistema(contexto, origen.alias)
        } ?: return@withContext ResultadoPdf.Fallo(ErrorPdf.CERTIFICADO_INVALIDO)

        try {
            // Si hay firma visible se estampa antes: asi el dibujo queda dentro
            // de lo que la firma criptografica protege, y no encima de ella.
            val ficheroOrigen = if (apariencia?.imagenPng != null) {
                val intermedio = File(directorioTrabajo, "firma_visible_${System.nanoTime()}.pdf")
                estamparApariencia(ruta, apariencia, intermedio)
                intermedio
            } else {
                File(ruta)
            }

            PDDocument.load(ficheroOrigen).use { documento ->
                File(rutaSalida).outputStream().use { salida ->
                    firmador.firmar(
                        documento = documento,
                        origen = ficheroOrigen,
                        salida = salida,
                        credenciales = credenciales,
                        nombre = apariencia?.nombreVisible?.takeIf { it.isNotBlank() }
                            ?: credenciales.titular,
                        motivo = motivo,
                        lugar = lugar,
                    )
                }
            }

            if (ficheroOrigen.absolutePath != ruta) ficheroOrigen.delete()
            ResultadoPdf.Exito(rutaSalida)
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
        }
    }

    private fun estamparApariencia(ruta: String, apariencia: AparienciaFirma, destino: File) {
        PDDocument.load(File(ruta)).use { documento ->
            val indice = apariencia.paginaIndice.coerceIn(0, documento.numberOfPages - 1)
            val pagina = documento.getPage(indice)
            val transformador = TransformadorPagina(pagina)
            val marco = transformador.aRectanguloVisible(apariencia.marco)

            val mapa = apariencia.imagenPng?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            PDPageContentStream(
                documento,
                pagina,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true,
            ).use { flujo ->
                flujo.saveGraphicsState()
                flujo.transform(transformador.matriz)
                if (mapa != null) {
                    val objeto = LosslessFactory.createFromImage(documento, mapa)
                    flujo.drawImage(objeto, marco.x, marco.y, marco.ancho, marco.alto)
                }
                flujo.restoreGraphicsState()
            }
            mapa?.recycle()
            documento.save(destino)
        }
    }

    override suspend fun firmasExistentes(ruta: String): ResultadoPdf<List<FirmaExistente>> =
        withContext(Dispatchers.IO) {
            conDocumento(ruta, null) { documento ->
                documento.signatureDictionaries.map { firma: PDSignature ->
                    val delCertificado = datosDelSobre(firma)
                    FirmaExistente(
                        nombre = firma.name ?: "",
                        motivo = firma.reason,
                        lugar = firma.location,
                        fechaEpochMillis = firma.signDate?.timeInMillis,
                        // Un rango de bytes que llega al final del fichero indica
                        // que la firma cubre todo el documento.
                        cubreTodoElDocumento = firma.byteRange.let { rango ->
                            rango.size == 4 &&
                                (rango[2] + rango[3]).toLong() >= File(ruta).length() - 2
                        },
                        firmante = delCertificado?.firmante,
                        emisor = delCertificado?.emisor,
                        numeroSerie = delCertificado?.numeroSerie,
                        validoDesdeEpochMillis = delCertificado?.desde,
                        validoHastaEpochMillis = delCertificado?.hasta,
                        algoritmo = delCertificado?.algoritmo,
                        formato = formatoLegible(firma.subFilter),
                        conSelloDeTiempo = delCertificado?.conSello == true,
                    )
                }
            }
        }

    /** Lo que se saca de abrir el sobre CMS de una firma. */
    private class DatosDelSobre(
        val firmante: String?,
        val emisor: String?,
        val numeroSerie: String?,
        val desde: Long?,
        val hasta: Long?,
        val algoritmo: String?,
        val conSello: Boolean,
    )

    /**
     * Abre el PKCS#7 de una firma y saca de el lo que interesa ensenar.
     *
     * Devuelve `null` sin quejarse si el sobre no se puede leer: un PDF puede
     * traer una firma en un formato que esta biblioteca no entienda, y eso no es
     * motivo para que la lista de firmas deje de funcionar.
     */
    private fun datosDelSobre(firma: PDSignature): DatosDelSobre? = runCatching {
        val sobre = CMSSignedData(firma.contents ?: return@runCatching null)
        val firmanteCms = sobre.signerInfos.signers.firstOrNull() ?: return@runCatching null
        // `sid` identifica al firmante por emisor y numero de serie; el
        // almacen del sobre lo admite como selector, pero hay que decirle de
        // que tipo son los certificados que guarda.
        @Suppress("UNCHECKED_CAST")
        val almacen = sobre.certificates as Store<X509CertificateHolder>
        val certificado = almacen.getMatches(firmanteCms.sid as Selector<X509CertificateHolder>)
            .firstOrNull()

        DatosDelSobre(
            firmante = certificado?.subject?.let { nombreComunDe(it.toString()) },
            emisor = certificado?.issuer?.let { nombreComunDe(it.toString()) },
            numeroSerie = certificado?.serialNumber?.toString(16)?.uppercase(),
            desde = certificado?.notBefore?.time,
            hasta = certificado?.notAfter?.time,
            algoritmo = algoritmoLegible(firmanteCms.digestAlgOID, firmanteCms.encryptionAlgOID),
            conSello = firmanteCms.unsignedAttributes
                ?.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken) != null,
        )
    }.getOrNull()

    /** El CN de un nombre distinguido, que es lo unico que se lee de un tiron. */
    private fun nombreComunDe(distinguido: String): String =
        distinguido.split(',')
            .map { it.trim() }
            .firstOrNull { it.startsWith("CN=", ignoreCase = true) }
            ?.removeRange(0, 3)
            ?: distinguido

    /**
     * Traduce los OID a algo que se pueda leer.
     *
     * Ensenar "1.2.840.113549.1.1.11" no le dice nada a nadie, y ademas los
     * certificados espanoles usan siempre los mismos tres o cuatro.
     */
    private fun algoritmoLegible(oidResumen: String, oidClave: String): String {
        val resumen = when (oidResumen) {
            "2.16.840.1.101.3.4.2.1" -> "SHA-256"
            "2.16.840.1.101.3.4.2.2" -> "SHA-384"
            "2.16.840.1.101.3.4.2.3" -> "SHA-512"
            "1.3.14.3.2.26" -> "SHA-1"
            else -> oidResumen
        }
        val clave = when {
            oidClave.startsWith("1.2.840.113549.1.1") -> "RSA"
            oidClave.startsWith("1.2.840.10045") -> "ECDSA"
            oidClave.startsWith("1.2.840.113549.1.1.10") -> "RSA-PSS"
            else -> oidClave
        }
        return "$resumen / $clave"
    }

    /** El subfiltro, dicho en los terminos de la norma en vez de en los del PDF. */
    private fun formatoLegible(subFiltro: String?): String? = when (subFiltro) {
        null -> null
        "ETSI.CAdES.detached" -> "PAdES (ETSI.CAdES.detached)"
        "ETSI.RFC3161" -> "Sello de tiempo (ETSI.RFC3161)"
        "adbe.pkcs7.detached" -> "PKCS#7 (adbe.pkcs7.detached)"
        "adbe.pkcs7.sha1" -> "PKCS#7 SHA-1 (adbe.pkcs7.sha1)"
        else -> subFiltro
    }

    // --- Utilidades ----------------------------------------------------------

    private inline fun <T> conDocumento(
        ruta: String,
        contrasena: String?,
        bloque: (PDDocument) -> T,
    ): ResultadoPdf<T> = try {
        PDDocument.load(File(ruta), contrasena ?: "").use { ResultadoPdf.Exito(bloque(it)) }
    } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
        ResultadoPdf.Fallo(ErrorPdf.NECESITA_CONTRASENA, e.message)
    } catch (e: OutOfMemoryError) {
        ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
    } catch (e: Exception) {
        ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, e.message)
    }

    /**
     * Crea un documento con paginas tomadas de otro y lo guarda.
     *
     * El bloque devuelve el documento de origen y esta funcion lo cierra
     * *despues* de guardar. El orden importa: una PDPage anadida a otro
     * documento sigue leyendo sus flujos de contenido del documento original,
     * de modo que cerrarlo antes del `save` produce un fichero invalido.
     */
    private inline fun copiarPaginas(
        rutaSalida: String,
        bloque: (PDDocument) -> PDDocument,
    ): ResultadoPdf<String> = try {
        var origen: PDDocument? = null
        try {
            PDDocument().use { destino ->
                origen = bloque(destino)
                File(rutaSalida).parentFile?.mkdirs()
                destino.save(File(rutaSalida))
            }
        } finally {
            runCatching { origen?.close() }
        }
        ResultadoPdf.Exito(rutaSalida)
    } catch (e: OutOfMemoryError) {
        ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
    } catch (e: Exception) {
        ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
    }

    private inline fun escribirDocumento(
        rutaSalida: String,
        bloque: (PDDocument) -> Unit,
    ): ResultadoPdf<String> = try {
        PDDocument().use { documento ->
            bloque(documento)
            File(rutaSalida).parentFile?.mkdirs()
            documento.save(File(rutaSalida))
        }
        ResultadoPdf.Exito(rutaSalida)
    } catch (e: OutOfMemoryError) {
        ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
    } catch (e: Exception) {
        ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
    }

    private companion object {
        /** Tope de resultados: mas de esto no se lee, solo se hace esperar. */
        const val MAXIMO_COINCIDENCIAS = 200
        const val MAXIMO_SECCIONES = 500
        const val MAXIMO_NIVEL_ESQUEMA = 4
        const val CONTEXTO = 40

        /** Mas alla de esto una pagina no gana nitidez visible y si consume memoria. */
        const val MAXIMO_ANCHO_PX = 2400

        /** Descriptores de fichero abiertos a la vez. */
        const val MAXIMO_DOCUMENTOS_ABIERTOS = 3

        /** Resolucion a la que se rasteriza una pagina al aplicarle un filtro. */
        const val DPI_FILTRO = 200f

        /** Lado maximo al que se reduce una foto antes de incrustarla. */
        const val MAXIMO_LADO_IMAGEN = 2600

        /** Colores distintos a partir de los cuales se considera fotografia. */
        const val UMBRAL_COLORES = 180
    }
}

/**
 * Miniaturas ya rasterizadas, acotadas por memoria.
 *
 * Se acota por bytes y no por numero de entradas porque una miniatura de 320 px
 * pesa un cuarto de mega y una de 1080 px seis veces mas: contar entradas daria
 * un techo que no significa nada.
 *
 * El limite es una dieciseisava parte del monton. Con una octava la memoria de
 * la aplicacion subia de 215 a 232 MB, y no servia de nada: una pantalla de
 * miniaturas son seis, poco mas de un mega y medio, asi que con dieciseis MB
 * caben nueve pantallas y sobra. Guardar mas solo era retener paginas que nadie
 * iba a volver a mirar.
 */
private class CacheMiniaturas {

    private data class Clave(val ruta: String, val indice: Int, val ancho: Int)

    private class Entrada(val imagen: ImageBitmap, val bytes: Int)

    private val limiteBytes = (Runtime.getRuntime().maxMemory() / 16).toInt()
    private var ocupado = 0

    // accessOrder = true: al leer una entrada pasa al final, asi que la primera
    // de la lista es siempre la que lleva mas tiempo sin usarse.
    private val entradas = object : LinkedHashMap<Clave, Entrada>(16, 0.75f, true) {}

    @Synchronized
    fun buscar(ruta: String, indice: Int, ancho: Int): ImageBitmap? =
        entradas[Clave(ruta, indice, ancho)]?.imagen

    @Synchronized
    fun guardar(ruta: String, indice: Int, ancho: Int, mapa: Bitmap, imagen: ImageBitmap) {
        val bytes = mapa.allocationByteCount
        // Una sola miniatura que ya no cabe no tiene sentido guardarla: dejaria
        // la cache vacia de todo lo demas para nada.
        if (bytes > limiteBytes / 2) return
        val previa = entradas.put(Clave(ruta, indice, ancho), Entrada(imagen, bytes))
        ocupado += bytes - (previa?.bytes ?: 0)
        val iterador = entradas.entries.iterator()
        while (ocupado > limiteBytes && iterador.hasNext()) {
            ocupado -= iterador.next().value.bytes
            iterador.remove()
        }
    }

    /** Al reescribir un documento, lo que hubiera de el ya no vale. */
    @Synchronized
    fun olvidar(ruta: String) {
        val iterador = entradas.entries.iterator()
        while (iterador.hasNext()) {
            val entrada = iterador.next()
            if (entrada.key.ruta == ruta) {
                ocupado -= entrada.value.bytes
                iterador.remove()
            }
        }
    }

    @Synchronized
    fun vaciar() {
        entradas.clear()
        ocupado = 0
    }
}
