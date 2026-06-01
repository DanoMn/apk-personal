# Documentación canónica

Este directorio guarda la doc de **producto/contrato**, organizada por tema. La doc
de **proceso de agente** (guías, handoffs, backlog) vive en `meta/`, no acá. Si hay
conflicto entre documentos, lo vigente manda sobre lo archivado (`old/`).

## `producto/` — visión, estado, tono

- `producto/estado-actual-mvp.md`: snapshot del producto, alcance y próximas decisiones.
- `producto/nucleo-dominio-autonomia.md`: centro conceptual de la app.
- `producto/vocal-01-filosofia-producto.md`: filosofía de producto ("patas de la mesa").
- `producto/tono-comunicacion.md`: voz de la app y reglas para mensajes.
- `producto/plan-maestro-roadmap.md`: hoja de ruta viva por feature.
- `producto/research-apps-similares.md`: investigación de apps similares (material de apoyo).

## `frontend/` — diseño visual y UX

- `frontend/frontend-design.md`: reglas visuales actuales (orgánico/editorial; reemplaza cualquier idea cyberpunk/neón).
- `frontend/mis-anclas-ux-canon-v1.md`: patrón cerrado de UX/UI para Mis anclas.
- `frontend/mis-soportes-ux-canon-v1.md`: patrón cerrado de UX/UI para Soportes.
- `frontend/vocal_mapa_componentes_v_0_2_borrador.md`: mapa de componentes y nombres canónicos.
- `frontend/prototipo/`: prototipos HTML vivos (`index.html`, `dashboard.html`, `score-states.html`, `styles.css`).

## `dominio/` — modelo conceptual y reglas

- `dominio/configuracion-canonica-sistema-v1.md`: reglas vigentes de configuración por superficie.
- `dominio/decisiones-capas-actividades-v1.md`: decisiones sobre capas y modelo de actividades.
- `dominio/arquitectura-recomendada-autonomia.md`: decisión arquitectónica (backend local, dominio, data, UI).
- `dominio/definicion-reestructuracion-soporte.md`: reglas de dominio de Soportes.
- `dominio/frases-ancla.md`: catálogo de frases, taxonomías y rotación.
- `dominio/mapa-flujos-estado-actual-2026-05-24.md`: mapa de navegación, datos y flujos.

## `datos-room/` — esquema y seeds

- `datos-room/definicion-tablas-room-v1.md`: esquema Room y tablas (⚠ describe v5; real v12 — actualizar).
- `datos-room/actividades-ancla-predeterminadas-v1.md`: catálogo canónico de anclas predeterminadas.
- `datos-room/presets-actividades-v1.md`: seed dataset de actividades.
- `datos-room/preset-soportes-v1.md`: seed dataset de soportes.

## `scoring/` — motor de puntuación

- `scoring/arbol-scoring-vocal-v1.md`: contrato matemático canónico (fórmulas).
- `scoring/plan-tecnico-scoring-vocal.md`: plan técnico vivo del scoring.

## `sueno/` — feature Sueño

- `sueno/decisiones-diseno-sueno-v1.md`: diseño conceptual cerrado del feature Sueño.
- `sueno/sleep-feature-preliminar.md`: edge cases del feature (borrador / deuda diferida).

## `auditorias/` — auditorías vigentes

- `auditorias/auditoria-permisos-v1.md`: auditoría de permisos Android (v0.3.0).

## `old/` — archivado / deprecated

Documentación histórica o reemplazada. **NO usar como contrato vigente.** Incluye los
scoring exploratorios, las auditorías ya resueltas, `entidades-room-v1.md`, specs
pre-reestructuración, etc.

## `alt/` — reservada

Carpeta de archivo declarada, actualmente vacía.

## Decisión de init

- La dirección visual oficial es orgánica/editorial.
- La app es local-first y privacy-first: los datos sensibles viven en el dispositivo.
- Room guarda hechos; el dominio calcula inferencias.
- El dashboard es la pantalla inicial real.
