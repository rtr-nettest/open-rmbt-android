package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentStatisticsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.StatisticsViewModel

class StatisticsFragment : BaseFragment() {

    private val statisticsViewModel: StatisticsViewModel by viewModelLazy()
    private val binding: FragmentStatisticsBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_statistics

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = statisticsViewModel.state

        statisticsViewModel.text.listen(this) {
            statisticsViewModel.state.text.set(it)
        }
    }
}