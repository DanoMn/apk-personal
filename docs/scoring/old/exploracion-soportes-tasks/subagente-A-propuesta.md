# Subagente A — SOPORTES y TASKS en el scoring (puristas del motor / tasks cosméticas)

> **Estado: propuesta divergente (1 de 3).** Modela cómo entran SOPORTES y TASKS al motor v4
> cerrado, con el sesgo del Subagente A. No toca ancla (A1–A10) ni opt-ins (O1–O13).
> Fecha: 2026-06-15. Proyecto Engram: `apk-personal`. Verificación: `python3` (script al final).
> Motor base reutilizado verbatim: `exploracion-valor-capa/modelo_valor_capa_v4_merge.py`.

---

## 1. Filosofía

El motor v4 ya está cerrado y es bueno: el **canal base** dice "¿está en pie?" y el **canal extra
(superhabit)** dice "¿se destacó?", y *solo las anclas* pueden destacarse (O1). Mi tesis purista es:
**no abrir un canal nuevo para soportes ni tasks, ni darles poder para mover el eje semántico.** Un
soporte es **cimiento, no gloria**: estructuralmente es un *opt-in atenuado* — invisible cuando se
sostiene, baja **poquísimo** cuando se descuida, y **jamás** genera extra ni banda. Una task es un
**evento puntual sin recurrencia**: meterla al motor contamina el estado con ruido de "esfuerzo
puntual" que no es base ni superhabit sostenido. Por eso las tasks **no tocan el motor**: viven solo
en el **número visible 700–1000**, como una "ayuda mental" meritocrática (empujás unos puntos si ya
estás alto), sin poder comprar una banda. Resultado: `ANCLAS ≫ SOPORTES ≫ TASKS`, con tasks fuera
del estado por completo. Coherente con el método del proyecto: las magnitudes se **despejan de
axiomas de estado**, no se heredan (el ±0.1 / 0.05 viejos quedan descartados).

---

## 2. Axiomas de SOPORTES (`S1…S7`)

### S1 — El soporte solo toca la BASE, nunca el EXTRA
El soporte afecta el canal base ("¿está en pie?"), **jamás** el extra (superhabit). No genera extra
(su señal está topada en 1 por diseño). Espejo exacto de O1.
- **Por qué:** sostener la higiene/el orden mínimo es cimiento, no un logro que te destaca. La gloria
  se gana con anclas.
- **Mecanismo:** `extra_global` se calcula solo con `max(r−1,0)` de las anclas; el soporte no entra.

### S2 — Soporte SOSTENIDO = neutro exacto (incluso con anclas en déficit)
Con señal de capa `G=1` (todos los soportes sostenidos), el soporte es invisible: el estado es
idéntico a no tenerlo. Espejo de O2. **Cumplir-justo de soporte = sostenerlo como se espera = neutro.**
- **Por qué:** mantenerte aseado no debe "regalar" puntos ni mover el estado; solo evita la caída
  cuando se abandona. El neutro del soporte se ancla en `G_REF = 1.0`.
- **Mecanismo:** término-sombra de peso dinámico `w_sup = ETA·(1−G)`; con `G=1 → w=0` → desaparece.

### S3 — Soporte DESCUIDADO arrastra MUY levemente (asimetría a favor de no-premiar)
Descuidar soportes baja el estado **muy poco** (cumple "soportes restan muy levemente"). Sostener no
puede subir banda; descuidar resta un mínimo. Es **centrado en `G_REF=1`** → estructuralmente solo
puede empatar o bajar, nunca subir (igual que el anti-incentivo aceptado de O12).
- **Por qué:** el dueño: "descuidar un soporte baja apenas". No es un evento crítico (eso es la
  recaída del opt-in); es erosión leve del cimiento.
- **Mecanismo:** `w_sup = ETA·(1−G)` con `ETA` chico (≈11 % del `BETA` del opt-in).

### S4 — El soporte NO mata el valor de las anclas de su capa
Las anclas de la capa con soportes conservan su valor completo; el soporte es **término aparte** en
el promedio base, no se mezcla dentro del bloque de anclas. Espejo de O4.
- **Mecanismo:** el soporte entra como un sumando `(G, w_sup)` extra en el promedio ponderado, igual
  que el opt-in; nunca multiplica ni recorta el `anchor_base`.

### S5 — El soporte NO distorsiona el superhabit (`Sol = Tin`)
Un superhabit rinde lo mismo en cualquier capa, tenga soportes o no. El caso `Sol = Tin` se mantiene.
- **Mecanismo:** `extra_global` con **pesos iguales** (no por el peso de la capa); el soporte solo
  mueve `base_global`, que está topado en 1 y no entra al extra.

