package es.ghatostudio.nexapdf.domain.plataforma

/**
 * Lo que la app necesita del sistema operativo.
 *
 * Cada plataforma aporta su implementacion al construir el contenedor de
 * dependencias, de modo que `commonMain` no importa nada de Android ni de iOS.
 */
interface ServiciosPlataforma {

    /** Carpeta privada donde se copian los ficheros importados. */
    val directorioTrabajo: String

    /** Carpeta privada con los documentos ya generados. */
    val directorioSalida: String

    /** Carpeta privada con las copias de seguridad .nexaPDF.bak. */
    val directorioCopias: String

    /** Nombre legible de la plataforma, para "Acerca de". */
    val nombrePlataforma: String

    /**
     * Si la pantalla de donacion puede mostrarse.
     *
     * Activa en Android y en escritorio; desactivada por defecto en iOS, donde
     * las directrices de App Review son mas estrictas con los enlaces de pago
     * externos. Ver el apartado 4.4.1 de la guia de publicacion.
     */
    val donacionesDisponibles: Boolean

    /** El usuario ha pedido al sistema reducir las animaciones. */
    val reducirAnimaciones: Boolean

    /** Milisegundos desde epoch. */
    fun ahora(): Long

    /** Fecha formateada segun la locale activa. */
    fun formatearFecha(epochMillis: Long, conHora: Boolean = false): String

    /** Tamano de fichero formateado segun la locale activa. */
    fun formatearTamano(bytes: Long): String

    /** Numero formateado segun la locale activa. */
    fun formatearNumero(valor: Double, decimales: Int): String

    /** Abre una URL en el navegador del sistema, nunca dentro de la app. */
    fun abrirEnNavegador(url: String)

    /** Comparte un fichero generado con otras aplicaciones. */
    fun compartirFichero(ruta: String, tipoMime: String, asunto: String?)

    /**
     * Comparte varios ficheros a la vez.
     *
     * Con uno se comparte tal cual. Con varios se empaquetan en un ZIP: casi
     * ninguna aplicacion de mensajeria acepta varios adjuntos de golpe, y
     * compartirlos uno a uno obliga a elegir destino tantas veces como
     * ficheros haya.
     */
    suspend fun compartirVarios(rutas: List<String>, nombreZip: String)

    /** Comparte texto plano (por ejemplo el enlace a la ficha de la tienda). */
    fun compartirTexto(texto: String, asunto: String?)

    /** Copia texto al portapapeles. */
    fun copiarAlPortapapeles(texto: String)

    /**
     * Guarda una copia del fichero donde el usuario pueda encontrarla fuera de
     * la app (en Android, la carpeta Descargas). Devuelve un nombre legible de
     * la ubicacion, o `null` si no se pudo.
     */
    suspend fun guardarEnDescargas(rutaOrigen: String, nombre: String, tipoMime: String): String?

    /**
     * Copia el fichero a la carpeta que el usuario eligio en los ajustes.
     * Devuelve un nombre legible de donde quedo, o `null` si no se pudo.
     */
    suspend fun guardarEnCarpeta(
        rutaOrigen: String,
        nombre: String,
        tipoMime: String,
        uriCarpeta: String,
    ): String?

    /** Aplica la preferencia de idioma por aplicacion. `null` = la del sistema. */
    fun aplicarIdioma(etiquetaBcp47: String?)

    /** Idioma por aplicacion activo, o `null` si se sigue al sistema. */
    fun idiomaActual(): String?

    /** Etiqueta BCP-47 del idioma que esta usando el sistema. */
    fun idiomaDelSistema(): String
}
