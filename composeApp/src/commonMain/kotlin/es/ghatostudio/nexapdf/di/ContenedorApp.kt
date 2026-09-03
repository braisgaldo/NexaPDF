package es.ghatostudio.nexapdf.di

import androidx.compose.runtime.staticCompositionLocalOf
import es.ghatostudio.nexapdf.data.CodecCopiaSeguridad
import es.ghatostudio.nexapdf.data.RepositorioAjustes
import es.ghatostudio.nexapdf.domain.pdf.ConversorDocumentos
import es.ghatostudio.nexapdf.domain.pdf.MotorPdf
import es.ghatostudio.nexapdf.domain.plataforma.AlmacenFicheros
import es.ghatostudio.nexapdf.domain.plataforma.SelectorFicheros
import es.ghatostudio.nexapdf.domain.plataforma.ServiciosPlataforma

/**
 * Contenedor de dependencias de la aplicacion.
 *
 * No hay biblioteca de inyeccion: la app tiene seis dependencias y todas viven
 * lo que vive el proceso. Un contenedor construido a mano en cada plataforma y
 * repartido por un CompositionLocal hace el mismo trabajo sin anadir un
 * procesador de anotaciones ni tiempo de compilacion.
 */
class ContenedorApp(
    val motorPdf: MotorPdf,
    val conversor: ConversorDocumentos,
    val servicios: ServiciosPlataforma,
    val ficheros: AlmacenFicheros,
    val selector: SelectorFicheros,
    val ajustes: RepositorioAjustes,
    val codecCopias: CodecCopiaSeguridad = CodecCopiaSeguridad(),
)

val LocalContenedor = staticCompositionLocalOf<ContenedorApp> {
    error("No hay ContenedorApp disponible. Envuelve la interfaz en NexaPdfApp(contenedor).")
}
