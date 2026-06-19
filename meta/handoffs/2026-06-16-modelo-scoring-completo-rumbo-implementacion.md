# Handoff MASIVO — Modelo de scoring COMPLETO → rumbo implementación en el motor real

> Cierre de la sesión que dejó el **modelo de scoring entero diseñado, calibrado, consolidado,
> verificado (27/27), revisado por 2 Opus y documentado**, con el quilombo de docs archivado.
> Objetivo del próximo tramo: **incorporar este modelo al motor real (código Kotlin)**. Este handoff es
> autocontenido para arrancar a planificar sin re-leer toda la historia. Fecha: 2026-06-16. Proyecto: `apk-personal`.

---

## 0. TL;DR
- **Antes:** ancla + valor de capa + opt-ins cerrados; soportes/tasks/peso de capa/puntos PENDIENTES; código = modelo viejo/roto.
- **Ahora:** TODO el modelo de scoring cerrado y verificado. Fuente de verdad: **`docs/scoring/modelo-matematico-nucleo-v1.md`** (matemática completa + código Python ejecutable) + `modelo-scoring-oficial-v1.md` (alto nivel) + `axiomas-modelo-scoring-v1.md` (contrato, 27/27).
- **Falta:** llevarlo a **código Kotlin** (el código actual es el modelo VIEJO — deuda, no copiar sus constantes).

## 1. Dónde está LA VERDAD (docs vigentes en `docs/scoring/`)
| Doc | Rol |
|---|---|
| **`modelo-matematico-nucleo-v1.md`** | 📐 Matemática COMPLETA, 7 niveles + **código Python ejecutable** (el blueprint a portar) |
| `modelo-scoring-oficial-v1.md` | Descripción alto nivel + filosofía + ejemplo integrado |
| `axiomas-modelo-scoring-v1.md` | Contrato de comportamiento (estilo O1–O13) |
| `verificacion_modelo_oficial.py` | Verificación: **27/27 axiomas verdes** (la base de los tests Kotlin) |
| `axiomas-opt-in-v1.md` | Contrato opt-ins O1–O13 (con nota: bajo peso variable, `w=BETA·Σpesos`) |
| `arbol-scoring-v1.md`, `plan-tecnico-scoring.md` | Contrato matemático base / plan técnico (referenciados) |
| `revisiones/revision-rigor-matematico.md`, `revisiones/revision-fidelidad-filosofia.md` | Informes de la auditoría doble Opus |
| `old/` (34 items) | TODO el proceso/exploración archivado (consolidados, propuestos, merges, historias, dataset, exploraciones) — **NO usar como contrato** |

## 2. El modelo en una página (pipeline + fórmulas clave)
```
hechos (ventana móvil 7 días)
 → NIVEL 1  Ancla R(F,T,mins) = base + base^p·S        (gate base²; superhabit tiempo St + días Sd)
 → NIVEL 2  Valor capa: base_anclas=avg min(R,1); extra_capa=avg max(R−1,0)
            + Soporte: base_eff=(1−WS)·base_anclas+WS·G   (G=avg min(días/4,1))
            + Task: extra += saturación conjunta (gate base², techo TAU, efímero diario)
 → NIVEL 3  Peso de capa: votos (1−r^n)/(1−r); capa solo-soportes=ρ; solo-opt-in=W0
 → NIVEL 4  Opt-ins: señal M; término-sombra w = BETA·Σpesos·(1−M)
 → NIVEL 5  Bolsa-global: base_global=Σ(v·w)/Σw (anclas+opt-ins);  extra_global=avg extra (PLANO, Sol=Tin)
            ESTADO = min(base_global,1) + extra_global  ∈ [0,1.5]
 → NIVEL 6  Bandas: R<0.40 · A<0.62 · EM<0.85 · P<1.10 · I≥1.10
 → NIVEL 7  Puntos: mapeo E (suma de logísticas) → [650,1100]; 1000 se gana al entrar a Inquebrantable
```

