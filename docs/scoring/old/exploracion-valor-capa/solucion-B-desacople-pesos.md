# Solución B — Desacople de pesos base/extra

> **Proponente a ciegas (sesgo: MODIFICAR LOS PESOS).** Resuelve el trilema del arrastre
> del opt-in jugando con el **peso de capa**, pero **desacoplando** el peso del canal *base*
> del peso del canal *extra*. Verificación reproducible:
> `python3 solucion_B_desacople_pesos.py` (script hermano, salida real pegada en §4).
> Fundación: modelo **v3 de §1** del planteamiento (estado = base + extra, **sin** `EM_TOP`).

---

## 1. El modelo completo

### 1.1 La idea en una frase

El opt-in **sí** le da más peso a su capa para arrastrar hacia abajo, pero ese peso extra se
aplica **solo al canal base** (¿está en pie?). El canal **extra** (superhabit) se agrega
**siempre con pesos iguales**. Resultado: el opt-in arrastra fuerte y **sin tope 1/N**
(porque su capa pesa `DRAG_BASE`, fijo, no `1/N`), pero el superhabit queda **inmune** a ese
peso (Sol = Tin), y las anclas conservan su valor (`K_INT` queda **moderado**, no infla).

### 1.2 Dos canales por capa (sin cambios respecto al eje)

Cada capa produce dos números independientes:

| Canal | Símbolo | Rango | Qué mide | Quién contribuye |
|-------|---------|-------|----------|------------------|
| **Base** | `en_pie(L)` | `[0, 1]` | ¿está en pie? | anclas (topadas a 1) **+** opt-in vía `K_INT` |
| **Extra** | `destaco(L)` | `[0, ∞)` | superhabit | **SOLO** anclas (excedente sobre 1) |

```
en_pie(L):
    ab = promedio de min(r,1) de las anclas        # bloque ancla, ≤1 (None si no hay)
    si hay opt-in M:
        core = (ab + K_INT·g(M)) / (1+K_INT)        # mezcla moderada
        si no hay anclas: core = g(M)                # capa solo-opt-in: valor = señal
    si no: core = ab (o 0)
    en_pie = min(core, 1)                            # base topada a 1

destaco(L) = promedio de max(r-1, 0) de las anclas  # 0 si no hay anclas; opt-in NUNCA da extra
```

- `g(M) = M` (curva del opt-in lineal: `FLOOR_OPT=0`, `Q_OPT=1`). `M ∈ [0,1]`.
- Sueño: `M` continuo `[0,1]` (sin dato → `B_SLEEP`, no 0). Sobriedad: `M = ∏ tracks` (binaria
  multi-track; **1 recaída no se diluye**: `signal([T,F,T]) = 0`).

### 1.3 El desacople — la agregación (ACÁ está el cambio)

```
estado = BASE + EXTRA

BASE  = Σ  w_base(L) · en_pie(L)         # pesos de ARRASTRE (capa-opt-in pesa más)
EXTRA = ( Σ destaco(L) ) / (nº capas con anclas)   # pesos IGUALES (promedio simple)
```

**Pesos del canal base** (`w_base`):

```
w_base(L):
    sea N = nº de capas activas, k = nº de capas con opt-in
    si k == 0  (no hay opt-in)        → 1/N  para todas         # idéntico a v3
    si todas son opt-in (N-k == 0)    → 1/N  para todas         # degenerado, simétrico
    si no:
        cada capa-opt-in              → DRAG_BASE  (FIJO)        # ← el arrastre
        cada capa sin opt-in          → (1 - DRAG_BASE·k)/(N-k)  # se reparten el resto
```

**Pesos del canal extra:** SIEMPRE iguales (promedio simple sobre las capas con anclas).
Esto es lo que hace al superhabit **inmune** a `DRAG_BASE`.

### 1.4 De dónde sale `DRAG_BASE` (se despeja de un axioma de estado)

`DRAG_BASE` **no se inventa**: se despeja del **axioma de arrastre** —"opt-in en su piso
(`M=0`), todo lo demás perfecto → borde de Atención (0.62)"—. Con 3 capas, las dos sin
opt-in en `en_pie=1` y la de opt-in en `ep_opt(0) = (1 + K_INT·0)/(1+K_INT) = 1/(1+K_INT)`:

```
estado = DRAG_BASE·ep_opt(0) + (1-DRAG_BASE)·1 = 1 - DRAG_BASE·(1 - ep_opt(0))
target = 0.62
⇒  DRAG_BASE = (1 - target) / (1 - ep_opt(0))
```

