package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.util.Maybe
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.TestResultRecord

interface TestResultsRepository {

    fun getQoEItems(testOpenUUID: String): LiveData<List<QoeInfoRecord>>

    fun getServerTestResult(testUUID: String): LiveData<TestResultRecord?>

    fun loadTestResults(testUUID: String, callBack: (Maybe<Boolean>) -> Unit)
}