# Pro-Prompt — Refit del script de scoring a modelo unificado (v2)

> **Estado: plan para aprobar** (Protocolo de Meta-Prompting, `AGENTS.md` paso 4–6).
> NO se escribe Python hasta que el dueño apruebe. Fecha: 2026-06-08.

## Contexto humano

El dueño marcó las 5 tandas nuevas (`base-pura`, `sueño`, `sobriedad`, `soportes/tasks`,
`inquebrantable`). Ya están destiladas al dataset (`docs/scoring/dataset-decisiones-estado-v1.md`,
lotes BP/SU/SBR/SO/IN). El script viejo (`scripts/scoring/weight_model_fit.py`) ajustó la
instancia "sueño siempre ON, 3 capas" (40/43). Las marcas nuevas **re-anclan la base sin sueño**
y revelan que el `wCu=0.5` + worst-term del modelo viejo **eran efecto del sueño**, no de la base.

Objetivo: un **modelo unificado de pesos dinámicos** que reproduzca TODAS las marcas (viejas + nuevas)
SIN reglas-parche, validado por grid search.

## Decisión de archivo

- **Crear** `scripts/scoring/weight_model_fit_v2.py`. **No** modificar el viejo: queda como
  referencia de la instancia sueño-on (trazabilidad). No es código de la app → no aplica Camino A;
  igual lo dejo intacto.
- Correr con `python3 scripts/scoring/weight_model_fit_v2.py` (sin deps, solo stdlib `itertools`).

## El modelo (estructura matemática concreta)

Cada **capa activa** produce un valor en `[0,1]`; el estado sale de un **agregado ponderado + cortes**,
con **ω=0 (sin worst-term)**.

### 1. Valor de capa

- `ancla_val` = promedio de las fracs de sus anclas; `frac = min(hecho/meta, 1)`.
- **Cuerpo:** si sueño activo → `core = β·sleep_val + (1−β)·ancla_val`, con `sleep_val`: ok=1.0,
  mal/none=`s_bad`. Si sueño OFF → `core = ancla_val`.
- **Conducta:** si sobriedad activa, el valor depende del **estado de sobriedad** (NO del largo de racha):
  - `limpia`   → `core = ancla_val`   (la racha NO sube el valor; binaria; el premio va a la feature)
  - `recaída`  → `core = r_relapse`    (valor bajo fijo)
  - `sin-marcar` → `core = r_unmarked` (valor bajo; topea EM dentro de la ventana de perdón)
  - Sobriedad OFF → `core = ancla_val`.
- **Soporte** (si la capa tiene soporte): aplica una de estas **formas funcionales** (el grid elige la
  que mejor reproduzca SO1–SO6 — ver Tensión 1):
  - (a) lineal: `layer_val = (1−p_sop)·core + p_sop·soporte_frac`
  - (b) asimétrica con bonus chico + penalización grande:
    `layer_val = core + b_sop·soporte_frac − p_sop·(1−soporte_frac)`, con `p_sop ≫ b_sop`
  - (c) penalización con umbral: el descuido penaliza fuerte solo bajo cierto `soporte_frac`.
  - `layer_val` se clava en `[0,1]`.

### 2. Pesos dinámicos

- Base: **pesos iguales** `1/n` por capa activa (capas parejas — lote BP lo exige).
- Si **sueño activo** → `w_Cuerpo *= k_sleep`, renormalizar.
- Si **sobriedad activa** → `w_Conducta *= k_sobr`, renormalizar.
- Nada más modula pesos.

### 3. Score y estado

- `score = Σ wᵢ·layer_valᵢ`  (ω=0, sin término de peor capa).
- Cortes: `R < cRA · A < cAEM · EM < cEMP · P ≥ cEMP`.
- **Gate Inquebrantable** (encima de P): si `estado == P` Y todas las anclas al 100% Y el superhabit
  cubre **≥ `cov_min` capas** (decisión del dueño: `cov_min = 2`) → `I`.

## Representación de casos (encoding genérico)

Cada caso es un dict explícito (no días crudos, para evitar bugs de meta):

```python
case = dict(
    id="SO2",
    layers={ "I":1.0, "Cu":1.0, "Co":1.0 },   # ancla_val por capa activa
    sleep="ok",        # off | ok | mal | none
    sobriety="off",    # off | clean | relapse | unmarked   (aplica a "Co")
    support={ "I":0.286, "Cu":0.286, "Co":0.286 },  # soporte_frac por capa, o None
    superhabit_cap=0,  # nº de capas con superhabit (para el gate I)
    all100=True,       # todas las anclas al 100% (para el gate I)
    expect="EM",
)
```

## Casos a incluir

