package com.footprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.footprint.ui.theme.FootprintTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val initialDestination = if (intent?.getStringExtra("destination") == "map") {
            FootprintDestination.Map
        } else {
            FootprintDestination.Dashboard
        }

        setContent {
            FootprintTheme {
                FootprintApp(initialDestination = initialDestination)
            }
        }
    }
}
