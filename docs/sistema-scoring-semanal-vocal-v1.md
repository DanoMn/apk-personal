# Sistema de Scoring Semanal v1 — Vocal / Autonomía sin límites

**Estado:** borrador de especificación conceptual y funcional
**Propósito:** definir el sistema de scoring semanal de Vocal antes de convertirlo en implementación.
**Alcance:** scoring semanal de base, estados, capas, anclas, soportes, tasks, sueño semanal, sobriedad opt-in, superávit, gates y lectura visual.
**Fuera de alcance por ahora:** métrica diaria/readiness de sueño como experiencia separada, algoritmo final de bloqueo nocturno, UI final del dashboard, implementación Kotlin/Room/Compose.

---

## 1. Principio central

Vocal no mide felicidad, salud mental clínica, valor personal ni productividad pura.

Vocal mide una lectura práctica:

```text
Qué tanto el usuario está sosteniendo la base personal que él mismo configuró.
```

El scoring semanal no debe responder:

```text
¿Qué tan buena persona fue esta semana?
```

Debe responder:

```text
¿La base configurada se sostuvo, empezó a ceder o se desarmó?
```

La app debe mantener una lectura honesta sin castigar ni humillar. Un score bajo no significa fracaso moral. Significa que hay una señal y toca volver a la base.

---

## 2. Separación obligatoria: métrica diaria vs scoring semanal

Durante la conversación se detectó un problema conceptual importante: no conviene meter en el mismo cálculo, sin separación interna, dos fenómenos distintos.

### 2.1 Métrica diaria

La métrica diaria queda separada del scoring semanal.

Su rol conceptual es responder:

```text
¿Con qué piso físico/energético amaneció el usuario hoy?
```

Nace principalmente del sueño del día:

- bloqueo nocturno;
- desbloqueos;
- duración protegida;
- alineación con ventana objetivo;
- continuidad del descanso;
- higiene digital antes de dormir.

La métrica diaria puede mover la experiencia del día y puede servir como readiness o lectura física inmediata, pero no es el núcleo de este documento.

### 2.2 Scoring semanal de base

El scoring semanal responde:

```text
¿Qué tanto sostuvo el usuario su base configurada durante la semana?
```

Nace de:

- anclas;
- soportes;
- tasks relevantes;
- sueño semanal dentro de Cuerpo;
- sobriedad opt-in dentro de Conducta;
- equilibrio entre capas;
- superávit;
- memoria temporal;
- gates.

Este documento define el scoring semanal.

---

## 3. Base construida, sin datos y score provisional

### 3.1 Sin datos

`Sin datos` no es un estado bajo.

Debe usarse cuando todavía no existe información suficiente para calcular una lectura.

Regla:

```text
Sin datos ≠ Restauración
Sin datos ≠ Atención
```

La app no debe mostrar al usuario nuevo que está en Restauración o Atención solo porque aún no ha registrado suficiente.

### 3.2 Base en construcción

La base se considera en construcción durante la primera semana desde que el usuario estableció sus metas y objetivos.

Durante esta etapa puede existir una lectura provisional, pero no debe comunicarse como estabilidad real acumulada.

Regla:

```text
Antes de cerrar la primera semana, los estados bajos no deben aparecer por falta de historial.
```

### 3.3 Base construida

La base se considera construida cuando termina la primera semana desde que el usuario configuró sus metas.

Desde ese momento, si el usuario definió una base y no la cumplió, ya es válido mostrar estados bajos como Atención o Restauración.

Esto no debe comunicarse como culpa. Debe comunicarse como señal.

---

## 4. Estados de base: definición conceptual

Los estados no deben definirse primero por rangos numéricos. Primero deben definirse por hechos reales.

La escala visual puede usar rangos, pero el dominio debe entender qué significa cada estado.

---

### 4.1 Sin datos

**Nombre visible:** Sin datos
**Número visible:** `--`
**Lectura:** aún no hay registros suficientes para inferir base.

Representa:

```text
La app todavía no tiene suficiente información para leer la base del usuario.
```

No representa:

- abandono;
- recaída;
- falta de disciplina;
- riesgo;
- deterioro.

Mensaje posible:

```text
Aún estamos construyendo tu primera lectura. Registra esta semana para empezar a ver tu base.
```

---

### 4.2 Restauración

**Nombre visible:** Restauración
**Alias conceptual posible:** En riesgo, base en reconstrucción
**Lectura:** base baja, reconstrucción mínima.

Representa:

```text
La base configurada está muy desarmada o casi abandonada.
```

Hechos que pueden llevar a Restauración:

- abandono fuerte de anclas semanales;
- varias capas activas en déficit severo;
- peor capa extremadamente baja;
- sueño semanal crítico dentro de Cuerpo;
- omisiones graves en soportes esenciales;
- sobriedad activa con recaída crítica;
- ausencia de estructura mínima después de haber definido una base.

No significa:

```text
El usuario falló como persona.
```

Significa:

```text
La base se desarmó y toca reconstruir desde lo mínimo.
```

Tono recomendado:

```text
La base necesita cuidado mínimo. Volvamos a lo indispensable.
```

---

### 4.3 Atención

**Nombre visible:** Atención
**Alias conceptual posible:** Cuidado, margen reducido
**Lectura:** la base empezó a ceder.

Representa:

```text
El usuario todavía sostiene parte de su base, pero ya hay facetas que están bajando.
```

