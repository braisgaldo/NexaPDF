package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.pdf.FirmaExistente
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.doc_contrasena
import es.ghatostudio.nexapdf.resources.firma_aviso_legal
import es.ghatostudio.nexapdf.resources.firma_certificado
import es.ghatostudio.nexapdf.resources.firma_certificado_desc
import es.ghatostudio.nexapdf.resources.firma_cubre_todo
import es.ghatostudio.nexapdf.resources.firma_dibuja_aqui
import es.ghatostudio.nexapdf.resources.firma_elegir_certificado
import es.ghatostudio.nexapdf.resources.firma_existentes
import es.ghatostudio.nexapdf.resources.firma_firmar_ahora
import es.ghatostudio.nexapdf.resources.firma_colocar
import es.ghatostudio.nexapdf.resources.firma_limpiar
import es.ghatostudio.nexapdf.resources.firma_lugar
import es.ghatostudio.nexapdf.resources.firma_manuscrita
import es.ghatostudio.nexapdf.resources.firma_manuscrita_desc
import es.ghatostudio.nexapdf.resources.firma_motivo
import es.ghatostudio.nexapdf.resources.firma_nombre_visible
import es.ghatostudio.nexapdf.resources.firma_sin_existentes
import es.ghatostudio.nexapdf.resources.firma_titulo
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.TituloSeccion
import org.jetbrains.compose.resources.stringResource

/** Datos que la pantalla devuelve al pedir una firma con certificado. */
data class PeticionFirmaCertificado(
    val contrasena: String,
    val motivo: String,
    val lugar: String,
    val nombreVisible: String,
)

@Composable
fun PantallaFirma(
    nombreDocumento: String,
    firmasExistentes: List<FirmaExistente>,
    nombreCertificado: String?,
    nombreSugerido: String,
    snackbar: SnackbarHostState,
    alColocarManuscrita: (List<List<Punto>>) -> Unit,
    alElegirCertificado: () -> Unit,
    alFirmarConCertificado: (PeticionFirmaCertificado) -> Unit,
    alVolver: () -> Unit,
) {
    val trazos = remember { mutableStateListOf<List<Punto>>() }
    var contrasena by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var lugar by remember { mutableStateOf("") }
    var nombreVisible by remember(nombreSugerido) { mutableStateOf(nombreSugerido) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(titulo = stringResource(Res.string.firma_titulo), alVolver = alVolver)
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item {
                Text(
                    text = nombreDocumento,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            // --- Firmas ya presentes -----------------------------------------
            item { TituloSeccion(stringResource(Res.string.firma_existentes)) }
            if (firmasExistentes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.firma_sin_existentes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            } else {
                items(firmasExistentes.size) { posicion ->
                    val firma = firmasExistentes[posicion]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                            if (firma.cubreTodoElDocumento) {
                                Text(
                                    text = stringResource(Res.string.firma_cubre_todo),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // --- Firma manuscrita ---------------------------------------------
            item { TituloSeccion(stringResource(Res.string.firma_manuscrita)) }
            item {
                Text(
                    text = stringResource(Res.string.firma_manuscrita_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            item {
                LienzoFirma(
                    trazos = trazos,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = { trazos.clear() },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.firma_limpiar))
                    }
                    Button(
                        onClick = { alColocarManuscrita(trazos.toList()) },
                        enabled = trazos.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.Draw, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.firma_colocar))
                    }
                }
            }

            // --- Firma con certificado ----------------------------------------
            item { TituloSeccion(stringResource(Res.string.firma_certificado)) }
            item {
                Text(
                    text = stringResource(Res.string.firma_certificado_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            item {
                TextButton(
                    onClick = alElegirCertificado,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Filled.Badge, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(nombreCertificado ?: stringResource(Res.string.firma_elegir_certificado))
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    OutlinedTextField(
                        value = contrasena,
                        onValueChange = { contrasena = it },
                        label = { Text(stringResource(Res.string.doc_contrasena)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nombreVisible,
                        onValueChange = { nombreVisible = it },
                        label = { Text(stringResource(Res.string.firma_nombre_visible)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text(stringResource(Res.string.firma_motivo)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = lugar,
                        onValueChange = { lugar = it },
                        label = { Text(stringResource(Res.string.firma_lugar)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            alFirmarConCertificado(
                                PeticionFirmaCertificado(contrasena, motivo, lugar, nombreVisible),
                            )
                        },
                        enabled = nombreCertificado != null && contrasena.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(Res.string.firma_firmar_ahora))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(Res.string.firma_aviso_legal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Lienzo para firmar con el dedo o el lapiz.
 *
 * Guarda los trazos como puntos normalizados dentro de su propia caja, no en
 * pixeles: asi la misma firma se puede estampar a cualquier tamano en cualquier
 * pagina sin perder calidad ni deformarse.
 */
@Composable
private fun LienzoFirma(
    trazos: androidx.compose.runtime.snapshots.SnapshotStateList<List<Punto>>,
    modifier: Modifier = Modifier,
) {
    var enCurso by remember { mutableStateOf<List<Punto>>(emptyList()) }
    val etiqueta = stringResource(Res.string.firma_dibuja_aqui)
    val colorTinta = MaterialTheme.colorScheme.onSurface
    val vacio = trazos.isEmpty() && enCurso.isEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.6f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        if (vacio) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = etiqueta }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { posicion ->
                            enCurso = listOf(
                                Punto(posicion.x / size.width, posicion.y / size.height),
                            )
                        },
                        onDrag = { cambio, _ ->
                            enCurso = enCurso + Punto(
                                (cambio.position.x / size.width).coerceIn(0f, 1f),
                                (cambio.position.y / size.height).coerceIn(0f, 1f),
                            )
                            cambio.consume()
                        },
                        onDragEnd = {
                            if (enCurso.size >= 2) trazos.add(enCurso)
                            enCurso = emptyList()
                        },
                    )
                },
        ) {
            val trazo = Stroke(
                width = size.height * 0.035f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            (trazos + listOf(enCurso)).filter { it.size >= 2 }.forEach { puntos ->
                val camino = Path().apply {
                    moveTo(puntos.first().x * size.width, puntos.first().y * size.height)
                    puntos.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
                }
                drawPath(camino, colorTinta, style = trazo)
            }
        }
    }
}

/** Color de la tinta de firma dentro del PDF: negro, no el color del tema. */
@Suppress("unused")
private val TINTA_FIRMA = Color(0xFF1A1A1A)
