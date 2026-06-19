# Research 1 — Normalización de dos métricas acopladas con dominancia regulable

> Researcher 1 | Sesión: exploración del rendimiento de un ancla (2026-06-09)
> Ángulo: cómo otros dominios combinan dos cantidades normalizadas donde una domina sin anular a la otra.

---

## §1. Estructuras encontradas

### 1.1 Función CES (Constant Elasticity of Substitution)

**Forma explícita (dos factores, pesos asimétricos):**

```
CES(x, y; α, ρ) = [α · x^ρ + (1−α) · y^ρ]^(1/ρ)
```

- `x, y ∈ [0,1]` — las dos métricas normalizadas
- `α ∈ (0,1)` — participación del factor dominante (weight share)
- `ρ ∈ (−∞, 1)` — parámetro de sustitución; controla cuánto compensa una dimensión a la otra

**Elasticidad de sustitución:** `σ = 1/(1−ρ)`

- `σ` mide cuánto puede reemplazar un factor alto al otro cuando este es bajo
- `σ → 0` (ρ → −∞): complementos perfectos → `CES = min(x, y)` (Leontief)
- `σ = 1` (ρ → 0): Cobb-Douglas → `CES = x^α · y^(1−α)`
- `σ → ∞` (ρ → 1): sustitutos perfectos → `CES = α·x + (1−α)·y`

**Propiedades:**
- Continua y diferenciable en todo el dominio interior
- Monótona creciente en ambas variables
- `CES(1, 1) = 1` exactamente para cualquier (α, ρ) bien formado
- `CES(0, ·) = 0` si ρ ≤ 0 (garantía de piso cero cuando x = 0)
- Superávit (x > 1 o y > 1): la función sube naturalmente por encima de 1

**Control de dominancia:**
- `α` controla cuál factor pesa más dentro de la media
- `ρ` (o `σ`) controla **si** la debilidad de uno puede compensarse con el otro
- Zona de interés para este problema: `ρ < 0` (σ < 1), donde la sustitución es baja, es decir, un factor bajo no se compensa con el otro alto — sin llegar a Leontief puro

---

### 1.2 F-beta Score (Media armónica ponderada)

**Forma explícita:**

```
F_β(P, R) = (1 + β²) · P · R / (β² · P + R)
```

Equivalente como media armónica ponderada de P y R con peso relativo β²:

```
1/F_β = [1/(1+β²)] · (1/P) + [β²/(1+β²)] · (1/R)
```

O en forma simétrica de la media armónica pesada:

```
F_β = 1 / [λ/P + (1−λ)/R],  donde  λ = 1/(1+β²)
```

- `P, R ∈ [0,1]` — las dos métricas (aquí: precisión, recall)
- `β > 0` — parámetro de dominancia: β > 1 favorece R, β < 1 favorece P

**Casos límite:**
- `β → 0`: `F_β → P` (solo importa precisión; recall ignorado)
- `β = 1`: F1, media armónica simétrica
- `β → ∞`: `F_β → R` (solo importa recall)

**Propiedades:**
- Continua, diferenciable, en [0,1] si P, R ∈ [0,1]
- `F_β(0, ·) = F_β(·, 0) = 0` — piso cero garantizado
- `F_β(1, 1) = 1` — cumplimiento exacto = 1
- Monótona creciente en P y R
- Penaliza fuertemente los desequilibrios: si una dimensión es muy baja, jala el resultado hacia abajo de forma supralineal (propiedad distintiva de la media armónica frente a la aritmética)
- No soporta superávit (P, R > 1) sin adaptación: requiere rango extendido

**Control de dominancia:** `β` ajusta la asimetría; β² es exactamente cuántas veces pesa más una dimensión que la otra.

---

### 1.3 Media generalizada (Power Mean) con pesos

**Forma explícita:**

```
M_p(x, y; w) = [w · x^p + (1−w) · y^p]^(1/p)
```

- `x, y ∈ [0,1]` — métricas normalizadas
- `w ∈ (0,1)` — peso de la dimensión dominante
- `p ∈ ℝ \ {0}` — orden de la media (controla compensación)

**Casos especiales:**

