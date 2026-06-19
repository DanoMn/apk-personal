# Revisión — Rigor matemático e implementación (REVISOR 1)

> Revisión crítica (no complaciente) de `docs/scoring/modelo-matematico-nucleo-v1.md` contra:
> corrección de fórmulas · coherencia fórmulas↔código · contrato `axiomas-modelo-scoring-v1.md` ·
> robustez/red-team con `python3` real. Fecha: 2026-06-16. Proyecto: `apk-personal`.

---

## Veredicto general: **SÓLIDO, con observaciones (sin bugs de fórmula).**

El núcleo matemático es correcto: rango, monotonía, continuidad, gates, saturaciones y agregación
se comportan como dice el contrato. **`verificacion_modelo_oficial.py` corre 27/27 verde** (confirmado
ejecutándolo). El código de referencia del doc reproduce **exactamente** (diff = 0.0 en todas las
configs probadas) el del script de verificación. **No encontré ningún 🔴 que rompa la matemática del
modelo.** Lo que sí hay: **valores de ejemplo erróneos en la prosa** (engañan a quien lee el contrato),
y **fragilidad de borde en el código de referencia** (excepciones ante configs vacías / inputs fuera de
dominio). Ninguna afecta la corrección del modelo bien-formado, pero el código se publica como
"implementación de referencia, ejecutable", así que sus bordes importan.

---

## Criterio 1 — Corrección de cada fórmula

Repaso nivel por nivel. Todo corrido con `python3`.

- 🟢 **NIVEL 1 — ancla `R`.** Rango `[0,1.5]` confirmado por barrido exhaustivo (F=2..7, t∈{0.001..1e6},
  0..7 días): `mín=0.0, máx=1.5`. Cumplir-justo = 1.0 EXACTO para todo F=2..7 (probado los 6). Piso cero
  (D=0). Invarianza de escala exacta (`T=30,[40,30,30] ≡ T=120,×4`, diff 0.0). Monotonía y continuidad
  OK. `Sd=V/(7−F) if F<7 else 0` coincide con el texto ("0 si F=7"). Gate `base^p` correcto. **Sin errores
  de signo/índice/paréntesis.**
- 🟢 **NIVEL 2 — valor de capa.** `base_anclas=avg min(R,1)`, `extra=avg max(R−1,0)`. Correcto.
- 🟢 **Soportes (blend).** `base_eff=(1−WS)·base+WS·G`, convexo y leve. Correcto.
- 🟢 **Tasks (saturación conjunta).** Verifiqué que `task_lift ≥ 0` SIEMPRE (grid ex∈[0,0.5), nt∈{ε..100}:
  `mín lift = 1.3e-7 > 0`). El "techo TAU=0.06" **emerge** de verdad: capa base=1 con 100000 tasks →
  `extra = 0.06000…` exacto. Nunca pasa el cap 0.5. Correcto.
- 🟢 **NIVEL 3 — peso de capa.** `(1−r^n)/(1−r)` con r=0.5 → 1/1.5/1.75/…→techo 2.0; `votos(0)=ρ=0.15`.
  Correcto. Peor caso 3 capas = 50.0% exacto (PC3).
- 🟢 **NIVEL 4 — opt-ins, término-sombra `BETA·Σpesos·(1−M)`.** `M=1→w=0` (neutralidad exacta verificada
  con anclas en déficit: diff 0.0). Arrastre PLANO a 0.55 con 3 configs distintas. Sobriedad
  `(1−A)^días`, multi-track = producto. Correcto.
- 🟢 **NIVEL 5 — bolsa-global.** `base=Σ(v·w)/Σ(w)`, `extra=avg plano`, `ESTADO=min(base,1)+extra`.
  `Sol=Tin` exacto (diff 0.0). `ESTADO ≤ 1.5` confirmado por barrido de configs extremas (máx hallado
  = 1.5). Correcto.
- 🟢 **NIVEL 6 — bandas.** Cortes `R<0.40·A<0.62·EM<0.85·P<1.10·I≥1.10`. `banda(1.10)=Inquebrantable`,
  `banda(1.0999999)=Plenitud`, `banda(0.85)=Plenitud`, `banda(0.84)=En marcha`. Coincide con BA1/BA2.
- 🟢 **NIVEL 7 — mapeo E logístico.** `PUNTOS` estrictamente monótono sobre [0,1.5] (15001 puntos).
  Hitos: 650/941/1011/1100 OK. Clamp fuera de rango correcto (`PUNTOS(-5)=650`, `PUNTOS(99)=1100`).

