# Merge consolidado — la fórmula del rendimiento de un ancla

> Fase 2 de la sesión de exploración (2026-06-09). El orquestador leyó las 3 propuestas
> (A: dominancia fuerte · B: acoplamiento suave · C: saturación) y los 3 research
> (1: métricas acopladas · 2: saturación · 3: pesos desplazables), y consolidó.
> **Estado: propuesta consolidada para lectura del dueño. Sin calibrar. Sin código.**

---

## 1. El hallazgo central: convergencia independiente

Los tres proponentes, trabajando a ciegas con sesgos opuestos, convergieron en **cuatro decisiones
estructurales idénticas**. Cuando tres derivaciones independientes desde los mismos axiomas llegan a
la misma estructura, eso no es coincidencia: es la forma del problema.

1. **La base es un esqueleto de F slots.** El cumplimiento se promedia sobre `F` (lo comprometido),
   no sobre `D` (lo hecho). Un día faltante es un cero dentro del promedio **que el tiempo no puede
   rellenar**. Acá vive "frecuencia sobre intensidad" — estructural, no paramétrico.
2. **El superávit subordinado a la base.** Los tres descubrieron (A lo documentó como su tensión
   honesta: su primera versión daba Inquebrantable a una ráfaga de 10 horas en 1 día) que el superávit
   libre **rompe P2**. La corrección unánime: multiplicar el superávit por una potencia de la base
   (`base^p`). "Superávit sobre base completada" deja de ser una regla y pasa a ser una propiedad
   continua de la matemática.
3. **El peso del superávit de tiempo crece con `(F/7)^κ`.** En F=7 no hay días que ganar; el tiempo es
   la única vía y pesa máximo. (Researcher 3 lo confirma como combinación convexa con peso desplazable
   — su estructura E1/E5, la más limpia de su catálogo.)
4. **Inquebrantable = `R ≥ 1+δ`**, no `>1` a secas: una migaja voluntaria sobre base perfecta no
   asciende de estado.

## 2. La fórmula consolidada

### 2.1 Preprocesamiento (Best-F, todo porcentual)

```
r_i = t_i / T                      razón de tiempo de cada día marcado
Ordenar descendente. D = días marcados.
Compromiso  = los min(D, F) mejores.       Voluntaria = los D−F restantes (si D > F).
Si D = 0  ⟹  R = 0.
```

### 2.2 Las piezas

```
u(r)  = min(r, 1)^γ                                  valor-día (γ ≥ 1: mata trivialidad)

φ     = (1/F) · Σ_compromiso  u(r_i)                 base de compromiso ∈ [0,1]
                                                     (slots vacíos = 0)

V     = Σ_voluntaria  u(r_j)                         días-equivalentes voluntarios ≥ 0

base  = 1 − (1 − φ) · exp(−λ_v · V)                  el voluntario repara el déficit de
                                                     tiempo con retornos decrecientes;
                                                     NUNCA pasa de 1

S_t   = (1/F) · Σ_compromiso  max(r_i − 1, 0)        superávit de tiempo (crudo)
S_d   = V / (7 − F)        (0 si F = 7)              superávit de días, fracción del
                                                     espacio disponible (7−F)

w_t   = (F/7)^κ            w_d = 1 − w_t             pesos desplazables (P2)

S     = σ_max · ( 1 − exp( −(w_t·S_t + w_d·S_d) / σ_0 ) )    superávit saturado: acotado
                                                              en σ_max, no explota

R     = base  +  base^p · S                          rendimiento final ∈ [0, 1+σ_max]
```

