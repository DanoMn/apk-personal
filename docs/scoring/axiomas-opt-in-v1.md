# Axiomas de comportamiento de los OPT-INS (sueño · sobriedad) — v1

> ⚠ **NOTA DE GENERALIZACIÓN (2026-06-16):** este doc describe el opt-in en el modelo v4 con **pesos de
> capa IGUALES** (`w = BETA·N·(1−M)`, "1/N"). El modelo vigente usa **peso de capa VARIABLE**, donde el
> opt-in generaliza a **`w = BETA·Σpesos·(1−M)`** (`Σpesos=N` si los pesos son iguales → este doc es el
> caso particular). Fuente de verdad: `modelo-matematico-nucleo-v1.md` §4 + `modelo-scoring-oficial-v1.md`.
> Los axiomas de COMPORTAMIENTO (O1–O12) siguen vigentes; solo O13 ("1/N") quedó superado por el peso variable.

> **Estado: vivo — contrato canónico.** Define el comportamiento que el motor de scoring DEBE
> cumplir para los opt-ins (sueño en Cuerpo, sobriedad en Conducta). Equivalente, para los opt-ins,
> al contrato A1–A10 del ancla (`old/exploracion-rendimiento-ancla/merge-consolidado.md`).
> Fecha de cierre: 2026-06-12. Modelo que los implementa: **v4**
> (`old/exploracion-valor-capa/merge-arrastre-optin-consolidado.md` + `modelo_valor_capa_v4_merge.py`).
> Cada axioma lleva su verificación numérica (script reproducible).

---

## Contexto en una frase

Un opt-in es una señal `M ∈ [0,1]` (sueño: continua; sobriedad: por días de recaída) que vive en su
capa y **arrastra el score hacia abajo cuando está mal, sin tocar lo que valen las anclas ni el
superhabit, y sin subir nunca el score cuando está bien**. Entra como un **término-sombra de peso
dinámico** `w = BETA·N·(1−M)` en el canal base; `N` = nº capas activas, `BETA=0.818`, `W0=1`.

---

## Grupo 1 — Cómo entra al score

### O1 — El opt-in solo toca la BASE, nunca el EXTRA
El opt-in afecta el canal base ("¿está en pie?"), **jamás** el canal extra (superhabit). No genera
superhabit (su señal está topada en 1 por diseño).
- **Por qué:** dormir/no-recaer es la *base*, no un logro que te destaca. La gloria (Plenitud/
  Inquebrantable) se gana con el esfuerzo de las anclas, no con el cimiento.
- **Mecanismo:** `extra_global` se calcula solo con `max(R_i−1,0)` de las anclas; el opt-in no aparece.
- **Verificación:** capa solo-opt-in → `extra=0`; superhabit repartido = Inquebrantable solo por anclas.

### O2 — Opt-in BIEN = neutro exacto (incluso con anclas en déficit)
Con señal `M=1`, el opt-in es invisible: el score es idéntico a no tenerlo, aun si las anclas de otras
capas están en déficit.
- **Por qué:** un cuidado sostenido no debe "regalar" puntos ni mover el estado; solo evita la caída.
- **Mecanismo:** `w = BETA·N·(1−M)`, con `M=1 → w=0` → el término-sombra desaparece del promedio.
- **Verificación:** Interior en déficit (0.75) + sueño bien → `0.9167 = 0.9167` (sin opt-in). ✅

### O3 — Opt-in MAL arrastra fuerte, y plano en N
El mal estado del opt-in baja el score con fuerza, y ese arrastre **no se diluye** al haber más capas.
- **Por qué:** el sueño/sobriedad son base sensible; su deterioro debe verse, tenga el usuario 3 o 5
  áreas. (El "techo 1/N" del modelo viejo diluía el golpe — eliminado.)
- **Mecanismo:** el peso de la sombra escala con `N` (`w=BETA·N·(1−M)`) → con anclas perfectas,
  `base = 1/(1+BETA)` **independiente de N**.
- **Verificación:** recaída total → **0.55** idéntico de N=3 a N=7. ✅

### O4 — El opt-in NO mata el valor de las anclas de su capa
Las anclas de la capa con opt-in conservan su valor completo; el opt-in no las arrincona.
- **Por qué:** caminar sigue contando aunque duermas mal — son cosas distintas.
- **Mecanismo:** el opt-in es un **término aparte** en el promedio, no se mezcla dentro del bloque de
  anclas (a diferencia del viejo `K_INT` que las llevaba al 6%).
- **Verificación:** `anchor_base` de Cuerpo = 1.000 con 1 o 3 anclas, aunque el sueño esté en el piso. ✅

### O5 — El opt-in NO distorsiona el superhabit
Un superhabit rinde lo mismo en cualquier capa, tenga opt-in o no.
- **Por qué:** el mérito de destacarse no debe depender de un opt-in ajeno.
- **Mecanismo:** `extra_global` se agrega con **pesos iguales** (no por el peso de la capa).
- **Verificación:** Sol (superhabit en Interior) = Tin (superhabit en Cuerpo+sueño) = **1.144** exacto. ✅

### O13 — El opt-in NO sube el peso de la capa en el total
Todas las capas activas pesan igual (`1/N`); el opt-in no infla el peso de su capa.
- **Por qué:** inflar el peso de capa distorsionaba el superhabit (rareza rechazada). El arrastre viene
  de la sombra dinámica, no del peso de capa.
