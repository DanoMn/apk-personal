# Frases ancla v1 - catalogo, reglas y esquema

> **Estado: vivo** — se actualiza cuando cambia el codigo que describe.

Fecha: 2026-05-20  
Proyecto: Vocal / Autonomia sin limites  
Objetivo: dejar listo el sistema de frases ancla antes de implementarlo en Room, dominio y dashboard.

---

## 1. Alcance

Este documento define:

- las familias taxonomicas de frases;
- el catalogo inicial de citas;
- las reglas de uso por estado del score;
- las reglas de rotacion por fase del dia;
- el esquema recomendado para Room;
- el contrato de dominio que debe consumir el dashboard.

La frase ancla aparece en el dashboard debajo de:

```text
1. tarjeta de score-state;
2. tarjeta/barra de progreso diario;
3. frase ancla contextual.
```

El dashboard del prototipo solo debe mostrar una referencia visual. La rotacion real pertenece al codigo Android.

---

## 2. Regla central

```text
Room guarda catalogo e impresiones.
El dominio elige la frase.
Compose solo presenta cita y autor.
```

La UI no debe decidir que frase corresponde. Tampoco debe filtrar por estado, familia o fase del dia.

---

## 3. Decisiones cerradas

- Todas las frases visibles deben tener cita y autor/referencia.
- Las frases sin autor o referencia se retiran del catalogo activo.
- La tarjeta de frase debe mostrar siempre la atribucion para darle peso editorial.
- La rotacion base ocurre por fase del dia, no por cada apertura de la app.
- Las fases iniciales son `Amanecer` y `Atardecer`.
- `contemplacion` solo aparece en estados altos.
- `contemplacion` debe ser mas frecuente en `Inquebrantable` que en `Plenitud`.
- El usuario no debe sentir que "perdio" una frase; debe percibir inconscientemente que ciertos estados tienen otra calidad.
- El sistema no debe explicar en UI que hay frases desbloqueables.

---

## 4. Estados del score

Los estados usados por frases son los mismos del laboratorio visual:

| Enum sugerido | Nombre visible | Lectura |
| --- | --- | --- |
| `NoData` | Sin datos | Aun no hay registros suficientes. |
| `Restoration` | Restauracion | Base baja. |
| `Attention` | Atencion | Hay margen. |
| `Motion` | En marcha | Base activa. |
| `Plenitude` | Plenitud | Base sostenida. |
| `Unbreakable` | Inquebrantable | Nucleo solido. |

Regla:

```text
El estado no se elige manualmente.
El dominio calcula el estado y el selector de frases lo usa como entrada.
```

---

## 5. Familias taxonomicas

Las familias describen la funcion de la frase, no su estetica.

| Enum sugerido | Nombre editorial | Funcion |
| --- | --- | --- |
| `Containment` | Contencion | Baja culpa, verguenza o dureza interna. Ayuda a no caer mas. |
| `MinimalAction` | Accion minima | Convierte el estado actual en una accion pequena y concreta. |
| `RegulationClarity` | Claridad / regulacion | Baja ruido mental, pausa impulsos y ordena la atencion. |
| `Persistence` | Persistencia | Ayuda a sostener continuidad cuando aparece friccion. |
| `IdentityValues` | Identidad / valores | Recuerda quien quiere ser el usuario y que esta construyendo. |
| `Recognition` | Reconocimiento | Reconoce avance, dignidad o estabilidad sin euforia barata. |
| `Contemplation` | Contemplacion | Abre percepcion amplia: vida, silencio, unidad, misterio o aceptacion profunda. |

---

## 6. Mapeo por estado

Este mapeo define que familias puede usar cada estado.

