# Revisión — Fidelidad a lo decidido + Filosofía de producto

> **Revisor 2** (fidelidad a los contratos fuente + filosofía de producto).
> Target: `docs/scoring/modelo-matematico-nucleo-v1.md` ("el núcleo").
> Fecha: 2026-06-16. Proyecto: `apk-personal`.
> Método: comparación línea-a-línea contra los contratos fuente + verificación numérica
> de los axiomas clave ejecutando el Python del propio núcleo.

---

## Veredicto general

**El núcleo es ALTAMENTE FIEL.** Captura las decisiones cerradas con precisión y, en lo
ejecutable, reproduce los axiomas clave (verifiqué corriendo su propio Python: cumplir-justo
3 capas = **1.0**, recaída total + anclas perfectas = **0.5501**, Sol = Tin = **1.1667**).
La filosofía de producto se respeta de punta a punta: motor de pesos puros sin gates/caps/
worst-term duros, orden ANCLAS > SOPORTES > TASKS, ventana de 7 días, cumplir-justo = Plenitud,
el "1000" se gana con superhabit, soportes muy levemente, tasks efímeras que no compran estados.

**No hay distorsiones 🔴 que cambien el comportamiento del modelo.** Los hallazgos son
inconsistencias de documentación / valores residuales en docs fuente (no en el núcleo) y un par
de omisiones menores de trazabilidad. El núcleo, de hecho, está MÁS al día que algunos de sus
propios docs fuente (eligió correctamente los valores calibrados finales sobre los borradores).

---

## Criterio 1 — Fidelidad a los contratos fuente

### 🟢 Ancla (A1–A10 / merge-consolidado)
- Fórmula `R = base + base²·S` copiada **idéntica** del `merge-consolidado.md §2.2` (líneas 43-65).
  Preprocesamiento Best-F, `u(r)=min(r,1)^γ`, `φ`, `V`, reparación voluntaria
  `base = 1−(1−φ)·exp(−λ_v·V)`, `St`, `Sd`, `wt=(F/7)^κ`, saturación `smax·(1−exp(−·/s0))`:
  todo fiel. Núcleo líneas 64-83.
- Parámetros `γ=1.5, λ_v=0.5, κ=1.5, p=2.0, smax=0.5, s0=0.5` coinciden con los ilustrativos
  del merge (línea 139) y con `axiomas-modelo-scoring-v1.md §0`. El merge los marcaba como
  "ilustrativos / sin calibrar"; el núcleo los declara "cerrados". Es coherente: se congelaron
  en `axiomas-modelo-scoring-v1.md`.
- Casos de referencia del núcleo (línea 96-97) coinciden con el merge: `F=3,T=30→1.0`, `D=0→0`,
  `[30/45/120]×7 → 1.0/1.32/1.46`. (El merge da 1.499 para `120×7`; el núcleo dice 1.46. Ver 🟡-1.)

### 🟢 Opt-ins (O1–O13 / axiomas-opt-in-v1 + merge-arrastre-optin)
- Señales `M`: sueño `c·avg + (1−c)·B_SLEEP` (O8), sobriedad `Π(1−A)^días` (O9, A=0.55,
  multi-track = producto): fieles. Núcleo líneas 152-157.
- Término-sombra: el núcleo usa `w = BETA·Σpesos·(1−M)` (línea 162). El contrato O3/merge-v4
  original usaba `w = BETA·N·(1−M)`. **Esto NO es una distorsión: es la decisión cerrada I1**
  (`axiomas-modelo-scoring-v1.md §9-bis`, líneas 164-167): generalizar `N → Σpesos` por el peso
  de capa variable. El núcleo lo documenta explícitamente (línea 165-167). Verificado: arrastre
  plano 0.55 se mantiene. ✅
- O13 reflejado correctamente: el opt-in no infla su propia capa; las capas ya NO pesan 1/N
  (peso variable). Coincide con el cambio cerrado.
- I2 (capa solo-opt-in pesa W0=1, no ρ) presente: núcleo línea 142 y código (`opt → W0`).
- O6 (componen sin tope), O10 (ventana 7d), O11 (capa solo-opt-in): todos presentes.

