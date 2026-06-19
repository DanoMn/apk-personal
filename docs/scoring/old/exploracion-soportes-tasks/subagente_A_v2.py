"""
SUBAGENTE A v2 — "surplus virtual / reusar las fórmulas cerradas".
Soporte y Task como ENTRADAS VIRTUALES que pasan por las fórmulas de v4.
Reusa el motor v4 verbatim (R, band, curva de saturación, gate base²).
Reproducible: python3 subagente_A_v2.py
"""
import math

# ================= MOTOR v4 VERBATIM =================
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

DELTA=0.10; W0=1.0
def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")

# ================= PARÁMETROS DESPEJADOS DE AXIOMAS =================
SMAX=0.5; S0=0.5            # curva superhabit v4 (NO se toca)

# --- SOPORTE: nudge aditivo bidireccional, neutral a la par de las anclas ---
# nudge = EPS_S * (G_s - base_anc). EPS_S despejado del axioma S-mag (ver doc).
EPS_S=0.10                  # swing máx +/-0.05 a base_anc=0.5 (leve, ANCLAS>>SOPORTES)

# --- SOPORTE: ventana indulgente de WIN días por soporte ---
WIN=4

# --- TASK: surplus virtual que comparte la FORMA de la curva v4 ---
# Cada task suma surplus crudo (1 task = 1 unidad) que pasa por la MISMA saturación
# exponencial de v4 y el MISMO gate base², pero con techo propio TASK_CAP (<< smax=0.5).
# TASK_CAP despejado del axioma T-techo (ver doc): cumplir-justo + tasks ∞ < 1.10.
TASK_CAP=0.09              # techo de extra por task/capa (DERIVADO de la banda Inquebrantable)
S0_TASK=3/math.log(10)     # ~3 tasks => 90% del techo (1ª task vale mucho, 10ª casi nada)

# --- PESO DE CAPA reducido para capa solo-soportes (continuo) ---
# w_capa = RHO + (1-RHO) * dens_anclas ; dens=anclas presentes/esperadas (cap 1)
RHO=0.4                    # piso de peso para capa sin anclas (despejado, ver doc)

# ================= ENTRADAS VIRTUALES =================
def support_block_signal(supports_days):
    """supports_days: lista de #días cumplidos por soporte. Señal de bloque = promedio
    de s_i = min(dias/WIN,1). NO crece con la cantidad (promedio, no suma)."""
    if not supports_days: return None
    sig=[min(d/WIN,1.0) for d in supports_days]
    return sum(sig)/len(sig)

def layer_value(anchors, supports_days=None, tasks_today=0):
    """Valor de capa con soporte (en base) y task (en extra), via fórmulas v4."""
    if anchors:
        base_anc=sum(min(r,1) for r in anchors)/len(anchors)
        surplus_anc=sum(max(r-1,0) for r in anchors)/len(anchors)
    else:
        base_anc=0.0; surplus_anc=0.0
    # ---- SOPORTE: entra a la base como componente virtual (bidireccional leve) ----
    G_s=support_block_signal(supports_days)
    if G_s is not None and anchors:
        base_eff=min(max(base_anc+EPS_S*(G_s-base_anc),0.0),1.0)
    elif G_s is not None and not anchors:
        base_eff=G_s                      # capa solo-soportes: la señal ES la base
    else:
        base_eff=base_anc
    # ---- EXTRA de anclas: curva v4 verbatim (techo 0.5, gate base²) ----
    S_anc=SMAX*(1-math.exp(-surplus_anc/S0))
    extra_anc=(base_eff**2)*S_anc
    # ---- TASK: surplus virtual, MISMA forma de saturación + MISMO gate, techo propio ----
    S_task=TASK_CAP*(1-math.exp(-tasks_today/S0_TASK))  # saturación, 1ª task vale mucho
    extra_task=(base_eff**2)*S_task                      # gate base² (sin cimiento, castrado)
    return min(base_eff,1.0)+extra_anc+extra_task

def layer_weight(anchors, n_expected=1):
    """Peso de capa: reducido si no hay anclas (continuo)."""
    dens=min(len(anchors)/max(n_expected,1),1.0) if anchors else 0.0
    return RHO+(1-RHO)*dens

def score(capas):
    """capas: lista de dict {anchors, supports, tasks, n_expected}. Promedio ponderado."""
    vals=[]; ws=[]
    for L in capas:
        a=L.get('anchors') or []
        sd=L.get('supports'); tt=L.get('tasks',0); ne=L.get('n_expected',max(len(a),1))
        vals.append(layer_value(a,sd,tt)); ws.append(layer_weight(a,ne))
    return sum(v*w for v,w in zip(vals,ws))/sum(ws) if ws else 0.0

# ================= VERIFICACIÓN =================
J=R(4,30,[30]*4); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30]); half=R(4,30,[15,15,15,15])
print(f"Anclas ref: J(justo)={J:.4f} XL={XL:.4f} DEF={DEF:.4f} half(base~0.5)={min(half,1):.4f}")
print("="*70)

# (a) Sol=Tin intacto (sin soportes ni tasks; layer_value == merge v4 sin opt-in)
sSol=score([{'anchors':[XL]},{'anchors':[J]},{'anchors':[J]}])
sTin=score([{'anchors':[J]},{'anchors':[XL]},{'anchors':[J]}])
print(f"(a) Sol={sSol:.6f} Tin={sTin:.6f} empatan={abs(sSol-sTin)<1e-9}")

# (b) cumplir-justo = 1.0 = PLENITUD
s_just=score([{'anchors':[J]},{'anchors':[J]},{'anchors':[J]}])
print(f"(b) cumplir-justo={s_just:.4f} band={band(s_just)}")

