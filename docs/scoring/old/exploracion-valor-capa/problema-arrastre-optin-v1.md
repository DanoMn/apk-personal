# Problema: arrastre del opt-in sin matar anclas ni distorsionar superhabit

> **Planteamiento del orquestador (2026-06-12), verificable.** Documento que define el problema
> exacto a resolver y alimenta a 3 proponentes Opus a ciegas. Toda referencia numérica sale del
> modelo v3 corregido (código abajo, reproducible con python3).

---

## 1. El modelo ACTUAL (v3 corregido) — punto de partida

```python
import math
def R(F,T,mins,gamma=1.5,lam_v=0.5,kappa=1.5,p=2.0,smax=0.5,s0=0.5):   # ancla, da [0,1.5]
    mk=sorted([m for m in mins if m>0],reverse=True);D=len(mk)
    if D==0:return 0.0
    r=[m/T for m in mk];c,v=r[:min(D,F)],r[min(D,F):]
    u=lambda x:min(x,1.0)**gamma
    phi=sum(u(x) for x in c)/F;V=sum(u(x) for x in v)
    base=1-(1-phi)*math.exp(-lam_v*V)
    St=sum(max(x-1,0) for x in c)/F;Sd=V/(7-F) if F<7 else 0.0
    wt=(F/7)**kappa;S=smax*(1-math.exp(-(wt*St+(1-wt)*Sd)/s0))
    return base+(base**p)*S

K_INT=4.0; DELTA=0.10
def band(s):
    return ("RESTAURACION" if s<0.40 else "ATENCION" if s<0.62 else
            "EN MARCHA" if s<0.85 else "PLENITUD" if s<1.0+DELTA else "INQUEBRANTABLE")
def valor_capa(anclas, M=None):
    ab=(sum(min(r,1.0) for r in anclas)/len(anclas)) if anclas else None
    base=(ab+K_INT*M)/(1+K_INT) if (M is not None and ab is not None) else (M if M is not None else (ab or 0))
    extra=(sum(max(r-1,0) for r in anclas)/len(anclas)) if anclas else 0.0
    return min(base,1.0)+extra
def score(capas):  # capas: lista de (anclas, M).  PESOS IGUALES 1/N
    vals=[valor_capa(a,M) for a,M in capas]; return sum(vals)/len(vals)
```

- **valor de capa** ∈ [0, ~1.5], escala del ancla. `base` (≤1, opt-in mezclado adentro con peso
  `K_INT/(1+K_INT)`) + `extra` (≥0, superhabit, SOLO anclas).
- **score** = promedio simple de valores de capa (pesos de capa IGUALES = 1/N).
- bandas: `R<0.40 · A<0.62 · EM<0.85 · P≥0.85 · I≥1.10`.

## 2. EL PROBLEMA

El opt-in (sueño/sobriedad) vive DENTRO de su capa, mezclado con las anclas vía `K_INT`. Eso crea
un trilema que hoy no se puede satisfacer a la vez:

1. **Arrastre débil:** como el peso de la capa es 1/N, el opt-in en su peor valor (mal sueño/recaída)
   solo puede bajar el score hasta un **techo de 1/N**. Con N=5, aunque el sueño sea 100% de Cuerpo,
   el score no baja de **0.80** (sigue En marcha). El golpe se diluye con más capas.
2. **Subir `K_INT` mata las anclas:** para arrastrar más, el opt-in tiene que pesar más DENTRO de la
   capa (ej. 94%), lo que deja a las anclas de esa capa (caminar) en ~6% — prácticamente sin valor.
3. **Inflar el peso de la CAPA distorsiona el superhabit:** darle más peso de capa al opt-in haría
   que el superhabit de esa capa pese más que el de otras (un superhabit en Cuerpo valdría más que en
   Interior) → rareza ya rechazada; manda a Inquebrantable a quien no debe.

> **OBJETIVO:** lograr que el opt-in arrastre FUERTE hacia abajo (parecido al "relacional agresivo",
> con un golpe que NO se diluya tanto con N) **sin (2) matar las anclas de su capa** y **sin (3)
> distorsionar el superhabit.**

## 3. AXIOMAS DUROS (NO romper)

1. Motor de pesos puros: `score = agregación de valores de capa → bandas`. CERO reglas/gates/caps/
   worst-term/`min()` duro.
