# Publicar NexaPDF en Google Play

Todo lo necesario para crear la ficha y subir la versión 1.2.0. Los datos que
Play Console pide, en el orden en que los pide.

> **Consultado el 3 de septiembre de 2026.** Las políticas de las tiendas
> cambian: antes de publicar, vuelve a leer los apartados citados en la sección
> «Donaciones y política de pagos» y guarda una captura con la fecha.

---

## 1. Datos de la aplicación

| Campo | Valor |
|---|---|
| **Nombre de la app** | `NexaPDF` |
| **Nombre del paquete** (application ID) | `es.ghatostudio.nexapdf` |
| **Versión** | `1.2.0` |
| **Código de versión** | `10200` |
| **SDK mínimo** | 26 (Android 8.0 Oreo) |
| **SDK objetivo** | 37 |
| **Categoría** | Productividad |
| **Tipo** | Aplicación (no juego) |
| **Precio** | Gratuita |
| **Contiene anuncios** | No |
| **Compras integradas** | **No** |
| **Idioma predeterminado de la ficha** | Español (España) — `es-ES` |
| **Correo de contacto** | ghatostudio@proton.me |
| **Sitio web** | https://braisgaldo.github.io/NexaPDF/ |
| **Política de privacidad** | https://braisgaldo.github.io/NexaPDF/privacidad.html |

El nombre del paquete **no se puede cambiar nunca** una vez publicado. Está
elegido con dominio inverso propio (`es.ghatostudio`) para que no colisione.

## 2. Ficheros de esta carpeta

```
docs/google_play/
├── bundle/NexaPDF-1.2.0-release.aab   ← esto es lo que se sube
├── claves/nexapdf-release.p12         ← clave de firma (NO subir a git)
├── graficos/
│   ├── icono-512.png                  512 x 512, para la ficha
│   └── grafico-destacado-1024x500.png 1024 x 500
├── capturas/                          8 capturas del dispositivo real
│                                      (sólo con documentos y certificado de
│                                       prueba: ningún dato personal)
└── ficha/<locale>.md                  textos de la ficha en 13 idiomas
```

`bundle/` y `claves/` están en `.gitignore`: el binario se adjunta a la GitHub
Release y la clave de firma no puede vivir en un repositorio público. Se
regeneran con `./gradlew :composeApp:bundleRelease` y con el procedimiento de
`docs/INSTALL.md`.

## 3. La clave de firma

| | |
|---|---|
| Fichero | `claves/nexapdf-release.p12` |
| Formato | PKCS#12 |
| Alias | `nexapdf` |
| Algoritmo | RSA 4096, firma SHA384withRSA |
| Validez | 30 años (10 950 días) |
| Contraseña | en `keystore.properties`, en la raíz del proyecto |

**Guarda una copia del `.p12` y de su contraseña fuera de este ordenador.**
En la primera subida, activa **Play App Signing**: así Google custodia la clave
con la que se firma lo que llega a los usuarios y esta pasa a ser solo la clave
de *subida*, que se puede restablecer si se pierde. Sin App Signing, perder este
fichero significa no poder volver a actualizar la aplicación jamás.

Huella SHA-256 de la clave (para verificar que no se ha cambiado):

```
95:17:10:E3:14:47:8A:92:E5:9E:77:6C:0F:9B:F2:11:B6:95:BF:51:4D:71:38:C7:20:72:53:E4:0A:2C:E0:D3
```

Comprobar en cualquier momento con:

```bash
keytool -list -v -keystore docs/google_play/claves/nexapdf-release.p12
```

## 4. Formulario de Seguridad de los datos

Este es el formulario que más se falla. En NexaPDF las respuestas son todas
iguales y todas verificables en el manifiesto:

| Pregunta | Respuesta |
|---|---|
| ¿Tu app recopila o comparte alguno de los tipos de datos requeridos? | **No** |
| ¿Se cifran los datos en tránsito? | No procede (no hay tránsito) |
| ¿Pueden los usuarios solicitar la eliminación de sus datos? | No procede (no hay datos) |
| ¿La app tiene acceso a la ubicación? | No |
| ¿Recopila identificadores de dispositivo o de publicidad? | No |
| ¿Usa servicios de terceros que recopilen datos? | No |

