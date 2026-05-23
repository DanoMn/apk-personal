package dev.panopt.autonomia.data.local.seed

import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.DisplaySurface
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityDefinitionEntity
import dev.panopt.autonomia.data.ActivityEntity
import dev.panopt.autonomia.data.LayerEntity
import dev.panopt.autonomia.data.UserActivityConfigEntity

/**
 * Canonical activity presets per [docs/presets-actividades-v1.md].
 * PrimaryChecklist = base personal elegida.
 * SecondaryChecklist = mantenimiento diario y soporte ligero.
 */
internal object DefaultSeeds {
    val layers = listOf(
        LayerEntity("layer_interior", "Interior", "Mundo interno: conciencia, aprendizaje, reflexion.", 10),
        LayerEntity("layer_cuerpo", "Cuerpo", "Base fisica: movimiento, descanso, alimentacion e higiene.", 20),
        LayerEntity("layer_conducta", "Conducta", "Autocontrol, limites y estructura diaria.", 30),
        LayerEntity("layer_vinculos", "Vinculos", "Contacto humano y relaciones importantes.", 40),
        LayerEntity("layer_proyecto", "Proyecto", "Futuro, identidad, trabajo y creacion.", 50),
    )

    val activities: List<ActivityEntity> = listOf(
        // All seed activities use DisplaySurface.Available — they appear
        // in "Anclas disponibles" but are NOT pre-assigned to any checklist.
        // The user moves them to PrimaryChecklist manually.

        // Interior
        activity("act_meditar", "layer_interior", "Meditar",
            ActivityType.Time, ActivityRole.Practice,
            DisplaySurface.Available, ContributionRole.Core, ImportanceTier.High,
            targetValue = 5, minimumValue = 1, unit = ActivityUnit.Minutes, sortOrder = 10),
        activity("act_escribir", "layer_interior", "Escribir",
            ActivityType.Note, ActivityRole.Practice,
            DisplaySurface.Available, ContributionRole.Core, ImportanceTier.High,
            unit = ActivityUnit.Text, sortOrder = 11),

        // Cuerpo
        activity("act_ejercicio", "layer_cuerpo", "Ejercicio / gimnasio / caminar",
            ActivityType.Time, ActivityRole.Practice,
            DisplaySurface.Available, ContributionRole.Core, ImportanceTier.High,
            targetValue = 40, unit = ActivityUnit.Minutes, sortOrder = 20),
        activity("act_dormir_temprano", "layer_cuerpo", "Dormir temprano",
            ActivityType.TimeOfDay, ActivityRole.SelfCare,
            DisplaySurface.Available, ContributionRole.Core, ImportanceTier.High,
            unit = ActivityUnit.Time, sortOrder = 21),

        // Conducta
        activity("act_no_celular_cama", "layer_conducta", "No celular antes de dormir",
            ActivityType.Check, ActivityRole.DigitalHygiene,
            DisplaySurface.Available, ContributionRole.Protective, ImportanceTier.High,
            unit = ActivityUnit.Boolean, sortOrder = 30),
        activity("act_no_decidir_madrugada", "layer_conducta", "No decidir de madrugada",
            ActivityType.Check, ActivityRole.Boundary,
            DisplaySurface.Available, ContributionRole.Protective, ImportanceTier.High,
            unit = ActivityUnit.Boolean, sortOrder = 31),

        // Vínculos
        activity("act_no_aislarme", "layer_vinculos", "No aislarme",
            ActivityType.Check, ActivityRole.RelationalHabit,
            DisplaySurface.Available, ContributionRole.Core, ImportanceTier.High,
            unit = ActivityUnit.Boolean, sortOrder = 40),

        // Proyecto
        activity("act_digitaliza", "layer_proyecto", "Avanzar Digitaliza",
            ActivityType.Time, ActivityRole.ProjectWork,
            DisplaySurface.Available, ContributionRole.Core, ImportanceTier.Critical,
            targetValue = 30, unit = ActivityUnit.Minutes, sortOrder = 50),
        activity("act_musica", "layer_proyecto", "Musica / composicion",
            ActivityType.Time, ActivityRole.ProjectWork,
            DisplaySurface.Available, ContributionRole.Core, ImportanceTier.High,
            targetValue = 20, unit = ActivityUnit.Minutes, sortOrder = 51),

        // Interior (secondary)
        activity("act_leer", "layer_interior", "Leer",
            ActivityType.Time, ActivityRole.Learning,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Medium,
            targetValue = 20, unit = ActivityUnit.Minutes, sortOrder = 12),

        // Cuerpo (secondary)
        activity("act_agua", "layer_cuerpo", "Tomar agua",
            ActivityType.Count, ActivityRole.SelfCare,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Medium,
            targetValue = 8, unit = ActivityUnit.Count, sortOrder = 22),
        activity("act_banarse", "layer_cuerpo", "Banarse",
            ActivityType.Check, ActivityRole.SelfCare,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Medium,
            unit = ActivityUnit.Boolean, sortOrder = 23),
        activity("act_dientes", "layer_cuerpo", "Cepillarse los dientes",
            ActivityType.Check, ActivityRole.SelfCare,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Medium,
            unit = ActivityUnit.Boolean, sortOrder = 24),
        activity("act_ropa", "layer_cuerpo", "Cambiarse de ropa",
            ActivityType.Check, ActivityRole.SelfCare,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Low,
            unit = ActivityUnit.Boolean, sortOrder = 25),
        activity("act_comer", "layer_cuerpo", "Comer algo decente",
            ActivityType.Check, ActivityRole.SelfCare,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Medium,
            unit = ActivityUnit.Boolean, sortOrder = 26),

        // Conducta (secondary)
        activity("act_orden_minimo", "layer_conducta", "Orden minimo",
            ActivityType.Time, ActivityRole.DomesticOrder,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Medium,
            targetValue = 15, unit = ActivityUnit.Minutes, sortOrder = 32),

        // Vínculos (secondary)
        activity("act_interaccion", "layer_vinculos", "Una interaccion limpia",
            ActivityType.Note, ActivityRole.RelationalHabit,
            DisplaySurface.Available, ContributionRole.Support, ImportanceTier.Medium,
            unit = ActivityUnit.Text, sortOrder = 41),
    )