Con `K_INT=4.0` → `ep_opt(0)=0.200` → **`DRAG_BASE = (1-0.62)/(1-0.20) = 0.4750`**.

> **La clave del arrastre sin dilución:** `DRAG_BASE` es **fijo** (no `1/N`). Una capa-opt-in
> pesa 0.475 en la base tanto si hay 3 capas como si hay 7. Por eso el golpe del mal
> sueño/recaída **no se diluye** con N (ver §4, arrastre PLANO 0.677 de N=3 a N=7).

---

## 2. Justificación del sesgo (modificar los pesos)

El planteamiento describe un trilema:

1. **Arrastre topado a 1/N** — porque el opt-in vive dentro de una capa que pesa `1/N`.
2. **Subir `K_INT` mata las anclas** — el opt-in las arrincona al 6%.
3. **Inflar el peso de la capa distorsiona el superhabit** — un superhabit en Cuerpo valdría
   más que en Interior.

El sesgo de pesos ataca (1) directamente: si la capa-opt-in pesa **más que 1/N** en la base,
el arrastre deja de estar topado. Pero darle más peso a la capa "a secas" caería en (3). **La
jugada fina es el desacople:** ese peso extra vive **solo en el canal base**. El canal extra
sigue con pesos iguales, así que el superhabit no sabe nada de `DRAG_BASE` → **Sol = Tin**
(C5) por construcción.

Y como el arrastre ahora viene del **peso de capa** (no de matar las anclas adentro),
`K_INT` puede quedarse **moderado** (4.0 → anclas conservan el 20% del core, no el 6%). Eso
resuelve (2). Tres palancas, un solo movimiento limpio: **separar el peso de la base del peso
del extra.**

---

## 3. Parámetros calibrables

| Parámetro | Valor | Qué controla | Cómo se calibra |
|-----------|-------|--------------|-----------------|
| `K_INT` | `4.0` | peso del opt-in **dentro** de su capa (mezcla con anclas) | **moderado a propósito**: 20% anclas / 80% opt-in. Subirlo arrincona las anclas (trilema-2); bajarlo deja a las anclas mandar dentro de la capa. |
| `TARGET_DRAG` | `0.62` | estado objetivo del axioma de arrastre (opt-in en piso, resto perfecto) | es el borde Atención. Bajarlo → arrastre más agresivo (`DRAG_BASE` sube). |
| `DRAG_BASE` | `0.4750` (derivado) | peso de capa-opt-in **en el canal base** (FIJO, sin 1/N) | **no se fija a mano**: se despeja de `TARGET_DRAG` y `K_INT` (§1.4). |
| `DELTA` | `0.10` | holgura Plenitud → Inquebrantable | igual que v3. |
| `g(M)` | `M` lineal | curva del opt-in | `FLOOR_OPT=0`, `Q_OPT=1`. Una `Q>1` castigaría más el sueño regular. |
| `B_SLEEP` | `0.50` | base de sueño sin dato | sin dato no penaliza a fondo. |

**Invariante de diseño:** `DRAG_BASE` se recalcula si se cambia `K_INT` o `TARGET_DRAG`. No es
un número mágico; es la solución de una ecuación de un axioma del dueño.

---

## 4. Verificación — los 8 casos (salida REAL de python3)

### 4.1 Tabla ANTES (v3) vs DESPUÉS (Solución B)

| Caso | v3 (antes) | **Sol. B (después)** | Veredicto |
|------|-----------|----------------------|-----------|
| **C1** P1 justo + sueño bien (N=3) | 1.000 Plenitud | **1.000 Plenitud** | ✅ igual |
| **C3** P2 mal sueño M=.15 **N=3** | 0.773 En marcha | **0.677 En marcha** | ✅ arrastra más |
| **C3** P2 mal sueño M=.15 **N=5** | 0.864 Plenitud | **0.677 En marcha** | ✅ arrastra mucho más, **sin diluir** |
| **C3** P3 recaída M=0 **N=3** | 0.733 En marcha | **0.620 En marcha** | ✅ arrastra más |
| **C3** P3 recaída M=0 **N=5** | 0.840 En marcha | **0.620 En marcha** | ✅ arrastra más, **plano** |
| P4 sueño regular M=.5 N=5 | 0.920 Plenitud | **0.810 En marcha** | ✅ arrastre intermedio |
| **C4** P5 anclas conservan valor | 6% con K agresivo ❌ | **20% del core (0.170)** | ✅ resuelto |
| **C5** P6 Sol vs Tin | 1.144 = 1.144 ✅ | **1.144 = 1.144** | ✅ empatan exacto |
| **C6** P7 superhabit repartido | 1.432 Inquebrantable | **1.432 Inquebrantable** | ✅ igual |
| P8 capa solo-opt-in sueño bien | 1.000 (valor=señal) | **1.000, extra=0** | ✅ valor = señal |

