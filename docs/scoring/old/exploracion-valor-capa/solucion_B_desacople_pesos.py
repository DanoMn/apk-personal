"""
Solución B — DESACOPLE DE PESOS base/extra.
La capa con opt-in pesa MÁS en el canal BASE (arrastra fuerte, sin tope 1/N),
pero el canal EXTRA (superhabit) se agrega SIEMPRE con pesos iguales (inmune al
peso de arrastre). Fundación = modelo v3 de §1 (estado = base + extra, sin EM_TOP).
Reproducible: python3 solucion_B_desacople_pesos.py
"""
import math

# ── Caja negra del ancla (idéntica a v3/v2, NO se toca) ──
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

# ── Parámetros calibrables ──
DELTA   = 0.10   # holgura de Plenitud → Inquebrantable
K_INT   = 4.0    # peso del opt-in DENTRO de su capa (MODERADO: anclas conservan 20%)
TARGET_DRAG = 0.62  # axioma de arrastre: opt-in en piso (M=0), resto perfecto → borde Atención

def g(M): return M               # curva del opt-in (lineal: FLOOR=0, Q=1)
def sobriety_signal(tracks):
    sig=1.0
    for held in tracks: sig*=(1.0 if held else 0.0)
    return sig

def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")

class Layer:
    def __init__(s,name,anchors=None,M=None):
        s.name=name; s.anchors=anchors or []; s.M=M
    def has(s): return len(s.anchors)>0
    def active(s): return s.has() or (s.M is not None)
    def is_optin(s): return s.M is not None
    def en_pie(s):                       # canal BASE de la capa, ∈[0,1]
        ab=(sum(min(r,1.0) for r in s.anchors)/len(s.anchors)) if s.has() else None
        if s.M is not None:
            core=(ab+K_INT*g(s.M))/(1+K_INT) if ab is not None else g(s.M)
        else:
            core=ab if ab is not None else 0.0
        return min(core,1.0)
    def destaco(s):                      # canal EXTRA de la capa, ≥0, SOLO anclas
        return 0.0 if not s.has() else sum(max(r-1,0) for r in s.anchors)/len(s.anchors)

# ── EL DESACOPLE ──
# DRAG_BASE: peso de capa para una capa-opt-in EN EL CANAL BASE (fijo, NO depende de N).
# Despejado del axioma de arrastre con 3 capas (resto perfecto, opt-in en piso):
#   estado = DRAG·ep_opt(M=0) + (1-DRAG)·1  = 1 - DRAG·(1-ep_opt)
#   ⇒ DRAG = (1 - target)/(1 - ep_opt) ,  ep_opt(M=0)=1/(1+K_INT)
def solve_drag(target):
    ep_opt = (1.0 + K_INT*g(0.0))/(1+K_INT)
    return (1-target)/(1-ep_opt)
DRAG_BASE = solve_drag(TARGET_DRAG)

def base_weights(layers):
    """Pesos del canal BASE. Cada capa-opt-in pesa DRAG_BASE (FIJO, sin 1/N);
       las capas sin opt-in se reparten el resto en partes iguales."""
    N=len(layers)
    optins=[L for L in layers if L.is_optin()]; k=len(optins)
    if k==0 or (N-k)==0:                  # sin opt-in (o todas opt-in) → pesos iguales
        return {id(L):1.0/N for L in layers}
    w_other=(1.0-DRAG_BASE*k)/(N-k)
    return {id(L):(DRAG_BASE if L.is_optin() else w_other) for L in layers}

def score(layers):
    act=[L for L in layers if L.active()]
    wb=base_weights(act)
    base = sum(wb[id(L)]*L.en_pie() for L in act)        # BASE: pesos de arrastre
    al=[L for L in act if L.has()]
    extra= sum(L.destaco() for L in al)/len(al) if al else 0.0   # EXTRA: pesos IGUALES
    return base, extra, base+extra                       # estado = base + extra (fundación v3)