| p       | Media obtenida         | Comportamiento                                      |
|---------|------------------------|-----------------------------------------------------|
| p → −∞  | Mínimo                 | Compensación cero: todo depende del peor            |
| p = −1  | Armónica ponderada     | Alta penalización de desequilibrios                 |
| p → 0   | Geométrica ponderada   | Penalización moderada (HDI usa este caso)           |
| p = 1   | Aritmética ponderada   | Compensación plena; un alto compensa un bajo        |
| p = 2   | Cuadrática (RMS)       | Énfasis en valores altos                            |
| p → +∞  | Máximo                 | Solo el mayor importa                               |

**Monotonía fundamental:** Si p < q entonces M_p ≤ M_q (con igualdad solo si x = y).

**Propiedades:**
- Para p < 0: una variable muy baja jala fuertemente el resultado hacia abajo
- Para p > 0: una variable muy alta puede compensar una baja (en la misma proporción)
- `M_p(1, 1) = 1` para todo p
- `M_p(0, y)` con p < 0: resultado = 0 (piso cero garantizado por el límite)

**Control de dominancia:**
- `w` controla cuál dimensión pesa más (a igualdad de magnitudes)
- `p` controla qué tan "reemplazable" es una dimensión por la otra

*Nota: la CES es algebraicamente equivalente a la media generalizada pesada con ρ = p y α = w; son la misma familia vista desde dos dominios distintos.*

---

### 1.4 Media geométrica ponderada (Caso HDI, ρ → 0)

**Forma explícita:**

```
G(x, y; α) = x^α · y^(1−α)
```

- `x, y ∈ [0,1]` — dimensiones normalizadas
- `α ∈ (0,1)` — exponente/peso de la dimensión dominante

**Propiedades:**
- `G(0, y) = 0` para cualquier y: si la dimensión dominante es cero, el índice es cero
- `G(1, 1) = 1` exactamente
- Penalización de desequilibrios: menor que media aritmética, mayor que media cuadrática
- No permite compensación total: una dimensión en 0 colapsa el resultado
- Soporta x > 1 naturalmente (superávit eleva el índice por encima de 1)
- Diferenciable en todo el interior del dominio

**Por qué el HDI migró de aritmética a geométrica (2010):** con media aritmética, un país con ingreso muy alto pero salud muy baja obtenía un HDI alto. La geométrica impone "imperfect substitutability" — los avances en una dimensión contribuyen menos si las otras ya están saturadas.

**Control de dominancia:** solo `α` ajusta el peso; la geometría misma impone la complementariedad sin parámetro adicional.

---

### 1.5 Media de Lehmer

**Forma explícita:**

```
L_p(x, y) = (x^p + y^p) / (x^(p−1) + y^(p−1))
```

Con pesos `w`:

```
L_p(x, y; w) = (w·x^p + (1−w)·y^p) / (w·x^(p−1) + (1−w)·y^(p−1))
```

**Casos especiales:**

| p     | Media resultante   |
|-------|--------------------|
| 0     | Armónica           |
| 1/2   | Geométrica (solo para n=2) |
| 1     | Aritmética         |
| 2     | Contraarmónica     |
| → −∞  | Mínimo             |
| → +∞  | Máximo             |

**Propiedad de monotonía en p:** si p ≤ q, entonces L_p ≤ L_q (estrictamente para x ≠ y).

**Comportamiento asimétrico:**
- p < 1: favorece los valores más pequeños (la dimensión baja arrastra el resultado)
- p > 1: favorece los valores más grandes (la dimensión alta potencia el resultado)
- La contraarmónica (p=2) es la única media que pesa más los valores grandes que la aritmética

**Diferencia con la media generalizada:** en la Lehmer media, el parámetro p aparece en tanto numerador como denominador, lo que produce una curva de ponderación diferente — especialmente útil cuando se quiere énfasis en valores altos sin explotar hacia ∞.

---

### 1.6 OWA (Ordered Weighted Averaging)

**Forma explícita:**

```
OWA(x, y; w₁, w₂) = w₁ · max(x, y) + w₂ · min(x, y),  con w₁ + w₂ = 1, wᵢ ≥ 0
```

Para dos variables, los pesos se asignan a la posición ordenada, no a la variable específica.

**Parámetro de optimismo ("orness"):**

```
A-C(W) = w₁  (para n=2)
```

- `w₁ = 1`: toma el máximo (optimista puro)
- `w₁ = 0.5`: media aritmética
- `w₁ = 0`: toma el mínimo (pesimista puro — sin compensación)

**Propiedades:**
- Continua, monótona
- Casos especiales: max, min, media aritmética, son todos instancias del OWA
- No fija qué variable domina; asigna peso a la *posición* (mayor/menor) — quien esté más alto recibe w₁
- `OWA(0, ·)`: si una variable es 0 y w₂ > 0, el resultado puede ser > 0 (diferencia con media armónica)

