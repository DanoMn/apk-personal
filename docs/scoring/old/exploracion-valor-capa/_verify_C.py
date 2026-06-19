#!/usr/bin/env python3
"""
Subagente C — Modelo UNIFICADO RELACIONAL del valor de capa.
Verificacion de los 12 casos limite. Corre con python3 sin dependencias.
"""
import math

# ----------------------------------------------------------------------------
# CAJA NEGRA: rendimiento del ancla (consolidado, no se toca). R in [0, 1.5]
# ----------------------------------------------------------------------------
def R(F, T, mins, gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0, smax=0.5, s0=0.5):
    marked = sorted([m for m in mins if m > 0], reverse=True); D = len(marked)
    if D == 0: return 0.0
    r = [m / T for m in marked]
    commit, vol = r[:min(D, F)], r[min(D, F):]
    u = lambda x: min(x, 1.0) ** gamma
    phi = sum(u(x) for x in commit) / F
    V = sum(u(x) for x in vol)
    base = 1 - (1 - phi) * math.exp(-lam_v * V)
    St = sum(max(x - 1, 0) for x in commit) / F
    Sd = V / (7 - F) if F < 7 else 0.0
    wt = (F / 7) ** kappa
    S = smax * (1 - math.exp(-(wt * St + (1 - wt) * Sd) / s0))
    return base + (base ** p) * S

# ----------------------------------------------------------------------------
# PARAMETROS RELACIONALES (un K por tipo de miembro). Calibrables.
# ----------------------------------------------------------------------------
K_OPT  = 2.0    # peso relacional del opt-in (miembro pesado) -> despejado de axioma de estado
K_SUP  = 0.30   # soporte (miembro medio-bajo)
K_TASK = 0.08   # task con capa (miembro muy bajo, < soporte, no neutra)
B_SLEEP = 0.55  # base de senal de sueno sin dato (no tira a 0)
B_BROKE = 0.0   # senal de un track de sobriedad recaido (held=1)
A_SOB  = 0.5    # severidad del golpe de recaida en la senal de Conducta
# EJE (axioma 3): la BASE sola llega COMO MAXIMO a "En marcha". Plenitud/Inq SOLO via extra.
# Por eso el estado se arma como: estado = EM_TOP * base_global + extra_global
# con base_global in [0,1] -> base perfecta toca EM_TOP (tope de En marcha) y NO entra a Plenitud.
# El canal extra (>=0) es el unico que cruza a Plenitud/Inquebrantable.
EM_TOP = 0.85          # base perfecta cae justo en el tope de En marcha
# bandas sobre ESTADO = EM_TOP*base + extra
BANDS = [("Rojo", 0.40 * EM_TOP), ("Atencion", 0.62 * EM_TOP),
         ("En marcha", EM_TOP + 1e-9),
         ("Plenitud", EM_TOP + 0.25), ("Inquebrantable", float("inf"))]

def banda(estado):
    for nombre, tope in BANDS:
        if estado < tope: return nombre
    return "Inquebrantable"

# ----------------------------------------------------------------------------
# SENALES DE OPT-IN -> M in [0,1]
# ----------------------------------------------------------------------------
def sleep_M(noches, m_base=B_SLEEP):
    """7 noches, algunas None. M = cobertura*avg(datos) + (1-cobertura)*base."""
    datos = [x for x in noches if x is not None]
    if not datos: return m_base
    c = len(datos) / 7.0
    return c * (sum(datos) / len(datos)) + (1 - c) * m_base

def sobriety_M(tracks, a=A_SOB):
    """tracks: lista de 'held'/'broke'.
    Cada recaida aplica un golpe MULTIPLICATIVO (1-a) por recaida, contando recaidas
    NO tracks: M = (1-a) ** n_recaidas.
    - 0 recaidas -> M=1 (todo held).
    - 1 recaida -> M=(1-a), IGUAL con 1 o N tracks (no se diluye: solo cuenta cuantas se rompieron).
    - 2 recaidas -> M=(1-a)^2 < 1 recaida (pega mas, sin premiar tener mas tracks).
    Evita min()/worst-term; multiplicativo puro."""
    n_broke = sum(1 for t in tracks if t == "broke")
    return (1 - a) ** n_broke

