# dev.ps1 — Driver del entorno de verificación (Vocal / Autonomia).
# Corre el emulador oficial, compila, instala, abre la app, concede permisos,
# lee logs y saca capturas. Pensado para invocarse desde WSL via scripts/dev/dev.sh.
#
# Uso:  powershell -ExecutionPolicy Bypass -File scripts\dev\dev.ps1 <comando> [args]
#
# Comandos:
#   emu-start [-window]   Prende el AVD (headless por defecto) y espera el boot.
#   emu-stop              Apaga el emulador.
#   emu-status            Muestra dispositivos y si termino de bootear.
#   build                 Compila el APK debug (assembleDebug).
#   install [-clean]      Instala el APK (-clean = desinstala antes, install limpio).
#   launch                Abre la MainActivity.
#   grant                 Concede acceso de uso (usage stats) a la app.
#   stop-app              Fuerza el cierre de la app.
#   logcat [-clear|N]     Vuelca logs de la app (N lineas, default 200) o limpia el buffer.
#   crash                 Vuelca solo el buffer de crashes.
#   lint                  Corre Android Lint (lintDebug) y resume los issues.
#   shot [nombre]         Captura la pantalla a scripts\dev\.artifacts\<nombre>.png
#   run [-clean]          Cadena completa: build -> emu -> install -> grant -> launch -> logs.
#   doctor                Diagnostico del entorno (adb, avd, aceleracion, device).

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Cmd,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Rest
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

# --- Config ---
$Sdk       = 'D:\Android-Studio'
$ProjDir   = 'D:\APK-Personal'
$Adb       = Join-Path $Sdk 'platform-tools\adb.exe'
$Emulator  = Join-Path $Sdk 'emulator\emulator.exe'
$JavaHome  = 'C:\Program Files\Android\Android Studio\jbr'
$AvdName   = 'vocal_api36'
$Pkg       = 'dev.panopt.autonomia'
$Activity  = "$Pkg/.MainActivity"
$AdminCmp  = "$Pkg/.sleep.SleepDeviceAdminReceiver"
$Apk       = Join-Path $ProjDir 'app\build\outputs\apk\debug\app-debug.apk'
$ArtDir    = Join-Path $ProjDir 'scripts\dev\.artifacts'

function Invoke-Adb { & $Adb @args }

function Wait-Boot {
    param([int]$TimeoutSec = 180)
    Write-Output 'Esperando que el dispositivo aparezca...'
    & $Adb wait-for-device
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        $booted = (& $Adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        if ($booted -eq '1') {
            Write-Output 'Boot completo.'
            & $Adb shell input keyevent 82 2>$null | Out-Null  # despierta/desbloquea
            return $true
        }
        Start-Sleep -Seconds 3
    }
    Write-Output "TIMEOUT: el emulador no termino de bootear en $TimeoutSec s."
    return $false
}

function Get-AppPid {
    return (& $Adb shell pidof $Pkg 2>$null | Out-String).Trim()
}

