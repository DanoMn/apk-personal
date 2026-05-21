$ErrorActionPreference = "Stop"

$toolRoot = Join-Path $env:LOCALAPPDATA "Codex\AndroidTooling"
$jdkRoot = Join-Path $toolRoot "jdk-21"
$sdkRoot = Join-Path $toolRoot "android-sdk"

$jdkHome = Get-ChildItem -Path $jdkRoot -Directory |
    Select-Object -First 1 -ExpandProperty FullName

$env:JAVA_HOME = $jdkHome
$env:ANDROID_HOME = $sdkRoot
$env:PATH = "$jdkHome\bin;$sdkRoot\platform-tools;$env:PATH"

.\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "APK debug generada en:"
Write-Host "app\build\outputs\apk\debug\app-debug.apk"
