package es.ghatostudio.nexapdf.domain.escaner

import es.ghatostudio.nexapdf.domain.model.Punto
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Encuentra los cuatro bordes de un papel dentro de una imagen en escala de
 * grises.
 *
 * Es Kotlin puro y vive en `commonMain` a proposito: no depende de Android ni de
 * OpenCV. Meter OpenCV por esto habria sumado unos veinte megabytes de codigo
 * nativo a una aplicacion que entera pesa seis, y habria dejado la deteccion
 * fuera del alcance de las pruebas unitarias.
 *
 * El metodo es el clasico y tiene cuatro pasos:
 *
 *  1. **Reducir**. Se trabaja sobre una copia de unos 180 px de lado. Los bordes
 *     de un folio siguen ahi y el coste baja dos ordenes de magnitud, que es lo
 *     que permite hacerlo en cada fotograma de la vista previa.
 *  2. **Sobel**. Gradiente en las dos direcciones; lo que no es contorno se cae
 *     con un umbral sacado del histograma, no fijo: una foto a contraluz y otra
 *     sobre folio blanco no comparten umbral.
 *  3. **Hough**. Cada pixel de contorno vota por todas las rectas que pasan por
 *     el. Las cuatro rectas mas votadas, dos casi verticales y dos casi
 *     horizontales, son los bordes del papel.
 *  4. **Comprobar**. De las combinaciones posibles se queda la que forma un
 *     cuadrilatero convexo, de area razonable y con angulos cerca de los 90
 *     grados, y se mide **que parte de su contorno cae de verdad sobre pixeles
 *     de borde**. Ese porcentaje es la confianza que se ensena en pantalla.
 *
 * Una instancia guarda sus buffers y **no es segura entre hilos**: se crea una
 * por camara y se usa siempre desde el hilo de analisis.
 */
class DetectorDocumento {

    private var ancho = 0
    private var alto = 0
    private var gris = IntArray(0)
    private var suave = IntArray(0)
    private var magnitud = IntArray(0)
    private var acumulador = IntArray(0)
    private var rhos = 0

    private val senos = FloatArray(PASOS_ANGULO)
    private val cosenos = FloatArray(PASOS_ANGULO)

    init {
        for (paso in 0 until PASOS_ANGULO) {
            val angulo = paso * PI.toFloat() / PASOS_ANGULO
            senos[paso] = sin(angulo)
            cosenos[paso] = cos(angulo)
        }
    }

    /**
     * Busca el papel en un plano de luminancia.
     *
     * @param luminancia un byte por pixel, sin signo. Es justo lo que entrega el
     *   plano Y de la camara, asi que no hay ninguna conversion de color de por
     *   medio: de ahi que se pueda hacer en cada fotograma.
     * @param saltoDeFila bytes entre el principio de una fila y el de la
     *   siguiente. La camara suele alinear las filas y este numero es mayor que
     *   el ancho; ignorarlo inclina la imagen y no se detecta nada.
     */
    fun detectar(
        luminancia: ByteArray,
        anchoOrigen: Int,
        altoOrigen: Int,
        saltoDeFila: Int = anchoOrigen,
    ): DeteccionDocumento {
        if (anchoOrigen < 16 || altoOrigen < 16) return DeteccionDocumento.NADA

        val paso = max(1, max(anchoOrigen, altoOrigen) / LADO_TRABAJO)
        val w = anchoOrigen / paso
        val h = altoOrigen / paso
        if (w < 16 || h < 16) return DeteccionDocumento.NADA

        prepararBuffers(w, h)
        reducir(luminancia, saltoDeFila, paso)
        desenfocar()
        val umbral = sobelYUmbral()
        if (umbral == null) return DeteccionDocumento.NADA

        val lineas = rectasCandidatas(umbral)
        if (lineas.size < 4) return DeteccionDocumento.NADA

        return mejorCuadrilatero(lineas, umbral)
    }

    // --- Pasos ---------------------------------------------------------------

