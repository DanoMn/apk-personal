# Specs — anchor-phrase-rotation

> **Estado SDD:** archivado (2026-06-04)
> **Proyecto:** apk-personal
> **Fuente:** `docs/dominio/frases-ancla.md` (vivo) · `meta/instructions/2026-06-04-rotacion-frases-ancla.md`

## Dominios especificados

| Dominio | Archivo | Requisitos | Escenarios | Estado final |
|---|---|---|---|---|
| Política de fase del día | [day-phase-policy/spec.md](day-phase-policy/spec.md) | 2 | 5 | MERGED a main specs |
| Selector puro ponderado | [anchor-phrase-selector/spec.md](anchor-phrase-selector/spec.md) | 4 | 9 | MERGED a main specs |
| Resolver (coordinador de datos) | [anchor-phrase-resolver/spec.md](anchor-phrase-resolver/spec.md) | 5 | 5 | MERGED a main specs |
| Seed canónico (invariantes) | [anchor-phrase-seed/spec.md](anchor-phrase-seed/spec.md) | 4 | 7 | MERGED a main specs |
| Migración de las 5 tablas | [anchor-phrase-migration/spec.md](anchor-phrase-migration/spec.md) | 3 | 3 | DROPPED — Camino A (ver nota abajo) |
| Integración dashboard | [dashboard-integration/spec.md](dashboard-integration/spec.md) | 4 | 6 | MERGED a main specs |
| **Total mergeado** | | **19** | **32** | |

> **Nota Camino A:** La spec `anchor-phrase-migration` NO se mergeó a main specs. La migración v12→v13
> fue planificada y parcialmente implementada, pero REVERTIDA por decisión de proyecto: en fase dev,
> DB descartable, sin migraciones Room manuales. DB permanece en v12. La 5 tablas existen en el
> esquema v12 y se crean en instalación limpia. El androidTest y `schemas/13.json` fueron eliminados.
> Este bug latente de release queda abierto para una futura issue/cambio SDD antes de liberar.

## Invariantes clave (no negociables — implementados)

- `DayPhase.Dawn` = 05:00–14:59 local; `DayPhase.Dusk` = 15:00–04:59 local. Bordes explícitos en 14:59 y 15:00.
- `Contemplation` SOLO aparece en `Plenitude` y `Unbreakable` (§8.6). Ningún otro estado puede recibirla.
- Relajación de ventana: primero se relaja la ventana de 7 días; NUNCA las reglas de estado.
- Seed: 83 frases activas, 0 con `authorReference` vacío; reglas derivadas de mapas, no escritas a mano.
- Dashboard: sin frase hardcodeada; el slot se lee reactivo vía `Flow`; el resolver corre después del snapshot.

## Fuera de alcance

Editor de frases propias · personalización de familias · explicación de desbloqueos ·
rotación por apertura · geolocalización · algoritmo complejo por capa/riesgo/sobriedad.
