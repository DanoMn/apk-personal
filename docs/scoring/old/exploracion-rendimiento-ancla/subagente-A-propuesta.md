# Propuesta — Proponente A: Dominancia fuerte de frecuencia

> Exploración independiente del rendimiento de un ancla. Sesgo asignado: **dominancia fuerte de frecuencia**.
> Sesión 2026-06-09. Derivación desde axiomas, sin búsqueda web, sin leer otras propuestas ni el código viejo.

---

## 1. Sesgo asignado — cómo interpreté la dominancia fuerte de frecuencia

La consigna me pide la familia de funciones donde **los días pesan tanto que el tiempo casi no mueve la
aguja salvo en extremos**. Mi lectura, llevada al límite honesto que los axiomas permiten:

- **La frecuencia es el esqueleto; el tiempo, hasta el target, es relleno del esqueleto.** Un día de
  compromiso no aporta "minutos": aporta hasta **un slot lleno** y nada más. El cumplimiento de
  frecuencia es la suma de slots llenos sobre `F`. Esto hace que la constancia sea estructural: no hay
  forma de que el tiempo de un día "se derrame" sobre los días faltantes. Falta un día ⟹ falta un slot ⟹
  la base nunca llega a 1 por más minutos que se acumulen en otro día.
- **El tiempo solo "mueve la aguja en extremos"** de dos maneras: (a) por debajo, cuando un día de
  compromiso no alcanza el target (déficit que baja el slot de 1 a `t/T`); (b) por encima, cuando hay
  **superávit sobre una base ya construida** — y ese superávit es el único canal que cruza por encima de 1.
- **La dominancia se acota por construcción, no por parche.** El canal de superávit de tiempo está
  **ponderado por la base ya completada** (`base^p`). Si la base es parcial, el tiempo extra casi no
  cuenta; recién con base completa el superávit despliega todo su peso. Eso resuelve la tensión que el
  brief marca como crítica (P2): cuando `F = 7` no hay superávit de días posible, pero la base se completa
  con frecuencia perfecta y entonces el superávit de tiempo **sí** puede empujar hasta Inquebrantable.
- **El peso del superávit crece con F** vía un factor `g(F) = (F/7)^κ`. A menor `F`, el usuario tiene
  margen para sumar días voluntarios (otra vía de superávit), así que el tiempo pesa poco. A `F = 7`, esa
  vía se cierra y el factor llega a su máximo (`g = 1`). La dominancia de frecuencia es, así, **máxima
  cuando hay días por ganar y mínima cuando ya no los hay** — exactamente lo que pide P2.

En síntesis: la frecuencia define **dónde** está el rendimiento (la base, territorio `[0,1]`); el tiempo
solo modula **dentro** de cada slot (déficit) o **por encima** de la base completa (superávit). Nunca
compite de igual a igual con la frecuencia.

---

## 2. Fórmula explícita

### 2.1 Notación

Para un ancla con frecuencia objetivo `F ∈ {1..7}`, tiempo objetivo `T > 0` y array de 7 días:

- `D` = cantidad de días marcados (`t_i > 0`).
- Si `D = 0` ⟹ `R = 0` (cierra A2; el resto de la fórmula no se evalúa).
- Ordenar los `D` días marcados por tiempo **descendente**.
- **Zona de compromiso** = los primeros `min(D, F)` días (los de mayor tiempo). Índice `j`.
- **Zona voluntaria** = los `max(D − F, 0)` días restantes (los de menor tiempo). Índice `k`.
- Razones porcentuales (invarianza de escala, A10): `r_j = t_j / T`, `r_k = t_k / T`.

### 2.2 Piezas

**(a) Cumplimiento de frecuencia (la base del esqueleto):**

```
φ = (1/F) · Σ_j  min(r_j, 1)
```

