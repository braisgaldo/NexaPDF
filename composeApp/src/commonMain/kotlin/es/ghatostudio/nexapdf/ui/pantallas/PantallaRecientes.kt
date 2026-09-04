package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.inicio_recientes
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes_ayuda
import es.ghatostudio.nexapdf.resources.rec_orden_nombre
import es.ghatostudio.nexapdf.resources.rec_orden_recientes
import es.ghatostudio.nexapdf.resources.rec_orden_tamano
import es.ghatostudio.nexapdf.resources.rec_ordenar_por
import es.ghatostudio.nexapdf.resources.rec_vista
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.EstadoVacio
import es.ghatostudio.nexapdf.ui.componentes.MiniaturaPagina
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import es.ghatostudio.nexapdf.resources.comun_aceptar
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.comun_eliminar
import es.ghatostudio.nexapdf.resources.rec_borrar
import es.ghatostudio.nexapdf.resources.rec_borrar_pregunta
import es.ghatostudio.nexapdf.resources.rec_nombre_nuevo
import es.ghatostudio.nexapdf.resources.rec_opciones
import es.ghatostudio.nexapdf.resources.rec_renombrar
import androidx.compose.material.icons.filled.Share
import es.ghatostudio.nexapdf.resources.comun_compartir

/** Como se ordenan los ficheros recientes. */
private enum class Orden(val etiqueta: StringResource) {
    RECIENTES(Res.string.rec_orden_recientes),
    NOMBRE(Res.string.rec_orden_nombre),
    TAMANO(Res.string.rec_orden_tamano),
}

/** Como se ven. */
private enum class Vista { LISTA, DETALLE, CUADRICULA }

