# Propuesta — Rotación de frases ancla (`anchor-phrase-rotation`)

> **Estado SDD:** propuesta
> **Proyecto:** apk-personal — "Autonomía sin límites" (Android, Kotlin + Jetpack Compose + Room, local-first)
> **Spec de producto (fuente de verdad):** `docs/dominio/frases-ancla.md` (vivo)
> **Plan técnico (fuente de verdad):** `meta/instructions/2026-06-04-rotacion-frases-ancla.md`
> **Decisión de arquitectura:** Manera A (con persistencia / "libreta") — ya cerrada en el plan.

---

## 1. Intención / problema

Hoy el dashboard muestra **una frase hardcodeada** (cita de Kierkegaard en
`DashboardState.kt:44-45`). No es un descuido: es el *fallback* del default de
`DashboardAnchorPhraseState` porque **el catálogo de frases está vacío** y el selector
actual (`DashboardProjection.selectAnchorPhrase`) es un **stub** que agarra la primera
frase por `sortOrder` e ignora fase, estado, pesos, impresiones y la ventana de 7 días.

El andamiaje de datos **ya existe y está ~70% hecho**: las 5 entidades Room
(`anchor_phrases`, `anchor_phrase_state_rules`, `anchor_phrase_phase_rules`,
`anchor_phrase_impressions`, `anchor_phrase_daily_slots`), el DAO completo, el modelo de
dominio `AnchorPhrase` con sus enums (`PhraseFamily`, `AttributionStatus`) y el mapper.
Falta el corazón funcional:

1. **Seed** de las 83 frases canónicas (`frases-ancla.md §13, §15`) + reglas de
   estado/fase derivadas.
2. **Motor de rotación** real: política de fase del día + selector ponderado puro.
3. **Persistencia** de "qué frase se mostró" (slot + impresión) para dar **estabilidad
   dentro de la fase** y **no repetir en 7 días** (`frases-ancla.md §8`).
4. **Integración reactiva** en el dashboard que lea el slot resuelto en vez de inventar
   una cita.

### Por qué ahora

El dashboard es la prioridad #1 vigente (`AGENTS.md`). La frase ancla contextual es una
de las tres tarjetas del dashboard (score-state → progreso diario → **frase ancla**,
`frases-ancla.md §1`). Con el andamiaje ya construido, dejarlo a medias significa enviar
una cita fija que contradice el contrato de producto y el doc vivo.

### Bug latente de release (descubierto en el research)

Las 5 tablas `anchor_phrase*` están en el set de entidades de la **DB v12**, pero
**ninguna migración las crea** (`MIGRATION_11_12` solo crea `sleep_nights`).
Consecuencia:

- **Instalación limpia v12** → Room crea las tablas desde el esquema → funciona (caso de dev).
- **Dispositivo que migró 11→12** → la tabla no existe → **crash de validación de esquema Room** en release.

En dev queda enmascarado por la DB descartable (`CLAUDE.md`), pero para release es un
bug. Esta propuesta lo trata como tarea explícita y bloqueante (no se puede liberar la
feature sin la migración).

### Cómo se ve el éxito

- El dashboard muestra una frase **elegida por estado del score + fase del día**, con
  cita y autor, estable dentro de la fase y sin repetir las mostradas en 7 días.
- `contemplacion` aparece solo en `Plenitude`/`Unbreakable` y más en `Unbreakable`
  (recompensa sutil, sin explicación en UI — `frases-ancla.md §6, §8.6`).
- Cero cita hardcodeada en `DashboardState.kt`.
- Migración de las 5 tablas presente y cubierta con `MigrationTestHelper`.
- `docs/dominio/frases-ancla.md` deja de contradecir al código (doc vivo al día).

---

## 2. Alcance

### Dentro de alcance

- **Seed canónico** de las 83 frases (`AnchorPhraseSeed.kt`) + reglas de estado y fase
  **derivadas** de mapas familia→peso (no escritas a mano), cableado en `ensureSeeded`.
- **Política pura de fase del día** (`DayPhasePolicy`): `Dawn` 05:00–14:59 / `Dusk`
  15:00–04:59 local (`frases-ancla.md §7`).
- **Selector puro ponderado** (`AnchorPhraseSelector`): determinístico, sin Room, sin
  `suspend`; aplica filtros de estado, gating de contemplación, ventana de 7 días con
  relajación, y elección ponderada reproducible con `Random(seed)`.
- **Coordinador de capa de datos** (`AnchorPhraseResolver`, espejo de
  `WeeklyScoreSnapshotWriter`): lee slot/impresiones/catálogo, llama al selector puro,
  persiste slot + impresión en `@Transaction`.
