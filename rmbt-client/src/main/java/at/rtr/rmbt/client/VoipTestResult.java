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
 *******************************************************************************/

package at.rtr.rmbt.client;

import com.google.gson.annotations.SerializedName;

import java.text.DecimalFormat;
import java.text.Format;

/**
 * Created by michal.cadrik on 7/6/2017.
 * <p>
 * ==== voip (= Voice over IP test)
 * <p>
 * [IMPORTANT]
 * The *VoIP* test uses the RTP protocol as defined in RFC 3550 <<citation-1,[1]>>.
 * <p>
 * `voip_objective_delay` => (_optional_) delay between packets in ns, default: 20000000ns (=20ms)
 * `voip_objective_timeout` => (_optional_) test timeout, default: 3000000000ns (=3000ms)
 * [[voip_objective_payload]]`voip_objective_payload` => (_optional_) payload type, as defined in RFC 3551 <<citation-2,[2]>>, supported payload types and their values can be found in the table below. The relevant column is *"Payload type"*. The default value is: `0` (=PCMU).
 * +
 * [IMPORTANT]
 * Codecs with payload type "_dyn_" have no static payload type assigned and are only used with a dynamic payload type <<citation-2,[2]>>. These codecs are not supported by the VoIP test.
 * +
 * [format="csv", options="header", cols="<,^s,<,<"]
 * |===
 * Codec name, Payload type, Clock rate, Codec type
 * "PCMU", 0, 8000, AUDIO
 * "GSM", 3, 8000, AUDIO
 * "G723", 4, 8000, AUDIO
 * "DVI4_8", 5, 8000, AUDIO
 * "DVI4_16", 6, 16000, AUDIO
 * "LPC", 7, 8000, AUDIO
 * "PCMA", 8, 8000, AUDIO
 * "G722", 9, 8000, AUDIO
 * "L16_1", 10, 44100, AUDIO
 * "L16_2", 11, 44100, AUDIO
 * "QCELP", 12, 8000, AUDIO
 * CN,13, 8000, AUDIO
 * MPA,14, 90000, AUDIO
 * G728,15, 8000, AUDIO
 * DVI4_11,16, 11025, AUDIO
 * DVI4_22,17, 22050, AUDIO
 * G729,18, 8000, AUDIO
 * "G726_40",_dyn_, 8000, AUDIO
 * "G726_32",_dyn_, 8000, AUDIO
 * "G726_24",_dyn_, 8000, AUDIO
 * "G726_16",_dyn_, 8000, AUDIO
 * G729D,_dyn_, 8000, AUDIO
 * G729E,_dyn_, 8000, AUDIO
 * "GSM_EFR",_dyn_, 8000, AUDIO
 * L8,_dyn_,_variable_, AUDIO
 * RED,_dyn_,_variable_, AUDIO
 * VDVI,_dyn_,_variable_, AUDIO
 * CELB,25, 90000, VIDEO
 * JPEG,26, 90000, VIDEO
 * NV,28, 90000, VIDEO
 * H261,31, 90000, VIDEO
 * MPV,32, 90000, VIDEO
 * MP2T,33, 90000, BOTH
 * H263,34, 90000, VIDEO
 * "H263_1998",_dyn_, 90000, VIDEO
 * |===
 * <p>
 * `voip_objective_in_port` => the port for the incoming voice stream
 * `voip_objective_out_port` => the port for the outgoing voice stream
 * `voip_objective_call_duration` => (_optional_) duration of the simulated call, default: 1000000000ns (=1000ms)
 * `voip_objective_bits_per_sample` => (_optional_) bits per sample, default: 8
 * `voip_objective_sample_rate` => (_optional_) the sample rate in _Hz_, default: 8000
 * `voip_result_status` => the test result, enum:
 * * `OK` - the test was successful (=test execution; regardless of the test result)
 * * `TIMEOUT` - the test timeout has beed reached
 * * `ERROR` - another error occured
 * incoming voice stream results (client side):
 * * `voip_result_in_short_seq` => the shortest correct packet sequence (fewest number of packets in correct order)
 * * `voip_result_in_long_seq` => the longest correct packet sequence (most number of packets in correct order)
 * * `voip_result_in_max_jitter` => the max jitter in ns
 * * `voip_result_in_mean_jitter` => the mean jitter in ns
 * * `voip_result_in_skew` => the skew in ns
 * * `voip_result_in_num_packets` => number of packets received
 * * `voip_result_in_max_delta` => highest delay between received packets
 * * `voip_result_in_sequence_error` => number of sequence errors (packets out of order)
 * outgoing voice stream results:
 * * `voip_result_out_short_seq` => the shortest correct packet sequence (fewest number of packets in correct order)
 * * `voip_result_out_long_seq` => the longest correct packet sequence (most number of packets in correct order)
 * * `voip_result_out_max_jitter` => the max jitter in ns
 * * `voip_result_out_mean_jitter` => the mean jitter in ns
 * * `voip_result_out_skew` => the skew in ns
 * * `voip_result_out_max_delta` => highest delay between received packets
 * * `voip_result_out_num_packets` => number of packets received
 * * `voip_result_out_sequence_error` => number of sequence errors (packets out of order)
 */

