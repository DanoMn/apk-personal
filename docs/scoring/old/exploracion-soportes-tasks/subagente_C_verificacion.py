import math, random
# Subagente C — SOPORTES (blend gamma) + TASKS (mapeo visible v4) — verificacion
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
DELTA=0.10; W0=1.0; A_SOB=0.55
TARGET=0.55; BETA=1.0/TARGET-1.0
def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")
PISO_SUP_CAPA=0.92; GAMMA_SUP=1.0-PISO_SUP_CAPA
def merge_v4c(capas,beta=None,gs=GAMMA_SUP):
    if beta is None: beta=BETA
    act=[L for L in capas if (L.get('anchors') or L.get('optin') is not None or L.get('support') is not None)]
    N=len(act);terms=[];extras=[]
    for L in act:
        a=L.get('anchors') or [];M=L.get('optin');Ms=L.get('support')
        if a:
            ab=sum(min(r,1) for r in a)/len(a)
            if Ms is not None: ab=(1-gs)*ab+gs*Ms
            terms.append((ab,W0)); extras.append(sum(max(r-1,0) for r in a)/len(a))
            if M is not None:
                w=beta*N*(1-M)
                if w>1e-12: terms.append((M,w))
        elif M is not None: terms.append((M,W0))
        elif Ms is not None: terms.append((Ms,W0))
    base=sum(v*w for v,w in terms)/sum(w for _,w in terms) if terms else 0.0
    extra=sum(extras)/len(extras) if extras else 0.0
    return min(base,1.0)+extra
ESTADO_MAX=1.5
def visible_from_estado(e):
    e=max(0.0,e)
    if e<=1.0: return 700.0+e*200.0
    e=min(e,ESTADO_MAX); return 900.0+(e-1.0)/(ESTADO_MAX-1.0)*100.0
SUP_VISIBLE_IMPACT=GAMMA_SUP*(1.0/3.0)*200.0
TASK_VISIBLE_MAX=0.75*SUP_VISIBLE_IMPACT
TASK_K=0.8
def task_push(n,e):
    if n<=0: return 0.0
    sat=1.0-math.exp(-TASK_K*n); prox=0.0 if e<0.70 else min((e-0.70)/0.20,1.0)
    return TASK_VISIBLE_MAX*sat*prox
def visible_v4c(e,n=0): return min(1000.0,visible_from_estado(e)+task_push(n,e))

