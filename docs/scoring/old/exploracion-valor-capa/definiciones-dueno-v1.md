# Definiciones del dueño — motor de capa (captura en vivo)

> **Estado: en construcción** — sesión iniciada 2026-06-12. Captura fiel de las definiciones
> del dueño ANTES de formalizar axiomas. Método: igual que el ancla — el dueño define el
> comportamiento buscado → se formalizan axiomas → la matemática hace EMERGER los valores.
> **Ninguna magnitud de docs previos (±0.1 soporte, k_sobr, etc.) es verdad acá.** Los números
> que aparecen son EJEMPLOS ilustrativos del dueño, nunca valores decididos.

---

## Definiciones recibidas (textual-fiel, numeradas)

- **D1.** Orden de importancia: **anclas > soportes > tasks**.
- **D2.** Capas activas: **entre 3 y 5, ni más ni menos**.
- **D3.** Los opt-ins (sueño, sobriedad) **impactan el PESO de su capa directamente**.
  La magnitud exacta NO la fija el dueño: la debe calcular el modelo matemático.
- **D4.** Activar un opt-in cuya capa anfitriona NO estaba activa **activa esa capa**
  (3 elegidas + sueño = 4 activas; puede quedar capa sin anclas). Ídem sobriedad → Conducta.
- **D5.** Prioridad de la sesión: sueño y sobriedad en TODOS los casos límite, antes que soportes/tasks.
- **D6.** *(respuestas 2026-06-12)* **Justificación del peso extra**: sin opt-in todas las capas pesan
  igual; con opt-in la capa pesa más porque es un apartado MÁS SENSIBLE para ese usuario específico
  (insomnio, adicción al alcohol, sustancias, masturbación, etc.).
- **D7.** El scoring mide SOLO los últimos 7 días — para sueño Y sobriedad (cerrado). La info de
  sobriedad (racha larga, historial) vive en su panel aparte; una recaída solo afecta al score si
  ocurrió dentro de los últimos 7 días. Si no, se castigaría constantemente al usuario con sobriedad
  activa y el motor perdería sentido.
- **D8.** La recaída debe pegar MÁS fuerte en el scoring que una mala semana de sueño.

## Respuestas del dueño a los bloques (2026-06-12)

| # | Pregunta | Respuesta del dueño |
|---|----------|---------------------|
| 1 | ¿Opt-in entra al valor además del peso? | SÍ, ambos. Idea inicial (NO tomar como decisión): peso interno dentro de la capa (ej. ancla 0.4 / sueño 0.6); sin ancla, sueño = 1.0 de la capa. Detectó él mismo el problema de normalización: ancla va 0–1.5. **Pide propuesta más inteligente del agente.** |
| 2 | ¿Dato malo hunde el valor? | SÍ. Recaída = binaria; sueño = continuo desde telemetría (ver hallazgos abajo). |
| 3 | ¿Opt-in sin dato? | Sueño: poner una BASE para no tirar todo al piso (la telemetría puede fallar; es complejo). Sobriedad: YA DEFINIDO — ventana de 5 días para registrar días faltantes (ver hallazgos). |
| 4 | ¿Capa sin anclas + opt-in pesa? | MÁS que las demás (opt-in es sensible). |
| 5 | ¿Su valor = señal del opt-in? | SÍ. |
| 6 | ¿Exporta superávit (>1)? | NO. ⚠️ Conflicto detectado por el dueño: ¿cómo promedia el superávit real de las demás anclas si esta capa no puede tenerlo? ¿Y si la capa anfitriona SÍ tiene ancla? **Pide análisis del agente.** |
| 7 | ¿Agregar ancla cambia el comportamiento? | SÍ — el CÓMO queda a definir. |
| 8 | ¿Ambos opt-ins + 3 capas sin anfitrionas? | **Pide ayuda para pensarlo.** |
| 9 | ¿Los dos refuerzos conviven o se limitan? | No entendió la pregunta — re-explicar. |
| 10 | ¿Recaída pega más que mal sueño? | SÍ (= D8), con la aclaración D7 (ventana 7d). |
| 11 | ¿Multi-sobriedad? | **Pide ideas.** |
| 12 | ¿Desactivar opt-in? | La capa vuelve a peso normal; si hay ancla activa, ocupa todo el peso de la capa. |

## Hallazgos de código/docs (verificados 2026-06-12)

### Señal de sueño (la real, `domain/sleep/SleepScoring.kt`)
- **Por NOCHE** (no por semana): `SleepNightScore = 0.40·Duración + 0.25·Continuidad +
  0.20·Alineación + 0.15·InterrupciónDigital`, pesos SELLADOS. Ya implementa los 4 componentes.
