#!/usr/bin/env python3
"""Deterministic verification + multi-layer simulation for the consolidated anchor formula.

Session: docs/scoring/exploracion-rendimiento-ancla/ (2026-06-10).
Part A — deterministic tests of the anchor value formula (axioms A1-A10 + edge cases).
Part B — multi-layer simulation: N = 3..8 active layers, anchors only (no sleep/sobriety/
         supports/tasks). States — including Inquebrantable — are decided ONLY on the
         GLOBAL score (owner's correction 2026-06-10): score = mean(layer values),
         layer value = mean(anchor R). Inquebrantable <=> score >= 1 + DELTA.

Illustrative parameters (NOT calibrated): gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0,
smax=0.5, s0=0.5, delta=0.10 (sensitivity shown at 0.15).
"""
import math

# ---------------------------------------------------------------- formula ---
GAMMA, LAM_V, KAPPA, P, SMAX, S0 = 1.5, 0.5, 1.5, 2.0, 0.5, 0.5
DELTA = 0.10

def anchor_R(F, T, mins, gamma=GAMMA, lam_v=LAM_V, kappa=KAPPA, p=P, smax=SMAX, s0=S0):
    marked = sorted([m for m in mins if m > 0], reverse=True)
    D = len(marked)
    if D == 0:
        return 0.0
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

# ---------------------------------------------------------------- helpers ---
PASS, FAIL = 0, []

def check(name, cond, detail=""):
    global PASS
    if cond:
        PASS += 1
    else:
        FAIL.append(f"{name}  {detail}")

def band(score):
    if score >= 1 + DELTA: return "INQUEBRANTABLE"
    if score >= 0.85: return "Pleno"
    if score >= 0.62: return "En marcha"
    if score >= 0.40: return "Amarillo"
    return "Rojo"

# ============================================================ PART A: tests ==
print("=" * 76)
print("PARTE A — VERIFICACION DETERMINISTA DE LA FORMULA DEL ANCLA")
print("=" * 76)

# --- A1: range [0, 1+smax] over a stress grid -------------------------------
for F in range(1, 8):
    for T in (5, 30, 120):
        for week in ([], [T] * F, [T * 10] * 7, [0.5] * 7, [T * 0.1] * F,
                     [T * 3] * min(F + 2, 7), [T] * 7):
            v = anchor_R(F, T, week)
            check("A1-rango", -1e-12 <= v <= 1 + SMAX + 1e-12, f"F={F} T={T} w={week} R={v}")

# --- A2: D=0 -> 0 ------------------------------------------------------------
for F in range(1, 8):
    check("A2", anchor_R(F, 30, []) == 0.0, f"F={F}")
    check("A2", anchor_R(F, 30, [0, 0, 0]) == 0.0, f"F={F} zeros")

# --- A3: exact compliance == 1 exactly, for every F and several T -----------
for F in range(1, 8):
    for T in (1, 7, 30, 45, 120, 900):
        v = anchor_R(F, T, [T] * F)
        check("A3-exacto=1", abs(v - 1.0) < 1e-12, f"F={F} T={T} R={v!r}")

# --- A4: adding a marked day never decreases R -------------------------------
BASE_WEEKS = [[], [30], [90], [30, 30], [30, 30, 30], [10, 10, 10], [45, 45],
              [5, 5, 5, 5, 5], [30, 10, 50], [60, 60], [30] * 6, [1] * 6]
EXTRAS = [0.5, 1, 5, 15, 30, 45, 90]
for F in range(1, 8):
    for w in BASE_WEEKS:
        if len(w) >= 7:
            continue
        r0 = anchor_R(F, 30, w)
        for t in EXTRAS:
            r1 = anchor_R(F, 30, w + [t])
            check("A4-mas-dias", r1 >= r0 - 1e-9, f"F={F} w={w} +{t}: {r0}->{r1}")

