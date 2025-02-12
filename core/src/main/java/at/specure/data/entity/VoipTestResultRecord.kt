package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.rtr.rmbt.client.VoipTestResult
import at.specure.data.Tables

@Entity(
    tableName = Tables.JPL
)
data class VoipTestResultRecord(

    @PrimaryKey
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

/**
 * Returns jitter in millis
 */
fun VoipTestResultRecord.getJitter(): Double? {
    var meanJitter: Double? = null
    this.resultInMeanJitter?.let { resultInMeanJitterLocal ->
        this.resultOutMeanJitter?.let { resultOutMeanJitterLocal ->
            meanJitter = ((resultInMeanJitterLocal + resultOutMeanJitterLocal) / (2 * 1000000.toDouble()))
        }
    }
    return meanJitter
}

/**
 * Returns packet loss in percents
 */
fun VoipTestResultRecord.getPacketLoss(): Double? {
    var packetLossPercent: Double? = null
    this.objectiveCallDuration?.let { objectiveCallDurationLocal ->
        this.objectiveDelay?.let { objectiveDelayLocal ->
            this.resultInNumPackets?.let { resultInNumPacketsLocal ->
                this.resultOutNumPackets?.let { resultOutNumPacketsLocal ->
                    val total = (objectiveCallDurationLocal / objectiveDelayLocal)
                    val packetLossDown = (total - resultInNumPacketsLocal) / total
                    val packetLossUp = (total - resultOutNumPacketsLocal) / total
                    packetLossPercent = (packetLossDown + packetLossUp) / 2.toDouble()
                }
            }
        }
    }
    return packetLossPercent
}
