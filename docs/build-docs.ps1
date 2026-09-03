# Genera la documentacion de NexaPDF en HTML, PDF y DOCX (version para Windows).
#
# Equivalente a build-docs.sh. El HTML y el PDF salen de la misma plantilla
# (plantilla/documento.html + documento.css), asi que lo que se ve en pantalla y
# lo que se imprime son el mismo documento: el PDF se obtiene imprimiendo el
# HTML con un navegador sin interfaz, no con un motor distinto que interprete el
# Markdown por su cuenta.
#
# Requisitos: pandoc en el PATH (o en $env:PANDOC) y Edge o Chrome instalados.
#
# Uso:  .\docs\build-docs.ps1

$ErrorActionPreference = "Stop"

$raiz   = Split-Path -Parent $PSScriptRoot
$docs   = Join-Path $raiz "docs"
$salida = Join-Path $docs "out"
$plant  = Join-Path $docs "plantilla"

# --- Herramientas -------------------------------------------------------------

$pandoc = if ($env:PANDOC) { $env:PANDOC } else { (Get-Command pandoc -ErrorAction SilentlyContinue).Source }
if (-not $pandoc) { throw "Falta pandoc. Instalalo o define `$env:PANDOC con su ruta." }

$navegador = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $navegador) { Write-Warning "Sin Edge ni Chrome no se generan los PDF." }

New-Item -ItemType Directory -Force -Path $salida | Out-Null

$version = (Select-String -Path (Join-Path $raiz "composeApp\build.gradle.kts") `
    -Pattern 'val appVersionName = "(.+)"' | Select-Object -First 1).Matches[0].Groups[1].Value
$fecha = (Get-Date).ToString("d 'de' MMMM 'de' yyyy",
    [Globalization.CultureInfo]::GetCultureInfo("es-ES"))

# --- Documentos ---------------------------------------------------------------

$documentos = @(
    @{ nombre = "NexaPDF-manual-usuario"; titulo = "Manual de usuario";
       ficheros = @("MANUAL-USUARIO.md") },
    @{ nombre = "NexaPDF-manual-tecnico"; titulo = "Manual tecnico";
       ficheros = @("ARCHITECTURE.md", "INSTALL.md", "adr\0001-stack.md",
                    "adr\0002-sin-backend.md", "adr\0003-portabilidad.md",
                    "adr\0004-conversion-ofimatica.md") },
    @{ nombre = "NexaPDF-guia-publicacion"; titulo = "Guia de publicacion";
       ficheros = @("google_play\README.md") },
    @{ nombre = "NexaPDF-privacidad"; titulo = "Politica de privacidad";
       ficheros = @("PRIVACIDAD.md") }
)

$perfilTemporal = Join-Path $env:TEMP "nexapdf-docs-perfil"

foreach ($doc in $documentos) {
    Write-Output "==> $($doc.titulo)"
    # El @() es obligatorio: con un unico fichero la tuberia devuelve una
    # cadena, y al hacer splat de una cadena PowerShell la reparte en trozos
    # (pandoc acababa recibiendo "C:" como nombre de fichero).
    $entradas = @($doc.ficheros | ForEach-Object { Join-Path $docs $_ })

    $comunes = @(
        "--from=gfm",
        "--standalone",
        "--toc", "--toc-depth=3",
        "--template=$plant\documento.html",
        "--metadata", "documento=$($doc.titulo)",
        "--metadata", "version=$version",
        "--metadata", "author=Brais Castineiras Galdo — Ghato Studio",
        "--metadata", "date=$fecha",
        "--metadata", "web=github.com/braisgaldo/NexaPDF",
        "--metadata", "lang=es",
        "--metadata", "toc-title=Contenido",
        "--metadata", "title=NexaPDF $version - $($doc.titulo)"
    )

    $ficheroHtml = Join-Path $salida "$($doc.nombre).html"
    & $pandoc @comunes --css="$plant\documento.css" --embed-resources `
        -o $ficheroHtml @entradas
    Write-Output "    $($doc.nombre).html"

    # El DOCX no usa la plantilla HTML: Word tiene sus propios estilos.
    & $pandoc "--from=gfm" "--standalone" "--toc" "--toc-depth=3" `
        "--metadata" "title=NexaPDF $version - $($doc.titulo)" `
        "--metadata" "author=Brais Castineiras Galdo — Ghato Studio" `
        "--metadata" "date=$fecha" "--metadata" "lang=es" `
        -o (Join-Path $salida "$($doc.nombre).docx") @entradas
    Write-Output "    $($doc.nombre).docx"

    if ($navegador) {
        $ficheroPdf = Join-Path $salida "$($doc.nombre).pdf"
        Remove-Item $ficheroPdf -ErrorAction SilentlyContinue
        $uri = "file:///" + ($ficheroHtml -replace '\\', '/' -replace ' ', '%20')
        $proceso = Start-Process $navegador -Wait -PassThru -NoNewWindow -ArgumentList @(
            "--headless=new", "--disable-gpu", "--no-sandbox",
            "--user-data-dir=$perfilTemporal",
            "--no-pdf-header-footer",
            "--print-to-pdf=$ficheroPdf",
            $uri
        )
        if ($proceso.ExitCode -ne 0 -or -not (Test-Path $ficheroPdf)) {
            throw "El navegador no genero $($doc.nombre).pdf (codigo $($proceso.ExitCode))."
        }
        Write-Output "    $($doc.nombre).pdf"
    }
}

Remove-Item $perfilTemporal -Recurse -Force -ErrorAction SilentlyContinue

Write-Output ""
Write-Output "Documentacion en $salida"
Get-ChildItem $salida | Sort-Object Name |
    Select-Object Name, @{ n = "kB"; e = { [math]::Round($_.Length / 1KB) } } |
    Format-Table -AutoSize | Out-String | Write-Output
