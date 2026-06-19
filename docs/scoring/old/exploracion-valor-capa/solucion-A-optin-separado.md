# Solución A — Opt-in como CIMIENTO global separado (no toca las anclas)

> Propuesta a ciegas (un proponente de tres). Sesgo asignado: **el opt-in es un sistema
> SEPARADO que NO se mezcla dentro de la capa con las anclas.** Resuelve el trilema del
> arrastre (`problema-arrastre-optin-v1.md`). Verificación python3 real al final, salida pegada.

---

## 1. El modelo completo

### Intuición del diseño

Las anclas viven su vida. Caminar nunca se diluye, leer nunca se diluye: cada capa calcula
su **base** (promedio truncado de sus anclas, ∈[0,1]) y su **extra** (superhabit, ≥0) como
siempre, con pesos de capa **iguales** (1/N). El opt-in (sueño/sobriedad) **no entra a
ninguna capa**: es un **cimiento** (`C`) — una señal de otra naturaleza que se agrega aparte
y **modula multiplicativamente el canal base agregado**.

La clave del sesgo: como `C` multiplica al **agregado completo** (y no a una sola capa de
N), su arrastre **no se diluye con N**. Da igual que haya 3 o 5 capas: dormir mal te baja lo
mismo. El cimiento está roto → toda la casa se inclina, sin importar cuántas habitaciones
tenga. Y como `C` **solo toca el canal base** (jamás el extra), el superhabit nunca se
distorsiona: un superhabit en Cuerpo+sueño vale exactamente lo mismo que en Interior.

### Fórmulas y rangos

**Caja negra del ancla** `R(F,T,mins,…)` — idéntica a v3, devuelve [0, ~1.5]. No se toca.

**Base y extra por capa con anclas** (el opt-in NO participa):

```
base_capa_i  = (1/k_i) · Σ_j min(r_ij, 1)        ∈ [0, 1]   (r_ij = R de la ancla j)
extra_capa_i = (1/k_i) · Σ_j max(r_ij − 1, 0)    ∈ [0, ~0.5]
```

`k_i` = nº de anclas de la capa (las anclas promedian a un bloque; nº de anclas NO cambia el
peso de capa — axioma 4).

**Capa solo-opt-in** (sin anclas, axioma 6): su valor base = la señal del opt-in; extra = 0.

**Señal de cimiento** `C` (el sistema separado):

```
g(m) = m^GAMMA_C                                  m ∈ [0,1] = señal del opt-in
C    = 1                       si NO hay opt-in activo            (NEUTRO — axioma 7)
C    = FLOOR_C + (1−FLOOR_C)·g( min(m_1,…,m_t) )   si hay opt-ins  ∈ [FLOOR_C, 1]
```

- `m` = señal del opt-in: **sueño** continuo [0,1] (telemetría, 4 comp; sin dato → base
  `B_SLEEP`, no 0 — axioma 5); **sobriedad** binaria por track held/broke, producto sobre
  tracks (1 recaída → 0; no se diluye con más tracks — axioma 5).
- `min(…)` sobre todas las señales de opt-in activas: **el peor opt-in manda** (dormir bien
  no compensa una recaída). Esto preserva el axioma de sobriedad multi-track sin diluir.
- `C = 1` cuando todos los opt-ins están bien (m=1) → **neutro** (axioma 7): el opt-in bien
  no sube ni baja respecto a no tenerlo.

**Score (agregación final):**

```
base_anc = (1/N) · Σ_i base_capa_i            (pesos de capa IGUALES, N = capas activas)
base     = base_anc · C                        ← el cimiento modula SOLO el canal base
extra    = (1/M) · Σ_i extra_capa_i            (pesos iguales; el opt-in NO lo toca; M = capas con anclas)
estado   = base + extra
```

Bandas (sin cambios): `R<0.40 · A<0.62 · EM<0.85 · P∈[0.85,1.10) · I≥1.10`.

### Por qué cada axioma se respeta

