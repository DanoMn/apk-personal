# Contraste — Soporte: Estado actual vs Estado esperado

Fecha: 2026-05-25
Propósito: comparación archivo por archivo del código actual contra lo que debe ser.
Usar ANTES de implementar. Cada sección describe el estado real (no documental) encontrado
en la auditoría del código.

---

## Archivos involucrados

| # | Archivo | Estado | Cambios necesarios |
|---|---------|--------|--------------------|
| 1 | `DefaultSeeds.kt` | Funcional con bug | 1 línea + dataset nuevo |
| 2 | `AutonomiaRepository.kt` | Funcional sin validación | Agregar validación |
| 3 | `SupportsConfigScreen.kt` | Funcional, falta filtro | Agregar filtro por capas |
| 4 | `SupportsConfigPanel` (dentro de `DashboardPanels.kt`) | Funcional, incompleto | Extraer a archivo propio + deferred save + botón redirigir |
| 5 | `DashboardViewModel.kt` | Funcional | Nuevos métodos |
| 6 | `SupportsPreviewSection.kt` | Funcional, jerarquía incorrecta | Rediseño completo de UX |
| 7 | `DashboardScreen.kt` | Funcional | Verificar conexiones |
| 8 | `ActivityPolicy.kt` | Funcional con landmine | Corregir isSupport() |
| 9 | `ActivityDefinition.kt` | Dead code | Remover displaySurface |
| 10 | `DashboardState.kt` | OK | Verificar modelo |
| 11 | `DomainMappers.kt` | OK | Sin cambios |
| 12 | `DashboardProjection.kt` | OK | Sin cambios |
| 13 | `CheckItem.kt` | OK (isInverted funciona) | Sin cambios |
| 14 | `MainActivity.kt` | OK (navegación conectada) | Sin cambios |
| 15 | `AutonomiaDao.kt` | OK | Sin cambios |

---

## 1. DefaultSeeds.kt

### Actual (líneas 142-167)

```kotlin
// === Soportes: 7 ===  ← el comentario dice 7, pero hay 8 actividades
createActivityDefinition(
    id = "act_dormir_temprano",
    layerId = "cuerpo",
    name = "Dormir temprano",
    description = "...",
    type = ActivityType.TimeOfDay.name,
    role = ActivityRole.SelfCare.name,
    unit = "Time",
    contributionRole = ContributionRole.Support.name,
    importanceTier = ImportanceTier.Medium.name,
    presetCategory = "anchor",        // ← BUG: debería ser "support"
    sortOrder = 27                    // ← BUG: colisiona con anchors de Cuerpo (20-26)
),
// ... 7 soportes correctos con presetCategory = "support"
```

### Esperado

- `act_dormir_temprano.presetCategory = "support"`
- `act_dormir_temprano.sortOrder` en rango de soportes (ej: 50+)
- 8 soportes funcionales en el catálogo
- Cuando llegue el dataset extendido: reemplazar los 8+ actuales con ~20+ actividades

### Impacto del bug

`DomainMappers.kt:79-80` mapea `presetCategory` → `ActivitySurface`:
```kotlin
"anchor" -> ActivitySurface.Anchor
"support" -> ActivitySurface.Support
```

`act_dormir_temprano` con `presetCategory = "anchor"` → `activityType = Anchor` → NUNCA aparece como opción de soporte en el catálogo.

---

## 2. AutonomiaRepository.kt

### Actual

No existe un método `addSupport()`. Lo que ocurre:

```kotlin
// DashboardViewModel.kt:339-346
fun addToSupports(activityId: String) {
    viewModelScope.launch {
        repository.configureActivity(
            activityId = activityId,
            activityType = ActivitySurface.Support
            // NO valida: capa, duplicados, existencia
        )
    }
}
```

`configureActivity()` (línea ~316) hace un upsert directo sin verificaciones.

### Esperado

