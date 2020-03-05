package at.rtr.rmbt.android.ui.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import at.rmbt.client.control.Server
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogServerSelectionBinding
import at.rtr.rmbt.android.ui.adapter.ServerSelectionAdapter
import at.rtr.rmbt.android.util.args
import java.io.Serializable

class ServerSelectionDialog : FullscreenDialog() {

    private lateinit var binding: DialogServerSelectionBinding
    private var measurementServers = mutableListOf<Server>()
    private val adapter: ServerSelectionAdapter by lazy { ServerSelectionAdapter() }

    private val callback: Callback?
        get() = when {
            targetFragment is Callback -> targetFragment as Callback
            activity is Callback -> activity as Callback
            else -> null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_server_selection, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewServerList.adapter = adapter
        binding.recyclerViewServerList.apply {

            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.recyclerViewServerList.addItemDecoration(itemDecoration)
        }

        arguments?.getSerializable(KEY_SERVER_LIST)?.let { list ->
            measurementServers.clear()
            measurementServers.add(Server(context?.getString(R.string.preferences_default_server_selection), "default"))
            measurementServers.addAll(list as List<Server>)
        }
        adapter.init(measurementServers, arguments?.getString(KEY_DEFAULT_VALUE))

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        adapter.actionCallback = {
            callback?.onSelectServer(it)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        dialog?.let {
            dialog.window?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    companion object {

        private const val KEY_DEFAULT_VALUE: String = "key_default_value"
        private const val KEY_SERVER_LIST: String = "key_server_list"
        private const val KEY_IS_CANCELABLE: String = "key_is_cancelable"

        fun instance(
            defaultValue: String?,
            measurementServers: List<Server>?,
            fragment: Fragment? = null,
            isCancelable: Boolean = true,
            requestCode: Int = -1
        ): FullscreenDialog {

            val inputSettingDialog = ServerSelectionDialog()
            fragment?.let { inputSettingDialog.setTargetFragment(it, requestCode) }
            inputSettingDialog.args {
                putString(KEY_DEFAULT_VALUE, defaultValue)
                putSerializable(KEY_SERVER_LIST, measurementServers as Serializable)
                putBoolean(KEY_IS_CANCELABLE, isCancelable)
            }
            return inputSettingDialog
        }
    }

    interface Callback {
        fun onSelectServer(server: Server)
    }
}