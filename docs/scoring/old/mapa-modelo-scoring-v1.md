> ⛔ HISTÓRICO (archivado 2026-06-16) — superado por la fuente de verdad única `docs/scoring/modelo-scoring-oficial-v1.md`. NO usar como contrato vigente.

# Mapa del modelo de scoring — v1 (consolidado, INCOMPLETO por diseño)

> **Estado: vivo, en construcción.** Última actualización: 2026-06-12. Consolidación de TODO lo
> iterado hasta 2026-06-12. Marca explícitamente qué está CERRADO y qué FALTA. **No es el modelo
> final** — faltan los modelos matemáticos de soportes y tasks (ver §6).
>
> Supersede como base de entendimiento a `modelo-consolidado-v1.md` y `v2.md` (que tenían errores:
> v2 metía la racha larga al valor, lo cual está MAL — ver §5).

---

## 1. El motor — CERRADO

**El motor es SOLO pesos.** `score = min(base,1) + extra` → estado por bandas. **CERO reglas,
caps, gates, colapsos, worst-term.** Todo comportamiento (sueño domina Cuerpo, recaída te tira, la
peor capa no colapsa) **EMERGE** del peso × valor. El motor debe ser inteligente, no estar lleno de
reglas-parche.

### Valor de una capa — DOS CANALES (2026-06-12, CERRADO)

El valor de una capa se construye en dos canales **separados** que nunca se mezclan entre sí:

**Canal BASE `∈ [0,1]` — "¿está en pie?"**

```
anchor_base(capa)  = promedio_i  min(R_i, 1)          ∈ [0,1]   (si hay anclas)
```

- **Capa solo-anclas:** `en_pie = anchor_base`
- **Capa con opt-in (sueño o sobriedad):** el opt-in entra como **término-sombra de peso dinámico**:

  ```
  término-ancla:  (anchor_base, W0=1)
  término-sombra: (M, w)    con   w = BETA·N·(1−M)       ← w(M=1)=0: invisible cuando bien
  ```

  `base = Σ(valor·peso) / Σ(peso)` sobre los dos términos (media ponderada dinámica).

  Con `M=1` → `w=0` → el opt-in desaparece: **neutralidad exacta** (incluso con anclas en déficit).
  Con `M=0` → `w=BETA·N` → `base = 1/(1+BETA)`, **arrastre plano en N** (no se diluye).

- **Capa solo-opt-in (sin anclas):** el opt-in ES la capa; `base = M` (no exporta extra).

**Canal EXTRA `≥ 0` — "¿te destacaste?"** — SOLO anclas

```
extra_capa = promedio_i  max(R_i − 1, 0)              (SOLO anclas; opt-in nunca da extra)
```

Agregado entre capas con anclas con **pesos IGUALES** (Sol = Tin: mismo superhabit rinde igual en
cualquier capa, independientemente de si esa capa tiene opt-in).

**Estado global**

```
base_global  = Σ(valor·peso) / Σ(peso)    sobre TODOS los términos de todas las capas
extra_global = promedio(extra_capa)        solo capas con anclas, pesos IGUALES

ESTADO = min(base_global, 1) + extra_global

Bandas: R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10   (I = 1+δ, δ=0.10)
```

**EJE:** cumplir TODO justo = score 1.0 = PLENITUD. En marcha (0.62–0.85) = cumplimiento PARCIAL.
Superhabit repartido en anclas = Inquebrantable (emergente del promedio, sin gate).

### Peso de una capa — IGUAL para todas (2026-06-12, CERRADO)

- Todas las capas activas con anclas pesan `W0=1` (masa base).
- El opt-in **NO infla el peso de capa en el promedio global**: entra como término-sombra dentro del
  valor de la capa (ver arriba), pero la capa no pesa más que sus vecinas.
- Las anclas promedian a un bloque dentro de la capa: el nº de anclas NO cambia el peso de capa.
- El peso dinámico `w = BETA·N·(1−M)` del término-sombra ESCALA con N para que el arrastre del
  opt-in sea plano en N (no diluido al agregar más capas).

> ~~**Forma A (multiplicador + renormalización) — OBSOLETO (2026-06-12):**~~ el esquema donde el
> opt-in subía el peso de su capa (Cuerpo ×k_sleep, Conducta ×k_sobr + renormalizar) quedó
> **reemplazado** por el modelo de término-sombra de peso dinámico descrito arriba. La tabla de
> pesos (43%/27%/etc.) y los valores k_sleep=1.5/k_sobr=3.0 ya NO rigen.

