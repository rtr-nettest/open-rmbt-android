package at.specure.data.repository

interface SettingsRepository {

    fun refreshSettings(): Boolean

    fun refreshSettings(success: (Boolean) -> Unit)
}
