# Plan de reestructuración — 3 capas (basado en análisis de código)

Fecha: 2026-05-23
Basado en: `docs/analisis-codigo-pre-reestructuracion.md` (10 hallazgos)
Fuentes canónicas: `docs/configuracion-canonica-sistema-v1.md`, `docs/actividades-ancla-predeterminadas-v1.md`

---

## ⚠️ LECTURA OBLIGATORIA PARA EL AGENTE

Antes de tocar UNA SOLA línea de código, leé estos documentos en orden:

### Documentos creados en esta auditoría (2026-05-23)

1. **`docs/analisis-codigo-pre-reestructuracion.md`** — Estado real del código. 10 hallazgos con números de línea exactos. Te dice qué existe, qué está roto, qué es dead code.

2. **`docs/configuracion-canonica-sistema-v1.md`** — Definición canónica de las 5 features (Ancla, Soporte, TaskList, Sueño, Sobriedad). Las reglas del dominio que el código DEBE respetar.

3. **`docs/dashboard-auditoria-2026-05-23.md`** — Diagnóstico original de los 5 problemas del dashboard. Contexto de por qué se tomó cada decisión.

### Documentos canónicos del proyecto (pre-existentes)

4. **`docs/actividades-ancla-predeterminadas-v1.md`** — 33 anclas canónicas con definiciones y tono. Fuente de verdad para el catálogo de anclas.

5. **`docs/presets-actividades-v1.md`** — Dataset técnico para seeds. 33 anclas + 8 soportes con IDs, tipos, capas. Sin targets (los pone el usuario).

6. **`docs/nucleo-dominio-autonomia.md`** — Núcleo del dominio. Capas, scoring, estados, reglas de surface (Anchor/Support/Task).

7. **`docs/definicion-tablas-room-v1.md`** — Esquema Room v4. **Verificado y actualizado 2026-05-23.** PK corregida, campos `customName`/`customDescription`/`createdAt` agregados, reglas de targets por superficie documentadas.

8. **`docs/frontend-design.md`** — Paleta, tipografía, tarjetas, iconografía. La guía visual vigente.

9. **`docs/tono-comunicacion.md`** — Cómo habla Vocal. Sin culpa, sin diagnóstico, sin coach barato.

NO improvisar. NO proponer soluciones alternativas. Los documentos ya contienen las decisiones tomadas.

---

## Principio

```
CONFIGURACIÓN → DOMINIO → DASHBOARD
   valida         calcula      presenta
```

---

## Fase 1 — DOMINIO (quitar filtros que rompen el dashboard)

**Motivo:** va primero porque sin esto, la Fase 2 no se puede verificar visualmente.
Si arreglás la configuración pero el dominio sigue filtrando, no vas a ver el resultado.

### 1.1 DashboardProjection.kt — Eliminar filtro isGoal()

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/dashboard/DashboardProjection.kt`

| Línea | Cambio |
|-------|--------|
| 52 | **ELIMINAR** `val goalActivities = visibleActivities.filter { it.isGoal() }` — dead code, nunca se usa |
| 53 | **ELIMINAR** `.filterNot { it.isGoal() }` de `dashboardActivities`. Debe ser: `val dashboardActivities = visibleActivities` |

**Resultado:** las anclas con metas semanales/mensuales aparecen en `anchorItems` y por lo tanto en el dashboard.

### 1.2 DashboardProjection.kt — Eliminar más código muerto

| Línea | Cambio |
|-------|--------|
| 64 | **ELIMINAR** `val secondaryActivities = selfCareActivities` — alias redundante. Usar `selfCareActivities` directamente |
| 382-389 | **ELIMINAR** función `metaLabel()` — definida pero nunca invocada |

### 1.3 ActivityPolicy.kt — Quitar !isGoal() de isAnchor()

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/activity/ActivityPolicy.kt`

