package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.escaner.TipoTarjeta
import es.ghatostudio.nexapdf.domain.model.TamanoPagina
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.esc_nombre_etiqueta
import es.ghatostudio.nexapdf.resources.esc_nombre_titulo
import es.ghatostudio.nexapdf.resources.esc_nombre_vacio
import es.ghatostudio.nexapdf.resources.esc_ocr_detalle
import es.ghatostudio.nexapdf.resources.esc_ocr_no_disponible
import es.ghatostudio.nexapdf.resources.esc_ocr_titulo
import es.ghatostudio.nexapdf.resources.esc_rev_crear
import es.ghatostudio.nexapdf.resources.esc_tamano
import es.ghatostudio.nexapdf.resources.esc_tamano_escaneo
import es.ghatostudio.nexapdf.resources.esc_tarjeta_auto
import es.ghatostudio.nexapdf.resources.esc_tarjeta_detalle
import es.ghatostudio.nexapdf.resources.esc_tarjeta_documento
import es.ghatostudio.nexapdf.resources.esc_tarjeta_id1
import es.ghatostudio.nexapdf.resources.esc_tarjeta_id2
import es.ghatostudio.nexapdf.resources.esc_tarjeta_id3
import es.ghatostudio.nexapdf.resources.esc_tarjeta_titulo
import es.ghatostudio.nexapdf.resources.img_tamano_a4
import es.ghatostudio.nexapdf.resources.img_tamano_carta
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Lo que el usuario decide antes de que se escriba el PDF. */
data class OpcionesEscaneo(
    val nombre: String,
    val conTexto: Boolean,
    val tamano: TamanoPagina,
    /**
     * Formato de tarjeta, o `null` para un documento normal.
     *
     * Cuando lleva valor, las caras se montan a **tamano fisico real** sobre una
     * hoja normal en lugar de ocupar cada una su pagina. Es toda la diferencia
     * del modo tarjeta: el recorte y la mejora son los mismos.
     */
    val tarjeta: TipoTarjeta? = null,
)

/**
 * Nombre, texto buscable y tamano de pagina, en un solo paso.
 *
 * El nombre se pide **antes** de crear el fichero y no despues: renombrar un
 * documento ya escrito significa cerrarlo, moverlo y volver a abrirlo, y si
 * entretanto se ha copiado a la carpeta de descargas queda una copia con el
 * nombre viejo. Preguntando antes, el fichero nace con el nombre que se queda.
 */
@Composable
fun DialogoGuardarEscaneo(
    nombreSugerido: String,
    ocrDisponible: Boolean,
    ocrPorDefecto: Boolean,
    alGuardar: (OpcionesEscaneo) -> Unit,
    alCancelar: () -> Unit,
) {
    var nombre by remember { mutableStateOf(nombreSugerido) }
    var conTexto by remember { mutableStateOf(ocrDisponible && ocrPorDefecto) }
    var tamano by remember { mutableStateOf(TamanoPagina.AJUSTAR_A_IMAGEN) }
    var tarjeta by remember { mutableStateOf<TipoTarjeta?>(null) }
    val valido = nombre.isNotBlank()

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.esc_nombre_titulo)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(MAXIMO_NOMBRE) },
                    label = { Text(stringResource(Res.string.esc_nombre_etiqueta)) },
                    singleLine = true,
                    isError = !valido,
                    supportingText = if (valido) {
                        null
                    } else {
                        { Text(stringResource(Res.string.esc_nombre_vacio)) }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.esc_tarjeta_titulo),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FORMATOS.forEach { (valor, etiqueta) ->
                        FilterChip(
                            selected = tarjeta == valor,
                            onClick = { tarjeta = valor },
                            label = { Text(stringResource(etiqueta), maxLines = 1) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

                if (tarjeta != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.esc_tarjeta_detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // El tamano de pagina solo se ofrece para un documento. En modo
                // tarjeta la hoja es siempre una hoja normal: "ajustar a la
                // imagen" daria una pagina del tamano del carne, que es justo lo
                // contrario de lo que este modo sirve.
                if (tarjeta == null) {
                    Text(
                        text = stringResource(Res.string.esc_tamano),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TAMANOS.forEach { (valor, etiqueta) ->
                            FilterChip(
                                selected = tamano == valor,
                                onClick = { tamano = valor },
                                label = { Text(stringResource(etiqueta), maxLines = 1) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.esc_ocr_titulo),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = if (ocrDisponible) {
                                stringResource(Res.string.esc_ocr_detalle)
                            } else {
                                stringResource(Res.string.esc_ocr_no_disponible)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = conTexto,
                        onCheckedChange = { conTexto = it },
                        enabled = ocrDisponible,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = {
                    alGuardar(
                        OpcionesEscaneo(
                            nombre = nombre.trim(),
                            conTexto = conTexto && ocrDisponible,
                            tamano = tamano,
                            tarjeta = tarjeta,
                        ),
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.esc_rev_crear))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.comun_cancelar))
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

private val FORMATOS: List<Pair<TipoTarjeta?, StringResource>> = listOf(
    null to Res.string.esc_tarjeta_documento,
    TipoTarjeta.AUTOMATICO to Res.string.esc_tarjeta_auto,
    TipoTarjeta.ID1 to Res.string.esc_tarjeta_id1,
    TipoTarjeta.ID3 to Res.string.esc_tarjeta_id3,
    TipoTarjeta.ID2 to Res.string.esc_tarjeta_id2,
)

private val TAMANOS: List<Pair<TamanoPagina, StringResource>> = listOf(
    TamanoPagina.AJUSTAR_A_IMAGEN to Res.string.esc_tamano_escaneo,
    TamanoPagina.A4 to Res.string.img_tamano_a4,
    TamanoPagina.CARTA to Res.string.img_tamano_carta,
)

/** Tope del nombre. Los sistemas de ficheros aceptan mas, pero nadie escribe mas. */
private const val MAXIMO_NOMBRE = 80