    private fun prepararBuffers(w: Int, h: Int) {
        if (ancho == w && alto == h) {
            acumulador.fill(0)
            return
        }
        ancho = w
        alto = h
        gris = IntArray(w * h)
        suave = IntArray(w * h)
        magnitud = IntArray(w * h)
        // El radio va de -diagonal a +diagonal, con un bin por pixel: mas fino
        // no distingue mejor un borde de folio y multiplica el acumulador.
        rhos = 2 * sqrt((w * w + h * h).toFloat()).toInt() + 3
        acumulador = IntArray(PASOS_ANGULO * rhos)
    }

    private fun reducir(luminancia: ByteArray, saltoDeFila: Int, paso: Int) {
        for (y in 0 until alto) {
            val filaOrigen = y * paso * saltoDeFila
            val filaDestino = y * ancho
            for (x in 0 until ancho) {
                val indice = filaOrigen + x * paso
                gris[filaDestino + x] = if (indice < luminancia.size) {
                    luminancia[indice].toInt() and 0xFF
                } else {
                    0
                }
            }
        }
    }

    /** Media de 3x3: sin esto el grano del sensor vota en el Hough como si fuera contorno. */
    private fun desenfocar() {
        for (y in 0 until alto) {
            val arriba = max(0, y - 1)
            val abajo = min(alto - 1, y + 1)
            for (x in 0 until ancho) {
                val izquierda = max(0, x - 1)
                val derecha = min(ancho - 1, x + 1)
                var suma = 0
                for (fila in arriba..abajo) {
                    val base = fila * ancho
                    for (columna in izquierda..derecha) suma += gris[base + columna]
                }
                val celdas = (abajo - arriba + 1) * (derecha - izquierda + 1)
                suave[y * ancho + x] = suma / celdas
            }
        }
    }

    /**
     * Gradiente de Sobel y umbral sacado del histograma.
     *
     * Devuelve `null` cuando en la escena no hay contornos que valgan: una mesa
     * vacia o una pared. Sin ese corte, el Hough encuentra "rectas" en el ruido
     * y el porcentaje sube apuntando a cualquier sitio, que es la forma mas
     * rapida de que el usuario deje de fiarse del numero.
     */
    private fun sobelYUmbral(): Int? {
        val histograma = IntArray(HISTOGRAMA)
        magnitud.fill(0)

        for (y in 1 until alto - 1) {
            val fila = y * ancho
            val previa = fila - ancho
            val siguiente = fila + ancho
            for (x in 1 until ancho - 1) {
                val gx = (suave[previa + x + 1] + 2 * suave[fila + x + 1] + suave[siguiente + x + 1]) -
                    (suave[previa + x - 1] + 2 * suave[fila + x - 1] + suave[siguiente + x - 1])
                val gy = (suave[siguiente + x - 1] + 2 * suave[siguiente + x] + suave[siguiente + x + 1]) -
                    (suave[previa + x - 1] + 2 * suave[previa + x] + suave[previa + x + 1])
                val valor = abs(gx) + abs(gy)
                magnitud[fila + x] = valor
                histograma[min(HISTOGRAMA - 1, valor / ESCALA_HISTOGRAMA)]++
            }
        }

        val total = (ancho - 2) * (alto - 2)
        if (total <= 0) return null

        // Se conserva la fraccion mas fuerte del gradiente. Es un umbral
        // relativo: lo que en una foto a plena luz es un borde flojo, en una
        // penumbra es el borde mas marcado que hay.
        val objetivo = (total * FRACCION_BORDES).toInt().coerceAtLeast(40)
        var acumulados = 0
        var corte = HISTOGRAMA - 1
        for (cubo in HISTOGRAMA - 1 downTo 0) {
            acumulados += histograma[cubo]
            if (acumulados >= objetivo) {
                corte = cubo
                break
            }
        }
        val umbral = max(corte * ESCALA_HISTOGRAMA, UMBRAL_MINIMO)

        // Si ni con el umbral minimo hay contornos suficientes, no hay papel.
        var fuertes = 0
        for (cubo in HISTOGRAMA - 1 downTo umbral / ESCALA_HISTOGRAMA) fuertes += histograma[cubo]
        return if (fuertes < objetivo / 4) null else umbral
    }

