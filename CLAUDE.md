# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Las convenciones del proyecto viven en AGENTS.md

Este repo se preparó originalmente para agentes que leen `AGENTS.md`. Ese archivo
es la **única fuente de verdad** para reglas de trabajo, nombres canónicos de UI,
estilo visual, tono de comunicación y el protocolo de meta-prompting. **No
dupliques su contenido acá** — se importa abajo y se lee como parte de este
archivo. Si una regla cambia, se edita en `AGENTS.md`, no acá.

@AGENTS.md

## Fase de desarrollo — sin usuarios reales

El proyecto está en **desarrollo activo**. No hay usuarios reales ni datos de
producción. La base local Room (`autonomia.db`) es **descartable** (decisión #29
de `AGENTS.md`). Trabajá sin miedo a romper datos de dev:

- Para probar tras un cambio de esquema, preferí **instalación limpia**
  (`adb uninstall dev.panopt.autonomia` y luego `adb install ...`) en vez de
  pelear con migraciones sobre la DB vieja. Un crash de migración al hacer
  `install -r` casi siempre es esto: reinstalá limpio y listo.
- Está OK borrar/resetear la DB local; no hay que preservar registros legacy de
  dev ni escribir migraciones defensivas para datos viejos.

Pero esto **NO** te exime de la corrección de migraciones para el eventual
release:

- Las migraciones igual deben quedar correctas. Los `gradlew test` de dominio
  **NO** ejercen migraciones reales de Room — un esquema mal migrado pasa los
  tests en verde y recién crashea en el dispositivo al actualizar.
- Si tocás entidades o migraciones Room, agregá cobertura con
  `MigrationTestHelper`. Patrón conocido a vigilar: los índices de migración
  deben llamarse `index_<tabla>_<col>` (no `idx_*`) para coincidir con los que
  Room genera desde `Index(...)` en las entidades.

## Build & test

App Android (Kotlin + Jetpack Compose + Room), build con Gradle (Kotlin DSL). El
JDK sale del JBR de Android Studio. El repo vive en WSL pero compila a través de
PowerShell de Windows (el Android SDK y el JBR están del lado Windows). Nunca
encadenes con `&&` — usá `;` (regla de `AGENTS.md`).

- **APK debug (WSL → PowerShell):**
  ```powershell
  powershell.exe -Command "$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
  ```
- **Todos los tests unitarios:** reemplazá `assembleDebug` por `test`.
- **Un solo test (clase o método):**
  ```powershell
  powershell.exe -Command "$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat test --tests 'dev.panopt.autonomia.domain.scoring.ScoreEngineTest' --no-daemon"
  ```
  Agregá `.nombreDelMetodo` al `--tests` para correr un solo caso.
- Tests = JUnit 4, dominio puro JVM bajo `app/src/test/java/...`.
- **No corras tests para cambios triviales** (strings, imports, ajustes de layout,
  limpieza de seeds). Solo cuando el cambio toca lógica de negocio o queries Room,
  o cuando se pide explícito (regla de `AGENTS.md`).
- Instalar en dispositivo: `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
  `minSdk 26` / `targetSdk 36`, sin permisos especiales.

## Arquitectura: hechos → dominio → estado → Compose

Local-first. Room guarda **hechos**; el **dominio** convierte hechos en
inferencias/estado; Compose solo renderiza estado y envía acciones. Nunca
calcules scoring ni reglas de negocio en Compose ni en el ViewModel.

Puntos de entrada: `MainActivity.kt` (single-activity + navegación local),
`AutonomiaRepository.kt` (todo el acceso a Room + cierre diario), `app/AppGraph.kt`.

### Modelo de actividades (reglas de superficie completas en AGENTS.md)

Catálogo → config de usuario → hecho diario:
`ActivityDefinitionEntity` → `UserActivityConfigEntity` (Anchor/Support/Task,
targets) → `DailyActivityLogEntity` (hecho diario canónico, status
`Done`/`NotDone`/`Omitted`). `activity_logs` quedó **legacy**: la fuente diaria
canónica del scoring es `daily_activity_logs`.

### Pipeline de scoring (el subsistema más grande)

El motor semanal es dominio puro: no toca Room ni Compose. Flujo:

```
Hechos Room
  → ScoreInputSource / BuildScoreInputUseCase   (recolecta y normaliza hechos semanales)
  → ScoreEngine                                  (orquestador)
      → domain/scoring/*Policy.kt                (fórmulas atómicas: Anchor, Support,
        TaskMomentum, Sobriety, Layer, Special,   Weekly, Visible, Stability, BaseState)
  → ScoreReport
  → DashboardProjection → DashboardScoreReportState
  → Compose (resumen en DashboardScreen / detalle en ScoringScreen "Estado Base")
```

- El contrato matemático está en `docs/arbol-scoring-vocal-v1.md` (fórmulas
  canónicas) y `docs/plan-tecnico-scoring-vocal.md` (plan técnico, estado por
  fases, decisiones). Al tocar scoring, esos docs son el spec.
- La historia semanal es un **cache derivado y versionado**
  (`WeeklyScoreSnapshotEntity` vía `WeeklyScoreSnapshotWriter`), nunca verdad
  primaria — siempre recalculable desde los hechos diarios.
- El cierre diario (`AutonomiaRepository.closeElapsedActivityDays`) materializa
  estados editables del día en hechos históricos; corre con `DailyClosureWorker`
  (WorkManager, medianoche local) y como garantía al abrir la app.
- El esquema Room está en versión 10; las migraciones viven en
  `AutonomiaDatabase.kt`. Agregar o alterar entidades exige una migración nueva
  numerada.

## SDD + memoria

- El proyecto usa Spec-Driven Development. El contexto de proyecto, las
  capacidades de testing y **Strict TDD (enabled)** están cacheados en engram bajo
  `sdd-init/apk-personal`.
- La clave de proyecto en engram es **`apk-personal`** (nunca `digitaliza-server`).
  Toda memoria/observación se guarda ahí.
- Usá el MCP **Context7** para mejores prácticas actuales de Room/Compose/Kotlin
  al escribir código o arquitectura nueva (regla de `AGENTS.md`).
