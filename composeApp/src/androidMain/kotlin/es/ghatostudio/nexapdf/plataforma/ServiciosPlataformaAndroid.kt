package es.ghatostudio.nexapdf.plataforma

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.LocaleList
import android.provider.MediaStore
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import es.ghatostudio.nexapdf.domain.plataforma.ServiciosPlataforma
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class ServiciosPlataformaAndroid(
    private val contexto: Context,
    /** La actividad viva, para abrir Custom Tabs y hojas de compartir. */
    private val actividadActual: () -> Activity?,
) : ServiciosPlataforma {

    /**
     * Espacio de trabajo: copias temporales de los ficheros que el usuario abre.
     *
     * Se vacia al arrancar. Su contenido son copias de ficheros que el usuario
     * ya tiene en su telefono y que solo hacen falta mientras dura la tarea; sin
     * esta limpieza, cada documento abierto dejaria su copia dentro para
     * siempre y la app acabaria ocupando cientos de megas sin que se note.
     */
    override val directorioTrabajo: String =
        File(contexto.filesDir, "workspace").apply {
            runCatching { deleteRecursively() }
            mkdirs()
        }.absolutePath

    override val directorioSalida: String =
        File(contexto.filesDir, "output").apply { mkdirs() }.absolutePath

    override val directorioCopias: String =
        File(contexto.filesDir, "backups").apply { mkdirs() }.absolutePath

    override val nombrePlataforma: String
        get() = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    /**
     * En Android la donacion esta activa. La politica de Google Play permite
     * enlazar a una pasarela externa siempre que no compre nada dentro de la
     * app, que es justo el caso: NexaPDF no vende nada.
     */
    override val donacionesDisponibles: Boolean = true

    override val reducirAnimaciones: Boolean
        get() = runCatching {
            Settings.Global.getFloat(
                contexto.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)

    override fun ahora(): Long = System.currentTimeMillis()

    override fun formatearFecha(epochMillis: Long, conHora: Boolean): String {
        val formato = if (conHora) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, localeActiva())
        } else {
            DateFormat.getDateInstance(DateFormat.LONG, localeActiva())
        }
        return formato.format(Date(epochMillis))
    }

    override fun formatearTamano(bytes: Long): String {
        val unidades = listOf("B", "kB", "MB", "GB")
        var valor = bytes.toDouble()
        var indice = 0
        while (valor >= 1024 && indice < unidades.lastIndex) {
            valor /= 1024
            indice++
        }
        val decimales = if (indice == 0) 0 else 1
        return "${formatearNumero(valor, decimales)} ${unidades[indice]}"
    }

    override fun formatearNumero(valor: Double, decimales: Int): String =
        NumberFormat.getNumberInstance(localeActiva()).apply {
            minimumFractionDigits = decimales
            maximumFractionDigits = decimales
        }.format(valor)

    private fun localeActiva(): Locale =
        contexto.resources.configuration.locales.get(0) ?: Locale.getDefault()

    /**
     * Abre la URL fuera de la app.
     *
     * Se usa Custom Tabs, que es el navegador del sistema con la barra de
     * direcciones visible, nunca un WebView incrustado: un formulario de pago
     * dentro de la propia app es exactamente lo que las politicas de las tiendas
     * consideran una compra integrada.
     */
    override fun abrirEnNavegador(url: String) {
        val actividad = actividadActual() ?: return
        val uri = Uri.parse(url)
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()
                .launchUrl(actividad, uri)
        }.recoverCatching {
            actividad.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    override fun compartirFichero(ruta: String, tipoMime: String, asunto: String?) {
        val actividad = actividadActual() ?: return
        val fichero = File(ruta)
        if (!fichero.exists()) return
        val uri = FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            fichero,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = tipoMime
            putExtra(Intent.EXTRA_STREAM, uri)
            asunto?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        actividad.startActivity(Intent.createChooser(intent, asunto))
    }

    override fun compartirTexto(texto: String, asunto: String?) {
        val actividad = actividadActual() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            asunto?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        actividad.startActivity(Intent.createChooser(intent, asunto))
    }

    override fun copiarAlPortapapeles(texto: String) {
        val gestor = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        gestor?.setPrimaryClip(ClipData.newPlainText("NexaPDF", texto))
    }

    /**
     * Copia el resultado a la carpeta Descargas mediante MediaStore.
     *
     * MediaStore no exige ningun permiso de almacenamiento para escribir en la
     * coleccion de descargas, que es la razon de que NexaPDF no declare ninguno.
     */
    override suspend fun guardarEnDescargas(
        rutaOrigen: String,
        nombre: String,
        tipoMime: String,
    ): String? = withContext(Dispatchers.IO) {
        val origen = File(rutaOrigen)
        if (!origen.exists()) return@withContext null

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val valores = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, nombre)
                    put(MediaStore.Downloads.MIME_TYPE, tipoMime)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NexaPDF")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = contexto.contentResolver
                val destino = resolver.insert(
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    valores,
                ) ?: return@runCatching null

                resolver.openOutputStream(destino)?.use { salida ->
                    origen.inputStream().use { entrada -> entrada.copyTo(salida) }
                } ?: return@runCatching null

                resolver.update(
                    destino,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null,
                )
                "Descargas/NexaPDF/$nombre"
            } else {
                // En Android 8 y 9 aun se puede escribir directamente en la
                // carpeta publica de descargas del propio paquete.
                @Suppress("DEPRECATION")
                val carpeta = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "NexaPDF",
                ).apply { mkdirs() }
                val destino = File(carpeta, nombre)
                origen.copyTo(destino, overwrite = true)
                "Descargas/NexaPDF/$nombre"
            }
        }.getOrNull()
    }

    /**
     * Fija el idioma de la aplicacion.
     *
     * En Android 13 y posteriores se delega en LocaleManager, que es lo que
     * alimenta el selector de idioma por app de los ajustes del sistema. Por
     * debajo no existe esa API, asi que la preferencia se guarda en un
     * SharedPreferences que MainActivity lee en `attachBaseContext` para
     * envolver el contexto con la locale elegida, y la actividad se recrea.
     *
     * Se usa SharedPreferences y no DataStore solo para este dato porque
     * `attachBaseContext` corre antes de que haya corrutinas donde esperar una
     * lectura asincrona.
     */
    override fun aplicarIdioma(etiquetaBcp47: String?) {
        preferenciasIdioma.edit().apply {
            if (etiquetaBcp47 == null) remove(CLAVE_IDIOMA) else putString(CLAVE_IDIOMA, etiquetaBcp47)
        }.apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val gestor = contexto.getSystemService(android.app.LocaleManager::class.java)
            gestor?.applicationLocales = if (etiquetaBcp47 == null) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(etiquetaBcp47)
            }
        } else {
            actividadActual()?.recreate()
        }
    }

    override fun idiomaActual(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val gestor = contexto.getSystemService(android.app.LocaleManager::class.java)
            val lista = gestor?.applicationLocales ?: return null
            return if (lista.isEmpty) null else lista.get(0)?.toLanguageTag()
        }
        return preferenciasIdioma.getString(CLAVE_IDIOMA, null)
    }

    override fun idiomaDelSistema(): String =
        LocaleList.getDefault().get(0)?.toLanguageTag() ?: Locale.getDefault().toLanguageTag()

    private val preferenciasIdioma
        get() = contexto.getSharedPreferences(PREFERENCIAS_IDIOMA, Context.MODE_PRIVATE)

    companion object {
        const val PREFERENCIAS_IDIOMA = "nexapdf_idioma"
        const val CLAVE_IDIOMA = "etiqueta"

        /** Idioma guardado, legible sin corrutinas desde `attachBaseContext`. */
        fun idiomaGuardado(contexto: Context): String? =
            contexto.getSharedPreferences(PREFERENCIAS_IDIOMA, Context.MODE_PRIVATE)
                .getString(CLAVE_IDIOMA, null)
    }
}
