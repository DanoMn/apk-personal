# Mapa de flujos del proyecto — estado actual

Fecha: 2026-05-24
Proyecto: Vocal / Autonomia sin limites
Proposito: dejar una referencia visual clara de como fluye la app despues de la
reestructuracion de dashboard Fases 1-3.

Este documento describe el estado implementado a dia de hoy. No reemplaza a los
documentos canonicos de dominio; sirve como mapa de lectura para ubicarse rapido
en el codigo.

---

## 1. Mapa general

```mermaid
flowchart TD
    Usuario["Usuario"] --> MainActivity["MainActivity"]
    MainActivity --> DashboardScreen["DashboardScreen"]
    MainActivity --> AnchorConfigScreen["AnchorConfigScreen / Mis anclas"]
    MainActivity --> SupportsConfigScreen["SupportsConfigScreen / Soportes"]
    MainActivity --> TasksScreen["TasksScreen / Pendientes"]
    MainActivity --> SobrietyConfigScreen["SobrietyConfigScreen / Sobriedad"]

    DashboardScreen --> DashboardSheetHost["DashboardSheetHost / paneles inferiores"]
    DashboardScreen --> NavigationDrawer["NavigationDrawer / menu lateral"]

    DashboardScreen --> AnchorPreview["AnchorPreviewSection / Mis anclas"]
    DashboardScreen --> SupportsPreview["SupportsPreviewSection / Soportes"]
    DashboardScreen --> TasksPreview["TasksPreviewSection / Pendientes"]
    DashboardScreen --> SobrietySection["SobrietySection / Sobriedad"]
    DashboardScreen --> WeekSection["WeekSection / semana"]

    NavigationDrawer --> AnchorConfigScreen
    NavigationDrawer --> SupportsConfigScreen
    NavigationDrawer --> TasksScreen
    NavigationDrawer --> SobrietyConfigScreen

    DashboardSheetHost --> EntryMenu["EntryMenuPanel"]
    DashboardSheetHost --> AnchorPanel["AnchorPanel / registrar ancla"]
    DashboardSheetHost --> SupportPanel["AnchorPanel usado como soporte"]
    DashboardSheetHost --> SleepPanel["SleepPanel"]
    DashboardSheetHost --> ActivitySettingsPanel["ActivitySettingsPanel"]
    DashboardSheetHost --> RelapsePanel
```

### Lectura rapida

- La pantalla inicial real es `DashboardScreen`.
- `MainActivity` cambia entre pantallas completas: Dashboard, Mis anclas,
  Soportes, Pendientes y Sobriedad.
- El dashboard tambien abre paneles inferiores para registro rapido.
- `SobrietyConfigScreen` gestiona presets opt-in y rachas personalizadas.

---

## 2. Flujo de datos del dashboard

```mermaid
flowchart TD
    Room["Room / tablas locales"] --> Repository["AutonomiaRepository"]

    Repository --> LayersFlow["layersFlow"]
    Repository --> ConfiguredActivities["observeConfiguredActivities"]
    Repository --> CatalogActivities["observeCatalogActivities"]
    Repository --> ActivityLogs["activityLogsForDate / week / month"]
    Repository --> AbstinenceFlows["abstinenceTracks / abstinenceLogs"]
    Repository --> RiskEvents["riskEventsForDate"]
    Repository --> TasksFlow["tasksFlow"]
    Repository --> AnchorPhrases["anchorPhrasesFlow"]
    Repository --> SleepFlow["sleepLogForDate"]
    Repository --> FocusSignal["focusSignalActivityIdFlow"]

    LayersFlow --> ViewModel["DashboardViewModel"]
    ConfiguredActivities --> ViewModel
    CatalogActivities --> ViewModel
    ActivityLogs --> ViewModel
    AbstinenceFlows --> ViewModel
    RiskEvents --> ViewModel
    TasksFlow --> ViewModel
    AnchorPhrases --> ViewModel
    SleepFlow --> ViewModel
    FocusSignal --> ViewModel

    ViewModel --> CoreSnapshot["DashboardCoreSnapshot"]
    CoreSnapshot --> FactSnapshot["DashboardFactSnapshot"]
    FactSnapshot --> Engine["DashboardEngine.buildState"]
    Engine --> Projection["buildDashboardState"]
    Projection --> ScoreEngine["ScoreEngine.calculate"]
    ScoreEngine --> Projection
    Projection --> DashboardState["DashboardState"]
    DashboardState --> DashboardScreen["DashboardScreen"]
```

### Regla estructural

