# Registro de cambios

Todos los cambios reseñables de NexaPDF se documentan en este fichero.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el
proyecto se versiona con [SemVer](https://semver.org/lang/es/).

## [No publicado]

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
- **Firma electrónica** con certificado PKCS#12, incrustada mediante guardado
  incremental y verificable por cualquier lector, además de la firma manuscrita
  como sello visible.
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
