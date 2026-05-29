# Handoff — Verificación del sistema de scoring (próximas sesiones SDD)

Fecha: 2026-05-29
Proyecto: apk-personal (Vocal / Autonomía sin límites)
Rama actual: `sdd/scoring-state-alignment`
Estado: handoff para sesiones SDD nuevas. Cargar este doc + recuperar memoria de Engram.

---

## 0. Secuencia correcta (leer esto primero)

El objetivo grande es **confiar en que el scoring funciona bien**. Pero NO se arranca por los
tests. Se arranca por **entender y depurar la implementación real de cada feature**. No se
pueden escribir tests con valores esperados confiables si todavía no se entendió ni se limpió
cómo cada feature configura y procesa sus datos.

```
FASE A (primero, 1+ sesiones)  →  FASE B (después)
Revisar + depurar la lógica de      Verificar con tests
configuración y procesamiento       (integración + escenario en device)
de datos, feature por feature
```

Este handoff cubre **ambas fases**, pero la próxima sesión es la **FASE A**.

---

## 1. Estado actual (de dónde partís)

- **Slice 1 hecho** (rama `sdd/scoring-state-alignment`): `BaseStatePolicy` reescrito para
  evaluar estados sobre `weeklyBaseScore` (bandas 0.40 / 0.70 / 0.85), escalera de peor capa
  (`<0.30` colapso → Restauración, `<0.55` tope Atención, `≥0.75` Plenitud, `≥0.80`
  Inquebrantable), histéresis 0.03. Asimetría `rawScore`/`baseScore` ratificada (solo docs).
- **Crash de migración resuelto** (commit `6d5ba17`): en dev se usa
  `fallbackToDestructiveMigration(dropAllTables = true)`. La app abre OK.
- **Tests actuales**: 104 unitarios, **solo dominio puro** (fórmulas aisladas). NO hay test de
  integración del pipeline, ni `MigrationTestHelper`, ni escenarios temporales realistas.
- La app **abre y el seed de actividades está** (verificado en device por el usuario).

### Pipeline completo (lo que hay que entender en Fase A)

```
Config de usuario (UI) ─┐
                        ├→ Room (daily_activity_logs, sleep, abstinence, configs, snapshots)
Registro diario (UI) ───┘        │
                                 → ScoreInputSource / BuildScoreInputUseCase  (arma ScoreInput)
                                 → ScoreEngine.calculate(ScoreInput): ScoreReport  (dominio PURO)
                                 → DashboardProjection  (mapea a estado de UI)
                                 → DashboardScreen (resumen) / ScoringScreen "Estado Base"
```

---

## 2. FASE A — Revisión y depuración por feature (PRÓXIMA SESIÓN)

Objetivo: para **cada feature**, entender de punta a punta cómo se **configura** y cómo se
**procesan sus datos** hasta el scoring; encontrar y depurar inconsistencias. Recién con esto
claro y confiable se pasa a Fase B (tests).

Para cada feature, responder y dejar documentado:

1. **Configuración**: ¿qué pantalla la configura? ¿qué entidad/targets/límites guarda? ¿hay
   validación de rangos? ¿qué pasa con valores borde o vacíos?
2. **Persistencia**: ¿qué hecho se escribe, dónde y con qué semántica? (estados
   `Done/NotDone/Omitted`, logs de sueño/sobriedad, etc.)
3. **Procesamiento**: ¿cómo lo lee `BuildScoreInputUseCase` + `WeeklyScoringContextBuilder` y
   qué policy lo convierte en score? ¿coincide con el árbol de fórmulas?
4. **Depuración**: anotar y arreglar lo que esté inconsistente o roto.

### Mapa de archivos por feature

| Feature | Config (UI) | Persistencia | Procesamiento / scoring |
|---------|-------------|--------------|-------------------------|
| **Anclas** | `ui/anchors/*` | `UserActivityConfigEntity` + `daily_activity_logs` | `AnchorScoringPolicy`, `AnchorTargets`, `domain/activity/*` |
| **Soportes** | `ui/supports/*` | `daily_activity_logs` (semántica inversa: `Omitted`) | `SupportScoringPolicy` (opt-in, 80/20) |
| **Sueño** | `ui/sleep/SleepConfigScreen` | `SleepConfigEntity`, `SleepLogEntity` | `SleepScoring` (⚠ usa 2 de 4 componentes), `SpecialLayerScoringPolicy` (30% Cuerpo) |
| **Sobriedad** | `ui/sobriety/*` | `AbstinenceTrack/Log/RelapseEvent` | `SobrietyScoringPolicy`, `AbstinenceRelapseMaterializationPolicy` (30% Conducta) |
| **Tasks** | `ui/tasks/TasksScreen` | `TaskEntity` | `TaskMomentumPolicy`, `domain/task/*` (bonus ≤5%, no penaliza) |

### Issues conocidos a vigilar durante la depuración (de la auditoría #574)

- **Sueño**: `SleepScoring` usa `0.70 duración + 0.30 horario` (2 de 4 componentes); el árbol
  pide `0.40/0.25/0.20/0.15` (Duration/Continuity/ScheduleAlignment/DigitalInterruption). Y es
  por día, no por sesiones. (Slice 2 lo arregla, pero entender el estado actual.)
- **Sobriedad**: faltan range queries (`observeAbstinenceLogsBetween`,
  `observeRelapseEventsOverlapping`); el writer hace full-scan. `PendingConfirmation` no está
  en el enum (usa `Unknown`).
