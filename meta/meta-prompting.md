# Meta-Prompting

Este documento sirve como registro y bitacora de diseno para las instrucciones complejas o ambiguas.

## Registro de Terminos y Decisiones

Aqui se documentan conceptos tecnicos, terminos de dominio y dudas de arquitectura que surgen durante la traduccion de peticiones de usuario a soluciones tecnicas.

### Conceptos del Dominio (APK-Personal / Vocal)

* **Dashboard Components (UI)**:
  * `LayerPill`: Representa "Capas de hoy". Requiere centrado vertical y barras de progreso mas grandes.
  * `SignalCard`: Representa "Senales". Necesita reestructuracion visual para legibilidad tipografica.
  * `StreakCard`: Representa "Sobriedad". Necesita reestructuracion visual para legibilidad tipografica.
  * `SupportCard`: Representa "Soportes". La tipografia actual es demasiado pequena.
  * `FlagIcon` / `ChecklistIcon`: Presentan problemas de centrado interno de sus SVG/Paths en sus contenedores respectivos.

### Backend / Data y Domain (21/05/2026)

* **Hechos vs Inferencias**: Room almacena hechos de forma local, mientras que el dominio calcula el estado general. La UI (Compose) nunca debe contener logica de negocio.
* **Modelo de Capas (Layers)**: Las capas principales son 5: `Interior`, `Cuerpo`, `Conducta`, `Vinculos`, `Proyecto`.
* **Rachas y Abstinencias (Sobriedad)**: No son un checklist comun, son un modelo propio (`AbstinenceTrack`, `AbstinenceLog`) y operan como feature separada debido a su peso en el algoritmo.
* **Context 7 MCP**: Servidor configurado para proveer mejores practicas de arquitectura y codigo durante el desarrollo.

### Sistema de Puntuacion y Features (21/05/2026)

* **Score por capas**: El score global se calcula desde las 5 capas canonicas del core: `Interior`, `Cuerpo`, `Conducta`, `Vinculos`, `Proyecto`. Las features no son capas nuevas.
* **Features como fuentes de hechos**: `SleepLog`, `AbstinenceLog`, `ActivityLog` y `Task` alimentan capas. Room guarda esos hechos; el dominio calcula inferencias.
* **Exterior vs Vinculos**: `Exterior` fue un error de tipeo del usuario. El nombre canonico sigue siendo `Vinculos`.
* **Estados altos**: `Plenitude` e `Unbreakable` no deben salir solo de completar la base diaria. Requieren base estable mas goals semanales o mensuales sostenidos.
* **Goals semanales/mensuales**: En la version actual se modelan como `Activity` con `cadence` `Weekly`/`Monthly` y `targetPeriod` `Week`/`Month`. No se crea tabla nueva de goals todavia.
* **Jerarquia anti-abuso**: Checklist principal pesa mas que checklist secundaria. `Task` pesa menos todavia y solo suma si tiene `layerId` y `contributionRole != Neutral`.
* **Sueno**: Feature propia y base obligatoria suave. Aporta principalmente a `Cuerpo` usando duracion, cumplimiento del horario planificado y calidad subjetiva.
* **Sobriedad**: Feature propia. Alimenta `Conducta`; una recaida pesa fuerte hoy y los estados altos requieren racha limpia sostenida.
* **Boton rojo**: No es riesgo generico. Debe abrir registro/desmarcado de recaidas de abstinencias activas definidas por el usuario.

### Nucleo de Dominio y Backend Local (22/05/2026)

* **Mis anclas**: Son `Activity` configuradas por el usuario como base principal (`displaySurface = PrimaryChecklist`). No son hechos diarios. Los hechos diarios viven en `ActivityLog`.
* **TrackedActivity como nombre transitorio**: En el codigo actual, `TrackedActivity` representa una actividad configurada y registrable, no una actividad ya registrada. El nombre confunde y debe migrar a `Activity`, `ActivityDefinition` o `ConfiguredActivity` cuando se reorganice el nucleo.
* **Sueno minimo configurable**: La ventana objetivo de sueno no puede ser menor a 5 horas. El usuario puede configurar mas. El score de sueno debe evaluar cumplimiento del objetivo personal, calidad y consistencia, no castigar por elegir una ventana valida de 5h o mas.
* **Dashboard actual como contrato vivo**: El dashboard ya esta bastante definido en Compose/prototipo. Debe mantener tarjeta de estado/score siempre visible (`NoData` usa `--`/`sin score`), progreso diario, frase ancla, botones de accion, capas, senales importantes, abstinencias activas, preview de Mis anclas, soportes y resumen semanal.
* **Senales importantes del dashboard**: Se componen de `Sueno`, `Proyecto` y un `Foco` configurable por el usuario. Sueno vive dentro de senales importantes y abre su panel de registro desde ahi.
* **Backend local dentro del trabajo actual**: Backend significa Room, DAO, repositorios, seeds, modelos de dominio, motor de scoring, inferencias, flujos y ViewModels. Fuera de la etapa inmediata quedan servidor remoto como fuente de datos personales, auth obligatoria, cuentas obligatorias, nube de logs sensibles, multiusuario, sync cloud y telemetria automatica sensible.
* **Direccion de migracion del nucleo**: Separar modelos de dominio puros, entidades Room, repositorios por area o API local clara, casos de uso/motores de dominio y mappers. Mantener flujo unidireccional: Room/repository -> domain/use case -> ViewModel state -> Compose.

