# Set-prompt v2 — SOPORTES y TASKS dentro del motor (Forma A, por capa)

> **Plan masterizado del orquestador.** Reemplaza al set-prompt v1 (`2026-06-15`), que partía de un
> modelo EQUIVOCADO (sombra global + tasks en el número visible). Este v2 es el modelo correcto,
> destilado de una sesión larga de corrección con el dueño. Cada opus recibe este núcleo + su sesgo (§7).
> Fecha: 2026-06-16. Proyecto Engram: `apk-personal`. Verificación: `python3`.
> **Lo que el dueño corrigió (clave):** soportes y tasks entran **DENTRO del valor de cada capa**
> (Forma A), **NO** como sombra global. Y las tasks se inyectan **DENTRO de la curva de superhabit
> existente**, que tiene candados — meterlas crudo la rompe.

---

## 1. Objetivo

Definir cómo **SOPORTES** y **TASKS** entran al **valor de cada capa** (Forma A), respetando el motor
v4 cerrado. Es la última pieza del valor de capa. Cada opus entrega un **plan matemático completo**;
el orquestador hará merge.

## 2. El marco CERRADO (no se toca — es ground truth)

### 2.1 Fórmula del ancla (verdad ejecutable, de `exploracion-valor-capa/modelo_valor_capa_v4_merge.py`)

```python
def R(F,T,mins,gamma=1.5,lam_v=0.5,kappa=1.5,p=2.0,smax=0.5,s0=0.5):
    mk=sorted([m for m in mins if m>0],reverse=True);D=len(mk)
    if D==0:return 0.0
    r=[m/T for m in mk];c,v=r[:min(D,F)],r[min(D,F):]
    u=lambda x:min(x,1.0)**gamma
    phi=sum(u(x) for x in c)/F;V=sum(u(x) for x in v)
    base=1-(1-phi)*math.exp(-lam_v*V)              # base saturada ∈ [0,1]
    St=sum(max(x-1,0) for x in c)/F;Sd=V/(7-F) if F<7 else 0.0
    wt=(F/7)**kappa;S=smax*(1-math.exp(-(wt*St+(1-wt)*Sd)/s0))   # superhabit saturado, techo smax=0.5
    return base+(base**p)*S                          # R = base + base² · S  (gate base², p=2)
```

### 2.2 LA CURVA DE SUPERHABIT — los 3 candados (la parte más difícil, NO romper)
1. **Saturación exponencial, techo 0.5.** `S = 0.5·(1−exp(−surplus/0.5))`. Los primeros excesos valen
   mucho, después casi nada. (Ej.: 30→45min = +0.175 extra; 240→600min ≈ +0.001.)
2. **Gate `base²`.** El superhabit se multiplica por `base²`: sin cimiento (base<1) la gloria se
   castra. (4/4 días a 120min → extra 0.463; 2/4 días a 120min → R 0.591, superhabit casi nulo.)
3. **Agregación con pesos iguales + semanal.** `extra_capa = promedio max(R−1,0)` ∈ [0,0.5];
   `extra_global = promedio de extra_capa` con pesos iguales (esto es lo que hace `Sol=Tin`, O5).

### 2.3 Valor de capa, score y bandas
```
valor_capa  = min(base_capa, 1) + extra_capa
base_capa   = promedio min(R,1) de anclas (+ opt-in sombra si la capa lo tiene)
score       = promedio de valores de capa, pesos de capa iguales (1/N)
Bandas: R<0.40 · A<0.62 · EM<0.85 · P<1.10 · I≥1.10 ; cumplir-justo = 1.0 = Plenitud
```

### 2.4 Opt-ins (sueño/sobriedad) — feature SEPARADA, NO TOCAR
Término-sombra global `w=BETA·N·(1−M)`, `BETA=0.818`, plano en N, solo base. Contrato O1–O13
(`axiomas-opt-in-v1.md`). **Soportes/tasks NO se combinan con esto** — son mecanismos propios.

## 3. SOPORTES — lo que el dueño definió (Forma A)

