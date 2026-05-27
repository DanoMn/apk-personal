# Plan de implementación técnica — Scoring semanal v1
## Vocal / Autonomía sin límites

**Estado:** plan técnico para revisión con Codex
**Propósito:** guiar a Codex para contrastar la especificación conceptual de scoring con el código real antes de implementar.
**Documento conceptual base que debe leerse junto a este plan:** `sistema-scoring-semanal-vocal-v1.md`
**Regla principal:** este plan NO debe ejecutarse a ciegas. Codex debe auditar el repo real, mapear entidades/clases actuales y producir un plan de patches por fases antes de modificar código.

---

## 1. Principio técnico

El scoring semanal es lógica de dominio. No debe vivir en Compose, Room, DAO ni directamente dentro del ViewModel.

La arquitectura objetivo debe respetar esta dirección:

```text
Room / DataStore / fuentes locales
        ↓
Repositorios
        ↓
Input builders / use cases
        ↓
Domain scoring engine
        ↓
Dashboard domain / UI state mapper
        ↓
ViewModel
        ↓
Compose
```

Reglas:

```text
Room guarda hechos.
Repositorios exponen hechos.
El dominio calcula inferencias.
ViewModel compone estado de pantalla.
Compose solo presenta estado y dispara acciones.
```

El dominio del scoring debe poder probarse con unit tests sin Android framework.

---

## 2. Referencias técnicas externas

Android Developers recomienda usar domain layer cuando hay lógica compleja o reutilizable, y ubica esta capa entre UI y data. También define que los repositorios son la puerta de entrada a la data layer y que otras capas no deberían acceder directamente a data sources.
Fuentes:
- https://developer.android.com/topic/architecture
- https://developer.android.com/topic/architecture/domain-layer
- https://developer.android.com/topic/architecture/data-layer

Esto encaja con Vocal porque el scoring ya no es una suma visual: es una inferencia de dominio con reglas, pesos, gates, memoria temporal, módulos opt-in y explicación causal.

---

## 3. Alcance de implementación

Este plan cubre:

```text
- scoring semanal de base;
- score por capa;
- anclas;
- soportes;
- tasks relevantes;
- sueño semanal dentro de Cuerpo;
- sobriedad activa dentro de Conducta;
- superávit;
- gates;
- estados de base;
- visibleScore;
- layerReports;
- reasons;
- pruebas unitarias;
- integración futura con dashboard.
```

Fuera de alcance inmediato:

```text
- métrica diaria/readiness;
- UI final del dashboard;
- rediseño visual;
- bloqueo nocturno o implementación de permisos;
- cloud sync;
- auth;
- export/import;
- recomendaciones avanzadas;
- calibración clínica o médica.
```

---

## 4. Antes de implementar: auditoría obligatoria del repo

Codex debe hacer una fase read-only.

Debe localizar y describir:

```text
1. Modelos actuales:
   - ActivityDefinition / TrackedActivity / ActivityEntity;
   - UserActivityConfig;
   - ActivityLog;
   - Layer;
   - SleepLog / SleepSession / entidad real de bloqueo nocturno;
   - AbstinenceTrack;
   - AbstinenceLog;
   - Task;
   - ScoreEngine actual;
   - DashboardInference;
   - DashboardViewModel;
   - Repositories;
   - DAOs;
   - seeds.

2. Qué scoring existe hoy:
   - dónde se calcula;
   - qué pesos usa;
   - qué inputs consume;
   - qué estados produce;
   - qué partes están hardcodeadas;
   - qué está mezclado con UI.

3. Qué datos existen para calcular:
   - frecuencia semanal de anclas;
   - minutos objetivo por sesión;
   - sesiones reales;
   - minutos reales;
   - soportes/omisiones;
   - tasks con capa;
   - sleep sessions reales;
   - abstinence logs;
   - fecha de configuración de base;
   - historial semanal.

4. Qué falta en schema o modelos:
   - campos inexistentes;
   - campos legacy;
   - nombres ambiguos;
   - denominadores faltantes;
   - migraciones requeridas.
```

La auditoría debe terminar con un mapa tipo:

```text
Concepto de dominio → Clase/archivo actual → Estado → Riesgo → Acción propuesta
```

Ejemplo:

```text
ActivityDefinition → TrackedActivity en Models.kt → nombre ambiguo → riesgo medio → mapear sin renombrar en fase 1
```

---

## 5. Arquitectura propuesta de paquetes

Codex debe ajustar estos nombres al repo real. No crear módulos Gradle si no hace falta.

Estructura conceptual:

```text
domain/
  scoring/
    model/
      BaseState.kt
      ScoreMode.kt
      ScoreGate.kt
      ScoreReason.kt
      WeeklyScoreInput.kt
      WeeklyScoreReport.kt
      LayerScoreReport.kt
      AnchorScoreReport.kt
      SupportScoreReport.kt
      TaskScoreReport.kt
      SleepWeeklyScoreReport.kt
      SobrietyScoreReport.kt

    policy/
      AnchorScoringPolicy.kt
      SupportScoringPolicy.kt
      TaskScoringPolicy.kt
      SleepWeeklyScoringPolicy.kt
      SobrietyScoringPolicy.kt
      LayerScoringPolicy.kt
      WeeklyBaseScoringPolicy.kt
      SurplusScoringPolicy.kt
      ScoreGatePolicy.kt
      StabilityScoringPolicy.kt
      VisibleScorePolicy.kt
      BaseStatePolicy.kt

    engine/
      WeeklyScoreEngine.kt

    input/
      BuildWeeklyScoreInputUseCase.kt
```

Regla:

```text
WeeklyScoreEngine no consulta Room.
WeeklyScoreEngine no conoce Compose.
WeeklyScoreEngine no escribe datos.
WeeklyScoreEngine recibe WeeklyScoreInput y devuelve WeeklyScoreReport.
```

---

## 6. Contratos de dominio sugeridos

### 6.1 BaseState

```kotlin
enum class BaseState {
    NoData,
    Restoration,
    Attention,
    Motion,
    Plenitude,
    Unbreakable
}
```

### 6.2 ScoreMode

```kotlin
enum class ScoreMode {
    NoData,
    Provisional,
    WeeklyBuilt
}
```

### 6.3 WeeklyScoreInput

Debe representar hechos ya agrupados, no entidades Room crudas.

Campos conceptuales:

```kotlin
data class WeeklyScoreInput(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val baseConfiguredAt: LocalDate?,
    val activeLayers: List<LayerInput>,
    val anchors: List<AnchorWeeklyInput>,
    val supports: List<SupportWeeklyInput>,
    val tasks: List<TaskWeeklyInput>,
    val sleep: SleepWeeklyInput?,
    val sobriety: SobrietyWeeklyInput?,
    val previousStabilityScore: Double?
)
```

### 6.4 WeeklyScoreReport

```kotlin
data class WeeklyScoreReport(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val mode: ScoreMode,
    val state: BaseState,
    val weeklyBaseScore: Double,
    val weeklySurplusBonus: Double,
    val weeklyScore: Double,
    val stabilityScore: Double?,
    val visibleScore: Int?,
    val layerReports: List<LayerScoreReport>,
    val gates: List<ScoreGate>,
    val reasons: List<ScoreReason>
)
```

### 6.5 LayerScoreReport

Debe permitir explicar cada capa en UI.

```kotlin
data class LayerScoreReport(
    val layerId: String,
    val layerName: String,
    val score: Double,
    val baseScore: Double,
    val anchorScore: Double,
    val supportScore: Double,
    val taskScore: Double,
    val sleepScore: Double?,
    val sobrietyScore: Double?,
    val surplusBonus: Double,
    val stateHint: LayerStateHint,
    val reasons: List<ScoreReason>
)
```

---

## 7. Fórmulas v1 a implementar

### 7.1 AnchorScoringPolicy

```text
frequencyScore = sessionsDone / targetSessions
minuteScore = minutesDone / targetMinutes

frequencyBase = min(frequencyScore, 1.0)
minuteBase = min(minuteScore, 1.0)

anchorBaseScore =
0.70 * frequencyBase
+ 0.30 * minuteBase
```