- Dormir MÁS que el objetivo = 1.0 neutral (sin decay). La señal **no puede superar 1** por diseño.
- `NoData` → **null** (no 0; ADR-3, "propagar ausencia"). `Ambiguous` → score atenuado por factor.
- ⚠️ **GAP**: la agregación de 7 scores nocturnos (algunos null) → señal SEMANAL `M` no está
  definida. Es parte de lo que el motor de capa debe definir (¿promedio? ¿cobertura? ¿base?).

### Sobriedad — ventana de 5 días (encontrada)
- `AbstinenceRelapseMaterializationPolicy.kt`: `MISSING_TRACKING_WINDOW_DAYS = 5`. Días sin log
  (o `Unknown`) MÁS VIEJOS que 5 días se materializan como **recaída asumida** (rangos), corregible.
- `docs/scoring/arbol-scoring-v1.md` decisiones 11–12: "Sobriedad pendiente usa ventana de 5 días.
  Recaída asumida penaliza como manual hasta corrección."
- Consecuencia para el motor: dentro de la ventana, no-marcado ≠ recaída (perdón); fuera, el
  pipeline de hechos ya entrega held/broke materializado. El motor de capa CONSUME eso.

## Propuestas del agente (candidatas — NADA decidido)

### P-A. Composición valor de capa: cimiento multiplicativo (candidata principal)
`valor_capa = M^a · promedio(R_anclas)`, con `promedio(R) ≡ 1` si no hay anclas.
- Sin anclas → `valor = M^a` (≈ M): cumple R5 y el tope 1 de R6 EMERGE (M ≤ 1 por diseño).
- Con anclas y señal buena (M=1) → el superávit de anclas PASA COMPLETO (resuelve el conflicto R6:
  el cimiento habilita, las anclas logran).
- Señal mala → hunde multiplicativamente TODO el valor (cumple R2); la señal nunca infla (M≤1).
- Asimetría estructural sin reglas — mismo espíritu que `base^p` en el ancla.
- `a` = severidad calibrable.
- Transición R7 (agregar ancla): la forma no cambia (`promedio` pasa de vacío≡1 a R_1).
- Contra: ¿mal sueño moderado (M=0.6) × anclas perfectas = 0.6 es demasiado duro? → calibración (a, forma de M).

### P-B. Alternativa: blend convexo `γ·M + (1−γ)·avg(R)`
- Simétrico: amortigua tanto la caída como el superávit ((1−γ)·σ_max de techo extra).
- Sin anclas: γ→1. Más "suave", pero el superávit de anclas queda PERMANENTEMENTE recortado
  en capas con opt-in, incluso con señal perfecta.

### P-C. Señal semanal M con cobertura (caso sin dato, R3-sueño)
`M = c·avg(noches con dato) + (1−c)·m_base`, con `c = noches_con_dato/7` y `m_base` = base
calibrable ("no tirar todo al piso"). Sin registro alguno → M = m_base. Emergente, sin regla de tope.

### P-D. Multi-sobriedad multiplicativa (R11)
`M_sobr = Π m_track` (held=1, broke=b calibrable). 1 recaída hunde igual con 1 o 5 tracks activos
(sin dilución por promedio); 2 recaídas hunden más. Evita el `min()` (worst-term). Pregunta
abierta: ¿más tracks activos también suben (un poco, saturando) el peso de Conducta?

### P-E. Re-explicación R9 (convivencia de refuerzos)
Con renormalización (Forma A del motor), los dos boosts se limitan mutuamente SOLOS: si Cuerpo ×k_s
y Conducta ×k_b, cada peso final = su boost / (suma total). Activar el segundo opt-in reduce
automáticamente la porción del primero. No hace falta decidir nada extra: es emergente.
La pregunta real para el dueño era si ESO le parece bien (cada capa boosteada pesa menos que si
fuera el único opt-in) — la aritmética dice que sí pasa.

## Tensión cuantificada (para que el dueño la marque)

Caso R8 (Interior+Vínculos+Proyecto + ambos opt-ins, sin anclas en Cuerpo/Conducta), con boosts
ILUSTRATIVOS (k_s=1.5, k_b=3.0): pesos → Conducta 40%, Cuerpo 20%, I/V/P 13.3% c/u.
- Las 3 áreas ELEGIDAS por el usuario = 40% del score; los 2 opt-ins (capados en 1) = 60%.
- Inquebrantable (δ=0.10): las 3 capas con anclas deben promediar ≈1.25 — duro pero alcanzable (techo 1.5).
- A marcar por el dueño: ¿está bien que los opt-ins dominen el score? ¿O el boost de una capa
  SIN anclas debería ser menor que el de una capa anfitriona con anclas (señal única vs capa completa)?

