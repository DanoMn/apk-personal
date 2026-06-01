# Configuración canónica del sistema — v1

Fecha: 2026-05-23
Proyecto: Vocal / Autonomía sin límites
Propósito: definir cómo se configura cada feature del dominio desde la capa de
configuración. Este documento es vinculante para cualquier implementación futura.

---

## 1. Ancla

### Definición (ya cerrada)

Práctica recurrente que el usuario elige porque construye su base personal. 33
actividades canónicas en el catálogo. Fuente completa: `docs/datos-room/actividades-ancla-predeterminadas-v1.md`.

### Reglas de configuración

- El usuario elige del catálogo o crea una personalizada.
- **Campos obligatorios de configuración**:
  - **Frecuencia semanal (`weeklyFrequencyTarget`)**: obligatoria. Botones rápidos del 2 al 7 (veces por semana). No existe opción rápida de 1 vez/semana ni frecuencia mensual para anclas.
  - **Tiempo objetivo por sesión (`sessionTargetMinutes`)**: obligatorio. Máximo permitido: 15 horas o 900 minutos.
  - **Duración del compromiso (`commitmentDurationMonths`)**: siempre se decide en el flujo, pero puede guardarse como `null` cuando el usuario elige **Indefinido**. Ese `null` es una configuración válida, no ausencia de target.
- **Sin excepción**: no se puede guardar un ancla sin `weeklyFrequencyTarget` ni sin `sessionTargetMinutes`.
- **Persistencia actual**: las anclas nuevas guardan `cadence = "Weekly"`, `targetPeriod = "Week"`, `targetCount = weeklyFrequencyTarget`, `targetValue = sessionTargetMinutes`, y además los campos explícitos `weeklyFrequencyTarget`, `sessionTargetMinutes`, `commitmentDurationMonths`.
- Aparece TODOS los días en el dashboard, aunque la meta sea de varias veces por semana. Si el usuario cumple más días, se registra como superávit positivo.
- UX normal: el usuario marca lo que SÍ hizo.
- Sin límite de cantidad, pero se recomiendan pocas.
- **Visión futura (no implementar aún):** al mantener presionado un item en la
  checklist, el usuario podrá modificar el tiempo real que hizo. Si hizo más
  del target, se registra superávit. Si hizo menos, se registra déficit.

### UX/UI vigente

Fuente canonica: `docs/frontend/mis-anclas-ux-canon-v1.md`.

- Orden del editor: identidad/nombre, tiempo objetivo, meta semanal, duracion
  del compromiso, acciones.
- La recomendacion sobre `Indefinido` vive dentro del dialogo de configurar
  duracion.
- En Configuracion rapida > Anclas solo se ajustan anclas configuradas; no se
  muestra catalogo ni checklist.

### Lo que NO es un ancla

- Una regla ("no usar celular en cama")
- Un pendiente administrativo ("pagar alquiler")
- Una acción de mantenimiento ("bañarse")

---

## 2. Soporte

### Definición

Acción de mantenimiento diario que sostiene dignidad y estructura. Complementa
las anclas, no compite con ellas. 8 actividades canónicas en el seed.

### Reglas de configuración

- El usuario elige del catálogo de Soportes.
- Puede crear Soportes personalizados: escribe el nombre y selecciona a qué capa
  pertenece (Interior, Cuerpo, Conducta, Vínculos, Proyecto).
- **Sin targets.** Por diseño del dominio.
- **La capa es obligatoria.** Todo Soporte debe alimentar una capa.
- **Sin límite de cantidad.** El usuario configura los que necesite.
- UX inversa: el sistema asume todo cumplido. El usuario solo desmarca lo que
  NO hizo ese día.

### UX/UI vigente

Fuente canónica: `docs/frontend/mis-soportes-ux-canon-v1.md`.

- El panel de configuración rápida de Soportes tiene una altura máxima del 60%.
- Las vistas estáticas (cabecera y botón de catálogo) están fuera de la zona de scroll.
- Comparte el diseño de botones con Anclas (`colorCardboard`).
- Protegido contra cierres accidentales mediante `NestedScrollConnection`.

---

## 3. TaskList (Pendientes)

### Definición

Tarea puntual, una sola vez, sin recurrencia. No es un hábito ni una práctica
cultivable.

### Reglas de configuración

- Se crea desde cero: el usuario escribe el nombre.
- **La capa es opcional.** Solo se asigna si el usuario considera que la tarea
  aporta a una capa. Si no tiene capa, no contribuye al scoring.
