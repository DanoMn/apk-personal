# Handoff — Motor de VALOR DE CAPA + OPT-INS (sueño/sobriedad)

> **Congelado** (foto de la sesión 2026-06-12). No se edita. Si la realidad cambia, se escribe otro.
> Objetivo de la sesión: definir el modelo matemático del **valor de una capa** y cerrar el
> **motor de opt-ins** (sueño en Cuerpo, sobriedad en Conducta), partiendo del motor del ancla ya
> consolidado. **Resultado: motor de opt-ins CERRADO y verificado.** Falta soportes y tasks.

---

## 1. Estado al cerrar (qué quedó hecho)

- **Valor de capa — CERRADO.** Dos canales (base + extra), eje corregido, pesos de capa iguales.
- **Motor de opt-ins (sueño + sobriedad) — CERRADO y verificado** (modelo v4).
- **Contrato de axiomas O1–O13** escrito (`docs/scoring/axiomas-opt-in-v1.md`).
- **Docs oficiales vivos actualizados** (mapa, árbol, plan técnico) + auditoría de la sesión.
- **PENDIENTE:** soportes y tasks (cómo entran al valor de capa); reconciliar estabilidad temporal
  multi-semana con v4; calibración fina; implementación en código.

## 2. El modelo final (v4) — resumen ejecutivo

**Escala:** tres niveles distintos —
- Ancla: `R ∈ [0, 1.5]` (fórmula consolidada, ver merge del ancla). `R=1` = cumplió justo.
- Capa: `valor_capa = min(base,1) + extra` ∈ [0, ~1.5].
- Estado global: `ESTADO = min(base_global,1) + extra_global` ∈ [0, ~1.15]. **De acá salen las bandas.**

**Bandas:** `R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10` (I = 1+δ, δ=0.10).
**Eje:** cumplir todo justo = **1.0 = Plenitud**. Superhabit repartido = Inquebrantable. Parcial =
En marcha/Atención/Restauración. **(NO existe EM_TOP — fue un error anulado, ver §4.)**

**Dos canales por capa:**
- `base_capa = promedio min(R_i,1)` de las anclas (¿está en pie?, ≤1).
- `extra_capa = promedio max(R_i−1,0)` de las anclas (superhabit, SOLO anclas).

**Opt-in = término-sombra de peso dinámico en el canal base:**
- `w = BETA·N·(1−M)`, con `w(M=1)=0` (invisible cuando está bien). `N` = nº capas activas, `BETA=0.818`.
- Señal sueño `M`: continua [0,1] (4 comp por noche; sin dato → `B_SLEEP=0.5`).
- Señal sobriedad `M`: `(1−A)^(días de recaída)`, `A=0.55`; multi-track = producto por track.

**Agregación:**
- `base_global = Σ(valor·peso)/Σ(peso)` sobre términos: {(base_capa, W0=1) por capa} + {(M, w) por
  opt-in en capa con anclas}; capa solo-opt-in = término (M, W0).
- `extra_global = promedio de extra_capa sobre capas con anclas` (**pesos iguales** → Sol=Tin).
- `ESTADO = min(base_global,1) + extra_global`.

**Verificado:** `modelo_valor_capa_v4_merge.py` — C1–C6 + D8 + anti-gate (paso máx 0.0007) todos verdes.

## 3. Decisiones CERRADAS (no re-discutir)

ESTRUCTURA:
1. Dos canales: base (¿en pie?) + extra (superhabit, SOLO anclas; opt-in nunca da extra).
2. valor de capa en escala del ancla [0,1.5]; cumplir justo = 1.0 = Plenitud.
3. score = promedio de valores de capa, **pesos de capa IGUALES (1/N)**.
4. Las anclas promedian a un bloque; nº de anclas no cambia el peso de capa.
5. extra agregado con pesos iguales → mismo superhabit rinde igual en cualquier capa (Sol=Tin).

OPT-INS (los 13 axiomas O1–O13 en `docs/scoring/axiomas-opt-in-v1.md`):
6. Opt-in = término-sombra de peso dinámico `w=BETA·N·(1−M)`, `w(M=1)=0` (neutro exacto, incluso con
   anclas en déficit).
7. COMPONER dos opt-ins malos (suman arrastre), SIN tope.
8. MISMO BETA para sueño y sobriedad (decisión humana/compasiva — no castigar más al sensible).
9. BETA=0.818 (de TARGET=0.55: opt-in en piso + anclas perfectas → Atención).
10. Sueño continuo (sin dato → B_SLEEP=0.5). Sobriedad `(1−A)^días`, A=0.55, multi-track producto.
11. Capa solo-opt-in (sin anclas) = el opt-in ES la capa (valor=M, no exporta extra).
12. Anti-incentivo del opt-in (solo empata o baja, nunca sube) = ACEPTADO (es opt-in por diseño).

COMPORTAMIENTO ACEPTADO (rarezas que el dueño dio por OK, NO son bugs):
13. Superhabit fuerte en una capa COMPENSA déficit en otra (efecto del promedio).
14. Capa muerta NO colapsa el estado (sigue el promedio; no hay worst-term).
15. Agregar una capa y cumplirla justo puede BAJAR de banda (extra promediado).

