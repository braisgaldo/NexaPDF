package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
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
import es.ghatostudio.nexapdf.resources.comun_eliminar
import es.ghatostudio.nexapdf.resources.comun_guardar
import es.ghatostudio.nexapdf.resources.comun_rehacer
import es.ghatostudio.nexapdf.resources.ed_anadir_texto
import es.ghatostudio.nexapdf.resources.ed_borrar
import es.ghatostudio.nexapdf.resources.ed_color
import es.ghatostudio.nexapdf.resources.ed_descartar_texto
import es.ghatostudio.nexapdf.resources.ed_descartar_titulo
import es.ghatostudio.nexapdf.resources.ed_coloca_la_firma
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
import es.ghatostudio.nexapdf.resources.ed_pagina_anterior
import es.ghatostudio.nexapdf.resources.ed_pagina_de
import es.ghatostudio.nexapdf.resources.ed_pagina_siguiente
import es.ghatostudio.nexapdf.resources.ed_rectangulo
import es.ghatostudio.nexapdf.resources.ed_relleno
import es.ghatostudio.nexapdf.resources.ed_resaltar
import es.ghatostudio.nexapdf.resources.ed_sustituir_aviso
import es.ghatostudio.nexapdf.resources.ed_texto_fondo
import es.ghatostudio.nexapdf.resources.ed_sustituir_texto
import es.ghatostudio.nexapdf.resources.ed_texto
import es.ghatostudio.nexapdf.resources.ed_mover_asas
import es.ghatostudio.nexapdf.resources.ed_mover_ayuda
import es.ghatostudio.nexapdf.resources.ed_tocar_para_sustituir
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max

