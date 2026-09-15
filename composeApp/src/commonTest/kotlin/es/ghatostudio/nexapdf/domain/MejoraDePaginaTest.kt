package es.ghatostudio.nexapdf.domain

import es.ghatostudio.nexapdf.domain.escaner.AplanadoDeLuz
import es.ghatostudio.nexapdf.domain.escaner.Deconvolucion
import es.ghatostudio.nexapdf.domain.escaner.TonoDeDocumento
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Las dos correcciones que separan un escaneo de una foto de un papel.
 *
 * Las dos salieron de mirar resultados reales, no de leer el codigo: una pagina
 * con la sombra del propio telefono en una esquina, y un texto que se veia
 * blando aunque el recorte fuese perfecto. Aqui se comprueban sobre escenas
 * fabricadas, que es donde cada defecto se puede aislar y medir.
 */
class MejoraDePaginaTest {

    private val ancho = 300
    private val alto = 400

    // --- Aplanado de luz -----------------------------------------------------

    @Test
    fun `una pagina con sombra lateral queda pareja`() {
        // Papel uniforme visto con bastante mas luz a la derecha que a la
        // izquierda. Es lo que produce cualquier lampara de lado.
        val pagina = IntArray(ancho * alto) { indice ->
            val x = indice % ancho
            90 + (140 * x / ancho)
        }

        val plano = AplanadoDeLuz.aplanar(pagina, ancho, alto)

        val izquierda = mediaDeColumna(plano, 20)
        val derecha = mediaDeColumna(plano, ancho - 20)
        assertTrue(
            abs(izquierda - derecha) < 12,
            "la izquierda queda en $izquierda y la derecha en $derecha: sigue habiendo sombra",
        )
        assertTrue(izquierda > 200, "el papel deberia quedar claro y queda en $izquierda")
    }

    @Test
    fun `la tinta sigue siendo mas oscura que el papel`() {
        // Un papel con degradado y una franja de tinta encima. Aplanar no puede
        // borrar el texto: si lo hiciera, la pagina quedaria en blanco.
        val pagina = IntArray(ancho * alto) { indice ->
            val x = indice % ancho
            val y = indice / ancho
            val papel = 100 + (130 * x / ancho)
            if (y in 180..200) papel / 4 else papel
        }

        val plano = AplanadoDeLuz.aplanar(pagina, ancho, alto)
        val enLaTinta = plano[190 * ancho + ancho / 2]
        val enElPapel = plano[60 * ancho + ancho / 2]

        assertTrue(
            enLaTinta < enElPapel - 60,
            "la tinta ($enLaTinta) tiene que seguir muy por debajo del papel ($enElPapel)",
        )
    }

    @Test
    fun `una pagina ya pareja no se estropea`() {
        val pagina = IntArray(ancho * alto) { 235 }
        val plano = AplanadoDeLuz.aplanar(pagina, ancho, alto)
        assertTrue(
            plano.all { abs(it - 255) < 30 },
            "una pagina bien iluminada no deberia cambiar apenas",
        )
    }

    // --- Deconvolucion -------------------------------------------------------

    /** Una pagina y sus dos apoyos, que es como la usa el filtro. */
    private fun deconvolucionar(
        pagina: IntArray,
        iteraciones: Int = 8,
        haciaElPapel: Float = 0.30f,
    ): IntArray = pagina.copyOf().also {
        Deconvolucion.aplicarEnSitio(
            luminancia = it,
            ancho = ancho,
            alto = alto,
            iteraciones = iteraciones,
            haciaElPapel = haciaElPapel,
            estimacion = FloatArray(ancho * alto),
            apoyo = FloatArray(ancho * alto),
        )
    }

    @Test
    fun `la deconvolucion estrecha un borde emborronado`() {
        // Se fabrica el caso al reves de como se mide: se parte de un borde
        // limpio, se emborrona con una campana como la que la deconvolucion
        // supone, y se comprueba que lo devuelve mas cerca del original. Es la
        // unica forma honesta de probar esto, porque mide si **deshace** el
        // desenfoque y no si exagera los bordes, que es otra cosa.
        val limpia = IntArray(ancho * alto) { if (it % ancho < 150) 30 else 240 }

        val borrosa = IntArray(ancho * alto)
        for (y in 0 until alto) {
            val base = y * ancho
            for (x in 0 until ancho) {
                val izq = limpia[base + (x - 1).coerceAtLeast(0)]
                val der = limpia[base + (x + 1).coerceAtMost(ancho - 1)]
                borrosa[base + x] = (0.239f * izq + 0.522f * limpia[base + x] + 0.239f * der)
                    .toInt()
            }
        }

        val recuperada = deconvolucionar(borrosa, haciaElPapel = 1f)

        val fila = alto / 2
        fun error(pagina: IntArray): Int {
            var suma = 0
            for (x in 145 until 156) suma += abs(pagina[fila * ancho + x] - limpia[fila * ancho + x])
            return suma
        }

        assertTrue(
            error(recuperada) < error(borrosa),
            "el borde borroso tenia error ${error(borrosa)} y despues ${error(recuperada)}",
        )
    }

