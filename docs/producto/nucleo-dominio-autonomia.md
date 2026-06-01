# Nucleo de dominio - Autonomia sin limites

Estado: puente de nucleo en desarrollo

Este documento define el centro operativo de Vocal para seguir construyendo
features de backend local, dominio, scoring y UI sin mezclar conceptos.

No es un documento maestro de todo el producto. Es la brujula minima para que
el codigo respete el dominio.

Fuentes relacionadas:

- `docs/producto/vocal-01-filosofia-producto.md`
- `docs/frontend/vocal_mapa_componentes_v_0_2_borrador.md`
- `docs/dominio/decisiones-capas-actividades-v1.md`
- `docs/datos-room/definicion-tablas-room-v1.md`
- `docs/frontend/prototipo/score-states.html`
- `docs/producto/tono-comunicacion.md`

## Idea central

Vocal no mide felicidad abstracta, valor personal, productividad ni salud
mental clinica.

Vocal mide una lectura practica:

```text
Que tanto esta sosteniendo el usuario su base personal configurada.
```

La app existe para detectar cuando la base se esta desarmando antes de que el
usuario vuelva al bucle.

Frase de dominio:

> Si la base cae, el bucle vuelve. La app existe para ver la base,
> reconstruirla y cortar el circuito antes de caer.

Estar bajo no significa fallar. Significa que hay una senal y toca volver a la
base.

## Nucleo operativo

El nucleo operativo de Vocal es:

```text
Cinco capas
+ Mis anclas
+ sueno
+ score de base
+ estado de base
+ tono de comunicacion
+ configuracion local personal
```

### Cinco capas

Las capas son el modelo conceptual estable.

Responden:

```text
Que parte de mi vida se esta sosteniendo o cayendo?
```

Capas canonicas:

1. Interior
2. Cuerpo
3. Conducta
4. Vinculos
5. Proyecto

Las capas no son features, pantallas ni formulas. Son dimensiones donde el
dominio agrupa hechos para producir una lectura.

### Mis anclas

Nombre UI recomendado:

```text
Mis anclas
```

Definicion:

```text
Practicas recurrentes que el usuario elige porque sostienen su base personal.
```

Son pocas, visibles y centrales.

Conceptualmente son `Activity` configuradas por el usuario como base principal.
No son el registro diario. El registro diario vive en `ActivityLog`.

Nota sobre el codigo actual:

```text
El modelo actual se llama TrackedActivity, pero ese nombre es transitorio y
puede confundir porque suena a actividad ya registrada.
```

En el dominio futuro, el nombre deberia migrar a algo mas claro como
`ActivityDefinition`, `ConfiguredActivity` o simplemente `Activity`, segun la
separacion final del codigo.

Regla actual de superficie:

```text
Mis anclas = actividades configuradas con ActivitySurface.Anchor.
```

Reglas:

- deben ser elegidas o aceptadas por el usuario;
- deben ser pocas;
- alimentan las capas;
- pesan mas que Soportes y Pendientes;
- no deben convertirse en una lista infinita;
- configuracion obligatoria para consistencia semanal:
  - Frecuencia semanal (`weeklyFrequencyTarget`, valor de 2 a 7).
  - Tiempo objetivo por sesion (`sessionTargetMinutes`, maximo de 15 horas/900 minutos).
  - Duracion del compromiso (`commitmentDurationMonths`): puede ser `null` solo cuando el usuario elige **Indefinido**. Ese `null` no significa que falte configuracion.
- UX normal: el usuario marca lo que SI hizo.

Sin anclas no hay suficiente base diaria para leer estabilidad.

### Soportes

Nombre UI recomendado:

```text
Soportes
```

Definicion:

```text
Acciones de mantenimiento diario que sostienen dignidad y estructura.
```

Conceptualmente son actividades configuradas con `ActivitySurface.Support`.

UX inversa:

```text
El usuario marca lo que NO hizo. El sistema asume que todo esta hecho
por defecto y solo registra omisiones.
```

Ejemplos:

- banarse;
- cepillarse los dientes;
- tomar agua;
- comer algo decente;
- cambiarse de ropa;
- orden minimo.

Reglas:

- sin targets;
- complementan las anclas, no compiten con ellas;
- especialmente utiles para usuarios en restauracion o saliendo de abandono;
- no son obligatorias para el sistema.

