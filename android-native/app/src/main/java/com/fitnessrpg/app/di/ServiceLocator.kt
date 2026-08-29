package com.fitnessrpg.app.di

import android.content.Context
import com.fitnessrpg.app.data.auth.AuthRepository
import com.fitnessrpg.app.data.cache.PersistentCache
import com.fitnessrpg.app.data.local.ActiveWorkoutStore
import com.fitnessrpg.app.data.repo.AnalyticsRepository
import com.fitnessrpg.app.data.repo.AssessmentRepository
import com.fitnessrpg.app.data.repo.GateRepository
import com.fitnessrpg.app.data.repo.ImportRepository
import com.fitnessrpg.app.data.repo.PrRepository
import com.fitnessrpg.app.data.repo.ProfileRepository
import com.fitnessrpg.app.data.repo.ProgressionRepository
import com.fitnessrpg.app.data.repo.QuestRepository
import com.fitnessrpg.app.data.repo.TrainingPlanRepository
import com.fitnessrpg.app.data.repo.WorkoutRepository
import com.fitnessrpg.app.data.steps.StepRepository

/**
 * Manual dependency container. A small app doesn't need Hilt; ViewModels read
 * their repositories from here. All singletons, created lazily on first use.
 */
object ServiceLocator {
    private lateinit var applicationContext: Context

    fun init(context: Context) {
        applicationContext = context.applicationContext
        PersistentCache.init(applicationContext)
    }

    val authRepository: AuthRepository by lazy { AuthRepository() }
    val gateRepository: GateRepository by lazy { GateRepository() }
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepository() }
    val prRepository: PrRepository by lazy { PrRepository() }
    val progressionRepository: ProgressionRepository by lazy { ProgressionRepository() }
    val questRepository: QuestRepository by lazy { QuestRepository() }
    val analyticsRepository: AnalyticsRepository by lazy { AnalyticsRepository() }
    val assessmentRepository: AssessmentRepository by lazy { AssessmentRepository() }
    val profileRepository: ProfileRepository by lazy { ProfileRepository() }
    val trainingPlanRepository: TrainingPlanRepository by lazy { TrainingPlanRepository() }
    val importRepository: ImportRepository by lazy { ImportRepository() }
    val activeWorkoutStore: ActiveWorkoutStore by lazy { ActiveWorkoutStore() }
    val stepRepository: StepRepository by lazy {
        check(::applicationContext.isInitialized) { "ServiceLocator.init(context) must run before step tracking." }
        StepRepository(applicationContext)
    }
}
