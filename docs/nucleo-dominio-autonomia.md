# Nucleo de dominio - Autonomia sin limites

Este documento define el centro de la app. No es una arquitectura enorme; es una brujula para que el codigo no se convierta en tallarin cuando entren nuevas features.

## Idea central

La app no mide felicidad abstracta.

La app mide si el usuario esta sosteniendo la base diaria que le permite no autosabotearse.

Estar bien significa:

- sostener habitos base;
- cuidar el cuerpo;
- sostener sobriedad/abstinencias importantes;
- avanzar proyectos sin sacrificar la estructura;
- dormir y vivir con suficiente orden;
- detectar el bucle antes de caer.

Estar mal no significa "ser malo" o "fallar".

Estar mal significa:

- la base se esta desarmando;
- los habitos protectores desaparecen;
- aparecen conductas de escape;
- el cuerpo y la casa empiezan a abandonarse;
- aumenta la probabilidad de recaida o crisis.

## Filosofia operativa

En los mejores momentos, el usuario sostiene:

- ejercicio;
- meditacion;
- lectura;
- escritura;
- proyectos;
- cuidado basico;
- sobriedad.

Cuando esa base se pierde, suele ser reemplazada por:

- dormir tarde;
- uso excesivo del celular;
- no banarse;
- no cepillarse;
- no ordenar;
- conducta sexual compulsiva como escape;
- alcohol;
- aislamiento;
- decisiones desde cansancio, rabia o madrugada.

La app debe ayudar a detectar esa sustitucion antes de tocar fondo.

## Frase de dominio

> Si la base cae, el bucle vuelve. La app existe para ver la base, reconstruirla y cortar el circuito antes de caer.

## Conceptos separados del dominio

### 1. Sobriedad / abstinencias

No es una actividad comun.

Es una bandera critica de estabilidad.

Ejemplos:

- alcohol;
- conducta sexual;
- marihuana;
- otras abstinencias configurables.

Debe tener:

- racha;
- marca diaria;
- estado desconocido si no se registro;
- recaida;
- impulso;
- intensidad opcional;
- nota opcional.

### 2. Cuidado personal basico

No es productividad.

Es dignidad corporal y estructura minima.

Ejemplos:

- banarse;
- cepillarse dientes;
- cambiarse;
- comer algo decente;
- dormir con algo de orden;
- volver al cuerpo.

Debe ser tratado con lenguaje compasivo. No debe humillar.

### 3. Actividades de practica

Son acciones que construyen estabilidad y futuro.

Ejemplos:

- meditar;
- leer;
- escribir;
- gimnasio;
- caminar;
- musica;
- Digitaliza.

Pueden tener:

- objetivo;
- minimo valido;
- tiempo real;
- frecuencia diaria o semanal;
- capa;
- nivel.

### 4. Objetivos semanales

No todo se mide dia a dia.

Ejemplos:

- gimnasio 3 veces por semana;
- caminar 4 veces por semana;
- cocinar 4 veces por semana;
- leer 5 veces por semana.

### 5. Eventos de riesgo

No son estado manual.

Son hechos registrados:

- se abrio modo riesgo;
- hubo impulso;
- hubo detonante;
- se uso una estrategia;
- se evito o no se evito actuar.

### 6. Senales externas o automaticas futuras

Mas adelante:

- uso del celular;
- hora de dormir;
- hora de despertar;
- comida;
- datos corporales.

No deben ser usadas como vigilancia punitiva, sino como senales de cuidado.

## Dimensiones del dashboard

El dashboard debe tener una lectura global, pero no reducir todo a una sola palabra.

Debe mostrar dimensiones separadas.

Dimensiones iniciales:

- Sobriedad / abstinencias.
- Cuidado personal.
- Cuerpo / movimiento.
- Interior / mente.
- Proyecto / identidad.
- Sueno / ritmo.
- Uso del celular, futuro.

Ejemplo:

```text
Lectura general: bajo movimiento

Sobriedad       estable
Cuidado basico  alerta
Cuerpo          en pausa
Interior        en marcha
Proyecto        estable
Sueno           alerta
```

