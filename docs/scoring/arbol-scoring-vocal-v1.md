# Arbol de scoring Vocal v1

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Estado: referencia canonica de formulas aprobadas; implementacion v0 iniciada
Fecha: 2026-05-26
Producto: Vocal / Autonomia sin limites

Este documento define el arbol matematico del scoring de Vocal. Sirve como
guia futura para auditorias de codigo, tests, refactors y revisiones de
producto.

El objetivo no es explicar la filosofia completa de la app, sino responder:

```text
Que variables entran.
Como se agrupan.
Que formula se aplica.
Que resultado debe revisar el codigo.
```

## 1. Alcance

Este documento cubre:

- score semanal de base;
- score por capa;
- anclas;
- soportes;
- TaskMomentum;
- superhabit de anclas;
- sueno semanal dentro de Cuerpo;
- sobriedad activa dentro de Conducta;
- score visible;
- snapshots semanales derivados.

No cubre:

- UI final;
- textos visibles;
- permisos Android concretos;
- calibracion final de estados;
- implementacion Room/DAO.

## 2. Estado de formulas

| Bloque | Estado |
| --- | --- |
| Escala interna `0.000..1.000` | Aprobado |
| Escala visible `700..1000` | Aprobado |
| Capa sin soportes: anclas 100% | Aprobado |
| Capa con soportes: anclas 80%, soportes 20% | Aprobado |
| Anclas: frecuencia 70%, valor 30% | Aprobado |
| Superhabit separado en magnitud visible y bonus capado | Aprobado |
| TaskMomentum por capa | Aprobado |
| Sueno: 40/25/20/15 | Aprobado |
| Sobriedad: Conducta 70%, Sobriedad 30% | Aprobado |
| Sobriedad: pending 0.5, recaida asumida como manual | Aprobado |
| Sobriedad multi-track: promedio 70%, peor track 30% | Aprobado |
| WeeklyBaseScore: promedio 75%, peor capa 25% | Aprobado |
| Politica exacta de estados/umbrales | Aprobado — sellado en scoring-audit-remediation slice 1 |
| Algoritmo exacto de cierre diario | Pendiente de validacion |
| Formula final de StabilityScore | Pendiente de validacion |

Nota de implementacion v0:

```text
El motor actual implementa las formulas aprobadas con los datos disponibles en
el repo. Sueno usa todavia el SleepScoring incremental existente
(duracion + alineacion de horario), porque la telemetria completa de
continuidad/interrupciones/desbloqueos no existe aun.

Inquebrantable no se emite desde una sola semana. Queda reservado para cuando
exista memoria temporal/snapshots con StabilityScore.
```

## 3. Escalas

### 3.1 Escala interna

Todas las formulas de dominio usan escala interna:

```text
0.000 = nada sostenido
1.000 = base cumplida
> 1.000 = superhabit/margen positivo
```

Regla:

```text
Los scores base se cortan en 1.000.
Los bonus pueden llevar una capa hasta 1.200 como maximo local.
```

### 3.2 Escala visible

La UI puede mostrar escala:

```text
700..1000
```

Formula:

```text
VisibleScore =
700 + round(clamp(WeeklyBaseScore, 0.000, 1.000) * 300)
```

Notas:

- `VisibleScore` no debe mostrar valores humillantes bajo 700.
- El superhabit no necesita romper el techo visible.
- La auditoria debe revisar el score interno, no solo el visible.

## 4. Arbol general

```text
WeeklyScoreReport
├─ LayerScores
│  ├─ Interior
│  ├─ Cuerpo
│  │  └─ SleepWeeklyScore
│  ├─ Conducta
│  │  └─ SobrietyWeeklyScore si sobriedad activa
│  ├─ Vinculos
│  └─ Proyecto
│
├─ WeeklyBaseScore
│  ├─ AverageLayerScore   75%
│  └─ WorstLayerScore     25%
│
├─ SurplusSignals
│  ├─ AnchorSurplusMagnitude
│  ├─ AnchorSurplusBonus
│  └─ TaskMomentumBonus
│
├─ StabilityScore         pendiente de formula final
└─ VisibleScore
```

