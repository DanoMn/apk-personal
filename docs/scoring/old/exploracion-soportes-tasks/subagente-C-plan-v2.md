# Subagente C — Plan v2: SOPORTES y TASKS por PRESUPUESTO / HEADROOM y MASA DE CAPA

> Forma A (dentro del valor de cada capa). Sesgo C: pensar en **espacios a llenar**.
> Motor v4 reusado verbatim. Verificación: `subagente_C_v2.py` (python3, salida pegada al final).
> Proyecto Engram: `apk-personal`. Fecha: 2026-06-16.

---

## 1. Filosofía del sesgo

Cada capa tiene **dos presupuestos cerrados** que el motor v4 ya define: la **base ∈ [0,1]** (el
cimiento) y el **extra ∈ [0, smax=0.5]** (la gloria / superhabit). En vez de inventar términos
nuevos que sumen por fuera, los SOPORTES y las TASKS **llenan el espacio que dejan las anclas dentro
de esos presupuestos**. El SOPORTE llena el *gap* de base `(1−base_anclas)` con un factor chico y
bidireccional: si las anclas dejaron base sin usar, sostener el soporte mete un toque ahí; descuidarlo
saca un toque. La TASK llena el *headroom* de extra `(smax−extra_anclas)` con saturación y un techo,
y como es un pulso de HOY se recalcula cada día (efímera). La gran ventaja del enfoque presupuesto:
los candados del motor (**techo 0.5, base topada en 1**) se respetan **gratis, por construcción** —no
hay que “recordar” caparlos porque el aporte se *clampa al espacio disponible* (`min(raw, headroom)`),
nunca lo desborda. El **peso de capa** sale de la “masa de sustancia”: una capa con anclas tiene masa
1; una capa solo-soportes tiene masa reducida (~0.35) y por eso pesa menos en el promedio global.

---

## 2. Axiomas

### Soportes (S1–S6)
- **S1 — Mecanismo de presupuesto-base.** El soporte solo opera sobre la base, llenando el gap
  `(1−base_anclas)`. Nunca toca el extra. Su techo natural es 1 (la base ya está topada).
- **S2 — Bidireccional leve.** Señal de bloque `g∈[0,1]`. Aporte `= η_s·gap·(2g−1)`: sostenido
  (g→1) suma, descuidado (g→0) resta, a la par de las anclas (g=0.5) es neutro.
- **S3 — No crece con la cantidad.** La señal de bloque es el **promedio** de las señales por
  soporte; 1 o 5 soportes pesan lo mismo (se reparten el peso del bloque).
- **S4 — Ventana indulgente.** Señal por soporte `s_i = min(días_sostenidos / 4, 1)`: con 4 días ya
  estás al 100%. Medir 7 sería abusivo.
- **S5 — Anclas ≫ soportes.** La autoridad máxima del soporte (swing total `2·η_s·gap`) es ≪ que la
  de las anclas (rango de base completo). Verificado en (h).
- **S6 — Topado por gap.** Si `base_anclas=1` no hay gap → el soporte aporta 0 (no puede pasar de 1).
  El techo de base se respeta por construcción, sin clamp ad-hoc.

### Tasks (T1–T6)
- **T1 — Mecanismo de presupuesto-extra.** La task solo opera sobre el extra, llenando el headroom
  `(smax−extra_anclas)`. Nunca toca la base. Comparte la curva de superhabit (techo 0.5).
- **T2 — Saturación por cantidad.** `task_extra_raw = C_t·(1−exp(−k_t·n))`: la 1ª task vale mucho, la
  10ª casi nada. 100 tasks ≈ techo `C_t` (anti-abuso).
- **T3 — Techo y clamp al headroom.** `task_extra = min(raw, smax−extra_anclas)`. Por construcción el
  extra total **nunca supera 0.5** aunque las anclas ya tengan superhabit alto.
- **T4 — Efímera diaria.** El aporte es función de `n_tasks de HOY`; mañana `n=0 → 0`. Pulso diario
  embebido en el motor semanal: lo que persiste son las anclas.
- **T5 — Araña cruces, no fabrica estados.** `C_t` despejado para que tasks empujen UN cruce cuando
  ya estás cerca, pero cumplir-justo+tasks en todas las capas **no** alcance Inquebrantable (ver T6).
- **T6 — Nunca resta; task sin capa no aporta.** Aporte ≥ 0 siempre. Task neutral → no entra a
  ninguna capa.

