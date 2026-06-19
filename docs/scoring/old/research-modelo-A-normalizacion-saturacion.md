> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Research: Modelo A — Normalización y Saturación de una Variable

> **Tipo:** Documento de investigación — base para diseño del motor de scoring  
> **Fecha:** 2026-06-08  
> **Ángulo:** Forma de la curva `f(r)` para una sola variable de rendimiento

---

## 1. Resumen ejecutivo

Dada la razón de rendimiento `r = hecho/meta` de una variable, necesitamos una función continua `f(r)` que: (a) penalice suavemente el déficit (`r < 1`) con piso emergente en 0, (b) mapee el cumplimiento en `r = 1` a un punto de referencia natural, y (c) sature con rendimientos decrecientes el superhábit (`r > 1`) hacia un techo blando. La investigación muestra que la **función de Hill** (también llamada Michaelis-Menten generalizada) y la **función de potencia tipo CRRA** son las candidatas más limpias: ambas son continuas, analíticas, implementables con `exp`/`pow`, y producen piso y techo **emergentes de la forma**, sin ningún `min()`/`max()` ni rama lógica. La función de Hill tiene la ventaja adicional de ser una sola fórmula que cubre todo el dominio positivo.

---

## 2. Familias de funciones candidatas

### 2.1 Función de Hill / Michaelis-Menten generalizada

**Origen:** Bioquímica enzimática (Hill 1910, Michaelis-Menten 1913). Ampliamente adoptada en marketing mix modeling (MMM) para modelar saturación de gasto publicitario.

**Fórmula canónica (forma Hill, normalizada a [0, 1]):**

```
f(r) = r^n / (K^n + r^n)
```

O en forma equivalente con la constante de media-saturación en el denominador:

```
f(r) = 1 / (1 + (K/r)^n)
```

**Parámetros y qué controla cada uno:**

| Parámetro | Rango | Efecto |
|-----------|-------|--------|
| `K` | > 0 | Half-saturation point: el valor de `r` donde `f(r) = 0.5`. Controla dónde está "la rodilla" de la curva. Si queremos que la meta (`r=1`) sea el punto de media-saturación, fijamos `K = 1`. |
| `n` | > 0 | Hill coefficient / exponente de cooperatividad. Con `n = 1`: curva hiperbólica suave (Michaelis-Menten puro). Con `n > 1`: curva sigmoidal más pronunciada, transición más abrupta alrededor de `K`. Con `n < 1`: curva cóncava, sube rápido al principio y luego satura más gradualmente. |

**Con `K = 1` (meta como punto de media-saturación):**

```
f(r) = r^n / (1 + r^n)
```

- `f(0) = 0` — el piso es exactamente 0 cuando no se hace nada. Emerge de la forma, no de un clamp.
- `f(1) = 0.5` — la meta mapea a exactamente la mitad del máximo.
- `f(∞) → 1` — el techo es 1, asintótico. Emerge de la forma, nunca alcanzado.
- Toda la curva es estrictamente creciente, continua, diferenciable en todo su dominio.

**Comportamiento verbal de la curva:**
- Debajo de la meta: crece como potencia `r^n` cuando `r` es pequeño (para `r << K`), luego desacelera. No hay penalización explícita; el valor simplemente es bajo.
- En la meta: punto de inflexión natural (para `n > 1`) o punto de curvatura (para `n = 1`).
- Sobre la meta: rendimientos fuertemente decrecientes. Doblar la meta (`r = 2`) no dobla la contribución.

**Ejemplos numéricos con `K = 1`, varios `n`:**

| `r` | `f(r)` con `n=1` | `f(r)` con `n=2` | `f(r)` con `n=0.5` |
|-----|-----------------|-----------------|-------------------|
| 0.25 | 0.200 | 0.059 | 0.333 |
| 0.50 | 0.333 | 0.200 | 0.414 |
| 1.00 | 0.500 | 0.500 | 0.500 |
| 1.50 | 0.600 | 0.692 | 0.550 |
| 2.00 | 0.667 | 0.800 | 0.586 |
| 3.00 | 0.750 | 0.900 | 0.634 |

Interpretación: con `n=2` la penalización del déficit es más severa (0.25 meta → solo 5.9% del máximo) y la saturación del superhábit es más rápida. Con `n=1` hay un gradiente continuo lineal-ish. Con `n=0.5` la curva sube rápido y luego es muy plana.

