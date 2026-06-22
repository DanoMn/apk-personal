# Especificación: anchor-scoring (ventana de N días)

Cambio: `scoring-arranque-cuenta` · lote 1 (Núcleo)
Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` §NIVEL 1 ·
`app/src/main/java/dev/panopt/autonomia/domain/scoring/AnchorScoringPolicy.kt` L39-73

## Purpose

`AnchorScoringPolicy.rFromRatios` calcula `R ∈ [0, 1.5]` de un ancla a partir de sus
ratios diarios. Este cambio lo generaliza de "la semana SIEMPRE tiene 7 días" a "la
ventana tiene `windowDays = N` días", para que el motor pueda puntuar con justicia una
ventana parcial (cuenta nueva, `d < 7` días vividos) SIN castigar los días que todavía
no llegaron. La policy sigue siendo dominio puro JVM: no conoce Room, Compose ni
arranque; solo recibe un parámetro más. El comportamiento maduro (semana completa) NO
cambia: `windowDays = 7` produce un resultado byte-idéntico al actual.

Lo que NO le toca: detectar arranque, atenuar por `d/7`, conocer la gracia de anclas,
ni leer `weeklyHistory`. Eso vive en la capability `startup-counter`. Esta policy solo
acepta un horizonte `N` y reparte sus términos sobre `N` en vez de sobre `7`.

---

## Inputs / Outputs

`rFromRatios(f: Int, dayRatios: List<Double>, windowDays: Int = 7): Double`

| Campo | Significado de dominio | Forma | Obligatoriedad |
|-------|------------------------|-------|----------------|
| `f` | frecuencia meta del ancla (días/semana, 2–7) | `Int` | requerido |
| `dayRatios` | razón `m_i / T_i` de cada día con actividad en la ventana | `List<Double>` (solo se usan los `> 0`) | requerido (vacío → `R = 0`) |
| `windowDays` | horizonte de la ventana en días vividos | `Int`, default `7` | opcional; default `7` = comportamiento maduro |
| **salida** | `R` del ancla | `Double ∈ [0, 1.5]` | — |

`windowDays` MUST estar en `[1, 7]`. Valores fuera de rango SHALL clamparse a ese
rango antes de calcular (un caller que pase `windowDays > 7` u `8+` obtiene el
comportamiento de `7`; `0` o negativo obtiene el de `1`).

`f_eff = min(f, windowDays)` es la **frecuencia efectiva de ventana**: en una ventana
parcial no se puede comprometer más días de los vividos. `f_eff` gobierna los términos
de superhábit de la ventana (`sd`, `wt`). `f` crudo se mantiene SOLO donde el modelo
maduro lo exige (ver reglas).

---

## Requirements

### Requirement: Default windowDays=7 es byte-idéntico al modelo actual

Con `windowDays = 7` (o ausente, por default), `rFromRatios` MUST producir un `Double`
byte-idéntico al de la implementación actual para CUALQUIER `(f, dayRatios)`. Ningún
término cambia: `phi = commit/f`, `cut = min(d, f)`, `sd = if (f < 7) v/(7-f) else 0`,
`wt = (f/7)^κ`. La generalización es una refactorización transparente cuando `N = 7`,
porque `f_eff = min(f, 7) = f` para todo `f ∈ [2,7]`.

#### Scenario: f=3, semana completa con superhábit de días, windowDays default

- GIVEN `f = 3`, `dayRatios = [1.0, 1.0, 1.0, 0.5, 0.5]` (5 días con actividad)
- WHEN `rFromRatios(3, dayRatios)` se invoca sin pasar `windowDays`
- THEN el resultado es byte-idéntico a `rFromRatios(3, dayRatios, windowDays = 7)`
- AND `f_eff = min(3, 7) = 3`, por lo que `sd = v/(7-3)` y `wt = (3/7)^κ` igual que hoy

#### Scenario: f=7, semana completa, sin término de días

- GIVEN `f = 7`, `dayRatios = [1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0]`
- WHEN `rFromRatios(7, dayRatios, windowDays = 7)` se invoca
- THEN `sd = 0.0` (porque `f_eff = 7` no es `< 7`)
- AND el resultado es byte-idéntico al actual

#### Scenario: La suite existente de AnchorScoringPolicy queda verde sin tocarse

- GIVEN la suite actual de `AnchorScoringPolicyTest` (sin parámetro `windowDays`)
- WHEN se ejecuta tras agregar `windowDays` con default `7`
- THEN todos los tests pasan sin modificación (cero regresión)

---

### Requirement: windowDays<7 reparte el superhábit sobre la ventana parcial

Con `windowDays = N < 7`, los términos que el modelo maduro divide por `7` MUST
dividirse por `N`, y la frecuencia efectiva de ventana es `f_eff = min(f, N)`:

- `phi = commit.sumOf{ u } / f` — **divisor `f` crudo, SIN cambio.** `phi` mide
  cumplimiento del compromiso meta; la meta es semanal y no se prorratea. Un día que
  cumple su ratio aporta a `phi` lo mismo en arranque que maduro.
- `cut = min(d, f)` — **SIN cambio.** `d` es la cantidad de días con actividad (≤ N por
  construcción), por lo que en ventana parcial `cut` ya queda acotado naturalmente.
- `sd = if (f_eff < N) v / (N - f_eff) else 0.0` — divisor generalizado `7 → N` usando
  `f_eff`. Reparte el superhábit de días sobre los días de ventana restantes.
- `wt = (f_eff / N).pow(κ)` — peso convexo generalizado `7 → N` usando `f_eff`. Queda
  en `[0, 1]` porque `f_eff ≤ N`.
- `st = commit.sumOf{ max(it-1,0) } / f` — divisor `f` crudo, SIN cambio (superhábit de
  tiempo se mide contra la meta semanal).
- `base = 1 - (1 - phi) * exp(-λ_v · v)` y el gate `base + base^p · s` — SIN cambio
  estructural.

La generalización NO castiga los días no llegados: con `N` chico, `phi` puede alcanzar
1.0 cumpliendo el compromiso en los días vividos, y `base` no arrastra penalización por
días futuros.

#### Scenario: f=3, N=4, ventana parcial sin penalizar días futuros

- GIVEN `f = 3`, `dayRatios = [1.0, 1.0, 1.0]` (3 días vividos, todos cumplidos), `N = 4`
- WHEN `rFromRatios(3, dayRatios, windowDays = 4)` se invoca
- THEN `cut = min(3, 3) = 3`, `commit = [1.0, 1.0, 1.0]`, `vol = []`, `v = 0`
- AND `phi = (1+1+1)/3 = 1.0`
- AND `f_eff = min(3, 4) = 3`; `sd = 0/(4-3) = 0.0`; `wt = (3/4)^κ`
- AND `base = 1 - (1-1.0)·exp(0) = 1.0`
- AND el resultado es `1.0` (compromiso pleno, sin superhábit, sin castigo por el día 4 no vivido)

#### Scenario: f=2, N=4, superhábit de días repartido sobre la ventana

- GIVEN `f = 2`, `dayRatios = [1.0, 1.0, 1.0, 1.0]` (4 días, los 4 cumplidos), `N = 4`
- WHEN `rFromRatios(2, dayRatios, windowDays = 4)` se invoca
- THEN `cut = min(4, 2) = 2`, `commit = [1.0, 1.0]`, `vol = [1.0, 1.0]`, `v = 2.0`
- AND `phi = (1+1)/2 = 1.0`
- AND `f_eff = min(2, 4) = 2`; `sd = v/(N - f_eff) = 2.0/(4-2) = 1.0`
- AND `wt = (2/4)^κ`
- AND `s = smax · (1 - exp(-(wt·st + (1-wt)·sd)/s0))` con `st = 0` → `s = smax·(1-exp(-((1-wt)·1.0)/s0))`
- AND el resultado es `base + base^p · s` con `base = 1.0`, mostrando superhábit positivo repartido sobre `N=4`, no sobre 7

---

### Requirement: f ≥ N no produce división por cero ni peso fuera de rango

Cuando `f ≥ windowDays` (caller pide más frecuencia meta que días vividos; p. ej.
`f = 5, N = 2`), la fórmula NO debe dividir por cero ni por un número negativo, ni
producir un peso `wt > 1`. La regla es: `f_eff = min(f, N)` para los términos de
ventana. En consecuencia:

- `sd = if (f_eff < N) … else 0.0`. Cuando `f ≥ N`, `f_eff = N`, por lo que la guarda
  `f_eff < N` es falsa y `sd = 0.0` (NO se evalúa `v/(N - f_eff)` = `v/0`). El
  superhábit de días no aplica: con tan pocos días vividos no hay días "extra" que
  premiar por encima del compromiso.
- `wt = (f_eff / N).pow(κ) = (N/N)^κ = 1.0`. El peso queda en `1.0` (todo el peso al
  superhábit de tiempo `st`), nunca `> 1`.
- `cut = min(d, f)` con `d ≤ N ≤ f` → `cut = d`: todos los días vividos entran al
  compromiso, `vol` queda vacío, `v = 0`.

Esto cierra la validación numérica fina que el proposal dejó abierta: `f ≥ N` es un
estado válido y acotado, no un crash ni un `NaN`/`Infinity`.

#### Scenario: f=5, N=2 — guarda contra división por cero

- GIVEN `f = 5`, `dayRatios = [1.0, 0.8]` (2 días vividos), `N = 2`
- WHEN `rFromRatios(5, dayRatios, windowDays = 2)` se invoca
- THEN `cut = min(2, 5) = 2`, `commit = [1.0, 0.8]`, `vol = []`, `v = 0.0`
- AND `f_eff = min(5, 2) = 2`
- AND `sd = 0.0` (guarda `f_eff < N` es `2 < 2` = falso; NUNCA se calcula `v/(2-2)`)
- AND `wt = (2/2)^κ = 1.0` (peso acotado, no `> 1`)
- AND `phi = (u(1.0) + u(0.8))/5` (divisor `f = 5` crudo) y el resultado es finito, sin `NaN` ni `Infinity`

#### Scenario: f=7, N=1 — un solo día vivido, sin crash

- GIVEN `f = 7`, `dayRatios = [1.0]` (1 día vivido cumplido), `N = 1`
- WHEN `rFromRatios(7, dayRatios, windowDays = 1)` se invoca
- THEN `cut = min(1, 7) = 1`, `commit = [1.0]`, `vol = []`, `v = 0.0`
- AND `f_eff = min(7, 1) = 1`; `sd = 0.0` (`1 < 1` falso); `wt = (1/1)^κ = 1.0`
- AND el resultado es finito (`base = 1 - (1-phi)·exp(0)`, con `phi = u(1.0)/7`), sin `NaN` ni `Infinity`

#### Scenario: dayRatios vacío con windowDays parcial

- GIVEN `f = 3`, `dayRatios = []`, `N = 4`
- WHEN `rFromRatios(3, dayRatios, windowDays = 4)` se invoca
- THEN `d = 0` → el resultado es `0.0` (misma guarda temprana que hoy, independiente de `windowDays`)

---

### Requirement: windowDays se clampa a [1,7]

`rFromRatios` MUST clampar `windowDays` al rango `[1, 7]` antes de calcular. Esto
protege contra callers que pasen valores absurdos y garantiza que el modelo maduro
(`= 7`) sea el tope superior.

#### Scenario: windowDays > 7 se trata como 7

- GIVEN `f = 3`, `dayRatios = [1.0, 1.0, 1.0]`, `windowDays = 9`
- WHEN `rFromRatios(3, dayRatios, windowDays = 9)` se invoca
- THEN el resultado es idéntico a `rFromRatios(3, dayRatios, windowDays = 7)`

#### Scenario: windowDays <= 0 se trata como 1

- GIVEN `f = 3`, `dayRatios = [1.0]`, `windowDays = 0`
- WHEN `rFromRatios(3, dayRatios, windowDays = 0)` se invoca
- THEN el resultado es idéntico a `rFromRatios(3, dayRatios, windowDays = 1)`

---

## Restricciones y reglas de negocio

- **Local-first / dominio puro:** `AnchorScoringPolicy` permanece sin dependencias de
  Room ni Compose. `windowDays` es un `Int` plano; la policy no decide su valor (lo
  decide el use case de arranque). El cálculo sigue íntegro en `Double`.
- **Cero regresión:** el contrato maduro es intocable. La única forma admisible de
  introducir `windowDays` es con default `7` y `f_eff = min(f, 7) = f`. Cualquier
  cambio que altere el resultado con `windowDays = 7` viola esta spec.
- **Sin migraciones Room** (Camino A): este lote es 100% dominio puro JVM, no toca
  esquema ni persistencia.
- **Constantes:** `windowDays` NO es una constante de `ScoringConstants` — es un
  parámetro de entrada. El default `7` mantiene la semántica "semana" del modelo. El
  `7` que hoy está hardcodeado en `sd`/`wt` se reemplaza por `N` (el parámetro), no por
  una constante nueva.

## Criterios de aceptación

- Test (`testDebugUnitTest`): `rFromRatios(f, ratios)` sin `windowDays` == `rFromRatios(f, ratios, 7)`
  para una batería de `(f, ratios)` representativa → cero regresión.
- Test: la suite `AnchorScoringPolicyTest` existente queda verde sin modificarse.
- Test: escenarios `f=3,N=4` y `f=2,N=4` producen los valores de superhábit repartidos
  sobre `N` descritos arriba (assert numérico con tolerancia).
- Test: escenarios `f=5,N=2`, `f=7,N=1` y `dayRatios=[]` producen resultados finitos
  (no `NaN`, no `Infinity`) — guarda de división por cero verificada.
- Test: `windowDays = 9` == `windowDays = 7` y `windowDays = 0` == `windowDays = 1` (clamp).
- Runtime: install limpio, app arranca sin crashear (ver `verificacion-por-capas.md`).