- **Corrección del bug de migración**: migración que crea las 5 tablas `anchor_phrase*`
  con índices `index_<tabla>_<col>` + cobertura `MigrationTestHelper`.
- **Integración dashboard**: query DAO reactiva del slot, flow en `DashboardRepository`,
  combinación en `DashboardViewModel`, lookup puro en `DashboardProjection`, y remoción
  del default hardcodeado de `DashboardState.kt`.
- **Enums/modelos de dominio faltantes**: `DayPhase`, `AnchorPhraseSelection`,
  `AnchorPhraseStateRule`, `AnchorPhrasePhaseRule` + mappers.
- **Actualización del doc vivo** `docs/dominio/frases-ancla.md` (§17/§18: marcar implementado).

### Fuera de alcance (`frases-ancla.md §17`)

- Editor de frases propias / mensajes propios de la app como citas.
- Personalización manual de familias.
- Explicación visible de desbloqueos en la UI.
- Rotación por **cada apertura** de la app (la rotación base es por fase del día).
- Geolocalización para amanecer/atardecer real (las ventanas son funcionales, no astronómicas).
- Algoritmo complejo por capa más baja, riesgo o sobriedad.

---

## 3. Enfoque (resumen)

**Manera A — con persistencia.** El 99% de la app es read-only (hechos → dominio →
Compose) y el estado del score **no se cachea**: se recalcula reactivo desde la fuente de
verdad. Las frases son la **excepción justificada** por dos requisitos de producto
(`frases-ancla.md §8`): estabilidad dentro de la fase y no-repetición en 7 días. Para
cumplirlos la app debe **acordarse de qué frase mostró** — y esa anotación es un
**hecho** ("el 2026-06-04, fase Dawn, se mostró `phrase_X`"), no un caché de estado
derivado. Guardarlo en Room **respeta** la arquitectura, no la viola.

**Patrón gemelo ya probado en el repo:** el `AnchorPhraseResolver` es a la "libreta" lo
que `WeeklyScoreSnapshotWriter` es a la historia semanal — escribe un hecho derivado en
la capa de datos durante el mantenimiento diario, y el dashboard lo **lee reactivo**.

**Principio de diseño — engranajes, no monolitos.** Cada pieza hace una sola cosa, vive
en su archivo y se compone desde quien la necesita. Es la convención **ya establecida**:
el scoring son 13 policies atómicas (`domain/scoring/*Policy.kt`) que `ScoreEngine` solo
orquesta. Las frases siguen el mismo molde: `DayPhasePolicy`, `AnchorPhraseSelector`
(compuesto de funciones puras chiquitas, una por regla), `AnchorPhraseResolver`,
`AnchorPhraseSeed`, lookup de proyección, `AnchorPhraseCard`. Beneficio: cambiar solo los
pesos toca el seed; cambiar solo las franjas horarias toca `DayPhasePolicy`. Cero efecto
dominó.

**Reparto de responsabilidades (dominio puro intacto):**

| Capa | Componente | Toca Room |
|---|---|---|
| Dominio puro | `DayPhasePolicy` (hora → fase) | No |
| Dominio puro | `AnchorPhraseSelector` (inputs → frase, determinístico) | No |
| Datos (coordinador) | `AnchorPhraseResolver` (leer/elegir/persistir) | Sí (única autorizada) |
| UI (proyección pura) | `DashboardProjection` (slot → estado de UI, lookup) | No |
| UI (Compose) | `AnchorPhraseCard` (cita + autor, sin cambios) | No |

**Seed en Kotlin (no JSON):** consistente con `DefaultSeeds` (que ya hardcodea layers,
actividades, tracks), seguro en compilación, y son **data canónica respaldada por doc**,
no editable por el usuario. Estructurado para NO ser un muro: helper `phrase(...)`
one-liner para las 83 frases, y reglas **generadas** desde dos mapas chicos
(`stateWeights`, `phaseWeights`) porque los pesos de `§9` son por familia, no por frase.
Es **data predeterminada NO descartable** (`CLAUDE.md`).

**Fuente del `scoreState` del resolver:** lee `WeeklyScoreSnapshotEntity.state` de la
semana actual, ya refrescado por `refreshCurrentWeeklyScoreSnapshot` justo antes en
`runDailyMaintenance`. **No recalcula** desde los hechos.

**Mejores prácticas (Context7):** `@Insert(onConflict = REPLACE)` + `suspend` para
seed/slot idempotentes; `@Transaction` para slot+impresión atómicos; observar queries
como `Flow` y derivar UiState sin almacenar estado derivado; `collectAsStateWithLifecycle`
(ya en uso); `Random(seed)` para aleatoriedad reproducible y testeable.

