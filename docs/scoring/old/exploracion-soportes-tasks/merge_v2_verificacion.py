#!/usr/bin/env python3
"""MERGE v2 (orquestador) — SOPORTES y TASKS dentro del valor de capa (Forma A).
Síntesis: soporte = blend convexo (A≡B); task = saturación CONJUNTA reparametrizada (B, respeta 0.5
y encoge con el superhabit); peso de capa = piso + (1-piso)*densidad de anclas (A+B); techo de task
< 0.10 (hallazgo unánime; el 0.1 del dueño rompe el candado anti-abuso)."""
import math

# --- motor v4 VERBATIM ---
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
SMAX=0.5; S0=0.5; DELTA=0.10

# --- parámetros del MERGE (despejados) ---
WS   = 0.07           # blend del soporte (B): neglect total de capa justa baja la base 0.07
TAU  = 0.06           # techo de task por capa (C): cumplir-justo+tasks-full(todas)=1.06<1.10
THETA= -S0*math.log(1-TAU/SMAX)   # presupuesto pre-saturación de task (B), da lift máx = TAU
N0   = 1.0            # saturación por conteo: 1ª task ~63% del techo
PISO = 0.35           # piso de peso de capa solo-soportes
D0   = 1.0            # densidad de anclas: 1 ancla -> 0.63

def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else "EN MARCHA"
            if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")

def capa(anchors=None, sup_days=None, n_tasks=0):
    """anchors: lista de R; sup_days: lista de días sostenidos por soporte (0..7); n_tasks: tasks de HOY"""
    anchors = anchors or []
    base_anc  = sum(min(r,1) for r in anchors)/len(anchors) if anchors else None
    extra_anc = sum(max(r-1,0) for r in anchors)/len(anchors) if anchors else 0.0
    # --- SOPORTE: blend convexo en la base (no crece con cantidad: G_s = promedio) ---
    if sup_days:
        G_s = sum(min(d/4,1) for d in sup_days)/len(sup_days)
        base_eff = (1-WS)*base_anc + WS*G_s if base_anc is not None else G_s
    else:
        base_eff = base_anc if base_anc is not None else 0.0
    base_eff = min(max(base_eff,0),1)
    # --- TASK: saturación conjunta reparametrizada (respeta 0.5, encoge con superhabit, gate base²) ---
    if n_tasks>0:
        su_anc = -S0*math.log(1-extra_anc/SMAX) if extra_anc<SMAX else 1e9
        g_task = 1-math.exp(-n_tasks/N0)
        extra_joint = SMAX*(1-math.exp(-(su_anc+THETA*g_task)/S0))
        task_lift = (extra_joint-extra_anc)*(base_eff**2)
    else:
        task_lift = 0.0
    extra = extra_anc + task_lift
    valor = min(base_eff,1)+extra
    n_anc = len(anchors)
    masa = PISO + (1-PISO)*(1-math.exp(-n_anc/D0))   # densidad continua; 0 anclas -> piso
    return valor, masa

def score(capas):
    vs = [capa(**c) for c in capas]
    num = sum(v*m for v,m in vs); den = sum(m for _,m in vs)
    return num/den if den else 0.0

J=R(4,30,[30]*4); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30]); HALF=R(4,30,[15]*4)
print(f"MERGE v2  WS={WS} TAU={TAU} THETA={THETA:.4f} N0={N0} PISO={PISO} D0={D0}")
print(f"J(justo)={J:.4f} XL={XL:.4f} DEF={DEF:.4f} HALF(base~.35)={HALF:.4f}")
print("="*88)

