package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.app_lema
import es.ghatostudio.nexapdf.resources.app_nombre
import es.ghatostudio.nexapdf.resources.cd_ajustes
import es.ghatostudio.nexapdf.resources.herr_convertir_desc
import es.ghatostudio.nexapdf.resources.herr_convertir_titulo
import es.ghatostudio.nexapdf.resources.herr_editar_desc
import es.ghatostudio.nexapdf.resources.herr_editar_titulo
import es.ghatostudio.nexapdf.resources.herr_firmar_desc
import es.ghatostudio.nexapdf.resources.herr_firmar_titulo
import es.ghatostudio.nexapdf.resources.herr_imagen_desc
import es.ghatostudio.nexapdf.resources.herr_imagen_titulo
import es.ghatostudio.nexapdf.resources.herr_separar_desc
import es.ghatostudio.nexapdf.resources.herr_separar_titulo
import es.ghatostudio.nexapdf.resources.herr_unir_desc
import es.ghatostudio.nexapdf.resources.herr_unir_titulo
import es.ghatostudio.nexapdf.resources.herr_varias_imagenes_desc
import es.ghatostudio.nexapdf.resources.herr_varias_imagenes_titulo
import es.ghatostudio.nexapdf.resources.herr_visor_desc
import es.ghatostudio.nexapdf.resources.herr_visor_titulo
import es.ghatostudio.nexapdf.resources.inicio_herramientas
import es.ghatostudio.nexapdf.resources.inicio_recientes
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes
import es.ghatostudio.nexapdf.resources.inicio_sin_recientes_ayuda
import es.ghatostudio.nexapdf.ui.componentes.EstadoVacio
import es.ghatostudio.nexapdf.ui.componentes.TituloSeccion
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/** Las seis herramientas del punto 14 del encargo, en el orden en que se pidieron. */
enum class Herramienta(
    val titulo: StringResource,
    val descripcion: StringResource,
    val icono: ImageVector,
) {
    VISOR(Res.string.herr_visor_titulo, Res.string.herr_visor_desc, Icons.Filled.MenuBook),
    UNIR(Res.string.herr_unir_titulo, Res.string.herr_unir_desc, Icons.AutoMirrored.Filled.MergeType),
    SEPARAR(Res.string.herr_separar_titulo, Res.string.herr_separar_desc, Icons.Filled.ContentCut),
    // Una sola entrada para las fotos. Eran dos, "Imagen a PDF" e
    // "Imagenes a PDF", y llevaban a la misma pantalla: lo unico que
    // cambiaba era si el selector admitia una o varias. El selector de
    // fotos de Android deja elegir una o varias en la misma interfaz, asi
    // que la distincion solo servia para hacer elegir antes de empezar.
    IMAGENES(
        Res.string.herr_varias_imagenes_titulo,
        Res.string.herr_varias_imagenes_desc,
        Icons.Filled.PhotoLibrary,
    ),
    EDITAR(Res.string.herr_editar_titulo, Res.string.herr_editar_desc, Icons.Filled.Edit),
    FIRMAR(Res.string.herr_firmar_titulo, Res.string.herr_firmar_desc, Icons.Filled.Draw),
    CONVERTIR(
        Res.string.herr_convertir_titulo,
        Res.string.herr_convertir_desc,
        Icons.Filled.SwapHoriz,
    ),
}

/** Un documento ya generado, listo para reabrirse. */
data class DocumentoReciente(
    val ruta: String,
    /** Tamano en bytes, para poder ordenar por el. */
    val tamanoBytes: Long = 0L,
    val nombre: String,
    val detalle: String,
)