## Estados globales

Los estados no son diagnosticos.

Son lecturas operativas.

Estados candidatos:

- Sin datos: todavia no hay suficiente registro.
- En marcha: ya hay acciones protectoras hoy.
- Estable: la base esta sostenida.
- Bajo movimiento: faltan acciones basicas, pero aun hay margen.
- Riesgo: se acumulan senales de abandono o escape.
- Crisis: hay recaida, abandono fuerte o evento grave.
- Recuperacion: despues de crisis; el objetivo es no empeorar.

## Senales de alerta

### Alerta roja

Ejemplos:

- Beber alcohol.
- Recaida en abstinencia critica activa.
- No banarse o no cepillarse por mas de 3 dias.
- Uso de celular extremadamente alto, por ejemplo 12 horas.
- Varios dias consecutivos durmiendo tarde.
- Evento de riesgo fuerte.

### Alerta amarilla

Ejemplos:

- Un dia sin actividades protectoras.
- Falta de cuidado personal por 1 o 2 dias.
- No meditar/leer/moverse varios dias.
- Uso alto del celular, pero no extremo.
- Dormir tarde uno o dos dias.
- Mucho proyecto y cero cuerpo/cuidado.

### Senal positiva

Ejemplos:

- Cumplir version minima de una actividad.
- Registrar impulso y no actuar.
- Marcar dia sobrio.
- Banarse o cepillarse despues de varios dias mal.
- Volver a una actividad protectora.

## Reglas iniciales del estado calculado

Nota de prioridad:

- Los umbrales exactos no son necesarios para la primera implementacion del nuevo sistema.
- Primero se necesita tener tablas/modelo y checklist funcionando.
- Los umbrales se disenaran despues, porque son el medidor real del algoritmo.

Reglas simples para empezar:

- Si hay recaida de alcohol, estado global minimo: crisis o riesgo alto.
- Si hay recaida en abstinencia critica activa, estado global minimo: riesgo alto.
- Si hay no cuidado personal por mas de 3 dias, estado global minimo: riesgo.
- Si uso de celular futuro supera umbral extremo, estado global minimo: riesgo.
- Si hay varios dias seguidos durmiendo tarde, sube alerta de sueno.
- Si no hay acciones protectoras y ya es tarde, estado: bajo movimiento.
- Si hay acciones protectoras en varias dimensiones, estado: estable.
- Si se abrio modo riesgo, registrar evento y subir alerta temporalmente.

Importante:

- Estas reglas deben ser ajustables con el tiempo.
- La app debe evitar lenguaje clinico o diagnostico.
- La app no debe decir "estas mal"; debe traducir a accion concreta.

## Motor de recomendaciones

La app debe recomendar desde cuidado, no desde control.

Una "recomendacion valida por dimension" significa que la app no debe sugerir cualquier cosa al azar. Debe sugerir una accion que tenga sentido para la dimension que esta baja.

Cuando detecta bajo movimiento:

- accion minima de cuerpo;
- agua;
- ducha;
- cepillarse;
- 5 minutos de orden;
- caminar;
- meditar 1 minuto;
- abrir checklist.

Cuando detecta riesgo:

- abrir modo riesgo;
- alejar detonante;
- salir del cuarto;
- registrar impulso;
- usar protocolo de 20 minutos.

Cuando detecta abandono de cuidado personal:

- una accion basica, no diez.
- lenguaje: "Volvamos al cuerpo."

Cuando detecta exceso de proyecto sin base:

- recordar que el futuro no se construye sacrificando cuerpo, mente ni sobriedad.

Ejemplos por dimension:

- Sobriedad / abstinencias: abrir modo riesgo, alejar detonante, registrar impulso, ganar 20 minutos.
- Cuidado personal: banarse, cepillarse, cambiarse, comer algo simple, volver al cuerpo.
- Cuerpo / movimiento: caminar 10 minutos, estirar, gimnasio si hay energia, salir del cuarto.
- Interior / mente: meditar 1 minuto, escribir una linea honesta, respirar, no decidir de madrugada.
- Proyecto / identidad: avance minimo de 10 minutos, cerrar una tarea pequena, no sacrificar cuerpo por proyecto.
- Sueno / ritmo: apagar celular, preparar cama, registrar hora, bajar estimulacion.
- Uso del celular, futuro: dejar celular lejos de la cama, bloquear detonante, hacer pausa fisica.

