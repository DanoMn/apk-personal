#!/usr/bin/env python3
"""Genera casos-limite-soportes-tasks-v2.md con tablas detalladas (config, pesos, promedio, estado).
Usa el modelo MERGE v2 (merge_v2_verificacion). Cada caso estresa un candado del modelo."""
import math
from merge_v2_verificacion import R, band
WS,TAU,N0,PISO,D0,SMAX,S0=0.07,0.06,1.0,0.35,1.0,0.5,0.5
THETA=-S0*math.log(1-TAU/SMAX)

def calc(capa):
    anclas=capa.get('anclas',[]); soportes=capa.get('soportes',[]); n=capa.get('tasks',0)
    Rs=[R(F,T,mins) for _,F,T,mins in anclas]
    base_anc=sum(min(r,1) for r in Rs)/len(Rs) if Rs else None
    extra_anc=sum(max(r-1,0) for r in Rs)/len(Rs) if Rs else 0.0
    if soportes:
        sigs=[min(d/4,1) for _,d in soportes]; G=sum(sigs)/len(sigs)
        base_eff=(1-WS)*base_anc+WS*G if base_anc is not None else G
    else:
        G=None; base_eff=base_anc if base_anc is not None else 0.0
    base_eff=min(max(base_eff,0),1)
    if n>0:
        su=-S0*math.log(1-extra_anc/SMAX) if extra_anc<SMAX else 1e9
        g=1-math.exp(-n/N0); ej=SMAX*(1-math.exp(-(su+THETA*g)/S0)); lift=(ej-extra_anc)*(base_eff**2)
    else:
        lift=0.0
    extra=extra_anc+lift; valor=min(base_eff,1)+extra
    na=len(anclas); masa=PISO+(1-PISO)*(1-math.exp(-na/D0))
    return dict(base_anc=base_anc,extra_anc=extra_anc,G=G,base_eff=base_eff,lift=lift,extra=extra,valor=valor,masa=masa,Rs=Rs)

def cfg_anclas(c):
    if not c.get('anclas'): return "— (sin anclas)"
    out=[]
    for (nom,F,T,mins),r in zip(c['anclas'],[R(F,T,mins) for _,F,T,mins in c['anclas']]):
        out.append(f"{nom} {len([m for m in mins if m>0])}d×{mins[0] if mins else 0} (meta {T})→R={r:.2f}")
    return "; ".join(out)
def cfg_sop(c):
    if not c.get('soportes'): return "—"
    return "; ".join(f"{nom} {d}d→{min(d/4,1):.2f}" for nom,d in c['soportes'])

def emit(f, titulo, prueba, capas, lectura):
    f.write(f"## {titulo}\n\n**Qué candado prueba:** {prueba}\n\n")
    f.write("### Config\n\n| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |\n")
    f.write("|---|---|---|---|\n")
    for c in capas:
        f.write(f"| {c['nombre']} | {cfg_anclas(c)} | {cfg_sop(c)} | {c.get('tasks',0)} |\n")
    f.write("\n### Cálculo por capa\n\n")
    f.write("| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |\n")
    f.write("|---|---|---|---|---|---|---|---|\n")
    rows=[]; num=den=0.0
    for c in capas:
        r=calc(c); rows.append((c,r))
        ba="—" if r['base_anc'] is None else f"{r['base_anc']:.4f}"
        gs="—" if r['G'] is None else f"{r['G']:.4f}"
        f.write(f"| {c['nombre']} | {ba} | {r['extra_anc']:.4f} | {gs} | {r['base_eff']:.4f} | +{r['lift']:.4f} | **{r['valor']:.4f}** | **{r['masa']:.4f}** |\n")
        num+=r['valor']*r['masa']; den+=r['masa']
    sc=num/den
    f.write("\n### Promedio exacto y estado\n\n")
    terms=" + ".join(f"{r['valor']:.3f}×{r['masa']:.3f}" for _,r in rows)
    masas=" + ".join(f"{r['masa']:.3f}" for _,r in rows)
    f.write(f"```\nΣ(valor·masa) = {terms} = {num:.4f}\nΣ(masa)       = {masas} = {den:.4f}\nSCORE = {num:.4f} / {den:.4f} = {sc:.4f}  →  {band(sc)}\n```\n\n")
    f.write(f"**Lectura:** {lectura}\n\n---\n\n")
    return sc

