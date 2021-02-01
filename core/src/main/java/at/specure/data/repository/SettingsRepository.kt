package at.specure.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun refreshSettings(): Boolean

    fun refreshSettingsByFlow(): Flow<Boolean>

    fun getTermsAndConditions(): Flow<String>
}
