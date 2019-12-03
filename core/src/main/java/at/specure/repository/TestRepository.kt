package at.specure.repository

import at.specure.database.entity.TestRecord

interface TestRepository {

    fun getTest(uuid: String): TestRecord?

    fun getLatestTest(): TestRecord?

    fun saveTest(test: TestRecord)

    fun deleteTest(test: TestRecord)
}