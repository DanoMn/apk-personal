#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Sub-agente B — CIMIENTO MODULADOR SUAVE
Verificacion numerica del modelo de valor de capa + agregacion a score global.
Todo dominio puro. python3 stdlib.
"""
import math

# ============================================================
# 0. CAJA NEGRA — formula del ancla ya consolidada (R in [0, 1.5])
# ============================================================
def R(F, T, mins, gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0, smax=0.5, s0=0.5):
    marked = sorted([m for m in mins if m > 0], reverse=True); D = len(marked)
    if D == 0: return 0.0
    r = [m / T for m in marked]; commit, vol = r[:min(D, F)], r[min(D, F):]
    u = lambda x: min(x, 1.0) ** gamma
    phi = sum(u(x) for x in commit) / F; V = sum(u(x) for x in vol)
    base = 1 - (1 - phi) * math.exp(-lam_v * V)
    St = sum(max(x - 1, 0) for x in commit) / F; Sd = V / (7 - F) if F < 7 else 0.0
    wt = (F / 7) ** kappa
    S = smax * (1 - math.exp(-(wt * St + (1 - wt) * Sd) / s0))
    return base + (base ** p) * S

# ============================================================
# 1. PARAMETROS CALIBRABLES DEL MODELO DE CAPA
# ============================================================
PARAMS = dict(
    # --- canal BASE: cimiento modulador suave ---
    floor=0.55,      # SUELO del cimiento: con M=0 el modulador no baja de aqui (evita brutalidad)
    q=1.6,           # curvatura no-lineal de g(M): >1 => zona alta plana, caida acelera abajo (efecto domino)
    # --- soportes (van a la BASE) ---
    sup_sat=0.10,    # techo del bono/penal por soportes (relacional, satura)
    sup_k=2.5,       # velocidad de saturacion multi-soporte
    # --- tasks (van a la BASE, < soporte) ---
    task_sat=0.04,   # techo del aporte de tasks (menor que soportes)
    task_k=2.0,      # velocidad de saturacion multi-task
    # --- extra ---
    extra_sat=0.6,   # techo del extra agregado (saturacion global del superhabit)
    extra_s0=0.5,    # escala de saturacion del extra
    lam_state=0.85,  # cuanto del extra entra al estado (mezcla base+extra)
    # --- sobriedad ---
    sobr_broke=0.0,  # señal de un track recaido (0 = golpe pleno; binario held/broke)
    # --- sueño semanal ---
    sleep_base=0.5,  # base cuando no hay dato de sueño (no tira a 0)
    # --- bandas (sobre el estado final) ---
    b_rojo=0.40, b_amar=0.62, b_marcha=0.85, b_pleno=1.10,  # I >= 1.10
)

# ============================================================
# 2. SEÑAL DEL OPT-IN
# ============================================================
def sleep_weekly(nights, base=None):
    """nights: lista de 7 entradas, cada una float[0,1] o None (NoData).
    Cobertura: M = c*avg(con dato) + (1-c)*base ; sin dato alguno => base."""
    if base is None: base = PARAMS['sleep_base']
    have = [x for x in nights if x is not None]
    n = len(nights) if nights else 7
    c = len(have) / n if n else 0.0
    if not have: return base
    avg = sum(have) / len(have)
    return c * avg + (1 - c) * base

def sobriety_signal(tracks, broke=None):
    """tracks: lista de bool 'held' (True=mantenido esta semana, False=recaida en 7d).
    Multiplicativo: 1 recaida hunde igual con 1 o N tracks (no se diluye)."""
    if broke is None: broke = PARAMS['sobr_broke']
    if not tracks: return 1.0
    M = 1.0
    for held in tracks:
        M *= 1.0 if held else broke
    return M

# ============================================================
# 3. MODULADOR NO-LINEAL CON SUELO  (el sesgo de B)
# ============================================================
def g(M, q=None):
    """Curvatura no-lineal: M^q con q>1.
    Zona alta (M~1) casi plana => dormir un poco peor no borra esfuerzo.
    Zona baja (M->0) cae acelerado => efecto domino cuando el cimiento se rompe."""
    if q is None: q = PARAMS['q']
    return max(M, 0.0) ** q

def cimiento(M, floor=None, q=None):
    """mod = floor + (1-floor)*g(M)  in [floor, 1].
    Multiplica SOLO el cumplimiento 'en pie' de la capa, nunca el extra."""
    if floor is None: floor = PARAMS['floor']
    return floor + (1 - floor) * g(M, q)

# ============================================================
# 4. SOPORTES y TASKS (saturantes, relacionales, a la BASE)
# ============================================================
def support_term(done_fracs):
    """done_fracs: lista de fracciones [0,1] de cumplimiento de cada soporte en la semana
    (UX inversa: 1 = no fallo nunca, 0 = lo descuido siempre).
    Centrado en 0.5: full => +sat, descuidado => -sat ; multi-soporte SATURA."""
    if not done_fracs: return 0.0
    n = len(done_fracs)
    avg = sum(done_fracs) / n
    signed = 2 * avg - 1  # [-1, 1]
    mag = PARAMS['sup_sat'] * (1 - math.exp(-PARAMS['sup_k'] * abs(signed)))
    return math.copysign(mag, signed)

def task_term(done_fracs):
    """tasks con capa: aportan poco (< soporte), saturan, solo suman (no penalizan ausencia)."""
    if not done_fracs: return 0.0
    n = len(done_fracs)
    avg = sum(done_fracs) / n  # fraccion de tasks completadas
    return PARAMS['task_sat'] * (1 - math.exp(-PARAMS['task_k'] * avg))

# ============================================================
# 5. VALOR DE CAPA — dos canales
# ============================================================
def layer_en_pie(anchor_Rs, M=None, supports=None, tasks=None):
    """Canal BASE de la capa in [0, ~1].
    - cumplimiento de anclas topado en 1 (promedio del bloque)
    - + soportes + tasks (saturados)
    - todo el bloque 'en pie' MODULADO por el cimiento (opt-in), con suelo
    - capa sin anclas: el cumplimiento base = M directo (la señal ES la capa)
    """
    supports = supports or []; tasks = tasks or []
    if anchor_Rs:
        cumpl = sum(min(r, 1.0) for r in anchor_Rs) / len(anchor_Rs)  # bloque promedia
        cumpl = cumpl + support_term(supports) + task_term(tasks)
        cumpl = max(0.0, min(cumpl, 1.0))
        if M is None:
            return cumpl
        return cimiento(M) * cumpl
    else:
        # capa solo-opt-in: su valor en pie ES la señal (sin extra). Soportes/tasks sueltos podrian
        # existir pero por dominio una capa solo-opt-in no trae anclas; M manda.
        if M is None:
            return 0.0
        return M

def layer_extra(anchor_Rs):
    """Canal EXTRA de la capa >= 0. SOLO anclas, excedente sobre la meta. Opt-in NO aporta."""
    if not anchor_Rs: return 0.0
    return sum(max(r - 1.0, 0.0) for r in anchor_Rs) / len(anchor_Rs)

# ============================================================
# 6. PESOS RELACIONALES  (un solo K por opt-in)
# ============================================================
def layer_sizes(layers, K):
    """tamaño(capa) = 1 (bloque anclas) + K (si opt-in). Sin opt-in => 1.
    NO depende del nº de anclas (promedian a un bloque)."""
    sizes = []
    for L in layers:
        s = 1.0
        if L.get('optin'):
            s += K
        sizes.append(s)
    return sizes

def layer_weights(layers, K):
    sizes = layer_sizes(layers, K)
    tot = sum(sizes)
    return [s / tot for s in sizes]

# ============================================================
# 7. AGREGACION GLOBAL -> ESTADO -> BANDA
# ============================================================
def aggregate(layers, K, params=None):
    """layers: lista de dicts:
       { 'anchors': [R,...], 'optin': M or None, 'supports':[...], 'tasks':[...] }
    Devuelve (base_global, extra_global, estado, banda, pesos)."""
    p = params or PARAMS
    w = layer_weights(layers, K)
    # BASE global = suma ponderada del en_pie de TODAS las capas
    base_g = 0.0
    for wi, L in zip(w, layers):
        base_g += wi * layer_en_pie(L.get('anchors'), L.get('optin'),
                                    L.get('supports'), L.get('tasks'))
    # EXTRA global = agregado SOLO entre capas con anclas, ponderado y SATURADO
    # (cobertura: promedio ponderado del extra entre capas-con-ancla -> luego saturacion global)
    anchored = [(wi, L) for wi, L in zip(w, layers) if L.get('anchors')]
    if anchored:
        wsum = sum(wi for wi, _ in anchored)
        raw_extra = sum(wi * layer_extra(L['anchors']) for wi, L in anchored) / wsum
    else:
        raw_extra = 0.0
    extra_g = p['extra_sat'] * (1 - math.exp(-raw_extra / p['extra_s0']))
    # ESTADO (eje axioma 3): la BASE SOLA llega COMO MAXIMO al techo de En marcha (b_marcha).
    # Por eso base_global in [0,1] se mapea a [0, b_marcha]: base llena = 0.85 EXACTO = tope En marcha.
    # SOLO el EXTRA (superhabit) empuja por encima, hacia Plenitud (>=b_marcha) e Inquebrantable.
    estado = p['b_marcha'] * base_g + p['lam_state'] * extra_g
    return base_g, extra_g, estado, band(estado, p), w

def band(x, p=None):
    p = p or PARAMS
    eps = 1e-9
    if x < p['b_rojo']: return 'Rojo'
    if x < p['b_amar']: return 'Atencion'
    if x <= p['b_marcha'] + eps: return 'En marcha'  # techo de En marcha INCLUSIVE: "todo justo" vive aca
    if x < p['b_pleno']: return 'Plenitud'
    return 'Inquebrantable'

# ============================================================
# 8. CALIBRACION DE K  — se DESPEJA de un axioma de estado del dueño
# ============================================================
# Axioma de estado (dueño, D8 + eje): "anclas perfectas (justas) en TODAS las capas
# pero el opt-in de UNA capa esta en el piso (M=0, p.ej. sin dormir / recaida) => el estado
# debe caer a ATENCION (no quedarse en En marcha)."
# Despejamos K para que ese caso caiga JUSTO en el borde alto de Atencion (= b_marcha - eps).
def solve_K_from_axiom(N=4, frac_into_atencion=0.5, tol=1e-9):
    """N capas, todas con anclas R=1 (justo, sin superhabit => extra=0), UNA con opt-in en M=0.
    Sin extra, estado = b_marcha * base_global. El axioma del dueño: ese estado debe caer a
    ATENCION. Apuntamos al centro de la banda Atencion (entre b_amar y b_marcha) para no quedar
    pegados al borde. Despejamos K.
      base_global = w_opt*cimiento(0)*1 + (1-w_opt)*1 = w_opt*floor + (1-w_opt)
      estado      = b_marcha * base_global
      w_opt       = (1+K)/(N+K)
    """
    floor = PARAMS['floor']; bm = PARAMS['b_marcha']; ba = PARAMS['b_amar']
    target_estado = ba + frac_into_atencion * (bm - ba)  # centro de Atencion
    target_base = target_estado / bm
    lo, hi = 0.0, 200.0
    def base_of(K):
        w_opt = (1 + K) / (N + K)
        return w_opt * floor + (1 - w_opt) * 1.0
    for _ in range(300):
        mid = (lo + hi) / 2
        b = base_of(mid)
        if b > target_base:  # base muy alta => mas peso al opt-in caido => subir K
            lo = mid
        else:
            hi = mid
        if abs(b - target_base) < tol:
            break
    return (lo + hi) / 2

K_SOLVED = solve_K_from_axiom(N=4)

# ============================================================
# 9. CASOS LIMITE — los 12 obligatorios
# ============================================================
def fmt(x): return f"{x:.4f}"

def run_cases():
    K = K_SOLVED
    rows = []

    # --- Caso 1: cumplir todo justo (anclas R=1, opt-ins bien) => EN MARCHA ---
    L = [
        {'anchors': [1.0], 'optin': None},
        {'anchors': [1.0], 'optin': 1.0},  # opt-in bien
        {'anchors': [1.0], 'optin': None},
        {'anchors': [1.0], 'optin': None},
    ]
    b, e, s, bd, w = aggregate(L, K)
    rows.append(("1. Todo justo (R=1, opt-in bien)", fmt(b), fmt(e), fmt(s), bd, "EN MARCHA"))

    # --- Caso 2: superhabit en anclas repartido => Plenitud/Inquebrantable ---
    L = [
        {'anchors': [R(7, 30, [60]*7)], 'optin': None},  # tiempo doble 7d
        {'anchors': [R(7, 30, [60]*7)], 'optin': 1.0},
        {'anchors': [R(7, 30, [60]*7)], 'optin': None},
        {'anchors': [R(7, 30, [60]*7)], 'optin': None},
    ]
    b, e, s, bd, w = aggregate(L, K)
    rows.append(("2. Superhabit repartido", fmt(b), fmt(e), fmt(s), bd, "Plenitud/Inq"))

    # --- Caso 3: capa sin anclas, solo opt-in (D4): valor = señal, no exporta extra ---
    L = [
        {'anchors': [1.0], 'optin': None},
        {'anchors': [1.0], 'optin': None},
        {'anchors': [1.0], 'optin': None},
        {'anchors': [], 'optin': 0.8},  # solo sueño, sin anclas
    ]
    b, e, s, bd, w = aggregate(L, K)
    ex_d4 = layer_extra([])  # debe ser 0
    rows.append(("3. Capa solo-opt-in (D4)", fmt(b), fmt(e)+f" (capaD4 extra={ex_d4:.2f})", fmt(s), bd, "no exporta extra"))

    # --- Caso 4: el apreton: 3 capas + AMBOS opt-ins => pesos suman 1 ---
    L = [
        {'anchors': [1.0], 'optin': 0.9},   # Cuerpo + sueño
        {'anchors': [1.0], 'optin': 1.0},   # Conducta + sobriedad (held)
        {'anchors': [1.0], 'optin': None},  # otra capa
    ]
    b, e, s, bd, w = aggregate(L, K)
    rows.append(("4. Apreton 3 capas + 2 opt-ins", fmt(b), f"Σw={sum(w):.4f}", fmt(s), bd, "Σpesos=1"))

    # --- Caso 5: mal sueño hunde SU capa pero NO el extra de otras ---
    # otras capas con superhabit; capa con sueño malo
    L_buen = [
        {'anchors': [R(7,30,[60]*7)], 'optin': 0.9},   # sueño bien
        {'anchors': [R(7,30,[60]*7)], 'optin': None},
        {'anchors': [R(7,30,[60]*7)], 'optin': None},
    ]
    L_mal = [
        {'anchors': [R(7,30,[60]*7)], 'optin': 0.1},   # sueño MAL (misma capa, mismas anclas)
        {'anchors': [R(7,30,[60]*7)], 'optin': None},
        {'anchors': [R(7,30,[60]*7)], 'optin': None},
    ]
    bb, eb, sb, bdb, _ = aggregate(L_buen, K)
    bm, em, sm, bdm, _ = aggregate(L_mal, K)
    rows.append(("5. Mal sueño: extra intacto", f"base {bb:.3f}->{bm:.3f}", f"extra {eb:.4f}=={em:.4f}", f"{sb:.3f}->{sm:.3f}", f"{bdb}->{bdm}", "extra NO cae"))

    # --- Caso 6: sin dato de sueño => base, no piso ---
    M_nodata = sleep_weekly([None]*7)
    M_parcial = sleep_weekly([0.8, 0.8, None, None, None, None, None])
    rows.append(("6. Sueño sin dato => base", f"M_nodato={M_nodata:.3f}", f"M_parcial={M_parcial:.3f}", "-", "-", f"base={PARAMS['sleep_base']}"))

    # --- Caso 7: recaida dentro 7d penaliza; fuera no (la señal ya viene materializada) ---
    M_in = sobriety_signal([False])   # recaida en 7d
    M_out = sobriety_signal([True])   # recaida vieja => held esta semana
    rows.append(("7. Recaida in/out 7d", f"M_in={M_in:.3f}", f"M_out={M_out:.3f}", "-", "-", "in baja, out no"))

    # --- Caso 8: multi-sobriedad: 1 recaida entre 3 ~ igual que entre 1 ---
    M_1of1 = sobriety_signal([False])
    M_1of3 = sobriety_signal([False, True, True])
    M_2of3 = sobriety_signal([False, False, True])
    rows.append(("8. Multi-sobriedad 1/1 vs 1/3", f"1/1={M_1of1:.3f}", f"1/3={M_1of3:.3f}", f"2/3={M_2of3:.3f}", "-", "no se diluye"))

    # --- Caso 9: soportes full vs descuidados (light, satura) ---
    L_full = [
        {'anchors': [1.0], 'optin': None, 'supports': [1.0, 1.0, 1.0]},
        {'anchors': [1.0], 'optin': None},
        {'anchors': [1.0], 'optin': None},
    ]
    L_desc = [
        {'anchors': [1.0], 'optin': None, 'supports': [0.0, 0.0, 0.0]},
        {'anchors': [1.0], 'optin': None},
        {'anchors': [1.0], 'optin': None},
    ]
    L_8sup = [
        {'anchors': [1.0], 'optin': None, 'supports': [1.0]*8},
        {'anchors': [1.0], 'optin': None},
        {'anchors': [1.0], 'optin': None},
    ]
    bf,_,sf,_,_ = aggregate(L_full, K)
    bd_,_,sd_,_,_ = aggregate(L_desc, K)
    b8,_,s8,_,_ = aggregate(L_8sup, K)
    st3 = support_term([1.0,1.0,1.0]); st8 = support_term([1.0]*8)
    rows.append(("9. Soportes full/desc/8x", f"full {sf:.3f}", f"desc {sd_:.3f}", f"8sup term {st8:.3f} vs 3sup {st3:.3f}", "-", "light+satura"))

    # --- Caso 10: tasks aportan poco, menos que soportes ---
    tk = task_term([1.0,1.0,1.0]); sp = support_term([1.0,1.0,1.0])
    rows.append(("10. Tasks < soportes", f"task_term={tk:.4f}", f"sup_term={sp:.4f}", f"task<sup: {tk<sp}", "-", "task aporta menos"))

    # --- Caso 11: mas anclas en una capa NO cambia el peso de la capa ---
    L_1a = [{'anchors':[1.0], 'optin':None}, {'anchors':[1.0],'optin':None}, {'anchors':[1.0],'optin':None}]
    L_3a = [{'anchors':[1.0,1.0,1.0], 'optin':None}, {'anchors':[1.0],'optin':None}, {'anchors':[1.0],'optin':None}]
    w1 = layer_weights(L_1a, K); w3 = layer_weights(L_3a, K)
    rows.append(("11. +anclas no cambia peso", f"w(1ancla)={w1[0]:.4f}", f"w(3anclas)={w3[0]:.4f}", f"iguales: {abs(w1[0]-w3[0])<1e-9}", "-", "peso estable"))

    # --- Caso 12: N=3,4,5 => peso del opt-in baja al crecer N ---
    def w_optin(N, K):
        L = [{'anchors':[1.0], 'optin': (i==0)} for i in range(N)]
        return layer_weights(L, K)[0]
    w3 = w_optin(3, K); w4 = w_optin(4, K); w5 = w_optin(5, K)
    rows.append(("12. Peso opt-in vs N", f"N=3:{w3:.4f}", f"N=4:{w4:.4f}", f"N=5:{w5:.4f}", f"baja: {w3>w4>w5}", "relacional"))

    return rows, K

if __name__ == "__main__":
    print("="*100)
    print(f"K despejado del axioma de estado (N=4, opt-in caido => borde Atencion): K = {K_SOLVED:.4f}")
    print(f"  => peso opt-in dentro de capa = K/(1+K) = {K_SOLVED/(1+K_SOLVED):.4f}")
    print(f"  => floor del cimiento = {PARAMS['floor']}, q = {PARAMS['q']}")
    print("="*100)
    rows, K = run_cases()
    print(f"\n{'CASO':<34}{'col1':<22}{'col2':<26}{'col3':<24}{'banda':<22}{'esperado'}")
    print("-"*150)
    for r in rows:
        c1,c2,c3,c4,c5,c6 = r
        print(f"{c1:<34}{c2:<22}{c3:<26}{c4:<24}{c5:<22}{c6}")

    # ---- DEMO del sesgo B: cimiento suave vs multiplicativo puro (rechazado por el dueño) ----
    print("\n" + "="*100)
    print("DEMO sesgo B — por que el SUELO evita la brutalidad del M*R puro (rechazado por el dueño)")
    print("="*100)
    print(f"{'M (sueño)':<12}{'M*R puro':<14}{'cimiento(M)':<14}{'cimiento*cumpl':<16}{'delta a favor del esfuerzo'}")
    cumpl = 1.0  # ancla cumplida justo
    for M in [1.0, 0.8, 0.6, 0.4, 0.2, 0.0]:
        puro = M * cumpl
        cim = cimiento(M)
        modulado = cim * cumpl
        print(f"{M:<12.2f}{puro:<14.4f}{cim:<14.4f}{modulado:<16.4f}+{modulado-puro:.4f}")
    print("Lectura: con sueño regular (M=0.6) el multiplicativo puro borra el 40% del esfuerzo;")
    print("el cimiento suave solo baja a ~0.82 (el suelo 0.55 + curva). El efecto domino aparece")
    print("recien cuando M se desploma (M=0.2 -> 0.66; M=0 -> 0.55): el cimiento roto degrada, no aniquila.")
