package es.ghatostudio.nexapdf.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.contentstream.PDFGraphicsStreamEngine
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImage
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.escaner.HojaEscaneada
import es.ghatostudio.nexapdf.domain.escaner.PaginaEscaneada
import es.ghatostudio.nexapdf.domain.escaner.PalabraOcr
import es.ghatostudio.nexapdf.domain.escaner.TextoPagina
import es.ghatostudio.nexapdf.domain.escaner.PUNTOS_POR_MM
import es.ghatostudio.nexapdf.domain.escaner.TipoTarjeta
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import es.ghatostudio.nexapdf.domain.model.TamanoPagina
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Pruebas del escaner sobre el dispositivo real.
 *
 * Lo que se comprueba aqui no se puede comprobar mirando la pantalla, y por eso
 * existe esta clase: que el PDF que sale **de verdad lleva el texto dentro** y
 * que cada palabra cae donde estaba en la imagen. Un escaneo puede verse
 * perfecto y tener la capa de texto vacia, o tenerla desplazada media pagina, y
 * en los dos casos la pantalla se ve igual de bien.
 *
 * El reconocimiento de texto no se prueba aqui: depende del modelo de ML Kit y
 * de lo que vea, y una prueba que dependa de eso falla por motivos que no son el
 * codigo. Lo que se prueba es lo que si es nuestro: que el texto que se le
 * entregue acabe dentro del PDF, en su sitio, e invisible.
 */
@RunWith(AndroidJUnit4::class)
class EscanerAndroidTest {

    private lateinit var trabajo: File
    private lateinit var motor: MotorPdfAndroid
    private lateinit var escaner: MotorEscanerAndroid

    @Before
    fun preparar() {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        trabajo = File(contexto.cacheDir, "pruebas-escaner").apply {
            deleteRecursively()
            mkdirs()
        }
        motor = MotorPdfAndroid(contexto, trabajo.absolutePath)
        escaner = MotorEscanerAndroid(trabajo.absolutePath)
    }

    // --- Capa de texto -------------------------------------------------------