Hechos que pueden llevar a Atención:

- algunas capas activas están incumplidas;
- la semana va atrasada respecto a los objetivos;
- una capa importante está en déficit;
- sueño semanal irregular o bajo;
- soportes omitidos con frecuencia;
- tasks relevantes no resueltas;
- sobriedad activa con señales de riesgo, si aplica;
- tendencia descendente frente a semanas anteriores.

No representa caída total. Representa advertencia temprana.

Tono recomendado:

```text
Hay margen, pero la base está cediendo.
```

---

### 4.4 En marcha

**Nombre visible:** En marcha
**Lectura:** base activa y suficientemente sostenida.

Representa:

```text
El usuario está cumpliendo de forma suficiente la base que se propuso.
```

Hechos que pueden llevar a En marcha:

- cumplimiento razonable de anclas semanales;
- capas activas sin abandono fuerte;
- peor capa por encima de umbral mínimo;
- sueño semanal aceptable;
- soportes suficientemente cuidados;
- ausencia de gates críticos;
- sobriedad sin recaída, si está activa.

`En marcha` es el hogar operativo de Vocal. No debe sentirse mediocre. Debe sentirse como éxito real.

Tono recomendado:

```text
La base está activa y sosteniéndose.
```

---

### 4.5 Plenitud

**Nombre visible:** Plenitud
**Lectura:** base sostenida, equilibrada y con margen positivo.

Representa:

```text
El usuario no solo cumplió su base, sino que la sostuvo con equilibrio y buena calidad.
```

Diferencia con En marcha:

```text
En marcha = cumple la base.
Plenitud = cumple la base + la sostiene con equilibrio + empieza a tener margen positivo.
```

Hechos que pueden llevar a Plenitud:

- WeeklyBaseScore alto;
- peor capa suficientemente alta;
- sueño semanal no crítico;
- capas activas equilibradas;
- superávit saludable;
- ninguna capa abandonada;
- sin recaída en sobriedad activa;
- al menos cierta consistencia temporal.

Plenitud no debe significar perfección ni euforia. Significa base amplia y bien sostenida.

Tono recomendado:

```text
Las bases están activas formando unidad.
```

---

### 4.6 Inquebrantable

**Nombre visible:** Inquebrantable
**Lectura:** núcleo sólido sostenido en el tiempo.

Representa:

```text
La base no depende de una semana buena; ya muestra estructura sostenida.
```

Diferencia con Plenitud:

```text
Plenitud = base alta y equilibrada.
Inquebrantable = base alta, equilibrada y sostenida durante más tiempo.
```

Hechos que pueden llevar a Inquebrantable:

- stabilityScore muy alto;
- varias semanas de base alta;
- peor capa sostenida;
- superávit positivo sostenido;
- sueño semanal estable;
- sin gates críticos;
- si sobriedad está activa, racha/protección consistente;
- ausencia de déficits severos o abandono de capas.

Inquebrantable no debe aparecer por una sola semana excelente.

Tono recomendado:

```text
Núcleo protegido por consistencia.
```

---

## 5. Qué muestra cada capa

Cada capa debe mostrar su propio score o lectura parcial. El score global no debe ocultar de dónde viene la lectura.

Cada capa debería poder mostrar:

```text
- score de capa;
- estado breve de capa;
- anclas cumplidas;
- soportes/omisiones;
- tasks relevantes;
- déficit;
- superávit;
- razón principal de la lectura.
```

---

### 5.1 Interior

Representa:

```text
Claridad interna, reflexión, silencio, aprendizaje, conciencia y prácticas que sostienen el mundo interior.
```

Inputs posibles:

- anclas: meditar, leer, escribir, aprender, estudiar;
- soportes interiores;
- tasks relevantes de interior;
- registros de práctica.

Muestra recomendada:

```text
Interior: 0.86 — sostenido
Motivo: meditación cumplida, lectura parcial, sin déficit crítico.
```

---

### 5.2 Cuerpo

Representa:

```text
Base física: movimiento, cuerpo, descanso, alimentación, higiene, energía y recuperación.
```

Inputs posibles:

- anclas físicas: ejercicio, caminar, correr, gimnasio, yoga, deporte;
- soportes físicos: agua, comida, higiene, ropa limpia, etc.;
- tasks relevantes de cuerpo;
- sueño semanal.

Cuerpo es especial porque integra sueño.

Muestra recomendada:

```text
Cuerpo: 0.72 — activo con margen bajo
Motivo: gimnasio incompleto, sueño semanal aceptable, soportes parciales.
```

---

### 5.3 Conducta

Representa:

```text
Autogobierno conductual, límites, prevención de autosabotaje, orden y sobriedad si aplica.
```

Inputs posibles:

- anclas conductuales;
- soportes conductuales;
- tasks relevantes;
- sobriedad opt-in;
- recaídas o señales de protección.

Conducta es especial porque puede integrar sobriedad activa.

Muestra recomendada:

```text
Conducta: 0.91 — protegida
Motivo: gestión financiera cumplida, sobriedad limpia, sin gates.
```

---

### 5.4 Vínculos

Representa:

```text
Prácticas sostenidas de relación, conexión, pertenencia, reparación y presencia.
```

Inputs posibles:

- cultivar vínculo;
- grupo de estudio;
- entrenamiento grupal;
- voluntariado recurrente;
- proyecto compartido;
- mentoría;
- crianza presente;
- tasks relacionales relevantes.

