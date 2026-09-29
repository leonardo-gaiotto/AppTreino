package com.leonardo.apptreino.data

import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.MuscleGroup

/**
 * Catálogo mockado de exercícios. Todos os vídeos são do YouTube, em português,
 * e foram verificados (existem e têm miniatura em alta resolução).
 */
object ExerciseCatalog {

    val all: List<CatalogExercise> = listOf(
        // Peito
        CatalogExercise(
            id = "bench_press", nameRes = R.string.ex_bench_press_name, group = MuscleGroup.CHEST,
            targetRes = R.string.muscle_chest, stepsRes = R.array.ex_bench_press_steps,
            commonMistakesRes = R.array.ex_bench_press_mistakes, youtubeVideoId = "pCPyqW60Wuk",
        ),
        CatalogExercise(
            id = "incline_db_press", nameRes = R.string.ex_incline_db_press_name, group = MuscleGroup.CHEST,
            targetRes = R.string.muscle_upper_chest, stepsRes = R.array.ex_incline_db_press_steps,
            commonMistakesRes = R.array.ex_incline_db_press_mistakes, youtubeVideoId = "rCPwrZkrVVQ",
        ),
        CatalogExercise(
            id = "pec_deck", nameRes = R.string.ex_pec_deck_name, group = MuscleGroup.CHEST,
            targetRes = R.string.muscle_chest, stepsRes = R.array.ex_pec_deck_steps,
            commonMistakesRes = R.array.ex_pec_deck_mistakes, youtubeVideoId = "FzCnfD0gOXo",
        ),
        // Ombros
        CatalogExercise(
            id = "db_shoulder_press", nameRes = R.string.ex_db_shoulder_press_name, group = MuscleGroup.SHOULDERS,
            targetRes = R.string.muscle_shoulders, stepsRes = R.array.ex_db_shoulder_press_steps,
            commonMistakesRes = R.array.ex_db_shoulder_press_mistakes, youtubeVideoId = "0uB7PH3oFVo",
        ),
        CatalogExercise(
            id = "lateral_raise", nameRes = R.string.ex_lateral_raise_name, group = MuscleGroup.SHOULDERS,
            targetRes = R.string.muscle_side_delts, stepsRes = R.array.ex_lateral_raise_steps,
            commonMistakesRes = R.array.ex_lateral_raise_mistakes, youtubeVideoId = "IwWvZ0rlNXs",
        ),
        CatalogExercise(
            id = "face_pull", nameRes = R.string.ex_face_pull_name, group = MuscleGroup.SHOULDERS,
            targetRes = R.string.muscle_rear_delts, stepsRes = R.array.ex_face_pull_steps,
            commonMistakesRes = R.array.ex_face_pull_mistakes, youtubeVideoId = "jvwPfCguVQM",
        ),
        // Tríceps
        CatalogExercise(
            id = "rope_pushdown", nameRes = R.string.ex_rope_pushdown_name, group = MuscleGroup.TRICEPS,
            targetRes = R.string.muscle_triceps, stepsRes = R.array.ex_rope_pushdown_steps,
            commonMistakesRes = R.array.ex_rope_pushdown_mistakes, youtubeVideoId = "KhK5HWJfsrQ",
        ),
        CatalogExercise(
            id = "overhead_extension", nameRes = R.string.ex_overhead_extension_name, group = MuscleGroup.TRICEPS,
            targetRes = R.string.muscle_triceps, stepsRes = R.array.ex_overhead_extension_steps,
            commonMistakesRes = R.array.ex_overhead_extension_mistakes, youtubeVideoId = "rLxPTB2J7vo",
        ),
        CatalogExercise(
            id = "dips", nameRes = R.string.ex_dips_name, group = MuscleGroup.TRICEPS,
            targetRes = R.string.muscle_triceps_chest, stepsRes = R.array.ex_dips_steps,
            commonMistakesRes = R.array.ex_dips_mistakes, youtubeVideoId = "pV-RLZcNCGs",
            isBodyWeight = true,
        ),
        // Costas
        CatalogExercise(
            id = "lat_pulldown", nameRes = R.string.ex_lat_pulldown_name, group = MuscleGroup.BACK,
            targetRes = R.string.muscle_lats, stepsRes = R.array.ex_lat_pulldown_steps,
            commonMistakesRes = R.array.ex_lat_pulldown_mistakes, youtubeVideoId = "FepRH_MBX8E",
        ),
        CatalogExercise(
            id = "barbell_row", nameRes = R.string.ex_barbell_row_name, group = MuscleGroup.BACK,
            targetRes = R.string.muscle_back, stepsRes = R.array.ex_barbell_row_steps,
            commonMistakesRes = R.array.ex_barbell_row_mistakes, youtubeVideoId = "TfxJMertfsw",
        ),
        CatalogExercise(
            id = "seated_row", nameRes = R.string.ex_seated_row_name, group = MuscleGroup.BACK,
            targetRes = R.string.muscle_mid_back, stepsRes = R.array.ex_seated_row_steps,
            commonMistakesRes = R.array.ex_seated_row_mistakes, youtubeVideoId = "f8AVh4VBbos",
        ),
        CatalogExercise(
            id = "one_arm_row", nameRes = R.string.ex_one_arm_row_name, group = MuscleGroup.BACK,
            targetRes = R.string.muscle_lats, stepsRes = R.array.ex_one_arm_row_steps,
            commonMistakesRes = R.array.ex_one_arm_row_mistakes, youtubeVideoId = "m4h4jT9patY",
        ),
        CatalogExercise(
            id = "pull_up", nameRes = R.string.ex_pull_up_name, group = MuscleGroup.BACK,
            targetRes = R.string.muscle_lats, stepsRes = R.array.ex_pull_up_steps,
            commonMistakesRes = R.array.ex_pull_up_mistakes, youtubeVideoId = "thg6cGXSlvY",
            isBodyWeight = true,
        ),
        // Bíceps
        CatalogExercise(
            id = "barbell_curl", nameRes = R.string.ex_barbell_curl_name, group = MuscleGroup.BICEPS,
            targetRes = R.string.muscle_biceps, stepsRes = R.array.ex_barbell_curl_steps,
            commonMistakesRes = R.array.ex_barbell_curl_mistakes, youtubeVideoId = "hemxXmD6l4w",
        ),
        CatalogExercise(
            id = "hammer_curl", nameRes = R.string.ex_hammer_curl_name, group = MuscleGroup.BICEPS,
            targetRes = R.string.muscle_biceps_forearm, stepsRes = R.array.ex_hammer_curl_steps,
            commonMistakesRes = R.array.ex_hammer_curl_mistakes, youtubeVideoId = "OnNmaG_tZM4",
        ),
        CatalogExercise(
            id = "preacher_curl", nameRes = R.string.ex_preacher_curl_name, group = MuscleGroup.BICEPS,
            targetRes = R.string.muscle_biceps, stepsRes = R.array.ex_preacher_curl_steps,
            commonMistakesRes = R.array.ex_preacher_curl_mistakes, youtubeVideoId = "zpTK6eihdSA",
        ),
        // Pernas
        CatalogExercise(
            id = "back_squat", nameRes = R.string.ex_back_squat_name, group = MuscleGroup.LEGS,
            targetRes = R.string.muscle_quads_glutes, stepsRes = R.array.ex_back_squat_steps,
            commonMistakesRes = R.array.ex_back_squat_mistakes, youtubeVideoId = "qrZs_1K1CFI",
        ),
        CatalogExercise(
            id = "leg_press", nameRes = R.string.ex_leg_press_name, group = MuscleGroup.LEGS,
            targetRes = R.string.muscle_quads, stepsRes = R.array.ex_leg_press_steps,
            commonMistakesRes = R.array.ex_leg_press_mistakes, youtubeVideoId = "waAxlYvtCcI",
        ),
        CatalogExercise(
            id = "stiff", nameRes = R.string.ex_stiff_name, group = MuscleGroup.LEGS,
            targetRes = R.string.muscle_hamstrings_glutes, stepsRes = R.array.ex_stiff_steps,
            commonMistakesRes = R.array.ex_stiff_mistakes, youtubeVideoId = "F8q6ZIizAcY",
        ),
        CatalogExercise(
            id = "leg_extension", nameRes = R.string.ex_leg_extension_name, group = MuscleGroup.LEGS,
            targetRes = R.string.muscle_quads, stepsRes = R.array.ex_leg_extension_steps,
            commonMistakesRes = R.array.ex_leg_extension_mistakes, youtubeVideoId = "y6juG3XuRe4",
        ),
        CatalogExercise(
            id = "leg_curl", nameRes = R.string.ex_leg_curl_name, group = MuscleGroup.LEGS,
            targetRes = R.string.muscle_hamstrings, stepsRes = R.array.ex_leg_curl_steps,
            commonMistakesRes = R.array.ex_leg_curl_mistakes, youtubeVideoId = "dMYsB4Eb2BY",
        ),
        CatalogExercise(
            id = "lunge", nameRes = R.string.ex_lunge_name, group = MuscleGroup.LEGS,
            targetRes = R.string.muscle_quads_glutes, stepsRes = R.array.ex_lunge_steps,
            commonMistakesRes = R.array.ex_lunge_mistakes, youtubeVideoId = "-oIl0YJGf9c",
        ),
        CatalogExercise(
            id = "bulgarian_squat", nameRes = R.string.ex_bulgarian_squat_name, group = MuscleGroup.LEGS,
            targetRes = R.string.muscle_quads_glutes, stepsRes = R.array.ex_bulgarian_squat_steps,
            commonMistakesRes = R.array.ex_bulgarian_squat_mistakes, youtubeVideoId = "luFZWNBUMRc",
        ),
        // Glúteos
        CatalogExercise(
            id = "hip_thrust", nameRes = R.string.ex_hip_thrust_name, group = MuscleGroup.GLUTES,
            targetRes = R.string.muscle_glutes, stepsRes = R.array.ex_hip_thrust_steps,
            commonMistakesRes = R.array.ex_hip_thrust_mistakes, youtubeVideoId = "ptK0azwOXwM",
        ),
        CatalogExercise(
            id = "hip_abduction", nameRes = R.string.ex_hip_abduction_name, group = MuscleGroup.GLUTES,
            targetRes = R.string.muscle_glute_medius, stepsRes = R.array.ex_hip_abduction_steps,
            commonMistakesRes = R.array.ex_hip_abduction_mistakes, youtubeVideoId = "e2gmqTG1OgQ",
        ),
        // Panturrilhas
        CatalogExercise(
            id = "standing_calf_raise", nameRes = R.string.ex_standing_calf_raise_name, group = MuscleGroup.CALVES,
            targetRes = R.string.muscle_calves, stepsRes = R.array.ex_standing_calf_raise_steps,
            commonMistakesRes = R.array.ex_standing_calf_raise_mistakes, youtubeVideoId = "of5Z7yj-HqY",
        ),
        // Abdômen
        CatalogExercise(
            id = "plank", nameRes = R.string.ex_plank_name, group = MuscleGroup.CORE,
            targetRes = R.string.muscle_core, stepsRes = R.array.ex_plank_steps,
            commonMistakesRes = R.array.ex_plank_mistakes, youtubeVideoId = "A3YYT8wvxHs",
            isIsometric = true, isBodyWeight = true,
        ),
        CatalogExercise(
            id = "crunch", nameRes = R.string.ex_crunch_name, group = MuscleGroup.CORE,
            targetRes = R.string.muscle_core, stepsRes = R.array.ex_crunch_steps,
            commonMistakesRes = R.array.ex_crunch_mistakes, youtubeVideoId = "t4PBYd481nk",
            isBodyWeight = true,
        ),
    )

    private val byId: Map<String, CatalogExercise> = all.associateBy { it.id }

    fun find(id: String): CatalogExercise? = byId[id]

    /** Exercício do catálogo que certamente existe (usado pelos dados mockados). */
    fun require(id: String): CatalogExercise = requireNotNull(byId[id]) { "Exercício fora do catálogo: $id" }

    /**
     * Filtra o catálogo pelo grupo muscular (ou todos, se `null`) e pelo nome.
     * [nameOf] converte o recurso de nome em texto — fica fora daqui para o filtro ser testável.
     */
    fun search(query: String, group: MuscleGroup?, nameOf: (CatalogExercise) -> String): List<CatalogExercise> {
        val normalizedQuery = query.trim().normalizeForSearch()
        return all.filter { exercise ->
            (group == null || exercise.group == group) &&
                (normalizedQuery.isEmpty() || nameOf(exercise).normalizeForSearch().contains(normalizedQuery))
        }
    }
}

/** Minúsculas e sem acentos: "Elevação" encontra "elevacao". */
fun String.normalizeForSearch(): String =
    java.text.Normalizer.normalize(lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
