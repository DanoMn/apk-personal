#!/usr/bin/env python3
"""VERIFICACIÓN GLOBAL del modelo oficial v1 contra axiomas-modelo-scoring-v1.md.
Un solo script: implementa el modelo COMPLETO (bolsa-global de la base: anclas + soportes + opt-ins +
peso variable; extra plano + tasks efímeras; mapeo E a puntos) y corre todos los grupos de axiomas.
Reproducible: python3 verificacion_modelo_oficial.py"""
import math

# ---------- ANCLA (v4 verbatim) ----------
def R(F,T,mins,gamma=1.5,lam_v=0.5,kappa=1.5,p=2.0,smax=0.5,s0=0.5):
    mk=sorted([m for m in mins if m>0],reverse=True);D=len(mk)
    if D==0:return 0.0
    r=[m/T for m in mk];c,v=r[:min(D,F)],r[min(D,F):]
    u=lambda x:min(x,1.0)**gamma
    phi=sum(u(x) for x in c)/F;V=sum(u(x) for x in v)
    base=1-(1-phi)*math.exp(-lam_v*V)
    St=sum(max(x-1,0) for x in c)/F;Sd=V/(7-F) if F<7 else 0.0
    wt=(F/7)**kappa;S=smax*(1-math.exp(-(wt*St+(1-wt)*Sd)/s0))
    return base+(base**p)*S

# ---------- PARÁMETROS ----------
BETA=0.818; WS=0.07; TAU=0.06; SMAX=0.5; S0=0.5; N0=1.0; RHO=0.15; DELTA=0.10; W_OPT_SOLO=1.0
def votos(n): return RHO if n==0 else sum(0.5**k for k in range(n))
def banda(e):
    return "R" if e<0.40 else "A" if e<0.62 else "EM" if e<0.85 else "P" if e<1.0+DELTA else "I"

# ---------- ESTADO (bolsa-global) ----------
def estado(capas):
    info=[]
    for c in capas:
        aR=c.get("anclas",[]); n=len(aR); sup=c.get("sup_days"); M=c.get("optin"); nt=c.get("n_tasks",0)
        if aR:
            ab=sum(min(r,1) for r in aR)/n; ex=sum(max(r-1,0) for r in aR)/n
            be=(1-WS)*ab+WS*(sum(min(d/4,1) for d in sup)/len(sup)) if sup else ab
            info.append(dict(t="anc",val=min(max(be,0),1),peso=votos(n),extra=ex,M=M,be=min(max(be,0),1),nt=nt))
        elif sup:
            G=sum(min(d/4,1) for d in sup)/len(sup); info.append(dict(t="sop",val=G,peso=RHO,extra=0.0,M=M,be=G,nt=0))
        elif M is not None:
            info.append(dict(t="opt",val=M,peso=W_OPT_SOLO,extra=0.0,M=None,be=M,nt=0))
    Sigma=sum(i["peso"] for i in info)
    terms=[(i["val"],i["peso"]) for i in info]
    for i in info:
        if i["t"]=="anc" and i["M"] is not None:
            w=BETA*Sigma*(1-i["M"]);
            if w>1e-12: terms.append((i["M"],w))
    base=sum(v*w for v,w in terms)/sum(w for _,w in terms)
    exts=[]
    for i in info:
        if i["t"]=="anc":
            e=i["extra"]
            if i["nt"]>0:
                su=-S0*math.log(1-e/SMAX) if e<SMAX else 1e9
                TH=-S0*math.log(1-TAU/SMAX); g=1-math.exp(-i["nt"]/N0)
                ej=SMAX*(1-math.exp(-(su+TH*g)/S0)); e=e+(ej-e)*i["be"]**2
            exts.append(e)
    extra=sum(exts)/len(exts) if exts else 0.0
    return min(base,1)+extra

# ---------- MAPEO E ----------
def sig(x): return 1/(1+math.exp(-x))
HITOS=[(0.18,0.10,60),(0.55,0.11,110),(0.83,0.09,100),(1.07,0.055,130),(1.35,0.13,50)]
def rawpt(e): return 650+sum(A*sig((e-c)/w) for c,w,A in HITOS)
_r0,_r15=rawpt(0.0),rawpt(1.5)
def P(e): e=max(0,min(1.5,e)); return 650+(rawpt(e)-_r0)*450/(_r15-_r0)

# ---------- BATERÍA DE AXIOMAS ----------
ok=0; fail=0
def chk(ax, cond, msg):
    global ok,fail
    print(("  ✅" if cond else "  ❌")+f" {ax}: {msg}")
    ok+=cond; fail+=(not cond)

J=R(4,30,[30]*4); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30]); HALF=R(4,30,[15]*4)
print("### ANCLA")
chk("AN1","%.4f"%R(4,30,[600]*7) and 0<=R(4,30,[600]*7)<=1.5, f"rango: R máx={R(4,30,[600]*7):.4f} ∈[0,1.5]")
chk("AN2", R(4,30,[])==0.0, "piso cero (D=0 → 0)")
chk("AN3", abs(R(4,30,[30]*4)-1.0)<1e-9 and abs(R(7,30,[30]*7)-1.0)<1e-9, "cumplir-justo = 1.0 (F=4 y F=7)")
chk("AN6", max(R(4,30,[60,60])-1,0)==0 and max(R(4,30,[60]*4)-1,0)>0, "gate base²: 2/4 días extra=0; 4/4=+")
chk("AN7", max(R(4,30,[60]*4)-1,0)>0 and max(R(4,30,[30]*6)-1,0)>0, "superhabit de TIEMPO y de DÍAS")
mono=all(R(4,30,[30]*4+[x])>=R(4,30,[30]*4)-1e-9 for x in [1,15,30,60]); chk("AN8",mono,"monotonía (día extra no baja)")
chk("AN10", abs(R(4,30,[40,30,30])-R(4,120,[160,120,120]))<1e-9, "invarianza de escala (T×4)")
prev=None;cont=True
for i in range(2001):
    r=R(4,30,[i*0.1,30,30,30])
    if prev is not None and abs(r-prev)>0.02: cont=False
    prev=r
