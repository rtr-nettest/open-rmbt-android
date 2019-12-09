package at.specure.data.repository

import at.rmbt.util.exception.HandledException

interface ResultsRepository {

    @Throws(HandledException::class)
    fun sendTestResults(testUUID: String)
}