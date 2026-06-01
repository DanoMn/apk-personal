# Mis Soportes UX Canon v1

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Fecha: 2026-05-25
Producto: Vocal / Autonomía sin límites
Estado: canónico para la configuración rápida de soportes y su panel.

Este documento cierra el patrón UX/UI vigente de **Soportes** dentro del flujo de la configuración rápida en el Dashboard. Si entra en conflicto con propuestas anteriores, este documento es la única fuente de verdad.

## Principio

Los "Soportes" (Cuidado base) son las actividades secundarias que sirven de red de seguridad y mantenimiento diario. A diferencia de las anclas, no requieren un ajuste constante de metas, frecuencias ni compromisos rigurosos. Su interacción debe ser ágil, y su pantalla debe sentirse como un acceso rápido, sin inflarse visualmente ni saturar la pantalla.

## Estructura del Panel Rápido de Soportes

El panel de Configuración Rápida de Soportes (`SupportsConfigPanel`) es una hoja modal (ModalBottomSheet) que sigue reglas arquitectónicas estrictas de Compose para evitar comportamientos erráticos.

### 1. Altura y Dimensión
- **Regla:** El panel de Soportes debe estar topado a un máximo del **60% de la altura de la pantalla** (`fillMaxHeight(0.6f)`).
- **Razón:** Al no tener ajustes profundos (como el wheel picker o las metas de las Anclas), el panel no necesita adueñarse de la pantalla completa. Es una lista sencilla de agregar/quitar, por lo que una altura controlada mantiene la sensación de ligereza.

### 2. Zonas Estáticas vs. Zonas de Scroll
El árbol del panel se divide estrictamente en 3 áreas:

1. **Cabecera (Header):** Completamente estática y fuera de la zona de scroll. Contiene el título ("Soportes") y el contador de activos.
2. **Zona de Contenido (Listas):** Es la **única** sección que contiene el modificador de scroll vertical (`verticalScroll(rememberScrollState())`) y ocupa el espacio intermedio utilizando el modificador `weight(1f)`. Aquí viven las listas "Mis soportes" (activos) y "Agregar soporte" (recuperables).
3. **Pie (Footer / Botón):** Completamente estático en la base inferior de la pantalla. Nunca debe hacer scroll junto con la lista.

### 3. Botón "Ver Catálogo Completo"
Debe ser visualmente idéntico al botón de "Abrir Mis anclas" para mantener consistencia transversal en la UI de la aplicación:

- **Fondo:** `palette.colorCardboard`
- **Color de Texto:** `palette.bgBase`
- **Bordes:** `RoundedCornerShape(14.dp)`
- **Altura:** `48.dp`

## Físicas del ModalBottomSheet y Protección de Gestos

El uso del `ModalBottomSheet` nativo de Material 3 presenta un riesgo documentado en este proyecto: deslizar (drag) hacia abajo dentro del contenido de las listas o cuadrículas puede provocar el cierre involuntario (dismiss) del panel.

Para solucionar esto, se aplica una protección mediante `NestedScrollConnection`:

### Bloqueo de Overscroll (`blockNestedScrollConnection`)
Toda la envoltura (`Box`) del contenido de los bottom sheets rápidos en `DashboardPanels.kt` está interceptada por un `NestedScrollConnection` especial.
- **Función:** Consume todo el "overscroll" vertical (`available.copy(x = 0f)`).
- **Efecto en la UI:** El usuario puede intentar deslizar hacia abajo una lista de anclas, soportes o tareas sin que la ventana modal reaccione cerrándose. 
- **Modo de cierre correcto:** El usuario solo puede arrastrar la ventana modal para cerrarla agarrándola desde el controlador superior (drag handle / "el top") o tocando el área oscura fuera del panel.

*Cualquier intento futuro de agregar paneles rápidos o cuadrículas (como `EntryMenuPanel`) debe respetar este envoltorio para no reinstaurar cierres accidentales.*

## Gestión de Estado en la Sesión Rápida

- Quitar un soporte desde este panel rápido **no lo elimina inmediatamente de la base de datos**. 
- Las eliminaciones se guardan en un estado temporal de sesión (`removedIds`).
- El componente `DisposableEffect` procesa y persiste los cambios solo cuando el usuario cierra la ventana. 
- **Razón UX:** Permite que el usuario quite un soporte, este baje a la lista de "Agregar soporte" de forma instantánea, y pueda revertir su decisión con un solo tap ("Agregar") sin hacer consultas innecesarias a la capa de persistencia (Room).

## Fuentes de Verdad en Código

- `dev.panopt.autonomia.ui.supports.SupportsConfigPanel.kt`: Formulario rápido de Mis soportes (el verdadero, no la copia fantasma).
- `DashboardPanels.kt`: Host global del ModalBottomSheet que provee la protección `blockNestedScrollConnection`.
