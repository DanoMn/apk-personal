# Núcleo matemático del scoring — v1 (completo, definitivo)

> **Estado: ✅ VIGENTE (2026-06-16).** El modelo matemático COMPLETO de punta a punta — el núcleo del
> scoring. Toda la matemática junta, sin reduccionismo. Es la implementación precisa del contrato
> `axiomas-modelo-scoring-v1.md` (27/27 verde, `verificacion_modelo_oficial.py`). El doc oficial
> (`modelo-scoring-oficial-v1.md`) es la descripción de alto nivel + filosofía + ejemplos; ESTE es el
> detalle matemático exacto. Proyecto: `apk-personal`.

---

## 0. El pipeline (de hechos a puntos)

```
hechos de la semana (ventana móvil 7 días)
  └─ NIVEL 1: por cada ancla → R(F,T,mins) ∈ [0, 1.5]         (cumplimiento + superhabit, gate base²)
  └─ NIVEL 2: por cada capa → base_capa, extra_capa            (dos canales)
        ├─ soportes: blend en base_capa
        └─ tasks: aporte efímero al extra_capa (saturación conjunta)
  └─ NIVEL 3: peso de cada capa (votos por anclas)
  └─ NIVEL 4: opt-ins (sueño/sobriedad) → señal M + término-sombra
  └─ NIVEL 5: AGREGACIÓN bolsa-global → base_global, extra_global → ESTADO ∈ [0, 1.5]
  └─ NIVEL 6: ESTADO → BANDA (Restauración…Inquebrantable)
  └─ NIVEL 7: ESTADO → PUNTOS ∈ [650, 1100]  (dashboard)
```

## 0.1 Constantes (todas, con origen)

| Símbolo | Valor | Nivel | Origen |
|---|---|---|---|
| `γ` (gamma) | 1.5 | ancla | mata trivialidad del valor-día |
| `λ_v` (lambda_v) | 0.5 | ancla | fuerza reparadora del voluntario |
| `κ` (kappa) | 1.5 | ancla | desplazamiento tiempo↔días con F |
| `p` | 2.0 | ancla | dureza del gate "base completa" |
| `smax` | 0.5 | ancla | techo del superhabit por ancla |
| `s0` | 0.5 | ancla | escala de saturación del superhabit |
| `WS` | 0.07 | soportes | peso del blend del soporte |
| `TAU` | 0.06 | tasks | techo del aporte de tasks por capa |
| `N0` | 1.0 | tasks | saturación por conteo de tasks |
| `r` | 0.5 | peso capa | decrecimiento del voto por ancla |
| `ρ` (rho) | 0.15 | peso capa | peso de una capa solo-soportes |
| `W0` | 1.0 | peso capa | peso base (1 ancla, o capa solo-opt-in) |
| `BETA` | 0.818 | opt-ins | intensidad del término-sombra (despejado de TARGET=0.55) |
| `A` | 0.55 | opt-ins | golpe por día de recaída (sobriedad) |
| `B_SLEEP` | 0.5 | opt-ins | señal de sueño sin dato |
| `δ` (delta) | 0.10 | bandas | margen de Inquebrantable (1+δ) |
| piso/tope puntos | 650 / 1100 | puntos | mapeo E |

---

## NIVEL 1 — El ancla: `R(F, T, mins)`

Una ancla tiene dos metas: **frecuencia** `F` (días/semana, 2–7) y **tiempo** `T` (min/sesión). `mins` =
lista de minutos hechos por día en la semana. Produce `R ∈ [0, 1.5]`.

### 1.1 Preprocesamiento Best-F (todo porcentual)
```
r_i = t_i / T                          razón de tiempo de cada día con actividad (t_i>0)
Ordenar r descendente. D = nº de días con actividad.
commit = los  min(D, F)  mejores días     (los que cuentan para el compromiso)
vol    = los  D − F  restantes            (días voluntarios, si D > F)
Si D = 0  ⟹  R = 0.                       (piso cero, AN2)
```

### 1.2 Las piezas
```
u(r)  = min(r, 1)^γ                                   valor-día (γ=1.5: un día cortísimo casi no cuenta)

φ     = (1/F) · Σ_{commit}  u(r_i)                    BASE de compromiso ∈ [0,1]
                                                      (los F slots; un día faltante = 0 que el tiempo NO rellena)

V     = Σ_{vol}  u(r_j)                               días-equivalentes voluntarios ≥ 0

base  = 1 − (1 − φ) · exp(−λ_v · V)                   el voluntario REPARA el déficit con retornos
                                                      decrecientes; nunca pasa de 1            (∈ [0,1])

St    = (1/F) · Σ_{commit}  max(r_i − 1, 0)           superhabit de TIEMPO (te pasaste de minutos)
Sd    = V / (7 − F)   (0 si F = 7)                    superhabit de DÍAS (fracción del espacio libre usado)

wt    = (F/7)^κ                                       peso del tiempo extra (crece con F)
S     = smax · (1 − exp(−(wt·St + (1−wt)·Sd) / s0))   superhabit saturado, techo smax=0.5

R     = base  +  base^p · S                           gate base²: sin cimiento no hay gloria   (∈ [0, 1.5])
```

