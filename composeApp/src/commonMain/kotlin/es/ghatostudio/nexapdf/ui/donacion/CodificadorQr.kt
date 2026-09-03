package es.ghatostudio.nexapdf.ui.donacion

/**
 * Generador de codigos QR en Kotlin puro, sin dependencias ni red.
 *
 * NexaPDF necesita pintar un codigo QR con el enlace de donacion, y hacerlo con
 * una biblioteca supondria o bien una dependencia solo de Android (ZXing), que
 * rompe la regla de no meter nada de plataforma en el codigo compartido, o bien
 * una multiplataforma poco mantenida. El algoritmo esta completamente
 * especificado en la norma ISO/IEC 18004 y cabe en un fichero.
 *
 * Cubre modo byte (UTF-8) y versiones 1 a 10, de sobra para una URL: la version
 * 10 con correccion media admite 213 bytes.
 */
object CodificadorQr {

    enum class NivelCorreccion(val indicador: Int) {
        BAJO(0b01),
        MEDIO(0b00),
        ALTO(0b11),
        MAXIMO(0b10),
    }

    /** Matriz de modulos. `true` es un modulo oscuro. */
    class MatrizQr(val tamano: Int, private val modulos: BooleanArray) {
        operator fun get(x: Int, y: Int): Boolean =
            if (x in 0 until tamano && y in 0 until tamano) modulos[y * tamano + x] else false
    }

    class TextoDemasiadoLargo(mensaje: String) : IllegalArgumentException(mensaje)

    fun generar(texto: String, nivel: NivelCorreccion = NivelCorreccion.MEDIO): MatrizQr =
        generar(texto, nivel, mascaraForzada = null)

    /**
     * Variante con la mascara fijada. Solo la usan las pruebas, para poder
     * separar un fallo de codificacion de uno de eleccion de mascara.
     */
    internal fun generar(
        texto: String,
        nivel: NivelCorreccion,
        mascaraForzada: Int?,
    ): MatrizQr {
        val datos = texto.encodeToByteArray()
        val version = elegirVersion(datos.size, nivel)
            ?: throw TextoDemasiadoLargo("El texto no cabe en un QR de version 10")

        val codewords = construirCodewords(datos, version, nivel)
        val bloques = repartirEnBloques(codewords, version, nivel)

        val tamano = 17 + 4 * version
        val modulos = BooleanArray(tamano * tamano)
        val reservado = BooleanArray(tamano * tamano)

        dibujarPatronesFijos(modulos, reservado, tamano, version)
        colocarDatos(modulos, reservado, tamano, bloques)

        val mascara = mascaraForzada ?: elegirMascara(modulos, reservado, tamano, nivel)
        aplicarMascara(modulos, reservado, tamano, mascara)
        escribirFormato(modulos, tamano, nivel, mascara)
        if (version >= 7) escribirVersion(modulos, tamano, version)

        return MatrizQr(tamano, modulos)
    }

    // --- Capacidad y version -------------------------------------------------

    /** Codewords totales de cada version (1..10). */
    private val TOTAL_CODEWORDS = intArrayOf(26, 44, 70, 100, 134, 172, 196, 242, 292, 346)

    /**
     * Para cada version, y para cada nivel en orden BAJO, MEDIO, ALTO, MAXIMO:
     * codewords de correccion por bloque y numero de bloques.
     * Valores de las tablas 13 a 22 de la norma.
     */
    private val CORRECCION = arrayOf(
        //          BAJO       MEDIO      ALTO       MAXIMO
        intArrayOf(7, 1, 10, 1, 13, 1, 17, 1),
        intArrayOf(10, 1, 16, 1, 22, 1, 28, 1),
        intArrayOf(15, 1, 26, 1, 18, 2, 22, 2),
        intArrayOf(20, 1, 18, 2, 26, 2, 16, 4),
        intArrayOf(26, 1, 24, 2, 18, 4, 22, 4),
        intArrayOf(18, 2, 16, 4, 24, 4, 28, 4),
        intArrayOf(20, 2, 18, 4, 18, 6, 26, 5),
        intArrayOf(24, 2, 22, 4, 22, 6, 26, 6),
        intArrayOf(30, 2, 22, 5, 20, 8, 24, 8),
        intArrayOf(18, 4, 26, 5, 24, 8, 28, 8),
    )

