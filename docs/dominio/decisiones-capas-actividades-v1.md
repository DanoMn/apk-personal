# Decisiones de diseño - Capas, actividades y modelo de datos

Fecha: 2026-05-20  
Proyecto: Vocal / Autonomía sin límites  
Objetivo: normalizar la estructura conceptual y de datos antes de ajustar Room, dominio y UI.

---

## 1. Alcance de esta versión

Esta versión NO intenta cerrar todavía el algoritmo de tracking, los pesos exactos, los umbrales ni una fórmula definitiva de estabilidad.

La meta de esta etapa es más básica y más importante:

```text
Definir qué cosas existen.
Definir dónde se guardan.
Definir qué significan.
Evitar mezclar checklist, hábitos, registros especializados y tareas puntuales.
```

El algoritmo inteligente vendrá después, cuando existan datos confiables.

---

## 2. Regla central

```text
Room guarda hechos.
El dominio interpreta hechos.
Compose presenta estado y envía acciones.
```

La app no mide una moral universal ni una vida ideal fija.

La app mide si la persona está sosteniendo su base personal configurable.

Ejemplo:

```text
Para una persona, alcohol puede ser un eje crítico.
Para otra persona, alcohol puede no existir en el sistema.

Para una persona, música puede ser proyecto vital.
Para otra persona, música puede ser una tarea neutral.
```

El modelo debe permitir esa diferencia desde el inicio.

---

## 3. Mapa conceptual corregido

```text
Capa
= dimensión visual estable del dashboard.

Feature
= módulo especializado que guarda hechos propios y puede alimentar una o más capas.

Activity
= acción recurrente/configurable que el usuario puede registrar.

ActivityLog
= hecho registrado de una Activity en una fecha o momento.

Task
= pendiente puntual. Puede contribuir o no a estabilidad, pero no es hábito.

AbstinenceTrack
= racha o hábito a dejar (ej. alcohol). No es una Activity común, es una feature propia protectora.

AnchorPhrase
= cita o frase mostrada en la UI que funciona como soporte o ancla psicológica, elegida según el ScoreState.

DashboardSignal
= lectura rápida mostrada en dashboard. Puede venir de actividades, features o tareas.

ContributionRole
= etiqueta cualitativa que dice si algo aporta a la estabilidad futura.
```

Separación clave:

```text
Activity no es ActivityLog.
Activity no es Task (pendientes).
Activity no es AbstinenceTrack (rachas a dejar).
FeatureLog (ej. AbstinenceLog) no es ActivityLog.
DashboardSignal no es Capa.
```

---

## 4. Capas finales

Se baja de 6 capas a 5 capas principales.

`Casa / comida` deja de ser capa principal porque funcionaba más como entorno, soporte doméstico o cuidado corporal.

Capas finales:

```text
1. Interior
2. Cuerpo
3. Conducta
4. Vínculos
5. Proyecto
```

Nota de migración:

```text
Actividades antiguas de Casa/comida deben moverse según intención:
- comida, agua, higiene, descanso -> Cuerpo
- orden doméstico, preparación del día -> Conducta
- tarea puntual de casa -> Task
```

---

## 5. Qué es una capa

Una capa responde:

```text
¿Qué parte de mi vida se está sosteniendo o cayendo?
```

Una capa NO responde:

```text
¿Cómo se mide esto?
¿Dónde aparece en pantalla?
¿Cuántos puntos vale?
```

La medición pertenece a `Activity` o a una `Feature`.

La aparición visual pertenece a `displaySurface`.

La contribución futura al algoritmo pertenece a `contributionRole` e `importanceTier`.

---

## 6. Capas y sentido de producto

### Interior

Mide si la persona está cultivando su mundo interno: conciencia, aprendizaje, reflexión, claridad y dirección personal.

No debe sentirse como religión obligatoria, espiritualidad forzada ni terapia falsa.

Ejemplos:

```text
- Meditar
- Leer
- Escribir
- Aprender
- Reflexionar
- Practicar gratitud mediante una acción concreta
- Revisar objetivos personales
- Desconectarse
```

