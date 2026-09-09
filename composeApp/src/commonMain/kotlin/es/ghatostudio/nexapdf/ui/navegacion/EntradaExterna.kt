package es.ghatostudio.nexapdf.ui.navegacion

/**
 * Lo que llega cuando otra aplicacion abre o comparte algo con NexaPDF.
 *
 * Antes esto era un `String?` con la direccion de un PDF, y por el camino se
 * perdian tres de los cuatro casos que el manifiesto declara aceptar: compartir
 * varios ficheros a la vez, compartir una foto y abrir una copia de seguridad.
 * Los tres entraban y no pasaba nada, porque todo se trataba como "un PDF para
 * leer". Distinguirlos aqui es lo que permite llevar cada cosa a su sitio.
 */
sealed interface EntradaExterna {

    /**
     * Un fichero suelto.
     *
     * Casi siempre un PDF, que se abre para leer. Si al copiarlo resulta ser una
     * copia de seguridad de NexaPDF se importa: eso no se puede saber por el
     * intent, porque en una direccion `content://` la ruta es un identificador
     * y no el nombre del fichero.
     */
    data class UnDocumento(val uri: String) : EntradaExterna

    /** Varios PDF de golpe: lo unico que se puede querer con ellos es unirlos. */
    data class VariosDocumentos(val uris: List<String>) : EntradaExterna

    /** Una o varias imagenes: van a la pantalla que las convierte en un PDF. */
    data class Imagenes(val uris: List<String>) : EntradaExterna
}
