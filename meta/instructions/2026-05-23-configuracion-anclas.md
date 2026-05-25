# Instrucción de Refactor de Configuración de Anclas (2026-05-23)

## 1. Contexto y Objetivos

Modificar la ventana interna de configuración de anclas (tanto para agregar una existente como para crear una personalizada) para que la configuración sea consistente y constructora de consistencia. Las anclas ya no tienen frecuencia mensual ni de 1 vez por semana.

## 2. Requerimientos de Dominio y Lógica Interna

* **Meta Semanal (`weeklyFrequencyTarget`)**: Un entero entre 2 y 7 (veces por semana). Se guarda en la base de datos en `targetCount` con `targetPeriod = "Week"` y `cadence = "Weekly"`.
* **Duración del Compromiso (`commitmentDurationMonths`)**: Un entero nullable (`Int?`).
  * Si es `null` significa "Indefinido".
  * Se almacena como una nueva columna `commitmentDurationMonths` en `user_activity_configs`.
* **Tiempo por Sesión (`sessionTargetMinutes`)**: Un entero que representa los minutos objetivo.
  * No debe superar los 900 minutos (15 horas diarias).
  * Se guarda en `targetValue` en `user_activity_configs`.
* **Migración de Room**: Subir la versión de la base de datos de 4 a 5. Escribir `MIGRATION_4_5` para agregar la columna `commitmentDurationMonths` a la tabla `user_activity_configs`.

## 3. Requerimientos de Interfaz de Usuario (UX)

### Paso 1: Selección de Meta Semanal
En `ActivityConfigSection` y `CreateCustomActivitySection`, reemplazar el componente `GoalPresetGrid` por una fila/grilla simplificada que permita seleccionar rápidamente la meta semanal mediante botones:
* **2 veces/semana**
* **3 veces/semana**
* **4 veces/semana**
* **5 veces/semana**
* **6 veces/semana**
* **7 veces/semana**

*Nota: No usar 1 vez por semana como opción.*

### Paso 2: Diálogo/Modal de Duración del Compromiso
Inmediatamente después de seleccionar la meta semanal, debe abrirse un cuadro/modal que pregunte:
`¿Cuántos meses quieres sostener esta ancla?`
Opciones rápidas del modal:
* **Indefinido** (preseleccionado por defecto)
* **3 meses**
* **5 meses**
* **7 meses**
* **9 meses**
* **11 meses**
* **13 meses**
* **Personalizado** (despliega un campo de texto numérico para ingresar un valor libre)

Nota sugerida dentro del modal:
> Te recomendamos dejarlo como indefinido si esta ancla representa una base que aporta estabilidad a tu vida general.

Al presionar **Aceptar**:
1. Se cierra el modal.
2. Se regresa a la ventana principal de configuración.
3. El valor de meses se almacena temporalmente y se guardará al presionar "Guardar ancla" o "Crear ancla".

### Paso 3: Configuración de Tiempo Objetivo
* El límite del selector de horas y minutos por sesión pasa de 8 horas a 15 horas diarias (900 minutos).
* Evitar que el usuario pueda configurar más de 15 horas.

## 4. Archivos a Modificar

1. **`app/src/main/java/dev/panopt/autonomia/data/Entities.kt`**:
   * Agregar `val commitmentDurationMonths: Int? = null` a `UserActivityConfigEntity`.
2. **`app/src/main/java/dev/panopt/autonomia/data/AutonomiaDatabase.kt`**:
   * Cambiar `version = 4` a `version = 5`.
   * Agregar `MIGRATION_4_5` con `ALTER TABLE user_activity_configs ADD COLUMN commitmentDurationMonths INTEGER` y registrarlo en el builder.
3. **`app/src/main/java/dev/panopt/autonomia/domain/activity/ActivityDefinition.kt`**:
   * Agregar `val commitmentDurationMonths: Int? = null` a `ActivityDefinition`.
4. **`app/src/main/java/dev/panopt/autonomia/data/local/mapper/DomainMappers.kt`**:
   * Mapear `commitmentDurationMonths` en `mergeToDomain`.
5. **`app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt`**:
   * Actualizar la firma de `addActivityAsAnchor` y `configureActivity` para aceptar y persistir `commitmentDurationMonths`.
6. **`app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardViewModel.kt`**:
   * Actualizar la firma de `addActivityAsAnchor` y `createActivity` para pasar `commitmentDurationMonths` al repositorio.
7. **`app/src/main/java/dev/panopt/autonomia/ui/anchors/AnchorConfigScreen.kt`**:
   * Modificar `ActivityConfigSection` y `CreateCustomActivitySection` para implementar el nuevo flujo de meta semanal -> modal de duración -> límite de 15 horas.
8. **Documentación**:
   * Actualizar `docs/configuracion-canonica-sistema-v1.md`, `docs/definicion-tablas-room-v1.md`, y `docs/nucleo-dominio-autonomia.md`.
