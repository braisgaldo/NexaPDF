# -----------------------------------------------------------------------------
# Reglas de R8 para NexaPDF
# -----------------------------------------------------------------------------

# --- PDFBox-Android -----------------------------------------------------------
# PDFBox resuelve filtros, fuentes y manejadores de seguridad por reflexion a
# partir de nombres que aparecen en el propio PDF, asi que R8 no puede ver esas
# referencias. Se conserva el arbol completo de la biblioteca.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.tom_roush.fontbox.**

# PDFBox referencia clases de Java SE que no existen en Android; nunca se
# alcanzan en tiempo de ejecucion porque hay implementaciones alternativas.
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.naming.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn sun.misc.**

# --- BouncyCastle (firma electronica con certificado) -------------------------
# Los proveedores JCE se instancian por nombre.
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.cms.** { *; }
-keep class org.bouncycastle.operator.** { *; }
-dontwarn org.bouncycastle.**

# --- kotlinx.serialization ----------------------------------------------------
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class es.ghatostudio.nexapdf.**$$serializer { *; }
-keepclassmembers class es.ghatostudio.nexapdf.** {
    *** Companion;
}
-keepclasseswithmembers class es.ghatostudio.nexapdf.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Compose ------------------------------------------------------------------
-dontwarn org.jetbrains.compose.**

# Mantener los nombres de fichero y lineas para que los informes de fallo de
# Play Console sean legibles tras la ofuscacion.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