Cada día de compromiso aporta como máximo **un slot lleno** (`min(r_j,1) ≤ 1`). Se divide por `F`, no por
`min(D,F)`: los slots faltantes (`F − D` cuando `D < F`) cuentan como `0`. Por eso `φ < 1` **siempre que
falte un día comprometido**, sin importar cuánto tiempo se acumule en otro lado. `φ ∈ [0, 1]`.

**(b) Días-equivalentes voluntarios (piso cero, peso menor):**

```
V = Σ_k  min(r_k, 1)         (suma sobre la zona voluntaria; cada término en [0,1])
```

`V` mide cuántos "días extra" aportó el usuario, ponderados por su fracción de tiempo. Es siempre `≥ 0` y
tiende a `0` cuando los tiempos voluntarios tienden a `0` (A7).

**(c) Base (frecuencia + voluntario, saturada suavemente a 1):**

```
base = 1 − (1 − φ) · exp(−λ_v · V)
```

Interpretación: `(1 − φ)` es el **déficit de base** (slots de compromiso sin llenar). El voluntario
**rellena ese déficit con rendimientos decrecientes** y **nunca lo lleva por encima de 1**:
- Si `φ = 1` (base ya completa) ⟹ `base = 1` sin importar `V`. El voluntario no infla la base.
- Si `φ < 1` ⟹ cada día voluntario reduce el déficit exponencialmente; cuanto más cerca de la base
  completa, menos aporta cada día extra. Continuo y monótono creciente en `V` y en `φ`.
- `V → 0` ⟹ `base → φ`.

`λ_v ∈ (0, 1)` es la **fuerza del voluntario**: bajo ⟹ los días extra (sobre todo los triviales) casi no
suben la base; alto ⟹ suben más rápido. El rango `(0,1)` garantiza que un día voluntario vale **menos**
que un día de compromiso.

**(d) Superávit de tiempo (canal Inquebrantable, ponderado por base completa, escalado con F):**

```
S = (1/F) · Σ_j  max(r_j − 1, 0)          (solo en zona de compromiso; el exceso sobre el target)
g(F) = (F/7)^κ                             (peso que crece con F; g(7)=1)
```

**(e) Rendimiento final:**

```
R = base  +  μ · g(F) · base^p · S
```

- `base ∈ [0, 1]`: territorio normal, dominado por frecuencia.
- `μ · g(F) · base^p · S`: el **único término que cruza por encima de 1**. Está ponderado por `base^p`,
  así que un superávit sobre base incompleta aporta poco (con `p ≥ 1`, cada vez menos), y solo con base
  ≈ 1 despliega su peso pleno. Esto hace de "superávit sobre base completada" una propiedad **estructural
  y continua**, sin gate duro.

### 2.3 Parámetros calibrables (sin fijar — rangos plausibles)

| Param | Rol | Rango plausible |
|-------|-----|-----------------|
| `λ_v` | Fuerza del aporte voluntario (días extra) | `0.2 – 0.7` |
| `μ`   | Ganancia global del superávit de tiempo | `0.4 – 0.8` |
| `κ`   | Cuánto crece el peso del superávit con F | `1 – 2` |
| `p`   | Dureza del requisito "base completa" para el superávit | `1 – 2` |

Adicionalmente, para mapear a estados:

| Param | Rol | Rango plausible |
|-------|-----|-----------------|
| `δ_inq` | Margen sobre 1 que define **Inquebrantable** (`R ≥ 1 + δ_inq`) | `0.10 – 0.20` |

> Las bandas de los 4 primeros estados (`Rojo < 0.40 · Amarillo < 0.62 · En marcha < 0.85 · Pleno ≥ 0.85`)
> se aplican sobre `R` truncado a `[0,1]` para la clasificación; **Inquebrantable** se activa cuando
> `R ≥ 1 + δ_inq`. Como `R > 1` solo es alcanzable por el término de superávit (que exige `base ≈ 1`),
> Inquebrantable hereda automáticamente la condición "superávit sobre base completada" sin necesidad de un
> gate explícito (A9 respetado).

---

## 3. Derivación desde los axiomas

