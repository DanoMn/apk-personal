# Bootstrap one-time: instala la system image y crea un AVD para un nivel de API dado.
# Requiere cmdline-tools ya instalado (_bootstrap-sdk.ps1).
#
# Uso: powershell -ExecutionPolicy Bypass -File scripts\dev\_bootstrap-avd.ps1 -Api 26
#   -Api 26  => piso / minSdk          (caza bugs clase NewApi ejecutando, no solo Lint)
#   -Api 36  => intermedio (default)
#   -Api 37  => techo / targetSdk      (valida el comportamiento runtime de targetSdk 37)
#
# Tambien se puede invocar desde WSL con:  scripts/dev/dev.sh bootstrap -api 37
param([int]$Api = 36)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$Sdk = 'D:\Android-Studio'
$Sdkmanager = Join-Path $Sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
$Avdmanager = Join-Path $Sdk 'cmdline-tools\latest\bin\avdmanager.bat'

# Mapa explicito api -> system image. NO se puede concatenar "android-$Api;google_apis;x86_64":
# API 37 usa el paquete `android-37.0` (con .0) y solo existe la variante `google_apis_ps16k`
# (pagina de 16 KB), no hay `google_apis` pelado. Todas x86_64 = acelerables con WHPX.
$ImageMap = @{
    26 = 'system-images;android-26;google_apis;x86_64'
    36 = 'system-images;android-36;google_apis;x86_64'
    37 = 'system-images;android-37.0;google_apis_ps16k;x86_64'
}
if (-not $ImageMap.ContainsKey($Api)) {
    Write-Output "ERROR: API $Api no soportada. Validas: $($ImageMap.Keys -join ', ')."
    exit 1
}
$Image = $ImageMap[$Api]
$AvdName = "vocal_api$Api"
$Device = 'pixel_6'

Write-Output "== Target: API $Api  |  AVD: $AvdName  |  Image: $Image =="

Write-Output '== Aceptando licencias =='
$y = ('y' + "`n") * 60
$y | & $Sdkmanager --sdk_root="$Sdk" --licenses | Out-Null

Write-Output "== Instalando imagen: $Image (descarga grande) =="
& $Sdkmanager --sdk_root="$Sdk" "$Image"
if ($LASTEXITCODE -ne 0) { Write-Output 'ERROR instalando system image'; exit 1 }

Write-Output "== Creando AVD: $AvdName =="
# Si ya existe, lo borra y recrea limpio.
$existing = & $Avdmanager list avd 2>$null | Select-String $AvdName
if ($existing) {
    Write-Output "AVD $AvdName ya existe, recreando limpio..."
    & $Avdmanager delete avd -n $AvdName | Out-Null
}
$noAnswer = 'no' + "`n"
$noAnswer | & $Avdmanager create avd -n $AvdName -k "$Image" -d $Device --force
if ($LASTEXITCODE -ne 0) { Write-Output 'ERROR creando AVD'; exit 1 }

Write-Output "== AVDs disponibles =="
& (Join-Path $Sdk 'emulator\emulator.exe') -list-avds
Write-Output "BOOTSTRAP-AVD OK (API $Api)"
