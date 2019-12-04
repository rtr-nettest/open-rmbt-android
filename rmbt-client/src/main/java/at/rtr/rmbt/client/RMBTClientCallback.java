package at.rtr.rmbt.client;

public interface RMBTClientCallback {

    void onThreadDownloadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onThreadUploadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onPingDataChanged(long clientPing, long serverPing, long timeNs);
}
