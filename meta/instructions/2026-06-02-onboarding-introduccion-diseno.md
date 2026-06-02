# Onboarding / tutorial de introducción — Diseño conceptual

Fecha: 2026-06-02
Estado: **diseño conceptual cerrado** — insumo directo para la spec (contra `meta/guias/contrato-de-spec.md`). NO es implementación.

> Captura la sesión de diseño del onboarding. Reemplaza, en lo que toca al
> onboarding, las partes desactualizadas de `meta/handoffs/handoff-ui-onboarding.md`
> (ver §8). Las decisiones también están en Engram (`apk-personal`), topic keys
> `onboarding/*` y `naming/retirar-vocal`.

---

## 1. Contexto humano

El motor de scoring quedó completo y exige dos cosas que la UI hoy no garantiza:
3 capas con ≥1 ancla (si no, `NoData`) y una ventana de sueño elegida (si no, el
estado se topea en "En marcha"). El dueño observó que estas dos "compuertas" y el
"tutorial introductorio" **convergen en lo mismo**: no son piezas separadas, son la
**columna vertebral** de un único onboarding que arranca en el primer uso de la app.

Regla de trabajo de la sesión: **conceptos antes que código.** No se diseñó con qué
Composable, sino el *qué* y el *cómo se siente*, anclado a la filosofía
(`docs/producto/vocal-01-filosofia-producto.md`), el tono
(`docs/producto/tono-comunicacion.md`) y las definiciones de capa
(`docs/producto/Definicion_anclas.md`).

---

## 2. Decisiones cerradas

1. **Espíritu: narrativo y guiado.** Obligatorio (el motor lo necesita) pero
   conversacional: una decisión por pantalla, con la voz del Cuidador Lúcido
   explicando el sentido mientras se configura. NO un wizard frío, NO una lista larga
   como primera experiencia. Se descartó el "gate seco" (tira el tono) y el
   "no-bloqueante" (deja el motor en `NoData` = app muerta).

2. **Dos configuraciones (no tres tipos de usuario).** Al inicio solo hay:
   - **Estándar / frágil** (la mayoría, el público objetivo): Anclas + Sueño.
   - **Protección / sobriedad**: Anclas + Sueño + Racha de sobriedad.
   El usuario "comprometido" de §13 del mapa de componentes **NO** es un tipo de
   onboarding: nace del uso prolongado (suma soportes/tasks/más anclas con el tiempo).

3. **Soportes, pendientes, targets, modo riesgo: fuera del onboarding.** Son
   crecimiento progresivo, a ritmo del usuario, descubiertos después desde el
   Dashboard. Nunca forzados en la intro.

4. **Principio de scoring relativo (ya es contrato, verificado en doc).** El score
   mide solo lo que el usuario definió para sí mismo. 3 anclas vs 8 anclas+soportes
   pueden tener el mismo score; cada feature **sube la vara**, no regala puntos.
   Nunca se castiga al que tiene poco. (Respaldo: mapa de componentes §47, §993,
   §1095; filosofía §14.) Esto **justifica** que el onboarding sea mínimo.