- **Mecanismo:** pesos de capa iguales; el opt-in agrega masa **solo** vía su término-sombra.
- **Verificación:** quitar el peso de capa por opt-in eliminó la rareza "superhabit vale más en la capa
  pesada".

---

## Grupo 2 — Cómo se combinan y se miden

### O6 — Dos opt-ins malos COMPONEN su arrastre (sin tope)
Mal sueño **y** recaída la misma semana pegan más que cualquiera solo. Sin tope.
- **Por qué:** dos bases (física y conductual) caídas a la vez es objetivamente peor — el patrón de
  bola de nieve que la app quiere detectar.
- **Mecanismo:** cada opt-in es su propia sombra; ambas suman masa al denominador. `base = 1/(1+2·BETA)`
  con ambos en piso.
- **Verificación:** 1 opt-in en piso → Atención (0.55); ambos en piso → Restauración (0.381). Casos
  realistas (mal sueño + recaída 2–3d) → Atención. ✅

### O7 — Sueño y sobriedad pegan IGUAL (mismo BETA)
La sobriedad no se castiga más fuerte que el sueño.
- **Por qué:** decisión humana/compasiva del dueño — no desmotivar al usuario más sensible (insomnio/
  adicción). Técnicamente la recaída ya pega un poco más porque su señal cae a 0 más rápido, pero el
  parámetro de intensidad es el mismo.
- **Mecanismo:** mismo `BETA=0.818` para ambos.

### O8 — Señal de SUEÑO: continua, y sin dato no tira al piso (pero SOLO si el opt-in está activo)
`M_sueño ∈ [0,1]` continuo (4 componentes de calidad por noche, telemetría). **El sueño es un opt-in:
solo entra al scoring si el opt-in de sueño está ACTIVO.** Con el opt-in activo y sin dato esa semana →
base `B_SLEEP=0.5`, nunca 0. Con el opt-in **inactivo** → el sueño **no aparece, no pesa y no limita**
(igual que la sobriedad inactiva); `B_SLEEP` NO aplica.
- **Por qué:** la telemetría puede fallar; con el opt-in activo, la ausencia de dato no es fracaso (de ahí
  el 0.5). Pero si el usuario no optó por el sueño, no hay término-sombra de sueño en absoluto.
- **Mecanismo (opt-in activo):** `M = cobertura·promedio(noches con dato) + (1−cobertura)·B_SLEEP`.

### O9 — Señal de SOBRIEDAD: por días de recaída, con golpe exponencial
`M_sobr = (1−A)^(días de recaída en la semana)`, con `A=0.55`. Multi-track = producto por track.
- **Por qué:** una recaída es un **evento crítico** (no un hábito más): 1 solo día ya saca de Plenitud,
  pero gradúa por días. El producto hace que 1 recaída pegue igual con 1 o 5 tracks (no se diluye, no
  premia "coleccionar" tracks); un track limpio = 1 (invisible).
- **Verificación:** 1 día → En marcha (0.829); 3 días → Atención (0.612); 7 días → Atención (0.553).
  Multi-track: 1 recaída de 1 track = 1 recaída de 3 tracks. ✅

### O10 — Ventana de 7 días
El score solo mira la última semana. Una recaída fuera de los 7 días no penaliza.
- **Por qué:** castigar para siempre una recaída vieja haría que el motor pierda sentido para el usuario
  con sobriedad activa. La racha larga es feature aparte (panel/hitos), no entra al score.
- **Mecanismo:** held/broke (o días de recaída) leídos solo de la ventana móvil de 7 días.

### O11 — Capa solo-opt-in: el opt-in ES la capa
Si se activa un opt-in en una capa sin anclas (D4), esa capa vale su señal `M` (peso normal) y **no
exporta extra**.
- **Por qué:** sin anclas que sostener, el opt-in es esa dimensión por sí solo; pero como no hay
  práctica, no puede destacarse.
- **Verificación:** capa solo-sueño (M=1) = vale 1.0, `extra=0`. ✅

### O12 — Anti-incentivo aceptado (es opt-in por diseño)
Activar un opt-in solo puede **empatar o bajar** el score, nunca subir. Esto es **aceptado**, no un bug.
- **Por qué:** es opt-in — el usuario pone la dificultad y el compromiso; la app no es niñera, solo
  muestra estados internos (tono ya trabajado en el lenguaje de comunicación). Además el sueño es
  telemetría automática (no hay "activar para no arriesgar").

---

## Verificación global

Los 13 axiomas se verifican en `modelo_valor_capa_v4_merge.py` (reproducible: `python3 …`). Resultados
clave: C1–C6 + D8 + anti-gate (paso máximo 0.0007, sin discontinuidades) → todos verdes. Parámetros:
`BETA=0.818` (de TARGET=0.55), `A=0.55` (sobriedad), `B_SLEEP=0.5` (sueño sin dato), `δ=0.10`.

## Origen de cada axioma

- Decisión explícita del dueño: O6, O7, O9 (A=0.55), O10, O12, O13.
- Emergente del modelo / verificado: O1, O2, O3, O4, O5, O8, O11.
- Método: comportamiento definido por el dueño → modelo que lo hace emerger (axiomas primero, no
  heredar magnitudes). Ver `definiciones-dueno-v1.md`.

## Pendiente (NO cubierto por estos axiomas)

Soportes y tasks (otra sesión); reconciliar la estabilidad temporal multi-semana (`arbol §15`) con v4;
calibración fina de A/BETA/bandas contra marcas.
