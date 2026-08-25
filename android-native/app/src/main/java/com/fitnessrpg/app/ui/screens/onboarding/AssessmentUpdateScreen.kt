package com.fitnessrpg.app.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.data.repo.OnboardingOutcome
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.rankings.BodyAssessmentSource
import com.fitnessrpg.app.domain.rankings.BodyCompositionData
import com.fitnessrpg.app.domain.rankings.ConditioningInput
import com.fitnessrpg.app.domain.rankings.ConditioningTestType
import com.fitnessrpg.app.domain.rankings.DumbbellWeightMode
import com.fitnessrpg.app.domain.rankings.Equipment
import com.fitnessrpg.app.domain.rankings.StrengthAssessmentInput
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AppTextField
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.ChoiceGroup
import com.fitnessrpg.app.ui.components.ChoiceOption
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.launch

private val CONDITIONING_TESTS = listOf(
    ChoiceOption("12-minute run", ConditioningTestType.COOPER_12_MINUTE),
    ChoiceOption("1.5-mile run", ConditioningTestType.RUN_1_5_MILE),
    ChoiceOption("3-minute step", ConditioningTestType.STEP_3_MINUTE),
)
private val ASSESSMENT_EQUIPMENT = listOf(
    ChoiceOption("Barbell", Equipment.BARBELL),
    ChoiceOption("Dumbbells", Equipment.DUMBBELL),
    ChoiceOption("Bodyweight", Equipment.BODYWEIGHT),
)

private class AssessmentLift(val exercise: String, val variation: String) {
    var equipment by mutableStateOf(if (exercise == "pullup") Equipment.BODYWEIGHT else Equipment.BARBELL)
    var weight by mutableStateOf("")
    var reps by mutableStateOf("")
    var perHand by mutableStateOf(true)
    fun input(): StrengthAssessmentInput? {
        val w = weight.toDoubleOrNull() ?: return null
        val r = reps.toIntOrNull() ?: return null
        if (w < 0.0 || r !in 1..50) return null
        return StrengthAssessmentInput(
            exercise, equipment, w, r,
            if (equipment == Equipment.DUMBBELL) if (perHand) DumbbellWeightMode.PER_HAND else DumbbellWeightMode.COMBINED else null,
            variation,
        )
    }
}

