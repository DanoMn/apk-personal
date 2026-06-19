# Guía de variables de la fórmula del ancla

> Documento de lectura humana. No es spec técnica ni contrato — es una
> explicación en lenguaje llano de qué hace cada parámetro y cómo interactúan.
> Audiencia: el dueño del producto, sin necesidad de conocimiento matemático.

---

## El ancla en una línea

Cada ancla produce un número `R`. Ese número sale de comparar lo que el usuario
**prometió hacer** contra lo que **realmente hizo** en la semana.

```
R < 1.0  →  no llegaste a lo que prometiste
R = 1.0  →  cumpliste exactamente
R > 1.0  →  fuiste más allá del compromiso (superávit)
R máximo →  1.5  (el techo absoluto)
```

El estado final de la app es el **promedio de los R de todas las capas activas**.
Ese promedio cae en una banda y eso determina el estado:

```
Promedio    0 ──── 0.40 ──── 0.62 ──── 0.85 ──── 1.10 ──── 1.5
Estado       Rojo  Amarillo  En marcha   Pleno   Inquebrantable
```

El ancla de ejemplo que usamos en todo este documento:

> **"Salir a caminar"** — meta: 3 días por semana, 30 minutos cada vez.
> (F = 3, T = 30)

---

## La fórmula tiene tres capas que no se mezclan

```
┌─────────────────────────────────────────────────────────┐
│  CAPA 1: BASE (phi)                                     │
│  ¿Cumpliste los días comprometidos? ← vive la constancia│
├─────────────────────────────────────────────────────────┤
│  CAPA 2: REPARACIÓN (V)                                 │
│  ¿Fuiste días extra para cerrar el déficit?             │
├─────────────────────────────────────────────────────────┤
│  CAPA 3: BONUS (S)                                      │
│  ¿Fuiste más allá? Tiempo extra o días extra.           │
└─────────────────────────────────────────────────────────┘

R = base + (base² × bonus)
```

Cada variable del modelo opera sobre **una sola capa**. No se contradicen — se
complementan porque cada una mide una dimensión distinta del comportamiento.

---

## Variable 1 — γ (gamma) = 1.5

**Capa donde opera:** BASE

**Pregunta que responde:** Si fuiste un día comprometido pero solo hiciste la
mitad del tiempo prometido, ¿cuánto crédito recibís?

Con γ = 1 (neutro):
```
Hiciste 15 min de 30  →  crédito = 0.50  (proporcional exacto)
Hiciste 22 min de 30  →  crédito = 0.73
```

Con γ = 1.5 (el valor actual — más exigente con el tiempo parcial):
```
Hiciste 15 min de 30  →  crédito = 0.35  (menos que proporcional)
Hiciste 22 min de 30  →  crédito = 0.64
```

Con γ > 1 los días donde hiciste menos tiempo del prometido valen
proporcionalmente menos. Un día cortísimo duela más de lo que su porcentaje de
tiempo sugiere.

### Lo que γ NO hace

γ no toca el mecanismo de constancia principal. Ese mecanismo es otro:
cuando prometiste 3 días y solo fuiste 2, el tercer slot **siempre vale 0 en el
denominador**. Ese castigo existe independientemente de γ.

```
Prometiste 3 días × 30 min. Fuiste 2 días × 60 min.

  crédito de los días que fuiste = 1.0 cada uno (60 min capeado)
  tercer día faltante             = 0.0
  phi = (1.0 + 1.0 + 0.0) / 3   = 0.667
  R ≈ 0.74
```

```
Prometiste 3 días × 30 min. Fuiste 3 días × 30 min.

  phi = (1.0 + 1.0 + 1.0) / 3   = 1.0
  R = 1.0
```

**Conclusión: 3 días × 30 min gana sobre 2 días × 60 min.
La constancia está garantizada por la fórmula, no por γ.**

### La decisión real que habilita γ

La pregunta que γ deja en manos del dueño es esta:

> Si te comprometiste a ir 4 días y fuiste los 4, pero cada uno solo hiciste la
> mitad del tiempo: ¿eso vale igual, más, o menos que ir 2 de los 4 días al
> tiempo completo?

