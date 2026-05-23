# Pro-Prompt: Limpieza de código muerto + Rename checklist → anchor/support

Fecha: 2026-05-23
Estado: ✅ COMPLETADO — 2026-05-23 (3 fases ejecutadas, verificación aprobada)

## Objetivo

Eliminar código muerto del refactor de entidades v2 y renombrar todas las
referencias a "checklist" por los nombres canónicos del dominio: **anchor**
(anclas), **support** (soportes) y **task** (pendientes).

La palabra "checklist" debe reservarse EXCLUSIVAMENTE para:
- La lista de marcado diario en el dashboard (el acto de check-ear items)
- `DashboardCheckItemState` (el modelo de estado compartido para items checkeables)

## Contexto

El refactor de entidades (`meta/instructions/2026-05-22-actualizacion-nucleo-backend-local.md`)
separó correctamente `ActivityDefinitionEntity` + `UserActivityConfigEntity` +
`ActivityLogEntity`, pero **los nombres no se actualizaron**. "Checklist" quedó
envenenando 38 referencias en ~15 archivos donde el dominio ya dice "Anchor".

Fuente de verdad para nombres canónicos: `AGENTS.md` tabla "Nombres canónicos del frontend".

## Auditoría previa

Auditoría completa ejecutada el 2026-05-23 contra el código actual. Resultados
completos en Engram: `architecture/refactor-audit-2026-05-22`.

Resumen:
- **4 dead code items** (eliminación directa)
- **38 renames** (checklist → anchor, secondaryChecklist → support)
- **7 items confusos** (2 requieren cambios adicionales, 5 se arreglan solos con los renames)

---

## Fase 1: Eliminar código muerto (5 minutos)

### D1 — `ActivityEntity` class

**Archivo**: `app/src/main/java/dev/panopt/autonomia/data/Entities.kt`
**Líneas**: 21–42 (22 líneas)
**Acción**: Eliminar la data class completa `ActivityEntity`.
**Razón**: La tabla `activities` fue dropeada en `MIGRATION_3_4`. No está en
`@Database` entities. El DAO no tiene queries contra ella. Nadie la instancia.

```kotlin
// ELIMINAR todo este bloque (líneas 21-42):
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val layerId: String,
    val name: String,
    val description: String,
    val type: String,
    val role: String,
    val displaySurface: String,
    val unit: String,
    val contributionRole: String,
    val importanceTier: String,
    val presetCategory: String? = null,
    val active: Boolean = true,
    val archived: Boolean = false,
    val cadence: String? = null,
    val targetValue: Int? = null,
    val minimumValue: Int? = null,
    val targetCount: Int? = null,
    val targetPeriod: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
```

### D2 — Import `ActivityEntity` en DomainMappers

**Archivo**: `app/src/main/java/dev/panopt/autonomia/data/local/mapper/DomainMappers.kt`
**Línea**: 29
**Acción**: Eliminar la línea `import dev.panopt.autonomia.data.ActivityEntity`

### D3 — `ActivityEntity.toDomain()` mapper

**Archivo**: `app/src/main/java/dev/panopt/autonomia/data/local/mapper/DomainMappers.kt`
**Líneas**: 42–64 (23 líneas)
**Acción**: Eliminar la función completa `internal fun ActivityEntity.toDomain(): ActivityDefinition`.
**Razón**: Nadie la llama. El DAO `observeActivityDefinitions()` retorna
`ActivityDefinitionEntity`, no `ActivityEntity`. La migración 3→4 eliminó la
tabla vieja.

```kotlin
// ELIMINAR este bloque (líneas 42-64):
internal fun ActivityEntity.toDomain(): ActivityDefinition = ActivityDefinition(
    id = id,
    layerId = layerId,
    name = name,
    description = description,
    type = runCatching { ActivityType.valueOf(type) }.getOrDefault(ActivityType.Check),
    role = runCatching { ActivityRole.valueOf(role) }.getOrDefault(ActivityRole.Practice),
    displaySurface = runCatching { DisplaySurface.valueOf(displaySurface) }.getOrDefault(DisplaySurface.Available),
    activityType = when (presetCategory) { ... },
    ...
)
```