```mermaid
flowchart LR
    Configuracion["Configuracion valida"] --> Dominio["Dominio calcula"]
    Dominio --> Dashboard["Dashboard presenta"]

    Configuracion -.-> Room["Room guarda hechos"]
    Room -.-> Dominio
```

- La configuracion decide que se puede guardar.
- Room conserva hechos y configuracion local.
- El dominio interpreta esos hechos.
- Compose no decide reglas del dominio; pinta `DashboardState` y emite acciones.

---

## 3. Modelo de actividades

```mermaid
flowchart TD
    ActivityDefinitionEntity["ActivityDefinitionEntity / Catalogo"] --> CatalogDomain["toDomain / opcion de catalogo"]
    ActivityDefinitionEntity --> Merge["mergeToDomain"]
    UserActivityConfigEntity["UserActivityConfigEntity / Mis actividades"] --> Merge
    Merge --> ConfiguredDomain["ActivityDefinition configurada"]

    ConfiguredDomain --> Surface{"ActivitySurface"}
    Surface --> Anchor["Anchor / Mis anclas"]
    Surface --> Support["Support / Soportes"]
    Surface --> Task["Task / Pendientes"]

    Anchor --> AnchorRules["Targets obligatorios: tiempo, count, periodo"]
    Support --> SupportRules["Sin targets, capa obligatoria"]
    Task --> TaskRules["Una sola vez, capa opcional"]
```

### Estado actual de cada superficie

| Superficie | Configuracion | Registro diario | Dashboard |
|------------|---------------|-----------------|-----------|
| Mis anclas | `AnchorConfigScreen` valida target obligatorio | `toggleActivity` / `saveActivityValue` | `AnchorPreviewSection` |
| Soportes | `SupportsConfigScreen` agrega catalogo o custom sin targets | `onToggleSupport` usa semantica invertida | `SupportsPreviewSection` |
| Pendientes | `TasksScreen` crea y revive tareas | `completeTask` marca Done | `TasksPreviewSection` muestra solo abiertos |

---

## 4. Flujo de Mis anclas

```mermaid
flowchart TD
    User["Usuario"] --> AnchorConfig["Mis anclas"]
    AnchorConfig --> Catalog["Anclas disponibles"]
    AnchorConfig --> Current["Anclas actuales"]
    AnchorConfig --> Custom["Crear actividad personalizada"]

    Catalog --> WeeklySelector["WeeklyFrequencySelector 2..7/semana"]
    Custom --> WeeklySelector
    WeeklySelector --> DurationDialog["CommitmentDurationDialog"]
    DurationDialog --> TimeTarget["Tiempo objetivo 1..900 min"]
    TimeTarget --> Validation{"weeklyFrequencyTarget + sessionTargetMinutes validos?"}

    Validation -- "No" --> Error["Mostrar: La meta semanal y el tiempo objetivo son obligatorios."]
    Validation -- "Si" --> AddAnchor["DashboardViewModel.addActivityAsAnchor"]
    Validation -- "Si / custom" --> CreateActivity["DashboardViewModel.createActivity"]

    AddAnchor --> Repository["AutonomiaRepository.configureActivity"]
    CreateActivity --> Definition["upsertActivityDefinition"]
    CreateActivity --> Config["upsertUserActivityConfig"]
    Repository --> UserActivityConfig["user_activity_configs"]
    Definition --> ActivityDefinitions["activity_definitions"]
    Config --> UserActivityConfig

    UserActivityConfig --> DashboardProjection["buildDashboardState"]
    DashboardProjection --> AnchorItems["DashboardState.anchorItems"]
    AnchorItems --> AnchorPreview["AnchorPreviewSection"]
```

### Reglas vigentes

- Una ancla nueva siempre tiene `weeklyFrequencyTarget` obligatorio entre 2 y 7.
- Una ancla nueva siempre tiene `sessionTargetMinutes` obligatorio entre 1 y 900.
- `commitmentDurationMonths = null` representa **Indefinido** y es una configuracion valida.
- La frecuencia mensual ya no existe en la UI de anclas; `TargetPeriod.Month` queda solo como compatibilidad legacy.
- Las anclas aparecen todos los dias en `anchorItems`, aunque su meta sea semanal.

---

## 5. Flujo de Soportes

