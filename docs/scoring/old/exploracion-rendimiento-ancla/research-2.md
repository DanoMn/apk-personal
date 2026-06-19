# Research 2 — Saturación y retornos decrecientes

> Researcher 2 · Sesión exploración rendimiento ancla · 2026-06-09

---

## §1. Estructuras encontradas

### 1.1 Hipérbola rectangular de Michaelis-Menten (n = 1)

**Forma:**

```
f(x) = x / (K + x)
```

- Dominio: x ≥ 0 → salida en [0, 1)
- En x = 0: f = 0
- En x → ∞: f → 1 (asíntota)
- En x = K: f = 0.5 — el parámetro K es literalmente el punto medio
- Primera derivada: f' = K/(K+x)² > 0 siempre (monótona creciente)
- Segunda derivada: f'' = −2K/(K+x)³ < 0 siempre (estrictamente cóncava)
- **La curva es cóncava pura: no hay zona convexa inicial.** Partiendo de x=0, ya el primer tramo tiene la mayor pendiente y todo es retornos decrecientes desde el inicio.
- Parámetro de control: K. K pequeño → satura rápido (a mitad de camino de K ya se acerca al techo). K grande → zona casi lineal más extensa antes de saturar.

**Normalización para que f(1) = 1 exactamente:** no es posible con la forma estándar (la asíntota es 1, no se alcanza). Para que f(1) = valor_de_referencia, se reescala el denominador: f_norm(x) = [x/(K+x)] / [1/(K+1)] = x(K+1)/(K+x). Así f_norm(1) = 1 para cualquier K, y para x > 1 el output supera 1 con retornos decrecientes — exactamente el territorio superávit.

---

### 1.2 Función de Hill (generalización con coeficiente n)

**Forma:**

```
f(x) = x^n / (K^n + x^n)    [n > 0]
```

- En x = 0: f = 0
- En x → ∞: f → 1
- En x = K: f = 0.5 siempre (K sigue siendo el punto de semisaturación independientemente de n)
- Para n = 1: recupera Michaelis-Menten (hipérbola, cóncava pura)
- Para n > 1: la curva adquiere zona convexa inicial (forma de S). El tramo 0 < x < inflexión es convexo (penaliza fuerte valores pequeños); el tramo inflexión < x es cóncavo (retornos decrecientes). **La zona convexa baja es el mecanismo natural para que días triviales aporten casi nada, sin umbral duro.**
- Para n < 1: más cóncava que M-M; los primeros incrementos valen mucho, luego satura muy rápido.
- Punto de inflexión (para n > 1): x_inf = K · ((n−1)/(n+1))^(1/n)
- Parámetros de control: **K** mueve el punto de semisaturación (dónde "vale la pena" el esfuerzo); **n** controla el codo (n grande = transición abrupta; n = 2 a 4 son valores "razonables" para sensación de umbral suave).

**Normalización a f(1) = 1:** misma reparametrización: f_norm(x) = [x^n/(K^n+x^n)] / [1/(K^n+1)] = x^n(K^n+1)/(K^n+x^n). Para x = 1: f_norm = 1. Para x > 1: output > 1 con saturación.

---

### 1.3 Exponencial saturante — negativo exponencial (1 − e^(−x/k))

**Forma:**

```
f(x) = 1 − e^(−x/k)
```

- En x = 0: f = 0 ✓
- En x → ∞: f → 1 (asíntota)
- Siempre cóncava (f'' = −e^(−x/k)/k² < 0 para todo x ≥ 0)
- Tangente en x = 0 tiene pendiente 1/k — con k pequeño, sube rápido; con k grande, crece lento
- Característica clave: **sin punto de inflexión** (cóncava pura como M-M, pero con decaimiento exponencial en vez de polinomial)
- Parámetro de control: k. Tiene interpretación física directa: en x = k la función vale 1 − 1/e ≈ 0.632 (el "63% del objetivo")