## Objeciones del dueño a las propuestas P-A/P-B (2026-06-12) — VÁLIDAS

1. **El cimiento multiplicativo (P-A) es demasiado penalizante**: con M<1, `M^a · R` come el
   superávit ganado con esfuerzo. Un sueño regular borra el mérito del ancla. Rechazado.
2. **No resuelve el superhabit, lo empeora**: una capa capada en 1 (solo-opt-in) ocupa peso en el
   promedio global y BAJA el techo de Inquebrantable. Nunca se especificó si el superávit se promedia
   entre TODAS las capas o solo entre las que pueden generarlo. Hueco real.
3. **Capa con ancla + opt-in**: no estaba definido cómo sobrevive su superávit. El superhabit es
   emergente del promedio de todas las capas — meter capas que no pueden superar 1 rompe esa lógica.

## Research (2026-06-12) — análogos del mundo real al problema de "compensabilidad"

- **HDI (ONU, 2010)**: cambió media aritmética → geométrica para que una dimensión mala no se tape
  con una buena (= la "pata coja de la mesa" del producto). PERO la geométrica es MÁS penalizante
  que P-A → contradice la objeción 1 del dueño. **Descartada como dirección.**
- **Ley del mínimo de Liebig**: el resultado lo dicta el recurso más escaso (worst-term puro).
  Ya rechazado por el dueño en el motor. **Descartada.**
- **Agregación de dos niveles (composite indicators no-compensatorios)**: la idea útil — *permitir
  compensación DENTRO de un aspecto, impedirla ENTRE aspectos*. Fundamenta la propuesta P-F.

Fuentes: HDR/UNDP (hdr.undp.org/content/improving-measurement-human-development),
Springer "Aggregating the HDI: A Non-compensatory Approach", JRC "Non-Compensatory Composite
Indicators", Wikipedia "Liebig's law of the minimum".

### P-F. DOS CANALES SEPARADOS: base + extra (candidata principal nueva)

El score deja de meter todo en un número por capa. Se calcula en dos canales:

- **Canal BASE ("¿está todo en pie?")** ∈ [0,1]: por capa, el cumplimiento topado en 1 (anclas
  hasta su meta + señal del opt-in). Participan TODAS las capas activas (incluidas las solo-opt-in).
  El sueño/sobriedad pesan fuerte acá. Techo 1 — dormir extra bien no sube la base.
- **Canal EXTRA ("¿te destacaste?")** ≥ 0: el excedente sobre la meta, **SOLO de las anclas**.
  Se agrega únicamente entre las capas que tienen anclas (responde la pregunta del dueño:
  la división del superávit es entre capas que PUEDEN tenerlo, no entre todas). Sueño/sobriedad
  no generan extra (señal topada en 1 por diseño del código).
- **Estado = base + extra** (ponderación a definir). Inquebrantable = base llena (todas las áreas
  firmes, incl. buen sueño) + extra suficiente repartido en las anclas.

Resuelve las 3 objeciones:
1. Mal sueño baja la BASE pero NO toca el extra ganado → menos penalizante, el mérito sobrevive.
2. Capa solo-opt-in entra a la base pero NO a la cuenta del extra → no baja el techo de Inquebrantable.
3. Capa ancla+opt-in: extra = excedente del ancla; opt-in juega en la base de esa capa, no en su extra.

**Decisiones que P-F deja abiertas para el dueño:**
- ¿Cómo pondera base vs extra en el estado final?
- En una capa ancla+opt-in, ¿el opt-in malo recorta ALGO del extra de esa capa, o cero?
- ¿El extra se promedia o se suma entre las capas con anclas? (cobertura vs acumulación)
- El boost de PESO de la capa por opt-in (D3) ¿sigue existiendo encima de esto, o el doble canal
  ya hace que el opt-in "pese" sin tocar pesos?

### Fórmulas P-F explícitas (ilustrativas — w y agregación SIN decidir)

Por capa, dos canales:
```
en_pie(capa)   ∈ [0,1]
  - capa solo-anclas:   promedio_i  min(R_i, 1)
  - capa con opt-in:    w · M  +  (1−w) · promedio_i min(R_i, 1)
                        (si no hay anclas: en_pie = M)
destaco(capa)  ≥ 0
  - max(R_i − 1, 0) agregado entre anclas; el opt-in NO aporta extra
```
Global:
```
ESTADO = promedio_capas(en_pie)  +  AGG_capas-con-ancla(destaco)
bandas: R<0.40 · A<0.62 · EM<0.85 · P<1.10 · I≥1.10  (δ=0.10, ilustrativo)
```
Variables:
- `R_i` = rendimiento del ancla i (fórmula consolidada, ∈[0,1.5]). REAL.
- `M` = señal de sueño/sobriedad semanal ∈[0,1]. Sueño: SleepScoring (4 comp, por noche →
  agregación semanal NO definida = gap). Magnitud del ejemplo (0.35) ilustrativa.
