package at.rtr.rmbt.android.di

import at.rtr.rmbt.android.ui.dialog.IpInfoDialog
import at.rtr.rmbt.android.ui.dialog.LocationInfoDialog
import at.specure.measurement.MeasurementService
import at.specure.worker.request.SendDataWorker
import at.specure.worker.request.SettingsWorker

/**
 * Keeps and delegates all calls to [AppComponent]
 */
object Injector : AppComponent {

    lateinit var component: AppComponent

    override fun viewModelFactory() = component.viewModelFactory()

    override fun inject(dialog: IpInfoDialog) = component.inject(dialog)

    override fun inject(dialog: LocationInfoDialog) = component.inject(dialog)

    override fun inject(settingsWorker: SettingsWorker) = component.inject(settingsWorker)

    override fun inject(sendDataWorker: SendDataWorker) = component.inject(sendDataWorker)

    override fun inject(service: MeasurementService) = component.inject(service)
}