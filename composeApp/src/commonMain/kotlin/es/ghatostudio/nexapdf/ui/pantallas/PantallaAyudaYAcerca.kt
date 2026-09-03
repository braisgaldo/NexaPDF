package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.BuildInfo
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.acerca_codigo_fuente
import es.ghatostudio.nexapdf.resources.acerca_commit
import es.ghatostudio.nexapdf.resources.acerca_compilacion
import es.ghatostudio.nexapdf.resources.acerca_contacto
import es.ghatostudio.nexapdf.resources.acerca_fecha
import es.ghatostudio.nexapdf.resources.acerca_gratis
import es.ghatostudio.nexapdf.resources.acerca_licencia
import es.ghatostudio.nexapdf.resources.acerca_licencias_terceros
import es.ghatostudio.nexapdf.resources.acerca_plataforma
import es.ghatostudio.nexapdf.resources.acerca_privacidad
import es.ghatostudio.nexapdf.resources.acerca_titulo
import es.ghatostudio.nexapdf.resources.acerca_version
import es.ghatostudio.nexapdf.resources.app_lema
import es.ghatostudio.nexapdf.resources.ayuda_donde_c
import es.ghatostudio.nexapdf.resources.ayuda_donde_t
import es.ghatostudio.nexapdf.resources.ayuda_editar_c
import es.ghatostudio.nexapdf.resources.ayuda_editar_t
import es.ghatostudio.nexapdf.resources.ayuda_firmar_c
import es.ghatostudio.nexapdf.resources.ayuda_firmar_t
import es.ghatostudio.nexapdf.resources.ayuda_privacidad_c
import es.ghatostudio.nexapdf.resources.ayuda_privacidad_t
import es.ghatostudio.nexapdf.resources.ayuda_problemas_c
import es.ghatostudio.nexapdf.resources.ayuda_problemas_t
import es.ghatostudio.nexapdf.resources.ayuda_que_es_c
import es.ghatostudio.nexapdf.resources.ayuda_que_es_t
import es.ghatostudio.nexapdf.resources.ayuda_titulo
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.resources.tour_ver_otra_vez
import es.ghatostudio.nexapdf.ui.componentes.SeparadorSuave
import es.ghatostudio.nexapdf.ui.donacion.IlustracionCafe
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PantallaAyuda(
    snackbar: SnackbarHostState,
    alVerTour: () -> Unit,
    alVolver: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(titulo = stringResource(Res.string.ayuda_titulo), alVolver = alVolver)
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            // El tour se ve una vez y se olvida; aqui esta para volver a el.
            item {
                TextButton(
                    onClick = alVerTour,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Explore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.tour_ver_otra_vez))
                }
            }

            items(APARTADOS.size) { posicion ->
                val (titulo, cuerpo, conContacto) = APARTADOS[posicion]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .semantics(mergeDescendants = true) { },
                ) {
                    Text(
                        text = stringResource(titulo),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (conContacto) {
                            stringResource(cuerpo, BuildInfo.CONTACT_EMAIL)
                        } else {
                            stringResource(cuerpo)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (posicion < APARTADOS.lastIndex) SeparadorSuave()
            }
        }
    }
}

@Composable
fun PantallaAcercaDe(
    plataforma: String,
    snackbar: SnackbarHostState,
    alAbrirEnlace: (String) -> Unit,
    alVolver: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(titulo = stringResource(Res.string.acerca_titulo), alVolver = alVolver)
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IlustracionCafe(tamano = 92.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.app_lema),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(Res.string.acerca_gratis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            item { Dato(stringResource(Res.string.acerca_version), BuildInfo.VERSION_NAME) }
            item {
                Dato(stringResource(Res.string.acerca_compilacion), BuildInfo.VERSION_CODE.toString())
            }
            item { Dato(stringResource(Res.string.acerca_fecha), BuildInfo.BUILD_DATE) }
            item { Dato(stringResource(Res.string.acerca_commit), BuildInfo.COMMIT_HASH) }
            item { Dato(stringResource(Res.string.acerca_plataforma), plataforma) }
            item { Dato(stringResource(Res.string.acerca_licencia), LICENCIA) }

            item { SeparadorSuave(Modifier.padding(vertical = 8.dp)) }

            item {
                Enlace(
                    icono = Icons.Filled.Shield,
                    texto = stringResource(Res.string.acerca_privacidad),
                    alPulsar = { alAbrirEnlace(BuildInfo.PRIVACY_URL) },
                )
            }
            item {
                Enlace(
                    icono = Icons.Filled.Code,
                    texto = stringResource(Res.string.acerca_codigo_fuente),
                    alPulsar = { alAbrirEnlace(BuildInfo.PROJECT_URL) },
                )
            }
            item {
                Enlace(
                    icono = Icons.Filled.Mail,
                    texto = stringResource(Res.string.acerca_contacto),
                    alPulsar = { alAbrirEnlace("mailto:${BuildInfo.CONTACT_EMAIL}") },
                )
            }

            item { SeparadorSuave(Modifier.padding(vertical = 8.dp)) }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(Res.string.acerca_licencias_terceros),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    TERCEROS.forEach { (nombre, licencia) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(
                                text = nombre,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = licencia,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Enlace(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    alPulsar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = alPulsar)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}

private data class Apartado(
    val titulo: StringResource,
    val cuerpo: StringResource,
    val conContacto: Boolean = false,
)

private operator fun Apartado.component1() = titulo
private operator fun Apartado.component2() = cuerpo
private operator fun Apartado.component3() = conContacto

private val APARTADOS = listOf(
    Apartado(Res.string.ayuda_que_es_t, Res.string.ayuda_que_es_c),
    Apartado(Res.string.ayuda_privacidad_t, Res.string.ayuda_privacidad_c),
    Apartado(Res.string.ayuda_editar_t, Res.string.ayuda_editar_c),
    Apartado(Res.string.ayuda_firmar_t, Res.string.ayuda_firmar_c),
    Apartado(Res.string.ayuda_donde_t, Res.string.ayuda_donde_c),
    Apartado(Res.string.ayuda_problemas_t, Res.string.ayuda_problemas_c, conContacto = true),
)

/** Licencia de NexaPDF. No se traduce: es el identificador SPDX. */
private const val LICENCIA = "MIT"

/**
 * Bibliotecas de terceros y sus licencias.
 *
 * Se mantiene a mano y no se genera del grafo de dependencias porque la lista
 * cambia una vez cada muchos meses y un generador seria mas codigo del que
 * ahorra. Al cambiar una dependencia hay que actualizar esta lista.
 */
private val TERCEROS = listOf(
    "Kotlin, kotlinx" to "Apache-2.0",
    "Compose Multiplatform" to "Apache-2.0",
    "AndroidX" to "Apache-2.0",
    "PDFBox-Android" to "Apache-2.0",
    "BouncyCastle" to "MIT",
)
