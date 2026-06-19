import math, importlib, modelA
importlib.reload(modelA)
from modelA import R, Layer, score, band
import modelA as M

print("="*70)
print("DESPEJE DE K DESDE UN AXIOMA DE ESTADO DEL DUEÑO")
print("="*70)
print("""
Axioma del dueño (D8 + eje): "Anclas perfectas (todo justo, base=1) pero la
semana entera SIN dormir (señal de sueño M=0) debe dejar la capa Cuerpo lo
suficientemente hundida para que el estado global caiga a ATENCION (no quedarse
en En marcha)."

Setup: N=3 capas (Interior, Cuerpo+sueño, Conducta), todas las anclas justas
(en_pie=1 salvo Cuerpo). Cuerpo: anclas en_pie=1, sueño M=0.
  en_pie(Cuerpo) = [1/(1+K)]*1 + [K/(1+K)]*0 = 1/(1+K)
Pesos: size(Cuerpo)=1+K, size(otras)=1.  total = 3+K.
  w_cuerpo = (1+K)/(3+K),  w_otra = 1/(3+K)
BASE = w_cuerpo*[1/(1+K)] + 2*w_otra*1
     = [ (1+K)/(3+K) ]*[1/(1+K)] + 2/(3+K)
     = 1/(3+K) + 2/(3+K) = 3/(3+K)
ESTADO = EM_CEIL * BASE  (sin extra) = 0.85 * 3/(3+K)

Queremos ESTADO = umbral Atencion/En marcha = 0.62  =>  despejamos K:
  0.85 * 3/(3+K) = 0.62
  3/(3+K) = 0.62/0.85 = 0.72941
  3+K = 3/0.72941 = 4.1129
  K = 1.1129
""")
target = 0.62
EM = M.EM_CEIL
K_solved = 3*EM/target - 3
print(f"K despejado (sueño, axioma 'sin dormir -> borde Atencion') = {K_solved:.4f}")

# Verificacion numerica del despeje
M.K_SLEEP = K_solved
def exact(F,T): return R(F,T,[T]*F)
L = [Layer("Interior", anchors=[exact(3,30)]),
     Layer("Cuerpo",   anchors=[exact(5,20)], optin='sleep', optin_signal=0.0),
     Layer("Conducta", anchors=[exact(4,40)])]
b,e,s,w = score(L)
print(f"VERIFICA: estado con K={K_solved:.4f} y sueño=0 -> {s:.4f} ({band(s)})")
print(f"  (objetivo 0.62 = borde Atencion/En marcha)  match: {abs(s-0.62)<1e-3}")

print("\n--- Para sobriedad: axioma D8 (recaida pega MAS que mal sueño) ---")
print("""
Axioma: "Una recaida (M=0) en Conducta debe hundir MAS que el peor sueño.
Concretamente: recaida con anclas perfectas -> estado ROJO (< 0.40)."
Mismo algebra con K_sobr:  ESTADO = 0.85 * 3/(3+K_sobr) = 0.40
""")
target_r = 0.40
K_sobr_solved = 3*EM/target_r - 3
print(f"K despejado (sobriedad, 'recaida -> Rojo') = {K_sobr_solved:.4f}")
M.K_SOBR = K_sobr_solved
from modelA import sobriety_signal
L = [Layer("Interior", anchors=[exact(3,30)]),
     Layer("Cuerpo",   anchors=[exact(5,20)]),
     Layer("Conducta", anchors=[exact(4,40)], optin='sobriety',
           optin_signal=sobriety_signal([False]))]
b,e,s,w = score(L)
print(f"VERIFICA: recaida con K_sobr={K_sobr_solved:.4f} -> {s:.4f} ({band(s)})")
print(f"  K_sobr ({K_sobr_solved:.2f}) > K_sleep ({K_solved:.2f}): recaida pega mas -> D8 OK")

print("\n--- Superhabit fuerte alcanza Inquebrantable? ---")
M.K_SLEEP = K_solved; M.K_SOBR = K_sobr_solved
def sup(F,T,ex): return R(F,T,[T+ex]*F)
L = [Layer("Interior", anchors=[sup(7,30,60)]),       # 7 dias, doble de tiempo
     Layer("Cuerpo",   anchors=[sup(7,20,40)], optin='sleep', optin_signal=1.0),
     Layer("Conducta", anchors=[sup(7,40,80)], optin='sobriety', optin_signal=sobriety_signal([True]))]
b,e,s,w = score(L)
print(f"Superhabit fuerte y repartido -> base={b:.3f} extra={e:.3f} estado={s:.4f} ({band(s)})")
