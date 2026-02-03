package com.footprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.footprint.ui.theme.FootprintTheme
import com.footprint.ui.theme.LocalHazeState
import dev.chrisbanes.haze.HazeState
import androidx.compose.runtime.remember
import com.footprint.FootprintApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 让内容延伸到状态栏和导航栏下方，配合 Material 3 沉浸式体验
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            FootprintTheme {
                val hazeState = remember { HazeState() }

                CompositionLocalProvider(LocalHazeState provides hazeState) {
                    // Original App Content (now enhanced with Glass effects via LegacyCompatibility)
                    FootprintApp()
                }
            }
        }
    }
}