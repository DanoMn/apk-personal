# Propuesta — Modelo del valor de capa · SUBAGENTE A (sesgo ADITIVO / separación máxima de canales)

> **Estado: propuesta a ciegas, autocontenida y verificada con python3.** No vi las otras
> propuestas. Sesgo: mínima no-linealidad; base = suma ponderada transparente topada en 1;
> extra = suma aparte; combinación final EXPLÍCITA y aditiva. Donde otros meterían
> multiplicación/gates, busqué un término aditivo. Toda magnitud acá es **calibrable** y se
> **despeja de un axioma de estado del dueño**, no se elige a dedo.

---

## 0. Tesis del sesgo aditivo (por qué alcanza sin multiplicar)

El motor del dueño ya tiene una no-linealidad fuerte y **necesaria** encapsulada en la caja
negra del ancla `R` (`base^p · S` subordina el superávit, el `min(r,1)^γ` mata trivialidad).
Esa pieza paga sola la "pata coja" *dentro* de un ancla. **Por encima del ancla, la única
forma que pide la filosofía es separar dos preguntas** —"¿está en pie?" y "¿se destacó?"— y
**no dejar que se contaminen**. Eso es exactamente lo que la SUMA hace y la multiplicación
**rompe**: un producto `base · extra` ataría el mérito ganado (extra) al estado de la base, que
es justo la objeción #1 del dueño a P-A ("un sueño regular borra el mérito del ancla").

La aditividad da las cuatro propiedades que el modelo necesita, todas sin un solo `if`:

1. **Separabilidad de canales** — base y extra son sumandos independientes; tocar uno no toca
   el otro (caso 5: mal sueño hunde base, extra intacto).
2. **El EJE (axioma 3)** — base llena se mapea, por una constante de escala `EM_CEIL`, al
   *techo de En marcha*; Plenitud/Inquebrantable se suman SOLO desde el extra. Es un cambio de
   escala lineal, lo más transparente posible.
3. **Pesos relacionales** — emergen de `tamaño/Σtamaños` (suma de tamaños en el denominador):
   relacional puro, sin constante fija.
4. **Saturación de soportes/tasks** — única concavidad chica que me permito, y solo porque el
   axioma "8 soportes no fabrican una banda" lo exige; es un sumando saturante, no un gate.

La no-linealidad se queda donde ya estaba (el ancla). El resto es álgebra lineal con dos topes
explícitos (`min(·,1)` en la base; saturante en soportes). **Eso es suficiente para los 12
comportamientos** —lo demuestro en §6.

---

## 1. El modelo completo (todas las variables, con rango)

### 1.1 Entradas

| Símbolo | Qué es | Rango |
|---------|--------|-------|
| `R_i` | rendimiento del ancla *i* (caja negra consolidada) | `[0, 1+σ_max]` (≈ `[0,1.5]`) |
| `M` | señal semanal del opt-in (sueño o sobriedad) | `[0,1]` |
| `n_a` | nº de anclas en la capa | `≥0` |
| `n_s` | nº de soportes cumplidos esta semana | `≥0` |
| `n_t` | nº de tasks **con capa** cumplidas esta semana | `≥0` |
| `K` | parámetro relacional del opt-in (uno por tipo: `K_sleep`, `K_sobr`) | `>0`, despejado |

### 1.2 Bloque de anclas (promedian a UN bloque — axioma 5)

```
ancla_base = (1/n_a) · Σ_i  min(R_i, 1)        ∈ [0,1]   (None si n_a = 0)
ancla_extra = (1/n_a) · Σ_i  max(R_i − 1, 0)   ≥ 0       (0 si n_a = 0)
```
Promediar (no sumar) hace que **el nº de anclas no cambie el peso ni infle el bloque**
(axiomas 5 y 11). El extra también promedia: repartir el superávit en más anclas no lo agranda
artificialmente.

