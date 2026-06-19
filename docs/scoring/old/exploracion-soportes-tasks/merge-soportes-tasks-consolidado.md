# Merge consolidado — SOPORTES y TASKS en el scoring

> **⛔ OBSOLETO (2026-06-16) — SUPERSEDED por `merge-v2-consolidado.md`.** Este merge partía de un
> modelo EQUIVOCADO: sombra global (Forma B) y tasks en el número visible 700-1000. El dueño corrigió:
> soportes y tasks entran DENTRO del valor de cada capa (Forma A), soporte bidireccional leve a la base,
> task como extra efímero diario dentro de la curva de superhabit. Ver `merge-v2-consolidado.md`. NO usar
> este doc como contrato.

> **Estado: borrador para aprobar (merge del orquestador).** Síntesis de las 3 propuestas Opus
> divergentes (`subagente-A/B/C-propuesta.md`). No es contrato hasta que el dueño apruebe y se
> resuelvan las 2 decisiones abiertas (§5). Verificado con `python3 merge_verificacion.py` (resultados
> reales en §4). Fecha: 2026-06-15. Proyecto Engram: `apk-personal`.
> Set-prompt de origen: `meta/instructions/2026-06-15-set-prompt-soportes-tasks-3agentes.md`.

---

## 0. Veredicto en una frase

Los 3 convergen en lo estructural (soporte = cimiento que arrastra leve, nunca gloria; task = empujón
acotado que no compra estado). **El merge toma: el motor de soportes del consenso A+B (sombra
neutra-cuando-se-sostiene), las tasks solo-en-el-visible de A+C (tu intención explícita), y el gran
aporte de C: rediseñar el número visible 700–1000 para que deje de mentir.** Quedan **2 decisiones
tuyas** (§5): re-marcar SO5, y elegir cuán liviano es el arrastre del soporte.

---

## 1. Qué tomo de cada subagente (y qué descarto)

| Pieza | Gana | Por qué |
|-------|------|---------|
| **Estructura del soporte** | **A + B** (sombra estilo opt-in) | Vos dijiste "soporte ≈ sobriedad". La sobriedad ya es una sombra (O12): neutra cuando se sostiene, arrastra cuando se rompe. Reusar esa maquinaria (O1–O13) es lo más coherente y no inventa un canal nuevo. |
| **Magnitud del soporte** | **A** (muy liviana) | Pediste "muy muy levemente". A despeja `ETA` de `DROP_MAX=0.03` (3 centésimas). La de B (`BETA_SUP=0.15`, escala con N) hace que descuidar TODOS los soportes te baje una banda entera — demostrado en §4: con `BETA_SUP=0.10` ya caés 0.23. Demasiado para "levemente". |
| **Soporte ¿puede SUBIR?** | **A + B: NO** (descarto el blend γ de C para el motor) | El blend de C deja que un soporte sostenido *levante* una base de anclas floja. Eso choca con tu axioma `ANCLAS > SOPORTES` (el soporte rescataría mal trabajo de anclas). El soporte solo evita la caída. |
| **Dónde viven las tasks** | **A + C: solo el número visible** | Tu intención textual: "subir los puntos que se muestran de 700 a 1000, **no** en el motor de scoring". Descarto el canal-extra de B (mete la task al motor) — bien construido, pero contradice lo que pediste. |
| **Forma del empujón de task** | **A + C** (meritocrático + saturado + nunca resta) | A: solo ayuda a quien ya está alto (`base^q`). C: lo formaliza con `prox(estado)=0` bajo 0.70. Saturación para que coleccionar tasks no infle. Tope < impacto del soporte. |
| **Mapeo visible 700–1000** | **C** (el gran hallazgo) | Hoy el visible `700+clamp(base)·300` **satura en 1000 con ESTADO=1.0**: TODO Inquebrantable es invisible, el número miente. C lo reemplaza por una biyección honesta del ESTADO real con hitos legibles. Esto se adopta **independientemente** del debate de tasks: es arreglar un bug. |

---

## 2. El modelo fusionado

### 2.1 SOPORTES — término-sombra liviano en la base (de A+B)

```
M_sup (por soporte)  = días_sostenidos / 7         (UX inversa = presentación; interno: más = mejor)
M_sup (capa)         = promedio de los soportes de la capa     (sin dato del día = sostenido → 1.0)
término-sombra        = (M_sup, w_sup)  agregado al promedio del canal base
extra_global          = SIN CAMBIOS (solo anclas, v4)          → el soporte NUNCA genera superhabit
ESTADO                = min(base_global, 1) + extra_global       → bandas §16-NUEVO intactas
```

