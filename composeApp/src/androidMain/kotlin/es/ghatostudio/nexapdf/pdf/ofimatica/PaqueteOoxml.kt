package es.ghatostudio.nexapdf.pdf.ofimatica

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Lectura y escritura de paquetes OOXML.
 *
 * Un .docx, un .xlsx y un .pptx son lo mismo por dentro: un ZIP con ficheros
 * XML y una tabla de tipos de contenido. Trabajar con ellos no necesita ninguna
 * biblioteca de ofimatica, solo saber que partes leer y que partes escribir.
 */
object PaqueteOoxml {

    /** Lee una entrada del paquete como texto, o `null` si no existe. */
    fun leerTexto(fichero: File, ruta: String): String? = runCatching {
        ZipFile(fichero).use { zip ->
            zip.getEntry(ruta)?.let { entrada ->
                zip.getInputStream(entrada).use { it.readBytes().toString(Charsets.UTF_8) }
            }
        }
    }.getOrNull()

    /** Lee una entrada como bytes. */
    fun leerBytes(fichero: File, ruta: String): ByteArray? = runCatching {
        ZipFile(fichero).use { zip ->
            zip.getEntry(ruta)?.let { entrada -> zip.getInputStream(entrada).use { it.readBytes() } }
        }
    }.getOrNull()

    /** Nombres de las entradas que cuelgan de un directorio del paquete. */
    fun listar(fichero: File, prefijo: String): List<String> = runCatching {
        ZipFile(fichero).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { it.startsWith(prefijo) && !it.endsWith("/") }
                .sortedWith(ComparadorNatural)
                .toList()
        }
    }.getOrDefault(emptyList())

    /**
     * Ordena "slide2.xml" antes que "slide10.xml".
     *
     * El orden alfabetico dejaria la diapositiva 10 entre la 1 y la 2, y la
     * presentacion saldria descolocada.
     */
    private object ComparadorNatural : Comparator<String> {
        private val numeros = Regex("[0-9]+")

        override fun compare(a: String, b: String): Int {
            val na = numeros.find(a)?.value?.toIntOrNull()
            val nb = numeros.find(b)?.value?.toIntOrNull()
            return when {
                na != null && nb != null && na != nb -> na.compareTo(nb)
                else -> a.compareTo(b)
            }
        }
    }

    /** Escribe un paquete ZIP con las entradas dadas. */
    fun escribir(destino: File, entradas: List<Entrada>) {
        destino.parentFile?.mkdirs()
        ZipOutputStream(destino.outputStream().buffered()).use { zip ->
            entradas.forEach { entrada ->
                zip.putNextEntry(ZipEntry(entrada.ruta))
                zip.write(entrada.contenido)
                zip.closeEntry()
            }
        }
    }

    class Entrada(val ruta: String, val contenido: ByteArray) {
        constructor(ruta: String, xml: String) : this(ruta, xml.toByteArray(Charsets.UTF_8))
    }

    const val CABECERA_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""

    private const val AMPERSAND = 0x26
    private const val MENOR_QUE = 0x3C
    private const val MAYOR_QUE = 0x3E
    private const val COMILLA_DOBLE = 0x22
    private const val COMILLA_SIMPLE = 0x27
    private const val TABULADOR = 0x09
    private const val SALTO_LINEA = 0x0A
    private const val RETORNO_CARRO = 0x0D
    private const val PRIMER_IMPRIMIBLE = 0x20

    /**
     * Escapa lo que no puede ir suelto dentro de un nodo XML.
     *
     * Se compara por punto de codigo en vez de por caracter literal para que el
     * significado quede explicito y no dependa de como se lea el fichero fuente.
     */
    fun escapar(texto: String): String = buildString(texto.length + 16) {
        texto.forEach { caracter ->
            when (val codigo = caracter.code) {
                AMPERSAND -> append("&amp;")
                MENOR_QUE -> append("&lt;")
                MAYOR_QUE -> append("&gt;")
                COMILLA_DOBLE -> append("&quot;")
                COMILLA_SIMPLE -> append("&apos;")
                else ->
                    // XML 1.0 solo admite tabulador, salto de linea y retorno
                    // entre los caracteres de control. Cualquier otro invalida
                    // el documento entero, asi que se sustituye por un espacio.
                    if (codigo < PRIMER_IMPRIMIBLE &&
                        codigo != TABULADOR &&
                        codigo != SALTO_LINEA &&
                        codigo != RETORNO_CARRO
                    ) {
                        append(' ')
                    } else {
                        append(caracter)
                    }
            }
        }
    }

    /** Deshace el escapado al leer. */
    fun desescapar(texto: String): String = texto
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}

/**
 * Recorrido minimo de XML por expresiones regulares.
 *
 * Los XML de OOXML que hacen falta aqui son planos y predecibles: interesan las
 * etiquetas de parrafo, de fila y de texto, no el arbol completo. Un analizador
 * DOM cargaria en memoria un documento que puede pesar decenas de megas para
 * acabar leyendo lo mismo.
 */
object XmlPlano {

    /** Contenido de todas las apariciones de una etiqueta, en orden. */
    fun bloques(xml: String, etiqueta: String): List<String> {
        val patron = Regex(
            "<$etiqueta(?:\\s[^>]*)?>(.*?)</$etiqueta>",
            RegexOption.DOT_MATCHES_ALL,
        )
        return patron.findAll(xml).map { it.groupValues[1] }.toList()
    }

    /** Igual que [bloques] pero devolviendo tambien los atributos de apertura. */
    fun bloquesConAtributos(xml: String, etiqueta: String): List<Pair<String, String>> {
        val patron = Regex(
            "<$etiqueta((?:\\s[^>]*)?)>(.*?)</$etiqueta>",
            RegexOption.DOT_MATCHES_ALL,
        )
        return patron.findAll(xml).map { it.groupValues[1] to it.groupValues[2] }.toList()
    }

    /** Texto plano de todas las etiquetas indicadas dentro de un fragmento. */
    fun textoDe(fragmento: String, etiqueta: String): String =
        bloques(fragmento, etiqueta).joinToString("") { PaqueteOoxml.desescapar(it) }

    /**
     * Atributos de cada aparicion de una etiqueta, se cierre o no.
     *
     * Hace falta porque las etiquetas de OOXML que llevan la informacion util en
     * los atributos (`a:off`, `a:ext`, `a:rPr`) se escriben cerradas sobre si
     * mismas, y [bloquesConAtributos] solo encuentra las que tienen contenido.
     */
    fun atributosDe(xml: String, etiqueta: String): List<String> =
        Regex("<$etiqueta\\s([^>]*?)/?>").findAll(xml).map { it.groupValues[1] }.toList()

    /** Valor de un atributo dentro de una cadena de atributos. */
    fun atributo(atributos: String, nombre: String): String? =
        Regex("$nombre=\"([^\"]*)\"").find(atributos)?.groupValues?.get(1)

    /** Si el fragmento contiene una etiqueta, suelta o con contenido. */
    fun contiene(fragmento: String, etiqueta: String): Boolean =
        Regex("<$etiqueta(\\s[^>]*)?/?>").containsMatchIn(fragmento)
}
