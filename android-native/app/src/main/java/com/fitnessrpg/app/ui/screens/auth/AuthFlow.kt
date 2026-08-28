package com.fitnessrpg.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fitnessrpg.app.domain.auth.validateConfirmPassword
import com.fitnessrpg.app.domain.auth.validateEmail
import com.fitnessrpg.app.domain.auth.validateLoginPassword
import com.fitnessrpg.app.domain.auth.validateNewPassword
import com.fitnessrpg.app.ui.components.AppButton
import com.fitnessrpg.app.ui.components.AppCard
import com.fitnessrpg.app.ui.components.AppText
import com.fitnessrpg.app.ui.components.AppTextField
import com.fitnessrpg.app.ui.components.ButtonVariant
import com.fitnessrpg.app.ui.components.CardTone
import com.fitnessrpg.app.ui.components.ScreenScaffold
import com.fitnessrpg.app.ui.components.TextTone
import com.fitnessrpg.app.ui.components.TextVariant
import com.fitnessrpg.app.ui.theme.Palette
import com.fitnessrpg.app.ui.theme.Spacing

private enum class AuthScreen { LOGIN, REGISTER, FORGOT }

@Composable
fun AuthFlow(vm: AuthViewModel = viewModel()) {
    var screen by remember { mutableStateOf(AuthScreen.LOGIN) }

    ScreenScaffold {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppText("THE SYSTEM", variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
            AppText(
                when (screen) {
                    AuthScreen.LOGIN -> "Welcome back, Hunter"
                    AuthScreen.REGISTER -> "Begin your Awakening"
                    AuthScreen.FORGOT -> "Reset your password"
                },
                variant = TextVariant.DISPLAY,
            )
        }

        vm.errorMessage?.let { AppText(it, tone = TextTone.DANGER) }
        vm.infoMessage?.let { AppText(it, tone = TextTone.SUCCESS) }

        when (screen) {
            AuthScreen.LOGIN -> LoginForm(vm, onGoRegister = { vm.clearMessages(); screen = AuthScreen.REGISTER }, onGoForgot = { vm.clearMessages(); screen = AuthScreen.FORGOT })
            AuthScreen.REGISTER -> RegisterForm(vm, onGoLogin = { vm.clearMessages(); screen = AuthScreen.LOGIN })
            AuthScreen.FORGOT -> ForgotForm(vm, onGoLogin = { vm.clearMessages(); screen = AuthScreen.LOGIN })
        }
    }
}

@Composable
private fun LoginForm(vm: AuthViewModel, onGoRegister: () -> Unit, onGoForgot: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val emailErr = if (showErrors) validateEmail(email) else null
    val pwErr = if (showErrors) validateLoginPassword(password) else null

    AppCard(tone = CardTone.GLASS) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppTextField(email, { email = it }, label = "Email", error = emailErr, placeholder = "you@example.com", keyboardType = KeyboardType.Email)
            AppTextField(password, { password = it }, label = "Password", error = pwErr, secureToggle = true, imeAction = ImeAction.Done)
            Spacer(Modifier.height(Spacing.xs))
            AppButton(
                "Sign in",
                onClick = {
                    showErrors = true
                    if (validateEmail(email) == null && validateLoginPassword(password) == null) vm.signIn(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
                loading = vm.submitting,
            )
            AppText("Forgot your password?", variant = TextVariant.CAPTION, tone = TextTone.ACCENT, modifier = Modifier.clickable { onGoForgot() })
        }
    }
    AuthSwitchRow("New here?", "Create an account", onGoRegister)
}

@Composable
private fun RegisterForm(vm: AuthViewModel, onGoLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val emailErr = if (showErrors) validateEmail(email) else null
    val pwErr = if (showErrors) validateNewPassword(password) else null
    val confErr = if (showErrors) validateConfirmPassword(password, confirm) else null

    AppCard(tone = CardTone.GLASS) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppTextField(email, { email = it }, label = "Email", error = emailErr, placeholder = "you@example.com", keyboardType = KeyboardType.Email)
            AppTextField(password, { password = it }, label = "Password", error = pwErr, secureToggle = true)
            AppTextField(confirm, { confirm = it }, label = "Confirm password", error = confErr, secureToggle = true, imeAction = ImeAction.Done)
            Spacer(Modifier.height(Spacing.xs))
            AppButton(
                "Create account",
                onClick = {
                    showErrors = true
                    if (validateEmail(email) == null && validateNewPassword(password) == null && validateConfirmPassword(password, confirm) == null) {
                        vm.signUp(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                loading = vm.submitting,
            )
        }
    }
    AuthSwitchRow("Already have an account?", "Sign in", onGoLogin)
}

@Composable
private fun ForgotForm(vm: AuthViewModel, onGoLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val emailErr = if (showErrors) validateEmail(email) else null

    AppCard(tone = CardTone.GLASS) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppText("We'll email you a link to set a new password.", tone = TextTone.SECONDARY)
            AppTextField(email, { email = it }, label = "Email", error = emailErr, placeholder = "you@example.com", keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
            AppButton(
                "Send reset link",
                onClick = {
                    showErrors = true
                    if (validateEmail(email) == null) vm.sendReset(email)
                },
                modifier = Modifier.fillMaxWidth(),
                loading = vm.submitting,
            )
        }
    }
    AuthSwitchRow("Remembered it?", "Back to sign in", onGoLogin)
}

@Composable
private fun AuthSwitchRow(prompt: String, action: String, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AppText(prompt, variant = TextVariant.CAPTION, tone = TextTone.SECONDARY)
        AppButton(action, onClick = onClick, variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
    }
}
