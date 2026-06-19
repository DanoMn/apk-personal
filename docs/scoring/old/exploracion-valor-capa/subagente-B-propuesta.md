# Sub-agente B — Propuesta de valor de capa: **CIMIENTO MODULADOR SUAVE**

> Propuesta autocontenida, verificada con `python3`. Diseñada a ciegas (no vi a los otros 2 proponentes).
> Sesgo asignado: el opt-in actúa como un **cimiento** que **modula no-linealmente** el valor "en pie"
> de su capa, **con un suelo** que evita la brutalidad del multiplicativo puro `M·R` (rechazado por el
> dueño). La modulación toca **solo** el canal "en pie", **nunca** el extra de las anclas.

---

## 0. Resumen en una frase

El score son **dos canales**: una **BASE** ("¿está en pie?", tope = En marcha) y un **EXTRA**
("¿se destacó?", lo único que sube a Plenitud/Inquebrantable). El opt-in (sueño/sobriedad) no resta
puntos sueltos: **modula el cimiento** de su capa con una curva no-lineal que tiene un **suelo** —
mal sueño *degrada* la base de esa capa (efecto dominó), pero el suelo impide que la *aniquile*, y el
extra ganado con esfuerzo en las anclas **queda intacto**.

---

## 1. El modelo completo (todas las fórmulas, cada variable nombrada y su rango)

### 1.1 Caja negra del ancla (ya consolidada — NO la rediscuto)

`R(F, T, mins) ∈ [0, 1.5]`. `R=1` = cumplió justo; `R>1` = superhabit; `R<1` = déficit.
Es la única fuente de "rendimiento de un ancla". El modelo de capa la consume como dato.

### 1.2 Señales del opt-in semanal `M ∈ [0, 1]`

**Sueño (señal continua, con cobertura — axioma 6):**

```
noches = [s_1 … s_7],  s_i ∈ [0,1]  o  None (NoData, ADR-3)
c      = (#noches con dato) / 7            cobertura ∈ [0,1]
avg    = promedio de las noches con dato   ∈ [0,1]
M_sueño = c · avg + (1 − c) · sleep_base   si hay ≥1 dato
M_sueño = sleep_base                       si NO hay ningún dato
```

- `sleep_base ∈ [0,1]` (calibrable, ≈0.5): la **BASE** del axioma 6 — sin dato no se tira a 0,
  se asume un nivel neutro. A más cobertura, más manda el dato real; con cobertura total, `M = avg`.
- Por diseño del código, `s_i ≤ 1` (dormir de más = 1.0 neutral): el sueño **nunca infla**, solo
  sostiene o baja. Coherente con el eje (sueño no da Plenitud).

**Sobriedad (binaria held/broke, multiplicativa — axiomas 6 y 8):**

```
tracks = [held_1 … held_n],  held_j ∈ {True (mantenido en 7d), False (recaída en 7d)}
M_sobr = Π_j  ( 1            si held_j
                sobr_broke   si broke_j )      con sobr_broke ∈ [0,1] calibrable (≈0)
```

- **Multiplicativo, no promedio** (axioma 8): 1 recaída entre 1 o entre N tracks hunde **igual**
  (no se diluye por tener más tracks); 2 recaídas hunden más. Sin `min()`, sin worst-term — la
  no-compensabilidad **emerge** del producto.
- La racha larga **no entra** (axioma 6): `held/broke` es solo de la ventana de 7 días. Una recaída
  vieja ya viene materializada como `held` esta semana → no penaliza (axioma 7).

### 1.3 El cimiento modulador suave (**el corazón del sesgo B**)

```
g(M)       = M^q                              curvatura no-lineal, q ≥ 1
cimiento(M) = floor + (1 − floor) · g(M)      ∈ [floor, 1]
```

- `floor ∈ [0,1]` (≈0.55): el **SUELO**. Con `M=0`, el cimiento no baja de `floor` → evita la brutalidad.
- `q > 1`: la curva es **casi plana arriba** (M≈1 → cimiento≈1: dormir un poco peor casi no toca la
  base) y **se acelera abajo** (M↓ → caída cada vez más fuerte). Esa asimetría **es** el efecto dominó:
  el cimiento sano no cobra peaje; el cimiento que se desmorona arrastra cada vez más.
