# OPUS E — Mapeo ESTADO → PUNTOS por HITOS-META perseguibles

> Ronda 2 del panel. Sesgo OPUS E: **diseño al revés** — primero elijo los números-META
> que el usuario debe PERSEGUIR, y dónde caen en el ESTADO para que cruzarlos se SIENTA
> como un logro. El número es un OBJETIVO, no un termómetro. Estado: exploración. Fecha: 2026-06-16.
> Proyecto: `apk-personal`.

## 1. Filosofía

Las tres propuestas de ronda 1 (A/B/C) son **termómetros**: deciden una "resolución global"
(toda abajo, uniforme, toda arriba) y dejan que los números caigan donde caigan. Yo invierto
el orden de diseño. No empiezo por la función — empiezo por la **psicología del jugador que
mira un número**.

La gente no persigue una pendiente. Persigue **números redondos**. "Me falta poco para los
1000" es un motor real; "mi estado subió 0.04" no lo es. El diseño explota tres hechos:

1. **Un número redondo es una meta.** 700, 800, 900, 1000 son metas naturales aunque nadie
   las haya declarado. El usuario se las pone solo.
2. **La resolución debe APRETARSE justo antes de cada meta** — así el último tramo hacia el
   número redondo "se siente" alcanzable y cada acción mueve mucho el dial → empuja a cruzar.
3. **La resolución se AFLOJA justo después de cruzar** — ya lo lograste; hay una meseta de
   descanso (recompensa psicológica: "llegué") antes de que el siguiente hito empiece a tirar.

Esto produce una curva de **rampa→meseta→rampa→meseta**: el número "trepa rápido" cerca de
cada meta y "se calma" después. El usuario siente que conquista escalones, no que sube una
rampa infinita.

## 2. Los hitos-meta elegidos (y por qué)

| Meta | Cae cerca de ESTADO | Banda | Por qué este número, en este lugar |
|------|--------------------|-------|-----------------------------------|
| **700 — salir del pozo** | ~0.30 | Restauración alta | Primer escalón temprano y BARATO de cruzar. En el peor momento el usuario ve 650 (digno) y a poquísimo esfuerzo cruza 700: "ya me estoy moviendo". Esperanza inmediata, sin humillar. |
| **800 — terreno firme** | ~0.65 | En marcha | Cruzar a "hogar operativo". El 800 marca *dejé la zona de atención, estoy funcionando*. Hito de consolidación. |
| **900 — Plenitud a la vista** | ~0.88 | Plenitud (entra 0.85) | Anclado a la **entrada de Plenitud**. Cruzar 900 ≈ entrar a Plenitud: el número redondo coincide con el salto de banda más aspiracional. |
| **1000 — lo lograste** | ~1.09 | Inquebrantable (entra 1.10) | EL hito. La resolución se **aprieta al máximo** acá. Cumplir-justo (1.0) da **941**, NO mil — los 1000 se GANAN con superhabit, cruzándolos justo al entrar a Inquebrantable. Es el hallazgo de C, pero acá es *intencionalmente perseguible*: a 1.0 ves "941, me faltan 59 para los 1000" y eso te empuja al esfuerzo extra. |
| **1100 — techo que respira** | 1.50 (tope) | Inquebrantable | Ya sos inquebrantable; el último tramo AFLOJA. De 1.10 a 1.50 solo suben ~90 pts: el techo está cerca y respira sobre los 1000, pero ya no es la zanahoria — el premio fue cruzar el mil. |

Decisión clave de divergencia: **el "1000" NO es el tope ni cae en cumplir-justo.** Cae en la
entrada de Inquebrantable y se gana con superhabit. El usuario que cumple todo ve 941 y tiene
una meta clarísima arriba.

## 3. La función ESTADO → puntos (explícita)

No es lineal a tramos (eso es A/B/C). Es una **suma de rampas logísticas**, una por hito-meta.
Cada hito aporta su "salto" de puntos concentrado alrededor de su ESTADO-objetivo:

```
piso = 650
raw(estado) = piso + Σ_i  Aᵢ · σ((estado − cᵢ) / wᵢ)        con σ(x)=1/(1+e^-x)

hitos i = (centro cᵢ, ancho wᵢ, aporte Aᵢ):
  700  : c=0.18, w=0.10, A= 60     # rampa temprana, ancha (barata)
  800  : c=0.55, w=0.11, A=110
  900  : c=0.83, w=0.09, A=100     # anclado a entrada de Plenitud
  1000 : c=1.07, w=0.055,A=130     # ANCHO MÍNIMO => resolución máxima (la gran meta)
  1100 : c=1.35, w=0.13, A= 50     # rampa final ancha (afloja)

puntos(estado) = 650 + (raw(estado) − raw(0)) · (1100 − 650) / (raw(1.5) − raw(0))
```

- El **ancho `w`** controla la psicología: chico = rampa empinada = resolución concentrada
  (el caso de 1000); grande = rampa suave (700 y 1100, hitos "fáciles" de cruzar o de salida).
- La reescala afín final clava **piso 650** en estado 0 y **tope 1100** en estado 1.5 sin
  romper monotonía (afín de algo monótono creciente sigue siéndolo).
- Suma de sigmoides crecientes ⇒ derivada estrictamente positiva ⇒ **monótona y continua**
  por construcción (no a tramos, suave de verdad: sin codos).

## 4. Tabla de hitos (cortes de banda + metas)

| ESTADO | Banda / significado | PUNTOS |
|-------:|---------------------|-------:|
| 0.00 | Restauración (piso digno) | **650** |
| 0.40 | → Atención | 721 |
| 0.62 | → En marcha | 788 |
| 0.85 | → Plenitud | 873 |
| 1.00 | cumplir-justo (NO da 1000) | **941** |
| 1.10 | → Inquebrantable (cruzaste el 1000 hace nada) | **1011** |
| 1.50 | tope | **1100** |

Los **números redondos perseguibles** caen así: 700≈0.30 · 800≈0.65 · 900≈0.88 · 1000≈1.09.

## 5. Verificación (python3 — script y salida REAL)

Script `/tmp/opus_e_verify.py` (incluido íntegro abajo), corrido con Bash:

```python
import math
PISO = 650.0
HITOS = [
    ("700 - salir del pozo",   0.18, 0.10,  60.0),
    ("800 - terreno firme",    0.55, 0.11, 110.0),
    ("900 - Plenitud a la vista",0.83,0.09, 100.0),
    ("1000 - lo lograste",     1.07, 0.055,130.0),
    ("1100 - techo que respira",1.35, 0.13, 50.0),
]
def sigmoid(x): return 1.0 / (1.0 + math.exp(-x))
def raw(estado):
    total = PISO
    for _, c, w, a in HITOS: total += a * sigmoid((estado - c) / w)
    return total
R0, R15 = raw(0.0), raw(1.5)
def puntos(estado):
    return 650.0 + (raw(estado) - R0) * (1100.0 - 650.0) / (R15 - R0)
xs = [i*0.05 for i in range(0,31)]
prev=None; mono=True; cont=True
for x in xs:
    p=puntos(x); d="" if prev is None else f"{p-prev:+.1f}"
    if prev is not None:
        if p < prev-1e-9: mono=False
        if p-prev > 90: cont=False
    print(f"{x:7.2f} | {p:8.1f} | {d:>7}"); prev=p
print("Monotona:",mono,"Continua:",cont)
```

Salida real (pasos de 0.05):

```
 ESTADO |   PUNTOS |   delta
------------------------------
   0.00 |    650.0 |
   0.05 |    655.0 |    +5.0
   0.10 |    661.7 |    +6.7
   0.15 |    670.1 |    +8.4
   0.20 |    679.6 |    +9.5
   0.25 |    689.6 |   +10.0
   0.30 |    699.8 |   +10.1      <- cruza 700
   0.35 |    710.2 |   +10.4
   0.40 |    721.4 |   +11.2
   0.45 |    734.0 |   +12.6
   0.50 |    748.3 |   +14.3
   0.55 |    764.1 |   +15.8
   0.60 |    780.7 |   +16.6
   0.65 |    797.8 |   +17.1      <- ~800
   0.70 |    815.4 |   +17.6
   0.75 |    833.9 |   +18.5
   0.80 |    853.4 |   +19.5
   0.85 |    873.4 |   +20.0      <- entra Plenitud
   0.90 |    893.6 |   +20.1      <- ~900
   0.95 |    915.0 |   +21.4
   1.00 |    941.2 |   +26.2      <- cumplir-justo (NO 1000)
   1.05 |    974.9 |   +33.6      <- resolucion subiendo hacia el gran hito
   1.10 |   1011.1 |   +36.2      <- cruzo 1000, entra Inquebrantable
   1.15 |   1040.3 |   +29.2
   1.20 |   1059.1 |   +18.8      <- afloja (meseta post-meta)
   1.25 |   1070.7 |   +11.6
   1.30 |   1078.7 |    +8.1
   1.35 |   1085.2 |    +6.5
   1.40 |   1090.9 |    +5.7
   1.45 |   1095.8 |    +4.9
   1.50 |   1100.0 |    +4.2
------------------------------
Monotona creciente: True
Continua (sin saltos >90 por paso 0.05): True
```

