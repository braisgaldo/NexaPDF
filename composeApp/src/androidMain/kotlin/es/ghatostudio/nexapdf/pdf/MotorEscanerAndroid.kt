package es.ghatostudio.nexapdf.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import es.ghatostudio.nexapdf.domain.escaner.ApiladoDeFotogramas
import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.escaner.DeteccionDocumento
import es.ghatostudio.nexapdf.domain.escaner.DetectorDocumento
import es.ghatostudio.nexapdf.domain.escaner.HojaEscaneada
import es.ghatostudio.nexapdf.domain.escaner.MotorEscaner
import es.ghatostudio.nexapdf.domain.escaner.PalabraOcr
import es.ghatostudio.nexapdf.domain.escaner.TextoPagina
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import es.ghatostudio.nexapdf.domain.pdf.ErrorPdf
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * El escaner de documentos en Android.
 *
 * Tres operaciones y ninguna mas: encontrar el papel en una foto, enderezarlo, y
 * leer lo que pone. Cada una se apoya en la pieza que mejor lo hace:
 *
 *  - **Encontrar** lo hace [DetectorDocumento], que es Kotlin puro y comun a
 *    todas las plataformas.
 *  - **Enderezar** lo hace `Matrix.setPolyToPoly` de Android, que resuelve la
 *    homografia de cuatro puntos por hardware. Escribirla a mano en Kotlin
 *    significaria recorrer doce millones de pixeles interpolando, y tardaria
 *    segundos por pagina en lugar de decimas.
 *  - **Leer** lo hace ML Kit con su modelo empaquetado dentro del APK. No
 *    descarga nada ni consulta nada: la aplicacion no tiene permiso de internet
 *    y seguiria funcionando igual en un telefono en modo avion.
 */
