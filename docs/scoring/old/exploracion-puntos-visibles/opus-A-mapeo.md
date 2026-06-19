# OPUS A — Mapeo ESTADO → puntos visibles ("fiel a la idea del dueño")

> Exploración divergente (panel de 3 Opus). Set-prompt: `meta/instructions/2026-06-16-set-prompt-puntos-visibles.md`.
> Sesgo de este documento: **implementar LITERAL la idea del dueño** y mostrar su costo con números, sin maquillarlo.
> Fecha: 2026-06-16. Proyecto: `apk-personal`. NO es contrato cerrado — es una de las tres propuestas para el merge.

## 1. Filosofía

La idea del dueño manda: **el camino de salida del pozo es el que más tiene que verse**. Restauración
(ESTADO 0→0.40) es el tramo donde la persona está peor y donde cada acción mínima importa más, así que
se le da el **gap más grande de puntos: 0→700**. Subir desde el fondo se traduce en un número que se
mueve fuerte (87.5 pts cada 0.05 de ESTADO), porque ahí es donde el feedback motivacional rinde. De
0.40 (entrar a Atención) hasta 1.5 (tope de Inquebrantable) se reparten los 300 puntos restantes
(700→1000), con los cortes de banda cayendo en números **lindos y memorables**: 700 · 800 · 900 · 950 ·
1000. El precio —que arriba el número se mueva poco— se asume a conciencia y se cuantifica abajo: es el
costo honesto de poner toda la resolución donde el dueño dijo que la quería.

## 2. La función ESTADO → puntos (lineal a tramos)

Interpolación lineal entre 6 nodos `(ESTADO, PUNTOS)`. Cada corte de banda es un nodo **compartido** por
los dos tramos contiguos, así que la función es continua por construcción (no hay saltos).

```text
Nodos:
  (0.00,    0)   inicio Restauración
  (0.40,  700)   corte Restauración → Atención
  (0.62,  800)   corte Atención → En marcha
  (0.85,  900)   corte En marcha → Plenitud
  (1.10,  950)   corte Plenitud → Inquebrantable
  (1.50, 1000)   tope práctico de Inquebrantable

PUNTOS(e) =  lineal por tramo, con e recortado a [0, 1.5]:

  0.00 ≤ e < 0.40 :  PUNTOS =   0 + (e - 0.00)/0.40 * 700      =   0 + 1750.0·e
  0.40 ≤ e < 0.62 :  PUNTOS = 700 + (e - 0.40)/0.22 * 100      ≈ 700 +  454.5·(e-0.40)
  0.62 ≤ e < 0.85 :  PUNTOS = 800 + (e - 0.62)/0.23 * 100      ≈ 800 +  434.8·(e-0.62)
  0.85 ≤ e < 1.10 :  PUNTOS = 900 + (e - 0.85)/0.25 *  50      = 900 +  200.0·(e-0.85)
  1.10 ≤ e ≤ 1.50 :  PUNTOS = 950 + (e - 1.10)/0.40 *  50      = 950 +  125.0·(e-1.10)
```

Mostrar al usuario `round(PUNTOS(e))` (entero). Los cortes de banda dan enteros exactos por diseño.

## 3. Tabla de hitos

| ESTADO | Banda que abre | PUNTOS | Rango de puntos de la banda | Ancho ESTADO | Resolución |
|-------:|----------------|-------:|-----------------------------|-------------:|-----------:|
| 0.00 | Restauración (inicio) | **0** | **0 – 699** | 0.40 | 17.50 pts / 0.01 |
| 0.40 | Atención | **700** | **700 – 799** | 0.22 | 4.55 pts / 0.01 |
| 0.62 | En marcha | **800** | **800 – 899** | 0.23 | 4.35 pts / 0.01 |
| 0.85 | Plenitud | **900** | **900 – 949** | 0.25 | 2.00 pts / 0.01 |
| 1.00 | (cumplir-justo, dentro de Plenitud) | **930** | — | — | — |
| 1.10 | Inquebrantable | **950** | **950 – 1000** | 0.40 | 1.25 pts / 0.01 |
| 1.50 | Inquebrantable (tope) | **1000** | — | — | — |

