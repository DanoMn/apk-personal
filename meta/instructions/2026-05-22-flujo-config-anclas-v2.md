# Flujo de Configuracion de Anclas — Iteracion 2

Fecha: 2026-05-22
Estado: pendiente de aprobacion del usuario

## Contexto humano

El usuario descubrio un flujo natural: cuando desde el dashboard va a "Registra checklist" → "Checklist principal", ahi solo aparecen las actividades ya configuradas (PrimaryChecklist). Bien. Pero el boton "Configurar actividades" abre un panel viejo (`ActivitySettingsPanel`) que mezcla muchas cosas (señales, goals, creacion). El usuario quiere que ese boton redirija a la pagina nueva de configuracion completa (`ChecklistConfigScreen`).

Ademas, la pagina completa (`ChecklistConfigScreen`) actualmente solo permite seleccionar de actividades existentes. Falta la opcion de **crear una actividad personalizada** (nombre propio, capa, tiempo opcional, meta opcional) y que quede disponible en la lista de actividades filtrable.

## Objetivo — 2 cambios concretos

### Cambio A: Redirigir "Configurar actividades" a la pagina completa

**Flujo actual:**
```
Dashboard → "Registra checklist" → "Checklist principal" → "Configurar actividades"
  → Abre DashboardSheet.Activities (ActivitySettingsPanel) — mezcla senales, goals, crear actividad
```

**Flujo nuevo:**
```
Dashboard → "Registra checklist" → "Checklist principal" → "Configurar actividades"
  → Cierra el sheet
  → Navega a ChecklistConfigScreen (pagina completa)
```

**Archivos a modificar:**
- `DashboardPanels.kt` L117-127: El `ChecklistPanel` recibe `onOpenActivities` que llama `onSwitchSheet(DashboardSheet.Activities)`. Cambiar para que cierre el sheet y navegue a la pagina.
- `DashboardScreen.kt`: Pasar `onNavigateToChecklistConfig` al `DashboardSheetHost` de alguna forma. Opcion: agregar un callback `onNavigateToChecklistConfig` al `DashboardSheetHost` y que `ChecklistPanel` lo use.
- `DashboardPanels.kt` L65-82: `DashboardSheetHost` necesita recibir `onNavigateToChecklistConfig`.

### Cambio B: Agregar creacion de actividad personalizada a ChecklistConfigScreen

**Estado actual de `ChecklistConfigScreen`:**
- Lista de anclas actuales (colapsable)
- Busqueda
- Actividades disponibles con "Agregar"
- Filtros de capa
- Dialog de configuracion (tiempo + meta opcionales)

**Falta:**
- Seccion/boton "Crear nueva actividad" que permita:
  1. Nombre libre (campo de texto)
  2. Seleccionar capa (5 botones de capa — mismos que ya estan en la pagina)
  3. Tiempo objetivo (opcional, campo numerico o ±5 min)
  4. Meta semanal/mensual (opcional, mismos GoalOption que ya existen)
  5. Boton "Crear y agregar" que:
     - Crea la actividad en Room via `onCreateActivity`
     - La marca como PrimaryChecklist automaticamente

**Archivo a modificar:**
- `ChecklistConfigScreen.kt`: Agregar seccion de creacion debajo de la lista de actividades disponibles. Puede ser un card/seccion colapsable "Crear actividad personalizada" con el formulario inline.

**Callbacks necesarios:**
- `onCreateActivity: (name: String, layerId: String, targetMinutes: Int, isSecondary: Boolean, isGoal: Boolean, isMonthlyGoal: Boolean) -> Unit` — ya existe en el ViewModel. Se necesita pasar a `ChecklistConfigScreen`.

## Referencia tecnica directa

### Flujo de sheets actual
- `DashboardSheet.Checklist` → `ChecklistPanel` (L117-127)
  - `onOpenActivities = { onSwitchSheet(DashboardSheet.Activities) }` — ESTE ES EL QUE DEBE CAMBIAR
- `ChecklistPanel` (L180-222) tiene `SheetButton("Configurar actividades")` en L215-220

### Creacion de actividad existente
- `ActivitySettingsPanel` (L492-646) tiene formulario completo L569-644
  - `PanelField("Nombre", ...)` — campo de texto
  - Layer selector — row de 5 botones `layer.name.take(4)`
  - `PanelField("Minutos", ...)` — campo numerico
  - Checkboxes: Secundaria, Goal, Mensual
  - `SheetButton("Agregar actividad")` — llama `onCreateActivity`

### Repository
- `createActivity(name, layerId, targetMinutes, displaySurface, isGoal, isMonthlyGoal)` en `AutonomiaRepository.kt` L138-182

### ViewModel
- `createActivity(name, layerId, targetMinutes, isSecondary, isGoal, isMonthlyGoal)` en `DashboardViewModel.kt` L184-206

## Preguntas para el usuario

Ninguna. El flujo esta claro:
1. "Configurar actividades" desde el checklist del dashboard → navega a la pagina completa.
2. La pagina completa incluye creacion de actividades personalizadas con nombre, capa, tiempo y meta opcionales.
