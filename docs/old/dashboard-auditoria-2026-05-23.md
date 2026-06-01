# Auditoría del Dashboard — 2026-05-23

Estado: diagnóstico técnico histórico. No contiene el cierre de soluciones.
Canónico para entender los problemas detectados en esa fecha; no reemplaza los
documentos vigentes posteriores.

Para el cierre de UX de Mis anclas y Configuración rápida > Anclas, usar
`docs/mis-anclas-ux-canon-v1.md`.

## Problemas detectados

1. Inconsistencia de fuente de datos entre AnchorPreviewSection y "Mis Anclas"
2. Falta de animación visible en checklists (Anchor y Support)
3. Jerarquía visual plana entre AnchorPreview y SupportsPreview
4. UX incompleta para semántica invertida de Soportes
5. Redundancia de SupportsSection + necesidad de TasksSection independiente

---

## Problema 1 — Inconsistencia de fuente de datos

### Síntoma reportado

Cuando el usuario crea o configura una nueva ancla desde la página "Mis Anclas",
esa actividad aparece correctamente en dicha página, pero en la sección
AnchorPreviewSection del dashboard los datos no aparecen o aparecen incompletos.

### Flujo de datos actual (traceo completo)

#### Fuente A: AnchorPreviewSection (dashboard)

```
DashboardViewModel.dashboardState
  → observeConfiguredActivities() → Flow<List<ActivityDefinition>>
    → dao.observeUserActivityConfigs().combine(dao.observeActivityDefinitions())
      → mergeToDomain(definition, config) para cada par
  → DashboardEngine.buildState(activityDefinitions = ...)
  → buildDashboardState(activities = activityDefinitions)  ← PARÁMETRO "activities"
  → visibleActivities = activities.filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }
  → dashboardActivities = visibleActivities.filterNot { it.isGoal() }
  → primaryActivities = dashboardActivities.filter { it.activityType == ActivitySurface.Anchor }
  → anchorItems = primaryActivities.map { ... DashboardCheckItemState(...) }
```

**Datos que alimentan anchorItems:**
- `id` → `activity.id` (del domain)
- `title` → `activity.name` (de `mergeToDomain`: `config.customName ?: definition.name`)
- `layerId` → `activity.layerId`
- `layerName` → `layerById[activity.layerId]?.name`
- `value` → `activity.valueLabel()` (usa `targetValue ?: minimumValue ?: 0`)
- `completed` → `activity.id in completedActivityIds` (depende de logs de hoy)
- `activityType` → `activity.activityType.name`

#### Fuente B: AnchorConfigScreen ("Mis Anclas")

```
La pantalla recibe parámetros directamente:
  - layers: List<DashboardLayerState>
  - activityOptions: List<DashboardActivityOptionState>
  - palette: DashboardPalette
```

**Datos que alimentan activityOptions:**
Se construye en `DashboardProjection.kt:183`:
```kotlin
activityOptions = catalogActivities.map { activity ->
    val log = todayLogsByActivity[activity.id]
    val configured = configuredById[activity.id]
    val effective = configured ?: activity
    DashboardActivityOptionState(
        id = activity.id,
        title = effective.name,
        layerId = activity.layerId,
        layerName = layerById[activity.layerId]?.name.orEmpty(),
        targetValue = configured?.targetValue ?: activity.targetValue ?: activity.minimumValue ?: 1,
        actualValue = log?.actualValue ?: configured?.targetValue ?: activity.targetValue ?: activity.minimumValue ?: 0,
        isCompletedToday = effective.isCompletedBy(log),
        isFocusSignal = activity.id == focusSignalActivityId,
        displaySurface = effective.displaySurface.name,
        activityType = effective.activityType.name,
        isGoal = effective.isGoal(),
        isConfigured = configured != null,
    )
}
```

Donde:
- `catalogActivities` = `observeCatalogActivities()` → TODAS las `activity_definitions` mapeadas con `toDomain()`
- `configuredById` = `activities.associateBy { it.id }` → las que tienen config (de `observeConfiguredActivities()`)
- `effective = configured ?: activity` → prioriza la versión con config, fallback a la del catálogo

### Puntos de divergencia identificados

#### Divergencia 1: `toDomain()` vs `mergeToDomain()`

