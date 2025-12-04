package at.rtr.rmbt.android.di

import at.rtr.rmbt.android.App
import at.rtr.rmbt.android.ui.dialog.CoverageSettingsDialog
import at.rtr.rmbt.android.ui.dialog.HistoryDownloadDialog
import at.rtr.rmbt.android.ui.dialog.HistoryFiltersDialog
import at.rtr.rmbt.android.ui.dialog.IpInfoDialog
import at.rtr.rmbt.android.ui.dialog.LocationInfoDialog
import at.rtr.rmbt.android.ui.dialog.NetworkInfoDialog
import at.rtr.rmbt.android.ui.dialog.SyncDevicesDialog
import at.specure.measurement.MeasurementService
import at.specure.measurement.coverage.data.workers.CoverageSyncWorker
import at.specure.measurement.signal.SignalMeasurementService
import at.specure.worker.request.CoverageMeasurementWorker
import at.specure.worker.request.SendDataWorker
import at.specure.worker.request.SettingsWorker
import at.specure.worker.request.SignalMeasurementChunkWorker
import at.specure.worker.request.SignalMeasurementInfoWorker

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

    override fun inject(dialog: SyncDevicesDialog) = component.inject(dialog)

    override fun inject(service: SignalMeasurementService) = component.inject(service)

    override fun inject(dialog: HistoryFiltersDialog) = component.inject(dialog)

    override fun inject(worker: SignalMeasurementInfoWorker) = component.inject(worker)

    override fun inject(worker: SignalMeasurementChunkWorker) = component.inject(worker)

    override fun inject(worker: CoverageMeasurementWorker) = component.inject(worker)

    override fun inject(worker: CoverageSyncWorker) = component.inject(worker)

    override fun inject(dialog: NetworkInfoDialog) = component.inject(dialog)

    override fun inject(dialog: HistoryDownloadDialog) = component.inject(dialog)

    override fun inject(dialog: CoverageSettingsDialog) = component.inject(dialog)

    override fun inject(app: App) = component.inject(app)
}