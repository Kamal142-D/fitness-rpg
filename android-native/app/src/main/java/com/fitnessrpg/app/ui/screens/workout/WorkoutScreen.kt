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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitnessrpg.app.data.workout.FinishWorkoutUseCase
import com.fitnessrpg.app.data.workout.WorkoutResultHolder
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.data.remote.friendlyDataError
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
import com.fitnessrpg.app.ui.components.AppProgressBar
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text

private fun Double?.toField(): String =
    this?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""

/** Keep only digits and a single decimal separator (normalising ',' to '.'). */
private fun cleanDecimal(input: String): String {
    val sb = StringBuilder()
    var dotSeen = false
    for (ch in input) {
        when {
            ch.isDigit() -> sb.append(ch)
            (ch == '.' || ch == ',') && !dotSeen -> { dotSeen = true; sb.append('.') }
        }
    }
    return sb.toString()
}

/**
 * A decimal input that preserves exactly what the user types (so "7." and "7.5"
 * are not collapsed back to "7" by round-tripping through a Double). The parsed
 * value is pushed out via [onValueChange]; the raw text is only re-synced from
 * [value] on a genuine external change (e.g. a pre-filled set).
 */
@Composable
private fun DecimalField(value: Double?, onValueChange: (Double?) -> Unit, placeholder: String) {
    var text by remember { mutableStateOf(value.toField()) }
    LaunchedEffect(value) {
        if (text.toDoubleOrNull() != value) text = value.toField()
    }
    AppTextField(
        value = text,
        onValueChange = { raw ->
            val cleaned = cleanDecimal(raw)
            text = cleaned
            onValueChange(cleaned.toDoubleOrNull())
        },
        placeholder = placeholder,
        keyboardType = KeyboardType.Decimal,
    )
}

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
        ScreenHeader(
            eyebrow = "Gate in progress",
            title = w.name,
            subtitle = "Log each working set as you complete it.",
            action = { AppButton("Cancel", onClick = {
                store.clear()
                onCancel()
            }, variant = ButtonVariant.GHOST) },
        )

        val totalSets = w.exercises.sumOf { it.sets.count { set -> !set.isWarmup } }
        val completedSets = w.exercises.sumOf { it.sets.count { set -> !set.isWarmup && set.isCompleted } }
        SectionHeader("Workout progress", "$completedSets / $totalSets sets")
        AppProgressBar(if (totalSets == 0) 0f else completedSets.toFloat() / totalSets)

        AnimatedVisibility(visible = restRemaining > 0, enter = fadeIn(), exit = fadeOut()) {
            AppCard {
                StatusPill("Rest timer")
                AppText(formatClock(restRemaining), variant = TextVariant.DISPLAY, tone = TextTone.ACCENT, mono = true)
                AppText("Recover, breathe, and prepare for the next set.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            }
        }

        error?.let { AppText(it, tone = TextTone.DANGER) }

        SectionHeader("Exercises", "${w.exercises.size} movements")
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
                            error = friendlyDataError(it, "Couldn't save your workout.")
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText(ex.name, variant = TextVariant.HEADING)
                AppText(formatTargets(ex.targetSets, ex.targetRepsMin, ex.targetRepsMax), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
            }
            if (ex.sets.any { it.isCompleted }) StatusPill("Logging", color = Palette.Success)
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppText("SET ${set.setNumber}", variant = TextVariant.CAPTION, tone = if (set.isWarmup) TextTone.TERTIARY else TextTone.SECONDARY, mono = true)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                AppButton(
                    if (set.isWarmup) "Warm-up" else "Working",
                    onClick = { store.update { toggleWarmup(it, exIdx, setIdx) } },
                    variant = ButtonVariant.GHOST,
                )
                AppButton(
                    if (set.isCompleted) "Undo" else "Complete",
                    onClick = { store.update { if (set.isCompleted) uncompleteSet(it, exIdx, setIdx) else completeSet(it, exIdx, setIdx) } },
                    variant = if (set.isCompleted) ButtonVariant.SECONDARY else ButtonVariant.PRIMARY,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("WEIGHT (KG)", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
                DecimalField(
                    value = set.weightKg,
                    onValueChange = { store.update { s -> setWeight(s, exIdx, setIdx, it) } },
                    placeholder = "0",
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AppText("REPS", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
                AppTextField(
                    value = set.reps?.toString() ?: "",
                    onValueChange = { store.update { s -> setReps(s, exIdx, setIdx, it.toIntOrNull()) } },
                    placeholder = "0",
                    keyboardType = KeyboardType.Number,
                )
            }
        }
        if (set.isCompleted) AppText("Logged · rest timer started", variant = TextVariant.CAPTION, tone = TextTone.SUCCESS)
    }
}