### 🟢 Soportes / tasks / peso de capa (modelo-consolidado-v3)
- Soportes: blend `base_eff = (1−WS)·base_anclas + WS·G`, `WS=0.07`, `G=promedio`, ventana 4d,
  `s_i=min(días/4,1)`. Fiel a v3 §2 y a `axiomas-modelo-scoring-v1 SO1–SO6`. Núcleo 112-118.
- Tasks: saturación conjunta reparametrizada con `su_anc`, `g_task=1−exp(−n_hoy/N0)`,
  `THETA`, re-saturación por la misma curva, gate `base²`. `TAU=0.06`, `N0=1.0`. Fiel a
  v3 §2 / merge-v2 §2. Núcleo 122-130.
- Peso de capa: `peso(n)=Σr^k=(1−r^n)/(1−r)`, `r=0.5`, techo 2.0, `peso(0)=ρ=0.15`. Fiel a
  v3 §3 (con la calibración final de ρ — ver 🟡-2). Núcleo 138-142.
- Agregación: base ponderada por peso, extra plano (Opción 2 → Sol=Tin), Forma 1
  (promedio dentro de la capa). Fiel a v3 §4. Núcleo NIVEL 5.

### 🟢 Mapeo a puntos (opus-E-mapeo)
- Hitos `(c,w,A)` del núcleo (línea 210): `(0.18,0.10,60)·(0.55,0.11,110)·(0.83,0.09,100)·
  (1.07,0.055,130)·(1.35,0.13,50)`. **Idénticos** a `opus-E-mapeo.md §3` (líneas 53-57).
- Reescala afín, piso 650 / tope 1100, hitos en cortes (0→650, 1.0→941, 1.10→1011, 1.5→1100):
  fieles a opus-E §4 (líneas 71-79). ✅

### Hallazgos del Criterio 1

**🟡-1 — `120×7` da 1.46 en el núcleo, 1.499 en el merge fuente.**
Núcleo línea 97 lista `[120×7] → 1.46`; el merge-consolidado §4.2 (línea 170) da **1.499**.
La fórmula NO cambió (verifiqué: el Python del núcleo es idéntico al del merge). Es un error
de **transcripción del número de referencia** en la tabla del núcleo (probablemente arrastrado
de un caso distinto). No afecta el modelo, pero un caso de referencia mal anotado confunde al
implementador que use esa tabla como test. **Corregir a 1.499** (o recortar el ejemplo).

**🟡-2 — `ρ` arrastra una inconsistencia desde el doc fuente v3 (resuelta correctamente).**
`modelo-consolidado-v3 §3` (línea 70) dice `ρ=0.35`; su propio §7 (línea 119) lo recalibra a
`ρ=0.15`. El núcleo eligió **0.15** (el valor calibrado final), igual que `axiomas-modelo-
scoring-v1 §0`. **Decisión correcta del núcleo** — gana el valor calibrado. Lo dejo como 🟡
solo para señalar que el doc fuente v3 quedó auto-contradictorio (35 vs 15) y debería limpiarse,
no porque el núcleo se equivoque.

**🟡-3 — `axiomas-opt-in-v1.md` (vigente) quedó desactualizado respecto del núcleo.**
El contrato O3/O13 vigente todavía dice `w=BETA·N·(1−M)` y "todas las capas pesan 1/N"
(líneas 17, 42-44, 59-65). El núcleo aplica la generalización I1/I2 (Σpesos, peso variable).
El núcleo está BIEN (sigue la decisión cerrada más nueva en `axiomas-modelo-scoring-v1`), pero
hay DOS docs marcados "vigente/canónico" que se contradicen entre sí. **Riesgo de fidelidad
futura:** un agente que lea solo `axiomas-opt-in-v1.md` reconstruirá el modelo viejo. Recomiendo
añadir en `axiomas-opt-in-v1.md` una nota de superseded apuntando a `axiomas-modelo-scoring-v1
§9-bis`. (El propio `axiomas-modelo-scoring-v1` ya marca O13 como cambiado; falta el cruce inverso.)

**🟡-4 — Numeración de axiomas del ancla sin reconciliar (heredado, no introducido).**
El núcleo no enumera AN1–AN12 (eso vive en `axiomas-modelo-scoring-v1`). La nota de ese contrato
(líneas 48-49) admite que A6/A8 no figuran en el merge-consolidado. No es defecto del núcleo
(es detalle matemático, no contrato de axiomas), pero la trazabilidad ancla↔axioma queda en el
otro doc. Aceptable dado el rol del núcleo.

