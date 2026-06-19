# Merge — mapeo del ESTADO a PUNTOS VISIBLES

> ## ✅ DECISIÓN FINAL (2026-06-16): enfoque **E** (hitos-meta perseguibles)
> La ronda 1 (A/B/C, abajo) recomendaba C, pero fue **superada por la ronda 2** (D/E/F,
> ver `opus-D/E/F-mapeo.md`). El dueño **eligió E**. Fórmula y detalle: `opus-E-mapeo.md`.
>
> **Mapeo E — piso 650, tope 1100. Números redondos como metas; el 1000 se gana al entrar a Inquebrantable.**
>
> | Momento de cambio | Estado | Puntos |
> |---|---|---|
> | Piso | 0.00 | **650** |
> | → Atención | 0.40 | 721 |
> | → En marcha | 0.62 | 788 |
> | → Plenitud | 0.85 | 873 |
> | cumplir-justo | 1.00 | **941** |
> | → Inquebrantable | 1.10 | **1011** (cruzaste los 1000) |
> | tope | 1.50 | 1100 |
>
> Metas perseguibles: 700≈0.30 · 800≈0.65 · 900≈0.88 · **1000≈1.09**. Es **suma de rampas logísticas**
> (suave, sin codos): la resolución se aprieta justo antes de cada número redondo y afloja después.
>
> ---
>
> **(Histórico — merge de ronda 1, A/B/C, superado:)** Estado: recomendación de diseño que recomendaba C.
> Fecha: 2026-06-16. Proyecto: `apk-personal`.

## 0. Veredicto

El hallazgo central: **la idea base del dueño (poner la resolución abajo, 0-0.40→0-700) desmotiva
justo donde el producto quiere premiar.** Los 3 Opus lo confirman — incluso el que la implementó literal
(A) la cuantifica como su costo. La recomendación del merge es **adoptar el enfoque de OPUS C** (piso
digno + resolución arriba + el "1000" se gana con superhabit), que es el más alineado con el tono del
producto (compasivo, no humilla, premia el esfuerzo).

## 1. Tabla comparativa

| Dimensión | OPUS A (fiel al dueño) | OPUS B (lineal puro) | OPUS C (experiencia) |
|---|---|---|---|
| Función | lineal a tramos | `puntos = 1000·ESTADO` | lineal a tramos (pendiente creciente) |
| Piso (ESTADO 0) | **0** | **0** | **650** (digno) |
| Tope (ESTADO 1.5) | 1000 | **1500** | 1100 |
| Resolución | **toda ABAJO** (14× Rest. vs Inq.) | **uniforme** | **ARRIBA** (máx en ascenso a Plenitud) |
| Cumplir-justo (1.0) | 930 | **1000** | 960 |
| El "1000" significa | (dentro de Plenitud) | cumplir todo | **superhabit / Inquebrantable** |
| Esfuerzo arriba (1.10→1.30) | **+25 pts** (casi nada) | +200 pts | +50 pts |
| Simplicidad | media | **máxima** | media |
| Respeta "no humillar" | ❌ (muestra 0-300) | ❌ (muestra 0-400) | ✅ (piso 650) |

## 2. Lo que encuentro (los hallazgos)

1. **La idea del dueño, en números, mata la motivación arriba.** A la implementó literal y la cuantificó:
   en Restauración el número se mueve 17.5 pts por cada 0.01 de ESTADO; en Inquebrantable, 1.25 (ratio
   **14×**). Esforzarte de 1.10 a 1.30 (superhabit sostenido real) sube **solo 25 puntos**. B y C la
   **rechazaron** por esto. → **Recomendación: invertir — resolución ARRIBA, no abajo.**
2. **El tope 1000 no alcanza para [0, 1.5].** A lo respeta pero comprime brutalmente arriba; B se va a
   1500; C a 1100. → Si querés Inquebrantable visible Y esfuerzo premiado, **el número tiene que pasar de
   1000** (el 1000 deja de ser el techo y pasa a ser un hito).
3. **El mejor hallazgo (de C): el "1000" se GANA con superhabit, no con cumplir.** Cumplir-justo da 960;
   cruzás los 1000 solo cuando te destacás (Inquebrantable). Psicológicamente potente y coherente con la
   filosofía ("la gloria se gana con el esfuerzo extra"). B en cambio pone 1000 en cumplir-justo (menos
   especial).
4. **Piso 0 contradice el pilar "no humillar".** A y B muestran 0-300 en una mala semana. C lo resuelve
   con piso 650 (digno, sin mentir). El tono del producto favorece C.
5. **B gana en una sola cosa: simplicidad** (`estado×1000`, explicable en una frase). Si la simplicidad
   pesara más que todo, sería B — pero rompe el tope (1500) y el piso digno.

## 3. Recomendación del merge: base OPUS C

```
ESTADO   PUNTOS   (corte de banda)
0.00  ->  650     Restauración (piso de dignidad — nunca menos)
0.40  ->  750     → Atención
0.62  ->  820     → En marcha (hogar operativo)
0.85  ->  900     → Plenitud
1.00  ->  960     cumplir-justo (NO satura — queda cielo arriba)
1.10  -> 1000     → Inquebrantable (el "mil" se gana con superhabit)
1.50  -> 1100     tope (el techo respira sobre 1000)
```
- Resolución máxima en el ascenso a Plenitud (premia el esfuerzo).
- Piso 650 (no humilla), tope 1100 (Inquebrantable visible).
- Lineal a tramos: continuo, auditable, sin magia. Verificado (monótono, sin saltos).

## 4. Decisiones de producto para el dueño

1. **¿Aceptás invertir tu idea?** Los 3 Opus muestran que la resolución abajo (0-700 Restauración)
   desmotiva arriba. El merge recomienda **resolución arriba** (C). ¿Lo aceptás, o tenés una razón para
   mantener la resolución abajo que no estamos viendo?
2. **¿El número puede pasar de 1000?** Con [0,1.5] y tope 1000 estricto, hay que comprimir arriba (malo).
   - **Sí, que pase** (C: tope 1100, "1000 = te destacaste") — recomendado.
   - **No, tope 1000 duro** → entonces es A (comprime arriba) o reescalar.
3. **Menor: redondeo de cortes.** C usa 820 y 900 (múltiplos de 20/50). Si querés solo centenas redondas,
   se ajusta comprimiendo un poco.

## 5. Trade-off honesto si insistís en tu idea
Si querés mantener "resolución abajo" (A), es 100% viable y está documentado (`opus-A-mapeo.md`), pero
asumí el costo: el usuario que se esfuerza en Plenitud/Inquebrantable casi no ve mover el número. Para un
producto que premia constancia y esfuerzo, eso va en contra. Por eso el merge recomienda C.
