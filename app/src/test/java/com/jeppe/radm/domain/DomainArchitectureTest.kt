package com.jeppe.radm.domain

import com.jeppe.radm.domain.fixtures.DeterministicFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** VVM-ARCH-001 and VVM-ARCH-002. */
class DomainArchitectureTest {
    @Test
    fun `VVM ARCH 001 domain semantics execute as ordinary JVM tests`() {
        assertTrue(DeterministicFixtures.continuousRoute.isNotEmpty())
    }

    @Test
    fun `VVM ARCH 002 domain source has no prohibited framework dependencies`() {
        val domainDirectory = findDomainDirectory()
        val prohibitedImports = listOf(
            "import android.",
            "import androidx.",
            "android.location.Location",
            "android.hardware.SensorEvent",
            "androidx.room.",
            "androidx.compose.",
            "import org.maplibre.",
            "import com.patrykandpatrick.vico.",
        )

        domainDirectory.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { source ->
                val content = source.readText()
                prohibitedImports.forEach { prohibited ->
                    assertFalse("${source.path} contains $prohibited", content.contains(prohibited))
                }
            }
    }

    private fun findDomainDirectory(): File {
        val relativePath = "app/src/main/java/com/jeppe/radm/domain"
        val workingDirectory = requireNotNull(System.getProperty("user.dir"))
        return generateSequence(File(workingDirectory).absoluteFile) { it.parentFile }
            .map { File(it, relativePath) }
            .firstOrNull(File::isDirectory)
            ?: error("Could not locate $relativePath from the JVM test working directory")
    }
}