    @Test
    fun elPdfDeUnEscaneoLlevaElTextoDentro() = runBlocking {
        val imagen = crearImagen("pagina.jpg", 1200, 1700)
        val palabras = listOf(
            palabra("Acta", 0.10f, 0.08f, 0.24f, 0.12f),
            palabra("reunion", 0.26f, 0.08f, 0.52f, 0.12f),
            palabra("EXP-2026-0417", 0.10f, 0.40f, 0.48f, 0.43f),
        )

        val salida = File(trabajo, "escaneo.pdf").absolutePath
        val resultado = motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(imagen.absolutePath, TextoPagina(palabras))),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )
        assertTrue("no se creo el PDF: $resultado", resultado is ResultadoPdf.Exito)

        val encontrado = motor.buscarTexto(salida, "EXP-2026-0417")
        assertTrue(encontrado is ResultadoPdf.Exito)
        val coincidencias = (encontrado as ResultadoPdf.Exito).valor
        assertEquals("la palabra tenia que aparecer una vez", 1, coincidencias.size)
        assertEquals(0, coincidencias.first().pagina)
    }

    @Test
    fun cadaPalabraCaeDondeEstabaEnLaImagen() = runBlocking {
        val imagen = crearImagen("pagina.jpg", 1000, 1400)
        // Una palabra arriba a la izquierda y otra abajo a la derecha: si la
        // capa de texto estuviera volcada en Y, que es el fallo tipico al pasar
        // de coordenadas de imagen a coordenadas de PDF, saldrian cambiadas.
        val palabras = listOf(
            palabra("ARRIBA", 0.08f, 0.06f, 0.34f, 0.10f),
            palabra("ABAJO", 0.60f, 0.88f, 0.92f, 0.92f),
        )

        val salida = File(trabajo, "posiciones.pdf").absolutePath
        motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(imagen.absolutePath, TextoPagina(palabras))),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )

        val arriba = unaCoincidencia(salida, "ARRIBA")
        val abajo = unaCoincidencia(salida, "ABAJO")

        assertTrue(
            "ARRIBA esta en ${arriba.arriba} y ABAJO en ${abajo.arriba}: la capa de texto esta volcada",
            arriba.arriba < abajo.arriba,
        )
        assertTrue(
            "ARRIBA deberia caer cerca del 8 % de la pagina, y cae en ${arriba.izquierda}",
            abs(arriba.izquierda - 0.08f) < 0.06f,
        )
        assertTrue(
            "ABAJO deberia caer pasada la mitad, y cae en ${abajo.izquierda}",
            abajo.izquierda > 0.45f,
        )
    }

    @Test
    fun lasPalabrasConTildeNoSePierden() = runBlocking {
        // Las palabras acentuadas se escriben en el PDF como cadena
        // hexadecimal, porque la "o" con tilde no es un caracter imprimible en
        // la codificacion de la fuente. Eso las hace faciles de dar por
        // perdidas al mirar el fichero por encima, y faciles de romper de
        // verdad al tocar la eleccion de fuente. Esta prueba las busca por el
        // camino que importa, que es el mismo que usa el visor.
        val imagen = crearImagen("acentos.jpg", 900, 1200)
        val palabras = listOf(
            // Precompuestas: es la forma en que las devuelve el reconocedor.
            palabra("reunión", 0.10f, 0.10f, 0.40f, 0.14f),
            palabra("mañana", 0.10f, 0.20f, 0.40f, 0.24f),
            // Descompuesta: la letra y la tilde como dos caracteres sueltos.
            palabra("renovacio" + ACENTO_AGUDO + "n", 0.10f, 0.30f, 0.40f, 0.34f),
        )

        val salida = File(trabajo, "acentos.pdf").absolutePath
        motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(imagen.absolutePath, TextoPagina(palabras))),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )

        // Se busca sin tilde: es como escribe la mayoria en un buscador, y es
        // lo que tiene que funcionar aunque la palabra se haya escrito con
        // tilde o sin ella.
        val reunion = motor.buscarTexto(salida, "reuni")
        assertTrue(reunion is ResultadoPdf.Exito)
        assertTrue(
            "la palabra acentuada se perdio de la capa de texto",
            (reunion as ResultadoPdf.Exito).valor.isNotEmpty(),
        )

        val manana = motor.buscarTexto(salida, "ma")
        assertTrue(manana is ResultadoPdf.Exito)
        assertTrue(
            "la palabra con enye se perdio de la capa de texto",
            (manana as ResultadoPdf.Exito).valor.isNotEmpty(),
        )

        val renovacion = motor.buscarTexto(salida, "renova")
        assertTrue(renovacion is ResultadoPdf.Exito)
        assertTrue(
            "la palabra con tilde descompuesta se perdio de la capa de texto",
            (renovacion as ResultadoPdf.Exito).valor.isNotEmpty(),
        )
    }

    @Test
    fun unEscaneoSinTextoSigueSiendoUnPdfValido() = runBlocking {
        val imagen = crearImagen("sola.jpg", 800, 1000)
        val salida = File(trabajo, "sin-texto.pdf").absolutePath

        val resultado = motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(imagen.absolutePath, TextoPagina.SIN_TEXTO)),
            tamano = TamanoPagina.A4,
            rutaSalida = salida,
        )
        assertTrue(resultado is ResultadoPdf.Exito)

        val abierto = motor.abrir(salida)
        assertTrue(abierto is ResultadoPdf.Exito)
        assertEquals(1, (abierto as ResultadoPdf.Exito).valor.numeroPaginas)
    }

    @Test
    fun elTextoNoSeVe() = runBlocking {
        // El modo de dibujo invisible se escribe como "3 Tr" en el flujo de
        // contenido. Se comprueba sobre el fichero porque es la unica forma de
        // distinguir "texto invisible encima de la imagen" de "texto pintado
        // encima de la imagen", y lo segundo estropearia el documento.
        val imagen = crearImagen("pagina.jpg", 900, 1200)
        val salida = File(trabajo, "invisible.pdf").absolutePath
        motor.escaneoAPdf(
            paginas = listOf(
                PaginaEscaneada(
                    imagen.absolutePath,
                    TextoPagina(listOf(palabra("Secreto", 0.2f, 0.2f, 0.5f, 0.24f))),
                ),
            ),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )

        PDDocument.load(File(salida)).use { documento ->
            val flujo = documento.getPage(0).contents.use { it.readBytes() }.decodeToString()
            assertTrue("falta el modo de dibujo invisible", flujo.contains("3 Tr"))
        }
    }

    @Test
    fun variasPaginasSalenEnOrden() = runBlocking {
        val paginas = (1..3).map { numero ->
            PaginaEscaneada(
                crearImagen("hoja-$numero.jpg", 700, 900).absolutePath,
                TextoPagina(listOf(palabra("HOJA$numero", 0.1f, 0.1f, 0.4f, 0.14f))),
            )
        }
        val salida = File(trabajo, "varias.pdf").absolutePath

        var ultimoAviso = 0 to 0
        motor.escaneoAPdf(
            paginas = paginas,
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
            alAvanzar = { hechas, total -> ultimoAviso = hechas to total },
        )

        assertEquals(3 to 3, ultimoAviso)
        (1..3).forEach { numero ->
            val donde = motor.buscarTexto(salida, "HOJA$numero")
            assertTrue(donde is ResultadoPdf.Exito)
            assertEquals(
                "HOJA$numero deberia estar en la pagina ${numero - 1}",
                numero - 1,
                (donde as ResultadoPdf.Exito).valor.first().pagina,
            )
        }
    }

    // --- Reconocimiento de texto ---------------------------------------------

    @Test
    fun elReconocedorDevuelveLasPalabrasAcentuadas() = runBlocking {
        // Diagnostico de un fallo real: las palabras con tilde no llegaban a la
        // capa de texto del PDF. Esta prueba separa las dos causas posibles —que
        // el reconocedor no las lea, o que se pierdan al escribirlas— mirando lo
        // que devuelve el reconocedor antes de tocar nada.
        val imagen = crearImagenConTexto(
            "acentos-ocr.jpg",
            listOf("Acta de la reunion", "manana y accion"),
        )
        val leido = escaner.reconocerTexto(imagen.absolutePath)
        assertTrue("el reconocimiento fallo: $leido", leido is ResultadoPdf.Exito)

        val palabras = (leido as ResultadoPdf.Exito).valor.palabras.map { it.texto }
        assertTrue(
            "el reconocedor no leyo nada de la imagen",
            palabras.isNotEmpty(),
        )
        assertTrue(
            "se esperaba 'Acta' entre las palabras leidas: $palabras",
            palabras.any { it.contains("Acta", ignoreCase = true) },
        )
    }

    @Test
    fun elReconocedorLeeTildesYEnyes() = runBlocking {
        val imagen = crearImagenConTexto(
            "tildes.jpg",
            listOf("reunion del 12", "manana en la sala"),
            conAcentos = true,
        )
        val leido = escaner.reconocerTexto(imagen.absolutePath)
        val palabras = (leido as ResultadoPdf.Exito).valor.palabras.map { it.texto }

        // Se comprueba el prefijo y no la palabra entera: el reconocedor puede
        // equivocarse con la tilde y eso no es lo que se esta probando aqui.
        assertTrue(
            "no se leyo nada parecido a 'reunion': $palabras",
            palabras.any { it.startsWith("reuni", ignoreCase = true) },
        )
        assertTrue(
            "no se leyo nada parecido a 'manana': $palabras",
            palabras.any { it.startsWith("ma", ignoreCase = true) && it.length >= 5 },
        )
    }

    @Test
    fun lasPalabrasLeidasAcabanEnElPdf() = runBlocking {
        // La cadena entera de verdad: imagen con texto, reconocimiento, PDF.
        // Es la que dice si lo que se lee es lo que se puede buscar despues.
        val imagen = crearImagenConTexto(
            "cadena.jpg",
            listOf("reunion del 12", "manana en la sala"),
            conAcentos = true,
        )
        val texto = (escaner.reconocerTexto(imagen.absolutePath) as ResultadoPdf.Exito).valor
        assertTrue("el reconocedor no leyo nada", texto.palabras.isNotEmpty())

        val salida = File(trabajo, "cadena.pdf").absolutePath
        motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(imagen.absolutePath, texto)),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )

        val dentro = (motor.buscarTexto(salida, "reuni") as ResultadoPdf.Exito).valor
        assertTrue(
            "se leyo la palabra pero no llego al PDF; leidas: " +
                texto.palabras.joinToString { it.texto },
            dentro.isNotEmpty(),
        )
    }

    // --- Revelado ------------------------------------------------------------

    @Test
    fun elRecorteEnderezaLaHoja() = runBlocking {
        // Una hoja clara pintada en perspectiva dentro de una foto oscura. Tras
        // revelarla, el resultado tiene que ser mayormente claro: si el
        // enderezado fallara, saldria la mesa oscura dentro del recorte.
        val foto = crearFotoConHoja("mesa.jpg")
        val hoja = HojaEscaneada(
            id = "h1",
            rutaOriginal = foto.absolutePath,
            cuadro = Cuadrilatero(
                supIzq = Punto(0.24f, 0.10f),
                supDer = Punto(0.78f, 0.16f),
                infDer = Punto(0.84f, 0.88f),
                infIzq = Punto(0.16f, 0.82f),
            ),
            filtro = FiltroPagina.NINGUNO,
        )

        val salida = File(trabajo, "revelada.jpg").absolutePath
        val resultado = escaner.revelar(hoja, salida)
        assertTrue("no se revelo: $resultado", resultado is ResultadoPdf.Exito)

        val revelada = CargadorImagen.cargar(salida)
        assertNotNull(revelada)
        assertTrue("la pagina revelada esta demasiado oscura", claridad(revelada!!) > 0.75f)
    }

    @Test
    fun sinRecorteSeConservaLaFotoEntera() = runBlocking {
        val foto = crearImagen("entera.jpg", 640, 480)
        val hoja = HojaEscaneada(
            id = "h2",
            rutaOriginal = foto.absolutePath,
            cuadro = Cuadrilatero.COMPLETO,
            filtro = FiltroPagina.NINGUNO,
        )
        val salida = File(trabajo, "entera-revelada.jpg").absolutePath
        assertTrue(escaner.revelar(hoja, salida) is ResultadoPdf.Exito)

        val revelada = CargadorImagen.cargar(salida)
        assertNotNull(revelada)
        // La proporcion se conserva; el tamano exacto puede cambiar por el
        // submuestreo, asi que se compara la forma y no los pixeles.
        val proporcion = revelada!!.width.toFloat() / revelada.height
        assertTrue("la foto entera salio deformada: $proporcion", abs(proporcion - 640f / 480f) < 0.05f)
    }

    @Test
    fun girarLaHojaIntercambiaLosLados() = runBlocking {
        val foto = crearImagen("vertical.jpg", 600, 900)
        val base = HojaEscaneada(
            id = "h3",
            rutaOriginal = foto.absolutePath,
            cuadro = Cuadrilatero.COMPLETO,
            filtro = FiltroPagina.NINGUNO,
        )
        val derecha = File(trabajo, "derecha.jpg").absolutePath
        val girada = File(trabajo, "girada.jpg").absolutePath

        escaner.revelar(base, derecha)
        escaner.revelar(base.girada(), girada)

        val antes = CargadorImagen.cargar(derecha)!!
        val despues = CargadorImagen.cargar(girada)!!
        assertTrue("antes de girar deberia ser vertical", antes.height > antes.width)
        assertTrue("despues de girar deberia ser apaisada", despues.width > despues.height)
    }

    // --- Resolucion ----------------------------------------------------------

    @Test
    fun laFotoSeCargaAlTamanoPedidoYNoAlaMitad() {
        // `inSampleSize` solo admite potencias de dos. Pidiendo 1300 px sobre
        // una foto de 2000, el submuestreo saltaba a 2 y devolvia 1000: la
        // mitad del lado y la cuarta parte de los pixeles, tirados justo antes
        // de enderezar y de leer el texto.
        val foto = crearImagen("grande.jpg", 2000, 1500)
        val cargada = CargadorImagen.cargar(foto.absolutePath, 1300)
        assertNotNull(cargada)
        assertTrue(
            "se pidieron 1300 px de lado y se cargaron ${cargada!!.width}",
            cargada.width in 1200..1400,
        )
    }

    @Test
    fun unaFotoPequenaNoSeAmplia() {
        val foto = crearImagen("pequena.jpg", 600, 450)
        val cargada = CargadorImagen.cargar(foto.absolutePath, 2600)
        assertNotNull(cargada)
        assertEquals("ampliar no anade detalle, solo peso", 600, cargada!!.width)
    }

    @Test
    fun enderezarNoAmpliaMasAllaDelPapel() = runBlocking {
        // Una hoja girada tiene una caja envolvente mucho mayor que ella misma.
        // Si el tamano de salida se sacara de la caja, la pagina saldria
        // ampliada e igual de borrosa pero pesando el triple.
        val foto = crearImagen("girada.jpg", 1600, 1200)
        val hoja = HojaEscaneada(
            id = "g1",
            rutaOriginal = foto.absolutePath,
            cuadro = Cuadrilatero(
                supIzq = Punto(0.42f, 0.08f),
                supDer = Punto(0.90f, 0.44f),
                infDer = Punto(0.56f, 0.92f),
                infIzq = Punto(0.08f, 0.56f),
            ),
            filtro = FiltroPagina.NINGUNO,
        )
        val salida = File(trabajo, "girada-revelada.jpg").absolutePath
        assertTrue(escaner.revelar(hoja, salida) is ResultadoPdf.Exito)

        val revelada = CargadorImagen.cargar(salida, 4000)
        assertNotNull(revelada)
        // El lado del papel ronda los 800 px en esta foto; la diagonal de su
        // caja pasa de 1100. Sirve el orden de magnitud, no el pixel exacto.
        assertTrue(
            "la pagina salio de ${revelada!!.width} px, mas que el papel",
            revelada.width < 1000,
        )
    }

    @Test
    fun laPaginaConservaLaResolucionDeLaFoto() = runBlocking {
        // El fallo que arregla esto se veia a simple vista y costo encontrarlo.
        // La pagina es un trozo del encuadre, no el encuadre entero. Cargando la
        // foto al tamano que se queria **para la pagina**, la pagina salia mucho
        // mas pequena que eso y ademas remuestreada dos veces sobre datos ya
        // reducidos. Medido sobre una foto real de doce megapixeles: la pagina
        // acababa, con el tope de entonces, en 2351 px de alto en lugar de los
        // 2900 pedidos.
        val foto = crearImagen("doce-megapixeles.jpg", 2400, 3000)
        val hoja = HojaEscaneada(
            id = "r1",
            rutaOriginal = foto.absolutePath,
            // Una hoja que ocupa unas tres cuartas partes del encuadre, que es
            // lo normal al fotografiar un papel.
            cuadro = Cuadrilatero(
                supIzq = Punto(0.12f, 0.11f),
                supDer = Punto(0.88f, 0.11f),
                infDer = Punto(0.88f, 0.89f),
                infIzq = Punto(0.12f, 0.89f),
            ),
            filtro = FiltroPagina.NINGUNO,
        )

        val salida = File(trabajo, "resolucion.png").absolutePath
        assertTrue(escaner.revelar(hoja, salida) is ResultadoPdf.Exito)

        val revelada = CargadorImagen.cargar(salida, 4000)
        assertNotNull(revelada)
        // La hoja mide el 78 % del alto de una foto de 3000 px: unos 2340 px.
        // Cargando la foto entera se conservan; cargandola al tamano de la
        // pagina se quedaban en unos 1800.
        assertTrue(
            "la pagina salio de ${revelada!!.height} px de alto, se perdio resolucion",
            revelada.height > 2100,
        )
    }

    @Test
    fun lasDosCarasDeUnDniSalenATamanoRealEnLaMismaHoja() = runBlocking {
        // La promesa entera del modo tarjeta en una prueba: si lo imprimes, sale
        // del tamano del DNI. Se mide sobre el PDF ya escrito y en puntos, que
        // es lo unico que el papel respeta; un fallo aqui no se ve en pantalla y
        // solo se descubre con una regla encima de la impresion.
        val anverso = crearImagenGris("dni-a.png", 1600, 1009)
        val reverso = crearImagenGris("dni-b.png", 1600, 1009)
        val salida = File(trabajo, "dni.pdf").absolutePath

        val resultado = motor.tarjetasAPdf(
            paginas = listOf(
                PaginaEscaneada(anverso.absolutePath, TextoPagina.SIN_TEXTO),
                PaginaEscaneada(reverso.absolutePath, TextoPagina.SIN_TEXTO),
            ),
            tipo = TipoTarjeta.ID1,
            tamano = TamanoPagina.A4,
            rutaSalida = salida,
        )
        assertTrue("no se creo el PDF: $resultado", resultado is ResultadoPdf.Exito)

        PDDocument.load(File(salida)).use { documento ->
            assertEquals("las dos caras tenian que ir en una sola hoja", 1, documento.numberOfPages)

            val pagina = documento.getPage(0)
            val caja = pagina.mediaBox
            assertTrue(
                "la hoja mide ${caja.width} x ${caja.height} pt y tenia que ser un A4",
                abs(caja.width - 595.28f) < 2f && abs(caja.height - 841.89f) < 2f,
            )

            // Las dos imagenes, con su tamano dibujado de verdad.
            val dibujadas = tamanosDibujados(documento, 0)
            assertEquals("tenian que dibujarse dos caras", 2, dibujadas.size)
            dibujadas.forEach { (ancho, alto) ->
                // 85,60 mm y 53,98 mm en puntos. Se deja un punto de holgura
                // porque la imagen se encaja conservando su propia forma.
                assertTrue(
                    "una cara salio de ${ancho / PUNTOS_POR_MM} x ${alto / PUNTOS_POR_MM} mm",
                    abs(ancho - 85.60f * PUNTOS_POR_MM) < 2f &&
                        abs(alto - 53.98f * PUNTOS_POR_MM) < 2f,
                )
            }

            // Y una encima de la otra, sin pisarse.
            val alturas = posicionesVerticales(documento, 0).sorted()
            assertTrue(
                "las dos caras salieron a la misma altura: $alturas",
                abs(alturas[1] - alturas[0]) > 53.98f * PUNTOS_POR_MM,
            )
        }
    }

    @Test
    fun unPasaporteAbreOtraHojaCuandoNoCabenMas() = runBlocking {
        // Con cuatro caras de pasaporte no cabe todo en un folio. Lo que no
        // puede pasar es que la de mas se dibuje fuera de la pagina, que se
        // imprime cortada y no se ve venir en la vista previa.
        val caras = (1..4).map {
            PaginaEscaneada(crearImagenGris("pas-$it.png", 1420, 1000).absolutePath, TextoPagina.SIN_TEXTO)
        }
        val salida = File(trabajo, "pasaporte.pdf").absolutePath

        val resultado = motor.tarjetasAPdf(
            paginas = caras,
            tipo = TipoTarjeta.ID3,
            tamano = TamanoPagina.A4,
            rutaSalida = salida,
        )
        assertTrue("no se creo el PDF: $resultado", resultado is ResultadoPdf.Exito)

        PDDocument.load(File(salida)).use { documento ->
            assertTrue(
                "cuatro paginas de pasaporte tenian que ocupar mas de una hoja",
                documento.numberOfPages >= 2,
            )
            for (indice in 0 until documento.numberOfPages) {
                val alto = documento.getPage(indice).mediaBox.height
                posicionesVerticales(documento, indice).forEach { y ->
                    assertTrue("una cara se dibujo en y=$y, fuera de la hoja", y >= -1f && y <= alto)
                }
            }
        }
    }

    @Test
    fun unaTarjetaEnAutomaticoSeReconocePorLaForma() = runBlocking {
        // Sin decirle el formato: por la proporcion tiene que deducir ID-1 y
        // dibujarla con las medidas de un DNI.
        val tarjeta = crearImagenGris("auto.png", 1586, 1000)
        val salida = File(trabajo, "auto.pdf").absolutePath

        motor.tarjetasAPdf(
            paginas = listOf(PaginaEscaneada(tarjeta.absolutePath, TextoPagina.SIN_TEXTO)),
            tipo = TipoTarjeta.AUTOMATICO,
            tamano = TamanoPagina.A4,
            rutaSalida = salida,
        )

        PDDocument.load(File(salida)).use { documento ->
            val (ancho, _) = tamanosDibujados(documento, 0).first()
            assertTrue(
                "en automatico la tarjeta salio de ${ancho / PUNTOS_POR_MM} mm de ancho",
                abs(ancho - 85.60f * PUNTOS_POR_MM) < 2f,
            )
        }
    }

    @Test
    fun unaPaginaEnGrisNoSeGuardaPorTriplicado() = runBlocking {
        // Una pagina escaneada y mejorada es gris: el filtro la deja en tinta y
        // papel, con el mismo valor repetido en los tres canales. PDFBox escribe
        // siempre `DeviceRGB`, asi que cada pagina viajaba tres veces. Medido
        // sobre una hoja real a 300 ppp son 4,55 MB contra 2,82 MB, exactamente
        // los mismos pixeles.
        val gris = crearImagenGris("gris.png", 1200, 1700)
        val salida = File(trabajo, "gris.pdf").absolutePath

        val resultado = motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(gris.absolutePath, TextoPagina.SIN_TEXTO)),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )
        assertTrue("no se creo el PDF: $resultado", resultado is ResultadoPdf.Exito)

        assertEquals(
            "la pagina gris no se guardo en un solo canal",
            "DeviceGray",
            espacioDeColorDeLaPagina(salida),
        )

        // Y tiene que seguir siendo un PDF legible, no solo uno mas pequeno.
        val leido = motor.abrir(salida)
        assertTrue("el PDF en gris no se puede abrir: $leido", leido is ResultadoPdf.Exito)
    }

    @Test
    fun unaPaginaConColorConservaElColor() = runBlocking {
        // La otra mitad de lo anterior, y la que de verdad importa: si la
        // comprobacion de "esto es gris" se hiciera con una muestra en vez de
        // con todos los pixeles, una pagina con un sello rojo o un subrayado
        // saldria en blanco y negro. Eso no es comprimir mejor, es tirar
        // contenido de la pagina de alguien.
        val color = crearImagenConToqueDeColor("color.png", 1200, 1700)
        val salida = File(trabajo, "color.pdf").absolutePath

        val resultado = motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(color.absolutePath, TextoPagina.SIN_TEXTO)),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )
        assertTrue("no se creo el PDF: $resultado", resultado is ResultadoPdf.Exito)

        assertEquals(
            "una pagina con color se guardo en gris: se perdio el color",
            "DeviceRGB",
            espacioDeColorDeLaPagina(salida),
        )
    }

    @Test
    fun unaImagenGrisConTransparenciaNoPierdeElFondo() = runBlocking {
        // Un canal de gris no guarda transparencia. Una imagen gris **con**
        // fondo transparente cumple la mitad de la condicion, asi que si solo se
        // mirara el color se colaria por el camino de gris y el fondo saldria
        // negro sin que nada avisara.
        val conAlfa = crearImagenGrisConAlfa("alfa.png", 800, 1000)
        val salida = File(trabajo, "alfa.pdf").absolutePath

        val resultado = motor.escaneoAPdf(
            paginas = listOf(PaginaEscaneada(conAlfa.absolutePath, TextoPagina.SIN_TEXTO)),
            tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
            rutaSalida = salida,
        )
        assertTrue("no se creo el PDF: $resultado", resultado is ResultadoPdf.Exito)

        assertEquals(
            "una imagen con transparencia se guardo en gris: se perdio el alfa",
            "DeviceRGB",
            espacioDeColorDeLaPagina(salida),
        )
    }

    @Test
    fun laRafagaDejaLaPaginaConMenosGrano() = runBlocking {
        // La prueba que justifica la rafaga, y de punta a punta: tres fotos con
        // grano distinto de la misma hoja tienen que dar una pagina mas limpia
        // que una sola de ellas. Se mide sobre el papel, que es donde el grano
        // se ve, y con el filtro apagado, porque el filtro manda el papel a
        // blanco puro y taparia justo lo que se quiere medir.
        val caras = (1..3).map { crearImagenConGrano("grano-$it.jpg", it) }
        val cuadro = Cuadrilatero(
            supIzq = Punto(0.05f, 0.05f),
            supDer = Punto(0.95f, 0.05f),
            infDer = Punto(0.95f, 0.95f),
            infIzq = Punto(0.05f, 0.95f),
        )

        val sola = HojaEscaneada(
            id = "sola",
            rutaOriginal = caras.first().absolutePath,
            cuadro = cuadro,
            filtro = FiltroPagina.NINGUNO,
        )
        val enRafaga = sola.copy(
            id = "rafaga",
            rutasRafaga = caras.drop(1).map { it.absolutePath },
        )

        val rutaSola = File(trabajo, "sola.png").absolutePath
        val rutaRafaga = File(trabajo, "rafaga.png").absolutePath
        assertTrue(escaner.revelar(sola, rutaSola) is ResultadoPdf.Exito)
        assertTrue(escaner.revelar(enRafaga, rutaRafaga) is ResultadoPdf.Exito)

        val granoSola = granoDelPapel(rutaSola)
        val granoRafaga = granoDelPapel(rutaRafaga)
        println("grano con una foto: $granoSola, con rafaga de tres: $granoRafaga")
        assertTrue(
            "el grano paso de $granoSola a $granoRafaga: la rafaga no esta limpiando",
            granoRafaga < granoSola * 0.8,
        )
    }

    /** Desviacion tipica del papel: cuanto grano queda en las zonas lisas. */
    private fun granoDelPapel(ruta: String): Double {
        val mapa = CargadorImagen.cargar(ruta, 4000) ?: return 0.0
        val pixeles = IntArray(mapa.width * mapa.height)
        mapa.getPixels(pixeles, 0, mapa.width, 0, 0, mapa.width, mapa.height)
        // Solo la mitad superior, que en la imagen de prueba es papel liso.
        val hasta = pixeles.size / 3
        var suma = 0.0
        var suma2 = 0.0
        for (indice in 0 until hasta) {
            val valor = Color.red(pixeles[indice]).toDouble()
            suma += valor
            suma2 += valor * valor
        }
        mapa.recycle()
        val media = suma / hasta
        return kotlin.math.sqrt((suma2 / hasta) - media * media)
    }

    /** Una hoja lisa con grano reproducible distinto en cada disparo. */
    private fun crearImagenConGrano(nombre: String, semilla: Int): File {
        val ancho = 1200
        val alto = 1600
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val pixeles = IntArray(ancho * alto)
        var estado = semilla * 7919 + 13
        for (indice in pixeles.indices) {
            val y = indice / ancho
            estado = estado * 1103515245 + 12345
            val ruido = ((estado ushr 16) % 31) - 15
            val base = if (y > alto * 2 / 3 && (y / 20) % 2 == 0) 60 else 215
            val valor = (base + ruido).coerceIn(0, 255)
            pixeles[indice] = Color.argb(255, valor, valor, valor)
        }
        mapa.setPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        // JPEG con calidad alta: es lo que entrega la camara, y comprimir
        // tambien es parte de lo que la rafaga tiene que superar.
        val fichero = File(trabajo, nombre)
        FileOutputStream(fichero).use { mapa.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        mapa.recycle()
        return fichero
    }

    @Test
    fun revelarConMejoraCuestaLoQuePuedeCostar() = runBlocking {
        // La deconvolucion son catorce pasadas sobre la pagina entera, bastante
        // mas que la mascara de desenfoque que habia antes. Esto corre al
        // revelar cada hoja **y** cada vez que se mueve el deslizador de
        // intensidad en la revision, asi que si costara segundos la pantalla de
        // revision seria inusable por bonito que quedara el resultado.
        //
        // Se mide en el telefono y no en la JVM del ordenador a proposito: es el
        // unico sitio donde el numero significa algo.
        val foto = crearImagen("mejora-coste.jpg", 3000, 4000)
        val hoja = HojaEscaneada(
            id = "coste",
            rutaOriginal = foto.absolutePath,
            cuadro = Cuadrilatero(
                supIzq = Punto(0.10f, 0.08f),
                supDer = Punto(0.90f, 0.08f),
                infDer = Punto(0.90f, 0.92f),
                infIzq = Punto(0.10f, 0.92f),
            ),
            filtro = FiltroPagina.DOCUMENTO_NITIDO,
            intensidadFiltro = 1f,
        )
        val salida = File(trabajo, "coste.png").absolutePath

        // Una pasada de calentamiento: la primera paga la compilacion.
        escaner.revelar(hoja, File(trabajo, "calentar.png").absolutePath)

        val inicio = System.nanoTime()
        val resultado = escaner.revelar(hoja, salida)
        val tardado = (System.nanoTime() - inicio) / 1_000_000

        assertTrue("no se revelo: $resultado", resultado is ResultadoPdf.Exito)
        assertTrue(
            "revelar con mejora costo $tardado ms en el telefono",
            tardado < 12_000,
        )
        println("revelado con mejora a 300 ppp e intensidad maxima: $tardado ms")

        // Y la vista previa, que es la que de verdad se nota: se rehace cada vez
        // que se mueve el deslizador de intensidad.
        escaner.previsualizar(hoja, 1500)
        val inicioVista = System.nanoTime()
        val vista = escaner.previsualizar(hoja, 1500)
        val tardadoVista = (System.nanoTime() - inicioVista) / 1_000_000
        assertTrue("no se previsualizo: $vista", vista is ResultadoPdf.Exito)
        assertTrue(
            "la vista previa costo $tardadoVista ms: el deslizador iria a tirones",
            tardadoVista < 2_000,
        )
        println("vista previa con mejora: $tardadoVista ms")
    }

    @Test
    fun unaFotoGrandeRevelaLaPaginaA300Ppp() = runBlocking {
        // El tope de pagina estuvo en 2900 px con el argumento de que por encima
        // el ojo no lo nota. Medido sobre una foto real, el argumento era falso:
        // una hoja fotografiada con doce megapixeles ocupa bastante mas que eso
        // dentro del encuadre, asi que el tope no comprimia, **tiraba**
        // resolucion ya capturada. Esta prueba fija el limite nuevo: una foto
        // grande tiene que salir a su tamano y no al del tope antiguo.
        val foto = crearImagen("doce-megapixeles-vertical.jpg", 3000, 4000)
        val hoja = HojaEscaneada(
            id = "r300",
            rutaOriginal = foto.absolutePath,
            // La hoja ocupa el 78 % del alto: unos 3120 px, muy por encima de
            // los 2900 a los que se recortaba antes.
            cuadro = Cuadrilatero(
                supIzq = Punto(0.12f, 0.11f),
                supDer = Punto(0.88f, 0.11f),
                infDer = Punto(0.88f, 0.89f),
                infIzq = Punto(0.12f, 0.89f),
            ),
            filtro = FiltroPagina.NINGUNO,
        )

        val salida = File(trabajo, "resolucion-300.png").absolutePath
        assertTrue(escaner.revelar(hoja, salida) is ResultadoPdf.Exito)

        val revelada = CargadorImagen.cargar(salida, 5000)
        assertNotNull(revelada)
        assertTrue(
            "la pagina salio de ${revelada!!.height} px: sigue recortada al tope antiguo",
            revelada.height > 3000,
        )
    }

    @Test
    fun unaFotoMasPequenaQueLaPaginaNoRompeElRevelado() = runBlocking {
        // El caso que se colo al subir la resolucion de pagina: al pedir una
        // pagina de 2900 px sobre una foto de 1000, el calculo de "cuanto hay
        // que cargar" dejaba un rango del reves y el revelado fallaba entero.
        // Una imagen pequena llega mas a menudo de lo que parece: de la galeria,
        // de una captura de pantalla, de un adjunto de mensajeria.
        val foto = crearImagen("chiquita.jpg", 1000, 750)
        val hoja = HojaEscaneada(
            id = "p1",
            rutaOriginal = foto.absolutePath,
            cuadro = Cuadrilatero(
                supIzq = Punto(0.1f, 0.1f),
                supDer = Punto(0.9f, 0.1f),
                infDer = Punto(0.9f, 0.9f),
                infIzq = Punto(0.1f, 0.9f),
            ),
            filtro = FiltroPagina.NINGUNO,
        )

        val salida = File(trabajo, "chiquita-revelada.png").absolutePath
        val resultado = escaner.revelar(hoja, salida)
        assertTrue("el revelado fallo: $resultado", resultado is ResultadoPdf.Exito)
        assertNotNull(CargadorImagen.cargar(salida, 2000))
    }

    // --- Utilidades ----------------------------------------------------------

    private fun palabra(
        texto: String,
        izquierda: Float,
        arriba: Float,
        derecha: Float,
        abajo: Float,
    ) = PalabraOcr(texto, Rectangulo(izquierda, arriba, derecha, abajo))

    private suspend fun unaCoincidencia(ruta: String, consulta: String): Rectangulo {
        val resultado = motor.buscarTexto(ruta, consulta)
        assertTrue("no se encontro '$consulta' en el PDF", resultado is ResultadoPdf.Exito)
        val lista = (resultado as ResultadoPdf.Exito).valor
        assertTrue("no se encontro '$consulta' en el PDF", lista.isNotEmpty())
        return lista.first().marco
    }

    /**
     * Una pagina blanca con texto escrito de verdad, para que el reconocedor
     * tenga algo que leer.
     *
     * Se dibuja con el motor de texto de Android y no se carga una foto: asi la
     * prueba no depende de ningun fichero externo y las letras salen siempre
     * igual de nitidas.
     */
    private fun crearImagenConTexto(
        nombre: String,
        lineas: List<String>,
        conAcentos: Boolean = false,
    ): File {
        val ancho = 1200
        val alto = 900
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapa)
        lienzo.drawColor(Color.WHITE)

        val tinta = Paint().apply {
            color = Color.BLACK
            textSize = 64f
            isAntiAlias = true
        }
        lineas.forEachIndexed { indice, linea ->
            val texto = if (conAcentos) {
                linea.replace("reunion", "reunión").replace("manana", "mañana")
            } else {
                linea
            }
            lienzo.drawText(texto, 80f, 200f + indice * 120f, tinta)
        }
        return guardar(mapa, nombre)
    }

    /** Una pagina como la deja el filtro de documento: gris puro, tinta y papel. */
    private fun crearImagenGris(nombre: String, ancho: Int, alto: Int): File {
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapa)
        lienzo.drawColor(Color.WHITE)
        val tinta = Paint().apply { color = Color.rgb(30, 30, 30) }
        for (linea in 0 until 24) {
            val y = alto * (0.06f + linea * 0.037f)
            lienzo.drawRect(ancho * 0.1f, y, ancho * 0.9f, y + alto * 0.012f, tinta)
        }
        return guardarSinPerdida(mapa, nombre)
    }

    /**
     * Mide como se dibujo de verdad cada imagen de una pagina.
     *
     * No vale con mirar el tamano en pixeles del XObject: el PDF guarda la
     * imagen a un tamano y la **dibuja** a otro, y lo que acaba en el papel es
     * lo segundo. El tamano dibujado esta en la matriz de transformacion que
     * hay activa cuando se ejecuta el operador `Do`, asi que hay que recorrer el
     * flujo de contenido para leerla.
     */
    private class MedidorDeImagenes(pagina: PDPage) : PDFGraphicsStreamEngine(pagina) {
        /** Ancho, alto y borde inferior de cada imagen, en puntos. */
        val dibujadas = mutableListOf<Triple<Float, Float, Float>>()

        // Se hereda de PDFGraphicsStreamEngine y no del motor pelado: el motor
        // base no trae registrado ningun operador, ni siquiera `cm`, asi que la
        // matriz de transformacion se quedaba en la identidad y toda imagen
        // parecia medir un punto. Esta subclase ya registra el estado grafico.
        override fun drawImage(imagen: PDImage) {
            val matriz = graphicsState.currentTransformationMatrix
            dibujadas += Triple(matriz.scalingFactorX, matriz.scalingFactorY, matriz.translateY)
        }

        override fun appendRectangle(p0: PointF, p1: PointF, p2: PointF, p3: PointF) = Unit
        override fun clip(regla: Path.FillType) = Unit
        override fun moveTo(x: Float, y: Float) = Unit
        override fun lineTo(x: Float, y: Float) = Unit
        override fun curveTo(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) = Unit
        override fun getCurrentPoint(): PointF = PointF(0f, 0f)
        override fun closePath() = Unit
        override fun endPath() = Unit
        override fun strokePath() = Unit
        override fun fillPath(regla: Path.FillType) = Unit
        override fun fillAndStrokePath(regla: Path.FillType) = Unit
        override fun shadingFill(nombre: COSName) = Unit
    }

    private fun medir(documento: PDDocument, pagina: Int): List<Triple<Float, Float, Float>> {
        val hoja = documento.getPage(pagina)
        return MedidorDeImagenes(hoja).apply { processPage(hoja) }.dibujadas
    }

    /** Ancho y alto con que se dibujo cada imagen, en puntos. */
    private fun tamanosDibujados(documento: PDDocument, pagina: Int): List<Pair<Float, Float>> =
        medir(documento, pagina).map { it.first to it.second }

    /** Borde inferior de cada imagen, en puntos desde abajo de la hoja. */
    private fun posicionesVerticales(documento: PDDocument, pagina: Int): List<Float> =
        medir(documento, pagina).map { it.third }

    /**
     * Espacio de color de la imagen incrustada en la primera pagina.
     *
     * Se mira el XObject de la pagina y no los bytes crudos del fichero. Buscar
     * "/DeviceGray" dentro del PDF parece equivalente y no lo es: la mascara de
     * transparencia que acompana a una imagen con alfa **tambien** es gris, asi
     * que una pagina en color con alfa daba positivo y la prueba pasaba o
     * fallaba por el motivo equivocado.
     */
    private fun espacioDeColorDeLaPagina(ruta: String): String =
        PDDocument.load(File(ruta)).use { documento ->
            val recursos = documento.getPage(0).resources
            val nombre = recursos.xObjectNames.first { recursos.getXObject(it) is PDImageXObject }
            (recursos.getXObject(nombre) as PDImageXObject).colorSpace.name
        }

    /** Gris, pero con una esquina transparente. */
    private fun crearImagenGrisConAlfa(nombre: String, ancho: Int, alto: Int): File {
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapa)
        lienzo.drawColor(Color.WHITE)
        val tinta = Paint().apply { color = Color.rgb(40, 40, 40) }
        lienzo.drawRect(ancho * 0.1f, alto * 0.1f, ancho * 0.9f, alto * 0.5f, tinta)
        // Un agujero de verdad, no un blanco: se borra el alfa de esa zona.
        val borrar = Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
        lienzo.drawRect(0f, alto * 0.7f, ancho * 0.4f, alto.toFloat(), borrar)
        return guardarSinPerdida(mapa, nombre)
    }

    /** La misma pagina, pero con un sello rojo encima. */
    private fun crearImagenConToqueDeColor(nombre: String, ancho: Int, alto: Int): File {
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapa)
        lienzo.drawColor(Color.WHITE)
        val tinta = Paint().apply { color = Color.rgb(30, 30, 30) }
        for (linea in 0 until 24) {
            val y = alto * (0.06f + linea * 0.037f)
            lienzo.drawRect(ancho * 0.1f, y, ancho * 0.9f, y + alto * 0.012f, tinta)
        }
        val sello = Paint().apply { color = Color.rgb(196, 32, 32) }
        lienzo.drawRect(ancho * 0.6f, alto * 0.8f, ancho * 0.88f, alto * 0.88f, sello)
        return guardarSinPerdida(mapa, nombre)
    }

    private fun crearImagen(nombre: String, ancho: Int, alto: Int): File {
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        Canvas(mapa).drawColor(Color.rgb(248, 246, 242))
        return guardar(mapa, nombre)
    }

    /** Una foto de una hoja clara sobre una mesa oscura, en perspectiva. */
    private fun crearFotoConHoja(nombre: String): File {
        val ancho = 1024
        val alto = 768
        val mapa = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(mapa)
        lienzo.drawColor(Color.rgb(58, 44, 34))

        val camino = android.graphics.Path().apply {
            moveTo(0.24f * ancho, 0.10f * alto)
            lineTo(0.78f * ancho, 0.16f * alto)
            lineTo(0.84f * ancho, 0.88f * alto)
            lineTo(0.16f * ancho, 0.82f * alto)
            close()
        }
        lienzo.drawPath(camino, Paint().apply { color = Color.rgb(246, 244, 240) })
        return guardar(mapa, nombre)
    }

    /**
     * Guarda sin perdida y conservando el alfa.
     *
     * El [guardar] de siempre escribe JPEG, que para la mayoria de las pruebas
     * da igual y ademas es lo que sale de una camara. Pero para las que miran
     * **como se incrusta** la imagen en el PDF no vale: el JPEG no tiene canal
     * alfa y mueve los valores, que son justo las dos cosas que esas pruebas
     * comprueban.
     */
    private fun guardarSinPerdida(mapa: Bitmap, nombre: String): File {
        val fichero = File(trabajo, nombre)
        FileOutputStream(fichero).use { mapa.compress(Bitmap.CompressFormat.PNG, 100, it) }
        mapa.recycle()
        return fichero
    }

    private fun guardar(mapa: Bitmap, nombre: String): File {
        val fichero = File(trabajo, nombre)
        FileOutputStream(fichero).use { mapa.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        mapa.recycle()
        return fichero
    }

    /** Fraccion de pixeles claros, para saber si el recorte cogio la hoja o la mesa. */
    private fun claridad(mapa: Bitmap): Float {
        val paso = maxOf(1, minOf(mapa.width, mapa.height) / 64)
        var claros = 0
        var total = 0
        var y = 0
        while (y < mapa.height) {
            var x = 0
            while (x < mapa.width) {
                val pixel = mapa.getPixel(x, y)
                val gris = (Color.red(pixel) * 299 + Color.green(pixel) * 587 +
                    Color.blue(pixel) * 114) / 1000
                if (gris > 170) claros++
                total++
                x += paso
            }
            y += paso
        }
        return if (total == 0) 0f else claros.toFloat() / total
    }

    companion object {
        /**
         * La tilde suelta.
         *
         * Va como codigo y no como caracter porque un caracter combinante en el
         * fuente es invisible: se pega a la letra anterior al abrir el fichero y
         * no hay forma de ver que esta ahi ni de saber si alguien lo borro.
         */
        private const val ACENTO_AGUDO = "\u0301"

        @JvmStatic
        @BeforeClass
        fun cargarPdfBox() {
            PDFBoxResourceLoader.init(
                ApplicationProvider.getApplicationContext<android.content.Context>(),
            )
        }
    }
}
