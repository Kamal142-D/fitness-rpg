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
import com.fitnessrpg.app.ui.components.CardTone
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
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.components.SystemMark
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.util.rememberCached
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt

@kotlinx.serialization.Serializable
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
    onOpenPlan: () -> Unit,
) {
    val sys = rememberCached("system:$userId", SystemData.serializer()) {
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
            Dashboard(userId, d.profile, d.progression, d.recommended, d.hunter, onEnterGate, onSettings, onOpenPlan)
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
    userId: String,
    profile: Profile?,
    progression: PlayerProgression,
    recommended: GateTemplate?,
    hunter: HunterRankResult,
    onEnterGate: (String?) -> Unit,
    onSettings: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val p = progression
    val gate = recommended?.let { templateToSuggestedGate(it) } ?: STARTER_GATE

    ScreenScaffold {
        ScreenHeader(
            eyebrow = "System online",
            title = profile?.displayName ?: "Hunter",
            subtitle = "Your next objective is ready.",
            action = { AppButton("Settings", onClick = onSettings, variant = ButtonVariant.GHOST) },
        )

        val hr = hunter
        AppCard(tone = CardTone.GLASS) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                SystemMark()
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    AppText("PLAYER STATUS", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        RankBadge(hr.rank, size = RankBadgeSize.MD)
                        AppText("Hunter rank ${hr.rank.wire}", variant = TextVariant.TITLE)
                    }
                    AppText("Hunter score ${hr.hunterScore?.roundToInt()?.toString() ?: "—"}", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
                }
            }
            if (hr.provisional) StatusPill("Provisional")
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

        SectionHeader("Today's plan", "Built for your progress")
        TrainingPlanCard(userId = userId, onEnterGate = onEnterGate, onOpenPlan = onOpenPlan)

        SectionHeader("Recommended Gate")
        GateCard(gate, onEnter = { onEnterGate(recommended?.id) })

        SectionHeader("Momentum")
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
