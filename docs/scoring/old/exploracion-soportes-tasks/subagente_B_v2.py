"""
SUBAGENTE B v2 — "blends convexos por capa".
Reusa el motor v4 VERBATIM (R y la curva de superhabit no se tocan).
Agrega:
  - SOPORTE: blend convexo en la base de la capa (gamma_s fijo, no crece con la cantidad).
  - TASK: saturacion CONJUNTA reparametrizada del extra (comparte la exp del superhabit),
          gate base^2, efimero diario, techo por capa, anti-abuso por saturacion de conteo.
  - PESO DE CAPA: blend por densidad de anclas (solo-soportes pesa menos, continuo).
Reproducible: python3 subagente_B_v2.py
"""
import math

# ----------------------- MOTOR v4 VERBATIM -----------------------
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
def beta_from_target(target): return 1.0/target - 1.0
TARGET=0.55; BETA=beta_from_target(TARGET)

# ----------------------- PARAMETROS DESPEJADOS (B) -----------------------
SMAX=0.5; S0=0.5                 # de la curva v4 (no se toca)
GAMMA_S=0.07                     # blend de soporte: leve a nivel capa = drop por neglect total
TAU=0.05                         # techo de task por capa: despejado de (f-strict) = a lo sumo medio gap P->I (0.10)
THETA=-S0*math.log(1-TAU/SMAX)   # presupuesto pre-saturacion de task -> lift maximo = TAU
N0_TASK=1.0                      # saturacion de conteo: 1 task = 63% del techo
ETA=1.0/3.0                      # peso de capa solo-soportes = ETA*w0
D0=1.0                           # densidad de anclas: dens = 1-exp(-na/D0)

# ----------------------- SENALES -----------------------
def support_block_signal(supports):
    """G_soporte in [0,1]. supports = lista de dias_cumplidos por soporte (ventana indulgente /4).
    El bloque NO crece con la cantidad: se promedian los s_i (1 o 5 soportes pesan igual)."""
    if not supports: return None
    sigs=[min(d/4.0,1.0) for d in supports]
    return sum(sigs)/len(sigs)

def task_count_signal(n_tasks):
    """g_task in [0,1], saturado por conteo. Efimero: n_tasks son las de HOY."""
    if n_tasks<=0: return 0.0
    return 1-math.exp(-n_tasks/N0_TASK)

def anchor_density(na):
    return 1-math.exp(-na/D0)

# ----------------------- VALOR DE CAPA (B) -----------------------
def layer_value(anchors, supports=None, n_tasks=0):
    """Devuelve (base_term, extra, w_capa) para el agregado global.
    anchors: lista de R por ancla (>=1 si superhabit). supports: dias por soporte. n_tasks: HOY."""
    na=len(anchors or [])
    if anchors:
        base_anclas=sum(min(r,1) for r in anchors)/na
        extra_anclas=sum(max(r-1,0) for r in anchors)/na
    else:
        base_anclas=0.0; extra_anclas=0.0

    # --- SOPORTE: blend convexo en la base ---
    G=support_block_signal(supports)
    if G is not None and anchors:
        base_eff=(1-GAMMA_S)*base_anclas+GAMMA_S*G        # pull leve hacia G, bidireccional
    elif G is not None and not anchors:
        base_eff=G                                         # capa solo-soportes: la senal ES la base
    else:
        base_eff=base_anclas

    # --- TASK: saturacion CONJUNTA reparametrizada del extra ---
    # surplus_anclas pre-saturacion (invertimos la exp del v4 a nivel capa):
    su_anclas=-S0*math.log(1-min(extra_anclas,SMAX-1e-9)/SMAX) if extra_anclas>0 else 0.0
    g_task=task_count_signal(n_tasks)
    su_total=su_anclas+THETA*g_task
    extra_joint=SMAX*(1-math.exp(-su_total/S0))            # comparte la MISMA curva, techo 0.5
    # gate base^2 sobre el aporte de task (no fabrica gloria sin cimiento):
    task_lift=(extra_joint-extra_anclas)*(min(base_eff,1.0)**2)
    extra=extra_anclas+task_lift

    # --- PESO DE CAPA por densidad de anclas (B8) ---
    dens=anchor_density(na)
    w_capa=W0*(ETA+(1-ETA)*dens)                           # solo-soportes (na=0) -> ETA*w0
    base_term=min(base_eff,1.0)
    return base_term, extra, w_capa