    /**
     * Rectas mas votadas, con supresion de vecinos y afinado entre celdas.
     *
     * El afinado es lo que quita la mayor parte del temblor. El acumulador tiene
     * celdas de dos grados y de un pixel de radio; quedarse con el centro de la
     * celda hace que un borde que no se mueve salte de celda en celda con el
     * ruido del sensor, y ese salto se ve en pantalla aunque el papel este
     * quieto. Ajustando una parabola a la celda y sus dos vecinas se recupera la
     * posicion real dentro de la celda, que si varia de forma continua.
     */
    private fun rectasCandidatas(umbral: Int): List<Recta> {
        val desplazamiento = rhos / 2

        for (y in 1 until alto - 1) {
            val fila = y * ancho
            for (x in 1 until ancho - 1) {
                if (magnitud[fila + x] < umbral) continue
                for (t in 0 until PASOS_ANGULO) {
                    val rho = x * cosenos[t] + y * senos[t]
                    val indice = rho.roundToInt() + desplazamiento
                    if (indice in 0 until rhos) acumulador[t * rhos + indice]++
                }
            }
        }

        // Una recta que cruce la imagen entera recoge tantos votos como pixeles
        // tenga de largo. Se exige una fraccion de eso para no quedarse con
        // trocitos de contorno sueltos.
        val minimoVotos = max(MINIMO_VOTOS, (min(ancho, alto) * FRACCION_VOTOS).toInt())
        val encontradas = ArrayList<Recta>()

        for (t in 0 until PASOS_ANGULO) {
            val base = t * rhos
            for (r in 0 until rhos) {
                val votos = acumulador[base + r]
                if (votos < minimoVotos) continue
                if (!esMaximoLocal(t, r, votos)) continue
                encontradas += Recta(
                    angulo = (t + afinarAngulo(t, r)) * MEDIO_GIRO / PASOS_ANGULO,
                    rho = (r - desplazamiento) + afinarRadio(t, r),
                    votos = votos,
                )
            }
        }

        return encontradas.sortedByDescending { it.votos }.take(MAXIMO_RECTAS)
    }

    /** Vertice de la parabola que pasa por la celda y sus dos vecinas en radio. */
    private fun afinarRadio(t: Int, r: Int): Float {
        if (r <= 0 || r >= rhos - 1) return 0f
        val base = t * rhos
        return vertice(acumulador[base + r - 1], acumulador[base + r], acumulador[base + r + 1])
    }

    /**
     * Lo mismo en el angulo.
     *
     * En los extremos no se afina: ahi el angulo se envuelve y el radio cambia
     * de signo, asi que la celda vecina no mide lo mismo. Mejor no afinar que
     * afinar mal.
     */
    private fun afinarAngulo(t: Int, r: Int): Float {
        if (t == 0 || t == PASOS_ANGULO - 1) return 0f
        return vertice(
            acumulador[(t - 1) * rhos + r],
            acumulador[t * rhos + r],
            acumulador[(t + 1) * rhos + r],
        )
    }

    private fun vertice(izquierda: Int, centro: Int, derecha: Int): Float {
        val denominador = izquierda - 2 * centro + derecha
        if (denominador == 0) return 0f
        return (0.5f * (izquierda - derecha) / denominador).coerceIn(-0.5f, 0.5f)
    }

    private fun esMaximoLocal(t: Int, r: Int, votos: Int): Boolean {
        for (dt in -VECINDAD_ANGULO..VECINDAD_ANGULO) {
            // El angulo es circular: 0 y 180 grados son la misma direccion, solo
            // que con el radio cambiado de signo. Se mira el vecino de verdad en
            // lugar de cortar en los extremos.
            var vecinoT = t + dt
            var invertir = false
            if (vecinoT < 0) {
                vecinoT += PASOS_ANGULO
                invertir = true
            } else if (vecinoT >= PASOS_ANGULO) {
                vecinoT -= PASOS_ANGULO
                invertir = true
            }
            for (dr in -VECINDAD_RADIO..VECINDAD_RADIO) {
                if (dt == 0 && dr == 0) continue
                val vecinoR = if (invertir) rhos - 1 - (r + dr) else r + dr
                if (vecinoR !in 0 until rhos) continue
                if (acumulador[vecinoT * rhos + vecinoR] > votos) return false
            }
        }
        return true
    }

