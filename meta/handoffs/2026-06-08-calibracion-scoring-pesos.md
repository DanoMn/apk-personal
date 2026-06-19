# Handoff — Calibración del nuevo motor de scoring (5 tandas para marcar)

> **Estado: CONGELADO** (registro para la próxima sesión). Fecha: 2026-06-08.
> **Tipo:** calibración del rediseño de scoring (comportamiento → marcas del dueño →
> ajuste del modelo de pesos). NO se toca código todavía (fase de diseño, Camino A vigente).
> **Punto de entrada de la próxima sesión.**

## TL;DR — qué hacer en la próxima sesión

1. El dueño **marca las 5 tandas** que quedaron listas en `docs/scoring/historias-*` (con la panza, R/A/EM/P o P/I según la tanda).
2. Por cada tanda marcada: **destilar al dataset** (`dataset-decisiones-estado-v1.md`) y **reajustar el modelo** con el script (`scripts/scoring/weight_model_fit.py`).
3. Sacar el **modelo completo de 5 capas + moduladores**, validado contra las marcas.

Doc vivo con TODO el detalle + tabla de divergencias con el código: **`docs/scoring/rediseno-scoring-v2.md`**.

---

## El método (NO cambiarlo — es lo que está funcionando)

**Comportamiento antes que fórmula.** Se escriben historias de usuario concretas → el dueño
marca el estado con la panza → se destilan a un dataset terso → se ajustan los pesos con un
script de Python → se valida que reproduzca las marcas. Nada de números del motor en las
historias (sesgan al dueño). Skills: `scoring-historias-usuario` (escribir historias) y
`scoring-dataset-decisiones` (mantener el dataset AI-facing).

Para escribir tandas nuevas: delegar a un subagente sonnet con caso-por-caso explícito,
siguiendo la skill. Verificar siempre la salida (regla 10: sin números del motor; nombres
canónicos R/A/EM/P; una cajita; sin fila de sueño si es base pura).

---

## Las 5 tandas a marcar (`docs/scoring/`)

| Archivo | Casos | Qué mide | Marca | Estado |
| --- | --- | --- | --- | --- |
| `historias-base-pura-5capas-v1.md` | 11 (AP/AC/AN) | pesos PAREJOS de la base sin sueño + cortes con 5 capas; ¿alguna capa es especial sin sueño? | R/A/EM/P | ⏳ sin marcar |
| `historias-sueno-modulador-v1.md` | 9 (SU) | cuánto infla Cuerpo el sueño (opt-in activo) × rendimiento de anclas | R/A/EM/P | ⏳ sin marcar |
| `historias-sobriedad-modulador-v1.md` | 10 (SB) | racha LARGA × recaída × anclas | R/A/EM/P | ⚠️ marcada, **revisar** (ver abajo) |
| `historias-soportes-tasks-v1.md` | 7 (SO) | cuánto mueven soportes (UX inversa) y tasks vs anclas | R/A/EM/P | ⏳ sin marcar |
| `historias-inquebrantable-cobertura-v1.md` | 8 (IN) | umbral de COBERTURA para Inquebrantable con 5 capas | P/I | ⏳ sin marcar |

**Orden de procesamiento (por dependencia):** base-pura → sueño → sobriedad → soportes → inquebrantable.

---

## Decisiones FIRMES (NO reabrir)

- **Ventana móvil de 7 días** (no semana calendario). Superhabit = intensidad reciente; decae solo; no infla la base (frac topeado en 1.0).
- **Pesos, no reglas.** El estado EMERGE de bases ponderadas + cortes. **Cero caps, gates o colapsos** (fuera el sleep-cap, fuera el WORST_LAYER_COLLAPSE). Si algo pega fuerte es porque PESA.
- **Sueño opt-in.** Sin sueño activo: Cuerpo = solo sus anclas, no se penaliza. La base pura (sin sueño/sobriedad) tiene las **capas ~parejas**.
- **Sueño y sobriedad son los ÚNICOS dos moduladores de peso de capa** (sueño → Cuerpo, sobriedad → Conducta). Nada más modula pesos.
- **El sueño DOMINA Cuerpo.** Con sueño perfecto, aflojar el ejercicio (Caminar a la mitad) NO te baja de Plenitud. (Corrige las marcas B2/WC2 → Plenitud.)
- ⭐ **INQUEBRANTABLE = cumpliste el 100% + SUPERHABITS que cubren TODAS las capas activas.** Es un estado SEMANAL, de la ventana. **NO se gana "sostenido en el tiempo". NO es un rango aparte. NO se saca del estado semanal.** Esto se definió desde el inicio — no volver a marearlo.
- **Sobriedad — dos relojes:** (a) lo que ve el estado SEMANAL = "¿la sostuviste o se ROMPIÓ (recaída) esta semana?" (held/broke, NO conteo de 7 días); (b) la **racha larga** (meses) se premia **EN la feature de sobriedad** (contador de días, hitos) — **NO** en el estado semanal y **NO** con un rango nuevo. Consecuencia: una racha limpia con anclas al 100% pero **sin superhabit = Plenitud, no Inquebrantable**.
- **Worst-term** `0.2·min(Interior, Cuerpo)` = peso suave (Conducta no arrastra), NO una regla. Captura el castigo por abandonar una capa portante.
- **Score relativo a la meta**, no absoluto. La meta es una **hipótesis ajustable** (la app sugiere subir/bajar). **En marcha NO es castigo.** No inflar estados por ambición.

