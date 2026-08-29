package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.SessionSummary
import com.fitnessrpg.app.domain.rank.rankOrNull
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.UnknownRankBadge
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.theme.Spacing
import kotlin.math.roundToInt

/** History page: every Gate you've cleared, newest first, with the rank you earned. */
@Composable
fun HistoryScreen(userId: String, onBack: (() -> Unit)? = null) {
    val player = rememberPlayerBundle(userId)

    ScreenScaffold {
        ScreenHeader(
            title = "Gate History",
            subtitle = "Every completed Gate, newest first.",
            action = if (onBack != null) ({ AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST) }) else null,
        )

        val t = player.data
        when {
            t == null && player.error != null -> AppCard { AppText(friendlyDataError(player.error, "Couldn't load your history."), tone = TextTone.DANGER) }
            t == null -> AppText("Loading history…", tone = TextTone.SECONDARY)
            t.data.sessions.isEmpty() -> AppCard { AppText("No cleared Gates yet. Finish a workout to start your history.", tone = TextTone.SECONDARY) }
            else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                t.data.sessions.forEach { session -> HistoryRow(session) }
            }
        }
    }
}

@Composable
private fun HistoryRow(session: SessionSummary) {
    // "The gate's rank" throughout the app is the difficulty rank; the clear grade
    // is a separate metric, so show the difficulty badge and label both.
    val difficulty = rankOrNull(session.gateDifficultyRank)
    val clear = rankOrNull(session.gateClearRank)
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            if (difficulty != null) RankBadge(difficulty, size = RankBadgeSize.MD) else UnknownRankBadge(size = RankBadgeSize.MD)
            Column(Modifier.weight(1f)) {
                AppText(session.name?.takeIf { it.isNotBlank() } ?: "Gate", variant = TextVariant.LABEL)
                AppText(subtitle(session), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText(
                    "Difficulty ${difficulty?.wire ?: "—"}  ·  Clear ${clear?.wire ?: "—"}",
                    variant = TextVariant.CAPTION,
                    tone = TextTone.TERTIARY,
                    mono = true,
                )
            }
        }
    }
}

private fun subtitle(session: SessionSummary): String {
    val date = session.completedAt?.take(10) ?: "—"
    val volume = session.totalVolumeKg?.let { " · ${it.roundToInt()} kg" } ?: ""
    val mins = session.durationSeconds?.let { " · ${it / 60} min" } ?: ""
    return "$date$volume$mins"
}
