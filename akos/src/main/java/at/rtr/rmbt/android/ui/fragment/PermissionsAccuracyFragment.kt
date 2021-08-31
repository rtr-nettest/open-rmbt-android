package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentPermissionsAccuracyBinding
import at.rtr.rmbt.android.ui.activity.PermissionsActivity
import at.rtr.rmbt.android.viewmodel.PermissionsViewModel

class PermissionsAccuracyFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_permissions_accuracy

    private val activityViewModel: PermissionsViewModel by activityViewModels()
    private val binding: FragmentPermissionsAccuracyBinding by bindingLazy()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cornerImage.post {
            binding.cornerImage.apply {
                translationX = 0.3f * binding.cornerImage.measuredWidth
                translationY = -0.4f * binding.cornerImage.measuredHeight
                requestLayout()
                visibility = View.VISIBLE
            }
        }

        binding.next.setOnClickListener {
            if (activityViewModel.shouldAskAccuracy()) {
                requestPermissions(activityViewModel.accuracyPermissions, PermissionsActivity.REQUEST_CODE_ACCURACY)
            } else {
                activityViewModel.moveToNext()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        activityViewModel.moveToNext()
    }

    companion object {
        fun newInstance() = PermissionsAccuracyFragment()
    }
}