def score(capas):
    """capas: lista de dicts {anchors, supports, n_tasks}. Pesos de capa por densidad."""
    bt=[];ex=[];w=[]
    for L in capas:
        b,e,wc=layer_value(L.get('anchors') or [], L.get('supports'), L.get('n_tasks',0))
        bt.append(b);ex.append(e);w.append(wc)
    base=sum(b*wi for b,wi in zip(bt,w))/sum(w)
    extra=sum(e*wi for e,wi in zip(ex,w))/sum(w)           # extra ponderado por la misma masa
    return base+extra

# ----------------------- VALORES ANCLA DE REFERENCIA -----------------------
J=R(4,30,[30]*4); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30])
print(f"B v2. GAMMA_S={GAMMA_S} TAU={TAU} THETA={THETA:.4f} N0={N0_TASK} ETA={ETA:.3f} D0={D0}")
print(f"J(justo)={J:.3f} XL={XL:.3f} DEF(3d)={DEF:.3f}")
print("="*88)

# (a) Sol=Tin intacto (sin soportes ni tasks => identico a v4 modulo pesos de capa iguales)
sSol=score([{'anchors':[XL]},{'anchors':[J]},{'anchors':[J]}])
sTin=score([{'anchors':[J]},{'anchors':[XL]},{'anchors':[J]}])
print(f"(a) Sol={sSol:.4f} Tin={sTin:.4f} empatan={abs(sSol-sTin)<1e-9}")

# (b) cumplir-justo = 1.0 = PLENITUD (sin soportes ni tasks)
sJ=score([{'anchors':[J]},{'anchors':[J]},{'anchors':[J]}])
print(f"(b) cumplir-justo={sJ:.4f} banda={band(sJ)} ok={abs(sJ-1.0)<1e-9}")

# (c) soporte bidireccional leve + NO crece con la cantidad
neutral=score([{'anchors':[J],'supports':[4]},{'anchors':[J]},{'anchors':[J]}])  # G=1=ba -> neutro
em_anchors=[R(4,30,[30,30,30])]  # base<1 (3/4)
lift1=score([{'anchors':em_anchors,'supports':[4]},{'anchors':[J]},{'anchors':[J]}])
lift_more=score([{'anchors':em_anchors,'supports':[4,4,4,4,4]},{'anchors':[J]},{'anchors':[J]}])
base_noS=score([{'anchors':em_anchors},{'anchors':[J]},{'anchors':[J]}])
drop=score([{'anchors':[J],'supports':[0]},{'anchors':[J]},{'anchors':[J]}])
print(f"(c) neutro(G=ba)={neutral:.4f} (=1.0) | sube si sostengo>{base_noS:.4f}->{lift1:.4f}"
      f" | baja por neglect={drop:.4f}")
print(f"    NO crece con cantidad: 1soporte={lift1:.4f}  5soportes={lift_more:.4f} iguales={abs(lift1-lift_more)<1e-9}")

# (d) capa solo-soportes pesa MENOS
con_anchor=score([{'anchors':[J]},{'anchors':[DEF]},{'anchors':[J]}])
solo_sup =score([{'anchors':[J]},{'supports':[4]},{'anchors':[J]}])  # capa 2 = solo soporte perfecto
solo_sup_w=layer_value([], [4])[2]; anchor_w=layer_value([J])[2]
print(f"(d) peso solo-soportes={solo_sup_w:.3f} vs anchor-layer={anchor_w:.3f} ratio={solo_sup_w/anchor_w:.3f}")
# una capa solo-soportes perfecta impacta MENOS que una capa-ancla en plenitud:
impact_solo=score([{'anchors':[DEF]},{'supports':[4]}]) - DEF
print(f"    capa solo-sop perfecta junto a DEF: score={score([{'anchors':[DEF]},{'supports':[4]}]):.4f}"
      f" (anclas pesan mas)")

