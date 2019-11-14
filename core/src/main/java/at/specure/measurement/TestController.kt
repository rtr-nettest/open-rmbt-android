package at.specure.measurement

import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo

interface TestController {

    var stateListener: ((state: MeasurementState, progress: Int) -> (Unit))?
    var pingListener: ((pingMs: Long) -> (Unit))?
    var downloadSpeedListener: ((speedBps: Long) -> Unit)?
    var uploadSpeedListener: ((speedBps: Long) -> Unit)?
    var signalStrengthListener: ((signalStrengthInfo: SignalStrengthInfo?) -> (Unit))?
    var networkInfoListener: ((networkInfo: NetworkInfo?) -> (Unit))?

    fun start()

    fun stop()
}