| Estado | Familias principales | Familias secundarias | No usar |
| --- | --- | --- | --- |
| Sin datos | `Containment` | `MinimalAction` | `Contemplation` |
| Restauracion | `Containment`, `MinimalAction` | `RegulationClarity` | `Recognition`, `Contemplation` |
| Atencion | `MinimalAction`, `RegulationClarity` | `Containment`, `Persistence` | `Contemplation` |
| En marcha | `Persistence`, `MinimalAction` | `RegulationClarity`, `IdentityValues` | `Contemplation` |
| Plenitud | `Recognition`, `RegulationClarity` | `IdentityValues`, `Contemplation` | `Containment` salvo excepcion |
| Inquebrantable | `Contemplation`, `IdentityValues` | `Recognition` | `Containment`, `MinimalAction` salvo excepcion |

Regla de producto:

```text
Plenitud puede mostrar contemplacion como antesala.
Inquebrantable debe mostrar contemplacion como calidad principal.
```

---

## 7. Fases del dia

La rotacion inicial se basa en dos fases.

| Enum sugerido | Nombre visible | Funcion |
| --- | --- | --- |
| `Dawn` | Amanecer | Abrir el dia, orientar accion, estructura y direccion. |
| `Dusk` | Atardecer | Cerrar el dia, regular, reconocer y volver al centro. |

Ventanas horarias provisionales:

| Fase | Rango local inicial |
| --- | --- |
| Amanecer | 05:00 - 14:59 |
| Atardecer | 15:00 - 04:59 |

Estas ventanas son funcionales, no astronomicas. Si mas adelante se quiere usar hora real de amanecer/atardecer por ubicacion, debe hacerse como mejora separada.

---

## 8. Reglas de rotacion

La frase debe ser estable dentro de una fase.

Entrada minima del selector:

```text
date
dayPhase
scoreState
recentPhraseImpressions
activePhraseCatalog
```

Salida:

```text
AnchorPhraseSelection
- phraseId
- text
- authorReference
- family
- scoreState
- dayPhase
```

Reglas:

1. Al entrar en una nueva fase del dia, el dominio puede elegir una nueva frase.
2. Dentro de la misma fase, no debe cambiar en cada apertura.
3. Si el estado cambia de forma significativa dentro de la misma fase, puede elegirse otra frase compatible con el nuevo estado.
4. No repetir la misma frase en la misma fase durante una ventana reciente.
5. No mostrar frases sin autor/referencia.
6. No mostrar `Contemplation` fuera de `Plenitude` o `Unbreakable`.
7. Si no hay frase elegible por filtros estrictos, relajar primero la ventana de repeticion, no las reglas de estado.

Ventana inicial recomendada:

```text
Evitar repetir la misma frase durante 7 dias.
```

---

## 9. Pesos sugeridos

Los pesos no son puntos emocionales ni score. Solo ayudan a elegir familias.

### Por estado

| Estado | Familia | Peso |
| --- | --- | --- |
| Sin datos | `Containment` | 4 |
| Sin datos | `MinimalAction` | 1 |
| Restauracion | `Containment` | 4 |
| Restauracion | `MinimalAction` | 3 |
| Restauracion | `RegulationClarity` | 1 |
| Atencion | `MinimalAction` | 4 |
| Atencion | `RegulationClarity` | 4 |
| Atencion | `Containment` | 1 |
| Atencion | `Persistence` | 1 |
| En marcha | `Persistence` | 4 |
| En marcha | `MinimalAction` | 3 |
| En marcha | `RegulationClarity` | 2 |
| En marcha | `IdentityValues` | 1 |
| Plenitud | `Recognition` | 4 |
| Plenitud | `RegulationClarity` | 2 |
| Plenitud | `IdentityValues` | 2 |
| Plenitud | `Contemplation` | 1 |
| Inquebrantable | `Contemplation` | 5 |
| Inquebrantable | `IdentityValues` | 3 |
| Inquebrantable | `Recognition` | 2 |

### Por fase