# --- A5 (fuerte): more minutes on ANY single day never decreases R ----------
for F in (1, 2, 3, 5, 7):
    for others in ([], [30, 30], [10, 10], [45, 45, 45], [30] * 5, [5] * 4):
        if len(others) >= 7:
            continue
        prev = None
        t = 0.5
        while t <= 150:
            v = anchor_R(F, 30, others + [t])
            if prev is not None:
                check("A5-mas-tiempo", v >= prev - 1e-9,
                      f"F={F} others={others} t={t}: {prev}->{v}")
            prev = v
            t += 0.5

# --- A7: voluntary floor zero, contribution -> 0 -----------------------------
for F in (1, 2, 3):
    full = [30] * F
    r_full = anchor_R(F, 30, full)
    last_d = None
    for eps in (10, 1, 0.1, 0.01, 0.001):
        d = anchor_R(F, 30, full + [eps]) - r_full
        check("A7-piso", d >= -1e-12, f"F={F} eps={eps} d={d}")
        if last_d is not None:
            check("A7-tiende-a-0", d <= last_d + 1e-12, f"F={F} eps={eps}")
        last_d = d
    check("A7-limite", last_d < 1e-3, f"F={F} d(0.001)={last_d}")

# --- A9: continuity — fine sweeps, jump bounded ------------------------------
worst = 0.0
for F in (2, 3, 5, 7):
    for others in ([35, 25, 20], [30, 30], [90, 5], [30] * 6):
        if len(others) >= 7:
            continue
        prev = None
        t = 0.5
        while t <= 120:
            v = anchor_R(F, 30, others + [t])
            if prev is not None:
                worst = max(worst, abs(v - prev))
            prev = v
            t += 0.5
check("A9-continuidad", worst < 0.02, f"salto max barrido fino = {worst:.5f}")
print(f"  A9: salto maximo en barridos finos (paso 0.5 min) = {worst:.5f}")

# --- A10: scale invariance ----------------------------------------------------
for k in (0.5, 2, 4, 10):
    for (F, T, w) in [(3, 30, [40, 30, 30]), (5, 20, [60, 60]), (7, 30, [45] * 7),
                      (2, 30, [30, 30, 1, 1, 1]), (4, 40, [40, 40, 40, 40, 5, 5, 5])]:
        a = anchor_R(F, T, w)
        b = anchor_R(F, T * k, [x * k for x in w])
        check("A10-invarianza", abs(a - b) < 1e-9, f"k={k} F={F}: {a} vs {b}")

# --- §7 edge cases (expected behaviours) -------------------------------------
print("\n  Casos limite §7 (valores con parametros ilustrativos):")
edge = []
def case(name, F, T, w, cond_fn, expects):
    v = anchor_R(F, T, w)
    ok = cond_fn(v)
    check(f"caso:{name}", ok, f"R={v:.4f} esperado {expects}")
    edge.append((name, v, expects, ok))

case("exacto", 3, 30, [30, 30, 30], lambda v: abs(v - 1) < 1e-12, "=1")
case("nada", 3, 30, [], lambda v: v == 0, "=0")
case("sup.dias+def.tiempo", 3, 30, [10, 10, 10, 90, 90],
     lambda v: 0.85 < v < 1 + DELTA, "base<1, sin Inq global por si sola")
case("def.frec+tiempo-alto", 5, 20, [60, 60], lambda v: v < 0.62, "deficit domina")
case("40min-conc", 2, 30, [30, 30, 40], lambda v: v > anchor_R(2, 30, [30, 30, 1, 1]),
     "concentrado > repartido trivial")
case("vol-trivial", 2, 30, [30, 30, 1, 1, 1], lambda v: 1 <= v < 1.01, "~1, migajas")
case("def.puro-tiempo", 2, 5, [1] * 7, lambda v: v < 0.40, "bajo")
case("F7-exacto", 7, 30, [30] * 7, lambda v: abs(v - 1) < 1e-12, "=1")
case("F7-superavit", 7, 30, [45] * 7, lambda v: v >= 1 + DELTA, "exporta superavit pleno")
case("F7-no-explota", 7, 30, [120] * 7, lambda v: v < 1 + SMAX, "< 1+smax")
case("testigo-dueno", 4, 40, [40, 40, 40, 40, 5, 5, 5], lambda v: 1 <= v < 1 + DELTA,
     "Pleno-ish, no exporta superavit pleno")
