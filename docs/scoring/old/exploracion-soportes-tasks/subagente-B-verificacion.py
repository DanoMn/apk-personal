"""
Subagente B — verificacion del modelo de SOPORTES y TASKS (enfoque "todo unificado en el motor").
SOPORTE = senal opt-in con termino-sombra de peso DINAMICO PEQUENO (BETA_SUP), simetrico atenuado.
TASK    = aporte real al canal EXTRA, SOLO hacia arriba, saturante (TASK_MAX, TASK_K).
No toca ancla (A1-A10) ni opt-ins (O1-O13). Reproducible: python3 subagente-B-verificacion.py
"""
import math

# ============ ANCLA v4 — NO TOCAR (contrato A1-A10) ============
def R(F, T, mins, gamma=1.5, lam_v=0.5):
    mk = sorted([m for m in mins if m > 0], reverse=True); D = len(mk)
    if D == 0: return 0.0
    r = [m / T for m in mk]; c, v = r[:min(D, F)], r[min(D, F):]
    u = lambda x: min(x, 1.0) ** gamma
    phi = sum(u(x) for x in c) / F; V = sum(u(x) for x in v)
    base = 1 - (1 - phi) * math.exp(-lam_v * V)
    St = sum(max(x - 1, 0) for x in c) / F; Sd = V / (7 - F) if F < 7 else 0.0
    kappa = 1.5; smax = 0.5; s0 = 0.5; p = 2.0
    wt = (F / 7) ** kappa; S = smax * (1 - math.exp(-(wt * St + (1 - wt) * Sd) / s0))
    return base + (base ** p) * S

# ============ PARAMETROS ============
DELTA = 0.10; W0 = 1.0
TARGET = 0.55; BETA = 1.0 / TARGET - 1.0   # 0.818 opt-in (NO TOCAR)
BETA_SUP = 0.15   # soporte: despejado de S2 (SO2 ancla-100/sup-2/7 cae a EM con margen, ~0.826)
TASK_MAX = 0.08   # tope duro del bonus task; < rango maximo del soporte (0.31). Ver T3.
TASK_K  = 0.55    # tasa de saturacion: la 1a task aporta mucho, las siguientes decrecen. Ver T2.

def band(s):
    return ("R" if s < 0.40 else "A" if s < 0.62 else "EM" if s < 0.85
            else "P" if s < 1.0 + DELTA else "I")

def sup_signal(days):  # polaridad normal: dias sostenidos / 7, topada en 1
    return min(days / 7.0, 1.0)

def task_bonus(n):     # saturante, SOLO hacia arriba, jamas negativo
    return TASK_MAX * (1 - math.exp(-TASK_K * n)) if n > 0 else 0.0

# ============ MOTOR UNIFICADO ============
def engine(capas, tasks_total=0):
    """capas: dicts con 'anchors'(list de R), 'optin'(M sleep/sobr o None), 'sup'(M soporte o None).
       tasks_total: nº de tasks con capa completadas en la semana (suma global)."""
    act = [L for L in capas if (L.get('anchors') or L.get('optin') is not None or L.get('sup') is not None)]
    N = len(act); terms = []; extras = []
    for L in act:
        a = L.get('anchors') or []; M = L.get('optin'); Ms = L.get('sup')
        if a:
            ab = sum(min(r, 1) for r in a) / len(a); terms.append((ab, W0))
            extras.append(sum(max(r - 1, 0) for r in a) / len(a))
            if M is not None:                              # opt-in shadow (NO TOCAR)
                w = BETA * N * (1 - M)
                if w > 1e-12: terms.append((M, w))
            if Ms is not None:                             # SOPORTE shadow (peso chico)
                ws = BETA_SUP * N * (1 - Ms)
                if ws > 1e-12: terms.append((Ms, ws))
        elif M is not None: terms.append((M, W0))          # capa solo-optin
        elif Ms is not None: terms.append((Ms, W0))        # capa solo-soporte
    base = sum(v * w for v, w in terms) / sum(w for _, w in terms) if terms else 0.0
    extra = sum(extras) / len(extras) if extras else 0.0
    return min(base, 1.0) + extra + task_bonus(tasks_total)  # TASK al EXTRA, solo +

# ============ SENALES DE LAS HISTORIAS ============
J = R(4, 30, [30] * 4)        # anclas 100% (4 de 4d) -> 1.000
D75 = R(4, 30, [30, 30, 30])  # anclas 75%  (3 de 4d) -> 0.750
D50 = R(4, 30, [30, 30])      # anclas 50%  (2 de 4d) -> 0.500
XL = R(4, 30, [60] * 7)       # superhabit fuerte
def so(an, sd, tasks=0): return engine([{'anchors': [an], 'sup': sup_signal(sd)}] * 3, tasks_total=tasks)

print("PARAMS: BETA=%.3f  BETA_SUP=%.2f  TASK_MAX=%.2f  TASK_K=%.2f" % (BETA, BETA_SUP, TASK_MAX, TASK_K))
print("ancla: J=%.3f D75=%.3f D50=%.3f XL=%.3f" % (J, D75, D50, XL))
print("=" * 72)

print("\n[V1] HISTORIAS del dueno (historias-soportes-tasks-v1.md)")
rows = [("SO1", J, 7, 0, "P"), ("SO2", J, 2, 0, "EM"), ("SO3", D75, 7, 0, "EM"),
        ("SO4", D75, 2, 0, "EM"), ("SO5", D50, 7, 0, "EM"), ("SO6", D50, 2, 0, "A"),
        ("SO7", D75, 7, 3, "EM")]
