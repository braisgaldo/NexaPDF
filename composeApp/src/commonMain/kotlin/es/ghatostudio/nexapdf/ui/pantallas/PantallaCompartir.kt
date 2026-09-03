package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_compartir
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes_ayuda
import es.ghatostudio.nexapdf.resources.plural_seleccionadas
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.EstadoVacio
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Elegir que documentos compartir.
 *
 * El boton de compartir enviaba siempre el documento abierto, que sirve
 * mientras se trabaja con uno. Al terminar una sesion lo normal es querer
 * mandar varios, y hacerlo de uno en uno obliga a elegir destino tantas veces
 * como ficheros haya. Con varios marcados se empaquetan en un ZIP.
 */
@Composable
fun PantallaCompartir(
    documentos: List<DocumentoReciente>,
    snackbar: SnackbarHostState,
    alCompartir: (List<String>) -> Unit,
    alVolver: () -> Unit,
) {
    var marcados by remember { mutableStateOf(emptySet<String>()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(
                titulo = stringResource(Res.string.comun_compartir),
                alVolver = alVolver,
            )
        },
        floatingActionButton = {
            if (marcados.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { alCompartir(marcados.toList()) },
                    icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    text = {
                        Text(
                            pluralStringResource(
                                Res.plurals.plural_seleccionadas,
                                marcados.size,
                                marcados.size,
                            ),
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { relleno ->
        if (documentos.isEmpty()) {
            EstadoVacio(
                icono = Icons.Filled.Description,
                titulo = stringResource(Res.string.inicio_sin_recientes),
                detalle = stringResource(Res.string.inicio_sin_recientes_ayuda),
                modifier = Modifier.padding(relleno),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            items(documentos, key = { it.ruta }) { documento ->
                val marcado = documento.ruta in marcados
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clickable {
                            marcados = if (marcado) {
                                marcados - documento.ruta
                            } else {
                                marcados + documento.ruta
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = documento.nombre
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = marcado, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = documento.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = documento.detalle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