/**
 * Los documentos que la aplicacion ha ido creando.
 *
 * Estaban al final de la pantalla de inicio, donde solo se veian si se
 * desplazaba hasta abajo. Con pantalla propia caben todos y se llega en un
 * toque; al abrir uno se va al visor, que es lo que uno quiere hacer con un
 * documento terminado.
 *
 * Tres vistas porque sirven para cosas distintas: la lista para recorrer
 * muchos, el detalle para reconocerlos por la primera pagina, y la cuadricula
 * para encontrar uno que se recuerda por su aspecto y no por su nombre.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantallaRecientes(
    recientes: List<DocumentoReciente>,
    snackbar: SnackbarHostState,
    alAbrir: (DocumentoReciente) -> Unit,
    alRenombrar: (DocumentoReciente, String) -> Unit,
    alBorrar: (DocumentoReciente) -> Unit,
    alCompartirVarios: () -> Unit,
    alVolver: () -> Unit,
) {
    // La lista crecia sin freno y desde la aplicacion no habia forma de
    // limpiarla ni de corregir un nombre: la carpeta acababa llena de
    // "documento editado editado.pdf".
    var menuDe by remember { mutableStateOf<DocumentoReciente?>(null) }
    var renombrando by remember { mutableStateOf<DocumentoReciente?>(null) }
    var borrando by remember { mutableStateOf<DocumentoReciente?>(null) }
    var orden by remember { mutableStateOf(Orden.RECIENTES) }
    var vista by remember { mutableStateOf(Vista.DETALLE) }
    var menuOrden by remember { mutableStateOf(false) }

    // La lista ya llega del mas nuevo al mas viejo, que es el orden natural de
    // la carpeta; los otros dos se calculan aqui.
    val ordenados = remember(recientes, orden) {
        when (orden) {
            Orden.RECIENTES -> recientes
            Orden.NOMBRE -> recientes.sortedBy { it.nombre.lowercase() }
            Orden.TAMANO -> recientes.sortedByDescending { it.tamanoBytes }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(
                titulo = stringResource(Res.string.inicio_recientes),
                alVolver = alVolver,
                acciones = {
                    // Elegir varios documentos para compartirlos juntos existia
                    // desde hacia tiempo, pero no habia forma de llegar. Aqui es
                    // donde estan los ficheros, asi que aqui va el boton.
                    if (recientes.isNotEmpty()) {
                        IconButton(onClick = alCompartirVarios, modifier = Modifier.size(48.dp)) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = stringResource(Res.string.comun_compartir),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            vista = Vista.entries[(vista.ordinal + 1) % Vista.entries.size]
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = when (vista) {
                                Vista.LISTA -> Icons.Filled.ViewList
                                Vista.DETALLE -> Icons.Filled.ViewAgenda
                                Vista.CUADRICULA -> Icons.Filled.GridView
                            },
                            contentDescription = stringResource(Res.string.rec_vista),
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuOrden = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(Res.string.rec_ordenar_por),
                            )
                        }
                        DropdownMenu(
                            expanded = menuOrden,
                            onDismissRequest = { menuOrden = false },
                        ) {
                            Orden.entries.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(opcion.etiqueta)) },
                                    // Los tres salian iguales, asi que abrir
                                    // el menu no servia para saber por que
                                    // estaba ordenada la lista.
                                    trailingIcon = {
                                        if (opcion == orden) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                    onClick = {
                                        orden = opcion
                                        menuOrden = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { relleno ->
        if (ordenados.isEmpty()) {
            EstadoVacio(
                icono = Icons.Filled.Description,
                titulo = stringResource(Res.string.inicio_sin_recientes),
                detalle = stringResource(Res.string.inicio_sin_recientes_ayuda),
                modifier = Modifier.padding(relleno),
            )
            return@Scaffold
        }

        when (vista) {
            Vista.DETALLE -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(ordenados, key = { it.ruta }) { documento ->
                    FilaReciente(
                        documento = documento,
                        alPulsar = { alAbrir(documento) },
                        alMantener = { menuDe = documento },
                    )
                }
            }

            Vista.LISTA -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(ordenados, key = { it.ruta }) { documento ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .combinedClickable(
                                onClick = { alAbrir(documento) },
                                onLongClick = { menuDe = documento },
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = documento.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Vista.CUADRICULA -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 132.dp),
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ordenados, key = { it.ruta }) { documento ->
                    Column(
                        modifier = Modifier.combinedClickable(
                            onClick = { alAbrir(documento) },
                            onLongClick = { menuDe = documento },
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MiniaturaPagina(
                            ruta = documento.ruta,
                            indice = 0,
                            proporcion = null,
                            anchoPx = 320,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = documento.nombre,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
menuDe?.let { documento ->
        AlertDialog(
            onDismissRequest = { menuDe = null },
            title = { Text(documento.nombre) },
            text = { Text(stringResource(Res.string.rec_opciones)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        renombrando = documento
                        menuDe = null
                    },
                ) {
                    Text(stringResource(Res.string.rec_renombrar))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        borrando = documento
                        menuDe = null
                    },
                ) {
                    Text(stringResource(Res.string.rec_borrar))
                }
            },
        )
    }

    renombrando?.let { documento ->
        var nombre by remember(documento.ruta) {
            mutableStateOf(documento.nombre.removeSuffix(".pdf"))
        }
        AlertDialog(
            onDismissRequest = { renombrando = null },
            title = { Text(stringResource(Res.string.rec_renombrar)) },
            text = {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(Res.string.rec_nombre_nuevo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alRenombrar(documento, nombre)
                        renombrando = null
                    },
                    enabled = nombre.isNotBlank(),
                ) {
                    Text(stringResource(Res.string.comun_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { renombrando = null }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }

    borrando?.let { documento ->
        AlertDialog(
            onDismissRequest = { borrando = null },
            title = { Text(stringResource(Res.string.rec_borrar)) },
            text = { Text(stringResource(Res.string.rec_borrar_pregunta, documento.nombre)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        alBorrar(documento)
                        borrando = null
                    },
                ) {
                    Text(stringResource(Res.string.comun_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { borrando = null }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }
}
