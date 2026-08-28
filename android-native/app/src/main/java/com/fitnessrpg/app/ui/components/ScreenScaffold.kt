package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.fitnessrpg.app.ui.theme.MaxContentWidth
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing

/**
 * Page-level wrapper: paints the app background, applies safe-area insets, caps
 * content width, and optionally scrolls. Screens compose this rather than
 * re-implementing background/insets each time.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    padding: Dp = Spacing.xl,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bottomClearance = LocalBottomBarClearance.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Palette.Background)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Content cards use the static frosted fallback. Keeping live backdrop
        // capture to the floating navigation avoids full-screen blur layers on
        // text-heavy, vertically scrolling screens.
        GlassBackdrop(Modifier.fillMaxSize())

        val columnModifier = Modifier
            .fillMaxWidth()
            .widthIn(max = MaxContentWidth)
            .let { if (scroll) it.verticalScroll(rememberScrollState()) else it }
            .padding(
                start = padding,
                top = padding,
                end = padding,
                bottom = padding + bottomClearance,
            )

        Column(
            modifier = columnModifier,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            content = content,
        )
    }
}
