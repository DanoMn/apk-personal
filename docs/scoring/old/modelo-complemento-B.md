> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo de scoring — Complemento B (casos nuevos)

> **Estado: borrador para aprobar.** Complementa (NO rehace) `modelo-consolidado-v1.md`.
> Resuelve 3 casos límite nuevos: (1) sobriedad como parte del VALOR de Conducta —simetría con
> el sueño en Cuerpo—, (2) capa modulada sin actividades regulares, (3) reconciliación con las
> 45 marcas. Trabajo independiente. Fecha: 2026-06-08.
> Base cerrada: `docs/scoring/modelo-consolidado-v1.md`. Marcas: `dataset-decisiones-estado-v1.md`.
> Verificación: script descartable en `/tmp/complement_B_fit.py` (NO toca `scripts/scoring/`).

---

## 1. Qué cambia respecto al consolidado v1 (delta)

Todo lo del consolidado v1 queda IGUAL: motor de pesos puros (`score = Σ peso×valor`), CERO
reglas/caps/gates/worst-term, Forma A (multiplicador + renormalización) para los pesos, bandas
R/A/EM/P, gate Inquebrantable ≥2 capas, soportes asimétricos al valor, tasks neutras, higiene
digital = ancla de Conducta. **No se toca ningún peso, ni los cortes de banda, ni la mecánica de
renormalización.**

El **único** delta es la fórmula del **VALOR de la capa Conducta** cuando la sobriedad está
activa, más su caso degenerado (capa sin anclas). En una línea:

| | v1 (consolidado) | v1 + Complemento B |
|---|---|---|
| **Valor Conducta con sobriedad** | `limpia → avg(anclas)` · `recaída → r_rel` · `sin-marcar → r_unm` | `valor = γ·señal_sobr + (1−γ)·avg(anclas)`, con `señal_sobr = {limpia:1, recaída:r_rel, sin-marcar:r_unm}` |
| **La racha limpia, ¿suma al valor?** | NO (solo pasa el promedio de anclas; la racha no aporta nada) | **SÍ** — inyecta un término `1.0` de logro, EXACTAMENTE como el sueño `ok` inyecta `sv=1.0` |
| **Capa modulada sin anclas** | indefinido (promedio de conjunto vacío → el modelo se rompe) | `valor = señal del modulador` (límite natural del blend cuando #anclas→0) |

Esto **elimina la asimetría que el plan marcó como "olor"**: hoy el sueño entra al valor de
Cuerpo (mitad sueño/mitad caminar) pero la racha limpia NO entra al valor de Conducta. Tras B,
las dos capas moduladas usan **la misma forma de blend**.

---

## 2. Fórmula nueva del valor de Conducta + caso sin-actividades

### 2.1 Forma canónica (simétrica al sueño)

El consolidado v1 ya define el valor de Cuerpo con sueño como un blend:

```
valor_Cuerpo  = β·señal_sueño   + (1−β)·avg(anclas de Cuerpo)
   señal_sueño = { ok: 1.0,  mal: s_bad,  no-registrado: s_bad }
```

Complemento B le da a Conducta **la misma estructura**:

```
valor_Conducta = γ·señal_sobr   + (1−γ)·avg(anclas de Conducta)
   señal_sobr   = { limpia: 1.0,  recaída: r_rel,  sin-marcar: r_unm }
```

- **Racha limpia (held)** → `señal_sobr = 1.0`. La racha **suma como logro**: levanta el valor de
  Conducta por encima de lo que dan solas las actividades de Conducta. (En v1 la racha no aportaba
  nada — esto es el corazón del caso 1.)
- **Recaída (broke)** → `señal_sobr = r_rel` (≈0.4). Hunde el valor de Conducta hacia el piso de
  recaída, igual que el sueño malo hunde Cuerpo hacia `s_bad`.
- **Sin marcar (ventana de perdón)** → `señal_sobr = r_unm` (≈0.4). Topea, no asume recaída
  (mismo trato que el sueño no-registrado).
- **El largo de la racha NO entra** (5 días = 6 meses): `señal_sobr` es **binaria held/broke**, el
  largo se premia EN la feature de sobriedad (contador/hitos), NO en el estado. Esto **respeta SB1=
  SB2=SB3=P y SB4=SB5=SB6=EM** del consolidado v1; B no toca esa decisión.

El golpe de la recaída sobre TODO el Score sigue **emergiendo** (no es regla): con sobriedad activa
Conducta pesa `×k_sobr` Y la recaída le hunde el valor → el golpe sale del peso × valor. Idéntico a
v1; lo que cambia es solo que ahora, en limpio, el valor también **sube**.

### 2.2 Caso 2 — capa modulada SIN actividades regulares

Cuando una capa modulada **no tiene anclas** (Conducta con solo sobriedad y sin Orden digital;
Cuerpo con solo sueño y sin Caminar), el término `avg(anclas)` no existe. La regla cae sola del
blend en el límite `#anclas → 0`:

```
si la capa modulada no tiene anclas:
   valor_capa = señal del modulador          (NO es un caso especial: es γ→1 / β→1 efectivo)
```

- **Conducta solo-sobriedad**: `valor = señal_sobr` → limpia=1.0 · recaída=r_rel · sin-marcar=r_unm.
- **Cuerpo solo-sueño**: `valor = señal_sueño` → ok=1.0 · mal=s_bad · none=s_bad.

No hay división por cero, no hay promedio de conjunto vacío, no hay regla-parche. El valor sale
**enteramente del modulador**, que es exactamente lo que el dominio pide: si la única práctica de
esa capa es mantenerte limpio (o dormir bien), tu estado en esa capa ES esa práctica.

> Nota de implementación: es el mismo principio que "promediar dentro de la capa" del v1 (§4.3),
> tomado al borde. Con 0 anclas el promedio se reemplaza por la señal del modulador; con ≥1 ancla
> se mezcla vía γ (o β). Continuo, sin saltos.

---

## 3. El blend exacto (γ) y su justificación

**Elijo `γ = 0.5`** (la racha limpia y las actividades de Conducta pesan **mitad y mitad** dentro
del valor de la capa). Justificación, en orden de fuerza:

1. **Es lo que las marcas permiten y centran.** En la búsqueda de grid sobre las 45 marcas, los
   óptimos robustos viven en `γ ∈ {0.4, 0.5, 0.6}`. `γ=0.5` es el centro de ese rango identificable
   → la elección más robusta (máximo margen a los bordes del intervalo de fit).
2. **Simetría exacta con el sueño.** El consolidado v1 usa `β` para el sueño y deja `β ∈ {0.5, 0.6}`.
   `γ=0.5` pone a la sobriedad en el **mismo régimen** que el sueño: el modulador y las actividades
   pesan parejo dentro de su capa. Que sean iguales (`β=γ`) no es obligatorio, pero es el default
   limpio: "el modulador vale tanto como las prácticas de su capa".
3. **Es lo que produce el comportamiento que el plan pidió.** El plan describe el "olor": *racha
   limpia + actividad de Conducta floja → caés a Atención porque la racha no cuenta y el peso
   amplifica la flojera.* Con `γ=0.5` la racha SÍ cuenta: en SB9 (Conducta-ancla=0.25, limpia 6m)
   el valor de Conducta pasa de **0.25** (v1, la racha no aporta) a **0.625** (`0.5·1.0 + 0.5·0.25`),
   y el caso sube de **A → En marcha** — el flip que el plan anticipó.

`γ` no puede ser cualquier cosa: a `γ=0.7` la racha domina tanto que SB9 treparía a **Plenitud**
(S=0.8875), lo cual sería demasiado; a `γ=0.3` SB9 ya está en EM pero el resto del fit empieza a
degradarse. `γ=0.5` es el punto donde la racha cuenta como un logro real **sin** ahogar a las
actividades de la capa.

**Multiplicadores acompañantes (sin cambios estructurales):** el set robusto del fit de B es
`β=0.6, γ=0.5, s_bad=0.3, k_sleep=1.0, k_sobr=2, r_rel=0.4, r_unm=0.4, p_sop=0.25, sop_form=lin,
cortes 0.43/0.64/0.88`. Todos dentro de los rangos que v1 ya declaró libres (sub-identificados);
B no inventa números nuevos, solo agrega `γ` y lo pincha en 0.5.

---

## 4. Verificación contra las 45 marcas

Script descartable: `/tmp/complement_B_fit.py` (réplica de la estructura de `weight_model_fit_v2.py`
+ el delta de Conducta + la dimensión `γ` en el grid). **No se modificó `scripts/scoring/`.**

### 4.1 Resultado

| Corrida | Hits | Detalle |
|---|---|---|
| **A — 45 tal cual** (SB9 espera A) | **44/45** | Único fallo: **SB9 (A → EM)**. Margen mínimo 0.0133. 3348 sets dan 44/45. |
| **B — 45 con SB9 re-marcado a EM** | **45/45** | Limpio, robusto. Mismo set de parámetros. `γ ∈ {0.4,0.5,0.6}`. |

**Bajo la estructura nueva, el ÚNICO caso que flipea es SB9** (verificado contra las 45 marcas
originales, no solo las de sobriedad). Las otras 9 marcas de sobriedad (SB1–SB8, SB10) se reproducen
exactas, y las 35 no-sobriedad (BP, SU, SO, IN) quedan **idénticas** —el delta solo toca Conducta
con sobriedad activa, así que no podía mover nada más—.

### 4.2 Por qué flipea SB9 (y por qué está BIEN)

SB9 = Interior 100%, Cuerpo 100%, **Conducta-ancla 25%**, racha **limpia 6 meses**. El dueño lo
marcó **A** bajo el modelo viejo, con esta lógica explícita (dataset, lote SBR): *"Con sobriedad
activa Conducta pesa MÁS → su flojera arrastra más"*. Eso era cierto en v1 **porque la racha no
sumaba**: el valor de Conducta era 0.25 a secas, y el `×k_sobr` amplificaba esa flojera.

Tras B, la racha limpia de 6 meses **sí es un logro** que entra al valor:

```
v1:   valor_Conducta = 0.25                    → S = 0.625  → A
B :   valor_Conducta = 0.5·1.0 + 0.5·0.25 = 0.625 → S = 0.8125 → En marcha
```

Es una **consecuencia directa y deseada del caso 1**: si decidimos que la racha limpia suma al
valor (simétrico al sueño), entonces un usuario que sostuvo 6 meses limpio **no puede quedar en
Atención** solo porque una actividad de Conducta esté floja — su práctica central de esa capa
(mantenerse limpio) está intacta y al 100%. Es exactamente el "olor" que el plan pidió eliminar.

Comparación de simetría que lo confirma: **SU8** (Cuerpo-ancla Caminar al 50%, sueño ok) → el dueño
marcó **P/EM**, no A, *porque el sueño sostiene Cuerpo aunque el ejercicio se caiga*. SB9 es el
gemelo de Conducta: la sobriedad limpia debe sostener Conducta aunque la actividad se caiga. v1
trataba estos dos casos asimétricamente; B los alinea.

### 4.3 ¿El dueño debería re-marcar? — honestidad total

**Sí, SB9 debería re-marcarse de A a En marcha.** No fuerzo ninguna regla para tapar el cambio
(eso violaría la base pura). El flip NO es un bug: es la consecuencia lógica de la decisión de
diseño del caso 1. Hay dos lecturas posibles y son mutuamente excluyentes:

- **Si el dueño acepta que la racha limpia es un logro que suma** (caso 1) → SB9 = En marcha es
  correcto, y el modelo reproduce **45/45**.
- **Si el dueño insiste en que SB9 = Atención** → entonces la racha limpia NO debe sumar al valor,
  y estaríamos de vuelta en v1 (`γ=0`), descartando el caso 1 entero. No hay punto intermedio: con
  `γ>0` la racha de 6 meses inevitablemente rescata SB9 de Atención.

Mi recomendación: **re-marcar SB9 → En marcha.** Es coherente con SU8 (el sueño sostiene Cuerpo) y
con la intención del plan. Pero la decisión es del dueño —es su única marca afectada, y el modelo
queda 45/45 en cuanto la confirme—.

### 4.4 Caso 2 verificado (capa sin anclas)

Con el óptimo de B, la capa modulada sin anclas cae donde el dominio espera:

| Config (otras 2 capas al 100%) | valor capa | Score | Banda |
|---|---|---|---|
| Conducta solo-sobriedad, **limpia** | 1.000 | 1.0000 | **P** |
| Conducta solo-sobriedad, **recaída** | 0.400 | 0.7000 | **EM** |
| Conducta solo-sobriedad, **sin marcar** | 0.400 | 0.7000 | **EM** |
| Cuerpo solo-sueño, **ok** | 1.000 | 1.0000 | **P** |
| Cuerpo solo-sueño, **mal** | 0.300 | 0.7667 | **EM** |
| Cuerpo solo-sueño, **none** | 0.300 | 0.7667 | **EM** |

Coherente con el resto: el modulador bueno → P, el modulador roto/ausente → EM (mismo escalón que
una recaída o un sueño malo bajan en las capas CON anclas). Sin reglas, sin división por cero.

---

## 5. Tradeoffs y riesgos

- **🟡 SB9 obliga a una re-marca (1 sola).** Es el costo honesto del caso 1. No es evitable sin
  matar el caso 1. Si el dueño la rechaza, hay que volver a discutir si la racha limpia suma o no.
  Lo dejo explícito arriba en vez de esconderlo con una regla.
- **🟡 `γ` agrega un parámetro libre nuevo.** El sistema ya estaba sub-identificado (~3000 sets dan
  45/45 en v1); `γ` no agrava eso de forma crítica (queda pinchado en {0.4,0.5,0.6}), pero es una
  perilla más a calibrar el día del release. Mitigación: atarlo a `β` (`γ=β`) reduce el espacio sin
  perder ajuste, y tiene sentido conceptual ("el modulador pesa lo mismo en su capa, sea sueño o
  sobriedad").
- **🟡 Interacción sueño + sobriedad sigue ABIERTA (heredado de v1 §6).** B no la cierra: con ambas
  activas, `señal_sobr=1` y `señal_sueño` malo interactúan vía la renormalización de pesos, y NO hay
  marcas del dueño para ese cruce. B mantiene la asunción lineal de v1; **falta ≥1 marca** con
  sueño+sobriedad juntos para pinchar el comportamiento. No lo invento.
- **🟢 El caso 2 no tiene riesgo de borde.** El límite `#anclas→0` es continuo con el blend; no hay
  salto de comportamiento entre "1 ancla" y "0 anclas". Es robusto.
- **🟢 Cero reglas nuevas.** Todo el delta vive dentro del cálculo del VALOR de la capa (el blend) y
  del límite del promedio. El motor sigue siendo `score = Σ peso×valor`; no se agregó ningún
  cap/gate/worst-term. La base pura queda intacta.
- **🔵 Recomendación de merge:** adoptar el blend simétrico (`γ=0.5`, o `γ=β` atado), confirmar la
  re-marca de SB9→EM, y dejar registrada la interacción sueño+sobriedad como el único hueco vivo
  que falta pinchar antes de congelar números.