**Por qué se puede afirmar sin matices:** la aplicación no declara el permiso
`android.permission.INTERNET`. Sin ese permiso el sistema operativo bloquea
cualquier conexión, así que no hay forma técnica de que recopile o comparta
nada. Si una revisión lo cuestiona, basta con señalar el manifiesto:
`composeApp/src/androidMain/AndroidManifest.xml`, que no contiene ninguna
etiqueta `uses-permission`.

## 5. Clasificación del contenido (cuestionario IARC)

| Pregunta | Respuesta |
|---|---|
| Categoría | Utilidad / Productividad / Comunicación |
| Violencia, sexo, lenguaje soez, drogas | No a todo |
| Juegos de azar o simulados | No |
| Contenido generado por usuarios | No |
| Los usuarios pueden interactuar entre sí | No |
| Comparte la ubicación con otros usuarios | No |
| Permite comprar bienes digitales | **No** |
| Acceso sin restricciones a internet | **No** (no hay acceso a internet) |

Clasificación esperada: **PEGI 3 / ESRB Everyone / apta para todos los públicos**.

## 6. Público objetivo

- **Rango de edad:** 18 y más. La aplicación es una herramienta de trabajo con
  documentos; no está dirigida a menores.
- **¿Atrae a menores?** No.
- Al no dirigirse a menores y no recoger datos, no aplica la política de
  Familias ni se requiere el cumplimiento de COPPA.

## 7. Donaciones y política de pagos

**Declaración de compras integradas: NO.**

La donación de NexaPDF **no es una compra integrada** y no está sujeta a la
facturación obligatoria de Google Play, por tres motivos que se cumplen a la vez:

1. **No se adquiere ningún bien ni servicio digital.** La donación no desbloquea
   funciones, ni temas, ni contenido, ni quita anuncios (no hay). Todo lo que
   hace la aplicación lo hace para todo el mundo, done o no.
2. **La transacción ocurre fuera de la aplicación**, en el navegador del sistema
   mediante Custom Tabs. Nunca en un WebView incrustado, que es lo que las
   políticas consideran un flujo de pago interno.
3. **No hay ninguna biblioteca de facturación en el binario.**
   `com.android.billingclient` no aparece ni directa ni transitivamente.

### Cómo verificarlo

```bash
./gradlew :composeApp:dependencies --configuration releaseRuntimeClasspath | grep -i billing
# sin resultados

grep -c uses-permission composeApp/src/androidMain/AndroidManifest.xml
# 0
```

La integración continua comprueba las dos cosas en cada cambio
(`.github/workflows/ci.yml`), así que no puede colarse por descuido.

### Apartados de las políticas que amparan esto

- **Google Play — Política de Pagos** (`support.google.com/googleplay/android-developer/answer/9858738`),
  apartado *«Transacciones no sujetas a la facturación de Google Play»*: las
  donaciones a organizaciones o personas por contenido que no se adquiere no
  requieren el sistema de facturación de Play.
- **Google Play — Programa para Desarrolladores**: la aplicación no ofrece
  bienes ni servicios digitales dentro de la app.

> **Antes de publicar**, vuelve a leer el apartado enlazado y guarda una captura
> con la fecha en esta misma carpeta. Estas políticas cambian y conviene tener a
> mano la redacción vigente el día de la subida.

### Lenguaje

Ni en la aplicación ni en la ficha aparecen las palabras *comprar*, *pagar*,
*desbloquear*, *pro*, *premium*, *suscripción* ni *precio*. El texto es
«invítame a un café», «apoyar el desarrollo» y «gracias».
`tools/generar_ficha.py` **falla** si alguna de esas palabras se cuela en los
textos de la ficha.

## 8. Pruebas cerradas: 12 testers durante 14 días

**Esto aplica si la cuenta de desarrollador es personal y se creó después de
noviembre de 2023.** Google exige, antes de poder pasar a producción:

- Una prueba cerrada con **al menos 12 testers**.
- Que hayan estado **suscritos a la prueba durante 14 días seguidos**, sin
  interrupción.
- Solicitar después el acceso a producción, que Google revisa.

Planifícalo desde el principio, porque son dos semanas de calendario que no se
pueden acortar:

1. Crea una **prueba cerrada** («Cerrada» → «Crear track»).
2. Sube ahí el AAB.
3. Añade los 12 correos a la lista de testers, o crea un grupo de Google.
4. Confirma que los 12 **aceptan** la invitación: los que no la aceptan no
   cuentan.
