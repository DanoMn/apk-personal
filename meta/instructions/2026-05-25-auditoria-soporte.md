# Auditoría completa — Feature Soporte

Tipo: Auditoría adversarial post-implementación
Scope: Revisar, corregir y validar la implementación completa de Soporte
Agente objetivo: `sdd-verify` o equivalente con capacidad de lectura/escritura de código

---

## 1. Contexto del producto

- **Producto**: Vocal / Autonomía sin límites
- **Stack**: Android Kotlin + Jetpack Compose + Room (local-first)
- **Package**: `dev.panopt.autonomia`
- **Idioma**: código en inglés, UI en español
- **Visual**: base oscura orgánica, cartón/beige, coral mate, serif en títulos, tarjetas planas sin bordes duros
- **Tono**: adulto funcional y compasivo, sin culpa, sin diagnóstico, sin moralizar

## 2. Qué se implementó

Reestructuración completa del feature **Soporte** (Support) en 3 fases:

### Fase 1 — CONFIGURACIÓN
- **F1.1** ✅ Seeds reemplazados: 12 presets `sup_*` en `DefaultSeeds.kt`, nuevo rol `AdministrativeOrder` en `Models.kt`
- **F1.2** Validación en repositorio: `addSupport()` y `removeSupport()` en `AutonomiaRepository.kt`
- **F1.3** `SupportsConfigPanel` extraído a `ui/supports/SupportsConfigPanel.kt` (nuevo archivo)
- **F1.4** Filtro por capas en `SupportsConfigScreen.kt` (chips: Todas + 5 capas)
- **F1.5** Deferred save en `SupportsConfigPanel`: cambios solo al cerrar, sección "Agregar soporte" para recuperar, botón "Ver catálogo completo"

### Fase 2 — DOMINIO
- **F2.1** `isSupport()` corregido en `ActivityPolicy.kt` (sin `!isGoal()`)
- **F2.2** `displaySurface` deprecated removido de `ActivityDefinition.kt`, `DomainMappers.kt`, `DashboardState.kt`, `DashboardProjection.kt`
- **F2.3** `DashboardCheckItemState` verificado (sin cambios necesarios)

### Fase 3 — DASHBOARD
- **F3.1-3.5** `SupportsPreviewSection.kt` rediseñado: colapsado por defecto, animación expand/colapsar, jerarquía visual reducida
- **F3.6** `DashboardViewModel.kt`: `toggleAllSupports()`, `saveSupportChecklist()`
- **F3.7-3.8** Conexiones verificadas en `DashboardScreen.kt` y `MainActivity.kt`

## 3. Documentos canónicos de referencia

Leer en este orden antes de auditar:

1. `docs/configuracion-canonica-sistema-v1.md` §2 — Reglas canónicas de Soporte
2. `docs/preset-soportes-v1.md` — Dataset de 12 presets (reglas de inclusión/exclusión)
3. `docs/definicion-reestructuracion-soporte.md` — Plan completo con UX esperada
4. `docs/contraste-soporte-actual-vs-esperado.md` — Gap analysis pre-implementación
5. `docs/nucleo-dominio-autonomia.md` §Soportes — Definición de dominio
6. `docs/definicion-tablas-room-v1.md` — Esquema Room (ActivitySurface, user_activity_configs)
7. `docs/frontend-design.md` — Guía visual (paleta, tipografía)
8. `docs/tono-comunicacion.md` — Voz del producto

## 4. Reglas de arquitectura — la tríada sagrada

```
CONFIGURACIÓN (valida) → DOMINIO (calcula) → DASHBOARD (presenta)
```

| Capa | Debe | NO debe |
|------|------|---------|
| Configuración | Validar capa, tipo, existencia. Guardar solo datos válidos. | Passthrough sin checks. |
| Dominio | Leer hechos, calcular estado con semántica invertida. | Validar, filtrar arbitrariamente, decidir qué se muestra. |
| Dashboard | Pintar estado, emitir acciones. | Definir reglas, calcular scoring, filtrar por capricho. |

### Reglas específicas de Soporte

1. **Sin targets**: `weeklyFrequencyTarget`, `sessionTargetMinutes`, `commitmentDurationMonths` DEBEN ser null
2. **Capa obligatoria**: todo soporte pertenece a exactamente una capa
3. **UX inversa**: sin log del día = cumplido. `completed = true` en el log = omisión (NO lo hizo)
4. **Jerarquía visual**: Soporte < Ancla. Tipografía más chica, colores más sutiles, colapsado por defecto
5. **Independencia**: Soporte no comparte núcleo de configuración con Ancla
6. **Deferred save**: en SupportsConfigPanel, cambios solo se persisten al cerrar

## 5. Archivos modificados (auditar TODOS)

