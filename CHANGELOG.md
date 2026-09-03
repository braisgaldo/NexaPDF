# Registro de cambios

Todos los cambios reseñables de NexaPDF se documentan en este fichero.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el
proyecto se versiona con [SemVer](https://semver.org/lang/es/).

## [No publicado]

### Añadido

- **Visor de PDF** como primera herramienta: lectura, búsqueda en todo el
  documento con las apariciones **resaltadas sobre la página**, contador y
  navegación entre ellas, índice de secciones si el PDF trae marcadores, lista
  de firmas digitales, y pellizco para ampliar.
- **Convertir** como herramienta propia, en los dos sentidos. Las **tablas** del
  PDF se detectan y salen como tablas reales en Word.
- **Inicio en rejilla** que cabe entero en la pantalla, con los ficheros
  recientes como una baldosa más.
- **Ficheros recientes** con pantalla propia: ordenar por fecha, nombre o
  tamaño, y verlos en lista, detalle o cuadrícula.
- **Compartir varios documentos** a la vez, empaquetados en un ZIP, y botón de
  compartir en el visor.
- **Unir en dos pasos**: primero los documentos, después las páginas. Empieza
  con la lista vacía y se van añadiendo de uno en uno o de varios en varios.
- **Separar en partes** declarando de una vez el rango y el nombre de cada una,
  propuestos como `documento_part-1`.
- **Editor**: mover, escalar y girar cualquier cosa añadida; texto visible
  mientras se coloca, con fondo de color opcional; **goma** que tapa con el
  color de la página; **recorte** de página; rueda de color; zoom con dos dedos.
- **Seis familias de tema** (doce con claro y oscuro): se añaden Grafito, Vino
  y Océano.
- **Ajuste de guardado**: paso a paso o sólo al final, con carpeta elegible.
- **Abrir desde otras aplicaciones**: un PDF compartido o abierto con NexaPDF se
  abre en el visor.

### Corregido

- La rejilla de páginas salía vacía al abrir un documento, sin error ni
  explicación.
- El selector de ficheros abría en «Reciente», que en muchos teléfonos está
  vacío y hacía parecer que no había ningún PDF.
- Tocar una línea de texto para sustituirla casi nunca acertaba: la zona
  sensible era la caja exacta de las letras, unos treinta píxeles.
- «Mover» no cogía trazos ni figuras, justo lo que se acababa de dibujar.
- Las fotos se elegían con el explorador de archivos en vez de con la galería, y
  sus miniaturas no llegaban a cargarse nunca.
- La firma con certificado salía en BER en lugar de DER, y los validadores
  estrictos la rechazaban sin llegar a comprobarla.
- Los `.docx` y `.xlsx` guardados como `octet-stream` no se podían elegir y el
  sistema ofrecía abrirlos con otra aplicación.


## [1.0.0] — 2026-09-03

Primera versión publicable.

### Añadido

- **Unir documentos**, con orden ajustable arrastrando o con botones y vista
  previa de la primera página de cada uno. Admite PDF, Word, Excel, PowerPoint e
  imágenes; lo que no es PDF se convierte antes, avisando de ello.
- **Separar**: un fichero por página, por rangos o extrayendo una selección.
- **Imágenes a PDF** desde la galería o desde la cámara, con 1, 2, 4 o 6
  imágenes por página, tamaño de página y márgenes configurables.
- **Editor de página** en tiempo real: dibujo a mano alzada, marcador
  fluorescente, formas (rectángulo, elipse, línea y flecha), cajas de texto,
  sustitución del texto existente, imágenes incrustadas, firma manuscrita y
  filtros de mejora (documento nítido, escala de grises, blanco y negro con
  umbral de Otsu, alto contraste, aclarar e invertir).
- **Firma electrónica** con certificado, incrustada mediante guardado
  incremental y verificable por cualquier lector, además de la firma manuscrita
  como sello visible. El certificado puede venir de un fichero `.p12` o `.pfx`,
  o del **almacén de claves del dispositivo**: en ese caso la clave privada no
  sale del sistema y no hace falta contraseña, que es el caso habitual de los
  certificados de la FNMT. El sobre PKCS#7 se codifica en DER, comprobado
  contra un validador externo.
- **Reordenación de páginas** arrastrando, con vista previa y confirmación
  explícita antes de aplicar.
- **Conversión** entre PDF y Word, Excel y PowerPoint en ambos sentidos.
- **Seis temas** (Índigo, Bosque y Ocaso, en claro y oscuro) más «seguir al
  sistema», con muestras de color reales en el selector.
- **Trece idiomas** (inglés, español, francés, alemán, chino simplificado,
  japonés, ruso, italiano, griego, árabe, gallego, catalán y euskera), cada uno
  con su bandera dibujada en Compose, y soporte RTL para el árabe.
- **Exportación e importación** de preferencias y firmas en ficheros
  `.nexaPDF.bak`, con copia de seguridad automática previa a cada importación.
- Vista previa del documento antes de firmarlo, y selector de ficheros que
  abre en la carpeta **Descargas**.
- **Ayuda** y **Acerca de** con versión, compilación, commit, licencias de
  terceros y enlace a la política de privacidad.
- Aviso de donación, mostrado una sola vez tras la primera sesión con uso real y
  nunca encima de una tarea a medias, con código QR generado sin conexión.

### Seguridad

- La aplicación **no declara ningún permiso**, empezando por el de internet.
- Los certificados de firma se leen, se usan y se descartan; nunca se almacenan.
- El espacio de trabajo se vacía en cada arranque.
- Las copias de seguridad de Android excluyen los documentos del usuario.
- BouncyCastle fijado en 1.84 en lugar del 1.72 que arrastra PDFBox, que acumula
  vulnerabilidades conocidas.

[No publicado]: https://github.com/braisgaldo/NexaPDF/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/braisgaldo/NexaPDF/releases/tag/v1.0.0
