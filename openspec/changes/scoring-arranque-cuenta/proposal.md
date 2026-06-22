# Proposal: Barra de arranque de cuenta (contador 0→score real en los primeros 7 días)

> Materializado desde engram `sdd/scoring-arranque-cuenta/proposal` (#1299). La fuente
> de verdad del proposal vive en engram; este archivo es la copia openspec del cambio.

## Intent

Usuario nuevo configura 3 anclas (cobertura mínima: 3 capas con ancla).
`AnchorGraceRule.isWithinGrace` (`createdAt < 7d`) las filtra en `BuildScoreInputUseCase`
ANTES del gate del motor → `activeLayersWithAnchor = 0 < MIN_ACTIVE_LAYERS_WITH_ANCHOR(3)`
→ NoData → el dashboard muestra "Sin datos" 7 días (blackout de arranque, #858 root
cause 3). Reemplazar ese blackout por un CONTADOR (barra de carga) que sube de 0 hacia
el score real proyectado; al día 8 el score real toma la posta sin salto.

## Scope

### In Scope
- Generalizar `AnchorScoringPolicy.rFromRatios` de "semana de 7 días" a "ventana de N
  días" vía parámetro `windowDays` (DEFAULT 7 → comportamiento maduro byte-idéntico).
- Dominio de arranque NUEVO (todo puro salvo el use case): `StartupDetectionRule`,
  `StartupProjectionUseCase`, `StartupCounterPolicy`.
- Canal de presentación aparte: `DashboardState.startup: StartupCardState?` +
  `StartupStatusCard` (Compose hermano de `StatusCard`, que NO se toca).
- Tests: regresión cero (`windowDays=7`), justicia de ventana parcial, convergencia
  día 7→8 sin salto.
- Actualizar docs vivos de scoring afectados (modelo-matemático: parámetro `windowDays`).

### Out of Scope (explícito)
- PERSISTENCIA NO SE TOCA. El "0" que NoData guarda hoy en `WeeklyScoreSnapshot` se
  mantiene. El contador de arranque NO se persiste; el `ScoreReport` real en arranque
  sigue siendo NoData.
- NO se agrega `ScoreState.Arranque` (ensuciaría el enum y rompería ≥5 `when`
  exhaustivos). El estado real sigue NoData.
- NO es página nueva ni se modifica `StatusCard`/orbe existente.
- Migraciones Room (Camino A: dev no escribe migraciones).
- Soportes/tasks del modelo de scoring (fuera de este cambio).

## Approach

**Opción A — reusar el motor, NO una fórmula paralela.** El motor se generaliza de
"asume 7 días" a "ventana de N días". `f` (frecuencia) gobierna 4 términos —`phi`,
`cut`, `sd`, `wt`— y el `7` está hardcodeado en `sd` (L67) y `wt` (L68). Generalizar
7→N dentro de la policy evita duplicar el contrato matemático.

**Dos piezas matemáticas distintas y NO redundantes:**
- (a) `windowDays=N` en el motor = JUSTICIA: score proyectado correcto, no castiga
  días no llegados.
- (b) `× d/7` en `StartupCounterPolicy` (aparte) = BARRA DE CARGA: atenúa
  (día1≈score×1/7 … día7=score×7/7=score). En el día 7 ambas convergen al score real
  → sin salto día 7→8.

## Capabilities

### New Capabilities
- `startup-counter`: ventana parcial + contador de arranque (detección, proyección con
  `windowDays`, atenuación `d/7`, díasRestantes, card de presentación).

### Modified Capabilities
- `anchor-scoring`: `rFromRatios` gana `windowDays` (generaliza 7→N en `sd` y `wt`).
  Cambio de superficie matemática → delta spec.

## Lotes (orden estricto, cada uno < 400 líneas)
1. **Núcleo:** `rFromRatios` gana `windowDays` (default 7). Tests: `windowDays=7==hoy`
   (cero regresión) + `windowDays<7` justo + validación de los 4 términos.
2. **Dominio de arranque:** `StartupDetectionRule` + `StartupProjectionUseCase` +
   `StartupCounterPolicy` + `StartupCardState`. Tests, incluido no-salto día 7→8.
3. **Proyección + UI:** `DashboardState.startup`, `DashboardProjection` lo computa,
   `StartupStatusCard` animado, el dashboard elige el card.

## Success Criteria
- [ ] Usuario nuevo con 3 anclas en gracia ve contador 0→score proyectado, no NoData.
- [ ] `windowDays=7` produce score byte-idéntico al actual (suite verde sin cambios).
- [ ] Día 7 (×7/7) == score maduro día 8: cero salto (test explícito).
- [ ] <3 capas con ancla → NoData real ("configurá tu base"), NO arranque.
- [ ] Persistencia y enum `ScoreState` sin cambios; doc vivo modelo-matemático actualizado.
- [ ] Strict TDD respetado en los 3 lotes.