Todas las capas activas pesan igual en el promedio.

## 5. Convenciones de variables

| Variable | Definicion |
| --- | --- |
| `weekStart` | Primer dia local de la semana evaluada. |
| `weekEnd` | Ultimo dia local de la semana evaluada. |
| `layerId` | Identificador de capa. |
| `activeLayers` | Capas activas configuradas por el usuario. |
| `supportsConfigured` | `true` si la capa tiene soportes activos configurados. |
| `clamp(x, min, max)` | Limita `x` al rango indicado. |
| `exp(x)` | Funcion exponencial natural. |
| `average(values)` | Promedio aritmetico. |
| `worst(values)` | Menor valor de una lista. |

## 6. Capa normal

Una capa normal se compone de:

```text
LayerScore
├─ LayerBaseScore
│  ├─ AnchorLayerScore
│  └─ SupportLayerScore si aplica
├─ AnchorSurplusBonus
└─ TaskMomentumBonus
```

### 6.1 Sin soportes configurados

Si el usuario no activo soportes en esa capa:

```text
LayerBaseScore = AnchorLayerScore
```

Regla:

```text
Soportes son opt-in.
No tener soportes no penaliza.
```

### 6.2 Con soportes configurados

Si el usuario activo soportes en esa capa:

```text
LayerBaseScore =
0.800 * AnchorLayerScore
+ 0.200 * SupportLayerScore
```

### 6.3 Score final de capa

```text
LayerScore =
clamp(
  LayerBaseScore
  + AnchorSurplusBonus
  + TaskMomentumBonus,
  0.000,
  1.200
)
```

Reglas:

- `LayerBaseScore` se usa para detectar caidas reales.
- `AnchorSurplusBonus` y `TaskMomentumBonus` son margen positivo.
- Los bonus no reparan una capa estructuralmente caida.

## 7. Anclas

Las anclas son la base recurrente configurada por el usuario.

### 7.1 Variables por ancla

| Variable | Definicion |
| --- | --- |
| `targetDays` | Frecuencia semanal objetivo de la ancla. |
| `targetDailyValue` | Tiempo/cantidad objetivo por dia hecho. |
| `targetValue` | `targetDays * targetDailyValue`. |
| `doneDays` | Cantidad de dias `Done` en la semana. |
| `actualValue` | Suma semanal de minutos/cantidad reales. |

### 7.2 Score base por ancla

```text
FrequencyScore =
clamp(doneDays / targetDays, 0.000, 1.000)

ValueScore =
clamp(actualValue / targetValue, 0.000, 1.000)

AnchorBaseScore =
0.700 * FrequencyScore
+ 0.300 * ValueScore
```

Regla:

```text
La frecuencia pesa mas que el valor porque Vocal premia constancia antes que
acumulacion.
```

### 7.3 Score de anclas por capa

```text
AnchorLayerScore =
average(AnchorBaseScore de anclas activas de la capa)
```

Si la capa activa no tiene anclas, la configuracion es invalida para scoring
completo. La configuracion inicial exige al menos una ancla por capa activa
minima.

### 7.4 Gate de configuracion minima (decision 2026-06-01)

Para emitir un scoring se exigen MINIMO 3 capas activas con al menos 1 ancla cada
una (de las 5 capas: Interior, Cuerpo, Conducta, Vinculos, Proyecto; hasta 2 pueden
quedar inactivas). Una capa cuenta solo si esta activa Y tiene >= 1 ancla; sueno o
sobriedad SIN ancla no la hacen contar.

```text
activeLayersWithAnchor = count(capas activas con >= 1 ancla)
Si activeLayersWithAnchor < 3:
    state = NoData ("Sin datos"), visibleScore = null.
```

Razon: sin una base minima de capas configuradas no hay estructura suficiente para
medir. Se reutiliza el estado NoData existente (no se crea estado nuevo); es el mismo
camino que `hasAnyFact` (sin hechos -> NoData).

