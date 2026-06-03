# Handoff — Onboarding de introducción COMPLETO (5/5 slices). Falta archive + pendientes sueltos

> Para arrancar en **sesión nueva** sin contaminar contexto. Cargar este doc +
> recuperar memoria de Engram (proyecto `apk-personal`) por:
> "onboarding", "sdd/onboarding-introduccion/apply-progress", "onboarding-introduccion completo".
>
> Fecha: 2026-06-02 · Proyecto: apk-personal (app "Autonomía sin límites") · Rama base: `main`
> Antecesor: `meta/handoffs/handoff-onboarding-slices-1-2.md` (este lo cierra).

---

## TL;DR — dónde estamos

El change **`onboarding-introduccion` está COMPLETO y ARCHIVADO: los 5 slices
implementados, verificados en emulador, commiteados y el ciclo SDD cerrado.** Todo
pusheado a `origin/main`.

**`sdd-archive` HECHO** (commit `6a1f85f`): las 6 capabilities sincronizadas a
`openspec/specs/onboarding-*` (gate incluye el delta de slice 4 mergeado), y el change
movido a `openspec/changes/archive/2026-06-02-onboarding-introduccion/`. Ya NO queda nada
del ciclo SDD de este change.

> Nota de reparación: el sub-agente de archive condensó el `design.md` (313 vs 1300
> líneas) y no pudo borrar el directorio activo (sin `rm`). Se reparó a mano: archive
> re-copiado FIEL desde HEAD (design 1300 líneas) y directorio activo borrado.

```
BLOQUE 0    Bienvenida ........................... slice 1 ✅
BLOQUE 0.5  Intención (2 rutas) .................. slice 4 ✅
BLOQUE 1    Anclas (3 en 3 capas) ................ slice 2 ✅
BLOQUE 2    Sueño (ventana + telemetría) ......... slice 3 ✅
BLOQUE 3    Sobriedad (solo ruta protección) ..... slice 4 ✅
BLOQUE 4    Cierre + flag completado ............. slice 1 ✅
            Notificaciones (2 canales + permiso) .. slice 5 ✅
```

---

## 1. Lo PRIMERO al retomar: los pendientes sueltos (ver §4)

El change ya está archivado (ciclo SDD cerrado). Lo que queda NO es del change: son las
tareas sueltas del §4 (retirar "Vocal", `Definicion_anclas.md`, fix del `TimeField`,
revisar docs vivos). Empezar por la más barata.

Specs principales (fuente de verdad implementada): `openspec/specs/onboarding-gate`,
`onboarding-anchors`, `onboarding-sleep`, `onboarding-intention`, `onboarding-sobriety`,
`onboarding-notifications`. Artefactos del change en
`openspec/changes/archive/2026-06-02-onboarding-introduccion/`.

---

## 2. Lo que se implementó por slice (todo verificado en emulador + commiteado)

**Slice 1 (gate + estado + esqueleto)** y **Slice 2 (Anclas)** — verificados esta sesión
en emulador (estaban static-green del handoff anterior). Gate abre en Bloque 0, reanuda
tras kill, completa a Dashboard, no repite. Anclas: 3 capas distintas gatean "Continuar".

**Slice 3 (Sueño)** — `OnboardingSleepRule` (envuelve `SleepPolicy`, gate ventana ≥5h),
3 prefs sin Room (`sleep_usage_stats_requested/skipped`, `sleep_wind_down_consent`),
`OnboardingSleepStep` (Context7: ON_RESUME lifecycle re-check; UsageStats = special
access, no runtime permission). Helpers extraídos: `SleepAutoModeCard.kt`,
`SleepWindowFields.kt`. Verificado: ventana derivada 8h, gate <5h bidireccional, permiso
salteable, wind-down consent.

