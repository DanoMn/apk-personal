# Definición de tablas Room v1

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

> **Nota de version**: Actualizado 2026-06-01 — esquema **v12**. Se documentan las 22 entidades registradas en `@Database`; se marcan `activity_logs` y `sleep_logs` como legacy/eliminadas; se agrega historial de migraciones hasta la 12. Version anterior (v5): `user_activity_configs` incorporó `weeklyFrequencyTarget`, `sessionTargetMinutes` y `commitmentDurationMonths`. `commitmentDurationMonths = null` significa **Indefinido**, no ausencia de configuracion. Fuente canonica: `docs/dominio/configuracion-canonica-sistema-v1.md`.

## Principio de diseño

El esquema Room está en **versión 12** (`AutonomiaDatabase.kt`). Hay **22 entidades registradas** en `@Database`.

- `layers` define las capas de vida.
- `activity_definitions` define el catalogo inmutable de actividades (que se puede practicar o cuidar dentro de cada capa).
- `user_activity_configs` guarda la configuracion del usuario para cada actividad (tipo, metas, cadencia, archivado).
- `daily_activity_logs` guarda el hecho diario canonico por actividad (fuente del scoring). Reemplaza a `activity_logs`.
- `abstinence_tracks` separa las rachas críticas o moderadas del checklist diario.
- `abstinence_logs` guarda si un día fue limpio o hubo recaída.
- `abstinence_relapse_events` guarda los eventos de recaída con rango de fechas y fuente.
- `risk_events` guarda aperturas del modo riesgo o eventos de corte de circuito.
- `tasks` guarda pendientes puntuales sin recurrencia.
- `anchor_phrases` guarda el catalogo de citas ancla.
- `anchor_phrase_state_rules`, `anchor_phrase_phase_rules` guardan las reglas de elegibilidad por estado y fase.
- `anchor_phrase_impressions` guarda que frase se mostro, en que fase del dia y bajo que estado.
- `anchor_phrase_daily_slots` mantiene una frase estable dentro de una fase del dia.
- `sleep_config` guarda la configuracion de objetivos de sueño del usuario.
- `sleep_session_state` registra el estado de la sesion de sueño activa.
- `sleep_nights` guarda el encabezado de cada noche de sueño (v12, reemplaza `sleep_logs`).
- `sleep_segments` guarda los segmentos individuales de una noche (dormido / uso digital).
- `daily_closures` registra el cierre diario (hora, timezone, version).
- `weekly_score_snapshots` cachea el scoring semanal calculado (derivado, recalculable).
- `device_activity_events` guarda eventos de uso del dispositivo capturados por telemetría local.
- `telemetry_collection_lease` serializa el acceso de workers concurrentes al pipeline de telemetría.

## `layers`

Capas configurables del sistema personal.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key estable. |
| `name` | `String` | Nombre visible. |
| `description` | `String` | Sentido de la capa. |
| `sortOrder` | `Int` | Orden de UI. |
| `active` | `Boolean` | Permite ocultar sin borrar historial. |

Seeds actuales: Interior, Cuerpo, Conducta, Vinculos, Proyecto.

## `activities` (ELIMINADA — v4)

> **Eliminada en MIGRATION_3_4** (`DROP TABLE activities`). Fue reemplazada por `activity_definitions` + `user_activity_configs`. La clase `ActivityEntity` ya no está registrada en `@Database`. Esta sección se conserva como referencia histórica únicamente.

Actividades editables a futuro. Por ahora se inicializan desde seed.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key estable. |
| `layerId` | `String` | Capa a la que pertenece. |
| `name` | `String` | Nombre visible. |
| `description` | `String` | Ayuda breve o intención. |
| `type` | `String` | `Time`, `Check`, `SelfCare`, `AbstinenceSupport`, `Weekly`, `TimeOfDay`, `Note`. |
| `targetValue` | `Int` | Objetivo base, por ejemplo minutos esperados. |
| `minimumValue` | `Int` | Mínimo para contar como hecho. |
| `unit` | `String` | `Minutes`, `Count`, `Boolean`, `Time`, `Text`. |
| `weeklyTarget` | `Int` | Objetivo semanal si aplica. |
| `importance` | `Int` | Peso futuro para métricas. |
| `active` | `Boolean` | Ocultar sin borrar historial. |
| `sortOrder` | `Int` | Orden de UI. |

