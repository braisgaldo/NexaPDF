# Plan de mejoras antes de publicar

Estado: **ejecutado**, con los resultados medidos anotados en cada punto.
Fecha: 4 de septiembre de 2026.

Lo que sigue sale de recorrer la aplicación entera en un Samsung S22 Ultra
(Android 13, `SM-S908U`) pantalla a pantalla, y de medir lo que se podía medir
en vez de opinar. Cada punto lleva el número que lo justifica, la causa, qué se
propone y **cómo comprobar en el teléfono que ha servido de algo**. Un plan sin
esa última parte es una lista de deseos.

Al final hay un orden propuesto: qué entra antes de subir a Google Play y qué
puede esperar a la siguiente versión.

---

## Cómo se ha medido

Build de depuración instalado con `installDebug`, que en aspecto y en trabajo de
CPU es el mismo que el de publicación salvo por R8. Documento de prueba de
**120 páginas** generado con `build/pdf_tabla.py` y el guion de `tools/`.

```
adb shell am start-activity -W -n es.ghatostudio.nexapdf.debug/…MainActivity
adb shell dumpsys gfxinfo es.ghatostudio.nexapdf.debug reset
adb shell dumpsys gfxinfo es.ghatostudio.nexapdf.debug
adb shell dumpsys meminfo es.ghatostudio.nexapdf.debug
```

| Medida | Valor actual |
|---|---|
| Arranque en frío (3 intentos) | 1311 / 1343 / 1463 ms |
| Fotogramas con retraso al desplazar 120 páginas | **16,67 %** (150 fotogramas) |
| Percentil 90 / 99 de fotograma | 42 ms / 89 ms |
| Percentil 90 de GPU | 3 ms |
| PSS total con la rejilla de 120 páginas abierta | **215 MB** (69 MB de gráficos) |
| Búsqueda de una palabra en 120 páginas | ≈ 2 s netos |
| APK de publicación / AAB | 6,3 MB / 10,3 MB |

El dato que más dice es el contraste entre el percentil 90 de fotograma (42 ms)
y el de GPU (3 ms): **no es un problema de pintado, es de trabajo en la CPU**.

---

## Rendimiento

### R1 · Las miniaturas se vuelven a rasterizar cada vez que aparecen

`MiniaturaPagina` pide la página a `MotorPdf.renderizarPagina` en un
`LaunchedEffect` con clave `(ruta, indice, anchoPx)`. No hay caché: al
desplazar la rejilla, cada página que sale y vuelve a entrar se rasteriza otra
vez con `PdfRenderer`. Y todas las llamadas pasan por **un único mutex** en
`MotorPdfAndroid` (`cerrojo.withLock`), así que además se ponen en fila.

De ahí el 16,67 % de fotogramas con retraso con la GPU ociosa.

**Propuesta**

- Caché LRU de `ImageBitmap` con clave `ruta + índice + ancho`, acotada por
  bytes (≈ 1/8 del heap disponible, `Runtime.maxMemory()`), no por número de
  entradas: una miniatura de 320 px pesa unos 578 KB y una de 1080 px seis
  veces más.
- Invalidar la caché de un documento cuando se guarda encima de él.
- Cambiar el mutex global por uno **por documento**: el cuello de botella real
  es `PdfRenderer`, que no es seguro entre hilos por documento, no entre
  documentos distintos.

**Cómo comprobarlo:** repetir los seis desplazamientos sobre las 120 páginas.
Objetivo: **< 5 % de fotogramas con retraso y percentil 99 por debajo de 40 ms**.

**Resultado medido.** El objetivo **no se cumple**, y conviene decir por qué en
lugar de dejarlo en «mejorado»:

| | Antes | Después |
|---|---|---|
| Primera pasada, páginas nuevas | 16,67 % · p90 42 ms | 14,17 % · p90 32 ms |
| Volviendo sobre páginas ya vistas | — | **9,66 % · p90 22 ms** |

La caché sólo puede ahorrar trabajo que ya se hizo, así que en la primera pasada
apenas cambia nada: esas páginas hay que rasterizarlas sí o sí. Y no se pueden
rasterizar en paralelo, porque `PdfRenderer` no admite dos páginas abiertas a la
vez **del mismo documento**; el cerrojo por documento ayuda cuando hay varios
abiertos, no dentro de uno. Bajar del 5 % en la primera pasada pide otra cosa:
adelantarse y preparar la pantalla siguiente mientras no se desplaza.

### R2 · 215 MB de memoria con un documento largo abierto

69 MB son bitmaps. Todas las miniaturas se piden en `ARGB_8888` (4 bytes por
píxel) aunque un PDF de texto no tenga transparencia.

**Propuesta**

- `RGB_565` para las miniaturas de rejilla y de recientes: la mitad de memoria
  y, sobre papel, sin diferencia perceptible. La página grande del visor y del
  editor se queda en `ARGB_8888`, que ahí sí se amplía.
- La caché de R1 pone un techo a lo que se retiene; hoy no hay ninguno.