### Cuerpo

Mide si la persona está sosteniendo su base física: movimiento, descanso, alimentación e higiene.

No es fitness, estética ni rendimiento deportivo.

Ejemplos:

```text
- Hacer ejercicio
- Caminar
- Dormir
- Comer
- Tomar agua
- Bañarse
- Cepillarse los dientes
- Cambiarse de ropa
- Estirarse
- Descansar
```

### Conducta

Mide patrones de autocontrol, límites, ritmo diario, higiene digital y prevención del autosabotaje.

No debe ser una lista de cosas negativas a evitar.

Debe medir tanto:

```text
evitar autosabotaje
+
construir orden conductual
```

Ejemplos:

```text
- Dejar celular fuera de la cama
- Apagar pantallas antes de dormir
- Ordenar cuarto
- Preparar el día siguiente
- Hacer primera tarea antes de redes sociales
- Cerrar el día con una revisión breve
- Escribir antes de tomar una decisión importante
- Posponer una decisión importante
- No responder mensajes desde rabia
```

Features que pueden alimentar Conducta:

```text
- Sobriedad / abstinencias
- Sueño
- Uso digital futuro
- Modo riesgo
- Eventos de riesgo
```

### Vínculos

Mide si la persona está sosteniendo contacto humano, relaciones importantes y acciones pendientes con otros.

No es tener muchos amigos, vida social intensa ni agradarle a todos.

En Vínculos hay tres cosas distintas:

```text
1. Hábito relacional
2. Objetivo de contacto
3. Pendiente relacional
```

Ejemplos de hábito:

```text
- Hablar con alguien importante
- Responder mensajes pendientes
- Llamar a un familiar
- Ver a alguien en persona
- Tener contacto social sano
```

Ejemplos de pendiente relacional:

```text
- Pedir perdón
- Poner un límite
- Reparar una conversación
- Hablar de algo difícil
```

Los pendientes relacionales deben vivir como `Task` o futura `RelationalTask`, no como checklist diaria normal.

### Proyecto

Mide si la persona está construyendo algo que le importa: futuro, identidad, trabajo, aprendizaje, creación o práctica.

No es solo productividad, trabajo o negocio.

Ejemplos de hábito de avance:

```text
- Avanzar Digitaliza
- Componer el EP
- Estudiar
- Practicar una habilidad
- Crear algo
```

Ejemplos de tarea puntual:

```text
- Corregir un bug específico
- Grabar una canción específica
- Enviar una propuesta
- Terminar un documento específico
```

Las tareas puntuales de proyecto deben vivir como `Task` o futura `ProjectTask`, no como `Activity`, salvo que se conviertan en práctica recurrente.

---

## 7. Superficies de registro

La app puede tener varias superficies para registrar cosas sin volver pesada la experiencia.

### Checklist principal

La checklist principal es corta.

Debe representar la base que el usuario quiere sostener porque siente que, si eso cae, su estabilidad empieza a bajar.

Ejemplos personales posibles:

```text
- Meditar
- Avanzar Digitaliza
- Componer el EP
- Gimnasio o caminata fuerte
- No celular en cama
```

No debe contener todo.

### Checklist secundaria

La checklist secundaria es opcional, ligera y gamificable.

Sirve para mantenimiento diario, cuidado personal y pequeñas acciones que sostienen estructura, pero no deberían competir con la base principal.

Ejemplos:

```text
- Bañarse
- Cepillarse los dientes
- Cambiarse de ropa
- Tomar agua
- Comer algo simple
- Orden mínimo
- Despertarse con algo de orden
```

Puede sumar a una lectura futura de estabilidad, pero con menor peso y sin sentirse como castigo.

### Tareas puntuales

La task list guarda pendientes concretos.

No todo pendiente suma estabilidad.

Ejemplos neutrales:

```text
- Comprar cuerdas para guitarra
- Buscar una referencia
- Ordenar un archivo
```

Ejemplos que sí pueden sostener estabilidad:

```text
- Pagar alquiler
- Pedir cita médica
- Resolver un trámite urgente
- Llamar a alguien para reparar una conversación
```

Por eso `Task` también debe tener `contributionRole`.

---

## 8. Activity vs ActivityLog

`Activity` define qué existe en el sistema y cómo se registra normalmente.

`ActivityLog` define qué pasó un día o momento concreto.

Ejemplo:

```text
Activity
- id: activity_meditation
- name: Meditar antes de dormir
- layerId: Interior
- measurementType: Time
- role: Practice
- displaySurface: PrimaryChecklist
- contributionRole: Core
- targetValue: 5
- minimumValue: 1
- unit: Minutes
```

Log:

```text
ActivityLog
- activityId: activity_meditation
- date: 2026-05-20
- actualValue: 7
- completed: true
- note: ""
```

Si mañana la meta cambia de 5 a 10 minutos, el log antiguo no debe cambiar. El hecho histórico sigue siendo: ese día se meditaron 7 minutos.

---

## 9. Activity vs Task

Una `Activity` es una acción recurrente o configurable.

Una `Task` es una acción puntual.

Ejemplo de Activity:

```text
Avanzar Digitaliza
- recurrente
- medible por tiempo
- parte de Proyecto
- puede estar en checklist principal
```

Ejemplo de Task:

```text
Corregir bug de login
- puntual
- puede completarse una vez
- puede aportar a Proyecto o ser neutral
```

Una tarea puede contribuir a estabilidad, pero no por eso debe convertirse en hábito.

---

## 10. Tipos de medición

Estos son tipos de medición, no capas ni frecuencia.

```text
Check
Time
Count
Note
TimeOfDay
```

### Check

Para acciones sí/no.

Ejemplos:

```text
- Bañarse
- Cepillarse los dientes
- Dejar celular fuera de la cama
- Responder mensajes pendientes
```

### Time

Para actividades donde importa cuánto tiempo se practicó.

Ejemplos:

```text
- Meditar
- Leer
- Hacer ejercicio
- Avanzar Digitaliza
- Componer el EP
```

### Count

Para cantidades.

Ejemplos:

```text
- vasos de agua
- comidas caseras
- sesiones realizadas
```

### Note

Para registros breves de contexto.

Ejemplos:

```text
- reflexión del día
- qué comí
- qué aprendí
```

### TimeOfDay

Para una hora concreta.

Ejemplos:

```text
- hora de dormir
- hora de despertar
```

Nota:

```text
Sueño puede usar TimeOfDay de forma provisional, pero a largo plazo debe ser feature propia.
```

---

## 11. Frecuencia y objetivos

`Weekly` y `Monthly` no deben ser `measurementType`.

Son frecuencia, periodo de objetivo o ventana de lectura.

Una actividad puede ser:

```text
measurementType: Time
targetValue: 40
unit: Minutes
targetCount: 3
targetPeriod: Week
```

Ejemplo:

```text
Gimnasio
- se mide en minutos por sesión
- objetivo sugerido: 40 min
- objetivo de consistencia: 3 veces por semana
```

No toda actividad necesita objetivo.

Campos opcionales:

```text
cadence
targetValue
minimumValue
targetCount
targetPeriod
```

Si una actividad no tiene objetivo, igual puede existir y registrarse.

---

## 12. Contribución a estabilidad

No se deben definir puntos exactos todavía.

Sí se deben guardar metadatos cualitativos para que el algoritmo futuro pueda interpretar la importancia de cada cosa.

### ContributionRole

```text
Core
- parte de la base personal elegida por el usuario.
- si cae varios días, importa.

Support
- sostiene estructura diaria, pero no es el centro.

Protective
- reduce riesgo o protege contra autosabotaje.

Recovery
- ayuda a volver después de una caída o día difícil.

Neutral
- no aporta directamente a estabilidad; solo es una tarea o registro.
```

### ImportanceTier

```text
Low
Medium
High
Critical
```

`ContributionRole` dice qué tipo de aporte hace.

