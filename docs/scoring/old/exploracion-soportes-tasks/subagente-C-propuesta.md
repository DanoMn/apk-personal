# Subagente C — SOPORTES (blend γ) + TASKS (mapeo visible v4)

> **Estado: propuesta divergente para merge.** Subagente C del panel de 3.
> Sesgo: *repensar la capa de presentación / mapeo visible v4*. Reusa el motor v4
> cerrado (anclas A1–A10, opt-ins O1–O13) **sin tocarlo**: solo agrega un blend de
> soportes en el canal base y **rediseña el mapeo visible** para que las tasks vivan
> ahí. Fecha: 2026-06-15. Proyecto Engram: `apk-personal`.
> Script reproducible: `exploracion-soportes-tasks/subagente_C_verificacion.py`.

---

## 1. Filosofía

El hallazgo crítico del set-prompt (§3) es que hoy hay **dos ejes desalineados**: el
número visible `700–1000` codifica solo la **base recortada** (`VisibleScore = 700 +
clamp(base)·300`, pre-v4) y **satura en 1000 cuando ESTADO=1.0** — justo donde empieza
Plenitud. El superhabit y todo el rango de **Inquebrantable son invisibles**: el número
miente sobre el estado real. Mi tesis es que la pieza que falta no es "más fórmula en el
motor", sino **honestizar la presentación**: el visible debe ser una **biyección monótona
del ESTADO real (`min(base,1)+extra`)** sobre `[0, ESTADO_MAX]`, con hitos legibles
(900 = Plenitud, 920 = Inquebrantable, 1000 = tope superhabit). Una vez que el visible
codifica el estado, las **tasks aterrizan limpias**: empujan **solo el tramo final del
número** (la "ayuda mental" del dueño), **nunca el ESTADO ni la banda** — así araña el
cruce visual sin fabricar un estado regalado. Los **soportes**, fieles a su naturaleza de
mantenimiento base, entran por un **blend γ** (como el `γ` de los moduladores v1/v2) que
modula **solo la base** de su capa, con γ pequeño despejado de un axioma de piso.

**Por qué es coherente con lo cerrado:** el motor de ESTADO (base/extra, opt-ins,
`Sol=Tin`) queda **bit-a-bit intacto** salvo un blend aditivo en `anchor_base` (que es
parte de la base, no del extra). Las tasks **no entran al motor** — viven en la capa de
presentación, que es exactamente el eje que el set-prompt declaró abierto.

---

## 2. Axiomas de SOPORTES (S1…S6) — estilo O1–O13

### S1 — El soporte solo toca la BASE de su capa, nunca el EXTRA
El soporte modula el `anchor_base` de la capa (¿está en pie?), **jamás** el `extra_capa`
(superhabit). No genera ni distorsiona superhabit.
- **Por qué:** sostener higiene/orden/agua es *cimiento*, no logro que te destaca — misma
  lógica que O1 para los opt-ins. La gloria se gana con anclas.
- **Mecanismo:** `anchor_base_eff = (1−γ)·anchor_base + γ·M_sup`; `extra_capa` se calcula
  igual que en v4 (solo `max(R_i−1,0)` de anclas). El blend está **dentro del `min(·,1)`**.
- **Verificación:** `extra(XL)=0.4323` preservado aun con `M_sup=0` en la capa-superhabit;
  `Sol=Tin=1.1441` intacto. ✅

### S2 — Soporte sostenido (M_sup=1) = neutro EXACTO
Con todos los soportes sostenidos, el score es idéntico a no tener soportes.
- **Por qué:** mantener tu base no "regala" puntos (como O2) — solo evita la pequeña caída.
- **Mecanismo:** `M_sup=1 → anchor_base_eff = anchor_base`. El blend desaparece.
- **Verificación:** sin soportes `1.0000` = todos M=1 `1.0000` → no fabrica banda. ✅

