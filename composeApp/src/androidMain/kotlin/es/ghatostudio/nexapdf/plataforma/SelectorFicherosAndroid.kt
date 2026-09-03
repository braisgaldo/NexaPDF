package es.ghatostudio.nexapdf.plataforma

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import es.ghatostudio.nexapdf.domain.plataforma.FicheroElegido
import es.ghatostudio.nexapdf.domain.plataforma.SelectorFicheros
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Seleccion de ficheros con el Storage Access Framework.
 *
 * Los lanzadores se registran en el `init`, que MainActivity ejecuta dentro de
 * `onCreate`: registrar un contrato despues de que la actividad este iniciada
 * lanza una excepcion, y ese fallo solo aparece al rotar la pantalla.
 *
 * Cada seleccion se copia inmediatamente al espacio de trabajo. Ver
 * [FicheroElegido] para el porque.
 */
class SelectorFicherosAndroid(
    private val actividad: ComponentActivity,
    private val directorioTrabajo: String,
) : SelectorFicheros {

    private var pendienteVarios: CompletableDeferred<List<Uri>>? = null
    private var pendienteUno: CompletableDeferred<Uri?>? = null
    private var pendienteGuardar: CompletableDeferred<Uri?>? = null

    private val abrirVarios: ActivityResultLauncher<Array<String>> =
        actividad.registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            pendienteVarios?.complete(uris)
            pendienteVarios = null
        }

    private val abrirUno: ActivityResultLauncher<Array<String>> =
        actividad.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pendienteUno?.complete(uri)
            pendienteUno = null
        }

    private val crearDocumento: ActivityResultLauncher<String> =
        actividad.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            pendienteGuardar?.complete(uri)
            pendienteGuardar = null
        }

    private var pendienteFoto: CompletableDeferred<Boolean>? = null
    private var destinoFoto: File? = null

    private val hacerFotoLanzador: ActivityResultLauncher<Uri> =
        actividad.registerForActivityResult(ActivityResultContracts.TakePicture()) { hecha ->
            pendienteFoto?.complete(hecha)
            pendienteFoto = null
        }

    override suspend fun elegirPdf(multiple: Boolean): List<FicheroElegido> =
        elegir(TIPOS_PDF, multiple)

    override suspend fun elegirImagenes(multiple: Boolean): List<FicheroElegido> =
        elegir(TIPOS_IMAGEN, multiple)

    override suspend fun elegirParaUnir(): List<FicheroElegido> =
        elegir(TIPOS_PARA_UNIR, multiple = true)

    /**
     * Hace una foto con la app de camara del sistema.
     *
     * La foto se escribe directamente en el espacio de trabajo a traves del
     * FileProvider, sin pasar por la galeria: es una imagen intermedia para
     * construir un PDF, y llenar el carrete del usuario de fotos sueltas de
     * documentos seria ensuciarle el telefono.
     */
    override suspend fun hacerFoto(): FicheroElegido? {
        if (!hayCamara()) return null

        val carpeta = File(directorioTrabajo).apply { mkdirs() }
        val destino = File(carpeta, "foto_${System.currentTimeMillis()}.jpg")
        destinoFoto = destino

        val uri = runCatching {
            FileProvider.getUriForFile(
                actividad,
                "${actividad.packageName}.fileprovider",
                destino,
            )
        }.getOrNull() ?: return null

        val espera = CompletableDeferred<Boolean>()
        pendienteFoto = espera
        runCatching { hacerFotoLanzador.launch(uri) }.onFailure {
            pendienteFoto = null
            return null
        }

        val hecha = espera.await()
        if (!hecha || !destino.exists() || destino.length() == 0L) {
            destino.delete()
            return null
        }

        return FicheroElegido(
            ruta = destino.absolutePath,
            nombre = destino.name,
            tamanoBytes = destino.length(),
        )
    }

    override fun hayCamara(): Boolean {
        val intencion = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return intencion.resolveActivity(actividad.packageManager) != null
    }

    override suspend fun elegirCertificado(): FicheroElegido? =
        elegir(TIPOS_CERTIFICADO, multiple = false).firstOrNull()

    override suspend fun elegirCopiaSeguridad(): FicheroElegido? =
        elegir(TIPOS_COPIA, multiple = false).firstOrNull()

    private suspend fun elegir(tipos: Array<String>, multiple: Boolean): List<FicheroElegido> {
        val uris = if (multiple) {
            val espera = CompletableDeferred<List<Uri>>()
            pendienteVarios = espera
            runCatching { abrirVarios.launch(tipos) }.onFailure {
                pendienteVarios = null
                return emptyList()
            }
            espera.await()
        } else {
            val espera = CompletableDeferred<Uri?>()
            pendienteUno = espera
            runCatching { abrirUno.launch(tipos) }.onFailure {
                pendienteUno = null
                return emptyList()
            }
            listOfNotNull(espera.await())
        }
        return uris.mapNotNull { copiarAlEspacioDeTrabajo(it) }
    }

    override suspend fun guardarComo(
        rutaOrigen: String,
        nombreSugerido: String,
        tipoMime: String,
    ): String? {
        val espera = CompletableDeferred<Uri?>()
        pendienteGuardar = espera
        runCatching { crearDocumento.launch(nombreSugerido) }.onFailure {
            pendienteGuardar = null
            return null
        }
        val destino = espera.await() ?: return null

        return withContext(Dispatchers.IO) {
            runCatching {
                actividad.contentResolver.openOutputStream(destino)?.use { salida ->
                    File(rutaOrigen).inputStream().use { entrada -> entrada.copyTo(salida) }
                } ?: return@runCatching null
                nombreDeUri(actividad, destino) ?: nombreSugerido
            }.getOrNull()
        }
    }

    /**
     * Copia el contenido del URI a un fichero propio y devuelve sus datos.
     *
     * La copia va en una subcarpeta con nombre unico, conservando dentro el
     * nombre original del fichero. Poner el identificador en la carpeta y no en
     * el nombre importa: el nombre es lo que ve el usuario en la lista y lo que
     * hereda el resultado ("Informe editado.pdf"), y un prefijo de dieciocho
     * digitos delante lo dejaria ilegible.
     */
    private suspend fun copiarAlEspacioDeTrabajo(uri: Uri): FicheroElegido? =
        withContext(Dispatchers.IO) {
            runCatching {
                val nombreOriginal = nombreDeUri(actividad, uri) ?: "documento"
                val carpeta = File(directorioTrabajo, System.nanoTime().toString())
                    .apply { mkdirs() }
                val destino = File(carpeta, nombreSeguro(nombreOriginal))

                actividad.contentResolver.openInputStream(uri)?.use { entrada ->
                    destino.outputStream().use { salida -> entrada.copyTo(salida) }
                } ?: return@runCatching null

                FicheroElegido(
                    ruta = destino.absolutePath,
                    nombre = nombreOriginal,
                    tamanoBytes = destino.length(),
                )
            }.getOrNull()
        }

    /** Quita del nombre lo que no puede ir en un fichero del sistema. */
    private fun nombreSeguro(nombre: String): String =
        nombre.replace(Regex("""[\\/:*?"<>|]"""), "_").take(120)

    private companion object {
        val TIPOS_PDF = arrayOf("application/pdf")

        /**
         * Todo lo que se puede llevar al espacio de trabajo. Se toma de
         * FormatoDocumento para que anadir un formato alli lo habilite tambien
         * en el selector, sin tener que acordarse de tocar dos sitios.
         */
        val TIPOS_PARA_UNIR =
            es.ghatostudio.nexapdf.domain.pdf.FormatoDocumento.TIPOS_PARA_UNIR.toTypedArray()
        val TIPOS_IMAGEN = arrayOf("image/*")

        // Muchos gestores de archivos no reconocen el tipo de un .p12 y lo
        // presentan como octet-stream; sin ese comodin el fichero saldria en gris.
        val TIPOS_CERTIFICADO = arrayOf(
            "application/x-pkcs12",
            "application/pkcs12",
            "application/octet-stream",
        )

        // El .nexaPDF.bak no tiene tipo registrado en el sistema.
        val TIPOS_COPIA = arrayOf("application/octet-stream", "application/json", "text/plain")

        fun nombreDeUri(contexto: Context, uri: Uri): String? =
            runCatching {
                contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val columna = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columna >= 0 && cursor.moveToFirst()) cursor.getString(columna) else null
                }
            }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/')
    }
}