**A1 (Normalización `[0, 1+]`, exacto = 1, superávit > 1).** `base ∈ [0,1]` por construcción (la
saturación suave nunca pasa de 1). El único término que supera 1 es el superávit, que es `≥ 0`. Con
cumplimiento exacto, `φ = 1`, `S = 0` ⟹ `R = 1`. Con superávit, `S > 0` y `base = 1` ⟹ `R > 1`.

**A2 (Piso cero).** `D = 0` se corta de entrada con `R = 0`. Ningún término puede ser negativo: `φ ≥ 0`,
`V ≥ 0`, `base ≥ 0` (porque `(1−φ)·exp(...) ≤ 1`), `S ≥ 0`, `g ≥ 0`. No hay restas posibles.

**A3 (Cumplimiento exacto = 1).** `D = F`, todos `t_j = T` ⟹ `r_j = 1`, `min(r_j,1) = 1`, `φ = 1`,
`V = 0`, `S = 0` ⟹ `base = 1`, `R = 1`. Exacto, sin parámetros que lo ensucien.

**A4 (Monotonía en días).** Agregar un día marcado:
- Si el nuevo día entra al compromiso desplazando a uno menor a la zona voluntaria, ambos términos suben o
  quedan igual (el día desplazado ahora cuenta en `V` con piso cero, y el nuevo aporta a `φ` al menos lo
  mismo que aportaba el desplazado).
- Si entra directo a la zona voluntaria, sube `V` ⟹ sube `base`.
En ningún caso baja. La estructura "los F mejores al compromiso" garantiza que reordenar por un día nuevo
nunca degrada el cumplimiento de compromiso.

**A5 (Monotonía en tiempo de compromiso).** Más minutos hasta `T` en un día de compromiso suben `min(r_j,1)`
linealmente ⟹ sube `φ` ⟹ sube `base`. Más allá de `T`, suben `S` ⟹ sube el término de superávit (con
`base` fija o creciente). Monótono no decreciente en todo el rango.

**A7 (Piso cero en zona voluntaria).** `V = Σ min(r_k,1) ≥ 0` y cada término `→ 0` cuando `r_k → 0`.
Entra a la fórmula solo a través de `exp(−λ_v·V)`, que es `≤ 1`: el voluntario solo puede **subir** la
base (reducir el déficit), nunca bajarla. Matemáticamente incapaz de producir un término negativo.

**A9 (Continuidad / sin gates).** Todas las piezas son composiciones de funciones continuas: `min`, `max`,
suma, producto, exponencial, potencia. `min`/`max` son C0 (continuas, sin saltos). La reasignación Best-F
(ordenar) introduce **intercambios continuos**: cuando dos días empatan en tiempo, da igual cuál va al
compromiso (el valor de la función es idéntico en el cruce), así que no hay discontinuidad. El barrido
numérico fino (paso 0.5 min, cruzando la frontera `r = 1` y la frontera compromiso/voluntario) da un salto
máximo de `0.005` por paso: comportamiento suave, sin gates.

**A10 (Invarianza de escala).** Toda la fórmula depende SOLO de `r_j = t_j/T` y de cardinalidades (`D`, `F`).
Duplicar `T` y todos los `t_i` deja cada `r` idéntico ⟹ `R` idéntico. Verificado numéricamente:
`(F=3, T=30, [40,30,30])` y `(F=3, T=120, [160,120,120])` dan el mismo `R`.

**Frecuencia sobre intensidad (estructural, restricción §8.3).** Está en la arquitectura, no en un
parámetro: el tiempo de compromiso satura en `min(r,1)` (no aporta a la base más allá del slot), y el
superávit que sí pasa de 1 está estrangulado por `base^p`. Una ráfaga de 1 día jamás construye base:
`φ = 1/F` techa la base muy abajo y `base^p` ahoga el superávit. Solo la repetición de días llena la base.

