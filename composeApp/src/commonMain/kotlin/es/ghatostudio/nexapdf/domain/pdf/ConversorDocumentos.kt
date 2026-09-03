package es.ghatostudio.nexapdf.domain.pdf

/**
 * Formatos con los que sabe tratar NexaPDF.
 *
 * Todo lo que entra se convierte a PDF, que es el formato con el que trabaja la
 * aplicacion; al exportar se vuelve a salir a cualquiera de los cuatro.
 */
enum class FormatoDocumento(
    val extensiones: List<String>,
    val tiposMime: List<String>,
) {
    PDF(
        extensiones = listOf("pdf"),
        tiposMime = listOf("application/pdf"),
    ),
    DOCX(
        extensiones = listOf("docx"),
        tiposMime = listOf(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        ),
    ),
    XLSX(
        extensiones = listOf("xlsx"),
        tiposMime = listOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ),
    ),
    PPTX(
        extensiones = listOf("pptx"),
        tiposMime = listOf(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        ),
    ),
    IMAGEN(
        extensiones = listOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif"),
        tiposMime = listOf("image/*"),
    ),
    ;

    val esOfimatico: Boolean get() = this == DOCX || this == XLSX || this == PPTX

    companion object {
        /** Deduce el formato por la extension del nombre de fichero. */
        fun desdeNombre(nombre: String): FormatoDocumento? {
            val extension = nombre.substringAfterLast('.', "").lowercase()
            if (extension.isEmpty()) return null
            return entries.firstOrNull { extension in it.extensiones }
        }

        /** Tipos MIME que se ofrecen al elegir ficheros para unir. */
        val TIPOS_PARA_UNIR: List<String> =
            (PDF.tiposMime + DOCX.tiposMime + XLSX.tiposMime + PPTX.tiposMime + IMAGEN.tiposMime)

        /** Formatos a los que se puede exportar un documento ya en PDF. */
        val EXPORTABLES: List<FormatoDocumento> = listOf(PDF, DOCX, XLSX, PPTX)
    }
}

/**
 * Conversion entre PDF y los formatos de ofimatica.
 *
 * Lo que se puede y lo que no, dicho sin adornos:
 *
 *  - **A PDF** se traduce el *contenido*, no el diseno. Un .docx se vuelve a
 *    componer con sus parrafos, negritas y saltos de pagina; un .pptx pasa a
 *    una pagina por diapositiva con sus textos e imagenes; un .xlsx a una tabla.
 *    No es Word imprimiendo: para eso haria falta un motor de maquetacion
 *    completo, que no cabe en una app de movil.
 *  - **Desde PDF** se recupera el texto y las imagenes, que es lo que un PDF
 *    guarda. Un PDF no contiene parrafos ni celdas, sino letras colocadas en
 *    coordenadas, asi que la estructura se deduce de esas posiciones.
 *
 * La interfaz de usuario advierte de esto antes de convertir. Prometer
 * fidelidad total seria mentir.
 */
interface ConversorDocumentos {

    /** Convierte cualquier formato admitido a PDF. */
    suspend fun aPdf(ruta: String, rutaSalida: String): ResultadoPdf<String>

    /** Exporta un PDF a otro formato. */
    suspend fun desdePdf(
        ruta: String,
        formato: FormatoDocumento,
        rutaSalida: String,
    ): ResultadoPdf<String>
}
