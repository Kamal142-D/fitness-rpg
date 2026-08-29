package com.fitnessrpg.app.ui.screens.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.gates.formatTargets
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
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.theme.Spacing

@Composable
fun GateDetailScreen(userId: String, templateId: String, onBack: () -> Unit, onEdit: () -> Unit, onStarted: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val result by produceState<Result<GateDetail?>?>(null, templateId) {
        value = runCatching { ServiceLocator.gateRepository.getGate(templateId) }
    }

    ScreenScaffold {
        ScreenHeader("Gate Details", subtitle = "Review the routine before you enter.", action = {
            AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST)
        })

        val r = result
        when {
            r == null -> AppText("Loading Gate…", tone = TextTone.SECONDARY)
            r.isFailure -> AppCard { AppText(friendlyDataError(r.exceptionOrNull(), "Couldn't load this Gate."), tone = TextTone.DANGER) }
            r.getOrNull() == null -> AppCard { AppText("This Gate no longer exists.", tone = TextTone.SECONDARY) }
            else -> {
                val detail = r.getOrThrow()!!
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    val assessed = com.fitnessrpg.app.domain.rank.rankOrNull(detail.template.lastDifficultyRank)
                    if (assessed != null) RankBadge(assessed, size = RankBadgeSize.LG)
                    Column {
                        AppText(detail.template.name, variant = TextVariant.DISPLAY)
                        detail.template.description?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
                        AppText(if (assessed == null) "DIFFICULTY · Not Assessed" else "LAST DIFFICULTY · ${assessed.wire}", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    }
                }

                SectionHeader("Exercises", "${detail.exercises.size} movements")
                AppCard {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        detail.exercises.forEachIndexed { index, twe ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                                AppText((index + 1).toString().padStart(2, '0'), variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
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
                if (!detail.template.isSystemTemplate) AppButton("Edit Gate", onClick = onEdit, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
                AppButton(
                    "Delete Gate",
                    onClick = { confirmDelete = true },
                    variant = ButtonVariant.GHOST,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (confirmDelete) AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Delete Gate?") },
                    text = { Text("Are you sure you want to delete \"${detail.template.name}\"? Your previous completed workouts will remain in history.") },
                    confirmButton = { AppButton("Delete", onClick = {
                        confirmDelete = false
                        scope.launch {
                            runCatching {
                                // System starter Gates are a shared catalog and cannot be
                                // removed server-side, so deleting one removes it from this
                                // user's library; custom Gates are soft-deleted.
                                if (detail.template.isSystemTemplate) ServiceLocator.gateRepository.hideSystemGate(userId, detail.template.id)
                                else ServiceLocator.gateRepository.archiveGate(detail.template.id)
                            }.onSuccess { onBack() }
                                .onFailure { actionError = friendlyDataError(it, "Couldn't delete this Gate.") }
                        }
                    }) },
                    dismissButton = { AppButton("Cancel", onClick = { confirmDelete = false }, variant = ButtonVariant.GHOST) },
                )
                actionError?.let { message ->
                    AlertDialog(
                        onDismissRequest = { actionError = null },
                        title = { Text("Action failed") },
                        text = { Text(message) },
                        confirmButton = { AppButton("OK", onClick = { actionError = null }) },
                    )
                }
            }
        }
    }
}