### D4 — Import `ActivityEntity` en AutonomiaRepository

**Archivo**: `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt`
**Línea**: 5
**Acción**: Eliminar la línea `import dev.panopt.autonomia.data.ActivityEntity`

---

## Fase 2: Rename masivo — checklist → anchor / ancla

### Reglas de rename

| Patrón actual | Reemplazo | Aplica a |
|---------------|-----------|----------|
| `checklist` (en nombres de función/variable/archivo/package/string relacionado con anclas) | `anchor` | Funciones, variables, archivos, packages, strings UI |
| `Checklist` (PascalCase, relacionado con anclas) | `Anchor` | Clases, composables, enums |
| `onAddToChecklist` | `onAddAnchor` | Callbacks |
| `onRemoveFromChecklist` | `onRemoveAnchor` | Callbacks |
| `onNavigateToChecklistConfig` | `onNavigateToAnchorConfig` | Callbacks |
| `onOpenChecklist` | `onOpenAnchors` | Callbacks |
| `ChecklistIcon` | `AnchorIcon` | Composable |
| `ChecklistPreviewSection` | `AnchorPreviewSection` | Composable |
| `ChecklistConfigPanel` | `AnchorConfigPanel` | Composable |
| `ChecklistConfigScreen` | `AnchorConfigScreen` | Composable |
| `DashboardSheet.Checklist` | `DashboardSheet.Anchor` | Enum value |
| `DashboardSheet.ChecklistConfig` | `DashboardSheet.AnchorConfig` | Enum value |
| `checklistItems` | `anchorItems` | State field |
| `"Checklist principal"` | `"Anclas"` | String |
| `"Todas las actividades estan en tu checklist"` | `"Todas las actividades estan en tus anclas"` | String |

### Reglas de rename — secondaryChecklist → support

| Patrón actual | Reemplazo | Aplica a |
|---------------|-----------|----------|
| `secondaryChecklist` (en nombres) | `support` | Funciones, variables |
| `SecondaryChecklist` (PascalCase) | `Support` | Enums, clases |
| `DashboardSheet.SecondaryChecklist` | `DashboardSheet.Support` | Enum value |
| `secondaryChecklistItems` | `supportItems` | State field |
| `onOpenSecondaryChecklist` | `onOpenSupports` | Callbacks |
| `"Checklist secundaria"` | `"Soportes"` | String |
| `DashboardSupportKind.SecondaryChecklist` | `DashboardSupportKind.Support` | Enum value |

### Archivos a modificar (Fase 2)

Cada archivo se lista con TODOS los cambios necesarios, en orden.

---

#### Archivo W1-W4: `domain/scoring/ScoreEngine.kt`

| Línea | Cambio |
|-------|--------|
| 75 | `PrimaryChecklist,` → `Anchor,` |
| 76 | `SecondaryChecklist,` → `Support,` |
| 153, 168, 178 | `"Checklist principal"` → `"Anclas"` |
| 154, 169, 179 | `"Checklist secundaria"` → `"Soportes"` |

También actualizar todos los `when` branches que matchean `ScoreFeature.PrimaryChecklist` → `ScoreFeature.Anchor` y `ScoreFeature.SecondaryChecklist` → `ScoreFeature.Support`.

---

#### Archivo W5-W8: `domain/dashboard/DashboardState.kt`

| Línea | Cambio |
|-------|--------|
| 14 | `val checklistItems: List<DashboardChecklistItemState>` → `val anchorItems: List<DashboardCheckItemState>` |
| 20 | `val secondaryChecklistItems: List<DashboardChecklistItemState>` → `val supportItems: List<DashboardCheckItemState>` |
| 76 | `data class DashboardChecklistItemState` → `data class DashboardCheckItemState` |
| 98 | `SecondaryChecklist,` → `Support,` |

---

#### Archivo W9-W13: `domain/dashboard/DashboardProjection.kt`

