# Dataset de decisiones de estado — AI-facing (append-only)

<!--
PROPÓSITO: registro terso y codificado de las decisiones del dueño sobre qué estado
debe ver el usuario en cada caso. ESTE es el archivo que el agente LEE para inferir
patrones — NO las historias-*.md (grandes, humanas, para marcar). Append-only: cada
sesión agrega una tanda; no se reescriben tandas previas (registro histórico).
Mantener con la skill `scoring-dataset-decisiones`. Token-mínimo: códigos, no prosa.
-->

## Schema (legend)

`caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota`

- **cumpl**: % del plan cumplido (100 = todas las metas; <100 con déficit).
- **sup** (tipo de superávit): `0` ninguno · `D` días/frecuencia · `T` tiempo/minutos · `M` mixto.
- **mag** (tamaño del extra, grueso): `0 xs s m l xl`.
- **act**: nº de anclas con superávit/déficit (de las del plan).
- **cap**: nº de capas activas tocadas por el extra/déficit (de las activas).
- **forma**: `-` · `sost` sostenido · `pico` un solo día · `mix`.
- **sueño**: `ok` registrado bien · `no` sin registrar · `mal` malo.
- **→** (veredicto del dueño): `R` Restauración · `A` Atención · `EM` En marcha · `P` Plenitud · `I` Inquebrantable · `ND` NoData.

---

## Lote SH — superhabit, frontera P↔I

- **Fuente**: `docs/scoring/historias-superhabit-v1.md`
- **Config Juan** (3 capas activas): Interior(Meditar, Leer) · Cuerpo(Caminar + Sueño) · Conducta(Higiene digital).
- **Fijo en toda la tanda**: cumpl=100, sueño=ok. Varía solo el superávit.

| caso | cumpl | sup | mag | act | cap | forma | sueño | → |
|------|-------|-----|-----|-----|-----|-------|-------|---|
| SH-S0 | 100 | 0 | 0 | 0 | 0 | - | ok | P |
| SH-A1 | 100 | D | s | 1 | 1 | sost | ok | P |
| SH-A2 | 100 | D | s | 3 | 3 | sost | ok | I |
| SH-A3 | 100 | D | l | 1 | 1 | sost | ok | P |
| SH-A4 | 100 | D | l | 4 | 3 | sost | ok | I |
| SH-A5 | 100 | D | xl | 4 | 3 | sost | ok | I |
| SH-B1 | 100 | T | s | 1 | 1 | pico | ok | P |
| SH-B2 | 100 | T | m | 1 | 1 | sost | ok | P |
| SH-B3 | 100 | T | l | 4 | 3 | sost | ok | I |
| SH-B4 | 100 | T | l | 1 | 1 | pico | ok | P |
| SH-C1 | 100 | M | s | 2 | 2 | mix | ok | P |
| SH-C2 | 100 | M | m | 4 | 3 | sost | ok | I |
| SH-C3 | 100 | M | l | 4 | 3 | sost | ok | I |

### Patrón inferido (P↔I) — confianza ALTA (separación perfecta, n=13)

- **→ I ⟺ `cap`=3** (el superávit toca TODAS las capas activas).
- **→ P ⟺ `cap`≤2** (concentrado en 1–2 capas), sin importar `mag`, `sup` ni `forma`.
- Contraejemplos que lo confirman: `SH-A3` (D, l, cap1)→P pese a +3 días; `SH-B3` (T, l, cap3)→I aunque sea solo tiempo; `SH-B2`/`SH-B4` (tiempo grande, cap1)→P.
- **Variable decisiva = COBERTURA DE CAPAS.** Magnitud, tipo (días/tiempo) y forma NO deciden la frontera P↔I.
- Pendiente de validar con otra config (¿4–5 capas activas? ¿anclas múltiples por capa?).

---

## Lote CB — estados base por déficit (R/A/EM/P)

- **Fuente**: `docs/scoring/historias-estados-base-v1.md`
- **Config Juan**: igual que lote SH (3 capas, 4 anclas). `cap`/`act` aquí = nº de capas/anclas EN DÉFICIT (o con superávit en T04).

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| CB-C01 | 0 | 0 | - | 4 | 3 | sost | ok | R | abandono total |
| CB-C02 | 7 | 0 | - | 4 | 3 | sost | ok | R | 1 sola sesión salvada |
| CB-C03 | 29 | 0 | - | 4 | 3 | sost | ok | A | 1 por ancla (parejo) |
| CB-C04 | 43 | 0 | - | 4 | 3 | sost | ok | A | ~mitad parejo |
| CB-C05 | 71 | 0 | - | 4 | 3 | sost | ok | EM | ~75% parejo |
| CB-C06 | 93 | 0 | - | 1 | 1 | sost | ok | P | falló 1 marca |
| CB-C07 | 100 | 0 | 0 | 0 | 0 | - | ok | P | exacto |
| CB-T01 | 71 | 0 | - | 1 | 1 | sost | ok | EM | capa Cuerpo≈0.27 (Caminar 0 + Sueño ok) → NO colapsa a R |
| CB-T02 | 100 | 0 | 0 | 0 | 0 | - | no | EM | sin sueño → tope EM |
| CB-T03 | 50 | 0 | - | 2 | 2 | mix | ok | A | 2 anclas en 0 + 2 perfectas |
| CB-T04 | 43 | D | xl | 1sup+3def | 3 | pico | ok | R | superávit máx en 1 ancla; otras 3 al 25% → R |