@SuppressWarnings("SpellCheckingInspection")
public class VoipTestResult {


    public static final String JSON_OBJECT_IDENTIFIER = "jpl";

    /*****************************/
    /**         IN              **/
    /*****************************/
    /**
     * Number of packets arrived in
     */
    @SerializedName("voip_result_in_num_packets")
    private Integer resultInNumPackets = null;

    /**
     * Longest sequence of arrived packets in
     */
    @SerializedName("voip_result_in_long_seq")
    private Integer resultInLongestSeqPackets = null;

    /**
     * Shortest sequence of arrived packets in
     */
    @SerializedName("voip_result_in_short_seq")
    private Integer resultInShortestSeqPackets = null;

    /**
     * Mean jitter
     */
    @SerializedName("voip_result_in_mean_jitter")
    private Long resultInMeanJitter = null;

    /**
     * Max in jitter
     */
    @SerializedName("voip_result_in_max_jitter")
    private Long resultInMaxJitter = null;

    /**
     * Number of packets in incorrect order sequence
     */
    @SerializedName("voip_result_in_sequence_error")
    private Integer resultInSeqError = null;

    /**
     * Skew in (nanoseconds)
     */
    @SerializedName("voip_result_in_skew")
    private Long resultInSkew = null;

    /**
     * Max delta in
     */
    @SerializedName("voip_result_in_max_delta")
    private Long resultInMaxDelta = null;


    /*****************************/
    /**         OUT             **/
    /*****************************/

    /**
     * Skew out (nanoseconds)
     */
    @SerializedName("voip_result_out_skew")
    private Long resultOutSkew = null;

    /**
     * Max delta out
     */
    @SerializedName("voip_result_out_max_delta")
    private Long resultOutMaxDelta = null;

    /**
     * Number of packets in incorrect order sequence (out)
     */
    @SerializedName("voip_result_out_sequence_error")
    private Long resultOutSeqError = null;

    /**
     * Longest sequence of arrived packets out
     */
    @SerializedName("voip_result_out_long_seq")
    private Long resultOutLongestSeqPackets = null;

    /**
     * Shortest sequence of arrived packets out
     */
    @SerializedName("voip_result_out_short_seq")
    private Long resultOutShortestSeqPackets = null;

    /**
     * Mean jitter
     */
    @SerializedName("voip_result_out_mean_jitter")
    private Long resultOutMeanJitter = null;

    /**
     * Max in jitter
     */
    @SerializedName("voip_result_out_max_jitter")
    private Long resultOutMaxJitter = null;

    /**
     * Number of packets sent out
     */
    @SerializedName("voip_result_out_num_packets")
    private Long resultOutNumPackets = null;


    /*****************************/
    /**        OBJECTIVES       **/
    /*****************************/

