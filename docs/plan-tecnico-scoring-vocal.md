# Plan tecnico del sistema de scoring de Vocal

Estado: fuente de verdad tecnica viva; implementacion v0 en curso
Fecha: 2026-05-26
Proyecto: Vocal / Autonomia sin limites

Este documento guia la implementacion del sistema de scoring completo de Vocal.
Reemplaza el plan operativo previo cuando haya conflicto entre una idea
conceptual y la realidad del codigo.

La referencia canonica de formulas, variables y arbol matematico esta en:

```text
docs/arbol-scoring-vocal-v1.md
```

Los documentos `sistema-scoring-semanal-vocal-v1.md` y
`plan-implementacion-scoring-vocal.md` quedan como documentos exploratorios:
aportan lenguaje, reglas y direccion, pero no son canon tecnico cerrado.

## 1. Decisiones actuales

1. El `ScoreEngine` actual es provisional. Puede reemplazarse por completo.
2. No hace falta mantener compatibilidad de comportamiento con el score actual.
3. El sistema nuevo debe respetar el nucleo de dominio:
   - Room guarda hechos.
   - El dominio calcula inferencias.
   - El ViewModel expone estado.
   - Compose presenta estado y envia acciones.
4. El trabajo actual es el sistema de scoring completo.
5. La metrica diaria/readiness queda como indicador futuro, separado del trabajo
   actual.
6. Antes de implementar formulas finales, hay que cerrar el sistema de registro
   de hechos diarios/semanales.
7. El score no mide salud mental clinica, felicidad, valor personal ni moral.
   Mide que tanto el usuario sostiene la base configurada.
8. Scoring debe tener una pagina propia para mostrar el reporte completo.
9. El primer componente del dashboard es una simplificacion del reporte de
   scoring, no el lugar donde vive toda la explicacion.
10. El registro nuevo de actividades va en una tabla nueva limpia, no como
    parche semantico sobre `activity_logs`.
11. La pagina de scoring sera una pantalla principal propia, accesible desde el
    menu lateral y la navegacion principal. El dashboard solo muestra un resumen.
12. La tabla nueva de registros diarios sera la unica fuente de verdad para
    anclas, soportes y tasks dentro del scoring nuevo.
13. El dia operativo cierra a medianoche local segun la zona horaria del
    usuario.
14. El registro de anclas no maneja multiples sesiones en v1. Maneja estado
    diario y valor real: hecho/no hecho + tiempo o cantidad.
15. La frecuencia semanal de anclas se calcula como cantidad de dias marcados
    como hechos dentro de la semana.
16. El superhabit existe en dos formas: mas dias que la frecuencia objetivo y
    mas tiempo/cantidad que el objetivo diario.
17. Para anclas, soportes y tasks no existe "no registrado" como estado de
    negocio. Durante el dia el usuario puede modificar estados; al cierre
    local, todo lo configurado queda consolidado como hecho, no hecho u omitido
    segun corresponda.
18. Tasks no penalizan. Si tienen capa y se completan, aportan
    `TaskMomentum` positivo y acotado. Si no tienen capa, no tienen valor de
    scoring.
19. La configuracion inicial sera obligatoria: minimo 3 capas y al menos 1
    ancla por cada una de esas capas.
20. No hay gates duros. El sistema debe reflejar la realidad con pesos,
    penalizaciones, razones y calculos internos, no con bloqueos absolutos.
21. Los datasets canonicos de anclas y soportes, y la configuracion de usuario
    existente, deben preservarse. La tabla nueva corrige la semantica de logs,
    no borra catalogos ni configuraciones.
22. La unica feature que permite ausencia/pending moldeable por el usuario es
    sobriedad, por sensibilidad del dominio y por la ventana de olvido de 5
    dias.
23. La base declarada inicial se usa como contexto con amortiguacion. No crea
    logs falsos ni castiga de golpe al usuario por tener objetivos nuevos mas
    altos que su punto de partida.
24. El barrido diario debe intentar ejecutarse en segundo plano a medianoche
    local y tambien al abrir la app despues de medianoche como garantia.
25. Las tasks con capa se tratan como plus de impulso/momentum, no como deuda:
    pueden sumar una senal pequena, pero no penalizan si quedan pendientes.
26. El sistema de sueno debe apuntar a telemetria maxima local-first: inicio,
    fin, desbloqueos, interrupciones y confianza de la fuente cuando sea
    posible.
27. En sobriedad, despues de 5 dias sin respuesta se materializa una recaida
    asumida editable. Si el usuario no la levanta, se siguen sumando dias hasta
    que corrija o acepte el episodio.
28. El nombre visible de la pagina principal de scoring sera `Estado Base`.
29. Los registros legacy de usuario pueden ignorarse o descartarse durante
    desarrollo si no afectan datasets canonicos ni configuracion de usuario.
30. Decision aprobada: la amortiguacion inicial dura una semana y suaviza la
    lectura hasta el estado intermedio `En marcha`, sin crear logs falsos ni
    ocultar el reporte tecnico bruto.
31. Decision aprobada: las tasks solo cuentan si tienen capa. Si se completan,
    aportan `TaskMomentum` como superhabit/momentum diario de la capa asociada.
    No estan ligadas a goals ni a soportes, no penalizan pendientes y no sirven
    para abusar del score.
32. Decision aprobada: `WeeklyScoreSnapshotEntity` entra despues del motor
    estable y tests, como cache/historial derivado, versionado y recalculable.
33. Implementacion v0: el cierre diario es idempotente, registra fechas
    cerradas y procesa en orden cronologico los dias vencidos de la semana.
    Se ejecuta como garantia al abrir el dashboard y se programa con
    WorkManager alrededor de la medianoche local.
34. Implementacion consolidada: `daily_activity_logs` es la fuente diaria
    canonica para anclas, soportes y tasks. `activity_logs` queda como tabla
    legacy migrada y fuera del flujo nuevo.

## 1.1 Estado de validacion de propuestas Codex

No todo lo escrito en este documento esta sellado como contrato. Las propuestas
que Codex haya formulado deben revisarse explicitamente antes de pasar a
implementacion.

| Tema | Estado |
| --- | --- |
| Amortiguacion inicial de una semana hasta `En marcha` | Decision aprobada. |
| Soportes opt-in: sin soportes, anclas 100%; con soportes, 80/20 | Decision aprobada. |
| Tasks solo valen con capa y no deben abusar del score | Criterio conceptual aprobado. |
| `TaskMomentum` como superhabit diario por capa | Decision aprobada. |
| Formula exacta de `TaskMomentum` | Decision aprobada v0, calibrable con tests. |
| Superhabit separado en magnitud visible y bonus capado | Decision aprobada. |
| Recomendacion de metas: 7 dias tiempo/cantidad, 14 dias frecuencia | Decision aprobada. |
| Sobriedad: pending 0.5, recaida asumida igual que manual, 70/30 average/worst | Decision aprobada. |
| Politica de estados y umbrales v1 | Implementada v0, calibrable con historial real. |
| `DailyClosureEntity` y algoritmo de cierre idempotente | Implementado v0 con cierre de garantia al abrir dashboard y WorkManager periodico a medianoche local. |
| `WeeklyScoreSnapshotEntity` despues del motor estable | Entidad, DAO, escritura v0 y `stabilityScore` v0 creados. |

## 2. Estado de fases

| Fase | Estado | Objetivo |
| --- | --- | --- |
| 0 | Hecha | Auditoria read-only del codigo actual. |
| 1 | Hecha v0 | Consolidar modelo de registro de hechos. |
| 2 | Hecha v0 | Ajustar entidades Room y migraciones minimas. |
| 3 | Hecha v0 | Crear input builder semanal desde hechos reales. |
| 4 | Hecha v0 + modularizada | Reemplazar `ScoreEngine` por motor nuevo de dominio. |
| 5 | Hecha v0 | Integrar score al dashboard sin redisenar UI. |
| 6 | Hecha v0 | Crear pagina de scoring detallado. |
| 7 | Hecha v0 | Agregar memoria semanal derivada y versionada. |
| 8 | Pendiente | Refinar UI explicativa por capas. |

Regla de trazabilidad:

```text
Al terminar cada fase implementada y verificada, crear un commit dedicado antes
de pasar a la siguiente fase. El commit debe dejar claro que fase se cerro,
que archivos principales cambio y que verificacion se ejecuto.
```