### Sueno

Sueno es core.

No es solo una actividad dentro de Cuerpo. Es base fisiologica y conductual del
sistema.

En esta etapa se registra manualmente mientras se investiga una fuente mejor.

Modelo actual:

```text
SleepLog
- date
- plannedSleepAt
- plannedWakeAt
- sleptAt
- wokeAt
- quality
- note
- updatedAt
```

Regla de dominio:

```text
Si el sueno falta o esta bajo, la base no esta completa para estados altos.
```

Regla de configuracion:

```text
La ventana objetivo de sueno no puede ser menor a 5 horas.
```

El usuario puede configurar su hora objetivo de dormir y despertar. La app debe
aceptar objetivos desde 5 horas en adelante.

El score de sueno no debe castigar a alguien por elegir 5, 6, 7 u 8 horas. Debe
leer si la persona cumplio razonablemente la ventana que ella misma configuro,
junto con calidad subjetiva y consistencia.

Distincion:

```text
Minimo configurable: 5 horas.
Objetivo personal: ventana elegida por el usuario desde 5h en adelante.
Score de sueno: cumplimiento del objetivo personal + calidad + consistencia.
```

Esto no debe comunicarse como castigo. Debe comunicarse como cuidado:

```text
El descanso esta bajo. Volvamos al cuerpo.
```

Telemetria, wearables, uso/desuso del telefono o bloqueo nocturno quedan como
investigacion futura. No entran como requisito de esta etapa.

### Score y estado de base

El score es una inferencia del dominio, no un hecho guardado.

Nombre recomendado:

```text
Score de base
```

Estado visible recomendado:

```text
Estado de base
```

El score no mide:

- felicidad;
- valor personal;
- moral;
- productividad pura;
- diagnostico clinico.

Mide:

```text
Que tanto esta sosteniendo el usuario la base que configuro.
```

### Tono de comunicacion

La comunicacion es parte del dominio.

Vocal habla como un adulto funcional y compasivo. Nombra hechos sin convertirlos
en identidad.

Evitar:

- "fallaste";
- "estas mal";
- "deberias";
- tono policial;
- tono clinico;
- tono de coach barato.

Preferir:

- "La base esta baja."
- "Volvamos al cuerpo."
- "Una accion minima ahora."
- "Esto es una senal, no una condena."
- "Hoy toca estructura, no castigo."

## Backend local dentro de la version actual

Cuando este proyecto habla de backend en esta etapa, se refiere al backend
local de la app:

- Room;
- DAOs;
- repositorios;
- seeds;
- modelos de dominio;
- motor de scoring;
- inferencias;
- flujos de datos observables;
- ViewModels que exponen estado a Compose.

Esto si esta dentro del trabajo actual y es justamente lo que este documento
ayuda a delimitar.

Fuera de la etapa inmediata:

- servidor remoto como fuente de datos personales;
- auth obligatoria;
- cuentas obligatorias;
- multiusuario;
- nube para guardar logs sensibles;
- API externa;
- sincronizacion cloud de datos sensibles;
- analytics remotos;
- telemetria automatica sensible.

La app sigue siendo local-first.

## Perfil local y configuracion personal

Para esta etapa no hay cuentas ni login.

Existe un perfil local unico implicito:

```text
La configuracion activa del usuario en este dispositivo.
```

Ese perfil esta formado por:

- anclas activas;
- activities activas o archivadas;
- metas de anclas semanales;
- abstinencias activas;
- preferencias locales;
- registros diarios;
- configuracion de sueno.

El score se calcula contra esa base configurada, no contra una plantilla
universal.

Regla clave:

```text
Lo que el usuario no activo no aparece, no pesa y no limita el estado.
```

## Identidad opcional futura

La autenticacion futura esta permitida, pero no es obligatoria.

Proveedores posibles:

- Google;
- Auth0;
- Credential Manager;
- otro proveedor equivalente.

Regla central:

```text
Cuenta remota no significa datos en nube.
```

Una cuenta remota puede servir para:

- identidad;
- licencia;
- recuperacion futura no sensible;
- integraciones no sensibles.

No debe servir para:

- almacenar logs personales;
- leer sueno, recaidas, uso digital o abstinencias;
- calcular score en servidor;
- reemplazar el perfil local;
- obligar al usuario a iniciar sesion para usar Vocal.

