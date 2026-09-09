package com.jeppe.radm.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jeppe.radm.RadmContainer
import com.jeppe.radm.application.analysis.LoadActivityAnalysis
import com.jeppe.radm.application.library.DeleteSavedActivity
import com.jeppe.radm.application.library.EditSavedActivity
import com.jeppe.radm.application.library.LoadActivityLibrary
import com.jeppe.radm.domain.analysis.ActivityAnalysisData
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityLibraryItem
import com.jeppe.radm.domain.model.ActivityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActivityLibraryUiState(
    val items: List<ActivityLibraryItem> = emptyList(),
    val selected: ActivityAnalysisData? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
)

class ActivityLibraryViewModel(
    private val loadLibrary: LoadActivityLibrary,
    private val loadActivityAnalysis: LoadActivityAnalysis,
    private val editSavedActivity: EditSavedActivity,
    private val deleteSavedActivity: DeleteSavedActivity,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ActivityLibraryUiState())
    val state: StateFlow<ActivityLibraryUiState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            runCatching { withContext(Dispatchers.IO) { loadLibrary() } }
                .onSuccess { items -> mutableState.update { it.copy(items = items, loading = false) } }
                .onFailure(::publishFailure)
        }
    }

    fun open(activityId: ActivityId) {
        viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            runCatching { withContext(Dispatchers.IO) { loadActivityAnalysis(activityId) } }
                .onSuccess { selected -> mutableState.update { it.copy(selected = selected, loading = false) } }
                .onFailure(::publishFailure)
        }
    }

    fun closeActivity() {
        mutableState.update { it.copy(selected = null, error = null) }
    }

    fun edit(
        activityType: ActivityType,
        title: String?,
        notes: String?,
    ) {
        val activityId = state.value.selected?.activity?.id ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            runCatching {
                withContext(Dispatchers.Default) {
                    editSavedActivity(
                        activityId = activityId,
                        activityType = activityType,
                        title = title,
                        notes = notes,
                        updatedAt = AbsoluteTimestampUtcMillis(System.currentTimeMillis()),
                    )
                }
                withContext(Dispatchers.IO) {
                    loadActivityAnalysis(activityId) to loadLibrary()
                }
            }.onSuccess { (selected, items) ->
                mutableState.update {
                    it.copy(items = items, selected = selected, saving = false)
                }
            }.onFailure(::publishFailure)
        }
    }

    fun deleteSelected() {
        val activityId = state.value.selected?.activity?.id ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    deleteSavedActivity(activityId)
                    loadLibrary()
                }
            }.onSuccess { items ->
                mutableState.value = ActivityLibraryUiState(items = items, loading = false)
            }.onFailure(::publishFailure)
        }
    }

    private fun publishFailure(failure: Throwable) {
        mutableState.update {
            it.copy(
                loading = false,
                saving = false,
                error = failure.message ?: "Activity operation failed",
            )
        }
    }

    companion object {
        fun factory(container: RadmContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ActivityLibraryViewModel(
                        container.loadActivityLibrary,
                        container.loadActivityAnalysis,
                        container.editSavedActivity,
                        container.deleteSavedActivity,
                    ) as T
            }
    }
}
