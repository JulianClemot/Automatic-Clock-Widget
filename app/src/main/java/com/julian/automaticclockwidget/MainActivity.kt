package com.julian.automaticclockwidget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julian.automaticclockwidget.airplanemode.AirplaneModeMonitorService
import com.julian.automaticclockwidget.ui.AppNavigator
import com.julian.automaticclockwidget.ui.home.HomeUiEvent
import com.julian.automaticclockwidget.ui.home.HomeViewModel
import com.julian.automaticclockwidget.ui.theme.AutomaticClockWidgetTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val viewModel: HomeViewModel by viewModel()

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start foreground service
        val serviceIntent = Intent(this, AirplaneModeMonitorService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            AutomaticClockWidgetTheme {
                AppNavigator()
            }
        }
    }
}