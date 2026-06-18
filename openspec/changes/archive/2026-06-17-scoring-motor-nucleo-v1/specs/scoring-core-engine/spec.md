# Especificación: scoring-core-engine (NEW)

Cambio: `scoring-motor-nucleo-v1`
Fuente canónica (spec matemático): `docs/scoring/modelo-matematico-nucleo-v1.md`
(7 niveles + §0.1 constantes), `docs/scoring/axiomas-modelo-scoring-v1.md` (contrato
AN/VC/PC/SO/TA/AG/BA/PU), `docs/scoring/verificacion_modelo_oficial.py` (27 asserts verdes).

> Esta delta spec NO reescribe la matemática (esos docs YA son el spec); la traduce a
> requisitos verificables. Cada escenario WHEN/THEN reproduce un `chk(...)` del script de
> verificación con sus números EXACTOS, o un caso de referencia §1.4. La spec del modelo
> manda: si esta spec y el doc matemático difieren en un número, gana el doc.

> NOTA DE ARCHIVO: esta delta spec quedó mergeada en el spec canónico
> `openspec/specs/scoring-core-engine/spec.md` al archivar el cambio (2026-06-17). Este archivo
> es el audit trail del delta tal como se planeó. El contenido íntegro de requisitos/escenarios
> se conserva en el spec canónico.

## Propósito

`scoring-core-engine` es el motor de scoring de **pesos puros** (dominio puro JVM): dado el
estado semanal ya adaptado, produce `ESTADO ∈ [0, 1.5]` y su banda según el contrato
matemático de 7 niveles. Cero gates/caps/worst-term duros: todo comportamiento EMERGE del
peso × valor. NO conoce Room ni Compose ni la forma de los hechos crudos (eso es
`scoring-facts-adapter`); NO mapea a puntos (eso es `scoring-points-mapping`).

(Requisitos completos NIVEL 1–6 + salida del motor + estabilidad aparcada, con sus escenarios
AN/VC/SO/TA/PC/AG/I/O/BA — ver spec canónico `openspec/specs/scoring-core-engine/spec.md`.)
