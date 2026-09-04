package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.domain.model.RangoPaginas
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.cd_pagina_numero
import es.ghatostudio.nexapdf.resources.plural_paginas
import es.ghatostudio.nexapdf.resources.sep_anadir_parte
import es.ghatostudio.nexapdf.resources.sep_crear
import es.ghatostudio.nexapdf.resources.sep_desde
import es.ghatostudio.nexapdf.resources.sep_hasta
import es.ghatostudio.nexapdf.resources.sep_nombre_parte
import es.ghatostudio.nexapdf.resources.sep_parte_n
import es.ghatostudio.nexapdf.resources.sep_quitar_parte
import es.ghatostudio.nexapdf.resources.sep_titulo
import es.ghatostudio.nexapdf.resources.sep_toca_paginas
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.MiniaturaPagina
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Una parte de la division mientras se esta definiendo. */
private data class ParteEnCurso(
    val desde: String,
    val hasta: String,
    val nombre: String,
)

/**
 * Colores con los que se distinguen las partes.
 *
 * Tonos medios a proposito: tienen que leerse tanto sobre el papel blanco de
 * una miniatura como sobre el fondo oscuro de la pantalla, y ademas
 * distinguirse entre ellos para quien no percibe bien el rojo y el verde, asi
 * que no hay dos seguidos del mismo tono.
 */
private val COLORES_PARTE = listOf(
    Color(0xFF3D6BD6),
    Color(0xFFD9822B),
    Color(0xFF2E9E6B),
    Color(0xFFB5477F),
    Color(0xFF2C8FB0),
    Color(0xFF8A6ACF),
    Color(0xFFC0522D),
    Color(0xFF5F8F2C),
)

private fun colorDeParte(indice: Int) = COLORES_PARTE[indice % COLORES_PARTE.size]