Superávit:

```text
frequencySurplus =
max(0, sessionsDone - targetSessions) / targetSessions

minuteSurplus =
max(0, minutesDone - targetMinutes) / targetMinutes

anchorSurplusRaw =
0.70 * frequencySurplus
+ 0.30 * minuteSurplus

anchorSurplusBonus = min(anchorSurplusRaw, 0.20)
```

Reglas:

```text
- frequencyScore y minuteScore deben soportar target cero/null con manejo seguro.
- Si una ancla no tiene configuración válida, debe producir warning/reason y no romper cálculo.
- El score base se corta en 1.0.
- El superávit se calcula aparte.
```

---

### 7.2 SupportScoringPolicy

Vocal usa UX inversa para soportes: el usuario marca lo que NO hizo.

Modelo conceptual:

```text
SupportScore = 1.0 - omissionRate
```

Codex debe revisar cómo está registrado esto en el código real.

Regla v1:

```text
Si no hay soportes configurados en una capa:
- no asumir 0 automáticamente sin revisar UX real;
- Codex debe proponer fallback.
```

Fallback sugerido:

```text
SupportScore = neutral 1.0 si la capa no tiene soportes esperados.
SupportScore = 1.0 - omisiones/esperados si existen soportes esperados.
```

---

### 7.3 TaskScoringPolicy

Solo cuentan tasks relevantes.

Condición:

```text
layerId != null
contributionRole != Neutral
```

Fórmula conceptual:

```text
TaskScore = relevantCompletedTasks / relevantPlannedTasks
```

Punto a auditar:

```text
Si el sistema no tiene tasks planificadas por semana, Codex debe definir si el denominador será:
- tasks relevantes creadas esa semana;
- tasks relevantes con dueDate dentro de la semana;
- tasks relevantes completadas vs abiertas;
- o si TaskScore v1 debe funcionar como bonus binario limitado.
```

Regla:

```text
Tasks pesan solo 5% dentro de capa.
```

---

### 7.4 LayerScoringPolicy

Capa normal:

```text
LayerNormalScore =
0.75 * AnchorScore
+ 0.20 * SupportScore
+ 0.05 * TaskScore
```

Cuerpo:

```text
CuerpoScore =
0.70 * CuerpoNormalScore
+ 0.30 * SleepWeeklyScore
```

Conducta sin sobriedad:

```text
ConductaScore = ConductaNormalScore
```

Conducta con sobriedad activa:

```text
ConductaScore =
0.70 * ConductaNormalScore
+ 0.30 * SobrietyWeeklyScore
```

---

### 7.5 SleepWeeklyScoringPolicy

Propuesta v1:

```text
SleepWeeklyScore =
0.50 * durationScore
+ 0.25 * regularityScore
+ 0.15 * continuityScore
+ 0.10 * digitalHygieneScore
```

Codex debe auditar qué datos reales existen:

```text
- duración protegida;
- inicio/fin real de bloqueo;
- desbloqueos;
- fragmentación;
- higiene digital;
- ventana objetivo.
```

Si no existen todos los datos, Codex debe proponer versión incremental.

Ejemplo:

```text
Fase 1: solo durationScore + digitalHygieneScore si es lo único disponible.
Fase 2: agregar regularity y continuity cuando existan datos confiables.
```

---

### 7.6 SobrietyScoringPolicy

Solo si el usuario activó sobriedad.

Si no está activa:

```text
no aparece
no pesa
no limita
```

Si está activa:

```text
SobrietyWeeklyScore =
score de días limpios / impulsos resistidos / recaídas
```

Codex debe auditar modelo real:

```text
- estados diarios disponibles;
- Clean;
- Relapse;
- Unknown;
- urge/impulse resisted si existe;
- fecha de activación;
- logs faltantes.
```

Actualizacion:

```text
Recaida activa no usa gate duro. Penaliza fuerte mediante SobrietyWeeklyScore,
Conducta, peor capa y estabilidad.
```

---

### 7.7 WeeklyBaseScoringPolicy

