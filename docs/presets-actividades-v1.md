# Presets de actividades canónicas v1

Fecha: 2026-05-22
Proyecto: Vocal / Autonomía sin límites
Fuente: `docs/decisiones-capas-actividades-v1.md` secciones 6, 7 y 10
Objetivo: dataset oficial de actividades semilla para las 5 capas.

## ActivitySurface

Las actividades del seed existen en `ActivityDefinitionEntity` (catalogo inmutable) sin `UserActivityConfigEntity` asociada. Eso equivale al viejo `Available`: existen en el catalogo, no estan en ninguna superficie activa. El usuario las agrega a `Mis anclas`, `Soportes` o `Pendientes` desde la UI cuando las configura.

| Superficie | UI | UX | Targets |
|------------|-----|-----|---------|
| `Anchor` | Mis anclas | Normal: usuario marca lo que SI hizo | Obligatorios |
| `Support` | Soportes | Inversa: usuario marca lo que NO hizo | Sin targets |
| `Task` | Pendientes | Una sola vez, sin recurrencia | Sin targets |

Sin configuracion de usuario, la actividad no aparece en ninguna superficie. Cero pre-configuracion.

---

## Mis anclas (Anchor)

Base personal que el usuario quiere sostener. Pocas, claras, representativas.

| ID | Nombre | Capa | Tipo | Min | Unidad | Role | Contribution | Importance |
|----|--------|------|------|-----|--------|------|-------------|------------|
| `act_meditar` | Meditar | Interior | Time | 5 min | Minutes | Practice | Core | High |
| `act_escribir` | Escribir | Interior | Note | — | Text | Practice | Core | High |
| `act_ejercicio` | Ejercicio / gimnasio / caminar | Cuerpo | Time | 40 min | Minutes | Practice | Core | High |
| `act_dormir_temprano` | Dormir temprano | Cuerpo | TimeOfDay | — | Time | SelfCare | Core | High |
| `act_no_celular_cama` | No celular antes de dormir | Conducta | Check | — | Boolean | DigitalHygiene | Protective | High |
| `act_no_decidir_madrugada` | No decidir de madrugada | Conducta | Check | — | Boolean | Boundary | Protective | High |
| `act_no_aislarme` | No aislarme | Vínculos | Check | — | Boolean | RelationalHabit | Core | High |
| `act_digitaliza` | Avanzar Digitaliza | Proyecto | Time | 30 min | Minutes | ProjectWork | Core | Critical |
| `act_musica` | Música / composición | Proyecto | Time | 20 min | Minutes | ProjectWork | Core | High |

---

## Soportes (Support)

Mantenimiento diario, cuidado personal, soporte ligero.

| ID | Nombre | Capa | Tipo | Min | Unidad | Role | Contribution | Importance |
|----|--------|------|------|-----|--------|------|-------------|------------|
| `act_leer` | Leer | Interior | Time | 20 min | Minutes | Learning | Support | Medium |
| `act_agua` | Tomar agua | Cuerpo | Count | 8 | Count | SelfCare | Support | Medium |
| `act_banarse` | Bañarse | Cuerpo | Check | — | Boolean | SelfCare | Support | Medium |
| `act_dientes` | Cepillarse los dientes | Cuerpo | Check | — | Boolean | SelfCare | Support | Medium |
| `act_ropa` | Cambiarse de ropa | Cuerpo | Check | — | Boolean | SelfCare | Support | Low |
| `act_comer` | Comer algo decente | Cuerpo | Check | — | Boolean | SelfCare | Support | Medium |
| `act_orden_minimo` | Orden mínimo | Conducta | Time | 15 min | Minutes | DomesticOrder | Support | Medium |
| `act_interaccion` | Una interacción limpia | Vínculos | Note | — | Text | RelationalHabit | Support | Medium |

---

## Notas

- **Sin metas semanales/mensuales por ahora**: los campos `targetCount`, `targetPeriod` y `cadence` quedan `null` en el seed inicial. El usuario los configura desde la UI si quiere goals.
- **Sin abstinence tracks**: los tracks de sobriedad son feature propia, no actividades. Se sembrarán cuando se implemente onboarding.
- **"Leer" va en Interior** según `decisiones-capas-actividades-v1.md` sección 6.1 ejemplos.
- **"Comer"** y **"Tomar agua"** migran de la antigua capa Casa/comida a Cuerpo.
- **"Orden mínimo"** migra a Conducta.
- Los IDs usan prefijo `act_` para distinguirlos de actividades custom (`act_custom_<UUID>`).