# ----------------------------------------------------------------------------
# CAPA: base (K-weighted) y extra (solo anclas)
# ----------------------------------------------------------------------------
def layer_size(n_anchors, has_optin, n_support, n_task):
    """tamano(capa) = (1 si hay anclas) + K_OPT*opt + K_SUP*sat(sup) + K_TASK*sat(task).
    El bloque de anclas vale 1 sin importar n (axioma 5). Saturacion en sop/task."""
    size = 1.0 if n_anchors > 0 else 0.0
    if has_optin: size += K_OPT
    # saturacion multi-soporte: suma con retornos decrecientes -> no fabrica banda
    if n_support > 0:
        size += K_SUP * (1 - math.exp(-n_support))  # ~K_SUP para n>=2
    if n_task > 0:
        size += K_TASK * (1 - math.exp(-n_task))
    return size

def layer_base(anchor_Rs, optin_M, support_done, task_done,
               n_support=0, n_task=0):
    """base de capa in [0,1]: promedio K-ponderado de senales de miembros, capadas en 1.
    Cada miembro aporta size_miembro * senal_miembro / size_total."""
    members = []  # (size, signal in [0,1])
    if anchor_Rs:
        anchor_block = sum(min(r, 1.0) for r in anchor_Rs) / len(anchor_Rs)
        members.append((1.0, anchor_block))
    if optin_M is not None:
        members.append((K_OPT, optin_M))
    if n_support > 0:
        s_size = K_SUP * (1 - math.exp(-n_support))
        members.append((s_size, support_done))   # support_done in [0,1] = fraccion cuidada
    if n_task > 0:
        t_size = K_TASK * (1 - math.exp(-n_task))
        members.append((t_size, task_done))
    if not members: return 0.0
    tot = sum(s for s, _ in members)
    return sum(s * v for s, v in members) / tot

def layer_extra(anchor_Rs):
    """extra de capa >= 0: excedente sobre meta, SOLO anclas. Promedio de (R-1)+."""
    if not anchor_Rs: return 0.0
    return sum(max(r - 1, 0) for r in anchor_Rs) / len(anchor_Rs)

# ----------------------------------------------------------------------------
# GLOBAL: pesos relacionales por tamano; base = sum(peso*base_capa);
# extra = sum(peso_norm-entre-capas-con-ancla * extra_capa)
# ----------------------------------------------------------------------------
def engine(layers):
    """layers: lista de dicts con keys:
       anchor_Rs(list), optin_M(float|None), support_done, task_done,
       n_support, n_task, n_anchors"""
    sizes = []
    for L in layers:
        sizes.append(layer_size(len(L["anchor_Rs"]),
                                L["optin_M"] is not None,
                                L.get("n_support", 0), L.get("n_task", 0)))
    tot = sum(sizes)
    weights = [s / tot for s in sizes]

    base_global = 0.0
    for w, L in zip(weights, layers):
        base_global += w * layer_base(L["anchor_Rs"], L["optin_M"],
                                      L.get("support_done", 0.0),
                                      L.get("task_done", 0.0),
                                      L.get("n_support", 0), L.get("n_task", 0))

    # EXTRA: solo entre capas con anclas. Peso = size_anclas / sum(size_anclas).
    # El bloque de anclas pesa 1 en cada capa con anclas -> reparto por nro de capas-con-ancla.
    anc_idx = [i for i, L in enumerate(layers) if L["anchor_Rs"]]
    extra_global = 0.0
    if anc_idx:
        for i in anc_idx:
            extra_global += (1.0 / len(anc_idx)) * layer_extra(layers[i]["anchor_Rs"])

    estado = EM_TOP * base_global + extra_global   # eje: base sola tope En marcha
    return base_global, extra_global, estado, banda(estado), weights

# ============================================================================
# CASOS LIMITE
# ============================================================================
def fmt(x): return f"{x:.4f}"
rows = []

def run(name, layers, expect):
    b, e, s, band, w = engine(layers)
    rows.append((name, fmt(b), fmt(e), fmt(s), band, expect))
    return b, e, s, band, w

