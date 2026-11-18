package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PersonRecord
import com.example.data.PersonRepository
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LockerViewModel(
    private val repository: PersonRepository,
) : ViewModel() {

    private val hungarianCollator: Collator = Collator.getInstance(Locale.forLanguageTag("hu")).apply {
        strength = Collator.PRIMARY
    }

    private data class UiControlState(
        val searchQuery: String = "",
        val sortOption: SortOption = SortOption.NAME_ASC,
        val filterMode: FilterMode = FilterMode.ALL,
        val isFormVisible: Boolean = false,
        val formState: PersonFormState = PersonFormState(),
        val personPendingDelete: PersonRecord? = null,
        val snackbarEvent: SnackbarEvent? = null,
    )

    private val controlState = MutableStateFlow(UiControlState())

    val uiState: StateFlow<LockerDirectoryUiState> = combine(
        repository.allPeople,
        controlState,
    ) { people, controls ->
        val filtered = people
            .filter { person ->
                val matchesFilter = when (controls.filterMode) {
                    FilterMode.ALL -> true
                    FilterMode.MULTI_LOCKER -> person.lockerNumbers.size >= 2
                    FilterMode.MULTI_PHONE -> person.phoneNumbers.size >= 2
                }
                matchesFilter && person.matchesQuery(controls.searchQuery)
            }
            .let { list ->
                when (controls.sortOption) {
                    SortOption.NAME_ASC -> list.sortedWith { a, b ->
                        hungarianCollator.compare(a.name, b.name)
                    }
                    SortOption.LOCKER_ASC -> list.sortedWith(
                        compareBy<PersonRecord> { person ->
                            person.lockerNumbers.firstOrNull()?.let { extractNaturalLockerSortKey(it).first }
                                ?: Int.MAX_VALUE
                        }.thenBy { person ->
                            person.lockerNumbers.firstOrNull()?.let { extractNaturalLockerSortKey(it).second }
                                ?: "\uffff"
                        }.thenComparator { a, b ->
                            hungarianCollator.compare(a.name, b.name)
                        },
                    )
                    SortOption.RECENT -> list.sortedByDescending { it.updatedAt }
                }
            }

        val duplicateOwners = if (controls.isFormVisible) {
            findDuplicateLockerOwners(
                people = people,
                editingId = controls.formState.editingId,
                lockerInputs = controls.formState.lockerInputs,
            )
        } else {
            emptyMap()
        }

        LockerDirectoryUiState(
            isLoading = false,
            searchQuery = controls.searchQuery,
            sortOption = controls.sortOption,
            filterMode = controls.filterMode,
            allPeople = people,
            filteredPeople = filtered,
            totalPeopleCount = people.size,
            totalLockersCount = people.sumOf { it.lockerNumbers.size },
            totalPhonesCount = people.sumOf { it.phoneNumbers.size },
            multiLockerCount = people.count { it.lockerNumbers.size >= 2 },
            multiPhoneCount = people.count { it.phoneNumbers.size >= 2 },
            isFormVisible = controls.isFormVisible,
            formState = controls.formState,
            duplicateLockerOwners = duplicateOwners,
            personPendingDelete = controls.personPendingDelete,
            snackbarEvent = controls.snackbarEvent,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LockerDirectoryUiState(),
    )

    private fun findDuplicateLockerOwners(
        people: List<PersonRecord>,
        editingId: Long?,
        lockerInputs: List<String>,
    ): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        lockerInputs.forEachIndexed { index, raw ->
            val cleaned = raw.trim().removePrefix("#").trim()
            if (cleaned.isNotEmpty()) {
                val normalized = normalizeHungarianSearch(cleaned)
                val owner = people.firstOrNull { person ->
                    person.id != editingId &&
                        person.lockerNumbers.any { normalizeHungarianSearch(it) == normalized }
                }
                if (owner != null) {
                    result[index] = owner.name
                }
            }
        }
        return result
    }

    fun onSearchQueryChange(query: String) {
        controlState.update { it.copy(searchQuery = query) }
    }

    fun clearSearch() {
        controlState.update { it.copy(searchQuery = "") }
    }

    fun onSortOptionSelected(option: SortOption) {
        controlState.update { it.copy(sortOption = option) }
    }

    fun onFilterModeSelected(mode: FilterMode) {
        controlState.update { it.copy(filterMode = mode) }
    }

    fun openCreateForm(prefillName: String = "") {
        controlState.update {
            it.copy(
                isFormVisible = true,
                formState = PersonFormState(
                    editingId = null,
                    name = prefillName.trim(),
                    note = "",
                    lockerInputs = listOf(""),
                    phoneInputs = listOf(""),
                ),
            )
        }
    }

    fun openEditForm(person: PersonRecord) {
        controlState.update {
            it.copy(
                isFormVisible = true,
                formState = PersonFormState(
                    editingId = person.id,
                    name = person.name,
                    note = person.note,
                    lockerInputs = person.lockerNumbers.ifEmpty { listOf("") },
                    phoneInputs = person.phoneNumbers.ifEmpty { listOf("") },
                ),
            )
        }
    }

    fun closeForm() {
        controlState.update {
            it.copy(
                isFormVisible = false,
                formState = PersonFormState(),
            )
        }
    }

    fun onFormNameChange(value: String) {
        controlState.update { state ->
            state.copy(
                formState = state.formState.copy(
                    name = value,
                    showNameError = false,
                ),
            )
        }
    }

    fun onFormNoteChange(value: String) {
        controlState.update { state ->
            state.copy(formState = state.formState.copy(note = value))
        }
    }

    fun onLockerInputChange(index: Int, value: String) {
        controlState.update { state ->
            val updated = state.formState.lockerInputs.toMutableList()
            if (index in updated.indices) {
                updated[index] = value
            }
            state.copy(
                formState = state.formState.copy(
                    lockerInputs = updated,
                    showNeedDataError = false,
                ),
            )
        }
    }

    fun addLockerInput() {
        controlState.update { state ->
            state.copy(
                formState = state.formState.copy(
                    lockerInputs = state.formState.lockerInputs + "",
                ),
            )
        }
    }

    fun removeLockerInput(index: Int) {
        controlState.update { state ->
            val current = state.formState.lockerInputs
            val updated = if (current.size <= 1) {
                listOf("")
            } else {
                current.filterIndexed { i, _ -> i != index }
            }
            state.copy(formState = state.formState.copy(lockerInputs = updated))
        }
    }

    fun onPhoneInputChange(index: Int, value: String) {
        controlState.update { state ->
            val updated = state.formState.phoneInputs.toMutableList()
            if (index in updated.indices) {
                updated[index] = value
            }
            state.copy(
                formState = state.formState.copy(
                    phoneInputs = updated,
                    showNeedDataError = false,
                ),
            )
        }
    }

    fun addPhoneInput() {
        controlState.update { state ->
            state.copy(
                formState = state.formState.copy(
                    phoneInputs = state.formState.phoneInputs + "",
                ),
            )
        }
    }

    fun removePhoneInput(index: Int) {
        controlState.update { state ->
            val current = state.formState.phoneInputs
            val updated = if (current.size <= 1) {
                listOf("")
            } else {
                current.filterIndexed { i, _ -> i != index }
            }
            state.copy(formState = state.formState.copy(phoneInputs = updated))
        }
    }

    fun saveForm() {
        val form = controlState.value.formState
        val trimmedName = form.name.trim()
        val hasLockers = form.lockerInputs.any { it.trim().removePrefix("#").trim().isNotEmpty() }
        val hasPhones = form.phoneInputs.any { it.trim().isNotEmpty() }

        if (trimmedName.isEmpty() || (!hasLockers && !hasPhones)) {
            controlState.update { state ->
                state.copy(
                    formState = state.formState.copy(
                        showNameError = trimmedName.isEmpty(),
                        showNeedDataError = !hasLockers && !hasPhones,
                    ),
                )
            }
            return
        }

        val editingId = form.editingId
        viewModelScope.launch {
            repository.savePerson(
                personId = editingId,
                name = trimmedName,
                note = form.note,
                lockerNumbers = form.lockerInputs,
                phoneNumbers = form.phoneInputs,
            )
            controlState.update { state ->
                state.copy(
                    isFormVisible = false,
                    formState = PersonFormState(),
                    snackbarEvent = if (editingId == null) {
                        SnackbarEvent.PersonAdded(trimmedName)
                    } else {
                        SnackbarEvent.PersonUpdated(trimmedName)
                    },
                )
            }
        }
    }

    fun requestDelete(person: PersonRecord) {
        controlState.update { it.copy(personPendingDelete = person) }
    }

    fun requestDeleteFromForm() {
        val editingId = controlState.value.formState.editingId ?: return
        val person = uiState.value.allPeople.firstOrNull { it.id == editingId } ?: return
        controlState.update {
            it.copy(
                isFormVisible = false,
                formState = PersonFormState(),
                personPendingDelete = person,
            )
        }
    }

    fun cancelDelete() {
        controlState.update { it.copy(personPendingDelete = null) }
    }

    fun confirmDelete() {
        val target = controlState.value.personPendingDelete ?: return
        viewModelScope.launch {
            repository.deletePerson(target.id)
            controlState.update { state ->
                state.copy(
                    personPendingDelete = null,
                    snackbarEvent = SnackbarEvent.PersonDeleted(target.name),
                )
            }
        }
    }

    fun consumeSnackbarEvent() {
        controlState.update { it.copy(snackbarEvent = null) }
    }

    companion object {
        fun factory(repository: PersonRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LockerViewModel(repository) as T
                }
            }
    }
}