| Línea | Cambio |
|-------|--------|
| 163 | `checklistItems = primaryActivities.map {` → `anchorItems = primaryActivities.map {` |
| 164 | `DashboardChecklistItemState(` → `DashboardCheckItemState(` |
| 202 | `secondaryChecklistItems = secondaryActivities.map {` → `supportItems = secondaryActivities.map {` |
| 203 | `DashboardChecklistItemState(` → `DashboardCheckItemState(` |
| 518 | `DashboardSupportKind.SecondaryChecklist` → `DashboardSupportKind.Support` |
| 519 | `"Checklist secundaria"` → `"Soportes"` |

---

#### Archivo W14-W20, W38: `ui/dashboard/DashboardPanels.kt`

| Línea | Cambio |
|-------|--------|
| 62 | `Checklist,` → `Anchor,` |
| 63 | `SecondaryChecklist,` → `Support,` |
| 67 | `ChecklistConfig,` → `AnchorConfig,` |
| 85 | `onAddToChecklist: (String, Int?, Int?, TargetPeriod?) -> Unit,` → `onAddAnchor: (String, Int?, Int?, TargetPeriod?) -> Unit,` |
| 86 | `onRemoveFromChecklist: (String) -> Unit,` → `onRemoveAnchor: (String) -> Unit,` |
| 89 | `onNavigateToChecklistConfig: () -> Unit,` → `onNavigateToAnchorConfig: () -> Unit,` |
| 131 | `onOpenChecklist` → `onOpenAnchors` |
| 132 | `onOpenSecondaryChecklist` → `onOpenSupports` |
| 137 | `DashboardSheet.Checklist -> ChecklistPanel(` → `DashboardSheet.Anchor -> AnchorPanel(` |
| 139 | `items = state.checklistItems` → `items = state.anchorItems` |
| 148 | `onNavigateToChecklistConfig()` → `onNavigateToAnchorConfig()` |
| 151 | `DashboardSheet.SecondaryChecklist -> ChecklistPanel(` → `DashboardSheet.Support -> AnchorPanel(` |
| 153 | `state.secondaryChecklistItems` → `state.supportItems` |
| 185 | `DashboardSheet.ChecklistConfig -> ChecklistConfigPanel(` → `DashboardSheet.AnchorConfig -> AnchorConfigPanel(` |
| 189 | `onAddToChecklist = onAddToChecklist,` → `onAddAnchor = onAddAnchor,` |
| 190 | `onRemoveFromChecklist = onRemoveFromChecklist,` → `onRemoveAnchor = onRemoveAnchor,` |
| 207 | `private fun ChecklistPanel(` → `private fun AnchorPanel(` |
| 209 | `items: List<DashboardChecklistItemState>,` → `items: List<DashboardCheckItemState>,` |
| 254 | `onOpenChecklist: () -> Unit,` → `onOpenAnchors: () -> Unit,` |
| 255 | `onOpenSecondaryChecklist: () -> Unit = {},` → `onOpenSupports: () -> Unit = {},` |
| 272 | `ChecklistIcon(` → `AnchorIcon(` |
| 275 | `onClick = onOpenChecklist` → `onClick = onOpenAnchors` |
| 283 | `onOpenSecondaryChecklist` → `onOpenSupports` |

---

#### Archivo W21: `ui/dashboard/components/ChecklistPreview.kt` → `AnchorPreview.kt`

Renombrar el archivo completo de `ChecklistPreview.kt` a `AnchorPreview.kt`.

Dentro del archivo:
| Línea | Cambio |
|-------|--------|
| 42, 54, 135, 216 | `DashboardChecklistItemState` → `DashboardCheckItemState` |
| 52 | `internal fun ChecklistPreviewSection(` → `internal fun AnchorPreviewSection(` |
| 62 | Verificar que el título siga siendo "Anclas pendientes" (OK, ya está bien) |

Mover `DashboardIconKind` enum (línea 104) a su propio archivo `DashboardIcons.kt` si no está ya allí.

Mover `CheckItem` composable (línea 133) a un archivo compartido `components/CheckItem.kt` ya que se reutiliza desde `DashboardPanels.kt`.

