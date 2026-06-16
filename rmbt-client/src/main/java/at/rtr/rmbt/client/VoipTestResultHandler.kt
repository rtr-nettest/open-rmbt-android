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

import android.content.Context

/**
 * Created by michal.cadrik on 7/6/2017.
 *
 * Class dedicated to save and load voip test result from shared preferences
 */
class VoipTestResultHandler {

    /**
     * Converts results hash map to object used to send as Json
     */
    fun convertResultsToObject(resultMap: HashMap<String, Any?>): VoipTestResult {
        val voipTestResult = VoipTestResult()

        // IN
        if (resultMap.containsKey("voip_result_in_num_packets")) {
            voipTestResult.resultInNumPackets = resultMap["voip_result_in_num_packets"] as Int?
        }
        if (resultMap.containsKey("voip_result_in_long_seq")) {
            voipTestResult.resultInLongestSeqPackets = resultMap["voip_result_in_long_seq"] as Int?
        }
        if (resultMap.containsKey("voip_result_in_short_seq")) {
            voipTestResult.resultInShortestSeqPackets = resultMap["voip_result_in_short_seq"] as Int?
        }
        if (resultMap.containsKey("voip_result_in_mean_jitter")) {
            voipTestResult.resultInMeanJitter = resultMap["voip_result_in_mean_jitter"] as Long?
        }
        if (resultMap.containsKey("voip_result_in_max_jitter")) {
            voipTestResult.resultInMaxJitter = resultMap["voip_result_in_max_jitter"] as Long?
        }
        if (resultMap.containsKey("voip_result_in_sequence_error")) {
            voipTestResult.resultInSeqError = resultMap["voip_result_in_sequence_error"] as Int?
        }
        if (resultMap.containsKey("voip_result_in_skew")) {
            voipTestResult.resultInSkew = resultMap["voip_result_in_skew"] as Long?
        }
        if (resultMap.containsKey("voip_result_in_max_delta")) {
            voipTestResult.resultInMaxDelta = resultMap["voip_result_in_max_delta"] as Long?
        }

        // OUT
        if (resultMap.containsKey("voip_result_out_skew")) {
            voipTestResult.resultOutSkew = resultMap["voip_result_out_skew"] as Long?
        }
        if (resultMap.containsKey("voip_result_out_max_delta")) {
            voipTestResult.resultOutMaxDelta = resultMap["voip_result_out_max_delta"] as Long?
        }
        if (resultMap.containsKey("voip_result_out_sequence_error")) {
            voipTestResult.resultOutSeqError = resultMap["voip_result_out_sequence_error"] as Long?
        }
        if (resultMap.containsKey("voip_result_out_long_seq")) {
            voipTestResult.resultOutLongestSeqPackets = resultMap["voip_result_out_long_seq"] as Long?
        }
        if (resultMap.containsKey("voip_result_out_short_seq")) {
            voipTestResult.resultOutShortestSeqPackets = resultMap["voip_result_out_short_seq"] as Long?
        }
        if (resultMap.containsKey("voip_result_out_mean_jitter")) {
            voipTestResult.resultOutMeanJitter = resultMap["voip_result_out_mean_jitter"] as Long?
        }
        if (resultMap.containsKey("voip_result_out_max_jitter")) {
            voipTestResult.resultOutMaxJitter = resultMap["voip_result_out_max_jitter"] as Long?
        }
        if (resultMap.containsKey("voip_result_out_num_packets")) {
            voipTestResult.resultOutNumPackets = resultMap["voip_result_out_num_packets"] as Long?
        }

        // OBJECTIVES
        if (resultMap.containsKey("voip_objective_bits_per_sample")) {
            voipTestResult.objectiveBitsPerSample = resultMap["voip_objective_bits_per_sample"] as Int?
        }
        if (resultMap.containsKey("voip_objective_in_port")) {
            voipTestResult.objectivePortIn = resultMap["voip_objective_in_port"] as Int?
        }
        if (resultMap.containsKey("voip_objective_out_port")) {
            voipTestResult.objectivePortOut = resultMap["voip_objective_out_port"] as Int?
        }
        if (resultMap.containsKey("voip_objective_delay")) {
            voipTestResult.objectiveDelay = resultMap["voip_objective_delay"] as Long?
        }
        if (resultMap.containsKey("voip_objective_timeout")) {
            voipTestResult.objectiveTimeoutNS = resultMap["voip_objective_timeout"] as Long?
        }
        if (resultMap.containsKey("voip_objective_payload")) {
            voipTestResult.objectivePayload = resultMap["voip_objective_payload"] as Int?
        }
        if (resultMap.containsKey("voip_objective_call_duration")) {
            voipTestResult.objectiveCallDuration = resultMap["voip_objective_call_duration"] as Long?
        }
        if (resultMap.containsKey("voip_objective_sample_rate")) {
            voipTestResult.objectiveBitsPerSample = resultMap["voip_objective_sample_rate"] as Int?
        }

        // GENERAL
        if (resultMap.containsKey("duration_ns")) {
            voipTestResult.testDurationInNS = resultMap["duration_ns"] as Long?
        }
        if (resultMap.containsKey("start_time_ns")) {
            voipTestResult.startTimeInNS = resultMap["start_time_ns"] as Long?
        }
        if (resultMap.containsKey("voip_result_status")) {
            voipTestResult.testResultStatus = resultMap["voip_result_status"] as String?
        }

        return voipTestResult
    }

