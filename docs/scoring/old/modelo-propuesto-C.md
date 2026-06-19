> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo propuesto C — motor de scoring (pesos puros, multiplicador fijo)

> **Estado: propuesta independiente para síntesis.** Uno de los 3 modelos a ciegas del
> plan `meta/instructions/2026-06-08-tres-modelos-motor-scoring.md`. Respeta TODAS las
> bases cerradas: el motor es SOLO `score = Σ(peso_capa × valor_capa)` → estado por
> bandas. CERO reglas, caps, gates, colapsos, worst-term. Todo comportamiento EMERGE de
> peso × valor. Verificado a **45/45 marcas** contra `dataset-decisiones-estado-v1.md`.

---

## 1. Resumen del modelo (5 líneas)

1. **Base pura = promedio plano.** Cada capa activa pesa `1/N` y vale el promedio de sus anclas; el estado sale del agregado contra cuatro cortes (`R<0.40 · A<0.62 · EM<0.85 · P≥0.85`). Sin worst-term (`ω=0`).
2. **Opt-in = multiplicador fijo (Forma A).** Sueño multiplica el peso de Cuerpo por `k_sleep=1.5`, sobriedad multiplica el de Conducta por `k_sobr=3.0`; **se renormaliza** para que los pesos sumen 1. Al subir N, la porción del opt-in baja sola.
3. **El modulador entra al valor de su capa:** sueño domina el valor de Cuerpo (`β=0.55` del valor lo pone el sueño); sobriedad fija el valor de Conducta (recaída/sin-marcar lo hunden a un piso plano).
4. **Soportes** entran al valor de cada capa con una asimetría fuerte (descuidar castiga `0.25`, cuidar premia `0.02`). **Tasks: 0.** No tienen peso ni mueven el valor.
5. **Inquebrantable** no es banda: es estado sobre Plenitud (anclas 100% + superhabit en **≥2 capas**). El apretón (3 capas + ambos opt-in) deja la capa libre en 18.2% **sin ninguna regla** — emerge de la renormalización.

---

## 2. Fórmula matemática completa

### 2.1 Notación

- `N` = número de capas activas (3 ≤ N ≤ 5). Capas posibles: Interior (I), Cuerpo (Cu), Conducta (Co), Vínculos (V), Proyecto (P).
- Para cada capa `L`: `a_L` = **promedio plano de las fracciones de cumplimiento de sus anclas** (`a_L = mean(frac_ancla_i)`). La cantidad de anclas NO entra en la fórmula → no cambia el peso (caso límite 4).
- `sleep ∈ {off, ok, mal, none}` (estado del opt-in Sueño). `mal` y `none` se tratan igual.
- `sobr ∈ {off, clean, relapse, unmarked}` (estado del opt-in Sobriedad).
- `s_L ∈ [0,1]` = fracción de días sostenidos del soporte de la capa `L` (si esa capa tiene soporte; si no, ausente).

### 2.2 Valor de capa `v_L`

Se computa en dos pasos: primero el **núcleo** (anclas + modulador), después el **soporte**.

**Paso A — núcleo (`core_L`):**

```
core_Cu = β · sv + (1−β) · a_Cu          si Cuerpo tiene sueño activo (sleep ≠ off)
            con sv = 1.0 si sleep=ok ; sv = s_bad si sleep ∈ {mal, none}
core_Co = a_Co        si sobr = clean      (sobriedad limpia → vale las anclas normales)
        = r_relapse   si sobr = relapse    (recaída → piso plano)
        = r_unmarked  si sobr = unmarked   (activo sin marcar → piso plano, ventana perdón)
core_L  = a_L         en cualquier otro caso (capa normal, o modulador off)
```

**Paso B — soporte (solo si la capa tiene soporte):**

```
v_L = clamp_{[0,1]}( core_L + b_sop · s_L − p_sop · (1 − s_L) )
```

Si la capa no tiene soporte: `v_L = clamp_{[0,1]}(core_L)`.

El `clamp` a `[0,1]` NO es una regla de negocio: es la definición del dominio de una fracción (un valor de capa no puede ser <0 ni >1). No introduce comportamiento — solo evita aritmética fuera de rango.

