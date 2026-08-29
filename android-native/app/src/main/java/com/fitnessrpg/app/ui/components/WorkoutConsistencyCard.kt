package com.fitnessrpg.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fitnessrpg.app.R
import com.fitnessrpg.app.domain.analytics.workoutsThisWeek
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Radius
import com.fitnessrpg.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutConsistencyCard(
    streakDays: Int,
    workoutDates: Set<LocalDate>,
    weeklyGoal: Int,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val safeGoal = weeklyGoal.coerceIn(1, 7)
    val weeklyCompleted = workoutsThisWeek(workoutDates, today)
    val progress = (weeklyCompleted.toFloat() / safeGoal).coerceIn(0f, 1f)
    val percent = (progress * 100).toInt()
    val month = YearMonth.from(today)

    AppCard(modifier = modifier, tone = CardTone.GLASS) {
        ConsistencySection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppText("KEEP IT UP", variant = TextVariant.CAPTION, tone = TextTone.ACCENT)
                    AppText(
                        "$streakDays ${if (streakDays == 1) "Day" else "Days"} Streak",
                        variant = TextVariant.TITLE,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_streak),
                    contentDescription = null,
                    tint = Palette.Primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        ConsistencySection {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText("Weekly Goal", variant = TextVariant.HEADING)
                AppText("$percent%", variant = TextVariant.LABEL, tone = TextTone.SECONDARY, mono = true)
            }
            AppProgressBar(progress)
            AppText(
                "$weeklyCompleted / $safeGoal workouts completed",
                variant = TextVariant.CAPTION,
                tone = TextTone.TERTIARY,
            )
        }

        Column(
            modifier = Modifier.padding(top = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AppText(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                variant = TextVariant.HEADING,
            )
            CalendarGrid(month = month, workoutDates = workoutDates, today = today)
        }
    }
}

@Composable
private fun ConsistencySection(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Palette.Surface1)
            .border(BorderStroke(1.dp, Palette.Hairline), RoundedCornerShape(Radius.md))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content,
    )
}

@Composable
private fun CalendarGrid(month: YearMonth, workoutDates: Set<LocalDate>, today: LocalDate) {
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val leadingEmpty = month.atDay(1).dayOfWeek.value - 1
    val cells = List(leadingEmpty) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val rows = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    AppText(label, variant = TextVariant.CAPTION, tone = TextTone.TERTIARY)
                }
            }
        }
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { index ->
                    val date = week.getOrNull(index)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (date != null) CalendarDay(
                            date = date,
                            completed = date in workoutDates,
                            today = date == today,
                            future = date.isAfter(today),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(date: LocalDate, completed: Boolean, today: Boolean, future: Boolean) {
    val shape = RoundedCornerShape(Radius.sm)
    val background = when {
        completed && today -> Palette.Primary
        completed -> Palette.PrimaryContainer
        else -> Palette.Surface1
    }
    val border = when {
        today && !completed -> BorderStroke(1.dp, Palette.PrimaryMid)
        completed && !today -> BorderStroke(1.dp, Palette.PrimaryMid.copy(alpha = 0.35f))
        else -> BorderStroke(1.dp, Color.Transparent)
    }
    val textColor = when {
        completed && today -> Palette.Background
        completed -> Palette.Primary
        future -> Palette.TextTertiary.copy(alpha = 0.45f)
        else -> Palette.TextTertiary
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(background)
            .border(border, shape),
        contentAlignment = Alignment.Center,
    ) {
        AppText(date.dayOfMonth.toString(), variant = TextVariant.CAPTION, color = textColor, mono = true)
    }
}
