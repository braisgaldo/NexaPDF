package es.ghatostudio.nexapdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle

/**
 * Tipografia del sistema con la altura de linea centrada.
 *
 * No se empaqueta ninguna fuente propia: los trece idiomas incluyen chino,
 * japones, ruso, griego y arabe, y una fuente que cubriera todos esos alfabetos
 * anadiria varios megabytes al binario. La del sistema ya los cubre y respeta el
 * tamano de letra que el usuario haya configurado, incluido el 200 %.
 */
@Composable
internal fun nexaTypography(): Typography {
    val base = MaterialTheme.typography
    return remember(base) {
        val ajuste = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        )
        fun TextStyle.centrada() = copy(lineHeightStyle = ajuste)

        Typography(
            displayLarge = base.displayLarge.centrada(),
            displayMedium = base.displayMedium.centrada(),
            displaySmall = base.displaySmall.centrada(),
            headlineLarge = base.headlineLarge.centrada(),
            headlineMedium = base.headlineMedium.centrada(),
            headlineSmall = base.headlineSmall.centrada(),
            titleLarge = base.titleLarge.centrada(),
            titleMedium = base.titleMedium.centrada(),
            titleSmall = base.titleSmall.centrada(),
            bodyLarge = base.bodyLarge.centrada(),
            bodyMedium = base.bodyMedium.centrada(),
            bodySmall = base.bodySmall.centrada(),
            labelLarge = base.labelLarge.centrada(),
            labelMedium = base.labelMedium.centrada(),
            labelSmall = base.labelSmall.centrada(),
        )
    }
}
