> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo de scoring — Complemento A (3 casos nuevos)

> **Estado: borrador para aprobar.** COMPLEMENTA (no rehace) `modelo-consolidado-v1.md`.
> Trabajo independiente (opus A). Fecha: 2026-06-08.
> Base cerrada: Forma A, motor de pesos puros (`score = Σ peso×valor`), cero reglas/caps/gates/
> worst-term, bandas R/A/EM/P, gate Inquebrantable ≥2 capas, soportes asimétricos, tasks neutras.
> **Todo lo de v1 queda igual.** Acá solo se agrega/ajusta para los 3 casos del plan.
> Verificación: script descartable en `/tmp/complemento_a_fit.py` (NO toca `scripts/scoring/`).

---

## 1. Qué cambia respecto al consolidado v1 (delta claro)

**Una sola cosa cambia en la fórmula:** la forma del **valor de Conducta** cuando hay sobriedad
activa. Hoy había una **asimetría con el sueño** que el plan marca como "olor":

| Capa modulada | Hoy (v1) — valor de la capa | Simetría rota |
|---|---|---|
| **Cuerpo** + sueño | `β·sueño + (1−β)·promedio(anclas)` — el sueño SUMA al valor | — |
| **Conducta** + sobriedad | `clean → promedio(anclas)` — la racha **NO suma**, solo modula peso | ⬅️ rota |

En v1, la racha limpia **no aportaba nada al valor**: solo subía el peso de Conducta (`k_sobr`).
Consecuencia indeseada (el olor del plan): una **racha limpia de 6 meses + una ancla de Conducta
floja** caía a Atención (SB9), porque la racha no contaba y el peso amplificaba la flojera del ancla.

**Delta A:** la **racha limpia entra al valor de Conducta como logro** (igual que el sueño en Cuerpo),
y la **recaída lo hunde**. Es un cambio de **simetría**, no una regla nueva: el peso sigue siendo el
único mecanismo del motor; lo único que se toca es de qué se compone el `valor` de una capa modulada.

**Lo que NO cambia** (queda idéntico a v1):
- Pesos Forma A (multiplicador + renormalización), `k_sleep`, `k_sobr`.
- Bandas `R<0.40 · A<0.62/0.64 · EM<0.85/0.86 · P≥0.85/0.86`.
- Gate Inquebrantable (≥2 capas), soportes asimétricos/lineales, tasks neutras.
- El sueño en Cuerpo (su `β` y su `s_bad`), la base pura (ω=0, capas parejas).
- El **largo** de la racha sigue siendo BINARIO para el estado (clean=clean, da igual 5 días o 6 meses;
  el largo se premia en la feature de sobriedad, no en el score). Lo que entra al valor es el **estado**
  de la racha (limpia / rota / sin-marcar), no su longitud.

---

## 2. Fórmula nueva del valor de Conducta + el caso sin-actividades

### 2.1 Valor de Conducta con sobriedad activa (CASO 1)

Sea `a = promedio(anclas de Conducta)` (en `[0,1]`) y `m` el **valor del estado de sobriedad**:

```
m = 1.0          si la racha está LIMPIA   (clean)   → logro pleno
m = r_rel_streak si hubo RECAÍDA           (relapse) → logro perdido (hunde)
m = r_unm_streak si está SIN MARCAR        (unmarked)→ ventana de perdón (sin dato)
```

El valor de la capa es el **blend simétrico al sueño**:

```
valor(Conducta) = γ · m  +  (1 − γ) · a
```

- **`γ`** = cuánto pesa la racha (logro de sobriedad) dentro del valor de Conducta. Es el análogo de
  `β` (sueño dentro de Cuerpo).
- Si `clean` (m=1): `valor = γ + (1−γ)·a` → la racha **levanta** el valor del ancla flojo, pero sin
  taparlo del todo (el ancla sigue pesando `1−γ`).
- Si `relapse` (m=`r_rel_streak`≈0.1): `valor = γ·0.1 + (1−γ)·a` → la racha **hunde** el valor por
  debajo del ancla, y como Conducta pesa más (`k_sobr`), el golpe se **amplifica** en el score. La
  recaída "pega a todo el Score" EMERGE de peso×valor — sin regla, igual que en v1.
