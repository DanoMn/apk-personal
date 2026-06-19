# Arbol de scoring Autonomía sin límites v1

> **Estado: ARCHIVADO (2026-06-17) — modelo VIEJO, NO usar como contrato.** Supersedido por
> `docs/scoring/modelo-scoring-oficial-v1.md` (FUENTE DE VERDAD ÚNICA) + `modelo-matematico-nucleo-v1.md`
> (matemática completa) + `axiomas-modelo-scoring-v1.md` / `axiomas-opt-in-v1.md` (contrato). Se conserva
> como registro histórico y porque la fórmula SELLADA de los 4 componentes del sueño (§11.2) aún no se
> migró al núcleo. **OJO:** las secciones VIEJAS donde el sueño figura como "pilar CORE 30% de Cuerpo / no
> opt-in" (§11, §16.7) están SUPERADAS: el sueño es **opt-in** (término-sombra), igual que la sobriedad.

Estado historico: referencia canonica de formulas aprobadas; modelo de valor de capa + opt-ins cerrado 2026-06-12; implementacion v0 del motor VIEJO en codigo (deuda)
Fecha: 2026-06-12 (ultima actualizacion; redaccion original 2026-05-26)
Producto: Autonomía sin límites

Este documento define el arbol matematico del scoring de Autonomía sin límites. Sirve como
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
| Escala interna `0.000..~1.5` (con superhabit) | Aprobado — modelo v4 merge |
| Escala visible `700..1000` | Aprobado |
| Valor de capa: base = promedio min(R_i,1) de anclas; extra = promedio max(R_i-1,0) de anclas | **Cerrado 2026-06-12** — ver §6-NUEVO |
| Opt-in (sueno/sobriedad) como termino-sombra de peso dinamico `w=BETA·N·(1-M)` | **Cerrado 2026-06-12** — ver §11-NUEVO |
| Senales: sueno continuo [0,1]; sobriedad `M=(1-0.55)^(dias de recaida)`, multi-track producto | **Cerrado 2026-06-12** — ver §11-NUEVO y §12-NUEVO |
| Agregacion global: promedio ponderado con terminos ancla (W0=1) y sombra (w dinamico) | **Cerrado 2026-06-12** — ver §12-NUEVO |
| Bandas de estado: R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10 (1+δ, δ=0.10) | **Cerrado 2026-06-12** — ver §16-NUEVO |
| BETA = 0.818 (despejado de TARGET=0.55) | **Cerrado 2026-06-12** |
| Ancla: formula consolidada (base de compromiso + superhabit saturado) | Aprobado — ver §7 |
| Superhabit de anclas separado en magnitud y bonus | Aprobado |
| TaskMomentum por capa | Aprobado — formula v0; PENDIENTE integracion en modelo v4 |
| Soportes: formula opt-in | **PENDIENTE** — no definida en modelo v4 |
| StabilityScore (ventana 6 semanas, 75/25) | Aprobado — ver §15 |
| Politica exacta de estados/umbrales (Inquebrantable, histeresis, ladder peor capa) | Aprobado — sellado en scoring-audit-remediation slice 1 |
| Algoritmo exacto de cierre diario | Implementado v0 |

### 2.1 Modelo en codigo vs modelo cerrado

```text
ATENCION: el codigo actual en domain/scoring/ implementa el modelo VIEJO
(anclas 80/20 con soportes en mezcla intra-capa, sueno 30% de Cuerpo,
sobriedad 30% de Conducta con pesos fijos). Ese codigo es DEUDA tecnica:
el modelo de valor de capa + opt-ins cerrado el 2026-06-12 (§6-NUEVO,
§11-NUEVO, §12-NUEVO) NO esta implementado aun.

Los bloques marcados "Aprobado" en versiones anteriores de este doc y que
contradigan el modelo v4 quedan SUPERADOS. Las secciones §6.1-§6.3, §11,
§12 originales describen el modelo viejo; los nuevos §6-NUEVO, §11-NUEVO
y §12-NUEVO describen el contrato matematico cerrado.
```

