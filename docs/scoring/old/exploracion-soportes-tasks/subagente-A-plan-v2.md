# Subagente A — Plan v2: SOPORTES y TASKS como ENTRADAS VIRTUALES al motor v4

> Sesgo OPUS A: **"surplus virtual / reusar las fórmulas cerradas".** Mínima cirugía sobre v4.
> Soporte y task se expresan como dos entradas que pasan por las **mismas** fórmulas ya cerradas
> (saturación exponencial + gate `base²`). El lector debe ver "el mismo motor v4 con dos entradas más".
> Verificación real abajo (`subagente_A_v2.py`, motor v4 verbatim). Proyecto Engram: `apk-personal`.

---

## 1. Filosofía

El motor v4 ya resuelve TODO lo difícil: la base saturada en [0,1], el superhabit con techo 0.5,
el gate `base²` y la agregación con pesos iguales que hace `Sol=Tin`. En vez de inventar mecanismos
nuevos para soporte y task —que arriesgan romper esos candados—, los expreso como **entradas virtuales**
que reusan esas mismas fórmulas. La **task** es un *surplus virtual* que pasa por la **misma forma de
saturación exponencial** y el **mismo gate `base²`** que el superhabit de anclas (solo cambia su techo,
mucho más chico que 0.5). El **soporte** es una *señal de base* que ajusta el `base` de la capa antes
del gate, de forma aditiva, bidireccional y leve. Resultado: cero cirugía sobre `R`, cero riesgo para
los opt-ins, y consistencia matemática garantizada **por construcción** (no por casualidad numérica).

---

## 2. Axiomas

### SOPORTES (S1–S6)
- **S1 — Mecanismo propio, en la base de SU capa.** El soporte no es opt-in (no usa el término-sombra
  global `BETA·N·(1−M)`). Entra como un componente del `base_capa`, dentro del valor de su capa.
- **S2 — Solo base, sin superhabit.** El soporte nunca aporta al `extra`. Su techo natural es 1
  (la base está topada en 1). No hay "superhabit de soporte".
- **S3 — Bidireccional leve, neutral a la par de las anclas.** Sostener el soporte sube un toque la
  base; descuidarlo la baja un toque; si el soporte va a la par de las anclas, aporte ≈ 0.
  Formalmente: `nudge = EPS_S·(G_s − base_anc)`, con `EPS_S` pequeño (ANCLAS ≫ SOPORTES).
- **S4 — Ventana indulgente por soporte.** Señal por soporte `s_i = min(días_cumplidos / WIN, 1)`,
  `WIN=4` (con 4 días ya estás al 100%; medir 7 es abusivo).
- **S5 — Señal de bloque NO crece con la cantidad.** Señal del bloque `G_s = promedio(s_i)` (promedio,
  no suma). 1 o 5 soportes con el mismo cumplimiento → mismo `G_s` → mismo aporte.
- **S6 — Capa solo-soportes: la señal ES la base.** Si la capa no tiene anclas pero sí soportes,
  `base_eff = G_s` (y la capa pesa menos por el axiom de peso, ver L1).

### TASKS (T1–T7)
- **T1 — En el EXTRA de su capa, DENTRO de la curva.** La task aporta solo al `extra` (superhabit) y
  pasa por la **misma saturación exponencial** y el **mismo gate `base²`** de v4. Nunca suma crudo por fuera.
- **T2 — Techo propio `TASK_CAP` ≪ smax=0.5.** El extra-task de una capa nunca supera `TASK_CAP`.
  Despejado del axioma de banda (T-techo, ver §3): cumplir-justo + tasks ∞ debe quedar en PLENITUD,
  jamás INQUEBRANTABLE → `TASK_CAP=0.09`.
- **T3 — Saturación por cantidad.** La 1ª task vale mucho, la 10ª casi nada
  (`S_task = TASK_CAP·(1−exp(−n/S0_TASK))`). `S0_TASK` calibrado para ~3 tasks = 90% del techo.
