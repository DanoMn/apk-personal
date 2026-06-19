# Modelo consolidado v3 — pesos variables de capa + soportes + tasks

> **Estado: diseño consolidado de sesión (2026-06-16). NO es contrato oficial todavía** — faltan
> calibrar 4 números finos (§7) y la aprobación final para integrarlo a los docs vivos (§8).
> **Supersede a `merge-v2-consolidado.md`** en el **peso de capa** (v2 tenía una fórmula de masa con
> bug que hacía crecer el peso con la cantidad de anclas de forma incorrecta). El resto de v2
> (soportes blend, tasks saturación conjunta) se mantiene y se reconfirma acá.
> Verificación reproducible: `consolidado_explora.py` (+ `merge_v2_verificacion.py` para soportes/tasks).

---

## 0. Qué se decidió en esta sesión (el recorrido)

1. **Forma A confirmada**: soportes y tasks entran **DENTRO del valor de cada capa**, no como sombra
   global. (Corrige el merge v1, que era sombra global.)
2. **Peso de capa VARIABLE por cantidad de anclas** (decisión nueva del dueño): una capa con más
   anclas pesa más (es más importante para el usuario), **con freno** para no opacar a las demás. Esto
   reemplaza el "todas las capas pesan 1/N" de la filosofía de la mesa. **`r=0.5` CONFIRMADO
   (2026-06-16):** cada ancla nueva vale la mitad → techo de capa 2.0 → **ninguna capa decide más del
   50% del score** (límite de la filosofía de la mesa). Las anclas DENTRO de la capa pesan igual.
3. **Capa solo-soportes** (sin anclas): existe (el usuario puede agregar soportes a una capa sin
   anclas), y debe **pesar menos**. Al agregar 1 ancla, el peso se normaliza.
4. **Superhabit = Opción 2**: la **base** se pondera por peso de capa; el **superhabit** se reparte
   **plano** entre capas → mantiene `Sol=Tin` (un esfuerzo extra rinde igual en cualquier capa).
5. **Forma 1**: superhabit/déficit de una ancla se **promedia dentro de la capa** (÷ nº de anclas de
   esa capa). Una capa brilla cuando brillás en todas sus actividades; un bache en una sola se diluye.
6. **Aclaración (no cambio)**: la fórmula del ancla `R` ya maneja **superhabit de tiempo (`St`)** y
   **de días (`Sd`)** por separado y los combina. No se toca. El `extra` ya trae ambos adentro.

---

## 1. Marco cerrado (no se toca)

- **Ancla** `R = base + base²·S`, `R ∈ [0, 1.5]`:
  - `base ∈ [0,1]` = ¿cumpliste frecuencia y tiempo? (saturada).
  - `S = smax·(1−exp(−(wt·St + (1−wt)·Sd)/s0))`, `smax=0.5` = superhabit saturado, techo 0.5.
    - `St` = superhabit de **tiempo** (te pasaste de minutos en los días core).
    - `Sd` = superhabit de **días** (hiciste más días que tu meta de frecuencia).
    - `wt = (F/7)^kappa` = cuánto pesa tiempo vs días.
  - **gate `base²`** (`p=2`): sin cumplir, el superhabit casi no rinde (se usa para tapar lo que falta).
  - Parámetros: `gamma=1.5, lam_v=0.5, kappa=1.5, p=2.0, smax=0.5, s0=0.5`.
- **Bandas**: R<0.40 · A<0.62 · EM<0.85 · P<1.10 · I≥1.10. Cumplir-justo = 1.0 = Plenitud.
- **Opt-ins** (sueño/sobriedad): término-sombra global O1–O13. Feature **separada**, NO se mezcla con
  soportes/tasks. Viven en la **base**.

## 2. Por capa: las tres entradas

```
# anclas (lo que ya existe)
base_anc  = promedio de min(R, 1) de las anclas de la capa     # cumplimiento (de acá sale el déficit)
extra_anc = promedio de max(R−1, 0) de las anclas              # superhabit (tiempo+días ya combinados) — FORMA 1: promedio

# SOPORTES — blend leve en la base
G_s      = promedio_i( min(días_sostenidos_i / 4, 1) )          # ventana indulgente 4d; bloque = promedio (no crece con cantidad)
base_eff = (1 − WS)·base_anc + WS·G_s        [= G_s si la capa no tiene anclas]      WS = 0.07
                                              # bidireccional leve, solo base, nunca extra

# TASKS — empujón efímero diario al superhabit (saturación conjunta, dentro de la curva, gate base²)
su_anc   = −s0·ln(1 − extra_anc/0.5)
g_task   = 1 − exp(−n_hoy / N0)               # n_hoy = tasks completadas HOY (se resetea mañana)   N0 = 1.0
extra    = extra_anc + (0.5·(1−exp(−(su_anc + THETA·g_task)/s0)) − extra_anc) · base_eff²
                                              # THETA da lift máx = TAU (techo de task por capa)
```

## 3. Peso de capa (votos decrecientes por ancla) — lo nuevo de v3

