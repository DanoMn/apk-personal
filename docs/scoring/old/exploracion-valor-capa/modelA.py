import math

# ============================================================
# MODELO A — ADITIVO / SEPARACIÓN MÁXIMA DE CANALES
# ============================================================

# ---- Caja negra del ancla (consolidada) -------------------
def R(F, T, mins, gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0, smax=0.5, s0=0.5):
    marked = sorted([m for m in mins if m > 0], reverse=True); D = len(marked)
    if D == 0: return 0.0
    r = [m/T for m in marked]; commit, vol = r[:min(D,F)], r[min(D,F):]
    u = lambda x: min(x, 1.0)**gamma
    phi = sum(u(x) for x in commit)/F; V = sum(u(x) for x in vol)
    base = 1 - (1-phi)*math.exp(-lam_v*V)
    St = sum(max(x-1,0) for x in commit)/F; Sd = V/(7-F) if F < 7 else 0.0
    wt = (F/7)**kappa
    S = smax*(1 - math.exp(-(wt*St + (1-wt)*Sd)/s0))
    return base + (base**p)*S

# ---- PARÁMETROS CALIBRABLES ------------------------------
K_SLEEP   = 1.0    # K del opt-in sueño  (peso relacional)
K_SOBR    = 2.0    # K del opt-in sobriedad (recaída pega más que sueño -> K mayor)
M_BASE    = 0.5    # base de señal de sueño sin datos (axioma 6)
B_RELAPSE = 0.0    # señal de un track de sobriedad con recaída esta semana
S_SUP     = 0.06   # aporte por soporte cumplido (a la BASE), pre-saturación
S_SAT     = 0.12   # techo de saturación del bloque de soportes (base)
T_TASK    = 0.03   # aporte por task con capa cumplida (a la BASE), pre-saturación
T_SAT     = 0.06   # techo de saturación del bloque de tasks (base)
W_EXTRA   = 0.6    # ponderación del canal EXTRA en el estado final
DILUTE_OPTIN = False  # K/(1+K) (False) vs K/(n+K) (True) -- decisión abierta

# ---- EJE (axioma 3): la BASE llena llega COMO MAXIMO a "En marcha" -------
# El estado se compone aditivamente:  state = EM_CEIL*base + W_extra*extra
# base=1 (todo en pie) -> EM_CEIL = tope de En marcha. Plenitud/Inquebrantable
# SOLO via extra. Esto formaliza el eje del dueño sin reglas ni gates.
EM_CEIL = 0.85     # tope de la banda En marcha = techo del canal base solo

# ---- Bandas (sobre el ESTADO final) ----------------------
DELTA = 0.10
def band(state):
    if state < 0.40: return "Rojo"
    if state < 0.62: return "Atencion"
    if state <= EM_CEIL + 1e-9: return "En marcha"   # base llena (=EM_CEIL) cae aca
    if state < 1.0 + DELTA: return "Plenitud"
    return "Inquebrantable"

# ============================================================
# SEÑALES DE OPT-IN
# ============================================================
def sleep_weekly(nights):
    """nights: lista de 7 valores en [0,1] o None. Cobertura + base."""
    have = [n for n in nights if n is not None]
    c = len(have)/7.0
    if c == 0: return M_BASE
    avg = sum(have)/len(have)
    return c*avg + (1-c)*M_BASE

def sobriety_signal(tracks):
    """tracks: lista de bool 'held' (True = se mantuvo). Producto: 1 recaida hunde
    igual con 1 o N tracks; mas recaidas hunden mas. No premia tener mas tracks."""
    if not tracks: return None
    sig = 1.0
    for held in tracks:
        sig *= (1.0 if held else B_RELAPSE)
    return sig

# ============================================================
# SATURACIÓN ADITIVA (soportes / tasks) -- concava, suma con techo
# ============================================================
def saturating_block(n_done, per_unit, ceiling):
    """Aporte aditivo de n unidades cumplidas, satura asintoticamente al techo."""
    if n_done <= 0: return 0.0
    # 1 - exp(-x) saturante; per_unit fija la pendiente inicial
    return ceiling * (1 - math.exp(-(per_unit/ceiling)*n_done))

