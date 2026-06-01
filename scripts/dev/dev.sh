#!/usr/bin/env bash
# dev.sh — wrapper de WSL para el driver del entorno de verificacion (dev.ps1).
# Reenvia los argumentos a PowerShell del lado Windows y limpia los \r.
#
# Ejemplos:
#   scripts/dev/dev.sh doctor
#   scripts/dev/dev.sh emu-start
#   scripts/dev/dev.sh run            # build + emu + install + permisos + launch + logs
#   scripts/dev/dev.sh run -clean     # idem, pero install limpio (wipe app)
#   scripts/dev/dev.sh logcat 300
#   scripts/dev/dev.sh shot home
#   scripts/dev/dev.sh emu-stop
set -uo pipefail
powershell.exe -NoProfile -ExecutionPolicy Bypass -File 'D:\APK-Personal\scripts\dev\dev.ps1' "$@" 2>&1 | tr -d '\r'
exit "${PIPESTATUS[0]}"