Tipos conceptuales:

- `LocalProfile`: configuracion y datos del usuario en el dispositivo.
- `RemoteIdentity`: identidad externa opcional, sin acceso a logs sensibles.
- `DataOwnershipPolicy`: regla que impide que un backend remoto sea dueno de
  los datos personales.

El perfil local sigue siendo la fuente de verdad del dominio.

## Portabilidad y export/import

Como los datos sensibles viven en el dispositivo, export/import es la forma
prevista de mover datos entre dispositivos.

Reglas:

- export cifrado por defecto;
- import valida version, integridad y compatibilidad de esquema;
- el usuario controla el archivo exportado;
- export/import no afecta score ni dashboard si no se usa;
- la app no puede prometer recuperacion desde servidor si el usuario pierde el
  archivo exportado.

Tipos conceptuales:

- `ExportPackage`: paquete portable cifrado.
- `ExportManifest`: version, fecha, esquema, checksum y metadatos no sensibles.
- `ImportResult`: exito, incompatibilidad, error de contrasena, corrupcion o
  version no soportada.

## Configuracion inicial esperada

El flujo de entrada debe permitir dos caminos:

```text
1. Configurar la base personal.
2. Aceptar una configuracion predeterminada y empezar.
```

Flujo esperado:

1. Explicar brevemente que Vocal organiza la base diaria en capas.
2. Mostrar las 5 capas.
3. Pedir al usuario elegir al menos 3 activities para `Mis anclas`.
4. Permitir filtrar o elegir activities por capa.
5. Ofrecer un boton para aceptar una configuracion predeterminada.
6. Configurar para cada ancla su meta semanal, duracion del compromiso y tiempo por sesion.
7. Preguntar si quiere activar abstinencias como alcohol, conducta sexual,
   marihuana u otra personalizada.
8. Entrar al dashboard funcional.

La configuracion inicial debe ser breve. No debe sentirse como una entrevista
clinica ni como una pantalla de productividad pesada.

## Activities, logs y tasks

### ActivityDefinitionEntity

Define que actividad existe y como se mide. Es el catalogo inmutable.

Campos conceptuales importantes:

- capa;
- tipo de medicion (Time, Count, Check, Note);
- unidad (minutos, cantidad, booleano, texto);
- rol (Practice, SelfCare, Learning, etc.);
- aporte a estabilidad (Core, Support, Protective);
- importancia (Critical, High, Medium, Low).

### UserActivityConfigEntity

Configuracion personal que el usuario asigna a una actividad del catalogo.

Campos clave:

- `activitySurface`: `Anchor`, `Support` o `Task`.
- `isActive`: activo o archivado.
- targets (`weeklyFrequencyTarget`, `sessionTargetMinutes`, `commitmentDurationMonths`):
  - Para `Anchor` (Mis anclas): `weeklyFrequencyTarget` = 2-7, `sessionTargetMinutes` = 1-900, `commitmentDurationMonths` = meses de compromiso o `null` para **Indefinido**.
  - Mientras el scoring siga leyendo campos legacy, se mantienen espejos: `cadence = "Weekly"`, `targetPeriod = "Week"`, `targetCount = weeklyFrequencyTarget`, `targetValue = sessionTargetMinutes`.
  - Para `Support` y `Task`: ausentes (nulos).
- `isFocusSignal`: senal destacada en dashboard.

Reglas de superficie:

- `Anchor` (Mis anclas): UX normal, usuario marca lo que SI hizo. Targets obligatorios.
- `Support` (Soportes): UX inversa, usuario marca lo que NO hizo. Sin targets.
- `Task` (Pendientes): una sola vez, sin recurrencia.

### ActivityLog

`ActivityLog` guarda hechos de una actividad.

Ejemplos:

- medite 5 minutos;
- hice ejercicio;
- avance proyecto 40 minutos;
- me cepille los dientes;
- cocine en casa.

La version actual acepta un registro por activity por dia.

### Task

`Task` no es habito.

Es un pendiente puntual.

Puede contribuir a estabilidad solo si:

```text
layerId != null
contributionRole != Neutral
```

Una task neutral no debe sumar al score.

Ejemplos neutrales:

- comprar cuerdas;
- buscar una referencia;
- ordenar un archivo.

Ejemplos que si pueden sostener estabilidad:

- pagar alquiler;
- pedir cita medica;
- resolver un tramite urgente;
- llamar a alguien para reparar una conversacion.

## Sobriedad y abstinencias

La sobriedad no es una activity comun.

Es una feature propia porque necesita:

- racha;
- marca diaria;
- historial;
- impulso;
- recaida;
- lectura protectora propia.

Modelos canonicos:

```text
AbstinenceTrack
AbstinenceLog
```

Estados diarios:

```text
Unknown
Clean
Relapse
```

Reglas:

- abstinencias son opt-in desde el producto;
- si una abstinencia no esta activa, no aparece, no pesa y no limita estado;
- si esta activa, alimenta principalmente Conducta;
- una recaida no es una tarea fallida;
- una recaida es una senal critica de proteccion y recuperacion;
- registrar impulso sin actuar debe poder leerse como senal protectora.

Nota tecnica:

```text
El seed actual activa Alcohol y Conducta sexual. Eso debe corregirse despues
para alinear codigo y documentacion con el canon opt-in.
```

La UI debe evitar etiquetas crudas cuando generen verguenza. `Conducta sexual`
es el nombre visible preferido; internamente puede representar
pornografia/masturbacion si el usuario activa esa racha.

## Score v1

El score v1 se calcula desde las 5 capas.

Las features no son capas paralelas. Las features alimentan capas o generan
senales especiales.

Mapa:

```text
ActivityLog
SleepLog
AbstinenceLog
Task
RiskEvent
        ->
Capas
        ->
Score de base
        ->
Estado de base
```

Reglas de score v1:

- las capas producen una base visible de 700 a 900;
- metas de anclas semanales pueden aportar bonus de 0 a 100;
- `Plenitude` e `Unbreakable` requieren base sostenida y metas consistentes;
- Anclas pesan mas que Soportes;
- tasks pesan menos y solo cuentan si aportan a una capa;
- sueno bajo o ausente impide lecturas altas;
- recaidas en abstinencias activas pueden limitar fuertemente el estado;
- no registrar una abstinencia no es recaida automatica;
- un dia perfecto no debe inflar todo;
- un mal dia no debe destruir todo.

El score visible nunca debe mostrar numeros humillantes por debajo de 700.

`Sin datos` no tiene numero real, pero la tarjeta de estado/score sigue visible
y muestra `--` / `sin score`.

## Estados canonicos

La fuente canonica visual es `docs/frontend/prototipo/score-states.html`.

| Enum tecnico | UI canonica | Rango visible | Lectura |
| --- | --- | ---: | --- |
| `NoData` | Sin datos | - | No mostrar score todavia. |
| `Restoration` | Restauración | 700-749 | Base baja. |
| `Attention` | Atención | 750-799 | Hay margen. |
| `Motion` | En marcha | 800-899 | Base activa. |
| `Plenitude` | Plenitud | 900-949 | Base sostenida. |
| `Unbreakable` | Inquebrantable | 950-1000 | Núcleo sólido. |

Regla:

```text
En marcha es el hogar operativo de la app.
```

`Plenitude` e `Unbreakable` son picos organicos de consistencia. No deben
sentirse como obligacion diaria.

`Riesgo`, `Crisis` o `Recaida` no compiten como estados del enum. Son eventos,
senales u overrides que pueden empujar el estado hacia `Restoration` o
`Attention`.

## Hechos vs inferencias

Room guarda hechos.

Ejemplos:

- se registro una activity;
- se registraron minutos reales;
- se marco una abstinencia como limpia;
- se registro una recaida;
- se guardo sueno;
- se completo una task;
- se abrio modo riesgo;
- se mostro una frase ancla.

El dominio calcula inferencias.

Ejemplos:

- capa Cuerpo baja;
- Conducta protegida;
- sueno incompleto;
- estado de base `Attention`;
- recomendacion: volver al cuerpo;
- score visible;
- senales del dashboard.

Regla arquitectonica:

```text
Room guarda hechos.
El dominio interpreta hechos.
Compose presenta estado y envia acciones.
```

## Arquitectura recomendada

La arquitectura recomendada se define con mas detalle en:

```text
docs/dominio/arquitectura-recomendada-autonomia.md
```

Decision corta:

```text
Arquitectura local-first con dominio modular.
```

