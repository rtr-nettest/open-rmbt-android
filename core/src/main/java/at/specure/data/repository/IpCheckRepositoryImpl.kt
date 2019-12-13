package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.IpClient
import at.rmbt.client.control.IpInfoResponse
import at.rmbt.client.control.IpRequestBody
import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.toCapabilitiesBody
import at.specure.data.toIpRequest
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationWatcher
import at.specure.test.DeviceInfo

class IpCheckRepositoryImpl(
    context: Context,
    private val config: Config,
    private val clientUUID: ClientUUID,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val client: IpClient
) : IpCheckRepository {

    private val deviceInfo = DeviceInfo(context)

    private val ipRequestBody: IpRequestBody
        get() = deviceInfo.toIpRequest(
            clientUUID = clientUUID.value,
            location = locationWatcher.getLatestLocationInfo(),
            signalStrengthInfo = signalStrengthWatcher.lastSignalStrength,
            capabilities = config.toCapabilitiesBody()
        )

    @Throws(HandledException::class)
    override fun getPublicIpV4Address(): Maybe<IpInfoResponse> = client.getPublicIpV4Address(ipRequestBody)

    override fun getPublicIpV6Address(): Maybe<IpInfoResponse> = client.getPublicIpV6Address(ipRequestBody)

    override fun getPrivateIpV4Address(): Maybe<IpInfoResponse> = client.getPrivateIpV4Address()

    override fun getPrivateIpV6Address(): Maybe<IpInfoResponse> = client.getPrivateIpV6Address()
}