# Autonomía sin límites — Filosofía de producto

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Fecha: 2026-05-21  
Estado: Documento conceptual base  
Propósito: ordenar la teoría interna del producto antes de cerrar configuración, score y dashboard.

---

## 1. Idea central

Autonomía sin límites parte de una intuición práctica:

```text
Una persona se parece a una mesa.
Si una pata queda demasiado baja, toda la mesa pierde estabilidad.
```

La app no intenta diagnosticar salud mental ni medir el valor de una persona.  
Intenta ayudar al usuario a observar si está sosteniendo sus bases diarias.

La estabilidad no se entiende como felicidad permanente, productividad extrema o perfección.  
Se entiende como una base mínima suficientemente cuidada para no entrar en ciclos de caída.

---

## 2. Problema que resuelve

Muchas personas no caen de golpe.  
Caen por abandono progresivo de sus bases.

Ejemplos:

- la persona se enfoca demasiado en un proyecto;
- deja de dormir bien;
- deja de moverse;
- se aísla;
- abandona meditación, lectura o reflexión;
- se desordena conductualmente;
- aparece alcohol, evasión, impulsividad o autosabotaje;
- el proyecto que parecía avanzar termina cayendo también.

Autonomía sin límites existe para detectar ese patrón antes de que se vuelva una bola de nieve.

La app no dice:

```text
Estás mal.
```

La app dice:

```text
Hay una señal. Volvamos a la base.
```

---

## 3. Principio de equilibrio

La teoría del producto propone que una vida funcional se sostiene con varias dimensiones al mismo tiempo.

No basta con avanzar en proyectos si el cuerpo está abandonado.  
No basta con meditar si la conducta diaria está rota.  
No basta con trabajar mucho si los vínculos están secos.  
No basta con tener metas si no hay sueño, cuerpo ni estructura.

La app busca que el usuario vea su vida como un sistema de soporte, no como una lista infinita de tareas.

---

## 4. Las cinco capas

Las capas son dimensiones de vida.

No son categorías clínicas.  
No son moral.  
No son una fórmula universal de felicidad.

Son una forma práctica de preguntarse:

```text
¿Qué parte de mi vida se está sosteniendo o cayendo?
```

### 4.1 Interior

Representa el mundo interno de la persona.

Incluye conciencia, reflexión, aprendizaje, silencio, lectura, meditación, escritura, claridad y crecimiento personal.

No debe sentirse como religión obligatoria, espiritualidad forzada ni terapia falsa.

Ejemplos:

- meditar;
- leer;
- escribir una línea honesta;
- reflexionar;
- revisar dirección personal;
- aprender;
- desconectarse del ruido.

### 4.2 Cuerpo

Representa la base física.

Incluye movimiento, higiene, alimentación, descanso y cuidado corporal.

No es fitness, estética ni rendimiento deportivo.  
Es sostener el cuerpo para que la vida no se caiga desde lo básico.

Ejemplos:

- ejercicio;
- caminar;
- dormir;
- tomar agua;
- comer en casa;
- bañarse;
- cepillarse los dientes;
- cambiarse de ropa;
- descansar.

### 4.3 Conducta

Representa patrones de autocontrol, límites, orden diario y prevención del autosabotaje.

No debe ser una lista de pecados ni una lista de prohibiciones.  
Debe representar tanto evitar autosabotaje como construir ritmo conductual.

Ejemplos:

- no usar celular antes de dormir;
- dejar celular fuera de la cama;
- no decidir desde madrugada;
- preparar el día siguiente;
- cerrar el día con una revisión breve;
- cocinar en casa en vez de caer siempre en delivery;
- sostener sobriedad;
- abrir modo riesgo antes de actuar.

### 4.4 Vínculos

Representa contacto humano y relaciones importantes.

No significa tener muchos amigos.  
No significa vida social intensa.  
No significa agradarle a todos.

Significa no vivir completamente aislado de los vínculos que sostienen humanidad, comunidad y pertenencia.

Ejemplos:

- hablar con alguien importante;
- responder mensajes pendientes;
- llamar a un familiar;
- ver a alguien en persona;
- pedir perdón;
- poner un límite;
- reparar una conversación.

### 4.5 Proyecto

