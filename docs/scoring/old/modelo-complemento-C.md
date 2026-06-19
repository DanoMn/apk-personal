> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo de scoring — Complemento C (casos nuevos)

> **Estado: borrador para aprobar.** COMPLEMENTA (no rehace) `modelo-consolidado-v1.md`.
> Resuelve 3 casos límite nuevos: (1) sobriedad dentro del VALOR de Conducta — simetría con el
> sueño en Cuerpo; (2) capa modulada SIN anclas regulares; (3) reconciliación con las 45 marcas.
> Trabajo independiente. Fecha: 2026-06-08. Verificado con script descartable en `/tmp` (no toca `scripts/scoring/`).

---

## 1. Qué cambia respecto al consolidado v1 (delta puro)

Todo lo del consolidado v1 queda **intacto**: motor solo-pesos (`score = Σ peso×valor`), CERO
reglas/caps/gates/worst-term, Forma A (multiplicador + renormalización), bandas `R<0.40 · A<0.62 ·
EM<0.85 · P≥0.85`, gate Inquebrantable `≥2 capas`, soportes asimétricos al valor, tasks neutras,
higiene digital = ancla de Conducta. **Solo toco la fórmula del VALOR de la capa Conducta** (y, por
simetría, formalizo el caso degenerado de Cuerpo). Los pesos NO cambian: `k_sleep`, `k_sobr` y la
renormalización quedan exactamente igual.

| Tema | Consolidado v1 (antes) | Complemento C (ahora) |
|---|---|---|
| **Valor de Conducta, racha limpia** | `valor = promedio(anclas)` — la racha **NO suma nada** | `valor = γ·1.0 + (1−γ)·promedio(anclas)` — la racha **suma como logro** |
| **Valor de Conducta, recaída** | `valor = r_rel` (constante baja que **reemplaza** la ancla) | `valor = γ·r_rel + (1−γ)·promedio(anclas)` — la recaída **hunde**, mezclada |
| **Valor de Conducta, sin marcar** | `valor = r_unm` (reemplaza) | `valor = γ·r_unm + (1−γ)·promedio(anclas)` — mezclada |
| **Cuerpo + sueño** | `valor = β·sueño + (1−β)·promedio(anclas)` | **SIN CAMBIO** (ya era blend; ahora Conducta lo iguala) |
| **Capa modulada sin anclas** | indefinido (promedio de 0 anclas = se rompe) | `valor = modulador` (cae natural del blend renormalizado) |

**La asimetría que se corrige** (el "olor" del plan): antes, sueño-ok empujaba el valor de Cuerpo
hacia 1 (premio), pero racha-limpia dejaba el valor de Conducta clavado en el promedio de anclas (sin
premio). Resultado: una racha de 6 meses + una ancla de Conducta floja → Conducta pesada (k_sobr) y de
valor bajo → arrastraba a Atención. Ahora la racha limpia **es un logro que entra al valor**, igual que
dormir bien. La recaída sigue hundiendo —incluso más explícito— porque tira el término del modulador a `r_rel`.

---

## 2. Fórmula nueva del valor de Conducta (y el caso sin-anclas)

### 2.1 Forma unificada del valor de una capa modulada

Defino **una sola forma** para las dos capas con modulador (Cuerpo↔sueño, Conducta↔sobriedad),
de modo que la simetría sea estructural, no un parche:

```
valor_capa = blend·M + (1 − blend)·promedio(anclas)         [si la capa tiene anclas]
valor_capa = M                                               [si la capa NO tiene anclas]
```

donde:

- **Cuerpo**: `blend = β`, `M = sueño_valor` con `sueño_valor = { ok→1.0 , mal/no-registrado→s_bad }`.
- **Conducta**: `blend = γ`, `M = racha_valor` con `racha_valor = { limpia→1.0 , recaída→r_rel , sin-marcar→r_unm }`.
- Capa **sin modulador activo** (o capa normal): `valor = promedio(anclas)`.

El término `M` es un **valor en `[0,1]`**, igual que una ancla más. La racha limpia es el "mejor día
posible" de la sobriedad (1.0), exactamente como dormir bien es el mejor día del sueño. La recaída es
un día muy malo de esa "ancla-modulador" (`r_rel`, bajo), y sin-marcar es un día gris (`r_unm`).