Regla de modularidad:

```text
Evitar archivos monoliticos. Si una fase introduce logica amplia, dividirla en
componentes pequenos con responsabilidad clara: modelos, policies, builders,
repositorio, mappers y UI state. Un archivo grande solo se acepta como paso
temporal si hay una razon tecnica concreta y queda anotado como deuda de
refactor inmediata.
```

## 2.1 Nucleo conceptual por feature

Esta seccion recoge decisiones de diseno de producto que afectan directamente
al scoring. No son detalles secundarios: son el contrato de como Vocal entiende
la base personal del usuario.

### 2.1.1 Configuracion inicial y base personal

La app debe empezar con una configuracion inicial obligatoria. El usuario no
entra al sistema de scoring sin haber definido una base minima.

Reglas:

- minimo 3 capas activas;
- al menos 1 ancla por cada capa activa minima;
- el usuario declara primero que venia haciendo;
- despues define sus objetivos nuevos;
- esa base declarada no crea logs falsos;
- la base declarada sirve como contexto con amortiguacion.

Lectura conceptual:

Vocal no debe castigar al usuario por ser honesto sobre su punto de partida.
Si declara una base previa baja y luego define objetivos ambiciosos, el sistema
debe explicar que hay distancia entre origen y objetivo sin convertir esa
distancia en una condena inicial.

Contrato de amortiguacion inicial:

- dura una semana;
- solo afecta la lectura de onboarding/primera lectura, no los hechos;
- no crea registros falsos;
- calcula el score tecnico bruto desde hechos reales;
- suaviza la lectura visible hacia `En marcha` como estado intermedio;
- desaparece al cerrar la primera semana completa;
- debe mostrar razones honestas si hay distancia entre base declarada y objetivo.

Regla operativa:

```text
rawWeeklyReport = calculo normal desde hechos reales
initialAmortizedState = max(rawState, En marcha) durante la primera semana
```

La UI puede comunicar que la base esta en construccion/amortiguacion. El dominio
debe conservar el score bruto para auditoria y para que la amortiguacion no se
convierta en una segunda verdad.

### 2.1.2 Anclas

Las anclas son practicas recurrentes que construyen base. Son el centro del
scoring por capa.

Reglas:

- se registran diariamente;
- no hay multiples sesiones en v1;
- cada dia tiene estado y valor real;
- tap simple marca hecho con el objetivo diario;
- long press permite registrar deficit o superhabit de tiempo/cantidad;
- al cierre local, ancla no marcada queda `NotDone`;
- no existe ausencia moldeable para anclas despues del cierre;
- la frecuencia semanal se calcula como dias `Done`.

Conceptualmente, Vocal premia constancia antes que acumulacion. Un dia enorme
no reemplaza el ritmo semanal. El superhabit existe, pero no debe tapar capas
caidas ni empujar al usuario a subir metas sin decision propia.

### 2.1.3 Soportes

Los soportes son mantenimiento de base. No son anclas ni tareas. Representan
acciones de cuidado minimo que conservan estructura, dignidad, cuerpo y orden.

Reglas:

- usan minima friccion;
- el usuario marca omisiones;
- al cierre local, soporte no omitido queda `Done`;
- soporte omitido queda `Omitted`;
- no existe ausencia moldeable para soportes despues del cierre;
- soportes son opt-in;
- si una capa no tiene soportes configurados, anclas sostienen el 100% de la
  base de esa capa;
- si una capa tiene soportes configurados, la base de capa usa 80% anclas y 20%
  soportes.

Conceptualmente, el soporte no premia heroicidad. Protege lo basico. La app no
debe convertirlo en lista policial, pero si debe registrar cuando ese cuidado
se empieza a caer.

### 2.1.4 Tasks / Pendientes

Las tasks son acciones puntuales. No construyen base por si solas y no deben
volverse deuda moral.

Reglas:

- task pendiente no penaliza;
- task sin capa no tiene valor de scoring;
- task con capa completada aporta `TaskMomentum`;
- una task muere como pendiente cuando se completa;
- se registra en el dia de completado;
- muchas tasks pequenas deben tener rendimiento decreciente;
- `TaskMomentum` no repara una base caida.

Decision conceptual:

Tasks no deben estar en el denominador duro del `WeeklyBaseScore`. Deben ser
una senal positiva acotada de agencia y avance para la capa asociada. Si se
metieran como obligacion, una task movible se volveria castigo encubierto, que
contradice el tono y la filosofia de Vocal.

### 2.1.5 Sueno

Sueno es un subsistema especial dentro de Cuerpo. No es una ancla comun.
Refleja recuperacion, continuidad, ventana de descanso, interrupciones e
higiene digital.

Reglas:

- el sueno semanal entra como 30% de Cuerpo;
- una sesion pertenece al dia donde empezo;
- una sesion puede cruzar dos dias;
- el modelo debe apuntar a telemetria local maxima;
- capturar inicio, fin, desbloqueos, interrupciones y confianza de fuente;
- si hay eventos atomicos, guardarlos como eventos hijos;
- si una fuente no es confiable, el score debe reflejar incertidumbre, no
  inventar calidad.

Conceptualmente, sueno debe ser inteligente y progresivo. La primera version
puede calcular con menos datos, pero el modelo debe quedar preparado para leer
calidad y continuidad sin simplificar todo a "dormiste X horas".

### 2.1.6 Sobriedad

Sobriedad es una feature sensible y opt-in. Es la unica excepcion donde la
ausencia de registro puede quedar moldeable temporalmente por el usuario.

Reglas:

- si sobriedad esta inactiva, no aparece, no pesa y no limita;
- si esta activa, entra en Conducta;
- ventana de olvido: 5 dias;
- durante la ventana, dias sin marca quedan pendientes de confirmacion;
- tras 5 dias sin respuesta, se materializa recaida asumida editable;
- si el usuario no levanta la recaida, el episodio sigue sumando dias;
- al levantar/relevar, el usuario puede aceptar el rango o corregir dias reales;
- recaida penaliza fuerte, pero no hay gates duros.

Conceptualmente, la app debe ser honesta sin humillar. Sobriedad no se trata
como task fallida. Se trata como senal sensible de proteccion, recaida,
recuperacion y nuevo inicio.

### 2.1.7 Superhabit y recomendacion de metas

El superhabit existe en frecuencia y en tiempo/cantidad.

Reglas:

- superhabit de frecuencia: mas dias `Done` que el objetivo semanal;
- superhabit de tiempo/cantidad: valor real mayor que objetivo diario;
- nunca resta;
- no compensa capas caidas;
- no obliga a subir metas;
- puede activar recomendaciones si se sostiene semanal o quincenalmente;
- el usuario decide si aumenta su meta.

Conceptualmente, Vocal no determina el progreso del usuario. Lo observa y le
devuelve una lectura para que el usuario decida.

### 2.1.8 Estado Base y reporte

`Estado Base` es la pagina principal del scoring. El dashboard solo muestra una
simplificacion del reporte.

Reglas:

- entrada desde menu lateral/navegacion principal;
- Compose no calcula scoring;
- ViewModel expone estado;
- dominio calcula inferencias;
- la pagina muestra causas, capas, sueno, sobriedad, superhabit, tendencia y
  estabilidad;
- el dashboard muestra estado resumido, score visible y una razon principal.

Conceptualmente, el score visible no debe ser un numero aislado. Debe ser una
lectura explicable de la base configurada.

### 2.1.9 Memoria semanal derivada

El scoring inteligente necesita memoria temporal, pero esa memoria no debe
contaminar la verdad primaria.

Reglas:

- hechos diarios siguen siendo la verdad primaria;
- snapshot semanal es derivado, versionado y recalculable;
- sirve para estabilidad, tendencias y explicacion historica;
- permite detectar superhabits sostenidos;
- permite que `Inquebrantable` no salga de una sola semana;
- no se debe persistir como verdad primaria.

Conceptualmente, lo inteligente no es guardar un numero y creerle para siempre.
Lo inteligente es conservar hechos, derivar memoria y poder explicar por que el
sistema leyo una semana de cierta forma.

## 3. Diagnostico del codigo actual

### 3.1 Modelos y entidades

El modelo actual ya separa:

- `ActivityDefinitionEntity`: catalogo de actividades.
- `UserActivityConfigEntity`: configuracion personal del usuario.
- `ActivityLogEntity`: hecho diario de una actividad.
- `TaskEntity`: pendiente puntual.
- `AbstinenceTrackEntity`: racha o sobriedad configurable.
- `AbstinenceLogEntity`: marca diaria de sobriedad.
- `SleepLogEntity`: registro manual de sueno por dia.
- `SleepConfigEntity`: ventana objetivo de sueno.
- `LayerEntity`: capas.

Esto esta alineado con la arquitectura general, pero no alcanza todavia para un
scoring completo sin ambiguedad.

### 3.2 Score actual

`app/src/main/java/dev/panopt/autonomia/domain/scoring/ScoreEngine.kt` calcula
un score provisional.

Problemas:

- mezcla hechos diarios y semanales;
- usa pesos incompatibles con el sistema nuevo;
- trata anclas semanales como `Goal` y las separa de la base;
- interpreta soportes con la semantica normal aunque la UX real es inversa;
- da peso/baseline a Conducta cuando sobriedad no esta activa;
- usa sueno diario, no sueno semanal;
- no tiene `baseConfiguredAt`;
- no tiene `stabilityScore`;
- no explica capas con suficiente detalle para el sistema nuevo.

Decision: reemplazarlo por un motor nuevo. No hacer adaptacion incremental del
algoritmo viejo salvo para mantener la app compilando durante la transicion.

### 3.3 Dashboard actual

No existe una clase llamada `DashboardInference`.

Equivalentes actuales:

- `DashboardEngine`
- `DashboardProjection`
- `DashboardState`
- `DashboardViewModel`

`DashboardProjection` llama directamente al `ScoreEngine` actual. En el sistema
nuevo deberia llamar a un caso de uso o recibir un reporte ya calculado.

## 4. Problema bloqueante: registro de hechos

El scoring necesita denominadores y hechos confiables.

Hoy existen algunos hechos, pero varios significados estan mezclados:

- Anclas y soportes usan la misma tabla `activity_logs`.
- En anclas, `completed = true` significa "lo hizo".
- En soportes, por UX inversa, `completed = true` significa "lo omitio".
- La tabla actual solo permite un registro por actividad por dia.
- El modelo actual no diferencia con claridad registro diario, valor real,
  superhabit y omision.
- Las tasks actuales tienen `dueDate`, pero para scoring v1 se trataran como
  plus diario solo cuando se completen y tengan capa.
- Sueno solo tiene query diaria en DAO; el scoring semanal necesita rango.
- Sobriedad tiene logs diarios, pero falta query semanal directa.
- `baseConfiguredAt` no existe como hecho/configuracion propia.
- No existe una fuente unica para cerrar el dia local y convertir lo editable
  durante el dia en hecho historico.
- No existe una rutina explicita de barrido/cierre diario que materialice todos
  los estados configurados antes de que pasen a ser historicos.

Conclusion: la primera fase real no debe ser el motor de formulas. Debe ser el
contrato de hechos que el motor va a leer.

## 5. Mapa actual de datos disponibles

| Necesidad del scoring | Dato actual | Estado |
| --- | --- | --- |
| Ancla configurada | `UserActivityConfigEntity.activityType = Anchor` | Existe. |
| Frecuencia semanal objetivo | `weeklyFrequencyTarget` | Existe. |
| Minutos por sesion | `sessionTargetMinutes` | Existe. |
| Dias reales hechos | derivable por logs diarios | Parcial. |
| Minutos reales | `ActivityLogEntity.actualValue` | Existe. |
| Soportes esperados | configs tipo `Support` | Existe. |
| Omisiones de soportes | `ActivityLogEntity.completed = true` para support | Existe, pero ambiguo. |
| Tasks relevantes | `layerId != null` y `contributionRole != Neutral` | Existe. |
| Tasks completadas por dia | `completedAt` | Existe. |
| Sueno diario | `SleepLogEntity` | Existe. |
| Sueno semanal | query por rango | Falta. |
| Sobriedad activa | `AbstinenceTrackEntity.active` | Existe. |
| Logs semanales de sobriedad | `AbstinenceLogEntity` por rango | Falta query directa. |
| Base configurada desde | derivable por configs | Falta entidad/campo canonico. |
| Historial semanal | hechos historicos | Existe parcialmente. |
| Superhabit de frecuencia | dias hechos sobre `weeklyFrequencyTarget` | Derivable. |
| Superhabit de tiempo/cantidad | `actualValue` sobre objetivo diario | Derivable. |
| `stabilityScore` | ninguno | Falta, debe ser derivado/cacheado. |

## 6. Modelo de registro objetivo

### 6.0 Principio de trazabilidad

La fuente de verdad no debe ser el score semanal. La fuente de verdad son los
hechos diarios o atomicos:

- registros diarios de anclas, soportes y tasks;
- segmentos de sueno;
- marcas diarias de sobriedad y estados pendientes propios de sobriedad;
- eventos/rangos de recaida;
- configuracion vigente del usuario;
- base declarada inicial si se usa para contexto.

El score semanal se calcula desde esos hechos. Puede existir un snapshot semanal
para rendimiento o historial visual, pero no debe reemplazar los hechos que lo
originaron.

Regla:

```text
Estado diario editable -> barrido/cierre local -> hechos historicos
Hechos historicos -> agregacion semanal -> reporte de scoring
```

Esto evita perder trazabilidad y evita tener dos fuentes de verdad compitiendo.
La excepcion de ausencia moldeable pertenece solo a sobriedad; no aplica al
registro diario de anclas, soportes ni tasks.

### 6.1 Mantener

Mantener estas entidades como base:

- `LayerEntity`
- `ActivityDefinitionEntity`
- `UserActivityConfigEntity`
- `TaskEntity`
- `AbstinenceTrackEntity`
- `AbstinenceLogEntity`
- `SleepConfigEntity`
- `SleepLogEntity`

### 6.2 Crear o ajustar para scoring

#### `BaseConfigurationEntity`

Objetivo: guardar cuando la base personal empezo a existir.

En terminos simples: esta tabla responde "desde cuando existe una base personal
configurada que ya puede ser evaluada". No guarda el score. Guarda el inicio del
contrato de base del usuario.

Campos sugeridos:

```kotlin
data class BaseConfigurationEntity(
    val id: String = "default",
    val configuredAt: Long,
    val firstFullWeekStartsAt: String,
    val minimumActiveLayers: Int,
    val scoringVersion: Int,
    val updatedAt: Long,
)
```

Uso:

- saber desde cuando existe una base minima evaluable;
- validar la regla innegociable: minimo 3 capas, al menos 1 ancla por capa;
- no depender para siempre de `createdAt` de configs individuales.
- evitar que el scoring opere antes de la configuracion inicial obligatoria.

Decision:

- la base declarada inicial se representa como contexto con amortiguacion;
- no se convierte en logs falsos;
- no se usa para castigar de golpe al usuario por la distancia entre su punto
  de partida y sus objetivos nuevos;
- puede modular el lenguaje, la lectura inicial y recomendaciones durante las
  primeras semanas mientras se acumulan hechos reales.

#### Base declarada inicial

La configuracion inicial obligatoria recoge dos capas de informacion:

1. Lo que el usuario declara que venia haciendo.
2. Lo que el usuario define como objetivo nuevo.

Esto no debe mezclarse superficialmente con la configuracion vigente.

Posible entidad:

```kotlin
data class AnchorInitialBaselineEntity(
    val activityId: String,
    val declaredWeeklyFrequency: Int,
    val declaredSessionValue: Int,
    val capturedAt: Long,
)
```

Uso:

- dar contexto inicial al sistema;
- evitar arrancar con una lectura injustamente negativa;
- permitir comparar progreso contra la base declarada sin crear logs falsos.

Decision:

- entra como contexto con amortiguacion;
- sirve para explicar el punto de partida;
- no reemplaza registros diarios reales;
- permite que el sistema distinga "base baja porque el objetivo nuevo es
  ambicioso" de "base baja porque el usuario dejo de sostener lo configurado".

#### `DailyActivityLogEntity`

Objetivo: dejar de sobrecargar `completed`.

