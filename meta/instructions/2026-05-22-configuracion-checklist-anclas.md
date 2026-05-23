# Pantalla de Configuracion de Checklist / Mis Anclas

Fecha: 2026-05-22
Estado: pendiente de aprobacion del usuario

## Contexto humano

El usuario quiere una pantalla de configuracion del checklist principal ("Mis anclas"). No es la pantalla de registro diario, sino la pantalla donde el usuario **configura** que actividades forman su base diaria.

El acceso inicial sera desde el drawer lateral (link "Checklist" o "Configuracion"), usando el patron de bottom sheet existente. Mas adelante puede moverse a una pantalla completa o ruta propia.

## Objetivo

Crear un nuevo `DashboardSheet` (o panel full-screen dentro del sheet host) donde el usuario pueda:

1. Ver sus anclas actuales (actividades con `displaySurface = PrimaryChecklist`)
2. Explorar todas las actividades disponibles en la base de datos
3. Filtrar actividades por capa (5 botones de capa en la parte inferior)
4. Buscar actividades por texto
5. Seleccionar una actividad para agregarla al checklist
6. Configurar tiempo objetivo (si aplica) y meta semanal/mensual (opcional)
7. Guardar la configuracion

## Referencia tecnica directa

### Arquitectura existente

- **DashboardSheetHost**: `DashboardPanels.kt` L58-161. Switch sobre `DashboardSheet` enum. Patron bottom sheet con backdrop + column + handle.
- **DashboardSheet enum**: `DashboardPanels.kt` L48-56. Agregar nuevo valor `ChecklistConfig` (o reusar `Activities` y expandirlo).
- **DashboardScreen**: `DashboardScreen.kt`. Orquesta sheets via `activeSheet`.
- **NavigationDrawer**: `NavigationDrawer.kt` L60-231. Links del drawer disparan callbacks como `onOpenChecklist`, `onOpenActivitySettings`.
- **DashboardPalette**: `DashboardPalette.kt`. Colores por capa: `layerInterior`, `layerBody`, `layerConduct`, `layerVinculos`, `layerProject`.

### Datos

- **ActivityEntity**: `Entities.kt` L16-41. Campos: `id`, `layerId`, `name`, `type`, `targetValue`, `displaySurface`, `active`, `targetCount`, `targetPeriod`.
- **LayerEntity**: `Entities.kt` L7-14. Campos: `id`, `name`.
- **DefaultSeeds**: `DefaultSeeds.kt`. 8 actividades seed, 5 capas.
- **DashboardActivityOptionState**: `DashboardState.kt` L131-142. Modelo UI existente para opciones de actividad.
- **DashboardState**: `DashboardState.kt` L6-22. Ya contiene `activityOptions`, `layers`, `checklistItems`.

### Enums relevantes

- **DisplaySurface**: `PrimaryChecklist`, `SecondaryChecklist`, `Compact`, `Contextual`, `Silent`.
- **ActivityType**: `Check`, `Time`, `Count`, `Note`, `TimeOfDay`, `SelfCare`, `AbstinenceSupport`, `Weekly`.
- **ActivityCadence**: `Daily`, `Weekly`, `Monthly`, `Custom`, `EventBased`.

## Diseno de UI propuesto

### Estructura del panel (bottom sheet expandido)

```text
┌─────────────────────────────────┐
│         ══ handle ══            │
│                                 │
│  Mis anclas           4 activas │  ← SheetTitle
│                                 │
│  ┌─ Anclas actuales (colapsable)│
│  │ ☑ Meditar          5 min  ◆  │  ← sello Interior
│  │ ☑ Ejercicio       40 min  ≋  │  ← sello Cuerpo
│  │ ☑ Digitaliza     360 min  △  │  ← sello Proyecto
│  │ ☑ Musica         180 min  △  │  ← sello Proyecto
│  └──────────────────────────────│
│                                 │
│                                 │
│  ─── Todas las actividades ──── │
│  □ Cepillarse dientes    2x  ≋  │
│  □ Banarse                   ≋  │
│  □ Cocinar en casa           ≋  │
│  □ Limpiar los trastes       ∞  │
│                                 │
│  ┌──────────────────────────┐   │
│  │ ◆Int │ ≋Cpo │ ∞Con │ ♦Vin│△Pro│  ← filtros de capa
│  └──────────────────────────┘   │
│  🔍 Buscar actividad...         │  ← campo de busqueda
└─────────────────────────────────┘
```

### Al hacer tap en una actividad no seleccionada

Se levanta un sub-panel / dialog / card encima:

```text
┌─────────────────────────────────┐
│  Agregar a mis anclas           │
│                                 │
│  Cepillarse los dientes    ≋   │
│  Capa: Cuerpo                   │
│                                 │
│  Tiempo objetivo (si aplica)    │
│  ┌────────────────────────┐     │
│  │     ▲                  │     │
│  │    05 min              │     │
│  │     ▼                  │     │
│  └────────────────────────┘     │
│                                 │
│  Meta semanal (opcional)        │
│  ○ Sin meta                     │
│  ○ 3 veces / semana             │
│  ○ 5 veces / semana             │
│  ○ Personalizar: [__] veces     │
│                                 │
│  Meta mensual (opcional)        │
│  ○ Sin meta                     │
│  ○ 12 veces / mes               │
│  ○ 20 veces / mes               │
│  ○ Personalizar: [__] veces     │
│  │                              │
│  [      Guardar ancla      ]    │  ← boton primario cardboard
│  [       Cancelar          ]    │  ← boton secundario
└─────────────────────────────────┘
```

### Comportamiento

1. **Anclas actuales (seccion colapsable)**: Muestra las actividades ya en `PrimaryChecklist`, con sello de capa pequeno. Se colapsa automaticamente cuando el usuario busca o filtra. Se puede expandir/colapsar con tap.
2. **Busqueda**: `BasicTextField` con icono de lupa. Filtra `activityOptions` por `name` (case-insensitive, contains).
3. **Filtros de capa**: 5 botones horizontales en la parte inferior fija del panel. Cada uno con color de capa + icono de sello + nombre. Toggle: si se toca uno activo, se desactiva (muestra todas). Si se toca uno inactivo, filtra solo esa capa.
4. **Seleccion de actividad**: Tap sobre una actividad no seleccionada abre un sub-panel/dialog (puede ser otro sheet apilado o un dialog sobre el sheet).
5. **Configuracion de tiempo**: Solo aparece si `activityType == Time`. Selector de minutos estilo scroll/picker o input numerico. **No es obligatorio**. El usuario puede guardar sin definir tiempo.
6. **Meta semanal/mensual**: Opciones predefinidas + personalizar. Se guarda en `targetCount` + `targetPeriod` (o `cadence`). **No es obligatorio**. El usuario puede omitir y guardar directamente.
7. **Guardar**: Actualiza `displaySurface` de la actividad a `PrimaryChecklist` (si se agrega) o lo quita (si se remueve). Persiste en Room. El usuario puede guardar sin configurar tiempo ni goals.
8. **Quitar ancla**: Desde la seccion de anclas actuales, long-press o swipe para quitar. Cambia `displaySurface` a `SecondaryChecklist` o `Silent`.

## Archivos a crear/modificar

### Nuevo archivo
- `ui/dashboard/components/ChecklistConfigPanel.kt` — Composable principal del panel de configuracion.

### Modificar
- `DashboardPanels.kt` — Agregar `ChecklistConfig` al enum `DashboardSheet` y al `when` de `DashboardSheetHost`.
- `DashboardScreen.kt` — Conectar el nuevo sheet desde el drawer (ya tiene `onOpenChecklist`).
- `NavigationDrawer.kt` — El link "Checklist" del drawer debe apuntar al nuevo `DashboardSheet.ChecklistConfig` en lugar de `DashboardSheet.Checklist` (o agregar entrada separada).
- `DashboardViewModel.kt` — Agregar acciones para actualizar `displaySurface`, `targetValue`, `targetCount`, `targetPeriod` de una actividad.
- `AutonomiaRepository.kt` — Agregar metodo para actualizar configuracion de actividad.
- `AutonomiaDao.kt` — Query para update parcial de actividad.

## Preguntas abiertas para el usuario

1. **Drawer link**: El link "Checklist" del drawer actualmente abre el panel de registro diario (`DashboardSheet.Checklist`). ¿Quieres que el nuevo panel de configuracion reemplace ese link, o agregar un link nuevo tipo "Configurar anclas"?

2. **Quitar ancla**: ¿Como debe funcionar quitar una actividad del checklist principal? ¿Long-press, swipe, o un boton de quitar visible?

3. **Actividades nuevas**: ¿Se debe poder crear actividades personalizadas desde esta pantalla, o solo seleccionar de las existentes en la BD?

4. **Panel apilado vs dialog**: Cuando el usuario toca una actividad para configurarla, ¿prefieres un dialog centrado o un segundo sheet que se apile sobre el primero?

5. **Limite de anclas**: El dominio dice "deben ser pocas". ¿Definimos un maximo (ej. 7-10 anclas) o dejamos sin limite por ahora?