### Patrón inferido (estados base) — confianza MEDIA (n=11, bandas anchas)

- **El estado base sigue el % de cumplimiento GLOBAL (constancia repartida):**
  - R ≈ 0–15% · A ≈ 29–50% · EM ≈ 71% · P ≈ 93–100%.
  - Cortes aún ANCHOS (a refinar): R|A en 16–28% · A|EM en 51–70% · EM|P en 72–92%.
- **La peor capa NO colapsa el estado** (CB-T01: una capa ≈0.27 con resto perfecto → EM, sigue su % global 71%, NO R). ⚠️ Contradice `WORST_LAYER_COLLAPSE < 0.30` del motor actual → señal de rediseño.
- **Sin sueño → tope En marcha** (CB-T02 → EM). Valida el sleep cap actual.
- **Superávit localizado NO rescata desde abajo** (CB-T04 → R pese a +4 días en 1 ancla). Confirma "constancia=esqueleto, superávit=músculo" y el caso parkeado [[scoring/parked-riesgo-mas-superavit]]. Además, a igual % (43%), más anclas casi-abandonadas (T04) cae más bajo (R) que déficit parejo (C04→A): la **cobertura del déficit** importa.

### Meta-patrón (cruza lotes SH + CB) — confianza MEDIA-ALTA

> **La COBERTURA / repartición por todo el sistema domina en los dos extremos.**
> Arriba: para subir a Inquebrantable el superávit debe cubrir TODAS las capas.
> Abajo: el déficit que abandona la mayoría de las capas tira a Restauración.
> Lo concentrado (superávit o déficit en un rincón) NO mueve la frontera.

---

## Lote REF — refinamiento de cortes + gradientes (R/A/EM/P)

- **Fuente**: `docs/scoring/historias-refinamiento-v1.md` · Config Juan (3 capas, 4 anclas, 14 marcas/sem).
- `cumpl` = % de marcas global. En WC/RS, `nota` aclara la concentración.

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| REF-BR1 | 14 | 0 | - | 4 | 3 | sost | ok | R | parejo |
| REF-BR2 | 21 | 0 | - | 4 | 3 | sost | ok | R | parejo |
| REF-BR3 | 29 | 0 | - | 4 | 3 | sost | ok | A | parejo |
| REF-BA1 | 57 | 0 | - | 4 | 3 | sost | ok | EM | parejo |
| REF-BA2 | 64 | 0 | - | 4 | 3 | sost | ok | EM | parejo |
| REF-BA3 | 71 | 0 | - | 4 | 3 | sost | ok | EM | parejo |
| REF-BE1 | 79 | 0 | - | 3 | 3 | sost | ok | EM | parejo |
| REF-BE2 | 86 | 0 | - | 2 | 2 | sost | ok | P | parejo |
| REF-BE3 | 93 | 0 | - | 1 | 1 | sost | ok | P | parejo |
| REF-WC1 | 79 | 0 | - | 1 | 1 | sost | ok | EM | Conducta a 0 → NO colapsa (=BE1 mismo %) |
| REF-WC2 | 86 | 0 | - | 1 | 1 | sost | ok | EM | déficit en Cuerpo → EM < BE2 parejo 86%→P |
| REF-WC3 | 71 | 0 | - | 2 | 2 | sost | ok | EM | 2 capas a la mitad → EM (= su % global) |
| REF-RS1 | 64* | D | xl | 1+3 | 3 | pico | ok | A | superávit en 1; resto ~½ → A (< parejo 64%→EM) |
| REF-RS2 | 43* | D | xl | 1+3 | 3 | pico | ok | A | superávit en 1; resto ~⅓ → A |

(* RS = % de la línea base, sin contar el superávit.)

### Patrón inferido (cortes) — confianza ALTA, bandas tightened

A 14 marcas/sem el corte cae casi en el nº de marcas:
- **R | A**: ≤3 marcas (~21%) → R · 4 marcas (~29%) → A. **Corte ≈ 22–28%.**
- **A | EM**: 6 (~43%) → A · 8 (~57%) → EM. **Corte ≈ 50% (zona 44–56% aún por pinchar).**
- **EM | P**: 11 (~79%) → EM · 12 parejo (~86%) → P. **Corte ≈ 80–85%.**