| Campo | `toDomain()` (catálogo) | `mergeToDomain()` (configuradas) |
|-------|------------------------|----------------------------------|
| `name` | `definition.name` | `config.customName ?: definition.name` |
| `activityType` | `when(presetCategory)` → Anchor/Support/Task | `ActivitySurface.valueOf(config.activityType)` |
| `targetValue` | `null` | `config.targetValue` |
| `active` | `false` | `config.active` |
| `archived` | `false` | `config.archived` |

Si una actividad tiene `customName` en su config, `mergeToDomain` usa ese nombre,
pero `toDomain()` usa el nombre original de la definición. Si la AnchorConfigScreen
muestra `effective.name` donde `effective = configured ?: activity`, está usando
el nombre mergeado también, así que ESTO NO DEBERÍA CAUSAR INCONSISTENCIA por sí solo.

#### Divergencia 2: Filtro de visibilidad

```kotlin
// anchorItems (dashboard)
visibleActivities = activities.filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }

// activityOptions (config screen)
catalogActivities.map { ... }  // SIN FILTRO, todas las actividades del catálogo
```

**ESTA ES LA DIVERGENCIA CLAVE.** La AnchorConfigScreen muestra TODAS las
actividades del catálogo (incluyendo las inactivas, archivadas, y tipo Task).
El dashboard solo muestra las activas, no archivadas, y no Task.

Pero esto es INTENCIONAL para la pantalla de configuración — necesitás ver
todo para poder activar/desactivar.

La inconsistencia real podría estar en otro lado. Si el usuario crea una
actividad y LE ASIGNA `activityType = Anchor`, debería aparecer en
`primaryActivities` siempre que `active = true`, `archived = false`.

#### Divergencia 3: El valor de `value`

```kotlin
// anchorItems
value = activity.valueLabel()
// valueLabel() = "${targetValue ?: minimumValue ?: 0} min"

// activityOptions
targetValue = configured?.targetValue ?: activity.targetValue ?: activity.minimumValue ?: 1
```

Si una actividad del catálogo (`toDomain()`) no tiene `targetValue` (es null),
pero sí tiene `minimumValue`, `valueLabel()` usa `targetValue ?: minimumValue ?: 0`,
mientras que `activityOptions.targetValue` usa `activity.targetValue ?: activity.minimumValue ?: 1`.

**Ojo**: `toDomain()` pone `targetValue = null` y también omite `minimumValue`.
Entonces `activity.targetValue ?: activity.minimumValue ?: 1` → `null ?: null ?: 1` → **1**.
Mientras que `valueLabel()` → `null ?: null ?: 0` → **"0 min"**.

Para actividades del catálogo SIN config, `valueLabel()` muestra "0 min" mientras
que `targetValue` en `activityOptions` muestra 1.

#### Divergencia 4: `isGoal()`

```kotlin
// Actividad definida en domain/activity/ActivityDefinition.kt
fun ActivityDefinition.isGoal(): Boolean =
    targetPeriod != null && targetCount != null

// anchorItems: filtra las que NO son goals
dashboardActivities = visibleActivities.filterNot { it.isGoal() }

// activityOptions: incluye TODAS, con campo isGoal
isGoal = effective.isGoal()
```

Si una actividad es Goal, aparece en `activityOptions` con `isGoal = true` pero
NO aparece en `anchorItems`. Esto es por diseño (los goals van en otra sección),
pero podría confundir si el usuario espera ver sus goals en el checklist.

### Hipótesis del bug real

Basado en el traceo, la inconsistencia más probable está en el **momento de
creación de la actividad**. Cuando `createActivity()` en el ViewModel hace:

```kotlin
repository.upsertActivityDefinition(...)
repository.upsertUserActivityConfig(activityType = activityType.name, ...)
```

El `activityType` se guarda como String en Room. Luego `observeConfiguredActivities()`
combina configs con definitions. Si el config se inserta pero el JOIN en el DAO
no lo recoge inmediatamente (timing de Room Flow), podría haber un frame donde
la actividad existe en el catálogo pero no en las configuradas.

OTRO ESCENARIO: Si `UserActivityConfigEntity` tiene `active = true` por defecto
pero `mergeToDomain` interpreta `active` incorrectamente, la actividad podría
filtrarse fuera de `visibleActivities`.

