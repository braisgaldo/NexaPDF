package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.pdf.PermisosPdf
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.cifrar_accion_proteger
import es.ghatostudio.nexapdf.resources.cifrar_accion_quitar
import es.ghatostudio.nexapdf.resources.cifrar_actual
import es.ghatostudio.nexapdf.resources.cifrar_aviso_perdida
import es.ghatostudio.nexapdf.resources.cifrar_contrasena
import es.ghatostudio.nexapdf.resources.cifrar_corta
import es.ghatostudio.nexapdf.resources.cifrar_estado_libre
import es.ghatostudio.nexapdf.resources.cifrar_estado_protegido
import es.ghatostudio.nexapdf.resources.cifrar_fuerza_debil
import es.ghatostudio.nexapdf.resources.cifrar_fuerza_fuerte
import es.ghatostudio.nexapdf.resources.cifrar_fuerza_media
import es.ghatostudio.nexapdf.resources.cifrar_modo_poner
import es.ghatostudio.nexapdf.resources.cifrar_modo_quitar
import es.ghatostudio.nexapdf.resources.cifrar_no_coincide
import es.ghatostudio.nexapdf.resources.cifrar_ocultar
import es.ghatostudio.nexapdf.resources.cifrar_perm_anotar
import es.ghatostudio.nexapdf.resources.cifrar_perm_copiar
import es.ghatostudio.nexapdf.resources.cifrar_perm_imprimir
import es.ghatostudio.nexapdf.resources.cifrar_perm_modificar
import es.ghatostudio.nexapdf.resources.cifrar_permisos_aviso
import es.ghatostudio.nexapdf.resources.cifrar_permisos_titulo
import es.ghatostudio.nexapdf.resources.cifrar_repetir
import es.ghatostudio.nexapdf.resources.cifrar_titulo
import es.ghatostudio.nexapdf.resources.cifrar_ver
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import org.jetbrains.compose.resources.stringResource

/**
 * Poner o quitar la contrasena de un documento.
 *
 * Es una sola pantalla y no un asistente de tres pasos a proposito: proteger un
 * PDF son dos datos (la contrasena y si se repite bien) y una decision opcional
 * sobre permisos. Repartir eso en pasos anadiria pulsaciones sin anadir
 * claridad. Lo que si esta escondido de entrada son los permisos, que la
 * mayoria de las veces valen tal cual vienen.
 *
 * La pantalla evita prometer lo que el formato no cumple. Las casillas de
 * permisos son indicaciones que el lector obedece si quiere, y se dice con esas
 * palabras en lugar de dejar creer que desmarcar "imprimir" impide imprimir.
 */
@Composable
fun PantallaCifrar(
    nombreDocumento: String,
    /** El documento ya pide contrasena para abrirse. */
    yaProtegido: Boolean,
    snackbar: SnackbarHostState,
    alVolver: () -> Unit,
    alProteger: (contrasena: String, permisos: PermisosPdf, contrasenaActual: String) -> Unit,
    alQuitarProteccion: (contrasenaActual: String) -> Unit,
) {
    // Con un documento ya protegido lo que se viene a hacer casi siempre es
    // abrirlo para trabajar con el, asi que ese es el modo de partida.
    var quitando by remember { mutableStateOf(yaProtegido) }
    var actual by remember { mutableStateOf("") }
    var nueva by remember { mutableStateOf("") }
    var repetida by remember { mutableStateOf("") }
    var aLaVista by remember { mutableStateOf(false) }
    var permisosAbiertos by remember { mutableStateOf(false) }
    var permisos by remember { mutableStateOf(PermisosPdf()) }

    val corta = nueva.isNotEmpty() && nueva.length < MINIMO
    val distintas = repetida.isNotEmpty() && repetida != nueva
    val puede = if (quitando) {
        actual.isNotEmpty()
    } else {
        nueva.length >= MINIMO && repetida == nueva && (!yaProtegido || actual.isNotEmpty())
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { relleno ->
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno),
        ) {
            BarraSuperior(stringResource(Res.string.cifrar_titulo), alVolver)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TarjetaDocumento(nombreDocumento, yaProtegido)

                // Los dos modos solo tienen sentido si hay algo que quitar.
                if (yaProtegido) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = quitando,
                            onClick = { quitando = true },
                            label = { Text(stringResource(Res.string.cifrar_modo_quitar)) },
                            modifier = Modifier.heightIn(min = 44.dp),
                        )
                        FilterChip(
                            selected = !quitando,
                            onClick = { quitando = false },
                            label = { Text(stringResource(Res.string.cifrar_modo_poner)) },
                            modifier = Modifier.heightIn(min = 44.dp),
                        )
                    }
                }

                if (yaProtegido) {
                    CampoSecreto(
                        valor = actual,
                        alCambiar = { actual = it },
                        etiqueta = stringResource(Res.string.cifrar_actual),
                        aLaVista = aLaVista,
                        alAlternarVista = { aLaVista = !aLaVista },
                        ultimo = quitando,
                    )
                }

                if (!quitando) {
                    CampoSecreto(
                        valor = nueva,
                        alCambiar = { nueva = it },
                        etiqueta = stringResource(Res.string.cifrar_contrasena),
                        aLaVista = aLaVista,
                        alAlternarVista = { aLaVista = !aLaVista },
                        error = if (corta) stringResource(Res.string.cifrar_corta) else null,
                        ultimo = false,
                    )
                    if (nueva.isNotEmpty()) MedidorDeFuerza(nueva)
                    CampoSecreto(
                        valor = repetida,
                        alCambiar = { repetida = it },
                        etiqueta = stringResource(Res.string.cifrar_repetir),
                        aLaVista = aLaVista,
                        alAlternarVista = { aLaVista = !aLaVista },
                        error = if (distintas) stringResource(Res.string.cifrar_no_coincide) else null,
                        ultimo = true,
                    )

                    Text(
                        text = stringResource(Res.string.cifrar_aviso_perdida),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Permisos(
                        abiertos = permisosAbiertos,
                        alAlternar = { permisosAbiertos = !permisosAbiertos },
                        permisos = permisos,
                        alCambiar = { permisos = it },
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            // El boton fuera de la zona que se desplaza: es la unica cosa que
            // hay que pulsar y no debe irse de la pantalla al abrir permisos.
            Button(
                onClick = {
                    if (quitando) alQuitarProteccion(actual) else alProteger(nueva, permisos, actual)
                },
                enabled = puede,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .heightIn(min = 52.dp),
            ) {
                Icon(
                    imageVector = if (quitando) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(
                        if (quitando) Res.string.cifrar_accion_quitar
                        else Res.string.cifrar_accion_proteger,
                    ),
                )
            }
        }
    }
}

