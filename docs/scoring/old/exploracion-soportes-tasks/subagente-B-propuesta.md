# Subagente B — SOPORTES y TASKS: "todo unificado en el motor"

> **Estado: propuesta divergente (1 de 3).** Modela cómo entran SOPORTES y TASKS al motor de
> scoring v4 con el sesgo del Subagente B: **una sola maquinaria unificada**. No es contrato hasta
> el merge del orquestador. Verificación reproducible: `python3 subagente-B-verificacion.py`.
> Fecha: 2026-06-15 · Proyecto: `apk-personal` · Marco cerrado que respeta: A1-A10, O1-O13, §16-NUEVO.

---

## 1. Filosofía (el sesgo de B)

El motor ya tiene una pieza que funciona y está cerrada: el **término-sombra de peso dinámico**
`w = BETA·N·(1−M)` con el que entran los opt-ins (sueño/sobriedad). Es elegante, continuo, plano en
`N` y, sobre todo, **invisible cuando la señal está bien** (`M=1 → w=0`). El sesgo de B es **no
inventar un canal nuevo para soportes**: un soporte es, conceptualmente, lo mismo que un opt-in —
una base diaria que, descuidada, debe **arrastrar levemente** sin tocar lo que valen las anclas ni
el superhabit. Entonces **reuso la misma estructura O1-O13** (sin tocarla) con un **`BETA_SUP`
chico**, despejado de los axiomas de estado del dueño. Las tasks son la excepción simétrica: como
**nunca restan** y son un "empujón motivacional", no pueden ser una sombra (las sombras arrastran);
entran como un **aporte real al canal EXTRA, solo hacia arriba y saturante**, con un tope duro tan
chico que un usuario sin esfuerzo de anclas jamás se "compra" un estado. El resultado es **un solo
motor** (anclas + 2 sombras: opt-in y soporte; + 1 bonus saturante: task), todo continuo y
diferenciable, sin un gramo de gate/cap/worst-term nuevo. La jerarquía `anclas > soportes > tasks`
no se impone con reglas: **emerge de las magnitudes** (rango ancla 0.998 ≫ soporte 0.310 ≫ task 0.080).

---

## 2. Axiomas de SOPORTES (S1…S7) — estilo O1-O13

### S1 — El soporte solo toca la BASE, nunca el EXTRA
Igual que el opt-in (O1). Un soporte sostenido es **cimiento**, no gloria. No genera superhabit; su
señal está topada en 1 por diseño (`min(días/7, 1)`).
- **Mecanismo:** el soporte es un término-sombra en el canal base; `extra_global` se sigue calculando
  **solo** con `max(R_i−1,0)` de las anclas. El soporte no aparece ahí.
- **Verificación [V2]:** `Sol = Tin = 1.14411` con soportes 7/7 en todas las capas → superhabit intacto.

### S2 — Soporte BIEN = neutro exacto (define el tamaño del arrastre)
Con señal `M_sup=1` (sostenido los 7 días), el soporte es **invisible**: el ESTADO es idéntico a no
tenerlo (O2-análogo). El soporte **solo evita la caída, no regala puntos**.
- **Mecanismo:** `w_sup = BETA_SUP·N·(1−M_sup)`, con `M_sup=1 → w_sup=0` → desaparece del promedio.
- **Despeje de `BETA_SUP`:** lo fija la historia del dueño **SO2** (anclas 100% + soportes 2/7 →
  debe **bajar de Plenitud a En marcha**). Resolviendo `base_perf(M=2/7, N=3) ≈ 0.83` (margen bajo el
  borde 0.85) → `BETA_SUP ≈ 0.146`, redondeado a **`BETA_SUP = 0.15`** (el menor valor redondo que
  mete SO2 firme en EM). **No heredado** del viejo ±0.1: despejado del axioma de estado.
- **Verificación [V1]:** SO2 = 0.826 (EM) ✅.

### S3 — Soporte MAL arrastra, pero MUY levemente, y plano en N
Descuidar un soporte baja apenas (decisión dura del dueño: "puede restar, pero muy levemente"). El
arrastre **no se diluye** al haber más capas (O3-análogo).
- **Mecanismo:** el peso de la sombra escala con `N` → con anclas perfectas y soportes en piso,
  `base = 1/(1+BETA_SUP·N·...)` queda plano en `N`.
- **Verificación [V8]:** rango total del soporte (7d→0d) = **0.310** ≪ rango del ancla = **0.998**;
  drag de 1 soporte en piso (0.130) < perder 1 ancla entera (0.333). El soporte **no puede** pesar
  como un ancla.

