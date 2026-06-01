# Vocal / Autonomía sin límites — Mapa de componentes v0.2

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Estado: borrador vivo\
Propósito: ordenar los componentes actuales de la app antes de cerrar scoring, configuración o refactors técnicos.

---

## 1. Corrección de enfoque

La versión anterior fue demasiado rápida porque intentó convertir en documento algo que todavía necesitaba exploración.

Este documento debe funcionar como mapa de trabajo, no como especificación cerrada.

La pregunta central no es solo:

```text
¿Qué componentes tiene la app?
```

Sino también:

```text
¿Qué componentes son obligatorios para que el sistema exista?
¿Qué componentes son opcionales?
¿Qué componentes cambian la forma de calcular el score?
¿Qué componentes solo aparecen si el usuario los activa?
```

---

## 2. Regla arquitectónica base

```text
Room guarda hechos.
El dominio interpreta hechos.
Compose presenta estado y envía acciones.
```

Esto sigue siendo la regla central.

Pero ahora hay que añadir una segunda regla:

```text
No todos los usuarios activan los mismos componentes.
El score debe calcularse contra la base configurada del usuario, no contra una plantilla universal.
```

---

## 2.1 Mapa de capas técnicas (actualizado v12)

El sistema implementa estas capas de paquetes. Se lista aquí para que el mapa de
componentes no diverja del código real.

```text
platform/telemetry/          — captura de hechos del dispositivo (UsageStats)
                               sin conceptos de dominio; solo eventos crudos.
data/local/                  — entidades Room, seed, mappers
data/repository/             — AutonomiaRepository, TelemetryRepository
data/worker/                 — DailyClosureWorker, DeviceTelemetryDrainWorker
domain/sleep/                — scoring de sueño (SleepScoring, SleepPolicy)
domain/sleep/interpretation/ — interpretación de eventos → NightTimeline
                               (SleepInterpreter, SleepModels, InterpretationParams)
domain/scoring/              — motor de score semanal (*Policy.kt)
domain/abstinence/           — lógica de abstinencias
domain/activity/             — lógica de actividades
domain/closure/              — cierre diario
domain/dashboard/            — proyección para dashboard
domain/task/                 — lógica de tasks
ui/dashboard/                — DashboardScreen
ui/sleep/                    — SleepConfigScreen
ui/scoring/                  — ScoringScreen
ui/anchors/                  — AnchorConfig
ui/supports/                 — SupportsConfigScreen
ui/sobriety/                 — SobrietyScreen
ui/tasks/                    — TasksScreen
```

Flujo de datos del sueño (v12):

```text
UsageStats (Android)
  → platform/telemetry/TelemetryCaptureSource
  → DeviceTelemetryDrainWorker           (WorkManager)
  → DeviceActivityEventEntity            (Room, device_activity_events)
  → data/repository/TelemetryRepository
  → domain/sleep/interpretation/SleepInterpreter
      → NightTimeline (con SleepSegment[])
  → domain/sleep/SleepScoring
      → SleepNightScore (4 sub-scores + sleepScore)
  → SleepNightEntity + SleepSegmentEntity (Room, sleep_nights / sleep_segments)
  → domain/scoring/*Policy.kt            (incorporado al score semanal)
  → ScoreReport → DashboardProjection
  → Compose (DashboardScreen / ScoringScreen)
```

---

## 3. Núcleo real del negocio

El núcleo no debe confundirse con una lista de features.

El núcleo del negocio es el sistema mínimo que hace posible que Vocal mida y represente estabilidad.

### 3.1 Core obligatorio

Estos componentes son obligatorios porque sin ellos la app no puede funcionar como Vocal.

