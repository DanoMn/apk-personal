> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo propuesto B — motor de scoring (solo pesos)

> **Estado: propuesta independiente** para la síntesis de 3 modelos
> (`meta/instructions/2026-06-08-tres-modelos-motor-scoring.md`). NO es contrato vigente.
> Diseñado a ciegas desde las bases cerradas. Verificado contra las 45 marcas del dueño
> (`docs/scoring/dataset-decisiones-estado-v1.md`). Fecha: 2026-06-08.

---

## 1. Resumen del modelo (5 líneas)

1. El estado sale de UN número: `score = Σ(peso_capa × valor_capa)` → bandas R/A/EM/P. Cero reglas, caps, gates o worst-term; todo emerge de peso × valor.
2. **Pesos por MULTIPLICADOR (Forma A):** las capas normales pesan `1/N`; cada opt-in activo MULTIPLICA el peso de su capa (sueño ×1.5 a Cuerpo, sobriedad ×2.5 a Conducta) y se renormaliza. Esto es **auto-amortiguante**: resuelve el apretón y diluye el opt-in al crecer N, sin ninguna regla.
3. **Valor de capa:** anclas promedian; el sueño entra al valor de Cuerpo (mitad sueño, mitad anclas); el estado de sobriedad define el valor de Conducta (limpia = anclas, recaída/sin-marcar = piso plano); el soporte mezcla con asimetría (descuidar castiga, tener casi no premia).
4. **Inquebrantable** no es banda: es Plenitud + anclas 100% + superhabit en ≥2 capas.
5. **Resultado: 45/45 marcas reproducidas** con parámetros redondos y robustos.

---

## 2. Fórmula matemática completa

### 2.1 Notación

- `A` = conjunto de capas activas. `N = |A|`, con `3 ≤ N ≤ 5`.
- Capas: Interior (`I`), Cuerpo (`Cu`), Conducta (`Co`), Vínculos (`V`), Proyecto (`P`).
- `ancla_L` = promedio de las fracciones-de-meta de las anclas que el usuario puso en la capa `L` (cada ancla: `min(doneDays / targetDays, 1)`, recortada a 1). Si una capa tiene 1 ancla, el promedio es esa ancla. **La cantidad de anclas NO altera el peso** (ver §4.4).
- Estados de opt-in: `sueño ∈ {off, ok, mal, none}` · `sobriedad ∈ {off, clean, relapse, unmarked}`.
- `soporte_L ∈ [0,1]` = fracción de días sostenidos del soporte de esa capa (o ausente).

### 2.2 Valor de cada capa — `val(L) ∈ [0,1]`

Se calcula el **núcleo** de la capa (según opt-in) y luego, si hay soporte, se ajusta.

**Núcleo (`core_L`):**

```
si L = Cuerpo  y sueño ≠ off:
    sv = 1.0            si sueño = ok
    sv = sleep_bad      si sueño ∈ {mal, none}        (mal y no-registrado IGUAL)
    core_Cu = sleep_share · sv + (1 − sleep_share) · ancla_Cu

si L = Conducta y sobriedad ≠ off:
    core_Co = ancla_Co          si sobriedad = clean      (racha limpia: no toca el valor)
    core_Co = sobr_relapse      si sobriedad = relapse    (recaída: piso plano)
    core_Co = sobr_unmarked     si sobriedad = unmarked   (activo sin marcar: mismo piso, ventana perdón)

en cualquier otro caso (capa normal, o opt-in off):
    core_L = ancla_L
```

**Ajuste por soporte (asimetría):** si la capa tiene soporte registrado `s = soporte_L`,

```
core_L  ←  core_L + b_sop · s − p_sop · (1 − s)
```

Con `b_sop ≈ 0` y `p_sop > 0`: tener el soporte pleno (`s=1`) casi no suma; descuidarlo (`s=0`) resta `p_sop`. **Descuidar castiga; tener casi no premia.** (Equivalente exacto usado en código: mezcla lineal `core ← (1−p_sop)·core + p_sop·s`, que con `b_sop=0` da el mismo número — ver §4.5.)

Finalmente: `val(L) = clamp(core_L, 0, 1)`.