- **Mecanismo PROPIO**, aparte de los opt-ins. Entra **DENTRO del valor de SU capa**.
- **Solo BASE** (sin superhabit; la base está topada en 1 = su techo natural). **Bidireccional LEVE:**
  sostenerlo sube un toque la base (ayuda a llegar al cimiento si las anclas están a medias);
  descuidarlo la baja un toque. Neutro cuando va a la par de las anclas.
- **Señal por soporte (ventana indulgente):** punto de partida `s_i = min(días_cumplidos / 4, 1)` —
  con 4 días ya estás al 100%, no se exigen los 7 (medir 7 es abusivo). Los opus pueden refinar (3–4).
- **Multi-soporte:** una capa puede tener 1, 2, 3+ soportes. **El peso del bloque-soporte NO crece con
  la cantidad** (1 o 5 soportes pesan lo mismo; se reparten). Señal de bloque = combinación de los `s_i`.
- **ANCLAS ≫ SOPORTES** en peso dentro de la capa.
- UX inversa (el usuario marca lo que NO hizo) = presentación; interno: más sostenido = mejor.

## 4. TASKS — lo que el dueño definió ("opt-in gracioso y motivador")

- Entra **DENTRO del valor de su capa**, **solo al EXTRA (superhabit), DENTRO de la curva** (§2.2):
  debe respetar techo 0.5, gate base² y saturación. **NO sumar crudo por fuera.**
- **DIARIO y EFÍMERO:** el aporte de las tasks de HOY se borra mañana. Encajar este pulso diario en un
  motor semanal. (Si hoy llegaste a Inquebrantable por tasks, mañana arrancás sin ese empujón → enseña
  que lo que importa son las anclas.)
- **Techo por capa** (~0.1 es ORIENTATIVO, no literal — el número sale de respetar la curva). Saturado
  (la 1ª task vale más que la 10ª). **Aporte parejo** (NO más fuerte cerca del umbral): es lo bastante
  grande para **arañar un cruce de estado cuando ya estás cerca**, pero no fabrica estados de la nada.
- **Anti-abuso:** 100 tasks no inflan el superhabit (la saturación + el techo lo impiden).
- **Nunca resta.** Task **con capa** → aporta a esa capa. Task **neutral/sin capa** → no aporta. El
  usuario solo elige la capa (no el peso).
- **ANCLAS > SOPORTES > TASKS** en importancia/impacto.

## 5. CAPAS — algo nuevo (B8)

Una capa con **soportes pero SIN anclas** debe **pesar MENOS** en el promedio global de capas — si no,
una capa de poca sustancia impactaría desproporcionadamente todo el score. Definir el **peso de capa
reducido** (continuo, sin gate duro). (Análogo a considerar: capa solo-soportes < capa con anclas.)

## 6. Los 3 PROBLEMAS MATEMÁTICOS a resolver

1. **Peso del bloque-soporte dentro de la capa:** bidireccional leve, NO crece con la cantidad de
   soportes, anclas ≫ soportes, solo base, mecanismo propio. Despejar la magnitud de un axioma de estado.
2. **Capa solo-soportes → peso de capa reducido** en el global (continuo).
3. **Tasks = extra efímero diario DENTRO de la curva de superhabit:** compartir la saturación con el
   superhabit de anclas (no romper techo 0.5 ni gate base²), techo anti-abuso por capa, saturación por
   cantidad, aporte parejo que araña cruces, y la temporalidad diaria vs. el motor semanal.

## 7. Sesgos divergentes (uno por opus)

- **OPUS A — "surplus virtual / reusar las fórmulas cerradas".** Mínima cirugía: expresá soporte y task
  como **entradas virtuales** que pasan por las fórmulas YA existentes. Task = "surplus virtual" que se
  suma al `surplus` ANTES de la saturación `S=smax(1−exp(...))` (respeta techo y gate por construcción),
  contado solo HOY. Soporte = señal que entra al cálculo de `base` como un componente más. Máxima
  consistencia con v4.