### S4 — El soporte NO mata el valor de las anclas de su capa
Las anclas conservan su valor completo (O4-análogo): el soporte es un término aparte en el promedio,
no se mezcla dentro del bloque de anclas.
- **Verificación:** en todos los casos el `anchor_base` de cada capa es el `R` puro; el soporte solo
  agrega masa al denominador del promedio global.

### S5 — Señal de SOPORTE: días sostenidos / 7, polaridad normal, topada en 1
`M_sup = min(días_sostenidos / 7, 1)`. La UX inversa (el usuario marca lo que **no** hizo) es
presentación; internamente la lógica es positiva (más sostenido = mejor), como pide
`historias-soportes-tasks-v1.md` v2.
- **Sin dato del día = sostenido** (principio de cero fricción del dominio): la ausencia de log
  cuenta como cumplido, así que `M_sup` parte de 7/7 y solo baja con omisiones marcadas.

### S6 — Multi-soporte COMPONE su arrastre (sin tope), pero no fabrica banda
Varios soportes descuidados pegan más que uno solo (O6-análogo); cada soporte es su propia sombra y
suma masa. Pero por la magnitud chica de `BETA_SUP`, **nunca** puede hundir desde un buen lugar de
anclas a una banda catastrófica.
- **Verificación [V4]:** anclas perfectas + TODOS los soportes en piso → 0.690 (N=3) … 0.488 (N=7):
  baja a EM/A, **nunca** a Restauración. Con un solo soporte en piso queda en 0.870 (P) plano en N.

### S7 — Anti-incentivo aceptado (es opt-in por diseño)
Configurar un soporte solo puede **empatar o bajar** el ESTADO, nunca subirlo (O12-análogo). Aceptado:
el soporte es compromiso del usuario; la app muestra estados internos, no premia "coleccionar"
soportes.

---

## 3. Axiomas de TASKS (T1…T6) — estilo O1-O13

### T1 — La task aporta al EXTRA, SOLO hacia arriba, y JAMÁS resta
Una task con capa **completada** suma; una task no hecha **no existe** para el motor (no penaliza).
Decisión dura del dueño.
- **Canal elegido — EXTRA (no base):** justificación. La "ayuda mental" del dueño es **cruzar un
  umbral en el tramo final** (Plenitud → Inquebrantable). Ese umbral (`≥1.10`) vive **por encima**
  de la base topada en 1.0 — solo se alcanza con extra. Si la task fuera al canal base, **nunca**
  podría empujar a Inquebrantable (la base topa en 1). Va al **extra** porque ahí es donde la "ayuda
  mental" tiene sentido geométrico, y porque el extra ya es el canal de la gloria (coherente con que
  las tasks son un esfuerzo extra, puntual).
- **Mecanismo:** `task_bonus(n) = TASK_MAX·(1 − e^(−TASK_K·n))`, sumado a `extra_global`. Monótono
  creciente, jamás negativo.

### T2 — Multi-task SATURA (no fabrica banda)
Cerrar 1 task aporta mucho; la 2a, 3a… cada vez menos. Cerrar 50 tasks ≈ cerrar 5. Evita que un
usuario "junte tareas triviales" para inflar el estado.
- **Despeje de `TASK_K`:** se fija para que **3 tasks** (el caso del dueño en SO7) aporten ≈ el 80%
  del tope (`1−e^(−0.55·3) ≈ 0.81`), y de ahí casi nada más. `TASK_K = 0.55`.
- **Verificación [V5]:** tasks ∈ {0,1,3,5,10,50,1000} → ESTADO ∈ {1.000…1.080}; se aplana en
  `TASK_MAX`. Multi-task **no fabrica banda**.

### T3 — La task aporta MENOS que un soporte (magnitud)
Respeta `SOPORTES > TASKS`. El tope del bonus de task es **menor que el rango del soporte**.
- **Despeje de `TASK_MAX`:** el tope del aporte total de tasks debe ser `< rango del soporte (0.310)`
  y, además, lo suficientemente chico para que un cumplir-justo (ESTADO 1.0, extra=0) **no llegue a
  Inquebrantable** solo con tasks. Tope: `1.0 + TASK_MAX < 1.10 → TASK_MAX < 0.10`. Elijo
  **`TASK_MAX = 0.08`**: deja `1.0 + 0.08 = 1.08 < 1.10` (cumplir-justo nunca compra Inquebrantable)
  y `0.08 ≪ 0.310` (orden soporte>task garantizado).
- **Verificación [V8]:** rango task = 0.080 < rango soporte = 0.310 < rango ancla = 0.998 ✅.

