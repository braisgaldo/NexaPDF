# Arquitectura de NexaPDF

## En una frase

Una aplicación Kotlin Multiplatform en la que **la interfaz y el dominio son
código compartido** y lo específico de cada sistema operativo entra por cinco
interfaces.

## Estructura

```
composeApp/src/
├── commonMain/                  ← todo esto es portable, sin Android dentro
│   ├── kotlin/es/ghatostudio/nexapdf/
│   │   ├── domain/
│   │   │   ├── model/           Geometria, Documento, Edicion, Ajustes
│   │   │   ├── pdf/             MotorPdf, ConversorDocumentos, ResultadoPdf
│   │   │   └── plataforma/      ServiciosPlataforma, SelectorFicheros, AlmacenFicheros
│   │   ├── data/                RepositorioAjustes (DataStore), CodecCopiaSeguridad
│   │   ├── di/                  ContenedorApp
│   │   └── ui/
│   │       ├── theme/           ColorTokens (generado), AppTheme, Type, Shapes
│   │       ├── i18n/            Idiomas, Banderas (dibujadas en Compose)
│   │       ├── componentes/     BarraSuperior, MiniaturaPagina, Reordenable…
│   │       ├── donacion/        HojaDonacion, IlustracionCafe, CodificadorQr
│   │       ├── pantallas/       Inicio, Documento, Imagenes, Editor, Firma, Ajustes…
│   │       ├── navegacion/      Destino
│   │       ├── EstadoApp.kt     ViewModel de aplicación
│   │       └── NexaPdfApp.kt    Raíz: tema, navegación y orquestación
│   └── composeResources/        values, values-es … values-eu (generados)
├── androidMain/
│   ├── kotlin/…/pdf/            MotorPdfAndroid, ConversorDocumentosAndroid,
│   │                            PintorEdiciones, FiltrosPagina, FirmadorPdf,
│   │                            TransformadorPagina, FuentesPdf, ofimatica/
│   ├── kotlin/…/plataforma/     Servicios, Selector y Almacén de Android
│   └── res/                     iconos, temas de arranque, locales_config…
├── desktopMain/                 esqueleto (ver ADR-0003)
├── commonTest/                  32 pruebas: QR, copias, donación, geometría
└── androidInstrumentedTest/     31 pruebas en dispositivo: motor y conversor
```

## La regla que sostiene el diseño

**`commonMain` no importa nada de Android.** No es una convención: si alguien lo
intenta, el módulo deja de compilar para los targets no-Android.

Lo específico de plataforma se declara como interfaz en `commonMain` y se
implementa en cada plataforma:

| Interfaz | Qué resuelve | En Android |
|---|---|---|
| `MotorPdf` | Abrir, renderizar, unir, separar, reordenar, anotar, firmar | PDFBox + `android.graphics.pdf.PdfRenderer` |
| `ConversorDocumentos` | PDF ↔ Word, Excel, PowerPoint, imágenes | ZIP + XML a mano (ADR-0004) |
| `ServiciosPlataforma` | Carpetas, compartir, navegador, idioma, formatos | `Context`, Custom Tabs, MediaStore, `LocaleManager` |
| `SelectorFicheros` | Elegir ficheros, hacer fotos, guardar como | Storage Access Framework, `TakePicture` |
| `AlmacenFicheros` | Leer y escribir | `java.io.File` |

Las cinco se agrupan en `ContenedorApp`, que cada plataforma construye en su
punto de entrada y se reparte por un `CompositionLocal`. No hay biblioteca de
inyección de dependencias: cinco dependencias que viven lo que vive el proceso no
justifican un procesador de anotaciones.

## Decisiones que conviene conocer

### El motor de PDF usa dos bibliotecas, no una

- **`android.graphics.pdf.PdfRenderer`** para *mostrar* páginas. Es el
  renderizador del sistema, está acelerado por hardware y tarda milisegundos.
- **PDFBox** para *modificar*: unir, separar, anotar, incrustar imágenes, firmar.

Usar PDFBox también para renderizar haría que pasar páginas fuese notablemente
más lento. Usar solo `PdfRenderer` es imposible: no sabe escribir.

Los documentos cifrados se descifran una vez al abrirlos y se trabaja sobre la
copia, porque `PdfRenderer` no admite contraseñas.

### Las ediciones son datos, no píxeles

Dibujar, escribir o poner una forma **no toca el PDF**. Se guarda una lista de
objetos `Edicion` (trazos, figuras, texto, imágenes, tapados, firmas) en
coordenadas **normalizadas 0..1** con el origen arriba a la izquierda.

Eso da tres cosas gratis: dibujar es instantáneo por pesado que sea el
documento, deshacer es quitar el último elemento de una lista, y el mismo dato
sirve para pintar en pantalla y para escribir en el PDF.

Al guardar, `PintorEdiciones` los convierte en contenido real. `TransformadorPagina`
resuelve ahí el problema serio: un PDF mide en puntos desde abajo a la izquierda
y además puede llevar una rotación `/Rotate` que el lector aplica al mostrarlo
pero que no está en el contenido. En vez de convertir punto a punto, se instala
una matriz que lleva del «espacio visible» al espacio de la página; a partir de
ahí todo (texto, imágenes, trazos) se dibuja con las coordenadas que ve el
usuario.

### Los filtros son la única excepción: rasterizan

No hay forma de aplicar un ajuste de contraste al contenido vectorial de un PDF
sin convertirlo antes en imagen. Solo se hace en las páginas que el usuario
marca, y la interfaz avisa de que esas páginas pierden el texto seleccionable.
El umbral de blanco y negro usa el método de Otsu, que es el que usan los
escáneres y funciona sin ajustes con iluminación desigual.

### El texto y las trece escrituras

