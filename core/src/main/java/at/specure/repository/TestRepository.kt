package at.specure.repository

import androidx.lifecycle.MutableLiveData
import at.specure.database.entity.Test

interface TestRepository {

    fun getTest(): MutableLiveData<Test>
}