### Preguntas que necesitan respuesta antes de proponer solución

1. ¿El bug ocurre siempre o solo en algunas actividades?
2. ¿Se reproduce al crear actividad nueva vs al reconfigurar una existente?
3. ¿Los datos faltantes son: la actividad entera, el título, el valor, o el estado completed?
4. ¿El problema es inmediato o requiere reiniciar la app/el ViewModel?
5. ¿`observeConfiguredActivities()` emite correctamente después de `upsertUserActivityConfig`?
6. ¿Hay diferencia entre actividades preset y actividades custom?

---

## Problema 2 — Falta de animación visible en checklists

### Código actual de animaciones

`CheckItem.kt` ya implementa:
- `animateFloatAsState` para `rowAlpha`: 1f → 0.58f al completar
- `animateColorAsState` para `titleColor`: textMain → textMuted al completar
- `CheckBoxMark` con `animateColorAsState` (boxColor) y `animateFloatAsState` (checkAlpha + checkScale)
- `animateContentSize` en el Row para transiciones de altura
- `TextDecoration.LineThrough` al completar

`AnchorPreview.kt` envuelve en:
- `animateContentSize(animationSpec = tween(220))` en el Column
- `key(item.id)` para cada CheckItem

### Por qué PODRÍA no ser visible la animación

1. **El ítem no se mueve, solo cambia de sección**: Cuando marcás un ítem pendiente, pasa de la sección superior a la inferior (debajo de "Completados"). El `animateContentSize` en el Column padre debería animar este reordenamiento, pero `key(item.id)` solo ayuda a Compose a identificar qué ítem es cuál — no garantiza animación de posición.

2. **Falta `AnimatedVisibility`**: El ítem que desaparece de "pendientes" y aparece en "completados" no tiene una transición de entrada/salida. Simplemente se recompone en su nueva posición.

3. **El delay de Room**: Cuando el usuario hace toggle, el ViewModel llama al repository → Room escribe → Flow emite → estado se actualiza → recomposición. Si este ciclo es rápido (< 16ms), Compose podría saltarse frames de animación.

4. **No hay animación de movimiento**: Para que un ítem "baje" visualmente de pendientes a completados, necesitás `animateItemPlacement()` o `Modifier.animateItemPlacement()` (LazyColumn) o `AnimatedContent` con transiciones de slide.

### Lo que SÍ funciona

- El fade del ítem individual (alpha de 1f a 0.58f)
- El color del checkbox (coral al marcar)
- El tachado del texto
- La animación del checkmark (escala + fade-in)

### Lo que NO funciona

- El movimiento del ítem de la sección "pendientes" a "completados"
- La transición visual de "este ítem cambió de lugar"

### Preguntas para análisis

1. ¿Se espera animación de movimiento (slide down) o solo fade?
2. ¿El `animateContentSize` del Column padre está causando glitches?
3. ¿Compose está recomponiendo en vez de animando porque los items cambian de padre (de before-divider a after-divider)?
4. ¿Sería mejor usar `LazyColumn` con `animateItemPlacement()`?

---

## Problema 3 — Jerarquía visual plana entre AnchorPreview y SupportsPreview

### Análisis del código actual

**AnchorPreviewSection:**
```kotlin
SectionHeader("Anclas pendientes", note = "X pendientes")
// → title: colorCardboard, DashboardSerif, 19.84sp
// → note: textMuted, DashboardSans, 12.48sp

Column(animateContentSize, bgSurface, rounded 20dp) {
    CheckItem(..., isInverted = false)
}
// → CheckItem: height 62dp, title 15.36sp SemiBold, alpha animado
// → CheckBoxMark: 23dp, coral al marcar
```

**SupportsPreviewSection:**
```kotlin
SectionHeader("Soportes", note = "X/Y pendientes")
// → title: MISMO colorCardboard, MISMO DashboardSerif, MISMO 19.84sp
// → note: MISMO textMuted, MISMO DashboardSans, MISMO 12.48sp

Column(animateContentSize, bgSurface, rounded 20dp) {
    CheckItem(..., isInverted = true)
}
// → CheckItem: MISMO height 62dp, MISMO title 15.36sp
// → CheckBoxMark: MISMO 23dp
```

