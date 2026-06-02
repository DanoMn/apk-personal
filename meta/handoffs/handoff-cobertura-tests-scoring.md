# Handoff — cobertura de tests de scoring (3 gates cerrados)

> Para arrancar en **sesión nueva** sin contaminar contexto. Cargar este doc +
> recuperar memoria de Engram (proyecto `apk-personal`, observaciones 687, 688, 698,
> 700, 703, 705, 713).
>
> Fecha: 2026-06-02 · Proyecto: apk-personal (Vocal) · Rama base: `main`

---

## Objetivo de la sesión

El dueño quería **empezar a probar el sistema de scoring** ("definir qué se espera" +
"probar por features"), trabado por la duda de cómo testear un sistema que parece
necesitar semanas de constancia (estados base/elevados que dependen de métricas
semanales y memoria temporal).

## Conceptos clave establecidos (NO re-explicar al dueño)

1. **El tiempo es un DATO, no un reloj.** `ScoreEngine` es dominio puro: recibe
   `today: LocalDate` como parámetro (`ScoreInputSource.kt:32`, default `now()`) y
   hechos fechados. Para testear N semanas NO se espera: se fabrican hechos /
   `weeklyHistory` con fechas pasadas (ya lo hace `highHistory()` en
   `ScoreEngineTest.kt`). NO hace falta emulador para validar la matemática.
2. **Escenario golden** = ejercicio con su "hoja de respuestas" calculada **a mano
   desde el contrato** (`docs/scoring/arbol-scoring-vocal-v1.md`), NO desde el código.
   El dueño NO es desarrollador: explicarle con analogías, sin jerga.
3. **Caracterización vs TDD:** si falta comportamiento → TDD (test rojo primero). Si la
   fórmula ya existe y solo falta la red → caracterización (verde directo; el verde
   certifica que código y contrato coinciden).
4. **Verificación por capas** (`meta/guias/verificacion-por-capas.md`): "testear" =
   pasar TODAS las capas aplicables (build, lint, app-arranca, logs, tests, casos
   límite), no correr un solo test. Para cambios de **dominio puro** las capas 3-4
   (emulador/logs) se saltean legítimamente.

## Decisiones de producto tomadas (selladas — NO volver a preguntar)

- **D-sueño (§16.7):** sin registro de sueño en la semana, el estado se **topea en "En
  marcha" (Motion)** — no Plenitud ni Inquebrantable. NO penaliza el número de Cuerpo
  (ADR-3 intacto); solo topea el estado. Sueño es CORE, no opt-in.
- **D-config (§7.4):** de las 5 capas, el scoring exige **mínimo 3 capas activas con
  ≥1 ancla** (hasta 2 prescindibles). Con menos → `NoData`. Sueño/sobriedad sin ancla
  NO hacen contar una capa.

## Hecho y commiteado en `main` (todo verde: build + lint + suite unitaria)

| Commit | Qué |
|--------|-----|
| `46f8894` | feat: gate de sueño → Motion (§16.7). Bug real: antes daba Plenitud sin dormir. |
| `c228e76` | docs(backlog): onboarding obligatorio de sueño + notificaciones (diferido). |
| `d3d4c1b` | test: caracteriza sobriedad multi-track (§13.4, 70avg/30worst). Fórmula ya era correcta. |
| `fff3bd1` | docs: mapa de cobertura de tests de scoring. |
| `4af47ef` | feat: gate de 3+ capas con ancla (§7.4). Migró 10 tests que aislaban fórmulas con <3 capas. |
| `6c45879` | docs: mapa actualizado (gates marcados como hechos). |

Además: `SCORING_VERSION` `weekly-base-v0` → `weekly-base-v1` (la regla de estado del
sueño cambió; invalida snapshots viejos).

## Hallazgo importante de la auditoría