METODOLOGÍA:
16. Axiomas primero; NO heredar magnitudes de docs viejos. El dueño define comportamiento → el modelo
    hace emerger los valores. "área" = "capa" (no usar "área").

## 4. Errores cometidos y corregidos (NO repetir)

- ❌ **EM_TOP=0.85**: introduje un factor que comprimía la escala y hacía "cumplir justo = En marcha".
  CONTRADECÍA el ancla (mapa §1: P≥0.85; simulación de capas: EXACTO=1.0=Pleno). El dueño lo cazó
  mandándome a leer los tests del ancla. Corregido: cumplir justo = 1.0 = Plenitud. **Lección: nunca
  introducir factores de escala que contradigan el modelo del ancla; verificar contra mapa §1 y la
  simulación de capas antes de fijar eje/bandas.**
- ❌ **Opt-in inflando el peso de capa (Forma A, k_sleep/k_sobr):** distorsionaba el superhabit
  (un superhabit en la capa pesada valía más). Reemplazado por pesos iguales + sombra dinámica.
- ❌ Asumir magnitudes de docs "cerrados" como verdad (el dueño me frenó al inicio).

## 5. Cómo se llegó (trayectoria)

1. Partida: ancla consolidada; gap = valor de capa; prioridad = opt-ins.
2. Definiciones del dueño D1–D5.
3. Corrección de método (axiomas primero).
4. Salto: dos canales (base + extra) → el mal sueño no borra el mérito.
5. Eje (con el error EM_TOP en el medio, corregido).
6. Pesos de capa: de inflar (distorsionaba superhabit) a iguales.
7. Trilema: arrastrar el opt-in sin matar anclas ni distorsionar superhabit.
8. Sesión multi-agente (3 proponentes A/B/C) + merge → término-sombra dinámico con w(M=1)=0.
9. Cierre: componer, mismo BETA, señal de sobriedad por días (A=0.55).
10. Volcado a docs oficiales + auditoría (3 subagentes) + contrato de axiomas O1–O13.

## 6. Mapa de archivos de la sesión

OFICIALES VIVOS (actualizados):
- `docs/scoring/mapa-modelo-scoring-v1.md` — mapa del modelo (§1, §3.15–28, §5, §6 actualizados).
- `docs/scoring/arbol-scoring-v1.md` — fórmulas canónicas (§6/§11/§12/§16-NUEVO; worst-term/gates marcados OBSOLETO v4).
- `docs/scoring/plan-tecnico-scoring.md` — estado de fases.
- `docs/scoring/axiomas-opt-in-v1.md` — **contrato de axiomas O1–O13** (canónico).

EXPLORACIÓN (registro del proceso):
- `docs/scoring/exploracion-valor-capa/merge-arrastre-optin-consolidado.md` — modelo v4 final + §8 decisiones.
- `docs/scoring/exploracion-valor-capa/modelo_valor_capa_v4_merge.py` — script reproducible.
- `docs/scoring/exploracion-valor-capa/modelo-valor-capa-consolidado-v1.md` — consolidado previo (con §0-ERRATA del EM_TOP).
- `docs/scoring/exploracion-valor-capa/definiciones-dueno-v1.md` — definiciones del dueño.
- `docs/scoring/exploracion-valor-capa/problema-arrastre-optin-v1.md` — planteamiento del trilema.
- `docs/scoring/exploracion-valor-capa/solucion-{A,B,C}-*.md` — las 3 propuestas a ciegas.
- `docs/scoring/exploracion-valor-capa/auditoria-decisiones-sesion-2026-06-12.md` — auditoría (33 decisiones).
- ancla: `docs/scoring/exploracion-rendimiento-ancla/merge-consolidado.md`.

ENGRAM (project `apk-personal`): topic `scoring/modelo-valor-capa` (todo el detalle).

## 7. PENDIENTE para próximas sesiones

1. **SOPORTES y TASKS** — cómo entran al valor de capa. (Ver prompt de arranque:
   `meta/instructions/2026-06-12-planificar-soportes-tasks.md`.) Lo que se sabe: anclas > soportes >
   tasks; soportes = mantenimiento sin targets (UX inversa); tasks = puntuales con capa, aportan poco,
   las neutras no suman. Las magnitudes viejas (±0.1 soporte, 0.05 task) ESTÁN DESCARTADAS — definir
   por axiomas como se hizo con opt-ins.
2. **Reconciliar estabilidad temporal multi-semana** (`arbol §15`, "5 semanas de historial") con v4 —
   v4 hace Inquebrantable emergente; falta decidir si conserva la memoria temporal. Zona gris abierta.
3. **Calibración fina** de A, BETA, bandas contra marcas del dueño.
4. **Traducción score → puntos visibles** (el usuario ve puntos, no el [0,1.5] interno).
5. **Implementar v4 en código** (`domain/scoring/` sigue siendo el modelo VIEJO = deuda).
