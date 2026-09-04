package es.ghatostudio.nexapdf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import es.ghatostudio.nexapdf.BuildInfo
import es.ghatostudio.nexapdf.data.ErrorCopia
import es.ghatostudio.nexapdf.data.ResultadoCopia
import es.ghatostudio.nexapdf.di.ContenedorApp
import es.ghatostudio.nexapdf.di.LocalContenedor
import es.ghatostudio.nexapdf.domain.model.BloqueTexto
import es.ghatostudio.nexapdf.domain.model.BorradorEdicion
import es.ghatostudio.nexapdf.domain.model.DocumentoPdf
import es.ghatostudio.nexapdf.domain.model.EdicionPagina
import es.ghatostudio.nexapdf.domain.model.ModoGuardado
import es.ghatostudio.nexapdf.domain.model.PaginaPdf
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.model.RangoPaginas
import es.ghatostudio.nexapdf.domain.model.Rectangulo
import es.ghatostudio.nexapdf.domain.pdf.AparienciaFirma
import es.ghatostudio.nexapdf.domain.pdf.EntradaUnion
import es.ghatostudio.nexapdf.domain.pdf.ErrorPdf
import es.ghatostudio.nexapdf.domain.pdf.FirmaExistente
import es.ghatostudio.nexapdf.domain.pdf.FormatoDocumento
import es.ghatostudio.nexapdf.domain.pdf.OrigenCertificado
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import es.ghatostudio.nexapdf.domain.pdf.Seccion
import es.ghatostudio.nexapdf.resources.Res
import es.ghatostudio.nexapdf.resources.aj_compartir_texto
import es.ghatostudio.nexapdf.resources.comp_pregunta_titulo
import es.ghatostudio.nexapdf.resources.comun_cancelar
import es.ghatostudio.nexapdf.resources.comun_compartir
import es.ghatostudio.nexapdf.resources.comun_procesando
import es.ghatostudio.nexapdf.resources.copia_error_formato
import es.ghatostudio.nexapdf.resources.copia_error_incompleta
import es.ghatostudio.nexapdf.resources.copia_error_version
import es.ghatostudio.nexapdf.resources.copia_exportada
import es.ghatostudio.nexapdf.resources.copia_importada
import es.ghatostudio.nexapdf.resources.doc_convirtiendo
import es.ghatostudio.nexapdf.resources.doc_ficheros_creados
import es.ghatostudio.nexapdf.resources.doc_resultado_guardado
import es.ghatostudio.nexapdf.resources.donar_enlace_copiado
import es.ghatostudio.nexapdf.resources.donar_gracias
import es.ghatostudio.nexapdf.resources.error_certificado
import es.ghatostudio.nexapdf.resources.error_contrasena
import es.ghatostudio.nexapdf.resources.error_desconocido
import es.ghatostudio.nexapdf.resources.error_escritura
import es.ghatostudio.nexapdf.resources.error_faltan_documentos
import es.ghatostudio.nexapdf.resources.error_fichero_invalido
import es.ghatostudio.nexapdf.resources.error_nada_seleccionado
import es.ghatostudio.nexapdf.resources.error_sin_memoria
import es.ghatostudio.nexapdf.resources.firma_hecha
import es.ghatostudio.nexapdf.resources.img_sin_camara
import es.ghatostudio.nexapdf.ui.componentes.DialogoOrigenImagen
import es.ghatostudio.nexapdf.ui.componentes.VeloDeTrabajo
import es.ghatostudio.nexapdf.ui.donacion.HojaDonacion
import es.ghatostudio.nexapdf.ui.navegacion.Destino
import es.ghatostudio.nexapdf.ui.pantallas.AccionesDocumento
import es.ghatostudio.nexapdf.ui.pantallas.AccionesEditor
import es.ghatostudio.nexapdf.ui.pantallas.AccionesVisor
import es.ghatostudio.nexapdf.ui.pantallas.DocumentoReciente
import es.ghatostudio.nexapdf.ui.pantallas.Herramienta
import es.ghatostudio.nexapdf.ui.pantallas.OpcionesImagenes
import es.ghatostudio.nexapdf.ui.pantallas.PantallaAcercaDe
import es.ghatostudio.nexapdf.ui.pantallas.PantallaAjustes
import es.ghatostudio.nexapdf.ui.pantallas.PantallaAyuda
import es.ghatostudio.nexapdf.ui.pantallas.PantallaCompartir
import es.ghatostudio.nexapdf.ui.pantallas.PantallaDocumento
import es.ghatostudio.nexapdf.ui.pantallas.PantallaEditor
import es.ghatostudio.nexapdf.ui.pantallas.PantallaFirma
import es.ghatostudio.nexapdf.ui.pantallas.PantallaImagenes
import es.ghatostudio.nexapdf.ui.pantallas.PantallaInicio
import es.ghatostudio.nexapdf.ui.pantallas.PantallaRecientes
import es.ghatostudio.nexapdf.ui.pantallas.PantallaVisor
import es.ghatostudio.nexapdf.ui.pantallas.PeticionFirmaCertificado
import es.ghatostudio.nexapdf.ui.theme.NexaTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay
import es.ghatostudio.nexapdf.domain.model.AperturaAlTerminar
import es.ghatostudio.nexapdf.resources.res_abrir
import es.ghatostudio.nexapdf.resources.res_abrir_titulo
import es.ghatostudio.nexapdf.resources.res_ahora_no
import es.ghatostudio.nexapdf.resources.rec_borrado
import es.ghatostudio.nexapdf.resources.rec_renombrado
import es.ghatostudio.nexapdf.domain.model.TareaConResultado
import androidx.compose.ui.geometry.Rect
import es.ghatostudio.nexapdf.ui.pantallas.CapaTour
import es.ghatostudio.nexapdf.ui.pantallas.ZonaTour
import es.ghatostudio.nexapdf.resources.cifrar_hecho
import es.ghatostudio.nexapdf.resources.cifrar_quitado
import es.ghatostudio.nexapdf.resources.cifrar_sufijo
import es.ghatostudio.nexapdf.resources.cifrar_sufijo_sin
import es.ghatostudio.nexapdf.resources.cifrar_trabajando
import es.ghatostudio.nexapdf.ui.pantallas.PantallaCifrar
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import es.ghatostudio.nexapdf.ui.componentes.DialogoContrasena
import es.ghatostudio.nexapdf.resources.plural_comp_pregunta
import org.jetbrains.compose.resources.pluralStringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp

/**
 * Raiz de la interfaz: tema, navegacion, avisos y el hilo que une las pantallas
 * con el motor de PDF.
 *
 * Toda la orquestacion vive aqui a proposito. Las pantallas reciben datos y
 * devuelven eventos, sin saber que existe un motor de PDF ni un selector de
 * ficheros, que es lo que permite probarlas y lo que hace que portarlas a iOS
 * sea cuestion de dar otra implementacion del contenedor.
 */
@Composable
fun NexaPdfApp(contenedor: ContenedorApp, documentoDeEntrada: String? = null) {
    CompositionLocalProvider(LocalContenedor provides contenedor) {
        val estado = remember { EstadoApp(contenedor) }
        val ajustes by estado.ajustes.collectAsState()

        NexaTheme(
            familia = ajustes.familia,
            modo = ajustes.modo,
            reducirAnimaciones = contenedor.servicios.reducirAnimaciones,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                ContenidoApp(contenedor, estado, documentoDeEntrada)
            }
        }
    }
}

