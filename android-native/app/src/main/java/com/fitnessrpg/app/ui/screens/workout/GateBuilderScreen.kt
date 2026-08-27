package com.fitnessrpg.app.ui.screens.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.domain.gates.CreateGateDraft
import com.fitnessrpg.app.domain.gates.ExerciseFilters
import com.fitnessrpg.app.domain.gates.searchExercises
import com.fitnessrpg.app.domain.gates.validateCreateGate
import com.fitnessrpg.app.domain.model.CreateGateInput
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AppTextField
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.components.SectionHeader
import com.fitnessrpg.app.ui.components.StatusPill
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun GateBuilderScreen(userId: String, templateId: String? = null, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val catalogResult by produceState<Result<List<Exercise>>?>(null) {
        value = runCatching { ServiceLocator.gateRepository.listExercises() }
    }
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    var equipment by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<List<String>>(emptyList()) }
    var guideExercise by remember { mutableStateOf<Exercise?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(templateId) {
        if (templateId != null) ServiceLocator.gateRepository.getGate(templateId)?.let { detail ->
            name = detail.template.name
            selected = detail.exercises.map { it.exercise.id }
        }
    }

    LaunchedEffect(query) { delay(250); debouncedQuery = query }
    val catalog = catalogResult?.getOrNull().orEmpty()
    val results = remember(catalog, debouncedQuery, muscle, equipment) {
        searchExercises(catalog, debouncedQuery, ExerciseFilters(muscle = muscle, equipment = equipment)).take(60)
    }

    ScreenScaffold {
        ScreenHeader(
            eyebrow = if (templateId == null) "New routine" else "Edit routine",
            title = if (templateId == null) "Create Gate" else "Edit Gate",
            subtitle = "Choose exercises, then tap one to review its form guide.",
            action = { AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST) },
        )
        AppTextField(name, { name = it }, label = "Routine name", placeholder = "Push Day")
        AppText("Difficulty is assessed after your first completed workout.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppTextField(query, { query = it }, label = "Add exercise", placeholder = "Search exercises…")

        SectionHeader("Filters")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(null, "chest", "back", "shoulders", "legs").forEach { value ->
                val active = muscle == value
                FilterChip(
                    selected = active,
                    onClick = { muscle = value },
                    label = { AppText(value?.replaceFirstChar { it.uppercase() } ?: "All", variant = TextVariant.LABEL, tone = if (active) TextTone.ACCENT else TextTone.SECONDARY) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Palette.Surface1, selectedContainerColor = Palette.PrimaryContainer),
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(null, "barbell", "dumbbell", "cable", "body weight").forEach { value ->
                val active = equipment == value
                FilterChip(
                    selected = active,
                    onClick = { equipment = value },
                    label = { AppText(value?.replaceFirstChar { it.uppercase() } ?: "Any", variant = TextVariant.LABEL, tone = if (active) TextTone.ACCENT else TextTone.SECONDARY) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Palette.Surface1, selectedContainerColor = Palette.PrimaryContainer),
                )
            }
        }

        SectionHeader("Exercise library", "${results.size} shown")
        if (selected.isNotEmpty()) StatusPill("${selected.size} selected", color = Palette.Success)
        when {
            catalogResult == null -> AppText("Loading exercise library…", tone = TextTone.SECONDARY)
            catalogResult?.isFailure == true -> AppText("Couldn't load exercise library.", tone = TextTone.DANGER)
            results.isEmpty() -> AppText("No exercises found.", tone = TextTone.SECONDARY)
            else -> results.forEach { exercise ->
                val added = exercise.id in selected
                AppCard(Modifier.fillMaxWidth().clickable {
                    guideExercise = exercise
                }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            AppText(exercise.name, variant = TextVariant.HEADING)
                            AppText(listOfNotNull(exercise.primaryMuscleGroup, exercise.equipment).joinToString(" · "), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                            if (exercise.secondaryMuscleGroups.isNotEmpty()) AppText("Secondary: ${exercise.secondaryMuscleGroups.take(3).joinToString(" · ")}", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
                            AppText("Tap to open form guide", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
                        }
                        AppButton(
                            label = if (added) "Added" else "Add",
                            onClick = {
                                selected = if (added) selected - exercise.id else (selected + exercise.id).distinct()
                            },
                            variant = if (added) ButtonVariant.SECONDARY else ButtonVariant.GHOST,
                        )
                    }
                }
            }
        }
        guideExercise?.let { exercise ->
            val added = exercise.id in selected
            ExerciseGuideDialog(
                exercise = exercise,
                isAdded = added,
                onToggleAdded = {
                    selected = if (added) selected - exercise.id else (selected + exercise.id).distinct()
                },
                onDismiss = { guideExercise = null },
            )
        }
        error?.let { AppText(it, tone = TextTone.DANGER) }
        AppButton("Save Gate", onClick = {
            val errors = validateCreateGate(CreateGateDraft(name, selected))
            if (errors.hasErrors()) { error = errors.name ?: errors.exercises; return@AppButton }
            saving = true
        }, enabled = !saving, loading = saving, modifier = Modifier.fillMaxWidth())
        if (saving) LaunchedEffect(name, selected) {
            runCatching {
                if (templateId == null) ServiceLocator.gateRepository.createGate(userId, CreateGateInput(name, selected))
                else { ServiceLocator.gateRepository.updateGate(templateId, CreateGateInput(name, selected)); templateId }
            }.onSuccess(onSaved).onFailure { error = friendlyDataError(it, "Couldn't save Gate."); saving = false }
        }
    }
}