**P1 (normalización frecuencia↔tiempo, canales de superávit).** Resuelto: hay **dos canales de superávit
distintos**, no una sola bolsa. (i) Días voluntarios → suben `base` hacia 1 (via `V`), pero nunca la pasan.
(ii) Tiempo extra en compromiso → único canal que cruza 1 (via `S`). El déficit de frecuencia (`D < F`)
no tiene techo duro: aporta por su cuenta vía `φ` parcial + `V`, pero el tiempo (`S` estrangulado por
`base^p`) **no lo compensa**. El "punto medio" emerge de `λ_v`, `μ`, `p` — calibrables.

**P2 (superávit de tiempo alcanza Inquebrantable solo, peso escala con F).** Con `F = 7`, `D/F` topa en 1
y no hay días voluntarios posibles; la base se completa solo con frecuencia perfecta (`φ = 1`), `base = 1`,
y entonces `R = 1 + μ·1·1·S`. El factor `g(7) = 1` es el máximo, así que el superávit de tiempo tiene su
peso pleno justo cuando es la única vía. Verificado: `F = 7` con todos los días a `1.5·T` da `R = 1.3`,
sobre el umbral de Inquebrantable.

---

## 4. Autoverificación (números calculados)

> Parámetros **ilustrativos** usados para verificar: `λ_v = 0.5`, `μ = 0.6`, `κ = 1.0`, `p = 1.0`,
> `δ_inq = 0.15`. No son valores fijados — solo sirven para comprobar el comportamiento. Cálculos
> reproducidos con `python3` (cálculo local).

### 4.1 Axiomas

| Axioma | Prueba | Resultado | Veredicto |
|--------|--------|-----------|-----------|
| **A1** exacto = 1 | `F=3,T=30,[30,30,30]` | `R = 1.0000` | OK |
| **A1** superávit > 1 | `F=7,T=30,[60×7]` (`r=2`) | `R = 1.6000` | OK |
| **A2** piso cero | `D=0` | `R = 0` | OK |
| **A3** cumplimiento exacto | `F=5,T=20,[20×5]` | `R = 1.0000` | OK |
| **A4** monotonía días | `F=3,[30,30,d]`, `d=0,1,5,15,30` | `0.667 → 0.678 → 0.722 → 0.833 → 1.000` (no decreciente) | OK |
| **A5** monotonía tiempo | `F=3,[t,30,30]`, `t=0..30` | `0.667 → … → 1.000` (no decreciente) | OK |
| **A7** piso cero vol. | `[30,30,ε]` vs `[30,30,0]` | difieren en `<0.001`, ambos ≈1 | OK |
| **A9** continuidad | barrido `t∈[0,200]` paso 0.5, cruzando `r=1` y frontera de zona | salto máx `0.0051` | OK |
| **A10** invarianza escala | `(T=30,[40,30,30])` vs `(T=120,[160,120,120])` | `1.0286 = 1.0286` | OK |

### 4.2 Casos límite de §7

**Cumplimiento exacto** — `D=F=3`, `[30,30,30]`, `T=30`:
`φ=1, V=0, S=0 → base=1 → R = 1.0000`. **Esperado: 1.** OK

**Nada hecho** — `D=0`: `R = 0`. **Esperado: 0.** OK

**Superávit días + déficit tiempo** — `F=3,T=30,[10,10,10,90,90]`:
Best-F: compromiso = `[90,90,10]` (`r=3,3,0.333`), voluntarios = `[10,10]` (`r=0.333,0.333`).
`φ = (1+1+0.333)/3 = 0.778`, `V = 0.667`, `S = (2+2+0)/3 = 1.333`, `base = 0.841`,
`R = 0.841 + 0.6·0.429·0.841·1.333 = 1.129`.
El día de 90 **no tapa el déficit** del día de 10: la base se queda en `0.84` (no llega a 1), aunque el
superávit lo empuja por encima de 1. **Esperado: el 90 no tapa los déficits.** OK — los déficits dejan la
base incompleta; lo que sube por encima de 1 es superávit genuino, no compensación del déficit.

