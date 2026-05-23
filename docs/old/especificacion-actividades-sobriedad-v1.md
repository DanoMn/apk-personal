# Especificacion v1 - actividades, checklist y sobriedad

Este documento baja a tierra el nucleo funcional de actividades, checklist y abstinencias. Parte del MVP ya existe en Room/Compose, pero estas reglas siguen siendo la referencia para ajustar el modelo y la UI:

- sistema de actividades medibles;
- checklist diaria;
- marca diaria de sobriedad/abstinencias;
- tipos de actividad;
- reglas basicas de registro.

## Decision principal

La sobriedad/abstinencia no debe ser solo un item mas dentro del checklist diario.

Debe ser una feature propia porque:

- es una senal critica de salud mental;
- necesita racha, historial y lectura propia;
- debe aparecer en dashboard;
- puede alimentar el estado calculado del dia;
- tiene mas peso que una actividad comun.

Pero tambien debe poder registrarse de forma rapida desde el dashboard:

```text
Hoy: dia sobrio
[MARCAR DIA SOBRIO]
Racha: 12 dias
Ultima recaida: ...
```

## Tono emocional de esta feature

La app no debe sentirse como policia, juez o padre autoritario.

Debe sentirse como un adulto funcional y compasivo que ayuda a volver a lo basico sin humillar.

La intencion es:

- ensenar autocuidado;
- recordar lo basico cuando nadie lo enseno bien;
- sostener estructura sin castigar;
- mirar con comprension profunda y compasion;
- ayudar a construir dignidad diaria.

Lenguaje guia:

- "Volver al cuerpo."
- "Hoy toca cuidarte sin insultarte."
- "Lo basico tambien es amor propio."
- "No estas roto; estas aprendiendo estructura."

Evitar:

- tono moralista;
- verguenza;
- castigo;
- lenguaje de fracaso;
- frases tipo "fallaste".

## Separacion conceptual

### Sobriedad / abstinencias configurables

Tipo: constancia / abstinencia.

Registra:

- fecha;
- tipo de racha: alcohol, marihuana, conducta sexual u otra;
- estado: no consumi / consumi / desconocido;
- hubo impulso: si/no;
- intensidad del impulso, opcional;
- nota corta, opcional;
- si se abrio modo riesgo, opcional.

No es igual a:

- "hice una actividad";
- "complete un habito";
- "sume minutos".

Es una bandera de estabilidad y autocuidado.

### Rachas configurables

Desde configuracion se debe poder activar o desactivar que rachas se trackean.

Rachas base:

- Alcohol.
- Marihuana.
- Conducta sexual.

Nota interna:

- `Conducta sexual` es la etiqueta visible preferida para la UI.
- Puede referirse a pornografia/masturbacion cuando el usuario activa esa abstinencia.
- Evitar mostrar etiquetas crudas si generan verguenza o ruido innecesario.

Necesidad actual:

- Alcohol: activo.
- Conducta sexual: activo.
- Marihuana: disponible, pero no necesariamente activa porque ahora no es el problema principal.

Mas adelante:

- Permitir crear una racha personalizada.
- Permitir renombrar la racha para que el lenguaje no sea incomodo.
- Permitir ocultar rachas que no aplican.

### Checklist diaria

Registra actividades concretas del dia.

Ejemplos:

- meditar;
- caminar;
- gimnasio;
- leer;
- banarse;
- cepillarse dientes;
- cocinar;
- ordenar;
- avanzar musica;
- avanzar Digitaliza.

La checklist diaria representa lo ideal esperable de un dia:

- trabajar proyectos;
- ir al gimnasio o mover el cuerpo;
- meditar;
- leer;
- cuidar cuerpo;
- sostener orden basico.

No todo debe vivir en la misma lista plana.

Separaciones necesarias:

- Checklist diaria de practica/progreso.
- Rachas de sobriedad/abstinencia.
- Objetivos semanales.
- Cuidado personal basico.

### Modo riesgo

No marca directamente el estado del dia.

Registra un evento:

- fecha/hora;
- intensidad;
- detonante;
- accion tomada;
- si se evito actuar o no;
- nota posterior.

## Tipos de actividad v1

Antes de definir actividades predeterminadas exactas, hay que cerrar los tipos.

### 1. Actividad de tiempo

