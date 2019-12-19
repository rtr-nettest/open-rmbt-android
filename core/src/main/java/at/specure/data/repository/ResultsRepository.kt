package at.specure.data.repository

import at.rmbt.util.Maybe

interface ResultsRepository {

    fun sendTestResults(testUUID: String, callback: (Maybe<Boolean>) -> Unit)

    fun sendTestResults(testUUID: String): Maybe<Boolean>
}