1. **Pesos puros, cero gates/caps/worst-term/min duro sobre el score.** El `min` está
   adentro de la señal de cimiento (es la definición de "peor opt-in manda", una agregación
   de la señal, no un gate sobre el score). El score sigue siendo `agregación → bandas`.
2. **Dos canales.** base = `base_anc · C` (≤1, porque base_anc≤1 y C≤1). extra = solo anclas,
   intacto. El opt-in **nunca** genera extra: `C` solo multiplica base.
3. **Eje.** Cumplir justo (R=1) + opt-in bien (C=1) → base=1, extra=0 → estado=1.0 = Plenitud.
   Superhabit repartido → extra alto → Inquebrantable. Bandas idénticas.
4. **Nº de anclas no cambia el peso de capa.** Las anclas promedian a `base_capa_i` antes de
   entrar a la media de N capas.
5. **Sueño continuo / sobriedad binaria multi-track.** Modeladas en `m` y en `min(…)`.
6. **Capa solo-opt-in posible.** Aporta su señal como base propia.
7. **Opt-in bien = neutro.** `m=1 → C=1 → base = base_anc · 1`.

---

## 2. Justificación del sesgo (sistema separado)

El trilema de v3 nace de **mezclar** el opt-in dentro de la capa vía `K_INT`. Toda la tensión
(arrastre vs. matar anclas vs. distorsionar superhabit) es consecuencia de esa mezcla:

- **Arrastre diluido por N (techo 1/N):** porque el opt-in vive en UNA capa de N, su peor
  valor solo puede mover esa fracción del promedio. **Solución A lo saca de la capa:** `C`
  multiplica el agregado entero → arrastre **plano en N** (idéntico con 3 o 5 capas).
- **Subir K_INT mata las anclas (caminar a 6%):** porque comparten la misma "bolsa" de la
  capa. **Solución A no comparte bolsa:** la base de la capa es 100% anclas; `C` es un factor
  externo. Caminar siempre vale lo que vale (P5: base de capa = 1.000 con 1 o 3 anclas).
- **Inflar el peso de la capa distorsiona el superhabit:** porque mover el peso de capa
  afecta a los dos canales. **Solución A separa los canales:** `C` toca SOLO base; el extra
  se promedia con pesos iguales siempre → Sol=Tin garantizado por construcción (P6).

El opt-in deja de ser "una capa más con mucho peso" y pasa a ser lo que conceptualmente es:
el **cimiento** sobre el que se apoyan las prácticas. No es una práctica; es la condición que
las sostiene. Por eso modula el conjunto sin contaminar el valor de ninguna práctica.

---

## 3. Parámetros calibrables

| Parámetro | Default | Qué controla | Efecto |
|-----------|---------|--------------|--------|
| `FLOOR_C` | 0.55 | Piso del cimiento (peor opt-in posible) | Profundidad máxima del arrastre. 0.55 → recaída total lleva un día perfecto de anclas a 0.55 (Atención). Bajarlo = arrastre más agresivo; subirlo = más suave. |
| `GAMMA_C` | 0.9 | Curvatura de `g(m)=m^γ` | <1: penaliza más rápido el déficit leve de opt-in (sueño regular ya pesa). >1: tolera el déficit leve y solo castiga el opt-in muy malo. |
| `B_SLEEP` | 0.50 | Sueño sin dato | Señal por defecto (axioma 5). |

Calibración recomendada de `FLOOR_C` por axioma de estado del dueño: "recaída total con
anclas perfectas → ¿qué banda?". Hoy `FLOOR_C=0.55` la deja en **Atención** (0.550). Si el
dueño quiere que recaída total = borde Atención/Restauración, `FLOOR_C≈0.40`. Si quiere que
no baje de En marcha, `FLOOR_C≈0.62`. **Es un único número con significado directo.**

---

## 4. Verificación python3 (8 casos, ANTES v3 vs DESPUÉS A)

Script: ver `solucion-A-verif.py` (incluido abajo). `J=R(4,30,[30]×4)=1.0`,
`SUP=R(4,30,[30]×6)=1.266`, `XL=R(4,30,[60]×7)=1.432`. **Salida real pegada:**

