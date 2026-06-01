# Decisiones de diseño — Sueño (consumidor de `device-telemetry`) v1

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

> **Estado: DISEÑO CONCEPTUAL CERRADO.** Insumo directo del ciclo SDD
> (proposal → spec → design → tasks → apply → verify). No es implementación:
> es el contrato conceptual acordado con el dueño.
>
> Fecha: 2026-05-29 · Proyecto: apk-personal (Vocal)
>
> Leer junto con: `handoff-sleep-consumer.md` (arranque y contrato de consumo),
> `sleep-feature-preliminar.md` (edge cases originales), `arbol-scoring-vocal-v1.md`
> (fórmulas **selladas**), `nucleo-dominio-autonomia.md` (filosofía: sueño es core).

---

## 0. Qué cierra este documento

Captura TODAS las decisiones de diseño tomadas en la sesión de definición
(iteración idea ↔ pregunta con el dueño). La §9 lista la **deuda técnica / decisiones
diferidas a propósito** — no son olvidos, son recortes conscientes de alcance para v1.

---

## 1. Arquitectura y frontera (no reabrir)

Cadena de responsabilidad, cada caja ciega a la siguiente:

```
device-telemetry  → guarda HECHO crudo en Room        (ciega; no conoce a Sueño)
Sueño (consumidor) → LEE telemetría + interpreta → segmentos + sleepScore 0..1
Motor de scoring   → recibe el número ya digerido → LayerScore Cuerpo → ScoreReport
```

- El **motor de scoring NUNCA toca telemetría cruda**. Solo **Sueño** la lee.
- Sueño produce un `0..1`; no conoce la capa. El núcleo lo ubica en **Cuerpo**
  (`SpecialLayerScoringPolicy`, `SLEEP_WEIGHT_IN_BODY = 0.30`).
- La interpretación vive en `domain/sleep/` (pura). La captura vive fuera, en
  `platform/telemetry/` (ya entregado).

---

## 2. Modelo de experiencia

| Decisión | Detalle |
|----------|---------|
| **Compromiso asistido** | El humano lidera poniendo su **horario objetivo**; la telemetría completa/afina. |
| **El compromiso = el horario configurado** | Se setea una vez. NO es apretar un botón cada noche. |
| **Botón "voy a dormir" = OPCIONAL** | Si se aprieta, afina la detección (ancla precisa) + activa detox/bloqueo. Ideal: el usuario no marca nada. |
| **No se confía en "Desperté"** | Nadie aprieta a las 3am (mínima fricción). La telemetría determina el despertar real. |
| **Sueño es CORE, NO opt-in** | A diferencia de sobriedad (sí opt-in). Sin sueño la base no está completa. |
| **Ventana objetivo mínima = 5h** | El score NO castiga elegir 5/6/7/8h. Lee cumplimiento de **tu** ventana. |

---

## 3. Las dos ventanas (clave del diseño)

Dos relojes distintos alimentan componentes distintos. No mezclarlos.

| Ventana | Qué es | Qué alimenta |
|---------|--------|--------------|
| **OBJETIVO** | El horario que configuraste (ej. 12am–5am) | Solo **AlineaciónHorario** (0.20). Dormir fuera del objetivo baja SOLO este 20%. |
| **DETECCIÓN** | **Noche biológica fija = `20:00`–`12:00`** (8pm–mediodía, igual para todos) | **Duración** (0.40) + **Continuidad** (0.25) + **InterrupciónDigital** (0.15). |

> Consecuencia: dormir fuera del objetivo (3am–8am con objetivo 12am–5am) penaliza
> Alineación, pero **NO pierde las horas reales** — la ventana de detección las captura.

**Bordes temporales (cerrados):**

- **Ventana = `20:00` a `12:00`** del día siguiente. Cubre al madrugador (acostarse 8pm)
  y al que duerme tarde, sin tragarse siestas de tarde.
- **Anclaje al objetivo (mitigación de siesta)**: dentro de la ventana, "la noche" es el
  **período de sueño principal que solapa o está cerca de tu horario objetivo**. Un bloque
  aislado lejos del objetivo (siesta de 8pm con objetivo 12am) NO es la noche.
- **A qué día pertenece**: la noche es del **día en que te despertás** (la noche
  11pm lun → 7am mar es "el sueño del martes"). Alinea 7 noches con 7 días, sin desfase.

