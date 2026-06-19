# Set-prompt — mapeo del ESTADO a PUNTOS VISIBLES (panel de 3 Opus)

> Plan del orquestador para que 3 Opus diseñen, de forma divergente, cómo el ESTADO del motor de
> scoring se traduce al número de puntos que ve el usuario en el dashboard. Cada Opus recibe este
> núcleo + su sesgo (§5). Luego el orquestador hace merge. Fecha: 2026-06-16. Proyecto: `apk-personal`.

## 1. El problema

El motor entrega un **ESTADO ∈ [0, 1.5]** (continuo). El dashboard debe mostrar un **número de puntos**.
Hay que definir la **función ESTADO → puntos**. El reto: son dos escalas distintas ([0,1.5] vs el rango
de puntos) y los **cortes de banda deben caer en números "lindos"** (redondos, memorables), porque el
usuario los va a ver y asociar a su estado.

## 2. El input exacto (lo cerrado, no se toca)

```
ESTADO = base_global + extra_global   ∈ [0, 1.5]   (base [0,1] = ¿cumpliste?; extra [0,0.5] = superhabit)
Cortes de banda (CERRADOS):
  Restauración:    ESTADO < 0.40
  Atención:        0.40 ≤ ESTADO < 0.62
  En marcha:       0.62 ≤ ESTADO < 0.85
  Plenitud:        0.85 ≤ ESTADO < 1.10   (cumplir-justo = 1.0 cae acá, en zona alta)
  Inquebrantable:  ESTADO ≥ 1.10   (tope práctico del ESTADO ≈ 1.5)
```

**Regla dura: usar el ESTADO COMPLETO (0 a 1.5).** El mapeo viejo (`700+base·300`) usaba solo la base
y saturaba en 1000 al llegar a Plenitud → TODO Inquebrantable (1.10–1.5) quedaba invisible. **Eso NO se
repite:** Inquebrantable tiene que tener su propio rango visible.

## 3. La idea base del dueño (punto de partida, NO obligatoria)

- El tramo de puntos **0 → 700** cubre el ESTADO **0 → 0.40** (Restauración). Es el "gap más grande" de
  puntos. Es decir: Restauración ocupa mucho rango visible; de 0.40 a 1.5 se reparte el resto hasta 1000.
- **Libertad de modificar números:** se pueden mover los puntos de cada corte, e incluso proponer
  ajustar los cortes de ESTADO (0.40/0.62/0.85/1.10) si con eso los números visibles quedan más limpios
  — siempre que se justifique y no rompa el eje semántico (cumplir-justo=1.0=Plenitud, etc.).

## 4. Tensiones a resolver (cada Opus debe tomar postura)

1. **¿Piso 0 o piso ~700?** El doc viejo (`arbol §3.2`) decía "no mostrar valores humillantes bajo
   700" → piso 700. La idea del dueño (0→700 para Restauración) **revierte eso**: mostraría números
   bajos (hasta 0) en estados malos. ¿Se honra el "no humillar" o se acepta mostrar bajo? Tomar postura.
2. **¿Dónde va la resolución?** Si Restauración (0-0.40) se lleva 0-700 puntos, queda poco rango
   (700-1000) para todo lo bueno (0.40-1.5) → el usuario que se esfuerza arriba ve poco movimiento.
   ¿Es deseable o un problema motivacional? Analizar el trade-off con números.
3. **Números lindos:** los cortes de banda deben caer en puntos memorables (ej. múltiplos de 50/100).
4. **Continuidad:** la función debe ser continua (sin saltos bruscos de puntos al cruzar un corte),
   aunque puede ser lineal a tramos.

## 5. Sesgos divergentes (uno por Opus)

- **OPUS A — "fiel a la idea del dueño".** Implementá literal 0-0.40→0-700, y repartí 700-1000 entre
  Atención/En marcha/Plenitud/Inquebrantable con cortes en números lindos. Mostrá el costo (poca
  resolución arriba) honestamente.
- **OPUS B — "lineal limpio + ajustar cortes".** Mapeo lo más simple posible (idealmente lineal global
  o casi). Si hace falta, **proponé mover los cortes de ESTADO** a valores que den puntos redondos
  (ej. que las bandas caigan en 600/750/850/950). Prioridad: simplicidad y números memorables.
- **OPUS C — "centrado en la experiencia / motivación".** Diseñá el mapeo desde el tono del producto
  (no humillar, premiar el esfuerzo arriba). Decidí piso y resolución para que el número se sienta justo
  y motivador. Podés usar curvas (no solo lineal a tramos) si mejora la experiencia, justificando.

## 6. Contrato de entrega (cada Opus DEBE producir)

Escribir `docs/scoring/exploracion-puntos-visibles/opus-{A|B|C}-mapeo.md` con:
1. **Filosofía** del enfoque (1 párrafo).
2. **La función ESTADO → puntos** explícita (fórmula, lineal a tramos o curva).
3. **Tabla de hitos**: para cada corte de banda (0, 0.40, 0.62, 0.85, 1.0, 1.10, 1.5) → qué número de
   puntos da. Y el rango de puntos de cada banda.
4. **Postura sobre las 4 tensiones** (§4): piso, resolución, números lindos, continuidad.
5. **Verificación con `python3`**: tabla ESTADO→puntos en pasos de 0.05, mostrando los cortes; incluí el
   script y la salida. Comprobá que es monótona y continua.
6. **Trade-offs / riesgos** del enfoque.

Devolución al orquestador: resumen ≤25 líneas (ruta, función central, tabla de hitos, postura, en qué
diverge). NO vuelques todo.

## 7. Referencias
- `docs/scoring/arbol-scoring-v1.md` §3 (mapeo viejo, deuda) y §16-NUEVO (bandas, ESTADO).
- `docs/scoring/exploracion-soportes-tasks/modelo-consolidado-v3-pesos-variables.md` (de dónde sale el ESTADO).
- Tono del producto: `docs/producto/tono-comunicacion.md`, `AGENTS.md` (tono obligatorio, no humillar).
