package com.jeppe.radm.data.repository

import kotlinx.coroutines.flow.Flow

/** R00 currently defines no user-adjustable setting values. */
data object ApplicationSettings

interface SettingsRepository {
    val settings: Flow<ApplicationSettings>
}
