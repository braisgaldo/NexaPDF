package es.ghatostudio.nexapdf.plataforma

import es.ghatostudio.nexapdf.domain.plataforma.AlmacenFicheros
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AlmacenFicherosAndroid : AlmacenFicheros {

    override fun existe(ruta: String): Boolean = File(ruta).exists()

    override fun tamano(ruta: String): Long = File(ruta).let { if (it.exists()) it.length() else 0L }

    override fun nombre(ruta: String): String = File(ruta).name

    override fun borrar(ruta: String): Boolean = runCatching {
        val fichero = File(ruta)
        if (fichero.isDirectory) fichero.deleteRecursively() else fichero.delete()
    }.getOrDefault(false)

    override fun listar(directorio: String): List<String> =
        File(directorio).listFiles()?.sortedByDescending { it.lastModified() }?.map { it.absolutePath }
            ?: emptyList()

    override fun asegurarDirectorio(ruta: String) {
        File(ruta).mkdirs()
    }

    override fun unirRuta(directorio: String, nombre: String): String =
        File(directorio, nombre).absolutePath

    override suspend fun leerTexto(ruta: String): String? = withContext(Dispatchers.IO) {
        runCatching { File(ruta).readText() }.getOrNull()
    }

    override suspend fun escribirTexto(ruta: String, contenido: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val fichero = File(ruta)
                fichero.parentFile?.mkdirs()
                fichero.writeText(contenido)
                true
            }.getOrDefault(false)
        }

    override suspend fun leerBytes(ruta: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { File(ruta).readBytes() }.getOrNull()
    }

    override suspend fun escribirBytes(ruta: String, contenido: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val fichero = File(ruta)
                fichero.parentFile?.mkdirs()
                fichero.writeBytes(contenido)
                true
            }.getOrDefault(false)
        }

    override suspend fun copiar(origen: String, destino: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val ficheroDestino = File(destino)
                ficheroDestino.parentFile?.mkdirs()
                File(origen).copyTo(ficheroDestino, overwrite = true)
                true
            }.getOrDefault(false)
        }

    override fun nombreLibre(directorio: String, nombreDeseado: String): String {
        val carpeta = File(directorio)
        carpeta.mkdirs()
        if (!File(carpeta, nombreDeseado).exists()) return nombreDeseado

        // Se respeta la extension compuesta (.nexaPDF.bak) ademas de la simple.
        val extension = EXTENSIONES_COMPUESTAS.firstOrNull { nombreDeseado.endsWith(it, true) }
            ?: nombreDeseado.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        val base = nombreDeseado.removeSuffix(extension)

        var indice = 2
        while (File(carpeta, "$base ($indice)$extension").exists()) indice++
        return "$base ($indice)$extension"
    }

    private companion object {
        val EXTENSIONES_COMPUESTAS = listOf(".nexaPDF.bak")
    }
}
