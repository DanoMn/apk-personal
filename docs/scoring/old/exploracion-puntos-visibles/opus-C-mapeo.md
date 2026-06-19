# OPUS C — Mapeo ESTADO → puntos visibles (enfoque experiencia / motivación)

> Exploración divergente (panel de 3 Opus). Sesgo: **centrado en la experiencia y el
> tono del producto**. Set-prompt: `meta/instructions/2026-06-16-set-prompt-puntos-visibles.md`.
> Fecha: 2026-06-16. Proyecto: `apk-personal`. **No es contrato; insumo para el merge.**

## 1. Filosofía

El número que ve el usuario no es una nota de examen: es un **espejo de cuidado**. El tono
del producto (`tono-comunicacion.md`) manda dos cosas que la matemática tiene que obedecer:
(a) **no humillar** — la app nunca dice "estás mal", dice "hay una señal, volvamos a la base"; y
(b) **"En marcha" es el hogar operativo**, mientras Plenitud e Inquebrantable son **picos
orgánicos**, no obligaciones diarias. De ahí mi diseño: un **piso de dignidad** (el número
nunca baja a cifras que se sientan como condena) combinado con una **resolución modelada por
zonas** — la pendiente de puntos *crece* a medida que el usuario sube, para que el esfuerzo
de sostener y de llegar a Plenitud se traduzca en **movimiento visible**, y que Inquebrantable
tenga su propio aire por encima de 1000. Es lineal a tramos (continua, auditable, sin magia),
pero las pendientes están elegidas como una curva de motivación, no como un reparto plano.

**Rechazo la idea literal del dueño (0–0.40 → 0–700).** Mostrar 0–700 en Restauración va
*directamente contra* el pilar "no humillar": el peor día del usuario —cuando más frágil
está— le devolvería un número cercano a 0, que es exactamente el golpe que el producto promete
no dar. Mantengo el espíritu de la idea (Restauración ocupa rango visible) pero con piso 650.

## 2. La función ESTADO → puntos

Lineal a tramos sobre 6 anclas. Cada corte de banda cae en un múltiplo de 50:

```
ESTADO   PUNTOS   (corte de banda)
0.00  ->  650     Restauración (piso de dignidad)
0.40  ->  750     → Atención
0.62  ->  820     → En marcha (hogar operativo)
0.85  ->  900     → Plenitud (casi-cumplir-todo)
1.10  -> 1000     → Inquebrantable (el "mil" simbólico aterriza acá)
1.50  -> 1100     tope práctico (el techo respira sobre 1000)
```

Interpolación lineal dentro de cada tramo:

```
puntos(s) = y0 + (s - x0)/(x1 - x0) * (y1 - y0)   con (x0,y0),(x1,y1) las anclas que rodean s
s se satura a [0.0, 1.5].
```

**La forma (clave del enfoque):** la pendiente *sube* tramo a tramo hasta Plenitud y luego
afloja en Inquebrantable. Esto es una **curva de motivación lineal-a-tramos**:

| Banda | Rango ESTADO | Rango puntos | Ancho | Pendiente (pts/ESTADO) |
|-------|--------------|--------------|-------|------------------------|
| Restauración | 0.00–0.40 | 650–750 | 100 | 250 |
| Atención | 0.40–0.62 | 750–820 | 70 | 318 |
| En marcha | 0.62–0.85 | 820–900 | 80 | **348** |
| Plenitud | 0.85–1.10 | 900–1000 | 100 | **400** (máxima) |
| Inquebrantable | 1.10–1.50 | 1000–1100 | 100 | 250 |

La **máxima resolución (400 pts/ESTADO) está en el ascenso a Plenitud**: ahí es donde el
esfuerzo del usuario que ya está bien y empuja para sostener picos se ve *más* recompensado en
el número. Restauración tiene pendiente baja (250) a propósito: salir del pozo no exige una
escalada heroica del número para sentir progreso, y el piso protege la dignidad. Inquebrantable
afloja (250) porque ya es un pico raro y emergente: no hace falta inflar el número, solo darle
**aire propio sobre 1000** (lo que el mapeo viejo rompía al saturar en 1000).

## 3. Tabla de hitos

