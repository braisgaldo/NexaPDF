# Manual de NexaPDF

**Versión 1.0.0**

---

## Qué es NexaPDF

Una caja de herramientas para trabajar con documentos PDF desde el móvil: unir,
separar, convertir fotos en documentos, editar páginas y firmar.

Todo ocurre dentro de tu teléfono. La aplicación no pide permiso para conectarse
a internet, así que tus documentos no pueden salir de él aunque quisieras.

---

## La pantalla de inicio

Al abrir NexaPDF ves seis herramientas y, debajo, los ficheros que has ido
creando.

| Herramienta | Para qué sirve |
|---|---|
| **Unir PDF** | Juntar varios documentos en uno solo. |
| **Separar PDF** | Partir un documento o sacar algunas páginas. |
| **Imagen a PDF** | Convertir una foto en un documento. |
| **Imágenes a PDF** | Varias fotos, una por página o varias juntas. |
| **Editar PDF** | Dibujar, escribir y retocar las páginas. |
| **Firmar PDF** | Firmar a mano o con tu certificado. |

Arriba a la derecha, la rueda dentada abre los **Ajustes**.

---

## Unir documentos

1. Toca **Unir PDF**.
2. Elige los ficheros. Puedes seleccionar varios manteniendo pulsado el primero
   y tocando los demás.
3. Verás la lista con la primera página de cada uno.

Para **cambiar el orden**: mantén pulsado un documento y arrástralo, o usa las
flechas de subir y bajar. La papelera lo quita de la lista (no borra el fichero
de tu teléfono).

Con **Añadir PDF** puedes meter más documentos sin empezar de nuevo.

Cuando el orden esté como quieres, pulsa **Unir**.

> **También admite Word, Excel, PowerPoint e imágenes.** Si eliges alguno,
> NexaPDF lo convierte a PDF antes de unirlo y te avisa de ello.

---

## Separar un documento

Al abrir un solo documento verás sus páginas en miniatura.

**Para sacar algunas páginas:** tócalas para seleccionarlas (aparece una marca) y
pulsa **Extraer**. Se crea un documento nuevo solo con esas.

**Para partirlo entero:** pulsa **Un fichero por página**. Cada página se
convierte en un documento independiente.

**Para girar o borrar:** selecciona las páginas y usa **Girar a la izquierda**,
**Girar a la derecha** o **Eliminar**.

**Para reordenar:** mantén pulsada una página y arrástrala. Cuando el orden te
guste, pulsa **Aplicar el nuevo orden**. Hasta que no lo pulses, el documento no
cambia.

> El documento original de tu teléfono **nunca se modifica**. NexaPDF siempre
> crea un fichero nuevo.

---

## Convertir fotos en un documento

1. Toca **Imagen a PDF** o **Imágenes a PDF**.
2. Elige de dónde sacarla: **de la galería** o **haciendo una foto** en el
   momento.
3. Ajusta las opciones:
   - **Disposición**: 1, 2, 4 o 6 imágenes por página.
   - **Tamaño de página**: ajustar a la imagen (la página toma la forma de la
     foto), A4, Carta, A5 o A3.
   - **Orientación** y **margen**.
4. Pulsa **Crear PDF**.

Las fotos hechas en vertical salen derechas: NexaPDF lee la orientación que
graba la cámara y la aplica.

---

## Editar una página

Desde la vista de páginas, toca **Editar página** debajo de la que quieras.

Abajo tienes las herramientas:

| Herramienta | Qué hace |
|---|---|
| **Mover** | Solo mirar, sin dibujar. |
| **Dibujar** | Trazo a mano alzada con el dedo o el lápiz. |
| **Resaltar** | Como un marcador fluorescente: deja ver lo que hay debajo. |
| **Formas** | Rectángulo, elipse, línea y flecha, con o sin relleno. |
| **Texto** | Añadir una caja de texto o sustituir una línea existente. |
| **Imagen** | Incrustar una foto en la página. |
| **Firma** | Colocar tu firma manuscrita. |
| **Filtro** | Mejorar el aspecto de toda la página. |
| **Borrar** | Quitar lo que hayas añadido tocándolo. |

Debajo aparecen el **color** y el **grosor** de lo que estés usando.

Arriba tienes **deshacer**, **rehacer** y el botón de **guardar** (la marca de
verificación). Mientras no guardes, el documento no se toca.

### Cambiar el texto que ya hay

Elige la herramienta **Texto** y toca una línea del documento. NexaPDF tapa la
línea original y escribe la tuya en su lugar, como texto real que se puede
seleccionar y buscar.

No recoloca el resto del párrafo: un PDF guarda las letras en posiciones fijas,
no como un documento que fluya. Sirve para corregir una fecha, un nombre o un
importe, no para reescribir un capítulo.

### Los filtros

Útiles para dejar legible la foto de un papel:

- **Documento nítido**: blanquea el fondo y oscurece la tinta. El más usado.
- **Escala de grises** y **Blanco y negro**.
- **Alto contraste**, **Aclarar** e **Invertir**.

El deslizador ajusta la intensidad.