**Reparametrización para f(1) = 1:** escalar por 1/(1−e^(−1/k)):
```
f_norm(x) = (1 − e^(−x/k)) / (1 − e^(−1/k))
```
Ahora f_norm(0) = 0, f_norm(1) = 1, f_norm(x > 1) > 1 con retornos decrecientes. k controla qué tan rápido se satura más allá de x = 1.

**Variante Von Bertalanffy:** misma familia, usada en el sistema IPF GL de powerlifting:
```
f(x) = L · [1 − e^(−k(x − x₀))]
```
donde x₀ corre el punto de inicio y L es el techo. Tiene la misma concavidad pura.

---

### 1.4 Función potencia cóncava (power law con α < 1)

**Forma:**

```
f(x) = x^α    [0 < α < 1]
```

- En x = 0: f = 0 ✓
- En x = 1: f = 1 ✓ (pasa exactamente por (0,0) y (1,1) sin reparametrización)
- En x > 1: f > 1 con retornos decrecientes (f(2) = 2^α < 2)
- Estrictamente cóncava para α < 1: f'' = α(α−1)x^(α−2) < 0
- **No tiene asíntota superior** — crece sin límite, solo cada vez más lento
- Primera derivada: f'(x) = αx^(α−1) — en x = 0, la pendiente es infinita (para α < 1), lo que significa que los primeros incrementos desde cero son los más valiosos
- Parámetro de control: α. α = 1 es lineal; α → 0 satura muy rápido; α = 0.5 (raíz cuadrada) es un valor clásico en literatura económica y de game design

**Propiedad especial:** es la única estructura de esta lista que pasa exactamente por (1,1) por construcción, sin necesidad de reparametrización adicional. Esto la hace muy conveniente para el axioma A3.

**En game design:** raíces cuadradas (α = 0.5) y cúbicas (α = 1/3) son el estándar para curvas XP con diminishing returns. La forma "triangular" de niveles de juego usa f(puntos) = √(puntos × constante), que es x^0.5 escalado.

---

### 1.5 Función logarítmica ln(1 + x) / ln(2)

**Forma:**

```
f(x) = ln(1 + x) / ln(1 + r)    [normalizado para f(r) = 1]
```

Con r = 1 (normalización al target): f(x) = ln(1 + x) / ln(2)

- En x = 0: f = 0 ✓
- En x = 1: f = 1 ✓
- Estrictamente cóncava (f'' = −1/(1+x)² < 0)
- **No tiene asíntota — crece sin límite, pero muy lento**
- Parámetro de control: r (punto de referencia). Variar r equivale a correr el "escalado"
- Diferencia clave con Hill/MM: el log crece indefinidamente (sin techo), mientras que Hill y MM tienen asíntota en 1. Para superávit esto puede ser deseable si se quiere crecimiento sin límite absoluto pero con retornos muy bajos.

**Propiedad para triviales:** en x ≈ 0, ln(1+x) ≈ x — casi lineal cerca del cero. A diferencia de Hill con n > 1 (que penaliza fuertemente los valores triviales por la zona convexa inicial), el log **no penaliza fuerte los triviales** — los valora aproximadamente proporcional. Esto es importante para decidir si se quiere penalizar días triviales o solo no premiarlos tanto.

---

### 1.6 Función logística ajustada (sigmoide con cero en 0)

**Forma estándar:**

```
f(x) = 1 / (1 + e^(−n(x − x₀)))
```

**Forma ajustada para f(0) = 0:**

```
f(x) = [1/(1 + e^(−k(x − x₀))) − 1/(1 + e^(k·x₀))]  ×  constante_de_escalado
```

