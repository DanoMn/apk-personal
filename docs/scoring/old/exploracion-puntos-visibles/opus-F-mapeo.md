# OPUS F — mapeo ESTADO → PUNTOS (psicofísica / Weber-Fechner)

> Ronda 2 del panel de mapeo `ESTADO ∈ [0, 1.5]` → puntos visibles del dashboard.
> Proyecto: `apk-personal`. Fecha: 2026-06-16. Enfoque: **exponencial pura** derivada de
> la ley de Weber-Fechner. NO es lineal (ni lineal-a-tramos como la ronda 1).

## 1. Filosofía

### La intuición en criollo

Pensá en el volumen de la música. Subir de 10 a 20 se NOTA un montón. Subir de 90 a 100
—el mismo +10— casi no se escucha. El oído no mide diferencias, mide **proporciones**. Lo
mismo con el brillo, el peso, y —acá está la clave— con los **números que mirás en una
pantalla**. Un +50 puntos sobre 600 se SIENTE enorme; ese mismo +50 sobre 1000 se siente
flojo. Eso es la **ley de Weber-Fechner**: lo que percibimos es el logaritmo de la magnitud,
no la magnitud cruda. El cambio percibido ≈ Δ / valor.

¿Qué problema mató a la ronda 1? Que repartían los puntos de forma **aritmética** (lineal o
lineal a tramos). Resultado: el mismo esfuerzo se sentía DISTINTO según dónde estabas. En
OPUS A, moverte abajo movía el número 14× más que arriba → arriba el esfuerzo "no se veía".
En OPUS C lo invirtieron a mano (resolución arriba), pero sigue siendo una decisión
*arbitraria* de cuánto poner en cada tramo.

Mi propuesta no decide eso a mano. Lo **deriva de la percepción**: si querés que un mismo
salto de ESTADO se SIENTA igual en cualquier punto de la barra, los puntos tienen que crecer
de forma **exponencial**. Y cuando hacés eso, pasa algo hermoso: la columna de "cambio
percibido" sale **exactamente constante**. Cada 0.05 de ESTADO se siente igual de
gratificante, estés en Restauración o en Inquebrantable. No hay zona muerta. No hay zona
donde el esfuerzo "no se ve". El usuario percibe progreso parejo SIEMPRE.

Y como bonus, en puntos CRUDOS la curva crece cada vez más rápido (de +11.5 abajo a +19.1
arriba). O sea: para el ojo, parejo; para la aritmética, premia más arriba. Lo mejor de los
dos mundos, sin un solo número puesto a dedo.

### Por qué exponencial y no log

Weber-Fechner dice: percepción = k·ln(magnitud). Si queremos que percepción sea **lineal en
ESTADO** (cada paso de ESTADO = mismo paso percibido), despejamos:

```
percepción ∝ ESTADO   y   percepción ∝ ln(PUNTOS)
⟹  ln(PUNTOS) ∝ ESTADO   ⟹  PUNTOS = A·e^(B·ESTADO)   (EXPONENCIAL)
```

Una curva *log* haría lo contrario (aplastar arriba, que es justo el pecado de OPUS A). La
exponencial es la única forma que hace el progreso **perceptualmente uniforme**.

## 2. La función

Fijamos los dos extremos del contrato:
- `PUNTOS(0) = 650` → piso digno (la app no humilla).
- `PUNTOS(1.5) = 1100` → tope confirmado (Inquebrantable visible, el techo respira sobre 1000).

Forma cerrada:

```
PUNTOS(ESTADO) = 650 · (1100/650)^(ESTADO / 1.5)
              = 650 · e^(0.35073 · ESTADO)
```

donde `(1100/650) = 1.6923` es el factor de crecimiento total, repartido geométricamente
sobre el rango. Continua, suave (C∞), estrictamente creciente, sin tramos ni costuras.

**El "1000" no es el tope ni se regala al cumplir.** Cae en `ESTADO ≈ 1.23` —superhabit
sostenido, bien dentro de Inquebrantable—. Cumplir-justo (1.0) da 923; entrar a Inquebrantable
(1.10) da 956. El "mil" se GANA destacándose, exactamente como pidió el dueño.

## 3. Tabla de hitos

| ESTADO | PUNTOS | Banda |
|---|---|---|
| 0.00 | **650.0** | Restauración (piso digno — nunca menos) |
| 0.40 | 747.9 | → Atención |
| 0.62 | 807.9 | → En marcha (hogar operativo) |
| 0.85 | 875.8 | → Plenitud |
| 1.00 | 923.1 | cumplir-justo (queda cielo arriba) |
| 1.10 | 956.0 | → Inquebrantable |
| ~1.23 | **1000.0** | el "mil" se gana con superhabit |
| 1.50 | **1100.0** | tope (el techo respira sobre 1000) |

## 4. Verificación (python3 — salida REAL)

Script (`/tmp/opusF3.py`):

```python
import math
P0, PMAX, SMAX = 650.0, 1100.0, 1.5
R = PMAX/P0
def points(state):
    s = max(0.0, min(SMAX, state))
    return P0 * R**(s/SMAX)
# hitos + barrido 0.05 con columna de "cambio percibido" = 100*dPts/prev (Weber)
# monotonía, max salto, desviación de la percepción, y dónde cae el 1000
```

