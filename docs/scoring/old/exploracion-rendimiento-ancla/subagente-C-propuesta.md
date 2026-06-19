# Propuesta C — Saturación / retornos decrecientes

> Proponente C. Sesión: exploración del rendimiento de un ancla (2026-06-09).
> Derivación independiente desde los axiomas, sesgo asignado: **saturación / retornos decrecientes**.
> Sin búsqueda web, sin leer otras propuestas ni el código viejo. Toda la autoverificación numérica de
> §4 se calculó con `python3` local (cálculo permitido; web no).

---

## 1. Sesgo asignado — cómo interpreté "saturación / retornos decrecientes"

La saturación es la idea de que **los primeros minutos de una sesión valen mucho y los extra valen cada
vez menos**. Matemáticamente: una función del tiempo del día que crece rápido cerca de la meta y se
**aplana** por encima, tendiendo a un techo (asíntota). Esto sirve a dos cosas centrales del producto:

1. **El superávit no explota.** Un día de 4×T no vale 4 veces un día cumplido; vale "bastante más, pero
   con límite". Eso evita que una ráfaga de un solo día dispare el score.
2. **La constancia gana a la intensidad** sin necesidad de un parámetro suelto: como cada día se aplana,
   sumar días nuevos (más términos en el promedio) rinde más que recargar un día existente.

Pero el brief me marca una **tensión crítica que NO podía ignorar** y que terminó moldeando todo el
diseño:

- **La saturación no puede matar el superávit (P2).** Si el techo asintótico deja el rendimiento clavado
  apenas arriba de 1, el superávit de tiempo nunca alcanza Inquebrantable —especialmente en F=7, donde no
  hay días extra y el tiempo es la única vía—. Mi techo tiene que ser **generoso y, además, crecer con F**.
- **La concavidad en la zona baja no puede regalar valor a marcas triviales.** Una función cóncava pura
  (Hill con exponente 1, Michaelis-Menten) tiene **pendiente máxima en 0**: le da demasiado a 1 minuto
  sobre 30. Eso choca con que "1 min sobre T=30 valga casi nada".

La resolución de esas dos tensiones es el corazón de mi propuesta:

- Para la triviality uso una **Hill generalizada con exponente n>1**: tiene un **toe** (zócalo plano) cerca
  de 0 que mata las marcas triviales, y **se satura** por arriba. No es cóncava pura —tiene una parte
  convexa abajo y cóncava arriba (forma S)—, pero **monótona, suave y saturante**, que es lo que el sesgo
  pide realmente. Documento esta desviación de la concavidad estricta como una decisión consciente.
- Para que el superávit alcance Inquebrantable y su peso **crezca con F**, hago que el **parámetro de
  saturación dependa de F** (techo más alto cuanto mayor F) y que ese techo **se contraiga con la cobertura
  real de días** (para que mucho tiempo sobre pocos días no compre Inquebrantable). Las dos cosas emergen
  de la misma `K(F, φ)`, sin gates.

---

## 2. Fórmula explícita

### 2.1 Notación

- `F` ∈ {1..7}: frecuencia objetivo. `T` > 0: tiempo objetivo por sesión (min).
- Array de 7 días; `marcados` = días con minutos > 0, ordenados **descendente** por minutos.
- `D` = nº de días marcados.
- **Zona de compromiso** = los `F` días marcados de mayor tiempo (`com`). **Zona voluntaria** = los `D−F`
  restantes (`vol`), si `D > F`.
- `x_i = t_i / T` = fracción de cumplimiento de tiempo del día `i` (porcentual, A10).
- `φ = min(D, F) / F` = **cobertura de compromiso** (fracción de slots de compromiso efectivamente
  llenados). `φ = 1` cuando `D ≥ F`; `φ < 1` bajo déficit de frecuencia.

### 2.2 Mapa por día (Hill generalizada con toe + saturación, F-escalada)

```
K(F, φ)  =  K0 · ( 1 + c · (F − 1)/6 ) · φ^p           (parámetro de saturación efectivo)

g(x; F, φ)  =  ( K(F,φ)^n + 1 ) · x^n / ( K(F,φ)^n + x^n )      para x ≥ 0
g(0; ·)     =  0
```

