package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.gates.formatTargets
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.RankBadgeSize
import com.fitnessrpg.app.ui.components.UnknownRankBadge
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.AppIconButton
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.RevealContent
import com.fitnessrpg.app.ui.theme.MaxContentWidth
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing
import com.fitnessrpg.app.ui.util.rememberCached
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer

@Composable
fun GatesScreen(
    userId: String,
    onOpenGate: (String) -> Unit,
    onNewGate: () -> Unit,
    onWorkoutStarted: () -> Unit,
) {
    val gates = rememberCached(
        "gates:$userId",
        ListSerializer(GateTemplate.serializer()),
    ) { ServiceLocator.gateRepository.listGates() }
    var selectedGateId by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val data = gates.data

    LaunchedEffect(data) {
        val ids = data.orEmpty().mapTo(mutableSetOf()) { it.id }
        if (selectedGateId !in ids) selectedGateId = data?.firstOrNull()?.id
    }

    val selectedTemplate = data?.firstOrNull { it.id == selectedGateId }
    val detailResult by produceState<Result<GateDetail?>?>(
        initialValue = null,
        key1 = selectedGateId,
    ) {
        value = selectedGateId?.let { id ->
            runCatching { ServiceLocator.gateRepository.getGate(id) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxContentWidth)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.xl, top = Spacing.xl, end = Spacing.xl, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ScreenHeader(
                eyebrow = "Gate network",
                title = "Gates",
                subtitle = "Choose a routine and enter when you're ready.",
                action = {
                    AppIconButton(Icons.Filled.Add, "Create Gate", onNewGate)
                    Box {
                        AppIconButton(Icons.Filled.MoreVert, "Gate options", { menuOpen = true }, enabled = selectedTemplate != null)
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            selectedTemplate?.let { template ->
                                DropdownMenuItem(text = { Text("Open details") }, onClick = { menuOpen = false; onOpenGate(template.id) })
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = {
                                    menuOpen = false
                                    scope.launch {
                                        selectedGateId = ServiceLocator.gateRepository.duplicateGate(userId, template.id)
                                        gates.refresh()
                                    }
                                })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; confirmDelete = true })
                            }
                        }
                    }
                },
            )

            when {
                data != null && data.isEmpty() -> EmptyGateLibrary(onNewGate)
                data != null -> {
                    SectionHeader("Gate library", "${data.size} available")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        data.forEach { gate ->
                            val selected = gate.id == selectedGateId
                            FilterChip(
                                selected = selected,
                                onClick = { selectedGateId = gate.id },
                                label = { AppText(gate.name, variant = TextVariant.LABEL, tone = if (selected) TextTone.ACCENT else TextTone.SECONDARY) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Palette.Surface1,
                                    selectedContainerColor = Palette.PrimaryContainer,
                                    selectedLabelColor = Palette.Primary,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = Palette.HairlineStrong,
                                    selectedBorderColor = Palette.Primary.copy(alpha = 0.45f),
                                ),
                            )
                        }
                    }

                    val selectedDetail = detailResult?.getOrNull()
                    when {
                        detailResult == null -> AppText("Loading Gate…", tone = TextTone.SECONDARY)
                        detailResult?.isFailure == true -> {
                            AppCard {
                                AppText(
                                    friendlyDataError(detailResult?.exceptionOrNull(), "Couldn't load this Gate."),
                                    tone = TextTone.DANGER,
                                )
                            }
                        }
                        selectedDetail == null -> AppText("This Gate is unavailable.", tone = TextTone.SECONDARY)
                        else -> key(selectedGateId) {
                            RevealContent {
                                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                                    GatePlan(
                                        detail = selectedDetail,
                                        onStart = {
                                            ServiceLocator.activeWorkoutStore.start(selectedDetail)
                                            onWorkoutStarted()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                gates.loading -> AppText("Loading Gates…", tone = TextTone.SECONDARY)
                gates.error != null -> {
                    AppCard {
                        AppText(friendlyDataError(gates.error, "Couldn't load Gates."), tone = TextTone.DANGER)
                    }
                    AppButton(
                        "Retry",
                        onClick = { gates.refresh() },
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    selectedTemplate?.let { template ->
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete Gate?") },
                text = {
                    Text("Delete \"${template.name}\"? Previous completed workouts will remain in history.")
                },
                confirmButton = {
                    AppButton("Delete", onClick = {
                        confirmDelete = false
                        scope.launch {
                            runCatching {
                                // System starter Gates are a shared catalog and cannot be
                                // removed from the server, so deleting one removes it from
                                // this user's library; custom Gates are soft-deleted.
                                if (template.isSystemTemplate) {
                                    ServiceLocator.gateRepository.hideSystemGate(userId, template.id)
                                } else {
                                    ServiceLocator.gateRepository.archiveGate(template.id)
                                }
                            }.onSuccess {
                                selectedGateId = null
                                gates.refresh()
                            }.onFailure {
                                actionError = friendlyDataError(it, "Couldn't delete this Gate.")
                            }
                        }
                    })
                },
                dismissButton = {
                    AppButton("Cancel", onClick = { confirmDelete = false }, variant = ButtonVariant.GHOST)
                },
            )
        }
    }

    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("Action failed") },
            text = { Text(message) },
            confirmButton = {
                AppButton("OK", onClick = { actionError = null })
            },
        )
    }
}

@Composable
private fun GatePlan(detail: GateDetail, onStart: () -> Unit) {
    // A Gate's clear rank is earned per attempt, not fixed to the Gate, so it reads
    // "Unknown" here — the ranks you actually earned live in the History page.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnknownRankBadge(size = RankBadgeSize.LG)
        Column(Modifier.weight(1f)) {
            AppText(detail.template.name, variant = TextVariant.TITLE)
            AppText(
                "${detail.template.estimatedDurationMinutes ?: 45} min  ·  ${detail.exercises.size} exercises  ·  Unknown",
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
            )
        }
    }

    AppCard(modifier = Modifier.fillMaxWidth(), padding = Spacing.none) {
        detail.exercises.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    (index + 1).toString().padStart(2, '0'),
                    variant = TextVariant.CAPTION,
                    tone = TextTone.TERTIARY,
                    mono = true,
                )
                Column(Modifier.weight(1f)) {
                    AppText(item.exercise.name, variant = TextVariant.LABEL)
                    AppText(
                        formatTargets(item.templateExercise),
                        variant = TextVariant.CAPTION,
                        tone = TextTone.SECONDARY,
                    )
                }
                AppText("›", variant = TextVariant.HEADING, tone = TextTone.TERTIARY)
            }
            if (index != detail.exercises.lastIndex) HorizontalDivider(color = Palette.Hairline)
        }
    }

    AppButton(
        "Start Workout",
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyGateLibrary(onNewGate: () -> Unit) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        AppText("No Gates yet", variant = TextVariant.HEADING)
        AppText("Create a workout Gate to begin your next battle.", tone = TextTone.SECONDARY)
        AppButton("Create Gate", onClick = onNewGate, modifier = Modifier.fillMaxWidth())
    }
}
