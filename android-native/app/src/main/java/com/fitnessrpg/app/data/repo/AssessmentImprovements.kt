package com.fitnessrpg.app.data.repo

import com.fitnessrpg.app.data.dto.BodyAssessmentDto
import com.fitnessrpg.app.data.dto.ConditioningAssessmentDto
import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AssessmentArea {
    OVERALL,
    PHYSIQUE,
    STRENGTH,
    CONDITIONING,
}

data class AssessmentChange(
    val area: AssessmentArea,
    val metric: String,
    val previous: String,
    val current: String,
    val delta: String,
    val detail: String? = null,
)

data class AssessmentImprovementReport(
    val previousHunter: HunterRankResult,
    val currentHunter: HunterRankResult,
    val improvements: List<AssessmentChange>,
    val newBaselines: List<AssessmentChange>,
)

/**
 * Turns two complete assessment snapshots into a user-facing explanation.
 * Only objectively better ranking outputs are called improvements. New metrics
 * are kept separate as baselines so entering missing data is never presented as
 * a gain that the ranking engine did not calculate.
 */
fun compareAssessments(
    previous: RankAssessmentSnapshot,
    current: RankAssessmentSnapshot,
): AssessmentImprovementReport {
    val improvements = mutableListOf<AssessmentChange>()
    val baselines = mutableListOf<AssessmentChange>()

    fun addRank(area: AssessmentArea, metric: String, before: Rank?, after: Rank?) {
        if (after == null) return
        val item = AssessmentChange(
            area = area,
            metric = metric,
            previous = before?.wire ?: "Not assessed",
            current = after.wire,
            delta = if (before == null) "Baseline set" else "Rank increased",
        )
        when {
            before == null -> baselines += item
            after.ordinal > before.ordinal -> improvements += item
        }
    }

    fun addMetric(
        area: AssessmentArea,
        metric: String,
        before: Double?,
        after: Double?,
        unit: String = "pts",
        decimals: Int = 0,
        detail: String? = null,
    ) {
        if (after == null) return
        val formattedAfter = formatNumber(after, decimals, unit)
        if (before == null) {
            baselines += AssessmentChange(
                area = area,
                metric = metric,
                previous = "Not assessed",
                current = formattedAfter,
                delta = "Baseline set",
                detail = detail,
            )
            return
        }
        val difference = after - before
        if (difference <= 0.05) return
        improvements += AssessmentChange(
            area = area,
            metric = metric,
            previous = formatNumber(before, decimals, unit),
            current = formattedAfter,
            delta = "+${formatNumber(difference, decimals, unit)}",
            detail = detail,
        )
    }

    fun addTextImprovement(
        area: AssessmentArea,
        metric: String,
        before: String,
        after: String,
        delta: String,
    ) {
        improvements += AssessmentChange(area, metric, before, after, delta)
    }

    val oldHunter = previous.hunter
    val newHunter = current.hunter
    addRank(AssessmentArea.OVERALL, "Hunter rank", oldHunter.rank, newHunter.rank)
    addMetric(AssessmentArea.OVERALL, "Hunter score", oldHunter.hunterScore, newHunter.hunterScore)
    if (oldHunter.rank == newHunter.rank) {
        addMetric(AssessmentArea.OVERALL, "Hunter RP", oldHunter.rp.toDouble(), newHunter.rp.toDouble(), unit = "RP")
    }
    if (newHunter.confidence.ordinal > oldHunter.confidence.ordinal) {
        addTextImprovement(
            AssessmentArea.OVERALL,
            "Assessment confidence",
            oldHunter.confidence.displayName(),
            newHunter.confidence.displayName(),
            "Confidence increased",
        )
    }
    if (oldHunter.provisional && !newHunter.provisional) {
        addTextImprovement(AssessmentArea.OVERALL, "Rank status", "Provisional", "Validated", "Assessment completed")
    }

    addRank(AssessmentArea.PHYSIQUE, "Physique rank", previous.physique.rank, current.physique.rank)
    addMetric(AssessmentArea.PHYSIQUE, "Physique score", previous.physique.score, current.physique.score)
    if (previous.physique.rank == current.physique.rank) {
        addMetric(AssessmentArea.PHYSIQUE, "Physique RP", previous.physique.rp.toDouble(), current.physique.rp.toDouble(), unit = "RP")
    }
    addMetric(
        AssessmentArea.PHYSIQUE,
        "Body composition",
        previous.physique.bodyCompositionScore,
        current.physique.bodyCompositionScore,
        detail = bodyFatDetail(previous.latestBody, current.latestBody),
    )
    addMetric(
        AssessmentArea.PHYSIQUE,
        "Muscularity",
        previous.physique.muscularityScore,
        current.physique.muscularityScore,
        detail = muscularityDetail(previous.latestBody, current.latestBody),
    )
    addMetric(
        AssessmentArea.PHYSIQUE,
        "Waist health",
        previous.physique.waistScore,
        current.physique.waistScore,
        detail = waistDetail(previous.latestBody, current.latestBody),
    )
    addMetric(AssessmentArea.PHYSIQUE, "Muscle balance", previous.physique.balanceScore, current.physique.balanceScore)

    addRank(AssessmentArea.STRENGTH, "Strength rank", previous.strength.rank, current.strength.rank)
    addMetric(AssessmentArea.STRENGTH, "Strength score", previous.strength.score, current.strength.score)
    if (previous.strength.rank == current.strength.rank) {
        addMetric(AssessmentArea.STRENGTH, "Strength RP", previous.strength.rp.toDouble(), current.strength.rp.toDouble(), unit = "RP")
    }
    current.strength.movementScores.toSortedMap().forEach { (movement, score) ->
        addMetric(
            AssessmentArea.STRENGTH,
            movement.displayMovement(),
            previous.strength.movementScores[movement],
            score,
        )
    }

    addRank(AssessmentArea.CONDITIONING, "Conditioning rank", previous.conditioning.rank, current.conditioning.rank)
    addMetric(
        AssessmentArea.CONDITIONING,
        "Conditioning score",
        previous.conditioning.score,
        current.conditioning.score,
        detail = conditioningDetail(previous.latestConditioning, current.latestConditioning),
    )
    if (previous.conditioning.rank == current.conditioning.rank) {
        addMetric(
            AssessmentArea.CONDITIONING,
            "Conditioning RP",
            previous.conditioning.rp.toDouble(),
            current.conditioning.rp.toDouble(),
            unit = "RP",
        )
    }

    return AssessmentImprovementReport(
        previousHunter = oldHunter,
        currentHunter = newHunter,
        improvements = improvements.distinctBy { Triple(it.area, it.metric, it.current) },
        newBaselines = baselines.distinctBy { Triple(it.area, it.metric, it.current) },
    )
}

