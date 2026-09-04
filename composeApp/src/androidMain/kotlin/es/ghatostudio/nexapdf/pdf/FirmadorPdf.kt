package es.ghatostudio.nexapdf.pdf

import android.content.Context
import android.security.KeyChain
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSTypedData
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import org.bouncycastle.asn1.ASN1Encoding
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Calendar
import java.security.MessageDigest
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSet
import org.bouncycastle.asn1.cms.Attribute
import org.bouncycastle.asn1.cms.AttributeTable
import org.bouncycastle.asn1.cms.CMSAttributes
import org.bouncycastle.asn1.ess.ESSCertIDv2
import org.bouncycastle.asn1.ess.SigningCertificateV2
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.IssuerSerial
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions

/**
 * Firma electronica con certificado del usuario.
 *
 * Produce una firma **PAdES-B-B** (`ETSI.CAdES.detached`) incrustada mediante
 * guardado incremental, que es el mismo mecanismo que usan Acrobat y AutoFirma:
 * el contenido anterior del documento no se toca, se anade la firma al final, y
 * cualquier lector puede comprobar que nada cambio despues de firmar.
 *
 * No lleva sello de tiempo de una autoridad, asi que no es PAdES-B-T. No es un
 * descuido: pedirlo exige salir a internet y NexaPDF no declara el permiso. La
 * hora que consta es la del dispositivo, en la entrada `/M` del diccionario de
 * la firma.
 *
 * El certificado nunca sale del telefono ni se guarda: se lee del fichero que
 * elige el usuario, se usa y se descarta al terminar la operacion.
 */
class FirmadorPdf {

    /** Datos del certificado ya abiertos y validados. */
    class Credenciales(
        val clavePrivada: PrivateKey,
        val cadena: Array<Certificate>,
        /**
         * Si la clave vive en el almacen del sistema.
         *
         * No es un detalle informativo: una clave del almacen es un objeto
         * opaco que solo sabe manejar el proveedor de Android, asi que decide
         * quien firma. Ver [GeneradorCms].
         */
        val delAlmacenDelSistema: Boolean = false,
    ) {
        val titular: String
            get() = (cadena.firstOrNull() as? X509Certificate)
                ?.subjectX500Principal?.name
                ?.let { extraerNombreComun(it) }
                ?: ""

        private companion object {
            fun extraerNombreComun(distinguido: String): String =
                distinguido.split(',')
                    .map { it.trim() }
                    .firstOrNull { it.startsWith("CN=", ignoreCase = true) }
                    ?.removeRange(0, 3)
                    ?: distinguido
        }
    }

    /**
     * Abre un PKCS#12. Devuelve `null` si la contrasena no es correcta o el
     * fichero no contiene ninguna clave privada utilizable.
     */
    fun abrirCertificado(contenido: ByteArray, contrasena: String): Credenciales? = runCatching {
        val almacen = KeyStore.getInstance("PKCS12", proveedor)
        contenido.inputStream().use { almacen.load(it, contrasena.toCharArray()) }

        val alias = almacen.aliases().toList().firstOrNull { almacen.isKeyEntry(it) }
            ?: return@runCatching null
        val clave = almacen.getKey(alias, contrasena.toCharArray()) as? PrivateKey
            ?: return@runCatching null
        val cadena = almacen.getCertificateChain(alias)
            ?: return@runCatching null
        if (cadena.isEmpty()) return@runCatching null

        Credenciales(clave, cadena)
    }.getOrNull()

    /**
     * Toma un certificado del almacen de claves del dispositivo.
     *
     * [KeyChain.getPrivateKey] no devuelve la clave: devuelve un apoderado que
     * sabe firmar delegando en el sistema, que es lo que permite usar
     * certificados respaldados por hardware sin que la aplicacion llegue a ver
     * nunca el material criptografico. Ambas llamadas bloquean, asi que esto
     * tiene que correr fuera del hilo principal.
     */
    fun credencialesDelSistema(contexto: Context, alias: String): Credenciales? = runCatching {
        val clave = KeyChain.getPrivateKey(contexto, alias) ?: return@runCatching null
        val cadena = KeyChain.getCertificateChain(contexto, alias) ?: return@runCatching null
        if (cadena.isEmpty()) return@runCatching null

        Credenciales(clave, cadena.toList().toTypedArray(), delAlmacenDelSistema = true)
    }.getOrNull()