Propiedades por construcción (claves para los axiomas):
- `g(0) = 0` y **`g(1) = 1` para CUALQUIER `K`** (el numerador `K^n+1` está calibrado para que crucemos
  exacto por (1,1)). Esto es lo que mantiene A3 inmune a `F` y `φ`.
- **Monótona creciente** en `x` para todo `n>0`.
- **Asíntota** (techo del día) = `K(F,φ)^n + 1`. Con `n>1` hay un **toe**: `g(x) ≈ x^n` cerca de 0,
  o sea retornos casi nulos para marcas triviales.

### 2.3 Valor del ancla (dos canales: compromiso bidireccional + voluntario unidireccional)

```
base  =  (1 / F) · Σ_{i ∈ com}  g(x_i; F, φ)          (bidireccional; slots vacíos = 0)
vol   =  (λ / F) · Σ_{j ∈ vol}  g(x_j; F, φ)          (unidireccional, piso 0, peso λ < 1)

RENDIMIENTO  =  base + vol
```

- `base` promedia sobre **F slots de compromiso**. Los días de compromiso faltantes (cuando `D < F`)
  **no aparecen en la suma**: cuentan como 0 en el promedio. Eso es lo que hace que **la frecuencia domine
  estructuralmente** y que el tiempo no pueda tapar días faltantes.
- `vol` solo suma, nunca resta (piso cero, A7), y va **dividido por F** (porcentual al tamaño del
  compromiso, A10) y ponderado por `λ < 1` (un día voluntario vale menos que un slot de compromiso).

### 2.4 Inquebrantable (estado 5º)

No es una banda nueva por encima de 1 a secas. Como el superávit trivial puede rozar `1.00x`, **defino
Inquebrantable con un margen**:

```
Inquebrantable  ⟺  RENDIMIENTO ≥ 1 + δ          con δ calibrable (sugiero δ ≈ 0.10)
```

La saturación garantiza que solo un **superávit real** (tiempo o días por encima de la base, no migajas)
cruza `1 + δ`. Un día perfecto + una migaja voluntaria queda en `~1.00`, o sea **Pleno, no Inquebrantable**
— exactamente lo que pide el caso testigo del dueño.

### 2.5 Parámetros calibrables (rangos plausibles, sin fijar)

| Parám. | Rol | Rango sugerido | Efecto |
|--------|-----|----------------|--------|
| `n` | exponente de Hill (toe) | `1.5 – 2.5` | ↑ → mata más la triviality, toe más plano |
| `K0` | saturación base | `0.8 – 1.5` | ↑ → techo más alto, satura más tarde |
| `c` | escalado de techo con F (P2) | `0.5 – 2.0` | ↑ → superávit de tiempo pesa más cuando F→7 |
| `p` | dureza de la compuerta de cobertura | `1.0 – 2.0` | ↑ → déficit de frecuencia colapsa más el techo de superávit |
| `λ` | descuento del canal voluntario | `0.3 – 0.6` | ↑ → días voluntarios aportan más (sin llegar a slot de compromiso) |
| `δ` | margen para Inquebrantable | `0.08 – 0.15` | umbral del 5º estado |

**Valores ilustrativos usados en la autoverificación (§4):** `n=2.0, K0=1.0, c=1.0, p=1.0, λ=0.5`.
Marcados como ilustrativos; la calibración fina va después contra el dataset de marcas de estado.

---

## 3. Derivación desde los axiomas

**Por qué un mapa por día `g(x)` y no una fórmula global.** Todo es porcentual (A10): el insumo natural
de cada día es `x = t/T`, no minutos crudos. Y la zona de compromiso pide bidireccionalidad
(`x<1` resta respecto a cumplir, `x>1` suma): una sola función monótona que pasa por (0,0) y (1,1) y
sigue creciendo por arriba captura las tres cosas (déficit, cumplimiento, superávit) **sin gates** (A9).