- 🟡 **BETA=0.818 no da 0.55 exacto.** El despeje correcto de `1/(1+BETA)=0.55` es
  `BETA=1/0.55−1=0.81818…`. Con `0.818` truncado, el arrastre da **0.55006**, no 0.55. Es irrelevante
  numéricamente (5e-5), pero el contrato dice "TARGET=0.55 despejado". Si se quiere exactitud,
  documentar `BETA=9/11` o `0.8182`. (Severidad baja: cosmética.)

## Criterio 2 — Coherencia fórmulas ↔ código de referencia

- 🟢 **El código del doc ≡ el de verificación.** Extraje el bloque Python del doc y lo corrí contra
  `verificacion_modelo_oficial.py` en 8 configs (justas, opt-in, soportes, tasks, solo-soportes,
  solo-opt-in, mixtas): **diff máximo = 0.0**. `banda()` y `PUNTOS()`/`P()` idénticos en todos los puntos
  de borde. **No hay divergencia.**

- 🟡 **Gate de tasks hardcodeado `be**2` en vez de `be**P`.** El doc define la constante `p=2.0` y llama
  al gate de tasks "gate base²" (NIVEL 2.2: `task_lift = (…)·base_eff²`). En el código tanto del doc
  (`…)*be**2`) como del de verificación, el `2` está **literal**. El gate del ancla sí usa `base**P`. Es
  correcto MIENTRAS `p=2`, pero es un **acoplamiento oculto**: si alguien recalibra `p` (los valores son
  "afinables" según el propio doc, sección Pendientes), el gate de tasks **no lo seguiría** y quedaría
  silenciosamente desincronizado del gate del ancla. Recomendación: usar `be**P` (o documentar que el
  gate de tasks es fijo en 2 a propósito).

- 🟡 **`banda()` del doc usa `e<1+DELTA`; el de verificación usa `e<1.0+DELTA`.** Numéricamente idéntico
  (1.10). Solo nota de consistencia estilística.

## Criterio 3 — Contra el contrato de axiomas

- 🟢 **27/27 verde**, reproducido ejecutando `verificacion_modelo_oficial.py` (corrida limpia, 0 rojos).
- 🟢 Spot-checks propios extra que confirman axiomas no cubiertos por el script:
  - **AN9 (piso del voluntario):** día vol ε→0 aporta `+1.15e-9 ≥ 0`, no resta. OK.
  - **O4 (opt-in mal no mata anclas):** con `optin=0` la base global baja a 0.55 vía término-sombra, pero
    el término del ancla mantiene `val=1` (no se anula el ancla). OK.
  - **TA1/gate (tasks no aportan sin cimiento):** `be=0 + 10 tasks → lift 0`. OK.
- 🟡 **El script "27/27" no cubre todos los axiomas enunciados.** Faltan checks explícitos para, p.ej.,
  **AN12 (constancia>ráfaga)**, **VC4 (Forma 1, dilución 1/n)**, **SO6 / TA6 (ordenamiento de impacto
  ancla≫soporte>task)**, **O6 (dos opt-ins componen sin tope)**, **O9 multi-track**, **PU2 (hitos-meta)**.
  El "27/27" es real pero **no es cobertura total del contrato**; el doc lo presenta como si validara
  TODO. Verifiqué SO6/TA6 a mano: soporte swing ≈ 9.2 pts vs task lift ≈ 8.0 pts — el orden
  ancla≫soporte>task se cumple, pero **por margen chico entre soporte y task** (no es la jerarquía amplia
  que sugiere la prosa "0.07 vs 0.06"). Recomendación: o ampliar el script, o no afirmar "valida el
  contrato entero".

## Criterio 4 — Robustez / red-team (corrido con `python3`)

| Caso | Entrada | Resultado | Veredicto |
|---|---|---|---|
| `log(1−extra/smax)` con extra=smax | ancla R=1.5 (`extra=0.5`) + 1 task | guard `ex<SMAX else 1e9` → `exp(−2e9)=0` → extra=0.5 | 🟢 no rompe (guard correcto) |
| extra > smax inyectado | ancla R=1.6 + task | clamp a 1.5 | 🟢 |
| **`estado([])`** (0 capas) | lista vacía | **`ZeroDivisionError`** (`Σpeso=0`) | 🔴-menor |
| **`estado([{}])`** (capa sin nada) | capa sin anclas/sup/optin | **`ZeroDivisionError`** | 🔴-menor |
| **`R(F,0,…)`** (T=0) | tiempo-meta 0 | **`ZeroDivisionError`** (`m/T`) | 🔴-menor |
| **`R(0,30,…)`** (F=0) | frecuencia 0 | **`ZeroDivisionError`** (`/F`) | 🔴-menor |
| `R(8,30,[30]*8)` (F fuera de 2–7) | F=8 | devuelve 1.0 silencioso | 🟡 (acepta input ilegal sin avisar) |
| `R(4,30,[60]*10)` (>7 días/semana) | 10 días | 1.478 (Sd=2.0 satura) | 🟡 (acepta semana imposible) |
| **`sup_days=[-3]`** (negativo) | día sostenido negativo | `min(−3/4,1)=−0.75`, **señal NEGATIVA sin clamp inferior** → base_eff baja indebido (0.8775) | 🟡 |
| `optin=−0.5` | M fuera de [0,1] | 0.1736 (sin validar) | 🟡 |
| `optin=2.0` | M>1 | shadow weight negativo, da 1.0 | 🟡 |
| `sup_days=[]` lista vacía | m=0 | `[]` es falsy → blend salteado, sin crash | 🟢 (suerte, no diseño) |
| 50 capas justas | escala | 1.0 estable | 🟢 |
| tasks monótono en `n_tasks` 0..200 | — | monótono | 🟢 |
| `PUNTOS` fuera de [0,1.5] | −5, 99 | clamp 650/1100 | 🟢 |

