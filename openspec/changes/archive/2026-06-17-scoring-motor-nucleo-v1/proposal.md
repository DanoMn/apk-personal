# Proposal: scoring-motor-nucleo-v1 — portar el modelo de scoring cerrado al motor real

## Intent

El motor de scoring en `app/src/main/java/dev/panopt/autonomia/domain/scoring/` es el
**modelo VIEJO/deuda**: ancla `0.70·días + 0.30·tiempo`, soporte `0.80/0.20`, task fijo
`0.05`, agregación `avg + worst-layer`, bandas con cortes en `0.70`, gates duros
(`UNBREAKABLE_*`, `WORST_LAYER_*`) y `VisibleScore = 700 + base·300`. Nada de eso refleja
el modelo que el dueño cerró, calibró y verificó esta semana.

El **modelo nuevo está cerrado, calibrado y verificado 27/27** (`verificacion_modelo_oficial.py`,
2026-06-16), con la matemática completa de 7 niveles y un blueprint Python ejecutable en
`docs/scoring/modelo-matematico-nucleo-v1.md`, su contrato de comportamiento en
`docs/scoring/axiomas-modelo-scoring-v1.md`, y auditoría doble Opus (rigor + fidelidad)
aprobada. Es un motor de **pesos puros**: cero gates/caps/worst-term duros, todo el
comportamiento EMERGE del peso × valor.

Este cambio **reemplaza el motor viejo por el modelo cerrado**, en dominio puro JVM, sin
tocar Room ni Compose. Los docs de `docs/scoring/` YA son el spec matemático; este cambio
los traduce a Kotlin con Strict TDD, usando los 27 asserts del script de verificación como
suite base.

**Por qué ahora:** el modelo está congelado y verificado; el código es la única pieza que
quedó atrás. Mientras el motor real siga corriendo el modelo viejo, el dashboard miente
(estados disparan tarde, el `1000` está oculto, los superhabit no se distinguen). Cerrar
esta brecha desbloquea cualquier trabajo de producto que dependa del estado real del usuario.

## Scope

### In Scope

- **Niveles 1–7 del núcleo** portados a Kotlin como funciones/policies de dominio puro,
  traducción casi literal del blueprint Python (`§ Implementación de referencia`):
  - NIVEL 1 — ancla `R(F, T, mins) = base + base^p·S` (Best-F, gate `base²`, superhabit
    tiempo `St` + días `Sd` saturado a `smax`).
  - NIVEL 2 — valor de capa dos canales (`base_anclas`, `extra_capa`) + soportes (blend
    `WS=0.07`) + tasks (saturación conjunta, techo `TAU=0.06`, gate `base²`, efímero diario).
  - NIVEL 3 — peso de capa por votos `(1−r^n)/(1−r)`, capa solo-soportes `ρ`, solo-opt-in `W0`.
  - NIVEL 4 — opt-ins (sueño/sobriedad): señal `M` + término-sombra `w = BETA·Σpesos·(1−M)`.
  - NIVEL 5 — agregación bolsa-global → `base_global`, `extra_global` → `ESTADO ∈ [0, 1.5]`.
  - NIVEL 6 — `ESTADO → BANDA` (Restauración…Inquebrantable) como función pura.
  - NIVEL 7 — `ESTADO → PUNTOS ∈ [650, 1100]` (enfoque E), **en la proyección, no en el motor**.
- **El ADAPTER** hechos Room → forma que el modelo espera (ver "El trabajo real" abajo). Es
  el foco de esfuerzo y de riesgo: el modelo recibe datos con forma final (lista de minutos
  POR DÍA por ancla, días sostenidos por soporte, tasks de hoy por capa, días de recaída por
  track, señal `M` de sueño); el motor viejo nunca arma esas formas.
- **`ScoringConstants.kt` reescrito** con los 17 parámetros calibrados de `§0.1` del núcleo.
- **`ScoreReport` gana `estado: Float ∈ [0, 1.5]`** (el ESTADO crudo del NIVEL 5). El motor
  emite ESTADO + banda; la proyección mapea ESTADO → puntos `[650, 1100]` (enfoque E).
- **Mapeo a puntos E en `DashboardProjection`/`ScoringScreen`** (NIVEL 7), reemplazando
  `VisibleScore = 700 + base·300`. Rango visible cambia `700–1000` → `650–1100`.
- **Suite de tests JUnit** = traducción directa de `verificacion_modelo_oficial.py` (27
  asserts), bajo `app/src/test/java/dev/panopt/autonomia/domain/scoring/`. Ampliar cobertura
  a los axiomas verificados a mano (AN12, VC4, SO6/TA6, O6, O9, PU2).
