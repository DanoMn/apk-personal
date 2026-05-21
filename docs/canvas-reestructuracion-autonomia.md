# Canvas de reestructuracion - Autonomia sin limites

Este documento es el espacio vivo para iterar ideas antes de tocar codigo. La regla es simple: primero aclaramos producto, experiencia, datos y alcance; luego implementamos por fases.

Nota de init 2026-05-20:

- Este canvas conserva historia y decisiones utiles.
- Para el estado vigente del producto, leer primero `docs/estado-actual-mvp.md`.
- Para reglas visuales actuales, leer `docs/frontend-design.md` y `docs/prototipo/index.html`.
- Varias ideas de este canvas ya pasaron de "futuras" a "direccion vigente", especialmente Room, dashboard real y drawer lateral.

Research complementario:

- `docs/nucleo-dominio-autonomia.md`
- `docs/research-apps-similares.md`
- `docs/especificacion-actividades-sobriedad-v1.md`

## Norte del producto

La app no debe sentirse como una checklist generica. Debe sentirse como un sistema personal para volver al eje, registrar el dia y detectar patrones antes de repetir el ciclo.

Idea central:

> Abro esto para no volver a repetir el mismo ciclo.

La app debe servir para:

- Recordar identidad y direccion cuando la lucidez baja.
- Ver rapidamente como va el dia, la semana y el mes.
- Registrar acciones pequenas sin friccion.
- Medir practica real, no solo intencion.
- Detectar riesgo antes de crisis.
- Crecer hacia tracking de sueno, comida, entrenamiento, consumo y proyectos sin volverse caotica.

Brujula emocional:

- La app debe sentirse como un adulto funcional y compasivo.
- No debe sentirse como juez, policia o padre autoritario.
- Debe ensenar autocuidado basico sin humillar.
- Debe ayudar a volver al cuerpo y a la estructura cuando el usuario se siente abandonado por si mismo.
- La dignidad esta en hacer lo basico, no en perseguir perfeccion.

## Feedback validado de la version 0.2.0

### Funciona mejor

- La barra de progreso gusta.
- El mapa mensual es intuitivo.
- La navegacion y estructura mejoraron frente a la primera version.

### Hay que pulir

- El inicio no deberia abrir directamente en una lista larga de paneles.
- "Hoy" como pantalla principal se siente pobre; hace falta un dashboard real.
- Las acciones minimas estan demasiado hardcodeadas y parecen aleatorias.
- La checklist deberia estar accesible desde el dashboard, no ocupar todo el inicio.
- El modo riesgo debe seguir siendo mas explicito y accionable.
- La navegacion inferior con tres botones no escala para una app que luego tendra mas modulos.
- Export/import es importante, pero va al final; antes hay que definir bien el modelo de datos.

## Decisiones actualizadas

- Las seis capas propuestas quedan aceptadas como base.
- Las actividades dentro de cada capa deben ser editables.
- La app debe traer actividades predeterminadas, pero el usuario debe poder modificarlas.
- Cada actividad debe representar algo fisicamente medible en la realidad.
- El progreso no debe medir solo si se marco un check; tambien debe poder medir tiempo real dedicado.
- El estado del dia debe ser calculado, no manual ni hibrido.
- Puede existir un boton de panico/riesgo, pero como evento o atajo de emergencia, no como seleccion manual del estado.
- La navegacion objetivo debe reemplazar la bottom nav por un drawer/panel lateral.
- Las frases ancla seran 30, no 50.
- Las frases deben definirse primero en Markdown antes de implementarlas.
- La edicion de frases propias debe vivir en configuracion, no en el menu principal.
- Export/import se mueve a una fase final, no a una fase temprana.
- La sobriedad/abstinencias deben ser una feature propia, no solo un item del checklist.
- La sobriedad/abstinencias deben aparecer en dashboard como marca diaria y racha.
- Las rachas de abstinencia deben ser configurables: alcohol, marihuana, conducta sexual u otras.
- La migracion va directamente sobre Room como modelo local formal.

## Propuesta de arquitectura UX

### Pantalla inicial objetivo: Dashboard

Objetivo: abrir la app y entender en 5 segundos donde estoy.

Debe mostrar:

- Frase ancla diaria rotativa.
- Estado del dia calculado.
- Progreso del dia por capas.
- Grafico/resumen de practica real.
- Acceso rapido a checklist.
- Acceso rapido a modo riesgo.
- Resumen semanal minimo.
- Senales importantes si existen: sueno, consumo, cuerpo, foco, comida.

No debe mostrar:

- Una lista larga de todas las tareas al inicio.
- Texto motivacional generico.
- Botones sin contexto.

