package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import at.specure.result.QoSCategory

@Entity(
    tableName = Tables.QOS_CATEGORY
)
data class QosCategoryRecord(

    /**
     * id for internal purpose of the local DB
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Test uuid generated by control server - unique identification of the test
     */
    val testUUID: String,

    /**
     * Category of the qoe
     */
    val category: QoSCategory,

    /**
     * Localized name from the server side
     */
    val categoryName: String,

    /**
     * Localized description of the test from the server side
     */
    val categoryDescription: String,

    /**
     * Language code in which are text localized ("en")
     */
    val language: String,

    /**
     * Number of successfully completed tests in the category
     */
    val successCount: Int,

    /**
     * Number of failed tests in the category
     */
    val failedCount: Int
)