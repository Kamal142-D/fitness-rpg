package com.fitnessrpg.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.gates.formatTargets
import com.fitnessrpg.app.domain.gates.templateDifficulty
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.RankBadge
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing

@Composable
fun GateDetailScreen(templateId: String, onBack: () -> Unit, onStarted: () -> Unit) {
    val result by produceState<Result<GateDetail?>?>(null, templateId) {
        value = runCatching { ServiceLocator.gateRepository.getGate(templateId) }
    }

    ScreenScaffold {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("GATE", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST)
        }

        val r = result
        when {
            r == null -> AppText("Loading Gate…", tone = TextTone.SECONDARY)
            r.isFailure -> AppCard { AppText(r.exceptionOrNull()?.message ?: "Couldn't load this Gate.", tone = TextTone.DANGER) }
            r.getOrNull() == null -> AppCard { AppText("This Gate no longer exists.", tone = TextTone.SECONDARY) }
            else -> {
                val detail = r.getOrThrow()!!
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(templateDifficulty(detail.template), size = RankBadgeSize.LG)
                    Column {
                        AppText(detail.template.name, variant = TextVariant.DISPLAY)
                        detail.template.description?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
                    }
                }

                AppCard {
                    AppText("EXERCISES", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, modifier = Modifier.fillMaxWidth())
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        detail.exercises.forEach { twe ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                AppText(twe.exercise.name, variant = TextVariant.LABEL, modifier = Modifier.weight(1f))
                                AppText(formatTargets(twe.templateExercise), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
                            }
                        }
                    }
                }

                AppButton(
                    "Start workout",
                    onClick = {
                        ServiceLocator.activeWorkoutStore.start(detail)
                        onStarted()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