Representa aquello que la persona construye y que forma parte de su identidad.

No es solo productividad.  
No es solo trabajo.  
No es solo negocio.

Es la dimensión donde la persona ve frutos concretos de su esfuerzo y siente que está construyendo una dirección.

Ejemplos:

- avanzar un proyecto personal;
- terminar la universidad;
- construir un negocio;
- estudiar para ascender;
- componer música;
- practicar una habilidad;
- crear algo;
- terminar una obra o entrega concreta.

Nota importante:

Un objetivo puede tener relación con varias capas.  
Por ejemplo, dejar de beber puede ser un proyecto personal, pero dentro del sistema vive mejor como conducta/sobriedad. Tener buen físico puede sentirse como proyecto, pero vive mejor en cuerpo. La capa Proyecto se reserva para construcción concreta de futuro, identidad, creación, trabajo o aprendizaje.

---

## 5. Qué mide Autonomía sin límites

Autonomía sin límites no mide salud mental en sentido clínico.

Mide una lectura operativa de base:

```text
¿Qué tanto está sosteniendo el usuario sus anclas personales durante el tiempo?
```

Esa lectura se forma a partir de inputs como:

- prácticas base;
- cuidado básico;
- pendientes relevantes;
- sueño;
- sobriedad o abstinencias;
- eventos de riesgo;
- progreso por capas;
- consistencia semanal.

La app debe evitar convertir un mal día en identidad.  
Un día bajo es una señal, no una condena.

---

## 6. Estados de base

Los estados no deben llamarse internamente “estado mental” en la UI principal.  
El término recomendado es:

```text
Estado de base
```

Porque representa cómo va la base personal del usuario, no una etiqueta clínica sobre su mente.

Estados actuales:

| Enum técnico | Nombre visible | Rango | Lectura |
| --- | --- | ---: | --- |
| `NoData` | Sin datos | — | Aún no hay registros suficientes. |
| `Restoration` | Restauración | 700-749 | Base baja. Requiere cuidado mínimo. |
| `Attention` | Atención | 750-799 | Hay margen, pero la base está cediendo. |
| `Motion` | En marcha | 800-899 | Base activa y suficientemente sostenida. |
| `Plenitude` | Plenitud | 900-949 | Base sostenida con consistencia alta. |
| `Unbreakable` | Inquebrantable | 950-1000 | Núcleo muy sólido. Pico orgánico, no obligación. |

Regla conceptual:

```text
En marcha es el hogar operativo de la app.
```

El objetivo realista del usuario promedio no es vivir siempre en Plenitud o Inquebrantable.  
El objetivo sano es sostener una base activa, estable y recuperable.

---

## 7. Score de base

Nombre recomendado:

```text
Score de base
```

No “score mental”.  
No “salud mental”.  
No “nivel de vida”.

El score debe representar una lectura acumulada de estabilidad práctica.

Principios:

- no debe reaccionar de forma exagerada a un solo mal día;
- no debe regalar estados altos por completar muchas cosas una vez;
- debe necesitar consistencia para subir;
- debe bajar cuando hay abandono real de bases;
- debe usar una ventana mínima de datos para ser confiable;
- debe tener una fase inicial provisional mientras se reúnen registros.

Propuesta conceptual:

```text
Días 1-7:
score provisional de arranque, útil para feedback inmediato.

Días 7-15:
score de transición, empieza a leer consistencia real.

Después:
score de base más confiable, calculado con historial semanal/multidía.
```

Esto evita que el usuario sienta que la app está “muerta” al inicio, pero también evita vender una lectura falsa como definitiva.

---

## 8. Tono de producto

La comunicación de Autonomía sin límites es parte del producto.

La voz base es:

```text
El Cuidador Lúcido
```

Características:

- compasivo;
- directo;
- maduro;
- no moralista;
- no clínico;
- no humillante;
- no coach motivacional;
- no app de productividad agresiva.

La app debe sostener esta tensión:

```text
No castiga.
Pero tampoco permite que la inacción prolongada se disfrace de descanso.
```

Frase guía:

```text
No toca castigarte. Toca volver a la base.
```

---

## 9. Filosofía de recuperación

Cuando el usuario cae, la prioridad no es explicar toda su vida.  
La prioridad es cortar el circuito.

