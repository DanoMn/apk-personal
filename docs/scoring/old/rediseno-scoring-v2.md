> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Bitácora del rediseño de scoring — EN CURSO (iterando, NADA cerrado)

> **Estado: BORRADOR VIVO — estamos iterando.** NO es un diseño final ni un contrato.
> El dueño TODAVÍA está marcando las tandas; los pesos, los cortes y varias decisiones
> **pueden cambiar**. Esto es solo un **lugar único para no perder, mientras iteramos**:
> (a) las decisiones tomadas hasta ahora, (b) las preguntas abiertas, y (c) en qué
> **diverge del código actual** (§7). Se actualiza en cada vuelta. Recién cuando (y si)
> todo cierre, será contrato y superseder a `arbol-scoring-v1.md`. Hoy no es nada de eso.
>
> Detalle fino y datos: `docs/scoring/dataset-decisiones-estado-v1.md` (marcas del dueño
> + ajuste del modelo) y memoria engram (`apk-personal`, topics `scoring/*`).
> Fecha de inicio: 2026-06-06.

---

## 0. Por qué existe este documento

Durante la sesión del 2026-06-06 se replanteó el scoring **comportamiento primero,
fórmula después**: se escribieron historias de usuario, el dueño las marcó, y de ahí se
infirieron patrones y se ajustó un modelo de pesos contra esas marcas. En el proceso se
tomaron **muchas decisiones que el código actual NO implementa**. Este doc las hace
explícitas para que no se pierdan ni se confundan con lo que hoy corre.

Convención: 🟢 **decidido** · 🟡 **en calibración** (mecanismo decidido, número provisional)
· 🔴 **abierto** (falta decisión del dueño).

---

## 1. Principios (🟢 decididos)

1. **Comportamiento antes que fórmula.** La matemática se deriva de casos marcados por el
   dueño, no al revés.
2. **Pesos, no reglas.** El estado **emerge** de bases bien ponderadas + cortes. **Cero
   caps, gates o colapsos.** Si algo "pega fuerte" es porque **pesa**, no porque una regla
   lo fuerce.
3. **Score relativo a la meta, no absoluto.** Mide "¿sostenés TU compromiso?", no "¿quién
   hace más?". La meta es una **hipótesis ajustable** de tu base sostenible; la app ayuda
   a calibrarla (sugerir subir/bajar), no castiga la ambición.
4. **En marcha NO es un castigo.** Es "estás sosteniendo tu base, vas bien". El tono nunca
   humilla (ver `tono-comunicacion.md`).

---

## 2. La ventana de evaluación

- 🟢 **NUEVO: ventana móvil de 7 días.** Cada día evalúa `[hoy−6 … hoy]`. No hay "semana"
  ni reset. El estado responde "¿cómo venís en los últimos 7 días?".
- **Superhabit = intensidad reciente** (más veces por 7 días que tu ritmo-meta), no
  "superé un plan por período". Decae solo cuando el exceso sale de la ventana. Un ritmo
  estable nunca muestra superhabit.
- 🔴 **Usuario nuevo:** en los primeros 7 días de vida la ventana no está llena → prorratear
  por días-desde-config (pendiente de detallar).

---

## 3. El modelo de agregación (🟡 base validada 40/43)

Mide **CAPAS**, no actividades concretas. Cada capa = promedio de SUS anclas.

```
frac(ancla)   = min(hecho_en_7días / meta, 1)      # superávit no infla el estado base
Interior      = promedio(anclas de Interior)
Cuerpo        = promedio(anclas de Cuerpo)          # + sueño si está activo (ver §4)
Conducta      = promedio(anclas de Conducta)        # + sobriedad si está activa (ver §4)
Vínculos      = promedio(anclas de Vínculos)
Proyecto      = promedio(anclas de Proyecto)

score = (1−ω)·(Σ wL·capaL) + ω·min(capas portantes)
```