case("rafaga-absurda", 5, 20, [400], lambda v: v < 0.40, "1 dia no compensa 4")
for name, v, expects, ok in edge:
    print(f"    {'OK ' if ok else 'XX '}{name:24s} R={v:.4f}   ({expects})")

# --- P2: surplus weight grows with F ------------------------------------------
seq = [anchor_R(F, 30, [60] * F) for F in range(2, 8)]
check("P2-monotono-en-F", all(seq[i] < seq[i + 1] for i in range(len(seq) - 1)),
      f"{[round(x, 4) for x in seq]}")
print(f"\n  P2 (r=2, base completa) F=2..7: {[round(x, 4) for x in seq]}  (creciente)")

# ==================================================== PART A2: edge battery ==
print("\n  --- A2: bateria ampliada de casos limite del ancla (deterministas) ---")
import itertools as _it

# invariancia por permutacion del array (el orden de los dias no importa)
for w in ([30, 10, 50], [5, 90, 30, 1], [60, 60, 20]):
    vals = {round(anchor_R(3, 30, list(pm)), 12) for pm in _it.permutations(w)}
    check("A2-permutacion", len(vals) == 1, f"w={w}")

# dias en 0 son no-ops
for w in ([30, 30], [10], [45, 45, 45]):
    check("A2-ceros-noop", abs(anchor_R(3, 30, w) - anchor_R(3, 30, w + [0, 0])) < 1e-12, f"{w}")

# empate en la frontera Best-F (compromiso/voluntario intercambiables sin salto)
a = anchor_R(2, 30, [30, 30, 30]); b = anchor_R(2, 30, [30, 30, 29.9])
check("A2-empate-frontera", abs(a - b) < 0.01, f"{a:.4f} vs {b:.4f}")

# techo absoluto bajo inputs insensatos
for w in ([9000] * 7, [10000], [900] * 7):
    check("A2-techo", anchor_R(7, 30, w) <= 1 + SMAX + 1e-9, f"{w[:2]}...")

# F=1 (minimo): exacto=1; con 6 voluntarios plenos exporta fuerte por dias
check("A2-F1-exacto", abs(anchor_R(1, 30, [30]) - 1) < 1e-12)
v = anchor_R(1, 30, [30] * 7)
check("A2-F1+6vol", v >= 1.3, f"{v:.4f}")
print(f"    F=1 exacto = 1; F=1 + 6 voluntarios plenos = {v:.4f} (superavit de dias)")

# F=7 con deficit de dias: cada faltante pesa, sin tapadera posible
v6, v3 = anchor_R(7, 30, [30] * 6), anchor_R(7, 30, [30] * 3)
check("A2-F7-falta-1-dia", 0.85 <= v6 < 1, f"{v6:.4f}")
check("A2-F7-faltan-4", v3 < 0.62, f"{v3:.4f}")
print(f"    F=7 con 6/7 dias = {v6:.4f}; con 3/7 = {v3:.4f}")

# constancia con total de minutos FIJO (90 min, F=3, T=30): mas dias > menos dias
c1, c2, c3 = anchor_R(3, 30, [90]), anchor_R(3, 30, [45, 45]), anchor_R(3, 30, [30, 30, 30])
check("A2-constancia-total-fijo", c1 < c2 < c3, f"{c1:.3f},{c2:.3f},{c3:.3f}")
print(f"    mismo total 90min: [90]={c1:.3f} < [45,45]={c2:.3f} < [30,30,30]={c3:.3f}")

# frecuencia llena, mismo total: parejo >= desparejo (el superavit de un dia NO paga
# el deficit de otro — bidireccionalidad asimetrica deseada)
e1, e2 = anchor_R(3, 30, [30, 30, 30]), anchor_R(3, 30, [20, 30, 40])
check("A2-parejo>=desparejo", e1 >= e2 - 1e-9, f"{e1:.3f} vs {e2:.3f}")
print(f"    total 90 con frecuencia llena: parejo={e1:.3f} > desparejo[20,30,40]={e2:.3f}")

