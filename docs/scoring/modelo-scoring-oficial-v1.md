# Modelo de scoring — DOCUMENTO OFICIAL v1

> **Estado: ✅ VIGENTE (congelado 2026-06-16) — FUENTE DE VERDAD ÚNICA del scoring.** Consolida todo el
> modelo de punta a punta. Supersede a `modelo-consolidado-v1/v2/v3`, `mapa-modelo-scoring-v1` y las
> exploraciones/merges (todos archivados en `docs/scoring/old/`). El contrato de comportamiento vive en
> `axiomas-modelo-scoring-v1.md`; este doc lo IMPLEMENTA y se VERIFICA contra él
> (`verificacion_modelo_oficial.py`). Fecha: 2026-06-16. Proyecto: `apk-personal`.
> **✅ VERIFICADO: 27/27 axiomas verdes** (`verificacion_modelo_oficial.py`, 2026-06-16).
> **📐 Matemática COMPLETA (todas las fórmulas de los 7 niveles + código ejecutable, el NÚCLEO):
> `modelo-matematico-nucleo-v1.md`.** Este doc es la descripción de alto nivel + filosofía + ejemplos;
> para el detalle matemático exacto de cada nivel (sobre todo el ancla), ir al núcleo.

---

## 1. Filosofía

**Motor de pesos puros.** El estado del usuario emerge de `peso × valor`, no de reglas-parche. Cero
gates/caps/worst-term duros. Todo comportamiento (que el sueño domine Cuerpo, que el esfuerzo se premie,
que abusar de tasks no compre Inquebrantable) **EMERGE** de la matemática; nada se fuerza.

**Local-first, hechos → dominio → estado.** Room guarda hechos; el dominio los convierte en ESTADO;
Compose solo renderiza. El motor es semanal (ventana móvil de 7 días) y dominio puro (no toca Room ni UI).

## 2. El pipeline en una vista

```
hechos (7 días)
  → R por ancla          (cumplimiento + superhabit de tiempo/días, gateado por base²)
  → valor de capa        (dos canales: base [0,1] + extra [0,0.5]; + soportes en base, + tasks en extra)
  → peso de capa         (votos por cantidad de anclas; capa solo-soportes pesa poco)
  → agregación global    (base PONDERADA por peso · extra PLANO entre capas)
  → ESTADO ∈ [0, 1.5]    (= min(base_global,1) + extra_global)
  → BANDA                (Restauración…Inquebrantable)
  → PUNTOS [650, 1100]   (mapeo E para mostrar en el dashboard)
```

## 3. Las tres superficies de actividad

- **Ancla**: práctica recurrente con metas (frecuencia + tiempo). Construye base; puede generar superhabit.
- **Soporte**: mantenimiento diario sin metas (UX inversa). Conserva base; nunca da superhabit.
- **Task**: pendiente puntual con capa. Empujón motivacional efímero; no toca el motor de estado salvo
  un toque al superhabit del día.

## 4. El ancla — `R(F, T, mins)`

```
R = base + base²·S            R ∈ [0, 1.5]
base = 1 − (1−φ)·exp(−λ_v·V)               φ = cumplimiento core; saturada en [0,1]
S    = smax·(1 − exp(−(wt·St + (1−wt)·Sd)/s0))   smax=0.5  (superhabit saturado, techo 0.5)
  St = superhabit de TIEMPO (pasarte de minutos en los días core)
  Sd = superhabit de DÍAS (hacer más días que tu meta de frecuencia)
  wt = (F/7)^κ               (cuánto pesa tiempo vs días)
Parámetros (cerrados): γ=1.5, λ_v=0.5, κ=1.5, p=2.0, smax=0.5, s0=0.5
```
- **Gate `base²` (p=2): sin cimiento no hay gloria.** Si no cumpliste la frecuencia, el exceso casi no
  cuenta (primero tapa lo que falta). Ej.: 3 días×60 (meta 4×30) → extra **0.000**; 4 días×60 → **0.289**.
