package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns.TEST_UUID_PARENT_COLUMN

@Entity
data class MobileTest(
    @PrimaryKey
    @ForeignKey(entity = Test::class, parentColumns = [TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    /**
     *  operator name
     */
    val telNetworkOperatorName: String?,
    /**
     *  operator code (MMC-MNC)
     */
    val telNetworkOperator: String?,
    /**
     *  true if network is roaming
     */
    val telNetworkIsRoaming: Boolean?,
    /**
     *  Country code of the network
     */
    val telNetworkCountry: String?,
    /**
     *  SIM card issuer operator name
     */
    val telNetworkSimOperatorName: String?,
    /**
     *  SIM card issuer operator code (MMC-MNC)
     */
    val telNetworkSimOperator: String?,
    /**
     * country code of the SIM card issuer operator
     */
    val telNetworkSimCountry: String?,
    /**
     * phone type from TelephonyManager.getPhoneType()
     */
    val telPhoneType: String?,
    /**
     * data state from TelephonyManager.getDataState(), if there is security exception please save "s.exception"
     */
    val telDataState: String?,
    /**
     * Access point name from NetworkInfo.getExtraInfo()
     */
    val telApn: String?
)