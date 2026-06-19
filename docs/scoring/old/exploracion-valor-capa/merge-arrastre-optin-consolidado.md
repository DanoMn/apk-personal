# Merge consolidado — arrastre del opt-in sin matar anclas ni distorsionar superhabit

> **Consolidación del orquestador (2026-06-12), verificada con python3.** Merge de las 3 soluciones
> a ciegas (A cimiento separado · B desacople de pesos · C peso dinámico) + una mejora propia.
> Script: `modelo_valor_capa_v4_merge.py` (reproducible). Resuelve el trilema de
> `problema-arrastre-optin-v1.md`.

---

## 1. Convergencia de las 3 propuestas

Las tres, a ciegas, coincidieron en **3 movimientos**:
1. **Sacar el opt-in de la mezcla intra-capa** (chau `K_INT` alto que mataba las anclas).
2. **El extra/superhabit SIEMPRE con pesos iguales** → inmune al opt-in → Sol=Tin por construcción.
3. **El opt-in solo toca el canal base**, con un arrastre **plano en N** (no diluido).

Diferían en CÓMO arrastra: A multiplicativo global (`base·C`), B peso de capa fijo en base
(`DRAG_BASE`), C término de peso dinámico (`w(M,N)`).

## 2. El merge — qué tomé de cada una + la mejora

| Pieza | De quién | Por qué |
|-------|----------|---------|
| Opt-in como **término de peso dinámico** `w(M,N)` que crece cuando M empeora | **C** | Aditivo-ponderado (no multiplicativo) → evita el doble-castigo geométrico de A; controlable. |
| **Escalado por N** (`w ∝ N`) | **C** | Hace el arrastre **plano en N** (la tajada del opt-in en el denominador no se diluye). |
| **Neutralidad EXACTA** (también con anclas en déficit) | **A** (objetivo) | **MEJORA CLAVE:** puse `w(M=1)=0` (el opt-in bien es invisible, peso cero) en vez del `w(1)=W0` de C. Así el opt-in bien no agrega masa → no sube el promedio. C original fallaba esto; el merge lo cumple exacto. |
| **Despeje del parámetro de un axioma de estado** | **B** | `BETA` sale de "recaída total + anclas perfectas → tal estado", no a dedo. |
| **Extra con pesos iguales** (Sol=Tin) | A+B+C | inmunidad del superhabit. |
| Sobriedad binaria multi-track (producto), sueño continuo, capa solo-opt-in = la capa | contrato | axiomas. |

## 3. El modelo

```
Por cada capa activa:
  con anclas:  término (anchor_base, W0)        anchor_base = promedio min(R_i,1) ∈[0,1]
               extra_capa = promedio max(R_i−1,0)   (SOLO anclas)
               si además tiene opt-in con señal M:
                   término-sombra (M, w)  con  w = BETA·N·(1−M)     ← w(M=1)=0: invisible
  solo opt-in (sin anclas):  término (M, W0)    ← el opt-in ES la capa

base  = Σ(valor·peso) / Σ(peso)   sobre todos los términos      (peso dinámico)
extra = promedio de extra_capa sobre capas con anclas           (pesos IGUALES)
ESTADO = min(base,1) + extra
bandas: R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10
```

Variables: `W0=1` (masa de una capa-ancla); `M∈[0,1]` señal del opt-in (sueño continuo / sobriedad
binaria por producto de tracks; sin dato → B_SLEEP); `N` = nº capas activas; `BETA` = intensidad
del arrastre, **despejada** de un target.

**Por qué es elegante:** `w=BETA·N·(1−M)`. Con `M=1` → `w=0` → el opt-in desaparece (neutro exacto).
Con `M=0` → `w=BETA·N` → con anclas perfectas, `base = N/(N+BETA·N) = 1/(1+BETA)`, **independiente
de N** (arrastre plano exacto). Un solo término hace las tres cosas.

## 4. Verificación (salida REAL de python3)

```
MERGE v4. w_optin=BETA*N*(1-M), W0=1. TARGET=0.55 → BETA=0.818
--- 8 CASOS (v3 ANTES vs MERGE) ---
P1 justo + sueno bien N=3       v3=1.000 PLENITUD     | MERGE=1.000 PLENITUD
P2 mal sueno M=.15 N=3          v3=0.773 EN MARCHA    | MERGE=0.651 EN MARCHA
P2 mal sueno M=.15 N=5          v3=0.864 PLENITUD     | MERGE=0.651 EN MARCHA
P3 recaida M=0 N=3              v3=0.733 EN MARCHA    | MERGE=0.550 ATENCION
P3 recaida M=0 N=5             v3=0.840 EN MARCHA    | MERGE=0.550 ATENCION
P4 sueno regular M=.5 N=5       v3=0.920 PLENITUD     | MERGE=0.855 PLENITUD
P7 superhabit repartido x3      v3=1.432 INQUEBRANTABLE | MERGE=1.432 INQUEBRANTABLE
P8 capa solo-opt-in sueno bien  v3=1.000 PLENITUD     | MERGE=1.000 PLENITUD
--- CRITERIOS ---
C5 Sol=1.1441 Tin=1.1441 empatan=True
C2 NEUTRALIDAD con anclas en DEFICIT: sin=0.9167 con opt-in bien=0.9167 neutro=True
C3 arrastre PLANO en N (recaida M=0): [0.55, 0.55, 0.55, 0.55, 0.55]
D8 recaida 0.550 < mal sueno 0.651 = True
ANTI-GATE: paso maximo |dEstado| con dM=0.001 = 0.00070  -> continuo, sin gate
```

