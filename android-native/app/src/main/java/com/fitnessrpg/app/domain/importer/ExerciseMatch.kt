package com.fitnessrpg.app.domain.importer

/** A catalog exercise reduced to what matching needs. */
data class MatchCandidate(val id: String, val name: String)

private val STOPWORDS = setOf("the", "a", "an", "with", "and", "of", "to", "for", "grip", "variation")

private fun normalize(raw: String): List<String> = raw
    .lowercase()
    .replace(Regex("[^a-z0-9 ]"), " ")
    .split(' ')
    .map { it.trim() }
    .filter { it.isNotBlank() && it !in STOPWORDS }

/**
 * Best-effort match of an imported exercise name to a catalog exercise. Pure and
 * deterministic. Strategy, strongest first:
 *  1. exact normalized-token equality,
 *  2. one token set is a subset of the other (e.g. "Lat Pulldown" ⊂ "Cable Lat Pulldown"),
 *  3. high Jaccard token overlap (≥ 0.6).
 * Returns the candidate id, or null when nothing is confident enough.
 */
fun matchExercise(importedName: String, catalog: List<MatchCandidate>): String? {
    val target = normalize(importedName).toSet()
    if (target.isEmpty()) return null

    var bestId: String? = null
    var bestScore = 0.0
    var exactId: String? = null
    var bestSubsetId: String? = null
    var bestSubsetSize = Int.MAX_VALUE

    for (candidate in catalog) {
        val tokens = normalize(candidate.name).toSet()
        if (tokens.isEmpty()) continue

        if (tokens == target) {
            exactId = candidate.id
            break
        }
        // Subset either direction — prefer the tightest catalog name that still contains the query.
        if (target.containsAll(tokens) || tokens.containsAll(target)) {
            if (tokens.size < bestSubsetSize) {
                bestSubsetSize = tokens.size
                bestSubsetId = candidate.id
            }
        }
        val overlap = target.intersect(tokens).size.toDouble()
        val union = target.union(tokens).size.toDouble()
        val jaccard = if (union == 0.0) 0.0 else overlap / union
        if (jaccard > bestScore) {
            bestScore = jaccard
            bestId = candidate.id
        }
    }

    return exactId ?: bestSubsetId ?: bestId?.takeIf { bestScore >= 0.6 }
}
