package es.ghatostudio.nexapdf.domain.escaner

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Quita de la pagina la sombra de quien la fotografia.
 *
 * Un escaner de sobremesa ilumina el papel por igual. Un telefono no: la mano
 * hace sombra en una esquina, la lampara quema la otra y la ventana deja un
 * degradado por el medio. Con un umbral **global** —el mismo corte para toda la
 * pagina— eso se ve enseguida: la mitad iluminada sale blanca y limpia y la otra
 * se queda gris sucia, con el texto apagado. Es el defecto que delata que un PDF
 * es una foto de un papel y no un escaneo.
 *
 * La solucion clasica es no preguntar "que gris es este pixel" sino "que gris es
 * este pixel **comparado con el papel que tiene alrededor**". Para eso hace
 * falta saber de que color es el papel en cada zona, y eso es lo que estima esta
 * clase:
 *
 *  1. Se divide la pagina en una rejilla gruesa.
 *  2. De cada celda se toma un valor **alto** de luminancia, no la media. La
 *     media de una celda llena de letra es gris; el papel de esa celda sigue
 *     siendo casi blanco, y es el papel lo que se busca.
 *  3. Se suaviza la rejilla y se estira otra vez al tamano de la pagina, para
 *     que no se vean los bordes de las celdas.
 *
 * Dividiendo despues cada pixel por su fondo local, la pagina queda plana y el
 * mismo corte vale en todas partes.
 *
 * Va en `commonMain` y trabaja sobre enteros y no sobre mapas de bits para que
 * se pueda probar sin un dispositivo: es matematica, no dibujo.
 */
object AplanadoDeLuz {

    /**
     * Estima el color del papel en cada punto de la imagen.
     *
     * @param luminancia un valor de 0 a 255 por pixel.
     * @return un array del mismo tamano con el fondo estimado.
     */
    fun estimarFondo(luminancia: IntArray, ancho: Int, alto: Int): IntArray {
        require(luminancia.size >= ancho * alto) { "la luminancia no cubre la imagen" }
        if (ancho <= 0 || alto <= 0) return IntArray(0)

        val celdas = rejillaDeFondo(luminancia, ancho, alto)
        val suavizada = suavizar(celdas, COLUMNAS, FILAS)
        return estirar(suavizada, ancho, alto)
    }

    /**
     * Aplana la pagina y devuelve la luminancia corregida en un array nuevo.
     *
     * Cada pixel se divide por el papel que tiene alrededor y se vuelve a
     * escalar a 255. Lo que era papel queda en blanco venga de la zona
     * iluminada o de la sombra; la tinta conserva su contraste relativo, que es
     * el que el ojo y el reconocedor de texto usan para leer.
     */
    fun aplanar(luminancia: IntArray, ancho: Int, alto: Int): IntArray =
        luminancia.copyOf().also { aplanarEnSitio(it, ancho, alto) }

