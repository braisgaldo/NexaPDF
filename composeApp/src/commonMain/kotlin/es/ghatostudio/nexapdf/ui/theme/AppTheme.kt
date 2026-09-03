package es.ghatostudio.nexapdf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Familias de color de NexaPDF. Cada una da un tema claro y uno oscuro, de modo
 * que el usuario dispone de seis temas mas la opcion de seguir al sistema.
 */
enum class ThemeFamily {
    INDIGO,
    BOSQUE,
    OCASO,
    GRAFITO,
    VINO,
    OCEANO,
    ;

    companion object {
        val predeterminada: ThemeFamily = INDIGO

        fun desdeClave(clave: String?): ThemeFamily =
            entries.firstOrNull { it.name == clave } ?: predeterminada
    }
}

/** Claro, oscuro o lo que diga el sistema. */
enum class ThemeMode {
    CLARO,
    OSCURO,
    SISTEMA,
    ;

    companion object {
        val predeterminado: ThemeMode = SISTEMA

        fun desdeClave(clave: String?): ThemeMode =
            entries.firstOrNull { it.name == clave } ?: predeterminado
    }
}

/** Los seis temas concretos, para poder recorrerlos en capturas y pruebas. */
data class TemaConcreto(val familia: ThemeFamily, val oscuro: Boolean)

val TEMAS_CONCRETOS: List<TemaConcreto> = ThemeFamily.entries.flatMap { familia ->
    listOf(TemaConcreto(familia, oscuro = false), TemaConcreto(familia, oscuro = true))
}

internal fun esquemaDe(familia: ThemeFamily, oscuro: Boolean): ColorScheme = when (familia) {
    ThemeFamily.GRAFITO -> if (oscuro) esquemaGrafitoOscuro else esquemaGrafitoClaro
    ThemeFamily.VINO -> if (oscuro) esquemaVinoOscuro else esquemaVinoClaro
    ThemeFamily.OCEANO -> if (oscuro) esquemaOceanoOscuro else esquemaOceanoClaro
    ThemeFamily.INDIGO -> if (oscuro) esquemaIndigoOscuro else esquemaIndigoClaro
    ThemeFamily.BOSQUE -> if (oscuro) esquemaBosqueOscuro else esquemaBosqueClaro
    ThemeFamily.OCASO -> if (oscuro) esquemaOcasoOscuro else esquemaOcasoClaro
}

/**
 * Indica si el tema activo es oscuro. Se expone aparte del [ColorScheme] porque
 * Material 3 no permite consultarlo y algunas superficies (el lienzo del editor,
 * el codigo QR) necesitan saberlo.
 */
val LocalEsTemaOscuro = staticCompositionLocalOf { false }

/**
 * Preferencia del sistema de reducir animaciones. Las animaciones decorativas
 * (el vapor de la taza, la entrada escalonada del sheet) la consultan.
 */
val LocalReducirAnimaciones = staticCompositionLocalOf { false }

@Composable
fun NexaTheme(
    familia: ThemeFamily = ThemeFamily.predeterminada,
    modo: ThemeMode = ThemeMode.predeterminado,
    reducirAnimaciones: Boolean = false,
    content: @Composable () -> Unit,
) {
    val oscuro = when (modo) {
        ThemeMode.CLARO -> false
        ThemeMode.OSCURO -> true
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
    }

    CompositionLocalProvider(
        LocalEsTemaOscuro provides oscuro,
        LocalReducirAnimaciones provides reducirAnimaciones,
    ) {
        MaterialTheme(
            colorScheme = esquemaDe(familia, oscuro),
            typography = nexaTypography(),
            shapes = nexaShapes,
            content = content,
        )
    }
}