`ImportanceTier` dice cuánto importa para ese usuario.

Ejemplo:

```text
Alcohol
- contributionRole: Protective
- importanceTier: Critical
- active: true
```

Para otro usuario:

```text
Alcohol
- active: false
```

Esto permite que el sistema sea personal, no moralista ni universal.

---

## 13. Campos recomendados para Activity

```text
Activity
- id: String
- layerId: String
- name: String
- description: String

- measurementType: String
  - Check
  - Time
  - Count
  - Note
  - TimeOfDay

- role: String
  - Practice
  - SelfCare
  - Boundary
  - DigitalHygiene
  - DomesticOrder
  - RelationalHabit
  - ProjectWork
  - Learning
  - Custom

- displaySurface: String
  - PrimaryChecklist
  - SecondaryChecklist
  - Compact
  - Contextual
  - Silent

- contributionRole: String
  - Core
  - Support
  - Protective
  - Recovery
  - Neutral

- importanceTier: String
  - Low
  - Medium
  - High
  - Critical

- cadence: String?
  - Daily
  - Weekly
  - Monthly
  - Custom
  - EventBased

- targetValue: Int?
- minimumValue: Int?
- targetCount: Int?
- targetPeriod: String?
  - Day
  - Week
  - Month

- unit: String
  - Boolean
  - Minutes
  - Count
  - Time
  - Text

- active: Boolean
- archived: Boolean
- sortOrder: Int
- createdAt: Long
- updatedAt: Long
```

Notas:

```text
active = participa actualmente en UI y métricas.
archived = retirado por el usuario, pero conserva historial.
```

---

## 14. Campos recomendados para ActivityLog

Versión MVP agregada por día:

```text
ActivityLog
- activityId: String
- date: String
- completed: Boolean
- actualValue: Int?
- note: String
- updatedAt: Long
```

Esto alcanza para checklist y dashboard inicial.

Limitación aceptada:

```text
activityId + date permite un registro por actividad por día.
```

Más adelante, si se necesitan múltiples registros por día, se puede migrar a:

```text
ActivityEntry
- id: String
- activityId: String
- occurredAt: Long
- date: String
- actualValue: Int?
- note: String
```

No implementar esa complejidad todavía salvo necesidad real.

---

## 15. Campos recomendados para Task

```text
Task
- id: String
- title: String
- description: String
- layerId: String?
- projectId: String?
- status: String
  - Pending
  - Done
  - Archived

- contributionRole: String
  - Support
  - Protective
  - Recovery
  - Neutral

- importanceTier: String
  - Low
  - Medium
  - High
  - Critical

- dueDate: String?
- completedAt: Long?
- createdAt: Long
- updatedAt: Long
```

Las tareas puntuales no deben aparecer en la checklist principal salvo que el usuario las promueva explícitamente.

---

## 16. Features separadas

No todo debe ser `Activity`.

### Sobriedad / abstinencias

Feature propia.

```text
AbstinenceTrack
- id
- name
- label
- active
- severity
- contributionRole
- importanceTier
- sortOrder
- createdAt
- updatedAt
```

```text
AbstinenceLog
- trackId
- date
- status
  - Unknown
  - Clean
  - Relapse
- urge
- urgeIntensity
- note
- updatedAt
```

Impacta principalmente:

```text
Conducta
Estado global
Cuerpo indirectamente
```

La sobriedad debe ser configurable. Si una abstinencia no aplica al usuario, no debe pesar.

### Sueño

Feature futura, probablemente núcleo general.

```text
SleepLog
- date
- sleepStartAt
- wakeAt
- durationMinutes
- quality opcional
- note opcional
- updatedAt
```

Impacta:

```text
Cuerpo
Conducta
Estado global
```

### Uso digital

Feature futura importante.

```text
PhoneUsageSnapshot
- date
- totalUsageMinutes
- appUsageBreakdown
- limitBreaches
```

Debe tratarse como señal de cuidado, no como vigilancia punitiva.

### Riesgo

Feature propia ya contemplada.