```kotlin
// AutonomiaRepository.kt — NUEVO
suspend fun addSupport(activityId: String, layerId: String) {
    // 1. Validar que la actividad existe en el catálogo
    // 2. Validar que la capa es válida (no null)
    // 3. Validar que no está ya configurada con otro tipo
    // 4. Guardar con activityType = Support, targets = null
    dao.upsertUserActivityConfig(
        UserActivityConfigEntity(
            activityId = activityId,
            activityType = ActivitySurface.Support.name,
            active = true,
            archived = false,
            // targets: todos null (por diseño del dominio)
            weeklyFrequencyTarget = null,
            sessionTargetMinutes = null,
            commitmentDurationMonths = null,
            targetValue = null,
            targetCount = null,
            targetPeriod = null,
            cadence = null
        )
    )
}

suspend fun removeSupport(activityId: String) {
    // Validar que está configurada como Support
    val config = dao.getConfig(activityId)
    if (config?.activityType == ActivitySurface.Support.name) {
        dao.deleteUserActivityConfig(activityId)
    }
}
```

---

## 3. SupportsConfigScreen.kt

### Actual

Pantalla completa funcional con:
- Lista de soportes configurados (con botón Quitar)
- Catálogo de actividades disponibles filtradas (las configuradas no aparecen)
- Botón "+ Crear soporte personalizado" con nombre + capa
- Add/remove vía ViewModel

**Falta**: filtro por capas. No hay chips ni pestañas. Todas las actividades se muestran juntas sin agrupar por capa.

### Esperado

Mantiene todo lo actual y agrega:

```
┌─────────────────────────────────┐
│ ← Soportes                      │  TopBar
├─────────────────────────────────┤
│ [Interior] [Cuerpo] [Conducta]  │  Chips de filtro por capa
│ [Vínculos] [Proyecto]           │  (mismo patrón que AnchorConfigScreen)
├─────────────────────────────────┤
│ Mis soportes (Cuerpo)           │  Sección filtrada por capa
│  ☐ Bañarse              [Quitar]│
│  ☐ Tomar agua           [Quitar]│
├─────────────────────────────────┤
│ Catálogo (Cuerpo)               │  Solo actividades de la capa activa
│  ☐ Cepillarse los dientes [Agregar]│  que no están ya configuradas
│  ☐ Comer algo decente    [Agregar]│
├─────────────────────────────────┤
│ [+ Crear soporte personalizado] │  Diálogo con capa preseleccionada
└─────────────────────────────────┘
```

**Comportamiento del filtro**:
- Al seleccionar "Cuerpo": Mis soportes y Catálogo solo muestran actividades de Cuerpo
- "Crear soporte personalizado": el selector de capa arranca en "Cuerpo"
- Cambiar de capa: se refrescan ambas secciones
- Mismo patrón visual que `AnchorConfigScreen` (consistencia de UI)

---

## 4. SupportsConfigPanel (dentro de DashboardPanels.kt)

### Actual (DashboardPanels.kt:1003-1034)

Bottom sheet que:
- Muestra soportes ya configurados
- Permite quitar (toggle directo, sin deferred save)
- NO tiene sección "agregar" para recuperar
- NO tiene botón para ir a SupportsConfigScreen

```kotlin
@Composable
fun SupportsConfigPanel(
    supports: List<DashboardCheckItemState>,
    onRemoveSupport: (String) -> Unit,  // Directo, sin deferred save
    palette: DashboardPalette
) {
    // Solo muestra soportes actuales con toggle de quitar
    // Sin estado local, sin sección de recuperación
}
```

### Esperado

Archivo nuevo: `ui/supports/SupportsConfigPanel.kt`

