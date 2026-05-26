# Sobriedad V1 - Feature opt-in funcional

## Prompt real de la iteracion

Implementar Sobriedad como feature propia opt-in:

- Presets inactivos: Alcohol, Sustancias, Conducta sexual.
- Pantalla profunda accesible desde dashboard/drawer.
- Dashboard muestra solo rachas activas y permite marcar limpio.
- Panel de recaidas registra o desmarca recaida del dia actual para rachas activas.
- Tracks personalizados pueden crearse activos y eliminarse.

## Contrato tecnico

- Room guarda `AbstinenceTrackEntity` y `AbstinenceLogEntity`.
- `AbstinencePolicy` valida nombres custom, reconoce presets y bloquea eliminacion de presets.
- Repositorio valida que solo tracks activos reciban logs diarios.
- `DashboardState.sobrietyTracks` contiene solo activas.
- `DashboardState.sobrietyOptions` contiene activas e inactivas para la pantalla profunda.
- `ScoreEngine` ignora logs de tracks inactivos incluso para decidir `NoData`.

## Fuera de alcance V1

- Sistema de olvido de 2-3 dias.
- Recaidas multi-dia o correccion de duracion por version del usuario.
- Cambios mayores a formula de scoring.
