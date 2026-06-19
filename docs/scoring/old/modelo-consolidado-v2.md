> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo de scoring consolidado v2

> **Estado: borrador para aprobar.** Merge del orquestador de los 3 complementos opus
> (`modelo-complemento-A/B/C.md`), que convergieron. Supersede a `modelo-consolidado-v1.md`.
> Fecha: 2026-06-08.

## Qué cambió respecto a v1 (y por qué es MEJOR)

En v1 había una **asimetría**: el sueño entraba al **valor** de Cuerpo, pero la sobriedad NO entraba al
valor de Conducta (solo modulaba el peso + castigaba la recaída; la racha limpia no sumaba). Eso generaba
un olor (racha limpia + Conducta floja → caías a Atención, porque la racha no contaba) y **rompía** el caso
de una capa modulada sin actividades.

**v2 unifica:** sueño y sobriedad se tratan IGUAL — los dos entran al valor de su capa. Esto **elimina la
rama especial** que tenía la sobriedad → el motor queda **más simple** (Cuerpo y Conducta usan la misma
fórmula de valor).

## 1. La fórmula unificada del valor de una capa modulada

```
valor_capa = γ · M + (1 − γ) · promedio(anclas de la capa)
```
- `M` = señal del modulador:
  - **Sueño** (Cuerpo): bien=1.0 · mal/no-registrado = `s_bad`
  - **Sobriedad** (Conducta): racha limpia=1.0 · recaída = `r_rel` · sin-marcar = `r_unm`
- `γ = 0.5` (mitad modulador / mitad actividades — simétrico con el sueño).
- **La racha limpia ahora SUMA como logro** (sube el valor hacia 1), igual que dormir bien. La recaída lo hunde.
- Capa **sin opt-in**: `valor = promedio(anclas)` (como siempre).

**Caso sin actividades regulares (solo el opt-in):** con cero anclas, el término `(1−γ)·promedio` desaparece
y queda `valor = M`. Conducta-solo-sobriedad: limpia→1.0, recaída→`r_rel`. Cuerpo-solo-sueño: bien→1.0, mal→`s_bad`.
**El motor ya no se rompe** — es el límite natural del blend, sin regla.

## 2. Lo que NO cambió (queda igual que v1)

Pesos Forma A (multiplicador + renormalización; sueño ×k_sleep a Cuerpo, sobriedad ×k_sobr a Conducta);
`score = Σ peso×valor`; bandas `R<0.40 · A<0.62 · EM<0.85 · P≥0.85`; gate Inquebrantable ≥2 capas; soportes
asimétricos al valor; tasks neutras; higiene digital = ancla de Conducta (UI ≠ scoring); capas normales iguales;
anclas promedian dentro de la capa. **Cero reglas/caps/gates/worst-term.**

## 3. Verificación contra las 45 marcas

Los 3 complementos reproducen **44/45**. El **único flip es SB9: Atención → En marcha**, exactamente el
predicho. **Forzado, no de calibración:** ningún parámetro mantiene SB9=A si la racha cuenta en el valor
(eso exigiría un parche-regla prohibido). Con SB9 re-marcado a En marcha → **45/45 limpio**.

## 4. Lo que el dueño debe confirmar / lo que queda abierto

- **🟡 SB9 → En marcha (re-marca):** es la consecuencia CORRECTA de lo que pediste (la racha cuenta como
  logro). 6 meses limpio + el resto casi en orden ya no es Atención. **Confirmá la re-marca.**
- **🔴 Sueño + sobriedad juntos:** sigue con CERO marcas (heredado de v1). Falta ≥1 marca para pinchar números.
- **🔴 Recaída + actividades de Conducta también bajas** (riesgo nuevo, catch del modelo C): ahora la recaída
  se mezcla con las anclas; podría hundir más que antes. No hay marca que lo valide aún.
- **Sub-identificación:** `γ ∈ {0.4, 0.5}`, `r_rel`, `r_unm`, y los del caso sin-actividades tienen juego;
  se pinchan con 1-2 marcas de los huecos de arriba.

## 5. Recomendación

El esqueleto está cerrado y es más simple que v1 (motor unificado). Para congelar números finales: (a) confirmar
SB9→En marcha, (b) ≥1 marca de sueño+sobriedad juntos, (c) ≥1 marca de capa-sin-actividades. Tres marcas y cierra.
