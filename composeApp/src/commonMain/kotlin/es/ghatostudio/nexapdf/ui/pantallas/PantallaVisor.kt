package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.pdf.Coincidencia
import es.ghatostudio.nexapdf.domain.pdf.FirmaExistente
import es.ghatostudio.nexapdf.domain.pdf.Seccion
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.cd_volver
import es.ghatostudio.nexapdf.resources.comun_buscar
import es.ghatostudio.nexapdf.resources.comun_cerrar
import es.ghatostudio.nexapdf.resources.ed_pagina_anterior
import es.ghatostudio.nexapdf.resources.ed_pagina_de
import es.ghatostudio.nexapdf.resources.ed_pagina_siguiente
import es.ghatostudio.nexapdf.resources.firma_existentes
import es.ghatostudio.nexapdf.resources.firma_sin_existentes
import es.ghatostudio.nexapdf.resources.visor_indice
import es.ghatostudio.nexapdf.resources.visor_pagina_numero
import es.ghatostudio.nexapdf.resources.visor_sin_indice
import es.ghatostudio.nexapdf.resources.visor_sin_resultados
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.encuadre
import es.ghatostudio.nexapdf.ui.componentes.rememberEncuadre
import org.jetbrains.compose.resources.stringResource

/** Lo que el visor necesita del resto de la aplicacion. */
class AccionesVisor(
    val alBuscar: suspend (String) -> List<Coincidencia>,
    val alIrAPagina: (Int) -> Unit,
)

/**
 * Visor de PDF: leer, buscar y moverse por el documento.
 *
 * Es la puerta de entrada natural a un PDF y por eso es la primera
 * herramienta: antes de unir, separar o firmar nada, lo normal es querer
 * mirarlo. Ensena ademas dos cosas que el resto de la aplicacion sabe y que en
 * cualquier otro visor hay que buscar a mano: el indice de secciones, si el
 * documento lo trae, y las firmas digitales que ya tenga.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVisor(
    nombreDocumento: String,
    paginaActual: Int,
    totalPaginas: Int,
    proporcion: Float,
    pagina: ImageBitmap?,
    secciones: List<Seccion>,
    firmas: List<FirmaExistente>,
    snackbar: SnackbarHostState,
    acciones: AccionesVisor,
    alVolver: () -> Unit,
) {
    var buscando by remember { mutableStateOf(false) }
    var consulta by remember { mutableStateOf("") }
    var resultados by remember { mutableStateOf<List<Coincidencia>?>(null) }
    var buscandoAhora by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    val encuadre = rememberEncuadre()

    // Cambiar de pagina con la anterior ampliada dejaria la nueva a medio ver.
    LaunchedEffect(paginaActual) { encuadre.reiniciar() }

    // La busqueda se lanza sola al dejar de teclear: obligar a pulsar una lupa
    // despues de escribir es un paso que nadie echa de menos cuando no esta.
    LaunchedEffect(consulta) {
        if (consulta.isBlank()) {
            resultados = null
            return@LaunchedEffect
        }
        buscandoAhora = true
        resultados = acciones.alBuscar(consulta)
        buscandoAhora = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (buscando) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        // Esta fila sustituye a la barra superior, que si traia
                        // el margen de la barra de estado; sin pedirlo a mano,
                        // el cuadro de busqueda se mete debajo del reloj.
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = consulta,
                        onValueChange = { consulta = it },
                        placeholder = { Text(stringResource(Res.string.comun_buscar)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { buscando = false; consulta = "" },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.comun_cerrar),
                        )
                    }
                }
            } else {
                BarraSuperior(
                    titulo = nombreDocumento,
                    alVolver = alVolver,
                    acciones = {
                        IconButton(
                            onClick = { buscando = true },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(Res.string.comun_buscar),
                            )
                        }
                        IconButton(
                            onClick = { panel = Panel.INDICE },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ListAlt,
                                contentDescription = stringResource(Res.string.visor_indice),
                            )
                        }
                        IconButton(
                            onClick = { panel = Panel.FIRMAS },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Filled.VerifiedUser,
                                contentDescription = stringResource(Res.string.firma_existentes),
                                // El icono se tinta cuando hay firmas: es la
                                // forma de que se note sin ocupar sitio.
                                tint = if (firmas.isEmpty()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    // Sin esto los botones de pagina quedan debajo de la barra
                    // de navegacion del telefono y no se pueden pulsar.
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = { acciones.alIrAPagina(paginaActual - 1) },
                    enabled = paginaActual > 0,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = stringResource(Res.string.ed_pagina_anterior),
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.ed_pagina_de,
                        paginaActual + 1,
                        totalPaginas.coerceAtLeast(1),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                IconButton(
                    onClick = { acciones.alIrAPagina(paginaActual + 1) },
                    enabled = paginaActual < totalPaginas - 1,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = stringResource(Res.string.ed_pagina_siguiente),
                    )
                }
            }
        },
    ) { relleno ->
        Box(modifier = Modifier.fillMaxSize().padding(relleno)) {
            when {
                resultados != null -> ListaResultados(
                    resultados = resultados.orEmpty(),
                    buscando = buscandoAhora,
                    alElegir = { pagina2 ->
                        acciones.alIrAPagina(pagina2)
                        buscando = false
                        consulta = ""
                    },
                )

                pagina != null -> androidx.compose.foundation.Image(
                    bitmap = pagina,
                    contentDescription = stringResource(
                        Res.string.visor_pagina_numero,
                        paginaActual + 1,
                    ),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .encuadre(encuadre)
                        .graphicsLayer {
                            scaleX = encuadre.escala
                            scaleY = encuadre.escala
                            translationX = encuadre.desplazamiento.x
                            translationY = encuadre.desplazamiento.y
                        },
                )

                else -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    panel?.let { abierto ->
        ModalBottomSheet(onDismissRequest = { panel = null }) {
            when (abierto) {
                Panel.INDICE -> PanelIndice(secciones) { destino ->
                    acciones.alIrAPagina(destino)
                    panel = null
                }

                Panel.FIRMAS -> PanelFirmas(firmas)
            }
        }
    }
}

private enum class Panel { INDICE, FIRMAS }

@Composable
private fun ListaResultados(
    resultados: List<Coincidencia>,
    buscando: Boolean,
    alElegir: (Int) -> Unit,
) {
    if (buscando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (resultados.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.visor_sin_resultados),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(resultados) { coincidencia ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { alElegir(coincidencia.pagina) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(
                        Res.string.visor_pagina_numero,
                        coincidencia.pagina + 1,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = coincidencia.fragmento,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Encabezado comun de los paneles: titulo, icono y cuenta. */
