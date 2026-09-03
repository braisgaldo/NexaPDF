# ADR-0003 · Portabilidad a iOS y escritorio

- **Fecha:** 2026-09-03
- **Estado:** aceptada (con trabajo pendiente identificado)

## Contexto

El encargo pide Android como obligatorio, iOS como obligatorio en cuanto a
**portabilidad**, y escritorio como deseable. Hay que dejar claro qué está hecho,
qué falta y qué impide terminarlo hoy.

## Estado real

| Plataforma | Estado | Qué falta |
|---|---|---|
| **Android** | Completo y publicable. | — |
| **iOS** | Interfaz y dominio listos; sin implementación de plataforma. | `ContenedorApp` con PDFKit, y compilar en un Mac. |
| **Escritorio** | Arranca y muestra una ventana. | `ContenedorApp` de JVM. |

## El impedimento honesto con iOS

**No se ha compilado el target de iOS, y no se puede desde aquí.** Kotlin/Native
solo genera binarios para Apple desde macOS: necesita el toolchain de Xcode, que
Apple no distribuye para Windows ni Linux. Todo este proyecto se ha desarrollado
en Windows.

Por eso los targets de iOS se declaran condicionalmente:

```kotlin
if (HostManager.hostIsMac) {
    iosX64(); iosArm64(); iosSimulatorArm64()
}
```

Declararlos siempre haría fallar la compilación en Windows sin aportar nada.
Presentar «compila para iOS» sin haberlo compilado sería mentir.

## Qué hace falta para cerrar iOS

Todo el trabajo es implementar cinco interfaces. Ninguna pantalla se toca.

1. **`MotorPdf`** con PDFKit y CoreGraphics. `PDFDocument` cubre abrir, unir,
   separar y reordenar casi línea por línea. Las anotaciones se dibujan con
   `CGContext` sobre `PDFPage`. La firma con certificado es lo más laborioso:
   PDFKit no firma, hay que construir el PKCS#7 con `Security.framework`.
2. **`ConversorDocumentos`**. La lectura y escritura de OOXML es ZIP más XML: el
   código de `PaqueteOoxml` es Kotlin puro y **podría subirse a `commonMain`** en
   cuanto se sustituya `java.util.zip` por una biblioteca multiplataforma. Está
   en `androidMain` solo por eso.
3. **`ServiciosPlataforma`** con `Foundation`: `FileManager`, `UIActivity­View­Controller`,
   `SFSafariViewController`, `NSLocale` y `NumberFormatter`.
4. **`SelectorFicheros`** con `UIDocumentPickerViewController` y
   `UIImagePickerController`.
5. **`AlmacenFicheros`** con `FileManager`.

Además, para publicar en la App Store: el Apple Developer Program cuesta
**99 €/año** y no hay forma de evitarlo.

### Donaciones en iOS

`ServiciosPlataforma.donacionesDisponibles` existe precisamente para esto. En
Android vale `true`. En iOS debe empezar en **`false`**: las directrices 3.1.1 y
3.2.1 de App Review son bastante más estrictas con los enlaces de pago externos
que la política de Google Play. Se activará solo si App Review lo acepta, y como
alternativa el enlace apuntará a la página del proyecto en GitHub Pages en lugar
de directamente a la pasarela.

## Escritorio

`composeApp/src/desktopMain` arranca una ventana y muestra la versión. Falta el
`ContenedorApp` de JVM, que es el más fácil de los tres: PDFBox de JVM (la misma
API que usa Android), `JFileChooser` y `java.awt.Desktop`.

La distribución ya está configurada en `compose.desktop` para generar `.msi`,
`.dmg` y `.deb`, que irían a las Releases de GitHub sin pasar por ninguna tienda.

## Decisión

Se entrega Android completo y la base multiplataforma preparada y verificada por
construcción (`commonMain` no compila contra Android). iOS y escritorio quedan
como trabajo identificado y acotado, no como promesa vaga.
