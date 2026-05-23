# Entidades Room — Documentación completa

Fecha: 2026-05-22
Proyecto: Vocal / Autonomía sin límites
Archivo: `app/src/main/java/dev/panopt/autonomia/data/Entities.kt`

---

## 1. LayerEntity — Capas

Dimensión de vida. Son 5 fijas, el esqueleto sobre el que se cuelgan actividades, tareas y señales. No se crean ni borran en runtime.

```kotlin
@Entity(tableName = "layers")
data class LayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val active: Boolean = true,
)
```

| Campo | Tipo | Restricción | Descripción |
|-------|------|-------------|-------------|
| `id` | String | PK | `"layer_interior"`, `"layer_cuerpo"`, `"layer_conducta"`, `"layer_vinculos"`, `"layer_proyecto"` |
| `name` | String | NOT NULL | Nombre visible en UI |
| `description` | String | NOT NULL | Descripción para tooltips/onboarding |
| `sortOrder` | Int | NOT NULL | Orden visual: 10, 20, 30, 40, 50 |
| `active` | Boolean | default true | Si la capa participa en dashboard y métricas |

**Seed** (`DefaultSeeds.layers`): 5 capas con IDs estables.

```text
layer_interior  → Interior  (10)
layer_cuerpo    → Cuerpo    (20)
layer_conducta  → Conducta  (30)
layer_vinculos  → Vinculos  (40)
layer_proyecto  → Proyecto  (50)
```

**Relaciones**: referenciada por `ActivityEntity.layerId`, `TaskEntity.layerId`.

---

## 2. ActivityEntity — Actividades

Plantilla de una acción que el usuario puede registrar. Define QUÉ se mide y CÓMO. El hecho concreto de un día está en `ActivityLogEntity`.

```kotlin
@Entity(tableName = "activities", indices = [Index("layerId")])
data class ActivityEntity(
    @PrimaryKey val id: String,
    val layerId: String,
    val name: String,
    val description: String,
    val type: String,
    val role: String,
    val displaySurface: String,
    val contributionRole: String,
    val importanceTier: String,
    val cadence: String?,
    val targetValue: Int?,
    val minimumValue: Int?,
    val targetCount: Int?,
    val targetPeriod: String?,
    val unit: String,
    val active: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
```

### type — Cómo se mide

| Valor | Significado | Ejemplo |
|-------|-------------|---------|
| `Check` | Sí/No | Bañarse, No celular en cama |
| `Time` | Minutos practicados | Meditar, Ejercicio |
| `Count` | Cantidad numérica | Vasos de agua |
| `Note` | Texto breve | Escribir una línea, Reflexión |
| `TimeOfDay` | Hora concreta | Dormir temprano |

### role — Rol de la actividad

| Valor | Significado |
|-------|-------------|
| `Practice` | Práctica principal (meditar, ejercitarse) |
| `SelfCare` | Cuidado personal (bañarse, comer) |
| `Boundary` | Límite conductual (no decidir madrugada) |
| `DigitalHygiene` | Higiene digital (no celular en cama) |
| `DomesticOrder` | Orden doméstico (orden mínimo) |
| `RelationalHabit` | Hábito relacional (no aislarme, interacción) |
| `ProjectWork` | Trabajo de proyecto (Digitaliza, música) |
| `Learning` | Aprendizaje (leer) |
| `Custom` | Definida por el usuario |

### displaySurface — Dónde aparece

| Valor | Significado | Dashboard | Config panel |
|-------|-------------|-----------|--------------|
| `PrimaryChecklist` | Ancla configurada. Base personal | Sí (checklist) | "Anclas actuales" |
| `SecondaryChecklist` | Cuidado base. Mantenimiento | Sí (soportes) | "Anclas actuales" |
| **`Available`** | **Dataset, sin asignar. Espera que el usuario la elija** | **No** | **"Anclas disponibles"** |
| `Compact` | Vista compacta (no implementado) | — | — |
| `Contextual` | Goals, metas semanales/mensuales | No (sección goals) | Configurable |
| `Silent` | Oculta. No visible en ningún lado | No | No |

### contributionRole — Qué tipo de aporte hace a la estabilidad

| Valor | Significado |
|-------|-------------|
| `Core` | Base personal. Si cae varios días, importa |
| `Support` | Sostiene estructura diaria, secundario |
| `Protective` | Reduce riesgo, protege contra autosabotaje |
| `Recovery` | Ayuda a volver después de una caída |
| `Neutral` | No aporta a estabilidad directamente |

### importanceTier — Cuánto importa

| Valor | Peso |
|-------|------|
| `Low` | 0.75 |
| `Medium` | 1.0 |
| `High` | 1.20 |
| `Critical` | 1.35 |

### cadence — Frecuencia

`null` (sin meta de frecuencia), `Daily`, `Weekly`, `Monthly`, `Custom`, `EventBased`.

### targetValue / minimumValue — Objetivo y mínimo