**Hallazgo de robustez clave (🔴 de implementación, no de modelo):** el código de referencia **no valida
dominios** y revienta con `ZeroDivisionError` en cuatro entradas plausibles desde un wiring real:
config sin términos (`Σpeso=0`), capa vacía, `T=0`, `F=0`. Para un doc que se vende como
"implementación de referencia, ejecutable", esto es una trampa: la app que copie estas funciones
heredará los crashes. El **modelo** está bien (esas entradas son ilegales por contrato: F∈[2,7], T>0,
≥1 término), pero la **referencia** debería degradar con gracia o documentar precondiciones.

**Hallazgo de borde 🟡 real (no solo input basura):** `s_i = min(días/4, 1)` **no tiene clamp inferior**.
Días sostenidos negativos no deberían existir, pero la señal de soporte puede ir por debajo de 0 y
arrastrar `base_eff` más de lo que el contrato (SO1: techo 1, piso implícito 0) permite. Debería ser
`max(0, min(días/4, 1))`. (Mismo patrón faltante para `M` de opt-in: no se clampa a [0,1].)

---

## Errores de ejemplo en la prosa (🟡 — engañan al lector del contrato)

Las fórmulas están bien; **los números citados como "verificados" están mal en dos casos** (sección 1.4):

- 🟡 **"2/4 días×60 → 0.59"** — el valor real es **0.5438** (`R(4,30,[60,60])`). Off por ~0.05. (No es
  otra interpretación de F/T: probé F=2 y T=60, ninguna da 0.59.)
- 🟡 **"[120×7] → 1.46"** — real **1.499** (`R(7,30,[120]*7)`), pegado al cap 1.5. Off por ~0.04.
- 🟢 El resto de 1.4 verifica: `F=3,[30×3]=1.000`, `D=0=0`, `4×60=1.289`, `6×30=1.266`, `[45×7]=1.316≈1.32`.
- 🟢 Tabla de hitos de puntos (NIVEL 7): todos dentro de ±0.5 del valor real (diferencias por redondeo).

Estos números aparecen como "(verificados)" — corregirlos o quitar el sello.

---

## Recomendaciones concretas (priorizadas)

1. **(🔴-impl) Endurecer la implementación de referencia** o marcar precondiciones explícitas:
   - `estado()`: si `Σpeso==0` (lista vacía / solo capas-vacías) → devolver 0.0, no dividir por cero.
   - `R()`: validar `T>0` y `F≥1` (o documentar `F∈[2,7], T>0` como precondición dura).
2. **(🟡) Clamp inferior en señales:** `s_i = max(0, min(días/4, 1))` y `M ∈ [0,1]` (clamp o assert).
   Hoy un valor negativo se cuela y distorsiona la base.
3. **(🟡) Coherencia del gate de tasks:** cambiar `be**2` → `be**P` (o documentar que el gate de tasks es
   fijo en 2 a propósito, independiente de `p` del ancla).
4. **(🟡) Corregir los ejemplos 1.4:** `2/4×60 = 0.544` (no 0.59); `[120×7] = 1.499` (no 1.46).
5. **(🟡) Matizar "27/27 valida el contrato":** el script no chequea AN12, VC4, SO6/TA6, O6, O9, PU2.
   O ampliarlo, o decir "27 axiomas clave" en vez de "TODO el contrato".
6. **(baja) BETA exacto:** documentar `BETA = 1/0.55 − 1 = 0.8182` si se quiere 0.55 clavado.

**Conclusión:** la matemática del núcleo es correcta y autoconsistente; doc-code ≡ verif-code al bit; el
27/27 es real. Los problemas son de **borde de implementación** (crashes ante input ilegal, falta de
clamps) y de **prosa** (dos ejemplos mal, una afirmación de cobertura optimista). Nada que invalide el
modelo; sí cosas que arreglar antes de copiar el código a la app.
