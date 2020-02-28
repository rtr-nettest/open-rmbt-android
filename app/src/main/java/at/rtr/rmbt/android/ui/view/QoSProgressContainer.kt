package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemQosMeasurementBinding
import at.rtr.rmbt.android.util.bindWith
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import timber.log.Timber

private const val DELAY_PROGRESS_UPDATE_MS = 60L
private const val DELAY_REMOVAL_MS = 2000L
private const val PROGRESS_STEP = 10

class QoSProgressContainer @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    private val qosItems = listOf(
        Pair(QoSTestResultEnum.WEBSITE, R.string.measurement_qos_web_site),
        Pair(QoSTestResultEnum.NON_TRANSPARENT_PROXY, R.string.measurement_qos_transparent_connection),
        Pair(QoSTestResultEnum.DNS, R.string.measurement_qos_dns),
        Pair(QoSTestResultEnum.TCP, R.string.measurement_qos_tcp_ports),
        Pair(QoSTestResultEnum.UDP, R.string.measurement_qos_udp_ports),
        Pair(QoSTestResultEnum.HTTP_PROXY, R.string.measurement_qos_unmodified_content),
        Pair(QoSTestResultEnum.TRACEROUTE, R.string.measurement_qos_traceroute),
        Pair(QoSTestResultEnum.TRACEROUTE_MASKED, R.string.measurement_qos_traceroute),
        Pair(QoSTestResultEnum.VOIP, R.string.measurement_qos_voip)
    )

    private val items = mutableMapOf<QoSTestResultEnum, QoSEntry>()

    private val progressHandler = Handler {
        val type = QoSTestResultEnum.values()[it.what]

        items[type]?.let { item ->
            if (item.progress != item.visibleProgress) {
                if (item.progress - item.visibleProgress >= PROGRESS_STEP) {
                    item.visibleProgress += PROGRESS_STEP
                    Timber.i("Progress $type Updated: ${item.visibleProgress}")
                } else {
                    item.visibleProgress = item.progress
                    Timber.i("Progress $type Finished: ${item.visibleProgress}")
                }

                if (item.visibleProgress < item.progress) {
                    sendProgressMessage(type)
                    Timber.d("Progress $type scheduled: ${item.visibleProgress}")
                } else if (item.visibleProgress == 100) {
                    sendRemovalMessage(type)
                    Timber.w("Progress $type for removal: ${item.visibleProgress}")
                }
            }
        }

        true
    }

    private val removalHandler = Handler {
        val type = QoSTestResultEnum.values()[it.what]

        items[type]?.let { item ->
            item.isRemoved = true
        }
        true
    }

    init {
        reset()
    }

    fun reset() {
        removeAllViews()
        orientation = VERTICAL
        qosItems.forEach {
            val binding: ItemQosMeasurementBinding = bindWith(R.layout.item_qos_measurement)
            binding.textQosTitle.text = context.getString(it.second)
            addView(binding.root)
            items[it.first] = QoSEntry(binding)
            items[it.first]?.isRemoved = false
        }
        Timber.d("Items: ${items.size} QosItems: ${qosItems.size} Views size: ${this.childCount}")
    }

    private fun sendProgressMessage(type: QoSTestResultEnum) {
        progressHandler.removeMessages(type.ordinal)
        progressHandler.sendEmptyMessageDelayed(type.ordinal, DELAY_PROGRESS_UPDATE_MS)
    }

    private fun sendRemovalMessage(type: QoSTestResultEnum) {
        removalHandler.removeMessages(type.ordinal)
        removalHandler.sendEmptyMessageDelayed(type.ordinal, DELAY_REMOVAL_MS)
    }

    fun update(data: Map<QoSTestResultEnum, Int>) {
        Timber.d("update count ${data.size}")
        items.forEach { entry ->
            if (data.containsKey(entry.key)) {
                data[entry.key]?.let { progress ->
                    entry.value.progress = progress
                    sendProgressMessage(entry.key)
                }
            } else {
                entry.value.isRemoved = true
            }
        }
    }

    private data class QoSEntry(
        private val binding: ItemQosMeasurementBinding,
        var progress: Int = 0,
        val pendingRemoval: Boolean = false
    ) {
        var isRemoved: Boolean
            get() = binding.root.visibility != View.GONE
            set(value) {
                if (value) {
                    binding.root.visibility = View.GONE
                } else {
                    binding.root.visibility = View.VISIBLE
                }
            }

        var visibleProgress: Int
            get() = binding.progressBarQos.progress
            set(value) {
                binding.progressBarQos.progress = value
            }
    }
}