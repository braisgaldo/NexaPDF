# Capturas del escáner para la ficha de Google Play.
#
# Recorre el escáner en el móvil conectado y guarda las pantallas en
# docs\google_play\capturas. Usa el build de depuración, que en aspecto es
# idéntico al de publicación.
#
# Lo que NO puede hacer este guion, y hay que hacer a mano: la captura del
# visor en vivo con el porcentaje de detección. Para esa hay que apuntar con el
# teléfono a un papel de verdad, y eso no se automatiza desde el ordenador. El
# guion se para ahí y avisa.
#
# Uso:  .\tools\capturar_escaner.ps1

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

# --- Barra de estado limpia --------------------------------------------------
# Sin esto la captura sale con los iconos de notificacion del telefono, que
# ademas de quedar mal pueden delatar datos personales.
Write-Output "Limpiando la barra de estado..."
adb shell settings put global zen_mode 1 | Out-Null
adb shell settings put global sysui_demo_allowed 1 | Out-Null
adb shell settings put secure sysui_demo_allowed 1 | Out-Null
adb shell am broadcast -a com.android.systemui.demo -e command enter | Out-Null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false | Out-Null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000 | Out-Null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false | Out-Null

# --- Material de prueba ------------------------------------------------------
Write-Output "Preparando la foto de prueba..."
python (Join-Path $raiz "tools\generar_fotos_escaner.py") (Join-Path $raiz "build\escaner") | Out-Null
adb push (Join-Path $raiz "build\escaner\mesa-perspectiva.jpg") /sdcard/Pictures/nexapdf-prueba.jpg | Out-Null
adb shell "content call --uri content://media/external/file --method scan_file --arg /sdcard/Pictures/nexapdf-prueba.jpg" | Out-Null

# Se parte SIEMPRE de una instalacion limpia. El guion navega tocando
# coordenadas fijas, asi que depende de que la aplicacion este en el estado que
# el guion supone: con datos de una sesion anterior el tour no sale, todos los
# toques caen corridos y las capturas salen de pantallas que no son. Borrar los
# datos antes es lo unico que hace la captura repetible.
adb shell pm clear $paquete | Out-Null
adb shell pm grant $paquete android.permission.CAMERA | Out-Null

Write-Output "Capturando..."
adb shell am force-stop $paquete | Out-Null
adb shell am start -n $actividad | Out-Null
Start-Sleep -Seconds 7

# El tour sale en el primer arranque: se salta. La espera es larga a proposito:
# al cerrarse deja un destello de color en el borde de la pantalla, y capturando
# antes de tiempo ese marco sale en la imagen de la ficha.
Toca 593 2024 4000
Captura "01-inicio"

# Escanear -> Galeria -> la primera foto -> Hecho.
Toca 540 420 4000
Toca 205 2094 5000
Toca 180 1444 3000
Toca 897 2120 12000
Captura "03-escaneo-revision"

# Guardar como: nombre, modo tarjeta, tamano y texto buscable. La espera vuelve
# a ser larga por lo mismo que arriba: el tour marca esta pantalla al llegar y
# su destello tarda un par de segundos en apagarse.
Toca 860 2078 7000
Captura "09-escaneo-guardar"

# El modo tarjeta activado, que es lo que explica la funcion: al elegir un
# formato aparece la linea que dice para que sirve.
Toca 633 993 1500
Captura "10-escaneo-tarjeta"

Write-Output ""
Write-Output "Falta a mano: 02-escanear.png"
Write-Output "  Abre Escanear, apunta a un papel sobre una superficie de otro color"
Write-Output "  y captura cuando el contorno este verde y ponga 'Listo'."
Write-Output ""
Write-Output "Capturas en $destino"
Write-Output "Revisa que ninguna contenga documentos personales antes de publicarlas."
