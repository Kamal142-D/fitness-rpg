package com.fitnessrpg.app.domain.rank

/**
 * The ONE canonical definition of the E..S rank scale (PLAN.txt §6.1, §7).
 * The full ranking engine reuses these thresholds — the bands are never
 * duplicated elsewhere. Everything here is pure and deterministic.
 *
 * Enum order (E < D < C < B < A < S) is the rank ladder weakest -> strongest,
 * so [Rank.ordinal] is the ladder index.
 */
enum class Rank { E, D, C, B, A, S }

/** Inclusive lower/upper score bounds for each rank on a 0..100 scale. */
data class RankBand(val rank: Rank, val min: Int, val max: Int)

val RANK_THRESHOLDS: List<RankBand> = listOf(
    RankBand(Rank.E, 0, 19),
    RankBand(Rank.D, 20, 34),
    RankBand(Rank.C, 35, 49),
    RankBand(Rank.B, 50, 64),
    RankBand(Rank.A, 65, 79),
    RankBand(Rank.S, 80, 100),
)

/** Parse a stored rank letter (e.g. "A") to a [Rank], or null if unrecognized. */
fun rankOrNull(letter: String?): Rank? = Rank.entries.firstOrNull { it.name == letter }

/** Parse a stored rank letter, falling back to [default] when unrecognized. */
fun rankOrDefault(letter: String?, default: Rank = Rank.E): Rank = rankOrNull(letter) ?: default

/** Clamp a raw number into the valid 0..100 score range. */
fun clampScore(score: Double): Double = when {
    score.isNaN() -> 0.0
    score < 0.0 -> 0.0
    score > 100.0 -> 100.0
    else -> score
}

/**
 * Map a 0..100 score to its rank. Out-of-range inputs are clamped first, so this
 * always returns a valid Rank (never throws).
 */
fun scoreToRank(score: Double): Rank {
    val value = clampScore(score)
    for (band in RANK_THRESHOLDS.asReversed()) {
        if (value >= band.min) return band.rank
    }
    // clampScore guarantees value <= 100, so the loop always returns; this is a
    // defensive fallback for the strongest rank.
    return Rank.S
}
