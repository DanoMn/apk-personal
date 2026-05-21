# Pro-Prompt: Implementación del Backend (Room & Domain)

## Objetivo
Establecer la base de persistencia de datos (Room) y el núcleo del dominio (Modelos puros, Reglas de Estado) para la versión local-first de "Vocal / Autonomía sin límites", dividiendo el trabajo en tareas pequeñas y verificables para evitar acoplamiento.

## Contexto Técnico
- **Arquitectura**: Clean Architecture ligera.
- **Regla Central**: Room guarda *hechos* históricos. El dominio lee los hechos y calcula *inferencias* operativas.
- **MCP Context 7**: Se debe consultar para garantizar que la implementación se realiza utilizando las mejores prácticas de arquitectura en Kotlin/Android.
- **ScoreState**: Enum canónico: `NoData`, `Restoration`, `Attention`, `Motion`, `Plenitude`, `Unbreakable`.
- **Registro Diario**: Se acepta 1 `ActivityLog` por día (actualizable en caso de sumas o tiempos extras).
- **Features separadas**: `AbstinenceTrack` es independiente de la checklist normal. `Task` es para pendientes, no para hábitos.

## Plan de Ejecución

### Task técnica 1 — Migrar/actualizar esquema Room base
- Consultar a Context 7 sobre mejores prácticas para inicialización.
- Configurar Room (`AppDatabase`) y DAOs base utilizando `Flow`/Coroutines.
- Crear entidades base (solo estructura de datos, sin lógica compleja):
  - `Layer`, `Activity`, `ActivityLog`.
  - `AbstinenceTrack`, `AbstinenceLog`.
  - `RiskEvent`, `Task` (pendientes).
  - `AnchorPhrase` y reglas de frases.

### Task técnica 2 — Seed de capas core
- Programar el callback/script de inicialización de Room para crear las 5 capas principales:
  - `Interior`
  - `Cuerpo`
  - `Conducta`
  - `Vínculos`
  - `Proyecto`

### Task técnica 3 — Seed de presets de actividades/abstinencias
- **Checklist principal**: Meditar (Interior), Ejercicio (Cuerpo), Proyecto Digitaliza (Proyecto), Proyecto musical (Proyecto).
- **Checklist secundaria**: Cepillarse los dientes (Cuerpo), Bañarse (Cuerpo), Cocinar en casa (Cuerpo), Limpiar los trastes (Conducta).
- **Abstinencias protectoras**: Alcohol (crítico), Conducta sexual (moderado/crítico), Marihuana (inactiva).

## Estado
*Aprobado por el usuario.*
Instrucción refinada lista para ejecución técnica.