5. **Intención suave (no clínica) ramifica las 2 rutas.** Una pregunta cálida
   ("¿Qué te trae aquí?") con 2 opciones que NO etiquetan a la persona ni dicen
   "adicción"/"recuperación". Nada queda bloqueado: todo módulo sigue disponible
   después desde el Dashboard. (Resuelve §17 #5.)

6. **Paso de anclas = solo elegir.** El usuario elige 3 anclas en 3 capas distintas.
   NO se piden targets (frecuencia, minutos, duración) en el onboarding: toman
   defaults seguros y se afinan después. (Resuelve §17 #6. Evidencia: el motor solo
   exige "3 anclas en 3 capas" para salir de `NoData`; `AnchorTargets.kt` ya tiene
   `DEFAULT_ANCHOR_WEEKLY_FREQUENCY=3` y normalizadores que caen al default.)

7. **Paso de sueño = ventana objetivo + permiso telemetría salteable.**
   - Se pide UN solo concepto: la ventana = `targetSleepAt` (hora estimada de dormir)
     + `targetWakeAt` (hora de despertar). La duración se deriva; NO se pregunta.
     Mínimo 5h (`MIN_SLEEP_WINDOW_MINUTES=300`).
   - El usuario **NO registra el sueño cada noche**: lo detecta la telemetría
     (`SleepInterpreter`, modo auto). El botón "voy a dormir" es opcional.
   - La auto-detección necesita el permiso de uso (UsageStats): se **ofrece** en el
     onboarding, explicado y **salteable** ("Más tarde"). No se bloquea.
   - `digitalWindDown` (descanso digital): **fuera del onboarding** (diferido D3, no
     puntúa en v1, tiene default).

8. **Notificaciones de sueño** (reformuladas para el modelo de telemetría):
   - **B · Aviso de sueño sin datos / permiso** (informativa): tras N noches sin
     telemetría, avisa que el estado puede quedar incompleto y sugiere revisar el
     permiso. **Encendida por defecto.** El N exacto se calibra con datos reales
     (gemelo de la deuda D1 del diseño de sueño).
   - **A · Recordatorio de hora de dormir / wind-down** (compromiso): anclado a
     `targetSleepAt`; tono invitación, nunca orden. Su estado por defecto **se
     pregunta explícitamente** en el paso de sueño (consentimiento informado).
   - Permiso `POST_NOTIFICATIONS` (Android 13+): se pide **perezoso**, recién cuando
     la primera notificación tiene sentido, NO en la intro.

9. **Registro literario del copy.** El copy adopta un registro casi poético/inspirador
   de inspiración Borgesiana (fuente: `docs/producto/Definicion_anclas.md`) MEZCLADO
   con el tono Cuidador Lúcido. El registro literario va solo en las partes
   conceptuales (bienvenida, definiciones de capa, cierre); los pasos de **acción**
   quedan claros y simples para no sumar fricción. El copy es **español neutro**, NO
   voseo (la app habla en neutro: "No necesitas...", "Volvamos...").

10. **Nombre de la app = "Autonomía sin límites"** (no "Vocal"). Ver §8 (pendientes):
    el retiro global de "Vocal" es una tarea de consistencia aparte.

---

## 3. Mapa de bloques del onboarding

```
BLOQUE 0    Bienvenida — define "base" = tus cimientos
BLOQUE 0.5  Intención suave — 2 rutas (estándar / sobriedad)
BLOQUE 1    Anclas — elegir 3 en 3 capas; capas = sellos que despliegan la lectura
BLOQUE 2    Sueño — ventana + permiso telemetría salteable + consentir wind-down
BLOQUE 3    Sobriedad — SOLO ruta protección; sin culpa
BLOQUE 4    Cierre + flag "onboarding completado" → Dashboard
```

---

## 4. Copy canónico (v3)

> Registro neutro + literario. Pasos de acción simples. Nombre: "Autonomía sin límites".

### Bloque 0 · Bienvenida

> **Autonomía sin límites**
> Toda vida descansa sobre cimientos que no siempre vemos: el descanso, el cuerpo, el
> orden de los días, los otros, aquello que construimos. Cuando uno cede, lo demás
> empieza a inclinarse.
>
> Esta app no pretende medir esa abstracción inalcanzable que llamamos felicidad, sino
> algo más sencillo y más noble: saber si tus cimientos siguen en pie.
>
> A ese conjunto de cimientos lo llamamos **tu base**. Vamos a reconocerla juntos, sin
> apuro y sin exigir perfección.
>
> `[ Empecemos ]`

### Bloque 0.5 · Intención suave

> **¿Qué te trae aquí?**
> No hay respuesta correcta. Podrás cambiarla cuando quieras.
>
> `→ Quiero ordenar mi día a día`  *(ruta estándar)*
> `→ Quiero cuidarme de algo que me cuesta`  *(ruta sobriedad)*

### Bloque 1 · Anclas

> **Tus anclas**
> Un ancla es una práctica pequeña que te sostiene. No tiene que ser grande: una
> página leída, un vaso de agua, una caminata. Lo pequeño, sostenido, alcanza.
>
> Elige **tres**, en tres áreas distintas de tu vida.
>
> `◈ Interior`  `◈ Cuerpo`  `◈ Conducta`  `◈ Vínculos`  `◈ Proyecto`

Cada capa es un **sello/ícono** (sin texto a la vista; estilo "sellos con peso visual"
de `AGENTS.md`). Al tocarlo se despliega su **lectura completa** (texto íntegro de
`Definicion_anclas.md`). Ejemplo, **Cuerpo**:

> **El Cuerpo**
> El frágil navío de carne y vigilia que nos transporta a través de los años. Es la
> única ancla verdadera que tenemos en la tierra. Aquí reside el descanso y el sueño,
> que no son una indulgencia, sino el cimiento sagrado de nuestra dignidad; sin ellos,
> la mente pierde su lucidez y la realidad se vuelve un laberinto insoportable.

*(Los detalles finos de cada ancla se ajustan después, sin apuro.)*

### Bloque 2 · Sueño

> **El descanso primero**
> El sueño no es una indulgencia: es el cimiento de la lucidez. Sobre él se apoyan el
> cuerpo y la mente; sin él, la realidad se vuelve un laberinto.
>
> No buscamos un número perfecto de horas. Buscamos tu ventana.
>
> ¿A qué hora piensas dormir? ¿Y despertar?
> `[ 23:30 ]  →  [ 07:30 ]`
>
> **Para leer tu descanso sin que anotes nada**, la app puede notar cuándo tu teléfono
> queda quieto de noche. La decisión es tuya.
> `[ Activar ]   [ Más tarde ]`
>
> ¿Quieres que te avise cuando se acerque tu hora de descanso?
> `[ Sí ]   [ No ]`

### Bloque 3 · Sobriedad *(solo ruta "cuidarme de algo que me cuesta")*

> **Cuidar algo que te cuesta**
> A veces hay un hábito oscuro, algo que amenaza con trizar la propia dignidad. Aquí
> no hay condena policial: solo el ejercicio de tu libertad más profunda para no
> perder el rumbo.
>
> Una recaída no es un fracaso. Es una señal, no una condena.
>
> ¿Quieres llevar el registro de algo que estás cuidando?
> `[ Sí, agregar ]   [ Ahora no ]`

### Bloque 4 · Cierre

> **Tus cimientos están en pie**
> Esto es el comienzo de un viaje, no un examen. Podrás ajustar todo cuando quieras.
>
> Y si algún día el rigor decae, la app no te condena: solo te recuerda, con la calma
> de un adulto funcional, que es momento de volver a la base, de volver al cuerpo, y
> recomenzar.
>
> `[ Entrar ]`

---

## 5. Preguntas abiertas §17 — estado tras esta sesión

| # | Pregunta | Estado |
|---|----------|--------|
| 1 | ¿Sueño obligatorio desde onboarding? | ✅ Sí (Bloque 2, compuerta del motor) |
| 2 | ¿Cuántas anclas mínimas? | ✅ 3 en 3 capas |
| 5 | ¿Tipo de uso elegido o progresivo? | ✅ Intención suave (2 rutas) |
| 6 | ¿Targets al crear o después? | ✅ Después (solo elegir en onboarding) |
| 9 | ¿Sueño sin sonar controlador? | ✅ Ventana (no número), permiso salteable, wind-down consentido |
| 10 | ¿Mínimo para salir de "Sin datos"? | ✅ 3 capas con ancla |
| 3 | ¿Soportes desde el inicio? | ✅ No (crecimiento progresivo) |
| 4 | ¿Abstinencias en onboarding? | ✅ Solo en ruta sobriedad, sin culpa |
| 8 | ¿Recuperación sin sentirse castigado? | ✅ Copy Bloque 3 ("no condena policial", "señal, no condena") |
| 7 | ¿Score con modos internos según componentes? | 🟡 Fuera de alcance del onboarding (decisión de scoring) |

---

## 6. Referencias técnicas directas

**Compuertas del motor:**
- `domain/scoring/ScoringConstants.kt` — `MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3`.
- `domain/activity/AnchorTargets.kt` — `DEFAULT_ANCHOR_WEEKLY_FREQUENCY=3`,
  `MIN_SLEEP`... normalizadores con default; `hasRequiredAnchorTargets()`.
- `domain/sleep/SleepPolicy.kt` — `targetSleepAt`/`targetWakeAt`,
  `MIN_SLEEP_WINDOW_MINUTES=300`, default heredado 23:30→07:30 (a reemplazar por
  elección activa).

**Pantallas de config a orquestar (ya existen):**
- `ui/anchors/AnchorConfigScreen.kt`, `ui/anchors/GoalPreset.kt`,
  `ui/sleep/SleepConfigScreen.kt` (tiene `AutoModeCard` + `PermissionStep` para
  telemetría), `ui/sobriety/SobrietyConfigScreen.kt`.

**Entrada / persistencia:**
- `MainActivity.kt` — `enum AppScreen` privado + `when (currentScreen)`. Aquí engancha
  el gate de onboarding (decidir primera pantalla según flag "completado"). Habría que
  agregar `AppScreen.Onboarding`.
- `AutonomiaRepository.kt` — acceso a Room + `prefs.edit { }` (tema/automode). Aquí
  viviría el flag "onboarding completado".

**Contrato de producto/diseño:**
- `docs/producto/vocal-01-filosofia-producto.md` (§13 mínima fricción, §14
  personalización), `docs/producto/tono-comunicacion.md`,
  `docs/producto/Definicion_anclas.md` (capas), `docs/sueno/decisiones-diseno-sueno-v1.md`
  (modelo de sueño: telemetría, no registro nocturno),
  `docs/frontend/vocal_mapa_componentes_v_0_2_borrador.md` §17,
  `docs/frontend/mis-anclas-ux-canon-v1.md` ("no debe parecer un wizard").

---

## 7. Decisiones de implementación pendientes (para la spec/design, NO cerradas aquí)

- Flag "onboarding completado" en prefs (`AutonomiaRepository`) + gate de primera
  pantalla en `MainActivity` (`AppScreen.Onboarding`).
- Persistencia (¿se guarda?) de la intención del Bloque 0.5 — ¿solo ramifica el
  onboarding o informa tono/ofertas a futuro?
- El N exacto de "noches sin datos" para la notificación B (calibrar).
- ¿Recortar la densidad literaria de los pasos de acción en una próxima pasada de
  copy? (Pregunta abierta menor — hoy el copy v3 se acepta como base.)

---

## 8. Pendientes / correcciones derivadas

- **Corregir `meta/handoffs/handoff-ui-onboarding.md`** (líneas ~88-91): pide
  "recordatorio para registrar el sueño" / "días sin registrar", que **contradice** el
  diseño sellado de sueño (auto por telemetría, sin registro nocturno). Reformulado en
  §2.8 de este doc. (Nota: el handoff es semi-congelado; actualizar, no reescribir.)
- **Retirar "Vocal" de todo el repo** → "Autonomía sin límites". Tarea de consistencia
  **aparte** (~190 ocurrencias en 61 `.md`; solo 1 en `.kt`). Landmines: NO tocar docs
  congelados (`docs/old/`, `meta/handoffs/`, `docs/auditorias/`); renombrar archivos
  rompe `@references`; la clave de proyecto en Engram sigue siendo `apk-personal`.
- **Documentar formalmente `docs/producto/Definicion_anclas.md`** (hoy filename con
  mayúscula/guiones bajos, sin header `> Estado: vivo`). Tarea del dueño.

---

## 9. Próximo paso

Con el diseño conceptual cerrado, el siguiente paso es escribir la **spec del
onboarding** y pasarla por la compuerta de `meta/guias/contrato-de-spec.md` (sección 9)
ANTES de lanzar SDD. Este documento es el insumo directo de esa spec.