### Patrón inferido (concentración) — confianza MEDIA, NUEVO

- **Una capa a 0 NO colapsa** (REF-WC1: Conducta=0, 79% global → EM, igual que parejo). Reconfirma el anti-`WORST_LAYER_COLLAPSE`.
- **PERO cerca del techo, una capa floja tira para abajo**: REF-WC2 (86% global pero déficit metido en Cuerpo) → **EM**, mientras BE2 (86% parejo) → **P**. ⇒ a igual %, déficit concentrado en una capa = un escalón menos cerca de P.
- **Superávit localizado no rescata y hasta penaliza el desbalance**: REF-RS1 (64% base + superávit máx en 1) → A, un escalón bajo el 64% parejo (EM).

### Meta-patrón refinado (cruza SH + CB + REF)

> El sistema premia el **reparto parejo**. A igual nivel global, lo CONCENTRADO (superávit
> o déficit en un rincón) tiende a un estado **más bajo** que lo parejo. Arriba: Inquebrantable
> exige superávit en TODAS las capas. Abajo: el superávit en una sola no rescata. En el medio:
> una capa floja cerca de un corte baja un escalón.

---

## Lote REFv2 — concentración vs parejo al mismo % (R/A/EM/P)

- **Fuente**: `docs/scoring/historias-refinamiento-v2.md` · Config Juan.

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| RV2-A1 | 50 | 0 | - | 4 | 3 | parejo | ok | EM | referencia parejo |
| RV2-A2 | 50 | 0 | - | 2 | 2 | conc | ok | A | Interior full, Cuerpo+Conducta=0 → baja a A |
| RV2-A3 | 50 | 0 | - | 3 | 2 | conc | ok | A | Cuerpo full, resto flojo → A |
| RV2-B1 | 86 | 0 | - | 2 | 2 | parejo | ok | P | referencia parejo |
| RV2-B2 | 86 | 0 | - | 1 | 1 | conc | ok | EM | déficit en **Cuerpo** → bloquea P (baja a EM) |
| RV2-B3 | 86 | 0 | - | 1 | 1 | conc | ok | P | déficit en **Conducta** → NO baja (sigue P) |
| RV2-C1 | 64 | 0 | - | 4 | 3 | parejo | ok | EM | referencia |
| RV2-C2 | 64 | 0 | - | 2 | 2 | conc | ok | EM | Conducta=0 + Cuerpo½ → NO baja (sigue EM) |
| RV2-D1 | 46 | 0 | - | 4 | 3 | parejo | ok | A | |
| RV2-D2 | 57 | 0 | - | 4 | 3 | parejo | ok | EM | |

### Patrón inferido (concentración) — REFINADO, confianza MEDIA-ALTA

- **Cortes parejos confirmados**: A|EM ≈ 50% (D1 46%→A, D2 57%→EM); EM|P 86% parejo→P.
- **Al ~50%, concentrar BAJA un escalón** (EM→A): A2, A3. Abandonar capas castiga abajo.
- **Al ~64%, concentrar NO baja** (C2 sigue EM). En la zona media el % global manda.
- **Al ~86% (techo), depende de QUÉ capa está floja**: déficit en **Cuerpo** bloquea Plenitud (B2→EM); déficit en **Conducta** NO (B3→P, aun con Conducta a ⅓).

### HIPÓTESIS FUERTE NUEVA — Cuerpo es la capa "load-bearing"

> No todas las capas pesan igual. **Cuerpo (que aloja el Sueño, "piso del scoring") es base portante**: un Cuerpo flojo BLOQUEA Plenitud aunque el % global sea alto. Una Conducta floja no. Coherente con el dominio (sueño = piso). **A validar**: probar Interior flojo al tope, y el sueño como palanca propia. Si se confirma, el rediseño debe ponderar Cuerpo/sueño por encima del resto cerca de P.

### Meta-patrón (vigente)

> Estado ≈ % global, con DOS correcciones: (1) cerca del techo, un **Cuerpo (base) flojo** bloquea Plenitud; (2) en niveles medios-bajos, **abandonar capas** baja un escalón. El sistema premia reparto parejo + base sólida.

---

## Lote REFv3 — Cuerpo load-bearing + sueño como palanca (R/A/EM/P)