---

#### Archivo W22-W24: `ui/dashboard/components/ChecklistConfigPanel.kt` → `AnchorConfigPanel.kt`

Renombrar el archivo de `ChecklistConfigPanel.kt` a `AnchorConfigPanel.kt`.

| Línea | Cambio |
|-------|--------|
| 55 | Comentario: `primary checklist` → `anchors` |
| 59 | `internal fun ChecklistConfigPanel(` → `internal fun AnchorConfigPanel(` |
| 63 | `onAddToChecklist: (String, Int?, Int?, TargetPeriod?) -> Unit,` → `onAddAnchor: (String, Int?, Int?, TargetPeriod?) -> Unit,` |
| 64 | `onRemoveFromChecklist: (String) -> Unit,` → `onRemoveAnchor: (String) -> Unit,` |
| 111 | `onAddToChecklist(configuringActivity!!.id, targetValue, targetCount, targetPeriod)` → `onAddAnchor(configuringActivity!!.id, targetValue, targetCount, targetPeriod)` |
| 158 | `onRemoveFromChecklist(anchor.id)` → `onRemoveAnchor(anchor.id)` |
| 181 | String: `"Todas las actividades estan en tu checklist"` → `"Todas las actividades estan en tus anclas"` |

Actualizar imports en `DashboardPanels.kt` que referencian `ChecklistConfigPanel` → `AnchorConfigPanel`.

---

#### Archivo W25-W27: `ui/checklist/ChecklistConfigScreen.kt` → `ui/anchors/AnchorConfigScreen.kt`

**IMPORTANTE**: Esto implica renombrar el **directorio** `ui/checklist/` → `ui/anchors/` y el archivo `ChecklistConfigScreen.kt` → `AnchorConfigScreen.kt`.

Archivos dentro de `ui/checklist/`:
- `ChecklistConfigScreen.kt` → `AnchorConfigScreen.kt`
- `GoalPreset.kt` (se queda — es compartido)
- `TimeWheelPicker.kt` (se queda — es compartido)

| Línea | Cambio |
|-------|--------|
| 1 | `package dev.panopt.autonomia.ui.checklist` → `package dev.panopt.autonomia.ui.anchors` |
| 62 | Comentario: `primary checklist` → `anchors` |
| 66 | `internal fun ChecklistConfigScreen(` → `internal fun AnchorConfigScreen(` |
| 70 | `onAddToChecklist: (String, Int?, Int?, TargetPeriod?) -> Unit,` → `onAddAnchor: (String, Int?, Int?, TargetPeriod?) -> Unit,` |
| 71 | `onRemoveFromChecklist: (String) -> Unit,` → `onRemoveAnchor: (String) -> Unit,` |
| 204 | `onAddToChecklist(configuringActivity!!.id, ...)` → `onAddAnchor(configuringActivity!!.id, ...)` |
| 272 | `onRemoveFromChecklist(anchor.id)` → `onRemoveAnchor(anchor.id)` |
| 323 | String: `"Todas las actividades estan en tu checklist"` → `"Todas las actividades estan en tus anclas"` |

Actualizar TODOS los imports que referencian `dev.panopt.autonomia.ui.checklist.*` → `dev.panopt.autonomia.ui.anchors.*` en:
- `DashboardPanels.kt`
- `ChecklistConfigPanel.kt` (que pasará a ser `AnchorConfigPanel.kt`)
- `DashboardScreen.kt`
- `MainActivity.kt`

---

#### Archivo W28: `ui/dashboard/DashboardScreen.kt`

