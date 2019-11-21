package at.specure.repository

import at.specure.database.entity.Test

interface TestRepository {

    fun getTest(uuid: String): Test?

    fun getLatestTest(): Test?

    fun saveTest(test: Test)

    fun deleteTest(test: Test)
}