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
- [ ] **Documentación en vivo + política en CLAUDE.md.** Decidir CUÁLES docs son
  "documentación en vivo" (se actualizan conforme crece el proyecto) y **declarar en
  CLAUDE.md la regla** de mantenerlos al día al final de cada sesión que los afecte,
  para evitar doc desactualizada tras cada sesión.
- [ ] **Actualizar contenido de los DRIFT** (mover ≠ actualizar; esto es lo segundo):
  `datos-room/definicion-tablas-room-v1.md` (dice v5, real v12 + entidades nuevas),
  `producto/nucleo-dominio-autonomia.md` (modelo `SleepLog`→`SleepNight`),
  `producto/estado-actual-mvp.md`, `dominio/mapa-flujos-estado-actual-2026-05-24.md`,
  `producto/plan-maestro-roadmap.md`, `frontend/vocal_mapa_componentes_*`.
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

Salida de `dev.sh lint` tras el fix de desugaring: 0 errores, 34 warnings, 13 hints.

- [ ] `ModifierParameter` (×24 en `DashboardIcons.kt`) — el `Modifier` opcional
  debería tener default `Modifier`. Convención Compose.
- [ ] `UnusedResources` — `ic_spiral.xml` no se usa. Borrar o cablear.
- [ ] `UseKtx` (×4 en `AutonomiaRepository.kt`) — usar extensión KTX
  `SharedPreferences.edit`.
- [ ] `AutoboxingStateCreation` (13 hints) — preferir `mutableIntStateOf` sobre
  `mutableStateOf` para Int (micro-perf) en `ActivityValueInputDialog`,
  `AnchorConfigScreen`, `AnchorEditorForm`, `SleepConfigScreen`.
- [ ] `GradleDependency` — versiones más nuevas disponibles (compileSdk 37,
  `androidx.test.ext:junit` 1.3.0, `androidx.test:runner` 1.7.0). Evaluar bump.
- [ ] `ObsoleteSdkInt` — carpeta `mipmap-anydpi-v26` innecesaria (minSdk ya es 26);
  fusionar en `mipmap-anydpi`.
- [ ] `UseOfNonLambdaOffsetOverload` (`DashboardScreen.kt:190`) y
  `OldTargetApi` (`build.gradle.kts:14`) — revisar.

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
