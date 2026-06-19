# Brief — Proponente C: Saturación / retornos decrecientes

> Misión redactada por el orquestador. Sesión: exploración del rendimiento de un ancla (2026-06-09).

## 0. Tu rol y tu sesgo

Sos un proponente matemático independiente. Derivás UNA fórmula completa para el valor de un ancla,
desde los axiomas, usando SOLO tu conocimiento matemático interno. Sin búsqueda web. Sin leer las
propuestas de otros subagentes. Sin leer el código viejo del repo como fuente de verdad.

**Tu sesgo asignado — saturación / retornos decrecientes:** partís de la familia de funciones que
crecen rápido y se aplanan (cóncavas, exponenciales saturantes, racionales tipo Hill/Michaelis-Menten,
potencias < 1): los primeros minutos valen mucho, los extra valen cada vez menos. Esta familia es
natural para que un día trivial valga poco y para que el superávit no explote.
**Atención crítica:** la saturación no puede ser tan agresiva que mate el superávit — P2 exige que el
superávit de tiempo pueda, por sí solo, alcanzar Inquebrantable (especialmente en F=7). Si tu techo
asintótico deja el rendimiento clavado apenas arriba de 1, tu propuesta falla. Y la concavidad en la
zona baja no puede regalar valor a marcas triviales (1 min sobre T=30 debe valer casi nada).

## 1. El sistema (lo mínimo que necesitás)

App local de seguimiento de hábitos y consistencia personal. El score global refleja qué tan bien una
persona sostiene sus compromisos semanales en áreas de vida (capas). El motor es de pesos puros:
`score = Σ(peso_capa × valor_capa)`, y el valor de una capa es el **promedio de sus anclas**. Los
pesos de capa están CERRADOS. **El problema es uno solo: cómo se calcula el VALOR de un ancla.**

## 2. Inputs de un ancla

**Configurados por el usuario al crear el ancla:**
- `F` = frecuencia objetivo (días comprometidos en una ventana de 7 días). Rango 1–7. Ej: 3
- `T` = tiempo objetivo por sesión (minutos). Ej: 30

**Registrados por la app en la ventana móvil de 7 días:**
- Array de 7 entradas. Cada día: `0` si no marcó, o `N` minutos si marcó.

**Derivados del array:**
- `D` = cantidad de días marcados (entradas > 0)
- `t_i` = lista de minutos de cada día marcado (longitud = D)

El motor solo ve los últimos 7 días. La racha larga (meses) NO entra al scoring.

## 3. El mecanismo de zonas (núcleo del problema)

El array de marcas es plano: no etiqueta días "obligatorios". Para asignar obligatoriedad se usa
**Best-F assignment**:

> De los D días marcados, los **F con mayor tiempo** forman la **zona de compromiso**.
> Los **D−F días restantes** (los de menor tiempo) son la **zona voluntaria**.

**Zona de compromiso (los F mejores días):** tiempo **bidireccional**.
`t_i < T` → déficit de tiempo ese día. `t_i > T` → superávit de tiempo ese día.

**Zona voluntaria (los D−F restantes, si D > F):** tiempo **unidireccional**: solo suma, piso cero.
Un día voluntario con tiempo bajo contribuye poco, pero NUNCA resta.

**La única intersección donde algo puede restar:** día de compromiso × tiempo por debajo de T.

**Todo es porcentual respecto a la configuración del usuario** (`t_i/T`, `D/F`), nunca absoluto:
30 min sobre meta de 120 es superávit considerable; 1 min sobre meta de 30 es casi nada.

## 4. Axiomas duros (fijados, no negociables — verificá tu fórmula contra cada uno)

**A1 — Normalización.** Rendimiento en `[0, 1+]`. `0` = nada hecho. `1` = compromiso cumplido exacto. `>1` = superávit (territorio Inquebrantable).

**A2 — Piso cero.** `D = 0` ⟹ rendimiento = 0. Nunca hay valores negativos.

**A3 — Cumplimiento exacto = 1.** `D = F` y todos los `t_i = T` ⟹ rendimiento exactamente 1.

