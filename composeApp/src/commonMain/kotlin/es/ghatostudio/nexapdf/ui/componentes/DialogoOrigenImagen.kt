package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.img_desde_camara
import es.ghatostudio.nexapdf.resources.img_desde_galeria
import es.ghatostudio.nexapdf.resources.img_origen_titulo
import org.jetbrains.compose.resources.stringResource

/**
 * De donde sacar una imagen: de las que ya hay o haciendo una foto.
 *
 * Se pregunta en lugar de abrir siempre la galeria porque los dos casos son
 * igual de habituales: unir fotos que ya se tienen, y digitalizar un papel que
 * esta encima de la mesa. Si el telefono no tiene camara, la opcion no aparece
 * en vez de aparecer y fallar.
 */
@Composable
fun DialogoOrigenImagen(
    hayCamara: Boolean,
    alElegirGaleria: () -> Unit,
    alElegirCamara: () -> Unit,
    alCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.img_origen_titulo)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OpcionOrigen(
                    icono = Icons.Filled.PhotoLibrary,
                    texto = stringResource(Res.string.img_desde_galeria),
                    alPulsar = alElegirGaleria,
                )
                if (hayCamara) {
                    OpcionOrigen(
                        icono = Icons.Filled.PhotoCamera,
                        texto = stringResource(Res.string.img_desde_camara),
                        alPulsar = alElegirCamara,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text(stringResource(Res.string.comun_cancelar))
            }
        },
    )
}

@Composable
private fun OpcionOrigen(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    alPulsar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = alPulsar)
            .padding(vertical = 12.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}
