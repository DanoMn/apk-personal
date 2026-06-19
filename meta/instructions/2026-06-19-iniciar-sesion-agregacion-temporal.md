# Prompt de arranque — Sesión: Agregación temporal del scoring (Camino A vs B)

> Pegá el bloque de abajo para iniciar la sesión. Es de **exploración y decisión**, NO de implementación.

---

Sesión: decidir el **modelo de agregación temporal** del scoring de "Autonomía sin límites"
(proyecto `apk-personal`, Android Kotlin local-first). NO implementás nada: es exploración para
que el dueño decida con números a la vista.

## Leé PRIMERO (en este orden)
1. `meta/handoffs/2026-06-18-agregacion-temporal-scoring.md` — el handoff, **autocontenido**: el
   problema, la tensión semanal vs diario, Camino A vs B con tradeoffs, los 3 gaps acoplados
   (config retroactiva, recaídas asumidas en pausa, gracia de soporte), la conexión con el
   diagnóstico viejo #858, y los seams de código verificados (§7).
2. Memoria engram (project `apk-personal`): #858 (diagnóstico reinicio lunes), topics
   `scoring/gaps-manejo-operaciones-ciclo-vida` (#1200), `scoring/decisiones-ciclo-vida-y-doc-oficial`
   (#1194). Usá `mem_search` + `mem_get_observation`.
3. Contrato vigente: `docs/scoring/modelo-scoring-oficial-v1.md` + `modelo-matematico-nucleo-v1.md`.

## La decisión
- **Camino A** — versionar la config por fecha; el motor sigue siendo semanal pero usa el target
  vigente de cada día (resuelve la retroactividad, riesgo bajo-medio).
- **Camino B** — score diario congelado + promedio de 7 días (lo que describe el dueño; es un motor
  nuevo, riesgo alto, enmienda el contrato matemático).

## Método (del §8 del handoff) — sin código
1. **Calculá A MANO**, con ejemplos numéricos concretos, cómo queda el estado en A vs B en los dos
   escenarios que le molestan al dueño:
   - Editar el target de un ancla a mitad de semana (los días ya cumplidos no deben recalcularse).
   - Lunes nuevo vs fin de semana bueno (el reinicio del diagnóstico #858).
2. Evaluá el **tamaño real** de Camino A (qué esquema Room nuevo, qué entidades, cuántos tests a tocar).
3. Decidí si Camino B es viable **en esta fase** (rama `feat/scoring-motor-nucleo-v1` aún sin merge,
   motor núcleo recién cerrado → riesgo alto). Hacé el trade-off explícito.
4. Evaluá si el **Gap B** (recaídas asumidas durante pausa de un track) puede ser un fix separado
   más chico que no espere la decisión A/B.

## Reglas
- Verificá los seams en el código real antes de afirmar (el handoff §7 los lista). Nunca de memoria.
- Presentá los números para que el dueño "marque con la panza"; NO arranques SDD sin su decisión.
- Los hechos NO se borran (decisión firme). Anti-trampa: la ventana de 7 días pesa al reactivar.

## Estado del repo al arrancar
El SDD de "hardening de operaciones de ciclo de vida" YA está cerrado y commiteado en
`feat/scoring-motor-nucleo-v1` (commits `d916011` candado/Lote 1, `e309f2d` higiene/Lote 2), sin push.
Los 3 gaps acoplados (A/B/C del §5 del handoff) siguen pendientes — son ESTA sesión.
