package com.jeppe.radm.data

import android.content.Context
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jeppe.radm.data.db.RadmDatabase
import com.jeppe.radm.data.db.RadmDatabaseFactory
import com.jeppe.radm.data.repository.RoomActivityRepository
import com.jeppe.radm.domain.model.ProcessorName
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Initial-schema proof for the VVM-MIG infrastructure; no prior release exists. */
@RunWith(AndroidJUnit4::class)
class RadmMigrationInfrastructureTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = context.getDatabasePath(DATABASE_NAME),
        driver = AndroidSQLiteDriver(),
        databaseClass = RadmDatabase::class,
        databaseFactory = { RadmDatabaseFactory.create(context, DATABASE_NAME) },
    )

    @Before
    fun removePreviousTestDatabase() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun cleanUpTestDatabase() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun initialVersionSchema_canBeCreatedValidatedAndOpenedByRoom() {
        runBlocking {
            val connection = migrationHelper.createDatabase(RadmDatabase.VERSION)
            try {
                val statement = connection.prepare(
                    "SELECT name FROM sqlite_master WHERE type = 'table' " +
                        "AND name NOT IN ('android_metadata', 'room_master_table') " +
                        "AND name NOT LIKE 'sqlite_%'",
                )
                try {
                    val tables = buildSet {
                        while (statement.step()) add(statement.getText(0))
                    }
                    assertEquals(
                        setOf(
                            "activities",
                            "recording_sessions",
                            "recording_events",
                            "position_samples",
                            "step_samples",
                            "derived_track_metrics",
                            "derived_cadence",
                            "activity_summaries",
                            "processor_definitions",
                            "activity_processor_state",
                        ),
                        tables,
                    )
                } finally {
                    statement.close()
                }
            } finally {
                connection.close()
            }

            val database = RadmDatabaseFactory.create(context, DATABASE_NAME)
            try {
                assertNotNull(
                    RoomActivityRepository(database).getProcessorDefinition(ProcessorName.DISTANCE),
                )
            } finally {
                database.close()
            }
        }
    }

    private companion object {
        const val DATABASE_NAME = "radm-migration-test.db"
    }
}
