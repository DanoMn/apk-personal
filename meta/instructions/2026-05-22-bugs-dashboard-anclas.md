# Pro-Prompt: Bugs de Dashboard y Anclas

**Fecha**: 2026-05-22
**Origen**: Usuario reporta 5 bugs en la feature de anclas y dashboard.
**Estado**: Diagnostico completo. Listo para implementar.

---

## Contexto Tecnico

### Archivos involucrados

| Archivo | Rol | Cambio requerido |
|---------|-----|-----------------|
| `AutonomiaRepository.kt` | Logica de creacion de actividades | Bug 1, 2 |
| `DefaultSeeds.kt` | Datos semilla iniciales | Bug 2 |
| `ActionButtons.kt` | Botones de accion del dashboard | Bug 4, 5 |
| `DashboardScreen.kt` | Pantalla principal, conecta botones | Bug 4, 5 |
| `DashboardPanels.kt` | EntryMenuPanel y sheets | Bug 4, 5 |
| `DashboardProjection.kt` | buildDashboardState (checklistItems) | Bug 3 |

### Documentos de referencia

- `docs/prototipo/dashboard.html` — Prototipo visual de referencia
- `docs/frontend-design.md` — Guia de estilo
- `docs/nucleo-dominio-autonomia.md` — Dominio
- `meta/meta-prompting.md` — Glosario de terminos y decisiones

### Convenciones

- **Codigo**: Kotlin, Jetpack Compose, Room. Ingles.
- **UI/Texto**: Espanol. Nombres canonicos de `AGENTS.md`.
- **Visual**: Base oscura organica, carton/beige, coral mate. Tarjetas planas. Serif para titulos, sans para controles.
- **Arquitectura**: Room -> Repository -> ViewModel (StateFlow) -> Compose. Flujo unidireccional.
- **Strict TDD**: `enabled` (JUnit 4, 18 tests en 4 clases). Si los tests existen, deben pasar despues de cambios.
- **Proyecto engram**: `apk-personal`

---

## Bugs y Plan de Solucion

### Bug 1: Actividad personalizada con goal semanal/mensual no visible

**Causa raiz**: En `AutonomiaRepository.createActivity()` (linea 172), cuando `isGoal = true`, se sobrescribe `displaySurface` a `Contextual`, ignorando el valor `PrimaryChecklist` o `SecondaryChecklist` que eligio el usuario.

```kotlin
// Linea 172 actual:
displaySurface = if (isGoal) DisplaySurface.Contextual.name else displaySurface.name,
```

**Solucion**: Mantener el `displaySurface` elegido por el usuario, sin importar si es goal. La actividad debe respetar el displaySurface que viene del ViewModel.

```kotlin
// Solucion:
displaySurface = displaySurface.name,
```

**Efecto**: Las actividades con goal semanal/mensual se crearan en `PrimaryChecklist` y seran visibles en "Mis anclas" (configuracion) y en `activityOptions`. En el dashboard, el `checklistItems` seguira excluyendolas (correcto, los goals no son checkboxes diarios).

**Riesgo**: Los goals con `PrimaryChecklist` apareceran en `currentAnchors` del panel de configuracion, lo cual es deseado (el usuario quiere verlos).

---

### Bug 2: Usuario predeterminado con datos que no corresponden

**Causa raiz**: `DefaultSeeds.kt` siembra 30+ actividades predefinidas y 3 abstinence tracks. `ensureSeeded()` en `AutonomiaRepository` hace upsert de todo.

**Solucion**: Vaciar los seeds de actividades y abstinence tracks. Mantener solo las capas (layers) que son estructurales.

Cambios en `DefaultSeeds.kt`:
```kotlin
val activities = emptyList()  // Era una lista de 30+ actividades
val abstinenceTracks = emptyList()  // Era una lista de 3 tracks
```

Las capas (layers) se mantienen porque son estructurales y siempre necesarias.

**Nota**: Si la BD ya tiene datos de sesiones anteriores, se necesita limpiar la BD manualmente (desinstalar/reinstalar la app, o `RoomDatabase.clearAllTables()`). Esto es aceptable para testing.

---

### Bug 3: Dashboard inconsistente — anclas pendientes vs configuracion

**Causa raiz multiple**:

1. `buildDashboardState` excluye goals de `dashboardActivities` con `filterNot { it.isGoal() }`, pero `activityOptions` los incluye. Esto es intencional y correcto: goals no son checkboxes diarios pero deben ser configurables.