switch ($Cmd) {

    'emu-start' {
        $running = (& $Adb devices | Select-String 'emulator-')
        if ($running) { Write-Output 'Ya hay un emulador corriendo.'; Wait-Boot | Out-Null; break }
        $windowed = $Rest -contains '-window'
        Write-Output "Arrancando AVD '$AvdName'..."
        $emuArgs = @('-avd', $AvdName, '-no-audio', '-no-boot-anim', '-no-snapshot-save', '-gpu', 'swiftshader_indirect')
        if (-not $windowed) { $emuArgs += '-no-window' }
        # Arranca el emulador en proceso aparte (no bloquea).
        Start-Process -FilePath $Emulator -ArgumentList $emuArgs -WindowStyle Hidden
        Wait-Boot | Out-Null
    }

    'emu-stop' {
        Write-Output 'Apagando emulador...'
        & $Adb emu kill 2>$null
        Write-Output 'OK'
    }

    'emu-status' {
        & $Adb devices
        $booted = (& $Adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        Write-Output "boot_completed = '$booted'"
    }

    'build' {
        Write-Output 'Compilando APK debug...'
        $env:JAVA_HOME = $JavaHome
        Set-Location $ProjDir
        & "$ProjDir\gradlew.bat" assembleDebug --no-daemon
        if ($LASTEXITCODE -ne 0) { Write-Output 'BUILD FAILED'; exit 1 }
        Write-Output "APK: $Apk"
    }

    'install' {
        if (-not (Test-Path $Apk)) { Write-Output "No existe el APK ($Apk). Corre 'build' primero."; exit 1 }
        if ($Rest -contains '-clean') {
            Write-Output 'Desinstalando (install limpio)...'
            & $Adb uninstall $Pkg 2>$null | Out-Null
        }
        Write-Output 'Instalando APK...'
        & $Adb install -r $Apk
        if ($LASTEXITCODE -ne 0) { Write-Output 'INSTALL FAILED'; exit 1 }
        Write-Output 'OK'
    }

    'launch' {
        Write-Output "Abriendo $Activity ..."
        & $Adb shell am start -n $Activity
    }

    'grant' {
        Write-Output 'Concediendo acceso de uso (usage stats)...'
        & $Adb shell appops set $Pkg android:get_usage_stats allow
        Write-Output 'OK (acceso de uso). Nota: el device-admin del sueno puede requerir un tap manual.'
    }

    'stop-app' {
        & $Adb shell am force-stop $Pkg
        Write-Output "Detenida $Pkg"
    }

    'logcat' {
        if ($Rest -contains '-clear') { & $Adb logcat -c; Write-Output 'Buffer de logs limpiado.'; break }
        $n = 200
        if ($Rest.Count -gt 0 -and $Rest[0] -match '^\d+$') { $n = [int]$Rest[0] }
        $appPid = Get-AppPid
        if ($appPid) {
            Write-Output "== Logcat (pid=$appPid, ultimas $n lineas) =="
            & $Adb logcat -d -t $n --pid=$appPid
        } else {
            Write-Output "== App no corriendo. Logcat por tag '$Pkg' (ultimas $n) + crashes =="
            & $Adb logcat -d -t $n | Select-String $Pkg, 'AndroidRuntime', 'FATAL'
        }
    }

    'crash' {
        Write-Output '== Buffer de crashes =='
        & $Adb logcat -d -b crash
    }

    'lint' {
        Write-Output 'Corriendo Android Lint (lintDebug)...'
        $env:JAVA_HOME = $JavaHome
        Set-Location $ProjDir
        & "$ProjDir\gradlew.bat" lintDebug --no-daemon
        $xml  = Join-Path $ProjDir 'app\build\reports\lint-results-debug.xml'
        $html = Join-Path $ProjDir 'app\build\reports\lint-results-debug.html'
        if (Test-Path $xml) {
            [xml]$doc = Get-Content $xml
            $issues = @($doc.issues.issue)
            Write-Output ''
            Write-Output '== Resumen Android Lint =='
            if ($issues.Count -eq 0) {
                Write-Output 'Sin issues. Codigo limpio para Lint.'
            } else {
                $issues | Group-Object severity | ForEach-Object { Write-Output ("{0,-8}: {1}" -f $_.Name, $_.Count) }
                Write-Output ''
                Write-Output '== Detalle (severity | id | archivo:linea | mensaje) =='
                foreach ($i in $issues) {
                    $loc  = @($i.location)[0]
                    $file = if ($loc) { Split-Path $loc.file -Leaf } else { '-' }
                    $line = if ($loc -and $loc.line) { $loc.line } else { '-' }
                    Write-Output ("{0,-8} | {1} | {2}:{3} | {4}" -f $i.severity, $i.id, $file, $line, $i.message)
                }
            }
            Write-Output ''
            Write-Output "Reporte HTML completo: $html"
        } else {
            Write-Output "No se encontro el reporte XML ($xml). Mira la salida de gradle arriba."
        }
    }

    'shot' {
        New-Item -ItemType Directory -Force -Path $ArtDir | Out-Null
        $name = if ($Rest.Count -gt 0) { $Rest[0] } else { 'shot' }
        $local = Join-Path $ArtDir "$name.png"
        & $Adb shell screencap -p /sdcard/_shot.png
        & $Adb pull /sdcard/_shot.png $local | Out-Null
        & $Adb shell rm /sdcard/_shot.png 2>$null | Out-Null
        Write-Output "Captura: $local"
    }

    'run' {
        $clean = $Rest -contains '-clean'
        # 1. build
        $env:JAVA_HOME = $JavaHome
        Set-Location $ProjDir
        Write-Output '== [1/5] build =='
        & "$ProjDir\gradlew.bat" assembleDebug --no-daemon
        if ($LASTEXITCODE -ne 0) { Write-Output 'BUILD FAILED'; exit 1 }
        # 2. emu
        Write-Output '== [2/5] emulador =='
        $running = (& $Adb devices | Select-String 'emulator-')
        if (-not $running) {
            $emuArgs = @('-avd', $AvdName, '-no-audio', '-no-boot-anim', '-no-snapshot-save', '-no-window', '-gpu', 'swiftshader_indirect')
            Start-Process -FilePath $Emulator -ArgumentList $emuArgs -WindowStyle Hidden
        }
        if (-not (Wait-Boot)) { exit 1 }
        # 3. install
        Write-Output '== [3/5] install =='
        if ($clean) { & $Adb uninstall $Pkg 2>$null | Out-Null }
        & $Adb install -r $Apk
        if ($LASTEXITCODE -ne 0) { Write-Output 'INSTALL FAILED'; exit 1 }
        # 4. grant + launch
        Write-Output '== [4/5] permisos + launch =='
        & $Adb logcat -c
        & $Adb shell appops set $Pkg android:get_usage_stats allow 2>$null
        & $Adb shell am start -n $Activity
        Start-Sleep -Seconds 6
        # 5. logs + crash
        Write-Output '== [5/5] estado =='
        $appPid = Get-AppPid
        if ($appPid) {
            Write-Output "App VIVA (pid=$appPid). Ultimas lineas:"
            & $Adb logcat -d -t 120 --pid=$appPid
        } else {
            Write-Output 'App NO esta corriendo -> posible crash. Buffer de crash:'
            & $Adb logcat -d -b crash
        }
    }

    'doctor' {
        Write-Output '== adb =='; if (Test-Path $Adb) { 'OK ' + $Adb } else { 'FALTA adb' }
        Write-Output '== emulator =='; if (Test-Path $Emulator) { 'OK ' + $Emulator } else { 'FALTA emulator' }
        Write-Output '== aceleracion =='; & $Emulator -accel-check
        Write-Output '== AVDs =='; & $Emulator -list-avds
        Write-Output '== devices =='; & $Adb devices
    }

    default {
        Write-Output "Comando desconocido: '$Cmd'"
        Write-Output 'Comandos: emu-start emu-stop emu-status build install launch grant stop-app logcat crash lint shot run doctor'
        exit 1
    }
}