Muestra recomendada:

```text
Vínculos: 0.64 — cediendo
Motivo: vínculo principal sin registro suficiente esta semana.
```

---

### 5.5 Proyecto

Representa:

```text
Construcción concreta de futuro, identidad, creación, trabajo, estudio, producto o dirección personal.
```

Inputs posibles:

- trabajar en proyecto;
- crear;
- practicar habilidad;
- construir negocio;
- desarrollar producto;
- crear contenido;
- tasks de proyecto;
- superávit de avance.

Muestra recomendada:

```text
Proyecto: 1.12 — superávit
Motivo: objetivo cumplido y avance extra sostenido.
```

---

## 6. Scoring atómico de anclas

Las anclas son el núcleo del scoring semanal.

Cada ancla tiene:

```text
weeklyFrequencyTarget
sessionTargetMinutes
```

Ejemplo:

```text
Meditar
3 veces por semana
20 minutos por sesión
```

Entonces:

```text
targetSessions = 3
targetMinutes = 3 * 20 = 60
```

---

### 6.1 Fórmula de cumplimiento base

```text
frequencyScore = sesionesRealizadas / sesionesObjetivo
minuteScore = minutosRealizados / minutosObjetivoSemana
```

Para cumplimiento base, se cortan ambos a 1.00:

```text
frequencyBase = min(frequencyScore, 1.00)
minuteBase = min(minuteScore, 1.00)
```

Fórmula:

```text
anchorBaseScore =
0.70 * frequencyBase
+ 0.30 * minuteBase
```

Decisión:

```text
Frecuencia pesa 70%.
Minutos pesan 30%.
```

Razón:

```text
Vocal premia consistencia antes que acumulación de horas.
```

Una sesión enorme no debe reemplazar el ritmo semanal.

---

## 7. Superávit

El superávit nunca resta.

Representa:

```text
Excedente real sobre la base configurada.
```

No es malo por sí mismo. El problema no es hacer más. El problema aparece si, al hacer más en una capa, caen sueño u otras capas.

Regla central:

```text
El superávit es positivo si la base general se mantiene.
```

---

### 7.1 Cálculo de superávit por ancla

```text
frequencySurplus =
max(0, sesionesRealizadas - sesionesObjetivo) / sesionesObjetivo

minuteSurplus =
max(0, minutosRealizados - minutosObjetivoSemana) / minutosObjetivoSemana
```

Luego:

```text
anchorSurplusRaw =
0.70 * frequencySurplus
+ 0.30 * minuteSurplus
```

Límite propuesto:

```text
anchorSurplusBonus = min(anchorSurplusRaw, 0.20)
```

Decisión propuesta v1:

```text
Superávit máximo por ancla: +20%.
```

---

### 7.2 Regla de uso del superávit

El superávit se calcula separado de la base.

```text
Base cumplida = hasta 1.00
Superávit = 1.01 a 1.20
```

Reglas:

```text
- Nunca resta.
- No compensa una capa abandonada.
- No compensa sueño crítico.
- No compensa recaída activa.
- Ayuda a Plenitud e Inquebrantable si hay equilibrio.
```

---

## 8. Soportes

Los soportes son mantenimiento de base.

No son anclas. No son tasks. No son productividad.

Representan:

```text
Acciones de mantenimiento que conservan dignidad, cuerpo, orden mínimo y estructura.
```

Ejemplos:

- tomar agua;
- comer algo decente;
- bañarse;
- cepillarse los dientes;
- usar ropa limpia;
- orden mínimo;
- interacción limpia;
- escribir diario personal si se define como soporte.

Decisión:

```text
Soportes pesan 20% dentro de cada capa.
```

Razón:

```text
Soportes son más importantes que tasks porque sostienen la base cotidiana.
```

---

### 8.1 Cálculo de soportes

Si la UX es inversa:

```text
SupportScore = 1.00 - tasaDeOmisiones
```

Ejemplo:

```text
5 soportes esperados
1 omitido
SupportScore = 0.80
```

Regla:

```text
Soportes ayudan a sostener la capa.
No reemplazan anclas.
No desbloquean solos estados altos.
```

---

## 9. Tasks

Las tasks son puntuales.

Representan:

```text
Acciones concretas que pueden aportar a una capa, pero no construyen base por sí solas.
```

Decision actualizada:

```text
Tasks no pesan como denominador fijo dentro de la capa.
Tasks con capa completadas aportan TaskMomentum como superhabit diario.
Tasks pendientes no penalizan.
```

Una task solo cuenta si:

```text
layerId != null
contributionRole != Neutral
```

Tasks neutrales no suman.

Ejemplos de tasks neutrales:

```text
Comprar cuerdas.
Buscar una referencia.
Ordenar un archivo sin impacto real.
```

Ejemplos de tasks que sí pueden aportar:

```text
Pedir cita médica.
Resolver un trámite urgente.
Pagar una deuda crítica.
Llamar a alguien para reparar una conversación.
```

Fórmula simple:

```text
TaskScore =
tasksRelevantesCompletadas / tasksRelevantesPlanificadas
```

Si no hay tasks relevantes:

```text
TaskScore = 0
```

Regla:

```text
Tasks ayudan poco.
No sustituyen prácticas.
No deben mover de forma fuerte el score semanal.
```

---