/**
 * Dividir un documento en varios ficheros, viendo las paginas.
 *
 * La version anterior era un dialogo con dos casillas de numeros por parte.
 * Funcionaba, pero exigia saberse de memoria en que pagina empieza cada
 * capitulo: para partir un documento hay que verlo. Aqui las paginas estan a la
 * vista, cada parte tiene su color, y se marca tocando la primera pagina y
 * luego la ultima. Las casillas siguen ahi para quien prefiera teclear o para
 * documentos de cientos de paginas, donde tocar es peor que escribir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaSepararPartes(
    ruta: String,
    paginas: List<PaginaPdf>,
    nombreBase: String,
    alConfirmar: (List<Pair<RangoPaginas, String>>) -> Unit,
    alCancelar: () -> Unit,
) {
    val total = paginas.size.coerceAtLeast(1)
    val partes = remember {
        mutableStateListOf(
            ParteEnCurso(desde = "1", hasta = "$total", nombre = "${nombreBase}_part-1"),
        )
    }
    var activa by remember { mutableIntStateOf(0) }

    // Falso: el siguiente toque empieza un rango nuevo. Cierto: lo termina.
    // Es el mismo gesto que seleccionar un rango con la tecla de mayusculas, y
    // se entiende sin explicarlo porque la parte se va pintando al tocar.
    var esperandoFin by remember { mutableStateOf(false) }

    fun renumerar() {
        partes.forEachIndexed { indice, parte ->
            val propuesto = "${nombreBase}_part-${indice + 1}"
            val eraPropuesto = Regex(Regex.escape(nombreBase) + "_part-\\d+").matches(parte.nombre)
            if (eraPropuesto && parte.nombre != propuesto) {
                partes[indice] = parte.copy(nombre = propuesto)
            }
        }
    }

    fun tocarPagina(numero: Int) {
        val parte = partes.getOrNull(activa) ?: return
        if (!esperandoFin) {
            partes[activa] = parte.copy(desde = "$numero", hasta = "$numero")
            esperandoFin = true
        } else {
            val ancla = parte.desde.toIntOrNull() ?: numero
            partes[activa] = parte.copy(
                desde = "${minOf(ancla, numero)}",
                hasta = "${maxOf(ancla, numero)}",
            )
            esperandoFin = false
        }
    }

    /** Partes a las que pertenece una pagina, en numero de pagina (desde 1). */
    fun partesDe(numero: Int): List<Int> = partes.indices.filter { indice ->
        val d = partes[indice].desde.toIntOrNull() ?: return@filter false
        val h = partes[indice].hasta.toIntOrNull() ?: return@filter false
        numero in minOf(d, h)..maxOf(d, h)
    }

    BackHandler(enabled = true) { alCancelar() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                BarraSuperior(titulo = stringResource(Res.string.sep_titulo), alVolver = alCancelar)
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        val validas = partes.mapNotNull { parte ->
                            val desde = parte.desde.toIntOrNull() ?: return@mapNotNull null
                            val hasta = parte.hasta.toIntOrNull() ?: return@mapNotNull null
                            if (desde < 1 || hasta < 1 || desde > total) return@mapNotNull null
                            val rango = RangoPaginas(
                                desde = minOf(desde, hasta) - 1,
                                hasta = (maxOf(desde, hasta) - 1).coerceAtMost(total - 1),
                            )
                            rango to parte.nombre.ifBlank { "${nombreBase}_part" }
                        }
                        if (validas.isNotEmpty()) alConfirmar(validas)
                    },
                    icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                    text = { Text(stringResource(Res.string.sep_crear)) },
                )
            },
        ) { relleno ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 116.dp),
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            partes.forEachIndexed { indice, _ ->
                                FichaParte(
                                    numero = indice + 1,
                                    color = colorDeParte(indice),
                                    elegida = indice == activa,
                                    alPulsar = {
                                        activa = indice
                                        esperandoFin = false
                                    },
                                )
                            }
                            BotonNuevaParte(
                                alPulsar = {
                                    val finAnterior = partes.lastOrNull()?.hasta?.toIntOrNull() ?: 0
                                    val arranque = (finAnterior + 1).coerceIn(1, total)
                                    partes += ParteEnCurso(
                                        desde = "$arranque",
                                        hasta = "$total",
                                        nombre = "${nombreBase}_part-${partes.size + 1}",
                                    )
                                    activa = partes.lastIndex
                                    esperandoFin = false
                                },
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        partes.getOrNull(activa)?.let { parte ->
                            EditorDeParte(
                                parte = parte,
                                color = colorDeParte(activa),
                                sePuedeQuitar = partes.size > 1,
                                alCambiar = { partes[activa] = it },
                                alQuitar = {
                                    partes.removeAt(activa)
                                    activa = activa.coerceAtMost(partes.lastIndex)
                                    esperandoFin = false
                                    renumerar()
                                },
                            )
                        }

                        Text(
                            text = stringResource(Res.string.sep_toca_paginas),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }

                items(total) { posicion ->
                    val numero = posicion + 1
                    val suyas = partesDe(numero)
                    val descripcion = stringResource(Res.string.cd_pagina_numero, numero)
                    Column {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = { tocarPagina(numero) })
                                .semantics { contentDescription = descripcion },
                        ) {
                            MiniaturaPagina(
                                ruta = ruta,
                                indice = posicion,
                                proporcion = paginas.getOrNull(posicion)?.proporcion,
                                etiqueta = numero.toString(),
                            )
                            if (suyas.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .border(
                                            width = 3.dp,
                                            color = colorDeParte(suyas.first()),
                                            shape = RoundedCornerShape(10.dp),
                                        ),
                                )
                                // Abajo a la izquierda, enfrente del numero de
                                // pagina: arriba caian justo encima del titulo
                                // del documento, que es lo que se mira para
                                // saber por donde cortar.
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    suyas.take(3).forEach { indice ->
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(colorDeParte(indice)),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FichaParte(
    numero: Int,
    color: Color,
    elegida: Boolean,
    alPulsar: () -> Unit,
) {
    val etiqueta = stringResource(Res.string.sep_parte_n, numero)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (elegida) color else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(onClick = alPulsar)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!elegida) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (elegida) FontWeight.Bold else FontWeight.Normal,
            color = if (elegida) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BotonNuevaParte(alPulsar: () -> Unit) {
    val etiqueta = stringResource(Res.string.sep_anadir_parte)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = alPulsar)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = etiqueta },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EditorDeParte(
    parte: ParteEnCurso,
    color: Color,
    sePuedeQuitar: Boolean,
    alCambiar: (ParteEnCurso) -> Unit,
    alQuitar: () -> Unit,
) {
    val desde = parte.desde.toIntOrNull()
    val hasta = parte.hasta.toIntOrNull()
    val cuantas = if (desde != null && hasta != null) {
        (maxOf(desde, hasta) - minOf(desde, hasta) + 1).coerceAtLeast(0)
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        OutlinedTextField(
            value = parte.nombre,
            onValueChange = { alCambiar(parte.copy(nombre = it)) },
            label = { Text(stringResource(Res.string.sep_nombre_parte)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = parte.desde,
                onValueChange = { alCambiar(parte.copy(desde = it.filter(Char::isDigit))) },
                label = { Text(stringResource(Res.string.sep_desde)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = parte.hasta,
                onValueChange = { alCambiar(parte.copy(hasta = it.filter(Char::isDigit))) },
                label = { Text(stringResource(Res.string.sep_hasta)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = alQuitar,
                enabled = sePuedeQuitar,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(Res.string.sep_quitar_parte),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(
                text = pluralStringResource(Res.plurals.plural_paginas, cuantas, cuantas),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
