# Especificación: scoring-core-engine

Fuente canónica (spec matemático): `docs/scoring/modelo-matematico-nucleo-v1.md`
(7 niveles + §0.1 constantes), `docs/scoring/axiomas-modelo-scoring-v1.md` (contrato
AN/VC/PC/SO/TA/AG/BA/PU), `docs/scoring/verificacion_modelo_oficial.py` (27 asserts verdes).

> Esta spec NO reescribe la matemática (esos docs YA son el spec); la traduce a
> requisitos verificables. Cada escenario WHEN/THEN reproduce un `chk(...)` del script de
> verificación con sus números EXACTOS, o un caso de referencia §1.4. La spec del modelo
> manda: si esta spec y el doc matemático difieren en un número, gana el doc.

## Propósito

`scoring-core-engine` es el motor de scoring de **pesos puros** (dominio puro JVM): dado el
estado semanal ya adaptado (anclas con `(F,T,mins[7])`, soportes con días sostenidos, tasks
de hoy por capa, opt-ins con señal `M`), produce `ESTADO ∈ [0, 1.5]` y su banda según el
contrato matemático de 7 niveles. Cero gates/caps/worst-term duros: todo comportamiento
EMERGE del peso × valor. NO conoce Room ni Compose ni la forma de los hechos crudos (eso es
`scoring-facts-adapter`); NO mapea a puntos (eso es `scoring-points-mapping`).

---

## Requisitos

### Requirement: NIVEL 1 — Ancla `R(F, T, mins)`

El motor MUST computar, por ancla, `R(F, T, mins) ∈ [0, 1.5]` exactamente como el blueprint
`§ NIVEL 1` y la implementación de referencia Python: preprocesamiento Best-F (razones
`r_i = t_i/T`, ordenadas descendente, `D` días con actividad, `commit` = mejores `min(D,F)`,
`vol` = resto), `u(r)=min(r,1)^γ`, `φ = (1/F)·Σ_commit u`, `V = Σ_vol u`,
`base = 1 − (1−φ)·exp(−λ_v·V)`, superhabit `St`/`Sd` fundidos por `wt=(F/7)^κ` saturado a
`smax`, y `R = base + base^p·S`. Las constantes `γ,λ_v,κ,p,smax,s0` MUST leerse de
`ScoringConstants` (§0.1), NUNCA hardcodearse — en particular el exponente del gate MUST ser
el parámetro `p`, no el literal `2`.

#### Scenario: AN1 — Rango acotado
- GIVEN un ancla con `F=4, T=30, mins=[600]*7`
- WHEN se computa `R`
- THEN `R ∈ [0, 1.5]` (acotado por el techo `1 + smax`)

#### Scenario: AN2 — Piso cero (D=0)
- GIVEN `F=4, T=30, mins=[]` (ningún día con actividad)
- WHEN se computa `R`
- THEN `R = 0.0`

#### Scenario: AN3 — Cumplir-justo = 1.0 exacto (cualquier F)
- GIVEN `F=4, T=30, mins=[30,30,30,30]` y por separado `F=7, T=30, mins=[30]*7`
- WHEN se computa `R` en cada caso
- THEN `R = 1.0` (±1e-9) en ambos

#### Scenario: AN6 — Gate base²: sin frecuencia el exceso no rinde
- GIVEN `F=4, T=30, mins=[60,60]` (2 de 4 días, doble tiempo)
- AND `F=4, T=30, mins=[60,60,60,60]` (4 de 4 días, doble tiempo)
- WHEN se computa el extra `max(R−1, 0)` en cada caso
- THEN el primero da extra `0.000` y el segundo da extra `> 0`

#### Scenario: AN7 — Superhabit de TIEMPO y de DÍAS
- GIVEN `F=4, T=30, mins=[60,60,60,60]` (superhabit por tiempo)
- AND `F=4, T=30, mins=[30,30,30,30,30,30]` (superhabit por días)
- WHEN se computa el extra `max(R−1, 0)` en cada caso
- THEN ambos extras son `> 0`