### Navegacion objetivo

La navegacion inferior actual sirve para MVP, pero debe reemplazarse por una navegacion escalable.

Direccion:

- Drawer lateral desplegable.
- Posible gesto desde borde izquierdo.
- Dashboard como home.
- Accesos principales:
  - Dashboard
  - Checklist
  - Riesgo
  - Progreso
  - Registros
  - Capas y actividades
  - Configuracion

Exportar/importar no debe ser una entrada principal todavia. Puede vivir en configuracion cuando llegue su fase.

## Modelo de capas de vida

La checklist objetivo no debe ser una lista plana. Debe separarse en capas editables.

Capas aceptadas como base:

1. Espiritual / interior
   - Meditar.
   - Escritura.
   - Gratitud.
   - Paz consigo mismo.
   - Respeto propio.

2. Fisico / cuerpo
   - Gimnasio.
   - Caminar.
   - Dormir mejor.
   - Cuidar energia.
   - No sacrificar el cuerpo por productividad.

3. Conductual / sobriedad / autocontrol
   - No alcohol.
   - No marihuana.
   - Conducta sexual, si esta activa para el usuario.
   - No celular antes de dormir.
   - No decidir desde madrugada, rabia o cansancio.
   - Evitar conductas destructivas definidas como actividades concretas y medibles.

4. Alimentacion / cuidado domestico
   - Cocinar.
   - Comer dentro del rango prometido.
   - Registrar comidas.
   - Orden minimo de casa.

5. Social / vinculos / persona
   - Mejorar comportamientos con otros.
   - No aislarse destructivamente.
   - Comunicacion mas limpia.
   - Cuidar relaciones.

6. Proyecto / identidad creativa
   - Digitaliza.
   - Musica.
   - Anatomia de la ausencia.
   - Aprendizaje.
   - Construir futuro real.

Nota sobre Digitaliza y musica:

- Digitaliza puede ser economico/productivo, pero tambien esta ligado a identidad.
- Musica es mas identitaria, pero tambien es practica y construccion.
- La capa de proyecto/identidad creativa debe permitir ambas cosas sin separarlas artificialmente demasiado pronto.

## Sistema de actividades

Este es el nucleo de la siguiente reestructuracion.

Problema actual:

- Los habitos estan hardcodeados.
- No hay capas reales.
- No hay edicion.
- No hay diferencia entre hacer/no hacer, tiempo, frecuencia o registro.

Direccion:

- Cada capa contiene actividades.
- Las actividades son full editables.
- La app trae actividades predeterminadas.
- Cada actividad debe representar algo medible en la realidad.
- Cada actividad puede tener objetivo y registro real.

### Tipos de actividad

Tipos iniciales:

- Check simple: se hizo o no se hizo.
- Abstinencia / no hacer: no beber, no fumar, no usar celular antes de dormir.
- Tiempo: meditar 5 min, gimnasio 30 min, musica 20 min.
- Frecuencia semanal: gimnasio 3 veces por semana.
- Hora: dormir temprano, despertar.
- Conteo: vasos de agua, comidas caseras, sesiones.
- Nota corta: registrar comida, estado o contexto.

### Objetivo vs tiempo real

Hay que distinguir:

- Tiempo objetivo: lo que se planea invertir.
- Tiempo real: lo que realmente se hizo.

Ejemplo:

- Objetivo: gimnasio 30 minutos.
- Registro real: gimnasio 60 minutos.
- Resultado: cuenta como cumplido y ademas suma practica extra.

Regla conceptual:

- Si el tiempo real alcanza o supera el objetivo, la actividad se marca como cumplida.
- Si el tiempo real supera el objetivo, no se pierde informacion: queda registrado como excedente positivo.
- No se debe convertir en competencia infinita; el objetivo es ver consistencia y mejora, no obsesion.

### Niveles y progresion

La app debe permitir crecer de forma gradual.

Ejemplo:

- Nivel 1: meditar 5 minutos antes de dormir.
- Nivel 2: meditar 10 minutos.
- Nivel 3: meditar 15 minutos.
- Nivel 4: meditar 30 minutos.

La progresion puede ser manual al inicio. Mas adelante podria sugerirse en base a consistencia.

### Actividades iniciales importantes

Prioridad inicial del usuario:

- Meditar 5 minutos antes de dormir.
- No usar celular antes de dormir.
- Ir al gimnasio al menos 3 veces por semana.
- Dormir temprano.
- Cuidar alimentacion.
- Cuidado personal basico: banarse, cepillarse dientes, volver al cuerpo.
- Avanzar diariamente musica.
- Avanzar diariamente Digitaliza.
- No alcohol.
- Conducta sexual, si esta activa.
- No marihuana, aunque actualmente no sea prioridad personal porque no hay consumo.