- El "tramo bajo" de la sigmoide (x < x₀) es convexo: aumentos pequeños tienen impacto mínimo
- El "tramo alto" (x > x₀) es cóncavo: retornos decrecientes
- La inflexión en x₀ es el punto donde el comportamiento cambia de "penalización de triviales" a "retornos decrecientes"
- Parámetros: x₀ (dónde está la inflexión — en el sistema podría ser T, el target) y k (agudeza del codo)
- **Utilidad dual:** la parte convexa baja hace que días con muy poco tiempo aporten casi nada (sin umbral duro); la parte cóncava alta limita el superávit con retornos decrecientes
- **Aplicación en powerlifting (arxiv 2503.13040):** se propone exactamente esta función para reemplazar el Von Bertalanffy cuando existe una fase inicial de retornos crecientes antes del target, seguida de retornos decrecientes

---

### 1.7 Función 1 − (1/(1 + x)) = x/(1 + x) — "recíproco negativo"

**Forma:**

```
f(x) = 1 − (x + 1)^(−1) = x / (x + 1)
```

Es simplemente Michaelis-Menten con K = 1. Mencionada específicamente en literatura de game design como la forma "bounded" de una curva de recompensa anti-grinding.

- En x = 0: f = 0 ✓
- En x = 1: f = 0.5 (no está en 1 — necesita reparametrización)
- En x → ∞: f → 1
- Descenso muy rápido de la utilidad marginal: el primer kill/el primer minuto ya trae 50% del máximo posible
- Parámetro de control: agregar un escalador: f(x) = ax/(1 + ax) controla la velocidad de saturación

---

## §2. Dominio de origen

| Estructura | Dominio de origen | Problema original | Referencias |
|---|---|---|---|
| Michaelis-Menten | Cinética enzimática (bioquímica) | Velocidad de reacción enzima-sustrato como función de concentración de sustrato | Michaelis & Menten, 1913; Srinivasan 2022 (FEBS Journal) |
| Hill (n > 1) | Bioquímica / farmacología | Unión cooperativa de ligandos (hemoglobina-O₂); curvas dosis-respuesta | Hill, 1910; Wikipedia "Hill equation (biochemistry)"; GraphPad Prism Guide |
| Exponencial saturante | Física / ingeniería / ecología | Crecimiento limitado por recursos (Von Bertalanffy para peces); carga de capacitor RC | Von Bertalanffy, 1941; IPF GL scoring (arxiv 2503.13040); Illustrative Mathematics Task 569 |
| Power law (x^α) | Economía, psicología del aprendizaje, game design | Utilidad marginal decreciente (Bernoulli); curvas de aprendizaje power-law; XP de niveles RPG | Eintalu (Medium); blog.nerdbucket.com/diminishing-returns-in-game-design; numberanalytics.com |
| Logarítmica ln(1+x) | Economía (utilidad de Bernoulli/Arrow-Pratt) | Utilidad marginal decreciente del dinero; scoring de benchmarks LLM | NBER Working Paper w32077; eintalu.medium.com; impressiondigital.com |
| Logística / sigmoide ajustada | Biología de poblaciones, farmacología, ML | Crecimiento sigmoide con dos fases (retornos crecientes luego decrecientes); curvas 4PL | arxiv 2503.13040 (powerlifting); geeksforgeeks.org; pubs.acs.org |
| Recíproco negativo x/(1+x) | Game design / sistemas anti-grinding | Recompensas decrecientes por repetición en MMOs (Warframe, City of Heroes) | tvtropes.org/AntiGrinding; blog.nerdbucket.com |

**Detalle de dominios:**

**Cinética enzimática (M-M y Hill):** el motor de scoring toma "dosis de hábito" (minutos) y produce "respuesta de bienestar" — la analogía con sustrato-velocidad es directa. El punto de semisaturación K es el equivalente a T (el objetivo en minutos). La Hill equation fue adoptada por marketing mix modeling (Meta Robyn, Google Meridian) para modelar retornos decrecientes de gasto publicitario, que tiene la misma estructura matemática: inputs continuos con efecto que satura.

