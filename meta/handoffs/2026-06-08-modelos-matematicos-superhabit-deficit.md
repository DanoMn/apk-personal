# Handoff — Modelos matemáticos de superhabit y déficit (+ soportes/tasks)

> **Estado: CONGELADO** (registro para la próxima sesión). Fecha: 2026-06-08.
> **Punto de entrada de la próxima sesión.** Fase: diseño del núcleo del scoring. NO se toca código.

## TL;DR — qué hacer en la próxima sesión

El esqueleto del motor de scoring está **cerrado y validado** (pesos puros, 44/45 marcas). Pero el mapa
está **incompleto a propósito**: faltan los **modelos matemáticos** del núcleo de cómo se suma y se resta
puntaje. La próxima sesión arranca a DEFINIRLOS (hoy "modelo matemático" es solo una idea, no está definido
qué significa). En orden:

1. **Modelo matemático de SUPERHABIT y DÉFICIT** (son la misma normalización, dos direcciones): normalizar
   **minutos y días** de cada ancla. Días pesan más (constancia > ráfaga de 1 día); un día cuenta proporcional
   al tiempo hecho vs el objetivo. Arriba del objetivo = superhabit (suma puntos); abajo = déficit (resta).
2. **Cómo el score interno se traduce al PUNTAJE numérico que ve el usuario** (puntos vs estados — capa entera sin definir).
3. **Tratamiento/agregación de SOPORTES (multi-soporte) y TASKS** — hoy soporte = ±0.1 simétrico light,
   tasks neutras, pero falta el modelo de cómo agregan (3/4/5 soportes) y de tasks.

## Punto de entrada conceptual

**`docs/scoring/mapa-modelo-scoring-v1.md`** — el mapa consolidado: qué está CERRADO (§3), los ERRORES a no
repetir (§5), y todos los GAPS admitidos (§6). Leerlo PRIMERO. Es la base de entendimiento; supersede a los
`modelo-consolidado-v1/v2.md` (que tenían errores).

## Lo que está CERRADO (no re-abrir — detalle en el mapa §3)

- Motor = SOLO pesos: `score = Σ peso×valor`. Cero reglas/caps/gates/worst-term. Todo emerge.
- Forma A (opt-in multiplica el peso de su capa + renormaliza). Capas normales iguales; anclas promedian.
- Ventana móvil de 7 días. Sobriedad en el motor = held/broke esta semana. **La racha larga es feature aparte.**
- Soporte ±0.1 simétrico (light). Higiene digital = actividad de Conducta, se marca desde Sueño.
- Inquebrantable NO es gate "≥2 capas" — debe EMERGER del modelo normalizado de superhabit + cobertura.
- La app premia CONSTANCIA (días) sobre tiempo de 1 día.

## Los problemas a resolver (lo que "modelo matemático" debe llegar a significar)

1. **Normalización ancla = f(días, tiempo).** Definir la función que toma (días hechos, minutos por sesión)
   y el objetivo (días meta, minutos meta) y devuelve un valor donde: días pesan más que minutos; bajo el
   objetivo resta proporcional (déficit), sobre el objetivo suma proporcional con rendimientos decrecientes
   (superhabit). Caso límite testigo: 3 días extra × 5 min con tarea de 40 min NO es superhabit pleno.
2. **Inquebrantable emergente.** Reemplazar el gate "≥2 capas" por: cuánto superhabit normalizado + sobre
   cuántas capas → cuándo emerge Inquebrantable, variando con N (3/4/5).
3. **Puntos visibles.** Cómo el score 0–1 y los estados se traducen al puntaje que ve el usuario, y dónde
   entran los "puntitos extra" de los soportes.
4. **Multi-soporte y tasks.** Modelo de agregación de varios soportes; rol de las tasks.
5. **Casos sin dato** (a marcar o decidir): sueño+sobriedad juntos, capa modulada sin actividades,
   multi-actividad de sobriedad, cobertura de Inquebrantable a 3/4 capas.

## Contexto / documentos necesarios (leer para arrancar)

- **Mapa:** `docs/scoring/mapa-modelo-scoring-v1.md` (entrada principal).
- **Marcas del dueño:** `docs/scoring/dataset-decisiones-estado-v1.md` + `scripts/scoring/weight_model_fit_v2.py` (`CASES`).
- **Exploraciones opus:** `docs/scoring/modelo-propuesto-{A,B,C}.md`, `modelo-complemento-{A,B,C}.md`.
- **Filosofía soporte:** `docs/dominio/definicion-reestructuracion-soporte.md`.
- **Contrato matemático histórico:** `docs/scoring/arbol-scoring-v1.md`, `plan-tecnico-scoring.md` (revisar, parte es legacy).
- **Datos de ancla (días/minutos/metas):** `docs/datos-room/actividades-ancla-predeterminadas-v1.md`,
  `docs/datos-room/preset-soportes-v1.md`.
- **Memoria engram (`apk-personal`):** `scoring/filosofia-pesos-puros`, `scoring/correcciones-cerradas`,
  `scoring/refinamientos-soporte-superhabit-higiene`, `scoring/dimensiones-faltantes`.

## Método que viene funcionando (no cambiarlo)

Comportamiento antes que fórmula. El dueño define la conducta esperada; el agente NO inventa reglas ni
pide ratificar comportamientos (los calcula el motor). Para diseñar: subagentes opus independientes a
ciegas proponen modelos completos desde un brief común (plan en `meta/instructions/`), el orquestador
mergea. SIEMPRE planificar (MD en `meta/instructions/`) antes de lanzar o codear. Guardar decisiones en
engram al toque (el dueño notó que el agente pierde memoria entre iteraciones).

## Salida esperada de la próxima sesión

1. La función de normalización ancla (días × tiempo) definida, que cubre superhabit Y déficit.
2. Inquebrantable emergente (sin gate duro).
3. Definición de cómo se traduce a puntos visibles.
4. Modelo de multi-soporte y tasks.
5. Recién con eso: el mapa pasa de "incompleto" a modelo completo, y se evalúa la implementación.
