# SUBAGENTE B — Plan v2: "Blends convexos por capa"

> Forma A (soportes/tasks DENTRO del valor de cada capa). Reusa el motor v4 verbatim
> (`exploracion-valor-capa/modelo_valor_capa_v4_merge.py`): la fórmula del ancla `R`, la curva de
> superhabit y los opt-ins O1–O13 **no se tocan**. Verificación ejecutable: `subagente_B_v2.py`.
> Proyecto Engram: `apk-personal`.

---

## 1. Filosofía del sesgo

El valor de una capa es un **promedio convexo** entre lo que dicen las anclas y lo que dicen los
soportes, más un **extra saturado conjunto** donde anclas y tasks comparten una única curva. La idea
central: nada se "suma crudo por fuera"; todo entra como una **mezcla** o como un **empujón dentro de
la misma exponencial** que ya gobierna el superhabit. Esto da tres propiedades gratis: (1)
**bidireccionalidad natural** del soporte —el blend tira la base hacia la señal de soporte, sube si
sostenés mejor que tus anclas y baja si peor—; (2) **techo natural en 1** para la base (un promedio
convexo de cosas ≤1 es ≤1) y techo 0.5 para el extra (compartir la `exp` lo garantiza por
construcción); (3) **anti-abuso automático** —la saturación compartida hace que cada task extra valga
menos cuanto más gloria ya hay—. Es elegante porque cada capa se razona con dos números: un punto en
`[0,1]` (la base mezclada) y un punto en `[0,0.5]` (el extra conjunto).

## 2. Axiomas

### Soportes (S1–S6)
- **S1 — Mecanismo propio, intra-capa.** El soporte entra SOLO en el valor de su capa, nunca como
  sombra global ni mezclado con los opt-ins (O1–O13).
- **S2 — Blend convexo en la base.** `base_eff = (1−γ_s)·base_anclas + γ_s·G_soporte`, con `γ_s`
  chico y **FIJO**. El soporte es un peso fijo del cimiento, no un sumando.
- **S3 — Bidireccional leve y neutro en la par.** Si `G_soporte = base_anclas` → cero cambio. Si
  `G > base` → sube (pull hacia G); si `G < base` → baja. La magnitud del empujón es `γ_s·(G−base)`,
  que se **desvanece sola** cuando la base ya está alta (queda poco `(1−base)` para subir).
- **S4 — Solo base, nunca extra.** El soporte jamás toca el superhabit. La base ya está topada en 1.
- **S5 — Señal por soporte, ventana indulgente.** `s_i = min(días_cumplidos / 4, 1)` (4 días = 100%).
- **S6 — Bloque que NO crece con la cantidad.** `G_soporte = promedio(s_i)`. 1 o 5 soportes pesan lo
  mismo (`γ_s` es del bloque, no por soporte). Anclas ≫ soportes (`γ_s` es pequeño).

### Tasks (T1–T6)
- **T1 — Solo extra, dentro de la curva.** La task aporta únicamente al extra de su capa, a través de
  la **misma** exponencial `S = smax·(1−exp(−surplus/s0))` (smax=0.5, s0=0.5). Nunca suma crudo.
- **T2 — Saturación CONJUNTA reparametrizada.** Se invierte el extra de anclas a su `surplus`
  pre-saturación y se le agrega `θ·g_task`; se vuelve a saturar con la misma `exp`. Anclas y tasks
  comparten un único acumulador → diminishing returns compartidos.
- **T3 — Gate base².** El aporte de task se multiplica por `base_eff²`, igual que el superhabit de
  anclas: sin cimiento no hay gloria. Tasks arañan un cruce cuando ya estás cerca; no fabrican estado.
- **T4 — Saturación por conteo.** `g_task = 1 − exp(−n_tasks/n0)`, `n0=1.0`: la 1ª task vale 63% del
  techo, la 3ª ≈95%, 100 tasks ≈ techo. Anti-abuso por conteo + por curva compartida.
- **T5 — Diario y efímero.** `n_tasks` son las de HOY; mañana arranca en 0. El motor semanal usa el
  pulso del día; al cerrar el día el empujón se evapora (lo que queda son las anclas).
- **T6 — Techo por capa y nunca resta.** El aporte vive en `[0, τ]` por capa (τ despejado, §3). Task
  con capa aporta a esa capa; task neutral no aporta; jamás negativo.

