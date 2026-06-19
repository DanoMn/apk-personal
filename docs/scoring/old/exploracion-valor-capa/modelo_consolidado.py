"""
Modelo CONSOLIDADO del valor de capa — Autonomía sin límites.
Merge de los 3 proponentes (A aditivo · B cimiento suave · C unificación relacional) + research.
Verificación de los 12 casos límite + despeje de K. Todo reproducible: python3 modelo_consolidado.py
"""
import math

# ── Caja negra del ancla (consolidada) ──
def R(F, T, mins, gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0, smax=0.5, s0=0.5):
    marked = sorted([m for m in mins if m > 0], reverse=True); D = len(marked)
    if D == 0: return 0.0
    r = [m/T for m in marked]; commit, vol = r[:min(D, F)], r[min(D, F):]
    u = lambda x: min(x, 1.0)**gamma
    phi = sum(u(x) for x in commit)/F; V = sum(u(x) for x in vol)
    base = 1 - (1-phi)*math.exp(-lam_v*V)
    St = sum(max(x-1, 0) for x in commit)/F; Sd = V/(7-F) if F < 7 else 0.0
    wt = (F/7)**kappa
    S = smax*(1 - math.exp(-(wt*St + (1-wt)*Sd)/s0))
    return base + (base**p)*S

# ── Parámetros del modelo de capa (ilustrativos; K se DESPEJA, ver abajo) ──
EM_TOP   = 0.85   # tope de En marcha = techo del canal BASE solo (EJE, axioma 3)
W_EXTRA  = 0.6    # cuánto sube el superhabit en el estado
DELTA    = 0.10   # margen de Inquebrantable
B_SLEEP  = 0.50   # base de sueño sin dato (no tira a 0)
A_SOB    = 0.85   # golpe por recaída (señal tras 1 recaída = 1-A_SOB)
FLOOR_OPT= 0.0    # suelo de la curva del opt-in (innovación de B); 0 = sin suelo
Q_OPT    = 1.0    # curvatura del opt-in (B); 1 = lineal (convergencia A/C). >1 = sabor dominó
SUP_SAT  = 0.08; SUP_K = 2.0     # soportes: aporte ±light, saturado
TASK_SAT = 0.04; TASK_K = 2.0    # tasks: aporte +light, < soporte, saturado

def g(M):  # curva del opt-in (de B): con FLOOR=0,Q=1 -> g(M)=M (aditivo puro)
    return FLOOR_OPT + (1-FLOOR_OPT)*(M**Q_OPT)

def band(s):
    if s < 0.40: return "Rojo"
    if s < 0.62: return "Atencion"
    if s <= EM_TOP + 1e-9: return "En marcha"
    if s < 1.0 + DELTA: return "Plenitud"
    return "Inquebrantable"

def sleep_weekly(nights):  # nights: lista de 7, cada una [0,1] o None
    have = [n for n in nights if n is not None]; c = len(have)/7.0
    return B_SLEEP if c == 0 else c*(sum(have)/len(have)) + (1-c)*B_SLEEP

def sobriety_signal(n_relapses):   # recaidas DENTRO de 7d (de C: cuenta recaidas, no tracks)
    return (1 - A_SOB)**n_relapses

def sup_term(frac):   # frac = fraccion sostenida [0,1]; centrado (de B): full=+, descuidado=-
    if frac is None: return 0.0
    signed = 2*frac - 1
    s = SUP_SAT*(1 - math.exp(-SUP_K*abs(signed)))
    return s if signed >= 0 else -s

def task_term(frac):  # frac = fraccion de tasks hechas; solo suma, < soporte
    if frac is None or frac <= 0: return 0.0
    return TASK_SAT*(1 - math.exp(-TASK_K*frac))

