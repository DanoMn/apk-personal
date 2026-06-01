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
- **Imágenes de Android x86_64** (aceleradas con **WHPX**, no HAXM, porque el host usa
  WSL2/Hyper-V). El entorno es **multi-target** para cubrir piso y techo de la app:

  | API | AVD | System image | Para qué |
  |---|---|---|---|
  | 26 | `vocal_api26` | `system-images;android-26;google_apis;x86_64` | **Piso / minSdk** — caza bugs clase `NewApi` ejecutando de verdad, no solo con Lint. |
  | 36 | `vocal_api36` | `system-images;android-36;google_apis;x86_64` | Intermedio (default histórico). |
  | 37 | `vocal_api37` | `system-images;android-37.0;google_apis_ps16k;x86_64` | **Techo / targetSdk 37** — valida el comportamiento runtime del `targetSdk`. (Imagen de 16 KB de página: el paquete es `android-37.0`, no `android-37`.) |

  Todos perfil Pixel 6. Se elige el target con `-api NN` en cualquier comando (ver tabla).

Los scripts de instalación quedaron versionados en `scripts/dev/_bootstrap-sdk.ps1`
y `scripts/dev/_bootstrap-avd.ps1` por si hay que rehacerlo en otra máquina. El bootstrap
de AVD está parametrizado: `scripts/dev/dev.sh bootstrap -api NN` crea/recrea el AVD de ese
nivel (mapa explícito api→imagen porque el naming de 37 es asimétrico).

## Cómo se usa (desde WSL)

Todo pasa por un único comando: `scripts/dev/dev.sh <verbo>`.

> **Target multi-API:** agregá `-api NN` (26 · 36 · 37) a cualquier comando para elegir
> en qué device corre. Sin el flag, usa el default (36). Ej: `dev.sh run -api 37` valida el
> techo (targetSdk 37); `dev.sh run -api 26` valida el piso (minSdk).

| Comando | Qué hace |
|---|---|
| `dev.sh bootstrap -api NN` | Crea/recrea el AVD del API objetivo (26/36/37). Descarga grande la 1ª vez. |
| `dev.sh doctor` | Diagnóstico: adb, emulador, aceleración, AVDs, dispositivos. |
| `dev.sh emu-start [-api NN]` | Prende el emulador (sin ventana) y espera el boot. |
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