### Peso de capa (B8)
- **B8 — Peso por densidad de anclas.** `w_capa = w0·(η + (1−η)·dens)`, `dens = 1 − exp(−n_anclas/d0)`.
  Una capa solo-soportes (`n_anclas=0`) pesa `η·w0` (reducido, continuo, sin gate duro). Una capa con
  sustancia de anclas pesa más. ANCLAS ≫ soportes también en peso de capa.

## 3. Fórmulas y parámetros DESPEJADOS de axiomas de estado

| Parám. | Valor | Despeje (axioma de estado, no a dedo) |
|--------|-------|----------------------------------------|
| `γ_s`  | **0.07** | "Leve a nivel capa": el neglect total de un soporte (`G=0`) sobre una capa justa (`base=1`) baja la base de esa capa exactamente `γ_s·1 = 0.07`. Es la magnitud orientativa del dueño, pero anclada al axioma de que un descuido total no puede mover una capa más que un paso leve. |
| `τ`    | **0.05** | "Tasks arañan, no fabrican": el gap de banda P→I es 0.10. Axioma (f-strict): tasks al máximo en TODAS las capas, desde cumplir-justo (1.0), deben quedar **estrictamente** bajo Inquebrantable. Se exige que tasks cierren a lo sumo **medio** gap → `τ = 0.10/2 = 0.05`. Así `1.0 + τ = 1.05 < 1.10`. |
| `θ`    | **0.0527** | Presupuesto pre-saturación de task. Se despeja de `τ`: `θ = −s0·ln(1 − τ/smax)` para que el lift máximo de task (extra_anclas=0, gate=1) sea exactamente `τ`. |
| `n0`   | **1.0** | Saturación de conteo: 1 task ≈ 63% del techo (la 1ª vale mucho), 100 ≈ techo. |
| `η`    | **1/3** | Peso de capa solo-soportes: una capa sin sustancia de anclas pesa 1/3 de una capa-ancla. ANCLAS≫soportes en el global; ratio efectivo ≈ 0.44 frente a una capa-ancla densa. |
| `d0`   | **1.0** | Densidad: 1 ancla → 0.63 de densidad, 2 → 0.86, saturando hacia 1. |

**Valor de capa (B):**
```
base_anclas  = promedio min(R,1) de anclas         # v4
extra_anclas = promedio max(R−1,0) de anclas        # v4 (ya saturado por R)
G_soporte    = promedio min(días_i/4, 1)            # bloque, no crece con la cantidad
base_eff     = (1−γ_s)·base_anclas + γ_s·G_soporte  # blend convexo (sin soportes → = base_anclas)

su_anclas    = −s0·ln(1 − extra_anclas/smax)        # invierte la exp del v4 (surplus pre-sat)
g_task       = 1 − exp(−n_tasks/n0)
extra_joint  = smax·(1 − exp(−(su_anclas + θ·g_task)/s0))   # MISMA curva, mismo techo 0.5
task_lift    = (extra_joint − extra_anclas)·base_eff²       # gate base² (no fabrica sin cimiento)
extra        = extra_anclas + task_lift

w_capa       = w0·(η + (1−η)·(1 − exp(−n_anclas/d0)))
valor_capa   = min(base_eff, 1) + extra
score        = Σ w_capa·valor_capa / Σ w_capa
```
**Reducción a v4:** sin soportes y sin tasks, `base_eff = base_anclas`, `extra = extra_anclas`, y con
capas-ancla los pesos son ~iguales → el modelo colapsa al v4 (Sol=Tin, cumplir-justo=1.0). Verificado.

## 4. Verificación `python3` (resultados reales)

Script: `subagente_B_v2.py` (motor v4 verbatim + capa B). Salida real:

```
B v2. GAMMA_S=0.07 TAU=0.05 THETA=0.0527 N0=1.0 ETA=0.333 D0=1.0
J(justo)=1.000 XL=1.432 DEF(3d)=0.750
========================================================================================
(a) Sol=1.1441 Tin=1.1441 empatan=True
(b) cumplir-justo=1.0000 banda=PLENITUD ok=True
(c) neutro(G=ba)=1.0000 (=1.0) | sube si sostengo>0.9167->0.9225 | baja por neglect=0.9767
    NO crece con cantidad: 1soporte=0.9225  5soportes=0.9225 iguales=True
(d) peso solo-soportes=0.333 vs anchor-layer=0.755 ratio=0.442
    capa solo-sop perfecta junto a DEF: score=0.8266 (anclas pesan mas)
(e) task extra a nivel capa: 1=0.0322 3=0.0476 100=0.0500 techo<=0.5=True
    joint sat: lift task con extra_anclas=0 -> 0.0476; con anclas XL(superhabit alto) -> 0.0064 (menor)
    reset diario: HOY n_tasks=3 score=1.0159  MANANA n_tasks=0 score=1.0000
(f) cerca del borde: sin task=1.0347 PLENITUD -> con task=1.0495 PLENITUD
    cumplir-justo + 100 tasks en TODAS las capas = 1.0500 PLENITUD (NO Inquebrantable solo por tasks: True)
(g) continuo: paso max soporte=0.00006  paso max task=0.00085 (sin gate duro)
(h) impacto MAXIMO: ANCLA(J->XL)=0.1441 > SOPORTE(full sobre base debil)=0.0188 > TASK(100)=0.0167  orden=True
```
Caso extra (araña el cruce vs. no fabrica):
```
cerca I: sin task=1.0824 PLENITUD -> con tasks=1.1241 INQUEBRANTABLE (arana el cruce)
lejos:  sin task=1.0000          -> con tasks=1.0500 (no fabrica)
```

**Lectura de cada caso:**
- (a) Sol=Tin sigue exacto (el reparto del superhabit no se altera).
- (b) cumplir-justo = 1.0 = Plenitud, intacto.
- (c) Soporte neutro cuando va a la par (`G=base`→1.0), sube una base a medias (0.9167→0.9225), baja
  por descuido (→0.9767), e **idéntico** con 1 o 5 soportes.
- (d) Capa solo-soportes pesa 0.333 vs 0.755 de una capa-ancla densa (ratio 0.44): impacta menos.
- (e) Task satura (0.032→0.048→0.05), nunca pasa el techo 0.5, el lift se **encoge** cuando las anclas
  ya tienen superhabit (0.0476 → 0.0064: saturación conjunta), y resetea al día siguiente.
- (f) Tasks arañan dentro de Plenitud; 100 tasks en todas las capas desde justo = 1.05 < 1.10: **no**
  compran Inquebrantable solos. Pero si ya estás a 1.082 (cerca), arañan el cruce a I.
- (g) Continuo: paso máximo 0.00006 (soporte) y 0.00085 (task) → sin gate duro.
- (h) ANCLA (0.144) > SOPORTE (0.019) > TASK (0.017) en impacto máximo.

## 5. Riesgos / abierto

- **(h) es ajustado** (SOPORTE 0.019 vs TASK 0.017): el orden se sostiene midiendo el impacto MÁXIMO
  de cada mecanismo donde más importa (soporte sobre base débil, donde define la banda; task en su
  techo). Cerca de base=1 un task supera puntualmente a un soporte —semánticamente OK: si las anclas ya
  son perfectas, sostener no agrega nada pero el esfuerzo extra de tasks sí—. Si el dueño quiere
  ANCLAS≫SOPORTES≫TASKS con holgura uniforme, bajar `τ` a 0.04 o subir `γ_s` a 0.08 lo separa más.
- **τ vs DELTA:** `τ` está atado al gap P→I (0.10). Si el árbol cambia el ancho de banda, hay que
  re-despejar `τ`.
- **`θ` por capa, no global:** cada capa satura su propio acumulador; el global promedia. Esto evita
  que una capa con tasks contamine a otras, pero conviene confirmarlo contra escenarios multi-capa
  reales del dueño.
- **Solo-soportes con G como base directa:** una capa sin anclas usa `base_eff = G`. Decisión: el
  soporte ahí ES la sustancia, pero con peso reducido `η`. Pendiente validar con un caso del dueño.

## 6. En qué diverge mi enfoque
- **Soporte = blend convexo** (no entrada virtual a `R` como A, ni relleno de gap como C): la
  bidireccionalidad y el techo en 1 salen gratis de la convexidad.
- **Task = saturación conjunta reparametrizada**: comparto la MISMA `exp` del superhabit invirtiéndola
  y re-saturando, en vez de inyectar surplus virtual antes de `S` (A) o llenar headroom con tope (C).
  El anti-abuso es estructural, no un cap añadido.
- **Peso de capa = blend por densidad** `η+(1−η)·dens` con saturación exponencial de densidad.