**Powerlifting:** el sistema IPF GL usa Von Bertalanffy para modelar "cuánto potencial de fuerza tiene un atleta dado su peso corporal". El paralelo con nuestro sistema: "cuánto rendimiento produce un ancla dado el tiempo dedicado". La propuesta logística del arxiv 2503.13040 añade la fase convexa inicial (retornos crecientes antes del target), que es exactamente la propiedad que queremos para que días triviales aporten poco.

**Game design / anti-grinding:** el problema anti-grinding en RPGs es estructuralmente idéntico al problema P2 del brief — "3 días extra de 5 minutos con tarea de 40 min NO es superávit pleno". La solución de juegos usa funciones cóncavas o la forma recíproca negativa para que el valor marginal de cada "kill adicional" (aquí: día voluntario adicional de poco tiempo) sea muy bajo.

**Economía / utilidad:** la teoría de utilidad marginal decreciente (Bernoulli 1738, formalizada por Arrow-Pratt) establece que la utilidad logarítmica o cóncava es la representación canónica de "lo que ya tenés vale más que lo extra". El log(1+x) es la función de utilidad de referencia en economía del bienestar.

---

## §3. Mapeo explícito a D/F y t_i/T

### 3.1 Variable de tiempo: t_i/T (ratio minutos-sobre-target de un día)

Llamemos r = t_i/T. Queremos una función φ: [0, ∞) → [0, ∞) tal que:
- φ(0) = 0 (día sin hacer nada no aporta)
- φ(1) = 1 (cumplimiento exacto del target vale exactamente 1)
- φ creciente en r (A5)
- φ cóncava en r ≥ 1 (retornos decrecientes en superávit)
- Preferiblemente φ "penaliza" suavemente r trivial (r ≪ 1) sin umbral duro (P1)

**Opción A — Hill reparametrizado (recomendada para curva de día trivial + superávit):**

```
φ_Hill(r) = r^n · (K^n + 1) / (K^n + r^n)
```

- Para r = 0: φ = 0 ✓
- Para r = 1: φ = 1 ✓ (por construcción de la reparametrización)
- Para r > 1: φ > 1, creciente pero cóncava ✓
- Con n > 1 y K < 1: la curva tiene zona convexa en [0, K·((n−1)/(n+1))^(1/n)] — días con muy poco tiempo (r < K) aportan menos que proporcional (penalización suave de triviales)
- Con n = 1: recupera M-M reparametrizado (cóncava pura, sin zona convexa)
- **Axiomas satisfechos:** A2, A3, A5, A7, A9, A10 ✓ — todos naturalmente
- **Parámetro n:** controla qué tan fuerte es la "penalización" de triviales. n = 1 → ninguna (solo retornos decrecientes). n = 2 → suave. n = 4 → marcada.
- **Parámetro K:** posiciona el codo. K = 0.3 significa que el codo visible está en ~30% del target.

**Opción B — Exponencial saturante reparametrizada (más simple, solo retornos decrecientes):**

```
φ_exp(r) = (1 − e^(−r/k)) / (1 − e^(−1/k))
```

- Para r = 0: φ = 0 ✓
- Para r = 1: φ = 1 ✓
- Cóncava pura (sin zona convexa — no penaliza triviales, solo retorna decreciente)
- k pequeño → satura rápido más allá de r = 1 (superávit con poco impacto)
- k grande → curva casi lineal cerca de r = 1, satura lento
- **Axiomas:** A2, A3, A5, A7, A9, A10 ✓ — todos naturalmente
- **Cuándo elegirla:** cuando se quiere que los días de poco tiempo sigan aportando algo "proporcional" (sin penalización de triviales) pero el superávit esté acotado.

**Opción C — Power law x^α (más simple todavía, sin asíntota):**

```
φ_pow(r) = r^α    [0 < α < 1]
```

