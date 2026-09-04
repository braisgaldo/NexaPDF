#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera los strings.xml de NexaPDF a partir del catalogo en Python.

Trece idiomas por trece ficheros sueltos es la forma segura de acabar con un
texto sin traducir en alguno de ellos, y sin darse cuenta hasta que un usuario
ve una pantalla a medio idioma. Aqui el catalogo es uno solo y este script
**falla** si a alguna clave le falta un idioma, si sobra alguno o si los
marcadores de posicion no coinciden entre traducciones.

Uso:
    python tools/generar_traducciones.py             # genera
    python tools/generar_traducciones.py --comprobar # solo valida
"""

from __future__ import annotations

import argparse
import pathlib
import re
import sys
from xml.sax.saxutils import escape

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import traducciones as base  # noqa: E402
import traducciones_ajustes  # noqa: E402
import traducciones_ayuda  # noqa: E402
import traducciones_cifrar  # noqa: E402
import traducciones_documento  # noqa: E402
import traducciones_editor  # noqa: E402

IDIOMAS = base.IDIOMAS
CARPETAS = base.CARPETAS

DESTINO = (
    pathlib.Path(__file__).resolve().parent.parent
    / "composeApp/src/commonMain/composeResources"
)

# Categorias de plural validas en el formato de recursos de Android.
CATEGORIAS = ["zero", "one", "two", "few", "many", "other"]

MARCADOR = re.compile(r"%\d+\$[sd]")


def reunir_textos() -> dict[str, dict[str, str]]:
    textos: dict[str, dict[str, str]] = {}
    for modulo in (
        base,
        traducciones_documento,
        traducciones_editor,
        traducciones_ajustes,
        traducciones_ayuda,
        traducciones_cifrar,
    ):
        for clave, valores in modulo.TEXTOS.items():
            if clave in textos:
                raise SystemExit(f"Clave repetida en dos modulos: {clave}")
            textos[clave] = valores
    return textos


def validar(textos: dict[str, dict[str, str]], plurales: dict) -> list[str]:
    problemas: list[str] = []
    esperados = set(IDIOMAS)

    for clave, valores in sorted(textos.items()):
        faltan = esperados - set(valores)
        sobran = set(valores) - esperados
        if faltan:
            problemas.append(f"{clave}: faltan idiomas {sorted(faltan)}")
        if sobran:
            problemas.append(f"{clave}: idiomas desconocidos {sorted(sobran)}")

        # Todos los idiomas deben usar los mismos marcadores de posicion: si el
        # ingles espera un %1$s y el aleman no lo pone, esa pantalla sale sin el
        # dato y nadie se entera hasta verla.
        referencia = sorted(MARCADOR.findall(valores.get("en", "")))
        for idioma, texto in valores.items():
            encontrados = sorted(MARCADOR.findall(texto))
            if encontrados != referencia:
                problemas.append(
                    f"{clave} [{idioma}]: marcadores {encontrados} en vez de {referencia}"
                )
            if not texto.strip():
                problemas.append(f"{clave} [{idioma}]: texto vacio")

    for clave, valores in sorted(plurales.items()):
        faltan = esperados - set(valores)
        if faltan:
            problemas.append(f"{clave} (plural): faltan idiomas {sorted(faltan)}")
        for idioma, formas in valores.items():
            if "other" not in formas:
                problemas.append(f"{clave} [{idioma}]: falta la categoria 'other'")
            for categoria in formas:
                if categoria not in CATEGORIAS:
                    problemas.append(
                        f"{clave} [{idioma}]: categoria de plural desconocida '{categoria}'"
                    )

    return problemas


def escribir(idioma: str, textos: dict[str, dict[str, str]], plurales: dict) -> pathlib.Path:
    carpeta = DESTINO / CARPETAS[idioma]
    carpeta.mkdir(parents=True, exist_ok=True)
    fichero = carpeta / "strings.xml"

    lineas = ['<?xml version="1.0" encoding="utf-8"?>']
    lineas.append("<!--")
    lineas.append("    FICHERO GENERADO por tools/generar_traducciones.py.")
    lineas.append("    No editar a mano: el catalogo esta en tools/traducciones*.py y de ahi")
    lineas.append("    salen los trece idiomas a la vez.")
    lineas.append("-->")
    lineas.append("<resources>")

    for clave in sorted(textos):
        valor = escape(textos[clave][idioma])
        lineas.append(f'    <string name="{clave}">{valor}</string>')

    for clave in sorted(plurales):
        formas = plurales[clave][idioma]
        lineas.append(f'    <plurals name="{clave}">')
        for categoria in CATEGORIAS:
            if categoria in formas:
                valor = escape(formas[categoria])
                lineas.append(f'        <item quantity="{categoria}">{valor}</item>')
        lineas.append("    </plurals>")

    lineas.append("</resources>")
    fichero.write_text("\n".join(lineas) + "\n", encoding="utf-8")
    return fichero


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--comprobar", action="store_true")
    args = parser.parse_args()

    textos = reunir_textos()
    plurales = traducciones_ayuda.PLURALES

    problemas = validar(textos, plurales)
    if problemas:
        print("El catalogo de textos tiene problemas:", file=sys.stderr)
        for problema in problemas:
            print("  - " + problema, file=sys.stderr)
        return 1

    print(f"{len(textos)} textos y {len(plurales)} plurales x {len(IDIOMAS)} idiomas: correcto.")

    if args.comprobar:
        return 0

    for idioma in IDIOMAS:
        fichero = escribir(idioma, textos, plurales)
        print(f"  {idioma:>2}  ->  {fichero.relative_to(DESTINO.parent.parent.parent)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
