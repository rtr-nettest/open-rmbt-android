package at.rtr.rmbt.client;

import at.rtr.rmbt.client.helper.TestStatus;

public interface RMBTClientCallback {

    void onClientReady(String testUUID, String loopUUID, String testToken, long testStartTimeNanos, int threadNumber);

    void onSpeedDataChanged(int threadId, long bytes, long timestampNanos, boolean isUpload);

    void onPingDataChanged(long clientPing, long serverPing, long timeNs);

    void onTestCompleted(TotalTestResult result);

    void onTestStatusUpdate(TestStatus status);
}
