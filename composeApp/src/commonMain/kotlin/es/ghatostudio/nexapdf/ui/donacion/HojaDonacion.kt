package es.ghatostudio.nexapdf.ui.donacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.BuildInfo
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.donar_ahora_no
import es.ghatostudio.nexapdf.resources.donar_boton
import es.ghatostudio.nexapdf.resources.donar_copiar_enlace
import es.ghatostudio.nexapdf.resources.donar_no_mostrar
import es.ghatostudio.nexapdf.resources.donar_otro_dispositivo
import es.ghatostudio.nexapdf.resources.donar_qr_desc
import es.ghatostudio.nexapdf.resources.donar_texto
import es.ghatostudio.nexapdf.resources.donar_titulo
import es.ghatostudio.nexapdf.ui.theme.LocalEsTemaOscuro
import es.ghatostudio.nexapdf.ui.theme.LocalReducirAnimaciones
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Hoja de agradecimiento.
 *
 * Reglas duras que esta pantalla cumple, y que no deben romperse nunca:
 *
 *  - **No desbloquea nada.** Ni funciones, ni temas, ni contenido. Es lo que la
 *    mantiene fuera de la facturacion obligatoria de las tiendas: no se compra
 *    un bien digital, se agradece algo que ya es gratis.
 *  - **Se abre fuera de la app**, en el navegador del sistema. Nunca en un
 *    WebView incrustado, porque eso si parece un flujo de pago interno.
 *  - **No afirma que el pago se haya hecho.** Al volver del navegador solo se
 *    da las gracias por pasarse, porque la app no tiene forma de comprobarlo.
 *  - **Lenguaje sin presion**: nada de comprar, pagar, desbloquear, pro,
 *    premium, suscripcion ni precio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDonacion(
    alCerrar: () -> Unit,
    alDonar: () -> Unit,
    alAplazar: () -> Unit,
    alSilenciar: () -> Unit,
    alCopiarEnlace: () -> Unit,
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = alCerrar,
        sheetState = estado,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        ContenidoDonacion(
            alDonar = alDonar,
            alAplazar = alAplazar,
            alSilenciar = alSilenciar,
            alCopiarEnlace = alCopiarEnlace,
        )
    }
}

@Composable
private fun ContenidoDonacion(
    alDonar: () -> Unit,
    alAplazar: () -> Unit,
    alSilenciar: () -> Unit,
    alCopiarEnlace: () -> Unit,
) {
    val reducir = LocalReducirAnimaciones.current
    var qrVisible by remember { mutableStateOf(false) }

    // Entrada escalonada: cada elemento aparece 40 ms despues del anterior. Con
    // las animaciones reducidas todo se muestra de golpe.
    var paso by remember { mutableStateOf(if (reducir) TOTAL_PASOS else 0) }
    LaunchedEffect(reducir) {
        if (reducir) return@LaunchedEffect
        repeat(TOTAL_PASOS) {
            delay(40)
            paso++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Aparece(paso >= 1) {
            IlustracionCafe(tamano = 128.dp)
        }

        Aparece(paso >= 2) {
            Text(
                text = stringResource(Res.string.donar_titulo),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Aparece(paso >= 3) {
            Text(
                text = stringResource(Res.string.donar_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Aparece(paso >= 4) {
            Button(
                onClick = alDonar,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(top = 22.dp),
            ) {
                Icon(Icons.Filled.LocalCafe, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.donar_boton, IMPORTE_SUGERIDO),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Aparece(paso >= 5) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextButton(
                    onClick = { qrVisible = !qrVisible },
                    modifier = Modifier.heightIn(min = 48.dp).padding(top = 6.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.donar_otro_dispositivo),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (qrVisible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                }

                AnimatedVisibility(
                    visible = qrVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CodigoQr(
                            contenido = BuildInfo.DONATION_URL,
                            descripcion = stringResource(Res.string.donar_qr_desc),
                        )
                        TextButton(
                            onClick = alCopiarEnlace,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.donar_copiar_enlace))
                        }
                    }
                }
            }
        }

        Aparece(paso >= 6) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Las dos acciones secundarias tienen el mismo peso visual a
                // proposito: ninguna de las dos debe parecer la "correcta".
                TextButton(onClick = alAplazar, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        text = stringResource(Res.string.donar_ahora_no),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = alSilenciar, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(
                        text = stringResource(Res.string.donar_no_mostrar),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Muelle suave con desvanecido, o aparicion directa si se reducen animaciones. */
@Composable
private fun Aparece(visible: Boolean, contenido: @Composable () -> Unit) {
    val reducir = LocalReducirAnimaciones.current
    AnimatedVisibility(
        visible = visible,
        enter = if (reducir) {
            fadeIn(tween(0))
        } else {
            fadeIn(tween(220)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        },
    ) {
        contenido()
    }
}

/**
 * Codigo QR pintado con los colores del tema.
 *
 * Los modulos se dibujan sobre un fondo claro fijo aunque el tema sea oscuro:
 * un QR con poco contraste, o invertido, muchos lectores no lo reconocen. El
 * color oscuro si sale del tema, que basta para que la pieza no desentone.
 */
@Composable
fun CodigoQr(
    contenido: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    lado: androidx.compose.ui.unit.Dp = 176.dp,
) {
    val matriz = remember(contenido) {
        runCatching { CodificadorQr.generar(contenido) }.getOrNull()
    } ?: return

    val oscuro = LocalEsTemaOscuro.current
    val colorModulo = if (oscuro) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val colorFondo = if (oscuro) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.White
    }

    Surface(
        modifier = modifier.padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = colorFondo,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Canvas(
                modifier = Modifier
                    .size(lado)
                    .clip(RoundedCornerShape(4.dp))
                    .semantics { contentDescription = descripcion },
            ) {
                val modulo = size.width / matriz.tamano
                for (y in 0 until matriz.tamano) {
                    for (x in 0 until matriz.tamano) {
                        if (!matriz[x, y]) continue
                        drawRect(
                            color = colorModulo,
                            topLeft = Offset(x * modulo, y * modulo),
                            // Un pelo mas grande evita las lineas claras que
                            // deja el redondeo entre modulos contiguos.
                            size = Size(modulo + 0.6f, modulo + 0.6f),
                        )
                    }
                }
            }
        }
    }
}

private fun fadeOut() = androidx.compose.animation.fadeOut(tween(150))

/** Importe sugerido. Solo texto: la app no cobra nada por si misma. */
private const val IMPORTE_SUGERIDO = "1 €"

private const val TOTAL_PASOS = 6

/** Alto reservado para que la hoja no salte al aparecer el QR. */
@Suppress("unused")
private val ALTO_MINIMO_HOJA = 320.dp