**Estados (corrección del dueño, 2026-06-10):** el ancla NO tiene estado propio — `R` es un **valor**
que se promedia en su capa y sube al score global (`score = Σ peso_capa × valor_capa`). Los estados,
**incluido Inquebrantable, se determinan ÚNICAMENTE sobre el score global** de todas las capas: las 4
bandas (`Rojo < 0.40 · Amarillo < 0.62 · En marcha < 0.85 · Pleno ≥ 0.85`) y
**Inquebrantable ⟺ `score ≥ 1 + δ`**. El rol del ancla es **exportar superávit** (`R > 1`) hacia
arriba; como `R > 1` exige `S > 0` y `base ≈ 1` (por `base^p`), solo anclas con base completada
aportan ese empuje — y el score global solo cruza `1+δ` si el superávit está suficientemente
repartido entre capas (cobertura emergente, sin gate). Ver
`simulacion-capas-resultados.md` para el comportamiento con N = 3..8 capas.

### 2.3 Parámetros calibrables (sin fijar — la calibración va contra el dataset de marcas)

| Param | Rol | Rango plausible | De quién viene |
|-------|-----|-----------------|----------------|
| `γ` | mata trivialidad del valor-día (γ=1: lineal; γ↑: 1 min vale nada) | `1.3 – 2.5` | C (su *toe* de Hill, simplificado) |
| `λ_v` | fuerza reparadora del voluntario sobre déficit de tiempo | `0.2 – 0.7` | A (su exponencial saturante) |
| `κ` | velocidad del desplazamiento de peso tiempo↔días con F | `1 – 2` | A, B, C (unánime) + R3 |
| `p` | dureza del requisito "base completa" para el superávit | `1.5 – 3` | A, B, C (unánime) |
| `σ_max` | techo del superávit total (cuánto puede sobresalir R de 1) | `0.3 – 0.6` | merge (corrige a C) |
| `σ_0` | escala de saturación del superávit (qué tan rápido se aplana) | `0.3 – 0.8` | R2 (exponencial saturante) |
| `δ` | margen GLOBAL de Inquebrantable sobre 1 (se evalúa sobre el score total, no por ancla) | `0.08 – 0.20` | A y C (convergencia) |

## 3. Qué se tomó de cada uno — y qué se rechazó, y por qué

### De A (dominancia fuerte) — el chasis
- **TOMADO:** el esqueleto φ sobre F slots; la reparación voluntaria `1−(1−φ)·exp(−λ_v·V)` (la pieza
  más elegante de la sesión: el voluntario rellena el déficit con retornos decrecientes y satura a 1
  **por construcción**, sin clamp); la subordinación `base^p`; el factor `(F/7)^κ`.
- **RECHAZADO:** en A los días voluntarios **nunca** generan superávit (solo reparan; con base completa
  no valen nada). Hacer 7 días plenos con F=3 da R=1.000 exacto, igual que hacer 3. Eso contradice el
  contexto de sesión, que nombra el "superávit de día (voluntario)" como vía de superávit, y vacía la
  vía natural de Inquebrantable a F bajo. El merge conserva la reparación de A pero agrega el canal de
  superávit de días de B/C.

### De B (acoplamiento suave) — la confirmación y un rechazo informativo
- **TOMADO:** la confirmación independiente de canales separados (voluntario ≠ superávit de tiempo) y
  del apagado continuo `B^g`; su caso testigo (F=5, D=2, [60,60] → Rojo) calibró la dureza necesaria
  de `p`.
- **RECHAZADO:** la base `df^a · gt^b` con **media geométrica**. Dos razones: (i) acopla las dos
  dimensiones con exponentes calibrables `a > b` — la dominancia de frecuencia queda en manos de la
  calibración, cuando la restricción §8.3 la exige **estructural** (el esqueleto de slots de A/C la da
  gratis: dividir por F no es negociable por ningún parámetro); (ii) la media geométrica hace el caso
  "déficit puro de tiempo" demasiado generoso (B dio 0.70, "En marcha", para una semana entera al 20%
  del tiempo — contra 0.51 de A y 0.15 de C; el merge da ~0.28, entre A y C, ajustable con γ y λ_v).
  Además `0^a` con slots vacíos exige cuidado en bordes que el esqueleto aditivo no tiene.