> **Por qué es la misma fórmula que el sueño y no otra cosa.** El consolidado v1 ya define Cuerpo como
> `β·sueño + (1−β)·anclas`. Lo único que hacía a la sobriedad distinta era que su rama escribía
> `core = ancla` (limpia) o `core = constante` (recaída) — un `if` aparte. Al colapsar las dos ramas en
> el mismo blend, la sobriedad deja de ser un caso especial: es un modulador idéntico al sueño, con su
> propio `blend` y su propio mapa `estado→valor`. Cero reglas nuevas; una rama de código MENOS.

### 2.2 El caso sin-anclas (Conducta-solo-sobriedad, Cuerpo-solo-sueño)

Con cero anclas en la capa, `promedio(anclas)` es indefinido. La forma del blend lo resuelve **sola**
por renormalización: el peso `(1−blend)` que iría a las anclas no tiene a dónde ir, así que el modulador
se queda con todo el valor de la capa:

```
valor_capa(sin anclas) = M
```

No es una regla nueva: es el límite natural del blend cuando el conjunto de anclas es vacío (el promedio
de un conjunto vacío no aporta término, y se renormaliza al único término que queda — el modulador).
Es el **mismo mecanismo de renormalización** que ya usa Forma A para los pesos de capa.

Comportamiento resultante (verificado, ver §4):

| Caso (capa modulada sin anclas) | Valor de la capa | Estado típico (resto 100%) |
|---|---|---|
| Conducta solo sobriedad — **limpia** | `1.0` | **P** |
| Conducta solo sobriedad — **recaída** | `r_rel` (≈0.3) | **EM** (la recaída la hunde y pesa por k_sobr) |
| Conducta solo sobriedad — **sin marcar** | `r_unm` (≈0.4) | **EM** (= ventana perdón, topa EM) |
| Cuerpo solo sueño — **ok** | `1.0` | **P** |
| Cuerpo solo sueño — **mal/no-reg** | `s_bad` (≈0.3) | **EM** (sueño malo topa EM, como SU2/SU3) |

Esto es **coherente con el dominio**: si tu única actividad de Conducta es no recaer, entonces tu semana
de Conducta ES tu racha. Limpia → la capa está perfecta; recaída → la capa colapsa al piso del modulador
y, como la sobriedad infla el peso de Conducta (`k_sobr`), el golpe se propaga a todo el score. Idéntico
al sueño siendo la única "actividad" de Cuerpo.

---

## 3. El blend exacto: γ = 0.5, y por qué

**Elijo `γ = 0.5`** (la racha pesa la mitad del valor de Conducta; las anclas de Conducta la otra mitad),
**en simetría con `β = 0.5`** del sueño en Cuerpo.

Justificación:

1. **Simetría literal con el sueño (lo que pide el caso 1).** El consolidado v1 fija el sueño como "mitad
   sueño / mitad caminar" dentro de Cuerpo (`β=0.5`, ver `dataset-decisiones-estado-v1.md` línea del
   modelo base: "sueño = Caminar DENTRO de Cuerpo, 0.25 efectivo c/u"). La forma simétrica para la
   sobriedad es `γ=0.5`: mitad racha / mitad anclas de Conducta. Cualquier otro valor rompería la simetría
   que el caso 1 pide explícitamente.

2. **El verificador lo permite sin perder marcas.** El óptimo robusto sobre las 45 (ver §4) cae en
   `γ=0.5`, y `γ` queda **libre entre {0.5, 0.6}** — ambos reproducen 44/45. No hay tensión: 0.5 es el
   valor centrado y simétrico.

3. **Es el blend que hace que la racha "sea un logro" sin tapar las anclas.** Con `γ=0.5`, una racha
   limpia sobre anclas de Conducta al 25% da valor `0.5·1 + 0.5·0.25 = 0.625` (sube de 0.25 a 0.625 —
   la racha rescata medio camino), pero NO lo lleva a 1.0: las anclas flojas siguen visibles. La recaída
   con anclas al 100% da `0.5·r_rel + 0.5·1 ≈ 0.65` (baja de 1.0 — la recaída hunde sin aniquilar). Es el
   punto donde racha y anclas **se reparten parejo** la voz de la capa.

