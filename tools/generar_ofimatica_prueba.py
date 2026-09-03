#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera .docx, .xlsx y .pptx de prueba para verificar el conversor.

Se escriben a mano con zipfile, sin python-docx ni openpyxl: los tres formatos
son un ZIP con XML dentro, y generarlos aqui garantiza que lo que prueba la app
es un paquete OOXML de verdad y no lo que una biblioteca decida producir.

Uso:
    python tools/generar_ofimatica_prueba.py [carpeta-destino]
"""

from __future__ import annotations

import pathlib
import sys
import zipfile

NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships"
NS_A = "http://schemas.openxmlformats.org/drawingml/2006/main"
NS_P = "http://schemas.openxmlformats.org/presentationml/2006/main"
CABECERA = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'


def escribir_zip(destino: pathlib.Path, partes: dict[str, str]) -> None:
    destino.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destino, "w", zipfile.ZIP_DEFLATED) as zip_salida:
        for ruta, contenido in partes.items():
            zip_salida.writestr(ruta, contenido)


def relaciones_raiz(destino: str) -> str:
    return f"""{CABECERA}
<Relationships xmlns="{NS_PKG_REL}">
  <Relationship Id="rId1" Type="{NS_REL}/officeDocument" Target="{destino}"/>
</Relationships>"""


# --------------------------------------------------------------------------

def crear_docx(destino: pathlib.Path) -> None:
    parrafos = [
        ("Informe de prueba de NexaPDF", True, 32),
        ("", False, 22),
        ("Este documento sirve para comprobar que la conversion de Word a PDF "
         "conserva el texto, los saltos de linea y el formato basico.", False, 22),
        ("", False, 22),
        ("Segundo apartado", True, 26),
        ("Una linea con acentos, enes y simbolos: cafe, nino, 25 %, 12,50 EUR.", False, 22),
        ("Otra linea mas larga para comprobar que el ajuste de linea funciona cuando "
         "el parrafo no cabe de una sola vez en el ancho de la pagina y hay que "
         "repartirlo en varias lineas seguidas.", False, 22),
    ]

    cuerpo = ""
    for texto, negrita, medio_punto in parrafos:
        propiedades = f'<w:rPr>{"<w:b/>" if negrita else ""}<w:sz w:val="{medio_punto}"/></w:rPr>'
        if texto:
            cuerpo += (
                f'<w:p><w:r>{propiedades}'
                f'<w:t xml:space="preserve">{texto}</w:t></w:r></w:p>'
            )
        else:
            cuerpo += "<w:p/>"

    documento = f"""{CABECERA}
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>{cuerpo}</w:body>
</w:document>"""

    escribir_zip(destino, {
        "[Content_Types].xml": f"""{CABECERA}
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""",
        "_rels/.rels": relaciones_raiz("word/document.xml"),
        "word/document.xml": documento,
    })


def crear_xlsx(destino: pathlib.Path) -> None:
    filas = [
        ["Concepto", "Unidades", "Precio", "Total"],
        ["Teclado mecanico", "2", "89,90", "179,80"],
        ["Monitor 27 pulgadas", "1", "249,00", "249,00"],
        ["Cable HDMI", "3", "7,50", "22,50"],
        ["Soporte de monitor", "1", "45,00", "45,00"],
        ["", "", "Suma", "496,30"],
    ]

    textos: list[str] = []
    indices: dict[str, int] = {}
    for fila in filas:
        for celda in fila:
            if celda and celda not in indices:
                indices[celda] = len(textos)
                textos.append(celda)

    filas_xml = ""
    for numero_fila, fila in enumerate(filas, start=1):
        filas_xml += f'<row r="{numero_fila}">'
        for numero_columna, celda in enumerate(fila):
            if not celda:
                continue
            letra = chr(ord("A") + numero_columna)
            filas_xml += f'<c r="{letra}{numero_fila}" t="s"><v>{indices[celda]}</v></c>'
        filas_xml += "</row>"

    cadenas = "".join(f"<si><t>{t}</t></si>" for t in textos)

    escribir_zip(destino, {
        "[Content_Types].xml": f"""{CABECERA}
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>""",
        "_rels/.rels": relaciones_raiz("xl/workbook.xml"),
        "xl/workbook.xml": f"""{CABECERA}
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="{NS_REL}">
  <sheets><sheet name="Pedido" sheetId="1" r:id="rId1"/></sheets>
</workbook>""",
        "xl/_rels/workbook.xml.rels": f"""{CABECERA}
