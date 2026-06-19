#!/usr/bin/env python3
"""RECONCILIACIÓN: opt-in (término-sombra independiente) × peso de capa VARIABLE.
Bolsa-global unificada de la base. Clave derivada: el opt-in escala con Σpesos (no N) para mantener
el arrastre plano y BETA=0.818. Verifica que los axiomas del opt-in (v4) sobreviven al peso variable,
y ataca I1 (opt-in en capa pesada vs liviana), I2 (capa solo-opt-in), I3 (soporte+opt-in misma capa)."""
import math
from merge_v2_verificacion import R
BETA=0.818; WS=0.07; TAU=0.06; SMAX=0.5; S0=0.5; N0=1.0; RHO=0.15; W_OPT_SOLO=1.0
def votos(n): return RHO if n==0 else sum(0.5**k for k in range(n))
def band(s): return 'R' if s<0.40 else 'A' if s<0.62 else 'EM' if s<0.85 else 'P' if s<1.10 else 'I'

def estado(capas):
    # 1) info por capa
    info=[]
    for c in capas:
        aR=c.get('anclas',[]); n=len(aR); sup=c.get('sup_days'); M=c.get('optin'); nt=c.get('n_tasks',0)
        if aR:
            ab=sum(min(r,1) for r in aR)/n; ex=sum(max(r-1,0) for r in aR)/n
            be=(1-WS)*ab+WS*(sum(min(d/4,1) for d in sup)/len(sup)) if sup else ab
            info.append(dict(t='anc', val=min(max(be,0),1), peso=votos(n), extra=ex, M=M, be=min(max(be,0),1), nt=nt))
        elif sup:
            G=sum(min(d/4,1) for d in sup)/len(sup); info.append(dict(t='sop', val=G, peso=RHO, extra=0.0, M=M, be=G, nt=0))
        elif M is not None:
            info.append(dict(t='opt', val=M, peso=W_OPT_SOLO, extra=0.0, M=None, be=M, nt=0))  # O11: opt-in ES la capa
    Sigma=sum(i['peso'] for i in info)              # suma de pesos de capa
    # 2) bolsa-global de la base
    terms=[(i['val'], i['peso']) for i in info]
    for i in info:
        if i['t']=='anc' and i['M'] is not None:
            w=BETA*Sigma*(1-i['M'])                  # <-- opt-in escala con Σpesos (generaliza BETA·N)
            if w>1e-12: terms.append((i['M'], w))
    base=sum(v*w for v,w in terms)/sum(w for _,w in terms)
    # 3) extra (pesos iguales) + tasks efímeras por capa
    exts=[]
    for i in info:
        if i['t']=='anc':
            e=i['extra']
            if i['nt']>0:
                su=-S0*math.log(1-e/SMAX) if e<SMAX else 1e9
                TH=-S0*math.log(1-TAU/SMAX); g=1-math.exp(-i['nt']/N0)
                ej=SMAX*(1-math.exp(-(su+TH*g)/S0)); e=e+(ej-e)*i['be']**2
            exts.append(e)
    extra=sum(exts)/len(exts) if exts else 0.0
    return min(base,1)+extra

J=R(4,30,[30]*4); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30])
print("RECONCILIACIÓN opt-in × peso variable.  w_optin = BETA·Σpesos·(1−M),  BETA=0.818")
print("="*78)
# C3 — arrastre plano / target 0.55 con CONFIGS DE PESO DISTINTAS
print("C3 recaída total (M=0) + anclas perfectas → debe dar 0.55 SIEMPRE:")
print(f"   3 capas 1-ancla c/u, opt-in en una: {estado([{'anclas':[J],'optin':0.0},{'anclas':[J]},{'anclas':[J]}]):.4f}")
print(f"   capas 3/1/1 anclas, opt-in en la de 3: {estado([{'anclas':[J,J,J],'optin':0.0},{'anclas':[J]},{'anclas':[J]}]):.4f}")
print(f"   capas 3/1/1 anclas, opt-in en una de 1: {estado([{'anclas':[J,J,J]},{'anclas':[J],'optin':0.0},{'anclas':[J]}]):.4f}")
# I1 — opt-in malo en capa PESADA vs LIVIANA → mismo arrastre (es global)
print("\nI1 opt-in (M=0.15, mal sueño) en capa pesada (3 anclas) vs liviana (1 ancla):")
ep=estado([{'anclas':[J,J,J],'optin':0.15},{'anclas':[J]},{'anclas':[J]}])
el=estado([{'anclas':[J,J,J]},{'anclas':[J],'optin':0.15},{'anclas':[J]}])
print(f"   en capa pesada={ep:.4f} {band(ep)} | en capa liviana={el:.4f} {band(el)} | iguales={abs(ep-el)<1e-9}")
# C2 — neutralidad (opt-in bien con anclas en déficit)
print("\nC2 neutralidad (opt-in M=1 no cambia nada, aun con déficit):")
sin=estado([{'anclas':[DEF]},{'anclas':[J]},{'anclas':[J]}]); con=estado([{'anclas':[DEF],'optin':1.0},{'anclas':[J]},{'anclas':[J]}])
print(f"   sin opt-in={sin:.4f} | con opt-in bien={con:.4f} | neutro={abs(sin-con)<1e-9}")
# Sol=Tin
print("\nSol=Tin (superhabit rinde igual en cualquier capa):")
sol=estado([{'anclas':[XL]},{'anclas':[J]},{'anclas':[J]}]); tin=estado([{'anclas':[J]},{'anclas':[XL]},{'anclas':[J]}])
print(f"   Sol={sol:.4f} Tin={tin:.4f} empatan={abs(sol-tin)<1e-9}")
# cumplir-justo
print(f"\ncumplir-justo (3 capas justas) = {estado([{'anclas':[J]}]*3):.4f}")
# I2 — capa solo-opt-in
print(f"\nI2 capa solo-opt-in (sueño bien) junto a 2 capas justas = {estado([{'anclas':[J]},{'anclas':[J]},{'optin':1.0}]):.4f} (debe ~1.0, peso normal O11)")
# I3 — soporte + opt-in en la misma capa (Cuerpo: anclas + soportes + sueño)
print("\nI3 soporte + opt-in en la MISMA capa (Cuerpo):")
base3=estado([{'anclas':[J]},{'anclas':[J]},{'anclas':[J]}])
i3=estado([{'anclas':[J],'sup_days':[0,0],'optin':0.15},{'anclas':[J]},{'anclas':[J]}])
print(f"   3 justas={base3:.4f} | Cuerpo con soportes descuidados + mal sueño={i3:.4f} {band(i3)} (ambos arrastran, coexisten)")
# anti-gate barrido de M
mx=0;prev=None
for k in range(1001):
    M=k/1000; s=estado([{'anclas':[J],'optin':M}]+[{'anclas':[J]}]*2)
    if prev is not None: mx=max(mx,abs(s-prev))
    prev=s
print(f"\nanti-gate (barrido M, paso 0.001): paso máx |dEstado| = {mx:.6f} (continuo)")
