package es.ghatostudio.nexapdf.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Carga fotos del disco a un tamano manejable y ya derechas.
 *
 * Se saca a un objeto propio porque lo usan dos motores: el de PDF, para
 * incrustar imagenes, y el del escaner, para enderezar paginas. Duplicarlo
 * significaba tener dos sitios donde arreglar el mismo fallo de orientacion.
 */
internal object CargadorImagen {

    /** Lado maximo por defecto: mas alla de esto una pagina no gana nitidez visible. */
    const val LADO_MAXIMO = 2600

    /**
     * Lado mayor de una pagina escaneada: 300 puntos por pulgada en un A4.
     *
     * Estuvo en 2900, que son unos 250 ppp, con el argumento de que por encima
     * el fichero crece sin que el ojo lo note. Medido sobre una foto real, el
     * argumento era falso: una hoja fotografiada con doce megapixeles ocupa unos
     * 3770 px de alto dentro del encuadre, asi que el tope no estaba
     * comprimiendo nada, estaba **tirando** un 23 % de resolucion lineal que ya
     * venia capturada. Comparadas al mismo tamano en pantalla, la letra pasa de
     * 20 a 26 px de alto y se nota a simple vista.
     *
     * 300 ppp es donde se para: es la resolucion a la que se digitaliza un
     * documento por convenio, por encima ya no hay detalle en la foto que
     * recuperar —el borde de una letra mide dos pixeles y medio en el original—
     * y el coste en memoria y en fichero si sigue subiendo.
     */
    const val LADO_PAGINA_ESCANEADA = 3500

    /**
     * Tope de lo que se carga en memoria de una sola foto.
     *
     * Tiene que ir por encima de [LADO_PAGINA_ESCANEADA] y con holgura: la
     * pagina es un recorte del encuadre, asi que para que la hoja salga a 3500
     * px hay que cargar la foto proporcionalmente mas grande. Con el tope justo,
     * una hoja que no llene el encuadre nunca alcanzaba su resolucion.
     *
     * Una foto de doce megapixeles a este tamano son unos cuarenta y cinco
     * megabytes de mapa de bits. Es mucho, y por eso hay un tope; pero cargarla
     * pequena "por si acaso" sale mas caro en calidad de lo que ahorra en
     * memoria, que es el error que habia antes.
     */
    const val LADO_CARGA_MAXIMO = 4200

    /**
     * Tamano de la foto tal y como se vera, con el giro del EXIF ya aplicado.
     *
     * Hace falta antes de decidir a que resolucion cargarla: el recorte de la
     * pagina es un trozo del encuadre, asi que para que la pagina salga a un
     * tamano concreto hay que cargar la foto proporcionalmente mas grande.
     * Leerlo cuesta un abrir y cerrar el fichero, sin decodificar nada.
     */
    fun medir(ruta: String): Pair<Int, Int>? {
        val fichero = File(ruta)
        if (!fichero.exists()) return null

        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichero.absolutePath, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

        val girada = runCatching {
            when (
                ExifInterface(fichero.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface.ORIENTATION_ROTATE_270,
                ExifInterface.ORIENTATION_TRANSPOSE,
                ExifInterface.ORIENTATION_TRANSVERSE,
                -> true

                else -> false
            }
        }.getOrDefault(false)

        return if (girada) medidas.outHeight to medidas.outWidth else medidas.outWidth to medidas.outHeight
    }

    /**
     * Devuelve la foto reducida al lado pedido y ya girada segun su EXIF.
     *
     * Reducir hace falta: una rafaga de fotos de cincuenta megapixeles agota la
     * memoria del telefono. Y girar tambien: las fotos hechas en vertical salen
     * tumbadas, porque la camara guarda el sensor en horizontal y anota el giro
     * aparte.
     *
     * Lo que no es obvio es **como** se reduce. `inSampleSize` solo admite
     * potencias de dos, asi que pidiendo 2600 px una foto de 4000 se quedaba en
     * 2000: la mitad del lado, la cuarta parte de los pixeles, tirados en cada
     * escaneo. Y eso justo antes de enderezar y de leer el texto, que son las
     * dos operaciones a las que mas les duele.
     *
     * Aqui el submuestreo se queda en el escalon que **no baja** del lado
     * pedido, y el ajuste fino lo hace el propio decodificador con
     * `inDensity`/`inTargetDensity`. El resultado tiene el tamano pedido de
     * verdad y no hay ningun mapa de bits intermedio a resolucion completa.
     */
    fun cargar(ruta: String, ladoMaximo: Int = LADO_MAXIMO): Bitmap? {
        val fichero = File(ruta)
        if (!fichero.exists()) return null

        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichero.absolutePath, medidas)
        val ancho = medidas.outWidth
        val alto = medidas.outHeight
        if (ancho <= 0 || alto <= 0) return null

        val lado = maxOf(ancho, alto)
        // El ultimo escalon que deja la imagen igual o por encima del objetivo.
        var muestreo = 1
        while (lado / (muestreo * 2) >= ladoMaximo) muestreo *= 2

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = muestreo
            val ladoTrasMuestreo = lado / muestreo
            if (ladoTrasMuestreo > ladoMaximo) {
                // El decodificador escala mientras decodifica. Los dos numeros
                // no son densidades de pantalla: son la razon de reduccion.
                inScaled = true
                inDensity = ladoTrasMuestreo
                inTargetDensity = ladoMaximo
            }
        }

        val mapa = BitmapFactory.decodeFile(fichero.absolutePath, opciones) ?: return null
        return girarSegunExif(mapa, fichero.absolutePath)
    }

    /**
     * Gira la imagen segun lo que diga su EXIF.
     *
     * Se contemplan tambien los espejados: algunas camaras frontales los
     * escriben, y una pagina escaneada del reves no se puede leer.
     */
    fun girarSegunExif(mapa: Bitmap, ruta: String): Bitmap {
        val orientacion = runCatching {
            ExifInterface(ruta)
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matriz = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matriz.postRotate(90f)
                matriz.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matriz.postRotate(270f)
                matriz.postScale(-1f, 1f)
            }
            else -> return mapa
        }

        val girado = Bitmap.createBitmap(mapa, 0, 0, mapa.width, mapa.height, matriz, true)
        if (girado != mapa) mapa.recycle()
        return girado
    }
}