Constante: `MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3`.

## 8. Superhabit de anclas

El superhabit se divide en:

```text
Magnitude = lectura visible/explicativa del excedente.
Bonus = impacto controlado sobre score.
```

### 8.1 Magnitud

```text
FrequencyRatio = doneDays / targetDays
ValueRatio = actualValue / targetValue

FrequencySurplusMagnitude =
max(0, FrequencyRatio - 1)

ValueSurplusMagnitude =
max(0, ValueRatio - 1)
```

Ejemplo:

```text
targetValue = 5 min
actualValue = 50 min
ValueRatio = 10.000
ValueSurplusMagnitude = 9.000
```

La UI puede explicar esto como superhabit fuerte sin permitir que rompa el
score.

### 8.2 Bonus capado

```text
FrequencySurplusBonus =
0.100 * (1 - exp(-FrequencySurplusMagnitude / 2))

ValueSurplusBonus =
0.100 * (1 - exp(-ValueSurplusMagnitude / 2))

AnchorSurplusBonus =
0.700 * FrequencySurplusBonus
+ 0.300 * ValueSurplusBonus
```

Reglas:

- el bonus usa curva decreciente;
- el bonus no incentiva metas artificialmente bajas;
- el bonus no compensa capas caidas;
- la magnitud visible puede ser alta aunque el bonus este controlado.

### 8.3 Recomendaciones de meta

```text
Superhabit sostenido de tiempo/cantidad:
sugerir revisar meta tras 7 dias.

Superhabit sostenido de frecuencia:
sugerir revisar meta tras 14 dias.
```

La app recomienda; el usuario decide.

## 9. Soportes

Soportes son opt-in y usan UX inversa: el usuario marca omisiones.

### 9.1 Variables

| Variable | Definicion |
| --- | --- |
| `expectedSupportDays` | Soportes activos de capa por dias evaluables. |
| `omittedSupportDays` | Omisiones registradas por el usuario. |
| `doneSupportDays` | `expectedSupportDays - omittedSupportDays`. |

### 9.2 Formula

```text
SupportLayerScore =
doneSupportDays / expectedSupportDays
```

Equivalente:

```text
SupportLayerScore =
1.000 - (omittedSupportDays / expectedSupportDays)
```

Si no hay soportes configurados:

```text
SupportLayerScore no participa.
LayerBaseScore = AnchorLayerScore.
```

## 10. Tasks / TaskMomentum

Tasks no son goals, no son anclas y no penalizan si quedan pendientes.

### 10.1 Reglas

```text
Task sin capa -> no suma.
Task pendiente -> no penaliza.
Task con capa completada -> aporta TaskMomentumBonus a esa capa.
```

### 10.2 Variables

| Variable | Definicion |
| --- | --- |
| `completedLayerTasks` | Tasks completadas en la semana asociadas a una capa. |

### 10.3 Formula

```text
TaskMomentumRaw =
1 - exp(-completedLayerTasks / 2)

TaskMomentumBonus =
0.050 * TaskMomentumRaw
```

Lectura aproximada:

```text
1 task  -> ~0.020
2 tasks -> ~0.032
3 tasks -> ~0.039
5 tasks -> ~0.046
muchas  -> tiende a 0.050
```

Reglas:

- satura para evitar abuso;
- no compensa peor capa;
- no reemplaza anclas;
- no convierte pendientes en deuda.

## 11. Cuerpo con sueno

Cuerpo integra sueno semanal como modulo especial.

### 11.1 Formula de Cuerpo

```text
BodyScore =
0.700 * BodyBaseWithoutSleep
+ 0.300 * SleepWeeklyScore
```

`BodyBaseWithoutSleep` sigue las reglas normales de capa.

### 11.2 SleepWeeklyScore

```text
SleepWeeklyScore =
0.400 * DurationScore
+ 0.250 * ContinuityScore
+ 0.200 * ScheduleAlignmentScore
+ 0.150 * DigitalInterruptionScore
```