| Componente                       | Rol                          | Por qué es core                                                              |
| -------------------------------- | ---------------------------- | ---------------------------------------------------------------------------- |
| Mis anclas                       | Entrada principal de datos   | Sin anclas no hay hábitos base que medir.                                    |
| Sueño                            | Base fisiológica del scoring | El sueño debe pesar fuerte porque condiciona estabilidad, cuerpo y conducta. |
| Cinco capas                      | Modelo conceptual            | Organizan dónde impactan las acciones.                                       |
| Score de base                    | Lectura acumulada            | Traduce hechos en una lectura operativa.                                     |
| Estado de base                   | Representación visible       | Comunica al usuario cómo va su base.                                         |
| Tono de comunicación             | Política de producto         | Evita culpa, vergüenza y lecturas clínicas.                                  |

Conclusión:

```text
Anclas + sueño + capas + score + estado + tono = núcleo mínimo de Vocal.
```

---

## 4. Mis anclas como core

Nombre UI recomendado:

```text
Mis anclas
```

Antes:

```text
Checklist principal
```

Concepto técnico probable:

```text
ActivityDefinition con UserActivityConfig de ActivitySurface.Anchor
```

Definición:

```text
Prácticas recurrentes que el usuario elige porque sostienen su base personal.
```

Ejemplos:

- meditar;
- hacer ejercicio;
- leer;
- escribir;
- avanzar proyecto;
- no celular en cama.

Reglas:

- son obligatorias para el sistema;
- deben ser pocas;
- son la fuente inicial de datos de estabilidad;
- alimentan las capas;
- son la base del score inicial;
- no deben convertirse en una lista infinita.

Sin anclas, la app no tiene suficientes hechos diarios para calcular una lectura realista.

---

## 5. Sueño como core obligatorio

Nombre UI:

```text
Sueño
```

Concepto técnico (v12, activo):

```text
SleepNightEntity + SleepSegmentEntity
(reemplazan a SleepLogEntity, dropeada en migración 11→12)
```

Definición:

```text
Registro automático (vía telemetría del dispositivo) o manual del ritmo de sueño
como base fisiológica y conductual del estado del usuario.
```

El sueño debe considerarse core porque:

- condiciona energía;
- condiciona autocontrol;
- condiciona recaídas;
- afecta cuerpo;
- afecta conducta;
- afecta claridad mental;
- puede anticipar deterioro de la base.

Regla conceptual:

```text
El sueño sienta el piso del score.
```

Esto no significa que sueño sea lo único que importa.

Significa que, si el sueño está muy deteriorado, el sistema debe leerlo como una señal fuerte de inestabilidad aunque algunas tareas estén completas.

Ejemplo:

```text
Usuario completó varias anclas,
pero durmió 3 horas durante varios días.

Resultado:
El score no debería subir demasiado.
Debe aparecer una señal de sueño/ritmo.
```

Modelo Room vigente (`SleepNightEntity`, tabla `sleep_nights`):

```text
nightDate          — PK ISO yyyy-MM-dd, fecha del despertar
targetSleepAt      — objetivo de hora de dormir (e.g. "23:30")
targetWakeAt       — objetivo de hora de despertar (e.g. "07:30")
sleepOnsetAt       — epoch ms; null si NoData
definitiveWakeAt   — epoch ms; null si NoData
confidenceLevel    — High | Ambiguous | NoData
durationScore      — sub-score cacheado (recalculable)
continuityScore    — sub-score cacheado
alignmentScore     — sub-score cacheado
digitalInterruptionScore — sub-score cacheado
sleepScore         — null cuando NoData
note               — texto libre
source             — "auto" | "manual"
updatedAt          — epoch ms
```

Modelo de segmentos (`SleepSegmentEntity`, tabla `sleep_segments`):

```text
id          — PK autoincrement
nightDate   — FK → sleep_nights.nightDate
startAt     — epoch ms
endAt       — epoch ms
kind        — "Asleep" | "AwakeUse"
```

Nota: el campo `quality` fue eliminado (bug §10). El scoring usa el pipeline de
4 componentes: duración (0.40), continuidad (0.25), alineación (0.20),
interrupción digital (0.15).

Regla UX:

```text
Registrar sueño debe ser muy simple.
No debe sentirse como tracking médico complejo.
```