**Cómo comprobarlo:** `dumpsys meminfo` con las 120 páginas abiertas.
Objetivo: **por debajo de 150 MB de PSS**.

**Resultado medido: el objetivo no se cumple y la premisa era falsa.**
`PdfRenderer.render` sólo acepta `ARGB_8888`; pedirle `RGB_565` responde
«Unsupported pixel format». Se pinta como él quiere y la miniatura se queda con
una copia de 16 bits, que pesa la mitad, pero la caché retiene más páginas de
las que Compose retenía sola. Con el límite en una octava parte del montón la
memoria subía de 215 a 232 MB; con una dieciseisava se queda en **220 MB**, con
73 MB de gráficos frente a los 69 de partida.

O sea: se cambian **5 MB de memoria por casi la mitad de tirones al volver sobre
lo ya visto**. Es un intercambio razonable, pero es un intercambio, no la mejora
en los dos frentes que este punto daba por hecha.

### R3 · La búsqueda se lanza en cada tecla y no se puede cancelar

`LaunchedEffect(consulta)` dispara una pasada de `PDFTextStripper` por el
documento entero con cada pulsación. Escribir «Pagina» son seis pasadas. Al
cambiar la clave, Compose cancela la corrutina, pero `PDFTextStripper` no
comprueba la cancelación: la pasada en curso termina igual y ocupa el hilo.

Con 120 páginas se nota (≈ 2 s); con un contrato escaneado de 500 la aplicación
parece colgada.

**Propuesta**

- Esperar **350 ms** desde la última tecla antes de buscar.
- Recorrer el documento **página a página** en vez de de una vez, comprobando
  `ensureActive()` entre páginas y emitiendo las apariciones según se
  encuentran. Así la búsqueda se puede cortar de verdad y las primeras
  apariciones aparecen antes de terminar el documento.

**Cómo comprobarlo:** escribir «Pagina» del tirón en el documento de 120
páginas. Objetivo: **la primera aparición marcada en menos de 500 ms** y ningún
fotograma perdido mientras se teclea.

### R4 · Arranque en frío de 1,3 s

Es aceptable, pero está medido sobre el build de depuración. Antes de publicar
hay que medirlo sobre el de publicación; si sigue por encima de 1 s, un
**Baseline Profile** es lo que más rebaja el primer arranque en Compose.

**Cómo comprobarlo:** `am start-activity -W` sobre el APK de publicación, cinco
intentos, quedándose con la mediana.

### R5 · La goma escribe un rectángulo por punto

Al guardar, cada punto muestreado del trazo de goma se convierte en su propio
`q … re f Q`. Un trazo corto de prueba dejó **40 rectángulos** en el flujo de
la página. Un borrado a mano alzada sobre media hoja deja cientos: infla el PDF
y ralentiza tanto el guardado como los visores de terceros.

**Propuesta:** unir los puntos en un solo trazo grueso (`m`/`l` con el ancho de
la goma) en lugar de un rectángulo por punto.

**Cómo comprobarlo:** borrar media página, guardar y contar operaciones en el
flujo descomprimido. Objetivo: **una por trazo**, no una por punto.

---

## Visualización

### V1 · Las baldosas de inicio tienen mucho hueco muerto

Ocupan la pantalla entera, que era lo pedido, pero el icono y el rótulo van
centrados en una tarjeta muy alta y dejan un tercio de aire arriba y otro
abajo. Se ve vacío, no espacioso.

**Propuesta:** escalar el icono con la altura de la baldosa (del orden de un
40 % de su alto, con un máximo) y pegar el rótulo debajo, dejando el aire en los
márgenes y no en el centro.

### V2 · El menú de ordenar los recientes no dice cuál está activo

«Más recientes», «Nombre» y «Tamaño» salen los tres iguales. Añadir la marca de
verificación en el que está puesto, como ya hace el desplegable de temas.

### V3 · El resaltado de la búsqueda casi no se ve

Sobre papel blanco el resaltado actual es un lavanda muy claro. Además todas las
apariciones se pintan igual, así que el contador dice «3/8» pero la vista no
distingue cuál es la tercera.

**Propuesta:** subir la opacidad del resaltado y pintar **la aparición activa
con otro color y un borde**. Verificar el contraste igual que se hizo con las
paletas.

### V4 · La barra de acciones tapa las páginas

En la pantalla de páginas, los seis botones («Un fichero por página», «Dividir
en partes…», «Firmar», «Guardar como…», «Exportar como…») ocupan alrededor del
28 % del alto antes de que se vea la primera miniatura.

**Propuesta:** dejar arriba las dos acciones principales y recoger el resto en
un menú de desbordamiento, o convertir la fila en una barra inferior que se
esconda al desplazar.

### V5 · El diálogo de añadir texto tiene controles sin rótulo

El campo de texto no tiene marcador de posición y el deslizador de tamaño no
lleva etiqueta: es un deslizador suelto entre el campo y el interruptor de
fondo. Poner «Tamaño», igual que «Grosor» en la barra de dibujo.