## 10. Fórmula normal de capa

Para capas sin módulos especiales:

```text
LayerNormalScore =
0.75 * AnchorScore
+ 0.20 * SupportScore
+ 0.05 * TaskScore
```

Lectura conceptual:

```text
Anclas construyen base.
Soportes conservan base.
Tasks ayudan puntualmente.
```

---

## 11. Sueño semanal dentro de Cuerpo

Aunque la métrica diaria queda separada, el sueño también debe aportar semanalmente a Cuerpo.

El sueño no es una ancla común. Es una señal fisiológica especial.

### 11.1 SleepWeeklyScore

Propuesta v1:

```text
SleepWeeklyScore =
0.50 * durationScore
+ 0.25 * regularityScore
+ 0.15 * continuityScore
+ 0.10 * digitalHygieneScore
```

Donde:

```text
durationScore:
qué tanto cumplió su duración objetivo semanal.

regularityScore:
qué tan cerca estuvo de su ventana objetivo.

continuityScore:
qué tan continuo y poco fragmentado fue el sueño.

digitalHygieneScore:
si protegió el tiempo previo sin celular.
```

### 11.2 Cuerpo normal

```text
CuerpoNormalScore =
0.75 * AnchorScore
+ 0.20 * SupportScore
+ 0.05 * TaskScore
```

### 11.3 Cuerpo con sueño

```text
CuerpoScore =
0.70 * CuerpoNormalScore
+ 0.30 * SleepWeeklyScore
```

Decisión:

```text
Sueño pesa 30% de Cuerpo.
```

Razón:

```text
El sueño impacta fuertemente en el cuerpo, pero no debe borrar movimiento, comida, higiene, soporte físico ni anclas corporales.
```

---

## 12. Sobriedad dentro de Conducta

La sobriedad no es task, soporte ni ancla común.

Es un módulo protector opt-in.

Si no está activa:

```text
No aparece.
No pesa.
No limita.
```

Si está activa, entra en Conducta.

---

### 12.1 Conducta normal

```text
ConductaNormalScore =
0.75 * AnchorScore
+ 0.20 * SupportScore
+ 0.05 * TaskScore
```

### 12.2 Conducta con sobriedad activa

```text
ConductaScore =
0.70 * ConductaNormalScore
+ 0.30 * SobrietyWeeklyScore
```

Decisión:

```text
Sobriedad activa pesa 30% de Conducta.
```

---

### 12.3 SobrietyWeeklyScore

Propuesta conceptual:

```text
SobrietyWeeklyScore =
díasLimpios
+ impulsosResistidos
- recaídas
```

Pero una recaída no debe tratarse como task fallida.

Debe tratarse como señal crítica de recuperación/protección.

Actualizacion:

```text
Si hay recaída en sobriedad activa:
no hay gate duro. La recaida penaliza fuerte mediante SobrietyWeeklyScore,
Conducta, peor capa, razones y estabilidad temporal.
```

---

## 13. Agregación de capas

Solo cuentan las capas activas del usuario.

Mínimo:

```text
3 capas activas.
```

Máximo conceptual:

```text
5 capas activas.
```

Todas las capas activas pesan igual.

Más capas no significa más techo de score. Significa una base personal más amplia y más compleja de sostener.

---

### 13.1 Fórmula de base semanal

```text
averageLayers = promedio de capas activas
worstLayer = capa activa más baja
```

Fórmula:

```text
WeeklyBaseScore =
0.75 * averageLayers
+ 0.25 * worstLayer
```

Razón:

```text
La base funciona como una mesa.
Una pata baja debe arrastrar la lectura.
```

---

## 14. WeeklySurplusBonus

El superávit global se aplica después de calcular la base.

Regla propuesta:

```text
if worstLayer < 0.70:
    WeeklySurplusBonus = 0
else:
    WeeklySurplusBonus = min(promedioSurplus, 0.20)
```

Interpretación:

```text
Si una capa está baja, el sistema no castiga el superávit.
Simplemente no lo usa para empujar estados altos.
```

El superávit existe y puede mostrarse como mérito local, pero no debe tapar una base desequilibrada.

---

## 15. WeeklyScore final

```text
WeeklyScore =
WeeklyBaseScore
+ WeeklySurplusBonus
```

Límite:

```text
WeeklyScore máximo = 1.20
```

Interpretación:

```text
1.00 = base cumplida.
1.01 - 1.20 = base cumplida con superávit.
```

---

## 16. Memoria temporal

Plenitud e Inquebrantable no deben depender solo de una semana.

Para eso se usa un `stabilityScore`.

Propuesta:

```text
stabilityScore =
0.65 * previousStabilityScore
+ 0.35 * WeeklyScoreActual
```

Esto mantiene memoria sin ignorar la semana actual.

---

### 16.1 Uso conceptual

```text
Primera semana:
base construida inicial.

Segunda semana:
ya puede existir consistencia corta.

Cuarta semana:
ya puede hablarse de solidez mayor.

Octava semana o más:
puede tener sentido un Inquebrantable más fuerte.
```

Estos plazos son propuestas v1, no canon final.

---

## 17. Gates

Los gates protegen el significado de los estados.

```text
Gate = regla que limita estados altos aunque el número salga alto.
```

Gates sugeridos:

```text
Sueño semanal crítico:
bloquea Plenitud/Inquebrantable.

Capa activa abandonada:
bloquea Plenitud/Inquebrantable.

Sobriedad activa con recaída:
bloquea Plenitud/Inquebrantable.

Base recién creada:
modo provisional.

Sin datos:
no mostrar score.
```

---

## 18. Estados según score semanal y memoria

### 18.1 Restauración

Criterio base:

```text
WeeklyBaseScore < 0.40
```

O señales equivalentes:

- varias capas muy bajas;
- peor capa casi abandonada;
- sueño/cuerpo crítico;
- soportes esenciales omitidos;
- recaída activa crítica si aplica.

Lectura:

```text
Base baja. Reconstrucción mínima.
```

---

### 18.2 Atención

Criterio base:

```text
0.40 <= WeeklyBaseScore < 0.70
```

Lectura:

```text
Hay margen, pero la base está cediendo.
```

---

### 18.3 En marcha

Criterio base:

```text
0.70 <= WeeklyBaseScore < 0.90
```

Lectura:

```text
La base está activa y sosteniéndose.
```

---

### 18.4 Plenitud

Criterio sugerido:

```text
WeeklyBaseScore >= 0.90
worstLayer >= 0.80
sin gates críticos
mínimo consistencia corta o stabilityScore alto
```

Lectura:

```text
Base sostenida con equilibrio y margen positivo.
```

---

### 18.5 Inquebrantable

Criterio sugerido:

```text
stabilityScore >= 0.95
worstLayer >= 0.85
superávit positivo sostenido
sin gates críticos
varias semanas de historial
```

Lectura:

```text
Núcleo sólido sostenido en el tiempo.
```

---

## 19. Escala visible

El mockup actual usa:

```text
Sin datos: --
Restauración: 700-749
Atención: 750-799
En marcha: 800-899
Plenitud: 900-949
Inquebrantable: 950-1000
```

Regla visual actual:

```text
Si el score interno cae debajo de 700, el visible se fija en 700.
```

Esto evita mostrar números humillantes.

### 19.1 Mapeo sugerido

```text
visibleScore = 700 + normalizedFinalScore * 300
```

Donde:

```text
normalizedFinalScore = min(WeeklyScore, 1.00)
```

El superávit no necesita subir el score por encima de 1000. Sirve principalmente para:

```text
- reforzar Plenitud;
- desbloquear Inquebrantable;
- mostrar margen positivo;
- diferenciar cumplimiento exacto de cumplimiento con excedente.
```

---

## 20. Ejemplo completo

Usuario con 3 capas activas:

```text
Interior
Cuerpo
Proyecto
```

Configuración:

```text
Interior:
Meditar 3 veces x 20 min = 60 min

Cuerpo:
Gimnasio 3 veces x 60 min = 180 min

Proyecto:
Trabajar proyecto 4 veces x 90 min = 360 min
```

Resultado semanal:

```text
Meditar:
3 sesiones / 60 min

Gimnasio:
2 sesiones / 120 min

Proyecto:
5 sesiones / 500 min
```

Soportes:

```text
Interior SupportScore = 1.00
Cuerpo SupportScore = 0.80
Proyecto SupportScore = 0.00
```

Tasks:

```text
Interior TaskScore = 0.00
Cuerpo TaskScore = 0.00
Proyecto TaskScore = 1.00
```

Sueño semanal:

```text
SleepWeeklyScore = 0.75
```

---

### 20.1 Interior

```text
AnchorScore = 1.00
SupportScore = 1.00
TaskScore = 0.00
```

```text
InteriorScore =
0.75*1.00
+ 0.20*1.00
+ 0.05*0.00

InteriorScore = 0.95
```

Lectura:

```text
Interior está sostenido.
```

---

### 20.2 Cuerpo

Ancla:

```text
frequencyScore = 2/3 = 0.67
minuteScore = 120/180 = 0.67
AnchorScore = 0.67
```

Cuerpo normal:

```text
CuerpoNormal =
0.75*0.67
+ 0.20*0.80
+ 0.05*0.00

CuerpoNormal =
0.5025 + 0.16
= 0.6625
```

Cuerpo con sueño:

```text
CuerpoScore =
0.70*0.6625
+ 0.30*0.75

CuerpoScore =
0.46375 + 0.225
= 0.68875
```

Lectura:

```text
Cuerpo está bajo. No está abandonado, pero arrastra la semana.
```

---

### 20.3 Proyecto

Proyecto tuvo superávit.

```text
frequencyScore = 5/4 = 1.25
minuteScore = 500/360 = 1.39
AnchorBaseScore = 1.00
SurplusBonus existe aparte
```

```text
ProyectoScore =
0.75*1.00
+ 0.20*0.00
+ 0.05*1.00

ProyectoScore = 0.80
```

Lectura:

```text
Proyecto está cumplido y con superávit, pero su score de capa base no se infla infinitamente.
```

---

### 20.4 Base semanal

```text
averageLayers =
(0.95 + 0.68875 + 0.80) / 3
= 0.8129
```

```text
worstLayer = 0.68875
```

```text
WeeklyBaseScore =
0.75*0.8129
+ 0.25*0.68875

WeeklyBaseScore =
0.6097 + 0.1722
= 0.7819
```

---

### 20.5 Superávit

Proyecto tuvo superávit, pero:

```text
worstLayer = 0.68875
```

Como está por debajo de `0.70`, el superávit no se usa para empujar Plenitud.