```text
averageLayers = average(activeLayerScores)
worstLayer = min(activeLayerScores)

WeeklyBaseScore =
0.75 * averageLayers
+ 0.25 * worstLayer
```

Reglas:

```text
- solo cuentan capas activas;
- mínimo conceptual 3 capas;
- si no hay 3 capas activas, modo NoData o Provisional según etapa;
- más capas no aumentan techo, aumentan amplitud de base.
```

---

### 7.8 SurplusScoringPolicy

Regla conceptual:

```text
El superávit nunca resta.
```

Propuesta v1:

```text
if (worstLayer < 0.70) {
    WeeklySurplusBonus = 0.0
} else {
    WeeklySurplusBonus = min(averageSurplus, 0.20)
}
```

Importante:

```text
No usar superávit para tapar una capa caída.
Sí usarlo para diferenciar cumplimiento exacto de cumplimiento con margen.
```

---

### 7.9 StabilityScoringPolicy

Propuesta v1:

```text
stabilityScore =
0.65 * previousStabilityScore
+ 0.35 * weeklyScoreActual
```

Codex debe decidir con el repo real:

```text
- si stabilityScore se persiste;
- si se recalcula desde historial semanal;
- dónde se guarda snapshot semanal;
- cómo se versiona si cambian pesos.
```

Recomendación:

```text
No persistir como verdad primaria hasta que el modelo madure.
Se puede cachear como snapshot derivado si hace falta rendimiento.
```

---

### 7.10 VisibleScorePolicy

Mockup actual:

```text
Sin datos: --
Restauración: 700-749
Atención: 750-799
En marcha: 800-899
Plenitud: 900-949
Inquebrantable: 950-1000
```

Mapeo sugerido:

```text
visibleScore = 700 + normalizedScore * 300
```

Donde:

```text
normalizedScore = min(weeklyScore, 1.0)
```

El superávit no necesariamente aumenta más allá de 1000. Sirve para razones, estados altos y diferenciación.

---

## 8. Fases de implementación sugeridas

### Fase 0 — Auditoría read-only

Sin modificar código.

Entregables:

```text
- mapa de entidades/clases actuales;
- mapa del scoring actual;
- gap analysis contra sistema-scoring-semanal-vocal-v1.md;
- riesgos;
- propuesta de fases ajustada al repo.
```

---

### Fase 1 — Contratos de dominio y tests base

Crear modelos puros y tests vacíos/failing.

No integrar con dashboard todavía.

Entregables:

```text
- BaseState;
- ScoreMode;
- ScoreGate;
- ScoreReason;
- WeeklyScoreInput;
- WeeklyScoreReport;
- LayerScoreReport;
- test fixtures.
```

---

### Fase 2 — Policies atómicas

Implementar y testear:

```text
- AnchorScoringPolicy;
- SupportScoringPolicy;
- TaskScoringPolicy;
- SleepWeeklyScoringPolicy;
- SobrietyScoringPolicy;
```

Cada policy debe tener tests propios.

---

### Fase 3 — LayerScoringPolicy

Implementar:

```text
- capa normal;
- Cuerpo con sueño;
- Conducta con sobriedad activa;
- reports por capa;
- reasons por capa.
```

Tests:

```text
- soporte pesa más que task;
- sueño impacta Cuerpo;
- sobriedad inactiva no pesa;
- sobriedad activa modifica Conducta.
```

---

### Fase 4 — WeeklyScoreEngine

Implementar orquestador:

```text
WeeklyScoreInput → WeeklyScoreReport
```

Debe calcular:

```text
- layerReports;
- weeklyBaseScore;
- weeklySurplusBonus;
- weeklyScore;
- gates;
- state;
- visibleScore;
- reasons.
```

---

### Fase 5 — BuildWeeklyScoreInputUseCase

Leer datos reales desde repositorios y construir input de dominio.

No poner lógica de scoring aquí.

Responsabilidad:

```text
- agrupar semana;
- traer logs;
- traer configs;
- resolver active layers;
- mapear entidades a inputs puros.
```

---

### Fase 6 — Integración con dashboard sin rediseño UI

Conectar `WeeklyScoreReport` al dashboard actual.

Regla:

