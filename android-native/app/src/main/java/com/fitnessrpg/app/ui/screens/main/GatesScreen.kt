package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.gates.muscleGroupsFor
import com.fitnessrpg.app.domain.model.GateTemplate
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
import com.fitnessrpg.app.ui.util.rememberCached

@Composable
fun GatesScreen(userId: String, onOpenGate: (String) -> Unit, onNewGate: () -> Unit) {
    val gates = rememberCached("gates:$userId") { ServiceLocator.gateRepository.listGates() }

    ScreenScaffold {
        Column {
            AppText("GATES", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText("Gate library", variant = TextVariant.DISPLAY)
        }
        AppButton("Create a custom Gate", onClick = onNewGate, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())

        val data = gates.data
        when {
            data != null -> data.forEach { GateRow(userId, it, onOpenGate) { gates.refresh() } }
            gates.loading -> AppText("Loading Gates…", tone = TextTone.SECONDARY)
            gates.error != null -> {
                AppCard { AppText(friendlyDataError(gates.error, "Couldn't load Gates."), tone = TextTone.DANGER) }
                AppButton("Retry", onClick = { gates.refresh() }, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun GateRow(userId: String, template: GateTemplate, onOpen: (String) -> Unit, onChanged: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val difficulty = template.lastDifficultyRank?.let { runCatching { com.fitnessrpg.app.domain.rank.Rank.valueOf(it) }.getOrNull() }
    AppCard(modifier = Modifier.fillMaxWidth().clickable { onOpen(template.id) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            if (difficulty != null) RankBadge(difficulty, size = RankBadgeSize.MD)
            else AppText("NEW", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
            Column(modifier = Modifier.weight(1f)) {
                AppText(template.name, variant = TextVariant.HEADING)
                val muscles = muscleGroupsFor(template.description)
                AppText(
                    (if (muscles.isNotEmpty()) muscles.joinToString(" · ") else "Custom") +
                        "  ·  " + (difficulty?.let { "Last Difficulty ${it.name}" } ?: "Not Assessed"),
                    variant = TextVariant.CAPTION,
                    tone = TextTone.SECONDARY,
                )
            }
            AppText("${template.estimatedDurationMinutes ?: 45} min", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
            Column {
                AppButton("⋮", onClick = { menu = true }, variant = ButtonVariant.GHOST)
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (!template.isSystemTemplate) DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onOpen(template.id) })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = {
                        menu = false
                        scope.launch { ServiceLocator.gateRepository.duplicateGate(userId, template.id); onChanged() }
                    })
                    DropdownMenuItem(text = { Text(if (template.isSystemTemplate) "Hide" else "Delete") }, onClick = { menu = false; confirm = true })
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(if (template.isSystemTemplate) "Hide Gate?" else "Delete Gate?") },
        text = { Text(if (template.isSystemTemplate) "Hide \"${template.name}\" from My Gates?" else "Are you sure you want to delete \"${template.name}\"? Previous completed workouts will remain in history.") },
        confirmButton = { AppButton(if (template.isSystemTemplate) "Hide" else "Delete", onClick = {
            confirm = false
            scope.launch {
                if (template.isSystemTemplate) ServiceLocator.gateRepository.hideSystemGate(userId, template.id)
                else ServiceLocator.gateRepository.archiveGate(template.id)
                onChanged()
            }
        }) },
        dismissButton = { AppButton("Cancel", onClick = { confirm = false }, variant = ButtonVariant.GHOST) },
    )
}
