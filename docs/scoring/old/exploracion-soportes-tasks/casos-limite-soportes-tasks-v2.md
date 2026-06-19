# Casos límite — SOPORTES y TASKS (modelo MERGE v2)

> **Generado por `casos_limite_gen.py`** (números exactos del modelo MERGE v2, no a mano).
> Parámetros: `WS=0.07` (blend soporte), `TAU=0.06` (techo task/capa), `THETA=0.0639`, `N0=1.0` (saturación task), `PISO=0.35` (peso capa solo-soportes), `smax=0.5` (techo extra).
> Bandas: Restauración<0.40 · Atención<0.62 · En marcha<0.85 · Plenitud<1.10 · Inquebrantable≥1.10.
> Señal soporte por ítem = `min(días/4,1)`; bloque `G_s = promedio` (no crece con la cantidad).

---

## Caso 1 — Cumplir-justo exacto (el eje semántico)

**Qué candado prueba:** El punto 1.0 = entrada a Plenitud. Cumplir todas las anclas en la meta, sin soportes ni tasks, debe dar EXACTAMENTE 1.0.

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Interior | meditar 4d×10 (meta 10)→R=1.00 | — | 0 |
| Cuerpo | caminar 4d×30 (meta 30)→R=1.00 | — | 0 |
| Proyecto | estudiar 5d×60 (meta 60)→R=1.00 | — | 0 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Interior | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |
| Cuerpo | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |
| Proyecto | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |

### Promedio exacto y estado

```
Σ(valor·masa) = 1.000×0.761 + 1.000×0.761 + 1.000×0.761 = 2.2826
Σ(masa)       = 0.761 + 0.761 + 0.761 = 2.2826
SCORE = 2.2826 / 2.2826 = 1.0000  →  PLENITUD
```

**Lectura:** 3 anclas justas → cada base=1.0, extra=0. Score=1.0000 = inicio de Plenitud. Es el ancla del eje: cumplir lo pactado te pone en Plenitud, ni más ni menos.

---

## Caso 2 — Anti-abuso de tasks (techo TAU)

**Qué candado prueba:** 100 tasks en CADA capa desde cumplir-justo NO deben comprar Inquebrantable (≥1.10). Es el candado del techo de task.

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Interior | meditar 4d×10 (meta 10)→R=1.00 | — | 100 |
| Cuerpo | caminar 4d×30 (meta 30)→R=1.00 | — | 100 |
| Proyecto | estudiar 5d×60 (meta 60)→R=1.00 | — | 100 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Interior | 1.0000 | 0.0000 | — | 1.0000 | +0.0600 | **1.0600** | **0.7609** |
| Cuerpo | 1.0000 | 0.0000 | — | 1.0000 | +0.0600 | **1.0600** | **0.7609** |
| Proyecto | 1.0000 | 0.0000 | — | 1.0000 | +0.0600 | **1.0600** | **0.7609** |

### Promedio exacto y estado

```
Σ(valor·masa) = 1.060×0.761 + 1.060×0.761 + 1.060×0.761 = 2.4196
Σ(masa)       = 0.761 + 0.761 + 0.761 = 2.2826
SCORE = 2.4196 / 2.2826 = 1.0600  →  PLENITUD
```

**Lectura:** Cada capa suma su techo de task (~0.06) al extra → valor 1.06. Score=1.0600 = Plenitud. Aunque haga 100 tasks por capa, NUNCA llega a Inquebrantable (1.10). El techo TAU=0.06 lo garantiza.

---

## Caso 3 — Techo 0.5 del extra con superhabit + tasks (saturación conjunta)

**Qué candado prueba:** Una capa con superhabit EXTREMO de anclas (extra ya cerca de 0.5) + 100 tasks: el extra total NO puede pasar 0.5.

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Cuerpo (XL) | caminar 4d×600 (meta 30)→R=1.50 | — | 100 |
| Interior | meditar 4d×10 (meta 10)→R=1.00 | — | 0 |
| Proyecto | estudiar 5d×60 (meta 60)→R=1.00 | — | 0 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Cuerpo (XL) | 1.0000 | 0.5000 | — | 1.0000 | +0.0000 | **1.5000** | **0.7609** |
| Interior | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |
| Proyecto | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |

### Promedio exacto y estado

```
Σ(valor·masa) = 1.500×0.761 + 1.000×0.761 + 1.000×0.761 = 2.6631
Σ(masa)       = 0.761 + 0.761 + 0.761 = 2.2826
SCORE = 2.6631 / 2.2826 = 1.1667  →  INQUEBRANTABLE
```

**Lectura:** El Inquebrantable (1.1667) es LEGÍTIMO: viene del superhabit REAL de las anclas de Cuerpo (extra 0.50), NO de las tasks. Lo que el caso prueba es la columna `lift_task = +0.0000` en Cuerpo: las 100 tasks aportan CERO porque la curva ya está en el techo 0.5 (la saturación conjunta no deja pasar). El esfuerzo de tasks no se premia donde las anclas ya alcanzaron la gloria máxima.

---

## Caso 4a — Tasks arañan el cruce SÓLO al ras del borde

**Qué candado prueba:** Usuario En marcha pero MUY cerca de Plenitud (0.85). Con tasks debe cruzar.

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Interior | meditar 4d×8.76 (meta 10)→R=0.82 | — | 5 |
| Cuerpo | caminar 4d×26.28 (meta 30)→R=0.82 | — | 5 |
| Proyecto | estudiar 4d×52.56 (meta 60)→R=0.82 | — | 5 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Interior | 0.8199 | 0.0000 | — | 0.8199 | +0.0401 | **0.8600** | **0.7609** |
| Cuerpo | 0.8199 | 0.0000 | — | 0.8199 | +0.0401 | **0.8600** | **0.7609** |
| Proyecto | 0.8199 | 0.0000 | — | 0.8199 | +0.0401 | **0.8600** | **0.7609** |

### Promedio exacto y estado

```
Σ(valor·masa) = 0.860×0.761 + 0.860×0.761 + 0.860×0.761 = 1.9630
Σ(masa)       = 0.761 + 0.761 + 0.761 = 2.2826
SCORE = 1.9630 / 2.2826 = 0.8600  →  PLENITUD
```

**Lectura:** Base/capa ≈0.82 (En marcha, a 0.03 del borde 0.85). Las tasks empujan ~+0.04 → score cruza a **Plenitud**. El empujón ALCANZA porque ya estabas al ras.

---

## Caso 4b — Tasks NO fabrican el cruce si estás lejos

**Qué candado prueba:** Mismo usuario pero LEJOS del borde. Las tasks no deben hacerlo cruzar.

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Interior | meditar 4d×8.25 (meta 10)→R=0.75 | — | 5 |
| Cuerpo | caminar 4d×24.75 (meta 30)→R=0.75 | — | 5 |
| Proyecto | estudiar 4d×49.5 (meta 60)→R=0.75 | — | 5 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Interior | 0.7493 | 0.0000 | — | 0.7493 | +0.0335 | **0.7828** | **0.7609** |
| Cuerpo | 0.7493 | 0.0000 | — | 0.7493 | +0.0335 | **0.7828** | **0.7609** |
| Proyecto | 0.7493 | 0.0000 | — | 0.7493 | +0.0335 | **0.7828** | **0.7609** |

### Promedio exacto y estado

```
Σ(valor·masa) = 0.783×0.761 + 0.783×0.761 + 0.783×0.761 = 1.7869
Σ(masa)       = 0.761 + 0.761 + 0.761 = 2.2826
SCORE = 1.7869 / 2.2826 = 0.7828  →  EN MARCHA
```

**Lectura:** Base/capa ≈0.75 (En marcha, a 0.10 del borde). Las tasks suman ~+0.03 pero NO alcanzan: sigue **En marcha**. Las tasks arañan, no fabrican estados.

---

## Caso 5 — Gate base²: sin cimiento, las tasks casi no aportan