#### Scenario: AN8 — Monotonía (un día extra nunca baja R)
- GIVEN `F=4, T=30, mins=[30,30,30,30]`
- WHEN se agrega un quinto día con `x ∈ {1, 15, 30, 60}` minutos
- THEN `R(con día extra) ≥ R(sin día extra)` para todo `x` (±1e-9)

#### Scenario: AN10 — Invarianza de escala (depende de razones)
- GIVEN `F=4, T=30, mins=[40,30,30]` y `F=4, T=120, mins=[160,120,120]`
- WHEN se computa `R` en cada caso
- THEN ambos dan el mismo `R` (±1e-9)

#### Scenario: AN11 — Continuidad (sin saltos)
- GIVEN `F=4, T=30, mins=[x,30,30,30]` con `x` barriendo `0..200` en pasos de `0.1`
- WHEN se computa `R` en cada paso
- THEN ningún par consecutivo difiere en más de `0.02` (continuo)

#### Scenario: §1.4 — Casos de referencia verificados
- GIVEN los casos de referencia del doc §1.4
- WHEN se computa `R`
- THEN `F=3,T=30,[30,30,30] → 1.000`; `F=4,T=30,[60]*4 → 1.289`;
  `F=4,T=30,[30]*6 → 1.266`; `F=4,T=30,[60,60] → 0.544`;
  `F=7,T=30,[30]*7 → 1.0`; `F=7,T=30,[45]*7 → 1.32`; `F=7,T=30,[120]*7 → 1.499`
  (cada uno ±0.001)

---

### Requirement: NIVEL 2 — Valor de capa (dos canales) + soportes + tasks

Por capa con `n` anclas de valores `R_1..R_n`, el motor MUST producir dos canales separados:
`base_anclas = (1/n)·Σ min(R_i,1) ∈ [0,1]` y `extra_capa = (1/n)·Σ max(R_i−1,0) ∈ [0,0.5]`.
Las anclas pesan IGUAL dentro de la capa (promedio simple). Los soportes MUST mezclarse SOLO
en la base vía blend convexo `base_eff = (1−WS)·base_anclas + WS·G`, `WS=0.07`, con señal de
bloque `G = promedio(min(días_sostenidos_i/4, 1))` (NO crece con la cantidad de soportes); si
la capa no tiene anclas, `base_eff = G`. Las tasks MUST aportar SOLO al extra por saturación
conjunta (misma curva del superhabit, techo `smax`), con techo emergente `TAU=0.06`/capa, gate
`base_eff^p`, y conteo efímero `n_hoy` (tasks de HOY). Soportes y tasks NUNCA restan; ninguno
genera por encima de su techo.

#### Scenario: VC3/VC4 — Anclas pesan igual; brillar en 1 de n se diluye
- GIVEN una capa con 3 anclas donde una vale `R=1.5` y dos valen `R=1.0`
- WHEN se computa `extra_capa`
- THEN `extra_capa = (1/3)·0.5 = 0.1667` (el brillo de una se diluye en 1/3)

#### Scenario: SO2 — Soporte: blend bidireccional leve
- GIVEN una capa con un ancla `HALF = R(4,30,[15]*4)` y tres variantes: sin soporte,
  con `sup_days=[0]` (descuidado), con `sup_days=[4]` (sostenido)
- WHEN se computa el ESTADO de cada variante (capa única)
- THEN `valor(descuidado) < valor(sin soporte) < valor(sostenido)`

#### Scenario: SO4 — Bloque NO crece con la cantidad de soportes
- GIVEN una capa con `HALF` y `sup_days=[4]`, y otra con `HALF` y `sup_days=[4,4,4,4,4]`
- WHEN se computa el ESTADO de cada una
- THEN ambos ESTADOS son iguales (±1e-9): 1 soporte = 5 soportes a igual cumplimiento

#### Scenario: TA5 — Tasks: anti-abuso, el tope emerge de la saturación
- GIVEN 3 capas, cada una con un ancla `J = R(4,30,[30]*4)` y `n_tasks=100`
- WHEN se computa el ESTADO y su banda
- THEN la banda NO es `Inquebrantable` (ESTADO ≈ 1.06, queda en `Plenitud`)