/** Longitud minima. Cuatro es poco, pero menos que eso ya no es contrasena. */
private const val MINIMO = 4

@Composable
private fun TarjetaDocumento(nombre: String, protegido: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (protegido) Icons.Filled.Lock else Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (protegido) Res.string.cifrar_estado_protegido
                        else Res.string.cifrar_estado_libre,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CampoSecreto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    aLaVista: Boolean,
    alAlternarVista: () -> Unit,
    ultimo: Boolean,
    error: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (aLaVista) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (ultimo) ImeAction.Done else ImeAction.Next,
        ),
        trailingIcon = {
            // Un solo interruptor para todos los campos: quien quiere ver lo
            // que escribe lo quiere ver en los dos, y comprobar que la
            // repeticion coincide mirando puntitos no lo comprueba nadie.
            IconButton(onClick = alAlternarVista, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = if (aLaVista) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (aLaVista) Res.string.cifrar_ocultar else Res.string.cifrar_ver,
                    ),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Medidor de fuerza.
 *
 * No pretende puntuar una contrasena de verdad: cuenta longitud y variedad, que
 * es lo que separa "1234" de algo que aguante. Sirve para que quien pone cuatro
 * digitos lo vea, no para bloquearle.
 */
@Composable
private fun MedidorDeFuerza(contrasena: String) {
    val variedad = listOf(
        contrasena.any { it.isLowerCase() },
        contrasena.any { it.isUpperCase() },
        contrasena.any { it.isDigit() },
        contrasena.any { !it.isLetterOrDigit() },
    ).count { it }
    val puntos = when {
        contrasena.length >= 12 && variedad >= 3 -> 3
        contrasena.length >= 8 && variedad >= 2 -> 2
        else -> 1
    }
    val color = when (puntos) {
        3 -> Color(0xFF2E7D32)
        2 -> Color(0xFFEF6C00)
        else -> MaterialTheme.colorScheme.error
    }
    val avance by animateFloatAsState(puntos / 3f, label = "fuerza")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { avance },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
        Text(
            text = stringResource(
                when (puntos) {
                    3 -> Res.string.cifrar_fuerza_fuerte
                    2 -> Res.string.cifrar_fuerza_media
                    else -> Res.string.cifrar_fuerza_debil
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun Permisos(
    abiertos: Boolean,
    alAlternar: () -> Unit,
    permisos: PermisosPdf,
    alCambiar: (PermisosPdf) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = alAlternar)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.cifrar_permisos_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (abiertos) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(abiertos) {
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    Text(
                        text = stringResource(Res.string.cifrar_permisos_aviso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    FilaPermiso(
                        texto = stringResource(Res.string.cifrar_perm_imprimir),
                        marcado = permisos.permitirImprimir,
                        alCambiar = { alCambiar(permisos.copy(permitirImprimir = it)) },
                    )
                    FilaPermiso(
                        texto = stringResource(Res.string.cifrar_perm_copiar),
                        marcado = permisos.permitirCopiar,
                        alCambiar = { alCambiar(permisos.copy(permitirCopiar = it)) },
                    )
                    FilaPermiso(
                        texto = stringResource(Res.string.cifrar_perm_anotar),
                        marcado = permisos.permitirAnotar,
                        alCambiar = { alCambiar(permisos.copy(permitirAnotar = it)) },
                    )
                    FilaPermiso(
                        texto = stringResource(Res.string.cifrar_perm_modificar),
                        marcado = permisos.permitirModificar,
                        alCambiar = { alCambiar(permisos.copy(permitirModificar = it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaPermiso(texto: String, marcado: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { alCambiar(!marcado) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = texto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = marcado, onCheckedChange = alCambiar)
    }
}
