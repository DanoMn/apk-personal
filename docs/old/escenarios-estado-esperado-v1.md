# Escenarios de estado esperado — Scoring v1

> **Estado: borrador en construcción** — NO es contrato vigente todavía. Este
> documento define el COMPORTAMIENTO esperado del scoring (qué estado debería ver
> el usuario en cada situación) ANTES de tocar las fórmulas. La matemática se
> deriva de acá, no al revés.

Fecha de inicio: 2026-06-04
Producto: Autonomía sin límites

---

## 0. Por qué existe este documento

El scoring actual tiene dos problemas reales (ver
`scoring/diagnostico-reset-lunes-y-usuario-nuevo` en memoria y el diagnóstico de
sesión): **se reinicia cada lunes** y **manda al usuario nuevo al estado más bajo**.
La causa raíz no es un error de suma: es que **se escribieron fórmulas antes de
definir el comportamiento esperado**. No hay un contrato de "en esta situación, el
usuario debería ver este estado".

Este documento corrige eso. Es una colección de **escenarios de uso** (user
stories) con su **estado esperado**. Una vez acordados, estos escenarios se
convierten en:

1. el spec de comportamiento del scoring;
2. los casos de test del motor;
3. la fuente desde la que se re-deriva la matemática (umbrales, pesos, inercia).

**Regla de trabajo:** primero llenamos y acordamos los escenarios. Recién cuando el
comportamiento esté claro, traducimos a fórmula. Nada de matemática hasta entonces.

---

## 1. Los cinco estados (vocabulario)

Nombres canónicos de UI (ver `AGENTS.md`):

| Estado | Lectura humana (qué le dice al usuario) |
| --- | --- |
| **Restauración** | "La base está caída. Volvamos a lo mínimo, sin castigo." |
| **Atención** | "Algo se está aflojando. Es una señal, no una condena." |
| **En marcha** | "Estás sosteniendo tu base. Vas bien." |
| **Plenitud** | "Semana plena. Estás cumpliendo con holgura." |
| **Inquebrantable** | "Constancia probada en el tiempo. Esto ya es estructura." |

> Estado actual del motor (para contraste, NO es lo que queremos necesariamente):
> Restauración `base<0.40`, Atención `0.40–0.70`, En marcha `0.70–0.85`, Plenitud
> `≥0.85`; Inquebrantable requiere 5 semanas previas + base≥0.90 + peor capa≥0.80 +
> estabilidad≥0.90. Una peor capa `<0.30` fuerza Restauración sin importar el resto.

---

## ⭐ Decisión 2026-06-04 — Modelo de evaluación: ventana móvil de 7 días

**El cambio de raíz.** El estado NO se calcula sobre la semana de calendario
(lunes→domingo), sino sobre una **ventana móvil de los últimos 7 días**: cada día,
la ventana es `[hoy−6 … hoy]`. Igual que una app de finanzas muestra "lo que
gastaste en los últimos 30 días", no "lo que gastaste este mes calendario".

Lectura conceptual: el estado responde **"¿cómo venís en los últimos 7 días?"** —
refleja el presente del usuario, no una nota de examen que se entrega el domingo y
se borra el lunes.

Por qué esto resuelve los dos bugs de raíz:

- **Mata el reset del lunes.** Ya no existe un lunes que reinicia el conteo. El
  lunes, la ventana sigue conteniendo los 6 días anteriores; el score se mueve
  suave día a día (entra el día de hoy, sale el de hace 7 días).
- **Mata la proración rota.** Como la ventana siempre tiene 7 días (salvo en la
  vida temprana del usuario), el denominador de frecuencia ya no se infla
  artificialmente cada inicio de semana.

Nota técnica: las anclas tienen metas semanales ("3 veces por semana"). La ventana
móvil evalúa "doneDays en los últimos 7 días / targetDays" — encaja con metas
semanales mejor que el corte de calendario. Punto de entrada a cambiar:
`WeeklyScoringContextBuilder.kt` (hoy `weekStart = previousOrSame(MONDAY)`).

### Tabla de umbrales — propuesta v1 del dueño (a validar contra escenarios)

Sobre el % de cumplimiento de lo planificado en la ventana de 7 días:

| Cumplimiento (ventana 7 días) | Estado |
| --- | --- |
| 0–20% | Restauración |
| 20–60% | Atención (en riesgo) |
| 60–80% | En marcha |
| 80–100% | Plenitud |
| Superávit | ⚠️ ¿Inquebrantable? (ver nota) |

