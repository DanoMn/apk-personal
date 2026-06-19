# AGENTS.md - Instrucciones del proyecto

## Identidad

Proyecto local: `APK-Personal`

Producto: `Autonomía sin límites` (antes `Vocal`)

Proyecto canonico en Engram: `apk-personal`

Toda memoria, observacion, prompt y resumen de sesion de este repositorio debe
guardarse bajo `apk-personal`.

No usar `digitaliza-server` para este proyecto. `digitaliza-server` pertenece a
otro repositorio y no debe mezclarse con esta app.

## Reglas de trabajo

- Lee este archivo antes de editar codigo o documentacion.
- Respuestas, documentos `.md` e interacciones en espanol.
- Codigo fuente, nombres de clases, funciones, variables y commits en ingles cuando aplique. Preferible en ingles casi siempre.
- No uses `&&` para encadenar comandos. Usa `;` o comandos separados.
- No ejecutes `docker-compose build`; este proyecto Android no lo necesita.
- No agregues atribucion de IA en commits.
- No hagas `git reset --hard`, `git checkout --` ni reverts destructivos salvo pedido explicito.
- Si necesitas compilar el proyecto en la terminal, configura primero la ruta de Java:
  - **PowerShell (Windows nativo)**: `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`
  - **WSL (solo WSL, invocando PowerShell desde Linux)**:
    ```powershell
    powershell.exe -Command "$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
    ```
    Para tests: reemplazar `assembleDebug` por `test`.
- **NUNCA ejecutes tests (`gradlew test`) para cambios triviales** (arreglos de strings, imports, ajustes de layout minimos, limpieza de seeds). Solo ejecuta tests cuando el cambio toca logica de negocio, queries Room, o el usuario lo pide explicito.

## Estado actual del producto

La direccion vigente ya no es cyberpunk, terminal ni neon. El frontend actual
debe transmitir paz, estructura, calidez y control humano.

La fuente de verdad visual esta en:

- `docs/frontend/frontend-design.md`
- `docs/frontend/prototipo/index.html`
- `docs/frontend/prototipo/dashboard.html`

La fuente de verdad de producto/dominio esta en:

- `docs/README.md`
- `docs/producto/estado-actual-mvp.md`
- `docs/producto/nucleo-dominio-autonomia.md`
- `docs/old/especificacion-actividades-sobriedad-v1.md` (archivado — revisar si sigue citándose)
- `docs/datos-room/definicion-tablas-room-v1.md`
- `docs/producto/tono-comunicacion.md`

## Nombres canonicos del frontend

Fuente: `docs/frontend/mapa_componentes_v_0_2_borrador.md` y `docs/producto/nucleo-dominio-autonomia.md`.

Usa estos nombres para TODOS los textos visibles en la UI. No uses los nombres tecnicos en la interfaz.

| Nombre UI canonico | Nombre tecnico (Room / codigo) | Antes |
|--------------------|-------------------------------|-------|
| Mis anclas | `ActivitySurface.Anchor` | Checklist principal / PrimaryChecklist |
| Soportes | `ActivitySurface.Support` | Checklist secundaria / Cuidado base |
| Pendientes | `ActivitySurface.Task` | Tasks |
| Metas | targets en `UserActivityConfigEntity` | Goals / isGoal |
| Catalogo | `ActivityDefinitionEntity` | Actividades disponibles |
| Mis actividades | `UserActivityConfigEntity` | — |
| Registro diario | `ActivityLogEntity` | (sin cambios) |
| Capas | `LayerEntity` | (sin cambios) |
| Sobriedad | `AbstinenceTrackEntity` | (sin cambios) |
| Frase ancla | `AnchorPhraseEntity` | (sin cambios) |
| Modo riesgo | `RiskEventEntity` | (sin cambios) |
| Sueno | `SleepLogEntity` | (sin cambios) |

Regla: si agregas un texto nuevo en la UI, consulta esta tabla antes de hardcodear.

## Reglas de UI globales

- **Filtros de capa en pantallas de configuracion**: los chips de filtro por capa
  (Interior, Cuerpo, Conducta, Vinculos, Proyecto) son **globales** de la pantalla.
  Deben estar visibles y persistir su estado en TODAS las sub-vistas de la pantalla
  (vista de lista, formulario de creacion, editor). No se ocultan al cambiar de vista
  dentro de la misma pantalla. El estado del filtro (`selectedLayerFilter`) se preserva
  y afecta a todas las sub-vistas.

- **Pantalla de creacion/formulario**: cuando el usuario entra a crear un item
  personalizado, los chips de filtro y botones de accion globales (como "Crear X")
  NO deben mostrarse dentro del formulario de creacion. El formulario tiene su
  propia UI con sus propios botones (Cancelar/Agregar).

## Arquitectura actual

- App Android local-first en Kotlin + Jetpack Compose.
- Persistencia local con Room.
- La base de datos guarda hechos.
- El dominio interpreta hechos y produce estado, senales y recomendaciones.
- Compose solo debe presentar estado y enviar acciones.

### Modelo de actividades (3 entidades)

- `ActivityDefinitionEntity`: catalogo inmutable (que actividad existe, como se mide).
- `UserActivityConfigEntity`: configuracion del usuario (tipo ancla/soporte/task, targets, activo/archivado).
- `ActivityLogEntity`: registro diario.

