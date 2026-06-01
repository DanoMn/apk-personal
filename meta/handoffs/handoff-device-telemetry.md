# Handoff — `device-telemetry` (infraestructura de captura reutilizable)

> **Nombre corregido (2026-05-29):** antes "telemetry-**core**". Se renombró a
> **`device-telemetry`** porque NO es un núcleo/dominio: no interpreta ni decide nada,
> solo capta eventos del dispositivo y los deja crudos en Room. El núcleo (dominio) los
> CONSUME. El nombre del scope/cambio SDD es `device-telemetry`.

> Para arrancar en **sesión nueva** y no contaminar contexto. Cargar este doc +
> recuperar memoria de Engram (proyecto `apk-personal`).
>
> Fecha: 2026-05-29 · Proyecto: apk-personal (Vocal) · Rama base: `sdd/scoring-state-alignment`

---

## Estado: decisiones tomadas (sesión 2026-05-29)

> Esta sección consolida lo decidido en la conversación de scoping. Los agentes que
> retomen **NO deberían volver a preguntar esto al dueño**: ya está resuelto. Lo que
> sigue abierto está marcado explícitamente en §6. Para lectura no técnica, ver el
> **glosario** (§8).

### D1 — Reconstrucción, NO tiempo real

La telemetría no necesita enterarse de los eventos en el momento. Le alcanza con poder
**reconstruir** lo que pasó leyendo el registro que el propio Android ya mantiene. Esto
**descarta el foreground service** (proceso vivo toda la noche + notificación
persistente) por caro e innecesario.

### D2 — Mecanismo: `UsageStatsManager` + `WorkManager` (dirección decidida; detalles a validar en explore)

- **Fuente de datos:** `UsageStatsManager` — el subsistema de Android que YA registra
  uso de apps, transiciones de pantalla y bloqueo/desbloqueo. Es la misma fuente que
  alimenta el "Bienestar Digital" del sistema. Requiere el permiso especial
  `PACKAGE_USAGE_STATS` (lo concede el usuario en Ajustes, una sola vez).
- **Drenaje:** un job periódico de `WorkManager` que despierta cada cierto tiempo, lee
  los eventos nuevos desde la última lectura y los escribe en Room.
- **NO foreground service**, **NO `BroadcastReceiver` vivo** (consecuencia de D1).
- Estado: la dirección está decidida; los detalles finos del API y el gotcha de
  `minSdk 26` se validan en el `sdd-explore` (ver §4 y §6).

### D3 — Productor/consumidor: PULL, telemetría ciega

(Reafirma §1.) Telemetría escribe hechos crudos en Room y se olvida. Los consumidores
**leen**. Tres consumidores conocidos hoy:

1. **Sueño** — primer consumidor; alimenta la capa Cuerpo.
2. **Tracking de proyecto tipo StudyBunny** — la ventana la declara el usuario a mano; futuro.
3. **Bienestar digital** — feature opcional, NO core; futuro. Nota: es casi un
   *passthrough* de `UsageStats`, porque `UsageStats` ES la fuente del bienestar digital
   de Android.

> **Insight clave:** "manual vs automático" NO es problema de telemetría. La **ventana**
> (cuándo empieza/termina el interés) la declara el **consumidor**; la telemetría solo
> provee hechos. Por eso son **2 scopes separados**.

### D4 — Activación: opt-in, un solo interruptor

- Apagada por defecto. Si ninguna feature consumidora está activa, **no se recolecta
  nada** y el job ni se programa.
- El usuario la activa **una vez** (al prender, por ej., el sueño automático). Desde ahí
  el job corre solo, **periódicamente, todos los días**, sin que el usuario toque nada más.
