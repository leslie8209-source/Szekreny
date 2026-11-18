package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PersonRecord
import com.example.ui.FilterMode
import com.example.ui.LockerViewModel
import com.example.ui.SnackbarEvent
import com.example.ui.components.DirectoryHeaderBanner
import com.example.ui.components.EmptyDirectoryState
import com.example.ui.components.NoSearchResultsState
import com.example.ui.components.PersonCard
import com.example.ui.components.PersonFormSheet
import kotlinx.coroutines.launch

@Composable
fun DirectoryScreen(
    viewModel: LockerViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.snackbarEvent) {
        val event = uiState.snackbarEvent ?: return@LaunchedEffect
        val message = when (event) {
            is SnackbarEvent.PersonAdded -> context.getString(R.string.snackbar_person_added, event.name)
            is SnackbarEvent.PersonUpdated -> context.getString(R.string.snackbar_person_updated, event.name)
            is SnackbarEvent.PersonDeleted -> context.getString(R.string.snackbar_person_deleted, event.name)
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSnackbarEvent()
    }

    val onCallPhone: (String) -> Unit = remember(context, snackbarHostState, scope) {
        { phone ->
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
            if (dialIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(dialIntent)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.dialer_unavailable_message),
                    )
                }
            }
        }
    }

    val onCopyPhone: (String) -> Unit = remember(context, snackbarHostState, scope) {
        { phone ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("phone", phone))
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.phone_copied_message, phone),
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCreateForm() },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.fab_add_person)) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_person_fab"),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DirectoryHeaderBanner(
                totalPeople = uiState.totalPeopleCount,
                totalLockers = uiState.totalLockersCount,
                totalPhones = uiState.totalPhonesCount,
                searchQuery = uiState.searchQuery,
                sortOption = uiState.sortOption,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onClearSearch = viewModel::clearSearch,
                onSortOptionSelected = viewModel::onSortOptionSelected,
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
            ) {
                val isWideScreen = maxWidth >= 700.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 960.dp),
                ) {
                    // Filter Chips + Live Search Count
                    AnimatedVisibility(visible = uiState.totalPeopleCount > 0) {
                        FilterAndCountBar(
                            filterMode = uiState.filterMode,
                            totalCount = uiState.totalPeopleCount,
                            multiLockerCount = uiState.multiLockerCount,
                            multiPhoneCount = uiState.multiPhoneCount,
                            filteredCount = uiState.filteredPeople.size,
                            isFilteredOrSearching = uiState.searchQuery.isNotBlank() ||
                                uiState.filterMode != FilterMode.ALL,
                            onFilterSelected = viewModel::onFilterModeSelected,
                        )
                    }

                    when {
                        uiState.isLoading -> {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        uiState.totalPeopleCount == 0 -> {
                            EmptyDirectoryState(
                                onAddFirstPerson = { viewModel.openCreateForm() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        uiState.filteredPeople.isEmpty() -> {
                            NoSearchResultsState(
                                searchQuery = uiState.searchQuery,
                                hasActiveFilter = uiState.filterMode != FilterMode.ALL,
                                onClearSearch = viewModel::clearSearch,
                                onResetFilter = { viewModel.onFilterModeSelected(FilterMode.ALL) },
                                onCreateWithQuery = { viewModel.openCreateForm(prefillName = uiState.searchQuery) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        isWideScreen -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 8.dp,
                                    bottom = 112.dp,
                                ),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .testTag("people_grid"),
                            ) {
                                items(
                                    items = uiState.filteredPeople,
                                    key = { it.id },
                                ) { person ->
                                    PersonCard(
                                        person = person,
                                        onEditClick = { viewModel.openEditForm(person) },
                                        onDeleteClick = { viewModel.requestDelete(person) },
                                        onCallPhone = onCallPhone,
                                        onCopyPhone = onCopyPhone,
                                    )
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 112.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .testTag("people_list"),
                            ) {
                                items(
                                    items = uiState.filteredPeople,
                                    key = { it.id },
                                ) { person ->
                                    PersonCard(
                                        person = person,
                                        onEditClick = { viewModel.openEditForm(person) },
                                        onDeleteClick = { viewModel.requestDelete(person) },
                                        onCallPhone = onCallPhone,
                                        onCopyPhone = onCopyPhone,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isFormVisible) {
        PersonFormSheet(
            formState = uiState.formState,
            duplicateLockerOwners = uiState.duplicateLockerOwners,
            onNameChange = viewModel::onFormNameChange,
            onNoteChange = viewModel::onFormNoteChange,
            onLockerChange = viewModel::onLockerInputChange,
            onAddLocker = viewModel::addLockerInput,
            onRemoveLocker = viewModel::removeLockerInput,
            onPhoneChange = viewModel::onPhoneInputChange,
            onAddPhone = viewModel::addPhoneInput,
            onRemovePhone = viewModel::removePhoneInput,
            onSave = viewModel::saveForm,
            onDelete = viewModel::requestDeleteFromForm,
            onDismiss = viewModel::closeForm,
        )
    }

    uiState.personPendingDelete?.let { target ->
        DeletePersonDialog(
            person = target,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete,
        )
    }
}

@Composable
private fun FilterAndCountBar(
    filterMode: FilterMode,
    totalCount: Int,
    multiLockerCount: Int,
    multiPhoneCount: Int,
    filteredCount: Int,
    isFilteredOrSearching: Boolean,
    onFilterSelected: (FilterMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = filterMode == FilterMode.ALL,
                    onClick = { onFilterSelected(FilterMode.ALL) },
                    label = { Text("${stringResource(R.string.filter_all)} ($totalCount)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag("filter_all_chip"),
                )
            }
            item {
                FilterChip(
                    selected = filterMode == FilterMode.MULTI_LOCKER,
                    onClick = { onFilterSelected(FilterMode.MULTI_LOCKER) },
                    label = { Text("${stringResource(R.string.filter_multi_locker)} ($multiLockerCount)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    modifier = Modifier.testTag("filter_multi_locker_chip"),
                )
            }
            item {
                FilterChip(
                    selected = filterMode == FilterMode.MULTI_PHONE,
                    onClick = { onFilterSelected(FilterMode.MULTI_PHONE) },
                    label = { Text("${stringResource(R.string.filter_multi_phone)} ($multiPhoneCount)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    modifier = Modifier.testTag("filter_multi_phone_chip"),
                )
            }
        }

        if (isFilteredOrSearching) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.search_results_count, filteredCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DeletePersonDialog(
    person: PersonRecord,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.delete_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_dialog_message, person.name),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.testTag("confirm_delete_button"),
            ) {
                Text(stringResource(R.string.delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
