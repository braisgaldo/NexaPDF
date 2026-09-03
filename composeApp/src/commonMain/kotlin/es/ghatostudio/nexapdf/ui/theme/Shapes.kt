package es.ghatostudio.nexapdf.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Esquinas algo mas redondeadas que las de Material por defecto: la app maneja
 * tarjetas de documento y hojas de accion, y el redondeo generoso las separa
 * mejor del fondo sin necesidad de bordes.
 */
internal val nexaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