Esto significa usar MVVM para presentacion, dominio modular para reglas,
repositorios locales para datos, Room como fuente de hechos y flujo
unidireccional de estado.

Estructura conceptual minima:

```text
domain/
  modelos puros
  motor de scoring
  reglas de estado
  recomendaciones
  politicas de comunicacion

data/
  Room
  DAOs
  repositorios concretos
  seeds

ui/
  ViewModels
  StateFlow de pantalla
  Compose UI
```

Limites:

- Room no calcula score.
- El repositorio centraliza acceso a datos y expone flujos.
- El dominio interpreta los hechos.
- ViewModel compone estado de pantalla.
- Compose no inventa reglas de negocio.

Esta direccion toma los principios utiles de Clean Architecture y arquitectura
hexagonal, pero sin copiar su ceremonia completa. El objetivo no es llenar el
proyecto de interfaces, sino impedir que scoring, dashboard, Room y Compose se
mezclen.

## Dashboard

El dashboard no debe contener todo el sistema ni redefinir el dominio.

Su rol es presentar el estado calculado y las superficies principales. El
contrato concreto vive en el dashboard actual de Compose y en
`docs/frontend/prototipo/dashboard.html`.

El dashboard actual debe mantenerse como referencia:

- tarjeta de estado/score siempre visible;
- en `NoData`, la tarjeta muestra `--` / `sin score`, no se oculta;
- progreso diario;
- frase ancla;
- botones de accion, incluyendo acceso a Mis anclas/checklist;
- capas de hoy;
- senales importantes;
- abstinencias activas si el usuario las activo;
- preview de Mis anclas;
- Soportes y Pendientes;
- resumen semanal.

Senales importantes:

```text
1. Sueno
2. Proyecto
3. Foco configurable del usuario
```

Sueno no es una tarjeta separada del contrato principal del dashboard. Vive
dentro de senales importantes y desde ahi puede abrir su panel de registro.

Sobriedad/abstinencias es configurable. Si no hay abstinencias activas, esa
superficie no debe pesar ni forzar lectura visual.

El dashboard consume estado ya calculado. No calcula scoring directamente.

## Recomendaciones

El motor de recomendaciones debe sugerir acciones coherentes con la senal baja.

Ejemplos:

- Cuerpo bajo: agua, ducha, comida simple, caminar.
- Interior bajo: meditar 1 minuto, escribir una linea honesta.
- Conducta en riesgo: alejar detonante, registrar impulso, abrir modo riesgo.
- Proyecto sin base: avanzar poco sin sacrificar cuerpo ni sueno.
- Sueno bajo: preparar cama, bajar estimulacion, registrar descanso.

Regla:

```text
Una recomendacion valida no es motivacion generica.
Es una accion concreta para volver a la base.
```

## Pruebas de lectura

El documento debe sostener estos escenarios:

- usuario nuevo elige 3 anclas y entra al dashboard;
- usuario omite configuracion y acepta presets;
- usuario no activa abstinencias: no aparecen, no pesan y no limitan estado;
- usuario activa alcohol y registra recaida: afecta Conducta y estado sin
  lenguaje de verguenza;
- sueno no registrado o muy bajo: la base se considera incompleta para estados
  altos;
- sin registros suficientes: `Sin datos`, tarjeta de score visible con `--`;
- metas de anclas semanales ayudan a `Plenitude`/`Unbreakable`, pero no
  reemplazan la base diaria;
- tasks neutrales no suman al score.
- usuario usa Vocal sin iniciar sesion;
- usuario inicia sesion con Google/Auth0 y sus logs siguen solo en el
  dispositivo;
- usuario migra a otro telefono con export/import cifrado;
- usuario pierde el archivo exportado y la app no puede recuperar datos
  sensibles desde servidor;
- scoring funciona igual con o sin autenticacion;
- identidad remota no reemplaza perfil local.

## Deudas conocidas

- Alinear seed de abstinencias con el canon opt-in.
- Crear o pulir onboarding/configuracion inicial.
- Alinear labels del codigo con tildes visibles del HTML canonico cuando toque
  pulir UI.
- Seguir refinando pesos y umbrales con datos reales.
- Investigar sueno automatico, uso del telefono y telemetria solo cuando el
  nucleo local sea estable.
- Export/import queda postergado hasta estabilizar el esquema local, pero es
  feature futura necesaria por la decision de datos sensibles locales.