- Para r = 0: φ = 0 ✓
- Para r = 1: φ = 1 ✓ (por construcción, sin reparametrizar)
- Para r > 1: φ > 1 con retornos decrecientes, crece indefinidamente
- Pendiente infinita en r = 0 → los primeros minutos valen muchísimo relativamente
- **No tiene asíntota:** el superávit puede crecer sin límite (aunque lento). Si se necesita cota superior del superávit, requiere una capa adicional (truncado o composición con otra función)
- α = 0.5 (raíz cuadrada): estándar en game design para curvas XP
- **Axiomas:** A2, A3, A5, A7, A9, A10 ✓ — todos naturalmente

**Opción D — Logística ajustada (para días triviales con codo explícito en r = 1):**

```
φ_logit(r) = [σ(n·(r − 1)) − σ(−n)] / [σ(0) − σ(−n)]
donde σ(z) = 1/(1 + e^(−z))
```

- Inflexión en r = 1 (exactamente en el target): la curva cambia de convexa (penaliza triviales) a cóncava (retornos decrecientes del superávit)
- n controla la agudeza del codo. n grande → transición casi abrupta en r = 1; n pequeño → suave
- Para r → 0: tiende a 0 ✓; para r = 1: vale exactamente 1 si se ajusta la normalización
- **Axiomas:** A2, A3, A5, A9, A10 ✓ — A7 hay que verificar que la zona voluntaria (r pequeño pero positivo) no produzca valores negativos; la normalización puede causar valores muy cercanos a 0 pero no negativos si se escoge bien el escalado.
- **Cuándo elegirla:** cuando se quiere que el codo visible esté exactamente en r = 1 (target = umbral de "modo cóncavo").

---

### 3.2 Variable de frecuencia: D/F (días cumplidos sobre días comprometidos)

Llamemos q = D/F. Queremos una función ψ: [0, ∞) → [0, ∞) tal que:
- ψ(0) = 0 (cero días = cero frecuencia) (A2)
- ψ(1) = 1 (cumplimiento exacto de frecuencia = 1) (A3 parcial)
- Monótona creciente (A4)
- q > 1 produce ψ > 1 con retornos decrecientes (días extra voluntarios suman pero poco)

Las mismas familias aplican con r → q:

**M-M reparametrizado** (K = K_F): ψ(q) = q(K_F + 1)/(K_F + q) — satura suave, siempre cóncava
**Hill reparametrizado**: ψ(q) = q^n(K_F^n + 1)/(K_F^n + q^n) — zona convexa inicial si n > 1 (días muy escasos por debajo de K_F aportan poco)
**Power law**: ψ(q) = q^β — más simple, sin asíntota

**Nota de composición:** si el ancla combina φ(t_i/T) y ψ(D/F) en una fórmula compuesta, los axiomas A3 y A10 exigen que φ(1) = ψ(1) = 1 exactamente, de modo que la combinación en cumplimiento exacto dé 1. Todas las variantes reparametrizadas arriba satisfacen esta condición.

---

### 3.3 Mapeo a zona voluntaria (D > F, días extras)

Para la zona voluntaria (días más allá del compromiso F), el brief requiere piso cero y monotonía pero NO bidireccionalidad. Una función de retornos decrecientes para la contribución de día voluntario j (con tiempo t_j):

```
contribución_j = w_v · φ(t_j / T)    [w_v < 1, peso de zona voluntaria]
```

Donde φ es cualquiera de las funciones §3.1. La clave: φ(t_j/T) tiene piso cero por construcción (φ(0) = 0), y si t_j es trivial (pequeño), φ produce casi nada. El peso w_v < 1 asegura que la zona voluntaria siempre pese menos que la zona de compromiso por unidad equivalente.

**Anti-grinding:** para que múltiples días voluntarios de poco tiempo no sumen tanto como uno de mucho tiempo, la concavidad de φ ya lo garantiza estructuralmente: φ(0.1) × 3 < φ(0.3) si φ es cóncava y sub-aditiva (todas las funciones de esta lista lo son para α < 1 y n ≥ 1).

---

## §4. Explicación en lenguaje claro

### Qué aprendí

