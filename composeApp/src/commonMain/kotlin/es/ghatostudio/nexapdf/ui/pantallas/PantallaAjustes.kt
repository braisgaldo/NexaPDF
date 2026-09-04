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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
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
import es.ghatostudio.nexapdf.domain.model.ModoGuardado
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
import es.ghatostudio.nexapdf.resources.aj_carpeta_destino
import es.ghatostudio.nexapdf.resources.aj_carpeta_predeterminada
import es.ghatostudio.nexapdf.resources.aj_cuando_guardar
import es.ghatostudio.nexapdf.resources.aj_datos
import es.ghatostudio.nexapdf.resources.aj_elegir_carpeta
import es.ghatostudio.nexapdf.resources.aj_guardar_al_final
import es.ghatostudio.nexapdf.resources.aj_guardar_paso_a_paso
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
import es.ghatostudio.nexapdf.resources.aj_tema_grafito_claro
import es.ghatostudio.nexapdf.resources.aj_tema_grafito_oscuro
import es.ghatostudio.nexapdf.resources.aj_tema_oceano_claro
import es.ghatostudio.nexapdf.resources.aj_tema_oceano_oscuro
import es.ghatostudio.nexapdf.resources.aj_tema_vino_claro
import es.ghatostudio.nexapdf.resources.aj_tema_vino_oscuro
import es.ghatostudio.nexapdf.resources.aj_titulo
import es.ghatostudio.nexapdf.resources.aj_preguntar_compartir
import es.ghatostudio.nexapdf.resources.cd_bandera
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.comun_continuar
import es.ghatostudio.nexapdf.resources.copia_importar_texto
import es.ghatostudio.nexapdf.resources.copia_importar_titulo
import es.ghatostudio.nexapdf.resources.aj_resumen_separar
import es.ghatostudio.nexapdf.resources.aj_resumen_separar_desc
import es.ghatostudio.nexapdf.resources.aj_lectura
import es.ghatostudio.nexapdf.resources.aj_lectura_desc
import es.ghatostudio.nexapdf.resources.aj_lectura_lateral
import es.ghatostudio.nexapdf.resources.aj_lectura_vertical
import es.ghatostudio.nexapdf.resources.aj_pedir_manuscrita
import es.ghatostudio.nexapdf.resources.aj_pedir_manuscrita_desc
import es.ghatostudio.nexapdf.resources.aj_al_terminar
import es.ghatostudio.nexapdf.resources.aj_al_terminar_abrir
import es.ghatostudio.nexapdf.resources.aj_al_terminar_desc
import es.ghatostudio.nexapdf.resources.aj_al_terminar_no
import es.ghatostudio.nexapdf.resources.aj_al_terminar_preguntar
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import es.ghatostudio.nexapdf.domain.model.DireccionLectura
import androidx.compose.material.icons.filled.TaskAlt
import es.ghatostudio.nexapdf.domain.model.AperturaAlTerminar
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import es.ghatostudio.nexapdf.domain.model.TareaConResultado
import es.ghatostudio.nexapdf.resources.aj_tarea_convertir
import es.ghatostudio.nexapdf.resources.aj_tarea_editar
import es.ghatostudio.nexapdf.resources.aj_tarea_firmar
import es.ghatostudio.nexapdf.resources.aj_tarea_imagenes
import es.ghatostudio.nexapdf.resources.aj_tarea_unir
import es.ghatostudio.nexapdf.resources.aj_vista
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import es.ghatostudio.nexapdf.resources.aj_seccion_apariencia
import es.ghatostudio.nexapdf.resources.aj_seccion_avisos
import es.ghatostudio.nexapdf.resources.aj_seccion_copia
import es.ghatostudio.nexapdf.resources.aj_seccion_guardado
import androidx.compose.material.icons.filled.EditNote

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
    alCambiarModoGuardado: (ModoGuardado) -> Unit,
    alCambiarPreguntarCompartir: (Boolean) -> Unit,
    alCambiarResumenSeparar: (Boolean) -> Unit,
    alCambiarPedirManuscrita: (Boolean) -> Unit,
    alCambiarDireccionLectura: (DireccionLectura) -> Unit,
    alCambiarApertura: (TareaConResultado, AperturaAlTerminar) -> Unit,
    alElegirCarpeta: () -> Unit,
    alQuitarCarpeta: () -> Unit,
    nombreCarpeta: String?,
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
    // Solo una seccion abierta a la vez. Con todas desplegadas los ajustes
    // eran cuatro pantallas de desplazamiento y habia que recorrerlas para
    // saber que existia; plegadas caben enteras y se ve de un vistazo todo
    // lo que se puede tocar.
    var abierta by remember { mutableStateOf<Seccion?>(null) }
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
            // --- Aspecto e idioma ---------------------------------------------
            item {
                CabeceraPlegable(
                    icono = Icons.Filled.Palette,
                    titulo = stringResource(Res.string.aj_seccion_apariencia),
                    abierta = abierta == Seccion.APARIENCIA,
                    alPulsar = { abierta = if (abierta == Seccion.APARIENCIA) null else Seccion.APARIENCIA },
                )
            }
            if (abierta == Seccion.APARIENCIA) {
                // Van en desplegables y no en listas abiertas: entre los doce
                // temas y los trece idiomas ocupaban casi toda la pantalla.
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
                item {
                    ComboIdioma(
                        etiquetaActual = ajustes.idioma,
                        alElegir = alCambiarIdioma,
                    )
                }
            }

            // --- Lectura y vista ----------------------------------------------
            item {
                CabeceraPlegable(
                    icono = Icons.AutoMirrored.Filled.MenuBook,
                    titulo = stringResource(Res.string.aj_vista),
                    abierta = abierta == Seccion.VISTA,
                    alPulsar = { abierta = if (abierta == Seccion.VISTA) null else Seccion.VISTA },
                )
            }
            if (abierta == Seccion.VISTA) {
                item {
                    FilaDesplegable(
                        titulo = stringResource(Res.string.aj_lectura),
                        detalle = stringResource(Res.string.aj_lectura_desc),
                        opciones = listOf(
                            DireccionLectura.LATERAL to Res.string.aj_lectura_lateral,
                            DireccionLectura.VERTICAL to Res.string.aj_lectura_vertical,
                        ),
                        elegida = ajustes.lectura,
                        alElegir = alCambiarDireccionLectura,
                    )
                }
                item {
                    FilaDesplegable(
                        titulo = stringResource(Res.string.aj_calidad),
                        detalle = stringResource(Res.string.aj_calidad_desc),
                        opciones = CALIDADES,
                        elegida = ajustes.calidad,
                        alElegir = alCambiarCalidad,
                    )
                }
            }

            // --- Al terminar un documento --------------------------------------
            item {
                CabeceraPlegable(
                    icono = Icons.Filled.TaskAlt,
                    titulo = stringResource(Res.string.aj_al_terminar),
                    abierta = abierta == Seccion.TERMINAR,
                    alPulsar = { abierta = if (abierta == Seccion.TERMINAR) null else Seccion.TERMINAR },
                )
            }
            if (abierta == Seccion.TERMINAR) {
                item {
                    Text(
                        text = stringResource(Res.string.aj_al_terminar_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                items(TAREAS) { (tarea, etiqueta) ->
                    FilaDesplegable(
                        titulo = stringResource(etiqueta),
                        detalle = null,
                        opciones = APERTURAS,
                        elegida = ajustes.apertura(tarea),
                        alElegir = { alCambiarApertura(tarea, it) },
                        compacta = true,
                    )
                }
            }

            // --- Donde se guarda ------------------------------------------------
            item {
                CabeceraPlegable(
                    icono = Icons.Filled.Folder,
                    titulo = stringResource(Res.string.aj_seccion_guardado),
                    abierta = abierta == Seccion.GUARDADO,
                    alPulsar = { abierta = if (abierta == Seccion.GUARDADO) null else Seccion.GUARDADO },
                )
            }
            if (abierta == Seccion.GUARDADO) {
                item {
                    FilaDesplegable(
                        titulo = stringResource(Res.string.aj_cuando_guardar),
                        detalle = null,
                        opciones = listOf(
                            ModoGuardado.PASO_A_PASO to Res.string.aj_guardar_paso_a_paso,
                            ModoGuardado.SOLO_AL_FINAL to Res.string.aj_guardar_al_final,
                        ),
                        elegida = ajustes.guardado,
                        alElegir = alCambiarModoGuardado,
                    )
                }
                item {
                    FilaAccion(
                        icono = Icons.Filled.FolderOpen,
                        titulo = stringResource(Res.string.aj_elegir_carpeta),
                        detalle = stringResource(
                            Res.string.aj_carpeta_destino,
                            nombreCarpeta ?: stringResource(Res.string.aj_carpeta_predeterminada),
                        ),
                        alPulsar = alElegirCarpeta,
                    )
                }
                if (nombreCarpeta != null) {
                    item {
                        TextButton(
                            onClick = alQuitarCarpeta,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(Res.string.aj_carpeta_predeterminada))
                        }
                    }
                }
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
                        titulo = stringResource(Res.string.aj_preguntar_compartir),
                        detalle = null,
                        valor = ajustes.preguntarCompartir,
                        alCambiar = alCambiarPreguntarCompartir,
                    )
                }
            }

            // --- Avisos y firma -------------------------------------------------
            item {
                CabeceraPlegable(
                    icono = Icons.Filled.EditNote,
                    titulo = stringResource(Res.string.aj_seccion_avisos),
                    abierta = abierta == Seccion.AVISOS,
                    alPulsar = { abierta = if (abierta == Seccion.AVISOS) null else Seccion.AVISOS },
                )
            }
            if (abierta == Seccion.AVISOS) {
                item {
                    FilaConmutador(
                        titulo = stringResource(Res.string.aj_confirmar_destructivas),
                        detalle = null,
                        valor = ajustes.confirmarAccionesDestructivas,
                        alCambiar = alCambiarConfirmar,
                    )
                }
                item {
                    FilaConmutador(
                        titulo = stringResource(Res.string.aj_resumen_separar),
                        detalle = stringResource(Res.string.aj_resumen_separar_desc),
                        valor = ajustes.resumenAlSepararEnPartes,
                        alCambiar = alCambiarResumenSeparar,
                    )
                }
                item {
                    FilaConmutador(
                        titulo = stringResource(Res.string.aj_pedir_manuscrita),
                        detalle = stringResource(Res.string.aj_pedir_manuscrita_desc),
                        valor = ajustes.pedirFirmaManuscrita,
                        alCambiar = alCambiarPedirManuscrita,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            // --- Copia de seguridad ---------------------------------------------
            item {
                CabeceraPlegable(
                    icono = Icons.Filled.FileDownload,
                    titulo = stringResource(Res.string.aj_seccion_copia),
                    abierta = abierta == Seccion.COPIA,
                    alPulsar = { abierta = if (abierta == Seccion.COPIA) null else Seccion.COPIA },
                )
            }
            if (abierta == Seccion.COPIA) {
                item {
                    FilaAccion(
                        icono = Icons.Filled.FileDownload,
                        titulo = stringResource(Res.string.aj_exportar),
                        detalle = stringResource(
                            Res.string.aj_exportar_desc,
                            CopiaSeguridad.EXTENSION,
                        ),
                        alPulsar = alExportar,
                    )
                }
                item {
                    FilaAccion(
                        icono = Icons.Filled.FileUpload,
                        titulo = stringResource(Res.string.aj_importar),
                        detalle = stringResource(
                            Res.string.aj_importar_desc,
                            CopiaSeguridad.EXTENSION,
                        ),
                        alPulsar = { confirmandoImportacion = true },
                    )
                }
            }

            // --- Apoyo ----------------------------------------------------------
            item {
                CabeceraPlegable(
                    icono = Icons.Filled.FavoriteBorder,
                    titulo = stringResource(Res.string.aj_apoyo),
                    abierta = abierta == Seccion.APOYO,
                    alPulsar = { abierta = if (abierta == Seccion.APOYO) null else Seccion.APOYO },
                )
            }
            if (abierta == Seccion.APOYO) {
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
            }

            // Ayuda y Acerca de se quedan siempre a la vista: son las dos cosas
            // que se buscan cuando algo no se entiende, y esconderlas dentro de
            // una seccion plegable seria justo lo contrario de lo que hacen.
            item { SeparadorSuave(Modifier.padding(top = 8.dp)) }
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
 * Los temas disponibles: seis familias, cada una en claro y en oscuro.
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
    TemaDeLista(ThemeFamily.OCEANO, oscuro = false, Res.string.aj_tema_oceano_claro),
    TemaDeLista(ThemeFamily.OCEANO, oscuro = true, Res.string.aj_tema_oceano_oscuro),
    TemaDeLista(ThemeFamily.VINO, oscuro = false, Res.string.aj_tema_vino_claro),
    TemaDeLista(ThemeFamily.VINO, oscuro = true, Res.string.aj_tema_vino_oscuro),
    TemaDeLista(ThemeFamily.GRAFITO, oscuro = false, Res.string.aj_tema_grafito_claro),
    TemaDeLista(ThemeFamily.GRAFITO, oscuro = true, Res.string.aj_tema_grafito_oscuro),
)

private val CALIDADES: List<Pair<CalidadVista, StringResource>> = listOf(
    CalidadVista.RAPIDA to Res.string.aj_calidad_rapida,
    CalidadVista.EQUILIBRADA to Res.string.aj_calidad_equilibrada,
    CalidadVista.NITIDA to Res.string.aj_calidad_nitida,
)

/** Tareas que dejan un documento y su rotulo, en el orden en que se usan. */
private val TAREAS: List<Pair<TareaConResultado, StringResource>> = listOf(
    TareaConResultado.EDITAR to Res.string.aj_tarea_editar,
    TareaConResultado.UNIR to Res.string.aj_tarea_unir,
    TareaConResultado.FIRMAR to Res.string.aj_tarea_firmar,
    TareaConResultado.CONVERTIR to Res.string.aj_tarea_convertir,
    TareaConResultado.IMAGENES to Res.string.aj_tarea_imagenes,
)

/** Las tres formas de terminar, en el orden en que se entienden. */
private val APERTURAS: List<Pair<AperturaAlTerminar, StringResource>> = listOf(
    AperturaAlTerminar.ABRIR to Res.string.aj_al_terminar_abrir,
    AperturaAlTerminar.PREGUNTAR to Res.string.aj_al_terminar_preguntar,
    AperturaAlTerminar.NO_ABRIR to Res.string.aj_al_terminar_no,
)

/**
 * Un ajuste de varias opciones, en una sola linea.
 *
 * Antes cada uno de estos ocupaba una fila de botones y un parrafo debajo: tres
 * ajustes se comian una pantalla entera y habia que desplazarse para descubrir
 * que existia el resto. En una linea con el valor a la derecha se lee de un
 * vistazo lo que hay puesto, que es lo que se viene a mirar, y el desplegable
 * solo aparece cuando se va a cambiar.
 */
@Composable
private fun <T> FilaDesplegable(
    titulo: String,
    detalle: String?,
    opciones: List<Pair<T, StringResource>>,
    elegida: T,
    alElegir: (T) -> Unit,
    /** Sin detalle y con menos aire: para listas de varias filas seguidas. */
    compacta: Boolean = false,
) {
    var abierto by remember { mutableStateOf(false) }
    val actual = opciones.firstOrNull { it.first == elegida } ?: opciones.first()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compacta) 52.dp else 60.dp)
            .clickable { abierto = true }
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
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
        Spacer(Modifier.width(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { abierto = true }
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(actual.second),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEach { (valor, etiqueta) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(etiqueta)) },
                        trailingIcon = {
                            if (valor == elegida) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                        onClick = {
                            alElegir(valor)
                            abierto = false
                        },
                    )
                }
            }
        }
    }
}

/** Bloques de ajustes, en el orden en que se despliegan. */
private enum class Seccion { APARIENCIA, VISTA, TERMINAR, GUARDADO, AVISOS, COPIA, APOYO }

/** Cabecera de una seccion que se abre y se cierra al tocarla. */
@Composable
private fun CabeceraPlegable(
    icono: ImageVector,
    titulo: String,
    abierta: Boolean,
    alPulsar: () -> Unit,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = alPulsar)
                .padding(horizontal = 20.dp)
                .semantics(mergeDescendants = true) { },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (abierta) {
                    Icons.Filled.ExpandLess
                } else {
                    Icons.Filled.ExpandMore
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