### De C (saturación) — las dos mejores ideas locales
- **TOMADO:** el ***toe* anti-trivialidad** (su hallazgo más fino: la cóncava pura tiene pendiente
  máxima en 0 y le regala valor a 1 min sobre 30; se necesita una zona baja plana). El merge lo toma
  **simplificado**: `u(r) = min(r,1)^γ` con γ>1 da el mismo zócalo sin cargar la Hill completa
  (`g(1/30) = 0.0017` en C; `u(1/30) = 0.003` con γ=1.5 — mismo orden, una pieza menos). También:
  Inquebrantable con margen δ, y la evidencia de que el peso del superávit puede escalar con F.
- **RECHAZADO:** el techo del día `K(F,φ)^n + 1` — su asíntota llega a ~5 y produce R=4.0 (F=7 a 4×T).
  Un ancla en 4.0 **rompe el promedio de capa**: una sola ancla inflada arrastra a toda la capa (4.0
  promediada con dos anclas en 0.5 da capa 1.67 — un Inquebrantable de capa fabricado por una ancla).
  El motor aguas arriba espera valores en `[0, 1+]` moderado. El merge acota el superávit TOTAL con la
  exponencial saturante `σ_max·(1−exp(−·/σ_0))` (estructura del Researcher 2): R ≤ 1+σ_max siempre.

### De los researchers
- **R1 (métricas acopladas):** confirmó que dominancia y compensación son DOS perillas separables; su
  análisis CES muestra que la arquitectura de slots equivale a elasticidad de sustitución casi nula
  entre días faltantes y tiempo (lo que pide la restricción §8.5) mientras el canal voluntario da la
  compensación parcial. Validación conceptual del chasis elegido.
- **R2 (saturación):** la exponencial saturante reparametrizada es exactamente la pieza del superávit
  del merge (techo asintótico σ_max, codo σ_0); confirmó que Hill n>1 y power-law son intercambiables
  para el toe — el merge usa la power-law (más simple, pasa por (0,0) y (1,1) sin reparametrizar).
- **R3 (pesos desplazables):** la combinación convexa `w_t + w_d = 1` con `w_t = (F/7)^κ` es su
  estructura E1/E5 (gating tipo GRU / scheduler de annealing): pesos que suman 1 por construcción,
  límites exactos en F=1 y F=7, un solo parámetro. Es la solución canónica de P2.

## 4. Verificación de la fórmula consolidada (números, parámetros ilustrativos)

> `γ=1.5, λ_v=0.5, κ=1.5, p=2, σ_max=0.5, σ_0=0.5, δ=0.10` — **ilustrativos**, solo para verificar
> comportamiento. Calculado con python3 (script reproducible abajo, §7).

### 4.1 Axiomas

| Axioma | Test | Resultado | Veredicto |
|--------|------|-----------|-----------|
| A1 rango | nada / exacto / superávit F=7 r=2 | `0 / 1.0000 / 1.4323` | ✅ |
| A2 piso cero | D=0 | `0` | ✅ |
| A3 exacto = 1 | D=F, todos t=T (F=3 y F=7) | `1.0000` exacto | ✅ |
| A4 monotonía días | agregar día voluntario v=0.001→60 min | `1.000 → 1.0011 → … → 1.2106` no decrece | ✅ |
| A5 monotonía tiempo | día de compromiso t=0→90 (F=3) | `0.667 → … → 1.000 → 1.156` no decrece | ✅ |
| A7 piso voluntario | voluntario ε→0 | aporte → 0, nunca negativo (entra vía `exp(−λV)≤1` y `S_d≥0`) | ✅ |
| A9 continuidad | barrido fino (paso 0.5 min) cruzando r=1 y frontera de zona | salto máx `0.0075` | ✅ |
| A10 invarianza | (T=30,[40,30,30]) vs (T=120,×4) | `1.0302… == 1.0302…` exacto | ✅ |

