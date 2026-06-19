# Pro-Prompt — Correcciones post-auditoría del modelo scoring v2

> **Estado: plan para aprobar** (Protocolo de Meta-Prompting). NO se codea hasta el OK del dueño.
> Fecha: 2026-06-08. Origen: auditoría dual opus independiente de `weight_model_fit_v2.py`.

## Síntesis de la auditoría (lo que hay que corregir)

El 45/45 es real, el encoding es fiel y no hay bugs — pero el número sobrevende la evidencia.
Hallazgos convergentes (los dos auditores):

| # | Severidad | Hallazgo |
|---|-----------|----------|
| H1 | CRÍTICO | "El soporte lineal no entra" (plan, Tensión 1) es FALSO: la forma lineal también da 45/45. La asimetría NO está identificada. Hand-calc del orquestador equivocado. |
| H2 | CRÍTICO | SU2 (100%+sueño mal→EM) puntúa 0.83929 vs corte 0.84 → margen 0.0007. El sueño pesa demasiado poco; contradice "sueño = el peso más pesado". |
| H3 | ALTO | Fase 2 medida: params v2 sobre los 43 viejos → 34/43. Rompen B3/C2/J1/J3 por ω=0 vs worst-term. El modelo NO está unificado. |
| H4 | MEDIO | Modulación de pesos casi nula: k_sleep=1→44/45, k_sobr=1→44/45. Cada multiplicador justificado por UN caso (SU9, SB9). |
| H5 | MEDIO | Gate Inquebrantable: 8 casos degenerados (S=1.0) resueltos por un if; `all100` no-falsable. 45/45 infla; el fit de pesos es ~37 casos. |
| H6 | MEDIO | Sub-identificados: cRA (1 caso R), r_relapse, r_unmarked, forma soporte. Sin testear: sueño+sobriedad simultáneos. |
| — | LIMPIO | Fidelidad del encoding (45/45 cruzado), matemática, gate ≥2 bien identificado, ω=0 respetado. |

## Correcciones — por tiers

### TIER 1 — Inmediatas al script/plan (claras, SIN input del dueño)

- **T1.1 (H1)** Reincorporar `sop_form=["asym","lin"]` al grid y ejercitar de verdad la rama `lin`
  (hoy código muerto, línea 94-95). Reportar que AMBAS formas ajustan. **Corregir el plan**
  `2026-06-08-refit-script-scoring-v2.md` líneas 105-108: la Tensión 1 era un hand-calc errado;
  marcar la forma del soporte como **NO identificada** (se decide por dominio o con más casos, no por fit).
- **T1.2 (H2, H5, H6)** Reporte HONESTO: que el script imprima, además de X/N:
  - **margen al corte** por caso (detectar los que viven sobre la línea, ej. SU2=0.0007).
  - **ventana factible** por parámetro y etiqueta "identificado / sub-identificado".
  - **desglose**: nº de casos no-degenerados (peso real) vs casos resueltos por el gate.
  - distribución de etiquetas (R=1, A=7, EM=21, P=11, I=5) para no engañar con el "45".
- **T1.3 (H6)** Anotar en el dataset/diseño qué params están identificados (cEMP≈0.84, cAEM∈[0.61,0.62],
  s_bad≈0.25, gate≥2) vs sub-identificados (cRA, r_relapse, r_unmarked, k_sleep, k_sobr, forma soporte).

Criterio de éxito T1: el script ya no afirma "45/45" a secas; reporta qué prueba y qué NO.

### TIER 2 — Necesitan tu decisión / marcas nuevas

- **T2.1 (H2)** ¿Querés que el sueño malo te baje de Plenitud con HOLGURA (robusto), o te da igual
  que quede a 0.0007? Si robusto → recalibrar (subir efecto del sueño) y/o agregar casos de sueño.
- **T2.2 (H4, H5, H6)** Escribir historias DISCRIMINANTES nuevas para que marques (cierran los huecos):
  - **sueño × sobriedad simultáneos** (hoy CERO; la renormalización con ambos activos está sin probar).
  - **aislar peso de modulador del blend**: sobriedad activa × Conducta-ancla en 0.5/0.75 × limpia/recaída.
  - **banda R** (varios niveles bajos) para pinchar cRA.
  - **Inquebrantable con anclas <100% + superhabit ≥2 capas** (para falsar `all100`).
  - **soporte parcial** (4/7, 5/7; soporte alto con anclas medias) para discriminar asym vs lin.

### TIER 3 — Fase 2: la unificación real (el test decisivo)

- **T3.1 (H3)** Re-codificar los 43 casos viejos al formato v2 y correr el modelo unificado.
  Reconciliar **ω=0 (base pura) vs worst-term (viejos load-bearing)**. Hipótesis a probar primero:
  el "Cuerpo a 0 arrastra" de los viejos venía del **sueño SIEMPRE activo** (que infla Cuerpo) —
  ver si el modulador de sueño lo reproduce SIN reintroducir worst-term. Si no alcanza, decidir con
  el dueño si vuelve un worst-term suave SOLO cuando hay moduladores activos.

## Orden recomendado

**T1 (ahora, barato y honesto) → T3/Fase 2 (el test decisivo de estructura) → T2 (pinchar params con
casos nuevos, recién cuando la estructura esté confirmada).**

Razón: no tiene sentido gastar marcas nuevas (T2) para calibrar `k_sleep` si la Fase 2 (T3) fuerza un
cambio estructural. Primero se confirma que el esqueleto unifica; después se afinan los pesos.

## Fuera de alcance

- NO tocar código de la app (`*Policy.kt`). Sigue siendo diseño.
- NO borrar el script viejo ni las marcas. NO re-marcar nada sin pedírtelo.

## Decisión pendiente del dueño

1. ¿Apruebo TIER 1 (correcciones de honestidad del script + corregir el plan) y sigo con Fase 2?
2. ¿O preferís meter TIER 2 (escribir las historias discriminantes para marcar) antes de Fase 2?
3. T2.1: ¿el sueño malo debe bajarte con holgura o aceptás el barely-EM?