5. Deja pasar 14 días naturales sin sacar a nadie de la lista.
6. Solicita el acceso a producción y espera la revisión.

Si la cuenta de desarrollador es de organización, o es anterior a esa fecha,
este paso no aplica y se puede publicar directamente.

## 9. Coste

| Concepto | Coste |
|---|---|
| Cuenta de desarrollador de Google Play | **25 US$**, pago único |
| Servicios de nube | **0 €** (no se usa ninguno) |
| Apple Developer Program (solo si se publica en iOS) | 99 €/año |

No hay ningún servicio de pago asociado a la aplicación, así que **no hay
alertas de presupuesto que configurar**. Si algún día se añadiera uno, el
procedimiento de alertas en Google Cloud sería: *Facturación → Presupuestos y
alertas → Crear presupuesto → importe 5 € → avisos al 50 %, 90 % y 100 %*.

## 10. Lista de comprobación antes de subir

### Binario

- [ ] `./gradlew :composeApp:bundleRelease` termina sin errores.
- [ ] `jarsigner -verify` sobre el AAB dice `jar verified`.
- [ ] `versionCode` es mayor que el de la versión anterior en Play.
- [ ] El manifiesto no declara ningún permiso.
- [ ] No hay `billingclient` en las dependencias.
- [ ] Probado el flujo principal en un dispositivo real.

### Ficha

- [ ] Título, descripción corta y larga en los 13 idiomas (`ficha/`).
- [ ] Icono 512 × 512 subido.
- [ ] Gráfico destacado 1024 × 500 subido.
- [ ] Al menos 2 capturas de teléfono (mínimo de Play; hay 8 en `capturas/`).
- [ ] Categoría: Productividad.
- [ ] Aviso de «símbolos de depuración» al subir el AAB: es opcional. El código
      nativo no es de NexaPDF, son dos bibliotecas de AndroidX
      (`androidx.graphics:graphics-path` y el contador de DataStore). Si se
      quiere quitar el aviso, `python tools/simbolos_nativos.py <ruta al .aab>`
      arma el ZIP y se sube en la versión, en «App bundles», menú de tres
      puntos → «Subir archivo de símbolos de depuración nativos». La traza de
      los fallos de la propia aplicación no depende de esto: la desofusca el
      `proguard.map`, que ya viaja dentro del AAB.
- [ ] Correo de contacto verificado.

### Declaraciones

- [ ] Seguridad de los datos: no se recopilan ni se comparten datos.
- [ ] Clasificación de contenido completada.
- [ ] Público objetivo: 18+.
- [ ] Anuncios: No.
- [ ] Compras integradas: **No**.
- [ ] URL de política de privacidad accesible y con contenido real.

### Antes de darle a publicar

- [ ] Política de Pagos de Play releída y captura guardada con la fecha.
- [ ] Play App Signing activado.
- [ ] Copia de seguridad del `.p12` y su contraseña **fuera** de este ordenador.
- [ ] Prueba cerrada con 12 testers durante 14 días, si aplica.
- [ ] Etiqueta `v1.2.0` creada y GitHub Release publicada con el AAB adjunto.

## 11. App Store (para cuando toque)

Los equivalentes de Apple, resumidos, están en `docs/adr/0003-portabilidad.md`.
Lo que conviene saber ya:

- El **Apple Developer Program cuesta 99 €/año** y es inevitable para publicar.
- Se necesita un Mac con Xcode: Kotlin/Native no compila para Apple desde
  Windows ni Linux.
- **Las donaciones deben ir desactivadas de salida** (`donacionesDisponibles =
  false` en iOS). Las directrices 3.1.1 y 3.2.1 de App Review son bastante más
  estrictas que Google con los enlaces de pago externos. Se activarían solo si
  App Review lo acepta, y como alternativa el enlace apuntaría a la página del
  proyecto en GitHub Pages en lugar de a la pasarela.
- Apple pide su propio cuestionario de privacidad (*App Privacy*), con las
  mismas respuestas que aquí: no se recoge ningún dato.

## 12. Distribución fuera de las tiendas

El APK firmado se publica también en las **GitHub Releases** y es apto para
**F-Droid**. Ahí no aplica ninguna política de facturación, y es la vía para
quien no quiere pasar por Google Play. Se menciona en el `README.md`.
