package com.jeppe.radm.data.db.dao

import androidx.room3.Dao
import androidx.room3.Query

@Dao
interface SchemaDao {
    @Query("SELECT name FROM sqlite_master WHERE type = :type ORDER BY name")
    suspend fun listSchemaObjects(type: String): List<String>

    @Query("SELECT COUNT(*) FROM recording_sessions")
    suspend fun recordingSessionCount(): Int
}
