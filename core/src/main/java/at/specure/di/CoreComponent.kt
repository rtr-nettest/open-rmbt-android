package at.specure.di

import at.specure.measurement.MeasurementService
import at.specure.measurement.signal.SignalMeasurementService
import at.specure.worker.request.SendDataWorker
import at.specure.worker.request.SettingsWorker
import at.specure.worker.request.SignalMeasurementChunkWorker
import at.specure.worker.request.SignalMeasurementInfoWorker

/**
 * Core component interface that must be extended by main Dagger component to contribute injections
 */
interface CoreComponent {

    fun inject(settingsWorker: SettingsWorker)

    fun inject(sendDataWorker: SendDataWorker)

    fun inject(worker: SignalMeasurementInfoWorker)

    fun inject(worker: SignalMeasurementChunkWorker)

    fun inject(service: MeasurementService)

    fun inject(service: SignalMeasurementService)
}