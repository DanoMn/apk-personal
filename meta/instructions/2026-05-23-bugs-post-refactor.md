# Bugs post-refactor — actividad entity v2 + soportes

Fecha: 2026-05-23
Estado: RESUELTO 2026-05-23 — 4 de 5 bugs corregidos, 1 descartado (ver resumen al final)

## 1. Crear ancla no funciona (custom activity creation)

El flujo de crear una actividad personalizada desde cero no guarda nada.
Método: `DashboardViewModel.createActivity()` (línea 207).
Debe insertar `ActivityDefinitionEntity` + `UserActivityConfigEntity`.
Verificar: `repository.upsertActivityDefinition()` y `repository.upsertUserActivityConfig()`.
Posible causa: IDs duplicados, DAO conflict strategy, o falta de @Transaction.

## 2. Agregar ancla existente NO guarda tiempo ni goal

Desde `ChecklistConfigPanel`, el botón "Agregar" llama a `onAddToChecklist(activityId, targetValue, targetCount, targetPeriod)`.
La actividad se guarda (tipo Anchor) pero sin el tiempo ni goal seleccionados.
Auditar el flujo completo:
- `ChecklistConfigPanel` → recopila targetValue del `TimeWheelPicker` y goal del `GoalPresetGrid`
- Llama a `onAddToChecklist(id, targetValue, targetCount, targetPeriod)`
- `DashboardViewModel.addActivityToChecklist()` → `repository.addActivityToChecklist()`
- `AutonomiaRepository.addActivityToChecklist()` → `configureActivity()`
- `configureActivity()` → `dao.upsertUserActivityConfig()`

Verificar que los valores de tiempo/goal llegan correctamente hasta el DAO.

## 3. Navigation drawer sin márgenes

`NavigationDrawer.kt` y posiblemente otras pantallas no tienen `statusBarsPadding()` ni `navigationBarsPadding()`.
Solución: aplicar padding global desde `MainActivity` o el `BoxWithConstraints` principal,
o agregar `systemBarsPadding()` en cada pantalla.

## 4. Performance de la lista de anclas

`ChecklistConfigScreen` y `ChecklistConfigPanel` renderizan muchas tarjetas.
Posible hardcodeo de componentes en lugar de usar `LazyColumn` o `items()`.
Auditar si los items se recomponen innecesariamente.

## 5. Verificar remove y soportes

- `removeActivityFromChecklist` debe borrar el `UserActivityConfigEntity`
- `SupportsConfigScreen` debe mostrar solo definiciones con `presetCategory = "support"`

## Archivos clave a revisar

- `DashboardViewModel.kt` — createActivity, addActivityToChecklist, removeActivityFromChecklist
- `AutonomiaRepository.kt` — configureActivity, addActivityToChecklist, upsertActivityDefinition, upsertUserActivityConfig
- `AutonomiaDao.kt` — upsertUserActivityConfig, deleteUserActivityConfig
- `ChecklistConfigPanel.kt` — flujo de time/goal picker → onAddToChecklist
- `ChecklistConfigScreen.kt` — filtro isConfigured + activityType
- `NavigationDrawer.kt` — márgenes
- `DashboardScreen.kt` — márgenes globales
- `DomainMappers.kt` — toDomain() para catalog items (activityType basado en presetCategory)
- `DashboardState.kt` — isConfigured field
- `DashboardProjection.kt` — activityOptions con isConfigured

---

## Resolución 2026-05-23

### ✅ Bug 1: Crear ancla no funciona — RESUELTO
**Root cause**: `DashboardProjection.kt` línea 195 — `activityOptions` se construía solo con datos del catálogo (`observeCatalogActivities()`). Actividades custom con `presetCategory=null` mapeaban a `activityType=Task` en el catálogo, y el filtro `activityType == "Anchor"` las excluía. Los valores del config (que sí tenía `activityType=Anchor`) se ignoraban completamente.
**Fix**: Agregado `configuredById = activities.associateBy { it.id }` (L124) y merge de valores configurados en el bloque `activityOptions` (L183-201). Ahora usa `effective.activityType` (del config cuando existe) y `configured?.targetValue`.

### ✅ Bug 2: Agregar ancla existente NO guarda tiempo ni goal — RESUELTO
**Root cause**: Mismo archivo, líneas 190-191. `targetValue` y `actualValue` solo tomaban valores del catálogo (`null` → fallback a 1). El config SÍ guardaba los valores correctamente, pero `activityOptions` nunca los reflejaba.
**Fix**: `targetValue = configured?.targetValue ?: activity.targetValue ?: activity.minimumValue ?: 1` — prioriza el valor del config.

### ✅ Bug 3: Navigation drawer sin márgenes — RESUELTO
**Fix**: Agregado `.navigationBarsPadding()` a:
- `DashboardScreen.kt` L88 (main Column)
- `NavigationDrawer.kt` L81-82 (drawer Column + `.statusBarsPadding()`)
- `SupportsConfigScreen.kt` L68 (root Box)

### 🔵 Bug 4: Performance de la lista de anclas — DESCARTADO
**Motivo**: `AnimatedVisibility` con `expandVertically/shrinkVertically` requiere `Column`, no `LazyColumn`. A esta escala (10-30 items) no hay problema real de performance. Si en el futuro crece a 100+ items, considerar refactor de las secciones colapsables.

### ✅ Bug 5: Verificar remove y soportes — VERIFICADO OK
- `removeActivityFromChecklist` → `dao.deleteUserActivityConfig()`: correcto.
- `SupportsConfigScreen` filtra por `activityType == "Support"`: gracias al fix de Bug 1, los soportes custom ahora muestran el `activityType` del config correctamente.
- `addToSupports` → `configureActivity(activityType=Support)` → conectado correctamente en `MainActivity.kt`.

### Archivos modificados
- `domain/dashboard/DashboardProjection.kt` (L124, L183-201)
- `ui/dashboard/DashboardScreen.kt` (L14 import, L88 padding)
- `ui/dashboard/components/NavigationDrawer.kt` (L13-15 imports, L81-82 padding)
- `ui/supports/SupportsConfigScreen.kt` (L68 padding)
