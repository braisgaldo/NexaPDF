package es.ghatostudio.nexapdf

import androidx.compose.runtime.Composable
import es.ghatostudio.nexapdf.di.ContenedorApp
import es.ghatostudio.nexapdf.ui.NexaPdfApp
import es.ghatostudio.nexapdf.ui.navegacion.EntregaExterna

/**
 * Punto de entrada de la interfaz compartida.
 *
 * Cada plataforma construye su contenedor con sus propias implementaciones y
 * llama aqui. No hay `expect`/`actual`: el contenedor es una clase normal con
 * interfaces dentro, que es mas facil de sustituir en pruebas.
 */
@Composable
fun App(contenedor: ContenedorApp, entrega: EntregaExterna? = null) {
    NexaPdfApp(contenedor, entrega)
}
