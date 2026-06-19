# Research 3 — Pesos que se desplazan según un parámetro

> Researcher 3. Sesión: exploración del rendimiento de un ancla (2026-06-09).
> Ángulo: mecanismos matemáticos en los que el peso relativo de una dimensión cambia
> en función de otro parámetro del propio sistema — específicamente, que el peso del
> superávit de tiempo crezca con `F` de forma continua y estructural.

---

## §1 — Estructuras encontradas

### E1 — Combinación convexa dependiente de parámetro (estilo GRU/LSTM)

**Forma general:**

```
S = λ(F) · A  +  (1 − λ(F)) · B
```

donde `λ(F) ∈ [0, 1]` es una función **monótona de F**, `A` es el término de
frecuencia/días y `B` es el término de tiempo/intensidad. La clave: los dos pesos
`λ` y `1−λ` suman exactamente 1 para todo valor de `F`.

**Forma concreta para el problema:**

```
λ(F) = 1 − (F / 7)^β          (β > 0, calibrable)

→  F = 1  ⟹  λ ≈ 1 − (1/7)^β  ≈  1   (frecuencia domina)
→  F = 7  ⟹  λ = 1 − 1 = 0   (tiempo domina completamente)
```

O en versión más suave (sigmoid):

```
λ(F) = σ( −k · (F − F₀) )     con σ(x) = 1/(1+e^{−x}), k y F₀ calibrables
```

**Propiedades:**
- Continua y diferenciable en todo `F ∈ [1, 7]` — satisface A9 por construcción.
- En F=7: λ=0, todo el peso recae sobre el término de tiempo (B).
- En F=1: λ≈1 (o ajustable), frecuencia domina.
- Los dos pesos siempre suman 1: la normalización interna del score se preserva.
- No hay gates ni reglas-patch: el desplazamiento emerge de λ(F).

**Origen en ML:** en GRU, el vector `z_t = σ(W·[h_{t-1}, x_t])` genera exactamente
esta interpolación: `h_t = z_t ⊙ h_{t-1} + (1 − z_t) ⊙ h̃_t`. La red aprende
cuánto "pesar" memoria pasada vs. información nueva. Acá F juega el rol del input
que determina z.

---

### E2 — Función CES con parámetro de distribución variable

**Forma general (CES — Constant Elasticity of Substitution):**

```
Rendimiento = A · ( a(F) · x_frec^ρ  +  (1 − a(F)) · x_tiempo^ρ )^(1/ρ)
```

donde:
- `x_frec = D/F` (fracción días cumplidos)
- `x_tiempo = media(t_i/T)` (fracción tiempo promedio)
- `ρ ∈ (−∞, 1)` — parámetro de sustitución (controla cuánto se compensan entre sí)
- `a(F) ∈ (0, 1)` — **parámetro de distribución**, acá función de F

**El desplazamiento del peso:**

```
a(F) = 1 − (F/7)^γ          (γ > 0)

→  F = 1  ⟹  a ≈ 1 − (1/7)^γ ≈ 1   (distribución hacia frecuencia)
→  F = 7  ⟹  a = 0   (distribución total hacia tiempo)
```

Cuando `a → 0`, la CES se comporta como si solo importara `x_tiempo`.

**Propiedades:**
- Continua en F y en `a(F)`.
- `ρ → −∞`: Leontief (mínimo de las dos dimensiones — sin compensación).
- `ρ = 0`: Cobb-Douglas (multiplicativa — sin compensación perfecta).
- `ρ = 1`: Sustitución perfecta (lineal, compensación total).
- Se puede calibrar `ρ` para fijar el grado de compensación independientemente
  del desplazamiento del peso — son dos knobs distintos.
- Satisface A10 (invarianza de escala) porque trabaja con razones `t_i/T`, `D/F`.
- A2 (piso cero): si `D=0` ⟹ `x_frec = 0` y la CES devuelve 0 si `ρ ≤ 0`.
  Para `ρ > 0` necesita manejo del caso `x_frec = 0` (límite).

---

### E3 — Gating suave tipo Mixture-of-Experts

**Forma general:**

```
Rendimiento = Σᵢ  gᵢ(F) · expertᵢ
```

donde `gᵢ(F)` son los pesos de routing —siempre `gᵢ ≥ 0`, `Σᵢ gᵢ = 1`— y cada
`expertᵢ` es una función especializada (uno para días, uno para tiempo).

