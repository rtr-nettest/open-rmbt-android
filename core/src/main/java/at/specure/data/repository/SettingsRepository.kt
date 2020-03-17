package at.specure.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun refreshSettings(): Boolean

    fun getTermsAndConditions(): Flow<String>
}