---

## 4. Interpretación de la telemetría

### 4.1 Despertar real (la primitiva, edge case #1)

- **Gatillo = CUALITATIVO**: un despertar real es un evento de **uso real**
  (`USER_INTERACTION` o `APP_FOREGROUND`). Solo encender la pantalla (`SCREEN_ON`
  sin uso) = **vistazo, se ignora** (mirar la hora a las 3am no rompe el sueño).
- **Gravedad ≠ gatillo**: la duración y la cantidad miden **cuánto pesó**, no SI contó.
- **Episodio de uso real** = tanda seguida de actividad que termina cuando el
  teléfono se vuelve a quedar quieto = **un** despertar.
- **Inicio del sueño** = cuando el teléfono se queda quieto tras el último uso real
  (NO al apretar un botón). Esto disuelve el bug histórico de "el detox se cuenta como sueño".

| Señal | Alimenta |
|-------|----------|
| Conteo de episodios de uso real | **Continuidad** |
| Duración total del uso nocturno | **InterrupciónDigital** |

### 4.2 Espectro de confianza del dato

| Situación | Lectura |
|-----------|---------|
| Teléfono **quieto** durante la noche biológica | **Buen sueño, ALTA confianza** (la quietud es la firma del buen sueño, no su ausencia) |
| **Sin señal** (teléfono apagado / sin telemetría) | **NoData / base incompleta** — NUNCA 0, NUNCA estado bajo fabricado |
| Señal **genuinamente ambigua / contradictoria** | **Baja confianza real** → ahí sí baja el PUNTAJE |

> Regla de oro: "poca señal" ≠ "baja confianza". No castigar la mejor noche por
> generar poca señal (ejemplo de la cocina: teléfono cargando lejos, durmió perfecto).

---

## 5. Scoring

- **4 componentes y pesos SELLADOS** (`arbol-scoring-vocal-v1.md`, no se rediscuten):
  `SleepWeeklyScore = 0.40·Duración + 0.25·Continuidad + 0.20·AlineaciónHorario + 0.15·InterrupciónDigital`
- **Sueño entra a Cuerpo al 30%**: `Body = 0.70·BodyBaseWithoutSleep + 0.30·SleepWeeklyScore`.
- **Sin superávit en v1**: dormir de más = **NEUTRO** (Duración llega a 1.0 al cumplir
  el objetivo y se queda; no decae). El superávit como bonus de margen queda para futuro (§9).

---

## 6. Modelo de datos — noche fragmentada

- **Tabla de segmentos dedicada** (`SleepSegmentEntity`, nueva).
- **Razón**: la telemetría cruda se **purga en días** → los segmentos son el **HECHO
  PRIMARIO durable**, no un cache descartable. Y como las fórmulas se siguen calibrando,
  guardar segmentos permite **recalcular** los agregados al cambiar umbrales (los
  agregados no se pueden des-agregar).
- Los escribe **Sueño** al cerrar la noche. Los 4 componentes se derivan de los segmentos.
- ⚠ Implica **migración Room nueva** con la disciplina estricta del proyecto:
  índices `index_<tabla>_<col>` (no `idx_*`), `MigrationTestHelper`, `exportSchema`.

**Forma del modelo (cerrado):** dos piezas.

- **Cabecera de la noche** (evolución de `SleepLog`, PK = fecha de despertar): horario
  objetivo (`targetSleepAt`/`targetWakeAt`), `sleepOnsetAt`, `definitiveWakeAt`, nivel de
  **confianza** (alta / ambigua / NoData — §4.2), `note`. El `quality` hardcodeado se
  elimina; los sub-scores pueden cachearse acá o derivarse.
- **`SleepSegmentEntity`** (nueva, hija de la noche): **línea de tiempo completa** —
  `startAt`, `endAt`, `kind` (`Asleep` | `AwakeUse`). De los segmentos salen los 4
  componentes: suma de `Asleep` → Duración; conteo de `AwakeUse` + `Asleep` más largo →
  Continuidad; suma de `AwakeUse` → InterrupciónDigital.

> Nota de límite: un tramo en silencio se asume `Asleep`; la telemetría no distingue
> "dormido" de "despierto sin tocar el teléfono" (ej. leer un libro). Aceptado para v1.

