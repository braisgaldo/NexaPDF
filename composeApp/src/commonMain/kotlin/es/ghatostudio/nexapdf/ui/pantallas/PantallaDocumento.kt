package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import es.ghatostudio.nexapdf.domain.model.DocumentoPdf
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.domain.pdf.FormatoDocumento
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.cd_pagina_numero
import es.ghatostudio.nexapdf.resources.comun_aceptar
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.comun_compartir
import es.ghatostudio.nexapdf.resources.comun_eliminar
import es.ghatostudio.nexapdf.resources.comun_quitar_seleccion
import es.ghatostudio.nexapdf.resources.comun_seleccionar_todo
import es.ghatostudio.nexapdf.resources.doc_anadir_pdf
import es.ghatostudio.nexapdf.resources.doc_aplicar_orden
import es.ghatostudio.nexapdf.resources.doc_arrastrar_ayuda
import es.ghatostudio.nexapdf.resources.doc_arrastrar_documentos
import es.ghatostudio.nexapdf.resources.doc_bajar
import es.ghatostudio.nexapdf.resources.doc_confirmar_eliminar_texto
import es.ghatostudio.nexapdf.resources.doc_confirmar_eliminar_titulo
import es.ghatostudio.nexapdf.resources.doc_contrasena
import es.ghatostudio.nexapdf.resources.doc_documento_cifrado
import es.ghatostudio.nexapdf.resources.doc_editar_pagina
import es.ghatostudio.nexapdf.resources.doc_exportar_aviso
import es.ghatostudio.nexapdf.resources.doc_exportar_como
import es.ghatostudio.nexapdf.resources.doc_extraer
import es.ghatostudio.nexapdf.resources.doc_formato_docx
import es.ghatostudio.nexapdf.resources.doc_formato_pdf
import es.ghatostudio.nexapdf.resources.doc_formato_pptx
import es.ghatostudio.nexapdf.resources.doc_formato_xlsx
import es.ghatostudio.nexapdf.resources.doc_firmar
import es.ghatostudio.nexapdf.resources.doc_girar_derecha
import es.ghatostudio.nexapdf.resources.doc_girar_izquierda
import es.ghatostudio.nexapdf.resources.doc_guardar_como
import es.ghatostudio.nexapdf.resources.doc_quitar
import es.ghatostudio.nexapdf.resources.doc_separar_una_por_fichero
import es.ghatostudio.nexapdf.resources.doc_subir
import es.ghatostudio.nexapdf.resources.doc_titulo_paginas
import es.ghatostudio.nexapdf.resources.doc_titulo_union
import es.ghatostudio.nexapdf.resources.doc_paso_documentos
import es.ghatostudio.nexapdf.resources.doc_paso_paginas
import es.ghatostudio.nexapdf.resources.doc_unir
import es.ghatostudio.nexapdf.resources.doc_unir_y_seguir
import es.ghatostudio.nexapdf.resources.plural_documentos
import es.ghatostudio.nexapdf.resources.plural_paginas
import es.ghatostudio.nexapdf.resources.plural_seleccionadas
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.MiniaturaPagina
import es.ghatostudio.nexapdf.ui.componentes.rememberReordenable
import es.ghatostudio.nexapdf.ui.componentes.reordenable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.ui.text.style.TextAlign
import es.ghatostudio.nexapdf.resources.doc_unir_vacio

/** Acciones que la pantalla ofrece al resto de la app. */
class AccionesDocumento(
    val alUnir: () -> Unit,
    val alExtraer: (List<Int>) -> Unit,
    val alSepararTodo: () -> Unit,
    val alGirar: (List<Int>, Int) -> Unit,
    val alEliminar: (List<Int>) -> Unit,
    val alReordenarPaginas: (List<Int>) -> Unit,
    val alEditarPagina: (Int) -> Unit,
    val alFirmar: () -> Unit,
    val alGuardarComo: () -> Unit,
    val alExportar: (FormatoDocumento) -> Unit,
    val alCompartir: () -> Unit,
    val alAnadirDocumento: () -> Unit,
    val alQuitarDocumento: (Int) -> Unit,
    val alMoverDocumento: (Int, Int) -> Unit,
    val alDesbloquear: (String) -> Unit,
)