### 2.2 Nota de implementacion v0 (modelo viejo — deuda)

```text
El motor actual implementa las formulas del modelo viejo con los datos
disponibles en el repo. Sueno usa el SleepScoring incremental existente
(duracion + alineacion de horario), porque la telemetria completa de
continuidad/interrupciones/desbloqueos no existe aun.

Inquebrantable no se emite desde una sola semana. Queda reservado para cuando
exista memoria temporal/snapshots con StabilityScore.

El reemplazo de este motor por el modelo v4 es la proxima tarea de modelado
pendiente junto con soportes y tasks.
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

## 6-NUEVO. Valor de capa — modelo cerrado 2026-06-12

> **Este bloque reemplaza §6.1–§6.3 para el modelo nuevo.** La implementacion
> en codigo todavia corresponde al modelo viejo (deuda — ver §2.1).

Referencia reproducible: `docs/scoring/old/exploracion-valor-capa/modelo_valor_capa_v4_merge.py`

### 6-NUEVO.1 Rendimiento de ancla (R)

Cada ancla exporta un valor `R ≥ 0`. La formula consolidada (de
`old/exploracion-rendimiento-ancla/merge-consolidado.md`) calcula `R` a partir de
frecuencia semanal comprometida `F`, objetivo de tiempo por sesion `T` y
minutos reales marcados cada dia:

```text
r_i = t_i / T                       razon diaria (0..>>1)
Ordenar r descendente. D = dias marcados.
Compromiso = los min(D, F) mejores.  Voluntaria = los D-F restantes si D > F.
Si D = 0 → R = 0.

u(r)  = min(r, 1)^γ                 valor-dia (γ >= 1; default γ=1.5)
φ     = (1/F) · Σ_compromiso  u(r_i)  base de compromiso ∈ [0,1]
V     = Σ_voluntaria  u(r_j)        dias-equivalentes voluntarios
base  = 1 − (1 − φ) · exp(−λ_v · V)   reparacion voluntaria, acotada en 1

S_t   = (1/F) · Σ_compromiso  max(r_i − 1, 0)   superavit de tiempo (crudo)
S_d   = V / (7 − F)   (0 si F=7)                superavit de dias
w_t   = (F/7)^κ                                  peso desplazable (κ=1.5 default)
S     = σ_max · (1 − exp(−(w_t·S_t + (1−w_t)·S_d) / σ_0))   superavit saturado

R     = base  +  base^p · S         ∈ [0, 1 + σ_max]
```

Parametros con defaults ilustrativos: `γ=1.5, λ_v=0.5, κ=1.5, p=2.0, σ_max=0.5, σ_0=0.5`.
La calibracion final va contra el dataset de marcas del dueno.

Propiedades garantizadas:
- `R = 0` si no hubo ningun dia.
- `R = 1.000` exacto si se cumplio el objetivo exacto (D=F, cada dia en meta).
- `R > 1` solo con superhabit real; acotado en `1 + σ_max`.
- La frecuencia domina estructuralmente: ningun parametro puede comprar slots vacios.

### 6-NUEVO.2 Componentes del valor de capa

```text
anchor_base(capa) = promedio_i  min(R_i, 1)          ∈ [0, 1]
extra_capa        = promedio_i  max(R_i − 1, 0)      ≥ 0   (SOLO anclas)
```

Solo las anclas contribuyen a `extra_capa`. Los opt-ins (sueno/sobriedad) NO
generan extra/superhabit: su canal es unicamente la base.

### 6-NUEVO.3 Termino-ancla y termino-sombra (opt-in)

Para cada capa activa con anclas:

```text
termino ancla:    (anchor_base,  W0)         W0 = 1 (masa de una capa-ancla)

si la capa ademas tiene opt-in con senal M ∈ [0,1]:
    w_optin = BETA · N · (1 − M)            termino de peso dinamico
    termino sombra:  (M,  w_optin)          w(M=1) = 0 → invisible cuando bien
