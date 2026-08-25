package com.fitnessrpg.app.data.auth

/**
 * Map raw Supabase auth error messages to friendly, non-technical copy. Falls
 * back to the raw message (or a generic line) when nothing matches.
 */
fun friendlyAuthError(message: String?): String {
    if (message.isNullOrEmpty()) return "Something went wrong. Please try again."
    val m = message.lowercase()

    return when {
        "invalid login credentials" in m -> "The email or password is incorrect."
        "email not confirmed" in m -> "Please confirm your email first. Check your inbox for the link."
        "already registered" in m || "already been registered" in m ->
            "An account with this email already exists. Try signing in instead."
        "password should be at least" in m || "password is too short" in m -> "That password is too short."
        "unable to validate email address" in m || "invalid email" in m ->
            "That email address doesn't look valid."
        "rate limit" in m || "too many requests" in m || "for security purposes" in m ->
            "Too many attempts. Please wait a moment and try again."
        "network" in m || "failed to fetch" in m || "fetch failed" in m ->
            "Network error. Check your connection and try again."
        else -> message
    }
}
