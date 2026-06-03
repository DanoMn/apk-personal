# Especificación: onboarding-intention

Fuente canónica: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §2.2, §2.5, §3, §4 (Bloque 0.5), §7
Cambio origen: `onboarding-introduccion` (slice 4 — Bloque 0.5 del mapa de bloques)

## Purpose

El Bloque Intención reemplaza el placeholder `OnboardingStep.Intention` con una pregunta
cálida ("¿Qué te trae aquí?") que ofrece dos rutas — estándar y sobriedad — sin etiquetar
al usuario ni bloquear el avance. La elección se persiste en prefs y se usa únicamente para
**ramificar la secuencia**: la ruta estándar omite el Bloque Sobriedad; la ruta sobriedad lo
incluye. El uso de la intención para adaptar tono u ofertas futuras (mensajes del dashboard,
sugerencias) está fuera de alcance de este bloque.

---

## Requirements

### Requirement: Presentación de las dos rutas

El Bloque Intención MUST mostrar la pregunta "¿Qué te trae aquí?" con exactamente dos
opciones seleccionables: (a) "Quiero ordenar mi día a día" (ruta estándar) y (b) "Quiero
cuidarme de algo que me cuesta" (ruta sobriedad). El copy MUST usar el texto canónico v3
de `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §4 (Bloque 0.5). La
UI MUST reemplazar el placeholder existente (`OnboardingStep.Intention`) con la interacción
real. El bloque MUST incluir el aviso "No hay respuesta correcta. Podrás cambiarla cuando
quieras." conforme al copy canónico.

#### Scenario: Seleccionar ruta estándar

- GIVEN el Bloque Intención está visible
- WHEN el usuario toca "Quiero ordenar mi día a día"
- THEN la opción queda seleccionada (feedback visual)
- AND el botón de avance se habilita

#### Scenario: Seleccionar ruta sobriedad

- GIVEN el Bloque Intención está visible
- WHEN el usuario toca "Quiero cuidarme de algo que me cuesta"
- THEN la opción queda seleccionada (feedback visual)
- AND el botón de avance se habilita

#### Scenario: Sin selección, avance bloqueado

- GIVEN el Bloque Intención está visible y ninguna opción fue seleccionada
- WHEN el usuario intenta avanzar
- THEN el avance no ocurre
- AND no se muestra mensaje de error culpabilizador (acorde a tono)

---

### Requirement: Persistencia de la intención en prefs

La elección de intención MUST persistirse en `SharedPreferences` vía `AutonomiaRepository`
(mismo mecanismo que `onboarding_current_step`, sin Room). La clave MUST tener un valor
nominal que identifique la ruta: por ejemplo `STANDARD` o `PROTECTION`. La preferencia
MUST sobrevivir al cierre del proceso. MUST NOT introducir entidad Room ni migración.

#### Scenario: Intención persiste tras cierre de la app

- GIVEN el usuario seleccionó "Quiero cuidarme de algo que me cuesta" en el Bloque Intención
- AND la app se cierra antes de completar el onboarding
- WHEN la app se reabre y reanuda en el onboarding
- THEN la intención persiste en prefs como ruta sobriedad
- AND el flujo evalúa la ruta correcta al retomar la secuencia

#### Scenario: Intención cambiada al volver atrás se re-persiste

- GIVEN el usuario seleccionó ruta sobriedad y avanzó al Bloque Anclas
- WHEN el usuario toca "Volver" hasta llegar al Bloque Intención
- AND elige "Quiero ordenar mi día a día"
- AND avanza nuevamente
- THEN la pref de intención se actualiza a ruta estándar
- AND la nueva secuencia de pasos omite el Bloque Sobriedad

---

### Requirement: Tono y nombres canónicos

El Bloque Intención MUST usar español neutro (no voseo). MUST NOT mencionar "adicción",
"recuperación", "recaída" ni lenguaje clínico o policial en este bloque. El nombre de la
feature visible es **Sobriedad** (nombre canónico de UI — AGENTS.md) cuando referenciado
en contexto de la ruta, nunca el nombre técnico.

#### Scenario: Copy sin lenguaje clínico ni culpabilizador

- GIVEN el Bloque Intención se renderiza
- THEN el texto visible no contiene las palabras "adicción", "recuperación", "error", "falló"
- AND el encabezado es "¿Qué te trae aquí?"

---

### Requirement: Sin modelo de datos nuevo

MUST NOT introducir entidad Room ni migración. La intención MUST persistirse en prefs
(MUST NOT en `daily_activity_logs`, `user_activity_config`, ni ninguna tabla Room).

#### Scenario: Sin migración Room en slice 4

- GIVEN el onboarding-intention aplicado
- WHEN se inspecciona el esquema Room y las migraciones
- THEN no hay entidad ni migración nueva por el Bloque Intención

---

## Fuera de alcance (explícito)

- Adaptar el tono del dashboard o sus sugerencias según la intención persistida (futuro).
- Modo riesgo (`RiskEventEntity`): no se ofrece ni se menciona en el onboarding.
- Tres o más tipos de usuario: el modelo es exactamente dos rutas.
- Cambiar la intención después de completar el onboarding (fuera del v1).

---

## Criterios de aceptación

- **Tests (JVM puro, TDD primero):** función pura de dominio que valida si la intención
  tiene una selección válida (regla de habilitación del avance). El cambio de intención al
  retroceder y re-seleccionar produce la ruta correcta (evaluable sin Android).
- **Persistencia:** la pref de intención sobrevive a reinicio del proceso y es legible por
  `OnboardingFlow` para derivar la secuencia.
- **Static:** `assembleDebug` + `lintDebug` + `testDebugUnitTest` en verde.
- **Runtime (`verificacion-por-capas.md`, capas 1-4 aplicables):** el Bloque Intención
  reemplaza el placeholder; al seleccionar una opción se habilita "Continuar"; cerrar y
  reabrir reanuda con la intención persistida; logcat sin crashes.