    @Test
    fun `la deconvolucion no aclara el papel pegado a la tinta`() {
        // El mismo freno que el enfoque asimetrico, y por el mismo motivo:
        // aclarar el lado claro sube los grises flojos y la curva de tono los
        // manda a blanco, o sea que borra el lapiz.
        val pagina = IntArray(ancho * alto) { indice ->
            if (indice % ancho in 140..160) 120 else 210
        }

        val frenada = deconvolucionar(pagina, haciaElPapel = 0.30f)
        val suelta = deconvolucionar(pagina, haciaElPapel = 1f)

        val fila = alto / 2
        val haloFrenado = frenada[fila * ancho + 161] - 210
        val haloSuelto = suelta[fila * ancho + 161] - 210

        assertTrue(
            haloFrenado < haloSuelto,
            "el freno deja halo $haloFrenado y sin freno $haloSuelto",
        )
        // Y la tinta tiene que haberse oscurecido igual con freno y sin el: el
        // freno es solo del lado claro.
        assertTrue(
            frenada[fila * ancho + 150] == suelta[fila * ancho + 150],
            "el freno no debe tocar el lado de la tinta",
        )
    }

    @Test
    fun `sin vueltas la deconvolucion no toca nada`() {
        val pagina = paginaDeMuestra(ancho, alto)
        assertTrue(
            deconvolucionar(pagina, iteraciones = 0).contentEquals(pagina),
            "con cero vueltas la pagina no se toca",
        )
    }

    @Test
    fun `la deconvolucion no ensucia una pagina lisa`() {
        // Papel uniforme con el grano del sensor. Richardson-Lucy amplifica el
        // ruido si se le dan vueltas de mas, y eso en una pagina se ve como
        // suciedad. Aqui se comprueba que con las vueltas que usa el filtro
        // sigue siendo papel.
        val pagina = IntArray(ancho * alto) { indice -> 236 + (indice % 3) - 1 }
        val salida = deconvolucionar(pagina, iteraciones = 10)

        val antes = pagina.max() - pagina.min()
        val despues = salida.max() - salida.min()
        assertTrue(
            despues <= antes + 6,
            "el grano del papel paso de $antes a $despues niveles",
        )
    }

    // --- Curva de tono -------------------------------------------------------

    @Test
    fun `el lapiz flojo no se borra`() {
        // Este es el fallo que paso de verdad: sobre una hoja con anotaciones a
        // lapiz, el filtro dejaba el papel impecable y se llevaba por delante la
        // mitad de lo escrito a mano. El filtro no esta para borrar contenido.
        val paginaAplanada = intArrayOf(
            252, 250, 248, // papel
            215, 208, 222, // lapiz flojo
            40, 25, 60, // texto impreso
        )
        val tono = TonoDeDocumento.aplicar(paginaAplanada, 0.5f)

        assertTrue(
            tono.take(3).all { it == 255 },
            "el papel tiene que quedar blanco y quedo en ${tono.take(3)}",
        )
        assertTrue(
            tono.slice(3..5).all { it in 1..254 },
            "el lapiz tiene que seguir viendose y quedo en ${tono.slice(3..5)}",
        )
        assertTrue(
            tono.slice(6..8).all { it < 60 },
            "el texto impreso tiene que quedar muy oscuro y quedo en ${tono.slice(6..8)}",
        )
    }

    @Test
    fun `mas intensidad limpia mas y menos respeta mas`() {
        val lapizMuyFlojo = intArrayOf(228)

        val suave = TonoDeDocumento.aplicar(lapizMuyFlojo, 0f).first()
        val firme = TonoDeDocumento.aplicar(lapizMuyFlojo, 1f).first()

        assertTrue(suave in 1..254, "con intensidad minima el gris claro debe sobrevivir: $suave")
        assertTrue(
            firme >= suave,
            "mas intensidad tiene que aclarar mas el fondo, no menos ($suave -> $firme)",
        )
    }

    // --- Trabajar sobre el sitio ---------------------------------------------