Decision: crear tabla nueva limpia. Esta tabla sera la unica fuente de verdad
del scoring nuevo para anclas, soportes y tasks. `activity_logs` queda fuera del
sistema nuevo y se retirara o ignorara durante la migracion.

Campos conceptuales:

```kotlin
data class DailyActivityLogEntity(
    val id: String,
    val date: String,
    val timezoneId: String,
    val subjectType: String, // Anchor | Support | Task
    val subjectId: String,
    val layerId: String?,
    val status: String, // Done | NotDone | Omitted
    val actualValue: Int?,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
```

Reglas:

- Para anclas: `Done` significa que hubo practica real; `NotDone` significa
  que al cierre local no se marco como hecha.
- Para soportes: `Omitted` significa que el usuario marco que no lo hizo.
- Para tasks: `Done` significa que se completo ese dia y puede aportar plus si
  tiene capa.
- Al cierre local no queda estado conceptual de "no registrado".
- Para anclas configuradas, el barrido crea o consolida `NotDone` si el usuario
  no la marco.
- Para soportes configurados, el barrido crea o consolida `Done` si el usuario
  no marco omision.
- Para tasks: pendiente no penaliza; solo aporta cuando se completa.
- `actualValue` guarda minutos o cantidad cuando aplique.
- El registro diario es editable hasta que el dia local cierra.
- El cierre del dia usa la zona horaria del usuario.
- No debe permitirse rellenar libremente dias anteriores despues del cierre.
- La ausencia/pending moldeable no pertenece a esta tabla. Esa excepcion solo
  existe para sobriedad por sensibilidad del dominio.

Nombre recomendado:

```text
daily_activity_logs
```

Razon:

- evita que `completed = true` signifique "hecho" para anclas y "omitido" para
  soportes;
- permite expresar hechos con `status`;
- permite que anclas, soportes y tasks entren a una misma fuente diaria sin
  mezclar significados;
- deja el scoring leyendo una semantica clara;
- no impide crear mas adelante metricas diarias/readiness sobre la misma verdad
  historica.

#### `DailyClosureEntity` (propuesta Codex pendiente de validacion)

Objetivo: cerrar dias locales de forma idempotente y auditable.

Campos conceptuales:

```kotlin
data class DailyClosureEntity(
    val date: String,
    val timezoneId: String,
    val closedAt: Long,
    val source: String, // WorkManager | AppOpenCatchUp
    val closureVersion: Int,
)
```

Contrato propuesto:

- una fecha local cerrada no se vuelve a abrir para anclas, soportes ni tasks;
- el cierre es idempotente: correrlo dos veces no duplica registros;
- si la app estuvo cerrada varios dias, al abrir se cierran en orden cronologico
  todos los dias anteriores pendientes;
- se guarda `timezoneId` para no reinterpretar hechos pasados si cambia la zona
  horaria;
- el cierre programado intenta ejecutarse a medianoche local con `WorkManager`;
- el cierre al abrir la app despues de medianoche es la garantia;
- Android puede retrasar trabajo en segundo plano, por eso la garantia al abrir
  no es opcional.

Barrido propuesto por tipo:

```text
Anchor activa del dia sin log -> crear NotDone.
Anchor activa con log Done -> conservar Done y actualValue.
Support activo del dia sin omision -> crear Done.
Support con omision -> conservar Omitted.
Task pendiente -> no crear castigo.
Task completada con capa -> asegurar Done en dia de completado.
Sobriedad activa sin marca -> no usar daily_activity_logs; pasa a PendingConfirmation.
```

Restriccion:

No hay backfill libre para anclas, soportes ni tasks despues del cierre. La
excepcion sensible sigue siendo sobriedad.

#### Superhabit de anclas

El superhabit no cambia quien decide la meta. La app solo detecta una senal y
recomienda revisar objetivos.

Tipos:

```text
Superhabit de frecuencia:
dias Done de la semana > weeklyFrequencyTarget

Superhabit de tiempo/cantidad:
actualValue diario > objetivo diario
```

Uso futuro:

- si el usuario sostiene superhabit de tiempo/cantidad durante 7 dias, la app
  puede sugerir revisar el objetivo de tiempo/cantidad;
- si el usuario sostiene superhabit de frecuencia durante 14 dias, la app puede
  sugerir revisar la frecuencia objetivo;
- la decision final siempre es del usuario;
- el scoring debe adaptarse al nuevo objetivo cuando el usuario lo cambie.

Decision matematica:

- separar magnitud visible de bonus de score;
- `SurplusMagnitude` puede mostrar excedentes grandes, por ejemplo 10x el
  objetivo;
- `SurplusBonus` usa curva decreciente y cap para no incentivar metas
  artificialmente bajas;
- el superhabit nunca compensa capas caidas.

#### Tasks diarias

Decision:

- task con capa completada aporta un plus pequeno de impulso/momentum al
  dia/capa;
- task pendiente no penaliza;
- task sin capa no tiene valor de scoring;
- no hace falta planificacion semanal para tasks v1;
- una task muere como pendiente cuando se completa y queda registrada en el dia
  de completado.

Solucion recomendada:

- no meter tasks en el denominador duro de `WeeklyBaseScore`;
- calcular `TaskMomentum` como senal positiva separada, acotada y asociada a la
  capa de la task;
- no ligarlo a goals ni a soportes;
- permitir que opere como superhabit diario de la capa asociada;
- usar rendimientos decrecientes para evitar inflar score con muchas tasks
  pequenas;
- mostrarlo como evidencia de movimiento, no como obligacion incumplida.
- impedir que tasks reparen anclas caidas, soportes omitidos o peor capa baja.

Razon:

Las tasks son puntuales, movibles y no recurrentes. Si entran al denominador,
una task pendiente se vuelve castigo encubierto. Para el sistema inteligente
conviene que sean telemetria de agencia y avance, no deuda.

Formula aprobada v0:

```text
TaskMomentumRaw = 1 - exp(-completedLayerTasks / 2)
TaskMomentumBonus = 0.050 * TaskMomentumRaw
```

Uso:

- `TaskMomentumBonus` se muestra como superhabit/momentum de la capa asociada;
- satura rapido para evitar abuso;
- no compensa anclas ni peor capa;
- cuenta solo tasks completadas con capa.

#### `SleepSessionLogEntity`

Sueno cruza dias y puede tener segmentos. Por eso no debe depender solo de un
registro diario agregado.

Campos conceptuales:

```kotlin
data class SleepSessionLogEntity(
    val id: String,
    val sleepDate: String,
    val timezoneId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val plannedSleepAt: String,
    val plannedWakeAt: String,
    val unlockCount: Int?,
    val interruptionCount: Int?,
    val sourceConfidence: Double?,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)
```

Reglas:

- `sleepDate` es el dia donde empezo el sueno.
- Una sesion que empieza lunes 23:30 y termina martes 07:00 pertenece al lunes.
- La sesion empieza cuando el usuario bloquea el telefono para cumplir su meta
  de sueno o marca explicitamente que va a dormir.
- El sistema mide que sucede entre ese bloqueo/intencion de dormir y el despertar
  real.
- `unlockCount`, interrupciones y confianza de fuente se capturan con la mayor
  telemetria local posible.
- Si se pueden capturar timestamps atomicos de desbloqueos/interrupciones, se
  debe preferir una tabla hija antes que comprimirlo todo en un contador.
- `sleep_logs` actual queda como legacy o vista agregada temporal.

Casos que debe poder representar:

- el usuario bloquea a las 00:00 con meta hasta 07:00;
- desbloquea a las 03:00 y usa el telefono;
- vuelve a bloquear a las 04:00;
- duerme o permanece sin uso hasta las 08:00;
- la sesion conserva interrupciones, duracion protegida, tiempo despierto
  estimado y desviacion respecto a la ventana objetivo.

Principio:

No basta con guardar horas dormidas. El scoring de sueno debe poder distinguir
sueno continuo, sueno fragmentado, uso nocturno del telefono, reintentos de
dormir y despertar real.

Entidad hija sugerida:

```kotlin
data class SleepInterruptionEventEntity(
    val id: String,
    val sessionId: String,
    val occurredAt: Long,
    val type: String, // Unlock | ScreenOn | ManualAwake | Other
    val source: String,
    val createdAt: Long,
)
```

#### Sobriedad pendiente y recaidas