### S3 — Soporte descuidado resta MUY levemente (piso gentil)
Descuidar TODOS los soportes de una capa con anclas perfectas baja su base de `1.0` a un
**piso gentil PISO=0.92** — la capa **sigue en Plenitud**. Es la cota "muy levemente".
- **Por qué:** decisión del dueño — descuidar la base apenas baja, no es una condena.
- **Mecanismo:** **γ se despeja de este axioma**: con `anchor_base=1, M_sup=0` →
  `anchor_base_eff = 1−γ = PISO` → **`γ = 1 − PISO = 0.08`**.
- **Verificación:** todos los soportes en M=0 (N=3) → ESTADO `0.9200` = PLENITUD. ✅

### S4 — Señal del soporte: continua, y sin dato no penaliza
`M_sup ∈ [0,1]` = fracción de (soporte·día) sostenidos en la ventana de 7 días. Sin datos
del soporte → `M_sup = 1.0` (neutro), nunca 0.
- **Por qué:** análogo a O8 (sueño sin dato no tira al piso) — la ausencia de registro no
  es fracaso. La UX inversa (marcar lo que NO se hizo) es presentación; internamente
  *más sostenido = mejor* (polaridad normal).
- **Mecanismo:** `M_sup = (Σ días sostenidos) / (Σ días·soporte evaluables)`; vacío → 1.0.

### S5 — Multi-soporte agrega por promedio simple (saturación natural)
Varios soportes en una capa → `M_sup = promedio simple de las señales por soporte`.
- **Por qué:** ningún soporte individual hunde la capa; y muchos sostenidos **no fabrican
  banda** porque el techo del blend es la propia base de las anclas (no la supera).
- **Mecanismo:** el blend está topado en `anchor_base` cuando `M_sup=1`; la saturación es
  estructural (no hay término que exceda la base). Multi-soporte no agrega masa al extra.
- **Verificación:** todos M=1 = neutro; el blend nunca sube por encima de `anchor_base`. ✅

### S6 — Capa solo-soporte: el soporte ES la base de la capa
Una capa sin anclas pero con soportes vale `M_sup` (peso normal `W0`) y **no exporta extra**.
- **Por qué:** análogo a O11 (capa solo-opt-in). Sin anclas que sostener, el soporte es esa
  dimensión por sí solo; sin práctica, no puede destacarse.
- **Mecanismo:** `terms.append((M_sup, W0))`, `extra=0` para esa capa.

**Orden vs anclas (S vs A):** el impacto visible de un ancla (DEF→J en una capa) = **16.67
pts** frente a **5.33 pts** del soporte de una capa entera → **anclas > soportes** (axioma
duro). El γ=0.08 garantiza que el soporte sea estructuralmente menor.

---

## 3. Axiomas de TASKS (T1…T6) — estilo O1–O13

### T1 — Las tasks viven en el VISIBLE, jamás en el ESTADO/banda
Una task completada empuja **solo el número visible**; el ESTADO (`base+extra`) y por tanto
la banda **no se mueven**.
- **Por qué:** es el sesgo C honesto sobre el ejemplo del dueño. La banda es la verdad del
  motor; las tasks son motivación de presentación. Así nadie "compra" un estado regalado.
- **Mecanismo:** `VisibleScore = visible_from_estado(ESTADO) + task_push(n_tasks, ESTADO)`.
  La banda se calcula sobre `ESTADO`, **sin** el `task_push`.
- **Verificación:** barrido aleatorio (20 000 casos, n∈[0,50]) → **0 flips de banda** por
  tasks (banda = f(estado) solo). ✅

### T2 — Las tasks NUNCA restan
`task_push ≥ 0` siempre; una task no hecha no penaliza.
- **Por qué:** decisión del dueño — la task es puntual, su ausencia no es deuda.
- **Mecanismo:** `task_push = TASK_VISIBLE_MAX · sat(n) · prox(estado)`, todos los factores ≥ 0.
- **Verificación:** `task_push(0, ·)=0`; nunca negativo. ✅

