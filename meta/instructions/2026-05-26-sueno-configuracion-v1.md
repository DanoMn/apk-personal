# Sueno v1 - Configuracion Primero

## Objetivo

Implementar la pagina profunda de configuracion de Sueno y reducir el panel rapido del dashboard a registro de hechos.

## Contrato de arquitectura

- La configuracion valida limites antes de escribir en Room.
- Room guarda configuracion y hechos.
- El dominio calcula senales y scoring.
- El dashboard expone estado y registra acciones rapidas.

## Alcance implementado

- Crear `SleepConfigEntity` con fila unica `default`.
- Agregar `sleepConfigFlow()` y `saveSleepConfig(...)` al repositorio.
- Cambiar `saveSleepLog(...)` para tomar snapshot de la configuracion vigente.
- Mantener `SleepLogEntity.quality` temporalmente como `Acceptable`.
- Crear `SleepConfigScreen` conectada desde drawer y desde el panel de Sueno.
- Agregar `Ir a dormir` / `Desperte` como acciones automaticas en dashboard, configuracion rapida y panel de Sueno.
- Quitar calidad, objetivos editables y registro manual de horas del `SleepPanel`.
- Guardar la sesion pendiente en `sleep_session_state` hasta que el usuario toque `Desperte`.
- Actualizar `SleepScoring` y la senal de Sueno para no depender de calidad subjetiva.

## Fuera de alcance

- Bloqueo real del telefono.
- Device Admin, UsageStats, modo kiosco o sesiones multiples.
- Alarmas y compromiso asistido.
