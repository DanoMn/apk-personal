# Handoff — entorno de verificación, contratos y reorganización de docs

> Foto de sesión (2026-06-01). Snapshot histórico — NO se actualiza; si seguís el
> trabajo, escribí un handoff nuevo. El backlog VIVO está en `meta/pendientes.md`.

## Qué se hizo esta sesión

1. **Entorno de verificación autónomo** (`scripts/dev/`): emulador oficial `vocal_api36`
   (Android 36, WHPX), driver `dev.sh` (build/install/launch/grant/logcat/lint/shot/run).
   Guía: `meta/guias/entorno-verificacion.md`.
2. **Fix real cazado por el entorno**: `NewApi` (datesUntil/Stream.toList API 34+ con
   minSdk 26) → core library desugaring en `app/build.gradle.kts`. Lint 4 errores → 0.
3. **Contratos de proceso** (en `meta/guias/`, apuntados desde CLAUDE.md):
   - `verificacion-por-capas.md` — capas/gates de testing, RACI, política de boot en
     paralelo. Enforcement HOY es blando (pendiente endurecer, ver backlog).
   - `contrato-de-spec.md` — compuerta de calidad pre-SDD (qué debe contener una spec).
4. **Reorganización de `docs/`** por tema (producto/frontend/dominio/datos-room/scoring/
   sueno/auditorias), deprecated a `old/`, handoffs a `meta/handoffs/`. Refs y mapa
   actualizados en CLAUDE.md/AGENTS.md. Plan: `meta/plan-reorg-docs.md`.
5. **Política de documentación en vivo** declarada en CLAUDE.md (vivos vs congelados);
   header `> **Estado: vivo**` en los docs vivos.
6. **Sync de 6 docs en drift** con el código real (Room v12, sleep v2, telemetría) vía
   6 subagentes Sonnet + verificación. Plan: `meta/plan-update-drift-docs.md`.

## Estado de git

- 6 commits en `main` (de `e93822d` a `4c744cd`). No se hizo **push**.
- **Sin commitear (a propósito)**: `MainActivity.kt`, `platform/telemetry/TelemetryPermission.kt`,
  `ui/sleep/SleepConfigScreen.kt` (cambios previos, NO de esta sesión, a revisar);
  `.claude/`; untracked `docs/auditorias/auditoria-permisos-v1.md` y
  `meta/handoffs/handoff-sleep-followups.md` (a revisar); + 1 línea de `meta/pendientes.md`
  (checkbox del drift marcado) y este handoff.
- Backup de docs pre-reorg: `/mnt/d/APK-Personal-backups/docs-backup-2026-06-01.tar.gz`.

## Dónde continuar

- **Backlog vivo (la ruta principal): `meta/pendientes.md`.** Ahí están todos los
  pendientes con contexto. Abiertos hoy:
  - Endurecer enforcement de `sdd-verify` (script-compuerta `dev.sh verify-gate`, en
    paralelo) — LOCAL al proyecto, nunca global. Diseño ya definido en el backlog.
  - Emulador API 26 (minSdk) como segundo target.
  - Device-admin del sueño auto-activable por adb.
  - Limpieza de Lint (34 warnings + 13 hints, ninguno bloquea).
  - Refinar `meta/guias/contrato-de-spec.md` con recomendaciones del usuario.
  - Reconsiderar "mínimo privilegio" cuando la telemetría madure.
  - AGENTS.md cita `especificacion-actividades-sobriedad-v1.md` (deprecated en `old/`):
    decidir si se deja de citar.
  - Commitear (o revisar) los cambios previos no-míos + los 2 untracked flagged.

## Memoria (engram, proyecto `apk-personal`)

Topic keys relevantes: `dev-env/autonomous-verification`, `dev-env/contrato-de-spec`,
`dev-env/docs-reorg`, `dev-env/docs-drift-sync`, `sdd/apk-personal/testing-capabilities`,
`project/pendientes-backlog`, `bugs/newapi-datesuntil-abstinence`.
