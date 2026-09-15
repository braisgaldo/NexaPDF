package es.ghatostudio.nexapdf.domain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import es.ghatostudio.nexapdf.ui.componentes.EstadoEncuadre
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * El acercamiento a una coincidencia de la busqueda.
 *
 * Todo esto existe por un fallo que se vio usando la aplicacion y que no se veia
 * leyendo el codigo: al saltar a un resultado, el recuadro amarillo aparecia
 * descentrado, **a veces**. El "a veces" era la pista. La pagina se dibuja
 * encajada dentro de la vista, con margen a dos lados, y el margen depende de lo
 * apaisada que sea cada pagina frente a la pantalla: tomando las coordenadas de
 * la palabra como si fueran de la pantalla, con una pagina que casi llena la
 * vista casi se acertaba y con una estrecha la palabra se iba fuera.
 *
 * Por eso estas pruebas usan proporciones **distintas** a proposito. Con una
 * sola, y encima cuadrada, el fallo no aparece.
 */
class EncuadreTest {

    /** Donde acaba en la pantalla un punto de la pagina, tras el encuadre. */
    private fun enPantalla(
        estado: EstadoEncuadre,
        relativo: Offset,
        proporcion: Float,
    ): Offset {
        val anchoVista = estado.tamano.width.toFloat()
        val altoVista = estado.tamano.height.toFloat()
        val anchoPagina: Float
        val altoPagina: Float
        if (anchoVista / altoVista > proporcion) {
            altoPagina = altoVista
            anchoPagina = altoVista * proporcion
        } else {
            anchoPagina = anchoVista
            altoPagina = anchoVista / proporcion
        }
        val margenX = (anchoVista - anchoPagina) / 2f
        val margenY = (altoVista - altoPagina) / 2f

        val punto = Offset(
            margenX + relativo.x * anchoPagina,
            margenY + relativo.y * altoPagina,
        )
        val centro = Offset(anchoVista / 2f, altoVista / 2f)
        return centro + (punto - centro) * estado.escala + estado.desplazamiento
    }

    private fun estado(ancho: Int, alto: Int) = EstadoEncuadre().apply {
        tamano = IntSize(ancho, alto)
    }

    @Test
    fun `una palabra del centro queda en el centro de la pantalla`() {
        val vista = estado(1080, 2000)
        val proporcion = 0.707f // A4 vertical: sobra sitio arriba y abajo
        val centroPalabra = Offset(0.5f, 0.5f)

        vista.enfocar(centroPalabra, 0.2f, 0.02f, proporcion)

        val donde = enPantalla(vista, centroPalabra, proporcion)
        assertTrue(
            abs(donde.x - 540f) < 2f && abs(donde.y - 1000f) < 2f,
            "la palabra del centro acabo en $donde y la pantalla se centra en (540, 1000)",
        )
    }

    @Test
    fun `una palabra pegada al margen de la hoja tambien se centra`() {
        // El segundo fallo, y el que quedaba despues de arreglar el primero. El
        // tope de desplazamiento estaba medido contra la pantalla, asi que una
        // palabra del margen izquierdo pedia mas de lo que el tope daba y se
        // quedaba a un tercio de pantalla del centro. Importa mucho mas de lo
        // que parece: los margenes son donde empieza y acaba **cada linea**, o
        // sea que le pasaba a un monton de palabras.
        val vista = estado(1080, 2000)
        val proporcion = 0.707f

        for (x in listOf(0.04f, 0.10f, 0.5f, 0.90f, 0.96f)) {
            val vistaX = estado(1080, 2000)
            val palabra = Offset(x, 0.5f)
            vistaX.enfocar(palabra, 0.08f, 0.02f, proporcion)
            val donde = enPantalla(vistaX, palabra, proporcion)
            assertTrue(
                abs(donde.x - 540f) < 2f,
                "la palabra en x=$x acabo en ${donde.x} y el centro es 540",
            )
        }

        // Y lo mismo arriba y abajo del todo.
        for (y in listOf(0.03f, 0.97f)) {
            val palabra = Offset(0.5f, y)
            vista.enfocar(palabra, 0.08f, 0.02f, proporcion)
            val donde = enPantalla(vista, palabra, proporcion)
            assertTrue(
                abs(donde.y - 1000f) < 2f,
                "la palabra en y=$y acabo en ${donde.y} y el centro es 1000",
            )
        }
    }