**Control de dominancia:** el "orness" controla si la dimensión mejor (sin importar cuál) domina. No controla qué dimensión específica domina — diferencia clave frente a CES y F-beta.

---

## §2. Dominio de origen

| Estructura | Dominio de origen | Problema que resuelve allá |
|------------|------------------|---------------------------|
| **CES** | Economía (teoría de producción, Arrow-Chenery-Minhas-Solow 1961) | ¿Cuánto puede el capital sustituir al trabajo en la producción? La elasticidad de sustitución mide esta flexibilidad. Se usa en modelos de crecimiento (Solow), comercio internacional, energía. |
| **F-beta** | Machine Learning / recuperación de información (Van Rijsbergen 1979) | ¿Cómo combinar precisión (calidad) y recall (cobertura) cuando una importa más que la otra? El F1 es la media armónica simétrica; F-beta añade la asimetría controlada. |
| **Media generalizada (Power Mean)** | Matemática pura y estadística (Cauchy, Holder, Hardy) | Unificar todas las medias escalares bajo un solo parámetro. Aplicada en física (energía cinética promedio), teoría de información, índices compuestos, señales. |
| **Media geométrica / HDI** | Estadística económica (UNDP, 2010) | Combinar dimensiones de desarrollo humano (salud, educación, ingreso) sin permitir compensación total entre ellas. El cambio de aritmética a geométrica en 2010 fue deliberado para penalizar desequilibrios. |
| **Media de Lehmer** | Matemática aplicada / estadística (Lehmer 1971) | Generalizar la familia de medias con un parámetro que controla énfasis en valores grandes vs pequeños, con comportamiento diferente a la potencia. Usada en teoría de señales y redes. |
| **OWA** | Lógica difusa / decisión multicriterio (Yager 1988) | Agregar múltiples criterios con diferentes actitudes de optimismo. La clave es que el peso va a la *posición* en el ranking, no a la variable fija — permite políticas tipo "el mejor de dos" o "el peor de dos". |

**Referencias canónicas:**
- Arrow, K.J. et al. (1961). "Capital-Labor Substitution and Economic Efficiency." *Review of Economics and Statistics*, 43(3), 225–250.
- Van Rijsbergen, C.J. (1979). *Information Retrieval*, 2nd ed. Butterworth.
- Hardy, G.H., Littlewood, J.E., Pólya, G. (1934). *Inequalities*. Cambridge University Press.
- UNDP (2010). *Human Development Report 2010*. Technical notes, HDI methodology.
- Yager, R.R. (1988). "On ordered weighted averaging aggregation operators in multicriteria decision making." *IEEE Trans. Systems, Man, and Cybernetics*, 18(1), 183–190.
- Lehmer, D.H. (1971). "On the compounding of certain means." *Journal of Mathematical Analysis and Applications*, 36(1), 183–200.

---

## §3. Mapeo explícito a D/F y t_i/T

En todos los casos, las dos variables de entrada del problema son:

- **x = D/F** : razón de frecuencia (días completados / días comprometidos); `x ∈ [0, 1+]`
- **y = τ** : razón de tiempo promedio del día de compromiso; `τ = (1/F) Σᵢ∈compromiso tᵢ/T ∈ [0, 1+]`

La frecuencia debe dominar. Un día con tiempo trivial no activa la dominancia.

---

### 3.1 CES aplicada al problema

**Instanciación:**

```
rendimiento_compromiso(D/F, τ; α, ρ) = [α · (D/F)^ρ + (1−α) · τ^ρ]^(1/ρ)
```

Con `α > 0.5` (frecuencia tiene más peso) y `ρ < 0` (elasticidad σ < 1, baja sustitución).

**Qué parámetro controla la dominancia frecuencia↔tiempo:**
- `α`: cuánto pesa cada dimensión a igual magnitud
- `ρ` (o `σ = 1/(1-ρ)`): si tiempo bajo puede compensarse con frecuencia alta — con ρ < 0, NO puede: hay complementariedad