- **Sólo multiplica el "en pie" de la capa, jamás el extra** (ver §1.5).

### 1.4 Soportes y tasks (a la BASE, saturantes, relacionales — axioma 7)

```
SOPORTES (UX inversa: frac_i ∈ [0,1] = fracción de la semana SIN fallar el soporte i)
  avg_s   = promedio de frac_i
  signed  = 2·avg_s − 1                        ∈ [−1, 1]   (centrado: full=+1, descuidado=−1)
  sup_term = sign(signed) · sup_sat · (1 − exp(−sup_k · |signed|))   ∈ [−sup_sat, +sup_sat]

TASKS (con capa: solo suman, < soporte)
  avg_t    = fracción de tasks completadas    ∈ [0,1]
  task_term = task_sat · (1 − exp(−task_k · avg_t))   ∈ [0, task_sat]
```

- `sup_sat ∈ [0,~0.12]`, `task_sat < sup_sat` (≈0.04): magnitudes **chicas y relacionales** — mueven
  en bordes de banda, no fabrican un estado. La **saturación exponencial** garantiza que 8 soportes no
  pesen como 8 (axioma 9): `sup_term(3 soportes) ≈ sup_term(8 soportes)`.
- Tasks **siempre < soportes** por `task_sat < sup_sat` (axioma 10). Una task sin capa / rol neutral
  no entra (no se le pasa a `tasks`).

### 1.5 Valor de capa — dos canales

```
EN PIE (capa) ∈ [0, ~1]:
  capa CON anclas:
      cumpl = (1/n) · Σ_i min(R_i, 1)          bloque de anclas, topado en 1, PROMEDIA (axioma 5)
      cumpl = clamp( cumpl + sup_term + task_term , 0, 1 )
      en_pie = cimiento(M) · cumpl             ← el opt-in MODULA SOLO el "en pie"
                                                 (sin opt-in: en_pie = cumpl, cimiento≡1)
  capa SIN anclas (solo opt-in, D4):
      en_pie = M                               la señal ES la capa (axioma 8)

EXTRA (capa) ≥ 0:
  extra = (1/n) · Σ_i max(R_i − 1, 0)          SOLO anclas; opt-in NO aporta (axioma 2/3)
  capa sin anclas: extra = 0                   (no exporta extra — axioma 8, caso 3)
```

### 1.6 Pesos relacionales (un solo `K` por opt-in — axioma 4)

```
tamaño(capa) = 1 (bloque de anclas)  +  K   si la capa tiene opt-in
             = 1                            si no
peso(capa)   = tamaño(capa) / Σ tamaños
```

- **No depende del nº de anclas** (axioma 5): siempre `1` por el bloque. Las capas sin opt-in pesan
  **igual** entre sí.
- **Relacional a N** (axioma 12): al crecer N el peso de cada capa (y el del opt-in) baja solo.
- Peso del opt-in **dentro** de su capa = `K/(1+K)`. **Decisión abierta resuelta**: se **mantiene**
  `K/(1+K)`, NO se diluye con la cantidad de anclas (`K/(n+K)`) — porque las anclas **promedian a un
  bloque** de tamaño 1 (axioma 5). Diluir con `n` rompería esa regla.

### 1.7 Agregación global → estado → banda

```
base_g  = Σ_capas  peso(capa) · en_pie(capa)                              ∈ [0, ~1]

raw_extra = [ Σ_{capas con ancla} peso · extra(capa) ] / [ Σ_{capas con ancla} peso ]
            (promedio PONDERADO del extra, SOLO entre capas que pueden generarlo)
extra_g = extra_sat · (1 − exp(− raw_extra / extra_s0))                   ∈ [0, extra_sat]

ESTADO  = b_marcha · base_g  +  lam_state · extra_g
```

**La pieza clave del eje (axioma 3):** la base se mapea a `[0, b_marcha]`. Es decir, **base llena = el
techo exacto de En marcha** (`b_marcha`, ≈0.85). La base **sola jamás cruza a Plenitud**. Sólo el
EXTRA (`lam_state · extra_g`) empuja por encima → Plenitud/Inquebrantable. Como sueño/sobriedad no
generan extra, **nunca** suben a Plenitud: solo sostienen la base en pie o, si están mal, la bajan.

