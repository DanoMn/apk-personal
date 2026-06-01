# Pendientes — backlog vivo

Backlog de cosas que fueron quedando por el camino y que se tratan **más adelante**.
No es un plan ni un contrato: es memoria para no perder hilos. Cuando un ítem se
cierra, se marca `[x]` o se mueve a su doc/commit correspondiente.

> Última actualización: 2026-06-01

## Estructura / orden del repo

- [x] **Reorden de `docs/` por tema (HECHO 2026-06-01).** Carpetas `producto/`,
  `frontend/`, `dominio/`, `datos-room/`, `scoring/`, `sueno/`, `auditorias/`;
  deprecated archivados a `old/`; handoffs movidos a `meta/handoffs/`; referencias
  actualizadas en CLAUDE.md/AGENTS.md/docs vivos; ruta rota corregida; v10→v12; mapa
  de docs en CLAUDE.md; README reescrito. Backup en
  `/mnt/d/APK-Personal-backups/docs-backup-2026-06-01.tar.gz`. Plan:
  `meta/plan-reorg-docs.md`.
- [x] **Documentación en vivo + política en CLAUDE.md (HECHO 2026-06-01).** Split
  vivo/congelado por carpeta (vivos: producto/dominio/datos-room/scoring/frontend/sueno;
  congelados: auditorias/handoffs/old). Política declarada en CLAUDE.md ("Documentación
  en vivo"): actualizar un doc vivo es parte de "terminado". Header `> **Estado: vivo**`
  estampado en los 23 docs vivos.
- [x] **Actualizar contenido de los DRIFT (HECHO 2026-06-01).** 6 docs sincronizados
  con el código vía 6 Sonnet en paralelo + verificación: `definicion-tablas-room-v1`
  (v12, 22 entidades, historial de migraciones), `nucleo-dominio-autonomia`,
  `estado-actual-mvp`, `mapa-flujos-estado-actual-2026-05-24`, `plan-maestro-roadmap`,
  `vocal_mapa_componentes`. Plan: `meta/plan-update-drift-docs.md`.
- [ ] **AGENTS.md cita `especificacion-actividades-sobriedad-v1.md`** como fuente de
  verdad, pero ese doc está en `old/` (deprecated). Decidir si se deja de citar o se
  reemplaza por los docs que lo absorbieron (`nucleo-dominio` + `configuracion-canonica`).

## Verificación / testing

- [ ] **Reforzar el enforcement real de `sdd-verify` (de "blando" a "de fierro").**
  El contrato ya está declarado (`meta/guias/verificacion-por-capas.md` + cache
  engram `sdd/apk-personal/testing-capabilities` + CLAUDE.md). Hoy el enganche es
  **blando**: depende de que el agente de verify lea el contrato y corra `dev.sh`.
  - **Dónde va (importante): TODO local de este proyecto, NUNCA global.** El skill
    `sdd-verify` es el motor; lee QUÉ ejecutar de fuentes locales: el cache
    `sdd/apk-personal/testing-capabilities` (lo que produce el `sdd-init` de este
    proyecto) o `openspec/config.yaml` (que acá no usamos). No se toca el skill
    global → no afecta otros proyectos.
  - **Cómo endurecerlo:** el motor solo tiene dos ranuras que ejecuta y bloquea por
    exit code (`test_command`, `build_command`); NO tiene ranura nativa para Lint ni
    para arrancar la app. Solución: crear un **script-compuerta** propio (ej. verbo
    `dev.sh verify-gate`) que corra build → lint → run → logs y devuelva **≠ 0 si
    cualquiera falla**, y apuntar `build_command`/`test_command` del cache del init a
    ese script. Así el motor lo corre sí o sí y bloquea por su exit code.
  - **Diseño del `verify-gate` (DEFINIDO):** corre las capas **en paralelo** — lanza
    el boot del emulador en background al inicio, en simultáneo con build+lint+tests,
    para esconder el costo del boot detrás del build (no hace falta emulador
    caliente). Política de boot: **default arrancá; salteá solo si el cambio es
    inequívocamente dominio puro; ante la duda, arrancá.** Detalle en
    `meta/guias/verificacion-por-capas.md` (sección "Ejecución").
- [ ] **Emulador API 26 (minSdk).** El AVD actual es API 36, no prueba el
  comportamiento en dispositivos viejos. Un AVD API 26 cazaría la clase de bug
  `NewApi` también ejecutando, no solo con Lint. Agregar como segundo target del
  entorno.
- [ ] **Device-admin del sueño en emulador.** El `SleepDeviceAdminReceiver` puede
  exigir un tap manual; no se pudo forzar 100% por adb. Investigar si hay vía
  (`dpm set-active-admin` u otra) para activarlo en el emulador y automatizar las
  pruebas de la feature de sueño.

## Limpieza de Lint (no urgente — Warnings/Hints, ninguno bloquea)

Estado tras la pasada de limpieza (2026-06-01): **de 34 warnings + 13 hints a
0 warnings + 0 hints**, verificado con `dev.sh lint` (BUILD SUCCESSFUL). Sección
cerrada. (Quedan 2 warnings de *compile* preexistentes —`SleepLog` deprecated y
`unsafeCheckOpNoThrow` deprecated— que NO son de lint y son ajenos a esta pasada.)

- [x] `ModifierParameter` (×23 — 22 `DashboardIcons.kt` + 1 `SobrietyConfigScreen.kt`,
  HECHO 2026-06-01). Default `Modifier.size(N.dp)` → `Modifier`, con el tamaño movido
  a `Canvas(modifier = modifier.size(N.dp))`. Equivalente verificado: todos los callers
  pasan size (gana por `enforceIncoming`), ningún camino quedaba sin tamaño.
- [x] `UnusedResources` (HECHO 2026-06-01) — borrado `drawable/ic_spiral.xml` (huérfano).
  OJO: `ic_spiral_foreground.xml` SÍ se usa (es el foreground del launcher); ese se queda.
- [x] `UseKtx` (×4 en `AutonomiaRepository.kt`, HECHO 2026-06-01) — `prefs.edit { }`
  (KTX defaultea a `apply()`, equivalente). Extensión de `core-ktx` transitiva (lint
  solo la sugiere si es resoluble). Si se quiere declarar `core-ktx` explícito = Balde B.
- [x] `AutoboxingStateCreation` (13 hints, HECHO 2026-06-01) — `mutableIntStateOf` en
  `ActivityValueInputDialog`, `AnchorConfigScreen`, `AnchorEditorForm`, `SleepConfigScreen`.
  Los `Int?` nullable (commitmentDurationMonths) NO se tocan (no pueden ir en IntState).
- [x] `UseOfNonLambdaOffsetOverload` (`DashboardScreen.kt:190`, HECHO 2026-06-01) —
  overload lambda `offset { IntOffset(drawerOffset.roundToPx(), 0) }` (lee en layout phase;
  el valor es `animateDpAsState`, evita recomposición por frame).
- [x] **`ObsoleteSdkInt` (mipmap-anydpi-v26) — SUPRIMIDO como falso-positivo (2026-06-01).**
  El lint dice que el qualifier `-v26` sobra con minSdk 26, pero son **adaptive-icons con
  `<monochrome>`**: mover a `mipmap-anydpi` sin versión **rompe el resource linking**
  (`AAPT: resource mipmap/ic_launcher not found`) — probado y revertido. El `-v26` es lo
  que genera el template de Studio. Se suprimió con razón documentada en `app/lint.xml`
  (`<ignore path="src/main/res/mipmap-anydpi-v26"/>`), NO se aplicó el merge.
- [x] **Bumps de plataforma a API 37 (HECHO 2026-06-01).** `compileSdk` 36→37, `targetSdk`
  36→37, `androidx.test.ext:junit` 1.2.1→1.3.0, `androidx.test:runner` 1.6.2→1.7.0. El SDK
  Platform 37 se autoinstaló; `dev.sh lint` da BUILD SUCCESSFUL.
  - **CAVEAT abierto:** `targetSdk 37` cambia comportamiento en *runtime* y el único AVD
    es **API 36** — el bump compila pero NO se probó en un device API 37. Cuando se agregue
    el target de emulador nuevo (ver abajo), correr la app con `targetSdk 37` y validar.

## Specs / planeación

- [ ] **Refinar `meta/guias/contrato-de-spec.md`** con las recomendaciones del
  usuario (quedó pendiente de una pasada de ajustes). El doc ya está creado y en uso;
  faltan los retoques que el usuario quiera meter.
- [ ] **Reconsiderar "mínimo privilegio"** como restricción de spec cuando la fase de
  telemetría madure (hoy está afuera a propósito porque la captura de datos está en
  expansión: sueño ahora, avance de proyecto a futuro).

## Commits pendientes

- [ ] **Commitear el trabajo de esta sesión** (sin commitear aún), preferiblemente
  separado por tema:
  - Entorno de verificación (`scripts/dev/*`, `meta/guias/entorno-verificacion.md`)
  - Fix desugaring (`app/build.gradle.kts`)
  - Contrato de verificación (`meta/guias/verificacion-por-capas.md`, sección en
    `CLAUDE.md`, este `meta/pendientes.md`)
- [ ] Hay cambios **previos** a esta sesión sin commitear (no míos):
  `MainActivity.kt`, `platform/telemetry/TelemetryPermission.kt`,
  `ui/sleep/SleepConfigScreen.kt`, y untracked `docs/auditorias/auditoria-permisos-v1.md`,
  `meta/handoffs/handoff-sleep-followups.md`. Revisar antes de cualquier commit para no
  mezclarlos sin querer.
