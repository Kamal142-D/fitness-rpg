package com.iconshift.core.shell

import com.iconshift.core.applyengine.Requirement

/** Tells an engine whether privileged access is ready and hands out the shell when it is. */
interface ShellAccess {
    /** Empty when [shell] can be used right now. */
    suspend fun missingRequirements(): List<Requirement>

    /** Connected shell, or null if requirements are missing or the service failed to bind. */
    suspend fun shell(): PrivilegedShell?
}
