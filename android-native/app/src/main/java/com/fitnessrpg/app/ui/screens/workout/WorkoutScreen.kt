package com.fitnessrpg.app.ui.screens.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitnessrpg.app.data.workout.FinishWorkoutUseCase
import com.fitnessrpg.app.data.workout.WorkoutResultHolder
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.gates.formatTargets
import com.fitnessrpg.app.domain.gates.searchExercises
import com.fitnessrpg.app.domain.model.ActiveExercise
import com.fitnessrpg.app.domain.model.ActiveSet
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.domain.workouts.addExercise
import com.fitnessrpg.app.domain.workouts.replaceExercise
import com.fitnessrpg.app.domain.workouts.addSet
import com.fitnessrpg.app.domain.workouts.buildCompletionPayload
import com.fitnessrpg.app.domain.workouts.completeSet
import com.fitnessrpg.app.domain.workouts.formatClock
import com.fitnessrpg.app.domain.workouts.removeSet
import com.fitnessrpg.app.domain.workouts.restRemainingSeconds
import com.fitnessrpg.app.domain.workouts.setReps
import com.fitnessrpg.app.domain.workouts.setWeight
import com.fitnessrpg.app.domain.workouts.toggleWarmup
import com.fitnessrpg.app.domain.workouts.uncompleteSet
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AppTextField
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text

private fun Double?.toField(): String =
    this?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""

@Composable
fun WorkoutScreen(userId: String, onFinished: () -> Unit, onCancel: () -> Unit) {
    val store = ServiceLocator.activeWorkoutStore
    val workout by store.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickerIndex by remember { mutableStateOf<Int?>(null) }
    val exerciseCatalog by produceState<List<Exercise>>(emptyList()) {
        value = runCatching { ServiceLocator.gateRepository.listExercises() }.getOrDefault(emptyList())
    }

    val w = workout
    if (w == null) {
        ScreenScaffold {
            AppText("No active workout.", tone = TextTone.SECONDARY)
            AppButton("Back", onClick = onCancel, variant = ButtonVariant.SECONDARY)
        }
        return
    }

    // Ticking clock for the rest timer.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(w.restEndsAt) {
        while (w.restEndsAt != null) {
            now = System.currentTimeMillis()
            if (restRemainingSeconds(w.restEndsAt, now) <= 0) break
            delay(1000)
        }
    }
    val restRemaining = restRemainingSeconds(w.restEndsAt, now)

    ScreenScaffold {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                AppText("GATE IN PROGRESS", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText(w.name, variant = TextVariant.TITLE)
            }
            AppButton("Cancel", onClick = {
                store.clear()
                onCancel()
            }, variant = ButtonVariant.GHOST)
        }

        if (restRemaining > 0) {
            AppCard { AppText("Rest — ${formatClock(restRemaining)}", variant = TextVariant.HEADING, tone = TextTone.ACCENT) }
        }

        error?.let { AppText(it, tone = TextTone.DANGER) }

        w.exercises.forEachIndexed { exIdx, ex ->
            ExerciseCard(ex, exIdx, store, onReplace = { pickerIndex = exIdx })
        }

        AppButton("Add exercise", onClick = { pickerIndex = -1 }, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())

        AppButton(
            "Finish workout",
            onClick = {
                if (submitting) return@AppButton
                submitting = true
                error = null
                scope.launch {
                    val result = buildCompletionPayload(w)
                    if (result.aggregates.completedSets == 0) {
                        error = "Complete at least one working set first."
                        submitting = false
                        return@launch
                    }
                    runCatching { FinishWorkoutUseCase().finish(userId, result.payload, result.aggregates) }
                        .onSuccess {
                            WorkoutResultHolder.last = it
                            store.clear()
                            onFinished()
                        }
                        .onFailure {
                            error = it.message ?: "Couldn't save your workout."
                            submitting = false
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            loading = submitting,
        )

        pickerIndex?.let { index ->
            ExercisePickerDialog(exerciseCatalog, onDismiss = { pickerIndex = null }) { exercise ->
                store.update { state -> if (index < 0) addExercise(state, exercise) else replaceExercise(state, index, exercise) }
                pickerIndex = null
            }
        }
    }
}

@Composable
private fun ExerciseCard(ex: ActiveExercise, exIdx: Int, store: com.fitnessrpg.app.data.local.ActiveWorkoutStore, onReplace: () -> Unit) {
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppText(ex.name, variant = TextVariant.HEADING, modifier = Modifier.weight(1f))
            AppText(formatTargets(ex.targetSets, ex.targetRepsMin, ex.targetRepsMax), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
            AppButton("Replace", onClick = onReplace, variant = ButtonVariant.GHOST)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ex.sets.forEachIndexed { setIdx, set ->
                SetRow(set, exIdx, setIdx, store)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppButton("Add set", onClick = { store.update { addSet(it, exIdx) } }, variant = ButtonVariant.GHOST)
            AppButton("Remove", onClick = { store.update { removeSet(it, exIdx, ex.sets.size - 1) } }, variant = ButtonVariant.GHOST)
        }
    }
}

@Composable
private fun ExercisePickerDialog(catalog: List<Exercise>, onDismiss: () -> Unit, onSelected: (Exercise) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(catalog, query) { searchExercises(catalog, query).take(30) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppTextField(query, { query = it }, placeholder = "Search exercises…")
                results.forEach { exercise ->
                    AppCard(Modifier.fillMaxWidth().clickable { onSelected(exercise) }) {
                        AppText(exercise.name, variant = TextVariant.LABEL)
                        AppText(listOfNotNull(exercise.primaryMuscleGroup, exercise.equipment).joinToString(" · "), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    }
                }
                if (results.isEmpty()) AppText("No exercises found.", tone = TextTone.SECONDARY)
            }
        },
        confirmButton = {},
        dismissButton = { AppButton("Cancel", onClick = onDismiss, variant = ButtonVariant.GHOST) },
    )
}

@Composable
private fun SetRow(set: ActiveSet, exIdx: Int, setIdx: Int, store: com.fitnessrpg.app.data.local.ActiveWorkoutStore) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        AppText("${set.setNumber}", variant = TextVariant.LABEL, tone = if (set.isWarmup) TextTone.TERTIARY else TextTone.SECONDARY, modifier = Modifier.width(20.dp))
        Box(Modifier.weight(1f)) {
            AppTextField(
                value = set.weightKg.toField(),
                onValueChange = { store.update { s -> setWeight(s, exIdx, setIdx, it.toDoubleOrNull()) } },
                placeholder = "kg",
                keyboardType = KeyboardType.Decimal,
            )
        }
        Box(Modifier.weight(1f)) {
            AppTextField(
                value = set.reps?.toString() ?: "",
                onValueChange = { store.update { s -> setReps(s, exIdx, setIdx, it.toIntOrNull()) } },
                placeholder = "reps",
                keyboardType = KeyboardType.Number,
            )
        }
        AppButton(
            if (set.isWarmup) "W" else "•",
            onClick = { store.update { toggleWarmup(it, exIdx, setIdx) } },
            variant = ButtonVariant.GHOST,
        )
        AppButton(
            if (set.isCompleted) "Undo" else "Done",
            onClick = {
                store.update { if (set.isCompleted) uncompleteSet(it, exIdx, setIdx) else completeSet(it, exIdx, setIdx) }
            },
            variant = if (set.isCompleted) ButtonVariant.SECONDARY else ButtonVariant.PRIMARY,
        )
    }
    if (set.isCompleted) {
        AppText("logged", variant = TextVariant.CAPTION, tone = TextTone.SUCCESS, modifier = Modifier.padding(start = 24.dp))
    }
}
