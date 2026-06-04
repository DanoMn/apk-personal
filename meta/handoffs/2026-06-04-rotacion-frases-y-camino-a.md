# Handoff — Rotación de frases ancla + decisión "Camino A" (migraciones)

> **Estado: CONGELADO** (registro de sesión, no se edita). Fecha: 2026-06-04.

## Resumen de la sesión

Se implementó el **motor de rotación de frases ancla** del dashboard (feature
`anchor-phrase-rotation`) vía ciclo SDD completo, y en el camino se destapó y resolvió un
problema de proceso recurrente con las migraciones Room, que derivó en una decisión de forma
de trabajo: **Camino A**.

---

## 1. Lo que se implementó (feature de frases) — COMPLETO y verificado

Motor de rotación fiel a `docs/dominio/frases-ancla.md`, con el principio de "engranajes
chicos" (responsabilidad única), espejo del patrón de `domain/scoring/*Policy.kt`:

- **Enums/modelos** en `Models.kt`: `DayPhase`, `AnchorPhraseSelection`, `AnchorPhraseStateRule`,
  `AnchorPhrasePhaseRule` (+ mappers en `DomainMappers.kt`).
- **`domain/phrase/DayPhasePolicy.kt`** (puro): hora → Dawn (05:00–14:59) / Dusk (15:00–04:59).
- **`data/local/seed/AnchorPhraseSeed.kt`**: 83 frases (helper `phrase(...)`), reglas de
  estado/fase **DERIVADAS** de mapas familia→peso (no escritas a mano). Wired en `ensureSeeded`.
- **`domain/phrase/AnchorPhraseSelector.kt`** (puro): `select()` delgado que compone
  filtros/peso/elección con `Random(seed)` determinístico. Gate de Contemplación a estados altos.
- **`data/phrase/AnchorPhraseResolver.kt` + `AnchorPhraseDataSource.kt`**: coordinador de capa de
  datos (espejo de `WeeklyScoreSnapshotWriter`). Lee el `state` del snapshot semanal actual,
  reusa slot si el estado no cambió (estabilidad por fase), persiste slot + impresión. Wired en
  `AutonomiaRepository` + `runDailyMaintenance`/`onResumed` (`DashboardViewModel`).
- **Integración dashboard**: nuevo flow del slot → lookup puro en `DashboardProjection`; se
  **eliminó el default hardcodeado** (Kierkegaard) de `DashboardState.kt`.
- **Doc viva** `docs/dominio/frases-ancla.md` §18 actualizada (con nota ADR-3).

**Verificación:** `assembleDebug` SUCCESSFUL (DB v12); suite completa **353 tests verde**;
Android Lint limpio (3 warnings preexistentes ajenos); app **VIVA sin crash** en install limpio
en emulador; onboarding renderiza OK. **NO verificado visualmente:** la tarjeta de frase en el
dashboard (está detrás del onboarding; con taps a ciegas es frágil — pendiente eyeball).

SDD archivado en `openspec/changes/archive/2026-06-04-anchor-phrase-rotation/`; 5 specs
fusionados a `openspec/specs/` (day-phase-policy, anchor-phrase-selector, anchor-phrase-resolver,
anchor-phrase-seed, dashboard-integration). El spec de migración quedó EXCLUIDO (ver §2).

---

## 2. Decisión de proceso — "Camino A" para migraciones Room (IMPORTANTE)

**Contexto:** el SDD trató como centro un "bug latente de migración" (las 5 tablas `anchor_phrase*`
están en el esquema pero ninguna migración las crea). Al correr el test de migración en emulador
(verdad externa), se destapó que: (a) la infra de tests de migración del proyecto **nunca corrió en
verde** (falta `11.json`, `12.json` no deserializa con room-testing 2.8.4); (b) el fix de apply
(`MIGRATION_12_13` + bump a v13) estaba **mal ubicado** (v12 y v13 tienen el mismo esquema). Era la
**tercera vez** que las migraciones generaban fricción.

**Causa raíz:** `CLAUDE.md` se contradecía — "DB descartable, reinstalá limpio" Y "las migraciones
deben quedar correctas + MigrationTestHelper". Los agentes seguían la letra sin el criterio.