Escribir texto en un PDF choca con cómo funcionan las fuentes:

1. Las catorce fuentes estándar no se incrustan y dan un fichero diminuto, pero
   solo cubren Latin-1.
2. Una fuente del sistema incrustada como Type0 añade griego y cirílico, y el
   texto sigue siendo seleccionable.
3. Ninguna de las dos sabe **conformar** la escritura árabe, donde cada letra
   cambia de forma según sus vecinas.

`FuentesPdf` prueba la opción más barata que represente el texto (preguntando
con `getStringWidth`, que falla igual que `showText` ante un glifo que falte) y,
si ninguna sirve, avisa a `PintorEdiciones` para que lo dibuje como imagen con
`StaticLayout` de Android, que sí conforma cualquier escritura. Se pierde la
selección de ese texto y se gana que salga bien.

### Sin biblioteca de navegación

Ocho destinos, ninguno con enlaces profundos ni argumentos que serializar a una
URL. Una pila de destinos en memoria (`EstadoApp`) hace lo mismo sin añadir un
plugin de compilación ni rutas escritas como texto que fallan en ejecución.

El botón atrás se atiende con `BackHandler` en la raíz; el editor instala el
suyo, que tiene prioridad por estar compuesto más adentro, para preguntar antes
de descartar lo dibujado.

### Colores y textos: generados y verificados

- **`ColorTokens.kt`** lo genera `tools/generar_colores.py` a partir de rampas
  tonales CIELAB (el número de tono es el L\* del color), y el propio generador
  **falla** si algún par de roles baja de 4,5:1 en texto o 3:1 en bordes. Los
  seis temas pasan con 5,47:1 en el peor caso.
- **Los trece `strings.xml`** los genera `tools/generar_traducciones.py` desde un
  catálogo único, y **falla** si a una clave le falta un idioma o si los
  marcadores de posición no coinciden entre traducciones.

En ambos casos el generador sirve también como comprobación en la integración
continua: si algo no cuadra, no compila.

### El código QR se genera aquí

`CodificadorQr` implementa la norma ISO/IEC 18004 en Kotlin puro (modo byte,
versiones 1 a 10). Alternativa era ZXing, que es solo de Android y rompería la
regla de `commonMain`, o una biblioteca multiplataforma poco mantenida.

Está verificado contra dos implementaciones independientes: las matrices
coinciden con las de la biblioteca `qrcode` de Python fijando la máscara, y las
que produce la app con su propia máscara se decodificaron con OpenCV y devuelven
el texto original.

## Dependencias y por qué está cada una

| Biblioteca | Versión | Para qué | Licencia |
|---|---|---|---|
| Kotlin | 2.4.10 | Lenguaje | Apache-2.0 |
| Compose Multiplatform | 1.12.0 | Interfaz compartida | Apache-2.0 |
| kotlinx-coroutines | 1.11.0 | Concurrencia | Apache-2.0 |
| kotlinx-serialization | 1.11.0 | Copias `.nexaPDF.bak` y ediciones | Apache-2.0 |
| AndroidX DataStore | 1.2.1 | Preferencias | Apache-2.0 |
| AndroidX Lifecycle | 2.11.0 | ViewModel y ciclo de vida | Apache-2.0 |
| PDFBox-Android | 2.0.27.0 | Manipulación de PDF | Apache-2.0 |
| BouncyCastle | 1.84 | Firma PKCS#7 | MIT |

**No hay** biblioteca de facturación, ni de analítica, ni de publicidad, ni de
red. Comprobable con `./gradlew :composeApp:dependencies`.

Dos notas sobre BouncyCastle: se usa la familia `jdk15to18` porque es la que trae
PDFBox (mezclarla con `jdk18on` duplicaría clases y D8 abortaría), y se fija en
**1.84** y no en una más nueva porque a partir de 1.85 `bcprov` y `bcutil`
publican las mismas clases ASN.1. La 1.72 que arrastra PDFBox se descarta: es
antigua y acumula vulnerabilidades conocidas.

## Formato de las copias de seguridad

`.nexaPDF.bak` es **JSON en claro** con cabecera de formato, versión de esquema,
versión de aplicación y fecha.

Se eligió JSON y no ZIP ni SQLite porque lo único que NexaPDF guarda entre
sesiones son las preferencias y las firmas: unos pocos kilobytes. En claro tiene
dos ventajas concretas: el usuario puede abrirlo y ver que no hay nada raro
dentro, y una copia corrupta se puede reparar a mano.

Los documentos **no van dentro**: son ficheros que el usuario ya tiene, y
duplicarlos multiplicaría por mil el tamaño de la copia sin darle nada nuevo.

La lectura es estricta con la cabecera y tolerante con el contenido: si el
fichero no es una copia de NexaPDF hay que decirlo cuanto antes, pero una copia
de una versión anterior a la que le falten campos nuevos debe poder restaurarse.

## Pruebas

**32 unitarias** (`commonTest`, se ejecutan en la JVM de escritorio):
codificador QR contra referencias externas, códec de copias con todos sus modos
de fallo, reglas del aviso de donación caso por caso, y geometría.

**31 en el dispositivo** (`androidInstrumentedTest`): el motor de PDF completo
(abrir, renderizar, unir con y sin selección de páginas, extraer, separar por
rangos, reordenar y rotar, imágenes a PDF con distintas disposiciones, aplicar
todas las clases de edición, filtros, bloques de texto y firma con certificado
generado en la propia prueba) y el conversor de ofimática en los dos sentidos.

Corren en el teléfono y no en la JVM porque casi todo depende de APIs que solo
existen allí: `PdfRenderer`, `Bitmap`, el proveedor de seguridad del sistema. Una
prueba que se ejecutase en el escritorio no probaría el mismo código.