## `activity_definitions` (v4)

Catalogo inmutable de actividades. Define QUE se puede hacer, sin estado de usuario.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key estable. `act_*` para presets, `act_custom_<uuid>` para custom. |
| `layerId` | `String` | Capa a la que pertenece. |
| `name` | `String` | Nombre visible. |
| `description` | `String` | Ayuda breve o intencion. |
| `type` | `String` | `Time`, `Check`, `Count`, `Note`, `TimeOfDay`. |
| `role` | `String` | `Practice`, `SelfCare`, `Boundary`, `DigitalHygiene`, `DomesticOrder`, `RelationalHabit`, `ProjectWork`, `Learning`, `Custom`. |
| `unit` | `String` | `Minutes`, `Count`, `Boolean`, `Time`, `Text`. |
| `contributionRole` | `String` | `Core`, `Support`, `Protective`, `Recovery`, `Neutral`. |
| `importanceTier` | `String` | `Low`, `Medium`, `High`, `Critical`. |
| `presetCategory` | `String?` | `"anchor"` o `"support"` para presets; `null` para custom. |
| `sortOrder` | `Int` | Orden de UI. |
| `createdAt` | `Long` | Timestamp local. |
| `updatedAt` | `Long` | Timestamp local. |

Indice: `idx_def_layer` sobre `layerId`.

## `user_activity_configs` (v5, campos legacy v4)

Configuracion de usuario por actividad. Define COMO el usuario usa cada actividad.
Una fila por actividad configurada. Si una actividad del catalogo no tiene fila
aqui, no esta configurada y no aparece en el dashboard.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `activityId` | `String` | **Primary key**. FK → `activity_definitions.id`. Una config por actividad. |
| `activityType` | `String` | `"Anchor"`, `"Support"`, `"Task"`. |
| `active` | `Boolean` | Actividad activa para el usuario. Default `true`. |
| `archived` | `Boolean` | Archivada sin borrar historial. Default `false`. |
| `customName` | `String?` | Nombre personalizado por el usuario. Si es null, se usa el nombre original de la definicion. |
| `customDescription` | `String?` | Descripcion personalizada por el usuario. |
| `cadence` | `String?` | Para anclas nuevas siempre `"Weekly"`. `Monthly` queda como compatibilidad legacy. Null para Support y Task. |
| `targetValue` | `Int?` | Campo legacy/espejo de `sessionTargetMinutes` para scoring actual. Null para Support y Task. |
| `minimumValue` | `Int?` | Minimo para contar como hecho. |
| `targetCount` | `Int?` | Campo legacy/espejo de `weeklyFrequencyTarget`. Para anclas nuevas: `2..7`. Null para Support y Task. |
| `targetPeriod` | `String?` | Para anclas nuevas siempre `"Week"`. `"Month"` queda solo como dato legacy. Null para Support y Task. |
| `weeklyFrequencyTarget` | `Int?` | **Obligatorio para Anchor**: meta semanal, entero `2..7`. Null para Support y Task. |
| `sessionTargetMinutes` | `Int?` | **Obligatorio para Anchor**: tiempo por sesion, entero `1..900`. Null para Support y Task. |
| `commitmentDurationMonths` | `Int?` | Duracion del compromiso en meses. `null` representa **Indefinido** y es un valor valido configurado. Aplica a anclas de cualquier capa. |
| `sortOrder` | `Int` | Orden de UI. |
| `createdAt` | `Long` | Timestamp de creacion. |
| `updatedAt` | `Long` | Timestamp de ultima actualizacion. |

Indice unico: `idx_conf_activity` sobre `activityId`.
FK con `ON DELETE CASCADE`: borrar una definicion borra sus configs.

## `ActivitySurface` enum (v4 — actualizado 2026-05-23)

Reemplaza el viejo `DisplaySurface`.

