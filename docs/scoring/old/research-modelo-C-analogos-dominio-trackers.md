> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Research — Modelo C: Análogos de dominio real para scoring de constancia + intensidad

> **Estado:** investigación — insumo para diseño del motor de scoring  
> **Fecha:** 2026-06-08  
> **Propósito:** documentar cómo sistemas reales puntúan "constancia + intensidad sobre ventana móvil"
> y qué técnicas son adoptables en un modelo de pesos puros `score = Σ(peso × valor) ∈ [0,1]`.

---

## 1. Resumen ejecutivo

Los sistemas deportivos más sofisticados (ACWR/EWMA, Banister, TRIMP) y los wearables de
bienestar (Oura, Whoop, Garmin) resuelven exactamente el problema de "cuánta carga sostenida
hizo alguien en una ventana temporal" con técnicas **continuas y diferenciables**: medias
móviles exponenciales (EWMA), funciones de decaimiento exponencial, y combinaciones ponderadas
de sub-señales normalizadas. La ciencia del hábito (Lally 2010, Ebbinghaus) añade la noción de
**memoria asintótica**: los primeros días de constancia tienen el mayor impacto marginal; luego
la curva se satura. Todas estas técnicas son compatibles con un modelo de pesos puros — no
requieren gates ni if/else.

**Tres ideas centrales aplicables a nuestro caso:**
1. **EWMA con λ = 2/(N+1)** pesa automáticamente por recencia dentro de la ventana (días
   recientes pesan más que días viejos) sin ninguna lógica condicional.
2. **Saturación suave vía sigmoide o raíz** convierte minutos acumulados en una sub-señal
   acotada con rendimientos decrecientes, eliminando el incentivo al gaming de un solo día.
3. **Normalización por baseline personal** (como Oura/Whoop) ancla el score al historial del
   usuario en lugar de a umbrales absolutos, haciendo el modelo auto-calibrado.

---

## 2. Técnicas por dominio

### 2.1 Carga de entrenamiento deportivo — ACWR con EWMA

**Qué resuelve:** cuánta carga aguda ("esta semana") tiene alguien en relación a su carga
crónica ("mes pasado"). Detecta si la carga reciente es sostenible o excesiva.

**Fórmula EWMA:**

```
EWMA_hoy = Carga_hoy × λ + (1 - λ) × EWMA_ayer
```

Con:
- `λ = 2 / (N + 1)`, donde N es la ventana en días.
- Para ventana aguda (7 días): `λ_agudo = 2/(7+1) = 0.25`
- Para ventana crónica (28 días): `λ_crónico = 2/(28+1) ≈ 0.067`

Esto asigna peso exponencialmente decreciente: el día más reciente tiene peso `λ`, el anterior
`λ(1-λ)`, el de hace dos días `λ(1-λ)²`, etc.

**ACWR (Acute:Chronic Workload Ratio):**

```
ACWR = EWMA_agudo / EWMA_crónico
```

Zona óptima: 0.80 – 1.30. Por encima de 1.50: zona de riesgo.

**Ejemplo numérico (ventana de 7 días, λ=0.25):**

| Día | Carga (min) | EWMA_agudo |
|-----|-------------|-----------|
| D-6 | 0           | 0.0       |
| D-5 | 30          | 7.5       |
| D-4 | 0           | 5.6       |
| D-3 | 45          | 15.5      |
| D-2 | 30          | 19.1      |
| D-1 | 0           | 14.3      |
| D-0 | 60          | 25.7      |

Un día 0 de 60 min pesa `0.25 × 60 = 15 min`; el día de hace 6 días (30 min) pesa
`0.25 × 0.75⁶ × 30 ≈ 1.1 min`. La recencia está incorporada en la fórmula.

**Mapeo a nuestro caso:**
- Reemplazamos "carga deportiva" por "minutos realizados" o "días activos".
- Podemos construir un EWMA_7días de minutos y dividirlo por un baseline personal de 28 días.
- La ratio resultante se normaliza a [0,1] vía sigmoide o min-max suave.
- No hay gates: el EWMA es continuo y diferenciable por definición.