| Fase | Familia favorecida | Peso extra |
| --- | --- | --- |
| Amanecer | `MinimalAction` | +2 |
| Amanecer | `IdentityValues` | +1 |
| Amanecer | `Persistence` | +1 |
| Atardecer | `RegulationClarity` | +2 |
| Atardecer | `Recognition` | +2 |
| Atardecer | `Contemplation` | +1 |
| Atardecer | `Containment` | +1 |

---

## 10. Esquema Room recomendado

### `anchor_phrases`

Catalogo versionado de citas.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | Primary key estable. |
| `text` | `String` | Cita visible. |
| `authorReference` | `String` | Autor, obra o referencia visible. Obligatorio. |
| `family` | `String` | Enum `PhraseFamily`. |
| `language` | `String` | `es`, `en` u otro. |
| `attributionStatus` | `String` | `Clear`, `Traditional`, `Disputed`, `NeedsReview`. |
| `active` | `Boolean` | Si participa en seleccion. |
| `sortOrder` | `Int` | Orden estable dentro de familia. |
| `createdAt` | `Long` | Timestamp local o seed timestamp. |
| `updatedAt` | `Long` | Timestamp local o seed timestamp. |

Regla:

```text
authorReference no puede estar vacio para frases activas.
```

### `anchor_phrase_state_rules`

Define estados permitidos y peso por estado.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `phraseId` | `String` | Parte de primary key. |
| `scoreState` | `String` | Parte de primary key. |
| `weight` | `Int` | Peso relativo para ese estado. |

Primary key:

```text
phraseId + scoreState
```

### `anchor_phrase_phase_rules`

Define fases permitidas y peso por fase.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `phraseId` | `String` | Parte de primary key. |
| `dayPhase` | `String` | `Dawn` o `Dusk`. |
| `weight` | `Int` | Peso relativo para esa fase. |

Primary key:

```text
phraseId + dayPhase
```

### `anchor_phrase_impressions`

Historial de frases mostradas.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `id` | `String` | UUID. |
| `phraseId` | `String` | Frase mostrada. |
| `date` | `String` | Fecha local `YYYY-MM-DD`. |
| `dayPhase` | `String` | Fase en la que se mostro. |
| `scoreState` | `String` | Estado usado al elegir. |
| `shownAt` | `Long` | Timestamp local. |

Indice recomendado:

```text
date + dayPhase
phraseId + shownAt
```

### `anchor_phrase_daily_slots`

Cache opcional para mantener estabilidad dentro de una fase.

| Campo | Tipo | Nota |
| --- | --- | --- |
| `date` | `String` | Parte de primary key. |
| `dayPhase` | `String` | Parte de primary key. |
| `scoreState` | `String` | Estado con el que se resolvio. |
| `phraseId` | `String` | Frase elegida. |
| `resolvedAt` | `Long` | Timestamp local. |

Primary key:

```text
date + dayPhase
```

Esta tabla puede omitirse si el selector es deterministico y usa `anchor_phrase_impressions`, pero conviene para que el dashboard no cambie por recomposiciones.

---

## 11. Enums Kotlin sugeridos

```kotlin
enum class PhraseFamily {
    Containment,
    MinimalAction,
    RegulationClarity,
    Persistence,
    IdentityValues,
    Recognition,
    Contemplation
}

enum class DayPhase {
    Dawn,
    Dusk
}

enum class PhraseAttributionStatus {
    Clear,
    Traditional,
    Disputed,
    NeedsReview
}
```

`ScoreState` debe reutilizar el enum del dominio del score, no duplicarse dentro del modulo de frases.

---

## 12. Contrato de dominio

Servicio sugerido:

```kotlin
interface AnchorPhraseSelector {
    suspend fun selectPhrase(
        date: LocalDate,
        now: Instant,
        scoreState: ScoreState
    ): AnchorPhraseSelection
}
```

Resultado sugerido:

```kotlin
data class AnchorPhraseSelection(
    val phraseId: String,
    val text: String,
    val authorReference: String,
    val family: PhraseFamily,
    val scoreState: ScoreState,
    val dayPhase: DayPhase
)
```