---

## 7. Cierre de la noche (edge case #5)

- **Híbrido**: la noche se cierra al detectar el **despertar definitivo** (uso real
  sostenido tras la hora objetivo de despertar, sin volver a dormir).
- **Tope de seguridad**: si nunca se detecta, cae al **fin de la ventana biológica**.
- **Disparo**: reusar el cierre diario existente (`DailyClosureWorker`, medianoche)
  **+ garantía al abrir la app**. Sin maquinaria nueva. Debe materializarse **antes**
  de que la telemetría se purgue.

---

## 8. De la noche a la semana

- **Promedio de las noches CON dato (cobertura suave)**: cada noche → su `sleepScore`
  0..1; la semana = promedio de las noches con dato.
- Una noche **sin dato NO entra como cero** (respeta §4.2). Si hay pocas noches con
  dato, la lectura de sueño queda débil y **no habilita estados altos** (sueño es core).

---

## 9. Deuda técnica / decisiones diferidas (a propósito)

Recortes conscientes de alcance para v1. **No son olvidos** — quedan acá para no ser sorpresas.

| # | Item | Por qué se difiere |
|---|------|--------------------|
| D1 | **Piso de cobertura DURO** (mín. N noches con dato o NoData) | v1 usa cobertura suave; el N exacto se calibra con datos reales. |
| D2 | **Superávit de sueño** (bonus de margen por dormir de más hacia el techo de 8h) | Fuera de v1; entra como bonus de margen (gemelo de AnchorSurplusBonus), nunca dentro del puntaje base (el "vaso" se corta en 1.0). |
| D3 | **Detox digital en el scoring** | v1 lo deja como recordatorio visual en config (no puntúa). InterrupciónDigital se calcula solo por uso durante el sueño detectado. |
| D4 | **Término de consistencia explícito** | `nucleo-dominio` menciona "consistencia"; el árbol sellado tiene 4 componentes sin término propio de consistencia. Se evalúa a futuro. |
| D8 | **Deuda pre-release de `device-telemetry` (tarea 5.2)** | Registrar `MIGRATION_10_11` + `MigrationTestHelper` + `exportSchema`. Aplica también a la migración de segmentos. |

> **Cerrados en esta sesión** (ya no son deuda): D5 (bordes de la noche `20:00`–`12:00`
> + anclaje al objetivo, §3), D6 (la noche pertenece al día de despertar, §3),
> D7 (forma del modelo: cabecera + `SleepSegmentEntity` con línea de tiempo completa, §6).

---

## 10. Bugs detectados en el código actual (a corregir al implementar)

| Bug | Ubicación | Fix |
|-----|-----------|-----|
| `SleepScoring` usa 2 de 4 componentes (`duration·0.70 + schedule·0.30`) | `domain/sleep/SleepScoring.kt` | Implementar los 4 componentes sellados. |
| Dormir de más **decae** el puntaje (hasta 0.5) | `SleepScoring.kt` | Neutralizar: cumplir el objetivo = 1.0 y se queda (§5). |
| Sueño ausente → `null` → `0f` → **hunde Cuerpo** | `SpecialLayerScoringPolicy.kt` / pipeline | Ausencia = NoData / base incompleta, no estado bajo fabricado (§4.2). |
| `digitalWindDownMinutes` inerte (se valida pero no llega al scoring) | `ScoreInputSource` / `BuildScoreInputUseCase` | v1: queda inerte **a propósito** (§D3), documentado. |
| `quality` hardcodeado a `Acceptable` | `AutonomiaRepository.kt:435` | Reemplazado por el modelo de segmentos + 4 componentes. |
| `SleepLog` = un solo par `sleptAt`/`wokeAt` (no soporta fragmentación) | `Models.kt:85` | Migrar a tabla de segmentos (§6). |
| Scoring mira **una sola noche**, el árbol pide semanal | `WeeklyScoringContextBuilder.kt:32` | Promedio semanal de noches con dato (§8). |

---

## 11. Próximo paso

Diseño conceptual cerrado → **arrancar ciclo SDD** (`sdd-explore` / `sdd-new` de
`sleep-consumer`), usando este documento como contrato. Strict TDD activo: la
interpretación de sueño es lógica pura JVM → test-first.