- `targetValue`: meta en minutos o cantidad (ej. 5 min, 40 min, 8 vasos)
- `minimumValue`: umbral mínimo aceptable (ej. 1 min de meditación cuenta)

### targetCount / targetPeriod — Meta de frecuencia

- `targetCount`: veces por período (ej. 3)
- `targetPeriod`: `Day`, `Week`, `Month`

Ejemplo: `targetCount=3, targetPeriod=Week` = "3 veces por semana"

### unit — Unidad de medida

`Minutes`, `Boolean` (sí/no), `Count` (cantidad), `Time` (hora), `Text` (nota).

### active / archived

- `active`: participa en UI y métricas
- `archived`: retirada por el usuario, conserva historial

**IDs**: prefijo `act_` para seed, `act_custom_<UUID>` para creadas por usuario.

**Seed** (`DefaultSeeds.activities`): 17 actividades con `Available`. Ver `docs/presets-actividades-v1.md`.

**Problemas identificados**:
- Los campos `type`, `role`, `displaySurface`, `contributionRole`, `importanceTier`, `cadence`, `unit` son `String` en vez del enum nativo → frágil, sin type safety
- El mapper (`DomainMappers.kt`) usa `runCatching { Enum.valueOf(it) }.getOrDefault()` que silencia errores de tipeo
- `createdAt`/`updatedAt` en 0L para seeds — deberían usar timestamp real al insertar

---

## 3. ActivityLogEntity — Registros diarios de actividad

El HECHO de un día concreto. "Hoy medité 7 minutos". Un registro por actividad por día.

```kotlin
@Entity(tableName = "activity_logs", primaryKeys = ["activityId", "date"], indices = [Index("date")])
data class ActivityLogEntity(
    val activityId: String,
    val date: String,
    val completed: Boolean,
    val actualValue: Int?,
    val note: String = "",
    val updatedAt: Long,
)
```

| Campo | Tipo | Restricción | Descripción |
|-------|------|-------------|-------------|
| `activityId` | String | PK compuesta, FK → activities.id | Qué actividad |
| `date` | String | PK compuesta | `"2026-05-22"` |
| `completed` | Boolean | NOT NULL | ¿Se completó? |
| `actualValue` | Int? | nullable | Valor real (minutos, cantidad) |
| `note` | String | default "" | Nota opcional |
| `updatedAt` | Long | NOT NULL | Timestamp |

**PK compuesta**: `activityId + date`. Un solo registro por actividad por día.

**Problemas**:
- `date` es `String` en vez de `Long` (epoch) o tipo fecha nativo → difícil ordenar/filtrar por rango
- Sin índice en `activityId` solo

---

## 4. AbstinenceTrackEntity — Rachas de sobriedad

Feature propia. Define QUÉ abstinencia se sigue (alcohol, conducta sexual, marihuana).

```kotlin
@Entity(tableName = "abstinence_tracks")
data class AbstinenceTrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val substanceLabel: String,
    val severity: String,
    val contributionRole: String,
    val importanceTier: String,
    val active: Boolean = true,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | String PK | `"trk_alcohol"`, `"trk_sexual"`, `"trk_marihuana"` |
| `name` | String | Nombre visible |
| `substanceLabel` | String | Etiqueta corta |
| `severity` | String | `Critical`, `Moderate` |
| `contributionRole` | String | Generalmente `Protective` |
| `importanceTier` | String | `Low` a `Critical` |
| `active` | Boolean | Si el usuario la activó |
| `sortOrder` | Int | Orden |
| `createdAt` | Long | Timestamp |
| `updatedAt` | Long | Timestamp |

**Seed**: vacío actualmente. Se sembrarán en onboarding.

**Problemas**: mismos que ActivityEntity (Strings para enums, timestamps en 0L).

---

## 5. AbstinenceLogEntity — Registro diario de abstinencia

```kotlin
@Entity(tableName = "abstinence_logs", primaryKeys = ["trackId", "date"], indices = [Index("date")])
data class AbstinenceLogEntity(
    val trackId: String,
    val date: String,
    val status: String,
    val urge: Boolean = false,
    val urgeIntensity: Int = 0,
    val note: String = "",
    val updatedAt: Long,
)
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `trackId` | String PK | FK → abstinence_tracks.id |
| `date` | String PK | `"2026-05-22"` |
| `status` | String | `Unknown` (sin marcar), `Clean` (limpio), `Relapse` (recaída) |
| `urge` | Boolean | ¿Hubo impulso de consumo? |
| `urgeIntensity` | Int | 0-10 intensidad del impulso |
| `note` | String | Nota |
| `updatedAt` | Long | Timestamp |

---

## 6. RiskEventEntity — Eventos de riesgo

Registro del botón rojo. "Tuve un episodio de riesgo".