Para actividades donde importa cuanto tiempo se practico.

Ejemplos:

- Meditar: 5 min.
- Ejercicio o caminar: 40 min.
- Leer: 20 min.
- Musica: 20 min.
- Digitaliza: 30 min.

Campos:

- objetivoMinutos;
- minimoValidoMinutos;
- minutosRegistradosHoy;
- frecuencia;
- capa;
- importancia.

Regla de registro:

- Tap simple: marca completado con el tiempo objetivo predeterminado.
- Mantener presionado: permite editar el tiempo hecho hoy.
- El tiempo hecho hoy puede ser menor, igual o mayor al objetivo.
- Si el tiempo real supera el objetivo, cuenta como cumplido y guarda excedente positivo.
- Si el tiempo real es menor pero supera el minimo valido, puede contar como "minimo cumplido" pero no como objetivo completo.

Ejemplo:

```text
Actividad: Meditar
Objetivo: 5 min
Minimo valido: 1 min

Tap: registra 5 min.
Long press: registrar 3, 8, 20 min, etc.
```

### 2. Actividad de constancia / abstinencia

Para cosas importantes que consisten en no hacer algo.

Ejemplos:

- No beber alcohol.
- No fumar marihuana.
- No usar celular antes de dormir.
- No decidir cosas importantes de madrugada.

Campos:

- cumplido: si/no;
- hubo impulso: si/no;
- intensidad opcional;
- nota opcional;
- esCritica: si/no.

Nota:

- No alcohol debe vivir como feature propia de sobriedad.
- Otras abstinencias pueden vivir como actividades conductuales.

### 3. Actividad diaria necesaria / cuidado personal

Para acciones basicas de respeto propio.

Ejemplos:

- Banarse.
- Cepillarse dientes despues de cada comida.
- Cambiarse de ropa.
- Tender cama.
- Comer algo decente.

Campos:

- cumplido;
- cantidad objetivo, opcional;
- cantidad real, opcional;
- nota opcional.

No deben sentirse como castigo. Deben funcionar como "volver al cuerpo".

Nota:

- Estas actividades normalmente no requieren tiempo concreto.
- Su valor esta en la constancia y en recuperar cuidado basico.
- Ejemplo importante: cepillarse los dientes correctamente.
- Deben poder aparecer como "cuidado personal" y no mezclarse con productividad.

### 4. Actividad de frecuencia semanal

Para objetivos que no tienen que ocurrir todos los dias.

Ejemplos:

- Gimnasio 3 veces por semana.
- Lavar ropa 1 vez por semana.
- Limpieza profunda 1 vez por semana.

Campos:

- frecuenciaSemanalObjetivo;
- vecesRegistradasSemana;
- objetivoMinutos opcional;
- minutosRegistrados opcional.

Regla:

- En el dashboard diario puede aparecer como "esta semana: 1/3".
- En el dia solo se registra si hoy se hizo.

Esto cubre objetivos como:

- gimnasio 3 veces por semana;
- caminar 4 veces por semana;
- leer 5 veces por semana;
- cocinar en casa 4 veces por semana.

### 5. Actividad de hora

Para eventos donde importa el horario.

Ejemplos:

- Dormir temprano.
- Despertar.
- Celular fuera de cama.

Campos:

- horaObjetivo;
- horaReal;
- cumplido segun regla;
- nota opcional.

Esta categoria probablemente se conecte luego con sueno.

### 6. Registro libre / nota corta

Para registrar contexto sin convertirlo en checklist rigida.

Ejemplos:

- Que comi.
- Como me senti.
- Que detono un impulso.
- Que hice para cortar el circuito.

Campos:

- texto;
- fecha/hora;
- capa opcional;
- tags opcionales.

## Actividades base propuestas

Estas no son definitivas, pero son el primer set razonable.

### Espiritual / interior

- Meditar antes de dormir: tiempo, objetivo 5 min, minimo 1 min.
- Escribir una linea honesta: check o nota corta.

### Fisico / cuerpo

- Ejercicio/gimnasio/caminar: tiempo, objetivo 40 min.
- Gimnasio semanal: frecuencia, objetivo 3 veces por semana.
- Dormir temprano: hora.
- Banarse: cuidado personal.
- Cepillarse dientes: cuidado personal, frecuencia diaria.

