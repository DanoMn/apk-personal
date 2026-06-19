# Simulación multi-capa — resultados v2 (N = 3..5, 1 ancla por capa)

> Verificación posterior al merge. Script: `scripts/scoring/anchor_engine_sim.py` (determinista,
> reproducible). **v2 (2026-06-10) — corrección metodológica del dueño:**
> 1. **Sin reglas de cobertura.** Ningún estado —Inquebrantable incluido— se define por reglas tipo
>    "≥2 capas en superávit". El score = promedio ponderado de las capas activas (pesos `1/N`), y el
>    estado es **lo que el promedio da**. δ es un corte de banda calibrable igual que 0.40/0.62/0.85,
>    NO una herramienta para exigir cobertura. (La v1 de este doc enmarcaba "δ vs N" como decisión de
>    cobertura — eso era pensamiento de reglas y queda retirado.)
> 2. **N = 3..5.** Mínimo 3 capas activas = el ÚNICO axioma duro del motor. Máximo canónico 5.
>    (El stress N=6..8 de la v1 queda retirado.)
> 3. **1 ancla por capa** en toda la simulación.
> 4. **Las 45 marcas históricas NO son ground truth.** Sirvieron para descubrir qué buscar en el
>    modelo matemático; la verdad ahora son los axiomas. No se re-valida contra ellas.
>
> Sin sueño, sobriedad, soportes ni tasks. Parámetros ilustrativos
> (`γ=1.5, λ_v=0.5, κ=1.5, p=2, σ_max=0.5, σ_0=0.5, δ=0.10`) — SIN calibrar.
>
> **Aclaración de lectura:** todos los mundos tienen N ≥ 3 capas. Los "2" que aparecen en salidas
> son: `k` (cuántas de las N capas están en cierto perfil), `F=2` (frecuencia objetivo de un ancla)
> — nunca un mundo de 2 capas.

---

## 1. Parte A — verificación determinista de la fórmula del ancla

**9.876 checks totales, 0 fallas.** Cobertura:

- **A1** (rango `[0, 1+σ_max]`): grilla de stress F=1..7 × T∈{5,30,120} × 7 semanas tipo.
- **A2** (D=0 ⟹ 0) y **A3** (cumplimiento exacto = 1, error < 1e-12) para todo F y T∈{1..900}.
- **A4** (agregar un día nunca baja): 12 semanas base × 7 tiempos extra × F=1..7.
- **A5 versión fuerte** (más minutos en CUALQUIER día nunca baja — incluye la promoción
  voluntario→compromiso): barridos de 0.5 a 150 min, paso 0.5, sobre 6 contextos × 5 F.
- **A7** (voluntario ≥ 0, tiende a 0): ε de 10 a 0.001 min, aporte monótono decreciente → 0.
- **A9** (continuidad): salto máximo en todos los barridos finos = **0.0154**. Sin gates.
- **A10** (invarianza de escala): ×0.5, ×2, ×4, ×10 sobre 5 configuraciones — igualdad < 1e-9.
- Los 12 casos de comportamiento (§7 del merge + testigo del dueño + ráfaga absurda): OK.
- **P2**: mismo superávit de tiempo (r=2, base completa) crece monótono con F:
  `1.13 → 1.21 → 1.29 → 1.35 → 1.40 → 1.43` (F=2..7).

### 1b. Batería ampliada A2 (casos límite del ancla)

| Caso límite | Resultado | Lectura |
|-------------|-----------|---------|
| Permutación del array | idéntico para todo orden | solo importa el multiset de tiempos |
| Días en 0 agregados | no-op exacto | `[30,30,0,0] ≡ [30,30]` |
| Empate en la frontera Best-F | Δ < 0.001 | sin salto al intercambiar compromiso/voluntario |
| Inputs insensatos (`[9000×7]`) | R ≤ 1.5 siempre | techo `1+σ_max` inviolable |
| F=1 exacto / F=1 + 6 voluntarios plenos | 1.0000 / **1.4246** | a F mínimo el superávit es por días |
| F=7 con 6/7 días · 3/7 días | **0.857** · **0.429** | cada día faltante pesa; nada lo tapa |
| Mismo total 90 min (F=3,T=30) | `[90]`=0.35 < `[45,45]`=0.71 < `[30,30,30]`=1.00 | constancia con total fijo: más días gana |
| Frecuencia llena, mismo total | parejo 1.00 > desparejo `[20,30,40]` 0.87 | el superávit de un día NO paga el déficit de otro |
| Voluntario repara déficit de tiempo | `[30,30,10]` → `+[10]` sube | reparación con retornos decrecientes |
| Cruce del target (29.5→30.5) | Δ = 0.0099 | la frontera r=1 no salta |
| T extremos del dominio (1 y 900 min) | exacto = 1 en ambos | invarianza en los bordes reales |

**Tabla r\*** — factor de tiempo uniforme mínimo (D=F, solo vía tiempo) para que el ancla exporte `≥ 1+δ`:

| F | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|
| r* | 3.07× | 1.73× | 1.40× | 1.26× | 1.19× | 1.14× | **1.11×** |

Decrece monótono — P2 en números: a compromiso más alto, menos esfuerzo extra relativo por sesión.

**Hallazgos de calibración del ancla (estructura OK, magnitud a marcar por el dueño):**