> Nota A10: `F` entra SOLO como `F/7` (fracción comprometida de la semana) y `V/(7−F)` (fracción del
> espacio de días libres usado) — razones, no magnitudes. P2 **exige** que el peso dependa de F; la
> lectura consistente de A10 es "sin minutos crudos, sin escala absoluta", que se cumple exacto.

### 4.2 Casos límite de §7 + testigos

| Caso | Setup | R | Lectura | ✓ |
|------|-------|---|---------|---|
| Cumplimiento exacto | F=3,T=30,[30,30,30] | **1.0000** | Pleno exacto | ✅ |
| Nada hecho | D=0 | **0.0000** | Rojo | ✅ |
| Superávit días + déficit tiempo | F=3,T=30,[10,10,10,90,90] | **0.9559** | base=0.78: los 90 NO tapan los déficits de los 10; el superávit empuja pero `base^p` lo frena bajo 1+δ → Pleno, no Inq | ✅ |
| Déficit frecuencia + tiempo alto | F=5,T=20,D=2,[60,60] | **0.4495** | déficit dominante (Amarillo bajo); el 300% de tiempo apenas amortigua | ✅ |
| 40 min vs 1+1 min | F=2,T=30: [30,30,40] vs [30,30,1,1] | **1.1614 > 1.0021** | concentrado gana; trivial no cuenta como día | ✅ |
| Voluntario trivial | F=2,T=30,[30,30,1,1,1] | **1.0031** | base perfecta + migajas = Pleno, no Inq | ✅ |
| Déficit puro de tiempo | F=2,T=5,[1×7] | **0.2771** | el 20% de tiempo domina; 7 días presentes suben poco (Rojo) | ✅ |
| Saturación F=7 | [30×7]/[45×7]/[60×7]/[120×7] | **1.00 / 1.32 / 1.43 / 1.499** | el tiempo solo alcanza Inq (≥1.10); acotado en 1+σ_max=1.5, no explota | ✅ |
| Invarianza de escala | ver A10 | idéntico | — | ✅ |
| **Testigo del dueño** | F=4,T=40,[40×4,5,5,5] | **1.0245** | "3 días extra × 5 min NO es superhabit pleno" → Pleno, no Inq | ✅ |
| Ráfaga absurda | F=5,D=1,[400] (r=20) | **0.2198** | un día de 6.6 horas no compensa 4 faltantes (Rojo) | ✅ |
| P2 monótono | r=2 en todos, F=2/3/5/7 | **1.13 / 1.21 / 1.35 / 1.43** | el peso del superávit de tiempo crece con F | ✅ |

## 5. Cómo resuelve P1 y P2

**P1.** La dominancia de frecuencia es el esqueleto: dividir por F y dejar slots vacíos en 0 hace que
ningún parámetro pueda comprarla. El "un día vale como día según su fracción de tiempo" es `u(r)`:
lineal hasta la meta si γ=1, con zócalo anti-trivialidad si γ>1. Los dos superávits son **canales
distintos que confluyen en una bolsa ponderada** (`w_t·S_t + w_d·S_d`): la respuesta a la pregunta
abierta "¿una bolsa o dos canales?" resultó ser *ambas* — canales separados con pesos desplazables que
se funden en una sola saturación. El déficit de frecuencia no tiene techo duro: `φ` parcial + la
reparación voluntaria aportan lo suyo, pero el superávit estrangulado por `base^p` jamás lo compensa
(ráfaga de r=20 en 1 día sobre F=5 → 0.22).

**P2.** En F=7: `S_d = 0`, `w_t = 1` — el tiempo hereda TODO el peso del superávit exactamente cuando
es la única vía, y como la base se completa con frecuencia perfecta, `base^p = 1` deja pasar el empuje
completo: `[45×7]` → 1.32 ≥ 1+δ → Inquebrantable. A F bajo, `w_t` chico: el tiempo extra pesa poco
porque todavía quedan días por ganar (y `S_d` los paga). La transición es continua en F, sin un solo if.

