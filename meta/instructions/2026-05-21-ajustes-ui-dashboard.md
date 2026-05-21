# Pro-Prompt: Ajustes y Correcciones Visuales del Dashboard (21/05/2026)

## Contexto y Objetivo
El objetivo de esta iteración es mejorar la legibilidad y alineación de múltiples elementos del `DashboardScreen` en base al diseño orgánico y editorial del producto. No se deben añadir ni quitar elementos, sino reorganizar los existentes y corregir tamaños y alineaciones.

## Instrucciones Técnicas

1. **Capas de hoy (`LayerPill`)**:
   - Aumentar significativamente el tamaño (grosor) de la barra de progreso (actualmente es de `5.dp`, probar con `8.dp` o `10.dp`).
   - Corregir el centrado vertical del símbolo/icono.
   - Aumentar el tamaño de fuente del label (actualmente `11.2.sp`) para que sea más notorio y legible.

2. **Señales (`SignalCard`)**:
   - Rediseñar la distribución interna de la tarjeta para mejorar la jerarquía y legibilidad sin alterar los elementos (`label`, `value`, `meta`, icono).
   - Aumentar o destacar la tipografía de lectura rápida.

3. **Sobriedad (`StreakCard`)**:
   - Rediseñar la distribución interna (quizás alinear mejor los textos "días" y el número de la racha) para hacer el progreso de abstinencia visualmente más claro.
   - Ajustar tipografía para mayor notoriedad.

4. **Soportes (`SupportCard` y `SupportListLine`)**:
   - Aumentar el tamaño de la fuente. Actualmente los textos pequeños (títulos y `SupportListLine`) están entre `11.52.sp` y `13.sp`, lo cual es muy difícil de leer. Incrementarlos a tamaños que prioricen el control humano y la claridad (ej. `14.sp`, `13.sp` mínimo).

5. **Iconos descentrados**:
   - Corregir el centrado del icono de la Bandera (`FlagIcon`) en su contenedor de botón (riesgo).
   - Corregir el centrado del icono y texto en el botón "Registrar checklist".
   - Corregir el centrado de los símbolos de las checklists (`CheckItem` o los iconos por capa). Probablemente se deba a ajustes de `Alignment` o paddings asimétricos en el Canvas.

## Pregunta de Validación
¿Es este Pro-Prompt lo que necesitas para que comience la ejecución de los ajustes visuales del Dashboard?

> **Nota de Contexto Añadida:**
> Anteriormente otros agentes intentaron centrar los símbolos (íconos, checklist, etc.) y fracasaron. Por tanto, antes de implementar a ciegas, se requiere realizar un análisis profundo de la lógica de posicionamiento (por ejemplo, cómo dibujan internamente los `Canvas`, `drawIconViewport`, y el sistema de `Alignment` / `Arrangement` de Jetpack Compose). Solo se aplicarán los cambios una vez que se entienda la causa raíz del fallo de los agentes previos.
