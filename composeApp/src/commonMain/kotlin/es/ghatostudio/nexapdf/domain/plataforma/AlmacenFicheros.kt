package es.ghatostudio.nexapdf.domain.plataforma

/**
 * Operaciones de fichero que necesita el codigo compartido.
 *
 * Es una interfaz minima a proposito: solo lo que la app usa de verdad. Nada de
 * envolver un sistema de ficheros completo para acabar usando cinco metodos.
 */
interface AlmacenFicheros {
    fun existe(ruta: String): Boolean
    fun tamano(ruta: String): Long
    fun nombre(ruta: String): String
    fun borrar(ruta: String): Boolean

    /**
     * Cambia el nombre de un fichero dentro de su misma carpeta.
     *
     * Devuelve la ruta nueva, o `null` si no se ha podido. No sobrescribe:
     * si el nombre esta cogido, se busca uno libre, porque renombrar no
     * deberia poder borrar otro documento sin avisar.
     */
    fun renombrar(ruta: String, nombreNuevo: String): String?
    fun listar(directorio: String): List<String>
    fun asegurarDirectorio(ruta: String)
    fun unirRuta(directorio: String, nombre: String): String

    suspend fun leerTexto(ruta: String): String?
    suspend fun escribirTexto(ruta: String, contenido: String): Boolean
    suspend fun leerBytes(ruta: String): ByteArray?
    suspend fun escribirBytes(ruta: String, contenido: ByteArray): Boolean
    suspend fun copiar(origen: String, destino: String): Boolean

    /**
     * Deja libre un nombre de fichero en [directorio] anadiendo " (2)", " (3)"...
     * si hiciera falta. Sobrescribir en silencio un resultado anterior es la
     * clase de detalle que hace perder trabajo al usuario.
     */
    fun nombreLibre(directorio: String, nombreDeseado: String): String
}
