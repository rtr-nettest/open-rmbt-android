package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.rmbt.client.control.data.TestFinishReason
import at.rtr.rmbt.client.VoipTestResult
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(tableName = Tables.JPL)
data class VoipTestResultRecord(

    @PrimaryKey
    @ForeignKey(
        entity = TestRecord::class,
        parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
        childColumns = ["testUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testUUID: String,
    var classificationPacketLoss: Int?,
    var classificationJitter: Int?,
    var resultInNumPackets: Int?,
    var resultInLongestSeqPackets: Int?,
    var resultInShortestSeqPackets: Int?,
    var resultInMeanJitter: Long?,
    var resultInMaxJitter: Long?,
    var resultInSeqError: Int?,
    var resultInSkew: Long?,
    var resultInMaxDelta: Long?,
    var resultOutSkew: Long?,
    var resultOutMaxDelta: Long?,
    var resultOutSeqError: Long?,
    var resultOutLongestSeqPackets: Long?,
    var resultOutShortestSeqPackets: Long?,
    var resultOutMeanJitter: Long?,
    var resultOutMaxJitter: Long?,
    var resultOutNumPackets: Long?,
    var objectiveBitsPerSample: Int?,
    var objectivePortIn: Int?,
    var objectivePortOut: Int?,
    var objectiveDelay: Long?,
    var objectiveTimeoutNS: Long?,
    var objectivePayload: Int?,
    var objectiveCallDuration: Long?,
    var objectiveSampleRate: Int?,
    var testDurationInNS: Long?,
    var startTimeInNS: Long?,
    var testResultStatus: String?,
    var voipResultJitter: String?,
    var voipResultPacketLoss: String?
)

fun VoipTestResultRecord.toRequest(): at.rmbt.client.control.VoipTestResult? {

    return at.rmbt.client.control.VoipTestResult(
        classificationPacketLoss = classificationPacketLoss ?: -1,
        classificationJitter = classificationJitter ?: -1,
        resultInNumPackets = resultInNumPackets,
        resultInLongestSeqPackets = resultInLongestSeqPackets,
        resultInShortestSeqPackets = resultInShortestSeqPackets,
        resultInMeanJitter = resultInMeanJitter,
        resultInMaxJitter = resultInMaxJitter,
        resultInSeqError = resultInSeqError,
        resultInSkew = resultInSkew,
        resultInMaxDelta = resultInMaxDelta,
        resultOutSkew = resultOutSkew,
        resultOutMaxDelta = resultOutMaxDelta,
        resultOutSeqError = resultOutSeqError,
        resultOutLongestSeqPackets = resultOutLongestSeqPackets,
        resultOutShortestSeqPackets = resultOutShortestSeqPackets,
        resultOutMeanJitter = resultOutMeanJitter,
        resultOutMaxJitter = resultOutMaxJitter,
        resultOutNumPackets = resultOutNumPackets,
        objectiveBitsPerSample = objectiveBitsPerSample ?: 8,
        objectivePortIn = objectivePortIn,
        objectivePortOut = objectivePortOut,
        objectiveDelay = objectiveDelay ?: 20000000L,
        objectiveTimeoutNS = objectiveTimeoutNS ?: 3000000000L,
        objectivePayload = objectivePayload ?: 0,
        objectiveCallDuration = objectiveCallDuration ?: 1000000000L,
        objectiveSampleRate = objectiveSampleRate ?: 8000,
        testDurationInNS = testDurationInNS,
        startTimeInNS = startTimeInNS,
        testResultStatus = testResultStatus ?: TestFinishReason.ERROR.name,
        voipResultJitter = voipResultJitter ?: "-",
        voipResultPacketLoss = voipResultPacketLoss ?: "-"
    )
}

fun VoipTestResult.toRecord(testUUID: String): VoipTestResultRecord {
    return VoipTestResultRecord(
        testUUID = testUUID,
        classificationPacketLoss = classificationPacketLoss,
        classificationJitter = classificationJitter,
        resultInNumPackets = resultInNumPackets,
        resultInLongestSeqPackets = resultInLongestSeqPackets,
        resultInShortestSeqPackets = resultInShortestSeqPackets,
        resultInMeanJitter = resultInMeanJitter,
        resultInMaxJitter = resultInMaxJitter,
        resultInSeqError = resultInSeqError,
        resultInSkew = resultInSkew,
        resultInMaxDelta = resultInMaxDelta,
        resultOutSkew = resultOutSkew,
        resultOutMaxDelta = resultOutMaxDelta,
        resultOutSeqError = resultOutSeqError,
        resultOutLongestSeqPackets = resultOutLongestSeqPackets,
        resultOutShortestSeqPackets = resultOutShortestSeqPackets,
        resultOutMeanJitter = resultOutMeanJitter,
        resultOutMaxJitter = resultOutMaxJitter,
        resultOutNumPackets = resultOutNumPackets,
        objectiveBitsPerSample = objectiveBitsPerSample,
        objectivePortIn = objectivePortIn,
        objectivePortOut = objectivePortOut,
        objectiveDelay = objectiveDelay,
        objectiveTimeoutNS = objectiveTimeoutNS,
        objectivePayload = objectivePayload,
        objectiveCallDuration = objectiveCallDuration,
        objectiveSampleRate = objectiveSampleRate,
        testDurationInNS = testDurationInNS,
        startTimeInNS = startTimeInNS,
        testResultStatus = testResultStatus,
        voipResultJitter = voipResultJitter,
        voipResultPacketLoss = voipResultPacketLoss
    )
}