### Peso de capa (W1–W2, B8)
- **W1 — Masa de sustancia.** Peso de capa = masa: 1.0 con anclas, `m_sop≈0.35` si es solo-soportes,
  0 si está vacía. Global = promedio **ponderado por masa**: `Σ(v·m)/Σm`.
- **W2 — Continuo, sin gate.** La masa es un escalar fijo por tipo de capa; el blend ponderado es
  continuo. Una capa de poca sustancia arrastra menos el global (verificado en (d)).

---

## 3. Fórmulas explícitas (parámetros despejados de axiomas de estado)

```
# SOPORTE — señal de bloque (no crece con cantidad, ventana 4d)
g = mean_i( min(dias_i / 4, 1) )                         # None si no hay soportes

# base de la capa (presupuesto [0,1])
base = base_anclas + η_s · (1 − base_anclas) · (2g − 1)   # capa con anclas
base = g                                                  # capa solo-soportes (la señal ES la base)
base = clamp(base, 0, 1)

# TASK — aporte efímero al extra (presupuesto [0, smax])
raw       = C_t · (1 − exp(−k_t · n_tasks))
task_extra = min(raw, smax − extra_anclas)                # clamp al headroom -> respeta 0.5 gratis
extra     = min(extra_anclas + task_extra, smax)

# VALOR Y SCORE
valor_capa = min(base, 1) + extra
masa       = 1.0 (con anclas) | 0.35 (solo-soportes) | 0 (vacía)
score      = Σ(valor_capa · masa) / Σ(masa)
```

### Despeje de parámetros (NO a dedo)

- **η_s = 0.10.** Axioma S-cal: una capa con base a medias (`base_anclas=0.5`, gap=0.5) y soporte
  100% sostenido NO debe cruzar sola un estado. Peor swing dentro de capa = `η_s·gap·1`. Pidiendo
  ese toque ≈ 0.05 con gap=0.5 → `η_s = 0.05/0.5 = 0.10`. (En capa /N=3 eso es ±0.017 al score: leve.)
- **ventana = 4 días** (S4, definición del dueño; refinable 3–4).
- **C_t = 0.06** (techo de task por capa). Axioma anti-abuso T-cal: el **peor caso** es tasks-full en
  TODAS las capas → el promedio de `task_extra` por capa = `C_t`, luego `score = 1 + C_t` con
  cumplir-justo. Candado duro: ese peor caso NO debe alcanzar Inquebrantable (1.10) →
  **`C_t < 0.10`**. Además ANCLAS>SOPORTES>TASKS exige autoridad-task < autoridad-soporte; tomamos
  `C_t = 0.06` → cumplir-justo+tasks-full(todas) = **1.06 (Plenitud, no Inq)** y la autoridad-task
  (0.02 al score por capa) < autoridad-soporte máxima. El `~0.1` del dueño era ORIENTATIVO: la curva
  obliga a bajarlo a 0.06 para que el candado anti-abuso se respete.
- **k_t = 0.8** (saturación): 1 task ≈ 55% del techo, 3 ≈ 91%, 10 ≈ 99.97%, 100 ≈ techo. La 1ª task
  rinde mucho más que la 10ª (anti-abuso por saturación, complementa el techo).
- **m_sop = 0.35.** Axioma W-cal: capa solo-soportes < capa con anclas, continuo. ~1/3 de la masa de
  una capa-ancla: tiene voz (no la ignoramos), pero arrastra ~3× menos que una capa-ancla mala.

---

## 4. Verificación python3 (salida real)

Script: `docs/scoring/exploracion-soportes-tasks/subagente_C_v2.py` (motor v4 verbatim + mecanismos C).

