package com.fitnessrpg.app.ui.screens.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.fitnessrpg.app.R
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fitnessrpg.app.data.remote.friendlyDataError
import com.fitnessrpg.app.data.steps.DailyMarchOverview
import com.fitnessrpg.app.data.steps.HealthStepAvailability
import com.fitnessrpg.app.data.steps.LocalStepSnapshot
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.steps.DAILY_MARCH_REWARD_XP
import com.fitnessrpg.app.domain.steps.DEFAULT_DAILY_STEP_GOAL
import com.fitnessrpg.app.domain.steps.DailyStepProgress
import com.fitnessrpg.app.domain.steps.StepSource
import com.fitnessrpg.app.domain.steps.estimatedActiveMinutes
import com.fitnessrpg.app.domain.steps.estimatedDistanceKm
import com.fitnessrpg.app.domain.steps.stepGoalFraction
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.CardTone
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.StatChip
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.components.ScreenHeader
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private data class MarchLoad(
    val healthGranted: Boolean,
    val healthSteps: Int,
    val overview: DailyMarchOverview?,
    val error: String? = null,
)

@Composable
fun DailyMarchScreen(userId: String, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = ServiceLocator.stepRepository
    val today = LocalDate.now()
    val initialLocal = remember(today) { LocalStepSnapshot(today, 0, DEFAULT_DAILY_STEP_GOAL, null) }
    val local by repository.observeStoredSensorSteps(today).collectAsState(initial = initialLocal)
    val availability = remember { repository.healthAvailability() }
    val sensorAvailable = remember { repository.hasDeviceStepSensor() }
    val scope = rememberCoroutineScope()

    var reload by remember { mutableIntStateOf(0) }
    var load by remember { mutableStateOf(MarchLoad(false, 0, null)) }
    var liveSensorSteps by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    fun sensorPermissionGranted(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    val sensorPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        reload++
    }
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (repository.healthReadPermission in granted) reload++
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) reload++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(reload, local.goal, userId) {
        val granted = runCatching { repository.hasHealthPermission() }.getOrDefault(false)
        val healthRead = if (granted) runCatching { repository.readTodayFromHealthConnect(today) } else null
        val steps = healthRead?.getOrElse { local.sensorSteps } ?: local.sensorSteps
        load = runCatching { repository.syncAndLoad(today, steps, local.goal) }.fold(
            onSuccess = { MarchLoad(granted, steps, it) },
            onFailure = { MarchLoad(granted, steps, load.overview, friendlyDataError(it, "Daily March could not sync right now.")) },
        )
    }

    LaunchedEffect(load.healthGranted, sensorAvailable, reload) {
        if (!load.healthGranted && sensorAvailable && sensorPermissionGranted()) {
            repository.observeLiveSensorSteps()
                .catch { load = load.copy(error = "The device step counter stopped. Reopen Daily March to retry.") }
                .collect { liveSensorSteps = it }
        }
    }

    val source = when {
        load.healthGranted -> StepSource.HEALTH_CONNECT
        sensorAvailable && sensorPermissionGranted() -> StepSource.DEVICE_SENSOR
        else -> StepSource.NONE
    }
    val displayedSteps = max(max(load.healthSteps, local.sensorSteps), max(liveSensorSteps, load.overview?.today?.steps ?: 0))
    val goal = load.overview?.today?.goal ?: local.goal
    val rewardClaimed = load.overview?.today?.rewardClaimed == true
    val fraction = stepGoalFraction(displayedSteps, goal)

    ScreenScaffold {
        ScreenHeader(
            title = "Walk. Clear. Advance.",
            subtitle = "Every step fills today's March Gate.",
            action = {
                SourceBadge(source)
                if (onBack != null) AppButton("Back", onClick = onBack, variant = ButtonVariant.GHOST)
            },
        )

        AppCard(modifier = Modifier.fillMaxWidth(), padding = Spacing.xl) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                StepProgressRing(displayedSteps, goal, fraction)
                AppText(
                    when {
                        fraction >= 1f -> "MARCH GATE CLEARED"
                        source == StepSource.NONE -> "CONNECT A STEP SOURCE"
                        else -> "${(goal - displayedSteps).coerceAtLeast(0)} steps remaining"
                    },
                    variant = TextVariant.LABEL,
                    tone = if (fraction >= 1f) TextTone.SUCCESS else TextTone.SECONDARY,
                    mono = true,
                )
            }
        }

        if (source == StepSource.NONE) {
            StepConnectionCard(
                availability = availability,
                sensorAvailable = sensorAvailable,
                onConnectHealth = { healthPermissionLauncher.launch(setOf(repository.healthReadPermission)) },
                onEnableSensor = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) sensorPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    else reload++
                },
                onUpdateHealthConnect = {
                    val packageName = "com.google.android.apps.healthdata"
                    val uri = Uri.parse("market://details?id=$packageName&url=healthconnect%3A%2F%2Fonboarding")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending")) }
                },
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatChip("DISTANCE", String.format(Locale.US, "%.1f km", estimatedDistanceKm(displayedSteps)), Modifier.weight(1f))
            StatChip("ACTIVE", "${estimatedActiveMinutes(displayedSteps)} min", Modifier.weight(1f))
            StatChip("STREAK", "${load.overview?.streak ?: 0} d", Modifier.weight(1f))
        }

        AppCard {
            AppText("TODAY'S TARGET", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf(6_000, 8_000, 10_000, 12_000).forEach { option ->
                    FilterChip(
                        selected = goal == option,
                        onClick = {
                            scope.launch {
                                repository.setGoal(option)
                                reload++
                            }
                        },
                        enabled = !rewardClaimed,
                        label = { AppText("${option / 1_000}K", variant = TextVariant.CAPTION) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Palette.Primary.copy(alpha = .18f),
                            selectedLabelColor = Palette.Primary,
                        ),
                    )
                }
            }
            if (rewardClaimed) {
                AppText("Goal locked after reward claim.", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
            }
        }

        RewardCard(
            completed = displayedSteps >= goal,
            claimed = rewardClaimed,
            busy = busy,
            onClaim = {
                scope.launch {
                    busy = true
                    load = try {
                        val synced = repository.syncAndLoad(today, displayedSteps, goal)
                        val claimed = repository.claimReward(today)
                        MarchLoad(load.healthGranted, displayedSteps, synced.copy(today = claimed), null)
                    } catch (e: Exception) {
                        load.copy(error = friendlyDataError(e, "The Daily March reward could not be claimed."))
                    }
                    busy = false
                }
            },
        )

        WeeklyMarch(load.overview?.history.orEmpty(), today)

        load.error?.let {
            AppCard(tone = CardTone.FLAT) {
                AppText(it, tone = TextTone.DANGER)
                AppButton("Retry", onClick = { reload++ }, variant = ButtonVariant.GHOST, modifier = Modifier.padding(top = Spacing.sm))
            }
        }

        AppText(
            "Read-only step totals are used for this screen and its XP reward. Distance and active minutes are estimates. No location data is collected.",
            variant = TextVariant.CAPTION,
            tone = TextTone.TERTIARY,
        )
    }
}