- **Fuente**: `docs/scoring/historias-refinamiento-v3.md` · Config Juan.

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| RV3-K1 | 86 | 0 | - | 1 | 1 | conc | ok | P | Interior flojo (Meditar) → NO bloquea P |
| RV3-K2 | 86 | 0 | - | 1 | 1 | conc | ok | P | Interior flojo (Leer) → NO bloquea P |
| RV3-S1 | 100 | 0 | - | 0 | 0 | - | mal | EM | anclas 100% pero sueño MAL → tope EM |
| RV3-S2 | 86 | 0 | - | 2 | 2 | parejo | mal | EM | 86% + sueño mal → EM |
| RV3-S3 | 100 | 0 | - | 0 | 0 | - | no | EM | sueño NO registrado → EM (= S1) |
| RV3-J1 | 64 | 0 | - | 1 | 1 | conc | ok | A | Cuerpo a 0 → baja a A (< 64% parejo EM) |
| RV3-J2 | 64 | 0 | - | 1 | 1 | conc | ok | EM | Interior débil → NO baja (sigue EM) |
| RV3-J3 | 50 | 0 | - | 1 | 1 | conc | ok | A | Interior a 0 (concentrado) → A (regla 50%-conc) |

### Patrón CONFIRMADO (datos) — el peso del sueño/Cuerpo domina cerca de Plenitud

- **Al 86%, solo aflojar Cuerpo te saca de Plenitud.** Interior flojo (K1,K2)→P · Conducta floja (RV2-B3)→P · Cuerpo flojo (RV2-B2)→EM.
- **Sueño MAL o NO registrado → En marcha aunque las anclas estén al 100%** (S1, S2, S3). Mal y no-registrado dieron IGUAL.
- **Cuerpo arrastra también abajo.** Al 64%, Cuerpo a 0 → A (J1); Interior débil → EM (J2); Conducta a 0 tampoco bajaba (RV2-C2).
- **Concentración al 50% baja un escalón** sin importar la capa (J3).

### LEY DE PLENITUD — CORREGIDA por el dueño: PESOS, no gates/caps

> El dueño RECHAZÓ el framing "gate/cap/portón". El sistema debe ser preciso e inteligente
> para reflejar el estado mental del usuario, y **las bases (bien ponderadas) regulan los
> estados solas, SIN ninguna regla extra**. Lo de arriba NO es un cap: es el efecto natural
> de los pesos.
>
> Traducción de los datos a PESOS:
> - **Sueño = el peso más pesado del sistema.** Razón del dueño: sin buen sueño las capas
>   caen como dominó (de a poco). Por eso una semana de mal sueño puntúa NATURALMENTE en En
>   marcha — no por un cap, sino porque pesa mucho.
> - **Cuerpo pesa más que las otras capas, pero porque ALOJA al sueño.** El ejercicio
>   (Caminar) NO es lo pesado; el sueño sí.
> - Interior/Conducta pesan menos: aflojar ahí no baja de Plenitud porque su peso no alcanza
>   a mover el agregado bajo el umbral.
> - El estado sale del **agregado ponderado + cortes** (R|A ~22-28%, A|EM ~50%, EM|P ~85%).

### Severidad del sueño (matiz del dueño)

- **No dormir debería pesar MÁS que dormir mal.** Pero hoy el sueño no se mide con precisión,
  así que por ahora se tratan parecido (dato de baja confianza). Revisar cuando mejore la medición.

### Implicancias de rediseño (PESOS, no reglas-parche)

- **Quitar las reglas-parche**: `sleep cap` (topa EM), `WORST_LAYER_COLLAPSE`, y toda idea de "gate".
  El estado = lectura natural del agregado ponderado + cortes.
- **Recalibrar pesos**: sueño = el más pesado; Cuerpo por encima del resto (vía sueño). Calibrar
  hasta que el score natural caiga donde el dueño ya marcó.
- La peor capa NO colapsa a Restauración (lote CB) — sale solo si no hay reglas de colapso.

---

## Estado del mapeo — pivot a razonamiento del dueño

El mapa conductual está **sólido y SIN contradicciones reales** (las marcas son coherentes
entre lotes). Lo que falta ya NO es más marcar, sino el **por qué** de dos decisiones de
diseño (Cuerpo-portón y severidad del sueño) — pendiente de la opinión directa del dueño.

## Próximos pasos posibles

- Recoger el razonamiento del dueño sobre el portón Cuerpo/Sueño (decisión de diseño).
- (Opcional) Validar cobertura P↔I con 4–5 capas / varias anclas por capa.
- Traducir el mapa a fórmula (cortes + ley de Plenitud + anti-colapso).

---

## Modelo de pesos — BASE (3 capas, solo anclas) — validado 40/43 (93%)

ALCANCE (IMPORTANTE): **NO es el motor completo.** Es la calibración de (a) dónde caen
los estados sobre un score 0–1 y (b) el peso relativo de las capas base + rol del sueño.
Fit con Juan (3 capas, sin soportes, sin sobriedad, sin Vínculos/Proyecto).
Script: `scripts/scoring/weight_model_fit.py`.