for nm, an, sd, tk, exp in rows:
    s = so(an, sd, tk); b = band(s)
    print(f"  {nm}: ESTADO={s:.3f} {b:2s} (esp {exp}) {'OK' if b == exp else 'FLIP'}")

print("\n[V2] Sol=Tin INTACTO (superhabit no distorsionado por soporte ni task)")
sSol = engine([{'anchors': [XL], 'sup': 1.0}, {'anchors': [J], 'sup': 1.0, 'optin': 1.0}, {'anchors': [J], 'sup': 1.0}])
sTin = engine([{'anchors': [J], 'sup': 1.0}, {'anchors': [XL], 'sup': 1.0, 'optin': 1.0}, {'anchors': [J], 'sup': 1.0}])
print(f"  Sol={sSol:.5f} Tin={sTin:.5f} empatan={abs(sSol - sTin) < 1e-9}")

print("\n[V3] cumplir-justo = ESTADO 1.0 = inicio Plenitud (soportes 7/7, sin tasks)")
s = so(J, 7, 0); print(f"  ESTADO={s:.4f} {band(s)} (==1.0: {abs(s - 1.0) < 1e-9})")

print("\n[V4] multi-SOPORTE no fabrica banda (arrastre plano en N; mas capas no diluye)")
for N in [3, 4, 5, 6, 7]:
    s1 = engine([{'anchors': [J], 'sup': 0.0}] + [{'anchors': [J], 'sup': 1.0}] * (N - 1))
    sAll = engine([{'anchors': [J], 'sup': 0.0}] * N)
    print(f"  N={N}: 1 soporte piso={s1:.3f} {band(s1)} | TODOS piso={sAll:.3f} {band(sAll)}")

print("\n[V5] multi-TASK no fabrica banda (saturacion: no se compra Inquebrantable)")
for n in [0, 1, 3, 5, 10, 50, 1000]:
    s = engine([{'anchors': [J], 'sup': 1.0}] * 3, tasks_total=n)
    print(f"  tasks={n:4d}: ESTADO={s:.4f} {band(s)}  (tope cumplir-justo={1.0 + TASK_MAX:.2f} < 1.10)")

print("\n[V6] TASK 'ayuda mental' JUSTA: cruza P->I solo si hubo superhabit real")
sb = R(4, 30, [35] * 4)  # superhabit moderado -> ~1.067 (Plenitud alta)
b0 = engine([{'anchors': [sb], 'sup': 1.0}] * 3, 0); b3 = engine([{'anchors': [sb], 'sup': 1.0}] * 3, 3)
print(f"  superhabit moderado: sin tasks={b0:.4f} {band(b0)} | +3 tasks={b3:.4f} {band(b3)}  cruza={band(b0)=='P' and band(b3)=='I'}")
low = engine([{'anchors': [D75], 'sup': 1.0}] * 3, 0); lowt = engine([{'anchors': [D75], 'sup': 1.0}] * 3, 1000)
print(f"  EM sin esfuerzo: sin tasks={low:.4f} {band(low)} | +1000 tasks={lowt:.4f} {band(lowt)}  (NO salta de EM)")

print("\n[V7] ANTI-GATE: continuidad (barrido de soporte M y de tasks)")
prev = None; mx = 0
for i in range(1001):
    M = i / 1000; s = engine([{'anchors': [J], 'sup': M}] + [{'anchors': [J], 'sup': 1.0}] * 2)
    if prev is not None: mx = max(mx, abs(s - prev))
    prev = s
prev = None; mx2 = 0
for i in range(1001):
    n = i / 100.0; s = 1.0 + task_bonus(n)
    if prev is not None: mx2 = max(mx2, abs(s - prev))
    prev = s
print(f"  paso max |dESTADO| con dM_sup=0.001 = {mx:.6f}")
print(f"  paso max |dESTADO| con dtasks=0.01  = {mx2:.6f}  -> continuo, sin gate")

print("\n[V8] ORDEN anclas>soportes>tasks (rango MAXIMO de cada palanca, N=3)")
allzero = engine([{'anchors': [R(4, 30, [1])], 'sup': 1.0}] * 3)  # anclas casi-cero
allone = engine([{'anchors': [J], 'sup': 1.0}] * 3)
supfull = engine([{'anchors': [J], 'sup': 1.0}] * 3); supzero = engine([{'anchors': [J], 'sup': 0.0}] * 3)
ra, rs, rt = allone - allzero, supfull - supzero, TASK_MAX
print(f"  rango ancla (≈0 -> perfecto): {ra:.3f}")
print(f"  rango soporte (7d -> 0d):     {rs:.3f}")
print(f"  rango task (tope TASK_MAX):   {rt:.3f}")
print(f"  anclas>soportes>tasks: {ra > rs > rt}")
# y un solo soporte en piso nunca arrastra mas que perder una ancla entera
dsup1 = supfull - engine([{'anchors': [J], 'sup': 0.0}] + [{'anchors': [J], 'sup': 1.0}] * 2)
danc1 = supfull - engine([{'anchors': [R(4, 30, [1])], 'sup': 1.0}] + [{'anchors': [J], 'sup': 1.0}] * 2)
print(f"  drag de 1 soporte piso ({dsup1:.3f}) < perder 1 ancla entera ({danc1:.3f}): {dsup1 < danc1}")
