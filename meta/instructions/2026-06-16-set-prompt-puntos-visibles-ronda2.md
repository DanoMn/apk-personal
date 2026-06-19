# Set-prompt RONDA 2 — mapeo ESTADO→puntos (sesgos no-triviales)

> Segunda ronda de Opus para el mapeo ESTADO [0,1.5] → puntos. La ronda 1 (A fiel a la idea del dueño /
> B lineal / C experiencia) está en `docs/scoring/exploracion-puntos-visibles/`. El dueño RECHAZÓ el
> lineal trivial ("sumar 100 en todos lados no tiene gracia"). Esta ronda explora distribuciones
> INTELIGENTES de la resolución. Fecha: 2026-06-16.

## Marco fijo (confirmado por el dueño)
- Input: ESTADO ∈ [0, 1.5]. Cortes: R<0.40 · A<0.62 · EM<0.85 · P<1.10 · I≥1.10 (Plenitud entra en
  0.85; cumplir-justo=1.0 cae en zona alta de Plenitud).
- Output: puntos. **Piso DIGNO** (NO 0 — no humillar; ~600-700). **Tope = 1100 (CONFIRMADO).**
- El **"1000" debería ganarse con superhabit** (≈ entrada de Inquebrantable), idea de la ronda 1 que gustó.
- **PROHIBIDO el lineal trivial/uniforme.** El valor está en CÓMO se distribuye la resolución.
- Tensión a resolver: motivar TANTO la recuperación (que el número se mueva al salir del pozo) COMO el
  esfuerzo (que se mueva al empujar a la cima), de forma inteligente, no plana.
- Continuo, monótono, Inquebrantable visible, cortes en números memorables donde se pueda.

## Sesgos de esta ronda
- **OPUS D — doble énfasis (curva en S):** máxima resolución en AMBOS extremos (salir del pozo + tramo
  final a Inquebrantable), plano en el medio (zona estable). Resuelve la tensión de raíz.
- **OPUS E — hitos psicológicos / metas perseguibles:** diseñar alrededor de números-meta memorables que
  el usuario persigue; que cruzar cada umbral "se sienta". El número como objetivo, no como termómetro.
- **OPUS F — psicofísica (Weber-Fechner):** el movimiento PERCIBIDO de un número no es lineal (un +50
  pesa más sobre 600 que sobre 1000). Diseñar para que el cambio PERCIBIDO sea estratégico (log/exp).