Salida real (barrido en pasos de 0.05; `dPercibido% = 100·ΔPuntos/Puntos_previo`):

```
HITOS — exponencial pura Weber-Fechner
 ESTADO   PUNTOS  banda
   0.00    650.0  Restauracion (piso digno)
   0.40    747.9  -> Atencion
   0.62    807.9  -> En marcha
   0.85    875.8  -> Plenitud
   1.00    923.1  cumplir-justo
   1.10    956.0  -> Inquebrantable
   1.50   1100.0  tope

BARRIDO 0.05
 ESTADO   PUNTOS    dPts dPercibido%
   0.00    650.0    0.00     0.0000%
   0.05    661.5   11.50     1.7691%
   0.10    673.2   11.70     1.7691%
   0.15    685.1   11.91     1.7691%
   0.20    697.2   12.12     1.7691%
   0.25    709.6   12.33     1.7691%
   0.30    722.1   12.55     1.7691%
   0.35    734.9   12.78     1.7691%
   0.40    747.9   13.00     1.7691%
   0.45    761.1   13.23     1.7691%
   0.50    774.6   13.47     1.7691%
   0.55    788.3   13.70     1.7691%
   0.60    802.2   13.95     1.7691%
   0.65    816.4   14.19     1.7691%
   0.70    830.9   14.44     1.7691%
   0.75    845.6   14.70     1.7691%
   0.80    860.5   14.96     1.7691%
   0.85    875.8   15.22     1.7691%
   0.90    891.3   15.49     1.7691%
   0.95    907.0   15.77     1.7691%
   1.00    923.1   16.05     1.7691%
   1.05    939.4   16.33     1.7691%
   1.10    956.0   16.62     1.7691%
   1.15    972.9   16.91     1.7691%
   1.20    990.1   17.21     1.7691%
   1.25   1007.7   17.52     1.7691%
   1.30   1025.5   17.83     1.7691%
   1.35   1043.6   18.14     1.7691%
   1.40   1062.1   18.46     1.7691%
   1.45   1080.9   18.79     1.7691%
   1.50   1100.0   19.12     1.7691%

monotona_no_decreciente = True
max_salto_por_paso = 19.12 pts
cambio_percibido_medio = 1.7691%  desv = 0.000000pp  <- CONSTANTE (Weber)
el '1000' se gana en ESTADO = 1.2283 (superhabit sostenido)
forma cerrada: PUNTOS = 650 * (1100/650)^(ESTADO/1.5) = 650 * exp(0.35073*ESTADO)
```

**Lecturas clave de la verificación:**
- **Monótona y continua**: `mono=True`, máximo salto por paso 19.1 pts (sin brincos).
- **Percepción perfectamente uniforme**: la columna `dPercibido%` es **1.7691% constante**,
  desviación **0.000000pp**. Esto NO es coincidencia: es la firma matemática de Weber-Fechner.
  Cada 0.05 de ESTADO se siente exactamente igual de gratificante en toda la barra.
- **Puntos crudos crecientes**: de +11.5 (abajo) a +19.1 (arriba) → el esfuerzo arriba
  rinde MÁS en aritmética, justo lo opuesto al pecado de OPUS A.
- **1000 se gana**: cae en ESTADO 1.23 (superhabit), no en cumplir.

## 5. Trade-offs

**A favor:**
- **Único enfoque sin parámetros a dedo.** A y C eligieron a mano cuántos puntos por tramo.
  Acá solo fijás piso y tope; el reparto lo dicta la percepción. Cero arbitrariedad interna.
- **Progreso percibido uniforme.** No hay zona muerta. El usuario en Restauración y el de
  Inquebrantable sienten el mismo "tirón" por cada paso. Coherente con "no humillar" Y con
  "premiar el esfuerzo" a la vez.
- **Resuelve la queja de ronda 1 sin invertir nada.** En puntos crudos la resolución crece
  hacia arriba (premia superhabit) sin que nadie haya decidido "poné más arriba": emerge solo.
- **1000 como hito ganado** (1.23), piso 650, tope 1100: cumple los tres anclajes del dueño.

**En contra:**
- **Menos explicable que B.** "estado×1000" se cuenta en una frase; "exponencial Weber-Fechner"
  necesita el párrafo de la música. Pero el usuario NO ve la fórmula —ve un número que se mueve
  rico—; la complejidad es interna y se justifica.
- **Cumplir-justo da 923, no número redondo.** Si el dueño quiere hitos en centenas exactas
  (900/1000) en cortes de banda, hay que romper la pureza con un warp (lo probé; degrada la
  constante perceptual). Mi recomendación: NO redondear los cortes; el valor está en la uniformidad.
- **Diferencia chica abajo en crudo.** De 0→0.40 son ~98 pts (vs 100 en C). Casi igual; no es
  un costo real, pero quien quiera "más drama abajo" lo notará menos que en OPUS A.

## 6. Síntesis

`PUNTOS = 650 · (1100/650)^(ESTADO/1.5)`. Una sola curva exponencial, dos anclas (650, 1100),
y la percepción humana hace el resto: progreso que se SIENTE parejo en toda la barra, premio
que CRECE arriba en crudo, y el "1000" ganado con superhabit. Es el mapeo más sofisticado del
panel porque deriva el reparto de un principio (Weber-Fechner) en vez de elegirlo a mano.