# ════════════════════════════ VERIFICACIÓN ════════════════════════════
if __name__=="__main__":
    ep0=(1.0)/(1+K_INT)
    print("="*86)
    print(f"SOLUCIÓN B — DESACOPLE. K_INT={K_INT} (anclas {1/(1+K_INT)*100:.0f}% / opt-in {K_INT/(1+K_INT)*100:.0f}% dentro de capa)")
    print(f"DRAG_BASE={DRAG_BASE:.4f} (FIJO, no depende de N)  ep_opt(M=0)={ep0:.3f}")
    print(f"bandas: REST<0.40 · ATEN<0.62 · EN MARCHA<0.85 · PLENITUD<1.10 · INQUEBRANTABLE>=1.10")
    print("="*86)
    J=R(4,30,[30]*4); SUP=R(4,30,[30]*6); XL=R(4,30,[60]*7); JI=R(4,30,[30,30,20])
    print(f"J(justo)={J:.3f}  SUP(6d)={SUP:.3f}  XL(60x7)={XL:.3f}  JI(déficit)={JI:.3f}\n")

    def show(t,layers):
        b,e,s=score(layers); print(f"{t:42s} base={b:.3f} extra={e:.3f} estado={s:.3f} -> {band(s)}"); return s

    print("--- 8 CASOS DE PRUEBA ---")
    show("P1 justo+sueño bien N=3",[Layer("I",[J]),Layer("Cu",[J],1.0),Layer("Co",[J])])
    show("P2 mal sueño M=.15 N=3",[Layer("Cu",[J],0.15)]+[Layer(f"c{i}",[J]) for i in range(2)])
    show("P2 mal sueño M=.15 N=5",[Layer("Cu",[J],0.15)]+[Layer(f"c{i}",[J]) for i in range(4)])
    show("P3 recaída M=0 N=3",[Layer("Co",[J],sobriety_signal([False]))]+[Layer(f"c{i}",[J]) for i in range(2)])
    show("P3 recaída M=0 N=5",[Layer("Co",[J],sobriety_signal([False]))]+[Layer(f"c{i}",[J]) for i in range(4)])
    show("P4 sueño regular M=.5 N=5",[Layer("Cu",[J],0.5)]+[Layer(f"c{i}",[J]) for i in range(4)])
    print("\n--- P5 anclas conservan valor (sueño mal M=0.15) ---")
    s1=score([Layer("Cu",[J],0.15)]+[Layer(f"c{i}",[J]) for i in range(2)])[2]
    s3=score([Layer("Cu",[J,J,J],0.15)]+[Layer(f"c{i}",[J]) for i in range(2)])[2]
    epA=Layer("Cu",[J],0.15).en_pie(); ep0c=Layer("Cu",[],0.15).en_pie()
    print(f"   1 ancla estado={s1:.3f} | 3 anclas estado={s3:.3f}  (nº anclas no cambia peso: {abs(s1-s3)<1e-9})")
    print(f"   en_pie con anclas={epA:.3f} vs sin anclas={ep0c:.3f} → anclas aportan {epA-ep0c:.3f} (20% real, NO 6%)")
    print("\n--- P6 Sol vs Tin (superhabit en distinta capa) DEBEN EMPATAR ---")
    sSol=show("   Sol superhabit Interior",[Layer("I",[XL]),Layer("Cu",[J],1.0),Layer("Co",[J])])
    sTin=show("   Tin superhabit Cuerpo+sueño",[Layer("I",[J]),Layer("Cu",[XL],1.0),Layer("Co",[J])])
    print(f"   EMPATAN? {abs(sSol-sTin)<1e-9}  (dif={abs(sSol-sTin):.2e})")
    print()
    show("P7 superhabit repartido 3 capas",[Layer("I",[XL]),Layer("Cu",[XL],1.0),Layer("Co",[XL])])
    b,e,s=score([Layer("I",[J]),Layer("Cu",[],1.0),Layer("V",[J]),Layer("P",[J])])
    print(f"P8 capa solo-opt-in sueño bien            base={b:.3f} extra={e:.3f} estado={s:.3f} -> {band(s)}")

    print("\n--- C2 NEUTRALIDAD (opt-in BIEN no cambia el score) ---")
    sC=score([Layer("I",[J]),Layer("Cu",[J],1.0),Layer("Co",[J])])[2]
    sS=score([Layer("I",[J]),Layer("Cu",[J]),Layer("Co",[J])])[2]
    print(f"   anclas PERFECTAS: con opt-in bien={sC:.4f} | sin={sS:.4f} | neutro={abs(sC-sS)<1e-9}")
    sCd=score([Layer("I",[JI]),Layer("Cu",[JI],1.0),Layer("Co",[JI])])[2]
    sSd=score([Layer("I",[JI]),Layer("Cu",[JI]),Layer("Co",[JI])])[2]
    print(f"   anclas DÉFICIT:   con opt-in bien={sCd:.4f} | sin={sSd:.4f} | dif={abs(sCd-sSd):.3f} (tensión, ver doc)")

    print("\n--- ARRASTRE POR N (debe ser PLANO) ---")
    for N in [3,4,5,6,7]:
        sm=score([Layer("Cu",[J],0.15)]+[Layer(f"c{i}",[J]) for i in range(N-1)])[2]
        print(f"   N={N}: mal sueño={sm:.3f} {band(sm)}")
