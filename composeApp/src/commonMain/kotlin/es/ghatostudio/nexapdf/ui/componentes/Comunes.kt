package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.cd_volver
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import es.ghatostudio.nexapdf.resources.comun_cancelar
import androidx.compose.foundation.layout.heightIn

/** Barra superior con el boton de volver ya accesible. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(
    titulo: String,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = titulo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            if (alVolver != null) {
                IconButton(onClick = alVolver, modifier = Modifier.size(48.dp)) {
                    Icon(
                        // El icono de volver se refleja solo en arabe: en RTL la
                        // flecha debe apuntar a la derecha.
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.cd_volver),
                    )
                }
            }
        },
        actions = { acciones() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/** Encabezado de una seccion dentro de una lista de ajustes. */
@Composable
fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier,
    icono: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    // Con icono y una linea encima, cada apartado se lee como un bloque
    // aparte en vez de como un rotulo perdido entre filas. En una pantalla de
    // ajustes larga es la diferencia entre encontrar algo y recorrerla entera.
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icono != null) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = texto,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Estado vacio con instrucciones.
 *
 * Una pantalla vacia sin explicacion deja al usuario sin saber si la app esta
 * rota o si es que aun no ha hecho nada.
 */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    modifier: Modifier = Modifier,
    accion: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (accion != null) {
            Spacer(Modifier.height(20.dp))
            accion()
        }
    }
}

/** Velo con indicador mientras hay una operacion en curso. */
@Composable
fun VeloDeTrabajo(
    texto: String?,
    modifier: Modifier = Modifier,
    /** Partes hechas y totales, si la tarea las sabe. */
    progreso: Pair<Int, Int>? = null,
    /** Si se puede cortar, que hacer al pulsar cancelar. */
    alCancelar: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = texto != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = texto.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    if (progreso != null) {
                        val (hechas, total) = progreso
                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { hechas.toFloat() / total.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "$hechas / $total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (alCancelar != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = alCancelar,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(Res.string.comun_cancelar))
                        }
                    }
                }
            }
        }
    }
}

/** Separador fino y discreto entre bloques de una lista. */
@Composable
fun SeparadorSuave(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(MaterialTheme.colorScheme.outlineVariant)
            .clearAndSetSemantics { },
    )
}

/** Relleno estandar de las listas: deja sitio bajo la barra de navegacion. */
val RellenoLista = PaddingValues(bottom = 32.dp)
