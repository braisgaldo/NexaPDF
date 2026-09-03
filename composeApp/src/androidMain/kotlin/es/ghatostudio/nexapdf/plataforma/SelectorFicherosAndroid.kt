package es.ghatostudio.nexapdf.plataforma

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import es.ghatostudio.nexapdf.domain.plataforma.FicheroElegido
import es.ghatostudio.nexapdf.domain.plataforma.SelectorFicheros
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Uri de la carpeta Descargas del almacenamiento principal. */
private val uriDescargas: Uri = DocumentsContract.buildDocumentUri(
    "com.android.externalstorage.documents",
    "primary:${Environment.DIRECTORY_DOWNLOADS}",
)

private fun Intent.empezandoEnDescargas(): Intent = apply {
    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriDescargas)
}

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
        actividad.registerForActivityResult(AbrirVariosEnDescargas()) { uris ->
            pendienteVarios?.complete(uris)
            pendienteVarios = null
        }

    private val abrirUno: ActivityResultLauncher<Array<String>> =
        actividad.registerForActivityResult(AbrirUnoEnDescargas()) { uri ->
            pendienteUno?.complete(uri)
            pendienteUno = null
        }

    private val crearDocumento: ActivityResultLauncher<String> =
        actividad.registerForActivityResult(GuardarEnDescargas()) { uri ->
            pendienteGuardar?.complete(uri)
            pendienteGuardar = null
        }

    private var pendienteCarpeta: CompletableDeferred<Uri?>? = null

    private val elegirArbol: ActivityResultLauncher<Uri?> =
        actividad.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            pendienteCarpeta?.complete(uri)
            pendienteCarpeta = null
        }

    private var pendienteImagenUna: CompletableDeferred<Uri?>? = null
    private var pendienteImagenVarias: CompletableDeferred<List<Uri>>? = null

    /**
     * Selector de fotos del sistema.
     *
     * No es el mismo que el de documentos. El de documentos ensena un
     * explorador de carpetas, que para buscar una foto entre miles es
     * inservible: el usuario espera su galeria, con las miniaturas y los
     * albumes. Ademas este selector no necesita ningun permiso de
     * almacenamiento, porque solo entrega lo que el usuario toca.
     *
     * En los telefonos sin selector de fotos, el propio contrato recae en
     * ACTION_OPEN_DOCUMENT sin que haya que hacer nada aqui.
     */
    private val elegirImagenUna: ActivityResultLauncher<PickVisualMediaRequest> =
        actividad.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            pendienteImagenUna?.complete(uri)
            pendienteImagenUna = null
        }

    private val elegirImagenVarias: ActivityResultLauncher<PickVisualMediaRequest> =
        actividad.registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(),
        ) { uris ->
            pendienteImagenVarias?.complete(uris)
            pendienteImagenVarias = null
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

    override suspend fun elegirImagenes(multiple: Boolean): List<FicheroElegido> {
        val peticion = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        val uris = if (multiple) {
            val espera = CompletableDeferred<List<Uri>>()
            pendienteImagenVarias = espera
            runCatching { elegirImagenVarias.launch(peticion) }.onFailure {
                pendienteImagenVarias = null
                return emptyList()
            }
            espera.await()
        } else {
            val espera = CompletableDeferred<Uri?>()
            pendienteImagenUna = espera
            runCatching { elegirImagenUna.launch(peticion) }.onFailure {
                pendienteImagenUna = null
                return emptyList()
            }
            listOfNotNull(espera.await())
        }
        return uris.mapNotNull { copiarAlEspacioDeTrabajo(it) }
    }

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

    override fun hayAlmacenDeClaves(): Boolean = true

    /**
     * Abre el dialogo del sistema para elegir un certificado instalado.
     *
     * Lo pinta Android, no la aplicacion: aqui solo llega el alias del que el
     * usuario haya decidido conceder acceso. Si no tiene ninguno instalado, el
     * propio dialogo se lo dice y ofrece instalarlo.
     */
    override suspend fun elegirDelAlmacenDeClaves(): String? {
        val espera = CompletableDeferred<String?>()
        val respuesta = KeyChainAliasCallback { alias -> espera.complete(alias) }
        runCatching {
            KeyChain.choosePrivateKeyAlias(
                actividad,
                respuesta,
                // Sin filtro de tipo de clave ni de emisor: los certificados de
                // firma espanoles son RSA, pero limitarlo dejaria fuera los de
                // curva eliptica que ya empiezan a emitirse.
                null,
                null,
                null,
                null,
            )
        }.onFailure { return null }
        return espera.await()
    }

    override suspend fun elegirCarpeta(): String? {
        val espera = CompletableDeferred<Uri?>()
        pendienteCarpeta = espera
        runCatching { elegirArbol.launch(uriDescargas) }.onFailure {
            pendienteCarpeta = null
            return null
        }
        val elegida = espera.await() ?: return null

        // Sin esto el permiso se pierde al cerrar la aplicacion y la carpeta
        // elegida dejaria de servir en el siguiente arranque.
        runCatching {
            actividad.contentResolver.takePersistableUriPermission(
                elegida,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        return elegida.toString()
    }

    override fun nombreDeCarpeta(uri: String): String? = runCatching {
        val arbol = Uri.parse(uri)
        val documento = DocumentsContract.buildDocumentUriUsingTree(
            arbol,
            DocumentsContract.getTreeDocumentId(arbol),
        )
        actividad.contentResolver.query(
            documento,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

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

    /**
     * Contratos que abren el selector en la carpeta Descargas.
     *
     * El selector del sistema arranca por defecto en "Reciente", que solo
     * enseña documentos abiertos hace poco *por alguna aplicacion*. En un
     * telefono recien estrenado, o con ficheros que llegaron por cable o desde
     * otra app, esa pantalla sale vacia y da toda la impresion de que no hay
     * ningun PDF en el telefono. Con EXTRA_INITIAL_URI se abre en Descargas,
     * que es donde esta casi todo lo que la gente quiere firmar o unir.
     *
     * Es una sugerencia, no una carcel: el usuario sigue teniendo el menu
     * lateral para irse a Drive, a la tarjeta SD o a donde quiera.
     */
    private class AbrirVariosEnDescargas : ActivityResultContracts.OpenMultipleDocuments() {
        override fun createIntent(context: Context, input: Array<String>): Intent =
            super.createIntent(context, input).empezandoEnDescargas()
    }

    private class AbrirUnoEnDescargas : ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: Context, input: Array<String>): Intent =
            super.createIntent(context, input).empezandoEnDescargas()
    }

    private class GuardarEnDescargas :
        ActivityResultContracts.CreateDocument("application/octet-stream") {
        override fun createIntent(context: Context, input: String): Intent =
            super.createIntent(context, input).empezandoEnDescargas()
    }

    private companion object {
        val TIPOS_PDF = arrayOf("application/pdf")

        /**
         * Todo lo que se puede llevar al espacio de trabajo. Se toma de
         * FormatoDocumento para que anadir un formato alli lo habilite tambien
         * en el selector, sin tener que acordarse de tocar dos sitios.
         */
        val TIPOS_PARA_UNIR =
            es.ghatostudio.nexapdf.domain.pdf.FormatoDocumento.TIPOS_PARA_UNIR.toTypedArray()

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