- **`min(capas portantes)`** = término suave de "peor capa", **solo sobre las capas
  portantes** (las marcas del dueño mostraron que abandonar Conducta no arrastra, pero
  abandonar Cuerpo/Interior sí). NO es un cap: castiga abandonar una capa, suave.
- **Cortes** (🟡 provisionales, sobre el score 0–1): `R<0.40 · A<0.64 · EM<0.84 · P≥0.84`.
- ⚠️ **Pesos por capa: provisionales.** Se ajustaron con 3 capas y **sueño siempre activo**
  (wCu=0.50, wI=wCo=0.25). Con sueño opt-in y 5 capas, la base se re-ancla SIN sueño
  (capas ~parejas) — calibración en curso (`historias-base-pura-5capas-v1.md`).

---

## 4. Moduladores de peso: sueño y sobriedad (🟢 son los ÚNICOS dos)

Regla: **nada modula el peso de una capa salvo estos dos.** Todo lo demás son capas
ponderadas planas.

### 4.1 Sueño (🟢 opt-in · 🟡 magnitud)
- **Opt-in.** Si el usuario no lo activa: neutro, Cuerpo = solo sus anclas, **no se
  penaliza** (ausencia de dato ≠ dormir mal).
- Si lo activa: el sueño es el **componente más pesado** y eleva el peso de **Cuerpo** (la
  capa más pesada del sistema). Dormir mal / no registrar baja el estado **por peso, no por
  cap**. Decisión del dueño: con sueño bueno, aflojar el ejercicio NO baja de Plenitud.
- 🔴 **Severidad:** no-dormir debería pesar más que dormir-mal, pero hoy no se mide con
  precisión → por ahora se tratan parecido (baja confianza).

### 4.2 Sobriedad (🟢 mecanismo · 🔴 escala · 🟡 magnitud)
- **Opt-in.** Cuando está activa, eleva el peso de **Conducta** sobre el Score.
- 🟢 **Vive en DOS escalas distintas** (corrección clave 2026-06-06):
  - **Semanal (lo que ve el estado):** "¿la **sostuviste** o se **rompió** (recaída) esta
    semana?" — NO un conteo de "limpio 7/7 días" como si fuera frecuencia de hábito.
  - **Largo plazo (la racha real, "47 días", meses):** es el logro. Pertenece a la feature
    de sobriedad + al futuro **sistema de rangos / memoria larga**, NO a la ventana de 7 días.
- 🔴 **Bifurcación abierta:** ¿la sobriedad afecta el **estado semanal** (vía Conducta,
  "sostuvo/rompió") o es **puramente de largo plazo**, separada del R/A/EM/P semanal?
- 🔴 **Recaída + racha larga:** ¿la racha larga **protege** (inercia ganada, compasión —
  "una señal, no una condena") o hace **caer más**? A marcar por el dueño.
- ⚠️ La tanda `historias-sobriedad-modulador-v1.md` quedó con el encuadre VIEJO (racha de
  7 días) → **se rehace** con el encuadre de largo plazo una vez resuelta la bifurcación.

---

## 5. Estados (🟢 vocabulario · 🟡 cortes)

`R` Restauración · `A` Atención · `EM` En marcha · `P` Plenitud · `I` Inquebrantable.

- El estado base (R→P) sale del score + cortes (§3).
- 🟢 **La peor capa NO colapsa el estado.** Una capa caída con el resto fuerte baja suave,
  nunca fuerza Restauración.
- 🟢 **Inquebrantable = superhabit que cubre TODAS las capas activas** (constancia repartida
  por todo el sistema), NO un gate de estabilidad de 5 semanas. 🟡 Umbral exacto de
  cobertura con 5 capas: en calibración (`historias-inquebrantable-cobertura-v1.md`).
- El "Inquebrantable probado en el tiempo" (largo plazo) se difiere al sistema de rangos.

---

## 6. Soportes y Tasks (🟡)