# Caso 1: cumplir todo JUSTO (R=1 exacto, opt-ins bien) -> EN MARCHA, no Plenitud
L_just = [
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": None, "n_anchors":1},        # Interior, R=1
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": sleep_M([1.0]*7), "n_anchors":1}, # Cuerpo+sueno ok
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": sobriety_M(["held"]), "n_anchors":1}, # Conducta+sobr ok
]
run("1. Todo justo (R=1, opt-ins ok)", L_just, "EN MARCHA")

# Caso 2: superhabit repartido -> Plenitud/Inquebrantable
L_super = [
    {"anchor_Rs": [R(3,30,[45,45,45,45,45])], "optin_M": None, "n_anchors":1},
    {"anchor_Rs": [R(3,30,[45,45,45,45,45])], "optin_M": sleep_M([1.0]*7), "n_anchors":1},
    {"anchor_Rs": [R(3,30,[45,45,45,45,45])], "optin_M": sobriety_M(["held"]), "n_anchors":1},
]
run("2. Superhabit repartido", L_super, "Plenitud/Inq")

# Caso 3: capa SOLO opt-in (D4): valor = senal opt-in; no exporta extra
L_d4 = [
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": None, "n_anchors":1},   # Interior
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": None, "n_anchors":1},   # Proyecto
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": None, "n_anchors":1},   # Vinculos
    {"anchor_Rs": [], "optin_M": sleep_M([0.9]*7), "n_anchors":0},          # Cuerpo SOLO sueno
]
b,e,s,band,w = run("3. Capa solo opt-in (D4)", L_d4, "base=senal, extra solo anclas")

# Caso 4: el apreton: 3 capas + AMBOS opt-ins -> pesos suman 1
L_apreton = [
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": None, "n_anchors":1},
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": sleep_M([0.9]*7), "n_anchors":1},   # Cuerpo
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": sobriety_M(["held"]), "n_anchors":1},# Conducta
]
b,e,s,band,w = run("4. Apreton 3 capas + 2 opt-ins", L_apreton, f"pesos suman {sum(w):.2f}")

# Caso 5: mal sueno/recaida hunde SU capa pero NO el extra de otras
L_malsueno = [
    {"anchor_Rs": [R(3,30,[45,45,45,45,45])], "optin_M": None, "n_anchors":1},          # superhabit
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": sleep_M([0.2]*7), "n_anchors":1},    # Cuerpo mal sueno
    {"anchor_Rs": [R(3,30,[45,45,45,45,45])], "optin_M": None, "n_anchors":1},          # superhabit
]
b,e,s,band,w = run("5. Mal sueno: hunde base, no extra", L_malsueno, "extra sobrevive")

# Caso 6: sin dato de sueno -> base, no piso
M_nodata = sleep_M([None]*7)
L_nodata = [
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": None, "n_anchors":1},
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": M_nodata, "n_anchors":1},
    {"anchor_Rs": [R(3,30,[30,30,30])], "optin_M": None, "n_anchors":1},
]
run(f"6. Sin dato sueno (M={M_nodata:.2f}, no 0)", L_nodata, f"M={B_SLEEP}")

# Caso 7: recaida dentro 7d penaliza; fuera no.
M_in  = sobriety_M(["broke"])     # recaida dentro de ventana
M_out = sobriety_M(["held"])      # fuera de 7d -> el pipeline entrega held
L_in  = [{"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":None,"n_anchors":1},
         {"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":M_in,"n_anchors":1}]
L_out = [{"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":None,"n_anchors":1},
         {"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":M_out,"n_anchors":1}]
b_in,_,s_in,band_in,_   = run("7a. Recaida DENTRO 7d", L_in, "penaliza")
b_out,_,s_out,band_out,_= run("7b. Recaida FUERA 7d (held)", L_out, "no penaliza")

# Caso 8: multi-sobriedad: 1 recaida entre 3 tracks ~ igual que entre 1
M_1of1 = sobriety_M(["broke"])
M_1of3 = sobriety_M(["broke","held","held"])
# (se reporta abajo en tabla extra)

