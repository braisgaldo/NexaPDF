package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.escaner.DeteccionDocumento
import es.ghatostudio.nexapdf.domain.escaner.HojaEscaneada
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.comun_eliminar
import es.ghatostudio.nexapdf.resources.ed_filtro_aclarar
import es.ghatostudio.nexapdf.resources.ed_filtro_bn
import es.ghatostudio.nexapdf.resources.ed_filtro_contraste
import es.ghatostudio.nexapdf.resources.ed_filtro_documento
import es.ghatostudio.nexapdf.resources.ed_filtro_grises
import es.ghatostudio.nexapdf.resources.ed_filtro_ninguno
import es.ghatostudio.nexapdf.resources.esc_rev_ajustar
import es.ghatostudio.nexapdf.resources.esc_rev_anadir
import es.ghatostudio.nexapdf.resources.esc_rev_aviso_borde
import es.ghatostudio.nexapdf.resources.esc_rev_borrar_detalle
import es.ghatostudio.nexapdf.resources.esc_rev_borrar_titulo
import es.ghatostudio.nexapdf.resources.esc_rev_crear
import es.ghatostudio.nexapdf.resources.esc_rev_detectar
import es.ghatostudio.nexapdf.resources.esc_rev_eliminar
import es.ghatostudio.nexapdf.resources.esc_rev_girar
import es.ghatostudio.nexapdf.resources.esc_rev_hecho
import es.ghatostudio.nexapdf.resources.esc_rev_intensidad
import es.ghatostudio.nexapdf.resources.esc_rev_mejora
import es.ghatostudio.nexapdf.resources.esc_rev_pagina
import es.ghatostudio.nexapdf.resources.esc_rev_titulo
import es.ghatostudio.nexapdf.resources.esc_rev_todo
import es.ghatostudio.nexapdf.resources.esc_rev_vacio
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.EditorCuadrilatero
import es.ghatostudio.nexapdf.ui.componentes.TituloSeccion
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Revisar lo escaneado antes de convertirlo en documento.
 *
 * Esta pantalla existe porque la deteccion automatica acierta casi siempre y
 * "casi" no es suficiente: la vez que falla, si no hay forma de corregirlo hay
 * que repetir la foto, y repetirla con la carpeta de facturas ya recogida es lo
 * que hace que alguien deje de usar un escaner. Aqui se ve cada hoja como va a
 * quedar, se mueven las esquinas, se cambia la mejora y se tira lo que sobra.
 */