- **Soportes** (UX inversa, sin meta): alimentan el **valor** de su capa (ancla 0.80 /
  soporte 0.20). No modulan pesos.
- **Tasks** con capa: pequeño momentum al valor de la capa. No modulan pesos.
- Magnitud exacta: en calibración (`historias-soportes-tasks-v1.md`).

---

## 7. Tabla maestra de DIVERGENCIAS con el código actual

| Tema | Diseño NUEVO (v2) | Código HOY (viejo) | Dónde |
| --- | --- | --- | --- |
| **Ventana** | móvil 7 días | semana calendario (resetea lunes) | `WeeklyScoringContextBuilder.kt:11` |
| **Peor capa** | baja suave (término ω); NUNCA colapsa | `worst<0.30 → fuerza Restauración` | `BaseStatePolicy.kt:17` |
| **Sleep cap** | no existe; mal sueño baja por PESO | `sin sueño → topea en En marcha` | `BaseStatePolicy.kt:31` |
| **Peso del sueño** | ~50% de Cuerpo; Cuerpo la capa más pesada | sueño 30% de Cuerpo | `ScoringConstants.SLEEP_WEIGHT_IN_BODY=0.30` |
| **Sueño opt-in** | sí; sin sueño la base son capas parejas | parcial (ADR-3 no penaliza NoData) | `SpecialLayerScoringPolicy.kt` |
| **Agregación** | `Σ wL·capa + ω·min(portantes)`, pesos por capa | `0.75·avg(raw) + 0.25·worst(base)`, capas iguales | `WeeklyScorePolicy.kt` |
| **Escala / superávit** | el superávit tiene adónde ir | `base.coerceIn(0,1)` mata el superávit | `BaseStatePolicy.kt:14` |
| **Inquebrantable** | superhabit que cubre TODAS las capas | gate de estabilidad de 5 semanas | `BaseStatePolicy.kt:33-42` |
| **Sobriedad** | 2 escalas (semanal sostuvo/rompió + racha largo plazo) | 30% de Conducta, decay semanal de 7 días | `SobrietyScoringPolicy.kt`, `SOBRIETY_WEIGHT_IN_CONDUCT` |
| **Cortes** | `R<0.40 A<0.64 EM<0.84` (prov., S-space) | `R<0.40 A<0.70 P<0.85` sobre weeklyBaseScore | `ScoringConstants` |

> Nota: estas divergencias se implementan **recién cuando la calibración cierre**. Hoy NO
> se toca código (fase de diseño). Camino A de migraciones sigue vigente.

---

## 8. Estado de la calibración

| Dimensión | Estado | Fuente de marcas |
| --- | --- | --- |
| Estados base + cortes (3 capas, sueño on) | 🟡 validado 40/43 | lotes CB/REF/REFv2/REFv3 |
| Base pura sin sueño (3/4/5 capas) | ⏳ marcando | `historias-base-pura-5capas-v1.md` |
| Sueño modulador | ⏳ marcando | `historias-sueno-modulador-v1.md` |
| Sobriedad modulador | 🔴 rehacer (encuadre largo plazo) | `historias-sobriedad-modulador-v1.md` |
| Soportes / Tasks | ⏳ marcando | `historias-soportes-tasks-v1.md` |
| Inquebrantable (cobertura, 5 capas) | ⏳ marcando | `historias-inquebrantable-cobertura-v1.md` |

## 9. Decisiones abiertas (🔴 pendientes del dueño)

1. Sobriedad: ¿afecta el estado **semanal** o es **puramente largo plazo**?
2. Recaída + racha larga: ¿**protege** o **amplifica**?
3. Severidad sueño: no-dormir vs dormir-mal (atado a precisión de medición).
4. ¿Señal de "carga" separada para reconocer al usuario exigente sin distorsionar el estado?
5. Usuario nuevo: detalle de proración en los primeros 7 días.
