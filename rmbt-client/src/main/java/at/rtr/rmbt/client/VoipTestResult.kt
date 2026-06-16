/*******************************************************************************
 * Copyright 2014-2017 Specure GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.rtr.rmbt.client

import com.google.gson.annotations.SerializedName
import java.text.DecimalFormat
import java.text.Format

/**
 * Created by michal.cadrik on 7/6/2017.
 *
 * voip (= Voice over IP test) result. The *VoIP* test uses the RTP protocol as defined in RFC 3550.
 */
@Suppress("SpellCheckingInspection")
class VoipTestResult {

    // IN
    @SerializedName("voip_result_in_num_packets")
    var resultInNumPackets: Int? = null

    @SerializedName("voip_result_in_long_seq")
    var resultInLongestSeqPackets: Int? = null

    @SerializedName("voip_result_in_short_seq")
    var resultInShortestSeqPackets: Int? = null

    @SerializedName("voip_result_in_mean_jitter")
    var resultInMeanJitter: Long? = null

    @SerializedName("voip_result_in_max_jitter")
    var resultInMaxJitter: Long? = null

    @SerializedName("voip_result_in_sequence_error")
    var resultInSeqError: Int? = null

    @SerializedName("voip_result_in_skew")
    var resultInSkew: Long? = null

    @SerializedName("voip_result_in_max_delta")
    var resultInMaxDelta: Long? = null

    // OUT
    @SerializedName("voip_result_out_skew")
    var resultOutSkew: Long? = null

    @SerializedName("voip_result_out_max_delta")
    var resultOutMaxDelta: Long? = null

    @SerializedName("voip_result_out_sequence_error")
    var resultOutSeqError: Long? = null

    @SerializedName("voip_result_out_long_seq")
    var resultOutLongestSeqPackets: Long? = null

    @SerializedName("voip_result_out_short_seq")
    var resultOutShortestSeqPackets: Long? = null

    @SerializedName("voip_result_out_mean_jitter")
    var resultOutMeanJitter: Long? = null

    @SerializedName("voip_result_out_max_jitter")
    var resultOutMaxJitter: Long? = null

    @SerializedName("voip_result_out_num_packets")
    var resultOutNumPackets: Long? = null

    // OBJECTIVES
    @SerializedName("voip_objective_bits_per_sample")
    var objectiveBitsPerSample: Int? = 8

    @SerializedName("voip_objective_in_port")
    var objectivePortIn: Int? = null

    @SerializedName("voip_objective_out_port")
    var objectivePortOut: Int? = null

    @SerializedName("voip_objective_delay")
    var objectiveDelay: Long? = 20000000L

    @SerializedName("voip_objective_timeout")
    var objectiveTimeoutNS: Long? = 3000000000L

    @SerializedName("voip_objective_payload")
    var objectivePayload: Int? = 0

    @SerializedName("voip_objective_call_duration")
    var objectiveCallDuration: Long? = 1000000000L

    @SerializedName("voip_objective_sample_rate")
    var objectiveSampleRate: Int? = 8000

    // GENERAL
    @SerializedName("duration_ns")
    var testDurationInNS: Long? = null

    @SerializedName("start_time_ns")
    var startTimeInNS: Long? = null

    @SerializedName("voip_result_status")
    var testResultStatus: String? = TestResultConst.TEST_RESULT_ERROR

    @SerializedName("classification_packet_loss")
    var classificationPacketLoss: Int? = -1

    @SerializedName("classification_jitter")
    var classificationJitter: Int? = -1

    @SerializedName("voip_result_packet_loss")
    var voipResultPacketLoss: String? = "-"

    @SerializedName("voip_result_jitter")
    var voipResultJitter: String? = "-"

    fun getMeanJitter(): String {
        val resultOutMeanJitter = this.resultOutMeanJitter
        val resultInMeanJitter = this.resultInMeanJitter
        var meanJitterFormattedString = "-"
        if (resultInMeanJitter != null && resultOutMeanJitter != null) {
            val meanJitter = (resultInMeanJitter + resultOutMeanJitter) / 2
            meanJitterFormattedString = format(meanJitter, DecimalFormat("@@@ ms"), 1000000.0)
        }
        return meanJitterFormattedString
    }

    fun getMeanPacketLossInPercent(): String {
        val resultInNumPackets = this.resultInNumPackets
        val resultOutNumPackets = this.resultOutNumPackets
        val objectiveCallDuration = this.objectiveCallDuration
        val objectiveDelay = this.objectiveDelay

        var total = 0
        if (objectiveDelay != null && objectiveDelay != 0L && objectiveCallDuration != null) {
            total = (objectiveCallDuration / objectiveDelay).toInt()
        }

        var packetLossStr = "-"

        val packetLossDown = (100f * ((total - (resultInNumPackets ?: 0)).toFloat() / total.toFloat())).toInt()
        val packetLossUp = (100f * ((total - (resultOutNumPackets ?: 0)).toFloat() / total.toFloat())).toInt()
        if (packetLossDown >= 0 && packetLossUp >= 0) {
            val meanPacketLoss = ((packetLossDown + packetLossUp) / 2).toLong()
            packetLossStr = format(meanPacketLoss, DecimalFormat("0.0 %"), 100.0)
        }
        return packetLossStr
    }

    fun format(value: Long?, viewFormat: Format?, roundingValue: Double): String {
        if (value != null) {
            return if (viewFormat != null) {
                viewFormat.format(value / roundingValue)
            } else {
                " - "
            }
        }
        return " - "
    }

    companion object {
        const val JSON_OBJECT_IDENTIFIER = "jpl"
    }
}
