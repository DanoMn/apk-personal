"""
Casos REALES (personas, situaciones de vida) para juzgar el merge del valor de capa.
Autocontenido. Reusa el modelo consolidado. python3 casos_reales.py
"""
import math

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

EM_TOP=0.85; W_EXTRA=0.6; DELTA=0.10; B_SLEEP=0.50; A_SOB=0.85
FLOOR_OPT=0.0; Q_OPT=1.0; SUP_SAT=0.08; SUP_K=2.0; TASK_SAT=0.04; TASK_K=2.0
def g(M): return FLOOR_OPT + (1-FLOOR_OPT)*(M**Q_OPT)
def band(s):
    if s<0.40: return "RESTAURACION"
    if s<0.62: return "ATENCION"
    if s<=EM_TOP+1e-9: return "EN MARCHA"
    if s<1.0+DELTA: return "PLENITUD"
    return "INQUEBRANTABLE"
def sleep_weekly(nights):
    have=[n for n in nights if n is not None]; c=len(have)/7.0
    return B_SLEEP if c==0 else c*(sum(have)/len(have))+(1-c)*B_SLEEP
def sobriety_signal(n): return (1-A_SOB)**n
def sup_term(f):
    if f is None: return 0.0
    sg=2*f-1; s=SUP_SAT*(1-math.exp(-SUP_K*abs(sg))); return s if sg>=0 else -s
def task_term(f):
    return 0.0 if (f is None or f<=0) else TASK_SAT*(1-math.exp(-TASK_K*f))
class Layer:
    def __init__(s,name,anchors=None,K_opt=0.0,M=None,sup=None,task=None):
        s.name=name;s.anchors=anchors or [];s.K_opt=K_opt;s.M=M;s.sup=sup;s.task=task
    def has(s): return len(s.anchors)>0
    def size(s): return (1.0 if s.has() else 0.0)+(s.K_opt if s.M is not None else 0.0)
    def en_pie(s):
        ab=(sum(min(r,1.0) for r in s.anchors)/len(s.anchors)) if s.has() else None
        if s.M is not None:
            core=(ab+s.K_opt*g(s.M))/(1+s.K_opt) if ab is not None else g(s.M)
        else: core=ab if ab is not None else 0.0
        return max(0.0,min(core+sup_term(s.sup)+task_term(s.task),1.0))
    def destaco(s): return 0.0 if not s.has() else sum(max(r-1,0) for r in s.anchors)/len(s.anchors)
def score(layers):
    sz=[L.size() for L in layers]; tot=sum(sz); w=[x/tot for x in sz]
    base=sum(wi*L.en_pie() for wi,L in zip(w,layers))
    al=[(wi,L) for wi,L in zip(w,layers) if L.has()]
    extra=sum((wi/sum(x for x,_ in al))*L.destaco() for wi,L in al) if al else 0.0
    return base,extra,EM_TOP*base+W_EXTRA*extra,w

def solve_K(target,Mv,N=3):
    def f(K):
        ls=[Layer("o",[1.0],K,Mv)]+[Layer(f"c{i}",[1.0]) for i in range(N-1)]
        return score(ls)[2]-target
    lo,hi=0.001,50.0
    for _ in range(100):
        m=(lo+hi)/2
        if f(lo)*f(m)<=0: hi=m
        else: lo=m
    return (lo+hi)/2
KS=solve_K(0.62,0.0); KB=solve_K(0.50,sobriety_signal(1))

def caso(titulo, layers, lectura):
    b,e,s,w=score(layers)
    print(f"\n### {titulo}")
    for wi,L in zip(w,layers):
        det=[]
        if L.has(): det.append(f"anclas→{sum(min(r,1) for r in L.anchors)/len(L.anchors):.2f}")
        if L.M is not None: det.append(f"opt-in M={L.M:.2f}")
        if L.sup is not None: det.append(f"sup={L.sup:.1f}")
        if L.task is not None: det.append(f"task={L.task:.1f}")
        ex=L.destaco()
        exs=f" +extra {ex:.2f}" if ex>0 else ""
        print(f"   {L.name:9s} peso {wi:.2f} | en_pie {L.en_pie():.2f}{exs}  ({', '.join(det)})")
    print(f"   ►► base={b:.3f}  extra={e:.3f}  ESTADO={s:.3f}  →  \033[1m{band(s)}\033[0m")
    print(f"      {lectura}")

# atajos de anclas
JUSTO  = R(4,30,[30,30,30,30])              # cumple su meta exacta
SUP    = R(4,30,[30,30,30,30,30,30])        # +2 dias (superhabit dias)
SUPXL  = R(4,30,[60,60,60,60,60,60,60])     # 7d doble tiempo (superhabit fuerte)
DEF1   = R(4,30,[30,30,30])                 # -1 dia
MITAD  = R(4,30,[30,30])                    # mitad
CASI0  = R(4,30,[30])                       # 1 sola sesion
CERO   = R(4,30,[])                         # nada

print("="*94)
print(f" CASOS REALES — modelo de valor de capa consolidado.  K_sleep={KS:.2f}  K_sobr={KB:.2f}")
print("="*94)