class Layer:
    def __init__(self, name, anchors=None, K_opt=0.0, M=None, sup=None, task=None):
        self.name=name; self.anchors=anchors or []; self.K_opt=K_opt
        self.M=M; self.sup=sup; self.task=task
    def has_anchors(self): return len(self.anchors) > 0
    def size(self):  # bloque de anclas = 1 (promedian); opt-in = K. nº anclas NO cambia tamaño
        return (1.0 if self.has_anchors() else 0.0) + (self.K_opt if self.M is not None else 0.0)
    def en_pie(self):
        ab = (sum(min(r,1.0) for r in self.anchors)/len(self.anchors)) if self.has_anchors() else None
        if self.M is not None:
            core = (1.0*ab + self.K_opt*g(self.M))/(1.0+self.K_opt) if ab is not None else g(self.M)
        else:
            core = ab if ab is not None else 0.0
        return max(0.0, min(core + sup_term(self.sup) + task_term(self.task), 1.0))
    def destaco(self):  # EXTRA: solo anclas
        return 0.0 if not self.has_anchors() else sum(max(r-1.0,0.0) for r in self.anchors)/len(self.anchors)

def score(layers):
    sizes=[L.size() for L in layers]; total=sum(sizes)
    weights=[s/total for s in sizes]
    base=sum(w*L.en_pie() for w,L in zip(weights,layers))
    al=[(w,L) for w,L in zip(weights,layers) if L.has_anchors()]   # extra SOLO entre capas con anclas
    extra = sum((w/sum(x for x,_ in al))*L.destaco() for w,L in al) if al else 0.0
    return base, extra, EM_TOP*base + W_EXTRA*extra, weights

# ═══════════ DESPEJE DE K (de un axioma de estado, por bisección) ═══════════
def solve_K(target_state, M_optin, N=3):
    """axioma: N capas, anclas perfectas (R=1), opt-in de UNA capa en señal M_optin -> target_state"""
    def f(K):
        layers=[Layer("opt", anchors=[1.0], K_opt=K, M=M_optin)] + \
               [Layer(f"c{i}", anchors=[1.0]) for i in range(N-1)]
        return score(layers)[2] - target_state
    lo,hi=0.001,50.0
    for _ in range(100):
        mid=(lo+hi)/2
        if f(lo)*f(mid)<=0: hi=mid
        else: lo=mid
    return (lo+hi)/2

K_SLEEP = solve_K(0.62, 0.0, N=3)            # "sin dormir toda la semana -> borde Atencion"
K_SOBR  = solve_K(0.50, sobriety_signal(1), N=3)  # "1 recaida -> Atencion baja (peor que mal sueño)"

print("="*92)
print(f"K DESPEJADOS (no elegidos): K_sleep={K_SLEEP:.3f}  K_sobr={K_SOBR:.3f}   (K_sobr>K_sleep => D8 OK)")
print(f"params: EM_TOP={EM_TOP} W_EXTRA={W_EXTRA} A_SOB={A_SOB} B_SLEEP={B_SLEEP} FLOOR={FLOOR_OPT} Q={Q_OPT}")
print("="*92)

R1   = R(3,30,[30,30,30])              # cumplio justo  -> 1.000
RSUP = R(3,30,[60,60,60])              # superhabit medio
RBIG = R(3,30,[90,90,90,90,90,90,90])  # superhabit fuerte (7d, triple tiempo)
print(f"anclas de prueba: R_justo={R1:.3f}  R_sup_medio={RSUP:.3f}  R_sup_fuerte={RBIG:.3f}\n")

def show(n, layers, nota=""):
    b,e,s,w = score(layers)
    print(f"{n:42s} base={b:.3f} extra={e:.3f} estado={s:.3f} -> {band(s):14s} {nota}")
    return b,e,s,w

print("--- 12 CASOS LIMITE ---")
# 1
show("1. Todo justo (R=1, sueño OK)",
    [Layer("Int",[R1]), Layer("Cue",[R1],K_SLEEP,1.0), Layer("Con",[R1])], "[EJE: debe ser En marcha]")
# 2
show("2. Superhabit medio repartido",
    [Layer("Int",[RSUP]), Layer("Cue",[RSUP],K_SLEEP,1.0), Layer("Con",[RSUP])])
# 2b
show("2b. Superhabit FUERTE repartido",
    [Layer("Int",[RBIG]), Layer("Cue",[RBIG],K_SLEEP,1.0), Layer("Con",[RBIG])])
