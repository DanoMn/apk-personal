# OPUS B — Mapeo ESTADO → puntos visibles ("lineal limpio + ajustar cortes")

> Panel de 3 Opus, sesgo B. Fecha 2026-06-16. Proyecto `apk-personal`.
> Contrato: `meta/instructions/2026-06-16-set-prompt-puntos-visibles.md`.

## 1. Filosofía

El mapeo más simple que existe es la **identidad escalada**: `puntos = 1000 × ESTADO`.
No hay tramos, no hay curvas, no hay pisos artificiales. Un solo número (la pendiente
1000) traduce toda la escala `[0, 1.5]` a `[0, 1500]`. Lo elegí porque cumple las dos
prioridades de mi sesgo a la vez: es **trivialmente simple** (el usuario puede entenderlo:
"mi estado por mil") y, gracias a un **único ajuste mínimo de corte** (En marcha 0.62→0.60),
**todos** los bordes de banda caen en múltiplos limpios de 50/100, y el hito sagrado
—cumplir-justo = 1.0— aterriza en el número icónico **1000**. La simplicidad no se paga
con números feos: se paga con mover un corte 0.02, un ajuste despreciable que no toca
ningún eje semántico.

## 2. La función ESTADO → puntos

```
puntos(ESTADO) = round(1000 × ESTADO)         ESTADO ∈ [0, 1.5]  →  puntos ∈ [0, 1500]
```

Lineal **global** (no a tramos): pendiente constante = 1000 puntos por unidad de ESTADO,
es decir **+50 puntos por cada +0.05 de ESTADO**, en todo el rango. Misma resolución
arriba que abajo: ningún tramo se "aplana".

## 3. Ajuste de cortes de ESTADO (antes / después)

Para que cada borde de banda caiga en múltiplo de 50/100 bajo `puntos=1000·ESTADO`,
solo hace falta mover **un** corte. Los demás ya eran limpios.

| Banda (entra en) | Corte VIEJO | Puntos viejos | Corte NUEVO | Puntos nuevos | Cambio |
|------------------|------------|---------------|------------|---------------|--------|
| Restauración     | 0.00       | 0             | 0.00       | **0**         | —      |
| Atención         | 0.40       | 400           | 0.40       | **400**       | —      |
| En marcha        | 0.62       | 620           | **0.60**   | **600**       | −0.02  |
| Plenitud         | 0.85       | 850           | 0.85       | **850**       | —      |
| Inquebrantable   | 1.10       | 1100          | 1.10       | **1100**      | —      |
| tope             | 1.50       | 1500          | 1.50       | **1500**      | —      |

**Justificación semántica del único cambio (0.62 → 0.60):**
- No toca cumplir-justo (1.0 sigue firme en zona alta de Plenitud → 1000 pts).
- No toca el borde de Plenitud (0.85, decisión "casi cumplir todo ya es Plenitud").
- No toca Inquebrantable (1.10).
- Solo ensancha 0.02 la ventana de Atención (a costa de En marcha), un desplazamiento
  inferior al paso de muestreo del propio modelo. El eje semántico
  Restauración<Atención<En marcha<Plenitud<Inquebrantable queda intacto.
- A cambio, "En marcha" abre exactamente en **600**, número que el usuario recuerda.

Con el corte viejo 0.62 también funciona (da 620, múltiplo de 20). Si el dueño prefiere
**no mover nada**, la función sigue siendo válida; solo se pierde el "600" redondo en una
banda. Mi recomendación: mover a 0.60 — es gratis y compra un número memorable.

## 4. Tabla de hitos

| ESTADO | Puntos | Banda          | Hito                         |
|--------|--------|----------------|------------------------------|
| 0.00   | 0      | Restauración   | piso real                    |
| 0.40   | 400    | Atención       | entra Atención               |
| 0.60   | 600    | En marcha      | entra En marcha (era 0.62)   |
| 0.85   | 850    | Plenitud       | entra Plenitud               |
| 1.00   | 1000   | Plenitud       | **cumplir-justo**            |
| 1.10   | 1100   | Inquebrantable | entra Inquebrantable         |
| 1.50   | 1500   | Inquebrantable | tope práctico                |

