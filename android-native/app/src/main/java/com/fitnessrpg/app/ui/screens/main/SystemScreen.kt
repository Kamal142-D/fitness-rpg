package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.gates.STARTER_GATE
import com.fitnessrpg.app.domain.gates.templateToSuggestedGate
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.model.Profile
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.GateCard
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.StatChip
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.XpBar
import com.fitnessrpg.app.ui.components.AttributeRow
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.util.rememberCached
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

private data class SystemData(
    val profile: Profile?,
    val progression: PlayerProgression?,
    val recommended: GateTemplate?,
    val hunter: HunterRankResult,
)

@Composable
fun SystemScreen(
    userId: String,
    onEnterGate: (gateId: String?) -> Unit,
    onSettings: () -> Unit,
) {
    val sys = rememberCached("system:$userId") {
        coroutineScope {
            val profileD = async { ServiceLocator.profileRepository.getProfile(userId) }
            val progD = async { ServiceLocator.progressionRepository.getProgression(userId) }
            val recD = async { ServiceLocator.gateRepository.getRecommendedGate() }
            val assessmentD = async { ServiceLocator.assessmentRepository.getRankAssessment(userId) }
            SystemData(profileD.await(), progD.await(), recD.await(), assessmentD.await().hunter)
        }
    }

    val d = sys.data
    when {
        d != null && d.progression != null ->
            Dashboard(d.profile, d.progression, d.recommended, d.hunter, onEnterGate, onSettings)
        d != null -> StateScreen(
            "No data yet",
            "Your progression hasn't been set up. Complete the Awakening to begin.",
            "Account & settings",
            onSettings,
        )
        sys.error != null -> StateScreen(
            "Couldn't load your System",
            friendlyDataError(sys.error, "Something went wrong reaching the server."),
            "Retry",
        ) { sys.refresh() }
        else -> com.fitnessrpg.app.ui.screens.SplashScreen("Loading System")
    }
}

@Composable
private fun Dashboard(
    profile: Profile?,
    progression: PlayerProgression,
    recommended: GateTemplate?,
    hunter: HunterRankResult,
    onEnterGate: (String?) -> Unit,
    onSettings: () -> Unit,
) {
    val p = progression
    val gate = recommended?.let { templateToSuggestedGate(it) } ?: STARTER_GATE

    ScreenScaffold {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                AppText("SYSTEM", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText(profile?.displayName ?: "Hunter", variant = TextVariant.DISPLAY)
            }
            AppButton("Settings", onClick = onSettings, variant = ButtonVariant.GHOST)
        }

        val hr = hunter
        AppCard {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                RankBadge(hr.rank, size = RankBadgeSize.LG)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    AppText("PLAYER STATUS", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    AppText("Rank ${hr.rank.name}${if (hr.provisional) " · Provisional" else ""}", variant = TextVariant.TITLE)
                    AppText("Hunter score ${hr.hunterScore?.roundToInt()?.toString() ?: "—"}", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
                }
            }
            XpBar(p.level, p.currentXp, modifier = Modifier.padding(top = Spacing.lg).fillMaxWidth())
            Column(modifier = Modifier.padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                hr.strengthScore?.let { AttributeRow("Strength", it) }
                hr.physiqueScore?.let { AttributeRow("Physique", it) }
                hr.conditioningScore?.let { AttributeRow("Conditioning", it) }
            }
            if (hr.limitingAttribute != null) {
                AppText("${hr.limitingAttribute!!.name.lowercase().replaceFirstChar { it.uppercase() }} is limiting your rank.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, modifier = Modifier.padding(top = Spacing.sm))
            }
        }

        GateCard(gate, onEnter = { onEnterGate(recommended?.id) })

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatChip("STREAK", "${p.currentStreakDays} d", modifier = Modifier.weight(1f))
            StatChip("BEST STREAK", "${p.longestStreakDays} d", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StateScreen(title: String, body: String, actionLabel: String, onAction: () -> Unit) {
    ScreenScaffold {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppText("SYSTEM", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText(title, variant = TextVariant.DISPLAY)
        }
        AppCard { AppText(body, tone = TextTone.SECONDARY) }
        AppButton(actionLabel, onClick = onAction, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
    }
}
