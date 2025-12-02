package com.julian.automaticclockwidget

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.julian.automaticclockwidget.ui.theme.AutomaticClockWidgetTheme
import com.julian.automaticclockwidget.widgets.AutomaticClockWidgetReceiver
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by viewModel()

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                GlanceAppWidgetManager(this@MainActivity).setWidgetPreviews(
                    AutomaticClockWidgetReceiver::class
                )
            }
        }
        setContent {
            AutomaticClockWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = CenterHorizontally,
                    ) {
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        Button(onClick = {
                            viewModel.onEvent(MainUiEvent.ManualRefresh)
                        }) {
                            Text("Refresh now and update widget")
                        }

                        // Error banner
                        state.errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = msg, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.onEvent(MainUiEvent.DismissError) }) {
                                    Text(
                                        "Dismiss"
                                    )
                                }
                            }
                        }

                        // Success banner
                        state.successMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = msg, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.onEvent(MainUiEvent.DismissSuccess) }) {
                                    Text(
                                        "OK"
                                    )
                                }
                            }
                        }

                        // URL Management UI
                        var newUrl by remember { mutableStateOf("") }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Manage ICS URLs")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextField(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Enter ICS URL") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newUrl.isNotBlank()) {
                                        viewModel.onEvent(MainUiEvent.AddUrl(newUrl))
                                        newUrl = ""
                                    }
                                },
                                enabled = newUrl.isNotBlank()
                            ) { Text("Add") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // List URLs with Select/Delete
                        state.urls.forEach { url ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                val isSelected =
                                    state.selected?.equals(url, ignoreCase = true) == true
                                Text(
                                    text = if (isSelected) "${url} (selected)" else url,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.onEvent(MainUiEvent.SelectUrl(url)) }) {
                                    Text(
                                        "Select"
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.onEvent(MainUiEvent.DeleteUrl(url)) }) {
                                    Text(
                                        "Delete"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AutomaticClockWidgetTheme {
    }
}