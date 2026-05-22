# Arquitectura recomendada - Vocal / Autonomia sin limites

Estado: decision arquitectonica inicial para guiar el backend local y el
dominio.

Fecha: 2026-05-22.

## Resumen ejecutivo

La arquitectura recomendada para Vocal es:

```text
MVVM Android
+ dominio modular explicito
+ repositorios locales
+ Room como fuente de hechos
+ flujo unidireccional de estado
+ platform adapters
+ privacidad local y portabilidad cifrada
```

Nombre corto dentro del proyecto:

```text
Arquitectura local-first con dominio modular.
```

No recomiendo implementar una Clean Architecture pesada ni una arquitectura
hexagonal completa ahora. Si se copian esas arquitecturas de forma literal, el
proyecto puede terminar con demasiadas interfaces, carpetas y casos de uso
vacíos antes de tener estabilidad real.

Tampoco recomiendo seguir con un MVVM simple donde el ViewModel o la UI armen
las reglas del producto. Vocal ya tiene suficiente dominio propio como para
merecer una capa de dominio clara: scoring, sueño, abstinencias, anclas,
señales, tono, recomendaciones y dashboard.

La decision practica:

```text
Usar una arquitectura Android recomendada por capas,
pero hacer que el dominio sea el centro operativo del producto.
```

## Fuentes y criterio

Esta recomendacion parte de:

- `AGENTS.md`;
- `docs/nucleo-dominio-autonomia.md`;
- `docs/vocal_mapa_componentes_v_0_2_borrador.md`;
- `docs/definicion-tablas-room-v1.md`;
- `docs/estado-actual-mvp.md`;
- codigo actual en `app/src/main/java/dev/panopt/autonomia`;
- Context7 sobre arquitectura Android moderna.

Context7 / Android Developers refuerza estos principios:

- arquitectura por capas;
- UI separada de datos;
- ViewModel como holder de estado de UI;
- repositorios como puerta de acceso a datos;
- `Flow` / `StateFlow` para datos observables;
- dominio opcional cuando hay logica compleja o reutilizable.

En Vocal, el dominio no es opcional en la practica. El sistema de scoring y
lectura de base ya es demasiado importante para vivir en Compose o en Room.

## Diagnostico del codigo actual

El proyecto ya apunta en la direccion correcta:

- Android local-first con Kotlin y Compose;
- Room como persistencia local;
- DAOs con `Flow`;
- `DashboardViewModel` exponiendo `StateFlow`;
- `ScoreEngine` puro dentro de `domain/scoring`;
- modelos como `Layer`, `ActivityLog`, `SleepLog`, `AbstinenceTrack`,
  `AbstinenceLog`, `Task` y `AnchorPhrase`;
- dashboard funcional conectado a datos reales.

Pero todavia hay mezcla de responsabilidades:

| Pieza actual | Problema | Direccion |
| --- | --- | --- |
| `Models.kt` | Todos los modelos viven juntos en raiz. | Separar modelos por dominio. |
| `TrackedActivity` | Nombre ambiguo: parece log, pero es definicion. | Migrar a `ActivityDefinition`. |
| `AutonomiaRepository` | Mezcla DB, mappers, seeds, prefs y escritura. | Partir en repositorios y mappers locales. |
| `DashboardInference.kt` | Calcula señales y lectura de dashboard dentro de `ui`. | Mover inferencia a dominio/dashboard. |
| `ScoreEngine` | Buen inicio, pero concentra sueño, goals, gates y abstinencia. | Mantener orquestador y extraer policies. |
| `saveSleepLog` | Guarda sin validar regla minima de 5h. | Crear `SleepPolicy`. |
| Seeds de abstinencias | Alcohol y conducta sexual nacen activos. | Corregir a opt-in. |
| `MainActivity` / Factory | Instancia repositorio directo. | Crear `AppGraph` manual antes de meter DI pesado. |

El problema no es que el codigo este mal. El problema es que si se sigue
creciendo asi, el dashboard se volvera el lugar donde todo se mezcla.

## Arquitecturas evaluadas

### 1. MVVM simple

Ventaja:

- es rapido;
- encaja con Compose y ViewModel;
- tiene poca ceremonia.

Riesgo:

- el ViewModel empieza a decidir reglas de negocio;
- las pantallas duplican inferencias;
- scoring y señales se vuelven dificiles de probar;
- cada feature nueva toca demasiados archivos.