/** Lo que el editor necesita del resto de la aplicacion. */
class AccionesEditor(
    val alGuardar: (es.ghatostudio.nexapdf.domain.model.EdicionPagina) -> Unit,
    val alElegirImagen: (aplicar: (String) -> Unit) -> Unit,
    val alPedirFirma: (aplicar: (List<List<Punto>>) -> Unit) -> Unit,
    /** Ir a otra pagina del mismo documento sin salir del editor. */
    val alIrAPagina: (Int) -> Unit,
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
    /**
     * Firma ya dibujada que falta por situar.
     *
     * Llega asi cuando se entro por "Firmar PDF" desde el inicio: alli se
     * dibuja primero y se coloca despues, al reves que dentro del editor. Con
     * un valor aqui, la herramienta de firma arranca seleccionada y el marco
     * que trace el usuario decide donde cae, en lugar de estamparla en una
     * esquina fija.
     */
    firmaPendiente: List<List<Punto>>? = null,
    snackbar: SnackbarHostState,
    acciones: AccionesEditor,
    alVolver: () -> Unit,
) {
    val estado = remember(ruta, indicePagina) { EstadoEditor(indicePagina) }

    LaunchedEffect(firmaPendiente) {
        if (firmaPendiente != null) estado.herramienta = HerramientaEditor.FIRMA
    }
    var textoEnEdicion by remember { mutableStateOf<TextoEnEdicion?>(null) }
    var confirmandoSalida by remember { mutableStateOf(false) }
    // Pagina a la que se quiere ir; se resuelve tras confirmar el descarte.
    var paginaPedida by remember { mutableStateOf<Int?>(null) }

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
                    // Cambiar de pagina con cambios sin guardar los tiraria,
                    // asi que se pregunta igual que al salir.
                    val irA = { destino: Int ->
                        if (estado.hayCambios) {
                            paginaPedida = destino
                        } else {
                            acciones.alIrAPagina(destino)
                        }
                    }
                    IconButton(
                        onClick = { irA(indicePagina - 1) },
                        enabled = indicePagina > 0,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = stringResource(Res.string.ed_pagina_anterior),
                        )
                    }
                    IconButton(
                        onClick = { irA(indicePagina + 1) },
                        enabled = indicePagina < totalPaginas - 1,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = stringResource(Res.string.ed_pagina_siguiente),
                        )
                    }
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
            // Quien llega aqui desde "Firmar PDF" trae la firma dibujada pero
            // no tiene forma de adivinar que ahora hay que tocar la pagina.
            if (firmaPendiente != null) {
                Text(
                    text = stringResource(Res.string.ed_coloca_la_firma),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

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
                            colorDeLaPagina = colorDeLaPaginaEn(pagina, bloque.marco),
                        )
                    },
                    alPedirTextoNuevo = { marco ->
                        textoEnEdicion = TextoEnEdicion(
                            contenido = "",
                            marco = marco,
                            sustituye = false,
                            colorDeLaPagina = colorDeLaPaginaEn(pagina, marco),
                        )
                    },
                    alPedirImagen = { marco ->
                        acciones.alElegirImagen { ruta -> estado.anadirImagen(ruta, marco) }
                    },
                    alPedirFirma = { marco ->
                        if (firmaPendiente != null) {
                            estado.anadirFirma(firmaPendiente, marco)
                        } else {
                            acciones.alPedirFirma { trazos -> estado.anadirFirma(trazos, marco) }
                        }
                    },
                )
            }

            PanelHerramientas(estado)
        }
    }

    textoEnEdicion?.let { enEdicion ->
        DialogoTexto(
            inicial = enEdicion,
            alConfirmar = { contenido, tamano, fondo ->
                estado.anadirTexto(
                    contenido = contenido,
                    marco = enEdicion.marco,
                    tamano = tamano,
                    taparDebajo = enEdicion.sustituye,
                    fondoArgb = fondo,
                )
                textoEnEdicion = null
            },
            alCancelar = { textoEnEdicion = null },
        )
    }

    paginaPedida?.let { destino ->
        AlertDialog(
            onDismissRequest = { paginaPedida = null },
            title = { Text(stringResource(Res.string.ed_descartar_titulo)) },
            text = { Text(stringResource(Res.string.ed_descartar_texto)) },
            confirmButton = {
                TextButton(onClick = { paginaPedida = null; acciones.alIrAPagina(destino) }) {
                    Text(stringResource(Res.string.comun_aceptar))
                }
            },
            dismissButton = {
                TextButton(onClick = { paginaPedida = null }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
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
    /** Color que tiene la pagina debajo del texto, para usarlo de fondo. */
    val colorDeLaPagina: Long,
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
    var asaActiva by remember { mutableStateOf<Asa?>(null) }
    val medidor = rememberTextMeasurer()
    val colorSeleccion = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = etiquetaLienzo }
            .pointerInput(herramienta) {
                detectDragGestures(
                    onDragStart = { posicion ->
                        val punto = aNormalizado(posicion, size.width, size.height)
                        when (herramienta) {
                            HerramientaEditor.DIBUJAR, HerramientaEditor.RESALTAR ->
                                estado.empezarTrazo(punto)

                            HerramientaEditor.FIGURA -> estado.empezarFigura(punto)
                            HerramientaEditor.BORRAR -> estado.borrarEn(punto)

                            // Con "Mover" el arrastre empieza cogiendo un asa
                            // del objeto seleccionado, si el dedo cae sobre
                            // ella, y si no el objeto entero.
                            HerramientaEditor.MOVER -> {
                                val objeto = estado.objetoSeleccionado
                                asaActiva = if (objeto == null) {
                                    null
                                } else {
                                    asaBajoElDedo(objeto.marco, punto)
                                }
                                if (asaActiva == null) estado.seleccionarEn(punto)
                            }

                            else -> Unit
                        }
                    },
                    onDrag = { cambio, desplazamiento ->
                        val punto = aNormalizado(cambio.position, size.width, size.height)
                        when (herramienta) {
                            HerramientaEditor.DIBUJAR, HerramientaEditor.RESALTAR ->
                                estado.continuarTrazo(punto)

                            HerramientaEditor.FIGURA -> estado.continuarFigura(punto)
                            HerramientaEditor.BORRAR -> estado.borrarEn(punto)

                            HerramientaEditor.MOVER -> {
                                val objeto = estado.objetoSeleccionado
                                if (objeto != null) {
                                    when (asaActiva) {
                                        Asa.ESCALAR -> {
                                            // El asa esta abajo a la derecha:
                                            // alejarla del centro agranda.
                                            val m = objeto.marco.normalizado()
                                            val diagonal = max(
                                                m.derecha - m.izquierda,
                                                m.abajo - m.arriba,
                                            ).coerceAtLeast(0.01f)
                                            val avance =
                                                (desplazamiento.x / size.width +
                                                    desplazamiento.y / size.height) / 2f
                                            estado.escalarSeleccion(1f + avance / diagonal)
                                        }

                                        Asa.ROTAR -> {
                                            val m = objeto.marco.normalizado()
                                            val cx = (m.izquierda + m.derecha) / 2f
                                            val cy = (m.arriba + m.abajo) / 2f
                                            val antes = atan2(
                                                punto.y - desplazamiento.y / size.height - cy,
                                                punto.x - desplazamiento.x / size.width - cx,
                                            )
                                            val ahora = atan2(punto.y - cy, punto.x - cx)
                                            estado.rotarSeleccion(
                                                ((ahora - antes) * 180f / PI).toFloat(),
                                            )
                                        }

                                        null -> estado.moverSeleccion(
                                            desplazamiento.x / size.width,
                                            desplazamiento.y / size.height,
                                        )
                                    }
                                }
                            }

                            else -> Unit
                        }
                        cambio.consume()
                    },
                    onDragEnd = {
                        when (herramienta) {
                            HerramientaEditor.DIBUJAR, HerramientaEditor.RESALTAR ->
                                estado.terminarTrazo()

                            HerramientaEditor.FIGURA -> estado.terminarFigura()
                            HerramientaEditor.MOVER -> asaActiva = null
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
                            // Se admite fallar por poco y, si varias lineas
                            // caen dentro del margen, gana la mas cercana.
                            val bloque = bloquesTexto
                                .filter { it.marco.contieneConMargen(punto, MARGEN_TOQUE) }
                                .minByOrNull { it.marco.distanciaAlCentro(punto) }
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
                        HerramientaEditor.MOVER -> estado.seleccionarEn(punto)
                        else -> Unit
                    }
                }
            },
    ) {
        estado.listaEdiciones.forEach { dibujarEdicion(it, medidor) }

        // Marco y asas del objeto seleccionado.
        if (herramienta == HerramientaEditor.MOVER) {
            estado.objetoSeleccionado?.let { objeto ->
                val marco = objeto.marco.normalizado()
                val centro = Offset(
                    ((marco.izquierda + marco.derecha) / 2f) * size.width,
                    ((marco.arriba + marco.abajo) / 2f) * size.height,
                )
                // El marco acompana al giro del objeto: si se queda recto
                // mientras el contenido esta torcido, parece que la seleccion
                // no es de lo que se acaba de girar.
                rotate(degrees = objeto.rotacion, pivot = centro) {
                    dibujarMarcoSeleccion(objeto.marco, colorSeleccion)
                }
            }
        }

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

private fun DrawScope.dibujarEdicion(edicion: Edicion, medidor: TextMeasurer) {
    // Los objetos girados se pintan con el lienzo rotado alrededor de su
    // centro: asi el dibujo de cada tipo no tiene que saber nada del giro.
    val giro = (edicion as? Edicion.Colocada)?.rotacion ?: 0f
    if (giro != 0f) {
        val m = edicion as Edicion.Colocada
        val marco = m.marco.normalizado()
        val centro = Offset(
            ((marco.izquierda + marco.derecha) / 2f) * size.width,
            ((marco.arriba + marco.abajo) / 2f) * size.height,
        )
        rotate(degrees = giro, pivot = centro) { dibujarEdicionSinGirar(edicion, medidor) }
        return
    }
    dibujarEdicionSinGirar(edicion, medidor)
}

private fun DrawScope.dibujarEdicionSinGirar(edicion: Edicion, medidor: TextMeasurer) {
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
            // Se compone el texto de verdad y no un recuadro que marque donde
            // ira: si lo que se anade es texto sobre el documento, hay que
            // verlo para poder colocarlo. El motor de PDF lo vuelve a componer
            // al guardar con la fuente que corresponda, pero lo que se ve aqui
            // ya es el texto y no un hueco gris.
            val r = edicion.marco.normalizado()
            edicion.fondoArgb?.let { fondo ->
                drawRect(
                    color = Color(fondo.toInt()),
                    topLeft = Offset(r.izquierda * size.width, r.arriba * size.height),
                    size = Size(r.ancho * size.width, r.alto * size.height),
                )
            }
            val medida = medidor.measure(
                text = edicion.contenido,
                style = TextStyle(
                    color = Color(edicion.colorArgb.toInt()),
                    fontSize = (edicion.tamano * size.height).toSp(),
                    fontWeight = if (edicion.negrita) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (edicion.cursiva) FontStyle.Italic else FontStyle.Normal,
                ),
                constraints = Constraints(maxWidth = (r.ancho * size.width).toInt().coerceAtLeast(1)),
            )
            drawText(
                textLayoutResult = medida,
                topLeft = Offset(r.izquierda * size.width, r.arriba * size.height),
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

                HerramientaEditor.MOVER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                if (estado.objetoSeleccionado == null) {
                                    Res.string.ed_mover_ayuda
                                } else {
                                    Res.string.ed_mover_asas
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (estado.objetoSeleccionado != null) {
                            TextButton(
                                onClick = { estado.borrarSeleccion() },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(Res.string.comun_eliminar))
                            }
                        }
                    }
                }

                HerramientaEditor.BORRAR -> Spacer(Modifier.height(4.dp))
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
    alConfirmar: (String, Float, Long?) -> Unit,
    alCancelar: () -> Unit,
) {
    var contenido by remember { mutableStateOf(inicial.contenido) }
    var tamano by remember {
        mutableStateOf(if (inicial.sustituye) inicial.marco.alto.coerceIn(0.012f, 0.08f) else 0.025f)
    }
    // Por defecto, el color que tiene la pagina justo debajo.
    var conFondo by remember { mutableStateOf(true) }
    var fondo by remember { mutableStateOf(inicial.colorDeLaPagina) }

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
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.ed_texto_fondo),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = conFondo, onCheckedChange = { conFondo = it })
                }
                if (conFondo) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        (listOf(inicial.colorDeLaPagina) + EstadoEditor.COLORES).distinct().take(7)
                            .forEach { opcion ->
                                val elegido = opcion == fondo
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(opcion.toInt()))
                                        .border(
                                            width = if (elegido) 3.dp else 1.dp,
                                            color = if (elegido) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                            shape = CircleShape,
                                        )
                                        .clickable { fondo = opcion },
                                )
                            }
                    }
                }
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
            TextButton(onClick = { alConfirmar(contenido, tamano, fondo.takeIf { conFondo }) }) {
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

/**
 * Cuanto se ensancha la zona sensible de una linea de texto, en fraccion de
 * pagina. 2,5 % de 842 puntos son unos 21 puntos por lado: suficiente para que
 * el dedo acierte sin que dos lineas seguidas se pisen.
 */
private const val MARGEN_TOQUE = 0.025f

/** Las dos asas del marco de seleccion. */
private enum class Asa { ESCALAR, ROTAR }

/**
 * Radio de las asas en fraccion de pagina.
 *
 * Se pintan mas pequenas de lo que responden al tacto: un circulo de 48 dp
 * encima del documento taparia justo lo que el usuario intenta colocar.
 */
private const val RADIO_ASA = 0.030f

/** Que asa hay bajo el dedo, si hay alguna. */
private fun asaBajoElDedo(marco: Rectangulo, punto: Punto): Asa? {
    val m = marco.normalizado()
    val escalar = Punto(m.derecha, m.abajo)
    val rotar = Punto(m.derecha, m.arriba)
    return when {
        cercaDe(punto, escalar) -> Asa.ESCALAR
        cercaDe(punto, rotar) -> Asa.ROTAR
        else -> null
    }
}

private fun cercaDe(a: Punto, b: Punto): Boolean {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy <= RADIO_ASA * RADIO_ASA
}

/**
 * Pinta el marco del objeto seleccionado con sus dos asas.
 *
 * Escalar abajo a la derecha y girar arriba a la derecha, que es donde las
 * pone todo el mundo y donde la mano no tapa el objeto al usarlas.
 */
private fun DrawScope.dibujarMarcoSeleccion(marco: Rectangulo, color: Color) {
    val m = marco.normalizado()
    val izquierda = m.izquierda * size.width
    val arriba = m.arriba * size.height
    val ancho = (m.derecha - m.izquierda) * size.width
    val alto = (m.abajo - m.arriba) * size.height
    val grosor = size.minDimension * 0.004f

    drawRect(
        color = color,
        topLeft = Offset(izquierda, arriba),
        size = Size(ancho, alto),
        style = Stroke(
            width = grosor,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(size.minDimension * 0.02f, size.minDimension * 0.015f),
            ),
        ),
    )

    val radio = size.minDimension * 0.018f
    listOf(
        Offset(izquierda + ancho, arriba + alto),
        Offset(izquierda + ancho, arriba),
    ).forEach { centro ->
        drawCircle(color = Color.White, radius = radio, center = centro)
        drawCircle(color = color, radius = radio, center = centro, style = Stroke(width = grosor))
    }
}