| Línea | Cambio |
|-------|--------|
| 10-11 | Cambiar `activityType == Anchor && !isGoal()` → `activityType == Anchor` |

**Resultado:** `isAnchor()` refleja correctamente el dominio. Una actividad tipo Anchor ES un ancla, tenga o no metas periódicas.

### 1.4 DomainMappers.kt — Corregir divergencia de active

**Archivo:** `app/src/main/java/dev/panopt/autonomia/data/local/mapper/DomainMappers.kt`

| Línea | Cambio |
|-------|--------|
| 85 | `toDomain()` pone `active = false`. Esto es incorrecto: una definición de catálogo debería reflejar el estado real. **Dejar `active = true`** (el default del data class). El filtro de si está configurada o no lo hace `isConfigured` en la proyección. |

### 1.5 DashboardState.kt — Eliminar DashboardSupportState

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/dashboard/DashboardState.kt`

| Línea | Cambio |
|-------|--------|
| 15 | **ELIMINAR** `val supports: List<DashboardSupportState>` |
| 86-101 | **ELIMINAR** `DashboardSupportState` data class y `DashboardSupportKind` enum |

### 1.6 DashboardProjection.kt — Eliminar buildSupports()

| Línea | Cambio |
|-------|--------|
| 174-179 | **ELIMINAR** `supports = buildSupports(...)` |
| 500-538 | **ELIMINAR** función `buildSupports()` completa |

**Resultado de la Fase 1:** el dominio ya no filtra anclas con metas, no tiene código muerto, y no produce el modelo de datos de la sección duplicada de soportes.

---

## Fase 2 — CONFIGURACIÓN (validar datos antes de guardar)

### 2.1 AnchorConfigScreen.kt — Obligar targets

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/anchors/AnchorConfigScreen.kt`

| Línea | Cambio |
|-------|--------|
| 780 | `GoalPreset.None` → **eliminar como opción seleccionable**. El default debe ser `GoalPreset.ThreePerWeek` o forzar al usuario a elegir |
| 808 | `"Meta (opcional)"` → `"Meta (obligatoria)"` |
| 880-887 | **Agregar validación**: si `selectedGoal == GoalPreset.None` o `targetValue == null`, no ejecutar `onConfirm`. Mostrar un `Text` de error: "El objetivo es obligatorio para las anclas" |
| GoalPreset.kt:10 | **Eliminar** `None("Sin meta")` del enum |

### 2.2 AnchorConfigScreen.kt — CreateCustomActivitySection

| Línea | Cambio |
|-------|--------|
| 952 | Mismo cambio: eliminar `GoalPreset.None` como default |
| 1017 | `"Meta (opcional)"` → `"Meta (obligatoria)"` |
| 1126-1153 | Agregar validación antes de `onConfirm` |

### 2.3 GoalPreset.kt — Eliminar None

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/anchors/GoalPreset.kt`

| Línea | Cambio |
|-------|--------|
| 10 | **ELIMINAR** `None("Sin meta")` |
| 37-38 | **ELIMINAR** `None -> null to null` del `when` en `toCountAndPeriod()` |

### 2.4 GoalPresetGrid.kt — Quitar botón "Sin meta"

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/anchors/GoalPresetGrid.kt`

| Cambio |
|--------|
| **ELIMINAR** el botón "Sin meta" del grid de presets |

### 2.5 Crear SupportConfigScreen.kt (NUEVO)

**Archivo nuevo:** `app/src/main/java/dev/panopt/autonomia/ui/supports/SupportConfigScreen.kt`

Contenido mínimo:
- TopBar con título "Soportes" y botón volver
- Sección "Soportes actuales": lista de los que ya tiene configurados, con botón "Quitar"
- Sección "Agregar soporte": elegir del catálogo de presets Support
- Botón "+ Crear soporte personalizado": diálogo con campo nombre + selector de capa (obligatorio)
- Sin campos de tiempo ni frecuencia (por diseño del dominio)

