# OPUS D — Mapeo ESTADO → PUNTOS: curva en S (doble énfasis en extremos)

> Ronda 2 del panel. Sesgo asignado: **doble énfasis / curva en S** — máxima resolución de
> puntos en AMBOS extremos (salir del pozo Y el empujón final a Inquebrantable), medio PLANO.
> Marco fijo: ESTADO ∈ [0, 1.5]; piso digno ~650; **tope 1100 (confirmado)**; el "1000" se gana
> con superhabit; continuo, monótono, Inquebrantable visible; **prohibido el lineal uniforme**.
> Fecha: 2026-06-16. Proyecto: `apk-personal`.

## 1. Filosofía

La ronda 1 dejó un dilema sin cerrar: el dueño quería resolución **abajo** (que recuperarse se
vea fuerte); el merge recomendó resolución **arriba** (que el esfuerzo se premie). Las tres
propuestas eran **lineales a tramos con una sola dirección de énfasis** — abajo (A), uniforme (B),
o arriba (C). Ninguna podía dar feedback fuerte en los dos lugares a la vez.

**OPUS D resuelve la tensión de raíz: no hay que elegir.** Las dos cosas que el producto quiere
gritar son TRANSICIONES, no la zona estable:

1. **Salir del pozo** (Restauración → Atención): que recuperarse se vea como un salto real.
2. **El último empujón** (entrada y travesía de Inquebrantable): que el superhabit se note.

Y hay un tercer lugar que **NO** debe gritar: **En marcha**, el hogar operativo. Ahí el usuario
ya está sostenido; el número subiendo de a poco transmite calma, no urgencia. Que el medio sea
plano es una decisión de tono, no una concesión.

Esto pide una **curva en S** (sigmoide a tramos): pendiente alta en los extremos, baja en el
centro. El perfil de PENDIENTE tiene forma de **U / "sonrisa"** (dos picos, un valle).

## 2. La función ESTADO → puntos (explícita)

Defino la **pendiente** `m(estado)` (puntos por unidad de ESTADO) como función escalonada por
banda, y los puntos como su **integral acumulada** desde el piso digno:

```
puntos(e) = 650 + ∫₀ᵉ m(x) dx
```

Como `m` es constante por tramo, la integral es lineal a tramos ⇒ **continua** (sin saltos) y
**monótona no decreciente** (toda pendiente > 0). La "S" emerge de la secuencia de pendientes
**alta → media → baja → media → alta**.

| Banda | Intervalo ESTADO | Pendiente `m` (pts/unidad) | Rol en la S |
|---|---|---|---|
| Restauración   | [0.00, 0.40) | **410** | **pico de salida** |
| Atención       | [0.40, 0.62) | 210 | hombro descendente |
| En marcha      | [0.62, 0.85) | **55** | **valle (hogar operativo)** |
| Plenitud       | [0.85, 1.10) | 235 | hombro ascendente |
| Inquebrantable | [1.10, 1.50] | **421** | **pico final** |

**Cierre exacto** (no es coincidencia, es de diseño): `Σ mᵢ·anchoᵢ = 450 = TOPE − PISO`, así
`puntos(0)=650` y `puntos(1.5)=1100` clavados.

Forma cerrada equivalente (lo que implementaría el motor):

```
puntos(e):
    e = clamp(e, 0, 1.5)
    if e < 0.40:  return 650 + 410*(e)
    if e < 0.62:  return 814 + 210*(e-0.40)
    if e < 0.85:  return 860.2 + 55*(e-0.62)
    if e < 1.10:  return 872.9 + 235*(e-0.85)
    return 931.6 + 421*(e-1.10)        # tope 1100 en e=1.5
```

## 3. Tabla de hitos

