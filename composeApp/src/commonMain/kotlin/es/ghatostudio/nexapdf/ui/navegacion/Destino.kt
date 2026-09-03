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
    data class Documento(val rutas: List<String>) : Destino

    data class Imagenes(val rutas: List<String>) : Destino

    data class Editor(val ruta: String, val paginaInicial: Int) : Destino

    data class Firma(val ruta: String) : Destino

    data object Ajustes : Destino

    data object Ayuda : Destino

    data object AcercaDe : Destino
}
