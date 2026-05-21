# Meta-Prompting

Este documento sirve como registro y bitácora de diseño para las instrucciones complejas o ambiguas.

## Registro de Términos y Decisiones

Aquí se documentarán los conceptos técnicos, términos de dominio y dudas de arquitectura que surjan durante el proceso de traducción de las peticiones de usuario a soluciones técnicas.

### Conceptos del Dominio (APK-Personal / Vocal)

* **Dashboard Components (UI)**:
  * `LayerPill`: Representa "Capas de hoy". Requiere centrado vertical y barras de progreso más grandes.
  * `SignalCard`: Representa "Señales". Necesita reestructuración visual para legibilidad tipográfica.
  * `StreakCard`: Representa "Sobriedad". Necesita reestructuración visual para legibilidad tipográfica.
  * `SupportCard`: Representa "Soportes". La tipografía actual es demasiado pequeña.
  * `FlagIcon` / `ChecklistIcon`: Presentan problemas de centrado interno de sus SVG/Paths en sus contenedores respectivos.

### Backend / Data y Domain (21/05/2026)

* **Hechos vs Inferencias**: Room almacena *hechos* de forma inmutable, mientras que el *dominio* calcula el estado general ("en marcha", "riesgo", "estable"). La UI (Compose) nunca debe contener lógica de negocio.
* **Modelo de Capas (Layers)**: Las capas principales han sido reducidas a 5: `Interior`, `Cuerpo`, `Conducta`, `Vínculos`, `Proyecto`.
* **Rachas y Abstinencias (Sobriedad)**: No son un checklist común, son un modelo propio (`AbstinenceTrack`, `AbstinenceLog`) y operan como *feature* separada debido a su peso en el algoritmo.
* **Context 7 MCP**: Servidor configurado para proveer las mejores prácticas de arquitectura y código durante el desarrollo de la etapa actual.
