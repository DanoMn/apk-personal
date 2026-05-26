# Pendientes V1 - Lista operativa, no configuracion rapida

## Prompt real de la iteracion

Implementar Pendientes como lista operativa:

- Dashboard muestra `Pendientes` debajo de `Soportes`.
- Dashboard solo lista tareas `Pending`, permite completar y abre la pantalla profunda.
- Configuracion rapida no muestra tarjeta de Pendientes.
- Pantalla Pendientes permite crear una task rapida, elegir capa opcional, completar y revivir completadas.
- Room mantiene hechos; dominio/nucleo separa `Pending` y `Done`; Compose no filtra reglas de negocio.

## Contrato tecnico

- Crear `domain/task/TaskPolicy.kt`.
- `TaskPolicy.createDraft()` normaliza titulo, rechaza vacios y decide aporte:
  - sin capa: `ContributionRole.Neutral`;
  - con capa: `ContributionRole.Support`.
- Repositorio:
  - `createTask(title, layerId)` usa la politica;
  - `completeTask(taskId)` solo permite `Pending -> Done`;
  - `reactivateTask(taskId)` solo permite `Done -> Pending`.
- `DashboardState` expone `pendingTasks` y `completedTasks`.
- `DashboardProjection` separa estados de task.
- El drawer y la seccion dashboard navegan a `AppScreen.Tasks`, no a un bottom sheet.
- Eliminar `DashboardSheet.Tasks` y `TasksPanel` si quedan sin uso.

## Verificacion esperada

- `TaskPolicyTest`.
- `DashboardProjectionTest`.
- `ScoreEngineTest`.
- `:app:compileDebugKotlin --no-daemon`.
