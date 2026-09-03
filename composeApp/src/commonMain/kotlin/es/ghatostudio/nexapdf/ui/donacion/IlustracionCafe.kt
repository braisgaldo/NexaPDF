package es.ghatostudio.nexapdf.ui.donacion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.donar_taza_desc
import es.ghatostudio.nexapdf.ui.theme.LocalReducirAnimaciones
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

/**
 * Taza de cafe dibujada con Canvas.
 *
 * Va dibujada y no como imagen porque toma sus colores de los tokens del tema
 * activo: una taza en PNG se veria fuera de sitio en cuatro de los seis temas, y
 * habria que mantener seis versiones del fichero.
 *
 * El vapor ondula con un ciclo de tres segundos. Si el usuario ha pedido al
 * sistema reducir las animaciones, se dibuja quieto: una animacion infinita es
 * justo lo que molesta a quien activa esa preferencia.
 */
@Composable
fun IlustracionCafe(modifier: Modifier = Modifier, tamano: Dp = 120.dp) {
    val colores = MaterialTheme.colorScheme
    val quieto = LocalReducirAnimaciones.current
    val descripcion = stringResource(Res.string.donar_taza_desc)

    val transicion = rememberInfiniteTransition(label = "vapor")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fase",
    )

    val avance = if (quieto) 0f else fase

    Canvas(
        modifier = modifier
            .size(tamano)
            .semantics { contentDescription = descripcion },
    ) {
        dibujarVapor(avance, colores.primary)
        dibujarTaza(
            cuerpo = colores.primaryContainer,
            borde = colores.primary,
            liquido = colores.tertiary,
            platillo = colores.secondaryContainer,
        )
    }
}

private fun DrawScope.dibujarTaza(
    cuerpo: Color,
    borde: Color,
    liquido: Color,
    platillo: Color,
) {
    val ancho = size.width
    val alto = size.height
    val grosor = ancho * 0.035f

    // --- Platillo ---
    val platilloAlto = alto * 0.07f
    drawRoundedBar(
        x = ancho * 0.14f,
        y = alto * 0.84f,
        ancho = ancho * 0.72f,
        alto = platilloAlto,
        color = platillo,
    )

    // --- Asa ---
    val asa = Path().apply {
        moveTo(ancho * 0.72f, alto * 0.50f)
        cubicTo(
            ancho * 0.94f, alto * 0.48f,
            ancho * 0.94f, alto * 0.72f,
            ancho * 0.72f, alto * 0.70f,
        )
    }
    drawPath(asa, borde, style = Stroke(width = grosor * 1.6f, cap = StrokeCap.Round))

    // --- Cuerpo de la taza ---
    val cuerpoPath = Path().apply {
        moveTo(ancho * 0.24f, alto * 0.42f)
        lineTo(ancho * 0.76f, alto * 0.42f)
        lineTo(ancho * 0.70f, alto * 0.80f)
        cubicTo(
            ancho * 0.685f, alto * 0.845f,
            ancho * 0.315f, alto * 0.845f,
            ancho * 0.30f, alto * 0.80f,
        )
        close()
    }
    drawPath(cuerpoPath, cuerpo)
    drawPath(cuerpoPath, borde, style = Stroke(width = grosor))

    // --- Superficie del cafe ---
    drawOval(
        color = liquido,
        topLeft = Offset(ancho * 0.265f, alto * 0.395f),
        size = Size(ancho * 0.47f, alto * 0.055f),
    )
}

private fun DrawScope.drawRoundedBar(
    x: Float,
    y: Float,
    ancho: Float,
    alto: Float,
    color: Color,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(ancho, alto),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(alto / 2f, alto / 2f),
    )
}

/**
 * Tres hilos de vapor.
 *
 * Cada uno es una senoidal cuyo desfase avanza con el tiempo y que se desvanece
 * hacia arriba, que es lo que hace que parezca vapor y no un garabato.
 */
private fun DrawScope.dibujarVapor(avance: Float, color: Color) {
    val ancho = size.width
    val alto = size.height
    val grosor = ancho * 0.028f

    listOf(0.38f, 0.50f, 0.62f).forEachIndexed { indice, x ->
        val desfase = avance * 2f * kotlin.math.PI.toFloat() + indice * 0.9f
        val amplitud = ancho * 0.035f
        val arriba = alto * 0.06f + indice % 2 * alto * 0.03f
        val abajo = alto * 0.36f

        val camino = Path()
        val pasos = 18
        for (paso in 0..pasos) {
            val t = paso / pasos.toFloat()
            val y = abajo + (arriba - abajo) * t
            val desplazamiento = sin(desfase + t * 3.2f) * amplitud * t
            val punto = Offset(ancho * x + desplazamiento, y)
            if (paso == 0) camino.moveTo(punto.x, punto.y) else camino.lineTo(punto.x, punto.y)
        }

        drawPath(
            path = camino,
            color = color.copy(alpha = 0.30f - indice * 0.06f),
            style = Stroke(width = grosor, cap = StrokeCap.Round),
        )
    }
}
