#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera fotos de prueba para el escáner de documentos.

Hacen falta porque probar el escáner con el teléfono conectado por USB y en la
mesa no es viable: la cámara ve lo que ve, y no se puede poner un folio delante
desde aquí. Estas imágenes recorren el mismo camino que una foto real en cuanto
se eligen desde la galería: detección de bordes, corrección de perspectiva,
filtro y reconocimiento de texto.

Cada una aporta un caso distinto:

  - `mesa-recto`: el caso fácil. Folio claro sobre madera oscura, casi de frente.
  - `mesa-perspectiva`: como sale de verdad al fotografiar a pulso, con los
    lados convergiendo.
  - `mesa-girado`: el papel torcido sobre la mesa.
  - `poco-contraste`: folio blanco sobre mesa clara, que es donde la detección
    lo tiene difícil y donde hay que poder ajustar a mano.

Uso:
    python tools/generar_fotos_escaner.py [destino]
"""

from __future__ import annotations

import pathlib
import random
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ANCHO, ALTO = 2048, 1536

TEXTO = [
    ("NexaPDF", 58, True),
    ("Acta de la reunión del 12 de marzo", 34, True),
    ("", 18, False),
    ("Asistentes: Brais Galdo, Marta Iglesias y Luis Ferreiro.", 26, False),
    ("", 10, False),
    ("Se aprueba por unanimidad el presupuesto para el", 26, False),
    ("segundo trimestre, con una partida de 4.200 euros", 26, False),
    ("destinada a la renovación de los equipos.", 26, False),
    ("", 14, False),
    ("La siguiente reunión queda fijada para el jueves 26", 26, False),
    ("de marzo a las diez de la mañana, en la sala grande.", 26, False),
    ("", 14, False),
    ("Importe total aprobado .................. 4.200,00 EUR", 26, False),
    ("Referencia del expediente ......... EXP-2026-0417", 26, False),
    ("", 20, False),
    ("Firmado: la secretaria", 24, False),
]


def fuente(tamano: int, negrita: bool) -> ImageFont.FreeTypeFont:
    nombres = (
        ["arialbd.ttf", "DejaVuSans-Bold.ttf", "segoeuib.ttf"]
        if negrita
        else ["arial.ttf", "DejaVuSans.ttf", "segoeui.ttf"]
    )
    for nombre in nombres:
        try:
            return ImageFont.truetype(nombre, tamano)
        except OSError:
            continue
    return ImageFont.load_default(tamano)


def pagina(ancho: int = 1240, alto: int = 1754) -> Image.Image:
    """Un folio con texto, en blanco y a resolución de impresión."""
    hoja = Image.new("RGB", (ancho, alto), (252, 251, 248))
    lapiz = ImageDraw.Draw(hoja)

    y = 150
    for linea, tamano, negrita in TEXTO:
        if linea:
            lapiz.text((120, y), linea, font=fuente(tamano, negrita), fill=(24, 24, 28))
        y += tamano + 22

    # Una regla y un recuadro: dan bordes internos que no deben confundirse con
    # el borde del papel. Si el detector los tomara por el contorno, el recorte
    # se comería medio documento, y eso es justo lo que hay que comprobar.
    lapiz.line((120, y + 30, ancho - 120, y + 30), fill=(120, 120, 130), width=3)
    lapiz.rectangle(
        (120, y + 70, ancho - 120, y + 260), outline=(150, 150, 160), width=3
    )
    lapiz.text(
        (150, y + 110),
        "Observaciones: ninguna.",
        font=fuente(24, False),
        fill=(60, 60, 70),
    )
    return hoja


def mesa(claro: bool) -> Image.Image:
    """Una superficie con grano, para que el fondo no sea un color plano."""
    base = (196, 190, 180) if claro else (74, 56, 42)
    fondo = Image.new("RGB", (ANCHO, ALTO), base)
    ruido = Image.effect_noise((ANCHO, ALTO), 26).convert("L")
    fondo = Image.blend(fondo, Image.merge("RGB", (ruido, ruido, ruido)), 0.16)
    return fondo.filter(ImageFilter.GaussianBlur(1.2))


def coeficientes(destino, origen):
    """Coeficientes de la homografía que PIL necesita para `Image.PERSPECTIVE`.

    PIL transforma del destino al origen, así que se resuelve el sistema al
    revés de como se lee: para cada esquina de salida, de dónde saca el píxel.
    """
    import numpy as np

    matriz = []
    for (xd, yd), (xo, yo) in zip(destino, origen):
        matriz.append([xd, yd, 1, 0, 0, 0, -xo * xd, -xo * yd])
        matriz.append([0, 0, 0, xd, yd, 1, -yo * xd, -yo * yd])
    a = np.array(matriz, dtype=float)
    b = np.array(origen, dtype=float).reshape(8)
    return np.linalg.solve(a, b)


def componer(hoja: Image.Image, esquinas, claro: bool) -> Image.Image:
    """Pega el folio sobre la mesa en la posición y perspectiva dadas."""
    fondo = mesa(claro)

    origen = [(0, 0), (hoja.width, 0), (hoja.width, hoja.height), (0, hoja.height)]
    coef = coeficientes(esquinas, origen)

    deformada = hoja.transform(
        (ANCHO, ALTO), Image.PERSPECTIVE, coef, Image.BICUBIC
    )
    mascara = Image.new("L", (hoja.width, hoja.height), 255).transform(
        (ANCHO, ALTO), Image.PERSPECTIVE, coef, Image.BICUBIC
    )

    # Una sombra bajo el papel: es lo que hay en cualquier foto real y lo que
    # hace que el borde no sea un salto de color perfecto.
    sombra = mascara.filter(ImageFilter.GaussianBlur(14)).point(lambda v: v // 3)
    fondo.paste((0, 0, 0), (0, 0), sombra)
    fondo.paste(deformada, (0, 0), mascara)

    # Un degradado de iluminación, que es lo que tiene toda foto hecha a mano y
    # lo que obliga a que el umbral del detector sea relativo y no fijo.
    luz = Image.linear_gradient("L").resize((ANCHO, ALTO)).rotate(18, expand=False)
    fondo = Image.composite(fondo, Image.new("RGB", (ANCHO, ALTO), (255, 255, 255)), luz.point(lambda v: 210 + v // 8))
    return fondo.filter(ImageFilter.GaussianBlur(0.6))


CASOS = {
    "mesa-recto": ([(300, 190), (1760, 205), (1745, 1380), (312, 1360)], False),
    "mesa-perspectiva": ([(470, 150), (1560, 230), (1740, 1330), (300, 1400)], False),
    "mesa-girado": ([(600, 130), (1820, 520), (1450, 1430), (240, 1020)], False),
    "poco-contraste": ([(330, 200), (1720, 190), (1735, 1370), (318, 1375)], True),
}


def main() -> int:
    destino = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path("build/escaner")
    destino.mkdir(parents=True, exist_ok=True)

    random.seed(7)
    hoja = pagina()
    for nombre, (esquinas, claro) in CASOS.items():
        imagen = componer(hoja, esquinas, claro)
        ruta = destino / f"{nombre}.jpg"
        imagen.save(ruta, quality=88)
        print(f"  {ruta}  ({imagen.width}x{imagen.height})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
