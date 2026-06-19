# Brief — Researcher 3: Pesos que se desplazan según un parámetro

> Misión redactada por el orquestador. Sesión: exploración del rendimiento de un ancla (2026-06-09).

## 0. Tu rol y tu ángulo

Sos un researcher matemático. NO proponés la solución final: proveés vocabulario matemático externo,
bien mapeado al problema, para que el orquestador consolide. Podés y debés usar búsqueda web.
NO leas las propuestas de otros subagentes.

**Tu ángulo asignado — pesos que se desplazan según un parámetro:** cómo se modela que el peso
relativo de una dimensión cambie en función de OTRA variable del propio sistema — acá: que el peso
del superávit de tiempo CREZCA con F (la frecuencia objetivo), porque a mayor F quedan menos días
libres para superávit de frecuencia, y en F=7 el tiempo es la única vía de superávit. Esto es el
mecanismo del problema P2. Dominios candidatos (no exhaustivo, no te limites): mixing
weights/interpolaciones dependientes de contexto (gating en mixture-of-experts), pesos adaptativos en
índices compuestos, elasticidades variables, funciones de producción con participación de factores
variable, modelos de votación/poder con peso dependiente del tamaño, schedulers/annealing (pesos que
migran con un parámetro de control), priors jerárquicos cuya influencia escala con n, shrinkage tipo
James-Stein / regularización cuyo factor depende de los grados de libertad disponibles.

## 1. El sistema (lo mínimo que necesitás)

App local de hábitos/consistencia. Motor de pesos puros: `score = Σ(peso_capa × valor_capa)`; el
valor de capa es el promedio de sus anclas. El problema de la sesión: **el VALOR de un ancla**.

**Inputs del ancla:** `F` = días comprometidos por semana (1–7); `T` = minutos objetivo por sesión.
Registro: array de 7 días con minutos (0 = no marcó). Derivados: `D` = días marcados, `t_i` = minutos
de cada día marcado.

**Mecanismo de zonas (Best-F):** de los D días marcados, los F con mayor tiempo = zona de compromiso
(tiempo bidireccional: déficit si `t_i < T`, superávit si `t_i > T`); los D−F restantes = zona
voluntaria (unidireccional, solo suma, piso cero). Todo porcentual: `t_i/T`, `D/F`.

**Dato estructural clave para tu ángulo:** el "espacio de superávit de días" disponible es `7 − F`
días. En F=1 hay 6 días posibles de superávit de frecuencia; en F=7 hay cero. El peso del superávit
de tiempo debería desplazarse en consecuencia — suavemente, sin saltos (axioma A9).

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

**P2 — El superávit de tiempo debe poder alcanzar Inquebrantable por sí solo, y su peso escala con F.**
(TU FOCO PRINCIPAL) Existe un 5º estado, **Inquebrantable**, sobre Pleno; se alcanza con superávit
sobre una base ya completada. En F=7, `D/F` topa en 1: el tiempo es la única vía. El peso relativo
del superávit de tiempo debe CRECER cuando F se acerca a 7 — de forma continua y estructural, no como
un `if`. Esto acota la dominancia de frecuencia: no puede ser tan absoluta que el tiempo no pueda
mover el estado.

**P1 — (contexto, no tu foco)** La frecuencia domina al tiempo sin anularlo; un día solo vale como
día según su fracción `t_i/T`; el déficit de días no se compensa del todo con tiempo alto.

## 4. Restricciones absolutas

1. Cero reglas-patch / gates duros — el comportamiento emerge de la matemática.
2. Rango operativo `[0, 1+]`.
3. Frecuencia sobre intensidad, estructural en la fórmula.
4. Zona voluntaria con piso cero (sin términos negativos posibles).
5. Sin compensación total entre dimensiones (ni impermeabilidad total).
6. Todo porcentual (`t_i/T`, `D/F`).
7. Parámetros calibrables explícitos SIN fijar valores.

## 5. Entregable

Escribí **un solo archivo**: `docs/scoring/exploracion-rendimiento-ancla/research-3.md`

Secciones requeridas:
1. **Estructuras encontradas** — cada mecanismo de peso variable con su forma explícita y propiedades
   (continuidad respecto del parámetro de control, comportamiento en los extremos, normalización de
   los pesos entre sí).
2. **Dominio de origen** — de dónde viene cada uno y qué problema resuelve allá, con referencias.
3. **Mapeo explícito a `D/F` y `t_i/T`** — para CADA estructura: cómo se instanciaría con F (o `7−F`)
   como parámetro de control del peso del superávit de tiempo, qué pasa exactamente en F=7 y en F=1,
   qué axiomas satisface naturalmente y cuáles exigirían adaptación.
4. **Explicación en lenguaje claro** — qué aprendiste, legible sin notación.

## 6. Reglas de operación

- Documento en español. Búsqueda web PERMITIDA y esperada.
- NO propongas la fórmula final del sistema; tu valor es el vocabulario matemático bien mapeado.
- NO leas otros archivos de `exploracion-rendimiento-ancla/` (salvo este brief).