| Línea | Cambio |
|-------|--------|
| 63 | `onAddToChecklist: (String, Int?, Int?, TargetPeriod?) -> Unit,` → `onAddAnchor: (String, Int?, Int?, TargetPeriod?) -> Unit,` |
| 64 | `onRemoveFromChecklist: (String) -> Unit,` → `onRemoveAnchor: (String) -> Unit,` |
| 65 | `onNavigateToChecklistConfig: () -> Unit,` → `onNavigateToAnchorConfig: () -> Unit,` |
| 117 | `ChecklistPreviewSection(` → `AnchorPreviewSection(` |
| 119 | `state.checklistItems` → `state.anchorItems` |
| 124 | `state.secondaryChecklistItems` → `state.supportItems` |
| 131 | `onOpenSecondaryChecklist` → `onOpenSupports` |
| 132 | `DashboardSheet.SecondaryChecklist` → `DashboardSheet.Support` |
| 164 | `onOpenChecklist = onNavigateToChecklistConfig` → `onOpenAnchors = onNavigateToAnchorConfig` |
| 184 | `onAddToChecklist = onAddToChecklist,` → `onAddAnchor = onAddAnchor,` |
| 185 | `onRemoveFromChecklist = onRemoveFromChecklist,` → `onRemoveAnchor = onRemoveAnchor,` |
| 188 | `onNavigateToChecklistConfig = onNavigateToChecklistConfig,` → `onNavigateToAnchorConfig = onNavigateToAnchorConfig,` |

---

#### Archivo W29: `ui/dashboard/DashboardViewModel.kt`

| Línea | Cambio |
|-------|--------|
| 283 | `fun addActivityToChecklist(` → `fun addActivityAsAnchor(` |
| 290 | `repository.addActivityToChecklist(` → `repository.addActivityAsAnchor(` |
| 299 | `fun removeActivityFromChecklist(` → `fun removeActivityAsAnchor(` |
| 301 | `repository.removeActivityFromChecklist(` → `repository.removeActivityAsAnchor(` |

---

#### Archivo W30: `AutonomiaRepository.kt`

| Línea | Cambio |
|-------|--------|
| 267 | `suspend fun addActivityToChecklist(` → `suspend fun addActivityAsAnchor(` |
| 289 | `suspend fun removeActivityFromChecklist(` → `suspend fun removeActivityAsAnchor(` |

---

#### Archivo W31: `MainActivity.kt`

| Línea | Cambio |
|-------|--------|
| 54 | `onAddToChecklist = dashboardViewModel::addActivityToChecklist` → `onAddAnchor = dashboardViewModel::addActivityAsAnchor` |
| 55 | `onRemoveFromChecklist = dashboardViewModel::removeActivityFromChecklist` → `onRemoveAnchor = dashboardViewModel::removeActivityAsAnchor` |
| 56 | `onNavigateToChecklistConfig = { currentScreen = AppScreen.ChecklistConfig }` → `onNavigateToAnchorConfig = { currentScreen = AppScreen.AnchorConfig }` |
| 60 | `AppScreen.ChecklistConfig -> ChecklistConfigScreen(` → `AppScreen.AnchorConfig -> AnchorConfigScreen(` |
| 64 | `onAddToChecklist = dashboardViewModel::addActivityToChecklist` → `onAddAnchor = dashboardViewModel::addActivityAsAnchor` |
| 65 | `onRemoveFromChecklist = dashboardViewModel::removeActivityFromChecklist` → `onRemoveAnchor = dashboardViewModel::removeActivityAsAnchor` |
| 108 | `enum class AppScreen { ..., ChecklistConfig, ... }` → `enum class AppScreen { ..., AnchorConfig, ... }` |

---

#### Archivo W32, W35: `ui/dashboard/components/NavigationDrawer.kt`

| Línea | Cambio |
|-------|--------|
| 36 | Import `ChecklistIcon` → `AnchorIcon` |
| 70 | `onOpenChecklist: () -> Unit,` → `onOpenAnchors: () -> Unit,` |
| 144 | `onClick = onOpenChecklist` → `onClick = onOpenAnchors` |
| 147, 158 | `ChecklistIcon(` → `AnchorIcon(` |

---

#### Archivo W33, W37: `ui/dashboard/components/SupportCard.kt`

| Línea | Cambio |
|-------|--------|
| 28 | Import `ChecklistIcon` → `AnchorIcon` |
| 41 | `onOpenSecondaryChecklist: () -> Unit = {},` → `onOpenSupports: () -> Unit = {},` |
| 62 | `onOpenSecondaryChecklist` → `onOpenSupports` |
| 176 | `DashboardSupportKind.SecondaryChecklist -> ChecklistIcon(` → `DashboardSupportKind.Support -> AnchorIcon(` |