private fun formatNumber(value: Double, decimals: Int, unit: String): String {
    val number = if (decimals == 0 || abs(value - value.roundToInt()) < 0.05) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')
    }
    return if (unit.isBlank()) number else "$number $unit"
}

private fun Enum<*>.displayName(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun String.displayMovement(): String = lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun bodyFatDetail(before: BodyAssessmentDto?, after: BodyAssessmentDto?): String? =
    measurementDetail("Body fat", before?.bodyFatPercent, after?.bodyFatPercent, "%")

private fun muscularityDetail(before: BodyAssessmentDto?, after: BodyAssessmentDto?): String? =
    measurementDetail("Skeletal muscle", before?.skeletalMuscleMassKg, after?.skeletalMuscleMassKg, "kg")
        ?: measurementDetail("Lean mass", before?.leanBodyMassKg, after?.leanBodyMassKg, "kg")

private fun waistDetail(before: BodyAssessmentDto?, after: BodyAssessmentDto?): String? =
    measurementDetail("Waist", before?.waistCm, after?.waistCm, "cm")

private fun measurementDetail(label: String, before: Double?, after: Double?, unit: String): String? {
    if (before == null || after == null || abs(before - after) < 0.05) return null
    return "$label ${formatNumber(before, 1, unit)} → ${formatNumber(after, 1, unit)}"
}

private fun conditioningDetail(before: ConditioningAssessmentDto?, after: ConditioningAssessmentDto?): String? {
    if (before == null || after == null || before.testType != after.testType || abs(before.result - after.result) < 0.05) return null
    val (label, unit) = when (after.testType.lowercase()) {
        "cooper_12_minute" -> "12-minute run" to "m"
        "run_1_5_mile" -> "1.5-mile run" to "min"
        "step_3_minute" -> "Recovery heart rate" to "bpm"
        else -> "Test result" to ""
    }
    return "$label ${formatNumber(before.result, 1, unit)} → ${formatNumber(after.result, 1, unit)}"
}
