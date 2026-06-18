# Especificación: scoring-facts-adapter

Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` (formas de entrada que el
modelo exige por nivel), `proposal.md` § "El trabajo real es el adapter" del cambio
`scoring-motor-nucleo-v1`.

> El adapter es el foco de esfuerzo y de riesgo del cambio. El motor recibe datos con FORMA
> FINAL; esta spec declara, sin huecos, cómo se derivan esas formas desde los hechos crudos de
> `daily_activity_logs`. La spec NO toca Room (Camino A).

## Propósito

`scoring-facts-adapter` transforma los hechos Room de la ventana semanal (ya recolectados en
`ScoreInput` / `WeeklyScoringContext`) en las estructuras de entrada que el motor exige:
por ancla `(F, T, mins[7])`, por soporte `días_sostenidos` (ventana 4 días, UX inversa), por
capa `n_tasks_hoy` (tasks de HOY, efímeras), por track `días_recaída` (ventana 7 días) y la
señal `M` de sueño (Cuerpo). Es la única pieza que conoce la forma cruda de los hechos; el
motor no la conoce. NO recalcula scoring; solo adapta forma.

---

## Entradas y salidas (frontera de datos — sin ambigüedad)

### Entrada
- **`weeklyLogsByActivity: Map<activityId, List<ActivityLog>>`** — logs de actividad de la
  ventana de 7 días, agrupados por ancla, ya deduplicados por `"activityId:date"` (regla
  existente `distinctBy` en `WeeklyScoringContextBuilder`). Cada `ActivityLog` aporta
  `date: String` (ISO `yyyy-MM-dd`, zona local del dispositivo), `actualValue: Int?` (minutos
  o conteo según unidad de la actividad), `status: DailyActivityStatus?` (`Done`/`NotDone`/
  `Omitted`).
- **Config de ancla** (`UserActivityConfig` / `ActivityDefinition`): metas `F` (frecuencia
  días/semana, 2–7), `T` (tiempo objetivo min/sesión), unidad.
- **Logs de soporte** de la ventana (UX inversa: solo se registran omisiones).
- **`tasks: List<Task>`** con `layerId`, `status`, fecha de completado.
- **`abstinenceTracks` + logs de recaída** de la ventana.
- **`sleepNights: List<SleepNightScore>`** ya puntuadas (señal `M` de sueño).
- **`today: LocalDate`** (zona local del dispositivo).

### Salida (forma que el motor consume)
- Por **ancla**: `(F: Int, T: Int, mins: List<Int>)` donde `mins` son los minutos hechos por
  día con actividad en la semana (lista de longitud = nº de días con actividad, no fija en 7;
  el modelo Best-F filtra `m > 0`).
- Por **soporte**: `días_sostenidos: Int` en la ventana indulgente de 4 días.
- Por **capa**: `n_tasks_hoy: Int` (tasks completadas HOY con esa capa).
- Por **track**: `días_recaída: Int` en la ventana de 7 días.
- **Sueño**: señal `M ∈ [0,1]` (o `null` si no hay dato → el motor trata como opt-in inactivo).

Señal de obligatoriedad: si una config de ancla carece de `F` o `T` válidos (≤0), el modelo
degrada esa ancla a `R=0` por contrato; el adapter MUST pasar la entrada tal cual (no inventar
metas).

---

## Requisitos

### Requirement: Derivar `(F, T, mins[7])` por ancla desde `daily_activity_logs`

El adapter MUST construir, por ancla, la lista de minutos POR DÍA de la semana a partir de los
logs deduplicados (`distinctBy "activityId:date"`). Para cada log con actividad: el minuto del
día = `actualValue` (minutos). **Invariante "anclas = solo `Minutes`"** (regla dura del catálogo,
con enforcement de dominio): una ancla NUNCA es `Boolean`/`Text`/`Count`/`Time`, así que NO hay
conversión multi-unidad — el mapeo es directo. Un día SIN log para ese ancla aporta `0` minutos
(no entra a `mins` como valor positivo; el modelo lo trata como slot vacío). `F` y `T` salen de
la config de la actividad. El adapter NO recalcula `R` — solo arma la forma.

#### Scenario: Tres días cumplidos reconstruyen mins → cumplir-justo
- GIVEN un ancla con `F=3, T=30` y logs `[(lun, actualValue=30), (mar, 30), (mié, 30)]`
- WHEN el adapter construye `(F, T, mins)`
- THEN produce `F=3, T=30, mins` con tres valores `30` (y el motor da `R = 1.000`)

#### Scenario: Superhabit por días reconstruido
- GIVEN un ancla con `F=4, T=30` y 6 logs de `actualValue=30`
- WHEN el adapter construye `(F, T, mins)`
- THEN produce `mins` con seis valores `30` (y el motor da `R ≈ 1.266`, caso §1.4)

### Requirement: Logs duplicados, omitidos y NotDone

El adapter MUST respetar la deduplicación por `"activityId:date"` (un solo log por ancla por
día; si hay duplicados, queda el deduplicado existente). Un log con `status = NotDone`
(o `actualValue = 0`) MUST contar como día SIN actividad (no aporta minuto positivo). Un log
con `status = Omitted` MUST excluirse de la ventana (no penaliza ni cuenta como día con
actividad). Solo los logs con minuto/valor `> 0` entran a `mins` como días con actividad.

#### Scenario: Log duplicado no infla la frecuencia
- GIVEN dos logs del mismo ancla en la misma fecha
- WHEN el adapter procesa la ventana
- THEN solo cuenta un día con actividad para esa fecha (dedup `activityId:date`)

#### Scenario: NotDone cuenta como día sin actividad
- GIVEN un ancla con `F=4, T=30` y logs `[(lun, Done, 30), (mar, NotDone, 0), (mié, Done, 30)]`
- WHEN el adapter construye `mins`
- THEN `mins` contiene solo los dos días con `30` (el día `NotDone` no aporta minuto positivo)

#### Scenario: Omitted se excluye de la ventana
- GIVEN un log con `status = Omitted` para un ancla
- WHEN el adapter procesa la ventana
- THEN ese día NO cuenta como día con actividad NI penaliza (se excluye)

### Requirement: Soportes — días sostenidos (ventana 4d, UX inversa)

El adapter MUST derivar, por soporte, `días_sostenidos` en la ventana indulgente de 4 días,
aplicando UX inversa: **sin registro del día = sostenido** (la ausencia de dato NO penaliza).
Solo los registros de OMISIÓN restan días sostenidos. El motor convierte a señal
`s = min(días_sostenidos/4, 1)`.

#### Scenario: Soporte sin registros → totalmente sostenido
- GIVEN un soporte sin ningún registro de omisión en la ventana
- WHEN el adapter deriva `días_sostenidos`
- THEN `días_sostenidos = 4` (señal `s = 1.0`: cumplido por defecto)

#### Scenario: Soporte con una omisión → señal degradada
- GIVEN un soporte con una omisión registrada en la ventana de 4 días
- WHEN el adapter deriva `días_sostenidos`
- THEN `días_sostenidos < 4` (el motor da `s < 1.0`)

### Requirement: Tasks — `n_tasks_hoy` por capa (efímero diario)

El adapter MUST contar, por capa, las tasks completadas HOY (`today`) con esa capa
(`layerId` no nulo). Las tasks de días anteriores NO cuentan (efímero diario: mañana se
resetea). Una task `Neutral` o sin capa NO entra.

#### Scenario: Tasks de hoy cuentan; las de ayer no
- GIVEN dos tasks completadas con capa `Cuerpo`: una completada HOY, otra ayer
- WHEN el adapter deriva `n_tasks_hoy` para `Cuerpo`
- THEN `n_tasks_hoy = 1` (solo la de hoy)

#### Scenario: Task sin capa no cuenta
- GIVEN una task completada hoy con `layerId = null`
- WHEN el adapter deriva `n_tasks_hoy`
- THEN esa task no incrementa el conteo de ninguna capa

### Requirement: Tracks — días de recaída (ventana 7d) → señal de sobriedad

El adapter MUST contar, por track activo, los días de recaída en la ventana de 7 días, que el
motor convierte a `M_sobr = Π_tracks (1−A)^días_recaída` (`A=0.55`). Un track limpio
(0 recaídas) MUST producir factor `1` (invisible, no diluye).

#### Scenario: Track limpio no diluye
- GIVEN un track activo sin recaídas en la ventana
- WHEN el adapter deriva `días_recaída`
- THEN `días_recaída = 0` (el motor da factor `1.0`, opt-in invisible)

#### Scenario: Multi-track compone
- GIVEN dos tracks, uno con 1 día de recaída y otro con 1 día de recaída
- WHEN el adapter deriva `días_recaída` por track
- THEN ambos aportan al producto `M_sobr = (1−A)^1 · (1−A)^1` (composición sin tope)

### Requirement: Sueño — señal M (sin dato = inactivo)

El adapter MUST derivar la señal `M` de sueño desde `sleepNights` ya puntuadas (promedio de
noches con dato; noches `NoData` excluidas). Si no hay ninguna noche con dato en la semana, la
señal MUST propagarse como ausente (`null`) y el motor trata el opt-in de sueño como inactivo
(no penaliza por ausencia). El refinamiento a 4 componentes de sueño está FUERA de alcance
(el adapter consume la señal tal como hoy se computa).

#### Scenario: Sin noches con dato → opt-in de sueño inactivo
- GIVEN una semana sin ninguna noche con dato de sueño
- WHEN el adapter deriva `M`
- THEN `M = null` (ausente) y el motor no aplica término-sombra de sueño

#### Scenario: Noches con dato → promedio
- GIVEN tres noches con `sleepScore` `0.8, 0.6, 1.0` y el resto `NoData`
- WHEN el adapter deriva `M`
- THEN `M = 0.8` (promedio de las noches con dato; `NoData` excluidas)

---

## Restricciones

- **No toca Room** (Camino A): los datos crudos (`actualValue` + fecha por log en
  `daily_activity_logs`) YA existen; el adapter solo los re-forma. Sin migraciones.
- **Zona local del dispositivo** para todo cómputo de fecha/ventana (la fecha del log es
  `LocalDate` en zona local, día calendario del cierre).
- **Dominio puro JVM:** el adapter no importa Compose; vive junto al motor.
- **Reglas de superficie:** anclas (UX normal, targets `F`/`T`), soportes (UX inversa, sin
  targets), tasks (una vez, sin recurrencia).
- **Strict TDD:** tests con hechos sintéticos que reproducen `mins[7]`, días sostenidos, etc.,
  ANTES del cableado.

## Casos límite (escenarios adicionales)

#### Scenario: Semana vacía (DB recién wipeada/re-sembrada)
- GIVEN una semana sin ningún log de actividad, soporte, task ni track
- WHEN el adapter procesa la ventana
- THEN produce estructuras vacías por ancla/soporte/capa/track y `M = null`; el motor degrada
  a `ESTADO = 0` (sin capas activas)

#### Scenario: `actualValue = null` en un log de ancla
- GIVEN un log de ancla con `actualValue = null` y `status = Done`
- WHEN el adapter deriva el minuto del día
- THEN el día se trata como `0` minutos (no como día con actividad positiva): un ancla es
  siempre de unidad `Minutes`, y un `Done` sin minutos cargados no asume cumplimiento de tiempo

> Invariante "anclas = solo `Minutes`" (decidido 2026-06-16): el catálogo NO admite anclas
> `Boolean`/`Text`/`Count`/`Time`; el seed ya se reclasificó y hay enforcement de dominio. Por
> eso el mapeo `actualValue → minuto` para anclas es directo (`mins = actualValue`), sin la
> conversión multi-unidad que contemplaba la versión previa de esta spec.

## Criterios de aceptación

- El adapter reconstruye `(F,T,mins[7])` por ancla, `días_sostenidos` por soporte,
  `n_tasks_hoy` por capa, `días_recaída` por track y señal `M` de sueño, desde los hechos de
  `daily_activity_logs` — verificado con tests de adapter que reproducen los casos §1.4
  (cumplir-justo, superhabit por días, gate sin frecuencia).
- Duplicados deduplicados (`activityId:date`), `NotDone`/`actualValue=0` = día sin actividad,
  `Omitted` excluido — cada uno con su test.
- Soportes UX inversa (sin registro = sostenido), tasks efímeras (solo hoy), tracks y sueño
  derivados correctamente — tests verdes.
- Esquema Room sin cambios; build verde con `testDebugUnitTest`.

---

> **Estado de implementación:** Implementado y verificado en el cambio
> `scoring-motor-nucleo-v1` (archivado 2026-06-17). `ScoringFactsAdapter` con su test suite
> verde; Room no tocado (Camino A).
