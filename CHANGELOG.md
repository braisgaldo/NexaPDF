# Registro de cambios

Todos los cambios reseñables de NexaPDF se documentan en este fichero.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el
proyecto se versiona con [SemVer](https://semver.org/lang/es/).

## [1.2.0] — 2026-09-04

### Nuevo

- **Proteger PDF**: una baldosa más en el inicio para cerrar un documento con
  contraseña, cifrado con AES de 256 bits. La misma pantalla se la quita
  después, y detecta sola en qué estado está el documento que eliges.
  Los permisos del PDF (imprimir, copiar, anotar, modificar) se pueden ajustar,
  y la pantalla dice sin adornos lo que valen: son peticiones que el lector
  respeta si quiere, no un candado.
- El **recorrido guiado** tiene un paso más para la herramienta nueva.

### Corregido

- **La firma con certificado no era PAdES.** Le faltaba `signingCertificateV2`,
  el atributo firmado que lleva dentro el hash del certificado del firmante y
  que CAdES-BES exige, y salía con el subfiltro antiguo de Adobe
  (`adbe.pkcs7.detached`). La criptografía era correcta, pero un validador
  conforme a la norma europea —VALIDe, DSS— no la clasificaba como PAdES. Ahora
  es **PAdES-B-B** (`ETSI.CAdES.detached`), como la de AutoFirma. Se quita
  además el atributo `signingTime`, que en PAdES no debe estar porque la hora va
  en la entrada `/M` del diccionario de la firma.
- Un PDF cifrado abierto con **Leer PDF** se quedaba girando para siempre. El
  diálogo de la contraseña sólo existía dentro de la pantalla de páginas; ahora
  vale para cualquier herramienta.
- La copia interna de un documento descifrado se llamaba «algo.abierto.pdf», y
  ese «.abierto» acababa en la barra del visor y en el nombre de lo que se
  guardara después.
- El aviso de la donación se colaba encima del documento recién elegido: el rato
  de quietud se contaba también mientras el selector de ficheros del sistema
  tapaba la aplicación.
- En el editor, una **imagen insertada** se dibujaba como un rectángulo gris: se
  elegía la foto, se colocaba y no se sabía cuál era ni cómo quedaba hasta
  guardar y volver a abrir el documento.
- El aviso de «¿lo comparto?» decía «este documento» encima de una lista de
  tres, y con muchos ficheros ocupaba la pantalla entera: enseñaba los treinta
  primeros y se comía el resto sin decirlo. Ahora cuenta cuántos son y la lista
  se desplaza.
- **Ficheros recientes** ya tiene por dónde compartir varios a la vez. La
  pantalla existía y no había forma de llegar a ella.

### Rendimiento

- Una **imagen insertada** se guardaba entera y sin pérdidas. Una foto de móvil
  dejaba un PDF de 11 MB; reducida a los píxeles que caben en su hueco a 300 ppp
  y guardada en JPEG, el mismo documento pesa 97 kB.

## [1.1.0] — 2026-09-04

### Añadido

- **Visor de PDF** como primera herramienta: lectura, búsqueda en todo el
  documento con las apariciones **resaltadas sobre la página**, contador y
  navegación entre ellas, índice de secciones si el PDF trae marcadores, lista
  de firmas digitales, y pellizco para ampliar.
- **Convertir** como herramienta propia, en los dos sentidos. Las **tablas** del
  PDF se detectan y salen como tablas reales en Word.
- **Inicio en rejilla** que cabe entero en la pantalla, con los ficheros
  recientes como una baldosa más. «Imagen a PDF» e «Imágenes a PDF» pasan a
  ser una sola: el selector de fotos ya deja elegir una o varias.
- **Ficheros recientes** con pantalla propia: ordenar por fecha, nombre o
  tamaño, y verlos en lista, detalle o cuadrícula.
- **Compartir varios documentos** a la vez, empaquetados en un ZIP, y botón de
  compartir en el visor.
- **Unir en dos pasos**: primero los documentos, después las páginas. Empieza
  con la lista vacía y se van añadiendo de uno en uno o de varios en varios.
- **Separar en partes** con las páginas a la vista: cada parte tiene su color y
  se marca tocando la primera página y luego la última, o escribiendo el rango.
  Cada parte puede ser un tramo seguido o **páginas sueltas**. Antes de crear
  nada se enseña un **resumen** de lo que va a salir, que se puede desactivar
  en los ajustes. Los nombres se proponen como `documento_part-1`.
- **Editor**: mover, escalar y girar cualquier cosa añadida; texto visible
  mientras se coloca, con fondo de color opcional; **goma** que tapa con el
  color de la página; **recorte** de página; rueda de color; zoom con dos dedos.
- **Seis familias de tema** (doce con claro y oscuro): se añaden Grafito, Vino
  y Océano.
- **Ajuste de guardado**: paso a paso o sólo al final, con carpeta elegible.
- **Abrir desde otras aplicaciones**: un PDF compartido o abierto con NexaPDF se
  abre en el visor.
- **Cómo se lee**, en los ajustes: página a página deslizando de lado, o
  desplazamiento continuo con todas las páginas seguidas.
- **Barra de páginas** en el visor: flechas, número editable para saltar donde
  se quiera y una barra que recorre el documento entero.
- **Editar lo ya añadido** en el editor: corregir un texto puesto, y cambiar el
  color y el grosor de un trazo o una forma con sólo seleccionarlos.
- **Firmar en dos pasos**: primero la rúbrica a mano, si se quiere, y después
  el certificado. Al guardar la rúbrica se sigue solo con la firma digital, y
  el primer paso se puede quitar desde los ajustes.
- **Botón de acciones** en la pantalla de páginas: una acción a la vista con
  su icono y el resto en un desplegable, en lugar de una fila de tarjetas que
  ocupaba un cuarto de la pantalla.
- **Al terminar un documento**, en los ajustes: abrirlo, preguntar o no
  abrirlo, **con un valor por tarea**. Editar, unir, firmar, convertir a PDF e
  imágenes a PDF van cada una a su aire: ver lo que acabas de editar tiene
  sentido, que se abran veinte ficheros convertidos no.
- **Ajustes en secciones plegables**: la pantalla cabe entera sin desplazarse
  y cada opción es una línea con su valor a la derecha, en lugar de filas de
  botones con un párrafo debajo. Al pie, la versión y el lema, que es el dato
  que hace falta para informar de un fallo.

- **Ficheros recientes**: mantener pulsado un fichero deja renombrarlo o
  borrarlo, con confirmación. Antes la lista sólo crecía.
- **Recorrido guiado sobre la pantalla de verdad**: cada paso ilumina el
  elemento del que habla y oscurece el resto, en lugar de cuatro páginas de
  texto con un dibujo.
- **Progreso y cancelación** en las tareas que crean varios ficheros: se ve por
  dónde van y se pueden cortar.
- **Aviso antes de crear un fichero por página**, que sobre un documento largo
  son decenas de ficheros de golpe.

### Rendimiento

- **Caché de miniaturas** acotada por memoria y un cerrojo por documento en vez
  de uno para toda la aplicación: las páginas dejan de rasterizarse cada vez que
  vuelven a la vista y varias pueden prepararse a la vez.
- **Miniaturas en 16 bits de color**: la mitad de memoria y, sobre papel, sin
  diferencia visible. La página grande del visor y del editor sigue a 24 bits.
- **La búsqueda espera a que dejes de teclear** y se puede cortar por la mitad.
  Antes cada letra lanzaba un recorrido del documento entero que no se podía
  parar.
- **La goma escribe un trazo, no un rectángulo por punto.** Un borrado a mano
  alzada dejaba cientos de operaciones en el PDF.
- **Arranque en frío de 350 ms** en el build de publicación, medido sobre cinco
  intentos. No hace falta Baseline Profile.

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
- El tour de bienvenida volvía a salir en cada arranque aunque ya se hubiera
  visto o saltado.
- Los ficheros de «Dividir en partes» llevaban el rango de páginas pegado al
  nombre, no se copiaban a la carpeta de destino y no ofrecían compartirse.
- El aviso de donación aparecía al volver del selector de ficheros, encima del
  documento que se acababa de elegir.
- En el editor, la goma, el recorte y el borrado quedaban fuera de la pantalla:
  las once herramientas iban en una fila que había que desplazar, y la rueda de
  color tampoco se veía.
- El recorte de página no se dibujaba: sólo un rótulo decía que existía, sin
  poder comprobar qué parte se conservaba.
- La goma enseñaba la paleta de colores aunque tape siempre con el color de la
  página.
- Las asas de escalar y girar caían encima de las últimas letras del texto.
- Las páginas sólo se reordenaban manteniéndolas pulsadas: ahora tienen asa,
  como los documentos.
- Los nombres de los documentos a unir se partían a mitad de palabra en dos
  líneas.
- El resaltado de la búsqueda casi no se veía sobre papel blanco, y todas las
  apariciones se pintaban igual: el contador decía «3 de 8» y la vista no
  sabía cuál era la tercera.
- El menú de ordenar los recientes no marcaba cuál estaba puesto.
- El diálogo de añadir texto tenía el campo y el deslizador sin rótulo.
- **El documento firmado no llegaba a la carpeta de destino** ni ofrecía
  compartirse: la firma se saltaba el registro del resultado, que sí hacían
  unir, editar y extraer.
- Las marcas de la búsqueda caían desplazadas respecto a la palabra desde que el
  visor pasó a usar un pager: el margen se aplicaba dos veces.
- El botón de saltar la firma manuscrita quedaba debajo del lienzo de dibujo,
  que se traga el desplazamiento, así que no había forma de llegar a él.
- Las miniaturas se quedaban en blanco con una marca de error: `PdfRenderer`
  sólo pinta en `ARGB_8888` y se le estaba pidiendo `RGB_565`. Ahora se pinta
  como él quiere y la miniatura se guarda como copia de 16 bits.
- **Importar ajustes no funcionaba**: Android da a la extensión `.bak` un tipo
  propio que el selector no aceptaba, así que la copia salía atenuada y no
  había forma de elegirla. El filtro de tipos no aportaba seguridad, porque la
  cabecera y el esquema ya se comprueban al leer.


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