```
BANDAS sobre ESTADO:
  Rojo            x <  b_rojo        (0.40)
  Atención        x <  b_amar        (0.62)
  En marcha       x <= b_marcha      (0.85)   ← techo INCLUSIVE: "todo justo" vive acá
  Plenitud        x <  b_pleno       (1.10)
  Inquebrantable  x >= b_pleno       (1.10)
```

---

## 2. Justificación de cada decisión abierta (con el sesgo de cimiento modulador suave)

**base + extra en el estado final / dónde caen las bandas.**
La base se *reescala* a `[0, b_marcha]` para que "todo justo" caiga **exacto** en el techo de En marcha
(eje axioma 3). El extra es un **sumando saturado** por encima. No hay gate ni `min()`: la imposibilidad
de llegar a Plenitud solo con base es **estructural** (la base nunca supera `b_marcha`). Esto es la
versión "dos canales" de la misma idea del ancla (`base^p` subordina el superhabit): acá el techo del
canal base hace el trabajo.

**Por qué la no-linealidad (cimiento) captura el efecto dominó mejor que un término aditivo.**
Un término aditivo (`en_pie = cumpl − penal(M)`) penaliza **lo mismo** caiga el sueño de 1.0 a 0.8 que
de 0.4 a 0.2 — es una resta plana, no modela que "cuando el cimiento se desmorona, todo lo de arriba se
vuelve frágil". La forma `cimiento(M) = floor + (1−floor)·M^q` es **multiplicativa sobre el cumplimiento**
y con `q>1` su pendiente **crece** a medida que M baja: un cimiento sano (M≈1) casi no cobra, un cimiento
en ruina (M→0) arrastra aceleradamente. Eso **es** el dominó. Y el **suelo `floor`** es exactamente lo
que faltaba en el `M·R` puro que el dueño rechazó: en el multiplicativo puro, M=0.6 borra el 40% del
esfuerzo del ancla; con el cimiento suave M=0.6 solo baja a ~0.82 (ver demo §4). La asimetría
"degrada pero no aniquila" sale de **dos** perillas: `q` (curvatura del dominó) y `floor` (cuánto piso
queda cuando el cimiento está roto).

**El extra queda intacto.** `cimiento(M)` multiplica **sólo** `cumpl` (el "en pie"). `extra(capa)` se
calcula aparte de los `R_i` crudos. Mal sueño baja la base de su capa, pero el superhabit ganado en las
anclas (de esa capa o de otras) **no se toca** (caso 5). Eso resuelve la objeción #1 del dueño al P-A.

**Reparto interno del opt-in con más anclas → se mantiene `K/(1+K)`** (no `K/(n+K)`). Justificación:
axioma 5 dice que las anclas **promedian a un bloque** de tamaño 1; meter `n` en el denominador haría
que el opt-in pesara menos por tener más anclas, contradiciendo "el nº de anclas no cambia el peso".

**Soportes → BASE, saturados, relacionales.** Son "mantenimiento": pertenecen a "¿está en pie?", no a
"¿se destacó?". Centrados en 0.5 (full = +, descuidado = −) para que muevan **en ambos sentidos** pero
*light*. La saturación exponencial impide que multi-soporte fabrique una banda (axioma 9).

**Tasks → BASE, magnitud < soporte, saturada, solo suman.** Por dominio (axioma 7) una task con capa
aporta menos que un soporte (`task_sat < sup_sat`) y no penaliza su ausencia (es puntual, no
mantenimiento). Las neutras/sin capa no entran.

**Multi-sobriedad → producto.** El producto hace que 1 recaída pegue igual con 1 o N tracks (axioma 8),
sin premiar tener más tracks ni diluir el golpe. Tener más tracks **no sube** el peso de la capa (el
opt-in de Conducta es uno solo, con su `K`); lo que cambia es la **señal** `M_sobr`.

**Sueño semanal → cobertura con base.** `M = c·avg + (1−c)·base`: a poco dato, la base domina y evita
el piso (axioma 6); a dato completo, manda el promedio real. Emergente, sin tope artificial.

---

## 3. Cómo emergen los pesos + parámetros calibrables y cómo se despejan

Los pesos **emergen** de `tamaño = 1 + K·[tiene opt-in]` normalizado. No hay constante de peso fija:
todo es relacional a N y a K (ver §1.6 y caso 12). El único grado de libertad de peso es **`K`, uno por
opt-in**, y se **despeja de un axioma de estado del dueño** (no se elige a dedo):

