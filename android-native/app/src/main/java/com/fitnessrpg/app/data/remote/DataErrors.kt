package com.fitnessrpg.app.data.remote

/** Never expose Supabase URLs, headers, keys, JWTs, SQL, or stack details in UI. */
fun friendlyDataError(error: Throwable?, fallback: String = "Something went wrong. Please try again."): String {
    val raw = error?.message.orEmpty().lowercase()
    return when {
        "could not find the table" in raw || "schema cache" in raw || "pgrst205" in raw ->
            "The app database needs an update. Please try again after the service update is installed."
        "jwt" in raw || "unauthorized" in raw || "not authenticated" in raw || "401" in raw ->
            "Your session expired. Please sign in again."
        "network" in raw || "unable to resolve host" in raw || "timeout" in raw || "failed to connect" in raw ->
            "Could not reach the server. Check your connection and try again."
        else -> fallback
    }
}