- **Preservar el seam de persistencia semanal** (constraint duro, ver Restricciones).

### Out of Scope

- **Estabilidad temporal multi-semana — APARCADA** (decisión LOCKED). El motor emite estado
  PURO de la ventana de 7 días; la banda no mira historia. `StabilityScoringPolicy` queda
  inerte/deuda. (Se conserva la persistencia de métricas semanales — eso NO es estabilidad.)
- **Cambios de esquema Room** — Camino A vigente: en dev no se escriben ni testean migraciones.
  Los datos crudos (`actualValue` + fecha por log en `daily_activity_logs`) YA existen.
- **Higiene digital a Conducta** — deuda técnica conocida, fuera de este alcance.
- **Sueño de 4 componentes** (el código usa 2) — el adapter consume la señal `M` de sueño tal
  como hoy se computa; refinar a 4 componentes es deuda separada.
- **Calibración fina futura** contra más marcas del dueño (los valores son afinables, pero el
  set calibrado de `§0.1` es el que se porta tal cual).
- **Reescritura de los docs de scoring** — ya son el spec; no se tocan como parte de este cambio.

## Capabilities

### New Capabilities

- `scoring-core-engine`: motor de scoring de pesos puros que traduce hechos semanales en
  `ESTADO ∈ [0, 1.5]` y banda, según el contrato matemático de 7 niveles
  (`docs/scoring/modelo-matematico-nucleo-v1.md`).
- `scoring-facts-adapter`: adaptador que transforma los hechos Room de la semana en las
  estructuras de entrada que el modelo exige por ancla / soporte / capa / track / sueño.
- `scoring-points-mapping`: mapeo `ESTADO → PUNTOS [650, 1100]` (enfoque E) en la proyección.

### Modified Capabilities

- `base-state-policy`: la resolución de estado pasa de bandas sobre `weeklyBaseScore` con
  gates duros e histéresis a `banda(ESTADO)` función pura sobre los cortes del NIVEL 6
  (Plenitud entra en `0.85`, Inquebrantable en `1.10`). Supersede la spec previa de
  `openspec/specs/base-state-policy/spec.md` (la spec phase reconciliará).

## Approach

### El trabajo real es el adapter, no las fórmulas

Las fórmulas se copian casi literal del Python. El esfuerzo está en el **adapter**: el modelo
`R(F, T, mins)` espera la **lista de minutos POR DÍA** de la semana; el motor viejo
(`AnchorScoringPolicy`) agrega a nivel semanal (`frequencyRatio`/`valueRatio` sobre sumas) y
nunca arma esa lista. Hay que construir, desde `weeklyLogsByActivity` en
`WeeklyScoringContextBuilder`:

- por **ancla** → `(F, T, mins[7])` (minutos por día, derivados de `actualValue` + fecha);
- por **soporte** → `días_sostenidos` (ventana 4d, UX inversa: sin registro del día = cumplido);
- por **capa** → `n_tasks_hoy` (tasks completadas HOY con capa);
- por **track** → `días de recaída` en la ventana de 7 días;
- **sueño** → señal `M` (Cuerpo).

Los datos crudos YA existen en `daily_activity_logs`. **No se toca Room.**

### Orden de implementación — bottom-up por nivel, TDD primero

Cada nivel se implementa con su test ANTES del código (Strict TDD activo). El orden respeta
las dependencias del pipeline:

1. **NIVEL 1 — ancla** `R(F, T, mins)` + casos de referencia `§1.4` (`F=3,T=30,[30,30,30]→1.000`,
   `D=0→0`, `4×60→1.289`, `6×30→1.266`, `2/4×60→0.544`, acotado en `1.5`).
2. **NIVEL 2 — valor de capa** (dos canales) + soportes (blend) + tasks (saturación conjunta).
3. **NIVEL 3 — peso de capa** (votos `1.00/1.50/1.75/…/→2.0`; `ρ`; `W0`).
4. **NIVEL 4 — opt-ins** (señal `M`; término-sombra `BETA·Σpesos·(1−M)`).
5. **NIVEL 5 — agregación** bolsa-global (`base_global` ponderado + `extra_global` plano →
   `ESTADO`). Aquí entran los casos integrados (cumplir-justo `→1.0`, `Sol=Tin`, I1/I2/I3).
6. **NIVEL 6 — bandas** `banda(ESTADO)` (cortes `0.40/0.62/0.85/1.10`).
7. **ADAPTER** hechos Room → formas del modelo (el grueso del riesgo; sus tests usan hechos
   sintéticos que reproducen `mins[7]`, días sostenidos, etc.).