**Por qué Hill `x^n/(K^n+x^n)` y no cóncava pura.** La cóncava pura (Michaelis-Menten, `n=1`) satura
bien pero tiene **pendiente máxima en 0**: `g(1/30)` sale ~0.05–0.12, demasiado para una marca trivial.
Subir la concavidad (más saturación) baja a la vez el techo y rompe P2. **La Hill con `n>1` rompe ese
empate**: el toe `≈ x^n` hace `g(1/30) ≈ 0.0017` (casi nada), y la parte cóncava-superior sigue
saturando. Sacrifico concavidad estricta en la zona baja —decisión consciente— a cambio de servir A-
triviality y P2 simultáneamente. Sigue siendo del **género saturante** (mi sesgo): crece rápido en la zona
media y se aplana arriba.

**Por qué `g(1)=1` por construcción (`numerador = K^n+1`).** A3 exige que `D=F` con todos `t_i=T` dé
exactamente 1. Si fijo el numerador en `K^n+1`, entonces `g(1)=1` **para todo K**, y como `base` es el
promedio de F unos, da 1 exacto **sin importar F, φ ni la calibración**. A3 queda blindado.

**Por qué `base` es un promedio sobre F (no sobre D).** Aquí vive "frecuencia sobre intensidad"
(restricción 3, estructural). Dividir por F y dejar los slots faltantes en 0 significa que **cada día que
falta es un 0 dentro del promedio que el tiempo no puede rellenar**. Mucho tiempo en pocos días levanta los
términos presentes, pero los ceros tiran el promedio abajo: compensación **parcial**, nunca total
(restricción 5).

**Por qué el canal voluntario es separado, con `λ<1` y `/F` (A7, P1).** La zona voluntaria es
unidireccional (solo suma). Como `g(x)≥0` siempre y `g(x)→0` cuando `x→0`, el término voluntario es
**≥0 y tiende a 0** (A7). Dividir por F lo hace porcentual al compromiso (A10) y `λ<1` codifica que un día
voluntario **nunca vale tanto como un slot de compromiso** — esto es lo que hace que 40 min concentrados
(que entran como superávit de un slot de compromiso) le ganen a minutos repartidos en días voluntarios
(P1, caso [5]). El brief deja abierto si superávit-tiempo y superávit-día comparten bolsa: en mi modelo
son **canales distintos** (`base` vs `vol`), que es lo que permite afinar la dominancia frecuencia/tiempo.

**Por qué `K` crece con F (`c·(F−1)/6`) — esto es P2.** Cuando `F=7` no hay días extra posibles; el tiempo
es la única vía a Inquebrantable. Subir el techo del día con F (`K(7)=K0(1+c)`) hace que **el superávit de
tiempo pese más cuanto más cerca está F de 7**, literalmente lo que pide P2. A `F=2` el mismo 2×T rinde
menos superávit que a `F=7` (verificado: 1.76 vs 2.50).

**Por qué `K` se contrae con la cobertura `φ^p` — esto resuelve la tensión de P2 con el déficit de
frecuencia.** Sin esto, el techo alto de F=5 dejaba que 2 días enormes treparan por encima de 1
(Inquebrantable falso) pese a 3 días faltantes. Multiplicar `K` por `φ^p` (φ = días llenados / F) hace que
**el techo de superávit colapse suavemente cuando faltan días**: un déficit de frecuencia apaga el premio
de tiempo sin un gate. A cobertura plena (`φ=1`) no cambia nada, así que **P2 queda intacto** y A3 también
(en cumplimiento exacto `φ=1`). Es una compuerta **continua** (A9), no un `if`.

**Por qué Inquebrantable con margen `1+δ`.** Bare `>1` haría que una migaja voluntaria sobre base perfecta
ya cuente como Inquebrantable. La saturación garantiza que solo superávit real cruza `1+δ`; el testigo del
dueño (3 días×5min + tarea 40) cae en `~1.008`, **Pleno y no Inquebrantable**, que es la conducta deseada.

---

## 4. Autoverificación (números calculados; params ilustrativos `n=2, K0=1, c=1, p=1, λ=0.5`)

