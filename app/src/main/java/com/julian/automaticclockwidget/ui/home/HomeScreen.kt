package com.julian.automaticclockwidget.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.julian.automaticclockwidget.designsystem.AppSpacing
import com.julian.automaticclockwidget.designsystem.components.AppBadge
import com.julian.automaticclockwidget.designsystem.components.AppButton
import com.julian.automaticclockwidget.designsystem.components.AppCard
import com.julian.automaticclockwidget.designsystem.components.AppInfoHint
import com.julian.automaticclockwidget.designsystem.components.AppTextField
import com.julian.automaticclockwidget.designsystem.components.AppTextButton
import com.julian.automaticclockwidget.designsystem.components.PasteTextField
import com.julian.automaticclockwidget.settings.CalendarEntry
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute : NavKey

@Composable
fun HomeEntryPoint(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state) { event -> viewModel.onEvent(event) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(state: HomeUiState, onEvent: (HomeUiEvent) -> Unit) {
    val snackBarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddSheet by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

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

    if (state.deletionState is DeletionState.ConfirmationPending) {
        AlertDialog(
            onDismissRequest = { onEvent(HomeUiEvent.DismissDeleteConfirmation) },
            title = { Text("Delete calendar?") },
            text = { Text("This will remove the calendar from your list.") },
            confirmButton = {
                TextButton(onClick = { onEvent(HomeUiEvent.DeleteUrl(state.deletionState.url)) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(HomeUiEvent.DismissDeleteConfirmation) }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showAddSheet) {
        val dismiss = {
            showAddSheet = false
            newName = ""
            newUrl = ""
            onEvent(HomeUiEvent.ValidateAddUrl(""))
        }
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = sheetState,
        ) {
            AddCalendarSheetContent(
                name = newName,
                url = newUrl,
                urlError = state.addUrlError,
                onNameChange = { newName = it },
                onUrlChange = {
                    newUrl = it
                    onEvent(HomeUiEvent.ValidateAddUrl(it))
                },
                onAdd = {
                    onEvent(HomeUiEvent.AddCalendar(newName, newUrl))
                    dismiss()
                },
                onDismiss = dismiss,
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add calendar")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Calendars",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                AppBadge(text = "${state.entries.size} Active")
            }

            state.entries.forEach { entry ->
                val isSelected = state.selected?.equals(entry.url, ignoreCase = true) == true
                if (isSelected) {
                    SelectedCalendarCard(
                        entry = entry,
                        refreshState = state.refreshState,
                        onRefresh = { onEvent(HomeUiEvent.ManualRefresh) },
                        onDelete = { onEvent(HomeUiEvent.RequestDeleteUrl(entry.url)) },
                    )
                } else {
                    UnselectedCalendarCard(
                        entry = entry,
                        onSelect = { onEvent(HomeUiEvent.SelectUrl(entry.url)) },
                        onDelete = { onEvent(HomeUiEvent.RequestDeleteUrl(entry.url)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun AddCalendarSheetContent(
    name: String,
    url: String,
    urlError: String?,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg)
            .padding(bottom = AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Add Calendar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Text(
            text = "Paste the ICS or Webcal URL of the calendar you want to sync to your widget.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PasteTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = "ICS or Webcal URL",
            placeholder = "webcal:// or https://",
            isError = urlError != null,
            supportingText = urlError?.let { { Text(it) } },
        )

        AppTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = "Nickname (optional)",
            placeholder = "e.g., Gym Schedule",
        )

        AppInfoHint(
            text = "Common URLs start with webcal:// or https://. Make sure your calendar is set to \"Public\" or \"Published\" to sync correctly.",
        )

        AppButton(
            text = "Add to List",
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            enabled = url.isNotBlank() && urlError == null,
            leadingIcon = Icons.Default.Add,
        )

        AppTextButton(
            text = "Cancel",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectedCalendarCard(
    entry: CalendarEntry,
    refreshState: RefreshState,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh-rotation")
    val refreshRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "refresh-icon-rotation",
    )
    val iconRotation = if (refreshState is RefreshState.Refreshing) refreshRotation else 0f

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        selected = true,
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            CalendarCardHeader(entry = entry, isSelected = true, onDelete = onDelete)
            AppButton(
                text = "Refresh and Update Widget",
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Sync,
            )
        }
    }
}

@Composable
private fun UnselectedCalendarCard(
    entry: CalendarEntry,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)) {
            CalendarCardHeader(entry = entry, isSelected = false, onDelete = onDelete)
        }
    }
}

@Composable
private fun CalendarCardHeader(
    entry: CalendarEntry,
    isSelected: Boolean,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(AppSpacing.sm))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            ) {
                Text(
                    text = entry.name.ifBlank { entry.url },
                    style = if (isSelected) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isSelected) {
                    AppBadge(
                        text = "DEFAULT",
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (entry.name.isNotBlank()) {
                Text(
                    text = entry.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete calendar",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