### T3 — Magnitud de tasks < magnitud de soportes (orden duro)
El empuje **máximo** de tasks `< impacto visible de descuidar los soportes de una capa`.
- **Por qué:** `ANCLAS > SOPORTES > TASKS` en magnitud.
- **Mecanismo:** **el tope se despeja del impacto del soporte**: `SUP_VISIBLE_IMPACT ≈
  γ·(1/N)·300tramo ≈ 5.33 pts` → **`TASK_VISIBLE_MAX = 0.75 · SUP_VISIBLE_IMPACT ≈ 4.0 pts`**.
- **Verificación:** orden en el visible: ancla `16.67` > soporte `5.33` > task `4.00`. ✅

### T4 — Las tasks SATURAN (multi-task no fabrica número libre)
Muchas tasks no escalan linealmente: `sat(n) = 1 − e^(−K·n)`, `K=0.8`, asintótico a 1.
- **Por qué:** evitar que "coleccionar tasks" empuje sin límite el visible.
- **Mecanismo:** tope duro `TASK_VISIBLE_MAX`; 10 tasks ya rozan el tope.
- **Verificación:** 1t→2.20, 2t→3.19, 10t→4.00 pts. ✅

### T5 — "Ayuda mental" sin injusticia: solo empuja a quien YA empuja
Las tasks solo aportan cuando el ESTADO ya es alto (cerca de un hito): `prox(estado) = 0`
para `estado < 0.70`, rampa lineal hasta `1` en `[0.70, 0.90]`.
- **Por qué:** la idea del dueño — premiar el esfuerzo del usuario avanzado que araña el
  cruce, no rescatar al que está lejos. En estados bajos las tasks son **nulas** (no
  maquillan una base caída).
- **Verificación:** `task_push(5, estado=0.50) = 0.000`. ✅

### T6 — Tasks neutras (sin capa / rol Neutral) no aportan
Una task sin capa asignada no cuenta (consistente con el dominio §Task).
- **Por qué:** la "ayuda mental" requiere intención dirigida a una capa.
- **Mecanismo:** solo cuentan las tasks con capa en `n_tasks`.

---

## 4. Fórmulas explícitas (parámetros despejados de axiomas)

```text
# --- SOPORTES (blend γ en la base de la capa; γ de S3) ---
anchor_base_eff(capa) = (1 − γ)·anchor_base(capa) + γ·M_sup(capa)      (si la capa tiene soportes)
γ = 1 − PISO          PISO = 0.92  →  γ = 0.08                          (axioma S3)
M_sup = promedio_simple( señal_por_soporte )    señal = días_sostenidos / días_evaluables   (S4,S5)
extra_capa  : SIN CAMBIOS (solo anclas, v4)                              (S1)

# El resto del motor v4 (base_global, extra_global, opt-ins, ESTADO) queda IDÉNTICO.
ESTADO = min(base_global, 1.0) + extra_global       escala [0, ~1.5]    (CERRADO, intacto)

# --- MAPEO VISIBLE v4 (biyección monótona del ESTADO real) ---  ← reemplaza §3.2
ESTADO_MAX = 1.5
VisibleScore_base(ESTADO) =
    700 + ESTADO·200                                  si ESTADO ≤ 1.0     # tramo base [700, 900]
    900 + (ESTADO−1)/(ESTADO_MAX−1)·100               si ESTADO > 1.0     # tramo extra [900, 1000]
# Hitos legibles: 0.00→700 · 1.00→900(Plenitud) · 1.10→920(Inquebrantable) · 1.50→1000

# --- TASKS (empuje solo del visible; tope de T3, saturación T4, proximidad T5) ---
SUP_VISIBLE_IMPACT = γ·(1/N)·300_tramo ≈ 5.33 pts
TASK_VISIBLE_MAX   = 0.75 · SUP_VISIBLE_IMPACT ≈ 4.0 pts                 (axioma T3)
sat(n)   = 1 − e^(−K·n)        K = 0.8                                   (T4)
prox(e)  = 0                          si e < 0.70                        (T5)
           clamp((e−0.70)/0.20, 0, 1) si e ≥ 0.70
task_push(n, e) = TASK_VISIBLE_MAX · sat(n) · prox(e)        ≥ 0         (T2)

VisibleScore(ESTADO, n_tasks) = min(1000, VisibleScore_base(ESTADO) + task_push(n_tasks, ESTADO))
```

