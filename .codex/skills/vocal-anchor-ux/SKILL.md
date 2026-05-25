---
name: vocal-anchor-ux
description: Maintain the canonical UX/UI contract for Vocal's Mis anclas, anchor editor, custom anchor creation, quick settings Anclas panel, adaptive bottom sheet behavior, duration commitment dialog, and time wheel. Use when modifying anchor configuration flows, dashboard quick settings, or related Compose components.
---

# Vocal Anchor UX

## Overview

Use this skill before changing any anchor configuration flow in Vocal. It keeps
the Mis anclas UX coherent across the full screen and dashboard quick settings.

Read `docs/mis-anclas-ux-canon-v1.md` when the task changes layout, copy,
fields, validation, or navigation. For a shorter checklist, read
`references/mis-anclas-canon.md`.

## Workflow

1. Identify the surface:
   - Full screen: `AnchorConfigScreen.kt`.
   - Shared add/edit/adjust form: `AnchorEditorForm.kt`.
   - Quick settings: `AnchorConfigPanel.kt` and `DashboardPanels.kt`.
   - Duration/frequency controls: `GoalPresetGrid.kt`.
   - Time wheel: `TimeWheelPicker.kt`.
2. Preserve the canonical field order:
   - identity/name;
   - time target;
   - weekly frequency;
   - commitment duration;
   - actions.
3. Keep custom anchor layer selection fixed above the footer actions, not buried
   inside the scroll.
4. Keep quick settings focused on configured anchors only. Do not reintroduce
   catalog search, filters, checklist, or remove/delete actions there unless the
   user explicitly expands the scope.
5. Use an adaptive bottom sheet: wrap content with a maximum height. Never use a
   forced full-height sheet for quick settings.
6. Put the Indefinido guidance inside the commitment duration dialog.
7. Compile with `:app:compileDebugKotlin --no-daemon`; build the APK when the
   user needs to test the app.

## Guardrails

- Do not add Room or repository fields for UX-only changes when the current
  anchor contract is enough.
- Do not make frequency selection open the duration dialog automatically.
- Do not change minutes back to 5-minute increments.
- Do not let quick settings back navigation close the app from a nested panel.
- Do not use harsh, clinical, gamified, neon, or corporate UI language.