```mermaid
flowchart TD
    User["Usuario"] --> SupportsConfig["Soportes"]
    SupportsConfig --> SupportCatalog["Agregar soporte del catalogo"]
    SupportsConfig --> CustomSupport["Crear soporte personalizado"]
    SupportsConfig --> CurrentSupports["Soportes actuales"]

    SupportCatalog --> AddToSupports["DashboardViewModel.addToSupports"]
    AddToSupports --> ConfigureSupport["configureActivity como ActivitySurface.Support"]

    CustomSupport --> CreateSupport["DashboardViewModel.createActivity isSecondary=true"]
    CreateSupport --> SupportDefinition["ActivityDefinition: unit Boolean"]
    CreateSupport --> SupportConfig["UserActivityConfig: targets null"]

    CurrentSupports --> RemoveSupport["removeFromSupports"]
    RemoveSupport --> DeleteConfig["deleteUserActivityConfig"]

    ConfigureSupport --> UserActivityConfig["user_activity_configs"]
    SupportConfig --> UserActivityConfig
    SupportDefinition --> ActivityDefinitions["activity_definitions"]

    UserActivityConfig --> DashboardProjection["buildDashboardState"]
    DashboardProjection --> SupportItems["DashboardState.supportItems"]
    SupportItems --> SupportsPreview["SupportsPreviewSection"]

    SupportsPreview --> ToggleSupport["onToggleSupport"]
    ToggleSupport --> ActivityLog["activity_logs del dia"]
    SupportsPreview --> ResetAll["Restablecer todo"]
    ResetAll --> ClearSupportLogs["resetSupportOmissions / borra omisiones"]
```

### Semantica invertida

```mermaid
stateDiagram-v2
    [*] --> AssumedDone: Sin log del dia
    AssumedDone: Se asume cumplido
    AssumedDone --> Omitted: Usuario desmarca / registra que no lo hizo
    Omitted: Log completed=true como omision visual
    Omitted --> AssumedDone: Usuario vuelve a tocar o Restablecer todo
```

- Soportes no tienen targets.
- El sistema asume todo cumplido por defecto.
- El usuario marca solo lo que no hizo.
- `Restablecer todo` limpia las omisiones del dia.

---

## 6. Flujo de Pendientes

```mermaid
flowchart TD
    User["Usuario"] --> TasksScreen["TasksScreen / pantalla completa"]
    Dashboard["DashboardScreen / seccion Pendientes"] --> CompleteTask
    Dashboard --> TasksScreen

    TasksScreen --> CreateTask["DashboardViewModel.createTask"]
    CreateTask --> RepositoryCreate["AutonomiaRepository.createTask"]
    RepositoryCreate --> TaskPolicy["TaskPolicy.createDraft"]
    TaskPolicy --> TaskEntity["tasks / status Pending"]

    TasksScreen --> CompleteTask["DashboardViewModel.completeTask"]
    CompleteTask --> RepositoryDone["AutonomiaRepository.completeTask"]
    RepositoryDone --> TaskDone["tasks / status Done"]

    TasksScreen --> ReactivateTask["DashboardViewModel.reactivateTask"]
    ReactivateTask --> RepositoryReactivate["AutonomiaRepository.reactivateTask"]
    RepositoryReactivate --> TaskEntity

    TaskEntity --> Projection["buildDashboardState"]
    TaskDone --> Projection
    Projection --> PendingTasks["DashboardState.pendingTasks"]
    Projection --> CompletedTasks["DashboardState.completedTasks"]
    PendingTasks --> TasksPreview["TasksPreviewSection"]
    PendingTasks --> TasksScreen
    CompletedTasks --> TasksScreen
```

### Regla vigente

- Solo se muestran en dashboard las tareas con `TaskStatus.Pending`.
- Completar una tarea la saca de `pendingTasks` y la mueve a `completedTasks`.
- Revivir una tarea completada la vuelve `Pending`.
- La capa es opcional; sin capa queda `ContributionRole.Neutral`, con capa queda `ContributionRole.Support`.
- Pendientes no aparece en Configuracion rapida; no configura una base recurrente.

---

## 7. Flujo de Sueño y Sobriedad

```mermaid
flowchart TD
    Dashboard["DashboardScreen"] --> SleepSignal["SignalsSection / Sueño"]
    SleepSignal --> SleepPanel["SleepPanel"]
    SleepPanel --> SaveSleep["DashboardViewModel.saveSleep"]
    SaveSleep --> SleepPolicy["SleepPolicy.validatePlannedWindow"]
    SleepPolicy --> SleepLog["sleep_logs"]

    Dashboard --> SobrietySection["SobrietySection"]
    Dashboard --> SobrietyConfig["SobrietyConfigScreen"]
    SobrietyConfig --> SetTrackActive["setAbstinenceTrackActive"]
    SobrietyConfig --> CreateTrack["createCustomAbstinenceTrack"]
    SobrietyConfig --> DeleteTrack["deleteCustomAbstinenceTrack"]
    SetTrackActive --> AbstinenceTrack["abstinence_tracks / active"]
    CreateTrack --> AbstinenceTrack
    DeleteTrack --> AbstinenceTrack
    SobrietySection --> ToggleClean["toggleAbstinenceClean"]
    ToggleClean --> AbstinenceLogClean["abstinence_logs / Clean"]

    Dashboard --> RelapsePanel["RelapsePanel"]
    RelapsePanel --> ToggleRelapse["toggleAbstinenceRelapse"]
    ToggleRelapse --> AbstinenceLogRelapse["abstinence_logs / Relapse"]

    AbstinenceTrack --> Projection["buildDashboardState"]
    AbstinenceLogClean --> Projection
    AbstinenceLogRelapse --> Projection
    SleepLog --> Projection
    Projection --> DashboardState["DashboardState"]
```