**Nota de continuidad:** el visible es lineal a tramos con quiebre en ESTADO=1.0 (pendiente
200 → 100 pts/unidad). Es **continuo** (sin salto); solo cambia la pendiente, lo que refleja
honestamente que el extra (superhabit) es más "caro" por punto visible que la base.

---

## 5. Verificación numérica con `python3`

Script: `exploracion-soportes-tasks/subagente_C_verificacion.py` (reproducible:
`python3 subagente_C_verificacion.py`). Resultados:

```text
==============================================================================
PARAMS GAMMA_SUP=0.080(piso=0.92) TASK_VISIBLE_MAX=4.000pts TASK_K=0.8
       BETA=0.818(opt-ins intacto) ESTADO_MAX=1.5 SUP_VISIBLE_IMPACT=5.333pts
       J=1.000 XL=1.432 DEF=0.750
==============================================================================

(b) CUMPLIR-JUSTO estado=1.0000 PLENITUD visible=900.0
(a) SUPERHABIT Sol=1.1441 Tin=1.1441 empatan=True
    extra(XL)=0.4323 preservado aun con soporte descuidado en esa capa (estado=1.1174, baja solo la base)

(c) MULTI-SOPORTE sin=1.0000 todos M=1=1.0000 no_fabrica_banda=True
    todos M=0 -> 0.9200 PLENITUD (baja muy levemente, sigue Plenitud)

(d) ANTI-GATE soportes paso max|dEstado|(dM=.001)=0.000027; visible paso max|dVis|(dE=.001)=0.2000pts

(e) ORDEN (impacto en VISIBLE, moneda comun): ancla=16.67pts > soporte=5.33pts > task=4.00pts

--- CASO DUENO: tasks empujan el tramo final del VISIBLE (banda NUNCA cambia por tasks) ---
  estado=0.835 EN MARCHA   vis_base= 867.0 | 1t-> 868.5 2t-> 869.2 5t-> 869.7 (banda fija EN MARCHA)
  estado=1.080 PLENITUD    vis_base= 916.0 | 1t-> 918.2 2t-> 919.2 5t-> 919.9 (banda fija PLENITUD)
  EJEMPLO estado=0.835 vis_base=867.0 -> 5 tasks=869.7 (+2.7 pts, arana hacia el siguiente hito)
          banda eje ESTADO sigue EN MARCHA (NO se compra Plenitud: estado<0.85). Honesto.
  GARANTIA: banda=f(estado) solo; tasks no entran al estado -> flips por tasks=0

--- TASKS nunca restan / saturan / nulas en estado bajo ---
  task_push(0,1.0)=0.000  task_push(5,e=0.50)=0.000(nula)
  satur e=1.0: 1t->2.20 2t->3.19 10t->4.00 (tope 4.00)

--- TABLA VISIBLE pre-v4(solo base) vs v4-C(estado real) ---
   ESTADO banda          pre-v4    v4-C
     0.00 RESTAURACION      700   700.0
     0.35 RESTAURACION      805   770.0
     0.55 ATENCION          865   810.0
     0.75 EN MARCHA         925   850.0
     0.90 PLENITUD          970   880.0
     1.00 PLENITUD         1000   900.0
     1.10 INQUEBRANTABLE    1000   920.0
     1.30 INQUEBRANTABLE    1000   960.0
     1.50 INQUEBRANTABLE    1000  1000.0

TODOS LOS ASSERTS PASARON OK
```