    /**
     * Bits per sample
     */
    @SerializedName("voip_objective_bits_per_sample")
    private Integer objectiveBitsPerSample = 8;

    /**
     * Port in
     */
    @SerializedName("voip_objective_in_port")
    private Integer objectivePortIn = null;

    /**
     * Port out
     */
    @SerializedName("voip_objective_out_port")
    private Integer objectivePortOut = null;

    /**
     * Delay between packets in ns, default: 20000000ns (=20ms)
     */
    @SerializedName("voip_objective_delay")
    private Long objectiveDelay = 20000000L;

    /**
     * test timeout in nanoseconds, default: 3000000000ns (=3000ms)
     */
    @SerializedName("voip_objective_timeout")
    private Long objectiveTimeoutNS = 3000000000L;


    /**
     * Payload type, as defined in RFC 3551 <<citation-2,[2]>>,
     * supported payload types and their values can be found in the comment at the begining of this class.
     * The relevant column is *"Payload type"*. The default value is: `0` (=PCMU)
     */
    @SerializedName("voip_objective_payload")
    private Integer objectivePayload = 0;

    /**
     * Call duration in nanoseconds
     */
    @SerializedName("voip_objective_call_duration")
    private Long objectiveCallDuration = 1000000000L;

    /**
     * Sample rate
     */
    @SerializedName("voip_objective_sample_rate")
    private Integer objectiveSampleRate = 8000;


    /*****************************/
    /**         GENERAL         **/
    /*****************************/

    /**
     * Duration of the test in nanoseconds
     */
    @SerializedName("duration_ns")
    private Long testDurationInNS = null;

    /**
     * Test start timestamp
     */
    @SerializedName("start_time_ns")
    private Long startTimeInNS = null;

    /**
     * General test result status string [OK, TIMEOUT, ERROR]
     * defined in @{@link TestResultConst}}
     */
    @SerializedName("voip_result_status")
    private String testResultStatus = TestResultConst.TEST_RESULT_ERROR;

    /**
     * For values see:
     *
     * @{@link at.specure.android.util.Helperfunctions.getClassificationImage},
     * @{@link at.specure.android.util.Helperfunctions.getClassificationColor}
     */
    @SerializedName("classification_packet_loss")
    private Integer classificationPacketLoss = -1;

    /**
     * For values see:
     *
     * @{@link at.specure.android.util.Helperfunctions.getClassificationImage},
     * @{@link at.specure.android.util.Helperfunctions.getClassificationColor}
     */
    @SerializedName("classification_jitter")
    private Integer classificationJitter = -1;

    @SerializedName("voip_result_packet_loss")
    private String voipResultPacketLoss = "-";

    @SerializedName("voip_result_jitter")
    private String voipResultJitter = "-";


    public VoipTestResult() {
    }

