> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Research: Modelo B — Agregación entre ejes/componentes sin gates

> Documento de investigación — 2026-06-08  
> Propósito: identificar operadores matemáticos de agregación que produzcan comportamiento emergente (peor arrastra sin colapsar, parejo gana, días pesan más que minutos) SIN reglas lógicas, caps, gates ni `if/else`. Todo debe emerger de la forma continua de la función y sus parámetros.

---

## 1. Resumen ejecutivo

La **media de potencia ponderada** (power mean, Hölder mean) con exponente `p < 1` es el corazón de la respuesta. Un solo número `p` controla de forma continua y sin branches cuánto arrastra el valor más bajo: `p = 1` es promedio aritmético (sin arrastre), `p = 0` es media geométrica (arrastre moderado; cero colapsa), `p → −∞` tiende al mínimo estricto. El rango `p ∈ [−2, −0.5]` da exactamente el comportamiento pedido: la peor componente arrastra, pero una sola en cero con el resto perfecto NO colapsa el agregado, y el reparto parejo gana frente a la concentración. Para combinar **días × minutos** dentro de una ancla, la **función CES** (Constant Elasticity of Substitution) separa con precisión quirúrgica el peso relativo de cada factor y la elasticidad de sustitución entre ambos: con `α_días = 0.7` y `σ < 1` (baja sustituibilidad), los días dominan y los minutos solo ayudan marginalmente. Estos dos operadores son implementables en Kotlin puro con `pow`, `log`, `exp`.

---

## 2. Medias de potencia (Power Means / Medias de Hölder)

### 2.1 Fórmula

Para `n` valores `x₁, …, xₙ ∈ (0, 1]` y exponente `p ∈ ℝ \ {0}`:

```
Mₚ(x₁, …, xₙ) = ( (x₁ᵖ + x₂ᵖ + ⋯ + xₙᵖ) / n ) ^ (1/p)
```

