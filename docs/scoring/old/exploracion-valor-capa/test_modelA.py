import importlib, modelA
importlib.reload(modelA)
from modelA import R, Layer, score, band, sleep_weekly, sobriety_signal, show
import modelA as M

print("="*70)
print("MODELO A -- VERIFICACION DE 12 CASOS LIMITE")
print(f"Params: K_sleep={M.K_SLEEP} K_sobr={M.K_SOBR} W_extra={M.W_EXTRA} "
      f"M_base={M.M_BASE} S_sup={M.S_SUP}/{M.S_SAT} T_task={M.T_TASK}/{M.T_SAT} delta={M.DELTA}")
print("="*70)

# Anclas "justas": R=1.0 exacto (F dias, T min, exactos)
def exact(F, T): return R(F, T, [T]*F)
def surplus(F, T, extra_min): return R(F, T, [T+extra_min]*F)

# --- CASO 1: todo justo -> EN MARCHA (no Plenitud) ---
L = [Layer("Interior", anchors=[exact(3,30)]),
     Layer("Cuerpo",   anchors=[exact(5,20)], optin='sleep', optin_signal=1.0),
     Layer("Conducta", anchors=[exact(4,40)], optin='sobriety', optin_signal=sobriety_signal([True]))]
b,e,s = show("CASO 1: todo justo (R=1, opt-ins OK)", L)
print("   ESPERA: En marcha (base lleva como max a En marcha)  ->", "OK" if band(s)=="En marcha" else "FALLA")

# --- CASO 2: superhabit repartido -> Plenitud/Inquebrantable ---
L = [Layer("Interior", anchors=[surplus(3,30,30)]),
     Layer("Cuerpo",   anchors=[surplus(5,20,20)], optin='sleep', optin_signal=1.0),
     Layer("Conducta", anchors=[surplus(4,40,40)], optin='sobriety', optin_signal=sobriety_signal([True]))]
b,e,s = show("CASO 2: superhabit repartido", L)
print("   ESPERA: Plenitud o Inquebrantable ->", "OK" if band(s) in ("Plenitud","Inquebrantable") else "FALLA")

# --- CASO 3: capa solo opt-in (D4): valor = señal; no exporta extra ---
L = [Layer("Interior", anchors=[exact(3,30)]),
     Layer("Cuerpo",   anchors=[exact(5,20)]),
     Layer("Conducta", optin='sobriety', optin_signal=sobriety_signal([True]))]  # SIN anclas
csob = L[2]
b,e,s = show("CASO 3: Conducta solo-sobriedad (sin anclas)", L)
print(f"   en_pie(Conducta)={csob.en_pie():.3f} destaco(Conducta)={csob.destaco():.3f}")
print("   ESPERA: valor=señal(1.0), destaco=0 ->",
      "OK" if abs(csob.en_pie()-1.0)<1e-9 and csob.destaco()==0 else "FALLA")

# --- CASO 4: el apreton: 3 capas + AMBOS opt-ins -> pesos suman 1 ---
L = [Layer("Interior", anchors=[exact(3,30)]),
     Layer("Cuerpo",   anchors=[exact(5,20)], optin='sleep', optin_signal=1.0),
     Layer("Conducta", anchors=[exact(4,40)], optin='sobriety', optin_signal=sobriety_signal([True]))]
b,e,s,w = score(L)
show("CASO 4: 3 capas + ambos opt-ins (pesos)", L)
print(f"   suma pesos = {sum(w):.6f} ->", "OK" if abs(sum(w)-1.0)<1e-9 else "FALLA")

# --- CASO 5: mal sueño hunde SU capa pero no el extra de otras ---
extra_anchor = surplus(3,30,30)  # ancla con superhabit
L_good = [Layer("Interior", anchors=[extra_anchor]),
          Layer("Cuerpo",   anchors=[exact(5,20)], optin='sleep', optin_signal=1.0),
          Layer("Conducta", anchors=[exact(4,40)], optin='sobriety', optin_signal=sobriety_signal([True]))]
