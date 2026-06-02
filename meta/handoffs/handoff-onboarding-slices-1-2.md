# Handoff — Onboarding de introducción: slices 1-2 hechos (static), faltan verificación + slices 3-5

> Para arrancar en **sesión nueva** sin contaminar contexto. Cargar este doc +
> recuperar memoria de Engram (proyecto `apk-personal`) por:
> "onboarding", "sdd/onboarding-introduccion/apply-progress", "onboarding/espiritu".
>
> Fecha: 2026-06-02 · Proyecto: apk-personal (app "Autonomía sin límites") · Rama base: `main`
> Antecesor: `meta/handoffs/handoff-ui-onboarding.md` (corregido inline; este lo continúa).

---

## TL;DR — dónde estamos

El onboarding de introducción está **diseñado por completo** (10 decisiones + copy) y
**en implementación por slices encadenados**. Hechos hasta acá:

- **Slice 1 (gate + estado + esqueleto): IMPLEMENTADO, static-green.**
- **Slice 2 (Bloque Anclas): IMPLEMENTADO, static-green.**
- **NINGUNA UI verificada en runtime** — el emulador no estaba disponible en la sesión.
- **Slices 3, 4, 5: NO empezados.**
- **Nada commiteado** (working tree con los cambios sin commit).

"static-green" = `assembleDebug` + `lintDebug` + `testDebugUnitTest` en verde, sin
regresiones. Por contrato (`meta/guias/verificacion-por-capas.md`), como es **UI**, NO
está "terminado" hasta correr las capas de emulador.

---

## 1. Lo PRIMERO al retomar: verificar en emulador (deuda bloqueante)

Esto es lo más urgente. Sin esto, slices 1-2 NO están "terminados".

```bash
scripts/dev/dev.sh run -clean    # build + emu + install limpio + launch + logs
scripts/dev/dev.sh shot home     # screenshot
scripts/dev/dev.sh logcat 300    # crashes
```

> ⚠ En la sesión anterior `scripts/dev/dev.sh doctor` quedó **colgado (0 bytes, sin
> bootear AVD)**. Si vuelve a pasar, revisar el entorno (AVD creado, `_bootstrap-avd.ps1`,
> emulador del lado Windows) antes de seguir.

**Qué confirmar — Slice 1 (`onboarding-gate`):**
- Install limpio → la app abre en el **Bloque 0 (Bienvenida)**, no en Dashboard.
- Avanzar, **matar la app**, reabrir → **reanuda** en el bloque donde quedó.
- Completar (Cierre → "Entrar") → **Dashboard**; relanzar → no repite el onboarding.

**Qué confirmar — Slice 2 (`onboarding-anchors`):**
- En el Bloque Anclas: **agregar** del catálogo y **crear propia** (nombre + capa).
- "Continuar" queda **deshabilitado** hasta cubrir **3 capas distintas** con ancla.
- **Quitar** un ancla baja la cobertura y vuelve a deshabilitar.
- Al avanzar, las anclas **persisten** (no se pierden). Logcat sin crashes.

Si algo falla, el dominio puro ya está testeado (la lógica del gate/regla es correcta);
los bugs probables serían de wiring/Compose (estado, recomposición, IDs).

---

## 2. Lo que se implementó (mapa de archivos)

**Dominio puro (testeado, JVM):**
- `domain/onboarding/OnboardingStep.kt` — enum 6 bloques (Welcome, Intention, Anchors, Sleep, Sobriety, Closing); orden = secuencia.
- `domain/onboarding/OnboardingState.kt` — `OnboardingState` + `OnboardingFlow` (resolve/next/previous/shouldStartOnboarding). Persiste paso por NOMBRE; inválido → Welcome.
- `domain/onboarding/OnboardingAnchorsRule.kt` — `canAdvance`/`distinctLayersWithAnchor`/`minLayers`; referencia `ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR` (no duplica el 3).
- `domain/activity/AnchorTargets.kt` — `DEFAULT_ANCHOR_SESSION_MINUTES = 10` (agregado).
- Tests: `test/.../onboarding/OnboardingFlowTest.kt` (11) + `OnboardingAnchorsRuleTest.kt` (5) — verdes.

**Datos:**
- `AutonomiaRepository.kt` — clave `onboarding_current_step` (flow + setter) para reanudación.
  **REUSA** el flag huérfano `initial_configuration_complete` (`isInitialConfigurationCompleteFlow`/
  `setInitialConfigurationComplete`) como "onboarding completado". Sin Room/migración.

**UI:**
- `ui/onboarding/OnboardingViewModel.kt` — combina los 2 flows → `OnboardingFlow.resolve`, `stateIn(Eagerly)`; `advance/back/complete`; `Factory(context)`.
- `ui/onboarding/OnboardingScreen.kt` — esqueleto; Bloque 0 (Bienvenida) y 4 (Cierre) con copy canónico ("Autonomía sin límites"); branch Anclas → `OnboardingAnchorsStep`; `OnboardingPrimaryButton` (internal, con `enabled`). Intention/Sleep/Sobriety = placeholders.
- `ui/onboarding/OnboardingAnchorsStep.kt` — picker por capa (catálogo + quitar) + crear propia (nombre + capa) + "Continuar" gated por `OnboardingAnchorsRule`.
- `MainActivity.kt` — `AppScreen.Onboarding` + gate (siembra `currentScreen` del valor síncrono de `onboardingState`, sin flicker); pasa `dashboardState.layers`/`activityOptions` + callbacks (`addActivityAsAnchor`/`createActivity`/`removeActivityAsAnchor`) con defaults al Bloque Anclas.

