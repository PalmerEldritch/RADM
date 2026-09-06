package com.jeppe.radm.domain.model

import java.util.UUID

@JvmInline
value class ActivityId private constructor(val value: String) {
    companion object {
        fun parse(value: String): ActivityId {
            val parsed = runCatching { UUID.fromString(value) }
                .getOrElse { throw IllegalArgumentException("Activity ID must be a UUID", it) }
            val canonical = parsed.toString()
            require(value == canonical) { "Activity ID must use canonical lowercase UUID text" }
            return ActivityId(canonical)
        }
    }

    override fun toString(): String = value
}

enum class ActivityType {
    RUNNING,
    CYCLING,
    CROSS_COUNTRY_SKIING,
}

@JvmInline
value class ProvenanceType(val value: String) {
    init {
        require(value.isNotBlank()) { "Provenance type must not be blank" }
    }

    companion object {
        val RADM_NATIVE = ProvenanceType("RADM_NATIVE")
    }
}

data class Activity(
    val id: ActivityId,
    val provenanceType: ProvenanceType,
    val provenanceExternalId: String? = null,
    val type: ActivityType,
    val title: String? = null,
    val notes: String? = null,
    val startedAt: AbsoluteTimestampUtcMillis,
    val endedAt: AbsoluteTimestampUtcMillis? = null,
    val savedAt: AbsoluteTimestampUtcMillis? = null,
    val activeDuration: ActiveElapsedTimeMillis = ActiveElapsedTimeMillis.ZERO,
    val createdAt: AbsoluteTimestampUtcMillis,
    val updatedAt: AbsoluteTimestampUtcMillis,
)
