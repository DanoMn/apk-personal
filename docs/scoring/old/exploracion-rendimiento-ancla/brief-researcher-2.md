# Brief — Researcher 2: Saturación y retornos decrecientes

> Misión redactada por el orquestador. Sesión: exploración del rendimiento de un ancla (2026-06-09).

## 0. Tu rol y tu ángulo

Sos un researcher matemático. NO proponés la solución final: proveés vocabulario matemático externo,
bien mapeado al problema, para que el orquestador consolide. Podés y debés usar búsqueda web.
NO leas las propuestas de otros subagentes.

**Tu ángulo asignado — saturación y retornos decrecientes:** cómo se modela "los primeros valen
mucho, los extra valen cada vez menos" y cómo se premia consistencia sobre picos. Esto alimenta dos
necesidades concretas: (a) que un día con tiempo trivial aporte casi nada sin necesidad de umbral
duro, y (b) que el superávit sume de forma acotada/decreciente sin explotar. Dominios candidatos (no
exhaustivo, no te limites): cinética de saturación (Michaelis-Menten, Hill), exponenciales saturantes
(1−e^(−x/k)), funciones cóncavas de utilidad marginal decreciente, sigmoides (logística, Gompertz —
ojo: la parte convexa baja sirve para castigar lo trivial), softplus/log(1+x), curvas de
dosis-respuesta, modelos de fatiga/aprendizaje, diseño de XP/niveles en juegos (XP curves con
diminishing returns y anti-grinding), sistemas de rating con anti-farming.

## 1. El sistema (lo mínimo que necesitás)

App local de hábitos/consistencia. Motor de pesos puros: `score = Σ(peso_capa × valor_capa)`; el
valor de capa es el promedio de sus anclas. El problema de la sesión: **el VALOR de un ancla**.

**Inputs del ancla:** `F` = días comprometidos por semana (1–7); `T` = minutos objetivo por sesión.
Registro: array de 7 días con minutos (0 = no marcó). Derivados: `D` = días marcados, `t_i` = minutos
de cada día marcado.

**Mecanismo de zonas (Best-F):** de los D días marcados, los F con mayor tiempo = zona de compromiso
(tiempo bidireccional: déficit si `t_i < T`, superávit si `t_i > T`); los D−F restantes = zona
voluntaria (unidireccional, solo suma, piso cero). Todo porcentual: `t_i/T`, `D/F`.

## 2. Axiomas duros que toda estructura candidata debe poder respetar

**A1 — Normalización.** Rendimiento en `[0, 1+]`. `0` = nada hecho. `1` = compromiso cumplido exacto. `>1` = superávit (territorio Inquebrantable).

**A2 — Piso cero.** `D = 0` ⟹ rendimiento = 0. Nunca hay valores negativos.

**A3 — Cumplimiento exacto = 1.** `D = F` y todos los `t_i = T` ⟹ rendimiento exactamente 1.

**A4 — Monotonía en días.** Manteniendo todo lo demás igual, agregar un día marcado nunca baja el rendimiento.

**A5 — Monotonía en tiempo (compromiso).** En un día de compromiso, más minutos hasta T nunca bajan el rendimiento.

**A7 — Piso cero en zona voluntaria.** El término de la zona voluntaria es siempre ≥ 0, y tiende a 0 cuando el tiempo del día voluntario tiende a 0.

**A9 — Continuidad / sin gates.** Cambios pequeños en los inputs producen cambios pequeños en el rendimiento. Sin saltos. Esto descarta gates y reglas-patch por construcción.

**A10 — Invarianza de escala.** El rendimiento depende SOLO de las razones `t_i/T` y `D/F`, nunca de minutos crudos ni de la magnitud de T o F.

## 3. Los problemas que tu research alimenta

**P1 — (parcial, tu foco en la parte de tiempo trivial)** Un día solo "vale como día" en la medida de
su fracción `t_i/T`: 40 min concentrados (T=30) le ganan a 1+1 min repartidos en dos días. Las marcas
triviales (1 min sobre meta de 30) deben aportar casi nada SIN umbral duro — eso es una curva, no una regla.

**P2 — (tu foco en la parte de superávit)** El superávit (tiempo sobre T en días de compromiso, días
voluntarios sobre F) suma con retornos decrecientes: debe poder empujar el rendimiento claramente
sobre 1 (hasta territorio Inquebrantable) pero sin explotar ni hacer rentable el grinding. Caso
testigo: 3 días extra × 5 min con tarea de 40 min NO es superávit pleno.

## 4. Restricciones absolutas

1. Cero reglas-patch / gates duros — el comportamiento emerge de la matemática.
2. Rango operativo `[0, 1+]`.
3. Frecuencia sobre intensidad, estructural en la fórmula.
4. Zona voluntaria con piso cero (sin términos negativos posibles).
5. Sin compensación total entre dimensiones (ni impermeabilidad total).
6. Todo porcentual (`t_i/T`, `D/F`).
7. Parámetros calibrables explícitos SIN fijar valores.

## 5. Entregable

Escribí **un solo archivo**: `docs/scoring/exploracion-rendimiento-ancla/research-2.md`

Secciones requeridas:
1. **Estructuras encontradas** — cada curva/familia con su forma explícita y propiedades (dónde
   acelera, dónde se aplana, comportamiento en 0 y en ∞, parámetros que controlan el codo).
2. **Dominio de origen** — de dónde viene cada una y qué problema resuelve allá, con referencias.
3. **Mapeo explícito a `D/F` y `t_i/T`** — para CADA estructura: cómo se instanciaría acá (curva del
   día trivial, curva del superávit, o ambas), qué axiomas satisface naturalmente y cuáles exigirían
   adaptación, y qué parámetro controla la velocidad de saturación.
4. **Explicación en lenguaje claro** — qué aprendiste, legible sin notación.

## 6. Reglas de operación

- Documento en español. Búsqueda web PERMITIDA y esperada.
- NO propongas la fórmula final del sistema; tu valor es el vocabulario matemático bien mapeado.
- NO leas otros archivos de `exploracion-rendimiento-ancla/` (salvo este brief).