- **T4 — Gate `base²` compartido.** El extra-task se multiplica por `base_eff²`: sin cimiento, la
  task casi no aporta (igual que el superhabit de anclas).
- **T5 — Aporte parejo (no más fuerte cerca del umbral).** `S_task` no depende del estado ni de la
  distancia a un umbral. Es lo bastante grande para **arañar UN cruce cuando ya estás cerca de un
  borde**, pero no fabrica estados de la nada.
- **T6 — Diario y efímero.** `n = tasks completadas HOY`. Mañana `n=0` → el empujón desaparece.
  Encaja el pulso diario en el motor semanal sin contaminar la historia: el extra-task no se persiste.
- **T7 — Nunca resta; sin capa = no aporta.** Task con capa → suma a esa capa. Task neutral/sin capa
  → `n_capa` no la cuenta. El usuario solo elige la capa, no el peso.

### PESO DE CAPA (L1)
- **L1 — Capa con menos sustancia pesa menos (continuo).** `w_capa = RHO + (1−RHO)·dens_anclas`,
  donde `dens_anclas = min(anclas_presentes / anclas_esperadas, 1)` y `RHO` es el piso de peso de una
  capa sin anclas. Despejado del axioma: una capa solo-soportes no debe impactar el global como una con
  anclas. `RHO=0.4` (una capa solo-soportes pesa el 40% de una capa con anclas; continuo, sin gate duro).

---

## 3. Fórmulas con parámetros despejados de axiomas (no a dedo)

```
# ---- SOPORTE (entra a la base, via componente virtual) ----
s_i        = min(dias_cumplidos_i / WIN, 1)          # WIN=4 (S4)
G_s        = promedio(s_i)                            # bloque, NO crece con cantidad (S5)
base_eff   = clamp( base_anc + EPS_S·(G_s − base_anc), 0, 1 )   # bidireccional leve (S3)
           = G_s   si la capa no tiene anclas         # capa solo-soportes (S6)

# ---- EXTRA de anclas (curva v4 VERBATIM) ----
S_anc      = smax·(1 − exp(−surplus_anc / s0))        # smax=0.5, s0=0.5  (NO se toca)
extra_anc  = base_eff² · S_anc                        # gate base² v4

# ---- TASK (surplus virtual: MISMA forma de saturación + MISMO gate, techo propio) ----
S_task     = TASK_CAP·(1 − exp(−n / S0_TASK))         # n = tasks de HOY (T1,T3,T6)
extra_task = base_eff² · S_task                        # MISMO gate base² (T4)

valor_capa = min(base_eff, 1) + extra_anc + extra_task
w_capa     = RHO + (1−RHO)·dens_anclas                 # peso reducido (L1)
score      = Σ(valor_capa·w_capa) / Σ(w_capa)
```

### Despeje de `EPS_S` (axioma S-mag → magnitud, no a dedo)
Axioma: con anclas a medias (`base_anc=0.5`), un soporte llevado a un extremo (sostenido `G_s=1` /
abandonado `G_s=0`) produce un swing **leve** de ±0.05 en la base (un toque, no un salto). El nudge es
`EPS_S·(G_s−base_anc)`; en `base_anc=0.5` el gap extremo es ±0.5, así que `EPS_S·0.5 = 0.05` →
**`EPS_S = 0.10`**. Esto garantiza ANCLAS ≫ SOPORTES (verificado en (h): impacto soporte ≈ 1/10 del de ancla).

### Despeje de `TASK_CAP` y `S0_TASK` (axioma T-techo → magnitud, no a dedo)
Axioma de banda: si **todas** las capas están en cumplir-justo (`base=1`, `extra_anc=0`) y el usuario
hace **infinitas** tasks, el score no puede entrar a INQUEBRANTABLE (`≥1.10`); debe quedar en PLENITUD.
Con `base=1` y gate `base²=1`, score = `1 + extra_task_∞ = 1 + TASK_CAP`. Para `1 + TASK_CAP < 1.10`
con margen → **`TASK_CAP = 0.09`** (confirma el "~0.1" orientativo del dueño, pero **derivado** de la
banda, no impuesto). `S0_TASK = 3/ln(10) ≈ 1.303` se despeja del axioma de saturación T3 (3 tasks =
90% del techo: `0.9 = 1−exp(−3/S0_TASK)`).