    /**
     * Lo mismo, pero escribiendo sobre la propia pagina.
     *
     * Una pagina revelada a resolucion nativa son ocho o nueve millones de
     * pixeles, y un `IntArray` de ese tamano son treinta y cinco megabytes. La
     * cadena de mejora encadenaba cinco, mas el mapa de bits: un cuarto de giga
     * en vuelo para revelar una hoja, y sin `largeHeap`. Trabajando sobre el
     * sitio, la cadena entera se queda en dos.
     *
     * Aqui tampoco se materializa el fondo: la rejilla suavizada tiene veinte
     * por veintiseis celdas y se interpola al vuelo, asi que el papel estimado
     * nunca ocupa una pagina entera de memoria.
     */
    fun aplanarEnSitio(luminancia: IntArray, ancho: Int, alto: Int) {
        require(luminancia.size >= ancho * alto) { "la luminancia no cubre la imagen" }
        if (ancho <= 0 || alto <= 0) return

        val celdas = suavizar(rejillaDeFondo(luminancia, ancho, alto), COLUMNAS, FILAS)

        for (y in 0 until alto) {
            val fy = ((y + 0.5f) * FILAS / alto) - 0.5f
            val fila = fy.toInt().coerceIn(0, FILAS - 1)
            val filaSiguiente = min(fila + 1, FILAS - 1)
            val pesoY = (fy - fila).coerceIn(0f, 1f)
            val base = y * ancho

            for (x in 0 until ancho) {
                val fx = ((x + 0.5f) * COLUMNAS / ancho) - 0.5f
                val columna = fx.toInt().coerceIn(0, COLUMNAS - 1)
                val columnaSiguiente = min(columna + 1, COLUMNAS - 1)
                val pesoX = (fx - columna).coerceIn(0f, 1f)

                val arribaIzq = celdas[fila * COLUMNAS + columna]
                val arribaDer = celdas[fila * COLUMNAS + columnaSiguiente]
                val abajoIzq = celdas[filaSiguiente * COLUMNAS + columna]
                val abajoDer = celdas[filaSiguiente * COLUMNAS + columnaSiguiente]

                val arriba = arribaIzq + (arribaDer - arribaIzq) * pesoX
                val abajo = abajoIzq + (abajoDer - abajoIzq) * pesoX
                val papel = max((arriba + (abajo - arriba) * pesoY).roundToInt(), MINIMO_FONDO)

                luminancia[base + x] = ((luminancia[base + x] * 255) / papel).coerceIn(0, 255)
            }
        }
    }

    // --- Pasos ---------------------------------------------------------------

    /**
     * Un valor de papel por celda.
     *
     * Se toma un percentil alto y no el maximo: el maximo lo fija cualquier
     * reflejo o cualquier pixel quemado, y entonces la celda entera se corrige
     * como si su papel fuese mas brillante de lo que es, y el texto se
     * emborrona.
     */
    private fun rejillaDeFondo(luminancia: IntArray, ancho: Int, alto: Int): IntArray {
        val celdas = IntArray(COLUMNAS * FILAS)
        var globalMaximo = 1

        for (fila in 0 until FILAS) {
            val desdeY = fila * alto / FILAS
            val hastaY = max(desdeY + 1, (fila + 1) * alto / FILAS)
            for (columna in 0 until COLUMNAS) {
                val desdeX = columna * ancho / COLUMNAS
                val hastaX = max(desdeX + 1, (columna + 1) * ancho / COLUMNAS)

                val histograma = IntArray(256)
                var total = 0
                var y = desdeY
                while (y < hastaY && y < alto) {
                    val base = y * ancho
                    var x = desdeX
                    while (x < hastaX && x < ancho) {
                        histograma[luminancia[base + x].coerceIn(0, 255)]++
                        total++
                        x += PASO_MUESTREO
                    }
                    y += PASO_MUESTREO
                }

                val valor = percentil(histograma, total, PERCENTIL_PAPEL)
                celdas[fila * COLUMNAS + columna] = valor
                globalMaximo = max(globalMaximo, valor)
            }
        }

        // Ninguna celda puede declarar un papel absurdamente oscuro. Una celda
        // tapada entera por una foto o por un sello daria un fondo casi negro, y
        // al dividir por el, esa zona saldria blanca y se perderia el contenido.
        val suelo = (globalMaximo * SUELO_RELATIVO).roundToInt()
        for (indice in celdas.indices) celdas[indice] = max(celdas[indice], suelo)
        return celdas
    }

    private fun percentil(histograma: IntArray, total: Int, fraccion: Float): Int {
        if (total <= 0) return 255
        val objetivo = max(1, (total * fraccion).roundToInt())
        var acumulado = 0
        for (valor in 0..255) {
            acumulado += histograma[valor]
            if (acumulado >= objetivo) return valor
        }
        return 255
    }