@Composable
fun PantallaInicio(
    numeroRecientes: Int,
    snackbar: SnackbarHostState,
    alElegirHerramienta: (Herramienta) -> Unit,
    alAbrirRecientes: () -> Unit,
    alAbrirAjustes: () -> Unit,
    /**
     * Donde ha quedado cada elemento en pantalla.
     *
     * Lo usa el tour para iluminar lo que esta explicando. Se mide aqui y
     * no se calcula fuera porque las baldosas se reparten el alto y su
     * posicion depende del telefono.
     */
    alMedirZona: (ZonaTour, Rect) -> Unit = { _, _ -> },
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { relleno ->
        // Rejilla y no lista: con ocho entradas, una lista con descripcion
        // obliga a desplazarse para ver la mitad de lo que la aplicacion sabe
        // hacer, y lo que no se ve no existe. Dos columnas y no tres: con
        // ocho baldosas salen cuatro filas justas, sin huecos, y cada una es
        // lo bastante ancha para que el icono se vea de lejos.
        Column(modifier = Modifier.fillMaxSize().padding(relleno)) {
            Cabecera(alAbrirAjustes, alMedirZona)

            // Rejilla con pesos y no LazyVerticalGrid: son unas pocas entradas
            // fijas, y repartiendo la altura entre las filas la pantalla queda
            // llena en cualquier movil en lugar de dejar un tercio vacio.
            val herramientas = Herramienta.entries.toList()
            val total = herramientas.size + 1
            val filas = (total + COLUMNAS - 1) / COLUMNAS

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onGloballyPositioned { alMedirZona(ZonaTour.BALDOSAS, it.boundsInRoot()) },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                repeat(filas) { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        repeat(COLUMNAS) { columna ->
                            val indice = fila * COLUMNAS + columna
                            val hueco = Modifier.weight(1f).fillMaxHeight()
                            when {
                                indice < herramientas.size -> {
                                    val herramienta = herramientas[indice]
                                    val zona = when (herramienta) {
                                        Herramienta.VISOR -> ZonaTour.LEER
                                        Herramienta.EDITAR -> ZonaTour.EDITAR
                                        else -> null
                                    }
                                    val medido = if (zona == null) {
                                        hueco
                                    } else {
                                        hueco.onGloballyPositioned {
                                            alMedirZona(zona, it.boundsInRoot())
                                        }
                                    }
                                    BaldosaHerramienta(herramienta, medido) {
                                        alElegirHerramienta(herramienta)
                                    }
                                }

                                indice == herramientas.size ->
                                    BaldosaRecientes(
                                        numeroRecientes,
                                        hueco.onGloballyPositioned {
                                            alMedirZona(ZonaTour.RECIENTES, it.boundsInRoot())
                                        },
                                        alAbrirRecientes,
                                    )

                                // Hueco vacio para que la ultima fila no
                                // estire las baldosas que si tiene.
                                else -> Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Una herramienta en la rejilla: icono grande y nombre corto.
 *
 * Sin la linea de descripcion que tenia la lista. La descripcion es util la
 * primera vez y estorba las siguientes cien; para la primera vez esta el tour.
 */
@Composable
private fun BaldosaHerramienta(
    herramienta: Herramienta,
    modifier: Modifier,
    alPulsar: () -> Unit,
) {
    val titulo = stringResource(herramienta.titulo)
    val descripcion = stringResource(herramienta.descripcion)
    Baldosa(
        modifier = modifier,
        icono = herramienta.icono,
        titulo = titulo,
        // El lector de pantalla si lee la descripcion: ahi no estorba y es la
        // unica pista que tiene quien no ve el icono.
        descripcionAccesible = "$titulo. $descripcion",
        alPulsar = alPulsar,
    )
}

@Composable
private fun BaldosaRecientes(cuantos: Int, modifier: Modifier, alPulsar: () -> Unit) {
    val titulo = stringResource(Res.string.inicio_recientes)
    Baldosa(
        modifier = modifier,
        icono = Icons.Filled.History,
        titulo = titulo,
        descripcionAccesible = titulo,
        insignia = cuantos.takeIf { it > 0 },
        alPulsar = alPulsar,
    )
}

@Composable
private fun Baldosa(
    modifier: Modifier,
    icono: ImageVector,
    titulo: String,
    descripcionAccesible: String,
    insignia: Int? = null,
    alPulsar: () -> Unit,
) {
    Card(
        onClick = alPulsar,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = descripcionAccesible },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // El icono se mide contra la baldosa. Con un tamano fijo, en
            // una baldosa que ocupa un cuarto de la pantalla quedaba
            // diminuto en medio de un hueco enorme: no parecia espacioso,
            // parecia vacio.
            val ladoIcono = (minOf(maxWidth, maxHeight) * 0.42f).coerceIn(34.dp, 72.dp)
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ladoIcono),
                )
                if (insignia != null) {
                    Text(
                        text = insignia.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .offset(x = 10.dp, y = (-6).dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        }
    }
}

@Composable
private fun Cabecera(
    alAbrirAjustes: () -> Unit,
    alMedirZona: (ZonaTour, Rect) -> Unit = { _, _ -> },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.app_nombre),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.app_lema),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = alAbrirAjustes,
            modifier = Modifier
                .size(48.dp)
                .onGloballyPositioned { alMedirZona(ZonaTour.AJUSTES, it.boundsInRoot()) },
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(Res.string.cd_ajustes),
            )
        }
    }
}

@Composable
private fun TarjetaHerramienta(herramienta: Herramienta, alPulsar: () -> Unit) {
    val titulo = stringResource(herramienta.titulo)
    val descripcion = stringResource(herramienta.descripcion)

    Card(
        onClick = alPulsar,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .heightIn(min = 76.dp)
            // El lector de pantalla anuncia una sola cosa por tarjeta, no el
            // icono, el titulo y la descripcion por separado.
            .semantics(mergeDescendants = true) { contentDescription = "$titulo. $descripcion" },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = herramienta.icono,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FilaReciente(
    documento: DocumentoReciente,
    alPulsar: () -> Unit,
    /** Mantener pulsado abre lo que se puede hacer con el fichero. */
    alMantener: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(onClick = alPulsar, onLongClick = alMantener)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).clearAndSetSemantics { },
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = documento.nombre,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = documento.detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(0.dp))
}

/** Columnas de la rejilla del inicio. */
private const val COLUMNAS = 2

/** Elementos de la pantalla de inicio que el tour puede senalar. */
enum class ZonaTour { BALDOSAS, LEER, EDITAR, RECIENTES, AJUSTES }
