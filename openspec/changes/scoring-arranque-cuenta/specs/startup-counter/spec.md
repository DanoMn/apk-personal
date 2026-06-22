# Especificación: startup-counter (contador de arranque de cuenta)

Cambio: `scoring-arranque-cuenta` · lotes 2 (Dominio de arranque) y 3 (Proyección + UI)
Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` ·
`docs/scoring/cambios-config-en-el-tiempo-v1.md` §3.3 (gracia/arranque)

## Purpose

Mientras una cuenta es nueva —sus anclas están en período de gracia y el motor real
todavía devuelve NoData— el contador de arranque reemplaza el blackout de 7 días por
una barra de carga: un número que sube desde 0 hacia el score real proyectado y, al
cumplirse la gracia (día 8), converge sin salto con el score maduro. El contador es un
canal de presentación SEPARADO del scoring real: el motor nunca se entera de que existe
arranque, el `ScoreReport` real sigue NoData, y nada se persiste.

Responsabilidad delimitada en tres piezas puras de dominio + un canal de presentación:
- `StartupDetectionRule`: decide SI la cuenta está en arranque.
- `StartupProjectionUseCase`: corre el motor con `windowDays = d` y sin filtrar las
  anclas en gracia → score proyectado.
- `StartupCounterPolicy`: atenúa el score proyectado por `d/7` y expone díasRestantes.
- `DashboardState.startup: StartupCardState?` + `StartupStatusCard`: presentación.

Lo que NO le toca: agregar `ScoreState.Arranque`, modificar `StatusCard`/orbe real,
persistir el contador, ni alterar el gate mínimo del motor.

---

## Inputs / Outputs

### StartupDetectionRule
| Campo | Significado | Forma | Obligatoriedad |
|-------|-------------|-------|----------------|
| `weeklyHistory` | historial semanal de scores (ya fluye a `ScoreInput`) | `List<WeeklyScoreHistoryEntry>` con `state: ScoreState` | requerido |
| `activeLayersWithAnchor` | nº de capas con al menos un ancla configurada, **contando las anclas en gracia** | `Int` | requerido |
| `minLayersGate` | cobertura mínima configurada | `Int`, default `MIN_ACTIVE_LAYERS_WITH_ANCHOR` (3) | opcional |
| **salida** `isStartup` | la cuenta está en arranque | `Boolean` | — |

### StartupProjectionUseCase
| Campo | Significado | Forma | Obligatoriedad |
|-------|-------------|-------|----------------|
| `daysLived` (`d`) | días vividos desde la creación de las anclas (`= GRACE_DAYS - díasRestantes`) | `Int ∈ [1, 7]` | requerido |
| **salida** `projectedScore` | score proyectado: motor con `windowDays = d`, SIN filtrar anclas en gracia | `Int` (puntos visibles) o estado proyectado | — |

### StartupCounterPolicy
| Campo | Significado | Forma | Obligatoriedad |
|-------|-------------|-------|----------------|
| `projectedScore` | salida de `StartupProjectionUseCase` | `Int` | requerido |
| `daysLived` (`d`) | días vividos | `Int`, clamp a `[1, 7]` | requerido |
| **salida** `counter` | `round(projectedScore × d/7)` | `Int` | — |
| **salida** `daysRemaining` | `GRACE_DAYS - d` (de `AnchorGraceRule`) | `Int ∈ [0, 6]` | — |

---

## Requirements

### Requirement: Detección de arranque por historial sin score real + gate de cobertura

`StartupDetectionRule` MUST devolver `isStartup = true` solo cuando AMBAS condiciones se
cumplen:

1. `weeklyHistory` NO contiene ninguna entrada con un score real, es decir, ninguna
   entrada con `state != ScoreState.NoData`.
2. `activeLayersWithAnchor >= minLayersGate` (≥3 capas con ancla), **contando las anclas
   aunque estén en gracia**.

Si `weeklyHistory` tiene al menos un score real → la cuenta ya maduró alguna vez → NO
es arranque (`false`). Si `activeLayersWithAnchor < minLayersGate` → la cuenta no
alcanza la cobertura mínima → NoData REAL ("configurá tu base"), NO arranque (`false`).
El gate de cobertura manda: una cuenta sin base suficiente nunca entra en arranque.

#### Scenario: Historial vacío + 3 capas en gracia → arranque

- GIVEN `weeklyHistory = []` (sin snapshots)
- AND `activeLayersWithAnchor = 3` (3 anclas en 3 capas, todas creadas hace < 7 días)
- WHEN `StartupDetectionRule.isStartup(...)` se invoca
- THEN devuelve `true`

#### Scenario: Historial con 1 score real → NO arranque

- GIVEN `weeklyHistory` con una entrada de `state = ScoreState.Motion` y el resto NoData
- AND `activeLayersWithAnchor = 3`
- WHEN `StartupDetectionRule.isStartup(...)` se invoca
- THEN devuelve `false` (la cuenta ya tuvo score real; no es nueva)

#### Scenario: Solo 2 capas con ancla → NO arranque (gate manda)

- GIVEN `weeklyHistory = []`
- AND `activeLayersWithAnchor = 2` (< 3)
- WHEN `StartupDetectionRule.isStartup(...)` se invoca
- THEN devuelve `false` (NoData real, "configurá tu base"; NUNCA arranque)

#### Scenario: Historial todo-NoData + 3 capas → arranque

- GIVEN `weeklyHistory` con 6 entradas, TODAS `state = ScoreState.NoData`
- AND `activeLayersWithAnchor = 3`
- WHEN `StartupDetectionRule.isStartup(...)` se invoca
- THEN devuelve `true` (NoData no cuenta como score real)

---

### Requirement: Proyección corre el motor con windowDays=d y sin filtrar gracia

`StartupProjectionUseCase` MUST producir el score proyectado corriendo el motor de
scoring con dos diferencias frente al camino maduro:

1. `windowDays = daysLived` (la ventana parcial de la capability `anchor-scoring`).
2. Las anclas en gracia NO se filtran: la proyección usa los hechos reales de los días
   vividos, incluyendo las anclas que `AnchorGraceRule.isWithinGrace` excluiría en el
   camino maduro.

La proyección es read-only sobre los hechos: corre el motor en memoria, NO modifica el
camino maduro de `BuildScoreInputUseCase` (que sigue filtrando la gracia para el
`ScoreReport` real). En arranque el motor se ejecuta dos veces (real → NoData, proyección
→ score); ambas corridas son dominio puro JVM, sin Room ni IO.

#### Scenario: 3 anclas en gracia, día 4, proyección no las descarta

- GIVEN 3 anclas creadas hace 4 días (en gracia), con hechos en los días vividos
- AND `daysLived = 4`
- WHEN `StartupProjectionUseCase` corre la proyección
- THEN el motor se ejecuta con `windowDays = 4` y SIN el `filterNot { Anchor && isWithinGrace }`
- AND el score proyectado es > 0 (las anclas en gracia SÍ aportan a la proyección)
- AND el `ScoreReport` REAL (camino maduro) sigue siendo NoData (las anclas siguen filtradas allí)

#### Scenario: La proyección no muta el camino maduro

- GIVEN una cuenta en arranque
- WHEN `StartupProjectionUseCase` corre
- THEN `BuildScoreInputUseCase` del camino maduro mantiene su filtro de gracia intacto
- AND el `ScoreReport` real expuesto por `ScoreEngine.calculate` no cambia por la proyección

---

### Requirement: Contador = scoreProyectado × d/7 con clamp de d

`StartupCounterPolicy` MUST calcular `counter = round(projectedScore × d/7)` con
`d = daysLived` clampado a `[1, 7]`. La atenuación `d/7` es la barra de carga: en día 1
el contador muestra ~1/7 del score proyectado; en día 7, exactamente el score proyectado
completo. `daysRemaining = GRACE_DAYS - d` proviene de `AnchorGraceRule` (`GRACE_DAYS = 7`).

#### Scenario: d=1 → contador = score × 1/7

- GIVEN `projectedScore = 700`, `daysLived = 1`
- WHEN `StartupCounterPolicy.compute(...)` se invoca
- THEN `counter = round(700 × 1/7) = 100`
- AND `daysRemaining = 7 - 1 = 6`

#### Scenario: d=4 → contador = score × 4/7

- GIVEN `projectedScore = 700`, `daysLived = 4`
- WHEN `StartupCounterPolicy.compute(...)` se invoca
- THEN `counter = round(700 × 4/7) = 400`
- AND `daysRemaining = 7 - 4 = 3`

#### Scenario: d=7 → contador = score × 7/7 (score completo)

- GIVEN `projectedScore = 700`, `daysLived = 7`
- WHEN `StartupCounterPolicy.compute(...)` se invoca
- THEN `counter = round(700 × 7/7) = 700`
- AND `daysRemaining = 7 - 7 = 0`

#### Scenario: d fuera de rango se clampa

- GIVEN `projectedScore = 700`, `daysLived = 9`
- WHEN `StartupCounterPolicy.compute(...)` se invoca
- THEN `d` se clampa a `7`; `counter = 700`; `daysRemaining = 0`

#### Scenario: Cuenta que no marcó nada → contador 0, mensaje compasivo

- GIVEN 3 anclas en gracia pero SIN hechos en ningún día vivido → `projectedScore = 0`
- AND `daysLived = 3`
- WHEN `StartupCounterPolicy.compute(...)` se invoca
- THEN `counter = round(0 × 3/7) = 0`
- AND `daysRemaining = 4`
- AND el estado de arranque sigue activo (la card se muestra con contador 0, NO se cae a NoData)

---

### Requirement: No-salto día 7→8 (convergencia con el score maduro)

Para los MISMOS hechos, el contador de arranque del día 7 (`× 7/7` sobre la proyección
con `windowDays = 7`) MUST coincidir con el score maduro del día 8, salvo el rodaje
natural de 1 día de la ventana móvil (`today.minusDays(6)`). En el día 7 las dos piezas
matemáticas convergen: `windowDays = 7` hace la proyección igual al cálculo maduro, y
`× 7/7 = ×1` no atenúa. El día 8, el camino maduro deja de filtrar la gracia (las anclas
cumplen 7 días) y produce el mismo número.

Este es un test obligatorio: garantiza que el usuario NO ve un salto brusco al pasar de
la barra de arranque al score real.

#### Scenario: Día 7 contador == día 8 score maduro (mismos hechos)

- GIVEN un conjunto fijo de hechos de anclas para 7 días
- AND en el día 7: cuenta en arranque, `daysLived = 7`, proyección con `windowDays = 7`, contador `× 7/7`
- AND en el día 8: las anclas cumplen gracia, camino maduro normal con `windowDays = 7`
- WHEN se comparan `counter(día 7)` y `scoreMaduro(día 8)` sobre los mismos hechos
- THEN ambos números coinciden, salvo la diferencia atribuible al desplazamiento de 1 día de la ventana móvil
- AND NO hay salto discontinuo (la transición es el rodaje natural de la ventana, no un escalón)

---

### Requirement: Persistencia NO se toca (invariante)

El contador de arranque MUST NOT persistirse. En arranque, el `ScoreReport` real
producido por `ScoreEngine.calculate` SIGUE siendo NoData y, por lo tanto, el
`WeeklyScoreSnapshotWriter` persiste su `visibleScore = 0` exactamente como hoy
(comportamiento preexistente, NO se modifica en este cambio). Ningún componente nuevo
de arranque escribe en Room.

#### Scenario: Arranque no escribe snapshot del contador

- GIVEN una cuenta en arranque con `counter = 400`
- WHEN el dashboard se refresca y `WeeklyScoreSnapshotWriter` corre
- THEN el snapshot persistido refleja el `ScoreReport` REAL (NoData → `visibleScore = 0`)
- AND el valor `400` del contador NO aparece en ninguna fila de `WeeklyScoreSnapshot`

#### Scenario: ScoreReport real en arranque sigue NoData

- GIVEN una cuenta en arranque (3 anclas en gracia)
- WHEN `ScoreEngine.calculate` produce el `ScoreReport`
- THEN `ScoreReport.state = ScoreState.NoData` (sin rama `Arranque`)
- AND el contador vive aparte, en el canal de presentación, no en el `ScoreReport`

---

### Requirement: Canal de presentación separado (DashboardState.startup)

El arranque MUST exponerse como `DashboardState.startup: StartupCardState?` (nullable),
NO como una rama nueva del enum `ScoreState`. Cuando la cuenta está en arranque,
`startup != null` (contiene contador + díasRestantes) y `scoreState` SIGUE siendo
NoData. Cuando la cuenta madura (o no aplica arranque), `startup == null`. El enum
`ScoreState` y sus `when` exhaustivos NO se tocan.

#### Scenario: Cuenta en arranque → startup != null, scoreState NoData

- GIVEN una cuenta en arranque detectada por `StartupDetectionRule`
- WHEN `DashboardProjection` computa el `DashboardState`
- THEN `DashboardState.startup != null` (con `counter` y `daysRemaining`)
- AND `DashboardState.scoreState == ScoreState.NoData`

#### Scenario: Cuenta madura → startup == null

- GIVEN una cuenta con al menos un score real en `weeklyHistory`
- WHEN `DashboardProjection` computa el `DashboardState`
- THEN `DashboardState.startup == null`
- AND el dashboard usa el `scoreState` real (no la card de arranque)

#### Scenario: <3 capas → startup == null y NoData real

- GIVEN una cuenta con solo 2 capas con ancla
- WHEN `DashboardProjection` computa el `DashboardState`
- THEN `DashboardState.startup == null`
- AND `scoreState == ScoreState.NoData` (mensaje "configurá tu base", no arranque)

---

### Requirement: UI — StartupStatusCard separado, StatusCard intacto

La UI MUST renderizar la barra de arranque con un componente Compose hermano,
`StartupStatusCard`, que recibe `StartupCardState`. El `StatusCard`/orbe real NO se
modifica. El dashboard elige cuál renderizar según `startup != null`. El copy respeta
el tono adulto/compasivo del proyecto: "Faltan N días para tu puntaje real" (o
equivalente aprobado); nada de "fallaste", diagnóstico ni castigo. El número central lo
calcula el dominio (`StartupCounterPolicy`); Compose SOLO anima.

#### Scenario: Dashboard elige StartupStatusCard cuando startup != null

- GIVEN `DashboardState.startup != null`
- WHEN `DashboardScreen` compone el área de estado
- THEN renderiza `StartupStatusCard` (no `StatusCard`)
- AND `StatusCard` permanece sin cambios en su código fuente

#### Scenario: Copy del contador respeta el tono y cubre "no marcó nada"

- GIVEN `startup = StartupCardState(counter = 0, daysRemaining = 4)`
- WHEN `StartupStatusCard` renderiza
- THEN el texto secundario dice "Faltan 4 días para tu puntaje real" (o copy canónico aprobado)
- AND no aparece ningún término del tono prohibido ("fallaste", "estás mal", "deberías")
- AND con `counter = 0` la card sigue siendo informativa/compasiva, no punitiva

#### Scenario: Cuenta madura usa el StatusCard real

- GIVEN `DashboardState.startup == null` y un `scoreState` real
- WHEN `DashboardScreen` compone el área de estado
- THEN renderiza el `StatusCard` real existente, idéntico a hoy

---

## Restricciones y reglas de negocio

- **Local-first / dominio puro:** la detección, proyección y atenuación viven en
  `domain/scoring`. `StartupProjectionUseCase` es el único con orquestación (corre el
  motor); `StartupDetectionRule` y `StartupCounterPolicy` son objetos puros. Compose
  SOLO presenta y anima; no calcula el contador.
- **Sin `ScoreState.Arranque`:** el enum y sus `when` exhaustivos quedan intactos. El
  arranque es un canal de presentación nullable, no un estado del motor.
- **Persistencia intacta:** ningún componente de arranque escribe en Room. El snapshot
  sigue reflejando el `ScoreReport` real (NoData → 0), comportamiento preexistente.
- **Gate mínimo soberano:** `< 3` capas con ancla → NoData real, NUNCA arranque.
- **Tono:** copy de la card respeta el tono adulto/compasivo (AGENTS.md); usa nombres
  canónicos de UI cuando corresponda ("Mis anclas", etc.).
- **Sin migraciones Room** (Camino A).
- **Nombre técnico vs UI:** "arranque" es término técnico; el texto visible usa copy
  aprobado ("Faltan N días para tu puntaje real").

## Criterios de aceptación

- Test (`testDebugUnitTest`): `StartupDetectionRule` — los 4 escenarios de detección
  (historial vacío→arranque, 1 score real→no, <3 capas→no, todo-NoData→arranque).
- Test: `StartupProjectionUseCase` corre el motor con `windowDays = d` y sin filtro de
  gracia; el `ScoreReport` real permanece NoData.
- Test: `StartupCounterPolicy` — `d=1`→×1/7, `d=4`→×4/7, `d=7`→×7/7, clamp, y
  `projectedScore = 0`→`counter = 0` con card activa.
- Test (obligatorio): no-salto día 7→8 — contador(día7) == scoreMaduro(día8) salvo el
  rodaje de 1 día de la ventana móvil.
- Test: invariante de persistencia — el contador NO aparece en snapshot; `ScoreReport`
  real = NoData.
- Test: `DashboardProjection` — arranque→`startup != null` y `scoreState = NoData`;
  madura→`startup == null`; <3 capas→`startup == null` + NoData.
- Runtime: install limpio, app arranca sin crashear; usuario nuevo con 3 anclas ve el
  contador 0→score proyectado, no "Sin datos" (ver `verificacion-por-capas.md`).
