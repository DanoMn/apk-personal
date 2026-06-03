# Especificación: onboarding-anchors

Fuente canónica: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` · `docs/scoring/arbol-scoring-vocal-v1.md` §7.4
Cambio origen: `onboarding-introduccion` (slice 2)

## Purpose

El Bloque Anclas del onboarding permite al usuario elegir al menos 3 anclas en al menos
3 capas distintas —del catálogo sembrado o creando propias— y hace cumplir la compuerta
del motor (`MIN_ACTIVE_LAYERS_WITH_ANCHOR`) antes de avanzar. NO configura targets (toman
defaults; se afinan después). Reusa la persistencia de anclas existente; no introduce
modelo de datos nuevo.

---

## Requirements

### Requirement: Anchor Layer Gate

El avance del Bloque Anclas MUST estar bloqueado hasta que existan anclas configuradas en
al menos `OnboardingAnchorsRule.minLayers` capas distintas, donde ese mínimo MUST referenciar
`ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR` (sin duplicar el umbral). La regla es pura
y testeable (`OnboardingAnchorsRule`).

#### Scenario: Anclas en tres capas distintas habilitan avanzar

- GIVEN anclas configuradas con layerIds `["interior", "cuerpo", "conducta"]`
- WHEN se evalúa `OnboardingAnchorsRule.canAdvance`
- THEN devuelve `true`

#### Scenario: Tres anclas en la misma capa no habilitan avanzar

- GIVEN anclas con layerIds `["interior", "interior", "interior"]`
- WHEN se evalúa `canAdvance`
- THEN devuelve `false` (1 capa distinta < mínimo)

#### Scenario: Dos capas distintas no alcanzan

- GIVEN layerIds `["interior", "cuerpo"]`
- WHEN se evalúa `canAdvance`
- THEN devuelve `false`

---

### Requirement: Catálogo o creación propia

El usuario MUST poder agregar anclas eligiendo del catálogo sembrado Y MUST poder crear
anclas propias (nombre + capa) dentro del onboarding. Ambos caminos reusan la persistencia
existente (`addActivityAsAnchor` / `createActivity` del `DashboardViewModel`).

#### Scenario: Crear ancla propia

- GIVEN el usuario escribe un nombre y elige una capa en el formulario de creación
- WHEN confirma "Agregar ancla propia"
- THEN se crea la actividad como ancla (`ActivitySurface.Anchor`) en esa capa
- AND aparece como ancla configurada en su capa

---

### Requirement: Sin targets en el onboarding (defaults)

Al agregar/crear un ancla en el onboarding, NO se piden targets. La persistencia MUST usar
defaults: `DEFAULT_ANCHOR_SESSION_MINUTES`, `DEFAULT_ANCHOR_WEEKLY_FREQUENCY`,
`commitmentDurationMonths = null`. El usuario los afina después desde el Dashboard.

#### Scenario: Ancla del catálogo agregada con defaults

- GIVEN el usuario toca "Agregar" en una actividad del catálogo
- WHEN se persiste
- THEN el ancla queda con frecuencia semanal = `DEFAULT_ANCHOR_WEEKLY_FREQUENCY` y minutos = `DEFAULT_ANCHOR_SESSION_MINUTES`

---

### Requirement: Quitar anclas

El usuario MUST poder quitar un ancla agregada (vuelve a estar disponible). Reusa
`removeActivityAsAnchor`.

#### Scenario: Quitar un ancla baja la cobertura de capas

- GIVEN 3 anclas en 3 capas distintas (avance habilitado)
- WHEN el usuario quita el ancla de una capa que solo tenía esa
- THEN las capas cubiertas bajan a 2 y el avance se deshabilita

---

### Requirement: Sin modelo de datos nuevo

El Bloque Anclas MUST reusar las entidades y métodos existentes (`UserActivityConfig` vía
`addActivityAsAnchor`/`createActivity`/`removeActivityAsAnchor`). MUST NOT introducir
migración Room ni entidad nueva.

#### Scenario: Sin migración

- GIVEN el onboarding-anchors aplicado
- WHEN se inspecciona el esquema Room
- THEN no hay entidad ni migración nueva por el Bloque Anclas

---

## Criterios de aceptación

- **Tests (JVM puro, hecho/verde):** `OnboardingAnchorsRuleTest` — distinct/canAdvance en
  bordes (0, misma capa, 2 capas, 3 capas, repetidas).
- **Static (hecho/verde):** `assembleDebug` + `lintDebug` + `testDebugUnitTest` en verde.
- **Runtime (`verificacion-por-capas.md`):** en el Bloque Anclas, agregar del catálogo y
  crear propia; "Continuar" deshabilitado hasta cubrir 3 capas distintas; quitar baja la
  cobertura; al avanzar persisten las anclas; logcat sin crashes.
