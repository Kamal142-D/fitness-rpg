package com.fitnessrpg.app.ui.screens.main

import androidx.compose.runtime.Composable
import com.fitnessrpg.app.data.repo.RankAssessmentSnapshot
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.domain.analytics.PlayerData
import com.fitnessrpg.app.domain.model.PlayerProgression
import com.fitnessrpg.app.domain.model.Profile
import com.fitnessrpg.app.ui.util.CachedState
import com.fitnessrpg.app.ui.util.rememberCached
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable

/**
 * The bundle of per-user data behind both the Ranking and Profile tabs. They share
 * one cache key so switching between them never refetches, and a single loader keeps
 * the two screens consistent.
 */
@Serializable
data class PlayerBundle(
    val progression: PlayerProgression?,
    val data: PlayerData,
    val assessment: RankAssessmentSnapshot,
    val displayName: String? = null,
    val profile: Profile? = null,
)

@Composable
fun rememberPlayerBundle(userId: String): CachedState<PlayerBundle> =
    rememberCached("player:2:$userId", PlayerBundle.serializer()) {
        coroutineScope {
            val prog = async { ServiceLocator.progressionRepository.getProgression(userId) }
            val data = async { ServiceLocator.analyticsRepository.getPlayerData(userId) }
            val assessment = async { ServiceLocator.assessmentRepository.getRankAssessment(userId) }
            val profile = async { runCatching { ServiceLocator.profileRepository.getProfile(userId) }.getOrNull() }
            val loadedProfile = profile.await()
            PlayerBundle(prog.await(), data.await(), assessment.await(), loadedProfile?.displayName, loadedProfile)
        }
    }
