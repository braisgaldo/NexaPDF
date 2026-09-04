#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Arma el ZIP de símbolos nativos que pide Google Play.

Play avisa en cada subida de que el App Bundle lleva código nativo sin símbolos.
Ese código no es de NexaPDF: son dos bibliotecas que arrastran AndroidX
(`androidx.graphics:graphics-path` y el contador compartido de DataStore).

Lo normal sería que AGP los adjuntara solo con `ndk { debugSymbolLevel }`, pero
esa tarea necesita el NDK instalado para usar su `llvm-objcopy`. Instalar varios
gigas de NDK para extraer la tabla de símbolos de una biblioteca ajena de diez
kilobytes no sale a cuenta, así que se saca directamente del propio AAB: las
bibliotecas viajan dentro tal y como las publica Google, con los símbolos que
tengan.

Uso:
    python tools/simbolos_nativos.py docs/google_play/bundle/NexaPDF-1.2.0-release.aab

Deja el ZIP al lado del AAB. En Play Console se sube en la versión, dentro de
"App bundles", en el menú de tres puntos: "Subir archivo de símbolos de
depuración nativos".
"""

from __future__ import annotations

import pathlib
import struct
import sys
import zipfile


def secciones_elf(datos: bytes) -> list[str]:
    """Nombres de las secciones de un ELF de 64 o 32 bits."""
    if datos[:4] != b"\x7fELF":
        return []
    de64 = datos[4] == 2
    if de64:
        shoff = struct.unpack_from("<Q", datos, 0x28)[0]
        shentsize, shnum, shstrndx = struct.unpack_from("<HHH", datos, 0x3A)
        desplazamiento_nombres = 0x18
    else:
        shoff = struct.unpack_from("<I", datos, 0x20)[0]
        shentsize, shnum, shstrndx = struct.unpack_from("<HHH", datos, 0x2E)
        desplazamiento_nombres = 0x10

    cabecera = shoff + shstrndx * shentsize
    if de64:
        tabla = struct.unpack_from("<Q", datos, cabecera + desplazamiento_nombres)[0]
    else:
        tabla = struct.unpack_from("<I", datos, cabecera + desplazamiento_nombres)[0]

    nombres = []
    for i in range(shnum):
        posicion = struct.unpack_from("<I", datos, shoff + i * shentsize)[0]
        fin = datos.index(b"\0", tabla + posicion)
        nombres.append(datos[tabla + posicion:fin].decode("ascii", "replace"))
    return nombres


def main(ruta_aab: str) -> int:
    aab = pathlib.Path(ruta_aab)
    if not aab.exists():
        print(f"No existe {aab}")
        return 1

    destino = aab.with_name(aab.stem + "-simbolos-nativos.zip")
    conteo = 0
    con_simbolos = 0

    with zipfile.ZipFile(aab) as paquete, zipfile.ZipFile(
        destino, "w", zipfile.ZIP_DEFLATED
    ) as salida:
        for entrada in paquete.namelist():
            if not entrada.endswith(".so"):
                continue
            # base/lib/<abi>/<nombre>.so  ->  <abi>/<nombre>.so, que es la
            # estructura que Play espera dentro del ZIP.
            partes = entrada.split("/")
            if len(partes) < 3:
                continue
            abi, nombre = partes[-2], partes[-1]
            contenido = paquete.read(entrada)
            tiene = ".symtab" in secciones_elf(contenido)
            salida.writestr(f"{abi}/{nombre}", contenido)
            conteo += 1
            con_simbolos += 1 if tiene else 0
            print(f"  {abi}/{nombre:38} {'con símbolos' if tiene else 'ya venía sin símbolos'}")

    print()
    print(f"{conteo} bibliotecas, {con_simbolos} con tabla de símbolos.")
    print(f"ZIP: {destino}")
    if con_simbolos == 0:
        print()
        print("Ninguna trae símbolos: subirlo no aportaría nada y el aviso de")
        print("Play seguiría teniendo razón. Es preferible dejarlo estar.")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        raise SystemExit(2)
    raise SystemExit(main(sys.argv[1]))