Veredicto:

```text
Insuficiente para Vocal.
```

MVVM debe seguir existiendo, pero solo como arquitectura de presentacion.

### 2. Clean Architecture completa

Ventaja:

- fronteras muy claras;
- dominio testeable;
- buena separacion entre data, domain y UI.

Riesgo:

- demasiadas interfaces prematuras;
- use cases vacios para operaciones simples;
- carpetas mas grandes que el producto;
- lentitud para iterar en una app que aun esta encontrando su forma.

Veredicto:

```text
Buena como inspiracion, mala si se aplica de forma ceremonial.
```

Vocal necesita el espiritu de Clean Architecture, no su cosplay completo.

### 3. Arquitectura hexagonal

Ventaja:

- dominio muy protegido;
- puertos/adaptadores claros;
- util si luego hay servidor, sync, import/export o multiples fuentes.

Riesgo:

- demasiado abstracta para el estado actual;
- puede crear puertos que solo tienen una implementacion;
- complica features pequeñas.

Veredicto:

```text
No usar como arquitectura principal ahora.
```

Conviene tomar una idea hexagonal: el dominio no debe saber si los hechos
vienen de Room, una API, un archivo o telemetria futura.

### 4. Arquitectura local-first con dominio modular

Ventaja:

- se ajusta al producto real;
- mantiene velocidad;
- protege scoring y reglas;
- permite crecer sin reescribir todo;
- usa patrones Android actuales;
- no exige multi-modulo ni DI complejo desde el dia uno.

Riesgo:

- requiere disciplina de fronteras;
- si no se documenta, puede degenerar otra vez en repository gigante.

Veredicto:

```text
Recomendada.
```

## Decision recomendada

Vocal debe usar una arquitectura local-first con dominio modular:

```text
UI
  Compose + ViewModel + UiState

Application / Use cases ligeros
  Orquestan acciones de usuario y lecturas de pantalla.

Domain
  Modelos puros, policies, engines, scoring, dashboard, recomendaciones.

Data
  Repositorios, mappers, Room, DAOs, seeds, preferences.

Platform
  Android Context, database builder, tiempo del sistema, permisos futuros.
```

Regla principal:

```text
Room guarda hechos.
Repositorios exponen flujos y operaciones.
Dominio calcula inferencias.
ViewModel produce estado de pantalla.
Compose presenta estado y envia acciones.
```

## Diagrama conceptual

```text
Compose Screen
    |
    v
ViewModel
    |
    v
Use case / State producer
    |
    v
Domain engines and policies
    |
    v
Repository interfaces or concrete local repositories
    |
    v
Room DAO + Preferences + Seeds
```

El flujo de datos observable va hacia arriba:

```text
Room / Preferences
    -> Repository Flow
    -> Domain calculation
    -> ViewModel StateFlow
    -> Compose
```

Las acciones del usuario bajan:

```text
Compose event
    -> ViewModel function
    -> Use case or repository write
    -> Room fact
    -> Flow emits new state
```

## Estructura de paquetes recomendada

No crear modulos Gradle todavia. Un solo modulo `:app` es suficiente en esta
fase. La separacion debe empezar por paquetes.

Estructura objetivo:

```text
app/src/main/java/dev/panopt/autonomia/
  app/
    MainActivity.kt
    AppGraph.kt

  domain/
    common/
      DateProvider.kt
      DomainError.kt
      ValidationResult.kt

    layer/
      Layer.kt
      LayerId.kt

    activity/
      ActivityDefinition.kt
      ActivityLog.kt
      ActivityPolicy.kt
      ActivityProgress.kt

    sleep/
      SleepLog.kt
      SleepPolicy.kt
      SleepScoring.kt

    abstinence/
      AbstinenceTrack.kt
      AbstinenceLog.kt
      AbstinencePolicy.kt
      AbstinenceScoring.kt

    task/
      Task.kt
      TaskPolicy.kt

    identity/
      LocalProfile.kt
      RemoteIdentity.kt
      DataOwnershipPolicy.kt

    portability/
      ExportPackage.kt
      ExportManifest.kt
      ImportResult.kt
      PortabilityPolicy.kt

    phrase/
      AnchorPhrase.kt
      AnchorPhraseSelector.kt

    scoring/
      ScoreEngine.kt
      ScoreInput.kt
      ScoreReport.kt
      ScoreState.kt
      ScoreGate.kt

    dashboard/
      DashboardEngine.kt
      DashboardSnapshot.kt
      DashboardRecommendation.kt

  data/
    local/
      db/
        AutonomiaDatabase.kt
      dao/
        AutonomiaDao.kt
      entity/
        ActivityEntity.kt
        SleepLogEntity.kt
        ...
      mapper/
        ActivityMapper.kt
        SleepMapper.kt
        ...
      seed/
        LayerSeed.kt
        ActivitySeed.kt
        AbstinenceSeed.kt
        AnchorPhraseSeed.kt

    repository/
      ActivityRepository.kt
      AbstinenceRepository.kt
      SleepRepository.kt
      TaskRepository.kt
      PhraseRepository.kt
      DashboardRepository.kt
      UserPreferencesRepository.kt

    portability/
      ExportPackageWriter.kt
      ImportPackageReader.kt

  platform/
    identity/
      GoogleIdentityProvider.kt
      Auth0IdentityProvider.kt
      CredentialManagerAdapter.kt

    secure_storage/
      ExportEncryption.kt
      LocalKeyStore.kt

  ui/
    dashboard/
      DashboardViewModel.kt
      DashboardState.kt
      DashboardScreen.kt
      components/

    onboarding/
    activity/
    sleep/
    abstinence/
    settings/
```

## Fronteras obligatorias

### Dominio no depende de Android

Los archivos dentro de `domain/` no deben importar:

- `android.*`;
- `androidx.compose.*`;
- `androidx.room.*`;
- `Context`;
- entidades Room;
- componentes visuales.

El dominio debe poder probarse con JUnit normal.

### Room no calcula producto

Room puede:

- guardar hechos;
- consultar rangos de fechas;
- ordenar;
- filtrar por ids;
- exponer `Flow`.

Room no debe:

- calcular score;
- decidir estados;
- generar recomendaciones;
- decidir si una recaida limita el estado;
- decidir si el sueño permite Plenitud;
- traducir tono de comunicacion.

### Repositorios no deben convertirse en cerebro

El repositorio puede:

- leer y escribir hechos;
- mapear entidad local a modelo de dominio;
- exponer flujos;
- centralizar transacciones;
- inicializar seeds.

El repositorio no deberia:

- calcular score;
- armar todo el dashboard;
- contener mensajes al usuario;
- tener reglas de scoring;
- validar politicas complejas si pertenecen al dominio.

### ViewModel no inventa reglas

El ViewModel puede:

- combinar flujos;
- llamar use cases;
- exponer `StateFlow<UiState>`;
- recibir eventos de UI;
- manejar loading/error de pantalla.

El ViewModel no debe:

- calcular pesos;
- decidir gates;
- seleccionar frases por estado;
- duplicar reglas de sueño;
- decidir si una feature activa pesa o no.

### Compose no interpreta dominio

Compose puede:

- dibujar;
- llamar callbacks;
- mostrar estado;
- contener micro-logica visual.

Compose no debe:

- calcular score;
- filtrar hechos para negocio;
- decidir mensajes de dominio;
- conocer Room;
- conocer mappers.

## Bounded contexts del dominio

Para evitar codigo espagueti, el dominio debe dividirse por areas reales del
producto.

### Activity

Responsabilidad:

- definir actividades configuradas;
- separar anclas, cuidado base, goals y contextuales;
- calcular progreso de una actividad contra su log;
- validar configuracion de actividad.

Concepto clave:

```text
ActivityDefinition != ActivityLog
```

`ActivityDefinition` describe lo que el usuario decidio trackear.
`ActivityLog` describe lo que ocurrio un dia.

### Sleep

Responsabilidad:

- validar ventana objetivo minima de 5 horas;
- calcular cumplimiento de sueño;
- aportar señal fuerte al score;
- evitar castigar por elegir una ventana valida corta.

Pieza recomendada:

```text
SleepPolicy
SleepScoring
```

### Abstinence

Responsabilidad:

- tracks opt-in;
- logs diarios;
- recaida como señal, no castigo;
- impulso sin recaida como señal protectora;
- impacto fuerte en Conducta solo si el track esta activo.

### Scoring

Responsabilidad:

- tomar hechos y configuracion;
- calcular capas;
- calcular score visible;
- aplicar gates;
- producir `ScoreReport`.

Debe ser puro y testeable.

### Dashboard

Responsabilidad:

- traducir reportes y hechos a estado de dashboard;
- construir progreso diario;
- construir señales importantes;
- construir soportes;
- no dibujar UI.

Pieza recomendada:

```text
DashboardEngine
```

Este engine debe reemplazar gradualmente la logica de `DashboardInference.kt`.

### Recommendation

Responsabilidad:

- producir acciones minimas segun estado y señales;
- mantener tono de comunicacion;
- no generar motivacion generica.

Puede nacer pequeño y crecer despues.

### Identity

Responsabilidad:

- representar la identidad opcional del usuario;
- separar cuenta remota de datos personales;
- permitir login futuro con Google, Auth0 o Credential Manager;
- no bloquear el uso local de la app si no hay cuenta.

Concepto clave:

```text
RemoteIdentity != LocalProfile
```

`LocalProfile` es la configuracion y los datos del usuario en este dispositivo.
`RemoteIdentity` es una identidad externa opcional. No es duena de los logs,
del score ni del historial personal.

### Portability

Responsabilidad:

- exportar datos locales bajo control del usuario;
- importar datos en otro dispositivo;
- validar version, integridad y compatibilidad;
- cifrar el paquete por defecto;
- reemplazar sync cloud como mecanismo de portabilidad.

Concepto clave:

```text
Export/import es portabilidad, no sincronizacion automatica.
```

El usuario controla el archivo exportado y decide donde guardarlo. Si pierde el
archivo y no hay copia, la app no puede reconstruir datos sensibles desde un
servidor porque el servidor no los posee.

## Contratos de datos

### Hechos persistidos

Estos son hechos. Room los guarda.

- `ActivityLog`;
- `SleepLog`;
- `AbstinenceLog`;
- `RiskEvent`;
- `Task` cuando se crea o completa;
- `AnchorPhraseImpression`;
- configuracion local del usuario.

### Definiciones persistidas

Estas no son hechos diarios, pero viven en persistencia porque configuran la
base del usuario.

- `Layer`;
- `ActivityDefinition`;
- `AbstinenceTrack`;
- `AnchorPhrase`;
- reglas de frase;
- preferencias locales.

### Identidad y propiedad de datos

La autenticacion futura debe ser opcional.

Una cuenta remota puede representar:

- identidad;
- licencia;
- recuperacion futura no sensible;
- integraciones externas no sensibles.

Una cuenta remota no debe representar:

- almacenamiento del diario personal;
- sync cloud de logs sensibles;
- fuente de verdad del score;
- acceso remoto a sueno, recaidas, uso digital o abstinencias.

Tipos conceptuales:

- `LocalProfile`: configuracion y datos del usuario en el dispositivo.
- `RemoteIdentity`: identidad externa opcional, sin acceso a logs sensibles.
- `DataOwnershipPolicy`: regla que impide que el backend remoto sea dueno de
  datos personales.

### Portabilidad cifrada

Export/import es la forma prevista para mover datos entre dispositivos.

Tipos conceptuales:

- `ExportPackage`: paquete portable cifrado.
- `ExportManifest`: version, fecha, esquema, checksum y metadatos no sensibles.
- `ImportResult`: exito, incompatibilidad, error de contrasena, corrupcion o
  version no soportada.

Reglas:

- export cifrado por defecto;
- import valida version, integridad y compatibilidad de esquema;
- el usuario controla el archivo;
- export/import no afecta score ni dashboard si no se usa;
- un export legible solo deberia existir como modo explicito y advertido, no
  como camino normal.

### Inferencias no persistidas como verdad primaria

Estas se calculan.

- `ScoreReport`;
- `LayerScore`;
- `ScoreGate`;
- estado de base;
- señales importantes;
- recomendaciones;
- progreso diario;
- dashboard state.

Se pueden cachear en el futuro por rendimiento, pero no deben convertirse en
la fuente de verdad mientras el modelo siga cambiando.

## Interfaces y abstraccion

No crear interfaces por reflejo.

Crear una interfaz cuando se cumpla una de estas condiciones:

- el dominio necesita depender de un contrato y no de Room;
- hay mas de una implementacion real o probable;
- facilita una prueba importante;
- separa una frontera que sabemos que va a cambiar.

Ejemplo aceptable:

```kotlin
interface ActivityRepository {
    fun observeActivityDefinitions(): Flow<List<ActivityDefinition>>
    fun observeLogsForDate(date: LocalDate): Flow<List<ActivityLog>>
    suspend fun upsertLog(log: ActivityLog)
}
```

