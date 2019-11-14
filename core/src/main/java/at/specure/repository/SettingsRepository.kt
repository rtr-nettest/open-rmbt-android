package at.specure.repository

interface SettingsRepository {

    fun refreshSettings(): Boolean
}