```text
RiskEvent
- id
- date
- createdAt
- intensity
- trigger
- actionTaken
- actedOnImpulse
- note
```

---

## 17. Dashboard

El dashboard no debe mostrar toda la checklist.

Debe mostrar lectura rápida:

```text
- estado calculado
- progreso global del día
- capas del día
- señales importantes
- sobriedad / abstinencias activas
- checklist principal compacta
- acceso a checklist secundaria
- acceso a task list
- resumen semanal
```

Separación visual:

```text
Capas
= lectura de dimensiones de vida.

Señales
= lecturas rápidas como sueño, cuerpo, proyecto, riesgo.

Sobriedad
= feature propia con rachas y marca diaria.

Checklist principal
= hábitos/prácticas elegidas como base.

Checklist secundaria
= mantenimiento diario y soporte gamificado.

Tasks
= pendientes puntuales.
```

---

## 18. Onboarding y presets

La app debe permitir dos caminos:

```text
1. Usuario configura su base personal.
2. Usuario acepta presets y empieza sin pensar demasiado.
```

Flujo sugerido:

```text
1. Explicar que Vocal organiza la base diaria en capas.
2. Mostrar las 5 capas.
3. Elegir 3 a 5 actividades para checklist principal.
4. Elegir acciones opcionales para checklist secundaria.
5. Activar o no features especializadas como sobriedad.
6. Permitir omitir y usar presets simples.
```

Criterio de presets:

```text
- pocos
- claros
- no invasivos
- representativos
- fáciles de registrar
- configurables después
```

---

## 19. Decisiones cerradas

```text
- Las capas finales son Interior, Cuerpo, Conducta, Vínculos y Proyecto.
- Casa/comida deja de ser capa principal.
- Capa no define medición.
- Weekly y Monthly no son measurementType.
- Activity define una acción recurrente/configurable.
- ActivityLog guarda hechos de una Activity.
- Task guarda pendientes puntuales.
- Una Task puede contribuir a estabilidad o ser neutral.
- Checklist principal es para la base personal elegida.
- Checklist secundaria es para soporte, cuidado personal y gamificación ligera.
- Sobriedad no es checklist: es feature propia.
- Sueño debe evolucionar a feature propia.
- Uso digital será feature futura importante.
- No se fijan puntos exactos todavía.
- Sí se guardan contributionRole e importanceTier para preparar el algoritmo futuro.
- Room guarda hechos; dominio calcula señales.
```

---

## 20. No implementar todavía

```text
- Algoritmo complejo de estabilidad.
- Pesos numéricos definitivos.
- Tracking automático de celular.
- Analytics avanzados.
- Export/import.
- Múltiples ActivityEntry por día salvo necesidad real.
```

---

## 21. Pendientes inmediatos para Codex

1. Revisar esquema Room actual.
2. Migrar seed de `layers` a 5 capas.
3. Reemplazar `activities.type` por `measurementType` o mapearlo claramente.
4. Agregar `role`, `displaySurface`, `contributionRole`, `importanceTier`, `cadence`, `targetCount`, `targetPeriod`, `archived`, `createdAt`, `updatedAt`.
5. Mover actividades de Casa/comida a Cuerpo, Conducta o Task según intención.
6. Crear entidad `Task` para pendientes puntuales.
7. Mantener `AbstinenceTrack`, `AbstinenceLog` y `RiskEvent` como features separadas.
8. Definir presets mínimos para checklist principal y secundaria.
9. Ajustar dashboard para leer capas, señales, sobriedad, checklist y tareas como superficies distintas.
10. No implementar todavía puntuaciones definitivas.

---

## 22. Próximo documento recomendado

Crear:

```text
docs/datos-room/presets-actividades-v1.md
```

Contenido:

```text
- actividades sugeridas por capa
- cuáles van a checklist principal
- cuáles van a checklist secundaria
- measurementType
- role
- displaySurface
- contributionRole
- importanceTier sugerido
- objetivo opcional
- presets de sobriedad activables, no obligatorios
```