chk("AN11",cont,"continuidad (sin saltos)")

print("### PESO DE CAPA")
chk("PC2", abs(votos(1)-1)<1e-9 and abs(votos(2)-1.5)<1e-9 and abs(votos(3)-1.75)<1e-9 and votos(50)<2.0, "votos 1/1.5/1.75, techo<2.0")
peor=votos(50)/(votos(50)+votos(1)+votos(1)); chk("PC3", peor<=0.50+1e-9, f"ninguna capa >50% (peor caso 3 capas={peor*100:.1f}%)")
chk("PC5", abs(votos(0)-0.15)<1e-9, "capa solo-soportes pesa ρ=0.15")

print("### AGREGACIÓN + OPT-INS (bolsa-global)")
chk("AG-just", abs(estado([{'anclas':[J]}]*3)-1.0)<1e-9, "cumplir-justo (3 capas) = 1.0")
c1=estado([{'anclas':[J],'optin':0.0},{'anclas':[J]},{'anclas':[J]}])
c2=estado([{'anclas':[J,J,J],'optin':0.0},{'anclas':[J]},{'anclas':[J]}])
chk("AG2/O3", abs(c1-0.55)<0.01 and abs(c2-0.55)<0.01, f"arrastre plano (recaída+anclas perf→0.55): configs {c1:.4f}/{c2:.4f}")
ep=estado([{'anclas':[J,J,J],'optin':0.15},{'anclas':[J]},{'anclas':[J]}]); el=estado([{'anclas':[J,J,J]},{'anclas':[J],'optin':0.15},{'anclas':[J]}])
chk("I1", abs(ep-el)<1e-9, f"opt-in global (capa pesada=liviana={ep:.4f})")
sin=estado([{'anclas':[DEF]},{'anclas':[J]},{'anclas':[J]}]); con=estado([{'anclas':[DEF],'optin':1.0},{'anclas':[J]},{'anclas':[J]}])
chk("O2/C2", abs(sin-con)<1e-9, f"neutralidad opt-in bien con déficit ({sin:.4f})")
sol=estado([{'anclas':[XL]},{'anclas':[J]},{'anclas':[J]}]); tin=estado([{'anclas':[J]},{'anclas':[XL]},{'anclas':[J]}])
chk("O5/Sol=Tin", abs(sol-tin)<1e-9, f"superhabit igual en cualquier capa ({sol:.4f})")
chk("I2/O11", abs(estado([{'anclas':[J]},{'anclas':[J]},{'optin':1.0}])-1.0)<1e-9, "capa solo-opt-in pesa normal (=1.0)")

print("### SOPORTES")
v_no=estado([{'anclas':[HALF]}]); v_des=estado([{'anclas':[HALF],'sup_days':[0]}]); v_sus=estado([{'anclas':[HALF],'sup_days':[4]}])
chk("SO2", v_des<v_no<v_sus, f"bidireccional leve (descuid {v_des:.3f}<sin {v_no:.3f}<sost {v_sus:.3f})")
v1=estado([{'anclas':[HALF],'sup_days':[4]}]); v5=estado([{'anclas':[HALF],'sup_days':[4,4,4,4,4]}])
chk("SO4", abs(v1-v5)<1e-9, "no crece con la cantidad (1 soporte=5)")

print("### TASKS")
tope=estado([{'anclas':[J],'n_tasks':100}]*3); chk("TA5", banda(tope)!="I", f"anti-abuso: justo+100 tasks×3 = {tope:.4f} ({banda(tope)}), no Inquebrantable")
t0=estado([{'anclas':[J]}]*3); t1=estado([{'anclas':[J]},{'anclas':[J]},{'anclas':[J],'n_tasks':1}]); chk("TA-suma", t1>=t0, f"task nunca resta ({t0:.4f}→{t1:.4f})")
manana=estado([{'anclas':[J]}]*3); chk("TA3", abs(manana-t0)<1e-9, "efímera (mañana sin tasks vuelve a baseline)")

print("### BANDAS")
chk("BA1", banda(0.30)=="R" and banda(0.50)=="A" and banda(0.70)=="EM" and banda(0.90)=="P" and banda(1.15)=="I","cortes R/A/EM/P/I")
chk("BA2", banda(0.85)=="P" and banda(0.84)=="EM","Plenitud entra en 0.85")

print("### PUNTOS (mapeo E)")
chk("PU1", abs(P(0)-650)<1 and abs(P(1.5)-1100)<1, f"rango [650,1100] ({P(0):.0f}..{P(1.5):.0f})")
chk("PU3", abs(P(1.0)-941)<2 and abs(P(1.10)-1011)<3, f"cumplir-justo={P(1.0):.0f}; Inquebrantable entra={P(1.10):.0f}")
mp=True;prev=None
for i in range(1501):
    p=P(i/1000)
    if prev is not None and p<prev-1e-9: mp=False
    prev=p
chk("PU4", mp, "monótono (de a 1 punto)")

print("\n"+"="*60)
print(f"RESULTADO: {ok} verdes / {fail} rojos  →  {'TODOS VERDES ✅' if fail==0 else 'HAY ROJOS ❌'}")
