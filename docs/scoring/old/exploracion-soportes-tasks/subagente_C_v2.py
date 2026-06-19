"""
SUBAGENTE C v2 — SOPORTES y TASKS en Forma A vía PRESUPUESTO / HEADROOM y MASA DE CAPA.
Reusa el motor v4 verbatim (R, band) y agrega:
  - soporte: llena el GAP de base (1-base_anclas) con factor chico, bidireccional, topado en 1 por construcción.
  - task:    llena el HEADROOM de extra (smax-extra_anclas) con saturación, efímera diaria, techo por construcción.
  - peso de capa por MASA DE SUSTANCIA: capa solo-soportes pesa menos en el promedio global.
Reproducible: python3 subagente_C_v2.py
"""
import math

# ============ MOTOR v4 VERBATIM (no se toca) ============
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
DELTA=0.10
def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")

SMAX=0.5   # techo del extra, candado del motor

# ============ PARÁMETROS DESPEJADOS DE AXIOMAS DE ESTADO ============
# --- SOPORTE (gap de base) ---
# Axioma S-cal: "una capa con anclas a medias (base=0.5) y soporte 100% sostenido NO debe
#   cruzar sola un estado: el toque sube ~+0.05 en el peor caso (gap=0.5)."
#   aporte = ETA_S * gap * (2*g-1),  con g=señal de soporte en [0,1] (bidireccional via 2g-1).
#   peor caso descuido/sostén: |2g-1|=1, gap maximo util ~0.5 -> ETA_S*0.5 = 0.05 -> ETA_S=0.10
ETA_S = 0.10
DIAS_VENTANA_SOPORTE = 4   # 4 días = 100% (ventana indulgente, no 7)

# --- TASK (headroom del extra) ---
# Axioma T-cal: "tasks de un día, con anclas en cumplir-justo (extra=0, base=1), aportan a UNA capa
#   un techo TASK_CAP tal que NO basten para comprar Inquebrantable solas: cumplir-justo da score=1.0
#   (Plenitud); aún saturando tasks en N=3 el score debe quedar < 1.10."
#   extra_task = TASK_CAP * (1 - exp(-k_t * n_tasks))   (saturación: 1ra task >> 10ma)
#   pero clamp al headroom restante: min(extra_task, SMAX - extra_anclas) -> nunca rompe techo 0.5.
# Despeje del techo por capa desde la banda Inquebrantable (1.10) con cumplir-justo (base=1, extra=0):
#   PEOR caso anti-abuso = tasks-full en TODAS las capas -> score = 1 + TASK_CAP (el promedio de
#   extra_task por capa es TASK_CAP). Candado duro: ese peor caso NO debe alcanzar Inquebrantable (1.10).
#   => TASK_CAP < 0.10. Además ANCLAS>SOPORTES>TASKS exige impacto-task < impacto-soporte (~0.05/capa
#   en base-media). Tomamos TASK_CAP=0.06: cumplir-justo+tasks-full(todas) = 1.06 (Plenitud, no Inq),
#   y el impacto-task por capa (0.06) < impacto-soporte máximo (~0.10 sobre gap=1). El ~0.1 del dueño
#   era orientativo: la curva exige bajarlo a 0.06 para que el candado anti-abuso se respete gratis.
TASK_CAP = 0.06
K_T = 0.8   # saturación: 1 task -> 55% del techo, 3 -> 91%, 10 -> ~99.97%

# --- PESO DE CAPA por MASA DE SUSTANCIA ---
# Axioma W-cal: "capa solo-soportes pesa menos que capa con anclas, continuo, sin gate duro.
#   masa = MASS_MIN si no hay anclas (solo sustancia de soporte), 1.0 con anclas; el peso es la masa."
MASS_ANCHOR = 1.0
MASS_SUPPORT_ONLY = 0.35   # capa sin anclas, solo soportes: ~1/3 de la masa de una capa-ancla

# ============ SEÑAL DE SOPORTE (bloque, no crece con cantidad) ============
def support_signal(soportes_dias):
    # soportes_dias: lista de "días sostenidos" por soporte (0..7). Señal por soporte: min(d/4,1).
    # Bloque = PROMEDIO de las señales (no suma) -> 1 o 5 soportes pesan lo mismo; se reparten.
    if not soportes_dias: return None
    sigs=[min(d/DIAS_VENTANA_SOPORTE,1.0) for d in soportes_dias]
    return sum(sigs)/len(sigs)

# ============ APORTE DE TASKS DEL DÍA (efímero) ============
def task_extra(n_tasks, extra_anclas):
    if n_tasks<=0: return 0.0
    raw = TASK_CAP*(1-math.exp(-K_T*n_tasks))      # saturado por cantidad, techo TASK_CAP
    headroom = max(SMAX - extra_anclas, 0.0)         # presupuesto restante hacia el techo 0.5
    return min(raw, headroom)                          # clamp -> nunca rompe smax por construcción

