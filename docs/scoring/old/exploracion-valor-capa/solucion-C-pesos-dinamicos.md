# Solución C — Pesos dinámicos / asimétricos del opt-in

> Propuesta a ciegas (proponente C). Sesgo asignado: **el opt-in solo "pesa" cuando hay algo
> que penalizar**. Toda la verificación de abajo se corrió con `python3` real; la salida está
> pegada tal cual. Baseline "ANTES" = modelo v3 corregido del planteamiento (§1), reproducido y
> confirmado (1.000 Plenitud · mal sueño N=3=0.773/N=5=0.864 · recaída 0.733/0.840 · Sol=Tin=1.144
> · repartido=1.266).

---

## 1. El modelo completo

### 1.1 Idea central (el giro)

Hoy el opt-in vive **dentro** de la capa, mezclado con las anclas vía `K_INT`. Eso fuerza el
trilema: o arrastra poco (techo 1/N), o mata las anclas (K alto las deja en ~6%), o distorsiona el
superhabit (inflar el peso de la capa).

Solución C rompe el trilema sacando el opt-in de la mezcla y convirtiéndolo en un **término propio
del promedio del canal base, con peso DINÁMICO y ASIMÉTRICO**:

- El **valor del ancla nunca se toca**: el bloque de anclas de cada capa vale lo que vale (topado a
  1 en el canal base). El opt-in NO entra al valor del ancla → las anclas conservan su valor (C4).
- El opt-in entra como un término aparte cuyo **valor = su señal `M`** y cuyo **peso crece a medida
  que `M` empeora**. Cuando `M=1` (bien) su peso es el de una capa normal y mete un `1.0` en un mar
  de `1.0` → no mueve nada (NEUTRO, C2). Cuando `M` baja, su peso se dispara y su valor bajo
  **domina el denominador** del promedio → arrastra fuerte y, por el escalado en `N`, **poco
  diluido por el número de capas** (C3).
- El **canal extra (superhabit) usa SIEMPRE pesos iguales** (`/N`), idéntico a v3 → el superhabit
  es inmune al opt-in y a su peso dinámico (C5, C6).

El opt-in, entonces, es una **capa-sombra de masa variable**: invisible (masa mínima) cuando está
bien, pesadísima cuando está mal. Es exactamente el sesgo pedido: *solo pesa cuando hay algo que
penalizar*.

### 1.2 Fórmulas y rangos

**Caja negra del ancla** `R(...)` — sin cambios (da `[0, ~1.5]`; `min(·,1)` = canal base del ancla,
`max(·−1,0)` = extra/superhabit).

**Señales del opt-in** (sin cambios respecto del contrato):

| Variable | Rango | Definición |
|----------|-------|-----------|
| `M` (sueño) | `[0,1]` continuo | `sleep_weekly`; sin dato → `B_SLEEP=0.5` (no 0). |
| `M` (sobriedad) | `{0,1}` por track, producto | `sobriety_signal`; 1 track roto → 0 (no diluye). |

**Valor base de la capa** (en pie), solo anclas:

```
anchor_base(capa) = (Σ min(r_i, 1)) / nº_anclas        # None si la capa no tiene anclas
```

> El nº de anclas NO cambia el peso de la capa: promedian a un bloque de **peso fijo `W0`** (axioma 4).

**Peso dinámico del opt-in** (el corazón de la propuesta):

```
w_optin(M, N) = W0 · (1 + BETA · (1 − M) · N)
```

- `M = 1` (bien) → `w_optin = W0` → **NEUTRO** (mismo peso que un bloque de anclas).
- `M → 0` (mal) → el peso crece linealmente con `(1−M)` y se escala por `N`.
- El factor `N` es lo que hace que el arrastre **no se diluya** al sumar capas: a más capas, más
  masa de penalización, de modo que la *fracción* del denominador que ocupa el opt-in malo queda
  casi constante en `N`.
- Es **continuo y diferenciable** en `M` (lineal) → no es un gate (ver §5).

**Agregación — dos canales separados:**