## Estado del dia

Decision:

- El estado del dia debe ser calculado.
- No debe ser manual.
- No debe ser hibrido.

El boton de riesgo/panico puede existir, pero no como selector de estado. Debe registrar un evento de riesgo o abrir un protocolo.

Estados futuros posibles:

- Sin datos: todavia no empece.
- Bajo movimiento: hay pocas acciones o poco tiempo invertido.
- En marcha: ya existe practica real durante el dia.
- Estable: progreso suficiente y sin senales rojas.
- Riesgo: pocas acciones, hora avanzada, mal sueno, consumo tentador o evento de panico.
- Crisis: recaida, impulso fuerte o registro explicito de crisis.
- Recuperacion: despues de crisis, objetivo es no empeorar.

Variables que podrian alimentar el calculo:

- Porcentaje de actividades cumplidas.
- Minutos reales practicados.
- Actividades criticas incumplidas.
- Marca diaria de sobriedad/abstinencias activas.
- Hora del dia.
- Sueno.
- No celular antes de dormir.
- Consumo o abstinencia.
- Evento de riesgo/panico.

## Progreso

Ya gustan:

- Barra de progreso.
- Mapa mensual.

Agregar:

- Grafico de barras por capas para ver donde voy bien y mal.
- Promedio semanal por capa.
- Promedio mensual por capa.
- Comparacion entre sobriedad, cuerpo, interior, casa, vinculos y proyecto.
- Indicador de consistencia.
- Minutos reales invertidos por actividad/capa.

Idea de visualizacion:

```text
[CAPAS / 7 DIAS]
Interior      ####--- 4/7
Cuerpo        ##----- 2/7
Conducta      ######- 6/7
Casa/comida   ###---- 3/7
Vinculos      #------ 1/7
Proyecto      #####-- 5/7
```

Idea de visualizacion por tiempo:

```text
[MINUTOS / 7 DIAS]
Meditacion       35 min
Gimnasio        150 min
Musica          120 min
Digitaliza      210 min
Orden casa       45 min
```

## Frases ancla

Problema actual:

- La frase es fija.

Direccion:

- Set de 30 frases.
- Rotacion diaria deterministica segun fecha.
- Mantener tono directo, no coach, no clinico, no motivacional barato.
- Definir primero en Markdown.
- La personalizacion de frases debe vivir en configuracion, no en menu principal.

Categorias de frases:

- No repetir ciclo.
- No tocar fondo.
- Cuerpo primero.
- Sobriedad.
- Digitaliza y futuro.
- Musica / identidad creativa.
- Riesgo y madrugada.
- Volver a mi.

Tarea futura:

- Crear `docs/frases-ancla.md` con 30 frases antes de implementarlas.

## Tracking futuro

No todo entra ahora, pero el modelo debe permitir crecer.

### Sueno

Muy importante para futuro.

Registrar:

- Hora de dormir.
- Hora de despertar.
- Calidad subjetiva.
- Si hubo celular en cama.

Uso futuro:

- Influir en estado del dia.
- Mostrar patrones de riesgo.
- Relacionar sueno con practica real.

### Alimentacion

Mas adelante:

- Registrar comidas.
- Marcar si se mantuvo dentro del rango prometido.
- Posible campo de notas o resumen generado fuera de la app.

No implementar todavia:

- Calorias detalladas.
- Base de datos de alimentos.
- Complejidad tipo app fitness.

### Consumo

Registrar:

- Alcohol: si/no.
- Marihuana: si/no.
- Impulso sin consumo.
- Recaida.

## Exportar / importar

Decision actual:

- Import/export es importante, pero va al final.
- No debe condicionar la siguiente reestructuracion.
- Antes hay que definir bien capas, actividades y registros.
- Antes de export/import hay que estabilizar que datos existen, que campos se guardan y que base de datos/formato local se usara.

Motivo futuro:

- Si se reinstala la APK o cambia el modelo de datos, no se debe perder el tracking.

Requisitos futuros:

- Exportar datos locales a archivo JSON.
- Importar archivo JSON.
- Incluir version de esquema.
- Incluir fecha de exportacion.
- Permitir migraciones simples cuando cambie el modelo.