@Composable
private fun StepProgressRing(steps: Int, goal: Int, fraction: Float) {
    val animated by animateFloatAsState(fraction, label = "daily-march-progress")
    Box(Modifier.size(224.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(224.dp)) {
            val stroke = 15.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Palette.Surface3, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (animated > 0f) {
                drawArc(Palette.Primary, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.ic_march), contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(28.dp))
            AppText("%,d".format(steps), variant = TextVariant.HERO, mono = true)
            AppText("of %,d steps".format(goal), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY, mono = true)
        }
    }
}

@Composable
private fun SourceBadge(source: StepSource) {
    val (label, color) = when (source) {
        StepSource.HEALTH_CONNECT -> "HEALTH CONNECT" to Palette.Success
        StepSource.DEVICE_SENSOR -> "DEVICE SENSOR" to Palette.Primary
        StepSource.NONE -> "OFFLINE" to Palette.TextTertiary
    }
    Box(
        Modifier.background(color.copy(alpha = .12f), RoundedCornerShape(Radius.pill))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        AppText(label, variant = TextVariant.CAPTION, color = color, mono = true)
    }
}

@Composable
private fun StepConnectionCard(
    availability: HealthStepAvailability,
    sensorAvailable: Boolean,
    onConnectHealth: () -> Unit,
    onEnableSensor: () -> Unit,
    onUpdateHealthConnect: () -> Unit,
) {
    AppCard {
        AppText("Awaken Daily March", variant = TextVariant.TITLE)
        AppText(
            "Connect read-only steps for accurate daily totals. Fitness RPG never requests routes or location.",
            tone = TextTone.SECONDARY,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Column(Modifier.padding(top = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            when (availability) {
                HealthStepAvailability.AVAILABLE -> AppButton("Connect Health Connect", onConnectHealth, modifier = Modifier.fillMaxWidth())
                HealthStepAvailability.UPDATE_REQUIRED -> AppButton("Update Health Connect", onUpdateHealthConnect, modifier = Modifier.fillMaxWidth())
                HealthStepAvailability.UNAVAILABLE -> Unit
            }
            if (sensorAvailable) {
                AppButton("Use device step counter", onEnableSensor, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
            }
            if (availability == HealthStepAvailability.UNAVAILABLE && !sensorAvailable) {
                AppText("This device does not expose Health Connect or a step-counter sensor.", tone = TextTone.SECONDARY)
            }
        }
    }
}

@Composable
private fun RewardCard(completed: Boolean, claimed: Boolean, busy: Boolean, onClaim: () -> Unit) {
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AppText("DAILY CLEAR REWARD", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                AppText("+$DAILY_MARCH_REWARD_XP Level XP", variant = TextVariant.TITLE, tone = if (completed) TextTone.SUCCESS else TextTone.PRIMARY)
                AppText(
                    when {
                        claimed -> "Claimed for today"
                        completed -> "Goal complete. Reward unlocked."
                        else -> "Complete today's step target to unlock."
                    },
                    variant = TextVariant.CAPTION,
                    tone = TextTone.SECONDARY,
                )
            }
        }
        AppButton(
            label = if (claimed) "Reward claimed" else "Claim reward",
            onClick = onClaim,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            enabled = completed && !claimed,
            loading = busy,
        )
    }
}

@Composable
private fun WeeklyMarch(history: List<DailyStepProgress>, today: LocalDate) {
    val byDate = history.associateBy { it.date }
    AppCard {
        AppText("LAST 7 DAYS", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            (6L downTo 0L).forEach { offset ->
                val date = today.minusDays(offset)
                val day = byDate[date]
                val fraction = day?.fraction ?: 0f
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(72.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth(.62f)
                                .height(max(5f, 72f * fraction).dp)
                                .background(if (fraction >= 1f) Palette.Success else Palette.Primary.copy(alpha = .45f), RoundedCornerShape(Radius.pill)),
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    AppText(
                        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        variant = TextVariant.CAPTION,
                        tone = if (date == today) TextTone.ACCENT else TextTone.TERTIARY,
                    )
                }
            }
        }
    }
}