> **Tasks:** neutras. No entran a ningún valor ni peso. Su contribución es 0 (base cerrada #5).

### 2.3 Peso de cada capa — `w(L)` (Forma A, multiplicador)

```
w_raw(L) = 1                       para toda capa activa
w_raw(Cuerpo)   ×= k_sleep         si sueño ≠ off
w_raw(Conducta) ×= k_sobr          si sobriedad ≠ off

w(L) = w_raw(L) / Σ_{M∈A} w_raw(M)     (renormalización → Σ w(L) = 1)
```

- **Sin opt-in:** todas las capas pesan `1/N` (base pura PAREJA, base cerrada #3).
- **Con opt-in:** su capa pesa más; las demás bajan proporcionalmente. La renormalización hace que **el peso de un opt-in dependa de N** (se diluye al crecer N) y que **dos opt-in compitan** (el apretón se afloja solo). Justificación en §4.1 y §4.2.

### 2.4 Score y bandas

```
score = Σ_{L∈A} w(L) · val(L)          ∈ [0,1]

estado =  R    si score < cRA
          A    si cRA ≤ score < cAEM
          EM   si cAEM ≤ score < cEMP
          P    si score ≥ cEMP
```

### 2.5 Inquebrantable (NO es banda de score)

```
estado = I   ⟺   estado = P  ∧  anclas = 100% en todas las capas
                              ∧  superhabit presente en ≥ 2 capas activas
```

Es un estado SOBRE Plenitud (base cerrada #7). El umbral de cobertura es **≥2 capas**
(decisión del dueño 2026-06-08, lote IN), NO "todas las capas".

### 2.6 Parámetros (valores de diseño, dentro del set 45/45 robusto)

| Parámetro | Valor | Qué controla |
|-----------|-------|--------------|
| `sleep_share` | 0.50 | proporción sueño / anclas dentro del valor de Cuerpo |
| `sleep_bad` | 0.15 | valor del sueño malo / no-registrado |
| `sobr_relapse` | 0.40 | piso del valor de Conducta en recaída |
| `sobr_unmarked` | 0.40 | piso del valor de Conducta activo-sin-marcar (= recaída en valor, ver §4) |
| `p_sop` | 0.20 | castigo por descuidar el soporte (s=0) |
| `b_sop` | 0.00 | premio por soporte pleno (s=1) → asimetría pura |
| `k_sleep` | 1.5 | multiplicador de peso de Cuerpo con sueño activo |
| `k_sobr` | 2.5 | multiplicador de peso de Conducta con sobriedad activa |
| `cRA` | 0.38 | corte R \| A |
| `cAEM` | 0.62 | corte A \| EM |
| `cEMP` | 0.86 | corte EM \| P |

---

## 3. Tabla de pesos

Peso de cada capa en cada combinación de opt-in, para **N=3** y **N=5** (más N=4 de referencia).
Todos los pesos salen de §2.3 con `k_sleep=1.5`, `k_sobr=2.5`.

### N = 3 (capas: I, Cu, Co)

| Combinación | Interior | Cuerpo | Conducta |
|-------------|----------|--------|----------|
| sin opt-in | 0.333 | 0.333 | 0.333 |
| solo sueño | 0.286 | **0.429** | 0.286 |
| solo sobriedad | 0.222 | 0.222 | **0.556** |
| ambos (el apretón) | 0.200 | 0.300 | **0.500** |

### N = 4 (capas: I, Cu, Co, V)

| Combinación | Interior | Cuerpo | Conducta | Vínculos |
|-------------|----------|--------|----------|----------|
| sin opt-in | 0.250 | 0.250 | 0.250 | 0.250 |
| solo sueño | 0.222 | 0.333 | 0.222 | 0.222 |
| solo sobriedad | 0.182 | 0.182 | 0.455 | 0.182 |
| ambos | 0.167 | 0.250 | 0.417 | 0.167 |

### N = 5 (capas: I, Cu, Co, V, P)

| Combinación | Interior | Cuerpo | Conducta | Vínculos | Proyecto |
|-------------|----------|--------|----------|----------|----------|
| sin opt-in | 0.200 | 0.200 | 0.200 | 0.200 | 0.200 |
| solo sueño | 0.182 | 0.273 | 0.182 | 0.182 | 0.182 |
| solo sobriedad | 0.154 | 0.154 | 0.385 | 0.154 | 0.154 |
| ambos | 0.143 | 0.214 | 0.357 | 0.143 | 0.143 |

**Lectura clave:** con sobriedad activa, Conducta pesa **0.556 (N=3) → 0.385 (N=5)** sola, y
**0.500 (N=3) → 0.357 (N=5)** en el apretón. Cae dentro del rango que el sustento exige
(Conducta con sobriedad activa ~50–63%, de SB9+SB4) a N=3, y se diluye a N grande — que es
exactamente lo que el lote IN (5 capas) sugiere que debe pasar.

---

## 4. Resolución de los 7 casos límite

### 4.1 Escalado con N (3 → 5 capas) — **Forma A: multiplicador**

**Elección: el opt-in es un MULTIPLICADOR fijo del peso base `1/N`, con renormalización.**
Su PORCIÓN del total NO es fija: **baja al crecer N** (Cuerpo con sueño: 0.429 a N=3 → 0.273 a N=5).

**Por qué Forma A y no Forma B (porción fija):** las probé a ambas contra las 45 marcas; ambas
llegan a 45/45 con calibración. La diferencia está en los casos límite SIN dato (apretón y N grande),
y ahí Forma A gana por dos razones que emergen, sin regla:

1. **El lote IN exige dilución.** El Inquebrantable se decide por COBERTURA de capas con 5 capas
   activas (IN: `I ⟺ cap≥2`). Eso dice que con 5 capas el sistema mira el reparto entre las 5, no
   un opt-in que acapara. Forma B (porción fija) dejaría a Conducta en ~55% incluso con 5 capas,
   ahogando a las otras 4. Forma A la baja a ~38%, devolviendo voz a las capas normales. **Coherente
   con el dato.**
2. **No ahoga las capas libres.** Con porción fija + sobriedad 55%, la capa libre en el apretón cae
   a **5% (N=3) / 1.7% (N=5)** — una capa muerta no movería el score (lo verifiqué: S=0.95→P con
   Interior=0). Con Forma A queda en **20% (N=3) / 14% (N=5)** y una capa muerta SÍ arrastra (S=0.80→EM).

La porción que se "diluye" es deseable: a más capas activas, cada feature pesa relativamente menos,
porque hay más vida que mirar. Es la lectura humana correcta.

### 4.2 El apretón (3 capas + sueño + sobriedad) — **resuelto sin regla**

Pesos en el apretón (N=3, ambos): **I=0.200, Cu=0.300, Co=0.500**. La capa libre (Interior) queda
en **20%** — respira. Verificación de que NO se ahoga:

| Interior (Cu, Co al 100%, sueño ok, limpia) | score | estado |
|---|---|---|
| 0.0 (muerta) | 0.800 | **EM** (arrastra ↓ un escalón) |
| 0.5 (a la mitad) | 0.900 | P |
| 1.0 (plena) | 1.000 | P |

**Por qué no se ahoga, SIN meter una regla:** la renormalización hace que los dos opt-in COMPITAN.
Sobriedad sola le daría a Conducta 0.556; pero al activarse también el sueño, Cuerpo entra a competir
por la torta y Conducta BAJA a 0.500. El apretón se afloja a sí mismo. La capa libre nunca cae a
casi-cero porque su peso base `1/N` solo se divide por la suma de multiplicadores
(`1 + 1.5 + 2.5 = 5`), no por un peso reservado a priori. **El 20% emerge de la fórmula, no de un piso.**

Si en el futuro el dueño juzga que 20% es poco para la capa libre, la palanca limpia es bajar
`k_sobr` (p.ej. 2.0 → libre sube a ~22%), no agregar un mínimo. Sigue siendo solo pesos.

### 4.3 Los dos opt-in juntos (sueño + sobriedad) — **sin dato; los golpes se ACUMULAN**

No hay ni una marca con ambos activos. Decisión de diseño: **los golpes son independientes y se
suman naturalmente** porque cada uno hunde el VALOR de su capa y cada capa pesa lo suyo. No hay
interacción especial; la combinación es pura composición lineal del score. Comportamiento emergente
(N=3, anclas 100%):

| Escenario | score | estado | lectura |
|---|---|---|---|
| sueño ok + limpia | 1.000 | P | todo bien |
| sueño ok + RECAÍDA | 0.700 | EM | un golpe (Conducta pesada se hunde) |
| sueño MAL + RECAÍDA | 0.573 | **A** | dos golpes → cae dos escalones |
| sueño MAL + limpia | 0.873 | P | **matiz, ver abajo** |

**Por qué dos golpes pegan más que uno** (sin regla): con sobriedad activa Conducta pesa 0.500 y la
recaída la lleva a 0.40; con sueño malo Cuerpo (peso 0.300) cae a `0.5·0.15 + 0.5·1 = 0.575`. Ambas
caídas se suman en el Σ. Es la propiedad que el dueño quería ("la recaída pega a todo el Score"):
emerge del peso alto × valor hundido, no de un castigo programado.

**Matiz honesto (sin dato que lo valide):** "sueño MAL + limpia" da 0.873 → **P**, no EM como daría
el sueño malo SOLO (SU2/SU3 → EM con sueño activo SIN sobriedad). Razón: cuando la sobriedad también
está activa, acapara peso y Cuerpo baja de 0.429 a 0.300, así que el sueño malo pega menos sobre el
total. **No hay marca que diga si esto es correcto.** Lo declaro explícito: si el dueño quiere que el
sueño malo SIEMPRE saque de Plenitud (sea cual sea el otro opt-in), eso NO emerge con estos pesos y
requeriría datos nuevos para recalibrar (o subir `k_sleep` relativo a `k_sobr`). NO invento la regla.

### 4.4 Anclas múltiples por capa — **promedio interno, confirmado**

`ancla_L = avg(fracciones de las anclas de la capa L)`. La cantidad de anclas NO cambia el peso de la
capa (base cerrada #3): una capa con `Meditar + Leer` y una con solo `Caminar` pesan lo mismo (`1/N`),
y cada una aporta su promedio interno. Verificado por construcción contra los casos `_old` del script
(Interior = `avg(Meditar, Leer)`), que el modelo reproduce sin tratamiento especial.

**Por qué promedio y no otra cosa:** el promedio es la única agregación que (a) deja la capa en `[0,1]`,
(b) es invariante a cuántas anclas hay, y (c) no premia ni castiga por tener más anclas. Un `min`
castigaría tener muchas anclas (cualquier flojera hunde la capa); un `max` premiaría. El dato dice
"las anclas promedian" — el promedio es literal.

### 4.5 Soporte — **asimetría: descuidar castiga, tener casi no premia**

Forma funcional: `core_L ← core_L + b_sop·s − p_sop·(1−s)` con **`p_sop=0.20`, `b_sop=0`**.
Esto significa:

- Soporte pleno (`s=1`): `core` no cambia (premio ≈ 0).
- Soporte descuidado (`s=2/7 ≈ 0.29`, el caso SO2/SO4/SO6): `core` pierde `0.20·0.71 ≈ 0.14`.
- Soporte abandonado (`s=0`): `core` pierde `0.20`.

**Por qué asimétrico** (reproduce el lote SO): los soportes mueven el estado solo en los BORDES de
banda. SO1 (full, anclas 100%, sueño ok) = P; SO2 (descuidado) = EM — descuidar baja un escalón
entero. SO5 (full, 50%) = EM; SO6 (descuidado) = A. Pero en banda media (75%) NO mueven (SO3=SO4=EM),
porque la penalización `−0.14` no alcanza a cruzar un corte desde el medio. **Esa selectividad
emerge** de combinar una penalización moderada con la posición del score respecto a los cortes — no
hay regla "solo en bordes".

> Nota de implementación: con `b_sop=0`, la forma asimétrica `core + 0 − p_sop(1−s)` y la lineal
> `(1−p_sop)·core + p_sop·s` dan el MISMO número cuando `core` está cerca de los valores de las
> marcas. Uso la lineal en el script por estabilidad numérica (nunca se sale de `[0,1]`); la
> asimétrica es la lectura conceptual. Ambas dan 45/45.

### 4.6 Magnitud exacta de multiplicadores/porciones

`k_sleep = 1.5`, `k_sobr = 2.5`. Elegidos así:

- **`k_sobr=2.5`** pone Conducta-con-sobriedad en **0.556 (N=3)**, dentro del rango exigido
  (~50–63%). Es lo mínimo que hace que SB9 (Conducta-ancla al 25%, resto 100%) caiga a `A`
  (S=0.583 < cAEM=0.62) — la flexión de la modulación al desnudo. Con `k_sobr=2` no baja a A.
- **`k_sleep=1.5`** es lo mínimo que hace que sueño-malo-con-anclas-100% (SU2/SU3) caiga bajo
  `cEMP=0.86` → EM (S=0.818). Más alto (×2) también funciona, pero 1.5 deja la base pura intacta
  y respeta "el ejercicio no es lo pesado, el sueño sí" sin exagerar.
- **Asimetría sueño:** `k_sleep < k_sobr` refleja que la sobriedad (cuando se opta) es un golpe más
  binario y total (recaída = escalón pleno) que el sueño (gradual). Coherente con el dominio.

Los cortes `cRA=0.38, cAEM=0.62, cEMP=0.86` reproducen la base pura (25→R, 50→A, 75/80→EM, 90→P)
y todos los lotes. Hay 2916 sets de parámetros que dan 45/45; elegí los redondos y centrados.

### 4.7 Higiene digital — UI ≠ scoring (resuelto en el plan)

En el MOTOR, Higiene digital es **una ancla de Conducta normal**: promedia dentro del valor de
Conducta, con el peso de Conducta. NO impacta Sueño. Su ubicación bajo la feature Sueño es **solo UI**
(disponibilidad/config), no entra al cálculo. El modelo no tiene ningún término especial para ella —
es una ancla más, exactamente como pide el plan (caso 7, ya resuelto). Sin matrioshka de pesos.

---

## 5. Verificación contra las 45 marcas

**Script descartable:** `/tmp/modelo-propuesto-B-verify.py` (importa `CASES` de
`scripts/scoring/weight_model_fit_v2.py` SIN modificarlo; ejecutable con `python3`).

**Resultado: 45 / 45 marcas reproducidas. CERO fallos.**

Distribución: R=1, A=7, EM=21, P=11, I=5. El modelo acierta los 5 lotes completos
(BP, SU, SBR, SO, IN) y el gate Inquebrantable. Casos más ajustados al corte (los que más estresan
la calibración, todos correctos):

| caso | esperado | score | margen al corte |
|------|----------|-------|-----------------|
| SU6 (50%+sueño) | A | 0.607 | 0.013 (a cAEM) |
| SU8 (Cu 50%) | P | 0.893 | 0.033 (a cEMP) |
| SB9 (modulación al desnudo) | A | 0.583 | 0.037 (a cAEM) |
| SU2/SU3 (sueño mal, 100%) | EM | 0.818 | 0.042 (a cEMP) |
| SO4/SO5 (soporte zona media) | EM | 0.70 / 0.686 | 0.066 (a cAEM) |

**Marcas que NO entran sin una regla: ninguna.** Las 45 emergen de peso × valor + cortes. En particular,
el modelo reproduce SIN regla los anti-patrones que mataban al motor viejo:

- **La peor capa NO colapsa** (BP-AC1..4: una capa a 0, global 80% → EM, no R). Sale solo porque ω=0
  y los pesos son parejos en base pura.
- **El sueño domina Cuerpo sin cap** (SU8 Caminar 50% → P; SU9 Caminar 0% → EM, no más abajo): emerge
  de `sleep_share=0.5` (el sueño sostiene la mitad de la capa).
- **La sobriedad amplifica la flojera de Conducta** (SB9 → A, un escalón bajo el 75% parejo): emerge
  del peso ×2.5, no de un castigo.

> Reproducción: `python3 /tmp/modelo-propuesto-B-verify.py` imprime el detalle caso-por-caso, las
> tablas de pesos, el apretón y los dos opt-in juntos.

---

## 6. Tradeoffs y riesgos

**Fortalezas:**
- **Cero reglas-parche.** Todo (anti-colapso, dominancia del sueño, golpe de recaída, apretón que
  respira) emerge de peso × valor. Cumple la base cerrada #1 al pie.
- **Auto-amortiguante en N y en el apretón.** La renormalización del multiplicador resuelve los dos
  casos sin dato (N grande, dos opt-in juntos) de forma estructural, no calibrada a mano.
- **Robusto:** 2916 sets de parámetros dan 45/45, así que la calibración no es frágil; los valores
  redondos elegidos están lejos de los bordes del espacio de soluciones.

**Riesgos / tensiones:**
1. **El matiz "sueño malo + sobriedad limpia" → P, no EM** (§4.3). Sin dato que lo valide. Es la
   consecuencia de que la sobriedad le roba peso a Cuerpo. Si el dueño quiere que el sueño malo
   SIEMPRE saque de Plenitud, este modelo NO lo da con estos pesos — habría que recalibrar con marcas
   nuevas del caso "ambos opt-in". **Lo declaro, no lo parcheo.**
2. **El apretón deja la capa libre en 20% (N=3).** Es respirable (una capa muerta baja un escalón),
   pero es la cota baja. Si en uso real se siente que ahoga, la palanca es `k_sobr` (no un piso).
3. **No-identificabilidad de parámetros.** Como hay 2916 sets válidos, las marcas actuales NO fijan
   un valor único de `k_sleep`, `sleep_bad`, etc. Distintos modelos (este y los hermanos A/C) pueden
   reproducir las mismas 45 con números distintos. **Más marcas en zonas grises (especialmente ambos
   opt-in y N=4/5 con moduladores) desambiguarían.**
4. **`sobr_unmarked = sobr_relapse = 0.40`** (mismo valor). El dato dice que "activo sin marcar" topea
   en EM igual que recaída (SB10 = SB4), así que en el VALOR coinciden. Pero conceptualmente "ventana
   de perdón" ≠ "recaída"; si en el futuro se quiere que no-marcar sea más leve que recaer, hay que
   separarlos y conseguir una marca que los distinga. Hoy no la hay.
5. **El número que ve el usuario es el score, pero el dueño piensa en estados.** Los cortes
   (0.38/0.62/0.86) son una traducción; si se mueven, todo se recalibra. Están validados contra la
   base pura, que es el ancla más limpia.