    /**
     * Elige el mejor cuadrilatero entre las rectas encontradas.
     *
     * Las dos familias de bordes —los dos lados largos y los dos cortos— **no**
     * se separan por "casi vertical" y "casi horizontal". Eso valia para un
     * folio derecho y fallaba justo donde mas se nota: un papel girado treinta
     * grados sobre la mesa mete sus cuatro bordes del mismo lado del corte, se
     * queda sin ninguna pareja que cruzar y la deteccion desaparece.
     *
     * Aqui las familias se definen **respecto al propio papel**: se toma una
     * recta fuerte como referencia, una familia son las que van con ella y la
     * otra las que le cruzan en angulo recto. Asi da igual como este girado.
     *
     * Se prueba con varias referencias porque la recta mas votada no siempre es
     * un borde del papel: puede ser el canto de la mesa. Cada candidata produce
     * su cuadrilatero y gana el que mas contorno real tenga debajo.
     */
    private fun mejorCuadrilatero(rectas: List<Recta>, umbral: Int): DeteccionDocumento {
        var mejor: DeteccionDocumento = DeteccionDocumento.NADA

        for (referencia in rectas.take(REFERENCIAS)) {
            val conLaReferencia = ArrayList<Recta>()
            val cruzadas = ArrayList<Recta>()
            for (recta in rectas) {
                val alineada = alinear(recta, referencia.angulo)
                val diferencia = diferenciaAngular(alineada.angulo, referencia.angulo)
                if (diferencia <= TOLERANCIA_FAMILIA) {
                    conLaReferencia += alineada
                } else if (abs(diferencia - MEDIO_GIRO / 2f) <= TOLERANCIA_FAMILIA) {
                    cruzadas += alinear(recta, referencia.angulo + MEDIO_GIRO / 2f)
                }
            }
            if (conLaReferencia.size < 2 || cruzadas.size < 2) continue

            val candidato = combinar(
                conLaReferencia.take(MAXIMO_POR_FAMILIA),
                cruzadas.take(MAXIMO_POR_FAMILIA),
                umbral,
            )
            if (candidato.confianza > mejor.confianza) mejor = candidato
        }
        return mejor
    }

    private fun combinar(
        unaFamilia: List<Recta>,
        laOtra: List<Recta>,
        umbral: Int,
    ): DeteccionDocumento {
        val unas = parejasSeparadas(unaFamilia)
        val otras = parejasSeparadas(laOtra)
        if (unas.isEmpty() || otras.isEmpty()) return DeteccionDocumento.NADA

        var mejor: DeteccionDocumento = DeteccionDocumento.NADA
        var evaluados = 0

        for ((primera, segunda) in unas) {
            for ((tercera, cuarta) in otras) {
                if (evaluados >= MAXIMO_CANDIDATOS) return mejor
                val cuadro = cuadrilateroDe(primera, segunda, tercera, cuarta) ?: continue
                if (!esPlausible(cuadro)) continue
                evaluados++
                val confianza = confianzaDe(cuadro, umbral)
                if (confianza > mejor.confianza) {
                    mejor = DeteccionDocumento(normalizar(cuadro), confianza)
                }
            }
        }
        return mejor
    }

    /**
     * Parejas de rectas de la misma familia lo bastante separadas entre si.
     *
     * Ya vienen alineadas, es decir, con el angulo dentro de la misma ventana y
     * el radio con el signo que le corresponde. Eso es lo que permite medir la
     * separacion restando radios sin mas: sin alinear, dos bordes opuestos del
     * mismo folio pueden salir con radios casi iguales solo porque uno de los
     * dos angulos cayo al otro lado de los ciento ochenta grados.
     */
    private fun parejasSeparadas(rectas: List<Recta>): List<Pair<Recta, Recta>> {
        val diagonal = sqrt((ancho * ancho + alto * alto).toFloat())
        val separacion = diagonal * SEPARACION_MINIMA
        val parejas = ArrayList<Pair<Recta, Recta>>()
        for (primera in rectas.indices) {
            for (segunda in primera + 1 until rectas.size) {
                val a = rectas[primera]
                val b = rectas[segunda]
                // Casi paralelas: si no lo son, no son dos bordes opuestos de la
                // misma hoja sino un borde y otra cosa cualquiera.
                if (diferenciaAngular(a.angulo, b.angulo) > DESVIO_PARALELAS) continue
                if (abs(a.rho - b.rho) < separacion) continue
                parejas += a to b
            }
        }
        return parejas.sortedByDescending { it.first.votos + it.second.votos }
            .take(MAXIMO_PAREJAS)
    }