> ~~**Blend `γ·M + (1−γ)·promedio(anclas)` — OBSOLETO (2026-06-12):**~~ el modelo de mezcla
> intra-capa (donde el opt-in pesaba un γ fijo del valor de capa) quedó **reemplazado** por el
> término-sombra de peso dinámico. El γ ya no existe; el peso es dinámico y depende de M y N.

### OPT-INS — señales (2026-06-12, CERRADO)

```
Sueño (M_sleep ∈ [0,1]):
  M = c·avg(noches con dato) + (1−c)·B_SLEEP
  c = nº noches con dato / 7 ;  sin ningún dato → M = B_SLEEP = 0.5

Sobriedad (M_sobr ∈ [0,1]):
  M_sobr = (1 − A)^(días de recaída en la ventana de 7d)   A=0.55
  Multi-track: producto de señales por producto — track limpio = 1 = invisible (no diluye).
  1 día recaída → En marcha (0.829) · 3 días → Atención (0.612) · 7 días → Atención (0.553).
```

**BETA = 0.818** (despejado de TARGET=0.55: "opt-in en su piso + anclas perfectas → Atención").
**Mismo BETA** para sueño y sobriedad (decisión humana/compasiva: no castigar más al usuario
sensible).

Dos opt-ins malos a la vez **suman sus arrastres** (no "peor manda"), SIN tope. Es coherente: dos
cimientos rotos peor que uno.

### Estado

`score = min(base_global,1) + extra_global`; bandas `R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10`
(I=1+δ, δ=0.10). Los cortes se calibran contra el dataset de marcas del dueño.

---

## 2. Las dimensiones / variables del sistema

| Dimensión | Valores | Nota (grounded en código + dueño) |
|---|---|---|
| Nº de capas activas | 3 · 4 · 5 | **Capa activa = ≥1 ancla.** Mínimo 3 con ≥1 ancla c/u, forzado al iniciar la app (`MIN_ACTIVE_LAYERS_WITH_ANCHOR=3`). |
| Opt-ins | ninguno · sueño · sobriedad · ambos | **Acople:** sueño SOLO en Cuerpo, sobriedad SOLO en Conducta (el opt-in vive en su capa). Interior/Vínculos/Proyecto sin opt-in (por ahora). |
| Sueño | score continuo 0–1 (**NO binario**) | 4 componentes: Duración 0.40 · Continuidad 0.25 · Horario 0.20 · Interrupción digital 0.15. Telemetría AUTOMÁTICA (device-telemetry). Sin dato → `B_SLEEP=0.5`. Va a Cuerpo. (Código usa 2 de 4 = deuda.) |
| Sobriedad (ventana 7d) | continuo `M_sobr=(1−A)^días_recaída` · A=0.55 | Señal continua derivada de recaídas en los últimos 7 días. Multi-track = producto (track limpio=1, no diluye). Sin recaída → M=1 (invisible). |
| Anclas por capa | 1 · 2+ | Pesan **IGUAL** dentro de la capa (`importanceTier` NO afecta scoring). Futuro: pesos modulares por usuario, con límites. |
| Performance de un ancla | días × tiempo | **Modelo consolidado (2026-06-09/10):** `docs/scoring/exploracion-rendimiento-ancla/merge-consolidado.md` (axiomas A1–A10, Best-F, superávit subordinado a la base, verificado con 9.876 checks). Pendiente: calibración de parámetros. |
| Superhabit / Déficit | sobre / justo / bajo el objetivo | Misma normalización días+tiempo, dos direcciones. Fórmula consolidada del ancla (`merge-consolidado.md`). Pendiente: calibración de parámetros. |
| Soportes | 0..N por capa | Aportan al valor de su capa. **Modelo matemático PENDIENTE** (esta sesión 2026-06-12 no los tocó; el ±0.1 del doc viejo NO es verdad del modelo nuevo). |
| Tasks | 0..N, con capa asignada | Aportan al valor de su capa (menos que soporte). **Modelo de tasks PENDIENTE.** |
| Higiene digital | ancla de **CONDUCTA**, automática | Medida por telemetría (no se marca a mano). ⚠️ HOY mal codeada en Sueño/Cuerpo = **DEUDA TÉCNICA** (mover a Conducta). |

---

## 3. Decisiones CERRADAS (no se re-discuten)

