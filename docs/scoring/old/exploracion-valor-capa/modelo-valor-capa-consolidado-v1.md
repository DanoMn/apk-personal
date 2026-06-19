# Modelo del VALOR DE CAPA — consolidado v1

> **Estado: propuesta consolidada, verificada con python3 (12/12 casos límite).** Merge del
> orquestador sobre las 3 propuestas a ciegas (A aditivo · B cimiento suave · C unificación
> relacional) + research propio. **Sin calibrar** (los parámetros se despejan de axiomas de
> estado del dueño). Script reproducible: `modelo_consolidado.py` (misma carpeta).
> Fecha: 2026-06-12.

---

## 0-ERRATA (2026-06-12) — el factor EM_TOP=0.85 fue un ERROR, anulado

Las versiones previas de este doc usaban `ESTADO = EM_TOP·base + W_extra·extra` con `EM_TOP=0.85`,
haciendo que "cumplir todo justo" cayera en **En marcha**. **Eso CONTRADICE el modelo del ancla**
(`mapa-modelo-scoring-v1.md` §1: `P≥0.85`; `simulacion-capas-resultados.md`: EXACTO=1.0000 = "Pleno
exacto"). El eje correcto: **cumplir todo justo = score 1.0 = PLENITUD**; superhabit → Inquebrantable;
**En marcha (0.62–0.85) = cumplimiento PARCIAL**. Bandas: `R<0.40·A<0.62·EM<0.85·P≥0.85·I≥1.10`.
El `EM_TOP` queda eliminado; el valor de capa va en la misma escala del ancla `[0,~1.5]` y el score es
el promedio de los valores de capa. Toda referencia a EM_TOP abajo está OBSOLETA y se reescribe en v3.

## 0-bis. CAMBIO ESTRUCTURAL v2 (decisión del dueño, 2026-06-12) — pesos de capa IGUALES

> Supersede §2.4 y §2.5 abajo. El dueño decidió **eliminar el peso extra de capa por opt-in**.

- **Todas las capas activas pesan IGUAL: `peso = 1/N`.** El opt-in ya NO infla el peso de la capa
  en el total (se elimina `tamaño = 1+K`).
- **El opt-in pesa MUCHÍSIMO DENTRO de su capa** (K interno alto): `en_pie = (señal_anclas + K_INT·g(M))/(1+K_INT)`.
  Con K_INT despejado de "sin dormir → borde Atención" sale `K_INT≈4.31` ⟹ el sueño pesa **81%**
  de Cuerpo, las anclas 19%. Arrastra el valor de SU capa para "mostrar la verdad".
- **El extra se promedia SIMPLE** entre capas con anclas (ya no ponderado por peso de capa).
- **Por qué:** el peso de capa inflado causaba (a) rareza 3 (mismo superhabit rendía más en la capa
  con opt-in) y (b) mandaba a Inquebrantable a quien tenía una capa pesada con superhabit. Con pesos
  iguales ambas desaparecen: Sol y Tin empatan (0.936), e Inquebrantable exige reparto real
  (superhabit solo-en-Cuerpo → 0.936 Plenitud; repartido en 3 → 1.109 Inquebrantable). Verificado.
- **Efecto a confirmar:** el mal sueño arrastra proporcional a 1/N (N=3 → 0.655; N=5 → 0.733). A más
  capas, el opt-in pega más suave. Pendiente: el dueño confirma si está OK o sube K_INT.

## 1. El hallazgo central: convergencia independiente

Los 3 proponentes, a ciegas y con sesgos opuestos, **coincidieron en 9 decisiones estructurales**.
Cuando tres derivaciones independientes llegan a la misma forma, esa es la forma del problema:

1. **`ESTADO = EM_TOP·base + W_extra·extra`** — dos canales sumados, nunca multiplicados entre sí.
2. **base ∈[0,1]** = promedio K-ponderado de señales topadas en 1 (todas las capas).
3. **extra ≥0** = `max(R−1,0)` promediado, **solo de anclas**, agregado solo entre capas con anclas.
4. **El EJE es un cambio de escala lineal:** base llena → `EM_TOP` (=0.85 = tope de En marcha).
   Plenitud/Inquebrantable se alcanzan SOLO sumando extra. Sin gates, sin `if`.
5. **El opt-in NO se diluye con el nº de anclas:** mantiene `K/(1+K)` (las anclas promedian a un
   bloque de tamaño 1). **Los 3 lo decidieron igual** → responde la decisión abierta del dueño.
6. **Soportes y tasks → canal BASE, saturados** (8 soportes no fabrican una banda); task < soporte.
7. **Multi-sobriedad = potencia/producto** (cuenta recaídas, no tracks): 1 recaída pega igual con
   1 o N tracks; 2 pegan más. Sin `min()`.
8. **Sueño semanal = cobertura + base** (`c·avg + (1−c)·B_SLEEP`); sin dato → base, no 0.
9. **Pesos por un solo K por opt-in, relacionales a N** (bajan al crecer N). Nada fijo.

## 2. El modelo consolidado

### 2.1 Señales del opt-in `M ∈ [0,1]`
```
Sueño:     M = c·avg(noches con dato) + (1−c)·B_SLEEP      c = nº noches con dato / 7
           (sin ninguna noche → M = B_SLEEP)
Sobriedad: M = (1 − A_SOB)^(nº recaídas en los últimos 7 días)
```

### 2.2 Curva del opt-in (perilla de B, calibrable)
```
g(M) = FLOOR_OPT + (1 − FLOOR_OPT)·M^Q_OPT
```
Con `FLOOR_OPT=0, Q_OPT=1` ⟹ `g(M)=M` (aditivo puro, convergencia A/C). Subiendo `FLOOR_OPT`
(suelo) y `Q_OPT>1` (curva) se obtiene el "sabor dominó con piso" de B: plano arriba, acelera
abajo, sin aniquilar. **Default lineal; el dueño activa la curva si la quiere.**

### 2.3 Valor de una capa — dos canales
```
señal_anclas = promedio_i  min(R_i, 1)                    ∈ [0,1]   (None si no hay anclas)

en_pie(capa) = clamp(  core  +  sup_term  +  task_term ,  0, 1 )
  core = (1·señal_anclas + K·g(M)) / (1+K)   si hay anclas + opt-in
       = señal_anclas                        si hay anclas, sin opt-in
       = g(M)                                si solo opt-in (sin anclas)  → axioma 8

destaco(capa) = promedio_i  max(R_i − 1, 0)              ≥ 0   (0 si no hay anclas)
```
- `sup_term = ±SUP_SAT·(1−e^(−SUP_K·|2f−1|))` — f=fracción sostenida; **centrado** (full suma,
  descuidado resta), saturado, light.
- `task_term = TASK_SAT·(1−e^(−TASK_K·f))` — solo suma, `TASK_SAT < SUP_SAT` (task < soporte).

### 2.4 Pesos relacionales (un solo K por opt-in)
```
tamaño(capa) = [1 si tiene anclas] + [K si tiene opt-in]   (el nº de anclas NO cambia el tamaño)
peso(capa)   = tamaño(capa) / Σ tamaños                    → baja al crecer N (relacional)
```
Soportes y tasks **NO** entran al tamaño (son light, no deben mover el peso de la capa ni dar gaming).

### 2.5 Agregación global y estado
```
BASE_global  = Σ peso(capa)·en_pie(capa)
EXTRA_global = Σ_{capas con ancla} (peso/Σpeso_con_ancla)·destaco(capa)   (promedio ponderado)
ESTADO = EM_TOP · BASE_global + W_EXTRA · EXTRA_global
bandas: Rojo<0.40 · Atención<0.62 · En marcha≤0.85 · Plenitud<1.10 · Inquebrantable≥1.10
```

## 3. Qué se tomó y qué se arbitró de cada proponente

| Decisión | Elegido | De quién | Por qué |
|----------|---------|----------|---------|
| Esqueleto base+extra, eje por escala | aditivo `EM_TOP·base+W·extra` | A + C (convergen) | Suma mantiene base y extra independientes: el mal sueño no roba el mérito (objeción #1 del dueño). |
| Composición del opt-in en la base | promedio K-ponderado **lineal** | A + C | El efecto dominó ya emerge del **peso K alto**, sin multiplicar. Más simple y trazable. |
| Curva del opt-in (suelo+convexidad) | **perilla opcional** `g(M)` | B | Se conserva la mejor idea de B (degradar sin aniquilar) como calibración, no como estructura. |
| Soportes/tasks | aporte aditivo saturado a la BASE | A + B | **Rechazado** que sean "miembros del peso" (C): haría que soportes cambien el peso de la capa = gaming y contradice "light". |
| Soportes bidireccionales | `sup_term` centrado (±) | B | El dueño marcó que descuidar soportes BAJA (SO2); full sube (SO1). |
| Multi-sobriedad | `(1−A_SOB)^n_recaídas` | C (= producto de A/B) | Cuenta recaídas, no tracks. Forma más explícita. |
| Extra entre capas | promedio **ponderado** | A | Coherencia: si una capa pesa más, todo lo suyo (base y extra) cuenta proporcional. |
| Reparto interno opt-in | NO diluye, `K/(1+K)` | A + B + C (unánime) | Las anclas promedian a un bloque; diluir rompería la regla "nº anclas no cambia pesos". |

**Del research:** confirmó (a) que el aditivo (reward shaping: base + bonus saturado) es la forma
canónica de "premiar el extra sin tocar la base"; (b) que la curva de B es un **cuello de botella
suave** (familia CES/softmin) — el dial entre promedio y mínimo, sin el `min()` duro rechazado;
(c) saturación exponencial para multi-* y producto para multi-sobriedad.

## 4. Verificación — salida REAL de python3 (12/12)

`K despejados: K_sleep=1.113, K_sobr=2.819` (K_sobr>K_sleep ⟹ D8 emerge). Anclas de prueba:
R_justo=1.000, R_sup_medio=1.215, R_sup_fuerte=1.461.

```
1. Todo justo (R=1, sueño OK)        base=1.000 extra=0.000 estado=0.850 -> En marcha     [EJE ✓]
2. Superhabit medio repartido        base=1.000 extra=0.215 estado=0.979 -> Plenitud
2b. Superhabit FUERTE repartido      base=1.000 extra=0.461 estado=1.127 -> Inquebrantable
3. Capa solo-opt-in (sin anclas)     base=0.946 extra=0.000 estado=0.804 -> En marcha     [extra=0 ✓]
4. El apreton: 3 capas + 2 opt-ins   base=1.000 extra=0.000 estado=0.850   Σpesos=1.000000 ✓
5a. Sueño OK                         base=1.000 extra=0.215 estado=0.979 -> Plenitud
5b. ...sueño MALO (0.3)              base=0.811 extra=0.215 estado=0.818 -> En marcha
        extra igual? True   base baja? True     [el mérito sobrevive ✓]
6. Sueño SIN dato  -> M=0.500 (=B_SLEEP, no 0);  parcial 3 noches -> M=0.586
7a. Recaida DENTRO 7d                estado=0.500 -> Atencion
7b. Recaida FUERA 7d                 estado=0.850 -> En marcha     [dentro<fuera ✓]
8. Multi-sobriedad: 1 recaida=0.150 (igual con 1 o N tracks); 2 recaidas=0.023 (<) ✓
9. Soportes full vs descuidado: dif estado = 0.029 (light ✓)
10. Task=0.0346 < soporte=0.0692 ✓
11. size(1 ancla)=2.11 == size(3 anclas)=2.11 ✓
12. Peso opt-in: N=3:0.514  N=4:0.413  N=5:0.346 (baja con N ✓)
```

## 5. Parámetros y cómo se despejan (ninguno a dedo)

| Param | Rol | Cómo se fija |
|-------|-----|--------------|
| `K_sleep`, `K_sobr` | peso del opt-in (relacional) | **se DESPEJAN** de un axioma de estado del dueño (bisección). Ej: "sin dormir→Atención 0.62"→1.11; "recaída→0.50"→2.82. |
| `EM_TOP` | tope de En marcha = techo de la base sola (eje) | = corte de banda Plenitud (≈0.85) |
| `W_EXTRA` | cuánto sube el superhabit | axioma "superhabit X → Plenitud/Inq" |
| `A_SOB` | golpe por recaída | D8 + cuánto graduar 1 vs 2 recaídas |
| `B_SLEEP` | base de sueño sin dato | "sin telemetría ≠ fracaso" (≈0.5) |
| `FLOOR_OPT`,`Q_OPT` | suelo y curva del opt-in (sabor dominó) | default 0,1 (lineal); el dueño los sube si quiere |
| `SUP_SAT/SUP_K`,`TASK_SAT/TASK_K` | aporte light de soportes/tasks | "mueven borde, no banda"; task<soporte |

## 6. Decisiones que quedan para el dueño (NADA cerrado todavía)

1. **La curva del opt-in:** ¿lineal (default) o con suelo+convexidad (sabor B, dormir un poco peor
   casi no toca, dormir muy mal hunde)? Es `FLOOR_OPT`/`Q_OPT`.
2. **Los axiomas de estado** para despejar K_sleep y K_sobr: usé "sin dormir→borde Atención (0.62)"
   y "recaída→0.50". Confirmá o cambiá esos targets — de ahí salen los K reales.
3. **A_SOB:** ¿una recaída debe ser golpe casi total (no gradúa multi-recaída) o más suave (1 vs 3
   días/recaídas gradúan)?
4. **Extra ponderado vs simple** entre capas con anclas (elegí ponderado).
5. **Bandas exactas** sobre el estado (usé 0.40/0.62/0.85/1.10): se calibran con tus marcas.

## 7. En criollo

Tu score se arma con **dos preguntas separadas por capa**, que nunca se mezclan:
- **"¿Está en pie?"** — junta tus anclas (hasta la meta), el sueño/sobriedad si los activaste (que
  pesan fuerte porque son sensibles), y un toque de soportes y pendientes. Esto te lleva como
  máximo a **En marcha**: cumplir todo es tu hogar, no la gloria.
- **"¿Te destacaste?"** — solo el excedente de tus anclas (días/tiempo de más). Es lo único que te
  sube a **Plenitud** e **Inquebrantable**. El sueño y la sobriedad no suben acá: te mantienen en
  pie.

Lo clave: dormir mal **baja tu base pero no te borra el esfuerzo** ganado en las anclas (caso 5,
demostrado: la base cae, el extra queda intacto). Una recaída pega más fuerte que el mal sueño, y
solo dentro de la última semana. Los pesos no los puse a mano: salen de un solo número por opt-in
("cuántas anclas vale el sueño") que se **despeja de lo que vos sentís** que merece cada situación.

## 8. Tensiones honestas heredadas

- `EM_TOP` comprime la base en [0, 0.85]: una base mala (0.5) da estado 0.425. Si querés que la
  base mala caiga más rápido a Rojo, se curva la base (calibración).
- `A_SOB` alto cumple D8 (recaída fuerte) pero NO gradúa 1 vs 2 recaídas; bajo gradúa pero la
  recaída pega menos. Es el péndulo de sobriedad a marcar.
- Caso 5 queda en En marcha (no más abajo) porque el extra ganado sostiene: darle veto al sueño
  sobre el extra sería un gate, prohibido.

## 9-bis. Rarezas detectadas en RED-TEAM (2026-06-12) — requieren decisión del dueño

Verificadas con `red_team` (script en el historial). Ordenadas por gravedad:

1. **ANTI-INCENTIVO DEL OPT-IN (grave, filosófico).** Activar un opt-in **solo puede empatar
   (señal=1) o bajar** el estado, nunca subir (no da extra, y su tope ya es En marcha). 3 capas
   justas: sin sueño 0.850; con sueño M=1 → 0.850; M=0.7 → 0.781; M=0.15 → 0.655. Para SOBRIEDAD
   (opt-in MANUAL) esto premia NO trackear lo que va mal. Para sueño es menos grave (telemetría
   automática). Perillas: aceptarlo (honesto), o dar al buen dato un pequeño plus (rompería el eje).
2. **HACER MÁS Y BAJAR DE BANDA (grave, producto).** 3 capas heroicas = Inquebrantable (1.109).
   Agregás 1 área y la cumplís justo (no heroica) → Plenitud (1.045). Hizo MÁS y bajó. Causa: el
   extra se PROMEDIA entre capas-con-ancla; una capa sin superhabit baja el promedio. Perilla:
   extra SUMADO (saturado) o promediado solo entre capas que generaron extra (§6.4).
3. **EL MISMO SUPERHABIT VALE MÁS EN LA CAPA CON OPT-IN (media).** Pasarse en Cuerpo+sueño rinde
   +0.070 que pasarse lo mismo en Interior, porque el extra se pondera por peso de capa y el sueño
   infla ese peso. El opt-in (que no da extra) contamina indirectamente el valor del superhabit.
   Perilla: extra promediado SIMPLE (no ponderado) → destacar rinde igual en cualquier capa.
4. **FRONTERA INQUEBRANTABLE FRÁGIL (menor).** Dani=1.1094, margen +0.009 sobre el corte 1.10; un
   día menos en una capa lo saca. Inquebrantable es exigente (deseado), pero los que entran quedan
   al borde con estos parámetros. Perilla: `W_EXTRA` o `δ`.

## 9. Próximos pasos

1. El dueño marca las decisiones de §6 (sobre todo §6.1 la curva y §6.2 los axiomas de K).
2. Calibración fina de bandas y `W_EXTRA` contra el dataset de marcas.
3. Soportes/tasks: confirmar magnitudes con casos reales (lote SO del dataset).
4. Recién entonces, llevar a código (`domain/scoring/`).

Referencias: `subagente-{A,B,C}-propuesta.md` (las 3 propuestas), `research-orquestador.md`,
`definiciones-dueno-v1.md` (axiomas del dueño), `../exploracion-rendimiento-ancla/merge-consolidado.md`
(fórmula del ancla).