/**
 * Color que tiene la pagina en el centro de [marco].
 *
 * Se lee un unico pixel del mapa ya renderizado, no la pagina entera: el PDF
 * de un movil puede ser una imagen de veinte megapixeles y copiarla para mirar
 * un punto seria absurdo. Si aun no hay pagina dibujada se supone blanco, que
 * es el fondo de practicamente cualquier PDF.
 */
private fun colorDeLaPaginaEn(pagina: ImageBitmap?, marco: Rectangulo): Long {
    val mapa = pagina ?: return BLANCO_PAGINA
    val m = marco.normalizado()
    val x = (((m.izquierda + m.derecha) / 2f) * mapa.width).toInt().coerceIn(0, mapa.width - 1)
    val y = (((m.arriba + m.abajo) / 2f) * mapa.height).toInt().coerceIn(0, mapa.height - 1)
    return runCatching {
        val pixel = mapa.toPixelMap(startX = x, startY = y, width = 1, height = 1)[0, 0]
        // Opaco siempre: un fondo semitransparente dejaria ver el texto viejo.
        val r = (pixel.red * 255f).toLong().coerceIn(0, 255)
        val g = (pixel.green * 255f).toLong().coerceIn(0, 255)
        val b = (pixel.blue * 255f).toLong().coerceIn(0, 255)
        0xFF000000L or (r shl 16) or (g shl 8) or b
    }.getOrDefault(BLANCO_PAGINA)
}

private const val BLANCO_PAGINA = 0xFFFFFFFFL