### 5.1 Sueño por telemetría — IMPLEMENTADO

> **Actualización (v12):** esto ya no es una "idea futura". El pipeline de
> inferencia automática de sueño está implementado y activo.

La estimación de sueño a partir de patrones de uso del teléfono está en producción:

**Capa `platform/telemetry/`** — captura eventos de dispositivo (UsageStats API):

```text
TelemetryCaptureSource       — fuente de eventos; lee UsageStats
UsageStatsTelemetrySource    — implementación concreta vía Android UsageStats
DeviceTelemetryDrainWorker   — WorkManager worker; drena eventos a Room
DeviceTelemetryWorkScheduler — programa el worker
TelemetryPermission          — verifica permiso PACKAGE_USAGE_STATS
TelemetryEventMapper         — mapea UsageStats → DeviceActivityEvent
TelemetryGatingPolicy        — controla cuándo se drena
TelemetryRetentionPolicy     — define cuánto tiempo se retienen eventos
DeviceActivityEvent          — modelo puro: (eventType, packageName, timestamp, source)
DeviceActivityEventType      — enum: SCREEN_ON, SCREEN_OFF, UNLOCK, LOCK,
                               APP_FOREGROUND, APP_BACKGROUND, USER_INTERACTION
```

Entidades Room asociadas: `DeviceActivityEventEntity` (device_activity_events) +
`TelemetryCollectionLeaseEntity` (telemetry_collection_lease).

`TelemetryRepository` vive en `data/repository/`.

**Capa `domain/sleep/interpretation/`** — interpreta eventos en línea de tiempo de sueño:

```text
SleepInterpreter     — objeto puro JVM; convierte List<DeviceActivityEvent>
                       → NightTimeline. Lógica: ventana 20:00 D-1 → 12:00 D,
                       agrupa AwakeUse, detecta onset/wake, asigna confidence.
SleepModels.kt       — modelos de dominio de interpretación:
  SleepSegment         — segmento (startAt, endAt, kind: Asleep|AwakeUse)
  SleepSegmentKind     — enum: Asleep, AwakeUse
  SleepConfidence      — enum: High, Ambiguous, NoData
  NightTimeline        — resultado: nightDate, segments, sleepOnsetAt,
                         definitiveWakeAt, confidence
InterpretationParams — umbrales calibrables (quietGapMillis, etc.)
```

**Resto de la capa `domain/sleep/`**:

```text
SleepScoring         — aplica el pipeline de 4 componentes a un NightTimeline
SleepNightScore      — resultado del scoring: 4 sub-scores + sleepScore final
SleepPolicy          — reglas de política (e.g., qué cuenta como NoData)
SleepScoringParams   — pesos y umbrales del scoring
```

Fuentes de sueño disponibles:

| Fuente | Estado |
| --- | --- |
| Telemetría del teléfono (UsageStats) | **Implementado** (`platform/telemetry/`) |
| Registro manual | **Disponible** (source="manual" en SleepNightEntity) |
| Smartwatch / wearables | No implementado |
| Bloqueo nocturno de teléfono | No implementado |

Configuración del usuario: `SleepConfigEntity` (tabla `sleep_config`) guarda
`targetSleepAt` y `targetWakeAt`. Pantalla: `SleepConfigScreen` (`ui/sleep/`).

### 5.2 Idea futura: bloqueo nocturno como ritual de sueño

Esta idea sigue pendiente. El pipeline de detección ya existe; el bloqueo nocturno
activo como "ritual" no está implementado y requiere un SDD propio antes de encararse.

---

## 6. Capas como modelo de dominio

Las capas no son feature.

Son el modelo conceptual que responde:

```text
¿Qué parte de mi vida se está sosteniendo o cayendo?
```

Capas finales:

```text
1. Interior
2. Cuerpo
3. Conducta
4. Vínculos
5. Proyecto
```

Las capas reciben información desde:

- Mis anclas;
- Soportes;
- tareas;
- sueño;
- abstinencias;
- riesgo;
- futuras señales automáticas.

Las capas no calculan solas.

El dominio interpreta los hechos asociados a ellas.

---

## 7. Score y estado como núcleo de lectura

### Score de base

El score no es una feature editable.

Es una inferencia del dominio.

Debe representar:

```text
Qué tanto está sosteniendo el usuario su base personal configurada.
```

No debe representar:

- felicidad;
- valor personal;
- salud mental clínica;
- productividad;
- moral.

### Estado de base

El estado es la forma visible de comunicar el score y las señales.

Estados:

| Estado         | Lectura                                  |
| -------------- | ---------------------------------------- |
| Sin datos      | Todavía no hay registros suficientes.    |
| Restauración   | Base baja; prioridad: no empeorar.       |
| Atención       | Hay margen, pero la base está cediendo.  |
| En marcha      | Base activa y suficientemente sostenida. |
| Plenitud       | Base sostenida con consistencia alta.    |
| Inquebrantable | Núcleo muy sólido; pico orgánico.        |

Regla:

```text
En marcha es el hogar operativo de la app.
Plenitud e Inquebrantable son estados difíciles y no deben sentirse obligatorios.
```

---

## 8. Features opcionales o progresivas

No todo lo importante es obligatorio desde el inicio.

Hay componentes que pueden activarse según el tipo de usuario o nivel de compromiso.

| Componente         | Tipo                   | Obligatorio                       | Comentario                                           |
| ------------------ | ---------------------- | --------------------------------- | ---------------------------------------------------- |
| Soportes            | Feature progresiva     | No necesariamente al inicio       | Puede aparecer cuando el usuario quiere más soporte. |
| Pendientes / Tasks | Feature progresiva     | No                                | Requiere más compromiso con la app.                  |
| ActivityTarget     | Configuración opcional | No                                | Ayuda a leer consistencia.                           |
| Abstinencias       | Feature opt-in         | Solo para usuarios que la activan | Puede pesar fuerte si está activa.                   |
| Modo riesgo        | Feature protectora     | Probablemente opcional al inicio  | Muy relevante para usuarios de riesgo.               |
| Frases ancla       | Soporte transversal    | Sí en dashboard                   | No requiere configuración del usuario.               |

---

## 9. Soportes como feature progresiva

Nombre UI:

```text
Soportes
```

Antes:

```text
Cuidado base / Checklist secundaria
```

Concepto tecnico:

```text
ActivityDefinition con UserActivityConfig de ActivitySurface.Support
```

Definicion:

```text
Acciones simples de mantenimiento diario que sostienen cuerpo, dignidad y estructura.
```

UX inversa:

```text
El usuario marca lo que NO hizo. El sistema asume que todo esta hecho
por defecto y solo registra omisiones. A diferencia de las Anclas,
donde el usuario marca lo que SI hizo.
```

Ejemplos:

- banarse;
- cepillarse los dientes;
- comer algo simple;
- tomar agua;
- cambiarse de ropa;
- ordenar minimo.

Regla conceptual:

```text
Soportes no es productividad.
Es dignidad y mantenimiento minimo.
```

Relacion con Anclas:

```text
Las Anclas son la base personal activa que el usuario quiere sostener.
Los Soportes son el piso minimo que deberia estar cubierto sin esfuerzo consciente.
No compiten: las Anclas pesan mas en el score, los Soportes protegen contra el abandono.
```

Puede ser especialmente util para:

- usuarios cansados;
- usuarios en restauracion;
- usuarios saliendo de abandono;
- usuarios que necesitan volver al cuerpo.

Pendiente:

Definir si aparece desde onboarding o si se activa progresivamente.

---

## 10. Pendientes / Tasks como feature de compromiso mayor

Nombre UI:

```text
Pendientes
```

Concepto técnico:

```text
Task
```

Definición:

```text
Acciones puntuales que pueden o no contribuir a estabilidad.
```