### 2.3 Pesos de capa `w_L` — **Forma A: multiplicador fijo renormalizado**

```
w'_L = 1                       (todas las capas activas arrancan iguales)
w'_Cu = k_sleep                si sleep ≠ off
w'_Co = k_sobr                 si sobr ≠ off
w_L  = w'_L / Σ_j w'_j         (renormalización → Σ w_L = 1)
```

La renormalización es lo que hace que **al subir N la porción de cada opt-in baje sola** y que el **apretón nunca produzca una capa ≤0** (caso límite 1 y 2).

### 2.4 Score y bandas

```
score = Σ_{L activa} w_L · v_L                      (score ∈ [0,1])

estado = R   si score < c_RA   (= 0.40)
       = A   si score < c_AEM  (= 0.62)
       = EM  si score < c_EMP  (= 0.85)
       = P   si score ≥ c_EMP
```

### 2.5 Inquebrantable (estado sobre Plenitud, NO banda)

```
Inquebrantable  ⟺  estado = P  ∧  todas las anclas al 100%  ∧  superhabit en ≥2 capas
```

No es un corte de score: es una etiqueta que se aplica encima de Plenitud cuando el superhabit cubre **al menos 2 capas activas** (decisión del dueño 2026-06-08, lote IN).

### 2.6 Parámetros calibrados (punto final)

| Parámetro | Valor | Qué controla | Cómo quedó fijado |
|-----------|-------|--------------|-------------------|
| `β` (beta) | **0.55** | peso del sueño DENTRO del valor de Cuerpo | sueño domina Cuerpo (>50%); libre en [0.5, 0.55] |
| `s_bad` | **0.20** | valor del sueño mal/no-registrado | libre en [0.15, 0.30] |
| `k_sleep` | **1.5** | multiplicador de peso de Cuerpo con sueño | **PINNED** por SU5/SU6 (zona media) |
| `k_sobr` | **3.0** | multiplicador de peso de Conducta con sobriedad | Conducta a 60%, dentro del sustento 50–63% |
| `r_relapse` | **0.50** | valor de Conducta en recaída | libre en [0.40, 0.50] |
| `r_unmarked` | **0.50** | valor de Conducta sin marcar (perdón) | libre en [0.40, 0.50] |
| `p_sop` | **0.25** | castigo del soporte descuidado | **PINNED** |
| `b_sop` | **0.02** | premio del soporte cuidado | **PINNED** |
| `c_RA` | **0.40** | corte R\|A | **PINNED** |
| `c_AEM` | **0.62** | corte A\|EM | **PINNED** por la tensión SU5/SU6 |
| `c_EMP` | **0.85** | corte EM\|P | libre en [0.84, 0.86] |

(Identificabilidad medida sobre los 342 puntos que dan 45/45; ver sección 5.)

---

## 3. Tabla de pesos

Pesos de cada capa en cada combinación de opt-in, para N=3 y N=5 (Forma A, `k_sleep=1.5`, `k_sobr=3.0`, renormalizados).

### N = 3 capas (I, Cu, Co)

| Combinación | Interior | Cuerpo | Conducta | Nota |
|-------------|---------:|-------:|---------:|------|
| sin opt-in | 0.333 | 0.333 | 0.333 | capas parejas (base pura) |
| solo sueño | 0.286 | **0.429** | 0.286 | Cuerpo sube a 43% |
| solo sobriedad | 0.200 | 0.200 | **0.600** | Conducta a 60% (sustento 50–63%) |
| **ambos (apretón)** | **0.182** | 0.273 | **0.545** | capa libre = 18.2%, nunca ahogada |

### N = 5 capas (I, Cu, Co, V, P)

| Combinación | Interior | Cuerpo | Conducta | Vínculos | Proyecto | Nota |
|-------------|---------:|-------:|---------:|---------:|---------:|------|
| sin opt-in | 0.200 | 0.200 | 0.200 | 0.200 | 0.200 | parejas |
| solo sueño | 0.182 | **0.273** | 0.182 | 0.182 | 0.182 | Cuerpo a 27% (menor que en N=3) |
| solo sobriedad | 0.143 | 0.143 | **0.429** | 0.143 | 0.143 | Conducta a 43% (menor que en N=3) |
| ambos | 0.133 | 0.200 | **0.400** | 0.133 | 0.133 | apretón se diluye con N |

