# Manejo de cambios de configuración en el tiempo — Anclas (v1)

> **Estado: diseño / plan (pre-implementación).** No es contrato vigente todavía: es la
> guía acordada de QUÉ se va a hacer y CÓMO se implementa en código, para no perder
> contexto. Se vuelve contrato vivo cuando se implemente.
>
> **Scope: SOLO anclas.** Soportes, sobriedad y sueño quedan EXPLÍCITAMENTE fuera de
> este documento (se tratan aparte, por partes). Fecha: 2026-06-22.

---

## 0. El problema

El motor lee la configuración **actual** de cada ancla (sus metas de hoy) y la aplica a
los **hechos viejos** de la ventana de 7 días. Por eso, editar una meta a mitad de
semana reescribe el pasado: días ya cumplidos pueden volverse fallidos (al subir la
meta) o días flojos pueden volverse cumplidos (al bajarla, inflando el puntaje).

Seam exacto: `ScoringFactsAdapter.anchorWindow` (`ScoringFactsAdapter.kt:54-68`) arma
`AnchorWindow(f = def.targetDays(), t = def.targetDailyValue(), mins = ...)` leyendo
`def`, que es la config con los **targets actuales**. `targetDays()` /
`targetDailyValue()` (`ScoringExtensions.kt:15-34`) leen la config vigente. No hay
versionado por fecha.

## 1. Principio rector

> **El pasado nunca se reescribe.** Cada día se juzga con la meta que regía ESE día;
> un cambio de hoy solo afecta hacia adelante.

## 2. Pieza central: tabla de versiones de la vara

En vez de **pisar** la meta al editarla, se **versiona**. Entidad Room nueva:

```
ActivityTargetVersionEntity(
    activityId: String,      // a qué ancla pertenece
    validFrom: LocalDate,    // desde qué fecha rige esta versión
    targetMinutes: Int,      // vara de tiempo vigente
    targetDays: Int,         // vara de frecuencia vigente (días/semana)
)
```

Cada edición de meta **inserta una fila nueva** con `validFrom = fecha del cambio`. La
versión vieja queda con su rango intacto. El motor, para cualquier día, sabe qué meta
regía consultando la versión vigente en esa fecha.

Esta única tabla alimenta los tres casos que la necesitan (minutos, frecuencia,
agregar). **Lo que cambia es cómo se usa**, no la tabla.

## 3. Los cuatro casos

### 3.1 Cambiar minutos (subir/bajar el tiempo) — GRADUAL

**Qué pasa:** cada día se mide con la meta de tiempo que regía ese día. Los días viejos
quedan congelados con su meta vieja; la nueva solo aplica desde su `validFrom`.

**Implementación:**
- `ScoringFactsAdapter.anchorWindow` deja de leer `def.targetDailyValue()`. Para cada
  día de la ventana, busca la versión vigente ese día y usa su `targetMinutes`.
- El ratio de cada día = `minutos_del_día / targetMinutes_vigente_ese_día`.
- `AnchorScoringPolicy.r` (`AnchorScoringPolicy.kt:28`) cambia su entrada: en vez de
  `(f, t, mins)` recibe los **ratios ya calculados por día** (cada uno con su `t`). El
  núcleo de la fórmula (commit/vol/phi/superhábit/gate) **no se toca** → no se recalibra
  el contrato matemático.

### 3.2 Cambiar frecuencia (subir/bajar días por semana) — DE GOLPE A LOS 7 DÍAS

**Qué pasa:** la frecuencia mide la semana entera, no un día; no se puede congelar día
por día. La frecuencia nueva entra en vigor recién cuando pasaron 7 días completos bajo
ella. Durante esa semana de transición, manda la frecuencia vieja.

**Implementación:**
- El adapter usa el `targetDays` vigente en el **día más viejo de la ventana**. Mientras
  la ventana contenga días vividos bajo la `f` vieja, rige la vieja. A los 7 días, todos
  los días son nuevos y rige la nueva.
- Es un solo número de `f` por evaluación → entra en `AnchorScoringPolicy.r` sin tocar
  su núcleo.

**Comportamiento:** subir = gracia de una semana antes de exigir más; bajar = no se puede
inflar durante la transición (anti-trampa).

### 3.3 Agregar una actividad — GRACIA DE 7 DÍAS

