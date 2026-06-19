# Futuro — Feedback diario motivacional (idea, NO implementar todavía)

> **Estado: IDEA FUTURA / CONGELADO.** Esto NO es contrato vigente ni un doc vivo.
> Es un registro de una exploración de diseño (sesión 2026-06-04) que se decidió
> **posponer**. No tocar el scoring ni el dashboard en base a este documento.
> Cuando se retome, primero revalidar contra el código y el tono vigentes.

## De dónde salió

Explorando el problema "usuario nuevo cae a Restauración el día 1" surgió la idea
de darle al usuario una **sensación de progreso diario** que el estado de base
(semanal, lento) no transmite. La conversación derivó hacia diseñar una feature de
feedback diario. Al revisar el dashboard real, se concluyó que **gran parte ya
existe** y que la feature no es el núcleo del problema → se pospone.

## Por qué se pospone (lo que ya existe en el dashboard)

El dashboard actual (`ui/dashboard/DashboardScreen.kt`) ya tiene:

- **`StatusCard`** — estado de base (pill con color + headline + orbe con número
  700-1000). El reloj **lento** (semanal). Es el corazón.
- **`DailyProgressCard`** ("Progreso de hoy") — porcentaje + barra coral +
  "N de M capas activas". Ya es un reloj **rápido** diario: sube con cada marca.
- **`AnchorPhraseCard`** — frases ancla motivacionales.

Es decir: el dinamismo diario (DailyProgressCard) y el aliento (frases) **ya
están**. Cualquier feature nueva debe justificar por qué no es redundante con esto.

## La idea, si se retoma

Núcleo conceptual (no un "medidor de productividad" — eso choca con el tono):
mostrar **en positivo** lo que el usuario ya tocó hoy, como reconocimiento, no como
medición de lo que falta. Conceptos explorados:

1. **Espejo de la base** — qué capas tocó hoy (parcialmente cubierto por
   `DailyProgressCard.activeLayersLabel`).
2. **Reconocimiento del gesto** — afirmación al marcar (parcialmente cubierto por
   frases ancla).
3. **Invitación suave** — ofrecer una acción mínima disponible (cubierto por el
   tono de las frases).

### Restricción dura descubierta (mecánica de datos)

No existe un "estado del día" en tiempo real. `closeActivityDay`
(`AutonomiaRepository.kt`) cierra desde el lunes **hasta ayer**, nunca hoy. Durante
el día solo existe lo que el usuario marcó como **Done**; lo no marcado **no es
fallo** (no es `NotDone` hasta el cierre de medianoche). Por eso una franja diaria
**no puede** mostrar "lo que falta" como déficit — solo puede comunicar en positivo
lo ya hecho. (Ver memoria engram: "no existe estado del día en tiempo real".)

### Insumo de research (UX compasiva)

- Los usuarios se desenganchan cuando el seguimiento se siente **evaluativo**;
  conviene **reflejar/reconocer**, no medir.
- Lo que funciona: orientar, dar tranquilidad, "celebrar lo pequeño".
- Evitar mecánicas de pérdida/daño (estilo Habitica) — contradicen el tono.

## Relacionado (problema estructural mayor, tampoco resuelto)

El score arranca bajo **cada lunes** (ratio = días cumplidos esta semana / meta
semanal completa), no solo en la primera semana de uso. Si alguna vez se ataca,
es un cambio al contrato matemático (`arbol-scoring-v1.md`) y va en su propia
sesión. Ver memoria engram: "dos relojes de scoring".
