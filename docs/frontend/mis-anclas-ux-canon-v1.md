# Mis anclas UX canon v1

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Fecha: 2026-05-25
Producto: Vocal / Autonomia sin limites
Estado: canonico para la configuracion de anclas y su panel rapido.

Este documento cierra el patron UX/UI vigente de **Mis anclas**. Si entra en
conflicto con propuestas anteriores, este documento manda para la experiencia
de configuracion de anclas.

## Principio

Mis anclas no es un catalogo de productividad. Es la pantalla donde el usuario
elige que practicas sostienen su base personal.

La interfaz debe ayudar a responder, sin friccion:

- que anclas tengo activas;
- que tiempo realista le asigno a cada una;
- cuantas veces por semana quiero sostenerla;
- por cuanto tiempo quiero comprometerme;
- a que capa aporta una actividad personalizada.

La pantalla debe sentirse concreta, sobria y tactil. No debe parecer un wizard
corporativo ni un formulario clinico.

## Contrato de dominio

Toda ancla configurada debe guardar:

- `weeklyFrequencyTarget`: obligatorio, rango `2..7`.
- `sessionTargetMinutes`: obligatorio, rango `1..900`.
- `commitmentDurationMonths`: nullable. `null` significa `Indefinido` y es una
  decision valida.

No existe meta mensual para anclas. Lo que antes podia confundirse con
"objetivo mensual" es **duracion del compromiso**.

## Orden canonico del editor

El formulario para agregar, editar o ajustar una ancla debe seguir este orden:

1. Identidad de la actividad.
2. `Tiempo objetivo`.
3. `Meta semanal`.
4. `Duracion del compromiso`.
5. Acciones: cancelar y guardar.

Razon:

- el tiempo es el dato mas fisico e inmediato;
- poner el wheel arriba reduce conflicto entre scroll de pantalla y scroll del
  selector;
- la frecuencia semanal depende mejor de una idea previa de esfuerzo por
  sesion;
- la duracion del compromiso es una configuracion secundaria, no debe secuestrar
  el flujo.

## Time wheel

El selector de tiempo usa dos ruedas:

- horas: `0..15`;
- minutos: `0..59`.

Reglas UX:

- no usar saltos de 5 minutos;
- no notificar cambios al estado padre mientras la rueda sigue scrolleando;
- actualizar el valor solo cuando el scroll se asienta;
- si horas llega al maximo de 15, minutos debe quedar en 0 para respetar
  `MAX_ANCHOR_SESSION_MINUTES = 900`.

## Meta semanal

La frecuencia se elige con botones rapidos del 2 al 7.

Reglas:

- tocar una frecuencia no abre ningun dialogo automaticamente;
- solo actualiza el valor seleccionado;
- no existe opcion rapida de 1 vez por semana;
- no existe frecuencia mensual para anclas.

## Duracion del compromiso

El boton muestra el estado actual, por defecto `Indefinido`, y debe verse como
boton de configuracion, no como campo de texto.

Reglas:

- el dialogo de meses solo se abre al tocar `Configurar`;
- la recomendacion sobre dejarlo como `Indefinido` vive dentro del dialogo, no
  debajo del boton;
- el texto de ayuda es:
  "Te recomendamos dejarlo como indefinido si esta ancla representa una base que
  aporta estabilidad a tu vida general."

Razon:

- la recomendacion pertenece al momento de decidir meses;
- mantenerla fuera del formulario reduce ruido visual;
- evita que seleccionar frecuencia parezca obligar una decision de duracion.

## Actividad personalizada

El flujo para crear una ancla personalizada debe mostrar:

1. `Nombre`.
2. `Tiempo objetivo`.
3. `Meta semanal`.
4. `Duracion del compromiso`.

La seleccion de `Capa` no debe quedar dentro del contenido scrolleable. Debe
estar fija justo encima de `Cancelar` / `Crear ancla`.

Razon:

- la capa es la decision de destino de la actividad;
- el usuario debe verla antes de confirmar;
- moverla al footer evita que quede enterrada al final del scroll.

## Edicion de anclas

Cada tarjeta de `Anclas actuales` en la pantalla principal debe permitir:

- `Editar`: abre el mismo formulario base en modo edicion;
- `Quitar`: desactiva la configuracion del usuario;
- eliminar definitivamente solo cuando la actividad es personalizada.

El modo edicion debe precargar:

- tiempo objetivo;
- meta semanal;
- duracion del compromiso.

El guardado usa el upsert existente de configuracion de usuario. No debe crear
otro contrato de persistencia si los campos vigentes alcanzan.

## Configuracion rapida > Anclas

El panel rapido de Anclas sirve para ajustar, no para administrar catalogo.

Debe mostrar solo:

- anclas ya configuradas por el usuario;
- resumen de capa, minutos, frecuencia semanal y duracion;
- accion `Ajustar` por ancla;
- accion final `Abrir Mis anclas`.

No debe mostrar:

- catalogo completo;
- buscador;
- filtros por capa;
- checklist;
- acciones de registro diario;
- `Quitar`, salvo que se decida explicitamente ampliar el alcance.

`Ajustar` reutiliza el editor base de ancla en modo edicion.

## Sheet de configuracion rapida

El bottom sheet debe ser adaptativo:

- contraerse cuando hay poco contenido;
- crecer cuando el contenido lo necesita (ej. Anclas);
- respetar `navigationBarsPadding()`.

### Protección de gestos (Swipe to Dismiss)

Dado que `ModalBottomSheet` asume que deslizar en cualquier lugar de su superficie cierra el panel, el contenedor principal de los bottom sheets en `DashboardPanels.kt` **debe interceptar el scroll anidado**. 

Patrón Compose canónico para bloquear cierres accidentales por overscroll:

```kotlin
val blockNestedScrollConnection = remember {
    object : NestedScrollConnection {
        override fun onPostScroll(...) = available.copy(x = 0f)
        override suspend fun onPostFling(...) = available.copy(x = 0f)
    }
}

Box(
    modifier = Modifier
        .fillMaxWidth()
        .nestedScroll(blockNestedScrollConnection)
        .navigationBarsPadding()
) {
    // contenido de las vistas
}
```

Gracias a esto, la ventana modal **solo se puede cerrar deslizando desde el drag handle superior** (el "top") o desde un área que no sea capturada por un contenedor scrolleable interno, protegiendo al usuario de cierres accidentales al navegar listas.

## Navegacion atras

En Configuracion rapida:

- si el usuario esta dentro de `Anclas`, atras debe volver al menu anterior;
- si esta en el menu raiz, atras debe cerrar el sheet;
- nunca debe cerrar la app por accidente.

## Feedback visual

El sheet no debe oscurecerse ni parpadear al tocar zonas internas.

Regla tecnica:

- los `clickable` usados para consumir toques en overlay/sheet deben usar
  `MutableInteractionSource` e `indication = null`.

## Fuentes de verdad en codigo

- `AnchorEditorForm.kt`: formulario compartido de agregar, editar y ajustar.
- `AnchorConfigScreen.kt`: pantalla completa de Mis anclas y creacion
  personalizada.
- `AnchorConfigPanel.kt`: panel rapido de Anclas.
- `GoalPresetGrid.kt`: frecuencia semanal y duracion del compromiso.
- `TimeWheelPicker.kt`: selector de horas/minutos.
- `DashboardPanels.kt`: host global del sheet de configuracion rapida.
