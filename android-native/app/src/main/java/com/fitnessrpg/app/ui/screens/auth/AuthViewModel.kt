package com.fitnessrpg.app.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitnessrpg.app.data.auth.AuthOutcome
import com.fitnessrpg.app.data.auth.SignUpOutcome
import com.fitnessrpg.app.di.ServiceLocator
import kotlinx.coroutines.launch

/** Drives sign-in / sign-up / password-reset. Success routing is handled by the
 *  session state flipping to Authenticated; this only reports errors/info. */
class AuthViewModel : ViewModel() {

    private val auth = ServiceLocator.authRepository

    var submitting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var infoMessage by mutableStateOf<String?>(null)
        private set

    fun clearMessages() {
        errorMessage = null
        infoMessage = null
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            submitting = true
            clearMessages()
            when (val r = auth.signIn(email.trim(), password)) {
                is AuthOutcome.Ok -> Unit // session flips -> nav switches
                is AuthOutcome.Error -> errorMessage = r.message
            }
            submitting = false
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            submitting = true
            clearMessages()
            when (val r = auth.signUp(email.trim(), password)) {
                is SignUpOutcome.Ok ->
                    if (r.needsEmailConfirmation) {
                        infoMessage = "Check your inbox to confirm your email, then sign in."
                    }
                is SignUpOutcome.Error -> errorMessage = r.message
            }
            submitting = false
        }
    }

    fun sendReset(email: String) {
        viewModelScope.launch {
            submitting = true
            clearMessages()
            when (val r = auth.sendPasswordReset(email.trim())) {
                is AuthOutcome.Ok -> infoMessage = "If that email has an account, a reset link is on its way."
                is AuthOutcome.Error -> errorMessage = r.message
            }
            submitting = false
        }
    }
}