**Decisión de wiring (slice 2):** el Bloque Anclas **reusa `DashboardViewModel`** (datos +
métodos) vía `MainActivity`, NO duplica el pipeline en `OnboardingViewModel`. Y NO reusa
`AnchorConfigScreen` (esa pide targets en su UI, contradice "solo elegir").

---

## 3. Lo que falta (slices 3-5)

Todos montan sobre el esqueleto de `OnboardingScreen` (reemplazan su placeholder). Diseño
ya cerrado en el doc de captura (ver §5).

- **Slice 3 — Bloque Sueño.** Ventana objetivo (`targetSleepAt`+`targetWakeAt`, mín 5h, duración derivada) + ofrecer **permiso de telemetría** (UsageStats) explicado y **salteable** + preguntar consentimiento del recordatorio wind-down. `digitalWindDown` NO va. Reusa `SleepPolicy`/`SleepConfigScreen` (AutoModeCard/PermissionStep). El usuario NO registra el sueño a mano (telemetría).
- **Slice 4 — Bloque Sobriedad + Bloque 0.5 Intención.** Intención suave ("¿Qué te trae aquí?": 2 rutas — estándar / "cuidarme de algo que me cuesta") **persistida en prefs**; ramifica si aparece el Bloque Sobriedad. Sobriedad reusa `AbstinenceTrackEntity`/`SobrietyConfigScreen`. Tono sin culpa (#8).
- **Slice 5 — Notificaciones.** B (sueño sin-datos / permiso, ON por defecto) + A (wind-down, según lo consentido en slice 3) + permiso `POST_NOTIFICATIONS` perezoso. Reformula el "registro" del handoff viejo (no hay registro nocturno).

**Compuerta del motor #2 (sueño):** la imposición de "ventana de sueño elegida" entra con
el slice 3 (igual que la #1 de anclas entró con el slice 2).

---

## 4. Tareas aparte (no del onboarding, no bloquean)

- **Retirar "Vocal" del repo** → "Autonomía sin límites". ~190 ocurrencias en 61 `.md`; solo 1 en `.kt`. **Landmines:** NO tocar docs congelados (`docs/old/`, `meta/handoffs/`, `docs/auditorias/`); renombrar archivos rompe `@references`; la clave de proyecto en engram sigue siendo `apk-personal`. (Memoria engram: `naming/retirar-vocal`.)
- **Documentar formalmente `docs/producto/Definicion_anclas.md`** (hoy filename con mayúscula/guiones bajos, sin header `> Estado: vivo`).
- **Refrescar el cache `sdd-init/apk-personal` en engram** (no aparece en búsquedas; el proyecto está inicializado igual — openspec activo).
- **Commitear** lo de slices 1-2 (nada commiteado aún). Sugerido: un commit por slice tras verificar en emulador. Conventional commits, sin atribución IA (regla AGENTS.md).

---

## 5. Contexto de diseño (la fuente de verdad)

- **Doc de captura (LEER PRIMERO):** `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` — 10 decisiones, copy v3 canónico de los 5 bloques, mapa de bloques, referencias técnicas.
- **Artefactos SDD:** `openspec/changes/onboarding-introduccion/` — `proposal.md`, `design.md`, `specs/onboarding-gate/spec.md`, `specs/onboarding-anchors/spec.md`, `tasks.md` (slice 1), `tasks-slice2.md`.
- **Memorias engram** (`apk-personal`): `onboarding/espiritu`, `onboarding/configuraciones-y-modulos`, `onboarding/paso-sueno`, `onboarding/notificaciones-sueno`, `onboarding/registro-literario-copy`, `onboarding/flag-existente-reuso`, `sdd/onboarding-introduccion/apply-progress`.
- **Contrato/tono:** `docs/producto/tono-comunicacion.md`, `docs/producto/Definicion_anclas.md` (capas, Borgesiano), `docs/sueno/decisiones-diseno-sueno-v1.md` (modelo telemetría), `docs/frontend/vocal_mapa_componentes_v_0_2_borrador.md` §17.

**Recordatorios de tono para el copy de los slices 3-5:** español NEUTRO (no voseo);
"Cuidador Lúcido" + registro literario; app = "Autonomía sin límites"; sin culpa/diagnóstico.

---

## 6. Cómo retomar (orden sugerido)

1. Cargar este handoff + `mem_search` en engram por "onboarding" / "apply-progress".
2. **Verificar slices 1-2 en emulador** (§1). Arreglar bugs de wiring si aparecen.
3. Commitear slices 1-2 verificados (un commit por slice).
4. Seguir el ciclo SDD por slice: spec (contra `contrato-de-spec.md`) → design → tasks → apply (Strict TDD) → verify (capas) para **slice 3**, luego 4, luego 5.
5. Cada slice reemplaza su placeholder en `OnboardingScreen` y respeta el budget de 400 líneas.
