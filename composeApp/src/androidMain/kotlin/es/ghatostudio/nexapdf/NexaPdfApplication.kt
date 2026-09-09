package es.ghatostudio.nexapdf

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class NexaPdfApplication : Application() {

    /**
     * El almacen de ajustes, uno solo en todo el proceso.
     *
     * Vive aqui y no en la actividad por una razon que costo un cierre
     * inesperado: DataStore aborta si detecta dos instancias abiertas sobre el
     * mismo fichero. Colgado de la actividad, bastaba con que hubiera dos
     * —abrir un segundo documento desde otra aplicacion recreaba la actividad—
     * para que la aplicacion se cerrara.
     */
    val almacenPreferencias: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { applicationContext.preferencesDataStoreFile("ajustes") },
        )
    }

    override fun onCreate() {
        super.onCreate()
        // PDFBox necesita cargar sus recursos (fuentes base, glifos) desde los
        // assets antes del primer uso. Es barato y evita un fallo en el primer
        // documento que se abra.
        PDFBoxResourceLoader.init(applicationContext)
    }
}
