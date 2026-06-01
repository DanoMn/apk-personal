# Bootstrap one-time: instala system image (Android 36, x86_64) y crea el AVD.
# Requiere cmdline-tools ya instalado (_bootstrap-sdk.ps1).
# Uso: powershell -ExecutionPolicy Bypass -File scripts\dev\_bootstrap-avd.ps1
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$Sdk = 'D:\Android-Studio'
$Sdkmanager = Join-Path $Sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
$Avdmanager = Join-Path $Sdk 'cmdline-tools\latest\bin\avdmanager.bat'

# Imagen acelerable con WHPX (host x86 + Hyper-V/WSL2). google_apis = adb root + APIs Google, sin Play Store.
$Image = 'system-images;android-36;google_apis;x86_64'
$AvdName = 'vocal_api36'
$Device = 'pixel_6'

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
Write-Output 'BOOTSTRAP-AVD OK'
