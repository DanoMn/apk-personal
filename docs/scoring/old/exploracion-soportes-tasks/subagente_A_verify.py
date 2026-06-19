"""
Subagente A — verificacion del modelo de SOPORTES (canal base, light/centrado/saturado)
y TASKS (solo eje visible, nunca el motor). python3 /tmp/subA_verify.py
Reusa el motor v4 cerrado (no se toca).
"""
import math

# ====== MOTOR v4 CERRADO (copiado verbatim de modelo_valor_capa_v4_merge.py) ======
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
def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")
def beta_from_target(target): return 1.0/target - 1.0
TARGET=0.55; BETA=beta_from_target(TARGET)

# ====== SUBAGENTE A : SOPORTES ======
# Soporte = senal de mantenimiento diario s in [0,1] por soporte (fraccion de dias sostenidos
# en la ventana de 7). UX inversa: marcar lo NO hecho => internamente s = 1 - omisiones/7.
# Entra como TERMINO-SOMBRA BIDIRECCIONAL CENTRADO en el canal base de SU capa, peso dinamico
# pequeno: w_sup = ETA * (1 - g) ... NO. Lo hacemos centrado: la senal agregada de la capa
# G in [0,1] (1 = todos los soportes sostenidos). El neutro es el punto de "cumplir-justo"
# del soporte. Para que "sostener suma poco / descuidar resta poquisimo" y que cumplir-justo
# (G=Gref) sea EXACTAMENTE neutro, centramos en un ancla Gref y damos peso dinamico chico.
#
# AXIOMA-DERIVADO de magnitudes:
#  - Ancla recaida-total baja a 0.55 (BETA grande). Soporte total-descuidado debe bajar MUY
#    poco: definimos TARGET_SUP = piso al que cae una capa de anclas perfectas si TODOS sus
#    soportes estan totalmente descuidados (G=0), N=3. El dueno: "descuidar resta poquisimo".
#    Elegimos que el peor descuido total cueste a lo sumo ~0.03 de estado (anclas perfectas).
#  - Sostener perfecto (G=1) por encima del neutro NO debe fabricar banda: aporte saturado y
#    centrado => por arriba del neutro suma AUN MENOS que lo que resta por abajo (asimetria
#    suave a favor de no-premiar; soporte sostiene, no luce).

# Senal del soporte: como termino-sombra CENTRADO. Definimos el neutro en G_REF.
# El dueno: cumplir-justo = ESTADO 1.0. "Cumplir-justo" de soporte = sostenerlos como se espera.
# Tomamos G_REF = 1.0 (sostener todos = lo esperado = neutro EXACTO). Asi:
#   - G = 1 (sostiene todo)  -> aporte 0 (neutro, no premia)
#   - G < 1 (descuida)       -> resta MUY poco (saturado)
# Esto respeta O-style: soporte sostenido = invisible (como opt-in bien), descuidado = leve baja.
# => El soporte es estructuralmente un OPT-IN ATENUADO con G_REF=1 y ETA << BETA.

# Peso dinamico del soporte (analogo a w=BETA*N*(1-M) del opt-in, pero ETA<<BETA y NO plano:
# lo dejamos local a la capa -> NO escala con N, para que reste aun menos y respete
# anclas>>soportes). w_sup = ETA * (1 - G).
# ETA se DESPEJA del axioma "descuido total cuesta <= DROP_MAX de estado con anclas perfectas".

def estado_con_soportes(capas, eta):
    """capas: lista de dicts {'anchors':[r..], 'optin':M|None, 'support':G|None}.
    Soporte: termino-sombra centrado en G_REF=1, peso w=eta*(1-G), valor de la sombra = G."""
    act=[L for L in capas if (L.get('anchors') or L.get('optin') is not None or L.get('support') is not None)]
    N=len(act); terms=[]; extras=[]
    for L in act:
        a=L.get('anchors') or []; M=L.get('optin'); G=L.get('support')
        if a:
            ab=sum(min(r,1) for r in a)/len(a); terms.append((ab,W0))
            extras.append(sum(max(r-1,0) for r in a)/len(a))
            if M is not None:
                w=BETA*N*(1-M)
                if w>1e-12: terms.append((M,w))
            if G is not None:
                w=eta*(1-G)            # local, NO escala con N
                if w>1e-12: terms.append((G,w))
        elif M is not None:
            terms.append((M,W0))
        elif G is not None:
            # capa solo-soporte: sin anclas el soporte ES un piso suave -> vale G con peso W0?
            # NO: el dueno dice soportes no compiten con anclas y no son obligatorios.
            # Una capa solo-soporte vale su G (peso normal) como limite natural del blend.
            terms.append((G,W0))
    base=sum(v*w for v,w in terms)/sum(w for _,w in terms) if terms else 0.0
    extra=sum(extras)/len(extras) if extras else 0.0
    return min(base,1.0)+extra

