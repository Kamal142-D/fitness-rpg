package com.fitnessrpg.app.data.local

import com.fitnessrpg.app.domain.model.ActiveWorkout
import com.fitnessrpg.app.domain.model.GateDetail
import com.fitnessrpg.app.domain.workouts.createActiveWorkout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory holder for the active workout, exposed as a [StateFlow] the workout
 * screen observes. The pure reducers in `domain.workouts` produce the new state;
 * this only holds the latest value. (DataStore persistence across process death
 * is a follow-up.)
 */
class ActiveWorkoutStore {
    private val _state = MutableStateFlow<ActiveWorkout?>(null)
    val state: StateFlow<ActiveWorkout?> = _state.asStateFlow()

    val hasActive: Boolean get() = _state.value != null

    fun start(detail: GateDetail) {
        _state.value = createActiveWorkout(detail)
    }

    /** Apply a pure reducer to the current workout (no-op when none is active). */
    fun update(transform: (ActiveWorkout) -> ActiveWorkout) {
        _state.value = _state.value?.let(transform)
    }

    fun current(): ActiveWorkout? = _state.value

    fun clear() {
        _state.value = null
    }
}