Con dos "expertos":

```
g_tiempo(F) = softmax_F([0, s(F)])₂  =  e^{s(F)} / (1 + e^{s(F)})
g_frec(F)   = 1 − g_tiempo(F)

s(F) = α · (F/7)    (o cualquier función monótona creciente de F, α calibrable)
```

**Propiedades:**
- El gating es suave porque usa softmax: sin saltos, A9 satisfecha.
- A mayor F → mayor `s(F)` → mayor `g_tiempo(F)`: el experto de tiempo gana peso.
- F=7 → `g_tiempo = e^α / (1 + e^α)` (máximo, ajustable con α).
- F=1 → `g_tiempo = e^{α/7} / (1 + e^{α/7})` (mínimo).
- Para hacer F=7 → g_tiempo ≈ 1 exactamente, se usa `s(F) = α · (F−1)/(7−1)` o
  la combinación convexa E1 (que llega exactamente a 0/1 en los extremos).
- La diferencia con E1: los pesos no tienen que ser lineales en F; la forma de la
  transferencia se aprende/calibra libremente con la función s(F).

**Origen en ML:** Soft MoE de Google DeepMind (2023); cada token activa a todos
los expertos con peso proporcional, evitando tokens caídos. El gating network
toma el input y produce pesos normalizados de activación.

---

### E4 — Shrinkage tipo James-Stein dependiente de "grados de libertad disponibles"

**Forma general:**

```
ŵ_tiempo(F)  =  1 − λ_JS(F)

λ_JS(F)  =  (F_max − F) / F_max  =  (7 − F) / 7
```

O en la forma inspirada en James-Stein (donde el shrinkage depende del espacio
disponible en el denominador):

```
λ_JS(F)  =  c · σ² / (σ² + (7−F)/7)

→  F = 7  ⟹  7−F = 0  ⟹  λ_JS = 0  ⟹  ŵ_tiempo = 1   (sin shrinkage)
→  F = 1  ⟹  7−F = 6  ⟹  λ_JS → c·σ²/(σ² + 6/7)  ⟹  ŵ_tiempo < 1
```

**Interpretación:** cuando hay pocas "dimensiones libres" de superávit (F grande),
no hay razón para contraer el peso del tiempo hacia cero — el sistema lo usa todo.
Cuando hay muchas dimensiones libres (F pequeño), el peso del tiempo puede
"encogerse" porque el superávit de frecuencia tiene más espacio para operar.

**Propiedades:**
- Continua en F — A9 satisfecha.
- El factor `(7−F)/7 ∈ [0, 1]` funciona como proxy del "espacio de superávit de
  frecuencia disponible", que es la cantidad de información complementaria que la
  otra dimensión puede aportar.
- Es la única estructura de las cuatro que tiene justificación estadística formal:
  el shrinkage JS es óptimo en riesgo cuadrático cuando las dimensiones son
  redundantes. Acá: cuando F=1 y quedan 6 días para hacer superávit de frecuencia,
  el tiempo es "redundante" como canal de superávit — tiene sentido reducirle peso.
- Requiere elección de `c` (intensidad del shrinkage) — parámetro calibrable.
- Para la variante positiva: `[1 − λ_JS]₊` (truncada en 0) evita pesos negativos.

---

### E5 — Scheduler / annealing inverso: peso como función de "temperatura" F/7

**Forma general (inspirada en annealing):**

```
w_tiempo(F)  =  1 − exp(−k · F/7)

→  F = 0  ⟹  w_tiempo = 0   (límite teórico)
→  F = 7  ⟹  w_tiempo = 1 − e^{−k}  ≈ 1   (para k suficientemente grande)
```

O con temperatura inversa más limpia:

```
w_tiempo(F)  =  (F/7)^k      (k > 0, calibrable)

→  F = 7  ⟹  w_tiempo = 1
→  F = 1  ⟹  w_tiempo = (1/7)^k   (pequeño para k grande)
```

La idea: F/7 juega el rol de la "temperatura inversa" en annealing — a mayor F/7
(sistema "frío", pocas opciones de exploración de frecuencia), el peso del tiempo
crece como el acceptance de soluciones cercanas al óptimo.

**Propiedades:**
- Continua y monótona en F — A9.
- El exponente k controla la velocidad de transición (lineal con k=1, cóncava con
  k < 1, convexa con k > 1).