Sobriedad es la unica feature donde la ausencia de registro puede quedar
moldeable temporalmente por el usuario. Esto no se replica en anclas, soportes
ni tasks.

Sobriedad necesita distinguir:

- dia limpio confirmado;
- dia pendiente de confirmacion;
- recaida manual;
- recaida asumida por falta de tracking despues de la ventana permitida;
- episodio de recaida con rango editable por el usuario.

Campos/entidades sugeridas:

```text
AbstinenceDailyStatus:
Clean | PendingConfirmation | Relapse

RelapseEvent:
trackId
startDate
endDate
source // Manual | AssumedAfterMissingTracking
userAdjusted
note
```

Reglas:

- la ventana de olvido es de 5 dias;
- durante esa ventana, los dias sin marca quedan `PendingConfirmation`;
- dentro de la ventana, cada dia pendiente cuenta como `0.5` dia limpio para
  no castigar como recaida ni contar como limpio completo;
- si el usuario ignora la confirmacion despues de 5 dias, se materializa una
  recaida asumida editable;
- la recaida asumida penaliza igual que una manual hasta que el usuario la
  corrija;
- si el usuario no levanta esa recaida, el episodio sigue sumando dias;
- cuando el usuario hace el levante/relevo, puede indicar cuantos dias fueron
  realmente o aceptar el rango asumido para iniciar una nueva racha;
- el usuario puede corregir la duracion real del episodio en cualquier caso;
- la recaida penaliza fuerte, pero no dispara gates duros.

Impacto aprobado en scoring:

- sobriedad activa pesa 30% de Conducta;
- si hay varios tracks activos, se usa 70% promedio + 30% peor track;
- la recaida afecta organicamente Conducta y puede arrastrar el score global
  mediante peor capa y estabilidad;
- esto no es castigo moral, es indicador de que el usuario debe reforzar sus
  capas para protegerse de recaer.

#### Queries semanales

Agregar al DAO:

- `observeDailyActivityLogsBetween(startDate, endDate)`;
- `observeSleepSessionLogsBetween(startDate, endDate)`;
- `observeAbstinenceLogsBetween(startDate, endDate)`;
- `observeRelapseEventsOverlapping(startDate, endDate)`;
- opcional: queries suspend para tests/casos de uso.

#### `WeeklyScoreSnapshotEntity` derivado

No debe ser verdad primaria. Es memoria derivada versionada.

Campos sugeridos:

```kotlin
data class WeeklyScoreSnapshotEntity(
    val weekStart: String,
    val scoringVersion: Int,
    val calculatedAt: Long,
    val weeklyBaseScore: Double,
    val weeklyScore: Double,
    val stabilityScore: Double?,
    val state: String,
    val visibleScore: Int?,
)
```

Regla:

- se puede borrar y recalcular desde hechos;
- debe versionarse;
- se invalida o recalcula si cambian hechos, configuracion o version del
  algoritmo;
- no se introduce antes de tener motor estable o necesidad real de memoria
  temporal.

Por que ayuda al scoring inteligente:

- permite calcular `stabilityScore` e `Inquebrantable` con memoria temporal sin
  releer todo el historial cada vez;
- permite detectar superhabits sostenidos y recomendar subir metas sin que una
  sola semana sobrerreaccione;
- conserva explicaciones historicas versionadas: "esa semana se leyo asi con
  esta version del algoritmo";
- facilita tendencias por capa, recaidas acumuladas, recuperaciones y patrones
  de sueno;
- sirve como cache de lectura y auditoria visual, no como fuente primaria.

Riesgo controlado:

- nunca debe ser la unica fuente de verdad;
- no debe persistirse antes de que el motor tenga tests fuertes;
- el reporte actual siempre debe poder reconstruirse desde hechos diarios,
  segmentos de sueno, sobriedad y configuracion.

## 7. Reglas de scoring objetivo

Estas reglas quedan vigentes para el motor nuevo, sujetas a ajuste si el modelo
de registro obliga a una version incremental:

- Escala interna: `0.000` a `1.000`.
- Escala visible: `700` a `1000`.
- Si una capa no tiene soportes configurados: anclas pesan 100% de la base de
  capa.
- Si una capa tiene soportes configurados: anclas pesan 80% y soportes 20% de
  la base de capa.
- Tasks no entran en la base. Aportan hasta 5% como `TaskMomentum` positivo por
  capa, no como deuda ni denominador que penalice pendientes.
- Anclas: 70% frecuencia y 30% minutos.
- Cuerpo integra sueno semanal como 30% de Cuerpo.
- Conducta integra sobriedad activa como 30% de Conducta.
- Sobriedad inactiva no aparece, no pesa y no limita.
- Todas las capas activas pesan igual.
- Score semanal/base: 75% promedio de capas activas + 25% peor capa activa.
- Superavit nunca resta.
- Superavit no compensa capas caidas.
- Plenitud requiere base alta y equilibrio.
- Inquebrantable requiere memoria temporal, no una sola semana.
- No hay gates duros: una recaida, sueno bajo o capa caida se expresa mediante
  pesos, penalizaciones y razones.

### 7.0 Arbol matematico aprobado v0

#### Capa normal

```text
Si supportsConfigured == false:
LayerBaseScore = AnchorLayerScore

Si supportsConfigured == true:
LayerBaseScore =
0.800 * AnchorLayerScore
+ 0.200 * SupportLayerScore

LayerScore =
clamp(LayerBaseScore + AnchorSurplusBonus + TaskMomentumBonus, 0.000, 1.200)
```

Reglas:

- `LayerBaseScore` es la base que se usa para detectar caidas;
- `AnchorSurplusBonus` y `TaskMomentumBonus` son superhabit/momentum;
- los bonus se muestran y pueden mejorar margen, pero no reparan una capa
  estructuralmente caida.

#### Anclas

```text
FrequencyScore = clamp(doneDays / targetDays, 0.000, 1.000)
ValueScore = clamp(actualValue / targetValue, 0.000, 1.000)

AnchorBaseScore =
0.700 * FrequencyScore
+ 0.300 * ValueScore

AnchorLayerScore = promedio(AnchorBaseScore de anclas de la capa)
```

Superhabit de anclas:

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

La magnitud puede ser grande y visible. El bonus va capado por curva decreciente.

#### Soportes

```text
SupportLayerScore =
doneSupportDays / expectedSupportDays

SupportLayerScore =
1.000 - (omittedSupportDays / expectedSupportDays)
```

Si no hay soportes configurados en la capa, `SupportLayerScore` no participa y
anclas toman el 100% de la base.

#### Tasks

```text
TaskMomentumRaw = 1 - exp(-completedLayerTasks / 2)
TaskMomentumBonus = 0.050 * TaskMomentumRaw
```

Solo cuentan tasks completadas con capa. Tasks sin capa no suman. Tasks
pendientes no penalizan.

#### Cuerpo con sueno

```text
BodyScore =
0.700 * BodyBaseWithoutSleep
+ 0.300 * SleepWeeklyScore

SleepWeeklyScore =
0.400 * DurationScore
+ 0.250 * ContinuityScore
+ 0.200 * ScheduleAlignmentScore
+ 0.150 * DigitalInterruptionScore
```

#### Conducta con sobriedad

```text
Si sobrietyActive == false:
ConductScore = ConductBaseScore

Si sobrietyActive == true:
ConductScore =
0.700 * ConductBaseScore
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

#### Score semanal

```text
WeeklyBaseScore =
0.750 * AverageLayerScore
+ 0.250 * WorstLayerScore

