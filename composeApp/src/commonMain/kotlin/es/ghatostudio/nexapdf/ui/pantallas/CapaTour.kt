package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.tour_empezar
import es.ghatostudio.nexapdf.resources.tour_saltar
import es.ghatostudio.nexapdf.resources.tour_siguiente
import es.ghatostudio.nexapdf.resources.tour_z_ajustes_c
import es.ghatostudio.nexapdf.resources.tour_z_ajustes_t
import es.ghatostudio.nexapdf.resources.tour_z_baldosas_c
import es.ghatostudio.nexapdf.resources.tour_z_baldosas_t
import es.ghatostudio.nexapdf.resources.tour_z_editar_c
import es.ghatostudio.nexapdf.resources.tour_z_editar_t
import es.ghatostudio.nexapdf.resources.tour_z_leer_c
import es.ghatostudio.nexapdf.resources.tour_z_leer_t
import es.ghatostudio.nexapdf.resources.tour_z_recientes_c
import es.ghatostudio.nexapdf.resources.tour_z_recientes_t
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import es.ghatostudio.nexapdf.resources.tour_z_proteger_c
import es.ghatostudio.nexapdf.resources.tour_z_proteger_t

/** Los pasos del recorrido, en el orden en que se usa la aplicacion. */
private val PASOS: List<Triple<ZonaTour, StringResource, StringResource>> = listOf(
    Triple(ZonaTour.BALDOSAS, Res.string.tour_z_baldosas_t, Res.string.tour_z_baldosas_c),
    Triple(ZonaTour.LEER, Res.string.tour_z_leer_t, Res.string.tour_z_leer_c),
    Triple(ZonaTour.EDITAR, Res.string.tour_z_editar_t, Res.string.tour_z_editar_c),
    Triple(ZonaTour.PROTEGER, Res.string.tour_z_proteger_t, Res.string.tour_z_proteger_c),
    Triple(ZonaTour.RECIENTES, Res.string.tour_z_recientes_t, Res.string.tour_z_recientes_c),
    Triple(ZonaTour.AJUSTES, Res.string.tour_z_ajustes_t, Res.string.tour_z_ajustes_c),
)

/**
 * Recorrido guiado sobre la pantalla de verdad.
 *
 * El tour anterior eran cuatro paginas de texto con un dibujo: contaba lo que la
 * aplicacion hace, pero no ensenaba donde esta, asi que al terminarlo seguias
 * sin saber que la baldosa de leer trae buscador. Aqui se oscurece la pantalla
 * entera menos el elemento del que se habla, que se queda iluminado con su
 * recorte: lo que se explica y lo que se senala son la misma cosa.
 *
 * Se dibuja encima de [PantallaInicio], que sigue viva debajo. Las posiciones
 * las manda ella al medirse, porque las baldosas se reparten el alto y donde
 * cae cada una depende del telefono.
 */
@Composable
fun CapaTour(
    zonas: Map<ZonaTour, Rect>,
    alTerminar: () -> Unit,
) {
    var paso by remember { mutableIntStateOf(0) }
    val (zona, titulo, cuerpo) = PASOS[paso.coerceIn(0, PASOS.lastIndex)]
    val hueco = zonas[zona]
    val densidad = LocalDensity.current
    val ultimo = paso == PASOS.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Se traga los toques: durante el tour la pantalla de debajo se ve
            // pero no se usa, para que nadie acabe a medias en otra pantalla.
            .pointerInput(paso) {},
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sombra = Color(0xD9000000)
            if (hueco == null) {
                drawRect(sombra)
                return@Canvas
            }
            val margen = 8.dp.toPx()
            val marco = Rect(
                left = (hueco.left - margen).coerceAtLeast(0f),
                top = (hueco.top - margen).coerceAtLeast(0f),
                right = (hueco.right + margen).coerceAtMost(size.width),
                bottom = (hueco.bottom + margen).coerceAtMost(size.height),
            )
            // Cuatro rectangulos alrededor en lugar de recortar con mezcla de
            // capas: hace lo mismo, no necesita capa aparte y no depende de que
            // el dispositivo soporte los modos de fusion.
            drawRect(sombra, Offset.Zero, Size(size.width, marco.top))
            drawRect(sombra, Offset(0f, marco.bottom), Size(size.width, size.height - marco.bottom))
            drawRect(sombra, Offset(0f, marco.top), Size(marco.left, marco.height))
            drawRect(
                sombra,
                Offset(marco.right, marco.top),
                Size(size.width - marco.right, marco.height),
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(marco.left, marco.top),
                size = Size(marco.width, marco.height),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // El cartel se va a la mitad contraria de lo que ilumina: tapar justo
        // lo que se esta senalando seria el colmo. Se mide contra el alto real
        // y no contra un numero fijo, que en una pantalla corta caia encima.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val altoPx = with(densidad) { maxHeight.toPx() }
        val centroDelHueco = hueco?.let { (it.top + it.bottom) / 2f } ?: 0f
        val abajo = hueco == null || centroDelHueco < altoPx / 2f
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Sin esto, los botones del cartel caen bajo la barra de
                // navegacion del telefono y no se pueden pulsar.
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(20.dp),
            verticalArrangement = if (abajo) Arrangement.Bottom else Arrangement.Top,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(titulo),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(cuerpo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PASOS.indices.forEach { indice ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(if (indice == paso) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (indice == paso) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        },
                                    ),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = alTerminar,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(Res.string.tour_saltar))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { if (ultimo) alTerminar() else paso++ },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(
                                stringResource(
                                    if (ultimo) Res.string.tour_empezar else Res.string.tour_siguiente,
                                ),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