**🟢 — Decisiones abiertas del merge-v2 (§5.1, §5.2) resueltas correctamente.**
El merge-v2 dejaba 2 decisiones del dueño: castigo del soporte en capa perfecta y piso tibio
para capa solo-soportes. El núcleo implementa **blend simple** (descuidar siempre cuesta un toque,
incluso con anclas perfectas → coincide con SO2 "a la par = neutro", castigo 0.07) y **sin piso
tibio** (`ρ=0.15` sin piso, capa solo-soportes abandonada arrastra). Coincide con la calibración
final de v3 §7 (línea 119: "ρ SIN piso"). Fiel.

---

## Criterio 2 — Filosofía de producto

### 🟢 Motor de pesos puros (cero gates/caps/worst-term duros)
- Todo emerge: el gate `base²` es una **propiedad continua** (no un if), el techo de superhabit
  emerge de la saturación exponencial, el techo de task (~+0.06) **emerge** de re-saturar por la
  misma curva (núcleo línea 132), el límite "ninguna capa > 50%" emerge del techo 2.0 de votos.
  No hay un solo `if` de negocio. Cumple el principio rector de `axiomas-modelo-scoring-v1 §0`.
- Los dos `min(...,1)` (base_eff y base_global, líneas 184 y código) y el `max(be,0)` son
  saturaciones de rango, no gates de comportamiento. ✅

### 🟢 ANCLAS > SOPORTES > TASKS
- Impacto por unidad documentado y respetado: ancla 0.22 > soporte 0.07 > task 0.06
  (`axiomas-modelo-scoring-v1 TA6`, verif merge-v2 §4(h)). El swing del soporte (±0.07·base) es
  estructuralmente menor que el de un ancla; las tasks topan en ~+0.06/capa. Orden correcto.
- TA5/PU3 verificados conceptualmente: cumplir-justo + ∞ tasks → ESTADO 1.06 (Plenitud), **nunca
  Inquebrantable**. El esfuerzo de élite se gana con anclas. Núcleo línea 132 + axiomas TA5. ✅

### 🟢 No humillar / tono
- Piso de puntos **650** (digno, no 0) — núcleo línea 215, alineado con PU1 ("no humilla").
- Bandas con nombres compasivos (Restauración, Atención, En marcha, Plenitud, Inquebrantable),
  no punitivos. Coincide con el tono obligatorio de `AGENTS.md` (evitar "fallaste/estás mal").
- Soporte UX inversa: "sin registro del día = cumplido" (núcleo línea 119) → cero fricción,
  no penaliza la ausencia de dato. Alineado con la filosofía compasiva.

### 🟢 Ventana de 7 días
- Declarada en NIVEL 0 ("ventana móvil 7 días"), opt-ins (línea 168 "Ventana de 7 días"),
  tasks efímeras diarias (reset). Coincide con O10. ✅

### 🟢 Cumplir-justo = Plenitud
- BANDA: Plenitud entra en 0.85; cumplir-justo (1.0) cae DENTRO de Plenitud, zona alta
  (núcleo línea 200, BA2). Resuelve la contradicción histórica del árbol. Verificado = 1.0. ✅

### 🟢 El "1000" se gana con superhabit
- Cumplir-justo → **941** puntos, no 1000. El 1000 se cruza al entrar a Inquebrantable (~1.09,
  1011 pts). Núcleo línea 216. Fiel a opus-E §2 y PU3. ✅

### 🟢 Soportes "muy levemente" / tasks efímeras que no compran estados
- WS=0.07 (swing ±~13 pts máx), tasks 1 task ≈ +8 pts, tope +41 inviable, reset diario, nunca
  compran Inquebrantable. Núcleo NIVEL 2.1/2.2 + axiomas SO6/TA2/TA5. Fiel. ✅

### Hallazgo del Criterio 2

**🟡-5 — O12 "anti-incentivo" no se menciona en el núcleo.**
El contrato `axiomas-opt-in-v1 O12` (líneas 114-118) define explícitamente que activar un opt-in
solo puede empatar o bajar, nunca subir, y que eso es **aceptado por diseño** (decisión humana del
dueño). El núcleo lo implementa correctamente (un opt-in con M<1 solo arrastra; M=1 es neutro
exacto), pero no lo enuncia. Es una propiedad filosófica importante (el dueño la marcó explícita).
Sugerencia: una línea en NIVEL 4 ("anti-incentivo aceptado, O12"). Menor — el comportamiento está,
falta la nota.