### 2.6 Verificar TasksPanel existente

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardPanels.kt`

Verificar que:
- La capa sea opcional (ya lo es)
- Las tareas completadas desaparezcan del panel (verificar `TasksPanel`)
- Agregar capacidad de "revivir" tareas completadas (mostrar historial y reactivar)

### 2.7 Crear SobrietyConfigScreen.kt (NUEVO)

**Archivo nuevo:** `app/src/main/java/dev/panopt/autonomia/ui/sobriety/SobrietyConfigScreen.kt`

Contenido mínimo:
- TopBar con título "Sobriedad" y botón volver
- Lista de tracks activos con sus rachas
- Botón "Agregar track personalizado" (nombre libre)
- Botón "Eliminar" solo en tracks personalizados
- Tracks predeterminados (Alcohol, Sustancias, Conducta sexual): no eliminables, solo desactivables

---

## Fase 3 — DASHBOARD (presentar, no decidir)

### 3.1 DashboardScreen.kt — Eliminar SupportsSection

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardScreen.kt`

| Línea | Cambio |
|-------|--------|
| 128-133 | **ELIMINAR** `SupportsSection(...)` completo |
| 43 | **ELIMINAR** import `SupportsSection` |

### 3.2 DashboardScreen.kt — Agregar TasksPreviewSection

| Cambio |
|--------|
| Después de `SupportsPreviewSection`, agregar `TasksPreviewSection(palette, tasks = state.pendingTasks, onOpenTasks = { activeSheet = DashboardSheet.Tasks })` |

### 3.3 Crear TasksPreviewSection.kt (NUEVO)

**Archivo nuevo:** `app/src/main/java/dev/panopt/autonomia/ui/dashboard/components/TasksPreviewSection.kt`

Contenido:
- SectionHeader con título "Pendientes" y nota "X abiertos"
- Tarjeta compacta (fondo `mix(bgSurface, 0.6)`, texto `textMuted`)
- Muestra los primeros 2 títulos de tareas pendientes
- Si no hay tareas: texto "Sin pendientes"
- Click abre `TasksPanel` sheet

### 3.4 Ajustar jerarquía visual

**Archivos:** `AnchorPreview.kt`, `SupportsPreviewSection.kt`, `LayerPill.kt`

| Componente | Cambio |
|-----------|--------|
| `AnchorPreviewSection` | **Sin cambios.** Mantiene `SectionHeader` con `colorCardboard` 20sp, `CheckItem` 62dp |
| `SupportsPreviewSection` | `SectionHeader`: título en `textMuted` 17sp (en vez de `colorCardboard` 20sp). `CheckItem`: altura 52dp (en vez de 62dp). Checkbox más chico (20dp en vez de 23dp) |
| `TasksPreviewSection` | El de menor peso visual. `SectionHeader` en `textMuted` 15sp. Tarjeta con fondo semitransparente |

### 3.5 AnchorPreviewSection — Agregar empty state

**Archivo:** `AnchorPreview.kt`

| Cambio |
|--------|
| Si `items.isEmpty()`: mostrar texto "Sin anclas configuradas. Agregá actividades a tu base diaria." en vez de tarjeta vacía |

### 3.6 Animaciones en checklists

**Archivos:** `AnchorPreview.kt`, `SupportsPreviewSection.kt`

| Cambio |
|--------|
| Envolver `pendingItems` y `completedItems` en `AnimatedVisibility` con `slideInVertically` / `slideOutVertically` |
| Agregar `key(item.id)` en cada `CheckItem` para trackear identidad entre secciones |
| Pequeño delay (100ms) post-toggle antes de actualizar el estado para que la animación de salida se complete |

### 3.7 SupportsPreviewSection — UX semántica invertida

**Archivo:** `SupportsPreviewSection.kt`