# ---- Despeje de ETA desde axioma de estado ----
# Anclas perfectas N=3 => sin soporte estado=1.0. Con UN soporte totalmente descuidado (G=0)
# en una capa: base = (1*1 + 1*1 + 1*1 + 0*eta)/(3+eta) = 3/(3+eta).
# DROP = 1 - 3/(3+eta). Queremos DROP_MAX <= 0.03 (descuidar resta poquisimo).
J=R(4,30,[30]*4)        # ancla cumplir-justo = 1.0
DROP_MAX=0.03
# 1 - 3/(3+eta) = DROP_MAX  ->  eta = 3*DROP_MAX/(1-DROP_MAX)
N_ref=3
eta_solved = N_ref*DROP_MAX/(1-DROP_MAX)
ETA=eta_solved
print(f"[DESPEJE] ETA desde DROP_MAX={DROP_MAX} (descuido total 1 soporte, anclas perfectas N=3) -> ETA={ETA:.4f}")
print(f"   (compara: BETA opt-in={BETA:.3f}.  ETA/BETA={ETA/BETA:.3f}  -> soporte pega ~{ETA/BETA*100:.0f}% de un opt-in)")

# ====== SUBAGENTE A : TASKS (solo eje visible) ======
# Tasks NO entran a estado/banda. Solo empujan el numero visible 700-1000, acotado, saturado,
# y SOLO si el usuario ya esta alto (ayuda mental sin injusticia: no se compra una banda).
# Eje visible reconciliado bajo v4: el visible refleja la BASE recortada del ESTADO.
#   base_vis = clamp(min(base_global,1), 0, 1)   (el extra/superhabit no entra al visible, igual que hoy)
# VisibleScore = 700 + round( clamp(base_vis + task_push, 0,1) * 300 )
# task_push = TASK_MAX * base_vis^q * (1 - exp(-K * n_eff))
#   - base_vis^q (q>1): gate SUAVE de merito -> en base baja, el push es ~0 (no rescata a nadie).
#   - (1-exp(-K*n)): saturacion multi-task -> coleccionar tasks no fabrica un salto.
#   - clamp a 1 antes de *300: nunca pasa de 1000.
TASK_MAX=0.04   # despeje abajo
Q_TASK=3.0
K_TASK=0.55
def n_eff_tasks(tasks):
    # solo tasks con capa != None y rol != Neutral suman; cada una vale 1 unidad
    return sum(1 for t in tasks if t.get('layer') is not None and t.get('role')!='Neutral')
def visible(base_global, tasks=()):
    base_vis=min(max(base_global,0.0),1.0)
    n=n_eff_tasks(tasks)
    push=TASK_MAX*(base_vis**Q_TASK)*(1-math.exp(-K_TASK*n))
    return 700+round(min(base_vis+push,1.0)*300)
# Despeje TASK_MAX: "ayuda mental = arañar unos puntos para cruzar un umbral en el tramo final".
# Un umbral de banda en el visible: P empieza en base=0.85 -> visible 700+0.85*300=955.
# Queremos que ~3 tasks a base alta (0.84) puedan empujar a alguien borde-Plenitud sin regalar.
# TASK_MAX=0.04 => a base=0.85, sat con n grande -> push<=0.04*0.85^3=0.0246 -> ~7 pts visibles.
print(f"[TASKS] TASK_MAX={TASK_MAX} Q={Q_TASK} K={K_TASK}. push acotado y meritocratico (base_vis^Q).")

print("="*94)
def estado(capas): return estado_con_soportes(capas, ETA)

# ---------- VERIFICACION ----------
SUP=R(4,30,[30]*6); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30])

