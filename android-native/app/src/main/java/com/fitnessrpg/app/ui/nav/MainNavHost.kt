package com.fitnessrpg.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.ui.screens.main.HomeTabs
import com.fitnessrpg.app.ui.screens.main.PlaceholderScreen
import com.fitnessrpg.app.ui.screens.main.SettingsScreen
import com.fitnessrpg.app.ui.screens.workout.GateDetailScreen
import com.fitnessrpg.app.ui.screens.workout.GateBuilderScreen
import com.fitnessrpg.app.ui.screens.workout.WorkoutCompleteScreen
import com.fitnessrpg.app.ui.screens.workout.WorkoutScreen

/** Navigation for the authed area: tabs + settings + the gate/workout stack. */
@Composable
fun MainNavHost(userId: String, onSignOut: () -> Unit) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeTabs(
                userId = userId,
                onOpenGate = { id -> if (id != null) nav.navigate("gate/$id") },
                onOpenGates = { nav.navigate("gate_new") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("settings") {
            SettingsScreen(
                email = ServiceLocator.authRepository.currentUserEmail(),
                onBack = { nav.popBackStack() },
                onSignOut = onSignOut,
            )
        }
        composable("gate_new") {
            GateBuilderScreen(
                userId = userId,
                onBack = { nav.popBackStack() },
                onSaved = { id -> nav.navigate("gate/$id") { popUpTo("home") } },
            )
        }
        composable("gate_edit/{id}") { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            GateBuilderScreen(userId, templateId = id, onBack = { nav.popBackStack() }, onSaved = {
                nav.navigate("gate/$it") { popUpTo("home") }
            })
        }
        composable("gate/{id}") { entry ->
            GateDetailScreen(
                userId = userId,
                templateId = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("gate_edit/${entry.arguments?.getString("id").orEmpty()}") },
                onStarted = {
                    nav.navigate("workout") { popUpTo("home") }
                },
            )
        }
        composable("workout") {
            WorkoutScreen(
                userId = userId,
                onFinished = { nav.navigate("complete") { popUpTo("home") } },
                onCancel = { nav.popBackStack("home", inclusive = false) },
            )
        }
        composable("complete") {
            WorkoutCompleteScreen(onDone = { nav.popBackStack("home", inclusive = false) })
        }
    }
}
