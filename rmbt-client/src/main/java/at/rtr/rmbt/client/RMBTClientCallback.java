package at.rtr.rmbt.client;

import java.util.List;

public interface RMBTClientCallback {

    void onThreadDownloadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onThreadUploadDataChanged(int threadId, long timeNanos, long bytesTotal);

    void onPingDataChanged(List<Ping> pings);
}