# ---------- GRADIENTE NORMAL ----------
caso("1. ANA — soltó casi todo (depresión). 3 capas, apenas 1 sesión por área. Sin opt-ins.",
    [Layer("Interior",[CASI0]),Layer("Cuerpo",[CASI0]),Layer("Conducta",[CASI0])],
    "Abandono real → debería ser Restauración.")
caso("2. BETO — media tabla. Cumple la mitad de cada meta. Sin opt-ins.",
    [Layer("Interior",[MITAD]),Layer("Cuerpo",[MITAD]),Layer("Conducta",[MITAD])],
    "Media base → Atención.")
caso("3. CARO — cumple JUSTO sus 3 metas y duerme bien. El usuario promedio sano.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo",[JUSTO],KS,1.0),Layer("Conducta",[JUSTO])],
    "Cumplir todo = el hogar operativo → En marcha (NO Plenitud).")
caso("4. SOL — cumple todo y se PASA en las 3 anclas (+2 días c/u). Duerme bien.",
    [Layer("Interior",[SUP]),Layer("Cuerpo",[SUP],KS,1.0),Layer("Conducta",[SUP])],
    "Se destacó repartido → Plenitud.")
caso("5. DANI — semana heroica: 7 días doble tiempo en las 3 anclas. Duerme bien.",
    [Layer("Interior",[SUPXL]),Layer("Cuerpo",[SUPXL],KS,1.0),Layer("Conducta",[SUPXL])],
    "Superhabit fuerte y repartido → Inquebrantable.")

# ---------- SENSIBILIDAD DE OPT-INS ----------
caso("6. INSOMNE FUNCIONAL — cumple sus 3 anclas perfecto, pero durmió PÉSIMO toda la semana.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo",[JUSTO],KS,0.15),Layer("Conducta",[JUSTO])],
    "El sueño es sensible: baja de En marcha hacia Atención aunque las anclas estén perfectas.")
caso("7. BETO RECAÍDA — cumple sus metas, sobriedad activa, pero RECAYÓ esta semana.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo",[JUSTO]),Layer("Conducta",[JUSTO],KB,sobriety_signal(1))],
    "Recaída en la ventana → golpe fuerte (más que el mal sueño).")
caso("8. BETO RECUPERADO — recayó hace un mes, pero esta semana limpio. Mismas metas.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo",[JUSTO]),Layer("Conducta",[JUSTO],KB,sobriety_signal(0))],
    "La recaída vieja NO entra (ventana 7d) → vuelve a En marcha. No castiga para siempre.")

# ---------- DESBALANCE / TRAMPAS ----------
caso("9. EL OBSESIVO — Proyecto BRILLANTE (7d doble tiempo) pero abandonó las otras 2 capas.",
    [Layer("Interior",[CASI0]),Layer("Cuerpo",[CASI0]),Layer("Proyecto",[SUPXL])],
    "Superhabit localizado NO rescata: la base hundida manda → Restauración/Atención.")
caso("10. CARO ESFUERZO TAPADO — se rompió caminando (+superhabit en Cuerpo) pero durmió MAL.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo",[SUP],KS,0.3),Layer("Conducta",[JUSTO])],
    "El mal sueño baja la base, pero el extra ganado caminando SOBREVIVE (no se borra).")
caso("11. EL GAMER DE SOPORTES — anclas a la mitad, pero llena de soportes para inflar.",
    [Layer("Interior",[MITAD],sup=1.0),Layer("Cuerpo",[MITAD],sup=1.0),Layer("Conducta",[MITAD],sup=1.0)],
    "Soportes full NO fabrican banda: sigue donde lo dejan sus anclas (Atención).")

# ---------- CASOS LÍMITE ESTRUCTURALES ----------
caso("12. SOLO TRACKEA SUEÑO — Interior/Vínculos/Proyecto con anclas; activó sueño en Cuerpo SIN anclas ahí.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo (solo sueño)",[],KS,0.9),Layer("Vínculos",[JUSTO]),Layer("Proyecto",[JUSTO])],
    "Cuerpo vale lo que durmió (0.9), NO exporta extra, y no baja el techo de los demás.")
caso("13. EL CARGADO — 5 capas + sueño + sobriedad, todo cumplido justo y ambos opt-ins bien.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo",[JUSTO],KS,1.0),Layer("Conducta",[JUSTO],KB,sobriety_signal(0)),
     Layer("Vínculos",[JUSTO]),Layer("Proyecto",[JUSTO])],
    "El apretón: 2 opt-ins activos, pesos relacionales. Todo justo → En marcha.")
caso("14. MULTI-ADICCIÓN — 3 tracks de sobriedad (alcohol+tabaco+otra), rompió UNO esta semana.",
    [Layer("Interior",[JUSTO]),Layer("Cuerpo",[JUSTO]),Layer("Conducta",[JUSTO],KB,sobriety_signal(1))],
    "1 recaída pega igual tenga 1 o 3 tracks (cuenta recaídas, no tracks). = caso 7.")
print()
