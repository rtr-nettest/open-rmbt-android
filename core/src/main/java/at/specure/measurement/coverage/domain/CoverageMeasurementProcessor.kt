package at.specure.measurement.coverage.domain

interface CoverageMeasurementProcessor {

    fun start()

    fun stop()

    fun pause()

    fun getData()
}