### 1.3 Lectura
- **Frecuencia domina (estructural):** dividir por `F` y dejar slots vacíos en 0 hace que ningún día
  largo compense un día faltante. (P1)
- **Gate `base^p` (p=2):** el superhabit se multiplica por `base²`; con base incompleta, el exceso casi
  no rinde — primero el cimiento. (P2)
- **Dos vías de superhabit:** tiempo (`St`) y días (`Sd`), fundidas por `wt=(F/7)^κ`. En F=7 (`Sd=0`,
  `wt=1`) el tiempo hereda todo el peso; en F bajo, los días extra pesan más.
- **Techo:** `R ≤ 1 + smax = 1.5`. El superhabit no explota (no rompe el promedio de capa).

### 1.4 Casos de referencia (verificados)
`F=3,T=30,[30,30,30]`→**1.000** · `D=0`→**0** · `4×60 (meta 4×30)`→**1.289** (tiempo) ·
`6×30 (meta 4×30)`→**1.266** (días) · `2/4 días×60`→**0.544** (gate: sin frecuencia, el exceso no cuenta) ·
`[30×7]/[45×7]/[120×7]`→**1.0/1.32/1.499** (acotado en 1.5).

---

## NIVEL 2 — Valor de capa (dos canales)

Una capa tiene `n` anclas con valores `R_1..R_n`.
```
base_anclas = (1/n) · Σ  min(R_i, 1)        ∈ [0,1]    "¿está en pie?"
extra_capa  = (1/n) · Σ  max(R_i − 1, 0)     ∈ [0,0.5]  "¿se destacó?"  (SOLO anclas)
```
- Las anclas pesan **igual** dentro de la capa (promedio simple). Brillar/fallar en 1 de `n` se diluye
  (cuenta `1/n`): la capa brilla cuando brilla en todas (Forma 1).

### 2.1 Soportes — blend leve en la base
`m` soportes en la capa, cada uno con `días_sostenidos_i` (ventana indulgente de 4 días):
```
s_i  = min(días_sostenidos_i / 4, 1)                  señal por soporte (4 días = 100%)
G    = (1/m) · Σ s_i                                  señal de bloque (PROMEDIO: no crece con la cantidad)
base_eff = (1 − WS)·base_anclas + WS·G    (WS=0.07)   blend bidireccional leve
         = G                                           si la capa NO tiene anclas (la señal ES la base)
```
- Sin registro del día = cumplido (cero fricción / UX inversa). Solo base, nunca extra.

### 2.2 Tasks — aporte efímero al extra (saturación conjunta)
`n_hoy` = tasks completadas HOY con capa (se resetea cada día):
```
su_anc      = −s0 · ln(1 − extra_capa / smax)              invierte la exp del superhabit (surplus crudo)
g_task      = 1 − exp(−n_hoy / N0)             (N0=1.0)    saturación por conteo
THETA       = −s0 · ln(1 − TAU / smax)         (TAU=0.06)  presupuesto pre-saturación (da lift máx = TAU)
extra_joint = smax · (1 − exp(−(su_anc + THETA·g_task)/s0))   re-satura por la MISMA curva (techo 0.5)
task_lift   = (extra_joint − extra_capa) · base_eff²       gate base²: sin cimiento no aporta
extra_final = extra_capa + task_lift
```
- Comparte la curva del superhabit de anclas (respeta el techo 0.5). Nunca resta. Task neutral/sin capa
  no cuenta. El tope (~+0.06/capa) **emerge** de la saturación, no es una regla.

---

## NIVEL 3 — Peso de capa (votos por anclas)

```
peso_capa(n) = Σ_{k=0}^{n−1} r^k = (1 − r^n)/(1 − r)   (r=0.5, n≥1)
             → 1:1.00 · 2:1.50 · 3:1.75 · 4:1.875 · … · techo (1/(1−r)) = 2.0
peso_capa(0) = ρ = 0.15                                capa SOLO-soportes (peso reducido)
capa solo-opt-in (sin anclas)  →  peso = W0 = 1.0      (O11: el opt-in es sustancia real)
```
- Más anclas = capa más importante = pesa más, con freno (cada ancla nueva suma la mitad). El techo 2.0
  garantiza que **ninguna capa decide más del 50%** del score (peor caso, mínimo 3 capas).