**Decisión del usuario — Camino A:** en fase dev NO se escriben ni testean migraciones. Cambio de
esquema → reinstalación limpia; `fallbackToDestructiveMigration(dropAllTables=true)` (ya activo)
recrea la DB. Las migraciones reales empiezan recién desde un baseline limpio en release.

**Acciones aplicadas:**
- `AutonomiaDatabase.kt`: revertido a **version 12**; borrado `MIGRATION_12_13`; comentario
  reescrito a Camino A.
- Borrados: `AnchorPhraseMigration12To13Test.kt` (androidTest) y `schemas/13.json`.
- Revertido el `sourceSets.androidTest.assets` en `build.gradle.kts` (era solo para ese test).
- `CLAUDE.md`: reemplazada la contradicción por la regla única "Migraciones Room — Camino A"
  (con el aviso: si una tarea SDD propone arreglar/crear migraciones en dev, está MAL → frená).
- Engram: decisión guardada en `convention/dev-migrations-camino-a` (obs #845).

---

## 3. Trabajo PREEXISTENTE en el working tree (no era de esta sesión)

Al arrancar la sesión, el `git status` ya tenía cambios sin commitear. Se analizaron: son **dos
mejoras chicas, sanas y armónicas** con el feature (137 líneas + 2 archivos nuevos):

1. **Fecha del header del dashboard**: `domain/dashboard/DashboardHeaderDate.kt` (+test) — formatea
   "Jueves 4 de junio" en español, reemplaza el hardcode "Miércoles 20 de mayo" en `TopBar`.
   Tocó `TopBar.kt`, `DashboardScreen.kt`, `MainActivity.kt` (+13).
2. **Refactor de testabilidad del snapshot semanal**: `data/scoring/WeeklySnapshotDataSource.kt`
   (+98 en `WeeklyScoreSnapshotWriter.kt`) — extrae un seam para testear el writer en JVM puro.
   (Mi `AnchorPhraseDataSource` **copia este mismo patrón** → son coherentes.)
3. Menores: `DailyClosureWorker.kt` (+1), `AbstinenceRelapseMaterializationPolicyTest.kt` (+36),
   `docs/scoring/plan-tecnico-scoring.md` (+10).

**No conflictúan con el feature**: todo compila y los 353 tests pasan con ambos cuerpos juntos.
Comparten archivos del dashboard (`DashboardProjection/State/ViewModel`), así que separar 100% en
commits distintos es trabajo fino.

---

## 4. Pendientes para la próxima sesión

- [ ] **Commitear el working tree.** Decisión pendiente: ¿un commit grande, o intentar separar
      (a) header-date, (b) refactor snapshot, (c) feature frases? Comparten archivos del dashboard.
      Conventional commits, sin atribución de IA, ramificar si se está en `main`.
- [ ] **Confirmar visualmente la tarjeta de frase** completando el onboarding en el emulador
      (o con un atajo de debug). La lógica está testeada; falta el eyeball.
- [ ] **Limpiar el plan** `meta/instructions/2026-06-04-rotacion-frases-ancla.md`: todavía
      describe el Slice 2 de migración + `MIGRATION_12_13` + v13 (contradice Camino A).
- [ ] **Borrar `SleepMigration11To12Test.kt`** (androidTest legacy, muerto bajo Camino A) — opcional.
- [ ] Warnings menores del verify (no bloqueantes): test JVM del path de `onResumed` por cambio de
      fase; test de idempotencia del seed.

## 5. Archivos clave

- Feature: `domain/phrase/`, `data/phrase/`, `data/local/seed/AnchorPhraseSeed.kt`, `Models.kt`,
  `data/AutonomiaDao.kt`, `domain/dashboard/DashboardProjection.kt`, `domain/dashboard/DashboardState.kt`.
- Migración/Camino A: `data/AutonomiaDatabase.kt`, `CLAUDE.md`.
- Decisiones engram: `convention/dev-migrations-camino-a` (#845),
  `data/room-migration-test-infra` (#843), `feature/anchor-phrase-rotation` (#834).
- SDD archivado: `openspec/changes/archive/2026-06-04-anchor-phrase-rotation/`.