**Titular del arrastre (C3):** el golpe del opt-in es **PLANO en N** — mal sueño da **0.677**
de N=3 a N=7. En v3 iba de 0.773 (N=3) a 0.864+ (N=5, ya entraba a Plenitud). La dilución por
N **desaparece** porque `DRAG_BASE` es fijo.

### 4.2 Salida REAL del script (pegada tal cual)

```
======================================================================================
SOLUCIÓN B — DESACOPLE. K_INT=4.0 (anclas 20% / opt-in 80% dentro de capa)
DRAG_BASE=0.4750 (FIJO, no depende de N)  ep_opt(M=0)=0.200
bandas: REST<0.40 · ATEN<0.62 · EN MARCHA<0.85 · PLENITUD<1.10 · INQUEBRANTABLE>=1.10
======================================================================================
J(justo)=1.000  SUP(6d)=1.266  XL(60x7)=1.432  JI(déficit)=0.636

--- 8 CASOS DE PRUEBA ---
P1 justo+sueño bien N=3                    base=1.000 extra=0.000 estado=1.000 -> PLENITUD
P2 mal sueño M=.15 N=3                     base=0.677 extra=0.000 estado=0.677 -> EN MARCHA
P2 mal sueño M=.15 N=5                     base=0.677 extra=0.000 estado=0.677 -> EN MARCHA
P3 recaída M=0 N=3                         base=0.620 extra=0.000 estado=0.620 -> EN MARCHA
P3 recaída M=0 N=5                         base=0.620 extra=0.000 estado=0.620 -> EN MARCHA
P4 sueño regular M=.5 N=5                  base=0.810 extra=0.000 estado=0.810 -> EN MARCHA

--- P5 anclas conservan valor (sueño mal M=0.15) ---
   1 ancla estado=0.677 | 3 anclas estado=0.677  (nº anclas no cambia peso: True)
   en_pie con anclas=0.320 vs sin anclas=0.150 → anclas aportan 0.170 (20% real, NO 6%)

--- P6 Sol vs Tin (superhabit en distinta capa) DEBEN EMPATAR ---
   Sol superhabit Interior                 base=1.000 extra=0.144 estado=1.144 -> INQUEBRANTABLE
   Tin superhabit Cuerpo+sueño             base=1.000 extra=0.144 estado=1.144 -> INQUEBRANTABLE
   EMPATAN? True  (dif=0.00e+00)

P7 superhabit repartido 3 capas            base=1.000 extra=0.432 estado=1.432 -> INQUEBRANTABLE
P8 capa solo-opt-in sueño bien            base=1.000 extra=0.000 estado=1.000 -> PLENITUD

--- C2 NEUTRALIDAD (opt-in BIEN no cambia el score) ---
   anclas PERFECTAS: con opt-in bien=1.0000 | sin=1.0000 | neutro=True
   anclas DÉFICIT:   con opt-in bien=0.7744 | sin=0.6361 | dif=0.138 (tensión, ver doc)

--- ARRASTRE POR N (debe ser PLANO) ---
   N=3: mal sueño=0.677 EN MARCHA
   N=4: mal sueño=0.677 EN MARCHA
   N=5: mal sueño=0.677 EN MARCHA
   N=6: mal sueño=0.677 EN MARCHA
   N=7: mal sueño=0.677 EN MARCHA
```

### 4.3 Lectura criterio por criterio

- **C1** ✅ P1 = 1.000 Plenitud (con anclas y opt-in perfectos `en_pie=1`, `DRAG_BASE` es
  irrelevante porque todas las capas valen 1; `estado = 1·base = 1`).
- **C2** ✅ (en el caso canónico) opt-in BIEN con anclas perfectas = 1.0000 = sin opt-in. Ver
  tensión en §5 para anclas en déficit.