Axiomas (estilo O1–O13, espejo del opt-in): **S1** solo base, nunca extra · **S2** sostenido = neutro
exacto · **S3** descuidado arrastra **muy levemente** · **S4** no mata el valor de las anclas · **S5**
no distorsiona el superhabit (`Sol=Tin`) · **S6** ventana de 7 días · **S7** sostener no fabrica banda
(neutro en M=1).

**La magnitud (`w_sup`) es la perilla a calibrar — ver decisión §5.2.** Dos formas candidatas:
- **A (recomendada, local):** `w_sup = ETA·(1−M_sup)`, `ETA≈0.093` despejado de `DROP_máx=0.03` por
  soporte. No escala con N → el soporte pesa aún menos al crecer N (refuerza `OPT-IN > SOPORTE`).
- **B (alternativa, N-escalada):** `w_sup = BETA_SUP·N·(1−M_sup)`, `BETA_SUP` despejado de una historia
  (SO2). Plano en N como el opt-in, pero más pesado.

### 2.2 TASKS — empujón solo en el número visible (de A+C)

```
prox(e)   = 0                              si e < 0.70           (solo ayuda a quien ya empuja)
            clamp((e−0.70)/0.20, 0, 1)     si e ≥ 0.70
task_push(n, e) = TASK_MAX_PTS · (1 − e^(−K·n)) · prox(e)        ≥ 0   (nunca resta; satura)
n = nº de tasks HECHAS con capa asignada y rol no-Neutral        (neutral no cuenta)
```

`TASK_MAX_PTS = 4.0` pts (despejado de C: `0.75 × impacto_visible_soporte`, garantiza `SOPORTE > TASK`).
`K = 0.7`. **Las tasks NO entran al ESTADO ni a la banda** — la banda se calcula solo sobre el ESTADO.
Axiomas: **T1** solo visible, nunca banda · **T2** nunca resta · **T3** magnitud < soporte · **T4**
satura · **T5** meritocrática (nula bajo estado 0.70) · **T6** neutral no suma.

### 2.3 VISIBLE 700–1000 — biyección honesta del ESTADO real (de C)

```
VisibleScore_base(ESTADO) =
    700 + ESTADO·200                          si ESTADO ≤ 1.0     # tramo base   [700, 900]
    900 + (ESTADO−1)/(1.5−1)·100              si ESTADO > 1.0     # tramo extra  [900, 1000]
VisibleScore(ESTADO, n) = min(1000, VisibleScore_base(ESTADO) + task_push(n, ESTADO))
```

Hitos: **0→700 · 1.00→900 (entrada Plenitud) · 1.10→920 (Inquebrantable) · 1.50→1000**.
**Esto resuelve tu ejemplo del "897→900" de forma honesta:** tu intuición de que ~900 = la zona de
élite **es correcta bajo este mapeo** (900 = Plenitud). Lo que NO existe es "cruzar a Inquebrantable
con tasks": las tasks arañan el número hacia el siguiente hito (empujón mental) pero la **banda real**
sigue saliendo del ESTADO (anclas + opt-ins + soportes). Para cruzar de verdad → anclas.

---

## 3. La tensión central que los 3 destaparon (y que solo vos podés cerrar)

**SO5: "anclas al 50% + soportes 7/7" → vos lo marcaste En marcha.** Bajo CUALQUIER modelo donde el
soporte sea sombra (A y B) eso es **imposible**: una sombra sostenida es neutra (no levanta), así que
el estado se queda en 0.50 = Atención (verificado en §4). Es el mismo *flip forzado* que el SB9 de
`modelo-consolidado-v2.md`. Solo el blend γ de C podría subirlo… pero para llevar 0.50→0.62
necesitaría `γ≥0.24`, que rompe "soporte muy leve" y `ANCLAS > SOPORTES`. **No hay número que salve
SO5=En marcha sin contradecir tus propios axiomas.** Esto NO es un defecto del modelo: es tu axioma
`ANCLAS > SOPORTES` hablando. Decisión en §5.1.

---

## 4. Verificación del modelo fusionado (`python3 merge_verificacion.py`, resultados reales)

