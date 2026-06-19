# Handoff — Replanteo conceptual del motor de scoring (ventana móvil de 7 días)

> **Estado: CONGELADO** (registro para una sesión futura). Fecha: 2026-06-04.
> **Tipo:** rediseño conceptual del motor de scoring (NO se toca código todavía;
> primero se cierra el concepto). Grande. Marco maestro que reencuadra los handoffs
> `2026-06-04-base-inicial-usuario-nuevo.md` y `2026-06-04-edge-cases-scoring.md`.

## Por qué este handoff

El usuario reportó dos síntomas: (1) el scoring **se reinicia cada lunes** en vez de
sentirse continuo, y (2) un **usuario nuevo** que marca su primera ancla cae al estado
más bajo. Al investigar, se concluyó que la raíz NO es un bug de suma sino **falta de
definición conceptual**: se escribieron fórmulas antes de definir el comportamiento
esperado. Se decidió **replantear el motor de forma conceptual primero**, aterrizarlo a
código mucho después.

El artefacto vivo de este trabajo es **`docs/scoring/escenarios-estado-esperado-v1.md`**
(borrador en construcción). Este handoff resume el estado del replanteo.

## Diagnóstico confirmado (en código + contrato)

1. **El motor actual es 100% semana-de-calendario.** `WeeklyScoringContextBuilder.kt:11`
   fija `weekStart = previousOrSame(MONDAY)`. El estado lo decide el `WeeklyBaseScore`
   de la semana en curso. Cada lunes `doneDays` vuelve a ~0 → colapso. El único elemento
   cross-week (`StabilityScore`) requiere 5 semanas y solo habilita `Inquebrantable`;
   nunca levanta el estado visible.
2. **Sin proración por días transcurridos.** `AnchorScoringPolicy.kt:20`:
   `frequencyRatio = doneDates.size / targetDays`, con `targetDays` = frecuencia semanal
   COMPLETA. Al inicio de semana el máximo posible es 1/targetDays → arrastra la capa
   bajo `WORST_LAYER_COLLAPSE (0.30)` → fuerza Restauración. Castiga el arranque de cada
   semana y al usuario nuevo.
3. **La amortiguación inicial (`plan-tecnico-scoring.md` §2.1.1) se diseñó y NUNCA se
   implementó** (grep en `app/src/main/java` → 0 resultados).

## Decisiones TOMADAS esta sesión (firmes)