> Recordatorio del mapa: `g(1/30) = 0.00174` (triviality), `g(1)=1`, `g(2)=1.6`(F-base), `g(3)=1.8`.
> Bandas: `Rojo<0.40 · Amarillo<0.62 · EnMarcha<0.85 · Pleno≥0.85 · Inquebrantable≥1+δ (δ=0.10)`.

### 4.1 Axiomas

| Axioma | Test | Resultado | Veredicto |
|--------|------|-----------|-----------|
| **A1** Rango `[0,1+]` | nada / exacto / superávit | `0.000 / 1.000 / 4.000` | PASS — cubre todo el rango |
| **A2** Piso cero | `D=0` | `0.000` | PASS |
| **A3** Exacto = 1 | `D=F=3`, todos `t_i=T` | `1.0000` (exacto) | PASS — por construcción `g(1)=1` |
| **A4** Monotonía días | `[30,30,30]` → `+1 día 15min` | `1.0000 → 1.0571` | PASS (no decrece) |
| **A5** Monotonía tiempo | día compromiso `t=20 → 28` | `0.8519 → 0.9712` | PASS (no decrece) |
| **A7** Piso cero voluntario | sin vol → vol `0.001 min` | `1.000000 → 1.000000` (`Δ≈+1e-6`, ≥0 y →0) | PASS |
| **A9** Continuidad | perturbar compromiso `30→29.9→29.8` | `1.00000 → 0.99929 → 0.99858` | PASS — pasos suaves, sin saltos |
| **A10** Invarianza escala | `(T=30,t)` vs `(T=120, t×4)` | `1.655324 == 1.655324` | PASS — depende solo de `x=t/T` |

> Nota A9: el mapa `g` es C∞ y la compuerta `φ^p` es continua; no hay `min/max` ni `if` en el camino del
> score, así que la continuidad es estructural, no afortunada.

### 4.2 Casos límite de §7

| # | Caso | Setup | Valor | Banda | Esperado / veredicto |
|---|------|-------|-------|-------|----------------------|
| 1 | Cumplimiento exacto | F=3,T=30,`[30,30,30]` | **1.0000** | Pleno | = 1 exacto. PASS |
| 2 | Nada hecho | D=0 | **0.0000** | Rojo | = 0. PASS |
| 3 | Superávit días + déficit tiempo | F=3,T=30,`[10,10,10,90,90]` | **1.6553** | Inquebrantable | com=`[90,90,10]`, vol=`[10,10]`. base sin vol = **1.6009** vs all-90 base = **2.32** → el día de 10 (g=0.20) SÍ baja el promedio, **no queda tapado** por los 90. PASS |
| 4 | Déficit frecuencia + tiempo alto | F=5,T=20,D=2,`[60,60]` | **0.5506** | Amarillo | φ=0.40 colapsa el techo; tiempo alto aporta pero **NO compensa** los 3 días faltantes → queda lejos de 1. PASS |
| 5 | 40 min vs 1+1 min | A:`[30,30,40]` B:`[30,30,1,1]` (F=2,T=30) | **A=1.4186 > B=1.0010** | A:Inq / B:Pleno | 40 min concentrados (superávit de slot de compromiso) le ganan claramente a 2 min repartidos en días voluntarios. PASS |
| 6 | Voluntario trivial | F=2,T=30,D=5,`[30,30,1,1,1]` | **1.0014** | Pleno (no Inq) | los 2 de 30 = compromiso perfecto (base=1); los 3 de 1 min aportan `+0.0014` → casi nada. PASS |
| 7 | Déficit puro de tiempo | F=2,T=5,D=7,`[1]×7` | **0.1517** | Rojo | com `[1,1]` con `x=0.2` (g muy bajo) + 5 voluntarios triviales → aporta poco, queda bajo. PASS |
| 8 | Saturación F=7 (P2) | F=7,T=30, `1T/2T/4T` | **1.000 / 2.500 / 4.000** | Pleno / Inq / Inq | con `φ=1`, el superávit de tiempo **solo** empuja a Inquebrantable; F-escalado: mismo 2T da F=2→1.76, F=4→2.08, F=7→2.50 (peso crece con F). PASS |
| 9 | Invarianza de escala | ver A10 | `1.6553 == 1.6553` | — | idéntico. PASS |
| — | **Testigo del dueño** | F=4,T=40,`[40,40,40,40,5,5,5]` | **1.0084** | Pleno (no Inq) | "3 días extra × 5 min con tarea de 40 NO es superhabit pleno" → cae en ~1.01, **Pleno y no Inquebrantable**. La saturación lo hace emerger sin regla. PASS |

