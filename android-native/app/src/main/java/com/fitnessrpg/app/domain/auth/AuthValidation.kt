package com.fitnessrpg.app.domain.auth

/** Pure form validators for auth screens (ported from the Zod schemas). */

const val MIN_PASSWORD_LENGTH = 8

private val EMAIL_RE = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Returns an error message, or null when the email is valid. */
fun validateEmail(email: String): String? {
    val trimmed = email.trim()
    return when {
        trimmed.isEmpty() -> "Email is required"
        !EMAIL_RE.matches(trimmed) -> "Enter a valid email address"
        else -> null
    }
}

/** Sign-in only checks presence; length rules apply at registration. */
fun validateLoginPassword(password: String): String? =
    if (password.isEmpty()) "Password is required" else null

fun validateNewPassword(password: String): String? =
    if (password.length < MIN_PASSWORD_LENGTH) "Use at least $MIN_PASSWORD_LENGTH characters" else null

fun validateConfirmPassword(password: String, confirm: String): String? = when {
    confirm.isEmpty() -> "Confirm your password"
    password != confirm -> "Passwords do not match"
    else -> null
}
