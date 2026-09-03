package es.ghatostudio.nexapdf

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class NexaPdfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // PDFBox necesita cargar sus recursos (fuentes base, glifos) desde los
        // assets antes del primer uso. Es barato y evita un fallo en el primer
        // documento que se abra.
        PDFBoxResourceLoader.init(applicationContext)
    }
}
