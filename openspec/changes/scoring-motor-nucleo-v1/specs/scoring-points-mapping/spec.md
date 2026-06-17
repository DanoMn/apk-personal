# Especificación: scoring-points-mapping (NEW)

Cambio: `scoring-motor-nucleo-v1`
Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` § NIVEL 7 (mapeo E),
`docs/scoring/axiomas-modelo-scoring-v1.md` § 9 (PU1–PU5),
`docs/scoring/verificacion_modelo_oficial.py` (`PU1`, `PU3`, `PU4`).

## Propósito

`scoring-points-mapping` mapea `ESTADO ∈ [0, 1.5]` a PUNTOS visibles `∈ [650, 1100]` con el
enfoque E (suma de rampas logísticas), para el dashboard. Vive en la PROYECCIÓN
(`DashboardProjection` / `ScoringScreen`), NO en el motor: el motor emite ESTADO + banda; esta
capability traduce ESTADO a número visible. Reemplaza `VisibleScorePolicy` (`700 + base·300`);
el rango visible pasa de `700–1000` a `650–1100`.

---

## Requisitos

### Requirement: Mapeo E `ESTADO → PUNTOS [650, 1100]`

La proyección MUST mapear `ESTADO` a puntos con la fórmula del NIVEL 7: `σ(x)=1/(1+e^−x)`,
hitos `(c,w,A)` = `(0.18,0.10,60)·(0.55,0.11,110)·(0.83,0.09,100)·(1.07,0.055,130)·(1.35,0.13,50)`,
`raw(e)=650+Σ A·σ((e−c)/w)`, y normalización
`PUNTOS(e)=650+(raw(e)−raw(0))·450/(raw(1.5)−raw(0))`, con `e` clampeado a `[0,1.5]`. El
resultado es continuo, monótono y se mueve de a 1 punto. Los hitos y el piso/tope MUST estar en
`ScoringConstants`.

#### Scenario: PU1 — Rango [650, 1100]
- GIVEN `ESTADO = 0` y `ESTADO = 1.5`
- WHEN se computa `PUNTOS`
- THEN `PUNTOS(0) = 650` y `PUNTOS(1.5) = 1100` (±1)

#### Scenario: PU3 — Cumplir-justo = 941; Inquebrantable entra ≈ 1011
- GIVEN `ESTADO = 1.0` (cumplir-justo) y `ESTADO = 1.10` (entrada a Inquebrantable)
- WHEN se computa `PUNTOS`
- THEN `PUNTOS(1.0) = 941` (±2) y `PUNTOS(1.10) = 1011` (±3): el "1000" se gana al entrar a
  Inquebrantable, no cumpliendo justo

#### Scenario: PU4 — Monótono (de a 1 punto)
- GIVEN `ESTADO` barriendo `0..1.5` en pasos de `0.001`
- WHEN se computa `PUNTOS` en cada paso
- THEN ningún paso baja respecto del anterior (monótono no decreciente)

#### Scenario: PU5 — Hitos en los cortes de banda
- GIVEN `ESTADO ∈ {0.40, 0.62, 0.85}`
- WHEN se computa `PUNTOS`
- THEN `0.40→721`, `0.62→788`, `0.85→873` (±2 cada uno)

---

## Restricciones

- **Va en la PROYECCIÓN, no en el motor.** El motor emite ESTADO; el mapeo a puntos vive en
  `DashboardProjection` / `ScoringScreen`. Coherente con local-first (Compose solo presenta;
  el cálculo del número es dominio de proyección, no del ViewModel ni del Composable).
- **Reemplaza `VisibleScorePolicy`** (`700 + base·300`): se elimina del motor. El rango visible
  cambia `700–1000` → `650–1100`.
- **El seam de persistencia semanal** necesita un `visibleScore: Int`; el valor materializado
  en el snapshot MUST provenir de este mapeo (ESTADO → puntos), no de la fórmula vieja.
- **Constantes** (hitos, piso 650, tope 1100) en `ScoringConstants`; sin literales inline.
- **Strict TDD:** PU1/PU3/PU4 (y PU5) como tests JUnit antes del código.
- **Idioma:** código/clases/commits en inglés.

## Criterios de aceptación

- Mapeo E `ESTADO → [650, 1100]` implementado en la proyección; `PUNTOS(1.0)=941`,
  `PUNTOS(1.10)=1011`, rango `650–1100`, monótono — verificado con tests PU1/PU3/PU4(/PU5).
- `VisibleScorePolicy` (motor) eliminada; el `visibleScore` del snapshot proviene del nuevo
  mapeo.
- Build verde con `testDebugUnitTest`.