```text
No rediseñar UI en esta fase.
Solo reemplazar/integrar fuente de score.
```

Dashboard debe poder mostrar:

```text
- estado;
- visibleScore;
- razones principales;
- score por capa;
- gates si corresponde.
```

---

### Fase 7 — Migración gradual de ScoreEngine anterior

Codex debe decidir si:

```text
- reemplaza ScoreEngine actual;
- lo deja como legacy;
- crea WeeklyScoreEngine paralelo;
- adapta DashboardInference.
```

Recomendación:

```text
Crear motor nuevo paralelo primero.
No borrar el motor viejo hasta pasar tests y confirmar dashboard.
```

---

### Fase 8 — UI explicativa por capas

Después de validar motor:

```text
- mostrar layerReports;
- mostrar déficit/superávit;
- mostrar motivo del estado;
- evitar mostrar solo número.
```

---

## 9. Tests mínimos obligatorios

```text
1. Ancla perfecta: 3/3 sesiones y minutos completos = 1.0.
2. Una sesión gigante no reemplaza frecuencia.
3. Superávit se calcula separado y nunca resta.
4. Soportes son opt-in: sin soportes, anclas 100%; con soportes, anclas 80% y soportes 20%.
5. Cuerpo integra sueño al 30%.
6. Conducta sin sobriedad no usa SobrietyScore.
7. Conducta con sobriedad activa usa 30% SobrietyScore.
8. Recaída activa penaliza fuerte sin gate duro.
9. Peor capa arrastra 25% del WeeklyBaseScore.
10. Si worstLayer < 0.70, superávit no empuja estados altos.
11. Sin datos devuelve BaseState.NoData y visibleScore null.
12. Primera semana devuelve ScoreMode.Provisional.
13. Plenitude requiere base alta + worstLayer alto + sin gates.
14. Unbreakable requiere stabilityScore alto + historial suficiente.
15. ViewModel no calcula fórmulas de scoring.
```

---

## 10. Riesgos técnicos

```text
1. Entidades actuales pueden tener nombres legacy o ambiguos.
2. Soportes pueden no tener denominador claro.
3. Tasks pueden no tener dueDate o planificación semanal.
4. SleepWeeklyScore puede no tener todos los datos necesarios.
5. Sobriedad puede estar activa por seed aunque debería ser opt-in.
6. DashboardInference puede estar mezclando reglas de dominio.
7. ScoreEngine actual puede tener fórmulas incompatibles.
8. Persistir stabilityScore demasiado pronto puede congelar un modelo inmaduro.
9. Cambiar score sin tests puede romper dashboard silenciosamente.
```

---

## 11. Pendientes no bloqueantes antes de Codex

No bloquean la auditoría, pero Codex debe detectarlos:

```text
1. Fórmula final de SupportScore según UX inversa real.
2. Denominador real de TaskScore.
3. Datos disponibles para SleepWeeklyScore.
4. Cómo identificar baseConfiguredAt.
5. Si stabilityScore se recalcula o cachea.
6. Umbrales exactos de gates críticos.
7. Cómo representar Plenitude candidata.
8. Duración mínima real para Unbreakable.
```

---

## 12. Criterio de éxito

El plan va bien si al final Codex puede decir:

```text
Este hecho vino de Room.
Este input lo armó el use case.
Esta policy calculó esta parte.
Esta capa quedó así por estas razones.
Este gate limitó el estado.
Este score visible salió de este reporte.
La UI solo lo mostró.
```

---

## 13. Instrucción final para Codex

Codex no debe empezar implementando fórmulas en el dashboard.

Debe empezar con:

```text
1. auditoría read-only;
2. mapa del código;
3. gap analysis;
4. plan de patches;
5. tests;
6. motor de dominio;
7. integración gradual.
```

---

## 14. Actualizacion tecnica tras decisiones de dominio

Esta seccion actualiza el plan de implementacion con las decisiones de diseno
cerradas durante la revision. Prevalece sobre secciones anteriores cuando haya
conflicto.

### 14.1 Prioridad real de implementacion

Antes del motor de formulas hay que implementar el contrato de hechos.

