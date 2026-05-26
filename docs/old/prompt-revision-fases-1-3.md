# Prompt de revisión — Reestructuración Fases 1-3

> Estado 2026-05-25: prompt historico de revision. No usar como contrato
> vigente de Mis anclas. Para el patron actual usar
> `docs/mis-anclas-ux-canon-v1.md`.

## Contexto

Se completó la reestructuración del dashboard de Vocal en 3 fases. Necesito que
revisés, verifiques y corrijas si es necesario. NO implementes features nuevas.
Solo verificá que lo implementado sea correcto, completo y no rompa nada.

## Documentos que DEBÉS leer antes de revisar

1. `docs/configuracion-canonica-sistema-v1.md` — Reglas del dominio
2. `docs/plan-reestructuracion-3-capas.md` — El plan con cambios exactos
3. `docs/analisis-codigo-pre-reestructuracion.md` — Estado del código antes de los cambios

## Cambios realizados — Qué revisar

### Fase 1 — Dominio

| Archivo | Cambio | Verificar |
|---------|--------|-----------|
| `DashboardProjection.kt` | Eliminado filtro `isGoal()` (línea 53), `goalActivities` dead code, `secondaryActivities` alias, `metaLabel()`, `buildSupports()` completo | Que las anclas con metas semanales/mensuales ahora aparezcan en `anchorItems`. Que no haya referencias rotas a nada eliminado |
| `ActivityPolicy.kt` | `isAnchor()` ya no excluye goals (`&& !isGoal()` removido) | Que `isAnchor()` retorne true para actividades Anchor con metas |
| `DomainMappers.kt` | `toDomain()` ahora pone `active = true` en vez de `false` | Que actividades del catálogo tengan `active = true` |
| `DashboardState.kt` | Eliminados `DashboardSupportState` y `DashboardSupportKind` | Que ningún código referencie estos tipos eliminados |
| `DashboardScreen.kt` | Eliminada `SupportsSection` (las 2 tarjetas) | Que no haya referencias a `state.supports` |
| `SupportCard.kt` | Archivo eliminado | Que no queden imports o referencias |

### Fase 2 — Configuración

| Archivo | Cambio | Verificar |
|---------|--------|-----------|
| `AnchorConfigScreen.kt` | `GoalPreset.None` eliminado como default. Default ahora es `ThreePerWeek`. "Meta (opcional)" → "Meta (obligatoria)". Validación antes de guardar. Mensaje de error si no se selecciona meta | Que no se pueda guardar un ancla sin target. Que el mensaje de error aparezca. Que `CreateCustomActivitySection` también tenga la validación |
| `GoalPreset.kt` | Eliminado `None` del enum y de `toCountAndPeriod()` | Que no queden referencias a `GoalPreset.None` en producción |
| `GoalPresetGrid.kt` | Eliminado botón "Sin meta". Tabs ahora defaultean a presets válidos al cambiar | Que el grid funcione sin la opción "Sin meta" |
| `SupportsConfigScreen.kt` | **NUEVO** — Pantalla básica de configuración de soportes | Que compile, que los parámetros coincidan con el caller en MainActivity |
| `SobrietyConfigScreen.kt` | **NUEVO** — Pantalla básica de configuración de sobriedad | Que compile, que sea internal |

### Fase 3 — Dashboard

| Archivo | Cambio | Verificar |
|---------|--------|-----------|
| `TasksPreviewSection.kt` | **NUEVO** — Tarjeta compacta de tareas pendientes en dashboard | Que aparezca entre SupportsPreview y WeekSection |
| `AnchorPreview.kt` | Empty state agregado: "Sin anclas configuradas..." | Que cuando no hay anclas muestre el texto en vez de tarjeta vacía |
| `SupportsPreviewSection.kt` | Header reducido (textMuted, 17sp). Indicador de semántica invertida. Botón "Restablecer todo". `onResetAll` callback | Que el indicador aparezca, que el botón aparezca solo cuando hay omisiones |
| `CheckItem.kt` | `CheckBoxMark` acepta `isInverted`. Color ámbar/gris en vez de coral para items invertidos | Que los soportes marcados usen color distinto a coral |
| `LayerPill.kt` | `SectionHeader` acepta `titleColor` y `titleSize` opcionales | Que los headers de soportes y tasks usen los nuevos parámetros |
| `DashboardScreen.kt` | Agregado `TasksPreviewSection` entre SupportsPreview y WeekSection | Orden correcto de secciones |

### Tests

| Archivo | Cambio | Verificar |
|---------|--------|-----------|
| `ActivityPolicyTest.kt` | Test `weeklyGoalIsNotAnchor` → `anchor with weekly goal is still an anchor` | Que el test refleje la nueva regla |
| `DashboardProjectionTest.kt` | `secondaryChecklistItems` → `supportItems`. `checklistItems` → `anchorItems` | Que los tests pasen |
| `GoalPresetTest.kt` | Eliminadas referencias a `GoalPreset.None` | Que los tests pasen sin None |

## Reglas

- NO modificar la lógica de negocio
- NO agregar features nuevas
- Si algo no compila, arreglalo
- Si algo no sigue las reglas de `configuracion-canonica-sistema-v1.md`, corregilo
- Si encontrás código muerto o referencias rotas, eliminalas
- Si un test falla, arreglalo para que refleje la nueva realidad del dominio