**La idea madre de todo esto es "los primeros valen más".** Las matemáticas para modelarlo son bien antiguas — Bernoulli la formalizó para el dinero en 1738 — y existen en varios sabores, cada uno con matices distintos.

**Michaelis-Menten (M-M) es el caso base.** Es como una hipérbola: sube rápido al principio y se aplana. El parámetro K es donde está "la mitad del camino". Si K = 1 (igual al target), en el target se llega a la mitad de saturación — eso no cumple A3 directo. Con la reparametrización que propongo, K se convierte en un control de "qué tan cóncava es la curva" y siempre se pasa exactamente por (1, 1).

**Hill es M-M con un switch extra.** Agrega el exponente n. Con n = 1, es idéntico a M-M (cóncava pura). Con n > 1, la curva tiene una "S" — convexa abajo, cóncava arriba. La parte convexa baja es la clave para P1 (días triviales): si t_i = 1 min con meta T = 30 min, eso es r = 0.033. Con Hill n = 2 y K = 0.3, ese día aporta algo así como 0.002 del total — casi nada, sin ningún umbral duro. Es matemáticamente elegante porque el comportamiento "emerge" de la curvatura, no de una regla.

**La exponencial saturante (1 − e^(−r/k))** es más intuitiva para ingenieros. Siempre cóncava pura (sin zona convexa), lo que significa que incluso minutos pequeños valen algo, solo que menos. Es perfecta para modelar el superávit (zona r > 1) porque satura exponencialmente rápido. No penaliza triviales — simplemente los valora proporcional pero con retornos decrecientes globales.

**La raíz cuadrada (power law α = 0.5)** es la fórmula de los diseñadores de videojuegos. Tiene la ventaja de que pasa exactamente por (0,0) y (1,1) sin trucos de reparametrización. La desventaja es que no tiene techo — el superávit puede crecer indefinidamente aunque muy lento. En games se usa justamente para los sistemas XP de nivelado donde cada nivel siguiente requiere más XP pero no hay techo absoluto.

**El log(1+r)** es la fórmula de los economistas. Propiedad rara: los primeros incrementos desde cero valen casi proporcional (log(1+x) ≈ x para x pequeño), y luego satura suave. Útil si se quiere que incluso la primera acción mínima tenga impacto "honesto", sin castigo por trivialidad.

**La sigmoide ajustada** es la más potente pero la más compleja. Tiene exactamente dos fases: antes del target (r < 1), la curva es convexa — los días de poco tiempo "cuestan mucho para lo que aportan". Después del target (r > 1), la curva es cóncava — retornos decrecientes. El codo está en r = 1 (o donde se ponga x₀). Esta estructura es la que usó un paper de powerlifting (2025) para reemplazar el sistema anterior de la IPF porque modela mejor la realidad: no es lo mismo pasar de 0% a 50% del target que de 50% a 100%.

**La intuición anti-grinding** que aprendí de game design: los sistemas que no tienen saturación matemática siempre terminan con jugadores que "grindean" — repiten la acción más fácil infinitamente. Los games que funcionan usan concavidad estructural: el primero vale 1, el segundo vale 0.7, el tercero 0.5, etc. La función cóncava lo hace automáticamente. No hay que poner un límite duro — la matemática desincentiva el grinding porque la utilidad marginal colapsa.

**El punto más importante para el problema del brief:** Hill con n > 1 es la única estructura que **simultáneamente** resuelve P1 (días triviales no aportan casi nada) Y P2 (superávit con retornos decrecientes), en una sola curva, con dos parámetros explícitos y sin ningún gate duro. M-M resuelve solo P2. La exponencial resuelve solo P2. La logística resuelve ambos pero es más compleja de calibrar. Power law y log resuelven P2 pero no P1.

---

## Referencias

