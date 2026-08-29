package com.fitnessrpg.app.domain.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One exercise pulled from a shared LiftoffRank preset. */
data class ImportedExercise(
    val name: String,
    val sourceId: String?,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val superset: String?,
)

/** A whole shared training preset (name + ordered exercises). */
data class ImportedPlan(
    val name: String,
    val exercises: List<ImportedExercise>,
)

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Extract the `__NEXT_DATA__` JSON blob from a LiftoffRank preset page's HTML.
 * The page is a Next.js app that inlines its data in a single script tag.
 */
fun extractNextData(html: String): String? {
    val marker = "id=\"__NEXT_DATA__\""
    val tagStart = html.indexOf(marker).takeIf { it >= 0 } ?: return null
    val jsonStart = html.indexOf('>', tagStart).takeIf { it >= 0 }?.plus(1) ?: return null
    val jsonEnd = html.indexOf("</script>", jsonStart).takeIf { it >= 0 } ?: return null
    return html.substring(jsonStart, jsonEnd).trim().ifBlank { null }
}

/**
 * Parse a LiftoffRank `__NEXT_DATA__` payload into an [ImportedPlan]. Pure: it
 * walks the dehydrated tRPC/react-query cache to the preset whose data carries an
 * `exerciseData` array, so it does not depend on the exact query ordering.
 */
fun parseLiftoffPlan(nextDataJson: String): ImportedPlan? {
    val root = runCatching { lenientJson.parseToJsonElement(nextDataJson).jsonObject }.getOrNull() ?: return null
    val queries = root["props"]?.jsonObject
        ?.get("pageProps")?.jsonObject
        ?.get("trpcState")?.jsonObject
        ?.get("json")?.jsonObject
        ?.get("queries")?.jsonArray ?: return null

    val data = queries.firstNotNullOfOrNull { q ->
        q.jsonObject["state"]?.jsonObject?.get("data")?.jsonObject?.takeIf { it.containsKey("exerciseData") }
    } ?: return null

    val name = data["name"]?.jsonPrimitive?.contentOrNull()?.trim().orEmpty().ifBlank { "Imported plan" }
    val exercises = data["exerciseData"]?.jsonArray.orEmpty().mapNotNull { element ->
        val obj = element.jsonObject
        val exerciseName = obj["exerciseName"]?.jsonPrimitive?.contentOrNull()?.trim()
            ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val sets = obj["setsData"]?.jsonArray.orEmpty()
        val reps = sets.mapNotNull { set ->
            val p = set.jsonObject["inputTwo"]?.jsonPrimitive ?: return@mapNotNull null
            p.intOrNull ?: p.doubleOrNull?.toInt()
        }.filter { it > 0 }
        ImportedExercise(
            name = exerciseName,
            sourceId = obj["exerciseId"]?.jsonPrimitive?.contentOrNull(),
            sets = sets.size.coerceAtLeast(1),
            repsMin = reps.minOrNull() ?: 8,
            repsMax = reps.maxOrNull() ?: 12,
            superset = obj["superset"]?.jsonPrimitive?.contentOrNull(),
        )
    }
    return if (exercises.isEmpty()) null else ImportedPlan(name, exercises)
}

private fun kotlinx.serialization.json.JsonPrimitive.contentOrNull(): String? =
    if (isString) content else content.takeIf { it != "null" }
