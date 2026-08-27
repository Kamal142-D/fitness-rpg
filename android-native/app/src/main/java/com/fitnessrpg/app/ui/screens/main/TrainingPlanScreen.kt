package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.plan.SlotKind
import com.fitnessrpg.app.domain.plan.TrainingPlan
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/** Editor for the rolling training cycle: shows every slot and maps a Gate to each workout day. */
@Composable
fun TrainingPlanScreen(userId: String, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    val plan by produceState<TrainingPlan?>(initialValue = null, key1 = userId, key2 = reload) {
        value = runCatching { ServiceLocator.trainingPlanRepository.get(userId) }.getOrNull()
    }
    val gates by produceState<List<GateTemplate>>(initialValue = emptyList(), key1 = userId) {
        value = runCatching { ServiceLocator.gateRepository.listGates() }.getOrDefault(emptyList())
    }

    ScreenScaffold {
        ScreenHeader(
            eyebrow = "Training plan",
            title = "Your Split",
            subtitle = "A rolling cycle that advances every time you train.",
            action = if (onBack != null) ({ AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST) }) else null,
        )

        val p = plan
        if (p == null) {
            AppText("Loading your plan…", tone = TextTone.SECONDARY)
        } else {
            val currentIdx = p.currentIndex.mod(p.slots.size.coerceAtLeast(1))
            p.slots.forEachIndexed { index, slot ->
                SlotRow(
                    number = index + 1,
                    label = slot.label,
                    isRest = slot.kind == SlotKind.REST,
                    isCurrent = index == currentIdx,
                    mappedGateName = slot.gateTemplateId?.let { id -> gates.firstOrNull { it.id == id }?.name },
                    gates = gates,
                    onPick = { gateId ->
                        scope.launch { ServiceLocator.trainingPlanRepository.setSlotGate(userId, index, gateId); reload++ }
                    },
                )
            }
            AppButton(
                "Reset to default split",
                onClick = { scope.launch { ServiceLocator.trainingPlanRepository.resetToDefault(userId); reload++ } },
                variant = ButtonVariant.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SlotRow(
    number: Int,
    label: String,
    isRest: Boolean,
    isCurrent: Boolean,
    mappedGateName: String?,
    gates: List<GateTemplate>,
    onPick: (String?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            AppText(number.toString().padStart(2, '0'), variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
            Column(Modifier.weight(1f)) {
                AppText(label + if (isCurrent) "  · TODAY" else "", variant = TextVariant.LABEL, tone = if (isCurrent) TextTone.ACCENT else TextTone.PRIMARY)
                if (isRest) {
                    AppText("Rest day", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                } else {
                    AppText(mappedGateName ?: "No Gate mapped", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                }
            }
            if (!isRest) {
                Box {
                    AppButton(if (mappedGateName == null) "Set Gate" else "Change", onClick = { menuOpen = true }, variant = ButtonVariant.SECONDARY)
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (mappedGateName != null) {
                            DropdownMenuItem(text = { Text("Clear mapping") }, onClick = { menuOpen = false; onPick(null) })
                        }
                        gates.forEach { gate ->
                            DropdownMenuItem(text = { Text(gate.name) }, onClick = { menuOpen = false; onPick(gate.id) })
                        }
                    }
                }
            }
        }
    }
}
