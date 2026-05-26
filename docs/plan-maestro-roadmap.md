# Plan Maestro — Roadmap de desarrollo

Fecha: 2026-05-23
Fuente: `docs/configuracion-canonica-sistema-v1.md`
Estado: vivo — se actualiza conforme se completan features.

---

## Propósito

Este documento define QUÉ falta implementar para cada feature del dominio,
basado en la configuración canónica. Cada ítem tiene referencias al código
actual y a la documentación relevante. Es la hoja de ruta para múltiples
sesiones de desarrollo.

---

## 1. Ancla

### ✅ Completado (2026-05-23)

- Targets obligatorios en UI de configuración
- `GoalPreset.None` eliminado
- `isAnchor()` ya no excluye goals
- Filtro `isGoal()` removido del dashboard

### 🔲 Pendiente

| ID | Tarea | Documentación | Código |
|----|-------|--------------|--------|
| A1 | **Registro de déficits/superávits** — Long-press en CheckItem para modificar tiempo real | `configuracion-canonica-sistema-v1.md` §1 | `CheckItem.kt` |
| A2 | **Validación server-side** — `addActivityAsAnchor()` en repositorio debe rechazar si targets son null | `configuracion-canonica-sistema-v1.md` §1 | `AutonomiaRepository.kt:266` |

---

## 2. Soporte

### ✅ Completado (2026-05-23)

- `SupportsConfigScreen` creada (básica)
- Jerarquía visual reducida en dashboard
- Indicador de semántica invertida
- Botón "Restablecer todo"
- `isInverted` funcional en `CheckBoxMark`
- `SectionHeader` con tamaño reducido

### 🔲 Pendiente

| ID | Tarea | Documentación | Código |
|----|-------|--------------|--------|
| S1 | **Agregar del catálogo de presets** — `SupportsConfigScreen` actualmente solo permite crear soportes personalizados. Debe permitir elegir de los 8 presets de Support definidos en `presets-actividades-v1.md` | `presets-actividades-v1.md` (Support section) | `SupportsConfigScreen.kt` |
| S2 | **Integrar navegación** — `SupportsConfigScreen` no está conectado al drawer ni a "editar soportes" | `plan-reestructuracion-3-capas.md` §2.5 | `MainActivity.kt`, `NavigationDrawer.kt` |
| S3 | **Inicialización diaria** — Verificar que al empezar un nuevo día, todos los soportes aparezcan como cumplidos por defecto | `configuracion-canonica-sistema-v1.md` §2 | `DashboardViewModel.kt`, `AutonomiaRepository.kt` |
| S4 | **onResetAll funcional** — El botón "Restablecer todo" necesita un callback real en el ViewModel | `plan-reestructuracion-3-capas.md` §3.7 | `DashboardViewModel.kt`, `DashboardScreen.kt` |

---

## 3. TaskList

### ✅ Completado (2026-05-23)

- `TasksPreviewSection` creada (tarjeta compacta en dashboard)
- Panel de tasks existente verificado (capa opcional, creación libre)

### ✅ Completado (2026-05-26)

- Pendientes sale de Configuración rápida; queda como lista operativa en dashboard y pantalla propia.
- `TaskPolicy` valida título, capa opcional, aporte neutral/soporte y transición `Done -> Pending`.
- `DashboardState` separa `pendingTasks` y `completedTasks`.
- `TasksScreen` permite crear, filtrar por capa, completar y revivir.
- `TasksPanel` / `DashboardSheet.Tasks` eliminado.

### 🔲 Pendiente

| ID | Tarea | Documentación | Código |
|----|-------|--------------|--------|
| T3 | **Archivo de pendientes** — Definir UX para `TaskStatus.Archived` si se necesita ocultar sin completar | `configuracion-canonica-sistema-v1.md` §3 | `TasksScreen.kt`, `AutonomiaRepository.kt` |

---

## 4. Sueño

### ✅ Actual

- Panel de registro desde dashboard (SleepPanel)
- SleepLog con campos: plannedSleepAt, plannedWakeAt, sleptAt, wokeAt, quality, note

### 🔲 Pendiente — Configuración