@Composable
fun PantallaDocumento(
    documentos: List<DocumentoPdf>,
    paginas: List<PaginaPdf>,
    rutaActiva: String?,
    /**
     * Se llego aqui para unir. Lo dice quien navega y no se deduce del numero
     * de documentos: una union puede empezar con uno solo e ir creciendo, y
     * antes esa lista de un elemento se confundia con abrir un documento
     * suelto y se ensenaba la rejilla de paginas.
     */
    modoUnion: Boolean,
    /** Se llego a la rejilla de paginas despues de unir. */
    desdeUnion: Boolean = false,
    necesitaContrasena: Boolean,
    confirmarBorrado: Boolean,
    snackbar: SnackbarHostState,
    acciones: AccionesDocumento,
    alVolver: () -> Unit,
) {
    var seleccion by remember(rutaActiva) { mutableStateOf(emptySet<Int>()) }
    var pidiendoBorrado by remember { mutableStateOf(false) }
    var eligiendoFormato by remember { mutableStateOf(false) }

    // Orden de las paginas mientras se reordena. Se mantiene aparte de la lista
    // real: el documento no se toca hasta que el usuario aplica el cambio.
    //
    // La clave del efecto son los indices y no la lista en si: `paginas` llega
    // como lista observable, su identidad no cambia cuando se rellena, y usarla
    // como clave dejaria el orden vacio para siempre.
    val indicesOriginales = paginas.map { it.indice }
    val orden = remember(rutaActiva) { mutableStateListOf<Int>() }
    LaunchedEffect(rutaActiva, indicesOriginales) {
        orden.clear()
        orden.addAll(indicesOriginales)
    }
    val ordenCambiado = orden.isNotEmpty() && orden.toList() != indicesOriginales

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(
                titulo = if (modoUnion) {
                    stringResource(Res.string.doc_titulo_union)
                } else {
                    stringResource(Res.string.doc_titulo_paginas)
                },
                alVolver = alVolver,
                acciones = {
                    if (!modoUnion && paginas.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                seleccion = if (seleccion.size == paginas.size) {
                                    emptySet()
                                } else {
                                    paginas.indices.toSet()
                                }
                            },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SelectAll,
                                contentDescription = if (seleccion.size == paginas.size) {
                                    stringResource(Res.string.comun_quitar_seleccion)
                                } else {
                                    stringResource(Res.string.comun_seleccionar_todo)
                                },
                            )
                        }
                    }
                    if (modoUnion) {
                        IconButton(
                            onClick = acciones.alAnadirDocumento,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(Res.string.doc_anadir_pdf),
                            )
                        }
                    }
                    IconButton(onClick = acciones.alCompartir, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = stringResource(Res.string.comun_compartir),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            when {
                // Sin documentos no hay nada que unir: el boton estorba y
                // ademas prometeria algo que no puede hacer.
                modoUnion && documentos.isEmpty() -> Unit

                modoUnion -> ExtendedFloatingActionButton(
                    onClick = acciones.alUnir,
                    icon = { Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null) },
                    text = { Text(stringResource(Res.string.doc_unir_y_seguir)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                ordenCambiado -> ExtendedFloatingActionButton(
                    onClick = { acciones.alReordenarPaginas(orden.toList()) },
                    icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                    text = { Text(stringResource(Res.string.doc_aplicar_orden)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                seleccion.isNotEmpty() -> ExtendedFloatingActionButton(
                    onClick = { acciones.alExtraer(seleccion.sorted()) },
                    icon = { Icon(Icons.Filled.ContentCut, contentDescription = null) },
                    text = { Text(stringResource(Res.string.doc_extraer)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { relleno ->
        Column(modifier = Modifier.fillMaxSize().padding(relleno)) {
            if (modoUnion) {
                ListaDocumentos(
                    documentos = documentos,
                    acciones = acciones,
                    modifier = Modifier.weight(1f),
                )
            } else {
                BarraAcciones(
                    seleccion = seleccion.sorted(),
                    acciones = acciones,
                    alPedirBorrado = { pidiendoBorrado = true },
                    alPedirExportar = { eligiendoFormato = true },
                    confirmarBorrado = confirmarBorrado,
                )
                RejillaPaginas(
                    ruta = rutaActiva,
                    segundoPaso = desdeUnion,
                    orden = orden,
                    paginas = paginas,
                    seleccion = seleccion,
                    alAlternar = { indice ->
                        seleccion = if (indice in seleccion) seleccion - indice else seleccion + indice
                    },
                    alAbrirEditor = acciones.alEditarPagina,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (necesitaContrasena) {
        DialogoContrasena(alConfirmar = acciones.alDesbloquear, alCancelar = alVolver)
    }

    if (eligiendoFormato) {
        DialogoFormatoExportacion(
            alElegir = {
                eligiendoFormato = false
                acciones.alExportar(it)
            },
            alCancelar = { eligiendoFormato = false },
        )
    }

    if (pidiendoBorrado) {
        AlertDialog(
            onDismissRequest = { pidiendoBorrado = false },
            title = { Text(stringResource(Res.string.doc_confirmar_eliminar_titulo)) },
            text = { Text(stringResource(Res.string.doc_confirmar_eliminar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    pidiendoBorrado = false
                    acciones.alEliminar(seleccion.sorted())
                    seleccion = emptySet()
                }) {
                    Text(stringResource(Res.string.comun_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { pidiendoBorrado = false }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }
}

@Composable
private fun BarraAcciones(
    seleccion: List<Int>,
    acciones: AccionesDocumento,
    alPedirBorrado: () -> Unit,
    alPedirExportar: () -> Unit,
    confirmarBorrado: Boolean,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (seleccion.isNotEmpty()) {
            Text(
                text = pluralStringResource(
                    Res.plurals.plural_seleccionadas,
                    seleccion.size,
                    seleccion.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp),
            )
            Accion(Icons.Filled.RotateLeft, stringResource(Res.string.doc_girar_izquierda)) {
                acciones.alGirar(seleccion, -90)
            }
            Accion(Icons.Filled.RotateRight, stringResource(Res.string.doc_girar_derecha)) {
                acciones.alGirar(seleccion, 90)
            }
            Accion(Icons.Filled.Delete, stringResource(Res.string.comun_eliminar)) {
                if (confirmarBorrado) alPedirBorrado() else acciones.alEliminar(seleccion)
            }
        } else {
            Accion(Icons.Filled.ContentCut, stringResource(Res.string.doc_separar_una_por_fichero)) {
                acciones.alSepararTodo()
            }
            Accion(Icons.Filled.Draw, stringResource(Res.string.doc_firmar), acciones.alFirmar)
            Accion(Icons.Filled.FileDownload, stringResource(Res.string.doc_guardar_como)) {
                acciones.alGuardarComo()
            }
            Accion(Icons.Filled.SaveAlt, stringResource(Res.string.doc_exportar_como)) {
                alPedirExportar()
            }
        }
    }
}

@Composable
private fun Accion(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    alPulsar: () -> Unit,
) {
    // Tarjeta y no chip: el chip de Material es una etiqueta con borde fino,
    // pensada para filtrar, y como boton de accion se lee mal y se acierta
    // peor. Con superficie propia, icono grande y 56 dp de alto la fila pasa a
    // parecer lo que es, una barra de acciones.
    Card(
        onClick = alPulsar,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.heightIn(min = 56.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/**
 * Rejilla de paginas: seleccionar, abrir el editor y reordenar arrastrando.
 *
 * El orden se guarda en una lista aparte y no se aplica al documento hasta que
 * el usuario lo confirma, de modo que se puede probar como queda y salir sin
 * haber tocado nada.
 */
@Composable
private fun RejillaPaginas(
    ruta: String?,
    segundoPaso: Boolean,
    orden: androidx.compose.runtime.snapshots.SnapshotStateList<Int>,
    paginas: List<PaginaPdf>,
    seleccion: Set<Int>,
    alAlternar: (Int) -> Unit,
    alAbrirEditor: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ruta == null) return
    val etiquetaEditar = stringResource(Res.string.doc_editar_pagina)
    // La clave es una copia inmutable y no `paginas` en si. `paginas` es una
    // lista observable cuya identidad no cambia al rellenarse: al abrir un
    // documento la rejilla se compone antes de que termine la lectura, este
    // mapa se quedaba calculado sobre la lista vacia y ya no se recalculaba
    // nunca, asi que `porIndice[indice]` devolvia null para todas las paginas y
    // la rejilla salia en blanco sin dar ningun error. Una List normal compara
    // por contenido y el memo se rehace cuando toca.
    val porIndice = remember(paginas.toList()) { paginas.associateBy { it.indice } }

    val estadoRejilla = rememberLazyGridState()
    val reordenar = rememberReordenable(estadoRejilla) { desde, hasta ->
        if (desde in orden.indices && hasta in orden.indices) {
            orden.add(hasta, orden.removeAt(desde))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (segundoPaso) {
            Text(
                text = stringResource(Res.string.doc_paso_paginas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
        }
        Text(
            text = stringResource(Res.string.doc_arrastrar_ayuda),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 132.dp),
            state = estadoRejilla,
            modifier = Modifier.fillMaxSize().reordenable(reordenar),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(orden.size, key = { orden[it] }) { posicion ->
                val indicePagina = orden[posicion]
                val pagina = porIndice[indicePagina] ?: return@items
                val numero = posicion + 1
                val descripcion = stringResource(Res.string.cd_pagina_numero, numero)
                val arrastrada = reordenar.indiceArrastrado == posicion
                val desplazamiento = reordenar.desplazamientoDe(posicion)

                Column(
                    modifier = Modifier
                        .zIndex(if (arrastrada) 1f else 0f)
                        .graphicsLayer {
                            translationX = desplazamiento.x
                            translationY = desplazamiento.y
                            val escala = if (arrastrada) 1.06f else 1f
                            scaleX = escala
                            scaleY = escala
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .alpha(if (arrastrada) 0.85f else 1f)
                            .clickable(
                                onClick = { alAlternar(indicePagina) },
                                onClickLabel = descripcion,
                            )
                            .semantics {
                                contentDescription =
                                    if (indicePagina in seleccion) "$descripcion ✓" else descripcion
                            },
                    ) {
                        MiniaturaPagina(
                            ruta = ruta,
                            indice = indicePagina,
                            proporcion = pagina.proporcion,
                            seleccionada = indicePagina in seleccion,
                            etiqueta = numero.toString(),
                        )
                    }
                    TextButton(
                        onClick = { alAbrirEditor(indicePagina) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = etiquetaEditar,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Lista de documentos a unir: miniatura, orden por arrastre y por botones. */
@Composable
private fun ListaDocumentos(
    documentos: List<DocumentoPdf>,
    acciones: AccionesDocumento,
    modifier: Modifier = Modifier,
) {
    val estadoLista = rememberLazyListState()
    // El primer elemento de la lista es la cabecera, asi que los indices de
    // documento van desplazados una posicion respecto a los del gesto.
    val reordenar = rememberReordenable(estadoLista) { desde, hasta ->
        acciones.alMoverDocumento(desde - 1, hasta - 1)
    }

    LazyColumn(
        state = estadoLista,
        modifier = modifier.fillMaxSize().reordenable(reordenar),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        if (documentos.isNotEmpty()) item(key = "cabecera") {
            Column(modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp)) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.plural_documentos,
                        documentos.size,
                        documentos.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.doc_paso_documentos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.doc_arrastrar_documentos),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(documentos.size, key = { documentos[it].ruta }) { posicion ->
            val documento = documentos[posicion]
            val arrastrado = reordenar.indiceArrastrado == posicion + 1
            val desplazamiento = reordenar.desplazamientoDe(posicion + 1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .zIndex(if (arrastrado) 1f else 0f)
                    .graphicsLayer {
                        translationY = desplazamiento.y
                        val escala = if (arrastrado) 1.03f else 1f
                        scaleX = escala
                        scaleY = escala
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(
                        if (arrastrado) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        MaterialTheme.shapes.medium,
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniaturaPagina(
                    ruta = documento.ruta,
                    indice = 0,
                    proporcion = null,
                    anchoPx = 160,
                    modifier = Modifier.width(52.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = documento.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralStringResource(
                            Res.plurals.plural_paginas,
                            documento.numeroPaginas,
                            documento.numeroPaginas,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { acciones.alMoverDocumento(posicion, posicion - 1) },
                    enabled = posicion > 0,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.Filled.ArrowUpward,
                        contentDescription = stringResource(Res.string.doc_subir),
                    )
                }
                IconButton(
                    onClick = { acciones.alMoverDocumento(posicion, posicion + 1) },
                    enabled = posicion < documentos.lastIndex,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.Filled.ArrowDownward,
                        contentDescription = stringResource(Res.string.doc_bajar),
                    )
                }
                IconButton(
                    onClick = { acciones.alQuitarDocumento(posicion) },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(Res.string.doc_quitar),
                    )
                }
            }
        }

        item(key = "anadir") {
            BotonAnadirDocumentos(
                destacado = documentos.isEmpty(),
                alPulsar = acciones.alAnadirDocumento,
            )
        }
    }
}

/**
 * Boton de anadir documentos a la union.
 *
 * Cuando la lista esta vacia se presenta grande y centrado, porque es lo unico
 * que se puede hacer en esa pantalla y no tiene sentido esconderlo; en cuanto
 * hay documentos se encoge a un circulo discreto para no competir con la lista
 * ni con el boton de unir.
 */
@Composable
private fun BotonAnadirDocumentos(destacado: Boolean, alPulsar: () -> Unit) {
    val etiqueta = stringResource(Res.string.doc_anadir_pdf)

    if (!destacado) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            FilledTonalIconButton(
                onClick = alPulsar,
                modifier = Modifier.size(56.dp).semantics { contentDescription = etiqueta },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = etiqueta },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilledTonalIconButton(
            onClick = alPulsar,
            modifier = Modifier.size(88.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.doc_unir_vacio),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DialogoContrasena(alConfirmar: (String) -> Unit, alCancelar: () -> Unit) {
    var contrasena by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.doc_documento_cifrado)) },
        text = {
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text(stringResource(Res.string.doc_contrasena)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(contrasena) }) {
                Text(stringResource(Res.string.comun_aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text(stringResource(Res.string.comun_cancelar))
            }
        },
    )
}

/**
 * Eleccion del formato al exportar.
 *
 * El aviso de debajo no es decorativo: quien exporta a Word espera su documento
 * de vuelta tal cual, y conviene decirle antes que lo que sale es el texto y las
 * imagenes recompuestos, no el original.
 */
@Composable
private fun DialogoFormatoExportacion(
    alElegir: (FormatoDocumento) -> Unit,
    alCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.doc_exportar_como)) },
        text = {
            Column {
                FORMATOS_EXPORTACION.forEach { (formato, etiqueta) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable { alElegir(formato) }
                            .padding(vertical = 10.dp)
                            .semantics(mergeDescendants = true) { },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(etiqueta), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.doc_exportar_aviso),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = alCancelar) { Text(stringResource(Res.string.comun_cancelar)) }
        },
    )
}

private val FORMATOS_EXPORTACION = listOf(
    FormatoDocumento.PDF to Res.string.doc_formato_pdf,
    FormatoDocumento.DOCX to Res.string.doc_formato_docx,
    FormatoDocumento.XLSX to Res.string.doc_formato_xlsx,
    FormatoDocumento.PPTX to Res.string.doc_formato_pptx,
)
