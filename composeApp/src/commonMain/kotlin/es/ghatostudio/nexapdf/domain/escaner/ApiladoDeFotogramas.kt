package es.ghatostudio.nexapdf.domain.escaner

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Funde varias fotos de la misma hoja en una sola, mas limpia.
 *
 * **Por que hace falta.** El limite de nitidez de un escaneo no es el filtro: es
 * el grano. La deconvolucion recupera detalle de verdad, pero amplifica el ruido
 * a la vez que el detalle, asi que hay un punto —sobre las diez vueltas— en el
 * que el papel empieza a ensuciarse mas rapido de lo que las letras ganan, y ahi
 * hay que parar. Con menos grano de partida, ese punto se aleja.
 *
 * Y el grano se puede quitar sin inventar nada: fotografiando varias veces. El
 * ruido del sensor es distinto en cada disparo y el papel es el mismo, asi que
 * promediando se cancela solo. Cuatro fotos dejan la mitad de ruido que una.
 *
 * **Lo que no es.** Esto no arregla una foto movida ni enfoca lo desenfocado. Lo
 * unico que hace es bajar el ruido para que el paso siguiente pueda apretar mas.
 *
 * **El problema, y como se resuelve.** Entre una foto y la siguiente la mano se
 * mueve. Promediar sin mas emborronaria la pagina, que seria justo lo contrario
 * de lo que se busca. Asi que antes de promediar se mide cuanto se ha movido
 * cada foto respecto a la primera —con precision de menos de un pixel, porque un
 * pixel entero ya se ve— y se remuestrea en su sitio.
 *
 * Y para lo que la medida no alcanza a corregir —un giro de la muneca, una
 * sombra que se mueve, un dedo que entra en el encuadre— esta el **rechazo**:
 * cada pixel se compara con el de la foto de referencia y solo se promedia si se
 * parece. Donde no se parece se usa el de referencia y ya. De ese modo el peor
 * caso de esta funcion no es una pagina borrosa, sino una pagina que simplemente
 * no ha ganado nada: se degrada quedandose quieta, no estropeando.
 *
 * Vive en `commonMain` y trabaja sobre enteros: es matematica y se prueba sin un
 * dispositivo.
 */
object ApiladoDeFotogramas {

    /**
     * Funde los fotogramas tomando el primero como referencia.
     *
     * @param fotogramas planos de luminancia del mismo tamano, ya enderezados.
     *   El primero manda: es el que fija el encuadre y con el que se comparan
     *   los demas.
     * @return un plano nuevo con la media robusta. Con un solo fotograma
     *   devuelve una copia y no hace nada.
     */
    fun apilar(fotogramas: List<IntArray>, ancho: Int, alto: Int): IntArray {
        require(fotogramas.isNotEmpty()) { "no hay fotogramas que apilar" }
        val total = ancho * alto
        val referencia = fotogramas.first()
        require(referencia.size >= total) { "el fotograma no cubre la imagen" }
        if (fotogramas.size == 1) return referencia.copyOf()

        val suma = IntArray(total) { referencia[it] }
        var cuentas = 1

        for (fotograma in fotogramas.drop(1)) {
            if (fotograma.size < total) continue
            val (dx, dy) = estimarDesplazamiento(referencia, fotograma, ancho, alto)
            // Un salto enorme no es temblor de la mano: es que esa foto no es de
            // la misma escena, o que la estimacion se ha perdido. Se descarta
            // entera antes que meter basura en la media.
            if (abs(dx) > MAXIMO_DESPLAZAMIENTO || abs(dy) > MAXIMO_DESPLAZAMIENTO) continue

            // Y aunque el desplazamiento parezca razonable, hay que comprobar
            // que alinear ha servido de algo. Hace falta **ademas** del rechazo
            // pixel a pixel: ese es local y deja pasar los pixeles que coinciden
            // por casualidad, que en una imagen ajena son muchos y bastan para
            // mover la pagina de sitio.
            //
            // La comprobacion es una proporcion y no un valor absoluto, y eso
            // importa: se compara el error en la mejor posicion con el error a
            // veinte pixeles de ahi. Si son parecidos, alinear no ha servido de
            // nada y esa foto no es de esta escena. Un corte absoluto no vale
            // porque el error depende muchisimo de lo que haya en la pagina: una
            // hoja con mucho texto, con medio pixel de desajuste en cada borde,
            // ya se sale de cualquier numero que sirva para una hoja mas vacia.
            val dxEntero = dx.roundToInt()
            val dyEntero = dy.roundToInt()
            val enSuSitio = error(referencia, fotograma, ancho, alto, dxEntero, dyEntero, PASO_FINO)
            val fueraDeSitio = error(
                referencia, fotograma, ancho, alto,
                dxEntero + DESVIO_DE_CONTROL, dyEntero + DESVIO_DE_CONTROL, PASO_FINO,
            )
            if (fueraDeSitio == Long.MAX_VALUE) continue
            if (enSuSitio > fueraDeSitio * PROPORCION_MAXIMA) continue

            for (y in 0 until alto) {
                val base = y * ancho
                for (x in 0 until ancho) {
                    val esperado = referencia[base + x]
                    val muestra = muestrear(fotograma, ancho, alto, x + dx, y + dy)
                    suma[base + x] += if (abs(muestra - esperado) <= UMBRAL_PARECIDO) {
                        muestra
                    } else {
                        esperado
                    }
                }
            }
            cuentas++
        }

        if (cuentas == 1) return referencia.copyOf()
        val salida = IntArray(total)
        for (indice in 0 until total) salida[indice] = suma[indice] / cuentas
        return salida
    }

