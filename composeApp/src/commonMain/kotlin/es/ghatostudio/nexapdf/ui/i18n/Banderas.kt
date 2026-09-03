package es.ghatostudio.nexapdf.ui.i18n

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Banderas dibujadas a mano en Compose en lugar de emojis.
 *
 * Los emojis de bandera dependen de la fuente del dispositivo: hay moviles que
 * los pintan como dos letras sueltas, y tres de los idiomas de la app (gallego,
 * catalan y euskera) ni siquiera tienen emoji, porque Unicode solo cubre paises
 * y unas pocas subdivisiones. Dibujarlas garantiza que las trece opciones se ven
 * igual en cualquier telefono.
 *
 * Los trece idiomas llevan bandera. Dos no corresponden a un unico pais y se
 * resuelven con la ensena mas habitual para representar la lengua: el ingles
 * con la del Reino Unido, de donde procede, y el arabe con la de la Liga Arabe,
 * que agrupa a los paises que lo tienen como lengua oficial y evita tener que
 * elegir uno entre veintitantos.
 *
 * Son representaciones simplificadas y decorativas: a 28 dp no caben los
 * detalles heraldicos. El nombre del idioma escrito al lado es la informacion
 * real, y la descripcion de contenido la aporta la pantalla que las usa.
 */
@Composable
fun BanderaIdioma(idioma: Idioma, modifier: Modifier = Modifier, tamano: Dp = 28.dp) {
    Canvas(
        modifier = modifier
            .size(width = tamano, height = tamano * 0.72f)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        dibujarBandera(idioma)
    }
}

private fun DrawScope.dibujarBandera(idioma: Idioma) {
    when (idioma) {
        Idioma.INGLES -> dibujarUnionJack()

        Idioma.ARABE -> dibujarLigaArabe()

        Idioma.ESPANOL -> franjasHorizontales(
            listOf(ROJO_ES to 0.25f, AMARILLO_ES to 0.5f, ROJO_ES to 0.25f),
        )

        Idioma.FRANCES -> franjasVerticales(
            listOf(AZUL_FR to 1f, Color.White to 1f, ROJO_FR to 1f),
        )

        Idioma.ALEMAN -> franjasHorizontales(
            listOf(Color.Black to 1f, ROJO_DE to 1f, AMARILLO_DE to 1f),
        )

        Idioma.ITALIANO -> franjasVerticales(
            listOf(VERDE_IT to 1f, Color.White to 1f, ROJO_IT to 1f),
        )

        Idioma.RUSO -> franjasHorizontales(
            listOf(Color.White to 1f, AZUL_RU to 1f, ROJO_RU to 1f),
        )

        Idioma.CHINO -> {
            drawRect(ROJO_CN)
            estrella(
                centro = Offset(size.width * 0.2f, size.height * 0.28f),
                radio = size.height * 0.16f,
                color = AMARILLO_CN,
            )
            listOf(0.42f to 0.12f, 0.53f to 0.24f, 0.53f to 0.42f, 0.42f to 0.54f).forEach { (x, y) ->
                estrella(
                    centro = Offset(size.width * x, size.height * y),
                    radio = size.height * 0.06f,
                    color = AMARILLO_CN,
                )
            }
        }

        Idioma.JAPONES -> {
            drawRect(Color.White)
            drawCircle(
                color = ROJO_JP,
                radius = size.height * 0.3f,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }

        Idioma.GRIEGO -> {
            // Nueve franjas y el canton con la cruz.
            val alturaFranja = size.height / 9f
            repeat(9) { indice ->
                drawRect(
                    color = if (indice % 2 == 0) AZUL_GR else Color.White,
                    topLeft = Offset(0f, indice * alturaFranja),
                    size = Size(size.width, alturaFranja),
                )
            }
            val lado = alturaFranja * 5f
            drawRect(AZUL_GR, size = Size(lado, lado))
            val grosor = alturaFranja
            drawRect(
                Color.White,
                topLeft = Offset(lado / 2f - grosor / 2f, 0f),
                size = Size(grosor, lado),
            )
            drawRect(
                Color.White,
                topLeft = Offset(0f, lado / 2f - grosor / 2f),
                size = Size(lado, grosor),
            )
        }

        Idioma.GALLEGO -> {
            // Campo blanco con la banda azul de esquina a esquina.
            drawRect(Color.White)
            drawLine(
                color = AZUL_GL,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height),
                strokeWidth = size.height * 0.22f,
                cap = StrokeCap.Butt,
            )
        }

        Idioma.CATALAN -> {
            // Senyera: cuatro palos rojos sobre oro.
            drawRect(AMARILLO_CA)
            val paso = size.height / 9f
            repeat(4) { indice ->
                drawRect(
                    color = ROJO_CA,
                    topLeft = Offset(0f, paso * (1 + indice * 2)),
                    size = Size(size.width, paso),
                )
            }
        }

        Idioma.EUSKERA -> {
            // Ikurrina: campo rojo, aspa verde y cruz blanca.
            drawRect(ROJO_EU)
            val grosor = size.height * 0.16f
            drawLine(VERDE_EU, Offset(0f, 0f), Offset(size.width, size.height), grosor)
            drawLine(VERDE_EU, Offset(size.width, 0f), Offset(0f, size.height), grosor)
            drawRect(
                Color.White,
                topLeft = Offset(size.width / 2f - grosor / 2f, 0f),
                size = Size(grosor, size.height),
            )
            drawRect(
                Color.White,
                topLeft = Offset(0f, size.height / 2f - grosor / 2f),
                size = Size(size.width, grosor),
            )
        }
    }
}