```

Para una capa solo-opt-in (sin anclas):

```text
termino:  (M, W0)                           el opt-in ES la capa, peso normal
```

Soportes y tasks: pendiente de definicion (no inventes formula — ver §2.1).

## 6. Capa normal — modelo VIEJO (deuda — codigo actual)

> **ATENCION:** Esta seccion describe el modelo VIEJO implementado en codigo.
> Para el contrato matematico cerrado ver §6-NUEVO arriba.

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
La frecuencia pesa mas que el valor porque Autonomía sin límites premia constancia antes que
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

## 11-NUEVO. Opt-ins — modelo cerrado 2026-06-12

> **Este bloque reemplaza §11 y §12 para el modelo nuevo.** La implementacion
> en codigo todavia corresponde al modelo viejo (deuda — ver §2.1).

### 11-NUEVO.1 Parametros globales del motor de opt-ins

```text
BETA = 0.818     despejado de TARGET=0.55
                 "recaida total + anclas perfectas → Atencion (0.55)"
N    = numero de capas activas en la sesion
W0   = 1         masa de un termino-ancla

Mismo BETA para sueno y sobriedad (decision compasiva: no castigar mas al
usuario mas sensible por insomnio o adiccion).
```

### 11-NUEVO.2 Senal del sueno `M_sleep ∈ [0,1]`

```text
4 componentes por noche (duracion, continuidad, alineacion horaria,
interrupciones digitales); cobertura c = noches con dato / 7.

M_sleep = c · avg(noches con dato) + (1 − c) · B_SLEEP

Sin ninguna noche con dato → M_sleep = B_SLEEP = 0.5
B_SLEEP = 0.5  (sin telemetria ≠ fracaso; pendiente decision del dueno si
quiere "sin dato = peso 0" en vez de B_SLEEP)
```

### 11-NUEVO.3 Senal de sobriedad `M_sobr ∈ [0,1]`

```text
M_sobr = (1 − A)^(dias de recaida en la semana)    A = 0.55

Multi-track: producto de la senal por track.
  Track limpio (0 dias de recaida) → M_track = 1.0 → invisible (no penaliza)
  Track con recaida → M_track < 1 → arrastra

M_sobr = Π_tracks  (1 − 0.55)^(dias_recaida_track)

Verificado (N=3, anclas justas):
  1 dia de recaida  → M≈0.45 → MERGE≈0.829 → En marcha
  3 dias de recaida → M≈0.09 → MERGE≈0.612 → Atencion
  7 dias de recaida → M≈0.01 → MERGE≈0.553 → Atencion
```

### 11-NUEVO.4 Peso dinamico del termino-sombra

```text
w_optin = BETA · N · (1 − M)

Propiedades:
  M = 1 (opt-in perfecto) → w = 0 → INVISIBLE, no suma masa al denominador
  M = 0 (recaida total)   → w = BETA·N → arrastre PLANO en N
  Arrastre plano: con anclas perfectas, base = N/(N+BETA·N) = 1/(1+BETA) = 0.55
    → independiente de N (no se diluye con mas capas)
