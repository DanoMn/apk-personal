> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Modelo propuesto A — "Multiplicador fijo normalizado" (Forma A)

> **Estado: propuesta independiente para síntesis.** Uno de los 3 modelos del motor de
> scoring (plan: `meta/instructions/2026-06-08-tres-modelos-motor-scoring.md`). Respeta
> TODAS las bases cerradas: el motor es **solo pesos** → `score = Σ(peso_capa × valor_capa)`
> → estado por bandas. **Cero reglas, caps, gates, colapsos, worst-term.** Todo comportamiento
> EMERGE de peso × valor.

---

## 1. Resumen del modelo (5 líneas)

1. **Cada capa activa pesa lo mismo (piso = 1).** Un opt-in activo **multiplica** el peso de su
   capa por un factor fijo (`k_sleep`=1.5, `k_sobr`=3.0) y luego **todo se normaliza a suma 1**.
2. **Escalado con N → Forma A (multiplicador fijo):** como se normaliza, la *porción* del opt-in
   **baja al subir N** (sueño es 43% de Cuerpo a 3 capas, 27% a 5). El opt-in pesa relativo al resto,
   no en porción absoluta. Justificación abajo (§4.1).
3. **El valor de cada capa** es el promedio plano de sus anclas; el sueño entra al **valor de Cuerpo**
   (mezcla `beta`), la sobriedad **reemplaza** el valor de Conducta (clean=anclas, recaída=piso plano),
   y los soportes mezclan linealmente dentro del valor de su capa (asimetría emergente).
4. **El apretón** (3 capas + ambos opt-in) **no ahoga** la capa libre: por la normalización
   multiplicativa nunca cae a 0 — conserva ~18% y sigue moviendo el score. Sin regla.
5. **Inquebrantable** no es banda: es estado sobre Plenitud (anclas 100% + superhabit en **≥2 capas**).

**Reproduce 45/45 marcas** del dataset (§5).

---

## 2. Fórmula matemática completa

### 2.1 Notación

- `N` = nº de capas activas (3 ≤ N ≤ 5). Capas posibles: Interior (I), Cuerpo (Cu), Conducta (Co),
  Vínculos (V), Proyecto (P).
- `a_L` = **valor de anclas** de la capa L = promedio plano de las fracciones de cumplimiento de sus
  anclas: `a_L = (1/n_L)·Σ frac(ancla_i)`. **La cantidad de anclas NO cambia el peso** (promedian).
- `sleep ∈ {off, ok, mal, none}`; `sobr ∈ {off, clean, relapse, unmarked}`.
- `sup_L ∈ [0,1]` = fracción de soporte cumplida en L (o `None` si no hay soporte en L).

### 2.2 Valor de capa `v_L`

```
core_L =
   Cuerpo con sueño activo :  beta·sleepval + (1-beta)·a_Cu          # sueño DENTRO del valor
   Conducta con sobr activa:  { clean→a_Co · relapse→r_relapse · unmarked→r_unmarked }
   resto                   :  a_L

  donde sleepval = 1.0 si sleep=ok ; = s_bad si sleep∈{mal,none}

soporte (si hay soporte en L):
   core_L ← (1 - p_sop)·core_L + p_sop·sup_L      # mezcla lineal

v_L = clamp(core_L, 0, 1)
```

- **Sueño** se mezcla al valor de Cuerpo con `beta` (peso de "una ancla pesada"). `sleep=ok` lo sube,
  `mal`/`none` lo hunden a `s_bad` (mismo veredicto: el dueño los trata igual hoy, baja confianza).
- **Sobriedad** *reemplaza* el valor de Conducta: limpia = el valor real de las anclas de Conducta;
  recaída = piso plano `r_relapse` **independiente del largo de la racha** (binario, dato SBR);
  no-marcado (ventana perdón) = `r_unmarked` (≈ recaída-piso, para topear EM sin asumir recaída).
- **Soporte:** mezcla lineal. Descuidar el soporte (`sup_L`→0) **hunde** `v_L` (castiga); tenerlo full
  (`sup_L`=1) **premia poco** si las anclas ya están altas (techo en 1). La asimetría "castiga más que
  premia" emerge de que el descuido resta de un valor alto, pero el premio choca con el clamp a 1.

