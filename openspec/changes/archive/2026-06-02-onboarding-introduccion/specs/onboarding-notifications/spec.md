# Especificación: onboarding-notifications

Cambio: `onboarding-introduccion` · slice 5 (último slice)
Fuente canónica: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §2.8, §7 ·
`docs/sueno/decisiones-diseno-sueno-v1.md` §9 (D1) ·
`openspec/changes/onboarding-introduccion/specs/onboarding-sleep/spec.md` (consentimiento wind-down)

## Purpose

El slice 5 activa las dos notificaciones de sueño acordadas en el diseño, programa la
infraestructura de canales y gestiona el permiso `POST_NOTIFICATIONS` (Android 13+) de
forma perezosa. Consume el consentimiento capturado en el slice 3 (`windDownConsentGiven`)
y los datos de sueño interpretados por `SleepInterpreter`. NO introduce ningún registro
nocturno manual: el sueño sigue siendo telemetría automática.

---

## Requirements

### Requirement: Notification Channel Registration

La app MUST registrar al menos un `NotificationChannel` (o dos canales separados, uno por
tipo) antes de mostrar cualquier notificación. Los canales MUST crearse en el arranque
de la app, de forma idempotente (re-creación sin efecto sobre canales existentes).

Las propiedades mínimas de cada canal:

| Canal | `channelId` sugerido | Importancia | Descripción visible |
|-------|---------------------|-------------|---------------------|
| Informativo (Notif B) | `sleep_data_alert` | `IMPORTANCE_DEFAULT` | Alertas sobre datos de sueño |
| Compromiso (Notif A) | `sleep_wind_down` | `IMPORTANCE_DEFAULT` | Recordatorio de hora de descanso |

Usar un único canal (`sleep_notifications`) en su lugar es decisión del implementador —
la spec admite ambas opciones. Lo que MUST NOT suceder es enviar notificaciones a un
canal no registrado (crash en tiempo de ejecución en API ≥ 26).

#### Scenario: Canales creados al arrancar

- GIVEN la app inicia (cualquier arranque, no solo primer uso)
- WHEN se inicializa el `NotificationManager` / `NotificationChannel`
- THEN los canales requeridos existen en el sistema
- AND una segunda inicialización no borra ni duplica el canal

#### Scenario: Sin crash en API 26+ por canal ausente

- GIVEN el slice 5 está aplicado
- WHEN se intenta enviar una notificación de cualquier tipo
- THEN el canal ya existe y la notificación se entrega sin crash

---

### Requirement: Notification A — Wind-Down Reminder

La Notificación A MUST programarse como alarma diaria anclada a `targetSleepAt` (hora
de dormir configurada en el Bloque Sueño). MUST activarse si y solo si
`windDownConsentGiven = true` en prefs. Si el consentimiento es `false` o `null`, la
notificación MUST NOT programarse ni dispararse.

El patrón de implementación MUST seguir la arquitectura Worker+Scheduler (igual que
`DailyClosureWorkScheduler` / `DeviceTelemetryWorkScheduler`): un `Scheduler` que decide
cuándo programar y un `Worker` (o `AlarmManager` callback) que entrega la notificación.

Contenido y tono (MUST):

- El texto MUST invitar, no ordenar. Ejemplos válidos: "Se acerca tu hora de descanso",
  "Es momento de prepararte para dormir". Ejemplos inválidos: "Debes dormir ahora",
  "No olvidaste irte a dormir".
- MUST NOT usar las palabras "fallaste", "deberías", ni signos de exclamación de alarma.
- El nombre visible en la notificación MUST usar el nombre canónico del producto
  ("Autonomía sin límites"), no "Vocal".

Permiso `POST_NOTIFICATIONS` (ver Requirement separado): la programación de la
notificación MUST verificar si el permiso está concedido antes de intentar mostrarla.
Si no está concedido, MUST activar el flujo de solicitud perezosa (ver Requirement:
POST_NOTIFICATIONS Lazy Request).

#### Scenario: Notificación A programada al completar el onboarding (consentimiento true)