- No garantiza w_tiempo = 1 exactamente en F=7 para la forma exponencial; la forma
  potencial sí. Se elige según si se necesita el límite exacto.
- Facilita calibración separada de la velocidad de transición (k) y del máximo
  absoluto del peso (parámetro W_max multiplicador externo).

---

### E6 — Peso de distribución Cobb-Douglas / exponent share

**Forma:**

```
Rendimiento = (D/F)^{α(F)} · (media t_i/T)^{1−α(F)}

α(F) = 1 − (F/7)^γ
```

Siendo la función Cobb-Douglas multiplicativa, los exponentes son las
"participaciones" de cada factor en el output. Al hacer `α` dependiente de F,
la participación de frecuencia cae cuando F crece.

**Propiedades:**
- Continua — A9.
- Impone `α + (1−α) = 1` automáticamente — los "factores" siempre suman al 100% del
  score si rendimiento = 1.
- Multiplicativa: un factor cero mata el score completo (más penalizador que la CES).
  Posible violación de A2 si se quiere piso = 0 solo con D=0. Con D/F > 0 y t_i > 0
  el piso natural es positivo.
- Es un caso especial de CES con `ρ → 0` (elasticidad de sustitución σ = 1).

---

## §2 — Dominio de origen

| ID | Dominio | Problema que resuelve allá |
|----|---------|---------------------------|
| E1 | Redes neuronales recurrentes (GRU, LSTM) | Balancear memoria de largo plazo vs. señal nueva. El update gate aprende qué tan "viejo" mantener el estado. |
| E2 | Economía neoclásica — funciones de producción CES | Modelar la sustituibilidad entre capital y trabajo; el parámetro de distribución a controla la participación de cada factor en el output. |
| E3 | Machine learning — Mixture of Experts (Soft MoE, Google DeepMind 2023) | Enrutar tokens a expertos especializados con pesos continuos para evitar token dropping y colapso de expertos. |
| E4 | Estadística — estimador James-Stein / shrinkage adaptativo | Estimar medias en alta dimensión con error cuadrático reducido; el shrinkage depende del número de parámetros (dimensiones) disponibles. |
| E5 | Optimización combinatoria — simulated annealing | Balancear exploración vs. explotación con un parámetro de temperatura que decae; la probabilidad de aceptar soluciones subóptimas cae con la temperatura. |
| E6 | Economía — función de producción Cobb-Douglas | Modelar producción con dos factores donde la participación de cada uno en el output es fija (acá: la hacemos variable). |

### Referencias