| Cambio |
|--------|
| Entre el header y los items: `Text` pequeño (11sp, `textMuted`, itálico): "Todo cumplido por defecto. Desmarcá solo lo que no hiciste hoy." |
| Botón "Restablecer todo" (`TextButton`) que pone todos los items como no marcados |
| Cuando un soporte está desmarcado (checked = true = NO lo hizo), el checkbox usa color ámbar/gris en vez de coral (porque no es un logro) |

### 3.8 CheckItem.kt — Hacer funcionar isInverted

**Archivo:** `CheckItem.kt`

| Cambio |
|--------|
| Cuando `isInverted = true`, el `CheckBoxMark` usa `palette.textMuted` o un color ámbar en vez de `palette.colorCoral` al estar checked |

---

## Archivos afectados — Resumen

| Archivo | Fase | Tipo |
|---------|------|------|
| `DashboardProjection.kt` | 1.1, 1.2, 1.6 | Eliminar filtros, dead code, buildSupports |
| `ActivityPolicy.kt` | 1.3 | Quitar `!isGoal()` de isAnchor |
| `DomainMappers.kt` | 1.4 | Corregir `active = false` en toDomain |
| `DashboardState.kt` | 1.5 | Eliminar DashboardSupportState |
| `AnchorConfigScreen.kt` | 2.1, 2.2 | Obligar targets |
| `GoalPreset.kt` | 2.3 | Eliminar None |
| `GoalPresetGrid.kt` | 2.4 | Quitar botón "Sin meta" |
| `SupportConfigScreen.kt` | 2.5 | **NUEVO** |
| `DashboardPanels.kt` | 2.6 | Verificar TasksPanel |
| `SobrietyConfigScreen.kt` | 2.7 | **NUEVO** |
| `DashboardScreen.kt` | 3.1, 3.2 | Quitar SupportsSection, agregar TasksPreview |
| `TasksPreviewSection.kt` | 3.3 | **NUEVO** |
| `AnchorPreview.kt` | 3.5, 3.6 | Empty state, animaciones |
| `SupportsPreviewSection.kt` | 3.4, 3.6, 3.7 | Jerarquía, animaciones, UX invertida |
| `CheckItem.kt` | 3.8 | isInverted funcional |
| `LayerPill.kt` | 3.4 | SectionHeader con variante reducida |
| `SupportCard.kt` | 3.1 | **ARCHIVAR** (ya no se usa) |

---

## Orden de ejecución

```
Fase 1 (Dominio) → Fase 2 (Configuración) → Fase 3 (Dashboard)
```

Cada fase se verifica antes de pasar a la siguiente.

---

## Documentos relacionados

### Canónicos (vigentes)

| Documento | Rol |
|-----------|-----|
| `docs/actividades-ancla-predeterminadas-v1.md` | 33 anclas canónicas con definiciones |
| `docs/presets-actividades-v1.md` | Dataset técnico para seeds (33 anclas + 8 soportes) |
| `docs/nucleo-dominio-autonomia.md` | Núcleo del dominio, capas, scoring, reglas |
| `docs/definicion-tablas-room-v1.md` | Esquema Room v4 |
| `docs/frontend-design.md` | Guía visual (paleta, tipografía, iconografía) |
| `docs/tono-comunicacion.md` | Voz de Vocal |
| `docs/configuracion-canonica-sistema-v1.md` | Definición de las 5 features |
| `docs/analisis-codigo-pre-reestructuracion.md` | 10 hallazgos del código actual |
| `docs/dashboard-auditoria-2026-05-23.md` | Diagnóstico de los 5 problemas |
| `docs/plan-reestructuracion-3-capas.md` | Este documento |

### Deprecados (NO USAR)

| Documento | Motivo |
|-----------|--------|
| `docs/old/presets-actividades-v1.md` | Versión anterior con anclas espurias y columna Min |
| `docs/old/canvas-reestructuracion-autonomia.md` | Borrador antiguo |
| `docs/old/especificacion-actividades-sobriedad-v1.md` | Especificación vieja |
