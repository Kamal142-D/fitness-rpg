package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.plan.PlanState
import com.fitnessrpg.app.domain.plan.PlanStatus
import com.fitnessrpg.app.domain.plan.TrainingPlan
import com.fitnessrpg.app.domain.plan.status
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The "today, per your plan" card on the Home dashboard. Reads the rolling cycle,
 * shows today's workout / rest / missed-workout state, and lets the user start the
 * mapped Gate, do a missed day, or skip to the next slot.
 */
@Composable
fun TrainingPlanCard(userId: String, onEnterGate: (String?) -> Unit, onOpenPlan: () -> Unit) {
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    val plan by produceState<TrainingPlan?>(initialValue = null, key1 = userId, key2 = reload) {
        value = runCatching { ServiceLocator.trainingPlanRepository.get(userId) }.getOrNull()
    }

    val p = plan ?: return
    val today = LocalDate.now(ZoneOffset.UTC).toEpochDay()
    val status = p.status(today) ?: return

    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppText("TRAINING PLAN", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppButton("Edit plan", onClick = onOpenPlan, variant = ButtonVariant.GHOST)
        }
        when (status.state) {
            PlanState.TODAY_WORKOUT -> WorkoutBody(status, onEnterGate, onOpenPlan)
            PlanState.TODAY_REST -> RestBody(status) {
                scope.launch { ServiceLocator.trainingPlanRepository.advance(userId); reload++ }
            }
            PlanState.MISSED_WORKOUT -> MissedBody(
                status = status,
                onDoNow = { scope.launch { ServiceLocator.trainingPlanRepository.renewToday(userId); reload++ } },
                onSkip = { scope.launch { ServiceLocator.trainingPlanRepository.advance(userId); reload++ } },
            )
        }
    }
}

@Composable
private fun WorkoutBody(status: PlanStatus, onEnterGate: (String?) -> Unit, onOpenPlan: () -> Unit) {
    AppText("Today · ${status.current.label}", variant = TextVariant.TITLE)
    val gateId = status.current.gateTemplateId
    if (gateId != null) {
        AppText("Next up: ${status.next.label}", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppButton("▶  Start ${status.current.label}", onClick = { onEnterGate(gateId) }, modifier = Modifier.fillMaxWidth())
    } else {
        AppText("No Gate mapped to this day yet.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppButton("Choose a Gate", onClick = onOpenPlan, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RestBody(status: PlanStatus, onTrainAnyway: () -> Unit) {
    AppText("Rest day", variant = TextVariant.TITLE)
    AppText("Recovery is part of the plan. Next up: ${status.next.label}.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
    AppButton("Train anyway", onClick = onTrainAnyway, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun MissedBody(status: PlanStatus, onDoNow: () -> Unit, onSkip: () -> Unit) {
    val days = status.daysOverdue
    AppText("Missed: ${status.current.label}", variant = TextVariant.TITLE, tone = TextTone.DANGER)
    AppText(
        "You haven't done your ${status.current.label} day (${days} day${if (days == 1L) "" else "s"} ago). Do it now, or skip to ${status.next.label}.",
        variant = TextVariant.CAPTION,
        tone = TextTone.SECONDARY,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        AppButton("Do it now", onClick = onDoNow, modifier = Modifier.weight(1f))
        AppButton("Skip to ${status.next.label}", onClick = onSkip, variant = ButtonVariant.SECONDARY, modifier = Modifier.weight(1f))
    }
}