1. **Ventana móvil de 7 días** (rolling window), NO semana de calendario. Cada día la
   ventana es `[hoy−6 … hoy]`. Analogía del dueño: app de finanzas ("lo que gastaste en
   los últimos 30 días"). Mata el reset del lunes Y la proración rota de un solo tiro.
2. **Los CINCO estados salen SOLO de la ventana de 7 días** (Capa 1). Sin memoria larga
   por ahora.
3. **Inquebrantable**: por ahora es el tramo superior (superávit). El Inquebrantable
   "probado en el tiempo" se **DIFIERE** a un futuro **sistema de rangos acumulativos
   estilo CS:GO** (Capa 2, fuera de alcance ahora).
4. **NO botar el motor entero.** La estructura por capas (anclas → capa → score) se
   reutiliza. Cambian: la **ventana** (calendario → 7 días) y el **mapeo a estados**
   (umbrales). Es refactor focalizado, no demolición.
5. **Método: comportamiento antes que matemática.** Se define un documento de escenarios
   (estado esperado por caso); la fórmula se deriva de ahí, no al revés.
6. **Principio rector de los superávits — "la constancia es el esqueleto, el superávit es
   músculo; no hay músculo sin esqueleto".** El estado base lo define SOLO la constancia
   (frecuencia). El superávit es un modificador **subordinado y acotado**: solo agrega
   altura cuando la base ya es sólida (de Plenitud hacia Inquebrantable); **nunca rescata
   desde abajo**. Caso resuelto: 3/5 actividades con mucho superávit → **Atención**, no
   Plenitud (le falta constancia).

## Propuesta de umbrales (sin cerrar)

El % que mapea a estado = fracción del plan cumplida en la ventana de 7 días (modelo
simple: 3 anclas, solo frecuencia). NO es lineal cuando se reintegre valor/peor-capa.

| Estado | v1 dueño | v2 razonada (Claude) |
| --- | --- | --- |
| Restauración | 0–20% | 0–25% (fondo solo para abandono real) |
| Atención | 20–60% | 25–65% |
| En marcha | 60–80% | 65–85% (sostener la base = clara mayoría) |
| Plenitud | 80–100% | 85–100% (con holgura) |
| Inquebrantable | superávit | por encima del 100% → depende de la escala (ver abajo) |

## PROBLEMAS ABIERTOS (a resolver en la próxima sesión)

En orden de ataque recomendado:

1. **Cerrar los umbrales de los 4 estados base (solo constancia).** Dos palancas:
   (a) el fondo de Restauración (`<25%` indulgente vs más alto); (b) dónde arranca En
   marcha (`65%` exige clara mayoría vs `~55-60%` la mitad-largo). La banda de Atención
   sale de estas dos.
2. **Decidir la escala.** ¿Cerrada `0–100` (cumplir el plan es el techo) o abierta y
   ACOTADA `0–~120` (hay un "más allá del 100%" para el superávit, con rendimientos
   decrecientes/saturación)? Recomendación de Claude: **abierta acotada (B)** — da el
   superávit que el dueño quiere sin que un día gigante rompa la escala, y respeta
   "constancia, no acumulación".
3. **Definir cómo entra el superávit (subordinado).** Aplicar el principio rector:
   resolver formalmente que baja constancia + alto superávit NO sube de estado. Ubicar
   Inquebrantable.
4. **¿Plenitud se gana cumpliendo o superando?** Decisión pendiente; define si el 100%
   exacto es Plenitud y el superávit es Inquebrantable, o si Plenitud ya exige superávit.
5. **Reintegrar (después de cerrar lo de arriba):** valor/intensidad (hoy freq 70% /
   valor 30%), peor-capa (25%), sueño (30% de Cuerpo), sobriedad (30% de Conducta). El
   replanteo conceptual se hizo con 3 anclas; falta reconciliar el motor completo.
6. **Mucho más adelante:** memoria larga / arrastre = sistema de rangos CS:GO (Capa 2),
   donde vive el Inquebrantable temporal.

## Cómo reencuadra los otros handoffs de hoy

- **`base-inicial-usuario-nuevo.md`**: con la ventana móvil, el castigo al usuario nuevo
  se reduce a un caso ACOTADO (prorratear por días-desde-config solo en los primeros 7
  días de vida de la cuenta). Esto puede hacer innecesaria la amortiguación §2.1.1 — a
  reconciliar. La idea del "onboarding de hábitos / base falsa" sigue siendo decisión
  aparte.
- **`edge-cases-scoring.md`**: el cambio de ventana (de calendario a móvil) cambia el rol
  de los snapshots semanales y del recálculo; revisar esas preguntas con el nuevo modelo.

## Referencias

- **`docs/scoring/escenarios-estado-esperado-v1.md`** — artefacto vivo de este replanteo
  (escenarios, modelo de ventana móvil, tabla de umbrales, principio de superávits, ideas
  a futuro). **Punto de entrada de la próxima sesión.**
- `docs/scoring/arbol-scoring-v1.md` — contrato matemático del motor VIEJO (§16 estados
  sellados, §16.6 asimetría superávit). Útil como referencia de qué cambia.
- `docs/scoring/plan-tecnico-scoring.md` §2.1.1 (amortiguación inicial nunca implementada).
- Memoria Engram (`apk-personal`), topic_keys:
  `scoring/diagnostico-reset-lunes-y-usuario-nuevo`,
  `scoring/metodo-escenarios-antes-que-formula`,
  `scoring/modelo-ventana-movil-7-dias`,
  `scoring/alcance-actual-solo-capa-1`.
- Código clave: `domain/scoring/WeeklyScoringContextBuilder.kt` (ventana, punto de cambio),
  `AnchorScoringPolicy.kt` (frecuencia/valor/superávit), `BaseStatePolicy.kt` (estados),
  `ScoreEngine.kt` (orquestador, gate NoData).

## Salida esperada de la próxima sesión

1. Umbrales de los 4 estados base CERRADOS (constancia pura).
2. Escala decidida (cerrada vs abierta-acotada) y tratamiento del superávit definido.
3. Escenarios del documento con su estado esperado resuelto (los ⚠️).
4. Recién entonces: empezar a reconciliar el motor completo (valor, peor-capa, sueño,
   sobriedad) sobre el nuevo modelo.
