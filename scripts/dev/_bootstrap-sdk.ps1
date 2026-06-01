# Bootstrap one-time: descarga cmdline-tools y lo deja en SDK\cmdline-tools\latest
# Uso: powershell -ExecutionPolicy Bypass -File scripts\dev\_bootstrap-sdk.ps1
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$Sdk = 'D:\Android-Studio'
$Url = 'https://dl.google.com/android/repository/commandlinetools-win-14742923_latest.zip'
$TmpDir = Join-Path $Sdk '.temp'
$Zip = Join-Path $TmpDir 'cmdline-tools.zip'
$Extract = Join-Path $TmpDir 'cmdline-tools-extract'
$Dest = Join-Path $Sdk 'cmdline-tools\latest'

if (Test-Path (Join-Path $Dest 'bin\sdkmanager.bat')) {
    Write-Output 'ALREADY-INSTALLED'
    exit 0
}

New-Item -ItemType Directory -Force -Path $TmpDir | Out-Null
Write-Output 'Downloading cmdline-tools...'
Invoke-WebRequest -Uri $Url -OutFile $Zip -UseBasicParsing

Write-Output 'Extracting...'
if (Test-Path $Extract) { Remove-Item -Recurse -Force $Extract }
Expand-Archive -Path $Zip -DestinationPath $Extract -Force

# El zip trae una carpeta 'cmdline-tools' con bin/ lib/ adentro.
$Inner = Join-Path $Extract 'cmdline-tools'
New-Item -ItemType Directory -Force -Path (Split-Path $Dest) | Out-Null
if (Test-Path $Dest) { Remove-Item -Recurse -Force $Dest }
Move-Item -Path $Inner -Destination $Dest

if (Test-Path (Join-Path $Dest 'bin\sdkmanager.bat')) {
    Write-Output 'OK: sdkmanager instalado en cmdline-tools\latest'
    Remove-Item -Force $Zip -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force $Extract -ErrorAction SilentlyContinue
} else {
    Write-Output 'ERROR: no se encontro sdkmanager.bat tras la instalacion'
    exit 1
}