### 11.3 Variables de sueno

| Variable | Definicion |
| --- | --- |
| `DurationScore` | Cumplimiento de duracion protegida frente al objetivo. |
| `ContinuityScore` | Continuidad, fragmentacion y bloque continuo mas largo. |
| `ScheduleAlignmentScore` | Cercania a ventana objetivo de dormir/despertar. |
| `DigitalInterruptionScore` | Penalizacion/lectura por desbloqueos, uso nocturno y reintentos. |

Pendiente tecnico:

```text
Definir APIs/permisos Android concretos para obtener desbloqueos,
interrupciones, screen-on y confianza de fuente.
```

## 12. Conducta con sobriedad

Sobriedad es opt-in. Si esta inactiva, no aparece, no pesa y no limita.

### 12.1 Conducta sin sobriedad

```text
ConductScore = ConductBaseScore
```

### 12.2 Conducta con sobriedad activa

```text
ConductScore =
0.700 * ConductBaseScore
+ 0.300 * SobrietyWeeklyScore
```

## 13. Sobriedad

### 13.1 Variables por track

| Variable | Definicion |
| --- | --- |
| `evaluableDays` | Dias de la semana donde el track estuvo activo/evaluable. |
| `confirmedCleanDays` | Dias confirmados limpios por el usuario. |
| `pendingDays` | Dias dentro de ventana de olvido sin confirmacion. |
| `relapseDays` | Dias dentro de episodio de recaida manual o asumida. |

### 13.2 Reglas de sensibilidad

```text
Ventana de olvido = 5 dias.
Pending dentro de ventana = 0.5 dia limpio.
Recaida asumida = penaliza igual que manual hasta correccion.
Recaida puede durar varios dias y sigue sumando hasta levante/relevo.
```

### 13.3 Formula por track

```text
EffectiveCleanDays =
confirmedCleanDays + 0.5 * pendingDays

CleanCoverageScore =
EffectiveCleanDays / evaluableDays

RelapseProtectionScore =
exp(-relapseDays / 1.5)

TrackingConfidenceScore =
1 - 0.15 * (pendingDays / evaluableDays)

SobrietyTrackScore =
CleanCoverageScore
* RelapseProtectionScore
* TrackingConfidenceScore
```

### 13.4 Varios tracks activos

```text
SobrietyWeeklyScore =
0.700 * average(SobrietyTrackScore)
+ 0.300 * worst(SobrietyTrackScore)
```

Razon:

```text
Una racha limpia no debe ocultar otra racha en recaida.
```

## 14. Score semanal

### 14.1 Variables

| Variable | Definicion |
| --- | --- |
| `LayerScore` | Score final de cada capa activa. |
| `AverageLayerScore` | Promedio de `LayerScore` de capas activas. |
| `WorstLayerScore` | Menor `LayerScore` entre capas activas. |

### 14.2 Formula

```text
AverageLayerScore =
average(LayerScore de capas activas)

WorstLayerScore =
worst(LayerScore de capas activas)

WeeklyBaseScore =
0.750 * AverageLayerScore
+ 0.250 * WorstLayerScore
```

Reglas:

- todas las capas activas pesan igual;
- peor capa arrastra 25%;
- superhabit no debe ocultar peor capa estructuralmente caida;
- sobriedad y sueno impactan a traves de Conducta y Cuerpo.

## 15. StabilityScore

StabilityScore mide la constancia a lo largo del tiempo (memoria temporal). Es una
de las cuatro condiciones de la puerta `Inquebrantable` (§16.4).

### 15.1 Requisito de memoria temporal

```text
REQUIRED_PREVIOUS_WEEKS = 5
```

Se consideran solo las semanas previas versionadas que cumplen:

```text
entry.scoringVersion == SCORING_VERSION   (misma version de scoring)
entry.weekStart != currentWeekStart        (no la semana en curso)
```

Si hay menos de 5 semanas previas validas:

```text
StabilityScore     = null
hasTemporalMemory  = false
evaluatedWeeks     = (semanas previas validas) + 1
```

`hasTemporalMemory = false` bloquea `Inquebrantable` (§16.4) sin importar el resto.

### 15.2 Formula (canonica)

Con al menos 5 semanas previas validas, se toman las 5 mas recientes (orden
descendente por `weekStart`) y se les agrega el `WeeklyBaseScore` de la semana en
curso: una ventana de 6 valores.

```text
window = (5 semanas previas mas recientes).weeklyBaseScore + WeeklyBaseScore_actual

StabilityScore =
0.750 * average(window)
+ 0.250 * worst(window)

hasTemporalMemory = true
evaluatedWeeks    = 6
```

El resultado se acota a `[0, 1]`.

Razon:

```text
La estabilidad usa el MISMO criterio que el score semanal (§14): 75% promedio,
25% peor. Una sola semana floja arrastra la estabilidad igual que una capa floja
arrastra el score, de modo que `Inquebrantable` exige constancia real, no un pico.
```

> Validada explicitamente el 2026-06-02 (decision del dueno). Reemplaza la propuesta
> recursiva historica `0.650 * PreviousStabilityScore + 0.350 * WeeklyBaseScore`, que
> nunca se implemento.

## 16. Estados

Los umbrales y el orden de precedencia estan sellados como contrato desde
`scoring-audit-remediation slice 1`. No son propuesta a discutir.

No usar gates duros.

### 16.1 Bandas sobre WeeklyBaseScore (lower-inclusive / upper-exclusive)

| Estado | Condicion |
| --- | --- |
| `Restauracion` | `WeeklyBaseScore < 0.40` |
| `Atencion` | `0.40 <= WeeklyBaseScore < 0.70` |
| `En marcha` | `0.70 <= WeeklyBaseScore < 0.85` |
| `Plenitud` | `WeeklyBaseScore >= 0.85` |

### 16.2 Ladder de peor capa (caps aplicados sobre la banda)

| Condicion | Cap maximo |
| --- | --- |
| `WorstLayerScore < 0.30` | Fuerza `Restauracion` sin importar base (collapse override) |
| `WorstLayerScore < 0.55` | Cap en `Atencion` |
| `WorstLayerScore < 0.75` | Cap en `En marcha` |
| `WorstLayerScore < 0.80` | Cap en `Plenitud` (bloquea `Inquebrantable`) |
| `WorstLayerScore >= 0.80` | Sin cap (permite `Inquebrantable` si el resto lo habilita) |

### 16.3 Histeresis de estado

Suprime descensos de UN escalon cuando:

```text
lowerBoundary(estadoPrevio) - WeeklyBaseScore <= 0.03
```

Reglas:

- solo amortigua descensos; nunca bloquea ascensos;
- no suprime mas de un escalon;
- `WeeklyBaseScore`, `visibleScore` y `reasons` se exponen crudos (sin alterar);
- `previousState` se deriva del `weeklyHistory` filtrado por `scoringVersion` y `weekStart != actual`.

### 16.4 Puerta Inquebrantable

Requiere que se cumplan TODOS simultaniamente:

1. `hasTemporalMemory = true` (minimo 5 semanas previas versionadas);
2. `WeeklyBaseScore >= 0.90`;
3. `WorstLayerScore >= 0.80`;
4. `StabilityScore >= 0.90`.

Una sola semana perfecta sin historial da como maximo `Plenitud`.

### 16.5 Constantes selladas

```kotlin
STATE_RESTORATION_THRESHOLD      = 0.40f
STATE_ATTENTION_THRESHOLD         = 0.70f
STATE_PLENITUDE_THRESHOLD         = 0.85f
WORST_LAYER_COLLAPSE              = 0.30f
WORST_LAYER_MIN_FOR_MOTION        = 0.55f
WORST_LAYER_MIN_FOR_PLENITUDE     = 0.75f
WORST_LAYER_MIN_FOR_UNBREAKABLE   = 0.80f
STATE_HYSTERESIS_MARGIN           = 0.03f
UNBREAKABLE_BASE_MIN              = 0.90f
UNBREAKABLE_STABILITY_MIN         = 0.90f
```