    /**
     * Firma [documento] y escribe el resultado en [salida].
     *
     * El documento debe haberse cargado desde el mismo fichero que se pasa como
     * [origen]: el guardado incremental copia los bytes originales tal cual y
     * anade la revision firmada detras.
     */
    fun firmar(
        documento: PDDocument,
        origen: File,
        salida: OutputStream,
        credenciales: Credenciales,
        nombre: String,
        motivo: String?,
        lugar: String?,
    ) {
        val firma = PDSignature().apply {
            setFilter(PDSignature.FILTER_ADOBE_PPKLITE)
            // ETSI.CAdES.detached y no adbe.pkcs7.detached: es lo que
            // distingue una firma PAdES de un PKCS#7 metido en un PDF. Con el
            // subfiltro de Adobe, un validador conforme a la norma europea
            // (VALIDe, DSS) no la clasifica como PAdES aunque la criptografia
            // este bien.
            setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED)
            setName(nombre)
            motivo?.takeIf { it.isNotBlank() }?.let { setReason(it) }
            lugar?.takeIf { it.isNotBlank() }?.let { setLocation(it) }
            signDate = Calendar.getInstance()
        }

        // El hueco que PDFBox reserva para el sobre CMS es fijo y por defecto
        // son 9 KB. Cabe de sobra con un certificado suelto, pero una cadena
        // real de la FNMT o del DNIe son tres certificados y se queda corto: la
        // firma falla al final, cuando ya se ha hecho todo el trabajo. Se pide
        // sitio a partir de lo que ocupa la cadena, con margen para la firma y
        // los atributos.
        val opciones = SignatureOptions().apply {
            val cadena = credenciales.cadena.sumOf { it.encoded.size }
            setPreferredSignatureSize(
                maxOf(SignatureOptions.DEFAULT_SIGNATURE_SIZE, cadena + HOLGURA_FIRMA),
            )
        }
        opciones.use {
            documento.addSignature(firma, GeneradorCms(credenciales), it)
            documento.saveIncremental(salida)
        }
    }

    /** Construye el sobre CMS que se guarda dentro del PDF. */
    private inner class GeneradorCms(
        private val credenciales: Credenciales,
    ) : SignatureInterface {

        override fun sign(contenido: InputStream): ByteArray {
            val certificado = credenciales.cadena.first() as X509Certificate
            val algoritmo = when (credenciales.clavePrivada.algorithm.uppercase()) {
                "EC", "ECDSA" -> "SHA256withECDSA"
                else -> "SHA256withRSA"
            }

            // Con un certificado del almacen del sistema NO se puede fijar el
            // proveedor: la clave es un apoderado de AndroidKeyStore y
            // BouncyCastle no sabe operar con ella. Dejando elegir a la JCA, la
            // firma la hace el proveedor duenno de la clave, que es justo lo que
            // mantiene el secreto dentro del almacen.
            val constructor = JcaContentSignerBuilder(algoritmo)
            if (!credenciales.delAlmacenDelSistema) constructor.setProvider(proveedor)
            val firmante = constructor.build(credenciales.clavePrivada)

            val generador = CMSSignedDataGenerator().apply {
                addSignerInfoGenerator(
                    JcaSignerInfoGeneratorBuilder(
                        JcaDigestCalculatorProviderBuilder().setProvider(proveedor).build(),
                    )
                        .setSignedAttributeGenerator(AtributosPades(certificado))
                        .build(firmante, certificado),
                )
                addCertificates(JcaCertStore(credenciales.cadena.toList()))
            }

            // El contenido firmado va aparte (detached), asi que se genera con
            // encapsulate = false.
            //
            // Se pide DER explicitamente. `encoded` a secas devuelve BER con
            // longitudes indefinidas, porque el contenido se ha firmado en
            // flujo; Acrobat lo traga, pero la norma exige DER para la firma de
            // un PDF y los validadores estrictos (openssl entre ellos) rechazan
            // el sobre entero antes de mirar la firma.
            return generador.generate(ContenidoFlujo(contenido), false)
                .toASN1Structure()
                .getEncoded(ASN1Encoding.DER)
        }
    }

    /**
     * Los atributos firmados que pide PAdES-B-B.
     *
     * Sobre los que pone BouncyCastle por su cuenta (contentType, messageDigest
     * y la proteccion de algoritmos) hace dos cosas:
     *
     * - **Anade `signingCertificateV2`**, que lleva el hash del certificado del
     *   firmante dentro de lo que se firma. Es lo que impide que alguien cambie
     *   el certificado del sobre por otro y siga cuadrando, y es obligatorio en
     *   CAdES-BES; sin el, la firma no es PAdES por mucho que la criptografia
     *   sea correcta.
     * - **Quita `signingTime`.** En PAdES la hora va en la entrada `/M` del
     *   diccionario de la firma, y la norma dice que este atributo no debe
     *   estar. Tenerlo duplicado y sin sello de tiempo solo da un aviso en los
     *   validadores.
     */
    private class AtributosPades(
        certificado: X509Certificate,
    ) : DefaultSignedAttributeTableGenerator(referenciaA(certificado)) {

        override fun getAttributes(parametros: MutableMap<Any?, Any?>): AttributeTable =
            super.getAttributes(parametros).remove(CMSAttributes.signingTime)

        private companion object {
            fun referenciaA(certificado: X509Certificate): AttributeTable {
                val huella = MessageDigest.getInstance("SHA-256").digest(certificado.encoded)
                val emisorYSerie = IssuerSerial(
                    GeneralNames(
                        GeneralName(X500Name.getInstance(certificado.issuerX500Principal.encoded)),
                    ),
                    ASN1Integer(certificado.serialNumber),
                )
                val identificador = ESSCertIDv2(
                    AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256),
                    huella,
                    emisorYSerie,
                )
                return AttributeTable(
                    ASN1EncodableVector().apply {
                        add(
                            Attribute(
                                PKCSObjectIdentifiers.id_aa_signingCertificateV2,
                                DERSet(SigningCertificateV2(arrayOf(identificador))),
                            ),
                        )
                    },
                )
            }
        }
    }

    /**
     * Adaptador entre el flujo que entrega PDFBox y lo que espera BouncyCastle.
     *
     * BouncyCastle trae `CMSProcessableByteArray`, pero obligaria a cargar el
     * PDF entero en memoria para firmarlo. Con este envoltorio se firma leyendo
     * en flujo, que es lo que permite firmar un documento de cien megas en un
     * movil sin agotar la memoria.
     */
    private class ContenidoFlujo(private val entrada: InputStream) : CMSTypedData {
        override fun getContentType(): ASN1ObjectIdentifier = CMSObjectIdentifiers.data
        override fun getContent(): Any = entrada
        override fun write(salida: OutputStream) {
            entrada.copyTo(salida, DEFAULT_BUFFER_SIZE)
        }
    }

    private companion object {
        /** Sitio de sobra, ademas de la cadena, para la firma y sus atributos. */
        const val HOLGURA_FIRMA = 12 * 1024

        /**
         * Se usa la instancia del proveedor, no su nombre.
         *
         * Android incorpora una version recortada y antigua de BouncyCastle bajo
         * el nombre "BC"; pedirlo por nombre devolveria esa y no la que trae la
         * app, y fallaria al firmar con algoritmos modernos.
         */
        val proveedor = BouncyCastleProvider()
    }
}