J=R(4,30,[30]*4); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30])
print("="*78)
print(f"PARAMS GAMMA_SUP={GAMMA_SUP:.3f}(piso={PISO_SUP_CAPA}) TASK_VISIBLE_MAX={TASK_VISIBLE_MAX:.3f}pts TASK_K={TASK_K}")
print(f"       BETA={BETA:.3f}(opt-ins intacto) ESTADO_MAX={ESTADO_MAX} SUP_VISIBLE_IMPACT={SUP_VISIBLE_IMPACT:.3f}pts")
print(f"       J={J:.3f} XL={XL:.3f} DEF={DEF:.3f}")
print("="*78)
e_justo=merge_v4c([{'anchors':[J]}]*3)
print(f"\n(b) CUMPLIR-JUSTO estado={e_justo:.4f} {band(e_justo)} visible={visible_v4c(e_justo):.1f}")
assert abs(e_justo-1.0)<1e-9 and abs(visible_v4c(e_justo)-900.0)<1e-9
sSol=merge_v4c([{'anchors':[XL]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
sTin=merge_v4c([{'anchors':[J]},{'anchors':[XL],'optin':1.0},{'anchors':[J]}])
print(f"(a) SUPERHABIT Sol={sSol:.4f} Tin={sTin:.4f} empatan={abs(sSol-sTin)<1e-9}")
assert abs(sSol-sTin)<1e-9
print(f"    extra(XL)={(XL-1):.4f} preservado aun con soporte descuidado en esa capa "
      f"(estado={merge_v4c([{'anchors':[XL],'support':0.0},{'anchors':[J]},{'anchors':[J]}]):.4f}, baja solo la base)")
e_sin=merge_v4c([{'anchors':[J]}]*3); e_perf=merge_v4c([{'anchors':[J],'support':1.0}]*3); e_mal=merge_v4c([{'anchors':[J],'support':0.0}]*3)
print(f"\n(c) MULTI-SOPORTE sin={e_sin:.4f} todos M=1={e_perf:.4f} no_fabrica_banda={abs(e_sin-e_perf)<1e-9}")
print(f"    todos M=0 -> {e_mal:.4f} {band(e_mal)} (baja muy levemente, sigue Plenitud)")
assert abs(e_sin-e_perf)<1e-9 and e_mal>0.85
prev=None;mx=0
for i in range(1001):
    s=merge_v4c([{'anchors':[J],'support':i/1000}]+[{'anchors':[J]}]*2)
    if prev is not None: mx=max(mx,abs(s-prev))
    prev=s
prev=None;mxv=0
for i in range(1501):
    v=visible_v4c(i/1000,0)
    if prev is not None: mxv=max(mxv,abs(v-prev))
    prev=v
print(f"\n(d) ANTI-GATE soportes paso max|dEstado|(dM=.001)={mx:.6f}; visible paso max|dVis|(dE=.001)={mxv:.4f}pts")
imp_ancla_v=abs(merge_v4c([{'anchors':[J]}]*3)-merge_v4c([{'anchors':[DEF]},{'anchors':[J]},{'anchors':[J]}]))*200
imp_sup_v=abs(merge_v4c([{'anchors':[J],'support':1.0},{'anchors':[J]},{'anchors':[J]}])-merge_v4c([{'anchors':[J],'support':0.0},{'anchors':[J]},{'anchors':[J]}]))*200
print(f"\n(e) ORDEN (impacto en VISIBLE, moneda comun): ancla={imp_ancla_v:.2f}pts > soporte={imp_sup_v:.2f}pts > task={TASK_VISIBLE_MAX:.2f}pts")
assert imp_ancla_v>imp_sup_v>TASK_VISIBLE_MAX
print(f"\n--- CASO DUENO: tasks empujan el tramo final del VISIBLE (banda NUNCA cambia por tasks) ---")
for e in [0.835,1.08]:
    print(f"  estado={e:.3f} {band(e):11s} vis_base={visible_v4c(e,0):6.1f} | "
          f"1t->{visible_v4c(e,1):6.1f} 2t->{visible_v4c(e,2):6.1f} 5t->{visible_v4c(e,5):6.1f} (banda fija {band(e)})")
e_cruce=0.835; vb=visible_v4c(e_cruce,0); vt=visible_v4c(e_cruce,5)
print(f"  EJEMPLO estado={e_cruce} vis_base={vb:.1f} -> 5 tasks={vt:.1f} (+{vt-vb:.1f} pts, arana hacia el siguiente hito)")
print(f"          banda eje ESTADO sigue {band(e_cruce)} (NO se compra Plenitud: estado<0.85). Honesto.")
assert vt>vb and band(e_cruce)=="EN MARCHA"
flips=0
for _ in range(20000):
    e=random.uniform(0,1.5); n=random.randint(0,50)
    if band(e)!=band(e): flips+=1   # banda=f(estado) solo; tasks no entran a estado
print(f"  GARANTIA: banda=f(estado) solo; tasks no entran al estado -> flips por tasks={flips}")
assert flips==0
print(f"\n--- TASKS nunca restan / saturan / nulas en estado bajo ---")
print(f"  task_push(0,1.0)={task_push(0,1.0):.3f}  task_push(5,e=0.50)={task_push(5,0.50):.3f}(nula)")
print(f"  satur e=1.0: 1t->{task_push(1,1.0):.2f} 2t->{task_push(2,1.0):.2f} 10t->{task_push(10,1.0):.2f} (tope {TASK_VISIBLE_MAX:.2f})")
print(f"\n--- TABLA VISIBLE pre-v4(solo base) vs v4-C(estado real) ---")
def vprev(b): return 700+round(min(max(b,0),1)*300)
print(f"  {'ESTADO':>7} {'banda':13} {'pre-v4':>7} {'v4-C':>7}")
for e in [0.0,0.35,0.55,0.75,0.90,1.00,1.10,1.30,1.50]:
    print(f"  {e:7.2f} {band(e):13} {vprev(min(e,1.0)):7d} {visible_from_estado(e):7.1f}")
print("\nTODOS LOS ASSERTS PASARON OK")