### Arquitectura Recomendada (22/05/2026)

* **Decision arquitectonica**: Usar arquitectura local-first con dominio modular: MVVM Android para presentacion, dominio modular para reglas, repositorios locales para datos, Room como fuente de hechos y flujo unidireccional de estado.
* **No usar Clean Architecture pesada**: Se toman sus principios utiles, pero no se debe crear una red de interfaces/use cases vacios. La arquitectura debe sostener el producto, no sustituirlo.
* **No usar hexagonal completa ahora**: La idea util es proteger el dominio de Room/Compose/API futura, pero no conviene llenar el proyecto de puertos prematuros.
* **Un solo modulo Gradle por ahora**: La separacion empieza por paquetes (`domain`, `data`, `ui`, `app`), no por multi-modulo.
* **AppGraph manual**: Preferir un `AppGraph` manual antes de introducir Hilt. Evaluar DI pesado solo cuando haya varias pantallas y dependencias repetidas.
* **DashboardEngine futuro**: La inferencia actual de dashboard debe migrar desde `ui/dashboard/DashboardInference.kt` hacia `domain/dashboard/DashboardEngine.kt`.

### Pantalla de Configuracion de Checklist / Mis Anclas (22/05/2026)

* **Mis anclas**: `Activity` configuradas con `displaySurface = PrimaryChecklist`. Son las anclas que el usuario elige para su base diaria. Viven en la tabla `activities`.
* **Filtro por capa**: Las 5 capas canonicas (Interior, Cuerpo, Conducta, Vinculos, Proyecto) sirven como filtros en la UI de configuracion. Cada actividad tiene `layerId` que la vincula a una capa.
* **Tipos de actividad con tiempo**: `ActivityType.Time` tiene `targetValue` (minutos objetivo). No todas las actividades tienen tiempo; `ActivityType.Check` y `ActivityType.SelfCare` son booleanas.
* **Goals (metas semanales/mensuales)**: Se modelan como `Activity` con `cadence` `Weekly`/`Monthly` y `targetPeriod` `Week`/`Month`. Campo `targetCount` almacena frecuencia objetivo (ej. 3 veces por semana).
* **Arquitectura actual**: Una sola `Activity` → `MainActivity` que aloja `DashboardScreen`. No hay navegacion con NavHost. Los paneles secundarios usan bottom sheets (`DashboardSheetHost` con enum `DashboardSheet`).
* **Drawer actual**: El drawer ya tiene un link "Checklist" que dispara `onOpenChecklist` y abre `DashboardSheet.Checklist`. La nueva pantalla de configuracion debe conectarse desde el drawer o desde un sheet.
* **DashboardPalette**: Centraliza colores para dark/light mode. Se usa `mix()` para blends. Los colores de capas: `layerInterior`, `layerBody`, `layerConduct`, `layerVinculos`, `layerProject`.
* **ActivityEntity vs Activity (dominio)**: `ActivityEntity` vive en Room con campos string para enums. `ActivityDefinition` vive en `domain/activity/`. Los mappers estan en `data/local/mapper/DomainMappers.kt`.
* **Seed actual de actividades**: 8 actividades base (Meditar, Ejercicio, Digitaliza, Musica, Dientes, Banarse, Cocinar, Trastes). 4 PrimaryChecklist, 4 SecondaryChecklist.
* **Bottom sheet patron**: `DashboardSheetHost` usa `Column` con `heightIn(max=680.dp)`, clip top corners `26.dp`, handle de 48x4dp, backdrop negro 48% opacidad.
* **Navegacion pantallas**: `MainActivity` usa enum `AppScreen` (Dashboard, ChecklistConfig) para navegar entre pantallas completas. El drawer abre `ChecklistConfigScreen` como pagina completa, no como sheet.
* **ChecklistPanel (sheet)**: El flujo Dashboard → "Registra checklist" → Checklist principal muestra SOLO actividades que ya estan en `PrimaryChecklist`. El boton "Configurar actividades" abre `DashboardSheet.Activities` (viejo). Debe redirigir a la pagina nueva.
* **ChecklistConfigPanel (sheet rapido)**: Bottom sheet accesible desde el dashboard para configurar anclas sin salir. Coexiste con la pagina completa.
* **ChecklistConfigScreen (pagina completa)**: Pagina propia accesible desde drawer. Tiene top bar, back button, busqueda, filtros, y seccion de creacion custom.
* **Creacion de actividad personalizada**: El `ActivitySettingsPanel` tiene formulario para crear actividades custom (nombre, capa, minutos, secundaria, goal, mensual). Usa `onCreateActivity(name, layerId, minutes, isSecondary, isGoal, isMonthlyGoal)`. Esta funcionalidad debe replicarse en `ChecklistConfigScreen`.
* **Seed IDs**: Los IDs de actividades predeterminadas empiezan con `act_` (ej. `act_meditar`, `act_ejercicio`). Los custom usan UUID. Esto permite distinguir cuales son borrables.
* **Zona de pulgar (UX mobile)**: Los controles de accion (guardar, confirmar) deben estar en la zona inferior de la pantalla para accesibilidad con una mano.
* **Sheet sin gesto de cierre**: El `DashboardSheetHost` actual usa `Box` + `Column` custom, no Material3 `ModalBottomSheet`. No tiene swipe-down nativo.

