# Definición de tablas Room v1

> **Nota de version**: Actualizado 2026-05-24 — `user_activity_configs` pasa a esquema v5 para anclas: `weeklyFrequencyTarget`, `sessionTargetMinutes` y `commitmentDurationMonths`. `commitmentDurationMonths = null` significa **Indefinido**, no ausencia de configuracion. Fuente canonica: `docs/configuracion-canonica-sistema-v1.md`.

## Principio de diseño

- `layers` define las capas de vida.
- `activity_definitions` define el catalogo inmutable de actividades (que se puede practicar o cuidar dentro de cada capa).
- `user_activity_configs` guarda la configuracion del usuario para cada actividad (tipo, metas, cadencia, archivado).
- `activity_logs` guarda lo hecho en una fecha concreta.
- `abstinence_tracks` separa las rachas críticas o moderadas del checklist diario.
- `abstinence_logs` guarda si un día fue limpio o hubo recaída.
- `risk_events` guarda aperturas del modo riesgo o eventos de corte de circuito.
- `anchor_phrases` guarda el catalogo de citas ancla.
- `anchor_phrase_impressions` guarda que frase se mostro, en que fase del dia y bajo que estado.

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

## `activities` (DEPRECATED)

> **Deprecada en v4**. Esta tabla fue reemplazada por `activity_definitions` + `user_activity_configs`. Se mantiene esta seccion como referencia historica. La entidad `ActivityEntity` en codigo sigue presente pero sera removida en una limpieza futura.

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

## `activity_definitions` (NEW — v4)

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

## `user_activity_configs` (v5 — actualizado 2026-05-24)

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

Fuente canonica de reglas: `docs/configuracion-canonica-sistema-v1.md`.

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

## `activity_logs`

Hechos diarios por actividad.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `activityId` | `String` | Parte de primary key. |
| `date` | `String` | Parte de primary key, formato `YYYY-MM-DD`. |
| `completed` | `Boolean` | Si la acción cuenta como cumplida. |
| `actualValue` | `Int` | Valor real registrado, por ejemplo minutos. |
| `note` | `String` | Reservado para texto futuro. |
| `updatedAt` | `Long` | Timestamp local. |

Primary key compuesta: `activityId + date`. Esto evita duplicados por día.

## `abstinence_tracks`

Rachas separadas del checklist diario.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key estable. |
| `name` | `String` | Alcohol, Conducta sexual, Marihuana. |
| `substanceLabel` | `String` | Etiqueta conceptual. |
| `severity` | `String` | `Critical` o `Moderate`. |
| `active` | `Boolean` | Marihuana queda inactiva por defecto. |
| `sortOrder` | `Int` | Orden de UI. |

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

## `anchor_phrases`

Catalogo local de citas ancla. La fuente completa de producto esta en
`docs/frases-ancla.md`.

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

## Tablas futuras probables

- `sleep_logs`: hora de dormir, hora de despertar, calidad subjetiva.
- `nutrition_logs`: comidas, rango calórico y notas.
- `phone_usage_snapshots`: horas de celular si más adelante se integra UsageStats.
- `activity_templates` o edición avanzada de `activities`: cuando la UI permita crear, editar y archivar actividades desde ajustes.
- `insight_events`: patrones detectados por el algoritmo cuando haya suficiente historial.