Orden recomendado en caída:

1. cuerpo;
2. sueño/comida/agua/higiene;
3. una acción mínima;
4. revisión posterior;
5. ajuste del sistema.

La app debe actuar como una cuerda, no como un juez.

---

## 10. Qué no es Autonomía sin límites

Autonomía sin límites no es:

- un terapeuta;
- un diagnóstico;
- una app de productividad;
- una app de hábitos genérica;
- un juego de puntos;
- una app de castigo;
- una moral universal;
- una vigilancia punitiva.

Autonomía sin límites es:

```text
Una estructura de cuidado personal configurable para sostener la base de una persona.
```

---

## 11. Relación entre filosofía y arquitectura

La arquitectura debe obedecer esta filosofía.

Regla central:

```text
Room guarda hechos.
El dominio interpreta hechos.
Compose presenta estado y envía acciones.
```

Esto significa:

- la base de datos no debe guardar conclusiones emocionales rígidas;
- la UI no debe inventar lógica de salud mental;
- el dominio debe interpretar registros con cuidado;
- el dashboard debe mostrar señales sin diagnosticar.

---

## 12. Decisiones filosóficas cerradas

- La app trabaja con cinco capas: Interior, Cuerpo, Conducta, Vínculos y Proyecto.
- La app mide base personal configurable, no moral universal.
- El estado visible recomendado es `Estado de base`.
- El score visible recomendado es `Score de base`.
- `En marcha` debe sentirse como éxito real y hogar operativo.
- `Plenitud` e `Inquebrantable` son picos de consistencia, no obligación diaria.
- El tono debe proteger sin consentir el abandono prolongado.
- La app debe detectar abandono de bases antes del colapso.
- La app debe evitar lenguaje clínico, culpa, vergüenza y promesas grandilocuentes.

---

## 13. Principio de mínima fricción

La aplicación está pensada para sostener al usuario, especialmente en sus momentos más difíciles, de desbalance o recaídas. Por lo tanto, la fricción que debe generar la interacción debe ser mínima. Usar Autonomía sin límites no puede sentirse como una tarea pesada o una obligación agotadora.

Para lograr esto, el producto se estructura en diferentes herramientas de uso ágil:

- **Filtro principal de sueño:** El sueño es la base de todo, impacta tanto en el cuerpo como en la conducta. Es el cimiento físico y conductual que determina la estabilidad diaria.
- **Checklists principales y secundarias:** Para separar lo innegociable (core) de lo complementario.
- **Task lists:** Para tareas y pendientes puntuales.
- **Rachas de sobriedad o abstinencia:** Seguimiento visual directo sin fricción.

Además, para mantener la motivación y reducir la carga psicológica, el producto implementa un sistema de **frases rotativas (más de 87 frases seleccionadas)**. Estas frases están elegidas con extremo cuidado para apoyar al usuario tanto en su mejor como en su peor fase, respetando siempre el tono compasivo y maduro.

---

## 14. Personalización profunda

Autonomía sin límites nace de una necesidad personal: la ausencia en el mercado de una aplicación con un nivel de complejidad y personalización que se adapte realmente a las necesidades de cada individuo.

Aunque el núcleo del producto siempre serán las **Cinco Capas** y las **actividades base** (que ayudan a construir hábitos y mantienen la base de estabilidad, teniendo predeterminadamente el mayor peso), el resto del sistema es altamente personalizable.

- **Configuración por usuario:** No todas las personas necesitan las mismas features. Alguien lidiando con depresión profunda podría necesitar solo unas pocas checklists principales y ningún seguimiento de sobriedad.
- **Pesos dinámicos:** El progreso y las métricas se miden en base a cómo el usuario configure sus features y el peso que decida darle a cada una dentro de sus dimensiones de vida.
- **Adaptabilidad:** Cada checklist y herramienta puede crearse y moldearse en relación al dominio del producto, permitiendo que Autonomía sin límites se adapte a lo que el usuario considera vital en ese momento.

---

## 15. Pendiente conceptual importante

Todavía falta cerrar profesionalmente el algoritmo del score.

Este documento no define fórmula.  
Solo define la filosofía que debe respetar esa fórmula.
