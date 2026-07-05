package com.lazyjournal.app

import android.app.Application
import com.lazyjournal.app.core.AppContainer

class LazyJournalApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
