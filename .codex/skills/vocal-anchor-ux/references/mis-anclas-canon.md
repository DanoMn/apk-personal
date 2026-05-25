# Referencia rapida: Mis anclas

Fuente canonica completa: `docs/mis-anclas-ux-canon-v1.md`.

## Invariantes de producto

- Anclas requieren `weeklyFrequencyTarget` (`2..7`) y `sessionTargetMinutes`
  (`1..900`).
- `commitmentDurationMonths = null` significa `Indefinido`; no es ausencia de
  configuracion.
- No crear metas mensuales para anclas.
- Configuracion rapida > Anclas ajusta anclas existentes; no administra catalogo.

## Orden de formulario

Usar siempre:

1. Identidad o nombre.
2. Tiempo objetivo.
3. Meta semanal.
4. Duracion del compromiso.
5. Acciones.

En actividad personalizada, la capa va fija encima de los botones, fuera del
scroll principal.

## Bottom sheet

Debe ser adaptativo:

```kotlin
BoxWithConstraints {
    val maxSheetHeight = maxHeight * 0.94f
    Column(Modifier.heightIn(max = maxSheetHeight)) { ... }
}
```

No usar `fillMaxHeight(0.94f)` para este sheet.

## Duracion

Mostrar la guia de `Indefinido` dentro del dialogo de configurar meses, no en el
formulario.

## Wheel

Minutos `0..59`. Notificar cambios solo cuando el scroll se asienta.