    private val POSICIONES_ALINEACION = arrayOf(
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50),
    )

    private fun ordenNivel(nivel: NivelCorreccion): Int = when (nivel) {
        NivelCorreccion.BAJO -> 0
        NivelCorreccion.MEDIO -> 1
        NivelCorreccion.ALTO -> 2
        NivelCorreccion.MAXIMO -> 3
    }

    private fun eccPorBloque(version: Int, nivel: NivelCorreccion): Int =
        CORRECCION[version - 1][ordenNivel(nivel) * 2]

    private fun numeroBloques(version: Int, nivel: NivelCorreccion): Int =
        CORRECCION[version - 1][ordenNivel(nivel) * 2 + 1]

    private fun codewordsDeDatos(version: Int, nivel: NivelCorreccion): Int =
        TOTAL_CODEWORDS[version - 1] - eccPorBloque(version, nivel) * numeroBloques(version, nivel)

    /** Bits que ocupa el contador de caracteres en modo byte. */
    private fun bitsContador(version: Int): Int = if (version <= 9) 8 else 16

    private fun elegirVersion(bytes: Int, nivel: NivelCorreccion): Int? {
        for (version in 1..10) {
            val disponibles = codewordsDeDatos(version, nivel) * 8
            val necesarios = 4 + bitsContador(version) + bytes * 8
            if (necesarios <= disponibles) return version
        }
        return null
    }

    // --- Codificacion de datos ----------------------------------------------

    private class Bits {
        val valores = ArrayList<Boolean>()
        fun anadir(valor: Int, cuantos: Int) {
            for (posicion in cuantos - 1 downTo 0) {
                valores += ((valor ushr posicion) and 1) == 1
            }
        }
    }

    private fun construirCodewords(
        datos: ByteArray,
        version: Int,
        nivel: NivelCorreccion,
    ): IntArray {
        val capacidad = codewordsDeDatos(version, nivel)
        val bits = Bits()
        bits.anadir(0b0100, 4) // modo byte
        bits.anadir(datos.size, bitsContador(version))
        datos.forEach { bits.anadir(it.toInt() and 0xFF, 8) }

        // Terminador de hasta cuatro ceros y relleno hasta completar el byte.
        val maximoBits = capacidad * 8
        repeat(minOf(4, maximoBits - bits.valores.size)) { bits.valores += false }
        while (bits.valores.size % 8 != 0) bits.valores += false

        val codewords = IntArray(capacidad)
        for (indice in 0 until bits.valores.size / 8) {
            var valor = 0
            for (bit in 0 until 8) {
                valor = (valor shl 1) or if (bits.valores[indice * 8 + bit]) 1 else 0
            }
            codewords[indice] = valor
        }

        // Bytes de relleno alternos definidos por la norma.
        var posicion = bits.valores.size / 8
        var alterna = true
        while (posicion < capacidad) {
            codewords[posicion++] = if (alterna) 0xEC else 0x11
            alterna = !alterna
        }
        return codewords
    }

    /** Reparte en bloques, calcula su correccion y los entrelaza. */
    private fun repartirEnBloques(
        codewords: IntArray,
        version: Int,
        nivel: NivelCorreccion,
    ): IntArray {
        val bloques = numeroBloques(version, nivel)
        val eccPorBloque = eccPorBloque(version, nivel)
        val datosTotales = codewords.size

        val cortos = bloques - (datosTotales % bloques)
        val tamanoCorto = datosTotales / bloques

        val datosPorBloque = ArrayList<IntArray>(bloques)
        val eccPorBloqueLista = ArrayList<IntArray>(bloques)
        var desplazamiento = 0

        for (indice in 0 until bloques) {
            val tamano = if (indice < cortos) tamanoCorto else tamanoCorto + 1
            val bloque = codewords.copyOfRange(desplazamiento, desplazamiento + tamano)
            desplazamiento += tamano
            datosPorBloque += bloque
            eccPorBloqueLista += ReedSolomon.calcular(bloque, eccPorBloque)
        }

        val resultado = ArrayList<Int>(TOTAL_CODEWORDS[version - 1])
        val maximoDatos = datosPorBloque.maxOf { it.size }
        for (columna in 0 until maximoDatos) {
            datosPorBloque.forEach { bloque ->
                if (columna < bloque.size) resultado += bloque[columna]
            }
        }
        for (columna in 0 until eccPorBloque) {
            eccPorBloqueLista.forEach { bloque -> resultado += bloque[columna] }
        }
        return resultado.toIntArray()
    }

    // --- Reed-Solomon sobre GF(256) -----------------------------------------

    private object ReedSolomon {
        private val exp = IntArray(512)
        private val log = IntArray(256)

        init {
            var valor = 1
            for (indice in 0 until 255) {
                exp[indice] = valor
                log[valor] = indice
                valor = valor shl 1
                // Polinomio primitivo x^8 + x^4 + x^3 + x^2 + 1
                if (valor and 0x100 != 0) valor = valor xor 0x11D
            }
            for (indice in 255 until 512) exp[indice] = exp[indice - 255]
        }

        private fun multiplicar(a: Int, b: Int): Int =
            if (a == 0 || b == 0) 0 else exp[log[a] + log[b]]

        /** Polinomio generador de grado [grado]. */
        private fun generador(grado: Int): IntArray {
            var polinomio = intArrayOf(1)
            for (indice in 0 until grado) {
                val siguiente = IntArray(polinomio.size + 1)
                for (posicion in polinomio.indices) {
                    siguiente[posicion] = siguiente[posicion] xor polinomio[posicion]
                    siguiente[posicion + 1] = siguiente[posicion + 1] xor
                        multiplicar(polinomio[posicion], exp[indice])
                }
                polinomio = siguiente
            }
            return polinomio
        }

        fun calcular(datos: IntArray, cuantos: Int): IntArray {
            val generador = generador(cuantos)
            val resto = IntArray(cuantos)
            datos.forEach { byte ->
                val factor = byte xor resto[0]
                for (indice in 0 until cuantos - 1) resto[indice] = resto[indice + 1]
                resto[cuantos - 1] = 0
                for (indice in 0 until cuantos) {
                    resto[indice] = resto[indice] xor multiplicar(generador[indice + 1], factor)
                }
            }
            return resto
        }
    }

    // --- Patrones fijos ------------------------------------------------------

    private fun dibujarPatronesFijos(
        modulos: BooleanArray,
        reservado: BooleanArray,
        tamano: Int,
        version: Int,
    ) {
        fun poner(x: Int, y: Int, oscuro: Boolean) {
            if (x in 0 until tamano && y in 0 until tamano) {
                modulos[y * tamano + x] = oscuro
                reservado[y * tamano + x] = true
            }
        }

        fun buscador(origenX: Int, origenY: Int) {
            for (dy in -1..7) {
                for (dx in -1..7) {
                    val x = origenX + dx
                    val y = origenY + dy
                    if (x !in 0 until tamano || y !in 0 until tamano) continue
                    val dentro = dx in 0..6 && dy in 0..6
                    val borde = dentro && (dx == 0 || dx == 6 || dy == 0 || dy == 6)
                    val centro = dx in 2..4 && dy in 2..4
                    poner(x, y, borde || centro)
                }
            }
        }

        buscador(0, 0)
        buscador(tamano - 7, 0)
        buscador(0, tamano - 7)

        // Patrones de sincronizacion en la fila y la columna 6.
        for (posicion in 8 until tamano - 8) {
            val oscuro = posicion % 2 == 0
            poner(posicion, 6, oscuro)
            poner(6, posicion, oscuro)
        }

        // Patrones de alineacion, salvo los que caerian sobre un buscador.
        val posiciones = POSICIONES_ALINEACION[version - 1]
        posiciones.forEach { centroY ->
            posiciones.forEach { centroX ->
                val enBuscador =
                    (centroX <= 8 && centroY <= 8) ||
                        (centroX >= tamano - 9 && centroY <= 8) ||
                        (centroX <= 8 && centroY >= tamano - 9)
                if (enBuscador) return@forEach
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val anillo = kotlin.math.max(kotlin.math.abs(dx), kotlin.math.abs(dy))
                        poner(centroX + dx, centroY + dy, anillo != 1)
                    }
                }
            }
        }

        // Modulo siempre oscuro.
        poner(8, tamano - 8, true)

        // Se reservan las casillas de la informacion de formato.
        for (indice in 0..8) {
            if (indice != 6) {
                reservado[6 * 0 + indice + 8 * tamano] = true
                reservado[indice * tamano + 8] = true
            }
        }
        for (indice in 0..7) reservado[8 * tamano + (tamano - 1 - indice)] = true
        for (indice in 0..6) reservado[(tamano - 1 - indice) * tamano + 8] = true

        if (version >= 7) {
            for (indice in 0 until 18) {
                val fila = indice / 3
                val columna = indice % 3
                reservado[(tamano - 11 + columna) * tamano + fila] = true
                reservado[fila * tamano + (tamano - 11 + columna)] = true
            }
        }
    }

    // --- Colocacion de datos -------------------------------------------------

    private fun colocarDatos(
        modulos: BooleanArray,
        reservado: BooleanArray,
        tamano: Int,
        codewords: IntArray,
    ) {
        var bitActual = 0
        val totalBits = codewords.size * 8

        fun siguienteBit(): Boolean {
            if (bitActual >= totalBits) return false
            val byte = codewords[bitActual / 8]
            val bit = ((byte ushr (7 - bitActual % 8)) and 1) == 1
            bitActual++
            return bit
        }

        var columna = tamano - 1
        var haciaArriba = true
        while (columna > 0) {
            if (columna == 6) columna-- // la columna 6 es de sincronizacion
            for (paso in 0 until tamano) {
                val fila = if (haciaArriba) tamano - 1 - paso else paso
                for (desplazamiento in 0..1) {
                    val x = columna - desplazamiento
                    val indice = fila * tamano + x
                    if (!reservado[indice]) modulos[indice] = siguienteBit()
                }
            }
            haciaArriba = !haciaArriba
            columna -= 2
        }
    }

    // --- Mascaras ------------------------------------------------------------

    private fun condicionMascara(patron: Int, x: Int, y: Int): Boolean = when (patron) {
        0 -> (x + y) % 2 == 0
        1 -> y % 2 == 0
        2 -> x % 3 == 0
        3 -> (x + y) % 3 == 0
        4 -> ((y / 2) + (x / 3)) % 2 == 0
        5 -> (x * y) % 2 + (x * y) % 3 == 0
        6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
        else -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
    }

    private fun aplicarMascara(
        modulos: BooleanArray,
        reservado: BooleanArray,
        tamano: Int,
        patron: Int,
    ) {
        for (y in 0 until tamano) {
            for (x in 0 until tamano) {
                val indice = y * tamano + x
                if (!reservado[indice] && condicionMascara(patron, x, y)) {
                    modulos[indice] = !modulos[indice]
                }
            }
        }
    }

    private fun elegirMascara(
        modulos: BooleanArray,
        reservado: BooleanArray,
        tamano: Int,
        nivel: NivelCorreccion,
    ): Int {
        var mejorPatron = 0
        var mejorPenalizacion = Int.MAX_VALUE

        for (patron in 0..7) {
            val copia = modulos.copyOf()
            aplicarMascara(copia, reservado, tamano, patron)
            escribirFormato(copia, tamano, nivel, patron)
            val penalizacion = penalizar(copia, tamano)
            if (penalizacion < mejorPenalizacion) {
                mejorPenalizacion = penalizacion
                mejorPatron = patron
            }
        }
        return mejorPatron
    }

    /** Las cuatro reglas de penalizacion de la norma. */
    private fun penalizar(modulos: BooleanArray, tamano: Int): Int {
        fun oscuro(x: Int, y: Int) = modulos[y * tamano + x]
        var total = 0

        // Regla 1: rachas de cinco o mas del mismo color.
        for (linea in 0 until tamano) {
            var rachaFila = 1
            var rachaColumna = 1
            for (posicion in 1 until tamano) {
                if (oscuro(posicion, linea) == oscuro(posicion - 1, linea)) rachaFila++
                else { if (rachaFila >= 5) total += rachaFila - 2; rachaFila = 1 }
                if (oscuro(linea, posicion) == oscuro(linea, posicion - 1)) rachaColumna++
                else { if (rachaColumna >= 5) total += rachaColumna - 2; rachaColumna = 1 }
            }
            if (rachaFila >= 5) total += rachaFila - 2
            if (rachaColumna >= 5) total += rachaColumna - 2
        }

        // Regla 2: bloques de 2x2 del mismo color.
        for (y in 0 until tamano - 1) {
            for (x in 0 until tamano - 1) {
                val valor = oscuro(x, y)
                if (valor == oscuro(x + 1, y) &&
                    valor == oscuro(x, y + 1) &&
                    valor == oscuro(x + 1, y + 1)
                ) {
                    total += 3
                }
            }
        }

        // Regla 3: secuencias que un lector podria confundir con un buscador.
        // Son los once modulos 1:1:3:1:1 con cuatro claros a un lado u otro.
        val patronA = booleanArrayOf(
            true, false, true, true, true, false, true, false, false, false, false,
        )
        val patronB = booleanArrayOf(
            false, false, false, false, true, false, true, true, true, false, true,
        )
        for (y in 0 until tamano) {
            for (x in 0..tamano - 11) {
                if (encaja(modulos, tamano, x, y, patronA, horizontal = true) ||
                    encaja(modulos, tamano, x, y, patronB, horizontal = true)
                ) {
                    total += 40
                }
            }
        }
        for (x in 0 until tamano) {
            for (y in 0..tamano - 11) {
                if (encaja(modulos, tamano, x, y, patronA, horizontal = false) ||
                    encaja(modulos, tamano, x, y, patronB, horizontal = false)
                ) {
                    total += 40
                }
            }
        }

        // Regla 4: desequilibrio entre modulos claros y oscuros.
        val oscuros = modulos.count { it }
        val porcentaje = oscuros * 100 / (tamano * tamano)
        val desviacion = kotlin.math.abs(porcentaje - 50) / 5
        total += desviacion * 10

        return total
    }

    private fun encaja(
        modulos: BooleanArray,
        tamano: Int,
        x: Int,
        y: Int,
        patron: BooleanArray,
        horizontal: Boolean,
    ): Boolean {
        for (indice in patron.indices) {
            val posX = if (horizontal) x + indice else x
            val posY = if (horizontal) y else y + indice
            if (modulos[posY * tamano + posX] != patron[indice]) return false
        }
        return true
    }

    // --- Informacion de formato y version ------------------------------------

    private fun escribirFormato(
        modulos: BooleanArray,
        tamano: Int,
        nivel: NivelCorreccion,
        mascara: Int,
    ) {
        val datos = (nivel.indicador shl 3) or mascara
        var resto = datos shl 10
        // BCH(15,5) con el polinomio generador 0b10100110111
        while (bitsSignificativos(resto) >= 11) {
            resto = resto xor (0b10100110111 shl (bitsSignificativos(resto) - 11))
        }
        val formato = ((datos shl 10) or resto) xor 0b101010000010010

        for (indice in 0..14) {
            val bit = ((formato ushr indice) and 1) == 1

            // Copia junto al buscador superior izquierdo.
            val (x1, y1) = when {
                indice < 6 -> 8 to indice
                indice == 6 -> 8 to 7
                indice == 7 -> 8 to 8
                indice == 8 -> 7 to 8
                else -> (14 - indice) to 8
            }
            modulos[y1 * tamano + x1] = bit

            // Copia repartida entre los otros dos buscadores.
            val (x2, y2) = if (indice < 8) {
                (tamano - 1 - indice) to 8
            } else {
                8 to (tamano - 15 + indice)
            }
            modulos[y2 * tamano + x2] = bit
        }
    }

    private fun escribirVersion(modulos: BooleanArray, tamano: Int, version: Int) {
        var resto = version shl 12
        // BCH(18,6) con el polinomio generador 0b1111100100101
        while (bitsSignificativos(resto) >= 13) {
            resto = resto xor (0b1111100100101 shl (bitsSignificativos(resto) - 13))
        }
        val informacion = (version shl 12) or resto

        for (indice in 0 until 18) {
            val bit = ((informacion ushr indice) and 1) == 1
            val fila = indice / 3
            val columna = indice % 3
            modulos[(tamano - 11 + columna) * tamano + fila] = bit
            modulos[fila * tamano + (tamano - 11 + columna)] = bit
        }
    }

    private fun bitsSignificativos(valor: Int): Int {
        var restante = valor
        var cuantos = 0
        while (restante != 0) {
            cuantos++
            restante = restante ushr 1
        }
        return cuantos
    }
}