@Composable
private fun CabeceraPanel(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    cuenta: Int?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (cuenta != null && cuenta > 0) {
            Text(
                text = cuenta.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun PanelIndice(secciones: List<Seccion>, alElegir: (Int) -> Unit) {
    CabeceraPanel(
        icono = Icons.AutoMirrored.Filled.ListAlt,
        titulo = stringResource(Res.string.visor_indice),
        cuenta = secciones.size,
    )
    if (secciones.isEmpty()) {
        Text(
            text = stringResource(Res.string.visor_sin_indice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )
        Spacer(Modifier.height(24.dp))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
        items(secciones) { seccion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clickable { alElegir(seccion.pagina) }
                    // La sangria dice de un vistazo que es capitulo y que es
                    // apartado, sin necesidad de numerarlo.
                    .padding(
                        start = (20 + seccion.nivel * 16).dp,
                        end = 20.dp,
                        top = 10.dp,
                        bottom = 10.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = seccion.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${seccion.pagina + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PanelFirmas(firmas: List<FirmaExistente>) {
    CabeceraPanel(
        icono = Icons.Filled.VerifiedUser,
        titulo = stringResource(Res.string.firma_existentes),
        cuenta = firmas.size,
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (firmas.isEmpty()) {
            Text(
                text = stringResource(Res.string.firma_sin_existentes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            firmas.forEach { firma ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.shapes.medium,
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(firma.nombre, style = MaterialTheme.typography.bodyLarge)
                        firma.motivo?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
