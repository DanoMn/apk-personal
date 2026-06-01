# Verificación por capas — metodología de la fase de testing

> **Fuente única de verdad de la fase de testing de este proyecto.**
> CLAUDE.md declara este documento como contrato vinculante: antes de iniciar
> cualquier fase SDD que toque código, la IA DEBE cargar esta guía y cumplir sus
> gates. Ninguna capa es opcional.

## Por qué existe esto

Trabajamos con SDD + Strict TDD, donde el **mismo modelo de IA** escribe el spec,
el test, el código y corre el Verify. Es el alumno corrigiendo su propio examen. Una
sola verificación es un circuito cerrado que se cree a sí mismo.

La defensa son **capas independientes** —sobre todo las que ejecutan de verdad sobre
un Android real—. Una capa puede mentir; varias capas que se cruzan, no. Un bug que
pasa los tests en verde y arranca sin crashear en el emulador igual puede explotarle
a un usuario: por eso cada capa cubre lo que la otra no ve.

## Las capas (gates obligatorios, en orden)

El orden va de **barato a caro**. No saltees una capa porque "seguro está bien". El
entorno está en `scripts/dev/` (ver `entorno-verificacion.md`); todo se corre con
`scripts/dev/dev.sh <verbo>`.

> Para cambios **triviales** (strings, imports, ajustes de layout, limpieza de
> seeds) NO se corre esta escalera — vale lo que dice `Build & test` en CLAUDE.md.

| # | Capa | Comando | Gate (qué BLOQUEA) |
|---|------|---------|--------------------|
| 1 | **Build / compila** | `dev.sh build` | Build en rojo = BLOQUEANTE |
| 2 | **Android Lint** | `dev.sh lint` | Cualquier **Error** de Lint = BLOQUEANTE |
| 3 | **App arranca** | `dev.sh run` | No dice "App VIVA" / crashea al inicio = BLOQUEANTE |
| 4 | **Logs / logcat** | `dev.sh logcat`, `dev.sh crash` | Excepción, `FATAL` o `AndroidRuntime` = BLOQUEANTE |
| 5 | **Tests desde spec** | `gradlew test` | Test en rojo = BLOQUEANTE |
| 6 | **Casos límite** | (en el spec, ver abajo) | Escenario de borde sin test que pasa = BLOQUEANTE |

### Notas por capa

1. **Build.** En Kotlin el compilador YA chequea tipos: si el build pasa, el
   type-check pasó. NO existe un paso de "type check" separado en Kotlin — no lo
   inventes ni lo reportes como capa aparte.
2. **Lint.** Los **Error** bloquean (ej. `NewApi`: código que crashea en
   dispositivos viejos porque `minSdk = 26` usa APIs nuevas). Los **Warning** y
   **Hint** se reportan pero NO bloquean. `detekt`/`ktlint` NO están configurados;
   no los menciones como si existieran.
3. **App arranca.** Para cambios de esquema Room usá `dev.sh run -clean` (la DB de
   dev es descartable, decisión #29). "App VIVA" = no es prueba de que la feature
   ande, solo de que no muere al inicio; la prueba de comportamiento son las capas
   5 y 6.
4. **Logs.** Acá vivía el bug de migración Room. Leer la salida de runtime es
   obligatorio, no opcional: un crash silencioso post-arranque solo aparece acá.
5. **Tests desde spec.** En Strict TDD el test se escribe ANTES del código, derivado
   del spec. Corren en JVM de escritorio (dominio puro).
6. **Casos límite.** No es una capa difusa "a ojo" al final — ver la sección
   dedicada.

## Ejecución: en paralelo, y cuándo arrancar la app

El arranque del emulador es independiente del build, así que **NO se corre
secuencial.** El `verify-gate` debe lanzar el boot del emulador en background al
inicio, en paralelo con build + lint + tests:

```
t=0  ├── arranca el emulador (background) ───────────────┐ (1-2 min frío)
     ├── build + lint + tests (host) ──────┐
                                            ▼
                  APK listo + emulador ya (casi) booteado
                                            ▼
                       instalar + abrir + leer logs (~20-30s)
```

Así el costo del boot se **esconde** detrás del build/lint que igual corren, sin
necesidad de tener el emulador prendido todo el tiempo. (Nuance: build y boot
compiten por recursos del host; en una máquina justa cada uno va un toque más
lento, pero el solapamiento igual gana contra lo secuencial.)

**Cuándo arrancar la app (guía, no regla rígida):**

- **Default: arrancá** (en paralelo). Con el boot escondido, "arrancar de más" casi
  no cuesta.
- **Salteá el boot SOLO** si el cambio es **inequívocamente dominio puro** — nada
  bajo `ui/`, `data/`, `platform/`, manifest, DI/wiring, recursos. En ese caso el
  boot no aporta evidencia nueva (ya lo cubre `gradlew test`), no se saltea por
  lento sino porque no suma.
- **Ante la duda, arrancá.** El sesgo es siempre a la seguridad.

## Casos límite: cómo se realizan (y quién)

Los casos límite **no son un paso freeform** que el agente improvisa al verificar.
Son **escenarios del spec** (que es exactamente lo que un spec SDD enumera). El flujo:

1. Durante **spec/design**, el **agente PROPONE** los escenarios de borde en lenguaje
   natural (no código).
2. El **humano APRUEBA / audita** esa descripción en lenguaje natural. Esta es la
   intervención humana clave del ciclo.
3. En **apply** (Strict TDD), cada escenario aprobado obtiene su test ANTES del
   código.
4. En **verify**, la matriz de cumplimiento exige que cada escenario tenga un test
   que pasa.

Así el trabajo cognitivo se mueve al spec —donde corresponde y donde el humano ya
está mirando— en vez de aparecer como carga difusa al final.

## Quién corre y quién juzga cada capa

| Capa | Quién la corre | Quién la juzga |
|------|----------------|----------------|
| Build | agente | automático (exit code) |
| Lint | agente | agente (Error = bloquea) |
| App arranca | agente | agente (App VIVA / crash) |
| Logs | agente | agente (excepción = bloquea) |
| Tests desde spec | agente escribe y corre | **humano audita la descripción** |
| Casos límite | agente propone en el spec | **humano aprueba** |

## Límites honestos (qué NO prueban estas capas)

- `gradlew test` corre en la JVM de escritorio: NO ejercita migraciones Room reales,
  ni Compose, ni el comportamiento por nivel de API. **Verde ≠ app que funciona.**
- El emulador es **API 36** (el más nuevo): confirma que bootea, pero NO prueba el
  comportamiento en `minSdk = 26`. Esa clase de bug (APIs nuevas en dispositivos
  viejos) la caza **Lint (`NewApi`)**, no el emulador. Por eso Lint es obligatorio.
  *Mejora futura:* un AVD API 26 cazaría esa clase también ejecutando.
- El emulador headless no juzga estética ("¿se ve bien?"). Para eso está
  `dev.sh shot`, que saca una captura que el agente sí puede mirar.

## Definición de "terminado"

Un cambio NO trivial está **terminado** solo cuando TODAS las capas aplicables están
en verde. Si una capa aplicable quedó en rojo, el cambio NO está terminado, sin
importar lo que digan las demás. Saltear una capa = incumplir el contrato.