```
MERGE  BETA_SUP=0.1  TASK_MAX_PTS=4.0  TASK_K=0.7
(b) cumplir-justo estado=1.0000 PLENITUD visible=900.0          (eje semántico intacto)
    +soportes 7/7 estado=1.0000 neutro=True                     (S2: sostener = neutro exacto)
(a) Sol=1.1440 Tin=1.1440 empatan=True                          (S5: superhabit intacto)
(c) anclas 100% + TODOS soportes en piso estado=0.7692 EN MARCHA drop=0.2308   ← ⚠ con BETA_SUP=0.10
    1 soporte en piso estado=0.9091 PLENITUD drop=0.0909
[SO5] anclas 50% + soportes 7/7 estado=0.5000 ATENCION          (sombra NO rescata → flip, ver §3)
(d) anti-gate soporte paso max|dEstado|(dM=.001)=0.000173       (continuo, sin gate/cap)
(e) impacto VISIBLE: ancla=33.33 > soporte=18.18 > task=4.00 orden=True
    task_push(5,e=0.50)=0.000 (nula bajo 0.70)  satur 10t=4.00 (tope)  flips de banda por tasks=0

   ESTADO banda            pre-v4   merge(C)     ← el visible deja de mentir
    0.90 PLENITUD            970    880.0
    1.00 PLENITUD           1000    900.0
    1.10 INQUEBRANTABLE     1000    920.0        (antes: invisible — saturaba en 1000)
    1.50 INQUEBRANTABLE     1000   1000.0
```

**Lo verde:** eje semántico (cumplir-justo=1.0=Plenitud=visible 900), superhabit intacto (`Sol=Tin`),
soporte neutro al sostenerse, anti-gate continuo, orden `ancla>soporte>task`, tasks nulas en estado
bajo y 0 flips de banda, y el visible ahora codifica Inquebrantable.

**⚠ El hallazgo del merge (`drop=0.23`):** con la forma N-escalada de B y `BETA_SUP=0.10`, descuidar
TODOS los soportes te tira de Plenitud a En marcha. Eso es **demasiado** para "muy muy levemente". Por
eso el merge **recomienda la calibración liviana de A** (`DROP` por soporte ≈ 0.03, total acotado).
Esto es exactamente la decisión §5.2.

---

## 5. Decisiones que necesito de vos antes de cerrar

### 5.1 — Re-marcar SO5 (obligatoria)
SO5 ("anclas 50% + soportes 7/7") no puede ser **En marcha** sin romper `ANCLAS > SOPORTES`.
- **Opción recomendada:** re-marcarla a **Atención** (los soportes sostienen el cimiento, no rescatan
  un trabajo flojo de anclas). Coherente con tu propio axioma de orden.
- **Alternativa:** aceptar que el soporte *suma al alza* (blend γ grande) — pero eso degrada la
  jerarquía y vuelve el soporte casi un ancla. No lo recomiendo.

### 5.2 — Cuán liviano arrastra el soporte (calibración)
Elegí el techo de "descuidar todos los soportes de una capa":
- **Liviano (A, recomendado):** ~3–5 centésimas por soporte; descuidar todo te deja **dentro de
  Plenitud**. Máxima fidelidad a "muy muy levemente".
- **Notorio (B):** descuidar todo puede bajarte una banda (más "se siente", menos "leve").
Lo ideal: que me des 1–2 marcas de "descuidé soportes" en `historias-soportes-tasks-v1.md` y despejo
el número fino, en vez de elegirlo a dedo.

---

## 6. Lo que queda abierto (menor)

- **Tasks por-capa vs globales:** el merge las cuenta globales. Si querés que una task "ayude" a la
  capa de su rol, se mueve el push a esa capa. No hay marca que lo exija aún.
- **Quiebre de pendiente del visible en ESTADO=1.0** (200→100 pts/unidad): continuo pero no suave;
  honesto (el extra es "más caro"). Se puede suavizar si molesta visualmente.
- **`prox(estado)` arranca en 0.70:** atar el umbral al inicio de una banda (En marcha) en vez de un
  número suelto.
- **Soporte + opt-in en la misma capa:** ambos modulan la base por sombras distintas; verificado que
  no se rompen, sin marca que valide la magnitud combinada.
- **Estabilidad multi-semana (`arbol §15`)** sigue ortogonal — fuera de scope de esta sesión.

---

## 7. Próximo paso sugerido

1. Resolvés §5.1 (re-marca SO5) y §5.2 (calibración liviana) — idealmente con 1–2 marcas nuevas.
2. Con eso despejo el número fino del soporte y congelo `axiomas-soportes-tasks-v1.md` (contrato,
   estilo O1–O13), análogo a `axiomas-opt-in-v1.md`.
3. Recién ahí se toca código: el motor v4 + el nuevo mapeo visible (es cambio de `ScoringScreen` /
   `DashboardProjection`, no del esquema Room).