---

## El modelo BASE validado (🟡 provisional, 40/43 = 93%)

Ajustado con 3 capas y **sueño siempre activo** → es la instancia "sueño on", NO la base pura.
Los pesos por capa se **re-anclan sin sueño** con la tanda `base-pura` (ahí deberían salir parejos).

```
frac(ancla)   = min(hecho_en_7días / meta, 1)
Interior      = promedio(anclas de Interior)
Cuerpo        = 0.5·sueño + 0.5·promedio(anclas de Cuerpo)   # si sueño activo
Conducta      = promedio(anclas de Conducta)
score = (1−ω)·(wCu·Cu + wI·I + wCo·Co) + ω·min(Interior, Cuerpo)

wCu=0.50  wI=0.25  wCo=0.25  ω=0.20
cortes:  R<0.40   A<0.64   EM<0.84   P≥0.84
sueño mal/ausente = 0.15
```

3 fallos irreducibles (no son falla del modelo): **T04** (inconsistencia del dueño con RS2),
**RS1** (penaliza desbalance del superávit, el modelo capea superávit), **A1** (borde 50%).

Marcas YA destiladas al dataset esta sesión: lotes **SH, CB, REF, REFv2, REFv3**.

---

## ⚠️ Revisar la tanda de sobriedad ya marcada

El dueño la marcó pero con el encuadre de Inquebrantable confundido. Con la definición firme
(Inquebrantable = superhabits, semanal):
- **SB2/SB3** → marcadas "Inquebrantable" por la racha larga. **Deberían ser Plenitud** (no hay superhabit). La racha se premia en la feature, no en el estado.
- **SB9** (Interior+Cuerpo perfectos, solo Orden digital floja, 6 meses limpio) → marcada **R**. Es un **outlier**: tiene mejores anclas que SB7 (que es A) con la misma racha. Probable error — re-marcar.
- Falta resolver: en una **recaída**, ¿la racha larga **protege** (cae menos) o **amplifica** (cae más)? Las marcas SB4/5/6 dieron todas EM (amplifica). Confirmar contra la filosofía "una señal, no una condena".

---

## Decisiones ABIERTAS (🔴 pendientes del dueño)

1. Sobriedad en recaída: ¿la racha larga protege o amplifica?
2. Severidad del sueño: no-dormir vs dormir-mal (atado a precisión de medición; hoy se tratan parecido).
3. Usuario nuevo: proración en los primeros 7 días de vida de la cuenta.
4. ¿Señal de "carga" separada para reconocer al usuario exigente sin distorsionar el estado? (opcional)

---

## Referencias

- **`docs/scoring/rediseno-scoring-v2.md`** — bitácora viva del rediseño + **§7 tabla de divergencias con el código** (archivo por archivo qué cambiar al codear). Punto de entrada conceptual.
- **`docs/scoring/dataset-decisiones-estado-v1.md`** — dataset AI-facing: marcas destiladas + el modelo + patrones. Es lo que se LEE para inferir (no las historias grandes).
- **`scripts/scoring/weight_model_fit.py`** — el ajuste del modelo contra las marcas (correr con `python3`).
- Skills: `.claude/skills/scoring-historias-usuario/`, `.claude/skills/scoring-dataset-decisiones/`.
- Memoria engram (`apk-personal`), topics `scoring/*`: filosofía pesos-no-reglas, modulación sueño/sobriedad, modelo base validado, ventana móvil, sobriedad largo plazo, etc.
- Código VIEJO (NO tocar aún): `app/src/main/java/dev/panopt/autonomia/domain/scoring/*Policy.kt`. Implementa el modelo anterior; las divergencias se aplican recién cuando cierre la calibración.

## Salida esperada de la próxima sesión

1. Las 5 tandas marcadas y destiladas al dataset.
2. Modelo de pesos de 5 capas + moduladores (sueño, sobriedad) + soportes/tasks, validado contra las marcas con el script.
3. Cortes y umbral de cobertura de Inquebrantable confirmados.
4. Recién entonces: evaluar pasar de diseño a implementación (aplicar la tabla de divergencias §7).
