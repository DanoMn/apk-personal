# Entorno de verificación autónomo

Este documento explica el entorno que permite al agente **probar la app de
verdad** —prenderla, abrirla, ver si crashea, leer los logs— sin que vos tengas
que abrir el Nox o enchufar el celular y copiar logs a mano.

> Esto es la **herramienta** (cómo correr cada cosa). La **metodología** —qué capas
> son obligatorias, qué bloquea, quién juzga cada una— vive en
> [`verificacion-por-capas.md`](./verificacion-por-capas.md), que es el contrato.

## Por qué existe esto (lo importante)

Trabajamos con SDD + Strict TDD. El problema: el **mismo modelo de IA** escribe el
spec, el test, el código y corre el Verify. Es el alumno corrigiendo su propio
examen. Un `gradlew test` en verde prueba que la *lógica que el modelo se imaginó*
funciona — **no** prueba que la app corra. El propio `CLAUDE.md` ya lo advierte: un
esquema de base de datos mal migrado pasa los tests en verde y recién crashea en el
dispositivo.

La única defensa real contra eso es una **verdad externa** que el modelo no escribió:
un Android real ejecutando la app. Este entorno es esa verdad externa. Convierte el
Verify de *"el modelo dice que está bien"* en *"el dispositivo dice que está bien"*.

## Qué se instaló (una sola vez)

- **cmdline-tools** del SDK en `D:\Android-Studio\cmdline-tools\latest`
  (herramientas `sdkmanager` / `avdmanager`).
- **Imagen de Android 36**, `google_apis;x86_64` — coincide con el `targetSdk = 36`
  del proyecto. Es x86_64 porque el host usa WSL2/Hyper-V y el emulador acelera con
  **WHPX** (no con HAXM).
- **Dispositivo virtual (AVD)** llamado `vocal_api36` (perfil Pixel 6).

Los scripts de instalación quedaron versionados en `scripts/dev/_bootstrap-sdk.ps1`
y `scripts/dev/_bootstrap-avd.ps1` por si hay que rehacerlo en otra máquina.

## Cómo se usa (desde WSL)

Todo pasa por un único comando: `scripts/dev/dev.sh <verbo>`.

| Comando | Qué hace |
|---|---|
| `dev.sh doctor` | Diagnóstico: adb, emulador, aceleración, AVDs, dispositivos. |
| `dev.sh emu-start` | Prende el emulador (sin ventana) y espera el boot. |
| `dev.sh emu-start -window` | Igual, pero con ventana visible (si querés mirarlo vos). |
| `dev.sh emu-stop` | Apaga el emulador. |
| `dev.sh emu-status` | Muestra dispositivos y si terminó de bootear. |
| `dev.sh build` | Compila el APK debug. |
| `dev.sh install` | Instala el APK (agregá `-clean` para wipe + install limpio). |
| `dev.sh launch` | Abre la pantalla principal. |
| `dev.sh grant` | Concede el **acceso de uso** (usage stats) a la app. |
| `dev.sh stop-app` | Fuerza el cierre de la app. |
| `dev.sh logcat [N]` | Vuelca las últimas N líneas de log de la app (default 200). |
| `dev.sh logcat -clear` | Limpia el buffer de logs. |
| `dev.sh crash` | Vuelca solo el buffer de crashes. |
| `dev.sh lint` | Corre Android Lint (`lintDebug`) y resume los issues por severidad. |
| `dev.sh shot [nombre]` | Captura la pantalla a `scripts/dev/.artifacts/<nombre>.png`. |
| `dev.sh run` | **Todo junto**: build → emu → install → permisos → launch → logs. |
| `dev.sh run -clean` | Idem, pero borrando la app antes (DB limpia). |

### El flujo de cada día

Para verificar un cambio, en general alcanza con uno solo:

```bash
scripts/dev/dev.sh run
```

Eso compila, se asegura de que el emulador esté prendido, instala, concede el
acceso de uso, abre la app, espera unos segundos y te dice si la app quedó **viva**
(con sus últimas líneas de log) o si **crasheó** (con el buffer de crash). Si tocaste
el esquema de Room, usá `run -clean` para evitar peleas de migración (la DB de dev es
descartable, decisión #29).

## Permisos especiales del proyecto

La app pide dos permisos que en un dispositivo nuevo no están concedidos:

1. **Acceso de uso (`PACKAGE_USAGE_STATS`)** — lo usa la telemetría del sueño.
   `dev.sh grant` (y `dev.sh run`) lo concede solo, por `adb`. ✅ Automatizado.
2. **Admin de dispositivo (DeviceAdminReceiver del sueño)** — Android, por
   seguridad, normalmente exige un **tap manual** del usuario para activarlo; no
   siempre se puede forzar por `adb`. ⚠️ Puede requerir intervención manual si una
   prueba depende específicamente de esa función.

## Límites honestos (qué NO resuelve esto)

- El emulador headless **no tiene ojos**: confirma que la app **corre y no crashea** y
  deja leer logs, pero no juzga si la UI "se ve linda". Para eso está `dev.sh shot`,
  que saca una captura que el agente sí puede mirar.
- Sigue siendo un emulador, no tu celular real: el 99% de los bugs aparece igual, pero
  cosas muy de hardware (sensores físicos, cámara real) no se cubren acá.
- Verde de tests + app que bootea sin crashear = altísima confianza, **no** certeza
  absoluta. Para flujos críticos, una pasada manual tuya sigue valiendo.
```
