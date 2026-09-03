package es.ghatostudio.nexapdf.ui.i18n

/**
 * Los trece idiomas de NexaPDF.
 *
 * El nombre va escrito en el propio idioma, que es como lo busca quien lo habla:
 * alguien que solo lee ruso no encuentra "Ruso" en una lista en castellano.
 */
enum class Idioma(
    /** Etiqueta BCP-47 tal como la esperan LocaleManager y locales_config.xml. */
    val etiqueta: String,
    /** Nombre en su propio idioma. */
    val nombreNativo: String,
    val derechaAIzquierda: Boolean = false,
) {
    INGLES("en", "English"),
    ESPANOL("es", "Español"),
    FRANCES("fr", "Français"),
    ALEMAN("de", "Deutsch"),
    CHINO("zh-CN", "简体中文"),
    JAPONES("ja", "日本語"),
    RUSO("ru", "Русский"),
    ITALIANO("it", "Italiano"),
    GRIEGO("el", "Ελληνικά"),
    ARABE("ar", "العربية", derechaAIzquierda = true),
    GALLEGO("gl", "Galego"),
    CATALAN("ca", "Català"),
    EUSKERA("eu", "Euskara"),
    ;

    /**
     * El ingles y el arabe no son de un unico pais.
     *
     * Los dos se hablan como lengua propia en decenas de estados, asi que no
     * hay una bandera "suya". Se usa la ensena mas reconocible para cada lengua:
     * el Reino Unido para el ingles, de donde procede, y la Liga Arabe para el
     * arabe, que agrupa a los paises que lo tienen como oficial. Ver el dibujo
     * en Banderas.kt.
     */
    val banderaSupranacional: Boolean get() = this == INGLES || this == ARABE

    companion object {
        val predeterminado: Idioma = INGLES

        fun desdeEtiqueta(etiqueta: String?): Idioma? {
            if (etiqueta == null) return null
            entries.firstOrNull { it.etiqueta.equals(etiqueta, ignoreCase = true) }?.let { return it }
            // "es-ES", "zh-Hans-CN"... se reducen a su idioma base.
            val base = etiqueta.substringBefore('-')
            return entries.firstOrNull { it.etiqueta.substringBefore('-').equals(base, ignoreCase = true) }
        }

        /** Idioma efectivo: el elegido, el del sistema si encaja, o ingles. */
        fun resolver(preferido: String?, delSistema: String): Idioma =
            desdeEtiqueta(preferido) ?: desdeEtiqueta(delSistema) ?: predeterminado
    }
}
