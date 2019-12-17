package at.specure.di

import at.specure.measurement.MeasurementService
import at.specure.worker.request.SendDataWorker
import at.specure.worker.request.SettingsWorker

/**
 * Core component interface that must be extended by main Dagger component to contribute injections
 */
interface CoreComponent {

    fun inject(settingsWorker: SettingsWorker)

    fun inject(sendDataWorker: SendDataWorker)

    fun inject(service: MeasurementService)
}