    @Test
    fun `el papel no se empuja mas alla del centro de la vista`() {
        // La contrapartida del tope nuevo: se permite centrar cualquier punto
        // de la hoja, pero no llevarse la hoja entera fuera. El caso extremo es
        // justo el borde, y ahi el borde tiene que quedarse en el centro.
        val vista = estado(1080, 2000)
        val proporcion = 0.707f

        vista.enfocar(Offset(0f, 0.5f), 0.02f, 0.02f, proporcion)
        val borde = enPantalla(vista, Offset(0f, 0.5f), proporcion)
        assertTrue(
            borde.x <= 542f,
            "el borde izquierdo del papel acabo en ${borde.x}, pasado el centro",
        )
    }

    @Test
    fun `el centrado no depende de la forma de la pagina`() {
        // La comprobacion directa del fallo. La misma palabra, en el mismo sitio
        // relativo, sobre paginas de proporciones muy distintas: todas tienen
        // que acabar centradas. Con el calculo viejo, que ignoraba el margen,
        // cada una acababa en un sitio.
        //
        // La palabra va **fuera del centro en los dos ejes** a proposito. En el
        // centro exacto el fallo no se manifiesta: el margen es simetrico, asi
        // que el punto medio de la pagina y el de la pantalla coinciden y el
        // calculo equivocado acierta por casualidad. Una prueba con la palabra
        // centrada habria pasado igual de verde con el codigo roto.
        val palabra = Offset(0.35f, 0.35f)
        for (proporcion in listOf(0.5f, 0.707f, 1f, 1.41f, 2f)) {
            val vista = estado(1080, 2000)
            vista.enfocar(palabra, 0.12f, 0.02f, proporcion)
            val donde = enPantalla(vista, palabra, proporcion)
            assertTrue(
                abs(donde.x - 540f) < 2f && abs(donde.y - 1000f) < 2f,
                "con proporcion $proporcion la palabra quedo en $donde y el centro es (540, 1000)",
            )
        }
    }

    @Test
    fun `acercarse deja la palabra a un tamano legible`() {
        val vista = estado(1080, 2000)
        // Una palabra corta de cuerpo pequeno en un A4: una miseria en pantalla.
        vista.enfocar(Offset(0.5f, 0.5f), 0.12f, 0.012f, 0.707f)

        assertTrue(
            vista.escala > 1.5f,
            "se quedo en ${vista.escala}: sin acercarse no se lee nada",
        )
        assertTrue(
            vista.escala <= 4f,
            "se acerco ${vista.escala}, tanto que se pierde el contexto alrededor",
        )
    }

    @Test
    fun `sin saber el tamano de la vista no se toca nada`() {
        // Al llegar de otra pantalla la pagina aun no esta medida. Enfocar con
        // un tamano de cero daria una division por cero o un encuadre absurdo.
        val vista = EstadoEncuadre()
        vista.enfocar(Offset(0.5f, 0.5f), 0.2f, 0.02f, 0.707f)

        assertTrue(vista.escala == 1f, "se cambio la escala a ${vista.escala} sin saber el tamano")
        assertTrue(vista.desplazamiento == Offset.Zero, "se desplazo sin saber el tamano")
    }

    @Test
    fun `reiniciar deja la pagina entera otra vez`() {
        val vista = estado(1080, 2000)
        vista.enfocar(Offset(0.2f, 0.8f), 0.15f, 0.02f, 0.707f)
        vista.reiniciar()

        assertTrue(vista.escala == 1f, "la escala quedo en ${vista.escala}")
        assertTrue(vista.desplazamiento == Offset.Zero, "quedo desplazada")
    }
}
