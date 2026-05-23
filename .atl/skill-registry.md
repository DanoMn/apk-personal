# Skill Registry — apk-personal

Generated: 2026-05-22
SDD init: sdd-init/apk-personal

## User Skills

| Skill | Stack | Trigger |
|-------|-------|---------|
| admin-panel-ui | Flask + HTMX + Alpine.js + Tailwind | Crear o modificar templates en admin/app/templates/ |
| branch-pr | GitHub CLI | Crear PR, abrir PR, preparar cambios para review |
| cognitive-doc-design | Markdown | Escribir guías, READMEs, RFCs, onboarding, architecture docs |
| comment-writer | GitHub/Slack | Redactar feedback, review comments, maintainer replies |
| customize-opencode | OpenCode config | Editar opencode.json, .opencode/, ~/.config/opencode/ |
| gentle-ai-chained-pr | GitHub | PR > 400 líneas, chained PRs, stacked PRs, reviewable slices |
| go-testing | Go + Bubbletea TUI | Escribir tests Go, usar teatest, agregar coverage |
| issue-creation | GitHub | Crear issue, reportar bug, solicitar feature |
| judgment-day | Review (dual-agent) | "judgment day", "doble review", "juzgar", "que lo juzguen" |
| skill-creator | Agent Skills spec | Crear skill nuevo, agregar agent instructions |
| work-unit-commits | Git | Implementar cambio, preparar commits, split PRs |

## Project Skills

| Skill | Stack | Trigger |
|-------|-------|---------|
| compose-canvas-icons | Jetpack Compose Canvas | Crear o modificar iconos Canvas en DashboardIcons.kt |

## Project Conventions

Index: `AGENTS.md` (project root)

### Referenced files

- `docs/README.md` — índice canónico de documentación
- `docs/estado-actual-mvp.md` — snapshot vigente del producto
- `docs/nucleo-dominio-autonomia.md` — centro conceptual
- `docs/frontend-design.md` — reglas visuales (orgánico/editorial, paleta carbon/calida/coral)
- `docs/tono-comunicacion.md` — voz de la app
- `docs/especificacion-actividades-sobriedad-v1.md` — actividades, tipos, checklist
- `docs/definicion-tablas-room-v1.md` — esquema Room v1
- `docs/prototipo/index.html` — guía visual viva
- `docs/prototipo/dashboard.html` — maqueta mobile
- `meta/meta-prompting.md` — glosario de dominio
- `meta/instructions/*.md` — pro-prompts (archivos de instrucción por tarea)

### Reglas clave

- Respuestas y docs en español; código en inglés
- No usar `&&`; usar `;` o comandos separados
- No atribución de IA en commits
- Local-first con Room; el dominio calcula inferencias
- Visual: cuaderno oscuro bajo luz cálida, cartón/beige, coral mate
- Tono: adulto funcional, compasivo — no humilla ni moraliza