**Axiomas satisfechos naturalmente:**
- A1 (Normalización): ✓ — rango [0, 1+] si las entradas están en [0, 1+]
- A2 (Piso cero): ✓ — `CES(0, τ) = 0` cuando ρ < 0 (límite hacia Leontief)
- A3 (Cumplimiento exacto = 1): ✓ — `CES(1, 1) = 1` siempre
- A4 (Monotonía días): ✓ — creciente en D/F
- A5 (Monotonía tiempo): ✓ — creciente en τ
- A9 (Continuidad): ✓ — función suave sin gates
- A10 (Invarianza de escala): ✓ — opera solo sobre razones

**Axioma que requiere adaptación:**
- A7 (Piso zona voluntaria): la CES no distingue zona de compromiso de zona voluntaria por sí sola — la separación en zonas (Best-F / voluntaria) debe hacerse antes de aplicar la CES, que opera sobre la componente de compromiso

**Comportamiento del ejemplo testigo:**
- Caso A: 1 día × 40 min (T=30) → D/F = 1/F, τ = 40/30 = 1.33
- Caso B: 2 días × 1 min (T=30) → D/F = 2/F, τ = 1/30 ≈ 0.03
- Con F=3, ρ=-1, α=0.7: Caso A = [0.7·(0.33)^{-1} + 0.3·(1.33)^{-1}]^{-1} ≈ 0.39; Caso B = [0.7·(0.67)^{-1} + 0.3·(0.03)^{-1}]^{-1} ≈ 0.07 → Caso A gana ✓

---

### 3.2 F-beta aplicada al problema

**Instanciación:**

```
rendimiento_compromiso(D/F, τ; β) = (1 + β²) · (D/F) · τ / (β² · (D/F) + τ)
```

Con `β > 1` (recall/frecuencia domina sobre precisión/tiempo).

**Qué parámetro controla la dominancia:**
- `β`: directamente. `β = 2` significa que D/F importa el doble que τ. `β → ∞` converge a D/F puro.

**Axiomas satisfechos naturalmente:**
- A1: ✓ — si D/F ≤ 1 y τ ≤ 1, el resultado ≤ 1 (fuera de superávit)
- A2 (Piso cero): ✓ — `F_β(0, τ) = 0` siempre
- A3: ✓ — `F_β(1, 1) = 1` siempre
- A4 y A5 (Monotonías): ✓ — media armónica es creciente en ambas variables
- A9 (Continuidad): ✓
- A10 (Invarianza): ✓

**Limitación para superávit (A1 extendido):**
- La media armónica estándar no sube por encima de 1 cuando los inputs son ≤ 1
- Para soportar superávit (D/F > 1 no aplica en zona de compromiso, pero τ > 1 sí), se necesita rango extendido

**Observación importante:** la media armónica penaliza agresivamente los desequilibrios. Con β=2 y τ muy pequeño (tiempo trivial), el resultado colapsa aun si D/F es alto — esto es exactamente lo que buscamos: un día con 1 min sobre T=30 tiene τ = 0.03, y la media armónica lo castiga fuertemente. El ejemplo testigo se resuelve naturalmente.

---

### 3.3 Media generalizada (Power Mean) aplicada al problema

**Instanciación:**

```
rendimiento_compromiso(D/F, τ; w, p) = [w · (D/F)^p + (1−w) · τ^p]^(1/p)
```

Con `w > 0.5` y `p < 0`.

**Nota:** para p → 0, colapsa a la geométrica `(D/F)^w · τ^(1−w)`.

**Qué parámetro controla la dominancia:**
- `w`: peso estructural de la frecuencia
- `p`: grado de complementariedad/sustituibilidad

**Axiomas:**
- Todos los axiomas satisfechos para p < 0 (ver análisis CES — son isomorfos)
- Para p → 0 (geométrica): A2 garantizado (si D/F = 0, producto = 0)
- Para p > 0: compensación posible, puede violar el espíritu de A2 en bordes

**Ventaja sobre CES:** la familia completa (de min a max pasando por harmónica, geométrica, aritmética) se controla con un solo parámetro continuo, lo que facilita calibración.

---

### 3.4 Media geométrica ponderada (Cobb-Douglas) aplicada al problema

**Instanciación:**

```
rendimiento_compromiso(D/F, τ; α) = (D/F)^α · τ^(1−α)
```

Con `α > 0.5`.

**Caso límite de CES con ρ = 0.** Propiedades derivadas:
- `rendimiento(0, τ) = 0` — si no hay días, rendimiento es cero ✓
- `rendimiento(D/F, 0) = 0` — si tiempo es cero, rendimiento es cero ✓ (pero: un día con 1 min de τ = 1/30 da rendimiento (D/F)^α · (0.03)^{0.5} ≈ bajo — útil)
- `rendimiento(1, 1) = 1` ✓
- Soporta superávit naturalmente: `(1.2)^α · (1.1)^{1−α} > 1` ✓
- Sin parámetro de elasticidad separado: solo `α` controla todo