```

Comportamiento verificado (salida python3 `modelo_valor_capa_v4_merge.py`):

```text
P1 justo + sueno bien N=3    MERGE=1.000 PLENITUD
P2 mal sueno M=0.15 N=3      MERGE=0.651 EN MARCHA
P2 mal sueno M=0.15 N=5      MERGE=0.651 EN MARCHA   (plano en N)
P3 recaida M=0 N=3           MERGE=0.550 ATENCION
P3 recaida M=0 N=5           MERGE=0.550 ATENCION     (plano en N)
P4 sueno regular M=0.5 N=5   MERGE=0.855 PLENITUD
P7 superhabit repartido x3   MERGE=1.432 INQUEBRANTABLE
P8 capa solo-opt-in bien     MERGE=1.000 PLENITUD
C2 neutralidad exacta (incl. deficit de anclas): sin opt-in = con opt-in bien ✓
C3 arrastre plano en N: [0.550, 0.550, 0.550, 0.550, 0.550] ✓
C5 Sol=Tin (superhabit rinde igual en cualquier capa) ✓
D8 recaida (0.550) < mal sueno (0.651) ✓
Anti-gate: cambio maximo |dEstado| con dM=0.001 = 0.00070 → continuo ✓
```

### 11-NUEVO.5 Dos opt-ins malos al mismo tiempo

Cuando la capa de sueno tiene M_sleep bajo Y hay recaida en sobriedad (que
actua sobre Conducta), ambos terminos-sombra se acumulan en la agregacion
global (ver §14-NUEVO). Caso realista (mal sueno + recaida 2-3 dias) → Atencion;
extremo absoluto (sueno nulo + recaida 7 dias) → Restauracion. Sin tope
(aceptado por el dueno).

## 11. Cuerpo con sueno — modelo VIEJO (deuda — codigo actual)

> **ATENCION:** Esta seccion describe el modelo VIEJO implementado en codigo.
> Para el contrato matematico cerrado ver §11-NUEVO arriba.

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

## 12-NUEVO. Agregacion global — modelo cerrado 2026-06-12

> **Este bloque reemplaza §14.2 para el modelo nuevo.** La implementacion
> en codigo todavia corresponde al modelo viejo (deuda — ver §2.1).

```text
Terminos que entran al promedio ponderado:
  Por cada capa con anclas:     termino (anchor_base, W0=1)
  Si esa capa tiene opt-in M:   termino-sombra (M, w_optin) si w_optin > 0

  Para capa solo-opt-in (sin anclas):  termino (M, W0=1)

base_global  = Σ(valor · peso) / Σ(peso)    sobre todos los terminos activos

extra_global = promedio_simple de extra_capa sobre capas con anclas
               (pesos IGUALES — superhabit rinde igual en cualquier capa)

ESTADO = min(base_global, 1.0) + extra_global
```

Nota: `extra_global` usa pesos iguales (no ponderado por peso de capa) para
garantizar `Sol = Tin` (superhabit en la capa de sueno rinde igual que en
cualquier otra capa).

## 12. Conducta con sobriedad — modelo VIEJO (deuda — codigo actual)

> **ATENCION:** Esta seccion describe el modelo VIEJO implementado en codigo.
> Para el contrato matematico cerrado ver §11-NUEVO (senal) y §12-NUEVO
> (agregacion).

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

> **OBSOLETO bajo el modelo v4 (2026-06-12):** la sobriedad multi-track NO usa worst-term. La señal
> es producto por track `M_sobr = ∏ (1−A)^(días de recaída del track)`, A=0.55 (§11-NUEVO). Un track
> limpio = 1 (invisible, no diluye). No rige el `0.700·avg + 0.300·worst` de abajo.

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

> **OBSOLETO bajo el modelo v4 (2026-06-12):** el score semanal NO usa worst-term (`WorstLayerScore`
> al 25%). La agregación v4 es `base_global` (promedio ponderado de términos ancla + sombras dinámicas
> de opt-in) + `extra_global` (pesos IGUALES), ver §12-NUEVO. **No hay arrastre de peor capa**: que la
> peor capa no colapse el estado es comportamiento aceptado (emerge del promedio puro).

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

Los umbrales de estado estan en proceso de actualizacion. El modelo viejo
usaba `WeeklyBaseScore` con bandas 0.40/0.70/0.85. El modelo nuevo (v4 merge,
cerrado 2026-06-12) usa `ESTADO = min(base_global,1) + extra_global` con
bandas 0.40/0.62/0.85/1.10 (donde 1.10 = 1+δ con δ=0.10).

Las constantes de codigo (`ScoringConstants.kt`) siguen siendo el modelo viejo
hasta que se implemente el motor v4. No sincronizar codigo con estas bandas
hasta que el motor completo este migrado.

No usar gates duros.

### 16-NUEVO. Bandas modelo v4 (cerradas 2026-06-12)

```text
ESTADO = min(base_global, 1.0) + extra_global     escala [0, ~1.5]

