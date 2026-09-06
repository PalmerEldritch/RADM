package com.jeppe.radm.data.db

import androidx.room3.migration.Migration

/**
 * Explicit migration registry. Version 1 is the initial schema, so there is no
 * prior released RADM database to migrate yet.
 */
object RadmMigrations {
    val ALL: Array<Migration> = emptyArray()
}