Lectura clave: **cumplir todo justo (ESTADO = 1.0) = 930 puntos**, cómodamente dentro de Plenitud
(900–949), coherente con el eje semántico (§16-NUEVO: 1.0 cae en zona alta de Plenitud, no en su inicio).
Inquebrantable nace en 950 y tiene su propio rango visible (950→1000), respetando la regla dura del §2
del set-prompt: el mapeo viejo lo dejaba invisible; este no.

## 4. Postura sobre las 4 tensiones (§4 del set-prompt)

1. **¿Piso 0 o piso ~700?** → **Piso 0.** Es la consecuencia directa e ineludible de la idea del dueño:
   si Restauración ocupa 0→700, entonces el fondo del fondo es 0. Esto **revierte** el "no mostrar bajo
   700" del `arbol §3.2`. Mitigación honesta: el tono (no humillar) NO está en el número sino en el
   *texto que lo acompaña* ("La base está baja. Una acción mínima ahora."). Un número que sube fuerte
   desde el fondo (87.5 pts por 0.05) puede leerse como motivador —"me estoy moviendo"— en vez de
   humillante. Aun así, mostrar 0–150 en una mala semana es el riesgo emocional más grande de este
   enfoque y queda señalado para el merge (ver §6).
2. **¿Dónde va la resolución?** → **Toda abajo, a propósito.** Restauración tiene 17.50 pts/0.01;
   Inquebrantable 1.25 pts/0.01 → **ratio 14×** a favor del fondo. Es deseable según la idea (el que
   está mal necesita ver movimiento), pero tiene costo motivacional arriba: ver §6. Lo asumo, no lo
   escondo.
3. **Números lindos** → **Resuelto:** 700 · 800 · 900 · 950 · 1000. Cuatro de los cinco cortes son
   múltiplos de 100; el quinto (950) es múltiplo de 50. Todos memorables. No moví los cortes de ESTADO
   (0.40/0.62/0.85/1.10): la idea del dueño se sostiene con números visibles limpios SIN tocarlos.
4. **Continuidad** → **Garantizada.** Lineal a tramos con nodos compartidos. El script confirma 0 salto
   en cada corte y monotonía no-decreciente en toda la escala (paso 0.05). Sí hay **cambios de pendiente
   visibles** en los cortes (de 87.5 a 22.7 pts/0.05 en 0.40): es continuo pero "quiebra" — inherente a
   priorizar el fondo.

## 5. Verificación con `python3`

### Script

