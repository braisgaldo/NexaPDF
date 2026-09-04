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
import androidx.compose.material.icons.filled.Share
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
import es.ghatostudio.nexapdf.resources.comun_compartir
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
import es.ghatostudio.nexapdf.ui.componentes.encuadreConPaso
import es.ghatostudio.nexapdf.ui.componentes.rememberEncuadre
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import es.ghatostudio.nexapdf.resources.vis_anterior
import es.ghatostudio.nexapdf.resources.vis_siguiente
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.times
import es.ghatostudio.nexapdf.domain.model.DireccionLectura
import es.ghatostudio.nexapdf.resources.vis_ir
import es.ghatostudio.nexapdf.resources.vis_ir_a_pagina
import es.ghatostudio.nexapdf.resources.vis_numero_pagina
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.di.LocalContenedor
import es.ghatostudio.nexapdf.ui.componentes.encuadreDosDedos
import androidx.compose.foundation.layout.aspectRatio
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import es.ghatostudio.nexapdf.resources.vis_de_total
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.Stroke

/** Lo que el visor necesita del resto de la aplicacion. */
class AccionesVisor(
    val alBuscar: suspend (String) -> List<Coincidencia>,
    val alIrAPagina: (Int) -> Unit,
    val alCompartir: () -> Unit,
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
    ruta: String,
    nombreDocumento: String,
    paginaActual: Int,
    totalPaginas: Int,
    proporcion: Float,
    /** Ancho al que se rasteriza cada pagina, segun la calidad elegida. */
    anchoRender: Int,
    contrasena: String?,
    lectura: DireccionLectura,
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
    // Indice de la aparicion en la que se esta, dentro de todos los resultados.
    var actual by remember { mutableStateOf(0) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    val encuadre = rememberEncuadre()

    // Cambiar de pagina con la anterior ampliada dejaria la nueva a medio ver.
    LaunchedEffect(paginaActual) { encuadre.reiniciar() }

    // Las dos formas de recorrer el documento comparten el mismo numero de
    // pagina, que es el que manda: lo mueve el dedo, pero tambien la
    // busqueda, el indice y el salto directo. Cada estado se sincroniza en
    // los dos sentidos, comprobando antes que hay algo que cambiar para que
    // no se persigan el uno al otro.
    val paginas = rememberPagerState(initialPage = paginaActual) {
        totalPaginas.coerceAtLeast(1)
    }
    val listaVertical = rememberLazyListState(initialFirstVisibleItemIndex = paginaActual)

    // Solo se informa de la pagina cuando el desplazamiento ha parado.
    // Contando las intermedias, un salto animado de la 3 a la 100 iba
    // avisando de cada una, cada aviso cambiaba la pagina actual, y eso
    // reiniciaba la animacion: el salto se quedaba a medias en una pagina
    // cualquiera del camino.
    LaunchedEffect(paginas, lectura) {
        if (lectura != DireccionLectura.LATERAL) return@LaunchedEffect
        snapshotFlow { paginas.settledPage }.distinctUntilChanged().collect {
            if (it != paginaActual) acciones.alIrAPagina(it)
        }
    }
    LaunchedEffect(listaVertical, lectura) {
        if (lectura != DireccionLectura.VERTICAL) return@LaunchedEffect
        snapshotFlow { listaVertical.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val visible = listaVertical.firstVisibleItemIndex
                if (visible != paginaActual) acciones.alIrAPagina(visible)
            }
    }
    LaunchedEffect(paginaActual, lectura) {
        when (lectura) {
            DireccionLectura.LATERAL ->
                if (paginas.currentPage != paginaActual) {
                    paginas.animateScrollToPage(paginaActual)
                }

            DireccionLectura.VERTICAL ->
                if (listaVertical.firstVisibleItemIndex != paginaActual) {
                    listaVertical.animateScrollToItem(paginaActual)
                }
        }
    }

    // La busqueda se lanza sola al dejar de teclear: obligar a pulsar una lupa
    // despues de escribir es un paso que nadie echa de menos cuando no esta.
    LaunchedEffect(consulta) {
        if (consulta.isBlank()) {
            resultados = null
            return@LaunchedEffect
        }
        // Se espera a que deje de teclear. Escribir "Pagina" lanzaba seis
        // recorridos del documento entero, uno por letra, y el ultimo era
        // el unico que servia.
        delay(350)
        buscandoAhora = true
        resultados = acciones.alBuscar(consulta)
        actual = 0
        buscandoAhora = false
        // Se salta a la primera aparicion: quien busca quiere verla, no leer
        // una lista y tener que elegir.
        resultados?.firstOrNull()?.let { acciones.alIrAPagina(it.pagina) }
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
                    val cuantos = resultados?.size ?: 0
                    if (cuantos > 0) {
                        Text(
                            text = "${actual + 1}/$cuantos",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                        IconButton(
                            onClick = {
                                actual = (actual - 1 + cuantos) % cuantos
                                resultados?.getOrNull(actual)?.let {
                                    acciones.alIrAPagina(it.pagina)
                                }
                            },
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = stringResource(Res.string.vis_anterior),
                            )
                        }
                        IconButton(
                            onClick = {
                                actual = (actual + 1) % cuantos
                                resultados?.getOrNull(actual)?.let {
                                    acciones.alIrAPagina(it.pagina)
                                }
                            },
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = stringResource(Res.string.vis_siguiente),
                            )
                        }
                    }
                    IconButton(
                        onClick = { buscando = false; consulta = ""; actual = 0 },
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
                            onClick = acciones.alCompartir,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = stringResource(Res.string.comun_compartir),
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
            BarraDePaginas(
                paginaActual = paginaActual,
                totalPaginas = totalPaginas,
                alIrAPagina = acciones.alIrAPagina,
            )
        },
    ) { relleno ->
        Box(modifier = Modifier.fillMaxSize().padding(relleno)) {
            when (lectura) {
                DireccionLectura.LATERAL -> HorizontalPager(
                    state = paginas,
                    // Con la pagina ampliada el dedo esta moviendola, no
                    // pasando de hoja: si el pager siguiera atendiendo al
                    // arrastre, ampliar y mirar un detalle seria imposible.
                    userScrollEnabled = !encuadre.ampliada,
                    modifier = Modifier.fillMaxSize(),
                ) { indice ->
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        PaginaDeVisor(
                            ruta = ruta,
                            indice = indice,
                            anchoPx = anchoRender,
                            contrasena = contrasena,
                            modifier = Modifier
                                .fillMaxSize()
                                .encuadreConPaso(encuadre)
                                .graphicsLayer {
                                    scaleX = encuadre.escala
                                    scaleY = encuadre.escala
                                    translationX = encuadre.desplazamiento.x
                                    translationY = encuadre.desplazamiento.y
                                },
                        )
                        Resaltados(
                            resultados = resultados,
                            pagina = indice,
                            activa = actual,
                            proporcion = proporcion,
                            escala = encuadre.escala,
                            desplazamiento = encuadre.desplazamiento,
                        )
                    }
                }

                // Desplazamiento continuo: todas las paginas seguidas. El
                // zoom aqui no mueve la pagina, la ensancha, y el ancho de
                // mas se recorre de lado: es como se lee un documento largo
                // cuando la letra es pequena.
                DireccionLectura.VERTICAL -> BoxWithConstraints(Modifier.fillMaxSize()) {
                    val anchoBase = maxWidth
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                            .encuadreDosDedos(encuadre),
                    ) {
                        LazyColumn(
                            state = listaVertical,
                            modifier = Modifier.width(anchoBase * encuadre.escala),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(totalPaginas.coerceAtLeast(1)) { indice ->
                                Box {
                                    PaginaDeVisor(
                                        ruta = ruta,
                                        indice = indice,
                                        anchoPx = anchoRender,
                                        contrasena = contrasena,
                                        proporcion = proporcion,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Resaltados(
                                        resultados = resultados,
                                        pagina = indice,
                                        activa = actual,
                                        proporcion = proporcion,
                                        escala = 1f,
                                        desplazamiento = Offset.Zero,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (buscandoAhora) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter))
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

/**
 * Una pagina del documento, rasterizada cuando hace falta.
 *
 * El visor pinta ahora varias paginas a la vez (la de al lado en el pager, las
 * de arriba y abajo en el desplazamiento continuo), asi que cada una se pide
 * por su cuenta en lugar de recibir una sola imagen ya hecha desde fuera.
 */
@Composable
private fun PaginaDeVisor(
    ruta: String,
    indice: Int,
    anchoPx: Int,
    contrasena: String?,
    modifier: Modifier = Modifier,
    proporcion: Float? = null,
) {
    val contenedor = LocalContenedor.current
    var imagen by remember(ruta, indice, anchoPx) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(ruta, indice, anchoPx) {
        imagen = contenedor.motorPdf
            .renderizarPagina(ruta, indice, anchoPx, contrasena)
            .valorONulo()
    }

    val mapa = imagen
    val conProporcion = if (proporcion != null) {
        modifier.aspectRatio(proporcion.coerceIn(0.2f, 5f))
    } else {
        modifier
    }
    Box(modifier = conProporcion, contentAlignment = Alignment.Center) {
        if (mapa != null) {
            androidx.compose.foundation.Image(
                bitmap = mapa,
                contentDescription = stringResource(Res.string.visor_pagina_numero, indice + 1),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

/** Las apariciones de la busqueda que caen en una pagina concreta. */
@Composable
private fun Resaltados(
    resultados: List<Coincidencia>?,
    pagina: Int,
    activa: Int,
    proporcion: Float,
    escala: Float,
    desplazamiento: Offset,
) {
    val enEstaPagina = resultados
        ?.withIndex()
        ?.filter { it.value.pagina == pagina }
        .orEmpty()
    if (enEstaPagina.isEmpty()) return
    CapaResaltados(
        coincidencias = enEstaPagina,
        activa = activa,
        proporcion = proporcion,
        escala = escala,
        desplazamiento = desplazamiento,
    )
}

/**
 * Barra de paginas: flechas, numero editable y deslizador.
 *
 * Las flechas solas obligan a ciento cuarenta y nueve toques para llegar a la
 * pagina 150. El numero se escribe directamente y el deslizador recorre el
 * documento entero de un gesto: son tres maneras de decir a donde se quiere ir,
 * y cada una gana en un caso distinto. Todo en la barra, sin dialogos de por
 * medio, porque moverse por un documento no es una decision que haya que
 * confirmar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraDePaginas(
    paginaActual: Int,
    totalPaginas: Int,
    alIrAPagina: (Int) -> Unit,
) {
    val total = totalPaginas.coerceAtLeast(1)
    val etiquetaPagina = stringResource(Res.string.vis_numero_pagina)
    val etiquetaIr = stringResource(Res.string.vis_ir_a_pagina)
    val teclado = LocalSoftwareKeyboardController.current
    val foco = LocalFocusManager.current

    // Mientras se arrastra el deslizador manda lo que marque; al soltar, se
    // salta. Cambiar de pagina en cada paso del arrastre obligaria a rasterizar
    // decenas de paginas que nadie va a mirar.
    var arrastrando by remember { mutableStateOf<Float?>(null) }
    var texto by remember(paginaActual) { mutableStateOf("${paginaActual + 1}") }

    fun saltar() {
        val destino = texto.toIntOrNull()?.coerceIn(1, total)
        if (destino != null && destino - 1 != paginaActual) alIrAPagina(destino - 1)
        texto = "${destino ?: (paginaActual + 1)}"
        teclado?.hide()
        foco.clearFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Sin esto la barra queda debajo de la del telefono y no se toca.
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(
                onClick = { alIrAPagina(paginaActual - 1) },
                enabled = paginaActual > 0,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = stringResource(Res.string.ed_pagina_anterior),
                )
            }
            // El numero y el total van en una misma pastilla: son una sola
            // cosa ("por donde voy"), y dos cajas sueltas separadas por texto
            // se leian como dos controles distintos.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(start = 4.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = texto,
                    onValueChange = { texto = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(onGo = { saltar() }),
                    modifier = Modifier
                        .width(64.dp)
                        .heightIn(min = 44.dp)
                        .padding(vertical = 10.dp)
                        .semantics { contentDescription = etiquetaPagina }
                        .onFocusChanged { if (!it.isFocused) texto = "${paginaActual + 1}" },
                )
                Text(
                    text = stringResource(Res.string.vis_de_total, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { alIrAPagina(paginaActual + 1) },
                enabled = paginaActual < total - 1,
                modifier = Modifier.size(52.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = stringResource(Res.string.ed_pagina_siguiente),
                )
            }
        }

        if (total > 1) {
            Slider(
                value = (arrastrando ?: (paginaActual + 1).toFloat()).coerceIn(1f, total.toFloat()),
                onValueChange = {
                    arrastrando = it
                    texto = "${it.roundToInt().coerceIn(1, total)}"
                },
                onValueChangeFinished = {
                    arrastrando?.let { valor ->
                        val destino = valor.roundToInt().coerceIn(1, total) - 1
                        if (destino != paginaActual) alIrAPagina(destino)
                    }
                    arrastrando = null
                },
                valueRange = 1f..total.toFloat(),
                // Barra fina y pulsador pequeno: es un indicador de por donde
                // se va que ademas se puede arrastrar, no el control principal
                // de la pantalla, y con el grosor de serie competia con el
                // documento.
                track = { estadoBarra ->
                    val recorrido = (estadoBarra.value - 1f) / (total - 1).coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(recorrido.coerceIn(0f, 1f))
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                },
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(if (arrastrando != null) 20.dp else 14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .semantics { contentDescription = etiquetaIr },
            )
        }
        }
    }
}

private enum class Panel { INDICE, FIRMAS }

/**
 * Marca sobre la pagina donde estan las apariciones.
 *
 * La activa va en un tono mas fuerte para poder seguirla al pasar de una a
 * otra; el resto quedan en amarillo suave, como un subrayado. Se pinta con el
 * mismo encuadre que la pagina para que las marcas no se despeguen al ampliar.
 */
@Composable
private fun CapaResaltados(
    coincidencias: List<IndexedValue<Coincidencia>>,
    activa: Int,
    proporcion: Float,
    escala: Float,
    desplazamiento: Offset,
) {
    val colorActiva = MaterialTheme.colorScheme.primary
    Canvas(
        // Sin margen propio: el hueco donde va la pagina ya lo trae puesto. Al
        // reescribir el visor con el pager, este margen se sumaba al de fuera y
        // las marcas caian un poco a la derecha y por debajo de la palabra.
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                translationX = desplazamiento.x
                translationY = desplazamiento.y
            },
    ) {
        // La imagen se dibuja con ContentScale.Fit: hay que recomponer el mismo
        // encaje para que las marcas caigan sobre las palabras y no al lado.
        val anchoDisponible = size.width
        val altoDisponible = size.height
        val anchoPagina: Float
        val altoPagina: Float
        if (anchoDisponible / altoDisponible > proporcion) {
            altoPagina = altoDisponible
            anchoPagina = altoDisponible * proporcion
        } else {
            anchoPagina = anchoDisponible
            altoPagina = anchoDisponible / proporcion
        }
        val margenX = (anchoDisponible - anchoPagina) / 2f
        val margenY = (altoDisponible - altoPagina) / 2f

        coincidencias.forEach { (indice, coincidencia) ->
            val marco = coincidencia.marco.normalizado()
            val esActiva = indice == activa
            // Se pinta un poco mas alto y ancho que la palabra: pegado al
            // trazo, el subrayado parecia una sombra del texto.
            val holguraX = anchoPagina * 0.004f
            val holguraY = altoPagina * 0.002f
            val esquina = Offset(
                margenX + marco.izquierda * anchoPagina - holguraX,
                margenY + marco.arriba * altoPagina - holguraY,
            )
            val tamano = Size(
                (marco.derecha - marco.izquierda) * anchoPagina + holguraX * 2,
                (marco.abajo - marco.arriba) * altoPagina + holguraY * 2,
            )
            drawRect(
                color = if (esActiva) {
                    NARANJA_ACTIVA
                } else {
                    AMARILLO_BUSQUEDA
                },
                topLeft = esquina,
                size = tamano,
            )
            // La activa ademas va enmarcada: con ocho apariciones amarillas
            // el contador decia "3 de 8" y la vista no sabia cual era la 3.
            if (esActiva) {
                drawRect(
                    color = colorActiva,
                    topLeft = esquina,
                    size = tamano,
                    style = Stroke(width = size.minDimension * 0.004f),
                )
            }
        }
    }
}

/**
 * Amarillo de subrayado, con transparencia para dejar leer debajo.
 *
 * Antes era un lavanda al 40 % que sobre papel blanco casi no se veia. Un
 * amarillo de rotulador al 70 % es lo que espera cualquiera de un
 * resaltado, y el texto negro debajo sigue leyendose de sobra.
 */
private val AMARILLO_BUSQUEDA = Color(0xB3FFD54F)

/** La aparicion en la que se esta: naranja, para no confundirla con el resto. */
private val NARANJA_ACTIVA = Color(0xCCFF8A3D)

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
