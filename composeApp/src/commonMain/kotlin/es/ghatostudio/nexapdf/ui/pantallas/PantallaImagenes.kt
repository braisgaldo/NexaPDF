package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.model.DisposicionImagenes
import es.ghatostudio.nexapdf.domain.model.Orientacion
import es.ghatostudio.nexapdf.domain.model.TamanoPagina
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_eliminar
import es.ghatostudio.nexapdf.resources.img_ajustar_a_imagen
import es.ghatostudio.nexapdf.resources.img_anadir_mas
import es.ghatostudio.nexapdf.resources.img_automatica
import es.ghatostudio.nexapdf.resources.img_crear
import es.ghatostudio.nexapdf.resources.img_cuatro_por_pagina
import es.ghatostudio.nexapdf.resources.img_disposicion
import es.ghatostudio.nexapdf.resources.img_dos_por_pagina
import es.ghatostudio.nexapdf.resources.img_horizontal
import es.ghatostudio.nexapdf.resources.img_margen
import es.ghatostudio.nexapdf.resources.img_orientacion
import es.ghatostudio.nexapdf.resources.img_seis_por_pagina
import es.ghatostudio.nexapdf.resources.img_sin_imagenes
import es.ghatostudio.nexapdf.resources.img_tamano_a3
import es.ghatostudio.nexapdf.resources.img_tamano_a4
import es.ghatostudio.nexapdf.resources.img_tamano_a5
import es.ghatostudio.nexapdf.resources.img_tamano_carta
import es.ghatostudio.nexapdf.resources.img_tamano_pagina
import es.ghatostudio.nexapdf.resources.img_titulo
import es.ghatostudio.nexapdf.resources.img_una_por_pagina
import es.ghatostudio.nexapdf.resources.img_vertical
import es.ghatostudio.nexapdf.resources.plural_imagenes
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.EstadoVacio
import es.ghatostudio.nexapdf.ui.componentes.TituloSeccion
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Opciones elegidas para convertir las imagenes en un PDF. */
data class OpcionesImagenes(
    val disposicion: DisposicionImagenes = DisposicionImagenes.UNA_POR_PAGINA,
    val tamano: TamanoPagina = TamanoPagina.AJUSTAR_A_IMAGEN,
    val orientacion: Orientacion = Orientacion.AUTOMATICA,
    val margenPt: Float = 24f,
)

@Composable
fun PantallaImagenes(
    imagenes: List<String>,
    miniaturas: Map<String, ImageBitmap>,
    snackbar: SnackbarHostState,
    alQuitar: (String) -> Unit,
    alAnadir: () -> Unit,
    alCrear: (OpcionesImagenes) -> Unit,
    alVolver: () -> Unit,
) {
    var opciones by remember { mutableStateOf(OpcionesImagenes()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(titulo = stringResource(Res.string.img_titulo), alVolver = alVolver)
        },
        floatingActionButton = {
            if (imagenes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { alCrear(opciones) },
                    icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                    text = { Text(stringResource(Res.string.img_crear)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { relleno ->
        if (imagenes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    icono = Icons.Filled.PhotoLibrary,
                    titulo = stringResource(Res.string.img_sin_imagenes),
                    detalle = stringResource(Res.string.img_anadir_mas),
                    accion = {
                        TextButton(onClick = alAnadir, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(Res.string.img_anadir_mas))
                        }
                    },
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    text = pluralStringResource(
                        Res.plurals.plural_imagenes,
                        imagenes.size,
                        imagenes.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp),
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Alto acotado: una rejilla dentro de una lista necesita
                        // un limite, y cuatro filas cubren el caso habitual.
                        .heightIn(max = 460.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(imagenes, key = { _, ruta -> ruta }) { _, ruta ->
                        Box {
                            val mapa = miniaturas[ruta]
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            ) {
                                if (mapa != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = mapa,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            IconButton(
                                onClick = { alQuitar(ruta) },
                                modifier = Modifier.align(Alignment.TopEnd).size(48.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(Res.string.comun_eliminar),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = alAnadir,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(Res.string.img_anadir_mas))
                }
            }

            item { TituloSeccion(stringResource(Res.string.img_disposicion)) }
            item {
                FilaOpciones(DISPOSICIONES, opciones.disposicion) {
                    opciones = opciones.copy(disposicion = it)
                }
            }

            item { TituloSeccion(stringResource(Res.string.img_tamano_pagina)) }
            item {
                FilaOpciones(TAMANOS, opciones.tamano) { opciones = opciones.copy(tamano = it) }
            }

            item { TituloSeccion(stringResource(Res.string.img_orientacion)) }
            item {
                FilaOpciones(ORIENTACIONES, opciones.orientacion) {
                    opciones = opciones.copy(orientacion = it)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.img_margen),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(16.dp))
                    Slider(
                        value = opciones.margenPt,
                        onValueChange = { opciones = opciones.copy(margenPt = it) },
                        valueRange = 0f..72f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> FilaOpciones(
    opciones: List<Pair<T, StringResource>>,
    elegida: T,
    alElegir: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opciones.forEach { (valor, etiqueta) ->
            FilterChip(
                selected = valor == elegida,
                onClick = { alElegir(valor) },
                label = { Text(stringResource(etiqueta), maxLines = 1) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

private val DISPOSICIONES = listOf(
    DisposicionImagenes.UNA_POR_PAGINA to Res.string.img_una_por_pagina,
    DisposicionImagenes.DOS_POR_PAGINA to Res.string.img_dos_por_pagina,
    DisposicionImagenes.CUATRO_POR_PAGINA to Res.string.img_cuatro_por_pagina,
    DisposicionImagenes.SEIS_POR_PAGINA to Res.string.img_seis_por_pagina,
)

private val TAMANOS = listOf(
    TamanoPagina.AJUSTAR_A_IMAGEN to Res.string.img_ajustar_a_imagen,
    TamanoPagina.A4 to Res.string.img_tamano_a4,
    TamanoPagina.CARTA to Res.string.img_tamano_carta,
    TamanoPagina.A5 to Res.string.img_tamano_a5,
    TamanoPagina.A3 to Res.string.img_tamano_a3,
)

private val ORIENTACIONES = listOf(
    Orientacion.AUTOMATICA to Res.string.img_automatica,
    Orientacion.VERTICAL to Res.string.img_vertical,
    Orientacion.HORIZONTAL to Res.string.img_horizontal,
)
