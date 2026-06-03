# Plan de reorganización de `docs/` (propuesta — NO ejecutado)

> Fecha: 2026-06-01. Síntesis de dos revisiones Sonnet en paralelo: inventario +
> corroboración contra código. Backup previo en
> `/mnt/d/APK-Personal-backups/docs-backup-2026-06-01.tar.gz`.
> Estado: **EJECUTADO 2026-06-01.** Movimientos, archivado, handoffs→meta, referencias
> (CLAUDE.md/AGENTS.md/docs vivos), ruta rota, v10→v12, mapa en CLAUDE.md y README:
> todo aplicado y verificado (refs resuelven). Pendientes derivados en `pendientes.md`.

## Hallazgos transversales (del cruce con código)

- **Esquema Room real = v12, no v10.** `AutonomiaDatabase.kt` está en `version = 12`.
  CLAUDE.md y varios docs dicen v10. Bug de doc a corregir.
- **Referencia ROTA ya hoy:** AGENTS.md cita `docs/especificacion-actividades-sobriedad-v1.md`
  en la raíz, pero el archivo vive en `docs/old/`. Hay que reapuntarla.
- `SleepLogEntity`, `ActivityEntity` (tabla `activities`) describir en docs como activas,
  pero fueron DROPEADAS (migraciones 3→4 y 11→12). Esos docs quedaron desactualizados.
- 15 archivos son "load-bearing" (citados por CLAUDE.md/AGENTS.md): mover cualquiera
  exige actualizar su referencia.

## Estructura propuesta + acción por archivo

Leyenda acción: **mover** (a carpeta temática) · **archivar** (a `old/`) · **queda**
· ⚠️ = referenciado (actualizar ref al mover). Estado del cruce con código entre ( ).

### `docs/producto/`
- `01-filosofia-producto.md` — mover (vigente)
- `estado-actual-mvp.md` ⚠️AGENTS — mover (DRIFT → actualizar contenido luego)
- `nucleo-dominio-autonomia.md` ⚠️AGENTS — mover (DRIFT: modelo sueño viejo → actualizar luego)
- `tono-comunicacion.md` ⚠️AGENTS — mover (vigente)
- `research-apps-similares.md` — mover (material de apoyo)
- `plan-maestro-roadmap.md` — mover (DRIFT → actualizar luego)

### `docs/frontend/`
- `frontend-design.md` ⚠️AGENTS — mover (vigente)
- `mis-anclas-ux-canon-v1.md` — mover (vigente)
- `mis-soportes-ux-canon-v1.md` — mover (vigente)
- `mapa_componentes_v_0_2_borrador.md` ⚠️AGENTS — mover (DRIFT → actualizar luego)
- `prototipo/` ⚠️AGENTS (index.html, dashboard.html) — mover carpeta entera (vigente)

### `docs/dominio/`
- `configuracion-canonica-sistema-v1.md` — mover (vigente)
- `decisiones-capas-actividades-v1.md` — mover (vigente)
- `arquitectura-recomendada-autonomia.md` — mover (vigente)
- `frases-ancla.md` — mover (vigente)
- `definicion-reestructuracion-soporte.md` — mover (vigente)
- `mapa-flujos-estado-actual-2026-05-24.md` — mover (DRIFT → actualizar luego)

### `docs/datos-room/`
- `definicion-tablas-room-v1.md` ⚠️AGENTS — mover (DRIFT: dice v5, real v12 → actualizar luego)
- `actividades-ancla-predeterminadas-v1.md` ⚠️CLAUDE — mover (vigente)
- `preset-soportes-v1.md` ⚠️CLAUDE — mover (vigente)
- `presets-actividades-v1.md` ⚠️CLAUDE — mover (vigente)

### `docs/scoring/`
- `arbol-scoring-v1.md` ⚠️CLAUDE — mover (vigente)
- `plan-tecnico-scoring.md` ⚠️CLAUDE — mover (vigente)

### `docs/sueno/`
- `decisiones-diseno-sueno-v1.md` — mover (vigente)
- `sleep-feature-preliminar.md` — mover (borrador; edge cases aún válidos como deuda)

### `docs/auditorias/`
- `auditoria-permisos-v1.md` — mover (vigente, reciente)

### Archivar a `docs/old/` (DEPRECATED — corroborado contra código)
- `analisis-codigo-pre-reestructuracion.md` (bug ya corregido)
- `contraste-soporte-actual-vs-esperado.md` (bugs de seed ya resueltos)
- `dashboard-auditoria-2026-05-23.md` (diagnóstico histórico)
- `sistema-scoring-semanal-vocal-v1.md` (supersedido por arbol-scoring + plan-tecnico)
- `plan-implementacion-scoring-vocal.md` (supersedido por plan-tecnico)
- `entidades-room-v1.md` (describe entidades dropeadas; auto-declarado histórico)
- `propuesta-redisenio-anclas-2026-05-23.md` (UX superada; scoring viejo)
- `Frases.docx` (binario, antecedente de frases-ancla.md)

### Queda como está
- `docs/README.md` — queda en raíz, pero **actualizar su índice** a la nueva estructura
- `docs/old/*` (ya archivados) — quedan
- `docs/alt/` (vacía declarada) — queda

## Decisión abierta: los handoffs

Los 4 `handoff-*.md` (device-telemetry, sleep-consumer, sleep-followups,
verificacion-scoring) son **proceso de agente**, no doc de producto. En esta sesión
acordamos que ese tipo de material va a `meta/`, no a `docs/`. Propuesta:
**mover a `meta/handoffs/`**. (Alternativa: agruparlos en `docs/sueno/` y
`docs/auditorias/`.) — A confirmar.

## Referencias a actualizar (parte de la ejecución)

- **CLAUDE.md:** 5 rutas (presets/anclas → `datos-room/`, scoring → `scoring/`) +
  corregir "esquema Room v10" → **v12**.
- **AGENTS.md:** 9 rutas (frontend, prototipo, producto, datos-room) + **arreglar la
  ruta rota** de `especificacion-actividades-sobriedad-v1.md` → `docs/old/...`.
- **`meta/instructions/*.md` y `meta/meta-prompting.md`:** también referencian docs —
  grep + actualizar en la ejecución.

## Pendiente para DESPUÉS (no en esta migración)

Los DRIFT no se archivan: siguen siendo la mejor fuente, solo están desactualizados.
Actualizar su contenido (sobre todo el modelo de sueño `SleepLog`→`SleepNight` y el
esquema v5→v12) es trabajo aparte, va al backlog.