@Composable
private fun ContenidoApp(
    contenedor: ContenedorApp,
    estado: EstadoApp,
    documentoDeEntrada: String?,
) {
    val alcance = rememberCoroutineScope()
    // Se usa la retroalimentacion haptica de Compose y no el Vibrator del
    // sistema: esta respeta los ajustes de vibracion del usuario y, sobre
    // todo, no exige el permiso VIBRATE. NexaPDF no declara ninguno.
    val haptica = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val ajustes by estado.ajustes.collectAsState()

    val textoProcesando = stringResource(Res.string.comun_procesando)
    val textoProtegiendo = stringResource(Res.string.cifrar_trabajando)
    val textoConvirtiendo = stringResource(Res.string.doc_convirtiendo)

    // --- Estado de trabajo ---------------------------------------------------
    val documentos = remember { mutableStateListOf<DocumentoPdf>() }
    val paginas = remember { mutableStateListOf<PaginaPdf>() }
    val imagenes = remember { mutableStateListOf<String>() }
    val miniaturasImagen = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Las miniaturas se cargan aqui y no en la pantalla porque sobreviven a
    // ir y volver de la camara: si se recalcularan al recomponer, cada foto
    // nueva volveria a decodificar todas las anteriores.
    LaunchedEffect(imagenes.toList()) {
        imagenes.toList().forEach { ruta ->
            if (miniaturasImagen.containsKey(ruta)) return@forEach
            val resultado = contenedor.motorPdf.renderizarImagen(ruta, ANCHO_MINIATURA_IMAGEN)
            if (resultado is ResultadoPdf.Exito) miniaturasImagen[ruta] = resultado.valor
        }
        // Las que ya no estan en la lista dejan de ocupar memoria.
        val vivas = imagenes.toSet()
        miniaturasImagen.keys.retainAll(vivas)
    }
    var necesitaContrasena by remember { mutableStateOf(false) }
    var rutasBloqueadas by remember { mutableStateOf<List<String>>(emptyList()) }
    var trasDesbloquear by remember { mutableStateOf<(List<DocumentoPdf>) -> Unit>({}) }
    var contrasenaActual by remember { mutableStateOf<String?>(null) }
    var recientes by remember { mutableStateOf(emptyList<DocumentoReciente>()) }

    // Estado del editor
    var paginaEditor by remember { mutableStateOf<ImageBitmap?>(null) }
    var bloquesEditor by remember { mutableStateOf(emptyList<BloqueTexto>()) }
    var aplicarImagen by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var aplicarFirma by remember { mutableStateOf<((List<List<Punto>>) -> Unit)?>(null) }
    // Firma dibujada desde "Firmar PDF" que falta por situar en la pagina.
    var firmaParaColocar by remember { mutableStateOf<List<List<Punto>>?>(null) }

    // Se entro al editor desde el asistente de firma para colocar la
    // rubrica. Al guardar hay que volver al asistente, en el paso del
    // certificado: firmar a mano y firmar con certificado son dos mitades
    // de la misma tarea, y antes la segunda habia que empezarla de cero.
    var siguiendoConCertificado by remember { mutableStateOf(false) }

    // Documento recien creado sobre el que preguntar si se abre.
    var abrirRecienCreado by remember { mutableStateOf<Destino?>(null) }

    // Documento recien creado sobre el que preguntar si se comparte.
    var compartirRecienCreado by remember { mutableStateOf<String?>(null) }

    // Lo mismo cuando la operacion ha dejado varios ficheros: se comparten
    // juntos, en un zip, sin volver a elegirlos uno a uno.
    var compartirVariosRecienCreados by remember { mutableStateOf<List<String>?>(null) }

    // Estado del visor
    var seccionesVisor by remember { mutableStateOf(emptyList<Seccion>()) }

    // Donde ha quedado cada elemento de la pantalla de inicio. Lo rellena ella
    // al medirse y lo lee el tour para saber que iluminar.
    val zonasDelTour = remember { mutableStateMapOf<ZonaTour, Rect>() }

    // Estado de la firma
    var firmasExistentes by remember { mutableStateOf(emptyList<FirmaExistente>()) }
    var certificado by remember { mutableStateOf<Pair<String, OrigenCertificado>?>(null) }

    // De donde sacar las imagenes. `null` = no se esta preguntando; `true` =
    // se pueden elegir varias, `false` = solo una.
    var pidiendoOrigenImagen by remember { mutableStateOf<Boolean?>(null) }

    val rutaActiva = documentos.firstOrNull()?.ruta

    // --- Utilidades ----------------------------------------------------------

    suspend fun mensajeDeError(error: ErrorPdf): String = getString(
        when (error) {
            ErrorPdf.NECESITA_CONTRASENA -> Res.string.error_contrasena
            ErrorPdf.FICHERO_INVALIDO -> Res.string.error_fichero_invalido
            ErrorPdf.SIN_MEMORIA -> Res.string.error_sin_memoria
            ErrorPdf.ERROR_ESCRITURA, ErrorPdf.OPERACION_NO_PERMITIDA -> Res.string.error_escritura
            ErrorPdf.CERTIFICADO_INVALIDO -> Res.string.error_certificado
            ErrorPdf.DESCONOCIDO -> Res.string.error_desconocido
        },
    )

    fun refrescarRecientes() {
        alcance.launch {
            recientes = contenedor.ficheros.listar(contenedor.servicios.directorioSalida)
                .filter { it.endsWith(".pdf", ignoreCase = true) }
                .take(20)
                .map { ruta ->
                    val bytes = contenedor.ficheros.tamano(ruta)
                    DocumentoReciente(
                        ruta = ruta,
                        tamanoBytes = bytes,
                        nombre = contenedor.ficheros.nombre(ruta),
                        detalle = contenedor.servicios.formatearTamano(bytes),
                    )
                }
        }
    }

    LaunchedEffect(Unit) { refrescarRecientes() }

    /** Rellena la lista de paginas del documento activo. */
    suspend fun cargarPaginas(ruta: String) {
        when (val resultado = contenedor.motorPdf.paginas(ruta, contrasenaActual)) {
            is ResultadoPdf.Exito -> {
                paginas.clear()
                paginas.addAll(resultado.valor)
                necesitaContrasena = false
            }

            is ResultadoPdf.Fallo -> {
                if (resultado.causa == ErrorPdf.NECESITA_CONTRASENA) {
                    necesitaContrasena = true
                } else {
                    estado.avisar(mensajeDeError(resultado.causa))
                }
            }
        }
    }

    /** Prepara el espacio de trabajo con los PDF elegidos. */
    /**
     * Abre los documentos y, cuando estan abiertos de verdad, ejecuta [alAbrir].
     *
     * Lo que hay que hacer despues va como continuacion y no como codigo detras
     * de la llamada porque abrir puede pararse a pedir la contrasena: navegar
     * al visor sin esperar dejaba una pantalla girando sobre un documento que
     * nunca llego a abrirse. La continuacion se guarda para repetirla en cuanto
     * se teclee la contrasena.
     *
     * [alAbrir] recibe los documentos ya abiertos, cuya ruta puede no ser la
     * pedida: de uno cifrado se trabaja sobre la copia descifrada.
     */
    fun abrirDocumentos(rutas: List<String>, alAbrir: (List<DocumentoPdf>) -> Unit = {}) {
        rutasBloqueadas = rutas
        trasDesbloquear = alAbrir
        alcance.launch {
            estado.empezarTrabajo(textoProcesando)
            documentos.clear()
            paginas.clear()
            var falta = false
            rutas.forEach { ruta ->
                when (val abierto = contenedor.motorPdf.abrir(ruta, contrasenaActual)) {
                    is ResultadoPdf.Exito -> documentos.add(abierto.valor)
                    is ResultadoPdf.Fallo -> {
                        if (abierto.causa == ErrorPdf.NECESITA_CONTRASENA) {
                            necesitaContrasena = true
                            falta = true
                        } else {
                            estado.avisar(mensajeDeError(abierto.causa))
                        }
                    }
                }
            }
            documentos.firstOrNull()?.let { if (documentos.size == 1) cargarPaginas(it.ruta) }
            estado.terminarTrabajo()
            if (!falta) alAbrir(documentos.toList())
        }
    }

    /**
     * Registra un resultado: lo copia a Descargas si toca, avisa y cuenta como
     * uso real de la app para el aviso de donacion.
     */
    suspend fun registrarResultado(rutaResultado: String) {
        estado.registrarUsoReal()
        refrescarRecientes()

        // Con SOLO_AL_FINAL el fichero se queda en la carpeta privada hasta
        // que el usuario lo guarde o lo comparta a proposito: una tarea larga
        // deja de sembrar de versiones intermedias la carpeta del telefono.
        val sacarloAhora = ajustes.guardado == ModoGuardado.PASO_A_PASO &&
            ajustes.guardarEnDescargasAlTerminar
        if (sacarloAhora) {
            val nombre = contenedor.ficheros.nombre(rutaResultado)
            val carpeta = ajustes.carpetaDestino
            val destino = if (carpeta != null) {
                contenedor.servicios.guardarEnCarpeta(
                    rutaResultado,
                    nombre,
                    "application/pdf",
                    carpeta,
                )
            } else {
                contenedor.servicios.guardarEnDescargas(rutaResultado, nombre, "application/pdf")
            }
            if (destino != null) {
                estado.avisar(getString(Res.string.doc_resultado_guardado, destino))
                return
            }
        }
        estado.avisar(
            getString(Res.string.doc_resultado_guardado, contenedor.ficheros.nombre(rutaResultado)),
        )
        if (ajustes.preguntarCompartir) compartirRecienCreado = rutaResultado
    }

    /**
     * Igual que [registrarResultado] pero para una operacion que deja varios
     * ficheros de golpe: separar en partes o un fichero por pagina.
     *
     * Sin esto los resultados de separar se quedaban en la carpeta privada de
     * la aplicacion aunque el usuario tuviera puesto "guardar paso a paso", y
     * tampoco se ofrecia compartirlos. Se hacia con unir y con extraer, pero no
     * con separar, que es justo la operacion que mas ficheros produce.
     */
    suspend fun registrarResultados(rutas: List<String>) {
        if (rutas.isEmpty()) return
        if (rutas.size == 1) {
            registrarResultado(rutas.first())
            return
        }
        estado.registrarUsoReal()
        refrescarRecientes()

        val sacarlosAhora = ajustes.guardado == ModoGuardado.PASO_A_PASO &&
            ajustes.guardarEnDescargasAlTerminar
        if (sacarlosAhora) {
            val carpeta = ajustes.carpetaDestino
            rutas.forEach { ruta ->
                val nombre = contenedor.ficheros.nombre(ruta)
                if (carpeta != null) {
                    contenedor.servicios.guardarEnCarpeta(ruta, nombre, "application/pdf", carpeta)
                } else {
                    contenedor.servicios.guardarEnDescargas(ruta, nombre, "application/pdf")
                }
            }
        }
        estado.avisar(getString(Res.string.doc_ficheros_creados, rutas.size))
        if (ajustes.preguntarCompartir) compartirVariosRecienCreados = rutas
    }

    /**
     * Lleva a donde se ve el documento recien creado, o no.
     *
     * Abrir siempre estorba a quien encadena tareas; no abrir nunca deja
     * con la duda de si ha salido bien. Lo decide el usuario en los
     * ajustes, y aqui solo se obedece.
     */
    fun mostrarResultado(tarea: TareaConResultado, destinoDelResultado: Destino) {
        when (ajustes.apertura(tarea)) {
            AperturaAlTerminar.ABRIR -> estado.reemplazar(destinoDelResultado)
            AperturaAlTerminar.PREGUNTAR -> abrirRecienCreado = destinoDelResultado
            AperturaAlTerminar.NO_ABRIR -> estado.volverAInicio()
        }
    }

    fun rutaDeSalida(nombre: String): String {
        val carpeta = contenedor.servicios.directorioSalida
        contenedor.ficheros.asegurarDirectorio(carpeta)
        return contenedor.ficheros.unirRuta(carpeta, contenedor.ficheros.nombreLibre(carpeta, nombre))
    }

    fun nombreBase(ruta: String?): String =
        (ruta?.let { contenedor.ficheros.nombre(it) } ?: "NexaPDF.pdf").removeSuffix(".pdf")

    // El boton atras del sistema recorre la pila de destinos de la app. Sin
    // esto cerraria la aplicacion desde cualquier pantalla, que es lo ultimo
    // que espera quien viene de Ajustes. Las pantallas que tienen algo que
    // confirmar antes de salir instalan el suyo propio, que tiene prioridad por
    // estar compuesto mas adentro.
    BackHandler(enabled = estado.puedeVolver) { estado.volver() }

    // Al pasar la app a segundo plano se decide si toca ofrecer la donacion.
    // Es el unico momento en que puede aparecer: nunca al arrancar y nunca
    // encima de una tarea a medias.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { estado.alCerrarSesion() }

    // Y se ensena al volver, ya en la pantalla de inicio: nunca encima de algo
    // a medias.
    // El aviso solo sale cuando la pantalla lleva un rato quieta en Inicio.
    // La espera es lo que evita que aparezca al volver del selector de
    // ficheros, que deja la app en Inicio unas decimas antes de abrir lo que se
    // acaba de elegir.
    // Tambien se exige que no haya nada en marcha: abrir un documento para ver
    // si pide contrasena, o convertir uno grande, deja la app en Inicio mas de
    // ese rato aunque el usuario ya haya pedido otra cosa.
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(estado.destinoActual, estado.donacionPendiente, estado.trabajando) {
        if (!estado.donacionPendiente ||
            estado.destinoActual != Destino.Inicio ||
            estado.trabajando != null
        ) {
            return@LaunchedEffect
        }
        // El rato de quietud solo cuenta con la app delante. Contarlo siempre
        // era el fallo: mientras el selector de ficheros del sistema esta
        // encima, la app sigue "en Inicio" y sin trabajo, asi que el aviso se
        // preparaba a espaldas del usuario y aparecia sobre el documento que
        // acababa de elegir.
        cicloDeVida.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(1500)
            estado.mostrarDonacionSiProcede()
        }
    }

    // --- Aviso pendiente -----------------------------------------------------
    LaunchedEffect(estado.aviso) {
        val aviso = estado.aviso ?: return@LaunchedEffect
        val resultado = snackbar.showSnackbar(
            message = aviso.texto,
            actionLabel = aviso.accion,
            duration = SnackbarDuration.Short,
        )
        if (resultado == SnackbarResult.ActionPerformed) aviso.alPulsar?.invoke()
        estado.avisoMostrado()
    }

    // Un PDF que llega desde el gestor de archivos o desde otra aplicacion se
    // copia al espacio de trabajo y se abre para leer, que es lo que espera
    // quien pulsa "abrir con".
    LaunchedEffect(documentoDeEntrada) {
        val entrada = documentoDeEntrada ?: return@LaunchedEffect
        val adoptado = contenedor.selector.adoptarExterno(entrada) ?: return@LaunchedEffect
        abrirDocumentos(listOf(adoptado.ruta)) { abiertos ->
            val abierto = abiertos.firstOrNull() ?: return@abrirDocumentos
            estado.ir(Destino.Visor(abierto.ruta))
        }
    }

    // El tour se decide con el valor que devuelve el repositorio, no con el que
    // haya en ese instante en el estado: el estado se rellena por su propio
    // colector y, si se mira antes de que llegue, todavia tiene el valor por
    // defecto y el tour vuelve a salir en cada arranque.
    LaunchedEffect(Unit) {
        val guardados = contenedor.ajustes.ajustes.first()
        if (!guardados.tourVisto && estado.destinoActual == Destino.Inicio) {
            estado.ir(Destino.Tour)
        }
    }

    // --- Navegacion ----------------------------------------------------------
    Box(Modifier.fillMaxSize()) {
        when (val destino = estado.destinoActual) {
            Destino.Inicio -> PantallaInicio(
                numeroRecientes = recientes.size,
                snackbar = snackbar,
                alElegirHerramienta = { herramienta ->
                    alcance.launch {
                        when (herramienta) {
                            Herramienta.UNIR -> {
                                // Admite PDF, Word, Excel, PowerPoint e imagenes:
                                // lo que no sea PDF se convierte antes de unir.
                                // Se entra con la lista vacia y se anade desde
                                // dentro. Abrir el selector de golpe obliga a
                                // decidir todos los ficheros antes de ver la
                                // pantalla, que es justo lo que la pantalla
                                // esta hecha para evitar.
                                documentos.clear()
                                paginas.clear()
                                estado.ir(Destino.Documento(emptyList(), modoUnion = true))
                            }

                            Herramienta.VISOR -> {
                                val elegido = contenedor.selector.elegirPdf(multiple = false)
                                    .firstOrNull() ?: return@launch
                                abrirDocumentos(listOf(elegido.ruta)) { abiertos ->
                                    val abierto = abiertos.firstOrNull() ?: return@abrirDocumentos
                                    estado.ir(Destino.Visor(abierto.ruta))
                                }
                            }

                            Herramienta.CONVERTIR -> {
                                // Un solo camino para las dos direcciones: se
                                // elige un fichero y la aplicacion deduce que
                                // toca. Lo que no es PDF se convierte y se
                                // abre; lo que ya es PDF se abre para elegir a
                                // que formato sale.
                                val elegidos = contenedor.selector.elegirParaUnir()
                                if (elegidos.isEmpty()) return@launch
                                val rutas = convertirTodoAPdf(
                                    contenedor = contenedor,
                                    estado = estado,
                                    ficheros = elegidos,
                                    textoConvirtiendo = textoConvirtiendo,
                                    rutaDeSalida = ::rutaDeSalida,
                                    mensajeDeError = { mensajeDeError(it) },
                                )
                                val primera = rutas.firstOrNull() ?: return@launch
                                abrirDocumentos(listOf(primera))
                                mostrarResultado(
                                    TareaConResultado.CONVERTIR,
                                    Destino.Documento(listOf(primera)),
                                )
                            }

                            Herramienta.SEPARAR -> {
                                val elegido = contenedor.selector.elegirPdf(multiple = false)
                                    .firstOrNull() ?: return@launch
                                abrirDocumentos(listOf(elegido.ruta)) { abiertos ->
                                    val abierto = abiertos.firstOrNull() ?: return@abrirDocumentos
                                    estado.ir(Destino.Documento(listOf(abierto.ruta)))
                                }
                            }

                            Herramienta.EDITAR -> {
                                // Quien pulsa "Editar PDF" quiere dibujar, no
                                // ver una rejilla de miniaturas: se entra
                                // directo al editor por la primera pagina y
                                // desde ahi se navega entre ellas.
                                val elegido = contenedor.selector.elegirPdf(multiple = false)
                                    .firstOrNull() ?: return@launch
                                abrirDocumentos(listOf(elegido.ruta)) { abiertos ->
                                    val abierto = abiertos.firstOrNull() ?: return@abrirDocumentos
                                    estado.ir(Destino.Editor(abierto.ruta, 0))
                                }
                            }

                            Herramienta.FIRMAR -> {
                                val elegido = contenedor.selector.elegirPdf(multiple = false)
                                    .firstOrNull() ?: return@launch
                                abrirDocumentos(listOf(elegido.ruta)) { abiertos ->
                                    val abierto = abiertos.firstOrNull() ?: return@abrirDocumentos
                                    alcance.launch {
                                        firmasExistentes = contenedor.motorPdf
                                            .firmasExistentes(abierto.ruta)
                                            .valorONulo().orEmpty()
                                        estado.ir(Destino.Firma(abierto.ruta))
                                    }
                                }
                            }

                            Herramienta.CIFRAR -> {
                                val elegido = contenedor.selector.elegirPdf(multiple = false)
                                    .firstOrNull() ?: return@launch
                                // Se averigua aqui si ya pide contrasena, y no
                                // dentro de la pantalla, porque la unica forma
                                // de saberlo es intentar abrirlo: mejor una vez
                                // al entrar que en cada recomposicion.
                                estado.empezarTrabajo(textoProcesando)
                                val intento = contenedor.motorPdf.abrir(elegido.ruta)
                                estado.terminarTrabajo()
                                val protegido = intento is ResultadoPdf.Fallo &&
                                    intento.causa == ErrorPdf.NECESITA_CONTRASENA
                                estado.ir(Destino.Cifrar(elegido.ruta, protegido))
                            }

                            Herramienta.IMAGENES -> {
                                // Se pregunta de donde sacar las imagenes: de
                                // la galeria o haciendo una foto en el momento.
                                // El selector siempre admite varias: elegir una
                                // sola tambien vale, y asi no hay que decidir
                                // cuantas se van a usar antes de verlas.
                                imagenes.clear()
                                pidiendoOrigenImagen = true
                            }
                        }
                    }
                },
                alAbrirRecientes = { estado.ir(Destino.Recientes) },
                alAbrirAjustes = { estado.ir(Destino.Ajustes) },
            )

            Destino.Compartir -> PantallaCompartir(
                documentos = recientes,
                snackbar = snackbar,
                alCompartir = { rutas ->
                    alcance.launch {
                        contenedor.servicios.compartirVarios(rutas, "NexaPDF.zip")
                        estado.volver()
                    }
                },
                alVolver = { estado.volver() },
            )

            Destino.Recientes -> PantallaRecientes(
                recientes = recientes,
                snackbar = snackbar,
                alAbrir = { reciente ->
                    abrirDocumentos(listOf(reciente.ruta))
                    estado.ir(Destino.Visor(reciente.ruta))
                },
                alRenombrar = { reciente, nombre ->
                    alcance.launch {
                        contenedor.motorPdf.cerrar(reciente.ruta)
                        val nueva = contenedor.ficheros.renombrar(reciente.ruta, nombre)
                        if (nueva == null) {
                            estado.avisar(mensajeDeError(ErrorPdf.ERROR_ESCRITURA))
                        } else {
                            refrescarRecientes()
                            estado.avisar(getString(Res.string.rec_renombrado))
                        }
                    }
                },
                alCompartirVarios = { estado.ir(Destino.Compartir) },
                alBorrar = { reciente ->
                    alcance.launch {
                        contenedor.motorPdf.cerrar(reciente.ruta)
                        if (contenedor.ficheros.borrar(reciente.ruta)) {
                            refrescarRecientes()
                            estado.avisar(getString(Res.string.rec_borrado))
                        } else {
                            estado.avisar(mensajeDeError(ErrorPdf.ERROR_ESCRITURA))
                        }
                    }
                },
                alVolver = { estado.volver() },
            )

            is Destino.Visor -> {
                LaunchedEffect(destino.ruta) {
                    seccionesVisor = contenedor.motorPdf
                        .esquema(destino.ruta, contrasenaActual).valorONulo().orEmpty()
                    firmasExistentes = contenedor.motorPdf
                        .firmasExistentes(destino.ruta).valorONulo().orEmpty()
                }

                PantallaVisor(
                    ruta = destino.ruta,
                    nombreDocumento = contenedor.ficheros.nombre(destino.ruta),
                    paginaActual = destino.pagina,
                    totalPaginas = paginas.size,
                    proporcion = paginas.getOrNull(destino.pagina)?.proporcion ?: 0.707f,
                    anchoRender = (1400 * ajustes.calidad.escala).toInt(),
                    contrasena = contrasenaActual,
                    lectura = ajustes.lectura,
                    secciones = seccionesVisor,
                    firmas = firmasExistentes,
                    formatearFecha = { contenedor.servicios.formatearFecha(it, conHora = true) },
                    snackbar = snackbar,
                    acciones = AccionesVisor(
                        alBuscar = { consulta ->
                            contenedor.motorPdf
                                .buscarTexto(destino.ruta, consulta, contrasenaActual)
                                .valorONulo().orEmpty()
                        },
                        alIrAPagina = { indice ->
                            if (indice in paginas.indices) {
                                estado.reemplazar(Destino.Visor(destino.ruta, indice))
                            }
                        },
                        alCompartir = {
                            contenedor.servicios.compartirFichero(
                                destino.ruta,
                                "application/pdf",
                                contenedor.ficheros.nombre(destino.ruta),
                            )
                        },
                    ),
                    alVolver = { estado.volver() },
                )
            }

            is Destino.Documento -> PantallaDocumento(
                documentos = documentos,
                paginas = paginas,
                rutaActiva = rutaActiva,
                modoUnion = destino.modoUnion,
                desdeUnion = destino.desdeUnion,
                confirmarBorrado = ajustes.confirmarAccionesDestructivas,
                conResumenAlSeparar = ajustes.resumenAlSepararEnPartes,
                snackbar = snackbar,
                alVolver = { estado.volver() },
                acciones = AccionesDocumento(
                    alUnir = {
                        alcance.launch {
                            estado.empezarTrabajo(textoProcesando)
                            val salida = rutaDeSalida("NexaPDF unido.pdf")
                            val resultado = contenedor.motorPdf.unir(
                                documentos.map { EntradaUnion(it.ruta) },
                                salida,
                            )
                            estado.terminarTrabajo()
                            when (resultado) {
                                is ResultadoPdf.Exito -> {
                                    registrarResultado(resultado.valor)
                                    abrirDocumentos(listOf(resultado.valor))
                                    // Segundo paso de la union: ya se puede
                                    // ordenar el conjunto pagina a pagina.
                                    mostrarResultado(
                                        TareaConResultado.UNIR,
                                        Destino.Documento(
                                            rutas = listOf(resultado.valor),
                                            desdeUnion = true,
                                        ),
                                    )
                                }

                                is ResultadoPdf.Fallo ->
                                    estado.avisar(mensajeDeError(resultado.causa))
                            }
                        }
                    },
                    alExtraer = { seleccion ->
                        alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            if (seleccion.isEmpty()) {
                                estado.avisar(getString(Res.string.error_nada_seleccionado))
                                return@launch
                            }
                            estado.empezarTrabajo(textoProcesando)
                            val salida = rutaDeSalida("${nombreBase(ruta)} extraido.pdf")
                            val resultado =
                                contenedor.motorPdf.extraerPaginas(ruta, seleccion, salida)
                            estado.terminarTrabajo()
                            when (resultado) {
                                is ResultadoPdf.Exito -> registrarResultado(resultado.valor)
                                is ResultadoPdf.Fallo ->
                                    estado.avisar(mensajeDeError(resultado.causa))
                            }
                        }
                    },
                    alSepararEnPartes = { partes ->
                        val trabajo = alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            val creados = mutableListOf<String>()
                            // Una llamada por parte, con la ruta de salida ya
                            // decidida: el nombre es el que ha escrito el
                            // usuario, tal cual. `separar` le pegaria detras el
                            // rango de paginas y devolveria "informe_part-1
                            // 1-5.pdf" en vez de "informe_part-1.pdf".
                            partes.forEach { (paginasParte, nombre) ->
                                val salida = rutaDeSalida("${nombre.removeSuffix(".pdf")}.pdf")
                                val resultado = contenedor.motorPdf.extraerPaginas(
                                    ruta,
                                    paginasParte,
                                    salida,
                                )
                                if (resultado is ResultadoPdf.Exito) creados += resultado.valor
                                estado.fijarProgreso(creados.size, partes.size)
                            }
                            estado.terminarTrabajo()
                            if (creados.isEmpty()) {
                                estado.avisar(mensajeDeError(ErrorPdf.ERROR_ESCRITURA))
                            } else {
                                registrarResultados(creados)
                            }
                        }
                        estado.empezarTrabajo(textoProcesando, trabajo)
                    },
                    alSepararTodo = {
                        val trabajo = alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            val rangos = paginas.map { RangoPaginas(it.indice, it.indice) }
                            val resultado = contenedor.motorPdf.separar(
                                ruta = ruta,
                                rangos = rangos,
                                directorioSalida = contenedor.servicios.directorioSalida,
                                nombreBase = nombreBase(ruta),
                                alAvanzar = { hechos, total -> estado.fijarProgreso(hechos, total) },
                            )
                            estado.terminarTrabajo()
                            when (resultado) {
                                is ResultadoPdf.Exito -> registrarResultados(resultado.valor)

                                is ResultadoPdf.Fallo ->
                                    estado.avisar(mensajeDeError(resultado.causa))
                            }
                        }
                        estado.empezarTrabajo(textoProcesando, trabajo)
                    },
                    alGirar = { seleccion, grados ->
                        alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            estado.empezarTrabajo(textoProcesando)
                            val salida = rutaDeSalida("${nombreBase(ruta)} girado.pdf")
                            val resultado = contenedor.motorPdf.reorganizar(
                                ruta = ruta,
                                ordenPaginas = paginas.map { it.indice },
                                rotaciones = seleccion.associateWith { grados },
                                rutaSalida = salida,
                            )
                            estado.terminarTrabajo()
                            when (resultado) {
                                is ResultadoPdf.Exito -> {
                                    registrarResultado(resultado.valor)
                                    contenedor.motorPdf.cerrar(ruta)
                                    abrirDocumentos(listOf(resultado.valor))
                                    estado.reemplazar(Destino.Documento(listOf(resultado.valor)))
                                }

                                is ResultadoPdf.Fallo ->
                                    estado.avisar(mensajeDeError(resultado.causa))
                            }
                        }
                    },
                    alEliminar = { seleccion ->
                        alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            val quedan = paginas.map { it.indice } - seleccion.toSet()
                            if (quedan.isEmpty()) {
                                estado.avisar(getString(Res.string.error_nada_seleccionado))
                                return@launch
                            }
                            estado.empezarTrabajo(textoProcesando)
                            val salida = rutaDeSalida("${nombreBase(ruta)} recortado.pdf")
                            val resultado =
                                contenedor.motorPdf.extraerPaginas(ruta, quedan, salida)
                            estado.terminarTrabajo()
                            when (resultado) {
                                is ResultadoPdf.Exito -> {
                                    registrarResultado(resultado.valor)
                                    contenedor.motorPdf.cerrar(ruta)
                                    abrirDocumentos(listOf(resultado.valor))
                                    estado.reemplazar(Destino.Documento(listOf(resultado.valor)))
                                }

                                is ResultadoPdf.Fallo ->
                                    estado.avisar(mensajeDeError(resultado.causa))
                            }
                        }
                    },
                    alReordenarPaginas = { nuevoOrden ->
                        alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            estado.empezarTrabajo(textoProcesando)
                            val salida = rutaDeSalida("${nombreBase(ruta)} reordenado.pdf")
                            val resultado = contenedor.motorPdf.reorganizar(
                                ruta = ruta,
                                ordenPaginas = nuevoOrden,
                                rotaciones = emptyMap(),
                                rutaSalida = salida,
                            )
                            estado.terminarTrabajo()
                            when (resultado) {
                                is ResultadoPdf.Exito -> {
                                    registrarResultado(resultado.valor)
                                    contenedor.motorPdf.cerrar(ruta)
                                    abrirDocumentos(listOf(resultado.valor))
                                    estado.reemplazar(Destino.Documento(listOf(resultado.valor)))
                                }

                                is ResultadoPdf.Fallo ->
                                    estado.avisar(mensajeDeError(resultado.causa))
                            }
                        }
                    },
                    alEditarPagina = { indice ->
                        val ruta = rutaActiva ?: return@AccionesDocumento
                        estado.ir(Destino.Editor(ruta, indice))
                    },
                    alFirmar = {
                        val ruta = rutaActiva ?: return@AccionesDocumento
                        alcance.launch {
                            firmasExistentes = contenedor.motorPdf.firmasExistentes(ruta)
                                .valorONulo().orEmpty()
                            estado.ir(Destino.Firma(ruta))
                        }
                    },
                    alGuardarComo = {
                        alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            val destino = contenedor.selector.guardarComo(
                                ruta,
                                contenedor.ficheros.nombre(ruta),
                                "application/pdf",
                            )
                            if (destino != null) {
                                estado.avisar(getString(Res.string.doc_resultado_guardado, destino))
                            }
                        }
                    },
                    alCompartir = {
                        val ruta = rutaActiva ?: return@AccionesDocumento
                        contenedor.servicios.compartirFichero(
                            ruta,
                            "application/pdf",
                            contenedor.ficheros.nombre(ruta),
                        )
                    },
                    alAnadirDocumento = {
                        alcance.launch {
                            val elegidos = contenedor.selector.elegirParaUnir()
                            if (elegidos.isEmpty()) return@launch
                            val rutas = convertirTodoAPdf(
                                contenedor = contenedor,
                                estado = estado,
                                ficheros = elegidos,
                                textoConvirtiendo = textoConvirtiendo,
                                rutaDeSalida = ::rutaDeSalida,
                                mensajeDeError = { mensajeDeError(it) },
                            )
                            rutas.forEach { ruta ->
                                contenedor.motorPdf.abrir(ruta).valorONulo()
                                    ?.let { documentos.add(it) }
                            }
                        }
                    },
                    alExportar = { formato ->
                        alcance.launch {
                            val ruta = rutaActiva ?: return@launch
                            estado.empezarTrabajo(textoConvirtiendo)
                            val extension = formato.extensiones.first()
                            val salida = rutaDeSalida("${nombreBase(ruta)}.$extension")
                            val resultado = contenedor.conversor.desdePdf(ruta, formato, salida)
                            estado.terminarTrabajo()

                            when (resultado) {
                                is ResultadoPdf.Exito -> {
                                    estado.registrarUsoReal()
                                    val destino = contenedor.selector.guardarComo(
                                        resultado.valor,
                                        contenedor.ficheros.nombre(resultado.valor),
                                        formato.tiposMime.first(),
                                    )
                                    if (destino != null) {
                                        estado.avisar(
                                            getString(Res.string.doc_resultado_guardado, destino),
                                        )
                                    }
                                }

                                is ResultadoPdf.Fallo ->
                                    estado.avisar(mensajeDeError(resultado.causa))
                            }
                        }
                    },
                    alQuitarDocumento = { posicion ->
                        if (posicion in documentos.indices) documentos.removeAt(posicion)
                    },
                    alMoverDocumento = { desde, hasta ->
                        if (desde in documentos.indices && hasta in documentos.indices) {
                            val movido = documentos.removeAt(desde)
                            documentos.add(hasta, movido)
                        }
                    },
                ),
            )

            is Destino.Imagenes -> PantallaImagenes(
                imagenes = imagenes,
                miniaturas = miniaturasImagen,
                snackbar = snackbar,
                alQuitar = { imagenes.remove(it) },
                alAnadir = { pidiendoOrigenImagen = true },
                alCrear = { opciones -> crearPdfDeImagenes(
                    contenedor = contenedor,
                    estado = estado,
                    alcance = alcance,
                    imagenes = imagenes.toList(),
                    opciones = opciones,
                    textoProcesando = textoProcesando,
                    rutaDeSalida = ::rutaDeSalida,
                    alTerminar = { ruta ->
                        alcance.launch {
                            registrarResultado(ruta)
                            abrirDocumentos(listOf(ruta))
                            mostrarResultado(
                                TareaConResultado.IMAGENES,
                                Destino.Documento(listOf(ruta)),
                            )
                        }
                    },
                    mensajeDeError = { error -> mensajeDeError(error) },
                ) },
                alVolver = { estado.volver() },
            )

            is Destino.Editor -> {
                val pagina = paginas.firstOrNull { it.indice == destino.paginaInicial }

                LaunchedEffect(destino.ruta, destino.paginaInicial, ajustes.calidad) {
                    paginaEditor = null
                    val ancho = (1400 * ajustes.calidad.escala).toInt()
                    paginaEditor = contenedor.motorPdf
                        .renderizarPagina(destino.ruta, destino.paginaInicial, ancho, contrasenaActual)
                        .valorONulo()
                    bloquesEditor = contenedor.motorPdf
                        .bloquesDeTexto(destino.ruta, destino.paginaInicial, contrasenaActual)
                        .valorONulo().orEmpty()
                }

                PantallaEditor(
                    ruta = destino.ruta,
                    indicePagina = destino.paginaInicial,
                    totalPaginas = paginas.size,
                    proporcion = pagina?.proporcion ?: 0.707f,
                    pagina = paginaEditor,
                    bloquesTexto = bloquesEditor,
                    firmaPendiente = firmaParaColocar,
                    cargarImagen = { deDonde ->
                        contenedor.motorPdf.renderizarImagen(deDonde, ANCHO_IMAGEN_EDITOR)
                            .valorONulo()
                    },
                    snackbar = snackbar,
                    alVolver = {
                        firmaParaColocar = null
                        siguiendoConCertificado = false
                        estado.volver()
                    },
                    acciones = AccionesEditor(
                        alGuardar = { edicion ->
                            alcance.launch {
                                guardarEdicion(
                                    contenedor = contenedor,
                                    estado = estado,
                                    ruta = destino.ruta,
                                    edicion = edicion,
                                    contrasena = contrasenaActual,
                                    textoProcesando = textoProcesando,
                                    salida = rutaDeSalida("${nombreBase(destino.ruta)} editado.pdf"),
                                    alTerminar = { rutaFinal ->
                                        alcance.launch {
                                            registrarResultado(rutaFinal)
                                            contenedor.motorPdf.cerrar(destino.ruta)
                                            abrirDocumentos(listOf(rutaFinal))
                                            if (siguiendoConCertificado) {
                                                siguiendoConCertificado = false
                                                estado.reemplazar(
                                                    Destino.Firma(rutaFinal, manuscritaHecha = true),
                                                )
                                            } else {
                                                mostrarResultado(
                                                    TareaConResultado.EDITAR,
                                                    Destino.Documento(listOf(rutaFinal)),
                                                )
                                            }
                                        }
                                    },
                                    mensajeDeError = { error -> mensajeDeError(error) },
                                )
                            }
                        },
                        alElegirImagen = { aplicar ->
                            aplicarImagen = aplicar
                            alcance.launch {
                                val elegida = contenedor.selector.elegirImagenes(multiple = false)
                                    .firstOrNull()
                                if (elegida != null) aplicarImagen?.invoke(elegida.ruta)
                                aplicarImagen = null
                            }
                        },
                        alPedirFirma = { aplicar ->
                            aplicarFirma = aplicar
                            estado.ir(Destino.Firma(destino.ruta))
                        },
                        alIrAPagina = { indice ->
                            if (indice in paginas.indices) {
                                estado.reemplazar(Destino.Editor(destino.ruta, indice))
                            }
                        },
                    ),
                )
            }

            is Destino.Firma -> PantallaFirma(
                rutaDocumento = destino.ruta,
                nombreDocumento = contenedor.ficheros.nombre(destino.ruta),
                firmasExistentes = firmasExistentes,
                nombreCertificado = certificado?.first,
                certificadoPideContrasena =
                    certificado?.second is OrigenCertificado.Fichero,
                hayAlmacenDeClaves = contenedor.selector.hayAlmacenDeClaves(),
                manuscritaHecha = destino.manuscritaHecha,
                pedirManuscrita = ajustes.pedirFirmaManuscrita,
                nombreSugerido = ajustes.nombreParaFirmas,
                snackbar = snackbar,
                alVolver = { estado.volver() },
                alColocarManuscrita = { trazos ->
                    val aplicar = aplicarFirma
                    if (aplicar != null) {
                        // Se venia del editor: alli ya se habia marcado donde
                        // va la firma antes de dibujarla.
                        aplicar(trazos)
                        aplicarFirma = null
                        estado.volver()
                    } else {
                        // Se entro por "Firmar PDF" desde el inicio. Antes la
                        // firma se estampaba siempre abajo a la derecha de la
                        // primera pagina y no habia forma de moverla; ahora se
                        // abre el editor con la firma en la mano para que el
                        // usuario marque donde cae y en que pagina.
                        firmaParaColocar = trazos
                        siguiendoConCertificado = true
                        estado.reemplazar(Destino.Editor(destino.ruta, 0))
                    }
                },
                alElegirCertificadoDeFichero = {
                    alcance.launch {
                        val elegido = contenedor.selector.elegirCertificado() ?: return@launch
                        val contenido = contenedor.ficheros.leerBytes(elegido.ruta)
                        if (contenido == null) {
                            estado.avisar(getString(Res.string.error_certificado))
                        } else {
                            // La contrasena se rellena al pulsar Firmar: aqui
                            // solo queda apuntado de donde salen los bytes.
                            certificado = elegido.nombre to
                                OrigenCertificado.Fichero(contenido, "")
                        }
                    }
                },
                alElegirCertificadoDelSistema = {
                    alcance.launch {
                        val alias = contenedor.selector.elegirDelAlmacenDeClaves()
                        if (alias == null) {
                            estado.avisar(getString(Res.string.error_certificado))
                        } else {
                            certificado = alias to OrigenCertificado.AlmacenDelSistema(alias)
                        }
                    }
                },
                alFirmarConCertificado = { peticion ->
                    alcance.launch {
                        val credenciales = certificado ?: return@launch
                        // La contrasena la escribe el usuario justo ahora, asi
                        // que el origen de fichero se rehace con ella; el del
                        // almacen del sistema no la necesita.
                        val origen = when (val elegido = credenciales.second) {
                            is OrigenCertificado.Fichero ->
                                OrigenCertificado.Fichero(elegido.contenido, peticion.contrasena)

                            is OrigenCertificado.AlmacenDelSistema -> elegido
                        }
                        estado.empezarTrabajo(textoProcesando)
                        val salida = rutaDeSalida("${nombreBase(destino.ruta)} firmado.pdf")
                        val resultado = contenedor.motorPdf.firmarConCertificado(
                            ruta = destino.ruta,
                            origen = origen,
                            apariencia = null as AparienciaFirma?,
                            motivo = peticion.motivo,
                            lugar = peticion.lugar,
                            rutaSalida = salida,
                        )
                        estado.terminarTrabajo()
                        when (resultado) {
                            is ResultadoPdf.Exito -> {
                                // Por aqui pasaba de largo el registro del
                                // resultado: el documento firmado se quedaba en
                                // la carpeta privada, no llegaba a la que el
                                // usuario habia elegido y tampoco se ofrecia
                                // compartirlo. Justo lo que uno quiere hacer
                                // nada mas firmar algo.
                                registrarResultado(resultado.valor)
                                estado.avisar(getString(Res.string.firma_hecha))
                                abrirDocumentos(listOf(resultado.valor))
                                mostrarResultado(
                                    TareaConResultado.FIRMAR,
                                    Destino.Documento(listOf(resultado.valor)),
                                )
                            }

                            is ResultadoPdf.Fallo -> estado.avisar(mensajeDeError(resultado.causa))
                        }
                    }
                },
            )

            // El tour va encima de la pantalla de inicio de verdad, no en una
            // pantalla aparte con dibujos: lo que se explica y lo que se
            // ilumina tienen que ser la misma cosa.
            Destino.Tour -> Box(Modifier.fillMaxSize()) {
                PantallaInicio(
                    numeroRecientes = recientes.size,
                    snackbar = snackbar,
                    alElegirHerramienta = {},
                    alAbrirRecientes = {},
                    alAbrirAjustes = {},
                    alMedirZona = { zona, marco -> zonasDelTour[zona] = marco },
                )
                CapaTour(
                    zonas = zonasDelTour,
                    alTerminar = {
                        alcance.launch { contenedor.ajustes.marcarTourVisto() }
                        estado.volver()
                    },
                )
            }

            is Destino.Cifrar -> PantallaCifrar(
                nombreDocumento = contenedor.ficheros.nombre(destino.ruta),
                yaProtegido = destino.yaProtegido,
                snackbar = snackbar,
                alVolver = { estado.volver() },
                alProteger = { contrasena, permisos, actual ->
                    alcance.launch {
                        estado.empezarTrabajo(textoProtegiendo)
                        val salida = rutaDeSalida(
                            nombreBase(destino.ruta) + " " + getString(Res.string.cifrar_sufijo) + ".pdf",
                        )
                        val hecho = contenedor.motorPdf.cifrar(
                            ruta = destino.ruta,
                            contrasenaApertura = contrasena,
                            contrasenaPermisos = "",
                            permisos = permisos,
                            rutaSalida = salida,
                            contrasenaActual = actual.ifBlank { null },
                        )
                        estado.terminarTrabajo()
                        when (hecho) {
                            is ResultadoPdf.Exito -> {
                                registrarResultado(hecho.valor)
                                estado.avisar(getString(Res.string.cifrar_hecho))
                                // Se abre con la contrasena que se acaba de
                                // teclear en lugar de volver a pedirla: es la
                                // unica forma de que "abrir al terminar" sirva
                                // de algo aqui, y comprobar que el documento
                                // sigue estando bien es justo lo que uno quiere
                                // hacer despues de cerrarlo con llave.
                                contrasenaActual = contrasena
                                abrirDocumentos(listOf(hecho.valor)) { abiertos ->
                                    val abierto = abiertos.firstOrNull()
                                        ?: return@abrirDocumentos
                                    mostrarResultado(
                                        TareaConResultado.CIFRAR,
                                        Destino.Documento(listOf(abierto.ruta)),
                                    )
                                }
                            }

                            is ResultadoPdf.Fallo ->
                                estado.avisar(mensajeDeError(hecho.causa))
                        }
                    }
                },
                alQuitarProteccion = { actual ->
                    alcance.launch {
                        estado.empezarTrabajo(textoProtegiendo)
                        val salida = rutaDeSalida(
                            nombreBase(destino.ruta) + " " +
                                getString(Res.string.cifrar_sufijo_sin) + ".pdf",
                        )
                        val hecho = contenedor.motorPdf.descifrar(destino.ruta, actual, salida)
                        estado.terminarTrabajo()
                        when (hecho) {
                            is ResultadoPdf.Exito -> {
                                registrarResultado(hecho.valor)
                                estado.avisar(getString(Res.string.cifrar_quitado))
                                abrirDocumentos(listOf(hecho.valor))
                                mostrarResultado(
                                    TareaConResultado.CIFRAR,
                                    Destino.Documento(listOf(hecho.valor)),
                                )
                            }

                            is ResultadoPdf.Fallo ->
                                estado.avisar(mensajeDeError(hecho.causa))
                        }
                    }
                },
            )

            Destino.Ajustes -> PantallaAjustes(
                ajustes = ajustes,
                donacionesDisponibles = contenedor.servicios.donacionesDisponibles,
                snackbar = snackbar,
                alCambiarModo = { estado.fijarModoTema(it.name) },
                alCambiarPaleta = { estado.fijarFamiliaTema(it.name) },
                alCambiarIdioma = { estado.fijarIdioma(it) },
                alCambiarCalidad = { estado.fijarCalidadVista(it.name) },
                alCambiarConfirmar = { estado.fijarConfirmarDestructivas(it) },
                alCambiarDescargas = { estado.fijarGuardarEnDescargas(it) },
                alCambiarModoGuardado = { estado.fijarModoGuardado(it) },
                alCambiarPreguntarCompartir = { estado.fijarPreguntarCompartir(it) },
                alCambiarResumenSeparar = { estado.fijarResumenAlSeparar(it) },
                alCambiarPedirManuscrita = { estado.fijarPedirFirmaManuscrita(it) },
                alCambiarDireccionLectura = { estado.fijarDireccionLectura(it.name) },
                alCambiarApertura = { tarea, valor ->
                    estado.fijarApertura(tarea, valor.name)
                },
                alElegirCarpeta = {
                    alcance.launch {
                        val elegida = contenedor.selector.elegirCarpeta()
                        if (elegida != null) estado.fijarCarpetaDestino(elegida)
                    }
                },
                alQuitarCarpeta = { estado.fijarCarpetaDestino(null) },
                nombreCarpeta = ajustes.carpetaDestino
                    ?.let { contenedor.selector.nombreDeCarpeta(it) },
                alCambiarNombreFirmas = { estado.fijarNombreParaFirmas(it) },
                alExportar = {
                    alcance.launch {
                        exportarCopia(contenedor, estado)
                    }
                },
                alImportar = {
                    alcance.launch {
                        importarCopia(contenedor, estado)
                    }
                },
                alDonar = { estado.abrirDonacion() },
                alCompartirApp = {
                    alcance.launch {
                        contenedor.servicios.compartirTexto(
                            getString(Res.string.aj_compartir_texto, BuildInfo.PLAY_STORE_URL),
                            null,
                        )
                    }
                },
                alAbrirAyuda = { estado.ir(Destino.Ayuda) },
                alAbrirAcercaDe = { estado.ir(Destino.AcercaDe) },
                alVolver = { estado.volver() },
            )

            Destino.Ayuda -> PantallaAyuda(
                snackbar = snackbar,
                alVerTour = { estado.ir(Destino.Tour) },
                alVolver = { estado.volver() },
            )

            Destino.AcercaDe -> PantallaAcercaDe(
                plataforma = contenedor.servicios.nombrePlataforma,
                snackbar = snackbar,
                alAbrirEnlace = { contenedor.servicios.abrirEnNavegador(it) },
                alVolver = { estado.volver() },
            )
        }

        VeloDeTrabajo(
            texto = estado.trabajando,
            modifier = Modifier.fillMaxSize(),
            progreso = estado.progreso,
            alCancelar = if (estado.sePuedeCancelar) {
                { estado.cancelarTrabajo() }
            } else {
                null
            },
        )
    }

    if (necesitaContrasena) {
        DialogoContrasena(
            alConfirmar = { contrasena ->
                contrasenaActual = contrasena
                necesitaContrasena = false
                abrirDocumentos(rutasBloqueadas, trasDesbloquear)
            },
            alCancelar = {
                necesitaContrasena = false
                estado.volver()
            },
        )
    }

    pidiendoOrigenImagen?.let { permiteVarias ->
        DialogoOrigenImagen(
            hayCamara = contenedor.selector.hayCamara(),
            alCancelar = { pidiendoOrigenImagen = null },
            alElegirGaleria = {
                pidiendoOrigenImagen = null
                alcance.launch {
                    val elegidas = contenedor.selector.elegirImagenes(permiteVarias)
                    if (elegidas.isEmpty()) return@launch
                    imagenes.addAll(elegidas.map { it.ruta })
                    if (estado.destinoActual !is Destino.Imagenes) {
                        estado.ir(Destino.Imagenes(imagenes.toList()))
                    }
                }
            },
            alElegirCamara = {
                pidiendoOrigenImagen = null
                alcance.launch {
                    val foto = contenedor.selector.hacerFoto()
                    if (foto == null) {
                        estado.avisar(getString(Res.string.img_sin_camara))
                        return@launch
                    }
                    imagenes.add(foto.ruta)
                    if (estado.destinoActual !is Destino.Imagenes) {
                        estado.ir(Destino.Imagenes(imagenes.toList()))
                    }
                }
            },
        )
    }

    abrirRecienCreado?.let { aDonde ->
        AlertDialog(
            onDismissRequest = { abrirRecienCreado = null },
            title = { Text(stringResource(Res.string.res_abrir_titulo)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        abrirRecienCreado = null
                        estado.reemplazar(aDonde)
                    },
                ) {
                    Text(stringResource(Res.string.res_abrir))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        abrirRecienCreado = null
                        estado.volverAInicio()
                    },
                ) {
                    Text(stringResource(Res.string.res_ahora_no))
                }
            },
        )
    }

    compartirRecienCreado?.let { ruta ->
        AlertDialog(
            onDismissRequest = { compartirRecienCreado = null },
            title = { Text(stringResource(Res.string.comp_pregunta_titulo)) },
            text = { Text(contenedor.ficheros.nombre(ruta)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        compartirRecienCreado = null
                        contenedor.servicios.compartirFichero(
                            ruta,
                            "application/pdf",
                            contenedor.ficheros.nombre(ruta),
                        )
                    },
                ) {
                    Text(stringResource(Res.string.comun_compartir))
                }
            },
            dismissButton = {
                TextButton(onClick = { compartirRecienCreado = null }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }

    compartirVariosRecienCreados?.let { rutas ->
        AlertDialog(
            onDismissRequest = { compartirVariosRecienCreados = null },
            title = {
                Text(pluralStringResource(Res.plurals.plural_comp_pregunta, rutas.size, rutas.size))
            },
            text = {
                // La lista se desplaza y no crece sin fin. Separar un documento
                // largo deja ciento veinte ficheros: sin esto el dialogo ocupaba
                // la pantalla entera, ensenaba los treinta primeros y se comia
                // los noventa restantes sin decirlo.
                Column(
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(rutas.joinToString(separator = "\n") { contenedor.ficheros.nombre(it) })
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        compartirVariosRecienCreados = null
                        alcance.launch {
                            contenedor.servicios.compartirVarios(rutas, "NexaPDF.zip")
                        }
                    },
                ) {
                    Text(stringResource(Res.string.comun_compartir))
                }
            },
            dismissButton = {
                TextButton(onClick = { compartirVariosRecienCreados = null }) {
                    Text(stringResource(Res.string.comun_cancelar))
                }
            },
        )
    }

    if (estado.mostrandoDonacion) {
        HojaDonacion(
            alCerrar = { estado.cerrarDonacion() },
            alDonar = {
                haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                contenedor.servicios.abrirEnNavegador(BuildInfo.DONATION_URL)
                estado.silenciarDonacion()
                alcance.launch { estado.avisar(getString(Res.string.donar_gracias)) }
            },
            alAplazar = { estado.aplazarDonacion() },
            alSilenciar = { estado.silenciarDonacion() },
            alCopiarEnlace = {
                contenedor.servicios.copiarAlPortapapeles(BuildInfo.DONATION_URL)
                alcance.launch { estado.avisar(getString(Res.string.donar_enlace_copiado)) }
            },
        )
    }
}

