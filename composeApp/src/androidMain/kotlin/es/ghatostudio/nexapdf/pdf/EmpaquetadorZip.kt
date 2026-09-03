package es.ghatostudio.nexapdf.pdf

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Mete varios ficheros en un ZIP.
 *
 * Compartir cinco documentos sueltos deja al usuario eligiendo destino cinco
 * veces, y muchas aplicaciones de mensajeria solo aceptan uno. Un ZIP se
 * comparte de una vez y llega entero.
 *
 * Se escribe sin comprimir cuando el contenido ya viene comprimido: un PDF con
 * imagenes o un DOCX son ZIP por dentro, y volver a comprimirlos gasta tiempo
 * para no ganar nada.
 */
object EmpaquetadorZip {

    fun empaquetar(ficheros: List<File>, destino: File): File? {
        val existentes = ficheros.filter { it.exists() && it.isFile }
        if (existentes.isEmpty()) return null

        return runCatching {
            ZipOutputStream(destino.outputStream().buffered()).use { salida ->
                val usados = mutableSetOf<String>()
                existentes.forEach { fichero ->
                    val nombre = nombreLibre(fichero.name, usados)
                    salida.putNextEntry(ZipEntry(nombre))
                    fichero.inputStream().use { it.copyTo(salida) }
                    salida.closeEntry()
                }
            }
            destino
        }.getOrNull()
    }

    /**
     * Evita que dos ficheros con el mismo nombre se pisen dentro del ZIP.
     *
     * Puede pasar de verdad: dos paginas extraidas de documentos distintos se
     * llaman igual, y el segundo sobreescribiria al primero sin avisar.
     */
    private fun nombreLibre(nombre: String, usados: MutableSet<String>): String {
        if (usados.add(nombre)) return nombre
        val base = nombre.substringBeforeLast('.', nombre)
        val extension = nombre.substringAfterLast('.', "")
        var intento = 2
        while (true) {
            val candidato = if (extension.isEmpty()) "$base ($intento)" else "$base ($intento).$extension"
            if (usados.add(candidato)) return candidato
            intento++
        }
    }
}