---

## Tabla: decisión cerrada → ¿está en el núcleo? ¿coincide?

| Decisión cerrada (fuente) | ¿En el núcleo? | ¿Coincide? |
|---|---|---|
| Ancla `R=base+base²·S`, Best-F, reparación voluntaria (merge §2.2) | Sí (N1) | 🟢 idéntico |
| Params ancla γ/λ_v/κ/p/smax/s0 = 1.5/0.5/1.5/2/0.5/0.5 | Sí (§0.1) | 🟢 |
| Cumplir-justo = R=1.0 exacto (AN3) | Sí | 🟢 verif 1.0 |
| Piso cero D=0→R=0 (AN2) | Sí (N1.1) | 🟢 |
| Gate base² subordina superhabit (P2/AN6) | Sí (N1.2) | 🟢 |
| Superhabit tiempo+días, techo smax=0.5 (AN7) | Sí | 🟢 |
| Valor capa: base + extra, anclas pesan igual (VC1–VC4) | Sí (N2) | 🟢 |
| Forma 1 (promedio dentro de capa) | Sí | 🟢 |
| Peso capa votos r=0.5, techo 2.0 (PC1–PC4) | Sí (N3) | 🟢 |
| Capa solo-soportes ρ=0.15 sin piso (PC5, v3§7) | Sí | 🟢 (eligió bien vs ρ=0.35 del borrador) |
| Soportes blend WS=0.07, G=promedio, ventana 4d (SO1–SO6) | Sí (N2.1) | 🟢 |
| Tasks saturación conjunta, TAU=0.06, efímera, gate base² (TA1–TA6) | Sí (N2.2) | 🟢 |
| Opt-in término-sombra, M, BETA=0.818, A=0.55, B_SLEEP=0.5 | Sí (N4) | 🟢 |
| Opt-in escala con Σpesos (I1, generaliza N) | Sí (N4.2) | 🟢 verif 0.55 plano |
| Capa solo-opt-in pesa W0=1 (I2/O11) | Sí (N5) | 🟢 |
| Sol=Tin (O5/AG3, extra plano) | Sí (N5) | 🟢 verif 1.1667=1.1667 |
| O6 componen sin tope / O10 ventana 7d | Sí (N4) | 🟢 |
| O12 anti-incentivo aceptado | Implementado, no enunciado | 🟡-5 |
| Bandas R<0.40·A<0.62·EM<0.85·P<1.10·I≥1.10, Plenitud=0.85 (BA1/BA2) | Sí (N6) | 🟢 |
| Mapeo E: hitos (c,w,A), piso 650, tope 1100, 1.0→941, 1.10→1011 | Sí (N7) | 🟢 idéntico |
| Caso ref `120×7` | Sí (N1.4) | 🟡-1 dice 1.46, fuente 1.499 |

---

## Recomendaciones

1. **🟡-1 Corregir el caso de referencia `120×7`** en el núcleo línea 97: debe ser **1.499**
   (o ~1.5), no 1.46 — para no envenenar la suite de tests del implementador.
2. **🟡-3 Cruzar la nota de superseded en `axiomas-opt-in-v1.md`**: agregar que O3/O13
   (`BETA·N`, "pesos 1/N") quedaron generalizados por I1/I2 en `axiomas-modelo-scoring-v1 §9-bis`.
   Hoy dos docs "vigentes" se contradicen; el riesgo es que un agente reconstruya el modelo viejo.
3. **🟡-2 Limpiar el doc fuente v3** (`ρ=0.35` en §3 vs `0.15` en §7) para que no quede
   auto-contradictorio — aunque sea `old/`, se sigue citando.
4. **🟡-5 Añadir una línea sobre O12** (anti-incentivo aceptado) en NIVEL 4 del núcleo:
   el comportamiento ya está; falta hacer explícita la decisión humana del dueño.
5. **🟡-4 Reconciliar la numeración AN del ancla** (A6/A8) si aparece la lista formal completa
   — ya señalado en `axiomas-modelo-scoring-v1`; no bloquea.

Ninguna recomendación es bloqueante. El núcleo es una consolidación fiel y filosóficamente
correcta; los ajustes son de higiene documental y un número de referencia mal transcrito.