// --- Operaciones largas -----------------------------------------------------

/**
 * Lleva a PDF todo lo que no lo sea.
 *
 * Devuelve las rutas ya en PDF, en el mismo orden en que se eligieron. Si algun
 * fichero no se puede convertir se avisa y se sigue con los demas: perder los
 * cinco documentos buenos porque el sexto estaba corrupto no le sirve a nadie.
 */
private suspend fun convertirTodoAPdf(
    contenedor: ContenedorApp,
    estado: EstadoApp,
    ficheros: List<es.ghatostudio.nexapdf.domain.plataforma.FicheroElegido>,
    textoConvirtiendo: String,
    rutaDeSalida: (String) -> String,
    mensajeDeError: suspend (ErrorPdf) -> String,
): List<String> {
    val necesitanConversion = ficheros.any {
        FormatoDocumento.desdeNombre(it.nombre) != FormatoDocumento.PDF
    }
    if (necesitanConversion) estado.empezarTrabajo(textoConvirtiendo)

    val resultado = mutableListOf<String>()
    ficheros.forEach { fichero ->
        val formato = FormatoDocumento.desdeNombre(fichero.nombre)
        if (formato == FormatoDocumento.PDF || formato == null) {
            resultado += fichero.ruta
            return@forEach
        }

        val nombreSinExtension = fichero.nombre.substringBeforeLast('.')
        val salida = rutaDeSalida("$nombreSinExtension.pdf")
        when (val convertido = contenedor.conversor.aPdf(fichero.ruta, salida)) {
            is ResultadoPdf.Exito -> resultado += convertido.valor
            is ResultadoPdf.Fallo -> estado.avisar(mensajeDeError(convertido.causa))
        }
    }

    if (necesitanConversion) estado.terminarTrabajo()
    return resultado
}