**Ajuste del punto de referencia de meta:**
Si se quiere que `r=1` mapee a un valor distinto de 0.5 (por ej., a 0.7 — "meta = bueno pero no el techo"), se puede usar `K ≠ 1`. Por ejemplo, `K = 0.43` hace que `f(1) ≈ 0.7`.

**¿El piso y el techo emergen de la forma?** Sí, completamente. `f(0) = 0` algebraicamente; `f(∞) = 1` como límite. Cero ramas.

**Implementación Kotlin:**
```kotlin
fun hill(r: Double, k: Double = 1.0, n: Double = 1.0): Double {
    val rn = r.pow(n)
    val kn = k.pow(n)
    return rn / (kn + rn)
}
```

---

### 2.2 Función de potencia tipo CRRA (isoelástica)

**Origen:** Economía — utilidad con aversión relativa al riesgo constante (CRRA, Constant Relative Risk Aversion). Ampliamente usada en teoría de la utilidad, teoría prospectiva y modelos de toma de decisiones.

**Fórmula base (solo ganancias, dominio `r > 0`):**

```
f(r) = r^α    con  0 < α < 1
```

- `α < 1`: cóncava — rendimientos decrecientes. Cada unidad adicional de `r` contribuye menos que la anterior.
- `α = 1`: lineal.
- `α > 1`: convexa — rendimientos crecientes (no útil para el caso superhábit).

**Forma completa CRRA (normalizada, `r > 0`):**

```
f(r) = (r^(1-η) - 1) / (1 - η)   para η ≠ 1
f(r) = ln(r)                       para η = 1
```

donde `η > 0` es el parámetro de aversión al riesgo / curvatura. A mayor `η`, más cóncava la función, más penalización relativa del déficit y más saturación del superhábit.

**Problema:** esta forma no está acotada superiormente. Para `r → ∞`, `r^α → ∞` (aunque crece lento). Para usar como contribución a un score `[0,1]` hay que normalizarla o combinarla con otro enfoque.

**Variante de la teoría prospectiva (Tversky-Kahneman 1992):**

```
v(x) = x^α              para x ≥ 0 (ganancias)
v(x) = -λ · (-x)^β      para x < 0 (pérdidas)
```

Con parámetros empíricos estimados: `α = β ≈ 0.88`, `λ ≈ 2.25`.

Si se reinterpreta `x = r - 1` (desviación desde la meta), la función cubre déficit y superhábit en una sola expresión, con asimetría: las pérdidas pesan 2.25x más que las ganancias equivalentes.

**Ejemplos numéricos con `α=0.88`, `λ=2.25`, `x = r-1`:**

| `r` | `x = r-1` | `v(x)` |
|-----|-----------|--------|
| 0.25 | -0.75 | -2.25 × 0.75^0.88 = -1.72 |
| 0.50 | -0.50 | -2.25 × 0.50^0.88 = -1.22 |
| 1.00 | 0.00 | 0.00 |
| 1.50 | 0.50 | 0.50^0.88 = 0.535 |
| 2.00 | 1.00 | 1.00^0.88 = 1.00 |
| 3.00 | 2.00 | 2.00^0.88 = 1.84 |

**Observación clave:** el output no está acotado. Para `r=3`, `v = 1.84` — y puede seguir creciendo. Hay rendimientos decrecientes (la función es cóncava) pero NO hay saturación dura hacia un techo finito. Tampoco hay piso natural en 0 para el lado déficit (los valores son negativos).

**¿El piso y el techo emergen de la forma?** Parcialmente. Hay rendimientos decrecientes pero no saturación acotada hacia un techo. Para nuestro caso (contribución `[0,1]`) requiere transformación adicional o normalización, lo que introduce una operación extra aunque no sea un `min()`/`max()` duro.

**Implementación Kotlin:**
```kotlin
fun prospectValue(r: Double, alpha: Double = 0.88, lambda: Double = 2.25): Double {
    val x = r - 1.0
    return if (x >= 0) x.pow(alpha) else -lambda * (-x).pow(alpha)
}
```

---

### 2.3 Función logística (sigmoide estándar)

