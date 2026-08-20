// Copyright 2009 Google Inc. All Rights Reserved.

package net.measurementlab.ndt

/**
 * Provides platform-specific UI services. Defines several functions for
 * platform-specific classes to implement, to help the working thread dispatch
 * messages to the UI.
 */
interface UiServices {

    /**
     * Sends message to the designated view.
     */
    fun appendString(str: String?, viewId: Int)

    /**
     * Called each time a test step completes.
     */
    fun incrementProgress()

    /**
     * Notifies the callee that the test is starting.
     */
    fun onBeginTest()

    /**
     * Notifies the callee that the test has ended.
     */
    fun onEndTest()

    /**
     * Called when the test ends abnormally. Should be called before onEndTest();
     */
    fun onFailure(errorMessage: String?)

    /**
     * Called when packet queuing is detected.
     */
    fun onPacketQueuingDetected()

    /**
     * Called when the test 'login' packet has been successfully sent.
     */
    fun onLoginSent()

    /**
     * Abstract to make the logging action generic in different platforms.
     */
    fun logError(str: String?)

    /**
     * Updates the status message, which indicates what test is running.
     */
    fun updateStatus(status: String?)

    /**
     * Updates the status panel. (Applet-specific)
     */
    fun updateStatusPanel(status: String?)

    /**
     * Returns true if the test should be aborted, false if it should continue.
     */
    fun wantToStop(): Boolean

    fun getClientApp(): String?

    // Hack for the Applet's JavaScript access API extension
    fun setVariable(name: String?, value: Int)
    fun setVariable(name: String?, value: Double)
    fun setVariable(name: String?, value: Any?)

    companion object {
        /**
         * Id refers to the main view for sending the output string.
         */
        const val MAIN_VIEW = 0

        /**
         * Id refers to the statistics view for sending the output string.
         */
        const val STAT_VIEW = 1

        /**
         * Id refers to the diagnose view for sending the output string.
         */
        const val DIAG_VIEW = 2

        /**
         * Id refers to the debug view for sending the output string.
         */
        const val DEBUG_VIEW = 3

        /**
         * Maximum test steps for ProgressBar setting.
         */
        const val TEST_STEPS = 7
    }
}