> **Axioma de calibración (ejemplo, a confirmar por el dueño):** "Con N=4 capas, todas las anclas
> cumplidas **justo** (R=1, sin superhabit ⇒ extra=0) pero el **opt-in de una capa en el piso** (M=0,
> p.ej. no dormí nada / recaí), el estado debe **caer a Atención**."

Sin extra, `ESTADO = b_marcha · base_g`. Con el opt-in caído, `base_g = w_opt·floor + (1−w_opt)·1` con
`w_opt = (1+K)/(N+K)`. Se iguala el estado al **centro de la banda Atención** y se resuelve `K` por
bisección. Con `floor=0.55`, bandas por defecto y N=4 → **`K ≈ 0.29`** (peso del opt-in dentro de su
capa `K/(1+K) ≈ 0.22`). Si el dueño quiere que el golpe sea más fuerte (caer al **borde bajo** de
Atención, o a Rojo), `K` sube; mismo método que la calibración del ancla.

---

## 4. Verificación numérica en python3 (script + salida REAL)

Script completo: [`_verif_B.py`](./_verif_B.py) (en esta misma carpeta). Corrido con `python3`.
Parámetros usados: `floor=0.55, q=1.6, sup_sat=0.10, sup_k=2.5, task_sat=0.04, task_k=2.0,
extra_sat=0.6, extra_s0=0.5, lam_state=0.85, sleep_base=0.5, sobr_broke=0.0`, bandas
`0.40/0.62/0.85/1.10`. **`K` se despeja del axioma** (no se fija): salió `K=0.2897`.

### 4.1 Salida real — los 12 casos límite

```
====================================================================================================
K despejado del axioma de estado (N=4, opt-in caido => borde Atencion): K = 0.2897
  => peso opt-in dentro de capa = K/(1+K) = 0.2246
  => floor del cimiento = 0.55, q = 1.6
====================================================================================================

CASO                              col1                  col2                      col3                    banda                 esperado
------------------------------------------------------------------------------------------------------------------------------------------------------
1. Todo justo (R=1, opt-in bien)  1.0000                0.0000                    0.8500                  En marcha             EN MARCHA
2. Superhabit repartido           1.0000                0.3473                    1.1452                  Inquebrantable        Plenitud/Inq
3. Capa solo-opt-in (D4)          0.9399                0.0000 (capaD4 extra=0.00)0.7989                  En marcha             no exporta extra
4. Apreton 3 capas + 2 opt-ins    0.9748                Σw=1.0000                 0.8286                  En marcha             Σpesos=1
5. Mal sueño: extra intacto       base 0.973->0.828     extra 0.3473==0.3473      1.122->0.999            Inquebrantable->Plenitud  extra NO cae
6. Sueño sin dato => base         M_nodato=0.500        M_parcial=0.586           -                       -                     base=0.5
7. Recaida in/out 7d              M_in=0.000            M_out=1.000               -                       -                     in baja, out no
8. Multi-sobriedad 1/1 vs 1/3     1/1=0.000             1/3=0.000                 2/3=0.000               -                     no se diluye
9. Soportes full/desc/8x          full 0.850            desc 0.824                8sup term 0.092 vs 3sup 0.092  -              light+satura
10. Tasks < soportes              task_term=0.0346      sup_term=0.0918           task<sup: True          -                     task aporta menos
11. +anclas no cambia peso        w(1ancla)=0.3333      w(3anclas)=0.3333         iguales: True           -                     peso estable
12. Peso opt-in vs N              N=3:0.3920            N=4:0.3007                N=5:0.2438              baja: True            relacional
```

### 4.2 Lectura caso por caso (las 3 columnas son: base_g / extra_g / estado salvo nota)

