"""
Modelo del VALOR DE CAPA — v2 (directiva del dueño 2026-06-12).
CAMBIO vs v1: pesos de capa IGUALES (1/N). El opt-in ya NO infla el peso de la capa;
pesa MUCHÍSIMO solo DENTRO de su capa (K_INT alto). Extra promediado SIMPLE.
Reproducible: python3 modelo_consolidado_v2.py
"""
import math

# ── Caja negra del ancla (consolidada, sin cambios) ──
def R(F, T, mins, gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0, smax=0.5, s0=0.5):
    mk = sorted([m for m in mins if m > 0], reverse=True); D = len(mk)
    if D == 0: return 0.0
    r = [m/T for m in mk]; c, v = r[:min(D, F)], r[min(D, F):]
    u = lambda x: min(x, 1.0)**gamma
    phi = sum(u(x) for x in c)/F; V = sum(u(x) for x in v)
    base = 1 - (1-phi)*math.exp(-lam_v*V)
    St = sum(max(x-1, 0) for x in c)/F; Sd = V/(7-F) if F < 7 else 0.0
    wt = (F/7)**kappa
    S = smax*(1 - math.exp(-(wt*St + (1-wt)*Sd)/s0))
    return base + (base**p)*S

# ── Parámetros del modelo de capa v2 ──
EM_TOP=0.85; W_EXTRA=0.6; DELTA=0.10
B_SLEEP=0.50              # base de sueño sin dato
FLOOR_OPT=0.0; Q_OPT=1.0  # curva del opt-in (default lineal)
SUP_SAT=0.08; SUP_K=2.0; TASK_SAT=0.04; TASK_K=2.0
def g(M): return FLOOR_OPT + (1-FLOOR_OPT)*(M**Q_OPT)

def band(s):
    if s < 0.40: return "RESTAURACION"
    if s < 0.62: return "ATENCION"
    if s <= EM_TOP+1e-9: return "EN MARCHA"
    if s < 1.0+DELTA: return "PLENITUD"
    return "INQUEBRANTABLE"

def sleep_weekly(nights):
    have=[n for n in nights if n is not None]; c=len(have)/7.0
    return B_SLEEP if c==0 else c*(sum(have)/len(have))+(1-c)*B_SLEEP
def sobriety_signal(tracks):   # BINARIO por track (held/broke). producto: 1 track roto -> 0
    sig=1.0
    for held in tracks: sig*= (1.0 if held else 0.0)
    return sig
def sup_term(f):
    if f is None: return 0.0
    sg=2*f-1; s=SUP_SAT*(1-math.exp(-SUP_K*abs(sg))); return s if sg>=0 else -s
def task_term(f):
    return 0.0 if (f is None or f<=0) else TASK_SAT*(1-math.exp(-TASK_K*f))

# ── K_INT: peso del opt-in DENTRO de su capa. Despejado del axioma de estado ──
def solve_Kint(target_state):
    # 3 capas, anclas perfectas, opt-in de UNA en su piso (M=0) -> target_state
    def f(K):
        ep_opt=(1.0 + K*g(0.0))/(1+K)   # capa con opt-in: anclas=1, M=0
        base=(1.0 + ep_opt + 1.0)/3
        return EM_TOP*base - target_state
    lo,hi=0.001,200.0
    for _ in range(200):
        m=(lo+hi)/2
        hi=m if f(lo)*f(m)<=0 else hi; lo=lo if f(lo)*f(m)<=0 else m
    return (lo+hi)/2
K_INT = solve_Kint(0.62)   # "sin dormir con todo perfecto -> borde Atención"

class Layer:
    def __init__(s,name,anchors=None,optin=None,M=None,sup=None,task=None):
        s.name=name;s.anchors=anchors or [];s.optin=optin;s.M=M;s.sup=sup;s.task=task
    def has(s): return len(s.anchors)>0
    def active(s): return s.has() or (s.M is not None)
    def en_pie(s):
        ab=(sum(min(r,1.0) for r in s.anchors)/len(s.anchors)) if s.has() else None
        if s.M is not None:
            core=(ab + K_INT*g(s.M))/(1+K_INT) if ab is not None else g(s.M)
        else:
            core=ab if ab is not None else 0.0
        return max(0.0,min(core+sup_term(s.sup)+task_term(s.task),1.0))
    def destaco(s): return 0.0 if not s.has() else sum(max(r-1,0) for r in s.anchors)/len(s.anchors)

def score(layers):
    act=[L for L in layers if L.active()]; N=len(act)
    w=1.0/N                                   # PESOS IGUALES (cambio v2)
    base=sum(w*L.en_pie() for L in act)
    al=[L for L in act if L.has()]
    extra=sum(L.destaco() for L in al)/len(al) if al else 0.0   # promedio SIMPLE
    return base, extra, EM_TOP*base + W_EXTRA*extra, w

J=R(4,30,[30]*4); SUP=R(4,30,[30]*6); XL=R(4,30,[60]*7); MIT=R(4,30,[30,30]); UNO=R(4,30,[30])

print("="*92)
print(f"MODELO v2 — pesos de capa IGUALES (1/N). K_INT (opt-in dentro de su capa) = {K_INT:.2f}")
print(f"   sueño/sobriedad pesan {K_INT/(1+K_INT)*100:.0f}% de su capa; anclas {1/(1+K_INT)*100:.0f}%")
print(f"   bandas: REST<0.40 · ATEN<0.62 · EN MARCHA<=0.85 · PLENITUD<1.10 · INQUEBRANTABLE>=1.10")
print("="*92)