    private fun cuadrilateroDe(
        primera: Recta,
        segunda: Recta,
        tercera: Recta,
        cuarta: Recta,
    ): Cuadrilatero? {
        val esquinas = listOf(
            cortar(primera, tercera),
            cortar(segunda, tercera),
            cortar(segunda, cuarta),
            cortar(primera, cuarta),
        )
        if (esquinas.any { it == null }) return null
        val puntos = esquinas.filterNotNull().map { Punto(it.x / ancho, it.y / alto) }
        // Se admite un poco de desborde: un folio que llena el encuadre tiene
        // los bordes justo en el limite, y exigirlos dentro lo descartaria.
        if (puntos.any { it.x < -MARGEN_FUERA || it.x > 1f + MARGEN_FUERA }) return null
        if (puntos.any { it.y < -MARGEN_FUERA || it.y > 1f + MARGEN_FUERA }) return null
        return Cuadrilatero.ordenar(puntos)
    }

    private fun cortar(a: Recta, b: Recta): Punto? {
        val divisor = sin(b.angulo - a.angulo)
        if (abs(divisor) < 1e-4f) return null
        val cosA = cos(a.angulo)
        val senA = sin(a.angulo)
        val cosB = cos(b.angulo)
        val senB = sin(b.angulo)
        val x = (a.rho * senB - b.rho * senA) / divisor
        val y = (b.rho * cosA - a.rho * cosB) / divisor
        return Punto(x, y)
    }

    /** Descarta formas que un papel no puede tener. */
    private fun esPlausible(cuadro: Cuadrilatero): Boolean {
        if (!cuadro.esConvexo) return false
        val area = cuadro.area
        if (area < AREA_MINIMA || area > AREA_MAXIMA) return false
        return desvioMaximoDeAngulos(cuadro) <= DESVIO_ANGULO_MAXIMO
    }

    /**
     * Cuanto se aleja de 90 grados el peor de los cuatro angulos.
     *
     * Es la comprobacion que descarta los trapecios imposibles: una hoja vista
     * en perspectiva se deforma, pero no tanto como para que una esquina baje de
     * 50 grados sin que la foto sea inservible.
     */
    private fun desvioMaximoDeAngulos(cuadro: Cuadrilatero): Float {
        val puntos = cuadro.esquinas
        var peor = 0f
        for (indice in puntos.indices) {
            val previo = puntos[(indice + 3) % 4]
            val actual = puntos[indice]
            val siguiente = puntos[(indice + 1) % 4]
            val ax = previo.x - actual.x
            val ay = previo.y - actual.y
            val bx = siguiente.x - actual.x
            val by = siguiente.y - actual.y
            val normaA = sqrt(ax * ax + ay * ay)
            val normaB = sqrt(bx * bx + by * by)
            if (normaA < 1e-5f || normaB < 1e-5f) return 180f
            val coseno = ((ax * bx + ay * by) / (normaA * normaB)).coerceIn(-1f, 1f)
            val grados = acos(coseno) * 180f / PI.toFloat()
            peor = max(peor, abs(grados - 90f))
        }
        return peor
    }

