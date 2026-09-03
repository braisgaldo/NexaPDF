package es.ghatostudio.nexapdf.pdf

import es.ghatostudio.nexapdf.domain.model.DisposicionImagenes
import es.ghatostudio.nexapdf.domain.model.Orientacion
import es.ghatostudio.nexapdf.domain.model.TamanoPagina
import es.ghatostudio.nexapdf.domain.pdf.ConversorDocumentos
import es.ghatostudio.nexapdf.domain.pdf.ErrorPdf
import es.ghatostudio.nexapdf.domain.pdf.FormatoDocumento
import es.ghatostudio.nexapdf.domain.pdf.MotorPdf
import es.ghatostudio.nexapdf.domain.pdf.ResultadoPdf
import es.ghatostudio.nexapdf.pdf.ofimatica.OfimaticaAPdf
import es.ghatostudio.nexapdf.pdf.ofimatica.PdfAOfimatica
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Conversion entre PDF y ofimatica en Android.
 *
 * Se apoya en [OfimaticaAPdf] y [PdfAOfimatica], que trabajan directamente sobre
 * el ZIP y el XML de los formatos OOXML. No hay ninguna biblioteca de ofimatica
 * detras: las que existen para Java pesan mas que toda la aplicacion y ninguna
 * funciona bien en Android.
 */
class ConversorDocumentosAndroid(
    private val motor: MotorPdf,
) : ConversorDocumentos {

    private val aPdf = OfimaticaAPdf()
    private val desdePdf = PdfAOfimatica()

    override suspend fun aPdf(ruta: String, rutaSalida: String): ResultadoPdf<String> =
        withContext(Dispatchers.IO) {
            val origen = File(ruta)
            if (!origen.exists()) {
                return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, ruta)
            }

            when (FormatoDocumento.desdeNombre(origen.name)) {
                FormatoDocumento.PDF, null -> ResultadoPdf.Exito(ruta)

                FormatoDocumento.IMAGEN -> motor.imagenesAPdf(
                    imagenes = listOf(ruta),
                    disposicion = DisposicionImagenes.UNA_POR_PAGINA,
                    tamano = TamanoPagina.AJUSTAR_A_IMAGEN,
                    orientacion = Orientacion.AUTOMATICA,
                    margenPt = 0f,
                    espaciadoPt = 0f,
                    rutaSalida = rutaSalida,
                )

                FormatoDocumento.DOCX -> convertir(rutaSalida) {
                    aPdf.docxAPdf(origen, File(rutaSalida))
                }

                FormatoDocumento.XLSX -> convertir(rutaSalida) {
                    aPdf.xlsxAPdf(origen, File(rutaSalida))
                }

                FormatoDocumento.PPTX -> convertir(rutaSalida) {
                    aPdf.pptxAPdf(origen, File(rutaSalida))
                }
            }
        }

    override suspend fun desdePdf(
        ruta: String,
        formato: FormatoDocumento,
        rutaSalida: String,
    ): ResultadoPdf<String> = withContext(Dispatchers.IO) {
        val origen = File(ruta)
        if (!origen.exists()) {
            return@withContext ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, ruta)
        }
        val titulo = origen.nameWithoutExtension

        when (formato) {
            FormatoDocumento.PDF -> ResultadoPdf.Exito(ruta)
            FormatoDocumento.DOCX -> convertir(rutaSalida) {
                desdePdf.aDocx(origen, File(rutaSalida), titulo)
            }

            FormatoDocumento.XLSX -> convertir(rutaSalida) {
                desdePdf.aXlsx(origen, File(rutaSalida), titulo)
            }

            FormatoDocumento.PPTX -> convertir(rutaSalida) {
                desdePdf.aPptx(origen, File(rutaSalida), titulo)
            }

            FormatoDocumento.IMAGEN -> ResultadoPdf.Fallo(
                ErrorPdf.OPERACION_NO_PERMITIDA,
                "Para exportar como imagen se separa el PDF en paginas",
            )
        }
    }

    private inline fun convertir(
        rutaSalida: String,
        bloque: () -> Unit,
    ): ResultadoPdf<String> = try {
        File(rutaSalida).parentFile?.mkdirs()
        bloque()
        if (File(rutaSalida).length() == 0L) {
            ResultadoPdf.Fallo(ErrorPdf.ERROR_ESCRITURA, "el resultado quedo vacio")
        } else {
            ResultadoPdf.Exito(rutaSalida)
        }
    } catch (e: OutOfMemoryError) {
        ResultadoPdf.Fallo(ErrorPdf.SIN_MEMORIA, e.message)
    } catch (e: Exception) {
        ResultadoPdf.Fallo(ErrorPdf.FICHERO_INVALIDO, e.message)
    }
}