**Axiomas:** todos satisfechos directamente.

**Limitación:** la geométrica no distingue entre "poca frecuencia con mucho tiempo" y "mucha frecuencia con poco tiempo" de forma asimétrica suficiente — la elasticidad fija en σ=1 puede ser insuficiente.

---

### 3.5 Media de Lehmer aplicada al problema

**Instanciación (para dos variables con pesos):**

```
rendimiento_compromiso(D/F, τ; w, p) = [w·(D/F)^p + (1−w)·τ^p] / [w·(D/F)^(p−1) + (1−w)·τ^(p−1)]
```

**Para p = 0 (armónica ponderada):**

```
rendimiento = [w·(D/F)^0 + (1−w)·τ^0] / [w·(D/F)^{-1} + (1−w)·τ^{-1}]
            = 1 / [w/(D/F) + (1−w)/τ]
```

Que es exactamente la media armónica ponderada = WHAM del punto anterior.

**Para p = 2 (contraarmónica):** pondera los valores más altos — potencia el superávit naturalmente.

**Axiomas:** similares a la media generalizada para p ≤ 1.

**Diferencia operativa con Power Mean:** el parámetro p produce curvas de compensación distintas en los bordes (la Lehmer media tiene un "sesgo" diferente cuando una variable es mucho mayor que la otra).

---

### 3.6 OWA aplicada al problema

**Instanciación:**

```
rendimiento_compromiso(D/F, τ; w₁) = w₁ · max(D/F, τ) + (1−w₁) · min(D/F, τ)
```

**Limitación fundamental para este problema:** el OWA no distingue qué variable es frecuencia y cuál es tiempo — asigna el peso mayor a quien esté más alto en ese día. Si τ > D/F, τ recibe el peso mayor, lo que viola el requerimiento de que la frecuencia domine estructuralmente.

**Uso posible:** como componente dentro de la zona voluntaria (donde no hay dirección de dominancia fija), no como agregador principal.

---

## §4. Explicación en lenguaje claro

### La pregunta central y el vocabulario que encontré

El problema es cómo combinar dos cosas que importan juntas pero de forma desigual: la frecuencia de días (cuántas veces hiciste el hábito) y el tiempo promedio por sesión (cuánto hiciste cuando lo hiciste). Queremos que la frecuencia mande, pero que no ignore al tiempo — especialmente cuando el tiempo es ridículo (1 minuto sobre un objetivo de 30).

La matemática tiene una respuesta vieja para esto. Es el mismo problema que enfrenta un economista cuando pregunta "¿cuánto puede el capital sustituir al trabajo en una fábrica?" o el informático cuando pondera "¿cuándo importa más no perder resultados que ser preciso?". En todos esos casos, el parámetro que regula la respuesta es la **elasticidad de sustitución**.

### Las tres familias y lo que ofrecen

**La familia CES / Media Generalizada** (son la misma cosa con distinto vocabulario):

Tiene dos perillas: `w` (cuánto pesa cada dimensión a igual magnitud) y `p` o `σ` (qué tan reemplazable es una dimensión por la otra). Con `p < 0`, el producto se acerca al mínimo — nadie puede compensar a nadie. Con `p = 0`, obtenés la media geométrica (que es lo que usa el HDI). Con `p = 1`, aritmética pura. La perilla `p` es el corazón: es exactamente el parámetro que controla si un día con tiempo trivial puede activar su "día contado". En `p < 0`, no puede: el tiempo bajo arrastra el resultado aunque la frecuencia sea alta.

**El F-beta score / Media armónica ponderada**:

Tiene una sola perilla: `β`. Es la versión más simple y elegante. La media armónica es la más punitiva de las medias clásicas: si una dimensión es 0.03 (1 minuto sobre T=30), la media armónica la arrastra ferozmente aunque la otra dimensión sea 1. `β` regula qué dimensión importa más. El caso `β = 2` dice "frecuencia importa cuatro veces más que el tiempo". Esta estructura satisface todos los axiomas del brief de forma natural, incluyendo piso cero, cumplimiento exacto = 1, y monotonías. La única limitación: el superávit (valores > 1) necesita manejo explícito.