Ejemplo innecesario ahora:

```text
IInsertActivityLogUseCase
IGetActivityByIdUseCase
IObserveAllLayersUseCase
```

La abstraccion debe comprar claridad real, no decorar el codigo.

## Inyeccion de dependencias

No hace falta meter Hilt todavia.

Recomendacion actual:

```text
AppGraph manual.
```

`AppGraph` puede construir:

- database;
- DAOs;
- repositories;
- engines;
- policies;
- ViewModel factories.

Cuando el proyecto tenga varias pantallas con dependencias repetidas, se puede
evaluar Hilt. Antes de eso, Hilt puede ser mas ceremonia que ayuda.

## Scoring dentro de esta arquitectura

El scoring debe quedar como motor de dominio:

```text
ScoreInput -> ScoreEngine -> ScoreReport
```

Pero `ScoreEngine` no debe absorber todo.

Separacion recomendada:

```text
SleepScoring
ActivityScoring
AbstinenceScoring
GoalScoring
LayerScoring
ScoreGatePolicy
ScoreEngine
```

`ScoreEngine` debe orquestar. Las piezas pequeñas calculan.

Regla:

```text
El score no consulta Room.
El score no conoce Compose.
El score no escribe datos.
```

## Dashboard dentro de esta arquitectura

El dashboard actual se mantiene como contrato visual y funcional.

Arquitectonicamente debe quedar asi:

```text
DashboardViewModel
    recibe flujos de repositorios
    llama DashboardEngine
    expone DashboardState

DashboardEngine
    llama ScoreEngine
    construye señales, soportes, preview y lectura

DashboardScreen
    dibuja DashboardState
```

Esto permite que el dashboard siga igual para el usuario, pero con menos
logica de negocio dentro de `ui`.

## Configuracion inicial

La configuracion inicial debe ser una feature de UI/aplicacion, pero sus
reglas pertenecen al dominio.

Reglas de dominio:

- minimo 3 anclas;
- sueño con objetivo minimo de 5 horas;
- abstinencias opt-in;
- presets aceptables;
- foco configurable;
- metas opcionales.

Arquitectura recomendada:

```text
ui/onboarding
    OnboardingScreen
    OnboardingViewModel
    OnboardingState

domain/onboarding
    OnboardingPolicy
    InitialConfiguration

data/repository
    UserConfigurationRepository
```

## Plan de migracion recomendado

### Fase 1 - Nombrar bien el dominio

Objetivo: quitar ambiguedad antes de crecer.

Cambios:

- mover modelos raiz hacia `domain/`;
- renombrar `TrackedActivity` a `ActivityDefinition`;
- mantener `ActivityEntity` como nombre Room;
- mantener `ActivityLog` como hecho diario;
- actualizar docs y tests.

Resultado:

```text
Mis anclas son ActivityDefinition con displaySurface = PrimaryChecklist.
Los registros diarios son ActivityLog.
```

### Fase 2 - Extraer policies pequeñas

Objetivo: evitar que `ScoreEngine` y `DashboardInference` se vuelvan enormes.

Crear:

- `ActivityPolicy`;
- `SleepPolicy`;
- `SleepScoring`;
- `AbstinencePolicy`;
- `TaskPolicy`.

Primera regla obligatoria:

```text
SleepPolicy valida que la ventana objetivo sea >= 5 horas.
```

### Fase 3 - Separar data local

Objetivo: que `AutonomiaRepository` deje de ser una bolsa de todo.

Crear:

- entidades por archivo;
- mappers por area;
- seeds por area;
- repositorios por area o un repositorio local con API claramente agrupada.

No hace falta hacerlo todo en una sola iteracion.

### Fase 4 - Mover inferencia de dashboard al dominio

Objetivo: sacar reglas de producto de `ui/dashboard`.

Crear:

```text
domain/dashboard/DashboardEngine.kt
```

Mover gradualmente:

- construcción de señales;
- soportes;
- dimensiones;
- seleccion de frase;
- lectura de progreso.

La UI conserva `DashboardState`, pero deja de decidir negocio.

### Fase 5 - Corregir configuracion y seeds

Objetivo: alinear codigo con canon.

Cambios:

- abstinencias inactivas por defecto;
- presets de anclas aceptables;
- sueño con default valido;
- foco configurable sin depender de proyectos hardcodeados;
- configuracion local unica implicita.