```
voto de la k-ésima ancla = r^(k−1)            r = 0.5  CERRADO (2026-06-16)  (cada ancla nueva vale la MITAD de la anterior)
peso_capa(n anclas) = Σ_{k=0}^{n−1} r^k       # 1 → 1.00 · 2 → 1.50 · 3 → 1.75 · 4 → 1.875 · 5 → 1.94 · ... techo 2.0
peso_capa(0 anclas) = ρ = 0.35                # capa SOLO-soportes: peso reducido (se normaliza a peso normal al agregar 1 ancla)
```
- **Aclaración clave (cómo se lee `r`):** `r=0.5` NO es el tope; es la velocidad del freno (cada ancla
  nueva suma la mitad que la anterior). El **tope** sale solo de `r`: `1/(1−r) = 2.0` → una capa nunca
  pesa más de 2.0 (el doble de una capa de 1 ancla), meta las anclas que meta.
- **`r` es del peso de la CAPA, NO de las anclas individuales.** Dentro de la capa, todas las anclas
  pesan IGUAL (1/n del valor de capa, promedio simple). El "0.25" de la 3ª ancla es lo que suma tener
  una 3ª ancla al peso de la capa entera, no "el voto de esa ancla".
- **Límite filosófico (la mesa):** con `r=0.5` y el mínimo de 3 capas, **una capa nunca decide más del
  50% del score** (peor caso: 1 capa saturada vs 2 con 1 ancla → 2.0/4.0 = 50%). Con más capas, menos.
  Ninguna área puede secuestrar la mesa.

## 4. Score global: DOS cuentas que se suman

```
base_global  = Σ( min(base_eff,1) · peso_capa ) / Σ peso_capa        # PONDERADA por peso de capa
extra_global = promedio PLANO de extra_capa entre las capas con anclas   # PLANO (Opción 2 → Sol=Tin)
ESTADO       = base_global + extra_global
```
- **El peso de capa solo manda en la cuenta de la BASE** (cumplir en Cuerpo cuenta más si Cuerpo tiene
  más anclas).
- **El superhabit se reparte parejo** → pasarte rinde igual en cualquier capa (`Sol=Tin` se mantiene).

## 5. Lo que esto produce (verificado, `consolidado_explora.py`)

- **Cumplir todo al 100%** → ESTADO 1.0 = Plenitud (no es superhabit; superhabit es PASARSE).
- **Déficit**: duele según la profundidad y el peso, **y se diluye dentro de la capa** (un bache en 1 de
  3 anclas baja poco; un bache en una capa de 1 ancla baja toda la capa).
- **Superhabit**: brillar en TODA una capa rinde igual en cualquier capa (`Sol=Tin` ✓). Brillar en 1 de
  varias anclas rinde a pedazos (Forma 1).
- **Soporte**: mueve la base ±0.07 máx, no genera superhabit, 1 o 5 soportes pesan igual.
- **Task**: empuja el superhabit un toque, con techo, se borra al día siguiente.
- **Superhabit de tiempo y de días**: ambos contribuyen al `extra` vía `R` (caso `6 días×30` da extra
  0.266 por días; `4 días×60` da 0.289 por tiempo; juntos 0.401).

## 6. Mapa de scripts de verificación
- `merge_v2_verificacion.py` — soportes (blend) y tasks (saturación conjunta), candados (Sol=Tin, techo
  0.5, gate base², anti-abuso).
- `consolidado_explora.py` — peso variable por anclas + Opción 2 + Forma 1, casos de 5 capas con
  déficit/superhabit/soportes.
- `casos_limite_gen.py` → `casos-limite-soportes-tasks-v2.md` — casos límite (techo task, gate, etc.).

## 7. Parámetros — TODOS CALIBRADOS (2026-06-16, sobre el mapeo de puntos E)
1. `r` (peso por ancla) = **0.5** — techo de capa 2.0; ninguna capa supera el 50% del score.
2. `WS` (blend soporte) = **0.07** — descuidar los soportes de una capa ≈ **−13 pts**; sostenerlos en
   capa floja ≈ **+5**; bien en capa perfecta = neutro. Bidireccional leve.
3. `TAU` (techo task/capa) = **0.06** — 1 task ≈ **+8 pts**; tope absoluto **+41** (inviable de alcanzar);
   nunca compra Inquebrantable. El tope EMERGE de la saturación (no es regla).
4. `ρ` (peso capa solo-soportes) = **0.15**, **SIN piso** — abandonada vale 0 → arrastra ≈ **−25** (mismo
   orden que descuidar soportes reales, no infla cuando está bien). El peso bajo evita el impacto
   excesivo de forma natural, SIN regla de piso artificial.

Todo calibrado sobre los puntos E (resolución fina). **Listo para congelar `axiomas-soportes-tasks-v1.md`.**

## 8. Impacto futuro en la documentación oficial (cuando se cierre)
Al integrar este modelo, habrá que actualizar (son docs VIVOS):
- `docs/scoring/arbol-scoring-v1.md` — agregar peso de capa variable, soportes (blend), tasks (extra
  efímero), y la separación base-ponderada / superhabit-plano. **El §16-NUEVO de bandas no cambia.**
- `docs/scoring/axiomas-opt-in-v1.md` — referenciar que soportes/tasks son features separadas (no tocan O1–O13).
- Crear `docs/scoring/axiomas-soportes-tasks-v1.md` — contrato estilo O1–O13 con los números cerrados.
- `docs/scoring/plan-tecnico-scoring.md` — estado por fases.
- **NO cambia el esquema Room** (es lógica de dominio/scoring, no entidades nuevas).