# ============================================================
# CAPA
# ============================================================
class Layer:
    def __init__(self, name, anchors=None, optin=None, optin_signal=None,
                 supports_done=0, tasks_done=0):
        self.name = name
        self.anchors = anchors or []          # lista de R_i ya calculados (∈[0,1.5])
        self.optin = optin                    # 'sleep' | 'sobriety' | None
        self.optin_signal = optin_signal      # M ∈ [0,1] o None
        self.supports_done = supports_done
        self.tasks_done = tasks_done

    def has_anchors(self): return len(self.anchors) > 0
    def K(self):
        return K_SLEEP if self.optin == 'sleep' else (K_SOBR if self.optin == 'sobriety' else 0.0)

    def size(self):
        """tamaño relacional = 1 (bloque anclas) si activa por anclas, + K si opt-in.
        Capa solo-opt-in (sin anclas): tamaño = K (su unico contenido es la señal)."""
        base_block = 1.0 if self.has_anchors() else 0.0
        return base_block + self.K()

    # ---- canal BASE ∈ [0,1] ----
    def en_pie(self):
        # bloque de anclas: promedio de min(R_i,1)
        if self.has_anchors():
            anchor_base = sum(min(r, 1.0) for r in self.anchors)/len(self.anchors)
        else:
            anchor_base = None

        # término del opt-in, ponderado por K relacional dentro de la capa
        K = self.K()
        if self.optin is not None and self.optin_signal is not None:
            if anchor_base is not None:
                if DILUTE_OPTIN:
                    n = len(self.anchors)
                    w_opt = K/(n+K); w_anch = n/(n+K)
                else:
                    w_opt = K/(1+K); w_anch = 1/(1+K)
                core = w_anch*anchor_base + w_opt*self.optin_signal
            else:
                core = self.optin_signal   # capa solo-opt-in
        else:
            core = anchor_base if anchor_base is not None else 0.0

        # términos chicos aditivos de soportes y tasks (saturados)
        sup = saturating_block(self.supports_done, S_SUP, S_SAT)
        tsk = saturating_block(self.tasks_done, T_TASK, T_SAT)

        return min(core + sup + tsk, 1.0)

    # ---- canal EXTRA ≥ 0 (solo anclas) ----
    def destaco(self):
        if not self.has_anchors(): return 0.0
        return sum(max(r - 1.0, 0.0) for r in self.anchors)/len(self.anchors)

# ============================================================
# AGREGACIÓN GLOBAL
# ============================================================
def score(layers):
    sizes = [L.size() for L in layers]
    total = sum(sizes)
    weights = [s/total for s in sizes]

    base = sum(w*L.en_pie() for w, L in zip(weights, layers))

    # EXTRA: solo entre capas con anclas, ponderado por su peso RE-normalizado
    anchor_layers = [(w, L) for w, L in zip(weights, layers) if L.has_anchors()]
    if anchor_layers:
        wsum = sum(w for w, _ in anchor_layers)
        extra = sum((w/wsum)*L.destaco() for w, L in anchor_layers)
    else:
        extra = 0.0

    # EJE: base llena -> EM_CEIL (techo de En marcha). Plenitud/Inq solo via extra.
    state = EM_CEIL*base + W_EXTRA*extra
    return base, extra, state, weights

def show(title, layers):
    base, extra, state, weights = score(layers)
    print(f"\n=== {title} ===")
    for w, L in zip(weights, layers):
        print(f"  {L.name:12s} peso={w:.3f} en_pie={L.en_pie():.3f} destaco={L.destaco():.3f}")
    print(f"  BASE={base:.4f}  EXTRA={extra:.4f}  ESTADO={state:.4f}  -> {band(state)}")
    return base, extra, state

if __name__ == "__main__":
    print("smoke test")