- Una vez completada, **desaparece del dashboard** pero queda almacenada en la
  base de datos.
- El usuario puede consultar su historial de tareas completadas y **revivirlas**
  (volver a ponerlas como pendientes) si lo desea.

---

## 4. Sueño

### Definición

Base fisiológica y conductual del sistema. El sueño es el piso del scoring: sin
sueño registrado, la base no está completa para estados altos.

### Configuración actual

- No tiene pantalla de configuración independiente.
- Se accede desde la tarjeta de señal de Sueño en el dashboard, que abre un
  panel inferior.
- El usuario define: hora planeada de dormir, hora planeada de despertar, hora
  real en que durmió, hora real en que despertó.
- Incluye botón de calidad (Baja / Aceptable / Buena).
- **El mínimo de 5 horas NO está implementado aún** — es dirección de diseño.

### Cambios bajo consideración

- **Quitar el botón de calidad.** No hay forma objetiva de medir la calidad del
  sueño. Queda a decisión del producto si se mantiene o se elimina.

### Visión futura (no implementar aún)

- El usuario entra a la sección de Sueño antes de dormir y activa un botón
  "Hora de dormir".
- La app bloquea el teléfono durante la ventana de sueño configurada por el
  usuario (mínimo aceptable: 5 horas).
- Al desbloquear, se registra el tiempo dormido.
- El usuario puede repetir el ciclo varias veces en una noche (bloquear →
  desbloquear → volver a bloquear). Cada bloqueo registra un segmento de sueño.
- Si el usuario desbloquea el teléfono durante la ventana, solo se registra lo
  efectivamente dormido hasta ese momento.
- Se asume la responsabilidad del usuario sobre el uso correcto de esta función.

### Impacto en scoring

- Si el usuario no registra sueño, el scoring **decae**.
- Sueño está pensado como la base del puntaje total. En un sistema de 700 a
  1000 puntos, el sueño podría ocupar los primeros 700 puntos base, y el resto
  se construye con frecuencia, capas y otras métricas.
- Esto no está implementado aún — es dirección de diseño.

---

## 5. Sobriedad

### Definición

Feature opt-in para usuarios que quieren dejar alcohol, sustancias o conductas
perniciosas. No es una actividad común: tiene racha, marca diaria, historial,
impulso, recaída y lectura protectora propia.

### Configuración

- Tracks predeterminados: **Alcohol**, **Sustancias**, **Conducta sexual**.
- El usuario puede **agregar** tracks personalizados y **eliminar** los que
  agregó. Los tracks predeterminados no se eliminan, pero pueden desactivarse.
- Debe tener **su propia ventana de configuración**, accesible desde el
  dashboard o el drawer.
- Al entrar, el usuario ve sus rachas activas y el estado de cada una.

### Registro diario

- Desde el dashboard, el usuario hace clic en el botón de Sobriedad para
  **marcar el día como limpio**.
- Esta marca se recarga cada día, adaptándose al horario del usuario (no
  necesariamente de 0 a 24 horas — se definirá más adelante).

### Diferencia entre "día sin marcar" y "recaída"

**No son lo mismo.** El sistema es más complaciente con el usuario en este tema
por ser sensible:

1. Si un día el usuario **no marca**, el sistema **no asume recaída
   automáticamente**.
2. Al día siguiente, se le pregunta: *"¿Olvidaste marcar ayer?"*
3. Esta pregunta se repite por **2 a 3 días máximo**.
4. Si no responde, recién entonces se marca como **recaída**.
5. El usuario también puede **marcar recaída manualmente** cuando lo considere
   necesario, especificando los días que duró.

### Responsabilidad del usuario sobre sus datos

- El sistema registra internamente los días sin marca y las recaídas.
- Pero **respeta la versión del usuario**: si el sistema detecta 5 días de
  recaída y el usuario dice que fueron 3, se registran 3.
- Esto aplica porque es un tema sensible. La app no es policial.
- El usuario puede modificar la duración de una recaída cuando recibe la
  notificación.

---

## 6. Reglas universales de arquitectura

1. **La configuración valida.** Lo que se guarda en la base de datos ya pasó
   todas las reglas. El dominio no debería tener que re-validar.

2. **El dominio calcula.** Recibe datos válidos, procesa, produce estado. No
   filtra, no descarta, no decide qué se muestra.

3. **El dashboard presenta.** Recibe estado del dominio y lo pinta. No define
   reglas, no filtra, no calcula.

4. **Lo que el usuario no activó no aparece, no pesa y no limita el estado.**