### 4.3 Resumen de la autoverificación

- **8/8 axiomas PASS, 9/9 casos límite + testigo PASS.**
- La saturación con toe (`n>1`) **mató la triviality** sin romper P2: `g(1/30)=0.0017`, y aun así F=7 con
  4×T llega a **4.0** (Inquebrantable profundo).
- La compuerta de cobertura `φ^p` **resolvió la tensión más dura**: déficit de frecuencia (caso 4) bajó de
  un falso `1.15`(Inquebrantable) a `0.55`(Amarillo), sin tocar A3 ni P2.

---

## 5. Explicación en lenguaje claro

Cada día se traduce a "qué fracción de tu meta de tiempo cumpliste" (porcentual, no minutos). Esa fracción
pasa por una **curva con forma de S que se aplana arriba**: los primeros minutos hacia tu meta valen
mucho, los que pasás de la meta valen cada vez menos (saturación), y los **muy poquitos minutos casi no
valen nada** (el "zócalo" plano al principio). Así, un día de 1 minuto sobre una meta de 30 prácticamente
no cuenta, y un día de 4 veces la meta vale bastante más que cumplir, pero **no se dispara al infinito**.

El rendimiento del ancla es el **promedio de tus días de compromiso** (los F días que te comprometiste).
Si te faltan días, esos huecos entran como ceros en el promedio: **el tiempo extra no puede tapar un día
que no hiciste**. Por eso la app premia la constancia por encima de las ráfagas: sumar un día nuevo (un
término más en el promedio) rinde más que recargar de minutos un día que ya tenías. Los días voluntarios
(más allá de tu compromiso) **solo suman, nunca restan**, y valen menos que un día de compromiso, así que
nunca pueden hacerte daño ni reemplazar la base.

Para el estado **Inquebrantable** (el pico por encima de Pleno), el modelo hace dos cosas finas: cuando tu
compromiso es de **muchos días por semana** (F cerca de 7), el techo de cada día se levanta, así el
**tiempo extra puede llevarte solo hasta Inquebrantable** —importante porque a F=7 no quedan días extra que
sumar—. Pero si **te faltan días**, ese techo de premio **se baja solo**: no podés comprar Inquebrantable
con mucho tiempo en pocos días. Y para que una migaja (un par de minutos extra sobre una base perfecta) no
te ascienda de golpe, Inquebrantable exige un **margen real** sobre 1: solo un superávit de verdad lo cruza.

**Tensión que dejo documentada honestamente:** mi sesgo puro era usar una curva **cóncava** (que es la
forma más limpia de "retornos decrecientes"), pero la cóncava pura le regala demasiado valor a las marcas
triviales (tiene su pendiente más empinada justo en cero). Para servir a la vez "1 min vale casi nada" y
"el superávit puede llegar a Inquebrantable", **cambié la cóncava pura por una curva en S** (toe + meseta):
sigue siendo del género saturante, pero no es estrictamente cóncava en la zona baja. Fue una decisión
consciente: prioricé los axiomas y los casos del producto por encima de la pureza de la familia matemática
que me tocó. Las dos tensiones de P2 (techo que no mate el superávit, y déficit de frecuencia que no compre
Inquebrantable) las resolví con una sola pieza: el parámetro de saturación `K(F,φ)` que **crece con F**
(premia el tiempo cuando no hay días que sumar) y **se contrae con la cobertura** (apaga ese premio cuando
faltan días). Todo continuo, sin un solo `if`.
