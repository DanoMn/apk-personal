# Axiomas del modelo de scoring — v1 (contrato completo)

> **Estado: ✅ CONGELADO (2026-06-16) — contrato vigente.** Contrato formal de TODO el modelo de scoring, estilo O1–O13.
> Define el comportamiento que el motor DEBE cumplir. Es el doc contra el que se **verifica** el modelo
> oficial (`modelo-scoring-oficial-v1.md`). Doc NUEVO y aparte para no contaminar los contratos previos
> (`axiomas-opt-in-v1.md` sigue vigente; el del ancla en `old/exploracion-rendimiento-ancla/merge-consolidado.md`).
> Fecha: 2026-06-16. Proyecto: `apk-personal`.

---

## 0. Parámetros calibrados (la verdad numérica)

| Símbolo | Valor | Qué es |
|---|---|---|
| `γ, λ_v, κ, p, smax, s0` | 1.5 · 0.5 · 1.5 · 2.0 · 0.5 · 0.5 | parámetros del ancla `R` (cerrados) |
| `BETA` | 0.818 | intensidad del término-sombra opt-in (sueño/sobriedad) |
| `A_sob` · `B_sleep` | 0.55 · 0.5 | señal de sobriedad · sueño sin dato |
| `r` | **0.5** | decrecimiento del peso por ancla (votos) |
| `ρ` | **0.15** | peso de una capa solo-soportes (sin anclas) |
| `WS` | **0.07** | blend del soporte en la base |
| `TAU` | **0.06** | techo de aporte de tasks por capa |
| `δ` | 0.10 | margen de Inquebrantable (1+δ) |
| mapeo a puntos | piso **650**, tope **1100**, enfoque **E** | ESTADO [0,1.5] → puntos |

Principio rector (todos los axiomas lo respetan): **motor de pesos puros. Cero gates/caps/worst-term
duros. Todo comportamiento EMERGE del peso × valor; nada se fuerza con reglas-parche.**

---

## 1. ANCLA (contrato detallado A1–A10 + verificación en `old/exploracion-rendimiento-ancla/merge-consolidado.md`)

- **AN1 — Rango.** `R ∈ [0, 1+smax] = [0, 1.5]`. (A1)
- **AN2 — Piso cero.** Nada hecho (D=0) → `R=0`. (A2)
- **AN3 — Cumplir-justo = 1.0 exacto.** Cumplir frecuencia y tiempo prometidos → `R=1.0` (cualquier F/T). (A3)
- **AN4 — Estructura `R = base + base²·S`.** `base ∈ [0,1]` (¿cumpliste?); `S` = superhabit.
- **AN5 — Frecuencia DOMINA (P1, estructural).** La base se promedia sobre `F` slots; un día faltante es
  un 0 que el tiempo de otros días NO puede rellenar. Ningún parámetro compra esto.
- **AN6 — Superávit subordinado a la base — gate `base²` (P2).** Sin cimiento no hay gloria; con base
  incompleta el exceso casi no rinde. *Verif:* 3 días×60 (meta 4×30) → extra 0.000; 4 días×60 → 0.289.
- **AN7 — Superhabit de TIEMPO y de DÍAS, saturado (techo `smax=0.5`).** `St` (pasarte en minutos) y `Sd`
  (más días que la meta), combinados por `wt=(F/7)^κ`. *Verif:* 4×60→0.289; 6×30→0.266; 6×60→0.401.
- **AN8 — Monotonía.** Más días o más tiempo NUNCA baja `R`. (A4/A5)
- **AN9 — Piso del voluntario.** Un día voluntario trivial (ε→0) no resta; aporta ≥0. (A7)
- **AN10 — Invarianza de escala.** `R` depende de razones, no de magnitudes: `T=30,[40,30,30]` ≡
  `T=120,×4`. F entra solo como `F/7` y `V/(7−F)`. (A10)
- **AN11 — Continuidad.** `R` es continuo (sin saltos) al variar días/tiempo. (A9)
- **AN12 — Constancia > ráfaga.** Cumplir la frecuencia pesa más que un pico de un solo día.
> *(A6 y A8 no figuran enunciados en el merge-consolidado; las propiedades estructurales P1/P2 están
> cubiertas por AN5/AN6. Si aparece la lista formal completa del ancla, reconciliar la numeración.)*

## 2. VALOR DE CAPA (dos canales)

