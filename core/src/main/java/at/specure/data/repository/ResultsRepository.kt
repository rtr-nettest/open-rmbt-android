package at.specure.data.repository

import at.rmbt.client.control.BaseResponse
import at.rmbt.util.Maybe

interface ResultsRepository {

    fun sendTestResults(testUUID: String, callback: (Maybe<BaseResponse>) -> Unit)

    fun sendTestResults(testUUID: String): Maybe<BaseResponse>
}