**Qué pasa:** una actividad recién creada tiene casi nada de historial; juzgarla ya la
hundiría (pocos días contra una meta semanal). Durante sus primeros 7 días **no entra al
puntaje** (ni suma ni resta). La marcás y construye historial. Al día 8 entra con su
ventana completa.

**Implementación:**
- El reloj de gracia se ancla a `UserActivityConfigEntity.createdAt`
  (`Entities.kt:217-235`). Mientras `today - createdAt < 7 días`, el ancla se **excluye
  del cálculo del puntaje** (filtro en el orquestador `ScoreEngine`, junto al de
  `active`).
- `createdAt` **NO se mueve** ante activar/desactivar (los toggles cambian `active`, no
  `createdAt`). Ver §4.

### 3.4 Quitar una actividad — YA FUNCIONA

**Qué pasa:** al quitar/desactivar/archivar, el ancla desaparece del cálculo de
inmediato y el motor promedia las que quedan ("como si nunca hubiera existido"). El
puntaje sube si era un ancla que arrastraba, pero es legítimo (tenés una meta menos).

**Implementación:** ya existe. `BuildScoreInputUseCase` (`BuildScoreInputUseCase.kt:11-13`)
filtra `activities.filter { it.active && !it.archived }`. El score se **deriva** de las
activas, no se persiste. **Sin cambios.**

## 4. El reloj de gracia se ancla a la fecha de creación (no a las activaciones)

Regla única que evita complicaciones de toqueteo:

> El reloj de gracia de un ancla = `createdAt + 7 días`. **Inmutable ante activar /
> desactivar / reactivar.**

- La **creación** arranca el reloj. Activar/desactivar son toggles posteriores que NO lo
  mueven.
- Reactivar una y otra vez en la misma semana NO reinicia nada: hay un solo reloj.
- Una ancla vieja reincorporada cuenta de inmediato (su `createdAt` ya pasó hace rato),
  sin nueva gracia — coherente con el anti-trampa (apagar/prender no resetea ni perdona
  lo de la ventana).
- Lo que define el resultado son los **hechos reales marcados** + el **estado actual**
  (activa/inactiva), nunca la secuencia de toggles.

## 5. Resumen de cambios en el código

| Pieza | Archivo | Cambio |
|-------|---------|--------|
| Tabla de versiones de la vara | `data/Entities.kt` (+ DAO) | Entidad nueva `ActivityTargetVersionEntity`. Dev: reinstalación limpia, sin migración (Camino A). |
| Insertar versión al editar meta | `AutonomiaRepository.kt` (`configureActivity`) | Editar meta inserta una versión nueva (`validFrom = hoy`), no pisa. |
| Leer vara por fecha | `ScoringFactsAdapter.anchorWindow` (`:54-68`) | Por cada día, vara vigente ese día; `f` = la del día más viejo de la ventana. |
| Entrada de la fórmula | `AnchorScoringPolicy.r` (`:28`) | De `(f, t, mins)` a `(f_borde, ratios_por_día)`. Núcleo intacto. |
| Gracia por antigüedad | `ScoreEngine` (orquestador) | Excluir del puntaje las anclas con `today - createdAt < 7 días`. |
| Quitar (motor) | `BuildScoreInputUseCase` (`:11-13`) | Sin cambios (ya filtra `active`). |
| Quitar como ancla | `removeActivityAsAnchor:913` | **Archivar** (`active=false`) en vez de `deleteUserActivityConfig`. Preserva config + `createdAt` → recuperable, sin trampa. |
| Eliminar custom | `deleteCustomActivity:1042` | **Mandar a baúl** (flag) en vez de `deleteActivityDefinition`. NO borra definición ni hechos. |

## 6. Casos límite

> Solo se incluyen operaciones que EXISTEN en el código (crear, editar metas,
> archivar/desarchivar, borrar custom, agregar/quitar como ancla). NO existen: mover un
> ancla de capa (`layerId` se fija al crear, `DashboardViewModel.createActivity:364`;
> inmutable) ni convertir ancla↔soporte (caminos de creación separados, `:347`).
> Tampoco existe "menos de 3 capas con ancla": el onboarding obliga a 3
> (`OnboardingAnchorsStep.kt:39`) y el candado impide bajar.

### Resueltos

- **Quitar por error y reincorporar a los 2 días** → cuenta de inmediato (su `createdAt`
  ya pasó); los días apagados son días sin marca. Ver §4.