- Si `unmarked` (m=`r_unm_streak`≈0.4): valor intermedio → con anclas 100% topea en EM (ventana perdón),
  exactamente como el sueño no-registrado.

### 2.2 Caso modulado SIN actividades regulares (CASO 2)

Si una capa modulada **no tiene anclas** (`a` indefinido), el blend colapsa a su modulador
(equivale a `γ=1` o `β=1` efectivo): el **valor sale enteramente del modulador**.

```
valor(Conducta sin anclas)  = m       (clean=1.0 · relapse=r_rel_streak · unmarked=r_unm_streak)
valor(Cuerpo sin anclas)    = sv      (ok=1.0 · mal/none=s_bad)
```

El **peso** de esa capa se calcula igual que siempre (sigue recibiendo `k_sobr` / `k_sleep`): la capa
no desaparece del agregado, solo su valor pasa a ser 100% modulador. Esto es lo correcto en el dominio:
si elegiste Conducta = solo sobriedad, tu "conducta de la semana" ES tu sobriedad; no hay con qué
promediar. El modelo deja de romperse (división por cero / promedio vacío) y degrada con sentido.

> **Regla de implementación**: tratar "capa modulada con 0 anclas activas" como `blend = modulador puro`.
> No es un caso especial del *estado* (no hay banda nueva): es la degeneración natural del mismo blend
> cuando uno de sus dos términos no existe.

---

## 3. El blend exacto (γ) y su justificación

**Elección: `γ = 0.4`** (con `0.5` como alternativa viable; ver identificabilidad abajo).

### Por qué 0.4 — barrido sobre las 45 marcas (manteniendo el resto del óptimo fijo)

| γ | reproduce | SB9 (esperado A) | lectura |
|---|---|---|---|
| 0.2 | 40/45 | EM (S=0.64) | racha casi no cuenta → rompe otros SB; vuelve a la asimetría de v1 |
| 0.3 | 43/45 | EM (S=0.685) | racha cuenta poco; aún rompe 2 |
| **0.4** | **44/45** | **EM (S=0.73)** | **máximo fit; SB9 es el único flip (esperado)** |
| 0.5 | 42/45 | EM (S=0.775) | racha pesa fuerte; empuja 2 casos sobre el corte |
| 0.6 | 42/45 | EM (S=0.82) | idem, más arriba |
| 0.7 | 39/45 | P (S=0.865) | la racha tapa el ancla flojo → SB9 sube hasta P (demasiado) |

**Justificación de diseño** (no solo del fit): `γ=0.4` significa que **el ancla regular de Conducta
sigue mandando levemente** (pesa `0.6`) mientras **la racha aporta un piso de logro** (`0.4`). Es la
contraparte natural del sueño con `β=0.5` en Cuerpo: el modulador es importante pero no monopoliza el
valor de la capa cuando hay actividades que medir. Mantenerlo **por debajo de 0.5** preserva el
principio del dueño "constancia = esqueleto, superávit/logro = músculo": el logro de sobriedad
**levanta**, no **reemplaza**, a la conducta del día a día.

### Parámetros nuevos (valores y rango identificado)

Óptimo más robusto (44/45, margen mínimo al corte = 0.0029):

```
γ            = 0.4    (libre en {0.4, 0.5})
r_rel_streak = 0.1    (libre en {0.1, 0.2, 0.3} — racha tras recaída: hunde)
r_unm_streak = 0.4    (PINNED — ventana de perdón, sin dato)
k_sobr       = 3      (libre en {2, 2.5, 3} — heredado de v1, sin cambios)
```

Resto del óptimo (idéntico a v1, sin tocar): `β=0.5, s_bad=0.25, k_sleep=1.5, p_sop=0.2, b_sop=0,
sop_form=lin, cRA=0.40, cAEM=0.64, cEMP=0.86`.

Restricción de validez impuesta: `r_rel_streak < r_unm_streak < 1` (recaída vale menos que sin-marcar,
que vale menos que limpia) — orden de dominio, no parche.