    // --- Alineado ------------------------------------------------------------

    /**
     * Cuanto se ha movido [otro] respecto a [referencia], en pixeles.
     *
     * Se busca en dos pasos, de grueso a fino, porque buscar directamente en el
     * rango que hace falta seria carisimo: una pagina son casi nueve millones de
     * pixeles y cada posicion candidata obliga a recorrerla. Primero se busca
     * sobre una version reducida, que abarca mucho por poco dinero, y luego se
     * afina alrededor de lo encontrado.
     */
    private fun estimarDesplazamiento(
        referencia: IntArray,
        otro: IntArray,
        ancho: Int,
        alto: Int,
    ): Pair<Float, Float> {
        val anchoMenor = ancho / REDUCCION
        val altoMenor = alto / REDUCCION
        if (anchoMenor < 8 || altoMenor < 8) return 0f to 0f

        val refMenor = reducir(referencia, ancho, alto)
        val otroMenor = reducir(otro, ancho, alto)

        val grueso = mejorPosicion(
            refMenor, otroMenor, anchoMenor, altoMenor,
            centroX = 0, centroY = 0, radio = RADIO_GRUESO, paso = PASO_GRUESO,
        )

        // De vuelta a la escala real, y afinado ahi.
        val fino = mejorPosicion(
            referencia, otro, ancho, alto,
            centroX = grueso.first * REDUCCION,
            centroY = grueso.second * REDUCCION,
            radio = RADIO_FINO,
            paso = PASO_FINO,
        )
        return afinar(referencia, otro, ancho, alto, fino.first, fino.second)
    }

    /** Media de bloques de [REDUCCION] x [REDUCCION]. */
    private fun reducir(datos: IntArray, ancho: Int, alto: Int): IntArray {
        val anchoMenor = ancho / REDUCCION
        val altoMenor = alto / REDUCCION
        val salida = IntArray(anchoMenor * altoMenor)
        for (y in 0 until altoMenor) {
            for (x in 0 until anchoMenor) {
                var suma = 0
                for (dy in 0 until REDUCCION) {
                    val base = (y * REDUCCION + dy) * ancho + x * REDUCCION
                    for (dx in 0 until REDUCCION) suma += datos[base + dx]
                }
                salida[y * anchoMenor + x] = suma / (REDUCCION * REDUCCION)
            }
        }
        return salida
    }

    /** La posicion entera, dentro del radio, en la que las dos se parecen mas. */
    private fun mejorPosicion(
        referencia: IntArray,
        otro: IntArray,
        ancho: Int,
        alto: Int,
        centroX: Int,
        centroY: Int,
        radio: Int,
        paso: Int,
    ): Pair<Int, Int> {
        var mejorX = centroX
        var mejorY = centroY
        var mejorError = Long.MAX_VALUE
        for (dy in (centroY - radio)..(centroY + radio)) {
            for (dx in (centroX - radio)..(centroX + radio)) {
                val error = error(referencia, otro, ancho, alto, dx, dy, paso)
                if (error < mejorError) {
                    mejorError = error
                    mejorX = dx
                    mejorY = dy
                }
            }
        }
        return mejorX to mejorY
    }

    /**
     * Cuanto se diferencian dos fotogramas con un desplazamiento dado.
     *
     * Se mira uno de cada [paso] pixeles y se deja un margen en los bordes: la
     * parte que se sale al desplazar no existe en la otra foto, y compararla
     * seria comparar con nada.
     */
    private fun error(
        referencia: IntArray,
        otro: IntArray,
        ancho: Int,
        alto: Int,
        dx: Int,
        dy: Int,
        paso: Int,
    ): Long {
        val desdeX = max(MARGEN, -dx + MARGEN)
        val hastaX = min(ancho - MARGEN, ancho - dx - MARGEN)
        val desdeY = max(MARGEN, -dy + MARGEN)
        val hastaY = min(alto - MARGEN, alto - dy - MARGEN)
        if (desdeX >= hastaX || desdeY >= hastaY) return Long.MAX_VALUE

        var suma = 0L
        var cuenta = 0
        var y = desdeY
        while (y < hastaY) {
            val base = y * ancho
            val baseOtro = (y + dy) * ancho + dx
            var x = desdeX
            while (x < hastaX) {
                val diferencia = referencia[base + x] - otro[baseOtro + x]
                suma += (diferencia * diferencia).toLong()
                cuenta++
                x += paso
            }
            y += paso
        }
        // Normalizado, porque el numero de pixeles comparados cambia con el
        // desplazamiento y si no, el desplazamiento mas pequeno ganaria siempre.
        return if (cuenta == 0) Long.MAX_VALUE else suma / cuenta
    }

