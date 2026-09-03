#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera el icono y el grafico destacado para la ficha de Google Play.

Se dibujan aqui y no a mano en un editor por una razon practica: son el mismo
diseno que el icono de la aplicacion (`ic_launcher_foreground.xml`), y tenerlo en
un script garantiza que si cambia el icono se puede regenerar la ficha sin que
se queden desincronizados.

Google Play pide:
  - Icono: 512 x 512, PNG de 32 bits, sin transparencia.
  - Grafico destacado: 1024 x 500, PNG o JPEG, sin transparencia.

Uso:
    pip install pillow
    python tools/generar_graficos_tienda.py
"""

from __future__ import annotations

import pathlib
import sys

from PIL import Image, ImageDraw, ImageFont

RAIZ = pathlib.Path(__file__).resolve().parent.parent
DESTINO = RAIZ / "docs" / "google_play" / "graficos"

# Paleta de la aplicacion (familia Indigo, generada por generar_colores.py).
AZUL_FONDO = (59, 78, 196)
AZUL_OSCURO = (27, 42, 138)
BLANCO = (255, 255, 255)
INDIGO_TEXTO = (223, 224, 255)

FUENTES_TITULO = [
    "C:/Windows/Fonts/segoeuib.ttf",
    "C:/Windows/Fonts/arialbd.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
]
FUENTES_TEXTO = [
    "C:/Windows/Fonts/segoeui.ttf",
    "C:/Windows/Fonts/arial.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
]


def cargar_fuente(candidatas: list[str], tamano: int) -> ImageFont.FreeTypeFont:
    for ruta in candidatas:
        if pathlib.Path(ruta).exists():
            return ImageFont.truetype(ruta, tamano)
    return ImageFont.load_default(tamano)


def dibujar_documentos(dibujo: ImageDraw.ImageDraw, escala: float, dx: float, dy: float) -> None:
    """Dibuja las dos hojas del icono en el lienzo de 108 unidades del original."""

    def punto(x: float, y: float) -> tuple[float, float]:
        return (dx + x * escala, dy + y * escala)

    def caja(x1: float, y1: float, x2: float, y2: float) -> list[tuple[float, float]]:
        return [punto(x1, y1), punto(x2, y2)]

    radio = 5 * escala

    # Hoja de atras, translucida sobre el fondo azul.
    dibujo.rounded_rectangle(
        caja(28, 24, 66, 72),
        radius=radio,
        fill=(
            int(AZUL_FONDO[0] + (255 - AZUL_FONDO[0]) * 0.45),
            int(AZUL_FONDO[1] + (255 - AZUL_FONDO[1]) * 0.45),
            int(AZUL_FONDO[2] + (255 - AZUL_FONDO[2]) * 0.45),
        ),
    )

    # Hoja principal con la esquina doblada.
    dibujo.polygon(
        [
            punto(46, 34), punto(70, 34), punto(80, 44), punto(80, 78),
            punto(77.3, 82.5), punto(74, 84), punto(46, 84),
            punto(42.7, 82.5), punto(40, 78), punto(40, 40), punto(42.7, 35.5),
        ],
        fill=BLANCO,
    )
    # Doblez.
    dibujo.polygon([punto(70, 34), punto(80, 44), punto(72, 44), punto(70, 41)],
                   fill=(120, 135, 205))

    # Lineas de contenido.
    for y1, y2, x2 in ((54, 58, 72), (63, 67, 70), (72, 76, 62)):
        dibujo.rounded_rectangle(caja(50, y1, x2, y2), radius=2 * escala, fill=AZUL_FONDO)


def generar_icono() -> pathlib.Path:
    lado = 512
    imagen = Image.new("RGB", (lado, lado), AZUL_FONDO)
    dibujo = ImageDraw.Draw(imagen)

    # El icono adaptativo de Android dibuja en un lienzo de 108 unidades con la
    # zona segura en el centro; aqui se aprovecha algo mas de superficie porque
    # Play aplica su propia mascara redondeada.
    escala = lado / 108 * 1.06
    desplazamiento = (lado - 108 * escala) / 2
    dibujar_documentos(dibujo, escala, desplazamiento, desplazamiento)

    destino = DESTINO / "icono-512.png"
    imagen.save(destino, "PNG")
    return destino


def generar_grafico_destacado() -> pathlib.Path:
    ancho, alto = 1024, 500
    imagen = Image.new("RGB", (ancho, alto), AZUL_OSCURO)
    dibujo = ImageDraw.Draw(imagen)

    # Degradado vertical suave, mas claro arriba.
    for y in range(alto):
        proporcion = y / alto
        color = tuple(
            int(AZUL_FONDO[i] + (AZUL_OSCURO[i] - AZUL_FONDO[i]) * proporcion)
            for i in range(3)
        )
        dibujo.line([(0, y), (ancho, y)], fill=color)

    # El icono a la izquierda, dentro de un cuadrado redondeado claro.
    lado_icono = 260
    margen = 72
    dibujo.rounded_rectangle(
        [(margen, (alto - lado_icono) // 2), (margen + lado_icono, (alto + lado_icono) // 2)],
        radius=58,
        fill=(255, 255, 255, 255),
    )
    escala = lado_icono / 108 * 0.78
    dx = margen + (lado_icono - 108 * escala) / 2
    dy = (alto - lado_icono) / 2 + (lado_icono - 108 * escala) / 2

    # Sobre fondo blanco, las hojas se dibujan en azul para que se vean.
    dibujo.rounded_rectangle(
        [(dx + 28 * escala, dy + 24 * escala), (dx + 66 * escala, dy + 72 * escala)],
        radius=5 * escala,
        fill=(196, 202, 240),
    )
    dibujo.polygon(
        [
            (dx + 46 * escala, dy + 34 * escala), (dx + 70 * escala, dy + 34 * escala),
            (dx + 80 * escala, dy + 44 * escala), (dx + 80 * escala, dy + 78 * escala),
            (dx + 74 * escala, dy + 84 * escala), (dx + 46 * escala, dy + 84 * escala),
            (dx + 40 * escala, dy + 78 * escala), (dx + 40 * escala, dy + 40 * escala),
        ],
        fill=AZUL_FONDO,
    )
    for y1, y2, x2 in ((54, 58, 72), (63, 67, 70), (72, 76, 62)):
        dibujo.rounded_rectangle(
            [(dx + 50 * escala, dy + y1 * escala), (dx + x2 * escala, dy + y2 * escala)],
            radius=2 * escala,
            fill=BLANCO,
        )

    # Texto a la derecha.
    x_texto = margen + lado_icono + 64
    titulo = cargar_fuente(FUENTES_TITULO, 92)
    lema = cargar_fuente(FUENTES_TEXTO, 34)
    detalle = cargar_fuente(FUENTES_TEXTO, 27)

    dibujo.text((x_texto, 168), "NexaPDF", font=titulo, fill=BLANCO)
    dibujo.text((x_texto, 272), "Herramientas PDF sin conexión", font=lema, fill=INDIGO_TEXTO)
    dibujo.text(
        (x_texto, 320),
        "Unir · Separar · Editar · Firmar · Convertir",
        font=detalle,
        fill=(190, 196, 240),
    )

    destino = DESTINO / "grafico-destacado-1024x500.png"
    imagen.save(destino, "PNG")
    return destino


def main() -> int:
    DESTINO.mkdir(parents=True, exist_ok=True)
    for generar in (generar_icono, generar_grafico_destacado):
        ruta = generar()
        with Image.open(ruta) as imagen:
            print(f"  {ruta.name}  {imagen.size[0]}x{imagen.size[1]}  "
                  f"{ruta.stat().st_size // 1024} kB  modo={imagen.mode}")
    print(f"\nGenerado en {DESTINO}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
