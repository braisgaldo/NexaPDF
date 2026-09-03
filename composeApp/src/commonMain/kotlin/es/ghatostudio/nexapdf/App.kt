package es.ghatostudio.nexapdf

import androidx.compose.runtime.Composable
import es.ghatostudio.nexapdf.di.ContenedorApp
import es.ghatostudio.nexapdf.ui.NexaPdfApp

/**
 * Punto de entrada de la interfaz compartida.
 *
 * Cada plataforma construye su contenedor con sus propias implementaciones y
 * llama aqui. No hay `expect`/`actual`: el contenedor es una clase normal con
 * interfaces dentro, que es mas facil de sustituir en pruebas.
 */
@Composable
fun App(contenedor: ContenedorApp, documentoDeEntrada: String? = null) {
    NexaPdfApp(contenedor, documentoDeEntrada)
}
