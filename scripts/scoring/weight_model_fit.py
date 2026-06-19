#!/usr/bin/env python3
"""
Ajusta un modelo de PESOS (sin gates/caps) contra las marcas del dueño.

Filosofía (decisión del dueño): el estado EMERGE de bases ponderadas + cortes.
- Sueño = peso pesado (vía Cuerpo).
- Cuerpo pesa más que Interior/Conducta (porque aloja el sueño).
- Un término de "peor capa" (breadth) captura el castigo por abandonar una capa
  entera. NO es un cap: es un peso suave (como el 0.25 worst del motor actual).

Modelo:
  frac = min(hechos/meta, 1)
  Interior  I  = (frac_meditar + frac_leer) / 2
  Conducta  Co = frac_higiene
  Cuerpo    Cu = beta*sueño + (1-beta)*frac_caminar
  avg   = wCu*Cu + wI*I + wCo*Co
  worst = min(I, Co, Cu)
  S     = (1-omega)*avg + omega*worst
  estado: S<cRA→R, <cAEM→A, <cEMP→EM, else P
"""
import itertools

GOOD, BAD, NONE = 1.0, None, None  # sleep flags; BAD/NONE resueltos por params

# (Meditar, Leer, Caminar, Higiene, sleep, mark)  metas: 3,4,4,3
CASES = [
    # CB
    ("C01", 0,0,0,0, "good", "R"),
    ("C02", 0,1,0,0, "good", "R"),
    ("C03", 1,1,1,1, "good", "A"),
    ("C04", 1,2,2,1, "good", "A"),
    ("C05", 2,3,3,2, "good", "EM"),
    ("C06", 3,4,3,3, "good", "P"),
    ("C07", 3,4,4,3, "good", "P"),
    ("T01", 3,4,0,3, "good", "EM"),
    ("T02", 3,4,4,3, "none", "EM"),
    ("T03", 3,0,4,0, "good", "A"),
    ("T04", 3,1,1,1, "good", "R"),
    # REF
    ("BR1", 0,1,0,1, "good", "R"),
    ("BR2", 1,1,0,1, "good", "R"),
    ("BR3", 1,2,1,0, "good", "A"),
    ("BA1", 2,2,2,2, "good", "EM"),
    ("BA2", 2,3,2,2, "good", "EM"),
    ("BA3", 2,3,3,2, "good", "EM"),
    ("BE1", 2,3,3,3, "good", "EM"),
    ("BE2", 3,3,3,3, "good", "P"),
    ("BE3", 3,4,3,3, "good", "P"),
    ("WC1", 3,4,4,0, "good", "EM"),
    ("WC2", 3,4,2,3, "good", "P"),   # corregido: Caminar-mitad + sueño perfecto = P (sueño domina)
    ("WC3", 2,2,4,2, "good", "EM"),
    ("RS1", 3,2,2,2, "good", "A"),
    ("RS2", 3,1,1,1, "good", "A"),   # mismo config que T04 (marcado R) -> inconsistencia
    # REFv2
    ("A1", 2,2,2,1, "good", "EM"),
    ("A2", 3,4,0,0, "good", "A"),
    ("A3", 1,1,4,1, "good", "A"),
    ("B1", 3,3,3,3, "good", "P"),
    ("B2", 3,4,2,3, "good", "P"),    # corregido: = WC2, Caminar-mitad + sueño perfecto = P
    ("B3", 3,4,4,1, "good", "P"),
    ("C1", 2,3,2,2, "good", "EM"),
    ("C2", 3,4,2,0, "good", "EM"),
    ("D1", 1,2,2,1, "good", "A"),
    ("D2", 2,2,2,2, "good", "EM"),
    # REFv3
    ("K1", 1,4,4,3, "good", "P"),
    ("K2", 3,2,4,3, "good", "P"),
    ("S1", 3,4,4,3, "bad", "EM"),
    ("S2", 3,3,3,3, "bad", "EM"),
    ("S3", 3,4,4,3, "none", "EM"),
    ("J1", 3,4,0,2, "good", "A"),
    ("J2", 1,2,4,2, "good", "EM"),
    ("J3", 0,0,4,3, "good", "A"),
]

TARGETS = (3,4,4,3)

def frac(x, t): return min(x/t, 1.0)

def predict(c, p):
    _, M,L,Ca,H, sleep, _ = c
    s = {"good":1.0, "bad":p["sleep_bad"], "none":p["sleep_none"]}[sleep]
    I  = (frac(M,3) + frac(L,4)) / 2
    Co = frac(H,3)
    Cu = p["beta"]*s + (1-p["beta"])*frac(Ca,4)
    avg = p["wCu"]*Cu + p["wI"]*I + p["wCo"]*Co
    # Peor-capa SOLO sobre capas portantes (Cuerpo, Interior). Conducta es liviana:
    # abandonarla no señala problema de base, así que no arrastra (fix B3/C2).
    worst = min(I, Cu)
    S = (1-p["omega"])*avg + p["omega"]*worst
    if S < p["cRA"]: return "R", S
    if S < p["cAEM"]: return "A", S
    if S < p["cEMP"]: return "EM", S
    return "P", S

def score(p):
    hits = 0; miss = []
    for c in CASES:
        pred, S = predict(c, p)
        if pred == c[6]: hits += 1
        else: miss.append((c[0], c[6], pred, round(S,3)))
    return hits, miss

def main():
    best = None
    grid = dict(
        # Principio del dueño: sueño es el peso MÁS pesado -> beta>=0.5 (sueño >= Caminar
        # dentro de Cuerpo) y Cuerpo "pesa muchísimo" -> wCu alto.
        beta=[0.45,0.5,0.55,0.6,0.65,0.7],
        wCu=[0.5,0.55,0.6,0.65,0.7],
        wI=[0.15,0.2,0.25],
        omega=[0.2,0.25,0.3,0.35,0.4],
        cRA=[0.40,0.43,0.46],
        cAEM=[0.58,0.61,0.64],
        cEMP=[0.82,0.84,0.86,0.88,0.90],
        sleep_bad=[0.15,0.2,0.3],
    )
    keys = list(grid)
    for combo in itertools.product(*[grid[k] for k in keys]):
        p = dict(zip(keys, combo))
        p["wCo"] = round(1 - p["wCu"] - p["wI"], 4)
        if p["wCo"] <= 0.05: continue
        # RESTRICCIONES de filosofía (el modelo debe SIGNIFICAR lo correcto):
        #  - Cuerpo es la capa más pesada
        #  - Conducta es la más liviana (Conducta <= Interior)
        #  - el sueño es el peso pesado DE Cuerpo (beta alto)
        if not (p["wCu"] > p["wI"] and p["wCu"] > p["wCo"]): continue
        if p["wCo"] > p["wI"]: continue
        if p["beta"] < 0.45: continue
        p["sleep_none"] = p["sleep_bad"]  # dueño: no-registro ~ mal sueño
        hits, miss = score(p)
        if best is None or hits > best[0]:
            best = (hits, miss, dict(p))
    hits, miss, p = best
    print(f"MEJOR AJUSTE: {hits}/{len(CASES)} casos")
    print("Parámetros:")
    for k in ["beta","wCu","wI","wCo","omega","cRA","cAEM","cEMP","sleep_bad","sleep_none"]:
        print(f"  {k} = {p[k]}")
    print(f"\nFallos ({len(miss)}): caso  esperado→predicho  (S)")
    for m in miss:
        print(f"  {m[0]:4} {m[1]:>3} -> {m[2]:<3}  S={m[3]}")

if __name__ == "__main__":
    main()
