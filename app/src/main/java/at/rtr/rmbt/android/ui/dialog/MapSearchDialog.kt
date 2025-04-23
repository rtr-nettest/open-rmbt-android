package at.rtr.rmbt.android.ui.dialog

import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.DialogMapSearchBinding
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class MapSearchDialog : FullscreenDialog() {

    private lateinit var binding: DialogMapSearchBinding
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var searchJob: Job? = null

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
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_map_search, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editTextValue.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    startSearch()
                    true
                }
                else -> false
            }
        }

        binding.buttonSearch.setOnClickListener {
            startSearch()
        }

        binding.buttonCancel.setOnClickListener {
            searchJob?.cancel()
            dismiss()
        }
    }

    private fun startSearch() {
        val searchText: String = binding.editTextValue.text.toString()
        if (searchText.isNotEmpty()) {
            val searchValue = if (!searchText.contains("Austria")) {
                "$searchText Austria"
            } else {
                searchText
            }
            binding.buttonSearch.visibility = View.INVISIBLE
            binding.progressbar.visibility = View.VISIBLE
            searchJob?.cancel()
            searchJob = coroutineScope.launch(CoroutineName("mapSearchDialog search job")) {
                loadResults(searchValue) {
                    val mainThreadHandler = Handler(Looper.getMainLooper())
                    mainThreadHandler.post {
                        callback?.onAddressResult(it)
                        binding.buttonSearch.visibility = View.VISIBLE
                        binding.progressbar.visibility = View.GONE
                        dismiss()
                    }
                }
            }
        }
    }

    private fun loadResults(value: String, found: (Address?) -> Unit) {
        val geocoder = Geocoder(requireContext())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(value, 1) { addressList ->
                    if (addressList.isNotEmpty()) {
                        found.invoke(addressList[0])
                    } else {
                        found.invoke(null)
                    }
                }
            } else {
                val addressList: List<Address>? = geocoder.getFromLocationName(value, 1)
                if (!addressList.isNullOrEmpty()) {
                    found.invoke(addressList[0])
                } else {
                    found.invoke(null)
                }
            }
        } catch (e: IOException) {
            handleMapSearchException(e, found)
        } catch (e: IllegalArgumentException) {
            handleMapSearchException(e, found)
        }
    }

    private fun handleMapSearchException(e: Exception, found: (Address?) -> Unit) {
        e.printStackTrace()
        found.invoke(null)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        dialog?.let {
            dialog.window?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    companion object {

        fun instance(
            fragment: Fragment,
            requestCode: Int
        ): FullscreenDialog {
            val mapSearchDialog = MapSearchDialog()
            mapSearchDialog.setTargetFragment(fragment, requestCode)
            return mapSearchDialog
        }
    }

    interface Callback {
        fun onAddressResult(address: Address?)
    }
}