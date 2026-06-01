# Handoff — Sueño: follow-ups post-implementación

> Para retomar en **sesión nueva** sin arrastrar contexto. Fecha: 2026-05-29 ·
> Proyecto: apk-personal (Vocal). Engram: buscar `sdd/sleep-consumer/*` y
> `sleep-consumer/design-decisions`.

## Estado: la feature Sueño está TERMINADA

`sleep-consumer` cerró el ciclo SDD completo (diseño → proposal → spec → design →
tasks → apply 4 PRs → verify PASS WITH WARNINGS → archive). **176 tests verdes**,
build limpio. Commits en `main` (SIN push):

- `e9d187e` docs(sleep): design + planning
- `7ec22c3` feat(sleep): implementación
- `4ad6e6e` docs(sleep): archive + specs canónicos

Contrato: `docs/sueno/decisiones-diseno-sueno-v1.md`. Specs canónicos: `openspec/specs/sleep-*`.

---

## Follow-up 1 — Fricción del permiso (UX in-app RESUELTA · device-side sigue manual)

> **Estado (2026-05-29):** La parte **codeable** está hecha. El prompt de permiso del
> `AutoModeCard` (`SleepConfigScreen.kt`) ahora guía **dos pasos** en vez de uno:
> (1) "Abrir info de la app" → `ACTION_APPLICATION_DETAILS_SETTINGS` para desbloquear
> *Permitir ajustes restringidos*; (2) "Ir a Acceso de uso" → `ACTION_USAGE_ACCESS_SETTINGS`.
> Nuevo intent `TelemetryPermission.appDetailsSettingsIntent(context)`. Build limpio.
> No se agregó detección heurística de Restricted Settings: no hay API pública confiable
> para saber si el install source es "no verificado", así que se ofrece el escape y el
> usuario lo usa si el toggle aparece bloqueado. Lo de abajo queda como referencia del
> síntoma y los atajos device-side (que el usuario igual tiene que ejecutar a mano).


**Síntoma (verificado en device del dueño — Pixel 7 Pro / GrapheneOS):** al activar el
modo automático, Android muestra *"A la app se le negó el acceso… puede poner en riesgo
tu información financiera y personal… permiso restringido"* y bloquea el toggle.

**Causa raíz (investigada, NO es solo GrapheneOS):**
- El permiso es `PACKAGE_USAGE_STATS` (Acceso de uso), pedido por la capa
  `device-telemetry` (`platform/telemetry/TelemetryPermission.kt`,
  `UsageStatsTelemetrySource.kt`; declarado en `AndroidManifest.xml`).
- Android 13+ tiene **Restricted Settings**: bloquea permisos sensibles para apps cuya
  **fuente de instalación** no es un instalador de confianza. Apps de Play Store/Aurora/
  F-Droid/Obtainium quedan eximidas; las instaladas con **`adb install`** quedan marcadas
  "no verificadas" → bloqueadas. **Por eso otras apps más invasivas funcionan: es la
  fuente de instalación, no el permiso.** GrapheneOS es más estricto y extiende el gate
  también a Usage Access (en stock el foco es Accesibilidad / Notification Listener).

**Fix (dos caminos):**
1. **En el teléfono (como un usuario real):** Ajustes → Apps → (app) → menú ⋮ →
   **"Permitir ajustes restringidos"** → recién ahí se activa el toggle de Acceso de uso.
2. **Atajo dev (adb):**
   `& 'D:\Android-Studio\platform-tools\adb.exe' shell appops set dev.panopt.autonomia GET_USAGE_STATS allow`
   (concede el appop directo, saltea el bloqueo de UI).

Fuentes: bayton.org/android/android-13-restricted-permissions, kaspersky.com/blog/android-restricted-settings/49991,
discuss.grapheneos.org/d/17160-cannot-grant-usage-access-to-any-app.

---

## Follow-up 2 — Trade-off de arquitectura (a DECIDIR, no es bug)

El dueño notó —con razón— que `PACKAGE_USAGE_STATS` es **más ancho de lo que Sueño
necesita** (da historial de uso de apps; Sueño solo usa eventos de pantalla/desbloqueo).
Es una decisión de la capa `device-telemetry` (D2/D6, ya entregada), NO de Sueño:

- **UsageStats (actual):** permiso ancho + fricción de Restricted Settings, PERO sin
  servicio en primer plano permanente; reutilizable para futuros consumidores.
- **BroadcastReceiver de pantalla** (`SCREEN_ON/OFF`, `USER_PRESENT`) en un servicio:
  permiso **inocuo**, PERO exige un servicio vivo con notificación permanente.

Si la fricción del permiso molesta para el producto final → reabrir como **cambio aparte
sobre `device-telemetry`** (no sobre Sueño). Pesá el trade-off antes de tocar nada.

---

## Follow-up 3 — Validación en device pendiente

- `SleepMigration11To12Test` (androidTest) escrito pero NO corrido (necesita device) — tareas 8.3/8.4.
- Flujo e2e de sueño en device (instalación limpia recomendada por decisión #29: la DB es
  descartable). APK: `app/build/outputs/apk/debug/app-debug.apk`. Paquete: `dev.panopt.autonomia`.
- adb está en `D:\Android-Studio\platform-tools\adb.exe` (no en PATH). SDK: `D:\Android-Studio`.

---

## Deuda diferida (del diseño, NO bloquea)

D1 piso de cobertura duro · D2 superávit de sueño · D3 detox en scoring · D4 término de
consistencia · D8 deuda migraciones de `device-telemetry` (tarea 5.2). Detalle en
`docs/sueno/decisiones-diseno-sueno-v1.md` §9.
