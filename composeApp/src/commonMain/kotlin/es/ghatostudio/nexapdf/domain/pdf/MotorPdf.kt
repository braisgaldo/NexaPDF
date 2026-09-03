package es.ghatostudio.nexapdf.domain.pdf

import androidx.compose.ui.graphics.ImageBitmap
import es.ghatostudio.nexapdf.domain.model.BloqueTexto
import es.ghatostudio.nexapdf.domain.model.BorradorEdicion
import es.ghatostudio.nexapdf.domain.model.DisposicionImagenes
import es.ghatostudio.nexapdf.domain.model.DocumentoPdf
import es.ghatostudio.nexapdf.domain.model.Orientacion
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.domain.model.RangoPaginas
import es.ghatostudio.nexapdf.domain.model.TamanoPagina

/**
 * Todo lo que la aplicacion sabe hacer con un PDF.
 *
 * Es la unica frontera entre el codigo compartido y las bibliotecas nativas de
 * cada plataforma: en Android lo implementa PDFBox mas el renderizador del
 * sistema, y en iOS lo haria PDFKit. Nada de esto asoma a `commonMain`.
 *
 * Todas las operaciones trabajan con rutas de fichero dentro del espacio de
 * trabajo de la app y devuelven la ruta del resultado.
 */
interface MotorPdf {

    /** Lee metadatos sin cargar el documento entero en memoria. */
    suspend fun abrir(ruta: String, contrasena: String? = null): ResultadoPdf<DocumentoPdf>

    /** Geometria de cada pagina. */
    suspend fun paginas(ruta: String, contrasena: String? = null): ResultadoPdf<List<PaginaPdf>>

    /**
     * Rasteriza una pagina al ancho pedido. La implementacion cachea el
     * documento abierto para que pasar paginas no vuelva a analizarlo entero.
     */
    suspend fun renderizarPagina(
        ruta: String,
        indice: Int,
        anchoPx: Int,
        contrasena: String? = null,
    ): ResultadoPdf<ImageBitmap>

    /** Libera el documento cacheado. Se llama al salir de la pantalla. */
    /**
     * Rasteriza una imagen del disco para ensenarla en pantalla.
     *
     * Se pide con el ancho que de verdad ocupa: una foto de movil son veinte
     * megapixeles y cargarla entera para una miniatura de 104 dp es la forma
     * mas rapida de quedarse sin memoria.
     */
    suspend fun renderizarImagen(ruta: String, anchoPx: Int): ResultadoPdf<ImageBitmap>

    suspend fun cerrar(ruta: String)

    /** Une varios documentos en el orden dado. */
    suspend fun unir(
        entradas: List<EntradaUnion>,
        rutaSalida: String,
    ): ResultadoPdf<String>

    /** Extrae las paginas indicadas a un unico documento nuevo. */
    suspend fun extraerPaginas(
        ruta: String,
        paginas: List<Int>,
        rutaSalida: String,
    ): ResultadoPdf<String>

    /** Parte el documento en varios ficheros, uno por rango. */
    suspend fun separar(
        ruta: String,
        rangos: List<RangoPaginas>,
        directorioSalida: String,
        nombreBase: String,
    ): ResultadoPdf<List<String>>

    /** Reordena, rota o elimina paginas y escribe el resultado. */
    suspend fun reorganizar(
        ruta: String,
        ordenPaginas: List<Int>,
        rotaciones: Map<Int, Int>,
        rutaSalida: String,
    ): ResultadoPdf<String>

    /** Crea un PDF a partir de imagenes. */
    suspend fun imagenesAPdf(
        imagenes: List<String>,
        disposicion: DisposicionImagenes,
        tamano: TamanoPagina,
        orientacion: Orientacion,
        margenPt: Float,
        espaciadoPt: Float,
        rutaSalida: String,
    ): ResultadoPdf<String>

    /** Aplica las anotaciones y filtros del borrador y guarda un documento nuevo. */
    suspend fun aplicarEdiciones(
        borrador: BorradorEdicion,
        rutaSalida: String,
        contrasena: String? = null,
    ): ResultadoPdf<String>

    /** Bloques de texto de una pagina, para poder sustituirlos. */
    suspend fun bloquesDeTexto(
        ruta: String,
        indice: Int,
        contrasena: String? = null,
    ): ResultadoPdf<List<BloqueTexto>>

