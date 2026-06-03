# Delta: onboarding-gate (slice 4 — ramificación por intención)

Cambio: `onboarding-introduccion` · slice 4
Modifica: `openspec/changes/onboarding-introduccion/specs/onboarding-gate/spec.md`
Fuente canónica: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §2.2, §2.5, §3, §7

---

## MODIFIED Requirements

### Requirement: Block Navigation Skeleton

El onboarding MUST renderizar los bloques en orden (0, 0.5, 1, 2, 3, 4) con avance y
retroceso. A partir del slice 4, la secuencia de navegación MUST ser **dependiente de la
intención persistida**: `OnboardingFlow.next` y `OnboardingFlow.previous` MUST recibir la
intención como parámetro y aplicar las siguientes reglas:

- **Ruta estándar** (`STANDARD`): `next(Sleep)` → `Closing`; `previous(Closing)` → `Sleep`.
  `OnboardingStep.Sobriety` MUST ser omitido completamente de la secuencia.
- **Ruta sobriedad** (`PROTECTION`): `next(Sleep)` → `Sobriety`; `next(Sobriety)` →
  `Closing`; `previous(Closing)` → `Sobriety`; `previous(Sobriety)` → `Sleep`.
- **Sin intención persistida** (usuario aún en Bloque 0.5 o antes): `next/previous` de
  los pasos anteriores a `Sobriety` MUST NO depender de la intención — la ramificación
  solo aplica desde `Sleep` en adelante.

La lógica de ramificación es dominio puro: MUST vivir en `OnboardingFlow`, MUST NOT filtrarse
a Compose ni al ViewModel.

(Previously: `next/previous` recorrían el enum en orden lineal sin ramificación; `Sobriety`
era siempre un paso navegable como placeholder — slice 1.)

#### Scenario: Avance entre bloques persiste el progreso

- GIVEN el usuario está en el Bloque 0
- WHEN avanza al siguiente bloque
- THEN `currentStep` se actualiza y persiste al bloque siguiente

#### Scenario: Ruta estándar — Sleep avanza a Closing

- GIVEN la intención persistida es `STANDARD`
- WHEN `OnboardingFlow.next(Sleep, intention=STANDARD)` es evaluado
- THEN retorna `Closing`

#### Scenario: Ruta estándar — previous(Closing) retorna Sleep

- GIVEN la intención persistida es `STANDARD`
- WHEN `OnboardingFlow.previous(Closing, intention=STANDARD)` es evaluado
- THEN retorna `Sleep`

#### Scenario: Ruta sobriedad — Sleep avanza a Sobriety

- GIVEN la intención persistida es `PROTECTION`
- WHEN `OnboardingFlow.next(Sleep, intention=PROTECTION)` es evaluado
- THEN retorna `Sobriety`

#### Scenario: Ruta sobriedad — Sobriety avanza a Closing

- GIVEN la intención persistida es `PROTECTION`
- WHEN `OnboardingFlow.next(Sobriety, intention=PROTECTION)` es evaluado
- THEN retorna `Closing`

#### Scenario: Ruta sobriedad — previous(Closing) retorna Sobriety

- GIVEN la intención persistida es `PROTECTION`
- WHEN `OnboardingFlow.previous(Closing, intention=PROTECTION)` es evaluado
- THEN retorna `Sobriety`

#### Scenario: Cambio de intención al volver atrás re-evalúa la ruta

- GIVEN el usuario llegó a `Sobriety` con intención `PROTECTION`
- WHEN vuelve atrás hasta `Intention` y elige `STANDARD`
- AND avanza de `Sleep` en adelante
- THEN `Sobriety` ya no aparece en la secuencia navegable
- AND la pref de intención refleja `STANDARD`

#### Scenario: Reanudación con intención persistida respeta la ruta

- GIVEN el usuario tenía intención `STANDARD` persistida y `currentStep = Sleep`
- AND la app se cerró
- WHEN la app se reabre y reanuda
- THEN `OnboardingFlow.next(Sleep, intention=STANDARD)` retorna `Closing`
- AND el bloque `Sobriety` no aparece al avanzar

#### Scenario: Placeholders intermedios no imponen compuertas

- GIVEN el usuario transita un bloque intermedio (p. ej. Bloque 1)
- WHEN avanza
- THEN el avance no exige haber configurado anclas/sueño

---

## ADDED Requirements

### Requirement: Intention-Aware State

`OnboardingState` MUST exponer la intención persistida de forma accesible a `OnboardingFlow`
para calcular la secuencia correcta. La intención MUST distinguir entre al menos dos
valores: `STANDARD` y `PROTECTION`. Un valor ausente (intención no elegida aún) MUST
tratarse como `STANDARD` a efectos de `next/previous` para pasos previos a `Sobriety`, de
modo que no haya crash si se evalúa el flujo antes de que el usuario pase por el Bloque
Intención.

#### Scenario: Intención ausente no causa crash en next/previous

- GIVEN no hay intención persistida en prefs
- WHEN `OnboardingFlow.next(Welcome, intention=null)` o cualquier paso anterior a Sleep es evaluado
- THEN retorna el paso siguiente correcto sin lanzar excepción

---

## Criterios de aceptación (delta)

- **Tests (JVM puro, TDD primero):** `OnboardingFlow.next/previous` con los cuatro
  escenarios de ramificación (STANDARD y PROTECTION, desde Sleep y Closing) en verde,
  escritos ANTES del wiring. Strict TDD activo.
- **Runtime:** ruta estándar no muestra `Sobriety`; ruta sobriedad muestra `Sobriety`
  entre Sleep y Closing; cambio de intención al retroceder produce la nueva ruta; logcat
  sin crashes.