| Valor | Significado | UI canonico | Targets | UX |
| --- | --- | --- | --- | --- |
| `Anchor` | Practica recurrente que construye base personal | "Ancla", "Mis anclas" | **Obligatorios**: `weeklyFrequencyTarget` + `sessionTargetMinutes`; `commitmentDurationMonths` admite `null = Indefinido` | Normal: usuario marca lo que SI hizo |
| `Support` | Accion de mantenimiento diario que sostiene dignidad | "Soporte", "Soportes" | **Sin targets** | Inversa: usuario marca lo que NO hizo |
| `Task` | Pendiente puntual, una sola vez, sin recurrencia | "Pendiente", "Pendientes" | **Sin targets** | Una vez: desaparece al completar |

Fuente canonica de reglas: `docs/dominio/configuracion-canonica-sistema-v1.md`.

## Migracion v3 → v4 (MIGRATION_3_4)

1. CREATE `activity_definitions` con indice en `layerId`
2. CREATE `user_activity_configs` con FK a `activity_definitions` e indice unico en `activityId`
3. COPY definiciones desde `activities` (mapeando `presetCategory` via CASE)
4. COPY configs desde `activities` (mapeando `displaySurface` → `activityType`, solo rows con config)
5. DROP `activities`

`activity_logs.activityId` sigue apuntando a `user_activity_configs.activityId` (mismos valores de ID, sin cambios en logs). |

## Migracion v4 → v5 (MIGRATION_4_5)

1. ADD COLUMN `weeklyFrequencyTarget INTEGER`.
2. ADD COLUMN `sessionTargetMinutes INTEGER`.
3. ADD COLUMN `commitmentDurationMonths INTEGER`.
4. Copia `targetValue` hacia `sessionTargetMinutes` en configs tipo `Anchor`.
5. Calcula `weeklyFrequencyTarget` desde datos legacy:
   - `targetPeriod = "Week"`: `targetCount` normalizado a `2..7`.
   - `targetPeriod = "Month"`: aproximacion semanal `ceil(targetCount / 4)` normalizada a `2..7`.

La migracion no borra `targetValue`, `targetCount` ni `targetPeriod`; quedan como
espejos/compatibilidad mientras el scoring actual sigue leyendo esos campos.

Regla actual de actividades de tiempo: tap registra `sessionTargetMinutes`
como valor por defecto; mantener presionado permite registrar minutos reales de hoy.

## `activity_logs` (LEGACY — no registrada en @Database desde v9)

> **Legacy**. La tabla sigue existiendo en disco para bases que no se recrearon, pero `ActivityLogEntity` **no está registrada** en `@Database` desde v9. La fuente canónica del scoring es `daily_activity_logs`. No escribir nuevos datos acá.

Hechos diarios por actividad (modelo antiguo).

| Campo | Tipo | Nota |
| --- | --- | --- |
| `activityId` | `String` | Parte de primary key. |
| `date` | `String` | Parte de primary key, formato `YYYY-MM-DD`. |
| `completed` | `Boolean` | Si la acción cuenta como cumplida. |
| `actualValue` | `Int` | Valor real registrado, por ejemplo minutos. |
| `note` | `String` | Reservado para texto futuro. |
| `updatedAt` | `Long` | Timestamp local. |

Primary key compuesta: `activityId + date`. Esto evita duplicados por día.

## `daily_activity_logs` (v9 — canónica desde v9)

Registro diario canónico por actividad. **Esta es la fuente del scoring**, no `activity_logs`.
Migrada desde `activity_logs` en MIGRATION_8_9.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `date` | `String` | Parte de primary key, formato `YYYY-MM-DD`. |
| `timezoneId` | `String` | Timezone en el momento del registro. |
| `subjectType` | `String` | Parte de primary key. Tipo de sujeto: `"Anchor"`, `"Support"`, `"Task"`. |
| `subjectId` | `String` | Parte de primary key. ID de la actividad o tarea. |
| `layerId` | `String?` | Capa asociada. Puede ser null para registros históricos migrados. |
| `status` | `String` | `Done`, `NotDone`, `Omitted`. |
| `actualValue` | `Int?` | Valor real registrado (minutos, conteo, etc.). |
| `note` | `String` | Contexto libre. Default `""`. |
| `createdAt` | `Long` | Timestamp de creacion. |
| `updatedAt` | `Long` | Timestamp de ultima actualizacion. |