- **VC1 — Dos canales separados.** Cada capa produce `base_capa ∈ [0,1]` ("¿en pie?") y
  `extra_capa ∈ [0,0.5]` ("¿se destacó?"). Nunca se mezclan.
- **VC2 — `valor_capa = min(base_capa,1) + extra_capa`.**
- **VC3 — Anclas dentro de la capa pesan IGUAL (promedio simple).** `base_capa = avg min(R,1)`,
  `extra_capa = avg max(R−1,0)`. *Verif:* un déficit en gym, comer o caminar da el mismo `base_capa`.
- **VC4 — Forma 1: brillar/fallar en 1 de varias anclas se DILUYE.** Como es promedio, pasarte en 1
  de 3 anclas cuenta 1/3. Una capa brilla cuando brilla en todas sus anclas.

## 3. PESO DE CAPA (nuevo — votos decrecientes)

- **PC1 — El peso de una capa lo da su cantidad de anclas.** Más anclas = capa más importante = pesa más.
- **PC2 — Votos decrecientes `r=0.5`.** `peso(n) = Σ_{k=0}^{n−1} r^k` → 1 ancla=1.00, 2=1.50, 3=1.75…
  techo natural **2.0**. Cada ancla nueva suma la mitad que la anterior. *Verif:* `1/(1−0.5)=2.0`.
- **PC3 — Límite de la filosofía: ninguna capa decide más del 50% del score.** Con el mínimo de 3 capas
  y una saturada, peor caso `2.0/4.0 = 50%`. *Verif:* peor caso 3 capas = 50%.
- **PC4 — El peso es de la CAPA, no de las anclas individuales.** Los "votos" derivan el peso de capa;
  dentro de la capa todas las anclas siguen pesando igual (VC3).
- **PC5 — Capa solo-soportes (sin anclas) pesa `ρ=0.15`** (reducido, no inflado). Al agregar 1 ancla,
  su peso se normaliza (pasa a la escala PC2). *Verif:* capa solo-soportes abandonada arrastra ≈ −25 pts
  (mismo orden que descuidar soportes reales), no infla cuando está bien (+0).

## 4. OPT-INS — sueño/sobriedad (contrato O1–O13 en `axiomas-opt-in-v1.md`)

Término-sombra **independiente** en la bolsa-global de la base: `w = BETA·Σpesos·(1−M)`, `BETA=0.818`
(generaliza el `BETA·N` de v4 al peso de capa variable; Σpesos=N si los pesos son iguales). Sueño→Cuerpo,
sobriedad→Conducta. **Feature separada de soportes/tasks** (no se mezclan). NO vive "dentro del valor de
una capa": pesa aparte en la base global.
- **O1** solo base, nunca extra · **O2** opt-in bien = neutro exacto (M=1→w=0) · **O3** opt-in mal
  arrastra fuerte y **plano en N** · **O4** no mata el valor de las anclas de su capa · **O5** no
  distorsiona el superhabit (`Sol=Tin`; extra con pesos iguales) · **O6** dos opt-ins malos componen
  (sin tope) · **O7** sueño y sobriedad pegan igual (mismo `BETA`) · **O8** señal de sueño continua, sin
  dato `B_SLEEP=0.5` (4 componentes: duración/continuidad/horario/interrupción digital) · **O9** señal
  de sobriedad `(1−0.55)^días_recaída`, multi-track = producto · **O10** ventana de 7 días · **O11** capa
  solo-opt-in: el opt-in ES la capa · **O12** anti-incentivo aceptado · **O13** ⚠ ver cambio.
- **⚠ O13 CAMBIÓ (2026-06-16).** Decía: "el opt-in no sube el peso de la capa; todas las capas pesan
  IGUAL (1/N)". → La parte **"el opt-in no infla el peso de su capa" SIGUE vigente**; la parte **"todas
  pesan 1/N" quedó REEMPLAZADA** por el peso de capa variable (PC1–PC5). Interacciones de O3/O11 con el
  peso variable: ver §9-bis (pendientes).

## 5. SOPORTES (nuevo — blend leve en la base)

- **SO0a — Capa obligatoria, sin targets.** Todo soporte pertenece a exactamente una capa; no tiene meta
  de frecuencia, tiempo ni duración (a diferencia del ancla).