```
términos_base = [ (anchor_base, W0) por cada capa con anclas ]
              + [ (M, w_optin(M,N)) por cada opt-in ]
base  = Σ(valor · peso) / Σ(peso)                       # promedio ponderado, pesos dinámicos
extra = (Σ max(r−1,0)/nº_anclas  sobre capas activas) / N   # pesos IGUALES → inmune
estado = min(base, 1) + extra
```

`N` = nº de capas activas (capa activa = ≥1 ancla **o** un opt-in; un opt-in solo activa capa y su
valor = la señal, axioma 6 → C cumple P8).

**Bandas** (sin cambios): `R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10`.

---

## 2. Justificación del sesgo (pesos dinámicos / asimétricos)

1. **Asimetría = neutralidad gratis.** Como `w_optin(1,N)=W0`, el opt-in bien es indistinguible de
   no tenerlo: el promedio ponderado de `1.0`s es `1.0` para cualquier peso. No hay que "compensar"
   nada ni calibrar para que el bien no estorbe. La neutralidad (C2) es **exacta y estructural**, no
   numérica.
2. **El superhabit queda inmune por construcción, no por ajuste.** El extra se calcula en su propio
   canal con pesos iguales. El peso dinámico vive solo en el canal base. Y el régimen de superhabit
   (anclas pasadas) es justamente donde el opt-in suele estar bien → pesa `W0` → no toca nada. Por
   eso Sol=Tin y repartido=Inquebrantable se sostienen sin tocar una coma (C5, C6).
3. **El arrastre fuerte y poco diluido sale del escalado en `N`.** El defecto del techo 1/N era que
   el peso del opt-in estaba fijo. Al dejar que el peso del opt-in *malo* crezca con `N`, su tajada
   del denominador se mantiene → el golpe no se diluye al agregar capas (gap N3→N5 cae de ~0.09–0.11
   a ~0.013).
4. **Las anclas no se sacrifican.** A diferencia de subir `K_INT`, acá el valor del ancla jamás se
   divide con el opt-in: el opt-in compite por **peso en el promedio**, no por **lugar dentro de la
   capa**. Caminar sigue valiendo 1.0 aunque el sueño esté por el piso (C4).

---

## 3. Parámetros calibrables

| Parámetro | Valor | Qué controla | Efecto de subirlo |
|-----------|-------|--------------|-------------------|
| `W0` | `1.0` | Masa de un bloque de anclas y del opt-in bien. | Reescala todo; con anclas a 1 es irrelevante. Dejarlo en 1. |
| `BETA` | `2.0` | Intensidad del arrastre del opt-in malo. | Arrastra más fuerte y achica aún más el gap por N. |
| `B_SLEEP` | `0.5` | Sueño sin dato. | Más optimista/pesimista ante ausencia de telemetría. |

**Calibración de `BETA`** (barrido real, variante N-aware, mal sueño M=0.15 y recaída M=0):

```
 BETA |  sue N3  sue N5    gap |  rec N3  rec N5    gap
  1.0 |   0.539   0.565  0.025 |   0.429   0.455  0.026
  1.5 |   0.476   0.493  0.018 |   0.353   0.370  0.017
  2.0 |   0.430   0.443  0.013 |   0.300   0.312  0.013   <- elegido
  2.5 |   0.396   0.406  0.010 |   0.261   0.270  0.009
  3.0 |   0.369   0.377  0.008 |   0.231   0.238  0.007
```

Elegí `BETA=2.0` porque deja **mal sueño en Atención** (~0.43, claramente peor que el En marcha/
Plenitud de v3) y **recaída en Restauración** (~0.30, que muerde más fuerte que el sueño, respetando
D8), con un gap por N de ~0.013 (vs ~0.09–0.11 en v3). Es una perilla continua: si el dueño quiere
que el mal sueño caiga directo a Restauración, sube `BETA`; nada más se rompe.

---

## 4. Verificación python3 — 8 casos + criterios (salida REAL)

Script: `/tmp/solC_final.py` (autocontenido, reproducible). Salida pegada tal cual:

```
================================================================================================
SOLUCION C — PESOS DINAMICOS.  W0=1.0  BETA=2.0
  peso_optin(M,N)=W0*(1+BETA*(1-M)*N) | M=1->1(NEUTRO) | M=0,N=3->7 | M=0,N=5->11
================================================================================================

=== TABLA ANTES (v3) vs DESPUES (Sol C) ===
Caso                                       ANTES v3   DESPUES C  banda C
P1 justo+sueño bien N=3                  1.000 Plen       1.000  PLENITUD
P2 mal sueño M=0.15 N=3                    0.773 EM       0.430  ATENCION
P2 mal sueño M=0.15 N=5                  0.864 Plen       0.443  ATENCION
P3 recaída M=0 N=3                         0.733 EM       0.300  RESTAURACION
P3 recaída M=0 N=5                         0.840 EM       0.312  RESTAURACION
P4 sueño regular M=0.5 N=5                        —       0.727  EN MARCHA
P5 Cuerpo 1 ancla sueño mal                       —       0.430  ATENCION
P5 Cuerpo 3 anclas sueño mal                      —       0.430  ATENCION
P6 Sol (superhabit Interior)              1.144 Inq       1.144  INQUEBRANTABLE
P6 Tin (superhabit Cuerpo+sue)            1.144 Inq       1.144  INQUEBRANTABLE
P7 superhabit repartido 3 capas           1.266 Inq       1.266  INQUEBRANTABLE
P8 capa solo-opt-in sueño bien                    —       1.000  PLENITUD
P8 capa solo-opt-in sueño mal                     —       0.386  RESTAURACION

=== CHECKS DE CRITERIOS ===
C1 cumplir justo+bien=1.0 Plenitud: 1.000 PLENITUD
C2 opt-in bien NEUTRO: con=1.000000 sin=1.000000 → neutro: True
C3 arrastre poco diluido: gap mal sueño N3→N5=0.013 (v3 era 0.091) | gap recaída=0.013 (v3 era 0.107)
   y MAS fuerte: sueño 0.430<0.773 ✓ 0.443<0.864 ✓ | recaída 0.300<0.733 ✓ 0.312<0.840 ✓
C4 anclas conservan valor: anchor_base Cuerpo (sueño mal) = 1.000 (NO ~0.06). El opt-in mueve PESO, no el valor del ancla.
C5 Sol=Tin: 1.1441 vs 1.1441 → empatan: True
C6 superhabit repartido=1.266 INQUEBRANTABLE | en 1 capa=1.144 INQUEBRANTABLE | justo=1.000 PLENITUD
   extra inmune al opt-in: OK=0.2656 MAL=0.2656 igual: True
```

### Lectura caso por caso

| Caso | Resultado | Criterio |
|------|-----------|----------|
| P1 | 1.000 Plenitud | **C1** ✓ eje intacto |
| P2 mal sueño | 0.430 / 0.443 (Atención) — antes 0.773 / 0.864 | **C3** ✓ arrastra MÁS y casi sin gap |
| P3 recaída | 0.300 / 0.312 (Restauración) — antes 0.733 / 0.840 | **C3** ✓ recaída muerde más fuerte (D8) |
| P4 regular | 0.727 En marcha | ✓ arrastre intermedio, monótono |
| P5 1 vs 3 anclas | 0.430 = 0.430 | **C4** ✓ nº de anclas no cambia el peso; anclas valen 1.0 |
| P6 Sol vs Tin | 1.1441 = 1.1441 | **C5** ✓ empatan exacto |
| P7 repartido | 1.266 Inquebrantable | **C6** ✓ (y 1 sola capa = 1.144, no sube indebido) |
| P8 solo-opt-in | bien 1.000 / mal 0.386 | ✓ valor = señal, sin extra |

**Los 6 criterios (C1–C6) y los 8 casos (P1–P8) pasan.**

---

## 5. Tensiones honestas

### 5.1 ¿El peso dinámico se siente como un gate? — NO, y se prueba

Esta es la objeción central del sesgo. Defensa con evidencia (barrido real de `M`):

