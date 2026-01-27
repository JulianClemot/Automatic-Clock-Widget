package com.julian.automaticclockwidget.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.julian.automaticclockwidget.ui.home.HomeEntryPoint
import com.julian.automaticclockwidget.ui.home.HomeRoute
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigator() {
    val backStack = rememberNavBackStack(HomeRoute)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeEntryPoint(koinViewModel())
            }
        }
    )
}