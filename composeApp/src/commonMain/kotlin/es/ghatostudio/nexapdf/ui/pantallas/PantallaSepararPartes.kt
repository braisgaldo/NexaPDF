package es.ghatostudio.nexapdf.ui.pantallas

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.cd_pagina_numero
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.plural_paginas
import es.ghatostudio.nexapdf.resources.sep_anadir_parte
import es.ghatostudio.nexapdf.resources.sep_crear
import es.ghatostudio.nexapdf.resources.sep_desde
import es.ghatostudio.nexapdf.resources.sep_hasta
import es.ghatostudio.nexapdf.resources.sep_modo_rango
import es.ghatostudio.nexapdf.resources.sep_modo_sueltas
import es.ghatostudio.nexapdf.resources.sep_ninguna
import es.ghatostudio.nexapdf.resources.sep_nombre_parte
import es.ghatostudio.nexapdf.resources.sep_parte_n
import es.ghatostudio.nexapdf.resources.sep_quitar_parte
import es.ghatostudio.nexapdf.resources.sep_rango
import es.ghatostudio.nexapdf.resources.sep_resumen_titulo
import es.ghatostudio.nexapdf.resources.sep_resumen_vacia
import es.ghatostudio.nexapdf.resources.sep_titulo
import es.ghatostudio.nexapdf.resources.sep_toca_paginas
import es.ghatostudio.nexapdf.resources.sep_toca_sueltas
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.MiniaturaPagina
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Como se eligen las paginas de una parte.
 *
 * Son dos tareas distintas, no dos formas de hacer la misma. Partir un contrato
 * en capitulos son tramos seguidos y va por rango. Quedarse con las tres hojas
 * firmadas de un expediente de ochenta son paginas sueltas, y obligar a
 * declarar tres partes de una pagina cada una para eso seria absurdo.
 */
private enum class ModoParte { RANGO, SUELTAS }

