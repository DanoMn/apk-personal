# Handoff — Edge cases del motor de scoring (recálculo, promedios, diario vs semanal)

> **Estado: CONGELADO** (registro para una sesión futura). Fecha: 2026-06-04.
> **Tipo:** revisión de diseño + posible caza de bugs en el motor de scoring. Importante.

## Por qué este handoff

El usuario levantó dudas de fondo sobre cómo se comporta el score ante cambios, y no están
resueltas/documentadas con claridad. Son edge cases que pueden esconder bugs o decisiones de
diseño no tomadas. Hay que responderlas con el código + el contrato matemático en mano.

## Preguntas abiertas a resolver

1. **Quitar un ancla después de semanas de buen uso.** Un usuario lleva ~3 semanas cumpliendo
   sus anclas y de repente **archiva/quita un ancla**. ¿Qué pasa?
   - ¿Se recalcula el score? ¿Le baja? ¿De qué semana(s)?
   - Las semanas ya cerradas están cacheadas en `WeeklyScoreSnapshotEntity` (cache derivado,
     versionado). ¿Quitar un ancla recalcula el pasado o solo la semana viva?
   - Verificar contra: `WeeklyScoreSnapshotWriter` (solo escribe semanas nuevas/faltantes, no
     reescribe cerradas — `WeeklyScoreSnapshotWriter.kt:46`) y la normalización por capa.

2. **Cómo se promedian los puntajes.** ¿Cómo se combinan?
   - Hechos diarios (`daily_activity_logs`) → score semanal → ¿promedio multi-semana?
   - Rol de `StabilityScoringPolicy` (estabilidad sobre varias semanas, `input.weeklyHistory`).
   - ¿El "estado" mostrado es de la semana actual o un promedio? (Hoy: `scoreReport.state` de la
     semana viva, `DashboardProjection.kt:116`.)

3. **"Tiempo real" vs registros diarios vs semanales — reconciliar el modelo.** El usuario está
   confundido (con razón): el score "se calcula al momento", pero hay registros diarios Y
   snapshots semanales. ¿Cuál es la fuente de verdad y cuándo?
   - Modelo actual (CLAUDE.md "Pipeline de scoring"): Room guarda **hechos**; el dominio recalcula
     el estado en vivo desde los hechos (reactivo, `combine`+`stateIn`); los snapshots semanales
     son **cache derivado del pasado inmutable**, recalculable. El cierre diario
     (`closeElapsedActivityDays`) materializa estados editables del día en hechos históricos.
   - Falta: documentar/clarificar este modelo de forma simple para que el comportamiento sea
     predecible, y verificar que no haya inconsistencias (ej. estado live vs estado del snapshot —
     ver nota ADR-3 de frases en `docs/dominio/frases-ancla.md §18`: el resolver de frases ancla
     usa el `state` del snapshot, que puede diferir del live a mitad de sesión — ¿es deseable en
     todos los consumidores?).

4. **Edge case relacionado (de la otra sesión):** si se implementa una "base inicial" simulada,
   ¿cómo conviven esos datos con el recálculo en tiempo real? (ver
   `meta/handoffs/2026-06-04-base-inicial-usuario-nuevo.md`).

## Referencias

- `docs/scoring/arbol-scoring-v1.md` — **contrato matemático canónico** (fórmulas, umbrales de
  estado, normalización por capa). Es el spec.
- `docs/scoring/plan-tecnico-scoring.md` — plan técnico, decisiones por fase, modos de lectura.
- Código:
  - `domain/scoring/ScoreEngine.kt` — orquestador.
  - `domain/scoring/*Policy.kt` — fórmulas atómicas (Layer, Weekly, Visible, Stability, BaseState…).
  - `domain/scoring/WeeklyScoringContextBuilder.kt` — arma el contexto semanal desde hechos.
  - `data/scoring/WeeklyScoreSnapshotWriter.kt` + `WeeklySnapshotDataSource.kt` — cache de semanas
    cerradas (NO reescribe el pasado).
  - `AutonomiaRepository.closeElapsedActivityDays` — cierre diario (materializa hechos históricos).
- `CLAUDE.md` sección "Pipeline de scoring" y "Arquitectura: hechos → dominio → estado → Compose".

## Salida esperada de esa sesión

- Respuestas claras y documentadas (en `docs/scoring/`) a las 3 preguntas.
- Tests que cubran los edge cases (quitar ancla, multi-semana, recálculo) — dominio puro JVM.
- Si se encuentran bugs, corregirlos; si son decisiones de diseño faltantes, tomarlas y registrarlas.
