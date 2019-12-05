package at.rtr.rmbt.client;

import at.rtr.rmbt.client.helper.TestStatus;

public interface RMBTClientCallback {

    void onClientReady(String testUUID, String loopUUID, String testToken, long testStartTimeNanos, int threadNumber);

    void onThreadDownloadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onThreadUploadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onPingDataChanged(long clientPing, long serverPing, long timeNs);

    void onResultUpdated(TotalTestResult result, TestStatus status);
}
