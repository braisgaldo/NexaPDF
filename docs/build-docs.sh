#!/usr/bin/env bash
# Genera la documentacion de NexaPDF en HTML, PDF y DOCX.
#
# La fuente unica es el Markdown de docs/, que si va en los commits. Los
# binarios NO se commitean: se generan aqui y se adjuntan a la GitHub Release,
# de modo que cada documento corresponde a una version concreta y el
# repositorio sigue siendo ligero de clonar. docs/out/ esta en .gitignore.
#
# Requisitos: pandoc. Para el PDF, wkhtmltopdf o un motor de LaTeX.
#
#   sudo apt-get install pandoc wkhtmltopdf     # Debian y derivados
#   brew install pandoc                          # macOS
#   winget install JohnMacFarlane.Pandoc         # Windows
#
# Uso:  ./docs/build-docs.sh

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS="$RAIZ/docs"
SALIDA="$DOCS/out"

if ! command -v pandoc > /dev/null; then
    echo "Falta pandoc. Instalalo y vuelve a ejecutar." >&2
    exit 1
fi

mkdir -p "$SALIDA"

VERSION="$(grep -m1 'val appVersionName' "$RAIZ/composeApp/build.gradle.kts" \
    | sed 's/.*"\(.*\)".*/\1/')"
FECHA="$(date +%Y-%m-%d)"

# --- Motor de PDF ------------------------------------------------------------
# Se prefiere wkhtmltopdf: no necesita una distribucion de LaTeX de un giga y
# maneja bien el arabe, el chino y el japones de los ejemplos.
MOTOR_PDF=""
if command -v wkhtmltopdf > /dev/null; then
    MOTOR_PDF="--pdf-engine=wkhtmltopdf --pdf-engine-opt=--enable-local-file-access"
elif command -v xelatex > /dev/null; then
    MOTOR_PDF="--pdf-engine=xelatex -V mainfont=DejaVuSerif -V monofont=DejaVuSansMono"
else
    echo "Aviso: sin wkhtmltopdf ni xelatex no se generan los PDF." >&2
fi

# --- Documentos --------------------------------------------------------------
# nombre-de-salida | titulo | ficheros de entrada
DOCUMENTOS=(
  "NexaPDF-manual-usuario|Manual de usuario|$DOCS/MANUAL-USUARIO.md"
  "NexaPDF-manual-tecnico|Manual tecnico|$DOCS/MANUAL-USUARIO.md $DOCS/ARCHITECTURE.md $DOCS/INSTALL.md $DOCS/adr/0001-stack.md $DOCS/adr/0002-sin-backend.md $DOCS/adr/0003-portabilidad.md $DOCS/adr/0004-conversion-ofimatica.md"
  "NexaPDF-guia-publicacion|Guia de publicacion|$DOCS/google_play/README.md"
  "NexaPDF-privacidad|Politica de privacidad|$DOCS/PRIVACIDAD.md"
)

COMUNES=(
  --from=gfm
  --standalone
  --toc
  --toc-depth=3
  --metadata "author=Brais Castineiras Galdo (Ghato Studio)"
  --metadata "date=$FECHA"
  --metadata "lang=es"
)

for entrada in "${DOCUMENTOS[@]}"; do
    IFS='|' read -r nombre titulo ficheros <<< "$entrada"
    # shellcheck disable=SC2086
    entradas=($ficheros)

    echo "==> $titulo"

    pandoc "${COMUNES[@]}" \
        --metadata "title=NexaPDF $VERSION - $titulo" \
        --embed-resources \
        -o "$SALIDA/$nombre.html" \
        "${entradas[@]}"
    echo "    $nombre.html"

    pandoc "${COMUNES[@]}" \
        --metadata "title=NexaPDF $VERSION - $titulo" \
        -o "$SALIDA/$nombre.docx" \
        "${entradas[@]}"
    echo "    $nombre.docx"

    if [ -n "$MOTOR_PDF" ]; then
        # shellcheck disable=SC2086
        pandoc "${COMUNES[@]}" \
            --metadata "title=NexaPDF $VERSION - $titulo" \
            $MOTOR_PDF \
            -V margin-top=20mm -V margin-bottom=20mm \
            -V margin-left=22mm -V margin-right=22mm \
            -o "$SALIDA/$nombre.pdf" \
            "${entradas[@]}"
        echo "    $nombre.pdf"
    fi
done

echo
echo "Documentacion en $SALIDA"
ls -1sh "$SALIDA"
