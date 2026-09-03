package es.ghatostudio.nexapdf

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Arranque de la version de escritorio.
 *
 * La interfaz y el dominio ya son multiplataforma; lo que falta para que esto
 * sea una aplicacion util es la implementacion de escritorio de [ContenedorApp]:
 * el motor de PDF (con PDFBox de JVM, que si esta disponible aqui), el selector
 * de ficheros con `JFileChooser` y los servicios del sistema. Ver
 * docs/adr/0003-portabilidad.md para el estado y lo que queda.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NexaPDF",
        state = rememberWindowState(size = DpSize(1100.dp, 820.dp)),
    ) {
        MaterialTheme {
            Text(
                text = "NexaPDF ${BuildInfo.VERSION_NAME}\n" +
                    "La version de escritorio necesita su propio ContenedorApp. " +
                    "Ver docs/adr/0003-portabilidad.md",
            )
        }
    }
}
