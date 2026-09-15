package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.escaner.DeteccionDocumento
import es.ghatostudio.nexapdf.domain.escaner.escenaDistinta
import es.ghatostudio.nexapdf.domain.plataforma.CamaraDocumentos
import es.ghatostudio.nexapdf.domain.plataforma.Disparador
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.esc_automatica
import es.ghatostudio.nexapdf.resources.esc_buscando
import es.ghatostudio.nexapdf.resources.esc_capturar
import es.ghatostudio.nexapdf.resources.esc_consejo
import es.ghatostudio.nexapdf.resources.esc_continuar
import es.ghatostudio.nexapdf.resources.esc_deteccion
import es.ghatostudio.nexapdf.resources.esc_encuadrando
import es.ghatostudio.nexapdf.resources.esc_galeria
import es.ghatostudio.nexapdf.resources.esc_linterna
import es.ghatostudio.nexapdf.resources.esc_listo
import es.ghatostudio.nexapdf.resources.esc_modo_auto
import es.ghatostudio.nexapdf.resources.esc_modo_manual
import es.ghatostudio.nexapdf.resources.esc_permiso_conceder
import es.ghatostudio.nexapdf.resources.esc_permiso_denegado
import es.ghatostudio.nexapdf.resources.esc_permiso_detalle
import es.ghatostudio.nexapdf.resources.esc_permiso_titulo
import es.ghatostudio.nexapdf.resources.esc_porcentaje
import es.ghatostudio.nexapdf.resources.esc_siguiente_hoja
import es.ghatostudio.nexapdf.resources.esc_salir_confirmar
import es.ghatostudio.nexapdf.resources.esc_salir_detalle
import es.ghatostudio.nexapdf.resources.esc_salir_titulo
import es.ghatostudio.nexapdf.resources.esc_sin_camara
import es.ghatostudio.nexapdf.resources.esc_titulo
import es.ghatostudio.nexapdf.resources.plural_esc_paginas
import es.ghatostudio.nexapdf.ui.componentes.ContornoDocumento
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * El visor del escaner: la camara, el contorno del papel y el porcentaje.
 *
 * El porcentaje esta ahi porque es la unica forma honesta de contestar a la
 * pregunta que se hace quien apunta con el telefono: "esto lo esta viendo bien o
 * no". Un contorno que aparece y desaparece no lo contesta; un numero que sube
 * segun se acerca el encuadre, si. Y es el mismo numero que decide si la captura
 * automatica dispara, de modo que lo que se ensena es lo que la aplicacion usa,
 * no una barra de adorno.
 */