Restauracion:       ESTADO < 0.40
Atencion:     0.40 ≤ ESTADO < 0.62
En marcha:    0.62 ≤ ESTADO < 0.85
Plenitud:     0.85 ≤ ESTADO < 1.10   (= 1 + δ, δ=0.10)
Inquebrantable:     ESTADO ≥ 1.10

Eje semantico:
  Plenitud entra en 0.85 (decision del dueno 2026-06-16: "casi cumplir todo ya es Plenitud").
  Cumplir todo justo = ESTADO = 1.0 → cae DENTRO de Plenitud (zona alta), NO es su inicio.
  Superhabit real repartido → Inquebrantable (≥ 1.10)
  Cumplimiento parcial BAJO (<0.85) → En marcha o Atencion ; parcial ALTO (0.85-1.0) ya es Plenitud
  Opt-in perfecto + todo cumplido → ESTADO = 1.0 (Plenitud; no sube sin extra)
  Recaida total + anclas perfectas → ESTADO ≈ 0.55 (Atencion)
```

### 16.1 Bandas sobre WeeklyBaseScore — modelo VIEJO (deuda — codigo actual)

> **ATENCION:** Estas bandas corresponden al modelo VIEJO en codigo. Para el
> modelo cerrado ver §16-NUEVO arriba.

(lower-inclusive / upper-exclusive)

| Estado | Condicion |
| --- | --- |
| `Restauracion` | `WeeklyBaseScore < 0.40` |
| `Atencion` | `0.40 <= WeeklyBaseScore < 0.70` |
| `En marcha` | `0.70 <= WeeklyBaseScore < 0.85` |
| `Plenitud` | `WeeklyBaseScore >= 0.85` |

### 16.2 Ladder de peor capa (caps aplicados sobre la banda) — OBSOLETO v4

> **OBSOLETO bajo el modelo v4 (2026-06-12):** v4 NO tiene ladder ni caps de peor capa, ni
> `WORST_LAYER_COLLAPSE`. El estado emerge del agregado de pesos puros (§16-NUEVO). La tabla de abajo
> es del modelo VIEJO en código (deuda).

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

### 16.4 Puerta Inquebrantable — OBSOLETO v4 (Inquebrantable es emergente)

> **OBSOLETO bajo el modelo v4 (2026-06-12):** v4 NO usa puerta de 4 condiciones. Inquebrantable
> EMERGE de `ESTADO ≥ 1.10` (§16-NUEVO), sin gates. ⚠️ La condición de **memoria temporal /
> estabilidad multi-semana** (§15) es ORTOGONAL al motor de valor de capa y **no se reconcilió con v4
> esta sesión** — queda pendiente decidir si v4 la conserva. La puerta de abajo es del modelo VIEJO.

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

Razon: Autonomía sin límites premia constancia, no acumulacion puntual. Un dia de superavit no
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

### 18-NUEVO. Items adicionales para el motor v4 (pendiente de implementacion)

Los siguientes items se verifican cuando el motor v4 se implemente:

17. `anchor_base` = promedio `min(R_i, 1)` de anclas de la capa (no mezcla con opt-in directamente).
18. `extra_capa` = promedio `max(R_i-1, 0)` de anclas — SOLO anclas, nunca opt-ins.
19. El termino-sombra del opt-in usa `w = BETA·N·(1-M)`; con `M=1`, `w=0` (invisible).
20. El extra global se promedia con pesos IGUALES entre capas con anclas (`Sol == Tin`).
21. La senal de sobriedad usa `M=(1-0.55)^(dias_recaida)`; multi-track = producto.
22. Sin dato de sueno → `M_sleep = B_SLEEP = 0.5` (no 0).
23. `BETA = 0.818` (constante; mismo valor para sueno y sobriedad).
24. `ESTADO = min(base_global, 1) + extra_global`; bandas 0.40/0.62/0.85/1.10.
25. Soportes y tasks: formula PENDIENTE — no implementar hasta cierre del modelo.
