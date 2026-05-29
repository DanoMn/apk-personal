# Handoff — Verificación del sistema de scoring (próxima sesión SDD)

Fecha: 2026-05-29
Proyecto: apk-personal (Vocal / Autonomía sin límites)
Rama actual: `sdd/scoring-state-alignment`
Estado: handoff para una sesión SDD nueva. Cargar este doc + recuperar memoria de Engram.

---

## 1. Objetivo de la próxima sesión

Verificar que el **sistema de scoring funciona correctamente de punta a punta**, no solo
en las fórmulas aisladas. La lección que motiva esto: tenemos 104 tests unitarios en
verde, pero el crash de migración demostró que **"en verde" ≠ "funciona en la app real"**.

NO es objetivo de esta sesión implementar features nuevas. Es **verificar** lo que ya existe.

---

## 2. Estado actual (de dónde partís)

- **Slice 1 hecho** (rama `sdd/scoring-state-alignment`): `BaseStatePolicy` reescrito para
  evaluar estados sobre `weeklyBaseScore` (bandas 0.40 / 0.70 / 0.85), escalera de peor capa
  (`<0.30` colapso → Restauración, `<0.55` tope Atención, `≥0.75` Plenitud, `≥0.80`
  Inquebrantable), histéresis 0.03. Asimetría `rawScore`/`baseScore` ratificada (solo docs).
- **Crash de migración resuelto** (commit `6d5ba17`): en dev se usa
  `fallbackToDestructiveMigration(dropAllTables = true)`. La app abre OK con instalación
  o reinstalación.
- **Tests actuales**: 104 unitarios, **solo dominio puro**. NO hay:
  - test de integración del pipeline completo (Room → input builder → motor → proyección);
  - test de migraciones reales (`MigrationTestHelper`);
  - test con simulación temporal de varias semanas más allá de lo mínimo de estabilidad.
- La app **abre y el seed de actividades está** (verificado en device por el usuario).

### Pipeline a verificar (cómo fluye el scoring)

```
Hechos Room (daily_activity_logs, sleep, abstinence, configs, weekly snapshots)
  → ScoreInputSource / BuildScoreInputUseCase   (arma ScoreInput)
  → ScoreEngine.calculate(ScoreInput): ScoreReport   (dominio PURO, sin Room/Compose)
  → DashboardProjection                          (mapea a estado de UI)
  → DashboardScreen (resumen) / ScoringScreen "Estado Base" (detalle)
```

Archivos clave: `domain/scoring/ScoreEngine.kt`, `domain/scoring/BuildScoreInputUseCase.kt`,
`domain/scoring/ScoreInputSource.kt`, `domain/dashboard/DashboardProjection.kt`.

---

## 3. La duda central resuelta: ¿cómo se testea lo diario/semanal?

El usuario preguntó, con razón: "el scoring requiere tracking diario y semanal, ¿cómo lo
hago en un solo test?". Respuesta importante que define el enfoque:

**El `ScoreEngine` es PURO: recibe un `ScoreInput` ya construido y devuelve un `ScoreReport`.**
No espera tiempo real, no lee Room. Por lo tanto:

- **En un test (unitario o de integración):** se *fabrica* una semana de hechos con **fechas
  fijas** (`LocalDate` sintéticas) — p. ej. 7 días de `DailyActivityLog` para una ancla, un
  `SleepLog`/sesiones, logs de sobriedad, etc. Un test = una semana completa simulada. Para
  cubrir lo **semanal/temporal** (estabilidad, histéresis, Inquebrantable) se inyecta
  `weeklyHistory` con snapshots de semanas previas, también con fechas fijas. **No hace falta
  esperar días reales.** Esto ya se hace parcialmente en `ScoreEngineTest` y
  `BuildScoreInputUseCaseTest`; falta llevarlo a escenarios realistas completos.

- **En end-to-end sobre el dispositivo (lo que el usuario prefiere):** acá SÍ el tracking
  diario/semanal es **tiempo de calendario real** y no se puede adelantar desde la app. Esto
  es la fricción principal. Opciones a decidir (ver §6):
  1. Verificar en device solo lo **diario inmediato** (configurar, registrar hoy, ver que el
     estado/score del día reacciona) + dejar lo semanal/temporal a tests de integración.
  2. Agregar un **camino de debug** (oculto) para inyectar una semana sintética de hechos en
     Room y así poder ver el reporte semanal real en pantalla.
  3. Híbrido (recomendado): integración para la lógica temporal/semanal + device para
     confirmar el cableado y la UI con datos de un día/parciales.

**Conclusión:** "end-to-end puro en device" NO cubre bien lo semanal sin un inyector de datos.
La estrategia más honesta es híbrida. Decidir esto es lo primero de la próxima sesión.

---

## 4. Las combinaciones de features (el "quilombo")

El scoring cambia según qué features estén activas. Recordatorio de cómo pesan (ver
`docs/arbol-scoring-vocal-v1.md` y `plan-tecnico-scoring-vocal.md` §7):

