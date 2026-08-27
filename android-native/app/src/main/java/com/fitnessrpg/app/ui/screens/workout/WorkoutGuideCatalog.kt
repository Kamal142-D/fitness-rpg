package com.fitnessrpg.app.ui.screens.workout

import android.content.Context
import com.fitnessrpg.app.R
import java.text.Normalizer
import org.json.JSONArray

internal data class WorkoutGuideEntry(
    val name: String,
    val slug: String,
    val equipment: String,
)

internal object WorkoutGuideCatalog {
    @Volatile
    private var cachedEntries: List<WorkoutGuideEntry>? = null

    fun load(context: Context): List<WorkoutGuideEntry> = cachedEntries ?: synchronized(this) {
        cachedEntries ?: context.resources.openRawResource(R.raw.workout_guide_catalog)
            .bufferedReader()
            .use { reader -> parseCatalog(reader.readText()) }
            .also { cachedEntries = it }
    }

    private fun parseCatalog(rawJson: String): List<WorkoutGuideEntry> {
        val array = JSONArray(rawJson)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    WorkoutGuideEntry(
                        name = item.getString("name"),
                        slug = item.getString("slug"),
                        equipment = item.getString("equipment"),
                    ),
                )
            }
        }
    }
}

internal fun findWorkoutGuideEntry(
    exerciseName: String,
    equipment: String?,
    catalog: List<WorkoutGuideEntry>,
): WorkoutGuideEntry? {
    val normalizedName = normalizeGuideText(exerciseName)
    catalog.firstOrNull { normalizeGuideText(it.name) == normalizedName }?.let { return it }

    val inputTokens = normalizedName.split(' ').filter(String::isNotBlank).toSet()
    if (inputTokens.size < 2) return null
    val normalizedEquipment = normalizeGuideText(equipment.orEmpty())

    val best = catalog.mapNotNull { candidate ->
        val candidateTokens = normalizeGuideText(candidate.name).split(' ').filter(String::isNotBlank).toSet()
        if (candidateTokens.size < 2) return@mapNotNull null

        val overlap = inputTokens.intersect(candidateTokens).size
        if (overlap < 2) return@mapNotNull null

        val candidateCoverage = overlap.toDouble() / candidateTokens.size
        val inputCoverage = overlap.toDouble() / inputTokens.size
        if (candidateCoverage < 0.75 || inputCoverage < 0.4) return@mapNotNull null

        val candidateEquipment = normalizeGuideText(candidate.equipment)
        val equipmentScore = when {
            normalizedEquipment.isBlank() -> 0.0
            normalizedEquipment == candidateEquipment -> 20.0
            normalizedEquipment.contains(candidateEquipment) || candidateEquipment.contains(normalizedEquipment) -> 15.0
            else -> -20.0
        }
        val score = overlap * 10.0 + candidateCoverage * 30.0 + inputCoverage * 20.0 + equipmentScore
        candidate to score
    }.maxByOrNull { it.second }

    return best?.takeIf { it.second >= 78.0 }?.first
}

internal fun workoutGuideFrameUrls(slug: String): List<String> = (1..3).map { frame ->
    "https://cdn.jsdelivr.net/npm/@bryllim/workout-guide@1.0.0/assets/$slug/frame-$frame.png"
}

private fun normalizeGuideText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase()
    .replace("body weight", "bodyweight")
    .replace("resistance band", "band")
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()
    .replace(Regex("\\s+"), " ")