**Lectura de la tabla (el hallazgo del sesgo C):** en pre-v4 el número **satura en 1000
con ESTADO=1.0** — todo el rango de Inquebrantable (1.10–1.50) es **invisible** y el visible
**miente** (muestra 1000 en plena Plenitud). En v4-C el número **codifica el estado real**:
900 = entrada Plenitud, 920 = Inquebrantable, 1000 = tope superhabit. Los dos ejes quedan
**alineados** (cada banda tiene su rango visible legible).

Casos exigidos por §8 — todos verdes:
- **(a) `Sol = Tin`** = `1.1441` exacto, y `extra(XL)=0.4323` preservado aun con soporte
  descuidado → superhabit **intacto** (S1).
- **(b) cumplir-justo** = ESTADO `1.0000` = Plenitud → visible **900.0** (hito del ejemplo
  del dueño).
- **(c) multi-soporte** = no fabrica banda (todos M=1 ≡ sin soportes); todos M=0 baja a
  `0.9200` (sigue Plenitud) → resta **muy leve** (S3).
- **(d) anti-gate** = soportes paso máx `0.000027`; visible paso máx `0.20 pts` → continuo,
  sin saltos.
- **(e) orden** anclas `16.67` > soportes `5.33` > tasks `4.00` pts (en el visible, moneda
  común) + 0 flips de banda por tasks.

---

## 6. Reconciliación de los dos ejes (§3)

| Eje | Antes (pre-v4) | Ahora (v4-C) |
|-----|----------------|--------------|
| **ESTADO / banda** | `min(base,1)+extra`, [0,~1.5]. Cerrado, correcto. | **Idéntico** (intacto). Soportes solo modulan `anchor_base` (no el extra). Tasks **no lo tocan**. |
| **Número visible** | `700+clamp(base)·300`. Satura en 1000 con base=1; Inquebrantable invisible; **miente**. | **Biyección monótona del ESTADO real** sobre [700,1000] con hitos 900/920/1000. Codifica base+extra. |
| **Tasks** | sin hogar claro (eje abierto). | viven **solo en el visible** (`task_push`), nunca en la banda. Ayuda mental honesta. |

**Resolución del ejemplo del dueño ("897 → cruzar"):** el 897 era un artefacto del mapeo
viejo. Bajo v4-C, un usuario en EN MARCHA (estado 0.835, visible 867) que completa tasks ve
su número **subir hacia el siguiente hito** (+ hasta 4 pts) **sin** cruzar la banda — araña
el cruce visual como empujón motivacional, pero **la verdad del estado se respeta** (no se
compra Plenitud con estado < 0.85). Si quería *de verdad* cruzar, lo logra con anclas (que
mueven el ESTADO real), no con tasks.

---

## 7. Riesgos / lo que queda abierto

1. **Quiebre de pendiente en ESTADO=1.0** (200→100 pts/unidad): es continuo pero no
   suave (la derivada salta). Honesto (el extra es "más caro"), pero si se quiere C¹ se
   puede usar una curva suave (ej. logística) — a costa de hitos menos redondos.
2. **`ESTADO_MAX=1.5` como tope visible:** un superhabit extremo > 1.5 satura en 1000. Es
   raro (requeriría múltiples anclas en superhabit fuerte) pero conviene confirmar el tope.
3. **`task_push` es presentación, no estado:** dos usuarios con misma banda pueden mostrar
   números visibles distintos por tasks. Es intencional (motivación), pero hay que
   comunicarlo en UI para que no se lea como inconsistencia.
4. **γ=0.08 sin marca empírica:** despejado del axioma de piso 0.92; falta ≥1 marca del
   dueño de "descuidar soportes" para calibrar fino el piso.
5. **Interacción soporte+opt-in en la misma capa:** ambos modulan la base por canales
   distintos (blend vs término-sombra); verificado que no se rompen, pero no hay marca que
   valide la magnitud combinada.
6. **`prox(estado)` arranca en 0.70:** el umbral de "quién ya empuja" es un parámetro de
   diseño; conviene atarlo a una banda (ej. inicio de EN MARCHA) en el merge.
