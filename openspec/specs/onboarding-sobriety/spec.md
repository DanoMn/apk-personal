# Especificación: onboarding-sobriety

Fuente canónica: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §2.4, §3, §4 (Bloque 3), §7
Cambio origen: `onboarding-introduccion` (slice 4 — Bloque 3 del mapa de bloques)

## Purpose

El Bloque Sobriedad aparece **únicamente** en la ruta sobriedad (intención "Quiero
cuidarme de algo que me cuesta"). Reemplaza el placeholder `OnboardingStep.Sobriety` con
una pantalla opcional que ofrece crear un track de abstinencia inicial, sin culpa y sin
bloquear el avance. "Sí, agregar" crea el track vía `AbstinencePolicy`; "Ahora no" avanza
sin crear nada. Este bloque NO introduce modelo de datos nuevo.

---

## Requirements

### Requirement: Oferta opcional de track de abstinencia

El Bloque Sobriedad MUST mostrar la pregunta "¿Quieres llevar el registro de algo que
estás cuidando?" con dos acciones: "Sí, agregar" y "Ahora no". El avance MUST NOT estar
bloqueado por si el usuario crea o no el track: ambas acciones avanzan al siguiente
bloque. MUST incluir el copy v3 completo del Bloque 3 conforme a
`meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §4, incluyendo los
mensajes de tono ("Una recaída no es un fracaso. Es una señal, no una condena."). El
nombre visible MUST ser **Sobriedad** (canónico — AGENTS.md), nunca "abstinencia" ni el
nombre técnico de la entidad.

#### Scenario: "Sí, agregar" crea el track y avanza

- GIVEN el Bloque Sobriedad está visible (ruta sobriedad activa)
- WHEN el usuario toca "Sí, agregar"
- THEN se muestra un formulario mínimo para nombrar lo que está cuidando
- AND al confirmar se crea un `AbstinenceTrackDraft` vía `AbstinencePolicy.createCustomDraft`
- AND el track se persiste en la tabla existente (`AbstinenceTrackEntity`)
- AND el flujo avanza al siguiente bloque (Closing)

#### Scenario: "Ahora no" avanza sin crear nada

- GIVEN el Bloque Sobriedad está visible
- WHEN el usuario toca "Ahora no"
- THEN no se crea ningún track de abstinencia
- AND el flujo avanza al siguiente bloque (Closing) sin bloqueo

#### Scenario: Track con nombre en blanco es rechazado sin culpa

- GIVEN el formulario de creación de track está visible (tras tocar "Sí, agregar")
- WHEN el usuario intenta confirmar con el campo de nombre vacío o solo espacios
- THEN `AbstinencePolicy.createCustomDraft` retorna `null`
- AND el formulario no avanza ni crea nada
- AND el mensaje de error (si se muestra) es neutral (sin "fallaste" ni diagnóstico)

---

### Requirement: Exclusividad de ruta (solo ruta sobriedad)

El Bloque Sobriedad MUST NOT aparecer cuando la intención persistida es ruta estándar.
Si la secuencia evalúa la ruta estándar, `OnboardingStep.Sobriety` MUST ser omitido de
la navegación: `OnboardingFlow.next(Sleep)` MUST retornar `Closing`; `OnboardingFlow.previous(Closing)` MUST retornar `Sleep`.

#### Scenario: Ruta estándar — Sobriety omitido en next

- GIVEN la intención persistida es ruta estándar
- WHEN `OnboardingFlow.next(Sleep, intention=STANDARD)` es evaluado
- THEN retorna `Closing` (Sobriety se saltea)

#### Scenario: Ruta sobriedad — Sobriety incluido en next

- GIVEN la intención persistida es ruta sobriedad
- WHEN `OnboardingFlow.next(Sleep, intention=PROTECTION)` es evaluado
- THEN retorna `Sobriety`

#### Scenario: Ruta estándar — previous(Closing) retorna Sleep

- GIVEN la intención es ruta estándar
- WHEN `OnboardingFlow.previous(Closing, intention=STANDARD)` es evaluado
- THEN retorna `Sleep` (no `Sobriety`)

#### Scenario: Ruta sobriedad — previous(Closing) retorna Sobriety

- GIVEN la intención es ruta sobriedad
- WHEN `OnboardingFlow.previous(Closing, intention=PROTECTION)` es evaluado
- THEN retorna `Sobriety`

---

### Requirement: Tono sin culpa (Bloque Sobriedad)

El copy del Bloque Sobriedad MUST respetar `docs/producto/tono-comunicacion.md`. MUST NOT
contener palabras como "adicción", "recuperación", "recaída como fracaso", "condena",
lenguaje clínico ni tono policial. El copy MUST incluir el mensaje canónico "Una recaída
no es un fracaso. Es una señal, no una condena." El español MUST ser neutro (no voseo).

#### Scenario: Texto visible sin lenguaje culpabilizador

- GIVEN el Bloque Sobriedad se renderiza
- THEN el texto no contiene "error", "fallaste", "estás mal", "deberías"
- AND el encabezado es "Cuidar algo que te cuesta" (o equivalente canónico v3)
- AND el copy incluye "Una recaída no es un fracaso. Es una señal, no una condena."

---

### Requirement: Sin modelo de datos nuevo

El Bloque Sobriedad MUST reusar `AbstinencePolicy` y la entidad existente
(`AbstinenceTrackEntity`) para crear el track. MUST NOT introducir entidad Room nueva ni
migración. La persistencia del track usa el writer de abstinencia existente (misma tabla).

#### Scenario: Sin migración Room en slice 4

- GIVEN el onboarding-sobriety aplicado
- WHEN se inspecciona el esquema Room y las migraciones
- THEN no hay entidad ni migración nueva por el Bloque Sobriedad

---

## Fuera de alcance (explícito)

- Presets de tracks de abstinencia (`presetTrackIds`): el onboarding usa solo nombre
  personalizado (flujo de creación custom); los presets son accesibles desde el dashboard.
- Modo riesgo (`RiskEventEntity`): no se ofrece ni se menciona.
- Configuración adicional del track (severidad, tier, rol de contribución): toman defaults
  de `AbstinenceTrackDraft` (`Moderate`, `High`, `Protective`); el usuario los ajusta después.
- Re-configuración del track desde el onboarding (el usuario puede modificarlo desde el
  dashboard tras completar el onboarding).
- Notificaciones de sobriedad: fuera del onboarding.

---

## Criterios de aceptación

- **Tests (JVM puro, TDD primero):** (a) `OnboardingFlow.next/previous` con ambas
  intenciones cubren los cuatro escenarios de ramificación; (b) `AbstinencePolicy.createCustomDraft`
  con nombre vacío retorna `null`; (c) con nombre válido retorna draft con defaults esperados.
  Tests escritos ANTES del wiring. Strict TDD activo.
- **Persistencia:** track creado en "Sí, agregar" persiste en `AbstinenceTrackEntity`;
  "Ahora no" no crea ninguna fila.
- **Static:** `assembleDebug` + `lintDebug` + `testDebugUnitTest` en verde.
- **Runtime (`verificacion-por-capas.md`, capas 1-4 aplicables):** en ruta estándar el
  Bloque Sobriedad no aparece (Sleep → Closing directo); en ruta sobriedad aparece tras
  Sleep; "Ahora no" avanza sin crear track; "Sí, agregar" + nombre → track visible desde
  el dashboard al completar; logcat sin crashes.
