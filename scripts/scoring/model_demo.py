#!/usr/bin/env python3
"""
model_demo.py — Motor de PESOS PUROS. score = Σ(peso_capa × valor_capa) -> estado.
SIN reglas, SIN caps, SIN worst-term. Todo emerge de los pesos.
- Capas normales: pesan IGUAL.
- Sueño opt-in activo: sube el peso de Cuerpo. Sobriedad opt-in activa: sube el de Conducta.
- Un modulador malo hunde el VALOR de su capa. La caída de estado EMERGE.
"""
from weight_model_fit_v2 import CASES

# ===== PESOS Y VALORES (lo único que el motor procesa) =====
K_SLEEP = 1.5   # CONSOLIDADO: sueño activo -> Cuerpo ×1.5 (43% en 3 capas)
K_SOBR = 3.0    # CONSOLIDADO: sobriedad activa -> Conducta ×3 (60% en 3 capas) <- LO QUE ESTAMOS TESTEANDO
BETA = 0.5
S_BAD = 0.15
R_REL = 0.45    # recaída -> valor de Conducta cae a 0.45
R_UNM = 0.45
P_SOP, B_SOP = 0.25, 0.02
CRA, CAEM, CEMP = 0.40, 0.62, 0.85   # bandas R / A / EM / P


def valor(L, ancla, sleep, sob, sup):
    if L == "Cu" and sleep != "off":
        sv = 1.0 if sleep == "ok" else S_BAD
        v = BETA * sv + (1 - BETA) * ancla
    elif L == "Co" and sob != "off":
        v = {"clean": ancla, "relapse": R_REL, "unmarked": R_UNM}[sob]
    else:
        v = ancla
    if sup is not None:
        v = v + B_SOP * sup - P_SOP * (1 - sup)
    return min(1.0, max(0.0, v))


def pesos(layers, sleep, sob):
    w = {L: 1.0 for L in layers}
    if sleep != "off" and "Cu" in w:
        w["Cu"] *= K_SLEEP
    if sob != "off" and "Co" in w:
        w["Co"] *= K_SOBR
    tot = sum(w.values())
    return {L: w[L] / tot for L in w}


def predict(layers, sleep="off", sob="off", support=None, scap=0, all100=False):
    w = pesos(layers, sleep, sob)
    S = sum(w[L] * valor(L, layers[L], sleep, sob, (support.get(L) if support else None)) for L in layers)
    st = "R" if S < CRA else "A" if S < CAEM else "EM" if S < CEMP else "P"
    if st == "P" and all100 and scap >= 2:
        st = "I"
    return st, S


# 1) Reproduce las 45 marcas reales
hits, miss = 0, []
for c in CASES:
    _id, layers, sleep, sob, support, scap, all100, exp = c
    st, _ = predict(layers, sleep, sob, support, scap, all100)
    if st == exp:
        hits += 1
    else:
        miss.append((_id, exp, st))
print(f"Reproduce tus marcas: {hits}/{len(CASES)}")
for m in miss:
    print(f"  difiere {m[0]:6} vos:{m[1]:>3} motor:{m[2]}")

# 2) Cómo quedan los pesos (lo único que procesa el motor)
print("\n--- PESO de cada capa según la situación (3 capas) ---")
for desc, sl, so in [("normal (sin opt-in)", "off", "off"),
                     ("sueño activo", "ok", "off"),
                     ("sobriedad activa", "off", "clean"),
                     ("sueño + sobriedad activos", "ok", "clean")]:
    w = pesos({"I": 1, "Cu": 1, "Co": 1}, sl, so)
    print(f"  {desc:28} Interior {w['I']*100:4.0f}%  Cuerpo {w['Cu']*100:4.0f}%  Conducta {w['Co']*100:4.0f}%")

# 3) DIAGNÓSTICO: ¿pesa demasiado Conducta con sobriedad activa (60%)?
# Interior y Cuerpo SIEMPRE perfectos. Hago caer Orden digital paso a paso.
print("\n--- ¿CONDUCTA PESA DEMASIADO? (Interior 100% + Cuerpo 100% fijos) ---")
print("Con SOBRIEDAD LIMPIA activa (Conducta pesa 60%):")
for orden, etiqueta in [(1.0, "Orden digital 4d (100%)"), (0.75, "Orden digital 3d (75%)"),
                        (0.5, "Orden digital 2d (50%)"), (0.25, "Orden digital 1d (25%)"),
                        (0.0, "Orden digital 0d (0%)")]:
    st, S = predict({"I": 1., "Cu": 1., "Co": orden}, "off", "clean")
    print(f"  {st:3} (S={S:.2f})  {etiqueta} + racha limpia")
st, S = predict({"I": 1., "Cu": 1., "Co": 1.}, "off", "relapse")
print(f"  {st:3} (S={S:.2f})  Orden digital 4d (100%) + RECAÍDA")

print("\nLas MISMAS, pero SIN sobriedad (Conducta pesa 33%, normal) — para contraste:")
for orden, etiqueta in [(0.75, "Orden digital 3d (75%)"), (0.5, "Orden digital 2d (50%)"),
                        (0.25, "Orden digital 1d (25%)"), (0.0, "Orden digital 0d (0%)")]:
    st, S = predict({"I": 1., "Cu": 1., "Co": orden}, "off", "off")
    print(f"  {st:3} (S={S:.2f})  {etiqueta} (sin sobriedad)")
