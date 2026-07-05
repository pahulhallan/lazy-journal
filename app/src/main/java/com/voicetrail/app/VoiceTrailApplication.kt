package com.voicetrail.app

import android.app.Application
import com.voicetrail.app.core.AppContainer

class VoiceTrailApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

