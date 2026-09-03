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
        val motor = MotorPdfAndroid(servicios.directorioTrabajo)
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

        setContent { App(contenedor) }
    }

    private val almacenPreferencias: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { applicationContext.preferencesDataStoreFile("ajustes") },
        )
    }
}
