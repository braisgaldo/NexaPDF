package es.ghatostudio.nexapdf.plataforma

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import es.ghatostudio.nexapdf.domain.escaner.Cuadrilatero
import es.ghatostudio.nexapdf.domain.escaner.DeteccionDocumento
import es.ghatostudio.nexapdf.domain.escaner.DetectorDocumento
import es.ghatostudio.nexapdf.domain.escaner.SuavizadorDeteccion
import es.ghatostudio.nexapdf.domain.model.Punto
import es.ghatostudio.nexapdf.domain.plataforma.CamaraDocumentos
import es.ghatostudio.nexapdf.domain.plataforma.Disparador
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * La camara del escaner, con CameraX.
 *
 * Tres usos conectados a la vez y ninguno mas: vista previa, analisis de
 * fotogramas y captura de foto. Es la combinacion que CameraX garantiza en todos
 * los dispositivos; anadir un cuarto haria que en los telefonos modestos alguno
 * de los tres dejara de funcionar sin avisar.
 *
 * Los tres van en proporcion 4:3, que es la del sensor. Mezclar proporciones es
 * lo que hace que el contorno que se dibuja encima del papel no coincida con el
 * papel: el analisis veria un encuadre y la pantalla otro.
 *
 * El analisis va en su propio hilo y en modo "mantener solo el ultimo": si el
 * detector tarda mas que el intervalo entre fotogramas, se saltan los de en
 * medio en lugar de acumularlos. Un encuadre con dos segundos de retraso es peor
 * que uno que actualiza diez veces por segundo.
 */
