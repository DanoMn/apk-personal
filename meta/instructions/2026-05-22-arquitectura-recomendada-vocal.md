# Pro-Prompt: Arquitectura recomendada para Vocal

## Contexto humano

El usuario quiere establecer las bases de documentacion de dominio y
arquitectura para evitar codigo espagueti antes de seguir construyendo el
backend local, scoring y features importantes.

El usuario no quiere perder tiempo discutiendo arquitectura de forma abstracta.
Necesita una recomendacion personalizada, intensiva y practica para este
proyecto, considerando que no es una app empresarial enorme, pero si tiene un
dominio propio suficientemente delicado.

## Objetivo

Crear un documento de arquitectura recomendado para `Vocal / Autonomia sin
limites` que:

- audite el codigo y los documentos actuales;
- compare opciones como MVVM simple, Clean Architecture y hexagonal;
- recomiende una arquitectura concreta para este proyecto;
- defina fronteras entre UI, dominio, data y backend local;
- planee una estructura de paquetes esperada;
- proponga una migracion gradual desde el codigo actual;
- evite sobrearquitectura.

## Fuentes obligatorias

- `AGENTS.md`
- `docs/nucleo-dominio-autonomia.md`
- `docs/vocal_mapa_componentes_v_0_2_borrador.md`
- `docs/definicion-tablas-room-v1.md`
- `docs/estado-actual-mvp.md`
- `docs/README.md`
- `app/src/main/java/dev/panopt/autonomia/Models.kt`
- `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt`
- `app/src/main/java/dev/panopt/autonomia/domain/scoring/ScoreEngine.kt`
- `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardInference.kt`
- `app/src/main/java/dev/panopt/autonomia/ui/dashboard/DashboardViewModel.kt`
- Context7 / Android Developers sobre arquitectura Android recomendada.

## Decision tomada

Recomendar:

```text
MVVM Android
+ dominio modular explicito
+ repositorios locales
+ Room como fuente de hechos
+ flujo unidireccional de estado
```

Nombre corto:

```text
Arquitectura local-first con dominio modular.
```

No recomendar Clean Architecture pesada ni hexagonal completa ahora. Tomar sus
principios utiles sin generar ceremonia innecesaria.

## Entregables

1. Crear `docs/arquitectura-recomendada-autonomia.md`.
2. Actualizar `docs/README.md` para registrar el documento como canonico.
3. Ajustar `docs/nucleo-dominio-autonomia.md` para que apunte al documento
   nuevo y no deje "clean architecture ligera" como decision vaga.
4. Registrar decision en `meta/meta-prompting.md`.

## Resultado esperado

El proyecto debe quedar con una decision arquitectonica clara:

```text
Room guarda hechos.
Repositorios exponen flujos y operaciones.
Dominio calcula inferencias.
ViewModel produce estado de pantalla.
Compose presenta estado y envia acciones.
```

La arquitectura debe permitir construir backend local, scoring, onboarding,
abstinencias, sueño, anclas y dashboard sin mezclar responsabilidades.