# anclas helper
A_just=lambda nom,F,T: (nom,F,T,[T]*F)
with open("casos-limite-soportes-tasks-v2.md","w") as f:
    f.write("# Casos límite — SOPORTES y TASKS (modelo MERGE v2)\n\n")
    f.write("> **Generado por `casos_limite_gen.py`** (números exactos del modelo MERGE v2, no a mano).\n"
            "> Parámetros: `WS=0.07` (blend soporte), `TAU=0.06` (techo task/capa), `THETA=0.0639`, "
            "`N0=1.0` (saturación task), `PISO=0.35` (peso capa solo-soportes), `smax=0.5` (techo extra).\n"
            "> Bandas: Restauración<0.40 · Atención<0.62 · En marcha<0.85 · Plenitud<1.10 · Inquebrantable≥1.10.\n"
            "> Señal soporte por ítem = `min(días/4,1)`; bloque `G_s = promedio` (no crece con la cantidad).\n\n---\n\n")

    # CASO 1
    emit(f,"Caso 1 — Cumplir-justo exacto (el eje semántico)",
         "El punto 1.0 = entrada a Plenitud. Cumplir todas las anclas en la meta, sin soportes ni tasks, debe dar EXACTAMENTE 1.0.",
         [{'nombre':'Interior','anclas':[A_just('meditar',4,10)]},
          {'nombre':'Cuerpo','anclas':[A_just('caminar',4,30)]},
          {'nombre':'Proyecto','anclas':[A_just('estudiar',5,60)]}],
         "3 anclas justas → cada base=1.0, extra=0. Score=1.0000 = inicio de Plenitud. Es el ancla del eje: cumplir lo pactado te pone en Plenitud, ni más ni menos.")

    # CASO 2
    emit(f,"Caso 2 — Anti-abuso de tasks (techo TAU)",
         "100 tasks en CADA capa desde cumplir-justo NO deben comprar Inquebrantable (≥1.10). Es el candado del techo de task.",
         [{'nombre':'Interior','anclas':[A_just('meditar',4,10)],'tasks':100},
          {'nombre':'Cuerpo','anclas':[A_just('caminar',4,30)],'tasks':100},
          {'nombre':'Proyecto','anclas':[A_just('estudiar',5,60)],'tasks':100}],
         "Cada capa suma su techo de task (~0.06) al extra → valor 1.06. Score=1.0600 = Plenitud. Aunque haga 100 tasks por capa, NUNCA llega a Inquebrantable (1.10). El techo TAU=0.06 lo garantiza.")

    # CASO 3
    emit(f,"Caso 3 — Techo 0.5 del extra con superhabit + tasks (saturación conjunta)",
         "Una capa con superhabit EXTREMO de anclas (extra ya cerca de 0.5) + 100 tasks: el extra total NO puede pasar 0.5.",
         [{'nombre':'Cuerpo (XL)','anclas':[('caminar',4,30,[600]*4)],'tasks':100},
          {'nombre':'Interior','anclas':[A_just('meditar',4,10)]},
          {'nombre':'Proyecto','anclas':[A_just('estudiar',5,60)]}],
         "El Inquebrantable (1.1667) es LEGÍTIMO: viene del superhabit REAL de las anclas de Cuerpo (extra 0.50), NO de las tasks. Lo que el caso prueba es la columna `lift_task = +0.0000` en Cuerpo: las 100 tasks aportan CERO porque la curva ya está en el techo 0.5 (la saturación conjunta no deja pasar). El esfuerzo de tasks no se premia donde las anclas ya alcanzaron la gloria máxima.")

    # CASO 4a y 4b — configs CALIBRADAS (base/capa 0.82 cruza, 0.75 no cruza), frecuencia completa 4/4
    r_cerca, r_lejos = 0.876, 0.825
    emit(f,"Caso 4a — Tasks arañan el cruce SÓLO al ras del borde",
         "Usuario En marcha pero MUY cerca de Plenitud (0.85). Con tasks debe cruzar.",
         [{'nombre':'Interior','anclas':[('meditar',4,10,[10*r_cerca]*4)],'tasks':5},
          {'nombre':'Cuerpo','anclas':[('caminar',4,30,[30*r_cerca]*4)],'tasks':5},
          {'nombre':'Proyecto','anclas':[('estudiar',4,60,[60*r_cerca]*4)],'tasks':5}],
         "Base/capa ≈0.82 (En marcha, a 0.03 del borde 0.85). Las tasks empujan ~+0.04 → score cruza a **Plenitud**. El empujón ALCANZA porque ya estabas al ras.")
    emit(f,"Caso 4b — Tasks NO fabrican el cruce si estás lejos",
         "Mismo usuario pero LEJOS del borde. Las tasks no deben hacerlo cruzar.",
         [{'nombre':'Interior','anclas':[('meditar',4,10,[10*r_lejos]*4)],'tasks':5},
          {'nombre':'Cuerpo','anclas':[('caminar',4,30,[30*r_lejos]*4)],'tasks':5},
          {'nombre':'Proyecto','anclas':[('estudiar',4,60,[60*r_lejos]*4)],'tasks':5}],
         "Base/capa ≈0.75 (En marcha, a 0.10 del borde). Las tasks suman ~+0.03 pero NO alcanzan: sigue **En marcha**. Las tasks arañan, no fabrican estados.")

    # CASO 5
    emit(f,"Caso 5 — Gate base²: sin cimiento, las tasks casi no aportan",
         "Anclas A MEDIAS (base baja) + 10 tasks. El gate base² debe castrar el aporte de las tasks (no hay gloria sin cimiento).",
         [{'nombre':'Interior (medias)','anclas':[('meditar',4,10,[5,5])],'tasks':10},
          {'nombre':'Cuerpo (medias)','anclas':[('caminar',4,30,[15,15])],'tasks':10},
          {'nombre':'Proyecto (medias)','anclas':[('estudiar',5,60,[30,30])],'tasks':10}],
         "Con base ≈0.35, el lift de task se multiplica por base²≈0.12 → casi 0. Las 10 tasks apenas mueven el score: primero hay que sostener las anclas. El gate base² funciona.")

    # CASO 6a y 6b
    emit(f,"Caso 6a — Capa solo-soportes PERFECTA no infla el score",
         "Una capa sin anclas, soportes perfectos. Pesa menos (masa 0.35) → no debe inflar como una capa con anclas.",
         [{'nombre':'Interior','anclas':[A_just('meditar',4,10)]},
          {'nombre':'Cuerpo','anclas':[A_just('caminar',4,30)]},
          {'nombre':'Vínculos (solo-sop)','soportes':[('mensajes',4),('llamar',4)]}],
         "Vínculos vale 1.0 pero pesa 0.35 (vs 0.76 de las capas con anclas). Aporta poco al promedio: una capa de poca sustancia no manda. Score se mantiene en Plenitud sin distorsión.")
    emit(f,"Caso 6b — Capa solo-soportes DESCUIDADA (decisión abierta 5.2)",
         "Misma capa solo-soportes pero TODO descuidado (valor 0). ¿Cuánto arrastra con masa 0.35?",
         [{'nombre':'Interior','anclas':[A_just('meditar',4,10)]},
          {'nombre':'Cuerpo','anclas':[A_just('caminar',4,30)]},
          {'nombre':'Vínculos (solo-sop)','soportes':[('mensajes',0),('llamar',0)]}],
         "Vínculos vale 0.0 (G_s=0). Con masa 0.35 arrastra el score, pero menos que si fuera una capa con anclas (masa 0.76). DECISIÓN 5.2: ¿está bien que valga 0 y arrastre, o querés un piso tibio para que una capa de poca sustancia no hunda tanto?")
print("OK -> casos-limite-soportes-tasks-v2.md")
