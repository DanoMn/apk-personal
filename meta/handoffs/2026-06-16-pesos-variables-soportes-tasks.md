# Handoff — Pesos variables de capa + soportes + tasks (2026-06-16)

> Registro congelado de la sesión. El contrato de diseño resultante vive en
> `docs/scoring/exploracion-soportes-tasks/modelo-consolidado-v3-pesos-variables.md`.

## Objetivo de la sesión
Definir cómo entran SOPORTES y TASKS al scoring, y cómo se ponderan las capas. Se hizo con paneles de
3 sub-agentes Opus (planes divergentes) + merge del orquestador, todo verificado con `python3`.

## Recorrido (de dónde venía a dónde llegó)
1. **Primer intento (merge v1)**: modelo EQUIVOCADO — soportes/tasks como sombra global + tasks en el
   número visible 700-1000. El dueño lo corrigió. → `merge-soportes-tasks-consolidado.md` (marcado OBSOLETO).
2. **Segundo intento (merge v2)**: Forma A (dentro del valor de capa), soportes blend + tasks saturación
   conjunta. Correcto en soportes/tasks, pero el **peso de capa tenía bug** (crecía con la cantidad de
   anclas de forma continua/incorrecta). → `merge-v2-consolidado.md` (válido salvo peso de capa).
3. **Esta sesión (v3)**: se corrigió el peso de capa y se cerraron las decisiones grandes de pesos,
   superhabit y dilución. → `modelo-consolidado-v3-pesos-variables.md`.

## Decisiones cerradas por el dueño
- **Forma A**: soportes/tasks DENTRO del valor de cada capa.
- **Peso de capa variable por anclas**: más anclas = más peso (capa más importante), con freno
  (rendimiento decreciente, techo natural). Reemplaza "todas pesan 1/N".
- **Capa solo-soportes** existe y pesa menos (ρ≈0.35); se normaliza al agregar 1 ancla.
- **Superhabit Opción 2**: base ponderada por peso; superhabit plano entre capas (mantiene `Sol=Tin`).
- **Forma 1**: superhabit/déficit de una ancla se promedia dentro de la capa (brillar/fallar en 1 de
  varias se diluye; la capa brilla cuando brillás en todas).
- **Soportes**: blend en la base `(1−0.07)·base_anc + 0.07·G_s`, señal `min(días/4,1)`, bloque promedio.
- **Tasks**: extra efímero diario, saturación conjunta, techo ≈0.06/capa, gate base², se resetea diario.

## Aclaraciones importantes (malentendidos resueltos)
- **Cumplir 100% ≠ superhabit.** 100% = base 1.0 = Plenitud. Superhabit = PASARSE de la meta.
- **El superhabit no vive en la base.** Hay dos canales: BASE (¿cumpliste? incluye opt-ins) y EXTRA
  (¿te pasaste? = superhabit + tasks). Sueño/sobriedad están en la base; el superhabit en el extra.
- **El ancla maneja superhabit de TIEMPO (`St`) y de DÍAS (`Sd`) por separado** y los combina en `S`.
  Hacer más días suma (Sd); pasarse en minutos suma (St); NO completar los días pactados es otra cosa
  (frecuencia incompleta → base<1, el exceso compensa pero no genera extra).
- **El peso de capa NO lo tocan los soportes.** El peso lo decide cuántas anclas hay; el soporte mueve
  el VALOR de su capa, no el peso.

## Cerrado al final de la sesión
- **`r=0.5` CONFIRMADO** (decrecimiento del peso por ancla): cada ancla nueva vale la mitad → techo de
  capa 2.0 → ninguna capa decide más del 50% del score (límite de la filosofía de la mesa). Las anclas
  dentro de la capa pesan igual (el `r` es del peso de la capa, no de las anclas individuales).

## Pendiente (números finos a calibrar)
`ρ`=0.35 (peso solo-soportes), `TAU`≈0.06 (techo task), `WS`=0.07 (blend soporte), y la semántica de
capa solo-soportes descuidada (vale 0 o piso). Calibrar con marcas del dueño en
`historias-soportes-tasks-v1.md`, luego congelar `axiomas-soportes-tasks-v1.md`.

## Hallazgo de calibración (2026-06-16)
Calibrar `WS`, `ρ` y la semántica de capa solo-soportes con **marcas de ESTADO no funciona**: su
efecto (0.02–0.10) es más chico que el ancho de una banda (~0.23), así que la banda no se mueve aunque
cambie el número (verificado en `calibracion_diseno.py`). Solo el **anti-abuso de `TAU`** tiene marca
útil (cumplir-justo + tasks masivas: ¿llega a Inquebrantable? con `TAU<0.10` no). **DECISIÓN del dueño:
posponer la calibración fina hasta definir los PUNTOS 700-1000** (más granulares que las bandas → ahí sí
discriminan), especialmente para calibrar tasks. (Nota: el "100 tasks" de las pruebas es estrés teórico,
no caso real — un usuario hace 1–5 tasks/día; el techo `TAU` satura enseguida.)

## Próximo paso
1. ~~Definir el mapeo a PUNTOS~~ **HECHO (2026-06-16): mapeo = enfoque E** (hitos-meta perseguibles,
   suma de rampas logísticas). Piso 650, tope 1100; el 1000 se gana al entrar a Inquebrantable.
   Hitos en los cortes: 0→650 · 0.40→721 · 0.62→788 · 0.85→873 · 1.0→941 · 1.10→1011 · 1.5→1100.
   Detalle: `docs/scoring/exploracion-puntos-visibles/opus-E-mapeo.md` + `merge-puntos-visibles.md`.
2. Calibrar tasks (`TAU`) y los finos (`WS`, `ρ`, semántica solo-sop) **sobre los puntos E** (ya hay resolución fina).
3. Congelar `docs/scoring/axiomas-soportes-tasks-v1.md` (contrato estilo O1–O13).
4. Recién entonces tocar código (motor de scoring; NO esquema Room) y actualizar docs vivos (§8 del v3).

## Artefactos de la sesión
- `meta/instructions/2026-06-16-set-prompt-soportes-tasks-v2.md` — set-prompt de los 3 Opus.
- `docs/scoring/exploracion-soportes-tasks/subagente-{A,B,C}-plan-v2.md` — los 3 planes Opus.
- `docs/scoring/exploracion-soportes-tasks/modelo-consolidado-v3-pesos-variables.md` — el contrato v3.
- Scripts: `merge_v2_verificacion.py`, `consolidado_explora.py`, `casos_limite_gen.py`.
