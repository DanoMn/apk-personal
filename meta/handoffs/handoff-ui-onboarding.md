# Handoff — UI + onboarding/tutorial de introducción

> Para arrancar en **sesión nueva** sin contaminar contexto. Cargar este doc +
> recuperar memoria de Engram (proyecto `apk-personal`) por "cobertura scoring",
> "gate", "onboarding".
>
> Fecha: 2026-06-02 · Proyecto: apk-personal (Vocal) · Rama base: `main`

---

> **🔄 Actualización 2026-06-02 (diseño conceptual cerrado).** El diseño del
> onboarding se resolvió en una sesión posterior. La fuente autoritativa ahora es
> **`meta/instructions/2026-06-02-onboarding-introduccion-diseno.md`** (10 decisiones +
> copy v3 + mapa de bloques + referencias técnicas). Este handoff se conserva como
> registro de arranque, pero **donde diga algo distinto al doc de captura, manda el doc
> de captura**. Correcciones clave: (a) las "Decisiones pendientes" §17 de más abajo
> quedaron resueltas (ver tabla del doc de captura §5); (b) el sueño **NO se registra a
> mano** — la sección de notificaciones de abajo estaba equivocada y se corrige inline;
> (c) el nombre de la app pasa a ser **"Autonomía sin límites"** (retiro de "Vocal":
> tarea aparte).

---

## Objetivo de la sesión

El motor de scoring quedó **completo y blindado** (ver
`meta/handoffs/handoff-cobertura-tests-scoring.md` + `meta/plan-cobertura-tests-scoring.md`,
todo en OK). Lo que sigue es **UI**, y dentro de la UI hay dos clases de trabajo:

1. **Dos compuertas heredadas del scoring** que la UI DEBE hacer cumplir (el motor
   ya las exige; hoy la UI no las garantiza).
2. **El tutorial / onboarding de introducción** — pieza nueva, grande, y **todavía
   sin diseñar**. Es el foco principal de este handoff.

> ⚠ **Conceptos antes que código.** El tutorial NO se empieza a programar hasta
> resolver las decisiones de producto de la sección "Decisiones pendientes". Hay
> preguntas abiertas selladas en el doc de frontend (§17) desde hace tiempo. Codear
> un wizard sin esas respuestas = retrabajo asegurado. Aplica el contrato de spec
> (`meta/guias/contrato-de-spec.md`) antes de lanzar SDD.

## Estado actual del UI (verificado en código, 2026-06-02)

**Navegación:** single-activity manual, SIN librería de navegación.
`MainActivity.kt` tiene un `var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }`
y un `when (currentScreen)` que conmuta pantallas. El enum `AppScreen` es **privado**
al final de `MainActivity.kt` (`Dashboard, Scoring, AnchorConfig, Supports, Tasks,
Sobriety, SleepConfig`).

**La app SIEMPRE arranca en `Dashboard`.** NO existe ningún gate de primer-uso,
flag de "onboarding completado", ni pantalla de bienvenida. Cero código de
onboarding/tutorial en el repo (verificado: los matches de "intro" son falsos
positivos de `introduce`/migración).

**Pantallas que YA existen** (todas en `app/src/main/java/.../ui/`):
- `dashboard/DashboardScreen.kt` — pantalla principal (la primera y única de entrada hoy).
- `anchors/AnchorConfigScreen.kt` (+ `AnchorEditorForm`, `GoalPreset*`, `TimeWheelPicker`) — configurar anclas.
- `supports/SupportsConfigScreen.kt` — configurar soportes.
- `sleep/SleepConfigScreen.kt` — configurar ventana de sueño.
- `sobriety/SobrietyConfigScreen.kt` — configurar tracks de sobriedad.
- `tasks/TasksScreen.kt` — pendientes.
- `scoring/ScoringScreen.kt` — detalle "Estado Base".

**Conclusión:** existen las pantallas de **configuración** de cada feature, pero NO
hay un flujo que las **orqueste** para un usuario nuevo, ni que las haga obligatorias.
El onboarding/tutorial es construcción nueva que se apoya en estas pantallas.