Primary key compuesta: `date + subjectType + subjectId`.
Indices: `date`, `subjectId`, `layerId`.

## `abstinence_tracks`

Rachas separadas del checklist diario.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key estable. |
| `name` | `String` | Alcohol, Conducta sexual, Marihuana. |
| `substanceLabel` | `String` | Etiqueta conceptual. |
| `severity` | `String` | `Critical` o `Moderate`. |
| `contributionRole` | `String` | Rol de contribucion al scoring: `Core`, `Support`, `Protective`, `Recovery`, `Neutral`. |
| `importanceTier` | `String` | Nivel de importancia: `Low`, `Medium`, `High`, `Critical`. |
| `active` | `Boolean` | Marihuana queda inactiva por defecto. |
| `sortOrder` | `Int` | Orden de UI. |
| `createdAt` | `Long` | Timestamp local. |
| `updatedAt` | `Long` | Timestamp local. |

Seeds actuales: Alcohol activo crítico, Conducta sexual activa moderada, Marihuana inactiva moderada.

Nota: `Conducta sexual` es la etiqueta visible. Internamente puede representar
una abstinencia de pornografia/masturbacion si el usuario la activa.

## `abstinence_logs`

Marcación diaria de rachas.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `trackId` | `String` | Parte de primary key. |
| `date` | `String` | Parte de primary key, formato `YYYY-MM-DD`. |
| `status` | `String` | `Unknown`, `Clean`, `Relapse`. |
| `urge` | `Boolean` | Reservado para impulsos sin recaída. |
| `urgeIntensity` | `Int` | Reservado para intensidad. |
| `note` | `String` | Reservado para contexto. |
| `updatedAt` | `Long` | Timestamp local. |

Primary key compuesta: `trackId + date`.

## `abstinence_relapse_events` (v10)

Eventos de recaída con rango de fechas. Permite reconstruir la racha incluso cuando
el `status` de `abstinence_logs` se ajusta manualmente.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | UUID. Primary key. |
| `trackId` | `String` | FK → `abstinence_tracks.id`. |
| `startDate` | `String` | Fecha de inicio de la recaída, formato `YYYY-MM-DD`. |
| `endDate` | `String` | Fecha de fin de la recaída, formato `YYYY-MM-DD`. |
| `source` | `String` | Origen del registro: `"user"`, `"system"`. |
| `userAdjusted` | `Boolean` | Si el usuario modificó el rango manualmente. Default `false`. |
| `note` | `String` | Contexto libre. Default `""`. |
| `createdAt` | `Long` | Timestamp local. |
| `updatedAt` | `Long` | Timestamp local. |

Indices: `trackId`, `startDate`, `endDate`.

## `risk_events`

Eventos de modo riesgo. No son castigo; son señales para leer patrones después.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | UUID. |
| `date` | `String` | Fecha local. |
| `createdAt` | `Long` | Timestamp local. |
| `intensity` | `Int` | Intensidad provisional. |
| `trigger` | `String` | Detonante o entrada manual. |
| `actionTaken` | `String` | Acción de corte de circuito. |
| `actedOnImpulse` | `Boolean` | Si el impulso se ejecutó. |
| `note` | `String` | Contexto libre. |

## `tasks`

Pendientes puntuales sin recurrencia (`ActivitySurface.Task`). Desaparecen del dashboard al completarse.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | UUID. Primary key. |
| `title` | `String` | Nombre visible de la tarea. |
| `description` | `String` | Descripcion o contexto adicional. |
| `layerId` | `String?` | Capa asociada. Puede ser null. |
| `projectId` | `String?` | Reservado para agrupacion futura. |
| `status` | `String` | `Pending`, `Done`, `Cancelled`. |
| `contributionRole` | `String` | Rol de contribucion: `Core`, `Support`, `Protective`, `Recovery`, `Neutral`. |
| `importanceTier` | `String` | Nivel de importancia: `Low`, `Medium`, `High`, `Critical`. |
| `dueDate` | `String?` | Fecha de vencimiento, formato `YYYY-MM-DD`. Puede ser null. |
| `completedAt` | `Long?` | Timestamp de completado. Null si pendiente. |
| `createdAt` | `Long` | Timestamp local. |
| `updatedAt` | `Long` | Timestamp local. |

