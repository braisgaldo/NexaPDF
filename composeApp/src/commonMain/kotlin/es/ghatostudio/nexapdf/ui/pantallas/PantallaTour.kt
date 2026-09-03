package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.tour_1_texto
import es.ghatostudio.nexapdf.resources.tour_1_titulo
import es.ghatostudio.nexapdf.resources.tour_2_texto
import es.ghatostudio.nexapdf.resources.tour_2_titulo
import es.ghatostudio.nexapdf.resources.tour_3_texto
import es.ghatostudio.nexapdf.resources.tour_3_titulo
import es.ghatostudio.nexapdf.resources.tour_4_texto
import es.ghatostudio.nexapdf.resources.tour_4_titulo
import es.ghatostudio.nexapdf.resources.tour_empezar
import es.ghatostudio.nexapdf.resources.tour_saltar
import es.ghatostudio.nexapdf.resources.tour_siguiente
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tour guiado de bienvenida.
 *
 * Cuatro pantallas y fuera. La primera es la que de verdad importa: que la
 * aplicacion no puede subir nada a ningun sitio, que es lo que la distingue del
 * resto y lo que nadie se cree si no se lo explicas. Las otras tres cuentan
 * donde estan las herramientas, que se puede hacer sobre una pagina y donde
 * acaban los ficheros.
 *
 * Los dibujos se hacen con Canvas y no con imagenes: son cuatro trazos, pesan
 * cero, se adaptan al tema claro u oscuro y no hay que mantener un PNG por cada
 * densidad de pantalla.
 */