    /**
     * Que parte del contorno propuesto cae sobre pixeles de borde reales.
     *
     * Esta es la cifra honesta, y por eso es la que se ensena. Un cuadrilatero
     * puede encajar en cuatro rectas muy votadas y aun asi tener un lado
     * inventado, porque el Hough vota por rectas infinitas y no sabe donde
     * empieza y acaba el segmento. Recorrer los cuatro lados y contar cuantas
     * muestras tienen contorno debajo es lo unico que distingue "he encontrado
     * el papel" de "he encontrado cuatro rectas".
     */
    private fun confianzaDe(cuadro: Cuadrilatero, umbral: Int): Float {
        val puntos = cuadro.esquinas
        var conBorde = 0
        var muestras = 0

        for (indice in puntos.indices) {
            val desde = puntos[indice]
            val hasta = puntos[(indice + 1) % 4]
            for (muestra in 0 until MUESTRAS_POR_LADO) {
                val t = (muestra + 0.5f) / MUESTRAS_POR_LADO
                val x = (desde.x + (hasta.x - desde.x) * t) * ancho
                val y = (desde.y + (hasta.y - desde.y) * t) * alto
                muestras++
                if (hayBordeCerca(x.roundToInt(), y.roundToInt(), umbral)) conBorde++
            }
        }
        if (muestras == 0) return 0f

        val cobertura = conBorde.toFloat() / muestras
        return (cobertura * factorArea(cuadro.area) * factorAngulos(cuadro)).coerceIn(0f, 1f)
    }

    private fun hayBordeCerca(x: Int, y: Int, umbral: Int): Boolean {
        for (dy in -RADIO_BUSQUEDA..RADIO_BUSQUEDA) {
            val fila = y + dy
            if (fila < 1 || fila >= alto - 1) continue
            val base = fila * ancho
            for (dx in -RADIO_BUSQUEDA..RADIO_BUSQUEDA) {
                val columna = x + dx
                if (columna < 1 || columna >= ancho - 1) continue
                if (magnitud[base + columna] >= umbral) return true
            }
        }
        return false
    }

    /** Un papel diminuto en el encuadre se detecta peor y se recorta peor. */
    private fun factorArea(area: Float): Float = when {
        area >= AREA_COMODA -> 1f
        area <= AREA_MINIMA -> 0.6f
        else -> 0.6f + 0.4f * (area - AREA_MINIMA) / (AREA_COMODA - AREA_MINIMA)
    }

    private fun factorAngulos(cuadro: Cuadrilatero): Float {
        val desvio = desvioMaximoDeAngulos(cuadro)
        return when {
            desvio <= DESVIO_COMODO -> 1f
            desvio >= DESVIO_ANGULO_MAXIMO -> 0.7f
            else -> 1f - 0.3f * (desvio - DESVIO_COMODO) /
                (DESVIO_ANGULO_MAXIMO - DESVIO_COMODO)
        }
    }

    private fun normalizar(cuadro: Cuadrilatero): Cuadrilatero = Cuadrilatero(
        supIzq = acotar(cuadro.supIzq),
        supDer = acotar(cuadro.supDer),
        infDer = acotar(cuadro.infDer),
        infIzq = acotar(cuadro.infIzq),
    )

    private fun acotar(punto: Punto) =
        Punto(punto.x.coerceIn(0f, 1f), punto.y.coerceIn(0f, 1f))

    /**
     * Reescribe una recta con el angulo dentro de la ventana de la referencia.
     *
     * Una recta, y la misma recta con el angulo girado media vuelta y el radio
     * cambiado de signo, son la misma recta. El Hough siempre devuelve el angulo
     * entre cero y ciento ochenta grados, asi que dos bordes paralelos pueden
     * salir uno a cinco grados y otro a ciento setenta y cinco. Aqui se lleva el
     * segundo junto al primero, que es lo que permite despues restarles los
     * radios para saber lo separados que estan.
     */
    private fun alinear(recta: Recta, referencia: Float): Recta {
        var angulo = recta.angulo
        var rho = recta.rho
        while (angulo - referencia > MEDIO_GIRO / 2f) {
            angulo -= MEDIO_GIRO
            rho = -rho
        }
        while (referencia - angulo > MEDIO_GIRO / 2f) {
            angulo += MEDIO_GIRO
            rho = -rho
        }
        return Recta(angulo, rho, recta.votos)
    }

    /** Diferencia entre dos angulos de recta, que se repiten cada media vuelta. */
    private fun diferenciaAngular(a: Float, b: Float): Float {
        var diferencia = abs(a - b) % MEDIO_GIRO
        if (diferencia > MEDIO_GIRO / 2f) diferencia = MEDIO_GIRO - diferencia
        return diferencia
    }

    /**
     * Una recta en forma normal: rho = x*cos(angulo) + y*sin(angulo).
     *
     * El angulo va en radianes y no como indice del acumulador porque tras el
     * afinado cae entre dos celdas, que es justo de donde sale la estabilidad
     * del contorno en pantalla.
     */
    private data class Recta(val angulo: Float, val rho: Float, val votos: Int)

