package com.fitnessrpg.app.data.auth

import com.fitnessrpg.app.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

private const val NOT_CONFIGURED =
    "The app is not connected to a server yet. Add your Supabase keys to local.properties, then rebuild."

sealed interface AuthOutcome {
    data object Ok : AuthOutcome
    data class Error(val message: String) : AuthOutcome
}

sealed interface SignUpOutcome {
    data class Ok(val needsEmailConfirmation: Boolean) : SignUpOutcome
    data class Error(val message: String) : SignUpOutcome
}

/** Authentication against Supabase Auth (GoTrue). Wraps errors into friendly copy. */
class AuthRepository {

    private val auth get() = SupabaseProvider.client.auth

    val sessionStatus: StateFlow<SessionStatus> get() = auth.sessionStatus

    fun currentUserId(): String? = auth.currentUserOrNull()?.id

    fun currentUserEmail(): String? = auth.currentUserOrNull()?.email

    suspend fun signIn(email: String, password: String): AuthOutcome {
        if (!SupabaseProvider.isConfigured) return AuthOutcome.Error(NOT_CONFIGURED)
        return runCatching {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }.fold(
            onSuccess = { AuthOutcome.Ok },
            onFailure = { AuthOutcome.Error(friendlyAuthError(it.message)) },
        )
    }

    suspend fun signUp(email: String, password: String): SignUpOutcome {
        if (!SupabaseProvider.isConfigured) return SignUpOutcome.Error(NOT_CONFIGURED)
        return runCatching {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            // No session after sign-up means the project requires email confirmation.
            auth.currentSessionOrNull() == null
        }.fold(
            onSuccess = { needsConfirmation -> SignUpOutcome.Ok(needsConfirmation) },
            onFailure = { SignUpOutcome.Error(friendlyAuthError(it.message)) },
        )
    }

    suspend fun sendPasswordReset(email: String): AuthOutcome {
        if (!SupabaseProvider.isConfigured) return AuthOutcome.Error(NOT_CONFIGURED)
        return runCatching {
            auth.resetPasswordForEmail(email)
        }.fold(
            onSuccess = { AuthOutcome.Ok },
            onFailure = { AuthOutcome.Error(friendlyAuthError(it.message)) },
        )
    }

    suspend fun signOut(): AuthOutcome = runCatching { auth.signOut() }.fold(
        onSuccess = { AuthOutcome.Ok },
        onFailure = { AuthOutcome.Error(friendlyAuthError(it.message)) },
    )
}