## 3. Parámetros calibrados (van a `ScoringConstants.kt`)
| Símbolo | Valor | Nivel |
|---|---|---|
| γ, λ_v, κ, p, smax, s0 | 1.5, 0.5, 1.5, 2.0, 0.5, 0.5 | ancla |
| WS | 0.07 | soportes (blend) |
| TAU, N0 | 0.06, 1.0 | tasks |
| r, ρ, W0 | 0.5, 0.15, 1.0 | peso de capa |
| BETA, A, B_SLEEP | 0.818, 0.55, 0.5 | opt-ins |
| δ | 0.10 | bandas |
| piso/tope puntos | 650 / 1100 | mapeo E |
| hitos E (c,w,A) | (.18,.10,60)(.55,.11,110)(.83,.09,100)(1.07,.055,130)(1.35,.13,50) | mapeo E |

## 4. Decisiones clave cerradas esta sesión (no re-discutir)
1. **Forma A:** soportes/tasks entran al valor de **cada capa** (no sombra global).
2. **Peso de capa VARIABLE por anclas** (votos r=0.5): más anclas = pesa más, con freno (techo 2.0 → ninguna capa >50% del score). Reemplaza el "1/N" anterior.
3. **Capa solo-soportes** pesa ρ=0.15 (sin piso; abandonada vale 0, arrastra poco por el peso bajo). **Capa solo-opt-in** pesa normal W0=1 (O11).
4. **Soporte:** blend leve en la base (WS=0.07), bidireccional, señal `min(días/4,1)` (ventana 4d), bloque=promedio (no crece con cantidad), cero fricción (sin dato=cumplido), solo base.
5. **Task:** aporte efímero diario al **extra**, saturación conjunta (techo TAU=0.06/capa, gate base²), nunca resta, neutral no suma; el tope **emerge** (no compra Inquebrantable).
6. **Opt-ins reconciliados con peso variable:** término independiente en la bolsa-global, `w=BETA·Σpesos·(1−M)` (generaliza `BETA·N`; BETA=0.818 intacto). I1/I2/I3 verificados.
7. **Agregación = dos cuentas:** base PONDERADA por peso de capa + extra PLANO (mantiene Sol=Tin).
8. **Plenitud entra en 0.85** (cumplir-justo=1.0 cae dentro, no es el inicio); resuelta la contradicción del árbol.
9. **Mapeo a puntos = enfoque E** (hitos-meta perseguibles, piso 650/tope 1100, 1000 se gana con superhabit, cumplir-justo=941). Reemplaza el `700+base·300` viejo (que saturaba y ocultaba Inquebrantable).
10. **Heredadas vigentes:** ancla R (A1–A10), opt-ins comportamiento (O1–O12), motor de pesos puros (cero gates/caps/worst-term), ventana 7 días, ANCLAS>SOPORTES>TASKS.

## 5. Verificación y auditoría (estado de confianza)
- `verificacion_modelo_oficial.py`: **27/27 axiomas verdes** (ancla, peso, opt-ins bolsa-global, soportes, tasks, agregación, bandas, puntos).
- **2 Opus auditaron** (rigor + fidelidad): sólido y fiel, sin bugs de modelo. Correcciones menores ya aplicadas (ejemplos §1.4, gate be^P, guardas anti-crash, clamps, O12, nota superseded en opt-in).
- El código Python del núcleo es **ejecutable y reproduce todo** (cumplir-justo→1.0→941; Martín c/opt-ins→0.821→862).

## 6. RUMBO IMPLEMENTACIÓN — punto de partida para planificar

### 6.1 Lo que hay hoy (código)
- Motor en `app/src/main/java/dev/panopt/autonomia/domain/scoring/` (`ScoreEngine.kt`, `*Policy.kt`, `ScoringConstants.kt`) = **modelo VIEJO/roto**. NO copiar sus constantes. Sirve solo para ver qué existe y qué es deuda.
- Pipeline actual: hechos Room → `ScoreInputSource`/`BuildScoreInputUseCase` → `ScoreEngine` → `*Policy.kt` → `ScoreReport` → `DashboardProjection` → `DashboardScoreReportState` → Compose.
- Fuente diaria canónica: `daily_activity_logs` (`activity_logs` es legacy).

