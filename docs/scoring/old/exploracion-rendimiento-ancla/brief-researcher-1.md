# Brief — Researcher 1: Normalización de dos métricas acopladas

> Misión redactada por el orquestador. Sesión: exploración del rendimiento de un ancla (2026-06-09).

## 0. Tu rol y tu ángulo

Sos un researcher matemático. NO proponés la solución final: proveés vocabulario matemático externo,
bien mapeado al problema, para que el orquestador consolide. Podés y debés usar búsqueda web.
NO leas las propuestas de otros subagentes.

**Tu ángulo asignado — normalización de dos métricas acopladas:** cómo otros dominios combinan dos
cantidades normalizadas donde una debe DOMINAR a la otra sin anularla. Este es el mecanismo del
problema P1 (frecuencia domina al tiempo, pero un día solo vale como día según su fracción de tiempo).
Dominios candidatos (no exhaustivo, no te limites): medias generalizadas/potencia y sus pesos,
funciones de producción CES y Cobb-Douglas (elasticidad de sustitución = exactamente "cuánto compensa
una dimensión a la otra"), F-beta score (precision/recall con dominancia regulable), agregadores
difusos (t-normas, OWA), índices compuestos tipo HDI (media geométrica para evitar compensación
total), funciones de utilidad con complementariedad.

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

**P1 — Normalización porcentual frecuencia ↔ tiempo.** (TU FOCO PRINCIPAL)
La frecuencia debe dominar al tiempo, PERO un día con tiempo trivial no debe activar esa dominancia:
un día solo "vale como día" en la medida de su fracción `t_i/T`. Ejemplo testigo: 40 min concentrados
en un día (T=30) deben ganarle a 1+1 min repartidos en dos días, sin destruir la regla general de que
la constancia importa más que la intensidad. Quedan dentro de P1: la fuerza exacta de la dominancia,
si superávit de tiempo y superávit de día comparten bolsa o son canales distintos, y el comportamiento
del déficit de frecuencia (`D < F`): mucho tiempo no compensa días faltantes pero sí aporta — sin techo duro.

**P2 — (contexto, no tu foco)** El superávit de tiempo debe poder alcanzar Inquebrantable por sí solo
y su peso escala con F (en F=7 no hay superávit de días posible).

## 4. Restricciones absolutas

1. Cero reglas-patch / gates duros — el comportamiento emerge de la matemática.
2. Rango operativo `[0, 1+]`.
3. Frecuencia sobre intensidad, estructural en la fórmula.
4. Zona voluntaria con piso cero (sin términos negativos posibles).
5. Sin compensación total entre dimensiones (ni impermeabilidad total).
6. Todo porcentual (`t_i/T`, `D/F`).
7. Parámetros calibrables explícitos SIN fijar valores.

## 5. Entregable

Escribí **un solo archivo**: `docs/scoring/exploracion-rendimiento-ancla/research-1.md`

Secciones requeridas:
1. **Estructuras encontradas** — cada estructura matemática con su forma explícita y propiedades
   (monotonía, continuidad, comportamiento en bordes, cómo regula la dominancia/sustitución).
2. **Dominio de origen** — de dónde viene cada una (economía, ML, teoría de decisión, etc.) y qué
   problema resuelve allá, con referencias.
3. **Mapeo explícito a `D/F` y `t_i/T`** — para CADA estructura: cómo se instanciaría con las
   variables de este problema, qué axiomas satisface naturalmente y cuáles exigirían adaptación,
   y qué parámetro controla la dominancia frecuencia↔tiempo.
4. **Explicación en lenguaje claro** — qué aprendiste, legible sin notación.

## 6. Reglas de operación

- Documento en español. Búsqueda web PERMITIDA y esperada.
- NO propongas la fórmula final del sistema; tu valor es el vocabulario matemático bien mapeado.
- NO leas otros archivos de `exploracion-rendimiento-ancla/` (salvo este brief).