### Reglas de superficie

- **Anclas**: UX normal (usuario marca lo que SI hizo). Targets obligatorios (cadence, targetValue, targetCount, targetPeriod).
- **Soportes**: UX inversa (usuario marca lo que NO hizo). Sin targets.
- **Tasks**: una sola vez, sin recurrencia.

## Prioridades vigentes

1. Consolidar dashboard mobile real siguiendo el prototipo organico/editorial.
2. Definir actividades por capa, tipos de actividad y datos necesarios para metricas.
3. Mantener sobriedad/abstinencias como feature propia, visible desde dashboard.
4. Definir tono de comunicacion antes de escribir mensajes finales.
5. Postergar export/import hasta estabilizar el esquema local.

## Estilo visual obligatorio

- Base oscura organica, carton/beige, coral mate.
- Nada de estetica cyberpunk, neon, terminal o corporativa fria.
- Tarjetas planas, sin bordes duros ni sombras pesadas.
- Tipografia editorial: serif para titulos, sans limpia para controles.
- Iconografia:
  - Capas = sellos con peso visual.
  - UI = trazo fino, redondeado, discreto.
  - Senales importantes pueden usar relleno solido o nucleos geometricos.

## Tono obligatorio

La app habla como un adulto funcional y compasivo. No humilla, no diagnostica,
no moraliza y no castiga.

Evitar:

- "fallaste"
- "estas mal"
- "deberias"
- tono policial
- tono clinico
- tono de coach barato

Preferir:

- "La base esta baja."
- "Volvamos al cuerpo."
- "Una accion minima ahora."
- "Esto es una senal, no una condena."
- "Hoy toca estructura, no castigo."

## Protocolo de Meta-Prompting

Este protocolo se utiliza para evitar ambigüedades, ahorrar tokens y crear un historial técnico de diseño y aprendizaje mutuo.

1. **Detección de Ambigüedad:** Si el usuario envía una instrucción vaga o ambigua, la IA debe detenerse y pedir aclaraciones.
2. **Análisis de Contexto Técnico:** Antes de proponer una solución, la IA DEBE investigar los archivos del proyecto relevantes (usando `view_file`, `grep_search`, etc.) para entender la estructura actual.
3. **Registro (Solo Glosario):** Anotar dudas, términos de dominio y análisis de contexto en `meta/meta-prompting.md`. Este archivo sirve **solo como registro/diccionario**, NO como prompt de la tarea.
4. **Propuesta PRO (El Prompt Real):** Por cada nueva petición compleja, redactar la instrucción refinada en un archivo `.md` único dentro de `meta/instructions/` (ej. `YYYY-MM-DD-nombre-tarea.md`).
5. **Auto-Adopción y Ejecución:** Una vez creado el Pro-Prompt en `meta/instructions/`, la IA sustituirá la instrucción original del usuario y **tomará el contenido de ese nuevo archivo como el prompt real de la iteración**. Esto evita la contaminación de contexto (context pollution) con instrucciones previas.
6. **Contenido del Pro-Prompt:** Mantener el contexto humano relevante y añadir todas las referencias técnicas directas. Preguntar al usuario: "¿Es este Pro-Prompt lo que necesitas?" antes de ejecutar.

## Optimización de Comprobaciones (Tokens)

- **NO realizar comprobaciones automáticas con el navegador o emuladores** para cambios pequeños o triviales (ej. correcciones estéticas menores, typos, ajustes de lógica simples). Esto consume tokens de forma innecesaria.
- Solo realizar estas comprobaciones cuando el usuario lo pida **EXPLÍCITAMENTE** o cuando el cambio sea de gran magnitud y crítico para la estabilidad del sistema.

## Mejores Prácticas y Context 7

- Al enfrentarnos a la escritura de código nuevo, arquitectura o dudas sobre las mejores formas de codificar, **la IA debe apoyarse en el servidor MCP Context 7**.
- Tras cada iteración, al igual que se utiliza el "Pro-Prompting" para definir claramente qué hacer, se debe usar la información proporcionada por **Context 7** para garantizar que la implementación se realiza siguiendo los estándares más recientes y las mejores prácticas de desarrollo.

## Skills del proyecto

Skills auto-descubribles en `.claude/skills/`. Si tu tarea matchea el trigger, leé la
SKILL.md **antes** de escribir.

| Skill | Para qué | Trigger |
|-------|----------|---------|
| `scoring-historias-usuario` | Formato canónico para plantear historias/casos de decisión de scoring que el dueño marca (estado esperado, superhabit/déficit, umbrales). Evita ejemplos de juguete, casos no autocontenidos y teorizar sin calcular. | Escribir o pedir historias de usuario / escenarios / casos de scoring (`docs/scoring/historias-*`, `escenarios-estado-esperado`). Ver [SKILL.md](.claude/skills/scoring-historias-usuario/SKILL.md). |
| `scoring-dataset-decisiones` | Mantener el dataset AI-facing append-only (`docs/scoring/dataset-decisiones-estado-*.md`) que destila las marcas del dueño e infiere patrones por estado. Es el doc que el agente LEE para inferir (no las historias grandes). | El dueño marcó una tanda de historias, o hay que inferir qué variable define un estado. Ver [SKILL.md](.claude/skills/scoring-dataset-decisiones/SKILL.md). |