# el voluntario repara deficit de tiempo del compromiso
d0, d1 = anchor_R(3, 30, [30, 30, 10]), anchor_R(3, 30, [30, 30, 10, 10])
check("A2-voluntario-repara", d1 > d0, f"{d0:.4f} -> {d1:.4f}")

# frontera r=1 (cruzar el target no salta)
f0, f1 = anchor_R(3, 30, [30, 30, 29.5]), anchor_R(3, 30, [30, 30, 30.5])
check("A2-frontera-target", 0 <= f1 - f0 < 0.05, f"{f0:.4f} -> {f1:.4f}")

# T extremos del dominio (1 min y 900 min = 15h max)
check("A2-T=1", abs(anchor_R(3, 1, [1, 1, 1]) - 1) < 1e-12)
check("A2-T=900", abs(anchor_R(3, 900, [900] * 3) - 1) < 1e-12)

# HALLAZGO (observacion, sin pass/fail): gamma acopla anti-trivialidad con el
# reparto intra-compromiso. 2/4 dias plenos vs 4/4 dias a mitad de tiempo:
h_full, h_half = anchor_R(4, 30, [30, 30]), anchor_R(4, 30, [15, 15, 15, 15])
print(f"    HALLAZGO gamma: 2/4 dias plenos={h_full:.3f} vs 4/4 dias a mitad={h_half:.3f}"
      f"\n      (con gamma=1 serian iguales; gamma>1 favorece profundidad sobre presencia"
      f"\n       parcial — decision de calibracion para el dueno)")

# r*: superavit de tiempo uniforme minimo (D=F) para exportar >= 1+delta, por F
def rstar(F):
    lo, hi = 1.0, 50.0
    if anchor_R(F, 30, [30 * hi] * F) < 1 + DELTA:
        return None
    for _ in range(80):
        mid = (lo + hi) / 2
        if anchor_R(F, 30, [30 * mid] * F) >= 1 + DELTA:
            hi = mid
        else:
            lo = mid
    return hi
rs = [(F, rstar(F)) for F in range(1, 8)]
print("    r* (factor de tiempo uniforme para exportar 1+delta, solo via tiempo):")
print("      " + "  ".join(f"F={F}:{('%.3f' % r) if r else 'imposible'}" for F, r in rs))
if all(r is not None for _, r in rs):
    check("A2-rstar-decrece-con-F",
          all(rs[i][1] > rs[i + 1][1] for i in range(len(rs) - 1)), f"{rs}")

# dias voluntarios PLENOS minimos para exportar >= 1+delta (base exacta), por F
def vstar(F):
    for extra in range(0, 8 - F):
        if anchor_R(F, 30, [30] * (F + extra)) >= 1 + DELTA:
            return extra
    return None
vs = [(F, vstar(F)) for F in range(1, 7)]
print("    dias voluntarios plenos minimos para exportar 1+delta: "
      + "  ".join(f"F={F}:{v if v is not None else '--'}" for F, v in vs))

total = PASS + len(FAIL)
print(f"\n  RESULTADO PARTE A: {PASS}/{total} checks OK, {len(FAIL)} fallas")
for f in FAIL[:20]:
    print("   FALLA:", f)

# ====================================================== PART B: multi-layer ==
# Correccion metodologica del dueno (2026-06-10):
#  - N = 3..5 capas activas. Minimo 3 = UNICO axioma duro del motor. Maximo canonico 5.
#  - 1 ancla por capa.
#  - SIN reglas de cobertura: score = promedio ponderado (pesos 1/N) -> estado por
#    bandas. Lo que el promedio da, ESE es el estado. Inquebrantable incluido.
#  - Las 45 marcas historicas fueron herramienta de descubrimiento, no ground truth.
print("\n" + "=" * 76)
print("PARTE B — SIMULACION DEL MOTOR: N = 3..5 capas activas, 1 ANCLA por capa")
print("Axioma duro del motor: minimo 3 capas activas. Maximo canonico: 5.")
print("SIN reglas de cobertura: score = promedio (pesos 1/N) -> estado por bandas.")
print(f"Inquebrantable <=> score >= 1+delta (delta={DELTA}: corte de banda calibrable,")
print("igual que 0.40/0.62/0.85 — NO una herramienta para exigir capas en superavit)")
print("=" * 76)