```python
# OPUS A — verificacion de la funcion ESTADO -> puntos (lineal a tramos)
NODES = [
    (0.00,   0),   # Restauracion arranca en 0
    (0.40, 700),   # fin Restauracion / inicio Atencion
    (0.62, 800),   # fin Atencion / inicio En marcha
    (0.85, 900),   # fin En marcha / inicio Plenitud
    (1.10, 950),   # fin Plenitud / inicio Inquebrantable
    (1.50, 1000),  # tope practico Inquebrantable
]

def estado_to_points(e):
    e = max(0.0, min(1.5, e))
    for i in range(len(NODES) - 1):
        e0, p0 = NODES[i]; e1, p1 = NODES[i+1]
        if e <= e1 or i == len(NODES) - 2:
            t = (e - e0) / (e1 - e0)
            return p0 + t * (p1 - p0)
    return NODES[-1][1]

def banda(e):
    if e < 0.40: return "Restauracion"
    if e < 0.62: return "Atencion"
    if e < 0.85: return "En marcha"
    if e < 1.10: return "Plenitud"
    return "Inquebrantable"

print(f"{'ESTADO':>7} | {'PUNTOS':>7} | {'BANDA':<14} | {'pts/0.05':>9}")
print("-" * 50)
prev_p = None; mono = True; e = 0.0
while e <= 1.5001:
    p = estado_to_points(e)
    delta = "" if prev_p is None else f"{p - prev_p:+.1f}"
    if prev_p is not None and p < prev_p - 1e-9: mono = False
    marca = "  <-- corte" if round(e,2) in (0.40,0.62,0.85,1.10) else ("  <-- cumplir-justo" if abs(e-1.0)<1e-9 else "")
    print(f"{e:>7.2f} | {p:>7.1f} | {banda(e):<14} | {delta:>9}{marca}")
    prev_p = p; e = round(e + 0.05, 2)

print("\nContinuidad en los cortes:")
for e_cut in (0.40, 0.62, 0.85, 1.10):
    print(f"  ESTADO={e_cut:.2f}: valor_unico_en_nodo={estado_to_points(e_cut):.2f}  (mismo punto para ambos tramos -> sin salto)")

print("\nResolucion (puntos por 0.01 de ESTADO, por banda):")
for (e0,p0),(e1,p1) in zip(NODES, NODES[1:]):
    res = (p1 - p0) / ((e1 - e0) / 0.01)
    print(f"  [{e0:.2f},{e1:.2f}) ancho_estado={e1-e0:.2f} ancho_pts={p1-p0:>4} -> {res:6.2f} pts/0.01")

print(f"\nMonotona no-decreciente: {mono}")
print("Continua en los cortes:  True (cada corte es un nodo compartido por los dos tramos)")
print("\nCOSTO DE LA IDEA (resolucion arriba):")
print(f"  Restauracion (0.00->0.40): mueve {estado_to_points(0.40)-estado_to_points(0.0):.0f} pts en 0.40")
print(f"  Inquebrant.  (1.10->1.50): mueve {estado_to_points(1.50)-estado_to_points(1.10):.0f} pts en 0.40")
print(f"  Ratio resolucion Restauracion / Inquebrantable = {((estado_to_points(0.40)-estado_to_points(0.0))/0.40) / ((estado_to_points(1.50)-estado_to_points(1.10))/0.40):.1f}x")
```

### Salida real

```text
 ESTADO |  PUNTOS | BANDA          |  pts/0.05
--------------------------------------------------
   0.00 |     0.0 | Restauracion   |
   0.05 |    87.5 | Restauracion   |     +87.5
   0.10 |   175.0 | Restauracion   |     +87.5
   0.15 |   262.5 | Restauracion   |     +87.5
   0.20 |   350.0 | Restauracion   |     +87.5
   0.25 |   437.5 | Restauracion   |     +87.5
   0.30 |   525.0 | Restauracion   |     +87.5
   0.35 |   612.5 | Restauracion   |     +87.5
   0.40 |   700.0 | Atencion       |     +87.5  <-- corte
   0.45 |   722.7 | Atencion       |     +22.7
   0.50 |   745.5 | Atencion       |     +22.7
   0.55 |   768.2 | Atencion       |     +22.7
   0.60 |   790.9 | Atencion       |     +22.7
   0.65 |   813.0 | En marcha      |     +22.1
   0.70 |   834.8 | En marcha      |     +21.7
   0.75 |   856.5 | En marcha      |     +21.7
   0.80 |   878.3 | En marcha      |     +21.7
   0.85 |   900.0 | Plenitud       |     +21.7  <-- corte
   0.90 |   910.0 | Plenitud       |     +10.0
   0.95 |   920.0 | Plenitud       |     +10.0
   1.00 |   930.0 | Plenitud       |     +10.0  <-- cumplir-justo
   1.05 |   940.0 | Plenitud       |     +10.0
   1.10 |   950.0 | Inquebrantable |     +10.0  <-- corte
   1.15 |   956.2 | Inquebrantable |      +6.2
   1.20 |   962.5 | Inquebrantable |      +6.2
   1.25 |   968.8 | Inquebrantable |      +6.2
   1.30 |   975.0 | Inquebrantable |      +6.2
   1.35 |   981.2 | Inquebrantable |      +6.2
   1.40 |   987.5 | Inquebrantable |      +6.2
   1.45 |   993.8 | Inquebrantable |      +6.2
   1.50 |  1000.0 | Inquebrantable |      +6.2

Continuidad en los cortes:
  ESTADO=0.40: valor_unico_en_nodo=700.00  (mismo punto para ambos tramos -> sin salto)
  ESTADO=0.62: valor_unico_en_nodo=800.00  (mismo punto para ambos tramos -> sin salto)
  ESTADO=0.85: valor_unico_en_nodo=900.00  (mismo punto para ambos tramos -> sin salto)
  ESTADO=1.10: valor_unico_en_nodo=950.00  (mismo punto para ambos tramos -> sin salto)

Resolucion (puntos por 0.01 de ESTADO, por banda):
  [0.00,0.40) ancho_estado=0.40 ancho_pts= 700 ->  17.50 pts/0.01
  [0.40,0.62) ancho_estado=0.22 ancho_pts= 100 ->   4.55 pts/0.01
  [0.62,0.85) ancho_estado=0.23 ancho_pts= 100 ->   4.35 pts/0.01
  [0.85,1.10) ancho_estado=0.25 ancho_pts=  50 ->   2.00 pts/0.01
  [1.10,1.50) ancho_estado=0.40 ancho_pts=  50 ->   1.25 pts/0.01

Monotona no-decreciente: True
Continua en los cortes:  True (cada corte es un nodo compartido por los dos tramos)

COSTO DE LA IDEA (resolucion arriba):
  Restauracion (0.00->0.40): mueve 700 pts en 0.40
  Inquebrant.  (1.10->1.50): mueve 50 pts en 0.40
  Ratio resolucion Restauracion / Inquebrantable = 14.0x
```