1. Motor = pesos puros, cero reglas/caps/gates/worst-term.
2. ~~Forma A (multiplicador + renormalización)~~ → **REEMPLAZADO (2026-06-12):** pesos de capa
   IGUALES (`W0=1` por capa con anclas); el opt-in entra al VALOR de la capa como término-sombra de
   peso dinámico (ver §1). La Forma A dejó de regir.
3. Capas normales pesan igual; nº de anclas no cambia el peso (promedian dentro de la capa).
4. ~~Opt-ins = sueño (Cuerpo) y sobriedad (Conducta), únicos. Suben el peso de su capa Y entran a
   su valor.~~ → **CORREGIDO (2026-06-12):** los opt-ins NO suben el peso de capa. Entran al VALOR
   de la capa como término-sombra de peso dinámico `w=BETA·N·(1−M)`. Mismos opt-ins (sueño↔Cuerpo,
   sobriedad↔Conducta); la restricción de no inflar el peso es nueva.
5. **Ventana móvil de 7 días.** El motor semanal ve SOLO los últimos 7 días. Sobriedad en el motor =
   señal continua por recaídas en la ventana. **La racha larga (meses) es una FEATURE APARTE — NO entra al scoring.**
6. Soporte y tasks: **modelo matemático PENDIENTE** (no tocado en 2026-06-12). El ±0.1 del doc viejo
   se descarta; el modelo nuevo se definirá en una sesión dedicada.
