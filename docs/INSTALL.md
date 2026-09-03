# Instalación, compilación y firma

## Requisitos

| | Versión | Nota |
|---|---|---|
| JDK | 17 | Temurin. Gradle y AGP 9 no funcionan con 8 ni con 11. |
| Android SDK | Platform 37, Build-Tools 36 | Los descarga AGP solo si las licencias están aceptadas. |
| Gradle | 9.7.1 | No hace falta instalarlo: usa el *wrapper*. |
| Dispositivo | Android 8.0 (API 26) o superior | `minSdk = 26`. |

Python 3.10 o superior solo hace falta para regenerar textos, colores, el sitio
web o los ficheros de prueba. No se necesita para compilar.

## Preparar el entorno desde cero

```bash
# JDK 17 (Temurin), sin permisos de administrador
curl -L -o jdk.zip \
  "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
unzip jdk.zip -d ~/dev-tools/jdk

# Herramientas de línea de órdenes de Android
curl -L -o cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-win-16111833_latest.zip"
unzip cmdline-tools.zip -d ~/dev-tools/android-sdk/cmdline-tools
mv ~/dev-tools/android-sdk/cmdline-tools/cmdline-tools \
   ~/dev-tools/android-sdk/cmdline-tools/latest

export JAVA_HOME=~/dev-tools/jdk/jdk-17...
export ANDROID_HOME=~/dev-tools/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

# Plataforma y herramientas de compilación
"$ANDROID_HOME/cmdline-tools/latest/bin/android" sdk install \
  "platforms;android-37" "build-tools;36.0.0" "platform-tools"
```

Después, apunta el proyecto al SDK creando `local.properties` en la raíz:

```properties
sdk.dir=/ruta/a/dev-tools/android-sdk
```

`local.properties` está en `.gitignore`: es de tu máquina y no se comparte.

## Compilar

```bash
./gradlew :composeApp:assembleDebug     # APK de depuración
./gradlew :composeApp:bundleRelease     # AAB firmado para Google Play
./gradlew :composeApp:assembleRelease   # APK firmado para las Releases y F-Droid
```

## Pruebas

```bash
./gradlew :composeApp:desktopTest                 # 32 pruebas unitarias
./gradlew :composeApp:connectedDebugAndroidTest   # 31 pruebas en el dispositivo
./gradlew :composeApp:lintRelease                 # lint de Android
```

Las pruebas en dispositivo necesitan un móvil conectado con depuración USB
activada. Comprueba que se ve con `adb devices`.

## Firma de release

### Dónde vive la clave

**El keystore no está en el repositorio y nunca debe estarlo.** Se lee de
`keystore.properties` en la raíz del proyecto (ignorado por git):

```properties
storeFile=docs/google_play/claves/nexapdf-release.p12
storePassword=...
keyAlias=nexapdf
keyPassword=...
```

Como alternativa, y es lo que usa la integración continua, se pueden dar por
variables de entorno: `NEXAPDF_STORE_FILE`, `NEXAPDF_STORE_PASSWORD`,
`NEXAPDF_KEY_ALIAS` y `NEXAPDF_KEY_PASSWORD`.

La ruta de `storeFile` se resuelve **contra la raíz del proyecto**, no contra el
módulo. Si el fichero no existe, la compilación avisa por consola y el resultado
sale **sin firmar**: conviene mirar ese aviso antes de subir nada.

### Regenerar la clave

```bash
keytool -genkeypair \
  -keystore docs/google_play/claves/nexapdf-release.p12 -storetype PKCS12 \
  -alias nexapdf -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=Brais Castineiras Galdo, OU=Ghato Studio, O=Ghato Studio, L=A Coruna, C=ES"
```

10 950 días son treinta años: Google Play exige que la clave siga siendo válida
después de 2033.

### Si se pierde la clave

Con **Play App Signing** activado (recomendado y activado en la primera subida),
perder la clave de *subida* no es grave: se solicita a Google el restablecimiento
de la clave de subida desde Play Console y se sigue publicando. La clave de
*firma de la app*, que es la que ve el usuario, la custodia Google.

Sin Play App Signing, perder la clave significa **no poder actualizar nunca más
la aplicación**: habría que publicarla con otro identificador de paquete y pedir
a todos los usuarios que la reinstalen. Por eso está activado.

### Verificar que el resultado está firmado

```bash
jarsigner -verify -certs composeApp/build/outputs/bundle/release/composeApp-release.aab
```

Debe decir `jar verified`. Los avisos de «certificado autofirmado» y «sin sello
de tiempo» son normales y esperables en una clave de subida de Android.

## Versionado

`versionName` sigue SemVer y se declara en `composeApp/build.gradle.kts`.

`versionCode` se deriva de él con una fórmula, para que no puedan
desincronizarse:

```
versionCode = MAJOR * 10 000 + MINOR * 100 + PATCH
```

Así, `1.0.0` → `10000`, `1.2.3` → `10203`, `2.0.0` → `20000`. La sucesión es
monótona creciente mientras MINOR y PATCH no pasen de 99, que es de sobra.

## Regenerar recursos

```bash
python tools/generar_colores.py          # tokens de color, verifica contraste
python tools/generar_traducciones.py     # los trece strings.xml
python tools/generar_sitio.py            # sitio de GitHub Pages
python tools/generar_ficheros_prueba.py  # PDF e imágenes de prueba
python tools/generar_ofimatica_prueba.py # .docx, .xlsx y .pptx de prueba
```

Los dos primeros **fallan** si algo no cuadra (un par de colores por debajo de
4,5:1, una clave sin traducir a alguno de los trece idiomas), así que sirven
igual de bien como comprobación en la integración continua.