# (a) Sol=Tin
sol=score([{'anchors':[XL]},{'anchors':[J]},{'anchors':[J]}])
tin=score([{'anchors':[J]},{'anchors':[XL]},{'anchors':[J]}])
print(f"(a) Sol={sol:.6f} Tin={tin:.6f} empatan={abs(sol-tin)<1e-9}")
# (b) cumplir-justo
b=score([{'anchors':[J]}]*3); print(f"(b) cumplir-justo={b:.4f} {band(b)}")
# (c) soporte bidireccional + no crece con cantidad (capa base media)
v_no,_=capa(anchors=[HALF]); v_par,_=capa(anchors=[HALF],sup_days=[2]);
v_sus,_=capa(anchors=[HALF],sup_days=[4]); v_des,_=capa(anchors=[HALF],sup_days=[0])
v1,_=capa(anchors=[HALF],sup_days=[4]); v5,_=capa(anchors=[HALF],sup_days=[4,4,4,4,4])
print(f"(c) base media: sin={v_no:.4f} descuid={v_des:.4f} par(2d)={v_par:.4f} sostiene={v_sus:.4f}")
print(f"    1 soporte={v1:.4f} vs 5 soportes={v5:.4f} igual={abs(v1-v5)<1e-9}")
# castigo en capa perfecta
vp_ok,_=capa(anchors=[J]); vp_des,_=capa(anchors=[J],sup_days=[0])
print(f"    capa PERFECTA: sin soporte={vp_ok:.4f} soporte descuidado={vp_des:.4f} (castigo={vp_ok-vp_des:.4f})")
# (d) capa solo-soportes pesa menos
_,m_anc=capa(anchors=[J]); _,m_sop=capa(sup_days=[4])
s_with_anc=score([{'anchors':[J]},{'anchors':[J]},{'anchors':[DEF]}])
s_with_sop=score([{'anchors':[J]},{'anchors':[J]},{'sup_days':[0]}])
print(f"(d) masa con-ancla={m_anc:.3f} solo-soportes={m_sop:.3f} menor={m_sop<m_anc}")
print(f"    3a capa mala-ancla arrastra a {s_with_anc:.4f} | 3a capa mala-solo-sop a {s_with_sop:.4f} (arrastra menos={s_with_sop>s_with_anc})")
# (e) task respeta techo 0.5 CONJUNTO + satura + reset
print("(e) task dentro de la curva (techo conjunto 0.5):")
for n in [0,1,3,10,100]:
    v,_=capa(anchors=[XL],n_tasks=n); extra=v-min(XL,1)
    print(f"    XL + {n:3d} tasks -> extra={extra:.4f} (<=0.5: {extra<=0.5+1e-9})")
vh,_=capa(anchors=[J],n_tasks=3); vt,_=capa(anchors=[J],n_tasks=0)
print(f"    reset diario: hoy(3t)={vh:.4f} -> mañana(0t)={vt:.4f}")
# (f) araña cruce / no compra Inquebrantable
near=[{'anchors':[R(4,30,[40]*4)]},{'anchors':[R(4,30,[40]*4)]},{'anchors':[R(4,30,[40]*4)]}]
s_near=score(near); near_t=[dict(c,n_tasks=5) for c in near]; s_near_t=score(near_t)
print(f"(f) cerca de I: sin task={s_near:.4f} {band(s_near)} -> +5t/capa={s_near_t:.4f} {band(s_near_t)}")
alljust=[{'anchors':[J],'n_tasks':100}]*3; s_abuse=score(alljust)
print(f"    cumplir-justo + 100 tasks en TODAS = {s_abuse:.4f} {band(s_abuse)} compra_Inq={band(s_abuse)=='INQUEBRANTABLE'}")
# borde EM->P
edge=[{'anchors':[R(4,30,[24]*4)]}]*3; s_e=score(edge); s_et=score([dict(c,n_tasks=5) for c in edge])
print(f"    borde: sin task={s_e:.4f} {band(s_e)} -> +tasks={s_et:.4f} {band(s_et)} cruce={band(s_e)!=band(s_et)}")
# (g) anti-gate
mx_s=0;prev=None
for i in range(401):
    g=i/400.0; v,_=capa(anchors=[HALF],sup_days=[g*4]);
    if prev is not None: mx_s=max(mx_s,abs(v-prev))
    prev=v
mx_t=0;prev=None
for i in range(401):
    n=i*0.25; v,_=capa(anchors=[J],n_tasks=n)
    if prev is not None: mx_t=max(mx_t,abs(v-prev))
    prev=v
print(f"(g) anti-gate: soporte paso máx={mx_s:.6f} | task paso máx={mx_t:.6f}")
# (h) orden
dA=score([{'anchors':[J]}]*3)-score([{'anchors':[HALF]}]+[{'anchors':[J]}]*2)
dS=capa(anchors=[HALF],sup_days=[4])[0]-capa(anchors=[HALF],sup_days=[0])[0]
dT=capa(anchors=[J],n_tasks=100)[0]-capa(anchors=[J],n_tasks=0)[0]
print(f"(h) impacto: ANCLA={dA:.4f} > SOPORTE(swing)={dS:.4f} > TASK(100)={dT:.4f} orden={dA>dS>dT}")
