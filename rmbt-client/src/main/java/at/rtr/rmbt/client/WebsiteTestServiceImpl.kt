package at.rtr.rmbt.client

import android.content.Context
import android.graphics.Bitmap
import android.net.TrafficStats
import android.os.Handler
import android.os.Process
import android.webkit.WebView
import android.webkit.WebViewClient
import at.rtr.rmbt.client.v2.task.service.WebsiteTestService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WebsiteTestServiceImpl(private val context: Context) : WebsiteTestService {

    private var webView: WebView? = null

    private val isRunning = AtomicBoolean(false)

    private val hasFinished = AtomicBoolean(false)

    private val hasError = AtomicBoolean(false)

    private val resourceCount = AtomicInteger(0)

    private var statusCode = -1

    private var duration: Long = -1

    private var listener: WebsiteTestService.RenderingListener? = null

    private var trafficRxStart: Long = 0

    private var trafficTxStart: Long = 0

    private var trafficRxEnd: Long = 0

    private var trafficTxEnd: Long = 0

    private val handler: Handler = Handler(context.mainLooper)

    private var processUid = 0

    override fun getInstance(): WebsiteTestServiceImpl {
        return WebsiteTestServiceImpl(context)
    }

    override fun getHash(): String? {
        return null
    }

    override fun getDownloadDuration(): Long = duration

    override fun run(targetUrl: String?, timeOut: Long) {
        handler.post {
            this@WebsiteTestServiceImpl.processUid = Process.myUid()

            var wv = webView
            if (wv == null) {
                wv = WebView(context)
                webView = wv
            }
            wv.clearCache(true)

            val start = System.nanoTime()

            println("Running WEBSITETASK $targetUrl")

            val isTrafficServiceSupported = if (USE_PROCESS_UID_FOR_TRAFFIC_MEASUREMENT) {
                TrafficStats.getUidRxBytes(processUid) != TrafficStats.UNSUPPORTED.toLong()
            } else {
                TrafficStats.getTotalRxBytes() != TrafficStats.UNSUPPORTED.toLong()
            }

            if (!isTrafficServiceSupported) {
                trafficRxStart = -1
                trafficTxStart = -1
                trafficRxEnd = -1
                trafficTxEnd = -1
            } else {
                if (USE_PROCESS_UID_FOR_TRAFFIC_MEASUREMENT) {
                    trafficTxStart = TrafficStats.getUidTxBytes(processUid)
                    trafficRxStart = TrafficStats.getUidRxBytes(processUid)
                } else {
                    trafficTxStart = TrafficStats.getTotalTxBytes()
                    trafficRxStart = TrafficStats.getTotalRxBytes()
                }
            }

            val timeoutThread = Thread(Runnable {
                try {
                    println("WEBSITETASK STARTING TIMEOUT THREAD: $timeOut ms")
                    Thread.sleep(timeOut)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    Thread.currentThread().interrupt() // restore interrupt state
                    return@Runnable
                }

                if (!this@WebsiteTestServiceImpl.hasFinished() && listener != null) {
                    setEndTrafficCounter()

                    if (listener!!.onTimeoutReached(this@WebsiteTestServiceImpl)) {
                        println("WEBSITETESTTASK TIMEOUT")
                        this@WebsiteTestServiceImpl.handler.post {
                            this@WebsiteTestServiceImpl.webView?.stopLoading()
                        }
                    }
                }
            })

            timeoutThread.start()

            wv.settings.javaScriptEnabled = true
            wv.webViewClient = object : WebViewClient() {

                override fun onLoadResource(view: WebView, url: String) {
                    println("getting resource: " + url + " progress: " + view.progress)
                    resourceCount.incrementAndGet()
                    super.onLoadResource(view, url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)

                    this@WebsiteTestServiceImpl.isRunning.set(false)
                    this@WebsiteTestServiceImpl.hasFinished.set(true)
                    this@WebsiteTestServiceImpl.hasError.set(false)
                    this@WebsiteTestServiceImpl.duration = System.nanoTime() - start

                    if (this@WebsiteTestServiceImpl.trafficRxStart != -1L) {
                        setEndTrafficCounter()
                    }

                    println("PAGE FINISHED " + targetUrl + " progress: " + view.progress + "%, resources counter: " + resourceCount.get())
                    listener?.onRenderFinished(this@WebsiteTestServiceImpl)
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    this@WebsiteTestServiceImpl.isRunning.set(true)
                    this@WebsiteTestServiceImpl.hasFinished.set(false)
                    this@WebsiteTestServiceImpl.hasError.set(false)

                    listener?.onDownloadStarted(this@WebsiteTestServiceImpl)

                    println("PAGE STARTED $targetUrl")

                    super.onPageStarted(view, url, favicon)
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(view: WebView, errorCode: Int, description: String?, failingUrl: String?) {
                    @Suppress("DEPRECATION")
                    super.onReceivedError(view, errorCode, description, failingUrl)

                    this@WebsiteTestServiceImpl.isRunning.set(false)
                    this@WebsiteTestServiceImpl.hasFinished.set(true)
                    this@WebsiteTestServiceImpl.hasError.set(true)
                    this@WebsiteTestServiceImpl.duration = System.nanoTime() - start

                    if (this@WebsiteTestServiceImpl.trafficRxStart != -1L) {
                        setEndTrafficCounter()
                    }

                    listener?.onError(this@WebsiteTestServiceImpl)
                }
            }

            val task = AsyncHtmlStatusCodeRetriever()
            task.setContentRetrieverListener(object : AsyncHtmlStatusCodeRetriever.ContentRetrieverListener {
                override fun onContentFinished(statusCode: Int?) {
                    val code = statusCode ?: -1
                    this@WebsiteTestServiceImpl.statusCode = code
                    if (code >= 0) {
                        wv.loadUrl(targetUrl!!)
                    } else {
                        this@WebsiteTestServiceImpl.isRunning.set(false)
                        this@WebsiteTestServiceImpl.hasFinished.set(true)
                        this@WebsiteTestServiceImpl.hasError.set(true)
                        this@WebsiteTestServiceImpl.duration = System.nanoTime() - start

                        if (this@WebsiteTestServiceImpl.trafficRxStart != -1L) {
                            setEndTrafficCounter()
                        }

                        listener?.onError(this@WebsiteTestServiceImpl)
                    }
                }
            })

            task.execute(targetUrl)
        }
    }

    private fun setEndTrafficCounter() {
        if (USE_PROCESS_UID_FOR_TRAFFIC_MEASUREMENT) {
            this.trafficRxEnd = TrafficStats.getUidRxBytes(processUid)
            this.trafficTxEnd = TrafficStats.getUidTxBytes(processUid)
        } else {
            this.trafficRxEnd = TrafficStats.getTotalRxBytes()
            this.trafficTxEnd = TrafficStats.getTotalTxBytes()
        }
    }

    override fun isRunning(): Boolean = isRunning.get()

    override fun hasFinished(): Boolean = hasFinished.get()

    override fun setOnRenderingFinishedListener(listener: WebsiteTestService.RenderingListener) {
        this.listener = listener
    }

    override fun hasError(): Boolean = hasError.get()

    override fun getStatusCode(): Int = statusCode

    override fun getTxBytes(): Long = if (trafficTxStart != -1L) trafficTxEnd - trafficTxStart else -1

    override fun getRxBytes(): Long = if (trafficRxStart != -1L) trafficRxEnd - trafficRxStart else -1

    override fun getTotalTrafficBytes(): Long = if (getRxBytes() != -1L) getRxBytes() + getTxBytes() else -1

    override fun getResourceCount(): Int = resourceCount.get()

    companion object {
        /**
         * if set to true the traffic will be recorded using process-uid traffic stats, otherwise total traffic stats.
         */
        private const val USE_PROCESS_UID_FOR_TRAFFIC_MEASUREMENT = true
    }
}