**Fórmula:**

```
f(r) = 1 / (1 + exp(-k · (r - r₀)))
```

**Parámetros:**

| Parámetro | Efecto |
|-----------|--------|
| `k` | Pendiente / steepness. Controla qué tan rápido transiciona de 0 a 1. A mayor `k`, más abrupta la transición. |
| `r₀` | Punto de inflexión: donde `f(r₀) = 0.5`. Para que la meta sea el punto central, `r₀ = 1`. |

**Comportamiento:**
- `f(-∞) = 0`, `f(+∞) = 1` — los límites emergen de la forma exponencial.
- Simétrica alrededor de `r₀`.
- Para `r = 0` con `r₀ = 1`, `k = 3`: `f(0) = 1/(1+e³) ≈ 0.047` — no llega exactamente a 0.
- Curva en S: primero convexa (debajo), luego cóncava (arriba).

**Problema para nuestro caso:** La logística es simétrica — trata el déficit y el superhábit con la misma "velocidad" de cambio. Eso es deseable en clasificación, pero en scoring de hábitos queremos asimetría: la zona de superhábit satura más rápido que el déficit crece. Además, `f(0) ≠ 0` exactamente — hay siempre un valor residual positivo aunque `r = 0` (nunca se hizo nada), lo que puede ser conceptualmente incorrecto.

**Ejemplos numéricos con `k=3, r₀=1`:**

| `r` | `f(r)` |
|-----|--------|
| 0.25 | 0.090 |
| 0.50 | 0.182 |
| 1.00 | 0.500 |
| 1.50 | 0.818 |
| 2.00 | 0.910 |
| 3.00 | 0.982 |

**¿El piso y el techo emergen?** Sí, asintóticamente. Pero `f(0) ≈ 0.047 ≠ 0` — hay valor residual no nulo incluso con actividad cero.

---

### 2.4 Función tanh (tangente hiperbólica)

**Fórmula:**

```
f(r) = tanh(k · (r - r₀))   →   rango (-1, 1)
```

O reescalada a [0, 1]:

```
f(r) = (1 + tanh(k · (r - r₀))) / 2
```

**Relación con la logística:** `tanh(x) = 2σ(2x) - 1`, es la versión centrada en 0. Mismas propiedades generales que la logística pero simétrica respecto al eje y. Para nuestro contexto, tiene la misma limitación de simetría que la logística.

**¿El piso y el techo emergen?** Sí, asintóticamente. Misma limitación: `f(0) ≠ 0` exactamente para la versión reescalada.

---

### 2.5 Saturación exponencial — `1 - exp(-k·r)`

**Fórmula:**

```
f(r) = 1 - exp(-k · r)
```

**Parámetros:**

| Parámetro | Efecto |
|-----------|--------|
| `k` | Tasa de saturación. A mayor `k`, más rápido se alcanza el techo. Controla la "rodilla" de la curva. |

**Comportamiento:**
- `f(0) = 0` — exactamente 0 en origen. Piso emergente.
- `f(∞) → 1` — techo asintótico emergente.
- Función cóncava en todo su dominio (rendimientos siempre decrecientes).
- Sin punto de inflexión: la pendiente es máxima en `r = 0` y decrece monótonamente.

**Problema:** No tiene un punto de referencia natural de "meta completa". Con `k = ln(2) ≈ 0.693`, `f(1) = 0.5` — la meta mapea a la mitad del máximo, similar a Hill con `n=1`. Pero la curva es siempre cóncava: no puede modelar una zona de "subida rápida hasta la meta y luego saturación" — ese comportamiento sigmoidal requiere convexidad inicial (que Hill con `n > 1` sí tiene).

**Ejemplos numéricos con `k=0.693` (para `f(1)=0.5`):**

| `r` | `f(r)` |
|-----|--------|
| 0.25 | 0.161 |
| 0.50 | 0.293 |
| 1.00 | 0.500 |
| 1.50 | 0.647 |
| 2.00 | 0.750 |
| 3.00 | 0.875 |

**¿El piso y el techo emergen?** Sí, completamente. La función es analítica, diferenciable en todo su dominio, sin ramas.

---

### 2.6 Función de Gompertz

**Fórmula:**

```
f(r) = A · exp(-exp(-s · (r - r₀)))
```