| # | Archivo | Qué cambió |
|---|---------|-----------|
| 1 | `Models.kt:107` | `ActivityRole` + `AdministrativeOrder` |
| 2 | `DefaultSeeds.kt:142-183` | 12 presets `sup_*` reemplazan 8 viejos `act_*` |
| 3 | `AutonomiaDao.kt` | Nuevos queries: `getActivityDefinition`, `getLayer`, `getUserActivityConfig` |
| 4 | `AutonomiaRepository.kt` | `addSupport()` con validación, `removeSupport()` con verificación de tipo |
| 5 | `DashboardViewModel.kt` | `addToSupports`/`removeFromSupports` actualizados, `toggleAllSupports()`, `saveSupportChecklist()` |
| 6 | **`ui/supports/SupportsConfigPanel.kt`** | **NUEVO** — deferred save, local state, sección recuperación, botón redirigir |
| 7 | `DashboardPanels.kt` | `SupportsConfigPanel` removido, referencias actualizadas, entry menu "Cuidado" |
| 8 | `SupportsConfigScreen.kt` | Filtro por capas (chips horizontales), pre-selección en custom create |
| 9 | `ActivityPolicy.kt` | `isSupport()` sin `!isGoal()` |
| 10 | `ActivityDefinition.kt` | `displaySurface` removido |
| 11 | `DomainMappers.kt` | `displaySurface` removido de `toDomain()` y `mergeToDomain()` |
| 12 | `DashboardState.kt` | `displaySurface` removido de `DashboardActivityOptionState` |
| 13 | `DashboardProjection.kt` | `displaySurface` removido |
| 14 | `SupportsPreviewSection.kt` | Rediseño completo: colapsado, animación, jerarquía reducida, botones |
| 15 | `DashboardScreen.kt` | Nuevos callbacks para soporte |
| 16 | `MainActivity.kt` | Callbacks cableados al ViewModel |

## 6. Lo que hay que auditar — checklist por archivo

### 6.1 Models.kt
- [ ] `AdministrativeOrder` agregado al enum `ActivityRole` sin romper orden alfabético/lógico
- [ ] Ningún otro enum modificado accidentalmente

### 6.2 DefaultSeeds.kt
- [ ] 12 presets con IDs `sup_*` (no `act_*`)
- [ ] `presetCategory = "support"` en TODOS
- [ ] `contributionRole = Support` en TODOS
- [ ] Sin `act_dormir_temprano`, `act_agua`, `act_banarse`, `act_dientes`, `act_ropa`, `act_comer`, `act_orden_minimo`, `act_interaccion`
- [ ] sortOrders en rango 100-111 sin colisiones
- [ ] Capas correctas según `preset-soportes-v1.md`: Interior(1), Cuerpo(5), Conducta(5), Vínculos(1), Proyecto(0)

### 6.3 AutonomiaDao.kt
- [ ] `getActivityDefinition(id)` devuelve `ActivityDefinitionEntity?`
- [ ] `getLayer(id)` devuelve `LayerEntity?`
- [ ] `getUserActivityConfig(activityId)` devuelve `UserActivityConfigEntity?`

### 6.4 AutonomiaRepository.kt
- [ ] `addSupport()` valida que la definición existe
- [ ] `addSupport()` valida que la capa existe
- [ ] `addSupport()` guarda con TODOS los targets en null
- [ ] `addSupport()` guarda con `activityType = "Support"`
- [ ] `removeSupport()` verifica que la config existe Y es tipo Support antes de borrar
- [ ] `removeSupport()` NO borra configs de otro tipo (Anchor, Task)

### 6.5 DashboardViewModel.kt
- [ ] `addToSupports` llama a `repository.addSupport()`
- [ ] `removeFromSupports` llama a `repository.removeSupport()`
- [ ] `toggleAllSupports()` alterna correctamente entre marcar todo y desmarcar todo
- [ ] `toggleAllSupports()` solo afecta actividades con `activityType == Support`
- [ ] `saveSupportChecklist()` tiene comportamiento definido (no es no-op)

### 6.6 SupportsConfigPanel.kt (NUEVO)
- [ ] Estado local con `Set<String>` de removidos (no persiste en cada toggle)
- [ ] Al quitar: item desaparece de "Mis soportes", aparece en "Agregar soporte"
- [ ] Al re-agregar desde "Agregar soporte": vuelve a "Mis soportes"
- [ ] `DisposableEffect` o mecanismo equivalente persiste al cerrar/destruir
- [ ] Solo los items en el set de removidos se persisten como eliminados
- [ ] Los items re-agregados NO generan operación de escritura
- [ ] Botón "Ver catálogo completo" redirige a `SupportsConfigScreen`
- [ ] El panel recibe callback `onOpenFullConfig: () -> Unit`
- [ ] UI respeta el estilo visual (dark base, carton/beige, coral mate)

### 6.7 DashboardPanels.kt
- [ ] Viejo `SupportsConfigPanel` removido completamente
- [ ] Entry menu "Cuidado" → `DashboardSheet.SupportsConfig` funciona
- [ ] Sin imports huérfanos ni referencias rotas