Orden:

```text
1. Registro diario consolidable.
2. Cierre local del dia.
3. Sueno por sesiones/eventos.
4. Sobriedad pendiente/recaida asumida.
5. Base inicial con amortiguacion.
6. Input builder semanal.
7. Motor de scoring.
8. Estado Base.
```

Razon:

```text
Sin hechos confiables, el scoring seria una simulacion.
```

### 14.2 Registro diario nuevo

Crear una tabla limpia para anclas, soportes y tasks:

```text
daily_activity_logs
```

Propuesta Codex pendiente de validacion: crear tambien:

```text
daily_closures
```

Contrato:

```text
Anchor:
Done | NotDone

Support:
Done | Omitted

Task:
Done solo cuando se completa.
```

Reglas:

```text
- no usar ausencia de fila como estado de negocio despues del cierre;
- no permitir backfill libre de dias anteriores;
- el dia cierra a medianoche local;
- usar WorkManager a medianoche local;
- ejecutar cierre de garantia al abrir la app despues de medianoche;
- sobriedad es la unica excepcion con pending moldeable.
```

`daily_closures` deberia guardar:

```text
date
timezoneId
closedAt
source // WorkManager | AppOpenCatchUp
closureVersion
```

Barrido propuesto:

```text
Anchor activa sin log -> NotDone.
Support sin omision -> Done.
Task pendiente -> no crea castigo.
Task completada con capa -> Done.
Sobriedad activa sin marca -> PendingConfirmation en modelo de sobriedad.
```

El cierre propuesto debe ser idempotente y procesar dias pendientes en orden cronologico
cuando la app se abre despues de varios dias.

### 14.3 Base inicial

Agregar entidad o mecanismo equivalente para:

```text
BaseConfigurationEntity
AnchorInitialBaselineEntity
```

La base declarada inicial:

```text
- es contexto;
- tiene amortiguacion;
- no crea logs falsos;
- no castiga de golpe objetivos nuevos ambiciosos;
- permite explicar la distancia entre punto de partida y objetivo.
```

Decision tecnica v1:

```text
La amortiguacion dura una semana.
Durante esa semana, la lectura visible se suaviza hasta En marcha.
El score tecnico bruto se conserva para auditoria.
Al cerrar la primera semana completa, desaparece la amortiguacion.
```

### 14.3.1 Soportes opt-in y base de capa

Soportes no son obligatorios desde el inicio.

```text
Si una capa no tiene soportes configurados:
LayerBaseScore = AnchorLayerScore

Si una capa tiene soportes configurados:
LayerBaseScore =
0.800 * AnchorLayerScore
+ 0.200 * SupportLayerScore
```

Tasks no entran en la base. Entran aparte como `TaskMomentumBonus`.

### 14.4 Tasks como TaskMomentum

Actualizar `TaskScoringPolicy`.

Decision:

```text
No implementar TaskScore = completadas / planificadas como denominador duro.
```

Implementar:

```text
TaskMomentum =
senal positiva acotada para tasks con capa completadas.
```

Reglas:

```text
- task pendiente no penaliza;
- task sin capa no cuenta;
- task completada con capa suma momentum pequeno;
- muchas tasks pequenas tienen rendimiento decreciente;
- TaskMomentum no compensa capa caida;
- maximo conceptual: 5%.
```

Formula aprobada v0:

```text
TaskMomentumRaw = 1 - exp(-completedLayerTasks / 2)
TaskMomentumBonus = 0.050 * TaskMomentumRaw
```

Interpretacion:

```text
TaskMomentum vive como superhabit diario de la capa asociada.
No esta ligado a goals ni a soportes.
Solo cuentan tasks completadas con capa.
```

Razon:

```text
Las tasks son movibles y puntuales. Penalizarlas convierte pendientes en deuda.
```

### 14.4.1 Superhabit de anclas

Separar magnitud visible de bonus capado.