    private companion object {
        /**
         * Lado mayor de la imagen de trabajo.
         *
         * Medido: a este tamano un fotograma cuesta un par de milisegundos en
         * una JVM de escritorio, asi que en el telefono queda holgado dentro del
         * tiempo de un fotograma. Se subio de 180 porque a esa resolucion los
         * bordes de un folio que ocupa media pantalla median poco mas de cien
         * pixeles, y las rectas salian cortas y mal situadas.
         */
        const val LADO_TRABAJO = 240

        /** Pasos de angulo del Hough: 120 pasos son 1,5 grados cada uno. */
        const val PASOS_ANGULO = 120

        const val HISTOGRAMA = 256
        const val ESCALA_HISTOGRAMA = 8
        const val UMBRAL_MINIMO = 90
        const val FRACCION_BORDES = 0.08f

        const val MINIMO_VOTOS = 22
        const val FRACCION_VOTOS = 0.30f
        const val VECINDAD_ANGULO = 3
        const val VECINDAD_RADIO = 6

        /** Media vuelta en radianes: el periodo del angulo de una recta. */
        const val MEDIO_GIRO = PI.toFloat()

        /** Rectas que pasan a la fase de emparejado. */
        const val MAXIMO_RECTAS = 40

        /**
         * Rectas fuertes que se prueban como orientacion del papel.
         *
         * Mas de una porque la mas votada puede ser el canto de la mesa o la
         * juntura del suelo, y entonces las dos familias salen giradas respecto
         * al folio. Con tres referencias basta con que una acierte.
         */
        const val REFERENCIAS = 3

        /** Cuanto puede desviarse una recta de su familia: treinta grados. */
        const val TOLERANCIA_FAMILIA = 0.52f

        const val MAXIMO_POR_FAMILIA = 8
        const val MAXIMO_PAREJAS = 10
        const val MAXIMO_CANDIDATOS = 28

        /** Separacion minima entre dos bordes opuestos, en fraccion de la diagonal. */
        const val SEPARACION_MINIMA = 0.20f

        /** Dos bordes opuestos convergen por la perspectiva, pero no 24 grados. */
        const val DESVIO_PARALELAS = 0.42f

        const val MARGEN_FUERA = 0.10f
        const val AREA_MINIMA = 0.12f
        const val AREA_MAXIMA = 1.05f
        const val AREA_COMODA = 0.38f
        const val DESVIO_ANGULO_MAXIMO = 38f
        const val DESVIO_COMODO = 12f

        const val MUESTRAS_POR_LADO = 26
        const val RADIO_BUSQUEDA = 2
    }
}

/**
 * Suaviza la deteccion entre fotogramas y decide cuando esta quieta.
 *
 * Sin esto el contorno baila y el porcentaje parpadea diez veces por segundo,
 * que se lee como que la aplicacion no sabe lo que hace aunque cada fotograma
 * por separado este bien detectado. Hace tres cosas:
 *
 *  - **Empareja las esquinas** con las del fotograma anterior antes de mezclar.
 *    El detector nombra las esquinas por su posicion, y con el papel muy girado
 *    dos contiguas empatan y se intercambian de un fotograma al siguiente;
 *    mezclar entonces la de arriba con la de la izquierda produce un latigazo.
 *    Se prueba el recorrido en sus cuatro giros y se toma el que menos se separa
 *    del anterior.
 *  - **Mezcla con peso adaptativo.** Mientras el papel esta quieto se mezcla
 *    poco, que es lo que da una linea firme; en cuanto la medida se va lejos se
 *    salta a ella, porque eso significa que el telefono se ha movido o que es
 *    otro documento, y ahi arrastrarse despacio se ve como pereza.
 *  - **Mide la quietud**, que es lo que autoriza el disparo automatico. Un
 *    encuadre al noventa por ciento que todavia se mueve da una foto movida.
 */
