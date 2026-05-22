package dev.panopt.autonomia.data.local.seed

import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.DisplaySurface
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityEntity
import dev.panopt.autonomia.data.LayerEntity

internal object DefaultSeeds {
    val layers = listOf(
        LayerEntity("layer_interior", "Interior", "Mundo interno: conciencia, aprendizaje, reflexion.", 10),
        LayerEntity("layer_cuerpo", "Cuerpo", "Base fisica: movimiento, descanso, alimentacion e higiene.", 20),
        LayerEntity("layer_conducta", "Conducta", "Autocontrol, limites y estructura diaria.", 30),
        LayerEntity("layer_vinculos", "Vinculos", "Contacto humano y relaciones importantes.", 40),
        LayerEntity("layer_proyecto", "Proyecto", "Futuro, identidad, trabajo y creacion.", 50),
    )

    val activities = listOf(
        ActivityEntity("act_meditar", "layer_interior", "Meditar", "", ActivityType.Time.name, ActivityRole.Practice.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.High.name, null, 5, null, null, null, ActivityUnit.Minutes.name, sortOrder = 10, createdAt = 0L, updatedAt = 0L),
        ActivityEntity("act_ejercicio", "layer_cuerpo", "Ejercicio", "", ActivityType.Time.name, ActivityRole.Practice.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.High.name, null, 40, null, null, null, ActivityUnit.Minutes.name, sortOrder = 20, createdAt = 0L, updatedAt = 0L),
        ActivityEntity("act_digitaliza", "layer_proyecto", "Proyecto Digitaliza", "", ActivityType.Time.name, ActivityRole.ProjectWork.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.Critical.name, null, 360, null, null, null, ActivityUnit.Minutes.name, sortOrder = 30, createdAt = 0L, updatedAt = 0L),
        ActivityEntity("act_musica", "layer_proyecto", "Proyecto musical / Anatomia de la ausencia", "", ActivityType.Time.name, ActivityRole.ProjectWork.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.High.name, null, 180, null, null, null, ActivityUnit.Minutes.name, sortOrder = 40, createdAt = 0L, updatedAt = 0L),
        ActivityEntity("act_dientes", "layer_cuerpo", "Cepillarse los dientes", "", ActivityType.Count.name, ActivityRole.SelfCare.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Medium.name, null, 2, null, null, null, ActivityUnit.Count.name, sortOrder = 50, createdAt = 0L, updatedAt = 0L),
        ActivityEntity("act_banarse", "layer_cuerpo", "Banarse", "", ActivityType.Check.name, ActivityRole.SelfCare.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Medium.name, null, null, null, null, null, ActivityUnit.Boolean.name, sortOrder = 60, createdAt = 0L, updatedAt = 0L),
        ActivityEntity("act_cocinar", "layer_cuerpo", "Cocinar en casa", "", ActivityType.Check.name, ActivityRole.DomesticOrder.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Medium.name, null, null, null, null, null, ActivityUnit.Boolean.name, sortOrder = 70, createdAt = 0L, updatedAt = 0L),
        ActivityEntity("act_trastes", "layer_conducta", "Limpiar los trastes", "", ActivityType.Check.name, ActivityRole.DomesticOrder.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Low.name, null, null, null, null, null, ActivityUnit.Boolean.name, sortOrder = 80, createdAt = 0L, updatedAt = 0L),
    )

    val abstinenceTracks = listOf(
        AbstinenceTrackEntity("trk_alcohol", "Alcohol", "alcohol", AbstinenceSeverity.Critical.name, ContributionRole.Protective.name, ImportanceTier.Critical.name, active = false, sortOrder = 10, createdAt = 0L, updatedAt = 0L),
        AbstinenceTrackEntity("trk_sexual", "Conducta sexual / masturbacion", "conducta sexual", AbstinenceSeverity.Critical.name, ContributionRole.Protective.name, ImportanceTier.High.name, active = false, sortOrder = 20, createdAt = 0L, updatedAt = 0L),
        AbstinenceTrackEntity("trk_marihuana", "Marihuana", "marihuana", AbstinenceSeverity.Moderate.name, ContributionRole.Protective.name, ImportanceTier.Medium.name, active = false, sortOrder = 30, createdAt = 0L, updatedAt = 0L),
    )
}