> **Nota sobre el rango de `γ`.** El sueño y la sobriedad NO necesitan el mismo `blend` numérico — son
> moduladores distintos. Pero como el dueño marcó al sueño con `β=0.5` y no hay marca que pida una
> sobriedad más o menos dominante DENTRO de su capa, `γ=0.5` es la elección de mínima sorpresa
> (sub-identificado igual que el resto; se pincha el día que haya una marca tipo "racha limpia + ancla
> de Conducta a X% → estado Y" que discrimine `γ`).

---

## 4. Verificación contra las 45 marcas

Script descartable: `/tmp/complemento_c_fit.py` (réplica exacta de la estructura de
`scripts/scoring/weight_model_fit_v2.py`, con el blend de sobriedad agregado; **NO se tocó
`scripts/scoring/`**). Las 45 marcas se copiaron idénticas a `CASES`.

### 4.1 Resultado: 44/45 — flipea EXACTAMENTE SB9 (A→EM), como predijo el plan

```
44/45 casos · margen mínimo = 0.0133 · 3072 sets dan 44/45
Óptimo robusto: beta=0.6 gamma=0.5 s_bad=0.3 r_rel=0.3 r_unm=0.4
                k_sleep=1.0 k_sobr=2 p_sop=0.25 b_sop=0.0 sop_form=lin
                cRA=0.43 cAEM=0.64 cEMP=0.88
Fallo (1): SB9  esperado=A  pred=EM  S=0.8125
```

El **único** caso que cambia de veredicto bajo la estructura nueva es **SB9**. Las otras 44 marcas
—incluidas todas las de sobriedad (SB1–SB8, SB10), las del sueño (SU1–SU9), base pura, soportes e
Inquebrantable— se reproducen sin tocar nada más. La distribución de etiquetas no se altera salvo ese
caso.

### 4.2 Por qué flipea SB9 — y por qué es CORRECTO que flipee

SB9 = `Interior 100%, Cuerpo 100%, Conducta-ancla 25%, racha limpia 6 meses`. El dueño lo marcó **A**.

**Bajo v1** (racha no suma al valor): Conducta valor = 0.25; con `k_sobr` la capa pesa ~60% del score →
el 0.25 arrastra → A. La racha de 6 meses **no contaba para nada** en el estado.

**Bajo el complemento C** (racha entra al valor, `γ=0.5`):

```
Conducta valor = 0.5·1.0 + 0.5·0.25 = 0.625
Pesos (3 capas, k_sobr): I=0.25  Cu=0.25  Co=0.50
Score = 0.25·1.0 + 0.25·1.0 + 0.50·0.625 = 0.8125  → EM
```

La racha limpia **rescata** la capa de 0.25 a 0.625, y el score sube de la zona A a la zona EM. **Esto es
lo que el plan pedía**: era un olor que una racha intachable + una sola ancla floja te dejara en Atención.
Con la racha contando como logro, el sistema reconoce que la persona viene sosteniendo lo más difícil
(la sobriedad) → **En marcha**, no Atención.

**El flip es FORZADO, no un artefacto de calibración.** Verifiqué exhaustivamente: **NINGÚN** set de
parámetros del grid mantiene SB9 en A si la racha entra al valor (`γ≥0.5`):

```
Mejor fit que MANTIENE SB9=A con gamma>=0.5:  -1/45   (params: None)
```

Es decir: para conservar SB9 en A habría que **sacar la racha del valor** (`γ=0`), que es justo lo que
el caso 1 prohíbe. No hay forma honesta de tener las dos cosas. 44/45 es el **techo** de la estructura nueva.

### 4.3 Recomendación honesta al dueño: re-marcar SB9

**SB9 debería re-marcarse a En marcha.** Razón: el dueño marcó A *bajo el modelo viejo*, donde la racha
no sumaba. Bajo la regla de diseño que él mismo pidió ("la sobriedad es un logro que cuenta, como el
sueño"), una racha de 6 meses limpia con el resto de la vida casi en orden NO es Atención. La marca vieja
A es coherente con la estructura vieja; la estructura nueva (que el dueño autorizó) implica EM. No fuerzo
ninguna regla-parche para tapar el cambio: el flip es la consecuencia natural y correcta de la simetría.

Ninguna otra marca pide re-marca. SB7/SB8 (50% + racha) siguen en A (la racha al 50% da
`0.5·1+0.5·0.5=0.75` de valor de Conducta, pero las otras dos capas al 50% mantienen el score en zona A).
SB1–SB6 y SB10 no se mueven (anclas 100% o piso, donde el blend no cruza ningún corte).

### 4.4 Casos nuevos sin-anclas (no estaban en las 45 — verificación de robustez)

Agregué 6 casos sintéticos de capa modulada sin anclas. Todos caen donde el dominio espera:

```
Conducta solo sobriedad limpia        -> P    (capa perfecta)
Conducta solo sobriedad recaída       -> EM   (recaída hunde + k_sobr propaga)
Conducta solo sobriedad sin marcar    -> EM   (= ventana perdón, topa EM)
Cuerpo solo sueño ok                  -> P
Cuerpo solo sueño mal                 -> EM   (= SU2/SU3, sueño malo topa EM)
Anclas flojas (I,Cu=50%) + Conducta-solo-limpia -> EM
```

El modelo **ya no se rompe** con cero anclas en la capa modulada: el valor sale enteramente del modulador.

---

## 5. Tradeoffs y riesgos

1. **SB9 cambia de veredicto (44/45).** No es un defecto del modelo: es el efecto buscado del caso 1.
   El riesgo es de **expectativa**: el dueño debe confirmar la re-marca SB9→EM. Si insistiera en SB9=A,
   sería incompatible con "la racha es un logro que cuenta" — habría que elegir uno de los dos. Lo dejo
   explícito para que el dueño decida con los ojos abiertos.

2. **`γ` queda sub-identificado (0.5 ó 0.6).** Igual que casi todos los parámetros del consolidado v1
   (~300–2900 sets dan fit). Elegí `γ=0.5` por simetría con `β`. Para pincharlo hace falta **una marca
   nueva** que varíe la ancla de Conducta con racha limpia (p.ej. "racha limpia + Conducta-ancla a 50% →
   ¿EM o P?"). Sin esa marca, 0.5 es la elección de mínima sorpresa.

3. **La recaída ahora se MEZCLA con las anclas (antes las reemplazaba).** Consecuencia: con anclas de
   Conducta altas, una recaída ya no clava el valor en `r_rel` puro, sino en `γ·r_rel + (1−γ)·anclas`. En
   la práctica las marcas de recaída (SB4–SB6) tienen anclas al 100%, así que el valor de Conducta tras
   recaída es `0.5·0.3 + 0.5·1.0 = 0.65` → con `k_sobr` sigue bajando el score a EM (P→EM, el escalón que
   marcó el dueño). El golpe se mantiene. **Pero ojo**: si en el futuro hay una marca de "recaída + anclas
   de Conducta también bajas", el valor podría hundirse MÁS que antes (los dos términos bajos se suman).
   Eso no contradice ninguna marca actual, pero conviene una marca que lo confirme.

4. **No resuelve el agujero abierto de v1 (sueño + sobriedad juntos: CERO marcas).** El complemento C
   hace a la sobriedad simétrica al sueño en la fórmula del valor, lo que hace la combinación más
   predecible, pero **sigue sin haber una marca** que valide qué pasa con ambos moduladores activos.
   Queda igual de abierto que en v1 §6. La simetría reduce el riesgo (ahora ambos se comportan igual),
   pero no lo cierra.

5. **Riesgo CERO de reglas-parche.** El complemento NO agrega ningún `if`, cap, gate ni clamp nuevo. Al
   contrario: **elimina** la rama especial de la sobriedad (que era un `if` aparte) y la funde en el mismo
   blend del sueño. El motor queda más simple: una sola forma de valor para capa-con-modulador, y el caso
   sin-anclas es el límite natural de esa forma. Todo sigue emergiendo de `peso × valor`.

---

## 6. Resumen de una línea para el merge

Sobriedad entra al valor de Conducta con el **mismo blend que el sueño** (`valor = γ·racha + (1−γ)·anclas`,
`γ=0.5`); la racha limpia suma como logro (=1.0), la recaída lo hunde (=`r_rel`); capa modulada sin anclas
→ `valor = modulador` (límite natural del blend, sin regla); reproduce **44/45** y flipea exactamente
**SB9 A→EM** (forzado, correcto, re-marcar). Cero reglas nuevas; una rama de código menos.