| γ | Resultado |
|---|-----------|
| < 1 | Ir 4 días a mitad tiempo **gana** (presencia total importa más) |
| = 1 | Empate exacto |
| > 1 | Ir 2 días completos **gana** (profundidad importa más que presencia parcial) |

Con el valor actual γ = 1.5: ir 2 días completos (R=0.50) gana sobre ir 4 días
a mitad (R=0.35). Si la filosofía de la app es que aparecer todos los días
cuenta aunque sea parcial, γ debería bajar hacia 1 o menos.

---

## Variable 2 — λ_v (lambda) = 0.5

**Capa donde opera:** REPARACIÓN

**Pregunta que responde:** Si tuve un déficit en los días comprometidos, ¿cuánto
ayudan los días voluntarios a cerrar ese agujero?

La fórmula real:

```
base = 1 - (déficit) × exp(-λ_v × V)
```

Donde:
- `déficit` = cuánto te faltó en los días comprometidos
- `V` = suma de los días voluntarios que agregaste
- `exp(-λ_v × V)` = cuánto del déficit sobrevive después de los voluntarios

### Ejemplo concreto

Semana con un día corto: fuiste los 3 días comprometidos pero el último solo
hiciste 20 minutos en vez de 30.

```
Días: [30, 30, 20] min
phi = 0.848
déficit = 0.152
base = 0.848  (LEVE_DEF — casi Pleno, no llega)
```

Ahora agregás un día voluntario de 30 min (un día que no era tu compromiso):

```
Con λ_v = 0.5:
  base = 1 - 0.152 × exp(-0.5 × 1.0)
       = 1 - 0.152 × 0.607
       = 0.908  ← subiste a Pleno
```

| Días voluntarios agregados | Déficit que queda | Base resultante |
|---|---|---|
| 0 | 100% | 0.848 |
| 1 | 61% | 0.908 |
| 2 | 37% | 0.944 |
| 4 | 14% | 0.979 |

### Lo que λ_v controla

λ_v no es "los voluntarios valen la mitad". Es la **velocidad de reparación**:
un λ_v más alto hace que un solo día voluntario cierre más del déficit.

```
λ_v = 0.1  →  1 día voluntario repara el  9.5% del déficit (lento)
λ_v = 0.5  →  1 día voluntario repara el 39.3% del déficit (moderado)
λ_v = 1.0  →  1 día voluntario repara el 63.2% del déficit (rápido)
λ_v = 2.0  →  1 día voluntario repara el 86.5% del déficit (muy rápido)
```

Si los días voluntarios son días que no eran compromiso, tiene sentido que no
reparen el déficit al instante — el compromiso tiene más peso que el impulso
espontáneo del momento.

---

## Variable 3 — κ (kappa) = 1.5

**Capa donde opera:** BONUS

**Pregunta que responde:** Si ya cumplí todo lo comprometido, ¿de dónde viene
el bonus de superávit — de hacer más tiempo por sesión o de ir más días?

La fórmula:

```
wt = (F / 7) ^ kappa

bonus_total = wt × tiempo_extra + (1 - wt) × días_extra
```

`wt` es el peso del tiempo extra. El resto, `1 - wt`, es el peso de los días
extra.

Con κ = 1.5:

| Frecuencia comprometida (F) | wt (peso del tiempo) | Peso de los días extra |
|---|---|---|
| F = 2 días/semana | 0.19 | 0.81 |
| F = 3 días/semana | 0.28 | 0.72 |
| F = 5 días/semana | 0.55 | 0.45 |
| F = 7 días/semana | 1.00 | 0.00 |

### En criollo

Cuando tu compromiso es liviano (ej. 2 o 3 días por semana), el sistema te
premia más por **agregar días extra** que por hacer más tiempo en cada sesión.
Cuando tu compromiso ya es máximo (7 días), no podés agregar más días — el único
superávit posible viene de **tiempo extra por sesión**.

```
Ejemplo A: F=3 (comprometiste 3 días), hiciste 5 días al tiempo exacto.
  Los 2 días extra son voluntarios y generan el bonus principal.

Ejemplo B: F=7 (comprometiste todos los días), hiciste todos + 45 min en vez de 30.
  No hay días extra posibles. El bonus viene del tiempo extra por sesión.
```

