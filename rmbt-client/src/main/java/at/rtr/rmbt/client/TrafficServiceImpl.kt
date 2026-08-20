package at.rtr.rmbt.client

import android.net.TrafficStats
import at.rtr.rmbt.client.v2.task.service.TrafficService

class TrafficServiceImpl : TrafficService {

    private var trafficRxStart: Long = -1

    private var trafficTxStart: Long = -1

    private var trafficRxEnd: Long = -1

    private var trafficTxEnd: Long = -1

    var running = false

    override fun start(): Int {
        trafficRxStart = TrafficStats.getTotalRxBytes()
        if (trafficRxStart == TrafficStats.UNSUPPORTED.toLong()) {
            return TrafficService.SERVICE_NOT_SUPPORTED
        }
        running = true
        trafficTxStart = TrafficStats.getTotalTxBytes()
        return TrafficService.SERVICE_START_OK
    }

    override fun getTxBytes(): Long =
        if (trafficTxStart != -1L) trafficTxEnd - trafficTxStart else -1

    override fun getRxBytes(): Long =
        if (trafficRxStart != -1L) trafficRxEnd - trafficRxStart else -1

    override fun stop() {
        if (running) {
            running = false
            trafficTxEnd = TrafficStats.getTotalTxBytes()
            trafficRxEnd = TrafficStats.getTotalRxBytes()
        }
    }

    override fun getTotalTxBytes(): Long = TrafficStats.getTotalTxBytes()

    override fun getTotalRxBytes(): Long = TrafficStats.getTotalRxBytes()

    override fun getCurrentTxBytes(): Long =
        if (trafficTxStart != -1L) getTotalTxBytes() - trafficTxStart else -1

    override fun getCurrentRxBytes(): Long =
        if (trafficRxStart != -1L) getTotalRxBytes() - trafficRxStart else -1

    override fun update() {
        if (running) {
            stop()
            running = true
        }
    }
}