    /**
     * Media de 3x3 sobre la rejilla, con la celda del centro pesando mas.
     *
     * El suavizado esta para que no se vean los bordes de celda. Pero con todas
     * las celdas pesando igual, una pagina con un degradado suave se aplana de
     * menos justo en los extremos: la celda del borde queda arrastrada por sus
     * vecinas, que son mas claras, y la esquina oscura sigue oscura. Dandole al
     * centro el peso de cuatro vecinas se quitan los bordes sin perder la
     * pendiente.
     */
    private fun suavizar(celdas: IntArray, columnas: Int, filas: Int): IntArray {
        val salida = IntArray(celdas.size)
        for (fila in 0 until filas) {
            for (columna in 0 until columnas) {
                var suma = 0
                var pesos = 0
                for (dy in -1..1) {
                    val f = fila + dy
                    if (f !in 0 until filas) continue
                    for (dx in -1..1) {
                        val c = columna + dx
                        if (c !in 0 until columnas) continue
                        val peso = if (dx == 0 && dy == 0) PESO_DEL_CENTRO else 1
                        suma += celdas[f * columnas + c] * peso
                        pesos += peso
                    }
                }
                salida[fila * columnas + columna] = suma / pesos
            }
        }
        return salida
    }

    /** Estira la rejilla al tamano de la imagen, interpolando entre celdas. */
    private fun estirar(celdas: IntArray, ancho: Int, alto: Int): IntArray {
        val salida = IntArray(ancho * alto)
        for (y in 0 until alto) {
            // Posicion en la rejilla, tomando el centro de cada celda.
            val fy = ((y + 0.5f) * FILAS / alto) - 0.5f
            val fila = fy.toInt().coerceIn(0, FILAS - 1)
            val filaSiguiente = min(fila + 1, FILAS - 1)
            val pesoY = (fy - fila).coerceIn(0f, 1f)

            for (x in 0 until ancho) {
                val fx = ((x + 0.5f) * COLUMNAS / ancho) - 0.5f
                val columna = fx.toInt().coerceIn(0, COLUMNAS - 1)
                val columnaSiguiente = min(columna + 1, COLUMNAS - 1)
                val pesoX = (fx - columna).coerceIn(0f, 1f)

                val arribaIzq = celdas[fila * COLUMNAS + columna]
                val arribaDer = celdas[fila * COLUMNAS + columnaSiguiente]
                val abajoIzq = celdas[filaSiguiente * COLUMNAS + columna]
                val abajoDer = celdas[filaSiguiente * COLUMNAS + columnaSiguiente]

                val arriba = arribaIzq + (arribaDer - arribaIzq) * pesoX
                val abajo = abajoIzq + (abajoDer - abajoIzq) * pesoX
                salida[y * ancho + x] = (arriba + (abajo - arriba) * pesoY).roundToInt()
            }
        }
        return salida
    }

    /**
     * Tamano de la rejilla.
     *
     * Cuanto mas gruesa, mas degradado queda **dentro** de cada celda sin
     * corregir: con doce columnas sobre una pagina con sombra fuerte quedaban
     * catorce niveles de diferencia entre un borde y el otro. Con veinte se
     * queda en la mitad. Tampoco conviene pasarse: una celda diminuta puede caer
     * entera dentro de una foto o de un sello, y ahi no hay papel del que sacar
     * el fondo.
     */
    private const val COLUMNAS = 20
    private const val FILAS = 26

    /**
     * Uno de cada dos pixeles al medir cada celda.
     *
     * Una celda de una pagina grande tiene decenas de miles de pixeles y el
     * percentil no cambia por mirar la mitad; el coste, si.
     */
    private const val PASO_MUESTREO = 2

    /** El papel de una celda es su valor mas alto, descontando reflejos. */
    private const val PERCENTIL_PAPEL = 0.92f

    /**
     * Ninguna celda declara un papel por debajo de esta parte del mas claro.
     *
     * Es la red de seguridad para una celda tapada entera por una foto o un
     * sello: sin ella, su fondo saldria casi negro y al dividir por el esa zona
     * se iria a blanco, borrando el contenido. Estaba en 0,45 y era demasiado
     * prudente: una sombra de verdad, la del propio telefono sobre el papel,
     * baja mas de eso, y el suelo impedia corregirla. A 0,28 se corrigen las
     * sombras reales y se sigue protegiendo el caso de la celda tapada.
     */
    private const val SUELO_RELATIVO = 0.28f

    /** Cuanto pesa la propia celda frente a cada vecina al suavizar. */
    private const val PESO_DEL_CENTRO = 4

    /** Division por cero, y por casi cero. */
    private const val MINIMO_FONDO = 24
}