### T4 — La "ayuda mental" SOLO funciona si hubo esfuerzo real (sin injusticia)
Las tasks cruzan Plenitud → Inquebrantable **únicamente** cuando el usuario ya tiene **superhabit
real** (extra > 0) que lo dejó en la zona alta de Plenitud (≈1.02–1.09). Un usuario sin superhabit
(ESTADO ≤ 1.0) **jamás** llega a Inquebrantable con tasks; un usuario en En marcha **jamás** salta a
Plenitud con tasks.
- **Por qué es justo:** la task **acompaña** el esfuerzo de las anclas, no lo **reemplaza**. El
  empujón motivacional existe, pero está acotado al "último tramo" de quien ya remó.
- **Verificación [V6]:** superhabit moderado (R=[35]×4 → 1.067 P) + 3 tasks → **1.132 (I)**: cruza.
  Usuario en EM (0.750) + 1000 tasks → **0.830 (sigue EM)**: NO salta. ✅

### T5 — Task NEUTRAL (sin capa / rol Neutral) NO suma
Coherente con el dominio (`nucleo-dominio-autonomia.md`): solo las tasks **con capa asignada** entran
a `tasks_total`. Una task neutral es organización personal, no estado.

### T6 — Task es continua y diferenciable (anti-gate)
El bonus saturante es `C^∞`; no introduce saltos.
- **Verificación [V7]:** barrido de tasks con paso 0.01 → paso máx |dESTADO| = 0.00044. Sin gate.

---

## 4. Fórmulas explícitas (parámetros despejados de axiomas)

```text
Señales:
  M_anchor_i  = R(F,T,mins)                       [ancla v4, NO TOCAR]
  M_optin     = sueño continuo | (1−0.55)^días    [O8/O9, NO TOCAR]
  M_sup       = min(días_sostenidos / 7, 1)       [S5]

Motor (por semana, N = capas activas):
  Para cada capa con anclas:
     anchor_base_i = promedio min(M_anchor, 1)      (peso W0 = 1)
     extra_i       = promedio max(M_anchor − 1, 0)
     si opt-in:  +término (M_optin, w = BETA·N·(1−M_optin))      BETA=0.818   [O1-O13]
     si soporte: +término (M_sup,   w_sup = BETA_SUP·N·(1−M_sup)) BETA_SUP=0.15 [S2]
  Capa solo-opt-in / solo-soporte: el término ES la capa (peso W0).

  base_global  = Σ(v·w) / Σ(w)        sobre todos los términos
  extra_global = promedio(extra_i)    [solo anclas]
  task_bonus   = TASK_MAX·(1 − e^(−TASK_K·n_tasks_con_capa))   TASK_MAX=0.08, TASK_K=0.55  [T1-T3]

  ESTADO = min(base_global, 1) + extra_global + task_bonus      escala [0, ~1.58]
  Bandas §16-NUEVO: R<0.40 · A<0.62 · EM<0.85 · P<1.10 · I≥1.10
```

**Origen de cada parámetro nuevo:**
- `BETA_SUP = 0.15` ← despejado de **SO2** (anclas 100% + soportes 2/7 cae a EM con margen). [S2]
- `TASK_K = 0.55` ← despejado de **SO7** (3 tasks ≈ 80% del tope; saturación rápida). [T2]
- `TASK_MAX = 0.08` ← despejado de **T3/T4** (`1.0 + TASK_MAX < 1.10` ⇒ cumplir-justo nunca compra
  Inquebrantable; `0.08 ≪ 0.310` ⇒ orden soporte>task). [T3]

---

## 5. Verificación numérica (`python3` — resultados reales)

Script completo: `subagente-B-verificacion.py` (en esta carpeta). Salida ejecutada:

```text
PARAMS: BETA=0.818  BETA_SUP=0.15  TASK_MAX=0.08  TASK_K=0.55
ancla: J=1.000 D75=0.750 D50=0.500 XL=1.432

[V1] HISTORIAS del dueno
  SO1: ESTADO=1.000 P  (esp P) OK
  SO2: ESTADO=0.826 EM (esp EM) OK
  SO3: ESTADO=0.750 EM (esp EM) OK
  SO4: ESTADO=0.637 EM (esp EM) OK
  SO5: ESTADO=0.500 A  (esp EM) FLIP        <-- unica divergencia, ver §7
  SO6: ESTADO=0.448 A  (esp A) OK
  SO7: ESTADO=0.815 EM (esp EM) OK

[V2] Sol=1.14411 Tin=1.14411 empatan=True            (superhabit INTACTO)
[V3] cumplir-justo ESTADO=1.0000 P (==1.0: True)     (Plenitud)
[V4] N=3..7: TODOS soportes piso = 0.690..0.488 (EM/A, nunca R); 1 soporte piso = 0.870 (P) plano
[V5] tasks 0..1000: ESTADO 1.000..1.080 (satura; tope 1.08 < 1.10, no compra Inquebrantable)
[V6] superhabit+3 tasks: 1.067 P -> 1.132 I (cruza); EM+1000 tasks: 0.750 -> 0.830 (sigue EM)
[V7] paso max |dESTADO| dM_sup=0.001 = 0.000244 ; dtasks=0.01 = 0.000439  (continuo, sin gate)
[V8] rango ancla 0.998 > soporte 0.310 > task 0.080 = True ; 1-soporte-piso 0.130 < perder-ancla 0.333
```