Resolución local (pts por +0.01 de ESTADO) — confirma la firma "apretado antes del 1000":

```
  cerca de 0.10:   1.5 pts/0.01
  cerca de 0.35:   2.1 pts/0.01
  cerca de 0.55:   3.3 pts/0.01
  cerca de 0.83:   4.0 pts/0.01
  cerca de 1.00:   6.0 pts/0.01
  cerca de 1.07:   7.4 pts/0.01   <- PICO: maxima resolucion justo antes del 1000
  cerca de 1.30:   1.4 pts/0.01   <- minima: ya cruzaste, afloja
```

La derivada máxima (7.4 pts/0.01) cae en 1.07 — el empujón final hacia los 1000 — y la mínima
en 1.30 — la meseta de descanso post-Inquebrantable. Esa es la firma de diseño de OPUS E.

## 6. En qué diverge de A/B/C (ronda 1)

- **A/B/C eligen una resolución global; E elige metas.** A/B/C son curvas (a tramos) cuya
  pendiente es una propiedad uniforme del tramo. E pone la pendiente **donde hay un número
  redondo que cruzar** y la quita donde no.
- **Función distinta.** A/B/C: lineal a tramos (codos). E: suma de logísticas (suave, sin
  codos, derivada continua). Más cara de explicar, pero la forma ES el mensaje.
- **El "1000" es perseguible por diseño.** C lo gana con superhabit pero como consecuencia.
  E lo *coloca* en 1.09 y aprieta la resolución antes a propósito, para que a 1.0 (=941) el
  usuario tenga la zanahoria "59 para los mil" empujándolo al esfuerzo extra.
- **Comparte con C** el piso 650 y el tope 1100 (marco confirmado por el dueño), pero la
  distribución interna es opuesta a la "resolución arriba uniforme" de C: E concentra en
  picos discretos (las metas), no en una zona.

## 7. Trade-offs (honesto)

- **A favor:** máxima fuerza motivacional cerca de cada número redondo; el 1000 se gana y se
  persigue; respeta piso digno y tope 1100; continua y monótona; la meseta post-meta da
  cierre psicológico ("llegué, descanso").
- **En contra — los redondos no caen clavados.** 800 cae en 788 a estado 0.62 (corte En
  marcha) y llega a 800 recién en ~0.65. Si el dueño quiere que CADA corte de banda coincida
  con una centena exacta, hay que mover centros/anchos (se puede, pero pelea con anclar el
  1000 a Inquebrantable — no todo entra). **Decisión de producto:** ¿los redondos siguen a
  las metas psicológicas (mi propuesta) o a los cortes de banda?
- **En contra — complejidad.** No se explica en una frase como `estado×1000` (B). Hay 5
  hitos con 3 parámetros cada uno = una tabla de tuning. A cambio, cada parámetro tiene un
  significado psicológico claro (centro=dónde está la meta, ancho=qué tan empinado el último
  empujón, aporte=cuánto sube cruzarla).
- **En contra — tuning manual.** Mover un hito cambia levemente a los vecinos (las colas
  sigmoides se solapan). Es robusto (sigue monótono siempre) pero recalibrar pide correr el
  script. Mitigación: el script ES la herramienta de calibración.
- **Riesgo de percepción:** los deltas no son uniformes entre semanas — subir de 1.0 a 1.05
  da +34 pts y de 1.3 a 1.35 da +6. Para el usuario eso es una feature ("cerca de mil cada
  acción rinde"), pero si el dueño quiere "cada % de esfuerzo vale lo mismo", esto lo viola
  a propósito.
```