private fun crearPdfDeImagenes(
    contenedor: ContenedorApp,
    estado: EstadoApp,
    alcance: kotlinx.coroutines.CoroutineScope,
    imagenes: List<String>,
    opciones: OpcionesImagenes,
    textoProcesando: String,
    rutaDeSalida: (String) -> String,
    alTerminar: (String) -> Unit,
    mensajeDeError: suspend (ErrorPdf) -> String,
) {
    alcance.launch {
        if (imagenes.isEmpty()) return@launch
        estado.empezarTrabajo(textoProcesando)
        val salida = rutaDeSalida("NexaPDF imagenes.pdf")
        val resultado = contenedor.motorPdf.imagenesAPdf(
            imagenes = imagenes,
            disposicion = opciones.disposicion,
            tamano = opciones.tamano,
            orientacion = opciones.orientacion,
            margenPt = opciones.margenPt,
            espaciadoPt = 12f,
            rutaSalida = salida,
        )
        estado.terminarTrabajo()
        when (resultado) {
            is ResultadoPdf.Exito -> alTerminar(resultado.valor)
            is ResultadoPdf.Fallo -> estado.avisar(mensajeDeError(resultado.causa))
        }
    }
}

private suspend fun guardarEdicion(
    contenedor: ContenedorApp,
    estado: EstadoApp,
    ruta: String,
    edicion: EdicionPagina,
    contrasena: String?,
    textoProcesando: String,
    salida: String,
    alTerminar: (String) -> Unit,
    mensajeDeError: suspend (ErrorPdf) -> String,
) {
    estado.empezarTrabajo(textoProcesando)
    val borrador = BorradorEdicion(
        rutaDocumento = ruta,
        paginas = mapOf(edicion.indice to edicion),
    )
    val resultado = contenedor.motorPdf.aplicarEdiciones(borrador, salida, contrasena)
    estado.terminarTrabajo()
    when (resultado) {
        is ResultadoPdf.Exito -> alTerminar(resultado.valor)
        is ResultadoPdf.Fallo -> estado.avisar(mensajeDeError(resultado.causa))
    }
}