```text
FrequencyRatio = doneDays / targetDays
ValueRatio = actualValue / targetValue

FrequencySurplusMagnitude = max(0, FrequencyRatio - 1)
ValueSurplusMagnitude = max(0, ValueRatio - 1)

FrequencySurplusBonus =
0.100 * (1 - exp(-FrequencySurplusMagnitude / 2))

ValueSurplusBonus =
0.100 * (1 - exp(-ValueSurplusMagnitude / 2))

AnchorSurplusBonus =
0.700 * FrequencySurplusBonus
+ 0.300 * ValueSurplusBonus
```

Recomendaciones:

```text
Superhabit de tiempo/cantidad sostenido -> sugerir revisar meta tras 7 dias.
Superhabit de frecuencia sostenido -> sugerir revisar meta tras 14 dias.
```

### 14.5 Sueno con telemetria maxima

Crear o ajustar modelo de sueno para sesiones:

```text
SleepSessionLogEntity
SleepInterruptionEventEntity
```

Guardar:

```text
- sleepDate del dia donde empezo el sueno;
- startedAt;
- endedAt;
- ventana planificada;
- unlockCount;
- interruptionCount;
- eventos atomicos si existen;
- source;
- sourceConfidence.
```

Regla:

```text
Si Android no entrega una fuente confiable, guardar confianza de fuente y no
inventar calidad.
```

### 14.6 Sobriedad sensible

Sobriedad no debe usar el mismo contrato de ausencia que anclas, soportes y
tasks.

Implementar:

```text
AbstinenceDailyStatus:
Clean | PendingConfirmation | Relapse

RelapseEvent:
Manual | AssumedAfterMissingTracking
```

Reglas:

```text
- ventana de olvido: 5 dias;
- durante la ventana, dias sin marca quedan pendientes;
- cada pending dentro de ventana cuenta como 0.5 dia limpio;
- tras 5 dias, materializar recaida asumida editable;
- recaida asumida penaliza igual que manual hasta correccion del usuario;
- si el usuario no la levanta, seguir sumando dias;
- al levantar/relevar, permitir corregir duracion real o aceptar rango asumido;
- recaida penaliza fuerte, pero no hay gates duros.
```

Formula aprobada v0:

```text
Si sobriedad esta inactiva:
ConductaScore = ConductaBaseScore

Si sobriedad esta activa:
ConductaScore =
0.700 * ConductaBaseScore
+ 0.300 * SobrietyWeeklyScore

EffectiveCleanDays = confirmedCleanDays + 0.5 * pendingDays
CleanCoverageScore = EffectiveCleanDays / evaluableDays

RelapseProtectionScore = exp(-relapseDays / 1.5)
TrackingConfidenceScore = 1 - 0.15 * (pendingDays / evaluableDays)

SobrietyTrackScore =
CleanCoverageScore
* RelapseProtectionScore
* TrackingConfidenceScore

SobrietyWeeklyScore =
0.700 * average(SobrietyTrackScore)
+ 0.300 * worst(SobrietyTrackScore)
```

### 14.7 Sin gates duros

Eliminar `ScoreGatePolicy` como bloqueo duro del plan nuevo o renombrarlo a una
politica de razones/penalizaciones si se necesita expresar condiciones.

Nuevo enfoque:

```text
- pesos;
- penalizaciones;
- razones;
- peor capa;
- estabilidad temporal;
- memoria semanal derivada;
- explicacion visible.
```

Compose no debe interpretar gates. El dominio debe devolver razones y
penalizaciones ya calculadas.

### 14.7.1 Politica de estados v1 (propuesta pendiente)

Propuesta Codex: implementar estados desde score, peor capa, penalizaciones y
estabilidad. No implementar hasta validacion explicita.

Parametros iniciales:

```text
Restauracion: WeeklyBaseScore < 0.40
Atencion: 0.40 <= WeeklyBaseScore < 0.70
En marcha: 0.70 <= WeeklyBaseScore < 0.85
Plenitud: WeeklyBaseScore >= 0.85 + peor capa suficiente
Inquebrantable: WeeklyBaseScore >= 0.90 + stabilityScore >= 0.90 + historial
```

Constantes:

```text
worstLayerCollapse = 0.30
worstLayerMinimumForMotion = 0.55
worstLayerMinimumForPlenitude = 0.75
worstLayerMinimumForUnbreakable = 0.80
minimumWeeksForUnbreakable = 6
stateHysteresisMargin = 0.03
```

