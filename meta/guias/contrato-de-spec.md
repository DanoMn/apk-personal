# Contrato de Spec — qué debe contener una spec antes de lanzar un SDD

> Guía para el agente, específica de este proyecto (NO global). Define qué es una
> spec acá y qué debe contener ANTES de autorizar una ejecución SDD. La verificación
> (build, lint, app arranca, logs, tests) NO se define acá — vive en
> [`verificacion-por-capas.md`](./verificacion-por-capas.md). Este doc es solo la
> etapa de spec.

## Intención: fricción a propósito

El humano de este proyecto PREFIERE fricción en la etapa de spec. Tu rol es ser el
FILTRO ESTRICTO: exigí los detalles de diseño que faltan ANTES de tocar código. No
avances "para no molestar". Frenar y pedir una restricción ausente es barato;
debuggear el bug que esa ausencia provoca tras una ejecución larga es caro. Tu sesgo
por default es FRENAR (ver sección 9), no arrancar.

## 0. Cuándo aplica y qué secciones entran (alcance)

**Primero: ¿necesita spec?** Una sola pregunta: *¿el cambio altera comportamiento
observable, el modelo de datos, o una regla de negocio?*

- **NO → trivial, sin spec.** Texto/labels, colores, padding/layout, typos,
  renombrar variables, mover archivos, refactor sin cambio de comportamiento. Se hace
  directo (rige la regla de cambios triviales del CLAUDE.md). La compuerta NO aplica.
- **SÍ → necesita spec.** Pero **no toda spec usa las 8 secciones.**

**Qué secciones entran — por relevancia, NO por "tamaño".** Una spec incluye una
sección cuando esa sección tiene algo que decir sobre ESE cambio:

- **Núcleo (siempre, en toda spec):** Propósito (§2) y Criterios de aceptación (§7).
  El "para qué" y el "cómo sé que está hecho". Sin esos dos, no hay spec.
- **Situacionales (entran si el cambio las toca):**
  - Entradas/salidas (§3) → si el cambio tiene una frontera de datos (recibe/entrega).
  - Camino feliz (§4) → si agrega o cambia reglas (casi siempre).
  - Restricciones (§5) → si hay invariantes en juego (local-first, seed, migraciones…).
  - Casos límite (§6) → si hay condiciones de error o estados adversos posibles.

La compuerta (§9) evalúa SOLO las secciones que entraron. Ejemplos —ilustran el
principio, no son casilleros rígidos—: un botón que dispara una regla nueva → núcleo
+ §4; un campo nuevo en una entidad con su migración → núcleo + §3 + §5; un subsistema
de telemetría → casi todas. **Ante la duda, la sección entra.** Mejor de más que de
menos.

## 1. Qué es una spec y por qué existe

Una spec describe el **QUÉ** debe hacer un componente, separado del **CÓMO**. La spec
es la fuente de verdad del comportamiento; el código es solo una de las
implementaciones posibles que la satisfacen, no la verdad.

Tres consecuencias que rigen tu comportamiento:

1. **La spec se aprueba ANTES de tocar código.** No se deriva del código; el código se
   deriva de ella. Si te piden implementar sin spec suficiente, tu primer trabajo es
   completar la spec, no programar.
2. **Los tests se derivan de la spec, no de la implementación.** Un test debe poder
   escribirse mirando solo la spec. Esto rompe la circularidad: si escribís el código
   con un malentendido y después el test desde el mismo malentendido, el test pasa
   sobre un bug. (Mismo motivo por el que la verificación corre sobre un Android real,
   no sobre lo que el modelo cree.)
3. **Lo que no está en la spec no existe.** No inventes defaults, restricciones ni
   reglas de negocio que la spec no declare. Si falta algo importante, no lo asumas:
   frená y pedilo. La mayoría de los bugs de diseño no son código mal escrito — son
   restricciones que nunca se escribieron.

## 2. Propósito del componente (núcleo)

Una sola frase, en lenguaje de dominio, que responde "¿para qué existe esto?" y
delimita su responsabilidad (y por implicación, qué NO le toca).

- Adecuado: "Materializa los días transcurridos sin registro en hechos diarios, para
  que el scoring semanal opere sobre hechos completos."
- Inadecuado: "Worker con Room y una corrutina que recorre fechas." (Eso es CÓMO.)

Señal de spec insuficiente: si el propósito no permite decidir si una funcionalidad
*pertenece* a este componente, está mal delimitado.

## 3. Entradas y salidas (situacional)

La frontera del componente: qué recibe y qué entrega, sin describir la mecánica
interna. Para cada entrada/salida, declarar: nombre y significado de dominio,
forma/estructura, origen o destino, y obligatoriedad (qué pasa si falta).

No hace falta tipo de un lenguaje concreto; sí que cada campo sea inequívoco. "Un
día" es ambiguo; "`LocalDate` en zona local del dispositivo, día calendario del
cierre" no lo es.

Señal de spec insuficiente: si para implementar tendrías que adivinar el formato de un
campo o de dónde sale un dato, falta definición de entrada/salida.

## 4. Comportamiento esperado — camino feliz (situacional)

