package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.ui.theme.Palette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Extra scroll clearance supplied by an overlaid navigation surface. */
internal val LocalBottomBarClearance = compositionLocalOf { 0.dp }

internal val AppGlassStyle = HazeStyle(
    backgroundColor = Palette.GlassBase,
    tint = HazeTint(Palette.GlassTint),
    blurRadius = 30.dp,
    noiseFactor = 0.05f,
)

/**
 * Restrained frosted material, matching the Mangaku bottom bar: full-resolution
 * backdrop sampling (Haze's default) so the blur reads as smooth glass, never the
 * blocky low-res pixels a downscaled input produces. The translucent fallback
 * stays readable on Android versions without live backdrop blur.
 */
internal fun Modifier.appGlass(
    shape: Shape,
    hazeState: HazeState?,
): Modifier = this
    .clip(shape)
    .then(
        if (hazeState != null) {
            Modifier.hazeEffect(state = hazeState, style = AppGlassStyle)
        } else {
            Modifier.background(Palette.GlassBase)
        },
    )
    // Mangaku's material is evenly smoky: no directional shine or vertical
    // reflection. Accent color belongs to the selected control, not the glass.
    .background(Palette.GlassTint.copy(alpha = 0.10f))
    .border(
        width = 1.dp,
        color = Palette.GlassEdge,
        shape = shape,
    )

/** Low-contrast color behind glass so the blur has depth without becoming a gradient flood. */
@Composable
internal fun GlassBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Palette.Background, Color(0xFF0A1018), Palette.Background),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Palette.Primary.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.12f),
                radius = size.minDimension * 0.72f,
            ),
            radius = size.minDimension * 0.72f,
            center = Offset(size.width * 0.86f, size.height * 0.12f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Palette.Accent.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.08f, size.height * 0.72f),
                radius = size.minDimension * 0.62f,
            ),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.08f, size.height * 0.72f),
        )
    }
}