> **Un filtro convierte esa página en una imagen**, así que su texto deja de
> poder seleccionarse. La aplicación te lo advierte. Solo afecta a las páginas
> que marques.

---

## Firmar un documento

Hay dos formas y sirven para cosas distintas.

### Firma manuscrita

Dibuja tu firma con el dedo y colócala en la página. Es un dibujo sobre el
documento: vale para un albarán, un justificante o un formulario interno.

### Firma con certificado

Usa tu certificado electrónico (`.p12` o `.pfx`), el mismo que usarías en el
ordenador.

1. Pulsa **Elegir certificado** y busca tu fichero.
2. Escribe su **contraseña**.
3. Rellena, si quieres, el **motivo** y el **lugar**.
4. Pulsa **Firmar ahora**.

La firma queda incrustada en el documento y **cualquier lector de PDF puede
comprobar que no ha cambiado desde que lo firmaste**. Es la que sirve para lo
oficial.

> Tu certificado y su contraseña **no se guardan en ningún sitio**: se leen, se
> usan y se descartan. Y como la aplicación no tiene acceso a internet, tampoco
> pueden salir del teléfono.

Si el documento ya venía firmado, NexaPDF te lo dice arriba, con el nombre de
quien firmó.

---

## Convertir a Word, Excel o PowerPoint

Con un documento abierto, pulsa **Exportar como…** y elige el formato.

**Qué esperar de cada uno:**

- **Word (.docx)**: recupera el texto con sus líneas y saltos de página. Es la
  conversión más fiel.
- **Excel (.xlsx)**: agrupa el texto en filas y columnas según dónde estuviera
  colocado. Funciona bien con tablas alineadas.
- **PowerPoint (.pptx)**: una diapositiva por página, con la página como imagen.
  Es la que conserva el aspecto exacto, pero el texto deja de ser editable.

> Un PDF guarda letras colocadas sobre una página, no párrafos ni celdas. Lo que
> sale es el contenido recompuesto lo mejor posible, no el documento original de
> Office.

---

## Dónde se guardan los ficheros

En la carpeta **Descargas/NexaPDF** de tu teléfono. Cualquier gestor de archivos
o aplicación puede abrirlos.

También aparecen en **Ficheros recientes**, en la pantalla de inicio.

Desde el documento puedes:
- **Compartir** (el icono de arriba a la derecha).
- **Guardar como…** para elegir tú la carpeta.

---

## Ajustes

### Tema

Siete opciones: **seguir al sistema** más seis temas concretos, tres claros y
tres oscuros (Índigo, Bosque y Ocaso). Cada uno se muestra con sus colores
reales, así que se ve lo que se elige antes de aplicarlo.

### Idioma

Trece idiomas, cada uno escrito en su propia lengua y con su bandera. La opción
**Idioma del sistema** usa el que tenga configurado el teléfono.

El árabe cambia la interfaz a **de derecha a izquierda**.

### Calidad de la vista previa

Rápida, equilibrada o nítida. Solo afecta a lo que ves en pantalla: **los
ficheros guardados conservan siempre toda su calidad**. Si tu teléfono va justo
de memoria, ponla en rápida.

### Tus datos

- **Guardar también en Descargas**: activado, cada documento que crees se copia
  a `Descargas/NexaPDF` automáticamente.
- **Preguntar antes de borrar**: pide confirmación antes de eliminar páginas.
- **Nombre para las firmas**: el que aparecerá por defecto al firmar.
- **Exportar ajustes**: guarda un fichero `.nexaPDF.bak` con tus preferencias y
  tus firmas guardadas. **Tus documentos no van dentro**: ya están en tu
  teléfono.
- **Importar ajustes**: restaura ese fichero. Antes de hacerlo, NexaPDF guarda
  automáticamente una copia de lo que tenías, por si te arrepientes.

---

## Preguntas frecuentes

**¿Funciona sin internet?**
Sí, siempre. Es lo único que sabe hacer: no tiene permiso para conectarse.

**¿Se sube algo a la nube?**
No. Técnicamente no puede.

**¿Modifica mis documentos originales?**
Nunca. Trabaja sobre una copia y crea siempre un fichero nuevo.

**¿Puedo abrir un PDF con contraseña?**
Sí, te la pedirá al abrirlo.

**Se queda sin memoria con un documento muy grande.**
Sepáralo en partes con **Separar** y trabaja con ellas de una en una.

**Un documento no se abre.**
Puede estar dañado o protegido. Prueba a abrirlo antes con otro lector para
descartarlo.

**¿La app es gratis de verdad? ¿Hay versión de pago?**
Es gratis y no hay ninguna otra versión. Todas las funciones están disponibles
para todo el mundo. En Ajustes hay una forma voluntaria de dar las gracias, y no
cambia nada de lo que la aplicación hace.

---

## Si algo falla

Escribe a **ghatostudio@proton.me** contando qué estabas haciendo, con qué
versión de NexaPDF (está en «Acerca de») y qué versión de Android.

El código fuente está en <https://github.com/braisgaldo/NexaPDF>, por si quieres
mirarlo o informar de un fallo allí.