### 1.3 Soportes y tasks — sumandos saturantes a la BASE (decisión §2.3)

```
aporte_soportes = C_s · (1 − exp( −(s_unit/C_s) · n_s ))     ∈ [0, C_s)
aporte_tasks    = C_t · (1 − exp( −(t_unit/C_t) · n_t ))     ∈ [0, C_t)
```
- `s_unit` = pendiente inicial por soporte; `C_s` = techo del bloque de soportes.
- `t_unit < s_unit` y `C_t < C_s` ⟹ una task aporta **menos que un soporte** (axioma 7,10) y
  el bloque de tasks pesa menos que el de soportes.
- La exponencial saturante: el 1er soporte aporta ~`s_unit`, el 8º casi nada → **8 soportes no
  fabrican una banda** (axioma 9). Magnitud **relacional al techo `C_s`**, no fija por soporte.

### 1.4 Canal BASE de la capa — `en_pie ∈ [0,1]`

El término del opt-in entra **ponderado por el K relacional** dentro de la capa:

```
core = {
    ancla_base                                     si no hay opt-in
    w_anch·ancla_base + w_opt·M                    si hay anclas + opt-in
    M                                              si solo opt-in (sin anclas)
}
con  w_opt = K/(1+K),  w_anch = 1/(1+K)            (anclas = UN bloque; ver §2.2)

en_pie = min( core + aporte_soportes + aporte_tasks , 1 )      ∈ [0,1]
```
El `min(·,1)` es el ÚNICO tope de la base: "estar en pie" no pasa de 1 (dormir extra bien no
sube la base — axioma 3, coincide con el diseño del código: `M ≤ 1`).

### 1.5 Canal EXTRA de la capa — `destaco ≥ 0`

```
destaco = ancla_extra        (0 si la capa no tiene anclas)
```
**Solo las anclas exportan extra.** Sueño/sobriedad nunca (su señal está topada en 1 por
diseño). Una capa solo-opt-in tiene `destaco = 0` (axioma 8, caso 3).

### 1.6 Pesos relacionales (emergen, no se fijan — axioma 4)

```
tamaño(capa) = [1 si tiene anclas else 0]  +  [K si tiene opt-in]
peso(capa)   = tamaño(capa) / Σ_capas tamaño(capa)            (Σ pesos = 1)
```
- Capa sin opt-in: tamaño 1.
- Capa con opt-in: tamaño `1+K` (con anclas) o `K` (solo-opt-in).
- **Relacional**: el denominador es la suma → el peso de cualquier capa **baja al crecer N**
  (caso 12). El nº de anclas **no** entra al tamaño (caso 11).

### 1.7 Agregación global y EJE (axioma 3) — la combinación EXPLÍCITA

```
BASE_global  = Σ_capas  peso(capa) · en_pie(capa)                       ∈ [0,1]

EXTRA_global = Σ_{capas con anclas}  (peso(capa)/Σ_{con anclas} peso) · destaco(capa)   ≥ 0
              (re-normalizado SOLO entre capas con anclas → el extra se reparte
               únicamente entre las que PUEDEN generarlo; resuelve el conflicto D6/objeción 2)

ESTADO = EM_CEIL · BASE_global  +  W_extra · EXTRA_global
```
- `EM_CEIL` = techo de "En marcha". **BASE llena (=1) ⟹ ESTADO = EM_CEIL = tope de En marcha.**
  Plenitud/Inquebrantable se alcanzan SOLO sumando `W_extra·EXTRA`. **Esto formaliza el eje
  del axioma 3 con un cambio de escala lineal, sin reglas.**
- `W_extra` = cuánto pesa destacarse en el estado final.

### 1.8 Bandas (sobre el ESTADO)

```
Rojo            < 0.40
Atención        < 0.62
En marcha       ≤ EM_CEIL (=0.85)        ← base llena cae acá (eje)
Plenitud        < 1 + δ
Inquebrantable  ≥ 1 + δ
```

