# Presets de actividades — Seed dataset v1

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Fecha: 2026-05-23
Proyecto: Autonomía sin límites
Fuente canónica: `docs/datos-room/actividades-ancla-predeterminadas-v1.md`
Propósito: generar `ActivityDefinitionEntity` seeds. Sin targets, sin metas, sin
configuración de usuario. Solo definiciones inmutables del catálogo.

## ActivitySurface

Las actividades del seed existen solo como definiciones de catálogo. El usuario las
configura (tipo, targets, metas) desde la UI. Cero pre-configuración.

| Superficie | UI | UX | Targets |
|------------|-----|-----|---------|
| `Anchor` | Mis anclas | Normal: usuario marca lo que SI hizo | Obligatorios (configura el usuario) |
| `Support` | Soportes | Inversa: usuario marca lo que NO hizo | Sin targets |
| `Task` | Pendientes | Una sola vez, sin recurrencia | Sin targets |

---

## Mis anclas (Anchor)

33 actividades. Fuente: `docs/datos-room/actividades-ancla-predeterminadas-v1.md`.

### Interior

| ID | Nombre | Capa | Tipo | Unidad | Role | Contribution | Importance |
|----|--------|------|------|--------|------|-------------|------------|
| `act_meditar` | Meditar | Interior | Time | Minutes | Practice | Core | High |
| `act_leer` | Leer | Interior | Time | Minutes | Practice | Core | High |
| `act_escribir` | Escribir | Interior | Note | Text | Practice | Core | High |
| `act_aprender` | Aprender | Interior | Time | Minutes | Learning | Core | High |
| `act_estudiar` | Estudiar | Interior | Time | Minutes | Learning | Core | High |

### Cuerpo

| ID | Nombre | Capa | Tipo | Unidad | Role | Contribution | Importance |
|----|--------|------|------|--------|------|-------------|------------|
| `act_ejercicio` | Ejercicio | Cuerpo | Time | Minutes | Practice | Core | High |
| `act_caminar` | Caminar | Cuerpo | Time | Minutes | Practice | Core | High |
| `act_correr` | Correr | Cuerpo | Time | Minutes | Practice | Core | High |
| `act_gimnasio` | Gimnasio | Cuerpo | Time | Minutes | Practice | Core | High |
| `act_estirar` | Estirar | Cuerpo | Time | Minutes | Practice | Core | High |
| `act_yoga` | Yoga | Cuerpo | Time | Minutes | Practice | Core | High |
| `act_deporte` | Deporte | Cuerpo | Time | Minutes | Practice | Core | High |

### Conducta

| ID | Nombre | Capa | Tipo | Unidad | Role | Contribution | Importance |
|----|--------|------|------|--------|------|-------------|------------|
| `act_educacion_financiera` | Educación financiera | Conducta | Time | Minutes | Learning | Core | High |
| `act_gestion_financiera` | Gestión financiera | Conducta | Time | Minutes | Practice | Core | High |
| `act_orden_digital` | Orden digital | Conducta | Time | Minutes | Practice | Core | High |
| `act_higiene_digital` | Higiene digital | Conducta | Check | Boolean | Practice | Core | High |
| `act_autonomia_alimentaria` | Autonomía alimentaria | Conducta | Time | Minutes | Practice | Core | High |

### Vínculos

| ID | Nombre | Capa | Tipo | Unidad | Role | Contribution | Importance |
|----|--------|------|------|--------|------|-------------|------------|
| `act_cultivar_vinculo` | Cultivar vínculo | Vínculos | Note | Text | RelationalHabit | Core | High |
| `act_grupo_estudio` | Grupo de estudio | Vínculos | Time | Minutes | RelationalHabit | Core | High |
| `act_entrenamiento_grupal` | Entrenamiento grupal | Vínculos | Time | Minutes | RelationalHabit | Core | High |
| `act_voluntariado` | Voluntariado recurrente | Vínculos | Time | Minutes | RelationalHabit | Core | High |
| `act_proyecto_compartido` | Proyecto compartido | Vínculos | Time | Minutes | RelationalHabit | Core | High |
| `act_mentoria` | Mentoría | Vínculos | Time | Minutes | RelationalHabit | Core | High |
| `act_crianza` | Crianza presente | Vínculos | Note | Text | RelationalHabit | Core | High |

### Proyecto

| ID | Nombre | Capa | Tipo | Unidad | Role | Contribution | Importance |
|----|--------|------|------|--------|------|-------------|------------|
| `act_trabajar_proyecto` | Trabajar en proyecto | Proyecto | Time | Minutes | ProjectWork | Core | High |
| `act_crear` | Crear | Proyecto | Time | Minutes | ProjectWork | Core | High |
| `act_practicar_habilidad` | Practicar habilidad | Proyecto | Time | Minutes | ProjectWork | Core | High |
| `act_estudiar_carrera` | Estudiar carrera | Proyecto | Time | Minutes | Learning | Core | High |
| `act_construir_negocio` | Construir negocio | Proyecto | Time | Minutes | ProjectWork | Core | High |
| `act_desarrollar_producto` | Desarrollar producto | Proyecto | Time | Minutes | ProjectWork | Core | High |
| `act_componer_musica` | Componer música | Proyecto | Time | Minutes | ProjectWork | Core | High |
| `act_crear_contenido` | Crear contenido | Proyecto | Time | Minutes | ProjectWork | Core | High |
| `act_marca_personal` | Desarrollar marca personal | Proyecto | Time | Minutes | ProjectWork | Core | High |

---

## Soportes (Support)

8 actividades. Mantenimiento diario, cuidado personal. Sin targets en el seed.

| ID | Nombre | Capa | Tipo | Unidad | Role | Contribution | Importance |
|----|--------|------|------|--------|------|-------------|------------|
| `act_leer` | Leer | Interior | Time | Minutes | Learning | Support | Medium |
| `act_agua` | Tomar agua | Cuerpo | Count | Count | SelfCare | Support | Medium |
| `act_banarse` | Bañarse | Cuerpo | Check | Boolean | SelfCare | Support | Medium |
| `act_dientes` | Cepillarse los dientes | Cuerpo | Check | Boolean | SelfCare | Support | Medium |
| `act_ropa` | Cambiarse de ropa | Cuerpo | Check | Boolean | SelfCare | Support | Low |
| `act_comer` | Comer algo decente | Cuerpo | Check | Boolean | SelfCare | Support | Medium |
| `act_orden_minimo` | Orden mínimo | Conducta | Time | Minutes | DomesticOrder | Support | Medium |
| `act_interaccion` | Una interacción limpia | Vínculos | Note | Text | RelationalHabit | Support | Medium |

---

## Notas técnicas

- Los campos de configuración de usuario se dejan `null` en el seed. Esto incluye `weeklyFrequencyTarget`, `sessionTargetMinutes`, `commitmentDurationMonths` y los espejos legacy `targetValue`, `minimumValue`, `targetCount`, `targetPeriod`, `cadence`.
- Al activar una definición como ancla, la UI debe guardar `weeklyFrequencyTarget` obligatorio (`2..7`), `sessionTargetMinutes` obligatorio (`1..900`) y `commitmentDurationMonths` nullable (`null = Indefinido`).
- Los IDs usan prefijo `act_`. Actividades creadas por el usuario usan `act_custom_<UUID>`.
- `contributionRole` determina el peso en scoring: `Core` pesa más que `Support`.
- `importanceTier` modula el peso: `High` = 1.20x, `Medium` = 1x, `Low` = 0.75x.
