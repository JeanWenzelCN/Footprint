package com.footprint.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footprint.FootprintViewModel
import com.footprint.ui.screens.ExportTraceScreen
import com.footprint.ui.screens.GenerativeArtScreen
import com.footprint.ui.screens.art.FootprintArtStudioScreen
import com.footprint.ui.theme.FootprintTheme

class NativeScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val screenType = intent.getStringExtra("screen_type") ?: "art_studio"
        val initialYear = intent.getIntExtra("initial_year", -1).takeIf { it != -1 }
        
        setContent {
            val viewModel: FootprintViewModel = viewModel(factory = FootprintViewModel.Factory)
            val uiState by viewModel.uiState.collectAsState()
            
            FootprintTheme(
                themeMode = uiState.themeMode,
                style = uiState.themeStyle,
                dominantMood = uiState.summary.monthly.dominantMood
            ) {
                when (screenType) {
                    "art_studio" -> FootprintArtStudioScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                    "generative_art" -> GenerativeArtScreen(
                        viewModel = viewModel,
                        onBack = { finish() }
                    )
                    "export_trace" -> ExportTraceScreen(
                        viewModel = viewModel,
                        initialYear = initialYear,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