- GIVEN el usuario completó el onboarding con `windDownConsentGiven = true`
- AND `targetSleepAt` está configurado (ej. "23:30")
- WHEN la app entra en `Dashboard` por primera vez tras el onboarding
- THEN la Notificación A queda programada para dispararse diariamente a las "23:30"

#### Scenario: Notificación A no se programa con consentimiento false

- GIVEN el usuario completó el onboarding con `windDownConsentGiven = false`
- WHEN la app entra en `Dashboard`
- THEN ninguna Notificación A queda pendiente o programada

#### Scenario: Notificación A no se programa con consentimiento null

- GIVEN el usuario abandonó el Bloque Sueño sin responder la pregunta de consentimiento
  (`windDownConsentGiven = null`)
- WHEN la app se reabre
- THEN ninguna Notificación A queda pendiente o programada

#### Scenario: Tono de invitación en la notificación

- GIVEN `windDownConsentGiven = true` y la Notificación A se dispara
- WHEN el sistema entrega la notificación
- THEN el texto no contiene "deberías", "fallaste", "no olvidaste", ni signos de
  exclamación de alarma
- AND el texto es una invitación en tono adulto funcional compasivo

#### Scenario: Sin `targetSleepAt`, no se programa

- GIVEN `windDownConsentGiven = true` pero no existe `targetSleepAt` válido en prefs
- WHEN se intenta programar la Notificación A
- THEN no se produce crash
- AND no se programa ninguna alarma con hora inválida

---

### Requirement: Notification B — Sleep Data Alert

La Notificación B MUST dispararse tras **N noches consecutivas sin telemetría de sueño**,
donde N es la constante de dominio `SleepNotificationPolicy.NIGHTS_WITHOUT_DATA_THRESHOLD`
con valor por defecto **3** (tres). Esta constante MUST estar documentada como
calibrable con datos reales (gemelo de la deuda D1 de `decisiones-diseno-sueno-v1.md`).

La Notificación B MUST estar encendida por defecto: no requiere consentimiento explícito
del usuario. El usuario puede desactivarla desde la configuración del sistema (canales
de notificación de Android).

"Sin telemetría" MUST interpretarse como: para una noche dada, el resultado de
`SleepInterpreter` es `NoData` o la noche no tiene segmentos (`SleepSegmentEntity`)
en la base de datos.

Contenido y tono (MUST):

- El texto MUST informar que el estado de sueño puede estar incompleto y SUGERIR
  revisar el permiso de uso (UsageStats). MUST NOT culpabilizar al usuario.
- Ejemplos válidos: "No detectamos datos de sueño en los últimos 3 días. Podés revisar
  el permiso de uso en la configuración." (nota: la app habla en español neutro — "Puedes").
- Texto canónico: español neutro (no voseo).
- La acción de la notificación SHOULD llevar a la pantalla de configuración de sueño /
  permiso (tarea del implementador; si no se implementa, la notificación abre la app).

La verificación de las N noches MUST realizarse en el `DailyClosureWorker` (ya existente,
medianoche local), reutilizando el ciclo de cierre diario. MUST NOT crear un Worker
nuevo solo para esta verificación.

#### Scenario: Notificación B disparada tras N noches sin datos

- GIVEN N = 3 (valor default)
- AND las últimas 3 noches cerradas tienen resultado `NoData` en `SleepInterpreter`
- WHEN el `DailyClosureWorker` corre al cerrar la noche 3
- THEN se emite la Notificación B

#### Scenario: Una noche con datos reinicia el contador

- GIVEN 2 noches consecutivas sin datos
- AND la tercera noche tiene telemetría con dato válido
- WHEN el `DailyClosureWorker` procesa esa tercera noche
- THEN la Notificación B NO se emite
- AND el contador de noches sin datos vuelve a 0

#### Scenario: La notificación no culpabiliza

- GIVEN la Notificación B se dispara
- WHEN el sistema la entrega
- THEN el texto no contiene "fallaste", "no registraste", "olvidaste", ni diagnóstico
- AND el texto sugiere revisar el permiso de uso de forma neutral

