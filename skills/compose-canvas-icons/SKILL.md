---
name: compose-canvas-icons
description: Reglas y bugs conocidos para renderizar iconos con Canvas en Jetpack Compose. Contiene el fix crítico de scale pivot que causa desplazamiento de iconos.
---

# Compose Canvas Icons — Bugs conocidos y mejores prácticas

## Contexto

Este proyecto usa iconos dibujados manualmente con `Canvas`, `Path` y `PathParser`
en lugar de assets SVG o `ImageVector`. Todos los iconos viven en:

- `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardIcons.kt`

Las funciones de transformación que escalan el viewport 24×24 al tamaño real del
Canvas son:

- `drawIconViewport()` — para iconos dibujados con primitivas (drawLine, drawCircle, etc.)
- `drawPathInViewport()` — para iconos dibujados con Path precompilados
- `SpiralLogo` — tiene su propia transformación inline (viewport 64×64)

---

## ⚠️ BUG CRÍTICO: `scale()` con pivot incorrecto

### Síntoma

Todos los iconos se desplazan hacia la **esquina superior izquierda** del Canvas.
El desplazamiento es proporcional a la densidad del dispositivo:

| Densidad | Efecto |
|----------|--------|
| 1x       | Sin desplazamiento (bug invisible) |
| 1.5x     | Ligero desplazamiento, iconos parcialmente recortados |
| 2x       | Centro del icono aparece en esquina superior izquierda |
| 3x+      | Icono casi invisible, solo se ve un fragmento |

### Causa raíz

```kotlin
// ❌ INCORRECTO — pivot por defecto es `center` del Canvas
withTransform({
    translate(dx, dy)
    scale(scale, scale)  // pivot = Offset(size.width/2, size.height/2)
}) {
    block()
}
```

`DrawTransform.scale(scaleX, scaleY)` usa `pivot = center` por defecto.
Esto escala alrededor del **centro del Canvas en píxeles**, no del origen del
viewport. Cuando `scale > 1` (cualquier densidad > 1x), el contenido del
viewport se desplaza fuera de los bounds del Canvas.

**Matemática del error** (ejemplo: Canvas 48px, scale=2, pivot=(24,24)):

```
viewport(12, 12) → screen(24 + (12-24)×2, ...) = (0, 0) ← ¡esquina superior izquierda!
viewport(0, 0)   → screen(24 + (0-24)×2, ...)  = (-24, -24) ← ¡fuera de pantalla!
```

### Solución

```kotlin
// ✅ CORRECTO — pivot en Offset.Zero
withTransform({
    translate(dx, dy)
    scale(scale, scale, pivot = Offset.Zero)
}) {
    block()
}
```

Con `pivot = Offset.Zero`, el escalado parte del origen del sistema de
coordenadas (que ya fue trasladado por `translate(dx, dy)`):

```
viewport(0, 0)   → screen(0, 0)     ← esquina superior izquierda ✓
viewport(12, 12) → screen(24, 24)    ← centro del Canvas ✓
viewport(24, 24) → screen(48, 48)    ← esquina inferior derecha ✓
```

### Puntos de aplicación

Este fix debe aplicarse en **todas** las funciones que usan `withTransform` +
`scale` para mapear un viewport a un Canvas:

1. `drawIconViewport()` — viewport 24×24
2. `drawPathInViewport()` — viewport parametrizable
3. `SpiralLogo` — viewport 64×64
4. **Cualquier icono futuro** que use la misma técnica

---

## Reglas para crear iconos nuevos

### Estructura de un icono Canvas

```kotlin
@Composable
internal fun NuevoIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),  // ← siempre parámetro con default
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            // Dibujar en coordenadas 0-24
            // El centro visual del icono debe estar en (12, 12)
        }
    }
}
```

### Checklist obligatorio

- [ ] El icono acepta `modifier: Modifier = Modifier.size(22.dp)` como parámetro
- [ ] Usa `drawIconViewport {}` o `drawPathInViewport()` para el escalado
- [ ] El contenido se dibuja en viewport 0-24 (centrado en 12,12)
- [ ] La visibilidad es `internal` (no `private`)
- [ ] Si usa `PathParser`, el `Path` se envuelve en `remember {}`

### Errores comunes que NO deben repetirse

| Error | Consecuencia |
|-------|-------------|
| `scale(s, s)` sin `pivot = Offset.Zero` | Iconos desplazados a esquina superior izquierda |
| `private fun` en iconos compartidos | Otros archivos del paquete no pueden usarlos |
| `Canvas(Modifier.size(22.dp))` hardcodeado | No se puede cambiar el tamaño desde el call site |
| Olvidar `remember {}` en `PathParser` | Recálculo del Path en cada recomposición |

---

## Historia

- **2026-05-21**: Bug descubierto después de 3+ horas y 10+ intentos fallidos
  por múltiples agentes. Todos los agentes trataban de arreglar padding, layout
  y tamaños sin identificar que la transformación Canvas era la causa raíz.
  El fix fue agregar `pivot = Offset.Zero` en 3 líneas.
