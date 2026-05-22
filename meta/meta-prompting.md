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

### Identidad, Privacidad y Portabilidad (22/05/2026)

* **Auth opcional**: Vocal podra usar Google, Auth0, Credential Manager u otro proveedor en el futuro, pero el login no debe ser obligatorio para usar la app local.
* **Cuenta no equivale a nube**: `RemoteIdentity` representa identidad externa opcional. No reemplaza `LocalProfile` ni toma propiedad de logs, score, sueno, recaidas, abstinencias o uso digital.
* **Datos sensibles locales**: Los registros personales viven en el dispositivo. Un servidor remoto no debe ser fuente de verdad de datos sensibles.
* **Export/import cifrado**: La portabilidad entre dispositivos se resuelve con `ExportPackage` cifrado por defecto, manifest de version/integridad y validacion en import.
* **Servidor remoto limitado**: Auth puede servir para identidad, licencia, recuperacion futura no sensible o integraciones no sensibles; no para almacenar diario personal ni calcular scoring.
