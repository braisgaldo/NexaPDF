# ADR-0004 · Conversión entre PDF y ofimática sin bibliotecas de terceros

- **Fecha:** 2026-09-03
- **Estado:** aceptada

## Contexto

NexaPDF debe poder unir documentos de Word, Excel y PowerPoint junto a los PDF,
y exportar el resultado a cualquiera de los cuatro formatos. Todo sin conexión.

## Alternativas consideradas

| Opción | Resultado |
|---|---|
| **Apache POI** | Es *la* biblioteca de OOXML en Java. En Android arrastra decenas de megas, depende de APIs de Java SE que Android no tiene y su uso en Android es notoriamente problemático. Descartada. |
| **LibreOffice embebido** | Único motor que convierte con fidelidad real. Ronda los 200 MB. Impensable en una app de móvil. Descartada. |
| **Un servicio de conversión en la nube** | Fidelidad excelente y coste bajo. Rompe la premisa del proyecto: los documentos saldrían del dispositivo. Descartada, y con ella el permiso de internet. |
| **Leer y escribir el OOXML directamente** | Sin dependencias, sin peso, sin red. A cambio, fidelidad limitada. **Elegida.** |

## Decisión

Se implementa la conversión leyendo y escribiendo los paquetes OOXML a mano.

Un `.docx`, un `.xlsx` y un `.pptx` son lo mismo por dentro: un ZIP con ficheros
XML y una tabla de tipos de contenido. `java.util.zip` abre el ZIP y las partes
concretas que interesan se leen con expresiones regulares (`XmlPlano`), porque
son XML planos y predecibles y un analizador DOM cargaría en memoria un
documento de decenas de megas para acabar leyendo lo mismo.

## Qué se promete y qué no

Esto es lo importante, y está dicho también dentro de la aplicación antes de
convertir:

**Hacia PDF** se traduce el **contenido**, no la maquetación:

- **Word** → párrafos con negrita, cursiva, tamaño y saltos de página.
- **Excel** → una tabla con sus celdas, cabecera destacada y página apaisada.
- **PowerPoint** → una página por diapositiva, con los textos y las imágenes
  incrustadas colocados según las coordenadas EMU de la propia diapositiva.

**Desde PDF** se recupera lo que un PDF guarda, que son letras colocadas en
coordenadas, no párrafos ni celdas:

- **Word** → el texto con sus líneas y sus saltos de página. Es la conversión
  más fiel, porque un documento de texto es justo lo que un PDF de texto tiene.
- **Excel** → las líneas se agrupan en filas por altura y en columnas por la
  separación horizontal. Funciona bien con tablas alineadas y regular con tablas
  irregulares. No hay forma de acertar siempre sin adivinar.
- **PowerPoint** → una diapositiva por página, con la página como imagen. Es la
  que conserva el aspecto exacto, a cambio de que el texto deja de ser editable.

## Consecuencias

**A favor.** Cero dependencias nuevas, cero kilobytes de biblioteca, cero red y
un código que cabe en tres ficheros y se entiende leyéndolo.

**En contra.** No es Word. Un documento con columnas, tablas anidadas, notas al
pie o estilos complejos saldrá simplificado. La interfaz lo advierte antes de
convertir y la ayuda lo explica; lo que no se hace es prometer una fidelidad que
no existe.

**Cubierto por pruebas.** `ConversorDocumentosAndroidTest` genera sus propios
`.docx`, `.xlsx` y `.pptx` escribiendo el ZIP y el XML a mano, los convierte, y
comprueba que el contenido sobrevive el viaje de ida y vuelta y que los paquetes
generados son ZIP válidos con su tabla de tipos de contenido.