**Son IDÉNTICOS.** No hay absolutamente ninguna diferencia visual entre ambas
secciones excepto el título de la sección y la semántica invertida del check.

### Por qué esto es un problema

Según `docs/nucleo-dominio-autonomia.md`:
- Anclas: "Pesan más que Soportes y Pendientes"
- Soportes: "Complementan las anclas, no compiten con ellas"

Y según `docs/vocal_mapa_componentes_v_0_2_borrador.md`:
- Las Anclas son la fuente inicial de datos de estabilidad
- Los Soportes son el piso mínimo que debería estar cubierto sin esfuerzo consciente

Si visualmente son iguales, el usuario no puede distinguir qué es prioritario.
El ojo no sabe dónde posarse primero. Esto contradice la filosofía del producto.

### Preguntas para definir la jerarquía visual

1. ¿Anclas debe ser MÁS GRANDE o Soportes MÁS CHICO? (o ambos)
2. ¿La diferencia debe ser de tamaño, color, posición, o combinación?
3. ¿El color del SectionHeader debería variar? (cartón para anclas, muted para soportes)
4. ¿Los CheckItems de soporte deberían ser visualmente distintos? (más chicos, menos contraste)
5. ¿Debería haber un separador visual explícito entre secciones?
6. ¿Cómo se relaciona esto con el futuro scoring? (anclas pesan más → deberían tener más presencia)

---

## Problema 4 — UX incompleta para semántica invertida de Soportes

### Lo que ya está implementado

```kotlin
// SupportsPreviewSection.kt
CheckItem(..., isInverted = true)

// CheckItem.kt — no hay diferencia visual por isInverted,
// solo cambia la semántica del check: checked = NO lo hizo

// DashboardViewModel.kt
fun onToggleSupport(activityId: String) {
    val currentlyCompleted = log?.completed == true
    completed = !currentlyCompleted  // INVIERTE
}
```

### Lo que falta (definido en la documentación)

Según `docs/nucleo-dominio-autonomia.md`:
> "El sistema asume que todo está hecho por defecto y solo registra omisiones."

Esto implica:
1. Al inicio del día, **todos los soportes deben aparecer como "cumplidos"** (checked = false en semántica invertida, completed = true en BD).
2. El usuario SOLO interactúa para desmarcar lo que NO hizo.
3. La app NUNCA debería mostrar soportes como "pendientes" al empezar el día.

### Preguntas de UX a resolver

1. **Indicador verbal**: ¿Qué texto ayuda al usuario a entender la semántica invertida sin ser condescendiente?
   - "Solo desmarca lo que no hiciste hoy" → suena a instrucción
   - "Todo cumplido por defecto" → más neutral
   - "Asumimos que ya lo hiciste. Desmarca si no." → cálido pero claro
   - O algo más sutil integrado en el diseño, no como texto explícito

2. **Botón global**: ¿Qué acción debería ofrecer?
   - "Restablecer todo" (vuelve a marcar todo como cumplido)
   - "Marcar todo cumplido" + "Desmarcar todo" (toggle)
   - "Hoy hice todo" (afirmación positiva)

3. **Visualización del estado**: ¿Cómo se ve un soporte "no cumplido"?
   - Actualmente: checked = true → checkbox coral, texto tachado, alpha bajo
   - ¿Esto es correcto? ¿O debería verse diferente porque es una OMISIÓN, no un "completado"?

4. **Inicialización diaria**: ¿El sistema garantiza que al empezar un nuevo día todos los soportes aparecen como cumplidos? Hay que verificar si Room/Flow resetea esto o si depende de logs del día actual.

5. **Comportamiento del check**: Cuando el usuario desmarca un soporte (indica que NO lo hizo), ¿el check debería ser coral (como ahora) o debería ser un color de advertencia diferente?

---

## Problema 5 — Redundancia de SupportsSection + necesidad de TasksSection

### Estructura actual

El dashboard tiene DOS secciones relacionadas con soportes/tasks:

**SupportsPreviewSection** (nueva, items individuales):
```
"Soportes" — X/Y pendientes
  ☐ Bañarse          Cuerpo
  ☐ Cepillarse dientes  Cuerpo
  editar soportes
```

