# Especificación: onboarding-sleep

Fuente canónica: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §2.7, §2.8, §3, §4 (Bloque 2), §5, §6 · `docs/sueno/decisiones-diseno-sueno-v1.md` (modelo de sueño sellado)
Cambio origen: `onboarding-introduccion` (slice 3 — Bloque 2 del mapa de bloques)

## Purpose

El Bloque Sueño del onboarding convierte el default de ventana heredado en una
**elección activa** del usuario: el usuario confirma o ajusta `targetSleepAt` y
`targetWakeAt`, con la duración derivada. También ofrece (sin bloquear) el permiso de
telemetría automática (UsageStats) y captura el consentimiento explícito para el
recordatorio de hora de descanso. Este bloque hace cumplir la segunda compuerta del
motor (`SleepPolicy.validatePlannedWindow`). NO introduce modelo de datos nuevo ni
migración Room.

---

## Requirements

### Requirement: Active Sleep Window Choice (motor gate 2)

El usuario MUST confirmar o ajustar explícitamente la ventana de sueño
(`targetSleepAt` + `targetWakeAt`) en el onboarding; el valor heredado del default
(`23:30` → `07:30`) MUST NOT usarse en silencio como elección efectiva. El avance
MUST estar bloqueado hasta que la ventana sea válida según
`SleepPolicy.validatePlannedWindow` (duración ≥ `MIN_SLEEP_WINDOW_MINUTES` = 300 min).
La duración MUST derivarse de los dos tiempos; MUST NOT pedirse como campo separado.
La regla pura MUST vivir en una función de dominio testeable sin Android (mismo patrón
que `OnboardingAnchorsRule`).

#### Scenario: Ventana válida con defaults habilita avanzar

- GIVEN los pickers muestran 23:30 → 07:30 (8h, > 5h mínimo)
- WHEN el usuario toca "Continuar" sin cambiar los valores
- THEN se evalúa `SleepPolicy.validatePlannedWindow("23:30", "07:30")`
- AND el resultado es `SleepWindowValidation.Valid`
- AND el avance al siguiente bloque se habilita

#### Scenario: Ventana < 5h bloquea el avance con mensaje

- GIVEN el usuario selecciona 23:30 como hora de dormir y 02:00 como hora de despertar (2,5h)
- WHEN se evalúa la ventana
- THEN el resultado es `SleepWindowValidation.Invalid`
- AND el avance permanece bloqueado
- AND se muestra un mensaje de tono no culpabilizador (sin "fallaste / error")

#### Scenario: Ventana exacta de 5h es válida

- GIVEN targetSleepAt = "23:30", targetWakeAt = "04:30" (exactamente 300 minutos)
- WHEN se evalúa `validatePlannedWindow`
- THEN el resultado es `SleepWindowValidation.Valid`

#### Scenario: Ajustar los pickers actualiza la derivación en tiempo real

- GIVEN el bloque de sueño está visible con 23:30 → 07:30
- WHEN el usuario mueve el picker de despertar a 05:30 (6h)
- THEN la ventana derivada se actualiza sin requerir confirmación explícita adicional
- AND el avance queda habilitado (6h > 5h mínimo)

---

### Requirement: Telemetry Permission Offer (skippable)

El onboarding MUST ofrecer el permiso UsageStats para detección automática de sueño,
con la explicación canónica ("para leer tu descanso sin que anotes nada"). El usuario
MUST poder saltar la solicitud con "Más tarde" sin bloquear el avance. Saltar MUST NOT
impedir completar el onboarding ni afectar el scoring (el score de sueño queda en
`NoData` hasta que se active la telemetría después). El consentimiento de activar/saltar
MUST persistirse en prefs (mismo mecanismo que el resto del estado de onboarding, sin
Room). El permiso SHOULD poder re-ofrecerse desde la configuración de sueño después
(`SleepConfigScreen`).

#### Scenario: Usuario activa telemetría

- GIVEN el bloque de sueño ofrece "Activar" y "Más tarde"
- WHEN el usuario toca "Activar"
- THEN se lanza el flujo del permiso UsageStats del sistema
- AND se persiste que el usuario intentó activar la telemetría (`usageStatsRequested = true`)
- AND el avance no queda bloqueado por esta acción

#### Scenario: Usuario salta con "Más tarde"

- GIVEN el bloque de sueño muestra la oferta de permiso
- WHEN el usuario toca "Más tarde"
- THEN no se lanza ningún diálogo del sistema
- AND se persiste que el usuario eligió saltar (`usageStatsSkipped = true`)
- AND el avance al siguiente bloque no está bloqueado por esta acción

#### Scenario: Permiso denegado por el sistema

- GIVEN el usuario tocó "Activar" y el sistema denegó el permiso UsageStats
- WHEN el onboarding retoma el foco (retorno del settings del sistema)
- THEN el bloque no muestra error culpabilizador
- AND el avance no queda bloqueado
- AND el estado persiste como "intentó activar, permiso no concedido"

---

### Requirement: Wind-Down Consent Capture