- El modelo pesa **CAPAS**, no actividades concretas. Cada capa = promedio de SUS anclas (las que el usuario eligió). Meditar/Leer/Caminar/Higiene son solo las anclas de ejemplo del usuario de prueba "Juan" — intercambiables por cualquier ancla de esa capa.
  - `Interior = avg(anclas de Interior)` · `Cuerpo = 0.5·sueño + 0.5·avg(anclas de Cuerpo)` · `Conducta = avg(anclas de Conducta)`
- `score = (1-ω)·(wCu·Cu + wI·I + wCo·Co) + ω·min(I,Cu)`  (Conducta NO entra al peor-capa)
- `wCu=0.50, wI=0.25, wCo=0.25, ω=0.20` · cortes `R<0.40  A<0.64  EM<0.84  P≥0.84` · sueño mal/ausente=0.15
- **40/43 (93%).** Fallos: T04 (inconsistencia con RS2 — mismo config, R vs A), RS1 (desbalance superávit), A1 (borde 50%). Ninguno es falla del modelo.
- **B2/WC2 CORREGIDOS por el dueño a P** (Caminar-mitad + sueño perfecto = Plenitud; el sueño domina Cuerpo).
- El modelo reproduce la NO-LINEALIDAD sin reglas: Caminar-mitad→P (B2) y Caminar-cero→arrastra a A (J1) salen solos del término peor-capa + el % global. No hizo falta caso especial.
- ⚠️ Pesos por capa = 3-capa-específicos; NO extrapolan a 5. Generaliza la ESTRUCTURA (Cuerpo dominante, Conducta liviana, peor-capa solo portantes, cortes 0–1).
- ⚠️ RESIDUAL: el dueño quiere SUEÑO estrictamente > toda actividad. El dato cae mejor con β=0.5
  (sueño = Caminar DENTRO de Cuerpo, 0.25 efectivo c/u; Cuerpo total 0.5 = capa dominante). Forzar
  β>0.5 baja a 38/43 (las marcas J1/T03 "ejercicio abandonado arrastra" resisten). A reconciliar
  cuando entre la métrica real de sueño + las 5 capas.

## Principio de MODULACIÓN DE PESOS (sueño + sobriedad) — decisión del dueño

> El motor NO pone límites/reglas fuera de lo que emerge de las capas y sus pesos. SOLO dos
> cosas modulan el peso de una capa:
> 1. **Sueño** → eleva el peso de **Cuerpo** (y es el pesado dentro de Cuerpo).
> 2. **Sobriedad activa** → eleva el peso de **Conducta** sobre el Score total (simétrico).
>
> Una **recaída** "pega a todo el Score" NO por una regla: con sobriedad activa Conducta pesa
> más Y la recaída le hunde el puntaje → el golpe EMERGE. Nada más modula pesos. Los pesos son
> **dinámicos** según qué features están activos.
> Pendiente de calibrar (con casos): cuánto sube Conducta al activar sobriedad, cuánto baja una recaída.

## Decisión: SUEÑO OPT-IN → re-anclar la base SIN sueño

El sueño pasa a ser **opt-in** (quita fricción). Reencuadre: la **base pura (solo anclas,
sin sueño/sobriedad) tiene capas ~PAREJAS**; sueño y sobriedad son los **únicos moduladores**
(sueño infla Cuerpo, sobriedad infla Conducta). El modelo base previo (wCu=0.5, 40/43) era la
instancia "sueño activo" → hay que **re-calibrar la base sin sueño**; los casos de sueño ya
marcados pasan a calibrar el modulador. Distinción: "nunca activó sueño" = neutro (no castiga,
= ADR-3); "activó pero no registró / durmió mal" = sí pesa.

## Orden de calibración (revisado) — estado al 2026-06-08

1. **Base pura sin sueño** (5 capas) → ✅ DESTILADO (lote BP). Promedio PAREJO, sin worst-term.
2. **Sueño modulador** (opt-in) → ✅ DESTILADO (lote SU). Domina Cuerpo, pesa en el techo.
3. **Sobriedad modulador + recaída** → ✅ DESTILADO (lote SBR). Racha binaria, recaída plana.
4. **Soportes y tasks** → ✅ DESTILADO (lote SO). **CORRECCIÓN: los soportes SÍ mueven el estado**
   (no era 0.80/0.20 inofensivo). Tasks neutras.
5. **Inquebrantable** (superhabit) → ✅ DESTILADO (lote IN). Umbral **≥2 capas** (revisa "todas las capas").

---

# TANDA 5-CAPAS + MODULADORES (2026-06-08) — destilado

> Cinco lotes nuevos. Reencuadre: **base pura = capas PAREJAS**; sueño/sobriedad/soportes
> modulan. Inquebrantable re-calibrado a ≥2 capas. Schema igual; `cumpl` = % global del plan;
> los moduladores van en `nota` (el schema base no tiene columna de sueño-off / sobriedad / soporte).

