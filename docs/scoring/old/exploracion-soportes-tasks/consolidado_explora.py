#!/usr/bin/env python3
"""Exploración del MODELO CONSOLIDADO (peso variable por anclas + Opción 2 superhabit plano + soportes).
NO es el modelo de producción todavía — es para VER casos antes de cerrar el contrato.
- Peso de capa = votos decrecientes por ancla (1ª=1, 2ª=0.6, 3ª=0.36...), techo natural 2.5. Solo-soportes=0.35.
- BASE: ponderada por peso de capa.   SUPERHABIT: promedio PLANO entre capas con anclas (Opción 2, Sol=Tin).
- SOPORTE: blend en la base (WS=0.07).   DÉFICIT: vive en la base (min(R,1)<1)."""
import math
from merge_v2_verificacion import R, band
WS=0.07
def votos(n, r=0.5):   # r=0.5 CERRADO (2026-06-16): cada ancla nueva vale la mitad; techo capa 2.0
    return 0.35 if n==0 else sum(r**k for k in range(n))

def sig_sop(dias_list):           # señal de soporte por ítem = min(días/4,1); bloque = promedio
    return [min(d/4,1) for d in dias_list]

def calc(capas):
    rows=[]
    for c in capas:
        aR=c.get('anclas_R',[]); sup=sig_sop(c.get('sop_dias',[]))
        if aR:
            base_anc=sum(min(r,1) for r in aR)/len(aR); extra_anc=sum(max(r-1,0) for r in aR)/len(aR)
        else:
            base_anc=None; extra_anc=0.0
        if sup:
            G=sum(sup)/len(sup); base_eff=(1-WS)*base_anc+WS*G if base_anc is not None else G
        else:
            G=None; base_eff=base_anc if base_anc is not None else 0.0
        base_eff=min(max(base_eff,0),1)
        rows.append(dict(nom=c['nombre'],na=len(aR),aR=aR,base_anc=base_anc,extra=extra_anc,
                         G=G,base_eff=base_eff,peso=votos(len(aR))))
    sw=sum(r['peso'] for r in rows)
    base_global=sum(r['base_eff']*r['peso'] for r in rows)/sw
    con_anclas=[r for r in rows if r['na']>0]
    extra_global=sum(r['extra'] for r in con_anclas)/len(con_anclas) if con_anclas else 0.0
    estado=base_global+extra_global
    return rows, base_global, extra_global, estado

def recibo(titulo, capas):
    rows,bg,eg,est=calc(capas)
    print("="*92); print(titulo); print("="*92)
    print(f"{'Capa':10} {'#anc':>4} {'R de anclas':22} {'base':>6} {'super':>6} {'sop':>6} {'base_ef':>8} {'PESO':>6}")
    for r in rows:
        rs='['+', '.join(f'{x:.2f}' for x in r['aR'])+']' if r['aR'] else '(solo soportes)'
        ba=f"{r['base_anc']:.3f}" if r['base_anc'] is not None else '  -  '
        g=f"{r['G']:.2f}" if r['G'] is not None else '  -'
        print(f"{r['nom']:10} {r['na']:>4} {rs:22} {ba:>6} {r['extra']:>6.3f} {g:>6} {r['base_eff']:>8.3f} {r['peso']:>6.2f}")
    sw=sum(r['peso'] for r in rows)
    print(f"  CUENTA BASE (ponderada por PESO): Σ(base_ef·peso)/Σpeso = {bg:.4f}")
    print(f"  CUENTA SUPERHABIT (plana, ÷ capas con anclas)         = {eg:.4f}")
    print(f"  >>> ESTADO = {bg:.4f} + {eg:.4f} = {est:.4f}  ->  {band(est)}")
    print()
    return est

# refs de ancla
JU=R(4,30,[30]*4)        # justo (R=1.0)
SH=R(4,30,[60]*4)        # superhabit medio (R≈1.29)
SHX=R(4,30,[120]*4)      # superhabit fuerte (R≈1.46)
DEF=R(4,30,[30,30])      # deficit: 2 de 4 dias (R≈0.54)
DEFL=R(4,30,[30,30,30])  # deficit leve: 3 de 4 dias (R≈0.88)

# ---------- CASO 1: usuario realista, 5 capas, 2-3 anclas, mix superhabit/deficit + soportes ----------
recibo("CASO 1 — 5 capas, mix realista (superhabit en Cuerpo y Proyecto, deficit en Conducta) + soportes",[
 {'nombre':'Interior','anclas_R':[JU,JU],'sop_dias':[4,3]},               # 2 anclas justas + soportes ok
 {'nombre':'Cuerpo','anclas_R':[SH,JU,JU],'sop_dias':[0,2]},              # 3 anclas, 1 superhabit; soportes flojos
 {'nombre':'Conducta','anclas_R':[DEF,JU],'sop_dias':[4]},                # 2 anclas, 1 en deficit
 {'nombre':'Vinculos','anclas_R':[JU],'sop_dias':[4,4]},                  # 1 ancla justa + soportes ok
 {'nombre':'Proyecto','anclas_R':[SHX,JU,JU],'sop_dias':[]},              # 3 anclas, 1 superhabit fuerte
])

# ---------- CASO 2: el MISMO deficit en capa PESADA (3 anclas) vs LIVIANA (1 ancla) ----------
print("CASO 2 — el MISMO deficit duele MAS si esta en la capa pesada (porque pesa mas):")
recibo("  2a) deficit en CUERPO (3 anclas, pesada)",[
 {'nombre':'Cuerpo','anclas_R':[DEF,JU,JU]},
 {'nombre':'Conducta','anclas_R':[JU,JU]},
 {'nombre':'Interior','anclas_R':[JU]},
])
recibo("  2b) MISMO deficit en INTERIOR (1 ancla, liviana)",[
 {'nombre':'Cuerpo','anclas_R':[JU,JU,JU]},
 {'nombre':'Conducta','anclas_R':[JU,JU]},
 {'nombre':'Interior','anclas_R':[DEF]},
])

# ---------- CASO 3: el MISMO superhabit en capa PESADA vs LIVIANA (debe dar IGUAL = Sol=Tin) ----------
print("CASO 3 — el MISMO superhabit da IGUAL este en la capa pesada o liviana (Sol=Tin):")
recibo("  3a) superhabit en CUERPO (3 anclas, pesada)",[
 {'nombre':'Cuerpo','anclas_R':[SHX,JU,JU]},
 {'nombre':'Conducta','anclas_R':[JU,JU]},
 {'nombre':'Interior','anclas_R':[JU]},
])
recibo("  3b) MISMO superhabit en INTERIOR (1 ancla, liviana)",[
 {'nombre':'Cuerpo','anclas_R':[JU,JU,JU]},
 {'nombre':'Conducta','anclas_R':[JU,JU]},
 {'nombre':'Interior','anclas_R':[SHX]},
])
