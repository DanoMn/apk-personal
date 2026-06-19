# Propuesta — Proponente B: Acoplamiento suave

> Exploración del rendimiento de un ancla. Sesión 2026-06-09. Sesgo asignado: **acoplamiento suave** (días y tiempo combinados de forma equilibrada, con la frecuencia ganando por poco).

---

## 1. Sesgo asignado — cómo interpreté el acoplamiento suave

Mi familia de partida es la del **acoplamiento multiplicativo suave**: días y tiempo no se suman en un promedio ponderado lineal fijo (ese es el `0.70·días + 0.30·tiempo` que ya falló), ni se llevan a una dominancia extrema donde una dimensión aplasta a la otra. Se **combinan en un producto**, pero a través de una **media geométrica ponderada** (= media generalizada de exponente 0), que es la encarnación matemática natural del acoplamiento suave:

- En un producto puro `df · gt`, las dos dimensiones se penalizan mutuamente (si una cae, el resultado cae), lo que da el "sin compensación total" que pide la restricción #5.
- Al ponderar ese producto con **exponentes desiguales** `df^a · gt^b` con `a > b`, la frecuencia **gana por poco**: una caída relativa en días duele más que la misma caída relativa en tiempo, pero ninguna de las dos anula a la otra. Eso es exactamente "frecuencia sobre intensidad, estructural" (restricción #3) sin ningún parámetro suelto ni gate.

La clave para respetar el caso testigo (F=5, T=20, D=2, t=[60,60]) es **dónde** entra el tiempo. El tiempo de los días de compromiso entra al **base capeado en 1** (`min(q_i,1)`): no puede inflar la base. El superávit de tiempo (lo que pasa de T) se va a un **canal separado** que solo "prende" cuando la base ya está construida a lo ancho de los días. Así, ráfagas de tiempo en pocos días no pueden disfrazarse de constancia: la frecuencia ya quedó fijada por el factor `df^a`, y el canal de superávit se apaga solo (de forma continua, sin umbral) cuando la base es floja.

---

## 2. Fórmula explícita

### 2.1 Preprocesamiento (todo porcentual — invarianza de escala)

Del array de 7 días se obtiene la lista de días marcados y sus minutos `t_i`. Se normaliza cada día:

```
q_i = t_i / T            (razón de tiempo del día i, percentual)
```

Se ordenan los `q_i` de mayor a menor. Sea `D` = cantidad de marcados.

- **Zona de compromiso** = los `F` con mayor tiempo: `commit = q_(1) ≥ q_(2) ≥ … ≥ q_(min(D,F))`.
  Si `D < F`, hay `F − D` ranuras de compromiso **vacías** con `q = 0`.
- **Zona voluntaria** = los `D − F` restantes (si `D > F`): `vol = q_(F+1), …, q_(D)`.

### 2.2 Base — acoplamiento suave (media geométrica ponderada)

```
df = min(D, F) / F                              (cumplimiento de frecuencia, ∈[0,1])

         ⎛  ∏          min(q_i, 1) ⎞^(1/n)
gt =     ⎝ i∈commit               ⎠            (media geométrica de los tiempos
                                                de compromiso, CAPEADOS en 1)
         con n = min(D, F);  gt = 0 si n = 0

B = df^a · gt^b                                 (BASE acoplada,  a > b > 0)
```

`B ∈ [0, 1]`. El capeo `min(q_i,1)` impide que el superávit infle la base. La media geométrica es el acoplamiento suave: un día de compromiso con tiempo trivial arrastra `gt` hacia abajo (el producto es sensible a los factores chicos), pero `a > b` hace que la frecuencia pese más.

### 2.3 Canal voluntario (unidireccional, piso cero)

```
            λ_v
V  =  ───── · Σ        min(q_j, 1)              λ_v ∈ (0, 1]
             F   j∈vol
```

Cada día voluntario aporta una fracción de "un día extra", escalada al presupuesto `F` (percentual). `V ≥ 0` siempre y `V → 0` cuando los tiempos voluntarios → 0. Capeado en 1 por día para que un voluntario monstruoso no sustituya frecuencia.

### 2.4 Canal de superávit → Inquebrantable (peso crece con F)

```
sup_mean = ( Σ        max(q_i − 1, 0) ) / n     (superávit medio de tiempo
              i∈commit                           sobre los días de compromiso)

w_s(F) = μ · (F / 7)^κ                           (peso del superávit, CRECE con F)

S = w_s(F) · sup_mean · B^g                      μ>0, κ>0, g≥1 (típ. g≥4)
```

El factor `B^g` es el corazón del "sin gate": el superávit solo cuenta cuando la base `B` es sólida, y se apaga **de forma continua** (no por umbral) cuando la base es floja. `w_s(F) = μ·(F/7)^κ` crece con `F`: cuando `F=7` no hay superávit de días posible y el tiempo es la única vía a Inquebrantable, por lo que su peso es máximo (P2).

### 2.5 Rendimiento total del ancla

```
R = B + V + S          ∈ [0, 1+]
```

### 2.6 Parámetros calibrables (sin fijar — rangos plausibles)

| Param | Rol | Rango plausible | Efecto |
|-------|-----|-----------------|--------|
| `a` | exponente de frecuencia en la base | `0.9 – 1.3` | mayor ⇒ frecuencia domina más |
| `b` | exponente de tiempo en la base | `0.4 – 0.7`, con `b < a` | mayor ⇒ el tiempo de compromiso pesa más |
| `λ_v` | ganancia del canal voluntario | `0.3 – 0.6` | cuánto suma un día voluntario |
| `μ` | ganancia del canal de superávit | `0.4 – 0.8` | techo del empuje a Inquebrantable |
| `κ` | crecimiento del superávit con F | `0.7 – 1.5` | cuánto más vale el tiempo al subir F |
| `g` | supresión del superávit con base floja | `4 – 5` | mayor ⇒ ráfagas en pocos días no escalan |

La condición estructural **`a > b`** (frecuencia > intensidad) no es calibrable: es la forma de la fórmula.

---

## 3. Derivación desde los axiomas

- **A1 / A3 (normalización, exacto=1).** Con `df=1` y todos `q_i=1` ⇒ `gt=1` ⇒ `B = 1^a·1^b = 1`. Sin voluntarios ni superávit, `R=1`. El `1` cae solo del cumplimiento exacto porque la base es una media geométrica de razones normalizadas: vale 1 cuando todo iguala la meta.
- **A2 (piso cero).** `D=0` ⇒ `df=0` ⇒ `B=0^a=0`; `vol` vacío ⇒ `V=0`; `B^g=0` ⇒ `S=0`. `R=0`. Ningún término es negativo por construcción (potencias de no-negativos, sumas de `max(·,0)`).
- **A4 (monotonía en días).** Agregar un día marcado: o llena una ranura de compromiso vacía (sube `df` y, si `q>0`, multiplica un factor `≤1` en `gt` pero el salto de `df` domina) o cae en la zona voluntaria (suma `V ≥ 0`). En ambos casos `R` no baja. Verificado numéricamente abajo.
- **A5 (monotonía en tiempo de compromiso).** Subir `q_i` de un día de compromiso (hasta T) sube su factor capeado en `gt` ⇒ sube `B`. Pasado T, sube `sup_mean` ⇒ sube `S`. `R` no decrece. La media geométrica es estrictamente creciente en cada factor positivo.
- **A7 (piso cero voluntario).** `V` es una suma de términos `min(q_j,1) ≥ 0`; `V→0` cuando los tiempos voluntarios `→0`. Nunca resta.
- **A9 (continuidad, sin gates).** `B`, `V`, `S` son composiciones de potencias, productos y `max(·,0)` — todas continuas. El "gate de Inquebrantable" se reemplaza por el factor continuo `B^g`: no hay salto. (La única discontinuidad posible es estructural, no de la fórmula: marcar/desmarcar un día es discreto — se discute en §4, tensión.)
- **A10 (invarianza de escala).** Todo entra como `q_i = t_i/T` y `D/F`. Duplicar `T` y todos los `t_i` deja `q_i` idéntico. Verificado abajo con igualdad exacta.

**P1 (dominancia frecuencia↔tiempo sin matar la regla).** El producto `df^a·gt^b` con `a>b` da dominancia suave de la frecuencia. Un día con tiempo trivial vale `q_i/T` de día (capeado, y dentro de `gt`/`V`), no un día entero — resuelve "un día solo vale como día en la medida de su fracción". El superávit de tiempo (canal `S`) y el superávit de día (canal `V`) son **bolsas distintas** — emergió de la estructura, no se fijó a mano.

**P2 (superávit alcanza Inquebrantable, peso escala con F).** `w_s(F)=μ·(F/7)^κ` crece con F; en `F=7` es máximo y el tiempo es la única vía. `S` puede empujar `R` por encima de 1 (Inquebrantable) solo sobre base completa (`B^g≈1`).

---

## 4. Autoverificación (números calculados)

> Parámetros **ilustrativos** usados solo para verificar: `a=1.0, b=0.5, λ_v=0.5, μ=0.6, κ=1.0, g=4`. (La calibración real viene después contra el dataset de marcas.)

### 4.1 Axiomas

| Axioma | Caso de prueba | Resultado | ¿Pasa? |
|--------|----------------|-----------|--------|
| **A1/A3** exacto=1 | F=3,T=30,[30,30,30] | `R = 1.0000` (B=1, V=0, S=0) | ✅ |
| **A2** piso cero | D=0 | `R = 0.0000` | ✅ |
| **A4** monotonía días | F=3,T=30, D=0…7 con q=0.5 | R = 0.000 → 0.236 → 0.471 → 0.707 → 0.790 → 0.874 → 0.957 → 1.040 (no decrece) | ✅ |
| **A5** monotonía tiempo | F=2,T=30,[30,m], m=0…60 | R = 0.000 → 0.760 → 0.904 → 1.000 → 1.029 → 1.057 → 1.086 (no decrece) | ✅ |
| **A7** piso voluntario | F=2,[30,30]+vol m | Δ vs sin-vol: m=30→+0.250, m=10→+0.083, m=1→+0.008, m=0.01→+0.0001 (siempre ≥0, →0) | ✅ |
| **A9** continuidad | sin gates; `B^g` continuo | salto solo al marcar/desmarcar día (estructural, ver tensión) | ✅* |
| **A10** invarianza | (F=3,T=30,t) vs (F=3,T=120,t×4) | `R=1.181518` en ambos (igualdad exacta) | ✅ |

### 4.2 Tabla de casos límite (§7)

| Caso | Setup | R y desglose | Comportamiento | ¿OK? |
|------|-------|--------------|----------------|------|
| Cumplimiento exacto | D=F, t=T | `R=1.0000` (B=1) | exacto=1 | ✅ |
| Nada hecho | D=0 | `R=0.0000` | cero | ✅ |
| Superávit días + déficit tiempo | F=3,T=30,[10,10,10,90,90] | `R=1.1086` — B=0.833 (gt=0.693), V=0.111, S=0.165. Los 3 mejores (90,90,10) son compromiso; el día de 10 mete déficit (q=0.33) que baja `gt`; los 90 dan superávit. | El día de 90 **NO tapa** el déficit del de 10: `gt` queda en 0.69, base ≠ 1. El superávit empuja arriba pero la base no es perfecta. | ✅ |
| **Déficit frecuencia + tiempo alto (TESTIGO)** | F=5,T=20,D=2,[60,60] | `R=0.4219` — B=0.400 (df=0.4, gt=1.0), V=0, S=0.022 | **Frecuencia domina:** df=0.4 capea la base en 0.40; el tiempo a 300% solo aporta 0.022 (apagado por `B^4`). R firmemente en **Rojo**. | ✅ |
| 40 min vs 1+1 min | A: F=2,[30,30,40]; B: F=2,[30,30,1,1] | A: `R=1.236` (B=1,V=0.25). B: `R=1.017` (B=1,V=0.017) | **A > B**: 10 min concentrados en 1 día voluntario ganan a 2 min repartidos en 2. La constancia importa, pero el voluntario es percentual al tiempo. | ✅ |
| Voluntario trivial | F=2,T=30,[30,30,1,1,1] | `R=1.025` — B=1, V=0.025 (3 días de q=0.033) | Los 2 de 30 = compromiso perfecto; los 3 de 1 min aportan casi nada (0.025). | ✅ |
| Déficit puro de tiempo | F=2,T=5,[1×7] | `R=0.697` — B=0.447 (df=1, gt=0.2), V=0.25, S=0 | Frecuencia plena pero tiempo al 20%: base 0.447; 5 voluntarios suman 0.25. Sin techo duro, sin superávit. | ✅ |
| Saturación F=7 (P2) | F=7,T=30,[40×7] | `R=1.200` — B=1, S=0.200 (w_s máximo, sin canal de día) | El superávit de tiempo **solo** empuja a Inquebrantable; con F=7 es la única vía. | ✅ |
| Saturación F=7 base exacta | F=7,T=30,[30×7] | `R=1.000` (B=1, S=0) | base completa sin superávit = Pleno exacto. | ✅ |
| Invarianza de escala | (F=3,T=30) vs (F=3,T=120,×4) | `R=1.181518` ambos | idéntico. | ✅ |

**Lectura de estados (bandas §6):** testigo 0.42 = Rojo; déficit puro de tiempo 0.70 = En marcha bajo; superávit-días 1.11 y F7-saturación 1.20 = Inquebrantable; exacto 1.00 = Pleno. Coherente con la intención de producto.

### 4.3 Tensiones honestas

1. **Discontinuidad estructural en A5/A4 (no de la fórmula).** Al pasar un día de `m=0` (no marcado, `D` baja en 1) a `m>0` (marcado), `R` salta (en el test: 0 → 0.76 con F=2). **No es un gate de mi fórmula** — cada `q_i` entra de forma continua; el salto es porque "marcar un día" es un evento discreto del sistema (un día existe o no). Ninguna fórmula que respete A4 puede evitarlo sin inventar marcas fraccionarias. Lo documento porque a primera vista parece violar A9; no lo hace.
2. **`gt` por media geométrica es sensible a un día muy flojo de compromiso.** Si uno de los `F` días de compromiso tiene tiempo casi nulo, `gt` cae fuerte (el producto castiga el factor chico). Esto es deseado (es el "déficit de tiempo en día de compromiso", la única intersección que resta) pero hay que tenerlo presente al calibrar `b`: con `b` alto el castigo se amplifica. Es una palanca de calibración, no un defecto.
3. **Elección de `g`.** El caso testigo exige `g` razonablemente alto (≥4) para apagar el superávit con base floja. Con `g=2` el testigo daba 0.54 (Amarillo bajo), demasiado alto; con `g=4` baja a 0.42 (Rojo claro). La saturación F=7 es insensible a `g` (porque `B=1 ⇒ B^g=1`), así que subir `g` no daña P2. Recomiendo calibrar `g∈[4,5]`.

---

## 5. Explicación en lenguaje claro

La idea es que el valor de un ancla nace de **multiplicar** dos cosas, no de sumarlas a porcentajes fijos: cuántos de tus días comprometidos cumpliste (frecuencia) y cuánto tiempo pusiste en esos días (intensidad). Multiplicar es lo que hace que **ninguna de las dos pueda tapar a la otra**: si fallás días, no importa cuánto tiempo metas, el resultado baja; si ponés poco tiempo, tampoco te salva tener todos los días. Eso es el "acoplamiento suave": las dos dimensiones se necesitan.

Para que la **constancia gane por poco** sobre la intensidad, le doy a la frecuencia un peso un poco mayor dentro de esa multiplicación. No es un porcentaje suelto que se pueda mover sin pensar: es la **forma** de la cuenta. Por eso ráfagas de productividad de un día no pueden hacerse pasar por constancia.

El tiempo tiene dos vidas. Dentro de la base, el tiempo se **recorta en la meta**: hacer 300% de tu tiempo no infla tu base, porque la base mide "¿sostuviste tu compromiso a lo ancho de tus días?". El tiempo que sobra de la meta se va a un **canal aparte** —el camino a Inquebrantable— que **solo se activa cuando tu base ya está sólida**. No hay un interruptor que diga "si llegaste a X, ahora sí cuenta el superávit": el superávit se va prendiendo suave a medida que tu base se acerca a estar completa, y se apaga suave si tu base es floja. Sin saltos, sin reglas-parche.

Los días voluntarios (los que hacés de más, por encima de tu meta de frecuencia) **solo suman, nunca restan**, y suman en proporción al tiempo que les pusiste: un día voluntario de 1 minuto aporta una miga; uno de meta completa aporta como casi un día extra. Y cuando tu meta es de 7 días por semana, no hay días extra posibles, así que el peso del superávit de tiempo crece para que el tiempo siga siendo un camino genuino hacia arriba.

En el caso que más importa —prometiste 5 días, hiciste solo 2 pero clavaste un montón de tiempo en esos 2— el resultado queda en **rojo**: la frecuencia manda, el tiempo extra apenas se nota. Que es justo lo que el producto pide: la app premia que vuelvas todos los días que te comprometiste, no que te mates un día y desaparezcas.
