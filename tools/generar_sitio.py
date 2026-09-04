#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Genera el sitio de GitHub Pages de NexaPDF a partir del Markdown de docs/.

Google Play exige una URL publica para la politica de privacidad, y GitHub Pages
la sirve gratis desde la carpeta `docs/` del repositorio. Este script convierte
el Markdown a HTML con el mismo aspecto que la aplicacion, para que quien llegue
desde la ficha de la tienda vea algo coherente y no un documento suelto.

Se escribe el conversor a mano en vez de usar un generador de sitios: el sitio
son dos paginas y la alternativa seria arrastrar Jekyll, sus plantillas y sus
dependencias para producir exactamente lo mismo.

Uso:
    python tools/generar_sitio.py
"""

from __future__ import annotations

import html
import pathlib
import re

RAIZ = pathlib.Path(__file__).resolve().parent.parent
DOCS = RAIZ / "docs"

# Paleta Indigo de la aplicacion, generada por tools/generar_colores.py.
PLANTILLA = """<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{titulo}</title>
<meta name="description" content="{descripcion}">
<style>
  :root {{
    color-scheme: light dark;
    --fondo: #F9F9FF;
    --superficie: #FFFFFF;
    --texto: #1B1B1F;
    --suave: #46464F;
    --primario: #4A5A9F;
    --borde: #C6C5D0;
    --contenedor: #DFE0FF;
  }}
  @media (prefers-color-scheme: dark) {{
    :root {{
      --fondo: #121318;
      --superficie: #1E1F25;
      --texto: #E4E1E9;
      --suave: #C7C5D0;
      --primario: #BDC2FE;
      --borde: #46464F;
      --contenedor: #323563;
    }}
  }}
  * {{ box-sizing: border-box; }}
  body {{
    margin: 0;
    background: var(--fondo);
    color: var(--texto);
    font: 16px/1.65 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  }}
  .barra {{
    background: var(--contenedor);
    border-bottom: 1px solid var(--borde);
    padding: 18px 24px;
  }}
  .barra a {{
    color: var(--texto);
    text-decoration: none;
    font-weight: 600;
    font-size: 18px;
  }}
  main {{
    max-width: 760px;
    margin: 0 auto;
    padding: 32px 24px 64px;
  }}
  h1 {{ font-size: 30px; line-height: 1.25; margin: 8px 0 4px; }}
  h2 {{ font-size: 22px; margin-top: 40px; color: var(--primario); }}
  h3 {{ font-size: 18px; margin-top: 28px; }}
  a {{ color: var(--primario); }}
  code {{
    background: var(--superficie);
    border: 1px solid var(--borde);
    border-radius: 5px;
    padding: 1px 5px;
    font-size: 0.9em;
  }}
  table {{ border-collapse: collapse; width: 100%; margin: 18px 0; }}
  th, td {{
    border: 1px solid var(--borde);
    padding: 9px 12px;
    text-align: left;
    vertical-align: top;
  }}
  th {{ background: var(--superficie); }}
  hr {{ border: 0; border-top: 1px solid var(--borde); margin: 32px 0; }}
  blockquote {{
    margin: 18px 0;
    padding: 12px 18px;
    border-left: 4px solid var(--primario);
    background: var(--superficie);
  }}
  footer {{
    max-width: 760px;
    margin: 0 auto;
    padding: 24px;
    color: var(--suave);
    font-size: 14px;
    border-top: 1px solid var(--borde);
  }}
  .tarjetas {{ display: grid; gap: 14px; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }}
  .tarjeta {{
    background: var(--superficie);
    border: 1px solid var(--borde);
    border-radius: 14px;
    padding: 16px 18px;
  }}
  .tarjeta h3 {{ margin: 0 0 6px; font-size: 16px; }}
  .tarjeta p {{ margin: 0; color: var(--suave); font-size: 14px; }}
</style>
</head>
<body>
<div class="barra"><a href="./">NexaPDF</a></div>
<main>
{cuerpo}
</main>
<footer>
  NexaPDF · <a href="https://github.com/braisgaldo/NexaPDF">Código fuente</a> ·
  <a href="./privacidad.html">Privacidad</a> ·
  <a href="mailto:ghatostudio@proton.me">ghatostudio@proton.me</a>
