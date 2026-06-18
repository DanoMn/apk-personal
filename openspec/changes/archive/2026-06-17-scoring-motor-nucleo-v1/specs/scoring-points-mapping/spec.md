# Especificación: scoring-points-mapping (NEW)

Cambio: `scoring-motor-nucleo-v1`
Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` § NIVEL 7 (mapeo E),
`docs/scoring/axiomas-modelo-scoring-v1.md` § 9 (PU1–PU5),
`docs/scoring/verificacion_modelo_oficial.py` (`PU1`, `PU3`, `PU4`).

> NOTA DE ARCHIVO: esta delta spec quedó mergeada en el spec canónico
> `openspec/specs/scoring-points-mapping/spec.md` al archivar el cambio (2026-06-17). Este
> archivo es el audit trail del delta tal como se planeó.

## Propósito

`scoring-points-mapping` mapea `ESTADO ∈ [0, 1.5]` a PUNTOS visibles `∈ [650, 1100]` con el
enfoque E (suma de rampas logísticas), para el dashboard. Reemplaza `VisibleScorePolicy`
(`700 + base·300`); el rango visible pasa de `700–1000` a `650–1100`.

(Requisito completo "Mapeo E ESTADO→PUNTOS [650,1100]" + escenarios PU1/PU3/PU4/PU5 — ver spec
canónico `openspec/specs/scoring-points-mapping/spec.md`. Desviación deliberada del design: el
mapeo quedó como policy de dominio reutilizable `PointsMappingPolicy`, no como función privada
de `DashboardProjection`, para que seam y dashboard usen el mismo cálculo.)