Regla de arquitectura:

```text
El ViewModel pide una seleccion.
El selector consulta catalogo e historial.
El selector guarda impresion o slot si corresponde.
Compose recibe texto final y autor.
```

---

## 13. Catalogo activo

Notas:

- Las frases estan separadas por familia.
- `authorReference` es obligatorio.
- Las atribuciones marcadas como `Disputed` o `NeedsReview` pueden usarse si la referencia visible es honesta.
- Las frases sin autor/referencia quedan fuera del catalogo activo.

### Contencion

| ID | Frase | Autor / referencia | Estado atribucion |
| --- | --- | --- | --- |
| `phrase_containment_001` | "This is a moment of suffering. Suffering is part of life. May I be kind to myself." | Kristin Neff | Clear |
| `phrase_containment_002` | "If your compassion does not include yourself, it is incomplete." | Jack Kornfield | Clear |
| `phrase_containment_003` | "Compassion is not a relationship between the healer and the wounded. It's a relationship between equals." | Pema Chodron | Clear |
| `phrase_containment_004` | "In some ways suffering ceases to be suffering at the moment it finds a meaning." | Viktor Frankl | Clear |
| `phrase_containment_005` | "There can be no lotus flower without the mud." | Thich Nhat Hanh | Clear |
| `phrase_containment_006` | "Hoy es siempre todavia." | Antonio Machado | Clear |
| `phrase_containment_007` | "Attention is the rarest and purest form of generosity." | Simone Weil | Clear |
| `phrase_containment_008` | "In the depths of winter, I finally learned that within me there lay an invincible summer." | Albert Camus | Clear |
| `phrase_containment_009` | "Quien ha visto la Esperanza, no la olvida." | Octavio Paz | NeedsReview |
| `phrase_containment_010` | "Only to the extent that we expose ourselves can that which is indestructible be found in us." | Pema Chodron | Clear |
| `phrase_containment_011` | "None of us is okay and all of us are fine. We are walking, talking paradoxes." | Pema Chodron | Clear |
| `phrase_containment_012` | "Un viaje de mil millas comienza con un solo paso." | Lao Tse | Traditional |

### Accion minima

| ID | Frase | Autor / referencia | Estado atribucion |
| --- | --- | --- | --- |
| `phrase_minimal_action_001` | "For the things we have to learn before we can do them, we learn by doing them." | Aristoteles | Clear |
| `phrase_minimal_action_002` | "Great things are not done by impulse, but by a series of small things brought together." | Vincent van Gogh | Clear |
| `phrase_minimal_action_003` | "Action is the antidote to despair." | Joan Baez | Clear |
| `phrase_minimal_action_004` | "Caminante, no hay camino, se hace camino al andar." | Antonio Machado | Clear |
| `phrase_minimal_action_005` | "The impediment to action advances action. What stands in the way becomes the way." | Marco Aurelio | Clear |
| `phrase_minimal_action_006` | "You never see further than your headlights, but you can make the whole trip that way." | E. L. Doctorow | Clear |
| `phrase_minimal_action_007` | "Knowing is not enough; we must apply. Willing is not enough; we must do." | Johann Wolfgang von Goethe | Traditional |
| `phrase_minimal_action_008` | "What saves a man is to take a step. Then another step." | Antoine de Saint-Exupery | Clear |
| `phrase_minimal_action_009` | "In order to do something well we must first be willing to do it badly." | Julia Cameron | Clear |
| `phrase_minimal_action_010` | "A small daily task, if it be really daily, will beat the labours of a spasmodic Hercules." | Anthony Trollope | Clear |
| `phrase_minimal_action_011` | "Well done is better than well said." | Benjamin Franklin | Traditional |
| `phrase_minimal_action_012` | "Life can only be understood backwards; but it must be lived forwards." | Soren Kierkegaard | Clear |
| `phrase_minimal_action_013` | "Inspiration exists, but it has to find you working." | Pablo Picasso | Traditional |
| `phrase_minimal_action_014` | "Nothing will work unless you do." | Maya Angelou | Clear |