    /**
     * Saves result hash map to shared preferences
     */
    fun save(resultMap: HashMap<String, Any?>, context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(VOIP_TEST_RESULT_SHARED_PREF_KEY, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // IN
        if (resultMap.containsKey("voip_result_in_num_packets")) {
            editor.putInt("voip_result_in_num_packets", resultMap["voip_result_in_num_packets"] as Int)
        }
        if (resultMap.containsKey("voip_result_in_long_seq")) {
            editor.putInt("voip_result_in_long_seq", resultMap["voip_result_in_long_seq"] as Int)
        }
        if (resultMap.containsKey("voip_result_in_short_seq")) {
            editor.putInt("voip_result_in_short_seq", resultMap["voip_result_in_short_seq"] as Int)
        }
        if (resultMap.containsKey("voip_result_in_mean_jitter")) {
            editor.putLong("voip_result_in_mean_jitter", resultMap["voip_result_in_mean_jitter"] as Long)
        }
        if (resultMap.containsKey("voip_result_in_max_jitter")) {
            editor.putLong("voip_result_in_max_jitter", resultMap["voip_result_in_max_jitter"] as Long)
        }
        if (resultMap.containsKey("voip_result_in_sequence_error")) {
            editor.putLong("voip_result_in_sequence_error", (resultMap["voip_result_in_sequence_error"] as Int).toLong())
        }
        if (resultMap.containsKey("voip_result_in_skew")) {
            editor.putLong("voip_result_in_skew", resultMap["voip_result_in_skew"] as Long)
        }
        if (resultMap.containsKey("voip_result_in_max_delta")) {
            editor.putLong("voip_result_in_max_delta", resultMap["voip_result_in_max_delta"] as Long)
        }

        // OUT
        if (resultMap.containsKey("voip_result_out_skew")) {
            editor.putLong("voip_result_out_skew", resultMap["voip_result_out_skew"] as Long)
        }
        if (resultMap.containsKey("voip_result_out_max_delta")) {
            editor.putLong("voip_result_out_max_delta", resultMap["voip_result_out_max_delta"] as Long)
        }
        if (resultMap.containsKey("voip_result_out_sequence_error")) {
            editor.putLong("voip_result_out_sequence_error", resultMap["voip_result_out_sequence_error"] as Long)
        }
        if (resultMap.containsKey("voip_result_out_long_seq")) {
            editor.putLong("voip_result_out_long_seq", resultMap["voip_result_out_long_seq"] as Long)
        }
        if (resultMap.containsKey("voip_result_out_short_seq")) {
            editor.putLong("voip_result_out_short_seq", resultMap["voip_result_out_short_seq"] as Long)
        }
        if (resultMap.containsKey("voip_result_out_mean_jitter")) {
            editor.putLong("voip_result_out_mean_jitter", resultMap["voip_result_out_mean_jitter"] as Long)
        }
        if (resultMap.containsKey("voip_result_out_max_jitter")) {
            editor.putLong("voip_result_out_max_jitter", resultMap["voip_result_out_max_jitter"] as Long)
        }
        if (resultMap.containsKey("voip_result_out_num_packets")) {
            editor.putLong("voip_result_out_num_packets", resultMap["voip_result_out_num_packets"] as Long)
        }

        // OBJECTIVES
        if (resultMap.containsKey("voip_objective_bits_per_sample")) {
            editor.putInt("voip_objective_bits_per_sample", resultMap["voip_objective_bits_per_sample"] as Int)
        }
        if (resultMap.containsKey("voip_objective_in_port")) {
            editor.putInt("voip_objective_in_port", resultMap["voip_objective_in_port"] as Int)
        }
        if (resultMap.containsKey("voip_objective_out_port")) {
            editor.putInt("voip_objective_out_port", resultMap["voip_objective_out_port"] as Int)
        }
        if (resultMap.containsKey("voip_objective_delay")) {
            editor.putLong("voip_objective_delay", resultMap["voip_objective_delay"] as Long)
        }
        if (resultMap.containsKey("voip_objective_timeout")) {
            editor.putLong("voip_objective_timeout", resultMap["voip_objective_timeout"] as Long)
        }
        if (resultMap.containsKey("voip_objective_payload")) {
            editor.putInt("voip_objective_payload", resultMap["voip_objective_payload"] as Int)
        }
        if (resultMap.containsKey("voip_objective_call_duration")) {
            editor.putLong("voip_objective_call_duration", resultMap["voip_objective_call_duration"] as Long)
        }
        if (resultMap.containsKey("voip_objective_sample_rate")) {
            editor.putInt("voip_objective_sample_rate", resultMap["voip_objective_sample_rate"] as Int)
        }

        // GENERAL
        if (resultMap.containsKey("duration_ns")) {
            editor.putLong("duration_ns", resultMap["duration_ns"] as Long)
        }
        if (resultMap.containsKey("start_time_ns")) {
            editor.putLong("start_time_ns", resultMap["start_time_ns"] as Long)
        }
        if (resultMap.containsKey("voip_result_status")) {
            editor.putString("voip_result_status", resultMap["voip_result_status"] as String?)
        }

        return editor.commit()
    }

    /**
     * Gets voip test results from shared preferences file
     */
    fun load(context: Context): VoipTestResult {
        val voipTestResult = VoipTestResult()
        val sharedPreferences = context.getSharedPreferences(VOIP_TEST_RESULT_SHARED_PREF_KEY, Context.MODE_PRIVATE)

        // IN
        if (sharedPreferences.contains("voip_result_in_num_packets")) {
            val v = sharedPreferences.getInt("voip_result_in_num_packets", -1)
            voipTestResult.resultInNumPackets = if (v == -1) null else v
        }
        if (sharedPreferences.contains("voip_result_in_long_seq")) {
            val v = sharedPreferences.getInt("voip_result_in_long_seq", -1)
            voipTestResult.resultInLongestSeqPackets = if (v == -1) null else v
        }
        if (sharedPreferences.contains("voip_result_in_short_seq")) {
            val v = sharedPreferences.getInt("voip_result_in_short_seq", -1)
            voipTestResult.resultInShortestSeqPackets = if (v == -1) null else v
        }
        if (sharedPreferences.contains("voip_result_in_mean_jitter")) {
            val v = sharedPreferences.getLong("voip_result_in_mean_jitter", Long.MIN_VALUE)
            voipTestResult.resultInMeanJitter = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_in_max_jitter")) {
            val v = sharedPreferences.getLong("voip_result_in_max_jitter", Long.MIN_VALUE)
            voipTestResult.resultInMaxJitter = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_in_sequence_error")) {
            val v = sharedPreferences.getInt("voip_result_in_sequence_error", Int.MIN_VALUE)
            voipTestResult.resultInSeqError = if (v == Int.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_in_skew")) {
            val v = sharedPreferences.getLong("voip_result_in_skew", Long.MIN_VALUE)
            voipTestResult.resultInSkew = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_in_max_delta")) {
            val v = sharedPreferences.getLong("voip_result_in_max_delta", Long.MIN_VALUE)
            voipTestResult.resultInMaxDelta = if (v == Long.MIN_VALUE) null else v
        }

        // OUT
        if (sharedPreferences.contains("voip_result_out_skew")) {
            val v = sharedPreferences.getLong("voip_result_out_skew", Long.MIN_VALUE)
            voipTestResult.resultOutSkew = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_out_max_delta")) {
            val v = sharedPreferences.getLong("voip_result_out_max_delta", Long.MIN_VALUE)
            voipTestResult.resultOutMaxDelta = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_out_sequence_error")) {
            val v = sharedPreferences.getLong("voip_result_out_sequence_error", Long.MIN_VALUE)
            voipTestResult.resultOutSeqError = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_out_long_seq")) {
            val v = sharedPreferences.getLong("voip_result_out_long_seq", Long.MIN_VALUE)
            voipTestResult.resultOutLongestSeqPackets = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_out_short_seq")) {
            val v = sharedPreferences.getLong("voip_result_out_short_seq", Long.MIN_VALUE)
            voipTestResult.resultOutShortestSeqPackets = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_out_mean_jitter")) {
            val v = sharedPreferences.getLong("voip_result_out_mean_jitter", Long.MIN_VALUE)
            voipTestResult.resultOutMeanJitter = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_out_max_jitter")) {
            val v = sharedPreferences.getLong("voip_result_out_max_jitter", Long.MIN_VALUE)
            voipTestResult.resultOutMaxJitter = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_out_num_packets")) {
            val v = sharedPreferences.getLong("voip_result_out_num_packets", Long.MIN_VALUE)
            voipTestResult.resultOutNumPackets = if (v == Long.MIN_VALUE) null else v
        }

        // OBJECTIVES
        if (sharedPreferences.contains("voip_objective_bits_per_sample")) {
            val v = sharedPreferences.getInt("voip_objective_bits_per_sample", Int.MIN_VALUE)
            voipTestResult.objectiveBitsPerSample = if (v == Int.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_objective_in_port")) {
            val v = sharedPreferences.getInt("voip_objective_in_port", Int.MIN_VALUE)
            voipTestResult.objectivePortIn = if (v == Int.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_objective_out_port")) {
            val v = sharedPreferences.getInt("voip_objective_out_port", Int.MIN_VALUE)
            voipTestResult.objectivePortOut = if (v == Int.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_objective_delay")) {
            val v = sharedPreferences.getLong("voip_objective_delay", Long.MIN_VALUE)
            voipTestResult.objectiveDelay = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_objective_timeout")) {
            val v = sharedPreferences.getLong("voip_objective_timeout", Long.MIN_VALUE)
            voipTestResult.objectiveTimeoutNS = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_objective_payload")) {
            val v = sharedPreferences.getInt("voip_objective_payload", Int.MIN_VALUE)
            voipTestResult.objectivePayload = if (v == Int.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_objective_call_duration")) {
            val v = sharedPreferences.getLong("voip_objective_call_duration", Long.MIN_VALUE)
            voipTestResult.objectiveCallDuration = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_objective_sample_rate")) {
            val v = sharedPreferences.getInt("voip_objective_sample_rate", Int.MIN_VALUE)
            voipTestResult.objectiveBitsPerSample = if (v == Int.MIN_VALUE) null else v
        }

        // GENERAL
        if (sharedPreferences.contains("duration_ns")) {
            val v = sharedPreferences.getLong("duration_ns", Long.MIN_VALUE)
            voipTestResult.objectiveDelay = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("start_time_ns")) {
            val v = sharedPreferences.getLong("start_time_ns", Long.MIN_VALUE)
            voipTestResult.startTimeInNS = if (v == Long.MIN_VALUE) null else v
        }
        if (sharedPreferences.contains("voip_result_status")) {
            val voipResultStatus = sharedPreferences.getString("voip_result_status", TestResultConst.TEST_RESULT_ERROR)
            voipTestResult.testResultStatus = voipResultStatus
        }

        return voipTestResult
    }

    /**
     * Clears shared preferences file with voip test result
     */
    fun delete(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(VOIP_TEST_RESULT_SHARED_PREF_KEY, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        return editor.commit()
    }

    companion object {
        private const val VOIP_TEST_RESULT_SHARED_PREF_KEY = "VOIP_TEST_RESULT_SHARED_PREF"
    }
}