### Despeje de `RHO` (axioma de masa de capa)
Axioma: una capa solo-soportes tiene "media sustancia" frente a una con anclas; debe pesar
sensiblemente menos pero no desaparecer (continuo). `RHO=0.4` la deja en 40% del peso de una capa con
anclas completa, interpolando linealmente con la densidad de anclas presentes.

---

## 4. Verificación con `python3` (resultados REALES, motor v4 verbatim)

Script: `subagente_A_v2.py`. Salida pegada tal cual:

```
Anclas ref: J(justo)=1.0000 XL=1.4323 DEF=0.7500 half(base~0.5)=0.3536
======================================================================
(a) Sol=1.096468 Tin=1.096468 empatan=True
(b) cumplir-justo=1.0000 band=PLENITUD
(c) SOPORTE bidireccional + invariante a cantidad (ancla media base~0.5):
    sin soporte        valor_capa=0.3536
    descuidado(0d)     valor_capa=0.3182
    a la par(2d)       valor_capa=0.3682
    sostenido(4d)      valor_capa=0.4182
    1 sostenido=0.4182 vs 5 sostenidos=0.4182 igual=True
    1 descuidado=0.3182 vs 5 descuidados=0.3182 igual=True
(d) PESO de capa solo-soportes < capa con anclas:
    w(con ancla)=1.000 w(solo-soportes)=0.400 menor=True
    score 3a-capa con XL=1.0965 | 3a-capa solo-soportes(plena)=1.0000
(e) TASK dentro de la curva (capa base=1=justo; techo task/capa=0.09):
      0 tasks -> extra_task=0.0000 techo_ok=True (<<0.5=True)
      1 tasks -> extra_task=0.0482 techo_ok=True (<<0.5=True)
      2 tasks -> extra_task=0.0706 techo_ok=True (<<0.5=True)
      5 tasks -> extra_task=0.0881 techo_ok=True (<<0.5=True)
     10 tasks -> extra_task=0.0900 techo_ok=True (<<0.5=True)
    100 tasks -> extra_task=0.0900 techo_ok=True (<<0.5=True)
    reset diario: hoy(5t)=1.0881 -> mañana(0t)=1.0000
    gate base²: capa base baja + 5 tasks = 0.1795 (extra castrado)
(f) TASK araña cruce cerca / no compra Inquebrantable sola:
    cerca: sin task=0.9672(PLENITUD) con 3t=0.9892(PLENITUD)
    cumplir-justo + 10t/capa = 1.0900(PLENITUD) compra_Inq=False
    borde: sin task=0.8374(EN MARCHA) con tasks=0.8683(PLENITUD) CRUCE=True
(g) anti-gate (continuidad):
    paso máx |dvalor| dtask=0.005 = 0.000345
    paso máx |dbase| dG_s=0.001 = 0.000100
(h) ANCLAS > SOPORTES > TASKS:
    Δ ANCLA(media->justo)=0.2155 | Δ SOPORTE pleno=0.0215 | Δ 1 TASK=0.0020
    orden ANCLA>SOPORTE>TASK = True
```

### Lectura de cada caso (§9 a–h)
- **(a) Sol=Tin intacto** — sin soportes/tasks, `layer_value` reproduce v4 bit a bit: 1.096468 = 1.096468. ✓
- **(b) cumplir-justo = 1.0 = PLENITUD.** ✓
- **(c) Soporte bidireccional leve + invariante a cantidad** — abandonado baja (−0.035), a la par
  neutro (+0.015 residual por base_anc<0.5), sostenido sube (+0.065); y 1 vs 5 soportes dan idéntico
  valor. ✓