**Qué pasó:** (a) `Sol=Tin` exacto → superhabit intacto; (b) cumplir-justo = 1.0 = Plenitud; (c)
multi-soporte y multi-task **no fabrican banda** (arrastre plano + saturación); (d) continuidad
(pasos ~1e-4, sin gate); (e) `anclas>soportes>tasks` emerge de las magnitudes; (f) la "ayuda mental"
de las tasks es **justa** (solo cruza el tramo final si hubo superhabit real). **6/7 historias OK**;
el único flip es SO5 (§7), forzado por la filosofía, no por mala calibración.

---

## 6. Reconciliación de los dos ejes (§3 del set-prompt)

El proyecto tiene **dos ejes**: el **ESTADO/banda** (motor v4, `min(base,1)+extra+task`) y el
**número visible 700–1000** (§3.2, pre-v4). El enfoque B **mantiene los dos pero los acopla por la
base**:

- **Eje ESTADO (mi foco):** soportes y tasks entran ahí, como queda demostrado. El soporte arrastra
  la base; la task empuja el extra. La banda **emerge** del agregado de pesos puros, sin tocar las
  fronteras §16-NUEVO.
- **Eje visible 700–1000:** B **no rediseña el mapeo** (eso es el sesgo de C). Pero deja una
  recomendación coherente con su filosofía unificada: el visible debería leer la **misma `base_global`
  del motor v4** —`VisibleScore = 700 + round(clamp(base_global,0,1)·300)`— para que soportes y opt-ins
  (que viven en la base) **sí** muevan el número visible, mientras que el superhabit y las tasks (que
  viven en el extra) **no rompen el techo de 1000** (consistente con la nota original "el superhabit no
  necesita romper el techo visible"). Así el **visible refleja la base** (incluido el arrastre de
  soportes) y el **estado refleja base+extra+task** (incluido el premio de Inquebrantable). Dos
  números, una sola fuente. El cierre formal del mapeo visible bajo v4 queda para el merge con C.

---

## 7. Riesgos / lo que queda abierto

- **🔴 SO5 flip (A en vez de EM) — la divergencia honesta de B.** El dueño marcó SO5 (anclas 50% +
  soportes 7/7) como **EM**. Eso exige que **soportes perfectos LEVANTEN** un estado por encima de su
  base de anclas (de 0.500 a ≥0.62). Pero el axioma **S2** (soporte bien = neutro, como O2) lo
  **prohíbe por diseño**: una sombra solo arrastra hacia abajo, nunca sube. Es el mismo tipo de flip
  forzado que el **SB9** del `modelo-consolidado-v2.md`: ningún `BETA_SUP` mantiene SO5=EM sin un
  parche-regla prohibido (que el soporte sume al alza, rompiendo S1/S7 y el anti-incentivo).
  **Decisión que el dueño debe tomar en el merge:** o (a) re-marcar SO5 a **Atención** (acepta que los
  soportes solo evitan la caída, no rescatan un mal trabajo de anclas — consistente con
  `ANCLAS > SOPORTES`), o (b) elegir un enfoque donde el soporte **suma al alza** (eso sería el blend
  `γ` del Subagente C, que sí puede subir, a costa de perder la neutralidad O2-análoga). **B
  recomienda (a):** es lo coherente con "anclas mandan; el soporte es cimiento, no rescate".
- **🟡 `BETA_SUP`, `TASK_MAX`, `TASK_K` son de 1 sola tanda de historias (SO1-SO7).** Despejados,
  no a dedo, pero con pocas marcas. Se afinan con: soportes en capa con opt-in (interacción de dos
  sombras en la misma capa), y tasks repartidas vs concentradas en una capa.
- **🟡 Tasks por-capa vs globales.** B las agrega **globales** (`n_tasks_con_capa` total). Si el dueño
  quiere que la task "ayude" a la capa de su rol específico, habría que mover el bonus al `extra_i` de
  esa capa. No hay marca que lo exija aún; lo dejo global por simplicidad (y porque el extra ya se
  promedia entre capas).
- **🟡 Mapeo visible bajo v4** sigue sin cerrar formalmente (§6) — converge con el Subagente C.
- **Estabilidad multi-semana (§15)** sigue ortogonal y sin reconciliar con v4 (heredado, fuera de
  scope de esta sesión).