<Relationships xmlns="{NS_PKG_REL}">
  <Relationship Id="rId1" Type="{NS_REL}/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="{NS_REL}/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>""",
        "xl/sharedStrings.xml": f"""{CABECERA}
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
     count="{len(textos)}" uniqueCount="{len(textos)}">{cadenas}</sst>""",
        "xl/worksheets/sheet1.xml": f"""{CABECERA}
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>{filas_xml}</sheetData>
</worksheet>""",
    })


def crear_pptx(destino: pathlib.Path) -> None:
    diapositivas = [
        ("NexaPDF", "Prueba de conversion de PowerPoint"),
        ("Que se comprueba", "Que los textos de cada diapositiva llegan al PDF "
                             "y que cada diapositiva ocupa una pagina."),
        ("Tercera diapositiva", "Con acentos: cafe, nino, accion, 25 %."),
    ]

    partes: dict[str, str] = {}
    overrides = ""
    lista = ""
    relaciones = f'<Relationship Id="rId1" Type="{NS_REL}/slideMaster" ' \
                 f'Target="slideMasters/slideMaster1.xml"/>'

    for numero, (titulo, cuerpo) in enumerate(diapositivas, start=1):
        overrides += (
            f'<Override PartName="/ppt/slides/slide{numero}.xml" '
            f'ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>'
        )
        lista += f'<p:sldId id="{255 + numero}" r:id="rId{numero + 1}"/>'
        relaciones += (
            f'<Relationship Id="rId{numero + 1}" Type="{NS_REL}/slide" '
            f'Target="slides/slide{numero}.xml"/>'
        )

        partes[f"ppt/slides/slide{numero}.xml"] = f"""{CABECERA}
<p:sld xmlns:a="{NS_A}" xmlns:r="{NS_REL}" xmlns:p="{NS_P}">
  <p:cSld><p:spTree>
    <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
    <p:grpSpPr/>
    <p:sp>
      <p:nvSpPr><p:cNvPr id="2" name="Titulo"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
      <p:spPr><a:xfrm><a:off x="685800" y="609600"/><a:ext cx="7772400" cy="1143000"/></a:xfrm></p:spPr>
      <p:txBody><a:bodyPr/><a:lstStyle/>
        <a:p><a:r><a:rPr lang="es-ES" sz="4000" b="1"/><a:t>{titulo}</a:t></a:r></a:p>
      </p:txBody>
    </p:sp>
    <p:sp>
      <p:nvSpPr><p:cNvPr id="3" name="Cuerpo"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
      <p:spPr><a:xfrm><a:off x="685800" y="2133600"/><a:ext cx="7772400" cy="2743200"/></a:xfrm></p:spPr>
      <p:txBody><a:bodyPr/><a:lstStyle/>
        <a:p><a:r><a:rPr lang="es-ES" sz="2000"/><a:t>{cuerpo}</a:t></a:r></a:p>
      </p:txBody>
    </p:sp>
  </p:spTree></p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""

    partes["ppt/slideMasters/slideMaster1.xml"] = f"""{CABECERA}
<p:sldMaster xmlns:a="{NS_A}" xmlns:r="{NS_REL}" xmlns:p="{NS_P}">
  <p:cSld><p:spTree>
    <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
    <p:grpSpPr/>
  </p:spTree></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2"
            accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6"
            hlink="hlink" folHlink="folHlink"/>
</p:sldMaster>"""

    partes["ppt/presentation.xml"] = f"""{CABECERA}
<p:presentation xmlns:a="{NS_A}" xmlns:r="{NS_REL}" xmlns:p="{NS_P}">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst>
  <p:sldIdLst>{lista}</p:sldIdLst>
  <p:sldSz cx="9144000" cy="6858000"/>
</p:presentation>"""

    partes["ppt/_rels/presentation.xml.rels"] = f"""{CABECERA}
<Relationships xmlns="{NS_PKG_REL}">{relaciones}</Relationships>"""

    partes["[Content_Types].xml"] = f"""{CABECERA}
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  {overrides}
</Types>"""
    partes["_rels/.rels"] = relaciones_raiz("ppt/presentation.xml")

    escribir_zip(destino, partes)


def main() -> int:
    destino = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "build/pruebas")
    destino.mkdir(parents=True, exist_ok=True)

    crear_docx(destino / "NexaPDF prueba.docx")
    crear_xlsx(destino / "NexaPDF prueba.xlsx")
    crear_pptx(destino / "NexaPDF prueba.pptx")

    for nombre in ("NexaPDF prueba.docx", "NexaPDF prueba.xlsx", "NexaPDF prueba.pptx"):
        print(f"  {nombre}  ({(destino / nombre).stat().st_size} bytes)")
    print(f"\nGenerado en {destino.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