**Parámetros:**

| Parámetro | Efecto |
|-----------|--------|
| `A` | Asíntota superior (máximo). |
| `s` | Tasa de crecimiento. |
| `r₀` | Punto de inflexión (aproximadamente 36.8% de la asíntota — asimétrico). |

**Característica distinctive:** Sigmoide asimétrica — la inflexión ocurre al 36.8% de la asíntota (a diferencia de la logística donde es al 50%). Esto la hace útil cuando la saturación llega más rápido después del pico de crecimiento.

**Comportamiento:**
- `f(-∞) → 0`, `f(+∞) → A`. Piso y techo emergentes.
- Curva S asimétrica: crece despacio → muy rápido → satura gradualmente.
- Su asimetría es fija por la estructura de la función; es menos controlable que Hill.

**Complejidad:** dos exponenciales anidados. Más costosa computacionalmente y más difícil de interpretar los parámetros. Para nuestro caso ofrece menos ventajas que Hill con menos complejidad adicional.

---

### 2.7 Smoothstep / Smootherstep

**Fórmula Smoothstep (cúbico, Hermite):**

```
f(t) = 3t² - 2t³    para t ∈ [0, 1]
```

**Fórmula Smootherstep (quíntico, Ken Perlin):**

```
f(t) = 6t⁵ - 15t⁴ + 10t³    para t ∈ [0, 1]
```

donde `t = r / r_max` (variable normalizada al máximo esperado).

**Propiedades:**
- `f(0) = 0`, `f(1) = 1`.
- Derivadas primera (y segunda para smootherstep) iguales a 0 en los extremos — transición suavísima.
- Diseñado para interpolación en `[0, 1]`, no para dominio infinito.

**Problema para nuestro caso:** Su dominio está definido para `t ∈ [0, 1]`. Para `r > r_max`, la función debe evaluarse fuera de dominio — lo que introduce necesariamente un clamp o una extensión manual. El piso y techo son exactos en los extremos del dominio definido, pero el comportamiento fuera de `[0, 1]` no está definido por la fórmula. **Requiere un clamp externo para `r > 1` si se usa como `t = r`**, lo que viola el núcleo "sin gates".

---

### 2.8 Softplus (suavizado de ReLU)

**Fórmula:**

```
f(x) = (1/a) · ln(1 + exp(a · x))
```

Con `a = 1`: `f(x) = ln(1 + eˣ)`.

**Propiedades:**
- Aproximación suave de `max(0, x)` — no acotado superiormente.
- Completamente diferenciable; derivada = `σ(x)` (sigmoide).
- Para nuestro caso: no tiene techo, no satura. Solo útil como componente intermedio (ej., para construir un piso suave antes de otra transformación).

**No es candidata directa** para `f(r)` porque no tiene saturación superior. Es útil para construir el "piso suave" de manera que valores negativos se mapeen a algo ≥ 0 sin clamp duro, pero necesita composición con otra función.

---

## 3. Comparativa de rendimientos decrecientes + saturación

| Función | Piso exacto en 0 | Techo asintótico | RD + saturación limpia | Asimetría déficit/superhábit | Parámetros |
|---------|-----------------|-----------------|----------------------|------------------------------|------------|
| **Hill** `r^n/(1+r^n)` | ✅ (algebraico) | ✅ (→1) | ✅ Excelente | ✅ Controlable con `n` | 2 (`K`, `n`) |
| **Exp. saturation** `1-e^(-kr)` | ✅ (exacto) | ✅ (→1) | ✅ Buena | ❌ Siempre cóncava, sin inflexión | 1 (`k`) |
| **CRRA** `r^α` | ✅ (en r=0) | ❌ No acotada | Parcial (RD sí, techo no) | ❌ No cubre déficit natively | 1 (`α`) |
| **Prospect Theory** | ✅ en r=1 | ❌ No acotada | Parcial | ✅ λ asimetría | 3 (`α, β, λ`) |
| **Logística** | ❌ Residuo ≠ 0 | ✅ (→1) | ✅ | ❌ Simétrica | 2 (`k, r₀`) |
| **tanh reescalado** | ❌ Residuo ≠ 0 | ✅ (→1) | ✅ | ❌ Simétrica | 2 (`k, r₀`) |
| **Gompertz** | ✅ (asintótico) | ✅ (→A) | ✅ | Fija (36.8%) | 3 (`A, s, r₀`) |
| **Smoothstep** | ✅ en extremos | ✅ en extremos | ✅ en [0,1] | ❌ Requiere clamp fuera de [0,1] | 0 (fija) |