Indice: `layerId`.

## `anchor_phrases`

Catalogo local de citas ancla. La fuente completa de producto esta en
`docs/dominio/frases-ancla.md`.

Regla:

```text
Toda frase activa debe tener cita y autor/referencia.
Las frases sin autor/referencia no entran al catalogo activo.
```

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key estable. |
| `text` | `String` | Cita visible. |
| `authorReference` | `String` | Autor, obra o referencia visible. Obligatorio si `active = true`. |
| `family` | `String` | `Containment`, `MinimalAction`, `RegulationClarity`, `Persistence`, `IdentityValues`, `Recognition`, `Contemplation`. |
| `language` | `String` | `es`, `en` u otro. |
| `attributionStatus` | `String` | `Clear`, `Traditional`, `Disputed`, `NeedsReview`. |
| `active` | `Boolean` | Participa en la seleccion. |
| `sortOrder` | `Int` | Orden estable dentro de familia. |
| `createdAt` | `Long` | Timestamp local o seed timestamp. |
| `updatedAt` | `Long` | Timestamp local o seed timestamp. |

## `anchor_phrase_state_rules`

Reglas de elegibilidad por estado calculado.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `phraseId` | `String` | Parte de primary key. |
| `scoreState` | `String` | Parte de primary key: `NoData`, `Restoration`, `Attention`, `Motion`, `Plenitude`, `Unbreakable`. |
| `weight` | `Int` | Peso relativo para ese estado. No es score emocional. |

Primary key compuesta:

```text
phraseId + scoreState
```

Regla especial:

```text
Contemplation solo puede existir en reglas de Plenitude y Unbreakable.
```

## `anchor_phrase_phase_rules`

Reglas de elegibilidad por fase del dia.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `phraseId` | `String` | Parte de primary key. |
| `dayPhase` | `String` | `Dawn` o `Dusk`. |
| `weight` | `Int` | Peso relativo para esa fase. |

Primary key compuesta:

```text
phraseId + dayPhase
```

## `anchor_phrase_impressions`

Hechos de visualizacion de frases. Sirven para evitar repeticion y auditar que
se mostro en cada fase del dia.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | UUID. |
| `phraseId` | `String` | Frase mostrada. |
| `date` | `String` | Fecha local `YYYY-MM-DD`. |
| `dayPhase` | `String` | `Dawn` o `Dusk`. |
| `scoreState` | `String` | Estado usado por el selector. |
| `shownAt` | `Long` | Timestamp local. |

Indices recomendados:

```text
date + dayPhase
phraseId + shownAt
```

## `anchor_phrase_daily_slots`

Cache opcional para mantener una frase estable dentro de una fase.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `date` | `String` | Parte de primary key. |
| `dayPhase` | `String` | Parte de primary key. |
| `scoreState` | `String` | Estado con el que se resolvio. |
| `phraseId` | `String` | Frase elegida. |
| `resolvedAt` | `Long` | Timestamp local. |

Primary key compuesta:

```text
date + dayPhase
```

Esta tabla puede omitirse si el selector es deterministico, pero conviene para
que el dashboard no cambie por recomposiciones.

## `sleep_config` (v6)

Configuracion de objetivos de sueño del usuario. Un unico registro (singleton).

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key. En la practica es un singleton (`"default"`). |
| `targetSleepAt` | `String` | Hora objetivo de dormir, p.ej. `"23:30"`. |
| `targetWakeAt` | `String` | Hora objetivo de despertar, p.ej. `"07:30"`. |
| `digitalWindDownMinutes` | `Int` | Minutos de wind-down digital antes de dormir. |
| `updatedAt` | `Long` | Timestamp local. |