```
J=1.0000 SUP=1.2656 XL=1.4323   FLOOR_C=0.55 GAMMA_C=0.9
==============================================================================================
CASO                                     v3 ANTES          banda |  A DESPUÉS          banda
----------------------------------------------------------------------------------------------
P1 justo + sueño bien (N=3)                 1.000       PLENITUD |      1.000       PLENITUD
P2 mal sueño M=0.15 N=3                     0.773      EN MARCHA |      0.632      EN MARCHA
P2 mal sueño M=0.15 N=5                     0.864       PLENITUD |      0.632      EN MARCHA
P3 recaída M=0 N=3                          0.733      EN MARCHA |      0.550       ATENCION
P3 recaída M=0 N=5                          0.840      EN MARCHA |      0.550       ATENCION
P4 sueño regular M=0.5 N=5                  0.920       PLENITUD |      0.791      EN MARCHA
P5 Cuerpo 3 anclas + sueño mal N=3          0.773      EN MARCHA |      0.632      EN MARCHA
P6 Sol (super en Interior)                  1.144 INQUEBRANTABLE |      1.144 INQUEBRANTABLE
P6 Tin (super en Cuerpo+sueño)              1.144 INQUEBRANTABLE |      1.144 INQUEBRANTABLE
P7 superhabit repartido XL x3               1.432 INQUEBRANTABLE |      1.432 INQUEBRANTABLE
P8 capa solo-opt-in (sueño bien)            1.000       PLENITUD |      1.000       PLENITUD

VERIFICACIONES:
  C2 neutralidad opt-in bien: sin=1.000 con=1.000 -> True
  C5 Sol=Tin: 1.144==1.144 -> True
  C3 arrastre PLANO en N: [0.632, 0.632, 0.632, 0.632] -> plano: True
  C4 base de capa Cuerpo intacta: 1 ancla=1.000 3 anclas=1.000 (no cae a 0.06)
```

### Lectura de los criterios

- **C1 (P1):** justo + opt-in bien = 1.000 Plenitud. ✅ (C=1 es neutro)
- **C2:** opt-in bien no cambia el score (1.000 con o sin). ✅
- **C3 (P2,P3,P4):** arrastre **más fuerte que v3** Y **plano en N**. Mal sueño: v3 0.773/0.864
  (diluye) → A **0.632/0.632** (idéntico). Recaída: v3 0.733/0.840 → A **0.550/0.550**. El
  golpe ya **no se diluye con más capas** — exactamente el objetivo. ✅✅
- **C4 (P5):** la base de la capa Cuerpo = 1.000 con 1 ó 3 anclas; caminar **nunca cae a 6%**
  (en v3 con K agresivo caía). Las anclas conservan su valor entero. ✅
- **C5 (P6):** Sol=Tin=1.144. Superhabit en Cuerpo+sueño = superhabit en Interior. **Empatan
  por construcción** (el extra se promedia con pesos iguales y `C` no lo toca). ✅
- **C6 (P7):** superhabit repartido = 1.432 Inquebrantable; cumplir justo = Plenitud. ✅
- **P8:** capa solo-opt-in con sueño bien = 1.000, sin extra. ✅

---

## 5. Tensiones honestas

1. **Arrastre más agresivo que v3 — y eso es deliberado, pero hay que calibrar el piso.** A
   baja más que v3 en TODOS los casos de déficit (es el punto). Pero eso significa que `P4
   sueño regular M=0.5` cae de Plenitud a **En marcha** (0.791). ¿Es deseable? Probablemente
   sí (sueño regular no debería ser Plenitud), pero es una decisión del dueño: `GAMMA_C`
   controla cuán pronto el déficit leve pesa. Hay que marcarlo con una historia de usuario.

2. **`min(opt-ins)` ignora cuántos opt-ins están bien.** Si dormís mal pero mantenés
   sobriedad, `C` solo mira el sueño (el peor). Es coherente con "el cimiento más débil
   manda" y con el axioma de sobriedad, pero alguien podría querer que tener 1 opt-in sano
   amortigüe. **No lo hago a propósito** (amortiguar reintroduce dilución). Si el dueño lo
   quiere, sería un promedio en vez de `min` — pero eso debilita el arrastre.