L_bad  = [Layer("Interior", anchors=[extra_anchor]),
          Layer("Cuerpo",   anchors=[exact(5,20)], optin='sleep', optin_signal=0.3),  # mal sueño
          Layer("Conducta", anchors=[exact(4,40)], optin='sobriety', optin_signal=sobriety_signal([True]))]
bg,eg,sg = show("CASO 5a: sueño OK", L_good)
bb,eb,sb = show("CASO 5b: sueño MALO (0.3)", L_bad)
print(f"   extra igual? {eg:.4f}=={eb:.4f} ->", "OK" if abs(eg-eb)<1e-9 else "FALLA")
print(f"   base bajo? {bb:.4f}<{bg:.4f} ->", "OK" if bb<bg else "FALLA")

# --- CASO 6: sin dato de sueño -> base, no piso ---
nights_none = [None]*7
m_nodata = sleep_weekly(nights_none)
L = [Layer("Interior", anchors=[exact(3,30)]),
     Layer("Cuerpo",   anchors=[exact(5,20)], optin='sleep', optin_signal=m_nodata),
     Layer("Conducta", anchors=[exact(4,40)])]
b,e,s = show("CASO 6: sueño sin dato (M_base)", L)
print(f"   M sin dato = {m_nodata:.3f} ->", "OK" if abs(m_nodata-M.M_BASE)<1e-9 else "FALLA")

# --- CASO 7: recaida dentro de 7d penaliza; fuera no ---
sig_in  = sobriety_signal([False])  # recaida esta semana
sig_out = sobriety_signal([True])   # recaida vieja -> el pipeline entrega held=True
L_in  = [Layer("Conducta", anchors=[exact(4,40)], optin='sobriety', optin_signal=sig_in),
         Layer("Interior", anchors=[exact(3,30)]), Layer("Cuerpo", anchors=[exact(5,20)])]
L_out = [Layer("Conducta", anchors=[exact(4,40)], optin='sobriety', optin_signal=sig_out),
         Layer("Interior", anchors=[exact(3,30)]), Layer("Cuerpo", anchors=[exact(5,20)])]
bi,_,si = show("CASO 7a: recaida DENTRO 7d", L_in)
bo,_,so = show("CASO 7b: recaida FUERA 7d", L_out)
print(f"   dentro<fuera? {si:.4f}<{so:.4f} ->", "OK" if si<so else "FALLA")

# --- CASO 8: multi-sobriedad: 1 recaida entre 3 ~ entre 1 ---
sig_1of1 = sobriety_signal([False])
sig_1of3 = sobriety_signal([False, True, True])
print(f"\n=== CASO 8: multi-sobriedad ===")
print(f"   señal 1 recaida/1 track = {sig_1of1:.3f}")
print(f"   señal 1 recaida/3 track = {sig_1of3:.3f}")
print("   ESPERA: iguales (no se diluye) ->", "OK" if abs(sig_1of1-sig_1of3)<1e-9 else "FALLA")
sig_2of3 = sobriety_signal([False, False, True])
print(f"   señal 2 recaidas/3 = {sig_2of3:.3f} (mas recaidas hunden mas) ->",
      "OK" if sig_2of3 <= sig_1of3 else "FALLA")

# --- CASO 9: soportes full vs descuidados (light, satura) ---
# Anclas sub-perfectas para que la base NO este clipeada en 1 (asi soportes mueven).
sub_anchor = R(4,40,[40,40,40])   # 3 de 4 dias -> base < 1
base_anchor = sub_anchor
L_none = [Layer("Conducta", anchors=[base_anchor], supports_done=0),
          Layer("Interior", anchors=[exact(3,30)]), Layer("Cuerpo", anchors=[exact(5,20)])]
