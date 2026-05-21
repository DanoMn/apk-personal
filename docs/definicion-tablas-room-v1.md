# Definición de tablas Room v1

Este esquema es el primer núcleo estable de la app. La idea es guardar hechos locales, no conclusiones finales. El dashboard puede inferir señales desde estos registros sin acoplar la lógica de salud mental a Compose.

## Principio de diseño

- `layers` define las capas de vida.
- `activities` define qué se puede practicar o cuidar dentro de cada capa.
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

Seeds actuales: Interior, Cuerpo, Conducta, Casa/comida, Vínculos, Proyecto.

## `activities`

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

Regla actual de actividades de tiempo: tap registra `targetValue`; mantener presionado permite registrar minutos reales de hoy.

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
