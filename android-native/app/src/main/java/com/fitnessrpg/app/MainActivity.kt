package com.fitnessrpg.app

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fitnessrpg.app.ui.nav.AppRoot
import com.fitnessrpg.app.ui.screens.main.HomeTabs
import com.fitnessrpg.app.ui.theme.FitnessRpgTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            FitnessRpgTheme {
                if (BuildConfig.DEBUG && intent.getBooleanExtra("previewGlass", false)) {
                    HomeTabs(
                        userId = "design-preview",
                        onOpenGate = {},
                        onOpenGates = {},
                        onWorkoutStarted = {},
                        onSettings = {},
                        onAssessment = {},
                        onOpenQuests = {},
                        onOpenDailyMarch = {},
                        onOpenHistory = {},
                        onOpenPlan = {},
                    )
                } else {
                    AppRoot()
                }
            }
        }
    }
}
