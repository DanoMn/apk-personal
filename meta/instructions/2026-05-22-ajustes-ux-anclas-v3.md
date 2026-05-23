# Ajustes UX en Pagina de Anclas y Sheet Rapido — v3

Fecha: 2026-05-22
Estado: pendiente de aprobacion del usuario

## Contexto humano

El usuario probo la pagina de "Mis anclas" y el sheet rapido. Encontro 7 problemas de usabilidad que necesitan correccion.

## Cambios — lista ordenada

### 1. Tiempo empieza en 0 (no en 1)
**Problema:** `coerceAtLeast(1)` impide dejar 0 min. El default es 15 min.
**Solucion:** Cambiar default a 0, coerceAtLeast a 0. Si es 0, la actividad no tiene objetivo de tiempo.

**Archivos:**
- `ChecklistConfigScreen.kt` L498: `mutableIntStateOf(if (hasTime) activity.targetValue else 15)` → else `0`
- `ChecklistConfigScreen.kt` L564: `.coerceAtLeast(1)` → `.coerceAtLeast(0)`
- `ChecklistConfigPanel.kt` (sheet rapido): mismos cambios en `ActivityConfigDialog`

### 2. Controles mas accesibles (zona de pulgar)
**Problema:** Botones +/-, metas y "Guardar ancla" estan muy arriba, incómodos para los dedos.
**Solucion:** Reorganizar el `ActivityConfigSection` para que los controles de accion queden en la zona inferior. Layout: info de actividad arriba (compacta) → contenido scrollable → botones de accion fijos abajo.

**Archivos:**
- `ChecklistConfigScreen.kt`: `ActivityConfigSection` — reestructurar con `weight(1f)` para scroll + botones fijos abajo.
- `ChecklistConfigPanel.kt`: `ActivityConfigDialog` — misma logica.

### 3. Meta personalizable
**Problema:** Solo hay opciones predeterminadas (3/sem, 5/sem, 12/mes, 20/mes). Falta "Personalizada".
**Solucion:** Agregar `GoalOption.Custom` que muestre un campo numerico + selector semanal/mensual.

**Archivos:**
- `ChecklistConfigScreen.kt`: Nuevo estado `customCount` y `customPeriod`. Nuevo entry `Custom("Personalizada")` en `GoalOption`. UI: cuando se selecciona Custom, mostrar campo de conteo + toggle semanal/mensual.
- `ChecklistConfigPanel.kt`: Misma logica replicada.

### 4. Iconos y texto de capas mas visibles
**Problema:** `.take(5)` trunca los nombres. Los iconos de 22dp son muy chicos.
**Solucion:** Ampliar a `.take(7)` o nombre completo. Subir iconos a 26dp. Subir texto a 11.5sp.

**Archivos:**
- `ChecklistConfigScreen.kt`: L358: `layer.name.take(5)` → `layer.name`. L352: `size = 22` → `size = 26`. L360: `10.5.sp` → `11.5.sp`. Lo mismo en el layer selector de `CreateCustomActivitySection`.
- `ChecklistConfigPanel.kt`: Mismos ajustes en los filtros de capa.

### 5. Buscador encima de los filtros de capa
**Problema:** El buscador esta arriba del listado pero los filtros de capa estan abajo (pineados). Es contra-intuitivo buscar arriba y filtrar abajo.
**Solucion:** Mover los filtros de capa justo debajo del buscador (dentro del scroll, no fijos abajo). Esto crea un bloque solido: busqueda → filtro de capa → lista de resultados.

**Archivos:**
- `ChecklistConfigScreen.kt`: Mover el bloque de `layers.forEach` de la zona fija inferior al bloque scrollable, justo debajo del buscador.
- `ChecklistConfigPanel.kt`: Misma reorganizacion.

### 6. Borrar actividades personalizadas
**Problema:** No hay forma de eliminar una actividad custom que el usuario creo por error. Las predeterminadas (seed) no deben poder borrarse.
**Solucion:** Las actividades seed tienen IDs `act_*`. Las custom usan UUID. Mostrar boton "Eliminar" solo si `!id.startsWith("act_")`.

**Archivos (backend):**
- `AutonomiaDao.kt`: Agregar `@Query("DELETE FROM activities WHERE id = :activityId") suspend fun deleteActivity(activityId: String)`
- `AutonomiaRepository.kt`: Agregar `suspend fun deleteActivity(activityId: String) { dao.deleteActivity(activityId) }`
- `DashboardViewModel.kt`: Agregar `fun deleteActivity(activityId: String) { viewModelScope.launch { repository.deleteActivity(activityId) } }`

**Archivos (UI):**
- `ChecklistConfigScreen.kt`: En `AnchorCard` y `AvailableActivityCard`, si `!activity.id.startsWith("act_")`, mostrar boton "Eliminar" (rojo discreto).
- Agregar callback `onDeleteActivity: (String) -> Unit` a `ChecklistConfigScreen`.
- `MainActivity.kt`: Pasar `dashboardViewModel::deleteActivity`.

### 7. Gestos de retroceso y cierre
**Problema:** El swipe back en la pagina completa funciona via `BackHandler` pero no hay animacion de swipe. El sheet no tiene gesto de swipe-down para cerrar.
**Solucion:**
- **Pagina completa**: `BackHandler` ya funciona con el gesto del sistema Android. Solo validar que `predictiveBackHandler` no interfiera.
- **Sheet rapido**: Agregar `draggable` vertical o usar Material3 `BottomSheetScaffold` / `ModalBottomSheet`. Alternativa liviana: agregar `Modifier.pointerInput` para detectar swipe-down > umbral → `onDismiss()`.

**Archivos:**
- `DashboardPanels.kt`: En `DashboardSheetHost`, agregar deteccion de swipe-down en la Column del sheet.

## Fuente de verdad unica

Todos los callbacks (`onAddToChecklist`, `onRemoveFromChecklist`, `onCreateActivity`, `onDeleteActivity`) apuntan al mismo `DashboardViewModel` → `AutonomiaRepository` → `AutonomiaDao`. Zero duplicacion.

## Preguntas

Ninguna. Los 7 cambios estan claros.