**Déficit de frecuencia + tiempo alto** — `F=5,T=20,D=2,[60,60]`:
compromiso `[60,60]` (`r=3,3`), sin voluntarios. `φ = (1+1)/5 = 0.400`, `S = (2+2)/5 = 0.800`,
`base = 0.400`, `R = 0.400 + 0.6·0.714·0.400·0.800 = 0.537`.
Comparar: semana `F=5` completa = `1.000`. **Esperado: el tiempo alto aporta pero NO compensa los 3 días
faltantes.** OK — `0.537` vs `1.000`: el déficit de frecuencia domina, el tiempo solo amortigua.

**40 min vs 1+1 min** — base `F=3` cumplida + extra:
A = `[30,30,30,40]` → voluntario `r=1.333→min=1`, `R = 1.0286`.
B = `[30,30,30,1,1]` → voluntarios `r=0.033×2`, `R = 1.0000`.
**Esperado: A > B.** OK — `1.0286 > 1.0000`. (Nota: como el día de 40 entra como voluntario y satura a 1,
su contribución es vía `S` del slot de compromiso desplazado; el barrido confirma A > B para todo
`λ_v ∈ [0.2,0.7]`.)

**Voluntario trivial** — `F=2,T=30,D=5,[30,30,1,1,1]`:
compromiso `[30,30]` (`r=1,1`), voluntarios `[1,1,1]` (`r=0.033×3`). `φ=1, V=0.1, base=1, S=0`,
`R = 1.0000`. **Esperado: los 2 de 30 = compromiso perfecto; los 3 de 1 min aportan casi nada.** OK — la
base ya está completa, el voluntario trivial no la mueve.

**Déficit puro de tiempo** — `F=2,T=5,D=7,[1×7]`:
compromiso `[1,1]` (`r=0.2,0.2`), voluntarios `[1,1,1,1,1]` (`r=0.2×5`). `φ = 0.4/2 = 0.200`, `V = 1.0`,
`base = 1 − 0.8·exp(−0.5) = 0.515`, `S = 0`, `R = 0.515`. **Esperado: compromiso con déficit de tiempo;
los 5 restantes aportan poco.** OK — el déficit de tiempo en el compromiso (`φ` bajo) domina; los 5
voluntarios suben la base modestamente (de `0.20` a `0.51`), sin llegar a 1. Sensible a `λ_v` (con
`λ_v=0.2` baja a `0.345`); calibrable.

**Saturación F=7 (vía P2)** — `F=7,T=30`:
- exacto `[30×7]`: `R = 1.000`.
- superávit `[45×7]` (`r=1.5`): `φ=1, S=0.5, base=1, R = 1 + 0.6·1·1·0.5 = 1.300`.
- superávit `[60×7]` (`r=2`): `R = 1.600`.
**Esperado: el superávit de tiempo solo empuja hasta Inquebrantable.** OK — con `δ_inq=0.15`, `R=1.30 ≥ 1.15`
clasifica Inquebrantable. La única vía (no hay días extra) funciona.

**Invarianza de escala (vía A10)** — `(F=3,T=30,[40,30,30])` vs `(F=3,T=120,[160,120,120])`:
ambos `R = 1.0286`. **Esperado: idéntico.** OK

### 4.3 Pruebas de tensión adicionales (chequeos del sesgo)

| Prueba | Números | Veredicto |
|--------|---------|-----------|
| Ráfaga de 1 día (180min, `r=6`) vs 3 días cumplidos (`F=3`) | `0.476` vs `1.000` | OK — constancia gana aunque la ráfaga sea 6× el target |
| Déficit de frecuencia no tapado por tiempo absurdo: `F=5,D=1,[600]` (`r=20`) vs `F=5` completo | `0.526` vs `1.000` | OK — un solo día, por extremo que sea, no llega ni a "En marcha" |
| Peso del superávit crece con F (`r=2` en todos, base completa) | `F=2:1.171 · F=3:1.257 · F=5:1.429 · F=7:1.600` | OK — monótono creciente en F |