**Origen del "default heredado":** hoy la DB se siembra desde
`data/local/seed/DefaultSeeds.kt` (capas/anclas/soportes predeterminados). Por eso un
usuario nuevo ya tiene config "de fábrica" — eso es lo que las compuertas de abajo
quieren reemplazar por una **elección activa**. (El seed NO se borra — es data
predeterminada, regla de `CLAUDE.md`/`AGENTS.md` #21.)

## Compuertas heredadas del scoring (HARD requirements de la UI)

Estas dos NO son opинión de diseño: el motor ya las exige y sin la UI correspondiente
el usuario queda en un estado inconsistente.

### 1. Onboarding obligatorio de 3 capas con ancla (§7.4)

El scoring exige **mínimo 3 capas activas con ≥1 ancla cada una**
(`ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3`). Con menos → el motor devuelve
`NoData` ("Sin datos"). Hoy esto se cumple solo por el seed heredado; si el usuario
desconfigura, cae a `NoData` sin que nada se lo haya explicado.

- **Lo que falta (UI):** el onboarding debe **obligar** a configurar 3 capas con ancla
  antes de soltar al usuario al dashboard, o comunicar con claridad por qué el estado
  dice "Sin datos" y guiarlo a completarlo.
- Esto **responde** la pregunta abierta #2 ("¿cuántas anclas mínimas?") y #10 ("¿qué
  mínimo para salir de Sin datos?") del doc de frontend §17.

### 2. Onboarding obligatorio de la ventana de sueño (sin default silencioso)

El sueño es **CORE, no opt-in** (decisión sellada). Sin registro de noches, el estado
se **topea en "En marcha" (Motion)** (§16.7). Hoy `SleepPolicy.defaultConfig()` hereda
una ventana silenciosa **23:30→07:30 / 480 min** (`domain/sleep/SleepPolicy.kt:13-17`).

- **Lo que falta (UI):** el usuario debe **SELECCIONAR activamente** su ventana de
  sueño en el onboarding, sin caer en el default heredado. Objetivo: una **ventana
  horaria** (`targetSleepAt`/`targetWakeAt`), no "horas sueltas". Mínimo 5h
  (`MIN_SLEEP_WINDOW_MINUTES = 300`).
- **Notificaciones (se implementan JUNTO con esta feature):**
  - ❌ ~~Recordatorio para no olvidarse de **registrar** el sueño.~~
  - ❌ ~~Aviso de **días seguidos sin registrar** (racha de no-registro).~~
  - > **Corregido (2026-06-02):** el sueño se detecta por **telemetría**; NO hay
    > registro nocturno que recordar (contradecía `docs/sueno/decisiones-diseno-sueno-v1.md`
    > §2). Modelo final en el doc de captura §2.8: **B ·** aviso de sueño *sin datos /
    > permiso* (informativa, encendida por defecto) **+ A ·** recordatorio de *hora de
    > dormir* (consentido en el onboarding). `POST_NOTIFICATIONS` se pide perezoso.
- Detalle en `meta/pendientes.md` → "Sueño / configuración".

## El tutorial de introducción (la pieza grande — sin diseñar aún)

Esto es lo que el dueño marcó como "bastante trabajo". NO está diseñado. Antes de
codear hay que cerrar el **qué** y el **cómo se siente**, no el **con qué Composable**.

**Restricciones de diseño NO negociables** (vienen del producto, no del programador):
- **NO debe parecer un wizard frío.** `docs/frontend/mis-anclas-ux-canon-v1.md:26`:
  "La pantalla debe sentirse concreta, sobria y táctil. No debe parecer un wizard."
- **Tono** (`docs/producto/tono-comunicacion.md` + `AGENTS.md`): adulto funcional y
  compasivo. No humilla, no diagnostica, no moraliza. Nada de "configurá todo ahora o
  no funciona".
- **Estilo visual** (`docs/frontend/frontend-design.md`): base oscura orgánica,
  cartón/beige, coral mate. Tipografía editorial. Nada cyberpunk/neón/terminal.
- **Filosofía** (`docs/frontend/frontend-design.md:196`): evitar "lista larga de
  tareas como primera experiencia". El onboarding debe bajar fricción, no abrumar.

**Lo que el tutorial probablemente tiene que cubrir** (a confirmar al diseñar):
- Bienvenida + qué es Vocal (anclas, soportes, capas, el estado base) en lenguaje humano.
- Elegir/confirmar 3 capas con ancla (compuerta #1).
- Elegir ventana de sueño (compuerta #2).
- Decidir si abstinencias/sobriedad se ofrecen acá o después (decisión pendiente #4).
- Explicar el estado y por qué el sueño pesa, sin sonar controlador (#9).
- Persistir un flag "onboarding completado" para no repetirlo (hoy NO existe — hay
  que crearlo; probablemente en prefs vía `AutonomiaRepository`, donde ya viven
  `prefs.edit { }` para tema/automode).

## Decisiones de producto pendientes (resolver ANTES de codear)

Del doc de frontend `vocal_mapa_componentes_v_0_2_borrador.md` §17 "Preguntas
abiertas". Algunas ya quedaron respondidas por el trabajo de scoring; el resto son
decisión del dueño y bloquean el diseño del tutorial:

| # | Pregunta | Estado |
|---|----------|--------|
| 1 | ¿Sueño obligatorio desde onboarding o después del primer día? | **Tiende a obligatorio** (sueño es CORE; ver compuerta #2) — confirmar el momento exacto. |
| 2 | ¿Cuántas anclas mínimas para empezar? | **RESUELTA**: 3 capas con ≥1 ancla (§7.4). |
| 3 | ¿Soportes desde el inicio o se sugiere en restauración? | **ABIERTA** — decisión del dueño. |
| 4 | ¿Abstinencias en onboarding o se ofrecen después con cuidado? | **ABIERTA** — sensible (gente en recuperación). |
| 5 | ¿El usuario elige "tipo de uso" o solo config progresiva? | **ABIERTA**. |
| 6 | ¿ActivityTarget al crear ancla o tras unos días de uso? | **ABIERTA**. |
| 8 | ¿Cómo se evita que usuarios de recuperación se sientan castigados? | **ABIERTA** — afecta tono del onboarding de sobriedad. |
| 9 | ¿Cómo se comunica que el sueño pesa mucho sin sonar controlador? | **ABIERTA** — afecta copy del paso de sueño. |
| 10 | ¿Mínimo de datos para salir de "Sin datos"? | **RESUELTA**: 3 capas con ancla (§7.4). |

> Recomendación: abrir estas decisiones con el dueño en una pasada de
> meta-prompting (`meta/instructions/`), cerrarlas, y recién ahí escribir la spec del
> onboarding contra `meta/guias/contrato-de-spec.md`.

## Archivos relevantes

**Entrada / navegación:**
- `MainActivity.kt` — switch de pantallas + `enum AppScreen` privado. Acá engancha el
  gate de onboarding (decidir primera pantalla según flag "completado").
- `app/AppGraph.kt` — wiring/DI.
- `AutonomiaRepository.kt` — todo el acceso a Room + prefs (`prefs.edit { }`); acá iría
  el flag de onboarding completado.

**Pantallas de config a orquestar:**
- `ui/anchors/AnchorConfigScreen.kt`, `ui/sleep/SleepConfigScreen.kt`,
  `ui/supports/SupportsConfigScreen.kt`, `ui/sobriety/SobrietyConfigScreen.kt`.

**Contrato de producto/diseño (leer antes de diseñar):**
- `docs/frontend/frontend-design.md` — estilo visual + "no lista larga como primera experiencia".
- `docs/frontend/mis-anclas-ux-canon-v1.md` — "no debe parecer un wizard".
- `docs/frontend/vocal_mapa_componentes_v_0_2_borrador.md` §17 — preguntas abiertas.
- `docs/producto/tono-comunicacion.md` — tono.
- `docs/producto/nucleo-dominio-autonomia.md:796` — "Crear o pulir onboarding/configuración inicial".

**Reglas de scoring que el onboarding hace cumplir:**
- `domain/scoring/ScoringConstants.kt:23` — `MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3` (§7.4).
- `domain/sleep/SleepPolicy.kt:13-17` — default heredado de sueño a reemplazar.
- `docs/scoring/arbol-scoring-vocal-v1.md` §7.4 y §16.7.

**Backlog / proceso:**
- `meta/pendientes.md` → "Sueño / configuración" (onboarding sueño + notificaciones).
- `meta/guias/contrato-de-spec.md` — compuerta obligatoria antes de SDD.
- `meta/guias/verificacion-por-capas.md` — gates obligatorios (ojo: el onboarding ES UI,
  así que NO se saltean las capas de emulador/logs como en dominio puro).

## Cómo retomar

1. Cargar este handoff + `mem_search` en Engram (proyecto `apk-personal`) por
   "onboarding" / "gate" / "cobertura scoring".
2. **Primero conceptos:** abrir con el dueño las decisiones pendientes (§17, tabla de
   arriba). Cerrar al menos #1, #3, #4, #5, #8, #9. Registrarlas en
   `meta/instructions/` (meta-prompting) y en Engram.
3. Escribir la spec del onboarding contra `meta/guias/contrato-de-spec.md` y pasar su
   compuerta (sección 9) ANTES de lanzar SDD.
4. **Recién entonces** implementar, en orden sugerido:
   a. Flag "onboarding completado" en prefs (`AutonomiaRepository`) + gate de primera
      pantalla en `MainActivity` (`AppScreen.Onboarding`).
   b. Flujo que haga cumplir la compuerta #1 (3 capas con ancla).
   c. Flujo que haga cumplir la compuerta #2 (ventana de sueño sin default) +
      notificaciones de registro.
   d. Copy/tono del tutorial y los pasos opcionales (soportes, sobriedad) según las
      decisiones cerradas.
5. Verificación: esto es UI → corren TODAS las capas (build, lint, app-arranca en
   emulador, logs, casos límite). Usar `scripts/dev/dev.sh` (ver
   `meta/guias/entorno-verificacion.md`). NO alcanza con tests de dominio.
