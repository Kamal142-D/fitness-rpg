package com.fitnessrpg.app.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {
    @Test
    fun `email validation`() {
        assertNotNull(validateEmail(""))
        assertNotNull(validateEmail("nope"))
        assertNotNull(validateEmail("a@b"))
        assertNull(validateEmail("user@example.com"))
        assertNull(validateEmail("  user@example.com  "))
    }

    @Test
    fun `login password only checks presence`() {
        assertNotNull(validateLoginPassword(""))
        assertNull(validateLoginPassword("x"))
    }

    @Test
    fun `new password enforces minimum length`() {
        assertNotNull(validateNewPassword("short"))
        assertNull(validateNewPassword("longenough"))
        assertEquals(8, MIN_PASSWORD_LENGTH)
    }

    @Test
    fun `confirm password must match`() {
        assertNotNull(validateConfirmPassword("abcd1234", ""))
        assertNotNull(validateConfirmPassword("abcd1234", "different"))
        assertNull(validateConfirmPassword("abcd1234", "abcd1234"))
    }
}
