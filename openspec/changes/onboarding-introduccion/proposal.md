# Proposal: onboarding-gate (onboarding-introduccion · slice 1)

> Diseño conceptual completo (insumo): `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md`.
> Este proposal cubre el change global `onboarding-introduccion` y especifica el **slice 1**.

## Intent

Hoy la app SIEMPRE arranca en `Dashboard`: no existe gate de primer-uso, ni flag de
"onboarding completado", ni flujo de bienvenida. Un usuario nuevo solo tiene config
"de fábrica" porque la DB se siembra desde `DefaultSeeds.kt`; nada lo guía a una
**elección activa** de su base. Las compuertas del motor (3 capas con ancla, ventana
de sueño elegida) no están garantizadas por la UI.

El change global `onboarding-introduccion` introduce un onboarding narrativo y guiado
que orquesta la configuración mínima en el primer uso. Por tamaño se entrega en
**slices encadenados** (PRs reviewable <400 líneas). Este **slice 1** entrega solo el
**andamiaje**: el gate de primer-uso, el estado de onboarding persistido (progreso +
completitud), la reanudación, y el esqueleto de navegación por bloques. NO configura
anclas/sueño/sobriedad ni impone las compuertas del motor (eso llega con los slices
2-4). Es el cimiento sobre el que se montan los bloques.

## Scope

### In Scope (slice 1)
- **Estado de onboarding en prefs** (vía `AutonomiaRepository`, SIN Room/migración):
  `completed: Boolean` + `currentStep` (índice del bloque en curso).
- **Gate de primer-uso**: la pantalla inicial se deriva del estado — onboarding no
  completado → `AppScreen.Onboarding` (en el `currentStep` persistido); completado →
  `Dashboard`. La decisión de ruteo se expone como **estado del dominio/repositorio**;
  Compose solo la renderiza (local-first).
- **`AppScreen.Onboarding`** nuevo en el enum de `MainActivity`, integrado al
  `when (currentScreen)`.
- **Esqueleto de navegación por bloques**: secuencia ordenada de pasos (0, 0.5, 1, 2,
  3, 4) con avance/retroceso. Bloque 0 (Bienvenida) y Bloque 4 (Cierre) implementados
  con su copy canónico; los bloques intermedios (0.5/1/2/3) quedan como **placeholders
  registrados** que llenan los slices 2-4.
- **Reanudación**: matar la app a mitad del onboarding y reabrir reanuda en el
  `currentStep` persistido.
- **Completitud**: alcanzar el cierre (Bloque 4 → "Entrar") setea `completed = true` y
  rutea a `Dashboard`; a partir de ahí el gate no vuelve a mostrar el onboarding.
- TDD primero: la lógica de ruteo (`estado → pantalla inicial`) y las transiciones de
  estado son funciones puras JVM testeables ANTES del wiring.

### Out of Scope (otros slices)
- Slice 2: bloque **Anclas** (elegir del catálogo + crear propias; regla 3 capas
  distintas). Slice 3: bloque **Sueño** (ventana + permiso telemetría salteable).
  Slice 4: bloque **Sobriedad** + **persistencia de la intención** (Bloque 0.5).
  Slice 5: **notificaciones** (B sin-datos + A wind-down + `POST_NOTIFICATIONS`).
- **Imposición de las compuertas del motor** (3 anclas en 3 capas, ventana de sueño):
  entra con los slices 2-3, NO acá. En slice 1 completar el andamiaje NO garantiza una
  base válida (aceptable: los slices son encadenados, no releases independientes).
- **Re-onboarding / recuperación de `NoData`** (volver al flujo guiado tras
  desconfigurar): fuera del v1; el gate de slice 1 es solo primer-uso.
- Retiro de "Vocal" del repo (tarea de consistencia aparte).

## Capabilities

### New Capabilities
- `onboarding-gate`: contrato de ruteo de primer-uso, estado de onboarding persistido
  (progreso + completitud), reanudación, y esqueleto de navegación por bloques.

### Modified Capabilities
None (no hay specs openspec previos para navegación; la fuente canónica de UX/tono son
los docs de `docs/` + el doc de captura).

## Approach

El estado de onboarding se modela como un valor inmutable (`completed`, `currentStep`)
leído/escrito por `AutonomiaRepository` sobre `prefs.edit { }` (ya usado para
tema/automode). Una función pura `initialScreen(onboardingState): AppScreen` decide el
arranque; `MainActivity` la consume para inicializar `currentScreen`. El esqueleto de
onboarding es un Composable contenedor que renderiza el bloque del `currentStep` y
emite acciones de avanzar/retroceder/completar, que el repositorio persiste. Bloque 0 y
4 traen su copy; los intermedios son placeholders con un contrato de "paso" estable
para que los slices siguientes los implementen sin tocar el andamiaje.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `AutonomiaRepository.kt` | Modified | Estado de onboarding en prefs (leer/escribir progreso + completitud) |
| `MainActivity.kt` | Modified | `AppScreen.Onboarding` nuevo + gate de pantalla inicial vía `initialScreen(...)` |
| `ui/onboarding/OnboardingScreen.kt` | New | Contenedor/esqueleto de navegación por bloques + Bloque 0 y 4 |
| `domain/onboarding/OnboardingState.kt` (o similar) | New | Modelo de estado + `initialScreen(...)` puro |
| `.../onboarding/OnboardingGateTest.kt` | New | Tests de ruteo + transiciones + reanudación (TDD primero) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `currentStep` persistido inválido tras cambiar la lista de bloques en un update | Med | Clamp/validación: índice fuera de rango → reiniciar en el bloque 0 |
| Completar el andamiaje deja al usuario en `NoData` (sin anclas/sueño) | Med (dev) | Aceptado: slices encadenados, no releases sueltos; las compuertas entran en slices 2-3 |
| Lógica de gate filtrada a Compose (rompe local-first) | Low | `initialScreen(...)` puro en dominio; Compose solo renderiza |

## Rollback Plan

Cambio acotado a UI + prefs, SIN migración Room. Revertir = `git revert` del commit
del slice 1: desaparece `AppScreen.Onboarding`, el gate, y el estado en prefs (claves
nuevas, inertes si quedaran). La app vuelve a arrancar siempre en `Dashboard`. Sin
estado persistido crítico afectado.

## Dependencies

- Ninguna externa. Las decisiones de diseño están cerradas en el doc de captura.
- Los slices 2-5 dependen de este (montan sus bloques sobre el andamiaje).

## Success Criteria

- [ ] Install limpio → la app arranca en `AppScreen.Onboarding` (Bloque 0), no en `Dashboard`.
- [ ] El estado de onboarding (completitud + `currentStep`) persiste en prefs, sin Room.
- [ ] Matar la app a mitad y reabrir reanuda en el `currentStep` persistido.
- [ ] Alcanzar el cierre setea `completed = true`; al relanzar, la app arranca en `Dashboard`.
- [ ] `currentStep` inválido → reinicio seguro en Bloque 0 (sin crash).
- [ ] Tests de ruteo, transiciones y reanudación verdes, escritos ANTES del wiring (TDD).
- [ ] Capas de `verificacion-por-capas.md` en verde (build, lint, app arranca en emulador, logs limpios).