### Estado vigente

- Sueño se registra desde el panel inferior.
- Sobriedad aparece en el dashboard si hay rachas activas y permite marcar limpio.
- `SobrietyConfigScreen` activa/desactiva presets opt-in y gestiona personalizadas.
- Recaidas se registran desde el panel inferior para rachas activas.

---

## 8. Salida de `DashboardState`

```mermaid
flowchart TD
    Projection["buildDashboardState"] --> Status["status / score y texto"]
    Projection --> DailyProgress["dailyProgress"]
    Projection --> AnchorPhrase["anchorPhrase"]
    Projection --> Layers["layers"]
    Projection --> Signals["signals"]
    Projection --> SobrietyTracks["sobrietyTracks"]
    Projection --> SobrietyOptions["sobrietyOptions"]
    Projection --> AnchorItems["anchorItems"]
    Projection --> SupportItems["supportItems"]
    Projection --> PendingTasks["pendingTasks"]
    Projection --> CompletedTasks["completedTasks"]
    Projection --> WeekRows["weekRows"]
    Projection --> Dimensions["dimensions"]
    Projection --> Sleep["sleep"]
    Projection --> ActivityOptions["activityOptions"]

    AnchorItems --> AnchorPreview["AnchorPreviewSection"]
    SupportItems --> SupportsPreview["SupportsPreviewSection"]
    SobrietyTracks --> SobrietyPreview["SobrietySection"]
    SobrietyOptions --> SobrietyConfigScreen["SobrietyConfigScreen"]
    PendingTasks --> TasksPreview["TasksPreviewSection"]
    PendingTasks --> TasksScreen["TasksScreen"]
    CompletedTasks --> TasksScreen
    WeekRows --> WeekSection["WeekSection"]
    ActivityOptions --> ConfigScreens["Mis anclas / Soportes / paneles"]
```

### Orden visual actual del dashboard

1. TopBar
2. StatusCard
3. DailyProgressCard
4. AnchorPhraseCard
5. ActionButtons
6. LayersSection
7. SignalsSection
8. SobrietySection
9. AnchorPreviewSection
10. SupportsPreviewSection
11. TasksPreviewSection
12. WeekSection

---

## 9. Puntos de entrada por archivo

| Area | Archivo principal | Rol |
|------|-------------------|-----|
| Navegacion de pantallas | `app/src/main/java/dev/panopt/autonomia/MainActivity.kt` | Cambia entre Dashboard, Mis anclas, Soportes, Pendientes y Sobriedad |
| Estado de UI | `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardViewModel.kt` | Combina flujos, recibe acciones y escribe en repositorio |
| Persistencia | `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt` | Fachada sobre Room, seeds y preferencias |
| Dominio dashboard | `app/src/main/java/dev/panopt/autonomia/domain/dashboard/DashboardProjection.kt` | Construye `DashboardState` |
| Modelo de estado | `app/src/main/java/dev/panopt/autonomia/domain/dashboard/DashboardState.kt` | DTOs consumidos por Compose |
| Anclas | `app/src/main/java/dev/panopt/autonomia/ui/anchors/AnchorConfigScreen.kt` | Configuracion completa de Mis anclas |
| Soportes | `app/src/main/java/dev/panopt/autonomia/ui/supports/SupportsConfigScreen.kt` | Configuracion de Soportes |
| Pendientes | `app/src/main/java/dev/panopt/autonomia/ui/tasks/TasksScreen.kt` | Pantalla completa de Pendientes |
| Sobriedad | `app/src/main/java/dev/panopt/autonomia/ui/sobriety/SobrietyConfigScreen.kt` | Pantalla completa de Sobriedad |
| Componentes dashboard | `app/src/main/java/dev/panopt/autonomia/ui/dashboard/components/` | Secciones visuales del dashboard |