VisibleScore =
700 + round(clamp(WeeklyBaseScore, 0.000, 1.000) * 300)
```

### 7.1 Politica de estados sin gates duros (v0 implementada, calibrable)

Los estados se calculan con score, peor capa, estabilidad y presion de
penalizaciones. No se usan bloqueos binarios.

Propuesta v1 para discutir antes de cerrar contrato:

| Estado | Criterio base |
| --- | --- |
| `Sin datos` | No hay configuracion minima o no hay hechos suficientes para lectura. |
| `Restauracion` | `WeeklyBaseScore < 0.40` o peor capa en colapso fuerte. |
| `Atencion` | `0.40 <= WeeklyBaseScore < 0.70` o peor capa bajo margen minimo. |
| `En marcha` | `0.70 <= WeeklyBaseScore < 0.85` con base operativa suficiente. |
| `Plenitud` | `WeeklyBaseScore >= 0.85`, peor capa suficientemente alta y penalizaciones bajas. |
| `Inquebrantable` | `WeeklyBaseScore >= 0.90`, `stabilityScore >= 0.90`, peor capa alta y memoria temporal suficiente. |

Parametros v1:

```text
worstLayerCollapse = 0.30
worstLayerMinimumForMotion = 0.55
worstLayerMinimumForPlenitude = 0.75
worstLayerMinimumForUnbreakable = 0.80
minimumWeeksForUnbreakable = 6
stateHysteresisMargin = 0.03
```

Notas:

- `stateHysteresisMargin` evita que el estado suba o baje por ruido minimo de
  una semana a otra;
- la histeresis no oculta razones ni score bruto;
- recaidas, sueno bajo y capas caidas afectan el estado porque bajan sus
  sub-scores, penalizaciones y peor capa, no porque exista un gate duro;
- `Plenitud` puede aparecer antes que `Inquebrantable`, pero debe tener base
  alta y equilibrio real;
- `Inquebrantable` no aparece sin historial.

## 8. Arquitectura objetivo

```text
Room
  -> Repositorios
  -> BuildScoreInputUseCase
  -> ScoreEngine nuevo
  -> Dashboard domain mapper
  -> ViewModel
  -> Compose
```

Paquete sugerido:

```text
domain/scoring/
  model/
  policy/
  engine/
  input/