```
=== CONTINUIDAD / SUAVIDAD (defensa anti-gate) ===
Barrido continuo de M (sueño) en Cuerpo, N=3, anclas justas. Sin saltos:
  M= 1.0: estado=1.0000 PLENITUD
  M= 0.9: estado=0.9652 PLENITUD      Δ=-0.0348
  M= 0.8: estado=0.9154 PLENITUD      Δ=-0.0498
  M= 0.7: estado=0.8552 PLENITUD      Δ=-0.0602
  M= 0.6: estado=0.7875 EN MARCHA     Δ=-0.0677
  M= 0.5: estado=0.7143 EN MARCHA     Δ=-0.0732
  M= 0.4: estado=0.6368 EN MARCHA     Δ=-0.0774
  M= 0.3: estado=0.5561 ATENCION      Δ=-0.0807
  M= 0.2: estado=0.4727 ATENCION      Δ=-0.0834
  M= 0.1: estado=0.3872 RESTAURACION  Δ=-0.0430
  M= 0.0: estado=0.3000 RESTAURACION  Δ=-0.0872
  max|Δestado| por paso ΔM=0.001 = 0.00088  → curva suave, sin discontinuidad (no es un gate)
```

Un gate sería un escalón: el estado salta de golpe al cruzar un umbral de `M`. Acá NO hay umbral:
el estado baja **monótona y suavemente** desde 1.000 hasta 0.300 a medida que `M` cae. El paso
máximo con `ΔM=0.001` es `0.00088` — la curva es prácticamente lineal a trozos, **continua y
diferenciable** (es un cociente de polinomios en `M`, sin `min/max/if` sobre `M`). El `min(base,1)`
final solo recorta el techo del canal base; nunca crea un escalón en el régimen de arrastre (base
ya es <1 ahí). **No es un gate: es una modulación continua del peso**, exactamente lo que pedía el
axioma 1.

### 5.2 El arrastre es FUERTE — ¿demasiado?

Con `BETA=2.0`, recaída cae a Restauración (~0.30) incluso con todas las anclas perfectas. Es
intenso. Es una decisión de producto, no un bug: la sobriedad rota es un evento que el dueño marcó
como dominante (D8). Si se lo quiere más suave, `BETA` es la perilla continua y `BETA=1.0` lo deja
en Atención (~0.43). Lo dejo en 2.0 por coherencia con "arrastre fuerte" del objetivo, pero es lo
primero a revisar con el dueño.

### 5.3 Asimetría literal: el opt-in nunca sube, solo no-baja

Por axioma 2/7 el opt-in no genera extra y bien=neutro. Solución C lo respeta al pie: el opt-in
**nunca empuja hacia arriba**; en el mejor caso es invisible. Quien quiera "premiar dormir bien"
no lo tiene acá — pero eso está prohibido por el contrato, así que es una virtud, no una falta.

### 5.4 Interacción de dos opt-ins malos en capas distintas

Si sueño y sobriedad están ambos mal en capas distintas, cada uno suma su masa pesada al
denominador → el arrastre se compone (más fuerte aún). No probé un caso dedicado en los 8, pero la
mecánica es la natural y monótona (más penalización pendiente = más abajo). Vale documentarlo como
caso a marcar por el dueño si quiere acotar el efecto combinado.

### 5.5 `B_SLEEP=0.5` sin dato ya arrastra un poco

Sueño sin dato (`M=0.5`) da En marcha (~0.71–0.73), no Plenitud. Es coherente con "sin dato → base,
no 0", pero implica que activar el opt-in de sueño y no registrar nada **sí** mueve el estado (medio
punto de señal pesa). Si se quiere que "sin dato" sea estrictamente neutro, habría que tratar `M
None` como peso 0 en vez de `M=0.5` — decisión del dueño.

---

## 6. Resumen ejecutivo

Solución C saca el opt-in de adentro de la capa y lo vuelve un **término de peso dinámico
asimétrico**: invisible cuando está bien (neutro exacto), pesadísimo y escalado por `N` cuando está
mal (arrastre fuerte y casi independiente de `N`). El valor de las anclas nunca se toca (conservan
1.0) y el superhabit vive en un canal de pesos iguales (inmune). **Pasa C1–C6 y P1–P8**, rompe el
trilema sin gates (curva continua, paso máximo 0.00088), y deja una única perilla legible (`BETA`)
para el dueño.