#### Scenario: N es constante configurable en dominio

- GIVEN `SleepNotificationPolicy.NIGHTS_WITHOUT_DATA_THRESHOLD = 3`
- WHEN se evalúan las noches sin datos
- THEN el umbral usado es exactamente 3
- AND cambiar el valor de la constante cambia el umbral sin tocar lógica de disparo

---

### Requirement: POST_NOTIFICATIONS Lazy Permission Request

En Android 13+ (API 33+), la app MUST declarar el permiso `android.permission.POST_NOTIFICATIONS`
en `AndroidManifest.xml`. En API < 33, el permiso no existe y MUST NOT pedirse.

El permiso MUST solicitarse de forma perezosa: el pedido al sistema MUST ocurrir
únicamente cuando la primera notificación tenga sentido para ese usuario, no antes
ni durante el onboarding. El gatillo es:

- **Para la Notificación A**: cuando el usuario completa el onboarding con
  `windDownConsentGiven = true` y la app intenta programar la Notificación A por primera
  vez. Si el permiso no está concedido, se pide en ese momento.
- **Para la Notificación B**: la primera vez que el `DailyClosureWorker` detecta N noches
  sin datos y va a emitir la Notificación B, si el permiso no está concedido, se pide.

Denegación no bloqueante: si el usuario deniega `POST_NOTIFICATIONS`:

- La app MUST continuar funcionando normalmente.
- Las notificaciones simplemente no se muestran; MUST NOT hay degradación de scoring,
  crashes, ni mensajes de error persistentes.
- MUST NOT se reintenta el pedido del permiso automáticamente en cada arranque.

#### Scenario: Permiso concedido — notificación se muestra

- GIVEN API ≥ 33
- AND el usuario concedió `POST_NOTIFICATIONS`
- WHEN la Notificación A o B está lista para mostrarse
- THEN la notificación aparece en el sistema

#### Scenario: Permiso denegado — la app no crashea ni se degrada

- GIVEN API ≥ 33
- AND el usuario denegó `POST_NOTIFICATIONS`
- WHEN la app intenta mostrar la Notificación A o B
- THEN la app sigue funcionando sin crash
- AND no se muestra ningún error en la UI
- AND el scoring no se ve afectado

#### Scenario: API < 33 — el permiso no se pide

- GIVEN el dispositivo tiene API < 33
- WHEN la app intenta programar las notificaciones
- THEN no se emite ningún diálogo de permiso `POST_NOTIFICATIONS`
- AND las notificaciones se muestran normalmente (sin declaración de runtime permission)

#### Scenario: Permiso perezoso — NO se pide durante el onboarding

- GIVEN el usuario está completando el onboarding (bloques 0-4)
- WHEN el onboarding está en cualquier bloque intermedio
- THEN NO se muestra ningún diálogo de `POST_NOTIFICATIONS`
- AND la solicitud se difiere hasta el momento de uso real

---

### Requirement: Sin registro nocturno manual

La Notificación B NO MUST hacer referencia ni sugerir que el usuario registre el sueño
manualmente. El sueño se detecta exclusivamente por telemetría automática (`SleepInterpreter`).
Cualquier texto que diga "no registraste tu sueño", "recordatorio para registrar" o
similar está PROHIBIDO: contradice el modelo de sueño sellado.

#### Scenario: Copy de la Notificación B no menciona registro manual

- GIVEN la Notificación B se dispara
- WHEN se inspecciona el texto de la notificación
- THEN el texto NO contiene las palabras "registrar", "registraste", "anotar",
  "marcar el sueño"
- AND el texto hace referencia a "datos de sueño" o "permiso de uso", no a acciones
  del usuario

---

### Requirement: Persistencia del estado de notificaciones (sin Room nuevo)

El estado necesario para las notificaciones (ej. contador de noches sin datos,
flag de si el permiso fue pedido antes) MUST persistirse en prefs si es posible.
MUST NOT introducir una migración Room nueva ni una entidad nueva únicamente para
soporte de notificaciones. Si el contador de noches sin datos se puede derivar
leyendo `SleepSegmentEntity` / `SleepLog` existentes en Room sin estado adicional,
esa es la opción preferida.