### Identidad, Privacidad y Portabilidad (22/05/2026)

* **Auth opcional**: Vocal podra usar Google, Auth0, Credential Manager u otro proveedor en el futuro, pero el login no debe ser obligatorio para usar la app local.
* **Cuenta no equivale a nube**: `RemoteIdentity` representa identidad externa opcional. No reemplaza `LocalProfile` ni toma propiedad de logs, score, sueno, recaidas, abstinencias o uso digital.
* **Datos sensibles locales**: Los registros personales viven en el dispositivo. Un servidor remoto no debe ser fuente de verdad de datos sensibles.
* **Export/import cifrado**: La portabilidad entre dispositivos se resuelve con `ExportPackage` cifrado por defecto, manifest de version/integridad y validacion en import.
* **Servidor remoto limitado**: Auth puede servir para identidad, licencia, recuperacion futura no sensible o integraciones no sensibles; no para almacenar diario personal ni calcular scoring.

### Sistema de Pendientes con Selección de Capas (22/05/2026)

* **Asociación de Capas en Tareas**: Las tareas (`Task`) pueden estar asociadas a una de las 5 capas principales de la aplicación: `Interior`, `Cuerpo`, `Conducta`, `Vínculos` o `Proyecto`. Si no está asociada a ninguna, se considera de contribución Neutral.
* **Interfaz de Creación de Pendientes**: Consiste en un campo de texto para el título del pendiente, y una fila horizontal debajo que muestra los 5 símbolos de las capas. El usuario puede tocar cualquiera de estos símbolos para asociar el pendiente a esa capa. Al tocarlo se destaca visualmente con el color de su respectiva capa. Si lo vuelve a tocar, se deselecciona. Al confirmar la creación del pendiente con el botón "Agregar pendiente", este se guarda en la base de datos con la capa seleccionada.
* **Visualización de la Lista de Pendientes**: Cada pendiente activo en la lista se renderiza en una tarjeta plana. Si tiene una capa asignada, se muestra su símbolo característico al lado izquierdo del título de la tarea, pintado con el color correspondiente de la capa. Al lado derecho, se incluye un check circular sutil para completar la tarea de forma directa ("check, check, check").
* **Flujo de Acciones de Pendientes**: El ViewModel expone `tasksFlow()` que lee desde Room. `createTask` recibe el título de la tarea, el `layerId` (nullable) y un booleano `contributesToCore` (que es true si el `layerId` no es nulo). `completeTask` marca la tarea como `Done` en Room.

### Diagnostico de Bugs — Dashboard y Anclas (22/05/2026)