### S6 — Arrastre del soporte LOCAL a la capa (no escala con N, a diferencia del opt-in)
El peso de la sombra del soporte **no** se multiplica por `N` (`w_sup = ETA·(1−G)`, no `ETA·N·(1−G)`).
- **Por qué:** el opt-in (sueño/sobriedad) es base sensible y su arrastre debe ser **plano en N** (O3);
  el soporte es **menos importante** que el opt-in. Dejarlo local lo hace pegar *aún menos* al crecer
  N (se diluye en el promedio), reforzando `OPT-IN > SOPORTE` y `ANCLA ≫ SOPORTE`. Decisión consciente
  de jerarquía.

### S7 — Multi-soporte: una señal de capa agregada, saturada y sin tope de premio
Varios soportes de la misma capa colapsan en **una** señal `G ∈ [0,1]` (fracción media sostenida en
la ventana de 7 días, UX inversa: `G = 1 − omisiones/(7·#soportes)`). Sostener muchos **no fabrica
banda** (el neutro es `G=1`); descuidar varios compone su leve arrastre vía un único `w_sup` más
grande, pero acotado por `ETA`.
- **Mecanismo:** la capa aporta a lo sumo **un** término-sombra `(G, ETA·(1−G))`; "coleccionar"
  soportes no acumula peso por encima de `ETA`.

**Ventana de 7 días (hereda O10):** el soporte mira solo la última semana.

---

## 3. Axiomas de TASKS (`T1…T6`)

### T1 — La task NO toca el motor (estado/banda intactos)
Una task **jamás** modifica `base_global`, `extra_global` ni el `ESTADO`. La banda (Restauración…
Inquebrantable) es **idéntica** con 0 o 50 tasks hechas.
- **Por qué:** una task es un evento puntual sin recurrencia; no es base sostenida ni superhabit. El
  motor mide patrón semanal, no esfuerzos sueltos. Meterla al estado contamina el eje semántico.

### T2 — La task vive SOLO en el número visible 700–1000 ("ayuda mental")
La única huella de una task es un **delta acotado al `VisibleScore`**, como empujón motivacional.
- **Por qué:** idea del dueño — que un usuario que se esforzó pueda arañar unos puntos y *sentir* el
  empujón sin que el estado real se distorsione.

### T3 — La task NUNCA resta
Una task no hecha **no penaliza**; solo la hecha suma al push del visible. `task_push ≥ 0` siempre.
- **Mecanismo:** `n_eff` cuenta solo tasks **hechas** (y válidas, T6); las pendientes no aparecen.

### T4 — Aporte MERITOCRÁTICO (no rescata a nadie bajo) → sin injusticia
El push escala con `base_vis^q` (q>1): en base baja el push es ≈0; solo en el **tramo final** (base
alta) la task empuja de verdad. No se "compra" una banda regalada.
- **Por qué:** "ayuda mental sin injusticia". Un usuario en Restauración no salta de banda por hacer
  trámites; uno borde-Plenitud sí puede arañar unos puntos visibles.
- **Mecanismo:** `task_push = TASK_MAX · base_vis^q · (1 − e^{−K·n_eff})`.

### T5 — Multi-task SATURADO (coleccionar no fabrica un salto) y por DEBAJO de un soporte
El factor `(1 − e^{−K·n_eff})` satura: la 1.ª task aporta casi todo, las siguientes decrecen; el push
total topa en `TASK_MAX·base_vis^q`. La magnitud máxima del push (en puntos visibles) es **menor**
que lo que mueve un soporte descuidado → `SOPORTE > TASK`.
- **Mecanismo:** `TASK_MAX` se despeja para que el push máximo (≈7 pts a base 0.84) sea menor que el
  movimiento de un soporte (≈9 pts) en el mismo eje visible.

### T6 — Task NEUTRAL no suma (capa obligatoria + rol no-Neutral)
Una task sin `layerId` o con `contributionRole = Neutral` **no entra** a `n_eff` (dominio:
`nucleo-dominio-autonomia.md §Task`). "Comprar cuerdas" no empuja nada.

### Reconciliación de "ayuda mental" sin injusticia
El push de tasks es **un parche cosmético acotado, meritocrático y saturado**: solo es perceptible
cuando ya estás alto (T4), se agota rápido (T5) y nunca cruza el techo 1000 (clamp). El usuario *ve*
el empujón; el motor *no se entera*. Cero injusticia: nadie cambia de banda real por hacer tareas.

---

## 4. Fórmulas y parámetros despejados de axiomas

### 4.1 Soportes (motor, sobre el canal base)

```
G ∈ [0,1]   señal de capa = 1 − omisiones/(7 · #soportes)   (G_REF = 1 neutro)
w_sup = ETA · (1 − G)                          # local, NO escala con N (S6)
term_sup = (G, w_sup)  se suma al promedio ponderado del canal base de la capa
base_global = Σ(v·w) / Σ(w)   sobre {anclas (W0=1), opt-in (BETA·N·(1−M)), soporte (ETA·(1−G))}
extra_global = media de max(r−1,0) de anclas   # SIN cambios — soporte no entra (S1,S5)
ESTADO = min(base_global,1) + extra_global     # bandas v4 sin tocar
```