    /**
     * Firma con un certificado del usuario, venga de un fichero PKCS#12 o del
     * almacen de claves del dispositivo. Produce una firma
     * `adbe.pkcs7.detached` incrustada en el PDF, comprobable por cualquier
     * lector, y opcionalmente su representacion visible.
     */
    suspend fun firmarConCertificado(
        ruta: String,
        origen: OrigenCertificado,
        apariencia: AparienciaFirma?,
        motivo: String?,
        lugar: String?,
        rutaSalida: String,
    ): ResultadoPdf<String>

    /** Lista las firmas ya presentes en el documento. */
    suspend fun firmasExistentes(ruta: String): ResultadoPdf<List<FirmaExistente>>
}

/** Un documento y las paginas suyas que entran en la union. */
data class EntradaUnion(
    val ruta: String,
    /** `null` = todas las paginas. */
    val paginas: List<Int>? = null,
    val contrasena: String? = null,
)

/** Donde y como se pinta la firma visible. */
data class AparienciaFirma(
    val paginaIndice: Int,
    val marco: es.ghatostudio.nexapdf.domain.model.Rectangulo,
    /** PNG de la firma manuscrita ya rasterizada, o `null` para solo texto. */
    val imagenPng: ByteArray?,
    val nombreVisible: String,
    val mostrarFecha: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AparienciaFirma) return false
        return paginaIndice == other.paginaIndice &&
            marco == other.marco &&
            nombreVisible == other.nombreVisible &&
            mostrarFecha == other.mostrarFecha &&
            imagenPng.contentEquals(other.imagenPng)
    }

    override fun hashCode(): Int {
        var resultado = paginaIndice
        resultado = 31 * resultado + marco.hashCode()
        resultado = 31 * resultado + (imagenPng?.contentHashCode() ?: 0)
        resultado = 31 * resultado + nombreVisible.hashCode()
        resultado = 31 * resultado + mostrarFecha.hashCode()
        return resultado
    }
}

/** Una firma ya presente en el documento. */
data class FirmaExistente(
    val nombre: String,
    val motivo: String?,
    val lugar: String?,
    val fechaEpochMillis: Long?,
    val cubreTodoElDocumento: Boolean,
)

/**
 * Resultado de una operacion sobre un PDF.
 *
 * Se usa un tipo propio en vez de excepciones porque cada fallo tiene un mensaje
 * distinto para el usuario, y la interfaz necesita distinguir "hace falta
 * contrasena" de "el fichero esta roto" sin inspeccionar tipos de excepcion de
 * una biblioteca concreta.
 */
sealed interface ResultadoPdf<out T> {
    data class Exito<T>(val valor: T) : ResultadoPdf<T>
    data class Fallo(val causa: ErrorPdf, val detalle: String? = null) : ResultadoPdf<Nothing>

    fun valorONulo(): T? = (this as? Exito)?.valor

    fun <R> mapear(transformar: (T) -> R): ResultadoPdf<R> = when (this) {
        is Exito -> Exito(transformar(valor))
        is Fallo -> this
    }
}

enum class ErrorPdf {
    /** El documento pide contrasena y no se ha dado, o es incorrecta. */
    NECESITA_CONTRASENA,

    /** El fichero no es un PDF valido o esta danado. */
    FICHERO_INVALIDO,

    /** El PDF prohibe la operacion mediante sus permisos internos. */
    OPERACION_NO_PERMITIDA,

    /** No hay memoria suficiente para el documento. */
    SIN_MEMORIA,

    /** El certificado no se pudo abrir o la contrasena no es correcta. */
    CERTIFICADO_INVALIDO,

    /** No se pudo escribir el resultado. */
    ERROR_ESCRITURA,

    /** Cualquier otro fallo. */
    DESCONOCIDO,
}

inline fun <T> ejecutarPdf(bloque: () -> T): ResultadoPdf<T> = try {
    ResultadoPdf.Exito(bloque())
} catch (e: OutOfMemoryError) {
    ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
} catch (e: Exception) {
    ResultadoPdf.Fallo(ErrorPdf.DESCONOCIDO, e.message)
}