- Maneja superhabit de **tiempo** (4×60 → +0.289) **y de días** (6×30 → +0.266), combinados.

## 5. Valor de capa — dos canales

```
base_capa  = promedio de min(R, 1) de las anclas        ∈ [0,1]   ("¿en pie?")
extra_capa = promedio de max(R−1, 0) de las anclas       ∈ [0,0.5] ("¿se destacó?")
valor_capa = min(base_capa, 1) + extra_capa
```
- **Anclas dentro de la capa pesan IGUAL** (promedio simple). Un déficit/superhabit en 1 de N anclas
  cuenta **1/N** (Forma 1): una capa brilla cuando brilla en todas sus anclas.

## 6. Peso de capa — votos por anclas

```
peso_capa(n anclas) = Σ_{k=0}^{n−1} r^k       r = 0.5   → 1:1.00 · 2:1.50 · 3:1.75 · 4:1.875 · techo 2.0
peso_capa(0 anclas) = ρ = 0.15                 (capa solo-soportes: peso reducido)
```
- Más anclas = capa más importante = pesa más, **con freno** (cada ancla nueva suma la mitad). El techo
  2.0 garantiza que **ninguna capa decida más del 50% del score** (peor caso, 3 capas).
- El peso es de la **capa**, no de las anclas individuales (dentro siguen iguales, §5).

## 7. Opt-ins — sueño y sobriedad (contrato O1–O13, VIGENTE)

Término-sombra **independiente** en la bolsa-global de la base: `w = BETA·Σpesos·(1−M)`, `BETA=0.818`
(generaliza el `BETA·N` de v4; `Σpesos` = suma de pesos de capa). Sueño→Cuerpo, sobriedad→Conducta. Neutro
cuando bien (M=1→w=0); arrastra cuando mal, **plano** (arrastre constante con cualquier config de peso).
Solo base, nunca extra. Señales: sueño continuo (sin dato `B_SLEEP=0.5`); sobriedad `M=(1−0.55)^días_recaída`.
**Feature separada de soportes/tasks.** Detalle completo: `axiomas-opt-in-v1.md`.
> ✅ **Verificado con el peso de capa variable** (I1/I2/I3, ver §10 y §15): arrastre plano (0.55 en cualquier
> config), opt-in global (capa pesada=liviana), capa solo-opt-in pesa normal, BETA intacto. Nada abierto acá.

## 8. Soportes — blend leve en la base

```
G_soporte = promedio de  min(días_sostenidos_i / 4, 1)        (ventana indulgente; no crece con cantidad)
base_eff  = (1 − WS)·base_anclas + WS·G_soporte               WS = 0.07   [= G_soporte si la capa no tiene anclas]
```
- Bidireccional **leve**: sostener (si hay margen) sube un toque, descuidar baja un toque, a la par =
  neutro. Solo base, nunca extra. 1 o 5 soportes pesan igual.
- **Cero fricción / UX inversa:** sin registro del día = **cumplido**; el usuario solo desmarca lo que NO
  hizo (la ausencia de dato no penaliza). Capa obligatoria, sin targets.
- En puntos: descuidar los soportes de una capa ≈ **−13**; sostener en capa floja ≈ **+5**.

## 9. Tasks — empujón efímero al superhabit

```
extra_con_task = extra_anclas + (saturación_conjunta(extra_anclas, n_tasks_hoy) − extra_anclas) · base_eff²
  saturación_conjunta re-satura por la MISMA curva del superhabit (techo 0.5), con techo propio TAU=0.06/capa
  n_tasks_hoy = tasks completadas HOY con capa (reset diario); task neutral/sin capa no cuenta
```
- Solo extra, dentro de la curva (respeta techo 0.5 y gate base²). **Efímera diaria.** Nunca resta.
- En puntos: 1 task ≈ **+8**; tope absoluto **+41** (inviable de alcanzar); **nunca compra Inquebrantable**
  (el tope emerge de la saturación, no es regla).