Los 6 criterios (C1–C6) + D8 + anti-gate: **todos verdes.**

## 5. Por qué el merge supera a cada propuesta sola

| Propiedad | A | B | C | **MERGE** |
|-----------|---|---|---|-----------|
| Neutralidad exacta CON déficit | ✅ | ❌ | ❌ | ✅ |
| Arrastre plano en N | ✅ | ✅ | casi | ✅ exacto |
| No multiplicativo (sin doble-castigo geométrico) | ❌ | ✅ | ✅ | ✅ |
| Anclas intactas (opt-in no las toca) | ✅ | parcial (K_INT) | ✅ | ✅ |
| Multi-opt-in se compone natural (no "peor manda") | ❌ (min) | — | ✅ | ✅ |
| Parámetro despejado de axioma | ✅ | ✅ | barrido | ✅ |
| Una sola perilla | ✅ | ✅ | ✅ | ✅ (BETA) |

## 6. La única decisión que queda — el TARGET (de ahí sale BETA)

`BETA` se despeja de *"recaída total (o sueño nulo) + anclas perfectas → ¿qué estado?"*:

| Recaída total + anclas perfectas → | BETA | mal sueño M=0.15 quedaría en |
|-----------------------------------|------|------------------------------|
| 0.40 (Restauración) | 1.500 | ~Atención |
| 0.50 (Atención) | 1.000 | ~Atención/En marcha |
| 0.55 (Atención) | 0.818 | En marcha (0.651) |
| 0.62 (borde En marcha) | 0.613 | En marcha alto |

Es la perilla del dueño: cuánto debe doler una recaída/sueño nulo con todo lo demás perfecto.

## 7. Tensiones honestas

1. **El arrastre es PLANO, no decreciente.** Es lo pedido (no diluir por N), pero significa que el
   mal sueño pesa igual con 3 o 7 capas. Si se quisiera que más estructura amortigüe un poco, `BETA`
   podría llevar un leve decaimiento — hoy es plano a propósito.
2. **Dos opt-ins malos se componen** (sueño mal + recaída → ambos términos pesados → arrastre fuerte
   combinado). Es coherente (dos cimientos rotos peor que uno), pero conviene marcar un caso con el
   dueño para ver que el combinado no sea excesivo.
3. **`BETA` con TARGET muy bajo (0.40) es agresivo:** recaída total → Restauración aun con anclas
   perfectas. Decisión de producto (D8); perilla continua.
4. **Sueño sin dato (B_SLEEP=0.5)** mueve un poco. Si se quiere "sin dato = estrictamente neutro",
   tratar M=None como peso 0 (igual que el opt-in bien).

## 8. DECISIONES CERRADAS DEL DUEÑO (2026-06-12) — motor de opt-ins COMPLETO

1. **Componer** (no "peor manda"): dos opt-ins malos a la vez suman sus arrastres. Caso realista
   (mal sueño + recaída 2–3 días) = Atención; extremo absoluto (cero sueño + recaída 7 días) =
   Restauración. **SIN tope** (aceptado por el dueño).
2. **Mismo BETA para sueño y sobriedad** — la sobriedad NO pega más fuerte que el sueño. Decisión
   humana/compasiva: no desmotivar al usuario más sensible (insomnio/adicción).
3. **Señal de SUEÑO:** `M ∈ [0,1]` continuo (4 componentes de calidad por noche, telemetría;
   sin dato → `B_SLEEP`).
4. **Señal de SOBRIEDAD (definida):** `M_sobr = (1 − A)^(días de recaída en la semana)`, **A=0.55**.
   Multi-track: producto de las señales por track (track limpio = 1 = invisible, no diluye).
   Verificado: 1 día → En marcha (0.829) · 3 días → Atención (0.612) · 7 días → Atención (0.553).
5. **BETA = 0.818** (despejado de TARGET=0.55: "opt-in en su piso + anclas perfectas → Atención").
6. El opt-in entra como **término-sombra de peso dinámico** `w = BETA·N·(1−M)` con `w(M=1)=0`
   (invisible cuando está bien) — mismo mecanismo para sueño y sobriedad, solo cambia cómo se
   calcula `M`. Extra (superhabit) siempre pesos iguales (Sol=Tin).

**→ El motor de OPT-INS (sueño + sobriedad) queda CERRADO.**

## 9. Próximo paso

Sueño y sobriedad cerrados. **Pendiente para otra sesión: SOPORTES y TASKS** (cómo entran al valor
de capa). Calibración fina de bandas y de A/BETA contra marcas del dueño, cuando toque. Referencias:
`solucion-{A,B,C}-*.md`, `problema-arrastre-optin-v1.md`, `modelo_valor_capa_v4_merge.py`.