private suspend fun exportarCopia(contenedor: ContenedorApp, estado: EstadoApp) {
    val ajustes = contenedor.ajustes.actual()
    val ahora = contenedor.servicios.ahora()
    val contenido = contenedor.codecCopias.escribir(
        ajustes = ajustes,
        versionApp = BuildInfo.VERSION_NAME,
        ahora = ahora,
        fechaLegible = contenedor.servicios.formatearFecha(ahora, conHora = true),
    )

    val carpeta = contenedor.servicios.directorioCopias
    contenedor.ficheros.asegurarDirectorio(carpeta)
    val nombre = contenedor.ficheros.nombreLibre(
        carpeta,
        "NexaPDF${es.ghatostudio.nexapdf.data.CopiaSeguridad.EXTENSION}",
    )
    val ruta = contenedor.ficheros.unirRuta(carpeta, nombre)

    if (!contenedor.ficheros.escribirTexto(ruta, contenido)) {
        estado.avisar(getString(Res.string.error_escritura))
        return
    }

    val destino = contenedor.selector.guardarComo(
        ruta,
        nombre,
        es.ghatostudio.nexapdf.data.CopiaSeguridad.TIPO_MIME,
    )
    estado.avisar(
        if (destino != null) {
            getString(Res.string.doc_resultado_guardado, destino)
        } else {
            getString(Res.string.copia_exportada)
        },
    )
}