| ESTADO | Puntos | Banda / significado | Pendiente de la banda (pts/+0.10) |
|---|---|---|---|
| 0.00 | **650.0** | piso digno (Restauración) — nunca menos | 41.0 |
| 0.40 | 814.0 | → Atención (saliste del pozo: **+164** desde el piso) | 21.0 |
| 0.62 | 860.2 | → En marcha (hogar operativo) | 5.5 |
| 0.85 | 872.9 | → Plenitud | 23.5 |
| 1.00 | 908.1 | **cumplir-justo** (austero a propósito; NO satura) | 23.5 |
| 1.10 | 931.6 | → Inquebrantable (el pico final arranca acá) | 42.1 |
| 1.50 | **1100.0** | tope | 42.1 |

> El **1000 se cruza en ESTADO ≈ 1.263** — dentro de Inquebrantable, no al entrar. El "mil" se
> **GANA destacándote**, no por cumplir. Cumplir-justo (1.0) da 908: buenísimo, pero deja casi
> 200 puntos de cielo arriba que solo el superhabit alcanza.

## 4. Dónde está la resolución

El perfil de pendiente es una **U simétrica**: los dos extremos pegan ~7.5× más fuerte que el medio.

```
pts por +0.10 de ESTADO:
Restauración   ████████████████████████████████████████  41.0   <- PICO salida
Atención       █████████████████████                     21.0
En marcha      █████                                      5.5   <- VALLE (medio plano)
Plenitud       ███████████████████████                   23.5
Inquebrantable █████████████████████████████████████████ 42.1   <- PICO final
```

- **Salir del pozo se ve fuerte**: de 0.00 a 0.40 sumás +164 pts (41/+0.10). Recuperarte grita.
- **El último empujón se ve fuerte**: de 1.10 a 1.50 sumás +168 pts (42.1/+0.10). El superhabit grita.
- **El medio NO grita**: En marcha mueve 5.5/+0.10. Estar sostenido se siente como calma estable.
- Ratios sobre el valle: **salida 7.5× · final 7.7×**. Casi simétrico — doble énfasis genuino.

## 5. Verificación (python3 — script y salida REAL)

### Script

```python
PISO, TOPE = 650.0, 1100.0
TRAMOS = [
    ("Restauracion",   0.00, 0.40, 410.0),  # PICO de salida
    ("Atencion",       0.40, 0.62, 210.0),  # hombro descendente
    ("En marcha",      0.62, 0.85,  55.0),  # VALLE (hogar operativo)
    ("Plenitud",       0.85, 1.10, 235.0),  # hombro ascendente
    ("Inquebrantable", 1.10, 1.50, 421.0),  # PICO final
]
def puntos(e):
    e = max(0.0, min(1.5, e)); acc = PISO
    for _, a, b, m in TRAMOS:
        if e <= a: break
        acc += m * (min(e, b) - a)
    return acc

total = sum(m*(b-a) for _,a,b,m in TRAMOS)
print(f"CIERRE: PISO={PISO} + sum(m*ancho)={total:.2f} = puntos(1.5)={puntos(1.5):.2f}  (TOPE={TOPE})")
# ... hitos, pendiente por banda, tabla 0.05, monotonia, continuidad ...
```

### Salida REAL