**Fuentes:**
- [Science for Sport — ACWR](https://www.scienceforsport.com/acutechronic-workload-ratio/)
- [PMC — Comparing ACWR Methods](https://pmc.ncbi.nlm.nih.gov/articles/PMC10051422/)
- [arXiv — ACWR challenges](https://arxiv.org/pdf/1907.05326)

---

### 2.2 Modelo Fitness-Fatiga de Banister (Impulse-Response)

**Qué resuelve:** cómo el rendimiento de un atleta evoluciona como resultado de la acumulación de
entrenamiento: la fitness (ganancia positiva) y la fatiga (coste negativo) tienen distintas
constantes de decaimiento temporal.

**Fórmula:**

```
P(t) = P₀ + k₁ × ∫ w(s) × e^(-(t-s)/τ₁) ds  −  k₂ × ∫ w(s) × e^(-(t-s)/τ₂) ds
```

Donde:
- `w(s)` = dosis de entrenamiento en el día s
- `τ₁ ≈ 42 días`: constante de tiempo de fitness (ganancia, decae lento)
- `τ₂ ≈ 7–14 días`: constante de tiempo de fatiga (coste, decae rápido)
- `k₁ < k₂`: la fatiga tiene mayor magnitud inicial que la fitness
- La versión discretizada de TrainingPeaks: **CTL** (fitness, τ=42 d) y **ATL** (fatiga, τ=7 d),
  con **TSB = CTL − ATL** (Training Stress Balance = forma del atleta).

**Versión discretizada (TrainingPeaks):**

```
CTL_hoy = CTL_ayer + (TSS_hoy - CTL_ayer) × (1 - e^(-1/42))
ATL_hoy = ATL_ayer + (TSS_hoy - ATL_ayer) × (1 - e^(-1/7))
TSB = CTL - ATL
```

Equivalente a EWMA con `λ_CTL = 1 - e^(-1/42) ≈ 0.024` y `λ_ATL = 1 - e^(-1/7) ≈ 0.133`.

**Ejemplo numérico:**
Si alguien entrena 60 TSS/día durante 4 semanas:
- CTL converge a ≈ 55 (fitness acumulada)
- ATL converge más rápido a ≈ 58 (fatiga reciente)
- TSB ≈ -3 (ligeramente en fatiga, normal con carga sostenida)

**Mapeo a nuestro caso:**
El concepto de dos señales con distintas constantes de tiempo es directo: podemos tener
una señal "memoria de constancia" (τ largo, ≈28 días) y otra "actividad reciente" (τ corto,
≈7 días). Su ratio o diferencia ponderada captura si el usuario está construyendo hábito
real (carga sostenida) vs. actividad esporádica. Ambas señales son continuas; el score
final es `peso_a × CTL_normalizado + peso_b × ATL_normalizado`.

**Fuentes:**
- [TrainingPeaks — Performance Manager Science](https://www.trainingpeaks.com/learn/articles/the-science-of-the-performance-manager/)
- [arXiv — Banister model cycling](https://arxiv.org/pdf/1902.02061)
- [Journals HK — Fitness-Fatigue numbers](https://journals.humankinetics.com/view/journals/ijspp/17/5/article-p810.xml)

---

### 2.3 TRIMP — Training Impulse (Intensidad con peso exponencial)

**Qué resuelve:** cuantificar el estrés fisiológico total de una sesión combinando duración e
intensidad con una función no lineal (intensidades altas escalan exponencialmente).

**Fórmula (Banister TRIMP):**

```
TRIMP = Σ (D_i × HR_r × 0.64 × e^(1.92 × HR_r))
```

Donde:
- `D_i` = duración en minutos en cada tramo de FC
- `HR_r` = fracción de FC de reserva = (FC_media − FC_reposo) / (FC_max − FC_reposo)
- `e^(1.92 × HR_r)`: peso exponencial por zona de intensidad (valores altos amplifican mucho)

**Variante por zonas (Edwards TRIMP):** multiplica minutos por zona con factores {1,2,3,4,5}.

**Mapeo a nuestro caso:**
En lugar de FC, nuestra "intensidad" puede ser el porcentaje del target completado
(`minutos_realizados / target_minutos`). La función exponencial se puede reemplazar por
una función sigmoide o raíz cuadrada que aplique rendimientos decrecientes:
```
intensidad_ponderada = f(ratio_target)   donde f es suave y acotada en [0,1]
```
El TRIMP diario acumulado sobre la ventana de 7 días, normalizado por el máximo teórico,
da un sub-score de intensidad acotado sin gates.

**Fuentes:**
- [Ludum — TRIMP explained](https://ludum.com/blog/data-performance-analytics/trimp-as-a-training-load-score/)
- [Firstbeat — What is TRIMP](https://www.firstbeat.com/en/blog/what-is-trimp/)

---

### 2.4 Wearables — Oura Readiness Score

**Qué resuelve:** combinar múltiples sub-señales en un score [0,100] de "qué tan lista está
la persona para rendir hoy", con baseline personal y pesos por ventanas temporales.

**Arquitectura del score (sin propietario, pero documentada parcialmente):**

```
Readiness = Σ (peso_i × contributor_i_normalizado)   ∈ [0, 100]
```

Contributors: Resting Heart Rate, HRV Balance, Body Temperature, Recovery Index, Sleep, Sleep
Balance, Sleep Regularity, Previous Day Activity, Activity Balance.

**Ventanas temporales de los contributors:**
- **HRV Balance** y **Activity Balance**: comparan el promedio de los últimos 14 días (con
  los últimos 2–5 días con más peso) contra el baseline de 2 meses.
- **Sleep Balance**: compara el promedio de sueño de la última semana contra la necesidad
  personal.
- **Previous Day Activity**: ventana de 24 horas.

**Normalización:** cada contributor se normaliza contra el **baseline personal** (no población),
establecido con ≥ 14 días de uso. Umbrales: ≥85 = Óptimo, 70–84 = Bueno, 60–69 = Regular, <60 = Atención.

**Mapeo a nuestro caso:**
- Cada "actividad" del usuario produce un sub-score normalizado contra su baseline personal.
- Los contribuidores con ventana larga (14 días) capturan consistencia; los de ventana corta
  capturan actividad reciente.
- La combinación ponderada `Σ(w_i × s_i)` es exactamente nuestro modelo de pesos puros.
- El baseline personal auto-calibra el score: un usuario que siempre hace 30 min no es
  penalizado por no hacer 60 min.

**Fuentes:**
- [Oura — Readiness Score](https://ouraring.com/blog/readiness-score/)
- [Oura Support — Readiness Contributors](https://support.ouraring.com/hc/en-us/articles/360057791533-Readiness-Contributors)
- [Oura Readiness Calculator — SimpleWearable](https://simplewearablereport.com/learn/metrics/readiness-score)

---

### 2.5 Wearables — Whoop Strain (escala logarítmica, baseline personal)

**Qué resuelve:** cuantificar el estrés cardiovascular de un día en una escala 0–21 que es
**logarítmica** (no lineal): pasar de 18 a 19 cuesta más que de 8 a 9.

**Arquitectura:**
- Combina duración de esfuerzo + intensidad cardíaca (similar a TRIMP).
- La escala está **basada en el Borg Scale** de esfuerzo percibido.
- **Personalización:** el mismo movimiento produce Strain diferente para personas distintas,
  según su baseline de FC máxima y nivel de fitness.
- Recovery Score (0-100%): combinación ponderada de HRV, FC reposo, calidad de sueño y
  Strain reciente; cada componente comparado contra el **baseline de 30 días**.

**Fórmula implícita (no pública, pero inferida):**
```
Strain ≈ log(Σ D_i × f(HR_i))  →  normalizado a [0, 21]
```
El logaritmo actúa como función de saturación: los primeros minutos de esfuerzo intenso
suman mucho; los adicionales cada vez menos.

**Mapeo a nuestro caso:**
La escala logarítmica o raíz cuadrada es la técnica clave para **anti-gaming**: un usuario
que hace 120 minutos de golpe no debe obtener el doble que quien hace 60 min en dos días.
Formula adaptable:
```
intensidad_dia = √(minutos_realizados / target_minutos)   ∈ [0, ~1.4]  → clampeado suave
```
O mejor (sin clamp duro):
```
intensidad_dia = 1 - e^(-minutos_realizados / target_minutos)   ∈ [0, 1)
```

**Fuentes:**
- [Whoop 101 — Developer Docs](https://developer.whoop.com/docs/whoop-101/)
- [Whoop Recovery — How it works](https://www.whoop.com/us/en/thelocker/how-does-whoop-recovery-work-101/)
- [Whoop strain explained — 5KRunner](https://the5krunner.com/2022/05/24/whoop-strain-everything/)

---

### 2.6 Ciencia del hábito — Curva asintótica de Lally (2010)

**Qué resuelve:** modelar cómo la automaticidad de un hábito crece con la repetición: rápidamente
al principio, luego más lento hasta alcanzar una meseta (asíntota).

**Fórmula (Lally et al., 2010 — European Journal of Social Psychology):**

```
Automaticidad(x) = a − b × e^(−c × x)
```

Donde:
- `x` = número de repeticiones (días de práctica)
- `a` = nivel asintótico máximo de automaticidad
- `b` = diferencia entre la asíntota y el valor inicial
- `c` = velocidad de crecimiento

**Parámetros empíricos:**
- Tiempo promedio para alcanzar el 95% de la asíntota: **66 días** (rango 18–254 días).
- La curva es convexa: el mayor salto de automaticidad ocurre en los **primeros 10–15 días**.

**Variante logística (Fournier et al., 2017):**
```
Habit(x) = L / (1 + e^(-k(x - x₀)))
```
- `L` = valor máximo; `k` = pendiente; `x₀` = punto de inflexión (el mayor crecimiento ocurre
  en x₀, no al principio como en Lally).

**Mapeo a nuestro caso:**
La forma `a − b × e^(-c×x)` es exactamente la función de saturación que necesitamos para
modelar el valor marginal de días de constancia acumulados. Los primeros días de racha
tienen mayor impacto que los últimos. Aplicado a la ventana de 7 días:
```
valor_constancia(d) = 1 - e^(-c × d)   donde d = días activos en la ventana, c ≈ 0.4
```

| Días activos (d) | c=0.4 | resultado |
|-----------------|-------|-----------|
| 1               | 0.33  | 0.33      |
| 2               | 0.55  | 0.55      |
| 3               | 0.70  | 0.70      |
| 5               | 0.86  | 0.86      |
| 7               | 0.94  | 0.94      |

Los días 6 y 7 apenas mueven el score respecto del 5 — anti-gaming natural.

**Fuentes:**
- [Lally 2010 — Wiley Online Library](https://onlinelibrary.wiley.com/doi/10.1002/ejsp.674)
- [Habit formation guidelines — Tandfonline](https://www.tandfonline.com/doi/full/10.1080/23311908.2022.2041277)

---

### 2.7 Decaimiento de Ebbinghaus — Memoria y vida media

**Qué resuelve:** cuánto de lo "aprendido/practicado" queda después de N días sin refuerzo.
La curva de olvido es exponencial.

**Fórmula:**

```
R(t) = e^(-t / S)
```

Donde:
- `R` = retención (fracción de lo que queda)
- `t` = tiempo sin refuerzo (días)
- `S` = estabilidad de memoria (depende de cuántas veces se ha practicado)

**Vida media:** sin refuerzo, se pierde ~50% en 1 día, ~70% en 24 horas, ~79% en 31 días.

**Relación con SM-2 (SuperMemo):**
SM-2 extiende Ebbinghaus: cada revisión exitosa aumenta el "Ease Factor" (E), que multiplica
el intervalo de revisión. La fórmula de intervalo:
```
I(n) = I(n-1) × EF   donde EF ∈ [1.3, 2.5], valor inicial 2.5
EF_nuevo = EF_viejo + (0.1 - (5 - q) × (0.08 + (5 - q) × 0.02))
```
`q` = calidad de respuesta (0–5). Respuesta fácil → EF sube → intervalo crece exponencialmente.

**Mapeo a nuestro caso:**
El decaimiento de Ebbinghaus modela perfectamente la **penalización por inactividad**: si un
usuario no hace una actividad por N días, su sub-score decae como `e^(-N/S)`. Esto es continuo
(no un cliff binario). Para nuestra ventana de 7 días:
- Inactividad de 1 día: `e^(-1/3) ≈ 0.72` (retiene 72%)
- Inactividad de 3 días: `e^(-3/3) = 0.37` (retiene 37%)
- Inactividad de 7 días: `e^(-7/3) ≈ 0.10` (retiene 10%)

**Fuentes:**
- [Ebbinghaus forgetting curve — Flashcardify](https://www.flashcardify.me/blog/ebbinghaus-forgetting-curve)
- [Modeling memory retention — TechRxiv](https://www.techrxiv.org/users/907969/articles/1286417)
- [SM-2 Algorithm — DEV Community](https://dev.to/umangsinha12/how-spaced-repetition-actually-works-the-sm-2-algorithm-1ge3)

---

### 2.8 Normalización suave — Sigmoide y min-max con percentil

**Qué resuelve:** convertir cualquier señal no acotada en un score [0,1] de forma continua,
sin clamps duros.

**Sigmoide (función logística):**
```
f(x) = 1 / (1 + e^(-k × (x - x₀)))
```
- `k` = pendiente (cuán rápido cambia alrededor del punto central)
- `x₀` = punto de inflexión (donde f = 0.5)

Para normalizar minutos de actividad con target T:
```
score_intensidad = 1 / (1 + e^(-k × (minutos/T - 1)))   con k ≈ 3
```
- `minutos = T`: score = 0.50
- `minutos = 1.5T`: score ≈ 0.82 (superhabit con rendimientos decrecientes)
- `minutos = 0`: score ≈ 0.047 (casi cero, pero no exactamente cero — sin cliff)

**Alternativa: 1 − e^(−x)** (exponencial inversa, más simple):
```
score = 1 - e^(-minutos / T)
```
- `minutos = 0`: score = 0 (exactamente cero)
- `minutos = T`: score ≈ 0.63
- `minutos = 2T`: score ≈ 0.86
- `minutos = 3T`: score ≈ 0.95 (saturación)

**Min-max suave con percentil 95:** recortar en el percentil 95 del historial propio del
usuario, luego normalizar. Esto hace que "100%" sea alcanzable para el usuario concreto
sin usar umbrales absolutos.

**Fuentes:**
- [Sigmoid Function — ScienceDirect](https://www.sciencedirect.com/topics/computer-science/sigmoid-function)
- [Sigmoid in ML — Medium](https://medium.com/@weidagang/demystifying-the-sigmoid-function-in-machine-learning-3b75f7ade3cd)

---

## 3. Qué técnicas chocan con el núcleo "sin gates" y cuáles encajan limpio

### Encajan limpio (continuas, diferenciables, sin if/else)

| Técnica | Por qué encaja |
|---------|---------------|
| EWMA (λ = 2/(N+1)) | Función lineal recurrente; no requiere ninguna condición |
| Curva asintótica de Lally: `1 - e^(-c×d)` | Función continua de días activos; sin clamps |
| Sigmoide `1/(1+e^(-k(x-x₀)))` | Diferenciable en todo su dominio |
| Exponencial inversa `1 - e^(-x/T)` | Continua, acotada en [0,1), saturación natural |
| Banister CTL/ATL discretizado | EWMA ponderada; puramente multiplicaciones y sumas |
| Decaimiento Ebbinghaus `e^(-t/S)` | Función continua de tiempo; penalización suave |
| Normalización por baseline personal (Oura-style) | División continua; sin umbrales binarios |

### Chocan con el núcleo (requieren gates o lógica condicional)

| Técnica | Por qué choca | Alternativa |
|---------|--------------|-------------|
| ACWR con zona "sweet spot" 0.8–1.3 | El beneficio de la zona se define con comparadores | Usar la ratio como input de una sigmoide continua |
| Edwards TRIMP con zonas discretas {1,2,3,4,5} | Las zonas son rangos → requieren if/else | Reemplazar por función exponencial continua de HR_r |
| SM-2 con EF condicional (si q<3 → reset) | La rama de reset es un gate duro | Usar solo la parte de decaimiento exponencial, sin reset |
| Min-max con percentil 95 "hard cap" | El cap en percentil 95 es un clamp duro | Usar sigmoide centrada en el percentil 95 como punto de inflexión |

**Nota clave:** el ACWR en sí es un *ratio* continuo; el problema son las *interpretaciones* con
zonas. La mecánica de cálculo (EWMA) es perfectamente compatible.

---

## 4. Recomendación: las 3 técnicas a adoptar y cómo combinarlas

### Técnica A — EWMA de 7 días para sub-score de constancia ponderada por recencia

**Fórmula:**
```kotlin
// λ = 2/(7+1) = 0.25
// ewma(0) = 0.0
fun ewma7(cargas: List<Double>, λ: Double = 0.25): Double =
    cargas.fold(0.0) { acc, carga -> carga * λ + acc * (1 - λ) }
```

Dado que los días más recientes pesan más, **un día activo reciente vale más que uno de
hace 5 días**. Esto es un comportamiento objetivo explícito del proyecto.

Normalización contra baseline personal (ventana de 28 días):
```
constancia_score = ewma_7dias / ewma_28dias_personal
→ normalizado vía sigmoide centrada en 1.0 (baseline = 0.5)
```

**Por qué adoptarla:** resuelve el requerimiento de "recencia pesa más" con matemática probada
en miles de estudios deportivos. Zero gates. Implementación trivial en Kotlin: un `fold`.

---

### Técnica B — Saturación exponencial `1 - e^(-x)` para sub-score de intensidad/minutos

**Fórmula:**
```kotlin
// ratio = minutos_realizados / target_minutos
// saturación natural: más allá de 2× el target, el score apenas sube
fun intensidadScore(minutos: Double, target: Double): Double =
    1.0 - exp(-minutos / target)
```

| minutos / target | score |
|-----------------|-------|
| 0.0             | 0.000 |
| 0.5             | 0.393 |
| 1.0             | 0.632 |
| 1.5             | 0.777 |
| 2.0             | 0.865 |
| 3.0             | 0.950 |

**Por qué adoptarla:** es la función de saturación más simple posible, sin parámetros libres
(o con solo el target como escala). Rendimientos decrecientes naturales anti-gaming.
Directamente inspirada en los modelos TRIMP (donde la intensidad tiene peso exponencial) y
en la curva de olvido de Ebbinghaus.

---

### Técnica C — Asíntota de Lally para sub-score de días activos en ventana

**Fórmula:**
```kotlin
// d = días activos en los últimos 7 días (0 a 7)
// c controla la velocidad de saturación; c=0.4 para 7 días
fun diasActivosScore(diasActivos: Int, c: Double = 0.4): Double =
    1.0 - exp(-c * diasActivos)
```

| días activos | score (c=0.4) |
|-------------|--------------|
| 0           | 0.000        |
| 1           | 0.330        |
| 2           | 0.551        |
| 3           | 0.699        |
| 4           | 0.798        |
| 5           | 0.865        |
| 6           | 0.909        |
| 7           | 0.939        |

El salto de 0 → 1 día es +0.33; el salto de 6 → 7 días es solo +0.03. Los primeros días de
constancia pesan mucho más que los últimos. **Exactamente el comportamiento objetivo.**

**Por qué adoptarla:** tiene respaldo empírico directo de neurociencia del hábito (Lally 2010).
La forma funcional es la misma que la técnica B, solo que aplicada a días (enteros) en lugar de
minutos (continuos). Se puede usar la misma función.

---

### Combinación en modelo de pesos puros

Las tres técnicas se combinan sin ninguna lógica condicional:

```
score_actividad = w_dias  × diasActivosScore(d)
               + w_minutos × intensidadScore(m, T)
               + w_recencia × ewmaScore_normalizado
```

Donde `w_dias + w_minutos + w_recencia = 1` y los pesos son los parámetros a calibrar.

**Configuración sugerida como punto de partida:**
- `w_dias = 0.55` (constancia > intensidad, regla del proyecto)
- `w_minutos = 0.30` (intensidad importa, con rendimientos decrecientes)
- `w_recencia = 0.15` (bonus por recencia, capturado con EWMA)

Este score por actividad se integra como un factor más en el scoring semanal global con
su propio peso en el `Σ(peso × valor)` del motor.

---

## 5. Fuentes consolidadas

- [Science for Sport — ACWR](https://www.scienceforsport.com/acutechronic-workload-ratio/)
- [PMC — ACWR systematic review](https://pmc.ncbi.nlm.nih.gov/articles/PMC12487117/)
- [PMC — Comparing ACWR methods volleyball](https://pmc.ncbi.nlm.nih.gov/articles/PMC10051422/)
- [arXiv — ACWR challenges](https://arxiv.org/pdf/1907.05326)
- [TrainingPeaks — Performance Manager Science](https://www.trainingpeaks.com/learn/articles/the-science-of-the-performance-manager/)
- [arXiv — Banister model road cycling](https://arxiv.org/pdf/1902.02061)
- [arXiv — Mathematical Modelling Athletic Performance](https://arxiv.org/html/2505.20859v1)
- [Journals HK — Fitness-Fatigue numbers](https://journals.humankinetics.com/view/journals/ijspp/17/5/article-p810.xml)
- [Ludum — TRIMP explained](https://ludum.com/blog/data-performance-analytics/trimp-as-a-training-load-score/)
- [Firstbeat — What is TRIMP](https://www.firstbeat.com/en/blog/what-is-trimp/)
- [Oura — Readiness Score](https://ouraring.com/blog/readiness-score/)
- [Oura Support — Readiness Contributors](https://support.ouraring.com/hc/en-us/articles/360057791533-Readiness-Contributors)
- [Whoop Developer 101](https://developer.whoop.com/docs/whoop-101/)
- [Whoop Recovery explained](https://www.whoop.com/us/en/thelocker/how-does-whoop-recovery-work-101/)
- [5KRunner — Whoop Strain](https://the5krunner.com/2022/05/24/whoop-strain-everything/)
- [Garmin Training Readiness — Wiki](https://wiki.garminrumors.com/Training_Readiness)
- [Lally 2010 — Habit formation in the real world](https://onlinelibrary.wiley.com/doi/10.1002/ejsp.674)
- [Habit formation guidelines 2022](https://www.tandfonline.com/doi/full/10.1080/23311908.2022.2041277)
- [Ebbinghaus forgetting curve](https://www.flashcardify.me/blog/ebbinghaus-forgetting-curve)
- [SM-2 Algorithm explained](https://dev.to/umangsinha12/how-spaced-repetition-actually-works-the-sm-2-algorithm-1ge3)
- [Sigmoid function — ScienceDirect](https://www.sciencedirect.com/topics/computer-science/sigmoid-function)
- [Exponential smoothing — Wikipedia](https://en.wikipedia.org/wiki/Exponential_smoothing)
- [Wellness streaks science](https://thewellnesshabit.life/articles/the-science-of-wellness-streaks.html)
