package com.fitnessrpg.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessrpg.app.ui.AppState
import com.fitnessrpg.app.ui.SessionViewModel
import com.fitnessrpg.app.ui.screens.SplashScreen
import com.fitnessrpg.app.ui.screens.auth.AuthFlow
import com.fitnessrpg.app.ui.screens.onboarding.OnboardingFlow

/** Top-level router: splash -> auth -> onboarding -> main, driven by session state. */
@Composable
fun AppRoot(session: SessionViewModel = viewModel()) {
    val state by session.state.collectAsStateWithLifecycle()

    when (val s = state) {
        AppState.Loading -> SplashScreen("Starting the System")
        AppState.NeedsAuth -> AuthFlow()
        is AppState.NeedsOnboarding -> OnboardingFlow(
            userId = s.userId,
            onComplete = { session.onOnboardingComplete(s.userId) },
        )
        is AppState.Ready -> MainNavHost(
            userId = s.userId,
            onSignOut = { session.signOut() },
        )
    }
}
