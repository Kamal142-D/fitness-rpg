package com.fitnessrpg.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fitnessrpg.app.data.repo.OnboardingOutcome
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.onboarding.ExperienceLevel
import com.fitnessrpg.app.domain.onboarding.FitnessGoal
import com.fitnessrpg.app.domain.onboarding.OnboardingDraft
import com.fitnessrpg.app.domain.onboarding.Sex
import com.fitnessrpg.app.domain.onboarding.TrainingLocation
import com.fitnessrpg.app.domain.onboarding.ageFromDob
import com.fitnessrpg.app.domain.onboarding.isValidDateString
import com.fitnessrpg.app.domain.rankings.BodyCompositionData
import com.fitnessrpg.app.domain.rankings.DumbbellWeightMode
import com.fitnessrpg.app.domain.rankings.Equipment
import com.fitnessrpg.app.domain.rankings.HunterRankResult
import com.fitnessrpg.app.domain.rankings.StrengthAssessmentInput
import com.fitnessrpg.app.domain.rankings.computeOnboardingHunterRank
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AppTextField
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ChoiceGroup
import com.fitnessrpg.app.ui.components.ChoiceOption
import com.fitnessrpg.app.ui.components.HunterRankPanel
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private val SEX_OPTIONS = listOf(
    ChoiceOption("Male", Sex.MALE), ChoiceOption("Female", Sex.FEMALE),
    ChoiceOption("Intersex", Sex.INTERSEX), ChoiceOption("Prefer not to say", Sex.PREFER_NOT_TO_SAY),
)
private val GOAL_OPTIONS = listOf(
    ChoiceOption("Build muscle", FitnessGoal.BUILD_MUSCLE),
    ChoiceOption("Lose fat", FitnessGoal.LOSE_FAT),
    ChoiceOption("Get stronger", FitnessGoal.GET_STRONGER),
    ChoiceOption("General fitness", FitnessGoal.GENERAL_FITNESS),
    ChoiceOption("Improve endurance", FitnessGoal.IMPROVE_ENDURANCE),
)
private val EXPERIENCE_OPTIONS = listOf(
    ChoiceOption("Beginner", ExperienceLevel.BEGINNER),
    ChoiceOption("Intermediate", ExperienceLevel.INTERMEDIATE),
    ChoiceOption("Advanced", ExperienceLevel.ADVANCED),
)
private val DAYS_OPTIONS = (1..7).map { ChoiceOption("$it ${if (it == 1) "day" else "days"} / week", it) }
private val EQUIPMENT_OPTIONS = listOf(
    ChoiceOption("Barbell", Equipment.BARBELL), ChoiceOption("Dumbbells", Equipment.DUMBBELL),
    ChoiceOption("Machine", Equipment.MACHINE), ChoiceOption("Smith machine", Equipment.SMITH_MACHINE),
    ChoiceOption("Other", Equipment.OTHER),
)
private val DUMBBELL_MODE_OPTIONS = listOf(
    ChoiceOption("Per dumbbell", DumbbellWeightMode.PER_HAND),
    ChoiceOption("Combined weight", DumbbellWeightMode.COMBINED),
)

private class Lift {
    var equipment by mutableStateOf(Equipment.BARBELL)
    var weight by mutableStateOf("")
    var reps by mutableStateOf("")
    var dumbbellMode by mutableStateOf(DumbbellWeightMode.PER_HAND)

    fun toInput(exerciseId: String): StrengthAssessmentInput? {
        val w = weight.toDoubleOrNull() ?: return null
        val r = reps.toIntOrNull() ?: return null
        if (w <= 0.0 || r < 1) return null
        return StrengthAssessmentInput(
            exerciseId, equipment, w, r,
            if (equipment == Equipment.DUMBBELL) dumbbellMode else null,
        )
    }
}