- **OPUS B — "blends convexos por capa".** Soporte = blend convexo en la base: `base_eff =
  (1−γ_s)·base_anclas + γ_s·G_soporte` (pull hacia la señal de soporte → bidireccional natural, γ_s
  chico, fijo). Task = **saturación conjunta reparametrizada** del extra. Peso de capa solo-soportes =
  blend por densidad de anclas.
- **OPUS C — "presupuesto / headroom y masa de capa".** Pensá la base como presupuesto [0,1] y el extra
  como presupuesto [0,0.5] a llenar. Soporte llena el *gap* de base `(1−base_anclas)` con factor chico
  (y resta del gap si se descuida). Task llena el *headroom* restante `(0.5−extra_anclas)` con un tope,
  efímero. Peso de capa por "masa de sustancia" (anclas presentes).

## 8. Restricciones duras (romper una = plan inválido)

1. No tocar la fórmula del ancla `R` (gate base², smax=0.5) ni los opt-ins (O1–O13).
2. No distorsionar el superhabit de las anclas: **`Sol=Tin` debe seguir**; las tasks comparten la curva
   pero NO la rompen (techo 0.5, gate base²).
3. Mantener **cumplir-justo = 1.0 = Plenitud** y el eje semántico.
4. **Sin gates/caps/worst-term duros.** Todo continuo y diferenciable (el gate base² es suave, OK).
5. **ANCLAS > SOPORTES > TASKS** (en impacto sobre el estado).
6. Soporte: solo base, bidireccional leve, no crece con cantidad. Task: solo extra dentro de la curva,
   efímera diaria, con techo, nunca resta.
7. NO heredar magnitudes a dedo: despejar de axiomas de estado. (Los `0.07/0.1` del dueño son
   ORIENTATIVOS, no literales — el número correcto sale de respetar la curva.)

## 9. Contrato de entrega (cada opus DEBE producir)

Escribir `docs/scoring/exploracion-soportes-tasks/subagente-{A|B|C}-plan-v2.md` con:
1. **Filosofía** del sesgo (1 párrafo).
2. **Axiomas de SOPORTES** `S1…Sn` y **de TASKS** `T1…Tn` (estilo O1–O13), + el de **peso de capa** (B8).
3. **Fórmulas explícitas** con parámetros **despejados de axiomas de estado** (no a dedo).
4. **Verificación con `python3`** (incluir script en `subagente_{A|B|C}_v2.py` y pegar resultados):
   (a) `Sol=Tin` intacto; (b) cumplir-justo=1.0=Plenitud; (c) soporte bidireccional leve y NO crece con
   la cantidad; (d) capa solo-soportes pesa menos; (e) tasks dentro de la curva (no rompen techo 0.5 ni
   gate; 100 tasks saturan; reset diario); (f) tasks arañan UN cruce cuando estás cerca, pero
   cumplir-justo + tasks NO compra Inquebrantable solo; (g) anti-gate (continuidad); (h) ANCLAS>SOPORTES>TASKS.
5. **Riesgos / lo que queda abierto.**

Reusá el motor v4 verbatim (`modelo_valor_capa_v4_merge.py`) como base del script.
**Devolución al orquestador:** resumen conciso (≤30 líneas): ruta, axiomas clave, fórmula central de
cada pieza, resultados de verificación, y en qué diverge tu enfoque. NO vuelques todo el contenido.

## 10. Referencias
- Motor v4: `docs/scoring/exploracion-valor-capa/modelo_valor_capa_v4_merge.py`.
- Curva superhabit (contrato): `docs/scoring/arbol-scoring-v1.md` §8 y §11-NUEVO; opt-ins O1–O13:
  `docs/scoring/axiomas-opt-in-v1.md`. Bandas: árbol §16-NUEVO.
- Dominio: `docs/producto/nucleo-dominio-autonomia.md`, `docs/dominio/definicion-reestructuracion-soporte.md`.
- Engram (`apk-personal`) topic `scoring/modelo-soportes-tasks`.
