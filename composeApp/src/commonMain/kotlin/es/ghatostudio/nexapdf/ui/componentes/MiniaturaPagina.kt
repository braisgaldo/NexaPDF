package es.ghatostudio.nexapdf.ui.componentes

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.di.LocalContenedor
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf

/**
 * Miniatura de una pagina, rasterizada bajo demanda.
 *
 * Se pide al motor solo cuando la miniatura entra en pantalla y con el ancho que
 * de verdad ocupa: renderizar un documento entero por adelantado, o a resolucion
 * completa, es lo que hace que estas rejillas vayan a tirones.
 */
@Composable
fun MiniaturaPagina(
    ruta: String,
    indice: Int,
    /**
     * Proporcion ancho/alto de la pagina. Si es `null` se deduce de la imagen
     * en cuanto llega, que es lo util cuando aun no se han leido los metadatos
     * del documento (por ejemplo en la lista de documentos a unir).
     */
    proporcion: Float?,
    modifier: Modifier = Modifier,
    anchoPx: Int = 320,
    seleccionada: Boolean = false,
    etiqueta: String? = null,
) {
    val contenedor = LocalContenedor.current
    var imagen by remember(ruta, indice, anchoPx) { mutableStateOf<ImageBitmap?>(null) }
    var fallo by remember(ruta, indice) { mutableStateOf(false) }

    val proporcionEfectiva = proporcion
        ?: imagen?.let { it.width.toFloat() / it.height.toFloat() }
        ?: PROPORCION_A4

    LaunchedEffect(ruta, indice, anchoPx) {
        val resultado = contenedor.motorPdf.renderizarPagina(
            ruta = ruta,
            indice = indice,
            anchoPx = anchoPx,
            miniatura = true,
        )
        when (resultado) {
            is ResultadoPdf.Exito -> imagen = resultado.valor
            is ResultadoPdf.Fallo -> fallo = true
        }
    }

    val bordeColor = if (seleccionada) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .aspectRatio(proporcionEfectiva.coerceIn(0.3f, 3f))
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(
                width = if (seleccionada) 3.dp else 1.dp,
                color = bordeColor,
                shape = RoundedCornerShape(10.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = imagen, label = "miniatura") { mapa ->
            when {
                mapa != null -> Image(
                    bitmap = mapa,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )

                fallo -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )

                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (etiqueta != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                shape = RoundedCornerShape(topStart = 8.dp),
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        if (seleccionada) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp),
            )
        }
    }
}

/** Proporcion de un A4 vertical, mientras no se conoce la real. */
private const val PROPORCION_A4 = 595f / 842f
