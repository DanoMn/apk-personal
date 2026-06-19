# Merge v2 consolidado — SOPORTES y TASKS dentro del valor de capa (Forma A)

> **⚠ PESO DE CAPA REVISADO (2026-06-16) en `modelo-consolidado-v3-pesos-variables.md`.** El peso de
> capa de este v2 (masa = piso + densidad continua) tenía un bug: hacía crecer el peso con la cantidad
> de anclas de forma incorrecta. El v3 lo reemplaza por votos decrecientes por ancla. Los soportes
> (blend) y tasks (saturación conjunta) de este v2 siguen vigentes. Leé el v3 para el modelo actual.

> **Estado: borrador para aprobar (merge del orquestador).** Síntesis ultrathink de los 3 planes Opus
> v2 (`subagente-A/B/C-plan-v2.md`), todos en Forma A (soportes/tasks DENTRO del valor de cada capa).
> Verificado por el orquestador con `python3 merge_v2_verificacion.py` (resultados reales en §4).
> NO es contrato hasta que el dueño apruebe y resuelva las decisiones abiertas (§5).
> Fecha: 2026-06-16. Proyecto Engram: `apk-personal`. Set-prompt: `meta/instructions/2026-06-16-set-prompt-soportes-tasks-v2.md`.
> **Supersede al merge v1** (`merge-soportes-tasks-consolidado.md`), que partía de un modelo equivocado
> (sombra global + tasks en el número visible).

---

## 0. Veredicto

Los 3 opus **convergieron en lo estructural** (señal de soporte `min(días/4,1)` promediada, bidireccional
leve, solo base; task solo extra dentro de la curva con gate base², efímera diaria, techo por capa) y el
hallazgo **unánime y fuerte**: el `~0.1` de techo de task **rompe el candado anti-abuso** — debe ser
**< 0.10**. El merge toma: **soporte = blend convexo (A≡B)**, **task = saturación conjunta
reparametrizada (B)** porque es la única que respeta el techo 0.5 *conjunto* y hace que cada task valga
menos cuanto más superhabit ya hay, y **peso de capa = piso + densidad (A+B)**. Quedan **2 decisiones
del dueño** (§5).

## 1. Tabla comparativa de los 3 enfoques

| Dimensión | OPUS A (surplus virtual) | OPUS B (blends convexos) | OPUS C (presupuesto/headroom) | **Merge** |
|-----------|--------------------------|--------------------------|-------------------------------|-----------|
| **Soporte (forma)** | `base+w·(G−base)` (= blend) | `(1−γ)·base+γ·G` (blend) | `base+η·(1−base)·(2g−1)` (gap-scaled) | **Blend (A≡B)** |
| **Soporte (neutro)** | G = base anclas | G = base anclas | g = 0.5 (mitad de días) | **G = base anclas** |
| **Soporte (magnitud)** | 0.10 | 0.07 | 0.10 | **0.07** |
| **Castigo capa perfecta** | −0.10 (A lo marca como riesgo) | −0.07 | 0 (gap=0) | **−0.07** (o variante C, §5.1) |
| **Task (forma)** | término aparte, techo propio | **saturación conjunta** (re-satura) | clamp al headroom `min(raw, 0.5−extra)` | **Saturación conjunta (B)** |
| **Task respeta techo 0.5 conjunto** | ❌ **NO** (suma aparte, puede pasar 0.5) | ✅ por re-saturación | ✅ por clamp | **✅ (B)** |
| **Task encoge con superhabit** | no | ✅ (lift 0.048→0.006) | parcial (clamp) | **✅ (B)** |
| **Techo task / capa** | 0.09 | 0.05 | 0.06 | **0.06** |
| **Saturación task** | 3t=90% | 1ª=63% | 3t=91% | **1ª≈63%, 3t≈95%** |
| **Peso capa solo-soportes** | `0.4+0.6·dens` lineal | `⅓+⅔·dens` exp | masa fija 0.35 | **`0.35+0.65·dens` exp** |
| **Filosofía** | mínima cirugía sobre v4 | todo es mezcla/saturación | llenar espacios libres | mezcla de las 3 |
| **Verificación** | 8/8 (con agujero techo 0.5) | 8/8 | 8/8 | **8/8 limpio** |

## 2. El modelo fusionado (fórmulas)

```
# --- por capa ---
base_anc  = promedio min(R,1) de anclas           # v4 (R no se toca)
extra_anc = promedio max(R−1,0) de anclas          # v4 (ya saturado, techo 0.5)

# SOPORTE — blend convexo en la base (bidireccional leve, no crece con la cantidad)
G_s      = promedio_i( min(días_sostenidos_i / 4, 1) )         # ventana indulgente 4d
base_eff = (1−WS)·base_anc + WS·G_s      [= G_s si la capa no tiene anclas]    WS=0.07

# TASK — saturación CONJUNTA reparametrizada (comparte la curva del superhabit, gate base²)
su_anc   = −s0·ln(1 − extra_anc/0.5)                # invierte la exp de v4 → surplus pre-saturación
g_task   = 1 − exp(−n_hoy / N0)                     # n_hoy = tasks completadas HOY (efímero)    N0=1.0
extra    = extra_anc + (0.5·(1−exp(−(su_anc + THETA·g_task)/s0)) − extra_anc) · base_eff²
                                                    # THETA=0.0639 (da lift máx = TAU=0.06)

valor_capa = min(base_eff, 1) + extra
masa       = 0.35 + 0.65·(1 − exp(−n_anclas/1.0))   # capa solo-soportes pesa 0.35
score      = Σ(valor_capa · masa) / Σ(masa)
```

