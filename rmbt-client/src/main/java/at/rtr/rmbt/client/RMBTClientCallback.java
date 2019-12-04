package at.rtr.rmbt.client;

public interface RMBTClientCallback {

    void onClientReady(String testUUID, String loopUUID, String testToken, long testStartTimeNanos, int threadNumber);

    void onThreadDownloadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onThreadUploadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onPingDataChanged(long clientPing, long serverPing, long timeNs);

    void onResultUpdated(TotalTestResult result);
}
