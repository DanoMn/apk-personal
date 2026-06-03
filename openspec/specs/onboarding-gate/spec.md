# Especificación: onboarding-gate

Fuente canónica: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` · `docs/producto/tono-comunicacion.md` · `AGENTS.md` (local-first, nombres canónicos)
Cambio origen: `onboarding-introduccion` (slices 1 y 4)

## Purpose

`onboarding-gate` es el contrato de primer-uso: decide la pantalla inicial de la app a
partir del estado de onboarding, persiste el progreso para reanudar y la completitud
para no repetirse, y provee el esqueleto de navegación por bloques con ramificación
dependiente de la intención persistida. NO configura la base del usuario
(anclas/sueño/sobriedad) ni impone las compuertas del motor — eso vive en capabilities
posteriores. Su única responsabilidad es **encaminar y recordar**.

---

## Requirements

### Requirement: First-Run Routing

La pantalla inicial de la app MUST derivarse del estado de onboarding mediante una
función pura del dominio (`initialScreen`), no de lógica en Compose:

- onboarding NO completado → `AppScreen.Onboarding`
- onboarding completado → `AppScreen.Dashboard`

La decisión de ruteo MUST exponerse como estado del dominio/repositorio; Compose
SOLO la renderiza (local-first, `AGENTS.md`).

#### Scenario: Install limpio arranca en Onboarding

- GIVEN no existe estado de onboarding persistido (install nuevo)
- WHEN la app inicia y se resuelve la pantalla inicial
- THEN la pantalla inicial es `AppScreen.Onboarding`
- AND el bloque mostrado es el Bloque 0 (Bienvenida)

#### Scenario: Onboarding completado arranca en Dashboard

- GIVEN el estado de onboarding tiene `completed = true`
- WHEN la app inicia
- THEN la pantalla inicial es `AppScreen.Dashboard`

---

### Requirement: Persisted Onboarding State (prefs, sin Room)

El estado de onboarding MUST persistirse vía `AutonomiaRepository` sobre prefs
(`prefs.edit { }`), NUNCA en Room. El cambio MUST NOT introducir ninguna migración de
esquema. El estado consta de:

- `completed: Boolean` — si el onboarding terminó.
- `currentStep` — índice/identificador del bloque en curso.

El estado por defecto (sin prefs) MUST ser `completed = false`, `currentStep` = primer
bloque (Bloque 0).

#### Scenario: Estado inicial sin prefs

- GIVEN no hay claves de onboarding en prefs
- WHEN se lee el estado de onboarding
- THEN `completed = false`
- AND `currentStep` corresponde al Bloque 0

#### Scenario: Sin migración Room

- GIVEN el onboarding-gate aplicado
- WHEN se inspecciona el esquema Room y las migraciones
- THEN no existe entidad ni migración nueva para el estado de onboarding
- AND el estado vive exclusivamente en prefs

---

### Requirement: Resume on Reopen

Si la app se cierra/mata con el onboarding en curso, al reabrir MUST reanudar en el
`currentStep` persistido, no reiniciar desde el principio. El avance entre bloques MUST
persistir `currentStep` de forma que sobreviva al cierre del proceso.

#### Scenario: Reanuda donde quedó

- GIVEN el usuario avanzó hasta el Bloque 2 (`currentStep` = Bloque 2, `completed = false`)
- AND el proceso de la app se mató
- WHEN la app se reabre
- THEN la pantalla inicial es `AppScreen.Onboarding`
- AND el bloque mostrado es el Bloque 2

---

### Requirement: Completion Sets Flag

Alcanzar el cierre del onboarding (Bloque 4 → acción "Entrar") MUST setear
`completed = true` y rutear a `Dashboard`. Una vez completado, el gate MUST NOT volver a
mostrar el onboarding en futuros arranques.

#### Scenario: Completar el onboarding

- GIVEN el usuario está en el Bloque 4 (Cierre)
- WHEN ejecuta la acción "Entrar"
- THEN `completed` pasa a `true` en prefs
- AND la app navega a `AppScreen.Dashboard`

#### Scenario: Relanzar tras completar no repite el onboarding

- GIVEN `completed = true`
- WHEN la app se relanza
- THEN la pantalla inicial es `AppScreen.Dashboard`
- AND el onboarding no se muestra

---

### Requirement: Invalid Persisted Step → Safe Restart

Si el `currentStep` persistido no corresponde a un bloque válido (p. ej. tras un update
que cambió la lista de bloques), el gate MUST reiniciar de forma segura en el Bloque 0
sin crashear, manteniendo `completed = false`.

#### Scenario: currentStep fuera de rango

- GIVEN `completed = false` y `currentStep` apunta a un bloque inexistente
- WHEN la app inicia y se resuelve el estado
- THEN el bloque mostrado es el Bloque 0
- AND la app no crashea

---

### Requirement: Block Navigation Skeleton

El onboarding MUST renderizar los bloques en orden (0, 0.5, 1, 2, 3, 4) con avance y
retroceso. La secuencia de navegación MUST ser **dependiente de la intención persistida**:
`OnboardingFlow.next` y `OnboardingFlow.previous` MUST recibir la intención como
parámetro y aplicar las siguientes reglas:

- **Ruta estándar** (`STANDARD`): `next(Sleep)` → `Closing`; `previous(Closing)` → `Sleep`.
  `OnboardingStep.Sobriety` MUST ser omitido completamente de la secuencia.
- **Ruta sobriedad** (`PROTECTION`): `next(Sleep)` → `Sobriety`; `next(Sobriety)` →
  `Closing`; `previous(Closing)` → `Sobriety`; `previous(Sobriety)` → `Sleep`.
- **Sin intención persistida** (usuario aún en Bloque 0.5 o antes): `next/previous` de
  los pasos anteriores a `Sobriety` MUST NO depender de la intención — la ramificación
  solo aplica desde `Sleep` en adelante.

La lógica de ramificación es dominio puro: MUST vivir en `OnboardingFlow`, MUST NOT filtrarse
a Compose ni al ViewModel.

En slice 1, SOLO el Bloque 0 (Bienvenida) y el Bloque 4 (Cierre) están implementados
con su copy canónico; los bloques intermedios (0.5, 1, 2, 3) existen como placeholders
navegables con un contrato de "paso" estable. El esqueleto MUST NOT imponer las
compuertas del motor (3 anclas, ventana de sueño) en sí mismo.

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

### Requirement: Tono y nombres canónicos del copy

El copy visible del Bloque 0 y el Bloque 4 MUST usar el texto canónico del doc de
captura (registro neutro + literario), respetar el tono (`tono-comunicacion.md`: sin
"fallaste", sin diagnóstico, sin castigo) y nombrar la app como **"Autonomía sin
límites"**, no "Vocal".

#### Scenario: Bloque 0 usa el nombre y tono correctos

- GIVEN el Bloque 0 (Bienvenida) se renderiza
- THEN el título es "Autonomía sin límites"
- AND el texto no contiene lenguaje de culpa/castigo/diagnóstico

---

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

## Criterios de aceptación

- **Tests (JVM puro, TDD primero):** `initialScreen(estado)` mapea cada estado a la
  pantalla correcta; las transiciones (avanzar/retroceder/completar) producen el
  `currentStep`/`completed` esperados; `currentStep` inválido → Bloque 0;
  `OnboardingFlow.next/previous` con los cuatro escenarios de ramificación (STANDARD y
  PROTECTION, desde Sleep y Closing) en verde, escritos ANTES del wiring.
- **Runtime (`verificacion-por-capas.md`):** install limpio abre en Bloque 0 (no
  Dashboard); matar a mitad y reabrir reanuda; completar → Dashboard y se mantiene tras
  relanzar; app arranca sin crashear; logcat sin errores; Lint sin Error; build verde;
  ruta estándar no muestra `Sobriety`; ruta sobriedad muestra `Sobriety` entre Sleep y
  Closing; cambio de intención al retroceder produce la nueva ruta.