### 6.2 Lo que hay que hacer (alto nivel — a detallar con SDD)
1. **Portar el núcleo Python → Kotlin** (dominio puro JVM, no toca Room ni Compose). Cada NIVEL del núcleo = una/s Policy.
2. **Reescribir `ScoringConstants.kt`** con los 17 parámetros de §3 (eliminar las constantes viejas).
3. **ELIMINAR reglas-parche viejas:** `WORST_LAYER_COLLAPSE`, `WORST_LAYER_MIN_FOR_*`, gates `UNBREAKABLE_*`, cortes viejos (0.70), ancla `0.70·días+0.30·tiempo`, sueño 0.30 en Cuerpo, soporte 0.80/0.20, task 0.05, k_sleep/k_sobr, EM_TOP, VisibleScore `700+base·300`.
4. **Mapeo a puntos E** reemplaza el VisibleScore viejo (afecta `DashboardProjection`/`ScoringScreen`, NO el motor).
5. **Tests (Strict TDD activo):** replicar los asserts de `verificacion_modelo_oficial.py` como tests JUnit Kotlin (`app/src/test/.../domain/scoring/`). Test runner: `gradlew.bat testDebugUnitTest --tests 'dev.panopt.autonomia.domain.scoring.*'`.
6. **Esquema Room NO cambia** — es lógica de dominio (Camino A vigente: sin migraciones en dev).
7. **Higiene digital:** moverla a Conducta (deuda técnica conocida); sueño usa 4 componentes (código usa 2).

### 6.3 Cosas a cuidar al implementar
- El opt-in escala con **Σpesos** (suma de pesos de capa), no con N — no hardcodear N.
- El gate de tasks usa `base_eff^p` (no hardcodear 2).
- Tasks son **diarias/efímeras** (reset al cerrar el día) — encaja en el cierre diario (`closeElapsedActivityDays`).
- Bandas: Plenitud entra en **0.85** (no 0.70 del código viejo, no 1.0).

## 7. Pendientes (fuera del modelo cerrado)
- **Estabilidad temporal multi-semana** (`arbol §15`): ortogonal, sin reconciliar con el modelo nuevo. Decidir si entra.
- **Cobertura de verificación 100%:** `verificacion_modelo_oficial.py` cubre la mayoría; faltan asserts para AN12, VC4, SO6/TA6, O6, O9, PU2 (verificados a mano por los revisores, pasan). Ampliar al portar a tests Kotlin.
- **Numeración A6/A8 del ancla** sin reconciliar (heredado; no afecta el modelo).
- **Calibración fina futura** contra más marcas reales del dueño (los valores son despejados de axiomas, afinables; especialmente WS, TAU, ρ, r y los cortes de banda).

## 8. Memoria (engram, project apk-personal) — para recuperar
Topics/observaciones clave guardadas: `scoring/modelo-soportes-tasks` (modelo v3 + reconciliación + calibración + verificación + revisión), `scoring/puntos-visibles` (mapeo E), + decisiones sueltas (Plenitud 0.85, r=0.5, TAU/WS/ρ, modelo consolidado y congelado). Buscar con `mem_search` por "scoring".

## 9. Próximo paso concreto
**Planificar la incorporación al motor real con SDD** (`/sdd-new` o exploración): leer el núcleo, mapear cada nivel a Policy/constante, definir el orden de implementación y los tests TDD (basados en `verificacion_modelo_oficial.py`). NO empezar a codear sin spec — contrato de spec + verificación por capas aplican.

## 10. Artefactos creados/modificados esta sesión
- **Nuevos vigentes:** `modelo-matematico-nucleo-v1.md`, `modelo-scoring-oficial-v1.md`, `axiomas-modelo-scoring-v1.md`, `verificacion_modelo_oficial.py`, `old/README.md`, 2 informes en `revisiones/`.
- **Modificados:** `axiomas-opt-in-v1.md` (nota superseded), `arbol-scoring-v1.md` + `plan-tecnico-scoring.md` (refs a old/ + nota).
- **Set-prompts de la sesión:** `meta/instructions/2026-06-16-set-prompt-soportes-tasks-v2.md`, `2026-06-16-set-prompt-puntos-visibles.md`, `2026-06-16-set-prompt-puntos-visibles-ronda2.md`.
- **Archivado a `docs/scoring/old/`:** 34 items (todo el proceso/exploración).