```
┌─────────────────────────────────────┐
│ Soportes                        [X] │  Header
├─────────────────────────────────────┤
│ Mis soportes                        │
│  ☑ Bañarse                    [−]   │  Toggle: quitar → va abajo
│  ☑ Tomar agua                 [−]   │
│  ☑ Cepillarse los dientes     [−]   │
├─────────────────────────────────────┤
│ Agregar soporte                     │  Sección de recuperación
│  ☐ Bañarse                    [+]   │  Solo lo que se quitó arriba
│  ☐ Tomar agua                 [+]   │  Re-agregar sin salir del panel
├─────────────────────────────────────┤
│ [Ver catálogo completo]             │  Botón → SupportsConfigScreen
└─────────────────────────────────────┘
```

**Comportamiento deferred save**:
1. Usuario quita "Bañarse" → desaparece de arriba, aparece en "Agregar soporte"
2. Usuario hace clic en "Bañarse" en Agregar → vuelve a "Mis soportes"
3. Usuario cierra el panel → se persisten SOLO los que quedaron quitados
4. Los re-agregados NO generan operación de escritura

---

## 5. DashboardViewModel.kt

### Actual

Métodos existentes:
- `addToSupports(activityId)` — passthrough
- `removeFromSupports(activityId)` — directo
- `onToggleSupport(activity, dateKey)` — semántica invertida, funciona
- `resetSupportOmissions()` — borra todos los logs del día

### Esperado

Agregar:
- `saveSupportChecklist()` — persiste el estado actual de la checklist de soportes
- `toggleAllSupports()` — marca/desmarca todos los soportes del día

Refactorizar:
- `addToSupports` → delegar a `repository.addSupport()` (con validación)
- `removeFromSupports` → delegar a `repository.removeSupport()` (con validación)

---

## 6. SupportsPreviewSection.kt

### Actual

- Muestra soportes con `SectionHeader` (mismo peso que AnchorPreviewSection)
- `isInverted = true` (color marrón al desmarcar)
- Contador: "X/Y pendientes"
- Texto sutil: "Todo cumplido por defecto..."
- Botón "Restablecer todo" (visible cuando hay omisiones)
- Link "editar soportes" → abre config

### Esperado

Rediseño completo:

```
┌─────────────────────────────────────┐
│ ▶ Soportes — 5/5 hoy                │  Colapsado por defecto
└─────────────────────────────────────┘

Al expandir:
┌─────────────────────────────────────┐
│ ▼ Soportes — 5/5 hoy                │
│                                     │
│ Todo cumplido por defecto.          │  Indicador de semántica invertida
│ Desmarcá solo lo que no hiciste.    │
│                                     │
│  ☐ Bañarse                          │  Sin marcar = hecho (default)
│  ☐ Tomar agua                       │
│  ☑ Cepillarse los dientes    [ámbar]│  Marcado = NO lo hizo
│  ☐ Comer algo decente               │
│  ☐ Una interacción limpia           │
│                                     │
│ [Marcar todo]  [Guardar]            │  Botones de acción
└─────────────────────────────────────┘
```

**Diferencias clave con el actual**:
- Colapsado por defecto (no ocupa espacio vertical innecesario)
- Jerarquía visual reducida: tipografía más chica, colores más sutiles
- Sin `SectionHeader` prominente (es un detalle, no una sección principal)
- Botón "Guardar" explícito (el usuario confirma sus omisiones)
- Botón "Marcar/Desmarcar todo" como toggle rápido

---

## 7. DashboardScreen.kt

### Actual

- `SupportsPreviewSection` se renderiza después de `AnchorPreviewSection`
- Ambas con mismo peso visual
- Link "editar soportes" → `onNavigateToSupportsConfig` → `AppScreen.Supports`

### Esperado

- Mantener el orden (Anclas arriba, Soportes abajo)
- `SupportsPreviewSection` ya trae su propio estado colapsado (cambio en ese archivo, no aquí)
- Verificar que `onNavigateToSupportsConfig` sigue funcionando
- Verificar que el menú de configuración rápida (bottom sheet) abre el nuevo `SupportsConfigPanel`

