package com.fitnessrpg.app.ui.components

import com.fitnessrpg.app.domain.rank.Rank
import com.fitnessrpg.app.domain.rank.scoreToRank

/** One canonical presentation of a physical pillar across Ranking and Profile. */
data class AttributeDisplay(
    val rank: Rank,
    val rp: Int,
)

fun attributeDisplay(score: Double, authoritativeRank: Rank?, authoritativeRp: Int?): AttributeDisplay =
    AttributeDisplay(
        rank = authoritativeRank ?: scoreToRank(score),
        rp = (authoritativeRp ?: 0).coerceIn(0, 100),
    )