```
CIERRE: PISO=650.0 + sum(m*ancho)=450.00 = puntos(1.5)=1100.00  (TOPE=1100.0)

HITOS
  0.00 ->   650.0  piso/Restauracion
  0.40 ->   814.0  ->Atencion
  0.62 ->   860.2  ->En marcha
  0.85 ->   872.9  ->Plenitud
  1.00 ->   908.1  cumplir-justo
  1.10 ->   931.6  ->Inquebrantable
  1.50 ->  1100.0  tope
  cruza 1000 en ESTADO ~1.263

PENDIENTE POR BANDA (doble enfasis)
  Restauracion   [0.00,0.40)  410.0/u   41.0 pts/+0.10
  Atencion       [0.40,0.62)  210.0/u   21.0 pts/+0.10
  En marcha      [0.62,0.85)   55.0/u    5.5 pts/+0.10
  Plenitud       [0.85,1.10)  235.0/u   23.5 pts/+0.10
  Inquebrantable [1.10,1.50)  421.0/u   42.1 pts/+0.10
  ratios vs valle: salida 7.5x  final 7.7x

TABLA pasos 0.05
  0.00 ->   650.0
  0.05 ->   670.5 d= +20.5
  0.10 ->   691.0 d= +20.5
  0.15 ->   711.5 d= +20.5
  0.20 ->   732.0 d= +20.5
  0.25 ->   752.5 d= +20.5
  0.30 ->   773.0 d= +20.5
  0.35 ->   793.5 d= +20.5
  0.40 ->   814.0 d= +20.5
  0.45 ->   824.5 d= +10.5
  0.50 ->   835.0 d= +10.5
  0.55 ->   845.5 d= +10.5
  0.60 ->   856.0 d= +10.5
  0.65 ->   861.9 d=  +5.9
  0.70 ->   864.6 d=  +2.8
  0.75 ->   867.4 d=  +2.8
  0.80 ->   870.1 d=  +2.8
  0.85 ->   872.9 d=  +2.8
  0.90 ->   884.6 d= +11.8
  0.95 ->   896.4 d= +11.8
  1.00 ->   908.1 d= +11.8
  1.05 ->   919.9 d= +11.8
  1.10 ->   931.6 d= +11.8
  1.15 ->   952.6 d= +21.0
  1.20 ->   973.7 d= +21.0
  1.25 ->   994.8 d= +21.1
  1.30 ->  1015.8 d= +21.0
  1.35 ->  1036.8 d= +21.0
  1.40 ->  1057.9 d= +21.0
  1.45 ->  1079.0 d= +21.1
  1.50 ->  1100.0 d= +21.0

MONOTONA: True
CONTINUA: True
```

La columna `d=` muestra la S directamente: deltas grandes en los extremos (+20.5 abajo, +21.0
arriba), deltas chicos en el medio (+2.8 en el valle). Monótona y continua confirmadas.

## 6. Trade-offs (honestos)

**A favor**
- **Única que da feedback fuerte ABAJO y ARRIBA** — resuelve la tensión que dejó abierta la ronda 1
  sin pedirle al dueño que elija. No es ni "la idea original" ni "el merge": es la síntesis.
- Respeta todo el marco fijo: piso 650, tope exacto 1100, "1000 se gana" (cruza en 1.263),
  Inquebrantable visible y con el empujón más vistoso del recorrido.
- El medio plano es coherente con el tono: el hogar operativo no necesita que el número grite.
- Continua, monótona, auditable, barata de implementar (5 tramos lineales). Verificada.

**En contra / tensiones reales**
- **cumplir-justo (1.0) cae a 908**, más austero que el 960 del merge de ronda 1. Es una
  consecuencia ESTRUCTURAL, no un capricho: con tope 1100 y dos picos grandes, el presupuesto del
  medio se achica. Hallazgo clave de esta exploración: **no se puede clavar a la vez "cumplir-justo
  alto" Y "doble pico fuerte" Y "1000 exacto en el borde de Inquebrantable"** — las tres se pelean
  por los mismos ~450 puntos. D sacrifica el "1000 en el borde" (lo deja cruzar adentro) y baja un
  poco cumplir-justo, para comprar el doble pico. Si el dueño quiere cumplir-justo ≈ 950, hay que
  achatar uno de los picos (deja de ser doble énfasis puro).
- El **valle (5.5/+0.10)** puede sentirse "muerto": en En marcha, una semana entera de progreso
  real mueve el número poco. Es el precio del contraste. Mitigable subiendo el valle a ~9-10 y
  bajando los picos, pero diluye la tesis.
- Dos "rodillas" en la curva (0.62 y 1.10) en vez de una. Más sofisticada de explicar que el
  lineal de B. El usuario igual no ve la fórmula; ve que recuperarse y destacarse "valen mucho".

**Cuándo NO elegir D**: si el producto prioriza que **cumplir-todo** se sienta como casi-techo
(premiar la constancia perfecta por encima del superhabit), C es mejor. D apuesta a lo contrario:
el techo se reserva para el que se destaca, y el medio es deliberadamente sobrio.