    val abstinenceTracks: List<AbstinenceTrackEntity> = emptyList()

    // New seed data for v4 schema
    private val seedTime = System.currentTimeMillis()

    val activityDefinitions: List<ActivityDefinitionEntity> = listOf(
        // -- Anchors (10) --
        activityDef("act_meditar", "layer_interior", "Meditar",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 10),
        activityDef("act_escribir", "layer_interior", "Escribir",
            ActivityType.Note, ActivityRole.Practice, ActivityUnit.Text,
            ContributionRole.Core, ImportanceTier.High, "anchor", 11),
        activityDef("act_ejercicio", "layer_cuerpo", "Ejercicio / gimnasio / caminar",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 20),
        activityDef("act_dormir_temprano", "layer_cuerpo", "Dormir temprano",
            ActivityType.TimeOfDay, ActivityRole.SelfCare, ActivityUnit.Time,
            ContributionRole.Core, ImportanceTier.High, "anchor", 21),
        activityDef("act_no_celular_cama", "layer_conducta", "No celular antes de dormir",
            ActivityType.Check, ActivityRole.DigitalHygiene, ActivityUnit.Boolean,
            ContributionRole.Protective, ImportanceTier.High, "anchor", 30),
        activityDef("act_no_decidir_madrugada", "layer_conducta", "No decidir de madrugada",
            ActivityType.Check, ActivityRole.Boundary, ActivityUnit.Boolean,
            ContributionRole.Protective, ImportanceTier.High, "anchor", 31),
        activityDef("act_no_aislarme", "layer_vinculos", "No aislarme",
            ActivityType.Check, ActivityRole.RelationalHabit, ActivityUnit.Boolean,
            ContributionRole.Core, ImportanceTier.High, "anchor", 40),
        activityDef("act_digitaliza", "layer_proyecto", "Avanzar Digitaliza",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.Critical, "anchor", 50),
        activityDef("act_musica", "layer_proyecto", "Musica / composicion",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 51),
        activityDef("act_leer", "layer_interior", "Leer",
            ActivityType.Time, ActivityRole.Learning, ActivityUnit.Minutes,
            ContributionRole.Support, ImportanceTier.Medium, "anchor", 12),

        // -- Supports (7) --
        activityDef("act_agua", "layer_cuerpo", "Tomar agua",
            ActivityType.Count, ActivityRole.SelfCare, ActivityUnit.Count,
            ContributionRole.Support, ImportanceTier.Medium, "support", 22),
        activityDef("act_banarse", "layer_cuerpo", "Banarse",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 23),
        activityDef("act_dientes", "layer_cuerpo", "Cepillarse los dientes",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 24),
        activityDef("act_ropa", "layer_cuerpo", "Cambiarse de ropa",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Low, "support", 25),
        activityDef("act_comer", "layer_cuerpo", "Comer algo decente",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 26),
        activityDef("act_orden_minimo", "layer_conducta", "Orden minimo",
            ActivityType.Time, ActivityRole.DomesticOrder, ActivityUnit.Minutes,
            ContributionRole.Support, ImportanceTier.Medium, "support", 32),
        activityDef("act_interaccion", "layer_vinculos", "Una interaccion limpia",
            ActivityType.Note, ActivityRole.RelationalHabit, ActivityUnit.Text,
            ContributionRole.Support, ImportanceTier.Medium, "support", 41),
    )

    val userActivityConfigs: List<UserActivityConfigEntity> = emptyList()

    // -- helper --
    private fun activity(
        id: String,
        layerId: String,
        name: String,
        type: ActivityType,
        role: ActivityRole,
        displaySurface: DisplaySurface,
        contributionRole: ContributionRole,
        importanceTier: ImportanceTier,
        targetValue: Int? = null,
        minimumValue: Int? = null,
        targetCount: Int? = null,
        unit: ActivityUnit,
        sortOrder: Int,
    ): ActivityEntity = ActivityEntity(
        id = id,
        layerId = layerId,
        name = name,
        description = "",
        type = type.name,
        role = role.name,
        displaySurface = displaySurface.name,
        contributionRole = contributionRole.name,
        importanceTier = importanceTier.name,
        cadence = null,
        targetValue = targetValue,
        minimumValue = minimumValue,
        targetCount = targetCount,
        targetPeriod = null,
        unit = unit.name,
        active = true,
        archived = false,
        sortOrder = sortOrder,
        createdAt = 0L,
        updatedAt = 0L,
    )
    private fun activityDef(
        id: String,
        layerId: String,
        name: String,
        type: ActivityType,
        role: ActivityRole,
        unit: ActivityUnit,
        contributionRole: ContributionRole,
        importanceTier: ImportanceTier,
        presetCategory: String?,
        sortOrder: Int,
    ): ActivityDefinitionEntity = ActivityDefinitionEntity(
        id = id,
        layerId = layerId,
        name = name,
        description = "",
        type = type.name,
        role = role.name,
        unit = unit.name,
        contributionRole = contributionRole.name,
        importanceTier = importanceTier.name,
        presetCategory = presetCategory,
        sortOrder = sortOrder,
        createdAt = seedTime,
        updatedAt = seedTime,
    )
}
