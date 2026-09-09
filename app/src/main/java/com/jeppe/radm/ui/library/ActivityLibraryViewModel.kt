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
import com.jeppe.radm.domain.analysis.ActivityAnalysisInteraction
import com.jeppe.radm.domain.analysis.ActivityAnalysisInteractionSnapshot
import com.jeppe.radm.domain.analysis.AnalysisCoordinateMode
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
    val analysisInteraction: ActivityAnalysisInteractionSnapshot? = null,
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
    private var interaction: ActivityAnalysisInteraction? = null
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
            runCatching {
                val selected = withContext(Dispatchers.IO) { loadActivityAnalysis(activityId) }
                val interaction = withContext(Dispatchers.Default) {
                    ActivityAnalysisInteraction(selected)
                }
                selected to interaction
            }.onSuccess { (selected, loadedInteraction) ->
                interaction = loadedInteraction
                mutableState.update {
                    it.copy(
                        selected = selected,
                        analysisInteraction = loadedInteraction.snapshot,
                        loading = false,
                    )
                }
            }
                .onFailure(::publishFailure)
        }
    }

    fun closeActivity() {
        interaction = null
        mutableState.update { it.copy(selected = null, analysisInteraction = null, error = null) }
    }

    fun selectAnalysisFraction(fraction: Double) {
        updateInteraction { selectVisibleFraction(fraction) }
    }

    fun setAnalysisCoordinateMode(mode: AnalysisCoordinateMode) {
        updateInteraction { setCoordinateMode(mode) }
    }

    fun setAnalysisRange(startFraction: Double, endFraction: Double) {
        updateInteraction { setRangeFractions(startFraction, endFraction) }
    }

    fun restoreFullAnalysisRange() {
        updateInteraction { restoreFullRange() }
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
                val selectedAndItems = withContext(Dispatchers.IO) {
                    loadActivityAnalysis(activityId) to loadLibrary()
                }
                val loadedInteraction = withContext(Dispatchers.Default) {
                    ActivityAnalysisInteraction(selectedAndItems.first)
                }
                Triple(selectedAndItems.first, selectedAndItems.second, loadedInteraction)
            }.onSuccess { (selected, items, loadedInteraction) ->
                interaction = loadedInteraction
                mutableState.update {
                    it.copy(
                        items = items,
                        selected = selected,
                        analysisInteraction = loadedInteraction.snapshot,
                        saving = false,
                    )
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
                interaction = null
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

    private inline fun updateInteraction(
        update: ActivityAnalysisInteraction.() -> ActivityAnalysisInteractionSnapshot,
    ) {
        val interaction = interaction ?: return
        val snapshot = interaction.update()
        mutableState.update { it.copy(analysisInteraction = snapshot) }
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