@Composable
fun PantallaRevisionEscaneo(
    hojas: List<HojaEscaneada>,
    /** Vista previa de cada hoja ya enderezada y mejorada, por identificador. */
    previsualizaciones: Map<String, ImageBitmap>,
    /** Foto original de cada hoja, por ruta. Solo hace falta al ajustar bordes. */
    originales: Map<String, ImageBitmap>,
    confirmarDestructivas: Boolean,
    snackbar: SnackbarHostState,
    alCambiarHoja: (HojaEscaneada) -> Unit,
    alEliminar: (String) -> Unit,
    alAnadir: () -> Unit,
    alRedetectar: (String) -> Unit,
    alPedirOriginal: (String) -> Unit,
    alCrear: () -> Unit,
    alVolver: () -> Unit,
) {
    var ajustando by remember { mutableStateOf(false) }
    var confirmandoBorrado by remember { mutableStateOf<String?>(null) }
    val paginas = rememberPagerState(pageCount = { hojas.size })

    // Al quedarse sin hojas se vuelve solo: una pantalla de revision vacia no
    // tiene nada que revisar y el usuario se queda mirando un hueco.
    //
    // "Quedarse sin" y "no haber tenido nunca" no son lo mismo, y por eso hace
    // falta recordar si llego a haber alguna: si se entra aqui mientras las
    // hojas todavia se estan cargando, la lista esta vacia un instante y sin
    // esta distincion la pantalla se cerraba sola nada mas abrirse.
    var huboHojas by remember { mutableStateOf(false) }
    LaunchedEffect(hojas.isEmpty()) {
        if (hojas.isNotEmpty()) huboHojas = true else if (huboHojas) alVolver()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(
                titulo = stringResource(Res.string.esc_rev_titulo),
                alVolver = { if (ajustando) ajustando = false else alVolver() },
            )
        },
        floatingActionButton = {
            if (hojas.isNotEmpty() && !ajustando) {
                ExtendedFloatingActionButton(
                    onClick = alCrear,
                    icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                    text = { Text(stringResource(Res.string.esc_rev_crear)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { relleno ->
        if (hojas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.esc_rev_vacio),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        val indice = paginas.currentPage.coerceIn(0, hojas.lastIndex)
        val hoja = hojas[indice]

        Column(Modifier.fillMaxSize().padding(relleno)) {
            Text(
                text = stringResource(Res.string.esc_rev_pagina, indice + 1, hojas.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                textAlign = TextAlign.Center,
            )

            if (ajustando) {
                AjusteDeBordes(
                    hoja = hoja,
                    original = originales[hoja.rutaOriginal],
                    alPedirOriginal = alPedirOriginal,
                    alCambiar = alCambiarHoja,
                    alRedetectar = { alRedetectar(hoja.id) },
                    alTerminar = { ajustando = false },
                    modifier = Modifier.weight(1f),
                )
            } else {
                HorizontalPager(
                    state = paginas,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    pageSpacing = 12.dp,
                ) { pagina ->
                    VistaDeHoja(
                        hoja = hojas.getOrNull(pagina),
                        imagen = hojas.getOrNull(pagina)?.let { previsualizaciones[it.id] },
                    )
                }

                HerramientasDeHoja(
                    hoja = hoja,
                    alAjustar = { ajustando = true },
                    alGirar = { alCambiarHoja(hoja.girada()) },
                    alAnadir = alAnadir,
                    alEliminar = {
                        if (confirmarDestructivas) {
                            confirmandoBorrado = hoja.id
                        } else {
                            alEliminar(hoja.id)
                        }
                    },
                )

                ControlesDeMejora(hoja = hoja, alCambiar = alCambiarHoja)
                Spacer(Modifier.height(88.dp))
            }
        }
    }

    confirmandoBorrado?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmandoBorrado = null },
            title = { Text(stringResource(Res.string.esc_rev_borrar_titulo)) },
            text = { Text(stringResource(Res.string.esc_rev_borrar_detalle)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoBorrado = null
                        alEliminar(id)
                    },
                ) {
                    Text(stringResource(Res.string.comun_eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoBorrado = null }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }
}

@Composable
private fun VistaDeHoja(hoja: HojaEscaneada?, imagen: ImageBitmap?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imagen != null -> Image(
                bitmap = imagen,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(8.dp),
            )

            else -> CircularProgressIndicator()
        }

        // El aviso sale solo cuando la deteccion no se fio de si misma. Salir
        // siempre seria ruido, y no salir nunca deja pasar el recorte torcido
        // hasta el PDF.
        if (hoja != null && hoja.confianza < DeteccionDocumento.UMBRAL_AUTOMATICO && hoja.recortada) {
            Text(
                text = stringResource(Res.string.esc_rev_aviso_borde),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun AjusteDeBordes(
    hoja: HojaEscaneada,
    original: ImageBitmap?,
    alPedirOriginal: (String) -> Unit,
    alCambiar: (HojaEscaneada) -> Unit,
    alRedetectar: () -> Unit,
    alTerminar: () -> Unit,
    modifier: Modifier,
) {
    LaunchedEffect(hoja.rutaOriginal) { alPedirOriginal(hoja.rutaOriginal) }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            if (original == null) {
                CircularProgressIndicator()
            } else {
                EditorCuadrilatero(
                    imagen = original,
                    cuadro = hoja.cuadro,
                    alCambiar = { alCambiar(hoja.copy(cuadro = it)) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            AssistChip(
                onClick = { alCambiar(hoja.copy(cuadro = Cuadrilatero.COMPLETO)) },
                label = { Text(stringResource(Res.string.esc_rev_todo)) },
                leadingIcon = { Icon(Icons.Filled.CropFree, contentDescription = null) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            AssistChip(
                onClick = alRedetectar,
                label = { Text(stringResource(Res.string.esc_rev_detectar)) },
                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
            AssistChip(
                onClick = alTerminar,
                label = { Text(stringResource(Res.string.esc_rev_hecho)) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun HerramientasDeHoja(
    hoja: HojaEscaneada,
    alAjustar: () -> Unit,
    alGirar: () -> Unit,
    alAnadir: () -> Unit,
    alEliminar: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonHerramienta(Icons.Filled.Crop, Res.string.esc_rev_ajustar, alAjustar)
        BotonHerramienta(Icons.AutoMirrored.Filled.RotateRight, Res.string.esc_rev_girar, alGirar)
        BotonHerramienta(Icons.Filled.AddAPhoto, Res.string.esc_rev_anadir, alAnadir)
        BotonHerramienta(Icons.Filled.Delete, Res.string.esc_rev_eliminar, alEliminar)
    }
}

@Composable
private fun BotonHerramienta(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: StringResource,
    alPulsar: () -> Unit,
) {
    val texto = stringResource(etiqueta)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = alPulsar, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(icono, contentDescription = texto, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ControlesDeMejora(hoja: HojaEscaneada, alCambiar: (HojaEscaneada) -> Unit) {
    TituloSeccion(stringResource(Res.string.esc_rev_mejora))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FILTROS.forEach { (filtro, etiqueta) ->
            FilterChip(
                selected = hoja.filtro == filtro,
                onClick = { alCambiar(hoja.copy(filtro = filtro)) },
                label = { Text(stringResource(etiqueta), maxLines = 1) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }

    if (hoja.filtro != FiltroPagina.NINGUNO) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.esc_rev_intensidad),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Slider(
                value = hoja.intensidadFiltro,
                onValueChange = { alCambiar(hoja.copy(intensidadFiltro = it)) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Los filtros que tienen sentido en un escaneo.
 *
 * Falta "invertir" a proposito: da un negativo, que es util para retocar una
 * imagen y no tiene ningun uso al digitalizar un papel.
 */
private val FILTROS = listOf(
    FiltroPagina.DOCUMENTO_NITIDO to Res.string.ed_filtro_documento,
    FiltroPagina.NINGUNO to Res.string.ed_filtro_ninguno,
    FiltroPagina.ESCALA_DE_GRISES to Res.string.ed_filtro_grises,
    FiltroPagina.BLANCO_Y_NEGRO to Res.string.ed_filtro_bn,
    FiltroPagina.ALTO_CONTRASTE to Res.string.ed_filtro_contraste,
    FiltroPagina.ACLARAR to Res.string.ed_filtro_aclarar,
)
