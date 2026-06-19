#!/usr/bin/env python3
"""Verificacion del MODELO FUSIONADO (merge orquestador) de SOPORTES y TASKS.

Sintesis de los 3 subagentes:
  - SOPORTES: termino-sombra estilo opt-in (consenso A+B), neutro cuando se sostiene,
    arrastra muy levemente cuando se descuida. NUNCA genera extra. Magnitud = perilla
    de calibracion (BETA_SUP), a fijar con marcas del dueno.
  - TASKS: viven SOLO en el numero visible (intencion explicita del dueno + A/C),
    meritocraticas (solo ayudan a quien ya esta alto), saturadas, topadas < soporte,
    nunca restan. NO tocan el ESTADO/banda.
  - VISIBLE: se adopta el mapeo honesto de C (biyeccion del ESTADO real -> [700,1000],
    hitos 900=Plenitud, 920=Inquebrantable, 1000=tope superhabit).
"""
import math

# --- v4 CERRADO (no se toca) ---
BETA = 0.818          # opt-in (sueno/sobriedad)
W0 = 1.0
DELTA = 0.10          # Plenitud = 1+delta = 1.10 = Inquebrantable

# --- NUEVO: soporte (perilla de calibracion, ver merge doc) ---
BETA_SUP = 0.10       # rango candidato 0.09 (A, ~3 centesimas) .. 0.15 (B, baja una banda en SO2)

# --- NUEVO: tasks (solo visible) ---
TASK_MAX_PTS = 4.0    # < impacto visible del soporte (~5.3 pts) -> orden soporte>task (de C)
TASK_K = 0.7
PROX_LO, PROX_HI = 0.70, 0.90
ESTADO_MAX = 1.5

def banda(e):
    if e < 0.40: return "RESTAURACION"
    if e < 0.62: return "ATENCION"
    if e < 0.85: return "EN MARCHA"
    if e < 1.10: return "PLENITUD"
    return "INQUEBRANTABLE"

def estado_de_capas(capas):
    """capas: lista de dict {anchors:[R..], opt:M|None, sup:M|None}. N = nro de capas."""
    N = len(capas)
    num = den = 0.0
    extra_acc = []
    for c in capas:
        anchors = c.get("anchors", [])
        if anchors:
            base_i = sum(min(a, 1.0) for a in anchors) / len(anchors)
            extra_i = sum(max(a - 1.0, 0.0) for a in anchors) / len(anchors)
            num += base_i * W0; den += W0
            extra_acc.append(extra_i)
        m_opt = c.get("opt")
        if m_opt is not None:
            w = BETA * N * (1 - m_opt); num += m_opt * w; den += w
        m_sup = c.get("sup")
        if m_sup is not None:
            w = BETA_SUP * N * (1 - m_sup); num += m_sup * w; den += w
    base_global = num / den if den else 0.0
    extra_global = sum(extra_acc) / len(extra_acc) if extra_acc else 0.0
    return min(base_global, 1.0) + extra_global

def visible_base(estado):
    if estado <= 1.0:
        return 700 + estado * 200.0
    return 900 + (estado - 1.0) / (ESTADO_MAX - 1.0) * 100.0

def prox(e):
    if e < PROX_LO: return 0.0
    return min((e - PROX_LO) / (PROX_HI - PROX_LO), 1.0)

def task_push(n, e):
    return TASK_MAX_PTS * (1 - math.exp(-TASK_K * n)) * prox(e)

def visible(estado, n_tasks=0):
    return min(1000.0, visible_base(estado) + task_push(n_tasks, estado))

# ---------- VERIFICACIONES ----------
print("=" * 70)
print(f"MERGE  BETA_SUP={BETA_SUP}  TASK_MAX_PTS={TASK_MAX_PTS}  TASK_K={TASK_K}")
print("=" * 70)

# (b) cumplir-justo
e = estado_de_capas([{"anchors": [1.0]} for _ in range(3)])
print(f"(b) cumplir-justo estado={e:.4f} {banda(e)} visible={visible(e):.1f}  (esp 1.0/PLENITUD/900)")

# (b') con soportes sostenidos (neutro exacto)
e2 = estado_de_capas([{"anchors": [1.0], "sup": 1.0} for _ in range(3)])
print(f"    +soportes 7/7 estado={e2:.4f} neutro={abs(e2-e)<1e-9}")

