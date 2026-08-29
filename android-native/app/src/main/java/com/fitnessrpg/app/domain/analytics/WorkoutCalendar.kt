package com.fitnessrpg.app.domain.analytics

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Distinct local dates on which at least one workout was completed. */
fun completedWorkoutDates(
    sessions: List<SessionSummary>,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Set<LocalDate> = sessions.mapNotNull { session ->
    session.completedAt?.let { completedAt -> parseWorkoutDate(completedAt, zoneId) }
}.toSet()

/** Number of distinct workout days in the current Monday-to-Sunday week. */
fun workoutsThisWeek(workoutDates: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
    val weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    return workoutDates.count { !it.isBefore(weekStart) && !it.isAfter(today) }
}

/** Profile values can be absent on older accounts, so three days is the safe default. */
fun normalizedWeeklyGoal(trainingDaysPerWeek: Int?): Int = trainingDaysPerWeek?.coerceIn(1, 7) ?: 3

private fun parseWorkoutDate(value: String, zoneId: ZoneId): LocalDate? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null

    return runCatching { Instant.parse(trimmed).atZone(zoneId).toLocalDate() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(trimmed).atZoneSameInstant(zoneId).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(trimmed.take(10)) }.getOrNull()
}