8. **NIVEL 7 — puntos** en la proyección (`ESTADO → [650, 1100]`, hitos `0→650 … 1.0→941 …
   1.10→1011 … 1.5→1100`).
9. **Recableado de `ScoreEngine`**: orquesta adapter → niveles 1–6 → `ScoreReport` con `estado`,
   banda y los campos que la persistencia semanal consume.

### Mapeo viejo → nuevo

| Policy vieja | Destino nuevo |
|---|---|
| `AnchorScoringPolicy` (0.70·días + 0.30·tiempo) | NIVEL 1 `R(F,T,mins)` (Best-F + gate `base²`) |
| `SupportScoringPolicy` (0.80/0.20) | NIVEL 2.1 blend `WS=0.07` |
| `TaskMomentumPolicy` (0.05) | NIVEL 2.2 saturación conjunta (techo `TAU=0.06`, efímera diaria) |
| `LayerScoringPolicy` / `LayerContributionPolicy` | NIVEL 2 dos canales (`base_eff` + `extra`) |
| `WeeklyScorePolicy` (avg + worst) | **ELIMINAR**; NIVEL 5 bolsa-global. El worst-layer desaparece entero |
| `SobrietyScoringPolicy` + sueño-en-Cuerpo | NIVEL 4 opt-ins, término-sombra `w = BETA·Σpesos·(1−M)` |
| `SpecialLayerScoringPolicy` | revisar/eliminar (capa solo-soportes `ρ` / solo-opt-in `W0` salen del agregado) |
| `BaseStatePolicy` (gates `UNBREAKABLE_*`, histéresis, worst-min) | gutear a NIVEL 6 `banda(ESTADO)` función pura |
| `VisibleScorePolicy` (700 + base·300) | **ELIMINAR del motor**; NIVEL 7 mapeo E va a `DashboardProjection` |
| `StabilityScoringPolicy` | inerte/deuda (estabilidad aparcada) |
| `ScoringConstants` viejo | reescribir con los 17 parámetros del `§0.1`; eliminar las constantes del modelo viejo |

## Constraints & Risks

### Restricciones (no negociables)

- **Local-first / dominio puro:** el motor NO toca Room ni Compose; el scoring NO se calcula
  en ViewModel ni Compose. El esquema Room NO cambia (Camino A: sin migraciones en dev).
- **Preservar el seam de persistencia semanal (CONSTRAINT explícito del dueño):** seguir
  materializando por semana **ESTADO + banda + puntos** en `WeeklyScoreSnapshotEntity` vía
  `WeeklyScoreSnapshotWriter` / `BuildWeeklyScoreSnapshotUseCase`. NO eliminar ese seam.
- **Strict TDD activo:** test ANTES de código en cada nivel.
- **Idioma:** docs/respuestas en español; código/clases/commits en inglés. Conventional
  commits, sin atribución de IA.

### Riesgos

(Los riesgos se mitigaron en la ejecución: adapter con tests §1.4, gate `base^P` con P de
constantes, término-sombra `BETA·Σpesos·(1−M)`, tasks efímeras, seam preservado, Plenitud en
0.85, PRs encadenados PR-A…PR-G por la política `ask-on-risk`.)

## Rollback Plan

Cambio acotado a dominio puro JVM, sin migración de DB. Revertir = `git revert` de los commits
del cambio; el motor vuelve al modelo viejo. Sin estado persistido afectado a nivel de esquema.

## Success Criteria

- [x] Los 7 niveles del núcleo implementados en Kotlin (dominio puro JVM).
- [x] Suite JUnit verde reproduciendo los 27 asserts de `verificacion_modelo_oficial.py`.
- [x] `ScoringConstantsV2` con los 17 parámetros de `§0.1`; constantes viejas eliminadas.
- [x] `ScoreReport.estado ∈ [0,1.5]`; banda `banda(ESTADO)` cortes `0.40/0.62/0.85/1.10`.
- [x] Adapter reconstruye `(F,T,mins[7])`/`días_sostenidos`/`n_tasks_hoy`/`días_recaída`/`M`.
- [x] Mapeo E `ESTADO → [650, 1100]`; cumplir-justo 1.0→941; Inquebrantable 1.10→1011.
- [x] Seam de persistencia semanal funcionando; `WeeklyScorePolicy`/`VisibleScorePolicy`
      eliminadas; `StabilityScoringPolicy` inerte; Room sin cambios; build verde.