- **Agregar, apagar y reactivar dentro de la gracia** → un solo reloj anclado a
  `createdAt`; el toqueteo no reinicia ni acumula nada. Ver §4.
- **Editar la meta mientras el ancla está en gracia** → editar NO reinicia el reloj de
  gracia (`createdAt` no cambia). El versionado registra los tramos igual; al salir de
  gracia, cada día se evalúa con la versión que regía ese día.
- **Editar la meta varias veces el mismo día** → si hay varias versiones con el mismo
  `validFrom`, **vale la última** (la más reciente del día). Las intermedias se descartan.
- **Editar la meta varias veces en días distintos** → cada día de la ventana usa la
  versión vigente ese día; el motor soporta N versiones en una misma ventana.
- **Persistencia de las versiones de la vara al borrar/quitar** → el hecho diario
  (`daily_activity_logs`, `Entities.kt:36-47`) guarda lo hecho (`actualValue`) pero NO la
  meta de ese día; la config (`user_activity_configs`) se pisa al editar y se borra por
  FK CASCADE (`Entities.kt:213`) al borrar el ancla, mientras los logs persisten. **Regla:
  las versiones de la vara persisten como los hechos, SIN cascade**, para poder evaluar
  logs resucitados (preset re-agregado). La forma exacta (tabla de versiones aparte vs
  estampar la meta en cada hecho diario) se define al implementar.
- **Frecuencia cambiada en cascada** → un cambio de frecuencia entra en vigor 7 días
  después de hacerlo; si se enciman, entran en orden. Ej.: subir 3→5 el 1/jun y 5→7 el
  4/jun ⇒ rige `3` del 1 al 7/jun, `5` del 8 al 10/jun, `7` desde el 11/jun. Bajar es
  simétrico: durante la transición rige la frecuencia vieja (más alta), lo que impide
  inflar la semana en curso (anti-trampa).
- **Quitar / Eliminar un ancla y recuperarla** → **nada se borra**: "Quitar" archiva el
  ancla (`active=false`, recuperable desde tus actividades); "Eliminar" (solo custom) la
  manda a un baúl recuperable, conservando su historial (métrica a largo plazo). Como no
  hay borrado, `createdAt` NUNCA se resetea → al recuperarla cuenta de inmediato (config y
  antigüedad intactas). Esto CIERRA la posible trampa "quitar + re-agregar para perdonar
  una mala semana": sin borrado, el reloj de gracia anclado a `createdAt` no tiene
  agujero. El baúl/recuperación en sí es feature de UI aparte; para el motor basta con que
  config y hechos persistan y las inactivas se filtren (ya ocurre). Reemplaza el supuesto
  previo erróneo de que `removeActivityAsAnchor` borraba la config.

### Manejo en código

La mayoría de estos casos **no requieren código dedicado**: son consecuencia de tres
mecanismos base bien hechos.

| Mecanismo base | Implementación | Casos que resuelve |
|---|---|---|
| **Versión vigente por fecha** | Query: `… WHERE activityId = :id AND validFrom <= :date ORDER BY validFrom DESC, createdAt DESC LIMIT 1`. El `createdAt DESC` desempata varias versiones del mismo día → vale la última. | Editar varias veces el mismo día · en días distintos · frecuencia en cascada |
| **Reloj de gracia en `createdAt`** | El ancla entra al puntaje solo si `today − createdAt ≥ 7` (filtro en `ScoreEngine`). `ConfigEditRule` preserva `createdAt` al editar; los toggles no lo tocan (`toggleUserActivityConfigActive` solo cambia `active`/`archived`/`updatedAt`). | Toqueteo en gracia · editar en gracia · reincorporar a los 2 días |
| **Persistencia sin cascade** | `ActivityTargetVersionEntity` **sin** `ForeignKey` (igual que `daily_activity_logs`, que no tiene FK y por eso sobrevive al borrado). Las versiones quedan aunque se borre la definición. | Persistencia al borrar/quitar · logs resucitados |

Detalle por superficie:
- **Minutos**: el adapter pide, por cada día con marca, la versión vigente ESE día → ratio
  con su `targetMinutes`.
- **Frecuencia**: el adapter pide la versión vigente en el día más viejo de la ventana →
  un solo `targetDays`. El escalonamiento (incluida la cascada, subir o bajar) sale de
  esa única regla.
