# Plan — 3 modelos del motor de scoring (opus en paralelo) + síntesis

> **Estado: plan para aprobar.** No se lanzan subagentes hasta el OK del dueño.
> Fecha: 2026-06-08. Objetivo: dejar de iterar por prueba-y-error; pasar a 3 teorías
> completas, escoger lo mejor.

## Flujo

1. (este plan) Define bases cerradas + fórmula de trabajo + casos límite.
2. **3 subagentes opus, independientes y a ciegas**, cada uno escribe un modelo COMPLETO del
   motor en su propio MD (`docs/scoring/modelo-propuesto-{A,B,C}.md`), resolviendo todos los
   casos límite desde el mismo piso.
3. El orquestador (yo) lee los 3, extrae lo mejor de cada uno por caso límite, y escribe UN
   modelo consolidado para aprobación del dueño.

---

## BASES CERRADAS (NO se tocan — son el piso común de los 3)

1. **El motor es SOLO pesos.** `score = Σ(peso_capa × valor_capa)` → estado por bandas.
   **CERO reglas, caps, gates, colapsos, worst-term.** Todo comportamiento EMERGE de peso × valor.
2. **Capas:** Interior, Cuerpo, Conducta, Vínculos, Proyecto. Mínimo 3 activas, máximo 5.
3. **Capas normales pesan IGUAL** (1/N). La **cantidad de actividades (anclas) por capa NO cambia
   su peso**; las anclas se **promedian** dentro de la capa.
4. **Opt-in (los únicos que rompen la igualdad):** Sueño modula **Cuerpo**, Sobriedad modula
   **Conducta**. Cuando un opt-in está activo: **sube el peso de su capa** Y el modulador entra
   al **valor** de esa capa (sueño dentro del valor de Cuerpo; estado de sobriedad dentro del de
   Conducta). Modulador malo / no-registrado → **hunde el valor** de su capa.
5. **Soportes:** NO tienen peso propio; entran al **valor** de su capa, **asimétrico** (descuidar
   castiga mucho más que tener premia). **Tasks: neutras (0).**
6. **Bandas** sobre el puntaje 0–1: `R < ~0.40 · A < ~0.62 · EM < ~0.85 · P ≥ ~0.85` (los cortes
   exactos se pueden afinar; el orden y rango es ese).
7. **Inquebrantable:** NO es banda de puntaje. Es estado sobre Plenitud: anclas 100% + superhabit
   en **≥2 capas**.
8. **Sustento ya derivado de las marcas (respetar):** Conducta con sobriedad activa pesa **~50–63%**
   (de las marcas SB9 + SB4). Las **45 marcas que el modelo DEBE reproducir** están en
   `docs/scoring/dataset-decisiones-estado-v1.md` (lotes BP/SU/SBR/SO/IN).

---

## CASOS LÍMITE QUE CADA MODELO DEBE RESOLVER (y justificar)

1. **Escalado con N (3→5 capas):** ¿el opt-in es **multiplicador fijo** (Forma A: su porción baja
   al subir N) o **porción fija** (Forma B: pesa igual a cualquier N) u otra? Plantear y justificar.
2. **El apretón:** 3 capas + sueño + sobriedad activos → la única capa libre queda muy baja
   (~17% con ×2/×3). ¿Es aceptable? ¿Cómo evitar que se ahogue **SIN meter una regla**?
3. **Los dos opt-in juntos (sueño + sobriedad):** **no hay ni una marca**. ¿Cómo combinan los
   pesos y los golpes? (Es una elección de diseño — justificarla.)
4. **Anclas múltiples por capa** (ej. Interior = Meditar + Leer vs Cuerpo = solo Caminar):
   confirmar el promedio interno o proponer algo mejor, sin que la cantidad de anclas altere el peso.
5. **Soporte:** forma funcional exacta de la asimetría (cuánto castiga descuidar, cuánto premia
   tener), que reproduzca las marcas SO.
6. **Magnitud exacta de los multiplicadores/porciones** dentro del rango que las marcas permiten.
7. **Higiene digital — UI ≠ scoring (RESUELTO, no es un sub-modulador).** En el MOTOR, Higiene
   digital es **una ancla de Conducta normal**: promedia dentro del valor de Conducta, con el peso
   de Conducta. **NO impacta Sueño.** Su ubicación como opt-in **dentro de la feature Sueño** es
   **SOLO UI** (están temáticamente relacionadas — pantallas antes de dormir), y **NO entra al
   cálculo** — mismo principio que la UX inversa de soportes (presentación ≠ dominio, ver
   `scoring/soportes-polaridad` en engram). No hay matrioshka en los pesos. Único matiz posible: la
   UI puede ofrecer/activar esta ancla solo cuando Sueño está prendido — eso es disponibilidad/config,
   NO scoring. Los 3 modelos la tratan como una ancla de Conducta más.

---

## FÓRMULA DE TRABAJO (cada MD, estas secciones EN ESTE ORDEN)

1. **Resumen del modelo** (5 líneas).
2. **Fórmula matemática completa:** peso de cada capa según N y según qué opt-in está activo;
   valor de cada capa (anclas, sueño, sobriedad, soporte); score; bandas; gate Inquebrantable.
3. **Tabla de pesos:** peso de cada capa en cada combinación (sin opt-in / solo sueño / solo
   sobriedad / ambos), para **N=3 y N=5**.
4. **Resolución de los 6 casos límite**, uno por uno, con justificación.
5. **Verificación contra las 45 marcas** (`dataset-decisiones-estado-v1.md`): cuántas reproduce,
   cuáles no y por qué. (Pueden escribir un script descartable en /tmp para chequear — NO modificar
   el código existente.)
6. **Tradeoffs y riesgos** del enfoque.

**Restricciones duras:** respetar TODAS las bases cerradas; CERO reglas-parche; todo emerge de
peso × valor. Si una marca no entra sin una regla, decirlo explícito (no inventar la regla).

---

## Salida

- `docs/scoring/modelo-propuesto-A.md`, `-B.md`, `-C.md` (uno por subagente).
- Luego: el orquestador escribe `docs/scoring/modelo-consolidado-v1.md` con lo mejor de cada uno.

## Decisión pendiente del dueño

¿Apruebo este plan y lanzo los 3 opus? ¿Cambiarías alguna base cerrada o agregarías algún caso
límite antes de arrancar?