> **Decisión 2026-06-04 (refinamiento) — Inquebrantable.** Por ahora los CINCO
> estados (incluido Inquebrantable) se calculan SOLO desde la ventana de 7 días.
> Inquebrantable pasa a ser el tramo superior (Plenitud + superávit en la ventana).
> La idea de Inquebrantable como "constancia probada en el tiempo" se DIFIERE a un
> futuro sistema de rangos acumulativos (estilo CS:GO: sumás y restás puntos de rango
> con el tiempo). No se construye la memoria larga ahora.

### Alcance actual: una sola capa

El dueño decidió mantenerlo simple: **por ahora, una sola capa.**

1. **Capa 1 — ventana móvil de 7 días** (lo único que hacemos ahora). Los cinco
   estados salen de acá: reflejan "cómo venís en los últimos 7 días".
2. **Capa 2 — memoria larga / arrastre + rangos acumulativos** (FUTURO, fuera de
   alcance ahora). El sistema de rangos estilo CS:GO y la inercia que protege tras un
   buen historial. Se retoma más adelante.

### Qué es el "%" que mapea a estado

El % que entra a la tabla de umbrales es el `WeeklyBaseScore` que el motor YA calcula
(se reutiliza tal cual; solo cambia la ventana a 7 días). NO es un "% de días hechos"
ingenuo: separa frecuencia (70%) de valor/intensidad (30%), y pondera la PEOR capa al
25%. Por eso, dos usuarios con el mismo "esfuerzo total" pueden caer en estados
distintos según cómo lo repartieron entre capas. → Los umbrales se calibran contra
escenarios concretos, no a ojo sobre un % supuestamente lineal.

> **Pendiente acotado — usuario nuevo.** En los **primeros 7 días de vida** del
> usuario la ventana aún no tiene 7 días completos. Ahí el denominador debe contar
> días-desde-config (días disponibles), no 7 fijos — si no, reaparece el castigo al
> usuario nuevo. La proración queda así reducida a un único caso acotado (el
> arranque de la cuenta), no a cada lunes.

---

## 2. Dimensiones de un escenario

Cada escenario se define por la combinación de:

- **Horizonte temporal** — ¿cuánto lleva el usuario usando la app? (semana 1, 2, 3–4, 6+).
- **Patrón de cumplimiento** — ¿qué hizo? (ver vocabulario §3).
- **(salida) Estado esperado + razón** — lo que el usuario debería ver, y por qué.

El horizonte importa porque el mismo patrón ("una semana al 100%") NO debería
significar lo mismo en la semana 1 que en la semana 6.

---

## 3. Vocabulario de patrones de cumplimiento

Para no repetir descripciones largas, definimos términos:

| Patrón | Definición |
| --- | --- |
| **Completa (100%)** | Cumplió las metas de frecuencia de TODAS sus anclas esa semana. |
| **Alta parcial** | Cumplió la mayor parte (~70–99% de lo propuesto). |
| **Media parcial** | Cumplió a medias (~40–70%). |
| **Baja** | Cumplió poco (~10–40%). |
| **Nula / abandono** | No registró nada (o casi nada) esa semana. |
| **Superávit** | Completa + excedió metas (más días o más valor que el objetivo). |
| **Intermitente** | Alterna semanas/días buenos y malos, sin tendencia clara. |
| **Decaimiento** | Venía alto y baja progresivamente semana a semana. |
| **Recuperación** | Venía bajo y sube progresivamente. |
| **Capa caída** | Una capa específica colapsa mientras el resto va bien (peor capa). |

Leyenda de la columna *Estado esperado*:

- ✅ **propuesta** — parece haber consenso; a confirmar.
- ⚠️ **DECISIÓN ABIERTA** — requiere tu criterio de producto. Las opciones en juego
  están en la nota. **No lo resolví por vos a propósito.**

---

## 4. Escenarios por horizonte

### 4.1 Horizonte corto — Semana 1 (días 1–7, onboarding)

Acá vive el "castigo al usuario nuevo". Es el terreno del bug de proración y de la
"amortiguación inicial" que se diseñó (`plan-tecnico-scoring.md` §2.1.1) y nunca se
implementó.

