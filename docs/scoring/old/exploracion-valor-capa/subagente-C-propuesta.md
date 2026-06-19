# Subagente C — Propuesta del valor de capa: UNIFICACIÓN RELACIONAL

> **Sesgo:** *todo componente de la capa es un MIEMBRO con un peso relacional emergente.* Un
> único mecanismo de "tamaños" genera TODOS los pesos —dentro de la capa y de la capa en el
> total— con el mínimo de parámetros, todo relacional y nada fijo. La jerarquía
> anclas > soportes > tasks no se impone: EMERGE de los `K` relativos de cada miembro.
> **Estado:** propuesta autocontenida, verificada con python3 (12 casos límite, salida real abajo).

---

## 0. La idea en una frase

Una capa no es "anclas + un opt-in + soportes + tasks tratados cada uno con su fórmula". Es
**una bolsa de miembros**, cada uno con un **tamaño relacional** `K_miembro` que dice *cuántos
bloques-de-ancla vale*. El bloque de anclas es la unidad (vale `1`). El opt-in es un miembro
grande (`K_OPT` alto). Un soporte, mediano-bajo (`K_SUP`). Una task, muy chico (`K_TASK`). De
ese **único esquema de tamaños** salen, por simple división, TODOS los pesos del motor:

```
peso de un miembro dentro de su capa  =  K_miembro / Σ K_de_la_capa
tamaño de la capa                     =  Σ K_de_sus_miembros
peso de la capa en el score global    =  tamaño_capa / Σ tamaños
```

Nada de constantes de peso a dedo. La importancia `anclas > soportes > tasks` es, literalmente,
`1 > K_SUP > K_TASK`. El opt-in pesa fuerte porque `K_OPT > 1`. **Un solo parámetro por tipo de
miembro**, y los del opt-in se despejan de un axioma de estado (no se eligen).

---

## 1. Por qué un mecanismo único es superior a "una fórmula por componente"