**Despeje de `ETA` (axioma de estado S3, no a dedo):** con anclas perfectas N=3 y **un** soporte
totalmente descuidado (G=0), `base = 3/(3+ETA)`, luego `DROP = 1 − 3/(3+ETA)`. Axioma del dueño:
"descuidar resta poquísimo" → fijamos `DROP_MAX = 0.03` (3 centésimas de estado). Despejando:

```
ETA = N_ref · DROP_MAX / (1 − DROP_MAX) = 3 · 0.03 / 0.97 = 0.0928
```

→ `ETA = 0.0928` ≈ **11 % del `BETA=0.818`** del opt-in. Esto materializa numéricamente
`ANCLA ≫ OPT-IN > SOPORTE`.

### 4.2 Tasks (solo eje visible, reconcilia §3.2 bajo v4)

```
base_vis = clamp(min(base_global, 1), 0, 1)         # el visible refleja la BASE (igual que §3.2)
n_eff    = #tasks hechas con layer≠null y role≠Neutral
task_push = TASK_MAX · base_vis^q · (1 − e^{−K·n_eff})    # ≥0, meritocrático, saturado
VisibleScore = 700 + round( clamp(base_vis + task_push, 0, 1) · 300 )
```

Parámetros: `TASK_MAX = 0.04`, `q = 3.0`, `K = 0.55`.
**Despeje de `TASK_MAX` (axioma T5, no a dedo):** el push máximo a base alta (0.84, borde-Plenitud)
debe ser **menor** que el movimiento de un soporte descuidado en el mismo eje visible (≈9 pts).
`push_max(0.84) = TASK_MAX·0.84^3·1 ≈ 0.0246 → round(·300) = 7 pts < 9`. `TASK_MAX=0.04` lo cumple
con margen. `q=3` mata el push en base baja (T4); `K=0.55` hace que la 1.ª task valga ~la mitad del
total (saturación T5).

---

## 5. Reconciliación de los dos ejes (§3 del set-prompt)

El hallazgo crítico: **número visible 700–1000** (§3.2, solo base recortada, topa en 1000) vs
**estado/banda** (§16-NUEVO, `base+extra`, escala [0,~1.5], Inquebrantable necesita extra de anclas).

Mi postura purista **mantiene los dos ejes separados a propósito** y NO intenta que el visible
codifique Inquebrantable:

- **Eje ESTADO (motor):** soportes entran (canal base, leve); tasks **no entran**. Inquebrantable
  sigue siendo territorio exclusivo del `extra` de anclas (O1) — soportes/tasks no pueden fabricarlo.
- **Eje VISIBLE (presentación):** `base_vis = min(base_global,1)` (el extra/superhabit no se muestra,
  igual que hoy — el techo 1000 representa "base perfecta"). Las **tasks empujan SOLO acá**, acotadas
  y meritocráticas, como ayuda mental. El visible nunca pasa 1000 (clamp).

Consecuencia honesta: un usuario Inquebrantable y uno Plenitud-justo pueden compartir visible 1000
(el visible no distingue superhabit) — pero **se distinguen por banda/estado**, que es el eje serio.
El visible es el "termómetro amable"; el estado es la verdad. Las tasks juegan en el termómetro amable
sin tocar la verdad. (Si el dueño quiere que el visible codifique Inquebrantable, ese rediseño es el
sesgo del **Subagente C**, no el mío — yo defiendo no mezclar.)

---

## 6. Verificación numérica (`python3`)

Script completo en este repo como bloque ejecutable (reusa el motor v4 verbatim). Resultados reales:

```
[DESPEJE] ETA desde DROP_MAX=0.03 (descuido total 1 soporte, anclas perfectas N=3) -> ETA=0.0928
   (compara: BETA opt-in=0.818.  ETA/BETA=0.113  -> soporte pega ~11% de un opt-in)
[TASKS] TASK_MAX=0.04 Q=3.0 K=0.55. push acotado y meritocratico (base_vis^Q).

(a) SUPERHABIT INTACTO  Sol == Tin  (soportes/tasks NO distorsionan extra)
   Sol(superhabit Interior)=1.1392  Tin(superhabit Cuerpo)=1.1392  empatan=True
   (sin soportes) Sol=1.1441 Tin=1.1441 empatan=True (== 1.1441 v4)

(b) CUMPLIR-JUSTO = ESTADO 1.0 = PLENITUD (anclas justas + soportes sostenidos G=1)
   estado=1.0000  banda=PLENITUD  ok=True
   (anclas justas SIN soportes) estado=1.0000 == cumplir-justo (True)

(c) MULTI-SOPORTE NO FABRICA BANDA (sostener no sube de banda)
   anclas parciales sin soportes=0.4648 ATENCION | +soportes perfectos=0.4648 ATENCION
   misma banda=True (soporte sostenido NO sube banda: G_REF=1 neutro)

(d) ANTI-GATE: barrido continuo de G (soporte), N=3, anclas justas
   paso maximo |dEstado| con dG=0.001 = 0.000059  -> continuo, sin gate/cap

(e) ORDEN ANCLAS > SOPORTES > TASKS (impacto maximo en estado/visible)
   |impacto ancla colapsada|   = 0.3333
   |impacto opt-in recaida|    = 0.4500
   |impacto soporte descuidado|= 0.0300
   |impacto task en ESTADO|    = 0.0000 (cero por diseno)
   ancla > soporte : True
   soporte > task(estado) : True
   visible base-justa sin task=1000  +5 tasks=1000 (push=0 pts)
   visible con soporte descuidado=991 (mueve 9 pts)  -> soporte mueve mas que tasks: True

(f) TASKS no rescatan a usuario bajo (anti-injusticia)
   base=0.30 (Restauracion  ) visible 790 -> +5 tasks 790  (push 0 pts)
   base=0.55 (Atencion      ) visible 865 -> +5 tasks 867  (push 2 pts)
   base=0.70 (En marcha     ) visible 910 -> +5 tasks 914  (push 4 pts)
   base=0.84 (borde-Plenitud) visible 952 -> +5 tasks 959  (push 7 pts)

(g) MULTI-TASK SATURADO (coleccionar no fabrica salto), base=0.84
    0 tasks -> 952 | 1 -> 955 | 2 -> 957 | 3 -> 958 | 5 -> 959 | 10 -> 959 | 50 -> 959 (push topa en 7)

(h) TASK NEUTRAL no suma (layer None o role Neutral)
   base=0.84 + 5 tasks neutras + 5 sin capa -> visible 952 == base sola 952 : True

(i) VISIBLE jamas pasa 1000 (clamp): base=1.0 + 50 tasks -> 1000 (<=1000)
```

**Lectura de los criterios obligatorios del §8:**
- (a) `Sol = Tin` ✅ — soportes/tasks no distorsionan el superhabit (también igual al 1.1441 v4 puro).
- (b) cumplir-justo = ESTADO 1.0 = Plenitud ✅ — soportes sostenidos son neutro exacto.
- (c) multi-soporte no fabrica banda ✅ — `G=1` es el neutro; sostener no sube.
- (d) anti-gate ✅ — paso máximo 0.00006 al barrer G (continuo, sin caps).
- (e) `ANCLAS(0.33) > OPT-IN(0.45)*… > SOPORTE(0.03) > TASK-en-estado(0.00)` ✅, y en el visible el
  soporte mueve 9 pts vs ≤7 de tasks. *(Nota: el opt-in pega más que una sola ancla colapsada porque
  el opt-in es deliberadamente sensible —O3—; ANCLA ≫ SOPORTE ≫ TASK se cumple, que es la jerarquía
  exigida; opt-in vs ancla individual es otra dimensión ya cerrada en O1–O13.)*

---

## 7. Riesgos / lo que queda abierto

1. **`DROP_MAX = 0.03` es la única magnitud "elegida"** (de ahí sale `ETA`). Está despejada de un
   axioma del dueño ("descuidar resta poquísimo"), pero el valor exacto (0.02 vs 0.03 vs 0.05) se
   debería pinchar con 1–2 marcas reales de historias de soportes (`historias-soportes-tasks-v1.md`).
2. **Visible no codifica Inquebrantable** (decisión consciente): un Inquebrantable y un Plenitud-justo
   comparten visible 1000. Si el dueño quiere distinguirlos en el número, hay que adoptar el rediseño
   del Subagente C — incompatible con mi postura purista.
3. **Tasks fuera del motor = no afectan estabilidad temporal multi-semana** (`arbol §15`). Bien para
   pureza, pero significa que el esfuerzo en tasks no deja huella histórica. Aceptado por diseño.
4. **`G` como fracción semanal** asume telemetría de omisiones por día; si el dato de soporte es
   binario "hoy sí/no" sin histórico de 7 días, `G` colapsa a `1 − #omisiones/#soportes` del día —
   habría que definir la ventana exacta con el dominio de soportes (pendiente menor).
5. **El push de tasks en base≥1.0 es 0** (ya topa en 1000): el usuario perfecto no ve el empujón. Es
   correcto (no hay dónde empujar), pero motivacionalmente "la ayuda mental no aparece justo cuando
   estás impecable". Trade-off aceptado de mantener el techo 1000.