---

## NIVEL 4 — Opt-ins: sueño y sobriedad

### 4.1 Señales `M ∈ [0,1]`
```
Sueño  (Cuerpo):     M_sleep = c · avg(noches con dato) + (1−c) · B_SLEEP
                     c = nº noches con dato / 7 ;  sin dato → M = B_SLEEP = 0.5
                     (4 componentes/noche: duración, continuidad, horario, interrupción digital)
Sobriedad (Conducta): M_sobr = Π_{tracks} (1 − A)^(días de recaída del track)   (A=0.55)
                     track limpio (0 recaídas) → 1 (invisible, no diluye)
```

### 4.2 Término-sombra (independiente, en la bolsa-global)
```
w_optin = BETA · Σpesos · (1 − M)            BETA=0.818,  Σpesos = suma de pesos de TODAS las capas
```
- `M=1` → `w=0`: invisible (neutralidad exacta, incluso con anclas en déficit).
- Escala con `Σpesos` (generaliza el `BETA·N` de v4; `Σpesos=N` si los pesos son iguales) → arrastre
  **plano** (recaída total + anclas perfectas → 0.55) con cualquier config, y **global** (arrastra igual
  esté el opt-in en una capa pesada o liviana).
- Solo base, nunca extra. Dos opt-ins malos **componen** sus arrastres (sin tope). Ventana de 7 días.
- **Anti-incentivo aceptado (O12):** activar un opt-in solo puede **empatar o bajar** el estado, nunca
  subirlo — es opt-in por diseño (decisión del dueño; el sueño además es telemetría automática).

---

## NIVEL 5 — Agregación: la bolsa-global

La base es **UNA bolsa de términos ponderados** (no un promedio de valores de capa aislados):
```
términos =
   por cada capa con anclas:   (base_eff_capa,  peso_capa(n))
   por cada capa solo-soportes: (G,  ρ)
   por cada capa solo-opt-in:   (M,  W0=1)
   por cada opt-in activo:      (M,  BETA·Σpesos·(1−M))

base_global  = Σ(valor · peso) / Σ(peso)                  sobre TODOS los términos
extra_global = (1/k) · Σ extra_final_capa                 promedio PLANO, k = capas con anclas  (Sol=Tin)
ESTADO       = min(base_global, 1) + extra_global          ∈ [0, ~1.5]
```
- **Base ponderada** por peso de capa (cumplir en capa pesada cuenta más); **extra plano** (un superhabit
  rinde igual en cualquier capa). Las dos cuentas se suman.

---

## NIVEL 6 — Bandas (estado)

```
ESTADO < 0.40                 → Restauración
0.40 ≤ ESTADO < 0.62          → Atención
0.62 ≤ ESTADO < 0.85          → En marcha
0.85 ≤ ESTADO < 1.10          → Plenitud      (1.10 = 1 + δ, δ=0.10)
ESTADO ≥ 1.10                 → Inquebrantable
```
- Cumplir todo justo = ESTADO 1.0 (cae DENTRO de Plenitud, en zona alta; Plenitud entra en 0.85).
- Sin gates/caps/worst-term duros: el estado EMERGE del agregado.

---

## NIVEL 7 — Mapeo a puntos visibles (enfoque E)

```
σ(x) = 1 / (1 + e^−x)
hitos i (centro cᵢ, ancho wᵢ, aporte Aᵢ):
   (0.18, 0.10, 60) · (0.55, 0.11, 110) · (0.83, 0.09, 100) · (1.07, 0.055, 130) · (1.35, 0.13, 50)
raw(e)   = 650 + Σᵢ Aᵢ · σ((e − cᵢ)/wᵢ)
PUNTOS(e) = 650 + (raw(e) − raw(0)) · (1100 − 650) / (raw(1.5) − raw(0))      e = ESTADO ∈ [0,1.5]
```
- Suma de rampas logísticas (suave, sin codos): la resolución se aprieta justo antes de cada número-meta.
- Piso 650 (digno), tope 1100 (respira sobre 1000). Hitos: 0→650 · 0.40→721 · 0.62→788 · 0.85→873 ·
  **1.0→941** (cumplir-justo) · **1.10→1011** (el 1000 se gana al entrar a Inquebrantable) · 1.5→1100.
- Continuo y monótono; el número se mueve de a 1 punto.

---

## Implementación de referencia (Python — el modelo entero, ejecutable)