3. **`C` multiplicativo sobre base puede "doble-castigar" si las anclas YA están bajas.** Si
   tenés anclas en déficit (base_anc=0.6) Y dormís mal (C=0.632), base=0.379 (Restauración).
   Es geométrico, no aditivo: el déficit de práctica y el déficit de cimiento se componen.
   Conceptualmente correcto (poca práctica + sin descanso = peor que cualquiera solo), pero
   es más severo que un modelo aditivo. Hay que validar que no sea demasiado punitivo en el
   tono de la app.

4. **El piso `FLOOR_C` aplica aunque NINGUNA práctica falle.** Con anclas perfectas y recaída
   total, el techo es `FLOOR_C` (0.55). Eso significa que el opt-in por sí solo puede mandarte
   a Atención teniendo todo lo demás impecable. Es el arrastre fuerte pedido (C3), pero es una
   afirmación fuerte sobre el peso del opt-in que conviene que el dueño firme explícitamente.

5. **No se distingue "1 opt-in mal" de "2 opt-ins mal" salvo por cuál es el peor.** Dos
   recaídas + mal sueño dan lo mismo que una recaída sola (C=FLOOR_C en ambos). Coherente con
   "el peor cimiento ya rompió la casa", pero quien quiera escalar por cantidad de fallos
   necesitaría otra agregación.

---

## Apéndice — script de verificación (`solucion-A-verif.py`)