### Claridad / regulacion

| ID | Frase | Autor / referencia | Estado atribucion |
| --- | --- | --- | --- |
| `phrase_regulation_clarity_001` | "Sufrimos mas a menudo en la imaginacion que en la realidad." | Seneca | Traditional |
| `phrase_regulation_clarity_002` | "There is nothing either good or bad but thinking makes it so." | William Shakespeare, Hamlet | Clear |
| `phrase_regulation_clarity_003` | "Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor." | Thich Nhat Hanh | Clear |
| `phrase_regulation_clarity_004` | "Name it to tame it." | Daniel J. Siegel | Clear |
| `phrase_regulation_clarity_005` | "The curious paradox is that when I accept myself just as I am, then I can change." | Carl Rogers | Clear |
| `phrase_regulation_clarity_006` | "The first principle is that you must not fool yourself, and you are the easiest person to fool." | Richard Feynman | Clear |
| `phrase_regulation_clarity_007` | "This is called the sacred pause, a moment where we stop and release our identification with problems and reactions." | Jack Kornfield | Clear |
| `phrase_regulation_clarity_008` | "Tienes paciencia para esperar hasta que el lodo se asiente y el agua este clara?" | Lao Tse | Traditional |
| `phrase_regulation_clarity_009` | "No era solo banarme lo que queria, sino mantener mi mente en buen orden." | Epicteto | NeedsReview |
| `phrase_regulation_clarity_010` | "Retirate en ti mismo." | Marco Aurelio | Traditional |
| `phrase_regulation_clarity_011` | "Breathing in, I am aware of my feeling. Breathing out, I calm my feeling." | Thich Nhat Hanh / Plum Village | Clear |
| `phrase_regulation_clarity_012` | "Please calm down, my friend. Lay down your sharp sword of conceptual thinking." | Thich Nhat Hanh | Clear |
| `phrase_regulation_clarity_013` | "As soon as the sun of awareness shines, at that very moment a great change takes place." | Thich Nhat Hanh | Clear |
| `phrase_regulation_clarity_014` | "Not everything that is faced can be changed, but nothing can be changed until it is faced." | James Baldwin, No Name in the Street | Clear |

### Persistencia

| ID | Frase | Autor / referencia | Estado atribucion |
| --- | --- | --- | --- |
| `phrase_persistence_001` | "Todo lo que sucede es soportable o no. Si es soportable, soportalo." | Marco Aurelio, Meditaciones | Clear |
| `phrase_persistence_002` | "Ever tried. Ever failed. No matter. Try again. Fail again. Fail better." | Samuel Beckett, Worstward Ho | Clear |
| `phrase_persistence_003` | "You may encounter many defeats, but you must not be defeated." | Maya Angelou | Clear |
| `phrase_persistence_004` | "The world breaks everyone and afterward many are strong at the broken places." | Ernest Hemingway, A Farewell to Arms | Clear |
| `phrase_persistence_005` | "Quien tiene un porque para vivir puede soportar casi cualquier como." | Friedrich Nietzsche, El crepusculo de los idolos | Clear |
| `phrase_persistence_006` | "Everything is gestation and then bringing forth." | Rainer Maria Rilke, Letters to a Young Poet | Clear |
| `phrase_persistence_007` | "La lucha misma hacia las cumbres basta para llenar un corazon humano." | Albert Camus, El mito de Sisifo | Clear |
| `phrase_persistence_008` | "Things take the time they take. Don't worry." | Mary Oliver, "Don't Worry" | Clear |
| `phrase_persistence_009` | "It is good to have an end to journey toward; but it is the journey that matters, in the end." | Ursula K. Le Guin, The Left Hand of Darkness | Clear |
| `phrase_persistence_010` | "I learned this, at least, by my experiment: that if one advances confidently in the direction of his dreams..." | Henry David Thoreau, Walden | Clear |

