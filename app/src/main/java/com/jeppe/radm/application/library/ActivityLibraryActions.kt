package com.jeppe.radm.application.library

import com.jeppe.radm.application.processing.ActivityRecalculationResult
import com.jeppe.radm.application.processing.RecalculateActivity
import com.jeppe.radm.data.repository.ActivityMetadataUpdate
import com.jeppe.radm.data.repository.ActivityRepository
import com.jeppe.radm.domain.model.AbsoluteTimestampUtcMillis
import com.jeppe.radm.domain.model.Activity
import com.jeppe.radm.domain.model.ActivityId
import com.jeppe.radm.domain.model.ActivityLibraryItem
import com.jeppe.radm.domain.model.ActivitySummary
import com.jeppe.radm.domain.model.ActivityType
import com.jeppe.radm.domain.model.ProcessorName

data class SavedActivityDetails(
    val activity: Activity,
    val summary: ActivitySummary?,
)

data class EditSavedActivityResult(
    val activity: Activity,
    val recalculation: ActivityRecalculationResult?,
)

class LoadActivityLibrary(
    private val repository: ActivityRepository,
) {
    suspend operator fun invoke(): List<ActivityLibraryItem> = repository.listLibraryItems()
}

class LoadSavedActivity(
    private val repository: ActivityRepository,
) {
    suspend operator fun invoke(activityId: ActivityId): SavedActivityDetails {
        val activity = requireNotNull(repository.get(activityId)) { "Activity does not exist: $activityId" }
        require(activity.savedAt != null) { "Activity is not saved" }
        val currentSummary = repository.getSummary(activityId).takeIf {
            repository.isProcessorCurrent(activityId, ProcessorName.SUMMARY)
        }
        return SavedActivityDetails(activity, currentSummary)
    }
}

class EditSavedActivity(
    private val repository: ActivityRepository,
    private val recalculateActivity: RecalculateActivity,
) {
    suspend operator fun invoke(
        activityId: ActivityId,
        activityType: ActivityType,
        title: String?,
        notes: String?,
        updatedAt: AbsoluteTimestampUtcMillis,
    ): EditSavedActivityResult {
        val before = requireNotNull(repository.get(activityId)) { "Activity does not exist: $activityId" }
        require(before.savedAt != null) { "Only saved activities can be edited" }
        check(
            repository.updateMetadata(
                activityId,
                ActivityMetadataUpdate(
                    type = activityType,
                    title = title.normalizedOptionalText(),
                    notes = notes.normalizedOptionalText(),
                    updatedAt = updatedAt,
                ),
            ),
        ) { "Activity metadata update failed" }
        val recalculation = if (before.type != activityType) {
            recalculateActivity(activityId, updatedAt)
        } else {
            null
        }
        return EditSavedActivityResult(
            activity = requireNotNull(repository.get(activityId)),
            recalculation = recalculation,
        )
    }
}

class DeleteSavedActivity(
    private val repository: ActivityRepository,
) {
    suspend operator fun invoke(activityId: ActivityId) {
        val activity = requireNotNull(repository.get(activityId)) { "Activity does not exist: $activityId" }
        require(activity.savedAt != null) { "Only saved activities can be deleted here" }
        check(repository.delete(activityId)) { "Activity deletion failed" }
    }
}

private fun String?.normalizedOptionalText(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
