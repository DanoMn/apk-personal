# Plan — Complemento del modelo: casos nuevos (opus) + merge

> **Estado: plan para aprobar.** No se lanzan subagentes hasta el OK del dueño. Fecha: 2026-06-08.
> Complementa (NO rehace) el modelo consolidado v1.

## Base (CERRADA — no se re-discute)

`docs/scoring/modelo-consolidado-v1.md` — Forma A (multiplicador + renormalización), motor de pesos
puros (`score = Σ peso×valor`), cero reglas/caps/gates/worst-term, bandas R/A/EM/P, gate Inquebrantable
≥2 capas, soportes asimétricos al valor, tasks neutras, higiene digital = ancla de Conducta (UI ≠ scoring).
**Los subagentes LEEN ese doc y solo agregan/ajustan para los 3 casos nuevos. Todo lo demás queda igual.**

## Casos NUEVOS a resolver (lo que surgió y no estaba contemplado)

1. **Sobriedad como parte del VALOR de Conducta (simetría con el sueño en Cuerpo).**
   Hoy hay una asimetría: el **sueño SÍ entra al valor de Cuerpo** (mitad sueño / mitad caminar), pero la
   **sobriedad NO entra al valor de Conducta** — solo sube el peso y, si recaés, lo hunde; la **racha limpia
   no suma nada**. Eso genera un olor (una racha limpia + actividad de Conducta floja = caés a Atención,
   porque la racha no cuenta y el peso amplifica la flojera). Definir: la **racha limpia entra al valor de
   Conducta** (suma, es un logro), la recaída lo hunde — igual que el sueño. Definir el blend (cuánto pesa
   la racha vs las actividades de Conducta dentro del valor de la capa).

2. **Capa modulada SIN actividades regulares (solo el opt-in).**
   Conducta con **solo sobriedad** (sin Orden digital ni ninguna actividad); por simetría, Cuerpo con **solo
   sueño** (sin Caminar). Hoy el valor sale del promedio de actividades → con cero actividades el modelo se
   **rompe**. Definir: el valor de la capa sale ENTERAMENTE del modulador en ese caso.

3. **Reconciliar con las 45 marcas existentes.**
   Bajo la nueva estructura (sobriedad en el valor), re-correr las 45 marcas: reportar **cuáles cambian**
   (se espera que **SB9** pase de Atención a En marcha, porque ahora la racha cuenta) y avisar si el dueño
   debería re-marcar alguna. Honestidad total: no forzar reglas para tapar un cambio.

## Fórmula de trabajo (cada subagente, su propio MD)

Cada uno escribe `docs/scoring/modelo-complemento-{A,B,C}.md` con:
1. **Qué cambia** respecto al consolidado v1 (delta claro, no reescribir lo que queda igual).
2. **Fórmula nueva del valor de Conducta** (racha + actividades), y el caso sin-actividades (Conducta y Cuerpo).
3. **El blend exacto** (cuánto pesa la racha dentro del valor de Conducta) y su justificación.
4. **Verificación contra las 45 marcas:** cuántas reproduce ahora, cuáles flipearon y por qué (script descartable
   en /tmp; NO tocar `scripts/scoring/`).
5. **Tradeoffs y riesgos.**

Reglas duras: respetar TODA la base del consolidado v1; cero reglas-parche; todo emerge de peso × valor.
Trabajan independientes y a ciegas.

## Salida + merge

3 MDs de complemento → el orquestador (yo) mergea lo mejor en `docs/scoring/modelo-consolidado-v2.md`.

## Decisión pendiente

¿Apruebo y lanzo los 3 opus? ¿Agregás o sacás algún caso nuevo antes?
