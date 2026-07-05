package com.lazyjournal.app.ui.navigation

sealed class Screen(val route: String) {
    data object Record : Screen("record")
    data object Timeline : Screen("timeline")
    data object Search : Screen("search")
    data object Detail : Screen("entry/{entryId}") {
        const val ARG_ENTRY_ID = "entryId"

        fun createRoute(entryId: Long): String = "entry/$entryId"
    }
}
