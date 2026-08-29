package com.fitnessrpg.app

import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fitnessrpg.app.ui.importer.ImportInbox
import com.fitnessrpg.app.ui.nav.AppRoot
import com.fitnessrpg.app.ui.screens.main.HomeTabs
import com.fitnessrpg.app.ui.theme.FitnessRpgTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
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
                        onImportPlan = {},
                        onReevaluate = {},
                    )
                } else {
                    AppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    /** A LiftoffRank link shared or opened into the app queues an import. */
    private fun handleShareIntent(intent: Intent?) {
        intent ?: return
        val url = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        ImportInbox.offer(url)
    }
}
