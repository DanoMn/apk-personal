# Rediseño de UI para Soportes (Dashboard & Config)

## Contexto y Petición
El usuario solicita dos ajustes clave para el manejo de "Soportes" (`ActivitySurface.Support`):
1. **Unificación visual en Configuración**: Igualar las dimensiones de las tarjetas de `SupportsConfigScreen.kt` para que tengan el mismo padding y altura que las de `AnchorConfigScreen.kt`.
2. **Rediseño del Componente en Dashboard**: Rehacer `SupportsPreviewSection.kt` para que sea intuitivo.
   - Las reglas de negocio (UX Inversa) dictan que los items deben aparecer **marcados por defecto**.
   - El usuario solo debe desmarcar lo que no hizo.
   - El botón para guardar y el botón para marcar/desmarcar todo deben ser **prominentes** (no solo texto).

## Análisis de Context 7 & Mejores Prácticas (UI/UX)
Para una UX inversa (donde el usuario interviene para registrar un *fallo* o una *omisión*, en lugar de un logro), la fricción debe ser mínima, pero las acciones destructivas (desmarcar) deben ser claras. El diseño debe transmitir paz y estructura, acorde a los lineamientos del proyecto (colores cartón, coral mate, tipografía editorial).

### Propuestas de Rediseño (Componente Dashboard)

#### Opción A: "Chips Orgánicos" (Envolventes)
En lugar de una lista vertical pesada, los soportes se presentan como una nube de chips redondeados (WrapLayout).
- **Estado por defecto**: Todos los chips tienen un fondo coral suave/cartón y un icono de "check" visible.
- **Interacción**: Al tocar un chip, este se "apaga" (fondo gris/muted, texto tachado suavemente).
- **Acciones**: Debajo de la nube de chips, dos botones anchos. Un botón secundario para "Desmarcar todos" y un botón principal prominente "Guardar Registro Diario".
- **Pro**: Ahorra mucho espacio vertical y hace que la pantalla se sienta menos como una lista de tareas corporativa.

#### Opción B: "Panel Editorial Sólido" (Lista Mejorada)
Se mantiene la estructura de lista, pero se enriquece el contenedor.
- **Encabezado Activo**: El encabezado del panel contiene un botón estilizado (tipo pastilla) para "Seleccionar / Desmarcar Todos".
- **Items Claros**: Cada item de la lista tiene un padding generoso (min 48dp de altura para accesibilidad) con el CheckBox en estado `checked` por defecto (usando un color ámbar/cartón en lugar del coral de logros).
- **Acción Principal**: Un botón full-width (ocupando todo el ancho) al final de la tarjeta que dice "Guardar Soportes", que usa el color principal del dashboard para llamar a la acción.

#### Opción C: "Tarjetas Colapsables de Alta Jerarquía"
- **Estado colapsado**: Muestra un resumen visual (ej. "5/5 soportes mantenidos hoy") con un botón rápido de "Guardar intacto".
- **Estado expandido**: Muestra la lista de soportes. Los botones de acción son prominentes flotando en la parte inferior del contenedor. Se usan componentes `Box` con `Modifier.clickable(Role.Button)` bien definidos, fondos contrastantes y bordes redondeados suaves.

## Cambios Técnicos a Realizar (Una vez elegida la opción)
1. Editar `SupportsConfigScreen.kt`:
   - Cambiar padding interno de tarjetas de `12.dp` a `14.dp`.
   - Cambiar tamaño de `LayerStamp` de `22` a `24`.
   - Cambiar altura de botones (Quitar, Agregar) de `32.dp` a `36.dp`.
2. Reemplazar `SupportsPreviewSection.kt` completamente basado en la Opción elegida por el usuario.
3. Asegurar que el estado "Marcar/Desmarcar" todo actualice el estado local correctamente y el botón "Guardar" dispare la lógica correspondiente.