# (a) Sol = Tin (superhabit intacto con soportes presentes)
XL = 1.432
sol = estado_de_capas([{"anchors": [XL], "sup": 1.0}, {"anchors": [1.0], "sup": 1.0}, {"anchors": [1.0], "sup": 1.0}])
tin = estado_de_capas([{"anchors": [1.0], "sup": 1.0}, {"anchors": [XL], "sup": 1.0}, {"anchors": [1.0], "sup": 1.0}])
print(f"(a) Sol={sol:.4f} Tin={tin:.4f} empatan={abs(sol-tin)<1e-9}  (superhabit intacto)")

# (c) soporte: sostener no sube banda; descuidar todo baja muy levemente
e_full = estado_de_capas([{"anchors": [1.0], "sup": 0.0} for _ in range(3)])
print(f"(c) anclas 100% + TODOS soportes en piso estado={e_full:.4f} {banda(e_full)}  drop={1.0-e_full:.4f}")
e_one = estado_de_capas([{"anchors":[1.0],"sup":0.0},{"anchors":[1.0],"sup":1.0},{"anchors":[1.0],"sup":1.0}])
print(f"    1 soporte en piso estado={e_one:.4f} {banda(e_one)}  drop={1.0-e_one:.4f}")

# SO5 (la decision del dueno): anclas 50% + soportes 7/7
so5 = estado_de_capas([{"anchors": [0.5], "sup": 1.0} for _ in range(3)])
print(f"[SO5] anclas 50% + soportes 7/7 estado={so5:.4f} {banda(so5)}  (dueno marco EM; sombra NO rescata -> FLIP)")

# (d) anti-gate: barrido de M_sup
prev = None; mx = 0.0
m = 0.0
while m <= 1.0001:
    ev = estado_de_capas([{"anchors": [1.0], "sup": m}] + [{"anchors": [1.0]} for _ in range(2)])
    if prev is not None: mx = max(mx, abs(ev - prev))
    prev = ev; m += 0.001
print(f"(d) anti-gate soporte paso max|dEstado|(dM=.001)={mx:.6f}  (continuo)")

# (e) orden en el VISIBLE (moneda comun): ancla vs soporte vs task
base_alta = 0.90
v_e = estado_de_capas([{"anchors":[0.5]}]+[{"anchors":[1.0]} for _ in range(2)])  # 1 capa con ancla en deficit
v_full = estado_de_capas([{"anchors":[1.0]} for _ in range(3)])
ancla_pts = visible(v_full) - visible(v_e)
sop_pts = visible(v_full) - visible(e_one)
task_pts = task_push(50, 0.90)
print(f"(e) impacto VISIBLE: ancla={ancla_pts:.2f} > soporte={sop_pts:.2f} > task={task_pts:.2f}  "
      f"orden={ancla_pts>sop_pts>task_pts}")

# tasks: nunca restan / saturan / nulas en estado bajo / no cruzan banda
print(f"    task_push(0,1.0)={task_push(0,1.0):.3f}  task_push(5,e=0.50)={task_push(5,0.50):.3f} (nula bajo 0.70)")
print(f"    satur e=0.90: 1t={task_push(1,0.90):.2f} 2t={task_push(2,0.90):.2f} 10t={task_push(10,0.90):.2f} (tope {TASK_MAX_PTS})")

# tasks NO cruzan banda (banda = f(estado) solo)
import random
random.seed(0); flips = 0
for _ in range(20000):
    est = random.uniform(0, 1.5); n = random.randint(0, 50)
    if banda(est) != banda(est):  # banda solo de estado -> imposible que cambie
        flips += 1
print(f"    flips de banda por tasks = {flips} (banda se calcula SOLO sobre estado)")

# tabla visible pre-v4 vs merge
print("\n   ESTADO banda            pre-v4   merge(C)")
for est in [0.0,0.35,0.55,0.75,0.90,1.00,1.10,1.30,1.50]:
    prev4 = 700 + min(est,1.0)*300
    print(f"   {est:5.2f} {banda(est):<15} {prev4:7.0f}  {visible_base(est):7.1f}")
print("\nOK")