/**
 * Union Jack.
 *
 * Se dibuja por capas, en el mismo orden en que se construye la bandera real:
 * campo azul, aspa blanca de San Andres, aspa roja de San Patricio, cruz blanca
 * y cruz roja de San Jorge. El contracambio del aspa roja (que en la bandera
 * autentica va desplazado en cada cuadrante) no se reproduce: a este tamano no
 * se distingue y complicaria el dibujo sin que se note.
 */
private fun DrawScope.dibujarUnionJack() {
    val ancho = size.width
    val alto = size.height
    drawRect(AZUL_UK)

    val diagonalBlanca = alto * 0.30f
    drawLine(Color.White, Offset(0f, 0f), Offset(ancho, alto), diagonalBlanca)
    drawLine(Color.White, Offset(ancho, 0f), Offset(0f, alto), diagonalBlanca)

    val diagonalRoja = alto * 0.11f
    drawLine(ROJO_UK, Offset(0f, 0f), Offset(ancho, alto), diagonalRoja)
    drawLine(ROJO_UK, Offset(ancho, 0f), Offset(0f, alto), diagonalRoja)

    val cruzBlanca = alto * 0.34f
    drawRect(
        Color.White,
        topLeft = Offset(ancho / 2f - cruzBlanca / 2f, 0f),
        size = Size(cruzBlanca, alto),
    )
    drawRect(
        Color.White,
        topLeft = Offset(0f, alto / 2f - cruzBlanca / 2f),
        size = Size(ancho, cruzBlanca),
    )

    val cruzRoja = alto * 0.20f
    drawRect(
        ROJO_UK,
        topLeft = Offset(ancho / 2f - cruzRoja / 2f, 0f),
        size = Size(cruzRoja, alto),
    )
    drawRect(
        ROJO_UK,
        topLeft = Offset(0f, alto / 2f - cruzRoja / 2f),
        size = Size(ancho, cruzRoja),
    )
}

/**
 * Bandera de la Liga Arabe: campo verde con la corona de cadena y la media luna.
 *
 * La media luna se obtiene tapando un circulo dorado con otro del color del
 * campo, desplazado: es como se construye cualquier creciente y evita tener que
 * describir el arco a mano.
 */
private fun DrawScope.dibujarLigaArabe() {
    drawRect(VERDE_AR)

    val centro = Offset(size.width / 2f, size.height / 2f)
    val radioAnillo = min(size.width, size.height) * 0.36f

    drawCircle(
        color = DORADO_AR,
        radius = radioAnillo,
        center = centro,
        style = Stroke(width = size.height * 0.06f),
    )

    // La media luna: circulo dorado menos otro desplazado del color del campo.
    val radioLuna = radioAnillo * 0.62f
    drawCircle(DORADO_AR, radius = radioLuna, center = centro)
    drawCircle(
        color = VERDE_AR,
        radius = radioLuna * 0.92f,
        center = Offset(centro.x + radioLuna * 0.38f, centro.y),
        blendMode = BlendMode.SrcOver,
    )
}

/**
 * Icono para la opcion de seguir el idioma del sistema.
 *
 * Esa opcion no es un idioma, es "el que diga el telefono", asi que no le
 * corresponde ninguna bandera: lleva un globo terraqueo con los colores del
 * tema para que quede claro que es de otra naturaleza que las trece de abajo.
 */