### Conductual / autocontrol

- No celular antes de dormir: abstinencia/hora.
- No decidir desde madrugada: abstinencia.
- Abrir modo riesgo antes de actuar: evento de riesgo, no checklist normal.

### Alimentacion / cuidado domestico

- Una comida hecha en casa: check.
- Cuidar alimentacion prometida: check o nota.
- Orden minimo de casa: tiempo, objetivo 15 min.

### Social / vinculos / persona

- No aislarme destructivamente: check/nota.
- Tener una interaccion limpia: nota corta.

### Proyecto / identidad creativa

- Avance en Digitaliza: tiempo, objetivo 30 min.
- Musica/cuaderno/composicion: tiempo, objetivo 20 min.
- Leer: tiempo, objetivo 20 min.

## Dashboard v1 esperado

El dashboard no debe mostrar toda la checklist.

Debe mostrar:

- frase ancla diaria;
- estado calculado;
- racha sobria/no alcohol;
- rachas activas de abstinencia configuradas;
- boton "marcar dia sobrio";
- progreso del dia por capas;
- cuidado personal basico pendiente;
- objetivo semanal destacado, por ejemplo gym 1/3;
- acceso a checklist;
- acceso a modo riesgo;
- resumen semanal pequeno.

Ejemplo:

```text
[DASHBOARD]
Frase del dia...

Estado calculado: en marcha
Alcohol: dia 12
Conducta sexual: dia 4
[Marcar dia limpio]

Practica de hoy:
Interior  5/5 min
Cuerpo    0/40 min
Proyecto  30/30 min

Cuidado basico:
Banarse pendiente
Dientes pendiente

Semana:
Gym 1/3

[Abrir checklist]
[Modo riesgo]
```

## Estado calculado v1

Por ahora no intentar medir salud mental de forma compleja.

Regla inicial simple:

- Si hubo recaida en una abstinencia critica activa: crisis o alerta alta.
- Si se abrio modo riesgo: riesgo.
- Si no hay ningun registro y ya es tarde: bajo movimiento.
- Si hay registro de actividad critica y abstinencias marcadas: en marcha/estable.
- Si varias capas tienen progreso: estable.

No usar lenguaje de diagnostico.

La app no debe decir:

```text
Tu salud mental esta mal.
```

Debe decir algo mas operativo:

```text
Senal del dia: bajo movimiento.
Primero cuerpo, agua, comida, 5 minutos de orden.
```

## Uso del celular

Es una idea futura valida, pero no entra ahora.

Es tecnicamente posible con permisos de uso del sistema, pero:

- requiere permisos sensibles;
- puede variar segun Android/GrapheneOS;
- puede complicar el MVP;
- debe tratarse como indicador, no como vigilancia punitiva.

Por ahora:

- registrar manualmente "no celular antes de dormir";
- luego evaluar tracking automatico.

## Decision de persistencia

Decision del usuario:

- Migrar directamente a un modelo mas formal.

Implicacion:

- Antes de implementar, definir schema.
- Probable direccion: Room.
- No implementar export/import hasta estabilizar schema.

Entidades minimas para Fase 1:

- Layer.
- Activity.
- ActivityLog.
- SobrietyLog.
- RiskEvent.

Regla arquitectonica:

- La base de datos guarda hechos.
- El dominio calcula inferencias.
- La UI solo presenta el resultado.

Ejemplos de hechos:

- se registraron 5 minutos de meditacion;
- se marco dia sobrio;
- se registro recaida;
- se abrio modo riesgo;
- se cumplio cuidado personal.

Ejemplos de inferencias:

- estado calculado del dia;
- dimension de cuidado personal en alerta;
- recomendacion de volver al cuerpo;
- patron de autosabotaje, futuro.

## Preguntas pendientes antes de cerrar la siguiente iteracion

- Los tipos v1 quedan cerrados o falta algun tipo?
- Para actividades de tiempo, el long press abre un dialog simple o una pantalla de edicion del registro del dia?
- "No celular antes de dormir" vive en conductual, sueno o ambos?
- La marca de sobriedad debe preguntarse al final del dia o estar siempre disponible en dashboard?
- Si un dia no se marca sobriedad, se interpreta como desconocido o como no sobrio? Recomendacion: desconocido.
