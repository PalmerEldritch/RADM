package com.jeppe.radm.data.db

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.jeppe.radm.data.db.dao.ActivityDao
import com.jeppe.radm.data.db.dao.DerivedDao
import com.jeppe.radm.data.db.dao.RecordingDao
import com.jeppe.radm.data.db.dao.SchemaDao
import com.jeppe.radm.data.db.entity.ActivityEntity
import com.jeppe.radm.data.db.entity.ActivityProcessorStateEntity
import com.jeppe.radm.data.db.entity.ActivitySummaryEntity
import com.jeppe.radm.data.db.entity.DerivedCadenceEntity
import com.jeppe.radm.data.db.entity.DerivedTrackMetricEntity
import com.jeppe.radm.data.db.entity.PositionSampleEntity
import com.jeppe.radm.data.db.entity.ProcessorDefinitionEntity
import com.jeppe.radm.data.db.entity.RecordingEventEntity
import com.jeppe.radm.data.db.entity.RecordingSessionEntity
import com.jeppe.radm.data.db.entity.StepSampleEntity

@Database(
    entities = [
        ActivityEntity::class,
        RecordingSessionEntity::class,
        RecordingEventEntity::class,
        PositionSampleEntity::class,
        StepSampleEntity::class,
        DerivedTrackMetricEntity::class,
        DerivedCadenceEntity::class,
        ActivitySummaryEntity::class,
        ProcessorDefinitionEntity::class,
        ActivityProcessorStateEntity::class,
    ],
    version = RadmDatabase.VERSION,
    exportSchema = true,
)
abstract class RadmDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun recordingDao(): RecordingDao
    abstract fun derivedDao(): DerivedDao
    abstract fun schemaDao(): SchemaDao

    companion object {
        const val VERSION = 1
        const val DEFAULT_NAME = "radm.db"
    }
}

object RadmDatabaseFactory {
    fun create(context: Context, name: String = RadmDatabase.DEFAULT_NAME): RadmDatabase =
        Room.databaseBuilder(context.applicationContext, RadmDatabase::class.java, name)
            .addMigrations(*RadmMigrations.ALL)
            .addCallback(RadmDatabaseCallback)
            .build()

    fun createInMemory(context: Context): RadmDatabase =
        Room.inMemoryDatabaseBuilder(context.applicationContext, RadmDatabase::class.java)
            .addCallback(RadmDatabaseCallback)
            .build()
}

private object RadmDatabaseCallback : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        seedProcessorDefinitions(connection)
        installInvariantTriggers(connection)
    }

    override suspend fun onOpen(connection: SQLiteConnection) {
        // Migration implementations must also install these artifacts. Keeping
        // this idempotent guards pre-release databases created during M2.
        seedProcessorDefinitions(connection)
        installInvariantTriggers(connection)
    }

    private fun seedProcessorDefinitions(connection: SQLiteConnection) {
        listOf("distance", "pace", "speed", "cadence", "summary").forEach { processor ->
            connection.execSQL(
                "INSERT OR IGNORE INTO processor_definitions(processor_name, current_version) " +
                    "VALUES ('$processor', 1)",
            )
        }
    }

    private fun installInvariantTriggers(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_recording_sessions_singleton_insert
            BEFORE INSERT ON recording_sessions
            WHEN NEW.singleton_id != 1
            BEGIN
                SELECT RAISE(ABORT, 'recording_sessions.singleton_id must equal 1');
            END
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TRIGGER IF NOT EXISTS trg_recording_sessions_singleton_update
            BEFORE UPDATE OF singleton_id ON recording_sessions
            WHEN NEW.singleton_id != 1
            BEGIN
                SELECT RAISE(ABORT, 'recording_sessions.singleton_id must equal 1');
            END
            """.trimIndent(),
        )
        connection.execSQL(uuidTrigger("insert", "INSERT"))
        connection.execSQL(uuidTrigger("update", "UPDATE OF activity_id"))
    }

    private fun uuidTrigger(suffix: String, operation: String): String =
        """
        CREATE TRIGGER IF NOT EXISTS trg_activities_uuid_$suffix
        BEFORE $operation ON activities
        WHEN length(NEW.activity_id) != 36
          OR NEW.activity_id != lower(NEW.activity_id)
          OR substr(NEW.activity_id, 9, 1) != '-'
          OR substr(NEW.activity_id, 14, 1) != '-'
          OR substr(NEW.activity_id, 19, 1) != '-'
          OR substr(NEW.activity_id, 24, 1) != '-'
          OR replace(NEW.activity_id, '-', '') GLOB '*[^0-9a-f]*'
        BEGIN
            SELECT RAISE(ABORT, 'activity_id must be a canonical lowercase UUID');
        END
        """.trimIndent()
}