- Se detiene solo cuando el usuario **apaga** la feature. **No hay arranque/parada por día.**
- Es coherente con la filosofía del producto: features opt-in ("lo que el usuario no
  activó no aparece, no pesa" — `nucleo-dominio-autonomia.md`), igual que sobriedad.

### D5 — Privacidad: gating de recolección, no recorte de esquema

- El producto es **local-first / privacy-first**: todo se queda en el dispositivo, nunca
  a la nube (`nucleo-dominio-autonomia.md` prohíbe telemetría/analytics remotos).
- La privacidad se maneja **no recolectando hasta que una feature lo pida**, NO mutilando
  el esquema. El hecho crudo puede ser genérico y completo; lo que protege al usuario es
  que la recolección está **apagada por defecto**.

### D6 — El hecho crudo carga la dimensión "qué app" desde el día 1 ✅ CONFIRMADO

- 2 de 3 consumidores (StudyBunny, bienestar digital) necesitan saber **qué app** estuvo
  en foco; sueño la ignora. `UsageStatsManager.queryEvents()` devuelve esa dimensión en
  la misma consulta, **sin permiso extra**.
- **Decisión del dueño (2026-05-29): SÍ.** El contrato genérico carga el `paquete` desde
  el día 1 aunque sueño no lo use, para no tener que migrar después. La privacidad se
  cubre con el gating de recolección (D5), no recortando el esquema.

### D7 — Ubicación arquitectónica

- **Captura** (telemetría) = capa **plataforma** nueva (`platform/telemetry/`, ver §2),
  porque toca `android.*`. Es el primer ciudadano legítimo de esa capa.
- **Hecho** = nueva entidad Room (`DeviceActivityEventEntity` o similar) + DAO +
  repositorio + **migración numerada** (esquema en v10 hoy).
- **Consumo/interpretación** = dominio (sueño ya vive en `domain/sleep/`).
- Todo **a la derecha de Room** (dominio, scoring, Compose) **no cambia**. Telemetría se
  enchufa como un productor más de hechos, **a la izquierda de Room**.

### D8 — Confirmado: el scoring de sueño está BIEN en el dominio

Se cuestionó si `domain/sleep/` era un error de otro agente. **Verificado en código que
NO lo es:**

- `domain/scoring/SpecialLayerScoringPolicy.kt` trata `sleepScore` y `sobrietyScore`
  **idénticamente** → sueño es el gemelo de sobriedad (señal especial tejida en una capa
  con su peso). Sacar sueño del dominio implicaría sacar también sobriedad y abstinencias.
- `domain/` tiene **una carpeta por feature** (`abstinence/`, `activity/`, `task/`,
  `sleep/`) → patrón consistente de toda la app, no un caso suelto.
- `SleepScoring.score(log: SleepLog): Float` es **función pura**; `SleepLog` es un
  `data class` sin Room/Android → dependencia limpia.
- **Regla:** la **interpretación** de sueño va en dominio (depende del **hecho**, no de
  su **origen**). La **captura** (telemetría, `SleepDeviceAdminReceiver`) va **FUERA** del
  dominio (plataforma). Sacar la interpretación re-acoplaría la matemática al origen del
  dato — justo lo que se evita.

---

## 0. Qué es esto (y qué NO es)

El **telemetry-core** es **infraestructura que produce hechos crudos** detectando
eventos del dispositivo (desbloqueo/bloqueo de pantalla, y a futuro uso de apps /
tiempo de pantalla). Su PRIMER consumidor es Sueño; el segundo previsto es un
tracking de tiempo dedicado a un proyecto (tipo StudyBunny).

**NO es un núcleo de dominio.** No calcula, no interpreta, no decide nada de
producto. Solo: **detectar → almacenar hecho crudo**. Quién lo lee y para qué, no
es su problema.

> ⚠ Corrección de naming (aplicada): se llamaba "telemetry-**core**" y se renombró a
> **`device-telemetry`**, porque NO es un núcleo. Es un **adapter de plataforma + stream
> de hechos**. El núcleo (dominio) sigue puro y solo CONSUME estos hechos.

---

## 1. Dos principios que NO se negocian

Estos dos son los que sostienen la reutilización. Si se rompen, se acopla todo.

1. **PULL, no PUSH.** Telemetría escribe el hecho en Room (buzón neutral) y se
   olvida. **NO conoce a Sueño.** El consumidor LEE de Room lo que necesita. La
   flecha va de Room → consumidor, nunca de telemetría → consumidor.
2. **La feature no conoce su capa.** El consumidor (Sueño) interpreta los hechos y
   produce una señal (un número 0..1). El **núcleo de scoring** decide a qué capa
   va y cuánto pesa. La capa es una dimensión de salida calculada, no un buzón.

```
[Telemetría]   detecta unlock/lock → escribe HECHO crudo en Room (neutral, ciego)
      │  (Room. Telemetría NO conoce a sueño)
      ▼
[Consumidor·dominio]  LEE hechos → interpreta → produce señal 0..1
      ▼
[Núcleo·scoring]  ubica la señal en la capa correcta con su peso
```

---

## 2. Dónde va (ubicación en la arquitectura existente)

La arquitectura ya lo anticipó. `docs/dominio/arquitectura-recomendada-autonomia.md`:

- Línea 166-167: *"el dominio no debe saber si los hechos vienen de Room, una API,
  un archivo o **telemetría futura**."*
- Líneas 196-210 / 367-376: define la capa **Platform** ("Android Context… permisos
  futuros") con estructura `platform/identity/`, `platform/secure_storage/`.
- Regla #7 (línea 956): *"No crear módulo Gradle si un paquete basta."*

Se parte en tres piezas, cada una en su capa:

| Pieza | Capa / paquete | Qué hace | ¿Toca `android.*`? |
|-------|----------------|----------|--------------------|
| **Adapter de captura** | `platform/telemetry/` *(NUEVO)* | Escucha unlock/lock; emite eventos crudos. | **SÍ** |
| **Hecho persistido** | `data/local/` + `data/repository/` | `DeviceActivityEventEntity` (tipo, timestamp) + DAO + `TelemetryRepository`. | Room |
| **Consumidor** | `domain/sleep`, futuro `domain/project` | Lee hechos crudos y los interpreta. | **NO — puro** |

Notas:
- La capa `platform/` **todavía no existe**. El telemetry-core sería su **primer
  ciudadano legítimo**.
- `sleep/SleepDeviceAdminReceiver.kt` está mal ubicado en la raíz; eventualmente
  debería mudarse a `platform/` (limpieza aparte, no parte de este scope).
- Es un **paquete**, NO un módulo Gradle (regla #7). Un solo `:app` por ahora.

---

## 3. El contrato de hechos a definir (corazón del scope)

El entregable central es el **contrato de hechos crudos** que telemetría emite y
los consumidores leen. A definir:

- **Forma del evento**: ¿`DeviceActivityEvent(type: UNLOCK|LOCK, timestamp)`? ¿se
  guardan ambos bordes o solo transiciones? ¿se agrupan en sesiones de pantalla?
- **Granularidad**: ¿cada evento individual, o ventanas agregadas (ej. minutos de
  pantalla activa por bloque horario)?
- **Retención**: ¿cuánto se guardan los eventos crudos? ¿se purgan tras
  materializar la inferencia del consumidor?
- **Esquema Room**: nueva entidad + migración numerada (el esquema está en v10;
  ver `AutonomiaDatabase.kt`). Agregar `MigrationTestHelper` (índices
  `index_<tabla>_<col>`, no `idx_*` — bug conocido #587).

> El contrato debe ser **genérico**: nada de "wakeUp"/"sleep" en el telemetry-core.
> Eso es interpretación del consumidor. Telemetría habla de unlock/lock, no de
> sueño.

---

## 4. Riesgo de plataforma Android (investigar en el explore)

La detección de pantalla en background es el riesgo técnico principal. A validar
con Context7 (Room/Compose/Android actuales) y/o docs oficiales:

- `ACTION_SCREEN_ON/OFF` y `USER_PRESENT` solo funcionan con un `BroadcastReceiver`
  **registrado en runtime** mientras un proceso vive; Android moderno mata procesos
  en background. Un receiver de manifest **no** recibe estos eventos.
- Opciones a evaluar (tradeoffs de batería, permisos, fiabilidad nocturna):
  - **Foreground service** con notificación persistente (fiable, pero visible).
  - **`UsageStatsManager`** (requiere permiso especial `PACKAGE_USAGE_STATS`,
    el usuario lo concede en Ajustes; da uso histórico, no tiempo real).
  - **WorkManager** con muestreo periódico (menos preciso, sobrevive al sistema).
- **Dirección decidida (D2):** `UsageStatsManager` (fuente; permiso `PACKAGE_USAGE_STATS`)
  + `WorkManager` (drenaje periódico) → Room. **Sin foreground service** (no hay tiempo
  real, D1). Lo que queda para el `sdd-explore` es **validar el modelo real de eventos**
  del API y el **gotcha `minSdk 26`**: `UsageEvents.Event.SCREEN_INTERACTIVE` /
  `SCREEN_NON_INTERACTIVE` es **API 28+**; en API 26/27 hay que reconstruir con otros
  eventos (p. ej. `KEYGUARD_HIDDEN`/`SHOWN`, `ACTIVITY_RESUMED`/`PAUSED`). El mecanismo
  condiciona el contrato de hechos (§3).

---

## 5. Cómo arrancar (sesión nueva)

1. Recuperar contexto de Engram (`mem_search` + `mem_get_observation`):
   - `sdd/sleep-redesign/findings` (#593) — hallazgos de Sueño + esta dirección.
   - `sdd-init/apk-personal` — capacidades de testing + Strict TDD.
2. Leer este handoff + `docs/dominio/arquitectura-recomendada-autonomia.md` (capa Platform)
   + `docs/sueno/sleep-feature-preliminar.md` (el primer consumidor).
3. Lanzar **`sdd-explore`** de `device-telemetry`: mecanismo de detección Android
   (§4), contrato de hechos (§3), ubicación (§2). NO mezclar con el consumidor
   (Sueño) — es otro scope.
4. Tras explorar: proponer (`sdd-propose`) y seguir el ciclo SDD.

---

## 6. Preguntas — resueltas vs abiertas

**Resueltas** (ver "Decisiones tomadas"):

- ✅ ¿Tiempo real o reconstrucción? → **reconstrucción** (D1).
- ✅ ¿Mecanismo de detección? → **`UsageStatsManager` + `WorkManager`, sin foreground
  service** (D2).
- ✅ ¿Arranca solo o corre siempre? → **opt-in, un interruptor**; corre periódico
  mientras una feature consumidora esté activa (D4).
- ✅ ¿Cómo se maneja la privacidad de datos de apps? → **local-first + gating de
  recolección** (D5).

**Abiertas** (para el `sdd-explore`):

- ⬜ Validar el **modelo real de eventos** de `UsageStatsManager` y el **gotcha
  `minSdk 26`** (`SCREEN_INTERACTIVE` es API 28+). Validar con Context7.
- ✅ **D6 confirmado:** el hecho crudo guarda el `paquete` (qué app) desde el día 1.
- ⬜ **Forma y granularidad** exacta del hecho crudo: ¿evento individual o ventanas
  agregadas? (§3)
- ⬜ **Retención/purga** de los eventos crudos. Nota: `UsageStats` del OS se borra en
  pocos días; si el scoring semanal necesita historia, hay que **materializar** en Room
  y definir la purga.
- ⬜ **Nombre/esquema** de la entidad Room + número de migración (esquema en v10).

**Fuera de este scope** (pertenecen a Sueño como consumidor, NO a telemetría):

- Flujos manual vs automático y su convivencia (¿cuál gana?).
- "¿Despertó de verdad?" (umbral/debounce), noche fragmentada, frontera detox/inicio de
  sueño, cierre de la noche. Ver `docs/sueno/sleep-feature-preliminar.md` §3.

---

## 7. Secuencia global (recordatorio)

```
1. device-telemetry (ESTE handoff) → contrato de hechos genérico
2. Sueño como consumidor          → docs/sueno/sleep-feature-preliminar.md
3. Tests con valores confiables    → recién cuando 1 y 2 estén cerrados
```

Otras features (anclas, soportes, sobriedad, tasks) son más livianas y van después,
cada una con su propio `sdd-explore` (Fase A del handoff de scoring).

---

## 8. Glosario para lectura no técnica

Para que el dueño del proyecto pueda releer este doc sin perderse en términos:

- **`UsageStatsManager`** — un **cuaderno de bitácora que Android lleva solo**: anota
  cuándo se prende/apaga la pantalla y qué app estuvo abierta. Es lo que alimenta la
  pantalla de "Bienestar Digital" del teléfono. La app **no vigila en vivo**: le pregunta
  a ese cuaderno cada tanto.
- **`WorkManager`** — un **despertador para tareas de la app**: "cada tanto, despertá,
  hacé este mandado chico y volvé a dormir". No corre todo el tiempo (no gasta batería de
  noche).
- **Hecho crudo** — un dato **tal cual pasó**, sin interpretar (ej. "pantalla
  desbloqueada a las 3:00"). Telemetría solo produce hechos.
- **Consumidor** — la feature que **lee** los hechos y decide qué significan (ej. sueño
  concluye "ese desbloqueo de 30s no fue despertar").
- **PULL** — el consumidor **va a buscar** los hechos a Room; telemetría no le **envía**
  nada a nadie.
- **Dominio** — la parte del código que **interpreta hechos** y produce significado (un
  número de sueño 0..1). No toca base de datos ni pantalla.
- **Plataforma (capa)** — la parte que **toca Android directamente** (permisos, sensores,
  captura). Telemetría vive acá.
- **Opt-in** — **apagado** hasta que el usuario lo **prende a propósito**.
- **Migración (Room)** — instrucción para actualizar la estructura de la base local sin
  perder datos cuando se agrega una tabla nueva (acá: la tabla de hechos crudos).
