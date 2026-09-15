# ADR 0005 · Declarar el permiso de cámara para el escáner

- **Estado:** aceptado
- **Fecha:** 14 de septiembre de 2026
- **Versión:** 1.5.0

## Contexto

Hasta la versión 1.3.0 NexaPDF **no declaraba ningún permiso**. No era un
accidente: estaba escrito en el manifiesto, en la política de privacidad, en la
ficha de Google Play y en el `README`, y la integración continua fallaba si
alguien metía un `uses-permission`.

Eso se consiguió delegando toda captura de fotos en la aplicación de cámara del
teléfono, mediante `ACTION_IMAGE_CAPTURE`. Funciona bien para «Imágenes a PDF»,
que es la herramienta que había: el usuario hace una foto con la cámara que ya
conoce y vuelve con el fichero.

La versión 1.5.0 añade un escáner de documentos. Su razón de ser es enseñar,
**mientras se apunta**, dónde están los bordes del papel y hasta qué punto se
distinguen, y disparar solo cuando el encuadre es bueno y la mano está quieta.

## Decisión

Declarar `android.permission.CAMERA` y abrir la cámara dentro de la aplicación
con CameraX, **solo** para el escáner.

Se pide al entrar en el escáner, nunca al arrancar, después de una pantalla que
explica para qué es y hasta dónde llega. Si se deniega, el escáner sigue
funcionando con fotos de la galería y el resto de la aplicación no se entera.

## Por qué no bastaba con lo que había

La cámara del sistema devuelve **una foto ya hecha**. No hay forma de ver los
fotogramas antes del disparo, y sin verlos no se puede:

- dibujar el contorno del papel encima de lo que se está viendo;
- decir qué porcentaje del borde se distingue de verdad;
- disparar solo cuando el encuadre está bien y el contorno lleva varios
  fotogramas sin moverse.

Se consideró detectar los bordes **después** de la foto, conservando cero
permisos. Se descartó: convierte el escáner en un editor de recortes a
posteriori. La persona hace la foto a ciegas, y si salió torcida o con media
hoja fuera se entera al volver, con el papel ya recogido. La diferencia entre
una foto de un folio y un documento escaneado está justo en ese momento.

## Lo que **no** cambia

Esto es lo importante y conviene decirlo con precisión, porque las dos promesas
se confundían en una sola:

| Promesa | Antes | Ahora |
|---|---|---|
| «No declara **ningún** permiso» | cierta | **ya no**: declara `CAMERA` |
| «No puede salir a internet» | cierta | **cierta**, sin matices |

La segunda es la que protege al usuario, y es la única que el sistema operativo
hace cumplir por su cuenta: sin `android.permission.INTERNET`, el núcleo bloquea
cualquier conexión, la haga el código de NexaPDF o el de una biblioteca.

CameraX y ML Kit declaran `INTERNET` en sus manifiestos para telemetría que aquí
no se usa. Se elimina en la fusión:

```xml
<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
```

El modelo de reconocimiento de texto viaja **dentro del APK** (la variante
empaquetada de ML Kit, no la que descarga desde Google Play), así que quitarlo
no rompe nada: el escáner y el OCR funcionan igual con el teléfono en modo
avión.

## Consecuencias

- La integración continua deja de comprobar «que no haya permisos» y pasa a una
  **lista blanca**: cualquier permiso que no sea `CAMERA` falla la compilación y
  obliga a escribir otro ADR. La regla protege lo mismo que antes sin impedir
  este cambio.
- Se añade una segunda comprobación, más fuerte que la que había: el manifiesto
  **fusionado** (no el nuestro) no puede contener `INTERNET` ni
  `ACCESS_NETWORK_STATE`. Eso cierra la vía por la que el permiso entraría de
  verdad, que es una dependencia y no un descuido propio.
- Hay que actualizar la política de privacidad, la ficha de Google Play y el
  formulario de Seguridad de los datos. Las respuestas del formulario **no
  cambian**: la cámara no es un dato recopilado ni compartido, porque las fotos
  no salen del dispositivo.
- `<uses-feature android:required="false">`: una tableta sin cámara tiene que
  poder instalar la aplicación, porque las otras ocho herramientas no la
  necesitan.
- El AAB crece: CameraX y el modelo de texto empaquetado suman unos megabytes.
  Se asume; la alternativa era descargar el modelo, y eso sí habría exigido
  internet.

## Alternativas descartadas

**ML Kit Document Scanner** (`play-services-mlkit-document-scanner`). Resuelve
el escáner entero con una llamada y lo hace muy bien. Descartada por dos cosas a
la vez: descarga el módulo desde Google Play la primera vez, lo que exige red y
tener Play Services, y la interfaz es de Google, no de la aplicación, así que ni
respeta los seis temas ni los trece idiomas de NexaPDF.

**OpenCV** para la detección de bordes. Es la herramienta natural y habría
ahorrado el detector. Descartada por tamaño: unos veinte megabytes de código
nativo en una aplicación que entera pesa seis, y además habría dejado la
detección fuera del alcance de las pruebas unitarias, que es donde se ha
depurado (ver `DetectorDocumentoTest` y `EstabilidadDeteccionTest`). El detector
propio son unas cuatrocientas líneas de Kotlin que corren en `commonMain` y
tardan un par de milisegundos por fotograma.

**Tesseract** para el reconocimiento de texto. Libre y sin nada de Google, que
es un punto a favor. Descartada por precisión y por tamaño: los ficheros de
idioma pesan más que el modelo empaquetado de ML Kit y acierta bastante menos en
fotos con iluminación desigual, que es exactamente lo que produce un teléfono.