**Parámetros despejados de axiomas (no a dedo):**
- `WS=0.07`: el descuido total de un soporte sobre una capa justa baja la base exactamente 0.07 (paso leve).
- `TAU=0.06`: cumplir-justo + tasks-full en TODAS las capas = 1.06 < 1.10 → nunca compra Inquebrantable solo.
- `THETA=0.0639 = −s0·ln(1−TAU/0.5)`: hace que el lift máximo de task sea exactamente TAU.
- `PISO=0.35`: una capa solo-soportes pesa 35% de una capa con anclas (continuo vía densidad).

## 3. Cómo se resuelven tus 3 reglas (en una línea cada una)

1. **Soporte sube/baja levemente, no crece con la cantidad** → blend con `G_s = promedio` y `WS=0.07`.
2. **Capa solo-soportes pesa menos** → `masa = 0.35 + 0.65·densidad de anclas`.
3. **Task = extra efímero diario, sin abuso** → saturación conjunta dentro de la curva (techo 0.5
   garantizado), techo 0.06/capa, reset diario, araña un cruce solo al ras.

## 4. Verificación del merge (`python3 merge_v2_verificacion.py`, resultados reales)

```
(a) Sol=1.144111 Tin=1.144111 empatan=True              (superhabit intacto, O5)
(b) cumplir-justo=1.0000 PLENITUD                        (eje semántico intacto)
(c) soporte base-media: descuid 0.329 < par 0.364 < sostiene 0.399 (bidireccional leve)
    1 soporte = 5 soportes = 0.3988 (NO crece con la cantidad)
    capa perfecta + soporte descuidado = 0.93 (castigo 0.07, leve)
(d) masa con-ancla 0.761 vs solo-soportes 0.350 (pesa menos); una 3a capa perfecta
    solo-soporte sube el score +0.000 vs +0.144 de una capa-ancla superhabit (no infla)
(e) XL + 100 tasks -> extra 0.4405 (≤0.5 SIEMPRE, techo conjunto respetado); reset diario 1.057→1.000
(f) al ras (0.821 En marcha) + 5 tasks -> 0.861 PLENITUD (CRUCE); cumplir-justo + 100 tasks
    en TODAS = 1.060 Plenitud (NO compra Inquebrantable)
(g) anti-gate: soporte paso máx 0.0002; task paso máx 0.014 (el 1er escalón de task se nota —
    son eventos discretos, no hay discontinuidad en la fórmula)
(h) impacto ANCLA 0.2155 > SOPORTE 0.0700 > TASK 0.0600 (orden correcto)
```

## 5. Decisiones que necesito de vos antes de cerrar

### 5.1 — Castigo del soporte en una capa perfecta
Con el blend (A/B), si tus anclas están **perfectas** y descuidás el soporte, la capa baja 0.07. La
variante de **C** (escalar por el gap `1−base_anclas`) haría que en una capa perfecta el soporte
descuidado **no reste nada** (no hay gap que llenar). ¿Cuál preferís?
- **Blend (recomendado):** descuidar el cuidado base siempre cuesta un toque, aún con anclas perfectas.
- **Gap-scaled (C):** el soporte solo importa cuando tu cimiento de anclas está incompleto.

### 5.2 — Semántica de una capa solo-soportes mal sostenida
Una capa **sin anclas** con todos los soportes descuidados hoy vale **0**. ¿Está bien que valga 0
(arrastra, aunque con masa 0.35), o querés un **piso tibio** (que nunca baje de, digamos, 0.3) porque
es una capa de poca sustancia y no debería hundir el score? (C lo dejó como riesgo abierto.)

## 6. Lo que queda abierto (menor)
- `WS`, `TAU`, `PISO` están despejados de axiomas cualitativos; se afinan con 1-2 marcas tuyas de
  historias de soportes/tasks.
- El 1er escalón de task (~0.04) se nota porque las tasks son discretas; es esperable, no es un gate.
- Interacción soporte+task+anclas simultáneos en una capa: verificada consistente, sin marca que la valide aún.

## 7. Próximo paso
1. Resolvés 5.1 y 5.2 (idealmente con 1-2 marcas).
2. Congelo `axiomas-soportes-tasks-v1.md` (contrato estilo O1-O13) con los números finales.
3. Recién ahí se toca código (el motor v4 + la capa de valor; NO esquema Room).
