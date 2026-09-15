package es.ghatostudio.nexapdf.domain.escaner

import androidx.compose.ui.graphics.ImageBitmap
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf

/**
 * Lo que hace falta para convertir una foto de un papel en una pagina de
 * documento.
 *
 * Va aparte de [es.ghatostudio.nexapdf.domain.pdf.MotorPdf] porque no toca
 * ningun PDF: trabaja con pixeles y con texto. El motor de PDF recibe despues el
 * resultado ya cocinado, y esa separacion es la que permite probar el recorte y
 * el OCR sin escribir un solo fichero PDF.
 */
interface MotorEscaner {

    /**
     * Si este dispositivo puede reconocer texto.
     *
     * Se pregunta en lugar de darlo por hecho porque el reconocimiento es una
     * pieza que puede no estar (en escritorio todavia no lo esta), y la pantalla
     * tiene que poder esconder la casilla en vez de ofrecer algo que fallara.
     */
    val ocrDisponible: Boolean

    /** Busca los bordes del papel en una foto ya guardada, para las de galeria. */
    suspend fun detectarEn(rutaImagen: String): DeteccionDocumento

    /**
     * Endereza, recorta, mejora y guarda la hoja como una imagen nueva.
     *
     * Devuelve la ruta del fichero escrito. El original no se toca: el usuario
     * puede volver atras y mover una esquina cuantas veces quiera sin que la
     * imagen se vaya degradando a cada intento.
     */
    suspend fun revelar(hoja: HojaEscaneada, rutaSalida: String): ResultadoPdf<String>

    /**
     * Vista previa de como quedara la hoja, al ancho pedido.
     *
     * Es la misma operacion que [revelar] pero en pequeno y sin escribir en
     * disco, que es lo que permite ensenar el resultado del filtro mientras se
     * mueve el deslizador.
     */
    suspend fun previsualizar(hoja: HojaEscaneada, anchoPx: Int): ResultadoPdf<ImageBitmap>

    /**
     * Reconoce el texto de una imagen ya revelada.
     *
     * Las coordenadas que devuelve son las de esa imagen, normalizadas: quien
     * las incrusta en el PDF solo tiene que multiplicar por el tamano de la
     * pagina. Si el dispositivo no sabe hacer OCR devuelve texto vacio en lugar
     * de fallar, porque un PDF sin capa de texto sigue siendo un PDF correcto.
     */
    suspend fun reconocerTexto(rutaImagen: String): ResultadoPdf<TextoPagina>
}
