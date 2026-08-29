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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
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
    // Prefer the exercise's own media (a gymvisual animated GIF) so it plays; fall
    // back to a workout-guide still only when no media URL exists.
    val usesWorkoutGuide = exercise.imageUrl.isNullOrBlank() && guideUrls.isNotEmpty()
    val mediaUrl = exercise.imageUrl?.takeIf { it.isNotBlank() } ?: guideUrls.firstOrNull()

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
                    GuideImage(mediaUrl, exercise.name)

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
private fun GuideImage(mediaUrl: String?, exerciseName: String) {
    val context = LocalContext.current
    // Coil ImageLoader with GIF support so gymvisual animations actually play.
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // A definite, generous height so the demonstration is large; the media
            // scales to fill it. gymvisual art sits on white, so a white ground
            // avoids dark bars around it.
            .height(340.dp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(if (mediaUrl.isNullOrBlank()) Palette.Background else Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (mediaUrl.isNullOrBlank()) {
            Image(
                painter = painterResource(R.drawable.exercise_guide_fallback),
                contentDescription = "Exercise guide illustration",
                modifier = Modifier.fillMaxSize().padding(Spacing.md),
                contentScale = ContentScale.Fit,
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context).data(mediaUrl).crossfade(true).build(),
                imageLoader = imageLoader,
                contentDescription = "$exerciseName demonstration",
                modifier = Modifier.fillMaxSize().padding(Spacing.sm),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.exercise_guide_fallback),
                error = painterResource(R.drawable.exercise_guide_fallback),
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