---

## 8. ActivityPolicy.kt

### Actual (líneas 13-14)

```kotlin
fun isSupport() = activityType == ActivitySurface.Support && !isGoal()
```

El `&& !isGoal()` es un filtro incorrecto. Si una actividad tuviera `activityType = Support` pero
`cadence = Weekly` (goal), `isSupport()` retornaría `false`.

### Esperado

```kotlin
fun isSupport() = activityType == ActivitySurface.Support
```

**Impacto**: bajo. `isSupport()` no se usa en el pipeline de proyección actual. Pero es una landmine
para futuros usos y viola la regla de dominio.

---

## 9. ActivityDefinition.kt

### Actual (línea 21)

```kotlin
val displaySurface: DisplaySurface, // DEPRECATED — replaced by activityType
```

Campo deprecated que sigue en el modelo. Tiene consumidores en `DomainMappers.kt` y `DashboardActivityOptionState`.

### Esperado

- Verificar si `displaySurface` tiene consumidores en producción
- Si no: remover el campo
- Si sí: planificar migración en scope separado (no bloquear Soporte por esto)

---

## 10. DashboardState.kt

### Actual (líneas 75-83)

```kotlin
data class DashboardCheckItemState(
    val id: String,
    val name: String,
    val completed: Boolean,
    val layerId: String?,
    val isInverted: Boolean,
)
```

### Esperado

Sin cambios. El modelo actual cubre las necesidades de soportes con semántica invertida.

---

## 11-15. Archivos sin cambios

| Archivo | Por qué no se toca |
|---------|-------------------|
| `DomainMappers.kt` | Mapea correctamente `presetCategory` → `activityType`. El bug está en los seeds, no aquí |
| `DashboardProjection.kt` | `supportItems` se calcula correctamente con semántica invertida |
| `CheckItem.kt` | `isInverted = true` ya afecta el color del checkbox (marrón en vez de coral) |
| `MainActivity.kt` | Navegación ya conectada correctamente |
| `AutonomiaDao.kt` | Queries existentes cubren las necesidades |

---

## Resumen de gaps

| Gap | Severidad | Archivo | Tipo |
|-----|-----------|---------|------|
| `act_dormir_temprano` mal etiquetado | Alta | `DefaultSeeds.kt:145` | Bug |
| Sin filtro por capas en config | Alta | `SupportsConfigScreen.kt` | Feature faltante |
| Sin validación en addToSupports | Media | `AutonomiaRepository.kt` | Arquitectura |
| SupportsConfigPanel sin deferred save | Alta | `DashboardPanels.kt` | Feature faltante |
| SupportsConfigPanel sin botón redirigir | Media | `DashboardPanels.kt` | Feature faltante |
| SupportsConfigPanel sin sección recuperación | Alta | `DashboardPanels.kt` | Feature faltante |
| Mismo peso visual que Anclas en dashboard | Alta | `SupportsPreviewSection.kt` | UX |
| Sin botón Guardar en dashboard | Alta | `SupportsPreviewSection.kt` | Feature faltante |
| Sin botón Marcar/Desmarcar todo | Alta | `SupportsPreviewSection.kt` | Feature faltante |
| `isSupport()` con `!isGoal()` | Baja | `ActivityPolicy.kt` | Landmine |
| `displaySurface` deprecated | Baja | `ActivityDefinition.kt` | Dead code |
| UI duplicada (config screen + config panel) | Media | Ambos archivos | Anti-patrón |

---

## Orden de lectura recomendado

Antes de implementar, leer los archivos en este orden:

1. Este documento (`docs/contraste-soporte-actual-vs-esperado.md`)
2. `docs/definicion-reestructuracion-soporte.md` (plan de fases)
3. `docs/configuracion-canonica-sistema-v1.md` §2 (reglas canónicas)
4. Código actual archivo por archivo según la tabla de arriba
