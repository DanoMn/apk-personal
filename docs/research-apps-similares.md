# Research - apps y marcos similares

Objetivo: mirar que ya existe en habit trackers, recovery apps, mood trackers, DBT/CBT y quantified-self para no inventar todo desde cero.

## Fuentes revisadas

- Loop Habit Tracker: https://github.com/iSoron/uhabits
- Loop sitio oficial: https://loophabits.org/
- Streaks App Store: https://apps.apple.com/us/app/streaks/id963034692
- I Am Sober Google Play: https://play.google.com/store/apps/details/?hl=en-US&id=com.thehungrywasp.iamsober
- Nomo: https://saynomo.com/
- Safety Plan VA Mobile: https://mobile.va.gov/app/safety-plan
- Stanley-Brown Safety Plan App: https://apps.apple.com/us/app/stanley-brown-safety-plan/id695122998
- SMART Recovery Urge Tool: https://smartrecoveryglobal.org/hubfs/SMART_The_Urge_Tool_Form_V3-2.pdf
- SMART Recovery Cost-Benefit Analysis: https://smartrecovery.org/hubfs/Tool%203.1f%20Cost-benefit%20analysis.pdf
- DBT diary card app example: https://www.diary-card.com/
- Bearable Google Play: https://play.google.com/store/apps/details?gl=US&hl=en-US&id=com.bearable
- Exist.io: https://exist.io/
- Fogg Behavior Model: https://www.behaviormodel.org/home
- Fabulous Help Center: https://help.thefabulous.co/en/support/solutions/articles/101000427430-how-does-fabulous-work-

## Hallazgos principales

### 1. Habit trackers buenos no son solo checklists

Loop Habit Tracker tiene varias ideas muy compatibles con esta app:

- Habit score / fuerza de habito en vez de solo racha.
- Horarios flexibles: diario, 3 veces por semana, cada dos dias, etc.
- Recordatorios por actividad.
- Graficos y estadisticas.
- Offline, sin cuenta y con privacidad.
- Exportacion de datos a CSV o SQLite.

Aplicacion para Autonomia:

- No depender solo de rachas.
- Crear una metrica tipo "fuerza" o "consistencia" por actividad/capa.
- Soportar frecuencia semanal desde el modelo de datos.
- Mantener local-first y sin cuenta.

### 2. Streaks aporta tipos de tarea utiles

Streaks destaca por:

- Tareas negativas para romper malos habitos.
- Tareas con timer.
- Frecuencias configurables.
- Notas por tarea.
- Integracion con datos de salud en iOS.

Aplicacion para Autonomia:

- Nuestro tipo "abstinencia / no hacer" esta bien fundamentado.
- El tipo "tiempo" debe poder ser timer o input manual.
- Las notas por actividad pueden ser muy valiosas.
- Integraciones externas no entran ahora, pero el modelo debe permitir datos medidos.

### 3. Apps de sobriedad usan pledge, motivos, triggers y milestones

I Am Sober combina:

- Contador de sobriedad.
- Motivos para no consumir.
- Promesa diaria.
- Review al final del dia.
- Analisis de triggers.
- Milestones.
- Comunidad.

Nomo aporta:

- Multiples relojes de sobriedad o "no hacer".
- Refocus / mini ejercicios para cortar cravings.
- Journal simple.
- Milestones.
- Partners y comunidad.

Aplicacion para Autonomia:

- Mantener "por que no quiero repetir el ciclo" como modulo personal.
- Agregar un micro-pledge diario podria funcionar, pero sin tono moralista.
- Riesgo deberia registrar triggers, intensidad y accion tomada.
- Milestones sobrios pueden ser utiles, pero sin convertirlo en app de medallas vacias.
- Comunidad/partners no encajan con MVP local-first.

### 4. Safety Plan ya tiene una estructura clara para crisis

Safety Plan / Stanley-Brown divide crisis en pasos:

- Senales de advertencia.
- Estrategias internas de afrontamiento.
- Distracciones: lugares/personas.
- Personas de apoyo.
- Profesionales/agencias.
- Hacer el entorno mas seguro.
- Razones para vivir.

Aplicacion para Autonomia:

- El modo riesgo deberia evolucionar de "protocolo generico" a "plan personal".
- Debe incluir senales propias: madrugada, cama, celular, soledad, frustracion, alcohol cerca.
- Debe incluir acciones internas: agua, ducha, caminar, escribir, respirar.
- Debe incluir entorno: salir del cuarto, alejar alcohol/celular/detonante.
- Contactos/profesionales pueden quedar como fase posterior o configuracion opcional.

### 5. SMART Recovery aporta un modelo excelente para urges

El Urge Tool de SMART Recovery registra:

- Fecha y hora.
- Fuerza del urge.
- Duracion.
- Trigger.
- Quien/donde estuvo involucrado.
- Como se afronto.
- Ideas para la proxima vez.

La herramienta de Cost-Benefit Analysis separa:

- Beneficios/costos de hacer la conducta.
- Beneficios/costos de no hacerla.
- Enfasis en diferenciar satisfaccion inmediata vs metas futuras.

Aplicacion para Autonomia:

- Crear `RiskEvent` o `UrgeLog`.
- Medir urge de 1 a 10.
- Registrar si se actuo o no se actuo.
- Registrar coping usado.
- Despues de crisis, hacer debrief corto: que lo disparo, que sirvio, que hago la proxima vez.
- Cost-benefit no debe estar siempre visible, pero podria aparecer en modo riesgo avanzado.

