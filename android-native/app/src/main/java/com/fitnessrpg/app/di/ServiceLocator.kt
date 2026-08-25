package com.fitnessrpg.app.di

import com.fitnessrpg.app.data.auth.AuthRepository
import com.fitnessrpg.app.data.local.ActiveWorkoutStore
import com.fitnessrpg.app.data.repo.AnalyticsRepository
import com.fitnessrpg.app.data.repo.GateRepository
import com.fitnessrpg.app.data.repo.PrRepository
import com.fitnessrpg.app.data.repo.ProfileRepository
import com.fitnessrpg.app.data.repo.ProgressionRepository
import com.fitnessrpg.app.data.repo.QuestRepository
import com.fitnessrpg.app.data.repo.WorkoutRepository

/**
 * Manual dependency container. A small app doesn't need Hilt; ViewModels read
 * their repositories from here. All singletons, created lazily on first use.
 */
object ServiceLocator {
    val authRepository: AuthRepository by lazy { AuthRepository() }
    val gateRepository: GateRepository by lazy { GateRepository() }
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository() }
    val prRepository: PrRepository by lazy { PrRepository() }
    val progressionRepository: ProgressionRepository by lazy { ProgressionRepository() }
    val questRepository: QuestRepository by lazy { QuestRepository() }
    val analyticsRepository: AnalyticsRepository by lazy { AnalyticsRepository() }
    val profileRepository: ProfileRepository by lazy { ProfileRepository() }
    val activeWorkoutStore: ActiveWorkoutStore by lazy { ActiveWorkoutStore() }
}