#### Scenario: TA-suma — Task nunca resta
- GIVEN 3 capas con ancla `J` y baseline sin tasks (ESTADO `t0`)
- AND la misma config con una capa con `n_tasks=1` (ESTADO `t1`)
- WHEN se comparan
- THEN `t1 ≥ t0`

---

### Requirement: NIVEL 3 — Peso de capa (votos por anclas)

El motor MUST asignar a cada capa un peso por votos decrecientes
`peso(n) = Σ_{k=0}^{n−1} r^k = (1 − r^n)/(1 − r)` con `r=0.5` (`n ≥ 1`), peso `ρ=0.15` a una
capa SOLO-soportes (`n=0`), y peso `W0=1.0` a una capa SOLO-opt-in. El techo natural del voto
es `1/(1−r) = 2.0`.

#### Scenario: PC2 — Votos 1/1.5/1.75, techo < 2.0
- GIVEN `n ∈ {1, 2, 3, 50}`
- WHEN se computa `peso(n)`
- THEN `peso(1)=1.0`, `peso(2)=1.5`, `peso(3)=1.75` (±1e-9) y `peso(50) < 2.0`

#### Scenario: PC3 — Ninguna capa decide más del 50%
- GIVEN 3 capas con `n=50, n=1, n=1`
- WHEN se computa la fracción de peso de la capa saturada `peso(50)/Σpesos`
- THEN la fracción `≤ 0.50` (peor caso 3 capas = 50%)

#### Scenario: PC5 — Capa solo-soportes pesa ρ
- GIVEN una capa sin anclas (`n=0`)
- WHEN se computa su peso
- THEN `peso = ρ = 0.15` (±1e-9)

---

### Requirement: NIVEL 4 — Opt-ins (sueño/sobriedad): señal M + término-sombra

El motor MUST tratar sueño y sobriedad como opt-ins con señal `M ∈ [0,1]` (defensivamente
clamp), aportando un **término-sombra independiente** en la bolsa-global de la base:
`w = BETA · Σpesos · (1 − M)` con `BETA=0.818` y `Σpesos` = suma de pesos de TODAS las capas.
El término-sombra MUST escalar con `Σpesos` (NO con `N`). `M=1` ⟹ `w=0` (invisible). Solo
afecta la base, nunca el extra. La señal de sobriedad es `M_sobr = Π_tracks (1−A)^días_recaída`
con `A=0.55` (track limpio → 1). Dos opt-ins malos componen sus arrastres (sin tope).

#### Scenario: AG2/O3 — Arrastre PLANO en cualquier config (recaída total → 0.55)
- GIVEN config A: 3 capas, la primera con 1 ancla `J` y `optin=0.0`, las otras dos con `J`
- AND config B: 3 capas, la primera con 3 anclas `[J,J,J]` y `optin=0.0`, las otras con `J`
- WHEN se computa el ESTADO de cada una
- THEN ambos ESTADOS ≈ `0.55` (±0.01): el arrastre es plano, independiente de la config

#### Scenario: I1 — Opt-in GLOBAL: capa pesada = capa liviana
- GIVEN config con el opt-in `0.15` en una capa de 3 anclas
- AND config con el mismo opt-in `0.15` en una capa de 1 ancla
- WHEN se computa el ESTADO de cada una
- THEN ambos ESTADOS son iguales (±1e-9)

#### Scenario: O2/C2 — Neutralidad: opt-in bien no cambia nada (aun con déficit)
- GIVEN una capa con ancla en déficit `DEF = R(4,30,[30,30,30])` sin opt-in
- AND la misma con `optin=1.0`
- WHEN se computa el ESTADO de cada una
- THEN ambos ESTADOS son iguales (±1e-9): `M=1` ⟹ `w=0`

#### Scenario: I2/O11 — Capa solo-opt-in pesa normal (W0=1)
- GIVEN 2 capas con ancla `J` y una tercera capa SOLO-opt-in con `M=1.0`
- WHEN se computa el ESTADO
- THEN ESTADO `= 1.0` (±1e-9): la capa solo-opt-in pesa `W0=1`, no `ρ`