Lista de reglas con forma **"cuando ocurre X, el sistema hace Y"**, cada una observable
(confirmable mirando una salida, un efecto o un estado, no la intención del código).

- Adecuado: "Cuando un día transcurre sin registro de un ancla de cadencia diaria, al
  cierre se materializa un hecho `NotDone` para ese día."
- Inadecuado: "El sistema gestiona los días eficientemente." (No observable.)

Señal de spec insuficiente: si una regla no se traduce a "dado este input/estado,
debería verse este resultado", no sirve como criterio.

## 5. Restricciones y reglas de negocio (situacional — la más omitida)

Los límites NO negociables: lo que el componente tiene prohibido hacer o debe respetar
siempre. Para ESTE proyecto, considerá al menos cuando apliquen:

- **Arquitectura local-first:** el dominio interpreta hechos; el scoring y las reglas
  de negocio NO se calculan en Compose ni en el ViewModel.
- **Seed canónico:** el seed de actividades/soportes (`DefaultSeeds.kt`) y los
  catálogos NO se vacían ni se borran — son data predeterminada (AGENTS.md #21).
- **Esquema Room:** toda alteración de entidad exige migración numerada + cobertura
  con `MigrationTestHelper`; índices `index_<tabla>_<col>`.
- **Nombres canónicos de UI:** los textos visibles usan los nombres canónicos (Mis
  anclas, Soportes, Pendientes, Metas…), no los técnicos.
- **Tono:** los mensajes respetan el tono (no "fallaste", no diagnóstico, no castigo).
- **Reglas de superficie:** anclas (UX normal, targets obligatorios), soportes (UX
  inversa, sin targets), tasks (una vez, sin recurrencia).

Señal de spec insuficiente: una sección de restricciones vacía casi siempre significa
que no se pensaron, no que no existan. Frená y preguntá.

## 6. Casos límite y de error (situacional)

Qué hace el sistema cuando NO todo va bien. Estos casos se vuelven **escenarios del
spec**: vos (agente) los proponés, el humano los aprueba (igual que en
`verificacion-por-capas.md`). Cubrir al menos las categorías relevantes:

- permiso denegado (acceso de uso / device-admin del sueño no concedido),
- estado vacío o inesperado (día sin registros, DB recién wipeada y re-sembrada),
- dato inválido o duplicado (evento de telemetría repetido, fecha fuera de rango),
- comportamiento por nivel de API (ej. dispositivos `minSdk 26` vs APIs nuevas),
- fallo parcial (drenaje de telemetría a medias, cómo se reintenta).

Cada caso con forma "cuando ocurre [condición adversa], el sistema [respuesta]".

Señal de spec insuficiente: si solo está el camino feliz, está incompleta. Preguntá
como mínimo por permiso denegado, estado vacío y dato inválido.

## 7. Criterios de aceptación (núcleo)

La definición de "terminado", objetiva y comprobable. Cada criterio debe mapear casi
directo a:

- un **test** (condición lógica, afirmable con assert), o
- un **oráculo de runtime** de `verificacion-por-capas.md` (la app arranca sin
  crashear, sin errores en logcat, Lint sin Error, build verde).

NO redescribas las capas de verificación acá — referenciá esa guía. Esta sección solo
exige que la spec DIGA cómo se sabrá que está hecho.

- Adecuado: "El cierre materializa exactamente un hecho por día transcurrido (test).
  Tras el cambio de esquema, install limpio y la app arranca sin crashear (runtime).
  `gradlew test` verde para las nuevas reglas."
- Inadecuado: "El cierre diario funciona bien." (No comprobable.)

## 8. Relación con la verificación

La spec gobierna todo el ciclo: los criterios de aceptación (§7) son la fuente de los
tests y oráculos que corre la verificación. El "terminado" de una ejecución NO es
"escribí el código" — son las capas de `verificacion-por-capas.md` en verde + los
criterios cumplidos. Lo que no se puede verificar por oráculo (UX, si el producto
resuelve el problema real) queda para juicio humano; no intentes automatizarlo.

## 9. Compuerta: frená antes del SDD (obligatoria)

Antes de autorizar/lanzar un SDD, evaluá la spec contra este checklist —**solo las
secciones que entraron según la sección 0**—. Si **alguna** respuesta aplicable es
"no", **frená y pedile al humano que complete la spec**: indicá qué sección falta y por
qué la necesitás. Tu sesgo es frenar.

- [ ] **(núcleo)** ¿Hay una frase de propósito que delimita la responsabilidad?
- [ ] **(núcleo)** ¿Cada criterio de aceptación mapea a un test o a una señal de runtime?
- [ ] (si aplica) ¿Entradas y salidas definidas sin ambigüedad de forma ni de origen?
- [ ] (si aplica) ¿El comportamiento está en reglas observables "cuando X → Y"?
- [ ] (si aplica) ¿Hay restricciones declaradas (local-first, seed, migraciones,
      nombres canónicos, tono, reglas de superficie)?
- [ ] (si aplica) ¿Cubiertos los casos clave: permiso denegado, estado vacío, dato
      inválido, nivel de API?

Frenar acá no es obstruir: es el filtro que evita errores de diseño caros. El humano
PREFIERE esta fricción.
