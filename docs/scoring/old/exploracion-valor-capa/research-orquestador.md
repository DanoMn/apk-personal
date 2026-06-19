# Research del orquestador — motor de valor de capa (2026-06-12)

> Mi investigación en paralelo a los 3 proponentes Opus. Herramientas matemáticas candidatas
> para la consolidación, mapeadas a las tensiones del modelo. NADA decidido — insumo para el merge.

## 1. El espectro de compensabilidad (la columna vertebral del problema)

Combinar sub-scores (capas, o componentes dentro de una capa) vive en un espectro entre dos extremos:

| Forma | Compensa una mala con una buena? | Nota |
|-------|----------------------------------|------|
| **Aritmético** `Σ wᵢxᵢ` | Sí, total | Lo que el dueño marcó para la BASE (promedio plano de capas). Sin cuello de botella. |
| **CES** `(Σ wᵢxᵢ^(-ρ))^(-1/ρ)` | Parcial, ajustable con ρ | El dial continuo. ρ→0 = geométrico; ρ→∞ = min; ρ=−1 = aritmético. "Preserva la intuición de cuello de botella siendo robusto al error de medición". |
| **Geométrico** (media) | Poco | HDI lo usa. Penaliza dimensiones bajas. Rechazado para superávit en el research del ancla (muy duro). |
| **Softmin** `−τ·log Σe^(−xᵢ/τ)` | Casi nada (cuello de botella suave) | El min() SUAVE: cuello de botella sin el acantilado del min duro que el dueño rechazó. τ controla la suavidad. |

**Lectura para el modelo:**
- El dueño YA marcó que la BASE de capas es **promedio plano** (aritmético, sin worst-term) — lote BP del dataset. Eso fija el agregado ENTRE capas como aritmético.
- Pero DENTRO de la capa con opt-in, el "efecto cimiento/dominó" del sueño (sesgo B) es justo un **cuello de botella suave**: CES con ρ chico o softmin entre la señal del opt-in y el bloque de anclas captura "si el cimiento falla, la capa cae" SIN el min duro ni la brutalidad del multiplicativo puro. Candidato fuerte para el sesgo B.
- El `min(r,1)` que topa el ancla en la base, y el `max(R−1,0)` del extra, ya son los "clamps de canal" — coherentes con dos canales.

## 2. Reward shaping / potential-based (respalda los DOS CANALES)

En RL, la práctica estándar para "premiar el extra sin romper la base" es **base + bonus aditivo acotado**:
`total = base + Σ shaping_i`, con el shaping saturado (diminishing returns) para que no domine.
Esto es EXACTAMENTE la estructura de dos canales aprobada: base ∈[0,1] + extra (bonus) saturado.
Refuerza el sesgo A (aditivo) y da la forma del extra: una suma con retornos decrecientes
(misma exponencial saturante `σ_max·(1−e^(−x/σ_0))` que ya usa el superávit del ancla).

## 3. Saturación para multi-soporte / multi-task / multi-sobriedad

La misma exponencial saturante del ancla resuelve los tres "multi-": N soportes no suman lineal,
saturan hacia un techo. Y para multi-sobriedad, el **producto** de factores (held=1, broke=b) evita
diluir el golpe de UNA recaída (a diferencia del promedio) — sin usar min().

## 4. Síntesis: qué llevar al merge

1. **Entre capas (base):** aritmético plano — ya marcado por el dueño. No tocar.
2. **Dentro de capa con opt-in (base):** evaluar CES/softmin (cuello de botella suave) vs aditivo vs
   mezcla — los proponentes A (aditivo) y B (cimiento no-lineal) cubren los extremos; el merge elige.
3. **Extra (superhabit):** bonus aditivo saturado (reward shaping + exponencial del ancla), solo anclas.
4. **Multi-* :** saturación exponencial; multi-sobriedad por producto.
5. **K relacional:** ortogonal a todo lo anterior — define los PESOS, no la forma de combinación.

Fuentes: [CES aggregation (Princeton)](https://swh.princeton.edu/~reddings/papers/AECES_Paper.pdf),
[CES production functions (ScienceDirect)](https://www.sciencedirect.com/science/article/abs/pii/0014292175900392),
[Reward shaping w/ trajectory aggregation (arXiv)](https://arxiv.org/pdf/2104.06163),
[Multi-level reward modeling (arXiv)](https://arxiv.org/pdf/2104.04748).
