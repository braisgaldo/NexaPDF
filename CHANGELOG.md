# Registro de cambios

Todos los cambios reseñables de NexaPDF se documentan en este fichero.

El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el
proyecto se versiona con [SemVer](https://semver.org/lang/es/).

## [1.5.0] — 2026-09-15

### Nuevo

- **Escáner de documentos.** Fotografía un papel y sale un PDF derecho y
  legible, no una foto torcida metida dentro de un PDF. Es la novena
  herramienta y va a lo ancho, encima de la rejilla del inicio: es la única
  que empieza sin fichero, porque las otras ocho piden un documento que ya
  tienes y esta sirve para cuando lo que tienes es un papel encima de la mesa.

  - **Detección de bordes en vivo, con porcentaje.** Mientras apuntas se dibuja
    el contorno de la hoja y se enseña qué parte de ese contorno se distingue
    de verdad. El número no es un adorno: es exactamente el mismo que decide si
    la captura automática dispara, así que lo que se ve es lo que la aplicación
    usa. Ámbar mientras no se fía, verde cuando además lleva varios fotogramas
    quieta.
  - **Captura automática.** Dispara sola cuando el encuadre es bueno **y** el
    contorno está parado. Exigir las dos cosas es lo que evita la foto movida:
    un encuadre puede estar al noventa por ciento y seguir bailando porque la
    mano aún no se ha detenido. Después de disparar **espera a que cambies de
    hoja**: si no, un segundo más tarde el mismo papel sigue encuadrado y
    quieto, y salían dos fotos de la misma página. Mientras espera lo dice, en
    lugar de dejar creer que ha dejado de funcionar.
  - **Auto y Manual, escrito con palabras** en la barra del escáner, no sólo con
    un icono. El modo de arranque se elige en *Ajustes › Escáner*.
  - **Salir del escáner con páginas sin convertir pregunta antes.** Una pulsación
    de atrás sin querer no debería costar diez hojas ya escaneadas.
  - **Corrección de perspectiva.** Los cuatro vértices se llevan a un
    rectángulo, así que un folio fotografiado de lado sale recto.
  - **Revisión antes de crear.** Cada hoja se ve como va a quedar, con las
    cuatro esquinas arrastrables, giro, filtro de mejora y su intensidad. Si el
    borde no se detectó con seguridad, esa página lleva un aviso.
  - **Texto buscable.** Reconoce las palabras y las incrusta en el PDF de forma
    invisible, encima de la imagen, en el sitio exacto donde están. El
    documento se ve igual que la foto pero se puede buscar, seleccionar y
    copiar en cualquier lector. Alfabeto latino.
  - **Conservar los colores originales**, en *Ajustes › Escáner*. La mejora de
    página hace dos cosas a la vez: quita las sombras y da nitidez, que es lo que
    se le pide, y de paso pasa la hoja a gris, que no. Para un folio impreso da
    igual; para una factura con un sello rojo, un apunte a bolígrafo azul o un
    gráfico de colores, es contenido que desaparece. Con esto activado la página
    pasa por la misma mejora —y el reconocimiento de texto sigue leyendo igual— y
    se guarda en color. Pesa más, y por eso no viene puesto.

  - **Varias fotos por página.** Cada disparo hace tres fotos seguidas y se
    funden en una. El límite de nitidez de un escaneo no es el filtro, es el
    grano: mejorar la página amplifica el ruido a la vez que el detalle, así que
    hay un punto en el que el papel se ensucia más rápido de lo que las letras
    ganan. Con tres fotos el grano se cancela solo —es distinto en cada disparo
    y el papel es el mismo— y ese punto se aleja.

    Entre foto y foto la mano se mueve, así que antes de promediar se mide
    cuánto, con precisión de menos de un píxel, y se remuestrea en su sitio. Y
    para lo que la medida no alcanza —un giro de muñeca, una sombra que se
    mueve, un dedo que entra en el encuadre— cada píxel se compara con el de la
    foto de referencia y sólo se promedia si se parece. **El peor caso de esto
    no es una página borrosa, sino una que simplemente no ha ganado nada.**

    Cuesta unos segundos más por hoja, así que se puede apagar en
    *Ajustes › Escáner › Varias fotos por página*.

  - **Modo tarjeta.** Para DNI, permiso de conducir, tarjeta de crédito, abono
    de transporte o pasaporte. Lo que lo define no es el recorte —de eso ya se
    encarga el detector, que no distingue una tarjeta de un folio— sino el
    **tamaño físico**: las caras se montan sobre una hoja normal a su medida de
    verdad, las de la norma ISO/IEC 7810, así que al imprimir sale del tamaño
    del carné y sirve para entregarlo en una ventanilla. Escalado a A4 se ve
    igual de bien y no sirve para eso.

    El anverso y el reverso caben en el mismo folio, centrados y con aire para
    poder recortarlos; cuando no caben más, se abre otra hoja. Hay *Automático*,
    que deduce el formato por la forma de lo capturado, y las opciones
    explícitas. Una advertencia honesta sobre *Automático*: ID-2 e ID-3 tienen
    prácticamente la misma proporción (1,419 y 1,421), así que **por la forma no
    se pueden distinguir**; acierta al separar una tarjeta de un pasaporte, que
    es la diferencia que se nota al imprimir, y entre los dos grandes elige
    pasaporte. Para lo demás están las opciones explícitas.

  - **Nombre elegido antes de escribir el fichero**, no después: renombrar un
    documento ya guardado deja una copia con el nombre viejo en la carpeta de
    descargas.
  - Al terminar se abre o no según *Ajustes › Al terminar un documento ›
    Escanear*, como el resto de tareas.

- **Al terminar un escaneo se abre la vista de lectura**, y no la del documento
  como el resto de tareas. Lo que se acaba de crear es un PDF **buscable**, y lo
  primero que uno quiere es leerlo y buscar dentro para comprobar que el
  reconocimiento pilló lo que tenía que pillar.

- **Buscar acerca y centra la coincidencia.** Antes saltaba a la página y la
  dejaba entera, con la palabra resaltada midiendo dos milímetros en un A4 visto
  en un móvil: eso resalta, pero no enseña. El acercamiento sale de lo que ocupa
  la palabra y se queda entre 1,6× y 4×, para que una coincidencia de dos letras
  no llene la pantalla de un trozo de letra sin contexto.

  Costó tres arreglos, y los tres se vieron usando la aplicación, no leyendo el
  código:

  - El centrado tomaba las coordenadas de la palabra como si fueran de la
    pantalla, y **la página no llena la pantalla**: se dibuja encajada, con
    margen a dos lados. Medido sobre 1080×2000 con la palabra al 35 % de la
    hoja, el desvío iba de 0 a 876 px según la forma de la página. De ahí que
    fallara «a veces».
  - El tope de desplazamiento estaba medido contra la pantalla, así que una
    palabra del margen no llegaba al centro y se quedaba a un tercio de pantalla.
    Importa más de lo que parece: los márgenes son donde empieza y acaba **cada
    línea**. Ahora el enfoque usa su propio tope, medido contra el papel.
  - Y el último: una búsqueda recién hecha saltaba a la página pero nunca marcaba
    **qué** coincidencia enfocar. Resaltaba bien y no se acercaba, mientras que
    moverse con las flechas sí funcionaba.

  Sólo en lectura lateral. En la vertical el encuadre no es una transformación
  sobre la página sino el ancho de la columna, así que acercarse ahí ensancharía
  el documento sin llevar a ninguna parte.

- **Eliminar la página que se está leyendo**, desde la caja de herramientas del
  visor. Antes sólo se podía desde la rejilla de páginas, seleccionando; pero el
  momento en que uno descubre que una página sobra —la hoja en blanco del final,
  la que salió movida al escanear— es leyéndola, y había que salir, entrar en la
  rejilla y buscarla. Como el resto de la aplicación, escribe un documento nuevo
  y no toca el original.
- **La rejilla de páginas dice cómo se seleccionan.** La ayuda hablaba sólo de
  reordenar arrastrando, así que girar y eliminar estaban ahí sin que nada lo
  indicara.
- **Ajustes del escáner**: texto buscable por defecto, captura automática y
  filtro de mejora por defecto.
- **Un paso más en el recorrido guiado** para la herramienta nueva.

### Cambiado

- **La aplicación declara un permiso.** Hasta la 1.3.0 no declaraba ninguno, y
  eso era parte de la ficha, del `README` y de la política de privacidad. El
  escáner necesita ver los fotogramas *antes* de la foto —es lo único que
  permite dibujar el contorno mientras apuntas—, y la cámara del sistema
  devuelve la foto ya hecha. Se declara `CAMERA`, se pide al entrar en el
  escáner y nunca al arrancar, y si se deniega el escáner sigue funcionando con
  fotos de la galería.

  **Lo que no cambia es lo que importa:** la aplicación sigue sin poder salir a
  internet. CameraX y el reconocedor de texto declaran `INTERNET` en sus
  manifiestos para telemetría que aquí no se usa, y NexaPDF lo **elimina** al
  fusionar. El modelo de reconocimiento viaja dentro del APK, así que el
  escáner funciona igual en modo avión. El razonamiento completo está en
  `docs/adr/0005-camara-escaner.md`.

- **La comprobación de permisos de la CI pasa de «que no haya» a lista
  blanca**, y se añade otra más fuerte que la anterior: el manifiesto
  **fusionado** —no el nuestro, que es donde nunca estaría el problema— no
  puede contener `INTERNET` ni `ACCESS_NETWORK_STATE`.

### Notas técnicas

- La detección de bordes es **Kotlin puro en `commonMain`**, sin OpenCV:
  reducción a 240 px, Sobel con umbral sacado del histograma, transformada de
  Hough y validación del cuadrilátero midiendo qué parte de su contorno cae de
  verdad sobre píxeles de borde. Cuesta un par de milisegundos por fotograma en
  una JVM de escritorio. Meter OpenCV habría sumado unos veinte megabytes de
  código nativo a una aplicación que entera pesa seis, y habría dejado la
  detección fuera del alcance de las pruebas unitarias.
- **Doce pruebas nuevas** sobre el detector, y no sólo de acierto: cuatro miden
  el **temblor entre fotogramas** de una escena quieta, que es lo que de verdad
  se ve en pantalla. Un fotograma bien detectado y el siguiente detectado tres
  píxeles más allá se lee como que la aplicación no sabe lo que hace.
- Las familias de bordes se agrupan **respecto a la orientación del propio
  papel** y no por «casi vertical» y «casi horizontal». Con el corte fijo a 45°,
  un folio girado unos treinta grados metía sus cuatro bordes del mismo lado, se
  quedaba sin ninguna pareja que cruzar y no se detectaba nada.
- Los picos de la transformada de Hough se **afinan entre celdas** ajustando una
  parábola a cada pico y sus vecinos. Sin eso, un borde que no se mueve salta de
  celda en celda con el ruido del sensor.
- **El reconocimiento lee la página sin el filtro de mejora**, aunque el PDF
  lleve la versión filtrada. El filtro está para que la página se vea limpia, y
  para eso lleva el papel a blanco puro y la tinta a negro; eso adelgaza los
  trazos más finos y el reconocedor empieza a confundir letras. Medido sobre la
  misma foto: con la página filtrada leía «lglesias», y sin filtrar lee
  «Iglesias». Cuesta un enderezado de más por página y las coordenadas siguen
  valiendo, porque las dos versiones salen del mismo recorte.
- `CargadorImagen` se saca a un objeto propio: lo usan el motor de PDF y el del
  escáner, y duplicarlo era tener dos sitios donde arreglar el mismo fallo de
  orientación EXIF.
- **Cuatro decisiones de resolución que estaban mal y se notaban en la página.**
  Salieron de probar con documentos de verdad, no de leer el código:

  - `inSampleSize` sólo admite potencias de dos, así que pidiendo 2600 px una
    foto de 4000 se cargaba a **2000**: la mitad del lado y la cuarta parte de
    los píxeles, tirados justo antes de enderezar y de leer el texto. Ahora el
    ajuste fino lo hace el propio decodificador.
  - La captura pedía **mínima latencia** y no pedía resolución. El argumento era
    que el filtro de mejora se comería el ruido igualmente, y era falso: el
    reconocimiento lee la página **sin** filtrar. Ahora es máxima calidad y
    máxima resolución.
  - **No se enfocaba antes de disparar.** El enfoque continuo persigue toda la
    escena y con un papel cerca se queda en la mesa. Ahora enfoca al centro
    antes de la foto, con un tope de 1,2 s para no dejar esperando.
  - El tamaño de la página enderezada salía de la **caja envolvente** del
    cuadrilátero. Con una hoja girada eso es la diagonal, no el lado, y la
    imagen se ampliaba por encima de los píxeles que había. Ahora sale de los
    lados reales del papel, y nunca se amplía.

  El reconocimiento de texto, además, leía a 1600 px: en un A4 son unos 135
  puntos por pulgada, y a esa resolución la letra de cuerpo 10 tiene trece
  píxeles de alto. Ahora lee a la misma resolución a la que se revela la página,
  unos 300 ppp, que es la horquilla en la que trabajan los escáneres de
  sobremesa.

- **La página se revela a 300 ppp, y antes se quedaba en 250.** El tope estaba
  en 2900 px con el argumento de que por encima el fichero crece sin que el ojo
  lo note. Medido sobre una foto real, el argumento era falso: una hoja
  fotografiada con doce megapíxeles ocupa unos 3770 px dentro del encuadre, así
  que el tope no estaba comprimiendo nada, estaba **tirando** un 23 % de
  resolución lineal que ya venía capturada. Comparadas al mismo tamaño en
  pantalla, la letra de cuerpo pasa de 20 a 26 px de alto.

  Se para en 300 ppp y no más arriba porque ahí se acaba el detalle que hay en
  la foto: medido sobre la misma imagen, el borde de una letra ocupa dos
  píxeles y medio **en el original**, así que pedir más resolución sólo
  agrandaría el borrón. Por el mismo motivo no se tocó la máscara de enfoque:
  probadas siete combinaciones de radio e intensidad sobre el mismo recorte, la
  mejor estrechaba el borde un 18 % y ninguna cambiaba nada que se viera. El
  problema no era el filtro, era la resolución a la que se le daba la página.

- **El apilado de ráfaga es Kotlin puro y se prueba sin teléfono.** El alineado
  busca en dos pasos, de grueso a fino: primero sobre una versión reducida a la
  cuarta parte, que abarca mucho por poco dinero, y después se afina a escala
  real ajustando una parábola al error para bajar del píxel. Sin ese último
  paso, medio píxel de error al promediar tres fotos ya se ve como una página
  algo más blanda que la original: se habría quitado ruido a cambio de nitidez,
  que es un mal negocio.

  Para decidir si una foto de la ráfaga sirve se compara el error en su mejor
  posición con el error a veinte píxeles de ahí. **Es una proporción y no un
  valor absoluto**, y esa parte importa: un corte absoluto no vale porque el
  error depende muchísimo de lo que haya en la página, y una hoja con mucho
  texto se sale de cualquier número que sirva para una hoja más vacía. Se probó
  con un corte absoluto primero y rechazaba fotos buenas.

  Las pruebas de esto miran **dos cosas a la vez, ruido y filo**, y es a
  propósito: promediar siempre baja el ruido, así que una prueba que sólo mire
  el ruido pasa incluso con el alineado roto, y con el alineado roto la página
  sale borrosa.

- **La página no se enfoca: se le deshace el desenfoque.** Una máscara de
  desenfoque —lo que hacía antes, y lo que hace casi todo el mundo— no deshace
  nada: exagera el contraste a los lados de cada borde para que el ojo lo lea
  como más definido. Tiene techo, y el techo se ve: los trazos salen
  **moteados**, con grises sucios por dentro que la máscara no puede arreglar
  porque ahí no hay borde que exagerar.

  Ahora se parte de un modelo de **cómo** se emborronó la foto y se busca la
  imagen que, al emborronarse así, daría la que tenemos. Es Richardson-Lucy, el
  método que se usa en astronomía por el mismo motivo: recuperar detalle que sí
  está en los datos, pero repartido entre píxeles vecinos. Los trazos salen
  macizos en lugar de moteados, y eso es lo que se lee como nitidez.

  Frente a la máscara asimétrica que hubo entre medias, medido sobre la misma
  hoja real a 300 ppp: los halos bajan del 0,21 % al 0,05 % y el lápiz que
  sobrevive sube del 81,1 % al 83,2 %. Mejor en las tres cosas a la vez, que es
  raro y por eso justificó el cambio.

  **La corrección se aplica de forma asimétrica**, y esa parte sí se conserva de
  la versión anterior. Richardson-Lucy oscurece la tinta y aclara el papel por
  igual, y aclarar el papel sube los grises flojos: la curva de tono que viene
  después los manda a blanco, o sea que **borra el lápiz**. Medido en su día:
  subiendo el enfoque simétrico lo justo para estrechar el borde de 2,64 a
  2,05 px, el trazo de lápiz que sobrevivía caía del 85 % al 50 %. Se ganaba
  filo en lo impreso a cambio de perder la mitad de lo escrito a mano. Por eso
  la corrección va entera hacia la tinta y frenada hacia el papel.

  El deslizador de intensidad ya no gradúa una cantidad sino **cuántas veces se
  corrige**, de cuatro a diez vueltas, que es lo único que la deconvolución
  tiene de graduable. Cuesta lo suyo y conviene decirlo: medido en un Galaxy
  S22 Ultra, revelar una página entera a 300 ppp con la intensidad al máximo son
  **2,85 s**, y rehacer la vista previa de la pantalla de revisión, **0,53 s**.
  Lo segundo es lo que se nota al mover el deslizador.

  Antes de llegar aquí se descartaron dos caminos, y los dos por medición: subir
  la resolución de captura (la cámara entrega 4000×3000 y es el máximo que
  expone el sistema; el modo de 108 MP es propietario de Samsung y CameraX no lo
  ve) y apretar más la máscara de desenfoque (ocho combinaciones probadas; la
  mejor estrechaba el borde un 18 % y ninguna cambiaba nada que se viera).

- **Una página sin color ya no se guarda por triplicado.** Una hoja escaneada y
  mejorada es gris: el filtro la deja en tinta y papel, con el mismo valor
  repetido en los tres canales. PDFBox escribe siempre `DeviceRGB`, así que cada
  página viajaba tres veces dentro del PDF. Ahora, cuando la imagen es gris de
  verdad, se guarda en un canal. Sobre la misma hoja a 300 ppp: 4,55 MB en RGB
  contra **2,82 MB** en gris, exactamente los mismos píxeles.

  Con eso, la página más nítida sale además **más ligera** que antes: la versión
  anterior, a 250 ppp y en RGB, pesaba 3,19 MB. La comprobación de «esto es
  gris» mira todos los píxeles y no una muestra, y descarta también las imágenes
  con transparencia: un canal de gris no puede guardar ni el color ni el alfa, y
  equivocarse ahí no es comprimir peor, es tirar contenido sin avisar.

- **La cadena de mejora trabaja sobre el sitio.** Encadenando las versiones que
  devuelven un array nuevo había cinco páginas enteras de enteros vivas a la
  vez; a 300 ppp eso son más de doscientos megabytes para revelar una hoja, y la
  aplicación no pide `largeHeap`. Ahora son dos, así que la resolución sube y la
  memoria en vuelo **baja** respecto a la versión anterior. El aplanado de luz
  ya no materializa el fondo estimado —interpola la rejilla al vuelo— y la
  pasada vertical del desenfoque se hace con una ventana deslizante sobre el
  propio array. Hay pruebas que comparan píxel a píxel las dos versiones de cada
  paso: un anillo desfasado por una fila no rompe nada, sólo deja la página
  sutilmente peor, y eso es justo lo que no se detecta mirando.

- **El binario crece, y conviene decir cuánto.** Lo que un usuario descarga pasa
  de unos 6 MB a unos **12,5 MB**: el modelo de reconocimiento de texto pesa
  unos 6 MB y viaja dentro de la aplicación. Es el precio de que el OCR funcione
  sin conexión; la alternativa era descargarlo, y eso habría exigido el permiso
  de internet. El AAB del repositorio marca 30 MB porque lleva las cuatro
  arquitecturas y 5,8 MB de mapa de ofuscación, y de eso Play sólo entrega la
  arquitectura del teléfono.
- Las notas de versión de Google Play se generan con
  `tools/generar_notas_version.py`, que comprueba los trece idiomas y el límite
  de 500 caracteres. Antes se escribían a mano y el límite se descubría al
  pegarlas en el navegador.

## [1.3.0] — 2026-09-09

### Nuevo

- **Lo que se abre desde otra aplicación ya se puede usar, no sólo leer.** Un
  PDF que llega por mensajería o desde el gestor de archivos abría el visor y
  ahí se acababa: para firmarlo había que salir, entrar en NexaPDF y volver a
  buscar el mismo fichero. Ahora el visor lleva una caja de herramientas con
  firmar, editar, proteger con contraseña, ir a las páginas para separar o
  exportar, y guardar donde quieras.
- **Compartir varios PDF a la vez** lleva a la pantalla de unir con todos
  cargados. Antes entraban y no pasaba nada.
- **Compartir fotos** lleva a «Imágenes a PDF» con ellas puestas. Antes se
  intentaban abrir como si fueran un PDF.
- **Abrir una copia de seguridad** desde fuera ofrece importarla, en lugar de
  fallar al leerla como documento.

### Corregido

- **Abrir un segundo documento con la aplicación ya abierta la cerraba.** Al
  llegar el documento nuevo se recreaba la actividad entera, y eso dejaba dos
  almacenes de ajustes vivos sobre el mismo fichero; DataStore aborta el
  proceso en cuanto lo detecta. Se veía como «falla la primera vez y a la
  segunda funciona», porque tras cerrarse el segundo intento era un arranque en
  frío. Ahora el almacén es uno solo en todo el proceso y la aplicación no se
  recrea: lo que llega es estado que la pantalla observa, así que además no se
  pierde nada de lo que estuvieras haciendo.
- El índice de secciones ya no ocupa sitio en la barra del visor cuando el
  documento no tiene índice, que es la mayoría de las veces.
- El build de depuración se llama «NexaPDF debug». Con la misma etiqueta que el
  de publicación no había forma de distinguirlos ni en el lanzador ni, peor, al
  elegir a cuál de los dos mandar un fichero.

## [1.2.0] — 2026-09-04

### Nuevo

- **Proteger PDF**: una baldosa más en el inicio para cerrar un documento con
  contraseña, cifrado con AES de 256 bits. La misma pantalla se la quita
  después, y detecta sola en qué estado está el documento que eliges.
  Los permisos del PDF (imprimir, copiar, anotar, modificar) se pueden ajustar,
  y la pantalla dice sin adornos lo que valen: son peticiones que el lector
  respeta si quiere, no un candado.
- El **recorrido guiado** tiene un paso más para la herramienta nueva.
- **Detalle de cada firma en el visor**: se toca una firma y se despliega
  debajo, en la misma tarjeta, lo que trae el certificado — firmante, autoridad
  emisora, fecha, validez, número de serie, algoritmo, formato y si cubre todo
  el documento o una versión anterior. Al abrir una se cierra la anterior.
  Antes sólo se veía el nombre de quien firmó.
- El botón de acciones del documento arranca en **«Guardar como…»**. El orden
  del desplegable no cambia —separar y dividir siguen primero, que es como se
  lee la pantalla—, pero lo que uno hace al terminar casi siempre es guardar, y
  eso merece estar a un toque en lugar de a dos.

### Corregido

- **La firma con certificado no era PAdES.** Le faltaba `signingCertificateV2`,
  el atributo firmado que lleva dentro el hash del certificado del firmante y
  que CAdES-BES exige, y salía con el subfiltro antiguo de Adobe
  (`adbe.pkcs7.detached`). La criptografía era correcta, pero un validador
  conforme a la norma europea —VALIDe, DSS— no la clasificaba como PAdES. Ahora
  es **PAdES-B-B** (`ETSI.CAdES.detached`), como la de AutoFirma. Se quita
  además el atributo `signingTime`, que en PAdES no debe estar porque la hora va
  en la entrada `/M` del diccionario de la firma.
- Al elegir un certificado del almacén del sistema no cambiaba nada a la vista:
  el nombre del certificado y el botón de firmar quedaban al final de la lista,
  fuera de la pantalla. Ahora la pantalla baja sola hasta ellos.
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
