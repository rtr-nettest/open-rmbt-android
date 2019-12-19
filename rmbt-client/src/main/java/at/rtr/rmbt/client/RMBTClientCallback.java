package at.rtr.rmbt.client;

import org.jetbrains.annotations.Nullable;

import at.rtr.rmbt.client.helper.TestStatus;
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector;

public interface RMBTClientCallback {

    void onClientReady(String testUUID, String loopUUID, String testToken, long testStartTimeNanos, int threadNumber);

    void onSpeedDataChanged(int threadId, long bytes, long timestampNanos, boolean isUpload);

    void onPingDataChanged(long clientPing, long serverPing, long timeNs);

    void onTestCompleted(TotalTestResult result, boolean waitQoSResults);

    void onQoSTestCompleted(@Nullable QoSResultCollector qosResult);

    void onTestStatusUpdate(TestStatus status);
}
