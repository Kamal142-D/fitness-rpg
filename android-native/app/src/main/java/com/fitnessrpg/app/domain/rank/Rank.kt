package com.fitnessrpg.app.domain.rank

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/** The single global V3 rank ladder, ordered weakest to strongest. */
@Serializable
enum class Rank(val wire: String) {
    E("E"), D("D"), C("C"), B("B"), A("A"), S("S"),
    @SerialName("S+") S_PLUS("S+"),
    SS("SS"), SSS("SSS");

    override fun toString(): String = wire
}

@Serializable
enum class RankConfidence { LOW, MEDIUM, HIGH }

data class RankBand(val rank: Rank, val min: Int, val max: Int)

/** Tunable percentile-style V3 boundaries. */
val RANK_THRESHOLDS: List<RankBand> = listOf(
    RankBand(Rank.E, 0, 9),
    RankBand(Rank.D, 10, 24),
    RankBand(Rank.C, 25, 49),
    RankBand(Rank.B, 50, 69),
    RankBand(Rank.A, 70, 84),
    RankBand(Rank.S, 85, 92),
    RankBand(Rank.S_PLUS, 93, 96),
    RankBand(Rank.SS, 97, 98),
    RankBand(Rank.SSS, 99, 100),
)

@Serializable
data class RankedResult(
    val rank: Rank,
    val rp: Int,
    val score: Double,
    val confidence: RankConfidence,
    val provisional: Boolean,
    val previousRank: Rank? = null,
    val previousRp: Int? = null,
    val rankChanged: Boolean = previousRank != null && previousRank != rank,
    val reasons: List<String> = emptyList(),
)

fun rankOrNull(value: String?): Rank? = Rank.entries.firstOrNull {
    it.wire.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true)
}

fun rankOrDefault(value: String?, default: Rank = Rank.E): Rank = rankOrNull(value) ?: default

fun clampScore(score: Double): Double = when {
    score.isNaN() -> 0.0
    score < 0.0 -> 0.0
    score > 100.0 -> 100.0
    else -> score
}

fun scoreToRank(score: Double): Rank {
    val value = clampScore(score)
    return RANK_THRESHOLDS.lastOrNull { value >= it.min }?.rank ?: Rank.E
}

/** Convert the continuous score's location inside its current band to 0..100 RP. */
fun scoreToRp(score: Double): Int {
    val value = clampScore(score)
    val band = RANK_THRESHOLDS.first { it.rank == scoreToRank(value) }
    if (band.rank == Rank.SSS && value >= 100.0) return 100
    val nextMin = RANK_THRESHOLDS.getOrNull(band.rank.ordinal + 1)?.min?.toDouble() ?: 100.0
    if (nextMin <= band.min) return 100
    return (((value - band.min) / (nextMin - band.min)) * 100.0)
        .coerceIn(0.0, 100.0)
        .roundToInt()
}

fun rankedResult(
    score: Double,
    confidence: RankConfidence,
    provisional: Boolean,
    cap: Rank = Rank.SSS,
    previous: RankedResult? = null,
    reasons: List<String> = emptyList(),
): RankedResult {
    val value = clampScore(score)
    val uncapped = scoreToRank(value)
    val rank = if (uncapped.ordinal > cap.ordinal) cap else uncapped
    return RankedResult(
        rank = rank,
        rp = if (rank == uncapped) scoreToRp(value) else 99,
        score = value,
        confidence = confidence,
        provisional = provisional,
        previousRank = previous?.rank,
        previousRp = previous?.rp,
        reasons = reasons.distinct(),
    )
}

fun Rank.nextOrNull(): Rank? = Rank.entries.getOrNull(ordinal + 1)