# 3
_,_,_,w3 = show("3. Capa solo-opt-in (Cuerpo sin anclas)",
    [Layer("Int",[R1]), Layer("Cue",[],K_SLEEP,0.8), Layer("Con",[R1]), Layer("Vin",[R1])],
    "[Cuerpo: extra=0]")
# 4
_,_,_,w4 = show("4. El apreton: 3 capas + 2 opt-ins",
    [Layer("Int",[R1]), Layer("Cue",[R1],K_SLEEP,1.0), Layer("Con",[R1],K_SOBR,sobriety_signal(0))])
print(f"     -> suma de pesos = {sum(w4):.6f}")
# 5
b_ok,e_ok,_,_ = show("5a. Mal sueño? primero con sueño OK",
    [Layer("Int",[RSUP]), Layer("Cue",[RSUP],K_SLEEP,1.0), Layer("Con",[RSUP])])
b_mal,e_mal,s_mal,_ = score([Layer("Int",[RSUP]), Layer("Cue",[RSUP],K_SLEEP,0.3), Layer("Con",[RSUP])])
print(f"{'5b. ...mismo pero sueño MALO (0.3)':42s} base={b_mal:.3f} extra={e_mal:.3f} estado={s_mal:.3f} -> {band(s_mal):14s} "
      f"[extra igual? {abs(e_ok-e_mal)<1e-9}  base baja? {b_mal<b_ok}]")
# 6
print(f"6. Sueño SIN dato            -> M = {sleep_weekly([None]*7):.3f} (=B_SLEEP, no 0)")
print(f"   Sueño parcial (3 noches)  -> M = {sleep_weekly([0.7,0.7,0.7,None,None,None,None]):.3f}")
# 7
_,_,s7in,_ = show("7a. Recaida DENTRO 7d",
    [Layer("Int",[R1]), Layer("Cue",[R1]), Layer("Con",[R1],K_SOBR,sobriety_signal(1))])
_,_,s7out,_ = score([Layer("Int",[R1]), Layer("Cue",[R1]), Layer("Con",[R1],K_SOBR,sobriety_signal(0))])
print(f"{'7b. Recaida FUERA 7d (held)':42s} estado={s7out:.3f} -> {band(s7out):14s} [dentro<fuera? {s7in<s7out}]")
# 8
print(f"8. Multi-sobriedad: 1 recaida={sobriety_signal(1):.3f}  (igual con 1 o N tracks: cuenta recaidas)")
print(f"   2 recaidas={sobriety_signal(2):.3f} (<1 recaida: pega mas, no premia tener tracks)")
# 9  (anclas con deficit leve para que el aporte del soporte sea visible)
RDEF = R(3,30,[30,30,20])  # un dia corto -> base < 1, asi el soporte mueve
b_full,_,s_full,_ = show("9a. Soportes FULL (anclas en deficit leve)",
    [Layer("Int",[RDEF],sup=1.0), Layer("Cue",[RDEF],K_SLEEP,1.0), Layer("Con",[RDEF])])
b_desc,_,s_desc,_ = show("9b. Soportes DESCUIDADOS",
    [Layer("Int",[RDEF],sup=0.0), Layer("Cue",[RDEF],K_SLEEP,1.0), Layer("Con",[RDEF])])
print(f"     -> mueve light: dif estado = {s_full-s_desc:.3f}")
# 10
print(f"10. Task vs soporte (full): task={task_term(1.0):.4f} < soporte={sup_term(1.0):.4f} -> {task_term(1.0)<sup_term(1.0)}")
# 11
L1=Layer("a",[R1],K_SLEEP,1.0); L3=Layer("b",[R1,R1,R1],K_SLEEP,1.0)
print(f"11. Mas anclas, mismo peso: size(1 ancla)={L1.size():.2f} == size(3 anclas)={L3.size():.2f}")
# 12
print("12. Peso del opt-in baja con N:")
for N in [3,4,5]:
    ls=[Layer("Cue",[R1],K_SLEEP,1.0)]+[Layer(f"c{i}",[R1]) for i in range(N-1)]
    _,_,_,w=score(ls)
    print(f"     N={N}: peso Cuerpo(+sueño)={w[0]:.3f}")