### Identidad / valores

| ID | Frase | Autor / referencia | Estado atribucion |
| --- | --- | --- | --- |
| `phrase_identity_values_001` | "This above all: to thine own self be true." | William Shakespeare, Hamlet | Clear |
| `phrase_identity_values_002` | "No legacy is so rich as honesty." | William Shakespeare, All's Well That Ends Well | Clear |
| `phrase_identity_values_003` | "I wished to live deliberately, to front only the essential facts of life." | Henry David Thoreau, Walden | Clear |
| `phrase_identity_values_004` | "The unexamined life is not worth living." | Socrates / Platon, Apology | Traditional |
| `phrase_identity_values_005` | "Act only according to that maxim whereby you can at the same time will that it should become a universal law." | Immanuel Kant | Clear |
| `phrase_identity_values_006` | "If I didn't define myself for myself, I would be crunched into other people's fantasies for me and eaten alive." | Audre Lorde | Clear |
| `phrase_identity_values_007` | "Freeing yourself was one thing, claiming ownership of that freed self was another." | Toni Morrison, Beloved | Clear |
| `phrase_identity_values_008` | "Definitions belong to the definers, not the defined." | Toni Morrison, Beloved | Clear |
| `phrase_identity_values_009` | "Do I contradict myself? Very well then I contradict myself, I am large, I contain multitudes." | Walt Whitman, Song of Myself | Clear |
| `phrase_identity_values_010` | "Your silence will not protect you." | Audre Lorde | Clear |

### Reconocimiento

| ID | Frase | Autor / referencia | Estado atribucion |
| --- | --- | --- | --- |
| `phrase_recognition_001` | "I am larger, better than I thought, I did not know I held so much goodness." | Walt Whitman, Song of the Open Road | Clear |
| `phrase_recognition_002` | "I celebrate myself, and sing myself." | Walt Whitman, Song of Myself | Clear |
| `phrase_recognition_003` | "Give back your heart to itself." | Derek Walcott, Love After Love | Clear |
| `phrase_recognition_004` | "And now that you don't have to be perfect, you can be good." | John Steinbeck, East of Eden | Clear |
| `phrase_recognition_005` | "Each of us is more than the worst thing we've ever done." | Bryan Stevenson, Just Mercy / EJI | Clear |
| `phrase_recognition_006` | "Instructions for living a life: Pay attention. Be astonished. Tell about it." | Mary Oliver, "Sometimes" | Clear |
| `phrase_recognition_007` | "You do not have to be good." | Mary Oliver, "Wild Geese" | Clear |
| `phrase_recognition_008` | "Before you know kindness as the deepest thing inside, you must know sorrow as the other deepest thing." | Naomi Shihab Nye, "Kindness" | Clear |
| `phrase_recognition_009` | "It is a serious thing just to be alive on this fresh morning in this broken world." | Mary Oliver, "Invitation" | Clear |
| `phrase_recognition_010` | "We can only be said to be alive in those moments when our hearts are conscious of our treasure." | Thornton Wilder, The Woman of Andros | Clear |

### Contemplacion