2. La inconsistencia real viene de Bug 1 (goals con `Contextual` no aparecen en ningun lado) + posible desincronizacion entre la BD y los flows.

3. El panel "Cuidado base" (SecondaryChecklist) muestra todo marcado posiblemente porque: actividades seed preexistentes + `secondaryChecklistItems` usa `completedActivities` que evalua `isCompletedBy()`.

**Solucion principal**: Al corregir Bug 1, los goals apareceran con su displaySurface correcto. Ademas, con Bug 2 (seed vacio), no habra datos preexistentes que contaminen el estado.

**Verificacion**: Despues de aplicar Bug 1 y Bug 2, el dashboard debe mostrar consistencia entre:
- `checklistItems` (Anclas pendientes) = solo PrimaryChecklist no-goals
- `activityOptions` (Configuracion) = todas las actividades incluyendo goals
- `secondaryChecklistItems` (Cuidado base) = solo SecondaryChecklist no-goals

---

### Bug 4: ActionButtons con 6 botones separados → un solo boton blanco + slider

**Estado actual**: `ActionButtons.kt` muestra 5-6 botones en grid 2x3. Cada boton abre su propio sheet directamente.

**Prototipo de referencia** (`dashboard.html` lineas 1168-1176):
```html
<div class="action-grid">
  <button class="primary-action">Registrar checklist</button>
  <button class="risk-action">flag</button>
</div>
```

**Solucion**: Reemplazar `ActionButtons` por un layout de 2 elementos en row:
1. Un boton blanco principal (`primary-action`, `palette.colorCardboard`) que al hacer clic abre `DashboardSheet.EntryMenu`
2. Un boton rojo al costado (`risk-action`, `mix(risk, 0.22, bgSurface)`) que abre `DashboardSheet.Relapse`

Cambios necesarios:
- `ActionButtons.kt`: Reescribir completamente. La nueva funcion recibe `onQuickConfigClick` y `onRiesgoClick` (solo 2 callbacks).
- `DashboardScreen.kt` linea 93-100: Actualizar la llamada:
  ```kotlin
  ActionButtons(
      palette = palette,
      onQuickConfigClick = { activeSheet = DashboardSheet.EntryMenu },
      onRiesgoClick = { activeSheet = DashboardSheet.Relapse },
  )
  ```
- `DashboardPanels.kt`: Remover "Sueno" del `EntryMenuPanel` (ver Bug 5).

**Layout nuevo de ActionButtons**:
```
[███████████████ Configuración rápida ███████████████] [🚩]
```
Row con: boton blanco (weight 1f) + spacer + boton rojo (58dp x 54dp).

---

### Bug 5: Sueno no debe estar en panel de configuracion rapida

**Estado actual**: `ActionButtons` tiene boton "Sueño" y `EntryMenuPanel` no tiene sueño (correcto), pero hay que verificar.

Revisando `EntryMenuPanel` (DashboardPanels.kt lineas 244-264):
```kotlin
SheetButton(text = "Mis anclas", ...)
SheetButton(text = "Cuidado base", ...)
SheetButton(text = "Pendientes", ...)
SheetButton(text = "Actividades, proyectos y goals", ...)
SheetButton(text = "Recaidas de sobriedad", ...)
```

No incluye "Sueño". OK.

**Solucion**: Remover el boton "Sueño" de `ActionButtons.kt`. Esto ya esta cubierto por Bug 4 (reescribir ActionButtons).

**Verificacion**: El acceso a sueño desde `SignalsSection` (linea 105 de DashboardScreen.kt) debe seguir funcionando.

---

## Orden de Implementacion

1. **Bug 2** (seeds vacios) — Primero, para no tener datos fantasmas durante el testeo
2. **Bug 1** (displaySurface en createActivity) — Core del problema
3. **Bug 4 + 5** (ActionButtons redesign) — Cambio de UI, remover sueño
4. **Bug 3** (verificacion de consistencia) — Verificar que todo funcione junto

## Verificacion Post-Cambio

1. Crear actividad personalizada con "3x semana" → debe aparecer en "Mis anclas" (configuracion)
2. Crear actividad sin goal → debe aparecer en "Anclas pendientes" del dashboard
3. Dashboard debe mostrar "Anclas pendientes" con las actividades PrimaryChecklist no-goal
4. Boton blanco "Configuracion rapida" abre slider con opciones (sin Sueno)
5. Boton rojo al costado abre recaidas
6. Click en senal de Sueno (SignalsSection) abre panel de sueño
7. Panel "Cuidado base" consistente con configuracion
