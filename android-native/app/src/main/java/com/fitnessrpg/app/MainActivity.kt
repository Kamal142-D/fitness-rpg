package com.fitnessrpg.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fitnessrpg.app.ui.nav.AppRoot
import com.fitnessrpg.app.ui.theme.FitnessRpgTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitnessRpgTheme {
                AppRoot()
            }
        }
    }
}
