# Tasks: onboarding-introduccion · slice 2 (onboarding-anchors)

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas estimadas | ~330 (dominio+tests ~70, UI OnboardingAnchorsStep ~230, wiring ~30) |
| 400-line budget risk | Med |
| Chained PRs | No (slice propio dentro del change chained) |
| Delivery strategy | ask-on-risk |

## Diseño (decisiones)

- D1 — **Regla pura** `OnboardingAnchorsRule` referencia `ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR` (no duplica el umbral).
- D2 — **Sin targets en onboarding**: defaults `DEFAULT_ANCHOR_SESSION_MINUTES` (nuevo, 10) + `DEFAULT_ANCHOR_WEEKLY_FREQUENCY` (3) + commitment null.
- D3 — **Reuso de datos/persistencia**: el Bloque Anclas consume `dashboardState.layers`/`activityOptions` y los métodos `addActivityAsAnchor`/`createActivity`/`removeActivityAsAnchor` del `DashboardViewModel`, cableados desde `MainActivity`. NO se duplica el pipeline de anclas en `OnboardingViewModel`. Sin Room nuevo.
- D4 — **No reusar `AnchorConfigScreen` tal cual**: esa pantalla pide targets en su UI, lo que contradice "solo elegir". Se hace un picker propio simple (`OnboardingAnchorsStep`).

## Phase 1: Dominio (TDD)

- [x] 1.1 `domain/onboarding/OnboardingAnchorsRule.kt` (`distinctLayersWithAnchor`, `canAdvance`, `minLayers`) referenciando `ScoringConstants`.
- [x] 1.2 `domain/activity/AnchorTargets.kt`: `DEFAULT_ANCHOR_SESSION_MINUTES = 10`.

## Phase 2: Tests (RED→GREEN)

- [x] 2.1 `OnboardingAnchorsRuleTest.kt` (5 casos: vacío, misma capa, 2 capas, 3 capas, repetidas) — verdes.

## Phase 3: UI + wiring

- [x] 3.1 `ui/onboarding/OnboardingAnchorsStep.kt`: picker por capa (catálogo + quitar) + crear propia (nombre + capa) + "Continuar" gated por `canAdvance`.
- [x] 3.2 `OnboardingScreen.kt`: parámetros de anclas + branch `Anchors` → `OnboardingAnchorsStep`; `OnboardingPrimaryButton` compartido con `enabled`.
- [x] 3.3 `MainActivity.kt`: pasar `dashboardState.layers`/`activityOptions` + callbacks con defaults (`addActivityAsAnchor`/`createActivity`/`removeActivityAsAnchor`).

## Phase 4: Verificación

- [x] 4.1 Static: `assembleDebug` + `lintDebug` + `testDebugUnitTest` en verde.
- [ ] 4.2 ⏳ PENDIENTE (sin emulador en la sesión) Runtime: agregar del catálogo + crear propia; "Continuar" deshabilitado hasta 3 capas distintas; quitar baja la cobertura; al avanzar persisten; logcat sin crashes.