- **SO0b — Cero fricción / UX inversa.** El sistema asume el soporte **cumplido por defecto**; el usuario
  solo desmarca lo que NO hizo. **Sin registro del día = sostenido** (la ausencia de dato no penaliza,
  análogo a O8 del sueño). Solo se registran omisiones. Es presentación; internamente más sostenido = mejor.
- **SO1 — Mecanismo propio, solo BASE de su capa.** El soporte nunca genera extra. Su techo es 1.
- **SO2 — Blend convexo bidireccional leve.** `base_eff = (1−WS)·base_anclas + WS·G_soporte`, `WS=0.07`.
  Sostener (si las anclas no están llenas) sube un toque; descuidar baja un toque; a la par = neutro.
  *Verif:* descuidar soportes de una capa ≈ −13 pts; sostener en capa floja ≈ +5; en capa perfecta = 0.
- **SO3 — Señal indulgente por soporte.** `s_i = min(días_sostenidos / 4, 1)` (con 4 días ya al 100%;
  medir 7 sería abusivo).
- **SO4 — Bloque NO crece con la cantidad.** `G_soporte = promedio(s_i)`. 1 o 5 soportes pesan lo mismo.
  *Verif:* 1 soporte = 5 soportes (mismo cumplimiento) → mismo valor.
- **SO5 — Capa sin anclas: la señal de soporte ES la base** (`base_eff = G_soporte`), con peso `ρ` (PC5).
- **SO6 — ANCLAS ≫ SOPORTES.** El swing máximo del soporte (±0.07·base) es mucho menor que el de un ancla.

## 6. TASKS (nuevo — empujón efímero al superhabit)

- **TA1 — Solo EXTRA, dentro de la curva.** La task aporta al superhabit de su capa pasando por la
  MISMA saturación (techo 0.5) y el MISMO gate `base²`. Nunca suma crudo por fuera.
- **TA2 — Saturación conjunta, techo `TAU=0.06`/capa.** Comparte la curva del superhabit de anclas
  (re-saturación reparametrizada). *Verif:* 1 task ≈ +8 pts, tope +41 pts (inviable), nunca pasa 0.5.
- **TA3 — Diaria y efímera.** Cuenta las tasks de HOY; mañana se resetea. Pulso diario en motor semanal.
- **TA4 — Nunca resta. Neutral no suma.** Task con capa aporta; sin capa / rol Neutral no entra.
- **TA5 — El tope EMERGE de la saturación, no es regla.** Cumplir-justo + ∞ tasks **no compra
  Inquebrantable** (queda < 1.10). El esfuerzo de élite se gana con anclas, no con tasks. *Verif:*
  cumplir-justo + 100 tasks/capa → ESTADO 1.06 (Plenitud), nunca Inquebrantable.
- **TA6 — ANCLAS > SOPORTES > TASKS** en impacto (ancla 0.22 > soporte 0.07 > task 0.06 por unidad).

## 7. AGREGACIÓN GLOBAL (bolsa-global de la base + extra plano)

- **AG1 — La base es una BOLSA-GLOBAL de términos** `base_global = Σ(valor·peso)/Σ(peso)` sobre: cada
  capa-con-anclas → (base_eff, peso_capa=votos); cada opt-in → (M, BETA·Σpesos·(1−M)); capa solo-soportes
  → (G, ρ); capa solo-opt-in → (M, W0=1). Cumplir en una capa pesada cuenta más.
- **AG2 — El opt-in escala con Σpesos** (suma de pesos de capa), generalizando el `BETA·N` de v4. Mantiene
  el arrastre PLANO (recaída total + anclas perfectas → 0.55) con cualquier config y BETA=0.818 intacto.
  Es **global**: arrastra igual esté en capa pesada o liviana. *Verif:* 0.5501 (3 configs); I1=0.6514.
- **AG3 — Superhabit PLANO entre capas con anclas.** `extra_global = promedio simple de extra_capa`. → un
  mismo superhabit rinde igual en cualquier capa (`Sol=Tin`). *Verif:* brillar entero Cuerpo = Interior.
- **AG4 — `ESTADO = min(base_global,1) + extra_global` ∈ [0, ~1.5].**

## 8. ESTADO / BANDAS

- **BA1 — Cortes (cerrados).** `R<0.40 · A<0.62 · EM<0.85 · P<1.10 · I≥1.10` (δ=0.10).
- **BA2 — Plenitud entra en 0.85; cumplir-justo (1.0) cae DENTRO de Plenitud (zona alta), NO es su
  inicio.** Cumplimiento parcial bajo (<0.85) = En marcha/Atención; parcial alto (0.85–1.0) ya es Plenitud.
