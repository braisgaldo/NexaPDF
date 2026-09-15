# Política de privacidad de NexaPDF

**Última actualización: 14 de septiembre de 2026**
**Aplicación:** NexaPDF · **Identificador:** `es.ghatostudio.nexapdf`
**Responsable:** Brais Castiñeiras Galdo (Ghato Studio) · ghatostudio@proton.me

---

## Resumen en una frase

NexaPDF no recoge, no almacena y no transmite ningún dato personal, porque no
puede: la aplicación no declara el permiso de acceso a internet.

> **Novedad de la versión 1.5.0.** El escáner de documentos usa la cámara, y por
> eso la aplicación ha pasado de declarar cero permisos a declarar uno. Lo que
> **no** cambia es lo de arriba: sigue sin poder conectarse a ninguna red, así
> que las fotos que hagas no salen del teléfono. El razonamiento completo está
> en [`docs/adr/0005-camara-escaner.md`](adr/0005-camara-escaner.md).

---

## Qué datos se recogen

**Ninguno.**

NexaPDF no recoge datos de uso, ni identificadores de dispositivo, ni de
publicidad, ni datos de localización, ni contactos, ni información de la cuenta.
No hay analítica, no hay telemetría, no hay informes de fallos automáticos y no
hay publicidad.

## Qué pasa con tus documentos

Los PDF, imágenes y documentos de ofimática que abres con NexaPDF **no salen de
tu dispositivo**. Todo el procesado (unir, separar, convertir, editar y firmar)
se hace con el procesador de tu teléfono.

Cuando eliges un fichero, NexaPDF hace una copia de trabajo en su carpeta
privada. Esa carpeta **se vacía cada vez que arrancas la aplicación**: solo
existe mientras dura la tarea.

## Qué pasa con las fotos del escáner

Lo mismo, y conviene detallarlo porque la cámara siempre levanta sospechas, con
razón:

- Las fotos que haces con el escáner se guardan en la **carpeta privada** de la
  aplicación, no en tu galería. No aparecen en Google Fotos ni en ninguna otra
  aplicación.
- Se borran solas: las páginas intermedias en cuanto el PDF está montado, y la
  carpeta entera al arrancar la aplicación.
- La cámara solo está encendida mientras la pantalla del escáner está abierta.
  Al salir se apaga; no hay ningún servicio en segundo plano.
- **El reconocimiento de texto ocurre en tu teléfono.** El modelo que lee las
  palabras viaja dentro de la propia aplicación, no se descarga y no consulta
  nada. El escáner funciona igual en modo avión, que es la forma más rápida de
  comprobarlo.

Los documentos que generas se guardan en `Descargas/NexaPDF`, en tu propio
teléfono, para que puedas encontrarlos con cualquier gestor de archivos.

## Certificados de firma electrónica

Si firmas un documento con un certificado, NexaPDF lee el fichero `.p12` o
`.pfx` que elijas, lo usa para firmar en el momento y **lo descarta al
terminar**. Ni el certificado ni su contraseña se guardan en ningún sitio, ni
dentro de la aplicación ni fuera de ella.

## Permisos que solicita la aplicación

NexaPDF declara **un solo permiso**. En concreto:

| Permiso | ¿Se solicita? | Por qué |
|---|---|---|
| `CAMERA` | **Sí**, desde la 1.5.0 | Solo para el escáner de documentos, y solo mientras esa pantalla está abierta. Hace falta para encontrar los bordes del papel mientras apuntas; una foto ya hecha no permite eso. Se pide al entrar en el escáner, nunca al arrancar, y si lo deniegas el escáner sigue funcionando con fotos de la galería. |
| `INTERNET` | **No** | La aplicación no se conecta a ninguna red. Las bibliotecas de cámara y de reconocimiento de texto lo declaran para telemetría que aquí no se usa, y NexaPDF **lo elimina expresamente** al fusionar el manifiesto. |
| `READ_EXTERNAL_STORAGE` | **No** | Los ficheros se eligen con el selector del sistema, que concede acceso solo a lo que elijas. |
| `WRITE_EXTERNAL_STORAGE` | **No** | Los resultados se guardan mediante MediaStore, que no requiere permiso. |
| `ACCESS_*_LOCATION` | **No** | La aplicación no usa la ubicación. |
| `RECORD_AUDIO` | **No** | El escáner hace fotos; no graba ni vídeo ni sonido. |

Puedes comprobarlo tú mismo sobre el APK publicado:

```
aapt2 dump permissions NexaPDF-1.5.0-release.apk
```

Tienen que aparecer `android.permission.CAMERA` y nada más. Que `INTERNET` no
esté es lo que hace que todo lo anterior no dependa de que nos creas: sin ese
permiso es el propio sistema operativo el que bloquea cualquier conexión, la
intente el código de NexaPDF o el de cualquier biblioteca que lleve dentro. La
integración continua lo comprueba en cada cambio sobre el manifiesto ya
fusionado.

Además aparece
`es.ghatostudio.nexapdf.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. No es un
permiso del sistema ni se te pide nunca: la biblioteca `androidx.core` la define
y la usa la propia aplicación para hablar consigo misma, es de nivel *signature*
(solo la concede el sistema a apps firmadas con la misma clave) y no da acceso a
nada del teléfono. Google Play no la muestra en la ficha por ese motivo.

## Enlaces externos

La aplicación tiene tres enlaces que abren el **navegador del sistema**, nunca
una ventana dentro de la aplicación:

1. Esta misma política de privacidad.
2. El código fuente del proyecto en GitHub.
3. El enlace de donación voluntaria (Revolut).

Al pulsarlos sales de NexaPDF. Lo que ocurra en esos sitios se rige por sus
propias políticas de privacidad, sobre las que NexaPDF no tiene control ni
información.

## Donaciones

La donación es **voluntaria y no desbloquea nada**: todas las funciones de
NexaPDF son gratuitas y lo seguirán siendo. Si decides donar, el pago se realiza
íntegramente en Revolut, fuera de la aplicación. NexaPDF **no recibe, no procesa
y no ve** ningún dato de pago: ni tarjetas, ni importes, ni si has donado o no.

## Copias de seguridad de Android

Si tienes activada la copia de seguridad de Android, se respalda únicamente el
fichero de preferencias de la aplicación (tema, idioma y firmas manuscritas que
hayas guardado). Los documentos **quedan excluidos expresamente** de la copia:
son ficheros tuyos y no tiene sentido subirlos a la nube de Google sin
necesidad. Puedes revisarlo en `backup_rules.xml` del código fuente.

## Menores de edad

NexaPDF no está dirigida específicamente a menores, pero tampoco recoge ningún
dato de nadie, con lo que no existe tratamiento de datos de menores.

## Tus derechos

El Reglamento General de Protección de Datos reconoce los derechos de acceso,
rectificación, supresión, oposición, limitación y portabilidad. En el caso de
NexaPDF **no hay ningún dato que ejercer sobre él**, porque no existe ningún
tratamiento de datos personales. Si quieres confirmarlo por ti mismo, el código
fuente es público y puedes revisar el manifiesto y el código de red (que no
existe).

## Cambios en esta política

Si en el futuro alguna versión de NexaPDF tratara algún dato, esta política se
actualizaría **antes** de publicar esa versión, y el cambio se recogería en el
`CHANGELOG.md` del proyecto. La fecha de la parte superior indica la última
revisión.

## Contacto

Para cualquier duda sobre privacidad: **ghatostudio@proton.me**

---

*Código fuente: <https://github.com/braisgaldo/NexaPDF>*
