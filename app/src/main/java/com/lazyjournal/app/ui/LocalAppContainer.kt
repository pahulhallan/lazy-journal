package com.lazyjournal.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.lazyjournal.app.core.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("Lazy Journal dependencies are not available.")
}