@Composable
fun PantallaEscaner(
    camara: CamaraDocumentos?,
    capturadas: Int,
    capturaAutomatica: Boolean,
    fotosPorDisparo: Int,
    snackbar: SnackbarHostState,
    alCambiarAutomatica: (Boolean) -> Unit,
    /**
     * Se llama con las fotos recien hechas: la primera es la de referencia y
     * las demas, si las hay, son el resto de la rafaga.
     */
    alCapturar: (List<String>) -> Unit,
    alAbrirGaleria: () -> Unit,
    alContinuar: () -> Unit,
    alPedirPermiso: () -> Unit,
    permisoConcedido: Boolean,
    permisoDenegado: Boolean,
    alVolver: () -> Unit,
) {
    // Salir con paginas capturadas y sin convertir tira el trabajo. Se pregunta
    // antes: una pulsacion de atras sin querer no deberia costar diez hojas ya
    // escaneadas. Sin paginas no se pregunta nada, que ahi no hay nada que
    // perder.
    var confirmandoSalida by remember { mutableStateOf(false) }
    val salir = { if (capturadas > 0) confirmandoSalida = true else alVolver() }

    BackHandler(enabled = capturadas > 0) { confirmandoSalida = true }

    if (confirmandoSalida) {
        AlertDialog(
            onDismissRequest = { confirmandoSalida = false },
            title = { Text(stringResource(Res.string.esc_salir_titulo)) },
            text = { Text(stringResource(Res.string.esc_salir_detalle)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoSalida = false
                        alVolver()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.esc_salir_confirmar))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmandoSalida = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }

    // El snackbar NO va en el Scaffold. Ahi se coloca al borde inferior de la
    // pantalla, encima de "Galeria" y "Continuar", y mientras dura no se pueden
    // pulsar: justo despues de capturar, que es cuando uno quiere seguir. Se
    // pinta dentro del visor, al pie de la imagen, donde no hay nada que tapar.
    Scaffold(containerColor = Color.Black) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .background(Color.Black),
        ) {
            BarraDelEscaner(
                capturadas = capturadas,
                alVolver = salir,
                automatica = capturaAutomatica,
                alCambiarAutomatica = alCambiarAutomatica,
                mostrarControles = camara != null && permisoConcedido,
            )

            // El area de la imagen es una caja para poder colgar de ella los
            // avisos. Va aqui y no en el Scaffold porque ahi se colocan al borde
            // inferior de la pantalla, encima de "Galeria" y "Continuar", y
            // mientras dura el aviso esos botones no se pueden pulsar: justo
            // despues de capturar, que es cuando uno quiere seguir. Y va en la
            // caja y no dentro del visor porque el visor no siempre esta
            // compuesto —puede estar el panel del permiso— y un aviso sin nadie
            // que lo pinte se queda esperando para siempre y atasca los
            // siguientes.
            Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                camara == null || !camara.disponible -> SinCamara(
                    mensaje = stringResource(Res.string.esc_sin_camara),
                    alAbrirGaleria = alAbrirGaleria,
                    modifier = Modifier.fillMaxSize(),
                )

                !permisoConcedido -> PanelDePermiso(
                    denegado = permisoDenegado,
                    alPedir = alPedirPermiso,
                    alAbrirGaleria = alAbrirGaleria,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> VisorEnVivo(
                    camara = camara,
                    capturaAutomatica = capturaAutomatica,
                    fotosPorDisparo = fotosPorDisparo,
                    alCapturar = alCapturar,
                    modifier = Modifier.fillMaxSize(),
                )
            }

                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ALTURA_DEL_DISPARADOR),
                )
            }

            PieDelEscaner(
                capturadas = capturadas,
                alAbrirGaleria = alAbrirGaleria,
                alContinuar = alContinuar,
            )
        }
    }
}

@Composable
private fun VisorEnVivo(
    camara: CamaraDocumentos,
    capturaAutomatica: Boolean,
    fotosPorDisparo: Int,
    alCapturar: (List<String>) -> Unit,
    modifier: Modifier,
) {
    var deteccion by remember { mutableStateOf(DeteccionDocumento.NADA) }
    var proporcion by remember { mutableStateOf(3f / 4f) }
    var disparador by remember { mutableStateOf<Disparador?>(null) }
    var linterna by remember { mutableStateOf(false) }
    var disparando by remember { mutableStateOf(false) }

    // El disparo automatico exige las dos cosas: encuadre bueno **y** contorno
    // quieto. Con la confianza sola, la foto salia movida en cuanto el papel se
    // encuadraba bien antes de que la mano se parase; la quietud la mide el
    // suavizador comparando fotogramas seguidos.
    val encuadreListo = deteccion.estable &&
        deteccion.confianza >= DeteccionDocumento.UMBRAL_AUTOMATICO

    // La hoja que se acaba de fotografiar, o `null` si la camara esta libre
    // para disparar otra vez.
    //
    // Sin esto la captura automatica sacaba **dos fotos de la misma pagina**: un
    // segundo despues del disparo el papel sigue ahi, sigue bien encuadrado y
    // sigue quieto, asi que se cumplian otra vez todas las condiciones. Una
    // pausa mas larga no lo arregla, solo lo retrasa. Lo que hay que exigir es
    // que **la escena cambie**: que se quite el papel o que se ponga otro en
    // otro sitio. Es lo que uno hace de todas formas al pasar de hoja.
    var hojaYaTomada by remember { mutableStateOf<Cuadrilatero?>(null) }
    var sinPapel by remember { mutableStateOf(0) }
    val puedeDisparar = encuadreListo && hojaYaTomada == null

    fun disparar() {
        if (disparando) return
        disparando = true
    }

    LaunchedEffect(disparando) {
        if (!disparando) return@LaunchedEffect
        val encuadrada = deteccion.cuadro
        val fotos = disparador?.disparar().orEmpty()
        if (fotos.isNotEmpty()) {
            alCapturar(fotos)
            // Se recuerda que esta hoja ya esta tomada. La automatica no vuelve
            // a disparar hasta que la escena cambie.
            hojaYaTomada = encuadrada ?: Cuadrilatero.COMPLETO
            sinPapel = 0
        }
        // Un respiro antes de volver a analizar, para que el obturador no
        // parpadee dos veces seguidas en pantalla.
        delay(PAUSA_TRAS_DISPARO)
        disparando = false
    }

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(proporcion)
                .background(Color.Black),
        ) {
            camara.Visor(
                modifier = Modifier.fillMaxSize(),
                linterna = linterna,
                analizando = !disparando,
                fotosPorDisparo = fotosPorDisparo,
                alMedirProporcion = { proporcion = it },
                alDetectar = { nueva ->
                    deteccion = nueva
                    val tomada = hojaYaTomada
                    if (tomada != null) {
                        val cuadro = nueva.cuadro
                        if (cuadro == null) {
                            // Que el papel falte un fotograma no basta: la mano
                            // tiembla y la deteccion se pierde un instante sin
                            // que nadie haya cambiado de hoja. Si con eso se
                            // rearmara, volveria a disparar sobre la misma
                            // pagina en cuanto la recuperase.
                            sinPapel += 1
                            if (sinPapel >= FOTOGRAMAS_SIN_PAPEL) hojaYaTomada = null
                        } else {
                            sinPapel = 0
                            if (escenaDistinta(tomada, cuadro)) hojaYaTomada = null
                        }
                    }
                },
                alEstarLista = { disparador = it },
            )

            ContornoDocumento(
                cuadro = deteccion.cuadro.takeIf { deteccion.hayPapel },
                color = colorSegunConfianza(deteccion.confianza, deteccion.estable),
                modifier = Modifier.fillMaxSize(),
            )

            IndicadorDeteccion(
                deteccion = deteccion,
                esperandoOtraHoja = capturaAutomatica && hojaYaTomada != null,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
            )

            if (disparando) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        BotonesDelVisor(
            linterna = linterna,
            alCambiarLinterna = { linterna = it },
            habilitado = disparador != null && !disparando,
            alDisparar = { disparar() },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
        )
    }

    LaunchedEffect(puedeDisparar, capturaAutomatica) {
        if (capturaAutomatica && puedeDisparar && !disparando) disparar()
    }
}