    public VoipTestResult(Integer classificationPacketLoss, Integer classificationJitter, Integer resultInNumPackets, Integer resultInLongestSeqPackets, Integer resultInShortestSeqPackets, Long resultInMeanJitter, Long resultInMaxJitter, Integer resultInSeqError, Long resultInSkew, Long resultInMaxDelta, Long resultOutSkew, Long resultOutMaxDelta, Long resultOutSeqError, Long resultOutLongestSeqPackets, Long resultOutShortestSeqPackets, Long resultOutMeanJitter, Long resultOutMaxJitter, Long resultOutNumPackets, Integer objectiveBitsPerSample, Integer objectivePortIn, Integer objectivePortOut, Long objectiveDelay, Long objectiveTimeoutNS, Integer objectivePayload, Long objectiveCallDuration, Integer objectiveSampleRate, Long testDurationInNS, Long startTimeInNS, String testResultStatus, String voipResultJitter, String voipResultPacketLoss) {
        this.resultInNumPackets = resultInNumPackets;
        this.classificationPacketLoss = classificationPacketLoss;
        this.classificationJitter = classificationJitter;
        this.resultInLongestSeqPackets = resultInLongestSeqPackets;
        this.resultInShortestSeqPackets = resultInShortestSeqPackets;
        this.resultInMeanJitter = resultInMeanJitter;
        this.resultInMaxJitter = resultInMaxJitter;
        this.resultInSeqError = resultInSeqError;
        this.resultInSkew = resultInSkew;
        this.resultInMaxDelta = resultInMaxDelta;
        this.resultOutSkew = resultOutSkew;
        this.resultOutMaxDelta = resultOutMaxDelta;
        this.resultOutSeqError = resultOutSeqError;
        this.resultOutLongestSeqPackets = resultOutLongestSeqPackets;
        this.resultOutShortestSeqPackets = resultOutShortestSeqPackets;
        this.resultOutMeanJitter = resultOutMeanJitter;
        this.resultOutMaxJitter = resultOutMaxJitter;
        this.resultOutNumPackets = resultOutNumPackets;
        this.objectiveBitsPerSample = objectiveBitsPerSample;
        this.objectivePortIn = objectivePortIn;
        this.objectivePortOut = objectivePortOut;
        this.objectiveDelay = objectiveDelay;
        this.objectiveTimeoutNS = objectiveTimeoutNS;
        this.objectivePayload = objectivePayload;
        this.objectiveCallDuration = objectiveCallDuration;
        this.objectiveSampleRate = objectiveSampleRate;
        this.testDurationInNS = testDurationInNS;
        this.startTimeInNS = startTimeInNS;
        this.testResultStatus = testResultStatus;
        this.voipResultJitter = voipResultJitter;
        this.voipResultPacketLoss = voipResultPacketLoss;
    }

    public Integer getResultInNumPackets() {
        return resultInNumPackets;
    }

    public VoipTestResult setResultInNumPackets(Integer resultInNumPackets) {
        this.resultInNumPackets = resultInNumPackets;
        return this;
    }

    public Integer getClassificationJitter() {
        return classificationJitter;
    }

    public VoipTestResult setClassificationJitter(Integer classificationJitter) {
        this.classificationJitter = classificationJitter;
        return this;
    }

    public Integer getClassificationPacketLoss() {
        return classificationPacketLoss;
    }

    public VoipTestResult setClassificationPacketLoss(Integer classificationPacketLoss) {
        this.classificationPacketLoss = classificationPacketLoss;
        return this;
    }

    public Integer getResultInLongestSeqPackets() {
        return resultInLongestSeqPackets;
    }

    public VoipTestResult setResultInLongestSeqPackets(Integer resultInLongestSeqPackets) {
        this.resultInLongestSeqPackets = resultInLongestSeqPackets;
        return this;
    }

    public Integer getResultInShortestSeqPackets() {
        return resultInShortestSeqPackets;
    }

    public VoipTestResult setResultInShortestSeqPackets(Integer resultInShortestSeqPackets) {
        this.resultInShortestSeqPackets = resultInShortestSeqPackets;
        return this;
    }

    public Long getResultInMeanJitter() {
        return resultInMeanJitter;
    }

    public VoipTestResult setResultInMeanJitter(Long resultInMeanJitter) {
        this.resultInMeanJitter = resultInMeanJitter;
        return this;
    }

    public Long getResultInMaxJitter() {
        return resultInMaxJitter;
    }

    public VoipTestResult setResultInMaxJitter(Long resultInMaxJitter) {
        this.resultInMaxJitter = resultInMaxJitter;
        return this;
    }

    public Integer getResultInSeqError() {
        return resultInSeqError;
    }

    public VoipTestResult setResultInSeqError(Integer resultInSeqError) {
        this.resultInSeqError = resultInSeqError;
        return this;
    }

    public Long getResultInSkew() {
        return resultInSkew;
    }

    public VoipTestResult setResultInSkew(Long resultInSkew) {
        this.resultInSkew = resultInSkew;
        return this;
    }

    public Long getResultInMaxDelta() {
        return resultInMaxDelta;
    }

