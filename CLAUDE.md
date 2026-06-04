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
  dev ni escribir migraciones defensivas para datos viejos. Wipear la DB es
  inofensivo: al reabrir, la app la reconstruye y re-siembra desde el seed.
- **PERO el seed de actividades NO es descartable.** El seed canónico de anclas y
  soportes predeterminados (`data/local/seed/DefaultSeeds.kt`) y los catálogos
  canónicos son **data predeterminada**, no datos de usuario. NO los borres ni los
  vacíes: son la fuente que repuebla la DB tras cada wipe. Están respaldados por la
  documentación (`docs/datos-room/actividades-ancla-predeterminadas-v1.md`,
  `docs/datos-room/preset-soportes-v1.md`, `docs/datos-room/presets-actividades-v1.md`) y deben
  preservarse (`AGENTS.md` #21). "Romper datos de dev" = filas de usuario en la DB;
  nunca = el seed/catálogo canónico.

### Migraciones Room en esta fase — Camino A (regla única, sin ambigüedad)

**En desarrollo NO se escriben ni se testean migraciones Room. Punto.**

- Cambiaste entidades/esquema → **reinstalación limpia** (`dev.sh run -clean` o
  `adb uninstall` + `adb install`). La DB se reconstruye desde el esquema actual con
  todas las tablas y se re-siembra. No hay nada que migrar porque no hay datos que salvar.
- **NO** agregues objetos `Migration`, **NO** bumpees `version` "para crear tablas", **NO**
  escribas tests con `MigrationTestHelper`, **NO** persigas esquemas históricos (`N.json`).
  La red de seguridad ya está puesta: `fallbackToDestructiveMigration(dropAllTables = true)`
  en `AutonomiaDatabase.kt` recrea la DB ante cualquier desajuste. Una tabla nueva declarada
  como `@Entity` aparece sola en la próxima instalación limpia.
- Los objetos `MIGRATION_*` que ya existen son **legacy**: no los toques ni los repliques.
  Dev nunca ejercita el camino de upgrade (siempre reinstalamos limpio).

**El interruptor de release (todavía NO):** el día que se decida lanzar, se fija un esquema
**baseline** limpio y *desde ahí* sí se escriben migraciones para cada cambio (con su
`MigrationTestHelper`, índices `index_<tabla>_<col>`, etc.). Eso se activa una sola vez, cuando
toque — **no antes**. Si una tarea SDD propone "arreglar/crear migraciones" en esta fase,
**está mal**: frená y aplicá Camino A.

## Build & test

App Android (Kotlin + Jetpack Compose + Room), build con Gradle (Kotlin DSL). El
JDK sale del JBR de Android Studio. El repo vive en WSL pero compila a través de
PowerShell de Windows (el Android SDK y el JBR están del lado Windows). Nunca
encadenes con `&&` — usá `;` (regla de `AGENTS.md`).

**Compilá para verificar/depurar.** Para cambios **NO triviales** (entidades o
migraciones Room, lógica de negocio, queries, wiring/DI), el agente **DEBE** compilar
con el comando de abajo y leer los errores — no asumir que compila. Esto **anula**
cualquier regla global tipo "never build after changes": en este proyecto el build es
la herramienta de verificación y depuración. (Para cambios triviales —strings, imports,
ajustes de layout— no hace falta compilar.)

- **APK debug (WSL → PowerShell):**
  ```powershell
  powershell.exe -Command "$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
  ```
  > ⚠ **Gotcha al invocarlo desde la shell del agente (bash de WSL):** escapá el `$`
  > como `\$env:JAVA_HOME`. Si no, bash expande `$env` (vacío) y el build falla con
  > `JAVA_HOME is not set`. Tipeado directo en una terminal PowerShell va **sin** escapar.
- **Todos los tests unitarios:** reemplazá `assembleDebug` por `test`.
- **Un solo test (clase o método) — usá `testDebugUnitTest`, NO `test`:**
  ```powershell
  powershell.exe -Command "$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat testDebugUnitTest --tests 'dev.panopt.autonomia.domain.scoring.ScoreEngineTest' --no-daemon"
  ```
  Agregá `.nombreDelMetodo` al `--tests` para correr un solo caso. **Ojo:** la tarea
  agregada `test` NO acepta `--tests` (falla con "Unknown command-line option '--tests'");
  el filtro `--tests` solo funciona sobre la tarea concreta `testDebugUnitTest`. También
  acepta comodín: `--tests 'dev.panopt.autonomia.platform.telemetry.*'`.
- Tests = JUnit 4, dominio puro JVM bajo `app/src/test/java/...`.
- **No corras tests para cambios triviales** (strings, imports, ajustes de layout,
  limpieza de seeds). Solo cuando el cambio toca lógica de negocio o queries Room,
  o cuando se pide explícito (regla de `AGENTS.md`).
- Instalar en dispositivo: `adb install -r app\build\outputs\apk\debug\app-debug.apk`.
  `minSdk 26` / `targetSdk 36`, sin permisos especiales.

## Contrato de Spec (obligatorio antes de planificar/lanzar SDD)

Antes de autorizar o lanzar CUALQUIER ejecución SDD, la IA **DEBE** evaluar la spec
contra `meta/guias/contrato-de-spec.md` y aplicar su compuerta (sección 9). Si la
spec no pasa el checklist —sobre las secciones que apliquen al cambio—, **FRENÁ y pedí
los detalles faltantes**; no empieces a codificar. La fricción en esta etapa es
deliberada. (Cambios triviales —ver sección 0— no requieren spec.)

## Contrato de verificación (obligatorio antes de SDD)

Antes de iniciar CUALQUIER fase SDD que toque código, la IA **DEBE** cargar
`meta/guias/verificacion-por-capas.md` y tratar sus capas como **gates
obligatorios**. Ninguna capa es opcional. Un cambio NO está "terminado" si una capa
aplicable quedó en rojo. Saltear una capa = incumplir el contrato.

- El entorno para correr esas capas (emulador, build, lint, logs, captura) vive en
  `scripts/dev/` — se maneja con `scripts/dev/dev.sh <verbo>`. Guía de uso:
  `meta/guias/entorno-verificacion.md`.
- Para cambios **triviales** (strings, imports, layout, limpieza de seeds) NO aplica
  la escalera completa — vale lo que dice **Build & test** arriba.

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

- El contrato matemático está en `docs/scoring/arbol-scoring-v1.md` (fórmulas
  canónicas) y `docs/scoring/plan-tecnico-scoring.md` (plan técnico, estado por
  fases, decisiones). Al tocar scoring, esos docs son el spec.
- La historia semanal es un **cache derivado y versionado**
  (`WeeklyScoreSnapshotEntity` vía `WeeklyScoreSnapshotWriter`), nunca verdad
  primaria — siempre recalculable desde los hechos diarios.
- El cierre diario (`AutonomiaRepository.closeElapsedActivityDays`) materializa
  estados editables del día en hechos históricos; corre con `DailyClosureWorker`
  (WorkManager, medianoche local) y como garantía al abrir la app.
- El esquema Room está en versión 12; las migraciones viven en
  `AutonomiaDatabase.kt`. Agregar o alterar entidades exige una migración nueva
  numerada.

## Mapa de documentación

La doc de **producto/contrato** vive en `docs/` (organizada por tema); la doc de
**proceso de agente** vive en `meta/`. Dónde leer según qué necesites:

- `docs/producto/` — filosofía, visión, estado MVP, roadmap, tono.
- `docs/frontend/` — diseño visual, UX canónica (anclas/soportes), `prototipo/` (HTML vivo).
- `docs/dominio/` — modelo conceptual, capas, configuración canónica, frases, flujos.
- `docs/datos-room/` — esquema Room, seeds/presets canónicos.
- `docs/scoring/` — contrato matemático y plan técnico del scoring.
- `docs/sueno/` — feature Sueño y telemetría.
- `docs/auditorias/` — auditorías técnicas vigentes.
- `docs/old/` — **archivado/deprecated**: NO usar como contrato vigente.
- `meta/guias/` — guías de proceso (verificación por capas, contrato de spec, entorno).
- `meta/handoffs/` — handoffs de sesión. `meta/pendientes.md` — backlog vivo.

## Documentación en vivo (mantenerla al día — parte de "terminado")

Parte de la doc es **contrato/estado vivo** (sigue al código); otra parte son **fotos
de un momento** (no se tocan). Cada doc vivo lleva un header `> **Estado: vivo**`.

- **VIVOS** (se actualizan cuando cambia el código que describen): `docs/producto/`,
  `docs/dominio/`, `docs/datos-room/`, `docs/scoring/`, `docs/frontend/`,
  `docs/sueno/`. (Excepción: `producto/research-apps-similares.md` es material de
  apoyo, no vive.)
- **CONGELADOS** (NO se actualizan; si la realidad cambia, se escribe uno nuevo):
  `docs/auditorias/` (auditorías fechadas), `meta/handoffs/`, `docs/old/`. Editar un
  handoff o una auditoría destruye su valor de registro.

**Regla:** si un cambio altera lo que un doc VIVO describe (esquema Room, fórmulas de
scoring, reglas de superficie, pantallas, flujos), actualizar ese doc es parte de la
Definición de Terminado —igual que las capas de `meta/guias/verificacion-por-capas.md`—.
Un cambio NO está terminado si dejó un doc vivo contradiciendo al código. Es
proporcional: solo el/los doc(s) afectado(s); los cambios triviales no tocan docs.

## SDD + memoria

- El proyecto usa Spec-Driven Development. El contexto de proyecto, las
  capacidades de testing y **Strict TDD (enabled)** están cacheados en engram bajo
  `sdd-init/apk-personal`.
- La clave de proyecto en engram es **`apk-personal`** (nunca `digitaliza-server`).
  Toda memoria/observación se guarda ahí.
- Usá el MCP **Context7** para mejores prácticas actuales de Room/Compose/Kotlin
  al escribir código o arquitectura nueva (regla de `AGENTS.md`).
