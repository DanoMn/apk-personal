# Auditoría de decisiones — sesión 2026-06-12 (motor de scoring: valor de capa + opt-ins)

> **Rol de este doc:** inventario exhaustivo de AUDITOR. No edita los oficiales — los contrasta.
> Fecha de auditoría: 2026-06-15. Fuentes leídas: todos los .md de `exploracion-valor-capa/`,
> engram (`scoring/modelo-valor-capa` #1068, `scoring/definiciones-motor-capa` #1042,
> `scoring/metodologia-motor-capa` #1041, `scoring/modelo-valor-capa` búsquedas adicionales),
> estado actual de `arbol-scoring-v1.md`, `mapa-modelo-scoring-v1.md`, `plan-tecnico-scoring.md`.

---

## LEYENDA DE COLORES

- **VERDE** — decisión documentada en al menos un doc de exploración O en engram, Y ya migrada al oficial relevante.
- **AMARILLO** — documentada en exploración/engram pero NO en los oficiales (en proceso por agentes actuales o pendiente).
- **🔴 ROJO** — riesgo real de pérdida: no está en ninguna fuente persistente, o contradice algo en los oficiales que sigue activo.

---

## TABLA MAESTRA DE DECISIONES

| # | Decisión | Doc de exploración | Engram | Debería ir a (oficial) | ¿Ya en el oficial? | Estado |
|---|----------|--------------------|--------|------------------------|-------------------|--------|
| 1 | **Dos canales separados: `base ∈[0,1]` + `extra ≥0`** — base pregunta "¿está en pie?", extra pregunta "¿se destacó?". Nunca multiplicados entre sí. | `definiciones-dueno-v1.md` §P-F; `problema-arrastre-optin-v1.md` §2 axioma 2; `modelo-valor-capa-consolidado-v1.md` §2.3; `merge-arrastre-optin-consolidado.md` §3 | #1068 parcialmente | `arbol-scoring-v1.md` §6-NUEVO | Solo mencionado en tabla §2 (`"Cerrado 2026-06-12 — ver §6-NUEVO"`) pero la sección §6-NUEVO **NO EXISTE** en el doc | **AMARILLO** |
| 2 | **EJE: cumplir todo justo = score 1.0 = Plenitud** (no En marcha). Bandas: `R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10` con δ=0.10 | `definiciones-dueno-v1.md` "CERRADO"; `modelo-valor-capa-consolidado-v1.md` §0-ERRATA + §2.5; `merge-arrastre-optin-consolidado.md` §3 bandas; `problema-arrastre-optin-v1.md` axioma 3 | #1068 (bandas mencionadas) | `arbol-scoring-v1.md` §16-NUEVO + tabla §2 | Tabla §2 dice "Cerrado — ver §16-NUEVO" pero §16-NUEVO NO EXISTE. Las bandas del §16.1 ACTUAL dicen `A < 0.70` (no 0.62) y no tienen el canal base+extra: contradicción activa con el modelo cerrado | **🔴 ROJO** — contradicción no resuelta en arbol oficial: §16.1 dice `Atención < 0.70`; modelo cerrado dice `A < 0.62` |
| 3 | **ERRATA EM_TOP anulada**: el factor EM_TOP=0.85 que comprimía base era un error del agente; "cumplir justo = En marcha" fue una mala interpretación. Versiones previas con `ESTADO = EM_TOP·base + W_extra·extra` (donde EM_TOP hacía que base perfecta → 0.85) son OBSOLETAS | `modelo-valor-capa-consolidado-v1.md` §0-ERRATA; `definiciones-dueno-v1.md` "EJE — CORREGIDO"; `problema-arrastre-optin-v1.md` axioma 3 "(NO existe EM_TOP; fue un error ya anulado)" | No salvado explícitamente | `arbol-scoring-v1.md` §2.1 (nota general) | §2.1 menciona "los nuevos §6-NUEVO, §11-NUEVO... describen el contrato cerrado" pero el modelo viejo de §6 sigue visible y no hay anotación de OBSOLETO en esas secciones antiguas | **AMARILLO** |
| 4 | **Pesos de capa IGUALES: `peso = 1/N`** — el opt-in NO infla el peso de la capa en el total (se eliminó `tamaño = 1+K` del pool global). Decisión: pesos por capa iguales + opt-in pesa dentro de su capa como término-sombra separado | `modelo-valor-capa-consolidado-v1.md` §0-bis; `merge-arrastre-optin-consolidado.md` §3 (W0=1 para anclas, w dinámico para opt-in) | #1068 (mecanismo término-sombra) | `arbol-scoring-v1.md` §14-NUEVO | §14-NUEVO NO EXISTE. El §14.2 ACTUAL usa `WeeklyBaseScore = 0.750·AverageLayerScore + 0.250·WorstLayerScore` (modelo viejo, peor-capa 25%) — contradice el modelo v4 | **🔴 ROJO** — el §14.2 oficial actual es el modelo viejo incompatible |
| 5 | **Opt-in = término-sombra de peso dinámico** `w = BETA·N·(1−M)`, con `w(M=1) = 0` (invisible cuando bien). Extra siempre con pesos iguales (Sol=Tin por construcción) | `merge-arrastre-optin-consolidado.md` §3 + §8; `problema-arrastre-optin-v1.md` §6 pista; `subagente-C-propuesta.md` (origen del mecanismo) | #1068 (mecanismo cerrado) | `arbol-scoring-v1.md` §11-NUEVO | §11-NUEVO NO EXISTE. El §11 actual describe `BodyScore = 0.700·BodyBase + 0.300·SleepWeekly` (modelo viejo con mezcla fija) | **🔴 ROJO** — §11 actual es modelo viejo contradictorio |
| 6 | **BETA = 0.818** (despejado de TARGET = 0.55: "opt-in en piso + anclas perfectas → Atención") | `merge-arrastre-optin-consolidado.md` §4 + §6 tabla de despeje; `problema-arrastre-optin-v1.md` §6 pista; `modelo_valor_capa_v4_merge.py` | #1068 ("BETA=0.818 (de TARGET=0.55)") | `arbol-scoring-v1.md` tabla §2 | Tabla §2 menciona "BETA=0.818 (despejado de TARGET=0.55)" como "Cerrado 2026-06-12" — SÍ está en la tabla pero sin detalle de fórmula ni sección de respaldo | **AMARILLO** — en tabla §2 pero sin sección §11-NUEVO que lo complete |
| 7 | **COMPONER opt-ins** (no "peor manda"): cuando sueño y sobriedad están mal a la vez, sus términos-sombra se suman (w total = suma de los w individuales). Sin tope. | `merge-arrastre-optin-consolidado.md` §8 decisión 1; `modelo-valor-capa-consolidado-v1.md` §2.5 | #1068 ("COMPONER... SIN tope") | `arbol-scoring-v1.md` §11-NUEVO | §11-NUEVO NO EXISTE | **AMARILLO** |
| 8 | **Mismo BETA para sueño y sobriedad** — la sobriedad técnicamente debería pegar más fuerte, pero el dueño decidió compasión: no desmotivar a quienes más la necesitan | `merge-arrastre-optin-consolidado.md` §8 decisión 2 | #1068 ("MISMO BETA... 'técnicamente debería pesar más pero hay que ser compasivos'") | `arbol-scoring-v1.md` §11-NUEVO | §11-NUEVO NO EXISTE | **AMARILLO** |
| 9 | **Señal de sueño continua** `M ∈ [0,1]` (4 componentes por noche, telemetría); sin dato → `B_SLEEP` (no 0) | `definiciones-dueno-v1.md` hallazgos, respuesta #2; `modelo-valor-capa-consolidado-v1.md` §2.1; `merge-arrastre-optin-consolidado.md` §8 decisión 3; todos los subagentes | #1068 (señal sueño) | `arbol-scoring-v1.md` §11-NUEVO + §12-NUEVO | §11-NUEVO y §12-NUEVO NO EXISTEN. El §11 actual menciona `SleepWeeklyScore` pero como 30% fijo de Cuerpo (modelo viejo) | **AMARILLO** |
| 10 | **B_SLEEP = 0.5** (base de sueño sin dato, calibrable) | `modelo-valor-capa-consolidado-v1.md` §5; `merge-arrastre-optin-consolidado.md` §3 variables | #1068 ("sin dato→B_SLEEP=0.5") | `arbol-scoring-v1.md` §11-NUEVO | §11-NUEVO NO EXISTE | **AMARILLO** |
| 11 | **Señal de sobriedad**: `M_sobr = (1 − A)^(días de recaída en los últimos 7 días)`, **A = 0.55**. Multi-track: producto por track (track limpio = 1 = invisible, no diluye el golpe de 1 recaída) | `merge-arrastre-optin-consolidado.md` §8 decisión 4; `modelo-valor-capa-consolidado-v1.md` §2.1 + §4 caso 8; `subagente-C-propuesta.md` §2.2 | #1068 ("M_sobr=(1-A)^(días de recaída), A=0.55. Multi-track: producto") | `arbol-scoring-v1.md` §12-NUEVO | §12-NUEVO NO EXISTE. El §12/§13 ACTUAL describe `SobrietyTrackScore` con fórmula completamente diferente (CleanCoverage×RelapseProtection×TrackingConfidence, 70/30 worst/avg) — contradice el modelo cerrado | **🔴 ROJO** — §12/§13 actual contradice el modelo cerrado con fórmula incompatible |
| 12 | **Sobriedad gradúa por días de recaída** (opción 3, punto medio): 1 día → En marcha (0.829), 3 días → Atención (0.612), 7 días → Atención (0.553). No es binario hold/broke | `merge-arrastre-optin-consolidado.md` §8 decisión 4 (verificado con script) | #1068 (valores verificados explícitamente) | `arbol-scoring-v1.md` §12-NUEVO | §12-NUEVO NO EXISTE. §13 actual usa binario held/broke con `relapseDays` en una fórmula distinta | **🔴 ROJO** — contradicción grave: arbol actual usa held/broke binario; modelo cerrado usa exponencial por días |
| 13 | **Arrastre PLANO en N**: opt-in en piso + anclas perfectas → mismo estado independientemente del nº de capas (N=3,4,5). Solución: el peso dinámico crece con N (`w = BETA·N·(1−M)`) para compensar exactamente la dilución | `merge-arrastre-optin-consolidado.md` §3 + §4 "C3 arrastre PLANO"; `problema-arrastre-optin-v1.md` §4 C3 | #1068 implícito | `arbol-scoring-v1.md` §11-NUEVO | §11-NUEVO NO EXISTE | **AMARILLO** |
| 14 | **El extra (superhabit) siempre con pesos iguales entre capas con anclas (Sol = Tin)**: el opt-in NO contamina el canal extra; un superhabit en Cuerpo+sueño vale lo mismo que en Interior | `merge-arrastre-optin-consolidado.md` §3 + §4 "C5 Sol=Tin"; `problema-arrastre-optin-v1.md` §4 C5; `modelo-valor-capa-consolidado-v1.md` §0-bis | #1068 ("Extra siempre pesos iguales (Sol=Tin)") | `arbol-scoring-v1.md` §6-NUEVO / §14-NUEVO | Las secciones NUEVO no existen | **AMARILLO** |
| 15 | **El opt-in bien (señal=1) es NEUTRO**: no sube ni baja el score respecto a no tenerlo. Mecanismo: `w(M=1) = BETA·N·(1−1) = 0` (el término-sombra desaparece) | `merge-arrastre-optin-consolidado.md` §3; `problema-arrastre-optin-v1.md` §3 axioma 7 + §4 C2 | #1068 (mecanismo w=0) | `arbol-scoring-v1.md` §11-NUEVO | §11-NUEVO NO EXISTE | **AMARILLO** |
| 16 | **Nº de anclas NO cambia el peso de la capa**: las anclas promedian a un bloque de tamaño W0=1. Más anclas en una capa = no cambia su peso en el total | `problema-arrastre-optin-v1.md` axioma 4; `modelo-valor-capa-consolidado-v1.md` §2.4 + §4 caso 11; `merge-arrastre-optin-consolidado.md` §3; todos subagentes | #1042 (D1–D5 implícito) | `arbol-scoring-v1.md` §6-NUEVO; `mapa-modelo-scoring-v1.md` §3 dec.3 | `mapa-modelo-scoring-v1.md` §1 y §3 SÍ lo menciona ("nº de anclas no cambia el peso"). Las secciones NUEVO no existen | **AMARILLO** |
| 17 | **Reparto interno del opt-in: mantiene `K/(1+K)`, NO diluye con nº de anclas** — "las anclas promedian a un bloque; meter n en el denominador rompería axioma 5". Unánime en los 3 subagentes y en el consolidado | `modelo-valor-capa-consolidado-v1.md` §1 punto 5; `subagente-A-propuesta.md` §2.2; `subagente-B-propuesta.md` §1.6; `subagente-C-propuesta.md` §3.2 | No salvado explícitamente como decisión separada | `arbol-scoring-v1.md` §6-NUEVO / §11-NUEVO | Las secciones NUEVO no existen | **AMARILLO** |
| 18 | **Capa solo-opt-in (sin anclas): su valor = la señal `g(M)`; extra = 0**. D4: activar un opt-in en una capa sin anclas activa esa capa (puede quedar capa activa sin anclas) | `definiciones-dueno-v1.md` D4 + respuesta #5; `problema-arrastre-optin-v1.md` axioma 6; `modelo-valor-capa-consolidado-v1.md` §2.3 `core = g(M)` si solo opt-in; `merge-arrastre-optin-consolidado.md` §3 | #1042 (D4 capturado) | `arbol-scoring-v1.md` §6-NUEVO / §11-NUEVO | §7.4 del arbol actual dice `activeLayersWithAnchor < 3 → NoData` (una capa sin ancla pero con opt-in NO cuenta). Esto contradice D4 del modelo cerrado donde una capa solo-opt-in SÍ participa | **🔴 ROJO** — §7.4 del arbol oficial contradice D4 del dueño: actualmente bloquea scoring si <3 capas con ancla, sin contar capas solo-opt-in |
| 19 | **Anti-incentivo del opt-in NO es un problema** (decisión filosófica aceptada): activar un opt-in solo puede empatar (bien) o bajar (mal) el estado, nunca subir. Para sobriedad manual esto técnicamente premia no trackear lo que va mal; el dueño lo aceptó | `modelo-valor-capa-consolidado-v1.md` §9-bis rareza 1 | No salvado explícitamente | No hay doc oficial que lo mencione | Sin rastro en oficiales | **🔴 ROJO** — sin documentación en ningún oficial; si alguien lo cuestiona en el futuro, no hay registro de que fue aceptado |
| 20 | **Rareza: mismo superhabit rinde igual en cualquier capa** (Sol=Tin): con pesos iguales y extra separado, no hay distorsión por opt-in. Aceptado | `merge-arrastre-optin-consolidado.md` §4 "C5 Sol=Tin empatan=True"; `modelo-valor-capa-consolidado-v1.md` §0-bis | No salvado como "rareza aceptada" | No hay doc oficial | Sin rastro en oficiales | **AMARILLO** |
| 21 | **Rareza: "hacer más y bajar de banda"** (agregar una capa con cumplimiento justo promedia el extra a la baja si las demás tienen superhabit). Tensión identificada pero no resuelta explícitamente (pendiente perilla de suma vs promedio) | `modelo-valor-capa-consolidado-v1.md` §9-bis rareza 2 | No salvado | No hay doc oficial | Sin rastro en oficiales | **🔴 ROJO** — rareza real sin resolución explícita ni registro en oficiales; podría sorprender en calibración |
| 22 | **Rareza: capa muerta (solo-opt-in) no colapsa el superhabit**: una capa sin anclas entra a la base pero NO a la cuenta del extra. Aceptado | `definiciones-dueno-v1.md` P-F + §6 "capa solo-opt-in no diluye techo de Inquebrantable"; `merge-arrastre-optin-consolidado.md` §3 "extra = promedio de extra_capa sobre capas con anclas" | Parcialmente en #1068 | No hay doc oficial explícito | Sin rastro claro en oficiales | **AMARILLO** |
| 23 | **METODOLOGÍA: axiomas primero, magnitudes emergen** — ninguna cifra de docs previos (`±0.1 soporte`, `k_sobr=3.0`, `γ`) es verdad del modelo v4; la sesión parte de comportamientos buscados → formaliza axiomas → despeja números | `definiciones-dueno-v1.md` header; #1041 | #1041 (completo) | `mapa-modelo-scoring-v1.md` §4 | §4 del mapa (nota de metodología 2026-06-10) menciona que las 45 marcas ya no son medidores; pero no hay una nota explícita de que los pesos de los docs previos (k_sleep=1.5, k_sobr=3.0, γ) sean obsoletos. La tabla de pesos del §1 usa k_sleep=1.5 y k_sobr=3.0 sin marcarlo como obsoleto | **🔴 ROJO** — `mapa-modelo-scoring-v1.md` §1 muestra tabla de pesos con k_sleep=1.5, k_sobr=3.0 como si fueran válidos, pero el modelo v4 los reemplazó con BETA despejado de axiomas (0.818). La tabla induce a error |
| 24 | **VOCABULARIO: "área" = "capa"** — en el código y contexto del proyecto estos términos son sinónimos | `definiciones-dueno-v1.md` referencia D6 y texto | No salvado | No relevante para oficiales | — | **VERDE** (trivial, no hay riesgo) |
| 25 | **Soportes → canal BASE, aditivos saturados; soportes bidireccionales (suman si full, restan si descuidados)**. Magnitud PENDIENTE (no definida en modelo v4). `sup_term` centrado (±) | `modelo-valor-capa-consolidado-v1.md` §2.3 + §3 tabla ("soportes bidireccionales, centrado B"); `subagente-A-propuesta.md` §2.3; `subagente-B-propuesta.md` §1.4 | No salvado para v4 | `arbol-scoring-v1.md` tabla §2 dice "Soportes: PENDIENTE" | La tabla §2 lo marca como PENDIENTE explícitamente — correcto | **AMARILLO** — correcto que esté pendiente; pero el §9 del arbol actual tiene fórmula de soporte vieja que debería marcarse OBSOLETA |
| 26 | **Tasks → canal BASE, menor que soportes, saturados**. `TASK_SAT < SUP_SAT`. Tasks neutras (sin capa) no suman. Magnitud PENDIENTE para v4 | `modelo-valor-capa-consolidado-v1.md` §2.3 `task_term`; `subagente-A-propuesta.md` §2.4; `subagente-B-propuesta.md` §1.4 | #1041 ("tasks con contributionRole=Neutral no suman, por diseño") | `arbol-scoring-v1.md` §10 + tabla §2 | `plan-tecnico-scoring.md` dec.31 y §10 del arbol tienen TaskMomentum que suma; pero el canal no está integrado en el modelo v4 | **AMARILLO** |
| 27 | **PENDIENTE (sesión): soportes y tasks — cómo entran al valor de capa** (soportes bidireccionales, magnitud exacta, saturación multi-soporte; tasks < soportes, saturación) | `definiciones-dueno-v1.md` §Pendiente dictado; `merge-arrastre-optin-consolidado.md` §9 | #1068 ("PENDIENTE para otra sesión: SOPORTES y TASKS") | `arbol-scoring-v1.md` tabla §2 | Tabla §2 lo marca como PENDIENTE — correcto | **VERDE** — conocido y registrado como pendiente |
| 28 | **PENDIENTE (sesión): calibración fina** de bandas (`W_EXTRA`, δ) y de A/BETA contra marcas del dueño | `merge-arrastre-optin-consolidado.md` §9; `modelo-valor-capa-consolidado-v1.md` §9 | #1068 ("calibración fina... cuando toque") | No aplica aún | — | **VERDE** — conocido y registrado |
| 29 | **PENDIENTE: micro-pregunta sobriedad** — ¿1 día de recaída vs 3 días pesan igual o gradúan? (respondido implícitamente por A=0.55: sí gradúan; pero el dueño no lo marcó explícitamente en sesión) | `definiciones-dueno-v1.md` §Pendiente dictado | #1068 (los valores lo confirman: 1d=0.829, 3d=0.612, 7d=0.553 — sí gradúa) | `arbol-scoring-v1.md` §12-NUEVO | §12-NUEVO no existe; la pregunta queda respondida en los scripts pero no en el oficial | **AMARILLO** |
| 30 | **PENDIENTE: multi-sobriedad** (alcohol + tabaco + otros) — producto de tracks parcialmente definido, pero la interacción multi-track sigue mencionada como abierta en definiciones | `definiciones-dueno-v1.md` §Pendiente (micro-pregunta); `merge-arrastre-optin-consolidado.md` §8 decisión 4 | #1068 (producto por track cerrado) | `arbol-scoring-v1.md` §12-NUEVO | §12-NUEVO no existe, pero la decisión de producto ya está cerrada en engram | **AMARILLO** — producto cerrado en engram pero no en oficial |
| 31 | **Decisión de metodología: NO heredar magnitudes de docs previos** — los K del mapa-modelo-scoring-v1.md (k_sleep=1.5, k_sobr=3.0) son fase de descubrimiento, no contrato del modelo v4 | `definiciones-dueno-v1.md` header | #1041 (completo y explícito) | `mapa-modelo-scoring-v1.md` §1 (debería marcarse obsoleto en §1 tabla pesos) | La tabla de pesos del mapa usa esos números sin nota de obsolescencia | **🔴 ROJO** — riesgo de confusión con la tabla vigente del §1 del mapa |
| 32 | **El opt-in motor de capa v4 NO está implementado en código** — el código actual es el modelo VIEJO (deuda técnica); no copiar valores del código | `arbol-scoring-v1.md` §2.1 (nota ya escrita) | No aplica | `arbol-scoring-v1.md` §2.1 | Ya documentado en §2.1 y §2.2 del arbol | **VERDE** |
| 33 | **El modelo de valor de capa v4 (merge)** usa `ESTADO = min(base,1) + extra` — NO usa EM_TOP como factor de escala. La base sola tiene techo 1.0 por el `min()`. El score global es el promedio ponderado de los términos (ancla+sombra) | `merge-arrastre-optin-consolidado.md` §3 (`ESTADO = min(base,1) + extra`); vs `modelo-valor-capa-consolidado-v1.md` §2.5 que usa `EM_TOP · BASE_global + W_EXTRA · EXTRA_global` — HAY DISCREPANCIA INTERNA entre los dos docs de exploración | Parcialmente en #1068 | `arbol-scoring-v1.md` §6-NUEVO / §14-NUEVO | §14-NUEVO NO EXISTE — la fórmula exacta no está en ningún oficial | **🔴 ROJO** — hay discrepancia entre dos docs de exploración (consolidado-v1 usa EM_TOP; merge-arrastre no); el oficial no tiene la sección NUEVO que resuelva cuál es el contrato definitivo |

---

## HALLAZGOS CRÍTICOS (decisiones sin rastro en ningún oficial)

### A. Las secciones §6-NUEVO, §11-NUEVO, §12-NUEVO, §14-NUEVO, §16-NUEVO del arbol NO EXISTEN

`arbol-scoring-v1.md` las referencia en la tabla §2 con "Cerrado 2026-06-12 — ver §X-NUEVO" pero esas secciones nunca fueron escritas. El documento tiene 824 líneas y ninguna contiene esos encabezados. Esto significa que el contrato matemático cerrado el 2026-06-12 **no está formalizado en ningún doc oficial** — solo vive en los docs de exploración y en engram.

### B. Contradicciones activas en `arbol-scoring-v1.md`

Las siguientes secciones del arbol oficial CONTRADICEN el modelo cerrado y no están marcadas como OBSOLETAS:

| Sección del arbol actual | Qué dice | Qué dice el modelo cerrado v4 |
|--------------------------|----------|-------------------------------|
| §11 (Cuerpo con sueño) | `BodyScore = 0.700·BodyBase + 0.300·SleepWeekly` (mezcla fija) | Sueño como término-sombra dinámico `w = BETA·N·(1-M)`, pesos relacionales |
| §12 (Conducta con sobriedad) | `ConductScore = 0.700·ConductBase + 0.300·SobrietyWeekly` (mezcla fija) | Sobriedad como término-sombra dinámico con mismo BETA |
| §13 (Sobriedad) | `SobrietyTrackScore = CleanCoverage × RelapseProtection × TrackingConfidence` con 70/30 worst/avg | `M_sobr = (1-0.55)^(días de recaída)`, producto por track |
| §14.2 (Score semanal) | `WeeklyBaseScore = 0.750·Avg + 0.250·Worst` (peor capa 25%) | Promedio ponderado con términos ancla (W0=1) + sombra (w dinámico); sin worst-term |
| §16.1 (Bandas) | `Atención < 0.70` | `A < 0.62` |
| §7.4 (Gate mínimo) | `activeLayersWithAnchor < 3 → NoData` (sin contar capas solo-opt-in) | D4: capa solo-opt-in es una capa activa válida |
| §16.2 (Ladder peor capa) | Ladder de caps por `WorstLayerScore` (reglas-parche) | Motor de pesos puros, cero reglas/caps/worst-term |
| §16.4 (Puerta Inquebrantable) | Gate duro con 4 condiciones simultáneas | Motor de pesos puros, Inquebrantable ≡ score ≥ 1+δ por promedio ponderado |

### C. `mapa-modelo-scoring-v1.md` §1 muestra pesos OBSOLETOS como si fueran vigentes

La tabla del §1 dice `k_sleep=1.5, k_sobr=3.0` (representativos) y la Forma A (multiplicador+renormalización) sin nota de que el modelo v4 los reemplazó con BETA=0.818 y el mecanismo de término-sombra.

### D. Discrepancia interna entre los dos consolidados de exploración

- `modelo-valor-capa-consolidado-v1.md` §2.5: `ESTADO = EM_TOP·BASE_global + W_EXTRA·EXTRA_global` (con EM_TOP como factor de escala)
- `merge-arrastre-optin-consolidado.md` §3: `ESTADO = min(base,1) + extra` (sin EM_TOP; el techo de 1 viene del `min()`)

Son dos fórmulas finales distintas. El `merge` es posterior y más refinado, pero el consolidado-v1 fue la referencia del primer agente. El oficial nunca resolvió cuál es el contrato.

---

## ENGRAM vs DOCS DE EXPLORACIÓN — COBERTURA

| Topic en engram | ID | Contenido guardado | ¿Cubre todo lo de la sesión? |
|-----------------|----|--------------------|------------------------------|
| `scoring/modelo-valor-capa` | #1068 | Motor de opt-ins cerrado: COMPONER, mismo BETA, señal sueño continua, señal sobriedad (1-A)^días A=0.55, BETA=0.818, mecanismo w=BETA·N·(1-M) | Sí, cubre las decisiones finales del motor de opt-ins. Fecha: 2026-06-14 (posterior a la sesión — actualizado) |
| `scoring/definiciones-motor-capa` | #1042 | D1–D5 del dueño, inicio de sesión | Parcial — D6–D12 están en `definiciones-dueno-v1.md` pero no en engram |
| `scoring/metodologia-motor-capa` | #1041 | Metodología: axiomas primero, no magnitudes previas | Sí, completo para esta decisión |

**Decisiones que no están en ninguna entrada engram:**
- Errata EM_TOP (anulación del error)
- Rareza: anti-incentivo opt-in aceptada (ítem 19)
- Rareza: "hacer más y bajar de banda" no resuelta (ítem 21)
- Reparto interno opt-in `K/(1+K)` vs `K/(n+K)` → elegido `K/(1+K)` (unánime los 3 subagentes)
- Discrepancia entre fórmulas finales de los dos consolidados (ítem 33)

---

## GAPS / ACCIONES PENDIENTES PARA EL ORQUESTADOR

### URGENTE — Secciones NUEVO que faltan en el arbol oficial

El arbol `arbol-scoring-v1.md` referencia secciones que no existen. Los agentes que actualizan los oficiales deben crear estas secciones O decidir que el arbol se reestructura. Sin estas secciones, el arbol es internamente incoherente (referencia a secciones inexistentes).

**Secciones a crear:**

1. **§6-NUEVO** — Valor de capa v4: base = `promedio_i min(R_i,1)`; extra = `promedio_i max(R_i-1,0)`. Dos canales separados. Capa solo-opt-in: valor = señal, extra = 0.

2. **§11-NUEVO** — Opt-in como término-sombra: `w = BETA·N·(1-M)` con `w(M=1)=0`. Mismo BETA para sueño y sobriedad. COMPONER (no peor manda). Sin tope. Arrastre plano en N.

3. **§12-NUEVO** — Señales: sueño `M = c·avg(noches) + (1-c)·B_SLEEP` (B_SLEEP=0.5); sobriedad `M = (1-0.55)^(días de recaída en 7d)`, multi-track producto (track limpio=1=invisible).

4. **§14-NUEVO** — Agregación global v4: `base = Σ(valor·peso)/Σ(peso)` sobre términos ancla (W0=1) y sombra (w dinámico); `extra = promedio_simple(extra_capa) sobre capas con anclas`; `ESTADO = min(base,1) + extra`.

5. **§16-NUEVO** — Bandas v4: `R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10` (δ=0.10). Estas bandas reemplazan las de §16.1 y el ladder de §16.2. El motor v4 no tiene worst-term ni caps.

### URGENTE — Marcar secciones OBSOLETAS en el arbol

Los siguientes bloques del arbol deben marcarse con `> ⚠️ OBSOLETO — reemplazado por §X-NUEVO (modelo v4, cerrado 2026-06-12)`:

- §6.1, §6.2, §6.3 (valor de capa viejo)
- §11 (Cuerpo con sueño — mezcla fija)
- §12 (Conducta con sobriedad — mezcla fija)
- §13 (Sobriedad — fórmula CleanCoverage, held/broke)
- §14.2 (WeeklyBaseScore con 75/25 worst)
- §16.1 (bandas con Atención < 0.70)
- §16.2 (ladder de caps)
- §16.4 (puerta Inquebrantable con 4 gates)

### IMPORTANTE — Resolver discrepancia entre los dos consolidados de exploración

`modelo-valor-capa-consolidado-v1.md` §2.5 usa `EM_TOP·BASE + W_EXTRA·EXTRA`; `merge-arrastre-optin-consolidado.md` §3 usa `min(base,1) + extra`. El `merge` es la versión más reciente y la que está en engram (#1068). El orquestador debe declarar explícitamente que el contrato definitivo es la fórmula del merge y actualizar el consolidado-v1 o marcarlo como superado.

### IMPORTANTE — Actualizar `mapa-modelo-scoring-v1.md` §1

La tabla de pesos del §1 (k_sleep=1.5, k_sobr=3.0, Forma A multiplicador) debe marcarse como OBSOLETA con referencia al modelo v4 (BETA=0.818, término-sombra). Sin esta nota, el mapa activo induce a usar parámetros del modelo viejo.

### NORMAL — Salvar en engram las decisiones que faltan

Las siguientes no tienen entrada propia en engram:
- Anulación de EM_TOP (errata)
- D6–D12 del dueño (solo D1–D5 están en #1042)
- Rareza anti-incentivo opt-in aceptada
- Rareza "hacer más y bajar de banda" (abierta)
- Decisión `K/(1+K)` unánime (no diluir con nº anclas)
- Decisión fórmula final del merge vs consolidado-v1

### NORMAL — Confirmar D4 en el gate de configuración mínima

`arbol-scoring-v1.md` §7.4 requiere `activeLayersWithAnchor ≥ 3`, sin contar capas solo-opt-in. Esto contradice D4 del dueño (activar un opt-in en capa sin anclas activa esa capa). El orquestador debe definir si el gate de ≥3 capas cuenta capas solo-opt-in o no.

---

## RESUMEN EJECUTIVO

**Decisiones cerradas en la sesión:** 8 (motor de opt-ins completo + eje + estructura de dos canales + pesos iguales + metodología)

**Decisiones bien documentadas (exploración + engram):** todas las 8 anteriores

**Decisiones que ya llegaron a los oficiales:** 0 (las secciones NUEVO están referenciadas pero no escritas)

**Contradicciones activas en oficiales:** 8 secciones del arbol + 1 tabla del mapa contradicen el modelo cerrado

**Decisiones en riesgo de pérdida (sin ninguna fuente persistente):**
- Rareza anti-incentivo opt-in aceptada (no está en ningún oficial ni en engram)
- Rareza "hacer más y bajar de banda" sin resolución explícita
- Discrepancia entre fórmulas finales de los dos consolidados

**Principales gaps para el orquestador:** crear §6-NUEVO, §11-NUEVO, §12-NUEVO, §14-NUEVO, §16-NUEVO en el arbol; marcar secciones obsoletas; actualizar tabla de pesos del mapa.
