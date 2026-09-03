package es.ghatostudio.nexapdf.ui.navegacion

/**
 * Pantallas de la aplicacion.
 *
 * No se usa una biblioteca de navegacion: NexaPDF tiene ocho destinos, ninguno
 * con enlaces profundos ni argumentos que haya que serializar a una URL, y una
 * pila de destinos en memoria hace exactamente lo mismo sin anadir un plugin de
 * compilacion ni rutas escritas como texto que fallan en tiempo de ejecucion.
 */
sealed interface Destino {

    data object Inicio : Destino

    /**
     * Espacio de trabajo con uno o varios documentos.
     *
     * Con un documento muestra sus paginas y permite girarlas, extraerlas,
     * separarlas o abrir el editor. Con varios, ordenarlos y unirlos. Es la
     * misma pantalla porque es la misma tarea vista de cerca o de lejos.
     */
    /**
     * @param modoUnion la pantalla se abrio para unir documentos. Se lleva
     *   explicito y no se deduce de `rutas.size` porque una union puede
     *   empezar con un solo fichero e ir creciendo, y sin este dato la
     *   pantalla ensenaria la rejilla de paginas en lugar de la lista.
     */
    data class Documento(
        val rutas: List<String>,
        val modoUnion: Boolean = false,
        /**
         * Se llego aqui despues de unir. Sirve para presentar la rejilla de
         * paginas como el segundo paso de la union en lugar de como una
         * pantalla suelta.
         */
        val desdeUnion: Boolean = false,
    ) : Destino

    data class Imagenes(val rutas: List<String>) : Destino

    data class Editor(val ruta: String, val paginaInicial: Int) : Destino

    data class Firma(val ruta: String) : Destino

    data object Ajustes : Destino

    /** Lectura de un documento: buscar, indice y firmas. */
    data class Visor(val ruta: String, val pagina: Int = 0) : Destino

    /** Lista de documentos ya creados. */
    data object Recientes : Destino

    /** Eleccion de que documentos compartir. */
    data object Compartir : Destino

    /** Tour guiado de bienvenida. */
    data object Tour : Destino

    data object Ayuda : Destino

    data object AcercaDe : Destino
}