---

#### Archivo W34: `ui/dashboard/DashboardIcons.kt`

| Línea | Cambio |
|-------|--------|
| 418 | `fun ChecklistIcon(` → `fun AnchorIcon(` |

---

#### Archivo W36: `ui/dashboard/components/ActionButtons.kt`

| Línea | Cambio |
|-------|--------|
| 27 | Import `ChecklistIcon` → `AnchorIcon` |
| 70 | `ChecklistIcon(` → `AnchorIcon(` |

---

#### Archivos a renombrar (sistema de archivos)

| Ruta actual | Ruta nueva |
|------------|------------|
| `ui/checklist/` | `ui/anchors/` |
| `ui/checklist/ChecklistConfigScreen.kt` | `ui/anchors/AnchorConfigScreen.kt` |
| `ui/dashboard/components/ChecklistConfigPanel.kt` | `ui/dashboard/components/AnchorConfigPanel.kt` |
| `ui/dashboard/components/ChecklistPreview.kt` | `ui/dashboard/components/AnchorPreview.kt` |

---

## Fase 3: Extracciones (mejora de estructura)

### C4 — Mover `DashboardIconKind` a su propio archivo

**Archivo origen**: `ui/dashboard/components/AnchorPreview.kt` (antes `ChecklistPreview.kt`)
**Línea**: 104
**Acción**: Extraer el enum `DashboardIconKind` a `ui/dashboard/DashboardIcons.kt` (si ya existe ese archivo, agregarlo; si no, crear `ui/dashboard/components/LayerIcons.kt`).
**Razón**: El enum de iconos de capa no pertenece a la preview de anclas.

### C5 — Mover `CheckItem` a archivo compartido

**Archivo origen**: `ui/dashboard/components/AnchorPreview.kt` (antes `ChecklistPreview.kt`)
**Línea**: 133
**Acción**: Extraer el composable `CheckItem` a `ui/dashboard/components/CheckItem.kt`.
**Razón**: Se reutiliza desde `DashboardPanels.kt` (AnchorPanel). No debería vivir dentro de AnchorPreview.

---

## Verificación post-cambios

Después de ejecutar las 3 fases, verificar:

1. **Compilación**: `./gradlew assembleDebug` debe compilar sin errores.
2. **Grep de seguridad**:
   - `grep -r "ChecklistConfig" app/src/main/java/` → debe dar 0 resultados
   - `grep -r "onAddToChecklist" app/src/main/java/` → debe dar 0 resultados
   - `grep -r "onRemoveFromChecklist" app/src/main/java/` → debe dar 0 resultados
   - `grep -r "ActivityEntity" app/src/main/java/` → debe dar 0 resultados (fuera de comentarios)
   - `grep -r "SecondaryChecklist" app/src/main/java/` → debe dar 0 resultados (fuera de migration SQL)
   - `grep -r "PrimaryChecklist" app/src/main/java/` → debe dar 0 resultados (fuera de migration SQL y DisplaySurface enum deprecado)
3. **Strings UI**: Verificar que los textos visibles usen "Anclas", "Soportes", "Pendientes" (no "Checklist principal", "Checklist secundaria").
4. **Navegación**: `AppScreen.AnchorConfig` debe navegar a `AnchorConfigScreen`.
5. **Dashboard sheet**: `DashboardSheet.Anchor`, `DashboardSheet.Support`, `DashboardSheet.AnchorConfig` deben mapear a los paneles correctos.

---

## Criterio de cierre

La limpieza queda cerrada cuando:
- `ActivityEntity` y su mapper están eliminados (0 referencias).
- "Checklist" solo aparece en: migration SQL (`AutonomiaDatabase.kt`), `DisplaySurface` enum deprecado, `DashboardCheckItemState`, y comentarios que describen el acto de checkear.
- Los nombres de archivos, packages, composables, enums, y callbacks usan `Anchor`/`anchor` y `Support`/`support` consistentemente.
- `./gradlew assembleDebug` compila.

