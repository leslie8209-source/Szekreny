package com.example.ui

import com.example.data.PersonRecord

enum class SortOption {
    NAME_ASC,
    LOCKER_ASC,
    RECENT,
}

enum class FilterMode {
    ALL,
    MULTI_LOCKER,
    MULTI_PHONE,
}

sealed interface SnackbarEvent {
    data class PersonAdded(val name: String) : SnackbarEvent
    data class PersonUpdated(val name: String) : SnackbarEvent
    data class PersonDeleted(val name: String) : SnackbarEvent
}

data class PersonFormState(
    val editingId: Long? = null,
    val name: String = "",
    val note: String = "",
    val lockerInputs: List<String> = listOf(""),
    val phoneInputs: List<String> = listOf(""),
    val showNameError: Boolean = false,
    val showNeedDataError: Boolean = false,
) {
    val isEditing: Boolean
        get() = editingId != null
}

data class LockerDirectoryUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.NAME_ASC,
    val filterMode: FilterMode = FilterMode.ALL,
    val allPeople: List<PersonRecord> = emptyList(),
    val filteredPeople: List<PersonRecord> = emptyList(),
    val totalPeopleCount: Int = 0,
    val totalLockersCount: Int = 0,
    val totalPhonesCount: Int = 0,
    val multiLockerCount: Int = 0,
    val multiPhoneCount: Int = 0,
    val isFormVisible: Boolean = false,
    val formState: PersonFormState = PersonFormState(),
    val duplicateLockerOwners: Map<Int, String> = emptyMap(),
    val personPendingDelete: PersonRecord? = null,
    val snackbarEvent: SnackbarEvent? = null,
)