# Baseline: anclas R~0.9 (no perfectas, para que soporte/task tengan margen para mover)
RA = R(3,30,[27,27,27])   # ~0.9 cumplimiento, deja espacio bajo el tope
def L_with(sup=None, task=None, n_sup=0, n_task=0):
    return [
        {"anchor_Rs":[RA],"optin_M":None,"support_done":(sup or 0.0),"task_done":(task or 0.0),
         "n_support":n_sup,"n_task":n_task,"n_anchors":1},
        {"anchor_Rs":[RA],"optin_M":None,"n_anchors":1},
        {"anchor_Rs":[RA],"optin_M":None,"n_anchors":1},
    ]
# Caso 9: soportes full vs descuidados (mismo nro de soportes) -> light, multi-soporte satura
b_sf,_,s_sf,_,_ = run("9a. Soportes full (3, cuidados)", L_with(sup=1.0, n_sup=3), "light +")
b_sb,_,s_sb,_,_ = run("9b. Soportes descuidados (3, 0)", L_with(sup=0.0, n_sup=3), "light -")
# saturacion multi-soporte: 1 soporte vs 8 soportes (full) NO debe fabricar banda
_,_,s_sup1,_,_  = engine(L_with(sup=1.0, n_sup=1))
_,_,s_sup8,_,_  = engine(L_with(sup=1.0, n_sup=8))

# Caso 10: tasks aportan poco, MENOS que un soporte (mismo escenario, 1 unidad full)
b_t,_,s_t,_,_   = run("10. Task full (1)", L_with(task=1.0, n_task=1), "< soporte")
_,_,s_sup1f,_,_ = engine(L_with(sup=1.0, n_sup=1))   # 1 soporte full, mismo baseline
_,_,s_base,_,_  = engine(L_with())                    # baseline sin sup ni task

# Caso 11: mas anclas en una capa NO cambia el peso de la capa
L_1anc = [
    {"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":None,"n_anchors":1},
    {"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":None,"n_anchors":1},
    {"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":None,"n_anchors":1},
]
L_3anc = [
    {"anchor_Rs":[R(3,30,[30,30,30]),R(3,30,[30,30,30]),R(3,30,[30,30,30])],"optin_M":None,"n_anchors":3},
    {"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":None,"n_anchors":1},
    {"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":None,"n_anchors":1},
]
_,_,_,_,w1 = engine(L_1anc)
_,_,_,_,w3 = engine(L_3anc)

# Caso 12: N=3,4,5 -> peso del opt-in baja al crecer N
def optin_weight_for_N(N):
    layers = [{"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":(sleep_M([0.9]*7) if i==0 else None),"n_anchors":1}
              for i in range(N)]
    _,_,_,_,w = engine(layers)
    return w[0]  # peso de la capa con opt-in

# ----------------------------------------------------------------------------
# SALIDA
# ----------------------------------------------------------------------------
print("="*92)
print("MODELO C — UNIFICACION RELACIONAL. Parametros: K_OPT=%.2f K_SUP=%.2f K_TASK=%.2f B_SLEEP=%.2f A_SOB=%.2f"
      % (K_OPT,K_SUP,K_TASK,B_SLEEP,A_SOB))
print("="*92)
hdr = f"{'Caso':<38}{'base':>8}{'extra':>8}{'estado':>9}  {'banda':<16}{'esperado'}"
print(hdr); print("-"*92)
for name,b,e,s,band,exp in rows:
    print(f"{name:<38}{b:>8}{e:>8}{s:>9}  {band:<16}{exp}")