@Composable
fun PantallaTour(alTerminar: () -> Unit) {
    val pasos = listOf(
        PasoTour(Res.string.tour_1_titulo, Res.string.tour_1_texto, Dibujo.CANDADO),
        PasoTour(Res.string.tour_2_titulo, Res.string.tour_2_texto, Dibujo.HERRAMIENTAS),
        PasoTour(Res.string.tour_3_titulo, Res.string.tour_3_texto, Dibujo.LAPIZ),
        PasoTour(Res.string.tour_4_titulo, Res.string.tour_4_texto, Dibujo.CARPETA),
    )

    val estadoPaginas = rememberPagerState(pageCount = { pasos.size })
    val alcance = rememberCoroutineScope()
    val ultima = estadoPaginas.currentPage == pasos.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Saltar arriba y siempre visible: quien ya conoce la app no tiene por
        // que pasar cuatro pantallas para empezar a usarla.
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = alTerminar, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.tour_saltar))
            }
        }

        HorizontalPager(
            state = estadoPaginas,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { indice ->
            val paso = pasos[indice]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                IlustracionTour(
                    dibujo = paso.dibujo,
                    modifier = Modifier.size(180.dp),
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    text = stringResource(paso.titulo),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(paso.texto),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pasos.indices.forEach { indice ->
                val activo = indice == estadoPaginas.currentPage
                val ancho by animateFloatAsState(
                    targetValue = if (activo) 24f else 8f,
                    label = "ancho del punto",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(ancho.dp)
                        .clip(CircleShape)
                        .background(
                            if (activo) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }

        Button(
            onClick = {
                if (ultima) {
                    alTerminar()
                } else {
                    alcance.launch {
                        estadoPaginas.animateScrollToPage(estadoPaginas.currentPage + 1)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .heightIn(min = 52.dp),
        ) {
            Text(
                stringResource(
                    if (ultima) Res.string.tour_empezar else Res.string.tour_siguiente,
                ),
            )
        }
    }
}

private data class PasoTour(
    val titulo: StringResource,
    val texto: StringResource,
    val dibujo: Dibujo,
)

private enum class Dibujo { CANDADO, HERRAMIENTAS, LAPIZ, CARPETA }

@Composable
private fun IlustracionTour(dibujo: Dibujo, modifier: Modifier = Modifier) {
    val tinta = MaterialTheme.colorScheme.primary
    val relleno = MaterialTheme.colorScheme.primaryContainer
    val descripcion = stringResource(
        when (dibujo) {
            Dibujo.CANDADO -> Res.string.tour_1_titulo
            Dibujo.HERRAMIENTAS -> Res.string.tour_2_titulo
            Dibujo.LAPIZ -> Res.string.tour_3_titulo
            Dibujo.CARPETA -> Res.string.tour_4_titulo
        },
    )

    Canvas(modifier = modifier.semantics { contentDescription = descripcion }) {
        when (dibujo) {
            Dibujo.CANDADO -> dibujarCandado(tinta, relleno)
            Dibujo.HERRAMIENTAS -> dibujarHojas(tinta, relleno)
            Dibujo.LAPIZ -> dibujarLapiz(tinta, relleno)
            Dibujo.CARPETA -> dibujarCarpeta(tinta, relleno)
        }
    }
}

/** Un candado cerrado sobre una hoja: el documento que no sale de aqui. */
private fun DrawScope.dibujarCandado(tinta: Color, relleno: Color) {
    val u = size.minDimension / 100f
    val cuerpo = Size(52 * u, 40 * u)
    val esquina = Offset(24 * u, 46 * u)

    drawRoundRect(
        color = relleno,
        topLeft = esquina,
        size = cuerpo,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8 * u, 8 * u),
    )
    drawRoundRect(
        color = tinta,
        topLeft = esquina,
        size = cuerpo,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8 * u, 8 * u),
        style = Stroke(width = 4 * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // El arco del cierre.
    val arco = Path().apply {
        moveTo(36 * u, 46 * u)
        lineTo(36 * u, 32 * u)
        cubicTo(36 * u, 18 * u, 64 * u, 18 * u, 64 * u, 32 * u)
        lineTo(64 * u, 46 * u)
    }
    drawPath(
        path = arco,
        color = tinta,
        style = Stroke(width = 4 * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    drawCircle(color = tinta, radius = 5 * u, center = Offset(50 * u, 64 * u))
}

/** Dos hojas superpuestas: varios documentos a la vez. */
private fun DrawScope.dibujarHojas(tinta: Color, relleno: Color) {
    val u = size.minDimension / 100f

    drawRoundRect(
        color = relleno,
        topLeft = Offset(20 * u, 22 * u),
        size = Size(42 * u, 54 * u),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6 * u, 6 * u),
    )
    drawRoundRect(
        color = tinta,
        topLeft = Offset(20 * u, 22 * u),
        size = Size(42 * u, 54 * u),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6 * u, 6 * u),
        style = Stroke(width = 3.5f * u, join = StrokeJoin.Round),
    )

    drawRoundRect(
        color = relleno,
        topLeft = Offset(38 * u, 32 * u),
        size = Size(42 * u, 54 * u),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6 * u, 6 * u),
    )
    drawRoundRect(
        color = tinta,
        topLeft = Offset(38 * u, 32 * u),
        size = Size(42 * u, 54 * u),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6 * u, 6 * u),
        style = Stroke(width = 3.5f * u, join = StrokeJoin.Round),
    )

    listOf(48f, 58f, 68f).forEachIndexed { indice, y ->
        drawLine(
            color = tinta,
            start = Offset(48 * u, y * u),
            end = Offset((70 - indice * 6) * u, y * u),
            strokeWidth = 3.5f * u,
            cap = StrokeCap.Round,
        )
    }
}

/** Un lapiz trazando: el editor. */
private fun DrawScope.dibujarLapiz(tinta: Color, relleno: Color) {
    val u = size.minDimension / 100f

    val trazo = Path().apply {
        moveTo(18 * u, 74 * u)
        cubicTo(34 * u, 44 * u, 50 * u, 88 * u, 76 * u, 52 * u)
    }
    drawPath(
        path = trazo,
        color = relleno,
        style = Stroke(width = 8 * u, cap = StrokeCap.Round),
    )

    val cuerpo = Path().apply {
        moveTo(58 * u, 20 * u)
        lineTo(78 * u, 34 * u)
        lineTo(46 * u, 74 * u)
        lineTo(30 * u, 78 * u)
        lineTo(32 * u, 62 * u)
        close()
    }
    drawPath(path = cuerpo, color = relleno)
    drawPath(
        path = cuerpo,
        color = tinta,
        style = Stroke(width = 4 * u, join = StrokeJoin.Round),
    )
    drawLine(
        color = tinta,
        start = Offset(52 * u, 28 * u),
        end = Offset(72 * u, 42 * u),
        strokeWidth = 4 * u,
        cap = StrokeCap.Round,
    )
}

/** Una carpeta abierta con una flecha hacia abajo: donde acaban los ficheros. */
private fun DrawScope.dibujarCarpeta(tinta: Color, relleno: Color) {
    val u = size.minDimension / 100f

    val carpeta = Path().apply {
        moveTo(18 * u, 42 * u)
        lineTo(18 * u, 78 * u)
        lineTo(82 * u, 78 * u)
        lineTo(82 * u, 42 * u)
        close()
    }
    drawPath(path = carpeta, color = relleno)
    drawPath(
        path = carpeta,
        color = tinta,
        style = Stroke(width = 4 * u, join = StrokeJoin.Round),
    )

    // La pestana de arriba.
    drawLine(
        color = tinta,
        start = Offset(18 * u, 42 * u),
        end = Offset(44 * u, 42 * u),
        strokeWidth = 4 * u,
        cap = StrokeCap.Round,
    )

    // La flecha que baja hacia la carpeta.
    drawLine(
        color = tinta,
        start = Offset(50 * u, 14 * u),
        end = Offset(50 * u, 36 * u),
        strokeWidth = 4 * u,
        cap = StrokeCap.Round,
    )
    val punta = Path().apply {
        moveTo(40 * u, 28 * u)
        lineTo(50 * u, 38 * u)
        lineTo(60 * u, 28 * u)
    }
    drawPath(
        path = punta,
        color = tinta,
        style = Stroke(width = 4 * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
