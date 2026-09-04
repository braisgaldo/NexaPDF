package es.ghatostudio.nexapdf.domain.pdf

import androidx.compose.ui.graphics.ImageBitmap
import es.ghatostudio.nexapdf.domain.model.BloqueTexto
import es.ghatostudio.nexapdf.domain.model.BorradorEdicion
import es.ghatostudio.nexapdf.domain.model.DisposicionImagenes
import es.ghatostudio.nexapdf.domain.model.DocumentoPdf
import es.ghatostudio.nexapdf.domain.model.Orientacion
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.domain.model.Rectangulo
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
        /**
         * Es una miniatura: se puede guardar en cache y con menos color.
         *
         * Una rejilla de miniaturas pide las mismas paginas una y otra vez
         * segun se desplaza, y a ese tamano nadie distingue 16 bits de
         * color de 24. La pagina grande del visor y del editor no lo es:
         * ahi se amplia y se nota.
         */
        miniatura: Boolean = false,
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
        /** Se llama con (hechos, total) segun van saliendo los ficheros. */
        alAvanzar: ((Int, Int) -> Unit)? = null,
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

    /**
     * Busca texto en todo el documento.
     *
     * Devuelve una entrada por aparicion, con la pagina y un fragmento de
     * contexto: en un documento de doscientas paginas, saber que la palabra
     * esta en la 137 no sirve de nada si no se ve en que frase.
     */
    suspend fun buscarTexto(
        ruta: String,
        consulta: String,
        contrasena: String? = null,
    ): ResultadoPdf<List<Coincidencia>>

    /**
     * Indice del documento, si lo trae.
     *
     * Muchos PDF generados a partir de Word o LaTeX llevan marcadores con la
     * estructura de secciones; los escaneados no llevan nada, y en ese caso la
     * lista sale vacia.
     */
    /**
     * Cifra el documento con una contrasena y escribe el resultado.
     *
     * @param contrasenaApertura la que hay que teclear para abrirlo. Es la que
     *   protege de verdad: sin ella el documento no se lee.
     * @param contrasenaPermisos la de quien puede cambiar los permisos. Si se
     *   deja vacia se usa la de apertura, porque dejarla en blanco significa
     *   "cualquiera puede quitar las restricciones" y eso confunde mas que
     *   ayuda.
     * @param permisos que se puede hacer sin la contrasena de permisos.
     */
    suspend fun cifrar(
        ruta: String,
        contrasenaApertura: String,
        contrasenaPermisos: String,
        permisos: PermisosPdf,
        rutaSalida: String,
        contrasenaActual: String? = null,
    ): ResultadoPdf<String>

    /** Quita la proteccion de un documento del que se conoce la contrasena. */
    suspend fun descifrar(
        ruta: String,
        contrasena: String,
        rutaSalida: String,
    ): ResultadoPdf<String>

    suspend fun esquema(ruta: String, contrasena: String? = null): ResultadoPdf<List<Seccion>>

    /** Lista las firmas ya presentes en el documento. */
    suspend fun firmasExistentes(ruta: String): ResultadoPdf<List<FirmaExistente>>
}

/** Una aparicion del texto buscado. */
data class Coincidencia(
    val pagina: Int,
    /** Frase alrededor de la aparicion, para reconocerla de un vistazo. */
    val fragmento: String,
    /**
     * Donde esta, en coordenadas normalizadas de la pagina.
     *
     * Sin esto solo se puede decir en que pagina cae; con esto se puede pintar
     * encima, que es lo que convierte una lista de resultados en una busqueda
     * util de verdad.
     */
    val marco: Rectangulo,
)

/** Una entrada del indice del documento. */
data class Seccion(
    val titulo: String,
    val pagina: Int,
    /** Profundidad en el arbol, para sangrar las subsecciones. */
    val nivel: Int,
)

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
    /**
     * Lo que dice el certificado que firmo, leido del sobre CMS.
     *
     * Va aqui y no se calcula en la pantalla porque abrir el sobre es trabajo
     * de disco y de criptografia, no de interfaz. Todo es opcional: un PDF puede
     * traer una firma que no se sepa interpretar, y en ese caso es mejor
     * ensenar lo que se tenga que no ensenar nada.
     */
    val firmante: String? = null,
    val emisor: String? = null,
    val numeroSerie: String? = null,
    val validoDesdeEpochMillis: Long? = null,
    val validoHastaEpochMillis: Long? = null,
    /** Ya legible: "SHA-256 con RSA", no un OID. */
    val algoritmo: String? = null,
    /** El subfiltro del PDF, que es lo que dice si es PAdES o el PKCS#7 viejo. */
    val formato: String? = null,
    /** Lleva sello de tiempo de una autoridad. */
    val conSelloDeTiempo: Boolean = false,
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

/**
 * Que deja hacer un PDF cifrado a quien solo tiene la contrasena de apertura.
 *
 * Son las restricciones estandar de PDF, y conviene decir lo que valen: un
 * lector que respete el formato las obedece, pero **no son una barrera
 * criptografica**. Lo unico que de verdad protege el contenido es la
 * contrasena de apertura. La aplicacion lo dice asi en pantalla en lugar de
 * dejar creer que marcar "no imprimir" impide imprimir.
 */
data class PermisosPdf(
    val permitirImprimir: Boolean = true,
    val permitirCopiar: Boolean = true,
    val permitirModificar: Boolean = false,
    val permitirAnotar: Boolean = true,
)