```python
import math
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
def Rv(mins): return R(4,30,mins)
DELTA=0.10
def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")
# --- v3 (ANTES) ---
K_INT=4.0
def valor_capa_v3(anclas,M=None):
    ab=(sum(min(r,1.0) for r in anclas)/len(anclas)) if anclas else None
    base=(ab+K_INT*M)/(1+K_INT) if (M is not None and ab is not None) else (M if M is not None else (ab or 0))
    extra=(sum(max(r-1,0) for r in anclas)/len(anclas)) if anclas else 0.0
    return min(base,1.0)+extra
def score_v3(capas):
    return sum(valor_capa_v3(a,M) for a,M in capas)/len(capas)
# --- Solución A (DESPUÉS) ---
FLOOR_C=0.55; GAMMA_C=0.9; DROP=1-FLOOR_C
def g_optin(m): return m**GAMMA_C
def foundation(sigs): return 1.0 if not sigs else FLOOR_C+DROP*g_optin(min(sigs))
def lbase(a): return None if not a else sum(min(r,1.0) for r in a)/len(a)
def lextra(a): return 0.0 if not a else sum(max(r-1,0) for r in a)/len(a)
def score_A(layers):
    bv=[];ev=[];sg=[]
    for L in layers:
        a=L.get('anchors') or []; op=L.get('optin')
        if a:
            bv.append(lbase(a)); ev.append(lextra(a))
            if op is not None: sg.append(op)
        elif op is not None:
            bv.append(op); sg.append(op)
    N=len(bv); base_anc=sum(bv)/N if N else 0.0
    C=foundation(sg); base=base_anc*C
    extra=sum(ev)/len(ev) if ev else 0.0
    return base,extra,base+extra,C

J=Rv([30]*4);SUP=Rv([30]*6);XL=Rv([60]*7)
rows=[]
def cmp(label,v3,a):
    s3=v3(); _,_,sA,_=score_A(a); rows.append((label,s3,band(s3),sA,band(sA)))
cmp("P1 justo + sueño bien (N=3)",
    lambda: score_v3([([J],None),([J],1.0),([J],None)]),
    [{'anchors':[J]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
cmp("P2 mal sueño M=0.15 N=3",
    lambda: score_v3([([J],0.15)]+[([J],None) for _ in range(2)]),
    [{'anchors':[J],'optin':0.15}]+[{'anchors':[J]} for _ in range(2)])
cmp("P2 mal sueño M=0.15 N=5",
    lambda: score_v3([([J],0.15)]+[([J],None) for _ in range(4)]),
    [{'anchors':[J],'optin':0.15}]+[{'anchors':[J]} for _ in range(4)])
cmp("P3 recaída M=0 N=3",
    lambda: score_v3([([J],0.0)]+[([J],None) for _ in range(2)]),
    [{'anchors':[J],'optin':0.0}]+[{'anchors':[J]} for _ in range(2)])
cmp("P3 recaída M=0 N=5",
    lambda: score_v3([([J],0.0)]+[([J],None) for _ in range(4)]),
    [{'anchors':[J],'optin':0.0}]+[{'anchors':[J]} for _ in range(4)])
cmp("P4 sueño regular M=0.5 N=5",
    lambda: score_v3([([J],0.5)]+[([J],None) for _ in range(4)]),
    [{'anchors':[J],'optin':0.5}]+[{'anchors':[J]} for _ in range(4)])
cmp("P5 Cuerpo 3 anclas + sueño mal N=3",
    lambda: score_v3([([J,J,J],0.15),([J],None),([J],None)]),
    [{'anchors':[J,J,J],'optin':0.15},{'anchors':[J]},{'anchors':[J]}])
cmp("P6 Sol (super en Interior)",
    lambda: score_v3([([XL],None),([J],1.0),([J],None)]),
    [{'anchors':[XL]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
cmp("P6 Tin (super en Cuerpo+sueño)",
    lambda: score_v3([([J],None),([XL],1.0),([J],None)]),
    [{'anchors':[J]},{'anchors':[XL],'optin':1.0},{'anchors':[J]}])
cmp("P7 superhabit repartido XL x3",
    lambda: score_v3([([XL],None),([XL],1.0),([XL],None)]),
    [{'anchors':[XL]},{'anchors':[XL],'optin':1.0},{'anchors':[XL]}])
cmp("P8 capa solo-opt-in (sueño bien)",
    lambda: score_v3([([J],None),([J],None),([J],None)]),
    [{'anchors':[J]},{'anchors':[],'optin':1.0},{'anchors':[J]},{'anchors':[J]}])

print(f"J={J:.4f} SUP={SUP:.4f} XL={XL:.4f}   FLOOR_C={FLOOR_C} GAMMA_C={GAMMA_C}")
print("="*94)
print(f"{'CASO':38s} {'v3 ANTES':>10s} {'banda':>14s} | {'A DESPUÉS':>10s} {'banda':>14s}")
print("-"*94)
for lab,s3,b3,sA,bA in rows:
    print(f"{lab:38s} {s3:>10.3f} {b3:>14s} | {sA:>10.3f} {bA:>14s}")
print("\nVERIFICACIONES:")
_,_,sNo,_=score_A([{'anchors':[J]},{'anchors':[J]},{'anchors':[J]}])
_,_,sBi,_=score_A([{'anchors':[J]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
print(f"  C2 neutralidad opt-in bien: sin={sNo:.3f} con={sBi:.3f} -> {abs(sNo-sBi)<1e-9}")
_,_,sSol,_=score_A([{'anchors':[XL]},{'anchors':[J],'optin':1.0},{'anchors':[J]}])
_,_,sTin,_=score_A([{'anchors':[J]},{'anchors':[XL],'optin':1.0},{'anchors':[J]}])
print(f"  C5 Sol=Tin: {sSol:.3f}=={sTin:.3f} -> {abs(sSol-sTin)<1e-9}")
print("  C3 arrastre PLANO en N: ", end="")
vals=[round(score_A([{'anchors':[J],'optin':0.15}]+[{'anchors':[J]} for _ in range(N-1)])[2],3) for N in [3,4,5,6]]
print(vals, "-> plano:", len(set(vals))==1)
print(f"  C4 base de capa Cuerpo intacta: 1 ancla={lbase([J]):.3f} 3 anclas={lbase([J,J,J]):.3f} (no cae a 0.06)")
```