### V6 · El visor sólo pasa página con las flechas

No hay gesto de deslizar entre páginas ni desplazamiento continuo. En un
documento largo, leer obliga a apuntar al botón cada vez.

**Propuesta:** `HorizontalPager` para el paso de página, manteniendo las flechas
y el «N de M» para quien navega con TalkBack.

---

## Funcionalidad

### F1 · Los ficheros recientes no se pueden borrar ni renombrar

La lista crece indefinidamente y desde la aplicación no hay forma de limpiarla
ni de corregir un nombre. Es el hueco más visible de la aplicación ahora mismo:
la carpeta se llena de «documento editado editado.pdf».

**Propuesta:** pulsación larga sobre un reciente para **renombrar**, **borrar**
o **quitar de la lista** (sin borrar el fichero), con confirmación cuando toca
borrar de verdad.

### F2 · Las operaciones largas no dicen por dónde van

`VeloDeTrabajo` es un velo con un texto fijo. En un documento de 120 páginas,
«Un fichero por página» tarda lo suyo sin dar ninguna señal de avance, y no se
puede cancelar.

**Propuesta:** progreso «página N de M» donde el motor lo sepa (separar, unir,
imágenes a PDF, convertir) y un botón de cancelar en las que se puedan
interrumpir entre páginas.

### F3 · «Un fichero por página» no pregunta

Sobre 120 páginas crea 120 ficheros sin confirmación. Debería avisar de cuántos
va a crear antes de hacerlo.

### F4 · Revalidar la firma con el certificado real antes de publicar

La firma con `.p12` y con el almacén del sistema está probada, y el sobre PKCS#7
se verificó en su día con `openssl cms -verify` contra el certificado de la
FNMT. Después de eso se ha tocado el editor y el guardado. Antes de subir hay
que **repetir esa verificación externa**, no sólo comprobar que la aplicación no
falla.

### V7 · El tour no enseña la aplicación que hay

El tour son cuatro páginas de texto con un dibujo. Cuenta lo que la aplicación
hace, pero no enseña **dónde está**, así que al terminarlo sigues sin saber que
el número de página del visor se puede escribir o que las acciones de la
pantalla de páginas están detrás de una flecha.

**Propuesta:** convertirlo en un recorrido sobre la pantalla de verdad. Cada
paso oscurece la pantalla entera menos el elemento del que habla, que se queda
iluminado con un recorte, y al lado sale el rótulo con lo que hace. Es el patrón
que usa cualquier aplicación para enseñar su propia interfaz, y funciona porque
lo que se explica y lo que se señala son la misma cosa.

Pasos, en el orden en que se usan: las baldosas de inicio, el botón de acciones
de la pantalla de páginas, la barra de páginas del visor, la barra de
herramientas del editor y el botón de ajustes.

**Cómo comprobarlo:** hacer el tour entero en el teléfono y comprobar que en
cada paso lo iluminado es el elemento del que habla el rótulo, y que al terminar
se llega a la pantalla de inicio sin que vuelva a salir.

### F5 · Las capturas de la ficha de Google Play están desactualizadas

`docs/google_play/capturas` sigue enseñando la interfaz de lista anterior, sin
el inicio en rejilla, el visor ni la pantalla nueva de dividir en partes.
Regenerarlas con `tools/capturar_tienda.ps1`, que además hay que actualizar: sus
coordenadas fijas ya no corresponden con el menú actual.

---

## Orden propuesto

**Antes de subir a Google Play** — lo que un usuario nota el primer día o lo que
compromete lo que la ficha promete:

1. R1 + R2 · caché de miniaturas y `RGB_565`. Es el arreglo con más efecto por
   línea escrita: quita el tirón al desplazar y baja la memoria a la mitad.
2. R3 · antirrebote y búsqueda cancelable.
3. V3 · resaltado visible y aparición activa distinguible.
4. F1 · borrar y renombrar recientes.
5. F3 · confirmar antes de crear muchos ficheros.
6. V2, V5 · ajustes de composición que quedan.
7. R5 · goma como trazo único.
8. F2 · progreso y cancelación.
9. R4 · medir el arranque del build de publicación y, si lo pide, Baseline
   Profile.
10. F4 · revalidar la firma con el certificado real.
11. V7 · tour sobre la pantalla de verdad, con lo que se explica iluminado.
12. F5 · capturas al día. **Va la última a propósito**: cualquier cosa de esta
    lista cambia lo que se ve, y unas capturas hechas antes habría que rehacerlas.

**Ya hechos** en el repaso anterior, se dejan anotados para no perderlos de
vista al comprobar:

- V1 · las baldosas de inicio llenan la pantalla y el icono crece con ellas.
- V4 · la barra de acciones es un botón con desplegable.
- V6 · el visor pasa página deslizando, con número editable y barra.

Cada punto se da por bueno con su comprobación en el teléfono, no con que
compile.