```text
WeeklySurplusBonus = 0
```

No se castiga el superávit. Simplemente no compensa el déficit de Cuerpo.

---

### 20.6 Resultado

```text
WeeklyScore = 0.7819
```

Estado:

```text
En marcha baja / Atención alta según gates y memoria temporal.
```

Lectura de producto:

```text
La base está activa, pero Cuerpo está arrastrando la semana. Proyecto tuvo impulso, pero no compensa el déficit físico.
```

---

## 21. Decisiones cerradas v1

```text
1. Scoring semanal separado de métrica diaria.
2. Sin datos no equivale a riesgo.
3. Base construida después de la primera semana.
4. Si no hay soportes configurados, anclas pesan 100% dentro de capa.
5. Si hay soportes configurados, anclas pesan 80% y soportes 20% dentro de capa.
6. Tasks no entran al denominador; aportan TaskMomentum si tienen capa.
7. Frecuencia pesa 70% dentro del ancla.
8. Minutos pesan 30% dentro del ancla.
9. Sueño semanal pesa 30% dentro de Cuerpo.
10. Sobriedad activa pesa 30% dentro de Conducta.
11. Todas las capas activas pesan igual.
12. Peor capa arrastra 25% del score semanal.
13. Superávit nunca resta.
14. Superávit no compensa capas caídas.
15. Plenitud requiere equilibrio.
16. Inquebrantable requiere memoria temporal.
17. Gates protegen estados altos.
18. La escala visible puede seguir 700-1000 como UI.
```

---

## 22. Pendientes de definición fina

```text
1. Umbrales exactos de estados sin gates duros.
2. Calibracion fina de SobrietyWeeklyScore con tests; formula v0 aprobada en
   seccion 24.7.
3. Calibracion de SupportScore por UX inversa; soporte opt-in aprobado en
   seccion 24.12.1.
4. Capas sin soportes configurados quedan resueltas: anclas pesan 100%.
5. Cómo mostrar score de capa en UI.
6. Duración mínima para Inquebrantable: 4, 8 o más semanas.
7. Relación final entre métrica diaria y scoring semanal.
8. Si Plenitud requiere 2 semanas o puede aparecer en semana 1 real.
9. Si superávit distribuido pesa más que superávit concentrado.
10. Cómo documentar recuperación posterior a recaída.
```

---

## 23. Frase guía del sistema

```text
Vocal no premia hacer más por hacerlo.
Vocal lee si la base configurada se sostuvo, si empezó a ceder o si se volvió sólida con el tiempo.
```

---

## 24. Actualizacion conceptual: nucleo de features que afectan al scoring

Esta seccion actualiza y expande el documento con decisiones de diseno cerradas
durante la revision tecnica. Si alguna seccion anterior habla de `gates`, tasks
como deuda, o ausencia de registro general, esta actualizacion prevalece para
la implementacion nueva.

### 24.1 Principio de registro diario

Para anclas, soportes y tasks no existe el estado de negocio "no registrado"
despues del cierre del dia.

Regla:

```text
Durante el dia:
el usuario puede modificar estados.

Al cierre local:
todo lo configurado queda consolidado como hecho historico.
```

Consecuencias:

```text
Ancla no marcada -> NotDone.
Soporte no omitido -> Done.
Soporte omitido -> Omitted.
Task pendiente -> no penaliza.
Task completada con capa -> aporta momentum.
```

La unica excepcion es sobriedad, porque es una feature sensible. Solo sobriedad
permite ausencia/pending moldeable temporalmente.

### 24.2 Configuracion inicial y base declarada

La configuracion inicial es obligatoria para activar el scoring.

Minimos:

```text
3 capas activas.
1 ancla minima por cada capa activa inicial.
```

El usuario declara:

```text
1. Que venia haciendo.
2. Que quiere lograr ahora.
```

La base declarada inicial se usa como contexto con amortiguacion. No debe crear
logs falsos ni castigar de golpe al usuario si sus objetivos nuevos son mas
ambiciosos que su punto de partida.

Lectura:

```text
La app distingue entre "estoy empezando desde aqui" y "deje caer algo que ya
estaba sosteniendo".
```

### 24.3 Anclas

Las anclas son practicas recurrentes que construyen base.

Reglas cerradas:

```text
- registro diario;
- no multiples sesiones en v1;
- tap simple marca hecho con objetivo diario;
- long press permite deficit o superhabit de tiempo/cantidad;
- frecuencia semanal = dias Done;
- superhabit de frecuencia = mas dias que objetivo semanal;
- superhabit de tiempo/cantidad = valor real superior al objetivo diario;
- ancla no marcada al cierre = NotDone.
```

Vocal premia consistencia antes que acumulacion. Un dia enorme no reemplaza el
ritmo semanal.

### 24.4 Soportes

Los soportes son mantenimiento de base, no productividad.

Reglas cerradas:

```text
- UX de minima friccion;
- el usuario marca omisiones;
- soporte no omitido al cierre = Done;
- soporte omitido = Omitted;
- no ausencia moldeable despues del cierre;
- pesan mas que tasks porque sostienen el suelo cotidiano.
```

Un soporte no tiene que sentirse heroico. Sirve para detectar si el cuidado
minimo se esta sosteniendo o se esta cayendo.

### 24.5 Tasks / Pendientes

Las tasks no penalizan. Una task pendiente puede moverse al dia siguiente sin
castigo.