## 10. Agregación global — BOLSA-GLOBAL de la base + extra plano

La base es **UNA bolsa de términos ponderados** (no un promedio de valores de capa aislados — así se
integra el opt-in como término independiente, mecánica v4):
```
términos de la base:
   · cada capa con anclas → (base_eff_capa, peso_capa)            peso_capa = votos(n_anclas)
   · cada opt-in (en capa con anclas) → (M, BETA·Σpesos·(1−M))    ← término INDEPENDIENTE (global)
   · capa solo-soportes → (G_soporte, ρ=0.15)
   · capa solo-opt-in   → (M, W0=1)   peso normal (O11; NO ρ)
base_global  = Σ(valor·peso) / Σ(peso)   sobre TODOS los términos
extra_global = promedio simple de extra_capa entre capas CON anclas    (PLANO → Sol=Tin)
ESTADO       = min(base_global, 1) + extra_global                       ∈ [0, ~1.5]
```
- El opt-in escala con **Σpesos** (suma de pesos de capa), generalizando el `BETA·N` de v4 (cuando los
  pesos son iguales, Σpesos=N). Así el arrastre sigue **plano** (recaída total + anclas perfectas → 0.55
  con CUALQUIER config) y **BETA=0.818 se mantiene**. *Verif:* 0.5501 en 3 configs distintas.
- El opt-in es **global**, no atado al peso de su capa: mal sueño en capa pesada o liviana arrastra
  IGUAL. *Verif (I1):* 0.6514 en ambas.
- La **base** respeta el peso de capa (cumplir en una capa pesada cuenta más); el **superhabit** se
  reparte parejo (pasarte rinde igual en cualquier capa).

## 11. Estado y bandas

```
Restauración < 0.40 · Atención < 0.62 · En marcha < 0.85 · Plenitud < 1.10 · Inquebrantable ≥ 1.10
```
- **Plenitud entra en 0.85**; cumplir-justo (1.0) cae DENTRO de Plenitud (zona alta), no es su inicio.
- Sin gates/caps/worst-term. `Sol=Tin` y cumplir-justo=1.0=Plenitud se mantienen.

## 12. Mapeo a puntos visibles — enfoque E

```
ESTADO [0, 1.5]  →  PUNTOS [650, 1100]      (piso digno; tope que respira sobre 1000)
puntos(e) = 650 + reescala_afín( Σ_i Aᵢ·σ((e−cᵢ)/wᵢ) ) a [650,1100]      σ = sigmoide
hitos i (centro cᵢ, ancho wᵢ, aporte Aᵢ): 700(.18,.10,60) 800(.55,.11,110) 900(.83,.09,100)
                                          1000(1.07,.055,130) 1100(1.35,.13,50)
```
- **Hitos-meta perseguibles**: 700≈0.30 · 800≈0.65 · 900≈0.88 · **1000≈1.09**. La resolución se aprieta
  justo antes de cada número redondo. El **1000 se gana** al entrar a Inquebrantable (cumplir-justo = **941**).
- Continuo, monótono, **de a 1 punto** (aprovecha la resolución del estado).
- Hitos en los cortes: 0→650 · 0.40→721 · 0.62→788 · 0.85→873 · 1.0→941 · 1.10→1011 · 1.5→1100.

## 13. Ejemplo integrado (punta a punta, verificado) — CON los dos opt-ins activos

Usuario con 5 capas: anclas (superhabit de tiempo y días, déficit), soportes, capa solo-soportes, tasks,
**y los dos opt-ins activos** (sueño en Cuerpo, sobriedad en Conducta). La base es la **bolsa-global**:

| Término de la base | valor | peso |
|---|---|---|
| Interior (2 anclas justas) | 1.000 | 1.50 |
| Cuerpo (caminar superh. + comer + gym déficit + soportes flojos) | 0.844 | 1.75 |
| Conducta (orden digital justo) | 1.000 | 1.00 |
| Vínculos (**solo soportes**) | 0.750 | **0.15** |
| Proyecto (estudiar superh. + escribir + 3 tasks) | 1.000 | 1.50 |
| **OPT-IN sueño regular (M=0.6)** | 0.60 | **1.93** `= BETA·Σ·(1−M)` |
| **OPT-IN sobriedad 1 recaída (M=0.45)** | 0.45 | **2.65** `= BETA·Σ·(1−M)` |

(Σpesos de capa = 5.90. Los opt-ins entran como **términos independientes** en la bolsa, con peso grande.)
```
base_global  = Σ(valor·peso)/Σ(peso) = 0.7575
extra_global = promedio plano (capas con anclas) = 0.0635   (superhabit de caminar/estudiar + tasks)
ESTADO = 0.7575 + 0.0635 = 0.8210  →  EN MARCHA  →  862 PUNTOS
```
**El MISMO usuario con sueño bien y sobriedad limpia** (opt-ins invisibles, peso 0): ESTADO 1.0109 →
**PLENITUD → 948 PUNTOS**. Los dos opt-ins malos (sueño regular + 1 recaída) lo bajan de Plenitud a En
marcha (**−86 pts**); limpios, no pesan nada (neutralidad exacta).

Se ve TODO junto: el déficit del gym baja Cuerpo; el superhabit de caminar/estudiar se reparte parejo;
Vínculos (solo-soportes) pesa poco (0.15); las tasks suman un toque; y los **dos opt-ins arrastran la
base como términos independientes** (pesos 1.93 y 2.65) — o desaparecen si están limpios.

## 14. Parámetros (tabla canónica)

| Símbolo | Valor | Componente |
|---|---|---|
| γ, λ_v, κ, p, smax, s0 | 1.5, 0.5, 1.5, 2.0, 0.5, 0.5 | ancla R |
| BETA, A_sob, B_sleep | 0.818, 0.55, 0.5 | opt-ins |
| r | 0.5 | peso por ancla (votos) |
| ρ | 0.15 | peso capa solo-soportes |
| WS | 0.07 | blend soporte |
| TAU | 0.06 | techo task/capa |
| δ | 0.10 | margen Inquebrantable |
| piso / tope puntos | 650 / 1100 | mapeo E |

## 15. Riesgos / pendientes
1. ✅ **Interacciones opt-in × peso variable — RESUELTAS y verificadas** (`reconciliacion_optin_peso.py`):
   **I1** opt-in escala con Σpesos → arrastre plano y global (capa pesada=liviana); **I2** capa solo-opt-in
   pesa normal W0=1 (O11, no ρ); **I3** soporte+opt-in coexisten (blend local + término global). BETA intacto.
2. **Calibración aún sobre pocos axiomas de estado:** los parámetros se despejaron de comportamiento, no
   de un dataset amplio de marcas; afinables.
3. **Estabilidad temporal multi-semana** (`arbol §15`): ortogonal, sin reconciliar.
4. **Implementación en código:** este doc es el diseño; el código actual es el modelo VIEJO (deuda).

## 16. Qué supersede / referencias
- **Supersede como descripción:** `modelo-consolidado-v1/v2/v3`, `mapa-modelo-scoring-v1`, merges y
  exploraciones (quedan como histórico), y **`arbol-scoring-v1.md`** (modelo VIEJO del 2026-06-12,
  archivado en `old/`; donde decía "sueño = pilar CORE 30% / no opt-in" está SUPERADO — el sueño es opt-in).
- **Contrato de comportamiento:** `axiomas-modelo-scoring-v1.md` (este doc se verifica contra él).
- **Sigue vigente integrado:** `axiomas-opt-in-v1.md` (O1–O13), ancla en
  `old/exploracion-rendimiento-ancla/merge-consolidado.md`, mapeo E en `old/exploracion-puntos-visibles/opus-E-mapeo.md`.