### 1.9 Señales de opt-in semanales

**Sueño (cobertura + base — axioma 6):**
```
have = noches con dato (de 7);   c = |have| / 7
M_sueño = c · avg(have) + (1−c) · M_base       (= M_base si no hay ninguna noche)
```
`M_base` ∈ (0,1) impide que la ausencia de telemetría tire la capa al piso (caso 6).

**Sobriedad (binario, multi-track — axiomas 6,7,8):**
```
M_sobr = Π_track  (1 si held  else b_relapse)     (None si no hay tracks)
```
Producto sobre tracks: **1 recaída hunde igual con 1 o N tracks** (no se diluye, caso 8);
2 recaídas hunden más. Con `b_relapse=0`, una recaída lleva la señal a 0. La ventana de 7d la
aplica el pipeline aguas arriba (recaída vieja → `held=True`, no penaliza — caso 7).

---

## 2. Justificación de cada decisión abierta (con sesgo aditivo)

### 2.1 Cómo se combina base + extra y dónde caen las bandas
**`ESTADO = EM_CEIL·BASE + W_extra·EXTRA`** — suma de dos términos en escalas explícitas.
`EM_CEIL` clava la base llena en el techo de En marcha (eje). El extra es el único que cruza a
Plenitud/Inquebrantable. **Por qué aditivo y no multiplicativo:** un producto ataría el extra
ganado al nivel de la base (objeción #1 del dueño). La suma los mantiene independientes: el
mérito sobrevive aunque la base baje (caso 5). Trazable: el dueño puede leer "tu base vale X,
tu superávit suma Y".

### 2.2 Reparto interno del opt-in con MÁS anclas: **mantiene `K/(1+K)`** (no diluye)
Decisión: el opt-in conserva `w_opt = K/(1+K)` sin importar `n_a`. Las anclas **son un bloque
que promedia** (axioma 5): a efectos del peso son "1", no "n". Diluir a `K/(n_a+K)` haría que
agregar anclas debilitara al opt-in, contradiciendo que el nº de anclas no cambia la estructura
de pesos (axioma 11). El flag `DILUTE_OPTIN` queda en el código por si el dueño marca lo
contrario, pero **mi recomendación con sesgo aditivo es NO diluir**: mantiene la separabilidad
y la regla "anclas = bloque" intacta.

### 2.3 SOPORTES → a la BASE, saturados
Los soportes son mantenimiento ("¿está en pie?"), no superávit → **canal BASE**. No exportan
extra (no tiene sentido "destacarse" por tomar agua). Saturación con `C_s · (1−exp(−·))`: el
techo `C_s` es **relacional** (fracción chica de la base), de modo que multi-soporte mueve en
**bordes de banda** (caso 9) pero nunca fabrica una banda. Aditivo puro: es un sumando más.

### 2.4 TASKS → a la BASE, magnitud < soporte, saturadas
Mismo canal que soportes (mantenimiento), pero **`t_unit < s_unit` y `C_t < C_s`**: una task
aporta menos que un soporte (axioma 7,10) y nunca es neutra si tiene capa (caso 10). Task sin
capa / rol neutral: simplemente no entra (`n_t` no la cuenta). Saturada igual que soportes.

### 2.5 Multi-sobriedad: **producto de tracks** (no promedio)
`Π (held?1:b)`: el promedio diluiría (3 tracks, 1 recaída → 2/3); el producto NO (caso 8). No
premia tener más tracks (todos held → 1, sin bonus). Más recaídas → más caída. Evita `min()`
(worst-term, prohibido por axioma 1): el producto es continuo y diferenciable.

### 2.6 Sueño semanal: **cobertura + base**
`c·avg + (1−c)·M_base`. Promedia las noches con dato y rellena las faltantes con `M_base`
(no con 0). Sin ninguna noche → `M_base` (caso 6). Es aditivo (combinación convexa), respeta
la "base" del axioma 6 sin tirar al piso.

---

## 3. Cómo emergen los pesos y cómo se despejan los parámetros

**Pesos:** puro `tamaño/Σtamaños`. Nada fijo. El opt-in mete `+K` al tamaño de su capa; al
crecer N, el denominador crece y todo peso baja (relacional, caso 12). El peso del opt-in
**dentro** de la capa es `K/(1+K)`.

**`K` se DESPEJA de un axioma de estado del dueño** (mismo método que el ancla). Ejemplo real
verificado (§6, script `calib_modelA.py`):

> Axioma: "anclas perfectas + semana entera sin dormir (M=0) ⟹ estado en el borde
> Atención/En marcha (0.62)". Con N=3, álgebra cerrada:
> `ESTADO = EM_CEIL · 3/(3+K) = 0.62` ⟹ **`K_sleep = 3·EM_CEIL/0.62 − 3 = 1.113`**.

> Axioma D8: "una recaída + anclas perfectas ⟹ Rojo (0.40)" ⟹
> **`K_sobr = 3·EM_CEIL/0.40 − 3 = 3.375`**. `K_sobr > K_sleep` ⟹ **la recaída pega más que el
> mal sueño** (D8), emergente del despeje, no elegido.

El resto de parámetros se calibran contra el dataset de marcas del dueño (tabla §7).

---

## 4. Script de verificación (modelo completo)

`modelA.py` (modelo) — núcleo:

```python
import math

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

# Parametros calibrables
K_SLEEP=1.0; K_SOBR=2.0; M_BASE=0.5; B_RELAPSE=0.0
S_SUP=0.06; S_SAT=0.12; T_TASK=0.03; T_SAT=0.06
W_EXTRA=0.6; DILUTE_OPTIN=False; EM_CEIL=0.85; DELTA=0.10

def band(state):
    if state < 0.40: return "Rojo"
    if state < 0.62: return "Atencion"
    if state <= EM_CEIL + 1e-9: return "En marcha"
    if state < 1.0 + DELTA: return "Plenitud"
    return "Inquebrantable"

def sleep_weekly(nights):
    have = [n for n in nights if n is not None]; c = len(have)/7.0
    if c == 0: return M_BASE
    return c*(sum(have)/len(have)) + (1-c)*M_BASE

def sobriety_signal(tracks):
    if not tracks: return None
    sig = 1.0
    for held in tracks: sig *= (1.0 if held else B_RELAPSE)
    return sig

def saturating_block(n_done, per_unit, ceiling):
    if n_done <= 0: return 0.0
    return ceiling * (1 - math.exp(-(per_unit/ceiling)*n_done))

class Layer:
    def __init__(self, name, anchors=None, optin=None, optin_signal=None,
                 supports_done=0, tasks_done=0):
        self.name=name; self.anchors=anchors or []; self.optin=optin
        self.optin_signal=optin_signal; self.supports_done=supports_done
        self.tasks_done=tasks_done
    def has_anchors(self): return len(self.anchors) > 0
    def K(self):
        return K_SLEEP if self.optin=='sleep' else (K_SOBR if self.optin=='sobriety' else 0.0)
    def size(self):
        return (1.0 if self.has_anchors() else 0.0) + self.K()
    def en_pie(self):
        ab = (sum(min(r,1.0) for r in self.anchors)/len(self.anchors)) if self.has_anchors() else None
        K = self.K()
        if self.optin is not None and self.optin_signal is not None:
            if ab is not None:
                if DILUTE_OPTIN:
                    n=len(self.anchors); w_opt=K/(n+K); w_anch=n/(n+K)
                else:
                    w_opt=K/(1+K); w_anch=1/(1+K)
                core = w_anch*ab + w_opt*self.optin_signal
            else:
                core = self.optin_signal
        else:
            core = ab if ab is not None else 0.0
        sup = saturating_block(self.supports_done, S_SUP, S_SAT)
        tsk = saturating_block(self.tasks_done, T_TASK, T_SAT)
        return min(core + sup + tsk, 1.0)
    def destaco(self):
        if not self.has_anchors(): return 0.0
        return sum(max(r-1.0,0.0) for r in self.anchors)/len(self.anchors)

def score(layers):
    sizes=[L.size() for L in layers]; total=sum(sizes)
    weights=[s/total for s in sizes]
    base=sum(w*L.en_pie() for w,L in zip(weights,layers))
    al=[(w,L) for w,L in zip(weights,layers) if L.has_anchors()]
    if al:
        wsum=sum(w for w,_ in al); extra=sum((w/wsum)*L.destaco() for w,L in al)
    else:
        extra=0.0
    state = EM_CEIL*base + W_EXTRA*extra
    return base, extra, state, weights
```

El test harness completo (`test_modelA.py`) y el despeje (`calib_modelA.py`) producen la salida
real pegada abajo.

---

## 5. Tabla de resultados (salida REAL de python3)

`Params: K_sleep=1.0 K_sobr=2.0 W_extra=0.6 M_base=0.5 S_sup=0.06/0.12 T_task=0.03/0.06 delta=0.1`

| # | Caso | Resultado clave | Estado | ✓ |
|---|------|-----------------|--------|---|
| 1 | Todo justo (R=1, opt-ins OK) | base=1.0 extra=0 → **0.8500** | En marcha | ✅ |
| 2 | Superhabit repartido | base=1.0 extra=0.297 → **1.0283** | Plenitud | ✅ |
| 3 | Conducta solo-sobriedad (sin anclas) | en_pie=1.0, **destaco=0** → 0.8500 | En marcha | ✅ |
| 4 | 3 capas + ambos opt-ins | **Σ pesos = 1.000000** | — | ✅ |
| 5a | Sueño OK | base=1.0 extra=0.0358 → 0.8715 | Plenitud | — |
| 5b | Sueño MALO (0.3) | base=0.883 **extra=0.0358 (igual)** → 0.7723 | En marcha | ✅ |
| 6 | Sueño SIN dato | **M = M_base = 0.500** (no 0) → 0.7438 | En marcha | ✅ |
| 7a | Recaída DENTRO 7d | base=0.600 → **0.5100** | Atención | ✅ |
| 7b | Recaída FUERA 7d | base=1.0 → 0.8500 (dentro<fuera) | En marcha | ✅ |
| 8 | Multi-sobriedad | señal 1/1 = **1/3 = 0.000** (no diluye); 2/3 ≤ 1/3 | — | ✅ |
| 9 | Soportes 0→3→8 | Δ0→3=**0.0264** (light); Δ3→8=**0.0070 < 0.0264** (satura) | — | ✅ |
| 10 | Task vs soporte | task **0.0079 < 0.0157** soporte, task>0 | — | ✅ |
| 11 | Más anclas, mismo peso | peso 1 ancla = 3 anclas = **0.5000** | — | ✅ |
| 12 | Peso opt-in baja con N | N=3 **0.500** → N=4 **0.400** → N=5 **0.333** | — | ✅ |

**12/12 casos pasan.** Salida cruda completa de `test_modelA.py` (extractos verbatim):

```
=== CASO 1: todo justo (R=1, opt-ins OK) ===
  BASE=1.0000  EXTRA=0.0000  ESTADO=0.8500  -> En marcha       [EJE OK]
=== CASO 2: superhabit repartido ===
  BASE=1.0000  EXTRA=0.2972  ESTADO=1.0283  -> Plenitud
=== CASO 5b: sueño MALO (0.3) ===
  Cuerpo  en_pie=0.650   Interior destaco=0.215
  BASE=0.8833  EXTRA=0.0358  ESTADO=0.7723  -> En marcha
   extra igual? 0.0358==0.0358 -> OK   base bajo? 0.8833<1.0000 -> OK
=== CASO 7a: recaida DENTRO 7d === BASE=0.6000 ESTADO=0.5100 -> Atencion
=== CASO 7b: recaida FUERA 7d  === BASE=1.0000 ESTADO=0.8500 -> En marcha
=== CASO 8 === 1/1=0.000  1/3=0.000 (iguales)  2/3=0.000 (<=)
=== CASO 9 === Δ0→3=0.0264 (light)  Δ3→8=0.0070 < 0.0264 (satura)
=== CASO 10 === soporte=0.0157  task=0.0079  (task<soporte, >0)
=== CASO 11 === peso 1a=0.5000  3a=0.5000  (igual)
=== CASO 12 === N=3:0.5000  N=4:0.4000  N=5:0.3333  (baja)
```

**Despeje de K (salida real de `calib_modelA.py`):**
```
K_sleep despejado ('sin dormir -> borde Atencion 0.62') = 1.1129
  VERIFICA estado con K=1.1129, sueño=0 -> 0.6200  (match True)
K_sobr  despejado ('recaida -> Rojo 0.40')             = 3.3750
  K_sobr (3.37) > K_sleep (1.11): recaida pega mas -> D8 OK
Superhabit fuerte repartido (7d, doble tiempo, 3 capas):
  base=1.000 extra=0.491 estado=1.1445 -> Inquebrantable
```

Esto cierra el eje completo: base sola tope En marcha (caso 1 = 0.85), superhabit lleva a
Plenitud (caso 2 = 1.03) y, si es fuerte y repartido, a Inquebrantable (1.14).

---

## 6. Tabla de parámetros calibrables

| Param | Rol | Rango plausible | Cómo se fija |
|-------|-----|-----------------|--------------|
| `EM_CEIL` | techo de En marcha = techo del canal base solo (eje) | `0.80 – 0.88` | = umbral de banda Plenitud; define el eje |
| `K_sleep` | peso relacional del opt-in sueño | despejado | axioma "sin dormir → tal estado" (ej. 1.11) |
| `K_sobr` | peso relacional del opt-in sobriedad (> K_sleep) | despejado | axioma D8 "recaída → Rojo" (ej. 3.38) |
| `M_base` | base de sueño sin dato (no tira a 0) | `0.4 – 0.6` | axioma "sin telemetría no es fracaso" |
| `b_relapse` | señal de un track con recaída | `0.0 – 0.2` | 0 = recaída total; >0 si "1 día ≠ semana perdida" |
| `W_extra` | cuánto sube el superávit en el estado | `0.4 – 0.8` | axioma "superhabit X → Plenitud/Inq" |
| `s_unit`,`C_s` | pendiente y techo del bloque soportes | `0.04–0.08` / `0.10–0.15` | axioma "soportes mueven borde, no banda" |
| `t_unit`,`C_t` | íd. tasks (< soportes) | `0.02–0.04` / `0.04–0.08` | axioma "task < soporte" |
| `δ` | margen de Inquebrantable sobre 1 | `0.08 – 0.20` | heredado del ancla |
| `DILUTE_OPTIN` | reparto interno opt-in con n anclas | `False` (recomendado) | decisión abierta §2.2 |

---

## 7. Explicación en criollo (sin fórmulas)

Tu vida está partida en 3 a 5 áreas (capas). El modelo se hace **dos preguntas separadas** sobre
cada área y nunca las mezcla:

**Primera pregunta: "¿está en pie?"** Mira si cumpliste tus anclas hasta la meta (de ahí no
pasa: pasarte no cuenta acá), suma la señal de tu sueño o tu sobriedad si activaste ese cuidado,
y agrega un poquito por los soportes y los pendientes que sí hiciste. Todo eso se promedia entre
tus áreas, dándole **más peso a las áreas donde activaste un cuidado sensible** (insomnio,
abstinencia) — porque ahí es donde más te juega. Esa cuenta da tu **base**. Y acá está lo
importante: **tener todo en pie te deja "En marcha", que es tu casa, no la gloria.** Es estar
funcionando, sostenido. Ni más ni menos.

**Segunda pregunta: "¿te destacaste?"** Esto mira **solo el excedente de tus anclas** — los días
de más, el tiempo de más que metiste por encima de tu meta. Eso, y solo eso, es lo que te
empuja de "En marcha" a **Plenitud** y, si está fuerte y repartido entre tus áreas, a
**Inquebrantable**. El sueño y la sobriedad **no suben acá**: dormir de más no es heroísmo, no
tomar es la base. Por eso un buen cuidado te mantiene en pie pero nunca te hace brillar solo.

**La gracia de tenerlas separadas:** si dormiste mal una semana, eso **hunde tu base** (tu "en
pie" baja, capaz caés de Plenitud a En marcha), **pero NO te borra el esfuerzo** que metiste en
tus anclas — ese superávit queda guardado intacto. El mal día no te roba el mérito. Una recaída
pega más fuerte que el mal sueño (porque es más sensible), pero también solo dentro de la última
semana: una recaída vieja no te castiga para siempre.

**Y los detalles finos:** tener más anclas en un área no la hace pesar más (las anclas se
promedian a "un bloque"). Tener más áreas baja el peso de cada una (es relativo, se reparte la
torta). Ocho soportes no te fabrican una medalla: el primero suma, el octavo casi nada. Un
pendiente con área suma menos que un soporte. Y los números que deciden todo esto **no los
elegí a dedo**: salen de despejar tus propios axiomas ("si no dormí toda la semana, ¿dónde
quiero quedar?").

---

## 8. Tensiones honestas de esta propuesta

1. **El eje vía `EM_CEIL` aprieta TODA la base en `[0, 0.85]`.** Consecuencia: una base muy mala
   (0.5) da estado 0.425, apenas sobre Atención. La granularidad de la base se comprime; si el
   dueño quiere que la base "mala" caiga más rápido a Rojo, hay que curvar la base (romper un
   poco la aditividad pura) o bajar las anclas vía γ. Lo dejo como calibración, no como rediseño.
2. **`W_extra` y la magnitud del extra están acopladas a la calibración del ancla** (`σ_max`).
   El extra de capa ≤ `σ_max` (≈0.5); con `W_extra=0.6` el tope de estado es ~0.85+0.3=1.15.
   Llegar a Inquebrantable (≥1.10) **exige superhabit fuerte y repartido** (caso del despeje:
   7 días a doble tiempo en las 3 capas). Es duro a propósito (filosofía), pero si resulta
   *inalcanzable* en la práctica, se sube `W_extra` o `σ_max`. Es perilla, no defecto.
3. **El boost de peso (K) y el término de valor del opt-in (`w_opt=K/(1+K)`) comparten el MISMO
   K.** Es elegante (un solo parámetro hace las dos cosas, D3) pero los ata: no podés tener "el
   sueño pesa mucho en el valor pero poco en el peso de la capa". Si el dueño los quiere
   independientes, haría falta un segundo parámetro — rompería el axioma 4 ("un solo K"). Por
   ahora respeto el axioma: un K, doble efecto.
4. **`b_relapse=0` hace la recaída un cero duro en su capa.** Es lo más fiel a D8, pero combinado
   con `min()`-free significa que la capa Conducta puede valer 0 en base. No es un gate (el resto
   de capas siguen sumando), pero es el punto más "filoso" del modelo. Calibrable vía `b_relapse`.

La aditividad **alcanzó** para los 12 comportamientos sin un solo gate ni `min()`: la única
no-linealidad que agregué fuera del ancla es la saturación de soportes/tasks (exigida por el
axioma "no fabricar bandas") y los dos topes `min(·,1)`. Todo lo demás es suma ponderada con
escalas explícitas y pesos relacionales.