# (e) tasks dentro de la curva: no rompen techo 0.5, saturan, reset diario
just_layer=[{'anchors':[J]}]
def extra_of(capas): return score(capas)-score([{k:(v if k!='n_tasks' else 0) for k,v in L.items()} for L in capas])
e1=layer_value([J],None,1)[1]; e3=layer_value([J],None,3)[1]; e100=layer_value([J],None,100)[1]
print(f"(e) task extra a nivel capa: 1={e1:.4f} 3={e3:.4f} 100={e100:.4f} techo<=0.5={e100<=0.5}")
# joint con anclas en superhabit: el lift de task se ENCOGE (saturacion compartida)
lift_lowsurplus=layer_value([J],None,3)[1]-0.0
lift_highsurplus=layer_value([XL],None,3)[1]-(XL-1)
print(f"    joint sat: lift task con extra_anclas=0 -> {lift_lowsurplus:.4f};"
      f" con anclas XL(superhabit alto) -> {lift_highsurplus:.4f} (menor)")
print(f"    reset diario: HOY n_tasks=3 score={score([{'anchors':[J],'n_tasks':3}]+[{'anchors':[J]}]*2):.4f}"
      f"  MANANA n_tasks=0 score={score([{'anchors':[J]}]*3):.4f}")

# (f) task arana UN cruce cuando estas cerca, pero cumplir-justo+task NO compra Inquebrantable solo
# perfil cerca del borde P->I (necesita 1.10). Anclas justo dan 1.0 exacto.
near=[{'anchors':[R(4,30,[30,30,30,40])]}]*3  # leve superhabit
near_no=score(near); near_task=score([{'anchors':[R(4,30,[30,30,30,40])],'n_tasks':3}]+near[1:])
print(f"(f) cerca del borde: sin task={near_no:.4f} {band(near_no)} -> con task={near_task:.4f} {band(near_task)}")
plen_task=score([{'anchors':[J],'n_tasks':100}]*3)
print(f"    cumplir-justo + 100 tasks en TODAS las capas = {plen_task:.4f} {band(plen_task)}"
      f" (NO Inquebrantable solo por tasks: {band(plen_task)!='INQUEBRANTABLE'})")

# (g) anti-gate: barrido continuo de soporte y de tasks
prev=None;mx=0
for i in range(401):
    d=i/100.0  # dias 0..4
    s=score([{'anchors':[R(4,30,[30,30,30])],'supports':[d]},{'anchors':[J]},{'anchors':[J]}])
    if prev is not None: mx=max(mx,abs(s-prev))
    prev=s
prevt=None;mxt=0
for i in range(2001):
    n=i/20.0
    s=score([{'anchors':[J],'n_tasks':n},{'anchors':[J]},{'anchors':[J]}])
    if prevt is not None: mxt=max(mxt,abs(s-prevt))
    prevt=s
print(f"(g) continuo: paso max soporte={mx:.5f}  paso max task={mxt:.5f} (sin gate duro)")

# (h) ANCLAS > SOPORTES > TASKS  (impacto MAXIMO de cada mecanismo, donde mas importa)
# ANCLA: J->XL en una capa (mueve base Y extra). SOPORTE: maximo pull sobre una base debil
# (donde el estado es fragil y el cimiento es lo que define la banda). TASK: techo total.
weak=[R(4,30,[10,10,10,10])]  # base baja ~0.33 (capa fragil: ahi el soporte vale)
base_weak=score([{'anchors':weak},{'anchors':[J]},{'anchors':[J]}])
d_anchor=score([{'anchors':[XL]},{'anchors':[J]},{'anchors':[J]}])-sJ
d_support=score([{'anchors':weak,'supports':[4]},{'anchors':[J]},{'anchors':[J]}])-base_weak
d_task=score([{'anchors':[J],'n_tasks':100}]+[{'anchors':[J]}]*2)-sJ
print(f"(h) impacto MAXIMO: ANCLA(J->XL)={d_anchor:.4f} > SOPORTE(full sobre base debil)={d_support:.4f}"
      f" > TASK(100)={d_task:.4f}  orden={d_anchor>d_support>d_task}")
