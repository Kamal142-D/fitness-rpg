package com.fitnessrpg.app.ui.screens.workout

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fitnessrpg.app.R
import com.fitnessrpg.app.domain.model.Exercise
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface GuideImageState {
    data object Loading : GuideImageState
    data object Unavailable : GuideImageState
    data class Ready(
        val frames: List<ImageBitmap>,
        val usesWorkoutGuide: Boolean,
    ) : GuideImageState
}

private val guideImageCache = LruCache<String, ImageBitmap>(24)

@Composable
fun ExerciseGuideDialog(
    exercise: Exercise,
    isAdded: Boolean,
    onToggleAdded: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val guideEntry = remember(exercise.name, exercise.equipment) {
        findWorkoutGuideEntry(
            exerciseName = exercise.name,
            equipment = exercise.equipment,
            catalog = WorkoutGuideCatalog.load(context.applicationContext),
        )
    }
    val guideUrls = remember(guideEntry?.slug) {
        guideEntry?.slug?.let(::workoutGuideFrameUrls).orEmpty()
    }
    val imageState by produceState<GuideImageState>(
        initialValue = GuideImageState.Loading,
        key1 = guideUrls,
        key2 = exercise.imageUrl,
    ) {
        value = loadGuideImages(guideUrls, exercise.imageUrl)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(Spacing.md)
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .heightIn(max = 860.dp),
            shape = RoundedCornerShape(Radius.xl),
            color = Palette.Surface1,
        ) {
            Column(Modifier.padding(Spacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        AppText("HOW TO PERFORM", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
                        AppText(exercise.name, variant = TextVariant.TITLE)
                    }
                    AppButton("Close", onClick = onDismiss, variant = ButtonVariant.GHOST)
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    GuideImage(imageState, exercise.name)

                    val details = listOfNotNull(
                        exercise.primaryMuscleGroup?.takeIf { it.isNotBlank() }?.let { "Target: ${it.replaceFirstChar(Char::uppercase)}" },
                        exercise.equipment?.takeIf { it.isNotBlank() }?.let { "Equipment: ${it.replaceFirstChar(Char::uppercase)}" },
                    )
                    if (details.isNotEmpty()) {
                        AppText(details.joinToString("  ·  "), variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
                    }

                    AppText("STEPS", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
                    if (exercise.instructions.isEmpty()) {
                        AppText(
                            "Written steps are not available for this movement yet. Follow the demonstration image and ask a qualified trainer if you are unsure about your form.",
                            tone = TextTone.SECONDARY,
                        )
                    } else {
                        exercise.instructions.forEachIndexed { index, instruction ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Radius.pill))
                                        .background(Palette.Primary)
                                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                                ) {
                                    AppText("${index + 1}", variant = TextVariant.CAPTION, color = Palette.Background)
                                }
                                AppText(instruction, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    val usesWorkoutGuide = (imageState as? GuideImageState.Ready)?.usesWorkoutGuide == true
                    if (usesWorkoutGuide) {
                        WorkoutGuideAttribution()
                    } else {
                        exercise.attribution?.takeIf { it.isNotBlank() }?.let {
                            AppText("Movement media: $it", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
                        }
                    }
                    AppText(
                        "Use a comfortable range of motion and stop if you feel sharp pain.",
                        variant = TextVariant.CAPTION,
                        tone = TextTone.TERTIARY,
                    )
                }

                AppButton(
                    label = if (isAdded) "Remove from Gate" else "Add to Gate",
                    onClick = onToggleAdded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                    variant = if (isAdded) ButtonVariant.SECONDARY else ButtonVariant.PRIMARY,
                )
            }
        }
    }
}

@Composable
private fun GuideImage(state: GuideImageState, exerciseName: String) {
    val ready = state as? GuideImageState.Ready
    var frameIndex by remember(ready) { mutableIntStateOf(0) }

    LaunchedEffect(ready) {
        val frameCount = ready?.frames?.size ?: 0
        if (frameCount > 1) {
            while (true) {
                delay(900)
                frameIndex = (frameIndex + 1) % frameCount
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .heightIn(max = 420.dp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(Palette.Background),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is GuideImageState.Ready -> Image(
                bitmap = state.frames[frameIndex.coerceIn(state.frames.indices)],
                contentDescription = "$exerciseName demonstration, frame ${frameIndex + 1} of ${state.frames.size}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (state.frames.size > 1) frameIndex = (frameIndex + 1) % state.frames.size
                    },
                contentScale = ContentScale.Fit,
            )
            GuideImageState.Loading -> {
                Image(
                    painter = painterResource(R.drawable.exercise_guide_fallback),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                    alpha = 0.22f,
                )
                CircularProgressIndicator(color = Palette.Primary)
            }
            GuideImageState.Unavailable -> Image(
                painter = painterResource(R.drawable.exercise_guide_fallback),
                contentDescription = "Exercise guide illustration",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }

        if (ready != null && ready.frames.size > 1) {
            AppText(
                "FRAME ${frameIndex + 1} / ${ready.frames.size}  ·  TAP TO ADVANCE",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.sm),
                variant = TextVariant.CAPTION,
                tone = TextTone.SECONDARY,
            )
        }
    }
}

@Composable
private fun WorkoutGuideAttribution() {
    val uriHandler = LocalUriHandler.current
    AppText(
        "Illustrations: Bryl Lim / Everkinetic · CC BY-SA 4.0",
        modifier = Modifier.clickable { uriHandler.openUri("https://github.com/bryllim/workout-guide") },
        variant = TextVariant.CAPTION,
        tone = TextTone.TERTIARY,
    )
    AppText(
        "View artwork license",
        modifier = Modifier.clickable { uriHandler.openUri("https://creativecommons.org/licenses/by-sa/4.0/") },
        variant = TextVariant.CAPTION,
        tone = TextTone.ACCENT,
    )
}

private suspend fun loadGuideImages(
    workoutGuideUrls: List<String>,
    fallbackUrl: String?,
): GuideImageState {
    if (workoutGuideUrls.isNotEmpty()) {
        val frames = coroutineScope {
            workoutGuideUrls.map { url -> async { loadGuideImage(url) } }.awaitAll().filterNotNull()
        }
        if (frames.isNotEmpty()) return GuideImageState.Ready(frames, usesWorkoutGuide = true)
    }

    val fallback = fallbackUrl?.takeIf(String::isNotBlank)?.let { loadGuideImage(it) }
    return if (fallback != null) {
        GuideImageState.Ready(listOf(fallback), usesWorkoutGuide = false)
    } else {
        GuideImageState.Unavailable
    }
}

private suspend fun loadGuideImage(rawUrl: String): ImageBitmap? = withContext(Dispatchers.IO) {
    synchronized(guideImageCache) { guideImageCache.get(rawUrl) }?.let { return@withContext it }
    runCatching {
        require(rawUrl.startsWith("https://")) { "Only HTTPS exercise media is supported" }
        val connection = URL(rawUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "FitnessRPG-Android")
            if (connection.responseCode !in 200..299) error("Exercise image request failed")
            val bitmap = connection.inputStream.use(BitmapFactory::decodeStream)
                ?: error("Exercise image could not be decoded")
            bitmap.asImageBitmap().also { image ->
                synchronized(guideImageCache) { guideImageCache.put(rawUrl, image) }
            }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
