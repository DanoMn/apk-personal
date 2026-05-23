# Instrucción Refinada: Prototipado del Sistema de Pendientes con Selección de Capas

## Contexto y Objetivo
El objetivo es rediseñar y prototipar la interfaz y lógica del sistema de pendientes en la aplicación Android. Se busca una experiencia de usuario (UX) extremadamente ágil, fluida y sin fricciones, inspirada en la facilidad de uso y la inmediatez de un **block de notas / Google Keep** o una lista de compras. El usuario debe poder apuntar cosas rápidamente y marcarlas/completarlas con un solo toque.

### Terminología Canónica y Acceso
- El término canónico de negocio según `docs/decisiones-capas-actividades-v1.md` y `docs/vocal-01-filosofia-producto.md` es **"Pendientes"** (que mapea a la entidad `Task` en el código).
- El acceso se realiza abriendo el sandwich menú lateral (drawer) y seleccionando la opción **"Pendientes"** (que actualmente dispara `onOpenTasks`).
- Consolidaremos todas las etiquetas en la UI (como en el menú de registro principal de sheets) para usar uniformemente **"Pendientes"** en español.

### Iconografía y Capas Establecidas
Asociaremos de forma directa y rápida un pendiente a una de las 5 capas principales de la app utilizando los iconos/símbolos y colores ya definidos en [DashboardIcons.kt](file:///d:/APK-Personal/app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardIcons.kt):
1. **Interior** (`layer_interior`) -> Icono: `InteriorLayerIcon` -> Color: `palette.layerInterior`
2. **Cuerpo** (`layer_cuerpo`) -> Icono: `WavesIcon` -> Color: `palette.layerBody`
3. **Conducta** (`layer_conducta`) -> Icono: `InfinityIcon` -> Color: `palette.layerConduct`
4. **Vínculos** (`layer_vinculos`) -> Icono: `VinculosLayerIcon` -> Color: `palette.layerVinculos`
5. **Proyecto** (`layer_proyecto`) -> Icono: `ProjectTriangleIcon` -> Color: `palette.layerProject`

Si un pendiente no tiene capa asociada, se considera de contribución Neutral.

## Cambios Solicitados

### 1. Unificación en la UI
- Cambiar la etiqueta `"Pendientes puntuales"` en `EntryMenuPanel` de `DashboardPanels.kt` a **"Pendientes"** para ser consistente con el drawer lateral y la tarjeta de soporte.

### 2. Rediseño del Panel de Pendientes (`TasksPanel` en [DashboardPanels.kt](file:///d:/APK-Personal/app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardPanels.kt))
- **UX de Lista Fluida (Estilo Block de Notas)**:
  - La visualización debe transmitir la simplicidad de una libreta táctil y limpia.
  - Renderizar cada pendiente activo en un contenedor plano (`palette.bgSurface`) con esquinas redondeadas (`14.dp`) y un espaciado equilibrado.
  - Al lado izquierdo del título del pendiente, si tiene una capa asignada (`layerId != null`), mostrar su icono establecido correspondiente (`InteriorLayerIcon`, `WavesIcon`, etc.) pintado con el color específico de esa capa.
  - Al lado derecho, un check circular discreto e interactivo (ej. un círculo con trazo sutil que muestra el icono `CheckIcon` al ser pulsado) que permita completar el pendiente directamente con un solo toque ("check, check, check"), eliminándolo/archivándolo de la lista de pendientes activos de forma fluida.
- **Creación Rápida e Inmediata**:
  - Mantener el campo de texto con la etiqueta `"Nuevo pendiente"`.
  - Reemplazar el checkbox `"Aporta al core"` y la fila de botones de texto abreviados por una fila horizontal elegante de los 5 símbolos de las capas.
  - Cada símbolo se pintará con su color de capa mezclado o con menor opacidad (`mix(color, 0.4f, palette.bgSurface2)`) cuando no esté seleccionado, y se iluminará por completo con su color nativo y un fondo redondeado sutil o indicador visual de selección cuando el usuario lo toque.
  - La selección será mutuamente excluyente (solo se puede elegir una capa a la vez). Si se toca la capa seleccionada actualmente, esta se deselecciona.
  - El botón final `"Agregar pendiente"` llamará a `onCreateTask(title, selectedLayerId, selectedLayerId != null)`. Al crearse, se limpiará el texto del input y la selección de la capa actual para agilizar la entrada de múltiples pendientes sucesivos, permitiendo un uso similar al de añadir elementos en un block de notas de forma consecutiva.

### 3. Estilo Visual y Tono
- Usar la tipografía y colores de `DashboardPalette` (evitar colores duros o negros puros).
- Seguir el estilo orgánico/editorial definido en `AGENTS.md` y `docs/frontend-design.md` (bordes suaves, micro-animaciones, discreción).

## Verificación Plan
- Validar mediante compilación que el código de Kotlin y Compose no presente errores de compilación.
- Verificar manualmente la armonía visual de los iconos y la selección en modo oscuro y claro.