1. Michaelis, L. & Menten, M.L. (1913). Die Kinetik der Invertinwirkung. *Biochemische Zeitschrift*.
2. Srinivasan, B. (2022). A guide to the Michaelis–Menten equation: steady state and beyond. *The FEBS Journal*. https://febs.onlinelibrary.wiley.com/doi/10.1111/febs.16124
3. Hill, A.V. (1910). The possible effects of the aggregation of the molecules of haemoglobin on its dissociation curves. *J Physiol*.
4. Wikipedia contributors. Hill equation (biochemistry). https://en.wikipedia.org/wiki/Hill_equation_(biochemistry)
5. GraphPad Prism 11 Curve Fitting Guide — Specific binding with Hill slope. https://www.graphpad.com/guides/prism/latest/curve-fitting/reg_specific_hill.htm
6. Chemistry LibreTexts. Sigmoid Kinetics / Hill Plot. https://chem.libretexts.org/Bookshelves/Biological_Chemistry/Supplemental_Modules_(Biological_Chemistry)/Enzymes/Enzymatic_Kinetics/Sigmoid_Kinetics
7. Gopinath, R. (2024). Hill Function and Its Implementation: A Strategic Overview. *Medium*. https://medium.com/@mail2rajivgopinath/hill-function-and-its-implementation-a-strategic-overview-2ffd443f92a0
8. MetricGate. Saturation Curve Modeling Calculator. https://metricgate.com/docs/saturation-curve-modeling/
9. MetricGate. Ad Frequency Saturation (Hill Curve) Calculator. https://metricgate.com/docs/ad-frequency-saturation-hill/
10. Rodrigues, T. & Schöbel, A. (2025). Discussing Diminishing Returns: A New Scoring System for Powerlifting. *arXiv:2503.13040*. https://arxiv.org/abs/2503.13040
11. Von Bertalanffy, L. (1957). Quantitative laws in metabolism and growth. *Quarterly Review of Biology*.
12. Charan, A. Concave (Diminishing Returns) Model — Response Function. *Marketing Analytics*. https://www.ashokcharan.com/Marketing-Analytics/~mx-mmm-response-function-concave.php
13. Eintalu, J. Some Simple Utility Functions. *Medium*. https://eintalu.medium.com/some-simple-utility-functions-cc04c545ce02
14. Number Analytics. A Deep Dive into Diminishing Marginal Utility Today. https://www.numberanalytics.com/blog/ultimate-guide-diminishing-marginal-utility
15. NBER. Diminishing Marginal Utility Revisited. *Working Paper w32077*. https://www.nber.org/system/files/working_papers/w32077/w32077.pdf
16. Nerdbucket. Diminishing Returns in Game Design: Roots and Negative Exponents. https://blog.nerdbucket.com/diminishing-returns-in-game-design-roots-and-negative-exponents/article
17. Aversa, D. GameDesign Math: RPG Level-based Progression. https://www.davideaversa.it/blog/gamedesign-math-rpg-level-based-progression/
18. TV Tropes. Anti-Grinding. https://tvtropes.org/pmwiki/pmwiki.php/Main/AntiGrinding
19. iSoron. Loop Habit Tracker — FAQ (uhabits GitHub Discussion #689). https://github.com/iSoron/uhabits/discussions/689
20. Gopinath, R. Saturation and Adstock Effects in Bayesian MMM. *Medium*. https://medium.com/@mail2rajivgopinath/saturation-and-adstock-effects-in-bayesian-mmm-ac62bfd16b12
21. Impression Digital. Diminishing Returns & Saturation Curves. https://www.impressiondigital.com/blog/saturation-curves/
22. PyMC Labs. Marketing Mix Modeling: A Complete Guide. https://www.pymc-labs.com/blog-posts/marketing-mix-modeling-a-complete-guide
23. Danaher Life Sciences. Dose-Response Curve. https://lifesciences.danaher.com/us/en/library/dose-response-curve.html
24. Grokipedia. Gompertz function. https://grokipedia.com/page/Gompertz_function
25. Frontiers in Psychology. Is more always better? An S-shaped impact of gamification. https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2025.1671543/full