El onboarding MUST preguntar explícitamente al usuario si desea recibir un recordatorio
cuando se acerque su hora de descanso ("¿Quieres que te avise cuando se acerque tu hora
de descanso?"). La respuesta (Sí / No) MUST persistirse como consentimiento. La
implementación del recordatorio (scheduling de notificación, permiso `POST_NOTIFICATIONS`)
es responsabilidad del slice 5; este bloque ONLY captura y guarda el consentimiento.

#### Scenario: Usuario consiente el wind-down

- GIVEN el bloque de sueño muestra la pregunta de recordatorio
- WHEN el usuario toca "Sí"
- THEN se persiste `windDownConsentGiven = true` en prefs
- AND no se pide ni se agenda ninguna notificación en este bloque

#### Scenario: Usuario rechaza el wind-down

- GIVEN el bloque de sueño muestra la pregunta de recordatorio
- WHEN el usuario toca "No"
- THEN se persiste `windDownConsentGiven = false` en prefs
- AND no se muestra mensaje de castigo/juicio

#### Scenario: El consentimiento sobrevive al cierre de la app

- GIVEN el usuario respondió "Sí" a la pregunta de recordatorio
- AND la app se cierra antes de completar el onboarding
- WHEN la app se reabre y reanuda en el Bloque Sueño (per onboarding-gate resume)
- THEN el valor `windDownConsentGiven = true` está disponible en prefs

---

### Requirement: Copy canónico y tono

El texto visible del Bloque Sueño MUST usar el copy v3 de `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §4 ("El descanso primero"). El tono MUST respetar `docs/producto/tono-comunicacion.md`: sin "fallaste", sin diagnóstico, sin lenguaje policial. El idioma MUST ser español neutro (no voseo: "Elige", "tu hora", no "Elegí", "tu hora"). El nombre de la feature visible es **Sueño** (nombre canónico de UI — ver `AGENTS.md`).

#### Scenario: Texto literal del encabezado

- GIVEN el Bloque Sueño se renderiza
- THEN el encabezado es "El descanso primero"
- AND el subtítulo menciona "ventana", no "número de horas"

#### Scenario: Mensaje de ventana inválida usa tono correcto

- GIVEN la ventana seleccionada es < 5h
- WHEN se muestra el mensaje de bloqueo
- THEN el mensaje NO contiene las palabras "error", "incorrecto", "fallaste", ni signos de exclamación de alarma
- AND el mensaje informa la restricción de forma neutral (ej. "La ventana mínima es de 5 horas")

---

### Requirement: Sin modelo de datos nuevo

El Bloque Sueño MUST reusar `SleepPolicy.validatePlannedWindow` para la validación de
la ventana. El consentimiento (telemetría, wind-down) MUST persistirse en prefs, NUNCA
en Room. MUST NOT introducir migración Room ni entidad nueva en este bloque.

#### Scenario: Sin migración Room

- GIVEN el onboarding-sleep aplicado
- WHEN se inspecciona el esquema Room y las migraciones
- THEN no hay entidad ni migración nueva por el Bloque Sueño

---

## Fuera de alcance (explícito)

- `digitalWindDown` (descanso digital): diferido (D3), default 0, NO se configura en el
  onboarding.
- Registro nocturno manual ("voy a dormir"): el usuario NO registra el sueño cada noche;
  la detección es automática por telemetría.
- Permiso `POST_NOTIFICATIONS` (Android 13+): se pide perezoso en onboarding-notifications, NUNCA en este bloque.
- Scheduling/activación del recordatorio wind-down: onboarding-notifications.
- Notificación B (noches sin datos): onboarding-notifications.
- Configuración de `digitalWindDownMinutes`: UI de config existente (`SleepConfigScreen`),
  no el onboarding.

---

## Criterios de aceptación

- **Tests (JVM puro, TDD primero):** regla pura del gate de ventana (≥ 5h habilita,
  < 5h bloquea, exactamente 300 min es válido) sobre `SleepPolicy.validatePlannedWindow`
  o una regla de dominio wrapper análoga a `OnboardingAnchorsRule`. Tests escritos ANTES
  del wiring. Strict TDD activo.
- **Persistencia:** `targetSleepAt` + `targetWakeAt` de la ventana elegida, el estado
  del permiso UsageStats (`usageStatsRequested` / `usageStatsSkipped`), y el
  `windDownConsentGiven` persisten en prefs y sobreviven a reinicio del proceso.
- **Static:** `assembleDebug` + `lintDebug` + `testDebugUnitTest` en verde.
- **Runtime (`verificacion-por-capas.md`, capas 1-4 aplicables):** el Bloque Sueño
  aparece al avanzar desde el Bloque Anclas; los pickers parten en 23:30 / 07:30;
  cambiar a ventana < 5h deshabilita "Continuar" con mensaje neutral; corregir a ≥ 5h
  lo rehabilita; "Más tarde" avanza sin diálogo del sistema; "Sí"/"No" al recordatorio
  persiste; logcat sin crashes.