class SuavizadorDeteccion(
    private val peso: Float = 0.25f,
    private val fotogramasParaOlvidar: Int = 4,
    private val fotogramasParaQuieto: Int = 4,
) {
    private var ultima: DeteccionDocumento = DeteccionDocumento.NADA
    private var sinPapel = 0
    private var quietos = 0

    fun siguiente(medida: DeteccionDocumento): DeteccionDocumento {
        val cuadro = medida.cuadro
        if (cuadro == null) {
            sinPapel++
            quietos = 0
            ultima = if (sinPapel >= fotogramasParaOlvidar) {
                DeteccionDocumento.NADA
            } else {
                ultima.copy(confianza = ultima.confianza * (1f - peso), estable = false)
            }
            return ultima
        }

        sinPapel = 0
        val anterior = ultima.cuadro
        if (anterior == null) {
            quietos = 0
            ultima = medida.copy(estable = false)
            return ultima
        }

        val emparejado = emparejar(anterior, cuadro)
        val movimiento = separacion(anterior, emparejado)

        // Un salto grande no se suaviza: es que ha cambiado la escena.
        if (movimiento > SALTO) {
            quietos = 0
            ultima = medida.copy(estable = false)
            return ultima
        }

        quietos = if (movimiento <= QUIETO) quietos + 1 else 0
        ultima = DeteccionDocumento(
            cuadro = mezclar(anterior, emparejado),
            confianza = ultima.confianza + (medida.confianza - ultima.confianza) * peso,
            estable = quietos >= fotogramasParaQuieto,
        )
        return ultima
    }

    fun reiniciar() {
        ultima = DeteccionDocumento.NADA
        sinPapel = 0
        quietos = 0
    }

    /**
     * El mismo cuadrilatero, empezado por la esquina que mejor encaja con el
     * fotograma anterior.
     *
     * Solo se prueban los cuatro giros del recorrido, no las veinticuatro
     * permutaciones: el recorrido ya viene ordenado sin cruces y lo unico que
     * puede haber cambiado es por donde empieza.
     */
    private fun emparejar(anterior: Cuadrilatero, nuevo: Cuadrilatero): Cuadrilatero {
        val puntos = nuevo.esquinas
        var mejor = nuevo
        var menor = Float.MAX_VALUE
        for (giro in 0 until 4) {
            val candidato = Cuadrilatero(
                puntos[giro],
                puntos[(giro + 1) % 4],
                puntos[(giro + 2) % 4],
                puntos[(giro + 3) % 4],
            )
            val distancia = separacion(anterior, candidato)
            if (distancia < menor) {
                menor = distancia
                mejor = candidato
            }
        }
        return mejor
    }

    /** Cuanto se ha movido de media cada esquina, en fraccion de pantalla. */
    private fun separacion(a: Cuadrilatero, b: Cuadrilatero): Float {
        val unas = a.esquinas
        val otras = b.esquinas
        var suma = 0f
        for (indice in 0 until 4) {
            val dx = unas[indice].x - otras[indice].x
            val dy = unas[indice].y - otras[indice].y
            suma += sqrt(dx * dx + dy * dy)
        }
        return suma / 4f
    }

    private fun mezclar(anterior: Cuadrilatero, nuevo: Cuadrilatero): Cuadrilatero {
        fun entre(a: Punto, b: Punto) = Punto(
            a.x + (b.x - a.x) * peso,
            a.y + (b.y - a.y) * peso,
        )
        return Cuadrilatero(
            supIzq = entre(anterior.supIzq, nuevo.supIzq),
            supDer = entre(anterior.supDer, nuevo.supDer),
            infDer = entre(anterior.infDer, nuevo.infDer),
            infIzq = entre(anterior.infIzq, nuevo.infIzq),
        )
    }

    private companion object {
        /**
         * Movimiento medio por esquina por debajo del cual se considera quieto.
         *
         * Medio punto porcentual de la pantalla: en un movil son cinco pixeles,
         * que es lo que se mueve una mano firme.
         */
        const val QUIETO = 0.006f

        /**
         * Por encima de esto no se suaviza, se salta.
         *
         * Es un cambio de escena, no un temblor: el telefono se ha movido o se
         * ha puesto otro papel delante. Arrastrar el contorno despacio hasta la
         * posicion nueva se ve peor que llevarlo de golpe.
         */
        const val SALTO = 0.10f
    }
}