## Politica de comunicacion

La comunicacion es parte del dominio.

La app debe hablar como:

- un adulto funcional;
- un apoyo emocional;
- una voz compasiva;
- una estructura que no humilla.

Debe evitar:

- "fallaste";
- "mal";
- "eres irresponsable";
- "deberias";
- tono policial;
- tono de coach barato;
- tono clinico.

Debe preferir:

- "La base esta baja."
- "Volvamos al cuerpo."
- "Una accion minima ahora."
- "No necesitas tocar fondo."
- "Esto es una senal, no una condena."
- "Hoy toca estructura, no castigo."

### Protocolo de comunicacion para recaidas

Pendiente de definir antes de escribir textos finales en la app.

Principio:

- Comunicar una recaida sin verguenza.
- Nombrar el hecho sin moralizar.
- Llevar al usuario a una accion de recuperacion.
- Evitar lenguaje de castigo.

Ejemplo de direccion:

```text
Esto ya paso. Ahora el objetivo es no empeorar.
Agua, comida, ducha, dormir, pedir ayuda si hace falta.
Manana revisamos el patron sin insultarte.
```

## Arquitectura recomendada

Usar clean architecture ligera.

No hacer ceremonia innecesaria, pero separar:

```text
domain/
  modelos puros
  reglas de estado
  motor de recomendaciones
  politicas de comunicacion

data/
  Room
  DAOs
  repositorios concretos

app/
  ViewModels
  Compose UI
```

Regla:

- El dashboard no debe calcular reglas directamente.
- Compose solo presenta.
- Room solo guarda hechos.
- El dominio interpreta hechos y produce senales, estado y recomendaciones.

## Entidades de dominio candidatas

- AbstinenceTrack
- AbstinenceLog
- SelfCareTask
- PracticeActivity
- WeeklyGoal
- ActivityLog
- DailyLog
- RiskEvent
- ExternalSignal
- DashboardDimension
- MentalStateAssessment
- Recommendation
- CommunicationMessage

## Hechos vs inferencias

La base de datos guarda hechos:

- hice 20 minutos;
- marque dia sobrio;
- no me bane;
- dormi a las 3am;
- use celular 12h;
- abri modo riesgo.

El dominio calcula inferencias:

- cuidado personal en alerta;
- cuerpo en pausa;
- riesgo alto;
- recomendacion: ducha + agua + salir del cuarto.

Esta separacion es clave para escalar.

## Patrones historicos

La deteccion de patrones es una feature futura.

Idea:

- La app podria aprender que ciertas secuencias aumentan riesgo.
- Ejemplo: varios dias sin cuidado personal + dormir tarde + mucho celular preceden alcohol.
- Esos patrones deben guardarse y mostrarse como observaciones, no como diagnosticos.

No entra en la primera implementacion del nuevo sistema.

Antes de eso se necesita:

- tablas estables;
- logs diarios confiables;
- suficientes datos historicos.

## Preguntas abiertas

- Cuales son los umbrales exactos para cada alerta.
- Como se pondera cada dimension.
- Como se comunica una recaida sin verguenza, mediante protocolo de tono.
- Si el dashboard debe mostrar historico de "patrones detectados" desde el inicio o mas adelante.
- Que recomendaciones son validas para cada dimension.

## Decisiones recientes

- Conducta sexual se considera abstinencia moderada por defecto. En documentacion interna puede referirse a pornografia/masturbacion si el usuario activa esa racha, pero la UI debe evitar etiquetas crudas.
- Umbrales exactos se definen despues.
- Patrones historicos y aprendizaje inteligente se dejan para feature futura.
- La siguiente implementacion debe priorizar schema/tablas y checklist base funcionando.