    /**
     * Una pagina de prueba con de todo: degradado de luz, lineas de texto,
     * una mancha oscura y grano. Sirve para comparar las dos versiones de cada
     * paso sobre algo que ejercite todas sus ramas.
     */
    private fun paginaDeMuestra(ancho: Int, alto: Int) = IntArray(ancho * alto) { indice ->
        val x = indice % ancho
        val y = indice / ancho
        val papel = 120 + (110 * x / ancho)
        val grano = (indice * 7) % 5 - 2
        when {
            y in 100..130 && x in 20..(ancho - 20) -> 35 + grano
            y % 23 < 3 -> papel / 3 + grano
            x in 200..240 && y in 250..300 -> 60 + grano
            else -> papel + grano
        }.coerceIn(0, 255)
    }

    @Test
    fun `aplanar sobre el sitio da exactamente lo mismo que copiando`() {
        val pagina = paginaDeMuestra(ancho, alto)

        val copiando = AplanadoDeLuz.aplanar(pagina, ancho, alto)
        val enSitio = pagina.copyOf().also { AplanadoDeLuz.aplanarEnSitio(it, ancho, alto) }

        assertTrue(copiando.contentEquals(enSitio), "las dos versiones de aplanar no coinciden")
        assertTrue(
            pagina.contentEquals(paginaDeMuestra(ancho, alto)),
            "la version que copia no puede tocar la pagina de entrada",
        )
    }

    @Test
    fun `la curva de tono sobre el sitio da exactamente lo mismo`() {
        val pagina = paginaDeMuestra(ancho, alto)

        val copiando = TonoDeDocumento.aplicar(pagina, 0.5f)
        val enSitio = pagina.copyOf().also { TonoDeDocumento.aplicarEnSitio(it, 0.5f) }

        assertTrue(copiando.contentEquals(enSitio), "las dos versiones de la curva no coinciden")
    }

    // --- Coste ---------------------------------------------------------------

    @Test
    fun `mejorar una pagina entera cuesta lo que puede costar`() {
        // Una pagina A4 revelada a 300 ppp son casi nueve millones de pixeles, y
        // esto corre cada vez que se revela una hoja y cada vez que se mueve el
        // deslizador de intensidad. Si costara segundos, la revision del escaneo
        // seria inusable aunque el resultado fuese bonito.
        //
        // Se mide con la cadena que usa la aplicacion, la que trabaja sobre el
        // sitio: es la que importa y ademas es la barata, porque no reserva una
        // pagina nueva en cada paso.
        val anchoPagina = 2480
        val altoPagina = 3500
        val pagina = IntArray(anchoPagina * altoPagina) { indice ->
            val x = indice % anchoPagina
            val y = indice / anchoPagina
            val papel = 150 + (80 * x / anchoPagina)
            if (y % 40 < 6) papel / 3 else papel
        }
        val estimacion = FloatArray(anchoPagina * altoPagina)
        val apoyo = FloatArray(anchoPagina * altoPagina)

        fun cadena(destino: IntArray) {
            AplanadoDeLuz.aplanarEnSitio(destino, anchoPagina, altoPagina)
            Deconvolucion.aplicarEnSitio(
                luminancia = destino,
                ancho = anchoPagina,
                alto = altoPagina,
                iteraciones = 7,
                haciaElPapel = 0.30f,
                estimacion = estimacion,
                apoyo = apoyo,
            )
            TonoDeDocumento.aplicarEnSitio(destino, 0.5f)
        }

        // Una pasada de calentamiento, para no medir la compilacion JIT.
        cadena(pagina.copyOf())

        val inicio = kotlin.time.TimeSource.Monotonic.markNow()
        cadena(pagina)
        val tardado = inicio.elapsedNow()

        // La deconvolucion es catorce pasadas sobre la pagina entera, asi que
        // cuesta bastante mas que la mascara de desenfoque que habia antes. El
        // tope va holgado porque esto corre en la integracion continua y no en
        // el telefono; sirve para cazar un cambio que lo multiplique por cinco,
        // no para medir el movil.
        assertTrue(
            tardado.inWholeMilliseconds < 6000,
            "mejorar una pagina costo $tardado",
        )
    }

    // --- Utilidades ----------------------------------------------------------

    private fun mediaDeColumna(pagina: IntArray, columna: Int): Int {
        var suma = 0
        for (y in 0 until alto) suma += pagina[y * ancho + columna]
        return suma / alto
    }

    /** El mayor salto de tono entre dos columnas contiguas, en mitad de la pagina. */
    private fun saltoMaximo(pagina: IntArray, desde: Int, hasta: Int): Int {
        val fila = alto / 2
        var mayor = 0
        for (x in desde until hasta) {
            val salto = abs(pagina[fila * ancho + x + 1] - pagina[fila * ancho + x])
            if (salto > mayor) mayor = salto
        }
        return mayor
    }
}