---

## 4. Verificación contra las 45 marcas (CASO 3)

Script descartable: `/tmp/complemento_a_fit.py` (copia los 45 `CASES` de `weight_model_fit_v2.py`
verbatim; NO modifica `scripts/scoring/`).

### Resultado: **44/45 reproduce. 1 flip: SB9 (A → En marcha).**

```
SB1  P→P    SB2  P→P    SB3  P→P     (limpia, anclas 100% → P, sin cambio)
SB4  EM→EM  SB5  EM→EM  SB6  EM→EM   (recaída → EM, golpe plano, sin cambio)
SB7  A→A    SB8  A→A                 (50% + limpia/recaída → A floor, sin cambio)
SB9  A→EM   <-- FLIP                 (Co-ancla 25% + racha limpia 6m)
SB10 EM→EM                          (sin marcar → ventana perdón → EM, sin cambio)
```

Todos los demás lotes (BP, SU, SO, IN) quedan **idénticos** — el cambio solo toca casos con sobriedad
activa, y dentro de esos, solo SB9 cruza un corte.

### Por qué SB9 flipea — y es CORRECTO, no un bug

SB9 = `{I:1.0, Cu:1.0, Co_ancla:0.25}`, sobriedad **limpia 6 meses**. Con `k_sobr=3`, los pesos
normalizados son `I=0.2, Cu=0.2, Co=0.6`.

- **v1 (racha no cuenta):** `valor(Co) = 0.25`. Score = `0.2·1 + 0.2·1 + 0.6·0.25 = 0.55` → **A**.
- **Complemento A (γ=0.4, racha cuenta):** `valor(Co) = 0.4·1.0 + 0.6·0.25 = 0.55`.
  Score = `0.2·1 + 0.2·1 + 0.6·0.55 = 0.73` → **EM**.

El flip es el **efecto explícitamente buscado** por el plan (Caso 1): el dueño marcó SB9=A **bajo la
estructura vieja**, donde "6 meses limpio impecable" no valía nada para el estado y solo amplificaba el
ancla floja. Con la simetría nueva, mantener una racha de 6 meses **es un logro que cuenta**, así que
una persona limpia con una sola conducta floja ya no es "Atención": está **En marcha**. Coincide con la
intuición del propio plan ("una racha limpia + actividad de Conducta floja = caés a Atención" era el olor).

### Recomendación de re-marca (honestidad total)

> **SB9 debería re-marcarse a En marcha.** No se puede mantener A bajo la estructura simétrica sin
> reintroducir la asimetría (γ≤0.2), lo que rompe otros 5 casos del lote SBR. El dueño tiene que
> confirmar la nueva semántica: *"¿una racha limpia larga rescata una conducta floja medio escalón?"*
> Bajo el Caso 1 del plan, la respuesta del modelo es SÍ. Si el dueño insiste en A, entonces la
> sobriedad **no** debe entrar al valor (se vuelve a v1) — son mutuamente excluyentes. No hay forma de
> tener ambas; sería un parche-regla, prohibido.

### Predicción para el Caso 2 (sin veredicto del dueño aún — son casos nuevos)

| caso | config | pred | S | lectura |
|---|---|---|---|---|
| Conducta solo-sobriedad, limpia | `{I:1, Cu:1, Co:∅}` clean | **P** | 1.00 | racha limpia = capa plena |
| Conducta solo-sobriedad, recaída | `{I:1, Cu:1, Co:∅}` relapse | **A** | 0.46 | recaída hunde la capa pesada → A |
| Conducta solo-sobriedad, sin marcar | `{I:1, Cu:1, Co:∅}` unmarked | **EM** | 0.64 | ventana perdón → EM |
| Cuerpo solo-sueño, ok | `{I:1, Cu:∅, Co:1}` ok | **P** | 1.00 | sueño bueno = capa plena |
| Cuerpo solo-sueño, mal | `{I:1, Cu:∅, Co:1}` mal | **EM** | 0.68 | sueño malo topea EM |
| Cuerpo solo-sueño, sin registrar | `{I:1, Cu:∅, Co:1}` none | **EM** | 0.68 | = mal (coherente con SU2=SU3) |