**Lectura clave:** la porción de un opt-in **baja al crecer N** (Cuerpo con sueño: 43% en N=3 → 27% en N=5). Esto es la consecuencia directa de la Forma A. Ver caso límite 1 para la justificación.

---

## 4. Resolución de los casos límite

### Caso 1 — Escalado con N (3→5 capas): **Forma A (multiplicador fijo)**

**Elección: multiplicador fijo renormalizado.** El opt-in multiplica el peso de su capa por un factor constante (`k`) y luego se renormaliza. Consecuencia: la *porción* del opt-in **baja al subir N** (Cuerpo con sueño = 43% a N=3, 27% a N=5).

**Por qué Forma A y no Forma B (porción fija):** la Forma B (el opt-in toma siempre la misma fracción del total, p. ej. Cuerpo = 43% a cualquier N) **colapsa matemáticamente en el apretón**. Lo verifiqué numéricamente:

> Forma B, 3 capas, ambos opt-in: Cuerpo (0.429) + Conducta (0.60) = **102.9% > 100%** → la capa libre queda en **0% o negativa**.

Para salvar la Forma B haría falta una regla de clamp/reescala de emergencia — **prohibido por las bases cerradas**. La Forma A no tiene ese problema: la renormalización garantiza `Σw=1` siempre y deja la capa libre positiva en cualquier N y cualquier combinación. **La elección no es estética: la Forma B es inviable sin meter una regla.**

**Coherencia conceptual:** que el sueño pese relativamente menos en un usuario con 5 dominios activos que en uno con 3 es razonable — con más áreas de vida en juego, ninguna sola debería monopolizar el agregado. El opt-in sigue siendo el peso más pesado de su grupo, pero no aplasta el sistema.

**Sin marca real** que distinga N=3 de N=5 para los moduladores (todas las marcas de sueño/sobriedad son a 3 capas; las de 5 capas son base pura sin opt-in). La elección se decide por **viabilidad matemática del apretón**, no por dato. Lo declaro explícito.

### Caso 2 — El apretón (3 capas + sueño + sobriedad): **aceptable, emerge solo**

Con `k_sleep=1.5` y `k_sobr=3.0`, el apretón a 3 capas da:

```
Interior (libre) = 18.2% · Cuerpo = 27.3% · Conducta = 54.5%
```

**La capa libre queda en 18.2%, no se ahoga.** Comprobé que no produce comportamientos absurdos (sin meter regla alguna):

- Interior (libre) a 0% con todo lo demás perfecto → score 0.818 (EM). No colapsa el sistema; resta un 18% coherente con el patrón "una capa muerta → EM" de la base pura (lote BP-AC).
- Recaída en el apretón → score 0.727. La recaída pega **más fuerte** que sin sobriedad activa, porque Conducta pesa 54.5%. **El golpe EMERGE del peso**, exactamente como pide el principio de modulación.

**¿Por qué es aceptable que la capa libre baje a 18%?** Porque el usuario *eligió* activar dos moduladores. Si activó sueño Y sobriedad, está declarando que esos dos dominios son su prioridad esta etapa; es coherente que su semana se juegue mayormente ahí. Que la capa libre no llegue a 0 (gracias a la renormalización) garantiza que **sigue contando** — descuidarla totalmente baja un escalón, pero no la vuelve irrelevante.

**Lo elegí así, no hay dato.** El brief lo dice: el apretón no tiene marca. Justifico por diseño: (a) la Forma A lo resuelve sin regla; (b) la capa libre conserva voz (18.2% > 0); (c) los golpes de los moduladores siguen emergiendo de sus pesos inflados.

### Caso 3 — Los dos opt-in juntos (sueño + sobriedad): **se acumulan en el agregado**