| ID | Frase | Autor / referencia | Estado atribucion |
| --- | --- | --- | --- |
| `phrase_contemplation_001` | "Out beyond ideas of wrongdoing and rightdoing, there is a field. I'll meet you there." | Rumi / version de Coleman Barks | Disputed |
| `phrase_contemplation_002` | "The quieter you become, the more you are able to hear." | Lema asociado a Kali Linux / atribucion popular discutida | Disputed |
| `phrase_contemplation_003` | "Vende tu astucia y compra asombro." | Rumi, Masnavi / traduccion libre | Traditional |
| `phrase_contemplation_004` | "El ojo con que veo a Dios es el mismo ojo con que Dios me ve." | Meister Eckhart | Traditional |
| `phrase_contemplation_005` | "Para venir a lo que no sabes, has de ir por donde no sabes." | San Juan de la Cruz, Monte de Perfeccion | Clear |
| `phrase_contemplation_006` | "El Tao que puede ser nombrado no es el Tao eterno." | Lao Tse, Tao Te Ching | Traditional |
| `phrase_contemplation_007` | "Estudiar el camino de Buda es estudiarse a uno mismo; estudiarse a uno mismo es olvidarse de uno mismo." | Dogen, Genjokoan | Clear |
| `phrase_contemplation_008` | "Por amor puede ser alcanzado y sostenido; por pensamiento, nunca." | Anonimo, The Cloud of Unknowing | Traditional |
| `phrase_contemplation_009` | "La rosa no tiene porque; florece porque florece." | Angelus Silesius, Cherubinischer Wandersmann | Clear |
| `phrase_contemplation_010` | "El Reino esta dentro de ti y fuera de ti." | Evangelio de Tomas | Traditional |
| `phrase_contemplation_011` | "La vida es el vuelo del solo hacia el Solo." | Plotino | Traditional |
| `phrase_contemplation_012` | "He vivido al borde de la locura, queriendo saber razones, tocando una puerta. Se abre. He estado tocando desde dentro." | Rumi | Traditional |
| `phrase_contemplation_013` | "Que es la vida? Un frenesi. Que es la vida? Una ilusion, una sombra, una ficcion; y el mayor bien es pequeno, que toda la vida es sueno, y los suenos, suenos son." | Pedro Calderon de la Barca, La vida es sueno | Clear |

---

## 14. Frases retiradas

| Frase | Motivo |
| --- | --- |
| "No confies en la motivacion cuando estas agotado; apoyate en tus sistemas. Que tu rutina basica sea tu red de seguridad." | Frase personal sin autor/referencia externa. Rompe la logica editorial de la tarjeta. |

Una frase retirada puede volver mas adelante si se convierte en mensaje propio de la app, pero no como cita ancla del catalogo inicial.

---

## 15. Seed inicial

Cantidad activa por familia:

| Familia | Cantidad activa |
| --- | ---: |
| Contencion | 12 |
| Accion minima | 14 |
| Claridad / regulacion | 14 |
| Persistencia | 10 |
| Identidad / valores | 10 |
| Reconocimiento | 10 |
| Contemplacion | 13 |
| **Total activo** | **83** |

La implementacion debe generar:

```text
83 anchor_phrases
reglas de estado por familia
reglas de fase por familia
0 frases activas sin authorReference
```

---

## 16. Uso en dashboard

La tarjeta debe mostrar:

```text
"Cita visible."
Autor / referencia
```

No debe mostrar:

```text
familia
estado interno
peso
explicacion de desbloqueo
```

La frase debe sentirse como una presencia editorial, no como un consejo generado al azar.

---

## 17. No implementar todavia

- Editor de frases propias.
- Personalizacion manual de familias.
- Explicacion visible de desbloqueos.
- Rotacion por cada apertura de app.
- Dependencia de geolocalizacion para amanecer/atardecer real.
- Algoritmo complejo por capa mas baja, riesgo o sobriedad.

---

## 18. Pendientes inmediatos para implementacion

1. Crear enums `PhraseFamily`, `DayPhase` y `PhraseAttributionStatus`.
2. Crear entidades Room `AnchorPhrase`, `AnchorPhraseStateRule`, `AnchorPhrasePhaseRule`, `AnchorPhraseImpression` y opcionalmente `AnchorPhraseDailySlot`.
3. Crear seed del catalogo activo.
4. Crear selector de frase por `date + dayPhase + scoreState`.
5. Integrar selector al ViewModel del dashboard.
6. Actualizar `dashboard.html` para mostrar visualmente score-state, progreso diario y frase ancla en el orden final.
7. Mantener `contemplacion` como recompensa sutil de estados altos.