Formato conceptual futuro:

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-05-19T00:00:00",
  "layers": [],
  "activities": [],
  "dailyLogs": []
}
```

## Modelo de datos futuro

Entidades candidatas:

- Layer
- Activity
- ActivityTarget
- DailyLog
- ActivityLog
- RiskEvent
- SleepEntry
- MealEntry
- AnchorPhrase
- AppSettings

Campos candidatos para `Activity`:

- id
- layerId
- name
- description
- type
- active
- archived
- targetValue
- targetUnit
- frequency
- importance
- level
- sortOrder

Campos candidatos para `ActivityLog`:

- id
- date
- activityId
- completed
- actualValue
- actualUnit
- note
- createdAt
- updatedAt

Persistencia:

- La app ya migro a Room como base local formal.
- El esquema v1 esta documentado en `docs/definicion-tablas-room-v1.md`.
- Room guarda hechos: capas, actividades, logs diarios, rachas y eventos de riesgo.
- Las inferencias de estado deben seguir viviendo fuera de Compose.
- Si mas adelante se agregan sueno, comida, uso de celular o patrones historicos, deben entrar como tablas nuevas versionadas.
- Export/import debe esperar hasta que este esquema este suficientemente claro.

Decision tomada:

- Evitar transicion larga con JSON.
- Partir desde Room v1 para que el dominio pueda crecer con migraciones claras.

## Backlog por fases

### Fase 1 - Checklist estructurada y dashboard

- Dashboard inicial real.
- Reemplazar bottom nav por drawer lateral.
- Migrar a modelo local formal para capas, actividades y logs.
- Checklist movida a pantalla propia.
- Seis capas base.
- Actividades predeterminadas agrupadas por capa.
- Actividades con tipo: check, abstinencia, tiempo, frecuencia semanal, hora, nota.
- Registro de objetivo vs valor real para actividades de tiempo.
- Sobriedad/abstinencias como modulo propio visible en dashboard.
- Rachas configurables para alcohol, marihuana, conducta sexual u otras abstinencias.
- Cuidado personal basico como categoria diferenciada de productividad.
- Objetivos semanales visibles, por ejemplo gimnasio 3 veces por semana.
- Progreso por capas con grafico de barras.
- Estado del dia calculado en version inicial simple.
- Mantener modo riesgo como boton/protocolo/evento.

### Fase 2 - Edicion completa de capas y actividades

- Crear capa.
- Editar capa.
- Archivar capa.
- Crear actividad.
- Editar actividad.
- Archivar actividad.
- Configurar objetivo, unidad, frecuencia y nivel.
- Reordenar actividades.
- Reordenar capas.
- Configuracion de frases ancla, sin ponerlo en menu principal.

### Fase 3 - Sueno y senales corporales

- Hora de dormir.
- Hora de despertar.
- Calidad de sueno.
- Relacionar sueno con estado calculado.
- Relacionar sueno con riesgo y practica real.

### Fase 4 - Alimentacion simple

- Registro simple de comidas.
- Check de rango prometido.
- Notas libres.

### Fase 5 - Portabilidad y backups

- Exportar JSON.
- Importar JSON.
- Versionar esquema.
- Validar datos antes de importar.
- Solo iniciar cuando el modelo de datos local este estable.

## Preguntas abiertas

- Cuales actividades predeterminadas exactas van dentro de cada capa.
- Como se calcula inicialmente el estado del dia sin hacerlo demasiado complejo, despues de tener datos base.
- Que umbrales definen estable, bajo movimiento, riesgo y crisis, despues de implementar logs confiables.
- Si la progresion por niveles sera manual al inicio o sugerida por consistencia.
- Si la primera implementacion de tiempo real sera por input manual simple o timer.
- Cuales campos exactos deben ser obligatorios para una actividad y para un registro diario.

## Decisiones actuales

- El canvas ya cumplio su primera funcion exploratoria; ahora las decisiones vigentes se consolidan en `docs/estado-actual-mvp.md`, `docs/frontend-design.md` y `docs/tono-comunicacion.md`.
- El siguiente cambio grande debe ser de estructura UX y modelo de actividades, no solo colores.
- El progreso debe mostrar capas y tiempo real, no solo cantidad total de checks.
- La app debe estar disenada para crecer hacia tracking diario mas amplio.
- El foco inmediato es definir bien checklist, capas, actividades, tipos medibles y estado calculado.
- La investigacion externa refuerza el modelo: capa -> actividad editable -> objetivo -> registro real -> senal para progreso/estado.
- La arquitectura debe separar hechos guardados de inferencias calculadas: Room guarda hechos, dominio calcula estado/senales/recomendaciones, Compose presenta.
- Prioridad inmediata antes de umbrales avanzados: schema/tablas y checklist base funcionando.
- Deteccion de patrones historicos queda como feature futura, cuando existan datos confiables.
