package com.iconshift.poc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iconshift.poc.poc.PocScreen
import com.iconshift.poc.ui.theme.IconShiftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IconShiftTheme {
                PocScreen()
            }
        }
    }
}