No son hábitos.

No son anclas.

Pueden ser:

```text
Neutral
Support
Protective
Recovery
```

Ejemplos neutrales:

- comprar cuerdas;
- buscar una referencia;
- ordenar un archivo.

Ejemplos relevantes para estabilidad:

- pagar alquiler;
- pedir cita médica;
- resolver trámite urgente;
- llamar a alguien para reparar una conversación.

Regla:

```text
Las tasks no deben ser necesarias para que un usuario base use Vocal.
```

Pero pueden enriquecer el score de usuarios más comprometidos.

---

## 11. ActivityTarget / objetivos de consistencia

Corrección conceptual:

```text
Goal no es feature independiente.
Goal tampoco es métrica.
```

Término recomendado:

```text
ActivityTarget
```

Definición:

```text
Objetivo opcional de consistencia asociado a una Activity.
```

Diferencia:

| Concepto        | Ejemplo                     |
| --------------- | --------------------------- |
| Métrica         | Medité 7 minutos.           |
| Target          | Meditar 5 veces por semana. |
| Señal calculada | Meditación 3/5 esta semana. |

Reglas:

- no tener target no castiga;
- cumplir target ayuda a leer consistencia;
- targets pueden ayudar a llegar a estados superiores;
- deben sentirse simples en UI;
- no deben crear una pantalla pesada de planificación.

Ejemplo UI:

```text
Meditar 3/5 esta semana
Ejercicio 1/3 esta semana
```

La complejidad la calcula el dominio.

---

## 12. Abstinencias como feature opt-in de alto impacto

Nombre UI posible:

```text
Rachas protectoras
```

Nombre técnico:

```text
AbstinenceTrack
AbstinenceLog
```

Definición:

```text
Feature opcional para usuarios que quieren dejar alcohol, sustancias o conductas perniciosas.
```

Presets iniciales:

```text
Alcohol
Sustancias
Conducta sexual
```

Regla clave:

```text
Si una abstinencia no está activa, no aparece, no pesa y no limita el estado.
```

Si el usuario la activa:

```text
entra en su sistema personal de estabilidad;
pesa más que una actividad común;
puede generar señales críticas;
puede limitar estados altos si hay recaída reciente.
```

Estados diarios:

```text
Unknown
Clean
Relapse
```

Reglas:

- `Unknown` no es recaída automática;
- `Clean` es señal positiva protectora;
- `Relapse` en abstinencia crítica es señal roja;
- registrar impulso sin actuar debe contar como señal positiva/protectora;
- nunca debe comunicarse con vergüenza.

Esta feature no está dirigida a todos los usuarios.

Está dirigida especialmente a usuarios que no solo quieren ordenar su vida, sino salir de un ciclo más destructivo.

---

## 13. Tipos de usuario — hipótesis inicial

Esta sección no debe cerrarse todavía.

Sirve para pensar cómo cambia la configuración y el score según el nivel de necesidad del usuario.

### 13.1 Usuario base

Definición:

```text
Usuario que descarga Vocal porque está en un mal momento y busca una ayuda simple para salir del pozo.
```

Configuración probable:

```text
Mis anclas
Sueño
Frases ancla
Estado de base
Score de base
```

Características:

- necesita baja fricción;
- no quiere configurar muchas cosas;
- puede estar cansado, triste, disperso o saturado;
- necesita pocas acciones claras;
- probablemente no usará tasks ni objetivos complejos al inicio.

Scoring:

```text
Debe basarse principalmente en anclas + sueño + capas.
```

Objetivo realista:

```text
Llegar a En marcha.
```

---

### 13.2 Usuario comprometido / intermedio

Definición:

```text
Usuario que ya empezó a confiar en la app y quiere ordenar mejor su vida diaria.
```

Configuración probable:

```text
Mis anclas
Sueño
Soportes
Pendientes
ActivityTargets
Resumen semanal
```

Características:

- tolera más estructura;
- quiere ver progreso;
- puede registrar más cosas;
- empieza a usar objetivos de consistencia;
- puede integrar tasks relevantes.

Scoring:

```text
Sigue usando el mismo core,
pero ahora incorpora cuidado base, targets y tasks relevantes.
```

Objetivo realista:

```text
Sostener En marcha y eventualmente tocar Plenitud.
```

---

### 13.3 Usuario de riesgo / recuperación

Definición:

```text
Usuario que no solo quiere ordenar hábitos, sino dejar alcohol, sustancias o conductas perniciosas.
```

Configuración probable:

```text
Mis anclas
Sueño
Abstinencias
Modo riesgo
Soportes
Frases de contención
```

Características:

- necesita protección fuerte;
- puede estar saliendo de ciclos destructivos;
- requiere tono especialmente cuidadoso;
- no debe ser castigado por estar en recuperación;
- necesita señales claras antes de recaer.

Scoring:

```text
El core sigue siendo anclas + sueño + capas,
pero las abstinencias activas modifican fuertemente la lectura.
```

Regla:

```text
Una recaída no es una tarea fallida.
Es una señal crítica de protección y recuperación.
```

Objetivo realista:

```text
Primero Restauración segura.
Luego Atención.
Luego En marcha.
```

---

## 14. Implicancia para el scoring

Esta sección todavía no define la fórmula final.

Solo corrige la fuente conceptual del score.

Error a evitar:

```text
No decir que el score sale directamente de anclas + sueño + señales básicas.
```

Corrección:

```text
El score debería salir principalmente de sueño + lectura por capas.
```

Las anclas, cuidado base, tasks, abstinencias, modo riesgo y otros registros no compiten como fuentes paralelas del score.

Más bien:

```text
Alimentan las capas.
Las capas, junto con sueño, alimentan el score.
```

Mapa conceptual:

```text
Mis anclas
Soportes
Tasks relevantes
Abstinencias activas
Modo riesgo
ActivityTargets
Futuras señales automáticas
        ↓
Capas de vida
        ↓
Score de base
        ↓
Estado de base
```

Pero sueño tiene un trato especial:

```text
Sueño no es solo una actividad dentro de Cuerpo.
Sueño funciona como piso fisiológico/conductual del score.
```

### 14.1 Modelo conceptual provisional

Todavía hay dos modelos posibles por explorar.

#### Modelo A — Sueño como piso de arranque

```text
Sueño sostiene gran parte del rango base.
Las capas completan el resto del score.
```

Idea aproximada:

```text
Sueño ayuda a sostener el mínimo operativo.
Capas permiten avanzar hacia En marcha / Plenitud.
Consistencia prolongada permite estados superiores.
```

#### Modelo B — Sueño como bloque fuerte dentro del score

```text
Sueño podría pesar una cantidad muy alta,
por ejemplo 500 puntos internos,
y las capas completar hasta cierto rango.
```

Luego, los estados superiores no dependerían solo de completar cosas un día, sino de consistencia real.

Ejemplo conceptual:

```text
700-800  = base mínima / recuperación / atención.
800-900  = rango operativo esperable para la mayoría de usuarios activos.
900-1000 = consistencia alta, difícil y realista; no estado común.
```

Esta distribución todavía no está cerrada.

### 14.2 Regla importante

```text
El score no debe ser una suma universal de puntos.
```

Debe leer:

```text
sueño
+ capas
+ configuración activa del usuario
+ consistencia
+ señales críticas
```

Pero sin penalizar features no activadas.

Ejemplo:

```text
Usuario sin abstinencias activas
→ no se le exige sobriedad como métrica.
```

```text
Usuario con alcohol activo como abstinencia crítica
→ esa abstinencia alimenta fuertemente Conducta y puede afectar el estado.
```

### 14.3 Regla sobre módulos activos

Los módulos activos no deben entenderse como “extras que suman puntos” de forma plana.

Deben entenderse como fuentes de información para las capas o como señales especiales.