- **Agregar**: el filtro de gracia (`today − createdAt ≥ 7`) excluye el ancla nueva hasta
  que su ventana se llena. Sin lógica extra.
- **Quitar**: ya funciona (`BuildScoreInputUseCase:11-13` filtra `active`). Sin cambios.

---

## 7. Estado de implementación

### FASE 1 — Migración a ventana móvil de 7 días ✅ IMPLEMENTADA

Prerequisito de todo lo demás. Sin ventana móvil, la regla de frecuencia ("la meta del día
más viejo de la ventana") y el comportamiento "entra 7 días después" no se cumplen, y persiste
el reset del lunes (#858).

- `WeeklyScoringContextBuilder.kt`: `weekStart = today.minusDays(6)` (antes
  `previousOrSame(MONDAY)`). La ventana ahora es SIEMPRE de 7 días → el lunes no colapsa.
- `WeeklyScoreSnapshotWriter.kt`: el snapshot de la semana en curso se calcula sobre la ventana
  móvil pero se persiste bajo la CLAVE de semana calendario (Opción A del diseño) → la historia
  mantiene una fila por semana y el estado en curso coincide con el cálculo en vivo. El back-fill
  de semanas vencidas no cambia (windowEnd = domingo ⇒ móvil ≡ calendario).
- Tests: `ScoreEngineTest.mondayDoesNotCollapseTheWindow` / `midWeekWindowIncludesPriorCalendarWeekDays`
  (nuevos, fijan #858); `WeeklyScoreSnapshotWriterTest` ajustado al rango móvil.
- Resuelve #858 root cause 1 (reset del lunes) por completo y vuelve obsoleta la proración
  (root cause 2): la ventana siempre tiene 7 días. La amortiguación inicial (root cause 3) la
  cubre la gracia de la FASE 2 (no tocada aún).
- NO se tocó: estabilidad multi-semana (sigue inerte), sueño-semanal-en-vivo (deuda preexistente,
  `DashboardProjection` pasa solo la noche de hoy), renombres `week*`→`window*` (cosmético).

### FASE 2 — Versionado de la vara + no-borrar ✅ IMPLEMENTADA (2 pendientes acotados)

Hecho:
- Tabla `activity_target_versions` (sin FK → sobrevive al borrado) + DAO + reglas puras
  (`ActiveTargetVersionRule` = versión vigente por fecha; `TargetVersionDecisionRule`).
- `configureActivity` registra versiones al crear/editar metas de un ancla, con backfill lazy
  de la versión inicial anclada a `createdAt`.
- El motor evalúa cada día con su meta: `anchorWindow` lee la versión vigente por fecha
  (minutos por día) y la frecuencia del día más viejo; `AnchorScoringPolicy.rFromRatios`
  (núcleo extraído, `r` delega). `ScoreInput.targetVersions` (default vacío = legacy).
- Cableado en AMBOS caminos: snapshot (`WeeklyScoreSnapshotWriter`) y live
  (`DashboardViewModel` → `DashboardEngine` → `DashboardProjection`). Editar una meta ya no
  reescribe el pasado en ningún lado.
- No-borrar: `removeActivityAsAnchor` ARCHIVA (`active=false`, no borra) → `createdAt` estable
  → cierra la trampa "quitar + re-agregar"; `addActivityAsAnchor` reactiva al re-agregar.
- Tests: `ActiveTargetVersionRuleTest`, `TargetVersionDecisionRuleTest`,
  `editingMinutesTargetMidWeekDoesNotRewritePastWithVersions`. Suite completa verde + APK arma.

Pendiente (acotado):
- **GRACIA de 7 días** (§3.3, excluir del puntaje las anclas con `today − createdAt < 7`): NO
  implementada. Su interacción con el mínimo de 3 capas rompe el caso del usuario nuevo (3 anclas
  recién creadas, todas en gracia → NoData), que es el arranque (#858 amortiguación inicial),
  fuera de este scope por decisión del dueño. Requiere resolver el arranque primero.
- **BAÚL para `deleteCustomActivity`** (Eliminar custom): hoy borra la definición del catálogo;
  "nada se borra" pide un flag de baúl en `ActivityDefinitionEntity` + filtrar el catálogo
  (feature de UI). La trampa del SCORING ya está cerrada por `removeActivityAsAnchor` (Quitar).
