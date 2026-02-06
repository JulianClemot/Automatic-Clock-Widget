package com.julian.automaticclockwidget.ui.home

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Composable
fun HomeEntryPoint(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state) { event -> viewModel.onEvent(event) }
}

@Composable
fun HomeContent(state: HomeUiState, onEvent: (HomeUiEvent) -> Unit) {
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackBarHostState.showSnackbar(it)
            onEvent(HomeUiEvent.DismissError)
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackBarHostState.showSnackbar(it)
            onEvent(HomeUiEvent.DismissSuccess)
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally,
        ) {
            Button(onClick = {
                onEvent(HomeUiEvent.ManualRefresh)
            }) {
                Text("Refresh now and update widget")
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
                            onEvent(HomeUiEvent.AddUrl(newUrl))
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
                        text = if (isSelected) "$url (selected)" else url,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onEvent(HomeUiEvent.SelectUrl(url)) }) {
                        Text(
                            "Select"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onEvent(HomeUiEvent.DeleteUrl(url)) }) {
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