**Sin una sola marca.** Decisión de diseño: los dos moduladores **no interactúan** — cada uno infla el peso de su capa y hunde el valor de su capa de forma independiente; el efecto combinado es la **suma ponderada** (no hay worst-term, no hay multiplicación cruzada).

Comprobación (anclas 100%, ambos malos: sueño mal + recaída):

```
score = 0.607  → frontera A/EM
```

Cada golpe resta lo suyo y se **acumulan linealmente** en el score. Esto es la consecuencia natural del agregado ponderado sin worst-term. **Lo justifico así** porque:

1. Es lo más simple y predecible (dos malas semanas combinadas dan peor que cada una sola, pero no un colapso catastrófico).
2. Respeta "todo emerge de peso × valor": no hay término de interacción que sería una regla encubierta.
3. Es coherente con que cada modulador ya está calibrado por separado; sumar sus efectos no contradice ninguna marca.

**Matiz honesto:** como `k_sobr (3.0) > k_sleep (1.5)`, en el apretón un mal de sobriedad (recaída → 0.727) pega más que un mal de sueño (→ 0.880). Esto sale de que la sobriedad eligió pesar más sobre su capa. Si el dueño quisiera que el sueño domine incluso sobre la sobriedad, habría que subir `k_sleep` — pero eso rompe SU5/SU6 (ver caso límite 6). Es una tensión real que el dato actual no resuelve.

### Caso 4 — Anclas múltiples por capa: **promedio plano confirmado**

`a_L = mean(frac de cada ancla de la capa L)`. La **cantidad de anclas no aparece** en la fórmula de pesos: una capa con 3 anclas y otra con 1 pesan igual (`1/N` cada una). Esto es exactamente la base cerrada 3.

**¿Por qué el promedio y no algo mejor?** Las marcas no piden nada distinto: el dataset codifica cada capa por su fracción agregada, nunca por el detalle ancla-por-ancla *dentro* de la misma capa que cambie el resultado. El promedio plano es el mínimo que cumple "las anclas promedian, la cantidad no cambia el peso". Cualquier alternativa (mín, peor-ancla, ponderado por target) introduciría asimetría intra-capa que **ninguna marca respalda** → sería inventar. Mantengo el promedio.

### Caso 5 — Soporte: **asimetría aditiva fuerte**

Forma: `v_L = core_L + b_sop·s_L − p_sop·(1−s_L)` con `p_sop=0.25`, `b_sop=0.02`.

- **Descuidar castiga mucho** (`−0.25·(1−s)`): un soporte abandonado (s=0) resta 0.25 al valor de la capa.
- **Cuidar premia poco** (`+0.02·s`): un soporte cuidado (s=1) suma solo 0.02.
- Ratio castigo/premio ≈ **12×** → la asimetría que pide el sustento ("descuidar castiga mucho más que tener premia").

Esto reproduce el lote SO completo (7/7), incluyendo la observación dura del dataset: con anclas al 100%, descuidar soportes baja **un escalón entero** P→EM (SO2: 1.0 anclas + soportes a 2/7 → score 0.827 → EM). El `p_sop=0.25` quedó **PINNED** (es el único valor del grid que da 45/45). Confirma la nota del dataset: el viejo "0.80/0.20" era insuficiente; el soporte pesa más.

### Caso 6 — Magnitud exacta de los multiplicadores/porciones

Dentro del rango que las marcas permiten (342 puntos dan 45/45), elegí el punto que respeta el sustento y balancea márgenes:

- `k_sleep = 1.5` **(PINNED)**: es el único valor que sobrevive la tensión SU5/SU6. Si subo `k_sleep` a 2.0+, el sueño bueno empuja SU6 (50% + sueño ok) por encima de 0.62 (debería ser A) y/o el sueño malo hunde SU5 (75% + sueño mal) por debajo de 0.62 (debería ser EM). **k_sleep=1.5 es la cota máxima del sueño compatible con el dato.**
- `k_sobr = 3.0`: pone Conducta a 60%, en el centro del sustento 50–63% (SB9+SB4). Libre en [2.5, 3.0]; elijo 3.0 por mejor margen en SB4.
- `β = 0.55`: el sueño es la mayoría del valor de Cuerpo (>50%), cumpliendo "sueño domina Cuerpo". Libre en [0.5, 0.55].
- `c_AEM = 0.62` **(PINNED)**: no 0.64. La tensión SU5/SU6 fuerza el corte A|EM a 0.62 (ver sección 5).