- **C3** ✅✅ arrastre fuerte **y plano**: 0.677 (sueño) / 0.620 (recaída) **idénticos de N=3 a
  N=7**. La dilución por N murió.
- **C4** ✅ las anclas aportan **0.170 al core** (20% real); el nº de anclas no cambia el peso.
- **C5** ✅ Sol = Tin = 1.144, **diferencia 0.00e+00** (el extra ignora `DRAG_BASE` por
  construcción).
- **C6** ✅ superhabit repartido = 1.432 Inquebrantable; cumplir justo = Plenitud.
- **P8** ✅ capa solo-opt-in: valor = señal, extra = 0.

---

## 5. Tensiones honestas

1. **C2 estricta se rompe con anclas en DÉFICIT** (no solo en mi modelo — **v3 también**).
   Cuando las anclas de la capa-opt-in están en déficit (`JI=0.636`) y el opt-in está BIEN
   (`M=1`), el opt-in perfecto **tira hacia arriba** las anclas vía `K_INT`
   (`core=(0.636+4)/5=0.927`), así que el estado sube vs no tener opt-in. Mi modelo: 0.774 vs
   0.636 (dif 0.138). **v3 hace lo mismo**: 0.733 vs 0.636 (dif 0.097) — lo verifiqué corriendo
   el v3 de §1. Es una propiedad inherente del mixing `K_INT`, no un defecto que introduzca el
   desacople. El desacople lo **amplifica** un poco (×~1.4) porque la capa-opt-in además pesa
   más en la base. **C2 se cumple exacto solo cuando las anclas están en 1** (que es como está
   redactado el axioma: "opt-in BIEN = neutro"). Si el dueño quiere neutralidad ESTRICTA con
   anclas en déficit, hay que cambiar el mixing (`max` en vez de promedio ponderado, o capar
   `g(M) ≤ ab`), pero eso toca el modelo del ancla, fuera del sesgo de pesos.

2. **`DRAG_BASE` puede volverse negativo con muchas capas-opt-in.** Con `DRAG_BASE=0.475`, dos
   capas-opt-in ya consumen 0.95 de la base; tres (1.425) dejarían a las capas-ancla con peso
   negativo. El código degrada a pesos iguales si `N-k==0`, pero el caso "2 opt-in + 1 ancla"
   da `w_other = 1 - 0.95 = 0.05` (la capa-ancla casi no pesa en la base). Es coherente con la
   intención (dos pilares opt-in caídos = casi todo el peso), pero conviene **capar
   `DRAG_BASE·k ≤ algún tope** (p.ej. 0.85) si se esperan ≥2 opt-ins simultáneos. Calibrable.

3. **El arrastre es PLANO, no decreciente.** Es lo pedido (poco diluido por N), pero significa
   que mal sueño pesa lo mismo con 3 anclas que con 30 actividades repartidas en 7 capas. Si el
   dueño quisiera que "más estructura amortigüe un poco el golpe", habría que hacer `DRAG_BASE`
   levemente decreciente en N (p.ej. interpolar entre fijo y 1/N). Hoy elegí PLANO porque el
   criterio C3 pedía "idealmente parejo entre N=3 y N=5".

4. **Asimetría base/extra como elección, no como ley.** El desacople trata base y extra con
   reglas de peso distintas. Es deliberado y es lo que hace funcionar Sol=Tin, pero rompe la
   "elegancia" de una única agregación. Quien revise debe aceptar que **base y extra son dos
   espacios con su propia métrica de peso** (base = ¿quién manda en pie?, extra = mérito puro
   sin jerarquía de capa).

---

## 6. Resumen ejecutivo

- **Qué cambia vs v3:** una sola cosa — el canal **base** usa un peso de capa **`DRAG_BASE`
  fijo** para la capa-opt-in (despejado de un axioma), mientras el canal **extra** sigue con
  pesos iguales. Todo lo demás (caja del ancla, `K_INT` moderado, `estado=base+extra`,
  bandas) **idéntico a v3**.
- **Qué resuelve:** arrastre fuerte y **plano en N** (C3), anclas conservan 20% (C4),
  superhabit inmune Sol=Tin (C5), Plenitud/Inquebrantable intactos (C1, C6).
- **Costo honesto:** C2 estricta solo con anclas en 1 (heredado de v3, amplificado); cuidar
  `DRAG_BASE·k` con múltiples opt-ins.
- **Reproducible:** `python3 solucion_B_desacople_pesos.py`.
