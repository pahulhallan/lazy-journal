package com.voicetrail.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.voicetrail.app.ui.LocalAppContainer
import com.voicetrail.app.ui.VoiceTrailApp
import com.voicetrail.app.ui.theme.VoiceTrailTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as VoiceTrailApplication
        setContent {
            CompositionLocalProvider(LocalAppContainer provides app.container) {
                VoiceTrailTheme {
                    VoiceTrailApp()
                }
            }
        }
    }
}