L_full = [Layer("Conducta", anchors=[base_anchor], supports_done=3),
          Layer("Interior", anchors=[exact(3,30)]), Layer("Cuerpo", anchors=[exact(5,20)])]
L_many = [Layer("Conducta", anchors=[base_anchor], supports_done=8),
          Layer("Interior", anchors=[exact(3,30)]), Layer("Cuerpo", anchors=[exact(5,20)])]
bn,_,sn = show("CASO 9a: soportes 0", L_none)
bf,_,sf = show("CASO 9b: soportes 3", L_full)
bm,_,sm = show("CASO 9c: soportes 8 (satura)", L_many)
print(f"   mueve light? delta 0->3 = {sf-sn:.4f} (chico, no salta banda) ->",
      "OK" if 0 < (sf-sn) < 0.10 else "FALLA")
print(f"   satura? aporte 3->8 = {sm-sf:.4f} < aporte 0->3 = {sf-sn:.4f} ->",
      "OK" if (sm-sf) < (sf-sn) else "FALLA")

# --- CASO 10: tasks aportan menos que soportes ---
L_sup = [Layer("Conducta", anchors=[base_anchor], supports_done=1),
         Layer("Interior", anchors=[exact(3,30)]), Layer("Cuerpo", anchors=[exact(5,20)])]
L_tsk = [Layer("Conducta", anchors=[base_anchor], tasks_done=1),
         Layer("Interior", anchors=[exact(3,30)]), Layer("Cuerpo", anchors=[exact(5,20)])]
_,_,s_sup = score(L_sup)[2], None, score(L_sup)[2]
b_s = score(L_sup)[0]; b_t = score(L_tsk)[0]
print(f"\n=== CASO 10: task < soporte ===")
print(f"   aporte 1 soporte (a base) = {b_s - bn:.4f}")
print(f"   aporte 1 task    (a base) = {b_t - bn:.4f}")
print("   ESPERA: task < soporte y task>0 ->",
      "OK" if 0 < (b_t-bn) < (b_s-bn) else "FALLA")

# --- CASO 11: mas anclas NO cambia el peso de la capa ---
L_1a = [Layer("Cuerpo", anchors=[exact(5,20)], optin='sleep', optin_signal=1.0),
        Layer("Interior", anchors=[exact(3,30)]), Layer("Conducta", anchors=[exact(4,40)])]
L_3a = [Layer("Cuerpo", anchors=[exact(5,20),exact(5,20),exact(5,20)], optin='sleep', optin_signal=1.0),
        Layer("Interior", anchors=[exact(3,30)]), Layer("Conducta", anchors=[exact(4,40)])]
w1 = score(L_1a)[3][0]; w3 = score(L_3a)[3][0]
print(f"\n=== CASO 11: mas anclas, mismo peso ===")
print(f"   peso Cuerpo 1 ancla = {w1:.4f}  3 anclas = {w3:.4f} ->",
      "OK" if abs(w1-w3)<1e-9 else "FALLA")

# --- CASO 12: N=3,4,5 -> peso del opt-in baja al crecer N ---
print(f"\n=== CASO 12: peso del opt-in baja con N ===")
def weight_optin(N):
    layers = [Layer("Cuerpo", anchors=[exact(5,20)], optin='sleep', optin_signal=1.0)]
    extras = ["Interior","Conducta","Vinculos","Proyecto"]
    for i in range(N-1):
        layers.append(Layer(extras[i], anchors=[exact(3,30)]))
    w = score(layers)[3]
    return w[0]
prev = None; ok = True
for N in (3,4,5):
    wo = weight_optin(N)
    print(f"   N={N}: peso Cuerpo(opt-in) = {wo:.4f}")
    if prev is not None and wo >= prev: ok = False
    prev = wo
print("   ESPERA: baja monotono ->", "OK" if ok else "FALLA")

print("\n" + "="*70)
print("FIN")