@Composable
fun IconoIdiomaSistema(modifier: Modifier = Modifier, tamano: Dp = 28.dp) {
    val fondo = MaterialTheme.colorScheme.secondaryContainer
    val trazo = MaterialTheme.colorScheme.onSecondaryContainer
    Canvas(
        modifier = modifier
            .size(width = tamano, height = tamano * 0.72f)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        dibujarGlobo(fondo, trazo)
    }
}

private fun DrawScope.dibujarGlobo(fondo: Color, tinta: Color) {
    drawRect(fondo)
    val radio = min(size.width, size.height) * 0.32f
    val centro = Offset(size.width / 2f, size.height / 2f)
    val grosor = Stroke(width = size.height * 0.075f)

    drawCircle(tinta, radius = radio, center = centro, style = grosor)
    drawLine(
        tinta,
        Offset(centro.x - radio, centro.y),
        Offset(centro.x + radio, centro.y),
        grosor.width,
    )
    // Un meridiano a cada lado da la sensacion de esfera sin recargar el dibujo.
    listOf(0.55f, 1f).forEach { factor ->
        val camino = Path().apply {
            moveTo(centro.x, centro.y - radio)
            cubicTo(
                centro.x + radio * factor, centro.y - radio * 0.55f,
                centro.x + radio * factor, centro.y + radio * 0.55f,
                centro.x, centro.y + radio,
            )
            cubicTo(
                centro.x - radio * factor, centro.y + radio * 0.55f,
                centro.x - radio * factor, centro.y - radio * 0.55f,
                centro.x, centro.y - radio,
            )
        }
        drawPath(camino, tinta, style = grosor)
    }
}

private fun DrawScope.franjasHorizontales(franjas: List<Pair<Color, Float>>) {
    val total = franjas.sumOf { it.second.toDouble() }.toFloat()
    var y = 0f
    franjas.forEach { (color, peso) ->
        val alto = size.height * (peso / total)
        drawRect(color, topLeft = Offset(0f, y), size = Size(size.width, alto))
        y += alto
    }
}

private fun DrawScope.franjasVerticales(franjas: List<Pair<Color, Float>>) {
    val total = franjas.sumOf { it.second.toDouble() }.toFloat()
    var x = 0f
    franjas.forEach { (color, peso) ->
        val ancho = size.width * (peso / total)
        drawRect(color, topLeft = Offset(x, 0f), size = Size(ancho, size.height))
        x += ancho
    }
}

private fun DrawScope.estrella(centro: Offset, radio: Float, color: Color) {
    val camino = Path()
    val puntas = 5
    repeat(puntas * 2) { indice ->
        val r = if (indice % 2 == 0) radio else radio * 0.42f
        val angulo = (-90.0 + indice * 180.0 / puntas) * kotlin.math.PI / 180.0
        val x = centro.x + (r * cos(angulo)).toFloat()
        val y = centro.y + (r * sin(angulo)).toFloat()
        if (indice == 0) camino.moveTo(x, y) else camino.lineTo(x, y)
    }
    camino.close()
    drawPath(camino, color)
}

// Colores oficiales aproximados de cada bandera. Viven aqui y no en los tokens
// del tema a proposito: no son colores de la interfaz, son el contenido del
// dibujo, y cambiarlos con el tema los volveria irreconocibles.
private val ROJO_ES = Color(0xFFAA151B)
private val AMARILLO_ES = Color(0xFFF1BF00)
private val AZUL_FR = Color(0xFF000091)
private val ROJO_FR = Color(0xFFE1000F)
private val ROJO_DE = Color(0xFFDD0000)
private val AMARILLO_DE = Color(0xFFFFCE00)
private val VERDE_IT = Color(0xFF008C45)
private val ROJO_IT = Color(0xFFCD212A)
private val AZUL_RU = Color(0xFF0039A6)
private val ROJO_RU = Color(0xFFD52B1E)
private val ROJO_CN = Color(0xFFEE1C25)
private val AMARILLO_CN = Color(0xFFFFFF00)
private val ROJO_JP = Color(0xFFBC002D)
private val AZUL_GR = Color(0xFF0D5EAF)
private val AZUL_GL = Color(0xFF0066CC)
private val AMARILLO_CA = Color(0xFFFCDD09)
private val ROJO_CA = Color(0xFFDA121A)
private val ROJO_EU = Color(0xFFD52B1E)
private val VERDE_EU = Color(0xFF009B48)
private val AZUL_UK = Color(0xFF012169)
private val ROJO_UK = Color(0xFFC8102E)
private val VERDE_AR = Color(0xFF007A3D)
private val DORADO_AR = Color(0xFFE8C25A)
