# Prompt de arranque — Sesión: Agregación temporal del scoring (EXPLORACIÓN abierta)

> Pegá el bloque de abajo para iniciar la sesión. Es de **exploración de ideas**, NO de
> implementación ni de elegir entre opciones predefinidas.

---

Sesión: **EXPLORAR ideas** sobre cómo el scoring de "Autonomía sin límites" (proyecto `apk-personal`,
Android Kotlin local-first) debería agregar en el TIEMPO. NO implementás nada y NO hay caminos
predefinidos para elegir: la sesión abre el espacio de ideas desde cero.

## Leé PRIMERO (en este orden)
1. `meta/handoffs/2026-06-18-agregacion-temporal-scoring.md` — el handoff, **autocontenido**: el
   problema (§2), por qué NO es trivial / tensión semanal vs diario (§3), la intuición de partida
   del dueño que es solo un disparador (§4), los 3 gaps acoplados (§5), la conexión con el
   diagnóstico #858 (§6) y los seams de código verificados (§7).
2. Memoria engram (project `apk-personal`): #858 (diagnóstico reinicio lunes), topics
   `scoring/gaps-manejo-operaciones-ciclo-vida` (#1200), `scoring/decisiones-ciclo-vida-y-doc-oficial`
   (#1194). Usá `mem_search` + `mem_get_observation`.
3. Contrato vigente: `docs/scoring/modelo-scoring-oficial-v1.md` + `modelo-matematico-nucleo-v1.md`.

## El problema (en una frase)
El motor lee la config ACTUAL y la aplica retroactivamente a los hechos viejos de la ventana de 7
días → editar un target a mitad de semana recalcula días ya cumplidos. El dueño intuye que "cada
día consolidado debería quedar con su score y promediarse" — pero eso es solo una intuición de
partida, no la respuesta.

## Método (sin código)
1. **Caracterizá el problema con números a mano** en los dos escenarios que le molestan al dueño:
   (a) editar un target a mitad de semana (ejemplo del handoff §2); (b) lunes nuevo vs fin de semana
   bueno (reinicio del diagnóstico #858).
2. **Generá varias ideas** de cómo el motor podría agregar en el tiempo — la intuición del dueño es
   UNA de partida; buscá más, no te quedes en una. Para cada idea, mostrá los números de los
   escenarios de arriba.
3. **Evaluá costo/riesgo de cada idea** contra el motor actual (366 tests, 27 axiomas, modelo
   semanal): qué esquema Room toca, cuántos tests, si enmienda el contrato matemático.
4. Evaluá si el **Gap B** (recaídas asumidas durante pausa de un track) se puede resolver aparte.

## Reglas
- Verificá los seams en el código real antes de afirmar (handoff §7). Nunca de memoria.
- NO cierres el espacio de ideas antes de mostrarle los números al dueño; él marca con la panza.
- NO arranques SDD ni escribas código sin su decisión.
- Los hechos NO se borran (decisión firme). Anti-trampa: la ventana de 7 días pesa al reactivar.

## Estado del repo al arrancar
El SDD de "hardening de operaciones de ciclo de vida" YA está cerrado y commiteado en
`feat/scoring-motor-nucleo-v1` (commits `d916011` Lote 1, `e309f2d` Lote 2), sin push. Los 3 gaps
acoplados (A/B/C del handoff §5) siguen pendientes — son ESTA sesión.