**Qué candado prueba:** Anclas A MEDIAS (base baja) + 10 tasks. El gate base² debe castrar el aporte de las tasks (no hay gloria sin cimiento).

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Interior (medias) | meditar 2d×5 (meta 10)→R=0.18 | — | 10 |
| Cuerpo (medias) | caminar 2d×15 (meta 30)→R=0.18 | — | 10 |
| Proyecto (medias) | estudiar 2d×30 (meta 60)→R=0.14 | — | 10 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Interior (medias) | 0.1768 | 0.0000 | — | 0.1768 | +0.0019 | **0.1787** | **0.7609** |
| Cuerpo (medias) | 0.1768 | 0.0000 | — | 0.1768 | +0.0019 | **0.1787** | **0.7609** |
| Proyecto (medias) | 0.1414 | 0.0000 | — | 0.1414 | +0.0012 | **0.1426** | **0.7609** |

### Promedio exacto y estado

```
Σ(valor·masa) = 0.179×0.761 + 0.179×0.761 + 0.143×0.761 = 0.3804
Σ(masa)       = 0.761 + 0.761 + 0.761 = 2.2826
SCORE = 0.3804 / 2.2826 = 0.1666  →  RESTAURACION
```

**Lectura:** Con base ≈0.35, el lift de task se multiplica por base²≈0.12 → casi 0. Las 10 tasks apenas mueven el score: primero hay que sostener las anclas. El gate base² funciona.

---

## Caso 6a — Capa solo-soportes PERFECTA no infla el score

**Qué candado prueba:** Una capa sin anclas, soportes perfectos. Pesa menos (masa 0.35) → no debe inflar como una capa con anclas.

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Interior | meditar 4d×10 (meta 10)→R=1.00 | — | 0 |
| Cuerpo | caminar 4d×30 (meta 30)→R=1.00 | — | 0 |
| Vínculos (solo-sop) | — (sin anclas) | mensajes 4d→1.00; llamar 4d→1.00 | 0 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Interior | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |
| Cuerpo | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |
| Vínculos (solo-sop) | — | 0.0000 | 1.0000 | 1.0000 | +0.0000 | **1.0000** | **0.3500** |

### Promedio exacto y estado

```
Σ(valor·masa) = 1.000×0.761 + 1.000×0.761 + 1.000×0.350 = 1.8718
Σ(masa)       = 0.761 + 0.761 + 0.350 = 1.8718
SCORE = 1.8718 / 1.8718 = 1.0000  →  PLENITUD
```

**Lectura:** Vínculos vale 1.0 pero pesa 0.35 (vs 0.76 de las capas con anclas). Aporta poco al promedio: una capa de poca sustancia no manda. Score se mantiene en Plenitud sin distorsión.

---

## Caso 6b — Capa solo-soportes DESCUIDADA (decisión abierta 5.2)

**Qué candado prueba:** Misma capa solo-soportes pero TODO descuidado (valor 0). ¿Cuánto arrastra con masa 0.35?

### Config

| Capa | Anclas (días×min / meta → R) | Soportes (días → señal) | Tasks hoy |
|---|---|---|---|
| Interior | meditar 4d×10 (meta 10)→R=1.00 | — | 0 |
| Cuerpo | caminar 4d×30 (meta 30)→R=1.00 | — | 0 |
| Vínculos (solo-sop) | — (sin anclas) | mensajes 0d→0.00; llamar 0d→0.00 | 0 |

### Cálculo por capa

| Capa | base_anc | extra_anc | G_s (prom soportes) | base_eff = (1−0.07)·base+0.07·G | lift_task | **valor capa** | **masa (peso)** |
|---|---|---|---|---|---|---|---|
| Interior | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |
| Cuerpo | 1.0000 | 0.0000 | — | 1.0000 | +0.0000 | **1.0000** | **0.7609** |
| Vínculos (solo-sop) | — | 0.0000 | 0.0000 | 0.0000 | +0.0000 | **0.0000** | **0.3500** |

### Promedio exacto y estado

```
Σ(valor·masa) = 1.000×0.761 + 1.000×0.761 + 0.000×0.350 = 1.5218
Σ(masa)       = 0.761 + 0.761 + 0.350 = 1.8718
SCORE = 1.5218 / 1.8718 = 0.8130  →  EN MARCHA
```

**Lectura:** Vínculos vale 0.0 (G_s=0). Con masa 0.35 arrastra el score, pero menos que si fuera una capa con anclas (masa 0.76). DECISIÓN 5.2: ¿está bien que valga 0 y arrastre, o querés un piso tibio para que una capa de poca sustancia no hunda tanto?

---