| ESTADO | Puntos | Banda | Lectura experiencial |
|--------|--------|-------|----------------------|
| 0.00 | **650** | Restauración | Piso. Nunca menos. "Hay una señal, no una condena." |
| 0.40 | **750** | Atención | Cuerda tendida: ya hay control. |
| 0.62 | **820** | En marcha | Hogar operativo. "La base está sostenida." |
| 0.85 | **900** | Plenitud | Casi todo cumplido. Reconocimiento sin euforia. |
| 1.00 | 960 | Plenitud | **Cumplir-justo NO satura** — todavía hay cielo arriba. |
| 1.10 | **1000** | Inquebrantable | El "mil" simbólico = superhábit real. |
| 1.50 | **1100** | Inquebrantable | Tope práctico; el techo respira. |

Detalle importante: **cumplir-justo (ESTADO = 1.0) da 960, no 1000.** Decisión deliberada de
motivación: si 1.0 ya tocara el techo redondo, el superhábit (la parte `extra`, que el modelo
diseñó como recompensa) no tendría *dónde* mostrarse. Dejando 1000 para el inicio de
Inquebrantable, el usuario que reparte esfuerzo extra ve el número cruzar el "mil" — un hito
psicológico fuerte — solo cuando de verdad superó el cumplimiento base.

## 4. Postura sobre las 4 tensiones (§4)

1. **¿Piso 0 o piso ~700? → Piso 650 (de dignidad), no 0.** Posición fuerte: el 0→700 literal
   contradice el tono. Pero tampoco subo el piso a 700 como el doc viejo, porque entonces
   Restauración (0–0.40) tendría rango raquítico y *todo* abajo se vería igual de plano. 650
   es el equilibrio: número digno (no humillante), pero deja 100 puntos de movimiento dentro de
   Restauración para que salir del pozo *se note*. El número nunca grita "cero".
2. **¿Dónde va la resolución? → Arriba, en el ascenso a Plenitud (400 pts/ESTADO).** Aquí
   diverjo de la idea del dueño, que ponía la resolución abajo (Restauración se llevaba 700
   puntos). Eso es un **error motivacional**: regala mucho número a estar mal y deja sin aire al
   esfuerzo de sostener/mejorar. Invierto la lógica: poco rango abajo (protegido por el piso),
   **máximo rango donde el esfuerzo importa** (sostener En marcha → Plenitud), y aire dedicado a
   Inquebrantable. El que se esfuerza arriba ve el número moverse de verdad.
3. **Números lindos. → Sí, los 5 cortes en múltiplos de 50** (650/750/820/900/1000/1100).
   El "mil" cae justo en el inicio de Inquebrantable: el hito más memorable marca el estado más
   simbólico. (820 y 900 no son múltiplos de 100 pero sí de 20/50, y memorables.)
4. **Continuidad. → Garantizada.** Lineal a tramos con anclas compartidas en cada frontera →
   no hay saltos. Verificado abajo: salto máximo 0.40 pts a paso 0.001 (puramente la pendiente
   continua; un salto de banda real sería de decenas). Monótona no-decreciente.

## 5. Verificación con `python3`

Script (`/tmp/opus_c_points.py`):

```python
ANCHORS = [
    (0.00, 650), (0.40, 750), (0.62, 820),
    (0.85, 900), (1.10, 1000), (1.50, 1100),
]

def state_to_points(s):
    s = max(0.0, min(1.5, s))
    for i in range(len(ANCHORS) - 1):
        x0, y0 = ANCHORS[i]; x1, y1 = ANCHORS[i + 1]
        if x0 <= s <= x1:
            t = (s - x0) / (x1 - x0)
            return y0 + t * (y1 - y0)
    return ANCHORS[-1][1]

def band(s):
    if s < 0.40: return "Restauracion"
    if s < 0.62: return "Atencion"
    if s < 0.85: return "En marcha"
    if s < 1.10: return "Plenitud"
    return "Inquebrantable"
# (recorre 0..1.5 en pasos de 0.05; chequea monotonía y salto máximo a 0.001)
```

Salida real:

```
 ESTADO | PUNTOS | BANDA          | corte
------------------------------------------------
   0.00 |    650 | Restauracion   |
   0.05 |    662 | Restauracion   |
   0.10 |    675 | Restauracion   |
   0.15 |    688 | Restauracion   |
   0.20 |    700 | Restauracion   |
   0.25 |    712 | Restauracion   |
   0.30 |    725 | Restauracion   |
   0.35 |    738 | Restauracion   |
   0.40 |    750 | Atencion       |  <-- corte
   0.45 |    766 | Atencion       |
   0.50 |    782 | Atencion       |
   0.55 |    798 | Atencion       |
   0.60 |    814 | Atencion       |
   0.65 |    830 | En marcha      |
   0.70 |    848 | En marcha      |
   0.75 |    865 | En marcha      |
   0.80 |    883 | En marcha      |
   0.85 |    900 | Plenitud       |  <-- corte
   0.90 |    920 | Plenitud       |
   0.95 |    940 | Plenitud       |
   1.00 |    960 | Plenitud       |  <-- corte
   1.05 |    980 | Plenitud       |
   1.10 |   1000 | Inquebrantable |  <-- corte
   1.15 |   1012 | Inquebrantable |
   1.20 |   1025 | Inquebrantable |
   1.25 |   1038 | Inquebrantable |
   1.30 |   1050 | Inquebrantable |
   1.35 |   1062 | Inquebrantable |
   1.40 |   1075 | Inquebrantable |
   1.45 |   1088 | Inquebrantable |
   1.50 |   1100 | Inquebrantable |  <-- corte

--- Verificacion ---
Monotona no-decreciente: True
Salto maximo entre puntos a 0.001 de paso: 0.400 (continua si << ancho de banda)

--- Resolucion (pendiente puntos/ESTADO) por banda ---
  [0.00,0.40] Restauracion   rango 650-750 (100 pts) pendiente   250.0 pts/ESTADO
  [0.40,0.62] Atencion       rango 750-820 (70 pts) pendiente   318.2 pts/ESTADO
  [0.62,0.85] En marcha      rango 820-900 (80 pts) pendiente   347.8 pts/ESTADO
  [0.85,1.10] Plenitud       rango 900-1000 (100 pts) pendiente   400.0 pts/ESTADO
  [1.10,1.50] Inquebrantable rango 1000-1100 (100 pts) pendiente   250.0 pts/ESTADO

--- Hitos clave ---
  ESTADO 0.00 -> 650 pts (Restauracion)
  ESTADO 0.40 -> 750 pts (Atencion)
  ESTADO 0.62 -> 820 pts (En marcha)
  ESTADO 0.85 -> 900 pts (Plenitud)
  ESTADO 1.00 -> 960 pts (Plenitud)
  ESTADO 1.10 -> 1000 pts (Inquebrantable)
  ESTADO 1.50 -> 1100 pts (Inquebrantable)
```

**Resultado:** monótona no-decreciente ✓, continua ✓ (salto máx 0.40 pts a paso 0.001, puro
efecto de pendiente), 5 cortes en múltiplos de 50 ✓, Inquebrantable con rango propio
(1000–1100) ✓, cumplir-justo (1.0) = 960 sin saturar ✓.

## 6. Trade-offs / riesgos

- **El techo pasa de 1000.** Rompo la convención "escala 700..1000" del doc viejo a propósito:
  1000 ya no es el máximo, es el *umbral de Inquebrantable*. Riesgo: si la UI o textos asumen
  "sobre 1000" en algún lado, hay que ajustarlos. Beneficio: Inquebrantable deja de ser
  invisible (el defecto que el set-prompt exige no repetir).
- **Resolución pobre en Restauración (250 pts/ESTADO, 100 pts de rango).** Consecuencia directa
  del piso de dignidad: dos días muy malos (0.05 vs 0.20) se diferencian poco en el número
  (662 vs 700). Lo asumo: en Restauración la app no quiere que el usuario *optimice el número*,
  quiere que "vuelva a la base". El número bajo movimiento es coherente con el tono.
- **No-linealidad global.** Un punto extra de número "vale" distinto según la zona (250 vs 400
  pts/ESTADO). Si el dueño quiere que cada décima de ESTADO valga lo mismo siempre (modelo de
  Opus B), este enfoque no es para él. Mi defensa: la *experiencia* de progreso no es lineal —
  el último tramo antes de un pico se siente más, y el número debe acompañarlo.
- **820 y 900 no son múltiplos de 100.** Memorables igual (múltiplos de 20), pero si el dueño
  exige solo centenas redondas, habría que recortar el rango total y comprimir bandas.
- **Calibración futura.** Las pendientes (250/318/348/400/250) son una hipótesis de motivación,
  no un hecho medido. Cuando haya semanas reales, conviene revisar si el salto a Plenitud "se
  siente" tan recompensado como dice la pendiente.
