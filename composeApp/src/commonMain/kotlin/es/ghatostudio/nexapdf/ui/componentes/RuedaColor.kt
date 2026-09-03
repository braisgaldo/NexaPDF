package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.color_personalizado
import es.ghatostudio.nexapdf.resources.comun_aceptar
import es.ghatostudio.nexapdf.resources.comun_cancelar
import org.jetbrains.compose.resources.stringResource
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Circulo con el degradado del disco de color, para cerrar una paleta.
 *
 * Va al final de las filas de colores fijos. Una paleta de siete colores
 * cubre el noventa por ciento de los casos, pero cuando alguien necesita
 * justo el azul de su empresa no hay forma de llegar a el; con este circulo
 * la paleta deja de ser un limite y sigue sin ocupar mas sitio.
 */
@Composable
fun CirculoRuedaColor(
    tamano: androidx.compose.ui.unit.Dp = 40.dp,
    alElegir: (Long) -> Unit,
) {
    var abierta by remember { mutableStateOf(false) }
    val descripcion = stringResource(Res.string.color_personalizado)

    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    listOf(
                        Color.Red, Color.Magenta, Color.Blue, Color.Cyan,
                        Color.Green, Color.Yellow, Color.Red,
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable { abierta = true }
            .semantics { contentDescription = descripcion },
    )

    if (abierta) {
        DialogoRuedaColor(
            alCerrar = { abierta = false },
            alElegir = {
                alElegir(it)
                abierta = false
            },
        )
    }
}

@Composable
private fun DialogoRuedaColor(alCerrar: () -> Unit, alElegir: (Long) -> Unit) {
    // Se guarda el tono y la saturacion del disco por separado del brillo, que
    // va en su propia barra: un disco que ademas oscureciera hacia el centro
    // deja los colores oscuros en un punto imposible de acertar con el dedo.
    var tono by remember { mutableStateOf(0f) }
    var saturacion by remember { mutableStateOf(1f) }
    var brillo by remember { mutableStateOf(1f) }

    val color = Color.hsv(tono, saturacion, brillo)

    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(stringResource(Res.string.color_personalizado)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DiscoTonos(
                        brillo = brillo,
                        alTocar = { nuevoTono, nuevaSaturacion ->
                            tono = nuevoTono
                            saturacion = nuevaSaturacion
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
                Slider(value = brillo, onValueChange = { brillo = it }, valueRange = 0.05f..1f)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = enHexadecimal(color),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { alElegir(aArgb(color)) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.comun_aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.comun_cancelar))
            }
        },
    )
}

/** Disco de tono y saturacion: el angulo es el tono y el radio la saturacion. */
@Composable
private fun DiscoTonos(brillo: Float, alTocar: (Float, Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .pointerInput(Unit) {
                fun avisar(punto: Offset) {
                    val centro = Offset(size.width / 2f, size.height / 2f)
                    val radio = minOf(size.width, size.height) / 2f
                    val delta = punto - centro
                    val distancia = hypot(delta.x, delta.y)
                    val angulo = Math.toDegrees(atan2(delta.y, delta.x).toDouble()).toFloat()
                    alTocar(
                        (angulo + 360f) % 360f,
                        (distancia / radio).coerceIn(0f, 1f),
                    )
                }
                detectTapGestures { avisar(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures { cambio, _ ->
                    val centro = Offset(size.width / 2f, size.height / 2f)
                    val radio = minOf(size.width, size.height) / 2f
                    val delta = cambio.position - centro
                    val distancia = hypot(delta.x, delta.y)
                    val angulo = Math.toDegrees(atan2(delta.y, delta.x).toDouble()).toFloat()
                    alTocar((angulo + 360f) % 360f, (distancia / radio).coerceIn(0f, 1f))
                    cambio.consume()
                }
            },
    ) {
        // Tono alrededor y saturacion del centro hacia fuera: el degradado
        // radial blanco encima del angular da las dos cosas a la vez.
        drawCircle(
            brush = Brush.sweepGradient(
                (0..12).map { Color.hsv((it * 30f) % 360f, 1f, brillo) },
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.hsv(0f, 0f, brillo), Color.hsv(0f, 0f, brillo, 0f)),
                radius = size.minDimension / 2f,
            ),
        )
    }
}

private fun aArgb(color: Color): Long {
    val r = (color.red * 255f).roundToInt().coerceIn(0, 255).toLong()
    val g = (color.green * 255f).roundToInt().coerceIn(0, 255).toLong()
    val b = (color.blue * 255f).roundToInt().coerceIn(0, 255).toLong()
    return 0xFF000000L or (r shl 16) or (g shl 8) or b
}

private fun enHexadecimal(color: Color): String {
    val valor = aArgb(color) and 0xFFFFFFL
    return "#" + valor.toString(16).padStart(6, '0').uppercase()
}
