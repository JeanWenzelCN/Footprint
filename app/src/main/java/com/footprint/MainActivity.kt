package com.footprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.footprint.ui.theme.FootprintTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 让内容延伸到状态栏和导航栏下方，配合 Material 3 沉浸式体验
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            FootprintApp()
        }
    }
}