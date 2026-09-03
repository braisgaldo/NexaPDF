package es.ghatostudio.nexapdf.domain.pdf

/**
 * De donde sale la clave con la que se firma.
 *
 * Son dos mundos distintos y conviene no mezclarlos. Un fichero PKCS#12 lo
 * abre la propia aplicacion: hay que pedirle la contrasena al usuario y la
 * clave privada pasa, por un momento, por la memoria del proceso. En el almacen
 * del sistema la clave **nunca sale de ahi**: Android da un objeto opaco que
 * sabe firmar pero del que no se puede extraer el material criptografico, y es
 * el propio sistema quien pide la autenticacion del usuario si el certificado
 * la exige.
 *
 * El segundo camino es el que usa casi todo el mundo en Espana, donde el
 * certificado de la FNMT suele instalarse una vez en los ajustes del telefono y
 * ya no se vuelve a tocar el `.p12`.
 */
sealed interface OrigenCertificado {

    /**
     * Un fichero `.p12` o `.pfx` elegido por el usuario.
     *
     * No es `data class` a proposito: el `equals` generado compararia el
     * ByteArray por identidad, lo que da falsos negativos, y el `toString`
     * volcaria material sensible a los registros.
     */
    class Fichero(
        val contenido: ByteArray,
        val contrasena: String,
    ) : OrigenCertificado

    /** Un certificado ya instalado en el almacen de claves del dispositivo. */
    data class AlmacenDelSistema(val alias: String) : OrigenCertificado
}