### 6. DBT Diary Cards separan emociones, urges, conductas y habilidades

Los diary cards DBT suelen registrar:

- Emociones con intensidad.
- Urges con intensidad.
- Si se actuo o no sobre el urge.
- Habilidades usadas.
- Sueno, medicamentos o conductas objetivo segun el caso.
- Patrones semanales.

Aplicacion para Autonomia:

- Estado mental no debe ser una etiqueta suelta; puede derivarse de:
  - urges
  - intensidad
  - acciones criticas
  - habilidades usadas
  - sueno
  - progreso real
- Registrar "tuve urge pero no actue" es progreso real y debe contarse.
- El modo riesgo deberia celebrar resistencia, no solo castigar recaida.

### 7. Bearable y Exist muestran el valor de correlaciones, pero hay que tener cuidado

Bearable y Exist permiten:

- Registrar sintomas, humor, energia, actividades y factores.
- Buscar correlaciones.
- Usar unidades diversas: cantidad, tiempo, escala, porcentaje, hora del dia, tags.
- Ver patrones y warning signs.

Aplicacion para Autonomia:

- Nuestra decision de tipos medibles esta alineada con esto.
- A futuro se puede medir correlacion entre:
  - sueno y riesgo
  - gimnasio y estado
  - meditacion y urges
  - musica/Digitaliza y estabilidad
- Pero no conviene prometer causalidad. Debe decir "patrones", no "esto causa aquello".

### 8. Fabulous confirma la idea de progresion por niveles

Fabulous usa:

- Journeys.
- Rutinas manana/tarde/noche.
- Habitos pequenos que escalan.
- Coaching y programas guiados.

Aplicacion para Autonomia:

- La idea de niveles es correcta.
- Meditar 5 min puede escalar a 10, 15, 30.
- La app puede tener "rutas" o "protocolos" sin volverse una app de coaching.
- Evitar rigidez: esta app debe ser editable y personal, no una experiencia cerrada.

### 9. Fogg Behavior Model ayuda a disenar actividades

Fogg resume conducta como:

- Motivacion.
- Habilidad / facilidad.
- Prompt.

Aplicacion para Autonomia:

Cada actividad podria tener:

- Version minima: lo mas pequeno que cuenta.
- Version objetivo: lo que quiero sostener.
- Prompt: cuando o despues de que se hace.
- Barrera comun: energia, tiempo, entorno, animo.

Ejemplo:

- Actividad: Meditar.
- Version minima: 1 minuto sentado.
- Objetivo actual: 5 minutos antes de dormir.
- Prompt: despues de lavar dientes / antes de cama.
- Barrera: celular en cama.

Esto evita que la app se vuelva punitiva: si el objetivo falla, todavia se puede salvar la version minima.

## Funcionalidades candidatas para incorporar

### Alta prioridad

- Actividades por capas con tipos medibles.
- Frecuencias flexibles, especialmente 3 veces por semana.
- Tiempo objetivo vs tiempo real.
- Version minima vs objetivo.
- Progreso por capa.
- Estado calculado con reglas simples.
- RiskEvent / UrgeLog con intensidad, trigger, duracion, coping y resultado.
- Dashboard con frase diaria, estado calculado, progreso por capa y accesos rapidos.
- Drawer lateral.

### Prioridad media

- Pledge diario privado: "hoy no negocio con el bucle".
- Review nocturno breve.
- Motivos personales para no repetir el ciclo.
- Milestones sobrios o de practica.
- Notas por actividad.
- Barreras: por que no se hizo una actividad.
- Plan personal de riesgo basado en Safety Plan.
- Graficos de tiempo invertido por actividad/capa.

### Mas adelante

- Correlaciones entre sueno, comida, actividad y riesgo.
- Timer integrado para actividades.
- Export/import.
- Widgets.
- Recordatorios inteligentes.
- Configuracion de frases propias.
- Contactos de apoyo.

### Evitar por ahora

- Comunidad, partners o chat.
- Gamificacion RPG tipo Habitica.
- Medallas excesivas.
- Analytics complejos que parezcan diagnostico.
- Notificaciones agresivas.
- Correlaciones presentadas como causalidad.
- Demasiadas pantallas antes de cerrar el modelo de actividades.

## Implicacion fuerte para nuestro diseno

La app no deberia ser solo:

```text
capa -> lista de checks
```

Deberia ser:

```text
capa -> actividad editable -> objetivo -> registro real -> senal para progreso/estado
```

Modelo conceptual:

```text
Layer
  Activity
    type: check | abstinence | time | frequency | time_of_day | count | note
    minimumTarget
    currentTarget
    unit
    frequency
    prompt
    importance
    level

DailyLog
  ActivityLog
    actualValue
    completed
    note
    barrier

RiskEvent
  intensity
  duration
  trigger
  context
  copingUsed
  acted
  nextTime
```

## Recomendacion para la siguiente fase

Antes de programar, definir una especificacion corta:

1. Actividades predeterminadas por cada una de las seis capas.
2. Tipo de cada actividad.
3. Objetivo inicial y unidad.
4. Frecuencia.
5. Version minima.
6. Si la actividad pesa mucho o poco en el estado calculado.
7. Reglas iniciales del estado calculado.

La implementacion siguiente deberia usar este orden:

1. Cambiar modelo local a capas + actividades + logs.
2. Crear dashboard real.
3. Crear checklist por capas.
4. Agregar input de tiempo real para actividades de tiempo.
5. Agregar grafico de barras por capas.
6. Agregar RiskEvent simple.
7. Reemplazar bottom nav por drawer.