---

## 4. Respeto al núcleo "sin gates"

**¿El piso en 0 y el techo emergen de la forma, o requieren clamp?**

- **Hill `r^n/(K^n + r^n)`**: Piso y techo 100% emergentes. `f(0) = 0` algebraicamente porque el numerador es `0^n = 0`. El techo `f(∞) = 1` emerge del límite. **Ningún `min()`/`max()` necesario.** Implementación de una sola línea aritmética.

- **Saturación exponencial `1 - exp(-kr)`**: Piso `f(0) = 1 - e⁰ = 0` exactamente algebraico. Techo emergente como límite. **Sin gates.**

- **CRRA `r^α`**: Piso `f(0) = 0` solo si `α > 0` (y la función está definida para `r ≥ 0`). Sin techo — se necesita una normalización posterior. **Requiere transformación extra para estar en [0,1]**.

- **Prospect Theory**: Sin piso en 0 ni techo en 1 naturales. La función toma valores en (-∞, +∞) relativos a la meta. **Requiere transformación para uso directo como contribución**.

- **Logística y tanh**: Piso y techo asintóticos (→0 y →1) pero nunca exactamente 0 para `r` finito. Para `r = 0` hay siempre un valor residual positivo. En la práctica esto puede ser aceptable (el residuo es muy pequeño con `k` grande), pero no es algebraicamente exacto. **Sin gates explícitos pero con residuo conceptual en origen.**

- **Smoothstep**: Requiere normalización del input a `[0,1]`. Para `r > r_max` la función está fuera de dominio — **requiere clamp externo**. Viola el núcleo.

- **Softplus**: Sin techo. **No viable directo.**

---

## 5. Recomendación: las 2 mejores `f(r)`

### Recomendación principal: Función de Hill con `K = 1`

```
f(r) = r^n / (1 + r^n)
```

**Por qué es la mejor:**

1. **Una sola fórmula continua** para todo `r ≥ 0`. Cubre déficit y superhábit sin ramas.
2. **Piso exacto en 0** algebraicamente. **Techo asintótico en 1** emergente.
3. **Control preciso de "la rodilla"** con solo dos parámetros:
   - `n = 1`: hiperbólica suave. Penalización suave del déficit.
   - `n = 2`: sigmoidal. Penalización más severa del déficit; saturación más rápida del superhábit.
   - `n = 0.5`: sube rápido desde el principio, luego muy plana. Útil si no quiero penalizar fuerte el déficit pero tampoco premiar mucho el superhábit.
4. **Probada industrialmente** en marketing mix modeling exactamente para este problema (gasto vs rendimiento con saturación).
5. **El punto de referencia `r = 1` mapea siempre a `f(1) = 0.5`** — exactamente la mitad del máximo. Si se quiere que la meta mapee a otro valor de referencia (ej. 0.7), se ajusta `K ≠ 1`.

**Rangos de parámetros sugeridos:**
- `K = 1.0` (meta como media-saturación) — ajustar si se quiere un punto de referencia diferente.
- `n ∈ [0.5, 3.0]`:
  - `n ≈ 1.0`: comportamiento hiperbólico, penalización suave.
  - `n ≈ 1.5`: leve sigmoide, buen balance.
  - `n ≈ 2.0–3.0`: sigmoide pronunciada, penalización fuerte del déficit y saturación rápida del superhábit (anti-gaming fuerte).

**Implementación Kotlin:**
```kotlin
fun hill(r: Double, k: Double = 1.0, n: Double = 1.5): Double =
    r.pow(n) / (k.pow(n) + r.pow(n))
```

---

### Recomendación secundaria: Saturación exponencial `1 - exp(-k·r)`

```
f(r) = 1 - exp(-k · r)
```

**Por qué es la segunda opción:**

1. **Piso exacto en 0**, **techo asintótico en 1**, todo emergente.
2. **Un solo parámetro** — más simple que Hill.
3. Siempre cóncava — cada unidad adicional de `r` aporta menos. Modela rendimientos decrecientes de manera natural.
4. Más simple matemáticamente: una sola exponencial.

