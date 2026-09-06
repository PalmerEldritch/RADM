package com.jeppe.radm.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jeppe.radm.data.repository.ApplicationSettings
import com.jeppe.radm.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore boundary for future specified R00 preferences.
 *
 * No keys are introduced while the baseline defines no adjustable values.
 */
class DataStoreSettingsRepository(
    dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<ApplicationSettings> = dataStore.data.map { ApplicationSettings }
}
