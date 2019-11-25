package at.rtr.rmbt.client;

public interface RMBTClientCallback {

    void onThreadDownloadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onThreadUploadDataChanged(int threadId, long timeNanos, long bytesTotal);
}