**El motor de scoring NO tiene bugs críticos ni divisiones por cero** — todos los
denominadores tienen guard (`targetDays().coerceIn(1,7)`, `targetDailyValue().coerceAtLeast(1)`,
Support `if(expectedSupportDays<=0)`, Sobriety `if(weekDates.isEmpty())`, capa sin anclas
usa `averageOrNull()`). Los agujeros restantes son de **caracterización** (red de
regresión), no de corrección.

## Qué falta (próxima sesión) — ver `meta/plan-cobertura-tests-scoring.md`

### 🧪 Tests de caracterización (el código anda, falta la red)
- **MEDIA:** curva de TaskMomentum (§10.3: 1→.020, 2→.032, 3→.039, 5→.046),
  curva de RelapseProtection de sobriedad (§13.3: `exp(-relapseDays/1.5)`),
  fórmula de StabilityScore (§15) y conteo de semanas.
- **BAJA:** bordes de VisibleScore (§3.2: clamp <0/>1, 700, 1000), units directos de
  `AnchorScoringPolicy`/`SupportScoringPolicy`, tests de regresión de los guards
  anti-NaN, cap de sueño a nivel unit en `BaseStatePolicyTest`.

### 🔧 Trabajo de UI (no scoring) — `meta/pendientes.md`
- Onboarding que OBLIGUE a configurar 3 capas con ancla (hoy hay default heredado).
- Onboarding que OBLIGUE a elegir horario de sueño (hoy `SleepPolicy.defaultConfig()`
  hereda 23:30–07:30 / 8h).
- Notificaciones: recordar registrar sueño + avisar días seguidos sin registro.

## Archivos relevantes

**Contrato (spec del scoring):**
- `docs/scoring/arbol-scoring-vocal-v1.md` — fórmulas canónicas. §7.4 (gate config),
  §13.4 (sobriedad multi-track), §16.7 (cap de sueño) son lo tocado esta sesión.
- `docs/sueno/decisiones-diseno-sueno-v1.md:51` — "Sueño es CORE, NO opt-in".

**Código de dominio (motor):**
- `domain/scoring/ScoreEngine.kt` — orquestador + gates de NoData (hasAnyFact + 3 capas).
- `domain/scoring/BaseStatePolicy.kt` — estados, caps, histéresis, cap de sueño.
- `domain/scoring/SobrietyScoringPolicy.kt` — fórmula multi-track (§13.4).
- `domain/scoring/ScoringConstants.kt` — `MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3`, etc.
- `domain/scoring/WeeklyScoreSnapshotModels.kt` — `SCORING_VERSION`.
- `domain/scoring/ScoringExtensions.kt` — `targetDays()`/`targetDailyValue()` (guards).

**Tests:**
- `app/src/test/.../domain/scoring/ScoreEngineTest.kt` — integración; helpers
  `fillerLayers()/fillerActivities()/fillerLogs()` para pasar el gate de 3 capas.
- `app/src/test/.../domain/scoring/SobrietyScoringPolicyTest.kt` — multi-track.
- `app/src/test/.../domain/scoring/BaseStatePolicyTest.kt` — 20 tests de estados.

**Plan y proceso:**
- `meta/plan-cobertura-tests-scoring.md` — mapa de cobertura priorizado (fuente para
  retomar).
- `meta/guias/verificacion-por-capas.md` — gates obligatorios.
- `meta/pendientes.md` — backlog (sección "Sueño / configuración").

## Cómo retomar

1. Cargar este handoff + `mem_search` en Engram (proyecto `apk-personal`) por
   "cobertura tests scoring" / "gate".
2. Leer `meta/plan-cobertura-tests-scoring.md` (qué falta, priorizado).
3. Si se siguen caracterizaciones: calcular cada hoja de respuestas **desde el contrato**
   `arbol-scoring-vocal-v1.md`, NO desde el código. Verde esperado = código correcto.
4. Comando para un test puntual:
   `.\gradlew.bat testDebugUnitTest --tests 'dev.panopt.autonomia.domain.scoring.*'`
   (vía `scripts/dev/dev.sh` para build/lint; `$env:JAVA_HOME` del JBR).
