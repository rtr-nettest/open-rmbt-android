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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Created by michal.cadrik on 7/6/2017.
 * <p>
 * Class dedicated to save and load voip test result from shared preferences
 */

@SuppressWarnings("UnnecessaryLocalVariable")
public class VoipTestResultHandler {

    private static final String VOIP_TEST_RESULT_SHARED_PREF_KEY = "VOIP_TEST_RESULT_SHARED_PREF";

    /**
     * Converts results hash map to object used to send as Json
     *
     * @param resultMap
     * @return
     */
    public VoipTestResult convertResultsToObject(HashMap<String, Object> resultMap) {

        VoipTestResult voipTestResult = new VoipTestResult();

        //IN
        if (resultMap.containsKey("voip_result_in_num_packets"))
            voipTestResult.setResultInNumPackets((Integer) resultMap.get("voip_result_in_num_packets"));

        if (resultMap.containsKey("voip_result_in_long_seq"))
            voipTestResult.setResultInLongestSeqPackets((Integer) resultMap.get("voip_result_in_long_seq"));

        if (resultMap.containsKey("voip_result_in_short_seq"))
            voipTestResult.setResultInShortestSeqPackets((Integer) resultMap.get("voip_result_in_short_seq"));

        if (resultMap.containsKey("voip_result_in_mean_jitter"))
            voipTestResult.setResultInMeanJitter((Long) resultMap.get("voip_result_in_mean_jitter"));

        if (resultMap.containsKey("voip_result_in_max_jitter"))
            voipTestResult.setResultInMaxJitter((Long) resultMap.get("voip_result_in_max_jitter"));

        if (resultMap.containsKey("voip_result_in_sequence_error"))
            voipTestResult.setResultInSeqError((Integer) resultMap.get("voip_result_in_sequence_error"));

        if (resultMap.containsKey("voip_result_in_skew"))
            voipTestResult.setResultInSkew((Long) resultMap.get("voip_result_in_skew"));

        if (resultMap.containsKey("voip_result_in_max_delta"))
            voipTestResult.setResultInMaxDelta((Long) resultMap.get("voip_result_in_max_delta"));


        //OUT

        if (resultMap.containsKey("voip_result_out_skew"))
            voipTestResult.setResultOutSkew((Long) resultMap.get("voip_result_out_skew"));

        if (resultMap.containsKey("voip_result_out_max_delta"))
            voipTestResult.setResultOutMaxDelta((Long) resultMap.get("voip_result_out_max_delta"));

        if (resultMap.containsKey("voip_result_out_sequence_error"))
            voipTestResult.setResultOutSeqError((Long) resultMap.get("voip_result_out_sequence_error"));

        if (resultMap.containsKey("voip_result_out_long_seq"))
            voipTestResult.setResultOutLongestSeqPackets((Long) resultMap.get("voip_result_out_long_seq"));

        if (resultMap.containsKey("voip_result_out_short_seq"))
            voipTestResult.setResultOutShortestSeqPackets((Long) resultMap.get("voip_result_out_short_seq"));

        if (resultMap.containsKey("voip_result_out_mean_jitter"))
            voipTestResult.setResultOutMeanJitter((Long) resultMap.get("voip_result_out_mean_jitter"));

        if (resultMap.containsKey("voip_result_out_max_jitter"))
            voipTestResult.setResultOutMaxJitter((Long) resultMap.get("voip_result_out_max_jitter"));

        if (resultMap.containsKey("voip_result_out_num_packets"))
            voipTestResult.setResultOutNumPackets((Long) resultMap.get("voip_result_out_num_packets"));


        //OBJECTIVES

        if (resultMap.containsKey("voip_objective_bits_per_sample"))
            voipTestResult.setObjectiveBitsPerSample((Integer) resultMap.get("voip_objective_bits_per_sample"));

        if (resultMap.containsKey("voip_objective_in_port"))
            voipTestResult.setObjectivePortIn((Integer) resultMap.get("voip_objective_in_port"));

        if (resultMap.containsKey("voip_objective_out_port"))
            voipTestResult.setObjectivePortOut((Integer) resultMap.get("voip_objective_out_port"));

        if (resultMap.containsKey("voip_objective_delay"))
            voipTestResult.setObjectiveDelay((Long) resultMap.get("voip_objective_delay"));

        if (resultMap.containsKey("voip_objective_timeout"))
            voipTestResult.setObjectiveTimeoutNS((Long) resultMap.get("voip_objective_timeout"));

        if (resultMap.containsKey("voip_objective_payload"))
            voipTestResult.setObjectivePayload((Integer) resultMap.get("voip_objective_payload"));

        if (resultMap.containsKey("voip_objective_call_duration"))
            voipTestResult.setObjectiveCallDuration((Long) resultMap.get("voip_objective_call_duration"));

        if (resultMap.containsKey("voip_objective_sample_rate"))
            voipTestResult.setObjectiveBitsPerSample((Integer) resultMap.get("voip_objective_sample_rate"));


        //GENERAL

        if (resultMap.containsKey("duration_ns"))
            voipTestResult.setTestDurationInNS((Long) resultMap.get("duration_ns"));

        if (resultMap.containsKey("start_time_ns"))
            voipTestResult.setStartTimeInNS((Long) resultMap.get("start_time_ns"));

        if (resultMap.containsKey("voip_result_status"))
            voipTestResult.setTestResultStatus((String) resultMap.get("voip_result_status"));

        return voipTestResult;
    }

