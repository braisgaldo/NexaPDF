package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.domain.model.RangoPaginas
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.comun_aceptar
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.sep_anadir_parte
import es.ghatostudio.nexapdf.resources.sep_desde
import es.ghatostudio.nexapdf.resources.sep_hasta
import es.ghatostudio.nexapdf.resources.sep_nombre_parte
import es.ghatostudio.nexapdf.resources.sep_quitar_parte
import es.ghatostudio.nexapdf.resources.sep_titulo
import androidx.compose.foundation.text.KeyboardOptions
import org.jetbrains.compose.resources.stringResource

/** Una parte de la division, con su rango y su nombre. */
data class ParteSeparar(
    val desde: String,
    val hasta: String,
    val nombre: String,
)

/**
 * Elegir en que partes se divide el documento.
 *
 * Separar ofrecia dos cosas: un fichero por pagina, o extraer lo seleccionado.
 * Para partir un contrato en tres capitulos habia que hacerlo tres veces y
 * renombrar a mano cada resultado. Aqui se declaran las partes de una vez, con
 * su rango y su nombre, y salen todas juntas.
 *
 * El nombre se propone como "documento_part-1", que es lo que espera la mayoria
 * y evita tener que teclear nada, pero se puede cambiar por parte.
 */
@Composable
fun DialogoSeparar(
    nombreBase: String,
    totalPaginas: Int,
    alConfirmar: (List<Pair<RangoPaginas, String>>) -> Unit,
    alCancelar: () -> Unit,
) {
    val partes = remember {
        mutableStateListOf(
            ParteSeparar(desde = "1", hasta = "$totalPaginas", nombre = "${nombreBase}_part-1"),
        )
    }

    fun renumerar() {
        // Los nombres que el usuario no ha tocado siguen la numeracion; los
        // que si, se respetan. Se detecta comparando con el patron propuesto.
        partes.forEachIndexed { indice, parte ->
            val propuesto = "${nombreBase}_part-${indice + 1}"
            val eraPropuesto = Regex(
                Regex.escape(nombreBase) + "_part-\\d+",
            ).matches(parte.nombre)
            if (eraPropuesto && parte.nombre != propuesto) {
                partes[indice] = parte.copy(nombre = propuesto)
            }
        }
    }

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(Res.string.sep_titulo)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                partes.forEachIndexed { indice, parte ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = parte.desde,
                            onValueChange = { partes[indice] = parte.copy(desde = it.filter { c -> c.isDigit() }) },
                            label = { Text(stringResource(Res.string.sep_desde)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = parte.hasta,
                            onValueChange = { partes[indice] = parte.copy(hasta = it.filter { c -> c.isDigit() }) },
                            label = { Text(stringResource(Res.string.sep_hasta)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                partes.removeAt(indice)
                                renumerar()
                            },
                            enabled = partes.size > 1,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(Res.string.sep_quitar_parte),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = parte.nombre,
                        onValueChange = { partes[indice] = parte.copy(nombre = it) },
                        label = { Text(stringResource(Res.string.sep_nombre_parte)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                TextButton(
                    onClick = {
                        val siguiente = partes.size + 1
                        partes += ParteSeparar(
                            desde = "1",
                            hasta = "$totalPaginas",
                            nombre = "${nombreBase}_part-$siguiente",
                        )
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.sep_anadir_parte))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Se descartan las partes con numeros imposibles en vez de
                    // avisar: quien deja un campo a medias y pulsa aceptar
                    // quiere las que si estan bien, no un sermon.
                    val validas = partes.mapNotNull { parte ->
                        val desde = parte.desde.toIntOrNull() ?: return@mapNotNull null
                        val hasta = parte.hasta.toIntOrNull() ?: return@mapNotNull null
                        if (desde < 1 || hasta < 1 || desde > totalPaginas) return@mapNotNull null
                        val rango = RangoPaginas(
                            desde = desde - 1,
                            hasta = (hasta - 1).coerceAtMost(totalPaginas - 1),
                        )
                        rango to parte.nombre.ifBlank { "${nombreBase}_part" }
                    }
                    if (validas.isNotEmpty()) alConfirmar(validas)
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.comun_aceptar))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.comun_cancelar))
            }
        },
    )
}