/**
 * El numero grande y su barra.
 *
 * El color va del ambar al verde por la misma escala que usa el contorno: la
 * asociacion "verde es dispara" se aprende en el primer documento y despues ya
 * no hace falta leer el numero.
 */
@Composable
private fun IndicadorDeteccion(
    deteccion: DeteccionDocumento,
    esperandoOtraHoja: Boolean,
    modifier: Modifier,
) {
    val objetivo = deteccion.confianza.coerceIn(0f, 1f)
    val valor by animateFloatAsState(objetivo, label = "confianza")
    val color = colorSegunConfianza(objetivo, deteccion.estable)

    // Tres estados y no dos: "listo" solo cuando ademas esta quieto, porque es
    // justo entonces cuando la foto sale bien y cuando dispara la captura
    // automatica. Decir "listo" con el contorno todavia bailando seria mentir.
    val rotulo = when {
        // Que la automatica este esperando a la hoja siguiente se dice, no se
        // deja adivinar: si no, parece que ha dejado de funcionar.
        esperandoOtraHoja -> stringResource(Res.string.esc_siguiente_hoja)

        objetivo >= DeteccionDocumento.UMBRAL_AUTOMATICO && deteccion.estable ->
            stringResource(Res.string.esc_listo)

        deteccion.hayPapel -> stringResource(Res.string.esc_encuadrando)
        else -> stringResource(Res.string.esc_buscando)
    }
    val porcentaje = stringResource(Res.string.esc_porcentaje, (valor * 100).toInt())
    val etiqueta = stringResource(Res.string.esc_deteccion)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$etiqueta $porcentaje. $rotulo"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = porcentaje,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Text(
            text = rotulo,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { valor },
            color = color,
            trackColor = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.width(120.dp).height(4.dp).clip(CircleShape),
        )
    }
}