**Slice 4 (Intención + Sobriedad)** — intención como campo de `OnboardingState` (default
`STANDARD`), `OnboardingFlow.next/previous/resolve` ramifican con `when(step,intention)`
(ruta estándar SALTA Sobriedad), pref `onboarding_intention`, `OnboardingIntentionRule`,
`OnboardingIntentionStep` + `OnboardingSobrietyStep`, track vía `createCustomAbstinenceTrack`
(sin Room nuevo). Migración de `OnboardingFlowTest` al nuevo contrato. Verificado: ambas
rutas (estándar salta / protección muestra Sobriedad), track "Tabaco" persiste e integra al
sistema de Sobriedad.

**Slice 5 (Notificaciones)** — `SleepNotificationPolicy` (dominio puro,
`NIGHTS_WITHOUT_DATA_THRESHOLD=3` calibrable) + `WindDownSchedulePolicy`, 2 canales
(`sleep_wind_down`, `sleep_data_alert`), `SleepNotifier` (NotificationCompat, self-guard
permiso), `WindDownNotificationScheduler`+`Worker` (notif A anclada a `targetSleepAt`), hook
en `DailyClosureWorker` (notif B vía `repository.maybeFireSleepDataAlert`, contador NoData
DERIVADO de `sleep_nights` = cero Room nuevo), permiso `POST_NOTIFICATIONS` perezoso desde
`MainActivity` (API<33 concedido). Verificado: permiso se pide al entrar al Dashboard, 2
canales registrados, workers programados, sin crash. **Limitación**: la aparición visual de
una notif NO se pudo forzar (depende de tiempo real 23:30 / 3 noches NoData); la lógica está
cubierta por 19 tests de dominio.

**Capas (todos los slices):** `assembleDebug` + `testDebugUnitTest` + `lintDebug` +
runtime emulador, todas verdes. Strict TDD en cada apply. Context7 inyectado por el
orquestador en cada `sdd-apply` (los sub-agentes SDD NO tienen la tool — ver memoria
`workflow/context7-en-sdd-apply`).

---

## 3. Decisiones de producto cerradas esta sesión (memoria engram)

- **Intención se PERSISTE** (`onboarding_intention`) pero en el slice 4 SOLO ramifica.
  "Teñir tono/ofertas según intención" = trabajo FUTURO fuera de alcance.
  (`onboarding/intencion-persistencia`)
- **Track de sobriedad huérfano** (usuario crea track y luego cambia a ruta estándar):
  SE MANTIENE, no se borra (regla no destruir datos del usuario).
- **`sleep-consumer` está ARCHIVADO/implementado** — `SleepInterpreter` es real; la notif B
  consulta `NoData` real (no es dependencia pendiente; un sub-agente se equivocó).

---

## 4. Pendientes (NO del change — backlog suelto)

- **`sdd-archive`** del change (ver §1) — lo más inmediato.
- **Retirar "Vocal" → "Autonomía sin límites"** del repo (~190 ocurrencias en 61 `.md`,
  1 en `.kt`). Landmines: NO tocar `docs/old/`, `meta/handoffs/`, `docs/auditorias/`
  (congelados); renombrar archivos rompe `@references`; clave engram sigue `apk-personal`.
- **Documentar `docs/producto/Definicion_anclas.md`** (filename con mayúscula/guiones bajos,
  sin header `> Estado: vivo`).
- **Deuda menor — `TimeField` (slice 3):** no auto-inserta el `:`. Si el usuario borra todo y
  teclea solo dígitos ("0730"), el campo queda no-parseable y el mensaje "La ventana mínima es
  de 5 horas" es ENGAÑOSO (la causa real es formato inválido, no duración <5h). Revisar que
  `filterTimeInput` auto-formatee o que el mensaje distinga formato-inválido de <5h.
- **Docs vivos:** revisar si `docs/sueno/` o `docs/dominio/` necesitan reflejar el onboarding
  (notificaciones de sueño, ventana objetivo elegida activamente).

---

## 5. Cómo retomar (orden sugerido)

1. Cargar este handoff + `mem_search` en engram por "onboarding-introduccion completo" /
   "apply-progress".
2. Lanzar **`sdd-archive`** del change `onboarding-introduccion`.
3. Atacar los pendientes sueltos del §4 a ritmo del dueño (empezar por el más barato:
   `Definicion_anclas.md` o el fix del `TimeField`).