* **Bug 1 — Actividad personalizada con goal no visible**: En `AutonomiaRepository.createActivity()`, cuando `isGoal=true`, el `displaySurface` se sobrescribe a `Contextual` ignorando el `displaySurface` que eligio el usuario (PrimaryChecklist). Esto hace que la actividad se cree pero no aparezca en "Mis anclas". Causa: linea `displaySurface = if (isGoal) DisplaySurface.Contextual.name else displaySurface.name` en `AutonomiaRepository.kt:172`.
* **Bug 2 — Datos seed contaminan testing**: `DefaultSeeds.kt` siembra 30+ actividades y 3 abstinence tracks en cada inicio que la BD este vacia. El usuario quiere BD limpia para testear.
* **Bug 3 — Inconsistencia dashboard vs configuracion**: `buildDashboardState` excluye goals con `filterNot { it.isGoal() }`, pero `activityOptions` los incluye. Ademas, el panel de configuracion (`ChecklistConfigPanel`) no filtra goals de `currentAnchors`. Combinado con Bug 1, genera la ilusion de que las actividades "desaparecen".
* **Bug 4 — ActionButtons con 6 botones en grid**: Debe ser UN solo boton blanco ("Registrar checklist" o "Configuracion rapida") + boton rojo al costado. El boton blanco debe abrir `DashboardSheet.EntryMenu` que ya existe.
* **Bug 5 — Sueno en panel rapido**: El boton de sueno esta en `ActionButtons` y `EntryMenuPanel`. Debe removerse de ambos. Sueno se accede desde la senal de sueno en `SignalsSection` (ya funciona).
* **Funcion `isGoal()`**: Definida en `ActivityPolicy.kt:16-20`. Retorna true si `cadence` es Weekly/Monthly o `targetPeriod` es Week/Month. Se usa en `buildDashboardState` para excluir goals del pipeline diario.
* **EntryMenuPanel**: Ya existe en `DashboardSheet.kt:244-264` con opciones: Mis anclas, Cuidado base, Pendientes, Actividades/proyectos/goals, Recaidas. No se usa actualmente porque `ActionButtons` dispara sheets individuales en vez de abrir EntryMenu.

### UX de Configuracion de Mis Anclas (23/05/2026)

* **Eliminar vs Quitar**: `Quitar` en Mis anclas debe borrar solo `UserActivityConfigEntity`; `Eliminar` en actividades custom debe borrar la `ActivityDefinitionEntity` custom completa. Si `Eliminar` solo borra config, la actividad custom queda en el catalogo y la UI parece bugueada.
* **Custom sin categoria**: Si una actividad custom queda sin config, `ActivityDefinitionEntity.presetCategory = null` no permite saber su superficie original. Para recuperar custom anchors antiguas, el mapper trata IDs custom sin categoria como `ActivitySurface.Anchor`; para nuevas custom, `DashboardViewModel.createActivity()` guarda `presetCategory = "anchor"` o `"support"`.
* **Feedback al agregar/crear ancla**: Despues de guardar una ancla, la seccion `Anclas actuales` debe expandirse y la tarjeta recien creada debe destellar sutilmente para confirmar la accion.
* **Presets de metas (historico)**: La idea anterior de metas semanales/mensuales fue reemplazada para anclas por `weeklyFrequencyTarget` obligatorio de 2 a 7 veces/semana. No usar presets mensuales en Mis anclas.

### Configuración de Anclas: Frecuencia Semanal, Duración de Compromiso y Límite de Tiempo (23/05/2026)

* **Meta Semanal y Cadencia**: Las anclas son obligatoriamente semanales. Se elimina la frecuencia mensual o de 1 vez por semana. Los botones rápidos para metas semanales van de 2 a 7 veces/semana. Esto se guarda en `targetCount` en `user_activity_configs` y `targetPeriod` se fija a `"Week"`.
* **Duración del Compromiso (`commitmentDurationMonths`)**: Nueva columna nullable (`Int?`) en `user_activity_configs` que indica cuántos meses se sostendrá el compromiso. Si es null, representa "indefinido" (opción preseleccionada por defecto).
* **Tiempo Objetivo por Sesión (`sessionTargetMinutes`)**: Máximo permitido de 15 horas por día (900 minutos) para evitar desbalances en otras áreas/capas (sueño, cuerpo, vínculos, etc.). Se guarda en `targetValue` en `user_activity_configs`.
* **Flujo UX de Duración del Compromiso**: Al seleccionar la meta semanal no se abre ningún modal. La recomendación de dejar la duración como `Indefinido` vive dentro del diálogo de meses, y ese diálogo solo se abre cuando el usuario presiona el botón de configuración.
* **Canon UX de Mis anclas (25/05/2026)**: Fuente vigente en `docs/mis-anclas-ux-canon-v1.md`. Orden del editor: identidad/nombre, tiempo objetivo, meta semanal, duración del compromiso, acciones. Configuración rápida > Anclas ajusta anclas configuradas, no administra catálogo.