## `sleep_session_state` (v7)

Estado de la sesion de sueño activa. Registra cuando el usuario inicia el
seguimiento de una noche. Un unico registro activo a la vez.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key. |
| `date` | `String` | Fecha de la noche, formato `YYYY-MM-DD`. |
| `startedAt` | `String` | Momento en que se inició la sesión (ISO string). |
| `updatedAt` | `Long` | Timestamp local. |

## `sleep_nights` (v12 — reemplaza `sleep_logs`)

Encabezado de cada noche de sueño. PK = fecha del despertar. Los sub-scores son
cacheados (derivables desde `sleep_segments`) y null cuando `confidenceLevel = NoData`.
No tiene campo `quality` — el scoring usa el pipeline de 4 componentes.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `nightDate` | `String` | Primary key. ISO `YYYY-MM-DD`, fecha del despertar. |
| `targetSleepAt` | `String` | Hora objetivo de dormir en el momento del registro. |
| `targetWakeAt` | `String` | Hora objetivo de despertar en el momento del registro. |
| `sleepOnsetAt` | `Long?` | Epoch millis. Null si `NoData`. |
| `definitiveWakeAt` | `Long?` | Epoch millis. Null si `NoData`. |
| `confidenceLevel` | `String` | `NoData`, `Low`, `Medium`, `High`. |
| `durationScore` | `Float?` | Sub-score cacheado (peso 0.40). Null si `NoData`. |
| `continuityScore` | `Float?` | Sub-score cacheado (peso 0.25). Null si `NoData`. |
| `alignmentScore` | `Float?` | Sub-score cacheado (peso 0.20). Null si `NoData`. |
| `digitalInterruptionScore` | `Float?` | Sub-score cacheado (peso 0.15). Null si `NoData`. |
| `sleepScore` | `Float?` | Score total cacheado. Null si `NoData`. |
| `note` | `String` | Contexto libre. Default `""`. |
| `source` | `String` | `"auto"` (pipeline de telemetría) o `"manual"` (entrada del usuario). |
| `updatedAt` | `Long` | Timestamp local. |

## `sleep_segments` (v12)

Segmentos individuales de una noche: periodos de sueño o uso digital. Hechos primarios
persistentes porque la telemetría se purga en días. Permiten recalcular scores cuando
cambian los parámetros de interpretacion.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `Long` | Primary key autoincremental. |
| `nightDate` | `String` | FK → `sleep_nights.nightDate` (CASCADE). |
| `startAt` | `Long` | Epoch millis de inicio del segmento. |
| `endAt` | `Long` | Epoch millis de fin del segmento. |
| `kind` | `String` | `"Asleep"` o `"AwakeUse"`. |

Indice: `index_sleep_segments_nightDate` sobre `nightDate` (nombre exacto generado por Room).
FK con `ON DELETE CASCADE`: borrar una noche borra sus segmentos.

## `sleep_logs` (ELIMINADA — v12)

> **Dropeada en MIGRATION_11_12** (`DROP TABLE sleep_logs`). Existió desde v2 hasta v11.
> Fue reemplazada por `sleep_nights` + `sleep_segments`. No hay backfill (datos de dev, ADR-5).

## `daily_closures` (v8)

Registro del cierre diario. Una fila por dia cerrado; el cierre materializa los
estados editables en hechos históricos. Ejecutado por `DailyClosureWorker` (WorkManager)
y como garantía al abrir la app.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `date` | `String` | Primary key, formato `YYYY-MM-DD`. |
| `timezoneId` | `String` | Timezone en el momento del cierre. |
| `closedAt` | `Long` | Epoch millis del cierre. |
| `source` | `String` | `"worker"` o `"app_open"`. |
| `closureVersion` | `Int` | Version del algoritmo de cierre usado. |

## `weekly_score_snapshots` (v8)