---

### Requirement: NIVEL 5 — Agregación bolsa-global → ESTADO

El motor MUST agregar todos los términos en UNA bolsa-global: por cada capa-con-anclas
`(base_eff, peso=votos)`, por cada capa solo-soportes `(G, ρ)`, por cada capa solo-opt-in
`(M, W0)`, y por cada opt-in activo el término-sombra `(M, BETA·Σpesos·(1−M))`. La base es
`base_global = Σ(valor·peso) / Σ(peso)` (ponderada). El extra es PLANO:
`extra_global = (1/k)·Σ extra_final_capa` sobre `k` capas con anclas. El resultado es
`ESTADO = min(base_global, 1) + extra_global ∈ [0, ~1.5]`. Si no hay capas activas, el motor
MUST degradar a `ESTADO = 0`.

#### Scenario: AG-just — Cumplir-justo (3 capas) = 1.0
- GIVEN 3 capas, cada una con un ancla `J = R(4,30,[30]*4)`
- WHEN se computa el ESTADO
- THEN `ESTADO = 1.0` (±1e-9)

#### Scenario: O5/Sol=Tin — Superhabit PLANO: brillar en cualquier capa rinde igual
- GIVEN config con un ancla `XL = R(4,30,[60]*7)` en la primera capa y `J` en las otras dos
- AND config con `XL` en la segunda capa y `J` en las otras dos
- WHEN se computa el ESTADO de cada una
- THEN ambos ESTADOS son iguales (±1e-9)

---

### Requirement: NIVEL 6 — Bandas `banda(ESTADO)` (función pura)

El motor MUST mapear `ESTADO` a banda como función PURA sobre los cortes
`0.40 / 0.62 / 0.85 / 1.10` (límite inferior inclusivo, superior exclusivo):
`<0.40 → Restauración`, `[0.40, 0.62) → Atención`, `[0.62, 0.85) → En marcha`,
`[0.85, 1.10) → Plenitud`, `≥1.10 → Inquebrantable` (`1.10 = 1 + δ`, `δ=0.10`). Sin gates,
sin worst-layer, sin histéresis: la banda solo mira el ESTADO de la ventana de 7 días. Los
cortes MUST estar en `ScoringConstants`. Esta resolución es la fuente única de banda; ver la
capability canónica `base-state-policy`.

#### Scenario: BA1 — Cortes R/A/EM/P/I
- GIVEN `ESTADO ∈ {0.30, 0.50, 0.70, 0.90, 1.15}`
- WHEN se computa la banda
- THEN `0.30→Restauración`, `0.50→Atención`, `0.70→En marcha`, `0.90→Plenitud`,
  `1.15→Inquebrantable`

#### Scenario: BA2 — Plenitud entra en 0.85
- GIVEN `ESTADO = 0.85` y `ESTADO = 0.84`
- WHEN se computa la banda
- THEN `0.85 → Plenitud` y `0.84 → En marcha`

#### Scenario: BA3 — Cumplir-justo (1.0) cae DENTRO de Plenitud
- GIVEN `ESTADO = 1.0` (cumplir-justo)
- WHEN se computa la banda
- THEN la banda es `Plenitud` (no Inquebrantable, no En marcha)

---

### Requirement: Salida del motor — `ScoreReport.estado` y campos del seam de persistencia

El motor MUST exponer el `ESTADO ∈ [0, 1.5]` crudo del NIVEL 5 en `ScoreReport` (campo nuevo
`estado: Float`). El motor MUST seguir poblando los campos que `WeeklyScoreSnapshotDraft`
consume hoy (`weeklyBaseScore`, `weeklyScore`, `state`, `visibleScore`, `worstLayerId`,
`stability*`), mapeados desde ESTADO/banda/puntos, de modo que `WeeklyScoreSnapshotWriter` /
`BuildWeeklyScoreSnapshotUseCase` compilen y persistan sin romperse. El motor NO calcula los
puntos visibles (eso es `scoring-points-mapping`, en la proyección); para el seam puede
recibir/mapear el valor de puntos sin contener la fórmula E.

