# Set-prompt — 3 subagentes Opus para SOPORTES y TASKS en el scoring

> **Qué es esto:** el prompt-base que el orquestador inyecta a los 3 subagentes Opus que modelan,
> de forma divergente, cómo entran **SOPORTES** y **TASKS** al motor de scoring. Cada subagente
> recibe ESTE núcleo compartido + su **sesgo propio** (§7). Luego el orquestador hace el merge.
> Sigue el protocolo de meta-prompting de `AGENTS.md` y el método axiomas-primero del proyecto.
> Fecha: 2026-06-15. Proyecto Engram: `apk-personal`.

---

## 1. Objetivo

Definir cómo **SOPORTES** y **TASKS** entran al scoring, completando la última pieza del valor de
capa. El ancla, el valor de capa y los opt-ins (sueño/sobriedad) ya están **CERRADOS**. Cada
subagente entrega **un sistema coherente y completo** para soportes + tasks; el orquestador hará
merge tomando lo mejor.

## 2. Decisiones del dueño que enmarcan TODO (no se re-discuten)

- **Orden de importancia (axioma duro):** `ANCLAS > SOPORTES > TASKS`.
- **SOPORTES**: actividad **diaria** (análoga a sobriedad en cadencia). Influye en el valor de una
  capa. **PUEDE restar, pero MUY levemente** (descuidar un soporte baja apenas). Sin targets. UX
  inversa (el usuario marca lo que NO hizo) — eso es presentación; la lógica interna es polaridad
  normal (más sostenido = mejor).
- **TASKS**: pendiente **puntual**, una sola vez, sin recurrencia. Tiene capa asignada. **NUNCA
  resta** (una task no hecha no penaliza; solo suma la hecha). Aporta **menos que un soporte**.
- **Tasks como "ayuda mental":** idea del dueño — las tasks sirven para que un usuario avanzado que
  se esforzó pueda **arañar unos puntos** y cruzar un umbral (ej. de un estado al siguiente), como
  un empujón motivacional, **sin que sea injusto** (no se "compra" un estado regalado).

## 3. Hallazgo crítico sobre los PUNTOS (leer con cuidado — el dueño no lo recordaba)

El modelo actual tiene **dos ejes separados** que el ejemplo "897 → Inquebrantable en 900" mezcla:

1. **Número visible 700–1000** (`docs/scoring/arbol-scoring-v1.md` §3.2):
   `VisibleScore = 700 + round(clamp(WeeklyBaseScore, 0, 1) · 300)`.
   Solo refleja la **base**, recortada en 1.0 → **topa en 1000**. El superhabit/extra NO aparece.
   Esta fórmula es **pre-v4** (usa el `WeeklyBaseScore` viejo, no el `ESTADO = base+extra` nuevo) →
   **el mapeo visible bajo v4 está sin cerrar.**
2. **Estado / banda** (§16-NUEVO, cerrado 2026-06-12):
   `ESTADO = min(base,1) + extra`, escala [0, ~1.5].
   `Restauración <0.40 · Atención <0.62 · En marcha <0.85 · Plenitud <1.10 · Inquebrantable ≥1.10`.
   Inquebrantable vive en otro eje: necesita **extra** (superhabit), que hoy **solo lo dan las anclas**
   (axioma O1). El número visible 700-1000 **no codifica** el rango de Inquebrantable.

**Conclusión:** la idea de tasks aterriza en esa zona abierta (mapeo visible bajo v4). Cada subagente
debe **reconciliar explícitamente** ambos ejes, no implementar a ciegas el ejemplo del dueño.

## 4. Lo que YA está cerrado (marco que soportes/tasks DEBEN respetar)

- **Valor de capa** = `min(base,1) + extra` (escala del ancla [0,1.5]). `base`=¿está en pie? (≤1),
  `extra`=superhabit (≥0, **solo de anclas** hasta ahora).
- **Score global** = promedio de valores de capa, **pesos de capa iguales (1/N)**.
- **Opt-ins** (sueño/sobriedad) = término-sombra de peso dinámico `w=BETA·N·(1−M)` en la base;
  contrato `docs/scoring/axiomas-opt-in-v1.md` (O1–O13). `BETA=0.818`, `A=0.55`, `B_SLEEP=0.5`,
  `δ=0.10`. **NO TOCAR.**
- Motor de pesos puros: **cero gates/caps/worst-term/min duro**. Continuo y diferenciable.

## 5. Lo que se sabe de dominio (verificar en fuentes)

- **Soportes**: `docs/dominio/definicion-reestructuracion-soporte.md`,
  `docs/producto/nucleo-dominio-autonomia.md`. Mantenimiento diario (agua, higiene, orden mínimo).
  Sin targets. Capa obligatoria. No obligatorios para el sistema.
- **Tasks**: `nucleo-dominio-autonomia.md` §Task. Puntuales, una vez, con capa. Una task **neutral**
  (sin capa / rol Neutral) **no suma**.
- ⚠️ **MAGNITUDES VIEJAS DESCARTADAS:** el ±0.1 del soporte y el 0.05 de la task son del modelo viejo
  y **NO se heredan**. Definir por axiomas.

