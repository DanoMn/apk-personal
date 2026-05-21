# Problema de UI en Dashboard

## Objetivo
- **Capas de hoy**: centrar verticalmente los símbolos dentro de cada `LayerPill`.
- **Barras de progreso**: aumentar grosor y contraste para que sean notorias.
- **Tipografía**: mejorar legibilidad del texto en tarjetas de **Capas**, **Señales**, **Sobriedad** y **Racha**.
- **Tarjeta de Sobriedad** y **Señales**: reorganizar los elementos existentes sin añadir ni eliminar componentes.
- **ActionButtons**: eliminar duplicación del texto "Registrar checklist" y asegurar que el `Row` contenga solo el ícono y el texto alineados al centro.

## Requisitos de estilo
- Paleta: base oscura orgánica, cartón/beige, coral mate.
- Tarjetas planas, sin bordes duros ni sombras pesadas.
- Tipografía editorial: serif para títulos, sans‑serif limpia para controles.
- Iconografía: sellos con peso visual para capas, trazos finos y redondeados para UI.

## Protocolo de corrección
1. **Analizar la lógica de posicionamiento** actual en `DashboardScreen.kt` (especialmente `LayerPill` y `ActionButtons`).
2. **Ajustar `Modifier.align(Alignment.CenterVertically)` o usar `Box(contentAlignment = Alignment.Center)`** para centrar los iconos.
3. **Incrementar altura de `LayerPill`** (e.g., `height = 100.dp`) y definir `progressBarHeight = 8.dp`.
4. **Aplicar tipografía** con `fontSize` ≥ `14.sp` y `fontWeight = FontWeight.Medium` para textos.
5. **Reordenar composables** dentro de `SobriedadCard` y `SignalCard` usando `Column`/`Row` con `Arrangement.spacedBy` para mejor espaciado.
6. **Eliminar código duplicado** en `ActionButtons` y dejar un solo `Row` con `ChecklistIcon` + `Text`.
7. **Compilar** con `./gradlew assembleDebug` después de los cambios y validar visualmente en el emulador.

## Restricciones
- No usar `&&` en comandos.
- No ejecutar `docker‑compose build`.
- No mezclar con `digitaliza‑server`.
- Mantener los elementos existentes; solo reorganizar.
- No remover atributos de accesibilidad (`contentDescription`).

## Verificación
- Ejecutar la app y comprobar que los símbolos están centrados.
- Las barras de progreso deben ser claramente visibles.
- Texto legible en todas las tarjetas.
- No hay errores de compilación.

---
*Este documento sigue el protocolo de **Meta‑Prompting** para que otro agente continúe la solución.*
