package com.fitnessrpg.app.data.remote

import org.junit.Assert.*
import org.junit.Test

class DataErrorsTest {
    @Test fun `raw backend request and token are never returned`() {
        val raw = "Could not find the table public.hidden_system_templates Headers Authorization Bearer secret.jwt.value"
        val friendly = friendlyDataError(IllegalStateException(raw))
        assertFalse(friendly.contains("Bearer", true))
        assertFalse(friendly.contains("jwt", true))
        assertFalse(friendly.contains("https://", true))
        assertTrue(friendly.contains("database", true))
    }
}