## 6. Explicación en lenguaje claro

Tu ancla es un compromiso con dos números: cuántos días por semana (F) y cuánto tiempo por sesión (T).
La fórmula te mira la semana así:

**Primero, el esqueleto.** Tenés F casilleros, uno por día comprometido. Tus mejores F días los van
llenando: un día con la meta cumplida llena su casillero; un día corto lo llena en proporción — y un
día de 2 minutos no llena casi nada, porque marcar por marcar no es práctica real. Los casilleros que
quedan vacíos son ceros que **ninguna cantidad de tiempo en otros días puede rellenar**. Eso es la
constancia mandando: la base de tu rendimiento es cuántos casilleros llenaste.

**Segundo, los días extra.** Si apareciste más días de los comprometidos, esos días ayudan: si tu base
quedó incompleta (días flojos de tiempo), la van reparando — cada día extra ayuda un poco menos que el
anterior, y nunca te regalan una base que no construiste. Si tu base ya está completa, los días extra
se convierten en superávit genuino: el camino a Inquebrantable cuando tu meta de frecuencia es baja.

**Tercero, el tiempo de más.** Pasarte de tu meta de tiempo en los días comprometidos también es
superávit — pero solo brilla **cuando el esqueleto ya está armado**. Con base incompleta, el tiempo
extra casi no cuenta: primero la presencia, después la intensidad. Y acá está la pieza fina: el peso
del tiempo extra **crece con tu frecuencia objetivo**. Si te comprometiste a 7 días, ya no hay días que
ganar — el tiempo es tu única forma de sobresalir, y la fórmula se lo reconoce al máximo. Si te
comprometiste a 2, el tiempo extra pesa poco: tu vía natural de superávit es aparecer más días.

**Y el techo.** Todo el superávit junto está saturado: sobresalir un 30% es alcanzable, un 400% no
existe. Nadie puede romper el promedio de su capa a fuerza de grinding, e Inquebrantable exige un
margen real sobre la base completa — no una migaja.

La app termina diciendo exactamente lo que el producto quiere decir: *mostrate los días que dijiste y
cumplí tu tiempo — esa es la base. Lo demás suma, se nota y tiene techo. Y si estás bajo, no es condena:
es la base pidiendo presencia, no heroísmo de un día.*

## 7. Reproducibilidad y próximos pasos

Script de verificación (idéntico al usado en §4):

```python
import math

def R(F, T, mins, gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0, smax=0.5, s0=0.5):
    marked = sorted([m for m in mins if m > 0], reverse=True)
    D = len(marked)
    if D == 0: return 0.0
    r = [m/T for m in marked]
    commit, vol = r[:min(D,F)], r[min(D,F):]
    u = lambda x: min(x, 1.0)**gamma
    phi  = sum(u(x) for x in commit) / F
    V    = sum(u(x) for x in vol)
    base = 1 - (1-phi)*math.exp(-lam_v*V)
    St   = sum(max(x-1, 0) for x in commit) / F
    Sd   = V/(7-F) if F < 7 else 0.0
    wt   = (F/7)**kappa
    S    = smax*(1 - math.exp(-(wt*St + (1-wt)*Sd)/s0))
    return base + (base**p)*S
```

**Decisiones que esta fórmula deja EXPLÍCITAMENTE para después (no son gaps, son etapas):**
1. **Calibración** de `γ, λ_v, κ, p, σ_max, σ_0, δ` contra el dataset de marcas del dueño — incluye
   marcar casos nuevos en los huecos (déficit puro de tiempo, voluntarios triviales, F intermedios).
2. **Testeo automatizado** (verificación de axiomas por barrido, red team) — solo si el dueño decide
   que la estructura vale la inversión.
3. **Cobertura multi-capa de Inquebrantable** (cuántas anclas/capas en superávit) — es la capa de
   agregación de arriba, fuera del alcance de esta sesión (el mapa §6 lo tiene como gap propio).