**A4 — Monotonía en días.** Manteniendo todo lo demás igual, agregar un día marcado nunca baja el rendimiento.

**A5 — Monotonía en tiempo (compromiso).** En un día de compromiso, más minutos hasta T nunca bajan el rendimiento.

**A7 — Piso cero en zona voluntaria.** El término de la zona voluntaria es siempre ≥ 0, y tiende a 0 cuando el tiempo del día voluntario tiende a 0.

**A9 — Continuidad / sin gates.** Cambios pequeños en los inputs producen cambios pequeños en el rendimiento. Sin saltos. Esto descarta gates y reglas-patch por construcción.

**A10 — Invarianza de escala.** El rendimiento depende SOLO de las razones `t_i/T` y `D/F`, nunca de minutos crudos ni de la magnitud de T o F. Duplicar T y todos los `t_i` deja el rendimiento idéntico. Aplica por igual a tiempo y días.

## 5. Problemas abiertos que tu modelo RESUELVE (no se asumen — emergen de la estructura)

**P1 — Normalización porcentual frecuencia ↔ tiempo.**
La frecuencia debe dominar al tiempo, PERO un día con tiempo trivial no debe activar esa dominancia:
un día solo "vale como día" en la medida de su fracción `t_i/T`. El modelo debe lograr que, por
ejemplo, 40 min concentrados en un día (T=30) le ganen a 1+1 min repartidos en dos días, sin destruir
la regla general de que la constancia importa más que la intensidad.
Dentro de P1 quedan absorbidos, y NO se fijan a mano:
- la fuerza exacta de la dominancia frecuencia/tiempo,
- si el superávit de tiempo (compromiso) y el superávit de día (voluntario) comparten una sola bolsa
  o son canales distintos,
- cómo se comporta el déficit de frecuencia (`D < F`): no hay techo duro — el comportamiento emerge.
  Mucho tiempo no compensa días faltantes, pero sí aporta por su cuenta; hasta cuánto, lo resuelve el modelo.

**P2 — El superávit de tiempo debe poder alcanzar Inquebrantable por sí solo, y su peso escala con F.**
Existe un 5º estado, **Inquebrantable**, por encima de Pleno. Se alcanza con superávit sobre una base
ya completada. Cuando `F = 7` no existe superávit de días posible (`D/F` topa en 1): el tiempo es la
única vía a Inquebrantable. El peso relativo del superávit de tiempo debe CRECER a medida que F se
acerca a 7. Todo relativo a la configuración del usuario.

## 6. Estados y rango operativo

Cinco estados. Bandas actuales (4 primeras, con ~10% de juego):
`Rojo < 0.40 · Amarillo < 0.62 · En marcha < 0.85 · Pleno ≥ 0.85`.
El 5º, **Inquebrantable**, está sobre Pleno y requiere superávit sobre base completada (P2). Cómo se
traduce al score (¿banda nueva? ¿condición compuesta?) es parte del problema. Rango operativo `[0, 1+]`.

## 7. Casos límite que tu fórmula debe manejar bien (autoverificá CADA uno con números)

| Caso | Setup | Comportamiento esperado |
|------|-------|------------------------|
| Cumplimiento exacto | D=F, todos t_i=T | Rendimiento = 1 |
| Nada hecho | D=0 | Rendimiento = 0 |
| Superávit días + déficit tiempo | F=3, T=30, t_i=[10,10,10,90,90] | Los 3 mejores (90,90,10) = compromiso (uno con déficit). Los 2 de 10 = voluntarios. El día de 90 NO tapa los déficits de los días de 10. |
| Déficit de frecuencia + tiempo alto | F=5, T=20, D=2, t_i=[60,60] | Déficit de frecuencia dominante. Tiempo alto aporta pero NO compensa los 3 días faltantes. |
| 40 min vs 1+1 min | A: días cumplidos + 1 día de 40 (T=30). B: días cumplidos + 2 días de 1 min | A debe valer más que B. 2 minutos repartidos no ganan a 10 min concentrados por estar en más días. |
| Voluntario trivial | F=2, T=30, D=5, t_i=[30,30,1,1,1] | Los 2 de 30 = compromiso perfecto. Los 3 de 1 min = voluntarios que aportan casi nada. |
| Déficit puro de tiempo | F=2, T=5, D=7, t_i=[1,1,1,1,1,1,1] | Los 2 mejores = compromiso con déficit de tiempo. Los 5 restantes = voluntarios que aportan poco. |
| Saturación F=7 (vía P2) | F=7, sin días extra posibles | El superávit de tiempo solo debe poder empujar hasta Inquebrantable. |
| Invarianza de escala (vía A10) | (F=3,T=30,t) vs (F=3,T=120,t×4) | Rendimiento idéntico. |

