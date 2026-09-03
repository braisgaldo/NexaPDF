package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.inicio_recientes
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes_ayuda
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.EstadoVacio
import org.jetbrains.compose.resources.stringResource

/**
 * Los documentos que la aplicacion ha ido creando.
 *
 * Estaban al final de la pantalla de inicio, donde solo se veian si se
 * desplazaba hasta abajo. Con pantalla propia caben todos y se llega en un
 * toque; al abrir uno se va al visor, que es lo que uno quiere hacer con un
 * documento terminado.
 */
@Composable
fun PantallaRecientes(
    recientes: List<DocumentoReciente>,
    snackbar: SnackbarHostState,
    alAbrir: (DocumentoReciente) -> Unit,
    alVolver: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(
                titulo = stringResource(Res.string.inicio_recientes),
                alVolver = alVolver,
            )
        },
    ) { relleno ->
        if (recientes.isEmpty()) {
            EstadoVacio(
                icono = Icons.Filled.Description,
                titulo = stringResource(Res.string.inicio_sin_recientes),
                detalle = stringResource(Res.string.inicio_sin_recientes_ayuda),
                modifier = Modifier.padding(relleno),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            items(recientes, key = { it.ruta }) { documento ->
                FilaReciente(documento) { alAbrir(documento) }
            }
        }
    }
}