    /**
     * Saves result hash map to shared preferences
     *
     * @param resultMap
     * @param context
     * @return
     */
    public boolean save(HashMap<String, Object> resultMap, @NonNull Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(VOIP_TEST_RESULT_SHARED_PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        //IN
        if (resultMap.containsKey("voip_result_in_num_packets"))
            editor.putInt("voip_result_in_num_packets", (Integer) resultMap.get("voip_result_in_num_packets"));

        if (resultMap.containsKey("voip_result_in_long_seq"))
            editor.putInt("voip_result_in_long_seq", (Integer) resultMap.get("voip_result_in_long_seq"));

        if (resultMap.containsKey("voip_result_in_short_seq"))
            editor.putInt("voip_result_in_short_seq", (Integer) resultMap.get("voip_result_in_short_seq"));

        if (resultMap.containsKey("voip_result_in_mean_jitter"))
            editor.putLong("voip_result_in_mean_jitter", (Long) resultMap.get("voip_result_in_mean_jitter"));

        if (resultMap.containsKey("voip_result_in_max_jitter"))
            editor.putLong("voip_result_in_max_jitter", (Long) resultMap.get("voip_result_in_max_jitter"));

        if (resultMap.containsKey("voip_result_in_sequence_error"))
            editor.putLong("voip_result_in_sequence_error", (Integer) resultMap.get("voip_result_in_sequence_error"));

        if (resultMap.containsKey("voip_result_in_skew"))
            editor.putLong("voip_result_in_skew", (Long) resultMap.get("voip_result_in_skew"));

        if (resultMap.containsKey("voip_result_in_max_delta"))
            editor.putLong("voip_result_in_max_delta", (Long) resultMap.get("voip_result_in_max_delta"));


        //OUT

        if (resultMap.containsKey("voip_result_out_skew"))
            editor.putLong("voip_result_out_skew", (Long) resultMap.get("voip_result_out_skew"));

        if (resultMap.containsKey("voip_result_out_max_delta"))
            editor.putLong("voip_result_out_max_delta", (Long) resultMap.get("voip_result_out_max_delta"));

        if (resultMap.containsKey("voip_result_out_sequence_error"))
            editor.putLong("voip_result_out_sequence_error", (Long) resultMap.get("voip_result_out_sequence_error"));

        if (resultMap.containsKey("voip_result_out_long_seq"))
            editor.putLong("voip_result_out_long_seq", (Long) resultMap.get("voip_result_out_long_seq"));

        if (resultMap.containsKey("voip_result_out_short_seq"))
            editor.putLong("voip_result_out_short_seq", (Long) resultMap.get("voip_result_out_short_seq"));

        if (resultMap.containsKey("voip_result_out_mean_jitter"))
            editor.putLong("voip_result_out_mean_jitter", (Long) resultMap.get("voip_result_out_mean_jitter"));

        if (resultMap.containsKey("voip_result_out_max_jitter"))
            editor.putLong("voip_result_out_max_jitter", (Long) resultMap.get("voip_result_out_max_jitter"));

        if (resultMap.containsKey("voip_result_out_num_packets"))
            editor.putLong("voip_result_out_num_packets", (Long) resultMap.get("voip_result_out_num_packets"));


        //OBJECTIVES

        if (resultMap.containsKey("voip_objective_bits_per_sample"))
            editor.putInt("voip_objective_bits_per_sample", (Integer) resultMap.get("voip_objective_bits_per_sample"));

        if (resultMap.containsKey("voip_objective_in_port"))
            editor.putInt("voip_objective_in_port", (Integer) resultMap.get("voip_objective_in_port"));

        if (resultMap.containsKey("voip_objective_out_port"))
            editor.putInt("voip_objective_out_port", (Integer) resultMap.get("voip_objective_out_port"));

        if (resultMap.containsKey("voip_objective_delay"))
            editor.putLong("voip_objective_delay", (Long) resultMap.get("voip_objective_delay"));

        if (resultMap.containsKey("voip_objective_timeout"))
            editor.putLong("voip_objective_timeout", (Long) resultMap.get("voip_objective_timeout"));

        if (resultMap.containsKey("voip_objective_payload"))
            editor.putInt("voip_objective_payload", (Integer) resultMap.get("voip_objective_payload"));

        if (resultMap.containsKey("voip_objective_call_duration"))
            editor.putLong("voip_objective_call_duration", (Long) resultMap.get("voip_objective_call_duration"));

        if (resultMap.containsKey("voip_objective_sample_rate"))
            editor.putInt("voip_objective_sample_rate", (Integer) resultMap.get("voip_objective_sample_rate"));


        //GENERAL

        if (resultMap.containsKey("duration_ns"))
            editor.putLong("duration_ns", (Long) resultMap.get("duration_ns"));

        if (resultMap.containsKey("start_time_ns"))
            editor.putLong("start_time_ns", (Long) resultMap.get("start_time_ns"));

        if (resultMap.containsKey("voip_result_status"))
            editor.putString("voip_result_status", (String) resultMap.get("voip_result_status"));

        boolean successfullySaved = editor.commit();

        return successfullySaved;
    }

    /**
     * Gets voip test results from shared preferences file
     *
     * @param context
     * @return object filed with data
     */
    public VoipTestResult load(@NonNull Context context) {
        VoipTestResult voipTestResult = new VoipTestResult();
        SharedPreferences sharedPreferences = context.getSharedPreferences(VOIP_TEST_RESULT_SHARED_PREF_KEY, Context.MODE_PRIVATE);

        //IN
        if (sharedPreferences.contains("voip_result_in_num_packets")) {
            int voip_result_in_num_packets = sharedPreferences.getInt("voip_result_in_num_packets", -1);
            voipTestResult.setResultInNumPackets(voip_result_in_num_packets == -1 ? null : voip_result_in_num_packets);
        }

        if (sharedPreferences.contains("voip_result_in_long_seq")) {
            int voip_result_in_long_seq = sharedPreferences.getInt("voip_result_in_long_seq", -1);
            voipTestResult.setResultInLongestSeqPackets(voip_result_in_long_seq == -1 ? null : voip_result_in_long_seq);
        }

        if (sharedPreferences.contains("voip_result_in_short_seq")) {
            int voip_result_in_short_seq = sharedPreferences.getInt("voip_result_in_short_seq", -1);
            voipTestResult.setResultInShortestSeqPackets(voip_result_in_short_seq == -1 ? null : voip_result_in_short_seq);
        }

        if (sharedPreferences.contains("voip_result_in_mean_jitter")) {
            long voip_result_in_mean_jitter = sharedPreferences.getLong("voip_result_in_mean_jitter", Long.MIN_VALUE);
            voipTestResult.setResultInMeanJitter(voip_result_in_mean_jitter == Long.MIN_VALUE ? null : voip_result_in_mean_jitter);
        }

        if (sharedPreferences.contains("voip_result_in_max_jitter")) {
            long voip_result_in_max_jitter = sharedPreferences.getLong("voip_result_in_max_jitter", Long.MIN_VALUE);
            voipTestResult.setResultInMaxJitter(voip_result_in_max_jitter == Long.MIN_VALUE ? null : voip_result_in_max_jitter);
        }

        if (sharedPreferences.contains("voip_result_in_sequence_error")) {
            int voip_result_in_sequence_error = sharedPreferences.getInt("voip_result_in_sequence_error", Integer.MIN_VALUE);
            voipTestResult.setResultInSeqError(voip_result_in_sequence_error == Integer.MIN_VALUE ? null : voip_result_in_sequence_error);
        }

        if (sharedPreferences.contains("voip_result_in_skew")) {
            long voip_result_in_skew = sharedPreferences.getLong("voip_result_in_skew", Long.MIN_VALUE);
            voipTestResult.setResultInSkew(voip_result_in_skew == Long.MIN_VALUE ? null : voip_result_in_skew);
        }

        if (sharedPreferences.contains("voip_result_in_max_delta")) {
            long voip_result_in_max_delta = sharedPreferences.getLong("voip_result_in_max_delta", Long.MIN_VALUE);
            voipTestResult.setResultInMaxDelta(voip_result_in_max_delta == Long.MIN_VALUE ? null : voip_result_in_max_delta);
        }


        //OUT

        if (sharedPreferences.contains("voip_result_out_skew")) {
            long voip_result_out_skew = sharedPreferences.getLong("voip_result_out_skew", Long.MIN_VALUE);
            voipTestResult.setResultOutSkew(voip_result_out_skew == Long.MIN_VALUE ? null : voip_result_out_skew);
        }

        if (sharedPreferences.contains("voip_result_out_max_delta")) {
            long voip_result_out_max_delta = sharedPreferences.getLong("voip_result_out_max_delta", Long.MIN_VALUE);
            voipTestResult.setResultOutMaxDelta(voip_result_out_max_delta == Long.MIN_VALUE ? null : voip_result_out_max_delta);
        }

        if (sharedPreferences.contains("voip_result_out_sequence_error")) {
            long voip_result_out_sequence_error = sharedPreferences.getLong("voip_result_out_sequence_error", Long.MIN_VALUE);
            voipTestResult.setResultOutSeqError(voip_result_out_sequence_error == Long.MIN_VALUE ? null : voip_result_out_sequence_error);
        }

        if (sharedPreferences.contains("voip_result_out_long_seq")) {
            long voip_result_out_long_seq = sharedPreferences.getLong("voip_result_out_long_seq", Long.MIN_VALUE);
            voipTestResult.setResultOutLongestSeqPackets(voip_result_out_long_seq == Long.MIN_VALUE ? null : voip_result_out_long_seq);
        }

        if (sharedPreferences.contains("voip_result_out_short_seq")) {
            long voip_result_out_short_seq = sharedPreferences.getLong("voip_result_out_short_seq", Long.MIN_VALUE);
            voipTestResult.setResultOutShortestSeqPackets(voip_result_out_short_seq == Long.MIN_VALUE ? null : voip_result_out_short_seq);
        }

        if (sharedPreferences.contains("voip_result_out_mean_jitter")) {
            long voip_result_out_mean_jitter = sharedPreferences.getLong("voip_result_out_mean_jitter", Long.MIN_VALUE);
            voipTestResult.setResultOutMeanJitter(voip_result_out_mean_jitter == Long.MIN_VALUE ? null : voip_result_out_mean_jitter);
        }

        if (sharedPreferences.contains("voip_result_out_max_jitter")) {
            long voip_result_out_max_jitter = sharedPreferences.getLong("voip_result_out_max_jitter", Long.MIN_VALUE);
            voipTestResult.setResultOutMaxJitter(voip_result_out_max_jitter == Long.MIN_VALUE ? null : voip_result_out_max_jitter);
        }

        if (sharedPreferences.contains("voip_result_out_num_packets")) {
            long voip_result_out_num_packets = sharedPreferences.getLong("voip_result_out_num_packets", Long.MIN_VALUE);
            voipTestResult.setResultOutNumPackets(voip_result_out_num_packets == Long.MIN_VALUE ? null : voip_result_out_num_packets);
        }


        //OBJECTIVES

        if (sharedPreferences.contains("voip_objective_bits_per_sample")) {
            int voip_objective_bits_per_sample = sharedPreferences.getInt("voip_objective_bits_per_sample", Integer.MIN_VALUE);
            voipTestResult.setObjectiveBitsPerSample(voip_objective_bits_per_sample == Integer.MIN_VALUE ? null : voip_objective_bits_per_sample);
        }

        if (sharedPreferences.contains("voip_objective_in_port")) {
            int voip_objective_in_port = sharedPreferences.getInt("voip_objective_in_port", Integer.MIN_VALUE);
            voipTestResult.setObjectivePortIn(voip_objective_in_port == Integer.MIN_VALUE ? null : voip_objective_in_port);
        }

        if (sharedPreferences.contains("voip_objective_out_port")) {
            int voip_objective_out_port = sharedPreferences.getInt("voip_objective_out_port", Integer.MIN_VALUE);
            voipTestResult.setObjectivePortOut(voip_objective_out_port == Integer.MIN_VALUE ? null : voip_objective_out_port);
        }

        if (sharedPreferences.contains("voip_objective_delay")) {
            long voip_objective_delay = sharedPreferences.getLong("voip_objective_delay", Long.MIN_VALUE);
            voipTestResult.setObjectiveDelay(voip_objective_delay == Long.MIN_VALUE ? null : voip_objective_delay);
        }

        if (sharedPreferences.contains("voip_objective_timeout")) {
            long voip_objective_timeout = sharedPreferences.getLong("voip_objective_timeout", Long.MIN_VALUE);
            voipTestResult.setObjectiveTimeoutNS(voip_objective_timeout == Long.MIN_VALUE ? null : voip_objective_timeout);
        }

        if (sharedPreferences.contains("voip_objective_payload")) {
            int voip_objective_payload = sharedPreferences.getInt("voip_objective_payload", Integer.MIN_VALUE);
            voipTestResult.setObjectivePayload(voip_objective_payload == Integer.MIN_VALUE ? null : voip_objective_payload);
        }

        if (sharedPreferences.contains("voip_objective_call_duration")) {
            long voip_objective_call_duration = sharedPreferences.getLong("voip_objective_call_duration", Long.MIN_VALUE);
            voipTestResult.setObjectiveCallDuration(voip_objective_call_duration == Long.MIN_VALUE ? null : voip_objective_call_duration);
        }

        if (sharedPreferences.contains("voip_objective_sample_rate")) {
            int voip_objective_sample_rate = sharedPreferences.getInt("voip_objective_sample_rate", Integer.MIN_VALUE);
            voipTestResult.setObjectiveBitsPerSample(voip_objective_sample_rate == Integer.MIN_VALUE ? null : voip_objective_sample_rate);
        }


        //GENERAL

        if (sharedPreferences.contains("duration_ns")) {
            long duration_ns = sharedPreferences.getLong("duration_ns", Long.MIN_VALUE);
            voipTestResult.setObjectiveDelay(duration_ns == Long.MIN_VALUE ? null : duration_ns);
        }

        if (sharedPreferences.contains("start_time_ns")) {
            long start_time_ns = sharedPreferences.getLong("start_time_ns", Long.MIN_VALUE);
            voipTestResult.setStartTimeInNS(start_time_ns == Long.MIN_VALUE ? null : start_time_ns);
        }

        if (sharedPreferences.contains("voip_result_status")) {
            String voip_result_status = sharedPreferences.getString("voip_result_status", TestResultConst.TEST_RESULT_ERROR);
            voipTestResult.setTestResultStatus(voip_result_status);
        }


        return voipTestResult;
    }

    /**
     * Clears shared preferences file with voip test result
     *
     * @param context
     * @return
     */
    public boolean delete(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(VOIP_TEST_RESULT_SHARED_PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        return editor.commit();
    }

}
