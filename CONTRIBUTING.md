# Cómo contribuir a NexaPDF

Gracias por el interés. Este documento explica lo que hace falta saber antes de
mandar un cambio.

## Antes de escribir código

Abre una incidencia describiendo el problema o la mejora. Es mucho mejor
descartar una idea en tres líneas que después de trescientas.

## Preparar el entorno

Los requisitos exactos (JDK, SDK de Android, cómo compilar y cómo firmar) están
en [`docs/INSTALL.md`](docs/INSTALL.md).

```bash
./gradlew :composeApp:assembleDebug          # compilar
./gradlew :composeApp:desktopTest            # pruebas unitarias
./gradlew :composeApp:connectedDebugAndroidTest   # pruebas en el dispositivo
./gradlew :composeApp:lintRelease            # lint de Android
```

## Reglas del proyecto

Son pocas y no negociables, porque cada una tiene un motivo concreto.

1. **Nada de Android en `commonMain`.** El código compartido no importa una sola
   clase del SDK de Android. Lo específico de plataforma entra por interfaces
   (`MotorPdf`, `ServiciosPlataforma`, `SelectorFicheros`, `ConversorDocumentos`)
   que cada plataforma implementa. Si para añadir algo hay que romper esto, el
   diseño de ese algo está mal.

2. **Ninguna cadena escrita a fuego en la interfaz.** Todo el texto vive en
   `tools/traducciones*.py` y se genera con
   `python tools/generar_traducciones.py`. El generador falla si a una clave le
   falta uno de los trece idiomas, así que no se puede olvidar una traducción.

3. **Ningún color suelto.** Los colores de la interfaz salen siempre de
   `MaterialTheme.colorScheme`. Los tokens los genera
   `python tools/generar_colores.py`, que además verifica el contraste y falla si
   algún par baja de 4,5:1. Las únicas excepciones son las banderas de los
   idiomas y la paleta del editor, que son *contenido* y no interfaz.

4. **Nada de bibliotecas de facturación.** `com.android.billingclient` no puede
   aparecer en el proyecto, ni directa ni transitivamente. La donación es un
   enlace externo y no desbloquea absolutamente nada. Comprobable con
   `./gradlew :composeApp:dependencies`.

5. **Permisos: ninguno.** Si un cambio necesita un permiso, hay que justificar en
   un ADR por qué no hay otra forma, y actualizar la política de privacidad y la
   ficha de la tienda antes de publicarlo.

## Estilo

- Kotlin oficial, líneas de hasta 100 caracteres, comas finales.
- **Identificadores de código en inglés no**: en este proyecto el dominio está
  nombrado en castellano (`MotorPdf`, `DocumentoPdf`, `alGuardar`) y conviene
  mantener la coherencia. Los nombres de las APIs de terceros se dejan como
  están, claro.
- Los comentarios explican **por qué**, no qué. Si un comentario repite lo que
  dice la línea de abajo, sobra.

## Commits

Conventional commits **en castellano**, con un cuerpo que explique el motivo:

```
feat(editor): permitir sustituir el texto de una linea

Tocar una linea y reescribirla es lo que la gente espera al "editar" un
PDF. Se tapa la original y se escribe encima, porque un PDF guarda
posiciones fijas y no admite recomposicion de parrafos.
```

## Pruebas

Un cambio de comportamiento viene con su prueba:

- **Dominio y lógica pura** → `commonTest`, se ejecuta con `desktopTest`.
- **Motor de PDF y conversor** → `androidInstrumentedTest`, porque dependen de
  APIs que solo existen en el dispositivo.

## Traducciones

Si hablas alguno de los trece idiomas y ves algo mal traducido, corrígelo en
`tools/traducciones*.py` y regenera. Es una de las contribuciones más útiles y
la más fácil de revisar.