**La media geométrica ponderada (Cobb-Douglas)**:

Es el caso bisagra entre "todo compensa" y "nada compensa". Con `α = 0.7`, la frecuencia tiene mucho más peso. Si cualquiera de las dos dimensiones es cero, el producto es cero (piso garantizado). Soporta superávit de forma natural. No tiene un parámetro separado de elasticidad — α hace todo el trabajo. Es la más simple de calibrar.

### La intuición del parámetro de dominancia

La clave del vocabulario que encontré es que **dominancia y compensación son dos cosas distintas y separables**:

- **Dominancia** = cuánto pesa cada dimensión. Se controla con `α` o `w`. Si `α = 0.8`, la frecuencia pesa cuatro veces más que el tiempo cuando ambas están en valores iguales.
- **Compensación** = si una dimensión alta puede tapar a una baja. Se controla con `p` (o `ρ`, o `σ`). Si `p = -1` (media armónica), ninguna compensa. Si `p = 1` (media aritmética), compensan al 100%.

El brief pide que frecuencia **domine** pero sin **compensar totalmente**: esto es exactamente `α > 0.5` (o `β > 1`) combinado con `p < 0` (o `σ < 1`) — los dos parámetros trabajan en ejes ortogonales.

### El caso del HDI como precedente práctico de diseño

El UNDP enfrentó exactamente este dilema en 2010: con media aritmética, un país rico podía compensar su mala salud con su alto ingreso. Migraron a media geométrica. Esto es `p → 0` en la familia de la media generalizada. La intuición es la misma: queremos que "todo el mundo tiene que aportar" y no que "uno puede tapar al otro". La media geométrica es el punto de equilibrio entre compensación total (aritmética) y complementariedad perfecta (mínimo de Leontief).

### Lo que no encontré y vale la pena señalar

La separación entre zona de compromiso y zona voluntaria (Best-F) no tiene un análogo directo en estos dominios. Ninguna estructura revisada distingue internamente dos sub-poblaciones de la misma variable (los F mejores días vs los D-F restantes). Eso es propio de este sistema y requiere composición: las estructuras aquí documentadas se aplican sobre la salida de la lógica de zonas, no la reemplazan.

---

## Referencias

- Arrow, K.J., Chenery, H.B., Minhas, B.S., & Solow, R.M. (1961). Capital-Labor Substitution and Economic Efficiency. *Review of Economics and Statistics*, 43(3), 225–250.
- Van Rijsbergen, C.J. (1979). *Information Retrieval*, 2nd ed. Butterworth-Heinemann.
- Hardy, G.H., Littlewood, J.E., & Pólya, G. (1934). *Inequalities*. Cambridge University Press.
- UNDP (2010). *Human Development Report 2010: The Real Wealth of Nations*. Technical Note 1.
- Yager, R.R. (1988). On ordered weighted averaging aggregation operators in multicriteria decision making. *IEEE Transactions on Systems, Man, and Cybernetics*, 18(1), 183–190.
- Lehmer, D.H. (1971). On the compounding of certain means. *Journal of Mathematical Analysis and Applications*, 36(1), 183–200.
- Bullen, P.S. (2003). *Handbook of Means and Their Inequalities*. Springer.
- Herrera, F., Herrera-Viedma, E., & Verdegay, J.L. (1996). Direct approach processes in group decision making using linguistic OWA operators. *Fuzzy Sets and Systems*, 79(2), 175–190.
- Greco, S., Ishizaka, A., Tasiou, M., & Torrisi, G. (2019). On the Methodological Framework of Composite Indices: A Review of Weighting, Aggregation, and Robustness. *Social Indicators Research*, 141(2), 61–94. doi:10.1007/s11205-017-1832-9.
- Foster, J., McGillivray, M., & Seth, S. (2009). Rank Robustness of Composite Indices. OPHI Working Paper 26.
- Wikipedia: [Constant elasticity of substitution](https://en.wikipedia.org/wiki/Constant_elasticity_of_substitution)
- Wikipedia: [F-score](https://en.wikipedia.org/wiki/F-score)
- Wikipedia: [Generalized mean](https://en.wikipedia.org/wiki/Generalized_mean)
- Wikipedia: [Lehmer mean](https://en.wikipedia.org/wiki/Lehmer_mean)
- Wikipedia: [Ordered weighted averaging aggregation operator](https://en.wikipedia.org/wiki/Ordered_weighted_averaging_aggregation_operator)
