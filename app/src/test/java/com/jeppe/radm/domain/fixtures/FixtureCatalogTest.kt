package com.jeppe.radm.domain.fixtures

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** VVM section 150 and REC section 99 deterministic fixture availability. */
class FixtureCatalogTest {
    @Test
    fun `M1 core fixture catalogue contains every required scenario`() {
        val rows = catalogRows()
        val requiredIds = setOf(
            "continuous_route",
            "irregular_timestamps",
            "initial_without_location",
            "poor_quality_interval",
            "route_gap",
            "manual_pause",
            "recovery_boundary",
            "gross_location_jump",
            "missing_elevation",
            "no_route",
            "running_with_steps",
            "running_without_steps",
            "step_counter_reset",
            "step_counter_unavailable",
            "database_transient_failure",
            "process_interruption",
            "reboot_recovery",
            "large_activity",
            "large_library",
        )

        assertEquals(requiredIds, rows.map { it[0] }.toSet())
    }

    @Test
    fun `committed fixture catalogue references existing assets`() {
        val activitiesDirectory = fixtureActivitiesDirectory()

        catalogRows()
            .filterNot { it[1] == "generator" }
            .forEach { row ->
                assertTrue(
                    "Fixture ${row[0]} references missing asset ${row[2]}",
                    File(activitiesDirectory, row[2]).normalize().exists(),
                )
            }
    }

    private fun catalogRows(): List<List<String>> =
        File(fixtureActivitiesDirectory(), "fixture_catalog.csv")
            .readLines()
            .drop(1)
            .filter(String::isNotBlank)
            .map { it.split(',', limit = 5) }
            .also { rows -> assertTrue(rows.all { it.size == 5 }) }

    private fun fixtureActivitiesDirectory(): File {
        val relativePath = "testdata/activities"
        val workingDirectory = requireNotNull(System.getProperty("user.dir"))
        return generateSequence(File(workingDirectory).absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull(File::isDirectory)
            ?: error("Could not locate $relativePath from the JVM test working directory")
    }
}