- **Sueño** solo afecta la capa **Cuerpo** (30% de Cuerpo).
- **Sobriedad** solo afecta la capa **Conducta** (30% de Conducta), y solo si hay track activo.
- **Soportes**: si una capa tiene soportes → base de capa = 80% anclas + 20% soportes; si no
  tiene → anclas 100%.
- **Tasks** con capa completadas → `TaskMomentumBonus` (hasta 5%), nunca penalizan.

Variantes que el usuario listó (NO hay que probarlas todas, pero la matriz debe ser consciente):

| # | Combinación | Qué ejercita |
|---|-------------|--------------|
| 1 | 3 anclas + sueño | Base mínima + sueño en Cuerpo |
| 2 | anclas + sueño + soportes | + ponderación 80/20 de soportes |
| 3 | anclas + sueño + soportes + sobriedad | + sobriedad en Conducta (caso completo) |
| 4 | anclas + sueño + sobriedad | sobriedad sin soportes |
| 5 | cualquiera + tasks | + TaskMomentum |

**Propuesta de matriz mínima (a validar):** caso 1 (base), caso 3 (todo junto), y un caso de
borde por feature (peor capa baja, recaída de sobriedad, superávit/superhabit, semana perfecta
sin historial → Plenitud no Inquebrantable). No permutación exhaustiva; cobertura por
*responsabilidad*, no por combinatoria.

---

## 5. Enfoque recomendado para la sesión

1. **Decidir la estrategia de verificación** (§3 / §6): híbrido es lo recomendado.
2. **Test de integración del pipeline** (`BuildScoreInput → ScoreEngine → DashboardProjection`)
   con 2-3 escenarios realistas de semana completa (fechas fijas), comparando contra valores
   **calculados a mano** desde las fórmulas. Esto cierra el hueco "tests no reflejan la app".
3. **Escenario(s) de device**: el agente calcula a mano estado + score esperado para una
   config concreta; el usuario la reproduce y compara. Para lo semanal, definir si se usa
   inyector de debug.
4. (Opcional, relacionado) `MigrationTestHelper` para blindar el bug de migración (#587) — es
   parte de "verificar que no se rompe en device", aunque es su propio cambio.

Hacerlo dentro del **flujo SDD** (como el slice 1): explorar → proponer → spec → tasks → apply
→ verify. Artifact store: **openspec**. Strict TDD: **enabled**.

---

## 6. Preguntas abiertas a resolver al inicio de la sesión

1. ¿Estrategia de verificación? (device-only diario / inyector de debug / **híbrido** recomendado)
2. ¿Qué casos entran en la matriz mínima? (propuesta en §4)
3. ¿Los valores esperados se calculan a mano y se hardcodean como ground-truth, o se documenta
   el cálculo en el spec?
4. ¿Se incluye el `MigrationTestHelper` en este mismo esfuerzo o se deja como cambio aparte?

---

## 7. Contexto a recuperar (Engram + repo)

Al arrancar la sesión nueva, recuperar de Engram (proyecto `apk-personal`, buscar por
topic_key exacto):

- `#574` — Auditoría scoring: estado real vs plan-técnico.
- `#578` — Decisiones D1 (alinear estados) y D2 (ratificar asimetría).
- `#587` — BUG migraciones Room (`idx_*` vs `index_*` + defaults espurios) — pendiente fix real.
- `#588` — Regla "mínimo 3" NO implementada; comportamiento bajo el mínimo indefinido.
- `#589` — Decisión: bajo mínimo-3 = "base en construcción" (NoData) + bloquear borrado (opción 3).

Artefactos SDD en `openspec/changes/scoring-audit-remediation/` (exploration, proposal,
spec/base-state-policy, design, tasks).

Commits de esta sesión (rama `sdd/scoring-state-alignment`, NO pusheada):
`388503b` CLAUDE.md · `1fdd36b` scoring thresholds · `01bf418` dev-phase · `6d5ba17` db
fallback destructivo · `e3227b7` carve-out del seed.

---

## 8. Cola de trabajo posterior a la verificación (no para esta sesión)

En orden de prioridad acordado con el usuario:

1. **Tutorial de bienvenida / onboarding** (parcialmente definido en docs — buscar en
   documentación de producto).
2. **Regla mínimo-3** (slice 3 `base-config-infra`): `BaseConfigurationEntity` +
   "base en construcción" en dominio + bloqueo de borrado bajo 3 + amortización primera semana.
   Contrato ya decidido (opción 3, ver `#589`).
3. **Documento de actividades pendientes (backlog)** — crear y mantener.
4. Slices restantes del plan: 2 (sleep-sessions telemetría), 4 (dao-range-queries),
   5 (abstinence PendingConfirmation), 6 (legacy-cleanup + fix migraciones #587), Fase 8 (UI
   explicativa).

> Nota: el slice 1 (alineación de estados) queda **sin archivar** (`sdd-archive` pendiente)
> por si se quiere revisar el diff antes de cerrarlo.