@Composable
fun AssessmentUpdateScreen(userId: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val load by produceState<Result<com.fitnessrpg.app.data.repo.RankAssessmentSnapshot>?>(null, userId) {
        value = runCatching { ServiceLocator.assessmentRepository.getRankAssessment(userId) }
    }
    val scope = rememberCoroutineScope()
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var smm by remember { mutableStateOf("") }
    var conditioningType by remember { mutableStateOf(ConditioningTestType.COOPER_12_MINUTE) }
    var conditioningResult by remember { mutableStateOf("") }
    val bench = remember { AssessmentLift("bench", "flat") }
    val squat = remember { AssessmentLift("squat", "back") }
    val deadlift = remember { AssessmentLift("deadlift", "conventional") }
    var initialized by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val snapshot = load?.getOrNull()
    LaunchedEffect(snapshot) {
        if (!initialized && snapshot != null) {
            height = snapshot.profile?.heightCm?.toString().orEmpty()
            weight = (snapshot.latestBody?.weightKg ?: snapshot.profile?.currentWeightKg)?.toString().orEmpty()
            bodyFat = snapshot.latestBody?.bodyFatPercent?.toString().orEmpty()
            waist = snapshot.latestBody?.waistCm?.toString().orEmpty()
            smm = snapshot.latestBody?.skeletalMuscleMassKg?.toString().orEmpty()
            initialized = true
        }
    }

    ScreenScaffold {
        AppText("ASSESSMENT UPDATE", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppText("Calibrate your Rank", variant = TextVariant.DISPLAY)
        when {
            load == null -> AppText("Loading your assessments…", tone = TextTone.SECONDARY)
            load?.isFailure == true -> {
                AppCard { AppText(friendlyDataError(load?.exceptionOrNull(), "Couldn't load your assessments."), tone = TextTone.DANGER) }
                AppButton("Back", onBack, modifier = Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST)
            }
            snapshot != null -> {
                AppCard {
                    AppText("Current ${snapshot.hunter.rank.name}-Rank${if (snapshot.hunter.provisional) " · Provisional" else ""}", variant = TextVariant.TITLE)
                    snapshot.hunter.reasons.take(3).forEach { AppText(it, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY) }
                }

                if (snapshot.physique.provisional || snapshot.physique.stale) {
                    AppCard {
                        AppText("BODY ASSESSMENT", variant = TextVariant.HEADING)
                        AppText("Add only measurements you actually know. Generic muscle mass is not accepted as skeletal muscle mass.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                        AppTextField(height, { height = it }, label = "Height (cm)", keyboardType = KeyboardType.Decimal)
                        AppTextField(weight, { weight = it }, label = "Weight (kg)", keyboardType = KeyboardType.Decimal)
                        AppTextField(bodyFat, { bodyFat = it }, label = "Body fat (%) — optional", keyboardType = KeyboardType.Decimal)
                        AppTextField(waist, { waist = it }, label = "Waist (cm) — optional", keyboardType = KeyboardType.Decimal)
                        AppTextField(smm, { smm = it }, label = "Skeletal muscle mass (kg) — optional", keyboardType = KeyboardType.Decimal)
                    }
                }

                if (snapshot.strength.provisional) {
                    AppCard {
                        AppText("STRENGTH ASSESSMENT", variant = TextVariant.HEADING)
                        AppText("Enter recent working sets. Three movement patterns are needed for A/S confidence.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                        AssessmentLiftForm("Bench Press", bench)
                        AssessmentLiftForm("Back Squat", squat)
                        AssessmentLiftForm("Conventional Deadlift", deadlift)
                    }
                }

                if (snapshot.conditioning.provisional || snapshot.conditioning.score == null) {
                    AppCard {
                        AppText("CONDITIONING", variant = TextVariant.HEADING)
                        AppText("Choose one standardized test, or leave the result blank to skip.", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                        ChoiceGroup(CONDITIONING_TESTS, conditioningType, { conditioningType = it })
                        val label = when (conditioningType) {
                            ConditioningTestType.COOPER_12_MINUTE -> "Distance in 12 minutes (metres)"
                            ConditioningTestType.RUN_1_5_MILE -> "1.5-mile time (minutes, e.g. 12.5)"
                            ConditioningTestType.STEP_3_MINUTE -> "Recovery heart rate (bpm)"
                        }
                        AppTextField(conditioningResult, { conditioningResult = it }, label = label, keyboardType = KeyboardType.Decimal)
                    }
                }

                error?.let { AppText(it, tone = TextTone.DANGER) }
                AppButton("Save assessment", onClick = {
                    val needsBody = snapshot.physique.provisional || snapshot.physique.stale
                    val h = height.toDoubleOrNull()
                    val w = weight.toDoubleOrNull()
                    if (needsBody && (h == null || h !in 100.0..250.0 || w == null || w !in 30.0..300.0)) {
                        error = "Enter a valid height and weight."
                        return@AppButton
                    }
                    val body = if (needsBody) BodyCompositionData(
                        weightKg = w!!, heightCm = h!!, bodyFatPercent = bodyFat.toDoubleOrNull(),
                        skeletalMuscleMassKg = smm.toDoubleOrNull(), waistCm = waist.toDoubleOrNull(),
                        source = BodyAssessmentSource.MANUAL, sex = snapshot.profile?.sex,
                    ) else null
                    val lifts = if (snapshot.strength.provisional) listOfNotNull(bench.input(), squat.input(), deadlift.input()) else emptyList()
                    val conditioning = if (snapshot.conditioning.provisional || snapshot.conditioning.score == null) {
                        conditioningResult.toDoubleOrNull()?.let { ConditioningInput(conditioningType, it, sex = snapshot.profile?.sex) }
                    } else null
                    saving = true
                    error = null
                    scope.launch {
                        when (val outcome = ServiceLocator.profileRepository.completeAssessment(userId, body, lifts, conditioning)) {
                            OnboardingOutcome.Ok -> onSaved()
                            is OnboardingOutcome.Error -> { error = outcome.message; saving = false }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth(), loading = saving)
                AppButton("Back", onBack, modifier = Modifier.fillMaxWidth(), variant = ButtonVariant.GHOST)
            }
        }
    }
}

@Composable
private fun AssessmentLiftForm(title: String, lift: AssessmentLift) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AppText(title, variant = TextVariant.LABEL)
        ChoiceGroup(ASSESSMENT_EQUIPMENT, lift.equipment, { lift.equipment = it })
        if (lift.equipment == Equipment.DUMBBELL) {
            ChoiceGroup(listOf(ChoiceOption("Per hand", true), ChoiceOption("Combined", false)), lift.perHand, { lift.perHand = it })
        }
        AppTextField(lift.weight, { lift.weight = it }, label = if (lift.equipment == Equipment.BODYWEIGHT) "Added weight (kg; 0 allowed)" else "Weight (kg)", keyboardType = KeyboardType.Decimal)
        AppTextField(lift.reps, { lift.reps = it }, label = "Reps", keyboardType = KeyboardType.Number)
    }
}