| ID | Qué hace el usuario | Estado esperado | Nota / decisión |
| --- | --- | --- | --- |
| E1.1 | Configura base (3+ capas con anclas), no marca nada todavía | ✅ Sin datos (NoData) | Ya implementado. Sin hechos no se juzga. |
| E1.2 | Día 1: marca su primera ancla cumpliendo | ⚠️ | **HOY el motor lo manda a Restauración** (proración + colapso de peor capa). Es el bug. ¿Qué mostrar? Opciones: estado de bienvenida neutro / "En marcha" amortiguado / "Atención". |
| E1.3 | Días 1–3: cumple el 100% de lo que toca cada día | ⚠️ | ¿Se puede juzgar con 3 días? ¿"En marcha"? ¿O nada hasta cerrar la semana? |
| E1.4 | Semana 1 cerrada al 100% (cumplió todas las metas semanales) | ⚠️ DECISIÓN MADRE | ¿**Plenitud** (fue difícil hacerlo todo) o **En marcha** (Plenitud se gana con tiempo)? Esto define casi todo lo demás. |
| E1.5 | Semana 1 al 100% **+ superávit** | ⚠️ | ¿El superávit SUBE de estado (Plenitud) o solo agrega una señal/margen visible sin cambiar el estado base? |
| E1.6 | Semana 1: media parcial (~50%) | ⚠️ | Primera semana floja. ¿Atención? ¿Amortiguado a En marcha porque recién arranca? |
| E1.7 | Configura y abandona: no marca nada en 7 días | ⚠️ | No hizo nada, pero tampoco "abandonó" algo que sostenía. ¿Sigue NoData? ¿Atención? |

### 4.2 Horizonte corto — Semana 2 (días 8–14, ~"15 días")

Primera semana CERRADA + semana en curso. Acá aparece el "reset del lunes": al
empezar la semana 2, el motor vuelve a calcular desde cero la semana en curso.

| ID | Qué hace el usuario | Estado esperado | Nota / decisión |
| --- | --- | --- | --- |
| E2.1 | Semana 1 cerrada al 100%; semana 2 día 1–2 cumpliendo | ⚠️ EL RESET DEL LUNES | ¿Mantiene el estado de la semana cerrada (arranque tibio) o se desploma por proración? Este es el síntoma principal. |
| E2.2 | Semana 1 al 100%, semana 2 al 100% | ⚠️ | Dos semanas completas seguidas. ¿Plenitud? ¿En marcha "fuerte"? |
| E2.3 | Semana 1 al 100%, semana 2 abandono total | ⚠️ | ¿Cuánto protege una semana buena? ¿Baja a Atención? ¿Puede caer a Restauración con solo 1 semana de respaldo? |
| E2.4 | Semana 1 baja (40%), semana 2 alta (90%) | ⚠️ Recuperación | ¿Premiamos la tendencia ascendente? ¿En marcha? |
| E2.5 | Semana 1 al 100%, semana 2 decae a 60% | ⚠️ Decaimiento | "Empieza a dejar de cumplir" (tu caso). ¿Atención como señal temprana? |

### 4.3 Horizonte medio — Semanas 3–4 (días ~15–30, ~"30 días")

Patrón establecido. Hay historial suficiente para hablar de tendencia, pero todavía
no para Inquebrantable (necesita 5 semanas).

| ID | Qué hace el usuario | Estado esperado | Nota / decisión |
| --- | --- | --- | --- |
| E3.1 | 4 semanas seguidas al 100% | ⚠️ | ¿Plenitud firme? ¿Antesala de Inquebrantable? |
| E3.2 | 4 semanas mixtas (100, 80, 100, 70) | ⚠️ | ¿Cómo pesa el promedio vs la última semana? ¿En marcha? |
| E3.3 | 3 semanas buenas, semana 4 abandono total | ⚠️ TU CASO CLAVE | "Buen mes y abandonó, no lo mandes a Restauración." ¿Hasta dónde cae? ¿Atención? ¿En marcha amortiguado? ¿Qué tanto pesa el buen historial? |
| E3.4 | 4 semanas decayendo (100, 80, 60, 40) | ⚠️ | Ninguna semana es cero, pero la tendencia es mala. ¿Atención? ¿Restauración por la trayectoria? |
| E3.5 | 2 semanas malas iniciales, luego 2 buenas | ⚠️ Recuperación | ¿La trayectoria ascendente reciente manda sobre el mal arranque? |

### 4.4 Horizonte largo — Semanas 5–6+ (días ~30–45+, memoria madura)

Recién con 5 semanas previas versionadas se habilita `StabilityScore` e
`Inquebrantable`.

| ID | Qué hace el usuario | Estado esperado | Nota / decisión |
| --- | --- | --- | --- |
| E4.1 | 6 semanas al 100% + superávit + peor capa alta + sueño registrado | ✅ Inquebrantable | Es el diseño. A confirmar umbrales exactos. |
| E4.2 | 6 semanas al 100% pero SIN registrar sueño nunca | ⚠️ | Hoy se topea en En marcha (§16.7, sueño es CORE). ¿Mantenemos esta regla? |
| E4.3 | 5 semanas impecables, semana 6 colapsa (abandono) | ⚠️ | Tras mes y medio perfecto, una semana de colapso. La inercia debería ser máxima acá. ¿A qué estado cae? |
| E4.4 | 6 semanas con una capa siempre caída (resto perfecto) | ⚠️ | Hoy la peor capa `<0.30` fuerza Restauración aunque 4 de 5 capas estén perfectas. ¿Es lo que queremos? |
| E4.5 | 8 semanas intermitentes (alterna buena/mala) | ⚠️ | Nunca constante, pero nunca colapsa del todo. ¿En marcha? ¿Atención? |