```
Anclas ref: cumplir-justo J=1.0000  superhabit XL=1.4323  deficit DEF(3d)=0.7500
Params despejados: ETA_S=0.1 ventana=4d | TASK_CAP=0.06 K_T=0.8 | masa solo-sop=0.35
============================================================================================
(a) Sol=1.144111 Tin=1.144111  EMPATAN=True
(b) cumplir-justo score=1.000000 banda=PLENITUD  (esperado 1.0 Plenitud)
(c) base-media: sin-sop=0.8333  sop-sostenido=0.8500 (sube) sop-descuidado=0.8167 (baja)
    bidireccional leve: +0.0167 / -0.0167
    NO crece con cantidad: 1 soporte=0.850000  5 soportes=0.850000  iguales=True
(d) capa-ancla mala arrastra a 0.9167; capa solo-sop mala arrastra MENOS a 0.8511
    (solo-sop pesa menos -> arrastra menos). solo-sop OK=1.0000
(e) extra_anclas(XL)=0.4323; con tasks NUNCA pasa SMAX=0.5:
    XL + 1 task -> extra=0.4654  + 100 tasks -> extra=0.4923 (<=0.5)
    cumplir-justo + 1 task=0.0330  + 100 tasks=0.0600 (saturan ~TASK_CAP=0.06)
    reset diario: hoy n=3 -> +0.0546; mañana n=0 -> +0.0000
(f) cerca del cruce EnMarcha->Plenitud: sin task=0.8492 (EN MARCHA) -> +tasks=0.8692 (PLENITUD) [araña el cruce=True]
    cumplir-justo + 100 tasks en TODAS las capas=1.0600 (PLENITUD) -> NO compra Inquebrantable=True
(g) anti-gate soporte: paso max |dScore| con d(señal)=0.001 -> 0.000058 (continuo)
    anti-gate tasks: paso max |dScore| con d(n_task)=0.25 -> 0.003625 (continuo, satura)
(h) AUTORIDAD máx por capa: ANCLA(base 0->1)=0.3333 > SOPORTE(swing sobre gap)=0.0467 > TASK(0->100)=0.0200
    orden ANCLAS>SOPORTES>TASKS = True
```

### Lectura de cada caso
- **(a) Sol=Tin** intacto (1.144111 = 1.144111): soportes/tasks no tocan el extra de anclas → el
  superhabit sigue rindiendo igual en cualquier capa (O5).
- **(b)** cumplir-justo = score 1.000000 = **Plenitud** (eje semántico preservado).
- **(c)** soporte bidireccional leve (±0.0167 al score con base-media) y NO crece con cantidad
  (1 soporte = 5 soportes = 0.850000, exacto).
- **(d)** capa solo-soportes mala arrastra MENOS (0.8511 vs 0.9167 de una capa-ancla mala): pesa menos
  por masa reducida.
- **(e)** con XL (extra 0.4323) + 100 tasks el extra queda 0.4923 ≤ 0.5: **techo 0.5 respetado por
  construcción**. 100 tasks saturan a C_t=0.06. Reset diario verificado (hoy +0.0546, mañana +0).
- **(f)** tasks arañan UN cruce real (En Marcha→Plenitud) cuando estás cerca; pero cumplir-justo +
  100 tasks en TODAS las capas = 1.06 (Plenitud) → **NO compra Inquebrantable**.
- **(g)** continuo: paso máximo del score 0.000058 (soporte) y 0.0036 (tasks) → sin gate.
- **(h)** **ANCLAS (0.333) > SOPORTES (0.047) > TASKS (0.020)** en autoridad por capa.

**8/8 verde.**

---

## 5. Riesgos / lo que queda abierto

1. **Capa solo-soportes — semántica de la base.** Decidí que en una capa sin anclas la señal de
   soporte ES la base (con masa 0.35). Alternativa: tratarla como gap sobre base 0 (siempre bajaría).
   El merge debería confirmar si una capa solo-soportes “bien sostenida” merece base alta (mi opción)
   o se mantiene siempre tibia.
2. **m_sop = 0.35** es un escalar fijo. Si una capa tiene MUCHOS soportes podría argumentarse más
   masa; lo mantuve fijo para respetar S3 (no crece con cantidad). Refinable a 0.30–0.40.
3. **Interacción soporte+task en la misma capa.** Operan en presupuestos disjuntos (base vs extra),
   así que se suman sin conflicto — pero no testée el caso combinado extremo (soporte negativo + task
   alta). Es ortogonal por diseño; conviene un caso extra en el merge.
4. **C_t=0.06 vs 0.1 del dueño.** Bajé el número orientativo para respetar el candado anti-abuso. Si
   el dueño quiere tasks más “jugosas”, hay que subir el umbral de Inquebrantable o aceptar que
   tasks-full-en-todo roce 1.10. Es una decisión de estado, no técnica.
5. La saturación de task usa `k_t=0.8` calibrado por intuición de “1ª≫10ª”; podría despejarse de un
   axioma más duro (p. ej. “3 tasks = 90% del techo”) si el dueño lo fija.
