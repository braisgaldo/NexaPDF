#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera PDFs e imagenes de prueba para probar NexaPDF en el movil.

Los PDF se escriben a mano, sin bibliotecas: un PDF minimo con texto son cuatro
objetos y una tabla de referencias cruzadas, y hacerlo asi evita anadir una
dependencia solo para las pruebas. El texto va en Helvetica, una de las catorce
fuentes que todo lector de PDF trae de serie.

Uso:
    python tools/generar_ficheros_prueba.py [carpeta-destino]
"""

from __future__ import annotations

import pathlib
import struct
import sys
import zlib


# --------------------------------------------------------------------------
# PDF
# --------------------------------------------------------------------------

def _escapar(texto: str) -> str:
    return texto.replace("\\", r"\\").replace("(", r"\(").replace(")", r"\)")


def crear_pdf(destino: pathlib.Path, titulo: str, paginas: int) -> None:
    """Escribe un PDF de [paginas] paginas A4, cada una con su numero."""
    ancho, alto = 595, 842

    objetos: list[bytes] = []

    def añadir(cuerpo: bytes) -> int:
        objetos.append(cuerpo)
        return len(objetos)

    # 1: catalogo, 2: arbol de paginas (se rellenan al final)
    objetos.append(b"")
    objetos.append(b"")
    fuente = añadir(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")

    ids_pagina = []
    for numero in range(1, paginas + 1):
        lineas = [
            f"BT /F1 34 Tf 60 {alto - 110} Td ({_escapar(titulo)}) Tj ET",
            f"BT /F1 22 Tf 60 {alto - 165} Td (Pagina {numero} de {paginas}) Tj ET",
            f"BT /F1 13 Tf 60 {alto - 215} Td "
            f"(Documento de prueba generado para verificar NexaPDF.) Tj ET",
            # Un marco y una linea, para ver a simple vista si el recorte o el
            # giro de pagina descolocan algo.
            f"1.5 w 0.2 0.3 0.7 RG 40 40 {ancho - 80} {alto - 80} re S",
            f"0.85 0.2 0.2 RG 3 w 60 {alto - 240} m {ancho - 60} {alto - 240} l S",
        ]
        contenido = "\n".join(lineas).encode("latin-1")
        comprimido = zlib.compress(contenido)
        flujo = añadir(
            b"<< /Length " + str(len(comprimido)).encode() + b" /Filter /FlateDecode >>\n"
            b"stream\n" + comprimido + b"\nendstream"
        )
        pagina = añadir(
            f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {ancho} {alto}] "
            f"/Resources << /Font << /F1 {fuente} 0 R >> >> "
            f"/Contents {flujo} 0 R >>".encode()
        )
        ids_pagina.append(pagina)

    hijos = " ".join(f"{i} 0 R" for i in ids_pagina)
    objetos[1] = f"<< /Type /Pages /Kids [{hijos}] /Count {paginas} >>".encode()
    objetos[0] = b"<< /Type /Catalog /Pages 2 0 R >>"

    salida = bytearray(b"%PDF-1.7\n%\xe2\xe3\xcf\xd3\n")
    posiciones = []
    for indice, cuerpo in enumerate(objetos, start=1):
        posiciones.append(len(salida))
        salida += f"{indice} 0 obj\n".encode() + cuerpo + b"\nendobj\n"

    inicio_xref = len(salida)
    salida += f"xref\n0 {len(objetos) + 1}\n".encode()
    salida += b"0000000000 65535 f \n"
    for posicion in posiciones:
        salida += f"{posicion:010d} 00000 n \n".encode()
    salida += (
        f"trailer\n<< /Size {len(objetos) + 1} /Root 1 0 R >>\n"
        f"startxref\n{inicio_xref}\n%%EOF\n"
    ).encode()

    destino.write_bytes(bytes(salida))


# --------------------------------------------------------------------------
# PNG
# --------------------------------------------------------------------------

def crear_png(destino: pathlib.Path, ancho: int, alto: int, color: tuple[int, int, int],
              franjas: bool = True) -> None:
    """Escribe un PNG plano con unas franjas, sin dependencias."""
    filas = bytearray()
    for y in range(alto):
        filas.append(0)  # filtro "None" al principio de cada fila
        for x in range(ancho):
            if franjas and (x // 40 + y // 40) % 2 == 0:
                r, g, b = (min(255, c + 45) for c in color)
            else:
                r, g, b = color
            # Un circulo claro en el centro da una referencia de escala y de
            # recorte al mirar el PDF resultante.
            dx, dy = x - ancho / 2, y - alto / 2
            if dx * dx + dy * dy < (min(ancho, alto) / 3.2) ** 2:
                r, g, b = 250, 250, 250
            filas += bytes((r, g, b))

    def trozo(tipo: bytes, datos: bytes) -> bytes:
        return (
            struct.pack(">I", len(datos))
            + tipo
            + datos
            + struct.pack(">I", zlib.crc32(tipo + datos) & 0xFFFFFFFF)
        )

    png = b"\x89PNG\r\n\x1a\n"
    png += trozo(b"IHDR", struct.pack(">IIBBBBB", ancho, alto, 8, 2, 0, 0, 0))
    png += trozo(b"IDAT", zlib.compress(bytes(filas), 6))
    png += trozo(b"IEND", b"")
    destino.write_bytes(png)


def main() -> int:
    destino = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "build/pruebas")
    destino.mkdir(parents=True, exist_ok=True)

    documentos = [
        ("NexaPDF prueba A.pdf", "Informe trimestral", 3),
        ("NexaPDF prueba B.pdf", "Anexo tecnico", 2),
        ("NexaPDF prueba largo.pdf", "Manual extenso", 8),
    ]
    for nombre, titulo, paginas in documentos:
        crear_pdf(destino / nombre, titulo, paginas)
        print(f"  {nombre}  ({paginas} paginas, {(destino / nombre).stat().st_size} bytes)")

    imagenes = [
        ("NexaPDF foto 1.png", 900, 640, (54, 92, 176)),
        ("NexaPDF foto 2.png", 640, 900, (32, 140, 96)),
        ("NexaPDF foto 3.png", 800, 800, (190, 96, 40)),
    ]
    for nombre, ancho, alto, color in imagenes:
        crear_png(destino / nombre, ancho, alto, color)
        print(f"  {nombre}  ({ancho}x{alto}, {(destino / nombre).stat().st_size} bytes)")

    print(f"\nGenerado en {destino.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
