# Pro-Prompt: UX y persistencia de Mis anclas

## Pedido del usuario

Mejorar la pantalla de configuracion de `Mis anclas`, enfocada en el flujo de agregar, crear, quitar y eliminar anclas, ademas de mejorar la seleccion de metas y el selector de tiempo.

## Contexto tecnico auditado

- Pantalla principal: `app/src/main/java/dev/panopt/autonomia/ui/anchors/AnchorConfigScreen.kt`.
- Sheet rapido relacionado: `app/src/main/java/dev/panopt/autonomia/ui/dashboard/components/AnchorConfigPanel.kt`.
- Presets de metas: `app/src/main/java/dev/panopt/autonomia/ui/anchors/GoalPreset.kt`.
- Grid de metas: `app/src/main/java/dev/panopt/autonomia/ui/anchors/GoalPresetGrid.kt`.
- Selector de tiempo: `app/src/main/java/dev/panopt/autonomia/ui/anchors/TimeWheelPicker.kt`.
- Persistencia: `AutonomiaDao`, `AutonomiaRepository`, `DashboardViewModel`.
- La app usa `ActivityDefinitionEntity` como catalogo y `UserActivityConfigEntity` como configuracion del usuario.

## Diagnostico

1. Despues de agregar o crear un ancla, la UI vuelve al listado sin forzar `Anclas actuales` expandido ni resaltar la actividad recien creada.
2. `onDeleteActivity` llama a `deleteUserActivityConfig`, por lo que no elimina la `ActivityDefinitionEntity` custom. Eso hace que la actividad custom quede en el catalogo.
3. La eliminacion por `X` no pide confirmacion.
4. La matriz semanal empieza en 2 y llega a 7, pero debe ser 1 a 6.
5. La matriz mensual puede ser mas clara con incrementos: 1, 2, 4, 6, 8, 12.
6. El modo `Personalizada` actualmente abre un control inline pobre; debe abrir un mini menu superpuesto.
7. El `TimeWheelPicker` tiene offset inicial incorrecto y demasiado aire vertical; las letras `h` y `m` deben ser mas legibles.
8. Se puede agregar un gesto de arrastre hacia abajo en la zona central/baja de la pantalla para expandir `Anclas actuales`.

## Prompt real adoptado

Implementar una mejora integral y acotada de `Mis anclas`:

- Al guardar un ancla existente o crear una actividad custom, volver al listado, expandir `Anclas actuales` y hacer un destello sutil en la tarjeta nueva.
- Mantener `Quitar` como desconfiguracion: borra solo `UserActivityConfigEntity`.
- Hacer que `Eliminar` de actividades custom pida confirmacion y borre la definicion custom completa, incluyendo su config por cascada.
- Mostrar la opcion de eliminar custom tanto en `Anclas actuales` como en `Anclas disponibles`.
- Agregar gesto vertical hacia abajo para desplegar `Anclas actuales` desde zona central/baja.
- Corregir presets semanales a 1..6 y mensuales a 1, 2, 4, 6, 8, 12.
- Reemplazar el selector `Personalizada` inline por un dialog/mini menu superpuesto con numero y periodo claros.
- Ajustar `TimeWheelPicker` para centrar visualmente el numero, reducir altura vacia y agrandar `h`/`m`.
- Compilar con `assembleDebug` configurando `JAVA_HOME` segun `AGENTS.md`.
