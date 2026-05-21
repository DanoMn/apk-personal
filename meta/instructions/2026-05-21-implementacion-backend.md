# Pro-Prompt: Implementación del Backend (Room & Domain)

## Objetivo
Establecer la base de persistencia de datos (Room) y el núcleo del dominio (Modelos puros, Reglas de Estado) para la versión local-first de "Vocal / Autonomía sin límites".

## Contexto Técnico
- **Arquitectura**: Clean Architecture ligera (Data: Room/DAOs, Domain: Modelos puros e Inferencias, UI: Compose).
- **Regla Central**: Room guarda *hechos* históricos sin procesar. El dominio lee los hechos y calcula *inferencias* operativas.
- **MCP Context 7**: Se debe consultar el servidor MCP Context 7 para garantizar que la implementación se realiza utilizando las mejores prácticas recomendadas de arquitectura en Kotlin/Android.

## Tareas a Ejecutar
1. **Definición de Entidades Room (Hechos)**:
   - Crear entidades base: `Layer`, `Activity`, `ActivityLog`, `AbstinenceTrack`, `AbstinenceLog`, `RiskEvent`, `AnchorPhrase`, etc., tal como se especifica en `docs/definicion-tablas-room-v1.md`.
2. **Definición de DAOs y Base de Datos**:
   - Crear los DAOs con inserciones, actualizaciones y consultas básicas (observando flujos reactivos con Flow/Coroutines).
   - Configurar la inicialización de Room (`AppDatabase`).
3. **Modelos de Dominio y Repositorios**:
   - Crear los data classes limpios en la capa `domain/`.
   - Crear interfaces de repositorios en `domain/` y sus implementaciones en `data/`.
4. **Validación con Context 7**:
   - Antes de escribir el código definitivo, consultar Context 7 para asegurar que las dependencias, anotaciones y arquitecturas se adecuen a los mejores estándares y prácticas vigentes.

## Aprobación
El usuario debe revisar esta instrucción de acuerdo al protocolo de Meta-Prompting y confirmar si es lo que necesita para arrancar la implementación.