/** Una parte de la division mientras se esta definiendo. */
private data class ParteEnCurso(
    val nombre: String,
    val modo: ModoParte = ModoParte.RANGO,
    /** Numeros de pagina, empezando en 1. */
    val paginas: Set<Int> = emptySet(),
    /** Texto de las casillas, que puede estar a medio escribir. */
    val desde: String = "",
    val hasta: String = "",
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

/** Rango cerrado a partir de dos casillas de texto, o vacio si no valen. */
private fun rangoDe(desde: String, hasta: String, total: Int): Set<Int> {
    val d = desde.toIntOrNull() ?: return emptySet()
    val h = hasta.toIntOrNull() ?: return emptySet()
    val inicio = minOf(d, h).coerceIn(1, total)
    val fin = maxOf(d, h).coerceIn(1, total)
    return (inicio..fin).toSet()
}

/**
 * Lista de paginas escrita como la escribiria una persona: "1, 4, 7-9".
 *
 * Con paginas sueltas la enumeracion cruda ("1, 4, 7, 8, 9") se vuelve
 * ilegible en cuanto pasan de diez, y el resumen esta justo para poder leerlo
 * de un vistazo antes de crear nada.
 */
private fun comoTexto(paginas: Set<Int>): String {
    if (paginas.isEmpty()) return ""
    val ordenadas = paginas.sorted()
    val tramos = mutableListOf<String>()
    var inicio = ordenadas.first()
    var previa = inicio
    for (n in ordenadas.drop(1)) {
        if (n == previa + 1) {
            previa = n
            continue
        }
        tramos += if (inicio == previa) "$inicio" else "$inicio–$previa"
        inicio = n
        previa = n
    }
    tramos += if (inicio == previa) "$inicio" else "$inicio–$previa"
    return tramos.joinToString(", ")
}

/**
 * Dividir un documento en varios ficheros, viendo las paginas.
 *
 * La version anterior era un dialogo con dos casillas de numeros por parte.
 * Funcionaba, pero exigia saberse de memoria en que pagina empieza cada
 * capitulo: para partir un documento hay que verlo. Aqui las paginas estan a la
 * vista, cada parte tiene su color, y se marcan tocandolas. Las casillas siguen
 * ahi para quien prefiera teclear o para documentos de cientos de paginas,
 * donde tocar es peor que escribir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaSepararPartes(
    ruta: String,
    paginas: List<PaginaPdf>,
    nombreBase: String,
    conResumen: Boolean,
    alConfirmar: (List<Pair<List<Int>, String>>) -> Unit,
    alCancelar: () -> Unit,
) {
    val total = paginas.size.coerceAtLeast(1)
    val partes = remember {
        mutableStateListOf(
            ParteEnCurso(
                nombre = "${nombreBase}_part-1",
                paginas = (1..total).toSet(),
                desde = "1",
                hasta = "$total",
            ),
        )
    }
    var activa by remember { mutableIntStateOf(0) }
    var resumiendo by remember { mutableStateOf(false) }

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
        if (parte.modo == ModoParte.SUELTAS) {
            val nuevas = if (numero in parte.paginas) {
                parte.paginas - numero
            } else {
                parte.paginas + numero
            }
            partes[activa] = parte.copy(paginas = nuevas)
            return
        }
        if (!esperandoFin) {
            partes[activa] = parte.copy(
                desde = "$numero",
                hasta = "$numero",
                paginas = setOf(numero),
            )
            esperandoFin = true
        } else {
            val ancla = parte.desde.toIntOrNull() ?: numero
            val d = minOf(ancla, numero)
            val h = maxOf(ancla, numero)
            partes[activa] = parte.copy(
                desde = "$d",
                hasta = "$h",
                paginas = (d..h).toSet(),
            )
            esperandoFin = false
        }
    }

    fun listasParaCrear(): List<Pair<List<Int>, String>> = partes.mapNotNull { parte ->
        val elegidas = parte.paginas.filter { it in 1..total }.sorted()
        if (elegidas.isEmpty()) return@mapNotNull null
        // El motor trabaja con indices desde cero; la pantalla, con numeros de
        // pagina desde uno, que es lo que ve el usuario.
        elegidas.map { it - 1 } to parte.nombre.ifBlank { "${nombreBase}_part" }
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
                        val listas = listasParaCrear()
                        if (listas.isEmpty()) return@ExtendedFloatingActionButton
                        if (conResumen) resumiendo = true else alConfirmar(listas)
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
                                    val ultimo = partes.lastOrNull()?.paginas?.maxOrNull() ?: 0
                                    val arranque = (ultimo + 1).coerceIn(1, total)
                                    partes += ParteEnCurso(
                                        nombre = "${nombreBase}_part-${partes.size + 1}",
                                        paginas = (arranque..total).toSet(),
                                        desde = "$arranque",
                                        hasta = "$total",
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
                                total = total,
                                sePuedeQuitar = partes.size > 1,
                                alCambiar = {
                                    partes[activa] = it
                                    esperandoFin = false
                                },
                                alQuitar = {
                                    partes.removeAt(activa)
                                    activa = activa.coerceAtMost(partes.lastIndex)
                                    esperandoFin = false
                                    renumerar()
                                },
                            )

                            Text(
                                text = stringResource(
                                    if (parte.modo == ModoParte.SUELTAS) {
                                        Res.string.sep_toca_sueltas
                                    } else {
                                        Res.string.sep_toca_paginas
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                items(total) { posicion ->
                    val numero = posicion + 1
                    val suyas = partes.indices.filter { numero in partes[it].paginas }
                    val descripcion = stringResource(Res.string.cd_pagina_numero, numero)
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
                            // pagina: arriba caian justo encima del titulo del
                            // documento, que es lo que se mira para saber por
                            // donde cortar.
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

    if (resumiendo) {
        val listas = listasParaCrear()
        AlertDialog(
            onDismissRequest = { resumiendo = false },
            title = { Text(stringResource(Res.string.sep_resumen_titulo)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    partes.forEachIndexed { indice, parte ->
                        val elegidas = parte.paginas.filter { it in 1..total }.toSet()
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 5.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(colorDeParte(indice)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = parte.nombre.ifBlank { "${nombreBase}_part" } + ".pdf",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = if (elegidas.isEmpty()) {
                                        stringResource(Res.string.sep_resumen_vacia)
                                    } else {
                                        stringResource(Res.string.sep_rango, comoTexto(elegidas)) +
                                            " · " +
                                            pluralStringResource(
                                                Res.plurals.plural_paginas,
                                                elegidas.size,
                                                elegidas.size,
                                            )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resumiendo = false
                        alConfirmar(listas)
                    },
                    enabled = listas.isNotEmpty(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.sep_crear))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { resumiendo = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
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
    total: Int,
    sePuedeQuitar: Boolean,
    alCambiar: (ParteEnCurso) -> Unit,
    alQuitar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = parte.modo == ModoParte.RANGO,
                onClick = {
                    // Al volver a rango se recompone el tramo con lo que
                    // hubiera elegido suelto, para no perder el trabajo hecho.
                    val d = parte.paginas.minOrNull()
                    val h = parte.paginas.maxOrNull()
                    alCambiar(
                        parte.copy(
                            modo = ModoParte.RANGO,
                            desde = d?.toString() ?: "",
                            hasta = h?.toString() ?: "",
                            paginas = if (d != null && h != null) (d..h).toSet() else emptySet(),
                        ),
                    )
                },
                label = { Text(stringResource(Res.string.sep_modo_rango), maxLines = 1) },
                modifier = Modifier.heightIn(min = 44.dp),
            )
            FilterChip(
                selected = parte.modo == ModoParte.SUELTAS,
                onClick = { alCambiar(parte.copy(modo = ModoParte.SUELTAS)) },
                label = { Text(stringResource(Res.string.sep_modo_sueltas), maxLines = 1) },
                modifier = Modifier.heightIn(min = 44.dp),
            )
            Spacer(Modifier.weight(1f))
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

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = parte.nombre,
            onValueChange = { alCambiar(parte.copy(nombre = it)) },
            label = { Text(stringResource(Res.string.sep_nombre_parte)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (parte.modo == ModoParte.RANGO) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = parte.desde,
                    onValueChange = {
                        val d = it.filter(Char::isDigit)
                        alCambiar(parte.copy(desde = d, paginas = rangoDe(d, parte.hasta, total)))
                    },
                    label = { Text(stringResource(Res.string.sep_desde)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = parte.hasta,
                    onValueChange = {
                        val h = it.filter(Char::isDigit)
                        alCambiar(parte.copy(hasta = h, paginas = rangoDe(parte.desde, h, total)))
                    },
                    label = { Text(stringResource(Res.string.sep_hasta)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.plural_paginas,
                        parte.paginas.size,
                        parte.paginas.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (parte.paginas.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.sep_rango, comoTexto(parte.paginas)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (parte.modo == ModoParte.SUELTAS && parte.paginas.isNotEmpty()) {
                TextButton(
                    onClick = { alCambiar(parte.copy(paginas = emptySet())) },
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(stringResource(Res.string.sep_ninguna), maxLines = 1)
                }
            }
        }
    }
}