¿Es este Pro-Prompt lo que necesitas?

---

## Traza de ejecución — 2026-05-23

### Fase 1: Dead code — ✅ COMPLETADO
- `ActivityEntity` data class eliminado de `data/Entities.kt:17-42`
- `ActivityEntity.toDomain()` mapper eliminado de `data/local/mapper/DomainMappers.kt:42-64`
- Import `ActivityEntity` eliminado de `DomainMappers.kt:29` y `AutonomiaRepository.kt:5`
- Grep de seguridad: 0 ocurrencias de `ActivityEntity` en `src/main/java/`

### Fase 2: Rename masivo — ✅ COMPLETADO
- **Archivos renombrados**:
  - `ui/checklist/` → `ui/anchors/` (directorio, 5 archivos fuente + 3 tests)
  - `ChecklistConfigScreen.kt` → `AnchorConfigScreen.kt`
  - `ChecklistConfigPanel.kt` → `AnchorConfigPanel.kt`
  - `ChecklistPreview.kt` → `AnchorPreview.kt`
- **Enums renombrados**:
  - `ScoreFeature.PrimaryChecklist` → `Anchor`, `SecondaryChecklist` → `Support`
  - `DashboardSheet.Checklist` → `Anchor`, `SecondaryChecklist` → `Support`, `ChecklistConfig` → `AnchorConfig`
  - `DashboardSupportKind.SecondaryChecklist` → `Support`
  - `AppScreen.ChecklistConfig` → `AnchorConfig`
- **Composables renombrados**:
  - `ChecklistConfigScreen` → `AnchorConfigScreen`
  - `ChecklistConfigPanel` → `AnchorConfigPanel`
  - `ChecklistPreviewSection` → `AnchorPreviewSection`
  - `ChecklistPanel` → `AnchorPanel`
  - `ChecklistIcon` → `AnchorIcon`
- **State fields**:
  - `checklistItems` → `anchorItems`, `secondaryChecklistItems` → `supportItems`
  - `DashboardChecklistItemState` → `DashboardCheckItemState`
- **Callbacks**:
  - `onAddToChecklist` → `onAddAnchor`
  - `onRemoveFromChecklist` → `onRemoveAnchor`
  - `onNavigateToChecklistConfig` → `onNavigateToAnchorConfig`
  - `onOpenChecklist` → `onOpenAnchors`
  - `onOpenSecondaryChecklist` → `onOpenSupports`
- **Repository/ViewModel**:
  - `addActivityToChecklist` → `addActivityAsAnchor`
  - `removeActivityFromChecklist` → `removeActivityAsAnchor`
- **Strings UI**:
  - `"Checklist principal"` → `"Anclas"` (3 lugares en ScoreEngine)
  - `"Checklist secundaria"` → `"Soportes"` (3 lugares en ScoreEngine)
  - `"Todas las actividades estan en tu checklist"` → `"anclas"` (2 lugares)
- **Verificación**: "Checklist" solo sobrevive en 8 lugares legítimos (migration SQL, DisplaySurface deprecado, comentarios)

### Fase 3: Extracciones — ✅ COMPLETADO
- `DashboardIconKind` + extensions (`.color()`, `.Icon()`, `.iconKind()`) → `ui/dashboard/DashboardIcons.kt`
- `CheckItem`, `CheckBoxMark`, `CompletedDivider` → `ui/dashboard/components/CheckItem.kt` (nuevo archivo)
- `AnchorPreview.kt` reducido de 290 a 68 líneas — solo contiene `AnchorPreviewSection`

### Compilación — ✅ BUILD SUCCESSFUL
- `./gradlew assembleDebug` compila sin errores.
- 3 errores encontrados y corregidos en post-compilación:
  - `DashboardScreen.kt:186`: `onCreateTask` y `onCompleteTask` faltaban en llamada a `DashboardSheetHost`
  - `AnchorPreview.kt:36`: `RoundedCornerShape` import faltante
