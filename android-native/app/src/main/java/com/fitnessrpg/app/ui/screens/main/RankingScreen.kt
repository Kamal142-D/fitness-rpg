package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.computeExerciseRanks
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.HunterRankPanel
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

/** Ranking tab: the Hunter Rank, the assessment nudge, and per-exercise ranks. */
@Composable
fun RankingScreen(userId: String, onAssessment: () -> Unit) {
    val player = rememberPlayerBundle(userId)

    ScreenScaffold {
        ScreenHeader("System analysis", "Ranking", subtitle = "Your overall rank and movement mastery.")

        val t = player.data
        when {
            t == null && player.error != null -> AppCard { AppText(friendlyDataError(player.error, "Couldn't load your ranking."), tone = TextTone.DANGER) }
            t == null -> AppText("Loading ranking…", tone = TextTone.SECONDARY)
            else -> {
                val assessment = t.assessment
                if (assessment.needsAssessmentUpdate) {
                    AppCard {
                        AppText("SYSTEM ASSESSMENT UPDATE", variant = TextVariant.HEADING, tone = TextTone.ACCENT)
                        AppText("The Hunter Ranking System has been improved. Complete the missing physical assessments to improve rank accuracy.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                        AppButton("Complete assessment", onAssessment, modifier = Modifier.fillMaxWidth())
                    }
                }
                HunterRankPanel(assessment.hunter, modifier = Modifier.fillMaxWidth())

                val ranks = computeExerciseRanks(t.data.stats, t.data.bodyweightKg, t.data.sex)
                SectionHeader("Exercise ranks", if (ranks.isEmpty()) null else "${ranks.size} ranked")
                AppCard {
                    if (ranks.isEmpty()) {
                        AppText("Log the main barbell lifts to earn Exercise Ranks.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            ranks.forEach { rank ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                                    RankBadge(rank.rank, size = RankBadgeSize.SM)
                                    AppText("${rank.name} · ${rank.rank.wire} — ${rank.rp} RP", variant = TextVariant.LABEL, modifier = Modifier.weight(1f))
                                    rank.best1RMkg?.let {
                                        AppText("${it.roundToInt()} kg 1RM", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