class MotorEscanerAndroid(
    private val directorioTrabajo: String,
) : MotorEscaner {

    private val detector by lazy { DetectorDocumento() }

    /**
     * El reconocedor se crea al primer uso y se conserva.
     *
     * Crearlo cuesta cargar el modelo en memoria, asi que hacerlo por pagina
     * multiplicaria el tiempo de un documento de diez hojas. Se conserva
     * abierto: es lo que recomienda la propia biblioteca para el uso por lotes.
     */
    private val reconocedor by lazy {
        runCatching { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }.getOrNull()
    }

    override val ocrDisponible: Boolean get() = reconocedor != null

    // --- Deteccion -----------------------------------------------------------

    override suspend fun detectarEn(rutaImagen: String): DeteccionDocumento =
        withContext(Dispatchers.Default) {
            val mapa = CargadorImagen.cargar(rutaImagen, LADO_DETECCION)
                ?: return@withContext DeteccionDocumento.NADA
            try {
                val ancho = mapa.width
                val alto = mapa.height
                val pixeles = IntArray(ancho * alto)
                mapa.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)

                // El detector trabaja sobre luminancia, que es lo que le llega de
                // la camara en vivo. Aqui se calcula para que las fotos de la
                // galeria pasen exactamente por el mismo codigo: una deteccion que
                // se comporta distinto segun de donde venga la foto es una
                // deteccion que hay que depurar dos veces.
                val luminancia = ByteArray(pixeles.size)
                for (indice in pixeles.indices) {
                    val pixel = pixeles[indice]
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    luminancia[indice] = ((r * 299 + g * 587 + b * 114) / 1000).toByte()
                }
                // Una instancia propia: esta llamada puede venir de otro hilo
                // mientras la camara esta analizando con la suya.
                DetectorDocumento().detectar(luminancia, ancho, alto)
            } finally {
                mapa.recycle()
            }
        }

    // --- Revelado ------------------------------------------------------------

    override suspend fun revelar(
        hoja: HojaEscaneada,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.Default) {
        try {
            val pagina = componer(hoja, CargadorImagen.LADO_PAGINA_ESCANEADA)
                ?: return@withContext ResultadoPdf.Fallo(
                    ErrorPdf.FICHERO_INVALIDO,
                    "no se pudo leer ${hoja.rutaOriginal}",
                )
            try {
                File(rutaSalida).parentFile?.mkdirs()
                FileOutputStream(rutaSalida).use { salida ->
                    // PNG y no JPEG. Esta imagen es un paso intermedio que se
                    // vuelve a leer para meterla en el PDF, asi que cualquier
                    // perdida aqui se arrastra hasta el documento final. Y el
                    // JPEG pierde justo donde mas duele: alrededor de las letras
                    // deja un cerco que se lee como falta de nitidez. Una pagina
                    // ya aplanada y con la tinta al negro comprime muy bien sin
                    // perdidas, asi que tampoco cuesta espacio. El fichero se
                    // borra en cuanto el PDF esta montado.
                    pagina.compress(Bitmap.CompressFormat.PNG, 100, salida)
                }
            } finally {
                pagina.recycle()
            }
            ResultadoPdf.Exito(rutaSalida)
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, e.message)
        }
    }

    override suspend fun previsualizar(
        hoja: HojaEscaneada,
        anchoPx: Int,
    ): ResultadoPdf<ImageBitmap> = withContext(Dispatchers.Default) {
        try {
            val pagina = componer(hoja, anchoPx.coerceIn(120, CargadorImagen.LADO_MAXIMO))
                ?: return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO)
            ResultadoPdf.Exito(pagina.asImageBitmap())
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.DESCONOCIDO, e.message)
        }
    }

    /**
     * Recorta, endereza, mejora y gira: la hoja tal y como quedara.
     *
     * El orden importa. Enderezar antes de filtrar deja el filtro trabajando
     * sobre la pagina sola, sin la mesa alrededor: el umbral automatico de
     * "documento nitido" se calcula con el histograma de lo que ve, y con media
     * foto de madera oscura dentro elegiria un corte que quema el papel.
     */
    private fun componer(hoja: HojaEscaneada, ladoMaximo: Int): Bitmap? {
        val original = CargadorImagen.cargar(hoja.rutaOriginal, cargaNecesaria(hoja, ladoMaximo))
            ?: return null

        val enderezada = try {
            enderezar(original, hoja.cuadro, ladoMaximo)
        } finally {
            if (original.isRecycled.not()) original.recycle()
        } ?: return null

        // La rafaga se funde siempre que la haya, tenga filtro o no. Importa que
        // sea asi: el reconocimiento de texto lee la pagina **sin** filtrar, y
        // si fundir dependiera del filtro, justo el paso que mas agradece una
        // imagen limpia se quedaria con la foto suelta.
        val fundida = fundirRafaga(hoja, enderezada, ladoMaximo)

        val mejorada = if (hoja.filtro == FiltroPagina.NINGUNO) {
            fundida
        } else {
            FiltrosPagina.aplicar(fundida, hoja.filtro, hoja.intensidadFiltro).also {
                if (it != fundida) fundida.recycle()
            }
        }

        if (hoja.giroGrados % 360 == 0) return mejorada
        val matriz = Matrix().apply { postRotate(hoja.giroGrados.toFloat()) }
        val girada = Bitmap.createBitmap(
            mejorada, 0, 0, mejorada.width, mejorada.height, matriz, true,
        )
        if (girada != mejorada) mejorada.recycle()
        return girada
    }

    /**
     * Funde la rafaga sobre la hoja ya enderezada.
     *
     * Las demas fotos se cargan y enderezan **con el mismo recorte** que la de
     * referencia. Eso las deja del mismo tamano y aproximadamente encima, que es
     * lo que el apilado necesita para arrancar; el ajuste fino, por debajo del
     * pixel, lo hace el propio apilado. No se vuelve a detectar el papel en cada
     * una a proposito: la deteccion tiene su propio temblor, de varios pixeles,
     * y meterlo aqui seria anadir el error que luego hay que corregir.
     *
     * Si algo falla se devuelve la de referencia sin tocar. Una rafaga que no
     * cuaja tiene que costar nitidez que no se gana, nunca una pagina peor.
     */
    private fun fundirRafaga(hoja: HojaEscaneada, referencia: Bitmap, ladoMaximo: Int): Bitmap {
        if (hoja.rutasRafaga.isEmpty()) return referencia

        val ancho = referencia.width
        val alto = referencia.height
        val pixeles = IntArray(ancho * alto)
        referencia.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)

        val planos = ArrayList<IntArray>(hoja.rutasRafaga.size + 1)
        planos += IntArray(ancho * alto) { indice ->
            val pixel = pixeles[indice]
            luminanciaDe(pixel)
        }

        for (ruta in hoja.rutasRafaga) {
            val otra = CargadorImagen.cargar(ruta, cargaNecesaria(hoja, ladoMaximo)) ?: continue
            val enderezadaOtra = try {
                enderezar(otra, hoja.cuadro, ladoMaximo)
            } finally {
                if (!otra.isRecycled) otra.recycle()
            } ?: continue

            try {
                // Un tamano distinto significa que esa foto salio con otra
                // resolucion; alinearla exigiria reescalarla y no compensa.
                if (enderezadaOtra.width != ancho || enderezadaOtra.height != alto) continue
                enderezadaOtra.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
                planos += IntArray(ancho * alto) { indice -> luminanciaDe(pixeles[indice]) }
            } finally {
                if (!enderezadaOtra.isRecycled) enderezadaOtra.recycle()
            }
        }

        if (planos.size == 1) return referencia

        val apilada = ApiladoDeFotogramas.apilar(planos, ancho, alto)
        val luminanciaOriginal = planos.first()
        referencia.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        for (indice in pixeles.indices) {
            // Se traslada a cada canal lo que la luminancia ha cambiado, en
            // lugar de escribir el gris directamente. Asi una pagina sin filtro
            // sale limpia y **en color**, que es lo que se pidio; y una con
            // filtro de documento acaba igual de todas formas, porque el filtro
            // vuelve a pasar a luminancia justo despues.
            val ajuste = apilada[indice] - luminanciaOriginal[indice]
            val pixel = pixeles[indice]
            pixeles[indice] = Color.argb(
                Color.alpha(pixel),
                (Color.red(pixel) + ajuste).coerceIn(0, 255),
                (Color.green(pixel) + ajuste).coerceIn(0, 255),
                (Color.blue(pixel) + ajuste).coerceIn(0, 255),
            )
        }
        val salida = Bitmap.createBitmap(pixeles, ancho, alto, Bitmap.Config.ARGB_8888)
        referencia.recycle()
        return salida
    }

    private fun luminanciaDe(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /**
     * A que resolucion hay que cargar la foto para que la **pagina** salga al
     * tamano pedido.
     *
     * Este es el detalle que faltaba y que se veia a simple vista. La pagina es
     * un trozo del encuadre, no el encuadre entero: si la foto se carga al
     * tamano que se quiere para la pagina, la pagina sale mas pequena que eso, y
     * ademas remuestreada dos veces —una al cargar y otra al enderezar— sobre
     * datos que ya venian reducidos. Medido sobre una foto de doce megapixeles:
     * con el tope de entonces, la pagina acababa en 2351 px de alto en lugar
     * de los 2900 pedidos.
     *
     * Cargando proporcionalmente mas grande, el enderezado reduce desde una
     * imagen con detalle de sobra en vez de estirar una que ya no lo tiene.
     */
    private fun cargaNecesaria(hoja: HojaEscaneada, ladoDeLaPagina: Int): Int {
        val medida = CargadorImagen.medir(hoja.rutaOriginal) ?: return ladoDeLaPagina
        val (anchoFoto, altoFoto) = medida
        val ladoFoto = max(anchoFoto, altoFoto)

        val (anchoPagina, altoPagina) = hoja.cuadro.ladosEnPixeles(anchoFoto, altoFoto)
        val ladoPagina = max(anchoPagina, altoPagina)
        if (ladoPagina < 1f) return ladoDeLaPagina

        val pedida = (ladoFoto * ladoDeLaPagina / ladoPagina).roundToInt()
        // Nunca mas que la foto —no hay detalle que inventar— ni mas que el tope
        // de memoria. Y solo un tope, no un suelo: con una foto mas pequena que
        // la pagina que se pide, un suelo dejaria el rango del reves y
        // reventaria. Pasa con cualquier foto mas corta que la pagina pedida,
        // que es justo lo que llega desde la galeria mas a menudo de lo que
        // parece.
        val tope = min(ladoFoto, CargadorImagen.LADO_CARGA_MAXIMO)
        return pedida.coerceAtMost(tope).coerceAtLeast(1)
    }

    /**
     * Correccion de perspectiva de cuatro puntos.
     *
     * `setPolyToPoly` con cuatro pares de puntos es exactamente la homografia
     * que convierte el trapecio de la foto en un rectangulo. El tamano de salida
     * sale de los lados largos del cuadrilatero y no de la media: el borde que
     * quedo mas cerca de la camara es el que trae mas pixeles, y encoger hasta
     * el mas corto seria tirar detalle que si estaba.
     */
    private fun enderezar(origen: Bitmap, cuadro: Cuadrilatero, ladoMaximo: Int): Bitmap? {
        if (cuadro == Cuadrilatero.COMPLETO) return origen.copy(Bitmap.Config.ARGB_8888, false)

        // El tamano de salida sale de los lados del papel, no de la caja que lo
        // envuelve: con la caja, una hoja girada pedia una imagen del tamano de
        // su diagonal y el enderezado la ampliaba mas alla de los pixeles que
        // habia. Y nunca se amplia: `escala` no pasa de uno.
        var (ancho, alto) = cuadro.ladosEnPixeles(origen.width, origen.height)
        if (ancho <= 1f || alto <= 1f || ancho.isNaN() || alto.isNaN()) return null

        val escala = min(1f, ladoMaximo / max(ancho, alto))
        ancho *= escala
        alto *= escala

        val anchoPx = ancho.roundToInt().coerceIn(16, ladoMaximo)
        val altoPx = alto.roundToInt().coerceIn(16, ladoMaximo)

        val fuente = FloatArray(8)
        cuadro.esquinas.forEachIndexed { indice, punto ->
            fuente[indice * 2] = punto.x * origen.width
            fuente[indice * 2 + 1] = punto.y * origen.height
        }
        val destino = floatArrayOf(
            0f, 0f,
            anchoPx.toFloat(), 0f,
            anchoPx.toFloat(), altoPx.toFloat(),
            0f, altoPx.toFloat(),
        )

        val matriz = Matrix()
        if (!matriz.setPolyToPoly(fuente, 0, destino, 0, 4)) return null

        val resultado = Bitmap.createBitmap(anchoPx, altoPx, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(resultado)
        lienzo.drawColor(android.graphics.Color.WHITE)
        lienzo.drawBitmap(origen, matriz, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return resultado
    }

    // --- Reconocimiento de texto ---------------------------------------------

    override suspend fun reconocerTexto(rutaImagen: String): ResultadoPdf<TextoPagina> {
        val cliente = reconocedor ?: return ResultadoPdf.Exito(TextoPagina.SIN_TEXTO)

        val mapa = withContext(Dispatchers.IO) {
            CargadorImagen.cargar(rutaImagen, CargadorImagen.LADO_PAGINA_ESCANEADA)
        } ?: return ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, rutaImagen)

        return try {
            val ancho = mapa.width.toFloat()
            val alto = mapa.height.toFloat()
            // Se pasa giro 0 porque la imagen ya viene derecha del revelado: el
            // EXIF se aplico al cargarla y el enderezado dejo la pagina recta.
            val entrada = InputImage.fromBitmap(mapa, 0)

            // El tipo va explicito: sin el, Kotlin infiere `Nothing?` del
            // `resume(null)` del fallo y no deja leer el resultado.
            val texto = suspendCancellableCoroutine<Text?> { continuacion ->
                cliente.process(entrada)
                    .addOnSuccessListener { continuacion.resume(it) }
                    .addOnFailureListener { continuacion.resume(null) }
            } ?: return ResultadoPdf.Exito(TextoPagina.SIN_TEXTO)

            val palabras = ArrayList<PalabraOcr>()
            texto.textBlocks.forEach { bloque ->
                bloque.lines.forEach { linea ->
                    linea.elements.forEach { elemento ->
                        val caja = elemento.boundingBox ?: return@forEach
                        val contenido = elemento.text.trim()
                        if (contenido.isEmpty()) return@forEach
                        palabras += PalabraOcr(
                            texto = normalizar(contenido),
                            marco = Rectangulo(
                                izquierda = (caja.left / ancho).coerceIn(0f, 1f),
                                arriba = (caja.top / alto).coerceIn(0f, 1f),
                                derecha = (caja.right / ancho).coerceIn(0f, 1f),
                                abajo = (caja.bottom / alto).coerceIn(0f, 1f),
                            ),
                        )
                    }
                }
            }
            ResultadoPdf.Exito(TextoPagina(palabras))
        } catch (e: OutOfMemoryError) {
            ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
        } catch (e: Exception) {
            ResultadoPdf.Fallo(ErrorPdf.DESCONOCIDO, e.message)
        } finally {
            mapa.recycle()
        }
    }

    /**
     * Deja el texto en forma compuesta.
     *
     * El reconocedor de este dispositivo devuelve las letras acentuadas ya
     * compuestas, en un solo caracter, pero no es algo que la biblioteca
     * prometa. Descompuestas —la "o" por un lado y la tilde por otro— muchas
     * fuentes no las saben escribir, y la palabra se caeria entera de la capa de
     * texto sin que nada avisara. Componerlas aqui cuesta nada y quita esa
     * dependencia.
     */
    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFC)

    /** Carpeta donde se guardan las fotos y las paginas reveladas del escaner. */
    fun directorioEscaner(): String = File(directorioTrabajo, "escaner")
        .also { it.mkdirs() }
        .absolutePath

    private companion object {
        /**
         * Lado al que se reduce una foto para buscarle los bordes.
         *
         * El detector reduce otra vez por su cuenta hasta 180 px; esto solo
         * evita decodificar veinte megapixeles para tirar el 99 % acto seguido.
         */
        const val LADO_DETECCION = 640

        /**
         * Lado al que se reduce una pagina antes del reconocimiento de texto.
         *
         * Estaba en 1600 con el argumento de que por encima no se gana
         * precision. Era falso y se noto leyendo documentos de verdad: en un A4,
         * 1600 px de lado son unos 135 puntos por pulgada, y a esa resolucion la
         * letra de cuerpo 10 tiene trece pixeles de alto. El reconocimiento
         * necesita el doble para no confundir la i con la l o el 0 con la O.
         *
         * A 2600 px un A4 sale a unos 220 ppp, que es la horquilla en la que
         * trabajan los escaneres de sobremesa. Es ademas el mismo tope al que se
         * revela la pagina, asi que se lee **exactamente** la imagen que se
         * guarda, sin una reduccion de mas por el camino.
         */
        const val LADO_OCR = 2600

        /**
         * Calidad JPEG de la pagina revelada.
         *
         * Alta a proposito: esta imagen es el documento. Lo que se ahorre aqui
         * se paga en letras con halo, y un escaneo con halo no se puede leer ni
         * volver a escanear. Tampoco lo puede leer el reconocedor de texto, que
         * trabaja sobre esta misma imagen.
         */
        const val CALIDAD_JPEG = 95
    }
}
