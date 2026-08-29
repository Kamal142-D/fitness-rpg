package com.fitnessrpg.app.ui.screens.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fitnessrpg.app.R
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.model.GateTemplate
import com.fitnessrpg.app.domain.plan.PlanSlot
import com.fitnessrpg.app.domain.plan.SlotKind
import com.fitnessrpg.app.domain.plan.TrainingPlan
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AppTextField
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class EditablePlanSlot(val id: String, val slot: PlanSlot)

/** Editor for the rolling training cycle: rename, reorder, and map every workout slot. */
@Composable
fun TrainingPlanScreen(userId: String, onBack: (() -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val editMutex = remember { Mutex() }
    val listState = rememberLazyListState()
    var reload by remember { mutableIntStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val plan by produceState<TrainingPlan?>(initialValue = null, key1 = userId, key2 = reload) {
        value = runCatching { ServiceLocator.trainingPlanRepository.get(userId) }.getOrNull()
    }
    val gates by produceState<List<GateTemplate>>(initialValue = emptyList(), key1 = userId) {
        value = runCatching { ServiceLocator.gateRepository.listGates() }.getOrDefault(emptyList())
    }

    ScreenScaffold(scroll = false) {
        ScreenHeader(
            title = "Your Split",
            subtitle = "Rename workouts or hold the handle to change their order.",
            action = if (onBack != null) ({ AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST) }) else null,
        )

        val loadedPlan = plan
        if (loadedPlan == null) {
            AppText("Loading your plan…", tone = TextTone.SECONDARY)
        } else {
            var editableSlots by remember(loadedPlan) {
                mutableStateOf(loadedPlan.slots.mapIndexed { index, slot -> EditablePlanSlot("slot:$index", slot) })
            }
            val currentSlotId = remember(loadedPlan) {
                editableSlots.getOrNull(loadedPlan.currentIndex.mod(editableSlots.size.coerceAtLeast(1)))?.id
            }
            var draggedId by remember { mutableStateOf<String?>(null) }
            var dragStartIndex by remember { mutableIntStateOf(-1) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            var dragOriginalSlots by remember { mutableStateOf<List<EditablePlanSlot>?>(null) }

            fun finishDrag(persist: Boolean) {
                val id = draggedId
                val fromIndex = dragStartIndex
                val toIndex = if (id == null) -1 else editableSlots.indexOfFirst { it.id == id }
                val original = dragOriginalSlots
                draggedId = null
                dragStartIndex = -1
                dragOffsetY = 0f
                dragOriginalSlots = null

                if (!persist) {
                    if (original != null) editableSlots = original
                    return
                }
                if (fromIndex !in editableSlots.indices || toIndex !in editableSlots.indices || fromIndex == toIndex) return
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch {
                    editMutex.withLock {
                        runCatching {
                            ServiceLocator.trainingPlanRepository.reorderSlot(userId, fromIndex, toIndex)
                        }.onSuccess {
                            saveError = null
                        }.onFailure {
                            saveError = "Couldn't save the new order. Your plan was restored."
                            reload++
                        }
                    }
                }
            }

            AppText("Changes save automatically.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            saveError?.let { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.DANGER) }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                itemsIndexed(
                    items = editableSlots,
                    key = { _, item -> item.id },
                ) { _, item ->
                    val itemIndex = editableSlots.indexOfFirst { it.id == item.id }
                    val isDragging = draggedId == item.id
                    SlotRow(
                        modifier = Modifier
                            .animateItem(
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                scaleX = if (isDragging) 1.015f else 1f
                                scaleY = if (isDragging) 1.015f else 1f
                                shadowElevation = if (isDragging) 14.dp.toPx() else 0f
                                shape = RoundedCornerShape(Radius.lg)
                            },
                        number = itemIndex + 1,
                        label = item.slot.label,
                        isRest = item.slot.kind == SlotKind.REST,
                        isCurrent = item.id == currentSlotId,
                        mappedGateName = item.slot.gateTemplateId?.let { id -> gates.firstOrNull { it.id == id }?.name },
                        gates = gates,
                        onPick = pick@{ gateId ->
                            val index = editableSlots.indexOfFirst { it.id == item.id }
                            if (index !in editableSlots.indices) return@pick
                            editableSlots = editableSlots.toMutableList().apply {
                                this[index] = this[index].copy(slot = this[index].slot.copy(gateTemplateId = gateId))
                            }
                            scope.launch {
                                editMutex.withLock {
                                    runCatching { ServiceLocator.trainingPlanRepository.setSlotGate(userId, index, gateId) }
                                        .onSuccess { saveError = null }
                                        .onFailure { saveError = "Couldn't save the Gate mapping."; reload++ }
                                }
                            }
                        },
                        onRename = rename@{ newLabel ->
                            val index = editableSlots.indexOfFirst { it.id == item.id }
                            if (index !in editableSlots.indices) return@rename
                            editableSlots = editableSlots.toMutableList().apply {
                                this[index] = this[index].copy(slot = this[index].slot.copy(label = newLabel))
                            }
                            scope.launch {
                                editMutex.withLock {
                                    runCatching { ServiceLocator.trainingPlanRepository.renameSlot(userId, index, newLabel) }
                                        .onSuccess { saveError = null }
                                        .onFailure { saveError = "Couldn't save the new workout name."; reload++ }
                                }
                            }
                        },
                        onDragStart = {
                            val index = editableSlots.indexOfFirst { it.id == item.id }
                            if (index >= 0) {
                                draggedId = item.id
                                dragStartIndex = index
                                dragOffsetY = 0f
                                dragOriginalSlots = editableSlots
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = drag@{ deltaY ->
                            if (draggedId != item.id) return@drag
                            dragOffsetY += deltaY
                            val layout = listState.layoutInfo
                            val draggedInfo = layout.visibleItemsInfo.firstOrNull { it.key == item.id } ?: return@drag
                            val draggedCenter = draggedInfo.offset + (draggedInfo.size / 2f) + dragOffsetY
                            val targetInfo = layout.visibleItemsInfo.firstOrNull { info ->
                                info.index < editableSlots.size &&
                                    draggedCenter >= info.offset &&
                                    draggedCenter <= info.offset + info.size
                            }
                            val fromIndex = editableSlots.indexOfFirst { it.id == item.id }
                            val targetIndex = targetInfo?.index ?: fromIndex
                            if (fromIndex in editableSlots.indices && targetIndex in editableSlots.indices && fromIndex != targetIndex) {
                                dragOffsetY += draggedInfo.offset - (targetInfo?.offset ?: draggedInfo.offset)
                                editableSlots = editableSlots.toMutableList().apply {
                                    add(targetIndex, removeAt(fromIndex))
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            val edge = 72f
                            when {
                                draggedCenter < layout.viewportStartOffset + edge -> scope.launch { listState.scrollBy(-24f) }
                                draggedCenter > layout.viewportEndOffset - edge -> scope.launch { listState.scrollBy(24f) }
                            }
                        },
                        onDragEnd = { finishDrag(persist = true) },
                        onDragCancel = { finishDrag(persist = false) },
                    )
                }

                item(key = "reset") {
                    AppButton(
                        "Reset to default split",
                        onClick = {
                            scope.launch {
                                editMutex.withLock {
                                    runCatching { ServiceLocator.trainingPlanRepository.resetToDefault(userId) }
                                        .onSuccess { saveError = null; reload++ }
                                        .onFailure { saveError = "Couldn't reset your split." }
                                }
                            }
                        },
                        variant = ButtonVariant.GHOST,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    )
                }
            }
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
    onRename: (String) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var renameOpen by remember { mutableStateOf(false) }
    var draftLabel by remember(label) { mutableStateOf(label) }

    AppCard(modifier = modifier.fillMaxWidth(), padding = Spacing.md) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(number.toString().padStart(2, '0'), variant = TextVariant.CAPTION, tone = TextTone.TERTIARY, mono = true)
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!isRest) Modifier.clickable { menuOpen = true } else Modifier)
                        .padding(vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    AppText(
                        label + if (isCurrent) "  · TODAY" else "",
                        variant = TextVariant.LABEL,
                        tone = if (isCurrent) TextTone.ACCENT else TextTone.PRIMARY,
                        maxLines = 1,
                    )
                    AppText(
                        if (isRest) "Rest day" else mappedGateName ?: "No Gate mapped · Tap to set",
                        variant = TextVariant.CAPTION,
                        tone = TextTone.SECONDARY,
                        maxLines = 1,
                    )
                }
                if (!isRest) {
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
            IconButton(
                onClick = { draftLabel = label; renameOpen = true },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = "Rename $label", tint = Palette.TextSecondary, modifier = Modifier.size(20.dp))
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Hold and drag to reorder $label" }
                    .pointerInput(label) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_drag), contentDescription = null, tint = Palette.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { AppText("Rename training", variant = TextVariant.HEADING) },
            text = {
                AppTextField(
                    value = draftLabel,
                    onValueChange = { draftLabel = it },
                    placeholder = "Training name",
                    error = if (draftLabel.isBlank()) "Enter a name" else null,
                    imeAction = ImeAction.Done,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draftLabel.isNotBlank(),
                    onClick = {
                        onRename(draftLabel.trim())
                        renameOpen = false
                    },
                ) { Text("Save", color = Palette.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) { Text("Cancel", color = Palette.TextSecondary) }
            },
            containerColor = Palette.Surface1,
            titleContentColor = Palette.TextPrimary,
            textContentColor = Palette.TextPrimary,
        )
    }
}
