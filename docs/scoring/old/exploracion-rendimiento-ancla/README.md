# Sesión de exploración — modelo matemático del rendimiento de un ancla

> **Bitácora de la sesión.** Fecha: 2026-06-09. Orquestador: Fable 5 (rol "Mythos").
> Objetivo: derivar la fórmula del VALOR de un ancla — `rendimiento = f(días, tiempos; F, T)` —
> haciendo EMERGER los comportamientos buscados desde la estructura matemática, sin gates.

## Contexto de entrada

- El motor de pesos puros está CERRADO (`docs/scoring/mapa-modelo-scoring-v1.md` §1, §3).
- El gap que esta sesión ataca es el §6 del mapa: "Normalización días/tiempo PENDIENTE desde cero".
- El código viejo (`0.70·días + 0.30·tiempo`, gates `UNBREAKABLE_*`, worst-term) es deuda: contexto
  de qué NO repetir, no fuente de verdad.
- El contrato completo de esta sesión (axiomas A1–A10, P1, P2, casos límite, restricciones) vive en
  el prompt de sesión y está copiado FIJO en cada brief.

## Pipeline

| Fase | Qué | Estado |
|------|-----|--------|
| −1 | Onboarding del orquestador (mapa, núcleo de dominio, motor viejo, handoff superhabit/déficit) | ✅ |
| 0 | Carpeta + briefs + bitácora | ✅ |
| 1 | 3 proponentes (Opus, sesgos A/B/C) + 3 researchers (Sonnet, ángulos 1/2/3) EN PARALELO | ✅ |
| 2 | Merge del orquestador → `merge-consolidado.md` | ✅ |

## Roles y sesgos

- **Proponente A** — dominancia fuerte de frecuencia (`D/F` manda, tiempo ajuste menor).
- **Proponente B** — acoplamiento suave (días × tiempo combinados, frecuencia gana por poco).
- **Proponente C** — saturación / retornos decrecientes (crecen rápido, se aplanan).
- **Researcher 1** — normalización de dos métricas acopladas (mecanismo de P1).
- **Researcher 2** — saturación y retornos decrecientes (tiempo trivial + superávit acotado).
- **Researcher 3** — pesos que se desplazan según un parámetro (mecanismo de P2: peso del tiempo crece con F).

Reglas de aislamiento: proponentes con conocimiento interno, sin web, sin ver a los otros.
Researchers con web, ángulos sin solapamiento, no proponen la solución final.
El merge espera a que TODOS terminen.

## Fuera de esta versión (a propósito)

- Testeo automatizado en Python (se decide DESPUÉS de leer la fórmula consolidada).
- Calibración de parámetros (α, β, …) — viene después, contra el dataset de marcas de estado.

## Decisiones del camino

1. **Briefs autocontenidos** (Fase 0): cada brief lleva FIJO los 8 axiomas, P1/P2, rango, restricciones,
   ruta y secciones del entregable; lo único destilado por rol es el contexto de proyecto. Los subagentes
   no leyeron nada más del repo.
2. **Aislamiento respetado** (Fase 1): proponentes sin web y sin verse entre sí (solo cálculo python3
   local); researchers con web, ángulos sin solapamiento. Los 6 corrieron en paralelo; el merge esperó
   a los 6.
3. **Convergencia independiente detectada** (Fase 2): los 3 proponentes coincidieron a ciegas en
   (a) base = esqueleto de F slots, (b) superávit subordinado a la base (`base^p`), (c) peso del
   superávit de tiempo creciendo con `(F/7)^κ`, (d) Inquebrantable = `R ≥ 1+δ`. El merge adoptó esa
   columna vertebral común.
4. **Arbitrajes del merge**: se rechazó el voluntario sin superávit de A (vaciaba la vía de
   Inquebrantable a F bajo), la base geométrica calibrable de B (la dominancia debe ser estructural,
   no paramétrica) y el techo explosivo de C (R=4.0 rompe el promedio de capa). Se tomó: reparación
   exponencial voluntaria (A), confirmación de canales separados (B), *toe* anti-trivialidad
   simplificado a `min(r,1)^γ` (C), saturación exponencial del superávit (R2) y pesos complementarios
   `w_t + w_d = 1` (R3).
5. **Verificación del consolidado**: 8/8 axiomas, 9/9 casos límite + testigo del dueño + ráfaga
   absurda, con parámetros ilustrativos (`γ=1.5, λ_v=0.5, κ=1.5, p=2, σ_max=0.5, σ_0=0.5, δ=0.10`).
   Script reproducible en `merge-consolidado.md` §7.

## Corrección metodológica v2 (2026-06-10, dueño) — SIN reglas de cobertura

El dueño detectó que el análisis post-merge enmarcaba "δ vs N" como decisión de cobertura
("¿exigimos ≥2 capas en superávit?") — pensamiento de reglas, exactamente lo que el modelo existe
para evitar. Correcciones aplicadas:

- **Ningún estado se define por reglas.** Score = promedio ponderado de capas activas (pesos 1/N) →
  estado por bandas. Inquebrantable ⟺ score ≥ 1+δ, con δ como un corte de banda más.
- **Las 45 marcas dejan de ser ground truth** — fueron la herramienta de descubrimiento para llegar
  a los axiomas; la verdad ahora son los axiomas. Mapa §4 y §6 actualizados.
- **Simulación corregida:** N = 3..5 (mínimo 3 = único axioma duro del motor; el stress N=6..8 se
  retiró), **1 ancla por capa**, y enumeración EXHAUSTIVA de mundos (120/330/792 combinaciones) para
  ver la emergencia real. Resultado: 9.876 checks, 0 fallas. Hallazgo emergente (no regla): a N=5
  todo mundo Inquebrantable resulta tener ≥2 capas con valor >1 por pura aritmética; a N=3 el más
  justo es SUP_MAX+SUP_MAX+DEBIL=1.105 (dos fuertes cargan una débil — emergió del promedio).
- **Próximo paso definido por el dueño:** axiomas de comportamiento de una CAPA respecto a opt-ins
  (sueño, sobriedad), soportes y tasks.

## Post-merge (2026-06-10): corrección del dueño + verificación determinista

- **Corrección del dueño:** Inquebrantable es ÚNICAMENTE un estado del score global (consumo de todas
  las capas), nunca por ancla. El ancla solo exporta valor `[0, 1+]`. `merge-consolidado.md` corregido.
- **Verificación determinista:** `scripts/scoring/anchor_engine_sim.py` — 9.825 checks, 0 fallas
  (axiomas por grillas/barridos + casos límite + P2 monótono).
- **Simulación multi-capa N=3..8** (solo anclas, pesos 1/N): resultados y hallazgos en
  `simulacion-capas-resultados.md`. Clave: la cobertura de Inquebrantable EMERGE (el viejo "≥2 capas"
  aparece solo a N=5 con δ=0.10) y quedó detectada la decisión de calibración δ vs N (a N=3..4 con
  δ=0.10, una sola capa en superávit máximo alcanza; δ=0.15 exige 2 en todo el rango canónico).

## Resultado

**`merge-consolidado.md`** — la fórmula final con justificación, tabla de qué se tomó/rechazó de cada
propuesta, verificación numérica completa y explicación en lenguaje claro. Pendiente (fuera de esta
versión, por diseño): calibración contra el dataset de marcas y testeo automatizado si el dueño decide
que la estructura vale la inversión.
