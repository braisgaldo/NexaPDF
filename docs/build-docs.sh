#!/usr/bin/env bash
# Genera la documentacion de NexaPDF en HTML, PDF y DOCX.
#
# La fuente unica es el Markdown de docs/, que si va en los commits. Los
# binarios NO se commitean: se generan aqui y se adjuntan a la GitHub Release,
# de modo que cada documento corresponde a una version concreta y el
# repositorio sigue siendo ligero de clonar. docs/out/ esta en .gitignore.
#
# El HTML y el PDF salen de la misma plantilla (plantilla/documento.html mas
# documento.css): el PDF se obtiene imprimiendo el HTML con un navegador sin
# interfaz, no con un motor distinto que vuelva a interpretar el Markdown. Asi
# lo que se lee en pantalla y lo que se imprime son el mismo documento, y las
# reglas @page del CSS deciden portada, saltos y margenes.
#
# Requisitos: pandoc, y Chrome o Chromium para el PDF.
#
#   sudo apt-get install pandoc chromium        # Debian y derivados
#   brew install pandoc --cask chromium         # macOS
#   winget install JohnMacFarlane.Pandoc        # Windows (o usa build-docs.ps1)
#
# Uso:  ./docs/build-docs.sh

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS="$RAIZ/docs"
SALIDA="$DOCS/out"
PLANT="$DOCS/plantilla"

if ! command -v pandoc > /dev/null; then
    echo "Falta pandoc. Instalalo y vuelve a ejecutar." >&2
    exit 1
fi

# --- Navegador para el PDF ----------------------------------------------------
NAVEGADOR=""
for candidato in "${CHROME:-}" chromium chromium-browser google-chrome \
                 google-chrome-stable microsoft-edge \
                 "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"; do
    [ -n "$candidato" ] || continue
    if command -v "$candidato" > /dev/null 2>&1 || [ -x "$candidato" ]; then
        NAVEGADOR="$candidato"
        break
    fi
done
[ -n "$NAVEGADOR" ] || echo "Aviso: sin Chrome ni Chromium no se generan los PDF." >&2

mkdir -p "$SALIDA"

VERSION="$(grep -m1 'val appVersionName' "$RAIZ/composeApp/build.gradle.kts" \
    | sed 's/.*"\(.*\)".*/\1/')"
FECHA="$(LC_TIME=es_ES.UTF-8 date '+%-d de %B de %Y' 2> /dev/null || date '+%Y-%m-%d')"

PERFIL="$(mktemp -d)"
trap 'rm -rf "$PERFIL"' EXIT

# --- Documentos ---------------------------------------------------------------
# nombre-de-salida | titulo | ficheros de entrada
DOCUMENTOS=(
  "NexaPDF-manual-usuario|Manual de usuario|$DOCS/MANUAL-USUARIO.md"
  "NexaPDF-manual-tecnico|Manual tecnico|$DOCS/ARCHITECTURE.md $DOCS/INSTALL.md $DOCS/adr/0001-stack.md $DOCS/adr/0002-sin-backend.md $DOCS/adr/0003-portabilidad.md $DOCS/adr/0004-conversion-ofimatica.md"
  "NexaPDF-guia-publicacion|Guia de publicacion|$DOCS/google_play/README.md"
  "NexaPDF-privacidad|Politica de privacidad|$DOCS/PRIVACIDAD.md"
)

for entrada in "${DOCUMENTOS[@]}"; do
    IFS='|' read -r nombre titulo ficheros <<< "$entrada"
    # shellcheck disable=SC2206
    entradas=($ficheros)

    echo "==> $titulo"

    pandoc --from=gfm --standalone --toc --toc-depth=3 \
        --template="$PLANT/documento.html" \
        --css="$PLANT/documento.css" \
        --embed-resources \
        --metadata "documento=$titulo" \
        --metadata "version=$VERSION" \
        --metadata "author=Brais Castineiras Galdo — Ghato Studio" \
        --metadata "date=$FECHA" \
        --metadata "web=github.com/braisgaldo/NexaPDF" \
        --metadata "lang=es" \
        --metadata "toc-title=Contenido" \
        --metadata "title=NexaPDF $VERSION - $titulo" \
        -o "$SALIDA/$nombre.html" \
        "${entradas[@]}"
    echo "    $nombre.html"

    # El DOCX no usa la plantilla HTML: Word trae sus propios estilos.
    pandoc --from=gfm --standalone --toc --toc-depth=3 \
        --metadata "title=NexaPDF $VERSION - $titulo" \
        --metadata "author=Brais Castineiras Galdo — Ghato Studio" \
        --metadata "date=$FECHA" \
        --metadata "lang=es" \
        -o "$SALIDA/$nombre.docx" \
        "${entradas[@]}"
    echo "    $nombre.docx"

    if [ -n "$NAVEGADOR" ]; then
        rm -f "$SALIDA/$nombre.pdf"
        "$NAVEGADOR" --headless=new --disable-gpu --no-sandbox \
            --user-data-dir="$PERFIL" \
            --no-pdf-header-footer \
            --print-to-pdf="$SALIDA/$nombre.pdf" \
            "file://$SALIDA/$nombre.html" 2> /dev/null
        [ -s "$SALIDA/$nombre.pdf" ] || { echo "No se genero $nombre.pdf" >&2; exit 1; }
        echo "    $nombre.pdf"
    fi
done

echo
echo "Documentacion en $SALIDA"
ls -1sh "$SALIDA"