Esto es coherente con la constancia: cuando tu base es liviana, el sistema te
incentiva a **aparecer más**, no a romperte en los días que ya ibas.

---

## Variable 4 — σ_max (sigma max) = 0.5

**Capa donde opera:** BONUS

**Pregunta que responde:** ¿Cuánto puede subir R por encima de 1.0?

```
R máximo = 1.0 + σ_max = 1.5
```

No importa cuántas horas extra hagas ni cuántos días voluntarios agregues — R
no puede superar 1.5. El sistema tiene un techo para que un área de vida no
pueda compensar ilimitadamente a otras.

```
σ_max = 0.3  →  R máximo = 1.3  (techo bajo, superávit poco influyente)
σ_max = 0.5  →  R máximo = 1.5  (techo moderado — valor actual)
σ_max = 0.8  →  R máximo = 1.8  (techo alto, superávit muy influyente)
```

---

## Variable 5 — δ (delta) = 0.10

**Capa donde opera:** BANDAS (umbral de Inquebrantable)

**Pregunta que responde:** ¿Cuánto por encima de 1.0 necesita estar el promedio
para activar Inquebrantable?

```
Inquebrantable  ⟺  promedio ≥ 1.0 + δ = 1.10
```

Esto significa que cumplir todo perfectamente en todas las capas (promedio = 1.0)
**nunca es Inquebrantable**. Siempre se necesita ir un poco más allá.

```
δ = 0.05  →  Inquebrantable requiere promedio ≥ 1.05  (más fácil)
δ = 0.10  →  Inquebrantable requiere promedio ≥ 1.10  (valor actual)
δ = 0.20  →  Inquebrantable requiere promedio ≥ 1.20  (más exigente)
```

---

## Cómo se correlacionan: el cuadro completo

```
Semana del usuario
       │
       ▼
[días marcados con sus minutos]
       │
       ├─► ¿Cuántos días comprometidos cumpliste?      ← afecta phi (constancia dura)
       │
       ├─► ¿Cuánto tiempo hiciste en cada día?         ← γ amplifica el castigo
       │   (vs. el target T)                              por días parciales
       │
       ├─► ¿Fuiste días voluntarios?                   ← λ_v controla cuánto
       │   (más allá de los F comprometidos)              reparan el déficit
       │
       ▼
    base (0.0 a 1.0)
       │
       ├─► ¿Hiciste más tiempo del target?             ── ┐
       │   (St = time surplus)                            │── κ mezcla estos dos
       ├─► ¿Fuiste más días de los prometidos?         ── ┘    en el bonus
       │   (Sd = day surplus)
       │
       ▼
    bonus (0.0 a σ_max)  ← el bonus solo es grande si la base ya es sólida (× base²)
       │
       ▼
    R = base + base² × bonus
       │
       ▼
  promedio de capas  →  banda  →  estado
```

### La propiedad más importante del bonus

```
bonus_real = base² × bonus_crudo
```

Si tu base está baja (ej. base = 0.4), el multiplicador es `0.4² = 0.16`. El
bonus no puede rescatar una base rota. Esto evita que hacer mucho tiempo en un
día compense no aparecer en los otros.

---

## Resumen de las decisiones de calibración abiertas

| Variable | Valor actual | Pregunta que el dueño debe responder |
|---|---|---|
| γ | 1.5 | ¿Ir todos los días a mitad de tiempo vale igual que ir la mitad de días al completo? |
| λ_v | 0.5 | ¿Cuánto reparan los días voluntarios el déficit de un día corto? |
| κ | 1.5 | ¿Cuánto más debe importar agregar días vs. agregar tiempo, para compromisos livianos? |
| σ_max | 0.5 | ¿Cuánto puede subir una capa por encima de 1.0 como máximo? |
| δ | 0.10 | ¿Qué margen sobre 1.0 define Inquebrantable? |

Los valores actuales son **ilustrativos** — verifican que la fórmula se
comporta sin errores. Los valores finales se deciden contra la sensación real
del dueño con datos propios.
