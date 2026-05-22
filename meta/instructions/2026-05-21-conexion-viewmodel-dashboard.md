# Pro-Prompt: Conexión de Datos, Lógica de Inferencia y Dashboard UI

Este documento contiene TODO el contexto crítico y las decisiones de diseño arquitectónico y de dominio tomadas en la sesión previa. **Es obligatorio leerlo y asimilarlo por completo antes de escribir cualquier código en la próxima iteración.**

## 1. Decisiones de Diseño y Dominio (Sesión Actual)

Durante la sesión anterior, tomamos decisiones fundamentales sobre el núcleo del producto que **no deben alterarse ni malinterpretarse**:

- **Nomenclatura Canónica del Dominio (`ScoreState`)**: 
  Se abandonaron los estados en español ("En marcha", "Bajo movimiento", "Crisis", etc.) como nombres de variables. El enum del dominio es ESTRICTAMENTE: `NoData`, `Restoration`, `Attention`, `Motion`, `Plenitude`, `Unbreakable`. Términos como "Riesgo" o "Crisis" ya no compiten como estados, sino que son *eventos o señales* (`RiskEvent`, `AbstinenceLog` con recaída).
- **Diferenciación estricta de Entidades**:
  - `Activity` y `ActivityLog`: Hábitos recurrentes y prácticas de la checklist. Poseen atributos avanzados como `role`, `displaySurface`, `contributionRole`, e `importanceTier`.
  - `Task`: Exclusivamente para "pendientes puntuales" que se completan y desaparecen. **No usar `Task` para hábitos.**
  - `AbstinenceTrack`: Son carriles independientes de protección (ej. Alcohol, Conducta sexual). No son actividades regulares y se manejan en secciones de UI separadas.
  - `AnchorPhrase`: Frases de anclaje. **Decisión técnica importante**: el campo `authorReference` es *nullable* (`String?`) porque las frases inactivas o borradores pueden no tener autor. Las activas siempre deben tener cita.
- **Flujo de Datos (Arquitectura Limpia)**:
  - **Room (Capa Data)**: Solo guarda hechos brutos e históricos (logs). No calcula rachas ni estados.
  - **Dominio / ViewModel**: Lee los hechos y calcula las *inferencias* (el `ScoreState` diario, si las dimensiones están estables o en alerta).
  - **Compose (Capa UI)**: Es completamente pasivo y estático. Solo presenta el `DashboardState` final y emite clicks (intents).

## 2. Lo que se Construyó y Refactorizó en esta Sesión

1. **Modelos Puros y Room (Data Layer)**:
   - Se reescribieron los archivos `Models.kt` y `Entities.kt` con el nuevo esquema exacto, añadiendo todas las nuevas columnas de metadatos (como `contributionRole`, `displaySurface`, etc.).
   - Se implementaron las tablas para: `Layer`, `Activity`, `ActivityLog`, `AbstinenceTrack`, `AbstinenceLog`, `RiskEvent`, `Task`, `AnchorPhrase`, `AnchorPhraseStateRule`, `AnchorPhrasePhaseRule`, `AnchorPhraseImpression`, `AnchorPhraseDailySlot`.
   - Se migró el `AutonomiaDao.kt` a Kotlin Flow.
2. **Repositorio y Seed Inicial**:
   - `AutonomiaRepository.kt` ahora inicializa las **5 capas core** (Interior, Cuerpo, Conducta, Vínculos, Proyecto).
   - Contiene los *presets* exactos acordados:
     - *Checklist Principal*: Meditar, Ejercicio, Proyecto Digitaliza, Proyecto musical.
     - *Checklist Secundaria*: Dientes, Bañarse, Cocinar, Limpiar trastes.
     - *Abstinencias protectoras*: Alcohol, Conducta sexual, Marihuana.
3. **Desacople Temporal de la UI**:
   - Se removió cualquier lógica compleja o dependencias de estado en `DashboardScreen` y sus componentes internos (`StatusCard`, etc.). Actualmente, la UI es una maqueta estática hermosa que compila pero espera datos.

## 3. Objetivo para la Próxima Sesión (Tu Misión)

Darle vida al Dashboard conectando el repositorio ya sembrado con la interfaz estática, creando el puente de lógica (ViewModel).

### Roadmap Técnico Obligatorio

1. **Crear y Conectar el `DashboardViewModel`**:
   - Deberás crear un ViewModel (usando Factory injection, ya que Hilt no está instalado en `build.gradle.kts` para el MVP).
   - Este ViewModel debe instanciar el `AutonomiaRepository`.

2. **Implementar el Algoritmo de Inferencia**:
   - Crear un `StateFlow<DashboardState>` que combine los flujos de la BD (capas, actividades, logs de hoy, abstinencias, eventos de riesgo).
   - Reconstruir la lógica que decide qué `ScoreState` es el actual basado en los hechos. (Ej: Si no hay cuidado personal y hay recaída, el estado baja. Si hay prácticas clave, sube a `Motion` o `Plenitude`).
   - Reconstruir la evaluación de las dimensiones (`DashboardDimension`: Sobriedad, Cuidado Básico, Práctica, Riesgo).

3. **Inyección Reactiva a Compose**:
   - Conectar los datos en vivo a `DashboardScreen.kt` y a cada tarjeta (`StatusCard`, `DailyProgressCard`, `ChecklistPreviewSection`, etc.).
   - Hacer que los *taps* disparen funciones en el ViewModel (`toggleActivity`, `markAbstinence`, etc.) cerrando el ciclo reactivo.

## 4. Reglas Críticas (Protocolo Meta-Prompting)

1. **NO usar `&&` en la terminal**. Usa `;` para encadenar comandos (es un sistema Windows/PowerShell).
2. **Context 7 MCP**: Úsalo si tienes dudas sobre patrones de diseño modernos en Compose o StateFlow.
3. **Consistencia Visual y de Tono**: Mantén la UI madura, orgánica y compasiva. Lee `AGENTS.md` y `docs/tono-comunicacion.md`. No agregues alertas policiales rojas ni copys humillantes; usa "La base está baja", "Una acción mínima ahora", "Esto es una señal, no una condena".
4. **Respeta los Tipos**: Ten cuidado con `authorReference: String?` en frases, y respeta la jerarquía de `ScoreState` descrita en el punto 1.