# ============ VALOR DE CAPA con presupuesto/headroom ============
def layer_value_and_mass(L):
    """L = {'anchors':[r..], 'supports':[dias..], 'tasks':n}. Devuelve (valor_capa, masa)."""
    a = L.get('anchors') or []
    sup = L.get('supports')
    n_tasks = L.get('tasks',0) or 0
    if a:
        base_anclas = sum(min(r,1) for r in a)/len(a)
        extra_anclas = sum(max(r-1,0) for r in a)/len(a)
        mass = MASS_ANCHOR
    else:
        base_anclas = 0.0
        extra_anclas = 0.0
        mass = MASS_SUPPORT_ONLY if sup else 0.0
    # --- SOPORTE: llena el gap de base, bidireccional ---
    g = support_signal(sup)
    if g is not None:
        gap = 1.0 - base_anclas                       # presupuesto de base sin usar
        if a:
            base = base_anclas + ETA_S*gap*(2*g-1)    # bidireccional leve, topado en 1 por gap
        else:
            # capa solo-soporte: la señal ES la base (sustancia reducida via masa)
            base = g
        base = max(0.0, min(base,1.0))
    else:
        base = base_anclas
    # --- TASK: llena el headroom del extra, efímero ---
    extra = extra_anclas + task_extra(n_tasks, extra_anclas)
    extra = min(extra, SMAX)                            # candado techo 0.5 explícito (redundante)
    return min(base,1.0)+extra, mass

def score_C(capas):
    vals=[]; masses=[]
    for L in capas:
        v,m = layer_value_and_mass(L)
        if m>0:
            vals.append(v*m); masses.append(m)
    return sum(vals)/sum(masses) if masses else 0.0

# ====================================================================
# VERIFICACIÓN
# ====================================================================
J=R(4,30,[30]*4); XL=R(4,30,[60]*7); DEF=R(4,30,[30,30,30])
print(f"Anclas ref: cumplir-justo J={J:.4f}  superhabit XL={XL:.4f}  deficit DEF(3d)={DEF:.4f}")
print(f"Params despejados: ETA_S={ETA_S} ventana={DIAS_VENTANA_SOPORTE}d | TASK_CAP={TASK_CAP} K_T={K_T} | masa solo-sop={MASS_SUPPORT_ONLY}")
print("="*92)

# (a) Sol=Tin intacto (superhabit rinde igual en cualquier capa; sin soportes/tasks)
sSol=score_C([{'anchors':[XL]},{'anchors':[J]},{'anchors':[J]}])
sTin=score_C([{'anchors':[J]},{'anchors':[J]},{'anchors':[XL]}])
print(f"(a) Sol={sSol:.6f} Tin={sTin:.6f}  EMPATAN={abs(sSol-sTin)<1e-9}")

# (b) cumplir-justo = 1.0 = Plenitud
sJ=score_C([{'anchors':[J]},{'anchors':[J]},{'anchors':[J]}])
print(f"(b) cumplir-justo score={sJ:.6f} banda={band(sJ)}  (esperado 1.0 Plenitud)")

# (c) soporte bidireccional leve y NO crece con la cantidad
base_half = 0.5  # capa con ancla a media base
def cap_with_support(base_r, sup):  # base_r mins ratio -> ancla con base=base_r
    return score_C([{'anchors':[base_r],'supports':sup},{'anchors':[J]},{'anchors':[J]}])
s_no   = cap_with_support(0.5, None)
s_full = cap_with_support(0.5, [7])          # 1 soporte sostenido
s_neg  = cap_with_support(0.5, [0])          # 1 soporte descuidado
s_5sup = cap_with_support(0.5, [7,7,7,7,7])  # 5 soportes sostenidos
s_1sup = cap_with_support(0.5, [7])
print(f"(c) base-media: sin-sop={s_no:.4f}  sop-sostenido={s_full:.4f} (sube) sop-descuidado={s_neg:.4f} (baja)")
print(f"    bidireccional leve: +{s_full-s_no:.4f} / {s_neg-s_no:.4f}")
print(f"    NO crece con cantidad: 1 soporte={s_1sup:.6f}  5 soportes={s_5sup:.6f}  iguales={abs(s_1sup-s_5sup)<1e-9}")

# (d) capa solo-soportes pesa menos
# Comparar impacto de una capa MALA (DEF) vs capa solo-soportes mala, sobre el global
s_anchor_bad = score_C([{'anchors':[DEF]},{'anchors':[J]},{'anchors':[J]}])
s_suponly_bad= score_C([{'supports':[0]},{'anchors':[J]},{'anchors':[J]}])  # solo-sop, descuidada
s_suponly_ok = score_C([{'supports':[7]},{'anchors':[J]},{'anchors':[J]}])
print(f"(d) capa-ancla mala arrastra a {s_anchor_bad:.4f}; capa solo-sop mala arrastra MENOS a {s_suponly_bad:.4f}")
print(f"    (solo-sop pesa menos -> arrastra menos). solo-sop OK={s_suponly_ok:.4f}")