PROFILES = {
    "EXACTO":   anchor_R(3, 30, [30, 30, 30]),
    "SUP_MAX":  anchor_R(7, 30, [60] * 7),
    "SUP_MED":  anchor_R(4, 30, [45, 45, 45, 45]),
    "SUP_DIAS": anchor_R(2, 30, [30, 30, 30, 30]),
    "LEVE_DEF": anchor_R(3, 30, [30, 30, 20]),
    "DEBIL":    anchor_R(5, 20, [60, 60]),
    "CERO":     anchor_R(3, 30, []),
    "TRIVIAL":  anchor_R(2, 30, [30, 30, 1, 1, 1]),
}
print("\nPerfiles de ancla (1 ancla = 1 capa; parametros ilustrativos):")
for k, v in PROFILES.items():
    print(f"  {k:9s} = {v:.4f}")

def world(layers):
    score = sum(layers) / len(layers)
    return score, band(score)

E, SX, SM, SD, LD, DB, C0, TRV = (PROFILES[k] for k in
    ("EXACTO", "SUP_MAX", "SUP_MED", "SUP_DIAS", "LEVE_DEF", "DEBIL", "CERO", "TRIVIAL"))

# --- B1: enumeracion EXHAUSTIVA de mundos ------------------------------------
from itertools import combinations_with_replacement as cwr
NAMES = list(PROFILES)
print("\n--- B1: enumeracion exhaustiva — toda combinacion de perfiles por capa ---")
for N in (3, 4, 5):
    worlds = []
    for combo in cwr(NAMES, N):
        s, bnd = world([PROFILES[c] for c in combo])
        worlds.append((s, bnd, combo))
    total_c = len(worlds)
    print(f"\n  N={N}: {total_c} mundos posibles con {len(NAMES)} perfiles")
    for b in ("Rojo", "Amarillo", "En marcha", "Pleno", "INQUEBRANTABLE"):
        n = sum(1 for _, bb, _ in worlds if bb == b)
        print(f"    {b:15s} {n:4d}  ({100 * n / total_c:5.1f}%)")
    inq = sorted((s, c) for s, bb, c in worlds if bb == "INQUEBRANTABLE")
    if inq:
        # descripcion EMERGENTE (no regla): que tienen en comun los mundos Inq
        min_sup = min(sum(1 for p in c if PROFILES[p] > 1 + 1e-9) for _, c in inq)
        s_min, c_min = inq[0]
        print(f"    emergente: a N={N}, todo mundo Inquebrantable resulto tener "
              f">= {min_sup} capa(s) con valor > 1")
        print(f"    el Inquebrantable mas justo: {'+'.join(c_min)} = {s_min:.4f}")
    miss = max((w for w in worlds if w[1] != "INQUEBRANTABLE"), key=lambda w: w[0])
    print(f"    el que mas cerca quedo: {'+'.join(miss[2])} = {miss[0]:.4f} ({miss[1]})")
    check("B1-exacto-uniforme-pleno", world([E] * N)[1] == "Pleno", f"N={N}")
    check("B1-migajas-no-inq", world([TRV] * N)[0] < 1 + DELTA, f"N={N}")
    check("B1-cero-rojo", world([C0] * N)[1] == "Rojo", f"N={N}")

# --- B2: k capas en superavit maximo, resto exacto (DESCRIPTIVO) -------------
print("\n--- B2: k capas en SUP_MAX, resto EXACTO — lo que el promedio da (descriptivo) ---")
print(f"{'N':>2} | " + " | ".join(f"   k={k}  " for k in range(0, 6)))
for N in (3, 4, 5):
    row = []
    for k in range(0, 6):
        if k > N:
            row.append("   --   ")
        else:
            s, bnd = world([SX] * k + [E] * (N - k))
            tag = "I" if bnd == "INQUEBRANTABLE" else bnd[0]
            row.append(f"{s:.3f} {tag} ")
    print(f"{N:>2} | " + " | ".join(row))