**Verificado:** monótona no-decreciente, continua en los 4 cortes, cortes en números lindos
(700/800/900/950/1000), Inquebrantable con rango propio (950→1000), cumplir-justo=930 dentro de Plenitud.

## 6. Trade-offs / riesgos

- **EL COSTO CENTRAL (cuantificado): resolución 14× peor arriba.** Una persona en Restauración ve el
  número moverse a 17.50 pts por cada 0.01 de ESTADO; una en Inquebrantable, a 1.25. Concreto: pasar de
  ESTADO 1.10 a 1.30 (esfuerzo real de superhabit sostenido) sube **solo 25 puntos** (950→975). El que
  ya está arriba y se rompe el lomo apenas ve mover la aguja → **riesgo motivacional alto en la zona que
  el producto quiere premiar.** Es el precio explícito de la idea del dueño.
- **Riesgo emocional del piso 0.** Una mala semana puede mostrar 80–300 puntos. Contradice el "no
  humillar" del `arbol §3.2`. Se delega al texto acompañante, pero el número crudo bajo es el flanco más
  débil de este enfoque frente a OPUS C (centrado en experiencia).
- **Quiebre de pendiente en 0.40.** La aguja pasa de +87.5 a +22.7 por 0.05 al cruzar a Atención. Es
  continuo pero perceptible: cruzar el primer corte se siente como "frenar". Inevitable si Restauración
  acapara 700 puntos.
- **Plenitud comprimida (2.00 pts/0.01).** Toda la zona "cumplí bien" (0.85→1.10) vive en apenas 50
  puntos (900→949). Mejorar dentro de Plenitud casi no se nota.
- **Fortalezas:** fiel 100% a la idea del dueño; cortes impecables; resuelve la regla dura (Inquebrantable
  visible); cero ambigüedad de implementación (5 ramas lineales). Es la opción más simple de explicar
  con la narrativa "salir del pozo se ve mucho".
- **Para el merge:** este enfoque gana si el producto prioriza el feedback de recuperación sobre el de
  excelencia. Pierde si el objetivo es premiar el esfuerzo en la zona alta (ahí conviene OPUS C). Una
  vía intermedia sería bajar el techo de Restauración (ej. 0→600) para liberar resolución arriba, pero
  eso ya **traiciona** la idea literal del dueño y es trabajo del merge, no de OPUS A.
```
