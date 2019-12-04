package at.specure.repository

import at.specure.database.entity.TestRecord

interface TestRepository {

    fun saveTest(test: TestRecord)

    fun update(testRecord: TestRecord)
}