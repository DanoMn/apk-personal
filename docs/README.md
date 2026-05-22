# Documentacion canonica

Este directorio separa lo vigente de lo exploratorio. Si hay conflicto entre
documentos, este orden manda.

## 1. Estado actual

- `estado-actual-mvp.md`: snapshot vigente del producto, alcance y proximas decisiones.
- `frontend-design.md`: reglas visuales actuales. Reemplaza cualquier idea anterior tipo cyberpunk, terminal o neon.
- `tono-comunicacion.md`: voz de la app y reglas para mensajes.

## 2. Dominio y datos

- `nucleo-dominio-autonomia.md`: centro conceptual de la app.
- `arquitectura-recomendada-autonomia.md`: decision arquitectonica para backend local, dominio, data y UI.
- `especificacion-actividades-sobriedad-v1.md`: actividades, tipos, checklist y abstinencias.
- `definicion-tablas-room-v1.md`: esquema Room v1 y tablas actuales.
- `frases-ancla.md`: catalogo de citas, taxonomias, rotacion por fase del dia y esquema de frases.

## 3. Prototipos

- `prototipo/index.html`: guia visual viva, iconografia y componentes.
- `prototipo/dashboard.html`: maqueta mobile del dashboard.
- `prototipo/score-states.html`: laboratorio de score general y estados del dashboard.
- `prototipo/styles.css`: estilos base del prototipo inicial.

## 4. Investigacion y canvas

- `canvas-reestructuracion-autonomia.md`: bitacora viva de decisiones y preguntas. Contiene historia util, pero no siempre es mas autoritativo que `estado-actual-mvp.md`.
- `research-apps-similares.md`: investigacion de apps similares. Es material de apoyo, no especificacion final.

## 5. Archivo historico

- `alt/`: documentos archivados o reemplazados. Nada debe moverse ahi si todavia aporta decisiones vigentes.

## Decision de init

A partir de este punto, el proyecto queda inicializado documentalmente:

- La direccion visual oficial es organica/editorial.
- La app es local-first y privacy-first: los datos sensibles viven en el
  dispositivo; auth futura es opcional.
- Room guarda hechos; el dominio calcula inferencias.
- Export/import cifrado sera la portabilidad futura entre dispositivos.
- El dashboard debe ser la pantalla inicial real.
- Las actividades por capa y los tipos medibles siguen en definicion.
- El tono de comunicacion debe definirse antes de cerrar textos finales.