- **BA3 — `Sol=Tin` y cumplir-justo=1.0=Plenitud** se mantienen tras agregar soportes/tasks/peso. *Verif:*
  `Sol=Tin=1.1441`; 3 capas justas = 1.0.

## 9. MAPEO A PUNTOS VISIBLES (nuevo — enfoque E)

- **PU1 — `ESTADO [0,1.5] → puntos [650, 1100]`.** Piso 650 (digno, no humilla); tope 1100 (respira sobre 1000).
- **PU2 — Enfoque E: hitos-meta perseguibles.** Suma de rampas logísticas; la resolución se aprieta justo
  antes de cada número redondo. Metas: 700≈0.30 · 800≈0.65 · 900≈0.88 · 1000≈1.09.
- **PU3 — El "1000" se GANA con superhabit** (entra ≈ a Inquebrantable, estado 1.09). Cumplir-justo (1.0)
  da **941**, no 1000. *Verif:* cumplir-justo=941; entra a Inquebrantable ≈ 1011.
- **PU4 — Continuo, monótono, de a 1 punto.** El número sube de a 1 (no de a 10); aprovecha la resolución
  continua del estado. *Verif:* +1 punto cada +0.0014 (zona empinada) a +0.0051 (plana) de estado.
- **PU5 — Hitos en los cortes:** 0→650 · 0.40→721 · 0.62→788 · 0.85→873 · 1.0→941 · 1.10→1011 · 1.5→1100.

---

## 9-bis. Cambios vs contratos previos + INTERACCIONES pendientes de verificar

**Cambios (decisiones cerradas que modifican contratos anteriores):**
- **O13** "pesos de capa iguales 1/N" → **peso de capa VARIABLE** (PC1–PC5). El opt-in sigue sin inflar
  su propia capa; lo que cambió es que las capas ya no pesan igual entre sí.
- **Bandas**: resuelta la contradicción del árbol — **Plenitud entra en 0.85** (1.0 cae dentro). (BA2)
- Magnitudes viejas de soporte (±0.1) y task (0.05) → **descartadas**, recalibradas (WS=0.07, TAU=0.06).

**Interacciones opt-in × peso variable — ✅ RESUELTAS y verificadas** (`reconciliacion_optin_peso.py`):
- **I1 ✅** — El opt-in escala con `Σpesos` (no N) → arrastre **plano** (0.55) con cualquier config y
  **global** (capa pesada=liviana, 0.6514). BETA=0.818 intacto, sin recalibrar.
- **I2 ✅** — Capa solo-opt-in pesa **W0=1 (normal)**, NO ρ (heredado de O11; el opt-in es sustancia real).
- **I3 ✅** — Soporte (blend local en la base de la capa) + opt-in (término global) **coexisten** sin
  conflicto; ambos arrastran su parte (verif: Cuerpo con soportes descuidados + mal sueño = 0.6376).

## 10. Cómo se verifica este contrato
Cada axioma con "*Verif:*" tiene su comprobación numérica. El script `verificacion_modelo_oficial.py`
corre TODOS contra el modelo de `modelo-scoring-oficial-v1.md`. Un axioma en rojo = el modelo no cumple.
> **✅ RESULTADO (2026-06-16): 27/27 VERDES** — ancla (AN1–AN11), peso de capa (PC2/PC3/PC5), agregación
> + opt-ins bolsa-global (cumplir-justo, arrastre plano, I1, neutralidad, Sol=Tin, I2), soportes
> (SO2/SO4), tasks (anti-abuso/nunca-resta/efímera), bandas (BA1/BA2), puntos (PU1/PU3/PU4). 0 rojos.

## 11. Origen de cada grupo
- Ancla (AN): cerrado 2026-06-09/10. Opt-ins (OP/O1–O13): cerrado 2026-06-12.
- Valor de capa (VC), peso (PC), soportes (SO), tasks (TA), agregación (AG): sesión 2026-06-16
  (`modelo-consolidado-v3-pesos-variables.md`).
- Bandas (BA): §16-NUEVO de `old/arbol-scoring-v1.md`, con Plenitud=0.85 confirmado 2026-06-16.
- Puntos (PU): enfoque E elegido 2026-06-16 (`old/exploracion-puntos-visibles/opus-E-mapeo.md`).
