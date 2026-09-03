package es.ghatostudio.nexapdf.ui.pantallas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ghatostudio.nexapdf.data.CopiaSeguridad
import es.ghatostudio.nexapdf.domain.model.Ajustes
import es.ghatostudio.nexapdf.domain.model.CalidadVista
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.aj_acerca_de
import es.ghatostudio.nexapdf.resources.aj_apoyar_desarrollo
import es.ghatostudio.nexapdf.resources.aj_apoyo
import es.ghatostudio.nexapdf.resources.aj_ayuda
import es.ghatostudio.nexapdf.resources.aj_calidad
import es.ghatostudio.nexapdf.resources.aj_calidad_desc
import es.ghatostudio.nexapdf.resources.aj_calidad_equilibrada
import es.ghatostudio.nexapdf.resources.aj_calidad_nitida
import es.ghatostudio.nexapdf.resources.aj_calidad_rapida
import es.ghatostudio.nexapdf.resources.aj_compartir_app
import es.ghatostudio.nexapdf.resources.aj_confirmar_destructivas
import es.ghatostudio.nexapdf.resources.aj_datos
import es.ghatostudio.nexapdf.resources.aj_exportar
import es.ghatostudio.nexapdf.resources.aj_exportar_desc
import es.ghatostudio.nexapdf.resources.aj_guardar_descargas
import es.ghatostudio.nexapdf.resources.aj_guardar_descargas_desc
import es.ghatostudio.nexapdf.resources.aj_idioma
import es.ghatostudio.nexapdf.resources.aj_idioma_detalle_fijo
import es.ghatostudio.nexapdf.resources.aj_idioma_detalle_sistema
import es.ghatostudio.nexapdf.resources.aj_idioma_sistema
import es.ghatostudio.nexapdf.resources.aj_importar
import es.ghatostudio.nexapdf.resources.aj_importar_desc
import es.ghatostudio.nexapdf.resources.aj_modo_sistema
import es.ghatostudio.nexapdf.resources.aj_nombre_firmas
import es.ghatostudio.nexapdf.resources.aj_tema
import es.ghatostudio.nexapdf.resources.aj_tema_bosque_claro
import es.ghatostudio.nexapdf.resources.aj_tema_bosque_oscuro
import es.ghatostudio.nexapdf.resources.aj_tema_indigo_claro
import es.ghatostudio.nexapdf.resources.aj_tema_indigo_oscuro
import es.ghatostudio.nexapdf.resources.aj_tema_ocaso_claro
import es.ghatostudio.nexapdf.resources.aj_tema_ocaso_oscuro
import es.ghatostudio.nexapdf.resources.aj_tema_sistema_desc
import es.ghatostudio.nexapdf.resources.aj_titulo
import es.ghatostudio.nexapdf.resources.cd_bandera
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.comun_continuar
import es.ghatostudio.nexapdf.resources.copia_importar_texto
import es.ghatostudio.nexapdf.resources.copia_importar_titulo
import es.ghatostudio.nexapdf.ui.componentes.BarraSuperior
import es.ghatostudio.nexapdf.ui.componentes.SeparadorSuave
import es.ghatostudio.nexapdf.ui.componentes.TituloSeccion
import es.ghatostudio.nexapdf.ui.i18n.BanderaIdioma
import es.ghatostudio.nexapdf.ui.i18n.IconoIdiomaSistema
import es.ghatostudio.nexapdf.ui.i18n.Idioma
import es.ghatostudio.nexapdf.ui.theme.ThemeFamily
import es.ghatostudio.nexapdf.ui.theme.ThemeMode
import es.ghatostudio.nexapdf.ui.theme.esquemaDe
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PantallaAjustes(
    ajustes: Ajustes,
    donacionesDisponibles: Boolean,
    snackbar: SnackbarHostState,
    alCambiarModo: (ThemeMode) -> Unit,
    alCambiarPaleta: (ThemeFamily) -> Unit,
    alCambiarIdioma: (Idioma?) -> Unit,
    alCambiarCalidad: (CalidadVista) -> Unit,
    alCambiarConfirmar: (Boolean) -> Unit,
    alCambiarDescargas: (Boolean) -> Unit,
    alCambiarNombreFirmas: (String) -> Unit,
    alExportar: () -> Unit,
    alImportar: () -> Unit,
    alDonar: () -> Unit,
    alCompartirApp: () -> Unit,
    alAbrirAyuda: () -> Unit,
    alAbrirAcercaDe: () -> Unit,
    alVolver: () -> Unit,
) {
    var confirmandoImportacion by remember { mutableStateOf(false) }
    var nombreFirmas by remember(ajustes.nombreParaFirmas) {
        mutableStateOf(ajustes.nombreParaFirmas)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraSuperior(titulo = stringResource(Res.string.aj_titulo), alVolver = alVolver)
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            // --- Tema y idioma ------------------------------------------------
            // Van en desplegables y no en listas abiertas: entre los siete temas
            // y los catorce idiomas ocupaban casi toda la pantalla de Ajustes y
            // dejaban el resto de opciones fuera de la vista. Cada desplegable
            // muestra lo elegido con su muestra de color o su bandera.
            item { TituloSeccion(stringResource(Res.string.aj_tema)) }
            item {
                ComboTema(
                    familia = ajustes.familia,
                    modo = ajustes.modo,
                    alElegirSistema = { alCambiarModo(ThemeMode.SISTEMA) },
                    alElegirTema = { tema ->
                        alCambiarPaleta(tema.familia)
                        alCambiarModo(if (tema.oscuro) ThemeMode.OSCURO else ThemeMode.CLARO)
                    },
                )
            }

            item { TituloSeccion(stringResource(Res.string.aj_idioma)) }
            item {
                ComboIdioma(
                    etiquetaActual = ajustes.idioma,
                    alElegir = alCambiarIdioma,
                )
            }

            // --- Vista --------------------------------------------------------
            item { TituloSeccion(stringResource(Res.string.aj_calidad)) }
            item {
                FilaFiltros(CALIDADES, ajustes.calidad, alCambiarCalidad)
                Text(
                    text = stringResource(Res.string.aj_calidad_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            // --- Datos --------------------------------------------------------
            item { TituloSeccion(stringResource(Res.string.aj_datos)) }
            item {
                FilaConmutador(
                    titulo = stringResource(Res.string.aj_guardar_descargas),
                    detalle = stringResource(Res.string.aj_guardar_descargas_desc),
                    valor = ajustes.guardarEnDescargasAlTerminar,
                    alCambiar = alCambiarDescargas,
                )
            }
            item {
                FilaConmutador(
                    titulo = stringResource(Res.string.aj_confirmar_destructivas),
                    detalle = null,
                    valor = ajustes.confirmarAccionesDestructivas,
                    alCambiar = alCambiarConfirmar,
                )
            }
            item {
                OutlinedTextField(
                    value = nombreFirmas,
                    onValueChange = {
                        nombreFirmas = it
                        alCambiarNombreFirmas(it)
                    },
                    label = { Text(stringResource(Res.string.aj_nombre_firmas)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item {
                FilaAccion(
                    icono = Icons.Filled.FileDownload,
                    titulo = stringResource(Res.string.aj_exportar),
                    detalle = stringResource(Res.string.aj_exportar_desc, CopiaSeguridad.EXTENSION),
                    alPulsar = alExportar,
                )
            }
            item {
                FilaAccion(
                    icono = Icons.Filled.FileUpload,
                    titulo = stringResource(Res.string.aj_importar),
                    detalle = stringResource(Res.string.aj_importar_desc, CopiaSeguridad.EXTENSION),
                    alPulsar = { confirmandoImportacion = true },
                )
            }

            // --- Apoyo --------------------------------------------------------
            item { TituloSeccion(stringResource(Res.string.aj_apoyo)) }
            if (donacionesDisponibles) {
                item {
                    FilaAccion(
                        icono = Icons.Filled.LocalCafe,
                        titulo = stringResource(Res.string.aj_apoyar_desarrollo),
                        detalle = null,
                        alPulsar = alDonar,
                    )
                }
            }
            item {
                FilaAccion(
                    icono = Icons.Filled.Share,
                    titulo = stringResource(Res.string.aj_compartir_app),
                    detalle = null,
                    alPulsar = alCompartirApp,
                )
            }

            item { SeparadorSuave(Modifier.padding(top = 16.dp)) }
            item {
                FilaAccion(
                    icono = Icons.AutoMirrored.Filled.HelpOutline,
                    titulo = stringResource(Res.string.aj_ayuda),
                    detalle = null,
                    alPulsar = alAbrirAyuda,
                )
            }
            item {
                FilaAccion(
                    icono = Icons.Filled.Info,
                    titulo = stringResource(Res.string.aj_acerca_de),
                    detalle = null,
                    alPulsar = alAbrirAcercaDe,
                )
            }
        }
    }

    if (confirmandoImportacion) {
        AlertDialog(
            onDismissRequest = { confirmandoImportacion = false },
            title = { Text(stringResource(Res.string.copia_importar_titulo)) },
            text = { Text(stringResource(Res.string.copia_importar_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoImportacion = false
                    alImportar()
                }) {
                    Text(stringResource(Res.string.comun_continuar))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoImportacion = false }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }
}

// --- Temas -------------------------------------------------------------------

/**
 * Muestra de un tema: un rectangulo con su color de superficie y tres circulos
 * con sus colores principales, pintados con el esquema real de ese tema. Basta
 * para reconocerlo sin tener que aplicarlo y volver.
 */
@Composable
private fun MuestraTema(familia: ThemeFamily, oscuro: Boolean, modifier: Modifier = Modifier) {
    val esquema = esquemaDe(familia, oscuro)
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(esquema.surface)
            .border(1.dp, esquema.outlineVariant, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(esquema.primary, esquema.secondary, esquema.tertiary).forEach { color ->
                Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(color))
            }
        }
    }
}

/** Muestra partida en dos para la opcion de seguir al sistema. */
@Composable
private fun MuestraSistema(familia: ThemeFamily, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .size(width = 56.dp, height = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
    ) {
        listOf(false, true).forEach { oscuro ->
            val esquema = esquemaDe(familia, oscuro)
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().background(esquema.surface),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(esquema.primary))
            }
        }
    }
}

/**
 * Desplegable generico.
 *
 * Se apoya en `ExposedDropdownMenuBox` de Material 3, que es lo que da el
 * comportamiento que se espera de un desplegable: el menu sale pegado al campo,
 * con su mismo ancho, se coloca solo arriba o abajo segun el sitio que quede y
 * se cierra al tocar fuera. Cada opcion lleva su muestra (color del tema o
 * bandera), que es lo que la hace reconocible sin tener que probarla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Combo(
    nombreActual: String,
    detalleActual: String?,
    muestraActual: @Composable () -> Unit,
    opciones: List<T>,
    esActual: (T) -> Boolean,
    nombreDe: @Composable (T) -> String,
    muestraDe: @Composable (T) -> Unit,
    alElegir: (T) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val giroFlecha by animateFloatAsState(
        targetValue = if (abierto) 180f else 0f,
        label = "flecha",
    )

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = if (abierto) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            muestraActual()
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = nombreActual, style = MaterialTheme.typography.bodyLarge)
                if (detalleActual != null) {
                    Text(
                        text = detalleActual,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(giroFlecha),
            )
        }

        ExposedDropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            // Con catorce idiomas la lista se salia de la pantalla y tapaba el
            // resto de los ajustes: parecia que la pantalla habia cambiado en
            // vez de haberse abierto un desplegable. Acotada, se desplaza
            // dentro y se sigue viendo la caja de la que sale.
            modifier = Modifier.heightIn(max = 340.dp),
        ) {
            opciones.forEach { opcion ->
                val elegida = esActual(opcion)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = nombreDe(opcion),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (elegida) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    leadingIcon = { muestraDe(opcion) },
                    trailingIcon = {
                        if (elegida) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    onClick = {
                        abierto = false
                        alElegir(opcion)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 52.dp),
                )
            }
        }
    }
}

/** Desplegable de tema: seguir al sistema mas los seis temas concretos. */
@Composable
private fun ComboTema(
    familia: ThemeFamily,
    modo: ThemeMode,
    alElegirSistema: () -> Unit,
    alElegirTema: (TemaDeLista) -> Unit,
) {
    val sigueAlSistema = modo == ThemeMode.SISTEMA
    val temaActual = TEMAS.firstOrNull {
        it.familia == familia && it.oscuro == (modo == ThemeMode.OSCURO)
    }

    val opciones = remember { listOf<TemaDeLista?>(null) + TEMAS }

    Combo(
        nombreActual = if (sigueAlSistema || temaActual == null) {
            stringResource(Res.string.aj_modo_sistema)
        } else {
            stringResource(temaActual.nombre)
        },
        detalleActual = if (sigueAlSistema) {
            stringResource(Res.string.aj_tema_sistema_desc)
        } else {
            null
        },
        muestraActual = {
            if (sigueAlSistema || temaActual == null) {
                MuestraSistema(familia)
            } else {
                MuestraTema(temaActual.familia, temaActual.oscuro)
            }
        },
        opciones = opciones,
        esActual = { opcion ->
            if (opcion == null) sigueAlSistema else !sigueAlSistema && opcion == temaActual
        },
        nombreDe = { opcion ->
            if (opcion == null) {
                stringResource(Res.string.aj_modo_sistema)
            } else {
                stringResource(opcion.nombre)
            }
        },
        muestraDe = { opcion ->
            if (opcion == null) {
                MuestraSistema(familia, Modifier.size(width = 44.dp, height = 30.dp))
            } else {
                MuestraTema(opcion.familia, opcion.oscuro, Modifier.size(width = 44.dp, height = 30.dp))
            }
        },
        alElegir = { opcion -> if (opcion == null) alElegirSistema() else alElegirTema(opcion) },
    )
}

/** Desplegable de idioma: el del sistema mas los trece idiomas, con su bandera. */
@Composable
private fun ComboIdioma(etiquetaActual: String?, alElegir: (Idioma?) -> Unit) {
    val actual = Idioma.desdeEtiqueta(etiquetaActual)
    val opciones = remember { listOf<Idioma?>(null) + Idioma.entries }
    val nombreSistema = stringResource(Res.string.aj_idioma_sistema)

    Combo(
        nombreActual = actual?.nombreNativo ?: nombreSistema,
        detalleActual = stringResource(
            if (actual == null) Res.string.aj_idioma_detalle_sistema
            else Res.string.aj_idioma_detalle_fijo,
        ),
        muestraActual = {
            if (actual == null) IconoIdiomaSistema() else BanderaIdioma(actual)
        },
        opciones = opciones,
        esActual = { it == actual },
        nombreDe = { it?.nombreNativo ?: nombreSistema },
        muestraDe = { opcion ->
            if (opcion == null) {
                IconoIdiomaSistema(tamano = 26.dp)
            } else {
                val descripcion = stringResource(Res.string.cd_bandera, opcion.nombreNativo)
                Row(modifier = Modifier.semantics { contentDescription = descripcion }) {
                    BanderaIdioma(opcion, tamano = 26.dp)
                }
            }
        },
        alElegir = alElegir,
    )
}

// --- Resto de filas ----------------------------------------------------------

@Composable
private fun <T> FilaFiltros(
    opciones: List<Pair<T, StringResource>>,
    elegida: T,
    alElegir: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opciones.forEach { (valor, etiqueta) ->
            FilterChip(
                selected = valor == elegida,
                onClick = { alElegir(valor) },
                label = { Text(stringResource(etiqueta), maxLines = 1) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun FilaConmutador(
    titulo: String,
    detalle: String?,
    valor: Boolean,
    alCambiar: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { alCambiar(!valor) }
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = valor, onCheckedChange = alCambiar)
    }
}

@Composable
private fun FilaAccion(
    icono: ImageVector,
    titulo: String,
    detalle: String?,
    alPulsar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = alPulsar)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Un tema concreto de la lista. */
private data class TemaDeLista(
    val familia: ThemeFamily,
    val oscuro: Boolean,
    val nombre: StringResource,
)

/**
 * Los seis temas del punto 4.2 del encargo: tres claros y tres oscuros.
 *
 * Van agrupados por familia para que la version clara y la oscura de una misma
 * paleta queden juntas, que es como se comparan.
 */
private val TEMAS = listOf(
    TemaDeLista(ThemeFamily.INDIGO, oscuro = false, Res.string.aj_tema_indigo_claro),
    TemaDeLista(ThemeFamily.INDIGO, oscuro = true, Res.string.aj_tema_indigo_oscuro),
    TemaDeLista(ThemeFamily.BOSQUE, oscuro = false, Res.string.aj_tema_bosque_claro),
    TemaDeLista(ThemeFamily.BOSQUE, oscuro = true, Res.string.aj_tema_bosque_oscuro),
    TemaDeLista(ThemeFamily.OCASO, oscuro = false, Res.string.aj_tema_ocaso_claro),
    TemaDeLista(ThemeFamily.OCASO, oscuro = true, Res.string.aj_tema_ocaso_oscuro),
)

private val CALIDADES: List<Pair<CalidadVista, StringResource>> = listOf(
    CalidadVista.RAPIDA to Res.string.aj_calidad_rapida,
    CalidadVista.EQUILIBRADA to Res.string.aj_calidad_equilibrada,
    CalidadVista.NITIDA to Res.string.aj_calidad_nitida,
)