@Composable
fun OnboardingFlow(userId: String, onComplete: () -> Unit) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf<Sex?>(null) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf<FitnessGoal?>(null) }
    var experience by remember { mutableStateOf<ExperienceLevel?>(null) }
    var days by remember { mutableStateOf<Int?>(null) }
    var bodyFat by remember { mutableStateOf("") }
    var muscleMass by remember { mutableStateOf("") }
    var strengthSkipped by remember { mutableStateOf(false) }
    val bench = remember { Lift() }
    val squat = remember { Lift() }

    var showErrors by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<HunterRankResult?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    fun body() = BodyCompositionData(
        weightKg = weight.toDoubleOrNull() ?: 0.0,
        heightCm = height.toDoubleOrNull() ?: 0.0,
        bodyFatPercent = bodyFat.toDoubleOrNull(),
        muscleMassKg = muscleMass.toDoubleOrNull(),
        sex = sex?.wire,
    )

    fun strengthInputs(): List<StrengthAssessmentInput>? {
        if (strengthSkipped) return null
        return listOfNotNull(bench.toInput("bench"), squat.toInput("squat")).ifEmpty { null }
    }

    fun draft() = OnboardingDraft(
        displayName = name.trim(),
        dateOfBirth = dob.trim(),
        sex = sex,
        heightCm = height.toDoubleOrNull(),
        currentWeightKg = weight.toDoubleOrNull(),
        fitnessGoal = goal,
        experienceLevel = experience,
        trainingDaysPerWeek = days,
        trainingLocation = TrainingLocation.GYM,
        preferredWorkoutMinutes = 45,
        bodyFatPercent = bodyFat.toDoubleOrNull(),
        skeletalMuscleMassKg = null, // never store total muscle mass as SMM
    )

    fun valid(): Boolean {
        val h = height.toDoubleOrNull()
        val wt = weight.toDoubleOrNull()
        return name.isNotBlank() &&
            isValidDateString(dob) && ageFromDob(dob) in 13..100 &&
            sex != null &&
            h != null && h in 100.0..250.0 &&
            wt != null && wt in 30.0..300.0 &&
            goal != null && experience != null && (days ?: 0) in 1..7
    }

    val r = result
    if (r != null) {
        RevealScreen(r, submitting, submitError, onEnter = {
            submitting = true
            submitError = null
            scope.launch {
                when (val outcome = ServiceLocator.profileRepository.completeOnboarding(userId, draft(), r)) {
                    OnboardingOutcome.Ok -> onComplete()
                    is OnboardingOutcome.Error -> {
                        submitError = outcome.message
                        submitting = false
                    }
                }
            }
        }, onBack = { result = null })
        return
    }

    ScreenScaffold {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppText("THE AWAKENING", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText("Register as a Hunter", variant = TextVariant.DISPLAY)
        }

        AppCard {
            SectionTitle("Personal information")
            AppTextField(name, { name = it }, label = "Name", error = err(showErrors && name.isBlank(), "Enter a name"))
            AppTextField(dob, { dob = it }, label = "Date of birth (YYYY-MM-DD)", placeholder = "1998-05-20", error = err(showErrors && !(isValidDateString(dob) && ageFromDob(dob) in 13..100), "Enter a valid date, age 13–100"))
            AppText("Sex", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            ChoiceGroup(SEX_OPTIONS, sex, { sex = it })
            AppTextField(height, { height = it }, label = "Height (cm)", keyboardType = KeyboardType.Decimal, error = err(showErrors && (height.toDoubleOrNull() ?: 0.0) !in 100.0..250.0, "100–250 cm"))
            AppTextField(weight, { weight = it }, label = "Weight (kg)", keyboardType = KeyboardType.Decimal, error = err(showErrors && (weight.toDoubleOrNull() ?: 0.0) !in 30.0..300.0, "30–300 kg"))
        }

        AppCard {
            SectionTitle("Your goal")
            ChoiceGroup(GOAL_OPTIONS, goal, { goal = it })
            AppText("Experience", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            ChoiceGroup(EXPERIENCE_OPTIONS, experience, { experience = it })
            AppText("Training days", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            ChoiceGroup(DAYS_OPTIONS, days, { days = it })
        }

        AppCard {
            SectionTitle("Body assessment (optional)")
            AppText("From an InBody scan or scale, if you have it. Leave blank if unknown.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppTextField(bodyFat, { bodyFat = it }, label = "Body fat (%)", keyboardType = KeyboardType.Decimal)
            AppTextField(muscleMass, { muscleMass = it }, label = "Muscle mass (kg)", keyboardType = KeyboardType.Decimal)
        }

        AppCard {
            SectionTitle("Strength assessment")
            AppText("Use a recent working set that represents your current ability.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppButton(
                if (strengthSkipped) "Enter my lifts instead" else "I don't know my current lifts",
                onClick = { strengthSkipped = !strengthSkipped },
                variant = ButtonVariant.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!strengthSkipped) {
                LiftForm("Bench Press", bench)
                LiftForm("Squat", squat)
            } else {
                AppText("No problem — your rank will stay provisional until the System assesses your strength from your first Gates.", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
            }
        }

        AppCard {
            SectionTitle("Why do we ask this?")
            AppText(
                "Your Hunter Rank is based on multiple areas of fitness — not just body weight " +
                    "or composition. One strong attribute cannot fully compensate for a weak one. " +
                    "You can update these numbers anytime as you get stronger.",
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
            )
        }

        if (showErrors && !valid()) {
            AppText("Please complete the highlighted fields.", tone = TextTone.DANGER)
        }

        AppButton(
            "Awaken",
            onClick = {
                showErrors = true
                if (valid()) result = computeOnboardingHunterRank(body(), strengthInputs())
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LiftForm(title: String, lift: Lift) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        AppText(title, variant = TextVariant.HEADING)
        AppText("Equipment", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        ChoiceGroup(EQUIPMENT_OPTIONS, lift.equipment, { lift.equipment = it })
        if (lift.equipment == Equipment.DUMBBELL) {
            AppText("Weight entered as", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            ChoiceGroup(DUMBBELL_MODE_OPTIONS, lift.dumbbellMode, { lift.dumbbellMode = it })
        }
        AppTextField(lift.weight, { lift.weight = it }, label = "Weight (kg)", keyboardType = KeyboardType.Decimal)
        AppTextField(lift.reps, { lift.reps = it }, label = "Reps", keyboardType = KeyboardType.Number)
    }
}

@Composable
private fun SectionTitle(text: String) {
    AppText(text.uppercase(), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, modifier = Modifier.fillMaxWidth())
}

private fun err(condition: Boolean, message: String): String? = if (condition) message else null

@Composable
private fun RevealScreen(
    result: HunterRankResult,
    submitting: Boolean,
    error: String?,
    onEnter: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold {
        AppText("SYSTEM ANALYSIS COMPLETE", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppText(if (result.provisional) "Provisional Hunter Rank" else "Your Hunter Rank", variant = TextVariant.DISPLAY)

        HunterRankPanel(result, modifier = Modifier.fillMaxWidth())

        if (result.provisional) {
            AppText(
                "Your current rank is provisional. Complete more Gates and fitness assessments to reveal your true Hunter Rank.",
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
            )
        }

        error?.let { AppText(it, tone = TextTone.DANGER) }

        AppButton("Begin your first Gate", onClick = onEnter, modifier = Modifier.fillMaxWidth(), loading = submitting)
        AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST, modifier = Modifier.fillMaxWidth())
    }
}
