package es.ghatostudio.nexapdf.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File

/**
 * Elige con que fuente escribir un texto dentro del PDF.
 *
 * NexaPDF acepta texto en trece idiomas, y eso choca con como funcionan las
 * fuentes en PDF:
 *
 *  - Las catorce fuentes estandar del formato no necesitan incrustarse y dan un
 *    fichero diminuto, pero solo cubren Latin-1.
 *  - Una fuente del sistema incrustada como Type0 cubre ademas griego y
 *    cirilico, y el texto sigue siendo seleccionable y buscable.
 *  - Ni una ni otra saben *conformar* la escritura arabe, donde cada letra
 *    cambia de forma segun sus vecinas. Escribirla asi saldria en letras sueltas
 *    y del reves.
 *
 * De ahi la escalera: se prueba la opcion mas barata que represente el texto y,
 * si ninguna sirve, se avisa a quien llama para que lo dibuje como imagen con el
 * motor de texto de Android, que si sabe conformar cualquier escritura.
 */
class FuentesPdf(private val documento: PDDocument) {

    private val cache = mutableMapOf<String, PDFont?>()

    /** Resultado de elegir fuente para un texto concreto. */
    sealed interface Eleccion {
        data class Vectorial(val fuente: PDFont) : Eleccion

        /** Ninguna fuente disponible representa el texto: hay que rasterizarlo. */
        data object Rasterizar : Eleccion
    }

    fun elegir(texto: String, negrita: Boolean, cursiva: Boolean): Eleccion {
        if (texto.isBlank()) return Eleccion.Vectorial(estandar(negrita, cursiva))

        val estandar = estandar(negrita, cursiva)
        if (representa(estandar, texto)) return Eleccion.Vectorial(estandar)

        for (ruta in FUENTES_DEL_SISTEMA) {
            val fuente = cargar(ruta) ?: continue
            if (representa(fuente, texto)) return Eleccion.Vectorial(fuente)
        }

        return Eleccion.Rasterizar
    }

    private fun estandar(negrita: Boolean, cursiva: Boolean): PDFont = when {
        negrita && cursiva -> PDType1Font.HELVETICA_BOLD_OBLIQUE
        negrita -> PDType1Font.HELVETICA_BOLD
        cursiva -> PDType1Font.HELVETICA_OBLIQUE
        else -> PDType1Font.HELVETICA
    }

    private fun cargar(ruta: String): PDFont? = cache.getOrPut(ruta) {
        val fichero = File(ruta)
        if (!fichero.exists()) return@getOrPut null
        runCatching {
            fichero.inputStream().use { PDType0Font.load(documento, it, true) }
        }.getOrNull()
    }

    /**
     * Comprueba si la fuente puede escribir el texto.
     *
     * `getStringWidth` recorre los glifos igual que lo haria `showText`, asi que
     * lanza la misma excepcion ante un caracter que falte. Preguntarlo antes es
     * la unica forma de no dejar el flujo de contenido a medias: una vez
     * empezado a escribir no hay marcha atras.
     */
    private fun representa(fuente: PDFont, texto: String): Boolean = runCatching {
        fuente.getStringWidth(texto.replace('\n', ' '))
        true
    }.getOrDefault(false)

    private companion object {
        /**
         * Fuentes que Android trae desde hace muchas versiones. Roboto cubre
         * latino ampliado, griego y cirilico, que es lo que hace falta para diez
         * de los trece idiomas. No se empaqueta ninguna fuente propia porque la
         * unica que cubriria tambien chino y japones ronda los quince megabytes.
         */
        val FUENTES_DEL_SISTEMA = listOf(
            "/system/fonts/Roboto-Regular.ttf",
            "/system/fonts/RobotoStatic-Regular.ttf",
            "/system/fonts/DroidSans.ttf",
            "/system/fonts/NotoSans-Regular.ttf",
        )
    }
}