def C(t, layers, nota=""):
    b,e,s,w=score(layers)
    print(f"{t:48s} base={b:.3f} extra={e:.3f} estado={s:.3f} → {band(s):14s}{nota}")

print("\n--- CASOS LÍMITE (12) ---")
C("1. Todo justo + duerme bien", [Layer("I",[J]),Layer("Cu",[J],'s',1.0),Layer("Co",[J])], "  [EJE]")
C("2. Superhabit medio repartido", [Layer("I",[SUP]),Layer("Cu",[SUP],'s',1.0),Layer("Co",[SUP])])
C("2b. Superhabit FUERTE repartido", [Layer("I",[XL]),Layer("Cu",[XL],'s',1.0),Layer("Co",[XL])])
C("3. Capa solo-opt-in (sin anclas)", [Layer("I",[J]),Layer("Cu",[],'s',0.9),Layer("V",[J]),Layer("P",[J])], "  [Cuerpo extra=0]")
b,e,s,w=score([Layer("I",[J]),Layer("Cu",[J],'s',1.0),Layer("Co",[J],'b',sobriety_signal([True]))])
print(f"{'4. Apretón 3 capas + 2 opt-ins':48s} base={b:.3f} extra={e:.3f} estado={s:.3f} → {band(s):14s}  [peso c/u={w:.3f}, suman {3*w:.2f}]")
b1,e1,s1,_=score([Layer("I",[SUP]),Layer("Cu",[SUP],'s',1.0),Layer("Co",[SUP])])
b2,e2,s2,_=score([Layer("I",[SUP]),Layer("Cu",[SUP],'s',0.15),Layer("Co",[SUP])])
print(f"{'5. Mal sueño: extra intacto?':48s} sueño OK extra={e1:.3f} | sueño malo extra={e2:.3f} | base {b1:.3f}→{b2:.3f}  [extra igual={abs(e1-e2)<1e-9}]")
print(f"{'6. Sueño sin dato':48s} M={sleep_weekly([None]*7):.3f} (=B_SLEEP, no 0)")
C("7a. Recaída DENTRO 7d", [Layer("I",[J]),Layer("Cu",[J]),Layer("Co",[J],'b',sobriety_signal([False]))])
C("7b. Recaída FUERA 7d (held)", [Layer("I",[J]),Layer("Cu",[J]),Layer("Co",[J],'b',sobriety_signal([True]))])
print(f"{'8. Multi-sobriedad (3 tracks, rompió 1)':48s} señal={sobriety_signal([True,False,True]):.2f} = 1 track roto {sobriety_signal([False]):.2f} (no diluye)")
C("9a. Soportes FULL (anclas en déficit leve)", [Layer("I",[R(4,30,[30,30,20])],sup=1.0),Layer("Cu",[R(4,30,[30,30,20])],'s',1.0),Layer("Co",[R(4,30,[30,30,20])])])
C("9b. Soportes DESCUIDADOS", [Layer("I",[R(4,30,[30,30,20])],sup=0.0),Layer("Cu",[R(4,30,[30,30,20])],'s',1.0),Layer("Co",[R(4,30,[30,30,20])])])
print(f"{'10. Task vs soporte (full)':48s} task={task_term(1.0):.4f} < soporte={sup_term(1.0):.4f}")
print(f"{'11. Peso de capa SIEMPRE 1/N (con o sin opt-in)':48s} 3 capas → c/u {1/3:.3f}; el opt-in NO cambia el peso de capa")
print(f"{'12. Peso de capa por N':48s} N=3→{1/3:.3f}  N=4→{1/4:.3f}  N=5→{1/5:.3f}")

print("\n--- RAREZAS RESUELTAS ---")
_,_,sSol,_=score([Layer("I",[XL]),Layer("Cu",[J],'s',1.0),Layer("Co",[J])])
_,_,sTin,_=score([Layer("I",[J]),Layer("Cu",[XL],'s',1.0),Layer("Co",[J])])
print(f"  R3 Sol(leyó+) vs Tin(caminó+): {sSol:.3f} vs {sTin:.3f}  → empatan: {abs(sSol-sTin)<1e-9}")
_,_,sSolo,_=score([Layer("I",[J]),Layer("Cu",[XL],'s',1.0),Layer("Co",[J])])
_,_,sRep,_=score([Layer("I",[XL]),Layer("Cu",[XL],'s',1.0),Layer("Co",[XL])])
print(f"  Inq exige reparto: superhabit solo-1-capa {sSolo:.3f} ({band(sSolo)}) vs repartido {sRep:.3f} ({band(sRep)})")

print("\n--- D8 y ARRASTRE POR N ---")
_,_,sMalSueno,_=score([Layer("I",[J]),Layer("Cu",[J],'s',0.15),Layer("Co",[J])])
_,_,sRecaida,_=score([Layer("I",[J]),Layer("Cu",[J]),Layer("Co",[J],'b',sobriety_signal([False]))])
print(f"  D8: mal sueño pésimo {sMalSueno:.3f} ({band(sMalSueno)}) vs recaída {sRecaida:.3f} ({band(sRecaida)}) → recaída pega más: {sRecaida<sMalSueno}")
for N in [3,4,5]:
    _,_,sm,_=score([Layer("Cu",[J],'s',0.15)]+[Layer(f"c{i}",[J]) for i in range(N-1)])
    print(f"  arrastre mal sueño N={N}: {sm:.3f} {band(sm)}")
