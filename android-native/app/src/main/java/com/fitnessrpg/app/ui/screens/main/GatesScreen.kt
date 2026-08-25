package com.fitnessrpg.app.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.gates.intensityForDifficulty
import com.fitnessrpg.app.domain.gates.muscleGroupsFor
import com.fitnessrpg.app.domain.gates.templateDifficulty
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

@Composable
fun GatesScreen(userId: String, onOpenGate: (String) -> Unit, onNewGate: () -> Unit) {
    var reload by remember { mutableIntStateOf(0) }
    val result by produceState<Result<List<GateTemplate>>?>(null, reload) {
        value = null
        value = runCatching { ServiceLocator.gateRepository.listGates() }
    }

    ScreenScaffold {
        Column {
            AppText("GATES", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText("Gate library", variant = TextVariant.DISPLAY)
        }
        AppButton("Create a custom Gate", onClick = onNewGate, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())

        val r = result
        when {
            r == null -> AppText("Loading Gates…", tone = TextTone.SECONDARY)
            r.isFailure -> {
                AppCard { AppText(r.exceptionOrNull()?.message ?: "Couldn't load Gates.", tone = TextTone.DANGER) }
                AppButton("Retry", onClick = { reload++ }, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
            }
            else -> r.getOrThrow().forEach { GateRow(it, onOpenGate) }
        }
    }
}

@Composable
private fun GateRow(template: GateTemplate, onOpen: (String) -> Unit) {
    val difficulty = templateDifficulty(template)
    AppCard(modifier = Modifier.fillMaxWidth().clickable { onOpen(template.id) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            RankBadge(difficulty, size = RankBadgeSize.MD)
            Column(modifier = Modifier.weight(1f)) {
                AppText(template.name, variant = TextVariant.HEADING)
                val muscles = muscleGroupsFor(template.description)
                AppText(
                    (if (muscles.isNotEmpty()) muscles.joinToString(" · ") else "Custom") +
                        "  ·  ${intensityForDifficulty(difficulty)}",
                    variant = TextVariant.CAPTION,
                    tone = TextTone.SECONDARY,
                )
            }
            AppText("${template.estimatedDurationMinutes ?: 45} min", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
        }
    }
}