### 6.8 SupportsConfigScreen.kt
- [ ] Chips de filtro: "Todas" + Interior, Cuerpo, Conducta, Vínculos, Proyecto
- [ ] Scroll horizontal en chips
- [ ] Al seleccionar capa: "Mis soportes" y "Catálogo" se filtran
- [ ] "Todas" muestra todas las capas
- [ ] "Crear soporte personalizado" preselecciona la capa del filtro activo
- [ ] UI de chips consistente con AnchorConfigScreen
- [ ] Sin regresiones: add/remove/create siguen funcionando

### 6.9 ActivityPolicy.kt
- [ ] `isSupport()` retorna `activityType == ActivitySurface.Support`
- [ ] Sin `&& !isGoal()` ni otros filtros

### 6.10 ActivityDefinition.kt
- [ ] Campo `displaySurface` removido del data class
- [ ] Import de `DisplaySurface` removido (si no hay otros usos)
- [ ] Sin referencias rotas en el resto del código

### 6.11 DomainMappers.kt
- [ ] `displaySurface` removido de `toDomain()` 
- [ ] `displaySurface` removido de `mergeToDomain()`
- [ ] Sin imports huérfanos

### 6.12 DashboardState.kt
- [ ] `displaySurface` removido de `DashboardActivityOptionState`
- [ ] `DashboardCheckItemState` tiene campo `isInverted: Boolean`

### 6.13 DashboardProjection.kt
- [ ] Sin referencias a `displaySurface`
- [ ] `supportItems` se construye con `isInverted = true` para soportes
- [ ] Semántica invertida funciona: sin log = `completed = false`, con log completado = `completed = true`

### 6.14 SupportsPreviewSection.kt
- [ ] **Colapsado por defecto**: el componente arranca cerrado
- [ ] Botón/touch area para expandir: "▶ Soportes — X/X hoy"
- [ ] Al expandir: animación `AnimatedVisibility` suave
- [ ] Indicador de semántica invertida: "Todo cumplido por defecto. Desmarcá solo lo que no hiciste hoy."
- [ ] Items: altura 48-52dp (NO 62dp como anclas)
- [ ] Items: tipografía 15sp (NO 20sp como anclas)
- [ ] Checkbox: 20dp, color ámbar/marrón cuando marcado (NO coral)
- [ ] Botón "Marcar todo" / "Desmarcar todo" funcional
- [ ] Botón "Guardar" visible y funcional
- [ ] `doneCount` calculado con semántica invertida: `items.count { !it.completed }`
- [ ] Link "editar soportes" funcional

### 6.15 DashboardScreen.kt
- [ ] `SupportsPreviewSection` recibe los callbacks nuevos
- [ ] `onAddSupport`, `onRemoveSupport`, `onToggleAllSupports`, `onSaveSupportChecklist` cableados
- [ ] Sin regresiones en otras secciones del dashboard

### 6.16 MainActivity.kt
- [ ] Callbacks de soporte conectados al ViewModel
- [ ] Navegación drawer → Soportes funcional
- [ ] Navegación dashboard → SupportsConfigScreen funcional
- [ ] Navegación bottom sheet → SupportsConfigPanel funcional

## 7. Correcciones esperadas

El agente auditor debe:

1. **Leer cada archivo** de la lista arriba
2. **Verificar contra la checklist** y contra los documentos canónicos
3. **Corregir inmediatamente** bugs obvios: imports faltantes, tipos incorrectos, null safety, regresiones
4. **Marcar como WARNING** desviaciones de arquitectura que no rompen compilación pero violan las reglas
5. **No tocar código de Anchor**, Sueño, Sobriedad ni Tasks
6. **No ejecutar tests** (regla del proyecto: seed changes no requieren tests)
7. **No cambiar el dataset de seeds** (ya fue aprobado por el usuario)

## 8. Formato de entrega

```markdown
# Reporte de Auditoría — Soporte

## CRITICAL (bugs — corregidos)
- [archivo:línea] descripción del bug + qué se corrigió

## WARNING (desviaciones de arquitectura/UX — no corregidos, requieren decisión)
- [archivo:línea] descripción de la desviación + sugerencia

## SUGGESTION (mejoras no críticas)
- [archivo:línea] descripción + sugerencia

## Archivos OK (sin hallazgos)
- lista de archivos que pasaron la auditoría

## Resumen
- Total archivos auditados: N
- Bugs corregidos: N
- Warnings: N
- Suggestions: N
```

## 9. Fuentes adicionales de verdad

- Nombres canónicos de UI: ver tabla en `AGENTS.md` (línea ~50-60)
- Estilo visual obligatorio: ver `AGENTS.md` §Estilo visual obligatorio
- Tono obligatorio: ver `AGENTS.md` §Tono obligatorio
- Principio de cero fricción: "tener una checklist de cosas diarias como cepillarse es molesto, asumimos que el usuario ya las completó"
- Momento de uso: "el usuario revisa esto antes de dormir, no es interacción frecuente durante el día"