1. **γ acopla anti-trivialidad con reparto intra-compromiso:** 2/4 días plenos = 0.500 vs 4/4 días a
   mitad de tiempo = 0.354 (con γ=1 serían iguales). ¿Profundidad o presencia parcial? → marcar.
2. **Un solo día voluntario pleno exporta `≥ 1+δ` para todo F** (base exacta). Palancas: `σ_0`, `δ`.

## 2. Perfiles usados en la Parte B (1 ancla = 1 capa)

| Perfil | Config | R |
|--------|--------|---|
| `EXACTO` | F=3, [30,30,30] | 1.0000 |
| `SUP_MAX` | F=7, [60×7] | 1.4323 |
| `SUP_MED` | F=4, [45×4] | 1.1754 |
| `SUP_DIAS` | F=2, [30×4] | 1.2461 |
| `LEVE_DEF` | F=3, [30,30,20] | 0.8481 |
| `DEBIL` | F=5, [60,60] | 0.4495 |
| `CERO` | [] | 0.0000 |
| `TRIVIAL` | F=2, [30,30,1,1,1] | 1.0031 |

## 3. Parte B — el motor con N = 3..5 capas (enumeración exhaustiva)

Toda combinación posible de perfiles por capa (con repetición): 120 mundos a N=3, 330 a N=4,
792 a N=5. **Distribución de estados emergente:**

| Estado | N=3 | N=4 | N=5 |
|--------|-----|-----|-----|
| Rojo | 5.8% | 3.9% | 2.9% |
| Amarillo | 10.0% | 11.2% | 10.1% |
| En marcha | 25.0% | 23.6% | 25.3% |
| Pleno | 34.2% | 39.1% | 42.4% |
| Inquebrantable | 25.0% | 22.1% | 19.3% |

### Lo que EMERGE (descripción, no reglas)

- **El Inquebrantable más justo a N=3 es `SUP_MAX + SUP_MAX + DEBIL` = 1.1047.** Dos capas muy
  fuertes pueden cargar una débil hasta Inquebrantable. Nadie lo legisló: es el promedio. Si al
  dueño este mundo no le parece Inquebrantable cuando lo vea con datos reales, las palancas son los
  parámetros del ancla y los cortes de banda — nunca una regla de capas.
- **A N=4 el más justo es `EXACTO + EXACTO + SUP_MED + SUP_DIAS` = 1.1054** — dos capas cumplidas
  justas + dos con superávit moderado. Inquebrantable sin ninguna capa heroica.
- **A N=5, todos los mundos Inquebrantable resultaron tener ≥ 2 capas con valor > 1** — es una
  consecuencia aritmética (el superávit máximo por ancla, 1.43, no alcanza para levantar 4 capas en
  1.0), no una regla. El más justo: `SUP_DIAS×2 + TRIVIAL×3` = 1.1003.
- **El que más cerca quedó sin entrar** (N=5): `EXACTO + SUP_DIAS×2 + TRIVIAL×2` = 1.0997 (Pleno) —
  a 0.0006 del corte. Continuidad pura: no hay acantilados alrededor de la banda.
- A más capas activas, Inquebrantable es proporcionalmente más exigente (25% → 19% de los mundos):
  diluir el superávit entre más áreas de vida sube la vara sola.

### Otros comportamientos del motor (descriptivos)

- **Pleno exacto nunca es Inquebrantable:** todas las capas cumplidas justas = 1.0000, para todo N.
- **k capas en SUP_MAX, resto exacto:** N=3: k=1 ya da 1.144 (I) · N=4: k=1 da 1.108 (I) ·
  N=5: k=1 da 1.086 (P), k=2 da 1.173 (I). Es solo el promedio.
- **Colapso progresivo** (j capas en CERO, resto exacto): degradación lineal y monótona, sin saltos
  — N=3: 1.00/0.67/0.33/0.00 · N=5: 1.00/0.80/0.60/0.40/0.20/0.00.
- **Una capa floja, resto exacto:** DEBIL → En marcha a N=3, Pleno desde N=4; CERO → En marcha en
  todo el rango 3..5; LEVE_DEF → Pleno siempre. La dilución por N es gradual.
- **El superávit no fabrica base:** migajas en todas las capas (TRIVIAL uniforme) = 1.0031 Pleno,
  no Inquebrantable; dos capas fuertes + una muerta = 0.955 Pleno.
- **Bordes de banda:** semántica `≥` verificada exacta en 0.40 / 0.62 / 0.85 / 1+δ.
- **Propiedad del motor (cerrada, registrada):** la dispersión entre capas es invisible al promedio
  (`[1.2, 0.8, 1.0] ≡ [1.0, 1.0, 1.0]`). Consecuencia de pesos puros; no es un bug y no se "arregla"
  con reglas.

## 4. Qué queda para adelante

1. **Calibración** de `γ, λ_v, κ, p, σ_max, σ_0` y de los CUATRO cortes de banda (δ incluido, como
   un corte más) — contra datos/sensación del dueño, no contra las 45 marcas históricas.
2. **Próxima sesión (definida por el dueño):** axiomas de comportamiento de una CAPA respecto a
   opt-ins (sueño, sobriedad), soportes y tasks — el mismo método que funcionó para el ancla.
3. Testeo automatizado formal (red team, barridos de parámetros) si el dueño lo decide.

**Reproducir:** `python3 scripts/scoring/anchor_engine_sim.py`