## Lote BP — base pura, 5 capas, SIN moduladores (R/A/EM/P)

- **Fuente**: `docs/scoring/historias-base-pura-5capas-v1.md` · Config Ana: 5 capas (Interior, Cuerpo,
  Conducta, Vínculos, Proyecto), 1 ancla c/u, meta 4d. **Sueño OFF, sobriedad OFF** (base pura).
- `act`/`cap` = nº de capas en déficit. `sueño=off` (opt-out, no aplica).

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| BP-AP1 | 25 | 0 | - | 5 | 5 | sost | off | R | 1/4 en las 5 capas |
| BP-AP2 | 50 | 0 | - | 5 | 5 | sost | off | A | 2/4 en las 5 |
| BP-AP3 | 75 | 0 | - | 5 | 5 | sost | off | EM | 3/4 en las 5 |
| BP-AP4 | 100 | 0 | 0 | 0 | 0 | - | off | P | exacto |
| BP-AP5 | 90 | 0 | - | 1 | 1 | conc | off | P | 4 capas 100% + Proyecto 50% |
| BP-AC1 | 80 | 0 | - | 1 | 1 | conc | off | EM | Cuerpo MUERTO, resto 100% |
| BP-AC2 | 80 | 0 | - | 1 | 1 | conc | off | EM | Interior muerto, resto 100% |
| BP-AC3 | 80 | 0 | - | 1 | 1 | conc | off | EM | Vínculos muerto, resto 100% |
| BP-AC4 | 80 | 0 | - | 1 | 1 | conc | off | EM | Proyecto muerto, resto 100% |
| BP-AN3 | 75 | 0 | - | 3 | 3 | sost | off | EM | solo 3 capas activas, c/u 75% |
| BP-AN4 | 75 | 0 | - | 4 | 4 | sost | off | EM | 4 capas activas, c/u 75% |

### Patrón inferido (base pura) — confianza ALTA (11/11, separación limpia)

- **El estado base = PROMEDIO PLANO de las fracs de capa.** Cortes confirmados: `R<0.40 · A<0.64 ·
  EM<0.84 · P≥0.84` (25→R, 50→A, 75/80→EM, 90/100→P).
- **NO hay worst-term en base pura** (ω=0): BP-AC (una capa a 0, global 80%) → EM, NO arrastra abajo;
  BP-AP5 (una capa a 50%, global 90%) → P, NO la frena. La peor capa no colapsa.
- **Da igual QUÉ capa cae** (AC1=AC2=AC3=AC4=EM) y **da igual cuántas capas activas** (AN3=AN4=AP3=EM
  al 75%). Capas PAREJAS, sin capa especial.
- **Consecuencia de modelo**: el `min(I,Cu)` y el peso pesado de Cuerpo del modelo "sueño-on" eran
  EFECTO de los moduladores, NO de la base. La base se re-ancla a pesos iguales + ω=0.

## Lote SU — sueño modulador, 3 capas, sueño OPT-IN activo (R/A/EM/P)

- **Fuente**: `docs/scoring/historias-sueno-modulador-v1.md` · Config Sol: 3 capas (Interior/Leer,
  Cuerpo/Caminar, Conducta/Orden), meta 4d. **Sueño activo**; sobriedad off.

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| SU1 | 100 | 0 | 0 | 0 | 0 | - | ok | P | anclas 100% |
| SU2 | 100 | 0 | 0 | 0 | 0 | - | mal | EM | 100% pero sueño MAL → cae a EM |
| SU3 | 100 | 0 | 0 | 0 | 0 | - | no | EM | 100% pero sueño NO registrado → EM (=SU2) |
| SU4 | 75 | 0 | - | 3 | 3 | sost | ok | EM | sueño bueno no rescata 75% |
| SU5 | 75 | 0 | - | 3 | 3 | sost | mal | EM | sueño malo no hunde más (=SU4) |
| SU6 | 50 | 0 | - | 3 | 3 | sost | ok | A | sueño bueno no rescata 50% |
| SU7 | 50 | 0 | - | 3 | 3 | sost | mal | A | (=SU6) |
| SU8 | 83 | 0 | - | 1 | 1 | conc | ok | P | Caminar 50% (resto 100%) + sueño ok → sigue P |
| SU9 | 67 | 0 | - | 1 | 1 | conc | ok | EM | Caminar 0% (resto 100%) + sueño ok → EM |

### Patrón inferido (sueño modulador) — confianza ALTA (9/9)

- **El sueño pesa en el TECHO**: con anclas 100%, sueño MAL o NO registrado saca de Plenitud → EM
  (SU2, SU3). Mal = no-registrado (mismo veredicto). Un modulador activo sin dato topea en EM (=lote SBR SB10).