## 8. Restricciones absolutas

1. **Cero reglas-patch / gates duros.** Nada de "si X entonces colapsa/fuerza Y". El comportamiento emerge de la matemática.
2. **Rango operativo `[0, 1+]`.** El superávit puede superar 1; el modelo define cuánto y cómo.
3. **Frecuencia sobre intensidad** debe ser estructural en la fórmula, no un parámetro suelto.
4. **Zona voluntaria con piso cero** — matemáticamente no puede producir un término negativo.
5. **Sin compensación total** entre dimensiones: el déficit de días no se anula por tiempo alto, pero tampoco es impermeable. El punto medio lo encuentra el modelo.
6. **Todo porcentual** (`t_i/T`, `D/F`) — invarianza de escala (A10) en ambas dimensiones.
7. **Parámetros calibrables explícitos** (α, β, etc.) SIN fijar valores — la calibración viene después contra el dataset de marcas de estado.

## 9. Contexto del proyecto destilado (lo que importa para tu rol)

- Filosofía: la app mide "qué tanto sostiene el usuario su base configurada". Estar bajo es una señal,
  no una condena. La meta del usuario es una hipótesis ajustable: el déficit resta proporcional y
  suave, nunca castiga moralmente. "En marcha" es el hogar operativo; Pleno/Inquebrantable son picos
  orgánicos, no obligación diaria.
- Decisión cerrada del producto: la app premia CONSTANCIA (días) por encima de ráfagas de
  productividad de 1 día (tiempo). Tus retornos decrecientes deben servirla, no diluirla.
- El motor viejo del código usaba `0.70·días + 0.30·tiempo` con gates duros (`UNBREAKABLE_*`,
  worst-term). Está descartado por diseño: NO lo uses como punto de partida ni como referencia.
- Tu salida alimenta el promedio de la capa: un ancla en `[0, 1+]` se promedia con sus hermanas.
- Pista de producto para tu sesgo: el caso testigo histórico del dueño es "3 días extra × 5 min con
  tarea de 40 min NO es superhabit pleno" — exactamente el tipo de comportamiento que una saturación
  bien puesta hace emerger sin reglas.

## 10. Entregable

Escribí **un solo archivo**: `docs/scoring/exploracion-rendimiento-ancla/subagente-C-propuesta.md`

Secciones requeridas (en este orden):
1. **Sesgo asignado** — cómo interpretaste saturación / retornos decrecientes.
2. **Fórmula explícita** — la fórmula completa, con todas sus piezas definidas y parámetros calibrables nombrados (sin fijar valores; podés sugerir rangos plausibles).
3. **Derivación desde los axiomas** — por qué cada pieza tiene la forma que tiene.
4. **Autoverificación** — caso por caso: los 8 axiomas (A1–A5, A7, A9, A10) Y cada fila de la tabla de casos límite de §7, **con números calculados** (elegí valores plausibles para los parámetros solo a efectos de verificar; marcalos como ilustrativos).
5. **Explicación en lenguaje claro** — por qué llegaste a esa forma, legible sin notación matemática.

## 11. Reglas de operación

- Documento en español. Notación matemática clara (LaTeX-like o ASCII, consistente).
- NO uses búsqueda web. NO leas otros archivos de `exploracion-rendimiento-ancla/`. NO leas el código del repo.
- Si encontrás una tensión real entre axiomas y tu sesgo, documentala honestamente en la propuesta — no la escondas.