class CamaraDocumentosAndroid(
    private val actividad: ComponentActivity,
    private val directorioFotos: String,
) : CamaraDocumentos {

    private var pendientePermiso: CompletableDeferred<Boolean>? = null

    private val pedirCamara: ActivityResultLauncher<String> =
        actividad.registerForActivityResult(ActivityResultContracts.RequestPermission()) { dado ->
            pendientePermiso?.complete(dado)
            pendientePermiso = null
        }

    override val disponible: Boolean
        get() = actividad.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    override fun hayPermiso(): Boolean = ContextCompat.checkSelfPermission(
        actividad,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    override suspend fun pedirPermiso(): Boolean {
        if (hayPermiso()) return true
        // Si ya hay una peticion en marcha se espera a esa: lanzar dos dialogos
        // de permiso a la vez deja el segundo sin respuesta para siempre.
        pendientePermiso?.let { return it.await() }
        val espera = CompletableDeferred<Boolean>()
        pendientePermiso = espera
        pedirCamara.launch(Manifest.permission.CAMERA)
        return espera.await()
    }

    @Composable
    override fun Visor(
        modifier: Modifier,
        linterna: Boolean,
        analizando: Boolean,
        fotosPorDisparo: Int,
        alMedirProporcion: (Float) -> Unit,
        alDetectar: (DeteccionDocumento) -> Unit,
        alEstarLista: (Disparador?) -> Unit,
    ) {
        val contexto = LocalContext.current
        val duenoDelCiclo = LocalLifecycleOwner.current

        // Se toman siempre los valores mas recientes sin volver a montar la
        // camara: reconectar los usos en cada recomposicion produce un parpadeo
        // negro y tarda medio segundo.
        val detectarAhora by rememberUpdatedState(alDetectar)
        val medirAhora by rememberUpdatedState(alMedirProporcion)
        val listaAhora by rememberUpdatedState(alEstarLista)
        val analizandoAhora by rememberUpdatedState(analizando)

        val detector = remember { DetectorDocumento() }
        val suavizador = remember { SuavizadorDeteccion() }
        val ocupado = remember { AtomicBoolean(false) }

        var camara by remember { mutableStateOf<Camera?>(null) }

        val vista = remember {
            PreviewView(contexto).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                // AJUSTAR y no LLENAR: el contorno del papel se dibuja encima en
                // coordenadas de 0 a 1 del fotograma. Con LLENAR, la vista previa
                // recorta por los lados y el contorno queda desplazado respecto
                // al papel que se ve, que es peor que ver dos franjas negras.
                scaleType = PreviewView.ScaleType.FIT_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        AndroidView(factory = { vista }, modifier = modifier)

        DisposableEffect(duenoDelCiclo) {
            val proveedorFuturo = ProcessCameraProvider.getInstance(contexto)
            var proveedor: ProcessCameraProvider? = null
            val hilo = Executors.newSingleThreadExecutor()

            proveedorFuturo.addListener({
                val actual = runCatching { proveedorFuturo.get() }.getOrNull()
                if (actual == null) {
                    listaAhora(null)
                    return@addListener
                }
                proveedor = actual

                val cuatroTercios = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)

                val vistaPrevia = Preview.Builder()
                    .setResolutionSelector(cuatroTercios.build())
                    .build()
                    .also { it.surfaceProvider = vista.surfaceProvider }

                val imagenCaptura = ImageCapture.Builder()
                    // Maxima calidad y maxima resolucion, aunque cueste unas
                    // decimas mas por hoja.
                    //
                    // Al principio estaba al reves, minimizando la latencia,
                    // con el argumento de que el filtro de mejora se comeria el
                    // ruido de todas formas. El argumento era falso por dos
                    // motivos: el reconocimiento de texto lee la pagina **sin**
                    // filtrar, asi que el ruido y el desenfoque le llegan
                    // enteros; y sin pedir resolucion, CameraX entrega un
                    // tamano comodo que en un A4 lleno de letra pequena no da
                    // para leerla. Lo que se ahorraba en decimas se pagaba en
                    // palabras mal leidas.
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY,
                            )
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build(),
                    )
                    .build()

                val analisis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        cuatroTercios
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(ANCHO_ANALISIS, ALTO_ANALISIS),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                                ),
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                analisis.setAnalyzer(hilo) { fotograma ->
                    val proporcion = proporcionEnPantalla(fotograma)
                    val deteccion = analizar(fotograma, detector, ocupado)
                    actividad.runOnUiThread {
                        medirAhora(proporcion)
                        if (analizandoAhora && deteccion != null) {
                            detectarAhora(suavizador.siguiente(deteccion))
                        }
                    }
                }

                runCatching {
                    actual.unbindAll()
                    val ligada = actual.bindToLifecycle(
                        duenoDelCiclo,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        vistaPrevia,
                        imagenCaptura,
                        analisis,
                    )
                    camara = ligada
                    listaAhora(
                        Disparador {
                            // Se enfoca en el centro justo antes de disparar. El
                            // enfoque continuo de la camara persigue toda la
                            // escena, y con un papel a treinta centimetros suele
                            // quedarse en la mesa; medio segundo de enfoque al
                            // centro es la diferencia entre una pagina legible y
                            // una que el reconocedor no sabe leer.
                            enfocarEnElCentro(ligada, vista)
                            hacerRafaga(imagenCaptura, contexto, fotosPorDisparo)
                        },
                    )
                }.onFailure { listaAhora(null) }
            }, ContextCompat.getMainExecutor(contexto))

            onDispose {
                runCatching { proveedor?.unbindAll() }
                hilo.shutdown()
                listaAhora(null)
            }
        }

        // La linterna se enciende sobre la camara ya conectada; no es un cambio
        // de configuracion y por eso no obliga a reconectar nada.
        LaunchedEffect(linterna, camara) {
            runCatching { camara?.cameraControl?.enableTorch(linterna) }
        }

        LaunchedEffect(analizando) {
            if (!analizando) suavizador.reiniciar()
        }
    }

    /**
     * Analiza un fotograma y suelta el siguiente si el anterior no ha acabado.
     *
     * `STRATEGY_KEEP_ONLY_LATEST` ya descarta fotogramas, pero solo mientras el
     * `ImageProxy` este sin cerrar. La bandera evita ademas que dos analisis se
     * pisen los buffers del detector, que no es seguro entre hilos.
     */
    private fun analizar(
        fotograma: ImageProxy,
        detector: DetectorDocumento,
        ocupado: AtomicBoolean,
    ): DeteccionDocumento? {
        if (!ocupado.compareAndSet(false, true)) {
            fotograma.close()
            return null
        }
        try {
            val plano = fotograma.planes.firstOrNull() ?: return null
            val buffer = plano.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val bruta = detector.detectar(
                luminancia = bytes,
                anchoOrigen = fotograma.width,
                altoOrigen = fotograma.height,
                saltoDeFila = plano.rowStride,
            )
            // El sensor entrega la imagen tumbada respecto a como se ve en
            // pantalla. Sin girar el resultado, el contorno sale cruzado sobre la
            // vista previa y parece que la deteccion esta rota cuando lo unico
            // que pasa es que se dibuja en otro sistema de coordenadas.
            return girar(bruta, fotograma.imageInfo.rotationDegrees)
        } finally {
            ocupado.set(false)
            fotograma.close()
        }
    }

    /** Proporcion ancho/alto de lo que se ve, ya con el giro del sensor aplicado. */
    private fun proporcionEnPantalla(fotograma: ImageProxy): Float {
        val giro = ((fotograma.imageInfo.rotationDegrees % 360) + 360) % 360
        val ancho = fotograma.width.toFloat()
        val alto = fotograma.height.toFloat()
        if (alto <= 0f || ancho <= 0f) return 3f / 4f
        return if (giro == 90 || giro == 270) alto / ancho else ancho / alto
    }

    /** Lleva la deteccion del sistema de coordenadas del sensor al de la pantalla. */
    private fun girar(deteccion: DeteccionDocumento, grados: Int): DeteccionDocumento {
        val cuadro = deteccion.cuadro ?: return deteccion
        val giro = ((grados % 360) + 360) % 360
        if (giro == 0) return deteccion

        val esquinas = cuadro.esquinas.map { punto ->
            when (giro) {
                90 -> Punto(1f - punto.y, punto.x)
                180 -> Punto(1f - punto.x, 1f - punto.y)
                270 -> Punto(punto.y, 1f - punto.x)
                else -> punto
            }
        }
        val girado = Cuadrilatero.ordenar(esquinas) ?: return deteccion
        return deteccion.copy(cuadro = girado)
    }

    /**
     * Pide enfoque y medicion de luz en el centro del encuadre, y espera.
     *
     * Se espera a proposito, con un tope: sin esperar, la foto sale con el
     * enfoque anterior y el paso de enfocar no sirve de nada. El tope esta para
     * que una camara que no consiga enfocar —un papel demasiado cerca, una
     * pared lisa— no deje al usuario esperando: pasado ese tiempo se dispara
     * igual, que es mejor que no disparar.
     */
    private suspend fun enfocarEnElCentro(camara: Camera, vista: PreviewView) {
        val ancho = vista.width.toFloat()
        val alto = vista.height.toFloat()
        if (ancho <= 0f || alto <= 0f) return

        val fabrica = vista.meteringPointFactory
        val centro = fabrica.createPoint(ancho / 2f, alto / 2f)
        val accion = FocusMeteringAction.Builder(centro, FocusMeteringAction.FLAG_AF)
            .addPoint(centro, FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        runCatching {
            withTimeoutOrNull(ESPERA_ENFOQUE) {
                val futuro = camara.cameraControl.startFocusAndMetering(accion)
                suspendCancellableCoroutine { continuacion ->
                    futuro.addListener(
                        { continuacion.resume(Unit) },
                        ContextCompat.getMainExecutor(vista.context),
                    )
                    continuacion.invokeOnCancellation { futuro.cancel(true) }
                }
            }
        }
    }

    /**
     * Varias fotos seguidas de la misma hoja.
     *
     * Se enfoca una sola vez, antes de la primera: reenfocar entre disparo y
     * disparo tardaria mas que los propios disparos y ademas movería el plano,
     * que es justo lo que no interesa.
     *
     * Si alguna falla se sigue con las que haya. Una rafaga de dos ya sirve, y
     * quedarse sin nada porque el tercer disparo fallo seria absurdo.
     */
    private suspend fun hacerRafaga(
        captura: ImageCapture,
        contexto: Context,
        cuantas: Int,
    ): List<String> {
        val rutas = ArrayList<String>(cuantas)
        repeat(cuantas.coerceAtLeast(1)) {
            val ruta = hacerFoto(captura, contexto) ?: return@repeat
            rutas += ruta
        }
        return rutas
    }

    private suspend fun hacerFoto(captura: ImageCapture, contexto: Context): String? =
        withContext(Dispatchers.IO) {
            val carpeta = File(directorioFotos).apply { mkdirs() }
            val destino = File(carpeta, "escaneo-${System.currentTimeMillis()}.jpg")
            val opciones = ImageCapture.OutputFileOptions.Builder(destino).build()

            val espera = CompletableDeferred<Boolean>()
            withContext(Dispatchers.Main) {
                captura.takePicture(
                    opciones,
                    ContextCompat.getMainExecutor(contexto),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                            espera.complete(true)
                        }

                        override fun onError(excepcion: ImageCaptureException) {
                            espera.complete(false)
                        }
                    },
                )
            }
            if (espera.await()) destino.absolutePath else null
        }

    private companion object {
        /**
         * Resolucion del analisis.
         *
         * Suficiente para encontrar los bordes de un folio y lo bastante pequena
         * para que copiar el plano de luminancia a un array no cueste mas que
         * analizarlo. El detector reduce otra vez hasta 180 px de lado.
         */
        const val ANCHO_ANALISIS = 640
        const val ALTO_ANALISIS = 480

        /** Tope de espera al enfocar antes de disparar. */
        const val ESPERA_ENFOQUE = 1200L
    }
}
