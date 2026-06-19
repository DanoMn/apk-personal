#!/usr/bin/env python3
"""DISEÑO de casos de calibración (AI-facing, NO para el doc humano).
Modelo v3: peso votos r=0.5, base ponderada, extra plano, soporte blend WS, task sat-conjunta TAU,
capa solo-soportes peso ρ. Busca casos AL BORDE de banda para que la marca del dueño discrimine
el número. Para cada candidato muestra qué banda da con valores BAJO/MEDIO/ALTO del parámetro."""
import math
from merge_v2_verificacion import R, band, SMAX, S0
WS_def=0.07; TAU_def=0.06; RHO_def=0.35; r_peso=0.5; N0=1.0

def votos(n): return RHO_def if n==0 else sum(r_peso**k for k in range(n))

def capa_val(anclas_R, sup_signals, n_tasks, WS, TAU):
    aR=anclas_R or []
    base_anc=sum(min(x,1) for x in aR)/len(aR) if aR else None
    extra_anc=sum(max(x-1,0) for x in aR)/len(aR) if aR else 0.0
    if sup_signals:
        G=sum(sup_signals)/len(sup_signals)
        base_eff=(1-WS)*base_anc+WS*G if base_anc is not None else G
    else:
        base_eff=base_anc if base_anc is not None else 0.0
    base_eff=min(max(base_eff,0),1)
    if n_tasks>0:
        su=-S0*math.log(1-extra_anc/SMAX) if extra_anc<SMAX else 1e9
        THETA=-S0*math.log(1-TAU/SMAX); g=1-math.exp(-n_tasks/N0)
        ej=SMAX*(1-math.exp(-(su+THETA*g)/S0)); extra_anc=extra_anc+(ej-extra_anc)*base_eff**2
    return base_eff, extra_anc, len(aR)

def estado(capas, WS=WS_def, TAU=TAU_def, RHO=RHO_def):
    global RHO_def; RHO_def=RHO
    vals=[capa_val(c.get('a'),c.get('s'),c.get('t',0),WS,TAU) for c in capas]
    pesos=[votos(n) for *_,n in vals]
    base=sum(min(be,1)*p for (be,ex,n),p in zip(vals,pesos))/sum(pesos)
    con=[ex for be,ex,n in vals if n>0]
    extra=sum(con)/len(con) if con else 0.0
    return base+extra

JU=R(4,30,[30]*4); SH=R(4,30,[48]*4); SHX=R(4,30,[120]*4)  # justo / superhabit chico / fuerte
def show(tag, capas, var, valores):
    print(f'{tag}')
    for v in valores:
        kw={var:v}
        e=estado(capas,**kw)
        print(f'    {var}={v}: estado={e:.4f} -> {band(e)}')

print('### WS (blend soporte) — descuidar soportes en capa(s) perfecta(s). ¿saca de P?')
# Cuerpo+Conducta+Interior perfectas, soportes descuidados en las que tienen soporte
show('  W1: 3 capas justas, soportes TODOS descuidados (Cuerpo,Conducta,Interior)',
     [{'a':[JU],'s':[0,0]},{'a':[JU],'s':[0]},{'a':[JU],'s':[0]}], 'WS',[0.05,0.07,0.10,0.15])
show('  W2: borde — anclas un toque bajo justo + soportes descuidados',
     [{'a':[SH],'s':[0,0]},{'a':[JU],'s':[0]},{'a':[JU]}], 'WS',[0.05,0.07,0.10,0.15])

print('\\n### RHO (peso capa solo-soportes) — 3 capas justas + 1 capa solo-soportes')
show('  R1: solo-soportes BIEN sostenida (no debe inflar)',
     [{'a':[JU]},{'a':[JU]},{'a':[JU]},{'s':[1,1]}], 'RHO',[0.25,0.35,0.50])
show('  R2: 3 capas EM (medias) + solo-soportes BIEN — ¿la solo-sop sube el estado?',
     [{'a':[SH]},{'a':[SH]},{'a':[SH]},{'s':[1,1]}], 'RHO',[0.25,0.35,0.50])

print('\\n### SEMANTICA capa solo-soportes DESCUIDADA (vale 0 vs piso) — cuanto arrastra')
for piso in [0.0, 0.3]:
    capas=[{'a':[JU]},{'a':[JU]},{'a':[JU]},{'s':[0,0]}]
    # con piso: base_eff de la solo-sop = max(G, piso). Simulo cambiando la señal a piso
    capas_piso=[{'a':[JU]},{'a':[JU]},{'a':[JU]},{'s':[piso/1.0 if piso>0 else 0]}] if piso>0 else capas
    e=estado([{'a':[JU]},{'a':[JU]},{'a':[JU]},{'s':[max(0,piso)]}])
    print(f'    solo-sop descuidada, piso={piso}: estado={e:.4f} -> {band(e)}')

print('\\n### TAU (techo task) — anti-abuso y araña-cruce')
show('  T1: cumplir-justo TODAS + muchas tasks en todas (no debe comprar I)',
     [{'a':[JU],'t':100},{'a':[JU],'t':100},{'a':[JU],'t':100}], 'TAU',[0.04,0.06,0.08,0.10])
show('  T2: al borde de I (superhabit real) + tasks — ¿cruza a I?',
     [{'a':[SHX],'t':5},{'a':[SHX],'t':5},{'a':[JU],'t':5}], 'TAU',[0.04,0.06,0.08,0.10])