**SupportsSection** (vieja, tarjetas de acceso):
```
"Soportes" — ligero
  ┌─────────────────┐  ┌─────────────────┐
  │ Soportes    icon │  │ Pendientes  icon │
  │ 2/4              │  │ 3                │
  │ cuidado básico   │  │ tareas abiertas  │
  │ ✓ Ducha marcada  │  │ ○ Pagar recibo   │
  │ ○ Dientes pend.  │  │ ○ Comprar cuerdas│
  └─────────────────┘  └─────────────────┘
```

### El problema

1. La tarjeta "Soportes" en `SupportsSection` es REDUNDANTE con `SupportsPreviewSection`. Muestra info duplicada (conteo 2/4, items específicos).

2. La tarjeta "Pendientes" en `SupportsSection` es el ÚNICO lugar donde se ven las tasks en el dashboard. Si eliminamos `SupportsSection`, necesitamos un reemplazo para tasks.

3. El título de sección "Soportes" aparece DOS VECES en el dashboard (SupportsPreviewSection y SupportsSection). Confuso.

### Restricciones

1. Las tasks NO tienen una sección de preview individual como los soportes. No son actividades recurrentes, son items puntuales.
2. Las tasks pueden o no estar asociadas a una capa.
3. Las tasks neutrales no suman al score.
4. La importancia de tasks es MENOR que soportes (que a su vez es menor que anclas).

### Preguntas para rediseño

1. ¿Eliminamos `SupportsSection` por completo? (las 2 tarjetas)
2. ¿Creamos `TasksPreviewSection` como reemplazo?
3. ¿Qué información mínima debe mostrar TasksPreviewSection?
   - ¿Solo el conteo? ("3 pendientes")
   - ¿Conteo + primeros 2 títulos?
   - ¿Conteo + capa asociada?
4. ¿TasksPreviewSection debe ser tappeable? (abre TasksPanel sheet)
5. ¿Dónde se ubica TasksPreviewSection en el orden del dashboard?
   - ¿Antes o después de la semana?
   - ¿Después de Soportes?
6. ¿Necesitamos mantener `DashboardSupportState` en el modelo o podemos eliminarlo?

---

## Arquitectura actual del DashboardState

Para referencia durante la solución:

```kotlin
data class DashboardState(
    val isLoading: Boolean,
    val status: DashboardStatusState,           // Tarjeta de estado + score
    val dailyProgress: DashboardDailyProgressState, // Barra de progreso
    val anchorPhrase: DashboardAnchorPhraseState,   // Frase ancla
    val layers: List<DashboardLayerState>,          // Capas de hoy
    val signals: List<DashboardSignalState>,        // Señales (sueño, proyecto, foco)
    val sobrietyTracks: List<DashboardSobrietyTrackState>, // Rachas
    val anchorItems: List<DashboardCheckItemState>,  // ← Anclas (AnchorPreviewSection)
    val supports: List<DashboardSupportState>,        // ← A ELIMINAR? (SupportsSection)
    val weekRows: List<DashboardWeekRowState>,        // Semana
    val dimensions: List<DashboardDimensionState>,     // (no se usa en UI actual)
    val sleep: DashboardSleepState,                   // Datos del sleep panel
    val activityOptions: List<DashboardActivityOptionState>, // Catálogo completo
    val supportItems: List<DashboardCheckItemState>,  // ← Soportes (SupportsPreviewSection)
    val pendingTasks: List<DashboardTaskState>,        // ← Tasks (NUEVO para TasksPreview)
)
```

---

## Próximos pasos

Cada problema se explorará individualmente usando `/sdd-explore` para
profundizar en el código, entender todas las aristas, y recién después
proponer soluciones concretas con su análisis de tradeoffs.

Orden propuesto de exploración:
1. Problema 1 (fuente de datos) — es el más urgente, bloquea funcionalidad
2. Problema 3 (jerarquía visual) — define cómo se verá todo lo demás
3. Problema 4 (UX invertida) — depende de la jerarquía visual definida
4. Problema 5 (SupportsSection/TasksSection) — cambio estructural grande
5. Problema 2 (animaciones) — es el más cosmético, puede esperar
