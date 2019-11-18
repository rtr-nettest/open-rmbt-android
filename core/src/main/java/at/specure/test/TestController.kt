package at.specure.test

interface TestController {

    fun start(listener: TestProgressListener)

    fun stop()
}