Reglas cerradas:

```text
- task sin capa no tiene valor de scoring;
- task con capa completada aporta TaskMomentum;
- TaskMomentum es positivo, pequeno y acotado;
- muchas tasks pequenas tienen rendimiento decreciente;
- TaskMomentum no repara una base caida;
- task completada queda asociada al dia de completado.
```

Decision:

```text
Tasks no entran como denominador duro del WeeklyBaseScore.
```

Razon:

```text
Si una task movible entrara como obligacion, se volveria deuda encubierta.
Vocal debe leer agencia y movimiento, no castigar pendientes.
```

### 24.6 Sueno

Sueno es un subsistema especial dentro de Cuerpo.

Reglas cerradas:

```text
- sueno semanal pesa 30% de Cuerpo;
- una sesion pertenece al dia donde empezo;
- una sesion puede cruzar medianoche;
- el modelo debe apuntar a telemetria local maxima;
- registrar inicio, fin, desbloqueos, interrupciones y confianza de fuente;
- si hay eventos atomicos, guardarlos como eventos hijos;
- no inventar calidad cuando la fuente no es confiable.
```

Sueno debe poder volverse inteligente con el tiempo. La v1 puede empezar con
menos datos, pero el modelo no debe cerrarse a duracion solamente.

### 24.7 Sobriedad

Sobriedad es opt-in y sensible.

Reglas cerradas:

```text
- si esta inactiva, no aparece, no pesa y no limita;
- si esta activa, entra en Conducta;
- es la unica feature con ausencia/pending moldeable;
- ventana de olvido: 5 dias;
- tras 5 dias sin respuesta, se materializa recaida asumida editable;
- si el usuario no levanta la recaida, el episodio sigue sumando dias;
- al levantar/relevar, el usuario corrige duracion real o acepta el rango;
- recaida penaliza fuerte, pero no hay gates duros.
```

La recaida no se trata como task fallida. Es una senal sensible de proteccion,
recuperacion y nuevo inicio.

Impacto aprobado en scoring:

```text
Si sobriedad esta inactiva:
ConductaScore = ConductaBaseScore

Si sobriedad esta activa:
ConductaScore =
0.700 * ConductaBaseScore
+ 0.300 * SobrietyWeeklyScore
```

Por track activo:

```text
EffectiveCleanDays = confirmedCleanDays + 0.5 * pendingDays
CleanCoverageScore = EffectiveCleanDays / evaluableDays

RelapseProtectionScore = exp(-relapseDays / 1.5)
TrackingConfidenceScore = 1 - 0.15 * (pendingDays / evaluableDays)

SobrietyTrackScore =
CleanCoverageScore
* RelapseProtectionScore
* TrackingConfidenceScore
```

Si hay varios tracks activos:

```text
SobrietyWeeklyScore =
0.700 * average(SobrietyTrackScore)
+ 0.300 * worst(SobrietyTrackScore)
```

Pendientes dentro de la ventana de 5 dias cuentan como `0.5` dia limpio. Una
recaida asumida penaliza igual que una manual hasta que el usuario la corrija.

### 24.8 Sin gates duros

El sistema nuevo no usa gates duros.

Regla:

```text
No bloquear estados por reglas binarias externas al score.
```

En su lugar:

```text
- pesos;
- penalizaciones;
- razones;
- peor capa;
- estabilidad temporal;
- memoria semanal derivada;
- explicaciones visibles.
```

Esto mantiene honestidad sin volver el sistema policial. Una recaida, sueno
bajo o capa caida deben sentirse en el numero y en las razones, no como una
condena opaca.

### 24.9 Estado Base

La pagina principal del scoring se llama:

```text
Estado Base
```

El dashboard solo muestra una simplificacion del reporte. `Estado Base` debe
mostrar:

```text
- score visible y estado;
- modo de lectura;
- capas;
- anclas;
- soportes;
- TaskMomentum;
- sueno dentro de Cuerpo;
- sobriedad dentro de Conducta;
- superhabit;
- peor capa;
- tendencia;
- estabilidad temporal;
- razones principales.
```

### 24.10 Memoria semanal derivada

El scoring inteligente necesita memoria temporal, pero la verdad primaria son
los hechos.

Regla:

```text
Hechos diarios -> agregacion semanal -> snapshot derivado versionado.
```

El snapshot semanal ayuda a:

```text
- stabilityScore;
- Inquebrantable;
- superhabits sostenidos;
- recomendaciones de subir metas;
- tendencias por capa;
- lectura historica;
- auditoria visual.
```

No debe ser verdad primaria. Debe poder borrarse y reconstruirse desde hechos,
configuracion y version del algoritmo.

Decision de fase:

```text
El snapshot persistido entra despues del motor estable y tests.
La agregacion semanal automatica desde hechos entra desde el motor inicial.
```

### 24.11 Amortiguacion inicial

La amortiguacion inicial dura una semana.

Regla:

```text
rawWeeklyReport = calculo normal desde hechos reales
initialAmortizedState = max(rawState, En marcha) durante la primera semana
```

Esto significa:

```text
- no se crean logs falsos;
- no se borra el score tecnico bruto;
- se suaviza la lectura visible hacia En marcha;
- al cerrar la primera semana completa desaparece la amortiguacion;
- la UI debe explicar que la base esta en construccion.
```

Razon:

