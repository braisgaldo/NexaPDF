package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.model.Punto

/**
 * El contorno del papel dibujado encima de lo que se ve.
 *
 * Se dibuja sobre una caja que tiene **exactamente** la proporcion del
 * fotograma, asi que las coordenadas normalizadas del cuadrilatero se
 * multiplican por el tamano de la caja y ya caen donde tienen que caer. Todo el
 * cuidado que se puso en que la vista previa no recorte era para poder hacer
 * esta multiplicacion y nada mas.
 */
@Composable
fun ContornoDocumento(
    cuadro: Cuadrilatero?,
    color: Color,
    modifier: Modifier = Modifier,
    grosor: Dp = 3.dp,
) {
    if (cuadro == null) return
    androidx.compose.foundation.Canvas(modifier.clearAndSetSemantics { }) {
        val camino = caminoDe(cuadro, size)
        // Un relleno translucido y el contorno marcado, sin oscurecer el resto
        // del encuadre. Se probo con velo alrededor y estorbaba mas que ayudaba:
        // al mover el telefono, lo que hace falta ver es lo que **todavia** no
        // esta dentro del recorte, y el velo es justo lo que lo tapa.
        drawPath(camino, color = color.copy(alpha = 0.16f))
        drawPath(camino, color = color, style = Stroke(width = grosor.toPx()))
        cuadro.esquinas.forEach { punto ->
            drawCircle(
                color = color,
                radius = grosor.toPx() * 2.2f,
                center = Offset(punto.x * size.width, punto.y * size.height),
            )
        }
    }
}

/**
 * La foto con las cuatro esquinas arrastrables.
 *
 * Cuando la deteccion automatica falla —una hoja blanca sobre una mesa blanca,
 * una factura doblada— esto es la salida. Y tiene que estar aunque la deteccion
 * funcione: un recorte que no se puede corregir obliga a repetir la foto.
 */
@Composable
fun EditorCuadrilatero(
    imagen: ImageBitmap,
    cuadro: Cuadrilatero,
    alCambiar: (Cuadrilatero) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CD964),
) {
    val cambiarAhora by rememberUpdatedState(alCambiar)
    val cuadroAhora by rememberUpdatedState(cuadro)
    var arrastrando by remember { mutableStateOf(-1) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(imagen.width.toFloat() / imagen.height.toFloat())
            .background(Color.Black),
    ) {
        Image(
            bitmap = imagen,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { posicion ->
                            arrastrando = esquinaMasCercana(
                                cuadroAhora,
                                posicion,
                                size.width.toFloat(),
                                size.height.toFloat(),
                            )
                        },
                        onDragEnd = { arrastrando = -1 },
                        onDragCancel = { arrastrando = -1 },
                        onDrag = { cambio, _ ->
                            cambio.consume()
                            val indice = arrastrando
                            if (indice < 0) return@detectDragGestures
                            val posicion = cambio.position
                            cambiarAhora(
                                cuadroAhora.conEsquina(
                                    indice,
                                    Punto(
                                        posicion.x / size.width,
                                        posicion.y / size.height,
                                    ),
                                ),
                            )
                        },
                    )
                },
        ) {
            val camino = caminoDe(cuadro, size)
            drawPath(camino, color = color.copy(alpha = 0.12f))
            drawPath(camino, color = color, style = Stroke(width = 2.5.dp.toPx()))
            cuadro.esquinas.forEachIndexed { indice, punto ->
                val centro = Offset(punto.x * size.width, punto.y * size.height)
                // El circulo que se arrastra es mas grande que el que se ve: el
                // dedo tapa el punto justo cuando hay que afinarlo, y un blanco
                // de 12 dp no se acierta.
                drawCircle(Color.White, radius = RADIO_TIRADOR.toPx(), center = centro)
                drawCircle(
                    color = if (indice == arrastrando) color else color.copy(alpha = 0.85f),
                    radius = RADIO_TIRADOR.toPx() - 3.dp.toPx(),
                    center = centro,
                )
            }
        }
    }
}

private fun DrawScope.caminoDe(cuadro: Cuadrilatero, tamano: Size): Path = Path().apply {
    val puntos = cuadro.esquinas
    moveTo(puntos[0].x * tamano.width, puntos[0].y * tamano.height)
    for (indice in 1 until puntos.size) {
        lineTo(puntos[indice].x * tamano.width, puntos[indice].y * tamano.height)
    }
    close()
}

private fun esquinaMasCercana(
    cuadro: Cuadrilatero,
    posicion: Offset,
    ancho: Float,
    alto: Float,
): Int {
    var mejor = -1
    var distanciaMinima = Float.MAX_VALUE
    cuadro.esquinas.forEachIndexed { indice, punto ->
        val dx = punto.x * ancho - posicion.x
        val dy = punto.y * alto - posicion.y
        val distancia = dx * dx + dy * dy
        if (distancia < distanciaMinima) {
            distanciaMinima = distancia
            mejor = indice
        }
    }
    return mejor
}

/** Radio del tirador de esquina. Cuarenta y ocho puntos de diametro, el minimo tactil. */
private val RADIO_TIRADOR = 12.dp