### Caso 7 — Higiene digital (RESUELTO en el brief): **ancla de Conducta normal**

Por el plan, Higiene digital es **una ancla de Conducta como cualquier otra**: promedia dentro de `a_Co` con el peso de Conducta. NO impacta Sueño, NO es sub-modulador. Su ubicación en la UI de la feature Sueño es presentación, no dominio. **El modelo no le da ningún tratamiento especial** — entra a `a_Co` y listo. No hay nada que calibrar.

---

## 5. Verificación contra las 45 marcas

**Resultado: 45/45 (100%).** Script descartable: `/tmp/scoring_c/verify_model_c_final.py`
(autocontenido, `CASES` = copia textual de `weight_model_fit_v2.py`; NO toca `scripts/scoring/`).

| Lote | Casos | Aciertos |
|------|-------|----------|
| BP (base pura, 5 capas) | 11 | 11/11 |
| SU (sueño modulador) | 9 | 9/9 |
| SB (sobriedad modulador) | 10 | 10/10 |
| SO (soportes + tasks) | 7 | 7/7 |
| IN (Inquebrantable) | 8 | 8/8 |
| **Total** | **45** | **45/45** |

### Robustez (no es un punto frágil sobreajustado)

- **342 puntos del grid dan 45/45** → el ajuste no depende de un valor mágico. La estructura es la que ajusta, no un afortunado redondeo.
- **Parámetros PINNED** (un solo valor sobrevive en los 45/45): `k_sleep=1.5`, `p_sop=0.25`, `b_sop=0.02`, `c_RA=0.40`, `c_AEM=0.62`. Estos son los que el dato realmente determina.
- **Parámetros libres** (varios valores valen): `β∈[0.5,0.55]`, `s_bad∈[0.15,0.30]`, `k_sobr∈[2.5,3.0]`, `r_relapse∈[0.4,0.5]`, `r_unmarked∈[0.4,0.5]`, `c_EMP∈[0.84,0.86]`. Hay holgura → elegí por sustento/margen.

### El caso más apretado (tensión estructural honesta)

El margen mínimo al corte es **0.0004** en **SU5** (75% + sueño mal → score 0.6204, corte A|EM = 0.62). Le sigue SU6 (50% + sueño ok → 0.6179). Estos dos casi se tocan:

- SU5 (75% + sueño **malo**) debe quedar **EM** (≥0.62).
- SU6 (50% + sueño **bueno**) debe quedar **A** (<0.62).

El dataset pide que "un 75% con mal sueño valga más que un 50% con buen sueño", pero por un pelo. El modelo los separa por <0.003 de score. **Es una tensión real e irreducible del dato** (no un bug): el sueño malo en zona media casi alcanza a anular la diferencia de 25 puntos de cumplimiento. Por eso `c_AEM` quedó clavado en 0.62 y `k_sleep` en 1.5 — cualquier otro valor cruza uno de los dos. **Lo reporto honesto:** si el dueño re-marcara SU5/SU6 con otra intención, este es el primer punto que se movería.

### Ninguna marca exigió una regla-parche

Las 45 entran con `score = Σ peso·valor` + 4 cortes + el estado Inquebrantable. **No hubo que inventar ningún cap, gate, worst-term ni colapso.** El anti-worst-term del dataset (una capa muerta NO colapsa a R) sale solo del promedio plano con `ω=0`. La no-linealidad del sueño (Caminar a la mitad sigue P, Caminar a 0 baja a EM) emerge del `β` dentro del valor de Cuerpo. La cobertura P↔I sale del conteo de capas con superhabit. Todo emergente.

---

## 6. Tradeoffs y riesgos

