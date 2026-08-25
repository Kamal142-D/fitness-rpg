package com.fitnessrpg.app.domain.gates

import com.fitnessrpg.app.domain.model.GateTemplate

/** Pure visibility rule used by routine lists; history is intentionally separate. */
fun activeGateTemplates(templates: List<GateTemplate>, hiddenSystemIds: Set<String> = emptySet()): List<GateTemplate> =
    templates.filter { it.deletedAt == null && it.id !in hiddenSystemIds }