**Decisión del implementador / dueño**: si derivar el contador desde Room existente
requiere una query costosa en cada cierre nocturno, se puede cachear el contador en
prefs. Esta decisión está abierta para el design/tasks.

#### Scenario: Sin migración Room por notificaciones

- GIVEN el slice 5 aplicado
- WHEN se inspecciona el esquema Room y las migraciones
- THEN no existe entidad ni migración nueva introducida únicamente por el slice 5

---

## Fuera de alcance (explícito)

- `digitalWindDown`: diferido (D3), no puntúa en v1, default 0. Ninguna notificación
  relacionada con detox digital.
- Registro nocturno manual ("voy a dormir como acción de registro"): no existe. La única
  acción manual opcional del sueño es el botón "voy a dormir" que afina la detección —
  y su estado no activa ninguna notificación.
- Re-onboarding o notificaciones de recuperación de `NoData` por falta de anclas.
- Notificaciones de otras features (sobriedad, anclas, actividades).
- Configuración de las notificaciones dentro de la app (más allá del consentimiento
  wind-down ya capturado en slice 3); la desactivación se hace desde el sistema Android.
- El N exacto de noches como decisión de producto final: la spec define el default (3)
  y lo declara calibrable; el valor final se ajusta con datos reales.

---

## Decisiones abiertas (flaggeadas para el dueño)

| # | Pregunta | Default adoptado en spec | Necesita confirmación |
|---|----------|--------------------------|-----------------------|
| D-N | ¿Cuántas noches sin datos activan la Notif B? | **3** (constante `NIGHTS_WITHOUT_DATA_THRESHOLD`) | Sí — calibrar con datos reales post-lanzamiento |
| D-APAGA | ¿La Notif B se apaga sola (o deja de dispararse) al detectar que el permiso UsageStats fue concedido? | No especificado — la lógica es: si hay datos, el contador se resetea naturalmente | Aclarar si se quiere suprimir proactivamente cuando UsageStats está activo |
| D-CANAL | ¿Un canal único `sleep_notifications` o dos canales separados? | Dos canales sugeridos; uno también es válido | Decisión de implementación; la spec admite ambas |
| D-GATILLO-B | ¿El gatillo del `POST_NOTIFICATIONS` para la Notif B es en el Worker (background) o en la siguiente apertura de la app? | Worker (background), con fallback a apertura si el Worker no puede pedir permiso en background | Aclarar la UX de permiso desde background en Android 13+ |

---

## Criterios de aceptación

- **Tests (JVM puro, TDD primero):**
  - `SleepNotificationPolicy`: dado un listado de N noches con/sin datos, la función
    determina correctamente si disparar la Notif B (umbral exacto, contador se resetea
    con una noche con dato).
  - Regla de gating de la Notif A: `windDownConsentGiven = true` con `targetSleepAt`
    válido → programar; `false` → no programar; `null` → no programar.
  - Casos de API < 33: la lógica de solicitud de permiso no se llama.
  - Tests escritos ANTES del wiring (Strict TDD activo).
- **Manifiesto:** `POST_NOTIFICATIONS` declarado en `AndroidManifest.xml` sin romper el
  build.
- **Static:** `assembleDebug` + `lintDebug` + `testDebugUnitTest` en verde.
- **Runtime (`verificacion-por-capas.md`, capas 1-4 aplicables):**
  - En emulador API 33+: completar el onboarding con `windDownConsentGiven = true`
    → aparece el diálogo de `POST_NOTIFICATIONS`.
  - Denegarlo → la app no crashea, el Dashboard carga normalmente.
  - Concederlo → la Notificación A aparece cerca de `targetSleepAt` en el día siguiente.
  - Sin datos de sueño 3 días → la Notificación B aparece (puede simularse ajustando
    el threshold a 1 para el test manual, luego revirtiendo).
  - En emulador API < 33: no se muestra el diálogo de permiso.
  - Logcat sin crashes ni excepciones de canal no registrado.
