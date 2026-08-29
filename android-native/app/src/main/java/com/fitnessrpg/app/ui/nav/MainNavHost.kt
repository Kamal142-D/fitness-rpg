package com.fitnessrpg.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitnessrpg.app.di.ServiceLocator
import com.fitnessrpg.app.ui.importer.ImportInbox
import com.fitnessrpg.app.ui.screens.importer.ImportPlanScreen
import com.fitnessrpg.app.ui.screens.main.DailyMarchScreen
import com.fitnessrpg.app.ui.screens.main.HistoryScreen
import com.fitnessrpg.app.ui.screens.main.HomeTabs
import com.fitnessrpg.app.ui.screens.main.PlaceholderScreen
import com.fitnessrpg.app.ui.screens.main.QuestsScreen
import com.fitnessrpg.app.ui.screens.main.SettingsScreen
import com.fitnessrpg.app.ui.screens.main.TrainingPlanScreen
import com.fitnessrpg.app.ui.screens.workout.GateDetailScreen
import com.fitnessrpg.app.ui.screens.workout.GateBuilderScreen
import com.fitnessrpg.app.ui.screens.workout.WorkoutCompleteScreen
import com.fitnessrpg.app.ui.screens.workout.WorkoutScreen
import com.fitnessrpg.app.ui.screens.onboarding.AssessmentUpdateScreen
import com.fitnessrpg.app.ui.theme.MotionTokens
import com.fitnessrpg.app.ui.theme.motionDuration

/** Navigation for the authed area: tabs + settings + the gate/workout stack. */
@Composable
fun MainNavHost(userId: String, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val duration = motionDuration(MotionTokens.Standard)

    // A link shared into the app (share sheet / deep link) opens the importer.
    val sharedUrl by ImportInbox.pendingUrl.collectAsState()
    LaunchedEffect(sharedUrl) {
        if (sharedUrl != null) nav.navigate("import")
    }

    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(duration)) + fadeIn(tween(duration))
        },
        exitTransition = { fadeOut(tween(duration / 2)) },
        popEnterTransition = { fadeIn(tween(duration)) },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(duration)) + fadeOut(tween(duration))
        },
    ) {
        composable("home") {
            HomeTabs(
                userId = userId,
                onOpenGate = { id -> if (id != null) nav.navigate("gate/$id") },
                onOpenGates = { nav.navigate("gate_new") },
                onWorkoutStarted = {
                    nav.navigate("workout") { popUpTo("home") }
                },
                onSettings = { nav.navigate("settings") },
                onAssessment = { nav.navigate("assessment") },
                onOpenQuests = { nav.navigate("quests") },
                onOpenDailyMarch = { nav.navigate("march") },
                onOpenHistory = { nav.navigate("history") },
                onOpenPlan = { nav.navigate("plan") },
                onImportPlan = { nav.navigate("import") },
                onReevaluate = { nav.navigate("reevaluate") },
            )
        }
        composable("import") {
            val initialUrl = remember { ImportInbox.consume() }
            ImportPlanScreen(
                userId = userId,
                initialUrl = initialUrl,
                onBack = { nav.popBackStack() },
                onCreated = { id -> nav.navigate("gate/$id") { popUpTo("home") } },
            )
        }
        composable("plan") {
            TrainingPlanScreen(userId = userId, onBack = { nav.popBackStack() })
        }
        composable("quests") {
            QuestsScreen(onBack = { nav.popBackStack() })
        }
        composable("march") {
            DailyMarchScreen(userId = userId, onBack = { nav.popBackStack() })
        }
        composable("history") {
            HistoryScreen(userId = userId, onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                email = ServiceLocator.authRepository.currentUserEmail(),
                onBack = { nav.popBackStack() },
                onSignOut = onSignOut,
            )
        }
        composable("assessment") {
            AssessmentUpdateScreen(
                userId = userId,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }
        composable("reevaluate") {
            AssessmentUpdateScreen(
                userId = userId,
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
                forceAll = true,
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