print("\n(a) SUPERHABIT INTACTO  Sol == Tin  (soportes/tasks NO distorsionan extra)")
Sol=estado([{'anchors':[XL]},{'anchors':[J],'optin':1.0,'support':0.6},{'anchors':[J],'support':0.3}])
Tin=estado([{'anchors':[J],'support':0.3}],)  # placeholder, real abajo
Sol2=estado([{'anchors':[XL],'support':0.6},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
Tin2=estado([{'anchors':[J],'support':0.6},{'anchors':[XL],'optin':1.0},{'anchors':[J]}])
print(f"   Sol(superhabit Interior)={Sol2:.4f}  Tin(superhabit Cuerpo)={Tin2:.4f}  empatan={abs(Sol2-Tin2)<1e-9}")
# Tambien el clasico sin soportes debe seguir intacto:
Sol0=estado([{'anchors':[XL]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
Tin0=estado([{'anchors':[J]},{'anchors':[XL],'optin':1.0},{'anchors':[J]}])
print(f"   (sin soportes) Sol={Sol0:.4f} Tin={Tin0:.4f} empatan={abs(Sol0-Tin0)<1e-9} (== 1.1441 v4)")

print("\n(b) CUMPLIR-JUSTO = ESTADO 1.0 = PLENITUD (anclas justas + soportes sostenidos G=1)")
cj=estado([{'anchors':[J],'support':1.0},{'anchors':[J],'support':1.0},{'anchors':[J],'support':1.0}])
print(f"   estado={cj:.4f}  banda={band(cj)}  ok={abs(cj-1.0)<1e-9}")
cj_noS=estado([{'anchors':[J]},{'anchors':[J]},{'anchors':[J]}])
print(f"   (anclas justas SIN soportes) estado={cj_noS:.4f} == cumplir-justo ({abs(cj-cj_noS)<1e-9})")

print("\n(c) MULTI-SOPORTE NO FABRICA BANDA (sostener no sube de banda)")
# capa con anclas en EN MARCHA-ish + muchos soportes perfectos no debe saltar a Plenitud
parc=R(4,30,[18,18,18,18])  # ancla parcial
base_parc=estado([{'anchors':[parc]},{'anchors':[parc]},{'anchors':[parc]}])
base_parc_S=estado([{'anchors':[parc],'support':1.0},{'anchors':[parc],'support':1.0},{'anchors':[parc],'support':1.0}])
print(f"   anclas parciales sin soportes={base_parc:.4f} {band(base_parc)} | +soportes perfectos={base_parc_S:.4f} {band(base_parc_S)}")
print(f"   misma banda={band(base_parc)==band(base_parc_S)} (soporte sostenido NO sube banda: G_REF=1 neutro)")

print("\n(d) ANTI-GATE: barrido continuo de G (soporte), N=3, anclas justas")
prev=None;mx=0
for i in range(1001):
    G=i/1000; s=estado([{'anchors':[J],'support':G}]+[{'anchors':[J]}]*2)
    if prev is not None: mx=max(mx,abs(s-prev))
    prev=s
print(f"   paso maximo |dEstado| con dG=0.001 = {mx:.6f}  -> continuo, sin gate/cap")

print("\n(e) ORDEN ANCLAS > SOPORTES > TASKS (impacto maximo en estado/visible)")
# impacto ancla: recaida-equiv no aplica a ancla; usamos: bajar 1 ancla de capa a 0
imp_anchor = 1.0 - estado([{'anchors':[0.0]},{'anchors':[J]},{'anchors':[J]}])  # 1 capa de ancla colapsada
imp_optin  = 1.0 - estado([{'anchors':[J],'optin':0.0}]+[{'anchors':[J]}]*2)    # recaida total
imp_support= 1.0 - estado([{'anchors':[J],'support':0.0}]+[{'anchors':[J]}]*2)  # soporte totalmente descuidado
imp_task_estado = 0.0  # tasks NUNCA tocan estado
print(f"   |impacto ancla colapsada|   = {imp_anchor:.4f}")
print(f"   |impacto opt-in recaida|    = {imp_optin:.4f}")
print(f"   |impacto soporte descuidado|= {imp_support:.4f}")
print(f"   |impacto task en ESTADO|    = {imp_task_estado:.4f} (cero por diseno)")
print(f"   ancla > soporte : {imp_anchor>imp_support}")
print(f"   soporte > task(estado) : {imp_support>imp_task_estado}")
# orden en el eje VISIBLE: task push acotado < lo que mueve un soporte en visible
base_just=min(estado([{'anchors':[J]}]*3),1.0)
vis_no=visible(base_just)
vis_task=visible(base_just,[{'layer':'P','role':'Real'}]*5)
base_sup_drop=min(estado([{'anchors':[J],'support':0.0}]+[{'anchors':[J]}]*2),1.0)
vis_sup=visible(base_sup_drop)
print(f"   visible base-justa sin task={vis_no}  +5 tasks={vis_task} (push={vis_task-vis_no} pts)")
print(f"   visible con soporte descuidado={vis_sup} (mueve {vis_no-vis_sup} pts)  -> soporte mueve mas que tasks: {(vis_no-vis_sup)>= (vis_task-vis_no)}")

print("\n(f) TASKS no rescatan a usuario bajo (anti-injusticia)")
for bx,label in [(0.30,'Restauracion'),(0.55,'Atencion'),(0.70,'En marcha'),(0.84,'borde-Plenitud')]:
    v0=visible(bx); v5=visible(bx,[{'layer':'P','role':'Real'}]*5)
    print(f"   base={bx:.2f} ({label:14s}) visible {v0} -> +5 tasks {v5}  (push {v5-v0} pts)")

print("\n(g) MULTI-TASK SATURADO (coleccionar no fabrica salto), base=0.84")
for n in [0,1,2,3,5,10,50]:
    v=visible(0.84,[{'layer':'P','role':'Real'}]*n)
    print(f"   {n:2d} tasks -> visible {v} (push {v-visible(0.84)} pts)")

print("\n(h) TASK NEUTRAL no suma (layer None o role Neutral)")
vN=visible(0.84,[{'layer':None,'role':'Real'}]*5 + [{'layer':'P','role':'Neutral'}]*5)
print(f"   base=0.84 + 5 tasks neutras + 5 sin capa -> visible {vN} == base sola {visible(0.84)} : {vN==visible(0.84)}")

print("\n(i) VISIBLE jamas pasa 1000 (clamp), base extrema + muchas tasks")
print(f"   base=1.0 + 50 tasks -> visible {visible(1.0,[{'layer':'P','role':'Real'}]*50)} (<=1000)")