```kotlin
@Entity(tableName = "risk_events", indices = [Index("date")])
data class RiskEventEntity(
    @PrimaryKey val id: String,
    val date: String,
    val createdAt: Long,
    val intensity: Int,
    val trigger: String,
    val actionTaken: String,
    val actedOnImpulse: Boolean,
    val note: String,
)
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | String PK | UUID |
| `date` | String | Fecha |
| `createdAt` | Long | Timestamp exacto del evento |
| `intensity` | Int | 1-10 |
| `trigger` | String | Qué lo disparó |
| `actionTaken` | String | Qué se hizo |
| `actedOnImpulse` | Boolean | ¿Se actuó sin pensar? |
| `note` | String | Nota |

---

## 7. TaskEntity — Pendientes

Tarea puntual. No es hábito ni actividad recurrente. Puede asociarse opcionalmente a una capa.

```kotlin
@Entity(tableName = "tasks", indices = [Index("layerId")])
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val layerId: String?,
    val projectId: String?,
    val status: String,
    val contributionRole: String,
    val importanceTier: String,
    val dueDate: String?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | String PK | `"task_<UUID>"` |
| `title` | String | Título |
| `description` | String | Detalle |
| `layerId` | String? | FK → capa (nullable) |
| `projectId` | String? | Futuro: FK → proyecto |
| `status` | String | `Pending`, `Done`, `Archived` |
| `contributionRole` | String | `Support`, `Protective`, `Recovery`, `Neutral` |
| `importanceTier` | String | `Low` a `Critical` |
| `dueDate` | String? | Fecha límite |
| `completedAt` | Long? | Timestamp completado |
| `createdAt` | Long | Timestamp creación |
| `updatedAt` | Long | Timestamp modificación |

**Nota**: `projectId` no se usa actualmente (no hay tabla `projects`).

---

## 8. SleepLogEntity — Registro de sueño

Feature propia. Un registro por día.

```kotlin
@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val date: String,
    val plannedSleepAt: String,
    val plannedWakeAt: String,
    val sleptAt: String,
    val wokeAt: String,
    val quality: String,
    val note: String = "",
    val updatedAt: Long,
)
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `date` | String PK | `"2026-05-22"` |
| `plannedSleepAt` | String | `"23:30"` hora planeada dormir |
| `plannedWakeAt` | String | `"07:30"` hora planeada despertar |
| `sleptAt` | String | `"00:20"` hora real dormir |
| `wokeAt` | String | `"07:00"` hora real despertar |
| `quality` | String | `Low`, `Acceptable`, `Good` |
| `note` | String | Nota |
| `updatedAt` | Long | Timestamp |

---

## 9. Frases Ancla (5 entidades)

### AnchorPhraseEntity

```kotlin
@Entity(tableName = "anchor_phrases")
data class AnchorPhraseEntity(
    @PrimaryKey val id: String,
    val text: String,
    val authorReference: String?,
    val family: String,
    val language: String,
    val attributionStatus: String,
    val active: Boolean = true,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
```

| Campo | Valores |
|-------|---------|
| `family` | `Containment`, `MinimalAction`, `RegulationClarity`, `Persistence`, `IdentityValues`, `Recognition`, `Contemplation` |
| `language` | `"es"`, `"en"` |
| `attributionStatus` | `Clear`, `Traditional`, `Disputed`, `NeedsReview` |

### AnchorPhraseStateRuleEntity

PK compuesta `[phraseId, scoreState]`. Asocia frase con estado de score (`NoData`, `Restoration`, `Attention`, `Motion`, `Plenitude`, `Unbreakable`) y peso.

### AnchorPhrasePhaseRuleEntity

PK compuesta `[phraseId, dayPhase]`. Asocia frase con momento del día (`morning`, `afternoon`, `evening`) y peso.

### AnchorPhraseImpressionEntity

Registro histórico de cada impresión mostrada. Índices en `[date, dayPhase]` y `[phraseId, shownAt]`.

### AnchorPhraseDailySlotEntity

PK compuesta `[date, dayPhase]`. Slot resuelto: qué frase se seleccionó para ese día y momento.

---

## Problemas globales identificados

1. **Enums como String**: `type`, `role`, `displaySurface`, `contributionRole`, `importanceTier`, `cadence`, `severity`, `status`, `quality`, `unit`, `family`, `attributionStatus` son todos `String`. Sin type safety. El mapper silencia errores con `runCatching`.

2. **Fechas como String**: `date` usa `"YYYY-MM-DD"` en vez de `Long` (epoch millis) o tipo nativo. Dificulta ordenamiento y comparación.

3. **Timestamps en 0L**: seeds tienen `createdAt=0L, updatedAt=0L`. Room no los auto-genera.

4. **Sin delete cascade**: borrar una capa no borra sus actividades. Borrar una actividad no borra sus logs. (Aceptable para MVP, peligroso a futuro).

5. **`projectId` huérfano**: `TaskEntity.projectId` referencia una tabla `projects` que no existe.

6. **Sin constraints de integridad**: Room no fuerza FK a nivel SQLite. `layerId` puede tener valores inválidos sin error.

7. **Mapper frágil**: `DomainMappers.kt` convierte entities a domain models con `runCatching { Enum.valueOf(it) }.getOrDefault(...)`. Un typo en la BD = valor silenciosamente incorrecto.