# (e) tasks dentro de la curva: no rompen techo 0.5, 100 tasks saturan, reset diario
# capa con superhabit XL (extra alto) + tasks: el clamp al headroom impide pasar 0.5
extra_xl = sum(max(r-1,0) for r in [XL])
te_1   = task_extra(1, extra_xl)
te_100 = task_extra(100, extra_xl)
te_justo_1   = task_extra(1, 0.0)
te_justo_100 = task_extra(100, 0.0)
print(f"(e) extra_anclas(XL)={extra_xl:.4f}; con tasks NUNCA pasa SMAX=0.5:")
print(f"    XL + 1 task -> extra={min(extra_xl+te_1,0.5):.4f}  + 100 tasks -> extra={min(extra_xl+te_100,0.5):.4f} (<=0.5)")
print(f"    cumplir-justo + 1 task={te_justo_1:.4f}  + 100 tasks={te_justo_100:.4f} (saturan ~TASK_CAP={TASK_CAP})")
# reset diario: el aporte es funcion de n_tasks de HOY; mañana n_tasks=0 -> 0
print(f"    reset diario: hoy n=3 -> +{task_extra(3,0):.4f}; mañana n=0 -> +{task_extra(0,0):.4f}")

# (f) tasks arañan UN cruce cerca, pero cumplir-justo+tasks NO compra Inquebrantable solo
#  buscamos una config justo por DEBAJO de 0.85 (cruce EnMarcha->Plenitud) para ver el arañazo
#  capa con leve superhabit en una, justas en otras, de modo que score quede ~0.83
# barrido fino: tercera capa con base creciente (resto justas) hasta quedar apenas debajo de 0.85
cfg_near=None;s_near=None
steps=200
for i in range(steps+1):
    bval=0.5+0.5*i/steps   # base de la 3a capa de 0.5 a 1.0
    cfg=[{'anchors':[J]},{'anchors':[J]},{'anchors':[bval]}]
    s=score_C(cfg)
    if 0.80<=s<0.85:
        cfg_near=cfg;s_near=s   # tomamos el más alto dentro de la banda
if cfg_near:
    # tasks en la capa débil (la 3a), saturadas
    cfg_task=cfg_near[:2]+[dict(cfg_near[2],tasks=10)]
    s_near_task=score_C(cfg_task)
    print(f"(f) cerca del cruce EnMarcha->Plenitud: sin task={s_near:.4f} ({band(s_near)}) -> +tasks={s_near_task:.4f} ({band(s_near_task)}) [araña el cruce={band(s_near)!=band(s_near_task)}]")
else:
    print("(f) (no se ubicó config en [0.80,0.85)) — ver barrido")
s_justo_task = score_C([{'anchors':[J],'tasks':100},{'anchors':[J],'tasks':100},{'anchors':[J],'tasks':100}])
print(f"    cumplir-justo + 100 tasks en TODAS las capas={s_justo_task:.4f} ({band(s_justo_task)}) -> NO compra Inquebrantable={s_justo_task<1.10}")

# (g) anti-gate: continuidad. Barremos señal de soporte y cantidad de tasks
prev=None;mx=0
for i in range(1001):
    g=i/1000
    s=score_C([{'anchors':[0.5],'supports':[g*7]},{'anchors':[J]},{'anchors':[J]}])
    if prev is not None: mx=max(mx,abs(s-prev))
    prev=s
print(f"(g) anti-gate soporte: paso max |dScore| con d(señal)=0.001 -> {mx:.6f} (continuo)")
prev=None;mxt=0
for i in range(0,201):
    s=score_C([{'anchors':[J],'tasks':i/4.0},{'anchors':[J]},{'anchors':[J]}])
    if prev is not None: mxt=max(mxt,abs(s-prev))
    prev=s
print(f"    anti-gate tasks: paso max |dScore| con d(n_task)=0.25 -> {mxt:.6f} (continuo, satura)")

# (h) ANCLAS > SOPORTES > TASKS en impacto MÁXIMO por capa (autoridad de cada mecanismo)
#  ancla: rango de base 0->1 (la capa entera). soporte: rango bidireccional sobre el MAYOR gap.
#  task: techo TASK_CAP. Medimos el swing máximo que cada mecanismo puede provocar en UNA capa.
N=3
imp_anchor = (1.0 - 0.0)/N                              # base de la capa de 0 a 1
# soporte: máximo |2g-1|=1 sobre el mayor gap útil. gap grande -> base_anclas baja.
sup_hi = cap_with_support(0.3,[7]); sup_lo = cap_with_support(0.3,[0])
imp_support = (sup_hi - sup_lo)
imp_task   = score_C([{'anchors':[J],'tasks':100}]+[{'anchors':[J]}]*2) - score_C([{'anchors':[J]}]*3)
print(f"(h) AUTORIDAD máx por capa: ANCLA(base 0->1)={imp_anchor:.4f} > SOPORTE(swing sobre gap)={imp_support:.4f} > TASK(0->100)={imp_task:.4f}")
print(f"    orden ANCLAS>SOPORTES>TASKS = {imp_anchor>imp_support>imp_task}")