### 2.3 Peso de capa `w_L` — **Forma A (multiplicador fijo + normalización)**

```
raw_L = 1                                   # piso: toda capa activa pesa igual
raw_Cu *= k_sleep   si sleep != off         # sueño infla el peso de Cuerpo
raw_Co *= k_sobr    si sobr  != off         # sobriedad infla el peso de Conducta

w_L = raw_L / Σ_j raw_j                      # normalización a suma 1
```

### 2.4 Score y bandas

```
score = Σ_L  w_L · v_L            # ω = 0 : SIN término de peor capa (lo exige la base pura BP)

estado =  R   si score <  cRA
          A   si score <  cAEM
          EM  si score <  cEMP
          P   si score >= cEMP

Inquebrantable (NO es banda): estado = I  si  (estado==P) ∧ (anclas 100%) ∧ (superhabit en ≥2 capas)
```

### 2.5 Parámetros (fijados por diseño, dentro de la familia que da 45/45)

| Param | Valor | Significado |
|-------|-------|-------------|
| `beta` | 0.45 | aporte del sueño al valor de Cuerpo (≈ una ancla pesada) |
| `s_bad` | 0.20 | valor del sueño cuando está mal / no registrado |
| `k_sleep` | 1.5 | multiplicador de peso de Cuerpo con sueño activo |
| `k_sobr` | 3.0 | multiplicador de peso de Conducta con sobriedad activa |
| `r_relapse` | 0.50 | valor plano de Conducta en recaída (indep. del largo) |
| `r_unmarked` | 0.50 | valor de Conducta con sobriedad activa sin marcar (ventana perdón) |
| `p_sop` | 0.25 | porción del valor de capa que aporta el soporte (lineal) |
| `cRA` | 0.40 | corte Restauración \| Atención |
| `cAEM` | 0.62 | corte Atención \| En marcha |
| `cEMP` | 0.86 | corte En marcha \| Plenitud |

> Esta es la región **más robusta** (máximo margen mínimo a los cortes) dentro del subespacio
> compatible con la Forma A. Hay una familia ancha de valores que también da 45/45 (§5); estos
> son representativos y redondos donde se puede.

---

## 3. Tabla de pesos (Forma A, `k_sleep`=1.5, `k_sobr`=3.0)

Peso normalizado de cada capa en cada combinación de opt-in, para **N=3** y **N=5**:

### N = 3 (Interior, Cuerpo, Conducta)

| combinación | Interior | Cuerpo | Conducta |
|-------------|----------|--------|----------|
| sin opt-in | 0.333 | 0.333 | 0.333 |
| solo sueño | 0.286 | **0.429** | 0.286 |
| solo sobriedad | 0.200 | 0.200 | **0.600** |
| ambos | 0.182 | 0.273 | **0.545** |

### N = 5 (I, Cu, Co, V, P)

| combinación | Interior | Cuerpo | Conducta | Vínculos | Proyecto |
|-------------|----------|--------|----------|----------|----------|
| sin opt-in | 0.200 | 0.200 | 0.200 | 0.200 | 0.200 |
| solo sueño | 0.182 | **0.273** | 0.182 | 0.182 | 0.182 |
| solo sobriedad | 0.143 | 0.143 | **0.429** | 0.143 | 0.143 |
| ambos | 0.133 | 0.200 | **0.400** | 0.133 | 0.133 |

