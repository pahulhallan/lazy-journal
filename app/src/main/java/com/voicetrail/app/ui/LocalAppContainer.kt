package com.voicetrail.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.voicetrail.app.core.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("VoiceTrail dependencies are not available.")
}