---

## 4. Estrategia de entrega (chained PRs)

El cambio **supera holgadamente las 400 líneas** (solo el seed de 83 frases + reglas
derivadas ya las supera). Se entrega en **PRs encadenados/apilados** con un presupuesto
de revisión de ~400 líneas por PR. Commits por **unidad de trabajo** (test + código + doc
juntos), no por tipo de archivo. El orden respeta dependencias:

1. Enums + modelos de dominio + `DayPhasePolicy` (+ tests).
2. **Migración de las 5 tablas** + cobertura `MigrationTestHelper` (crítico/bloqueante).
3. Seed aislado `AnchorPhraseSeed.kt` + wiring `ensureSeeded` (PR grande pero aislado).
4. Selector puro ponderado (+ tests).
5. Resolver coordinador + queries DAO nuevas + wiring mantenimiento/`onResumed`.
6. Integración dashboard (flow del slot + lookup + quitar hardcode).
7. Doc viva.

Strict TDD activo (`testDebugUnitTest`): test primero, luego implementación.

---

## 5. Áreas afectadas

| Archivo | Cambio |
|---|---|
| `Models.kt` | Nuevos `DayPhase`, `AnchorPhraseSelection`, `AnchorPhraseStateRule`, `AnchorPhrasePhaseRule` |
| `domain/phrase/DayPhasePolicy.kt` | Nuevo (puro) |
| `domain/phrase/AnchorPhraseSelector.kt` | Nuevo (puro, determinístico) |
| `data/phrase/AnchorPhraseResolver.kt` | Nuevo (coordinador capa de datos) |
| `data/local/seed/AnchorPhraseSeed.kt` | Nuevo (83 frases + reglas derivadas) |
| `data/local/seed/DefaultSeeds.kt` | Referencia al seed de frases |
| `data/AutonomiaDatabase.kt` | Nueva migración que crea las 5 tablas |
| `data/AutonomiaDao.kt` | Queries nuevas: impresiones en rango 7d, observar slots del día |
| `data/local/mapper/DomainMappers.kt` | Mappers de las nuevas entidades/reglas |
| `AutonomiaRepository.kt` | Wiring DI del resolver, `resolveAnchorPhraseForToday`, `ensureSeeded` |
| `ui/dashboard/DashboardRepository.kt` | `anchorPhraseSlotFlow(dateKey)` |
| `ui/dashboard/DashboardViewModel.kt` | Combinar slot; llamar resolver tras refrescar snapshot; extender `onResumed` |
| `ui/dashboard/DashboardProjection.kt` | Reemplazar `selectAnchorPhrase` stub por lookup puro |
| `ui/dashboard/DashboardState.kt` | Quitar default Kierkegaard; default neutro/vacío |
| `docs/dominio/frases-ancla.md` | Doc vivo: marcar implementado |
| Tests | `DayPhasePolicyTest`, migración, `DefaultSeedsAnchorPhraseTest`, `AnchorPhraseSelectorTest`, `AnchorPhraseResolverTest`, `DashboardProjectionTest` |

---

## 6. Riesgos / decisiones abiertas

1. **Bug de migración (bloqueante para release):** sin la migración que crea las 5
   tablas, cualquier dispositivo que migró 11→12 crashea al validar el esquema. La
   migración debe usar índices con nombre `index_<tabla>_<col>` (regla de `CLAUDE.md`)
   para coincidir con los que Room genera desde `@Index`. Riesgo mitigado con cobertura
   `MigrationTestHelper`.
2. **Supuesto a confirmar (slice 5):** que el `state` del snapshot de la semana actual
   (que lee el resolver) **coincide** con el estado que muestra el dashboard. Ambos salen
   de `ScoreEngine`, así que deberían igualar; se verifica al integrar.
3. **"Cambio significativo de estado" dentro de la fase (`§8.3`):** v1 = re-elegir si
   `slot.scoreState != currentState`. Política simple, suficiente para MVP.
4. **Cruce de fase con app abierta:** `onResumed` cubre el caso al volver del background;
   el cruce de las 15:00 con la app en primer plano se resuelve en la próxima apertura
   (aceptable v1).
5. **Tamaño del cambio:** excede 400 líneas → requiere PRs encadenados; sin eso, la
   revisión se vuelve inmanejable. Mitigado por el aislamiento del seed en su propio PR.

---

## 7. Compuerta de spec

Antes de codificar aplica `meta/guias/contrato-de-spec.md §9`. La spec de producto
(`docs/dominio/frases-ancla.md`) ya está completa y es la fuente de verdad; las fases
`sdd-spec` y `sdd-design` formalizan este plan dentro del flujo SDD.
