# Análisis de código — Estado actual pre-reestructuración

⚠️ **LECTURA OBLIGATORIA** — Este documento es requisito para entender el plan de reestructuración.
Leer ANTES de `docs/plan-reestructuracion-3-capas.md`.

Fecha: 2026-05-23
Propósito: auditoría completa del código antes de planificar la reestructuración.
Fuentes: análisis de 12 archivos del sistema.

> Estado 2026-05-25: documento historico de diagnostico. Conserva valor para
> entender bugs anteriores, pero no describe el estado vigente de Mis anclas.
> Para el contrato actual usar `docs/mis-anclas-ux-canon-v1.md`.

---

## 1. AnchorConfigScreen.kt

### Flujo de creación de ancla

1. Usuario toca "Agregar" en una actividad del catálogo
2. Se abre `ActivityConfigSection` con grid de metas y time wheel
3. Usuario configura (o no) y toca "Guardar ancla"
4. `onConfirm(targetValue, count, period)` → `onAddAnchor(activityId, ...)` 
5. ViewModel → Repository → `dao.upsertUserActivityConfig()`

### El problema: "Sin meta" es el default

```kotlin
var selectedGoal by remember { mutableStateOf(GoalPreset.None) }  // línea 780
```

- `GoalPreset.None.toCountAndPeriod()` retorna `Pair(null, null)` — sin targetCount ni targetPeriod
- La UI dice "Meta (opcional)" (línea 808)
- **No hay validación que impida guardar un ancla sin targets**
- El repositorio recibe `targetPeriod = null`, asigna `cadence = Daily` por defecto
- La actividad se persiste con `targetCount = null`, `targetPeriod = null`

### UI elements clave

- "Meta (opcional)" — debería decir "Meta (obligatoria)"
- "Sin meta" como opción default — debería eliminarse
- "Guardar ancla" sin validación — debería rechazar si targets ausentes

---

## 2. DashboardProjection.kt

### Filtros aplicados sobre actividades

| # | Línea | Filtro | Efecto |
|---|-------|--------|--------|
| F1 | 49 | `activities.filter { active && !archived && != Task }` | Primer filtro de visibilidad |
| F2 | 52 | `filter { isGoal() }` → `goalActivities` | **DEAD CODE** — nunca se usa el resultado |
| F3 | 53 | `filterNot { isGoal() }` → `dashboardActivities` | **Excluye anclas con meta semanal/mensual del dashboard** |
| F4 | 56 | `isCompletedBy(log)` | Separa completadas de pendientes |
| F5 | 60 | `activityType == Anchor` → `primaryActivities` | Partición para anchorItems |
| F6 | 61 | `activityType == Support` → `selfCareActivities` | Partición para supportItems |

### El bug del filtro isGoal()

`dashboardActivities = visibleActivities.filterNot { it.isGoal() }` (línea 53)

Una ancla con meta semanal o mensual (`targetPeriod = Week/Month`) es tratada como goal y **excluida** de `dashboardActivities`. Por lo tanto no aparece en `anchorItems` ni en el dashboard.

`isGoal()` se define como:
```kotlin
cadence == Weekly/Monthly || targetPeriod == Week/Month
```

### Código muerto encontrado

- `goalActivities` (línea 52): calculada pero nunca referenciada
- `metaLabel()` (línea 382-389): definida pero nunca invocada
- `secondaryActivities` (línea 64): alias redundante de `selfCareActivities`

### buildSupports()

Función que produce `List<DashboardSupportState>` con exactamente 2 elementos:
1. `DashboardSupportKind.Support` — resumen de soportes (conteo + 2 items)
2. `DashboardSupportKind.Tasks` — resumen de tareas (conteo + 2 items)

Consumido por `SupportsSection` (las 2 tarjetas del dashboard).

---

## 3. ActivityPolicy.kt

### isAnchor() — excluye goals

```kotlin
fun isAnchor() = activityType == Anchor && !isGoal()
```

- **Nunca se llama en código de producción.** Solo en tests.
- `DashboardPanels.kt` hace su propio filtro inline sin usar esta función.
- La exclusión `&& !isGoal()` contribuye a que las anclas con metas no se consideren anclas.

### isGoal() — sí se usa extensamente

7 call sites en producción: DashboardProjection (3), ScoreEngine (2), DashboardPanels (2).

---

## 4. DomainMappers.kt

### Divergencias entre toDomain() y mergeToDomain()

| Campo | toDomain() (catálogo) | mergeToDomain() (configurada) |
|-------|----------------------|------------------------------|
| `name` | `definition.name` | `config.customName ?: definition.name` |
| `displaySurface` | `Available` (hardcodeado) | `PrimaryChecklist` (fallback) |
| `activityType` | Heurística por `presetCategory` | `config.activityType` con fallback `Anchor` |
| `cadence` | `null` | `config.cadence` |
| `targetValue` | `null` | `config.targetValue` |
| `minimumValue` | `null` | `config.minimumValue` |
| `targetCount` | `null` | `config.targetCount` |
| `targetPeriod` | `null` | `config.targetPeriod` |
| `active` | `false` | `config.active` |
| `archived` | `false` | `config.archived` |
| `sortOrder` | `definition.sortOrder` | `config.sortOrder` |
| `updatedAt` | `definition.updatedAt` | `config.updatedAt` |

