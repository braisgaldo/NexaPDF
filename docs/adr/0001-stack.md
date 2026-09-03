# ADR-0001 · Kotlin Multiplatform con Compose Multiplatform

- **Fecha:** 2026-09-03
- **Estado:** aceptada

## Contexto

NexaPDF tiene que salir en Android (obligatorio), poder llevarse a iOS sin
reescribir la aplicación, y a poder ser también a escritorio. Es una app de un
solo desarrollador: mantener dos bases de código no es una opción realista.

## Alternativas consideradas

| Opción | A favor | En contra |
|---|---|---|
| **Android nativo (Kotlin + Compose)** | Lo más rápido de arrancar, todo el ecosistema disponible. | Portar a iOS es reescribirlo entero. Descartada por el requisito de portabilidad. |
| **Flutter** | Una base de código, muy buen soporte de iOS. | Dart obliga a salir del ecosistema Kotlin/JVM, y la manipulación de PDF depende de plugins que envuelven bibliotecas nativas: se acaba manteniendo el mismo código nativo, pero además con un puente por medio. |
| **KMP + SwiftUI nativa en iOS** | Interfaz idiomática en cada plataforma. | La interfaz de esta app es el 70 % del trabajo: duplicarla anula casi toda la ventaja de compartir código. |
| **KMP + Compose Multiplatform** | Dominio *e interfaz* compartidos; Kotlin en todo el proyecto; `expect`/`actual` solo donde de verdad hace falta. | Compose en iOS es más joven que SwiftUI; algunos artefactos aún se marcan como experimentales. |

## Decisión

**Kotlin Multiplatform con Compose Multiplatform**, con la interfaz completa en
`commonMain`.

La regla dura que lo sostiene: **`commonMain` no contiene ni una referencia a
Android**. Todo lo específico de plataforma entra por interfaces que declara el
código compartido y que cada plataforma implementa:

- `MotorPdf` — abrir, renderizar, unir, separar, anotar y firmar.
- `ConversorDocumentos` — PDF ↔ Word, Excel y PowerPoint.
- `ServiciosPlataforma` — ficheros, compartir, navegador, idioma, formatos de
  fecha y número.
- `SelectorFicheros` — selección de ficheros y cámara.
- `AlmacenFicheros` — lectura y escritura.

Se agrupan en `ContenedorApp`, que cada plataforma construye en su punto de
entrada. No hay biblioteca de inyección de dependencias: son cinco interfaces y
viven lo que vive el proceso; un contenedor a mano hace el mismo trabajo sin
añadir un procesador de anotaciones ni tiempo de compilación.

## Consecuencias

**A favor.** Portar a iOS es implementar cinco interfaces: PDFKit y
CoreGraphics para el motor, `UIDocumentPickerViewController` para el selector, y
`Foundation` para el resto. Ni una pantalla se reescribe.

**En contra.** Se depende de una pila joven. Ya se ha notado:

- AGP 9 dejó de admitir `com.android.application` junto al plugin de KMP en el
  mismo módulo. Se usa la ruta que documenta el propio AGP
  (`android.builtInKotlin=false` y `android.newDsl=false`), anotada en
  `gradle.properties` con su motivo.
- El `BackHandler` multiplataforma sigue marcado como experimental y hay que
  aceptarlo explícitamente. Se asume: sin él la aplicación se cerraría desde
  cualquier pantalla en vez de retroceder.
- `material3` y los iconos extendidos llevan líneas de versiones propias,
  distintas de la del release de Compose, por lo que se resuelven a través de
  los accesores del plugin y no con coordenadas sueltas.

**Versiones fijadas** en `gradle/libs.versions.toml`: Kotlin 2.4.10, Compose
Multiplatform 1.12.0, AGP 9.4.0, Gradle 9.7.1, `compileSdk`/`targetSdk` 37,
`minSdk` 26.
