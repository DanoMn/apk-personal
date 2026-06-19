> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo de scoring consolidado v1

> **Estado: borrador para aprobar.** Síntesis del orquestador a partir de los 3 modelos opus
> independientes (`modelo-propuesto-A/B/C.md`), que convergieron 45/45 los tres. Fecha: 2026-06-08.
> Piso: `meta/instructions/2026-06-08-tres-modelos-motor-scoring.md`. Marcas: `dataset-decisiones-estado-v1.md`.

## 1. Resumen (5 líneas)

El motor es **solo pesos**: `score = Σ(peso_capa × valor_capa)` → estado por bandas. CERO reglas/caps/gates/
worst-term. Capas normales pesan igual (1/N). Los opt-in (sueño→Cuerpo, sobriedad→Conducta) **multiplican**
el peso de su capa y se **renormaliza** (Forma A). Un opt-in malo hunde el **valor** de su capa. Todo el
comportamiento (sueño domina Cuerpo, recaída te tira, la peor capa no colapsa) **EMERGE** del peso × valor.

## 2. Fórmula

**Valor de cada capa** (en `[0,1]`):
- `valor = promedio(anclas de la capa)`; `frac_ancla = min(hecho/meta, 1)`. (La cantidad de anclas NO cambia el peso.)
- **Cuerpo** con sueño activo: `valor = β·sueño + (1−β)·promedio(anclas)`, sueño: bien=1, mal/no-registrado=`s_bad`.
- **Conducta** con sobriedad activa: `valor = limpia→promedio(anclas) · recaída→r_rel · sin-marcar→r_unm`.
- **Soporte** en una capa (asimétrico): `valor += b_sop·sostenido − p_sop·(1−sostenido)`, clavado en `[0,1]`. **Tasks = 0.**

**Peso de cada capa** (Forma A — multiplicador + renormalización):
- Base: 1 por capa activa. Sueño activo → peso de Cuerpo `×k_sleep`. Sobriedad activa → peso de Conducta `×k_sobr`.
- Normalizar: `peso_capa = bruto / Σ(brutos)`.

**Estado:** `score = Σ(peso × valor)`; bandas `R<0.40 · A<0.62 · EM<0.85 · P≥0.85`.
**Inquebrantable:** no es banda — gate sobre Plenitud: anclas 100% + superhabit en **≥2 capas**.

## 3. Tabla de pesos (representativos: k_sleep=1.5, k_sobr=3.0)

| Situación | N=3 (I · Cu · Co) | N=5 (I · Cu · Co · V · P) |
|---|---|---|
| Sin opt-in | 33 · 33 · 33 | 20 · 20 · 20 · 20 · 20 |
| Sueño activo | 29 · **43** · 29 | 18 · **27** · 18 · 18 · 18 |
| Sobriedad activa | 20 · 20 · **60** | 14 · 14 · **43** · 14 · 14 |
| Sueño + sobriedad | 18 · **27** · **55** | 13 · 18 · **39** · 13 · 13 |

(La porción del opt-in baja al crecer N — consecuencia de Forma A, coherente con el lote IN de 5 capas que decide por cobertura.)

## 4. Casos límite (resueltos)

1. **Escalado N (3→5): Forma A.** Forzada, no elegida: Forma B colapsa el apretón (>100% de peso → capa libre negativa → exigiría clamp = regla prohibida). Demostrado por Modelo C.
2. **El apretón (3 capas + ambos opt-in):** la capa libre queda en **~18%**, emerge de la renormalización, nunca se ahoga. Abandonarla cuesta una banda (P→EM). Sin regla.
3. **Anclas múltiples por capa:** se **promedian** dentro de la capa; el peso de la capa no cambia por tener más anclas (evita gaming; el motor mide áreas, no actividades).
4. **Soporte:** asimétrico (`p_sop≈0.25` castigo, `b_sop≈0.02` premio); entra al valor de la capa, sin peso propio.
5. **Higiene digital:** ancla de **Conducta** normal. Estar como opt-in **dentro de la feature Sueño** es **solo UI** (relacionadas), NO toca pesos (mismo principio que la UX inversa de soportes). Sin matrioshka en el motor.
6. **Magnitud de multiplicadores:** dentro del rango que las marcas permiten (Conducta-sobriedad 50–63%). Ver §6 (sub-identificación).
7. **Inquebrantable:** gate ≥2 capas, N-independiente.

## 5. Verificación

Los 3 modelos reproducen **45/45** de las marcas, cada uno con su script descartable independiente. Convergencia total: ningún caso requirió una regla.

## 6. Lo que queda ABIERTO (honesto — no inventado)

- **🔴 Las dos juntas (sueño + sobriedad): CERO marcas.** Los 3 asumieron acumulación lineal y los 3 toparon con la misma consecuencia sin validar: con ambas activas, *sueño malo + sobriedad limpia* queda en **Plenitud** (no En marcha), porque la sobriedad le resta peso a Cuerpo. **Falta ≥1 marca del dueño** para fijarlo.
- **🔴 SU5 vs SU6 a 0.0004 de score** (Modelo C): clava `c_AEM≈0.62` y `k_sleep≈1.5`, lo que **impide subir el peso del sueño** tanto como el dueño quiere ("el sueño es lo más pesado"). **Re-marcar SU5/SU6** liberaría eso.
- **Sub-identificación:** ~300–2900 sets de parámetros dan 45/45. Los valores exactos (k_sleep, k_sobr, cortes) tienen juego; se eligieron centrados. Se pinchan con las marcas faltantes de arriba.

## 7. Recomendación

El **esqueleto está cerrado y validado** (Forma A, pesos puros, 45/45). Para congelar los NÚMEROS finales faltan exactamente 2 cosas, ambas de 1-2 marcas: (a) un caso con sueño+sobriedad activos juntos, (b) re-marcar SU5/SU6 para liberar el peso del sueño. Recién con eso los multiplicadores quedan pinchados sin invento.