**Verificación del sustento cerrado (base #8):** con sobriedad activa, Conducta pesa **0.60 a N=3**
y **0.43 a N=5** — dentro del rango "~50–63%" derivado de SB9+SB4 (que vienen de config de 3 capas).
La caída a N=5 es la firma de la Forma A.

---

## 4. Resolución de los casos límite

### 4.1 Escalado con N → **Forma A: multiplicador fijo (su porción baja al subir N)**

**Elección: Forma A.** El opt-in multiplica el peso de su capa por un factor fijo *antes* de
normalizar, así que su **porción absoluta cae a medida que N crece** (sueño: 43% de Cuerpo a N=3,
27% a N=5).

**Justificación.**
1. **Coherencia conceptual:** el opt-in dice "esta capa importa MÁS que sus pares", una afirmación
   *relativa*. Cuando hay 5 vidas en juego en vez de 3, ninguna sola — ni la portante — debería
   acaparar la misma porción absoluta; el sistema sigue siendo de capas parejas con dos acentos.
2. **Es lo que los datos permiten sin contradicción:** el sustento "Conducta ~50–63%" se midió a
   N=3. La Forma A lo reproduce a N=3 (0.60) y deja que baje natural a N=5. No hay marca a N=5 con
   moduladores, así que la Forma A no rompe ningún dato y respeta el espíritu "más capas → más
   reparto".
3. **Mata el apretón de raíz** (ver §4.2): como nunca se asigna porción absoluta fija, la suma de
   los opt-in jamás puede comerse a la capa libre por construcción.

**Contraste con Forma B (porción fija).** Forma B fijaría, p.ej., "sueño = 30% del score a cualquier
N". Es más simple de explicar al usuario pero tiene dos problemas: (a) a N=3 + ambos opt-in, dos
porciones fijas grandes ahogan la capa libre (el apretón se vuelve estructural, no emergente); (b)
obliga a re-derivar la porción contra los datos de N=3 y luego *imponerla* a N=5 sin evidencia.
Forma A es la opción que menos inventa.

### 4.2 El apretón (3 capas + sueño + sobriedad) — **se resuelve solo, sin regla**

Con N=3 y ambos opt-in: **Interior (capa libre) = 0.182, Cuerpo = 0.273, Conducta = 0.545.**

- La capa libre conserva **~18%** del peso: **sigue moviendo el score** (no se ahoga). Con la capa
  libre a 0 y el resto perfecto, el score cae a `1·0.818 ≈ 0.818` → En marcha (no Plenitud): la capa
  libre abandonada **sí baja un escalón**, que es lo deseable.
- **Por qué no se ahoga sin meter una regla:** es matemático. Como el peso se construye
  multiplicando *sobre* un piso de 1 y luego se normaliza, **ningún peso puede llegar a 0** mientras
  los multiplicadores sean finitos. El "apretón" del 17% del enunciado venía de ×2/×3 con porción
  fija; con multiplicador 1.5/3.0 normalizado, el piso de la capa libre es 1/(1+1.5+3) = **0.182**, que
  es un peso sano. Si se quisiera más aire para la capa libre, se baja `k_sobr` (es el factor que más
  aprieta) — pero a 3.0 ya queda holgado y reproduce las marcas.
- **No hay regla anti-ahogo.** El aire emerge del piso=1 + normalización. Esa es la propiedad central
  de la Forma A.

### 4.3 Los dos opt-in juntos (sueño + sobriedad) — **elección de diseño, sin dato**

No hay ni una marca con ambos activos. La Forma A **combina por multiplicación independiente**:
Cuerpo ×1.5 y Conducta ×3.0, luego una sola normalización. Consecuencias (probadas en el script):

| escenario (N=3) | estado | score |
|-----------------|--------|-------|
| 100% + sueño ok + limpia | P | 1.000 |
| 100% + sueño **mal** + limpia | **P** | 0.902 |
| 100% + sueño ok + **recaída** | EM | 0.727 |
| 100% + sueño **mal** + **recaída** | EM | 0.629 |
| 50% + sueño ok + limpia | A | 0.561 |

**Decisión y su tradeoff honesto.** Hay UNA tensión que el dueño debe ver: cuando **ambos** opt-in
están activos, un **sueño malo solo (con sobriedad limpia) ya NO saca de Plenitud** (queda en P,
0.902), mientras que con sueño como único opt-in sí saca (SU2/SU3 → EM). **Razón emergente:** al
activar sobriedad, Conducta ×3 le roba porción a Cuerpo (de 0.43 baja a 0.27), así que el golpe del
sueño malo pesa menos en el agregado. **No es un bug del código: es la consecuencia directa de la
Forma A** (un segundo acento diluye al primero).

- **Lectura a favor:** el usuario que sostiene una racha de sobriedad limpia *está* haciendo algo
  pesado bien; que una mala semana de sueño no lo tire de Plenitud es defendible.
- **Lectura en contra:** contradice el espíritu "sueño = el peso más pesado del sistema; mal sueño
  topea EM". Si el dueño quiere preservar ese tope *incluso con sobriedad activa*, la Forma A pura no
  alcanza y haría falta o (a) un `k_sleep` más alto, o (b) que el golpe del sueño entre como término
  aditivo además del peso (mezcla A+C). **Lo dejo señalado, NO lo parcheo con una regla** (base
  cerrada). El consolidado debería elegir.

### 4.4 Anclas múltiples por capa — **promedio plano confirmado**

`a_L = promedio simple de las fracciones de sus anclas`. Interior=(Meditar+Leer)/2 pesa lo mismo
que Cuerpo=Caminar (una sola ancla): **la cantidad de anclas no altera el peso de la capa.** Esto:
- Respeta la base cerrada #3 ("las anclas se promedian dentro de la capa").
- Es coherente con las marcas BP (da igual cuántas anclas/capas: AN3=AN4=AP3=EM al 75%).
- **Alternativa descartada:** promedio ponderado por meta/dificultad de cada ancla. Lo descarto porque
  no hay dato que lo pida y agregaría un grado de libertad sin sustento. El promedio plano es el
  mínimo defendible.

### 4.5 Soporte — forma funcional de la asimetría

`core_L ← (1 - p_sop)·core_L + p_sop·sup_L`, con `p_sop`=0.25.

- **Reproduce las marcas SO** (7/7): en los **bordes de banda** el soporte voltea el escalón (SO1 full=P
  vs SO2 bajo=EM; SO5 full=EM vs SO6 bajo=A), pero **en banda media no mueve** (SO3=SO4=EM). Eso sale
  solo: cerca de un corte, restar 0.25·(1−sup) cruza la línea; lejos, no.
- **La asimetría "castiga más que premia" emerge del clamp + del nivel de las anclas:** cuando las
  anclas ya están altas (núcleo ≈1), el premio del soporte full choca con el techo (no sube nada),
  pero el descuido del soporte sí resta de ese valor alto. Por eso descuidar un soporte con anclas
  perfectas baja un escalón entero (SO2), mientras tenerlo full no agrega sobre 100% (SO1=P porque ya
  estaba en P). **No hace falta una forma explícitamente asimétrica:** la lineal + clamp + el hecho de
  que los soportes acompañan a anclas altas produce la asimetría observada. (Probé la forma
  explícitamente asimétrica `core + b·sup − p·(1−sup)`; ajusta igual de bien — la lineal es más simple
  y se queda.)

### 4.6 Magnitud exacta de multiplicadores/porciones

Calibrados a la región **más robusta** (máximo margen mínimo a los cortes) que da 45/45 con la
estructura Forma A: `k_sleep`=1.5, `k_sobr`=3.0, `beta`=0.45, `s_bad`=0.20, `r_relapse`=`r_unmarked`=0.50,
`p_sop`=0.25, cortes 0.40/0.62/0.86. Notas de identificabilidad (§5): `k_sleep`∈{1.0,1.5,2.0} y
`k_sobr`∈{2.0,2.5,3.0} dan 45/45 *si* se ajustan beta/cortes en conjunto — los datos **no fijan un
único valor**, fijan una región. Elegí el centro robusto.

### 4.7 Higiene digital — ancla de Conducta normal (resuelto en el plan)

En el motor, Higiene digital es **una ancla de Conducta más**: entra a `a_Co` como cualquier ancla,
con el peso de Conducta, y **no toca Sueño**. Su vínculo con Sueño es **solo UI** (pantallas antes de
dormir) y no entra al cálculo. El modelo A no le da tratamiento especial: cero matrioshka en los pesos.

---

## 5. Verificación contra las 45 marcas

Script descartable independiente: `/tmp/modelo-A-verify-final.py` (reimplementa el modelo A desde
cero; **NO importa ni modifica** `scripts/scoring/`). Copia las 45 `CASES` literalmente de
`weight_model_fit_v2.py`.

**Resultado: 45/45 marcas reproducidas.** Distribución: R=1, A=7, EM=21, P=11, I=5.

| Lote | Reproduce | Notas |
|------|-----------|-------|
| BP (base pura, 5 capas) | 11/11 | promedio plano, ω=0, sin worst-term — peor capa NO colapsa |
| SU (sueño modulador) | 9/9 | sueño domina el valor de Cuerpo; mal/none topean EM con anclas 100% |
| SBR (sobriedad) | 10/10 | recaída plana indep. del largo; SB9 (Conducta floja) → A por peso ×3 |
| SO (soportes/tasks) | 7/7 | soporte mueve en bordes, no en banda media; tasks neutras (no entran) |
| IN (Inquebrantable) | 8/8 | I ⟺ superhabit en ≥2 capas; magnitud no decide (IN6 cap1→P, IN7 cap5→I) |

**Márgenes ajustados al corte** (los más finos, para sinceridad): SB9 (S=0.55, margen al cAEM≈0.07),
SU5 (S=0.644, margen al cAEM≈0.024), SU6 (S=0.596, margen al cRA≈0.024). El modelo separa las 45 con
margen mínimo ≈ 0.011 — es ajustado pero **limpio (cero fallos)**.

**Casos sin dato que el modelo igual decide por diseño** (no son marcas, son consecuencias):
- **Ambos opt-in juntos:** ver §4.3 — el sueño malo deja de topear EM cuando hay sobriedad limpia
  activa. Es la única consecuencia del modelo que un humano podría querer revisar; queda señalada.
- **Tasks:** neutras por construcción (no entran a `v_L` ni a los pesos). Reproduce SO3=SO7.

**Honestidad sobre la unificación con los 43 casos viejos:** el dataset trae 43 marcas viejas
(sueño-on, 3 capas). El refit oficial (`weight_model_fit_v2.py`, Fase 2) muestra que **no existe un
punto único que ajuste limpio los 88** con ω=0 (rompe 5 viejas además de las 3 conocidas). El modelo A
**no fuerza** esa unificación: se calibra a las **45 marcas vigentes** (el reencuadre opt-in re-ancló la
base). Las viejas quedaron subsumidas por el nuevo marco; perseguir las 88 a la vez reintroduciría el
worst-term que las marcas BP prohíben. Lo digo explícito: **45/45 vigentes, no 88/88.**

---

## 6. Tradeoffs y riesgos

**A favor.**
- **Estructura mínima:** un solo mecanismo (multiplicador fijo + normalización) cubre N, el apretón y
  la combinación de opt-in. Cero reglas-parche; todo emerge de peso × valor.
- **El apretón no existe** por construcción (la capa libre nunca se ahoga).
- **Escala a N sin re-derivar nada:** la Forma A define el comportamiento a N=4,5 sin inventar datos.
- **45/45** con un punto robusto (no en el filo de un solo corte).

**Riesgos / costos.**
1. **Dilución del primer opt-in (§4.3):** la consecuencia más discutible — sueño malo deja de topear
   EM si hay sobriedad limpia activa. Es coherente con la Forma A pero puede chocar con "sueño = el
   peso más pesado". **El consolidado debe decidir** si se acepta o si se mezcla con un término
   aditivo de sueño (ya no Forma A pura).
2. **Identificabilidad floja:** los datos fijan una *región*, no un punto. `k_sleep`, `k_sobr`, `beta`,
   `s_bad` son parcialmente intercambiables (§4.6). Riesgo: dos calibraciones "45/45" pueden divergir
   en zonas sin marca (N=4, anclas múltiples por capa con moduladores). **Faltan marcas** en esas zonas.
3. **Márgenes finos** (≈0.011 al corte en SU5/SU6): pequeños cambios de calibración pueden voltear
   esos casos borde. Es inherente a tener bandas y datos densos cerca de los cortes, no exclusivo de A.
4. **`mal`=`none` y recaída plana** son decisiones de baja confianza heredadas del dataset (medición de
   sueño aún tosca; largo de racha binario). Revisar cuando mejore la medición de sueño.
5. **No unifica con las 43 viejas** (§5). Asumido: el reencuadre opt-in las deja como histórico, no como
   target. Si el dueño quisiera honrarlas todas, habría tensión estructural (worst-term vs ω=0).

**Veredicto.** Forma A es la opción que **menos inventa**: un mecanismo, cero reglas, 45/45, y el
apretón resuelto de raíz. Su punto débil único y honesto es la dilución mutua de los opt-in cuando
ambos están activos — la zona sin datos. Recomiendo llevarla al consolidado con esa decisión marcada
en rojo.
