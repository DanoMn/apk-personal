#!/usr/bin/env python3
"""
weight_model_fit_v2.py — Modelo UNIFICADO de pesos dinámicos (sin gates/caps-parche).

v2.1 — correcciones post-auditoría (dos auditores opus independientes):
  - sop_form = asym Y lin (la "Tensión 1" del plan era falsa: ambas formas ajustan).
  - selección = MÁXIMO margen mínimo a los cortes (el 45/45 más ROBUSTO), no el primero.
  - reporte HONESTO: márgenes al corte, identificabilidad por parámetro, casos degenerados.
  - FASE 2: re-codifica los 43 casos viejos (sueño-on, 3 capas) y busca si EXISTE un punto
    que ajuste los 88 juntos (test real de unificación, no "aplicar el ganador de Fase 1").

Plan: meta/instructions/2026-06-08-refit-script-scoring-v2.md
Correcciones: meta/instructions/2026-06-08-correcciones-modelo-v2.md
Dataset (marcas): docs/scoring/dataset-decisiones-estado-v1.md

Estructura del modelo:
  - valor de capa: Cuerpo+sueño, Conducta+sobriedad(por estado), soporte asimétrico/lineal.
  - pesos: IGUALES por capa activa; sueño infla Cuerpo (k_sleep); sobriedad infla Conducta (k_sobr).
  - score = Σ w·val   (ω=0, SIN término de peor capa — lo exige la base pura BP).
  - cortes R/A/EM/P; gate Inquebrantable = P + anclas 100% + superhabit en ≥2 capas.
"""
import itertools
from collections import Counter

SUP2 = 2 / 7  # soporte 2/7 días sostenidos (descuidado)

