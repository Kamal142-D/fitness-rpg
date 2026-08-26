package com.fitnessrpg.app.ui.screens.workout

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface GuideImageState {
    data object Loading : GuideImageState
    data object Unavailable : GuideImageState
    data class Ready(val bitmap: ImageBitmap) : GuideImageState
}

@Composable
fun ExerciseGuideDialog(
    exercise: Exercise,
    isAdded: Boolean,
    onToggleAdded: () -> Unit,
    onDismiss: () -> Unit,
) {
    val imageState by produceState<GuideImageState>(
        initialValue = if (exercise.imageUrl.isNullOrBlank()) GuideImageState.Unavailable else GuideImageState.Loading,
        key1 = exercise.imageUrl,
    ) {
        val imageUrl = exercise.imageUrl
        if (!imageUrl.isNullOrBlank()) {
            value = loadGuideImage(imageUrl)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(Spacing.lg)
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .heightIn(max = 760.dp),
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

                    exercise.attribution?.takeIf { it.isNotBlank() }?.let {
                        AppText("Movement media: $it", variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(Palette.Background),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is GuideImageState.Ready -> Image(
                bitmap = state.bitmap,
                contentDescription = "$exerciseName demonstration",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
            GuideImageState.Loading -> {
                Image(
                    painter = painterResource(R.drawable.exercise_guide_fallback),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.35f,
                )
                CircularProgressIndicator(color = Palette.Primary)
            }
            GuideImageState.Unavailable -> Image(
                painter = painterResource(R.drawable.exercise_guide_fallback),
                contentDescription = "Exercise guide illustration",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private suspend fun loadGuideImage(rawUrl: String): GuideImageState = withContext(Dispatchers.IO) {
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
            GuideImageState.Ready(bitmap.asImageBitmap())
        } finally {
            connection.disconnect()
        }
    }.getOrElse { GuideImageState.Unavailable }
}
