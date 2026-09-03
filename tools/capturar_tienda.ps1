# Capturas para la ficha de Google Play.
#
# Recorre la aplicacion en el movil conectado y guarda las pantallas mas
# representativas en docs/google_play/capturas. Usa el build de depuracion, que
# es identico en aspecto al de publicacion y no obliga a desinstalar el que el
# usuario tenga puesto.
#
# Uso:  .\tools\capturar_tienda.ps1

$ErrorActionPreference = "Stop"

. "C:\Users\brais.castineirasgal\dev-tools\env.ps1"

$raiz = Split-Path -Parent $PSScriptRoot
$destino = Join-Path $raiz "docs\google_play\capturas"
$paquete = "es.ghatostudio.nexapdf.debug"
$actividad = "$paquete/es.ghatostudio.nexapdf.MainActivity"

if (-not (adb devices | Select-String "\tdevice$")) {
    throw "No hay ningun dispositivo conectado."
}
New-Item -ItemType Directory -Force -Path $destino | Out-Null

function Captura([string]$nombre) {
    Start-Sleep -Seconds 2
    $ruta = Join-Path $destino "$nombre.png"
    # exec-out evita que adb traduzca los saltos de linea y corrompa el PNG.
    cmd /c "adb exec-out screencap -p > `"$ruta`""
    Write-Output "  $nombre.png"
}

function Toca([int]$x, [int]$y, [int]$esperaMs = 1500) {
    adb shell input tap $x $y | Out-Null
    Start-Sleep -Milliseconds $esperaMs
}

Write-Output "Preparando un documento de ejemplo..."
python (Join-Path $raiz "tools\generar_ficheros_prueba.py") | Out-Null
adb push (Join-Path $raiz "build\pruebas\NexaPDF prueba largo.pdf") /data/local/tmp/muestra.pdf | Out-Null
adb shell "run-as $paquete sh -c 'mkdir -p files/output && cat /data/local/tmp/muestra.pdf > files/output/Informe.pdf'" | Out-Null

Write-Output "Capturando..."
adb shell am force-stop $paquete | Out-Null
adb shell am start -n $actividad | Out-Null
Start-Sleep -Seconds 6

# El tour sale en el primer arranque: se captura y se salta.
Captura "01-tour-privacidad"
Toca 977 159 2500

Captura "02-inicio"

# Ficheros recientes y visor.
Toca 884 1183 2500
Captura "03-recientes"
Toca 400 300 4000
Captura "04-visor"

Write-Output ""
Write-Output "Capturas en $destino"
Write-Output "Revisa que ninguna contenga documentos personales antes de publicarlas."