# caso: (id, layers{capa:ancla}, sleep, sobriety, support{capa:frac}|None, superhabit_cap, all100, expect)
CASES = [
    # --- BP: base pura, 5 capas, sueño OFF, sobriedad OFF (capas parejas) ---
    ("BP-AP1", {"I": .25, "Cu": .25, "Co": .25, "V": .25, "P": .25}, "off", "off", None, 0, False, "R"),
    ("BP-AP2", {"I": .50, "Cu": .50, "Co": .50, "V": .50, "P": .50}, "off", "off", None, 0, False, "A"),
    ("BP-AP3", {"I": .75, "Cu": .75, "Co": .75, "V": .75, "P": .75}, "off", "off", None, 0, False, "EM"),
    ("BP-AP4", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "off", "off", None, 0, False, "P"),
    ("BP-AP5", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": .50}, "off", "off", None, 0, False, "P"),
    ("BP-AC1", {"I": 1., "Cu": 0., "Co": 1., "V": 1., "P": 1.}, "off", "off", None, 0, False, "EM"),
    ("BP-AC2", {"I": 0., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "off", "off", None, 0, False, "EM"),
    ("BP-AC3", {"I": 1., "Cu": 1., "Co": 1., "V": 0., "P": 1.}, "off", "off", None, 0, False, "EM"),
    ("BP-AC4", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 0.}, "off", "off", None, 0, False, "EM"),
    ("BP-AN3", {"I": .75, "Cu": .75, "Co": .75}, "off", "off", None, 0, False, "EM"),
    ("BP-AN4", {"I": .75, "Cu": .75, "Co": .75, "V": .75}, "off", "off", None, 0, False, "EM"),

    # --- SU: sueño modulador, 3 capas, sueño OPT-IN ---
    ("SU1", {"I": 1., "Cu": 1., "Co": 1.}, "ok", "off", None, 0, False, "P"),
    ("SU2", {"I": 1., "Cu": 1., "Co": 1.}, "mal", "off", None, 0, False, "EM"),
    ("SU3", {"I": 1., "Cu": 1., "Co": 1.}, "none", "off", None, 0, False, "EM"),
    ("SU4", {"I": .75, "Cu": .75, "Co": .75}, "ok", "off", None, 0, False, "EM"),
    ("SU5", {"I": .75, "Cu": .75, "Co": .75}, "mal", "off", None, 0, False, "EM"),
    ("SU6", {"I": .5, "Cu": .5, "Co": .5}, "ok", "off", None, 0, False, "A"),
    ("SU7", {"I": .5, "Cu": .5, "Co": .5}, "mal", "off", None, 0, False, "A"),
    ("SU8", {"I": 1., "Cu": .5, "Co": 1.}, "ok", "off", None, 0, False, "P"),
    ("SU9", {"I": 1., "Cu": 0., "Co": 1.}, "ok", "off", None, 0, False, "EM"),

    # --- SBR: sobriedad modulador, 3 capas, sueño OFF, sobriedad ON (Conducta) ---
    ("SB1", {"I": 1., "Cu": 1., "Co": 1.}, "off", "clean", None, 0, False, "P"),
    ("SB2", {"I": 1., "Cu": 1., "Co": 1.}, "off", "clean", None, 0, False, "P"),
    ("SB3", {"I": 1., "Cu": 1., "Co": 1.}, "off", "clean", None, 0, False, "P"),
    ("SB4", {"I": 1., "Cu": 1., "Co": 1.}, "off", "relapse", None, 0, False, "EM"),
    ("SB5", {"I": 1., "Cu": 1., "Co": 1.}, "off", "relapse", None, 0, False, "EM"),
    ("SB6", {"I": 1., "Cu": 1., "Co": 1.}, "off", "relapse", None, 0, False, "EM"),
    ("SB7", {"I": .5, "Cu": .5, "Co": .5}, "off", "clean", None, 0, False, "A"),
    ("SB8", {"I": .5, "Cu": .5, "Co": .5}, "off", "relapse", None, 0, False, "A"),
    ("SB9", {"I": 1., "Cu": 1., "Co": .25}, "off", "clean", None, 0, False, "A"),
    ("SB10", {"I": 1., "Cu": 1., "Co": 1.}, "off", "unmarked", None, 0, False, "EM"),

    # --- SO: soportes + tasks, 3 capas, sueño ok, soporte presente ---
    ("SO1", {"I": 1., "Cu": 1., "Co": 1.}, "ok", "off", {"I": 1., "Cu": 1., "Co": 1.}, 0, False, "P"),
    ("SO2", {"I": 1., "Cu": 1., "Co": 1.}, "ok", "off", {"I": SUP2, "Cu": SUP2, "Co": SUP2}, 0, False, "EM"),
    ("SO3", {"I": .75, "Cu": .75, "Co": .75}, "ok", "off", {"I": 1., "Cu": 1., "Co": 1.}, 0, False, "EM"),
    ("SO4", {"I": .75, "Cu": .75, "Co": .75}, "ok", "off", {"I": SUP2, "Cu": SUP2, "Co": SUP2}, 0, False, "EM"),
    ("SO5", {"I": .5, "Cu": .5, "Co": .5}, "ok", "off", {"I": 1., "Cu": 1., "Co": 1.}, 0, False, "EM"),
    ("SO6", {"I": .5, "Cu": .5, "Co": .5}, "ok", "off", {"I": SUP2, "Cu": SUP2, "Co": SUP2}, 0, False, "A"),
    ("SO7", {"I": .75, "Cu": .75, "Co": .75}, "ok", "off", {"I": 1., "Cu": 1., "Co": 1.}, 0, False, "EM"),

    # --- IN: Inquebrantable, 5 capas, anclas 100%, sueño ok; varía cobertura del superhabit ---
    ("IN0", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 0, True, "P"),
    ("IN1", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 1, True, "P"),
    ("IN2", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 2, True, "I"),
    ("IN3", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 3, True, "I"),
    ("IN4", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 4, True, "I"),
    ("IN5", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 5, True, "I"),
    ("IN6", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 1, True, "P"),
    ("IN7", {"I": 1., "Cu": 1., "Co": 1., "V": 1., "P": 1.}, "ok", "off", None, 5, True, "I"),
]


def _old(idc, M, L, Ca, H, sleep, mark):
    """Re-codifica un caso viejo (Meditar,Leer,Caminar,Higiene; metas 3,4,4,3) al formato v2.
    Interior = avg(frac Meditar, frac Leer); Cuerpo = frac Caminar (+sueño); Conducta = frac Higiene."""
    I = (min(M / 3, 1) + min(L / 4, 1)) / 2
    Cu = min(Ca / 4, 1)
    Co = min(H / 3, 1)
    sl = {"good": "ok", "bad": "mal", "none": "none"}[sleep]
    return (idc, {"I": round(I, 4), "Cu": round(Cu, 4), "Co": round(Co, 4)}, sl, "off", None, 0, False, mark)


# 43 casos viejos (sueño SIEMPRE activo, 3 capas). Fuente: weight_model_fit.py (modelo previo, 40/43).
OLD_CASES = [
    _old("o-C01", 0, 0, 0, 0, "good", "R"), _old("o-C02", 0, 1, 0, 0, "good", "R"),
    _old("o-C03", 1, 1, 1, 1, "good", "A"), _old("o-C04", 1, 2, 2, 1, "good", "A"),
    _old("o-C05", 2, 3, 3, 2, "good", "EM"), _old("o-C06", 3, 4, 3, 3, "good", "P"),
    _old("o-C07", 3, 4, 4, 3, "good", "P"), _old("o-T01", 3, 4, 0, 3, "good", "EM"),
    _old("o-T02", 3, 4, 4, 3, "none", "EM"), _old("o-T03", 3, 0, 4, 0, "good", "A"),
    _old("o-T04", 3, 1, 1, 1, "good", "R"),
    _old("o-BR1", 0, 1, 0, 1, "good", "R"), _old("o-BR2", 1, 1, 0, 1, "good", "R"),
    _old("o-BR3", 1, 2, 1, 0, "good", "A"), _old("o-BA1", 2, 2, 2, 2, "good", "EM"),
    _old("o-BA2", 2, 3, 2, 2, "good", "EM"), _old("o-BA3", 2, 3, 3, 2, "good", "EM"),
    _old("o-BE1", 2, 3, 3, 3, "good", "EM"), _old("o-BE2", 3, 3, 3, 3, "good", "P"),
    _old("o-BE3", 3, 4, 3, 3, "good", "P"), _old("o-WC1", 3, 4, 4, 0, "good", "EM"),
    _old("o-WC2", 3, 4, 2, 3, "good", "P"), _old("o-WC3", 2, 2, 4, 2, "good", "EM"),
    _old("o-RS1", 3, 2, 2, 2, "good", "A"), _old("o-RS2", 3, 1, 1, 1, "good", "A"),
    _old("o-A1", 2, 2, 2, 1, "good", "EM"), _old("o-A2", 3, 4, 0, 0, "good", "A"),
    _old("o-A3", 1, 1, 4, 1, "good", "A"), _old("o-B1", 3, 3, 3, 3, "good", "P"),
    _old("o-B2", 3, 4, 2, 3, "good", "P"), _old("o-B3", 3, 4, 4, 1, "good", "P"),
    _old("o-C1", 2, 3, 2, 2, "good", "EM"), _old("o-C2", 3, 4, 2, 0, "good", "EM"),
    _old("o-D1", 1, 2, 2, 1, "good", "A"), _old("o-D2", 2, 2, 2, 2, "good", "EM"),
    _old("o-K1", 1, 4, 4, 3, "good", "P"), _old("o-K2", 3, 2, 4, 3, "good", "P"),
    _old("o-S1", 3, 4, 4, 3, "bad", "EM"), _old("o-S2", 3, 3, 3, 3, "bad", "EM"),
    _old("o-S3", 3, 4, 4, 3, "none", "EM"), _old("o-J1", 3, 4, 0, 2, "good", "A"),
    _old("o-J2", 1, 2, 4, 2, "good", "EM"), _old("o-J3", 0, 0, 4, 3, "good", "A"),
]

# Inconsistencias irreducibles ya conocidas del fit viejo (40/43): no son fallas del modelo.
KNOWN_OLD = {"o-T04", "o-RS1", "o-A1"}

GRID = dict(
    beta=[0.5, 0.6],
    s_bad=[0.15, 0.2, 0.25, 0.3, 0.35],
    k_sleep=[1.0, 1.5, 2, 2.5],          # 1.0 incluido: sueño puede NO inflar peso (solo valor)
    k_sobr=[2, 2.5, 3],
    r_relapse=[0.3, 0.4, 0.5],
    r_unmarked=[0.4, 0.5],
    p_sop=[0.2, 0.25, 0.3],
    b_sop=[0.0, 0.02, 0.04],
    sop_form=["asym", "lin"],
    cRA=[0.36, 0.40, 0.43],
    cAEM=[0.62, 0.64, 0.66, 0.68],       # ampliado: el corte alto unifica viejas y nuevas (layer count)
    cEMP=[0.84, 0.85, 0.86, 0.88],
)


def layer_val(L, ancla, sleep, sobriety, support, p):
    if L == "Cu" and sleep != "off":
        sv = 1.0 if sleep == "ok" else p["s_bad"]
        core = p["beta"] * sv + (1 - p["beta"]) * ancla
    elif L == "Co" and sobriety != "off":
        core = {"clean": ancla, "relapse": p["r_relapse"], "unmarked": p["r_unmarked"]}[sobriety]
    else:
        core = ancla
    if support is not None:
        s = support
        if p["sop_form"] == "asym":
            core = core + p["b_sop"] * s - p["p_sop"] * (1 - s)
        else:  # lin
            core = (1 - p["p_sop"]) * core + p["p_sop"] * s
    return min(1.0, max(0.0, core))


def weights(actives, sleep, sobriety, p):
    w = {L: 1.0 for L in actives}
    if sleep != "off" and "Cu" in w:
        w["Cu"] *= p["k_sleep"]
    if sobriety != "off" and "Co" in w:
        w["Co"] *= p["k_sobr"]
    tot = sum(w.values())
    return {L: w[L] / tot for L in w}


def predict(case, p):
    _id, layers, sleep, sobriety, support, scap, all100, _exp = case
    actives = list(layers.keys())
    w = weights(actives, sleep, sobriety, p)
    S = sum(w[L] * layer_val(L, layers[L], sleep, sobriety, (support.get(L) if support else None), p)
            for L in actives)
    if S < p["cRA"]:
        st = "R"
    elif S < p["cAEM"]:
        st = "A"
    elif S < p["cEMP"]:
        st = "EM"
    else:
        st = "P"
    if st == "P" and all100 and scap >= 2:
        st = "I"
    return st, S


def margin(S, state, p):
    if state == "R":
        return p["cRA"] - S
    if state == "A":
        return min(S - p["cRA"], p["cAEM"] - S)
    if state == "EM":
        return min(S - p["cAEM"], p["cEMP"] - S)
    return S - p["cEMP"]


def _valid(p):
    if not (p["k_sleep"] >= 1 and p["k_sobr"] > 1 and p["p_sop"] >= p["b_sop"]):
        return False
    if p["sop_form"] == "lin" and p["b_sop"] != 0.0:   # lin ignora b_sop -> sin duplicados
        return False
    return True


def search(cases):
    """Devuelve el óptimo MÁS ROBUSTO (max hits, luego max margen mínimo) + identificabilidad."""
    keys = list(GRID)
    best = None
    max_hits = -1
    seen = {k: set() for k in keys}
    n_opt = 0
    for combo in itertools.product(*[GRID[k] for k in keys]):
        p = dict(zip(keys, combo))
        if not _valid(p):
            continue
        h = 0
        mins = 2.0
        for c in cases:
            pred, S = predict(c, p)
            if pred == c[7]:
                h += 1
                mg = margin(S, pred, p)
                if mg < mins:
                    mins = mg
        if h > max_hits:
            max_hits = h
            best = (h, round(mins, 4), dict(p))
            seen = {k: {p[k]} for k in keys}
            n_opt = 1
        elif h == max_hits:
            n_opt += 1
            for k in keys:
                seen[k].add(p[k])
            if mins > best[1]:
                best = (h, round(mins, 4), dict(p))
    return best, seen, n_opt


def report(label, cases, best, seen, n_opt, show_ident=True):
    hits, mm, p = best
    N = len(cases)
    miss, margins = [], []
    for c in cases:
        pred, S = predict(c, p)
        (margins if pred == c[7] else miss).append(
            (c[0], round(margin(S, pred, p), 4), round(S, 4)) if pred == c[7]
            else (c[0], c[7], pred, round(S, 3)))
    print(f"\n===== {label} =====")
    print(f"{hits}/{N} casos · margen mínimo al corte = {mm} · {n_opt} sets dan {hits}/{N}")
    print("Óptimo más robusto:", {k: p[k] for k in GRID})
    print(f"Fallos ({len(miss)}):", "(ninguno)" if not miss else "")
    for m in miss:
        flag = "  [inconsistencia vieja conocida]" if m[0] in KNOWN_OLD else ""
        print(f"  {m[0]:7} {m[1]:>3} -> {m[2]:<3}  S={m[3]}{flag}")
    print("Casos más ajustados al corte (8 de menor margen):")
    for cid, mg, S in sorted(margins, key=lambda x: x[1])[:8]:
        print(f"  {cid:7} margen={mg:<7} S={S}")
    if show_ident:
        print(f"Identificabilidad (valores entre los {n_opt} óptimos):")
        for k in GRID:
            vals = sorted(seen[k], key=lambda v: (isinstance(v, str), v))
            print(f"  {'PINNED' if len(vals) == 1 else 'libre ':7} {k:10} = {vals}")


def main():
    # ---- FASE 1: solo tandas nuevas ----
    best1, seen1, n1 = search(CASES)
    report("FASE 1 — tandas nuevas (45)", CASES, best1, seen1, n1)
    print("\nDistribución de etiquetas (Fase 1):", dict(Counter(c[7] for c in CASES)))

    # ---- FASE 2: unificación — ¿existe UN punto que ajuste los 88? ----
    allc = CASES + OLD_CASES
    best2, seen2, n2 = search(allc)
    report("FASE 2 — unificado (45 nuevos + 43 viejos = 88)", allc, best2, seen2, n2, show_ident=False)
    hits2, _, p2 = best2
    new_ok = sum(1 for c in CASES if predict(c, p2)[0] == c[7])
    old_ok = sum(1 for c in OLD_CASES if predict(c, p2)[0] == c[7])
    old_breaks = [c[0] for c in OLD_CASES if predict(c, p2)[0] != c[7]]
    new_break = [c[0] for c in CASES if predict(c, p2)[0] != c[7]]
    extra = [x for x in old_breaks if x not in KNOWN_OLD]
    print(f"\nUNIFICACIÓN — con el mejor punto sobre los 88:")
    print(f"  nuevos: {new_ok}/45   viejos: {old_ok}/43   (techo viejo realista = 40/43)")
    print(f"  nuevos que rompen: {new_break or '(ninguno)'}")
    print(f"  viejos que rompen: {old_breaks}")
    print(f"  de esos, NUEVAS rupturas (no las 3 conocidas {sorted(KNOWN_OLD)}): {extra or '(ninguna)'}")
    if not new_break and not extra:
        print("  => UNIFICA: un punto ajusta los 45 nuevos + 40 viejos (solo fallan las 3 conocidas).")
    else:
        print("  => NO unifica limpio: hay tensión estructural (revisar ω=0 vs worst-term).")


def _tanda(cid):
    for t in ("BP", "SU", "SB", "SO", "IN"):
        if cid.startswith(t):
            return t
    return "?"


if __name__ == "__main__":
    main()