Rango de puntos por banda:

| Banda          | Rango ESTADO    | Rango puntos     | Ancho (pts) |
|----------------|-----------------|------------------|-------------|
| Restauración   | [0.00, 0.40)    | [0, 400)         | 400         |
| Atención       | [0.40, 0.60)    | [400, 600)       | 200         |
| En marcha      | [0.60, 0.85)    | [600, 850)       | 250         |
| Plenitud       | [0.85, 1.10)    | [850, 1100)      | 250         |
| Inquebrantable | [1.10, 1.50]    | [1100, 1500]     | 400         |

## 5. Postura sobre las 4 tensiones (§4)

1. **¿Piso 0 o piso ~700?** → **Piso 0, honesto.** Rechazo el piso 700 del doc viejo.
   Una función lineal global *no puede* tener piso 700 sin dejar de ser lineal o sin
   comprimir brutalmente. Y conceptualmente: si el estado es bajo, mostrar 1000 mentiría.
   El no-humillar se resuelve por **tono** (el copy de la banda Restauración), no
   inflando el número. Un 250 honesto con un mensaje compasivo ("La base está baja, una
   acción mínima ahora") respeta más al usuario que un 700 que nadie se cree.
2. **¿Dónde va la resolución?** → **Uniforme: 50 pts por cada 0.05, en todo el rango.**
   Diverjo aquí frente a A (que comprime arriba) y a la idea base del dueño. El esfuerzo
   de superávit (1.0→1.5) se ve *exactamente igual de premiado* que recuperarse desde el
   fondo: 500 puntos de movimiento en ambos extremos. Inquebrantable tiene 400 puntos
   propios de rango visible (1100→1500), nunca queda invisible. Costo: Restauración NO
   acapara puntos (400, no 700) — ver trade-offs.
3. **Números lindos** → resueltos: 0 / 400 / 600 / 850 / 1000 / 1100 / 1500, todos
   múltiplos de 50, con cumplir-justo en 1000. Y la mecánica interna es la más memorable
   posible: "puntos = estado × 1000".
4. **Continuidad** → garantizada por construcción: una sola recta, sin tramos, sin
   saltos. Paso constante de 50 pts cada 0.05 (verificado abajo).

## 6. Verificación con `python3`

### Script

```python
def points(e): return round(1000.0 * e)

def band(e):
    if e < 0.40: return "Restauracion"
    if e < 0.60: return "Atencion"        # corte movido 0.62 -> 0.60
    if e < 0.85: return "En marcha"
    if e < 1.10: return "Plenitud"
    return "Inquebrantable"

hitos = [0.00, 0.40, 0.60, 0.85, 1.00, 1.10, 1.50]
print("=== HITOS ===")
for e in hitos:
    print(f"  ESTADO {e:.2f} -> {points(e):4d} pts | {band(e)}")

print("\n=== TABLA pasos 0.05 ===")
prev = None; mono = True; cont = True; e = 0.0
while e <= 1.5001:
    pts = points(e)
    if prev is not None:
        if pts < prev: mono = False
        if abs(pts - prev) > 60: cont = False   # paso esperado = 50
    print(f"  ESTADO {e:.2f} -> {pts:4d} pts | {band(e)}")
    prev = pts; e = round(e + 0.05, 2)
print(f"\nMONOTONA: {mono}   CONTINUA (paso ~50): {cont}")
```

### Salida (ejecutada)

```
=== HITOS ===
  ESTADO 0.00 ->    0 pts | Restauracion
  ESTADO 0.40 ->  400 pts | Atencion
  ESTADO 0.60 ->  600 pts | En marcha
  ESTADO 0.85 ->  850 pts | Plenitud
  ESTADO 1.00 -> 1000 pts | Plenitud
  ESTADO 1.10 -> 1100 pts | Inquebrantable
  ESTADO 1.50 -> 1500 pts | Inquebrantable

=== TABLA pasos 0.05 ===
  ESTADO 0.00 ->    0 pts | Restauracion
  ESTADO 0.05 ->   50 pts | Restauracion
  ESTADO 0.10 ->  100 pts | Restauracion
  ESTADO 0.15 ->  150 pts | Restauracion
  ESTADO 0.20 ->  200 pts | Restauracion
  ESTADO 0.25 ->  250 pts | Restauracion
  ESTADO 0.30 ->  300 pts | Restauracion
  ESTADO 0.35 ->  350 pts | Restauracion
  ESTADO 0.40 ->  400 pts | Atencion
  ESTADO 0.45 ->  450 pts | Atencion
  ESTADO 0.50 ->  500 pts | Atencion
  ESTADO 0.55 ->  550 pts | Atencion
  ESTADO 0.60 ->  600 pts | En marcha
  ESTADO 0.65 ->  650 pts | En marcha
  ESTADO 0.70 ->  700 pts | En marcha
  ESTADO 0.75 ->  750 pts | En marcha
  ESTADO 0.80 ->  800 pts | En marcha
  ESTADO 0.85 ->  850 pts | Plenitud
  ESTADO 0.90 ->  900 pts | Plenitud
  ESTADO 0.95 ->  950 pts | Plenitud
  ESTADO 1.00 -> 1000 pts | Plenitud
  ESTADO 1.05 -> 1050 pts | Plenitud
  ESTADO 1.10 -> 1100 pts | Inquebrantable
  ESTADO 1.15 -> 1150 pts | Inquebrantable
  ESTADO 1.20 -> 1200 pts | Inquebrantable
  ESTADO 1.25 -> 1250 pts | Inquebrantable
  ESTADO 1.30 -> 1300 pts | Inquebrantable
  ESTADO 1.35 -> 1350 pts | Inquebrantable
  ESTADO 1.40 -> 1400 pts | Inquebrantable
  ESTADO 1.45 -> 1450 pts | Inquebrantable
  ESTADO 1.50 -> 1500 pts | Inquebrantable

MONOTONA: True   CONTINUA (paso ~50): True
```

Confirmado: **monótona** y **continua** con paso exacto de 50 pts por 0.05 de ESTADO.

## 7. Trade-offs / riesgos

- **Pro — máxima simplicidad y transparencia.** "Puntos = estado × 1000" es explicable
  en una frase. Cero magia, cero tramos que mantener, cero saturación. El bug del mapeo
  viejo (Inquebrantable invisible por saturar en 1000) es estructuralmente imposible aquí.
- **Pro — esfuerzo arriba premiado de forma justa.** Superávit (1.0→1.5) mueve 500 pts
  reales, igual que la recuperación de fondo. Inquebrantable ocupa 1100–1500 visibles.
- **Contra — el techo es 1500, no 1000.** Rompo la convención "tope = 1000". Si el dueño
  quiere que el número máximo sea exactamente 1000, este enfoque exige reescalar
  (`puntos = 666.7×ESTADO`), y ahí los cortes dejan de ser redondos: vuelve el conflicto
  simplicidad-vs-números-lindos. Mi apuesta: 1500 como techo es aceptable y hasta deseable
  (deja "headroom" psicológico visible para el superávit). Es la tensión central de mi
  diseño y la decido a favor de los números redondos en los cortes.
- **Contra — Restauración NO acapara puntos (400, no 700).** Diverjo de la idea base del
  dueño y de A. Quien está mal ve números bajos (hasta 0). Lo asumo: el no-humillar es
  trabajo del copy, no del número. Si el dueño insiste en el "gap grande" para
  Restauración, mi enfoque no es el suyo — ese es A.
- **Riesgo de percepción** — números de 4 dígitos (1100–1500) pueden sentirse "infinitos"
  sin un tope visual claro. Mitigación de UI: mostrar "1250 / 1500" o una barra que llene
  hasta 1500. No cambia la función.