| # | Caso | Resultado | Veredicto |
|---|------|-----------|-----------|
| 1 | Todo justo (R=1, opt-in bien) | base=1.00, extra=0, estado=**0.8500 → En marcha** | ✅ eje: base sola tope En marcha, **no** Plenitud |
| 2 | Superhabit repartido (4 capas con anclas a 2× tiempo, 7d) | base=1.00, extra=0.347, estado=**1.145 → Inquebrantable** | ✅ el extra sube a Inq |
| 3 | Capa solo-opt-in (D4, M=0.8, sin anclas) | esa capa extra=0.00, estado **En marcha** | ✅ participa en base, **no exporta extra** |
| 4 | El apretón (3 capas + ambos opt-ins) | **Σpesos=1.0000**, estado En marcha | ✅ pesos relacionales suman 1 |
| 5 | Mal sueño en una capa (otras con superhabit) | base **0.973→0.828**, extra **0.347==0.347** (intacto) | ✅ hunde el en-pie de su capa, **no** el extra |
| 6 | Sueño sin dato | M=**0.500** (base, no 0); parcial=0.586 | ✅ base, no piso |
| 7 | Recaída in/out 7d | M_in=**0.000** (baja), M_out=**1.000** (no penaliza) | ✅ |
| 8 | Multi-sobriedad | 1/1=**0.000** == 1/3=**0.000**; 2/3 también 0 (broke=0) | ✅ 1 recaída no se diluye con más tracks |
| 9 | Soportes full/desc/8× | full 0.850 vs desc 0.824 (mueve **light**); term(8)=term(3)=0.092 | ✅ satura, no fabrica banda |
| 10 | Tasks vs soportes | task_term=0.0346 < sup_term=0.0918 | ✅ task aporta menos |
| 11 | +anclas misma capa | w(1)=0.3333 == w(3)=0.3333 | ✅ peso estable |
| 12 | Peso opt-in vs N | 0.392 (N=3) > 0.301 (N=4) > 0.244 (N=5) | ✅ relacional, baja con N |