### Implicancia práctica

Cuando `DashboardProjection` construye `activityOptions`, usa `effective = configured ?: activity`. Para una actividad NO configurada, `effective` viene de `toDomain()` con `targetValue = null` y `active = false`. Pero la cascada en la proyección asigna `targetValue = 1` como fallback. Esto crea una inconsistencia: actividades no configuradas muestran "1 min" en el catálogo.

---

## 5. DashboardViewModel.kt

### Árbol de combines

15 flujos del repositorio entran en 4 combines encadenados:

```
observeConfiguredActivities ──→ Combine #2 ──→ CoreSnapshot
                                            ──→ Combine #3 ──→ FactSnapshot
observeCatalogActivities ────────────────────────────────────→ Combine #4 ──→ DashboardState
```

- `observeConfiguredActivities` entra en el Combine #2 (temprano)
- `observeCatalogActivities` entra SOLO en el Combine #4 (final)
- 3 snapshots intermedios: `DashboardActivityLogSnapshot`, `DashboardCoreSnapshot`, `DashboardFactSnapshot`

### onToggleSupport vs toggleActivity

| | toggleActivity | onToggleSupport |
|---|---|---|
| Parámetro completed | Lo recibe del caller | Lo calcula leyendo BD |
| Lógica | Set directo | Invierte estado actual |
| Bloqueante | No | Sí (`.first()` sobre Flow) |

---

## 6. DashboardScreen.kt — Árbol de componentes

Orden de arriba hacia abajo:

1. TopBar
2. StatusCard
3. DailyProgressCard
4. AnchorPhraseCard
5. ActionButtons
6. LayersSection
7. SignalsSection
8. SobrietySection
9. **AnchorPreviewSection** ← `state.anchorItems`
10. **SupportsPreviewSection** ← `state.supportItems`
11. **SupportsSection** ← `state.supports` (las 2 tarjetas)
12. WeekSection

### Problema de duplicación

Hay DOS secciones de soportes consecutivas: SupportsPreviewSection (items individuales) + SupportsSection (tarjetas agregadas). Usan modelos de datos distintos.

---

## 7. SupportsPreviewSection.kt

### Semántica invertida

- `isInverted = true` se pasa a `CheckItem` pero **no se usa** en el cuerpo del composable
- La inversión real está en el contador: `doneCount = items.count { !it.completed }`
- El header dice "X/Y pendientes" donde "pendientes" = los que SÍ hizo (porque en semántica invertida, lo hecho = no marcado)

### Estado vacío

Cuando `items.isEmpty()`: muestra "Sin soportes configurados" en texto muted. No muestra el link "editar soportes".

---

## 8. AnchorPreviewSection.kt

### Sin estado vacío

Cuando `items` está vacío: el header dice "Anclas pendientes — 0 pendientes" y debajo aparece una tarjeta vacía (fondo bgSurface sin contenido). No hay mensaje de "sin anclas configuradas".

### Separación completados/pendientes

- `pendingItems` primero, con `onToggle(id, true)`
- Si hay `completedItems`: inserta `CompletedDivider` (texto "Completados")
- `completedItems` después, con `onToggle(id, false)`

---

## 9. SupportCard.kt — SupportsSection

### Las 2 tarjetas

Cada `DashboardSupportState` se renderiza como una tarjeta de 148dp con:
- Título + icono (arriba)
- Número grande (ej. "2/4") + texto descriptivo (medio)
- 2 líneas de checklist con icono check/circle (abajo)

### Callbacks

- `Support` → abre `DashboardSheet.Support`
- `Tasks` → abre `DashboardSheet.Tasks`

---

## Resumen de hallazgos críticos

| # | Hallazgo | Impacto |
|---|----------|---------|
| 1 | `GoalPreset.None` es el default en AnchorConfigScreen | Se crean anclas sin targets |
| 2 | `filterNot { isGoal() }` en DashboardProjection línea 53 | Anclas con metas semanales/mensuales no aparecen en el dashboard |
| 3 | `goalActivities` en DashboardProjection línea 52 | Código muerto |
| 4 | `isAnchor()` en ActivityPolicy excluye goals | Función sin uso en producción |
| 5 | 13 campos divergentes entre `toDomain()` y `mergeToDomain()` | Dos fuentes de verdad con defaults distintos |
| 6 | SupportsPreviewSection + SupportsSection duplicadas | Dos secciones de soportes consecutivas en el dashboard |
| 7 | `isInverted` en CheckItem no se usa | El parámetro existe pero no afecta el renderizado |
| 8 | AnchorPreviewSection sin empty state | Tarjeta vacía si no hay anclas |
| 9 | 15 flujos, 4 combines, 3 snapshots en ViewModel | Complejidad excesiva en la cadena de datos |
| 10 | `onToggleSupport` usa `.first()` bloqueante | Patrón frágil |
