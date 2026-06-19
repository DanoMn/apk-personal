# Pro-prompt — Planificar SOPORTES y TASKS en el valor de capa

> Prompt de arranque para la próxima sesión de scoring. Autocontenido: con esto + las referencias,
> un agente nuevo arranca sin re-leer toda la historia. Método del proyecto: **axiomas primero, no
> heredar magnitudes** (el dueño define el comportamiento; el modelo hace emerger los valores).

---

## 1. Objetivo de la sesión

Definir cómo **SOPORTES** y **TASKS** entran al **valor de una capa**, completando el motor de capa.
El ancla, el valor de capa y los opt-ins (sueño/sobriedad) ya están **CERRADOS** (no se re-discuten).
Esto es la última pieza de modelado del valor de capa.

## 2. Lo que YA está cerrado (NO re-discutir — es el marco que soportes/tasks deben respetar)

- **Valor de capa = `min(base,1) + extra`** (escala del ancla [0,1.5]).
  - `base` = ¿está en pie? (≤1). `extra` = superhabit (≥0, **SOLO de las anclas** hasta ahora).
- **Score global** = promedio de valores de capa, **pesos de capa iguales (1/N)**. Bandas
  `R<0.40·A<0.62·EM<0.85·P≥0.85·I≥1.10`. Cumplir todo justo = 1.0 = Plenitud.
- **Opt-ins** (sueño/sobriedad) = término-sombra de peso dinámico en la base; ver contrato
  `docs/scoring/axiomas-opt-in-v1.md` (O1–O13). NO los toques.
- **Orden de importancia (axioma del dueño): ANCLAS > SOPORTES > TASKS.**
- Motor de pesos puros: cero gates/caps/worst-term/min duro. Continuo y diferenciable.

## 3. Lo que se sabe de soportes y tasks (de dominio/producto — verificar en las fuentes)

**SOPORTES** (`docs/dominio/definicion-reestructuracion-soporte.md`, `nucleo-dominio-autonomia.md`):
- Mantenimiento diario que sostiene dignidad/estructura (tomar agua, cepillarse, orden mínimo).
- **Sin targets** (no tienen meta de frecuencia/tiempo como las anclas).
- **UX inversa**: el usuario marca lo que NO hizo (el sistema asume cumplido). Eso es SOLO
  presentación; la lógica interna es polaridad normal (días sostenidos, más alto = mejor).
- Capa obligatoria. Complementan las anclas, no compiten. Especialmente útiles en restauración.
- **No son obligatorios** para el sistema.

**TASKS** (`nucleo-dominio-autonomia.md` §Task):
- Pendientes puntuales, una sola vez, sin recurrencia. Tienen capa asignada.
- Aportan **poco** (menos que un soporte) y **NO son neutras** si tienen capa + rol no-neutral.
- Una task **neutral** (sin capa o rol Neutral) **NO suma** (por diseño del dominio).

⚠️ **MAGNITUDES VIEJAS DESCARTADAS:** el ±0.1 del soporte y el 0.05 de la task son del modelo viejo y
**NO se heredan**. Definir por axiomas.

## 4. Preguntas abiertas a resolver con el dueño (axiomas a definir)

SOPORTES:
- ¿Entran al canal **base** o al **extra**? (Intuición previa: base — son mantenimiento, no superhabit.)
- ¿Cómo se mide su señal? (¿días sostenidos / 7, análogo a la sobriedad? ¿binario? ¿continuo?)
- ¿Mueven en ambos sentidos (sostenerlos suma, descuidarlos resta) o solo restan?
- **Agregación multi-soporte**: ¿cómo se combinan 3, 5, 8 soportes sin que "fabriquen una banda"?
  (saturación, como en el ancla/opt-ins).
- ¿Afectan el **peso** de la capa o solo su **valor**? (Probable: solo valor, son light.)
- ¿Cuánto pesan respecto a las anclas? (axioma: anclas > soportes).

TASKS:
- Canal: ¿**base** o **extra** de la capa? (Pregunta abierta desde el inicio: ¿una task completada
  ayuda a "estar en pie" o es un "extra" de esa capa?)
- Magnitud: **< soporte** (axioma de orden). ¿Cuánto?
- **Agregación multi-task** (saturación).
- ¿Las tasks pendientes-no-hechas penalizan, o solo suman las hechas? (dominio: las pendientes no
  penalizan; son puntuales.)

## 5. Método (el que funcionó para ancla y opt-ins)

1. El dueño da las **definiciones de comportamiento** (no magnitudes).
2. Se formaliza un **contrato de axiomas** (estilo A1–A10 del ancla, O1–O13 del opt-in).
3. Si el problema es complejo: **3 proponentes a ciegas con sesgos distintos + research + merge**,
   todo verificado con python3 (casos límite + tabla antes/después).
4. Las magnitudes se **despejan de axiomas de estado** del dueño, no se eligen a dedo.
5. Verificar que NO se rompan los axiomas ya cerrados (O1–O13, dos canales, eje, pesos iguales).

## 6. Punto de partida candidato (de la sesión multi-agente del valor de capa — NO cerrado)

Los 3 proponentes del valor de capa propusieron (sin que el dueño lo confirme aún): soportes y tasks
como **aporte aditivo saturado al canal base**, con `task < soporte`, saturación exponencial para que
multi-soporte no fabrique banda, y soportes **centrados** (sostener suma, descuidar resta). Es un punto
de arranque razonable, pero el dueño debe definir su comportamiento desde cero (axiomas primero).

## 7. Restricciones duras (no romper)

- No tocar el ancla ni los opt-ins (O1–O13).
- Soportes/tasks NO deben distorsionar el superhabit (Sol=Tin debe seguir).
- Mantener cumplir-justo = Plenitud y el resto del eje.
- Sin gates/caps/worst-term. Continuo.
- Respetar anclas > soportes > tasks.

## 8. Referencias

- Handoff de la sesión previa: `meta/handoffs/2026-06-12-motor-valor-capa-optins.md`.
- Contrato opt-ins: `docs/scoring/axiomas-opt-in-v1.md`.
- Modelo v4: `docs/scoring/exploracion-valor-capa/merge-arrastre-optin-consolidado.md` +
  `modelo_valor_capa_v4_merge.py`.
- Mapa vivo: `docs/scoring/mapa-modelo-scoring-v1.md`. Árbol: `docs/scoring/arbol-scoring-v1.md`.
- Dominio soportes: `docs/dominio/definicion-reestructuracion-soporte.md`,
  `docs/producto/nucleo-dominio-autonomia.md`. Historias: `docs/scoring/historias-soportes-tasks-v1.md`.
- Engram (project `apk-personal`): topic `scoring/modelo-valor-capa`.

## 9. Primer paso sugerido al arrancar

Pedirle al dueño las **definiciones de comportamiento de soportes** (canal, señal, multi-soporte,
bidireccional o no), formalizarlas como axiomas S1…Sn, y recién después modelar. Igual para tasks
(T1…Tn). NO empezar por números.