- `w` = peso del opt-in dentro de su capa. **INVENTADO (0.5 en el ejemplo) — a definir/calibrar.**
- `AGG` = promedio o suma del extra entre capas-con-ancla. **A decidir.**

**Validación numérica (script corrido 2026-06-12, params ilustrativos del ancla):**
- Caro (leer4·caminar6·orden4·sueño malo 0.35), w=0.5, pesos de capa iguales:
  en_pie=0.89 + destaco=0.09 = **0.98 → Plenitud**. Durmió bien → 1.09 (Plenitud).
- Forma vieja multiplicativa (rechazada): sueño malo → 0.81 (En marcha): borra el esfuerzo.
- Prueba de que las 3 capas cuentan: bajar solo Interior (leer 2/4) → 0.81 (En marcha);
  bajar Interior+Conducta → 0.65 (En marcha). El estado responde a TODAS las capas.

**Pendiente #2 del sueño (D3) NO incluido aún en los números:** el boost de PESO de la capa
(Cuerpo pesa más que las otras) es una segunda perilla, separada de `w`. El ejemplo usa pesos iguales.

## CERRADO en esta sesión (2026-06-12) — axiomas confirmados por el dueño

- **EJE — CORREGIDO (2026-06-12):** cumplir todo JUSTO (cada capa = 1.0) = **score 1.0 = PLENITUD**,
  consistente con el ancla y `mapa-modelo-scoring-v1.md` §1 (`P≥0.85`) y `simulacion-capas-resultados.md`
  (EXACTO=1.0000, "Pleno exacto"). **En marcha (0.62–0.85) es cumplimiento PARCIAL**, no cumplir todo.
  Superhabit repartido → >1.0 → Inquebrantable (≥1.10). Bandas del mapa: `R<0.40·A<0.62·EM<0.85·P≥0.85·I≥1.10`.
  > ⚠️ ERRATA: una versión anterior de este doc decía "cumplir justo = En marcha" con un factor
  > `EM_TOP=0.85` que comprimía la escala. **Eso fue un error del agente** (mala interpretación de un
  > "en marcha" coloquial del dueño) y CONTRADECÍA el modelo del ancla. Queda anulado.
- **DOS CANALES (P-F) aprobado.** base ∈[0,1] (todas las capas, opt-in incluido) + extra ≥0 (solo anclas).
- **Pesos por UN solo K por opt-in, TODO relacional, NADA fijo** (el "B fijo" fue un error, rechazado):
  - tamaño(capa) = 1 (bloque de anclas, promediadas) + K (si tiene opt-in). peso = tamaño/Σ.
  - Peso de la capa en el total: RELACIONAL a N (baja al crecer N: 60%→50%→43% con K=2). Igual que Forma A.
  - Peso del opt-in dentro de la capa: K/(1+K) respecto al bloque de anclas.
  - K se DESPEJA de un axioma de estado del dueño (ej. "anclas perfectas + sin dormir = tal estado"),
    NO se elige a dedo. Mismo método que la calibración del ancla.
- **El nº de anclas NO cambia el peso de la capa** (las anclas promedian a un bloque). Confirmado.

## DECISIÓN ABIERTA (única que quedó sobre opt-ins)

- Reparto interno con más anclas: ¿el opt-in mantiene su peso K/(1+K) (anclas = bloque, respeta la regla)
  o se diluye con la cantidad de anclas (K/(n+K))? Pendiente.

## Pendiente de dictado del dueño

- Soportes: comportamiento buscado dentro de la capa (¿base o extra? ¿saturación multi-soporte?).
- Tasks: canal de aporte (¿base o extra? aportan menos que soportes; las neutras no suman, por dominio).
- Micro-pregunta: dentro de la ventana de 7 días, ¿1 día de recaída vs 3 días pesan igual o gradúan?
- Multi-sobriedad (alcohol+tabaco): tracks separados / agregados / peor manda.

## Sesión multi-agente lanzada (2026-06-12)

3 proponentes Opus a ciegas (sesgos: A aditivo · B cimiento modulador · C unificación relacional)
+ research del orquestador → consolidación en `modelo-valor-capa-consolidado-v1.md` con tests Python.