Estos valores son **coherentes** con los patrones ya marcados (recaída ≈ un escalón abajo, sueño
mal/ausente topea EM). Pero **falta el veredicto del dueño** para fijarlos: el plan los pide como
casos nuevos. **Recomendación: marcar ≥1 de cada** para pinchar `r_rel_streak` y `r_unm_streak` (hoy
solo `r_unm_streak=0.4` queda PINNED; `r_rel_streak` tiene juego en `{0.1,0.2,0.3}`).

---

## 5. Tradeoffs y riesgos

1. **SB9 cambia de estado — y es irreversible bajo simetría.** El precio de meter la sobriedad al valor
   es perder la marca SB9=A. No es negociable con un γ intermedio: o la racha cuenta (SB9→EM) o no
   (vuelve v1). **Riesgo bajo, decisión del dueño**: el flip va EN LA DIRECCIÓN que el plan pidió.

2. **`r_rel_streak` poco identificado** (`{0.1,0.2,0.3}` dan 44/45). Las 45 marcas no distinguen cuánto
   hunde la racha rota porque SB4/5/6 ya están al techo de anclas (cualquier hundimiento da EM). Para
   pincharlo hace falta una marca de **recaída + anclas flojas** (¿recaída al 75% global → A o EM?).

3. **`γ` con leve juego** (`{0.4, 0.5}`). Centrado en 0.4 por el principio "logro levanta, no reemplaza"
   y porque 0.4 maximiza fit. Si el dueño quiere que la racha rescate MÁS (que una racha impecable casi
   tape una conducta floja), sube a 0.5; si quiere que la conducta diaria mande más, hay que aceptar que
   SB9 vuelve a A y se pierde la simetría. **Centrado, sub-identificado — igual que v1 §6.**

4. **Caso 2 sin validar (CERO marcas).** Las 6 predicciones de capa-sin-anclas son consistentes pero no
   marcadas. Riesgo: el dueño podría querer que "Conducta = solo sobriedad limpia" NO llegue a Plenitud
   por sí sola (hoy da P). Es defendible (si tu única conducta elegida es no recaer y no recaés, esa
   capa está plena), pero **es una decisión de producto que falta**.

5. **Interacción sueño+sobriedad sigue ABIERTA** (heredado de v1 §6, no lo resuelve este complemento):
   con ambos moduladores activos, *sueño malo + sobriedad limpia* aún cae en Plenitud. El Caso 1 no
   lo toca; sigue necesitando ≥1 marca del dueño. **No inventé nada acá.**

6. **Compatibilidad hacia atrás del esquema:** meter la racha al valor NO agrega entidades Room ni cambia
   hechos almacenados — el dato de racha (`AbstinenceTrackEntity`) ya existe; solo cambia cómo el dominio
   puro lo combina dentro de `layer_val`. Cero impacto en persistencia/migraciones (Camino A intacto).

---

## Apéndice — diff mínimo sobre el motor (pseudocódigo)

```python
# layer_val — SOLO cambia la rama de Conducta-con-sobriedad y el guard de "sin anclas".
def layer_val(L, ancla, sleep, sobriety, support, p):
    has_anchor = ancla is not None
    if L == "Cu" and sleep != "off":
        sv = 1.0 if sleep == "ok" else p["s_bad"]
        core = (p["beta"]*sv + (1-p["beta"])*ancla) if has_anchor else sv      # CASO 2
    elif L == "Co" and sobriety != "off":
        m = {"clean":1.0, "relapse":p["r_rel_streak"], "unmarked":p["r_unm_streak"]}[sobriety]
        core = (p["gamma"]*m + (1-p["gamma"])*ancla) if has_anchor else m       # CASO 1 + CASO 2
    else:
        core = ancla if has_anchor else 0.0
    # ... soporte asimétrico/lineal idéntico a v1 ...
    return min(1.0, max(0.0, core))

# weights() — SIN CAMBIOS. La capa sin anclas sigue recibiendo k_sobr / k_sleep.
```
