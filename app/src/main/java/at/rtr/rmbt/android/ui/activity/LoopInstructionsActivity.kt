package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.viewpager.widget.PagerAdapter
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopInstructionsBinding
import at.rtr.rmbt.android.databinding.ViewLoopModeInstructionBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.viewmodel.LoopConfigurationViewModel

class LoopInstructionsActivity : BaseActivity(), Callback {

    private lateinit var binding: ActivityLoopInstructionsBinding
    private val viewModel: LoopConfigurationViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_instructions)
        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.title.text = getString(R.string.title_loop_instruction_1)

        binding.pager.adapter = InstructionsAdapter(this, this)
        binding.pager.currentItem = 0
    }

    override fun onDeclined() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onFirstPageAccepted() {
        binding.title.text = getString(R.string.title_loop_instruction_2)
        binding.pager.setCurrentItem(1, true)
    }

    override fun onSecondPageAccepted() {
        binding.title.text = getString(R.string.title_loop_instruction_3)
        binding.pager.setCurrentItem(2, true)
    }

    override fun onThirdPageAccepted() {
        setResult(Activity.RESULT_OK)
        if (isNeedToAskForNotificationPermission()) {
            checkNotificationPermission()
        } else {
            checkBackgroundLocationPermission()
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            checkBackgroundLocationPermission()
            finish()
        }
    }

    private fun isNeedToAskForNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasNotificationPermission) {
                false
            } else {
                viewModel.shouldAskForNotificationPermission()
            }
        } else {
            false
        }
    }
    private fun checkNotificationPermission() {
        if (isNeedToAskForNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (viewModel.shouldAskForNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
                viewModel.notificationPermissionsWereAsked()
            }
        }
    }

    //@TODO: De-duplicate from LoopConfigurationActivity
    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasForegroundLocationPermission =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasForegroundLocationPermission) {
                val hasBackgroundLocationPermission = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasBackgroundLocationPermission) {
                    // handle location update
                } else {
                    if (viewModel.shouldAskForBackgroundPermission()) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            REQUEST_CODE_BACKGROUND
                        )
                        viewModel.backgroundPermissionsWereAsked()
                    }
                }
            } else {
                if (viewModel.shouldAskForPermission()) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ), REQUEST_CODE_BACKGROUND
                    )
                    viewModel.backgroundPermissionsWereAsked()
                }
            }
        }
    }

    inner class InstructionsAdapter(context: Context, private val callback: Callback) : PagerAdapter() {

        private var items = listOf(
            context.getString(R.string.text_loop_instruction_1),
            context.getString(R.string.text_loop_instruction_2),
            context.getString(R.string.text_loop_instruction_3),
        )

        override fun isViewFromObject(view: View, o: Any) = view == o
        override fun getCount() = items.size

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val binding = ViewLoopModeInstructionBinding.inflate(LayoutInflater.from(container.context))

            binding.content.text = items[position]

            binding.decline.setOnClickListener { callback.onDeclined() }
            binding.accept.setOnClickListener {
                when (position) {
                    0 -> callback.onFirstPageAccepted()
                    1 -> callback.onSecondPageAccepted()
                    else -> callback.onThirdPageAccepted()
                }
            }
            container.addView(binding.root)
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, o: Any) {
            container.removeView(o as View)
        }
    }

    companion object {
        fun start(context: Context): Intent = Intent(context, LoopInstructionsActivity::class.java)
        private const val REQUEST_CODE_BACKGROUND = 1
        private const val REQUEST_CODE_NOTIFICATION = 2
    }
}

interface Callback {
    fun onDeclined()
    fun onFirstPageAccepted()
    fun onSecondPageAccepted()
    fun onThirdPageAccepted()
}