#### Scenario: ScoreReport expone ESTADO crudo
- GIVEN cualquier evaluación del motor que produce `ESTADO = 1.0`
- WHEN se lee el `ScoreReport`
- THEN `report.estado = 1.0` (±1e-9) y `report.state` (banda) = `Plenitude`

#### Scenario: El seam de persistencia semanal sigue funcionando
- GIVEN un `ScoreReport` emitido por el motor nuevo
- WHEN `WeeklyScoreSnapshotWriter` / `BuildWeeklyScoreSnapshotUseCase` construyen el draft
- THEN el código compila y el draft se materializa con `state`/banda/puntos sin campos
  faltantes (el seam NO se rompe)

---

### Requirement: Estabilidad multi-semana APARCADA (StabilityScoringPolicy inerte)

La banda NO MUST mirar historia: la estabilidad temporal multi-semana queda fuera de alcance.
`StabilityScoringPolicy` MUST quedar inerte (no se invoca en la resolución de banda). El campo
`stabilityScore` del `ScoreReport` puede persistir como deuda (no influye en la banda).

#### Scenario: La banda no depende de la historia
- GIVEN dos evaluaciones con el mismo ESTADO pero distinta historia semanal
- WHEN se computa la banda
- THEN ambas dan la misma banda (la historia no influye)

---

## Restricciones (NIVEL motor)

- **Dominio puro JVM.** El motor NO importa Room ni Compose; el scoring NO se calcula en
  ViewModel ni Compose. (Local-first.)
- **Esquema Room sin cambios.** Camino A: en dev no se escriben ni testean migraciones.
- **Pesos puros.** Cero gates/caps/worst-term/histéresis duros: todo comportamiento EMERGE.
  Las eliminaciones explícitas (worst-layer, histéresis, `UNBREAKABLE_*`) se especifican en
  `base-state-policy`.
- **17 constantes calibradas** de `§0.1` viven en `ScoringConstants`; el exponente del gate es
  el parámetro `p`, no el literal `2`.
- **Strict TDD:** cada escenario es un test JUnit escrito ANTES del código del nivel.
  Test runner: `gradlew.bat testDebugUnitTest --tests 'dev.panopt.autonomia.domain.scoring.*'`.
- **Idioma:** código/clases/commits en inglés; docs/specs en español.

## Criterios de aceptación

- Los 6 niveles del motor (1–6) implementados en Kotlin (dominio puro JVM), traducción fiel
  del blueprint Python de `modelo-matematico-nucleo-v1.md`.
- Suite JUnit verde reproduciendo los asserts del motor de `verificacion_modelo_oficial.py`
  (ANCLA AN1/AN2/AN3/AN6/AN7/AN8/AN10/AN11, casos §1.4; PESO PC2/PC3/PC5; AGREGACIÓN+OPT-INS
  AG-just/AG2-O3/I1/O2-C2/Sol=Tin/I2-O11; SOPORTES SO2/SO4; TASKS TA5/TA-suma; BANDAS BA1/BA2),
  cada test escrito ANTES del código de su nivel.
- `ScoreReport.estado ∈ [0,1.5]` expuesto; la banda emerge de `banda(ESTADO)` con cortes
  `0.40/0.62/0.85/1.10`.
- El seam de persistencia semanal sigue funcionando (`WeeklyScoreSnapshotWriter` compila y
  persiste; build verde con `testDebugUnitTest`).
- `StabilityScoringPolicy` inerte; ninguna banda mira historia.

---

> **Estado de implementación:** Implementado y verificado en el cambio
> `scoring-motor-nucleo-v1` (archivado 2026-06-17). 366 tests verdes, `assembleDebug` verde,
> los 27 asserts del Python con test JUnit 1:1. Deudas declaradas abiertas: `LayerScore.score`
> se emite como `0f` placeholder (detalle por-capa, deuda de presentación) y
> `reasons = emptyList()` (`ScoreReasonPolicy` borrada).
