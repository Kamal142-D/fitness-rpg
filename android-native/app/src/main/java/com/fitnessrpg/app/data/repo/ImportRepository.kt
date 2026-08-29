package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.importer.MatchCandidate
import com.fitnessrpg.app.domain.importer.extractNextData
import com.fitnessrpg.app.domain.importer.matchExercise
import com.fitnessrpg.app.domain.importer.parseLiftoffPlan
import com.fitnessrpg.app.domain.model.CreateGateInput
import com.fitnessrpg.app.domain.model.GateExerciseTarget
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

data class MatchedImportExercise(
    val importedName: String,
    val exerciseId: String,
    val catalogName: String,
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
)

data class ImportPreview(
    val planName: String,
    val matched: List<MatchedImportExercise>,
    val unmatched: List<String>,
)

/** Imports a shared LiftoffRank preset link into an app Gate. */
class ImportRepository {

    /** True for links this importer understands. */
    fun isSupported(url: String): Boolean {
        val u = url.trim().lowercase()
        return u.contains("liftoffrank.com/preset/") || u.contains("liftoffrank.com/p/")
    }

    /** Fetch + parse the preset and match its exercises to the catalog for preview. */
    suspend fun previewFromUrl(rawUrl: String): ImportPreview {
        val url = rawUrl.trim()
        require(url.startsWith("https://")) { "The link must be a full https:// LiftoffRank URL." }
        require(isSupported(url)) { "That doesn't look like a LiftoffRank preset link." }

        val html = HttpClient(Android).use { client ->
            client.get(url) { header("User-Agent", "FitnessRPG-Android") }.bodyAsText()
        }
        val nextData = extractNextData(html) ?: error("Couldn't read the plan from that page.")
        val plan = parseLiftoffPlan(nextData) ?: error("No workout plan was found on that page.")

        val catalog = ServiceLocator.gateRepository.listExercises()
            .map { MatchCandidate(it.id, it.name) }
        val byId = catalog.associateBy { it.id }

        val matched = mutableListOf<MatchedImportExercise>()
        val unmatched = mutableListOf<String>()
        for (exercise in plan.exercises) {
            val id = matchExercise(exercise.name, catalog)
            if (id != null) {
                matched += MatchedImportExercise(
                    importedName = exercise.name,
                    exerciseId = id,
                    catalogName = byId[id]?.name ?: exercise.name,
                    sets = exercise.sets,
                    repsMin = exercise.repsMin,
                    repsMax = exercise.repsMax,
                )
            } else {
                unmatched += exercise.name
            }
        }
        return ImportPreview(plan.name, matched, unmatched)
    }

    /** Create a Gate from a previewed import; returns the new Gate id. */
    suspend fun createGate(userId: String, preview: ImportPreview): String {
        require(preview.matched.isNotEmpty()) { "None of the exercises could be matched to your catalog." }
        val input = CreateGateInput(
            name = preview.planName.ifBlank { "Imported plan" },
            exerciseIds = preview.matched.map { it.exerciseId },
            targets = preview.matched.map {
                GateExerciseTarget(it.exerciseId, it.sets, it.repsMin, it.repsMax)
            },
        )
        return ServiceLocator.gateRepository.createGate(userId, input)
    }
}
