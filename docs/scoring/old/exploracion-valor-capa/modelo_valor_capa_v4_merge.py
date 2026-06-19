"""
MERGE de las 3 soluciones al arrastre del opt-in (v4).
Opt-in en capa CON anclas = término-sombra de peso DINÁMICO con w(M=1)=0 (invisible cuando está bien).
Opt-in en capa SIN anclas = ES la capa (peso normal W0, valor = señal).
Extra (superhabit) SIEMPRE pesos iguales. Arrastre PLANO exacto + neutralidad EXACTA (incl. déficit).
Reproducible: python3 modelo_valor_capa_v4_merge.py
"""
import math
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
DELTA=0.10; W0=1.0; A_SOB=0.55   # sobriedad: M_sobr=(1-A_SOB)^(días de recaída)
def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")
def beta_from_target(target): return 1.0/target - 1.0
TARGET=0.55; BETA=beta_from_target(TARGET)
def sleep_signal(noches):           # 4 comp por noche, cobertura; sin dato -> B_SLEEP
    have=[n for n in noches if n is not None]; c=len(have)/7.0
    return 0.5 if c==0 else c*(sum(have)/len(have))+(1-c)*0.5
def sobriety_signal(dias_recaida_por_track):  # lista de días de recaída por track
    M=1.0
    for d in dias_recaida_por_track: M*=(1-A_SOB)**d
    return M

def merge(capas, beta=None):
    if beta is None: beta=BETA
    act=[L for L in capas if (L.get('anchors') or L.get('optin') is not None)]
    N=len(act); terms=[]; extras=[]
    for L in act:
        a=L.get('anchors') or []; M=L.get('optin')
        if a:
            ab=sum(min(r,1) for r in a)/len(a); terms.append((ab,W0))
            extras.append(sum(max(r-1,0) for r in a)/len(a))
            if M is not None:
                w=beta*N*(1-M)
                if w>1e-12: terms.append((M,w))
        elif M is not None:
            terms.append((M,W0))
    base=sum(v*w for v,w in terms)/sum(w for _,w in terms) if terms else 0.0
    extra=sum(extras)/len(extras) if extras else 0.0
    return min(base,1.0)+extra

K_INT=4.0
def v3(capas):
    def vc(a,M):
        ab=(sum(min(r,1) for r in a)/len(a)) if a else None
        bs=(ab+K_INT*M)/(1+K_INT) if (M is not None and ab is not None) else (M if M is not None else (ab or 0))
        ex=(sum(max(r-1,0) for r in a)/len(a)) if a else 0.0
        return min(bs,1)+ex
    return sum(vc(L.get('anchors') or [],L.get('optin')) for L in capas)/len(capas)

J=R(4,30,[30]*4); SUP=R(4,30,[30]*6); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30])
print(f"MERGE v4. w_optin=BETA*N*(1-M), W0=1. TARGET={TARGET} (recaida+anclas perfectas) -> BETA={BETA:.3f}")
print(f"J={J:.3f} SUP={SUP:.3f} XL={XL:.3f} DEF(3d)={DEF:.3f}")
print("="*94)
def row(t,capas):
    s3=v3(capas); s4=merge(capas)
    print(f"{t:40s} v3={s3:.3f} {band(s3):11s} | MERGE={s4:.3f} {band(s4)}")
print("--- 8 CASOS (v3 ANTES vs MERGE) ---")
row("P1 justo + sueno bien N=3",[{'anchors':[J]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
row("P2 mal sueno M=.15 N=3",[{'anchors':[J],'optin':0.15}]+[{'anchors':[J]}]*2)
row("P2 mal sueno M=.15 N=5",[{'anchors':[J],'optin':0.15}]+[{'anchors':[J]}]*4)
row("P3 recaida M=0 N=3",[{'anchors':[J],'optin':0.0}]+[{'anchors':[J]}]*2)
row("P3 recaida M=0 N=5",[{'anchors':[J],'optin':0.0}]+[{'anchors':[J]}]*4)
row("P4 sueno regular M=.5 N=5",[{'anchors':[J],'optin':0.5}]+[{'anchors':[J]}]*4)
row("P7 superhabit repartido x3",[{'anchors':[XL]},{'anchors':[XL],'optin':1.0},{'anchors':[XL]}])
row("P8 capa solo-opt-in sueno bien",[{'anchors':[J]},{'anchors':[],'optin':1.0},{'anchors':[J]}])
print("\n--- CRITERIOS ---")
sSol=merge([{'anchors':[XL]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
sTin=merge([{'anchors':[J]},{'anchors':[XL],'optin':1.0},{'anchors':[J]}])
print(f"C5 Sol={sSol:.4f} Tin={sTin:.4f} empatan={abs(sSol-sTin)<1e-9}")
sn=merge([{'anchors':[DEF]},{'anchors':[J]},{'anchors':[J]}])
sc=merge([{'anchors':[DEF],'optin':1.0},{'anchors':[J]},{'anchors':[J]}])
print(f"C2 NEUTRALIDAD con anclas en DEFICIT: sin={sn:.4f} con opt-in bien={sc:.4f} neutro={abs(sn-sc)<1e-9}")
print("C4 anclas intactas: el opt-in es termino aparte, nunca toca el valor del ancla")
print("C3 arrastre PLANO en N (recaida M=0): "+str([round(merge([{'anchors':[J],'optin':0.0}]+[{'anchors':[J]}]*(N-1)),3) for N in [3,4,5,6,7]]))
sMal=merge([{'anchors':[J],'optin':0.15}]+[{'anchors':[J]}]*2); sRec=merge([{'anchors':[J],'optin':0.0}]+[{'anchors':[J]}]*2)
print(f"D8 recaida {sRec:.3f} < mal sueno {sMal:.3f} = {sRec<sMal}")
print("\n--- ANTI-GATE (barrido continuo de M, N=3) ---")
prev=None;mx=0
for i in range(1001):
    M=i/1000; s=merge([{'anchors':[J],'optin':M}]+[{'anchors':[J]}]*2)
    if prev is not None: mx=max(mx,abs(s-prev))
    prev=s
print(f"   paso maximo |dEstado| con dM=0.001 = {mx:.5f}  -> continuo, sin gate")
print("\n--- TARGET -> BETA (el dueno elige cuanto castiga la recaida total) ---")
for tg in [0.40,0.50,0.55,0.62]:
    print(f"   recaida total -> {tg:.2f} ({band(tg)}): BETA={beta_from_target(tg):.3f}")