@Composable
private fun BotonesDelVisor(
    linterna: Boolean,
    alCambiarLinterna: (Boolean) -> Unit,
    habilitado: Boolean,
    alDisparar: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = { alCambiarLinterna(!linterna) },
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(
                imageVector = if (linterna) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = stringResource(Res.string.esc_linterna),
                tint = if (linterna) Color(0xFFFFD54F) else Color.White,
            )
        }

        // Disparador redondo, grande y centrado: es el gesto que se repite una
        // vez por hoja y tiene que acertarse sin mirar.
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (habilitado) 0.95f else 0.4f))
                .border(4.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .clickable(enabled = habilitado, onClick = alDisparar)
                .semantics { contentDescription = "" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DocumentScanner,
                contentDescription = stringResource(Res.string.esc_capturar),
                tint = Color.Black,
                modifier = Modifier.size(30.dp),
            )
        }

        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun BarraDelEscaner(
    capturadas: Int,
    automatica: Boolean,
    mostrarControles: Boolean,
    alCambiarAutomatica: (Boolean) -> Unit,
    alVolver: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = alVolver, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.comun_cancelar),
                tint = Color.White,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.esc_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            if (capturadas > 0) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.plural_esc_paginas,
                        capturadas,
                        capturadas,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
        if (mostrarControles) {
            // Con el icono solo no habia forma de saber que hacia ni en que
            // estado estaba: se confundia con un adorno y la gente no
            // encontraba como pasar a manual. Con la palabra al lado se lee de
            // un vistazo, y el color dice cual de los dos modos esta puesto.
            val etiqueta = stringResource(Res.string.esc_automatica)
            val modo = if (automatica) {
                stringResource(Res.string.esc_modo_auto)
            } else {
                stringResource(Res.string.esc_modo_manual)
            }
            val color = if (automatica) Color(0xFF4CD964) else Color.White
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { alCambiarAutomatica(!automatica) }
                    .padding(horizontal = 14.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$etiqueta: $modo"
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (automatica) Icons.Filled.AutoAwesome else Icons.Filled.TouchApp,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = modo,
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun PieDelEscaner(
    capturadas: Int,
    alAbrirGaleria: () -> Unit,
    alContinuar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = alAbrirGaleria, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.esc_galeria), color = Color.White)
        }

        Text(
            text = stringResource(Res.string.esc_consejo),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )

        Button(
            onClick = alContinuar,
            enabled = capturadas > 0,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.esc_continuar))
        }
    }
}

@Composable
private fun PanelDePermiso(
    denegado: Boolean,
    alPedir: () -> Unit,
    alAbrirGaleria: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.DocumentScanner,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.esc_permiso_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.esc_permiso_detalle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (!denegado) {
            Button(onClick = alPedir, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.esc_permiso_conceder))
            }
        } else {
            Text(
                text = stringResource(Res.string.esc_permiso_denegado),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = alAbrirGaleria, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(Res.string.esc_galeria), color = Color.White)
        }
    }
}

@Composable
private fun SinCamara(mensaje: String, alAbrirGaleria: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = alAbrirGaleria, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(Res.string.esc_galeria))
        }
    }
}

/** Miniatura de la ultima hoja capturada, para saber que ha entrado. */
@Composable
internal fun MiniaturaCaptura(imagen: ImageBitmap?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.15f)),
    ) {
        if (imagen != null) {
            Image(
                bitmap = imagen,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Ambar mientras no se fia, verde cuando esta encuadrado y quieto. */
private fun colorSegunConfianza(confianza: Float, estable: Boolean): Color = when {
    confianza >= DeteccionDocumento.UMBRAL_AUTOMATICO && estable -> Color(0xFF4CD964)
    confianza >= DeteccionDocumento.UMBRAL_VISIBLE -> Color(0xFFFFB300)
    else -> Color(0xFFB0BEC5)
}

/** Pausa tras un disparo, solo para que el obturador no parpadee dos veces. */
private const val PAUSA_TRAS_DISPARO = 900L

/**
 * Hueco que deja libre el aviso para no taparle el disparador.
 *
 * El aviso va al pie de la imagen y no al de la pantalla: ahi abajo estan
 * "Galeria" y "Continuar", y mientras durase el aviso no se podrian pulsar,
 * justo despues de capturar, que es cuando uno quiere seguir.
 */
private val ALTURA_DEL_DISPARADOR = 96.dp

/**
 * Fotogramas seguidos sin papel que hacen falta para dar la hoja por retirada.
 *
 * A unos quince fotogramas por segundo son poco mas de medio segundo: lo que se
 * tarda en levantar el telefono para cambiar de hoja, y bastante mas de lo que
 * dura un temblor de mano.
 */
private const val FOTOGRAMAS_SIN_PAPEL = 8

