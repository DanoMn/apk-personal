# Especificación: scoring-facts-adapter (NEW)

Cambio: `scoring-motor-nucleo-v1`
Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` (formas de entrada que el
modelo exige por nivel), `proposal.md` § "El trabajo real es el adapter".

> El adapter es el foco de esfuerzo y de riesgo del cambio. El motor recibe datos con FORMA
> FINAL; esta spec declara, sin huecos, cómo se derivan esas formas desde los hechos crudos de
> `daily_activity_logs`. La spec NO toca Room (Camino A).

> NOTA DE ARCHIVO: esta delta spec quedó mergeada en el spec canónico
> `openspec/specs/scoring-facts-adapter/spec.md` al archivar el cambio (2026-06-17). Este archivo
> es el audit trail del delta tal como se planeó.

## Propósito

`scoring-facts-adapter` transforma los hechos Room de la ventana semanal en las estructuras de
entrada que el motor exige: por ancla `(F, T, mins[7])`, por soporte `días_sostenidos`
(ventana 4 días, UX inversa), por capa `n_tasks_hoy` (tasks de HOY, efímeras), por track
`días_recaída` (ventana 7 días) y la señal `M` de sueño (Cuerpo). Es la única pieza que conoce
la forma cruda de los hechos; el motor no la conoce. NO recalcula scoring; solo adapta forma.

(Requisitos completos: derivar `(F,T,mins[7])`, logs duplicados/omitidos/NotDone, soportes
ventana 4d UX inversa, tasks efímeras `n_tasks_hoy`, tracks días-recaída, sueño señal M, casos
límite — ver spec canónico `openspec/specs/scoring-facts-adapter/spec.md`. Invariante
"anclas = solo Minutes" decidido 2026-06-16.)