### 16.6 Asimetria rawScore / baseScore (decision sellada)

El superavit de anclas puede llevar el `rawScore` de una capa por encima de `1.000`,
pero el `weeklyBaseScore` usa solo los `baseScore` de capas. Los bonus de superavit
y `TaskMomentum` mejoran el margen visible de la capa pero NO compensan una capa
estructuralmente caida ni alteran la banda de estado.

Razon: Vocal premia constancia, no acumulacion puntual. Un dia de superavit no
rescata una capa con anclas incumplidas durante la semana.

Esta asimetria es intencional y no debe eliminarse en refactors futuros.

Entradas:

- `WeeklyBaseScore`;
- `WorstLayerScore`;
- `StabilityEvaluation` (hasTemporalMemory, stabilityScore);
- `previousState` (desde historial, para histeresis);
- `hasSleepData` (ver 16.7).

### 16.7 Cap por ausencia de registro de sueno (decision 2026-06-01)

Entrada:

```text
hasSleepData = existe al menos una noche de la semana con sleepScore != null.
```

Regla:

```text
Si hasSleepData == false, el estado se topea en En marcha (Motion):
no puede mostrar Plenitud ni Inquebrantable, sin importar WeeklyBaseScore.
```

Reglas:

- el cap solo baja el estado; `WeeklyBaseScore`, `visibleScore` y `reasons` se
  exponen crudos (igual que la histeresis 16.3);
- no penaliza el numero de Cuerpo: ADR-3 sigue intacto (ausencia != sueno malo).
  El sueno ausente re-normaliza Cuerpo; este cap actua solo sobre el estado;
- se aplica despues del ladder de peor capa (16.2) y antes de la puerta
  Inquebrantable (16.4): al topear en Motion, Plenitud/Inquebrantable quedan
  bloqueados por construccion.

Razon:

```text
El sueno es un pilar CORE (30% de Cuerpo). Sin registro, la base no esta completa
(decisiones-diseno-sueno-v1.md) y mostrar Plenitud mentiria sobre esa completitud.
Sobriedad es opt-in y no dispara este cap; el sueno no es opt-in.
```

## 17. Snapshots semanales

La agregacion semanal automatica desde hechos entra desde el motor inicial.

El snapshot persistido entra despues del motor estable y tests.

### 17.1 Snapshot no es verdad primaria

```text
Verdad primaria = hechos diarios + configuracion + sueno + sobriedad.
Snapshot = cache/historial derivado.
```

### 17.2 Campos sugeridos

```text
weekStart
weekEnd
scoringVersion
calculatedAt
configHash
factsHash
weeklyBaseScore
weeklyScore
stabilityScore
state
visibleScore
worstLayerId
layerSummariesJson
reasonsJson
```

Regla:

```text
Si cambian hechos, configuracion o scoringVersion, el snapshot se invalida y se recalcula.
```

## 18. Checklist de auditoria

Para revisar una implementacion, verificar:

1. Compose no calcula scoring.
2. ViewModel no calcula formulas.
3. Room guarda hechos, no inferencias primarias.
4. `BuildScoreInputUseCase` agrupa hechos semanales automaticamente.
5. `ScoreEngine` recibe input puro.
6. Soportes son opt-in.
7. Tasks sin capa no suman.
8. Tasks pendientes no penalizan.
9. Superhabit tiene magnitud visible y bonus capado.
10. Sobriedad inactiva no aparece ni pesa.
11. Sobriedad pendiente usa ventana de 5 dias.
12. Recaida asumida penaliza como manual hasta correccion.
13. Sueno entra en Cuerpo al 30%.
14. Sobriedad activa entra en Conducta al 30%.
15. Peor capa arrastra el semanal al 25%.
16. Snapshot no se usa como verdad primaria.