- **El sueño bueno DOMINA Cuerpo**: Caminar a la mitad (SU8) sigue P; Caminar a 0 (SU9) baja a EM
  pero NO más — el sueño sostiene la capa Cuerpo aunque el ejercicio se caiga.
- **El sueño NO rescata anclas flojas en zona media/baja**: SU4 (75%+sueño ok)=EM, SU6 (50%+ok)=A —
  igual que la base sin sueño. Y el sueño malo tampoco hunde más ahí (SU4=SU5, SU6=SU7).
- **Variable decisiva**: cerca del techo, la calidad del sueño define P vs EM. En el resto, manda el %.

## Lote SBR — sobriedad modulador + recaída, 3 capas (R/A/EM/P)

- **Fuente**: `docs/scoring/historias-sobriedad-modulador-v1.md` · Config Beto: 3 capas (Interior/Leer,
  Cuerpo/Caminar, Conducta/Orden), meta 4d. **Sobriedad activa** (modula Conducta); sueño off.
- `nota` lleva el estado de sobriedad: limpia(largo) / recaída(largo roto) / no-marcó.

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| SB1 | 100 | 0 | 0 | 0 | 0 | - | off | P | racha 5 días, limpia |
| SB2 | 100 | 0 | 0 | 0 | 0 | - | off | P | racha 3 semanas, limpia |
| SB3 | 100 | 0 | 0 | 0 | 0 | - | off | P | racha 6 meses, limpia |
| SB4 | 100 | 0 | - | 0 | 0 | - | off | EM | RECAÍDA (venía 5 días) |
| SB5 | 100 | 0 | - | 0 | 0 | - | off | EM | RECAÍDA (venía 3 semanas) |
| SB6 | 100 | 0 | - | 0 | 0 | - | off | EM | RECAÍDA (venía 6 meses) |
| SB7 | 50 | 0 | - | 3 | 3 | sost | off | A | 50% + racha 6 meses limpia |
| SB8 | 50 | 0 | - | 3 | 3 | sost | off | A | 50% + RECAÍDA (6 meses) |
| SB9 | 75 | 0 | - | 1 | 1 | conc | off | A | Interior+Cuerpo 100%, Conducta-ancla 25%, limpia 6m |
| SB10 | 100 | 0 | 0 | 0 | 0 | - | off | EM | racha 3 sem · NO marcó (ventana perdón) |

### Patrón inferido (sobriedad modulador) — confianza ALTA (10/10)

- **El LARGO de la racha es BINARIO para el estado semanal**: SB1=SB2=SB3=P (5 días / 3 sem / 6 meses
  dan IGUAL con anclas 100%). El largo se premia EN la feature de sobriedad (contador/hitos), NO en el estado.
- **RECAÍDA = golpe plano de ~un escalón, INDEPENDIENTE del largo roto**: SB4=SB5=SB6=EM (P→EM, da igual
  de qué racha venías). **Contesta la decisión abierta #1: la racha larga NO protege NI amplifica.** Binaria.
- **En zona baja (50%) la sobriedad no rescata ni hunde extra**: SB7 (limpia)=A, SB8 (recaída)=A (floor).
- **SB9 = la modulación al desnudo**: Conducta-ancla floja (25%) con sobriedad activa → A, un escalón BAJO
  el 75% parejo (que sería EM, cf. BP-AP3). Con sobriedad activa Conducta pesa MÁS → su flojera arrastra más.
- **SB10**: modulador activo sin marcar (ventana perdón) → topea EM, no asume recaída (= sueño no-registrado).
- **Variable decisiva**: held/broke (binario) + el peso extra que la sobriedad activa le da a Conducta.

## Lote SO — soportes y tasks, 3 capas + sueño (R/A/EM/P) — RE-MARCADO polaridad normal

- **Fuente**: `docs/scoring/historias-soportes-tasks-v1.md` (v2, polaridad normal) · Config Caro: 3 capas,
  meta 4d. **Sueño activo y bien (fijo)**. 1 soporte por capa. Tasks solo en SO7.
- ⚠️ **Reemplaza la lectura previa.** La primera marca (polaridad inversa mal leída) decía "soportes no
  mueven". FALSO: re-marcado en positivo, los soportes SÍ mueven. `nota` lleva días sostenidos (de 7).

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| SO1 | 100 | 0 | 0 | 0 | 0 | - | ok | P | soportes 7/7 (full) |
| SO2 | 100 | 0 | 0 | 0 | 0 | - | ok | EM | soportes 2/7 (descuidados) → P→EM |
| SO3 | 75 | 0 | - | 3 | 3 | sost | ok | EM | soportes 7/7 |
| SO4 | 75 | 0 | - | 3 | 3 | sost | ok | EM | soportes 2/7 (=SO3, banda media no mueve) |
| SO5 | 50 | 0 | - | 3 | 3 | sost | ok | EM | soportes 7/7 → sube 50% a EM |
| SO6 | 50 | 0 | - | 3 | 3 | sost | ok | A | soportes 2/7 → baja a A |
| SO7 | 75 | 0 | - | 3 | 3 | sost | ok | EM | soportes 7/7 + 3 TASKS con capa (=SO3) |

