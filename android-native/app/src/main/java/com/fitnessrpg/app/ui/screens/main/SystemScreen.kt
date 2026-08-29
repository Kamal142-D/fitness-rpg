package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.R
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.analytics.SessionSummary
import com.fitnessrpg.app.domain.analytics.completedWorkoutDates
import com.fitnessrpg.app.domain.analytics.normalizedWeeklyGoal
import com.fitnessrpg.app.domain.gates.STARTER_GATE
import com.fitnessrpg.app.domain.gates.templateToSuggestedGate
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.model.Profile
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppIconButton
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.GateCard
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.WorkoutConsistencyCard
import com.fitnessrpg.app.ui.util.rememberCached
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@kotlinx.serialization.Serializable
private data class SystemData(
    val profile: Profile?,
    val progression: PlayerProgression?,
    val recommended: GateTemplate?,
    val sessions: List<SessionSummary> = emptyList(),
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
            val sessionsD = async { ServiceLocator.analyticsRepository.getRecentCompletedSessions() }
            SystemData(profileD.await(), progD.await(), recD.await(), sessionsD.await())
        }
    }

    val d = sys.data
    when {
        d != null && d.progression != null ->
            Dashboard(userId, d.profile, d.progression, d.recommended, d.sessions, onEnterGate, onSettings, onOpenPlan)
        d != null -> StateScreen(
            "No data yet",
            "Your progression hasn't been set up. Complete the Awakening to begin.",
            "Account & settings",
            onSettings,
        )
        sys.error != null -> StateScreen(
            "Couldn't load Home",
            friendlyDataError(sys.error, "Something went wrong reaching the server."),
            "Retry",
        ) { sys.refresh() }
        else -> com.fitnessrpg.app.ui.screens.SplashScreen("Loading Home")
    }
}

@Composable
private fun Dashboard(
    userId: String,
    profile: Profile?,
    progression: PlayerProgression,
    recommended: GateTemplate?,
    sessions: List<SessionSummary>,
    onEnterGate: (String?) -> Unit,
    onSettings: () -> Unit,
    onOpenPlan: () -> Unit,
) {
    val p = progression
    val gate = recommended?.let { templateToSuggestedGate(it) } ?: STARTER_GATE

    ScreenScaffold {
        ScreenHeader(
            title = profile?.displayName ?: "Hunter",
            subtitle = "Your next objective is ready.",
            action = {
                AppIconButton(
                    iconRes = R.drawable.ic_settings,
                    contentDescription = "Settings",
                    onClick = onSettings,
                    contained = true,
                )
            },
        )

        WorkoutConsistencyCard(
            streakDays = p.currentStreakDays,
            workoutDates = completedWorkoutDates(sessions),
            weeklyGoal = normalizedWeeklyGoal(profile?.trainingDaysPerWeek),
        )

        SectionHeader("Today's plan", "Built for your progress")
        TrainingPlanCard(userId = userId, onEnterGate = onEnterGate, onOpenPlan = onOpenPlan)

        SectionHeader("Recommended Gate")
        GateCard(gate, onEnter = { onEnterGate(recommended?.id) })

    }
}

@Composable
fun StateScreen(title: String, body: String, actionLabel: String, onAction: () -> Unit) {
    ScreenScaffold {
        AppText(title, variant = TextVariant.DISPLAY)
        AppCard { AppText(body, tone = TextTone.SECONDARY) }
        AppButton(actionLabel, onClick = onAction, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
    }
}