| Componente | Cómo afecta |
| --- | --- |
| Mis anclas | Alimentan capas según actividad. |
| Soportes | Alimenta principalmente Cuerpo y Conducta. |
| Tasks relevantes | Alimentan capa asociada si tienen aporte real. |
| ActivityTargets | Ayudan a leer consistencia, no son puntos directos. |
| Abstinencias activas | Alimentan Conducta y pueden generar señales críticas. |
| Modo riesgo | Genera eventos/señales protectoras o críticas. |
| Sueño | Piso fuerte del score y señal transversal. |

---

## 15. Fórmula conceptual provisional

No es fórmula matemática final.

Es solo una orientación corregida.

```text
Score de base ≈
Sueño como piso fuerte
+ lectura agregada de capas
+ consistencia temporal
+ ajustes por señales críticas o protectoras
```

Donde las capas se alimentan de:

```text
Mis anclas
Soportes
Tasks relevantes
Abstinencias activas
Modo riesgo
ActivityTargets
futuras señales automáticas
```

Reglas:

```text
Un mal día no destruye todo.
Un día perfecto no infla todo.
El sueño puede limitar estados altos.
Las capas deben representar equilibrio real.
Una recaída activa puede forzar Atención/Restauración.
No registrar no siempre equivale a fallar.
No usar una feature no activada no penaliza.
```

Pendiente:

```text
El score necesita un documento propio.
Este mapa solo define de dónde debería salir conceptualmente.
```

---

## 16. Dashboard

No desarrollar dashboard en este documento.

El dashboard ya está trabajado/documentado en otro lugar y no debe contaminar este mapa de componentes.

Regla:

```text
Este documento solo define componentes y responsabilidades.
El contrato visual/datos del dashboard debe vivir en un documento separado.
```

---

## 17. Preguntas abiertas

Estas preguntas deben resolverse antes del documento de score:

1. ¿Sueño debe ser obligatorio desde onboarding o puede registrarse después del primer día?
2. ¿Cuántas anclas mínimas necesita el usuario para empezar?
3. ¿Soportes aparece desde el inicio o se desbloquea/sugiere en restauración?
4. ¿Las abstinencias se preguntan en onboarding o se ofrecen después con cuidado?
5. ¿El usuario elige su tipo de uso o la app solo ofrece configuración progresiva?
6. ¿ActivityTarget aparece al crear ancla o solo después de usarla unos días?
7. ¿El score debe tener modos internos según componentes activos?
8. ¿Cómo se evita que usuarios de recuperación se sientan castigados por recaídas?
9. ¿Cómo se comunica que sueño pesa mucho sin sonar controlador?
10. ¿Qué mínimo de datos se necesita para salir de `Sin datos`?

---

## 18. Próximo paso recomendado

Este documento debe cerrarse como mapa general y luego dividirse.

Documentos separados recomendados:

```text
01-core-negocio-vocal.md
02-usuarios-y-configuracion-v1.md
03-sueno-core-y-telemetria-futura.md
04-abstinencias-v1.md
05-activity-targets-v1.md
06-score-base-v1.md
07-dashboard-contract-v1.md
```

No escribir todavía `score-base-v1.md` como fórmula final.

Antes conviene separar:

```text
- qué es core obligatorio;
- qué cambia por tipo de usuario;
- qué componentes alimentan capas;
- cómo sueño funciona como piso del score;
- qué queda como telemetría futura;
- qué componentes son opt-in;
- qué no debe penalizar al usuario si no está activo.
```

Después recién tiene sentido cerrar:

```text
score-base-v1.md
```

---

## 19. Decisión provisional

La app debe tener un núcleo común:

```text
Mis anclas + sueño + capas + score + estado + tono.
```

Y módulos activables:

```text
Soportes
Pendientes
ActivityTargets
Abstinencias
Modo riesgo
```

El score no debe medir una vida ideal universal.

Debe medir si el usuario está sosteniendo la base que configuró para salir, mantenerse o reconstruirse.