</footer>
</body>
</html>
"""


def marcado_en_linea(texto: str) -> str:
    """Aplica negritas, cursivas, codigo y enlaces sobre texto ya escapado."""
    texto = re.sub(r"`([^`]+)`", r"<code>\1</code>", texto)
    texto = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", texto)
    texto = re.sub(r"(?<![*\w])\*([^*]+)\*(?!\*)", r"<em>\1</em>", texto)
    texto = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', texto)
    texto = re.sub(r"&lt;(https?://[^&]+)&gt;", r'<a href="\1">\1</a>', texto)
    return texto


def markdown_a_html(markdown: str) -> str:
    """Conversor suficiente para los documentos de este proyecto."""
    lineas = markdown.split("\n")
    salida: list[str] = []
    en_lista = False
    en_tabla = False

    def cerrar_lista() -> None:
        nonlocal en_lista
        if en_lista:
            salida.append("</ul>")
            en_lista = False

    def cerrar_tabla() -> None:
        nonlocal en_tabla
        if en_tabla:
            salida.append("</tbody></table>")
            en_tabla = False

    indice = 0
    while indice < len(lineas):
        linea = lineas[indice].rstrip()
        escapada = marcado_en_linea(html.escape(linea))

        if not linea.strip():
            cerrar_lista()
            cerrar_tabla()
        elif linea.startswith("### "):
            cerrar_lista(); cerrar_tabla()
            salida.append(f"<h3>{marcado_en_linea(html.escape(linea[4:]))}</h3>")
        elif linea.startswith("## "):
            cerrar_lista(); cerrar_tabla()
            salida.append(f"<h2>{marcado_en_linea(html.escape(linea[3:]))}</h2>")
        elif linea.startswith("# "):
            cerrar_lista(); cerrar_tabla()
            salida.append(f"<h1>{marcado_en_linea(html.escape(linea[2:]))}</h1>")
        elif linea.startswith("---"):
            cerrar_lista(); cerrar_tabla()
            salida.append("<hr>")
        elif linea.startswith("> "):
            cerrar_lista(); cerrar_tabla()
            salida.append(f"<blockquote>{marcado_en_linea(html.escape(linea[2:]))}</blockquote>")
        elif linea.startswith("| "):
            celdas = [c.strip() for c in linea.strip("|").split("|")]
            siguiente = lineas[indice + 1] if indice + 1 < len(lineas) else ""
            if not en_tabla and set(siguiente.replace("|", "").strip()) <= set("-: "):
                salida.append("<table><thead><tr>")
                salida += [f"<th>{marcado_en_linea(html.escape(c))}</th>" for c in celdas]
                salida.append("</tr></thead><tbody>")
                en_tabla = True
                indice += 1  # se salta la linea de guiones
            elif en_tabla:
                salida.append("<tr>")
                salida += [f"<td>{marcado_en_linea(html.escape(c))}</td>" for c in celdas]
                salida.append("</tr>")
        elif linea.lstrip().startswith(("- ", "* ")):
            if not en_lista:
                salida.append("<ul>")
                en_lista = True
            salida.append(f"<li>{marcado_en_linea(html.escape(linea.lstrip()[2:]))}</li>")
        else:
            cerrar_lista(); cerrar_tabla()
            salida.append(f"<p>{escapada}</p>")
        indice += 1

    cerrar_lista()
    cerrar_tabla()
    return "\n".join(salida)


PORTADA = """<h1>NexaPDF</h1>
<p>Herramientas PDF que funcionan sin conexión. Unir, separar, convertir,
editar y firmar documentos sin que salgan de tu teléfono.</p>

<div class="tarjetas">
  <div class="tarjeta"><h3>Sin conexión</h3><p>La aplicación no declara el permiso de internet: no puede enviar nada a ninguna parte.</p></div>
  <div class="tarjeta"><h3>Sin anuncios</h3><p>Ni publicidad, ni analítica, ni seguimiento de ningún tipo.</p></div>
  <div class="tarjeta"><h3>Gratis</h3><p>Todas las funciones, para todo el mundo. No hay versión de pago.</p></div>
  <div class="tarjeta"><h3>En trece idiomas</h3><p>Incluido el árabe, con la interfaz reflejada de derecha a izquierda.</p></div>
</div>

<h2>Qué hace</h2>
<ul>
<li>Unir varios documentos en uno, arrastrando para ordenarlos.</li>
<li>Separar un PDF por páginas o por rangos.</li>
<li>Convertir fotos en documentos, una por página o varias juntas.</li>
<li>Editar páginas: dibujar, resaltar, añadir texto, formas e imágenes, y aplicar filtros de mejora.</li>
<li>Firmar a mano o con un certificado electrónico, con firma verificable por cualquier lector.</li>
<li>Convertir entre PDF y Word, Excel o PowerPoint en los dos sentidos.</li>
<li>Proteger un PDF con contraseña, cifrado con AES de 256 bits, y quitársela después.</li>
</ul>

<h2>Apoyar el desarrollo</h2>
<p>NexaPDF es gratuita y lo seguirá siendo. Si te resulta útil, puedes
<a href="https://revolut.me/brais2oz6">invitarme a un café</a>. La donación es
voluntaria y no desbloquea nada.</p>

<h2>Enlaces</h2>
<ul>
<li><a href="./privacidad.html">Política de privacidad</a></li>
<li><a href="https://github.com/braisgaldo/NexaPDF">Código fuente en GitHub</a></li>
<li><a href="https://github.com/braisgaldo/NexaPDF/releases">Descargar el APK directamente</a></li>
</ul>
"""


def main() -> int:
    DOCS.mkdir(parents=True, exist_ok=True)

    # GitHub Pages pasa el sitio por Jekyll si no se le dice lo contrario, y
    # Jekyll ignora las carpetas que empiezan por guion bajo.
    (DOCS / ".nojekyll").write_text("", encoding="utf-8")

    (DOCS / "index.html").write_text(
        PLANTILLA.format(
            titulo="NexaPDF · Herramientas PDF sin conexión",
            descripcion="Une, separa, convierte, edita y firma PDF sin que salgan de tu teléfono.",
            cuerpo=PORTADA,
        ),
        encoding="utf-8",
    )

    privacidad = (DOCS / "PRIVACIDAD.md").read_text(encoding="utf-8")
    (DOCS / "privacidad.html").write_text(
        PLANTILLA.format(
            titulo="Política de privacidad · NexaPDF",
            descripcion="NexaPDF no recoge ningún dato: no tiene permiso de internet.",
            cuerpo=markdown_a_html(privacidad),
        ),
        encoding="utf-8",
    )

    print("Sitio generado en docs/:")
    for nombre in ("index.html", "privacidad.html", ".nojekyll"):
        print(f"  {nombre}  ({(DOCS / nombre).stat().st_size} bytes)")
    print("\nURL de privacidad: https://braisgaldo.github.io/NexaPDF/privacidad.html")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