| ID | Tarea | Documentación | Código |
|----|-------|--------------|--------|
| D1 | **Quitar botón de calidad** — Decisión de producto: eliminar selector Baja/Aceptable/Buena. No hay forma objetiva de medirla | `configuracion-canonica-sistema-v1.md` §4 | `DashboardPanels.kt` (SleepPanel) |
| D2 | **Pantalla de configuración de objetivos** — Separar la configuración de objetivos de sueño (horario fijo) del registro diario | `configuracion-canonica-sistema-v1.md` §4 | Nuevo: `SleepConfigScreen.kt` |

### 🔲 Pendiente — Visión futura (NO implementar aún)

| ID | Tarea | Documentación |
|----|-------|--------------|
| D3 | **Modo "Hora de dormir"** — Botón que bloquea el teléfono durante la ventana configurada. Al desbloquear, registra tiempo dormido. Múltiples segmentos por noche | `configuracion-canonica-sistema-v1.md` §4 |
| D4 | **Mínimo 5 horas** — Implementar validación de ventana mínima configurable | `configuracion-canonica-sistema-v1.md` §4 |
| D5 | **Scoring de sueño como base** — Sueño ocupa los primeros ~700 puntos del sistema 700-1000 | `nucleo-dominio-autonomia.md` §Sueño |

---

## 5. Sobriedad

### ✅ Completado (2026-05-23)

- `SobrietyConfigScreen` creada (básica)

### ✅ Completado (2026-05-26)

- `SobrietyConfigScreen` conectada como pantalla profunda.
- Presets opt-in: Alcohol, Sustancias, Conducta sexual.
- Activar/desactivar rachas desde la pantalla profunda.
- Crear y eliminar rachas personalizadas.
- Marcar dia limpio desde dashboard.
- Registrar/desmarcar recaida del dia actual desde panel rapido.
- Dashboard solo muestra rachas activas; inactivas no aparecen, no pesan y no limitan.

### 🔲 Pendiente

| ID | Tarea | Documentación | Código |
|----|-------|--------------|--------|
| B2 | **Sistema de olvido** — Si el usuario no marca por 2-3 días, preguntar "¿Olvidaste marcar?". Sin respuesta → recaída | `configuracion-canonica-sistema-v1.md` §5 | `AutonomiaRepository.kt`, `DashboardViewModel.kt` |
| B3 | **Respetar versión del usuario** — En recaída, permitir al usuario modificar la cantidad de días. El sistema sugiere pero el usuario decide | `configuracion-canonica-sistema-v1.md` §5 | `AutonomiaRepository.kt` |

---

## 6. Scoring

### 🔲 Pendiente — Scope separado

El scoring completo es un scope independiente que requiere su propio SDD.
Este roadmap solo lista las dependencias hacia el scoring.

| ID | Tarea | Depende de |
|----|-------|-----------|
| SC1 | Sueño como piso del score (~700 puntos base) | D4, D5 |
| SC2 | Peso diferenciado: Anchor > Support > Task | A1, S3 |
| SC3 | Goals semanales/mensuales como bonus (0-100 puntos) | A1 |
| SC4 | Recaídas en abstinencias activas limitan el estado | Base implementada; B2 y B3 refinan el cierre |
| SC5 | Estados Inquebrantable y Plenitud requieren consistencia alta | SC1-SC4 |

---

## 7. Arquitectura

### 🔲 Pendiente

| ID | Tarea | Documentación |
|----|-------|--------------|
| AR1 | **Unificar `toDomain()` y `mergeToDomain()`** — Una sola función canónica para mapear definiciones a dominio | `analisis-codigo-pre-reestructuracion.md` §4 |
| AR2 | **Simplificar DashboardViewModel** — Reducir 15 flujos y 4 combines. El dominio debería recibir los datos ya preparados | `analisis-codigo-pre-reestructuracion.md` §5 |
| AR3 | **Eliminar `displaySurface` deprecado** — Campo marcado DEPRECATED en `ActivityDefinition` y `DashboardActivityOptionState` | `ActivityDefinition.kt:20`, `DashboardState.kt:140` |
| AR4 | **Mover `isGoal()` al dominio** — Actualmente en `domain/activity` pero la lógica de goals debería estar en scoring | `ActivityPolicy.kt` |

---

## Orden recomendado de ejecución

1. **S1, S2** — Soportes funcionales (presets + navegación)
2. **B2, B3** — Sobriedad: sistema de olvido y respeto al usuario
3. **D1, D2** — Sueño (quitar calidad + config screen)
4. **SC1-SC5** — Scoring (scope separado)
5. **AR1-AR4** — Limpieza arquitectónica