**Fortalezas**
- **Cero reglas-parche.** Estructura mínima: pesos + valores + cortes. Trivial de implementar, auditar y explicar.
- **Forma A resuelve el apretón sin trucos.** La renormalización es la única "lógica", y es matemática pura, no negocio.
- **Robusto:** 342 puntos dan 45/45; los parámetros que importan están identificados y pinneados por el dato, no por capricho.
- **Conceptualmente alineado:** sueño domina Cuerpo, sobriedad domina Conducta (50–63%), recaída pega por peso, soporte castiga asimétrico — todo como pide el dueño.

**Riesgos / tensiones**
1. **SU5/SU6 al filo (margen 0.0004).** El corte A|EM está clavado por estos dos casos. Es la fragilidad #1: una re-marca cambiaría `c_AEM` y `k_sleep`. **Recomiendo re-validar SU5/SU6 con el dueño** antes de congelar.
2. **`k_sleep=1.5` es bajo.** El dueño quiere "sueño = el peso más pesado del sistema". A 3 capas, Cuerpo con sueño llega a 43% (sí es la capa más pesada). Pero el dato (SU5/SU6) **no deja subirlo más** sin romper la zona media. Hay una tensión latente entre "sueño dominante" (deseo) y "sueño no rompe la zona media" (dato). Se resuelve cuando entre la métrica real de sueño.
3. **El apretón y las dos-juntas son 100% diseño, 0% dato.** Mis elecciones (Forma A, golpes que se suman) son defendibles pero no validadas. **Recomiendo 2–3 marcas de apretón** (3 capas, ambos opt-in, capa libre a 0 vs perfecta) y 2 de dos-juntas (sueño mal + recaída) para cerrar esos huecos.
4. **`k_sobr > k_sleep`** hace que la recaída pese más que el mal sueño en el apretón. Coherente con la calibración por separado, pero si el dueño quisiera el sueño por encima de TODO, este modelo no lo da en el apretón. Es la consecuencia de respetar el sustento de Conducta (50–63%).
5. **Cortes anchos en base pura.** Los cortes 0.40/0.62/0.85 se calibraron con bandas anchas (lote BP/REF). Casos cerca de los cortes (BP-AP5, BP-AC en 0.80) tienen margen 0.05 — cómodo, pero si se afinan los cortes habría que re-chequear.

**Veredicto:** modelo completo, mínimo, sin reglas, 45/45. Las dos zonas de incertidumbre (SU5/SU6 al filo; apretón/dos-juntas sin dato) están declaradas explícitamente para que la síntesis y el dueño decidan si re-marcar antes de congelar.

---

### Anexo — implementación de referencia (pseudocódigo)

```
PARAMS = { β:0.55, s_bad:0.20, k_sleep:1.5, k_sobr:3.0,
           r_relapse:0.50, r_unmarked:0.50, p_sop:0.25, b_sop:0.02,
           c_RA:0.40, c_AEM:0.62, c_EMP:0.85 }

func layer_value(L, a_L, sleep, sobr, s_L):
    if L == Cuerpo and sleep != off:
        sv = (sleep == ok) ? 1.0 : s_bad
        core = β·sv + (1−β)·a_L
    elif L == Conducta and sobr != off:
        core = (sobr == clean)    ? a_L
             : (sobr == relapse)  ? r_relapse
             :                      r_unmarked      # unmarked
    else:
        core = a_L
    if L has support:
        core = core + b_sop·s_L − p_sop·(1−s_L)
    return clamp(core, 0, 1)

func layer_weights(actives, sleep, sobr):
    w = { L: 1.0 for L in actives }
    if sleep != off: w[Cuerpo]   *= k_sleep
    if sobr  != off: w[Conducta] *= k_sobr
    tot = sum(w.values()); return { L: w[L]/tot for L in actives }

func score(state):
    w = layer_weights(state.actives, state.sleep, state.sobr)
    S = Σ_L w[L] · layer_value(L, state.a[L], state.sleep, state.sobr, state.s[L])
    band = S < c_RA ? R : S < c_AEM ? A : S < c_EMP ? EM : P
    if band == P and all_anchors_100 and superhabit_layer_count ≥ 2: band = Inquebrantable
    return (S, band)
```
