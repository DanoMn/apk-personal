# Pro-Prompt: Actualizacion del nucleo backend local de Vocal

Fecha: 2026-05-22

## Objetivo

Reorganizar el nucleo interno de Vocal para que el backend local, el dominio y
el scoring queden preparados para desarrollar el core y las features
importantes sin seguir acumulando ambiguedad.

Este trabajo no implementa backend remoto, auth, nube ni multiusuario. El
backend de esta iteracion es local-first: Room, repositorios, modelos de
dominio, policies, motores de inferencia, scoring, seeds, flujos y ViewModels.

## Contexto humano

El usuario quiere dejar pulcro el nucleo antes de producir features. La app aun
esta en fase de desarrollo y se puede cambiar estructura, nombres, tablas y
modelo si eso deja una base mas sana.

Decisiones recientes que deben respetarse:

- `Mis anclas` no son logs. Son activities elegidas por el usuario como base
  principal.
- `TrackedActivity` es un nombre confuso del codigo actual. Representa una
  actividad configurada, no una actividad ya trackeada.
- Sueno es core. La ventana objetivo configurable no puede ser menor a 5 horas.
- El score de sueno evalua cumplimiento del objetivo personal, calidad y
  consistencia; no castiga por elegir una ventana valida de 5h o mas.
- El dashboard actual de Compose/prototipo es el contrato vivo. No redisenar el
  dashboard desde el nucleo.
- El dashboard siempre muestra la tarjeta de estado/score; en `NoData` muestra
  `--` / `sin score`.
- Las senales importantes son `Sueno`, `Proyecto` y un `Foco` configurable.
- Abstinencias son opt-in desde producto. El seed actual que activa Alcohol y
  Conducta sexual debe alinearse.

## Fuentes obligatorias

- `AGENTS.md`
- `docs/nucleo-dominio-autonomia.md`
- `docs/decisiones-capas-actividades-v1.md`
- `docs/prototipo/dashboard.html`
- `docs/prototipo/score-states.html`
- `docs/tono-comunicacion.md`
- `app/src/main/java/dev/panopt/autonomia/Models.kt`
- `app/src/main/java/dev/panopt/autonomia/data/Entities.kt`
- `app/src/main/java/dev/panopt/autonomia/data/AutonomiaDao.kt`
- `app/src/main/java/dev/panopt/autonomia/data/AutonomiaDatabase.kt`
- `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt`
- `app/src/main/java/dev/panopt/autonomia/domain/scoring/ScoreEngine.kt`
- `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardInference.kt`
- `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardState.kt`
- `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardViewModel.kt`

## Context7 aplicado

Context7 / Android Developers respalda mantener:

- separacion de responsabilidades por capas;
- repositorios como API de datos;
- Room como persistencia local;
- dominio aislable y testeable;
- ViewModel como state holder;
- UI observando `StateFlow`;
- flujo unidireccional: datos -> dominio -> estado de pantalla -> Compose;
- piezas testeables en aislamiento.

## Diagnostico del codigo actual

Estado encontrado:

- `Models.kt` concentra demasiados modelos y enums de dominios distintos.
- `TrackedActivity` representa definicion/configuracion de actividad, pero su
  nombre sugiere un hecho ya trackeado.
- `ActivityLog` esta bien separado como hecho diario.
- `ActivityEntity` y tabla `activities` pueden mantenerse.
- `ScoreEngine` ya existe y produce `ScoreReport`, `LayerScore`,
  `FeatureContribution` y `ScoreGate`.
- `DashboardInference.kt` vive en UI pero contiene bastante logica de dominio:
  score input, capas, senales, dimensiones, rachas y soporte.
- `AutonomiaRepository` mezcla acceso local, mappers, writes, seeds y
  preferencias simples.
- `SleepLog` existe, pero la regla de minimo configurable de 5 horas no esta
  expresada como policy reusable.
- `ScoreEngine.scoreSleep` actualmente acepta ventana planificada entre 300 y
  600 minutos; debe migrar a minimo de 300 sin imponer ese maximo como regla de
  producto.
- El dashboard ya tiene piezas correctas: `StatusCard`, `DailyProgressCard`,
  `AnchorPhraseCard`, `ActionButtons`, `LayersSection`, `SignalsSection`,
  `SobrietySection`, `ChecklistPreviewSection`, `SupportsSection` y
  `WeekSection`.