## 6. Restricciones duras (romper una = propuesta inválida)

1. No tocar ancla (A1–A10) ni opt-ins (O1–O13).
2. Soportes/tasks **NO deben distorsionar el superhabit** (caso `Sol = Tin` debe seguir igual).
3. Mantener **cumplir-justo = ESTADO 1.0 = Plenitud** y el resto del eje semántico.
4. **Sin gates/caps/worst-term.** Continuo y diferenciable.
5. Respetar `ANCLAS > SOPORTES > TASKS` (magnitud y peso).
6. Soportes restan **muy levemente**; tasks **nunca** restan.

## 7. Sesgos divergentes (uno por subagente)

Cada subagente trata SOPORTES **y** TASKS, pero con una filosofía propia. El eje de divergencia de
TASKS lo fijó el dueño:

- **Subagente A — "puristas del motor / tasks cosméticas".**
  Soportes: aporte **light, conservador**, al canal **base**, saturado y **centrado** (sostener suma
  poco, descuidar resta poquísimo). Tasks: **NO tocan el motor** (estado/banda intactos); dan un
  **delta acotado solo al número visible 700-1000** ("ayuda mental" sin injusticia). Debe resolver
  cómo conviven el visible y el estado sin contradicción.
- **Subagente B — "todo unificado en el motor".**
  Soportes: señal tipo opt-in (análoga a sobriedad), **término-sombra propio de peso dinámico
  pequeño** (puede restar levemente, simétrico atenuado). Tasks: **aporte real al valor de capa**,
  **solo hacia arriba**, magnitud `< soporte`, con **saturación** para que multi-task no fabrique
  banda; puede cruzar un umbral en el tramo final.
- **Subagente C — "repensar la capa de presentación / mapeo visible v4".**
  Soportes: enfoque **blend** (estilo `γ` de los moduladores) sobre el valor de capa. Tasks:
  **rediseña el mapeo visible bajo v4** para que el número 700-1000 codifique el estado real
  (base+extra) y las tasks **empujen el tramo final** del visible sin alterar la banda injustamente.

## 8. Contrato de entrega (cada subagente DEBE producir)

Escribir un MD en `docs/scoring/exploracion-soportes-tasks/subagente-{A|B|C}-propuesta.md` con:

1. **Filosofía** (1 párrafo): el sesgo y por qué es coherente.
2. **Axiomas de SOPORTES** `S1…Sn`: comportamiento (canal base/extra, señal/medida, bidireccional o
   no, agregación multi-soporte con saturación, peso vs anclas). Estilo O1–O13.
3. **Axiomas de TASKS** `T1…Tn`: canal/eje (visible vs estado), magnitud `<soporte`, agregación
   multi-task, por qué nunca resta, cómo encaja la "ayuda mental" sin injusticia.
4. **Fórmulas** explícitas y **parámetros despejados de axiomas de estado** (no a dedo).
5. **Verificación numérica con `python3`**: casos límite + tabla antes/después. Probar:
   (a) `Sol = Tin` sigue (superhabit intacto), (b) cumplir-justo = ESTADO 1.0, (c) multi-soporte no
   fabrica banda, (d) anti-gate (continuidad, sin saltos), (e) anclas>soportes>tasks se cumple.
   Incluir el snippet/script y sus resultados en el MD.
6. **Reconciliación de los dos ejes** (§3): qué pasa con el número visible y con el estado.
7. **Riesgos / lo que queda abierto.**

Devolver al orquestador un **resumen conciso** (≤25 líneas): ruta del archivo, axiomas clave,
fórmula central, qué resultados de verificación pasaron, y en qué se diferencia de los otros enfoques.
NO volcar todo el contenido en el mensaje de retorno (el merge lee los archivos).

## 9. Método y reglas operativas

- **Axiomas primero, no heredar magnitudes.** Las magnitudes se despejan de axiomas de estado.
- Apoyarse en MCP **Context7** solo si hace falta (no es código de app; es modelado + python).
- **NO es código de la app** → no aplica Strict TDD de Kotlin; la verificación es `python3`.
- Idioma: doc en español, identificadores/fórmulas en inglés cuando aplique.
- Si hay un descubrimiento grande, guardarlo en Engram (`project: apk-personal`).

## 10. Referencias canónicas (leer las que apliquen)

- Marco cerrado: `docs/scoring/axiomas-opt-in-v1.md` (O1–O13), `docs/scoring/arbol-scoring-v1.md`
  (§3.2 visible, §16-NUEVO bandas), `docs/scoring/modelo-consolidado-v2.md`.
- Pro-prompt previo: `meta/instructions/2026-06-12-planificar-soportes-tasks.md`.
- Modelo v4: `docs/scoring/exploracion-valor-capa/merge-arrastre-optin-consolidado.md` +
  `modelo_valor_capa_v4_merge.py`.
- Dominio: `docs/dominio/definicion-reestructuracion-soporte.md`,
  `docs/producto/nucleo-dominio-autonomia.md`, `docs/scoring/historias-soportes-tasks-v1.md`.
