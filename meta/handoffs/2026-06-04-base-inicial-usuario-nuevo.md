# Handoff — Base inicial del usuario nuevo (amortiguación / onboarding de hábitos)

> **Estado: CONGELADO** (registro para una sesión futura). Fecha: 2026-06-04.
> **Tamaño estimado:** grande (toca scoring + rediseño de onboarding). Va en sesión propia.

## El problema (síntoma observado)

Un usuario nuevo, apenas marca su **primer hecho** (una ancla hecha, un log), cae al
**estado más bajo (Restauración / "base baja")**. Tiene sentido técnico —solo hay datos de un
día— pero es una mala primera experiencia: la app lo recibe con un "reproche", lo que contradice
el tono (`AGENTS.md`: "no humilla, no moraliza"). Además la frase ancla pasa a familias de estado
bajo. El usuario espera arrancar neutro o "en marcha", no negativo.

Mecánica confirmada (`ScoreEngine.kt:12-17`): `NoData` dura **solo mientras hay cero hechos**.
Apenas hay un hecho → se calcula el estado real → con poca data positiva, es el más bajo.

## Lo que YA está decidido en doc pero NO implementado (clave)

`docs/scoring/plan-tecnico-scoring.md` **§2.1.1 "Amortiguación inicial"** (líneas ~182-204) — es
una **Decisión aprobada** (tabla de decisiones, ~línea 114) que **nunca se implementó**:

```text
Contrato de amortiguación inicial:
- dura una semana;
- solo afecta la lectura de onboarding/primera lectura, NO los hechos;
- no crea registros falsos;
- calcula el score técnico bruto desde hechos reales;
- suaviza la lectura visible hacia `En marcha` como estado intermedio;
- desaparece al cerrar la primera semana completa.

Regla operativa:
  rawWeeklyReport = cálculo normal desde hechos reales
  initialAmortizedState = max(rawState, En marcha) durante la primera semana
```

Verificado: `ScoreEngine.calculate` devuelve `rawState` directo (vía `BaseStatePolicy.stateFor`);
no hay rastro de `initialAmortizedState` ni ventana inicial. **Implementar esto solo ya resolvería
el síntoma** sin tocar el onboarding.

## La idea EXPANDIDA del usuario (esta sesión) — a reconciliar

El usuario propone algo más rico que la amortización simple: hacer el **onboarding interactivo**
para construir una "base falsa" de semana previa a partir de hábitos declarados:

1. Preguntar **"¿cuáles de estas actividades sueles hacer más en tu semana?"** — lista de
   genéricas frecuentes (ej. ejercicio, meditar, yoga, escribir...), que sumen varias capas;
   exigir **mínimo 3** (en 3 capas distintas, coherente con el gate `MIN_ACTIVE_LAYERS_WITH_ANCHOR`).
2. Preguntar **"¿con qué frecuencia lo haces?"** (frecuencia actual, no objetivo) → rellena la meta.
3. Segundo menú: **"¿continuás con esas mismas actividades o querés elegir nuevas?"**
   - Si "nuevas": el usuario reselecciona anclas + metas semanales/mensuales, y lo declarado antes
     se usa como **base falsa de la semana anterior** (datos "como si los hubiera hecho al 100%").
   - Si "mismas": esas son sus anclas + base inicial.

> ⚠️ Esto **choca/complementa** con la amortización aprobada (§2.1.1). Hay que decidir:
> (a) implementar solo la amortización (simple, ya aprobada), o
> (b) el onboarding de hábitos + base falsa de semana previa (más complejo, idea nueva), o
> (c) ambos. La amortización NO crea registros falsos; la base-falsa SÍ los simula. Son filosofías
> distintas — reconciliarlas es el primer paso de diseño de esa sesión.

## Edge cases nuevos que esto destapa (anotados por el usuario)

- El score se calcula **en tiempo real** desde hechos. Si la base inicial es "falsa", ¿cómo
  conviven esos datos simulados con el recálculo en vivo? ¿Se persisten como hechos o solo como
  lectura amortizada? (La amortización §2.1.1 dice NO crear registros falsos — la idea del usuario
  sí; ahí está la tensión a resolver.)
- Falta edge case para "el usuario cambia sus anclas después de declarar la base".

## Estado actual del onboarding (contexto)

El onboarding de introducción YA existe y fue implementado/archivado (no contemplaba este paso):
- Diseño conceptual: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md`
- SDD archivado: `openspec/changes/archive/` (cambio `onboarding-introduccion`), ver `meta/pendientes.md`
- Handoff UI: `meta/handoffs/handoff-ui-onboarding.md`
- Flujo actual (verificado en emulador 2026-06-04): intro → "¿Qué te trae aquí?" (intención) →
  "Tus anclas" (elegir ≥3 en 3 áreas) → "El descanso primero" (sueño) → "Tus cimientos están en pie".
  **No hay** paso de frecuencia actual / base previa.

## Referencias

- `docs/scoring/plan-tecnico-scoring.md` §2.1.1 (amortiguación), tabla de decisiones (~L114), modos
  de lectura (~L1357, L1457).
- `docs/scoring/arbol-scoring-v1.md` — contrato matemático (umbrales de estado).
- `app/.../domain/scoring/ScoreEngine.kt` (gate NoData L12-17), `BaseStatePolicy.kt` (estados),
  `WeeklyScoringContextBuilder.hasAnyFact` (qué cuenta como hecho).
- Onboarding: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md`, `handoff-ui-onboarding.md`.

## Nota menor (no es esta sesión, pero relacionada)

El usuario observó que en "Mis anclas" hay actividades que "no deberían estar" / no parece el
template original — posible drift del seed/catálogo (`DefaultSeeds.kt` vs docs en `docs/datos-room/`).
Revisar aparte si el catálogo genérico coincide con el canónico.
