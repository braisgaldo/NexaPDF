#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Escribe la ficha de Google Play y comprueba los limites de la tienda.

Play Console rechaza los textos que se pasan de largo, y descubrirlo pegandolos
uno a uno en el navegador es una perdida de tiempo. Este script comprueba los
tres limites y, ademas, que no se cuele ninguna de las palabras prohibidas por
las politicas de facturacion (comprar, premium, desbloquear...).

Uso:
    python tools/generar_ficha.py             # genera y comprueba
    python tools/generar_ficha.py --comprobar # solo comprueba
"""

from __future__ import annotations

import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

from ficha_tienda import (  # noqa: E402
    DESCRIPCION_CORTA,
    DESCRIPCION_LARGA,
    IDIOMAS,
    LOCALES,
    TITULO,
)

DESTINO = pathlib.Path(__file__).resolve().parent.parent / "docs" / "google_play" / "ficha"

LIMITE_TITULO = 30
LIMITE_CORTA = 80
LIMITE_LARGA = 4000

# El punto 4.4.1 del encargo prohibe este vocabulario en la app y en la ficha:
# es lo que separa "agradecer algo gratuito" de "vender un bien digital", y por
# tanto lo que mantiene la donacion fuera de la facturacion obligatoria.
PROHIBIDAS = {
    "en": ["buy", "purchase", "unlock", "premium", "subscription", "pro version", "upgrade"],
    "es": ["comprar", "compra", "pagar", "desbloquea", "premium", "suscripción", "versión pro",
           "versión completa", "precio"],
    "fr": ["acheter", "achat", "débloquer", "premium", "abonnement", "version pro"],
    "de": ["kaufen", "kauf", "freischalten", "premium", "abonnement", "pro-version"],
    "it": ["comprare", "acquisto", "sbloccare", "premium", "abbonamento", "versione pro"],
    "gl": ["comprar", "mercar", "desbloquea", "premium", "subscrición", "versión pro"],
    "ca": ["comprar", "compra", "desbloqueja", "premium", "subscripció", "versió pro"],
}


def comprobar() -> list[str]:
    problemas: list[str] = []

    for idioma in IDIOMAS:
        for nombre, textos, limite in (
            ("titulo", TITULO, LIMITE_TITULO),
            ("descripcion corta", DESCRIPCION_CORTA, LIMITE_CORTA),
            ("descripcion larga", DESCRIPCION_LARGA, LIMITE_LARGA),
        ):
            if idioma not in textos:
                problemas.append(f"[{idioma}] falta la {nombre}")
                continue
            largo = len(textos[idioma])
            if largo > limite:
                problemas.append(
                    f"[{idioma}] {nombre}: {largo} caracteres, el limite es {limite}"
                )

        texto_completo = " ".join(
            t.get(idioma, "") for t in (TITULO, DESCRIPCION_CORTA, DESCRIPCION_LARGA)
        ).lower()
        for palabra in PROHIBIDAS.get(idioma, []):
            if palabra in texto_completo:
                problemas.append(
                    f"[{idioma}] aparece la palabra prohibida \"{palabra}\" "
                    f"(ver punto 4.4.1: la donacion no es una compra)"
                )

    return problemas


def escribir() -> None:
    DESTINO.mkdir(parents=True, exist_ok=True)

    for idioma in IDIOMAS:
        locale = LOCALES[idioma]
        contenido = f"""# Ficha de Google Play — {locale}

> Fichero generado por `tools/generar_ficha.py`. Los textos estan en
> `tools/ficha_tienda.py`. No editar aqui.

## Titulo ({len(TITULO[idioma])}/{LIMITE_TITULO})

```
{TITULO[idioma]}
```

## Descripcion corta ({len(DESCRIPCION_CORTA[idioma])}/{LIMITE_CORTA})

```
{DESCRIPCION_CORTA[idioma]}
```

## Descripcion completa ({len(DESCRIPCION_LARGA[idioma])}/{LIMITE_LARGA})

```
{DESCRIPCION_LARGA[idioma]}
```
"""
        (DESTINO / f"{locale}.md").write_text(contenido, encoding="utf-8")

    resumen = ["# Ficha de la tienda por idioma", "",
               "| Idioma | Locale de Play | Titulo | Corta | Larga |",
               "|---|---|---|---|---|"]
    for idioma in IDIOMAS:
        resumen.append(
            f"| {idioma} | `{LOCALES[idioma]}` | {len(TITULO[idioma])}/{LIMITE_TITULO} "
            f"| {len(DESCRIPCION_CORTA[idioma])}/{LIMITE_CORTA} "
            f"| {len(DESCRIPCION_LARGA[idioma])}/{LIMITE_LARGA} |"
        )
    resumen += [
        "",
        "El idioma predeterminado de la ficha es **es-ES**. Los demas son ",
        "traducciones que Play muestra segun el idioma del dispositivo.",
    ]
    (DESTINO / "README.md").write_text("\n".join(resumen) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--comprobar", action="store_true")
    args = parser.parse_args()

    problemas = comprobar()
    if problemas:
        print("La ficha de la tienda tiene problemas:", file=sys.stderr)
        for problema in problemas:
            print("  - " + problema, file=sys.stderr)
        return 1

    print(f"{len(IDIOMAS)} idiomas, todos dentro de los limites de Play y sin "
          f"vocabulario de compra.")
    for idioma in IDIOMAS:
        print(f"  {LOCALES[idioma]:>6}  titulo {len(TITULO[idioma]):>2}/{LIMITE_TITULO}"
              f"  corta {len(DESCRIPCION_CORTA[idioma]):>2}/{LIMITE_CORTA}"
              f"  larga {len(DESCRIPCION_LARGA[idioma]):>4}/{LIMITE_LARGA}")

    if not args.comprobar:
        escribir()
        print(f"\nEscrito en {DESTINO}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
