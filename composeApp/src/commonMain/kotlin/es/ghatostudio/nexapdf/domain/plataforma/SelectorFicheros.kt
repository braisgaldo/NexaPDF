package es.ghatostudio.nexapdf.domain.plataforma

/**
 * Fichero elegido por el usuario y ya copiado al espacio de trabajo interno.
 *
 * La copia se hace en el momento de elegir, a proposito: los permisos que da el
 * sistema sobre un fichero externo son temporales, y una app que los conserve
 * mas de lo necesario acaba fallando al reabrir un documento. Trabajar siempre
 * sobre una copia propia tambien garantiza no estropear el original del usuario.
 */
data class FicheroElegido(
    val ruta: String,
    val nombre: String,
    val tamanoBytes: Long,
)

/** Seleccion de ficheros a traves de la interfaz del sistema. */
interface SelectorFicheros {

    /** Elige uno o varios PDF. Lista vacia si el usuario cancela. */
    suspend fun elegirPdf(multiple: Boolean = true): List<FicheroElegido>

    /**
     * Elige ficheros de cualquier formato que la app sepa unir: PDF, Word,
     * Excel, PowerPoint e imagenes. Lo que no sea PDF se convertira despues.
     */
    suspend fun elegirParaUnir(): List<FicheroElegido>

    /** Elige una o varias imagenes de la galeria o del gestor de archivos. */
    suspend fun elegirImagenes(multiple: Boolean = true): List<FicheroElegido>

    /**
     * Abre la camara del sistema y devuelve la foto tomada.
     *
     * Se delega en la app de camara del telefono en vez de abrir una propia:
     * asi NexaPDF no necesita declarar el permiso de camara, que es un permiso
     * que a nadie le gusta conceder a una app de PDF, y de paso el usuario usa
     * la camara que ya conoce.
     *
     * Devuelve `null` si cancela o si el dispositivo no tiene camara.
     */
    suspend fun hacerFoto(): FicheroElegido?

    /** Si el dispositivo tiene alguna app capaz de hacer fotos. */
    fun hayCamara(): Boolean

    /** Elige un certificado PKCS#12 (.p12 o .pfx). */
    suspend fun elegirCertificado(): FicheroElegido?

    /** Si el sistema tiene un almacen de claves donde elegir un certificado. */
    fun hayAlmacenDeClaves(): Boolean

    /**
     * Deja al usuario elegir un certificado ya instalado en el dispositivo y
     * devuelve su alias, o `null` si cancela o no concede el acceso.
     *
     * Devuelve un alias y no la clave: el material criptografico se queda en el
     * almacen del sistema y solo se usa a traves de el.
     */
    suspend fun elegirDelAlmacenDeClaves(): String?

    /** Elige una copia de seguridad .nexaPDF.bak. */
    suspend fun elegirCopiaSeguridad(): FicheroElegido?

    /**
     * Deja elegir una carpeta y devuelve su identificador, o `null` si cancela.
     *
     * El permiso sobre esa carpeta se conserva entre arranques: si hubiera que
     * volver a pedirlo cada vez, elegir carpeta no serviria de nada.
     */
    /**
     * Copia al espacio de trabajo un fichero que llega de fuera, por ejemplo
     * al abrir un PDF desde el gestor de archivos.
     */
    suspend fun adoptarExterno(identificador: String): FicheroElegido?

    suspend fun elegirCarpeta(): String?

    /** Nombre legible de una carpeta elegida, para ensenarlo en los ajustes. */
    fun nombreDeCarpeta(uri: String): String?

    /**
     * Pide al usuario donde guardar un fichero y escribe alli el contenido de
     * [rutaOrigen]. Devuelve un nombre legible del destino, o `null` si cancela.
     */
    suspend fun guardarComo(rutaOrigen: String, nombreSugerido: String, tipoMime: String): String?
}