No usar gates duros para recaida/sueno/capa. Esas senales deben bajar
sub-scores, penalizaciones, peor capa y estabilidad.

### 14.8 Memoria semanal derivada

`WeeklyScoreSnapshotEntity` queda recomendado como memoria derivada versionada,
no como verdad primaria.

Decision de fase:

```text
Fase de historial y estabilidad del plan tecnico vivo, despues de motor estable
y tests. No usar la numeracion legacy de fases de este documento si entra en
conflicto con docs/plan-tecnico-scoring-vocal.md.
```

Uso:

```text
- stabilityScore;
- Inquebrantable;
- superhabits sostenidos;
- recomendaciones de subir metas;
- tendencias por capa;
- historial explicable.
```

Reglas:

```text
- reconstruible desde hechos;
- versionado por algoritmo;
- invalidable si cambian hechos/configuracion/version;
- no introducir antes de tener motor probado.
```

### 14.9 Estado Base

La pagina de scoring detallado se llama:

```text
Estado Base
```

No vive dentro del dashboard. Se abre desde menu lateral/navegacion principal.
El dashboard solo muestra el resumen.

La pagina debe mostrar:

```text
- estado;
- score visible;
- modo de lectura;
- capas;
- razones;
- anclas;
- soportes;
- TaskMomentum;
- sueno;
- sobriedad;
- superhabit;
- tendencia;
- estabilidad.
```

### 14.10 Tests que reemplazan supuestos antiguos

Agregar o actualizar tests para:

```text
1. Ancla no marcada al cierre queda NotDone.
2. Soporte no omitido al cierre queda Done.
3. Soporte omitido queda Omitted.
4. No existe No registrado historico para anclas/soportes/tasks.
5. Sobriedad si permite PendingConfirmation por 5 dias.
6. Tras 5 dias, sobriedad materializa recaida asumida editable.
7. Task pendiente no penaliza.
8. Task con capa completada aporta TaskMomentum.
9. TaskMomentum tiene cap y rendimiento decreciente.
10. TaskMomentum no compensa base caida.
11. Sueno guarda sesion cruzando medianoche en el dia de inicio.
12. Sueno guarda telemetria y confianza de fuente.
13. Snapshot semanal se puede reconstruir desde hechos.
14. Compose no calcula scoring.
15. ViewModel no calcula formulas.
16. Amortiguacion inicial dura una semana y conserva score bruto.
17. Cierre diario es idempotente.
18. Cierre de garantia procesa dias pendientes en orden.
19. Inquebrantable no aparece antes de 6 semanas.
20. Histeresis evita cambios por ruido minimo sin ocultar score bruto.
```

---

## 15. Implementacion v0 realizada

Fecha: 2026-05-26

Esta implementacion toma como fuente viva principal
`docs/plan-tecnico-scoring-vocal.md` y como referencia matematica
`docs/arbol-scoring-vocal-v1.md`.

Cambios realizados:

```text
- ScoreEngine provisional reemplazado por motor semanal v0.
- Tests de ScoreEngine actualizados al contrato nuevo.
- DailyClosureEntity agregada como registro idempotente de cierre diario.
- WeeklyScoreSnapshotEntity agregada como cache/historial derivado versionado.
- Migracion Room 7 -> 8 creada.
- Repositorio ejecuta cierre de garantia de dias vencidos al abrir dashboard.
- Dashboard sigue consumiendo ScoreReport sin rediseño visual.
- `daily_activity_logs` queda pendiente: v0 mantiene `activity_logs` como fuente
  operativa unica para no introducir doble verdad durante la transicion.
```

Limitaciones v0:

```text
- No existe todavia BuildScoreInputUseCase formal.
- No existe WorkManager de medianoche local.
- No se escriben snapshots semanales todavia.
- SleepWeeklyScore usa el scoring incremental disponible de sleep_logs.
- Sobriedad aun no materializa recaidas asumidas como eventos/rangos editables.
- Estado Base como pagina propia queda pendiente.
```