**Limitación vs Hill:** No tiene punto de inflexión. La pendiente es siempre máxima en `r = 0` y decrece monótonamente. Esto significa que la penalización del déficit es más "suave" cerca de 0 que Hill con `n > 1`, y no hay un tramo inicial convexo que modele "hacer algo está bien, hacer la meta está mejor".

**Rangos de parámetros sugeridos:**
- Para que `f(1) = 0.5`: `k = ln(2) ≈ 0.693`.
- Para que `f(1) = 0.7`: `k = -ln(0.3) ≈ 1.204`.
- Para saturación más rápida (superhábit llega a techo más pronto): `k ∈ [1.5, 3.0]`.

**Implementación Kotlin:**
```kotlin
fun expSaturation(r: Double, k: Double = 0.693): Double =
    1.0 - exp(-k * r)
```

---

### Combinación avanzada: Hill + asimetría tipo Prospect Theory

Para casos donde se quiere **asimetría explícita** entre el peso del déficit y el superhábit (ej. el dolor de estar al 50% pesa más que el premio de estar al 200%), se puede combinar ambas familias:

```
f(r) = hill(r, K=1, n=n_gain)       para r ≥ 1  (superhábit — saturación suave)
f(r) = hill(r, K=1, n=n_loss) × λ   para r < 1  (déficit — penalización más pronunciada)
```

Pero esto introduce una rama (`if r ≥ 1`). La alternativa pura sin ramas es **usar Hill con `n` que ya produzca la asimetría deseada**: con `n = 2`, la curva ya penaliza más el déficit que lo que premia el superhábit de manera natural (la pendiente relativa en la zona de déficit es mayor que en la zona de superhábit para el mismo delta de `r`).

---

## 6. Fuentes consultadas

- [Michaelis-Menten / Hill equation — Mathematics LibreTexts](https://math.libretexts.org/Bookshelves/Calculus/Differential_Calculus_for_the_Life_Sciences_(Edelstein-Keshet)/01:_Power_functions_as_building_blocks/1.05:_Rate_of_an_enzyme-catalyzed_reaction)
- [Hill Function in Marketing Mix Modeling — Medium / Rajiv Gopinath](https://medium.com/@mail2rajivgopinath/hill-function-and-its-implementation-a-strategic-overview-2ffd443f92a0)
- [Isoelastic (CRRA) Utility — Wikipedia](https://en.wikipedia.org/wiki/Isoelastic_utility)
- [Prospect Theory — Wikipedia](https://en.wikipedia.org/wiki/Prospect_theory)
- [Prospect Theory Value Function (pedagogical) — jcx.au](https://be.jcx.au/prospect-theory/value-function)
- [Cumulative Prospect Theory — Tversky & Kahneman 1992, vía psych.fullerton.edu](https://psych.fullerton.edu/mbirnbaum/psych466/articles/Tversky_Kahneman_JRU_92.pdf)
- [Smoothstep — Wikipedia](https://en.wikipedia.org/wiki/Smoothstep)
- [Softplus — Wikipedia](https://en.wikipedia.org/wiki/Softplus)
- [Gompertz Function — Wikipedia](https://en.wikipedia.org/wiki/Gompertz_function)
- [Sigmoid Functions: The Ultimate ML Guide — NumberAnalytics](https://www.numberanalytics.com/blog/sigmoid-functions-ultimate-ml-guide)
- [Soft Clipping — HackAudio](https://hackaudio.com/digital-signal-processing/distortion-effects/soft-clipping/)
- [Marketing Mix Modeling — PyMC Labs Guide](https://www.pymc-labs.com/blog-posts/marketing-mix-modeling-a-complete-guide)
- [Saturating Non-Linearities — Baeldung CS](https://www.baeldung.com/cs/saturating-non-linearities)
- [New Framework for Marketing Mix Modeling — arXiv 2311.05587](https://arxiv.org/pdf/2311.05587)
- [Explaining the Characteristics of the Power (CRRA) Utility Family — ResearchGate](https://www.researchgate.net/publication/5637416_Explaining_the_Characteristics_of_the_Power_CRRA_Utility_Family)
