package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.model.GateTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineDeletionTest {
    @Test fun `archived routine disappears while historical session identity remains`() {
        val active = gate("user-gate", null)
        val archived = gate("user-gate", "2026-08-25T00:00:00Z")
        val historicalSessionTemplateIds = listOf(active.id)

        assertEquals(listOf(active), activeGateTemplates(listOf(active)))
        assertEquals(emptyList<GateTemplate>(), activeGateTemplates(listOf(archived)))
        assertEquals(listOf("user-gate"), historicalSessionTemplateIds)
    }

    private fun gate(id: String, deletedAt: String?) = GateTemplate(
        id, "user", "Push Day", null, 45, null, false, deletedAt = deletedAt,
    )
}