### Patrón inferido (soportes + tasks) — confianza MEDIA-ALTA (7/7)

- **Los soportes SÍ mueven el estado, en los BORDES de banda**: SO1 (full)=P vs SO2 (bajo)=EM — con anclas
  100% + sueño ok, descuidar soportes baja un escalón ENTERO (P→EM). SO5 (full)=EM vs SO6 (bajo)=A.
- **En banda media (75%) no mueven** (SO3=SO4=EM): cerca de un corte voltean; en el medio no.
- **Soportes full EMPUJAN hacia arriba**: SO5 (50% anclas + sueño + soportes full)=EM, por encima del 50% base (A).
- **TASKS NEUTRAS**: SO3 (sin tasks) = SO7 (3 tasks)=EM. Las tasks no mueven el estado.
- **Implicación de modelo**: el peso de soporte 0.80/0.20 dentro de capa es INSUFICIENTE para un salto de banda
  completo con anclas perfectas. El soporte pesa más, o necesita término propio. A calibrar con el script.

## Lote IN — Inquebrantable: cobertura del superhabit, 5 capas (P/I)

- **Fuente**: `docs/scoring/historias-inquebrantable-cobertura-v1.md` · Config Dani: 5 capas, meta 4d.
  **Piso fijo = Plenitud** (anclas 100% + sueño bien en todos). Varía SOLO en cuántas capas hay superhabit.

| caso | cumpl | sup | mag | act | cap | forma | sueño | → | nota |
|------|-------|-----|-----|-----|-----|-------|-------|---|------|
| IN0 | 100 | 0 | 0 | 0 | 0 | - | ok | P | referencia sin extra |
| IN1 | 100 | D | m | 1 | 1 | sost | ok | P | +2 días en 1 capa |
| IN2 | 100 | D | m | 2 | 2 | sost | ok | I | +2 días en 2 capas |
| IN3 | 100 | D | m | 3 | 3 | sost | ok | I | +2 días en 3 capas |
| IN4 | 100 | D | m | 4 | 4 | sost | ok | I | +2 días en 4 capas |
| IN5 | 100 | D | m | 5 | 5 | sost | ok | I | +2 días en las 5 |
| IN6 | 100 | D | l | 1 | 1 | sost | ok | P | +3 días concentrado en 1 capa |
| IN7 | 100 | D | xs | 5 | 5 | sost | ok | I | +1 día en las 5 capas |

### Patrón inferido (P↔I cobertura, 5 capas) — confianza ALTA (8/8, separación perfecta)

- **→ I ⟺ `cap`≥2** (el superhabit toca al menos 2 capas). **→ P ⟺ `cap`≤1.**
- **La MAGNITUD no decide**: IN6 (+3 días, cap1)→P; IN7 (+1 día, cap5)→I. Pura COBERTURA, igual que el meta-patrón.
- ⚠️ **CONFLICTO con el lote SH** (3 capas activas): ahí `I ⟺ cap=3` (TODAS) y SH-C1 (cap2 de 3)→P. Acá con 5
  capas, cap2→I. **Decisión del dueño (2026-06-08): vale ≥2 capas ABSOLUTO.** SH-C1 queda como conflicto viejo
  a re-mirar; revisa la "decisión firme" previa de "superhabit en TODAS las capas activas".

---

## Implicaciones para el refit del modelo (lo que el script debe encodear ahora)

1. **Base = promedio PLANO de capas activas + cortes** (R<0.40, A<0.64, EM<0.84, P≥0.84). **ω=0 (sin worst-term)**
   y **pesos de capa IGUALES** en base pura. (Mata el `min(I,Cu)` y el wCu=0.5 del modelo viejo: eran efecto del sueño.)
2. **Sueño (opt-in)** → modula Cuerpo: lo domina (Caminar puede caer y Cuerpo aguanta) y pesa fuerte cerca del techo
   (sueño mal/no-registrado con anclas 100% → EM). Sueño off = Cuerpo neutro (solo sus anclas).
3. **Sobriedad (opt-in)** → sube el peso de Conducta; recaída = golpe plano (held/broke binario, largo NO modula el
   estado). Modulador activo sin marcar → topea EM.
4. **Soportes** → mueven el estado en los bordes de banda; peso > 0.20. **Tasks neutras** (no mueven).
5. **Inquebrantable** = gate sobre Plenitud: anclas 100% (+ moduladores activos sin penalización) + superhabit en **≥2 capas**.