print("-"*92)
print()
print("VERIFICACIONES PUNTUALES:")
_,e_d4,_,_,wd4 = engine(L_d4)
print(f"  C3 (solo opt-in): pesos={[round(x,3) for x in wd4]}  capa Cuerpo(solo sueno)={wd4[3]:.4f}; extra_global={e_d4:.4f} (no exporta extra)")
print(f"  C4 (apreton): suma de pesos = {sum(w):.6f} (debe ser 1.0)")
print(f"  C5 (mal sueno): extra_global={engine(L_malsueno)[1]:.4f} (>0, sobrevive el superhabit)")
print(f"  C7 (recaida): DENTRO estado={s_in:.4f} ({band_in}) vs FUERA estado={s_out:.4f} ({band_out})  -> dentro < fuera: {s_in < s_out}")
print(f"  C8 (multi-sobr): M(1 recaida de 1)={M_1of1:.4f}  M(1 recaida de 3)={M_1of3:.4f}  -> ~igual: {abs(M_1of1-M_1of3)<1e-9}")
print(f"  C8b: M(2 recaidas de 3)={sobriety_M(['broke','broke','held']):.4f} (>1 recaida pega mas, no se premia tener tracks)")
print(f"  C9 (soportes): full estado={s_sf:.4f} vs descuidado estado={s_sb:.4f}  delta={s_sf-s_sb:.4f} (light, mueve bordes)")
print(f"  C9 saturacion: 1 soporte full estado={s_sup1:.4f}  vs 8 soportes full estado={s_sup8:.4f}  delta={s_sup8-s_sup1:.4f} (8 no fabrican banda)")
d_task = s_t - s_base
d_sup  = s_sup1f - s_base
print(f"  C10 (tasks vs soporte, 1 unidad full sobre mismo baseline {s_base:.4f}):")
print(f"       aporte task={d_task:.5f}   aporte soporte={d_sup:.5f}   -> task < soporte: {d_task < d_sup}")
print(f"  C11 (mas anclas): peso capa con 1 ancla={w1[0]:.4f}  con 3 anclas={w3[0]:.4f}  -> igual: {abs(w1[0]-w3[0])<1e-9}")
print(f"  C12 (N crece): peso opt-in  N=3:{optin_weight_for_N(3):.4f}  N=4:{optin_weight_for_N(4):.4f}  N=5:{optin_weight_for_N(5):.4f}  -> baja: {optin_weight_for_N(3)>optin_weight_for_N(4)>optin_weight_for_N(5)}")

# ============================================================================
# DESPEJE DE K desde un AXIOMA DE ESTADO (no a dedo)
# Axioma del dueno (ejemplo): "anclas perfectas en TODAS las capas pero NO dormi
# en toda la semana (M_sueno=0) -> el estado debe caer a ATENCION (banda < EM)".
# Con N capas, una con opt-in de peso K. base_global = sum(peso*base_capa).
# Capa Cuerpo: base = 1/(1+K)*anclas(=1) + K/(1+K)*M(=0) = 1/(1+K).
# Las otras (N-1) capas: base=1. Pesos: Cuerpo size=1+K, otras size=1 c/u.
# Tot = (1+K)+(N-1). peso_cuerpo=(1+K)/Tot, peso_otra=1/Tot.
# base_global = peso_cuerpo*(1/(1+K)) + (N-1)*peso_otra*1
#            = [1 + (N-1)] / Tot = N / (N + K)
# estado = EM_TOP * N/(N+K). Pedimos estado = umbral_atencion = 0.62*EM_TOP.
#   N/(N+K) = 0.62  ->  K = N*(1-0.62)/0.62 = N*0.6129
# Para N=4: K ~ 2.45. Para N=3: K ~ 1.84. (Relacional a N, como pide el axioma 4.)
print()
print("DESPEJE DE K_OPT desde axioma de estado ('anclas perfectas + M_sueno=0 -> Atencion'):")
for N in (3,4,5):
    target = 0.62  # fraccion de EM_TOP donde arranca Atencion
    K_solved = N*(1-target)/target
    # verificar
    layers = [{"anchor_Rs":[R(3,30,[30,30,30])],"optin_M":(0.0 if i==0 else None),"n_anchors":1}
              for i in range(N)]
    # usar K_solved temporalmente
    import importlib, types
    globals()['K_OPT'] = K_solved
    b,e,s,band,w = engine(layers)
    print(f"  N={N}: K_OPT={K_solved:.3f} -> estado={s:.4f} banda={band} (objetivo: tope de Atencion {0.62*EM_TOP:.4f})")
globals()['K_OPT'] = 2.0  # restaurar
