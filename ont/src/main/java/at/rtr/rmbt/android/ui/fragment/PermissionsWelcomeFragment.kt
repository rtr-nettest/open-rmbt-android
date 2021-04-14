package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentPermissionsWelcomeBinding
import at.rtr.rmbt.android.viewmodel.PermissionsViewModel

class PermissionsWelcomeFragment : BaseFragment() {

    private val activityViewModel: PermissionsViewModel by activityViewModels()
    private val binding: FragmentPermissionsWelcomeBinding by bindingLazy()

    override val layoutResId: Int
        get() = R.layout.fragment_permissions_welcome

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.next.setOnClickListener {
            activityViewModel.moveToNext()
        }
    }

    companion object {
        fun newInstance() = PermissionsWelcomeFragment()
    }
}