2. **Dos canales:** base (¿en pie?, ≤1) + extra (superhabit, ≥0, SOLO de las anclas). El opt-in
   NUNCA genera extra (su señal está topada en 1).
3. **Eje:** valor de capa en escala del ancla [0,~1.5]; cumplir todo justo = score 1.0 = Plenitud;
   superhabit repartido = Inquebrantable. Bandas `R<0.40·A<0.62·EM<0.85·P≥0.85·I≥1.10`. (NO existe
   EM_TOP; fue un error ya anulado.)
4. El nº de anclas NO cambia el peso de la capa (las anclas promedian a un bloque).
5. Sueño = señal continua [0,1] (telemetría, 4 comp); sin dato → base, no 0. Sobriedad = binaria
   held/broke en ventana 7d; multi-track por producto (1 recaída no se diluye con más tracks).
6. Capa activa = ≥1 ancla O un opt-in (opt-in puede activar una capa sin anclas: su valor = la señal).
7. El opt-in cuando está BIEN (señal=1) debe ser NEUTRO: no sube ni baja respecto a no tenerlo.

## 4. CRITERIOS DE ÉXITO (qué debe cumplir la solución)

| # | Requisito | Referencia actual (v3) |
|---|-----------|------------------------|
| C1 | Cumplir todo justo + opt-in bien = **1.0 Plenitud** (sin cambio) | 1.000 Plenitud |
| C2 | El opt-in BIEN (señal=1) no cambia el score vs no tenerlo | neutro ✅ |
| C3 | **Arrastre fuerte y poco diluido por N:** mal sueño / recaída deben bajar MÁS que hoy, idealmente parejo entre N=3 y N=5 | mal sueño N=3=0.773 / N=5=0.864; recaída N=3=0.733 / N=5=0.840 |
| C4 | **Las anclas de la capa con opt-in conservan valor** (caminar no cae a ~6%) | hoy con K agresivo cae a 6% ❌ |
| C5 | **Superhabit NO distorsionado:** Sol (superhabit en Interior) = Tin (superhabit en Cuerpo+sueño) | hoy empatan en 1.144 ✅ — debe seguir |
| C6 | Superhabit repartido = Inquebrantable; cumplir justo = Plenitud | 1.266 Inq / 1.0 Plen ✅ |

## 5. CASOS DE PRUEBA (verificar TODOS con python3)

```
Config base: meta de cada ancla 4d×30. "justo"=4d×30 (R=1.0). "superhabit"=6d×30 (R=1.266).
  P1. Cumplir justo + sueño bien (3 capas)                  → esperado 1.0 Plenitud
  P2. Mal sueño M=0.15 (anclas justas) N=3 y N=5            → debe arrastrar MÁS que 0.773 / 0.864
  P3. Recaída M=0 (anclas justas) N=3 y N=5                 → arrastre fuerte, parejo
  P4. Sueño regular M=0.5 N=5                               → arrastre intermedio
  P5. Cuerpo: 1 ancla vs 3 anclas (caminar+estirar+nadar), sueño mal → las anclas deben conservar valor
  P6. Sol: superhabit en Interior | Tin: superhabit en Cuerpo+sueño → DEBEN EMPATAR (C5)
  P7. Superhabit repartido en 3 capas                      → Inquebrantable (C6)
  P8. Capa solo-opt-in (Cuerpo sin anclas), sueño bien     → valor = señal, sin extra
```

## 6. PISTA (no obligatoria): el desacople base/extra

Una vía candidata: que el opt-in dé peso de capa extra **SOLO en el canal base** (arrastra fuerte)
y que el canal extra/superhabit se agregue **siempre con pesos iguales** (no se distorsiona). Los
proponentes pueden tomar, mejorar o descartar esta vía con su sesgo.

## 7. Referencia numérica de "antes" (modelo v3 actual, para comparar)

| Caso | score actual | estado |
|------|--------------|--------|
| Cumplir justo + sueño bien | 1.000 | Plenitud |
| Mal sueño M=0.15, N=3 | 0.773 | En marcha |
| Mal sueño M=0.15, N=5 | 0.864 | Plenitud |
| Recaída M=0, N=3 | 0.733 | En marcha |
| Recaída M=0, N=5 | 0.840 | En marcha |
| Superhabit repartido (3 capas) | 1.266 | Inquebrantable |
| Sol = Tin (superhabit en distinta capa) | 1.144 | Inquebrantable (empatan) |
