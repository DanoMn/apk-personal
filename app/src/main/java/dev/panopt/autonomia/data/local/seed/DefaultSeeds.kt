package dev.panopt.autonomia.data.local.seed

import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityDefinitionEntity
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

    val abstinenceTracks: List<AbstinenceTrackEntity> = emptyList()

    // New seed data for v4 schema
    private val seedTime = System.currentTimeMillis()

    val activityDefinitions: List<ActivityDefinitionEntity> = listOf(
        // === Interior: 5 anchors ===
        activityDef("act_meditar", "layer_interior", "Meditar",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 10),
        activityDef("act_escribir", "layer_interior", "Escribir",
            ActivityType.Note, ActivityRole.Practice, ActivityUnit.Text,
            ContributionRole.Core, ImportanceTier.High, "anchor", 11),
        activityDef("act_leer", "layer_interior", "Leer",
            ActivityType.Time, ActivityRole.Learning, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 12),
        activityDef("act_aprender", "layer_interior", "Aprender",
            ActivityType.Time, ActivityRole.Learning, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 13),
        activityDef("act_estudiar", "layer_interior", "Estudiar",
            ActivityType.Time, ActivityRole.Learning, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 14),

        // === Cuerpo: 7 anchors ===
        activityDef("act_ejercicio", "layer_cuerpo", "Ejercicio",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 20),
        activityDef("act_caminar", "layer_cuerpo", "Caminar",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 21),
        activityDef("act_correr", "layer_cuerpo", "Correr",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 22),
        activityDef("act_gimnasio", "layer_cuerpo", "Gimnasio",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 23),
        activityDef("act_estirar", "layer_cuerpo", "Estirar",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 24),
        activityDef("act_yoga", "layer_cuerpo", "Yoga",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 25),
        activityDef("act_deporte", "layer_cuerpo", "Deporte",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 26),

        // === Conducta: 5 anchors ===
        activityDef("act_no_celular_cama", "layer_conducta", "No celular antes de dormir",
            ActivityType.Check, ActivityRole.DigitalHygiene, ActivityUnit.Boolean,
            ContributionRole.Protective, ImportanceTier.High, "anchor", 30),
        activityDef("act_no_decidir_madrugada", "layer_conducta", "No decidir de madrugada",
            ActivityType.Check, ActivityRole.Boundary, ActivityUnit.Boolean,
            ContributionRole.Protective, ImportanceTier.High, "anchor", 31),
        activityDef("act_educacion_financiera", "layer_conducta", "Educación financiera",
            ActivityType.Time, ActivityRole.Learning, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 32),
        activityDef("act_gestion_financiera", "layer_conducta", "Gestión financiera",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 33),
        activityDef("act_autonomia_alimentaria", "layer_conducta", "Autonomía alimentaria",
            ActivityType.Check, ActivityRole.Practice, ActivityUnit.Boolean,
            ContributionRole.Core, ImportanceTier.High, "anchor", 34),

        // === Vínculos: 7 anchors ===
        activityDef("act_no_aislarme", "layer_vinculos", "No aislarme",
            ActivityType.Check, ActivityRole.RelationalHabit, ActivityUnit.Boolean,
            ContributionRole.Core, ImportanceTier.High, "anchor", 40),
        activityDef("act_cultivar_vinculo", "layer_vinculos", "Cultivar vínculo",
            ActivityType.Note, ActivityRole.RelationalHabit, ActivityUnit.Text,
            ContributionRole.Core, ImportanceTier.High, "anchor", 41),
        activityDef("act_grupo_estudio", "layer_vinculos", "Grupo de estudio",
            ActivityType.Time, ActivityRole.Learning, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 42),
        activityDef("act_entrenamiento_grupal", "layer_vinculos", "Entrenamiento grupal",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 43),
        activityDef("act_voluntariado", "layer_vinculos", "Voluntariado recurrente",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 44),
        activityDef("act_proyecto_compartido", "layer_vinculos", "Proyecto compartido",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 45),
        activityDef("act_mentoria", "layer_vinculos", "Mentoría",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 46),

        // === Proyecto: 9 anchors ===
        activityDef("act_digitaliza", "layer_proyecto", "Avanzar Digitaliza",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.Critical, "anchor", 50),
        activityDef("act_musica", "layer_proyecto", "Música",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 51),
        activityDef("act_trabajar_proyecto", "layer_proyecto", "Trabajar en proyecto",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 52),
        activityDef("act_crear", "layer_proyecto", "Crear",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 53),
        activityDef("act_practicar_habilidad", "layer_proyecto", "Practicar habilidad",
            ActivityType.Time, ActivityRole.Practice, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 54),
        activityDef("act_estudiar_carrera", "layer_proyecto", "Estudiar carrera",
            ActivityType.Time, ActivityRole.Learning, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 55),
        activityDef("act_construir_negocio", "layer_proyecto", "Construir negocio",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 56),
        activityDef("act_desarrollar_producto", "layer_proyecto", "Desarrollar producto",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 57),
        activityDef("act_crear_contenido", "layer_proyecto", "Crear contenido",
            ActivityType.Time, ActivityRole.ProjectWork, ActivityUnit.Minutes,
            ContributionRole.Core, ImportanceTier.High, "anchor", 58),

        // === Soportes: 12 presets canónicos (preset-soportes-v1.md) ===
        // Interior
        activityDef("sup_escribir_diario_personal", "layer_interior", "Escribir diario personal",
            ActivityType.Note, ActivityRole.SelfCare, ActivityUnit.Text,
            ContributionRole.Support, ImportanceTier.Low, "support", 100),
        // Cuerpo
        activityDef("sup_banarse", "layer_cuerpo", "Bañarse",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 101),
        activityDef("sup_cepillarse_dientes", "layer_cuerpo", "Cepillarse los dientes",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 102),
        activityDef("sup_usar_ropa_limpia", "layer_cuerpo", "Usar ropa limpia",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Low, "support", 103),
        activityDef("sup_tomar_agua", "layer_cuerpo", "Tomar agua",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 104),
        activityDef("sup_comer", "layer_cuerpo", "Comer",
            ActivityType.Check, ActivityRole.SelfCare, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 105),
        // Conducta
        activityDef("sup_cocinar_en_casa", "layer_conducta", "Cocinar en casa",
            ActivityType.Check, ActivityRole.DomesticOrder, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 106),
        activityDef("sup_lavar_platos", "layer_conducta", "Lavar los platos",
            ActivityType.Check, ActivityRole.DomesticOrder, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 107),
        activityDef("sup_tender_cama", "layer_conducta", "Tender la cama",
            ActivityType.Check, ActivityRole.DomesticOrder, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Low, "support", 108),
        activityDef("sup_registrar_gastos", "layer_conducta", "Registrar gastos",
            ActivityType.Check, ActivityRole.AdministrativeOrder, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Medium, "support", 109),
        activityDef("sup_revisar_correo", "layer_conducta", "Revisar correo",
            ActivityType.Check, ActivityRole.AdministrativeOrder, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Low, "support", 110),
        // Vínculos
        activityDef("sup_responder_mensajes", "layer_vinculos", "Responder mensajes",
            ActivityType.Check, ActivityRole.RelationalHabit, ActivityUnit.Boolean,
            ContributionRole.Support, ImportanceTier.Low, "support", 111),
    )

    val userActivityConfigs: List<UserActivityConfigEntity> = emptyList()

    // -- helper --
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
