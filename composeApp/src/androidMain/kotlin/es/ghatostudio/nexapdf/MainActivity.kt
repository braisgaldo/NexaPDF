package es.ghatostudio.nexapdf

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import es.ghatostudio.nexapdf.data.RepositorioAjustes
import es.ghatostudio.nexapdf.di.ContenedorApp
import es.ghatostudio.nexapdf.pdf.ConversorDocumentosAndroid
import es.ghatostudio.nexapdf.pdf.MotorPdfAndroid
import es.ghatostudio.nexapdf.plataforma.AlmacenFicherosAndroid
import es.ghatostudio.nexapdf.plataforma.SelectorFicherosAndroid
import es.ghatostudio.nexapdf.plataforma.ServiciosPlataformaAndroid
import java.util.Locale
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import es.ghatostudio.nexapdf.ui.navegacion.EntradaExterna

class MainActivity : ComponentActivity() {

    private lateinit var contenedor: ContenedorApp

    /**
     * Aplica el idioma elegido antes de que se cree nada de la interfaz.
     *
     * En Android 13 y posteriores lo hace LocaleManager por su cuenta; por
     * debajo hay que envolver el contexto a mano, y este es el unico momento en
     * que se puede hacer.
     */
    override fun attachBaseContext(base: Context) {
        val etiqueta = ServiciosPlataformaAndroid.idiomaGuardado(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || etiqueta == null) {
            super.attachBaseContext(base)
            return
        }
        val configuracion = Configuration(base.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(etiqueta))
        }
        super.attachBaseContext(base.createConfigurationContext(configuracion))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val servicios = ServiciosPlataformaAndroid(
            contexto = applicationContext,
            actividadActual = { this },
        )
        val motor = MotorPdfAndroid(applicationContext, servicios.directorioTrabajo)
        contenedor = ContenedorApp(
            motorPdf = motor,
            conversor = ConversorDocumentosAndroid(motor),
            servicios = servicios,
            ficheros = AlmacenFicherosAndroid(),
            // El selector registra sus lanzadores aqui dentro: hacerlo despues
            // de que la actividad este iniciada lanza una excepcion.
            selector = SelectorFicherosAndroid(this, servicios.directorioTrabajo),
            ajustes = RepositorioAjustes(almacenPreferencias),
        )

        setContent { App(contenedor, entradaExterna) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Con launchMode singleTask el sistema reutiliza la actividad viva, y
        // sin esto el segundo PDF que se abriera desde fuera no llegaria.
        setIntent(intent)
        recreate()
    }

    /**
     * Lo que se ha abierto o compartido con la aplicacion, si viene de fuera.
     *
     * El manifiesto declara cuatro casos y hasta ahora solo se resolvia uno:
     * ACTION_VIEW sobre un PDF. Compartir varios ficheros, compartir una foto o
     * abrir una copia de seguridad entraban y se quedaban en nada, porque todo
     * se leia como "la direccion de un PDF". Aqui se distingue cada caso para
     * que arriba se pueda llevar a su pantalla.
     *
     * Aqui solo se distingue lo que el intent sabe de verdad: la accion y si
     * lo que llega son imagenes. Que un fichero sea una copia de seguridad se
     * decide mas arriba, cuando ya se ha copiado y se conoce su nombre: en una
     * direccion `content://` de MediaStore la ruta es un numero, no el nombre,
     * y mirar ahi la extension no funciona.
     */
    private val entradaExterna: EntradaExterna?
        get() {
            val recibido = intent ?: return null
            val tipo = recibido.type.orEmpty()

            fun unaDireccion(): String? = IntentCompat
                .getParcelableExtra(recibido, Intent.EXTRA_STREAM, Uri::class.java)
                ?.toString()

            fun variasDirecciones(): List<String> = IntentCompat
                .getParcelableArrayListExtra(recibido, Intent.EXTRA_STREAM, Uri::class.java)
                ?.map { it.toString() }
                .orEmpty()

            return when (recibido.action) {
                Intent.ACTION_VIEW ->
                    recibido.data?.toString()?.let { EntradaExterna.UnDocumento(it) }

                Intent.ACTION_SEND -> {
                    val direccion = unaDireccion() ?: return null
                    if (tipo.startsWith("image/")) {
                        EntradaExterna.Imagenes(listOf(direccion))
                    } else {
                        EntradaExterna.UnDocumento(direccion)
                    }
                }

                Intent.ACTION_SEND_MULTIPLE -> {
                    val direcciones = variasDirecciones().ifEmpty { return null }
                    if (tipo.startsWith("image/")) {
                        EntradaExterna.Imagenes(direcciones)
                    } else {
                        EntradaExterna.VariosDocumentos(direcciones)
                    }
                }

                else -> null
            }
        }


    private val almacenPreferencias: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { applicationContext.preferencesDataStoreFile("ajustes") },
        )
    }
}