private suspend fun importarCopia(contenedor: ContenedorApp, estado: EstadoApp) {
    val elegido = contenedor.selector.elegirCopiaSeguridad() ?: return
    val contenido = contenedor.ficheros.leerTexto(elegido.ruta)
    if (contenido == null) {
        estado.avisar(getString(Res.string.error_fichero_invalido))
        return
    }

    when (val leida = contenedor.codecCopias.leer(contenido)) {
        is ResultadoCopia.Exito -> {
            // Copia de seguridad automatica previa: si la importacion no era lo
            // que el usuario esperaba, lo anterior sigue estando.
            val ahora = contenedor.servicios.ahora()
            val respaldo = contenedor.codecCopias.escribir(
                ajustes = contenedor.ajustes.actual(),
                versionApp = BuildInfo.VERSION_NAME,
                ahora = ahora,
                fechaLegible = contenedor.servicios.formatearFecha(ahora, conHora = true),
            )
            val carpeta = contenedor.servicios.directorioCopias
            contenedor.ficheros.asegurarDirectorio(carpeta)
            contenedor.ficheros.escribirTexto(
                contenedor.ficheros.unirRuta(
                    carpeta,
                    contenedor.ficheros.nombreLibre(
                        carpeta,
                        "NexaPDF anterior${es.ghatostudio.nexapdf.data.CopiaSeguridad.EXTENSION}",
                    ),
                ),
                respaldo,
            )

            contenedor.ajustes.reemplazar(leida.copia.ajustes)
            contenedor.servicios.aplicarIdioma(leida.copia.ajustes.idioma)
            estado.avisar(getString(Res.string.copia_importada))
        }

        is ResultadoCopia.Fallo -> estado.avisar(
            getString(
                when (leida.error) {
                    ErrorCopia.FORMATO_DESCONOCIDO, ErrorCopia.NO_ES_JSON ->
                        Res.string.copia_error_formato

                    ErrorCopia.ESQUEMA_MAS_NUEVO -> Res.string.copia_error_version
                    ErrorCopia.CONTENIDO_INCOMPLETO -> Res.string.copia_error_incompleta
                },
            ),
        )
    }
}

/** Ancho en pixeles de las miniaturas de la pantalla de imagenes. */
private const val ANCHO_MINIATURA_IMAGEN = 320

/**
 * Ancho al que se lee una imagen insertada para verla en el editor.
 *
 * Es la vista previa mientras se coloca, no lo que acaba en el PDF: al guardar
 * se vuelve a leer el original. Con mas pixeles no se nota nada en pantalla y
 * cada foto de la galeria se come varios megas de memoria.
 */
private const val ANCHO_IMAGEN_EDITOR = 900