- `StatusCard` ya muestra `--` / `sin score` para `NoData`.
- `buildSignals` ya compone `Sleep`, `Project` y `Focus`.
- El seed actual activa Alcohol y Conducta sexual; debe cambiar a opt-in.

## Direccion de arquitectura

Migrar hacia arquitectura local-first con dominio modular:

```text
domain/
  activity/
  abstinence/
  sleep/
  scoring/
  dashboard/
  recommendation/
  common/

data/
  local/
    entity/
    dao/
    database/
    mapper/
    seed/
  repository/
  preferences/

ui/
  dashboard/
  onboarding/
  activity/
  sleep/
  abstinence/
```

Reglas:

- Room guarda hechos y definiciones persistidas.
- El dominio define lenguaje, policies y calculos.
- Repositorios exponen flujos y operaciones locales.
- ViewModels componen estado de pantalla desde casos de uso o motores.
- Compose no calcula score, rachas, gates ni reglas de sueno.

## Migracion propuesta

### 1. Renombrar y separar Activity

Decidir nombre de codigo:

```text
ActivityDefinition
```

Motivo:

- comunica que es una definicion/configuracion registrable;
- evita confundirla con `ActivityLog`;
- evita colision mental con `android.app.Activity`;
- mantiene claro que `Mis anclas` son definitions con
  `displaySurface = PrimaryChecklist`.

Cambios:

- Crear `domain/activity/ActivityDefinition.kt`.
- Migrar `TrackedActivity` a `ActivityDefinition`.
- Mantener `ActivityEntity` y tabla `activities`.
- Mantener `ActivityLog` como hecho diario.
- Cambiar `activitiesFlow(): Flow<List<TrackedActivity>>` a
  `activityDefinitionsFlow(): Flow<List<ActivityDefinition>>`.
- Actualizar `ScoreInput`, `ScoreEngine`, `DashboardInference` y
  `DashboardViewModel`.
- Donde la UI diga checklist principal, el dominio debe entender `Mis anclas`.

### 2. Introducir policies de actividad

Crear helpers puros:

- `ActivityDefinition.isAnchor()`
- `ActivityDefinition.isSupport()`
- `ActivityDefinition.isGoal()`
- `ActivityDefinition.defaultActualValue()`
- `ActivityDefinition.progressFor(log)`

Eliminar duplicacion entre `ScoreEngine`, `DashboardInference` y repository.

### 3. Separar SleepPolicy y SleepScoring

Crear:

```text
domain/sleep/SleepPolicy.kt
domain/sleep/SleepScoring.kt
```

Reglas:

- `MIN_SLEEP_WINDOW_MINUTES = 300`.
- Guardar sueno solo si `plannedWakeAt - plannedSleepAt >= 300`.
- No imponer maximo de producto para ventana configurada.
- El score evalua:
  - duracion real contra objetivo personal;
  - cercania al horario planificado;
  - calidad subjetiva;
  - consistencia futura cuando haya historial.

Actualizar `saveSleepLog` para validar antes de persistir o devolver error de
dominio consumible por UI.

Actualizar `ScoreEngine` para usar `SleepScoring`.

### 4. Mover inferencia de dashboard fuera de UI

Crear una pieza de dominio o aplicacion:

```text
domain/dashboard/DashboardEngine.kt
```

Responsabilidad:

- convertir hechos y definiciones en un reporte de dashboard no-Compose;
- mantener senales: `Sleep`, `Project`, `Focus`;
- mantener progreso diario;
- mantener capas;
- mantener rachas activas;
- mantener supports;
- respetar que `NoData` conserva tarjeta de score con `--`.

La UI puede conservar `DashboardState` como modelo de pantalla, pero la logica
de lectura debe salir de `ui/dashboard/DashboardInference.kt`.

### 5. Reorganizar repositorio local

El `AutonomiaRepository` actual puede partirse o encapsularse.

Opcion recomendada:

- `LocalActivityRepository`
- `LocalSleepRepository`
- `LocalAbstinenceRepository`
- `LocalTaskRepository`
- `LocalPhraseRepository`
- `LocalSettingsRepository`
- `DashboardRepository` o `DashboardUseCase` que combina flujos para la home.

Si se prefiere menos archivos, mantener un facade:

```text
AutonomiaRepository
```

pero mover:

- mappers a `data/local/mapper`;
- seeds a `data/local/seed`;
- preferencias a `data/preferences`.

### 6. Corregir seeds y opt-in

Cambios:

- Alcohol: `active = false` por defecto.
- Conducta sexual: `active = false` por defecto.
- Marihuana: `active = false` por defecto.
- Dashboard solo muestra sobriedad si hay tracks activos.
- Onboarding/configuracion activa las abstinencias que el usuario elija.

Como estamos en desarrollo, se puede usar migracion Room o reset de DB local.
Si se conserva DB:

- crear version nueva de Room;
- migrar `abstinence_tracks.active = 0` para presets no confirmados;
- no borrar logs existentes salvo decision explicita.

### 7. Preparar configuracion inicial

Crear base para onboarding/configuracion:

- pantalla o flujo para elegir minimo 3 `ActivityDefinition` como `Mis anclas`;
- opcion de aceptar preset;
- opcion de metas semanales;
- opcion de abstinencias opt-in;
- configuracion de sueno con ventana minima de 5h;
- flag local para saber si la configuracion inicial fue completada u omitida.

Persistencia sugerida:

- `DataStore` o SharedPreferences temporal para flags simples;
- Room para activities, tracks y logs.

### 8. Ajustar score v1 sin redisenarlo entero

Mantener:

- `ScoreState`;
- rangos de `score-states.html`;
- base 700-900 por capas;
- bonus goals 0-100;
- `NoData` sin numero real;
- gates por sueno, recaida, base baja y goals.

Ajustar:

- usar `ActivityDefinition`;
- usar `SleepScoring`;
- quitar limite maximo artificial de 600 min como regla de producto;
- no exigir sobriedad si no hay abstinencias activas;
- revisar mensajes internos para que no digan recaida como castigo.

### 9. Mantener dashboard como contrato existente

No redisenar el dashboard.

Debe seguir presentando:

- `StatusCard`;
- `DailyProgressCard`;
- `AnchorPhraseCard`;
- `ActionButtons`;
- `LayersSection`;
- `SignalsSection` con `Sleep`, `Project`, `Focus`;
- `SobrietySection` solo con tracks activos;
- `ChecklistPreviewSection` para Mis anclas;
- `SupportsSection`;
- `WeekSection`.

Corregir labels con tilde cuando toque pulido UI:

- Restauración
- Atención
- Núcleo sólido

## Orden de implementacion recomendado

1. Crear nuevos modelos de dominio y aliases temporales para que compile la
   migracion.
2. Migrar `TrackedActivity` a `ActivityDefinition` en dominio y scoring.
3. Extraer policies de activity.
4. Extraer `SleepPolicy` y `SleepScoring`.
5. Mover mappers/seeds fuera de `AutonomiaRepository`.
6. Corregir abstinencias opt-in en seed/migracion.
7. Mover inferencia de dashboard fuera de UI.
8. Preparar base de configuracion inicial.
9. Correr tests unitarios y build.

## Pruebas obligatorias

- `ActivityDefinition` con `PrimaryChecklist` aparece como Mis anclas.
- `ActivityLog` sigue siendo el unico hecho diario de activities.
- `Task` neutral no suma al score.
- Sin datos devuelve `NoData`, `visibleScore = null`, pero UI muestra `--`.
- Ventana de sueno menor a 5h no se guarda o devuelve error de dominio.
- Ventana de sueno de 5h es valida.
- Score de sueno evalua cumplimiento contra objetivo personal.
- Sin abstinencias activas, sobriedad no aparece ni limita score.
- Con abstinencia activa y recaida, Conducta y estado se afectan sin lenguaje
  humillante.
- Senales del dashboard son `Sleep`, `Project`, `Focus`.
- Dashboard conserva progreso diario, capas, anclas, soportes, frase y resumen
  semanal.
- `:app:testDebugUnitTest` pasa.
- `:app:assembleDebug` compila.

## Criterio de cierre

La iteracion queda cerrada cuando:

- el lenguaje del codigo separa definitions de logs;
- el backend local tiene boundaries mas claros;
- el score usa policies reutilizables;
- el dashboard sigue funcionando con su contrato actual;
- el seed respeta abstinencias opt-in;
- el sueno aplica minimo configurable de 5 horas;
- hay tests para las reglas criticas.

¿Es este Pro-Prompt lo que necesitas?
