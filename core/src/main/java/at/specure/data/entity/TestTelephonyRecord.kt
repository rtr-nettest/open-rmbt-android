package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(tableName = Tables.TEST_TELEPHONY_RECORD)
data class TestTelephonyRecord(
    @PrimaryKey
    @ForeignKey(
        entity = TestRecord::class,
        parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
        childColumns = ["testUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testUUID: String,

    /**
     *  operator name
     */
    val networkOperatorName: String?,

    /**
     *  operator code (MMC-MNC)
     */
    val networkOperator: String?,

    /**
     *  true if network is roaming
     */
    val networkIsRoaming: Boolean?,

    /**
     *  Country code of the network
     */
    val networkCountry: String?,

    /**
     *  SIM card issuer operator name
     */
    val networkSimOperatorName: String?,

    /**
     *  SIM card issuer operator code (MCC-MNC)
     */
    val networkSimOperator: String?,

    /**
     * country code of the SIM card issuer operator
     */
    val networkSimCountry: String?,

    /**
     * phone type from TelephonyManager.getPhoneType()
     */
    val phoneType: String?,

    /**
     * data state from TelephonyManager.getDataState(), if there is security exception please save "s.exception"
     */
    val dataState: String?,

    /**
     * Access point name from NetworkInfo.getExtraInfo()
     */
    val apn: String?,

    /**
     * count of SIM cards
     */
    val simCount: Int,

    /**
     * phone has dual SIM
     */
    val hasDualSim: Boolean
)