---

## 5. Explicación en lenguaje claro

Pensá el ancla como **un esqueleto con casilleros**. Si te comprometiste a 3 días por semana, tenés 3
casilleros que llenar. Lo único que llena un casillero es **aparecer ese día y hacer al menos tu tiempo
objetivo**. Si hacés menos tiempo, el casillero queda lleno a medias. Si no aparecés, el casillero queda
vacío — y **no hay manera de llenarlo desde otro día**, por más horas que le metas. Esa es la dominancia
de frecuencia: la base de tu rendimiento es "cuántos casilleros llenaste", y eso lo decide la constancia,
no la intensidad.

Los **días extra** (más de los que te comprometiste) son un bonus que ayuda a **terminar de armar el
esqueleto** si te faltaba algo, pero con rendimientos decrecientes y sin nunca pasarte de "esqueleto
completo". Valen menos que un día comprometido, y un día extra de 1 minuto prácticamente no cuenta. Así, 40
minutos concentrados en un día le ganan a 1 minuto repartido en dos: la app premia hacer algo de verdad,
no marcar por marcar.

El **tiempo de más** (cuando ya hiciste tu objetivo y seguís) es el único camino para superar el 100% y
entrar en **Inquebrantable**. Pero — y acá está la clave — ese tiempo extra **solo cuenta si el esqueleto
ya está armado**. Si te faltan días, por más horas que acumules, casi no suma: primero armás la base, después
el extra brilla. Y ese extra **pesa más cuanto más alta es tu frecuencia objetivo**: si te comprometiste a
los 7 días, ya no podés "ganar días" (los hiciste todos), así que el único lugar donde podés destacar es el
tiempo — y ahí la fórmula te lo reconoce al máximo. Si te comprometiste a pocos días, el tiempo extra pesa
poco, porque todavía tenés la vía de sumar días.

El resultado es una app que dice, en los hechos: **"mostrate seguido y cumplí tu palabra; eso es la base.
Pasarte está bien y se nota, pero recién cuando la base está firme."** Estar bajo no es un castigo: es la
base incompleta pidiendo más presencia, no más esfuerzo de un solo día.

---

## Anexo — Tensión honesta encontrada (y resuelta)

En mi primera versión el canal de superávit `S` **no estaba ponderado por la base**. Eso producía una
patología contra el espíritu del sesgo: `F=5, D=1, t=600` (una sola ráfaga de 10 horas) daba `R = 1.83`,
**superando una semana completa** y alcanzando Inquebrantable con un solo día de los 5. Un día absurdo
ganaba a la constancia — exactamente lo que la dominancia de frecuencia debe impedir, y lo que P2 prohíbe
("superávit sobre base **completada**").

La corrección fue estructural, no un parche: multiplicar el superávit por `base^p`. Con esto, el superávit
de una base incompleta se estrangula de forma **continua** (sin gate, A9 intacto), y el mismo caso ahora da
`R = 0.53`. La propiedad "Inquebrantable exige base completa" queda **incrustada en la matemática**, no
impuesta por una regla. Documentado porque el brief pide honestidad: la dominancia de frecuencia ingenua
(solo `D/F` mandando, tiempo libre por encima) **rompe P2**; la dominancia correcta exige que el superávit
de tiempo esté **subordinado** a la frecuencia ya cumplida.

Tensión residual menor: el caso "déficit puro de tiempo" (`F=2,T=5,D=7,[1×7]`) da `R ≈ 0.51` con
`λ_v=0.5`. Es sensible a `λ_v` (baja a `0.35` con `λ_v=0.2`). No rompe ningún axioma, pero el valor exacto
de "cuánto deben valer 5 días voluntarios de tiempo trivial sobre una base a medias" es una **decisión de
calibración**, no de estructura — queda para el dataset de marcas del dueño.