1. **Las 5 tandas nuevas** (BP·11, SU·9, SBR·10, SO·7, IN·8) = 45 casos, codificados desde el dataset.
2. **Regresión de los viejos** (CB/REF/REFv2/REFv3, sueño-on 3-capa, ~43 casos) re-codificados al
   formato genérico — para garantizar que el modelo unificado **no rompe** lo ya validado.
   - ⚠️ Si re-codificar los 43 viejos es mucho de una, **fase 1** = solo tandas nuevas; **fase 2** =
     sumar regresión vieja. A definir con el dueño (ver "Decisión pendiente").

## Parámetros y grids (coarse)

| Param | Qué | Grid tentativo |
|-------|-----|----------------|
| `β` | peso del sueño dentro de Cuerpo | 0.5, 0.6, 0.7 |
| `s_bad` | valor de sueño mal/none | 0.0, 0.1, 0.15, 0.2 |
| `k_sleep` | multiplicador de peso de Cuerpo (sueño on) | 1.5, 2, 2.5, 3 |
| `k_sobr` | multiplicador de peso de Conducta (sobriedad on) | 1.5, 2, 2.5, 3 |
| `r_relapse` | valor de Conducta en recaída | 0.2, 0.3, 0.4, 0.5 |
| `r_unmarked` | valor de Conducta sin-marcar | 0.3, 0.4, 0.5 |
| `p_sop` / `b_sop` | penalización / bonus de soporte | p: 0.2–0.5 · b: 0.0–0.1 |
| `cRA, cAEM, cEMP` | cortes | 0.40 · 0.64 · 0.84 (±0.02) |
| `cov_min` | cobertura mínima para Inquebrantable | fijo en 2 (decisión) |

Restricciones de filosofía (el modelo debe SIGNIFICAR lo correcto, como el script viejo):
`k_sleep>1`, `k_sobr>1`, `β≥0.5` (sueño ≥ ejercicio dentro de Cuerpo), `p_sop>b_sop` (soporte asimétrico).

## Las 2 tensiones (lo que el grid debe resolver o escalar)

**Tensión 1 — Soporte SO2 vs SO3.** ⚠️ **CORREGIDO POST-AUDITORÍA (2026-06-08):** la afirmación
"un blend lineal NO entra" era FALSA — un hand-calc errado del orquestador (no consideró que los cortes
también son libres). Dos auditores opus independientes refutaron: la forma LINEAL también da 45/45. El
script v2.1 ahora prueba `sop_form=["asym","lin"]` y reporta que AMBAS ajustan. **La asimetría del soporte
NO está identificada** por los 7 casos SO; se decide por dominio o con más casos (soporte parcial 4/7, 5/7),
no por el fit. (Ver `2026-06-08-correcciones-modelo-v2.md`.)

**Tensión 2 — Sobriedad SB9 vs SB10.** Resuelta por diseño con `core` dependiente del estado: `limpia`
no levanta el valor (→ SB9 floja arrastra a A con peso alto), `sin-marcar` sí lo baja (→ SB10 topea EM).
Verificado a mano que entra con `k_sobr→w_Co≈0.5`, `r_unmarked≈0.4`. El grid lo confirma.

## Criterio de éxito y reporte

- Imprime `MEJOR AJUSTE: X/N`, los parámetros ganadores, y la **lista de fallos**
  (`caso  esperado→predicho  (score)`), igual que el script viejo.
- **Meta:** ≥ 90% global; **0 fallos** en BP e IN (son los más limpios, separación perfecta).
- Cada fallo se clasifica: ¿inconsistencia de marca conocida (ej. SH-C1) o falla del modelo?
- Los fallos del modelo que no cierren → se traen al dueño con números (no se fuerzan parámetros raros).

## Fuera de alcance (NO hacer)

- NO tocar código de la app (`domain/scoring/*Policy.kt`) — sigue siendo código viejo; las divergencias
  se aplican al cerrar la calibración (handoff + Camino A).
- NO escribir migraciones, NO tests Room, NO compilar la app (esto es un script puro de calibración).
- NO re-marcar tandas ni cambiar el dataset (ya destilado). El script solo LEE las marcas destiladas.

## Decisión pendiente del dueño (antes de codear)

1. **Alcance de regresión:** ¿v2 incluye los 43 casos viejos desde el arranque (una sola verdad), o
   **fase 1 = solo las 5 tandas nuevas** y fase 2 suma los viejos? (Recomiendo fase 1 primero: más rápido
   de validar el modelo nuevo; los viejos entran cuando el core ya cierra.)
2. ¿El plan refleja lo que necesitás, o ajustamos algo del modelo/params antes de escribir?