- **(d) Capa solo-soportes pesa menos** — peso 0.4 < 1.0; una 3ª capa solo-soportes plena (score 1.0)
  no infla como una con XL (score 1.0965). ✓
- **(e) Tasks dentro de la curva** — techo 0.09 ≪ 0.5; 100 tasks saturan en 0.09; gate `base²` castra
  el extra cuando la base es baja; reset diario (1.0881 hoy → 1.0 mañana). ✓
- **(f) Arañan UN cruce / no fabrican estado** — en un borde real EN MARCHA(0.8374) las tasks cruzan a
  PLENITUD(0.8683); pero cumplir-justo + 10t/capa = 1.0900 = PLENITUD, **nunca compra INQUEBRANTABLE**. ✓
- **(g) Anti-gate (continuidad)** — paso máximo |Δvalor| ≈ 0.0003 con dtask=0.005, |Δbase| = 0.0001
  con dG_s=0.001: todo continuo y diferenciable, sin saltos. ✓
- **(h) ANCLAS > SOPORTES > TASKS** — Δ por mejorar ancla (0.2155) ≫ Δ soporte pleno (0.0215) ≫
  Δ 1 task (0.0020). Orden estricto. ✓

---

## 5. Riesgos / lo que queda abierto

- **Saturación "compartida" vs "propia".** El set-prompt pide que la task "comparta la saturación con
  el superhabit de anclas". Mi diseño comparte la **forma** (misma exponencial + mismo gate `base²`)
  pero con **techo propio** `TASK_CAP=0.09`. Sumar la task al **mismo** `surplus` antes de un único
  `S=0.5(1−exp(...))` (literal del sesgo) rompe el techo: 10 tasks llevarían el extra a ~0.5 y
  cumplir-justo a INQUEBRANTABLE (lo verifiqué; falla §9f). La interpretación correcta del candado es
  techo task ≪ techo ancla → término saturado aparte. Es la mínima cirugía que respeta TODOS los candados.
- **`base_eff` media < 0.5.** El ancla "media" `[15,15,15,15]` da base 0.354 (concavidad γ=1.5), no 0.5.
  El nudge de soporte sigue siendo leve y bidireccional, pero el "neutro exacto" del axioma S3 ocurre
  cuando `G_s = base_anc`, que en una capa real puede no caer en 0.5. Es coherente con S3, solo que el
  punto neutro es móvil. Si el dueño quiere neutro fijo (G_s=0.5), basta cambiar el ancla del axioma.
- **Soporte en capa con anclas perfectas.** Con `base_anc=1` y soporte abandonado (`G_s=0`), el nudge
  es −EPS_S=−0.10 (el mayor castigo). Es el comportamiento bidireccional pedido, pero conviene que el
  dueño valide que −0.10 a una capa perfecta es "leve" y no excesivo. Atenuarlo por `(1−base_anc)` lo
  suaviza si se prefiere.
- **`RHO` no atado a una historia marcada.** `RHO=0.4` sale de un axioma cualitativo ("media
  sustancia"), no de un dataset de decisiones. Calibrable cuando haya historias del dueño sobre
  capas solo-soportes.
- **Interacción soporte+task acumulados** no se barrió exhaustivamente (ambos pasan por `base_eff`, así
  que son consistentes, pero falta un test cruzado de capa con anclas+soportes+tasks simultáneos).

---

## En qué diverge mi enfoque (A vs B, C)
A = **reusar las fórmulas cerradas**: una sola función de saturación y un solo gate `base²` para todo.
Soporte = nudge aditivo a la base; task = término saturado con la misma forma y techo propio. No
introduce blends convexos (B) ni razonamiento de presupuesto/headroom (C): solo dos entradas más al
motor v4 existente. Máxima trazabilidad: cualquiera que entienda v4 entiende esto sin teoría nueva.