    /**
     * Afina el desplazamiento por debajo del pixel.
     *
     * Se ajusta una parabola al error en la posicion ganadora y sus dos vecinas,
     * en cada eje, y se toma su minimo. Sin esto el alineado se queda en pixeles
     * enteros, y medio pixel de error al promediar cuatro fotos ya se ve como
     * una pagina algo mas blanda que la original: se habria quitado ruido a
     * cambio de nitidez, que es un mal negocio.
     */
    private fun afinar(
        referencia: IntArray,
        otro: IntArray,
        ancho: Int,
        alto: Int,
        dx: Int,
        dy: Int,
    ): Pair<Float, Float> {
        val centro = error(referencia, otro, ancho, alto, dx, dy, PASO_FINO)
        val izquierda = error(referencia, otro, ancho, alto, dx - 1, dy, PASO_FINO)
        val derecha = error(referencia, otro, ancho, alto, dx + 1, dy, PASO_FINO)
        val arriba = error(referencia, otro, ancho, alto, dx, dy - 1, PASO_FINO)
        val abajo = error(referencia, otro, ancho, alto, dx, dy + 1, PASO_FINO)
        return (dx + vertice(izquierda, centro, derecha)) to
            (dy + vertice(arriba, centro, abajo))
    }

    /** Minimo de la parabola que pasa por tres errores consecutivos. */
    private fun vertice(antes: Long, centro: Long, despues: Long): Float {
        if (antes == Long.MAX_VALUE || despues == Long.MAX_VALUE) return 0f
        val denominador = (antes - 2 * centro + despues).toDouble()
        if (denominador <= 0.0) return 0f
        val ajuste = (antes - despues).toDouble() / (2.0 * denominador)
        return ajuste.coerceIn(-1.0, 1.0).toFloat()
    }

    /** Valor de la imagen en un punto que cae entre pixeles. */
    private fun muestrear(datos: IntArray, ancho: Int, alto: Int, x: Float, y: Float): Int {
        val xs = x.coerceIn(0f, (ancho - 1).toFloat())
        val ys = y.coerceIn(0f, (alto - 1).toFloat())
        val x0 = xs.toInt()
        val y0 = ys.toInt()
        val x1 = min(x0 + 1, ancho - 1)
        val y1 = min(y0 + 1, alto - 1)
        val fx = xs - x0
        val fy = ys - y0

        val arriba = datos[y0 * ancho + x0] * (1 - fx) + datos[y0 * ancho + x1] * fx
        val abajo = datos[y1 * ancho + x0] * (1 - fx) + datos[y1 * ancho + x1] * fx
        return (arriba + (abajo - arriba) * fy).roundToInt().coerceIn(0, 255)
    }

    /**
     * Cuanto puede diferenciarse un pixel de su referencia y seguir contando.
     *
     * Es la linea entre "ruido del sensor" y "aqui ha pasado otra cosa". El
     * grano de una foto de movil se mueve unos pocos niveles; un borde mal
     * alineado, una sombra que se mueve o un dedo que entra en el encuadre se
     * salen muchisimo. Con el umbral bajo no se ganaria ruido; con el alto se
     * colarian los fantasmas.
     */
    private const val UMBRAL_PARECIDO = 24

    /** Cuanto se reduce la imagen para la busqueda gruesa. */
    private const val REDUCCION = 4

    /** Radio de busqueda sobre la imagen reducida: abarca cuatro veces mas. */
    private const val RADIO_GRUESO = 8

    /** Radio de afinado a escala real, alrededor de lo que dijo la gruesa. */
    private const val RADIO_FINO = 3

    /** Uno de cada tantos pixeles al comparar: el minimo no se mueve por esto. */
    private const val PASO_GRUESO = 2
    private const val PASO_FINO = 8

    /** Borde que no se compara, porque al desplazar se sale de la otra foto. */
    private const val MARGEN = 8

    /**
     * Mas alla de esto no es temblor de la mano.
     *
     * Con el movil apoyado en las manos sobre una mesa, entre disparo y disparo
     * se mueven unas decenas de pixeles. Un salto mayor significa que la escena
     * ha cambiado o que la estimacion se ha perdido, y en los dos casos lo
     * correcto es no usar esa foto.
     */
    private const val MAXIMO_DESPLAZAMIENTO = 48f

    /** A que distancia se mira para saber si alinear ha servido de algo. */
    private const val DESVIO_DE_CONTROL = 20

    /**
     * Cuanto tiene que mejorar el error al alinear para fiarse de la foto.
     *
     * Con dos disparos seguidos de la misma hoja, la posicion buena es
     * muchisimo mejor que una a veinte pixeles: la proporcion sale por debajo de
     * la decima parte. Con una foto de otra cosa, moverla no mejora nada y la
     * proporcion se queda pegada a uno. El hueco entre los dos casos es enorme,
     * asi que el numero exacto no es delicado.
     */
    private const val PROPORCION_MAXIMA = 0.6
}
