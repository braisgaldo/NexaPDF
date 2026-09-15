package es.ghatostudio.nexapdf.domain.escaner

import kotlin.math.roundToInt

/**
 * La curva de tono que convierte una pagina aplanada en una pagina de documento.
 *
 * Es el ultimo paso de la mejora y el mas facil de hacer mal. Lo que hace es
 * decidir dos cortes: por encima de uno todo es papel y se va a blanco puro, por
 * debajo del otro todo es tinta y se va a negro, y lo de en medio se estira para
 * que ocupe toda la escala.
 *
 * **Lo que puede salir mal.** Si el corte del papel es bajo, el filtro no borra
 * suciedad: borra contenido. Un lapiz flojo o una firma a boligrafo claro son
 * grises suaves, y con el corte demasiado bajo se van a blanco y desaparecen de
 * la pagina sin que nada avise. Paso de verdad: sobre una hoja con anotaciones a
 * lapiz, la mitad se perdieron.
 *
 * Por eso el corte del papel va **alto**. Puede permitirselo porque para cuando
 * llega aqui la pagina ya esta aplanada y el papel esta cerca del blanco en toda
 * su superficie; antes de aplanar, un corte alto habria dejado media pagina
 * gris.
 */
object TonoDeDocumento {

    /**
     * @param fuerza de 0 a 1. Mas fuerza es mas contraste: el papel se limpia
     *   antes y la tinta se oscurece antes. Menos fuerza respeta mas los grises,
     *   que es lo que conviene con anotaciones a lapiz o con sellos claros.
     */
    fun aplicar(gris: IntArray, fuerza: Float): IntArray =
        gris.copyOf().also { aplicarEnSitio(it, fuerza) }

    /** Lo mismo sobre la propia pagina: es punto a punto, no necesita copia. */
    fun aplicarEnSitio(gris: IntArray, fuerza: Float) {
        val nivel = fuerza.coerceIn(0f, 1f)
        val corteFondo = (PAPEL_SUAVE - (PAPEL_SUAVE - PAPEL_FIRME) * nivel).roundToInt()
        val corteTinta = (TINTA_SUAVE + (TINTA_FIRME - TINTA_SUAVE) * nivel).roundToInt()
        val rango = (corteFondo - corteTinta).coerceAtLeast(1)

        for (indice in gris.indices) {
            val valor = gris[indice]
            gris[indice] = when {
                valor >= corteFondo -> 255
                valor <= corteTinta -> 0
                else -> ((valor - corteTinta) * 255f / rango).roundToInt().coerceIn(0, 255)
            }
        }
    }

    /**
     * Corte del papel con la fuerza al minimo.
     *
     * Casi el blanco puro: solo se limpia lo que ya era papel. Es el ajuste para
     * una hoja con anotaciones flojas, donde perder un gris es perder contenido.
     */
    private const val PAPEL_SUAVE = 248f

    /**
     * Corte del papel con la fuerza al maximo.
     *
     * Sigue estando bastante alto. Bajarlo mas no limpia mejor una pagina ya
     * aplanada: lo unico que hace es empezar a comerse los grises claros.
     */
    private const val PAPEL_FIRME = 222f

    /** Corte de la tinta con la fuerza al minimo: solo lo muy negro se aplana a negro. */
    private const val TINTA_SUAVE = 30f

    /** Corte de la tinta con la fuerza al maximo. */
    private const val TINTA_FIRME = 75f
}