7. Higiene digital: actividad de Conducta, se marca desde Sueño (no en la lista de anclas).
8. Inquebrantable: **NO** es un gate duro "≥2 capas". **(2026-06-10, dueño):** el concepto de
   "cobertura mínima" queda **eliminado por completo** — Inquebrantable ⟺ score global ≥ 1+δ, donde
   δ es un corte de banda calibrable igual que 0.40/0.62/0.85. El estado es lo que el promedio
   ponderado da; nada se exige por regla. (La simulación exhaustiva muestra que "≥2 capas con
   superávit a N=5" emerge solo como consecuencia aritmética.)
9. La app premia **CONSTANCIA** (días) por encima de ráfagas de productividad de 1 día (tiempo).
10. Score relativo a la meta (la meta es hipótesis ajustable); en marcha no es castigo.
11. **Capa activa = ≥1 ancla**; mínimo 3 capas con ≥1 ancla, forzado al iniciar. El opt-in vive en
    su capa (sueño↔Cuerpo, sobriedad↔Conducta); activar el opt-in implica capa activa.
12. **Anclas dentro de una capa pesan IGUAL** (default; `importanceTier` no afecta scoring). Futuro:
    pesos modulares por usuario, con límites.
13. **Sueño = score continuo 0–1** de 4 componentes (Duración/Continuidad/Horario/Interrupción digital)
    vía telemetría automática. No es binario. Va a Cuerpo.
14. **Higiene digital = ancla de CONDUCTA**, automática (telemetría). (Hoy mal codeada en sueño =
    deuda técnica a corregir.)

### Decisiones cerradas — sesión 2026-06-12 (valor de capa + motor de opt-ins)

15. **DOS CANALES por capa: base `∈[0,1]` + extra `≥0`.** No se mezclan. El opt-in nunca da extra.
    El extra es SOLO de anclas.
16. **valor_capa = min(base,1) + extra**, en la misma escala del ancla `[0,~1.5]`. El score global
    es el promedio de estos valores (pesos de capa iguales, `W0=1`).
17. **EJE:** cumplir TODO justo = score 1.0 = PLENITUD. Superhabit repartido = Inquebrantable (≥1.10).
    En marcha (0.62–0.85) = cumplimiento PARCIAL. Bandas:
    `R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10` (δ=0.10).
18. **Score global = promedio de valores de capa, pesos de capa IGUALES (`1/N`).** El nº de anclas
    no cambia el peso; el opt-in no infla el peso de capa.
19. **Extra agregado con pesos iguales entre capas con anclas** → mismo superhabit rinde igual en
    cualquier capa (Sol=Tin verificado con python3).
20. **El opt-in entra como término-sombra de peso dinámico: `w = BETA·N·(1−M)`.** Con M=1 → w=0
    (invisible, neutralidad exacta incluso con anclas en déficit). Con M=0 → arrastre plano en N.
21. **Dos opt-ins malos suman arrastres, SIN tope.** (Multi-opt-in se compone por adición de términos-
    sombra, no por peor-manda.)
22. **MISMO BETA para sueño y sobriedad (BETA=0.818).** Decisión humana/compasiva.
23. **Señal de sueño:** M ∈ [0,1] continuo (4 comp por noche; sin dato → `B_SLEEP=0.5`).
24. **Señal de sobriedad:** `M_sobr=(1−A)^(días de recaída)`, A=0.55. Multi-track = producto;
    track limpio=1 (invisible, no diluye).
25. **Capa solo-opt-in (sin anclas):** el opt-in ES la capa (base=M, no exporta extra).
    Opt-in bien (M=1) = neutro exacto.
26. **Anti-incentivo del opt-in** (activar solo puede empatar o bajar el score, nunca subir): es
    **aceptado por diseño** — es opt-in, y la telemetría del sueño es automática.
27. **Rarezas de comportamiento ACEPTADAS por el dueño (2026-06-12, NO se corrigen — son
    comportamiento esperado, no bugs):** (a) un superhabit fuerte en una capa COMPENSA un déficit en
    otra (efecto natural del promedio); (b) una capa muerta NO colapsa el estado (sigue el promedio,
    no hay worst-term); (c) agregar una capa y cumplirla justo puede BAJAR de banda (el extra se
    promedia entre capas con ancla).
28. **Vocabulario:** "área" = "capa" (Interior/Cuerpo/Conducta/Vínculos/Proyecto). No usar "área".

---

## 4. Marcas del dueño (la verdad de base)

45 marcas en `docs/scoring/dataset-decisiones-estado-v1.md` (lotes BP/SU/SBR/SO/IN). El motor de pesos
puros las reproduce **44/45** (el flip de SB9 a En marcha es correcto bajo la regla nueva). Versión
machine-readable: `scripts/scoring/weight_model_fit_v2.py` (lista `CASES`). ⚠️ Las marcas son sobre
ESTADOS; el dato de PUNTOS y de tiempo/días fino aún no está marcado.

> **Cambio de metodología (2026-06-10, dueño):** las 45 marcas fueron la herramienta de
> DESCUBRIMIENTO — sirvieron para encontrar qué buscar en el modelo matemático, y eso ya se logró
> (los axiomas del ancla). **Ya NO son medidores de verdad:** el modelo nuevo no se re-valida contra
> ellas. La verdad son los axiomas (A1–A10 del ancla, y los que se definan por capa). Las marcas
> futuras servirán para calibrar parámetros y cortes, no para auditar la estructura.

---

## 5. ERRORES detectados (corregidos, no repetir)

- ❌ **La racha larga NO va al motor semanal.** `modelo-consolidado-v2.md` y los complementos metieron
  "la racha cuenta como logro en el valor" → MAL. Lo que entra es la señal semanal de la ventana de 7d.
- ❌ **Worst-term / min(I,Cu):** el agente lo metió varias veces; el dueño lo rechazó. No existe.
- ❌ **Soporte asimétrico (0.02/0.25):** incoherente; el modelo de soportes está PENDIENTE (sin valor
  decidido aún).
- ❌ **Higiene digital como modulador pesado:** no; es una actividad de Conducta normal.
- ❌ **`EM_TOP=0.85` como factor de compresión de la base (2026-06-12, ERRATA):** versiones previas del
  doc de valor de capa (`modelo-valor-capa-consolidado-v1.md` §0) usaban `ESTADO=EM_TOP·base+W_extra·extra`
  con `EM_TOP=0.85`, haciendo que "cumplir todo justo" cayera en En marcha. **Contradecía el modelo del
  ancla** (P≥0.85 = Plenitud). El eje correcto: cumplir todo justo = score 1.0 = PLENITUD. El factor
  `EM_TOP` queda **eliminado**; la escala es directa.
- ❌ **Opt-in inflando el peso de capa (Forma A con k_sleep/k_sobr) — OBSOLETO (2026-06-12):** el
  esquema de multiplicador + renormalización (`peso_Cuerpo × k_sleep`, etc.) fue reemplazado por pesos
  de capa iguales + término-sombra de peso dinámico en el valor.
- ❌ **Blend intra-capa `γ·M + (1−γ)·promedio(anclas)` — OBSOLETO (2026-06-12):** reemplazado por dos
  canales base+extra + sombra dinámica del opt-in.

---

## 6. GAPS — lo que FALTA (admitido, no resuelto)

> **Actualización 2026-06-10:** los gaps de superhabit y déficit tienen **propuesta consolidada** en
> `docs/scoring/exploracion-rendimiento-ancla/merge-consolidado.md`, verificada (9.876 checks) y
> pendiente solo de calibración.
>
> **Actualización 2026-06-12:** los gaps de "valor de capa" y "motor de opt-ins (sueño + sobriedad)"
> quedan **RESUELTOS** (ver §1 y §3.15–26). Referencias:
> `docs/scoring/exploracion-valor-capa/merge-arrastre-optin-consolidado.md` (decisiones de opt-ins, §8)
> y `docs/scoring/exploracion-valor-capa/modelo-valor-capa-consolidado-v1.md` (estructura dos canales,
> §0-ERRATA). El único gap de capa que queda es **SOPORTES y TASKS**.

✅ **RESUELTO — Fórmula del rendimiento del ancla (superhabit + déficit).**
Propuesta consolidada en `docs/scoring/exploracion-rendimiento-ancla/merge-consolidado.md` (axiomas
A1–A10, Best-F, superávit subordinado a la base). Pendiente: calibración de parámetros.

✅ **RESUELTO — Valor de capa (estructura dos canales).**
DOS canales por capa: base (¿en pie?, ∈[0,1]) + extra (superhabit, ≥0, SOLO anclas). Verificado
con 12/12 casos límite y 8 criterios del opt-in. Fecha cierre: 2026-06-12.

✅ **RESUELTO — Motor de opt-ins (sueño + sobriedad).**
Término-sombra de peso dinámico `w=BETA·N·(1−M)`, BETA=0.818, mismas señales, misma perilla para
sueño y sobriedad. Verificado con python3 (neutralidad exacta, arrastre plano en N, Sol=Tin).
Fecha cierre: 2026-06-12. Referencia: `docs/scoring/exploracion-valor-capa/merge-arrastre-optin-consolidado.md` §8.

🔴 **Modelo matemático de SOPORTES.** Cómo entra un soporte al valor de su capa (canal base,
saturación multi-soporte, dirección ±). Sesión 2026-06-12 no lo tocó. Siguiente gap a resolver.

🔴 **Modelo matemático de TASKS.** Cómo entra una task al valor de su capa (menos que soporte,
sin neutras). Pendiente junto con soportes.

🔴 **Puntos vs Estados.** Todo lo calibrado es sobre ESTADOS. Falta definir cómo el score interno
se TRADUCE al PUNTAJE numérico que ve el usuario.

🔴 **Multi-actividad de sobriedad** (2+ tracks, ej. alcohol + tabaco): el producto de señales está
cerrado (`M_sobr = Π m_track`), pero la agrupación UI y el comportamiento cuando hay N tracks activos
sin recaída vs 1 recaída en uno merece validación con marcas del dueño.

~~🔴 **Escalado de Inquebrantable / cobertura con N**~~ — **OBSOLETO (2026-06-10):** no hay concepto
de cobertura; el "≥2" histórico salió de las 45 marcas y ya no es medidor de verdad (ver §3.8 y §4).

~~🔴 **Sueño + sobriedad activos juntos sin marcas**~~ — **RESUELTO (2026-06-12):** la composición
es suma de términos-sombra (no peor-manda); verificado en casos extremos con python3 (§4 del merge).
El dueño aceptó el combinado sin tope.

🟡 **Calibración de parámetros.** `BETA=0.818` despejado de un axioma; `A=0.55` de la señal de
sobriedad; cortes de banda (0.40/0.62/0.85/1.10) y `δ=0.10` propuestos. Se pinchan con marcas del
dueño cuando toque, especialmente en casos de soportes y multi-opt-in.

---

## 7. Espacio de permutaciones a recorrer (para el análisis completo)

El análisis real necesita recorrer las combinaciones de §2. Ejes principales: **N capas × opt-ins activos
× estado de cada modulador × performance (días×tiempo) × soportes × tasks**. Casos límite críticos ya
cubiertos: el apretón (3 capas + 2 opt-in → verificado), capa modulada sin actividades (verificado),
superhabit repartido (Sol=Tin, verificado). Pendientes: multi-soporte, model tasks, combinado extremo de
opt-ins con anclas en déficit.

---

## 7-bis. Estado del CÓDIGO actual — referencia (para NO re-revisar el código)

El scoring en `app/.../domain/scoring/` (`ScoringConstants.kt`, `*Policy.kt`) es el modelo VIEJO. Esta
es la foto de qué está BIEN/informativo y qué es deuda técnica, **grounded en el código**, para que ningún
agente tenga que volver a grepearlo.

### ✅ Correcto / informativo (es así — no re-revisar)
- **Sueño = score continuo 0–1**, NO binario, vía telemetría AUTOMÁTICA (`device-telemetry`, ya en main).
  4 componentes: Duración 0.40 · Continuidad 0.25 · Horario 0.20 · Interrupción digital 0.15. Va a Cuerpo. Docs: `docs/sueno/`.
- **`importanceTier` NO afecta el scoring** (cero refs en `domain/scoring`). Anclas dentro de una capa pesan IGUAL.
- **Mínimo 3 capas con ≥1 ancla**, forzado al iniciar (`MIN_ACTIVE_LAYERS_WITH_ANCHOR=3`). Capa activa = ≥1 ancla.
- **Opt-ins solo en Cuerpo (sueño) y Conducta (sobriedad).** Interior/Vínculos/Proyecto no tienen.
- **Tasks tienen capa asignada** y dan un bonus por capa con rendimientos decrecientes (`TaskMomentumPolicy`).
- **El ancla mezcla días y tiempo** (estructura frecuencia + valor) — días y tiempo conviven en una ancla.

### ⚠️ A actualizar / deuda técnica (NO copiar valores)
- **Higiene digital** codeada como componente del score de SUEÑO (Cuerpo) → **debe moverse a CONDUCTA** como ancla automática. Deuda técnica.
- **Sueño usa 2 de 4 componentes** (`duration·0.70 + schedule·0.30`); faltan Continuidad e Interrupción digital.
- **Reglas-parche a ELIMINAR:** `WORST_LAYER_COLLAPSE`, `WORST_LAYER_MIN_FOR_*`, gates `UNBREAKABLE_*` — el modelo nuevo es pesos puros, sin estas reglas.
- **Valores VIEJOS a descartar (≠ los del modelo nuevo):** cortes 0.40/**0.70**/0.85 (nuevo: 0.40/**0.62**/0.85);
  soporte (pendiente, modelo aún sin definir); task 0.05; ancla `0.70·días+0.30·tiempo` (reemplazada por Best-F);
  sueño 0.30 en Cuerpo (reemplazado por término-sombra BETA·N·(1−M)); sobriedad 0.30 en Conducta (ídem);
  k_sleep/k_sobr como multiplicadores de peso de capa (eliminados).
- **Modelos matemáticos PENDIENTES desde cero:** soportes, tasks, traducción a puntos visibles. El modelo
  viejo NO es punto de arranque.

## 8. Referencias

- Ancla (fórmula y superhabit/déficit): `docs/scoring/exploracion-rendimiento-ancla/merge-consolidado.md`.
- Valor de capa (dos canales): `docs/scoring/exploracion-valor-capa/modelo-valor-capa-consolidado-v1.md`
  (incl. §0-ERRATA: EM_TOP eliminado; §0-bis: pesos iguales).
- Motor de opt-ins (término-sombra, BETA, señales): `docs/scoring/exploracion-valor-capa/merge-arrastre-optin-consolidado.md` (esp. §8: decisiones cerradas del dueño).
- **Contrato de axiomas de los opt-ins (O1–O13):** `docs/scoring/axiomas-opt-in-v1.md` (canónico, como A1–A10 del ancla).
- Definiciones del dueño 2026-06-12: `docs/scoring/exploracion-valor-capa/definiciones-dueno-v1.md`.
- Marcas: `docs/scoring/dataset-decisiones-estado-v1.md` · machine-readable: `scripts/scoring/weight_model_fit_v2.py`.
- Exploraciones opus: `docs/scoring/modelo-propuesto-{A,B,C}.md`, `modelo-complemento-{A,B,C}.md`.
- Consolidados previos (parciales, con errores §5): `modelo-consolidado-v1.md`, `v2.md`.
- Planes: `meta/instructions/2026-06-08-*.md`.
- Filosofía soporte: `docs/dominio/definicion-reestructuracion-soporte.md`.
- Memoria engram (`apk-personal`): `scoring/filosofia-pesos-puros`, `scoring/correcciones-cerradas`,
  `scoring/refinamientos-soporte-superhabit-higiene`, `scoring/dimensiones-faltantes`.