```python
import math
G_=1.5; LV=0.5; KP=1.5; P=2.0; SMAX=0.5; S0=0.5          # ancla
WS=0.07; TAU=0.06; N0=1.0                                 # soportes/tasks
RG=0.5; RHO=0.15; W0=1.0                                  # peso de capa
BETA=0.818; A=0.55; B_SLEEP=0.5; DELTA=0.10               # opt-ins/bandas

def R(F,T,mins):                                          # NIVEL 1
    if F<=0 or T<=0: return 0.0                           # entradas ilegales por contrato → degradan a 0
    mk=sorted([m for m in mins if m>0],reverse=True); D=len(mk)
    if D==0: return 0.0
    r=[m/T for m in mk]; c,v=r[:min(D,F)],r[min(D,F):]
    u=lambda x:min(x,1.0)**G_
    phi=sum(u(x) for x in c)/F; V=sum(u(x) for x in v)
    base=1-(1-phi)*math.exp(-LV*V)
    St=sum(max(x-1,0) for x in c)/F; Sd=V/(7-F) if F<7 else 0.0
    wt=(F/7)**KP; S=SMAX*(1-math.exp(-(wt*St+(1-wt)*Sd)/S0))
    return base+(base**P)*S

def votos(n): return RHO if n==0 else (1-RG**n)/(1-RG)    # NIVEL 3
def sob(dias_por_track): 
    m=1.0
    for d in dias_por_track: m*=(1-A)**d
    return m                                              # NIVEL 4 (sobriedad)

def estado(capas):                                        # NIVELES 2+4+5
    clampM=lambda M: None if M is None else min(max(M,0.0),1.0)        # M ∈ [0,1] (defensivo)
    sigG =lambda sup: sum(min(max(d,0)/4,1) for d in sup)/len(sup)     # señal soporte (días ≥ 0)
    info=[]
    for c in capas:
        aR=c.get("anclas",[]); n=len(aR); sup=c.get("sup_days"); M=clampM(c.get("optin")); nt=c.get("n_tasks",0)
        if aR:
            ab=sum(min(r,1) for r in aR)/n; ex=sum(max(r-1,0) for r in aR)/n
            be=(1-WS)*ab+WS*sigG(sup) if sup else ab
            be=min(max(be,0),1)
            if nt>0:                                       # tasks (saturación conjunta)
                su=-S0*math.log(1-ex/SMAX) if ex<SMAX else 1e9
                TH=-S0*math.log(1-TAU/SMAX); g=1-math.exp(-nt/N0)
                ex=ex+(SMAX*(1-math.exp(-(su+TH*g)/S0))-ex)*be**P     # gate base^P (no hardcodear 2)
            info.append(("anc",be,votos(n),ex,M))
        elif sup:
            info.append(("sop",sigG(sup),RHO,0.0,M))
        elif M is not None:
            info.append(("opt",M,W0,0.0,None))
    if not info: return 0.0                                # sin capas activas (contrato: mín 3) → degrada a 0
    Sig=sum(i[2] for i in info)
    terms=[(i[1],i[2]) for i in info]
    for t,val,pe,ex,M in info:
        if t=="anc" and M is not None:
            w=BETA*Sig*(1-M)
            if w>1e-12: terms.append((M,w))
    base=sum(v*w for v,w in terms)/sum(w for _,w in terms)
    exs=[i[3] for i in info if i[0]=="anc"]
    extra=sum(exs)/len(exs) if exs else 0.0
    return min(base,1)+extra

def banda(e):                                             # NIVEL 6
    return ("Restauración" if e<0.40 else "Atención" if e<0.62 else "En marcha"
            if e<0.85 else "Plenitud" if e<1+DELTA else "Inquebrantable")

_H=[(0.18,0.10,60),(0.55,0.11,110),(0.83,0.09,100),(1.07,0.055,130),(1.35,0.13,50)]
def _raw(e): return 650+sum(Aa/(1+math.exp(-(e-cc)/ww))*1 for cc,ww,Aa in _H)
_r0,_r15=_raw(0.0),_raw(1.5)
def PUNTOS(e):                                            # NIVEL 7
    e=max(0,min(1.5,e)); return 650+(_raw(e)-_r0)*450/(_r15-_r0)
```

> Verificación de este núcleo: `verificacion_modelo_oficial.py` (27/27 axiomas verdes, 2026-06-16).

---

## Pendientes (fuera del núcleo cerrado)
- Estabilidad temporal multi-semana (`arbol §15`) — ortogonal, sin reconciliar.
- Calibración fina contra más marcas del dueño (los valores son despejados de axiomas, afinables).
- Implementación en código de la app (el código actual es el modelo VIEJO/deuda — no copiar sus constantes).