```text
El usuario puede partir de una base declarada baja y objetivos nuevos altos.
La app debe reconocer ese punto de partida sin abrir con condena.
```

### 24.12 TaskMomentum como superhabit diario por capa

Tasks no tienen valor si no tienen capa. Si tienen capa y se completan, se leen
como impulso positivo o superhabit diario de esa capa.

Formula aprobada v0:

```text
TaskMomentumRaw = 1 - exp(-completedLayerTasks / 2)
TaskMomentumBonus = 0.050 * TaskMomentumRaw
```

Reglas:

```text
- solo cuentan tasks con capa;
- task pendiente no penaliza;
- task sin capa no vale para scoring;
- muchas tasks pequenas saturan rapido;
- TaskMomentum no repara anclas ni peor capa.
```

### 24.12.1 Soportes opt-in

Soportes no son obligatorios desde el inicio. Si una capa no tiene soportes
configurados, anclas sostienen el 100% de la base de esa capa.

```text
Sin soportes:
LayerBaseScore = AnchorLayerScore

Con soportes:
LayerBaseScore =
0.800 * AnchorLayerScore
+ 0.200 * SupportLayerScore
```

Tasks quedan fuera de la base y entran solo como `TaskMomentumBonus`.

### 24.12.2 Superhabit de anclas

El superhabit se separa en magnitud visible y bonus capado para score.

```text
FrequencyRatio = doneDays / targetDays
ValueRatio = actualValue / targetValue

FrequencySurplusMagnitude = max(0, FrequencyRatio - 1)
ValueSurplusMagnitude = max(0, ValueRatio - 1)

FrequencySurplusBonus =
0.100 * (1 - exp(-FrequencySurplusMagnitude / 2))

ValueSurplusBonus =
0.100 * (1 - exp(-ValueSurplusMagnitude / 2))

AnchorSurplusBonus =
0.700 * FrequencySurplusBonus
+ 0.300 * ValueSurplusBonus
```

La magnitud puede decir "hiciste 10x tu objetivo"; el bonus usa curva
decreciente para no incentivar metas artificialmente bajas.

Recomendaciones de metas:

```text
Superhabit sostenido de tiempo/cantidad -> sugerir revisar meta tras 7 dias.
Superhabit sostenido de frecuencia -> sugerir revisar meta tras 14 dias.
```

### 24.13 Estados sin gates duros (propuesta pendiente)

Propuesta v1 para discutir antes de cerrar contrato:

```text
Restauracion:
WeeklyBaseScore < 0.40 o peor capa en colapso fuerte.

Atencion:
0.40 <= WeeklyBaseScore < 0.70 o peor capa bajo margen minimo.

En marcha:
0.70 <= WeeklyBaseScore < 0.85 con base operativa suficiente.

Plenitud:
WeeklyBaseScore >= 0.85, peor capa suficientemente alta y penalizaciones bajas.

Inquebrantable:
WeeklyBaseScore >= 0.90, stabilityScore >= 0.90, peor capa alta y memoria temporal suficiente.
```

Parametros v1:

```text
worstLayerCollapse = 0.30
worstLayerMinimumForMotion = 0.55
worstLayerMinimumForPlenitude = 0.75
worstLayerMinimumForUnbreakable = 0.80
minimumWeeksForUnbreakable = 6
stateHysteresisMargin = 0.03
```

La recaida, el sueno bajo o una capa caida no bloquean por gate duro. Bajan la
lectura mediante sub-scores, penalizaciones, peor capa y estabilidad.

### 24.14 Cierre diario (propuesta pendiente)

Propuesta Codex: el cierre diario materializa hechos historicos mediante una
rutina idempotente. Debe validarse antes de implementacion.

Contrato propuesto:

```text
Anchor activa sin log -> NotDone.
Anchor activa con Done -> conservar Done y valor real.
Support sin omision -> Done.
Support omitido -> Omitted.
Task pendiente -> sin castigo.
Task completada con capa -> Done en dia de completado.
Sobriedad activa sin marca -> PendingConfirmation fuera de daily_activity_logs.
```

Ejecucion propuesta:

```text
- WorkManager intenta cerrar a medianoche local;
- al abrir la app despues de medianoche se ejecuta cierre de garantia;
- si hay varios dias pendientes, se cierran cronologicamente;
- el cierre es idempotente;
- se guarda timezoneId;
- no hay backfill libre para anclas, soportes ni tasks.
```

---

## 25. Nota de implementacion v0

Fecha: 2026-05-26

La primera implementacion de dominio ya reemplaza el scoring provisional por un
motor semanal v0 y mantiene esta especificacion como contrato conceptual.

Implementado:

```text
- anclas 70% frecuencia / 30% valor;
- soportes opt-in 80/20 cuando existen;
- tasks como TaskMomentum positivo y acotado;
- sueno dentro de Cuerpo al 30% con datos disponibles;
- sobriedad activa dentro de Conducta al 30%;
- sobriedad inactiva sin peso;
- peor capa al 25%;
- sin gates duros;
- Inquebrantable reservado para memoria temporal;
- cierre diario idempotente de garantia al abrir dashboard;
- snapshot semanal como entidad derivada versionada, aun sin escritura.
```

Pendiente:

```text
- WorkManager a medianoche local;
- telemetria avanzada de sueno;
- recaidas asumidas por rango editable;
- StabilityScore;
- pagina Estado Base.
```