# (c) soporte bidireccional leve y NO crece con la cantidad
print("(c) SOPORTE bidireccional + invariante a cantidad (ancla media base~0.5):")
for label,sd in [("sin soporte",None),("descuidado(0d)",[0]),("a la par(2d)",[2]),
                 ("sostenido(4d)",[4])]:
    print(f"    {label:18s} valor_capa={layer_value([half],sd):.4f}")
v1=layer_value([half],[4]); v5=layer_value([half],[4,4,4,4,4])
print(f"    1 sostenido={v1:.4f} vs 5 sostenidos={v5:.4f} igual={abs(v1-v5)<1e-9}")
vm1=layer_value([half],[0]); vm5=layer_value([half],[0,0,0,0,0])
print(f"    1 descuidado={vm1:.4f} vs 5 descuidados={vm5:.4f} igual={abs(vm1-vm5)<1e-9}")

# (d) capa solo-soportes pesa menos
print("(d) PESO de capa solo-soportes < capa con anclas:")
wa=layer_weight([J]); wso=layer_weight([])
print(f"    w(con ancla)={wa:.3f} w(solo-soportes)={wso:.3f} menor={wso<wa}")
s_anc=score([{'anchors':[J]},{'anchors':[J]},{'anchors':[XL]}])
s_sup=score([{'anchors':[J]},{'anchors':[J]},{'anchors':[],'supports':[4]}])
print(f"    score 3a-capa con XL={s_anc:.4f} | 3a-capa solo-soportes(plena)={s_sup:.4f}")

# (e) tasks dentro de la curva: techo TASK_CAP, 100 tasks saturan, gate base², reset diario
print(f"(e) TASK dentro de la curva (capa base=1=justo; techo task/capa={TASK_CAP}):")
for nt in [0,1,2,5,10,100]:
    extra=layer_value([J],tasks_today=nt)-1.0
    print(f"    {nt:3d} tasks -> extra_task={extra:.4f} techo_ok={extra<=TASK_CAP+1e-9} (<<0.5={extra<=0.5})")
print(f"    reset diario: hoy(5t)={layer_value([J],tasks_today=5):.4f} -> mañana(0t)={layer_value([J],tasks_today=0):.4f}")
print(f"    gate base²: capa base baja + 5 tasks = {layer_value([R(4,30,[15,15])],tasks_today=5):.4f} (extra castrado)")

# (f) tasks arañan UN cruce cuando estás cerca, pero no fabrican estado de la nada
print("(f) TASK araña cruce cerca / no compra Inquebrantable sola:")
near=R(4,30,[28,28,28,28])  # base alta <1
s_near=score([{'anchors':[near]},{'anchors':[J]},{'anchors':[J]}])
s_near_t=score([{'anchors':[near],'tasks':3},{'anchors':[J]},{'anchors':[J]}])
print(f"    cerca: sin task={s_near:.4f}({band(s_near)}) con 3t={s_near_t:.4f}({band(s_near_t)})")
s_just_t=score([{'anchors':[J],'tasks':10},{'anchors':[J],'tasks':10},{'anchors':[J],'tasks':10}])
print(f"    cumplir-justo + 10t/capa = {s_just_t:.4f}({band(s_just_t)}) compra_Inq={s_just_t>=1.10}")
# en un borde real EN MARCHA->PLENITUD, las tasks arañan el cruce:
edge=R(4,30,[26,26,26,21])
e0=score([{'anchors':[edge]},{'anchors':[J]},{'anchors':[R(4,30,[25,25,25,25])]}])
et=score([{'anchors':[edge],'tasks':3},{'anchors':[J]},{'anchors':[R(4,30,[25,25,25,25])],'tasks':3}])
print(f"    borde: sin task={e0:.4f}({band(e0)}) con tasks={et:.4f}({band(et)}) CRUCE={band(e0)!=band(et)}")

# (g) anti-gate: continuidad
print("(g) anti-gate (continuidad):")
prev=None;mx=0
for i in range(2001):
    nt=i/200.0
    v=layer_value([J],tasks_today=nt)
    if prev is not None: mx=max(mx,abs(v-prev))
    prev=v
print(f"    paso máx |dvalor| dtask=0.005 = {mx:.6f}")
prev=None;mx2=0
for i in range(1001):
    Gs=i/1000.0
    v=min(max(min(half,1)+EPS_S*(Gs-min(half,1)),0),1)
    if prev is not None: mx2=max(mx2,abs(v-prev))
    prev=v
print(f"    paso máx |dbase| dG_s=0.001 = {mx2:.6f}")

# (h) ANCLAS > SOPORTES > TASKS (impacto sobre el estado, misma capa media)
print("(h) ANCLAS > SOPORTES > TASKS:")
b=score([{'anchors':[half]},{'anchors':[J]},{'anchors':[J]}])
imp_anc=score([{'anchors':[J]},{'anchors':[J]},{'anchors':[J]}])-b
imp_sup=score([{'anchors':[half],'supports':[4]},{'anchors':[J]},{'anchors':[J]}])-b
imp_tsk=score([{'anchors':[half],'tasks':1},{'anchors':[J]},{'anchors':[J]}])-b
print(f"    Δ ANCLA(media->justo)={imp_anc:.4f} | Δ SOPORTE pleno={imp_sup:.4f} | Δ 1 TASK={imp_tsk:.4f}")
print(f"    orden ANCLA>SOPORTE>TASK = {imp_anc>imp_sup>imp_tsk}")