Cache del scoring semanal. **Dato derivado, siempre recalculable** desde los hechos
diarios. Se invalida cuando cambia `configHash` o `factsHash`.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `weekStart` | `String` | Parte de primary key, formato `YYYY-MM-DD`. |
| `weekEnd` | `String` | Fin de la semana, formato `YYYY-MM-DD`. |
| `scoringVersion` | `String` | Parte de primary key. Version del algoritmo de scoring. |
| `calculatedAt` | `Long` | Timestamp del calculo. |
| `configHash` | `String` | Hash de la configuracion usada; detecta invalidacion. |
| `factsHash` | `String` | Hash de los hechos diarios; detecta invalidacion. |
| `weeklyBaseScore` | `Float` | Score base semanal (sin bonificaciones). |
| `weeklyScore` | `Float` | Score semanal final. |
| `stabilityScore` | `Float?` | Score de estabilidad. Null si no hay historial suficiente. |
| `state` | `String` | Estado calculado: `NoData`, `Restoration`, `Attention`, `Motion`, `Plenitude`, `Unbreakable`. |
| `visibleScore` | `Int` | Score entero para mostrar en UI. |
| `worstLayerId` | `String?` | ID de la capa con peor performance. Null si no aplica. |
| `layerSummariesJson` | `String` | JSON con resumen por capa. |
| `reasonsJson` | `String` | JSON con razones del estado. |

Primary key compuesta: `weekStart + scoringVersion`.
Indices: `weekEnd`, `calculatedAt`.

## `device_activity_events` (v11)

Eventos de uso del dispositivo capturados por el pipeline de telemetría local (UsageStats API).
Sin red ni backend — todo permanece en el dispositivo.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `Long` | Primary key autoincremental. |
| `eventType` | `String` | Tipo de evento de UsageStats (ej. `"MOVE_TO_FOREGROUND"`). |
| `packageName` | `String?` | Nombre del paquete de la app. Puede ser null para eventos de sistema. |
| `timestamp` | `Long` | Epoch millis del evento. |
| `source` | `String` | Origen de la coleccion (ej. `"usage_stats_worker"`). |
| `createdAt` | `Long` | Timestamp de insercion en la DB. |

Indices: `timestamp`, `eventType`.

## `telemetry_collection_lease` (v11)

Singleton de lease para serializar el acceso de workers concurrentes al pipeline de
telemetría. Evita doble-coleccion cuando varios workers corren en paralelo.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `consumerKey` | `String` | Primary key. Clave del consumidor activo (ej. nombre del worker). |

## Tablas futuras probables

- `nutrition_logs`: comidas, rango calórico y notas.
- `activity_templates` o edición avanzada de actividades: cuando la UI permita crear, editar y archivar actividades desde ajustes.
- `insight_events`: patrones detectados por el algoritmo cuando haya suficiente historial.

> Notas: `sleep_logs` (hora/despertar/calidad subjetiva) existió en v2–v11 y fue **eliminada en v12** — reemplazada por `sleep_nights` + `sleep_segments`. El tracking de uso de celular via UsageStats **ya está implementado** como `device_activity_events` (v11).

## Historial de migraciones

| Versión | Cambio principal |
| --- | --- |
| 1 → 2 | CREATE `sleep_logs` |
| 2 → 3 | UPDATE seeds de `abstinence_tracks` (desactivar todos por defecto) |
| 3 → 4 | CREATE `activity_definitions` + `user_activity_configs`; migra datos desde `activities`; DROP `activities` |
| 4 → 5 | ADD COLUMN `weeklyFrequencyTarget`, `sessionTargetMinutes`, `commitmentDurationMonths` en `user_activity_configs`; migra datos desde campos legacy |
| 5 → 6 | CREATE `sleep_config` |
| 6 → 7 | CREATE `sleep_session_state` |
| 7 → 8 | CREATE `daily_closures` + `weekly_score_snapshots` (con índices) |
| 8 → 9 | CREATE `daily_activity_logs`; migra datos desde `activity_logs` |
| 9 → 10 | CREATE `abstinence_relapse_events` (con índices) |
| 10 → 11 | CREATE `device_activity_events` + `telemetry_collection_lease` |
| 11 → 12 | CREATE `sleep_nights` + `sleep_segments`; DROP `sleep_logs` |