    public VoipTestResult setResultInMaxDelta(Long resultInMaxDelta) {
        this.resultInMaxDelta = resultInMaxDelta;
        return this;
    }

    public Long getResultOutSkew() {
        return resultOutSkew;
    }

    public VoipTestResult setResultOutSkew(Long resultOutSkew) {
        this.resultOutSkew = resultOutSkew;
        return this;
    }

    public Long getResultOutMaxDelta() {
        return resultOutMaxDelta;
    }

    public VoipTestResult setResultOutMaxDelta(Long resultOutMaxDelta) {
        this.resultOutMaxDelta = resultOutMaxDelta;
        return this;
    }

    public Long getResultOutSeqError() {
        return resultOutSeqError;
    }

    public VoipTestResult setResultOutSeqError(Long resultOutSeqError) {
        this.resultOutSeqError = resultOutSeqError;
        return this;
    }

    public Long getResultOutLongestSeqPackets() {
        return resultOutLongestSeqPackets;
    }

    public VoipTestResult setResultOutLongestSeqPackets(Long resultOutLongestSeqPackets) {
        this.resultOutLongestSeqPackets = resultOutLongestSeqPackets;
        return this;
    }

    public Long getResultOutShortestSeqPackets() {
        return resultOutShortestSeqPackets;
    }

    public VoipTestResult setResultOutShortestSeqPackets(Long resultOutShortestSeqPackets) {
        this.resultOutShortestSeqPackets = resultOutShortestSeqPackets;
        return this;
    }

    public Long getResultOutMeanJitter() {
        return resultOutMeanJitter;
    }

    public VoipTestResult setResultOutMeanJitter(Long resultOutMeanJitter) {
        this.resultOutMeanJitter = resultOutMeanJitter;
        return this;
    }

    public Long getResultOutMaxJitter() {
        return resultOutMaxJitter;
    }

    public VoipTestResult setResultOutMaxJitter(Long resultOutMaxJitter) {
        this.resultOutMaxJitter = resultOutMaxJitter;
        return this;
    }

    public Long getResultOutNumPackets() {
        return resultOutNumPackets;
    }

    public VoipTestResult setResultOutNumPackets(Long resultOutNumPackets) {
        this.resultOutNumPackets = resultOutNumPackets;
        return this;
    }

    public Integer getObjectiveBitsPerSample() {
        return objectiveBitsPerSample;
    }

    public VoipTestResult setObjectiveBitsPerSample(Integer objectiveBitsPerSample) {
        this.objectiveBitsPerSample = objectiveBitsPerSample;
        return this;
    }

    public Integer getObjectivePortIn() {
        return objectivePortIn;
    }

    public VoipTestResult setObjectivePortIn(Integer objectivePortIn) {
        this.objectivePortIn = objectivePortIn;
        return this;
    }

    public Integer getObjectivePortOut() {
        return objectivePortOut;
    }

    public VoipTestResult setObjectivePortOut(Integer objectivePortOut) {
        this.objectivePortOut = objectivePortOut;
        return this;
    }

    public Long getObjectiveDelay() {
        return objectiveDelay;
    }

    public VoipTestResult setObjectiveDelay(Long objectiveDelay) {
        this.objectiveDelay = objectiveDelay;
        return this;
    }

    public Long getObjectiveTimeoutNS() {
        return objectiveTimeoutNS;
    }

    public VoipTestResult setObjectiveTimeoutNS(Long objectiveTimeoutNS) {
        this.objectiveTimeoutNS = objectiveTimeoutNS;
        return this;
    }

    public Integer getObjectivePayload() {
        return objectivePayload;
    }

    public VoipTestResult setObjectivePayload(Integer objectivePayload) {
        this.objectivePayload = objectivePayload;
        return this;
    }

    public Long getObjectiveCallDuration() {
        return objectiveCallDuration;
    }

    public VoipTestResult setObjectiveCallDuration(Long objectiveCallDuration) {
        this.objectiveCallDuration = objectiveCallDuration;
        return this;
    }