---

## 5. Casos límite / conflictivos (transversales)

Los que rompen la cabeza. No son escenarios sueltos: son las **tensiones de diseño**
que hay que resolver para que TODO lo de arriba sea coherente.

- **CL1 — Umbral de Restauración.** ¿Cuánto abandono lleva a Restauración? ¿Una
  semana ~0? ¿Varios días seguidos sin importar el historial? ¿Solo cuando NO hay
  historial que amortigüe?
- **CL2 — Qué hace el superávit.** ¿Sube de estado, o solo da una señal visible y
  margen sin cambiar la banda? (El contrato actual dice que NO sube de banda —
  premia constancia, no acumulación puntual; §16.6).
- **CL3 — Peso de la peor capa.** Una capa caída con el resto perfecto: ¿arrastra
  todo a Restauración (hoy sí, si `<0.30`) o solo baja un escalón?
- **CL4 — Asimetría subida/bajada.** Subir de estado debería costar (constancia).
  Bajar, ¿debería tener inercia (no caer de golpe) o ser inmediato?
- **CL5 — Semana en curso vs semana cerrada.** ¿El usuario ve el estado de la
  semana EN CURSO (que arranca incompleta cada lunes) o el de la última semana
  CERRADA hasta que la actual termine? Esto, solo, podría resolver el reset del lunes.

---

## 6. Preguntas abiertas (decisiones de producto a sellar)

Estas son las decisiones madre. Cada una destraba un montón de escenarios de arriba.
**Ninguna está decidida todavía.**

1. **¿Plenitud se gana en 1 semana perfecta, o requiere 2+ semanas sostenidas?**
   (destraba E1.4, E2.2, E3.1)
2. **¿El superávit sube de estado, o solo da margen/señal visible?**
   (destraba E1.5, CL2)
3. **Inercia de bajada: tras N semanas buenas, ¿hasta dónde puede caer en 1 semana
   mala? ¿Cuánto pesa el historial?** (destraba E2.3, E3.3, E4.3, CL4)
4. **Restauración: ¿requiere abandono sostenido (cuántos días/semanas) o basta una
   semana ~0?** (destraba E1.7, CL1)
5. **¿El usuario ve el estado de la semana en curso (parcial) o de la última semana
   cerrada?** (destraba E2.1, CL5 — el reset del lunes)
6. **¿Se implementa la amortiguación inicial de la primera semana** (diseñada en
   `plan-tecnico-scoring.md` §2.1.1, nunca construida)? (destraba E1.2, E1.3, E1.6)
7. **Proración de semanas parciales: ¿la frecuencia se juzga sobre los días
   transcurridos o sobre la semana completa?** (el bug; destraba E1.2, E2.1)

---

## Ideas a futuro (fuera de alcance ahora)

Anotadas para no perderlas; NO se implementan en esta tanda.

- **Notificación de ajuste de meta por valor/tiempo.** Si el usuario marca, p. ej.,
  5 min para una actividad pero registra de forma sostenida bastante más (20–30 min),
  tras N veces saltar una sugerencia para subir la meta al promedio real que viene
  haciendo. (Ya esbozado en `arbol-scoring-v1.md` §8.3.)
- **Notificación de ajuste de meta por constancia.** Equivalente para la frecuencia
  semanal: si sostiene más días de los pactados, sugerir subir la meta de frecuencia.
- **Superávit de constancia vale más que superávit de tiempo.** Ya reflejado en el
  motor (el bonus de superávit pondera frecuencia 70% / valor 30%); mantener este
  criterio al rediseñar.
- **Sistema de rangos acumulativos estilo CS:GO** (memoria larga / Capa 2). El
  Inquebrantable "probado en el tiempo" vive acá.

## 7. Cómo seguimos

1. Llenar / corregir los estados esperados de cada escenario (resolver los ⚠️).
2. Agregar los escenarios que falten (este set es siembra inicial, no es exhaustivo).
3. Cuando el comportamiento esté claro y acordado, recién ahí derivar la matemática:
   umbrales, pesos, fórmula de inercia/memoria temporal.
4. Convertir los escenarios acordados en tests del motor.
