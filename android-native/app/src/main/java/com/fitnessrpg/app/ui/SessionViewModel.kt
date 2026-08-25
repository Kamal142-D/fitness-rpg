package com.fitnessrpg.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnessrpg.app.di.ServiceLocator
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Top-level app routing state, derived from the auth session + onboarding flag. */
sealed interface AppState {
    data object Loading : AppState
    data object NeedsAuth : AppState
    data class NeedsOnboarding(val userId: String) : AppState
    data class Ready(val userId: String) : AppState
}

class SessionViewModel : ViewModel() {

    private val auth = ServiceLocator.authRepository
    private val profiles = ServiceLocator.profileRepository

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> resolveAuthenticated()
                    is SessionStatus.NotAuthenticated -> _state.value = AppState.NeedsAuth
                    else -> {
                        // Initializing / RefreshFailure: stay on Loading unless
                        // we've already resolved a signed-in state.
                        if (_state.value == AppState.Loading) _state.value = AppState.Loading
                    }
                }
            }
        }
    }

    private suspend fun resolveAuthenticated() {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.value = AppState.NeedsAuth
            return
        }
        val profile = runCatching { profiles.getProfile(userId) }.getOrNull()
        _state.value = if (profile?.onboardingCompleted == true) {
            AppState.Ready(userId)
        } else {
            AppState.NeedsOnboarding(userId)
        }
    }

    /** Called after the Awakening completes to move straight into the app. */
    fun onOnboardingComplete(userId: String) {
        _state.value = AppState.Ready(userId)
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