Caso límite `p = 0` (media geométrica, via L'Hôpital):

```
M₀(x₁, …, xₙ) = (x₁ · x₂ · ⋯ · xₙ)^(1/n)
```

Casos límite extremos:

```
p → +∞  →  max(x₁, …, xₙ)
p → −∞  →  min(x₁, …, xₙ)
```

### 2.2 Versión ponderada

Para pesos `w₁, …, wₙ ≥ 0` con `Σwᵢ = 1`:

```
Mₚ^w(x₁, …, xₙ) = ( w₁·x₁ᵖ + w₂·x₂ᵖ + ⋯ + wₙ·xₙᵖ ) ^ (1/p)
```

Nota: la potencia final es `1/p` directamente porque los pesos ya suman 1.

### 2.3 Casos especiales por valor de p

| p       | Nombre               | Comportamiento                        |
|---------|----------------------|---------------------------------------|
| +∞      | Máximo               | Domina el mejor valor                 |
| +2      | Cuadrática (RMS)     | Sesgo hacia valores altos             |
| +1      | Aritmética           | Promedio simple; compensación total   |
| 0       | Geométrica           | Arrastre moderado; cero colapsa total |
| −1      | Armónica             | Arrastre fuerte; robusto              |
| −2      | —                    | Arrastre muy fuerte; cero impacta     |
| −∞      | Mínimo               | Solo importa el peor                  |

**Propiedad clave (monotonicidad en p):** Si `p < q`, entonces `Mₚ ≤ Mq`, con igualdad solo cuando todos los componentes son idénticos. Esto implica que cuanto más negativo es `p`, más castiga el desbalance y más premia el reparto parejo — de forma continua y sin ninguna rama lógica.

### 2.4 Ejemplos numéricos

#### (a) Una componente en 0, resto perfectas: `{1.0, 1.0, 0.0}`

Para este caso, `0ᵖ = 0` para todo `p > 0`, pero la suma de las otras componentes puede dominar.

| p   | Cálculo                                                          | Resultado |
|-----|------------------------------------------------------------------|-----------|
| +1  | (1.0 + 1.0 + 0.0) / 3 = 2/3                                    | 0.667     |
| 0   | (1·1·0)^(1/3) = 0                                              | **0.000** ← COLAPSO |
| −0.5| (1^−0.5 + 1^−0.5 + 0^−0.5)^(−2) → diverge por 0^−0.5 → ∞    | **→ 0** ← COLAPSO |
| −1  | 3 / (1 + 1 + 1/0) → diverge                                    | **→ 0** ← COLAPSO |

**Conclusión crítica:** La media de potencia pura colapsa a 0 cuando un componente es exactamente 0 para `p ≤ 0`. Este es el mismo problema del `min()` pero disfrazado. La solución: **nunca permitir valores de entrada exactamente 0** — se trabaja en el rango `(ε, 1]` con `ε` pequeño (ej. 0.05), que representa "el mínimo detectable". En la práctica, `0 días = ε`, no `0` literal.

#### (a') Misma prueba con `{1.0, 1.0, 0.05}` (cero suavizado a ε=0.05)

| p    | Cálculo                                                                      | Resultado |
|------|------------------------------------------------------------------------------|-----------|
| +1   | (1.0 + 1.0 + 0.05) / 3                                                      | **0.683** |
| 0    | (1.0 · 1.0 · 0.05)^(1/3) = 0.05^(1/3)                                     | **0.368** |
| −0.5 | ((1^−0.5 + 1^−0.5 + 0.05^−0.5)/3)^(1/−0.5) = ((1+1+4.47)/3)^−2 = (2.156)^−2 | **0.215** |
| −1   | 3 / (1/1.0 + 1/1.0 + 1/0.05) = 3/(1+1+20) = 3/22                          | **0.136** |
| −2   | ((1^−2 + 1^−2 + 0.05^−2)/3)^(1/−2) = ((1+1+400)/3)^−0.5 = (134)^−0.5    | **0.086** |

Observar: ningún valor colapsa a 0. El arrastre es graduado y controlado por `p`. Más negativo = más arrastre pero no colapso.

#### (b) Parejo vs. concentrado con mismo promedio: `{0.5, 0.5, 0.5}` vs `{1.0, 0.5, 0.0+ε}`

Para `{1.0, 0.5, 0.05}` (representando concentración: un día excelente, uno medio, uno casi nulo):

| p    | {0.5, 0.5, 0.5}                    | {1.0, 0.5, 0.05}                                               | ¿Parejo gana? |
|------|-------------------------------------|------------------------------------------------------------------|---------------|
| +1   | 0.500                               | (1.0+0.5+0.05)/3 = 0.517                                       | No (promedio favorece concentración en este caso) |
| 0    | 0.5^(1/3)·3 no... = 0.500         | (1.0·0.5·0.05)^(1/3) = 0.025^(1/3) ≈ 0.293                   | **Sí, parejo gana** |
| −0.5 | ((0.5^−0.5·3)/3)^−2 = 0.5^1 = 0.500 | ((1^−0.5+0.5^−0.5+0.05^−0.5)/3)^−2 = ((1+1.414+4.47)/3)^−2 = (2.295)^−2 ≈ 0.190 | **Sí, parejo gana fuerte** |
| −1   | 3/(0.5^−1·3) = 3/6 = 0.500        | 3/(1/1+1/0.5+1/0.05) = 3/(1+2+20) = 3/23 ≈ 0.130             | **Sí, parejo gana mucho más** |
| −2   | (0.5^−2·3/3)^−0.5 = 4^−0.5 = 0.500 | ((1+4+400)/3)^−0.5 = (135)^−0.5 ≈ 0.086                      | **Sí, parejo gana** |

**Conclusión:** Para cualquier `p < 1`, la distribución pareja `{0.5,0.5,0.5}` supera a la concentrada `{1.0, 0.5, 0.05}` incluso cuando el promedio de la concentrada es mayor. El efecto crece a medida que `p` se vuelve más negativo. Este es el premio a la constancia emergiendo matemáticamente sin ningún `if`.

#### (c) Prueba de transición p=−1 mostrando gradación fina: `{0.8, 0.6, 0.4}` vs `{1.0, 0.5, 0.3}`

(Ambos vectores tienen suma ≈ 1.8, media aritmética ≈ 0.6)

| p   | {0.8, 0.6, 0.4}                                            | {1.0, 0.5, 0.3}                                               |
|-----|-------------------------------------------------------------|---------------------------------------------------------------|
| +1  | 0.600                                                       | 0.600 (empate)                                               |
| 0   | (0.8·0.6·0.4)^(1/3) = 0.192^(1/3) ≈ 0.577                | (1.0·0.5·0.3)^(1/3) = 0.15^(1/3) ≈ 0.531                   |
| −1  | 3/(1/0.8+1/0.6+1/0.4) = 3/(1.25+1.67+2.5) = 3/5.42 ≈ 0.554 | 3/(1+2+3.33) = 3/6.33 ≈ 0.474                              |

El vector más parejo siempre gana para `p < 1`, y la ventaja crece con p más negativo.

### 2.5 Implementación Kotlin

```kotlin
fun powerMean(values: List<Double>, p: Double, weights: List<Double>? = null): Double {
    val w = weights ?: List(values.size) { 1.0 / values.size }
    return if (p == 0.0) {
        // Media geométrica ponderada
        exp(w.zip(values).sumOf { (wi, xi) -> wi * ln(xi) })
    } else {
        w.zip(values).sumOf { (wi, xi) -> wi * xi.pow(p) }.pow(1.0 / p)
    }
}
```

### 2.6 Respeto al núcleo sin gates

El efecto "peor arrastra sin colapsar" emerge del exponente `p`:
- Para `p ∈ (−2, −0.5)` el arrastre es real pero ningún componente en el rango `(0.05, 1.0]` puede colapsar el total.
- No hay `if`, no hay `min()`, no hay gate. La función es continua y diferenciable en todo su dominio.

---

## 3. Función CES (Constant Elasticity of Substitution)

### 3.1 Fórmula para dos factores

Para combinar `días` (D) e `intensidad` (I) en el valor de una ancla:

```
A(D, I) = ( α · D^ρ + (1−α) · I^ρ ) ^ (1/ρ)
```

Donde:
- `α ∈ (0, 1)` — participación/peso relativo de días (D)
- `ρ ∈ (−∞, 1)` — parámetro de sustitución
- `σ = 1 / (1 − ρ)` — elasticidad de sustitución (cuánto puede I compensar D)

### 3.2 Casos especiales

| ρ      | σ = 1/(1−ρ)  | Forma resultante               | Interpretación                              |
|--------|--------------|--------------------------------|---------------------------------------------|
| ρ → 1  | σ → ∞        | Linear: α·D + (1−α)·I         | Sustitución perfecta (minutos compensan días) |
| ρ → 0  | σ = 1        | Cobb-Douglas: D^α · I^(1−α)   | Elasticidad unitaria                        |
| ρ → −∞ | σ → 0        | Leontief: min(D, I)            | Complementariedad perfecta (sin sustitución) |
| ρ < 0  | σ < 1        | Complementos (sustituibilidad baja) | Lo que queremos: días dominan           |

### 3.3 Qué controla cada parámetro

- **α (peso de días):** Controla quién domina estructuralmente. Con `α = 0.7`, los días aportan 70% de la "fuerza" en el agregado. Los minutos nunca pueden compensar días completamente.
- **ρ (o equivalentemente σ):** Controla cuánto pueden los minutos compensar la falta de días. Con `σ < 1` (ρ < 0), la sustituibilidad es baja: si no hay días, ni muchos minutos ayudan. Con `σ > 1` (ρ > 0), los minutos sí pueden sustituir la constancia.

### 3.4 Ejemplos numéricos (α = 0.7, distintos ρ)

Escenario 1: **Días altos, minutos bajos** → D=0.8, I=0.3  
Escenario 2: **Días bajos, minutos altos** → D=0.3, I=0.8  
(Queremos que escenario 1 supere claramente a escenario 2)

| ρ     | σ    | A(0.8, 0.3)                                                                         | A(0.3, 0.8)                                                                         | Días ganaron? |
|-------|------|--------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|---------------|
| +0.5  | 2.0  | (0.7·0.8^0.5 + 0.3·0.3^0.5)^2 = (0.7·0.894+0.3·0.548)^2 = (0.626+0.164)^2 = 0.625 | (0.7·0.3^0.5+0.3·0.8^0.5)^2 = (0.383+0.268)^2 = 0.423                             | Sí, leve      |
| 0     | 1.0  | 0.8^0.7 · 0.3^0.3 = 0.867 · 0.696 = 0.603                                          | 0.3^0.7 · 0.8^0.3 = 0.411 · 0.930 = 0.382                                          | Sí, moderado  |
| −1    | 0.5  | (0.7·0.8^−1 + 0.3·0.3^−1)^−1 = (0.875+1.0)^−1 = 1/1.875 = 0.533                  | (0.7·0.3^−1 + 0.3·0.8^−1)^−1 = (2.333+0.375)^−1 = 1/2.708 = 0.369               | Sí, fuerte    |
| −3    | 0.25 | (0.7·0.8^−3 + 0.3·0.3^−3)^(−1/3) = (0.7/0.512 + 0.3/0.027)^(−1/3) = (1.367+11.11)^(−1/3) = 12.48^(−1/3) ≈ 0.432 | (0.7·0.3^−3+0.3·0.8^−3)^(−1/3) = (0.7/0.027+0.3/0.512)^(−1/3) = (25.93+0.586)^(−1/3) = 26.5^(−1/3) ≈ 0.284 | Sí, muy fuerte |

Con `ρ = −1` (σ = 0.5), "Días altos/minutos bajos" supera a "Días bajos/minutos altos" en 44% (0.533 vs 0.369). Con `ρ = −3` (σ = 0.25) la brecha es del 52%. La constancia (días) domina de forma emergente, sin ningún gate.

### 3.5 CES normalizada a [0,1]

La función CES retorna valores en el rango del input pero puede no ser exactamente 1 cuando D=I=1. Para casos `ρ ≠ 0`:

```
A(D=1, I=1) = (α·1 + (1−α)·1)^(1/ρ) = 1^(1/ρ) = 1  ✓
A(D=0.05, I=0.05) → 0.05  ✓
```

La función ya está acotada en `[0,1]` si los inputs están en `(0,1]`. No necesita normalización adicional.

### 3.6 Implementación Kotlin

```kotlin
fun cesAggregation(days: Double, intensity: Double, alpha: Double, rho: Double): Double {
    return if (rho == 0.0) {
        // Caso Cobb-Douglas
        days.pow(alpha) * intensity.pow(1.0 - alpha)
    } else {
        (alpha * days.pow(rho) + (1.0 - alpha) * intensity.pow(rho)).pow(1.0 / rho)
    }
}
```

### 3.7 Respeto al núcleo sin gates

La CES es una función continua y analítica en `(0,1]`. El efecto "días pesan más que minutos" emerge de `α` y `ρ`, no de ninguna condición. La sustituibilidad baja (`ρ < 0`) hace que los minutos no puedan compensar falta de días — emergente, sin `min()`, sin branches.

---

## 4. Media Geométrica Ponderada (Cobb-Douglas)

### 4.1 Fórmula

La media geométrica ponderada es el caso `p = 0` de la media de potencia ponderada, equivalente al producto Cobb-Douglas:

```
G_w(x₁, …, xₙ) = x₁^w₁ · x₂^w₂ · ⋯ · xₙ^wₙ    (con Σwᵢ = 1)
```

En logaritmo (más estable numéricamente):

```
ln G_w = w₁·ln(x₁) + w₂·ln(x₂) + ⋯ + wₙ·ln(xₙ)
```

### 4.2 Propiedades de agregación

1. **Penaliza imbalance:** Si cualquier componente baja, el producto cae. La penalización es multiplicativa: un `x₁ = 0.5` reduce el producto a la mitad respecto a `x₁ = 1.0`, independientemente de los otros.
2. **Premio al reparto parejo:** La AM-GM inequality garantiza que `G_w ≤ M₁^w` (media aritmética ponderada), con igualdad solo cuando todos son iguales. Ergo, para la misma media aritmética, el reparto más parejo tiene media geométrica más alta.
3. **Sensibilidad a cero:** `G_w = 0` si cualquier componente es exactamente 0 → usar siempre `ε > 0` como mínimo.

### 4.3 Ejemplo: agregar n capas con pesos distintos

Capas: Interior (I), Cuerpo (Cu), Conducta (Co), Vínculos (V), Proyecto (P)  
Pesos propuestos: `w = [0.25, 0.25, 0.20, 0.15, 0.15]`

Escenario A (perfecto en todo excepto una capa mala):  
`scores = [1.0, 1.0, 1.0, 1.0, 0.1]`  
`G_w = 1.0^0.25 · 1.0^0.25 · 1.0^0.20 · 1.0^0.15 · 0.1^0.15 = 0.1^0.15 ≈ 0.708`

Escenario B (parejo mediocre):  
`scores = [0.7, 0.7, 0.7, 0.7, 0.7]`  
`G_w = 0.7^1.0 = 0.700`

Observar: la media geométrica no colapsa con una capa mala (da 0.708 ≈ 0.700). Pero la media geométrica es la misma. Para que "una capa muy mala arrastre más", necesitamos `p < 0` en la media de potencia.

### 4.4 Implementación Kotlin

```kotlin
fun weightedGeometricMean(scores: List<Double>, weights: List<Double>): Double {
    return exp(scores.zip(weights).sumOf { (x, w) -> w * ln(x) })
}
```

---

## 5. OWA (Ordered Weighted Averaging)

### 5.1 Fórmula

Para `n` valores y vector de pesos `w = (w₁, …, wₙ)` con `Σwᵢ = 1`:

1. Ordenar los valores de mayor a menor: `b₁ ≥ b₂ ≥ ⋯ ≥ bₙ`
2. Calcular: `OWA_w(x) = Σⱼ wⱼ · bⱼ`

Los pesos se asignan a **posiciones en el ranking**, no a componentes específicos.

### 5.2 Casos especiales

| Pesos         | Comportamiento                  |
|---------------|---------------------------------|
| w=(1,0,…,0)   | Máximo                          |
| w=(0,0,…,1)   | Mínimo                          |
| w=(1/n,…,1/n) | Media aritmética                |
| w=(0,1,0,…,0) | Mediana (si n impar)            |

**Orness:** `A-C(w) = [1/(n-1)] · Σ(n-j)·wⱼ` mide cuán cercano al máximo (1) o mínimo (0) está el operador. Valor 0.5 = media aritmética.

### 5.3 Diseño para "peor arrastra sin colapsar"

Pesos OWA que enfatizan las posiciones inferiores (baja orness):  
Para n=3: `w = (0.1, 0.2, 0.7)` — el valor más bajo recibe el 70% del peso.

Ejemplo: `{1.0, 0.8, 0.2}`  
- Ordenados: `[1.0, 0.8, 0.2]`  
- OWA: `0.1×1.0 + 0.2×0.8 + 0.7×0.2 = 0.10 + 0.16 + 0.14 = 0.40`

Con pesos uniformes: `(1.0+0.8+0.2)/3 = 0.667`

El OWA con ese diseño reduce fuertemente la influencia de los valores altos y da 40% vs 66.7%.

### 5.4 Limitación OWA para este proyecto

OWA asigna pesos por **posición en el ranking**, no por identidad. Esto rompe la interpretación semántica: no se puede decir "Interior pesa 0.25" de forma estable — si Interior es la peor capa un día, recibe el peso de posición 3; si es la mejor, recibe el peso de posición 1. **No recomendado** para agregar capas con pesos semánticos fijos.

---

## 6. Soft Minimum (log-sum-exp suavizado)

### 6.1 Fórmula

El soft minimum de dos variables con temperatura `t > 0`:

```
softmin(u, v; t) = −(1/t) · ln( e^(−t·u) + e^(−t·v) )
```

Para `n` variables:

```
softmin(x₁,…,xₙ; t) = −(1/t) · ln( Σ e^(−t·xᵢ) )
```

**Propiedad:** `min(x) − ln(n)/t ≤ softmin(x; t) ≤ min(x)`

A medida que `t → ∞`, converge al mínimo estricto. Para `t` pequeño, se acerca al promedio.

### 6.2 Ejemplo numérico

Valores: `{1.0, 1.0, 0.2}`, temperatura `t = 5`:

```
softmin = −(1/5)·ln(e^−5 + e^−5 + e^−1)
        = −0.2·ln(0.00674 + 0.00674 + 0.368)
        = −0.2·ln(0.3815)
        = −0.2·(−0.964)
        = 0.193
```

Para comparación, `min = 0.2`, `aritmética = 0.733`.

Con `t = 2`:
```
softmin = −0.5·ln(e^−2 + e^−2 + e^−0.4)
        = −0.5·ln(0.135 + 0.135 + 0.670)
        = −0.5·ln(0.940)
        = −0.5·(−0.062)
        = 0.031  ← muy agresivo con t=2 sobre este rango
```

### 6.3 Evaluación para el proyecto

El softmin es diferenciable y no usa `if`. Sin embargo, su output **no está acotado a [0,1]** de forma natural y la escala de `t` depende del rango de los datos, haciendo el calibrado difícil. No es el candidato principal para este proyecto — la media de potencia es más intuitiva y directamente acotada.

---

## 7. Dispersión como modulador externo (sin integrar en la media)

### 7.1 Coeficiente de variación (CV)

```
CV = σ(x) / μ(x)
```

Se puede usar como un modulador suave adicional:

```
score_final = Mₚ(x₁,…,xₙ) · (1 − λ·CV)
```

Con `λ ∈ (0, 0.3)` para que el módulo de penalización sea suave. Esto actúa como penalización extra al desbalance, encima de lo que ya hace `p < 0`.

### 7.2 Entropía de Shannon como uniformidad

Para `n` componentes normalizadas como proporciones `pᵢ = xᵢ/Σxᵢ`:

```
H = −Σ pᵢ · ln(pᵢ)     (máximo: ln(n) cuando todos iguales)
Uniformidad = H / ln(n)  ∈ [0,1]
```

Este modulador vale 1 cuando el reparto es perfectamente parejo y cae cuando hay concentración. Se puede multiplicar por el score o usar como peso adicional.

**Advertencia:** Estos moduladores añaden un factor más al modelo — son opcionales y se recomiendan solo si la media de potencia sola no da suficiente diferenciación al calibrar.

---

## 8. Síntesis: sin gates en todos los operadores

| Operador            | ¿Peor arrastra?  | ¿Sin colapso?               | ¿Sin gates?                | ¿Parejo gana? |
|---------------------|------------------|-----------------------------|----------------------------|---------------|
| Media pot. p<0      | Sí (continuo)    | Sí, si ε>0 en dominio       | Sí — solo pow+suma         | Sí            |
| CES ρ<0             | No (para 2 fact) | Sí                          | Sí — solo pow+suma         | N/A para 2    |
| Geométrica pond.    | Sí (moderado)    | No si hay cero exacto       | Sí — solo pow+ln           | Sí (AM-GM)    |
| OWA                 | Depende del diseño | Sí                         | Sí — solo suma ordenada    | Depende       |
| Softmin             | Muy sí           | Sí (suave)                  | Sí — solo exp+log          | N/A           |

---

## 9. RECOMENDACIÓN

### Operador 1 (recomendado para agregar capas): Media de potencia ponderada con `p ∈ [−1.5, −0.5]`

**Fórmula final para agregar `n` capas:**

```
ScoreTotal = ( Σᵢ wᵢ · scoreCapaᵢ^p ) ^ (1/p)
```

Con:
- `p = −1.0` (media armónica ponderada) como punto de partida de calibración
- `wᵢ` = pesos semánticos fijos por capa (ej. Interior=0.25, Cuerpo=0.25, …)
- Dominio de cada `scoreCapaᵢ ∈ [ε, 1]` con `ε = 0.05` (nunca 0 exacto)

**Emergencias garantizadas sin gates:**
1. Una capa mala arrastra el total — no collapsa, pero reduce significativamente.
2. Cinco capas en 0.6 (parejo) > cuatro en 1.0 + una en 0.2 (concentrado), para el mismo promedio.
3. Los pesos `wᵢ` expresan la importancia estructural de cada capa; el exponente `p` controla el castigo al desbalance. Son dos palancas independientes.

**Calibración sugerida:**
- Empezar en `p = −1.0`. Si el castigo al desbalance parece excesivo, subir hacia `p = −0.5`. Si parece insuficiente, bajar hacia `p = −2.0`.

### Operador 2 (recomendado para combinar días × minutos dentro de una ancla): CES con `α = 0.7`, `ρ = −1` (σ = 0.5)

**Fórmula final para valor de una ancla:**

```
valorAncla(D, I) = ( 0.7 · D^(−1) + 0.3 · I^(−1) )^(−1)
                 = 1 / ( 0.7/D + 0.3/I )
```

Esta es la media armónica ponderada entre días e intensidad, que es exactamente CES con ρ=−1. Es simple, implementable con una línea, y produce:

- `D=0.8, I=0.3` → `1/(0.875 + 1.0) = 1/1.875 ≈ 0.533` ← días altos ganan
- `D=0.3, I=0.8` → `1/(2.333 + 0.375) = 1/2.708 ≈ 0.369` ← días bajos pierden
- `D=0.8, I=0.8` → `1/(0.875 + 0.375) = 1/1.25 = 0.800` ← consistente en ambos = máximo
- `D=1.0, I=1.0` → `1/(0.7 + 0.3) = 1.0` ← perfecto = 1 ✓

**Emergencias:**
1. Días dominan (α=0.7): es imposible compensar 0 días con muchos minutos.
2. Sustituibilidad baja (σ=0.5, ρ=−1): los dos factores son complementarios — necesitás ambos para un buen score, pero no en proporciones iguales.
3. Sin gates, sin `if`, sin `min()`.

**Parámetros a ajustar:**
- `α` controla cuánto pesan días vs minutos; `α = 0.65-0.75` es el rango recomendado.
- `ρ ∈ [−2, −0.5]` controla la complementariedad; `ρ = −1` es el punto de partida.

### Fórmula completa combinada (sketch del pipeline)

```
// Nivel 1: valor de ancla individual
valorAncla_i = CES(días_i, minutos_i; α=0.70, ρ=−1.0)

// Nivel 2: score de capa (media de potencia de sus anclas)
scoreCapa_j = powerMean(valorAnclas_capa_j; p=−1.0, pesos=uniformes_dentro_capa)

// Nivel 3: score total (media de potencia de capas)
scoreTotal = powerMean(scoreCapas; p=−1.0, pesos=[0.25, 0.25, 0.20, 0.15, 0.15])
```

Todo implementable en Kotlin con `pow`, `ln`, `exp`. Cero `if`.

---

## 10. Fuentes

- [Generalized mean — Wikipedia](https://en.wikipedia.org/wiki/Generalized_mean)
- [Generalized mean — HandWiki](https://handwiki.org/wiki/Generalized_mean)
- [Constant elasticity of substitution — Wikipedia](https://en.wikipedia.org/wiki/Constant_elasticity_of_substitution)
- [CES Utility Function — EconGraphs](https://www.econgraphs.org/textbooks/intermediate_micro/scarcity_and_choice/preferences_and_utility/ces)
- [Ordered Weighted Averaging — Wikipedia](https://en.wikipedia.org/wiki/Ordered_weighted_averaging_aggregation_operator)
- [LogSumExp — Wikipedia](https://en.wikipedia.org/wiki/LogSumExp)
- [A new class of composite indicators: the penalized power means — arXiv 2206.11216](https://arxiv.org/abs/2206.11216)
- [Aggregating Composite Indicators through the Geometric Mean: A Penalization Approach — MDPI](https://www.mdpi.com/2079-3197/10/4/64)
- [Power Mean — Statistics How To](https://www.statisticshowto.com/power-mean-generalized-mean/)
- [CES preferences and production — Umbrex](https://umbrex.com/resources/economics-concepts/microeconomic-theory/ces-constant-elasticity-of-substitution-preferences-and-production/)
- [Geometric mean extension for data sets with zeros — arXiv 1806.06403](https://arxiv.org/abs/1806.06403)
- [Aggregation in AHP: Why weighted geometric mean should be used — ScienceDirect](https://www.sciencedirect.com/science/article/abs/pii/S0957417418303981)
