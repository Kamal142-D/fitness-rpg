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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.quests.UserQuestView
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppProgressBar
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private fun trim(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

@Composable
fun QuestsScreen() {
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val result by produceState<Result<List<UserQuestView>>?>(null, reload) {
        value = null
        value = runCatching { ServiceLocator.questRepository.listUserQuests() }
    }

    ScreenScaffold {
        Column {
            AppText("QUESTS", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText("Quests", variant = TextVariant.DISPLAY)
        }

        val r = result
        when {
            r == null -> AppText("Loading Quests…", tone = TextTone.SECONDARY)
            r.isFailure -> AppCard { AppText(r.exceptionOrNull()?.message ?: "Couldn't load Quests.", tone = TextTone.DANGER) }
            r.getOrThrow().isEmpty() -> AppCard { AppText("No active quests right now. Check back tomorrow.", tone = TextTone.SECONDARY) }
            else -> r.getOrThrow().forEach { quest ->
                QuestCard(quest, onClaim = {
                    scope.launch {
                        runCatching { ServiceLocator.questRepository.claimQuest(quest.id) }
                        reload++
                    }
                })
            }
        }
    }
}

@Composable
private fun QuestCard(quest: UserQuestView, onClaim: () -> Unit) {
    val fraction = if (quest.requirementValue > 0) (quest.progress / quest.requirementValue).toFloat() else 0f
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(quest.name, variant = TextVariant.HEADING)
                quest.description?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
            }
            AppText("+${quest.xpReward} XP", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
        }
        AppProgressBar(fraction, modifier = Modifier.padding(top = Spacing.md))
        Row(Modifier.fillMaxWidth().padding(top = Spacing.xs), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("${trim(quest.progress)} / ${trim(quest.requirementValue)}", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
            when {
                quest.claimed -> AppText("CLAIMED", variant = TextVariant.CAPTION, tone = TextTone.SUCCESS)
                quest.completed -> AppButton("Claim", onClick = onClaim)
                else -> AppText("In progress", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
            }
        }
    }
}