> Nota caso 8: con `sobr_broke=0` cualquier recaída hunde la señal a 0 (golpe máximo, D8 "recaída pega
> fuerte"). Si el dueño quiere graduar 1 día vs 3 días de recaída dentro de la ventana, `sobr_broke` se
> sube por encima de 0 y/o se hace `M_sobr = Π broke^(días_recaída_j)` — perilla calibrable, no estructural.

### 4.3 Salida real — demo del sesgo B (suelo vs `M·R` puro rechazado)

```
====================================================================================================
DEMO sesgo B — por que el SUELO evita la brutalidad del M*R puro (rechazado por el dueño)
====================================================================================================
M (sueño)   M*R puro      cimiento(M)   cimiento*cumpl  delta a favor del esfuerzo
1.00        1.0000        1.0000        1.0000          +0.0000
0.80        0.8000        0.8649        0.8649          +0.0649
0.60        0.6000        0.7487        0.7487          +0.1487
0.40        0.4000        0.6539        0.6539          +0.2539
0.20        0.2000        0.5843        0.5843          +0.3843
0.00        0.0000        0.5500        0.5500          +0.5500
```

Con sueño **regular** (M=0.6) el multiplicativo puro borra el 40% del esfuerzo (0.60); el cimiento suave
baja solo a **0.75**. El efecto dominó aparece recién cuando M se **desploma** (M=0.2 → 0.58; M=0 →
0.55 = el suelo). **Degrada, no aniquila** — exactamente la objeción del dueño, resuelta.

---

## 5. Tabla de parámetros calibrables (rangos plausibles)

| Param | Rol | Rango | Cómo se fija |
|-------|-----|-------|--------------|
| `K` (uno por opt-in) | peso de la capa con opt-in (relacional) | `0.2 – 3` | **se DESPEJA** de un axioma de estado del dueño (§3) |
| `floor` | suelo del cimiento (cuánto queda con M=0) | `0.45 – 0.65` | axioma "opt-in en piso pero anclas perfectas → tal estado" |
| `q` | curvatura del dominó (>1: degrada acelerando) | `1.3 – 2.2` | marcas: cuánto debe doler M=0.6 vs M=0.3 |
| `sup_sat` | techo del aporte/penal de soportes | `0.05 – 0.12` | bordes de banda (light) |
| `sup_k` | velocidad de saturación multi-soporte | `2 – 4` | que 8 ≈ 3 soportes |
| `task_sat` | techo del aporte de tasks (`< sup_sat`) | `0.02 – 0.05` | task < soporte (axioma 10) |
| `task_k` | saturación multi-task | `1.5 – 3` | — |
| `extra_sat` | techo del extra agregado al estado | `0.4 – 0.7` | que Inquebrantable sea alcanzable pero exigente |
| `extra_s0` | escala de saturación del extra | `0.3 – 0.8` | velocidad de subida a Plenitud/Inq |
| `lam_state` | cuánto del extra entra al estado | `0.7 – 1.0` | distancia En-marcha→Inquebrantable |
| `sleep_base` | base de sueño sin dato (axioma 6) | `0.4 – 0.6` | neutralidad ante NoData |
| `sobr_broke` | señal de un track recaído | `0.0 – 0.3` | D8 (recaída pega fuerte); >0 si se gradúa por días |
| `b_rojo/b_amar/b_marcha/b_pleno` | umbrales de banda sobre el estado | dados | contrato de bandas |

---

## 6. Explicación en criollo (sin fórmulas)

Pensá tu vida como una mesa con 3 a 5 patas (las capas). El score mira dos cosas distintas, separadas a
propósito:

**¿Está todo en pie?** Cada pata suma según cuánto cumpliste tus anclas (topado: cumplir la meta ya es
"en pie", pasarte no cuenta acá). Los soportes y las tareas mueven esto **poquito** — son detalles de
mantenimiento, no la viga maestra. Si tenés sueño o sobriedad activados, esos actúan como el **cimiento**
de su pata: cuando el cimiento está sano, ni se nota; cuando empieza a fallar, la pata se vuelve cada vez
más frágil — pero **nunca se cae del todo**, porque hay un piso. Dormir un poco peor no te borra el
esfuerzo (eso era lo que molestaba del modelo viejo); dormir **muy** mal sí te baja, y bastante, pero
de forma proporcional, no como una guillotina.

**¿Te destacaste?** Esto es el premio aparte: solo cuenta lo que hiciste **de más** en tus anclas (más
días, más tiempo del que prometiste). El sueño y la sobriedad **no** dan premio acá — solo te mantienen
en pie. Por eso, hagas lo que hagas, si cumplís **justo** todo, tu lugar natural es **"En marcha"**: el
hogar operativo, no la cima. Para llegar a **Plenitud** o **Inquebrantable** tenés que haberte
**destacado** en tus prácticas, repartido — no clavando un día heroico y abandonando el resto.

**Los pesos se acomodan solos.** Una pata con cimiento (sueño/sobriedad) pesa más, porque es un área
**sensible** para vos. Pero cuantas más patas tengas, menos pesa cada una — todo es relativo, nada está
clavado. Y agregar más anclas a una pata **no** la hace pesar más: las anclas se promedian, la pata sigue
siendo una pata. Si tenés tres adicciones que estás cuidando, una recaída pega **igual de fuerte** que si
tuvieras una sola — no te "diluís" el golpe por trackear más cosas.

---

## 7. Tensiones honestas de esta propuesta

1. **El `floor` es una decisión moral, no matemática.** ¿Cuánto piso merece una capa cuyo cimiento está
   en ruina (no dormiste nada toda la semana)? Lo puse en 0.55 (la capa conserva poco más de la mitad).
   Si el dueño cree que "no dormir nada" debe doler más, baja `floor` — pero entonces se acerca a la
   brutalidad que él mismo rechazó. Es el péndulo central a calibrar.
2. **`q` y `floor` interactúan.** Ambos controlan "cuánto duele M bajo". Conviene fijar uno por axioma
   (p.ej. `floor` desde "M=0 → tal estado") y el otro desde un segundo punto (p.ej. "M=0.5 → tal otro"),
   para no tener dos perillas peleando por lo mismo.
3. **El extra usa promedio ponderado entre capas-con-ancla, luego satura.** Es "cobertura" (destacarse
   en una sola capa rinde menos que repartido). Alternativa razonable: **suma** en vez de promedio
   (acumulación). Elegí promedio para que Inquebrantable exija reparto, coherente con el ancla; queda
   como decisión marcable.
4. **Caso 5 termina en Plenitud, no En marcha, pese al mal sueño.** Es correcto por diseño: el extra
   ganado (superhabit real en 4 capas) **sostiene** el estado aunque una capa pierda su cimiento. El
   sueño malo bajó base de 0.973 a 0.828 y el estado de Inquebrantable a Plenitud — lo movió, pero no
   borró el mérito. Si el dueño quisiera que el mal sueño tenga *veto* sobre el extra, eso sería un gate
   (prohibido por axioma 1) — no lo hago.
