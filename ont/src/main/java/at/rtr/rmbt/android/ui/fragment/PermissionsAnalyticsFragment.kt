package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentPermissionsAnalyticsBinding
import at.rtr.rmbt.android.viewmodel.PermissionsViewModel

class PermissionsAnalyticsFragment : BaseFragment() {

    override val layoutResId: Int
        get() = R.layout.fragment_permissions_analytics

    private val activityViewModel: PermissionsViewModel by activityViewModels()
    private val binding: FragmentPermissionsAnalyticsBinding by bindingLazy()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cornerImage.post {
            binding.cornerImage.apply {
                translationX = 0.3f * binding.cornerImage.measuredWidth
                translationY = -0.4f * binding.cornerImage.measuredHeight
                visibility = View.VISIBLE
                requestLayout()
            }
        }

        binding.allow.setOnClickListener {
            activityViewModel.setAnalyticsSettings(true)
            activityViewModel.moveToNext()
        }
        binding.notAllow.setOnClickListener {
            activityViewModel.setAnalyticsSettings(false)
            activityViewModel.moveToNext()
        }
    }

    companion object {
        fun newInstance() = PermissionsAnalyticsFragment()
    }
}