print("  (R/A/E/P/I = estado emergente. No hay regla de cobertura: es solo el promedio.)")

# --- B3: colapso progresivo ----------------------------------------------------
print("\n--- B3: colapso progresivo — j capas en CERO, resto EXACTO ---")
for N in (3, 4, 5):
    prev, cells = None, []
    for j in range(0, N + 1):
        s, bnd = world([C0] * j + [E] * (N - j))
        cells.append(f"j={j}:{s:.3f}({bnd[0]})")
        if prev is not None:
            check("B3-colapso-monotono", s < prev, f"N={N} j={j}")
        prev = s
    print(f"  N={N}:  " + "  ".join(cells))

# --- B4: una capa floja, resto exacto (dilucion segun N) -----------------------
print("\n--- B4: una capa floja, resto exacto ---")
print(f"{'N':>2} | 1 DEBIL + resto EXACTO     | 1 CERO + resto EXACTO      | 1 LEVE_DEF + resto EXACTO")
for N in (3, 4, 5):
    a1, b1 = world([DB] + [E] * (N - 1))
    a2, b2 = world([C0] + [E] * (N - 1))
    a3, b3 = world([LD] + [E] * (N - 1))
    print(f"{N:>2} | {a1:.4f} {b1:18s} | {a2:.4f} {b2:19s} | {a3:.4f} {b3}")

# --- B5: bordes de banda (semantica >=) ----------------------------------------
for v, expected in [(0.3999, "Rojo"), (0.40, "Amarillo"), (0.6199, "Amarillo"),
                    (0.62, "En marcha"), (0.8499, "En marcha"), (0.85, "Pleno"),
                    (1 + DELTA - 1e-6, "Pleno"), (1 + DELTA, "INQUEBRANTABLE")]:
    check("B5-borde-banda", band(v) == expected, f"{v} -> {band(v)}, esperado {expected}")
print("\n  B5: bordes de banda verificados (semantica >= en 0.40 / 0.62 / 0.85 / 1+delta).")

# --- B6: vinetas (1 ancla por capa, N=3..5) ------------------------------------
print("\n--- B6: vinetas realistas ---")
NEAR = anchor_R(7, 30, [27] * 7)  # todo apenas bajo meta (r=0.9)
for name, layers in [
    ("semana solida con un bache (N=5)", [SM, E, LD, E, DB]),
    ("recuperacion: DEBIL mejoro a LEVE_DEF (N=3)", [LD, E, E]),
    ("migajas + 1 capa heroica (N=3)", [SX, TRV, TRV]),
    ("todo apenas-bajo-meta r=0.9 (N=4)", [NEAR] * 4),
    ("dos fuertes y una muerta (N=3)", [SX, SX, C0]),
    ("superavit repartido moderado (N=5)", [SM] * 5),
    ("superavit por dias repartido (N=4)", [SD] * 4),
    ("escalera de bandas (N=5)", [E, LD, 0.62, DB, C0]),
]:
    s, bnd = world(layers)
    print(f"  {name:48s} score={s:.4f}  {bnd}")

# --- B7: propiedad registrada del motor (pesos puros, cerrado) ------------------
sA, _ = world([1.2, 0.8, 1.0]); sB, _ = world([1.0, 1.0, 1.0])
check("B7-dispersion-invisible", abs(sA - sB) < 1e-12)
print("\n  B7: [1.2, 0.8, 1.0] == [1.0, 1.0, 1.0] en score — la dispersion entre capas es"
      "\n      invisible al promedio (consecuencia del motor de pesos puros, cerrado).")

total = PASS + len(FAIL)
print(f"\nRESULTADO GLOBAL: {PASS}/{total} checks OK, {len(FAIL)} fallas")
for f in FAIL[:20]:
    print("  FALLA:", f)
print("\nFIN. Axioma duro del motor: minimo 3 capas activas (canonico 3..5). 1 ancla por capa.")
print("Sin reglas de cobertura: los estados (Inquebrantable incluido) EMERGEN del promedio.")
print("Las 45 marcas historicas = herramienta de descubrimiento, no ground truth (dueno, 2026-06-10).")