    public Integer getObjectiveSampleRate() {
        return objectiveSampleRate;
    }

    public VoipTestResult setObjectiveSampleRate(Integer objectiveSampleRate) {
        this.objectiveSampleRate = objectiveSampleRate;
        return this;
    }

    public Long getTestDurationInNS() {
        return testDurationInNS;
    }

    public VoipTestResult setTestDurationInNS(Long testDurationInNS) {
        this.testDurationInNS = testDurationInNS;
        return this;
    }

    public Long getStartTimeInNS() {
        return startTimeInNS;
    }

    public VoipTestResult setStartTimeInNS(Long startTimeInNS) {
        this.startTimeInNS = startTimeInNS;
        return this;
    }

    public String getTestResultStatus() {
        return testResultStatus;
    }

    public VoipTestResult setTestResultStatus(String testResultStatus) {
        this.testResultStatus = testResultStatus;
        return this;
    }


    public String getVoipResultPacketLoss() {
        return voipResultPacketLoss;
    }

    public void setVoipResultPacketLoss(String voipResultPacketLoss) {
        this.voipResultPacketLoss = voipResultPacketLoss;
    }

    public String getVoipResultJitter() {
        return voipResultJitter;
    }

    public void setVoipResultJitter(String voipResultJitter) {
        this.voipResultJitter = voipResultJitter;
    }

    public String getMeanJitter() {
        Long resultOutMeanJitter = getResultOutMeanJitter();
        Long resultInMeanJitter = getResultInMeanJitter();
        String meanJitterFormattedString = "-";
        if ((resultInMeanJitter != null) && (resultOutMeanJitter != null)) {
            Long meanJitter = (resultInMeanJitter + resultOutMeanJitter) / 2;
            meanJitterFormattedString = format(meanJitter, new DecimalFormat("@@@ ms"), 1000000.0);
        }
        return meanJitterFormattedString;
    }

    public String getMeanPacketLossInPercent() {
        Integer resultInNumPackets = getResultInNumPackets();
        Long resultOutNumPackets = getResultOutNumPackets();
        Long objectiveCallDuration = getObjectiveCallDuration();
        Long objectiveDelay = getObjectiveDelay();

        int total = 0;
        if ((objectiveDelay != null) && (objectiveDelay != 0)) {
            total = (int) (objectiveCallDuration / objectiveDelay);
        }

        String packetLossStr = "-";

        int packetLossDown = (int) (100f * ((float) (total - resultInNumPackets) / (float) total));
        int packetLossUp = (int) (100f * ((float) (total - resultOutNumPackets) / (float) total));
        if ((packetLossDown >= 0) && (packetLossUp >= 0)) {
            long meanPacketLoss = (packetLossDown + packetLossUp) / 2;
            packetLossStr = format(meanPacketLoss, new DecimalFormat("0.0 %"), 100.0);
        }
        return packetLossStr;
    }

    public String format(Long value, Format viewFormat, double roundingValue) {
        if (value != null) {
            if (viewFormat != null) {
                return viewFormat.format(value / roundingValue);
            } else {
                return " - ";
            }
        }
        return " - ";
    }


    public VoipTestResult createVoipTestResult() {
        return new VoipTestResult(classificationPacketLoss, classificationJitter, resultInNumPackets, resultInLongestSeqPackets, resultInShortestSeqPackets, resultInMeanJitter, resultInMaxJitter, resultInSeqError, resultInSkew, resultInMaxDelta, resultOutSkew, resultOutMaxDelta, resultOutSeqError, resultOutLongestSeqPackets, resultOutShortestSeqPackets, resultOutMeanJitter, resultOutMaxJitter, resultOutNumPackets, objectiveBitsPerSample, objectivePortIn, objectivePortOut, objectiveDelay, objectiveTimeoutNS, objectivePayload, objectiveCallDuration, objectiveSampleRate, testDurationInNS, startTimeInNS, testResultStatus, voipResultJitter, voipResultPacketLoss);
    }
}