### Fase 6 - Tests de dominio

Objetivo: que el nucleo no dependa de probarlo visualmente.

Tests minimos:

- `SleepPolicy` rechaza menos de 5h;
- `SleepScoring` evalua cumplimiento del objetivo personal;
- abstinencia no activa no pesa;
- recaida activa limita estado sin marcar task fallida;
- task neutral no suma;
- `NoData` produce score label `--`;
- goals ayudan a estados altos, no reemplazan base diaria;
- `ActivityDefinition` y `ActivityLog` no se mezclan.

### Fase 7 - Identidad y portabilidad futura

Objetivo: preparar el proyecto para lanzamiento sin entregar datos sensibles a
un servidor.

Cambios:

- crear contratos de identidad opcional;
- crear politica de propiedad de datos;
- disenar export/import cifrado;
- validar manifests de export;
- documentar errores de import;
- aislar Google, Auth0 y Credential Manager en `platform/identity`.

Esta fase no debe introducir sync cloud de datos sensibles.

## Reglas de complejidad

Para este proyecto, la arquitectura debe obedecer estas reglas:

1. Si una regla afecta scoring, estado, señales o tono, va al dominio.
2. Si una clase solo dibuja, va a UI.
3. Si una clase guarda o recupera hechos, va a data.
4. Si una operacion solo coordina una pantalla, puede vivir en ViewModel.
5. Si una operacion se comparte o contiene negocio, se mueve a dominio.
6. No crear interfaz si no hay frontera real.
7. No crear modulo Gradle si un paquete basta.
8. No persistir inferencias como verdad primaria.
9. No hacer que una feature opt-in pese si el usuario no la activo.
10. No introducir servidor remoto como condicion para ordenar el core local.
11. No hacer que login sea requisito para calcular scoring o usar dashboard.
12. No enviar datos sensibles a servicios remotos.

## Decision sobre backend remoto

La arquitectura actual define el backend local de la app: Room, repositorios,
dominio, engines, policies y ViewModels.

Si mas adelante existe servidor remoto o cuentas, no deberia redefinir el
dominio ni convertirse en fuente de verdad de los datos personales. La cuenta
remota sera opcional y estara separada del perfil local.

Contratos:

```text
LocalProfile
RemoteIdentity
DataOwnershipPolicy
ExportPackage
ExportManifest
ImportResult
```

Auth puede servir para identidad, licencia, recuperacion futura o integraciones
no sensibles. No puede ser requisito para leer el dashboard, calcular score,
registrar sueno, registrar abstinencias o conservar logs personales.

Esto evita que el core dependa de una API futura que todavia no existe y evita
que datos sensibles terminen viviendo en un servidor privado.

## Senales de que la arquitectura esta funcionando

La arquitectura va bien si:

- se puede probar `ScoreEngine` sin Android;
- se puede cambiar Room sin tocar Compose;
- se puede cambiar el diseño del dashboard sin tocar scoring;
- se puede agregar una nueva abstinencia sin tocar sueño;
- se puede cambiar la formula de sueño sin tocar pantallas;
- se puede explicar donde vive cada regla en menos de 30 segundos;
- los nombres del codigo coinciden con el dominio.
- se puede usar la app sin iniciar sesion;
- se puede iniciar sesion sin subir logs sensibles;
- se puede migrar de dispositivo con export/import cifrado.

La arquitectura va mal si:

- `DashboardViewModel` empieza a tener cientos de lineas de reglas;
- `AutonomiaRepository` sigue creciendo como clase central;
- Compose filtra logs para decidir estados;
- las entidades Room se usan como modelos de UI;
- cada feature nueva toca scoring sin contrato claro;
- aparecen interfaces vacias solo por parecer arquitectura.

## Recomendacion final

No hace falta planear una arquitectura enorme.

La recomendacion final es:

```text
MVVM para presentacion.
Dominio modular para reglas.
Repositorios locales para datos.
Room para hechos.
Platform adapters para capacidades Android.
AppGraph manual para dependencias.
Un solo modulo Gradle por ahora.
```

Esta arquitectura es suficiente para producir features importantes sin codigo
espagueti, y sigue dejando camino abierto para modularizar mas adelante si el
producto crece.

La recomendacion de identidad y datos es:

```text
Auth opcional.
Datos sensibles locales.
Export/import cifrado por defecto.
Servidor remoto nunca como fuente de verdad personal.
```