```

El motor nuevo:

- no consulta Room;
- no conoce Compose;
- no escribe datos;
- recibe input puro;
- devuelve reporte puro.

## 9. Plan de implementacion por fases

### Fase 1 - Contrato de registro

Objetivo: cerrar entidades y semantica antes de formulas.

Tareas:

- crear `daily_activity_logs`;
- definir `DailyActivityStatus`;
- definir cierre de dia local y timezone;
- definir superhabit de frecuencia y tiempo/cantidad;
- definir tasks como plus diario sin penalizacion;
- definir `sleep_session_logs`;
- definir sobriedad pendiente y eventos de recaida por rango;
- definir `BaseConfigurationEntity`;
- definir queries semanales de sueno y sobriedad.

Archivos probables:

- `data/Entities.kt`
- `data/AutonomiaDao.kt`
- `data/AutonomiaDatabase.kt`
- `Models.kt`
- `AutonomiaRepository.kt`
- `data/local/mapper/DomainMappers.kt`

### Fase 2 - Migracion y repositorio

Objetivo: que Room pueda guardar y leer hechos suficientes.

Tareas:

- agregar migracion de base de datos;
- actualizar mappers;
- actualizar metodos de repositorio;
- asegurar que anclas, soportes, tasks, sueno y sobriedad registran hechos con
  semantica clara.

### Fase 3 - Input builder

Objetivo: convertir hechos locales en input de dominio.

Crear:

```text
BuildScoreInputUseCase
ScoreInput
LayerInput
AnchorWeeklyInput
SupportWeeklyInput
TaskWeeklyInput
SleepWeeklyInput
SobrietyWeeklyInput
```

Regla:

- el input builder agrupa y normaliza;
- no decide estados;
- no aplica bloqueos duros;
- no calcula score final.

### Fase 4 - Motor nuevo

Objetivo: reemplazar `ScoreEngine`.

Crear policies:

- `AnchorScoringPolicy`
- `SupportScoringPolicy`
- `TaskScoringPolicy`
- `SleepWeeklyScoringPolicy`
- `SobrietyScoringPolicy`
- `LayerScoringPolicy`
- `WeeklyBaseScoringPolicy`
- `SurplusScoringPolicy`
- `StabilityScoringPolicy`
- `BaseStatePolicy`
- `VisibleScorePolicy`

Crear:

```text
ScoreEngine
ScoreReport
LayerScoreReport
ScoreReason
ScorePenalty
```

Decision: el nombre final puede seguir siendo `ScoreEngine` si se reemplaza el
archivo actual, o puede ser `BaseScoreEngine` si ayuda a leer mejor el dominio.

### Fase 5 - Integracion dashboard

Objetivo: consumir el reporte nuevo sin redisenar UI.

Tareas:

- `DashboardProjection` usa el nuevo reporte;
- `DashboardState.status` se alimenta de `ScoreReport`;
- `DashboardLayerState` se alimenta de `LayerScoreReport`;
- mantener Compose sin formulas.

El dashboard debe mostrar una lectura breve:

- estado;
- score visible o `--`;
- frase/resumen principal;
- progreso visual;
- una o dos razones principales como maximo cuando aplique.

No debe intentar contener todo el reporte de scoring.

### Fase 6 - Pagina de scoring detallado

Objetivo: crear una superficie propia para leer el reporte completo.

Nombre visible:

```text
Estado Base
```

Esta pagina es el lugar natural para mostrar:

- score visible y estado;
- modo de lectura (`NoData`, provisional, base construida);
- score por capa;
- anclas, soportes y tasks que explican cada capa;
- sueno dentro de Cuerpo;
- sobriedad activa dentro de Conducta;
- peor capa;
- superavit;
- penalizaciones y razones activas;
- razones principales;
- historial/semanas cuando exista;
- lectura de estabilidad temporal.

Regla:

- la pagina de scoring consume `ScoreReport`;
- no calcula formulas;
- puede tener mappers visuales propios;
- debe mantener tono compasivo y no clinico.

Archivos probables:

- `domain/scoring/...`
- `domain/scorepage/...` o mapper equivalente;
- `ui/scoring/ScoringScreen.kt`;
- `MainActivity.kt` o el contenedor de navegacion local;
- menu lateral / drawer para abrir la pagina.

Decision: la pagina de scoring es pantalla principal propia, no solo una seccion
dentro del dashboard. El dashboard no es el punto de entrada principal del
reporte detallado.

### Fase 7 - Historial y estabilidad

Objetivo: calcular memoria temporal.

Tareas:

- recalcular `stabilityScore` desde semanas anteriores;
- introducir `WeeklyScoreSnapshotEntity` como memoria derivada versionada cuando
  el motor este estable;
- usar snapshots para estabilidad, tendencias, superhabits sostenidos y lectura
  historica;
- mantener hechos diarios como verdad primaria.

### Fase 8 - UI explicativa

Objetivo: mostrar causas, no solo numero.

Tareas:

- exponer razones principales;
- mostrar capas bajas;
- mostrar penalizaciones y factores que bajan el score;
- mostrar superavit sin que compense caidas;
- mantener tono compasivo.

## 10. Tests por fase

### Registro

- soporte omitido no cuenta como soporte hecho;
- soporte no omitido queda `Done` al cierre local;
- ancla no marcada queda `NotDone` al cierre local;
- no existe estado historico `No registrado` despues del cierre;
- no se permite relleno libre de dias anteriores;
- cierre diario es idempotente;
- cierre por apertura procesa dias pendientes en orden cronologico;
- solo sobriedad permite ausencia/pending moldeable por ventana de olvido;
- ancla registra tiempo/cantidad real;
- frecuencia semanal cuenta dias hechos;
- task neutral no entra al denominador;
- task con capa completada aporta `TaskMomentum`;
- task pendiente no penaliza;
- muchas tasks pequenas tienen rendimiento decreciente y no reparan una base
  caida;
- sueno por segmentos se puede consultar por rango semanal;
- interrupciones/desbloqueos de sueno se guardan como telemetria local cuando
  sea posible;
- sobriedad se puede consultar por rango semanal;
- dia de sobriedad sin marca queda pendiente de confirmacion;
- despues de 5 dias sin respuesta, sobriedad materializa recaida asumida
  editable;
- recaida asumida sigue sumando dias hasta levante/relevo del usuario;
- recaida por rango permite correccion del usuario;
- `baseConfiguredAt` existe o se crea al configurar base.

### Policies

- ancla perfecta da 1.0;
- un dia enorme en tiempo/cantidad no reemplaza frecuencia semanal;
- superavit se calcula aparte;
- capa sin soportes configurados usa anclas como 100% de base;
- capa con soportes configurados usa 80% anclas y 20% soportes;
- Cuerpo integra sueno al 30%;
- Conducta sin sobriedad no usa sobriedad;
- Conducta con sobriedad activa usa 30%;
- recaida activa penaliza fuerte sin gate duro;
- peor capa arrastra 25%;
- peor capa baja impide que el superhabit compense la caida global;
- `NoData` no devuelve score visible;
- primera semana devuelve modo provisional;
- `Unbreakable` requiere historial.

### Integracion

- ViewModel no calcula formulas;
- Compose no calcula scoring;
- dashboard mantiene `--` en `NoData`;
- dashboard muestra estado desde reporte de dominio;
- score viejo queda removido o reemplazado sin usos residuales.

## 11. Riesgos

1. Cambiar entidades antes de cerrar semantica puede crear otra migracion
   ambigua.
2. Convivir mucho tiempo con `activity_logs` puede reintroducir doble verdad.
3. La base declarada inicial puede producir una lectura injustamente negativa si
   se compara de forma superficial contra objetivos nuevos. Mitigacion: usarla
   como contexto con amortiguacion, no como logs falsos.
4. Sueno exige telemetria amplia; si los permisos o APIs locales no entregan
   desbloqueos confiables, el modelo debe guardar confianza de fuente y no
   inventar calidad.
5. `stabilityScore` o snapshots persistidos demasiado pronto pueden congelar un algoritmo
   inmaduro.
6. Reemplazar el score sin tests puede romper dashboard de forma silenciosa.
7. Si el barrido diario no queda bien definido, el sistema podria permitir
   backfill de dias anteriores y romper la filosofia de uso diario.

## 12. Primer patch recomendado despues de este documento

El primer patch de codigo deberia ser de datos, no de formulas:

1. Crear `ActivityDailyStatus`.
2. Crear `daily_activity_logs` como fuente unica para anclas, soportes y tasks.
3. Revisar y aprobar o ajustar la propuesta de `DailyClosureEntity`.
4. Agregar cierre de dia local con timezone: `WorkManager` a medianoche local
   y cierre de garantia al abrir la app despues de medianoche.
5. Agregar `sleep_session_logs`.
6. Agregar telemetria de interrupciones/desbloqueos de sueno cuando sea
   posible.
7. Agregar estados de sobriedad pendiente y eventos de recaida por rango.
8. Agregar materializacion automatica de recaida asumida tras 5 dias sin
   respuesta y levante/relevo editable.
9. Agregar `BaseConfigurationEntity` o un mecanismo equivalente.
10. Agregar `AnchorInitialBaselineEntity` como contexto con amortiguacion.
11. Agregar tests de semantica de registro.

Despues de eso recien conviene escribir el motor de scoring.

## 13. Registro de implementacion v0

Fecha: 2026-05-26

### Implementado

- `ScoreEngine` provisional reemplazado por motor semanal de dominio en
  `app/src/main/java/dev/panopt/autonomia/domain/scoring/ScoreEngine.kt`.
- Se preservo la API que consume el dashboard: `ScoreInput`, `ScoreReport`,
  `LayerScore`, `FeatureContribution` y `ScoreGate`.
- Se eliminaron gates duros del calculo nuevo. `gates` queda vacio por
  compatibilidad mientras el dashboard migra a razones/penalizaciones.
- El score semanal usa escala interna `0.000..1.000` y visible `700..1000`.
- Anclas usan 70% frecuencia y 30% valor real.
- Soportes son opt-in: sin soportes, anclas sostienen 100% de la capa; con
  soportes, anclas 80% y soportes 20%.
- Soportes leen la semantica inversa actual: `completed = true` en soporte se
  interpreta como omision.
- Tasks con capa completadas en la semana aportan `TaskMomentumBonus` con
  curva decreciente. Pendientes y neutrales no penalizan.
- Cuerpo integra sueno como 30% cuando se evalua la capa Cuerpo. La v0 usa el
  `SleepScoring` disponible, que todavia no tiene toda la telemetria 40/25/20/15.
- Conducta integra sobriedad activa como 30%. Sobriedad inactiva no pesa.
- Sobriedad v0 calcula pending dentro de ventana de 5 dias como 0.5 dia limpio
  amortiguado y penaliza recaidas por formula, sin gates.
- `WeeklyBaseScore` usa 75% promedio de capas y 25% peor capa.
- `Inquebrantable` no se emite desde una sola semana; una semana perfecta llega
  como maximo a `Plenitud` hasta que exista memoria temporal.
- Se agregaron `DailyClosureEntity` y `WeeklyScoreSnapshotEntity` a Room con
  migracion 7 -> 8.
- El repositorio tiene cierre diario idempotente para dias vencidos de la
  semana. Al abrir el dashboard, se ejecuta cierre de garantia despues de
  seedear.
- El cierre v0 materializa anclas y soportes no tocados con `completed = false`
  y `actualValue = 0`, solo desde la fecha local de creacion de cada config.
- `WeeklyScoreSnapshotEntity` queda como cache/historial derivado versionado;
  todavia no se escribe desde dominio.
- `daily_activity_logs` no se creo en v0. Decision tecnica temporal: mantener
  `activity_logs` como fuente unica operativa y no duplicar hechos hasta tener
  un input builder formal y migracion completa de dashboard.

### Tests implementados

Archivo:

```text
app/src/test/java/dev/panopt/autonomia/domain/scoring/ScoreEngineTest.kt
```

Cobertura v0:

- `NoData` no devuelve score visible;
- anclas 70/30;
- superhabit con bonus capado;
- soportes opt-in y omisiones;
- tasks con capa como momentum;
- sueño 30% en Cuerpo;
- sobriedad inactiva no pesa;
- sobriedad activa 30% en Conducta;
- pending de sobriedad dentro de 5 dias;
- peor capa arrastra 25%;
- semana perfecta no emite `Inquebrantable`.

Comando ejecutado:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: build y tests en verde.

### Pendiente despues de v0

- Usar snapshots semanales derivados para tendencia, recomendaciones de
  superhabit e historial explicable en `Estado Base`.
- Calibrar `StabilityScore` con historial real cuando existan suficientes
  semanas de uso.
- Agregar histeresis de estado cuando exista suficiente historial real.
- Expandir `Estado Base` con tendencias y recomendaciones cuando exista mas
  historial.
- Implementar telemetria avanzada de sueno: sesiones, interrupciones,
  desbloqueos y confianza de fuente.
- Materializar recaidas asumidas por sobriedad como eventos/rangos editables.
- Crear la pagina principal `Estado Base`.

## 14. Registro de modularizacion de scoring

Fecha: 2026-05-27

Objetivo:

```text
Evitar que `ScoreEngine.kt` se convierta en archivo monolitico y dejar el
scoring preparado para crecer por piezas: snapshots, estabilidad, Estado Base
y telemetria avanzada.
```

Cambios realizados:

```text
ScoreEngine.kt
  Orquestador publico. Mantiene API actual y arma ScoreReport.

ScoreModels.kt
  Contratos publicos e internos del scoring.

WeeklyScoringContextBuilder.kt
  Agrupa hechos semanales desde ScoreInput.

AnchorScoringPolicy.kt
SupportScoringPolicy.kt
TaskMomentumPolicy.kt
SobrietyScoringPolicy.kt
LayerScoringPolicy.kt
SpecialLayerScoringPolicy.kt
LayerContributionPolicy.kt
WeeklyScorePolicy.kt
VisibleScorePolicy.kt
ScoreReasonPolicy.kt
  Policies atomicas de dominio.

ScoringConstants.kt
ScoringExtensions.kt
  Constantes y helpers internos.
```

Resultado:

```text
ScoreEngine.kt quedo como orquestador pequeno.
Las formulas principales viven en policies separadas.
El comportamiento se mantuvo cubierto por los tests existentes.
```

Verificacion:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: tests en verde.

## 15. Registro de input builder semanal

Fecha: 2026-05-27

Objetivo:

```text
Sacar del dashboard la decision de que entra al scoring y dejar una entrada
formal reutilizable por dashboard, Estado Base y snapshots.
```

Cambios realizados:

```text
ScoreInputSource.kt
  Fuente cruda de hechos/configuracion para construir ScoreInput.

BuildScoreInputUseCase.kt
  Normaliza capas activas, actividades visibles de scoring y sobriedad activa.

DashboardProjection.kt
  Deja de construir ScoreInput a mano y llama al use case.

