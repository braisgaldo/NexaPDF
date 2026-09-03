package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.model.BloqueTexto
import es.ghatostudio.nexapdf.domain.model.Edicion
import es.ghatostudio.nexapdf.domain.model.FiltroPagina
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import es.ghatostudio.nexapdf.domain.model.TipoFigura
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_aceptar
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.comun_deshacer
import es.ghatostudio.nexapdf.resources.comun_guardar
import es.ghatostudio.nexapdf.resources.comun_rehacer
import es.ghatostudio.nexapdf.resources.ed_anadir_texto
import es.ghatostudio.nexapdf.resources.ed_borrar
import es.ghatostudio.nexapdf.resources.ed_color
import es.ghatostudio.nexapdf.resources.ed_descartar_texto
import es.ghatostudio.nexapdf.resources.ed_descartar_titulo
import es.ghatostudio.nexapdf.resources.ed_dibujar
import es.ghatostudio.nexapdf.resources.ed_elipse
import es.ghatostudio.nexapdf.resources.ed_figuras
import es.ghatostudio.nexapdf.resources.ed_filtro
import es.ghatostudio.nexapdf.resources.ed_filtro_aclarar
import es.ghatostudio.nexapdf.resources.ed_filtro_aviso
import es.ghatostudio.nexapdf.resources.ed_filtro_bn
import es.ghatostudio.nexapdf.resources.ed_filtro_contraste
import es.ghatostudio.nexapdf.resources.ed_filtro_documento
import es.ghatostudio.nexapdf.resources.ed_filtro_grises
import es.ghatostudio.nexapdf.resources.ed_filtro_invertir
import es.ghatostudio.nexapdf.resources.ed_filtro_ninguno
import es.ghatostudio.nexapdf.resources.ed_firma
import es.ghatostudio.nexapdf.resources.ed_flecha
import es.ghatostudio.nexapdf.resources.ed_grosor
import es.ghatostudio.nexapdf.resources.ed_imagen
import es.ghatostudio.nexapdf.resources.ed_intensidad
import es.ghatostudio.nexapdf.resources.ed_linea
import es.ghatostudio.nexapdf.resources.ed_mover
import es.ghatostudio.nexapdf.resources.ed_pagina_de
import es.ghatostudio.nexapdf.resources.ed_rectangulo
import es.ghatostudio.nexapdf.resources.ed_relleno
import es.ghatostudio.nexapdf.resources.ed_resaltar
import es.ghatostudio.nexapdf.resources.ed_sustituir_aviso
import es.ghatostudio.nexapdf.resources.ed_sustituir_texto
import es.ghatostudio.nexapdf.resources.ed_texto
import es.ghatostudio.nexapdf.resources.ed_tocar_para_sustituir
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Lo que el editor necesita del resto de la aplicacion. */
class AccionesEditor(
    val alGuardar: (es.ghatostudio.nexapdf.domain.model.EdicionPagina) -> Unit,
    val alElegirImagen: (aplicar: (String) -> Unit) -> Unit,
    val alPedirFirma: (aplicar: (List<List<Punto>>) -> Unit) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditor(
    ruta: String,
    indicePagina: Int,
    totalPaginas: Int,
    proporcion: Float,
    pagina: ImageBitmap?,
    bloquesTexto: List<BloqueTexto>,
    snackbar: SnackbarHostState,
    acciones: AccionesEditor,
    alVolver: () -> Unit,
) {
    val estado = remember(ruta, indicePagina) { EstadoEditor(indicePagina) }
    var textoEnEdicion by remember { mutableStateOf<TextoEnEdicion?>(null) }
    var confirmandoSalida by remember { mutableStateOf(false) }

    val salir = { if (estado.hayCambios) confirmandoSalida = true else alVolver() }

    // Atras en el editor pregunta antes de tirar lo dibujado.
    BackHandler(enabled = true) { salir() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(
                titulo = stringResource(Res.string.ed_pagina_de, indicePagina + 1, totalPaginas),
                alVolver = salir,
                acciones = {
                    IconButton(
                        onClick = estado::deshacer,
                        enabled = estado.puedeDeshacer,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(Res.string.comun_deshacer),
                        )
                    }
                    IconButton(
                        onClick = estado::rehacer,
                        enabled = estado.puedeRehacer,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = stringResource(Res.string.comun_rehacer),
                        )
                    }
                    IconButton(
                        onClick = { acciones.alGuardar(estado.aEdicionPagina()) },
                        enabled = estado.hayCambios,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(Res.string.comun_guardar),
                        )
                    }
                },
            )
        },
    ) { relleno ->
        Column(modifier = Modifier.fillMaxSize().padding(relleno)) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                LienzoPagina(
                    pagina = pagina,
                    proporcion = proporcion,
                    estado = estado,
                    bloquesTexto = bloquesTexto,
                    alTocarBloque = { bloque ->
                        textoEnEdicion = TextoEnEdicion(
                            contenido = bloque.texto,
                            marco = bloque.marco,
                            sustituye = true,
                        )
                    },
                    alPedirTextoNuevo = { marco ->
                        textoEnEdicion = TextoEnEdicion("", marco, sustituye = false)
                    },
                    alPedirImagen = { marco ->
                        acciones.alElegirImagen { ruta -> estado.anadirImagen(ruta, marco) }
                    },
                    alPedirFirma = { marco ->
                        acciones.alPedirFirma { trazos -> estado.anadirFirma(trazos, marco) }
                    },
                )
            }

            PanelHerramientas(estado)
        }
    }

    textoEnEdicion?.let { enEdicion ->
        DialogoTexto(
            inicial = enEdicion,
            alConfirmar = { contenido, tamano ->
                estado.anadirTexto(contenido, enEdicion.marco, tamano, enEdicion.sustituye)
                textoEnEdicion = null
            },
            alCancelar = { textoEnEdicion = null },
        )
    }

    if (confirmandoSalida) {
        AlertDialog(
            onDismissRequest = { confirmandoSalida = false },
            title = { Text(stringResource(Res.string.ed_descartar_titulo)) },
            text = { Text(stringResource(Res.string.ed_descartar_texto)) },
            confirmButton = {
                TextButton(onClick = { confirmandoSalida = false; alVolver() }) {
                    Text(stringResource(Res.string.comun_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoSalida = false }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }
}

private data class TextoEnEdicion(
    val contenido: String,
    val marco: Rectangulo,
    val sustituye: Boolean,
)

// --- Lienzo -----------------------------------------------------------------

@Composable
private fun LienzoPagina(
    pagina: ImageBitmap?,
    proporcion: Float,
    estado: EstadoEditor,
    bloquesTexto: List<BloqueTexto>,
    alTocarBloque: (BloqueTexto) -> Unit,
    alPedirTextoNuevo: (Rectangulo) -> Unit,
    alPedirImagen: (Rectangulo) -> Unit,
    alPedirFirma: (Rectangulo) -> Unit,
) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
        val anchoDisponible = maxWidth
        val altoDisponible = maxHeight
        val anchoFinal = minOf(anchoDisponible, altoDisponible * proporcion)
        val altoFinal = anchoFinal / proporcion

        Box(
            modifier = Modifier
                .size(anchoFinal, altoFinal)
                .clip(MaterialTheme.shapes.small)
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
        ) {
            if (pagina != null) {
                Image(
                    bitmap = pagina,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            CapaGestos(
                estado = estado,
                bloquesTexto = bloquesTexto,
                alTocarBloque = alTocarBloque,
                alPedirTextoNuevo = alPedirTextoNuevo,
                alPedirImagen = alPedirImagen,
                alPedirFirma = alPedirFirma,
            )
        }
    }
}

@Composable
private fun CapaGestos(
    estado: EstadoEditor,
    bloquesTexto: List<BloqueTexto>,
    alTocarBloque: (BloqueTexto) -> Unit,
    alPedirTextoNuevo: (Rectangulo) -> Unit,
    alPedirImagen: (Rectangulo) -> Unit,
    alPedirFirma: (Rectangulo) -> Unit,
) {
    val herramienta = estado.herramienta
    val etiquetaLienzo = stringResource(Res.string.ed_dibujar)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = etiquetaLienzo }
            .pointerInput(herramienta) {
                if (herramienta == HerramientaEditor.MOVER) return@pointerInput
                detectDragGestures(
                    onDragStart = { posicion ->
                        val punto = aNormalizado(posicion, size.width, size.height)
                        when (herramienta) {
                            HerramientaEditor.DIBUJAR, HerramientaEditor.RESALTAR ->
                                estado.empezarTrazo(punto)

                            HerramientaEditor.FIGURA -> estado.empezarFigura(punto)
                            HerramientaEditor.BORRAR -> estado.borrarEn(punto)
                            else -> Unit
                        }
                    },
                    onDrag = { cambio, _ ->
                        val punto = aNormalizado(cambio.position, size.width, size.height)
                        when (herramienta) {
                            HerramientaEditor.DIBUJAR, HerramientaEditor.RESALTAR ->
                                estado.continuarTrazo(punto)

                            HerramientaEditor.FIGURA -> estado.continuarFigura(punto)
                            HerramientaEditor.BORRAR -> estado.borrarEn(punto)
                            else -> Unit
                        }
                        cambio.consume()
                    },
                    onDragEnd = {
                        when (herramienta) {
                            HerramientaEditor.DIBUJAR, HerramientaEditor.RESALTAR ->
                                estado.terminarTrazo()

                            HerramientaEditor.FIGURA -> estado.terminarFigura()
                            else -> Unit
                        }
                    },
                )
            }
            .pointerInput(herramienta, bloquesTexto) {
                detectTapGestures { posicion ->
                    val punto = aNormalizado(posicion, size.width, size.height)
                    when (herramienta) {
                        HerramientaEditor.TEXTO -> {
                            val bloque = bloquesTexto.firstOrNull { it.marco.contiene(punto) }
                            if (bloque != null) {
                                alTocarBloque(bloque)
                            } else {
                                alPedirTextoNuevo(marcoAlrededor(punto, 0.6f, 0.05f))
                            }
                        }

                        HerramientaEditor.IMAGEN ->
                            alPedirImagen(marcoAlrededor(punto, 0.4f, 0.3f))

                        HerramientaEditor.FIRMA ->
                            alPedirFirma(marcoAlrededor(punto, 0.45f, 0.14f))

                        HerramientaEditor.BORRAR -> estado.borrarEn(punto)
                        else -> Unit
                    }
                }
            },
    ) {
        estado.listaEdiciones.forEach { dibujarEdicion(it) }

        // Lo que se esta dibujando ahora mismo, aun sin confirmar.
        if (estado.trazoEnCurso.size >= 2) {
            dibujarTrazo(
                puntos = estado.trazoEnCurso,
                color = Color(estado.color.toInt()),
                grosorRelativo = if (estado.herramienta == HerramientaEditor.RESALTAR) {
                    estado.grosor * 4f
                } else {
                    estado.grosor
                },
                opacidad = if (estado.herramienta == HerramientaEditor.RESALTAR) 0.35f else estado.opacidad,
            )
        }
        estado.figuraEnCurso?.let { (inicio, fin) ->
            dibujarFiguraPrevia(estado.tipoFigura, inicio, fin, Color(estado.color.toInt()), estado.grosor)
        }
    }
}

private fun aNormalizado(posicion: Offset, ancho: Int, alto: Int): Punto = Punto(
    x = (posicion.x / ancho).coerceIn(0f, 1f),
    y = (posicion.y / alto).coerceIn(0f, 1f),
)

private fun marcoAlrededor(centro: Punto, ancho: Float, alto: Float): Rectangulo = Rectangulo(
    izquierda = (centro.x - ancho / 2f).coerceIn(0f, 1f - ancho),
    arriba = (centro.y - alto / 2f).coerceIn(0f, 1f - alto),
    derecha = (centro.x + ancho / 2f).coerceIn(ancho, 1f),
    abajo = (centro.y + alto / 2f).coerceIn(alto, 1f),
).normalizado()

private fun DrawScope.dibujarEdicion(edicion: Edicion) {
    when (edicion) {
        is Edicion.Trazo -> dibujarTrazo(
            puntos = edicion.puntos,
            color = Color(edicion.colorArgb.toInt()),
            grosorRelativo = edicion.grosor,
            opacidad = edicion.opacidad,
        )

        is Edicion.Figura -> dibujarFiguraPrevia(
            tipo = edicion.tipo,
            inicio = Punto(edicion.marco.izquierda, edicion.marco.arriba),
            fin = Punto(edicion.marco.derecha, edicion.marco.abajo),
            color = Color(edicion.colorTrazoArgb.toInt()),
            grosorRelativo = edicion.grosor,
            relleno = edicion.colorRellenoArgb?.let { Color(it.toInt()) },
        )

        is Edicion.Tapado -> {
            val r = edicion.marco.normalizado()
            drawRect(
                color = Color(edicion.colorArgb.toInt()),
                topLeft = Offset(r.izquierda * size.width, r.arriba * size.height),
                size = Size(r.ancho * size.width, r.alto * size.height),
            )
        }

        is Edicion.Texto -> {
            // La vista previa marca donde ira el texto; el texto real lo compone
            // el motor de PDF, que es quien sabe con que fuente cabe.
            val r = edicion.marco.normalizado()
            drawRect(
                color = Color(edicion.colorArgb.toInt()).copy(alpha = 0.18f),
                topLeft = Offset(r.izquierda * size.width, r.arriba * size.height),
                size = Size(r.ancho * size.width, r.alto * size.height),
            )
            drawRect(
                color = Color(edicion.colorArgb.toInt()),
                topLeft = Offset(r.izquierda * size.width, r.arriba * size.height),
                size = Size(r.ancho * size.width, r.alto * size.height),
                style = Stroke(width = 2f),
            )
        }

        is Edicion.Imagen -> {
            val r = edicion.marco.normalizado()
            drawRect(
                color = Color(0x33000000),
                topLeft = Offset(r.izquierda * size.width, r.arriba * size.height),
                size = Size(r.ancho * size.width, r.alto * size.height),
            )
        }

        is Edicion.Firma -> {
            val marco = edicion.marco.normalizado()
            edicion.trazos.filter { it.size >= 2 }.forEach { trazo ->
                dibujarTrazo(
                    puntos = trazo.map {
                        Punto(
                            marco.izquierda + it.x * marco.ancho,
                            marco.arriba + it.y * marco.alto,
                        )
                    },
                    color = Color(edicion.colorArgb.toInt()),
                    grosorRelativo = edicion.grosor,
                    opacidad = 1f,
                )
            }
        }
    }
}

private fun DrawScope.dibujarTrazo(
    puntos: List<Punto>,
    color: Color,
    grosorRelativo: Float,
    opacidad: Float,
) {
    if (puntos.size < 2) return
    val camino = Path().apply {
        moveTo(puntos.first().x * size.width, puntos.first().y * size.height)
        puntos.drop(1).forEach { lineTo(it.x * size.width, it.y * size.height) }
    }
    drawPath(
        path = camino,
        color = color.copy(alpha = opacidad),
        style = Stroke(
            width = grosorRelativo * size.width,
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.dibujarFiguraPrevia(
    tipo: TipoFigura,
    inicio: Punto,
    fin: Punto,
    color: Color,
    grosorRelativo: Float,
    relleno: Color? = null,
) {
    val x1 = inicio.x * size.width
    val y1 = inicio.y * size.height
    val x2 = fin.x * size.width
    val y2 = fin.y * size.height
    val trazo = Stroke(width = grosorRelativo * size.width, cap = StrokeCap.Round)

    when (tipo) {
        TipoFigura.RECTANGULO -> {
            val esquina = Offset(minOf(x1, x2), minOf(y1, y2))
            val medida = Size(kotlin.math.abs(x2 - x1), kotlin.math.abs(y2 - y1))
            relleno?.let { drawRect(it, topLeft = esquina, size = medida) }
            drawRect(color, topLeft = esquina, size = medida, style = trazo)
        }

        TipoFigura.ELIPSE -> {
            val esquina = Offset(minOf(x1, x2), minOf(y1, y2))
            val medida = Size(kotlin.math.abs(x2 - x1), kotlin.math.abs(y2 - y1))
            relleno?.let { drawOval(it, topLeft = esquina, size = medida) }
            drawOval(color, topLeft = esquina, size = medida, style = trazo)
        }

        TipoFigura.LINEA -> drawLine(color, Offset(x1, y1), Offset(x2, y2), trazo.width, StrokeCap.Round)

        TipoFigura.FLECHA -> {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), trazo.width, StrokeCap.Round)
            val angulo = kotlin.math.atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
            val largo = maxOf(trazo.width * 4f, 18f)
            val apertura = 0.5
            listOf(angulo - apertura, angulo + apertura).forEach { direccion ->
                drawLine(
                    color,
                    Offset(x2, y2),
                    Offset(
                        (x2 - largo * kotlin.math.cos(direccion)).toFloat(),
                        (y2 - largo * kotlin.math.sin(direccion)).toFloat(),
                    ),
                    trazo.width,
                    StrokeCap.Round,
                )
            }
        }
    }
}

// --- Panel de herramientas --------------------------------------------------

@Composable
private fun PanelHerramientas(estado: EstadoEditor) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HERRAMIENTAS.forEach { (herramienta, icono, etiqueta) ->
                    val texto = stringResource(etiqueta)
                    FilterChip(
                        selected = estado.herramienta == herramienta,
                        onClick = { estado.herramienta = herramienta },
                        label = { Text(texto, maxLines = 1) },
                        leadingIcon = {
                            Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            when (estado.herramienta) {
                HerramientaEditor.FILTRO -> ControlesFiltro(estado)
                HerramientaEditor.FIGURA -> {
                    ControlesFigura(estado)
                    ControlesTrazo(estado)
                }

                HerramientaEditor.TEXTO -> {
                    Text(
                        text = stringResource(Res.string.ed_tocar_para_sustituir),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                    Paleta(estado)
                }

                HerramientaEditor.MOVER, HerramientaEditor.BORRAR -> Spacer(Modifier.height(4.dp))
                else -> ControlesTrazo(estado)
            }
        }
    }
}

@Composable
private fun ControlesTrazo(estado: EstadoEditor) {
    Paleta(estado)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.ed_grosor),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Slider(
            value = estado.grosor,
            onValueChange = { estado.grosor = it },
            valueRange = 0.001f..0.03f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ControlesFigura(estado: EstadoEditor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FIGURAS.forEach { (tipo, etiqueta) ->
            FilterChip(
                selected = estado.tipoFigura == tipo,
                onClick = { estado.tipoFigura = tipo },
                label = { Text(stringResource(etiqueta), maxLines = 1) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
        FilterChip(
            selected = estado.conRelleno,
            onClick = { estado.conRelleno = !estado.conRelleno },
            label = { Text(stringResource(Res.string.ed_relleno), maxLines = 1) },
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun ControlesFiltro(estado: EstadoEditor) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FILTROS.forEach { (filtro, etiqueta) ->
                FilterChip(
                    selected = estado.filtro == filtro,
                    onClick = { estado.filtro = filtro },
                    label = { Text(stringResource(etiqueta), maxLines = 1) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        if (estado.filtro != FiltroPagina.NINGUNO) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.ed_intensidad),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Slider(
                    value = estado.intensidadFiltro,
                    onValueChange = { estado.intensidadFiltro = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(Res.string.ed_filtro_aviso),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Paleta(estado: EstadoEditor) {
    val etiquetaColor = stringResource(Res.string.ed_color)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .semantics { contentDescription = etiquetaColor },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EstadoEditor.COLORES.forEach { valor ->
            val elegido = estado.color == valor
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(valor) { detectTapGestures { estado.color = valor } },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (elegido) 32.dp else 26.dp)
                        .clip(CircleShape)
                        .background(Color(valor.toInt()))
                        .border(
                            width = if (elegido) 3.dp else 1.dp,
                            color = if (elegido) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun DialogoTexto(
    inicial: TextoEnEdicion,
    alConfirmar: (String, Float) -> Unit,
    alCancelar: () -> Unit,
) {
    var contenido by remember { mutableStateOf(inicial.contenido) }
    var tamano by remember {
        mutableStateOf(if (inicial.sustituye) inicial.marco.alto.coerceIn(0.012f, 0.08f) else 0.025f)
    }

    AlertDialog(
        onDismissRequest = alCancelar,
        title = {
            Text(
                if (inicial.sustituye) {
                    stringResource(Res.string.ed_sustituir_texto)
                } else {
                    stringResource(Res.string.ed_anadir_texto)
                },
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                Slider(
                    value = tamano,
                    onValueChange = { tamano = it },
                    valueRange = 0.008f..0.09f,
                )
                if (inicial.sustituye) {
                    Text(
                        text = stringResource(Res.string.ed_sustituir_aviso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(contenido, tamano) }) {
                Text(stringResource(Res.string.comun_aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) { Text(stringResource(Res.string.comun_cancelar)) }
        },
    )
}

private val HERRAMIENTAS: List<Triple<HerramientaEditor, androidx.compose.ui.graphics.vector.ImageVector, StringResource>> =
    listOf(
        Triple(HerramientaEditor.MOVER, Icons.Filled.PanTool, Res.string.ed_mover),
        Triple(HerramientaEditor.DIBUJAR, Icons.Filled.Brush, Res.string.ed_dibujar),
        Triple(HerramientaEditor.RESALTAR, Icons.Filled.Highlight, Res.string.ed_resaltar),
        Triple(HerramientaEditor.FIGURA, Icons.Filled.Rectangle, Res.string.ed_figuras),
        Triple(HerramientaEditor.TEXTO, Icons.Filled.TextFields, Res.string.ed_texto),
        Triple(HerramientaEditor.IMAGEN, Icons.Filled.Image, Res.string.ed_imagen),
        Triple(HerramientaEditor.FIRMA, Icons.Filled.Draw, Res.string.ed_firma),
        Triple(HerramientaEditor.FILTRO, Icons.Filled.AutoFixHigh, Res.string.ed_filtro),
        Triple(HerramientaEditor.BORRAR, Icons.Filled.Delete, Res.string.ed_borrar),
    )

private val FIGURAS: List<Pair<TipoFigura, StringResource>> = listOf(
    TipoFigura.RECTANGULO to Res.string.ed_rectangulo,
    TipoFigura.ELIPSE to Res.string.ed_elipse,
    TipoFigura.LINEA to Res.string.ed_linea,
    TipoFigura.FLECHA to Res.string.ed_flecha,
)

private val FILTROS: List<Pair<FiltroPagina, StringResource>> = listOf(
    FiltroPagina.NINGUNO to Res.string.ed_filtro_ninguno,
    FiltroPagina.DOCUMENTO_NITIDO to Res.string.ed_filtro_documento,
    FiltroPagina.ESCALA_DE_GRISES to Res.string.ed_filtro_grises,
    FiltroPagina.BLANCO_Y_NEGRO to Res.string.ed_filtro_bn,
    FiltroPagina.ALTO_CONTRASTE to Res.string.ed_filtro_contraste,
    FiltroPagina.ACLARAR to Res.string.ed_filtro_aclarar,
    FiltroPagina.INVERTIR to Res.string.ed_filtro_invertir,
)
