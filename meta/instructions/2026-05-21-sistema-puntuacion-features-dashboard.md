# Pro-Prompt: Sistema de Puntuacion y Features del Dashboard

Este documento es la instruccion refinada para implementar el sistema de puntuacion de `Vocal / Autonomia sin limites` sin volver a mezclar capas, features y UI.

## Contexto humano

El usuario aclaro que la app no es un juego ni una lista de productividad facil de explotar. El score debe ser un indicador duro pero compasivo de la base personal. El tono cuida al usuario, pero el algoritmo debe mostrar la realidad con claridad.

El error a evitar es tratar sueno, sobriedad, checklist, tasklist o semana como "pilares" paralelos al core. El core son las capas: `Interior`, `Cuerpo`, `Conducta`, `Vinculos`, `Proyecto`.

## Fuentes obligatorias

- `AGENTS.md`
- `docs/tono-comunicacion.md`
- `docs/decisiones-capas-actividades-v1.md`
- `docs/prototipo/dashboard.html`
- `docs/prototipo/score-states.html`
- `app/src/main/java/dev/panopt/autonomia/Models.kt`
- `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardInference.kt`

## Implementacion esperada

- Crear `domain/scoring` para sacar la formula de score fuera de `ui/dashboard`.
- Agregar tipos de dominio: `ScoreReport`, `LayerScore`, `FeatureContribution`, `ScoreGate`.
- El score global sale de las 5 capas canonicas.
- Room sigue guardando hechos: `ActivityLog`, `SleepLog`, `AbstinenceLog`, `Task`.
- Compose solo consume `DashboardState` y envia intents.
- Mantener `score-states.html` como fuente de rangos visibles:
  - `NoData`: sin numero.
  - `Restoration`: 700-749.
  - `Attention`: 750-799.
  - `Motion`: 800-899.
  - `Plenitude`: 900-949.
  - `Unbreakable`: 950-1000.

## Reglas de dominio v1

- Base visible: 700-900 por promedio de capas.
- Bonus de goals: 0-100 desde actividades `Weekly`/`Monthly` o `targetPeriod` `Week`/`Month`.
- `Plenitude` e `Unbreakable` requieren base estable mas goals sostenidos.
- Checklist principal pesa mas que checklist secundaria.
- Checklist secundaria tiene tope diario.
- `Task` pesa menos y solo suma si tiene `layerId` y `contributionRole != Neutral`.
- Sueno puntua por duracion, cercania al horario planificado y calidad subjetiva.
- Sueno bajo o ausente puede impedir estados altos sin borrar lo demas.
- Sobriedad alimenta `Conducta`; recaida hoy pesa fuerte y racha limpia sostenida desbloquea estados altos.
- El boton rojo abre recaidas de abstinencias activas; no debe ser riesgo generico.

## Pruebas obligatorias

- Sin datos devuelve `NoData`.
- Los rangos visibles respetan `score-states.html`.
- Sueno bajo impide estados altos.
- Recaida hoy baja/capea el estado y la racha bloquea estados altos.
- Checklist principal pesa mas que secundaria y tasks.
- Tasks neutrales no suman.
- Goals semanales/mensuales elevan a `Plenitude`/`Unbreakable`.

## Criterio de cierre

El proyecto debe compilar con `:app:assembleDebug` y el motor de score debe pasar tests unitarios con `:app:testDebugUnitTest`.
