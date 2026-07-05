package com.lazyjournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.lazyjournal.app.ui.LocalAppContainer
import com.lazyjournal.app.ui.LazyJournalApp
import com.lazyjournal.app.ui.theme.LazyJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as LazyJournalApplication
        setContent {
            CompositionLocalProvider(LocalAppContainer provides app.container) {
                LazyJournalTheme {
                    LazyJournalApp()
                }
            }
        }
    }
}