- **Cierre diario**: `closeElapsedActivityDays` solo cierra la semana actual, no días de
  semanas anteriores.
- **Mínimo-3**: hoy con <3 capas se calcula score igual (silenciosamente incorrecto); decidido
  contrato opción 3 (Engram `#589`) pero NO implementado.
- **Tasks/momentum**: confirmar que solo cuentan tasks **con capa** completadas.

Sugerencia: hacer Fase A **una feature a la vez**, cada una como su propia exploración SDD
(`sdd-explore`) que produce un doc de "cómo funciona X + bugs encontrados". No mezclar todas.

---

## 3. FASE B — Verificación con tests (DESPUÉS de Fase A)

Solo cuando la implementación de cada feature esté entendida y depurada.

### 3.1 La duda del tracking diario/semanal, resuelta

El `ScoreEngine` es **PURO**: recibe un `ScoreInput` ya armado y devuelve un `ScoreReport`. No
espera tiempo real ni lee Room. Por lo tanto:

- **En tests**: se *fabrica* una semana con **fechas fijas** (`LocalDate` sintéticas) — 7 días
  de logs, sueño, sobriedad, etc. Un test = una semana simulada. Lo **semanal/temporal**
  (estabilidad, histéresis, Inquebrantable) se inyecta vía `weeklyHistory` con snapshots de
  semanas previas. **No hace falta esperar días reales.**
- **End-to-end en device**: acá el tracking SÍ es tiempo de calendario real y no se puede
  adelantar. Por eso el end-to-end puro **no cubre bien lo semanal** sin un inyector de datos.
  → Enfoque recomendado **híbrido**: integración para lo temporal/semanal + device para
  confirmar cableado y UI con datos de un día/parciales. (Decidir al inicio de Fase B.)

### 3.2 Combinaciones de features (cómo pesan)

- **Sueño** → solo capa **Cuerpo** (30%). **Sobriedad** → solo capa **Conducta** (30%, si hay
  track activo). **Soportes** → 80/20 si la capa tiene; si no, anclas 100%. **Tasks** →
  `TaskMomentumBonus` (≤5%), nunca penalizan.

| # | Combinación | Qué ejercita |
|---|-------------|--------------|
| 1 | 3 anclas + sueño | Base mínima + sueño en Cuerpo |
| 2 | + soportes | Ponderación 80/20 |
| 3 | + sobriedad | Caso completo (Conducta) |
| 4 | anclas + sueño + sobriedad | Sobriedad sin soportes |
| 5 | + tasks | TaskMomentum |

Matriz mínima propuesta (a validar): caso 1, caso 3, y un borde por feature (peor capa baja,
recaída, superávit, semana perfecta sin historial → Plenitud no Inquebrantable). Cobertura por
**responsabilidad**, no por permutación exhaustiva.

### 3.3 Entregables de Fase B

1. Test(s) de integración del pipeline (`BuildScoreInput → ScoreEngine → DashboardProjection`)
   con escenarios de semana completa, comparando contra valores **calculados a mano**.
2. Escenario(s) de device: el agente calcula a mano estado + score esperados; el usuario
   reproduce y compara.
3. (Opcional) `MigrationTestHelper` para blindar el bug de migración `#587`.

---

## 4. Preguntas abiertas

- (Fase A) ¿Cada feature como exploración SDD separada, o una pasada conjunta?
- (Fase B) ¿Estrategia? (device-only diario / inyector de debug / **híbrido** recomendado)
- (Fase B) ¿Valores esperados hardcodeados como ground-truth o cálculo documentado en el spec?

---

## 5. Contexto a recuperar (Engram + repo)

Engram (proyecto `apk-personal`, buscar por topic_key EXACTO):

- `#574` — Auditoría scoring: estado real vs plan-técnico.
- `#578` — Decisiones D1 (alinear estados) y D2 (ratificar asimetría).
- `#587` — BUG migraciones Room (`idx_*` vs `index_*` + defaults) — fix real pendiente.
- `#588` — Regla "mínimo 3" NO implementada; comportamiento bajo el mínimo indefinido.
- `#589` — Decisión bajo mínimo-3 = "base en construcción" + bloquear borrado (opción 3).

Artefactos SDD: `openspec/changes/scoring-audit-remediation/`.

Commits de esta sesión (rama `sdd/scoring-state-alignment`, NO pusheada):
`388503b` CLAUDE.md · `1fdd36b` scoring thresholds · `01bf418` dev-phase · `6d5ba17` db
fallback destructivo · `e3227b7` carve-out del seed · `adf9ec1`/`(este)` handoff.

---

## 6. Cola de trabajo posterior (no para estas sesiones)

En orden de prioridad acordado:

1. **Tutorial de bienvenida / onboarding** (parcial en docs de producto).
2. **Regla mínimo-3** (slice 3 `base-config-infra`): `BaseConfigurationEntity` + "base en
   construcción" en dominio + bloqueo de borrado bajo 3 + amortización. Contrato decidido (`#589`).
3. **Documento de actividades pendientes (backlog)** — crear y mantener.
4. Slices 2 (sleep telemetría), 4 (dao-range-queries), 5 (PendingConfirmation), 6 (legacy +
   fix migraciones #587), Fase 8 (UI explicativa).

> Slice 1 queda **sin archivar** (`sdd-archive` pendiente) por si se revisa el diff antes.