| Tratamiento separado (lo que evito) | Unificación relacional (lo que propongo) |
|---|---|
| Cada componente con su propia escala → hay que **renormalizar** anclas (0–1.5) contra opt-in (0–1) contra soportes contra tasks. El dueño ya detectó este problema de normalización (respuesta #1). | Todos los miembros aportan en la **misma moneda**: `tamaño × señal∈[0,1]`. La señal de cada miembro se topa en 1 ANTES de pesar. No hay que renormalizar escalas distintas: la escala es siempre el tamaño. |
| El orden anclas>soportes>tasks se **declara** con constantes sueltas. | El orden **emerge** de `1 > K_SUP > K_TASK`. Cambiar el orden = cambiar un número, sin tocar fórmulas. |
| Agregar un tipo de componente nuevo = inventar otra fórmula. | Agregar un tipo nuevo = elegir su `K`. El esquema no cambia. |
| Las dos preguntas del dueño ("¿el opt-in entra al peso de la capa?" D3, "¿peso interno?" #1) se responden con dos mecanismos distintos. | **Un solo `K_OPT`** responde las dos a la vez: el peso del opt-in DENTRO de la capa es `K_OPT/(1+K_OPT)`, y el peso EXTRA de la capa en el total es `(1+K_OPT)/Σ`. Mismo número, dos efectos. Esto es lo más elegante de la propuesta. |

El precio de la elegancia (honesto): el opt-in es un miembro **especial** en un punto —solo él
y las anclas viven en el canal BASE con fuerza, y solo las anclas exportan EXTRA—. La
unificación es total para los **pesos**; el **canal** (base vs extra) sí distingue anclas del
resto, porque el axioma 2 lo exige. No lo escondo: es la única asimetría que el modelo conserva,
y es estructural (no paramétrica).

---

## 2. El modelo completo (todas las fórmulas, cada variable con su rango)

### 2.1 Caja negra del ancla (no se toca)

`R(F, T, mins) ∈ [0, 1.5]` — fórmula consolidada. `R=1` = cumplió justo; `R>1` superhabit;
`R<1` déficit. Es el único insumo "de ancla" del motor de capa.

### 2.2 Señales de los opt-in → `M ∈ [0,1]`

**Sueño** (agregación semanal de 7 noches, algunas `None` — el gap del axioma 6):

```
datos      = noches con telemetría (descarta None)
c          = |datos| / 7                      cobertura ∈ [0,1]
M_sueño    = c · promedio(datos)  +  (1−c) · B_SLEEP
```
- `B_SLEEP ∈ [0,1]` = base de "sin dato" (no tira a 0). Sin NINGÚN dato → `M = B_SLEEP`.
- Cada noche ∈[0,1] viene de `SleepScoring.kt` (4 componentes sellados, ya tope 1).

**Sobriedad** (held/broke en la ventana de 7 días; multi-track):

```
n_recaídas = nº de tracks con 'broke' DENTRO de los 7 días
M_sobr     = (1 − A_SOB) ^ n_recaídas
```
- `A_SOB ∈ [0,1]` = golpe por recaída. `0 recaídas → M=1`. **`1 recaída → (1−A_SOB)` sin
  importar cuántos tracks hay** (no se diluye: solo cuenta cuántos se rompieron, no cuántos
  existen). `2 recaídas → (1−A_SOB)² <` 1 recaída (pega más, sin premiar tener más tracks).
- Recaída FUERA de 7 días no llega como `'broke'` (el pipeline ya la materializa como held/limpia):
  no penaliza. Cumple axioma 6/7 y D7.

`A_SOB` se fija **mayor** que el golpe típico de mal sueño → cumple D8 ("recaída pega más que mala
semana de sueño"): con `A_SOB=0.5`, una recaída lleva `M_sobr=0.50`; una semana de sueño flojo
rara vez baja `M_sueño` de ~0.5–0.6 sin ser catastrófica.

### 2.3 Tamaño de cada miembro y de la capa (el corazón relacional)

```
tamaño_bloque_anclas = 1          si la capa tiene ≥1 ancla, si no 0
tamaño_optin         = K_OPT      si hay opt-in activo
tamaño_soportes      = K_SUP · (1 − e^(−n_sup))     saturación multi-soporte
tamaño_tasks         = K_TASK · (1 − e^(−n_task))   saturación multi-task

tamaño_capa = Σ de los anteriores
```
- `1 > K_SUP > K_TASK ⇒` orden de importancia emergente (axioma 7).
- El factor `(1 − e^(−n))` **satura**: 1 soporte ya aporta ~63% del tope; 8 soportes ~100%. Así
  *8 soportes no fabrican una banda* (axioma 9), y la magnitud es **relacional** (fracción de
  `K_SUP`), nunca fija.
- **El nº de anclas NO cambia el tamaño** del bloque: siempre vale 1 (axioma 5). Las anclas
  **promedian** dentro del bloque.

### 2.4 Canal BASE de la capa ∈ [0,1] (¿está en pie?)

Promedio K-ponderado de las señales de los miembros, cada señal topada en 1:

```
señal_anclas = promedio_i  min(R_i, 1)          ∈ [0,1]   (tope en 1: la base no premia superhabit)
base_capa = [ 1·señal_anclas + K_OPT·M + tamaño_sop·sop + tamaño_task·task ] / tamaño_capa
```
- `sop, task ∈ [0,1]` = fracción cuidada de soportes / fracción hecha de tasks.
- Capa **solo opt-in** (sin anclas, axioma 8): `tamaño_bloque_anclas=0 ⇒ base_capa = M`. Su valor
  ES la señal del opt-in. ✔ D4/D5.
- Peso del opt-in DENTRO de su capa solo-anclas+optin = `K_OPT/(1+K_OPT)`. **Decisión abierta
  resuelta abajo (§3.2).**

### 2.5 Canal EXTRA de la capa ≥ 0 (¿se destacó?) — SOLO anclas

```
extra_capa = promedio_i  max(R_i − 1, 0)        ∈ [0, 0.5]   (solo anclas; opt-in/sop/task NO aportan)
```

### 2.6 Agregación global

```
peso_capa  = tamaño_capa / Σ tamaños            (relacional: baja al crecer N)
base_global  = Σ_capas  peso_capa · base_capa
extra_global = Σ_{capas con ancla}  (1/n_capas_con_ancla) · extra_capa
ESTADO = EM_TOP · base_global  +  extra_global
```
- `EM_TOP = 0.85` = tope de la banda "En marcha". **Aquí vive el EJE del axioma 3:** una base
  perfecta (`base_global=1`) da `ESTADO = 0.85` = tope de En marcha, **nunca Plenitud**.
  Plenitud/Inquebrantable se cruzan **solo** sumando `extra_global > 0`. Como sueño/sobriedad no
  generan extra, **jamás suben a Plenitud** (solo mantienen en pie o bajan). ✔ axioma 3.
- El extra se promedia **solo entre capas con anclas** (no entre todas): una capa solo-opt-in no
  diluye el techo de Inquebrantable. ✔ axioma 2 / objeción 2 del dueño.

### 2.7 Bandas (sobre `ESTADO`)

```
Rojo        < 0.40·EM_TOP   (0.340)
Atención    < 0.62·EM_TOP   (0.527)
En marcha   ≤ EM_TOP        (0.850)
Plenitud    < EM_TOP+0.25   (1.100)
Inquebrantable ≥ 1.100
```
Los cortes interiores de base (0.40/0.62) son **calibrables** y se reusan en el despeje de `K`.

---

## 3. Decisiones abiertas, resueltas con el sesgo relacional

### 3.1 ¿Cómo se combina base + extra? ¿Dónde caen las bandas?

`ESTADO = EM_TOP·base + extra`. La base es un **techo** (lleva a lo sumo a En marcha); el extra
es el **único ascensor** a Plenitud/Inquebrantable. Es la lectura literal del axioma 3, y es la
combinación más simple posible (lineal). No hay `min()`, ni gates, ni caps duros: solo dos
canales sumados con un factor de escala.

### 3.2 Reparto interno del opt-in con MÁS anclas: ¿`K/(1+K)` o `K/(n+K)`?

**Mantengo `K_OPT/(1+K_OPT)`** (NO se diluye con el nº de anclas). Razón relacional pura: el
**bloque de anclas vale 1 cualquiera sea n** (axioma 5: las anclas promedian, no se cuentan). Si
diluyera con `K/(n+K)`, estaría contando anclas por la puerta de atrás y rompería el axioma 5.
El bloque es un miembro de tamaño 1; sumar anclas reparte DENTRO del bloque, no agranda el
bloque. Coherencia total con "más anclas NO cambia el peso de la capa".

### 3.3 SOPORTES: ¿base o extra? Saturación.

**Base.** Un soporte es mantenimiento de "estar en pie", no un logro que te destaca → no genera
superhabit. Entra como miembro de tamaño `K_SUP·(1−e^(−n_sup))`. La saturación
`(1−e^(−n))` garantiza magnitud **relacional** (fracción de `K_SUP`) y que multi-soporte no
fabrique banda (verificado: 1 vs 8 soportes mueve 0.004).

### 3.4 TASKS: canal y magnitud.

**Base, miembro muy chico** (`K_TASK < K_SUP`). Una task con capa aporta MENOS que un soporte
(verificado: 0.002 vs 0.007) y **no es neutra**. Una task sin capa / rol neutro → no es miembro,
`K=0`, no suma. Misma saturación que soportes.

### 3.5 Multi-sobriedad sin diluir el golpe de UNA recaída.

`M_sobr = (1−A_SOB)^n_recaídas` cuenta **recaídas, no tracks**. 1 recaída entre 1 o entre 5
tracks → mismo `M`. Tener más tracks NO sube ni baja el peso de Conducta por sí solo (no premia
"coleccionar" sobriedades). Es multiplicativo (no `min()`), respeta el axioma 1.

### 3.6 Agregación del sueño semanal (7 noches, algunas null).

`M_sueño = c·promedio(datos) + (1−c)·B_SLEEP`. La cobertura `c` mezcla lo medido con la base:
poca telemetría → el resultado tiende a `B_SLEEP` (no a 0). Sin dato alguno → exactamente
`B_SLEEP`. Cumple axioma 6.

---

## 4. Cómo emergen los pesos y cómo se despeja `K` de un axioma de estado

Los pesos NO se eligen. Salen de los tamaños. El único número con "libertad" es `K_OPT`, y se
**despeja** de un axioma de estado del dueño, igual que se calibró el ancla.

**Axioma de despeje (ejemplo):** *"anclas perfectas en todas las capas, pero NO dormí en toda la
semana (`M_sueño=0`) → el estado debe caer al tope de Atención"*. Con N capas (una con opt-in):

```
base_capa_optin = 1/(1+K)·1 + K/(1+K)·0 = 1/(1+K)
base_global = N / (N + K)                          (las otras N−1 capas en 1)
ESTADO = EM_TOP · N/(N+K) = 0.62·EM_TOP   ⇒   K = N·(1−0.62)/0.62 = N·0.6129
```

Verificado numéricamente (salida real §5): `N=3→K=1.84`, `N=4→K=2.45`, `N=5→K=3.07`, y el estado
cae EXACTO en el tope de Atención. **`K` es relacional a N** (crece con N para mantener el mismo
estado-objetivo), exactamente el comportamiento que pide el axioma 4. El dueño elige el axioma;
la matemática despeja el número.

---

## 5. Verificación numérica (python3 — salida REAL)

Script completo en `_verify_C.py` (mismo directorio). Parámetros usados (ilustrativos del ancla
+ los `K` de esta propuesta): `K_OPT=2.0, K_SUP=0.30, K_TASK=0.08, B_SLEEP=0.55, A_SOB=0.5`.

```
============================================================================================
MODELO C — UNIFICACION RELACIONAL. Parametros: K_OPT=2.00 K_SUP=0.30 K_TASK=0.08 B_SLEEP=0.55 A_SOB=0.50
============================================================================================
Caso                                      base   extra   estado  banda           esperado
--------------------------------------------------------------------------------------------
1. Todo justo (R=1, opt-ins ok)         1.0000  0.0000   0.8500  En marcha       EN MARCHA
2. Superhabit repartido                 1.0000  0.3161   1.1661  Inquebrantable  Plenitud/Inq
3. Capa solo opt-in (D4)                0.9600  0.0000   0.8160  En marcha       base=senal, extra solo anclas
4. Apreton 3 capas + 2 opt-ins          0.9714  0.0000   0.8257  En marcha       pesos suman 1.00
5. Mal sueno: hunde base, no extra      0.6800  0.2107   0.7887  En marcha       extra sobrevive
6. Sin dato sueno (M=0.55, no 0)        0.8200  0.0000   0.6970  En marcha       M=0.55
7a. Recaida DENTRO 7d                   0.7500  0.0000   0.6375  En marcha       penaliza
7b. Recaida FUERA 7d (held)             1.0000  0.0000   0.8500  En marcha       no penaliza
9a. Soportes full (3, cuidados)         0.8665  0.0000   0.7365  En marcha       light +
9b. Soportes descuidados (3, 0)         0.7797  0.0000   0.6628  En marcha       light -
10. Task full (1)                       0.8562  0.0000   0.7278  En marcha       < soporte
--------------------------------------------------------------------------------------------

VERIFICACIONES PUNTUALES:
  C3 (solo opt-in): pesos=[0.2, 0.2, 0.2, 0.4]  capa Cuerpo(solo sueno)=0.4000; extra_global=0.0000 (no exporta extra)
  C4 (apreton): suma de pesos = 1.000000 (debe ser 1.0)
  C5 (mal sueno): extra_global=0.2107 (>0, sobrevive el superhabit)
  C7 (recaida): DENTRO estado=0.6375 (En marcha) vs FUERA estado=0.8500 (En marcha)  -> dentro < fuera: True
  C8 (multi-sobr): M(1 recaida de 1)=0.5000  M(1 recaida de 3)=0.5000  -> ~igual: True
  C8b: M(2 recaidas de 3)=0.2500 (>1 recaida pega mas, no se premia tener tracks)
  C9 (soportes): full estado=0.7365 vs descuidado estado=0.6628  delta=0.0738 (light, mueve bordes)
  C9 saturacion: 1 soporte full estado=0.7331  vs 8 soportes full estado=0.7370  delta=0.0039 (8 no fabrican banda)
  C10 (tasks vs soporte, 1 unidad full sobre mismo baseline 0.7257):
       aporte task=0.00206   aporte soporte=0.00739   -> task < soporte: True
  C11 (mas anclas): peso capa con 1 ancla=0.3333  con 3 anclas=0.3333  -> igual: True
  C12 (N crece): peso opt-in  N=3:0.6000  N=4:0.5000  N=5:0.4286  -> baja: True

DESPEJE DE K_OPT desde axioma de estado ('anclas perfectas + M_sueno=0 -> Atencion'):
  N=3: K_OPT=1.839 -> estado=0.5270 banda=En marcha (objetivo: tope de Atencion 0.5270)
  N=4: K_OPT=2.452 -> estado=0.5270 banda=En marcha (objetivo: tope de Atencion 0.5270)
  N=5: K_OPT=3.065 -> estado=0.5270 banda=En marcha (objetivo: tope de Atencion 0.5270)
```

### Lectura de los 12 casos

| # | Caso | Resultado | Veredicto |
|---|------|-----------|-----------|
| 1 | Todo justo (R=1, opt-ins ok) | base=1.0 → estado **0.85 En marcha** | ✔ EJE: base sola NO pasa a Plenitud |
| 2 | Superhabit repartido | extra=0.32 → **1.17 Inquebrantable** | ✔ solo el extra cruza arriba |
| 3 | Capa solo opt-in (D4) | base=M, peso 0.40, **extra=0** | ✔ valor=señal, no exporta extra |
| 4 | Apretón 3 capas + 2 opt-ins | **Σ pesos = 1.000000** | ✔ relacional, normaliza solo |
| 5 | Mal sueño | base baja (0.68) pero **extra=0.21 sobrevive** | ✔ el mérito del ancla no se borra |
| 6 | Sin dato de sueño | **M=0.55** (no 0) | ✔ base, no piso |
| 7 | Recaída dentro vs fuera | dentro 0.64 **<** fuera 0.85 | ✔ ventana 7d |
| 8 | Multi-sobriedad | 1 de 1 = 1 de 3 = 0.50; 2 de 3 = 0.25 | ✔ no se diluye, no premia tracks |
| 9 | Soportes full vs mal | Δ=0.074 light; 1 vs 8 → Δ=0.004 | ✔ mueve bordes, satura |
| 10 | Task vs soporte | task 0.002 **<** soporte 0.007 | ✔ aporta menos, no neutra |
| 11 | Más anclas | peso 0.333 con 1 ancla **=** con 3 | ✔ nº anclas no cambia peso |
| 12 | N crece | opt-in 0.60 → 0.50 → 0.43 | ✔ baja al crecer N (relacional) |

**Los 12 pasan.** Tabla de despeje: `K` emergente de un axioma, exacto sobre el corte de banda.

---

## 6. Parámetros calibrables (rangos plausibles)

| Param | Rol | Rango | De qué se despeja / cómo se fija |
|-------|-----|-------|--------------------------------|
| `K_OPT` | tamaño del opt-in como miembro | `1.5 – 3.5` | **Se DESPEJA** de un axioma de estado ("anclas perfectas + opt-in en 0 → banda X"). Relacional a N. |
| `K_SUP` | tamaño de un soporte | `0.20 – 0.40` | De un axioma "soportes full vs descuidados mueve ~media banda en el borde". `< 1`. |
| `K_TASK` | tamaño de una task con capa | `0.05 – 0.12` | De "una task aporta menos que un soporte". `K_TASK < K_SUP`. |
| `B_SLEEP` | base de sueño sin dato | `0.45 – 0.65` | "Sin telemetría no te hundo, te dejo en marcha media". |
| `A_SOB` | golpe por recaída | `0.4 – 0.7` | De D8: `A_SOB` > caída típica de mal sueño → recaída pega más. |
| `EM_TOP` | tope de En marcha (escala base→estado) | `0.80 – 0.88` | Define dónde "base perfecta" toca el techo de En marcha (eje axioma 3). |
| cortes banda | Rojo/Atención (·EM_TOP) | `0.35–0.45 / 0.58–0.66` | Calibrables contra el dataset de marcas; entran al despeje de `K`. |

Todo se calibra contra el dataset de decisiones del dueño, igual que el ancla. **Ningún peso es
fijo**: los pesos son siempre cocientes de tamaños.

---

## 7. En criollo (sin fórmulas)

Pensá tu capa como una **mesa con varios objetos encima**, cada uno con su peso. El bloque de
tus anclas es el objeto de referencia: pesa "uno", no importa si tenés una o cinco anclas (se
promedian entre ellas). El sueño o la sobriedad, si los activaste, son un objeto **pesado** —pesan
más que el bloque de anclas, porque para vos ese tema es sensible (insomnio, alcohol, lo que sea)—.
Un soporte (tomar agua, comer bien) es un objeto **chico**. Una pendiente con capa, un objeto
**muy chico**. Y de cuánto pesa cada objeto sale TODO: cuánto cuenta cada cosa dentro de la capa,
y cuánto cuenta la capa entera en tu día. No hay números mágicos puestos a mano: es todo "este
pesa el doble que aquel".

El motor te mira con **dos lentes**:

- **¿Estás en pie?** (la base) — junta todos los objetos de la mesa: anclas hasta tu meta, sueño,
  sobriedad, soportes, pendientes. Acá el sueño y la sobriedad pesan fuerte: si dormís mal o
  tenés una recaída, esta base baja. Pero esta base, por más perfecta que esté, te lleva **como
  mucho a "En marcha"**. Cumplir todo justo es estar en marcha, que es tu hogar operativo, no la
  gloria.
- **¿Te destacaste?** (el extra) — esto **solo** lo dan tus anclas cuando te pasás de tu meta.
  Dormir de más o cumplir tu sobriedad no te "destaca": te mantiene en pie. Solo el esfuerzo
  voluntario en tus prácticas te empuja a **Plenitud** o **Inquebrantable**.

Lo lindo de separar las dos lentes: una mala semana de sueño te baja "el estar en pie", pero
**no te borra el mérito** que ganaste esforzándote en tus anclas. El esfuerzo sobrevive. Y una
recaída pega más fuerte que dormir mal (es lo que pediste), pero solo si pasó esta semana: lo
viejo queda en su panel aparte, no te castiga para siempre. Si activás un tema que no tiene
anclas (solo querés trackear el sueño), esa capa vale exactamente lo bien que dormiste, y no
arruina tu chance de llegar a lo más alto, porque a la gloria solo se sube por las anclas.

---

## 8. Tensiones honestas

1. **La asimetría base/extra no es 100% unificada.** El opt-in y los soportes/tasks viven solo en
   la base; las anclas además exportan extra. Es la única asimetría que queda, y es estructural
   (la pide el axioma 2), no un parche. La unificación es total para los **pesos**; el **canal**
   distingue anclas porque debe.
2. **`EM_TOP` como escala.** Multiplicar la base por 0.85 para que tope en En marcha es simple
   pero "aplana" la base: una base de 0.62 da 0.527, no 0.62. Es correcto para el eje, pero el
   dueño debe ver que el número de "estado" no es la base cruda — son escalas distintas a propósito.
3. **`A_SOB` y `B_SLEEP` compiten por D8.** Que "recaída pegue más que mal sueño" depende de que
   `A_SOB` sea suficientemente grande Y `B_SLEEP` no demasiado bajo. Si se calibra `B_SLEEP` muy
   bajo, un sueño sin dato podría pegar parecido a una recaída. Hay que calibrarlos **juntos**.
4. **Extra promediado entre capas-con-ancla.** Uso promedio (no suma) para que el techo de
   Inquebrantable sea estable con N. Alternativa (suma) premiaría tener más capas con anclas; lo
   descarté por coherencia con "más no es mejor", pero es una perilla que el dueño puede invertir.