- **GRU / update gate**: Cho et al. (2014), "Learning Phrase Representations using RNN Encoder-Decoder for Statistical Machine Translation"; resumen técnico en [The Math Behind Gated Recurrent Units — Towards Data Science](https://towardsdatascience.com/the-math-behind-gated-recurrent-units-854d88aded65/).
- **Soft MoE**: Puigcerver et al. (2023), "From Sparse to Soft Mixtures of Experts"; survey completo: [A Comprehensive Survey of Mixture-of-Experts](https://arxiv.org/html/2503.07137v1).
- **CES**: Arrow, Chenery, Minhas & Solow (1961); [Wikipedia: Constant elasticity of substitution](https://en.wikipedia.org/wiki/Constant_elasticity_of_substitution); [The CES Production Functions — Minneapolis Fed](https://www.minneapolisfed.org/-/media/files/research/prescott/macro_theory/cesprodfn.pdf).
- **James-Stein**: James & Stein (1961); [James-Stein estimator — Wikipedia](https://en.wikipedia.org/wiki/James%E2%80%93Stein_estimator); [Andrew Jones blog — detailed derivation](https://andrewcharlesjones.github.io/journal/james-stein-estimator.html).
- **Simulated annealing**: Kirkpatrick, Gelatt & Vecchi (1983); [Cornell Optimization Wiki — Simulated Annealing](https://optimization.cbe.cornell.edu/index.php?title=Simulated_annealing); Ingber (1993), temperatura schedule óptima.
- **Cobb-Douglas variable shares**: [Wikipedia: Cobb-Douglas](https://en.wikipedia.org/wiki/Cobb%E2%80%93Douglas_production_function); Jones (2003), "Growth, Capital Shares, and a New Perspective on Production Functions", [Stanford](https://web.stanford.edu/~chadj/alpha100.pdf).
- **Composite indicators con pesos variables**: OCDE (2008), [Handbook on Constructing Composite Indicators](https://www.oecd.org/content/dam/oecd/en/publications/reports/2008/08/handbook-on-constructing-composite-indicators-methodology-and-user-guide_g1gh9301/9789264043466-en.pdf).
- **Generalized mean**: [Wikipedia: Generalized mean](https://en.wikipedia.org/wiki/Generalized_mean); [Compind R package — ci_generalized_mean](https://rdrr.io/cran/Compind/man/ci_generalized_mean.html).

---

## §3 — Mapeo explícito a `D/F` y `t_i/T`

### Notación común

```
f = D/F          → fracción de días cumplidos (0 a 1+, zona compromiso)
τ = media(t_i/T) → fracción de tiempo promedio (zona compromiso + voluntaria)
φ = (7−F)/7      → "espacio libre de superávit de frecuencia" (0 en F=7, 6/7 en F=1)
```

---

### Mapeo E1 — Combinación convexa

```
Rendimiento = λ(F) · f  +  (1 − λ(F)) · τ

λ(F) = 1 − (F/7)^β     ó    λ(F) = σ(−k·(F − F₀))

Extremos:
  F=7  →  λ=0  →  Rendimiento = τ    (solo tiempo cuenta)
  F=1  →  λ≈1  →  Rendimiento ≈ f   (solo días cuenta)
```

- **A1**: rango depende de los máximos de `f` y `τ`; si ambos pueden superar 1 por
  superávit, la suma puede > 1 — compatible con Inquebrantable.
- **A3**: `D=F` y todos `t_i=T` → `f=1`, `τ=1` → Rendimiento = λ·1 + (1−λ)·1 = 1 ✓
- **A9**: λ(F) continua en F ✓
- **A10**: f y τ son razones puras ✓
- **Advertencia**: la zona voluntaria entra en τ pero no está separada en esta forma
  simple. Para separar, τ se definiría como suma de dos términos (compromiso + voluntario).

---

### Mapeo E2 — CES

```
Rendimiento = (a(F) · f^ρ + (1−a(F)) · τ^ρ)^(1/ρ)

a(F) = 1 − (F/7)^γ    (participación de la dimensión frecuencia)

Extremos:
  F=7  →  a=0  →  Rendimiento = τ  (solo tiempo)
  F=1  →  a≈1  →  Rendimiento ≈ f  (solo días)

Casos límite en ρ:
  ρ→0 (Cobb-Douglas): f^{a(F)} · τ^{1−a(F)}
  ρ→−∞ (Leontief):    min(f, τ)     sin importar el peso
  ρ=1 (lineal):       a(F)·f + (1−a(F))·τ  ≡ E1
```

- **A1**: para `ρ < 0` la CES queda en [0,1] si las entradas están en [0,1]; para
  superávit (entradas > 1) el resultado puede superar 1 — compatible.
- **A3**: `f=1`, `τ=1` → CES = (a + 1−a)^(1/ρ) = 1 ✓
- **A2**: `f=0` → término de frecuencia = 0, CES = (1−a)^(1/ρ) · τ. Problema: si a < 1
  y τ > 0, el rendimiento NO es 0 con D=0. **Requiere adaptación**: multiplicar por f
  (o usar forma condicional). Esto es la restricción de frecuencia sobre tiempo que
  menciona P1 del brief — el orquestador deberá decidir cómo aterrizarla.
- **A9**: continua en F si a(F) es continua ✓
- **A10**: f y τ son razones puras ✓

---

### Mapeo E3 — Soft MoE gating

```
s(F) = α · (F−1)/6        (normalizado: s=0 en F=1, s=α en F=7)

g_tiempo(F) = e^{s(F)} / (1 + e^{s(F)})   = σ(s(F))
g_frec(F)   = 1 − g_tiempo(F)

Rendimiento = g_frec(F) · Expert_frec(f)  +  g_tiempo(F) · Expert_tiempo(τ)
```

donde Expert_frec y Expert_tiempo son las sub-fórmulas de cada dimensión.

```
Extremos:
  F=1 → s=0 → g_tiempo = σ(0) = 0.5  (igual peso a ambos)
  F=7 → s=α → g_tiempo = σ(α)  (ajustable; para α=5, σ(5)≈0.993 ≈ 1)
```

- Para que F=7 dé g_tiempo ≈ 1 con exactitud arbitraria, se elige α grande.
- El gating no llega a 0/1 exactos salvo en el límite (α → ∞), pero puede
  acercarse tanto como se quiera — A9 satisfecha, continuidad total.
- Ventaja sobre E1/E2: los dos "expertos" se definen independientemente;
  Expert_frec puede ser no-lineal en f sin afectar la estructura del gating.

---

### Mapeo E4 — James-Stein / shrinkage por grados de libertad

```
φ = (7−F)/7       ∈ [0, 1]  ← proxy del espacio de superávit de frecuencia

Peso del superávit de tiempo:
  ŵ_tiempo(F) = 1 − λ_JS(F)
  λ_JS(F)  =  c · φ / (φ + δ)     (c, δ > 0 calibrables; forma de Stein suavizada)

Extremos:
  F=7  →  φ=0  →  λ_JS=0  →  ŵ_tiempo = 1   (sin shrinkage: tiempo lleva todo el peso)
  F=1  →  φ=6/7  →  λ_JS = c·(6/7)/(6/7 + δ)  ≈ c   (shrinkage máximo)
```

Interpretación directa: φ mide cuánto "espacio libre" tiene la frecuencia para
aportar superávit. Cuando φ = 0 (F=7), el tiempo ya no compite con nada — su peso
no se "encoge". Cuando φ es grande (F=1), hay alternativas abundantes de superávit
de frecuencia, y el tiempo puede ceder protagonismo.

- **A9**: continua en F ✓
- El shrinkage actúa sobre el peso del término de superávit, no sobre el rendimiento
  base — compatible con A2 y A3 si el score base se normaliza aparte.
- `δ` evita singularidad en φ=0 (denominador positivo siempre).
- La elección `λ_JS = c · φ` (sin δ) también es válida y más simple.

---

### Mapeo E5 — Scheduler / annealing

```
w_tiempo(F) = (F/7)^k      k > 0

Rendimiento = (1 − w_tiempo(F)) · Componente_dias  +  w_tiempo(F) · Componente_tiempo

Extremos:
  F=7  →  w_tiempo = 1     (tiempo domina)
  F=1  →  w_tiempo = (1/7)^k  (frecuencia domina; con k=2 → ≈ 0.02)
```

- k < 1: curva cóncava — transición rápida al inicio y lenta al final.
- k = 1: lineal.
- k > 1: curva convexa — transición lenta al inicio y rápida al final.
- A9 ✓. A10 ✓ (los componentes son en t_i/T y D/F).
- Análogo exacto: el annealing con schedule `T(t) = T₀ · α^t` usa el mismo
  esquema: una función monótona del parámetro de control determina el balance
  entre dos régimenes de comportamiento.

---

### Resumen de axiomas por estructura

| Axioma | E1 | E2 | E3 | E4 | E5 | E6 |
|--------|----|----|----|----|----|----|
| A1 rango [0, 1+] | ✓ | ✓ | ✓ | ✓* | ✓ | ✓ |
| A2 piso cero D=0 | ✓† | ⚠ necesita adaptación | ✓† | ✓* | ✓† | ✓ |
| A3 exacto en F=D, t=T | ✓ | ✓ | ✓ | ✓* | ✓ | ✓ |
| A4 monotonía días | ✓ | ✓ | ✓ | ✓* | ✓ | ✓ |
| A5 monotonía tiempo | ✓ | ✓ | ✓ | ✓* | ✓ | ✓ |
| A7 piso zona voluntaria | ✓† | ✓† | ✓† | ✓† | ✓† | ✓† |
| A9 continuidad / sin gates | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| A10 invarianza escala | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

*: satisfecho si los componentes interiores lo satisfacen (E4 es un modificador de
  peso, no una fórmula completa de score).
†: satisfecho si el componente de zona voluntaria se construye con piso cero
  independientemente del mecanismo de peso.
⚠: requiere una corrección externa multiplicativa tipo `f · CES(...)` o un
  factor `min(D, F)/F` que fuerce 0 cuando D=0.

---

## §4 — Explicación en lenguaje claro

### Qué buscamos

El problema central es que en una app de hábitos, el "superávit" de esfuerzo puede
llegar por dos vías: hacer más días de los comprometidos, o hacer sesiones más largas
que el objetivo. Cuando te comprometés a todos los días de la semana (F=7), la primera
vía está cerrada — no existe el día 8. La única forma de ir "más allá" es dedicar más
tiempo. La pregunta matemática es: ¿cómo hacemos que el peso de la dimensión tiempo
crezca con F de forma orgánica, sin un `if F==7 then...`?

### Qué aprendí

**Estructura 1: La palanca del GRU.** Las redes neuronales GRU resuelven exactamente
un problema análogo — decidir cuánto peso dar al pasado vs. al presente usando una
interpolación convexa con un "gate" aprendido. La clave es que `λ + (1−λ) = 1`
siempre: los dos términos suman uno. Trasladado acá: si definimos `λ(F) = 1 − (F/7)^β`,
obtenemos que a medida que F crece, la fracción del score atribuida a la dimensión
tiempo sube desde casi-0 hasta 1, suavemente, sin saltos. Es el mecanismo más simple
y limpio de los que encontré.

**Estructura 2: La función de producción CES.** En economía, cuando se modela una
empresa que usa capital y trabajo, el parámetro `a` controla qué factor "pesa más"
en el output. La CES generaliza la media aritmética (los factores se suman) y la
media geométrica (los factores se multiplican) con un parámetro `ρ` que controla
cuánto puede compensar un factor deficitario con el otro. El mismo truco sirve acá:
hacer `a = a(F)` convierte la participación de la dimensión frecuencia en una función
decreciente de F. Tiene la ventaja extra de que `ρ` y `a(F)` son knobs independientes:
uno controla la compensación entre dimensiones, el otro controla el desplazamiento del
peso según el compromiso del usuario.

**Estructura 3: Los expertos suaves.** El Soft MoE de Google es más flexible que E1:
en vez de interpolar linealmente dos dimensiones, tiene "expertos" que pueden tener
cualquier forma interna, y el gating decide a cada uno cuánto peso asignarle. La
ventaja es modularidad: la fórmula del "experto de días" y la del "experto de tiempo"
se diseñan independientemente, y el gating solo controla el balance entre ellos. El
precio: un parámetro más.

**Estructura 4: El James-Stein al revés.** El estimador James-Stein es la herramienta
clásica de la estadística para reducir el peso de estimaciones ruidosas. La intuición
clave es: cuando hay muchas "dimensiones libres" (análogo: cuando F es bajo y sobran
días para hacer superávit de frecuencia), podemos "encoger" el peso del tiempo porque
hay otras fuentes de información disponibles. Cuando esas fuentes desaparecen (F=7),
el shrinkage cae a cero y el tiempo lleva todo el peso. Es la justificación teórica
más sólida del grupo, aunque la más abstracta.

**Estructura 5: El annealing.** El simulated annealing usa la temperatura como palanca
de transición entre exploración (pesos difusos) y explotación (peso concentrado en el
mejor candidato). Trasladado acá: `F/7` juega el rol de "temperatura inversa" — a mayor
F/7, más concentrado el peso en el tiempo. La función `(F/7)^k` es la forma más simple:
convexa para k > 1, cóncava para k < 1, lineal en k=1.

### Insight transversal clave

Todas las estructuras comparten una idea: **un parámetro de control externo `F` modifica
la función de peso `λ(F)` que escala la contribución relativa de cada dimensión**. La
diferencia entre ellas es la forma funcional de λ(F) y sus propiedades en los extremos.
La más potente es la **combinación convexa** (E1) porque satisface todos los axiomas
por construcción, tiene exactamente los límites correctos (0 y 1), y es fácil de
calibrar con un solo exponente β. La CES (E2) añade un segundo grado de libertad ρ que
controla la compensación — útil si se quiere separar "cuánto pesa tiempo vs. frecuencia"
de "cuánto puede compensar uno al otro".

La observación más contraintuitiva: en la familia James-Stein, el peso del superávit
de tiempo sube cuando el "espacio libre" de la otra dimensión se **contrae** — es el
principio de escasez como motor del valor. En economía, el agua cuesta poco en el río
y mucho en el desierto. En el scoring del ancla: el superávit de tiempo vale más cuando
no hay alternativas de superávit de frecuencia disponibles.

---

*Fin del documento de research. Investigación completada por Researcher 3 el 2026-06-09.*
