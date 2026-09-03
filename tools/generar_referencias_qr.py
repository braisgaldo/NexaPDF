#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Regenera y valida las matrices de referencia de CodificadorQrTest.

Hace dos comprobaciones independientes del generador de QR de NexaPDF:

  1. **Contraste con otra implementacion.** Genera cada caso con la biblioteca
     `qrcode` de Python (modo byte forzado, para que ambas codifiquen igual) y
     saca la mascara que esa biblioteca eligio. La prueba de Kotlin fija esa
     misma mascara y compara la matriz entera.

  2. **Decodificacion real.** Renderiza a imagen las matrices que produce
     NexaPDF con su propia eleccion de mascara y las pasa por el detector de QR
     de OpenCV. Si alguna no devuelve el texto original, el generador esta mal
     por mucho que las matrices coincidan.

Uso:
    pip install qrcode opencv-python-headless numpy
    python tools/generar_referencias_qr.py            # solo valida
    python tools/generar_referencias_qr.py --volcado FICHERO
        FICHERO son las lineas "texto<TAB>nivel<TAB>tamano<TAB>matriz" que
        imprime la prueba de volcado de Kotlin.
"""

from __future__ import annotations

import argparse
import sys

CASOS = [
    ("https://revolut.me/brais2oz6", "MEDIO"),
    ("NexaPDF", "MEDIO"),
    ("https://github.com/braisgaldo/NexaPDF", "BAJO"),
    ("Gracias por invitarme a un cafe", "ALTO"),
    ("1234567890" * 8, "MEDIO"),
    ("Cafe con acentos: nino, cafe, accion", "MAXIMO"),
    ("https://play.google.com/store/apps/details?id=es.ghatostudio.nexapdf", "MEDIO"),
]

# Posiciones de los quince bits de informacion de formato, del bit 0 al 14,
# en la copia junto al buscador superior izquierdo.
POSICIONES_FORMATO = [
    (8, 0), (8, 1), (8, 2), (8, 3), (8, 4), (8, 5), (8, 7), (8, 8),
    (7, 8), (5, 8), (4, 8), (3, 8), (2, 8), (1, 8), (0, 8),
]


def matrices_de_referencia() -> list[tuple[str, str, int, int, str]]:
    import qrcode
    from qrcode.util import QRData, MODE_8BIT_BYTE
    from qrcode.constants import (
        ERROR_CORRECT_L, ERROR_CORRECT_M, ERROR_CORRECT_Q, ERROR_CORRECT_H,
    )

    niveles = {
        "BAJO": ERROR_CORRECT_L,
        "MEDIO": ERROR_CORRECT_M,
        "ALTO": ERROR_CORRECT_Q,
        "MAXIMO": ERROR_CORRECT_H,
    }

    resultado = []
    for texto, nivel in CASOS:
        codigo = qrcode.QRCode(error_correction=niveles[nivel], box_size=1, border=0)
        # Sin forzar el modo, `qrcode` elegiria modo numerico para el caso de
        # solo digitos y NexaPDF, que solo implementa modo byte, no coincidiria.
        codigo.add_data(QRData(texto.encode("utf-8"), mode=MODE_8BIT_BYTE))
        codigo.make(fit=True)

        matriz = codigo.get_matrix()
        lado = len(matriz)

        bits = 0
        for indice, (x, y) in enumerate(POSICIONES_FORMATO):
            if matriz[y][x]:
                bits |= 1 << indice
        mascara = ((bits ^ 0b101010000010010) >> 10) & 7

        plano = "".join("1" if celda else "0" for fila in matriz for celda in fila)
        resultado.append((texto, nivel, lado, mascara, plano))
    return resultado


def decodificar(plano: str, lado: int) -> str:
    import cv2
    import numpy as np

    escala, borde = 8, 4
    total = (lado + borde * 2) * escala
    imagen = np.full((total, total), 255, dtype=np.uint8)
    for y in range(lado):
        for x in range(lado):
            if plano[y * lado + x] == "1":
                y0 = (y + borde) * escala
                x0 = (x + borde) * escala
                imagen[y0:y0 + escala, x0:x0 + escala] = 0

    texto, _, _ = cv2.QRCodeDetector().detectAndDecode(imagen)
    return texto


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--volcado", help="fichero con las matrices que genera Kotlin")
    args = parser.parse_args()

    print("== Matrices de referencia (mascara elegida por qrcode) ==")
    for texto, nivel, lado, mascara, plano in matrices_de_referencia():
        version = (lado - 17) // 4
        print(f"{texto}\t{nivel}\tv{version}\tmascara={mascara}\t{plano}")

    if not args.volcado:
        print("\nSin --volcado no se comprueba la decodificacion.", file=sys.stderr)
        return 0

    print("\n== Decodificacion de las matrices de NexaPDF con OpenCV ==")
    fallos = 0
    with open(args.volcado, encoding="utf-8") as fichero:
        for linea in fichero:
            if not linea.strip():
                continue
            texto, nivel, lado, plano = linea.rstrip("\n").split("\t")
            obtenido = decodificar(plano, int(lado))
            correcto = obtenido == texto
            fallos += 0 if correcto else 1
            print(f"{'OK ' if correcto else 'MAL'}  {nivel:<7} {texto[:50]!r}")
            if not correcto:
                print(f"      se decodifico: {obtenido!r}")

    if fallos:
        print(f"\n{fallos} matrices no decodifican.", file=sys.stderr)
        return 1
    print("\nTodas las matrices decodifican al texto original.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