BuildScoreInputUseCaseTest.kt
  Protege el filtrado de capas, actividades y tracks activos.
```

Resultado:

```text
Dashboard ya no decide directamente la entrada del scoring.
La misma construccion de ScoreInput queda lista para Estado Base y snapshots.
```

Verificacion:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: tests en verde.

## 16. Registro de cierre diario programado

Fecha: 2026-05-27

Objetivo:

```text
Completar la mitad de infraestructura pendiente del cierre diario: intentar el
barrido en segundo plano alrededor de la medianoche local, manteniendo el cierre
de garantia al abrir la app.
```

Cambios realizados:

```text
DailyClosureSchedulePolicy.kt
  Policy puro que calcula la siguiente ejecucion local a las 00:01.

DailyClosureWorkScheduler.kt
  Agenda trabajo periodico unico con WorkManager y delay inicial hasta el
  siguiente cierre local.

DailyClosureWorker.kt
  Worker de infraestructura que seedeea, resuelve zona horaria local y llama al
  repositorio para cerrar dias vencidos.

AutonomiaRepository.kt
  `closeElapsedActivityDays` acepta `source` para diferenciar cierre por
  apertura de cierre por WorkManager.

MainActivity.kt
  Registra el scheduler al iniciar la app, antes de Compose.

DailyClosureSchedulePolicyTest.kt
  Protege calculo horario en zona local y evita delays negativos.
```

Decision tecnica:

```text
Se usa PeriodicWorkRequest diario con `ExistingPeriodicWorkPolicy.KEEP`.
WorkManager no garantiza ejecucion exacta al milisegundo, por eso el cierre de
garantia al abrir la app sigue siendo parte del contrato.
```

Verificacion:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: tests en verde.

## 17. Registro de snapshots semanales derivados

Fecha: 2026-05-27

Objetivo:

```text
Materializar la memoria semanal como cache derivado, versionado y recalculable,
sin convertir `weekly_score_snapshots` en verdad primaria.
```

Cambios realizados:

```text
WeeklyScoreSnapshotModels.kt
  Contrato puro del input/draft de snapshot y version de scoring.

BuildWeeklyScoreSnapshotUseCase.kt
  Construye el snapshot derivado desde ScoreInput + ScoreReport.

ScoreSnapshotHashPolicy.kt
  Calcula `configHash` y `factsHash` para saber cuando cambio la configuracion
  o cambiaron los hechos usados por el reporte.

ScoreSnapshotJson.kt
  Serializa resumenes de capas y razones en JSON compacto para auditoria.

WeeklyScoreSnapshotWriter.kt
  Writer de datos dedicado: lee hechos Room, arma ScoreInput, ejecuta dominio y
  persiste `WeeklyScoreSnapshotEntity`.

AutonomiaDao.kt
  Agrega queries suspend de snapshot para layers, logs, sobriedad, tasks y sueno.

DashboardViewModel.kt
DailyClosureWorker.kt
  Refrescan el snapshot actual despues del cierre de garantia/app-open o cierre
  programado por WorkManager.

BuildWeeklyScoreSnapshotUseCaseTest.kt
  Protege versionado, hashes, estado visible y serializacion basica.
```

Decision tecnica:

```text
La v0 refresca el snapshot de la semana actual. Desde la fase siguiente de esta
misma iteracion, el motor tambien puede usar 5 semanas previas versionadas para
calcular `stabilityScore`; si no hay historial suficiente, el campo queda null.
```

Verificacion:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: tests en verde.

## 18. Registro de estabilidad temporal v0

Fecha: 2026-05-27

Objetivo:

```text
Usar la memoria semanal derivada para que `Inquebrantable` requiera historial
temporal y no pueda salir desde una sola semana perfecta.
```

Cambios realizados:

```text
WeeklyScoreHistoryEntry
  Modelo de dominio para snapshots historicos usados por el motor.

StabilityScoringPolicy.kt
  Calcula estabilidad con la semana actual + 5 semanas previas versionadas.

BaseStatePolicy.kt
  Centraliza estado visible sin gates duros: score, peor capa, plenitud e
  inquebrantable por memoria temporal.

ScoreInput / ScoreInputSource / BuildScoreInputUseCase
  Aceptan historial semanal derivado.

AutonomiaRepository.kt / WeeklyScoreSnapshotWriter.kt / DashboardViewModel.kt
  Exponen y pasan historial semanal al motor. El writer tambien usa historial al
  refrescar snapshots.

ScoreEngineTest.kt
  Protege que una semana perfecta queda en `Plenitud` sin memoria y solo llega
  a `Inquebrantable` con 5 semanas previas fuertes.
```

Formula v0:

```text
StabilityScore =
0.750 * average(lastSixWeeklyBaseScores)
+ 0.250 * worst(lastSixWeeklyBaseScores)
```

Reglas:

- requiere 5 semanas previas versionadas mas la semana actual;
- ignora snapshots de otra version de scoring;
- no reemplaza hechos diarios;
- se guarda en snapshots como dato derivado;
- `Inquebrantable` requiere `WeeklyBaseScore >= 0.90`, peor capa actual
  `>= 0.80` y `StabilityScore >= 0.90`.

Verificacion:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: tests en verde.

## 19. Registro de pagina Estado Base v0

Fecha: 2026-05-27

Objetivo:

```text
Crear una pantalla principal propia para el reporte de scoring, dejando el
dashboard como resumen breve del estado del usuario.
```

Cambios realizados:

```text
DashboardScoreReportState
  Estado de reporte ya mapeado desde dominio/dashboard para que Compose no
  calcule scoring.

DashboardProjection.kt
  Convierte ScoreReport en labels, razones y detalle por capa.

ScoringScreen.kt
  Pantalla `Estado Base` con lectura semanal, razones y capas.

ScoringReportComponents.kt
  Componentes pequenos para metricas, razones, barras y cards de capa.

NavigationDrawer.kt / DashboardScreen.kt / MainActivity.kt
  Agregan entrada `Estado Base` desde el menu lateral y pantalla principal
  propia en la navegacion local.
```

Reglas preservadas:

- Compose solo renderiza estado;
- formulas y labels numericos salen del dominio/proyeccion;
- no se rediseno el dashboard;
- la pantalla usa el reporte actual y queda lista para tendencias/historial.

Verificacion:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: tests en verde.

## 20. Registro de daily_activity_logs canonico

Fecha: 2026-05-27

Objetivo:

```text
Cerrar la deuda de doble semantica de `activity_logs` y convertir el registro
diario nuevo en fuente canonica para anclas, soportes y tasks.
```

Cambios realizados:

```text
DailyActivityLogEntity
  Nueva tabla `daily_activity_logs` con status explicito:
  `Done`, `NotDone`, `Omitted`.

AutonomiaDatabase.kt
  Version 9 y migracion 8 -> 9 desde `activity_logs` legacy, usando
  `user_activity_configs` para interpretar soportes como omisiones reales.

AutonomiaDao.kt
  Las queries usadas por dashboard, input builder, snapshots y repositorio leen
  y escriben `daily_activity_logs`.

AutonomiaRepository.kt
  Anclas, soportes y tasks escriben logs diarios canonicos. El cierre local crea
  `NotDone` para anclas no marcadas y `Done` para soportes no omitidos.

DomainMappers.kt
  Traduce la semantica limpia a `ActivityLog` de compatibilidad para no romper
  UI existente: soporte `Done` no equivale a omision; soporte `Omitted` si.

DailyActivityLogMapperTest.kt
  Protege la semantica critica de anclas y soportes.
```

Decision tecnica:

```text
`activity_logs` queda como tabla legacy. No se usa como fuente nueva de scoring.
Se conserva el modelo `ActivityLog` como contrato de compatibilidad de dominio/UI
mientras las pantallas existentes terminan de migrar a nombres mas explicitos.
```

Verificacion:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat test --no-daemon
```

Resultado: tests en verde.

## 21. Preguntas abiertas antes de implementar

1. Definir permisos/API concretas para telemetria maxima de sueno en Android:
   desbloqueos, interrupciones, screen-on y nivel de confianza.
2. Validar con tests si los parametros v1 (`minimumWeeksForUnbreakable = 6`,
   `stateHysteresisMargin = 0.03`) se sienten correctamente calibrados o
   requieren ajuste.
