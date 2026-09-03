#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera los tokens de color de NexaPDF y verifica su contraste.

Los seis temas (tres familias x claro/oscuro) se derivan de rampas tonales
CIELAB, igual que hace Material 3: el tono de un color es su L*, y los pares
de roles (`primary` / `onPrimary`, ...) se eligen con una separacion tonal que
garantiza contraste. El script:

  1. construye las rampas,
  2. escribe `ColorTokens.kt` en commonMain,
  3. comprueba el contraste WCAG de cada par y falla si alguno baja de 4.5:1.

Uso:  python tools/generar_colores.py [--comprobar]
"""

from __future__ import annotations

import argparse
import math
import pathlib
import sys

# --------------------------------------------------------------------------
# Conversion de color: CIELCh(ab) D65 -> sRGB
# --------------------------------------------------------------------------

_XYZ_BLANCO_D65 = (0.95047, 1.00000, 1.08883)


def _lab_a_xyz(lightness: float, a: float, b: float) -> tuple[float, float, float]:
    fy = (lightness + 16.0) / 116.0
    fx = fy + a / 500.0
    fz = fy - b / 200.0

    def f_inv(t: float) -> float:
        return t ** 3 if t ** 3 > 0.008856 else (t - 16.0 / 116.0) / 7.787

    xn, yn, zn = _XYZ_BLANCO_D65
    return f_inv(fx) * xn, f_inv(fy) * yn, f_inv(fz) * zn


def _xyz_a_rgb_lineal(x: float, y: float, z: float) -> tuple[float, float, float]:
    r = x * 3.2404542 + y * -1.5371385 + z * -0.4985314
    g = x * -0.9692660 + y * 1.8760108 + z * 0.0415560
    b = x * 0.0556434 + y * -0.2040259 + z * 1.0572252
    return r, g, b


def _companding(c: float) -> float:
    return 12.92 * c if c <= 0.0031308 else 1.055 * (c ** (1 / 2.4)) - 0.055


def _en_gamut(rgb: tuple[float, float, float]) -> bool:
    return all(-0.0005 <= c <= 1.0005 for c in rgb)


def lch_a_hex(lightness: float, chroma: float, hue: float) -> str:
    """Convierte LCh a sRGB reduciendo croma hasta entrar en gamut."""
    c = chroma
    while c >= 0:
        rad = math.radians(hue)
        a, b = c * math.cos(rad), c * math.sin(rad)
        lineal = _xyz_a_rgb_lineal(*_lab_a_xyz(lightness, a, b))
        if _en_gamut(lineal) or c < 0.05:
            r, g, bl = (min(1.0, max(0.0, _companding(v))) for v in lineal)
            return "#{:02X}{:02X}{:02X}".format(
                round(r * 255), round(g * 255), round(bl * 255)
            )
        c -= 0.5
    return "#000000"


# --------------------------------------------------------------------------
# Contraste WCAG 2.1
# --------------------------------------------------------------------------

def _luminancia(hex_color: str) -> float:
    valor = hex_color.lstrip("#")
    canales = [int(valor[i:i + 2], 16) / 255.0 for i in (0, 2, 4)]
    lineales = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in canales]
    return 0.2126 * lineales[0] + 0.7152 * lineales[1] + 0.0722 * lineales[2]


def contraste(color_a: str, color_b: str) -> float:
    la, lb = _luminancia(color_a), _luminancia(color_b)
    claro, oscuro = max(la, lb), min(la, lb)
    return (claro + 0.05) / (oscuro + 0.05)


# --------------------------------------------------------------------------
# Familias de color
# --------------------------------------------------------------------------

class Familia:
    def __init__(self, clave: str, nombre: str, hue: float, croma: float, hue_terciario: float):
        self.clave = clave
        self.nombre = nombre
        self.primaria = (hue, croma)
        self.secundaria = (hue, 16.0)
        self.terciaria = (hue_terciario, 26.0)
        self.neutra = (hue, 3.0)
        self.neutra_variante = (hue, 8.0)
        self.error = (25.0, 60.0)

    def tono(self, paleta: tuple[float, float], t: float) -> str:
        hue, croma = paleta
        return lch_a_hex(t, croma, hue)


FAMILIAS = [
    Familia("INDIGO", "Indigo", hue=290.0, croma=42.0, hue_terciario=340.0),
    Familia("BOSQUE", "Bosque", hue=155.0, croma=38.0, hue_terciario=215.0),
    Familia("OCASO", "Ocaso", hue=50.0, croma=48.0, hue_terciario=350.0),
]


def esquema_claro(f: Familia) -> dict[str, str]:
    p, s, t, n, nv, e = (
        f.primaria, f.secundaria, f.terciaria, f.neutra, f.neutra_variante, f.error,
    )
    return {
        "primary": f.tono(p, 40), "onPrimary": f.tono(p, 100),
        "primaryContainer": f.tono(p, 90), "onPrimaryContainer": f.tono(p, 10),
        "inversePrimary": f.tono(p, 80),
        "secondary": f.tono(s, 40), "onSecondary": f.tono(s, 100),
        "secondaryContainer": f.tono(s, 90), "onSecondaryContainer": f.tono(s, 10),
        "tertiary": f.tono(t, 40), "onTertiary": f.tono(t, 100),
        "tertiaryContainer": f.tono(t, 90), "onTertiaryContainer": f.tono(t, 10),
        "error": f.tono(e, 40), "onError": f.tono(e, 100),
        "errorContainer": f.tono(e, 90), "onErrorContainer": f.tono(e, 10),
        "background": f.tono(n, 98), "onBackground": f.tono(n, 10),
        "surface": f.tono(n, 98), "onSurface": f.tono(n, 10),
        "surfaceVariant": f.tono(nv, 90), "onSurfaceVariant": f.tono(nv, 30),
        "surfaceTint": f.tono(p, 40),
        "inverseSurface": f.tono(n, 20), "inverseOnSurface": f.tono(n, 95),
        "outline": f.tono(nv, 50), "outlineVariant": f.tono(nv, 80),
        "scrim": "#000000",
        "surfaceBright": f.tono(n, 98), "surfaceDim": f.tono(n, 87),
        "surfaceContainerLowest": f.tono(n, 100), "surfaceContainerLow": f.tono(n, 96),
        "surfaceContainer": f.tono(n, 94), "surfaceContainerHigh": f.tono(n, 92),
        "surfaceContainerHighest": f.tono(n, 90),
    }


def esquema_oscuro(f: Familia) -> dict[str, str]:
    p, s, t, n, nv, e = (
        f.primaria, f.secundaria, f.terciaria, f.neutra, f.neutra_variante, f.error,
    )
    return {
        "primary": f.tono(p, 80), "onPrimary": f.tono(p, 20),
        "primaryContainer": f.tono(p, 30), "onPrimaryContainer": f.tono(p, 90),
        "inversePrimary": f.tono(p, 40),
        "secondary": f.tono(s, 80), "onSecondary": f.tono(s, 20),
        "secondaryContainer": f.tono(s, 30), "onSecondaryContainer": f.tono(s, 90),
        "tertiary": f.tono(t, 80), "onTertiary": f.tono(t, 20),
        "tertiaryContainer": f.tono(t, 30), "onTertiaryContainer": f.tono(t, 90),
        "error": f.tono(e, 80), "onError": f.tono(e, 20),
        "errorContainer": f.tono(e, 30), "onErrorContainer": f.tono(e, 90),
        "background": f.tono(n, 6), "onBackground": f.tono(n, 90),
        "surface": f.tono(n, 6), "onSurface": f.tono(n, 90),
        "surfaceVariant": f.tono(nv, 30), "onSurfaceVariant": f.tono(nv, 80),
        "surfaceTint": f.tono(p, 80),
        "inverseSurface": f.tono(n, 90), "inverseOnSurface": f.tono(n, 20),
        "outline": f.tono(nv, 60), "outlineVariant": f.tono(nv, 30),
        "scrim": "#000000",
        "surfaceBright": f.tono(n, 24), "surfaceDim": f.tono(n, 6),
        "surfaceContainerLowest": f.tono(n, 4), "surfaceContainerLow": f.tono(n, 10),
        "surfaceContainer": f.tono(n, 12), "surfaceContainerHigh": f.tono(n, 17),
        "surfaceContainerHighest": f.tono(n, 22),
    }


# Pares que deben cumplir 4.5:1 (texto normal, nivel AA).
PARES_AA = [
    ("onPrimary", "primary"),
    ("onPrimaryContainer", "primaryContainer"),
    ("onSecondary", "secondary"),
    ("onSecondaryContainer", "secondaryContainer"),
    ("onTertiary", "tertiary"),
    ("onTertiaryContainer", "tertiaryContainer"),
    ("onError", "error"),
    ("onErrorContainer", "errorContainer"),
    ("onBackground", "background"),
    ("onSurface", "surface"),
    ("onSurfaceVariant", "surfaceVariant"),
    ("onSurface", "surfaceContainer"),
    ("onSurface", "surfaceContainerHigh"),
    ("onSurface", "surfaceContainerHighest"),
    ("onSurfaceVariant", "surface"),
    ("inverseOnSurface", "inverseSurface"),
    ("primary", "surface"),
    ("primary", "surfaceContainer"),
]

# Pares no textuales: bordes e iconos decorativos, nivel AA no textual (3:1).
PARES_AA_NO_TEXTO = [
    ("outline", "surface"),
    ("outline", "surfaceContainer"),
]


def verificar(nombre_tema: str, esquema: dict[str, str]) -> list[str]:
    fallos = []
    for encima, debajo in PARES_AA:
        ratio = contraste(esquema[encima], esquema[debajo])
        if ratio < 4.5:
            fallos.append(f"{nombre_tema}: {encima} sobre {debajo} = {ratio:.2f}:1 (< 4.5)")
    for encima, debajo in PARES_AA_NO_TEXTO:
        ratio = contraste(esquema[encima], esquema[debajo])
        if ratio < 3.0:
            fallos.append(f"{nombre_tema}: {encima} sobre {debajo} = {ratio:.2f}:1 (< 3.0)")
    return fallos


ORDEN_ROLES = list(esquema_claro(FAMILIAS[0]).keys())


def kotlin_color(hex_color: str) -> str:
    return f"Color(0xFF{hex_color.lstrip('#')})"


def generar_kotlin() -> str:
    lineas: list[str] = [
        "package es.ghatostudio.nexapdf.ui.theme",
        "",
        "import androidx.compose.material3.ColorScheme",
        "import androidx.compose.material3.darkColorScheme",
        "import androidx.compose.material3.lightColorScheme",
        "import androidx.compose.ui.graphics.Color",
        "",
        "// -----------------------------------------------------------------------------",
        "// FICHERO GENERADO por tools/generar_colores.py. No editar a mano.",
        "//",
        "// Unico lugar del proyecto donde se declaran colores. Cualquier composable que",
        "// necesite un color lo toma de MaterialTheme.colorScheme; no hay literales de",
        "// color sueltos en el resto del codigo (lo verifica la tarea comprobarTokens).",
        "//",
        "// Las rampas son tonales CIELAB: el numero de tono es el L* del color, de modo",
        "// que las parejas de roles mantienen su contraste por construccion. Verificado",
        "// a 4.5:1 (texto) y 3:1 (bordes) por el propio generador.",
        "// -----------------------------------------------------------------------------",
        "",
    ]

    for familia in FAMILIAS:
        for modo, esquema, constructor in (
            ("Claro", esquema_claro(familia), "lightColorScheme"),
            ("Oscuro", esquema_oscuro(familia), "darkColorScheme"),
        ):
            nombre = f"esquema{familia.nombre}{modo}"
            lineas.append(f"internal val {nombre}: ColorScheme = {constructor}(")
            for rol in ORDEN_ROLES:
                lineas.append(f"    {rol} = {kotlin_color(esquema[rol])},")
            lineas.append(")")
            lineas.append("")

    return "\n".join(lineas)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--comprobar", action="store_true",
                        help="solo verifica el contraste, no escribe nada")
    args = parser.parse_args()

    fallos: list[str] = []
    for familia in FAMILIAS:
        fallos += verificar(f"{familia.nombre} claro", esquema_claro(familia))
        fallos += verificar(f"{familia.nombre} oscuro", esquema_oscuro(familia))

    for familia in FAMILIAS:
        for modo, esquema in (("claro", esquema_claro(familia)), ("oscuro", esquema_oscuro(familia))):
            peor = min(
                (contraste(esquema[a], esquema[b]), a, b) for a, b in PARES_AA
            )
            print(f"  {familia.nombre:8s} {modo:6s}  peor par de texto: "
                  f"{peor[1]} sobre {peor[2]} = {peor[0]:.2f}:1")

    if fallos:
        print("\nFALLOS DE CONTRASTE:", file=sys.stderr)
        for fallo in fallos:
            print("  - " + fallo, file=sys.stderr)
        return 1

    print("\nTodos los pares cumplen AA (4.5:1 texto, 3:1 no textual).")

    if not args.comprobar:
        destino = (
            pathlib.Path(__file__).resolve().parent.parent
            / "composeApp/src/commonMain/kotlin/es/ghatostudio/nexapdf/ui/theme/ColorTokens.kt"
        )
        destino.parent.mkdir(parents=True, exist_ok=True)
        destino.write_text(generar_kotlin(), encoding="utf-8")
        print(f"Escrito: {destino}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
